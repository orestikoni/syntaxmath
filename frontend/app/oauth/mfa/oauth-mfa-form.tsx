"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authFetch } from "@/lib/auth-api";

export function OAuthMfaForm() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function completeSignIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      const response = await authFetch("/mfa/oauth/complete", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      });
      if (!response.ok) {
        const body = (await response.json()) as { message?: string };
        setMessage(body.message ?? "Unable to complete sign-in. Try again.");
        return;
      }
      router.replace("/");
    } catch {
      setMessage("Could not reach the authentication service. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Verify your sign-in</CardTitle>
          <CardDescription>
            Enter a current authenticator code or an unused recovery code to finish signing in with Google.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={completeSignIn}>
            <div className="space-y-2">
              <Label htmlFor="oauth-mfa-code">Authenticator or recovery code</Label>
              <Input
                id="oauth-mfa-code"
                required
                autoComplete="one-time-code"
                maxLength={64}
                value={code}
                onChange={(event) => setCode(event.target.value)}
              />
            </div>
            <Button className="w-full" disabled={busy || !code}>
              {busy ? "Verifying..." : "Complete sign-in"}
            </Button>
            {message && <p role="alert" className="text-sm text-destructive">{message}</p>}
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
