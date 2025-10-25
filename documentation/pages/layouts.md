---
title: Layouts and Template Hierarchy
nav_order: 2
---

Statik supports a hierarchical template system with layouts, allowing you to share common HTML structure across all your pages while keeping individual templates focused on content.

## Layout System

### What are Layouts?

Layouts are base templates that define the overall HTML structure of your pages (doctype, head, body, etc.). They wrap around your content templates, eliminating duplication and making maintenance easier.

### Creating a Layout

Create a layout file in `templates/layouts/`:

**`templates/layouts/default.hbs`**
```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{#if title}}{{title}} - {{/if}}{{siteName}}</title>
    <meta name="description" content="{{#if description}}{{description}}{{else}}{{siteName}}{{/if}}">
    <link rel="stylesheet" href="{{baseUrl}}css/style.css">
</head>
<body>
    {{include "partials/header.hbs"}}
    <main class="main-content">
        {{{content}}}
    </main>
    {{include "partials/footer.hbs"}}
</body>
</html>
```

The `{{{content}}}` placeholder is where your page content will be injected.

### Using Layouts in Templates

With layouts, your templates become much simpler:

**`templates/post.hbs`** (before layouts)
```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>{{post.title}}</title>
    <link rel="stylesheet" href="{{baseUrl}}css/style.css">
</head>
<body>
    {{include "partials/header.hbs"}}
    <main>
        <article>
            <h1>{{post.title}}</h1>
            {{{post.content}}}
        </article>
    </main>
    {{include "partials/footer.hbs"}}
</body>
</html>
```

**`templates/post.hbs`** (with layouts)
```handlebars
<article class="post">
    <header class="page-header">
        <h1>{{post.title}}</h1>
        <time datetime="{{post.date}}">{{formatDate post.date}}</time>
    </header>
    <div class="content">
        {{{post.content}}}
    </div>
</article>
```

The layout is automatically applied and specified via the `layout` parameter in the template data.

### Specifying a Layout

You can specify which layout to use in your content frontmatter:

```markdown
---
title: My Custom Page
layout: minimal
---

# Page Content

This page uses the minimal layout.
```

If no layout is specified, the `default` layout is used.

### Multiple Layouts

You can create multiple layouts for different purposes:

- **`layouts/default.hbs`** - Standard layout with header/footer
- **`layouts/minimal.hbs`** - Bare-bones layout without navigation
- **`layouts/print.hbs`** - Print-friendly layout

Example **`layouts/minimal.hbs`**:
```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>{{title}}</title>
</head>
<body>
    {{{content}}}
</body>
</html>
```

## Template Hierarchy

The template hierarchy in Statik follows this structure:

```
Layouts (templates/layouts/)
  └─ Base HTML structure
     └─ Content Templates (templates/)
        └─ Page-specific markup
           └─ Partials (templates/partials/)
              └─ Reusable components
```

### Template Types

1. **Layouts** (`templates/layouts/*.hbs`)
   - Define overall page structure
   - Include HTML doctype, head, body
   - Contain `{{{content}}}` placeholder

2. **Content Templates** (`templates/*.hbs`)
   - `home.hbs` - Homepage template
   - `post.hbs` - Blog post template
   - `page.hbs` - Static page template
   - `year.hbs` - Year archive template (optional)

3. **Partials** (`templates/partials/*.hbs`)
   - `header.hbs` - Site header/navigation
   - `footer.hbs` - Site footer
   - Any custom reusable components

### Template Variables

All templates have access to these variables:

**Global Variables:**
- `siteName` - Your site name
- `baseUrl` - Base URL for the site
- `description` - Site description
- `pages` - Array of all pages

**Template-Specific:**
- **home.hbs**: `posts[]` - Array of all blog posts
- **post.hbs**: `post` - Current post object (title, content, date, metadata)
- **page.hbs**: `page` - Current page object (title, content, metadata)

## Best Practices

1. **Use Layouts for Common Structure**
   - Put HTML boilerplate in layouts
   - Keep content templates focused on content markup

2. **Create Multiple Layouts When Needed**
   - Different layouts for different page types
   - Special layouts for landing pages, documentation, etc.

3. **Use Partials for Shared Components**
   - Headers, footers, navigation
   - Reusable UI components

4. **Specify Layouts in Frontmatter**
   - Override default layout per-page as needed
   - Keep layout choice close to content

## Example Project Structure

```
my-site/
├── templates/
│   ├── layouts/
│   │   ├── default.hbs      # Main layout
│   │   └── minimal.hbs      # Alternative layout
│   ├── partials/
│   │   ├── header.hbs
│   │   └── footer.hbs
│   ├── home.hbs             # Uses default layout
│   ├── post.hbs             # Uses default layout
│   └── page.hbs             # Uses default layout
├── posts/
│   └── my-post.md           # Can specify custom layout
├── pages/
│   └── about.md             # Can specify custom layout
└── config.json
```

This hierarchical approach keeps your templates DRY (Don't Repeat Yourself) and makes maintaining your site easier.
