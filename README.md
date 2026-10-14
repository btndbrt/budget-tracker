# java_template

A minimal Maven starter for Java projects: compiles, runs, tests, and builds a runnable jar.

## Requirements

- JDK 21 (check with `java -version`)
- Maven 3.9+ (check with `mvn -version`)

## Usage

| Task | Command |
|------|---------|
| Run during development | `mvn -q compile exec:java` |
| Run tests | `mvn test` |
| Build the jar (also runs tests) | `mvn package` |
| Run the built jar | `java -jar target/*.jar` |
| Build and run in one go | `mvn package && java -jar target/*.jar` |
| Clean build output | `mvn clean` |

## Project structure

```
.
├── pom.xml
└── src
    ├── main/java/btndbrt/Main.java       # entry point
    └── test/java/btndbrt/MainTest.java   # JUnit 5 tests
```

## Starting a new project from this template

1. On GitHub, click **Use this template → Create a new repository**, then clone the new repo.
2. In `pom.xml`, change `artifactId` (and `groupId` if needed).
3. If you rename the package or main class, update the `exec.mainClass` property in `pom.xml` and move the folders under `src/main/java` and `src/test/java` to match.
4. Replace this README with a description of the new project.
