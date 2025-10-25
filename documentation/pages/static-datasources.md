---
title: Static Datasources
nav_order: 4
---

# Static Datasources

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
  "imagesFileName": "images.json"
}
```

- `enabled`: Set to `false` to skip datasource generation entirely.
- `outputDir`: Folder (under `theme.output`) where JSON files are written.
- `collectAttribute`: HTML attribute that marks custom collectable elements.
- `imagesFileName`: File name used for the auto-generated images list.

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
