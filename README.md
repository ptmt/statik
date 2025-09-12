# statik - Static web-site generator in Kotlin

This README provides quick guidelines for both human contributors and AI coding assistants.

## Build & Run Commands
- Build: `./amper`
- Run: `./amper run`
- Single test: `./amper test --tests <TestClassName>` or `./amper test --tests <TestClassName.testMethodName>`
- Clean: `./amper clean`
- Run on real website: `./amper run --root-path=../ptmt.github.com`

## Code Style Guidelines
- **Package Structure**: Follow `com.potomushto.statik.*` namespace convention
- **Imports**: Group imports by package, separate standard library imports
- **Naming**:
  - Classes: PascalCase (e.g., `BlogEngine`)
  - Functions/Properties: camelCase (e.g., `generateSite()`)
  - Constants: SCREAMING_SNAKE_CASE
- **Error Handling**: Use Kotlin's `Result` type or exceptions with descriptive messages
- **Documentation**: Add KDoc comments for public APIs
- **Formatting**: 4-space indentation, 120-character line limit
- **Null Safety**: Prefer non-nullable types, use safe calls (`?.`) or the Elvis operator (`?:`) when needed
- **Testing**: Write unit tests for core functionality

## Dependencies
The project uses the Amper build system with dependencies defined in `module.yaml`.

## Ktor

Use Ktor version 3.
This is the correct way for static files to be declared in Ktor 3:

```kotlin
staticFiles("/", Paths.get(rootPath, config.theme.output).toFile()) { }
```

