# Docker Deployment Guide

## Quick Start

### Prerequisites
- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- 512MB+ RAM available
- Ports 8080 and 5432 available (or configure custom ports)

### Deploy with Docker Compose

1. **Clone and navigate to the project:**
   ```bash
   cd /path/to/jsnippets
   ```

2. **Create environment file:**
   ```bash
   cp .env.example .env
   # Edit .env and set DB_PASSWORD to a secure value
   ```

3. **Start the application:**
   ```bash
   docker-compose up -d
   ```

4. **Check the logs:**
   ```bash
   docker-compose logs -f app
   ```

5. **Access the application:**
   ```
   http://localhost:8080
   ```

6. **Check health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Manual Docker Commands

### Build the Image
```bash
docker build -t jsnippets:latest .
```

### Run with External Database
```bash
docker run -d \
  --name jsnippets \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PG_HOST=your-db-host \
  -e PG_PORT=5432 \
  -e PG_DB=jsnippets \
  -e PG_USER=jsnippets \
  -e PG_PASS=your-secure-password \
  jsnippets:latest
```

### Run with Docker Network
```bash
# Create network
docker network create jsnippets-net

# Run PostgreSQL
docker run -d \
  --name jsnippets-db \
  --network jsnippets-net \
  -e POSTGRES_DB=jsnippets \
  -e POSTGRES_USER=jsnippets \
  -e POSTGRES_PASSWORD=password \
  -v jsnippets-data:/var/lib/postgresql/data \
  postgres:16-alpine

# Run application
docker run -d \
  --name jsnippets-app \
  --network jsnippets-net \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PG_HOST=jsnippets-db \
  -e PG_USER=jsnippets \
  -e PG_PASS=password \
  jsnippets:latest
```

## Docker Compose Commands

### Start Services
```bash
# Start in foreground
docker-compose up

# Start in background
docker-compose up -d

# Start and rebuild if needed
docker-compose up -d --build
```

### Stop Services
```bash
# Stop services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove containers AND volumes (deletes data!)
docker-compose down -v
```

### View Logs
```bash
# All services
docker-compose logs -f

# Just the app
docker-compose logs -f app

# Just the database
docker-compose logs -f db

# Last 100 lines
docker-compose logs --tail=100 -f app
```

### Execute Commands in Containers
```bash
# Open shell in app container
docker-compose exec app sh

# Check Java version
docker-compose exec app java -version

# Open PostgreSQL shell
docker-compose exec db psql -U jsnippets -d jsnippets
```

### Restart Services
```bash
# Restart all
docker-compose restart

# Restart just app
docker-compose restart app
```

### View Status
```bash
# List running containers
docker-compose ps

# View resource usage
docker stats jsnippets-app jsnippets-db
```

## Configuration Options

### Environment Variables

Create a `.env` file in the project root:

```env
# Application
APP_PORT=8080
SPRING_PROFILE=prod

# Database
DB_PORT=5432
DB_PASSWORD=your_secure_password_here

# JVM (optional)
JAVA_OPTS=-XX:MaxRAMPercentage=75.0 -Xlog:gc
```

### Available Profiles

- **prod** (recommended): Production mode with caching, minimal logging
- **development**: Development mode with SQL logging, debug output
- **test**: Test mode (not for Docker, used by test suite)

### Memory Configuration

Edit `docker-compose.yml` to adjust memory:

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          memory: 1G  # Maximum memory
        reservations:
          memory: 512M  # Minimum reserved
```

### Custom Ports

Edit `.env` file:
```env
APP_PORT=9090
DB_PORT=5433
```

## Production Deployment

### Using Docker Swarm

1. **Initialize Swarm:**
   ```bash
   docker swarm init
   ```

2. **Deploy Stack:**
   ```bash
   docker stack deploy -c docker-compose.yml jsnippets
   ```

3. **Scale the application:**
   ```bash
   docker service scale jsnippets_app=3
   ```

4. **View services:**
   ```bash
   docker stack services jsnippets
   ```

### Using Kubernetes

Create `k8s-deployment.yml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: jsnippets

---
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: jsnippets
type: Opaque
stringData:
  password: your-secure-password

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: jsnippets
data:
  SPRING_PROFILES_ACTIVE: "prod"
  PG_HOST: "jsnippets-db"
  PG_PORT: "5432"
  PG_DB: "jsnippets"
  PG_USER: "jsnippets"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jsnippets-app
  namespace: jsnippets
spec:
  replicas: 3
  selector:
    matchLabels:
      app: jsnippets
  template:
    metadata:
      labels:
        app: jsnippets
    spec:
      containers:
      - name: jsnippets
        image: jsnippets:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: app-config
        env:
        - name: PG_PASS
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: jsnippets-service
  namespace: jsnippets
spec:
  selector:
    app: jsnippets
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

Deploy:
```bash
kubectl apply -f k8s-deployment.yml
```

## Backup and Restore

### Backup Database
```bash
# Backup to file
docker-compose exec db pg_dump -U jsnippets jsnippets > backup.sql

# Or with timestamp
docker-compose exec db pg_dump -U jsnippets jsnippets > backup-$(date +%Y%m%d-%H%M%S).sql
```

### Restore Database
```bash
# Stop the app first
docker-compose stop app

# Restore from file
docker-compose exec -T db psql -U jsnippets jsnippets < backup.sql

# Restart app
docker-compose start app
```

### Backup Docker Volume
```bash
# Create volume backup
docker run --rm \
  -v jsnippets_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres-backup.tar.gz /data
```

## Monitoring

### View Metrics
```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# Memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### View Health
```bash
# Overall health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Disk space
curl http://localhost:8080/actuator/health/diskSpace
```

### Container Stats
```bash
# Real-time stats
docker stats jsnippets-app jsnippets-db

# One-time stats
docker stats --no-stream jsnippets-app
```

## Troubleshooting

### Container Won't Start
```bash
# Check logs
docker-compose logs app

# Check if database is ready
docker-compose logs db

# Restart services
docker-compose restart
```

### Database Connection Issues
```bash
# Verify database is healthy
docker-compose exec db pg_isready -U jsnippets

# Check connection from app
docker-compose exec app ping db

# Verify environment variables
docker-compose exec app env | grep PG_
```

### Out of Memory
```bash
# Check memory usage
docker stats jsnippets-app

# Increase memory limit in docker-compose.yml
# Or adjust JAVA_OPTS:
JAVA_OPTS=-XX:MaxRAMPercentage=50.0
```

### Port Already in Use
```bash
# Find what's using the port
sudo lsof -i :8080

# Change port in .env
echo "APP_PORT=9090" >> .env
docker-compose up -d
```

### Clean Start
```bash
# Stop and remove everything
docker-compose down -v

# Remove images
docker rmi jsnippets:latest

# Rebuild and start fresh
docker-compose up -d --build
```

## Security Best Practices

1. **Change Default Passwords:**
   - Always set `DB_PASSWORD` in `.env`
   - Never commit `.env` to git

2. **Use Secrets in Production:**
   - For Docker Swarm: Use Docker secrets
   - For Kubernetes: Use Kubernetes secrets
   - For cloud: Use cloud provider secret management

3. **Run Behind Reverse Proxy:**
   - Use nginx or Traefik for SSL/TLS
   - Enable rate limiting
   - Add security headers

4. **Regular Updates:**
   ```bash
   # Rebuild with latest base images
   docker-compose build --no-cache --pull
   docker-compose up -d
   ```

5. **Scan for Vulnerabilities:**
   ```bash
   # Using Docker scan
   docker scan jsnippets:latest
   
   # Using Trivy
   trivy image jsnippets:latest
   ```

## Performance Tuning

### Optimize for Low Memory
```yaml
services:
  app:
    environment:
      JAVA_OPTS: -XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC
```

### Optimize for High Throughput
```yaml
services:
  app:
    environment:
      JAVA_OPTS: -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Enable GC Logging
```yaml
services:
  app:
    environment:
      JAVA_OPTS: -XX:+UseG1GC -Xlog:gc*:file=/app/gc.log
```

## Additional Resources

- [Dockerfile Reference](https://docs.docker.com/engine/reference/builder/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [JVM Container Best Practices](https://developers.redhat.com/blog/2017/03/14/java-inside-docker)

