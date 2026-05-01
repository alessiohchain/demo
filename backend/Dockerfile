# syntax=docker/dockerfile:1.7
# Backend container — preferred build is `./mvnw spring-boot:build-image` (Buildpacks).
# This Dockerfile is provided for docker-compose convenience and CI portability.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw mvnw.cmd pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package && \
    mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
ARG DEPENDENCY=/workspace/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -cp /app:/app/lib/* za.co.csnx.demo.DemoBackendApplication"]
