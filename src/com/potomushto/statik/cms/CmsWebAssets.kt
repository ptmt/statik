package com.potomushto.statik.cms

internal object CmsWebAssets {
    fun indexHtml(siteName: String, basePath: String): String {
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>$siteName CMS</title>
              <link rel="stylesheet" href="$basePath/styles.css">
            </head>
            <body>
              <div class="workbench">
                <aside class="rail">
                  <header class="rail-header">
                    <p class="eyebrow">Statik CMS</p>
                    <h1>$siteName</h1>
                  </header>

                  <div id="status-chips" class="status-strip"></div>

                  <section class="tree-section">
                    <div class="tree-section-header">
                      <span>Posts</span>
                      <button id="new-post" class="tree-action" type="button">+ New</button>
                    </div>
                    <div id="post-tree" class="tree-root"></div>
                  </section>

                  <section class="tree-section">
                    <div class="tree-section-header">
                      <span>Pages</span>
                      <button id="new-page" class="tree-action" type="button">+ New</button>
                    </div>
                    <div id="page-tree" class="tree-root"></div>
                  </section>

                  <section class="tree-section">
                    <div class="tree-section-header">
                      <span>Media</span>
                      <div class="tree-actions">
                        <button id="upload-media" class="tree-action" type="button">Upload</button>
                        <button id="rename-media" class="tree-action" type="button" disabled>Rename</button>
                        <button id="delete-media" class="tree-action" type="button" disabled>Delete</button>
                      </div>
                    </div>
                    <div id="media-tree" class="tree-root"></div>
                  </section>
                </aside>

                <main class="editor-pane">
                  <header class="editor-header">
                    <div class="editor-heading">
                      <p class="eyebrow">Editing</p>
                      <h2 id="editor-title">Select a file</h2>
                      <p id="editor-subtitle" class="muted">Choose a post, page, or media item from the left.</p>
                    </div>
                    <div class="header-actions">
                      <a href="/" target="_blank" rel="noreferrer">Preview</a>
                      <button id="refresh-index" type="button">Rescan</button>
                      <button id="logs-button" type="button">Logs</button>
                      <button id="sync-button" type="button">Sync</button>
                    </div>
                  </header>

                  <section class="editor-card meta-card">
                    <input id="content-type" type="hidden">
                    <div class="meta-grid">
                      <label class="compact-field compact-path">
                        <span>Path</span>
                        <input id="source-path" type="text" placeholder="posts/hello-world.md">
                      </label>
                    </div>
                  </section>

                  <section class="editor-card source-card">
                    <div class="editor-toolbar">
                      <span>Source</span>
                      <button id="save-button" class="primary" type="button">Save And Rebuild</button>
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

        .rail {
          padding: 18px 14px;
          background: linear-gradient(180deg, rgba(239, 233, 224, 0.96), rgba(232, 224, 212, 0.96));
          border-right: 1px solid var(--line);
          display: grid;
          grid-template-rows: auto auto;
          grid-auto-rows: minmax(0, 1fr);
          gap: 12px;
          backdrop-filter: blur(16px);
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
          grid-template-rows: auto auto minmax(0, 1fr);
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

        .header-actions {
          display: flex;
          flex-wrap: wrap;
          justify-content: flex-end;
          gap: 8px;
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

        .editor-toolbar .primary {
          border: 0;
          background: var(--accent);
          color: #fff;
          border-radius: 10px;
          padding: 9px 12px;
          cursor: pointer;
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
            grid-template-rows: auto auto;
            grid-auto-rows: minmax(180px, auto);
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

          .source-area {
            min-height: 320px;
          }
        }
    """.trimIndent()

    val appJs: String = """
        (() => {
          const basePath = window.STATIK_CMS_BASE_PATH || "/__statik__/cms";
          const apiBase = basePath + "/api";
          const state = {
            items: [],
            mediaItems: [],
            mediaRoots: [],
            selected: null,
            selectedMediaPath: null,
            selectedMediaKind: null,
            mode: "empty",
            status: null,
            lastSync: null,
            pendingUploadFiles: []
          };

          const elements = {
            postTree: document.getElementById("post-tree"),
            pageTree: document.getElementById("page-tree"),
            mediaTree: document.getElementById("media-tree"),
            status: document.getElementById("status-chips"),
            log: document.getElementById("activity-log"),
            editorTitle: document.getElementById("editor-title"),
            editorSubtitle: document.getElementById("editor-subtitle"),
            type: document.getElementById("content-type"),
            sourcePath: document.getElementById("source-path"),
            source: document.getElementById("source-document"),
            logsButton: document.getElementById("logs-button"),
            logDialog: document.getElementById("log-dialog"),
            closeLogs: document.getElementById("close-logs"),
            save: document.getElementById("save-button"),
            sync: document.getElementById("sync-button"),
            refresh: document.getElementById("refresh-index"),
            newPost: document.getElementById("new-post"),
            newPage: document.getElementById("new-page"),
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
            elements.save.disabled = !editable;
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

          function showEmptyEditor() {
            state.mode = "empty";
            state.selected = null;
            elements.type.value = "";
            elements.sourcePath.value = "";
            elements.source.value = "";
            setEditorEditable(false);
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

            elements.status.innerHTML = chips.join("");
          }

          function groupItems(type) {
            return state.items.filter(item => item.type === type);
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
            state.mode = "media";
            state.selected = null;
            state.selectedMediaPath = sourcePath;
            state.selectedMediaKind = kind;
            elements.type.value = "";
            elements.sourcePath.value = sourcePath;
            elements.source.value = mediaEditorText(sourcePath, kind);
            setEditorEditable(false);
            setEditorHeading(fileNameFromPath(sourcePath), mediaSubtitle(sourcePath, kind));
            renderList();
            renderMediaTree();
            updateMediaActions();
          }

          function buildTree(items, rootLabel) {
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

            return root.children;
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
              const active = state.selected === item.sourcePath ? " active" : "";
              const meta = secondaryMeta(item);
              return '<button class="tree-file' + active + '" type="button" data-source-path="' + escapeHtml(item.sourcePath) + '" style="--depth:' + depth + '">' +
                '<div class="tree-file-main">' +
                  '<span class="tree-file-name">' + escapeHtml(node.name) + "</span>" +
                  '<span class="tree-file-badges">' + renderBadges(item) + "</span>" +
                "</div>" +
                '<div class="tree-file-meta">' + escapeHtml(meta) + "</div>" +
              "</button>";
            }).join("");
          }

          function renderTree(target, items, label) {
            if (!items.length) {
              target.innerHTML = '<p class="tree-empty">No ' + label.toLowerCase() + " yet.</p>";
              return;
            }

            target.innerHTML = renderTreeNodes(buildTree(items, label), 0);
            target.querySelectorAll("[data-source-path]").forEach(node => {
              node.addEventListener("click", () => {
                const sourcePath = node.getAttribute("data-source-path");
                openEntry(sourcePath).catch(error => log(error.message));
              });
            });
          }

          function renderList() {
            renderTree(elements.postTree, groupItems("POST"), "Posts");
            renderTree(elements.pageTree, groupItems("PAGE"), "Pages");
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
              node.addEventListener("click", () => {
                const sourcePath = node.getAttribute("data-media-path");
                const kind = node.getAttribute("data-media-kind");
                selectMedia(sourcePath, kind);
                log("Selected media " + sourcePath + ".");
              });
            });

            updateMediaActions();
          }

          function selectedMediaIsRoot() {
            return state.selectedMediaKind === "folder" && state.mediaRoots.includes(state.selectedMediaPath);
          }

          function updateMediaActions() {
            const hasSelection = !!state.selectedMediaPath;
            const disabled = !hasSelection || selectedMediaIsRoot();
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

          async function loadStatus() {
            const status = await api("/status");
            if (!status) return;
            renderStatus(status);
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

          async function openEntry(sourcePath) {
            const response = await api("/content/item?sourcePath=" + encodeURIComponent(sourcePath));
            if (!response) return;

            state.mode = "content";
            state.selected = response.sourcePath;
            state.selectedMediaPath = null;
            state.selectedMediaKind = null;
            elements.type.value = response.type;
            elements.sourcePath.value = response.sourcePath;
            elements.source.value = serializeDocument(response.frontmatter || "", response.body || "");
            setEditorEditable(true);

            const subtitle = response.isDraft ? "draft · " + response.title : response.title;
            setEditorHeading(fileNameFromPath(response.sourcePath), subtitle);
            renderList();
            renderMediaTree();
            log("Loaded " + response.sourcePath);
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
            state.selectedMediaPath = null;
            state.selectedMediaKind = null;
            elements.type.value = type;
            elements.sourcePath.value = defaultPath(type);
            elements.source.value = serializeDocument(defaultFrontmatter(type), "");
            setEditorEditable(true);
            setEditorHeading(fileNameFromPath(elements.sourcePath.value), "new file");
            renderList();
            renderMediaTree();
            log("Preparing " + elements.sourcePath.value + ".");
          }

          async function save() {
            if (!elements.type.value) {
              throw new Error("Select or create a post or page before saving.");
            }

            const parsed = parseDocument(elements.source.value);
            const payload = {
              type: elements.type.value,
              sourcePath: elements.sourcePath.value,
              frontmatter: parsed.frontmatter,
              body: parsed.body
            };

            const response = await api("/content", {
              method: "POST",
              body: JSON.stringify(payload)
            });
            if (!response) return;

            state.selected = response.item.sourcePath;
            log("Saved " + response.item.sourcePath + " and rebuilt the site.");
            if (response.sync) {
              updateSyncState(response.sync);
              log(response.sync.message + " " + syncSummary(response.sync));
            }

            await Promise.all([loadStatus(), loadList()]);
            await openEntry(response.item.sourcePath);
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
              await Promise.all([loadStatus(), loadList()]);
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
            log("Rescanning content and media from disk.");
            const response = await api("/refresh", { method: "POST" });
            if (!response) return;
            log("Rescan complete: " + response.items + " item(s), " + response.dirty + " dirty.");
            await Promise.all([loadStatus(), loadList(), loadMedia()]);
          }

          document.addEventListener("keydown", event => {
            const saveShortcut = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "s";
            if (!saveShortcut) {
              return;
            }
            event.preventDefault();
            save().catch(error => log(error.message));
          });

          elements.newPost.addEventListener("click", () => startNew("POST"));
          elements.newPage.addEventListener("click", () => startNew("PAGE"));
          elements.logsButton.addEventListener("click", () => openLogs());
          elements.closeLogs.addEventListener("click", () => closeLogs());
          elements.uploadMedia.addEventListener("click", () => openUploadDialog());
          elements.renameMedia.addEventListener("click", () => openRenameDialog());
          elements.deleteMedia.addEventListener("click", () => openDeleteDialog());
          elements.save.addEventListener("click", () => save().catch(error => log(error.message)));
          elements.sync.addEventListener("click", () => openSyncDialog());
          elements.refresh.addEventListener("click", () => refreshIndex().catch(error => log(error.message)));

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

          Promise.all([loadStatus(), loadList(), loadMedia()])
            .then(() => log("CMS ready."))
            .catch(error => log(error.message));
        })();
    """.trimIndent()

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
