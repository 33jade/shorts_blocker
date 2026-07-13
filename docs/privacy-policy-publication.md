# Privacy Policy Publication Plan

Last updated: 2026-07-12

## Current Status

- Source text: `docs/privacy-policy.md`
- In-app text: `app/src/main/res/values/strings.xml`
- GitHub repository: configured in the local Git remote
- Publication status: published by GitHub Pages
- Public URL: configured in Google Play Console
- Selected publication option: GitHub Pages
- Developer name: configured in Google Play Console
- Privacy contact: configured in Google Play Console

## Values Confirmed Before Publication

These values are managed outside this repository:

- Developer name
- Privacy contact

The developer name should match the Google Play developer profile. The privacy contact should be reachable by users.

## Selected Publication Option

GitHub Pages will be used because the project source will be published on GitHub.

Repository: configured in the local Git remote

Public privacy policy URL:

```text
Configure the GitHub Pages privacy policy URL in Google Play Console.
```

Use GitHub Pages with:

- Source branch: `main`
- Source folder: `/docs`

## Local Publish Commands

Run these commands after logging in with GitHub CLI:

```powershell
cd <repo-root>\ShortBlocker
gh auth login
git push -u origin main
```

After the repository is created, enable GitHub Pages for the repository:

```powershell
gh api `
  --method POST `
  -H "Accept: application/vnd.github+json" `
  /repos/<owner>/<repo>/pages `
  -f "source[branch]=main" `
  -f "source[path]=/docs"
```

Then confirm the Pages URL:

```powershell
gh api /repos/<owner>/<repo>/pages --jq .html_url
```

## Alternative Publication Options

1. Static hosting
   - Any HTTPS static host is acceptable.
   - Keep the URL stable after release.

2. Existing personal or project website
   - Good fit if the developer already owns a domain.
   - Suggested path: `/shortblocker/privacy-policy/`

## Publication Checklist

- [x] Keep developer name out of repository docs.
- [x] Keep privacy contact out of repository docs.
- [x] Enable GitHub Pages over HTTPS.
- [x] Confirm the page is accessible without login.
- [x] Record the public URL in `docs/roadmap.md`.
- [ ] Add the public URL to Google Play Console.
- [ ] Re-check that the published text matches the in-app disclosure.
