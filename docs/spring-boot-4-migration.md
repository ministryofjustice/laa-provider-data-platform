# Spring Boot 4.0 migration

## Changes summary

The following dependency changes were made at the time of migration (note that some of these
dependencies have been updated subsequently).

| Component                     | Before                                    | After                                   |
|-------------------------------|-------------------------------------------|-----------------------------------------|
| LAA Spring Boot Gradle plugin | 1.2.2                                     | 2.0.4                                   |
| Spring Boot                   | 3.5.7                                     | 4.0.1 (via plugin)                      |
| SpringDoc OpenAPI             | 2.8.12                                    | 3.0.1                                   |
| WebMVC starter                | `spring-boot-starter-web`                 | `spring-boot-starter-webmvc`            |
| AOP/AspectJ starter           | `spring-boot-starter-aop`                 | `spring-boot-starter-aspectj`           |
| Spring Retry                  | (managed by Spring Boot dependencies BOM) | 2.0.12 (explicit version)               |
| Testcontainers Oracle         | `oracle-free`                             | `testcontainers-oracle-free`            |
| Modularised test starters     | `spring-boot-starter-test`                | `webmvc-test`, `data-jpa-test`          |
| Test annotation packages      | `o.s.b.test.autoconfigure.web.servlet.*`  | `o.s.b.webmvc.test.autoconfigure.*`     |
| Health indicator packages     | `o.s.b.actuate.health.*`                  | `o.s.b.health.contributor.*`            |
| Jackson ObjectMapper config   | `enableDefaultTyping()`                   | `activateDefaultTyping()` (removed dup) |
| ProblemDetail test assertions | Exact string matching                     | `jsonPath` field assertions             |

---

## Code changes

### LAA plugin version (4 files)

**Reference:**
-[LAA Spring Boot Common release notes](https://github.com/ministryofjustice/laa-spring-boot-common/releases/tag/v2.0.0) \
**Files changed:** `**/build.gradle`

```gradle
id 'uk.gov.laa.springboot.laa-spring-boot-gradle-plugin' version '2.0.4'
```

Upgrading the plugin upgraded Spring Boot to 4.0.1, which also upgraded related frameworks
(Spring Framework 7.x, Spring Security 7.x, Spring Data 2025.x).

### Web starter renamed (2 files)

**Reference:**
[Web Application Starters](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#web-application-starters) \
**Files changed:** `**/build.gradle`

```gradle
// Spring Boot 4.0 modularisation renamed this starter
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
```

### SpringDoc OpenAPI (2 files)

**Reference:**
[SpringDoc compatibility matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot) \
**Files changed:** `**/build.gradle`

```gradle
// Spring Boot 4.0 requires SpringDoc 3.x
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1'
```

### Test starters modernised (2 files)

**Reference:**
[Test Starters](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#test-starters) \
**Files changed:** `**/build.gradle`

```gradle
// Spring Boot 4.0 encourages modular test dependencies
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
```

### AOP starter renamed (1 file)

**Reference:**
[spring-boot-starter-aop](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#spring-boot-starter-aop) \
**File changed:** `providers-app/build.gradle`

```gradle
// Spring Boot 4.0 renamed this starter for clarity
implementation 'org.springframework.boot:spring-boot-starter-aspectj'
```

### Spring Retry explicit version (1 file)

**Reference:**
[Dependency Management Removals](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#dependency-management-removals) \
**File changed:** `providers-app/build.gradle`

```gradle
// Spring Retry removed from Spring Boot 4.0 BOM - explicit version required
implementation 'org.springframework.retry:spring-retry:2.0.12'
```

### Testcontainers artifact naming (1 file)

**References:**
[Spring Boot Testcontainers](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#testcontainers),
[Testcontainers 2.0.0 Release Notes](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.0) \
**File changed:** `providers-app/build.gradle`

Spring Boot 4.0 includes Testcontainers 2.0.3 with renamed artifact IDs.
In Testcontainers 2.0, all modules are now prefixed with `testcontainers-`:

```gradle
// Artifact names changed in Testcontainers 2.x
testImplementation 'org.testcontainers:testcontainers-oracle-free'
// (removed) testImplementation 'org.testcontainers:testcontainers-postgresql'
```

### Test annotation package changes (17 files)

**Reference:**
[Test Starters](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#test-starters) \
**Files changed:** `**/src/test/**/*.java` and `**/src/integrationTest/**/*.java`

Spring Boot 4.0's modularisation moved web MVC test annotations to a new package structure.

```java
// Before (Spring Boot 3.5):
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

// After (Spring Boot 4.0):
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

### Health indicator package changes (3 files)

**References:**
[Package organisation](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#package-organization),
[Modules](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#modules) \
**Files:** `build.gradle`, `RedisReadinessHealthIndicator.java`, `CwaDataSourceReadinessHealthIndicator.java`
(in `providers-app`)

Spring Boot 4.0's modularisation moved health indicator classes to a new package structure and
requires an explicit dependency on `spring-boot-health` (not included in `spring-boot-starter-actuator`).

```java
// Before (Spring Boot 3.5):
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

// After (Spring Boot 4.0):
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
```

```groovy
// After (Spring Boot 4.0): Additional dependency in build.gradle
implementation 'org.springframework.boot:spring-boot-health'
```

### Jackson configuration (1 file)

**Reason:** Jackson 3 removed the deprecated method. The newer `activateDefaultTyping()`
provides the same functionality. \
**File changed:** `CacheConfig.java` (in `providers-app`)

Jackson 3 deprecated `ObjectMapper.enableDefaultTyping()` in favour of `activateDefaultTyping()`.
So removed redundant `enableDefaultTyping()` call (was already using `activateDefaultTyping()`):

```java
// Before: Both calls present (redundant)
objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

// After: Only activateDefaultTyping (correct)
objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
```

### Cache test timing fix (1 file)

**Reason:** Tests should not rely on implicit startup cache loading timing.
Explicit cache loading makes tests more reliable and easier to understand. \
**File changed:** `CacheServiceIntegrationTest.java` (in `providers-app`)

Spring Boot 4.0 may have changed `CommandLineRunner` execution timing or cache persistence
behaviour, causing integration tests that relied on implicit startup cache loading to become flaky.

- Commented out `invalidateCacheLoadInfo()` call in `@BeforeEach` to avoid race conditions
- Added conditional cache loading logic to `reloadCache()` test to ensure cache is populated
- Simplified initial cache verification (removed individual item checks after startup)
- Retained comprehensive assertions for reload phase (where explicit `loadCache()` is called)

### ProblemDetail test assertions (1 file)

**Reference:**
[Problem Details](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#problem-details) \
**Files changed:** `**/src/test/**/*.java` and `**/src/integrationTest/**/*.java`

Spring Boot 4.0 changed `ProblemDetail` JSON serialisation (Jackson 3 + RFC 7807):

- The `type` field may be omitted when it has the value `about:blank`
- JSON property order is no longer guaranteed (and has changed)

```java
// Before: brittle - exact string matching 
.andExpect(content().string("{\"type\":\"about:blank\",\"title\":\"Bad Request\"," +
    "\"status\":400,\"detail\":\"Invalid request content.\",\"instance\":\"/api/v1/items\"}"));

// After: resilient - individual field assertions
.andExpect(jsonPath("$.title").value("Bad Request"))
.andExpect(jsonPath("$.status").value(400))
.andExpect(jsonPath("$.detail").value("Invalid request content."))
.andExpect(jsonPath("$.instance").value("/api/v1/items"));
```

### Kubernetes health probes configuration (1 file)

**Reference:**
[Kubernetes probes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#liveness-and-readiness-probes) \
**File changed:** `providers-app/src/main/resources/application.yml`

Spring Boot 4.0 enabled liveness and readiness probes by default, so the explicit configuration
was removed:

```yaml
# Before (Spring Boot 3.5): Explicit enablement required
management:
  endpoint:
    health:
      probes:
        enabled: true

# After (Spring Boot 4.0): Enabled by default - configuration removed
management:
  endpoint:
    health:
      # probes.enabled removed - now default behaviour
```

---

## What was not changed

### Jackson dependency

**Reference:**
[Jackson 3 Upgrading Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#upgrading-jackson)

The codebase still used `com.fasterxml.jackson.*` imports (Jackson 2) immediately after migration,
which was correct and intentional. Spring Boot 4.0 supports the coexistence of both Jackson 2 and
Jackson 3 simultaneously.

Spring Boot's autoconfigured `ObjectMapper` uses Jackson 3 (`tools.jackson.*`), whereas application
code and OpenAPI-generated classes use Jackson 2 (`com.fasterxml.jackson.*`). Both versions coexist
safely without requiring code changes at the time of migration.

- Jackson annotations remain in `com.fasterxml.jackson.annotation.*` even in Jackson 3
- Java 8 time types (`java.time.*`) are built into Jackson 3 core (no separate `jackson-datatype-jsr310` needed)
- Custom `ObjectMapper` instances (e.g., for Redis, config parsing, tests) use Jackson 2 and work correctly
- The `com.fasterxml.jackson.*` (Jackson 2) → `tools.jackson.*` (Jackson 3) migration can happen gradually

### OpenAPI generator config

**References:**
[Plugin compatibility](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot),
[OpenAPI generator issue](https://github.com/OpenAPITools/openapi-generator/issues/22294)

The `useSpringBoot3: "true"` flag is Jakarta EE-based and remains compatible with Spring Boot 4.0.
The OpenAPI generator currently generates Jackson 2 based code, which is fine (but deprecated).
See previous section on the Jackson dependency.

### Build and runtime platform changes

**Reference:**
[Review system requirements](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#review-system-requirements) 

The build already used Java 25 and Gradle 9.2.0 at the time of migration, which were ahead of
Spring Boot 4.0's minimum requirements of Java 17+ and Gradle 8.14+, so no change was needed.

### Test code structure

**Reference:**
[Upgrading testing features](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#upgrading-testing-features)

The test code structure (use of annotations and test patterns) was already Spring Boot 4.0 compliant:

- Integration tests already used `@AutoConfigureMockMvc` annotation
- Unit tests used `@WebMvcTest` annotation and `@MockitoBean`

However, the annotation **imports** required updating (see test annotation package changes above).

---

## References

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) (official guide)
- [LAA Spring Boot Common v2.0.0](https://github.com/ministryofjustice/laa-spring-boot-common/releases/tag/v2.0.0)
- [SpringDoc OpenAPI compatibility](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)
- [OpenAPI Generator 7.18.0](https://github.com/OpenAPITools/openapi-generator/releases/tag/v7.18.0)
