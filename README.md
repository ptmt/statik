# statik - Static web-site generator in Kotlin

There's no description of how to use it or public documentation.

## Humans and agents contribution guidelines

This README provides quick guidelines for both human contributors and AI coding assistants.

## Build & Run Commands
- Build: `./amper`
- Run: `./amper run`
- Test: `./amper test`
- Single test: `./amper test --tests <TestClassName>` or `./amper test --tests <TestClassName.testMethodName>`
- Build and watch a documentation website: `./amper run -- --root-path=./statik.github.io --w`

## Code Style Guidelines
- **Package Structure**: Follow `com.potomushto.statik.*` namespace convention
- **Imports**: Group imports by package, separate standard library imports
- **Naming**:
  - Classes: PascalCase (e.g., `BlogEngine`)
  - Functions/Properties: camelCase (e.g., `generateSite()`)
  - Constants: SCREAMING_SNAKE_CASE
- **Documentation**: Add KDoc comments for public APIs
- **Null Safety**: Prefer non-nullable types, use safe calls (`?.`) or the Elvis operator (`?:`) when needed
- **Testing**: Write unit tests for core functionality

## Dependencies
The project uses the Amper build system with dependencies defined in `module.yaml`.

## Docker Usage

The project is published as a Docker image at `ghcr.io/ptmt/statik:latest`.

### Running with Docker

To generate a static website from your current directory:

```bash
docker run --rm -v $(pwd):/github/workspace ghcr.io/ptmt/statik:latest run -- --root-path .
```

To build and watch a documentation website:

```bash
docker run --rm -v $(pwd):/github/workspace -p 8080:8080 ghcr.io/ptmt/statik:latest run -- --root-path . --w
```

### GitHub Actions Usage

You can use this Docker image in GitHub Actions workflows:

```yaml
- name: Generate static site
  uses: docker://ghcr.io/ptmt/statik:latest
  with:
    args: run -- --root-path .
```

The Docker image contains the pre-built Statik application and all necessary dependencies.

## Ktor

Use Ktor version 3.
This is the correct way for static files to be declared in Ktor 3:

```kotlin
staticFiles("/", Paths.get(rootPath, config.theme.output).toFile()) { }
```

