import { Compass } from "lucide-react";

import { MathText } from "./MathText";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { ScopeTemplate } from "../types";

export function ScopeGuideModal({
  open,
  message,
  templates,
  onOpenChange,
  onPickTemplate,
}: {
  open: boolean;
  message: string;
  templates: ScopeTemplate[];
  onOpenChange: (open: boolean) => void;
  onPickTemplate: (query: string) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Compass className="size-4 text-primary" aria-hidden />
            Scope guide
          </DialogTitle>
          <DialogDescription className="text-[15px] leading-relaxed">
            {message}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            Try one of these instead
          </p>
          {templates.map((template) => (
            <button
              key={template.label}
              type="button"
              onClick={() => onPickTemplate(template.query)}
              className="w-full rounded-md border border-border bg-background px-3 py-2.5 text-left transition-colors hover:border-primary/40 hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
            >
              <span className="block text-sm font-medium text-foreground">
                {template.label}
              </span>
              <MathText className="mt-0.5 text-sm text-muted-foreground">
                {template.query}
              </MathText>
            </button>
          ))}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            I'll rephrase it myself
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}