package com.potomushto.statik.template

/**
 * Provides minimal fallback templates when user templates are missing.
 * These templates render content as basic HTML without styling.
 */
object FallbackTemplates {

    val HOME_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{siteName}}</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
                h1 { color: #333; }
                .post-list { list-style: none; padding: 0; }
                .post-item { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
                .post-title { margin: 0 0 10px 0; }
                .post-date { color: #666; font-size: 0.9em; }
                .pages-nav { margin: 20px 0; }
                .pages-nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
            </style>
        </head>
        <body>
            <header>
                <h1>{{siteName}}</h1>
                {{#if description}}<p>{{description}}</p>{{/if}}
                {{#if pages}}
                <nav class="pages-nav">
                    {{#each pages}}
                    <a href="{{path}}/">{{title}}</a>
                    {{/each}}
                </nav>
                {{/if}}
            </header>

            <main>
                {{#if posts}}
                <h2>Recent Posts</h2>
                <ul class="post-list">
                    {{#each posts}}
                    <li class="post-item">
                        <h3 class="post-title"><a href="{{path}}/">{{title}}</a></h3>
                        <div class="post-date">{{date}}</div>
                        <div class="post-excerpt">{{{content}}}</div>
                    </li>
                    {{/each}}
                </ul>
                {{else}}
                <p>No posts yet. Create some Markdown files in your posts directory!</p>
                {{/if}}
            </main>
        </body>
        </html>
    """.trimIndent()

    val POST_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{post.title}} - {{siteName}}</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
                h1 { color: #333; }
                .post-meta { color: #666; font-size: 0.9em; margin-bottom: 20px; }
                .pages-nav { margin: 20px 0; }
                .pages-nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
                .content { line-height: 1.6; }
            </style>
        </head>
        <body>
            <header>
                <h1><a href="{{baseUrl}}" style="text-decoration: none; color: inherit;">{{siteName}}</a></h1>
                {{#if pages}}
                <nav class="pages-nav">
                    {{#each pages}}
                    <a href="{{path}}/">{{title}}</a>
                    {{/each}}
                </nav>
                {{/if}}
            </header>

            <main>
                <article>
                    <h1>{{post.title}}</h1>
                    <div class="post-meta">{{post.date}}</div>
                    <div class="content">{{{post.content}}}</div>
                </article>
            </main>
        </body>
        </html>
    """.trimIndent()

    val PAGE_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{page.title}} - {{siteName}}</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
                h1 { color: #333; }
                .pages-nav { margin: 20px 0; }
                .pages-nav a { margin-right: 15px; text-decoration: none; color: #0066cc; }
                .content { line-height: 1.6; }
            </style>
        </head>
        <body>
            <header>
                <h1><a href="{{baseUrl}}" style="text-decoration: none; color: inherit;">{{siteName}}</a></h1>
                {{#if pages}}
                <nav class="pages-nav">
                    {{#each pages}}
                    <a href="{{path}}/">{{title}}</a>
                    {{/each}}
                </nav>
                {{/if}}
            </header>

            <main>
                <article>
                    <h1>{{page.title}}</h1>
                    <div class="content">{{{page.content}}}</div>
                </article>
            </main>
        </body>
        </html>
    """.trimIndent()
}