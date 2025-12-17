---
title: Markdown Reference
nav_order: 6
description: "Supported Markdown syntax, metadata, and Statik-specific extensions."
---

Statik ships with GitHub Flavored Markdown plus a few conveniences to make your docs and posts richer. Use this page as a quick refresher when authoring content.

## Frontmatter (metadata)

Every Markdown file can start with YAML frontmatter. Statik reads it into the `metadata` object for templates, RSS, and datasources.

```markdown
---
title: "My Post"
published: "2024-10-07T10:00:00"
description: "Optional meta description"
draft: false
nav_order: 2       # Controls sidebar ordering for pages
layout: "main"     # Overrides template layout if needed
category: Guides   # Custom fields are allowed
---
```

- `title` is recommended for both pages and posts.
- `published` controls post dates; falls back to file mtime when omitted.
- `draft: true` hides the post in production builds.
- Add any custom keys; access them in templates with `{{post.metadata.category}}` or `{{page.metadata.category}}`.

## Headings and anchors

Heading anchors are added automatically. Any `# Heading` becomes linkable via `#heading`.

## Tables

```markdown
| Feature      | Support |
|--------------|---------|
| Tables       | ✅      |
| Anchor links | ✅      |
```

## Footnotes

Write footnotes with reference-style markers. Statik renders them as a list or inline hover tooltips.

```markdown
Content that needs a citation.[^why]

[^why]: Footnote text shown at the bottom, or inline when hover tooltips are enabled.
```

To switch to hover tooltips instead of a list, set in `config.json`:

```json
"html": {
  "footnotes": { "display": "HOVER" }
}
```

## Strikethrough

Use double tildes: `~~deprecated~~`.

## Automatic links

Bare URLs and emails are linked automatically, e.g. `https://example.com` or `hello@example.com`.

## Images with captions

Add a title to an image to turn it into a `<figure>` with `<figcaption>`:

```markdown
![Server diagram](./images/server.png "High-level architecture")
```

## Blockquotes with attribution

Finish the last line with `--` or an em dash to capture the author and emit semantic HTML:

```markdown
> Good documentation survives redesigns.
> -- Statik Team
```

## Code fences and syntax highlighting

Triple backticks with a language flag enable highlighted blocks:

```kotlin
fun main() {
    println("Hello, Statik!")
}
```

## Inline HTML

Inline HTML is left intact, so you can sprinkle custom markup or data attributes wherever Markdown falls short.
