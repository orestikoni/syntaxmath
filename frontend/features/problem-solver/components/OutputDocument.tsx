import { MathEngineCallout } from "./MathEngineCallout";
import { ProblemOverviewCard } from "./ProblemOverviewCard";
import { StepByStepCard } from "./StepByStepCard";
import type { Solution } from "../types";

export function OutputDocument({ solution }: { solution: Solution }) {
  return (
    <div className="space-y-4">
      <ProblemOverviewCard overview={solution.overview} />
      <MathEngineCallout formula={solution.formula} result={solution.result} />
      <StepByStepCard steps={solution.steps} />
    </div>
  );
}