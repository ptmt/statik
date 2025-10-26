---
title: Static Datasources
nav_order: 5
---

Static datasources let you ship machine-friendly JSON alongside the generated HTML so your front-end code can power dynamic widgets without needing a live API. Think of it as **generating your own API at build time**—all the benefits of structured data endpoints without running a server.

The JSON files are written to a dedicated folder (by default `/datasource` in your build output) and can be fetched by JavaScript, used during template rendering, or even version-controlled as part of your static site. This pattern is particularly powerful for building searchable documentation, filterable galleries, or any feature that benefits from structured data access.

> **Real-world example**: This very documentation site uses static datasources. The `/api` folder contains markdown files describing configuration options, which are automatically collected into `api-reference.json` and rendered as an interactive API reference—no backend required.

Statik currently supports three datasource types:

- **`images.json`**: Every `<img>` encountered in Markdown or HTML content.
- **`<type>.json`**: Any element that carries the configured collectable attribute (defaults to `data-collect`).
- **Custom datasets**: Entity folders or metadata-tagged posts/pages defined in `datasource-config.json`.

Each datasource entry includes metadata about the originating page or post, making it easy to build galleries, quote rotators, searchable indexes, or other interactive components in plain JavaScript.

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

### Real Example: The `/api` Folder Pattern

This documentation site demonstrates a practical use case. The project includes:

**`datasource-config.json`:**
```json
{
  "datasets": [
    {
      "name": "api",
      "output": "api-reference.json",
      "folder": "api",
      "includeSources": []
    }
  ]
}
```

**`api/siteName.md`:**
```markdown
---
title: siteName
type: string
required: true
category: core
order: 1
---

Display name used across templates and metadata.
```

This generates `/datasource/api-reference.json` containing all configuration options with their metadata, which powers the searchable API reference on this site. The pattern works beautifully for documentation, component libraries, or any structured content that benefits from programmatic access.

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

## Using Datasources in Handlebars Templates

The entire bundle is available during template rendering under the `datasource` key, making it easy to consume in layouts and partials:

```hbs
{{#with datasource}}
  {{#each entities.team}}
    <article class="team-member">
      <h3>{{title}}</h3>
      <p class="role">{{metadata.role}}</p>
    </article>
  {{/each}}

  <section class="quotes">
    {{#each collectables.quotes}}
      <blockquote>{{text}}</blockquote>
    {{/each}}
  </section>

  <ul class="images">
    {{#each images}}
      <li><img src="{{src}}" alt="{{alt}}"></li>
    {{/each}}
  </ul>
{{/with}}
```

Each dataset is also exposed via `datasource.datasets`, which includes filenames for the generated JSON documents should you need to link directly to them.

## Consuming Datasources in JavaScript

Because the datasources are static JSON files in the `/datasource` folder (or your configured `outputDir`), you can fetch them with standard browser APIs:

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

For more complex examples, check how this documentation site uses `/datasource/api-reference.json` to build its interactive configuration reference. The JSON is fetched client-side and filtered/searched without any backend infrastructure.

This approach works whether your site is served statically or behind a CDN—there is no server-side processing required.

## Tips and Best Practices

- **Think of datasources as build-time APIs**: The `/datasource` folder effectively becomes your API endpoint directory—plan your JSON structure accordingly.
- **Use entity folders for structured content**: The `/api` folder pattern (demonstrated in this docs site) is ideal for configuration references, component libraries, glossaries, or any content that benefits from consistent structure.
- Keep `collectAttribute` values simple (letters, numbers, dashes) for friendly filenames.
- Use additional `data-*` attributes to attach metadata such as author, category, or rating.
- Datasources are regenerated on each build, so their contents always reflect the latest site content.
- **Combine with client-side filtering**: Since datasources are just JSON arrays, they work beautifully with search libraries, filters, and interactive widgets—no database required.

Static datasources give you the convenience of an API without leaving the static site workflow. Whether you're building documentation (like the `/api` reference here), a photo gallery, a team directory, or product catalog, this pattern lets you maintain content as markdown while exposing it as structured JSON for dynamic features.
