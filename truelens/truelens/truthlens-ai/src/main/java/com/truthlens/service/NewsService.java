package com.truthlens.service;

import com.truthlens.dto.NewsArticleDTO;
import com.truthlens.entity.NewsArticle;
import com.truthlens.entity.NewsCategory;
import com.truthlens.repository.NewsArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public NewsService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    public Page<NewsArticleDTO> getArticlesByCategory(NewsCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        return newsArticleRepository.findByCategoryOrderByPublishedDateDesc(category, pageable)
                .map(this::toDTO);
    }

    public List<NewsArticleDTO> getLatestByCategory(NewsCategory category) {
        return newsArticleRepository.findTop6ByCategoryOrderByPublishedDateDesc(category)
                .stream().map(this::toDTO).toList();
    }

    public Page<NewsArticleDTO> getAllArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        return newsArticleRepository.findAllByOrderByPublishedDateDesc(pageable).map(this::toDTO);
    }

    public List<NewsArticleDTO> getFeaturedArticles() {
        return newsArticleRepository.findTop5ByFeaturedTrueOrderByPublishedDateDesc()
                .stream().map(this::toDTO).toList();
    }

    public List<NewsArticleDTO> getLatestArticles() {
        return newsArticleRepository.findTop10ByOrderByPublishedDateDesc()
                .stream().map(this::toDTO).toList();
    }

    public List<NewsArticleDTO> getMostPopularArticles() {
        return newsArticleRepository.findTop5ByOrderByViewCountDesc()
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public Optional<NewsArticleDTO> getArticleById(Long id) {
        return newsArticleRepository.findById(id).map(article -> {
            newsArticleRepository.incrementViewCount(id);
            return toDTO(article);
        });
    }

    public List<NewsArticleDTO> getRelatedArticles(Long articleId, NewsCategory category) {
        return newsArticleRepository.findTop4ByCategoryAndIdNotOrderByPublishedDateDesc(category, articleId)
                .stream().map(this::toDTO).toList();
    }

    public Page<NewsArticleDTO> searchArticles(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return newsArticleRepository.searchByKeyword(keyword.trim(), pageable).map(this::toDTO);
    }

    public Page<NewsArticleDTO> searchByCategory(String keyword, NewsCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return newsArticleRepository.searchByCategoryAndKeyword(category, keyword.trim(), pageable).map(this::toDTO);
    }

    public Map<NewsCategory, Long> getCategoryCounts() {
        return Arrays.stream(NewsCategory.values())
                .collect(Collectors.toMap(cat -> cat, newsArticleRepository::countByCategory));
    }

    public long getTotalArticleCount() {
        return newsArticleRepository.count();
    }

    public List<NewsCategory> getAllCategories() {
        return Arrays.asList(NewsCategory.values());
    }

    public NewsArticleDTO toDTO(NewsArticle article) {
        NewsArticleDTO dto = new NewsArticleDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setShortSummary(article.getShortSummary());
        dto.setSourceName(article.getSourceName());
        dto.setSourceUrl(article.getSourceUrl());
        dto.setImageUrl(article.getImageUrl());
        dto.setCategory(article.getCategory());
        dto.setCategoryDisplayName(article.getCategory().getDisplayName());
        dto.setCategoryBadgeClass(article.getCategory().getBadgeClass());
        dto.setCategoryIcon(article.getCategory().getIcon());
        dto.setAuthor(article.getAuthor());
        dto.setPublishedDate(article.getPublishedDate());
        dto.setKeywords(article.getKeywords());
        dto.setViewCount(article.getViewCount());
        dto.setFeatured(article.isFeatured());
        dto.setCreatedAt(article.getCreatedAt());
        return dto;
    }
}
