import { Lightbulb } from "lucide-react";

import { MathText } from "./MathText";

export function ProblemOverviewCard({ overview }: { overview: string }) {
  return (
    <section className="rounded-lg border border-border bg-card p-5">
      <h3 className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        <Lightbulb className="size-3.5" aria-hidden />
        Problem overview
      </h3>
      <MathText className="text-[15px] text-foreground">{overview}</MathText>
    </section>
  );
}