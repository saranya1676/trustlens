package com.truthlens.controller;

import com.truthlens.dto.ApiResponse;
import com.truthlens.dto.NewsArticleDTO;
import com.truthlens.entity.NewsCategory;
import com.truthlens.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<NewsArticleDTO>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.ok("Featured articles", newsService.getFeaturedArticles()));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<NewsArticleDTO>>> getLatest() {
        return ResponseEntity.ok(ApiResponse.ok("Latest articles", newsService.getLatestArticles()));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<NewsArticleDTO>>> getPopular() {
        return ResponseEntity.ok(ApiResponse.ok("Popular articles", newsService.getMostPopularArticles()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        Map<NewsCategory, Long> counts = newsService.getCategoryCounts();
        List<Map<String, Object>> result = Arrays.stream(NewsCategory.values())
                .map(cat -> Map.<String, Object>of(
                        "name",        cat.name(),
                        "displayName", cat.getDisplayName(),
                        "icon",        cat.getIcon(),
                        "badgeClass",  cat.getBadgeClass(),
                        "count",       counts.getOrDefault(cat, 0L)))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Categories", result));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<NewsArticleDTO>>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        NewsCategory cat = parseCategory(category);
        return ResponseEntity.ok(ApiResponse.ok(cat.getDisplayName() + " articles",
                newsService.getArticlesByCategory(cat, page, size)));
    }

    @GetMapping("/category/{category}/latest")
    public ResponseEntity<ApiResponse<List<NewsArticleDTO>>> getLatestByCategory(
            @PathVariable String category) {
        NewsCategory cat = parseCategory(category);
        return ResponseEntity.ok(ApiResponse.ok("Latest " + cat.getDisplayName(),
                newsService.getLatestByCategory(cat)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<NewsArticleDTO>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Search query 'q' is required"));
        }
        Page<NewsArticleDTO> results = (category != null && !category.isBlank())
                ? newsService.searchByCategory(q, parseCategory(category), page, size)
                : newsService.searchArticles(q, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Search results for: " + q, results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsArticleDTO>> getById(@PathVariable Long id) {
        return newsService.getArticleById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok("Article retrieved", dto)))
                .orElseGet(() -> ResponseEntity.notFound().<ApiResponse<NewsArticleDTO>>build());
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<NewsArticleDTO>>> getRelated(@PathVariable Long id) {
        return newsService.getArticleById(id)
                .map(article -> ResponseEntity.ok(ApiResponse.ok("Related articles",
                        newsService.getRelatedArticles(id, article.getCategory()))))
                .orElseGet(() -> ResponseEntity.notFound().<ApiResponse<List<NewsArticleDTO>>>build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<NewsCategory, Long> counts = newsService.getCategoryCounts();
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("total", newsService.getTotalArticleCount());
        counts.forEach((cat, count) -> stats.put(cat.name().toLowerCase(), count));
        return ResponseEntity.ok(ApiResponse.ok("Statistics", stats));
    }

    private NewsCategory parseCategory(String category) {
        try {
            return NewsCategory.valueOf(category.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: '" + category
                    + "'. Valid values: " + Arrays.toString(NewsCategory.values()));
        }
    }
}
