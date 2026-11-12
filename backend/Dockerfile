FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app
COPY pom.xml .

RUN mvn dependency:go-offline

FROM deps AS build
COPY src ./src
RUN mvn -B clean verify

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8080