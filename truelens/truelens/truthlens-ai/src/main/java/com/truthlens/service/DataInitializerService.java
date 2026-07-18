package com.truthlens.service;

import com.truthlens.repository.NewsArticleRepository;
import com.truthlens.repository.TrustedArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataInitializerService {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerService.class);

    private final NewsArticleRepository    newsArticleRepository;
    private final TrustedArticleRepository trustedArticleRepository;

    public DataInitializerService(NewsArticleRepository newsArticleRepository,
                                   TrustedArticleRepository trustedArticleRepository) {
        this.newsArticleRepository    = newsArticleRepository;
        this.trustedArticleRepository = trustedArticleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void onApplicationReady() {
        long newsCount    = newsArticleRepository.count();
        long trustedCount = trustedArticleRepository.count();

        log.info("=======================================================");
        log.info("  TruthLens AI — Application Ready");
        log.info("  News Articles   : {}", newsCount);
        log.info("  Trusted Articles: {}", trustedCount);
        log.info("  Server URL      : http://localhost:8080");
        log.info("=======================================================");

        if (newsCount == 0) {
            log.warn("No news articles found. Please run the SQL seed script at:");
            log.warn("  src/main/resources/sql/schema.sql");
        }
    }
}
