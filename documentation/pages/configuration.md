---
title: config.json reference
nav_order: 2
---

# Configuration Reference

Every Statik project is driven by a `config.json` file located at the project root. This document describes each section of that file so you can tailor a site without digging through the source.

## Example `config.json`

```json
{
  "siteName": "My Website",
  "description": "A website built with Statik",
  "baseUrl": "https://mysite.com",
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
    "imagesFileName": "images.json",
    "configFile": "datasource-config.json"
  },
  "rss": {
    "enabled": true,
    "fileName": "feed.xml",
    "title": "My Website RSS Feed",
    "description": "Latest posts from my website",
    "language": "en-us",
    "maxItems": 20,
    "includeFullContent": true
  }
}
```

## Core Settings

- `siteName` *(required)* – Display name used across templates and metadata.
- `baseUrl` *(required)* – Fully qualified site URL. Statik uses this for canonical links and RSS items.
- `description` *(required)* – Default description applied to templates and feeds.
- `author` *(required)* – Author attribution for posts and metadata.

## Theme & Output Paths

The `theme` block controls where Statik looks for templates and static assets, as well as the destination folder for generated files.

- `templates` – Directory containing Handlebars templates (`templates` by default).
- `assets` – Directory for static files copied verbatim to the output (`static` by default).
- `output` – Build directory that receives generated HTML and assets (`build` by default).

The `paths` block maps your source content folders:

- `posts` – Location of blog posts. Markdown, HTML, and `.hbs` are all supported (`posts`).
- `pages` – Location of standalone pages (`content`).

## Development Server

- `devServer.port` – Port used by `--watch` mode’s local server. Change this if 3000 conflicts with another service.

## Static Datasource Options

Statik can emit structured JSON for use in JavaScript or directly in Handlebars templates.

- `staticDatasource.outputDir` – Folder inside the build directory that receives datasource JSON (`datasource`).
- `staticDatasource.collectAttribute` – Attribute that marks custom collectable elements in content (`data-collect`).
- `staticDatasource.imagesFileName` – Filename for the aggregated image list (`images.json`).
- `staticDatasource.configFile` – Optional dataset definition file (defaults to `datasource-config.json`). See [Static Datasources](./static-datasources.md) for advanced collectors and template integration.

Set `staticDatasource.enabled` to `false` to skip datasource generation entirely.

## RSS Feed

The `rss` block manages feed generation. Omitted values fall back to sensible defaults.

- `enabled` – Toggle feed generation (`true`).
- `fileName` – Output filename (`feed.xml`).
- `title` – Feed title (defaults to `siteName`).
- `description` – Feed description (defaults to `description`).
- `language` – Language code (`en-us`).
- `maxItems` – Maximum number of posts included (`20`).
- `includeFullContent` – When `true`, the full HTML body of each post is embedded. Set to `false` for excerpt-only feeds.

## Dataset Configuration File

If `staticDatasource.configFile` points to a JSON document (for example `datasource-config.json`), you can describe additional datasets that aggregate standalone entity folders or metadata-tagged posts/pages. The structure looks like this:

```json
{
  "datasets": [
    {
      "name": "team",
      "output": "entity-datasource.json",
      "folder": "entities",
      "metadataKey": "collectAs",
      "metadataValue": "team",
      "includeSources": ["posts", "pages"]
    }
  ]
}
```

Each dataset definition creates a JSON file under `staticDatasource.outputDir`. Refer to [Static Datasources](./static-datasources.md) for the full schema and usage patterns.

## Tips

- Keep `baseUrl` consistent with the published site to avoid broken canonical links.
- Commit `config.json` to version control so environments stay in sync.
- When experimenting, copy the example config and comment fields you are not using—the JSON serializer ignores unknown keys, so adding future settings is safe.

With a configured `config.json`, the rest of Statik (content folders, templates, and assets) falls naturally into place.
