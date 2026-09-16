"use client";

import katex from "katex";
import "katex/dist/katex.min.css";
import { useMemo } from "react";

import { cn } from "@/lib/utils";

/**
 * Renders a string containing $...$ and $$...$$ segments.
 * The whole string is parsed in one pass, so raw LaTeX is never shown
 * mid-render (stream shielding happens before this component is mounted).
 */
function toHtml(source: string): string {
  const escapeHtml = (s: string) =>
    s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  const parts = source.split(/(\$\$[\s\S]+?\$\$|\$[^$\n]+?\$)/g);

  return parts
    .map((part) => {
      const isBlock = part.startsWith("$$") && part.endsWith("$$") && part.length > 4;
      const isInline =
        !isBlock && part.startsWith("$") && part.endsWith("$") && part.length > 2;
      if (!isBlock && !isInline) return escapeHtml(part);

      const tex = isBlock ? part.slice(2, -2) : part.slice(1, -1);
      try {
        return katex.renderToString(tex.trim(), {
          displayMode: isBlock,
          throwOnError: false,
          output: "html",
        });
      } catch {
        return escapeHtml(tex);
      }
    })
    .join("");
}

export function MathText({
  children,
  className,
  as: Tag = "div",
}: {
  children: string;
  className?: string;
  as?: "div" | "span" | "p" | "h2" | "h3";
}) {
  const html = useMemo(() => toHtml(children), [children]);
  return (
    <Tag className={cn("prose-math", className)} dangerouslySetInnerHTML={{ __html: html }} />
  );
}