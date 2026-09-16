import type {
  ExplanationDepth,
  ScopeTemplate,
  Solution,
  SolveResponse,
} from "../types";

/*TODO:
 * TEMPORARY DEV STOPGAP.
 * Fakes the real pipeline (Llama 3 parsing → mXparser computation) so the UI
 * stays testable while the Spring Boot endpoint doesn't exist yet. Delete
 * this simulated logic once that endpoint is ready — nothing else on the
 * frontend needs to change, since everything else only depends on the
 * SolveResponse shape.
 */

const SIMULATED_DELAY_MS = 900;

const UNSUPPORTED_PATTERN =
  /\b(prove|proof|derive the general|simplify|factor(i[sz]e)?|solve for|in terms of|symbolic|identity)\b/i;

const SCOPE_TEMPLATES: ScopeTemplate[] = [
  {
    label: "Evaluate at a specific point",
    query: "Evaluate $\\frac{d}{dx}(x^3 - 4x)$ at $x = 2$",
  },
  {
    label: "Compute a definite integral",
    query: "Compute $$\\int_{1}^{3} 3x^2 \\, dx$$",
  },
  {
    label: "Numerically check an identity",
    query: "Check whether $\\sin^2(0.7) + \\cos^2(0.7) = 1$ numerically",
  },
];

function depthMultiplier(depth: ExplanationDepth): number {
  return depth === "concise" ? 2 : depth === "standard" ? 3 : 4;
}

function buildSolution(
  question: string,
  depth: ExplanationDepth,
): Solution {
  const allSteps = [
    {
      title: "Identify the integrand and limits",
      body: `We are integrating $3x^2$ over the closed interval $[1, 3]$.`,
    },
    {
      title: "Find the antiderivative",
      body: "By the power rule, $\\int 3x^2 \\, dx = x^3 + C$, since $\\frac{d}{dx}x^3 = 3x^2$.",
    },
    {
      title: "Apply the limits of integration",
      body: "Evaluate the antiderivative at both endpoints: $$\\left[x^3\\right]_{1}^{3} = 3^3 - 1^3$$",
    },
    {
      title: "Reduce to the final value",
      body: "This gives $27 - 1 = 26$, which matches the value returned by the deterministic engine.",
    },
  ];

  return {
    overview: `Evaluate the definite integral described by: "${question.replace(/\$+/g, "").trim()}".`,
    formula: "\\int_{1}^{3} 3x^2 \\, dx",
    result: "26",
    steps: allSteps.slice(0, depthMultiplier(depth)),
  };
}

export async function solveProblem(input: {
  question: string;
  depth: ExplanationDepth;
}): Promise<SolveResponse> {
  await new Promise((r) => setTimeout(r, SIMULATED_DELAY_MS));

  if (UNSUPPORTED_PATTERN.test(input.question)) {
    return {
      status: "UNSUPPORTED",
      message:
        "We spotted that this requires a symbolic proof. To ensure absolute arithmetic accuracy, our system currently focuses strictly on numerical calculations. Try adjusting your problem to evaluate at a specific point.",
      templates: SCOPE_TEMPLATES,
    };
  }

  return {
    status: "OK",
    solution: buildSolution(input.question, input.depth),
  };
}