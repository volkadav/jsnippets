# Application Configuration Guide

## Overview

The application uses Spring Boot profiles to manage different configurations for different environments. The configuration follows a production-safe default approach.

## Configuration Files

### 1. `application.properties` (Default/Production-Safe)

This is the base configuration file with **production-safe defaults**:
- `spring.jpa.show-sql=false` - SQL queries are NOT logged
- `logging.level.root=INFO` - Standard logging level
- `spring.thymeleaf.cache=true` - Templates are cached for performance
- `spring.jpa.open-in-view=false` - Prevents lazy loading issues

**Use case**: Production deployments, or when no profile is specified.

### 2. `application-development.properties`

Development-friendly configuration that overrides the defaults:
- `spring.jpa.show-sql=true` - SQL queries ARE logged
- `spring.jpa.properties.hibernate.format_sql=true` - SQL is formatted for readability
- `logging.level.com.norrisjackson.jsnippets=DEBUG` - Verbose logging for application code
- `logging.level.org.springframework.web=DEBUG` - Verbose Spring Web logging
- `logging.level.org.hibernate.SQL=DEBUG` - Hibernate SQL logging
- `spring.thymeleaf.cache=false` - Template hot-reloading enabled

**Use case**: Local development and debugging.

### 3. `application-prod.properties`

Explicit production configuration (same as defaults but explicit):
- `spring.jpa.show-sql=false` - No SQL logging
- `logging.level.root=INFO` - Minimal logging
- `logging.level.org.springframework.web=WARN` - Reduced Spring logging
- `server.compression.enabled=true` - HTTP compression
- `server.http2.enabled=true` - HTTP/2 support

**Use case**: Production deployments where you want to be explicit about settings.

### 4. `application-test.properties`

Test-specific configuration:
- Uses in-memory H2 database
- Minimal logging output during tests
- Liquibase disabled (uses JPA ddl-auto instead)

**Use case**: Automatically activated during JUnit tests.

## How to Use

### Development Mode

```bash
# Option 1: Set as JVM argument
./mvnw spring-boot:run -Dspring-boot.run.profiles=development

# Option 2: Set as environment variable
export SPRING_PROFILES_ACTIVE=development
./mvnw spring-boot:run

# Option 3: In IDE, set VM options
-Dspring.profiles.active=development
```

### Production Mode

```bash
# Option 1: Use default (no profile needed)
java -jar jsnippets.jar

# Option 2: Explicitly set prod profile
java -jar jsnippets.jar --spring.profiles.active=prod

# Option 3: Set as environment variable
export SPRING_PROFILES_ACTIVE=prod
java -jar jsnippets.jar
```

### Running Tests

```bash
# Tests automatically use the 'test' profile
./mvnw test
```

### Docker/Container Deployment

```bash
# Set via environment variable
docker run -e SPRING_PROFILES_ACTIVE=prod your-image:tag
```

## Configuration Priority

Spring Boot applies configurations in this order (later overrides earlier):

1. `application.properties` (base defaults)
2. Profile-specific file (e.g., `application-development.properties`)
3. Environment variables
4. Command-line arguments

## Environment Variables for Production

For production deployments, use environment variables for sensitive data:

```bash
export PG_HOST=your-db-host
export PG_PORT=5432
export PG_DB=jsnippets_prod
export PG_USER=jsnippets_user
export PG_PASS=your-secure-password
```

## Key Differences Summary

| Setting | Default/Prod | Development | Test |
|---------|--------------|-------------|------|
| SQL Logging | ❌ Off | ✅ On | ❌ Off |
| SQL Formatting | ❌ Off | ✅ On | ❌ Off |
| Log Level | INFO | DEBUG | WARN |
| Thymeleaf Cache | ✅ On | ❌ Off | N/A |
| Database | PostgreSQL | PostgreSQL | H2 (in-memory) |
| Liquibase | ✅ On | ✅ On | ❌ Off |

## Best Practices

1. **Never commit sensitive data** - Use environment variables for passwords and secrets
2. **Default to production-safe** - The application is safe to run without a profile
3. **Use development profile locally** - Get helpful debugging output
4. **Keep test profile minimal** - Fast test execution with minimal output
5. **Document environment variables** - Make deployment easier for operations teams

## Troubleshooting

### Check which profile is active:
```bash
# Look for this line in startup logs:
# "The following 1 profile is active: development"
```

### Override specific properties:
```bash
# Override individual properties via command line
java -jar jsnippets.jar --logging.level.root=DEBUG --spring.jpa.show-sql=true
```

### Verify configuration:
```bash
# Use Spring Boot Actuator (if enabled)
curl http://localhost:8080/actuator/env
```

