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

- Content-based keyword generation.
- RAKE-based key phrase extraction.
- Lightweight keyword extraction and phrase scoring.
- Intent-aware keyword generation templates.
- SEO optimization with long-tail preference.
- Demand validation using cached Pytrends data and Google Autocomplete.
- Redis caching for generated keyword results.
- MySQL storage for generated keywords.
- API response metadata for validation source, estimated status, trend direction, and processing time.
- Auto-suggest from internal search analytics.
- Internal search analytics storage.
- Keyword clustering by concept, pattern, and popularity tier.

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
    "searchVolume": 0,
    "type": "LONG_TAIL",
    "estimated": true,
    "source": "GOOGLE_AUTOCOMPLETE",
    "validationStatus": "ESTIMATED",
    "trendDirection": 0.1,
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
This project shows how SEO keyword generation can be made fast and practical using RAKE-based keyword extraction, caching, and real search-demand validation in a production-style system.