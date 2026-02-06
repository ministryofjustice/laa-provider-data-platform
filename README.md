# laa-provider-data-platform

[![Ministry of Justice repository compliance badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-provider-data-platform/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-provider-data-platform)

## Overview

The **provider data platform** implements a REST API for retrieving and updating provider firm, office, contract and schedule data.

### Project structure

Includes the following subprojects:

- `provider-data-api` - OpenAPI specification used for generating API stub interfaces and 
  documentation.
- `provider-data-service` - REST API service with CRUD operations interfacing a JPA repository
  with an in-memory database.
- `provider-data-e2e` - End-to-end tests to target a running system.

## Build and run the application

- Build the application: `./gradlew clean build`
- Run integration tests: `./gradlew integrationTest`
- Run the application: `./gradlew bootRun`
- Build application container: `./gradlew bootBuildImage`
- Run end-to-end tests: `./gradlew :provider-data-e2e:e2eTest -Penv=local -Dauth.token=Dummy1`

## Application endpoints

### REST API documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI specification (JSON): http://localhost:8080/v3/api-docs
- Test create: `curl -X POST http://localhost:8081/api/v1/items -H "Content-Type: application/json" -d '{"name":"Laptop","description":"Dell XPS"}'`
- Test retrieve: `curl http://localhost:8081/api/v1/items`

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

### Remaining setup

<details>
<summary>Click here for more details of these tasks</summary>

1. Ensure that this repository has been added to the
   [Legal Aid Agency Snyk](https://app.snyk.io/org/legal-aid-agency) organisation.
2. Update `build.gradle` in the project root directory as follows:
   ```
   subprojects {
       group = 'uk.gov.justice.laa.{application-name}'
   }
   ```
3. Ensure the GitHub workflows are working
4. Move the techdocs across (as simple `topic.md` files)
</details>

### Libraries used

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) - used to provide various endpoints to help monitor the application, such as view application health and information.
- [Spring Boot Web](https://docs.spring.io/spring-boot/reference/web/index.html) - used to provide features for building the REST API implementation.
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html) - used to simplify database access and interaction, by providing an abstraction over persistence technologies, to help reduce boilerplate code.
- [Springdoc OpenAPI](https://springdoc.org/) - used to generate OpenAPI documentation. It automatically generates Swagger UI, JSON documentation based on your Spring REST APIs.
- [Lombok](https://projectlombok.org/) - used to help to reduce boilerplate Java code by automatically generating common
  methods like getters, setters, constructors etc. at compile-time using annotations.
- [MapStruct](https://mapstruct.org/) - used for object mapping, specifically for converting between different Java object types, such as Data Transfer Objects (DTOs)
  and Entity objects. It generates mapping code at compile code.
- [H2](https://www.h2database.com/html/main.html) - used to provide an example database and should not be used in production.
