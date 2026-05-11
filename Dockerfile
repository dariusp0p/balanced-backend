# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# 1) Copy only pom.xml first, so dependencies are cached
COPY pom.xml .

# 2) Cache Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 3) Copy source code only after dependencies
COPY src ./src

# 4) Build using the same Maven cache
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]