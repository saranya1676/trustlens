package com.truthlens.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verification_results",
    indexes = {
        @Index(name = "idx_verdict",    columnList = "verdict"),
        @Index(name = "idx_vr_created", columnList = "created_at")
    })
public class VerificationResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(name = "submitted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String submittedText;

    @Column(name = "submitted_url", length = 1000)
    private String submittedUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 20)
    private SubmissionType submissionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private Verdict verdict;

    @Column(name = "confidence_score", nullable = false)
    private int confidenceScore;

    @NotBlank @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "matched_category", length = 50)
    private NewsCategory matchedCategory;

    @Column(name = "analysis_details", columnDefinition = "JSON")
    private String analysisDetails;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "verificationResult", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VerificationMatch> matches = new ArrayList<>();

    public VerificationResult() {}

    @Transient
    public boolean hasMatches() { return matches != null && !matches.isEmpty(); }

    @Transient
    public String getConfidenceLabel() {
        if (confidenceScore >= 80) return "High";
        if (confidenceScore >= 50) return "Medium";
        return "Low";
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long getId()                      { return id; }
    public String getSubmittedText()         { return submittedText; }
    public String getSubmittedUrl()          { return submittedUrl; }
    public SubmissionType getSubmissionType(){ return submissionType; }
    public Verdict getVerdict()              { return verdict; }
    public int getConfidenceScore()          { return confidenceScore; }
    public String getExplanation()           { return explanation; }
    public NewsCategory getMatchedCategory() { return matchedCategory; }
    public String getAnalysisDetails()       { return analysisDetails; }
    public String getIpAddress()             { return ipAddress; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public List<VerificationMatch> getMatches() { return matches; }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(Long id)                              { this.id = id; }
    public void setSubmittedText(String t)                  { this.submittedText = t; }
    public void setSubmittedUrl(String u)                   { this.submittedUrl = u; }
    public void setSubmissionType(SubmissionType t)         { this.submissionType = t; }
    public void setVerdict(Verdict v)                       { this.verdict = v; }
    public void setConfidenceScore(int s)                   { this.confidenceScore = s; }
    public void setExplanation(String e)                    { this.explanation = e; }
    public void setMatchedCategory(NewsCategory c)          { this.matchedCategory = c; }
    public void setAnalysisDetails(String a)                { this.analysisDetails = a; }
    public void setIpAddress(String ip)                     { this.ipAddress = ip; }
    public void setCreatedAt(LocalDateTime t)               { this.createdAt = t; }
    public void setMatches(List<VerificationMatch> matches) { this.matches = matches; }
}
