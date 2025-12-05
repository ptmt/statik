# statik - Static web-site generator in Kotlin

A fast, simple static site generator built with Kotlin that works out of the box with minimal setup.

## Quick Start

1. Create a `config.json`:
```json
{
  "siteName": "My Blog",
  "baseUrl": "https://example.com",
  "description": "My awesome blog",
  "author": "Your Name"
}
```

2. Add some content in `posts/` or `content/`:
```markdown
---
title: Hello World
---

# Hello World

This is my first post!
```

3. Generate your site:
```bash
./amper run -- --root-path .
```

That's it! No templates required - Statik includes clean, minimal built-in templates to get you started.

## Humans and agents contribution guidelines

This README provides quick guidelines for both human contributors and AI coding assistants.

## Build & Run Commands
- Build: `./amper`
- Run: `./amper run`
- Test: `./amper test`
- Single test: `./amper test --tests <TestClassName>` or `./amper test --tests <TestClassName.testMethodName>`
- Build and watch a documentation website: `./amper run -- --root-path=./documentation --w`
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
- `content/` or `posts/` directory with Markdown files
- `templates/` directory with Handlebars templates (optional - see below)

### Templates (Optional)

Statik includes built-in fallback templates, so you can start generating sites immediately without creating any templates. However, you can customize the look by creating your own `.hbs` templates:

- **`templates/home.hbs`** - Homepage template (shows list of posts and pages)
- **`templates/post.hbs`** - Individual blog post template
- **`templates/page.hbs`** - Static page template

If a template file doesn't exist, Statik will use a clean, minimal built-in template instead.

#### Template Variables Available:
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
  },
  "devServer": {
    "port": 3000
  },
  "staticDatasource": {
    "outputDir": "datasource",
    "collectAttribute": "data-collect",
    "imagesFileName": "images.json"
  },
  "rss": {
    "enabled": true,
    "fileName": "feed.xml",
    "maxItems": 20,
    "includeFullContent": true
  },
  "footnotes": {
    "display": "HOVER"
  }
}
```

- `staticDatasource.outputDir`: Directory (inside `theme.output`) for generated datasource JSON files.
- `staticDatasource.collectAttribute`: Attribute used to mark custom collectable elements (default `data-collect`).
- `staticDatasource.imagesFileName`: File name for the aggregated images list (default `images.json`).
- `staticDatasource.configFile`: Optional dataset definition file (default `datasource-config.json`).
- `rss.enabled`: Enable RSS feed generation (default `true`).
- `rss.fileName`: RSS feed filename (default `feed.xml`).
- `rss.maxItems`: Maximum number of posts in the RSS feed (default `20`).
- `rss.includeFullContent`: Include full post content in RSS feed (default `true`).
- `footnotes.display`: `LIST` renders classic reference lists while `HOVER` collapses them into inline tooltips.

## RSS Feed

Statik automatically generates an RSS feed for your blog posts. The feed is enabled by default and can be customized in `config.json`:

- **Enabled by default**: RSS feed is generated automatically at `build/feed.xml`
- **Configurable**: Customize filename, max items, language, and whether to include full content
- **RSS 2.0 compliant**: Compatible with all major feed readers
- **Automatic descriptions**: Uses post metadata or excerpt from content

To disable RSS generation, set `"enabled": false` in the `rss` config section.

## Static Datasources

Statik can emit JSON datasources alongside the generated HTML so client-side code can hydrate dynamic widgets:
- Images referenced in Markdown and HTML content are exported automatically to `/<outputDir>/<imagesFileName>`.
- Add `data-collect="quotes"` (or any value) to an element to collect it into `/<outputDir>/quotes.json`. Additional `data-*` attributes (e.g. `data-author`) are preserved in the JSON output.
- Configure custom datasets in `datasource-config.json` to pull standalone entities from folders or tag existing posts/pages via metadata keys.
- Access everything in templates through the `datasource` context (e.g. `{{datasource.images}}`, `{{datasource.entities.team}}`) for server-side rendering.

Each datasource entry includes its originating page/post metadata and the rendered HTML/text, making it simple to build interactive components.

Read the full guide in [documentation/pages/static-datasources.md](documentation/pages/static-datasources.md).

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
