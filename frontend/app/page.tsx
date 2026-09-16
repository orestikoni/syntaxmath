import type { Metadata } from "next";

import { Workspace } from "./workspace-client";

const TITLE = "AI Math Solver — Verified Step-by-Step Solutions";
const DESCRIPTION =
  "A hybrid math workspace: a deterministic engine computes the exact answer, the AI explains every step. Structured, textbook-style solutions.";

export const metadata: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  openGraph: { title: TITLE, description: DESCRIPTION, type: "website" },
  twitter: { card: "summary_large_image" },
};

export default function Page() {
  return <Workspace />;
}