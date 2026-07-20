package com.truthlens.repository;

import com.truthlens.entity.Verdict;
import com.truthlens.entity.VerificationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for VerificationResult — stores every verification request
 * and its outcome for history, analytics, and re-display.
 */
@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {

    // ── Pagination ────────────────────────────────────────────────────────────

    Page<VerificationResult> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ── Verdict-based queries ─────────────────────────────────────────────────

    List<VerificationResult> findByVerdictOrderByCreatedAtDesc(Verdict verdict);

    long countByVerdict(Verdict verdict);

    // ── Recent results ────────────────────────────────────────────────────────

    List<VerificationResult> findTop10ByOrderByCreatedAtDesc();

    // ── Statistics for dashboard ──────────────────────────────────────────────

    @Query("""
        SELECT v.verdict, COUNT(v)
        FROM VerificationResult v
        GROUP BY v.verdict
    """)
    List<Object[]> countGroupedByVerdict();

    @Query("""
        SELECT AVG(v.confidenceScore)
        FROM VerificationResult v
    """)
    Double findAverageConfidenceScore();

    // ── Date-range queries ────────────────────────────────────────────────────

    @Query("""
        SELECT v FROM VerificationResult v
        WHERE v.createdAt BETWEEN :from AND :to
        ORDER BY v.createdAt DESC
    """)
    List<VerificationResult> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    // ── Total count ───────────────────────────────────────────────────────────

    @Query("SELECT COUNT(v) FROM VerificationResult v")
    long countTotal();
}
