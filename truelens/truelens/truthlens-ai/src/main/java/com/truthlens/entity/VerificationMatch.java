package com.truthlens.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "verification_matches")
public class VerificationMatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_result_id", nullable = false)
    private VerificationResult verificationResult;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trusted_article_id", nullable = false)
    private TrustedArticle trustedArticle;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;

    public VerificationMatch() {}

    public VerificationMatch(VerificationResult result, TrustedArticle article,
                              int score, String type) {
        this.verificationResult = result;
        this.trustedArticle = article;
        this.matchScore = score;
        this.matchType = type;
    }

    @Transient
    public boolean isHighConfidence() { return matchScore >= 75; }

    @Transient
    public String getMatchBadgeClass() {
        if (matchScore >= 75) return "bg-success";
        if (matchScore >= 45) return "bg-warning text-dark";
        return "bg-secondary";
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public Long getId()                             { return id; }
    public VerificationResult getVerificationResult(){ return verificationResult; }
    public TrustedArticle getTrustedArticle()        { return trustedArticle; }
    public int getMatchScore()                       { return matchScore; }
    public String getMatchType()                     { return matchType; }

    public void setId(Long id)                                    { this.id = id; }
    public void setVerificationResult(VerificationResult r)       { this.verificationResult = r; }
    public void setTrustedArticle(TrustedArticle a)               { this.trustedArticle = a; }
    public void setMatchScore(int s)                              { this.matchScore = s; }
    public void setMatchType(String t)                            { this.matchType = t; }
}
