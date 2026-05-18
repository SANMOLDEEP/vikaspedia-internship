# Database Setup Instructions

## Prerequisites

- MySQL Server installed and running
- A MySQL user with permission to create/use the `keyword_generator` database

Do not commit local database passwords. The backend reads database settings from environment variables.

## Schema File

The schema is located at:

```text
database/schema.sql
```

It creates these tables:

- `keywords`
- `search_analytics`

## Apply Schema

```bash
mysql -u <your_mysql_user> -p < database/schema.sql
```

Example:

```bash
mysql -u root -p < database/schema.sql
```

## Backend Environment Variables

`backend/src/main/resources/application.yml` uses:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/keyword_generator?...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

For local PowerShell:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your_mysql_password>"
```

Then start the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## Verify Tables

```sql
USE keyword_generator;
SHOW TABLES;
DESC keywords;
DESC search_analytics;
```

Expected tables:

```text
keywords
search_analytics
```
