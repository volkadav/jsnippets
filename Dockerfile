FROM amazoncorretto:17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="jsnippets"
LABEL org.opencontainers.image.name="norrisjackson.com/jsnippets"
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
