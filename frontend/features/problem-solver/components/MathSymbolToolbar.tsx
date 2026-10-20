import { Button } from "@/components/ui/button";

const GROUPS = [
  { label: "Essentials", symbols: ["+", "−", "×", "÷", "=", "≠", "±", "∞"] },
  { label: "Powers & roots", symbols: ["^", "²", "³", "√", "∛", "|x|", "!", "%"] },
  { label: "Calculus", symbols: ["∫", "∬", "∂", "lim", "Σ", "∏", "d/dx", "∇"] },
  { label: "Greek & relations", symbols: ["π", "θ", "Δ", "λ", "α", "β", "≤", "≥"] },
] as const;

export function MathSymbolToolbar({ onInsert }: { onInsert: (symbol: string) => void }) {
  return (
    <div className="space-y-3" role="group" aria-label="Insert math symbol">
      {GROUPS.map((group) => (
        <div key={group.label}>
          <p className="mb-1.5 text-[11px] font-semibold uppercase text-muted-foreground">
            {group.label}
          </p>
          <div className="grid grid-cols-8 gap-1.5">
            {group.symbols.map((symbol) => (
              <Button
                key={symbol}
                type="button"
                variant="outline"
                size="icon"
                onClick={() => onInsert(symbol)}
                aria-label={`Insert ${symbol}`}
                className="h-9 w-full min-w-0 bg-background font-mono text-sm shadow-none"
              >
                {symbol}
              </Button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}