---
title: "Welcome to Statik"
layout: "main"
date: "2025-09-12"
---

# Statik - Modern Static Site Generator

Statik is a powerful static site generator built with **Kotlin** that combines simplicity with advanced features. This website itself is generated using Statik, serving as both documentation and a live example.

## ✨ Key Features

- **Markdown Processing**: Full support for GitHub Flavored Markdown with extensions
- **Handlebars Templates**: Flexible templating system with partials and helpers
- **Live Development**: Built-in server with hot reload during development
- **Flexible Configuration**: JSON-based configuration with sensible defaults
- **Extensible**: Plugin architecture for custom processors and generators

## 🚀 Quick Start

```bash
# Build the project
./amper

# Run on your content
./amper run --root-path=/path/to/your/site

# Development mode with live reload
./amper run --root-path=/path/to/your/site --watch
```

## 📖 How This Site Works

This website demonstrates Statik's capabilities:

1. **Markdown Content**: All pages are written in Markdown with YAML frontmatter
2. **Handlebars Templates**: Layout and partials define the site structure
3. **Static Assets**: CSS, JavaScript, and images are copied to the output
4. **GitHub Actions**: Automated building and deployment to GitHub Pages

## 🛠 Architecture

Statik follows a modular architecture:

- `BlogEngine`: Core engine coordinating all components
- `MarkdownProcessor`: Processes Markdown files with frontmatter
- `TemplateEngine`: Handles Handlebars template rendering
- `SiteGenerator`: Orchestrates the build process
- `FileWalker`: Efficiently traverses and processes file trees

## 📝 Markdown Extensions

This site showcases various Markdown features:

### Tables

| Feature | Status | Description |
|---------|--------|-------------|
| Tables | ✅ | GitHub-style tables |
| Footnotes | ✅ | Reference-style footnotes[^1] |
| Anchor Links | ✅ | Auto-generated heading anchors |
| Strikethrough | ✅ | ~~Crossed out text~~ |

### Code Highlighting

```kotlin
class BlogEngine(private val config: BlogConfig) {
    fun generateSite() {
        val posts = processor.loadPosts()
        val renderedPosts = posts.map { templateEngine.render(it) }
        generator.writeSite(renderedPosts)
    }
}
```

## 🌟 Live Example

You're viewing a live example right now! This entire site is:

- Generated from Markdown files
- Styled with modern CSS
- Built automatically via GitHub Actions
- Hosted on GitHub Pages

---

Ready to build your own static site? Check out the [Getting Started Guide](/getting-started) or explore the [source code](https://github.com/statik/statik).

[^1]: Footnotes provide additional context without cluttering the main text.