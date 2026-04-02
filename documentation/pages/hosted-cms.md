---
title: Hosted CMS
nav_order: 5
description: "Run Statik as a single-user hosted CMS with GitHub sign-in, managed repo checkout, and git sync."
---

Statik can run as a long-lived CMS server, not just as a one-shot static-site builder. In hosted mode it serves the generated site, exposes the embedded CMS UI, signs one GitHub user in, checks that a GitHub App is installed on one configured repository, clones that repository into a managed checkout, and commits content changes back to GitHub.

This mode is designed for the deployment shape you asked for:

- one configured repository
- one allowed GitHub user
- a container running somewhere continuously
- a local SQLite database for indexing and dirty-state tracking
- git-based sync back to the source repository

## How the Hosted Flow Works

1. Statik starts from a host directory that contains `config.json`, the CMS SQLite file, and the GitHub App private key.
2. The browser opens the CMS UI at `/<cms.basePath>`.
3. The user signs in with GitHub.
4. Statik checks `cms.auth.allowedUser`.
5. If the login does not match, it returns `permissions denied` immediately.
6. If the GitHub App is not installed on the configured repository, Statik shows the install screen.
7. After installation, Statik creates or refreshes the managed checkout in `cms.repo.checkoutDir`.
8. The editor writes to that checkout, rebuilds the site, and can commit and push edited content files.

The important boundary is this:

- GitHub sign-in decides **who may use the CMS**
- `cms.repo.*` decides **which repo may be edited**
- the GitHub App installation token decides **whether Statik may clone, pull, and push that repo**

## Required GitHub Setup

Statik’s hosted CMS needs a GitHub App plus one allowed GitHub user.

### GitHub App

Create a GitHub App and configure these values:

- **App ID**: goes to `cms.auth.appId`
- **Client ID**: goes to `cms.auth.clientId`
- **Client secret**: store it in an environment variable such as `GITHUB_CLIENT_SECRET`
- **App slug**: goes to `cms.auth.appSlug`
- **Private key PEM**: store it on disk and point `cms.auth.privateKeyPath` at it

Configure these URLs in the GitHub App settings:

- **Callback URL**: `https://your-host.example/__statik__/cms/auth/github/callback`
- **Setup URL**: `https://your-host.example/__statik__/cms/auth/github/setup`

Install the app on the single repository you want Statik to manage. Use GitHub’s “Only select repositories” mode and select just that repository.

### Permissions

The hosted CMS needs enough repository access to clone, pull, and push edited content. In practice the GitHub App should have:

- repository metadata: read-only
- repository contents: read and write

If you keep install scope to one repository, the app remains tightly scoped even though it has write access there.

## Example `config.json`

```json
{
  "siteName": "My Blog",
  "baseUrl": "https://blog.example.com",
  "description": "Hosted with Statik CMS",
  "author": "Your Name",
  "cms": {
    "enabled": true,
    "basePath": "/__statik__/cms",
    "databasePath": ".statik/cms.db",
    "autoSyncOnSave": false,
    "git": {
      "enabled": true,
      "remote": "origin",
      "branch": "main",
      "pushOnSync": false,
      "authorName": "Statik CMS",
      "authorEmail": "cms@example.com"
    },
    "auth": {
      "enabled": true,
      "allowedUser": "potomushto",
      "clientId": "Iv1.1234567890abcdef",
      "clientSecretEnv": "GITHUB_CLIENT_SECRET",
      "callbackUrl": "https://cms.example.com/__statik__/cms/auth/github/callback",
      "appId": "123456",
      "appSlug": "statik-cms",
      "privateKeyPath": "keys/statik-cms.private-key.pem",
      "setupUrl": "https://cms.example.com/__statik__/cms/auth/github/setup",
      "scopes": ["repo", "read:user"],
      "sessionTtlDays": 30
    },
    "repo": {
      "enabled": true,
      "owner": "potomushto",
      "name": "my-blog",
      "branch": "main",
      "checkoutDir": ".statik/checkout"
    }
  }
}
```

Key points:

- `cms.auth.allowedUser` is a hard single-user allowlist.
- `cms.repo.owner` and `cms.repo.name` pin the CMS to one repository.
- `cms.repo.checkoutDir` is the managed clone path on disk.
- `cms.databasePath` is resolved relative to the host config directory, not the git checkout.
- `cms.databasePath` must live on persistent storage if you want auth sessions to survive container restarts or deploys.

## Running the Container

The published Statik image can act as both:

- a builder
- a long-running CMS server

For hosted CMS, mount a persistent host directory that contains your Statik `config.json` and the GitHub App private key:

```bash
docker run --rm \
  -p 3000:3000 \
  -e GITHUB_CLIENT_SECRET=your-client-secret \
  -v /srv/statik-cms:/github/workspace \
  ghcr.io/ptmt/statik:latest \
  run -- --root-path /github/workspace --cms
```

After startup:

- the generated site is served on port `3000`
- the CMS UI is available at `http://localhost:3000/__statik__/cms`
- the managed checkout is created inside `/srv/statik-cms/.statik/checkout` unless you override `cms.repo.checkoutDir`

## First Login Experience

When the allowed user signs in for the first time:

1. the CMS verifies the GitHub login
2. if the login is not the configured user, the request ends with `permissions denied`
3. if the login is valid but the GitHub App is not installed on the configured repo, Statik shows an install page
4. after the install completes, Statik clones the repository and opens the editor

This means the container can start “empty” as long as it has:

- a valid Statik config
- the GitHub App private key
- the client secret environment variable

It does **not** require a repository checkout to be mounted into the container ahead of time.

## Editing and Sync

Inside the CMS:

- `Save And Rebuild` writes the source file into the managed checkout and regenerates the affected output
- `Refresh Index` rescans the repo into SQLite
- `Commit Sync` commits the edited content files

In hosted managed-checkout mode, pushes use a GitHub App installation token for the configured repository. The generated site output is not committed; only CMS-managed source content is.

## Operational Notes

- This mode is intentionally single-user.
- The SQLite database is local state, not a collaboration database.
- The allowlist is strict: any GitHub login other than `cms.auth.allowedUser` is rejected immediately.
- If the checkout already exists when the container starts, Statik will reuse it.
- If the checkout does not exist yet, Statik can still boot and wait for the install flow.

For the field-by-field config reference, continue to [config.json reference](./configuration.md).
