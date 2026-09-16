import type { Thread } from "../types";

export function groupThreads(threads: Thread[]) {
  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const t = startOfToday.getTime();
  const day = 86_400_000;

  const groups: Array<[string, Thread[]]> = [
    ["Today", []],
    ["Yesterday", []],
    ["Last 7 Days", []],
    ["Older", []],
  ];

  for (const thread of threads) {
    const ts = new Date(thread.createdAt).getTime();
    const index =
      ts >= t ? 0 : ts >= t - day ? 1 : ts >= t - 7 * day ? 2 : 3;
    groups[index]![1].push(thread);
  }

  return groups.filter(([, items]) => items.length > 0);
}