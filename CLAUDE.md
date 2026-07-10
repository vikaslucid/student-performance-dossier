# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a Spring Boot 3.5.16 / Java 17 project scaffold (`student-performance-dossier`). As of now it contains only the generated application skeleton — no entities, repositories, controllers, or services have been added yet. When implementing new functionality, there are no existing architectural conventions to follow beyond standard Spring Boot layering (controller / service / repository / entity), so establish clean structure as features are added.

## Commands

Build and run via the Maven wrapper (do not assume a global `mvn` install):

```
./mvnw clean compile      # compile
./mvnw test                # run all tests
./mvnw test -Dtest=StudentPerformanceDossierApplicationTests#contextLoads   # run a single test
./mvnw spring-boot:run     # run the application locally
./mvnw clean package       # build the executable jar
```

On Windows use `mvnw.cmd` instead of `./mvnw` if not in a POSIX shell.

## Configuration and running locally

- The app expects a PostgreSQL database at `jdbc:postgresql://localhost:5432/student_performance_dossier` (see `src/main/resources/application.yaml`).
- The DB password is read from the `DB_PASSWORD` environment variable — it must be set before running the app or any test that boots the full Spring context.
- `spring.jpa.hibernate.ddl-auto` is set to `update`, so schema changes are auto-applied from entities on startup; there are no migration tool (Flyway/Liquibase) files in the repo.

## Dependencies of note

- `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-boot-starter-validation`
- `postgresql` driver (runtime only)
- `lombok` (optional, annotation-processor wired into both compile and test-compile Maven executions)
- `spring-boot-devtools` (runtime, optional)
