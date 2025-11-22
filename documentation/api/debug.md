---
title: Debug Helper
layout: default
---

# Debug Helper

The `{{debug}}` helper is a powerful debugging tool that displays all available template variables and their nested properties in your Handlebars templates.

## Usage

Simply add `{{debug}}` anywhere in your template:

```handlebars
<h1>{{title}}</h1>
{{debug}}
<p>{{content}}</p>
```

## Output

The debug helper will render a styled `<pre>` block showing:
- All available variables in the current template context
- Nested properties up to 3 levels deep
- Lists with their first 3 items
- Object properties and their types

Example output:

```
Debug: Template Variables
════════════════════════════════════════════════════════════
count: 42
enabled: true
post: {3 properties}
  date: "2025-11-22"
  tags: [3 items]
    [0]: "kotlin"
    [1]: "handlebars"
    [2]: "debug"
  title: "Post Title"
title: "My Post"
════════════════════════════════════════════════════════════
```

## Features

- **Automatic nesting**: Shows nested objects and their properties up to 3 levels deep
- **List inspection**: Displays the first 3 items of any list/array
- **Type information**: Shows the type of complex objects
- **Truncation**: Long strings (>100 chars) are truncated with "..." for readability
- **Sorted keys**: Variables are displayed alphabetically for easy scanning
- **Styled output**: Renders in a fixed-width, scrollable box with a max height of 600px

## Use Cases

- **Template debugging**: See what data is available in your template context
- **Development**: Understand the structure of posts, pages, and custom data
- **Troubleshooting**: Verify that expected variables are present and have correct values

## Notes

- The debug helper is safe to use in production, but you should remove it before deploying
- The output is HTML-formatted and won't be escaped by Handlebars
- Variables starting with `__` (internal variables) may be present in the context
