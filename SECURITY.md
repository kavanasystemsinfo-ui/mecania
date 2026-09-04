# Security Policy

## Reporting a Vulnerability

Please do **not** disclose security vulnerabilities publicly until they have been addressed by the maintainers.

To report a security issue, please:
1. Open a **private issue** in this repository (or contact the maintainer via the agreed channel).
2. Include: description, steps to reproduce, impact, and any relevant logs or screenshots.
3. We will acknowledge receipt within 48 hours and work with you to understand and resolve the issue.

## Supported Versions

Only the latest release of Mecania receives security updates.

## Policy on Secrets

- Never commit `.env` files, tokens, or keys. The `.gitignore` excludes `*.env` except `.env.example`.
- In development, use local environment variables or a `.env.local` file (gitignored).
- In production, rely on platform-provided secrets (Render/Vercel environment variables, Docker secrets, or Kubernetes secrets).
- Pre-commit hooks (if added) will block commits containing potential secrets.

## Default Configuration (MVP)

For the MVP phase, Mecania runs with **open security** (`permitAll()`) to facilitate rapid iteration and demos. This decision is documented in ADR 002 and is **not suitable for production** without enabling proper authentication.

When authentication is enabled (future work), endpoints will be protected by JWT or session-based auth, and only specific routes (e.g., health checks, public documentation) will remain open.

## Dependencies

We use Maven Dependabot (via GitHub) to keep dependencies up-to-date. Critical CVEs are prioritized.