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
                </aside>

                <main class="editor-pane">
                  <header class="editor-header">
                    <div class="editor-heading">
                      <p class="eyebrow">Editing</p>
                      <h2 id="editor-title">Select a file</h2>
                      <p id="editor-subtitle" class="muted">Choose a post or page from the left.</p>
                    </div>
                    <div class="header-actions">
                      <a href="/" target="_blank" rel="noreferrer">Open site</a>
                      <button id="refresh-index" type="button">Refresh</button>
                      <button id="sync-button" type="button">Commit Sync</button>
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

                  <section class="editor-card activity-card">
                    <pre id="activity-log">Loading CMS state…</pre>
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

                  <div class="dialog-actions">
                    <button type="submit" value="cancel">Cancel</button>
                    <button type="submit" value="confirm" class="primary">Sync</button>
                  </div>
                </form>
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
          grid-template-rows: auto auto minmax(0, 1fr);
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
          grid-template-rows: auto auto minmax(0, 1fr) auto;
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

        .activity-card pre {
          margin: 0;
          min-height: 72px;
          max-height: 120px;
          overflow: auto;
          white-space: pre-wrap;
          word-break: break-word;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          font-size: 0.83rem;
          color: var(--muted);
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
            grid-template-rows: auto auto auto auto;
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
            selected: null,
            status: null,
            lastSync: null
          };

          const elements = {
            postTree: document.getElementById("post-tree"),
            pageTree: document.getElementById("page-tree"),
            status: document.getElementById("status-chips"),
            log: document.getElementById("activity-log"),
            editorTitle: document.getElementById("editor-title"),
            editorSubtitle: document.getElementById("editor-subtitle"),
            type: document.getElementById("content-type"),
            sourcePath: document.getElementById("source-path"),
            source: document.getElementById("source-document"),
            save: document.getElementById("save-button"),
            sync: document.getElementById("sync-button"),
            refresh: document.getElementById("refresh-index"),
            newPost: document.getElementById("new-post"),
            newPage: document.getElementById("new-page"),
            syncDialog: document.getElementById("sync-dialog"),
            syncCommitMessage: document.getElementById("sync-commit-message")
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

          function renderStatus(status) {
            state.status = status;
            const chips = [
              chip(status.ready ? "ready" : "checkout needed", !status.ready),
              chip(String(status.dirty) + " dirty", status.dirty > 0)
            ];

            if (status.lastSyncedAt) {
              chips.push(chip("synced " + formatClock(status.lastSyncedAt), false));
            }

            const syncChip = syncBadge(state.lastSync);
            if (syncChip) {
              chips.push(chip(syncChip.text, syncChip.warn));
            }

            if (status.git && status.git.branch) {
              chips.push(chip("git " + status.git.branch, false));
            }
            if (status.auth && status.auth.viewer) {
              chips.push(chip("@" + status.auth.viewer, false));
            }
            if (status.repository) {
              chips.push(chip(status.repository, false));
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

            if (state.items.length > 0) {
              const preferred = state.items.find(item => item.type === "POST") || state.items[0];
              await openEntry(preferred.sourcePath);
            } else {
              state.selected = null;
              elements.type.value = "";
              elements.sourcePath.value = "";
              elements.source.value = "";
              setEditorHeading("Select a file", "Choose a post or page from the left.");
            }
          }

          async function openEntry(sourcePath) {
            const response = await api("/content/item?sourcePath=" + encodeURIComponent(sourcePath));
            if (!response) return;

            state.selected = response.sourcePath;
            elements.type.value = response.type;
            elements.sourcePath.value = response.sourcePath;
            elements.source.value = serializeDocument(response.frontmatter || "", response.body || "");

            const subtitle = response.isDraft ? "draft · " + response.title : response.title;
            setEditorHeading(fileNameFromPath(response.sourcePath), subtitle);
            renderList();
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
            state.selected = null;
            elements.type.value = type;
            elements.sourcePath.value = defaultPath(type);
            elements.source.value = serializeDocument(defaultFrontmatter(type), "");
            setEditorHeading(fileNameFromPath(elements.sourcePath.value), "new file");
            renderList();
            log("Preparing " + elements.sourcePath.value + ".");
          }

          async function save() {
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

          async function sync(commitMessage) {
            const response = await api("/sync", {
              method: "POST",
              body: JSON.stringify({
                commitMessage: commitMessage || null,
                push: null
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
          }

          function openSyncDialog() {
            if (!elements.syncDialog || typeof elements.syncDialog.showModal !== "function") {
              const value = window.prompt("Commit message (leave blank for default):", "");
              if (value === null) {
                return;
              }
              sync(value.trim()).catch(error => log(error.message));
              return;
            }

            elements.syncCommitMessage.value = "";
            elements.syncDialog.showModal();
            window.setTimeout(() => {
              elements.syncCommitMessage.focus();
            }, 0);
          }

          async function refreshIndex() {
            const response = await api("/refresh", { method: "POST" });
            if (!response) return;
            log("Refreshed index: " + response.items + " item(s), " + response.dirty + " dirty.");
            await Promise.all([loadStatus(), loadList()]);
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
          elements.save.addEventListener("click", () => save().catch(error => log(error.message)));
          elements.sync.addEventListener("click", () => openSyncDialog());
          elements.refresh.addEventListener("click", () => refreshIndex().catch(error => log(error.message)));

          if (elements.syncDialog) {
            elements.syncDialog.addEventListener("close", () => {
              if (elements.syncDialog.returnValue !== "confirm") {
                return;
              }
              sync(elements.syncCommitMessage.value.trim()).catch(error => log(error.message));
            });
          }

          Promise.all([loadStatus(), loadList()])
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
