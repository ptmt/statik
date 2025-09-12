---
title: "Getting Started with Statik"
layout: "main"
date: "2025-09-12"
---

# Getting Started with Statik

This guide will help you set up and use Statik to create your own static website.

## Prerequisites

- **JVM**: Java 17 or later
- **Git**: For cloning the repository
- **Text Editor**: Your favorite editor for writing content

## Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/statik/statik.git
   cd statik
   ```

2. **Build the Project**
   ```bash
   ./amper
   ```

3. **Verify Installation**
   ```bash
   ./amper run --help
   ```

## Project Structure

A typical Statik project follows this structure:

```
my-website/
├── config.json          # Site configuration
├── content/             # Markdown content files
│   ├── index.md
│   └── blog/
│       └── first-post.md
├── templates/           # Handlebars templates
│   ├── main.hbs        # Main layout
│   └── partials/
│       └── header.hbs
├── static/             # Static assets
│   ├── css/
│   ├── js/
│   └── images/
└── docs/               # Generated output (configurable)
```

## Configuration

Create a `config.json` file in your project root:

```json
{
  "site": {
    "title": "My Website",
    "description": "A website built with Statik",
    "baseUrl": "https://mysite.com",
    "author": "Your Name"
  },
  "theme": {
    "name": "default",
    "templates": "templates",
    "static": "static",
    "output": "docs"
  }
}
```

## Writing Content

### Basic Markdown File

Create files in the `content/` directory:

```markdown
---
title: "My First Post"
layout: "main"
date: "2025-09-12"
tags: ["blog", "first-post"]
---

# Welcome to My Blog

This is my first post using **Statik**!

## Features I Love

- Simple Markdown writing
- Flexible templating
- Live development server
```

### Frontmatter Options

The YAML frontmatter supports these options:

- `title`: Page title
- `layout`: Template to use (defaults to "main")
- `date`: Publication date
- `tags`: Array of tags
- `draft`: Set to `true` to exclude from builds

## Creating Templates

### Main Layout (`templates/main.hbs`)

```handlebars
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{#if title}}{{title}} - {{/if}}{{site.title}}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
    {{> header}}
    <main>
        {{{content}}}
    </main>
    {{> footer}}
</body>
</html>
```

### Partials (`templates/partials/header.hbs`)

```handlebars
<header>
    <h1><a href="/">{{site.title}}</a></h1>
    <nav>
        <a href="/">Home</a>
        <a href="/blog">Blog</a>
        <a href="/about">About</a>
    </nav>
</header>
```

## Building Your Site

### Development Mode

Run with live reload for development:

```bash
./amper run --root-path=/path/to/your/site --watch
```

This starts a local server at `http://localhost:8080` and automatically rebuilds when files change.

### Production Build

Generate static files for deployment:

```bash
./amper run --root-path=/path/to/your/site
```

The generated files will be in your configured output directory (default: `docs/`).

## Deployment

### GitHub Pages

1. **Setup**: Use `docs/` as your output directory
2. **Configure**: In repository settings, set GitHub Pages source to `/docs` folder
3. **Automate**: Use GitHub Actions for automatic builds (see example workflow)

### Other Platforms

The generated static files can be deployed to:

- **Netlify**: Drag and drop the output folder
- **Vercel**: Connect your repository
- **AWS S3**: Upload files to your bucket
- **Any static hosting**: Copy files to your web server

## Next Steps

- Explore the [template system](/templates) for advanced layouts
- Learn about [custom processors](/processors) for special content types
- Check out [deployment examples](/deployment) for various platforms
- Browse the [API documentation](/api) for extending Statik

## Need Help?

- Browse the [source code](https://github.com/statik/statik) for examples
- Check the [issue tracker](https://github.com/statik/statik/issues) for known problems
- Contribute improvements via pull requests

Happy static site building! 🚀