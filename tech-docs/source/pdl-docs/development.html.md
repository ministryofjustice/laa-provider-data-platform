---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdl-docs/development.html.md
title: Development
weight: 5
---

# Development guide

## Local development setup

### Prerequisites

- Java 25
- IntelliJ IDEA (recommended)
- Gradle 9.3.0 (optional, as gradle-wrapper is used)
- Docker Desktop
- Git

### IDE setup (IntelliJ IDEA)

1. **Open the project:**
   - File → Open → Select the repository directory

2. **Configure Java:**
   - Ensure Java 25 is configured
   - Settings → Project Structure → Project → SDK → Select Java 25

3. **Enable Lombok:**
   - Settings → Plugins → Install Lombok Plugin
   - Settings → Compiler → Annotation Processors → Enable annotation processing

4. **Link Gradle project:**
   - Right-click on `build.gradle` → Link Gradle project

5. **Configure Run Configuration:**
   - Create a Spring Boot run configuration

## Building and testing

### Build

```bash
./gradlew clean build
```

### Run unit tests

```bash
./gradlew test
```

### Run integration tests

```bash
./gradlew integrationTest
```

### Run all tests

```bash
./gradlew clean build integrationTest
```

## Code quality

### Spotless formatting

The project uses Spotless for automatic code formatting.

```bash
# Apply formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Diagnose formatting issues
./gradlew spotlessDiagnose --info
```

### Checkstyle validation

```bash
# Run Checkstyle on main code
./gradlew checkstyleMain

# Run Checkstyle on test code
./gradlew checkstyleTest

# Run all checks
./gradlew check
```

### Pre-commit hooks

Pre-commit hooks ensure code quality on commit:

```bash
# Setup hooks
prek install

# Run all checks manually
prek run --all-files
```

## Running the application

### Using Gradle

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Database

### Local database

The application uses Testcontainer Oracle databases for local development:

- **CCMS Database**: Automatically created on startup
- **CWA Database**: Automatically created on startup

No manual setup is required.

## Debugging

### Debug mode in IntelliJ

1. Click the Debug icon (Shift + F9)
2. Set breakpoints in the code
3. Step through execution

### Remote debugging

To enable remote debugging, set JVM arguments:

```bash
export JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
./gradlew bootRun
```

## Common issues

### "prek: command not found"

Install `prek`:

```bash
brew install prek
```

### "Docker daemon is not running"

Start Docker Desktop and retry.

### "Permission denied" on hooks

Make hooks executable:

```bash
chmod +x .git/hooks/pre-commit
```

## Project structure

```
laa-data-provider-data/
├── providers-app/           # Main application
│   ├── src/
│   │   ├── main/
│   │   ├── test/
│   │   └── integrationTest/
│   └── build.gradle
├── tech-docs/               # Technical documentation
├── helm_deploy/             # Kubernetes deployment configs
├── scripts/                 # Utility scripts
└── build.gradle             # Root Gradle build file
```

## Committing code

Follow conventional commit format:

```bash
git commit -m "feat: DSTEW-1234 Add new feature"
git commit -m "fix: DSTEW-5678 Fix bug"
git commit -m "docs: Update README"
```

Valid types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`, `infra`

## Next steps

- See [Configuration](configuration.html) for environment setup
- See [API reference](api-reference.html) for endpoint documentation
- See [Deployment](deployment.html) for production deployment
