package com.truthlens.dto;

import com.truthlens.entity.NewsCategory;
import com.truthlens.entity.SubmissionType;
import com.truthlens.entity.Verdict;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VerificationResultDTO {

    private Long id;
    private String submittedText;
    private String submittedUrl;
    private SubmissionType submissionType;
    private Verdict verdict;
    private String verdictDisplayName;
    private String verdictCssClass;
    private String verdictIcon;
    private int confidenceScore;
    private String confidenceLabel;
    private String explanation;
    private NewsCategory matchedCategory;
    private String matchedCategoryDisplayName;
    private List<MatchedArticleDTO> matchedArticles = new ArrayList<>();
    private List<NewsArticleDTO> similarNews = new ArrayList<>();
    private String analysisDetails;
    private LocalDateTime analyzedAt;

    public VerificationResultDTO() {}

    // ── Getters ───────────────────────────────────────────────────
    public Long getId()                         { return id; }
    public String getSubmittedText()            { return submittedText; }
    public String getSubmittedUrl()             { return submittedUrl; }
    public SubmissionType getSubmissionType()   { return submissionType; }
    public Verdict getVerdict()                 { return verdict; }
    public String getVerdictDisplayName()       { return verdictDisplayName; }
    public String getVerdictCssClass()          { return verdictCssClass; }
    public String getVerdictIcon()              { return verdictIcon; }
    public int getConfidenceScore()             { return confidenceScore; }
    public String getConfidenceLabel()          { return confidenceLabel; }
    public String getExplanation()              { return explanation; }
    public NewsCategory getMatchedCategory()    { return matchedCategory; }
    public String getMatchedCategoryDisplayName(){ return matchedCategoryDisplayName; }
    public List<MatchedArticleDTO> getMatchedArticles() { return matchedArticles; }
    public List<NewsArticleDTO> getSimilarNews()        { return similarNews; }
    public String getAnalysisDetails()          { return analysisDetails; }
    public LocalDateTime getAnalyzedAt()        { return analyzedAt; }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(Long id)                                      { this.id = id; }
    public void setSubmittedText(String t)                          { this.submittedText = t; }
    public void setSubmittedUrl(String u)                           { this.submittedUrl = u; }
    public void setSubmissionType(SubmissionType t)                 { this.submissionType = t; }
    public void setVerdict(Verdict v)                               { this.verdict = v; }
    public void setVerdictDisplayName(String n)                     { this.verdictDisplayName = n; }
    public void setVerdictCssClass(String c)                        { this.verdictCssClass = c; }
    public void setVerdictIcon(String i)                            { this.verdictIcon = i; }
    public void setConfidenceScore(int s)                           { this.confidenceScore = s; }
    public void setConfidenceLabel(String l)                        { this.confidenceLabel = l; }
    public void setExplanation(String e)                            { this.explanation = e; }
    public void setMatchedCategory(NewsCategory c)                  { this.matchedCategory = c; }
    public void setMatchedCategoryDisplayName(String n)             { this.matchedCategoryDisplayName = n; }
    public void setMatchedArticles(List<MatchedArticleDTO> list)    { this.matchedArticles = list; }
    public void setSimilarNews(List<NewsArticleDTO> list)           { this.similarNews = list; }
    public void setAnalysisDetails(String a)                        { this.analysisDetails = a; }
    public void setAnalyzedAt(LocalDateTime t)                      { this.analyzedAt = t; }

    // ── Inner DTO ─────────────────────────────────────────────────
    public static class MatchedArticleDTO {
        private Long trustedArticleId;
        private String title;
        private String summary;
        private String sourceName;
        private String sourceUrl;
        private String author;
        private LocalDateTime publishedDate;
        private int matchScore;
        private String matchType;
        private String matchBadgeClass;
        private int credibilityScore;
        private NewsCategory category;
        private String categoryDisplayName;

        public MatchedArticleDTO() {}

        public Long getTrustedArticleId()       { return trustedArticleId; }
        public String getTitle()                { return title; }
        public String getSummary()              { return summary; }
        public String getSourceName()           { return sourceName; }
        public String getSourceUrl()            { return sourceUrl; }
        public String getAuthor()               { return author; }
        public LocalDateTime getPublishedDate() { return publishedDate; }
        public int getMatchScore()              { return matchScore; }
        public String getMatchType()            { return matchType; }
        public String getMatchBadgeClass()      { return matchBadgeClass; }
        public int getCredibilityScore()        { return credibilityScore; }
        public NewsCategory getCategory()       { return category; }
        public String getCategoryDisplayName()  { return categoryDisplayName; }

        public void setTrustedArticleId(Long id)        { this.trustedArticleId = id; }
        public void setTitle(String title)              { this.title = title; }
        public void setSummary(String summary)          { this.summary = summary; }
        public void setSourceName(String n)             { this.sourceName = n; }
        public void setSourceUrl(String u)              { this.sourceUrl = u; }
        public void setAuthor(String a)                 { this.author = a; }
        public void setPublishedDate(LocalDateTime d)   { this.publishedDate = d; }
        public void setMatchScore(int s)                { this.matchScore = s; }
        public void setMatchType(String t)              { this.matchType = t; }
        public void setMatchBadgeClass(String c)        { this.matchBadgeClass = c; }
        public void setCredibilityScore(int s)          { this.credibilityScore = s; }
        public void setCategory(NewsCategory c)         { this.category = c; }
        public void setCategoryDisplayName(String n)    { this.categoryDisplayName = n; }
    }
}
