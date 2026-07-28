# --- Etapa de build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# --- Etapa de runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl es para el healthcheck del contenedor: en staging y prod no se publican
# puertos, asi que "compose up --wait" es la unica forma de saber si arranco.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.30.0/opentelemetry-javaagent.jar /app/otel-agent.jar
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/otel-agent.jar"
ENV OTEL_TRACES_EXPORTER=none \
    OTEL_METRICS_EXPORTER=none \
    OTEL_LOGS_EXPORTER=none

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
