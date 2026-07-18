package com.truthlens.dto;

import com.truthlens.entity.NewsCategory;
import java.time.LocalDateTime;

public class NewsArticleDTO {
    private Long id;
    private String title;
    private String summary;
    private String shortSummary;
    private String sourceName;
    private String sourceUrl;
    private String imageUrl;
    private NewsCategory category;
    private String categoryDisplayName;
    private String categoryBadgeClass;
    private String categoryIcon;
    private String author;
    private LocalDateTime publishedDate;
    private String keywords;
    private int viewCount;
    private boolean featured;
    private LocalDateTime createdAt;

    public NewsArticleDTO() {}

    // ── Getters ───────────────────────────────────────────────────
    public Long getId()                     { return id; }
    public String getTitle()                { return title; }
    public String getSummary()              { return summary; }
    public String getShortSummary()         { return shortSummary; }
    public String getSourceName()           { return sourceName; }
    public String getSourceUrl()            { return sourceUrl; }
    public String getImageUrl()             { return imageUrl; }
    public NewsCategory getCategory()       { return category; }
    public String getCategoryDisplayName()  { return categoryDisplayName; }
    public String getCategoryBadgeClass()   { return categoryBadgeClass; }
    public String getCategoryIcon()         { return categoryIcon; }
    public String getAuthor()               { return author; }
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public String getKeywords()             { return keywords; }
    public int getViewCount()               { return viewCount; }
    public boolean isFeatured()             { return featured; }
    public LocalDateTime getCreatedAt()     { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(Long id)                              { this.id = id; }
    public void setTitle(String title)                      { this.title = title; }
    public void setSummary(String summary)                  { this.summary = summary; }
    public void setShortSummary(String shortSummary)        { this.shortSummary = shortSummary; }
    public void setSourceName(String sourceName)            { this.sourceName = sourceName; }
    public void setSourceUrl(String sourceUrl)              { this.sourceUrl = sourceUrl; }
    public void setImageUrl(String imageUrl)                { this.imageUrl = imageUrl; }
    public void setCategory(NewsCategory category)          { this.category = category; }
    public void setCategoryDisplayName(String name)         { this.categoryDisplayName = name; }
    public void setCategoryBadgeClass(String cls)           { this.categoryBadgeClass = cls; }
    public void setCategoryIcon(String icon)                { this.categoryIcon = icon; }
    public void setAuthor(String author)                    { this.author = author; }
    public void setPublishedDate(LocalDateTime d)           { this.publishedDate = d; }
    public void setKeywords(String keywords)                { this.keywords = keywords; }
    public void setViewCount(int viewCount)                 { this.viewCount = viewCount; }
    public void setFeatured(boolean featured)               { this.featured = featured; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }
}
