# Statik Documentation Site

This directory contains the source files for the Statik documentation website, which serves as both documentation and a live example of Statik's capabilities.

## Building Locally

To build and preview this site locally:

1. **Build Statik** (from project root):
   ```bash
   ./amper
   ```

2. **Generate the site**:
   ```bash
   ./amper run -- --root-path=./documentation
   ```
3. **Development with hot reload**:
   ```bash
   ./amper run -- --root-path=./documentation --watch
   ```

## Deployment

This site is automatically deployed to GitHub Pages via GitHub Actions when changes are pushed to the main branch. The workflow:

1. Builds the Statik generator
2. Runs Statik on this directory to generate static files
3. Deploys the generated `docs/` folder to GitHub Pages

## Features Demonstrated

This site showcases:

- **Markdown Processing**: GitHub Flavored Markdown with extensions
- **Handlebars Templates**: Layout system with partials
- **Static Assets**: CSS styling and responsive design
- **Configuration**: JSON-based site configuration
- **Automatic Deployment**: CI/CD via GitHub Actions
- **RSS Feed**: Automatic RSS 2.0 feed generation for blog posts
- **Static Datasources**: JSON feeds for images and custom collectables

## Content Guidelines

When adding content:

- Use descriptive frontmatter (title, layout, date)
- Follow the existing style and tone
- Include code examples where relevant
- Test locally before committing
- Keep the focus on demonstrating Statik features

## Styling

The site uses a modern, clean design with:

- Responsive layout that works on all devices
- Syntax highlighting for code blocks
- Professional typography and spacing
- Consistent color scheme based on the Statik brand

Feel free to enhance the styling while maintaining the professional appearance and excellent user experience.
