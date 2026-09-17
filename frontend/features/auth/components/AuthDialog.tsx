"use client";

import { useState } from "react";
import { Mail } from "lucide-react";

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

type Mode = "signin" | "signup";

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
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [notice, setNotice] = useState<string | null>(null);

  const signup = mode === "signup";

  function pretend(message: string) {
    setNotice(message);
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) setNotice(null);
      }}
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader className="items-center text-center">
          <DialogTitle className="text-lg">
            {signup ? "Create your account" : "Sign in to Math Solver"}
          </DialogTitle>
          <DialogDescription>
            {signup
              ? "Save your worked solutions and pick up where you left off."
              : "Welcome back — your solution history is waiting."}
          </DialogDescription>
        </DialogHeader>

        <Button
          variant="outline"
          className="w-full justify-center gap-2"
          onClick={() => pretend("Google sign-in is not connected in this preview.")}
        >
          <GoogleMark />
          Continue with Google
        </Button>

        <div className="flex items-center gap-3">
          <Separator className="flex-1" />
          <span className="text-[11px] uppercase tracking-wider text-muted-foreground">or</span>
          <Separator className="flex-1" />
        </div>

        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            pretend(
              signup
                ? "Account creation is not connected in this preview."
                : "Email sign-in is not connected in this preview.",
            );
          }}
        >
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
          <div className="space-y-1.5">
            <Label htmlFor="auth-password" className="text-xs text-muted-foreground">
              Password
            </Label>
            <Input
              id="auth-password"
              type="password"
              required
              minLength={6}
              autoComplete={signup ? "new-password" : "current-password"}
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <Button type="submit" className="w-full gap-2">
            <Mail className="size-4" aria-hidden />
            {signup ? "Create account" : "Sign in with email"}
          </Button>
        </form>

        {notice && (
          <p className="rounded-md border border-border bg-surface px-3 py-2 text-center text-xs text-muted-foreground">
            {notice}
          </p>
        )}

        <p className="text-center text-xs text-muted-foreground">
          {signup ? "Already have an account?" : "Don't have an account?"}{" "}
          <button
            type="button"
            className="font-medium text-primary underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            onClick={() => {
              setMode(signup ? "signin" : "signup");
              setNotice(null);
            }}
          >
            {signup ? "Sign in" : "Sign up"}
          </button>
        </p>
      </DialogContent>
    </Dialog>
  );
}