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
- Test Docker image: `./test-docker.sh` (comprehensive) 

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

### Building Locally

To build the Docker image for your platform:

```bash
# Build for current platform (recommended)
docker build --platform linux/$(uname -m) -t statik .

# Or build for specific platform
docker build --platform linux/amd64 -t statik .     # Intel/AMD
docker build --platform linux/arm64 -t statik .     # Apple Silicon/ARM
```

### Running with Docker

To generate a static website from your current directory:

```bash
docker run --rm -v $(pwd):/github/workspace ghcr.io/ptmt/statik:latest run -- --root-path .
```

To build and watch a documentation website:

```bash
docker run --rm -v $(pwd):/github/workspace -p 8080:8080 ghcr.io/ptmt/statik:latest run -- --root-path . --w
```

**Note**: Your project directory must contain:
- `config.json` with site configuration
- `templates/` directory with Handlebars templates (see below)
- `content/` or `posts/` directory with Markdown files

### Required Templates

Your `templates/` directory must include these Handlebars (.hbs) templates:

- **`home.hbs`** - Homepage template (shows list of posts and pages)
- **`post.hbs`** - Individual blog post template
- **`page.hbs`** - Static page template (optional)

Template variables available:
- `home.hbs`: `siteName`, `description`, `baseUrl`, `posts[]`, `pages[]`
- `post.hbs`: `post` (title, content, date, metadata), `siteName`, `baseUrl`, `pages[]`
- `page.hbs`: `page` (title, content, metadata), `siteName`, `baseUrl`, `pages[]`

You can also use partials in `templates/partials/` for shared components like headers and footers.

### Configuration

Example `config.json`:

```json
{
  "siteName": "My Blog",
  "baseUrl": "https://example.com",
  "description": "A static site built with Statik",
  "author": "Your Name",
  "theme": {
    "templates": "templates",
    "assets": "static",
    "output": "build"
  },
  "paths": {
    "posts": "posts",
    "pages": "content"
  }
}
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

