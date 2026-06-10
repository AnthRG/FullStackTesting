# --- Etapa de build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cachea dependencias de Gradle antes de copiar el código
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# --- Etapa de runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
