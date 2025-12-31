# jSnippets - make status updating easier

## history, reason for existence

Once upon a time I heard about something google did internally to make
status reporting easier, basically a small service that you'd send little
text updates -- aka "snippets" -- to (via cli, web ui, email, whatever) that'd
be collated periodically for your manager to view in lieu of having to have a
bunch of status update meetings. I've always thought that a sort of "status
twitter" would be potentially kind of a cool thing, even if using tech as a
hammer for social problems like communicating effectively has a track record
that is, at best, mixed.

So that ended up turning into this Spring Boot service. I'll probably
split out a separate client repo at some point once I get the basic
web ui and rest api stood up.

## outstanding TODOs

- cut a first release, i'm not completely embarrassed by it so i guess it's v1.0?

## future work ideas

- integrate with ticket systems like jira?
- fancier web reporting ui to view updates along an org chart tree?

## quickstart run

### local development

```bash
# 1. Create PostgreSQL database
createdb jsnippets
createuser jsnippets

# 2. Set environment variables
export SPRING_PROFILES_ACTIVE=development
export PG_HOST=localhost
export PG_USER=jsnippets
export PG_PASS=your_password

# 3. Run application
./mvnw spring-boot:run
```

### production jar

```bash
# With environment variables
export SPRING_PROFILES_ACTIVE=prod
export PG_HOST=prod-db-host
export PG_USER=jsnippets
export PG_PASS=secure_password

java -jar target/jsnippets-{version}.jar
```

### production docker

See [Docker Deployment](#docker-deployment) section above.

## configuration

The application uses Spring Boot profiles for environment-specific configuration:

- **Default/Production**: Production-safe defaults (`spring.jpa.show-sql=false`, `logging.level.root=INFO`)
- **Development**: Verbose logging with SQL output (`spring.jpa.show-sql=true`, DEBUG level)
- **Test**: In-memory H2 database for automated testing

### quick configuration

Set profile via environment variable or command line:
```bash
# Development mode (verbose logging)
export SPRING_PROFILES_ACTIVE=development
./mvnw spring-boot:run

# Production mode (uses secure defaults)
java -jar jsnippets.jar
```

### environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | - | Profile: `development`, `prod`, or `test` |
| `PG_HOST` | localhost | PostgreSQL host |
| `PG_PORT` | 5432 | PostgreSQL port |
| `PG_DB` | jsnippets | Database name |
| `PG_USER` | jsnippets | Database username |
| `PG_PASS` | password | Database password |

See [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md) for complete details.

## docker deployment

### quick start with docker compose (recommended)

```bash
# 1. Create environment file
cp .env.example .env

# 2. Set secure password in .env
echo "DB_PASSWORD=your_secure_password" >> .env

# 3. Build with dynamic metadata and start (podman or docker)
./build-image.sh
docker-compose up -d # or podman-compose up -d

# 4. Access application
open http://localhost:8080
```

### manual docker build and run

```bash
# Recommended: Use build script with dynamic OCI metadata
./build-image.sh

# Or build manually with dynamic labels
docker build \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg APP_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout) \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  -t jsnippets:latest .

# Run with external database
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PG_HOST=your-db-host \
  -e PG_USER=jsnippets \
  -e PG_PASS=your-password \
  jsnippets:latest
```

## package

### executable jar

```bash
mvn clean package
```

Find jar file in `target/jsnippets-{version}.jar`

### container image (dockerfile)

```bash
# Recommended: Use the build script for proper OCI metadata
./build-image.sh

# Build script options:
./build-image.sh --no-cache   # Force rebuild without cache
./build-image.sh --push       # Build and push to registry

# Or build manually with dynamic labels
docker build \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg APP_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout) \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  -t jsnippets:latest .

# Simple build (without metadata)
docker build -t jsnippets:latest .
```

The build script automatically:
- Detects podman or docker
- Extracts version from pom.xml
- Adds git commit hash (marks `-dirty` if uncommitted changes)
- Sets OCI-compliant image labels (version, created date, revision, etc.)
- Tags image with both version number and `latest`

Produces `jsnippets:latest` using a multi-stage Dockerfile (~350MB).

### alternative: spring boot buildpack

```bash
mvn clean spring-boot:build-image
```

Produces `jsnippets:latest` using Paketo buildpack (~400-500MB).

## run requirements

- jre 17+ (tested with 17, 21, and 25; docker images use 21)
- postgresql (anything recent, say 10+)
- mail server (optional)

## build requirements

- jdk 17+ (tested with 17, 21, and 25)
- maven 3

## documentation

Comprehensive guides are available for deployment and configuration:

- **[CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md)** - Complete configuration reference
  - Profile-based configuration (dev/prod/test)
  - Environment variables
  - Database configuration
  - Best practices

- **[DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)** - Docker deployment guide
  - Quick start with Docker Compose
  - Manual Docker commands
  - Kubernetes and Docker Swarm examples
  - Monitoring and troubleshooting
  - Backup and restore procedures

## license

Apache 2.0, copyright authors as noted in banner.txt
