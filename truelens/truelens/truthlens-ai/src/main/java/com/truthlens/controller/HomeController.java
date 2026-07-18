package com.truthlens.controller;

import com.truthlens.entity.NewsCategory;
import com.truthlens.service.NewsService;
import com.truthlens.service.VerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final NewsService         newsService;
    private final VerificationService verificationService;

    public HomeController(NewsService newsService, VerificationService verificationService) {
        this.newsService         = newsService;
        this.verificationService = verificationService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredArticles",  newsService.getFeaturedArticles());
        model.addAttribute("latestArticles",    newsService.getLatestArticles());
        model.addAttribute("popularArticles",   newsService.getMostPopularArticles());
        model.addAttribute("categories",        newsService.getAllCategories());
        model.addAttribute("categoryCounts",    newsService.getCategoryCounts());
        model.addAttribute("totalArticles",     newsService.getTotalArticleCount());
        model.addAttribute("verificationStats", verificationService.getStats());
        for (NewsCategory cat : NewsCategory.values()) {
            model.addAttribute(cat.name().toLowerCase() + "Articles",
                    newsService.getLatestByCategory(cat));
        }
        return "index";
    }

    @GetMapping("/news")
    public String newsBrowse(Model model) {
        model.addAttribute("categories",     newsService.getAllCategories());
        model.addAttribute("categoryCounts", newsService.getCategoryCounts());
        return "news-browse";
    }

    @GetMapping("/verify")
    public String verifyPage(Model model) {
        model.addAttribute("categories", newsService.getAllCategories());
        return "verify";
    }

    @GetMapping("/results")
    public String resultsPage(Model model) {
        model.addAttribute("categories", newsService.getAllCategories());
        return "results";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("categories", newsService.getAllCategories());
        return "about";
    }
}
