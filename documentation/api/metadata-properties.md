---
title: Metadata Properties
layout: default
---

# Metadata Properties

Both `BlogPost` and `SitePage` objects automatically parse common metadata fields into convenient properties that you can use directly in your templates.

## Tags

Tags are automatically parsed from comma-separated values in the frontmatter into a list.

**Frontmatter:**
```yaml
---
title: "My Post"
tags: "kotlin, web, tutorial"
---
```

**Template usage:**
```handlebars
{{#if post.tags}}
<div class="tags">
  {{#each post.tags}}
  <span class="tag">{{this}}</span>
  {{/each}}
</div>
{{/if}}
```

**Important:** Always use `post.tags` or `page.tags`, NOT `post.metadata.tags` or `page.metadata.tags`. The metadata value is a string, which will cause issues when iterating with `{{#each}}`.

## Summary

The summary property is the primary field for content summaries.

### For BlogPost

```handlebars
<p class="post-summary">{{post.summary}}</p>
```

**Priority:**
1. `metadata["summary"]` - Custom summary from frontmatter
2. First 160 characters of post content (auto-generated)

**Frontmatter:**
```yaml
---
title: "My Post"
summary: "A brief summary of this post"
---
```

### For SitePage

```handlebars
{{#if page.summary}}
<p class="page-summary">{{page.summary}}</p>
{{/if}}
```

**Priority:**
1. `metadata["summary"]` - Custom summary from frontmatter
2. `null` if not set

## Description

The description property is used for SEO meta descriptions and falls back to summary.

### For BlogPost

```handlebars
{{! Description from metadata, or summary, or first 160 chars of content }}
<meta name="description" content="{{post.description}}">
```

**Priority:**
1. `metadata["description"]` - Custom description for SEO from frontmatter
2. `metadata["summary"]` - Falls back to summary
3. First 160 characters of post content (auto-generated)

**Frontmatter:**
```yaml
---
title: "My Post"
summary: "A brief summary for listings"
description: "A different description optimized for search engines"
---
```

### For SitePage

```handlebars
{{! Description from metadata, or summary }}
{{#if page.description}}
<meta name="description" content="{{page.description}}">
{{/if}}
```

**Priority:**
1. `metadata["description"]` - Custom description for SEO from frontmatter
2. `metadata["summary"]` - Falls back to summary
3. `null` if neither is set

## Example: Complete Post Card

```handlebars
<article class="post-card">
  <h2><a href="{{post.path}}">{{post.title}}</a></h2>

  {{#if post.summary}}
  <p class="summary">{{post.summary}}</p>
  {{/if}}

  <div class="meta">
    <time>{{post.date}}</time>

    {{#if post.tags}}
    <div class="tags">
      {{#each post.tags}}
      <span class="tag">{{this}}</span>
      {{/each}}
    </div>
    {{/if}}
  </div>
</article>
```

## Direct Metadata Access

You can still access raw metadata values directly:

```handlebars
{{! Access raw metadata string }}
{{post.metadata.tags}}  <!-- "kotlin, web, tutorial" -->

{{! Access parsed list }}
{{#each post.tags}}{{this}}{{/each}}  <!-- kotlin web tutorial -->
```

However, it's recommended to use the convenience properties (`tags`, `description`, `summary`) instead of accessing metadata directly.
