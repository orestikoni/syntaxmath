"use client";

import { ArrowUp, FunctionSquare } from "lucide-react";
import { useRef, useState } from "react";

import { MathSymbolToolbar } from "./MathSymbolToolbar";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

export function ProblemInputCard({
  value,
  onChange,
  onSubmit,
  busy,
}: {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  busy: boolean;
}) {
  const ref = useRef<HTMLTextAreaElement>(null);
  const [mathOpen, setMathOpen] = useState(false);

  function insert(symbol: string) {
    const el = ref.current;
    if (!el) {
      onChange(value + symbol);
      return;
    }
    const start = el.selectionStart ?? value.length;
    const end = el.selectionEnd ?? value.length;
    const next = value.slice(0, start) + symbol + value.slice(end);
    onChange(next);
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(start + symbol.length, start + symbol.length);
    });
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
      className="rounded-lg border border-input bg-card px-3 py-2 shadow-sm transition-shadow focus-within:ring-1 focus-within:ring-ring"
    >
      <label htmlFor="problem-input" className="sr-only">
        Your math problem
      </label>
      <Textarea
        id="problem-input"
        ref={ref}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            onSubmit();
          }
        }}
        rows={1}
        placeholder="Ask a math question…"
        className="min-h-10 max-h-28 resize-none border-0 bg-transparent px-1 py-2 text-[15px] leading-6 shadow-none focus-visible:ring-0"
      />

      <div className="flex h-9 items-center justify-between gap-2">
        <Popover open={mathOpen} onOpenChange={setMathOpen}>
          <PopoverTrigger asChild>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              aria-label="Open math symbols"
              className={cn(
                "gap-1.5 px-2 text-muted-foreground shadow-none",
                mathOpen && "bg-accent text-accent-foreground",
              )}
            >
              <FunctionSquare className="size-4" aria-hidden />
              Math input
            </Button>
          </PopoverTrigger>
          <PopoverContent
            align="start"
            side="top"
            sideOffset={10}
            className="w-[min(30rem,calc(100vw-2rem))] p-3"
          >
            <div className="mb-3">
              <p className="text-sm font-semibold text-foreground">Math symbols</p>
              <p className="text-xs text-muted-foreground">Select a symbol to add it at the cursor.</p>
            </div>
            <MathSymbolToolbar onInsert={insert} />
          </PopoverContent>
        </Popover>

        <Button
          type="submit"
          size="icon"
          disabled={busy || value.trim().length === 0}
          aria-label={busy ? "Solving" : "Solve problem"}
          className="size-8 rounded-full"
        >
          <ArrowUp className="size-4" aria-hidden />
        </Button>
      </div>
    </form>
  );
}