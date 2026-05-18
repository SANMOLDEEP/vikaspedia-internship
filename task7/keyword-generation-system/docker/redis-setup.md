# Redis Setup with Docker

## Quick Start Commands

### 1. Pull and Run Redis Container
```bash
docker run -d --name redis -p 6379:6379 redis:latest
```

### 2. Verify Redis is Running
```bash
docker ps
```

### 3. Test Redis Connection (Optional)
```bash
docker exec -it redis redis-cli ping
```

## Detailed Instructions

### Step 1: Run Redis Container
Open PowerShell/Command Prompt and run:
```bash
docker run -d --name redis -p 6379:6379 redis:latest
```

This will:
- Download Redis image if not present
- Start Redis container in detached mode
- Map port 6379 on host to port 6379 in container
- Name the container "redis"

### Step 2: Check Container Status
```bash
docker ps
```
You should see redis container running.

### Step 3: Start the Backend
Once Redis is running, start the backend:
```bash
mvnw.cmd spring-boot:run -Dmaven.test.skip=true
```

## Managing Redis Container

### Stop Redis
```bash
docker stop redis
```

### Start Redis (if stopped)
```bash
docker start redis
```

### Remove Redis Container
```bash
docker rm redis
```

### View Redis Logs
```bash
docker logs redis
```

## Troubleshooting

### Port Already in Use
If port 6379 is already in use:
```bash
docker run -d --name redis -p 6380:6379 redis:latest
```
Then update application.yaml to use port 6380.

### Container Won't Start
Check if port is available:
```bash
netstat -an | findstr 6379
```

### Redis Connection Issues
Test connection manually:
```bash
docker exec -it redis redis-cli ping
# Should return: PONG
```
