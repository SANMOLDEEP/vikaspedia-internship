# SEO Keyword Generation System

An intelligent keyword generation and validation engine built to produce SEO-focused, search-demand-driven keywords from raw content while maintaining low latency through caching, optimized processing pipelines, and limited external API usage.

The goal of the project is not only to generate words, but to generate useful keywords that are relevant to the content and backed by search-demand signals where available.

## Project Goals

- Generate relevant keywords from article/content text.
- Prefer natural long-tail SEO phrases.
- Validate whether keywords are likely searched by real users.
- Keep generation fast by avoiding heavy API calls on every request.
- Cache repeated work using Redis.
- Store generated keywords and internal analytics in MySQL.
- Provide a usable React frontend demo.

## Feature Summary

Core features:

- Content-based keyword generation with natural long-tail bias.
- RAKE + noun-phrase extraction.
- Intent-aware optimization templates (tutorial / comparison / beginner / optimization).
- Naturalness + quality ranking (removes duplicates and low-quality/broken phrases).
- Demand validation using:
  - Google Autocomplete ("is searched?")
  - Cached Pytrends popularity prefilled into Redis
- Redis caching for low-latency keyword responses.
- MySQL storage for generated keywords + internal analytics.
- Keyword enrichment with:
  - confidenceScore
  - searchIntent
  - popularityTier
  - cluster

## Keyword-Quality Pipeline

The backend generates keywords through a multi-stage pipeline:

```text
KeywordExtractionService
  - preprocess + tokenization
  - RAKE + noun phrases + cached trending seeds
  - internal token frequency (for topic scoring)

KeywordOptimizationService
  - generates natural long-tail candidates
  - applies intent/pattern templates
      |
      v
KeywordRankingService
  - scores phrase naturalness + relevance
  - boosts long-tail candidates
      |
      v
KeywordValidationService
  - batch validates using existing trend signals
  - confidenceScore + popularityTier
  - internal frequency lookup via KeywordRepository
      |
      v
KeywordClusterService
  - assigns topic clusters using searchIntent + keyword text
```


## Technology Stack

Backend:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- Redis
- Maven

Frontend:

- React
- Vite
- CSS

Optional data prefetch:

- Python
- Pytrends
- Redis Python client

## Architecture

```text
User Content
  -> React Frontend
  -> Spring Boot API
  -> KeywordGenerationPipeline
  -> Extraction + RAKE
  -> SEO Optimization
  -> Trend / Autocomplete Validation
  -> MySQL Storage
  -> Redis Cache
  -> Keyword Response
```

## Repository Structure

```text
keyword-generation-system/
  backend/
    src/main/java/com/seo/keywordgenerator/
    src/main/resources/
    scripts/
    pom.xml
  frontend/
    src/
    package.json
    vite.config.js
  database/
    schema.sql
  docker/
  README.md
```

## Keyword Generation API Example

Request:

```http
POST http://localhost:8080/api/keywords/generate-keywords
Content-Type: application/json
```

```json
{
  "content": "Vue.js is a progressive JavaScript framework for building user interfaces with components and reactivity.",
  "maxKeywords": 20,
  "includeLongTail": true
}
```

Response example:

```json
[
  {
    "keyword": "vue tutorial guide",
    "score": 62.0,
    "confidenceScore": 78.4,
    "searchVolume": 120,
    "type": "LONG_TAIL",
    "estimated": true,
    "source": "GOOGLE_AUTOCOMPLETE",
    "validationStatus": "WEAKLY_VALIDATED",
    "trendDirection": 0.1,
    "searchIntent": "TUTORIAL",
    "popularityTier": "MEDIUM",
    "cluster": "Vue Js Tutorials",
    "processingTimeMs": 728,
    "createdAt": "2026-05-14 13:00:00",
    "validatedAt": "2026-05-14 13:00:00"
  }
]
```


## Database

The database schema is located at:

```text
database/schema.sql
```

Main tables:

- `keywords`
- `search_analytics`

The `keywords` table stores generated keyword results and validation metadata.

The `search_analytics` table stores internal user/search activity for auto-suggest and analytics.

## Setup

### Requirements

- Java 21
- MySQL 8
- Redis 7
- Node.js and npm

### MySQL Setup

Run the schema:

```bash
mysql -u root -p < database/schema.sql
```
Set local database credentials before starting the backend. PowerShell example:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your_mysql_password>"
```

### Redis Setup

If the Redis container already exists:

```bash
docker start redis
```

If it does not exist:

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

Check Redis:

```bash
docker ps --filter name=redis
```

### Backend Setup

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

Frontend URL:

```text
http://127.0.0.1:5173
```

## Frontend Features

The frontend includes:

- Content input field.
- Generate Keywords button.
- Keyword list.
- Popularity indicator.
- Search volume/status display.
- Validation source display.
- Keyword type display.
- Keyword Intelligence Tools panel.
- Auto-Suggest
- Keyword Clusters
- Internal Analytics

## Scheduled Trend Prefetch

Trending prefetch can be triggered from:

```http
POST /api/admin/trending/prefetch
```

The scheduler uses:

- `TrendingKeywordScheduler`
- `backend/scripts/pytrends_prefetch.py`

### Pytrends caching (Redis)

The Python script stores Pytrends popularity in Redis using keys such as:

```text
trend:pyt:<keyword>
```


## Configuration

Main configuration file:

```text
backend/src/main/resources/application.yml
```

Important configuration sections:

- MySQL datasource
- Redis host/port
- cache TTLs
- autocomplete rate limit
- Pytrends scheduler settings
- server port

Database credentials are read from environment variables in `backend/src/main/resources/application.yml`:

```yaml
username: ${DB_USERNAME:root}
password: ${DB_PASSWORD:}
```
## Internal keyword frequency lookup

During validation/enrichment, the pipeline performs an internal frequency lookup using `KeywordRepository.countByKeywordIgnoreCase(keyword)`. This helps boost phrases that have strong token overlap with the user’s content and reduces low-quality duplicates.

This project shows how SEO keyword generation can be made fast and practical using RAKE-based keyword extraction, caching, and real search-demand validation in a production-style system.