import { BookOpen } from "lucide-react";

import { MathText } from "./MathText";
import type { SolutionStep } from "../types";

export function StepByStepCard({ steps }: { steps: SolutionStep[] }) {
  return (
    <section className="rounded-lg border border-border bg-card p-5">
      <h3 className="mb-4 flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        <BookOpen className="size-3.5" aria-hidden />
        Step-by-step explanation
      </h3>
      <ol className="space-y-5">
        {steps.map((step, i) => (
          <li key={step.title} className="border-l-2 border-surface pl-4">
            <p className="mb-1 text-sm font-semibold text-foreground">
              Step {i + 1}: {step.title}
            </p>
            <MathText className="text-[15px] text-muted-foreground">{step.body}</MathText>
          </li>
        ))}
      </ol>
    </section>
  );
}