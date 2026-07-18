package com.truthlens.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponseDTO {

    private String answer;
    private String intent;          // ARTICLE_SEARCH, VERIFICATION_HELP, CATEGORY_INFO, GREETING, UNKNOWN
    private List<ArticleRef> relatedArticles;
    private int confidence;         // 0-100
    private LocalDateTime timestamp;

    public ChatResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────
    public String getAnswer()                   { return answer; }
    public String getIntent()                   { return intent; }
    public List<ArticleRef> getRelatedArticles(){ return relatedArticles; }
    public int getConfidence()                  { return confidence; }
    public LocalDateTime getTimestamp()         { return timestamp; }

    // ── Setters ───────────────────────────────────────────────────
    public void setAnswer(String answer)                        { this.answer = answer; }
    public void setIntent(String intent)                        { this.intent = intent; }
    public void setRelatedArticles(List<ArticleRef> articles)   { this.relatedArticles = articles; }
    public void setConfidence(int confidence)                   { this.confidence = confidence; }
    public void setTimestamp(LocalDateTime timestamp)           { this.timestamp = timestamp; }

    // ── Inner class: slim article reference for chat UI ──────────
    public static class ArticleRef {
        private Long id;
        private String title;
        private String category;
        private String categoryDisplayName;
        private String sourceName;
        private String snippet;
        private int relevanceScore;

        public ArticleRef() {}

        public Long getId()                     { return id; }
        public String getTitle()                { return title; }
        public String getCategory()             { return category; }
        public String getCategoryDisplayName()  { return categoryDisplayName; }
        public String getSourceName()           { return sourceName; }
        public String getSnippet()              { return snippet; }
        public int getRelevanceScore()          { return relevanceScore; }

        public void setId(Long id)                          { this.id = id; }
        public void setTitle(String title)                  { this.title = title; }
        public void setCategory(String category)            { this.category = category; }
        public void setCategoryDisplayName(String name)     { this.categoryDisplayName = name; }
        public void setSourceName(String sourceName)        { this.sourceName = sourceName; }
        public void setSnippet(String snippet)              { this.snippet = snippet; }
        public void setRelevanceScore(int score)            { this.relevanceScore = score; }
    }
}
