export type ExplanationDepth = "concise" | "standard" | "thorough";

export interface SolutionStep {
  title: string;
  body: string;
}

export interface Solution {
  overview: string;
  formula: string;
  result: string;
  steps: SolutionStep[];
}

export interface ScopeTemplate {
  label: string;
  query: string;
}

export type SolveResponse =
  | { status: "OK"; solution: Solution }
  | { status: "UNSUPPORTED"; message: string; templates: ScopeTemplate[] };

/*TODO: This is a temporary type until the backend is implemented. 
The backend will return a more structured response, and the frontend will need to be 
updated to handle that.
*/
export const SOLVE_STAGES = [
  "Parsing intent",
  "Validating formula",
  "Computing exact value",
  "Generating steps",
] as const;