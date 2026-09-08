# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the parent POM first so Maven can resolve the multi-module structure
COPY pom.xml /build/pom.xml

# Copy this module's POM and source
COPY user-service/pom.xml /build/user-service/pom.xml
COPY user-service/src /build/user-service/src

WORKDIR /build/user-service
RUN mvn -f pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /build/user-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]