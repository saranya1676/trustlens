# TruthLens AI — AI-Powered News Verification & Transparency Platform

A complete, production-ready full-stack Java web application that lets users browse
news by category, search articles, and submit news text, URLs, or images for
AI-powered fact-checking against a trusted source database.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 · Spring Boot 3.2.5 · Spring MVC · Spring Data JPA |
| Database | MySQL 8.x (Hibernate auto-DDL) |
| Frontend | Thymeleaf · Bootstrap 5.3 · Bootstrap Icons · JavaScript Fetch API |
| Build | Maven 3.x |
| ORM | Hibernate · Lombok |

---

## Project Structure

```
truthlens-ai/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/truthlens/
│       │   ├── TruthLensApplication.java          ← Spring Boot entry point
│       │   ├── entity/
│       │   │   ├── NewsArticle.java                ← Browsable news articles
│       │   │   ├── TrustedArticle.java             ← Verified reference corpus
│       │   │   ├── VerificationResult.java         ← Stored verification results
│       │   │   ├── VerificationMatch.java          ← Result ↔ TrustedArticle join
│       │   │   ├── NewsCategory.java               ← Enum: 5 categories
│       │   │   ├── Verdict.java                    ← Enum: LIKELY_TRUE/FALSE/NEEDS_VERIFICATION
│       │   │   └── SubmissionType.java             ← Enum: TEXT/URL/IMAGE
│       │   ├── dto/
│       │   │   ├── NewsArticleDTO.java
│       │   │   ├── VerificationRequestDTO.java
│       │   │   ├── VerificationResultDTO.java      ← Includes MatchedArticleDTO inner class
│       │   │   └── ApiResponse.java                ← Generic REST response wrapper
│       │   ├── repository/
│       │   │   ├── NewsArticleRepository.java
│       │   │   ├── TrustedArticleRepository.java
│       │   │   ├── VerificationResultRepository.java
│       │   │   └── VerificationMatchRepository.java
│       │   ├── service/
│       │   │   ├── NewsService.java                ← Browse, search, featured articles
│       │   │   ├── VerificationService.java        ← 8-step verification engine
│       │   │   └── DataInitializerService.java     ← Startup health log
│       │   ├── controller/
│       │   │   ├── HomeController.java             ← MVC views (Thymeleaf)
│       │   │   ├── NewsController.java             ← REST /api/news/**
│       │   │   └── VerificationController.java     ← REST /api/verify/**
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.properties
│           ├── sql/
│           │   └── schema.sql                      ← Full DB schema + seed data
│           ├── static/
│           │   ├── css/style.css
│           │   └── js/
│           │       ├── main.js                     ← Shared UI utilities
│           │       └── verify.js                   ← Verification form + results renderer
│           └── templates/
│               ├── index.html                      ← Homepage
│               ├── news-browse.html                ← Category/search browser
│               ├── verify.html                     ← Verification form (3 tabs)
│               ├── results.html                    ← Verification results
│               ├── about.html                      ← About page
│               └── fragments/
│                   ├── navbar.html
│                   └── footer.html
```

---

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher
- A modern web browser

---

## Setup & Run Instructions

### Step 1 — Clone or extract the project

```bash
cd truthlens-ai
```

### Step 2 — Create the MySQL database and run the seed script

Open a MySQL client (MySQL Workbench, DBeaver, or CLI) and run:

```sql
-- Option A: run the full schema + seed file directly
SOURCE src/main/resources/sql/schema.sql;
```

Or manually:

```sql
CREATE DATABASE IF NOT EXISTS truthlens_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE truthlens_db;
-- then paste the contents of src/main/resources/sql/schema.sql
```

The script creates all tables and loads:
- 5 news categories
- 10 trusted sources (Reuters, BBC, AP, Guardian, NPR, Al Jazeera, ABC, ESPN, TechCrunch, Wired)
- 12 trusted reference articles (used by the verification engine)
- 8 browsable news articles

### Step 3 — Configure database credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/truthlens_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root       # ← change to your MySQL username
spring.datasource.password=root       # ← change to your MySQL password
```

### Step 4 — Build and run

```bash
mvn clean install
mvn spring-boot:run
```

Or build a JAR and run it:

```bash
mvn clean package -DskipTests
java -jar target/truthlens-ai-1.0.0.jar
```

### Step 5 — Open in browser

```
http://localhost:8080
```

---

## Application Pages

| URL | Description |
|---|---|
| `/` | Homepage — hero verify box, featured news, category rows, stats |
| `/news` | Browse news — filter by category, search by keyword, pagination |
| `/verify` | Submit content for verification (text / URL / image tabs) |
| `/results` | Verification result — verdict, confidence gauge, matched sources, similar news |
| `/about` | Platform overview, tech stack, how verification works |

---

## REST API Reference

All REST endpoints are under `/api/` and return `ApiResponse<T>` JSON:

```json
{
  "success": true,
  "message": "OK",
  "data": { ... },
  "timestamp": "2024-12-01T10:00:00"
}
```

### News Endpoints — `/api/news`

| Method | Path | Description |
|---|---|---|
| GET | `/api/news/featured` | Up to 5 featured articles |
| GET | `/api/news/latest` | 10 most recent articles |
| GET | `/api/news/popular` | 5 most viewed articles |
| GET | `/api/news/categories` | All categories with counts |
| GET | `/api/news/category/{cat}` | Paginated articles by category |
| GET | `/api/news/category/{cat}/latest` | Latest 6 articles in category |
| GET | `/api/news/search?q=&category=` | Full-text keyword search |
| GET | `/api/news/{id}` | Single article (increments view count) |
| GET | `/api/news/{id}/related` | Related articles (same category) |
| GET | `/api/news/stats` | Article counts per category |

**Category values:** `INTERNATIONAL`, `NATIONAL`, `POLITICS`, `SPORTS`, `TECHNOLOGY`

### Verification Endpoints — `/api/verify`

| Method | Path | Content-Type | Description |
|---|---|---|---|
| POST | `/api/verify/text` | `application/json` | Verify plain text or article |
| POST | `/api/verify/url` | `application/json` | Verify a news URL |
| POST | `/api/verify/image` | `multipart/form-data` | Verify an image |
| GET | `/api/verify/result/{id}` | — | Retrieve a saved result by ID |
| GET | `/api/verify/stats` | — | Verdict counts (true/false/unverified) |

#### POST `/api/verify/text` — Request body

```json
{
  "content": "Your news article text here...",
  "submissionType": "TEXT"
}
```

#### POST `/api/verify/url` — Request body

```json
{
  "url": "https://example.com/article",
  "content": "Optional: article text for better accuracy",
  "submissionType": "URL"
}
```

#### POST `/api/verify/image` — Form fields

```
image        : file (JPEG/PNG/WebP/GIF, max 10MB)
extractedText: string (optional — text visible in the image)
```

#### Verification Result Response

```json
{
  "success": true,
  "data": {
    "id": 1,
    "verdict": "LIKELY_TRUE",
    "verdictDisplayName": "Likely True",
    "verdictCssClass": "verdict-true",
    "verdictIcon": "bi-check-circle-fill",
    "confidenceScore": 82,
    "confidenceLabel": "High",
    "explanation": "The submitted content strongly aligns with verified reporting from Reuters...",
    "matchedCategory": "TECHNOLOGY",
    "matchedCategoryDisplayName": "Technology",
    "matchedArticles": [
      {
        "title": "...",
        "sourceName": "Reuters",
        "sourceUrl": "https://...",
        "matchScore": 85,
        "matchType": "SIMILAR",
        "credibilityScore": 98
      }
    ],
    "similarNews": [ ... ],
    "analyzedAt": "2024-12-01T10:05:22"
  }
}
```

---

## Verification Engine — How It Works

The `VerificationService` runs a deterministic 8-step pipeline:

1. **Text preparation** — normalizes input; extracts URL slug text if no content provided
2. **Keyword extraction** — tokenizes text, removes stop words, extracts unigrams + bigrams (max 30 terms)
3. **Category detection** — scores text against signal keyword lists for all 5 categories
4. **Candidate retrieval** — queries `trusted_articles` using top keywords, category-scoped first, then broader
5. **Similarity scoring** — scores each candidate: keyword overlap (base) + title match bonus + content depth + shared words + credibility weight
6. **Verdict derivation** — thresholds: ≥70 → LIKELY_TRUE, 40–69 + multiple corroborators → LIKELY_TRUE, 40–69 alone → NEEDS_VERIFICATION, <25 with enough keywords → LIKELY_FALSE
7. **Persistence** — saves `VerificationResult` + `VerificationMatch` records to MySQL
8. **Similar news** — fetches browsable articles from the same category for comparison

### Verdict Thresholds

| Verdict | Condition |
|---|---|
| **Likely True** | Top match score ≥ 70, OR score 40–69 with ≥ 2 corroborating sources |
| **Likely False** | ≥ 5 keywords extracted, top match score < 25 — claims are specific but unverifiable |
| **Needs Verification** | Scores between 25–69 with insufficient corroboration, or no candidates found |

### Confidence Score Formula

```
confidence = (topScore × 0.60) + (avgScore × 0.25) + (matchCount × 2.5)
```
Capped at 98%, floor at 5%.

---

## News Categories

| Category | Description | Detection Signals |
|---|---|---|
| **International** | Global news, world events | UN, G7, NATO, treaty, summit, foreign, bilateral |
| **National** | Domestic news, government | federal, domestic, census, parliament, policy, budget |
| **Politics** | Politics, elections | election, vote, campaign, senator, supreme court, bill |
| **Sports** | Sports, athletics | championship, league, medal, FIFA, NFL, NBA, Olympics |
| **Technology** | Tech, science, AI | AI, semiconductor, quantum, startup, blockchain, 5G |

---

## Future API Integration

The codebase is designed for drop-in external API integration.
Hook points are already stubbed in `VerificationService.java` and `VerificationController.java`:

| API | Purpose | Config Key |
|---|---|---|
| NewsAPI.org | Live article ingestion | `truthlens.api.newsapi.key` |
| Google Fact Check Tools | Crowdsourced fact checks | `truthlens.api.factcheck.key` |
| OpenAI GPT | Semantic similarity, NLP | `truthlens.api.openai.key` |
| Google Vision / Tesseract | Server-side OCR for images | — |
| Reuters Connect / AP Wire | Real-time trusted feeds | — |

To enable, uncomment the API blocks in `VerificationService` and set the keys
in `application.properties`.

---

## Configuration Reference

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/truthlens_db?...
spring.datasource.username=root
spring.datasource.password=root

# JPA
spring.jpa.hibernate.ddl-auto=update     # use 'validate' in production
spring.jpa.show-sql=false

# File upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Server
server.port=8080
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Communications link failure` | Verify MySQL is running and credentials are correct in `application.properties` |
| `Table 'truthlens_db.xxx' doesn't exist` | Run `schema.sql` against your MySQL instance |
| Port 8080 in use | Change `server.port=8081` in `application.properties` |
| `java.lang.UnsupportedClassVersionError` | Ensure you are using Java 17+ (`java -version`) |
| Empty news grid on homepage | Confirm seed data was inserted — check `SELECT COUNT(*) FROM news_articles;` |
| Verification returns "Needs Verification" for everything | Seed data is missing — re-run `schema.sql` |
| Build fails on Lombok annotations | Ensure Lombok annotation processor is enabled in your IDE |

---

## Enabling Lombok in Your IDE

**IntelliJ IDEA:**
1. Install the Lombok plugin (Settings → Plugins → search "Lombok")
2. Enable annotation processing (Settings → Build → Compiler → Annotation Processors → Enable)

**Eclipse / Spring Tool Suite:**
1. Run `java -jar ~/.m2/repository/org/projectlombok/lombok/*/lombok-*.jar`
2. Follow the installer to add the Lombok agent to your IDE

---

## License

This project is provided for educational purposes. Built with Spring Boot, MySQL,
Bootstrap 5, and JavaScript — no placeholder code, fully functional end-to-end.
