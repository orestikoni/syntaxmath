"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authApi, authFetch } from "@/lib/auth-api";

type Enrollment = { secret: string; otpauthUri: string };

export function MfaSettings() {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [code, setCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    fetch(authApi("/mfa/status"), { credentials: "include", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(response.status === 401 ? "Sign in to manage multi-factor authentication." : "Unable to load MFA settings.");
        }
        return (await response.json()) as { enabled: boolean };
      })
      .then((result) => {
        if (active) setEnabled(result.enabled);
      })
      .catch((error: unknown) => {
        if (active) setMessage(error instanceof Error ? error.message : "Unable to load MFA settings.");
      });
    return () => {
      active = false;
    };
  }, []);

  async function startEnrollment() {
    setBusy(true);
    setMessage(null);
    try {
      const response = await authFetch("/mfa/enroll", { method: "POST" });
      const body = (await response.json()) as Enrollment | { message?: string };
      if (!response.ok || !("secret" in body)) {
        setMessage("message" in body ? body.message ?? "Unable to start MFA enrollment." : "Unable to start MFA enrollment.");
        return;
      }
      setEnrollment(body);
      setCode("");
    } catch {
      setMessage("Could not reach the authentication service. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function submitCode(path: "/mfa/enable" | "/mfa/disable") {
    setBusy(true);
    setMessage(null);
    try {
      const response = await authFetch(path, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      });
      if (!response.ok) {
        const body = (await response.json()) as { message?: string };
        setMessage(body.message ?? "Unable to update MFA settings.");
        return;
      }
      if (path === "/mfa/enable") {
        const body = (await response.json()) as { recoveryCodes: string[] };
        setRecoveryCodes(body.recoveryCodes);
        setEnabled(true);
        setEnrollment(null);
        setMessage("MFA is enabled. Save these recovery codes now; they will only be shown once.");
      } else {
        setEnabled(false);
        setEnrollment(null);
        setRecoveryCodes([]);
        setMessage("MFA has been disabled.");
      }
      setCode("");
    } catch {
      setMessage("Could not reach the authentication service. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-xl">
        <CardHeader>
          <CardTitle>Multi-factor authentication</CardTitle>
          <CardDescription>
            Use a TOTP authenticator app and keep recovery codes somewhere safe.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {enabled === null && !message && <p className="text-sm text-muted-foreground">Loading security settings...</p>}
          {enabled === false && !enrollment && (
            <Button disabled={busy} onClick={startEnrollment}>
              Set up authenticator
            </Button>
          )}
          {enrollment && (
            <div className="space-y-4">
              <div className="space-y-2">
                <p className="text-sm">
                  Add this account in an authenticator app by entering the setup key:
                </p>
                <code className="block break-all rounded-md bg-muted p-3 text-sm">{enrollment.secret}</code>
                <details>
                  <summary className="cursor-pointer text-sm text-primary">Show authenticator URI</summary>
                  <code className="mt-2 block break-all rounded-md bg-muted p-3 text-xs">
                    {enrollment.otpauthUri}
                  </code>
                </details>
              </div>
              <div className="space-y-2">
                <Label htmlFor="mfa-enrollment-code">Enter the 6-digit code from your app</Label>
                <Input
                  id="mfa-enrollment-code"
                  required
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  value={code}
                  onChange={(event) => setCode(event.target.value)}
                />
              </div>
              <div className="flex gap-2">
                <Button disabled={busy || code.length !== 6} onClick={() => submitCode("/mfa/enable")}>
                  Verify and enable
                </Button>
                <Button variant="outline" disabled={busy} onClick={() => setEnrollment(null)}>
                  Cancel
                </Button>
              </div>
            </div>
          )}
          {enabled && (
            <div className="space-y-3">
              <p className="text-sm text-muted-foreground">MFA is enabled for this account.</p>
              <Label htmlFor="mfa-disable-code">Enter an authenticator or unused recovery code to disable MFA</Label>
              <Input
                id="mfa-disable-code"
                autoComplete="one-time-code"
                maxLength={64}
                value={code}
                onChange={(event) => setCode(event.target.value)}
              />
              <Button variant="destructive" disabled={busy || !code} onClick={() => submitCode("/mfa/disable")}>
                Disable MFA
              </Button>
            </div>
          )}
          {recoveryCodes.length > 0 && (
            <section className="space-y-2 rounded-md border border-border p-4">
              <h2 className="text-sm font-semibold">Recovery codes — shown once</h2>
              <p className="text-xs text-muted-foreground">
                Each code can be used once instead of an authenticator code. Store them securely.
              </p>
              <ul className="grid grid-cols-2 gap-2 font-mono text-sm">
                {recoveryCodes.map((recoveryCode) => <li key={recoveryCode}>{recoveryCode}</li>)}
              </ul>
              <Button variant="outline" size="sm" onClick={() => setRecoveryCodes([])}>
                I saved these codes
              </Button>
            </section>
          )}
          {message && <p role="status" className="text-sm text-muted-foreground">{message}</p>}
          <Link className="block text-sm text-primary underline-offset-2 hover:underline" href="/">
            Return to SyntaxMath
          </Link>
        </CardContent>
      </Card>
    </main>
  );
}
