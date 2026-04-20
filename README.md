# laa-provider-data-platform

[![Ministry of Justice repository compliance badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-provider-data-platform/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-provider-data-platform)

## Overview

The **provider data API (persistent)** implements a REST API for retrieving and updating provider
firm and office (and, later on, contract and schedule) data.

### Project structure

Includes the following subprojects:

- `provider-data-api` - OpenAPI specification used for generating API stub interfaces and 
  documentation.
- `provider-data-service` - REST API service with CRUD operations interfacing a JPA repository
  with an in-memory database.
- `provider-data-e2e` - End-to-end tests to target a running system.

## Build and run the application

This application uses Spring Boot Docker Compose support, which will automatically
start the PostgreSQL database service defined in `docker-compose.yaml` on startup.
Alternatively, you can start the database manually with `docker-compose up`.

Run locally by starting the Spring Boot `main` class in your IDE
(or use `./gradlew :provider-data-service:bootRun` from the CLI).

- Build the application: `./gradlew clean build`
- Run integration tests: `./gradlew integrationTest`
- Build application container: `./gradlew bootBuildImage`
- Run end-to-end tests: `./gradlew :provider-data-e2e:e2eTest -Penv=local -Dauth.token=Dummy1`

## Application endpoints

### REST API documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI specification (JSON): http://localhost:8080/v3/api-docs

### Actuator endpoints

The following actuator endpoints have been configured:
- http://localhost:8080/actuator
- http://localhost:8080/actuator/health

## Additional information

The project uses the `laa-spring-boot-gradle-plugin` Gradle plugin which provides sensible defaults
for the following plugins:

- [Checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)
- [Dependency Management](https://plugins.gradle.org/plugin/io.spring.dependency-management)
- [Jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [Java](https://docs.gradle.org/current/userguide/java_plugin.html)
- [Maven Publish](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Spring Boot](https://plugins.gradle.org/plugin/org.springframework.boot)
- [Test Logger](https://github.com/radarsh/gradle-test-logger-plugin)
- [Versions](https://github.com/ben-manes/gradle-versions-plugin)

You can find more information regarding the setup and usage of the Gradle plugin in
[laa-spring-boot-common](https://github.com/ministryofjustice/laa-spring-boot-common).

### Libraries used

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) - used
  to provide various endpoints to help monitor the application, such as view application health and
  information.
- [Spring Boot WebMVC](https://docs.spring.io/spring-boot/reference/web/index.html) - used to
  provide features for building the REST API implementation.
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html) - used to simplify
  database access and interaction, by providing an abstraction over persistence technologies, to
  help reduce boilerplate code.
- [Springdoc OpenAPI](https://springdoc.org/) - used to generate OpenAPI documentation. It
  automatically generates Swagger UI, JSON documentation based on your Spring REST APIs.
- [Lombok](https://projectlombok.org/) - used to help to reduce boilerplate Java code by
  automatically generating common methods like getters, setters, constructors etc. at compile-time
  using annotations.
- [MapStruct](https://mapstruct.org/) - used for object mapping, specifically for converting
  between different Java object types, such as Data Transfer Objects (DTOs) and Entity objects.
  It generates mapping code at compile code.
- [H2](https://www.h2database.com/html/main.html) - used to provide a database for testing only and
  should not be used in production.
