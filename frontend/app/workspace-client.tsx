"use client";

import { FileText, Menu, PanelLeftClose, PanelLeftOpen, Plus } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { AuthDialog } from "@/features/auth/components/AuthDialog";
import { ProblemInputCard } from "@/features/problem-solver/components/ProblemInputCard";
import { ScopeGuideModal } from "@/features/problem-solver/components/ScopeGuideModal";
import { OutputDocument } from "@/features/problem-solver/components/OutputDocument";
import { ProgressGauge } from "@/features/problem-solver/components/ProgressGauge";
import { QuestionHeader } from "@/features/problem-solver/components/QuestionHeader";
import { solveProblem } from "@/features/problem-solver/server/solveProblem";
import type { ExplanationDepth, ScopeTemplate } from "@/features/problem-solver/types";
import { ContextToggles } from "@/features/history/components/ContextToggles";
import { ThreadHistoryList } from "@/features/history/components/ThreadHistoryList";
import type { Thread } from "@/features/history/types";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { TooltipProvider } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

const SAMPLE = "Compute $$\\int_{1}^{3} 3x^2 \\, dx$$";

export function Workspace() {
  const [threads, setThreads] = useState<Thread[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const [stage, setStage] = useState(-1);
  const [depth, setDepth] = useState<ExplanationDepth>("standard");
  const [collapsed, setCollapsed] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [scope, setScope] = useState<{ message: string; templates: ScopeTemplate[] } | null>(null);
  const canvasRef = useRef<HTMLDivElement>(null);

  // Session context persists — nothing resets silently.
  useEffect(() => {
    const saved = window.localStorage.getItem("math-solver-context");
    if (!saved) return;
    try {
      const parsed = JSON.parse(saved) as { depth?: ExplanationDepth };
      if (parsed.depth) setDepth(parsed.depth);
    } catch {
      /* ignore malformed state */
    }
  }, []);

  useEffect(() => {
    window.localStorage.setItem("math-solver-context", JSON.stringify({ depth }));
  }, [depth]);

  const activeThread = threads.find((t) => t.id === activeId) ?? null;
  const busy = stage >= 0;

  async function handleSolve() {
    const question = draft.trim();
    if (!question || busy) return;

    // TODO: once the backend streams real progress via SSE, drive `stage`
    // from that stream instead of just flipping it on/off around the call.
    setStage(0);
    const response = await solveProblem({ question, depth });
    setStage(-1);

    if (response.status === "UNSUPPORTED") {
      setScope({ message: response.message, templates: response.templates });
      return;
    }

    const entry = { id: `e-${Date.now()}`, question, solution: response.solution };

    if (activeThread) {
      setThreads((prev) =>
        prev.map((t) => (t.id === activeThread.id ? { ...t, entries: [...t.entries, entry] } : t)),
      );
    } else {
      const thread: Thread = { id: `t-${Date.now()}`, createdAt: new Date().toISOString(), entries: [entry] };
      setThreads((prev) => [thread, ...prev]);
      setActiveId(thread.id);
    }

    setDraft("");
    requestAnimationFrame(() =>
      canvasRef.current?.scrollTo({ top: canvasRef.current.scrollHeight, behavior: "smooth" }),
    );
  }

  function startNewProblem() {
    setActiveId(null);
    setDraft("");
    setDrawerOpen(false);
  }

  const sidebarBody = (
    <div className="flex h-full flex-col">
      <div className="px-4 py-4">
        <p className="mb-3 text-sm font-semibold tracking-tight text-foreground">Math Solver</p>
        <Button className="w-full justify-start gap-2" onClick={startNewProblem}>
          <Plus className="size-4" aria-hidden />
          New problem
        </Button>
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto">
        <ThreadHistoryList
          threads={threads}
          activeId={activeId}
          onSelect={(id) => {
            setActiveId(id);
            setDrawerOpen(false);
          }}
        />
      </div>
      <ContextToggles depth={depth} onDepthChange={setDepth} />
    </div>
  );

  return (
    <TooltipProvider delayDuration={150}>
      <div className="flex h-screen overflow-hidden bg-background">
        <aside
          className={cn(
            "hidden shrink-0 border-r border-sidebar-border bg-sidebar transition-[width] duration-200 md:flex md:flex-col",
            collapsed ? "w-14" : "w-72",
          )}
          aria-label="Control center"
        >
          {collapsed ? (
            <div className="flex flex-col items-center gap-2 py-4">
              <Button size="icon" aria-label="New problem" onClick={startNewProblem}>
                <Plus className="size-4" aria-hidden />
              </Button>
              {threads.slice(0, 6).map((thread) => (
                <button
                  key={thread.id}
                  type="button"
                  onClick={() => setActiveId(thread.id)}
                  aria-label="Open thread"
                  className={cn(
                    "flex size-9 items-center justify-center rounded-md transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                    thread.id === activeId
                      ? "bg-sidebar-accent text-foreground"
                      : "text-muted-foreground hover:bg-sidebar-accent/60",
                  )}
                >
                  <FileText className="size-4" aria-hidden />
                </button>
              ))}
            </div>
          ) : (
            sidebarBody
          )}
        </aside>

        <main className="flex min-w-0 flex-1 flex-col">
          <div className="flex items-center gap-2 border-b border-border bg-background/80 px-4 py-2.5 backdrop-blur">
            <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open history">
                  <Menu className="size-4" aria-hidden />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-72 bg-sidebar p-0">
                <SheetTitle className="sr-only">Control center</SheetTitle>
                {sidebarBody}
              </SheetContent>
            </Sheet>

            <Button
              variant="ghost"
              size="icon"
              className="hidden md:inline-flex"
              onClick={() => setCollapsed((c) => !c)}
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
              {collapsed ? <PanelLeftOpen className="size-4" aria-hidden /> : <PanelLeftClose className="size-4" aria-hidden />}
            </Button>

            <h1 className="text-sm font-medium text-foreground">
              {activeThread ? "Worked solutions" : "New problem"}
            </h1>
            <Button size="sm" className="ml-auto" onClick={() => setAuthOpen(true)}>
              Sign in
            </Button>
          </div>

          <div ref={canvasRef} className="min-h-0 flex-1 overflow-y-auto">
            <div className="mx-auto w-full max-w-3xl px-4 py-8 md:px-8">
              {!activeThread && !busy && (
                <section className="mb-8 rounded-lg border border-dashed border-border bg-card/60 p-6">
                  <h2 className="text-base font-semibold text-foreground">Start a new worked solution</h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Every answer is computed by a deterministic engine and then explained step by step.
                  </p>
                  <button
                    type="button"
                    onClick={() => setDraft(SAMPLE)}
                    className="mt-3 rounded-md border border-border bg-background px-3 py-1.5 text-sm text-foreground transition-colors hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                  >
                    Try a sample problem
                  </button>
                </section>
              )}

              {activeThread?.entries.map((entry, i) => (
                <article key={entry.id} className={cn("pb-10", i > 0 && "mt-10 border-t border-border pt-10")}>
                  <QuestionHeader question={entry.question} index={i + 1} />
                  <OutputDocument solution={entry.solution} />
                </article>
              ))}

              {busy && (
                <div className="pb-10">
                  <ProgressGauge current={stage} />
                </div>
              )}
            </div>
          </div>

          <div className="border-t border-border bg-background px-4 py-3 md:px-8">
            <div className="mx-auto w-full max-w-3xl">
              <ProblemInputCard value={draft} onChange={setDraft} onSubmit={handleSolve} busy={busy} />
            </div>
          </div>
        </main>

        <AuthDialog open={authOpen} onOpenChange={setAuthOpen} />

        <ScopeGuideModal
          open={scope !== null}
          message={scope?.message ?? ""}
          templates={scope?.templates ?? []}
          onOpenChange={(open) => !open && setScope(null)}
          onPickTemplate={(query) => {
            setDraft(query);
            setScope(null);
          }}
        />
      </div>
    </TooltipProvider>
  );
}