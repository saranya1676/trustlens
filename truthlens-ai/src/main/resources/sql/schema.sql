-- ============================================================
-- TruthLens AI - Database Schema
-- Database: truthlens_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS truthlens_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE truthlens_db;

-- ============================================================
-- Table: news_categories (lookup table)
-- ============================================================
CREATE TABLE IF NOT EXISTS news_categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)     NOT NULL UNIQUE,
    slug        VARCHAR(50)     NOT NULL UNIQUE,
    description VARCHAR(255),
    icon        VARCHAR(50),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: trusted_sources
-- ============================================================
CREATE TABLE IF NOT EXISTS trusted_sources (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NOT NULL,
    base_url       VARCHAR(500) NOT NULL,
    credibility    TINYINT      NOT NULL DEFAULT 80 COMMENT '0-100 credibility score',
    country        VARCHAR(100),
    language       VARCHAR(10)  NOT NULL DEFAULT 'en',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: trusted_articles (ground-truth reference corpus)
-- ============================================================
CREATE TABLE IF NOT EXISTS trusted_articles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(500)    NOT NULL,
    content         LONGTEXT        NOT NULL,
    summary         TEXT,
    source_name     VARCHAR(150)    NOT NULL,
    source_url      VARCHAR(1000),
    category        VARCHAR(50)     NOT NULL,
    author          VARCHAR(200),
    published_date  DATETIME,
    keywords        TEXT            COMMENT 'Comma-separated keywords',
    credibility_score TINYINT       NOT NULL DEFAULT 90,
    is_verified     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_category  (category),
    FULLTEXT INDEX ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: news_articles (user-submitted or browsable news)
-- ============================================================
CREATE TABLE IF NOT EXISTS news_articles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(500)    NOT NULL,
    content         LONGTEXT        NOT NULL,
    summary         TEXT,
    source_name     VARCHAR(200),
    source_url      VARCHAR(1000),
    image_url       VARCHAR(1000),
    category        VARCHAR(50)     NOT NULL,
    author          VARCHAR(200),
    published_date  DATETIME,
    keywords        TEXT,
    view_count      INT             NOT NULL DEFAULT 0,
    is_featured     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_category      (category),
    INDEX idx_published     (published_date),
    FULLTEXT INDEX ft_title (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: verification_results
-- ============================================================
CREATE TABLE IF NOT EXISTS verification_results (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    submitted_text      LONGTEXT        NOT NULL,
    submitted_url       VARCHAR(1000),
    submission_type     VARCHAR(20)     NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT, URL, IMAGE',
    verdict             VARCHAR(30)     NOT NULL COMMENT 'LIKELY_TRUE, LIKELY_FALSE, NEEDS_VERIFICATION',
    confidence_score    TINYINT         NOT NULL DEFAULT 0 COMMENT '0-100',
    explanation         TEXT            NOT NULL,
    matched_category    VARCHAR(50),
    analysis_details    JSON,
    ip_address          VARCHAR(45),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_verdict   (verdict),
    INDEX idx_created   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table: verification_matches (articles matched to a result)
-- ============================================================
CREATE TABLE IF NOT EXISTS verification_matches (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    verification_result_id  BIGINT      NOT NULL,
    trusted_article_id      BIGINT      NOT NULL,
    match_score             TINYINT     NOT NULL DEFAULT 0,
    match_type              VARCHAR(20) NOT NULL DEFAULT 'SIMILAR' COMMENT 'EXACT, SIMILAR, RELATED',
    PRIMARY KEY (id),
    FOREIGN KEY (verification_result_id) REFERENCES verification_results(id) ON DELETE CASCADE,
    FOREIGN KEY (trusted_article_id)     REFERENCES trusted_articles(id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Seed: News Categories
-- ============================================================
INSERT IGNORE INTO news_categories (name, slug, description, icon) VALUES
    ('International', 'international', 'Global news and world events',          'bi-globe'),
    ('National',      'national',      'Domestic news and local affairs',        'bi-flag'),
    ('Politics',      'politics',      'Political news and government updates',  'bi-building'),
    ('Sports',        'sports',        'Sports news and match results',          'bi-trophy'),
    ('Technology',    'technology',    'Tech news, science and innovation',      'bi-cpu');

-- ============================================================
-- Seed: Trusted Sources
-- ============================================================
INSERT IGNORE INTO trusted_sources (name, base_url, credibility, country, language) VALUES
    ('Reuters',           'https://www.reuters.com',        98, 'UK',  'en'),
    ('Associated Press',  'https://apnews.com',             97, 'USA', 'en'),
    ('BBC News',          'https://www.bbc.com/news',       95, 'UK',  'en'),
    ('The Guardian',      'https://www.theguardian.com',    93, 'UK',  'en'),
    ('NPR',               'https://www.npr.org',            92, 'USA', 'en'),
    ('Al Jazeera',        'https://www.aljazeera.com',      90, 'QA',  'en'),
    ('ABC News',          'https://abcnews.go.com',         89, 'USA', 'en'),
    ('ESPN',              'https://www.espn.com',           88, 'USA', 'en'),
    ('TechCrunch',        'https://techcrunch.com',         85, 'USA', 'en'),
    ('Wired',             'https://www.wired.com',          84, 'USA', 'en');

-- ============================================================
-- Seed: Trusted Articles (Reference Corpus)
-- ============================================================
INSERT INTO trusted_articles (title, content, summary, source_name, source_url, category, author, published_date, keywords, credibility_score) VALUES

-- INTERNATIONAL
('United Nations Climate Summit Reaches Historic Agreement on Carbon Emissions',
 'World leaders gathered at the United Nations Climate Summit have reached a historic agreement to reduce carbon emissions by 50% before 2035. The agreement, signed by 195 countries, represents the most ambitious climate commitment in history. Key provisions include mandatory renewable energy targets, carbon pricing mechanisms, and a $500 billion green transition fund for developing nations. Scientists and environmental groups have praised the deal, calling it a critical turning point in the fight against climate change. The agreement will now go to each nation''s legislature for ratification.',
 'UN Climate Summit secures landmark 50% carbon reduction deal signed by 195 nations with $500B green fund.',
 'Reuters', 'https://www.reuters.com/climate-summit-2024', 'INTERNATIONAL', 'Sarah Mitchell', '2024-11-15 09:00:00',
 'climate,UN,carbon emissions,renewable energy,global warming', 98),

('G7 Nations Pledge $200 Billion in Global Infrastructure Investment',
 'The Group of Seven industrialized nations have committed $200 billion over five years toward global infrastructure development in emerging economies. The initiative, titled "Build Back Better World Plus", targets roads, bridges, clean energy, and digital infrastructure across Africa, Asia, and Latin America. Leaders emphasized this as a counterweight to Chinese infrastructure lending. Individual nation pledges range from $20 billion to $50 billion. Projects will prioritize transparency and environmental sustainability standards.',
 'G7 pledges $200B for global infrastructure in emerging economies over five years.',
 'BBC News', 'https://www.bbc.com/news/g7-infrastructure', 'INTERNATIONAL', 'James Thornton', '2024-10-22 14:30:00',
 'G7,infrastructure,investment,emerging economies,development', 95),

('WHO Declares End of International Health Emergency for Major Respiratory Disease',
 'The World Health Organization has officially declared an end to the International Public Health Emergency for a major respiratory illness that affected millions worldwide over the past two years. WHO Director-General confirmed that while the disease remains a global health challenge, sustained vaccination efforts and improved treatment protocols have reduced hospitalizations by 90%. Member states are advised to transition to long-term management strategies integrated within existing health systems.',
 'WHO ends IPHE declaration for major respiratory disease as hospitalization falls 90% due to vaccination.',
 'Associated Press', 'https://apnews.com/who-health-emergency-end', 'INTERNATIONAL', 'Dr. Linda Chen', '2024-09-10 11:00:00',
 'WHO,health emergency,vaccination,respiratory disease,global health', 97),

-- NATIONAL
('Federal Government Announces $50 Billion National Infrastructure Renewal Plan',
 'The federal government has unveiled a comprehensive $50 billion National Infrastructure Renewal Plan aimed at rebuilding aging roads, bridges, railways, and public utilities over the next decade. The plan includes provisions for smart city technology, green energy grids, and expanded rural broadband connectivity. Infrastructure experts have called the plan overdue but achievable. Funding will come from a combination of federal bonds, public-private partnerships, and reallocation of existing transportation budgets. Construction is expected to create over 500,000 jobs nationally.',
 'Government launches $50B infrastructure plan covering roads, railways, utilities and rural broadband.',
 'NPR', 'https://www.npr.org/national-infrastructure-plan', 'NATIONAL', 'Michael Torres', '2024-12-01 08:00:00',
 'infrastructure,government,investment,jobs,broadband', 92),

('Census Bureau Releases New Population Data Showing Urban Growth Trends',
 'The National Census Bureau has released updated population data revealing significant urban growth trends over the past five years. Major metropolitan areas saw an average population increase of 8%, while rural counties experienced a net decline of 3%. The data shows increased migration from the Midwest to coastal cities and the Sun Belt region. Housing affordability and employment opportunities remain the primary drivers of internal migration. The new data will be used to redraw congressional districts and allocate federal funding.',
 'Census data shows 8% urban growth and 3% rural decline, with Midwest to coast migration trending up.',
 'The Guardian', 'https://www.theguardian.com/census-population-data', 'NATIONAL', 'Amy Johnson', '2024-11-05 10:00:00',
 'census,population,urban,rural,migration,housing', 93),

-- POLITICS
('Senate Passes Bipartisan Healthcare Reform Bill After Months of Negotiations',
 'The Senate has passed a landmark bipartisan healthcare reform bill with a 68-32 vote after seven months of intense negotiations. The legislation expands Medicare eligibility to age 60, caps prescription drug prices, and increases federal subsidies for marketplace insurance plans. Both parties praised the compromise, though progressive senators criticized it for not going further on drug pricing. The bill now heads to the House where leadership has indicated broad support. President signed executive support of the measure in a joint statement with Senate leadership.',
 'Senate passes 68-32 bipartisan health bill expanding Medicare to 60, capping drug prices.',
 'Associated Press', 'https://apnews.com/senate-healthcare-reform', 'POLITICS', 'Rachel Green', '2024-10-18 16:00:00',
 'senate,healthcare,medicare,bipartisan,prescription drugs,reform', 97),

('Supreme Court Rules on Landmark Digital Privacy Case',
 'The Supreme Court issued a 7-2 ruling in a landmark digital privacy case, determining that law enforcement agencies require a warrant to access individuals'' location data from technology companies. The ruling significantly expands Fourth Amendment protections into the digital realm and is expected to affect thousands of ongoing investigations. Civil liberties organizations hailed the decision as a major victory for privacy rights. Technology companies have pledged to update their data-sharing policies in compliance with the ruling within 90 days.',
 'Supreme Court rules 7-2 that warrants are required for location data access, expanding digital privacy rights.',
 'NPR', 'https://www.npr.org/supreme-court-digital-privacy', 'POLITICS', 'Jennifer Walsh', '2024-08-20 14:00:00',
 'supreme court,privacy,digital rights,fourth amendment,law enforcement,technology', 92),

-- SPORTS
('National Football Team Wins World Championship in Dramatic Final',
 'The national football team claimed the World Championship title in a thrilling final match that went to extra time before a golden goal sealed victory. The team, ranked 5th entering the tournament, defied all predictions to beat the defending champions 2-1 before a crowd of 85,000. The captain, who scored the decisive goal, dedicated the win to the country and to young players everywhere. Victory parades are planned in major cities across the nation. The win ends a 24-year drought since the country last lifted the trophy.',
 'National football team wins World Championship with a golden goal in extra time, ending 24-year wait.',
 'ESPN', 'https://www.espn.com/football-world-championship', 'SPORTS', 'Carlos Rivera', '2024-07-15 22:30:00',
 'football,world championship,national team,victory,sports', 88),

('Olympic Games Set New Records in Viewership and Athlete Participation',
 'The latest Olympic Games concluded with record-breaking viewership numbers and the highest ever athlete participation, with over 11,000 competitors from 206 nations. Global streaming viewership topped 3.5 billion, surpassing previous records by 400 million. Athletes broke 27 world records across various disciplines. The host city reported $4.2 billion in economic benefits. The International Olympic Committee announced expanded para-athlete categories and new sustainability commitments for future games.',
 'Olympics conclude with record 11,000 athletes, 27 world records broken, and 3.5B streaming viewers.',
 'BBC News', 'https://www.bbc.com/news/olympics-records', 'SPORTS', 'David Kim', '2024-09-12 18:00:00',
 'olympics,records,viewership,athletes,sports,global', 95),

-- TECHNOLOGY
('OpenAI Releases New AI Model with Advanced Reasoning Capabilities',
 'OpenAI has released its latest artificial intelligence model, demonstrating significantly improved reasoning, mathematical problem-solving, and coding abilities. Independent benchmarks show the model outperforms previous versions by 40% on complex reasoning tasks and achieves near-human performance on standardized professional exams. The model features improved safety guardrails and reduced hallucination rates. Enterprise pricing starts at $0.015 per 1,000 tokens. Researchers and developers can access the model through OpenAI''s API platform immediately.',
 'OpenAI launches new AI model with 40% better reasoning, near-human exam performance, and lower hallucination rates.',
 'TechCrunch', 'https://techcrunch.com/openai-new-model-2024', 'TECHNOLOGY', 'Mark Davidson', '2024-11-20 10:00:00',
 'OpenAI,artificial intelligence,AI model,reasoning,machine learning,technology', 85),

('Global Semiconductor Shortage Finally Eases as New Fab Plants Come Online',
 'The global semiconductor shortage that disrupted supply chains for three years is finally easing as new fabrication plants in the United States, Europe, and Taiwan come online. Industry analysts report chip supply has increased 35% year-over-year. Consumer electronics prices are expected to decline by 10-15% over the next two quarters. Automakers, who were among the hardest hit, report production has returned to pre-shortage levels. Governments worldwide are continuing to invest in domestic chip manufacturing to reduce future supply vulnerabilities.',
 'Semiconductor shortage eases as new fabs open globally, chip supply up 35% and electronics prices to drop.',
 'Wired', 'https://www.wired.com/semiconductor-shortage-easing', 'TECHNOLOGY', 'Priya Patel', '2024-10-30 09:00:00',
 'semiconductors,chips,supply chain,technology,manufacturing,electronics', 84),

('Quantum Computing Milestone: First Error-Corrected Logical Qubit Achieved',
 'Researchers at a leading quantum computing laboratory have achieved a long-sought milestone in quantum computing: the creation of the first fully error-corrected logical qubit that maintains coherence long enough for practical computation. The breakthrough, published in Nature, brings fault-tolerant quantum computing significantly closer to reality. Scientists project that practical quantum computers capable of breaking current encryption standards could be available within 8-12 years. Governments and cybersecurity agencies have begun preparing post-quantum cryptography standards in response.',
 'Scientists achieve first error-corrected logical qubit, moving fault-tolerant quantum computing closer to reality.',
 'Wired', 'https://www.wired.com/quantum-computing-qubit-milestone', 'TECHNOLOGY', 'Dr. Aiko Tanaka', '2024-09-28 12:00:00',
 'quantum computing,qubit,cryptography,research,technology,science', 84);

-- ============================================================
-- Seed: Browsable News Articles
-- ============================================================
INSERT INTO news_articles (title, content, summary, source_name, source_url, image_url, category, author, published_date, keywords, is_featured) VALUES

('Climate Activists Stage Global Protests Demanding Faster Action',
 'Millions of climate activists took to the streets in over 150 cities worldwide, demanding governments accelerate their commitments to reduce greenhouse gas emissions. Protests were coordinated across six continents with particularly large turnouts in London, New York, Berlin, and Sydney. Organizers called for an immediate halt to new fossil fuel projects and rapid phase-out of coal power plants. Several governments released statements acknowledging the urgency of public concern. The demonstrations come ahead of the next major international climate conference.',
 'Millions protest globally demanding faster climate action across 150+ cities on six continents.',
 'The Guardian', 'https://www.theguardian.com/climate-protests-global', 'https://images.unsplash.com/photo-1611270629569-8b357cb88da9?w=800',
 'INTERNATIONAL', 'Emma Clarke', '2024-12-10 07:00:00', 'climate,protest,environment,fossil fuels,activism', TRUE),

('Breakthrough Cancer Treatment Shows 90% Success Rate in Trials',
 'A groundbreaking cancer treatment combining CAR-T cell therapy with a novel immunotherapy drug has shown a 90% success rate in Phase 3 clinical trials involving 800 patients. The treatment, effective against multiple forms of lymphoma and leukemia, produced complete remission in most cases with manageable side effects. Regulatory approval is expected within 12 months. The pharmaceutical company plans to price the treatment at $250,000 per course, sparking debate about accessibility and insurance coverage.',
 'New combination cancer therapy shows 90% remission rate in 800-patient Phase 3 trial.',
 'Reuters', 'https://www.reuters.com/cancer-treatment-trial', 'https://images.unsplash.com/photo-1579154204601-01588f351e67?w=800',
 'NATIONAL', 'Dr. Patricia Moore', '2024-12-08 09:30:00', 'cancer,treatment,medical,health,breakthrough', TRUE),

('Election Results: Incumbent Party Wins by Slim Margin',
 'The incumbent party secured a narrow victory in the national election, winning 52% of the popular vote and retaining control of both chambers of parliament. Voter turnout reached 74%, the highest in two decades. The opposition has accepted the results while calling for an independent audit of several closely contested districts. Political analysts attribute the win to strong economic performance and healthcare policy popularity. Coalition negotiations are expected to begin next week for minor parties.',
 'Incumbent party wins election 52-48 with 74% voter turnout, highest in 20 years.',
 'BBC News', 'https://www.bbc.com/news/election-results', 'https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?w=800',
 'POLITICS', 'Andrew Williams', '2024-11-28 23:00:00', 'election,politics,voting,democracy,government', TRUE),

('Star Athlete Breaks 40-Year-Old World Record in Championship Meet',
 'At the World Athletics Championships, a 23-year-old sprinter shattered a 40-year-old world record in the 100-meter dash, crossing the finish line in 9.58 seconds. The performance stunned spectators and competitors alike. The athlete, who only turned professional two years ago, credits the achievement to intensive training and advances in sports science. The record had previously been considered unbreakable by many coaches and analysts. Anti-doping authorities confirmed clean test results from the event.',
 '23-year-old sprinter breaks 40-year 100m world record with 9.58s in World Athletics Championship.',
 'ESPN', 'https://www.espn.com/world-record-sprint', 'https://images.unsplash.com/photo-1541252260730-0412e8e2108e?w=800',
 'SPORTS', 'Marcus Johnson', '2024-11-15 20:00:00', 'athletics,world record,sprint,100m,sports', FALSE),

('Tech Giants Face New Antitrust Regulations in Europe',
 'The European Union has enacted sweeping new antitrust regulations targeting major technology companies, requiring them to open their platforms to competitors and share data with smaller businesses. Companies failing to comply face fines of up to 10% of global annual revenue. The regulations cover app stores, search engines, social media platforms, and cloud services. US tech giants have 18 months to comply. Industry groups warn the rules could harm innovation, while consumer advocates praise increased competition and user choice.',
 'EU enacts landmark antitrust rules for Big Tech, mandating platform openness under 10% revenue fine threat.',
 'TechCrunch', 'https://techcrunch.com/eu-antitrust-tech', 'https://images.unsplash.com/photo-1516321165247-4aa89a48be55?w=800',
 'TECHNOLOGY', 'Sophie Larsson', '2024-12-05 11:00:00', 'technology,antitrust,EU,regulation,big tech,competition', TRUE),

('National Budget Announced: Education and Healthcare Get Major Boost',
 'The finance minister unveiled the national budget with record allocations to education and healthcare. Education funding increases by 25% to build 5,000 new schools and hire 100,000 additional teachers. Healthcare receives an 18% boost targeting rural hospital upgrades and mental health services expansion. Defense spending remains flat. Critics from fiscal conservative groups warn of widening deficits, while social policy advocates applaud prioritizing human development. The budget projects 3.2% GDP growth for the coming fiscal year.',
 'Budget increases education 25% and healthcare 18%, targeting schools, teachers, and rural hospitals.',
 'NPR', 'https://www.npr.org/national-budget-education-health', 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800',
 'NATIONAL', 'Finance Correspondent', '2024-12-03 13:00:00', 'budget,education,healthcare,government,spending', FALSE),

('International Space Station Marks 25 Years of Continuous Human Presence',
 'The International Space Station celebrated 25 years of uninterrupted human habitation, marking a landmark achievement in international scientific cooperation. Astronauts from 19 countries have lived and worked aboard the station since its first permanent crew arrived. Over 3,000 scientific experiments have been conducted, yielding breakthroughs in medicine, materials science, and our understanding of human physiology in microgravity. NASA and partner agencies confirmed that operations will continue through at least 2030, with commercial successors planned.',
 'ISS marks 25 years of continuous habitation with crew from 19 nations and 3,000+ experiments completed.',
 'Reuters', 'https://www.reuters.com/iss-25-years', 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800',
 'INTERNATIONAL', 'NASA Correspondent', '2024-11-02 15:00:00', 'ISS,space,NASA,science,international cooperation', FALSE),

('Major Cybersecurity Breach Exposes Data of 100 Million Users',
 'A major cybersecurity breach at one of the world''s largest social media platforms has exposed personal data of approximately 100 million users, including names, email addresses, phone numbers, and encrypted passwords. The company disclosed the breach 48 hours after discovery, meeting new regulatory disclosure requirements. Cybersecurity experts believe the attackers exploited a zero-day vulnerability in a third-party authentication library. Affected users are being notified and advised to change passwords. Regulatory investigations have been launched in multiple jurisdictions.',
 'Cybersecurity breach exposes data of 100M users via zero-day vulnerability in authentication library.',
 'Wired', 'https://www.wired.com/social-media-breach', 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800',
 'TECHNOLOGY', 'Security Reporter', '2024-10-25 16:30:00', 'cybersecurity,data breach,privacy,technology,hacking', FALSE);
