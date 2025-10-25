---
title: Custom HTML and Template Files
nav_order: 3
---

Statik supports multiple file formats for your content, giving you flexibility in how you author your pages and posts.

## Supported File Types

### Markdown Files (`.md`)

The standard format for content. Markdown is processed and converted to HTML.

```markdown
---
title: My Blog Post
published: 2024-10-07T10:00:00
---

# Hello World

This is a **markdown** post.
```

### HTML Files (`.html`)

Pure HTML files with optional frontmatter. The HTML content is used as-is without any Markdown processing.

**`posts/custom-post.html`**
```html
---
title: Custom HTML Post
published: 2024-10-07T10:00:00
description: A post with custom HTML
layout: default
---
<div class="custom-container">
    <h1>Custom HTML Content</h1>
    <p class="highlight">This HTML is used directly without Markdown processing.</p>
    <ul class="custom-list">
        <li>Full control over markup</li>
        <li>Custom CSS classes</li>
        <li>Complex layouts</li>
    </ul>
</div>
```

### Handlebars Template Files (`.hbs`)

Dynamic template files that can use Handlebars expressions and have access to all template variables.

**`pages/dynamic-page.hbs`**
```handlebars
---
title: Dynamic Page
nav_order: 1
custom_var: Hello World
---
<div class="dynamic-content">
    <h1>{{page.title}}</h1>
    <p>Welcome to {{siteName}}</p>
    <p>Custom variable: {{page.metadata.custom_var}}</p>

    <h2>All Pages</h2>
    <ul>
    {{#each pages}}
        <li><a href="{{../baseUrl}}{{path}}">{{title}}</a></li>
    {{/each}}
    </ul>
</div>
```

## Frontmatter Support

All file types support YAML frontmatter for metadata:

```yaml
---
title: Page Title
published: 2024-10-07T10:00:00  # For posts
nav_order: 1                     # For pages
layout: custom                   # Optional: specify layout
description: Custom description  # For meta tags
custom_field: Any value         # Custom metadata
---
```

## When to Use Each Format

### Use Markdown (`.md`) when:
- Writing blog posts or articles
- You want simple, readable source files
- You need basic formatting (headers, lists, links, code blocks)
- You want to focus on content, not markup

### Use HTML (`.html`) when:
- You need precise control over the markup
- You want to use specific CSS classes or IDs
- You're creating complex layouts or components
- You have existing HTML content to integrate

### Use Handlebars (`.hbs`) when:
- You need dynamic content generation
- You want to iterate over data (pages, posts, custom arrays)
- You need conditional rendering
- You want to create reusable, data-driven templates

## Template Variables in HBS Files

HBS files have access to all template variables:

### For Posts (`posts/*.hbs`)

```handlebars
{{post.title}}                    - Post title
{{post.date}}                     - Publication date
{{post.content}}                  - Post content
{{post.metadata.custom_field}}   - Custom metadata
{{siteName}}                      - Site name
{{baseUrl}}                       - Base URL
{{#each pages}}...{{/each}}      - Loop through pages
```

### For Pages (`pages/*.hbs`)

```handlebars
{{page.title}}                    - Page title
{{page.content}}                  - Page content
{{page.metadata.custom_field}}   - Custom metadata
{{siteName}}                      - Site name
{{baseUrl}}                       - Base URL
{{#each pages}}...{{/each}}      - Loop through all pages
```

## Advanced Examples

### HTML Post with Custom Layout

**`posts/landing.html`**
```html
---
title: Product Launch
published: 2024-10-07T10:00:00
layout: landing
---
<section class="hero">
    <h1 class="hero-title">Introducing Our New Product</h1>
    <p class="hero-subtitle">The future of productivity</p>
    <button class="cta-button">Learn More</button>
</section>

<section class="features">
    <div class="feature">
        <h3>Fast</h3>
        <p>Lightning-fast performance</p>
    </div>
    <div class="feature">
        <h3>Simple</h3>
        <p>Easy to use interface</p>
    </div>
</section>
```

### Dynamic HBS Page with Conditional Logic

**`pages/archives.hbs`**
```handlebars
---
title: Archives
nav_order: 5
---
<div class="archives">
    <h1>{{page.title}}</h1>

    {{#if posts}}
        <p>Total posts: {{posts.length}}</p>

        <div class="posts-by-year">
        {{#each posts}}
            <article class="post-summary">
                <time>{{formatDate date format="yyyy-MM-dd"}}</time>
                <h2>
                    <a href="{{../baseUrl}}{{path}}">{{title}}</a>
                </h2>
                {{#if metadata.description}}
                    <p>{{metadata.description}}</p>
                {{/if}}
            </article>
        {{/each}}
        </div>
    {{else}}
        <p>No posts yet.</p>
    {{/if}}
</div>
```

### Mixed Content Site Structure

You can mix and match file types in the same site:

```
my-site/
├── posts/
│   ├── regular-post.md          # Standard markdown
│   ├── custom-post.html         # Custom HTML
│   └── dynamic-post.hbs         # Dynamic template
├── pages/
│   ├── about.md                 # Markdown page
│   ├── portfolio.html           # HTML portfolio
│   └── archives.hbs             # Dynamic archives
└── config.json
```

## Handlebars Helpers

All HBS files have access to these helpers:

### `formatDate`

Format dates with custom patterns:

```handlebars
{{formatDate post.date}}                          # Default: MMMM dd, yyyy
{{formatDate post.date format="yyyy-MM-dd"}}     # Custom format
{{formatDate post.date format="MMM d, yyyy"}}    # Oct 7, 2024
```

### `excerpt`

Create excerpts from content:

```handlebars
{{excerpt post.content}}                  # Default: 30 words
{{excerpt post.content words=50}}         # Custom length
```

### `include`

Include partials:

```handlebars
{{include "partials/sidebar.hbs"}}
```

### `datasource`

All templates receive a `datasource` object that mirrors the generated JSON files, so you can render galleries, quote blocks, or entity lists during server-side rendering without additional fetches:

```handlebars
{{#each datasource.images}}
  <img src="{{src}}" alt="{{alt}}">
{{/each}}
```

### `eq` (equality)

Conditional comparison:

```handlebars
{{#if (eq page.title "Home")}}
    <p>Welcome to the homepage!</p>
{{/if}}
```

## Best Practices

1. **Choose the Right Format**
   - Default to Markdown for most content
   - Use HTML for specific design needs
   - Use HBS for dynamic/data-driven content

2. **Use Frontmatter Consistently**
   - Always include `title` metadata
   - Add `description` for better SEO
   - Specify `layout` when using custom layouts

3. **Keep Logic in Templates**
   - Put complex logic in `.hbs` files
   - Keep `.html` files for static content
   - Use Markdown for writing-focused content

4. **Organize by Purpose**
   - Group similar file types together
   - Use descriptive filenames
   - Document custom metadata fields

## Processing Order

When Statik processes your files:

1. **Frontmatter is extracted** (all file types)
2. **Content is processed**:
   - `.md` → Markdown → HTML
   - `.html` → Used as-is
   - `.hbs` → Rendered with template variables
3. **Layout is applied** (if specified)
4. **Final HTML is written** to output directory

This gives you maximum flexibility in authoring while maintaining a consistent output structure.
