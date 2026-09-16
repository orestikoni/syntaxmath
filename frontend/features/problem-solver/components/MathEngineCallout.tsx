import { BadgeCheck, SearchCheck } from "lucide-react";

import { MathText } from "./MathText";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

export function MathEngineCallout({ formula, result }: { formula: string; result: string }) {
  return (
    <section className="rounded-lg border-2 border-verified-border bg-verified p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-verified-foreground">
          <SearchCheck className="size-3.5" aria-hidden />
          Deterministic calculation
        </h3>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              aria-label="Verified via deterministic calculation engine"
              className="flex items-center gap-1 rounded-full border border-verified-border bg-card px-2 py-0.5 text-[11px] font-medium text-verified-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
            >
              <BadgeCheck className="size-3.5" aria-hidden />
              Verified
            </button>
          </TooltipTrigger>
          <TooltipContent>Verified via deterministic calculation engine.</TooltipContent>
        </Tooltip>
      </div>

      <dl className="space-y-3">
        <div>
          <dt className="text-[11px] font-medium uppercase tracking-wide text-verified-foreground/80">
            Formula verified
          </dt>
          <dd>
            <MathText className="text-[15px] text-foreground">{`$${formula}$`}</MathText>
          </dd>
        </div>
        <div>
          <dt className="text-[11px] font-medium uppercase tracking-wide text-verified-foreground/80">
            Exact calculated output
          </dt>
          <dd className="text-3xl font-semibold tabular-nums text-verified-foreground">
            {result}
          </dd>
        </div>
      </dl>
    </section>
  );
}