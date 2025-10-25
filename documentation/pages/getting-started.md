---
title: "Getting Started with Statik"
layout: "main"
nav_order: 1
---

# Getting Started with Statik

This guide will help you set up and use Statik to create your own static website.

## Quick Start (No Templates Needed!)

Statik now works out of the box with built-in templates. Just create content and go!

1. **Create a config.json**
   ```json
   {
     "siteName": "My Blog",
     "baseUrl": "https://example.com",
     "description": "My awesome blog",
     "author": "Your Name"
   }
   ```

2. **Add some content**
   ```markdown
   ---
   title: Hello World
   ---

   # Hello World

   This is my first post!
   ```

3. **Generate your site**
   ```bash
   ./amper run -- --root-path .
   ```

That's it! Statik includes clean, minimal built-in templates to get you started.

## Prerequisites

- **JVM**: Java 17 or later (or use Docker)
- **Text Editor**: Your favorite editor for writing content

## Installation Options

### Option 1: Build from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/statik/statik.git
   cd statik
   ```

2. **Build the Project**
   ```bash
   ./amper
   ```

### Option 2: Use Docker (Recommended)

```bash
docker run --rm -v $(pwd):/github/workspace ghcr.io/ptmt/statik:latest run -- --root-path .
```

## Project Structure

A typical Statik project follows this structure:

```
my-website/
├── config.json          # Site configuration (required)
├── posts/               # Blog posts (optional)
│   └── first-post.md
├── content/             # Static pages (optional)
│   └── about.md
├── templates/           # Custom templates (optional)
│   ├── home.hbs        # Homepage template
│   ├── post.hbs        # Blog post template
│   ├── page.hbs        # Static page template
│   └── partials/       # Shared components
│       ├── header.hbs
│       └── footer.hbs
├── static/             # Static assets (optional)
│   ├── css/
│   ├── js/
│   └── images/
└── build/              # Generated output (default)
```

**Note**: Only `config.json` and content files are required. Templates and assets are optional!

## Configuration

Create a `config.json` file in your project root:

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
    "imagesFileName": "images.json"
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

**Configuration Options:**
- `siteName`: Your site's name (required)
- `baseUrl`: Your site's URL (required)
- `description`: Site description (required)
- `author`: Your name (required)
- `theme.templates`: Templates directory (default: "templates")
- `theme.assets`: Static assets directory (default: "static")
- `theme.output`: Output directory (default: "build")
- `paths.posts`: Blog posts directory (default: "posts")
- `paths.pages`: Static pages directory (default: "content")
- `devServer.port`: Development server port for `--watch` (default: `3000`)
- `staticDatasource.outputDir`: Datasource JSON folder under `theme.output` (default: `"datasource"`)
- `staticDatasource.collectAttribute`: Attribute for marking collectable elements (default: `"data-collect"`)
- `staticDatasource.imagesFileName`: Image datasource filename (default: `"images.json"`)
- `rss.enabled`: Enable RSS feed generation (default: `true`)
- `rss.fileName`: RSS feed filename (default: `"feed.xml"`)
- `rss.title`: RSS feed title (default: uses `siteName`)
- `rss.description`: RSS feed description (default: uses site `description`)
- `rss.language`: RSS feed language code (default: `"en-us"`)
- `rss.maxItems`: Maximum number of posts in RSS feed (default: `20`)
- `rss.includeFullContent`: Include full post content in feed (default: `true`)

### RSS Feed

Statik automatically generates an RSS feed for your blog posts, making it easy for readers to subscribe to your content.

**Features:**
- Automatically generates RSS 2.0 compliant feed
- Includes post titles, links, descriptions, and publication dates
- Supports full content or excerpt-only feeds
- Configurable feed title, description, and language
- Limits the number of items in the feed

**Configuration:**
The RSS feed is enabled by default and can be customized in your `config.json`:

```json
"rss": {
  "enabled": true,              // Toggle RSS generation
  "fileName": "feed.xml",       // Output filename
  "title": "My Blog Feed",      // Feed title (optional, defaults to siteName)
  "description": "Latest posts", // Feed description (optional, defaults to site description)
  "language": "en-us",          // Language code
  "maxItems": 20,               // Maximum posts in feed
  "includeFullContent": true    // Include full HTML content vs. excerpts only
}
```

**Disabling RSS:**
To disable RSS feed generation, set `enabled` to `false`:

```json
"rss": {
  "enabled": false
}
```

**Accessing the Feed:**
After generation, your RSS feed will be available at `https://yoursite.com/feed.xml` (or the filename you configured).

### Static Datasources

When generation finishes, Statik can emit helper JSON files that you can consume from JavaScript:
- **Images**: Every `<img>` found in Markdown or HTML content is added to `/<staticDatasource.outputDir>/<staticDatasource.imagesFileName>` along with its `alt`, `title`, and source page information.
- **Custom entities**: Add the configured attribute (e.g. `data-collect="quotes"`) to any HTML block to gather it into `/<staticDatasource.outputDir>/quotes.json`. Any extra `data-*` attributes travel with the entity for richer metadata.

This makes it easy to fuel interactive widgets (carousels, quotes, galleries) without a back end.

## Writing Content

Statik supports multiple file formats: Markdown (`.md`), HTML (`.html`), and Handlebars templates (`.hbs`).

### Blog Posts

Create files in the `posts/` directory using any supported format:

**Markdown (`.md`)**
```markdown
---
title: "My First Post"
published: "2024-01-01T00:00:00"
---

# Welcome to My Blog

This is my first post using **Statik**!

## Features I Love

- Simple Markdown writing
- Built-in templates (no setup required!)
- Live development server
- Docker support
```

**HTML (`.html`)**
```html
---
title: "Custom HTML Post"
published: "2024-01-01T00:00:00"
layout: default
---
<div class="custom-post">
    <h1>Welcome to My Blog</h1>
    <p class="highlight">This post uses pure HTML!</p>
</div>
```

**Handlebars Template (`.hbs`)**
```handlebars
---
title: "Dynamic Post"
published: "2024-01-01T00:00:00"
---
<article>
    <h1>{{post.title}}</h1>
    <p>Published on {{formatDate post.date}}</p>
    <p>Welcome to {{siteName}}!</p>
</article>
```

### Static Pages

Create files in the `content/` directory:

```markdown
---
title: "About Me"
nav_order: 1
---

# About Me

I'm a developer who loves static sites!

You can reach me at [email@example.com](mailto:email@example.com).
```

### Frontmatter Options

The YAML frontmatter supports these options:

- `title`: Page title (required)
- `published`: Publication date for posts (format: "YYYY-MM-DDTHH:MM:SS")
- `nav_order`: Order in navigation menu (for pages)
- `layout`: Specify a custom layout (e.g., "minimal", "landing")
- `description`: Page description for SEO
- Custom fields: Add any custom metadata you need

## Custom Templates (Optional)

Statik includes built-in templates that work great out of the box. But if you want to customize the look and feel, you can create your own templates.

### Built-in Templates

When you don't provide templates, Statik uses clean, minimal built-in templates that include:
- Responsive design with basic CSS
- Navigation between pages
- Proper HTML5 structure
- Support for all template variables

### Creating Custom Templates with Layouts

Statik supports a hierarchical template system with layouts to eliminate duplication.

#### Create a Layout (`templates/layouts/default.hbs`)

```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{#if title}}{{title}} - {{/if}}{{siteName}}</title>
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

#### Homepage Template (`templates/home.hbs`)

With layouts, templates become simpler:

```handlebars
<h1>{{siteName}}</h1>
<p>{{description}}</p>

{{#if posts}}
<h2>Recent Posts</h2>
<ul>
    {{#each posts}}
    <li><a href="{{path}}/">{{title}}</a> - {{formatDate date}}</li>
    {{/each}}
</ul>
{{/if}}
```

#### Post Template (`templates/post.hbs`)

```handlebars
<article class="post">
    <header>
        <h1>{{post.title}}</h1>
        <time datetime="{{post.date}}">{{formatDate post.date}}</time>
    </header>
    <div class="content">
        {{{post.content}}}
    </div>
</article>
```

The layout wrapper is automatically applied! See the [Layouts documentation](layouts.html) for more details.

### Template Variables

Available in all templates:
- `siteName`, `description`, `baseUrl`, `author`
- `pages[]` - Array of static pages
- `posts[]` - Array of blog posts (in home template)
- `post` - Current post object (in post template)
- `page` - Current page object (in page template)

## Building Your Site

### Development Mode

Run with live reload for development:

```bash
./amper run -- --root-path=/path/to/your/site --w
```

This starts a local server at `http://localhost:8080` and automatically rebuilds when files change.

### Production Build

Generate static files for deployment:

```bash
./amper run -- --root-path=/path/to/your/site
```

The generated files will be in your configured output directory (default: `build/`).

### Using Docker

You can also use Docker instead of building from source:

```bash
# Development with live reload
docker run --rm -v $(pwd):/github/workspace -p 8080:8080 \
  ghcr.io/ptmt/statik:latest run -- --root-path . --w

# Production build
docker run --rm -v $(pwd):/github/workspace \
  ghcr.io/ptmt/statik:latest run -- --root-path .
```

## Deployment

### GitHub Pages

1. **Setup**: Configure output directory in your `config.json`
   ```json
   {
     "theme": {
       "output": "docs"
     }
   }
   ```

2. **Configure**: In repository settings, set GitHub Pages source to `/docs` folder
3. **Automate**: Use GitHub Actions for automatic builds

### Other Platforms

The generated static files can be deployed to:

- **Netlify**: Drag and drop the output folder or connect your repository
- **Vercel**: Connect your repository with automatic builds
- **AWS S3**: Upload files to your bucket
- **Any static hosting**: Copy files to your web server

### Example GitHub Actions Workflow

```yaml
name: Deploy Site
on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Generate site with Statik
        run: |
          docker run --rm -v $(pwd):/github/workspace \
            ghcr.io/ptmt/statik:latest run -- --root-path .

      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./build
```

## Need Help?

- Browse the [source code](https://github.com/statik/statik) for examples
- Check the [issue tracker](https://github.com/statik/statik/issues) for known problems
- Contribute improvements via pull requests

Happy static site building! 🚀
