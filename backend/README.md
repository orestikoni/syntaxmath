# Backend authentication configuration

Password-based registration requires email verification. Password-reset and verification links are sent through Resend.

Configure these values for the backend:

| Variable | Purpose |
| --- | --- |
| `RESEND_API_KEY` | Resend API key. Keep it in the deployment secret store. |
| `RESEND_FROM_EMAIL` | Sender authorized in Resend, for example `SyntaxMath <no-reply@example.com>`. |
| `FRONTEND_ORIGIN` | Exact frontend origin used for CORS and emailed links, without a trailing slash. |

The frontend also needs `NEXT_PUBLIC_API_URL` set to the backend origin. For local development it defaults to `http://localhost:8080`.

Verification links expire after 24 hours and password-reset links after 30 minutes by default; both values can be configured under `syntaxmath.auth`. Tokens are random, single-use, and only their SHA-256 hashes are stored. Changing a password revokes every active session for that account.

Existing accounts are unverified after the schema migration and must request a verification email once before password sign-in. Google sign-ins with a verified Google email are marked verified.

Public endpoints:

- `POST /api/auth/verification/request`
- `POST /api/auth/verification/confirm`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/password-reset/confirm`
