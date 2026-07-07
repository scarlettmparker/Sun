# syntax=docker/dockerfile:1
#
# Builds the sun-graphql gateway (which aggregates every domain component) into
# a runnable image. Build from the `components/` directory so the gradle build
# can see every subproject:
#
#   docker build -f sun-graphql/Dockerfile -t sun-graphql:dev .

FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle :sun-graphql:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
# curl is needed for the compose healthcheck (temurin ships no client by default).
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /home/gradle/project/sun-graphql/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
