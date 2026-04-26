package com.potomushto.statik.cms

internal object CmsWebAssets {
    fun indexHtml(siteName: String, basePath: String, sharedStylesheetHrefs: List<String> = emptyList()): String {
        val sharedStylesheetsHtml = sharedStylesheetHrefs.joinToString("\n") { href ->
            """  <link rel="stylesheet" href="${htmlAttribute(href)}">"""
        }
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>$siteName CMS</title>
              $sharedStylesheetsHtml
              <link rel="stylesheet" href="$basePath/styles.css">
            </head>
            <body>
              <div id="workbench" class="workbench">
                <aside class="rail">
                  <header class="rail-header">
                    <p class="eyebrow">Statik CMS</p>
                    <h1>$siteName</h1>
                  </header>

                  <div id="status-chips" class="status-strip status-strip-loading" aria-live="polite" aria-busy="true">
                    <span class="chip chip-skeleton" aria-hidden="true">000 dirty</span>
                    <span class="chip chip-skeleton" aria-hidden="true">synced 00:00</span>
                  </div>
                  <div class="rail-actions">
                    <button id="refresh-index" class="tree-action" type="button">Refresh Index</button>
                  </div>

                  <section class="tree-section">
                    <div class="tree-section-header content-tree-header">
                      <div class="content-tabs" role="tablist" aria-label="Content type">
                        <button id="content-tab-posts" class="content-tab active" type="button" role="tab" aria-selected="true" data-content-type="POST">Posts</button>
                        <button id="content-tab-pages" class="content-tab" type="button" role="tab" aria-selected="false" data-content-type="PAGE">Pages</button>
                        <button id="content-tab-media" class="content-tab" type="button" role="tab" aria-selected="false" data-content-type="MEDIA">Media</button>
                      </div>
                      <div class="tree-actions">
                        <button id="new-content" class="tree-action" type="button">+ New</button>
                        <button id="upload-media" class="tree-action" type="button" hidden>Upload</button>
                        <button id="rename-media" class="tree-action" type="button" hidden disabled>Rename</button>
                        <button id="delete-media" class="tree-action" type="button" hidden disabled>Delete</button>
                      </div>
                    </div>
                    <div id="content-tree" class="tree-root"></div>
                    <div id="media-tree" class="tree-root" hidden></div>
                  </section>
                </aside>

                <main class="editor-pane">
                  <header class="editor-header">
                    <div class="editor-heading-wrap">
                      <button id="rail-toggle" class="rail-toggle" type="button" aria-label="Collapse navigation" aria-pressed="false">&lt;</button>
                      <div class="editor-heading">
                        <p class="eyebrow">Editing</p>
                        <h2 id="editor-title">Select a file</h2>
                        <p id="editor-subtitle" class="muted">Choose a post, page, or media item from the left.</p>
                      </div>
                    </div>
                    <div class="header-actions">
                      <a id="preview-link" href="/" target="_blank" rel="noreferrer">Preview</a>
                      <button id="logs-button" type="button">Logs</button>
                      <button id="sync-button" type="button">Sync</button>
                    </div>
                  </header>

                  <input id="content-type" type="hidden">
                  <input id="source-path" type="hidden">

                  <section class="editor-card source-card">
                    <div class="editor-toolbar">
                      <span>Source</span>
                      <span id="autosave-status" class="toolbar-status">Autosave on</span>
                    </div>
                    <textarea id="source-document" spellcheck="false" class="source-area"></textarea>
                  </section>

                </main>
              </div>

              <dialog id="sync-dialog" class="sync-dialog">
                <form method="dialog" class="sync-form">
                  <div class="sync-dialog-copy">
                    <p class="eyebrow">Git Sync</p>
                    <h3>Commit changes</h3>
                    <p class="muted">Leave the message blank to use the default commit message.</p>
                  </div>

                  <label class="compact-field">
                    <span>Commit Message</span>
                    <input id="sync-commit-message" type="text" placeholder="cms: sync content changes">
                  </label>

                  <label class="dialog-toggle" for="sync-push">
                    <input id="sync-push" type="checkbox" checked>
                    <span>Push commit to remote</span>
                  </label>

                  <div class="dialog-actions">
                    <button type="submit" value="cancel">Cancel</button>
                    <button type="submit" value="confirm" class="primary">Sync</button>
                  </div>
                </form>
              </dialog>

              <input id="media-file-input" type="file" class="hidden-file-input" multiple>

              <dialog id="upload-dialog" class="sync-dialog">
                <form method="dialog" class="sync-form">
                  <div class="sync-dialog-copy">
                    <p class="eyebrow">Media</p>
                    <h3>Upload files</h3>
                    <p id="upload-summary" class="muted">Choose where the selected files should be uploaded.</p>
                  </div>

                  <label class="compact-field">
                    <span>Target Folder</span>
                    <input id="upload-target-directory" type="text" placeholder="static/images">
                  </label>

                  <div class="dialog-actions">
                    <button type="submit" value="cancel">Cancel</button>
                    <button type="submit" value="confirm" class="primary">Upload</button>
                  </div>
                </form>
              </dialog>

              <dialog id="rename-dialog" class="sync-dialog">
                <form method="dialog" class="sync-form">
                  <div class="sync-dialog-copy">
                    <p class="eyebrow">Media</p>
                    <h3>Rename item</h3>
                    <p id="rename-summary" class="muted">Update the source path for the selected file or folder.</p>
                  </div>

                  <label class="compact-field">
                    <span>New Path</span>
                    <input id="rename-target-path" type="text" placeholder="static/images/renamed.png">
                  </label>

                  <div class="dialog-actions">
                    <button type="submit" value="cancel">Cancel</button>
                    <button type="submit" value="confirm" class="primary">Rename</button>
                  </div>
                </form>
              </dialog>

              <dialog id="delete-dialog" class="sync-dialog">
                <form method="dialog" class="sync-form">
                  <div class="sync-dialog-copy">
                    <p class="eyebrow">Media</p>
                    <h3>Delete item</h3>
                    <p id="delete-summary" class="muted">This will remove the selected media path.</p>
                  </div>

                  <div class="dialog-actions">
                    <button type="submit" value="cancel">Cancel</button>
                    <button type="submit" value="confirm" class="primary">Delete</button>
                  </div>
                </form>
              </dialog>

              <dialog id="log-dialog" class="log-dialog">
                <div class="log-shell">
                  <div class="editor-toolbar">
                    <span>Logs</span>
                    <button id="close-logs" type="button">Close</button>
                  </div>
                  <pre id="activity-log">Loading CMS state…</pre>
                </div>
              </dialog>

              <script>window.STATIK_CMS_BASE_PATH = ${jsonString(basePath)};</script>
              <script src="$basePath/app.js"></script>
            </body>
            </html>
        """.trimIndent()
    }

    fun loginHtml(siteName: String, basePath: String): String {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>$siteName CMS Login</title>
              <link rel="stylesheet" href="$basePath/styles.css">
            </head>
            <body>
              <main class="login-shell">
                <section class="login-card">
                  <p class="eyebrow">Statik CMS</p>
                  <h1>$siteName</h1>
                  <p class="muted">Authentication is restricted to the configured GitHub account.</p>
                  <a class="login-button" href="$basePath/auth/github">Continue With GitHub</a>
                </section>
              </main>
            </body>
            </html>
        """.trimIndent()
    }

    fun installHtml(siteName: String, basePath: String, repositoryName: String): String {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>$siteName CMS Setup</title>
              <link rel="stylesheet" href="$basePath/styles.css">
            </head>
            <body>
              <main class="login-shell">
                <section class="login-card">
                  <p class="eyebrow">Statik CMS</p>
                  <h1>Install Access</h1>
                  <p class="muted">The configured repository <strong>$repositoryName</strong> is not connected yet.</p>
                  <p class="muted">Continue to GitHub, install the app on that repository, and Statik will create the checkout inside this host automatically.</p>
                  <a class="login-button" href="$basePath/install">Install GitHub App</a>
                </section>
              </main>
            </body>
            </html>
        """.trimIndent()
    }

    val stylesCss: String = """
        :root {
          --bg: #f4efe8;
          --rail: #ebe4d9;
          --panel: rgba(255, 251, 246, 0.92);
          --panel-strong: #fffdf9;
          --line: rgba(82, 63, 38, 0.14);
          --line-strong: rgba(82, 63, 38, 0.26);
          --text: #241b12;
          --muted: #756553;
          --accent: #2f5d50;
          --accent-soft: rgba(47, 93, 80, 0.12);
          --warn: #9a4e2c;
          --warn-soft: rgba(154, 78, 44, 0.12);
          --editor-bg: rgba(255, 255, 255, 0.65);
          --shadow: 0 18px 48px rgba(42, 28, 15, 0.08);
          --radius: 18px;
        }

        * {
          box-sizing: border-box;
        }

        html,
        body {
          margin: 0;
          min-height: 100%;
        }

        body {
          font-family: "IBM Plex Sans", "Avenir Next", sans-serif;
          color: var(--text);
          background:
            radial-gradient(circle at top left, rgba(47, 93, 80, 0.10), transparent 28%),
            radial-gradient(circle at bottom right, rgba(154, 78, 44, 0.10), transparent 26%),
            linear-gradient(180deg, #f6f1ea, #eee7dd);
        }

        button,
        input,
        textarea,
        select {
          font: inherit;
          color: inherit;
        }

        a {
          color: var(--accent);
          text-decoration: none;
        }

        .workbench {
          min-height: 100vh;
          display: grid;
          grid-template-columns: 320px minmax(0, 1fr);
        }

        .workbench.rail-collapsed {
          grid-template-columns: minmax(0, 1fr);
        }

        .rail {
          padding: 18px 14px;
          background: linear-gradient(180deg, rgba(239, 233, 224, 0.96), rgba(232, 224, 212, 0.96));
          border-right: 1px solid var(--line);
          display: grid;
          grid-template-rows: auto auto auto minmax(0, 1fr) minmax(0, 1fr);
          gap: 12px;
          backdrop-filter: blur(16px);
        }

        .workbench.rail-collapsed .rail {
          display: none;
        }

        .rail-header h1,
        .editor-heading h2 {
          margin: 0;
          font-size: 1.4rem;
          line-height: 1.1;
        }

        .eyebrow {
          margin: 0 0 6px;
          font-size: 0.72rem;
          letter-spacing: 0.14em;
          text-transform: uppercase;
          color: var(--muted);
        }

        .muted {
          margin: 0;
          color: var(--muted);
          line-height: 1.45;
        }

        .status-strip {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          min-height: 32px;
          align-content: flex-start;
        }

        .rail-actions {
          display: flex;
          align-items: flex-start;
        }

        .chip {
          display: inline-flex;
          align-items: center;
          padding: 4px 8px;
          border-radius: 999px;
          font-size: 0.74rem;
          border: 1px solid var(--line);
          background: rgba(255, 255, 255, 0.55);
          color: var(--muted);
        }

        .chip.warn {
          background: var(--warn-soft);
          color: var(--warn);
          border-color: rgba(154, 78, 44, 0.18);
        }

        .status-strip-loading .chip-skeleton {
          color: transparent;
          border-color: rgba(82, 63, 38, 0.08);
          background: linear-gradient(90deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.52));
          background-size: 200% 100%;
          user-select: none;
        }

        @media (prefers-reduced-motion: no-preference) {
          .status-strip-loading .chip-skeleton {
            animation: chip-loading 1.2s linear infinite;
          }
        }

        @keyframes chip-loading {
          from {
            background-position: 200% 0;
          }

          to {
            background-position: -200% 0;
          }
        }

        .tree-section {
          min-height: 0;
          background: rgba(255, 251, 246, 0.5);
          border: 1px solid var(--line);
          border-radius: var(--radius);
          overflow: hidden;
          display: grid;
          grid-template-rows: auto minmax(0, 1fr);
        }

        .tree-section-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 8px;
          padding: 10px 12px;
          border-bottom: 1px solid var(--line);
          font-size: 0.78rem;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          color: var(--muted);
        }

        .content-tree-header {
          align-items: center;
        }

        .content-tabs {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
        }

        .content-tab {
          border: 1px solid transparent;
          border-radius: 999px;
          background: transparent;
          padding: 6px 10px;
          font-size: 0.74rem;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          color: var(--muted);
          cursor: pointer;
          transition: border-color 120ms ease, background 120ms ease, color 120ms ease;
        }

        .content-tab:hover {
          border-color: var(--line);
          background: rgba(255, 255, 255, 0.55);
        }

        .content-tab.active {
          border-color: var(--line);
          background: rgba(255, 255, 255, 0.82);
          color: var(--text);
        }

        .tree-actions {
          display: flex;
          flex-wrap: wrap;
          justify-content: flex-end;
          gap: 6px;
        }

        .tree-action,
        .header-actions button,
        .header-actions a,
        .login-button {
          border: 1px solid var(--line);
          border-radius: 10px;
          background: rgba(255, 255, 255, 0.72);
          padding: 8px 10px;
          cursor: pointer;
          transition: border-color 120ms ease, background 120ms ease;
        }

        .tree-action:hover,
        .header-actions button:hover,
        .header-actions a:hover,
        .login-button:hover {
          border-color: var(--line-strong);
          background: rgba(255, 255, 255, 0.95);
        }

        button:disabled {
          opacity: 0.52;
          cursor: not-allowed;
        }

        .tree-root {
          overflow: auto;
          padding: 8px 0;
        }

        .tree-empty {
          margin: 0;
          padding: 12px;
          color: var(--muted);
          font-size: 0.88rem;
        }

        .tree-folder {
          padding: 8px 12px 6px calc(12px + var(--depth, 0) * 14px);
          color: var(--muted);
          font-size: 0.78rem;
          letter-spacing: 0.04em;
          text-transform: uppercase;
        }

        .tree-file {
          width: 100%;
          text-align: left;
          border: 0;
          border-radius: 0;
          background: transparent;
          border-left: 2px solid transparent;
          padding: 9px 12px 9px calc(12px + var(--depth, 0) * 14px);
          cursor: pointer;
        }

        .tree-file:hover {
          background: rgba(255, 255, 255, 0.55);
        }

        .tree-file.active {
          background: rgba(47, 93, 80, 0.10);
          border-left-color: var(--accent);
        }

        .tree-file.tree-file-editing {
          cursor: default;
          background: rgba(255, 255, 255, 0.7);
          border-left-color: var(--accent);
        }

        .tree-file-main,
        .tree-file-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          min-width: 0;
        }

        .tree-file-main {
          justify-content: space-between;
        }

        .tree-file-name {
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          font-size: 0.86rem;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .tree-file-meta {
          margin-top: 5px;
          color: var(--muted);
          font-size: 0.74rem;
          min-height: 1em;
        }

        .tree-file-badges {
          display: flex;
          gap: 6px;
          flex-shrink: 0;
        }

        .tree-inline-input {
          width: 100%;
          border: 1px solid rgba(47, 93, 80, 0.2);
          border-radius: 10px;
          background: rgba(255, 255, 255, 0.92);
          padding: 8px 10px;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          font-size: 0.84rem;
          line-height: 1.3;
        }

        .badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 6px;
          border-radius: 999px;
          background: var(--accent-soft);
          color: var(--accent);
          font-size: 0.68rem;
          line-height: 1;
          text-transform: uppercase;
          letter-spacing: 0.08em;
        }

        .badge.warn {
          background: var(--warn-soft);
          color: var(--warn);
        }

        .editor-pane {
          min-width: 0;
          padding: 18px;
          display: grid;
          grid-template-rows: auto minmax(0, 1fr);
          gap: 12px;
        }

        .editor-header,
        .editor-card,
        .login-card {
          background: var(--panel);
          border: 1px solid var(--line);
          border-radius: 22px;
          box-shadow: var(--shadow);
        }

        .editor-header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 14px;
          padding: 16px 18px;
        }

        .editor-heading-wrap {
          min-width: 0;
          display: flex;
          align-items: flex-start;
          gap: 12px;
        }

        .editor-heading {
          min-width: 0;
        }

        .header-actions {
          display: flex;
          flex-wrap: wrap;
          justify-content: flex-end;
          gap: 8px;
        }

        .rail-toggle {
          width: 30px;
          height: 30px;
          flex: 0 0 auto;
          border: 1px solid var(--line);
          border-radius: 999px;
          background: rgba(255, 255, 255, 0.72);
          padding: 0;
          cursor: pointer;
          line-height: 1;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          transition: border-color 120ms ease, background 120ms ease;
        }

        .rail-toggle:hover {
          border-color: var(--line-strong);
          background: rgba(255, 255, 255, 0.95);
        }

        .editor-card {
          padding: 12px 14px;
        }

        .meta-grid {
          display: grid;
          grid-template-columns: minmax(0, 1fr);
          gap: 10px;
          align-items: end;
        }

        .compact-field {
          display: grid;
          gap: 6px;
          min-width: 0;
        }

        .compact-field span,
        .editor-toolbar {
          font-size: 0.74rem;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          color: var(--muted);
        }

        input,
        select,
        textarea {
          width: 100%;
          border: 1px solid var(--line);
          border-radius: 12px;
          background: var(--editor-bg);
          padding: 10px 12px;
        }

        textarea {
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          line-height: 1.55;
          resize: vertical;
        }

        .source-card {
          min-height: 0;
          display: grid;
          grid-template-rows: auto minmax(0, 1fr);
          gap: 10px;
        }

        .editor-toolbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
        }

        .toolbar-status {
          text-transform: none;
          letter-spacing: 0;
        }

        .toolbar-status.warn {
          color: var(--warn);
        }

        .source-area {
          min-height: 560px;
          height: 100%;
        }

        .log-dialog {
          width: min(960px, calc(100vw - 32px));
          height: min(72vh, 760px);
          padding: 0;
          border: 1px solid var(--line);
          border-radius: 22px;
          background: var(--panel-strong);
          box-shadow: var(--shadow);
        }

        .log-dialog::backdrop {
          background: rgba(36, 27, 18, 0.28);
          backdrop-filter: blur(4px);
        }

        .log-shell {
          height: 100%;
          display: grid;
          grid-template-rows: auto minmax(0, 1fr);
          gap: 10px;
          padding: 16px;
        }

        .log-shell .editor-toolbar button {
          border: 1px solid var(--line);
          border-radius: 10px;
          background: rgba(255, 255, 255, 0.72);
          padding: 9px 12px;
          cursor: pointer;
        }

        .log-shell pre {
          margin: 0;
          min-height: 0;
          overflow: auto;
          white-space: pre-wrap;
          word-break: break-word;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          font-size: 0.83rem;
          color: var(--muted);
          border: 1px solid var(--line);
          border-radius: 14px;
          background: var(--editor-bg);
          padding: 14px;
        }

        .sync-dialog {
          width: min(460px, calc(100vw - 32px));
          padding: 0;
          border: 1px solid var(--line);
          border-radius: 22px;
          background: var(--panel-strong);
          box-shadow: var(--shadow);
        }

        .sync-dialog::backdrop {
          background: rgba(36, 27, 18, 0.28);
          backdrop-filter: blur(4px);
        }

        .sync-form {
          display: grid;
          gap: 16px;
          padding: 20px;
        }

        .sync-dialog-copy {
          display: grid;
          gap: 8px;
        }

        .sync-dialog-copy h3 {
          margin: 0;
          font-size: 1.2rem;
        }

        .dialog-actions {
          display: flex;
          justify-content: flex-end;
          gap: 10px;
        }

        .dialog-actions button {
          border: 1px solid var(--line);
          border-radius: 10px;
          background: rgba(255, 255, 255, 0.72);
          padding: 9px 12px;
          cursor: pointer;
        }

        .dialog-actions .primary {
          border-color: rgba(47, 93, 80, 0.2);
          background: var(--accent);
          color: #fff;
        }

        .hidden-file-input {
          display: none;
        }

        .dialog-toggle {
          display: flex;
          align-items: center;
          gap: 10px;
          color: var(--text);
        }

        .dialog-toggle input {
          width: auto;
          margin: 0;
          padding: 0;
          border: 0;
          background: transparent;
          accent-color: var(--accent);
        }

        .login-shell {
          min-height: 100vh;
          display: grid;
          place-items: center;
          padding: 24px;
        }

        .login-card {
          max-width: 460px;
          padding: 30px;
        }

        .login-button {
          display: inline-flex;
          margin-top: 18px;
          background: rgba(47, 93, 80, 0.12);
          color: var(--accent);
          border-color: rgba(47, 93, 80, 0.2);
        }

        @media (max-width: 1100px) {
          .workbench {
            grid-template-columns: 1fr;
          }

          .rail {
            grid-template-rows: auto auto auto minmax(180px, auto) minmax(180px, auto);
            border-right: 0;
            border-bottom: 1px solid var(--line);
          }

          .meta-grid {
            grid-template-columns: 1fr;
          }
        }

        @media (max-width: 720px) {
          .editor-pane {
            padding: 12px;
          }

          .editor-header {
            flex-direction: column;
          }

          .header-actions {
            justify-content: flex-start;
          }

          .editor-heading-wrap {
            width: 100%;
          }

          .source-area {
            min-height: 320px;
          }
        }
    """.trimIndent()

    val appJs: String = """
        (() => {
          const basePath = window.STATIK_CMS_BASE_PATH || "/__statik__/cms";
          const apiBase = basePath + "/api";
          const AUTOSAVE_DELAY_MS = 5000;
          const RAIL_COLLAPSED_KEY = "statik.cms.railCollapsed";
          const state = {
            items: [],
            mediaItems: [],
            mediaRoots: [],
            activeContentTab: "POST",
            railCollapsed: false,
            selected: null,
            selectedMediaPath: null,
            selectedMediaKind: null,
            currentPreviewPath: "/",
            renamingContentPath: null,
            mode: "empty",
            status: null,
            lastSync: null,
            pendingUploadFiles: []
          };

          const elements = {
            workbench: document.getElementById("workbench"),
            railToggle: document.getElementById("rail-toggle"),
            contentTree: document.getElementById("content-tree"),
            contentTabPosts: document.getElementById("content-tab-posts"),
            contentTabPages: document.getElementById("content-tab-pages"),
            contentTabMedia: document.getElementById("content-tab-media"),
            mediaTree: document.getElementById("media-tree"),
            status: document.getElementById("status-chips"),
            log: document.getElementById("activity-log"),
            previewLink: document.getElementById("preview-link"),
            editorTitle: document.getElementById("editor-title"),
            editorSubtitle: document.getElementById("editor-subtitle"),
            type: document.getElementById("content-type"),
            sourcePath: document.getElementById("source-path"),
            source: document.getElementById("source-document"),
            autosaveStatus: document.getElementById("autosave-status"),
            logsButton: document.getElementById("logs-button"),
            logDialog: document.getElementById("log-dialog"),
            closeLogs: document.getElementById("close-logs"),
            sync: document.getElementById("sync-button"),
            refresh: document.getElementById("refresh-index"),
            newContent: document.getElementById("new-content"),
            uploadMedia: document.getElementById("upload-media"),
            renameMedia: document.getElementById("rename-media"),
            deleteMedia: document.getElementById("delete-media"),
            syncDialog: document.getElementById("sync-dialog"),
            syncCommitMessage: document.getElementById("sync-commit-message"),
            syncPush: document.getElementById("sync-push"),
            mediaFileInput: document.getElementById("media-file-input"),
            uploadDialog: document.getElementById("upload-dialog"),
            uploadSummary: document.getElementById("upload-summary"),
            uploadTargetDirectory: document.getElementById("upload-target-directory"),
            renameDialog: document.getElementById("rename-dialog"),
            renameSummary: document.getElementById("rename-summary"),
            renameTargetPath: document.getElementById("rename-target-path"),
            deleteDialog: document.getElementById("delete-dialog"),
            deleteSummary: document.getElementById("delete-summary")
          };
          let autosaveTimer = null;
          let saveInFlight = false;
          let savePromise = null;
          let saveController = null;
          let activeSaveSnapshot = null;
          let contentDraftSnapshot = null;
          let lastSavedSnapshot = null;
          const baseDocumentTitle = document.title || "Statik CMS";

          function log(message) {
            const stamp = new Date().toLocaleTimeString();
            elements.log.textContent = "[" + stamp + "] " + message + "\n" + elements.log.textContent;
          }

          async function api(path, options = {}) {
            const response = await fetch(apiBase + path, {
              headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
              },
              ...options
            });

            if (!response.ok) {
              if (response.status === 401) {
                window.location.href = basePath + "/login";
                return null;
              }
              const text = await response.text();
              throw new Error(text || ("Request failed with " + response.status));
            }

            const contentType = response.headers.get("content-type") || "";
            if (contentType.includes("application/json")) {
              return response.json();
            }
            return null;
          }

          function escapeHtml(value) {
            return String(value || "")
              .replaceAll("&", "&amp;")
              .replaceAll("<", "&lt;")
              .replaceAll(">", "&gt;")
              .replaceAll('"', "&quot;");
          }

          function chip(text, warn) {
            return '<span class="chip' + (warn ? ' warn' : '') + '">' + escapeHtml(text) + "</span>";
          }

          function formatClock(timestamp) {
            return new Date(timestamp).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit"
            });
          }

          function syncBadge(sync) {
            if (!sync) {
              return null;
            }

            if (!sync.committed) {
              return {
                text: "sync no changes",
                warn: false
              };
            }

            if (sync.pushAttempted) {
              return sync.pushSucceeded
                ? { text: "sync pushed", warn: false }
                : { text: "sync push failed", warn: true };
            }

            return {
              text: "sync local only",
              warn: false
            };
          }

          function syncSummary(sync) {
            if (!sync) {
              return "";
            }

            if (!sync.committed) {
              return "No new commit was created.";
            }

            if (sync.pushAttempted) {
              return sync.pushSucceeded ? "Push succeeded." : "Push failed.";
            }

            return "Push was not attempted.";
          }

          function updateSyncState(sync) {
            state.lastSync = sync;
            if (state.status) {
              renderStatus(state.status);
            }
          }

          function setEditorEditable(editable) {
            elements.sourcePath.readOnly = !editable;
            elements.source.readOnly = !editable;
            if (!editable) {
              setAutosaveStatus("Read only");
            }
          }

          function setAutosaveStatus(text, warn = false) {
            if (!elements.autosaveStatus) {
              return;
            }
            elements.autosaveStatus.textContent = text;
            elements.autosaveStatus.classList.toggle("warn", warn);
          }

          function isAbortError(error) {
            return !!error && error.name === "AbortError";
          }

          function snapshotKeyForContent(type, sourcePath, source) {
            return JSON.stringify({
              type: type,
              sourcePath: sourcePath,
              source: source
            });
          }

          function snapshotForContent(type, sourcePath, source) {
            const normalizedType = type || null;
            const normalizedSourcePath = String(sourcePath || "").trim();
            const normalizedSource = String(source || "");
            const parsed = parseDocument(normalizedSource);
            return {
              type: normalizedType,
              sourcePath: normalizedSourcePath,
              source: normalizedSource,
              frontmatter: parsed.frontmatter,
              body: parsed.body,
              key: snapshotKeyForContent(normalizedType, normalizedSourcePath, normalizedSource)
            };
          }

          function currentContentSnapshot() {
            return contentDraftSnapshot;
          }

          function captureContentSnapshot() {
            if (!elements.type.value || state.mode !== "content") {
              contentDraftSnapshot = null;
              return null;
            }
            contentDraftSnapshot = snapshotForContent(
              elements.type.value,
              elements.sourcePath.value,
              elements.source.value
            );
            return contentDraftSnapshot;
          }

          function applyContentSnapshot(snapshot) {
            contentDraftSnapshot = snapshot ? { ...snapshot } : null;
            return contentDraftSnapshot;
          }

          function contentSnapshot() {
            return contentDraftSnapshot ? contentDraftSnapshot.key : null;
          }

          function savedDocumentSnapshot(document) {
            return snapshotForContent(
              document.type,
              document.sourcePath,
              serializeDocument(document.frontmatter || "", document.body || "")
            );
          }

          function rememberSavedSnapshot(statusText = "Autosave on", snapshot = currentContentSnapshot()) {
            lastSavedSnapshot = snapshot ? snapshot.key : null;
            const currentSnapshot = contentSnapshot();
            if (!currentSnapshot) {
              setAutosaveStatus("Read only");
            } else if (currentSnapshot === lastSavedSnapshot) {
              setAutosaveStatus(statusText);
            } else {
              setAutosaveStatus("Unsaved changes");
            }
          }

          function currentContentPath() {
            if (state.mode !== "content") {
              return null;
            }
            const snapshot = currentContentSnapshot();
            return snapshot ? snapshot.sourcePath : String(elements.sourcePath.value || "").trim();
          }

          function currentContentType() {
            if (state.mode !== "content") {
              return null;
            }
            const snapshot = currentContentSnapshot();
            return snapshot ? snapshot.type : (elements.type.value || null);
          }

          function currentContentExtension() {
            const sourcePath = currentContentPath() || String(elements.sourcePath.value || "").trim();
            const dotIndex = sourcePath.lastIndexOf(".");
            return dotIndex >= 0 ? sourcePath.slice(dotIndex + 1).toLowerCase() : "";
          }

          function activeContentPath() {
            return currentContentPath() || state.selected;
          }

          function cancelAutosaveTimer() {
            if (autosaveTimer !== null) {
              window.clearTimeout(autosaveTimer);
              autosaveTimer = null;
            }
          }

          function interruptSaveInFlight() {
            if (saveController) {
              saveController.abort();
            }
          }

          async function waitForSaveToSettle() {
            if (!savePromise) {
              return;
            }
            try {
              await savePromise;
            } catch (error) {
              if (!isAbortError(error)) {
                throw error;
              }
            }
          }

          async function runAutosave(expectedSnapshotKey = null) {
            autosaveTimer = null;
            const snapshot = currentContentSnapshot();
            if (!snapshot || snapshot.key === lastSavedSnapshot) {
              return;
            }
            if (expectedSnapshotKey && snapshot.key !== expectedSnapshotKey) {
              return;
            }
            return save({ autosave: true, snapshot: snapshot });
          }

          function scheduleAutosave(delay = AUTOSAVE_DELAY_MS) {
            cancelAutosaveTimer();
            const snapshot = currentContentSnapshot();
            if (!snapshot || snapshot.key === lastSavedSnapshot) {
              return;
            }

            setAutosaveStatus("Unsaved changes");
            autosaveTimer = window.setTimeout(() => {
              runAutosave(snapshot.key).catch(error => log(error.message));
            }, delay);
          }

          async function flushAutosave() {
            cancelAutosaveTimer();

            while (true) {
              const snapshot = currentContentSnapshot();
              if (!snapshot || snapshot.key === lastSavedSnapshot) {
                await waitForSaveToSettle();
                return;
              }

              if (saveInFlight) {
                const sameSnapshot = activeSaveSnapshot && activeSaveSnapshot.key === snapshot.key;
                if (!sameSnapshot) {
                  interruptSaveInFlight();
                }
                await waitForSaveToSettle();
                continue;
              }

              await save({ autosave: true, snapshot: snapshot });
            }
          }

          function setSyncBusy(isBusy) {
            elements.sync.disabled = isBusy;
            elements.sync.textContent = isBusy ? "Syncing..." : "Sync";
          }

          function openLogs() {
            if (!elements.logDialog || typeof elements.logDialog.showModal !== "function") {
              window.alert(elements.log.textContent.slice(0, 4000));
              return;
            }
            elements.logDialog.showModal();
          }

          function closeLogs() {
            if (elements.logDialog && elements.logDialog.open) {
              elements.logDialog.close();
            }
          }

          function previewPathFromOutputPath(outputPath) {
            const normalized = String(outputPath || "").trim().replace(/^\/+|\/+$/g, "");
            if (!normalized) {
              return "/";
            }
            const lastSegment = normalized.split("/").pop() || "";
            return lastSegment.includes(".")
              ? "/" + normalized
              : "/" + normalized + "/";
          }

          function previewHrefFromSitePath(path) {
            const normalized = String(path || "").trim();
            if (!normalized || normalized === "/") {
              return basePath + "/preview/";
            }
            return basePath + "/preview" + (normalized.startsWith("/") ? normalized : "/" + normalized);
          }

          function setPreviewHref(href) {
            state.currentPreviewPath = href || previewHrefFromSitePath("/");
            if (elements.previewLink) {
              elements.previewLink.href = state.currentPreviewPath;
            }
          }

          function setContentPreviewPath(path) {
            setPreviewHref(previewHrefFromSitePath(path));
          }

          async function openPreview(event) {
            event.preventDefault();
            let previewWindow = null;
            try {
              previewWindow = window.open("", "_blank");
              if (previewWindow) {
                previewWindow.opener = null;
              }
              await flushAutosave();
              const nextHref = state.currentPreviewPath || previewHrefFromSitePath("/");
              if (previewWindow) {
                previewWindow.location.replace(nextHref);
              } else {
                window.open(nextHref, "_blank", "noopener,noreferrer");
              }
            } catch (error) {
              if (previewWindow && !previewWindow.closed) {
                previewWindow.close();
              }
              throw error;
            }
          }

          function showEmptyEditor() {
            state.mode = "empty";
            state.selected = null;
            state.renamingContentPath = null;
            elements.type.value = "";
            elements.sourcePath.value = "";
            elements.source.value = "";
            setEditorEditable(false);
            contentDraftSnapshot = null;
            lastSavedSnapshot = null;
            setContentPreviewPath("/");
            setEditorHeading("Select a file", "Choose a post, page, or media item from the left.");
          }

          function renderStatus(status) {
            state.status = status;
            const chips = [chip(String(status.dirty) + " dirty", status.dirty > 0)];

            if (status.lastSyncedAt) {
              chips.push(chip("synced " + formatClock(status.lastSyncedAt), false));
            }

            const syncChip = syncBadge(state.lastSync);
            if (syncChip) {
              chips.push(chip(syncChip.text, syncChip.warn));
            }

            elements.status.classList.remove("status-strip-loading");
            elements.status.removeAttribute("aria-busy");
            elements.status.innerHTML = chips.join("");
          }

          function setRailCollapsed(collapsed) {
            state.railCollapsed = collapsed;
            if (elements.workbench) {
              elements.workbench.classList.toggle("rail-collapsed", collapsed);
            }
            if (elements.railToggle) {
              elements.railToggle.textContent = collapsed ? ">" : "<";
              elements.railToggle.setAttribute("aria-label", collapsed ? "Expand navigation" : "Collapse navigation");
              elements.railToggle.setAttribute("aria-pressed", collapsed ? "true" : "false");
            }
            try {
              window.localStorage.setItem(RAIL_COLLAPSED_KEY, collapsed ? "1" : "0");
            } catch (_error) {
              // Ignore storage failures in private browsing or locked-down contexts.
            }
          }

          function setActiveContentTab(type) {
            state.activeContentTab = type === "PAGE" ? "PAGE" : (type === "MEDIA" ? "MEDIA" : "POST");
            if (elements.contentTabPosts) {
              const postsActive = state.activeContentTab === "POST";
              elements.contentTabPosts.classList.toggle("active", postsActive);
              elements.contentTabPosts.setAttribute("aria-selected", postsActive ? "true" : "false");
            }
            if (elements.contentTabPages) {
              const pagesActive = state.activeContentTab === "PAGE";
              elements.contentTabPages.classList.toggle("active", pagesActive);
              elements.contentTabPages.setAttribute("aria-selected", pagesActive ? "true" : "false");
            }
            if (elements.contentTabMedia) {
              const mediaActive = state.activeContentTab === "MEDIA";
              elements.contentTabMedia.classList.toggle("active", mediaActive);
              elements.contentTabMedia.setAttribute("aria-selected", mediaActive ? "true" : "false");
            }
            if (elements.contentTree) {
              elements.contentTree.hidden = state.activeContentTab === "MEDIA";
            }
            if (elements.mediaTree) {
              elements.mediaTree.hidden = state.activeContentTab !== "MEDIA";
            }
            if (elements.newContent) {
              const contentActive = state.activeContentTab !== "MEDIA";
              elements.newContent.hidden = !contentActive;
              elements.newContent.disabled = !contentActive;
            }
            if (elements.uploadMedia) {
              const mediaActive = state.activeContentTab === "MEDIA";
              elements.uploadMedia.hidden = !mediaActive;
            }
            if (elements.renameMedia) {
              elements.renameMedia.hidden = state.activeContentTab !== "MEDIA";
            }
            if (elements.deleteMedia) {
              elements.deleteMedia.hidden = state.activeContentTab !== "MEDIA";
            }
            updateMediaActions();
          }

          function virtualContentItem(type, sourcePath) {
            const fileName = fileNameFromPath(sourcePath);
            return {
              type: type,
              sourcePath: sourcePath,
              outputPath: "",
              title: stripExtension(fileName),
              extension: fileName.includes(".") ? fileName.slice(fileName.lastIndexOf(".") + 1) : "",
              publishedAt: null,
              navOrder: null,
              isDraft: true,
              dirty: contentSnapshot() !== lastSavedSnapshot,
              updatedAt: Date.now()
            };
          }

          function groupItems(type) {
            const currentType = currentContentType();
            const currentPath = currentContentPath();
            const hasCurrentPath = !!currentPath && state.items.some(item => item.sourcePath === currentPath);
            let items = state.items.filter(item => item.type === type);

            if (
              state.mode === "content" &&
              currentType === type &&
              state.selected &&
              currentPath &&
              state.selected !== currentPath &&
              !hasCurrentPath
            ) {
              items = items.filter(item => item.sourcePath !== state.selected);
            }

            if (
              state.mode === "content" &&
              currentType === type &&
              currentPath &&
              !items.some(item => item.sourcePath === currentPath)
            ) {
              items = items.concat(virtualContentItem(type, currentPath));
            }

            return items;
          }

          function fileNameFromPath(sourcePath) {
            const parts = sourcePath.split("/");
            return parts[parts.length - 1] || sourcePath;
          }

          function stripExtension(fileName) {
            const index = fileName.lastIndexOf(".");
            return index >= 0 ? fileName.slice(0, index) : fileName;
          }

          function formatPublished(publishedAt) {
            if (!publishedAt) {
              return "";
            }
            return publishedAt.slice(0, 10);
          }

          function formatBytes(size) {
            if (!Number.isFinite(size)) {
              return "";
            }
            if (size < 1024) {
              return size + " B";
            }
            if (size < 1024 * 1024) {
              return (size / 1024).toFixed(1).replace(/\.0$/, "") + " KB";
            }
            return (size / (1024 * 1024)).toFixed(1).replace(/\.0$/, "") + " MB";
          }

          function secondaryMeta(item) {
            const fileName = fileNameFromPath(item.sourcePath);
            const parts = [];
            const normalizedTitle = String(item.title || "").trim().toLowerCase();
            const normalizedStem = stripExtension(fileName).trim().toLowerCase();

            if (normalizedTitle && normalizedTitle !== normalizedStem) {
              parts.push(item.title);
            }
            if (item.type === "POST" && item.publishedAt) {
              parts.push(formatPublished(item.publishedAt));
            }
            if (item.type === "PAGE" && item.navOrder !== null && item.navOrder !== undefined) {
              parts.push("nav " + item.navOrder);
            }

            return parts.join(" · ");
          }

          function mediaSecondaryMeta(item) {
            const parts = [];
            if (item.contentType) {
              parts.push(item.contentType);
            }
            if (item.size !== null && item.size !== undefined) {
              parts.push(formatBytes(item.size));
            }
            return parts.join(" · ");
          }

          function findMediaItem(sourcePath) {
            return state.mediaItems.find(item => item.sourcePath === sourcePath) || null;
          }

          function resolveMediaSelectionKind(sourcePath) {
            if (!sourcePath) {
              return null;
            }
            if (findMediaItem(sourcePath)) {
              return "file";
            }
            if (state.mediaRoots.includes(sourcePath)) {
              return "folder";
            }
            return state.mediaItems.some(item => item.sourcePath.startsWith(sourcePath + "/"))
              ? "folder"
              : null;
          }

          function mediaItemCount(sourcePath, kind) {
            if (!sourcePath) {
              return 0;
            }
            if (kind === "file") {
              return findMediaItem(sourcePath) ? 1 : 0;
            }
            return state.mediaItems.filter(item => item.sourcePath.startsWith(sourcePath + "/")).length;
          }

          function mediaEditorText(sourcePath, kind) {
            if (kind === "file") {
              const item = findMediaItem(sourcePath);
              if (!item) {
                return sourcePath;
              }

              return [
                "Media file",
                "",
                "Source path: " + item.sourcePath,
                "Public URL: " + item.publicPath,
                item.contentType ? "Content type: " + item.contentType : null,
                item.size !== null && item.size !== undefined ? "Size: " + formatBytes(item.size) : null,
                item.dirty ? "State: dirty" : "State: synced"
              ].filter(Boolean).join("\n");
            }

            const itemCount = mediaItemCount(sourcePath, kind);
            return [
              "Media folder",
              "",
              "Path: " + sourcePath,
              itemCount + " file(s)"
            ].join("\n");
          }

          function mediaSubtitle(sourcePath, kind) {
            if (kind === "file") {
              const item = findMediaItem(sourcePath);
              return item ? (item.publicPath || "media file") : "media file";
            }

            return mediaItemCount(sourcePath, kind) + " file(s)";
          }

          function parentMediaPath(sourcePath, kind) {
            if (!sourcePath) {
              return null;
            }

            const path = kind === "file"
              ? sourcePath.slice(0, sourcePath.lastIndexOf("/"))
              : sourcePath.slice(0, sourcePath.lastIndexOf("/"));

            return path || state.mediaRoots[0] || null;
          }

          function selectMedia(sourcePath, kind) {
            setActiveContentTab("MEDIA");
            state.mode = "media";
            state.selected = null;
            state.renamingContentPath = null;
            state.selectedMediaPath = sourcePath;
            state.selectedMediaKind = kind;
            contentDraftSnapshot = null;
            elements.type.value = "";
            elements.sourcePath.value = sourcePath;
            elements.source.value = mediaEditorText(sourcePath, kind);
            setEditorEditable(false);
            const mediaItem = kind === "file" ? findMediaItem(sourcePath) : null;
            setPreviewHref(mediaItem && mediaItem.publicPath ? mediaItem.publicPath : previewHrefFromSitePath("/"));
            setEditorHeading(fileNameFromPath(sourcePath), mediaSubtitle(sourcePath, kind));
            renderList();
            renderMediaTree();
            updateMediaActions();
          }

          function focusContentRenameInput() {
            const input = document.getElementById("content-path-rename");
            if (!input) {
              return;
            }

            input.focus();
            const value = input.value;
            const start = Math.max(value.lastIndexOf("/") + 1, 0);
            const dotIndex = value.lastIndexOf(".");
            const end = dotIndex > start ? dotIndex : value.length;
            input.setSelectionRange(start, end);
          }

          function beginContentRename(sourcePath) {
            if (!sourcePath || state.mode !== "content") {
              return;
            }
            state.renamingContentPath = sourcePath;
            renderList();
            window.setTimeout(() => focusContentRenameInput(), 0);
          }

          function cancelContentRename() {
            state.renamingContentPath = null;
            renderList();
          }

          function commitContentRename(nextPath) {
            const normalizedPath = String(nextPath || "")
              .trim()
              .replace(/^\/+/, "")
              .replaceAll("\\", "/");
            const currentPath = currentContentPath();

            if (!currentPath) {
              state.renamingContentPath = null;
              renderList();
              return;
            }

            if (!normalizedPath) {
              log("Source path cannot be blank.");
              renderList();
              window.setTimeout(() => focusContentRenameInput(), 0);
              return;
            }

            const conflictingItem = state.items.find(item => item.sourcePath === normalizedPath);
            if (conflictingItem && conflictingItem.sourcePath !== state.selected) {
              log("A file already exists at " + normalizedPath + ".");
              renderList();
              window.setTimeout(() => focusContentRenameInput(), 0);
              return;
            }

            state.renamingContentPath = null;
            if (normalizedPath === currentPath) {
              renderList();
              return;
            }

            elements.sourcePath.value = normalizedPath;
            setEditorHeading(fileNameFromPath(normalizedPath), elements.editorSubtitle.textContent || "");
            renderList();
            interruptSaveInFlight();
            captureContentSnapshot();
            scheduleAutosave(0);
          }

          function buildTree(items, rootLabel, type) {
            const root = {
              kind: "folder",
              name: rootLabel,
              children: [],
              folders: Object.create(null)
            };

            items.forEach(item => {
              const segments = item.sourcePath.split("/").slice(1);
              let cursor = root;

              segments.forEach((segment, index) => {
                const isLeaf = index === segments.length - 1;
                if (isLeaf) {
                  cursor.children.push({
                    kind: "file",
                    name: segment,
                    item: item
                  });
                  return;
                }

                if (!cursor.folders[segment]) {
                  const folder = {
                    kind: "folder",
                    name: segment,
                    children: [],
                    folders: Object.create(null)
                  };
                  cursor.folders[segment] = folder;
                  cursor.children.push(folder);
                }
                cursor = cursor.folders[segment];
              });
            });

            return sortContentTreeNodes(root.children, type);
          }

          function contentSortTimestamp(item) {
            const timestamp = item && item.publishedAt ? Date.parse(item.publishedAt) : Number.NaN;
            return Number.isFinite(timestamp) ? timestamp : Number.NEGATIVE_INFINITY;
          }

          function contentNodeSortTimestamp(node) {
            if (node.kind === "file") {
              return contentSortTimestamp(node.item);
            }

            return node.children.reduce((latest, child) => {
              return Math.max(latest, contentNodeSortTimestamp(child));
            }, Number.NEGATIVE_INFINITY);
          }

          function sortContentTreeNodes(nodes, type) {
            if (type !== "POST") {
              return nodes;
            }

            nodes.forEach(node => {
              if (node.kind === "folder") {
                sortContentTreeNodes(node.children, type);
              }
            });

            nodes.sort((left, right) => {
              const byTimestamp = contentNodeSortTimestamp(right) - contentNodeSortTimestamp(left);
              if (byTimestamp !== 0) {
                return byTimestamp;
              }

              if (left.kind !== right.kind) {
                return left.kind === "folder" ? -1 : 1;
              }

              return String(left.name).localeCompare(String(right.name));
            });

            return nodes;
          }

          function renderBadges(item) {
            const badges = [];
            if (item.isDraft) {
              badges.push('<span class="badge warn">draft</span>');
            }
            if (item.dirty) {
              badges.push('<span class="badge">dirty</span>');
            }
            return badges.join("");
          }

          function renderTreeNodes(nodes, depth) {
            return nodes.map(node => {
              if (node.kind === "folder") {
                return '<div class="tree-folder-block">' +
                  '<div class="tree-folder" style="--depth:' + depth + '">' + escapeHtml(node.name) + "</div>" +
                  renderTreeNodes(node.children, depth + 1) +
                  "</div>";
              }

              const item = node.item;
              const active = activeContentPath() === item.sourcePath ? " active" : "";
              const meta = secondaryMeta(item);
              if (state.renamingContentPath === item.sourcePath) {
                return '<div class="tree-file tree-file-editing' + active + '" style="--depth:' + depth + '">' +
                  '<input id="content-path-rename" class="tree-inline-input" type="text" value="' + escapeHtml(item.sourcePath) + '" aria-label="Source path">' +
                  '<div class="tree-file-meta">' + escapeHtml(meta || "Press Enter to rename") + "</div>" +
                "</div>";
              }
              return '<button class="tree-file' + active + '" type="button" data-source-path="' + escapeHtml(item.sourcePath) + '" style="--depth:' + depth + '">' +
                '<div class="tree-file-main">' +
                  '<span class="tree-file-name">' + escapeHtml(node.name) + "</span>" +
                  '<span class="tree-file-badges">' + renderBadges(item) + "</span>" +
                "</div>" +
                '<div class="tree-file-meta">' + escapeHtml(meta) + "</div>" +
              "</button>";
            }).join("");
          }

          function renderTree(target, items, label, type) {
            if (!items.length) {
              target.innerHTML = '<p class="tree-empty">No ' + label.toLowerCase() + " yet.</p>";
              return;
            }

            target.innerHTML = renderTreeNodes(buildTree(items, label, type), 0);
            target.querySelectorAll("button[data-source-path]").forEach(node => {
              node.addEventListener("click", async () => {
                const sourcePath = node.getAttribute("data-source-path");
                try {
                  await flushAutosave();
                  await openEntry(sourcePath);
                } catch (error) {
                  log(error.message);
                }
              });
              node.addEventListener("dblclick", async event => {
                event.preventDefault();
                const sourcePath = node.getAttribute("data-source-path");
                try {
                  await flushAutosave();
                  if (currentContentPath() !== sourcePath || state.mode !== "content") {
                    await openEntry(sourcePath, { logLoad: false });
                  }
                  beginContentRename(sourcePath);
                } catch (error) {
                  log(error.message);
                }
              });
            });

            const renameInput = target.querySelector("#content-path-rename");
            if (renameInput) {
              let completed = false;
              const finish = commit => {
                if (completed) {
                  return;
                }
                completed = true;
                if (commit) {
                  commitContentRename(renameInput.value);
                } else {
                  cancelContentRename();
                }
              };

              renameInput.addEventListener("keydown", event => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  finish(true);
                } else if (event.key === "Escape") {
                  event.preventDefault();
                  finish(false);
                }
              });
              renameInput.addEventListener("blur", () => finish(true));
            }
          }

          function renderList() {
            setActiveContentTab(state.activeContentTab);
            if (state.activeContentTab === "MEDIA") {
              return;
            }
            renderTree(
              elements.contentTree,
              groupItems(state.activeContentTab),
              state.activeContentTab === "POST" ? "Posts" : "Pages",
              state.activeContentTab
            );
          }

          function buildMediaTree(items, roots) {
            const rootNodes = roots.map(rootPath => ({
              kind: "folder",
              name: fileNameFromPath(rootPath),
              sourcePath: rootPath,
              children: [],
              folders: Object.create(null),
              isRoot: true
            }));
            const rootMap = Object.create(null);

            rootNodes.forEach(node => {
              rootMap[node.sourcePath] = node;
            });

            items.forEach(item => {
              const rootNode = rootMap[item.rootPath];
              if (!rootNode) {
                return;
              }

              const rootDepth = item.rootPath.split("/").length;
              const segments = item.sourcePath.split("/").slice(rootDepth);
              let cursor = rootNode;
              let currentPath = item.rootPath;

              segments.forEach((segment, index) => {
                const isLeaf = index === segments.length - 1;
                if (isLeaf) {
                  cursor.children.push({
                    kind: "file",
                    name: segment,
                    item: item
                  });
                  return;
                }

                currentPath += "/" + segment;
                if (!cursor.folders[currentPath]) {
                  const folder = {
                    kind: "folder",
                    name: segment,
                    sourcePath: currentPath,
                    children: [],
                    folders: Object.create(null),
                    isRoot: false
                  };
                  cursor.folders[currentPath] = folder;
                  cursor.children.push(folder);
                }
                cursor = cursor.folders[currentPath];
              });
            });

            return sortTreeNodes(rootNodes);
          }

          function sortTreeNodes(nodes) {
            nodes.sort((left, right) => {
              if (left.kind !== right.kind) {
                return left.kind === "folder" ? -1 : 1;
              }
              return String(left.name).localeCompare(String(right.name));
            });

            nodes.forEach(node => {
              if (node.kind === "folder") {
                sortTreeNodes(node.children);
              }
            });

            return nodes;
          }

          function renderMediaNodes(nodes, depth) {
            return nodes.map(node => {
              if (node.kind === "folder") {
                const active = state.selectedMediaPath === node.sourcePath && state.selectedMediaKind === "folder" ? " active" : "";
                const meta = node.isRoot ? "root folder" : "folder";
                return '<div class="tree-folder-block">' +
                  '<button class="tree-file' + active + '" type="button" data-media-path="' + escapeHtml(node.sourcePath) + '" data-media-kind="folder" style="--depth:' + depth + '">' +
                    '<div class="tree-file-main">' +
                      '<span class="tree-file-name">' + escapeHtml(node.name) + "</span>" +
                    "</div>" +
                    '<div class="tree-file-meta">' + escapeHtml(meta) + "</div>" +
                  "</button>" +
                  renderMediaNodes(node.children, depth + 1) +
                  "</div>";
              }

              const item = node.item;
              const active = state.selectedMediaPath === item.sourcePath && state.selectedMediaKind === "file" ? " active" : "";
              const meta = mediaSecondaryMeta(item);
              return '<button class="tree-file' + active + '" type="button" data-media-path="' + escapeHtml(item.sourcePath) + '" data-media-kind="file" style="--depth:' + depth + '">' +
                '<div class="tree-file-main">' +
                  '<span class="tree-file-name">' + escapeHtml(node.name) + "</span>" +
                  '<span class="tree-file-badges">' + (item.dirty ? '<span class="badge">dirty</span>' : "") + "</span>" +
                "</div>" +
                '<div class="tree-file-meta">' + escapeHtml(meta) + "</div>" +
              "</button>";
            }).join("");
          }

          function renderMediaTree() {
            if (!state.mediaRoots.length) {
              elements.mediaTree.innerHTML = '<p class="tree-empty">No asset roots configured.</p>';
              updateMediaActions();
              return;
            }

            if (!state.mediaItems.length) {
              const emptyRoots = state.mediaRoots.map(rootPath => ({
                kind: "folder",
                name: fileNameFromPath(rootPath),
                sourcePath: rootPath,
                children: [],
                folders: Object.create(null),
                isRoot: true
              }));
              elements.mediaTree.innerHTML = renderMediaNodes(emptyRoots, 0);
            } else {
              elements.mediaTree.innerHTML = renderMediaNodes(buildMediaTree(state.mediaItems, state.mediaRoots), 0);
            }

            elements.mediaTree.querySelectorAll("[data-media-path]").forEach(node => {
              node.addEventListener("click", async () => {
                const sourcePath = node.getAttribute("data-media-path");
                const kind = node.getAttribute("data-media-kind");
                try {
                  await flushAutosave();
                  selectMedia(sourcePath, kind);
                  log("Selected media " + sourcePath + ".");
                } catch (error) {
                  log(error.message);
                }
              });
            });

            updateMediaActions();
          }

          function selectedMediaIsRoot() {
            return state.selectedMediaKind === "folder" && state.mediaRoots.includes(state.selectedMediaPath);
          }

          function updateMediaActions() {
            const mediaActive = state.activeContentTab === "MEDIA";
            const hasSelection = !!state.selectedMediaPath;
            const disabled = !mediaActive || !hasSelection || selectedMediaIsRoot();
            elements.uploadMedia.disabled = !mediaActive;
            elements.renameMedia.disabled = disabled;
            elements.deleteMedia.disabled = disabled;
          }

          function currentMediaDirectory() {
            if (state.selectedMediaKind === "folder" && state.selectedMediaPath) {
              return state.selectedMediaPath;
            }
            if (state.selectedMediaKind === "file" && state.selectedMediaPath) {
              const lastSlash = state.selectedMediaPath.lastIndexOf("/");
              return lastSlash >= 0 ? state.selectedMediaPath.slice(0, lastSlash) : "";
            }
            return state.mediaRoots[0] || "";
          }

          function selectedMediaLabel() {
            return state.selectedMediaPath || "selected media";
          }

          function fileToBase64(file) {
            return new Promise((resolve, reject) => {
              const reader = new FileReader();
              reader.onload = () => {
                const result = String(reader.result || "");
                const commaIndex = result.indexOf(",");
                resolve(commaIndex >= 0 ? result.slice(commaIndex + 1) : result);
              };
              reader.onerror = () => reject(reader.error || new Error("Failed to read " + file.name));
              reader.readAsDataURL(file);
            });
          }

          function setEditorHeading(title, subtitle) {
            elements.editorTitle.textContent = title;
            elements.editorSubtitle.textContent = subtitle;
            const nextTitle = String(title || "").trim();
            document.title = nextTitle && nextTitle !== "Select a file"
              ? nextTitle + " · " + baseDocumentTitle
              : baseDocumentTitle;
          }

          function parseDocument(source) {
            const match = /^---\s*\r?\n([\s\S]*?)\r?\n---\s*\r?\n?([\s\S]*)$/.exec(source || "");
            if (!match) {
              return {
                frontmatter: "",
                body: source || ""
              };
            }

            return {
              frontmatter: match[1],
              body: match[2]
            };
          }

          function serializeDocument(frontmatter, body) {
            if (!String(frontmatter || "").trim()) {
              return body || "";
            }

            const normalizedFrontmatter = String(frontmatter).replace(/\s+$/, "");
            const normalizedBody = String(body || "").replace(/^\n/, "");
            return "---\n" + normalizedFrontmatter + "\n---\n" + normalizedBody;
          }

          function markdownBodyStart(source) {
            const normalizedSource = String(source || "");
            const match = /^---\s*\r?\n([\s\S]*?)\r?\n---\s*\r?\n?([\s\S]*)$/.exec(normalizedSource);
            if (!match) {
              return 0;
            }
            return normalizedSource.length - match[2].length;
          }

          function supportsMarkdownFormattingShortcuts() {
            return state.mode === "content" &&
              !elements.source.readOnly &&
              currentContentExtension() === "md";
          }

          function replaceSourceSelection(replacement, start, end, selectionStart, selectionEnd) {
            const scrollTop = elements.source.scrollTop;
            elements.source.focus();
            elements.source.setSelectionRange(start, end);
            elements.source.setRangeText(replacement, start, end, "end");
            elements.source.setSelectionRange(selectionStart, selectionEnd);
            elements.source.scrollTop = scrollTop;
            elements.source.dispatchEvent(new Event("input", { bubbles: true }));
          }

          function wrapSourceSelection(marker) {
            if (!supportsMarkdownFormattingShortcuts()) {
              return false;
            }

            const selectionStart = elements.source.selectionStart;
            const selectionEnd = elements.source.selectionEnd;
            const bodyStart = markdownBodyStart(elements.source.value);
            if (selectionStart < bodyStart || selectionEnd < bodyStart) {
              return false;
            }

            const selectedText = elements.source.value.slice(selectionStart, selectionEnd);
            const replacement = marker + selectedText + marker;
            if (selectionStart === selectionEnd) {
              const cursor = selectionStart + marker.length;
              replaceSourceSelection(replacement, selectionStart, selectionEnd, cursor, cursor);
              return true;
            }

            replaceSourceSelection(
              replacement,
              selectionStart,
              selectionEnd,
              selectionStart + marker.length,
              selectionEnd + marker.length
            );
            return true;
          }

          async function loadStatus() {
            try {
              const status = await api("/status");
              if (!status) return;
              renderStatus(status);
            } catch (error) {
              if (!state.status) {
                elements.status.classList.remove("status-strip-loading");
                elements.status.removeAttribute("aria-busy");
                elements.status.innerHTML = "";
              }
              throw error;
            }
          }

          async function loadList() {
            const response = await api("/content");
            if (!response) return;
            state.items = response.items;
            renderList();

            const stillExists = state.selected && state.items.some(item => item.sourcePath === state.selected);
            if (stillExists) {
              return;
            }

            if (state.mode === "media") {
              return;
            }

            if (state.items.length > 0) {
              const preferred = state.items.find(item => item.type === "POST") || state.items[0];
              await openEntry(preferred.sourcePath);
            } else if (!state.selectedMediaPath) {
              showEmptyEditor();
            }
          }

          async function loadMedia() {
            const response = await api("/media");
            if (!response) return;

            state.mediaItems = response.items || [];
            state.mediaRoots = response.roots || [];

            const selectionKind = resolveMediaSelectionKind(state.selectedMediaPath);
            if (selectionKind) {
              state.selectedMediaKind = selectionKind;
            } else {
              state.selectedMediaPath = null;
              state.selectedMediaKind = null;
            }

            renderMediaTree();

            if (state.mode === "media") {
              if (state.selectedMediaPath && state.selectedMediaKind) {
                elements.sourcePath.value = state.selectedMediaPath;
                elements.source.value = mediaEditorText(state.selectedMediaPath, state.selectedMediaKind);
                setEditorEditable(false);
                setEditorHeading(fileNameFromPath(state.selectedMediaPath), mediaSubtitle(state.selectedMediaPath, state.selectedMediaKind));
              } else {
                showEmptyEditor();
              }
            }
          }

          async function openEntry(sourcePath, options = {}) {
            const logLoad = options.logLoad !== false;
            const response = await api("/content/item?sourcePath=" + encodeURIComponent(sourcePath));
            if (!response) return;

            state.mode = "content";
            state.selected = response.sourcePath;
            state.renamingContentPath = null;
            state.selectedMediaPath = null;
            state.selectedMediaKind = null;
            setActiveContentTab(response.type);
            elements.type.value = response.type;
            elements.sourcePath.value = response.sourcePath;
            elements.source.value = serializeDocument(response.frontmatter || "", response.body || "");
            setEditorEditable(true);
            setContentPreviewPath(previewPathFromOutputPath(response.outputPath));

            const subtitle = response.isDraft ? "draft · " + response.title : response.title;
            setEditorHeading(fileNameFromPath(response.sourcePath), subtitle);
            renderList();
            renderMediaTree();
            rememberSavedSnapshot("Autosave on", captureContentSnapshot());
            if (logLoad) {
              log("Loaded " + response.sourcePath);
            }
          }

          function defaultPath(type) {
            return type === "POST" ? "posts/untitled.md" : "pages/untitled.md";
          }

          function defaultFrontmatter(type) {
            if (type === "POST") {
              return "title: Untitled\npublished: " + new Date().toISOString().slice(0, 19) + "\ndraft: true";
            }
            return "title: Untitled";
          }

          function startNew(type) {
            state.mode = "content";
            state.selected = null;
            state.renamingContentPath = null;
            state.selectedMediaPath = null;
            state.selectedMediaKind = null;
            setActiveContentTab(type);
            elements.type.value = type;
            elements.sourcePath.value = defaultPath(type);
            elements.source.value = serializeDocument(defaultFrontmatter(type), "");
            setEditorEditable(true);
            setContentPreviewPath(previewPathFromOutputPath(""));
            setEditorHeading(fileNameFromPath(elements.sourcePath.value), "new file");
            renderList();
            renderMediaTree();
            rememberSavedSnapshot("Autosave on", captureContentSnapshot());
            beginContentRename(elements.sourcePath.value);
            log("Preparing " + elements.sourcePath.value + ".");
          }

          async function save(options = {}) {
            const autosave = options.autosave === true;
            let snapshot = options.snapshot || currentContentSnapshot();
            if (!snapshot || !snapshot.type) {
              throw new Error("Select or create a post or page before saving.");
            }

            if (!snapshot.sourcePath) {
              throw new Error("Source path cannot be blank.");
            }

            if (snapshot.key === lastSavedSnapshot) {
              return;
            }

            if (saveInFlight) {
              const sameSnapshot = activeSaveSnapshot && activeSaveSnapshot.key === snapshot.key;
              if (sameSnapshot) {
                return savePromise;
              }
              interruptSaveInFlight();
              await waitForSaveToSettle();
              snapshot = currentContentSnapshot();
              if (!snapshot || snapshot.key === lastSavedSnapshot) {
                return;
              }
            }

            const payload = {
              type: snapshot.type,
              sourcePath: snapshot.sourcePath,
              previousSourcePath: state.selected && state.selected !== snapshot.sourcePath ? state.selected : null,
              frontmatter: snapshot.frontmatter,
              body: snapshot.body
            };

            saveInFlight = true;
            setAutosaveStatus("Saving...");
            const controller = new AbortController();
            saveController = controller;
            activeSaveSnapshot = snapshot;

            let promise;
            promise = (async () => {
              try {
                const response = await api("/content", {
                  method: "POST",
                  body: JSON.stringify(payload),
                  signal: controller.signal
                });
                if (!response) return;

                const currentSnapshot = currentContentSnapshot();
                if (!currentSnapshot || currentSnapshot.key !== snapshot.key) {
                  return;
                }

                state.selected = response.item.sourcePath;
                state.renamingContentPath = null;
                setActiveContentTab(response.item.type);
                elements.type.value = response.item.type;
                elements.sourcePath.value = response.item.sourcePath;
                setContentPreviewPath(previewPathFromOutputPath(response.item.outputPath));
                setEditorHeading(
                  fileNameFromPath(response.item.sourcePath),
                  response.item.isDraft ? "draft · " + response.item.title : response.item.title
                );
                log((autosave ? "Autosaved " : "Saved ") + response.item.sourcePath + " and rebuilt the site.");
                if (response.sync) {
                  updateSyncState(response.sync);
                  log(response.sync.message + " " + syncSummary(response.sync));
                }

                const savedSnapshot = savedDocumentSnapshot(response.item);
                applyContentSnapshot(savedSnapshot);
                await Promise.all([loadStatus(), loadList()]);
                rememberSavedSnapshot("Saved", savedSnapshot);
              } catch (error) {
                if (isAbortError(error)) {
                  return;
                }
                setAutosaveStatus("Save failed", true);
                throw error;
              } finally {
                if (saveController === controller) {
                  saveController = null;
                }
                if (activeSaveSnapshot && activeSaveSnapshot.key === snapshot.key) {
                  activeSaveSnapshot = null;
                }
                saveInFlight = false;
                if (savePromise === promise) {
                  savePromise = null;
                }
                if (contentSnapshot() !== lastSavedSnapshot) {
                  setAutosaveStatus("Unsaved changes");
                }
              }
            })();
            savePromise = promise;
            return promise;
          }

          async function sync(commitMessage, push) {
            setSyncBusy(true);
            log(push ? "Starting sync and push." : "Starting sync.");
            try {
              const response = await api("/sync", {
                method: "POST",
                body: JSON.stringify({
                  commitMessage: commitMessage || null,
                  push: push
                })
              });
              if (!response) return;
              updateSyncState(response);
              log(
                response.message +
                " " + syncSummary(response) +
                (response.commitId ? " Commit " + response.commitId.slice(0, 7) + "." : "")
              );
              await Promise.all([loadStatus(), loadList(), loadMedia()]);
            } finally {
              setSyncBusy(false);
            }
          }

          function openSyncDialog() {
            if (!elements.syncDialog || typeof elements.syncDialog.showModal !== "function") {
              const value = window.prompt("Commit message (leave blank for default):", "");
              if (value === null) {
                return;
              }
              const shouldPush = window.confirm("Push this commit to the remote repository?");
              sync(value.trim(), shouldPush).catch(error => log(error.message));
              return;
            }

            elements.syncCommitMessage.value = "";
            if (elements.syncPush) {
              elements.syncPush.checked = true;
            }
            elements.syncDialog.showModal();
            window.setTimeout(() => {
              elements.syncCommitMessage.focus();
            }, 0);
          }

          function openUploadDialog() {
            elements.mediaFileInput.value = "";
            elements.mediaFileInput.click();
          }

          async function uploadPendingFiles(targetDirectory) {
            const normalizedTarget = String(targetDirectory || "").trim() || currentMediaDirectory();
            let lastSelectedPath = null;

            for (const file of state.pendingUploadFiles) {
              const contentsBase64 = await fileToBase64(file);
              const response = await api("/media/upload", {
                method: "POST",
                body: JSON.stringify({
                  targetDirectory: normalizedTarget,
                  fileName: file.name,
                  contentsBase64: contentsBase64
                })
              });
              if (!response) {
                continue;
              }
              lastSelectedPath = response.selectedPath || lastSelectedPath;
              log(response.message);
            }

            state.pendingUploadFiles = [];
            await Promise.all([loadStatus(), loadMedia()]);
            if (lastSelectedPath) {
              selectMedia(lastSelectedPath, resolveMediaSelectionKind(lastSelectedPath) || "file");
            }
          }

          function openRenameDialog() {
            if (!state.selectedMediaPath || selectedMediaIsRoot()) {
              return;
            }

            if (!elements.renameDialog || typeof elements.renameDialog.showModal !== "function") {
              const value = window.prompt("Rename media path:", state.selectedMediaPath);
              if (!value || value.trim() === state.selectedMediaPath) {
                return;
              }
              renameMedia(value.trim()).catch(error => log(error.message));
              return;
            }

            elements.renameSummary.textContent = "Rename " + selectedMediaLabel() + ".";
            elements.renameTargetPath.value = state.selectedMediaPath;
            elements.renameDialog.showModal();
            window.setTimeout(() => {
              elements.renameTargetPath.focus();
              elements.renameTargetPath.select();
            }, 0);
          }

          async function renameMedia(targetPath) {
            const response = await api("/media/rename", {
              method: "POST",
              body: JSON.stringify({
                sourcePath: state.selectedMediaPath,
                targetPath: targetPath
              })
            });
            if (!response) return;

            log(response.message);
            await Promise.all([loadStatus(), loadMedia()]);
            const nextPath = response.selectedPath || targetPath;
            const nextKind = resolveMediaSelectionKind(nextPath);
            if (nextKind) {
              selectMedia(nextPath, nextKind);
            } else {
              showEmptyEditor();
            }
          }

          function openDeleteDialog() {
            if (!state.selectedMediaPath || selectedMediaIsRoot()) {
              return;
            }

            if (!elements.deleteDialog || typeof elements.deleteDialog.showModal !== "function") {
              if (!window.confirm("Delete " + selectedMediaLabel() + "?")) {
                return;
              }
              deleteMedia().catch(error => log(error.message));
              return;
            }

            elements.deleteSummary.textContent = "Delete " + selectedMediaLabel() + ".";
            elements.deleteDialog.showModal();
          }

          async function deleteMedia() {
            const fallbackPath = parentMediaPath(state.selectedMediaPath, state.selectedMediaKind);
            const response = await api("/media/delete", {
              method: "POST",
              body: JSON.stringify({
                sourcePath: state.selectedMediaPath
              })
            });
            if (!response) return;

            log(response.message);
            await Promise.all([loadStatus(), loadMedia()]);
            const nextKind = resolveMediaSelectionKind(fallbackPath);
            if (fallbackPath && nextKind) {
              selectMedia(fallbackPath, nextKind);
            } else {
              showEmptyEditor();
            }
          }

          async function refreshIndex() {
            log("Refreshing index from content and media on disk.");
            const response = await api("/refresh", { method: "POST" });
            if (!response) return;
            if (response.message) {
              log(response.message);
            }
            log("Refresh complete: " + response.items + " item(s), " + response.dirty + " dirty.");
            await Promise.all([loadStatus(), loadList(), loadMedia()]);
          }

          document.addEventListener("keydown", event => {
            const saveShortcut = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "s";
            if (!saveShortcut) {
              return;
            }
            event.preventDefault();
            save({ autosave: false }).catch(error => log(error.message));
          });

          elements.contentTabPosts.addEventListener("click", () => {
            setActiveContentTab("POST");
            renderList();
          });
          elements.contentTabPages.addEventListener("click", () => {
            setActiveContentTab("PAGE");
            renderList();
          });
          elements.contentTabMedia.addEventListener("click", () => {
            setActiveContentTab("MEDIA");
            renderMediaTree();
          });
          elements.newContent.addEventListener("click", async () => {
            try {
              await flushAutosave();
              startNew(state.activeContentTab);
            } catch (error) {
              log(error.message);
            }
          });
          elements.railToggle.addEventListener("click", () => {
            setRailCollapsed(!state.railCollapsed);
          });
          if (elements.previewLink) {
            elements.previewLink.addEventListener("click", event => {
              openPreview(event).catch(error => log(error.message));
            });
          }
          elements.logsButton.addEventListener("click", () => openLogs());
          elements.closeLogs.addEventListener("click", () => closeLogs());
          elements.uploadMedia.addEventListener("click", async () => {
            try {
              await flushAutosave();
              openUploadDialog();
            } catch (error) {
              log(error.message);
            }
          });
          elements.renameMedia.addEventListener("click", async () => {
            try {
              await flushAutosave();
              openRenameDialog();
            } catch (error) {
              log(error.message);
            }
          });
          elements.deleteMedia.addEventListener("click", async () => {
            try {
              await flushAutosave();
              openDeleteDialog();
            } catch (error) {
              log(error.message);
            }
          });
          elements.sync.addEventListener("click", async () => {
            try {
              await flushAutosave();
              openSyncDialog();
            } catch (error) {
              log(error.message);
            }
          });
          elements.refresh.addEventListener("click", async () => {
            try {
              await flushAutosave();
              await refreshIndex();
            } catch (error) {
              log(error.message);
            }
          });
          elements.source.addEventListener("input", () => {
            interruptSaveInFlight();
            captureContentSnapshot();
            scheduleAutosave();
          });
          elements.source.addEventListener("keydown", event => {
            if (!(event.metaKey || event.ctrlKey) || event.altKey) {
              return;
            }

            const key = event.key.toLowerCase();
            if (key !== "b" && key !== "i") {
              return;
            }

            if (!wrapSourceSelection(key === "b" ? "**" : "*")) {
              return;
            }

            event.preventDefault();
          });

          elements.mediaFileInput.addEventListener("change", () => {
            state.pendingUploadFiles = Array.from(elements.mediaFileInput.files || []);
            if (!state.pendingUploadFiles.length) {
              return;
            }

            const targetDirectory = currentMediaDirectory();
            const summary = state.pendingUploadFiles.length === 1
              ? state.pendingUploadFiles[0].name
              : state.pendingUploadFiles.length + " file(s) selected";

            if (!elements.uploadDialog || typeof elements.uploadDialog.showModal !== "function") {
              const value = window.prompt("Upload target folder:", targetDirectory);
              if (value === null) {
                state.pendingUploadFiles = [];
                return;
              }
              uploadPendingFiles(value.trim()).catch(error => log(error.message));
              return;
            }

            elements.uploadSummary.textContent = summary;
            elements.uploadTargetDirectory.value = targetDirectory;
            elements.uploadDialog.showModal();
            window.setTimeout(() => {
              elements.uploadTargetDirectory.focus();
              elements.uploadTargetDirectory.select();
            }, 0);
          });

          if (elements.syncDialog) {
            elements.syncDialog.addEventListener("close", () => {
              if (elements.syncDialog.returnValue !== "confirm") {
                return;
              }
              sync(
                elements.syncCommitMessage.value.trim(),
                elements.syncPush ? elements.syncPush.checked : true
              ).catch(error => log(error.message));
            });
          }

          if (elements.uploadDialog) {
            elements.uploadDialog.addEventListener("close", () => {
              if (elements.uploadDialog.returnValue !== "confirm") {
                state.pendingUploadFiles = [];
                return;
              }
              uploadPendingFiles(elements.uploadTargetDirectory.value.trim()).catch(error => log(error.message));
            });
          }

          if (elements.renameDialog) {
            elements.renameDialog.addEventListener("close", () => {
              if (elements.renameDialog.returnValue !== "confirm") {
                return;
              }
              const targetPath = elements.renameTargetPath.value.trim();
              if (!targetPath || targetPath === state.selectedMediaPath) {
                return;
              }
              renameMedia(targetPath).catch(error => log(error.message));
            });
          }

          if (elements.deleteDialog) {
            elements.deleteDialog.addEventListener("close", () => {
              if (elements.deleteDialog.returnValue !== "confirm") {
                return;
              }
              deleteMedia().catch(error => log(error.message));
            });
          }

          setActiveContentTab(state.activeContentTab);
          setContentPreviewPath("/");
          try {
            setRailCollapsed(window.localStorage.getItem(RAIL_COLLAPSED_KEY) === "1");
          } catch (_error) {
            setRailCollapsed(false);
          }

          Promise.all([loadStatus(), loadList(), loadMedia()])
            .then(() => log("CMS ready."))
            .catch(error => log(error.message));
        })();
    """.trimIndent()

    private fun htmlAttribute(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    else -> append(char)
                }
            }
        }
    }

    private fun jsonString(value: String): String {
        return buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
    }
}
