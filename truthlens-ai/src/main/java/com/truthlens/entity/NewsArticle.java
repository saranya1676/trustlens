package com.truthlens.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_articles",
    indexes = {
        @Index(name = "idx_category",  columnList = "category"),
        @Index(name = "idx_published", columnList = "published_date")
    })
public class NewsArticle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(name = "title", nullable = false, length = 500)
    private String title;

    @NotBlank @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

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

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NewsArticle() {}

    @Transient
    public String getShortSummary() {
        String s = (summary != null && !summary.isEmpty()) ? summary : content;
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    public void incrementViewCount() { this.viewCount++; }

    // ── Getters ───────────────────────────────────────────────────
    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public String getContent()           { return content; }
    public String getSummary()           { return summary; }
    public String getSourceName()        { return sourceName; }
    public String getSourceUrl()         { return sourceUrl; }
    public String getImageUrl()          { return imageUrl; }
    public NewsCategory getCategory()    { return category; }
    public String getAuthor()            { return author; }
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public String getKeywords()          { return keywords; }
    public int getViewCount()            { return viewCount; }
    public boolean isFeatured()          { return featured; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(Long id)                          { this.id = id; }
    public void setTitle(String title)                  { this.title = title; }
    public void setContent(String content)              { this.content = content; }
    public void setSummary(String summary)              { this.summary = summary; }
    public void setSourceName(String sourceName)        { this.sourceName = sourceName; }
    public void setSourceUrl(String sourceUrl)          { this.sourceUrl = sourceUrl; }
    public void setImageUrl(String imageUrl)            { this.imageUrl = imageUrl; }
    public void setCategory(NewsCategory category)      { this.category = category; }
    public void setAuthor(String author)                { this.author = author; }
    public void setPublishedDate(LocalDateTime d)       { this.publishedDate = d; }
    public void setKeywords(String keywords)            { this.keywords = keywords; }
    public void setViewCount(int viewCount)             { this.viewCount = viewCount; }
    public void setFeatured(boolean featured)           { this.featured = featured; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }
}
