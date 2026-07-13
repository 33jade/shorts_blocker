# Privacy Policy Publication Plan

Last updated: 2026-07-12

## Current Status

- Source text: `docs/privacy-policy.md`
- In-app text: `app/src/main/res/values/strings.xml`
- GitHub repository: `33jade/shorts_blocker`
- Publication status: repository target corrected, GitHub Pages publication pending
- Public URL: `https://33jade.github.io/shorts_blocker/privacy-policy.html`
- Selected publication option: GitHub Pages
- Developer name: Sashimi Teriyaki
- Privacy contact: sashimi.teriyaki.343@gmail.com

## Values Confirmed Before Publication

These values have been applied to `docs/privacy-policy.md`:

- Developer name: Sashimi Teriyaki
- Privacy contact: sashimi.teriyaki.343@gmail.com

The developer name should match the Google Play developer profile. The contact email should be reachable by users.

## Selected Publication Option

GitHub Pages will be used because the project source will be published on GitHub.

Repository: `33jade/shorts_blocker`

Public privacy policy URL:

```text
https://33jade.github.io/shorts_blocker/privacy-policy.html
```

Use GitHub Pages with:

- Source branch: `main`
- Source folder: `/docs`

## Local Publish Commands

Run these commands after logging in with GitHub CLI:

```powershell
cd C:\Users\816a2\Dev-Projects\YoutubeShortBlocker
gh auth login
git push -u origin main
```

After the repository is created, enable GitHub Pages for the repository:

```powershell
gh api `
  --method POST `
  -H "Accept: application/vnd.github+json" `
  /repos/33jade/shorts_blocker/pages `
  -f "source[branch]=main" `
  -f "source[path]=/docs"
```

Then confirm the Pages URL:

```powershell
gh api /repos/33jade/shorts_blocker/pages --jq .html_url
```

## Alternative Publication Options

1. Static hosting
   - Any HTTPS static host is acceptable.
   - Keep the URL stable after release.

2. Existing personal or project website
   - Good fit if the developer already owns a domain.
   - Suggested path: `/shortblocker/privacy-policy/`

## Publication Checklist

- [x] Replace developer name.
- [x] Replace privacy contact.
- [ ] Enable GitHub Pages over HTTPS for `33jade/shorts_blocker`.
- [ ] Confirm the page is accessible without login.
- [x] Record the public URL in `docs/roadmap.md`.
- [ ] Add the public URL to Google Play Console.
- [ ] Re-check that the published text matches the in-app disclosure.
