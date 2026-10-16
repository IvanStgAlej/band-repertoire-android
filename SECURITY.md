# Security policy

## Scope

Band Repertoire Android is a **demo client** for
[band-repertoire-api](https://github.com/IvanStgAlej/band-repertoire-api), a portfolio project.

- **The API has no authentication.** It is meant to run on your own machine, with the app in the Android
  Emulator. Never expose it to the internet.
- The app stores one thing on the device: the id and name of the ensemble you picked. It is excluded from
  backups and device transfers.
- The app asks for one permission, `INTERNET`.

## What is checked on every push

| Check | How |
|---|---|
| No secrets in the repository or its history | gitleaks over the full history (`secrets` CI job) |
| Release builds refuse plain HTTP | `verifyReleaseSecurity` checks the release network security config. Only debug builds may reach `10.0.2.2`/`localhost` over HTTP |
| Release builds are not debuggable, are shrunk with R8, and do not log HTTP traffic | `verifyReleaseSecurity` + `AppContainerTest` |
| Minimal manifest: only `INTERNET`, no unprotected exported components, no backups | `verifyReleaseSecurity` |
| Android Lint security issues fail the build | `app/lint.xml` + `lintRelease` |
| Errors never show stack traces or raw server responses | `NetworkBandRepositoryTest` |
| Dependencies and GitHub Actions stay up to date | Dependabot |

## Reporting a vulnerability

Please **do not open a public issue**. Use GitHub's
[private vulnerability reporting](https://github.com/IvanStgAlej/band-repertoire-android/security/advisories/new)
for this repository, and include the steps to reproduce. You can expect a first answer within a week.
