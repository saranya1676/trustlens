package com.truthlens.service;

import com.truthlens.dto.NewsArticleDTO;
import com.truthlens.dto.VerificationRequestDTO;
import com.truthlens.dto.VerificationResultDTO;
import com.truthlens.entity.*;
import com.truthlens.repository.NewsArticleRepository;
import com.truthlens.repository.TrustedArticleRepository;
import com.truthlens.repository.VerificationResultRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private static final int HIGH_CONFIDENCE_THRESHOLD   = 70;
    private static final int MEDIUM_CONFIDENCE_THRESHOLD = 40;
    private static final int MIN_KEYWORD_LENGTH          = 4;
    private static final int MAX_CANDIDATE_ARTICLES      = 10;
    private static final int TOP_MATCHES_TO_SAVE         = 5;

    private static final Set<String> STOP_WORDS = Set.of(
        "the","a","an","is","are","was","were","be","been","being","have","has","had",
        "do","does","did","will","would","could","should","may","might","shall","can",
        "to","of","in","on","at","by","for","with","about","between","through","from",
        "up","down","out","this","that","these","those","i","me","my","we","our","you",
        "your","he","his","she","her","it","its","they","them","their","what","which",
        "who","when","where","why","how","and","but","or","not","so","yet","both",
        "than","too","very","into","as","news","report","says","said","new","also"
    );

    private static final Map<NewsCategory, List<String>> CATEGORY_SIGNALS = Map.of(
        NewsCategory.INTERNATIONAL, List.of("international","global","world","un","united nations",
            "foreign","country","countries","nation","nations","diplomat","treaty","summit",
            "eu","nato","g7","g20","imf","wto","embassy"),
        NewsCategory.NATIONAL, List.of("national","domestic","local","federal","state",
            "government","policy","law","legislation","parliament","congress","senate",
            "ministry","citizens","population","census","budget","economy","infrastructure"),
        NewsCategory.POLITICS, List.of("election","vote","voting","ballot","party","political",
            "president","prime minister","governor","senator","campaign","legislation","bill",
            "reform","supreme court","constitution","cabinet","opposition","scandal"),
        NewsCategory.SPORTS, List.of("football","soccer","basketball","cricket","tennis",
            "baseball","golf","athletics","olympics","championship","tournament","league",
            "match","game","player","team","score","goal","trophy","medal","record",
            "athlete","sport","fifa","nfl","nba","icc"),
        NewsCategory.TECHNOLOGY, List.of("technology","tech","ai","artificial intelligence",
            "machine learning","software","hardware","computer","internet","cyber","digital",
            "data","cloud","startup","app","algorithm","robot","automation","semiconductor",
            "chip","quantum","blockchain","crypto","5g")
    );

    private final TrustedArticleRepository    trustedArticleRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final NewsArticleRepository       newsArticleRepository;
    private final NewsService                 newsService;

    public VerificationService(TrustedArticleRepository trustedArticleRepository,
                                VerificationResultRepository verificationResultRepository,
                                NewsArticleRepository newsArticleRepository,
                                NewsService newsService) {
        this.trustedArticleRepository    = trustedArticleRepository;
        this.verificationResultRepository = verificationResultRepository;
        this.newsArticleRepository       = newsArticleRepository;
        this.newsService                 = newsService;
    }

    // ── Public API ────────────────────────────────────────────────

    @Transactional
    public VerificationResultDTO verify(VerificationRequestDTO request, String ipAddress) {
        log.info("Starting verification for type={}", request.getSubmissionType());

        String textToAnalyze = prepareText(request);
        List<String> keywords = extractKeywords(textToAnalyze);
        log.debug("Extracted {} keywords", keywords.size());

        NewsCategory detectedCategory = detectCategory(textToAnalyze, keywords);
        log.debug("Detected category: {}", detectedCategory);

        List<TrustedArticle> candidates = findCandidates(keywords, detectedCategory);
        List<ScoredCandidate> scored    = scoreCandidates(candidates, keywords, textToAnalyze);
        VerdictResult verdictResult     = deriveVerdict(scored, keywords.size());

        VerificationResult entity = buildEntity(request, textToAnalyze, ipAddress,
                verdictResult, detectedCategory, scored);
        VerificationResult saved = verificationResultRepository.save(entity);

        List<NewsArticleDTO> similarNews = fetchSimilarNews(detectedCategory);
        return toDTO(saved, scored, similarNews);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationResultDTO> getResultById(Long id) {
        return verificationResultRepository.findById(id).map(result -> {
            List<ScoredCandidate> scored = result.getMatches().stream()
                    .map(m -> new ScoredCandidate(m.getTrustedArticle(), m.getMatchScore(), m.getMatchType()))
                    .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                    .toList();
            List<NewsArticleDTO> similar = fetchSimilarNews(result.getMatchedCategory());
            return toDTO(result, scored, similar);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total",             verificationResultRepository.countTotal());
        stats.put("likelyTrue",        verificationResultRepository.countByVerdict(Verdict.LIKELY_TRUE));
        stats.put("likelyFalse",       verificationResultRepository.countByVerdict(Verdict.LIKELY_FALSE));
        stats.put("needsVerification", verificationResultRepository.countByVerdict(Verdict.NEEDS_VERIFICATION));
        return stats;
    }

    // ── Step 1: Text preparation ──────────────────────────────────

    private String prepareText(VerificationRequestDTO request) {
        String text = StringUtils.defaultIfBlank(request.getContent(), "");
        if (request.getSubmissionType() == SubmissionType.URL
                && StringUtils.isNotBlank(request.getUrl()) && text.isEmpty()) {
            text = "Article from: " + request.getUrl();
        }
        return text.trim();
    }

    // ── Step 2: Keyword extraction ────────────────────────────────

    List<String> extractKeywords(String text) {
        if (StringUtils.isBlank(text)) return Collections.emptyList();
        String normalized = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s'-]", " ")
                .replaceAll("\\s+", " ").trim();
        String[] tokens = normalized.split("\\s+");
        Set<String> seen = new LinkedHashSet<>();
        for (String token : tokens) {
            String clean = token.replaceAll("^['-]+|['-]+$", "");
            if (clean.length() >= MIN_KEYWORD_LENGTH && !STOP_WORDS.contains(clean))
                seen.add(clean);
        }
        for (int i = 0; i < tokens.length - 1; i++) {
            String bigram = tokens[i] + " " + tokens[i + 1];
            if (bigram.length() >= 8) seen.add(bigram);
        }
        return new ArrayList<>(seen).stream().limit(30).toList();
    }

    // ── Step 3: Category detection ────────────────────────────────

    NewsCategory detectCategory(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        Map<NewsCategory, Integer> scores = new EnumMap<>(NewsCategory.class);
        for (Map.Entry<NewsCategory, List<String>> entry : CATEGORY_SIGNALS.entrySet()) {
            int score = 0;
            for (String signal : entry.getValue()) {
                if (lower.contains(signal)) {
                    score += lower.indexOf(signal) < 200 ? 2 : 1;
                }
            }
            scores.put(entry.getKey(), score);
        }
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse(NewsCategory.INTERNATIONAL);
    }

    // ── Step 4: Candidate retrieval ───────────────────────────────

    private List<TrustedArticle> findCandidates(List<String> keywords, NewsCategory category) {
        if (keywords.isEmpty())
            return trustedArticleRepository.findTop5ByCategoryAndVerifiedTrueOrderByCredibilityScoreDesc(category);

        Set<TrustedArticle> candidates = new LinkedHashSet<>();
        List<String> topKw = keywords.stream().filter(k -> k.length() > MIN_KEYWORD_LENGTH).limit(5).toList();

        for (String kw : topKw) {
            candidates.addAll(trustedArticleRepository.searchByTermAndCategory(kw, category));
            if (candidates.size() >= MAX_CANDIDATE_ARTICLES) break;
        }
        if (candidates.size() < 3) {
            String k1 = topKw.size() > 0 ? topKw.get(0) : "";
            String k2 = topKw.size() > 1 ? topKw.get(1) : k1;
            String k3 = topKw.size() > 2 ? topKw.get(2) : k2;
            candidates.addAll(trustedArticleRepository.searchByMultipleTerms(
                    k1, k2, k3, PageRequest.of(0, MAX_CANDIDATE_ARTICLES)));
        }
        return new ArrayList<>(candidates).stream().limit(MAX_CANDIDATE_ARTICLES).toList();
    }

    // ── Step 5: Scoring ───────────────────────────────────────────

    List<ScoredCandidate> scoreCandidates(List<TrustedArticle> candidates,
                                           List<String> keywords, String submittedText) {
        if (candidates.isEmpty()) return Collections.emptyList();
        String lowerSubmitted = submittedText.toLowerCase();
        return candidates.stream().map(article -> {
            double raw = computeSimilarityScore(article, keywords, lowerSubmitted);
            double bonus = article.getCredibilityScore() * 0.10 * 0.5;
            double final_ = Math.min(100.0, raw + bonus);
            return new ScoredCandidate(article, (int) Math.round(final_), determineMatchType(final_));
        })
        .filter(sc -> sc.score() > 5)
        .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
        .limit(TOP_MATCHES_TO_SAVE)
        .toList();
    }

    private double computeSimilarityScore(TrustedArticle article, List<String> keywords, String lowerSubmitted) {
        if (keywords.isEmpty()) return 10.0;
        String titleLower   = article.getTitle().toLowerCase();
        String contentLower = article.getContent().toLowerCase();
        String kwStr        = StringUtils.defaultString(article.getKeywords()).toLowerCase();
        int matchCount = 0, titleMatches = 0, contentMatches = 0;
        for (String kw : keywords) {
            boolean inTitle   = titleLower.contains(kw);
            boolean inContent = contentLower.contains(kw);
            if (inTitle)   titleMatches++;
            if (inContent) contentMatches++;
            if (inTitle || inContent || kwStr.contains(kw)) matchCount++;
        }
        double base    = (double) matchCount / keywords.size() * 60.0;
        double tBonus  = titleMatches   * 4.0;
        double cBonus  = contentMatches * 1.5;
        long shared    = Arrays.stream(titleLower.split("\\s+"))
                .filter(w -> w.length() > 4 && lowerSubmitted.contains(w)).count();
        double overlap = Math.min(15.0, shared);
        return base + tBonus + cBonus + overlap;
    }

    private String determineMatchType(double score) {
        if (score >= 75) return "EXACT";
        if (score >= 45) return "SIMILAR";
        return "RELATED";
    }

    // ── Step 6: Verdict ───────────────────────────────────────────

    private record VerdictResult(Verdict verdict, int confidence, String explanation) {}

    private VerdictResult deriveVerdict(List<ScoredCandidate> scored, int keywordCount) {
        if (scored.isEmpty()) {
            return new VerdictResult(Verdict.NEEDS_VERIFICATION, 25,
                "No matching articles were found in the trusted reference database. "
                + "The content could not be cross-referenced. "
                + "We recommend consulting reputable news outlets directly.");
        }
        int topScore = scored.get(0).score();
        double avg   = scored.stream().mapToInt(ScoredCandidate::score).average().orElse(0);
        int confidence = computeConfidence(topScore, avg, scored.size());

        if (topScore >= HIGH_CONFIDENCE_THRESHOLD) {
            return new VerdictResult(Verdict.LIKELY_TRUE, confidence, buildTrueExplanation(scored, confidence));
        }
        if (topScore >= MEDIUM_CONFIDENCE_THRESHOLD) {
            boolean multi = scored.stream().filter(sc -> sc.score() >= 35).count() >= 2;
            if (multi) return new VerdictResult(Verdict.LIKELY_TRUE, confidence, buildPartialTrueExplanation(scored, confidence));
            return new VerdictResult(Verdict.NEEDS_VERIFICATION, confidence,
                String.format("Partial matches were found in %d trusted source(s), but the overlap "
                    + "is insufficient to confidently verify the claim (confidence: %d%%). "
                    + "The content shares some elements with known reporting but may include "
                    + "inaccuracies or missing context.", scored.size(), confidence));
        }
        if (keywordCount >= 5 && topScore < 25) {
            return new VerdictResult(Verdict.LIKELY_FALSE, Math.max(20, 100 - confidence),
                String.format("The submitted content contains %d identifiable claims, but none align "
                    + "with reporting from trusted sources. Content with specific claims that find "
                    + "no corroboration in credible sources is a strong indicator of misinformation.",
                    keywordCount));
        }
        return new VerdictResult(Verdict.NEEDS_VERIFICATION, Math.max(15, confidence),
            "The submitted content could only be weakly matched to trusted sources. "
            + "This may be a very recent event or a niche topic not yet in our database.");
    }

    private int computeConfidence(int topScore, double avg, int count) {
        return (int) Math.min(98, Math.max(5, Math.round(topScore * 0.6 + avg * 0.25 + count * 2.5)));
    }

    private String buildTrueExplanation(List<ScoredCandidate> scored, int confidence) {
        String sources = scored.stream().limit(3).map(sc -> sc.article().getSourceName())
                .distinct().collect(Collectors.joining(", "));
        return String.format("The submitted content strongly aligns with verified reporting from %s "
            + "and %d other trusted source(s). Key claims match with a confidence score of %d%%. "
            + "The topic, facts, and context are consistent with established journalism.",
            sources, Math.max(0, scored.size() - 1), confidence);
    }

    private String buildPartialTrueExplanation(List<ScoredCandidate> scored, int confidence) {
        long count = scored.stream().filter(sc -> sc.score() >= 35).count();
        return String.format("The submitted content is corroborated by %d trusted source(s) "
            + "with a confidence score of %d%%. Multiple credible publishers cover the same "
            + "topic with consistent reporting.", count, confidence);
    }

    // ── Step 7: Entity build ──────────────────────────────────────

    private VerificationResult buildEntity(VerificationRequestDTO request, String text,
                                            String ip, VerdictResult vr, NewsCategory category,
                                            List<ScoredCandidate> scored) {
        VerificationResult entity = new VerificationResult();
        entity.setSubmittedText(text);
        entity.setSubmittedUrl(request.getUrl());
        entity.setSubmissionType(request.getSubmissionType());
        entity.setVerdict(vr.verdict());
        entity.setConfidenceScore(vr.confidence());
        entity.setExplanation(vr.explanation());
        entity.setMatchedCategory(category);
        entity.setAnalysisDetails(buildAnalysisJson(scored, category));
        entity.setIpAddress(ip);

        List<VerificationMatch> matches = new ArrayList<>();
        for (ScoredCandidate sc : scored) {
            VerificationMatch m = new VerificationMatch(entity, sc.article(), sc.score(), sc.matchType());
            matches.add(m);
        }
        entity.setMatches(matches);
        return entity;
    }

    private String buildAnalysisJson(List<ScoredCandidate> scored, NewsCategory category) {
        String sources = scored.stream()
                .map(sc -> "\"" + sc.article().getSourceName() + "\"")
                .collect(Collectors.joining(", "));
        return String.format("{\"detectedCategory\":\"%s\",\"candidateCount\":%d,\"topScore\":%d,\"sources\":[%s]}",
                category, scored.size(), scored.isEmpty() ? 0 : scored.get(0).score(), sources);
    }

    // ── Step 8: Similar news ──────────────────────────────────────

    private List<NewsArticleDTO> fetchSimilarNews(NewsCategory category) {
        if (category == null) return Collections.emptyList();
        return newsArticleRepository.findTop6ByCategoryOrderByPublishedDateDesc(category)
                .stream().map(newsService::toDTO).toList();
    }

    // ── DTO mapping ───────────────────────────────────────────────

    private VerificationResultDTO toDTO(VerificationResult entity,
                                         List<ScoredCandidate> scored,
                                         List<NewsArticleDTO> similarNews) {
        List<VerificationResultDTO.MatchedArticleDTO> matchedDTOs = scored.stream().map(sc -> {
            VerificationResultDTO.MatchedArticleDTO m = new VerificationResultDTO.MatchedArticleDTO();
            m.setTrustedArticleId(sc.article().getId());
            m.setTitle(sc.article().getTitle());
            m.setSummary(sc.article().getShortSummary());
            m.setSourceName(sc.article().getSourceName());
            m.setSourceUrl(sc.article().getSourceUrl());
            m.setAuthor(sc.article().getAuthor());
            m.setPublishedDate(sc.article().getPublishedDate());
            m.setMatchScore(sc.score());
            m.setMatchType(sc.matchType());
            m.setMatchBadgeClass(badgeClass(sc.score()));
            m.setCredibilityScore(sc.article().getCredibilityScore());
            m.setCategory(sc.article().getCategory());
            m.setCategoryDisplayName(sc.article().getCategory().getDisplayName());
            return m;
        }).toList();

        VerificationResultDTO dto = new VerificationResultDTO();
        dto.setId(entity.getId());
        dto.setSubmittedText(entity.getSubmittedText());
        dto.setSubmittedUrl(entity.getSubmittedUrl());
        dto.setSubmissionType(entity.getSubmissionType());
        dto.setVerdict(entity.getVerdict());
        dto.setVerdictDisplayName(entity.getVerdict().getDisplayName());
        dto.setVerdictCssClass(entity.getVerdict().getCssClass());
        dto.setVerdictIcon(entity.getVerdict().getIcon());
        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setConfidenceLabel(entity.getConfidenceLabel());
        dto.setExplanation(entity.getExplanation());
        dto.setMatchedCategory(entity.getMatchedCategory());
        dto.setMatchedCategoryDisplayName(entity.getMatchedCategory() != null
                ? entity.getMatchedCategory().getDisplayName() : "Unknown");
        dto.setMatchedArticles(matchedDTOs);
        dto.setSimilarNews(similarNews);
        dto.setAnalysisDetails(entity.getAnalysisDetails());
        dto.setAnalyzedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        return dto;
    }

    private String badgeClass(int score) {
        if (score >= 75) return "bg-success";
        if (score >= 45) return "bg-warning text-dark";
        return "bg-secondary";
    }

    // ── Internal record ───────────────────────────────────────────
    record ScoredCandidate(TrustedArticle article, int score, String matchType) {}
}
