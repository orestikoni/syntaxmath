# Backend authentication configuration

Password-based registration requires email verification. Password-reset and verification links are sent through Resend.
New and reset passwords are checked against Have I Been Pwned's Pwned Passwords range API. Only the first five characters of the SHA-1 hash are sent (k-anonymity); the password itself is not transmitted. Password creation/reset fails with a temporary service error if this check is unavailable.

Configure these values for the backend:

| Variable | Purpose |
| --- | --- |
| `RESEND_API_KEY` | Resend API key. Keep it in the deployment secret store. |
| `RESEND_FROM_EMAIL` | Sender authorized in Resend, for example `SyntaxMath <no-reply@example.com>`. |
| `FRONTEND_ORIGIN` | Exact frontend origin used for CORS and emailed links, without a trailing slash. |

The frontend also needs `NEXT_PUBLIC_API_URL` set to the backend origin. For local development it defaults to `http://localhost:8080`.

Verification links expire after 24 hours and password-reset links after 30 minutes by default; both values can be configured under `syntaxmath.auth`. Tokens are random, single-use, and only their SHA-256 hashes are stored. Changing a password revokes every active session for that account.

Authenticated users can list active sessions, revoke one session, or revoke all sessions. The session list reports creation/expiry times and identifies the current session; it does not claim to show device or location details because those are not recorded. Expired and revoked session rows are cleaned up daily at 03:00 UTC.

Cookie-authenticated browser requests use CSRF protection. Before sending an unsafe request, the frontend fetches `GET /api/auth/csrf` with credentials and sends the returned token in the `X-XSRF-TOKEN` header. Keep the frontend origin in the exact CORS allowlist; cross-site deployments also need a compatible cookie `SameSite` policy.

## Optional TOTP multi-factor authentication

Signed-in users can manage TOTP at `/account/security`. Enrollment requires scanning/entering the displayed setup key and confirming a current six-digit code. Enabling MFA returns ten single-use recovery codes; save them because they are displayed only once. Password-based login and Google OAuth sign-in then require either an authenticator code or an unused recovery code before a full session is issued. The Google OAuth second step uses a short-lived, HttpOnly challenge cookie; the challenge expires after five minutes and is single-use.

Google identities are persisted by Google's stable subject identifier, with unique constraints preventing one Google identity from being attached to multiple accounts or multiple Google identities from being attached to one account. A verified Google email continues to link to an existing SyntaxMath account with the same normalized email, preserving the current sign-in behavior.

Authentication and security events are persisted in PostgreSQL for 365 days and then removed by the daily cleanup job. The audit table stores a SHA-256 account identifier hash, event type, and non-sensitive event details; it does not store client IP addresses. Existing application log messages still include client IP on login events, so configure the deployment's log collection and retention policy accordingly. A security-sensitive action fails if its persistent audit write fails.

Set `MFA_ENCRYPTION_KEY` to a Base64-encoded random 32-byte key in local/deployment secrets before enabling MFA. The app encrypts TOTP secrets with AES-GCM; changing or losing this key makes enrolled authenticators unusable. Keep a protected backup and use the same key across backend instances. Recovery-code hashes are stored instead of their plaintext values.

Existing accounts are unverified after the schema migration and must request a verification email once before password sign-in. Google sign-ins with a verified Google email are marked verified.

## Spring profiles

With no profile specified, the backend uses the `dev` default profile. For production, explicitly activate the `prod` profile with `SPRING_PROFILES_ACTIVE=prod`. Production requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FRONTEND_ORIGIN`, `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, and `MFA_ENCRYPTION_KEY` to be configured in the deployment environment. The production profile sets the session cookie's `Secure` attribute to `true`; terminate HTTPS at the application or a trusted TLS-terminating proxy.

Public endpoints:

- `POST /api/auth/verification/request`
- `POST /api/auth/verification/confirm`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/password-reset/confirm`
- `GET /api/auth/sessions`
- `DELETE /api/auth/sessions/{sessionId}`
- `POST /api/auth/logout-all`
