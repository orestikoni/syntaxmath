import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ExplanationDepth } from "@/features/problem-solver/types";

export function ContextToggles({
  depth,
  onDepthChange,
}: {
  depth: ExplanationDepth;
  onDepthChange: (value: ExplanationDepth) => void;
}) {
  return (
    <div className="space-y-3 border-t border-sidebar-border px-4 py-4">
      <h3 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
        Session context
      </h3>

      <div className="space-y-1.5">
        <Label htmlFor="explanation-depth" className="text-xs text-muted-foreground">
          Explanation depth
        </Label>
        <Select value={depth} onValueChange={(v) => onDepthChange(v as ExplanationDepth)}>
          <SelectTrigger id="explanation-depth" className="h-8 bg-card text-sm">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="concise">Concise</SelectItem>
            <SelectItem value="standard">Standard</SelectItem>
            <SelectItem value="thorough">Thorough</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <p className="text-[11px] leading-relaxed text-muted-foreground">
        These settings persist across the session and shape every solution.
      </p>
    </div>
  );
}