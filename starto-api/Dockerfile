# syntax=docker/dockerfile:1.4
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S starto && adduser -S starto -G starto
WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

# Firebase service account will be mounted as a secret/volume
# Do NOT bake it into the image
EXPOSE 8080

USER starto
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
