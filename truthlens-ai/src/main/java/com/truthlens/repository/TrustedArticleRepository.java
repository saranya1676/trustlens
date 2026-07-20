package com.truthlens.repository;

import com.truthlens.entity.NewsCategory;
import com.truthlens.entity.TrustedArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TrustedArticle — the verified reference corpus used by
 * the verification engine to compare submitted news content against.
 *
 * Future extension point: these queries will be complemented by external
 * API calls (NewsAPI, Google Fact Check Tools API, etc.) when API keys
 * are configured in application.properties.
 */
@Repository
public interface TrustedArticleRepository extends JpaRepository<TrustedArticle, Long> {

    // ── Category queries ──────────────────────────────────────────────────────

    List<TrustedArticle> findByCategoryAndVerifiedTrue(NewsCategory category);

    List<TrustedArticle> findTop5ByCategoryAndVerifiedTrueOrderByCredibilityScoreDesc(
            NewsCategory category);

    long countByCategoryAndVerifiedTrue(NewsCategory category);

    // ── Keyword-based search for verification engine ──────────────────────────

    /**
     * Searches across title, content, summary, and keywords fields.
     * Used by the verification engine to find candidate matches.
     */
    @Query("""
        SELECT t FROM TrustedArticle t
        WHERE t.verified = true
          AND (  LOWER(t.title)    LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.content)  LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.summary)  LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.keywords) LIKE LOWER(CONCAT('%', :term, '%'))
              )
        ORDER BY t.credibilityScore DESC
    """)
    List<TrustedArticle> searchByTerm(@Param("term") String term);

    /**
     * Searches within a specific category — narrows verification scope
     * when a category is already detected from the submitted text.
     */
    @Query("""
        SELECT t FROM TrustedArticle t
        WHERE t.verified = true
          AND t.category = :category
          AND (  LOWER(t.title)    LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.content)  LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.summary)  LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(t.keywords) LIKE LOWER(CONCAT('%', :term, '%'))
              )
        ORDER BY t.credibilityScore DESC
    """)
    List<TrustedArticle> searchByTermAndCategory(
            @Param("term") String term,
            @Param("category") NewsCategory category);

    /**
     * Multi-term search: tries each keyword independently and returns
     * all distinct matches, ordered by credibility.
     */
    @Query("""
        SELECT DISTINCT t FROM TrustedArticle t
        WHERE t.verified = true
          AND (  LOWER(t.title)    LIKE LOWER(CONCAT('%', :k1, '%'))
              OR LOWER(t.title)    LIKE LOWER(CONCAT('%', :k2, '%'))
              OR LOWER(t.title)    LIKE LOWER(CONCAT('%', :k3, '%'))
              OR LOWER(t.content)  LIKE LOWER(CONCAT('%', :k1, '%'))
              OR LOWER(t.content)  LIKE LOWER(CONCAT('%', :k2, '%'))
              OR LOWER(t.content)  LIKE LOWER(CONCAT('%', :k3, '%'))
              OR LOWER(t.keywords) LIKE LOWER(CONCAT('%', :k1, '%'))
              OR LOWER(t.keywords) LIKE LOWER(CONCAT('%', :k2, '%'))
              OR LOWER(t.keywords) LIKE LOWER(CONCAT('%', :k3, '%'))
              )
        ORDER BY t.credibilityScore DESC
    """)
    List<TrustedArticle> searchByMultipleTerms(
            @Param("k1") String k1,
            @Param("k2") String k2,
            @Param("k3") String k3,
            Pageable pageable);

    // ── Source-based queries ──────────────────────────────────────────────────

    List<TrustedArticle> findBySourceNameAndVerifiedTrue(String sourceName);

    // ── All verified, sorted by credibility ──────────────────────────────────

    List<TrustedArticle> findByVerifiedTrueOrderByCredibilityScoreDesc();

    List<TrustedArticle> findTop20ByVerifiedTrueOrderByCredibilityScoreDesc();
}
