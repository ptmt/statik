# statik - Static web-site generator in Kotlin

![logo](https://potomushto.com/statik/images/statik.png)

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

To run the hosted CMS server:
```bash
./amper run -- --root-path . --cms
```

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
- `home.hbs`: `siteName`, `siteDescription`, `description`, `baseUrl`, `posts[]`, `pages[]`
- `post.hbs`: `post` (title, content, date, metadata), `siteName`, `siteDescription`, `description`, `baseUrl`, `pages[]`
- `page.hbs`: `page` (title, content, metadata), `siteName`, `siteDescription`, `description`, `baseUrl`, `pages[]`

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
  "cms": {
    "enabled": false,
    "basePath": "/__statik__/cms",
    "databasePath": ".statik/cms.db",
    "autoSyncOnSave": false,
    "sharedStylesheets": ["static/css/tokens.css"],
    "git": {
      "enabled": true,
      "remote": "origin",
      "branch": "main",
      "pushOnSync": false,
      "tokenEnv": "GITHUB_TOKEN",
      "authorName": "Statik CMS",
      "authorEmail": "cms@example.com"
    },
    "auth": {
      "enabled": true,
      "allowedUser": "potomushto",
      "clientId": "github-app-client-id",
      "clientSecretEnv": "GITHUB_CLIENT_SECRET",
      "callbackUrl": "https://cms.example.com/__statik__/cms/auth/github/callback",
      "appId": "123456",
      "appSlug": "statik-cms",
      "privateKeyPath": "keys/statik-cms.private-key.pem",
      "setupUrl": "https://cms.example.com/__statik__/cms/auth/github/setup",
      "scopes": ["repo", "read:user"],
      "sessionTtlDays": 30
    },
    "repo": {
      "enabled": true,
      "owner": "potomushto",
      "name": "my-blog",
      "branch": "main",
      "checkoutDir": ".statik/checkout"
    }
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
  "html": {
    "format": "BEAUTIFY",
    "indentSize": 2,
    "footnotes": {
      "display": "HOVER"
    }
  }
}
```

If you want the CMS shell to share typography, colors, or tokens with your site and preview, add those CSS asset source paths to `cms.sharedStylesheets`. Each path must point to a `.css` file inside one of the configured `theme.assets` directories. Statik will load those files before the built-in CMS stylesheet.

- `staticDatasource.outputDir`: Directory (inside `theme.output`) for generated datasource JSON files.
- `staticDatasource.collectAttribute`: Attribute used to mark custom collectable elements (default `data-collect`).
- `staticDatasource.imagesFileName`: File name for the aggregated images list (default `images.json`).
- `staticDatasource.configFile`: Optional dataset definition file (default `datasource-config.json`).
- `cms.enabled`: Starts the embedded CMS server when enabled in config or via `--cms`.
- `cms.basePath`: Route prefix for the editor UI and API (default `/__statik__/cms`).
- `cms.databasePath`: SQLite file used as the CMS index, dirty-state store, and persisted auth session store. Put it on persistent storage if you want sessions to survive deploys/restarts.
- `cms.autoSyncOnSave`: If `true`, each save also commits through the configured git sync path.
- `cms.git.remote`: Remote name to use for sync operations (default `origin`).
- `cms.git.branch`: Optional branch override for pushes. When omitted, the current checked-out branch is used.
- `cms.git.pushOnSync`: Push after each manual sync.
- `cms.git.tokenEnv`: Optional env var containing a GitHub token for authenticated push.
- `cms.auth.enabled`: Protect the CMS behind GitHub sign-in.
- `cms.auth.allowedUser`: The only GitHub login allowed to access the editor. Any other user gets `permissions denied`.
- `cms.auth.clientId`: GitHub App client id used for the user sign-in flow.
- `cms.auth.clientSecretEnv`: Env var containing the GitHub App client secret.
- `cms.auth.callbackUrl`: Sign-in callback URL registered in the GitHub App.
- `cms.auth.appId`: Numeric GitHub App id used to mint installation tokens.
- `cms.auth.appSlug`: GitHub App slug used to open the install screen.
- `cms.auth.privateKeyPath`: Private key path for the GitHub App PEM file.
- `cms.auth.setupUrl`: Setup URL registered in the GitHub App. This should point to `${cms.basePath}/auth/github/setup` on your host.
- `cms.auth.scopes`: Requested GitHub user scopes for the sign-in flow. `repo` plus `read:user` is the practical hosted default.
- `cms.auth.sessionTtlDays`: Sliding auth session lifetime in days. Each authenticated CMS request renews the session and cookie. Default `30`.
- `cms.repo.enabled`: Turn on managed repo checkout mode for hosted/container CMS deployments.
- `cms.repo.owner`: Repository owner for the single repo the CMS may edit.
- `cms.repo.name`: Repository name for the single repo the CMS may edit.
- `cms.repo.branch`: Optional branch to clone, pull, and push. When omitted, the remote default branch is used.
- `cms.repo.checkoutDir`: Working directory where Statik clones the configured repo.
- `rss.enabled`: Enable RSS feed generation (default `true`).
- `rss.fileName`: RSS feed filename (default `feed.xml`).
- `rss.maxItems`: Maximum number of posts in the RSS feed (default `20`).
- `rss.includeFullContent`: Include full post content in RSS feed (default `true`).
- `html.footnotes.display`: `LIST` renders classic reference lists while `HOVER` collapses them into inline tooltips.

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

## CMS

Statik now includes a lightweight CMS that mounts next to the generated site. In hosted mode it signs one allowed GitHub user in, verifies that the configured GitHub App is installed on one configured repository, clones that repo into a managed checkout, indexes the configured `posts/` and `pages/` folders into SQLite, serves a web editor, writes changes back to source files, regenerates affected pages, and can commit and push those source changes back to GitHub.

Typical flow:
- Run `./amper run -- --root-path . --cms` on the host config directory. This directory stores `config.json`, the SQLite file, the GitHub App private key, and the managed checkout.
- Open `http://localhost:3000/__statik__/cms`.
- Sign in with GitHub as `cms.auth.allowedUser`. Any other GitHub login gets `permissions denied` immediately.
- If the GitHub App is not yet installed on `cms.repo.owner/cms.repo.name`, the CMS sends you to the install flow for that app and repo.
- After installation, Statik clones the configured repo into `cms.repo.checkoutDir`, indexes it into SQLite, and opens the editor.
- Edit a post or page, save it, and let Statik rebuild the affected output.
- Use the CMS Preview link to view the development preview. It includes draft posts and is served under `cms.basePath`, so it uses the same auth protection as the CMS UI when `cms.auth.enabled` is on.
- Use `Commit Sync` to create a git commit for the dirty CMS-managed source files.

Git sync notes:
- The CMS commits only the edited content source files, not the generated `build/` output.
- In managed checkout mode, sync uses a GitHub App installation token scoped to the configured repository.
- `cms.git.tokenEnv` remains available as a fallback for non-managed local checkout mode.
- The checkout is created and refreshed by Statik; you do not need to mount a pre-existing repo into the container.

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
