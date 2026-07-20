# ---- Build stage ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Cache dependencies separately from source so a source-only change doesn't
# force a full dependency re-download.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# mvnw's executable bit doesn't reliably survive a checkout on Windows.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
# Tests need a live Postgres DB with DB_PASSWORD set, which isn't available
# at image build time - the test suite runs separately (locally/CI), not here.
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
