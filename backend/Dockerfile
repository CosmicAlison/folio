# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app
COPY pom.xml .

RUN --mount=type=cache,id=folio-maven,target=/root/.m2 mvn -B -ntp dependency:go-offline

FROM deps AS build
COPY src ./src
RUN --mount=type=cache,id=folio-maven,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S folio && adduser -S -G folio folio
WORKDIR /app
COPY --from=build --chown=folio:folio /app/target/*.jar app.jar
USER folio
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

EXPOSE 8080
