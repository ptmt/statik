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
              <div class="shell">
                <aside class="sidebar">
                  <div class="brand">
                    <p class="eyebrow">Statik CMS</p>
                    <h1>$siteName</h1>
                    <p class="muted">SQLite-backed editor with git sync.</p>
                  </div>
                  <div class="toolbar">
                    <button id="new-post" class="primary" type="button">New Post</button>
                    <button id="new-page" type="button">New Page</button>
                  </div>
                  <div class="filters">
                    <button data-filter="ALL" class="filter active" type="button">All</button>
                    <button data-filter="POST" class="filter" type="button">Posts</button>
                    <button data-filter="PAGE" class="filter" type="button">Pages</button>
                  </div>
                  <div id="content-list" class="content-list"></div>
                </aside>

                <main class="editor">
                  <header class="topbar">
                    <div>
                      <p class="eyebrow">Status</p>
                      <div id="status-chips" class="status-chips"></div>
                    </div>
                    <div class="actions">
                      <button id="refresh-index" type="button">Refresh Index</button>
                      <button id="sync-button" class="primary" type="button">Commit Sync</button>
                      <button id="logout-button" type="button">Logout</button>
                    </div>
                  </header>

                  <section class="editor-card">
                    <div class="field-grid">
                      <label>
                        <span>Type</span>
                        <select id="content-type">
                          <option value="POST">Post</option>
                          <option value="PAGE">Page</option>
                        </select>
                      </label>
                      <label>
                        <span>Source Path</span>
                        <input id="source-path" type="text" placeholder="posts/hello-world.md">
                      </label>
                    </div>

                    <label>
                      <span>Frontmatter</span>
                      <textarea id="frontmatter" spellcheck="false" class="meta-area"></textarea>
                    </label>

                    <label>
                      <span>Body</span>
                      <textarea id="body" spellcheck="false" class="body-area"></textarea>
                    </label>

                    <div class="field-grid">
                      <label>
                        <span>Commit Message</span>
                        <input id="commit-message" type="text" placeholder="cms: update post">
                      </label>
                      <label class="checkbox-row">
                        <input id="sync-on-save" type="checkbox">
                        <span>Commit right after save</span>
                      </label>
                    </div>

                    <div class="footer-actions">
                      <button id="save-button" class="primary" type="button">Save And Rebuild</button>
                      <a href="/" target="_blank" rel="noreferrer">Open site</a>
                    </div>
                  </section>

                  <section class="activity-card">
                    <p class="eyebrow">Activity</p>
                    <pre id="activity-log">Loading CMS state…</pre>
                  </section>
                </main>
              </div>

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
          --bg: #f5f0e6;
          --panel: rgba(255, 251, 245, 0.92);
          --panel-strong: #fff9f1;
          --line: rgba(101, 77, 48, 0.22);
          --text: #2f2418;
          --muted: #7c6a57;
          --accent: #d46a2e;
          --accent-dark: #9e4312;
          --green: #3d7f57;
          --shadow: 0 18px 50px rgba(84, 54, 27, 0.12);
        }

        * {
          box-sizing: border-box;
        }

        body {
          margin: 0;
          min-height: 100vh;
          font-family: "IBM Plex Sans", "Avenir Next", sans-serif;
          color: var(--text);
          background:
            radial-gradient(circle at top left, rgba(212, 106, 46, 0.18), transparent 26%),
            radial-gradient(circle at bottom right, rgba(61, 127, 87, 0.16), transparent 24%),
            linear-gradient(135deg, #f7f1e7, #efe2cf);
        }

        button,
        input,
        textarea,
        select {
          font: inherit;
          color: inherit;
        }

        .shell {
          display: grid;
          grid-template-columns: 360px 1fr;
          min-height: 100vh;
        }

        .sidebar {
          padding: 28px 20px;
          border-right: 1px solid var(--line);
          background: rgba(255, 248, 238, 0.8);
          backdrop-filter: blur(18px);
        }

        .editor {
          padding: 28px;
          display: grid;
          grid-template-rows: auto 1fr auto;
          gap: 18px;
        }

        .brand h1 {
          margin: 0;
          font-size: 2rem;
          line-height: 1;
        }

        .eyebrow {
          margin: 0 0 8px;
          font-size: 0.78rem;
          letter-spacing: 0.14em;
          text-transform: uppercase;
          color: var(--accent-dark);
        }

        .muted {
          color: var(--muted);
          line-height: 1.5;
        }

        .toolbar,
        .filters,
        .actions,
        .footer-actions,
        .status-chips {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
        }

        .toolbar,
        .filters {
          margin-top: 20px;
        }

        button,
        .filter {
          border: 1px solid var(--line);
          background: var(--panel-strong);
          border-radius: 999px;
          padding: 10px 14px;
          cursor: pointer;
          transition: transform 120ms ease, border-color 120ms ease, background 120ms ease;
        }

        button:hover,
        .filter:hover {
          transform: translateY(-1px);
          border-color: rgba(212, 106, 46, 0.4);
        }

        button.primary,
        .filter.active {
          background: linear-gradient(135deg, var(--accent), #e39237);
          color: white;
          border-color: transparent;
        }

        .content-list,
        .editor-card,
        .activity-card {
          background: var(--panel);
          border: 1px solid var(--line);
          border-radius: 28px;
          box-shadow: var(--shadow);
        }

        .content-list {
          margin-top: 20px;
          overflow: auto;
          max-height: calc(100vh - 260px);
        }

        .content-item {
          width: 100%;
          padding: 16px 18px;
          border: 0;
          border-bottom: 1px solid rgba(101, 77, 48, 0.12);
          background: transparent;
          text-align: left;
          border-radius: 0;
        }

        .content-item:last-child {
          border-bottom: 0;
        }

        .content-item.active {
          background: rgba(212, 106, 46, 0.1);
        }

        .content-item strong {
          display: block;
          margin-bottom: 6px;
          font-size: 0.98rem;
        }

        .content-meta {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
          color: var(--muted);
          font-size: 0.86rem;
        }

        .topbar,
        .editor-card,
        .activity-card {
          padding: 22px;
        }

        .topbar {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: 16px;
        }

        .chip {
          padding: 8px 12px;
          border-radius: 999px;
          background: rgba(61, 127, 87, 0.12);
          color: #27543a;
          font-size: 0.88rem;
        }

        .chip.warn {
          background: rgba(212, 106, 46, 0.12);
          color: var(--accent-dark);
        }

        .field-grid {
          display: grid;
          grid-template-columns: 220px 1fr;
          gap: 14px;
        }

        label {
          display: grid;
          gap: 8px;
          margin-bottom: 16px;
          font-size: 0.92rem;
        }

        input,
        select,
        textarea {
          width: 100%;
          border: 1px solid var(--line);
          border-radius: 18px;
          background: rgba(255, 255, 255, 0.72);
          padding: 12px 14px;
        }

        textarea {
          resize: vertical;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          font-size: 0.92rem;
          line-height: 1.5;
        }

        .meta-area {
          min-height: 180px;
        }

        .body-area {
          min-height: 420px;
        }

        .checkbox-row {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 28px;
        }

        .checkbox-row input {
          width: auto;
        }

        .activity-card pre {
          margin: 0;
          white-space: pre-wrap;
          word-break: break-word;
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          color: var(--muted);
        }

        .login-shell {
          min-height: 100vh;
          display: grid;
          place-items: center;
          padding: 24px;
        }

        .login-card {
          max-width: 460px;
          padding: 36px;
          border-radius: 30px;
          background: var(--panel);
          border: 1px solid var(--line);
          box-shadow: var(--shadow);
        }

        .login-button {
          display: inline-flex;
          margin-top: 18px;
          padding: 12px 18px;
          border-radius: 999px;
          background: linear-gradient(135deg, var(--accent), #e39237);
          color: white;
        }

        a {
          color: var(--accent-dark);
          text-decoration: none;
        }

        @media (max-width: 980px) {
          .shell {
            grid-template-columns: 1fr;
          }

          .sidebar {
            border-right: 0;
            border-bottom: 1px solid var(--line);
          }

          .field-grid {
            grid-template-columns: 1fr;
          }

          .content-list {
            max-height: 280px;
          }

          .topbar {
            flex-direction: column;
          }
        }
    """.trimIndent()

    val appJs: String = """
        (() => {
          const basePath = window.STATIK_CMS_BASE_PATH || "/__statik__/cms";
          const apiBase = `${'$'}{basePath}/api`;
          const state = { items: [], filter: "ALL", selected: null };

          const elements = {
            list: document.getElementById("content-list"),
            status: document.getElementById("status-chips"),
            log: document.getElementById("activity-log"),
            type: document.getElementById("content-type"),
            sourcePath: document.getElementById("source-path"),
            frontmatter: document.getElementById("frontmatter"),
            body: document.getElementById("body"),
            commitMessage: document.getElementById("commit-message"),
            syncOnSave: document.getElementById("sync-on-save"),
            save: document.getElementById("save-button"),
            sync: document.getElementById("sync-button"),
            logout: document.getElementById("logout-button"),
            refresh: document.getElementById("refresh-index"),
            newPost: document.getElementById("new-post"),
            newPage: document.getElementById("new-page")
          };

          function log(message) {
            const stamp = new Date().toLocaleTimeString();
            elements.log.textContent = `[${'$'}{stamp}] ${'$'}{message}\n` + elements.log.textContent;
          }

          async function api(path, options = {}) {
            const response = await fetch(`${'$'}{apiBase}${'$'}{path}`, {
              headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
              },
              ...options
            });

            if (!response.ok) {
              if (response.status === 401) {
                window.location.href = `${'$'}{basePath}/login`;
                return null;
              }
              const text = await response.text();
              throw new Error(text || `Request failed with ${'$'}{response.status}`);
            }

            const contentType = response.headers.get("content-type") || "";
            if (contentType.includes("application/json")) {
              return response.json();
            }
            return null;
          }

          function renderStatus(status) {
            const chips = [
              chip(status.ready ? "checkout ready" : "checkout required", !status.ready),
              chip(`${'$'}{status.items} indexed`, false),
              chip(`${'$'}{status.dirty} dirty`, status.dirty > 0),
              chip(status.git.available ? `git ${'$'}{status.git.branch || "detached"}` : "git unavailable", !status.git.available),
              chip(status.git.remoteUrl || "no remote configured", false)
            ];
            if (status.repository) {
              chips.splice(1, 0, chip(status.repository, false));
            }
            if (status.auth?.enabled) {
              chips.unshift(chip(`viewer ${'$'}{status.auth.viewer || "signed out"}`, !status.auth.authenticated));
              chips.push(chip(`allowed ${'$'}{status.auth.allowedUser}`, false));
            }
            elements.status.innerHTML = chips.join("");
          }

          function chip(text, warn) {
            return `<span class="chip ${'$'}{warn ? "warn" : ""}">${'$'}{escapeHtml(text)}</span>`;
          }

          function renderList() {
            const visible = state.items.filter(item => state.filter === "ALL" || item.type === state.filter);
            if (visible.length === 0) {
              elements.list.innerHTML = `<div class="content-item"><strong>No content</strong><div class="content-meta">Create a new entry or refresh the index.</div></div>`;
              return;
            }

            elements.list.innerHTML = visible.map(item => {
              const active = state.selected === item.sourcePath ? "active" : "";
              const dirtyLabel = item.dirty ? "dirty" : "clean";
              return `
                <button class="content-item ${'$'}{active}" data-source-path="${'$'}{escapeHtml(item.sourcePath)}" type="button">
                  <strong>${'$'}{escapeHtml(item.title)}</strong>
                  <div class="content-meta">
                    <span>${'$'}{item.type}</span>
                    <span>${'$'}{escapeHtml(item.sourcePath)}</span>
                    <span>${'$'}{dirtyLabel}</span>
                  </div>
                </button>
              `;
            }).join("");

            elements.list.querySelectorAll("[data-source-path]").forEach(node => {
              node.addEventListener("click", () => openEntry(node.getAttribute("data-source-path")));
            });
          }

          function escapeHtml(value) {
            return (value || "")
              .replaceAll("&", "&amp;")
              .replaceAll("<", "&lt;")
              .replaceAll(">", "&gt;")
              .replaceAll('"', "&quot;");
          }

          async function loadStatus() {
            const status = await api("/status");
            if (!status) return;
            renderStatus(status);
          }

          async function loadList() {
            const typeQuery = state.filter === "ALL" ? "" : `?type=${'$'}{encodeURIComponent(state.filter)}`;
            const response = await api(`/content${'$'}{typeQuery}`);
            if (!response) return;
            state.items = response.items;
            renderList();

            if (!state.selected && state.items.length > 0) {
              await openEntry(state.items[0].sourcePath);
            }
          }

          async function openEntry(sourcePath) {
            const response = await api(`/content/item?sourcePath=${'$'}{encodeURIComponent(sourcePath)}`);
            if (!response) return;
            state.selected = response.sourcePath;
            elements.type.value = response.type;
            elements.sourcePath.value = response.sourcePath;
            elements.frontmatter.value = response.frontmatter || "";
            elements.body.value = response.body || "";
            renderList();
            log(`Loaded ${'$'}{response.sourcePath}`);
          }

          function startNew(type) {
            state.selected = null;
            elements.type.value = type;
            elements.frontmatter.value = type === "POST" ? "title: Untitled\npublished: " + new Date().toISOString().slice(0, 19) : "title: Untitled";
            elements.body.value = "";
            elements.sourcePath.value = type === "POST" ? "posts/untitled.md" : "pages/untitled.md";
            renderList();
            log(`Preparing a new ${'$'}{type.toLowerCase()} draft`);
          }

          async function save() {
            const payload = {
              type: elements.type.value,
              sourcePath: elements.sourcePath.value,
              frontmatter: elements.frontmatter.value,
              body: elements.body.value,
              sync: elements.syncOnSave.checked,
              commitMessage: elements.commitMessage.value || null
            };

            const response = await api("/content", {
              method: "POST",
              body: JSON.stringify(payload)
            });
            if (!response) return;

            state.selected = response.item.sourcePath;
            log(`Saved ${'$'}{response.item.sourcePath} and rebuilt the site.`);

            if (response.sync) {
              log(response.sync.message);
            }

            await Promise.all([loadStatus(), loadList()]);
          }

          async function sync() {
            const response = await api("/sync", {
              method: "POST",
              body: JSON.stringify({
                commitMessage: elements.commitMessage.value || null,
                push: null
              })
            });
            if (!response) return;
            log(response.message + (response.commitId ? ` Commit ${'$'}{response.commitId.slice(0, 7)}.` : ""));
            await Promise.all([loadStatus(), loadList()]);
          }

          async function refreshIndex() {
            const response = await api("/refresh", { method: "POST" });
            if (!response) return;
            log(`Refreshed index: ${'$'}{response.items} item(s), ${'$'}{response.dirty} dirty.`);
            state.selected = null;
            await Promise.all([loadStatus(), loadList()]);
          }

          async function logout() {
            await fetch(`${'$'}{basePath}/logout`, { method: "POST" });
            window.location.href = `${'$'}{basePath}/login`;
          }

          document.querySelectorAll("[data-filter]").forEach(button => {
            button.addEventListener("click", async () => {
              document.querySelectorAll("[data-filter]").forEach(node => node.classList.remove("active"));
              button.classList.add("active");
              state.filter = button.getAttribute("data-filter");
              state.selected = null;
              await loadList();
            });
          });

          elements.newPost.addEventListener("click", () => startNew("POST"));
          elements.newPage.addEventListener("click", () => startNew("PAGE"));
          elements.save.addEventListener("click", () => save().catch(error => log(error.message)));
          elements.sync.addEventListener("click", () => sync().catch(error => log(error.message)));
          elements.refresh.addEventListener("click", () => refreshIndex().catch(error => log(error.message)));
          elements.logout.addEventListener("click", () => logout().catch(error => log(error.message)));

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
