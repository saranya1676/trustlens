package com.truthlens.repository;

import com.truthlens.entity.VerificationMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for VerificationMatch — the join table linking a
 * VerificationResult to matched TrustedArticle records.
 */
@Repository
public interface VerificationMatchRepository extends JpaRepository<VerificationMatch, Long> {

    /**
     * Retrieves all matches for a given verification result,
     * ordered by match score descending (best match first).
     */
    @Query("""
        SELECT m FROM VerificationMatch m
        WHERE m.verificationResult.id = :resultId
        ORDER BY m.matchScore DESC
    """)
    List<VerificationMatch> findByVerificationResultIdOrderByMatchScoreDesc(
            @Param("resultId") Long resultId);

    /**
     * Deletes all matches for a verification result (used if re-running analysis).
     */
    void deleteByVerificationResultId(Long verificationResultId);

    /**
     * Counts how many times a trusted article has been matched overall.
     * Useful for surfacing frequently corroborated sources.
     */
    long countByTrustedArticleId(Long trustedArticleId);
}
