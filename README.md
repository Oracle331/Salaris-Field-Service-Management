# Salaris Field Service Management

I built this app to give contractors one place to prepare for a service call. The workspace brings together the job schedule, customer and site details, equipment, service history, and notes. It includes fictional HVAC, plumbing, and appliance jobs.

## Run locally

Install a JDK (Java 17 or newer) and run this from the project root in PowerShell:

```powershell
.\run-local.ps1 -JavaHome $env:JAVA_HOME
```

Set `JAVA_HOME` to your JDK directory first. The Maven wrapper downloads dependencies on the first run. Open http://localhost:8081 and use the password printed by the launcher.

| Demo account | What it can access |
| --- | --- |
| `worker@salaris.demo` | Assigned jobs and their equipment, notes, and checklists |
| `office@salaris.demo` | Scheduling, assignments, and the client directory |
| `client@salaris.demo` | Its own appointments and job status |
| `admin@salaris.demo` | Office features and integration status |

## How it works

- Spring Boot serves the API and the HTML, CSS, and JavaScript on the same origin. Spring Security handles sessions and role-based access.
- Client records can have multiple locations. Equipment records hold manuals, photos, and notes that carry over between visits.
- Staff can assign work and adjust appointment windows. The schedule refreshes while it is open, and the app records changes to job status.
- H2 stores local data under `backend/data/`. The demo accounts share a session password and are intended for fictional data. Keep local data and credentials outside Git.

Set `DEMO_PASSWORD` to use a specific demo password of at least 16 characters. Other optional settings include `APP_TIME_ZONE`, `PORT`, `DATABASE_URL`, and `SESSION_COOKIE_SECURE`. The backend reads environment variables directly. SMTP is disabled by default. To enable it, set `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, and `MAIL_FROM`, plus the credentials and TLS settings your provider requires.

See [location imagery](LOCATION-IMAGERY.md) for the optional Street View setup. [Accounting integrations](ACCOUNTING-INTEGRATIONS.md) and the [inventory API contract](HVAC-INVENTORY-CONTRACT.md) are design notes for planned work.

## Checks

With `JAVA_HOME` set, run `cd backend; .\mvnw.cmd test`. The tests cover role boundaries, client records, uploads, and appointment scheduling. From the project root, use `node --check backend/src/main/resources/static/app.js` and the same command for `workspace.js` to check frontend syntax.
