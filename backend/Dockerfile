# syntax=docker/dockerfile:1.7
# Backend container — fast variant that uses a pre-built jar from
# `./mvnw package`. Required for the consumer model: the shared engine
# artifacts (csnx-engine-spring / csnx-engine-ai) live only in the host
# ~/.m2, not Maven Central, so the jar must be built on the host first.
# Run `./mvnw package` before `docker compose up --build backend`.

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/demo-backend-*.jar /app/demo-backend.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/demo-backend.jar"]
