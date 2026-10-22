"use client";

import { useState } from "react";
import { Mail } from "lucide-react";

import { authFetch, AUTH_API_URL } from "@/lib/auth-api";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";

type Mode = "signin" | "signup" | "forgot";

function GoogleMark() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden>
      <path fill="#4285F4" d="M23.52 12.27c0-.85-.08-1.67-.22-2.45H12v4.64h6.46a5.52 5.52 0 0 1-2.4 3.62v3h3.88c2.27-2.09 3.58-5.17 3.58-8.81Z" />
      <path fill="#34A853" d="M12 24c3.24 0 5.96-1.08 7.94-2.92l-3.88-3c-1.08.72-2.45 1.15-4.06 1.15-3.12 0-5.77-2.11-6.72-4.95H1.28v3.1A12 12 0 0 0 12 24Z" />
      <path fill="#FBBC05" d="M5.28 14.28a7.2 7.2 0 0 1 0-4.56v-3.1H1.28a12 12 0 0 0 0 10.76l4-3.1Z" />
      <path fill="#EA4335" d="M12 4.75c1.76 0 3.34.61 4.59 1.8l3.43-3.43C17.95 1.19 15.23 0 12 0A12 12 0 0 0 1.28 6.62l4 3.1C6.23 6.86 8.88 4.75 12 4.75Z" />
    </svg>
  );
}

export function AuthDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [mode, setMode] = useState<Mode>("signin");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mfaCode, setMfaCode] = useState("");
  const [mfaRequired, setMfaRequired] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [canResendVerification, setCanResendVerification] = useState(false);

  const signup = mode === "signup";
  const forgot = mode === "forgot";

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) setNotice(null);
        if (!next) setCanResendVerification(false);
      }}
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader className="items-center text-center">
          <DialogTitle className="text-lg">
            {signup
              ? "Create your account"
              : forgot
                ? "Reset your password"
                : "Sign in to Math Solver"}
          </DialogTitle>
          <DialogDescription>
            {signup
              ? "Save your worked solutions and pick up where you left off."
              : forgot
                ? "We’ll email a secure link if an eligible account exists."
                : "Welcome back — your solution history is waiting."}
          </DialogDescription>
        </DialogHeader>

        {!forgot && (
          <>
            <Button
              asChild
              variant="outline"
              className="w-full justify-center gap-2"
            >
              <a href={`${AUTH_API_URL}/oauth2/authorization/google`}>
                <GoogleMark />
                Continue with Google
              </a>
            </Button>

            <div className="flex items-center gap-3">
              <Separator className="flex-1" />
              <span className="text-[11px] uppercase tracking-wider text-muted-foreground">
                or
              </span>
              <Separator className="flex-1" />
            </div>
          </>
        )}

        <form
          className="space-y-3"
          onSubmit={async (e) => {
            e.preventDefault();
            if (signup && password !== confirmPassword) {
              setNotice("Passwords do not match.");
              return;
            }
            setIsSubmitting(true);
            setNotice(null);
            setCanResendVerification(false);
            try {
              const path = signup
                ? "/register"
                : forgot
                  ? "/password-reset/request"
                  : "/login";
              const body = signup
                ? { email, password, firstName, lastName }
                : forgot
                  ? { email }
                  : { email, password, ...(mfaRequired ? { mfaCode } : {}) };
              const result = await authFetch(path, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
              });
              const payload = (await result.json()) as { message?: string; mfaRequired?: boolean };
              if (payload.mfaRequired) {
                setMfaRequired(true);
              }
              if (!result.ok) {
                setNotice(payload.message ?? "Unable to complete this request.");
                setCanResendVerification(result.status === 401 && !mfaRequired && !signup && !forgot);
                return;
              }
              if (signup) {
                setNotice(payload.message ?? "Check your email for a verification link.");
                setCanResendVerification(true);
              } else if (forgot) {
                setNotice(payload.message ?? "If the account is eligible, a reset email will be sent.");
              } else {
                setMfaRequired(false);
                setMfaCode("");
                onOpenChange(false);
                window.location.reload();
              }
            } catch {
              setNotice("Could not reach the authentication service. Please try again.");
            } finally {
              setIsSubmitting(false);
            }
          }}
        >
          {signup && (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="auth-first-name" className="text-xs text-muted-foreground">
                  Name
                </Label>
                <Input
                  id="auth-first-name"
                  type="text"
                  required
                  autoComplete="given-name"
                  placeholder="Jane"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="auth-last-name" className="text-xs text-muted-foreground">
                  Surname
                </Label>
                <Input
                  id="auth-last-name"
                  type="text"
                  required
                  autoComplete="family-name"
                  placeholder="Doe"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="auth-email" className="text-xs text-muted-foreground">
              Email
            </Label>
            <Input
              id="auth-email"
              type="email"
              required
              autoComplete="email"
              placeholder="you@university.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          {!forgot && (
            <div className="space-y-1.5">
              <Label htmlFor="auth-password" className="text-xs text-muted-foreground">
                Password
              </Label>
              <Input
                id="auth-password"
                type="password"
                required
                minLength={signup ? 12 : undefined}
                maxLength={signup ? 128 : undefined}
                autoComplete={signup ? "new-password" : "current-password"}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          )}

          {mfaRequired && !signup && (
            <div className="space-y-1.5">
              <Label htmlFor="auth-mfa-code" className="text-xs text-muted-foreground">
                Authenticator or recovery code
              </Label>
              <Input
                id="auth-mfa-code"
                type="text"
                required
                autoComplete="one-time-code"
                inputMode="text"
                maxLength={64}
                value={mfaCode}
                onChange={(event) => setMfaCode(event.target.value)}
              />
            </div>
          )}

          {signup && (
            <div className="space-y-1.5">
              <Label htmlFor="auth-confirm-password" className="text-xs text-muted-foreground">
                Verify password
              </Label>
              <Input
                id="auth-confirm-password"
                type="password"
                required
                minLength={12}
                maxLength={128}
                autoComplete="new-password"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
          )}

          <Button type="submit" className="w-full gap-2" disabled={isSubmitting}>
            <Mail className="size-4" aria-hidden />
            {isSubmitting
              ? "Please wait..."
              : signup
                ? "Create account"
                : forgot
                  ? "Send reset link"
                  : "Sign in with email"}
          </Button>
        </form>

        {!signup && !forgot && (
          <button
            type="button"
            className="text-center text-xs text-primary underline-offset-2 hover:underline"
            onClick={() => {
              setMode("forgot");
              setNotice(null);
            }}
          >
            Forgot password?
          </button>
        )}

        {canResendVerification && (
          <Button
            type="button"
            variant="outline"
            disabled={isSubmitting}
            onClick={async () => {
              setIsSubmitting(true);
              try {
                const result = await authFetch("/verification/request", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ email }),
                });
                const payload = (await result.json()) as { message?: string };
                setNotice(payload.message ?? "If the address requires verification, an email will be sent.");
              } catch {
                setNotice("Could not reach the authentication service. Please try again.");
              } finally {
                setIsSubmitting(false);
              }
            }}
          >
            Resend verification email
          </Button>
        )}

        {notice && (
          <p className="rounded-md border border-border bg-muted px-3 py-2 text-center text-xs text-muted-foreground">
            {notice}
          </p>
        )}

        <p className="text-center text-xs text-muted-foreground">
          {signup
            ? "Already have an account?"
            : forgot
              ? "Remember your password?"
              : "Don't have an account?"}{" "}
          <button
            type="button"
            className="font-medium text-primary underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            onClick={() => {
              setMode(signup || forgot ? "signin" : "signup");
              setNotice(null);
              setCanResendVerification(false);
            }}
          >
            {signup || forgot ? "Sign in" : "Sign up"}
          </button>
        </p>
      </DialogContent>
    </Dialog>
  );
}