import { Check, Loader2 } from "lucide-react";

import { cn } from "@/lib/utils";
import { SOLVE_STAGES } from "../types";

export function ProgressGauge({ current }: { current: number }) {
  const pct = Math.round((current / SOLVE_STAGES.length) * 100);

  return (
    <section
      aria-live="polite"
      aria-label="Solving progress"
      className="rounded-lg border border-border bg-card p-5"
    >
      <div className="mb-4 flex items-baseline justify-between">
        <h3 className="text-sm font-semibold text-foreground">Working through the problem</h3>
        <span className="font-mono text-xs text-muted-foreground">{pct}%</span>
      </div>

      <ol className="space-y-2.5">
        {SOLVE_STAGES.map((stage, i) => {
          const done = i < current;
          const active = i === current;
          return (
            <li
              key={stage}
              className={cn(
                "flex items-center gap-2.5 text-sm transition-colors",
                done ? "text-foreground" : active ? "text-foreground" : "text-muted-foreground/60",
              )}
            >
              <span className="flex size-5 items-center justify-center rounded-full border border-border bg-background">
                {done ? (
                  <Check className="size-3 text-primary" aria-hidden />
                ) : active ? (
                  <Loader2 className="size-3 animate-spin text-primary" aria-hidden />
                ) : (
                  <span className="size-1.5 rounded-full bg-border" />
                )}
              </span>
              <span className="font-mono text-xs tabular-nums text-muted-foreground">[{i + 1}]</span>
              <span>{stage}…</span>
            </li>
          );
        })}
      </ol>

      <div className="mt-4 h-1 w-full overflow-hidden rounded-full bg-surface">
        <div
          className="h-full rounded-full bg-primary transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
    </section>
  );
}