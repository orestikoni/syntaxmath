import type { Solution } from "@/features/problem-solver/types";

export interface Entry {
  id: string;
  question: string;
  solution: Solution;
}

export interface Thread {
  id: string;
  createdAt: string; // ISO
  entries: Entry[];
}