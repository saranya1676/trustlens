package com.truthlens.service;

import com.truthlens.dto.ChatResponseDTO;
import com.truthlens.entity.NewsArticle;
import com.truthlens.entity.NewsCategory;
import com.truthlens.entity.TrustedArticle;
import com.truthlens.repository.NewsArticleRepository;
import com.truthlens.repository.TrustedArticleRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * ChatbotService — core intelligence behind the TruthLens AI chat assistant.
 *
 * On the first chat request the service builds a parallel knowledge index
 * over ALL news_articles AND trusted_articles simultaneously using a
 * ForkJoinPool. Every subsequent request hits the in-memory cache.
 *
 * Response pipeline:
 *  1. Detect intent from the user message
 *  2. Extract search terms
 *  3. Score ALL indexed articles in parallel (ForkJoinPool)
 *  4. Pick the top-N matches
 *  5. Generate a natural-language answer
 */
@Service
@Transactional(readOnly = true)
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final NewsArticleRepository    newsRepo;
    private final TrustedArticleRepository trustedRepo;

    // ── In-memory knowledge base (built once, reused) ─────────────
    private volatile List<IndexedArticle> knowledgeBase = null;
    private final Object                  indexLock     = new Object();

    // ── Tunables ──────────────────────────────────────────────────
    private static final int MAX_RESULTS      = 4;
    private static final int MIN_SCORE        = 5;
    private static final int PARALLEL_THREADS = 4;

    // ── Stop words ────────────────────────────────────────────────
    private static final Set<String> STOP = Set.of(
        "a","an","the","is","are","was","were","be","been","do","does","did",
        "will","would","can","could","should","may","might","to","of","in",
        "on","at","by","for","with","and","but","or","not","this","that",
        "what","which","who","how","when","where","why","tell","me","show",
        "give","find","list","any","all","some","about","from","have","has"
    );

    // ── Intent keyword maps ───────────────────────────────────────
    private static final Map<String, List<String>> INTENTS = Map.of(
        "GREETING",          List.of("hi","hello","hey","greetings","good morning","good evening","howdy"),
        "VERIFICATION_HELP", List.of("verify","fact check","fake","false","true","misinformation","hoax","real","check","trust"),
        "CATEGORY_INFO",     List.of("categories","category","section","topics","types","international","national","politics","sports","technology"),
        "LATEST_NEWS",       List.of("latest","recent","new","today","now","just","breaking","current","update"),
        "HOW_IT_WORKS",      List.of("how","work","process","algorithm","engine","analyze","detect","score","confidence"),
        "ARTICLE_SEARCH",    List.of("news","article","story","report","post","about","find","search","show")
    );

    public ChatbotService(NewsArticleRepository newsRepo, TrustedArticleRepository trustedRepo) {
        this.newsRepo    = newsRepo;
        this.trustedRepo = trustedRepo;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Main entry point — processes a user message and returns a full response.
     */
    public ChatResponseDTO chat(String userMessage) {
        if (StringUtils.isBlank(userMessage)) {
            return buildResponse("Please type a question and I'll do my best to help!",
                    "UNKNOWN", Collections.emptyList(), 0);
        }

        // Ensure knowledge base is ready
        ensureKnowledgeBase();

        String lower  = userMessage.toLowerCase().trim();
        String intent = detectIntent(lower);
        log.debug("Chat intent={} for message='{}'", intent, userMessage);

        return switch (intent) {
            case "GREETING"          -> handleGreeting();
            case "HOW_IT_WORKS"      -> handleHowItWorks();
            case "VERIFICATION_HELP" -> handleVerificationHelp(lower);
            case "CATEGORY_INFO"     -> handleCategoryInfo(lower);
            case "LATEST_NEWS"       -> handleLatestNews();
            default                  -> handleArticleSearch(lower, userMessage);
        };
    }

    /**
     * Returns the current size of the knowledge base (for health/status endpoints).
     */
    public int getKnowledgeBaseSize() {
        if (knowledgeBase == null) return 0;
        return knowledgeBase.size();
    }

    // =========================================================================
    // Knowledge base construction (parallel)
    // =========================================================================

    /**
     * Builds the knowledge base the first time it is needed.
     * Uses a ForkJoinPool to index news articles and trusted articles in parallel.
     */
    private void ensureKnowledgeBase() {
        if (knowledgeBase != null) return;
        synchronized (indexLock) {
            if (knowledgeBase != null) return;
            log.info("Building chatbot knowledge base in parallel...");
            knowledgeBase = buildKnowledgeBase();
            log.info("Knowledge base ready: {} articles indexed", knowledgeBase.size());
        }
    }

    private List<IndexedArticle> buildKnowledgeBase() {
        ForkJoinPool pool = new ForkJoinPool(PARALLEL_THREADS);
        try {
            // Fetch both lists from DB
            List<NewsArticle>    newsArticles    = newsRepo.findAll();
            List<TrustedArticle> trustedArticles = trustedRepo.findByVerifiedTrueOrderByCredibilityScoreDesc();

            // Index both lists in parallel
            Future<List<IndexedArticle>> newsFuture = pool.submit(() ->
                newsArticles.parallelStream()
                    .map(a -> new IndexedArticle(
                        a.getId(),
                        a.getTitle(),
                        a.getContent(),
                        a.getSummary(),
                        a.getKeywords(),
                        a.getCategory().name(),
                        a.getCategory().getDisplayName(),
                        a.getSourceName(),
                        "NEWS",
                        buildTokenSet(a.getTitle(), a.getContent(), a.getKeywords(), a.getSummary())
                    ))
                    .collect(Collectors.toList())
            );

            Future<List<IndexedArticle>> trustedFuture = pool.submit(() ->
                trustedArticles.parallelStream()
                    .map(a -> new IndexedArticle(
                        a.getId(),
                        a.getTitle(),
                        a.getContent(),
                        a.getSummary(),
                        a.getKeywords(),
                        a.getCategory().name(),
                        a.getCategory().getDisplayName(),
                        a.getSourceName(),
                        "TRUSTED",
                        buildTokenSet(a.getTitle(), a.getContent(), a.getKeywords(), a.getSummary())
                    ))
                    .collect(Collectors.toList())
            );

            List<IndexedArticle> combined = new ArrayList<>();
            combined.addAll(newsFuture.get());
            combined.addAll(trustedFuture.get());
            return combined;

        } catch (InterruptedException | ExecutionException e) {
            log.error("Parallel index build failed, falling back to sequential", e);
            Thread.currentThread().interrupt();
            return buildKnowledgeBaseSequential();
        } finally {
            pool.shutdown();
        }
    }

    private List<IndexedArticle> buildKnowledgeBaseSequential() {
        List<IndexedArticle> list = new ArrayList<>();
        newsRepo.findAll().forEach(a -> list.add(new IndexedArticle(
            a.getId(), a.getTitle(), a.getContent(), a.getSummary(), a.getKeywords(),
            a.getCategory().name(), a.getCategory().getDisplayName(), a.getSourceName(),
            "NEWS", buildTokenSet(a.getTitle(), a.getContent(), a.getKeywords(), a.getSummary())
        )));
        trustedRepo.findByVerifiedTrueOrderByCredibilityScoreDesc().forEach(a -> list.add(new IndexedArticle(
            a.getId(), a.getTitle(), a.getContent(), a.getSummary(), a.getKeywords(),
            a.getCategory().name(), a.getCategory().getDisplayName(), a.getSourceName(),
            "TRUSTED", buildTokenSet(a.getTitle(), a.getContent(), a.getKeywords(), a.getSummary())
        )));
        return list;
    }

    /** Tokenises text into a lowercase word-frequency map. */
    private Map<String, Integer> buildTokenSet(String... texts) {
        Map<String, Integer> freq = new HashMap<>();
        for (String text : texts) {
            if (text == null) continue;
            for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
                if (word.length() >= 3 && !STOP.contains(word)) {
                    freq.merge(word, 1, Integer::sum);
                }
            }
        }
        return freq;
    }

    // =========================================================================
    // Intent detection
    // =========================================================================

    private String detectIntent(String lower) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : INTENTS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) score += (lower.startsWith(kw) ? 3 : 1);
            }
            scores.put(entry.getKey(), score);
        }
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("ARTICLE_SEARCH");
    }

    // =========================================================================
    // Intent handlers
    // =========================================================================

    private ChatResponseDTO handleGreeting() {
        int total   = knowledgeBase.size();
        int news    = (int) knowledgeBase.stream().filter(a -> "NEWS".equals(a.type)).count();
        int trusted = total - news;
        String answer = String.format(
            "👋 Hi! I'm the **TruthLens AI Assistant**.\n\n" +
            "I've analysed **%d articles** across %d news stories and %d trusted references. " +
            "You can ask me:\n" +
            "• *\"Show me news about climate\"*\n" +
            "• *\"What's happening in sports?\"*\n" +
            "• *\"How does fact-checking work?\"*\n" +
            "• *\"Find articles about AI or technology\"*\n\n" +
            "What would you like to know?",
            total, news, trusted);
        return buildResponse(answer, "GREETING", Collections.emptyList(), 100);
    }

    private ChatResponseDTO handleHowItWorks() {
        String answer =
            "🔍 **How TruthLens AI Verifies News**\n\n" +
            "**Step 1 — Submit content**\n" +
            "Paste text, a URL, or an image on the [Verify page](/verify).\n\n" +
            "**Step 2 — Keyword extraction**\n" +
            "The engine strips stop-words and extracts meaningful unigrams and bigrams.\n\n" +
            "**Step 3 — Category detection**\n" +
            "Signal keywords map the content to one of 5 categories: International, National, Politics, Sports, or Technology.\n\n" +
            "**Step 4 — Source matching**\n" +
            "Content is scored against verified articles from Reuters, BBC, AP, and more.\n\n" +
            "**Step 5 — Verdict**\n" +
            "A confidence score (0–100%) determines the verdict:\n" +
            "✅ *Likely True* · ❌ *Likely False* · ⚠️ *Needs Verification*\n\n" +
            "Want to try it? Go to the [Verify page](/verify).";
        return buildResponse(answer, "HOW_IT_WORKS", Collections.emptyList(), 100);
    }

    private ChatResponseDTO handleVerificationHelp(String lower) {
        List<IndexedArticle> top = searchArticles(lower, 3);
        String answer;
        if (top.isEmpty()) {
            answer = "🛡️ To verify a news article, head to the **[Verify page](/verify)**.\n\n" +
                     "You can submit:\n" +
                     "• **Text** — paste the article directly\n" +
                     "• **URL** — paste the news link\n" +
                     "• **Image** — upload a screenshot of the article\n\n" +
                     "You'll get a verdict, confidence score, and matched trusted sources within seconds.";
        } else {
            answer = "🛡️ I found some related verified articles that might help. " +
                     "You can also **[verify any article yourself](/verify)** — " +
                     "just paste the text and get an instant AI fact-check.";
        }
        List<ChatResponseDTO.ArticleRef> refs = toRefs(top);
        return buildResponse(answer, "VERIFICATION_HELP", refs, 90);
    }

    private ChatResponseDTO handleCategoryInfo(String lower) {
        // Check if asking about a specific category
        NewsCategory matched = null;
        for (NewsCategory cat : NewsCategory.values()) {
            if (lower.contains(cat.name().toLowerCase()) ||
                lower.contains(cat.getDisplayName().toLowerCase())) {
                matched = cat;
                break;
            }
        }

        List<IndexedArticle> top;
        String answer;
        if (matched != null) {
            final NewsCategory finalCat = matched;
            top = knowledgeBase.stream()
                    .filter(a -> finalCat.name().equals(a.category))
                    .limit(MAX_RESULTS)
                    .collect(Collectors.toList());
            long count = top.stream().filter(a -> "NEWS".equals(a.type)).count();
            answer = String.format(
                "📂 **%s News** — I found **%d articles** in this category.\n\n" +
                "Here are the top stories. Click any title to read more, " +
                "or visit the [%s section](/news?category=%s) for all articles.",
                matched.getDisplayName(), count, matched.getDisplayName(), matched.name());
        } else {
            top = Collections.emptyList();
            answer = "📂 **TruthLens covers 5 categories:**\n\n" +
                     "🌍 [International](/news?category=INTERNATIONAL) — Global news & world events\n" +
                     "🏠 [National](/news?category=NATIONAL) — Domestic news & local affairs\n" +
                     "🏛️ [Politics](/news?category=POLITICS) — Elections, government & policy\n" +
                     "🏆 [Sports](/news?category=SPORTS) — Matches, tournaments & athletes\n" +
                     "💻 [Technology](/news?category=TECHNOLOGY) — AI, science & innovation\n\n" +
                     "Which category would you like to explore?";
        }
        return buildResponse(answer, "CATEGORY_INFO", toRefs(top), 95);
    }

    private ChatResponseDTO handleLatestNews() {
        // Return the most recently added NEWS articles (first in list = most recently inserted)
        List<IndexedArticle> latest = knowledgeBase.stream()
                .filter(a -> "NEWS".equals(a.type))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());

        String answer = String.format(
            "📰 **Latest News** — Here are %d recent articles from our database.\n\n" +
            "For the full news feed, visit the [Browse News](/news) page.",
            latest.size());
        return buildResponse(answer, "LATEST_NEWS", toRefs(latest), 90);
    }

    private ChatResponseDTO handleArticleSearch(String lower, String original) {
        List<IndexedArticle> results = searchArticles(lower, MAX_RESULTS);

        if (results.isEmpty()) {
            String answer = String.format(
                "🔎 I searched all **%d articles** in our database but couldn't find " +
                "a strong match for *\"%s\"*.\n\n" +
                "**Try:**\n" +
                "• Different keywords (e.g. *climate* instead of *global warming*)\n" +
                "• Browsing by category: [International](/news?category=INTERNATIONAL) · " +
                "[Politics](/news?category=POLITICS) · [Technology](/news?category=TECHNOLOGY)\n" +
                "• [Searching all articles](/news?q=%s)",
                knowledgeBase.size(),
                original,
                original.replace(" ", "+"));
            return buildResponse(answer, "ARTICLE_SEARCH", Collections.emptyList(), 20);
        }

        int topScore = results.get(0).score;
        int confidence = Math.min(95, 40 + topScore);

        String answer = String.format(
            "📰 I found **%d article%s** matching *\"%s\"*.\n\n" +
            "Here are the most relevant results. Click a title to read the full story, " +
            "or [search all articles](/news?q=%s) for more.",
            results.size(),
            results.size() != 1 ? "s" : "",
            original,
            original.replace(" ", "+"));

        return buildResponse(answer, "ARTICLE_SEARCH", toRefs(results), confidence);
    }

    // =========================================================================
    // Parallel article scoring
    // =========================================================================

    /**
     * Scores ALL articles in the knowledge base in parallel using ForkJoinPool,
     * then returns the top-N above the minimum score threshold.
     */
    private List<IndexedArticle> searchArticles(String query, int limit) {
        // Build query token set
        Map<String, Integer> queryTokens = buildTokenSet(query);
        if (queryTokens.isEmpty()) return Collections.emptyList();

        ForkJoinPool pool = new ForkJoinPool(PARALLEL_THREADS);
        try {
            List<IndexedArticle> scored = pool.submit(() ->
                knowledgeBase.parallelStream()
                    .map(article -> {
                        int score = computeScore(article, queryTokens, query.toLowerCase());
                        article.score = score;
                        return article;
                    })
                    .filter(a -> a.score >= MIN_SCORE)
                    .sorted(Comparator.comparingInt((IndexedArticle a) -> a.score).reversed())
                    .limit(limit)
                    .collect(Collectors.toList())
            ).get();
            return scored;
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Parallel search failed, running sequential", e);
            Thread.currentThread().interrupt();
            return knowledgeBase.stream()
                    .map(a -> { a.score = computeScore(a, queryTokens, query); return a; })
                    .filter(a -> a.score >= MIN_SCORE)
                    .sorted(Comparator.comparingInt((IndexedArticle a) -> a.score).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } finally {
            pool.shutdown();
        }
    }

    private int computeScore(IndexedArticle article, Map<String, Integer> queryTokens, String rawQuery) {
        int score = 0;

        // Token overlap score
        for (Map.Entry<String, Integer> qToken : queryTokens.entrySet()) {
            Integer articleFreq = article.tokens.get(qToken.getKey());
            if (articleFreq != null) {
                // Title matches are worth 3x
                boolean inTitle = article.title != null &&
                        article.title.toLowerCase().contains(qToken.getKey());
                score += inTitle ? qToken.getValue() * 3 : qToken.getValue();
            }
        }

        // Exact phrase bonus
        if (article.title != null && article.title.toLowerCase().contains(rawQuery)) {
            score += 15;
        }
        if (article.summary != null && article.summary.toLowerCase().contains(rawQuery)) {
            score += 8;
        }

        // Trusted articles get a small credibility bonus
        if ("TRUSTED".equals(article.type)) score += 2;

        return score;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<ChatResponseDTO.ArticleRef> toRefs(List<IndexedArticle> articles) {
        return articles.stream().map(a -> {
            ChatResponseDTO.ArticleRef ref = new ChatResponseDTO.ArticleRef();
            ref.setId(a.id);
            ref.setTitle(a.title);
            ref.setCategory(a.category);
            ref.setCategoryDisplayName(a.categoryDisplayName);
            ref.setSourceName(a.sourceName != null ? a.sourceName : "TruthLens");
            ref.setSnippet(buildSnippet(a.summary != null ? a.summary : a.content));
            ref.setRelevanceScore(a.score);
            return ref;
        }).collect(Collectors.toList());
    }

    private String buildSnippet(String text) {
        if (text == null) return "";
        return text.length() > 120 ? text.substring(0, 120) + "…" : text;
    }

    private ChatResponseDTO buildResponse(String answer, String intent,
                                           List<ChatResponseDTO.ArticleRef> refs, int confidence) {
        ChatResponseDTO dto = new ChatResponseDTO();
        dto.setAnswer(answer);
        dto.setIntent(intent);
        dto.setRelatedArticles(refs);
        dto.setConfidence(confidence);
        return dto;
    }

    /**
     * Invalidates the knowledge base cache so it is rebuilt on the next request.
     * Call this after new articles are saved.
     */
    public void invalidateCache() {
        synchronized (indexLock) {
            knowledgeBase = null;
            log.info("Chatbot knowledge base cache invalidated.");
        }
    }

    // =========================================================================
    // Internal: indexed article record
    // =========================================================================

    /** Lightweight in-memory representation of an article for fast scoring. */
    private static class IndexedArticle {
        final Long   id;
        final String title;
        final String content;
        final String summary;
        final String keywords;
        final String category;
        final String categoryDisplayName;
        final String sourceName;
        final String type;               // "NEWS" or "TRUSTED"
        final Map<String, Integer> tokens;
        volatile int score = 0;

        IndexedArticle(Long id, String title, String content, String summary,
                        String keywords, String category, String categoryDisplayName,
                        String sourceName, String type, Map<String, Integer> tokens) {
            this.id                  = id;
            this.title               = title;
            this.content             = content;
            this.summary             = summary;
            this.keywords            = keywords;
            this.category            = category;
            this.categoryDisplayName = categoryDisplayName;
            this.sourceName          = sourceName;
            this.type                = type;
            this.tokens              = tokens;
        }
    }
}
