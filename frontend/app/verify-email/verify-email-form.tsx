"use client";

import { useState } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { authApi } from "@/lib/auth-api";

export function VerifyEmailForm({ token }: { token: string }) {
  const [status, setStatus] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function verifyEmail() {
    setIsSubmitting(true);
    setStatus(null);
    try {
      const response = await fetch(authApi("/verification/confirm"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token }),
      });
      const body = (await response.json()) as { message?: string };
      setStatus(body.message ?? (response.ok ? "Email verified." : "This link is invalid or expired."));
    } catch {
      setStatus("Could not reach the authentication service. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Verify your email</CardTitle>
          <CardDescription>
            Confirm your email address to finish setting up your SyntaxMath account.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {!token && <p className="text-sm text-destructive">This verification link is missing its token.</p>}
          <Button className="w-full" disabled={!token || isSubmitting} onClick={verifyEmail}>
            {isSubmitting ? "Verifying..." : "Verify email"}
          </Button>
          {status && <p role="status" className="text-sm text-muted-foreground">{status}</p>}
          <Link className="block text-center text-sm text-primary underline-offset-2 hover:underline" href="/">
            Return to SyntaxMath
          </Link>
        </CardContent>
      </Card>
    </main>
  );
}
