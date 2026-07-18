package com.truthlens.repository;

import com.truthlens.entity.NewsArticle;
import com.truthlens.entity.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for browsable NewsArticle entities.
 */
@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // ── Category queries ──────────────────────────────────────────────────────

    Page<NewsArticle> findByCategoryOrderByPublishedDateDesc(NewsCategory category, Pageable pageable);

    List<NewsArticle> findTop6ByCategoryOrderByPublishedDateDesc(NewsCategory category);

    long countByCategory(NewsCategory category);

    // ── Featured articles ─────────────────────────────────────────────────────

    List<NewsArticle> findByFeaturedTrueOrderByPublishedDateDesc();

    List<NewsArticle> findTop5ByFeaturedTrueOrderByPublishedDateDesc();

    // ── Latest articles ───────────────────────────────────────────────────────

    List<NewsArticle> findTop10ByOrderByPublishedDateDesc();

    Page<NewsArticle> findAllByOrderByPublishedDateDesc(Pageable pageable);

    // ── Full-text keyword search ──────────────────────────────────────────────

    @Query("""
        SELECT a FROM NewsArticle a
        WHERE LOWER(a.title)   LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(a.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY a.publishedDate DESC
    """)
    Page<NewsArticle> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT a FROM NewsArticle a
        WHERE a.category = :category
          AND (  LOWER(a.title)    LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.content)  LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.summary)  LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        ORDER BY a.publishedDate DESC
    """)
    Page<NewsArticle> searchByCategoryAndKeyword(
            @Param("category") NewsCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ── Similar articles (by category, excluding a given id) ─────────────────

    @Query("""
        SELECT a FROM NewsArticle a
        WHERE a.category = :category
          AND a.id <> :excludeId
        ORDER BY a.publishedDate DESC
    """)
    List<NewsArticle> findSimilarByCategory(
            @Param("category") NewsCategory category,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    List<NewsArticle> findTop4ByCategoryAndIdNotOrderByPublishedDateDesc(
            NewsCategory category, Long excludeId);

    // ── View count increment ──────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE NewsArticle a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // ── Most popular ──────────────────────────────────────────────────────────

    List<NewsArticle> findTop5ByOrderByViewCountDesc();
}
