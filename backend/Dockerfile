# ---------- Build stage ----------
  FROM eclipse-temurin:21-jdk-alpine AS build
  WORKDIR /workspace
  
  COPY . .
  RUN ./mvnw -DskipTests clean package
  
  # ---------- Runtime stage ----------
  FROM eclipse-temurin:21-jre-alpine
  WORKDIR /app
  
  COPY --from=build /workspace/target/*.jar /app/app.jar
  
  EXPOSE 8080
  ENTRYPOINT ["java","-jar","/app/app.jar"]
  