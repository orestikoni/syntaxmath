"use client";

import { FileText } from "lucide-react";
import { useEffect, useState } from "react";

import { cn } from "@/lib/utils";
import { groupThreads } from "../lib/groupThreads";
import type { Thread } from "../types";

const GLYPHS: Record<string, string> = {
  int: "∫", sqrt: "√", pi: "π", theta: "θ", Delta: "Δ", sum: "Σ",
  times: "×", cdot: "·", infty: "∞", sin: "sin", cos: "cos", tan: "tan",
  log: "log", frac: "÷",
};

/** Turns a LaTeX-bearing question into a readable one-line preview. */
function plain(source: string) {
  return source
    .replace(/\$+/g, "")
    .replace(/[_^]\{[^}]*\}/g, "")
    .replace(/[_^]\S/g, "")
    .replace(/\\[,;!:]/g, " ")
    .replace(/\\(left|right)/g, "")
    .replace(/\\([a-zA-Z]+)/g, (_, name: string) => GLYPHS[name] ?? " ")
    .replace(/[{}]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

export function ThreadHistoryList({
  threads,
  activeId,
  onSelect,
}: {
  threads: Thread[];
  activeId: string | null;
  onSelect: (id: string) => void;
}) {
  // Date grouping depends on "now", which differs between server and
  // browser — render a flat list first, then group after hydration.
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => setHydrated(true), []);

  const groups: Array<[string, Thread[]]> = hydrated ? groupThreads(threads) : [["", threads]];

  if (groups.length === 0) {
    return (
      <p className="px-3 py-6 text-sm text-muted-foreground">
        No problems yet. Your solved questions will be listed here.
      </p>
    );
  }

  return (
    <nav aria-label="Thread history" className="space-y-5 px-2 pb-4">
      {groups.map(([label, items]) => (
        <section key={label || "all"}>
          {label && (
            <h3 className="px-2 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
              {label}
            </h3>
          )}
          <ul className="space-y-0.5">
            {items.map((thread) => {
              const isActive = thread.id === activeId;
              return (
                <li key={thread.id}>
                  <button
                    type="button"
                    onClick={() => onSelect(thread.id)}
                    aria-current={isActive ? "page" : undefined}
                    className={cn(
                      "flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors",
                      "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 focus-visible:ring-offset-sidebar",
                      isActive
                        ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
                        : "text-muted-foreground hover:bg-sidebar-accent/60 hover:text-foreground",
                    )}
                  >
                    <FileText className="size-3.5 shrink-0 opacity-60" aria-hidden />
                    <span className="truncate">
                      {plain(thread.entries[0]?.question ?? "Untitled problem")}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </section>
      ))}
    </nav>
  );
}