# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy parent POM so Maven can resolve parent configuration
COPY pom.xml /build/pom.xml

# Copy API Gateway module POM and source
COPY api-gateway/pom.xml /build/api-gateway/pom.xml
COPY api-gateway/src /build/api-gateway/src

WORKDIR /build/api-gateway

RUN mvn -f pom.xml clean package -DskipTests


# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /build/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]