# syntax=docker/dockerfile:1.7
# Backend container — fast variant that uses a pre-built jar from
# `./mvnw package`. Required for the consumer model: the shared engine
# artifacts (csnx-engine-spring / csnx-engine-ai) live only in the host
# ~/.m2, not Maven Central, so the jar must be built on the host first.
# Run `./mvnw package` before `docker compose up --build backend`.

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Azure Monitor OpenTelemetry Java agent — inert unless
# APPLICATIONINSIGHTS_CONNECTION_STRING is set at runtime (Azure injects it;
# local/GCP runs never see the agent). Auto-instruments HTTP, JDBC, Log4j2
# and messaging for distributed traces.
ARG APPINSIGHTS_AGENT_VERSION=3.7.8
# --chmod: ADD-from-URL defaults to 600 root:root, unreadable by USER app.
ADD --chmod=444 https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPINSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APPINSIGHTS_AGENT_VERSION}.jar /app/appinsights-agent.jar

COPY target/demo-backend-*.jar /app/demo-backend.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","if [ -n \"$APPLICATIONINSIGHTS_CONNECTION_STRING\" ]; then JAVA_OPTS=\"$JAVA_OPTS -javaagent:/app/appinsights-agent.jar\"; fi; exec java $JAVA_OPTS -jar /app/demo-backend.jar"]
