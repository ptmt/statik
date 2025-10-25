---
title: Static Datasources
nav_order: 4
---

Static datasources let you ship machine-friendly JSON alongside the generated HTML so your front-end code can power dynamic widgets without needing a live API. Statik currently supports two datasource types:

- `images.json`: Every `<img>` encountered in Markdown or HTML content.
- `<type>.json`: Any element that carries the configured collectable attribute (defaults to `data-collect`).

Each datasource entry includes metadata about the originating page or post, making it easy to build galleries, quote rotators, or other interactive components in plain JavaScript.

## Enabling or Disabling Datasources

Datasources are enabled by default. You can toggle or configure them in `config.json`:

```json
"staticDatasource": {
  "enabled": true,
  "outputDir": "datasource",
  "collectAttribute": "data-collect",
  "imagesFileName": "images.json",
  "configFile": "datasource-config.json"
}
```

- `enabled`: Set to `false` to skip datasource generation entirely.
- `outputDir`: Folder (under `theme.output`) where JSON files are written.
- `collectAttribute`: HTML attribute that marks custom collectable elements.
- `imagesFileName`: File name used for the auto-generated images list.
- `configFile`: Optional location of dataset definitions for standalone entities.

All values are optional; Statik falls back to the defaults shown above.

## Images Datasource

Every `<img>` element found in posts and pages is collected into `/<outputDir>/<imagesFileName>`. Each entry includes:

```json
{
  "src": "/media/sunrise.jpg",
  "alt": "Sunrise",
  "title": "Golden hour",
  "source": {
    "type": "post",
    "id": "gallery",
    "path": "/gallery/",
    "title": "Gallery"
  }
}
```

Use this feed to build image carousels, responsive galleries, or preload assets.

## Custom Collectables

Add the configured attribute to any HTML element to collect it:

```html
<blockquote data-collect="quotes" data-author="Ada Lovelace">
  That brain of mine is more than merely mortal.
</blockquote>
```

Statik sanitises the attribute value to create a filename (for example, `quotes.json`). The collected JSON entry contains:

```json
{
  "source": {
    "type": "page",
    "id": "about",
    "path": "/about/",
    "title": "About"
  },
  "html": "<blockquote data-collect=\"quotes\" data-author=\"Ada Lovelace\">That brain of mine is more than merely mortal.</blockquote>",
  "text": "That brain of mine is more than merely mortal.",
  "attributes": {
    "data-author": "Ada Lovelace"
  }
}
```

Only `data-*` attributes (aside from the collect attribute itself) are preserved, keeping the payload compact while still exposing structured metadata.

## Custom Datasets with `datasource-config.json`

To expose bespoke datasets, describe them in `datasource-config.json` (relative to your project root by default):

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

- `name`: Logical name for the dataset. Exposed on each JSON item.
- `output`: File that will be written inside `staticDatasource.outputDir`.
- `folder`: Optional directory whose files should be ingested.
- `metadataKey` / `metadataValue`: Optional frontmatter key/value pair that selects posts or pages to include (presence alone is enough if `metadataValue` is omitted).
- `includeSources`: Limit metadata collection to `posts`, `pages`, or both.

You can define multiple datasets in the same configuration file.

## Standalone Entity Folders

Place Markdown or HTML files in the configured `folder` to create self-contained entities with free-form metadata:

```
entities/
└── alice.md
```

```markdown
---
id: alice
title: Alice Doe
role: Engineer
---
Alice keeps the CI green.
```

Each file is rendered through Statik’s Markdown pipeline so you can mix prose with metadata. The resulting JSON item looks like:

```json
{
  "dataset": "team",
  "id": "alice",
  "title": "Alice Doe",
  "content": "<p>Alice keeps the CI green.</p>\n",
  "metadata": {
    "id": "alice",
    "title": "Alice Doe",
    "role": "Engineer"
  },
  "source": {
    "type": "team",
    "id": "alice",
    "path": "/entities/alice/",
    "title": "Alice Doe"
  }
}
```

## Tagging Posts or Pages as Entities

Reuse your existing content by tagging posts or pages with the configured metadata key:

```markdown
---
title: Meet the Team
collectAs: team
---

Say hello to the people behind the product.
```

The entry will be appended to the same dataset, keeping `source.type` as `post` or `page` so you can distinguish the origin on the client side.

## Consuming Datasources in JavaScript

Because the datasources are static JSON files, you can fetch them with standard browser APIs:

```javascript
async function loadQuotes() {
  const response = await fetch('/datasource/quotes.json');
  if (!response.ok) return [];
  return await response.json();
}

loadQuotes().then(quotes => {
  const container = document.querySelector('#quotes');
  quotes.forEach(item => {
    const element = document.createElement('blockquote');
    element.textContent = item.text ?? '';
    element.dataset.author = item.attributes?.['data-author'] ?? '';
    container.appendChild(element);
  });
});
```

This approach works whether your site is served statically or behind a CDN—there is no server-side processing required.

## Tips and Best Practices

- Keep `collectAttribute` values simple (letters, numbers, dashes) for friendly filenames.
- Use additional `data-*` attributes to attach metadata such as author, category, or rating.
- Datasources are regenerated on each build, so their contents always reflect the latest site content.

Static datasources give you the convenience of an API without leaving the static site workflow.
