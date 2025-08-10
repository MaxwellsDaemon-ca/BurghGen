# Dockerfile (at repo root)

# --- Build stage ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy backend project files
COPY burghgen/.mvn .mvn
COPY burghgen/mvnw mvnw
COPY burghgen/pom.xml pom.xml

# Ensure wrapper is executable and prefetch deps
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests=true clean dependency:go-offline

# Copy source and build
COPY burghgen/src ./src
RUN ./mvnw -q -DskipTests=true package

# --- Runtime stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Helpful memory defaults for small instances
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render provides PORT; Spring uses it (see application.properties step below)
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
