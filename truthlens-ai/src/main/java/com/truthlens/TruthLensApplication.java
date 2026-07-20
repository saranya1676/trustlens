package com.truthlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TruthLens AI — AI-Powered News Verification & Transparency Platform
 *
 * Entry point for the Spring Boot application.
 *
 * Architecture:
 *   Entity → Repository → Service → Controller → Thymeleaf / REST API
 *
 * Key packages:
 *   com.truthlens.entity     — JPA entities and enums
 *   com.truthlens.dto        — Data Transfer Objects
 *   com.truthlens.repository — Spring Data JPA repositories
 *   com.truthlens.service    — Business logic and verification engine
 *   com.truthlens.controller — MVC and REST controllers
 *   com.truthlens.exception  — Global exception handling
 *
 * To run:
 *   1. Create MySQL database: truthlens_db
 *   2. Run src/main/resources/sql/schema.sql
 *   3. Update credentials in application.properties
 *   4. mvn spring-boot:run
 *   5. Open http://localhost:8080
 */
@SpringBootApplication
public class TruthLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(TruthLensApplication.class, args);
    }
}
