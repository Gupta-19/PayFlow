# Stage 1: build the jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline        # cached layer: re-runs only when the pom changes
COPY src ./src
RUN mvn -q package -DskipTests          # tests run in CI, not in the image build

# Stage 2: run it
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 payflow
# Matches only the Boot fat jar: the plain jar is renamed *.jar.original, which this glob skips
COPY --from=build /app/target/*.jar app.jar
USER payflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
