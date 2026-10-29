# Salaris Field Service

A working local MVP focused on getting contractors the details they need before arriving at a job. Fictional HVAC, plumbing, and appliance jobs are included.

## Run on Windows

Run `./run-local.ps1` from this directory in Windows PowerShell 5.1 or PowerShell 7. The launcher uses the installed JDK 24 by default; use `-JavaHome` for another JDK 17–25. Maven downloads dependencies on the first run.

Open http://localhost:8081. The launcher prints a newly generated password unless you set `DEMO_PASSWORD` (minimum 16 characters). Accounts:

| Account | Access |
| --- | --- |
| worker@salaris.demo | Assigned jobs, equipment, notes, checklist, work status |
| office@salaris.demo | All jobs, creation, assignment, client directory, appointment emails |
| client@salaris.demo | Their own appointment details and status; no internal notes or access instructions |
| admin@salaris.demo | Office access plus integration status |

The Windows Maven profile uses a short relative temporary socket path to avoid Java's Windows Unix-domain socket path limit in long or sandboxed temporary directories.

This version has four demo accounts, supplied by Spring Security and BCrypt hashed in memory. It does not yet have user provisioning, password recovery, multiple workers, or multiple contractor organizations. A shared demo password is for fictional-data demos only. Changing the launcher password changes all four demo accounts on restart. Other sample clients have no login account.

## What's working

- Responsive job workspace, job search, status filters, schedule, and client directory.
- Independent Client lookup for Office/Admin and assigned Workers: client contacts, multiple service locations, equipment, and notes can be added without a job. Office/Admin can create and edit clients. Workers only see clients tied to their assigned jobs.
- Equipment and location photos: upload JPG/PNG/WebP in the browser up to 25 MB, with an optional caption. Large images are resized before the server accepts a JPG/PNG up to 5 MB and 20 megapixels. Pictures are validated, decoded/re-encoded to remove metadata, stored in the local database, and served only after authorization. Photos are internal to the service team. Up to 50 photos per equipment item or location; deletion and direct HEIC support are not yet included.
- Job Photos tab and persistent client equipment library. Creating a job from a client record reuses the selected location and equipment. Job contact/specification snapshots remain historical; the client library is available separately on existing jobs.
- Live schedule refreshes every 30 seconds while visible, using `APP_TIME_ZONE` (America/New_York by default). Active travel/work stays first; then the oldest missed end-of-window deadline, due-now work, due-within-one-hour work, future visits, and completed jobs. Equal deadlines use urgency and then creation time. Appointment times are never automatically moved. Office/Admin can change the window from the job timeline.
- Existing jobs are linked to persistent clients and locations at startup. Old creation times were not recorded, so imported job ages are explicitly marked as estimates from their appointment. New jobs store actual creation and completion times.
- Backend authorization scopes workers to assignments and clients to their own records. Client responses redact internal work data.
- Job creation, worker assignment/unassignment, equipment capture, notes, checklist completion, and status updates persist in a local H2 database under `backend/data/`.
- Optimistic versions prevent overwriting stale job changes. Job notes and status/assignment history are captured per job.
- Directions launch Google Maps. Email draft links open a local mail app. SMTP appointment sending is implemented and disabled by default.
- Same-origin frontend served by Spring Boot. Session authentication, BCrypt cost 12, CSRF on every mutation including login/logout, HttpOnly SameSite cookies, and a restrictive content security policy. No access tokens stored in browser storage.

## Cloudflare demo links

Keep the app running locally. Install Cloudflare's `cloudflared` tool, then in a second terminal run:

```powershell
cloudflared tunnel --url http://127.0.0.1:8081
```

The tool prints a temporary HTTPS `trycloudflare.com` link. This repository has not created a public tunnel. Share the fictional-data demo link and its password only with intended reviewers. Quick Tunnel addresses change each run. The application binds to loopback by default, so the tunnel can reach it without exposing the port to the local network.

Cloudflare reference: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/

For HTTPS-only demos, set `SESSION_COOKIE_SECURE=true` before starting the app and use the HTTPS tunnel URL to sign in. Secure cookies will not work over ordinary local HTTP. Forwarded headers are deliberately ignored; no blanket trust of proxy headers is enabled.

## Integrations

Environment variables are read directly; no `.env` file is loaded automatically. Keep credentials out of source control.

| Variable | Purpose / default |
| --- | --- |
| DEMO_PASSWORD | Required by the backend; launcher generates it if missing |
| DEMO_ENABLED | Seed fictional jobs and apply one-time demo additions; default true |
| PORT / SERVER_ADDRESS | 8081 / 127.0.0.1 |
| APP_TIME_ZONE | America/New_York; used for appointment windows and schedule display |
| DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD | Local H2 defaults; PostgreSQL driver included for the next deployment stage |
| SESSION_COOKIE_SECURE | false locally; true for HTTPS-only access |
| GOOGLE_MAPS_API_KEY | Reserved server-side configuration; advanced maps functionality not implemented |
| MAIL_ENABLED | false; explicitly enable only after SMTP setup |
| MAIL_HOST / MAIL_PORT | SMTP host and port; localhost / 1025 defaults |
| MAIL_USERNAME / MAIL_PASSWORD | SMTP credentials |
| MAIL_AUTH / MAIL_STARTTLS | false defaults for local mail capture; set per SMTP provider |
| MAIL_FROM | Required sender for appointment email |

No Google OAuth credentials or tokens are copied from Champion. Gmail OAuth linking, inbound email, delivery retries, and transactional email outbox remain future work. SMTP sending is synchronous; clients can receive an appointment template only, not arbitrary content or recipients.

## Architecture and next steps

Uses Java 17 language level, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Spring Mail, and H2. The interface uses plain HTML/CSS/JavaScript, with no frontend dependency bundle or external font/asset services. This keeps the initial hosting footprint small, without implying a measured cost advantage yet.

Champion reference reviewed read-only: backend pom, security configuration, backend README, and local launcher. Reused its Spring Boot 3.5 / Java 17 approach, session security model, BCrypt, CSRF, local database path, and server-side email integration pattern. Maven wrapper files were copied; no Champion data, environment files, keys, or customer records were copied.

Before real customer use: durable individual user accounts and onboarding; contractor tenant boundaries; schema migrations (current local MVP uses Hibernate update); append-only audit logs; backups; login throttling/MFA; equipment/location editing and photo lifecycle controls; conflict-aware crew scheduling; monitoring and deployment hardening. PostgreSQL is a prepared driver/configuration path, not a verified production deployment. No billing, quoting, payroll, payments, or offline synchronization yet. See [the accounting integration proposal](ACCOUNTING-INTEGRATIONS.md) for a staged invoicing plan.

Back up the database before upgrading. Stop the old local process and run the launcher again, then refresh the browser to load these changes. Existing data is retained; startup links existing jobs to client records. A versioned, one-time demo upgrade adds fictional commercial records, prior visits, manufacturer images, and documentation to existing demo databases without replacing existing jobs. Set DEMO_ENABLED=false to disable demo additions. Photos currently live in H2 alongside records, so include the database in backups. A separate private object store is a future scaling option.

## Validation

From `backend/`, run `./mvnw.cmd test` with JAVA_HOME set. Access tests cover unauthenticated requests, real session login with CSRF, worker/client record boundaries, client redaction, write restrictions, disabled email, persistence, and stale-write rejection.

Frontend syntax check: `node --check backend/src/main/resources/static/app.js`.

Additional frontend syntax check: `node --check backend/src/main/resources/static/workspace.js`. The expanded suite has 25 tests, including independent client creation/editing, assignment-scoped records, equipment/location uploads, photo access restrictions, cross-client target rejection, idempotent record linking, appointment permissions, queue ordering, and clock-driven stage changes. Browser checks verified client creation without a job, saved notes, an equipment upload, and the live schedule refresh. Accounting integrations remain a proposal only.


## Customer-focused workspace

Staff now land in **Clients & locations**. Clients can be residential customers or companies with several service locations. Each location shows its own access notes, inline equipment photos, documentation, and visits. Workers can add equipment photos, documentation links, client notes, and tools used on assigned work.

Photo uploads accept JPG, PNG, and WebP in the browser (up to 25 MB). Large photos and WebP are converted locally to a JPEG up to 2560 pixels on the long edge. HEIC/HEIF must be exported as JPG first. The server accepts decoded JPG/PNG up to 5 MB and 20 megapixels, re-encodes images, and keeps them authenticated. Image processing now uses memory-backed streams instead of the OS image-cache directory. Specific validation messages replace the generic form error.

Tool plans prefer documented guidance when the exact stored model and job title keyword match. Next they reuse recorded tools from a completed visit for the same asset, trade, and job title. Otherwise they show a clearly marked trade-based estimate. Workers record actual tools against the job and equipment, so later visits can reuse that knowledge. Manufacturer tool entries require a document URL, model reference, job keyword, and source page/section. This is a curated reference workflow, not automatic PDF extraction or a complete manufacturer manual catalog. Model revisions and work scope must still be checked.

The demo includes three residential clients and Hudson Workspace Group with two office locations, five sourced equipment illustrations, and completed residential/commercial history. Existing demo data receives the additions once; it is not reset on restart.

Optional free Google Street View embedding, setup steps, provider comparison, and image credits are in [LOCATION-IMAGERY.md](LOCATION-IMAGERY.md). Set GOOGLE_MAPS_EMBED_KEY only after restricting it to Maps Embed API and your allowed website origins. The viewer needs saved location coordinates. No paid image API is enabled.
