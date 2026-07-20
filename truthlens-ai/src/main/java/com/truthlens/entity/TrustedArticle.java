package com.truthlens.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "trusted_articles",
    indexes = { @Index(name = "idx_ta_category", columnList = "category") })
public class TrustedArticle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(name = "title", nullable = false, length = 500)
    private String title;

    @NotBlank @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @NotBlank @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private NewsCategory category;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "credibility_score", nullable = false)
    private int credibilityScore = 90;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TrustedArticle() {}

    @Transient
    public List<String> getKeywordList() {
        if (keywords == null || keywords.isBlank()) return Collections.emptyList();
        return Arrays.stream(keywords.split(","))
                .map(String::trim).filter(k -> !k.isEmpty()).toList();
    }

    @Transient
    public String getShortSummary() {
        String s = (summary != null && !summary.isEmpty()) ? summary : content;
        if (s == null) return "";
        return s.length() > 250 ? s.substring(0, 250) + "..." : s;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public String getContent()           { return content; }
    public String getSummary()           { return summary; }
    public String getSourceName()        { return sourceName; }
    public String getSourceUrl()         { return sourceUrl; }
    public NewsCategory getCategory()    { return category; }
    public String getAuthor()            { return author; }
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public String getKeywords()          { return keywords; }
    public int getCredibilityScore()     { return credibilityScore; }
    public boolean isVerified()          { return verified; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(Long id)                          { this.id = id; }
    public void setTitle(String title)                  { this.title = title; }
    public void setContent(String content)              { this.content = content; }
    public void setSummary(String summary)              { this.summary = summary; }
    public void setSourceName(String sourceName)        { this.sourceName = sourceName; }
    public void setSourceUrl(String sourceUrl)          { this.sourceUrl = sourceUrl; }
    public void setCategory(NewsCategory category)      { this.category = category; }
    public void setAuthor(String author)                { this.author = author; }
    public void setPublishedDate(LocalDateTime d)       { this.publishedDate = d; }
    public void setKeywords(String keywords)            { this.keywords = keywords; }
    public void setCredibilityScore(int s)              { this.credibilityScore = s; }
    public void setVerified(boolean verified)           { this.verified = verified; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)           { this.updatedAt = t; }
}
