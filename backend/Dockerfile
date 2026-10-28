# ---------- Build stage ----------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Dépendances d'abord (cache Docker tant que le pom ne change pas)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Utilisateur non-root ; les photos sont stockées dans un volume
RUN addgroup -S atelier && adduser -S atelier -G atelier \
  && mkdir -p /app/uploads/photos && chown -R atelier:atelier /app
USER atelier

COPY --from=build /workspace/target/*.jar /app/app.jar

ENV APP_STORAGE_PHOTOS_DIRECTORY=/app/uploads/photos
VOLUME /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
