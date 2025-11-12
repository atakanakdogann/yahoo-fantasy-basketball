FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:11-jre-alpine

WORKDIR /app
COPY --from=build /app/target/yahoo-fantasy-basketball-1.0.0.jar app.jar


ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
