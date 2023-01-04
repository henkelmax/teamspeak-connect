FROM maven:3.8-eclipse-temurin-17-alpine as builder

COPY . .

RUN mvn install

FROM openjdk:17-jdk-slim

RUN mkdir /teamspeakconnect

COPY --from=builder target/teamspeakconnect.jar /teamspeakconnect/teamspeakconnect.jar

WORKDIR /teamspeakconnect

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "teamspeakconnect.jar"]
