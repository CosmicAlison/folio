# syntax=docker/dockerfile:1.7
# A single web image for hosts such as Render: React and /api share one origin.
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN --mount=type=cache,id=folio-npm,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN --mount=type=cache,id=folio-maven,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN --mount=type=cache,id=folio-maven,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S folio && adduser -S -G folio folio
WORKDIR /app
COPY --from=backend --chown=folio:folio /app/target/*.jar app.jar
USER folio
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
