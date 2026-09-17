import { MathText } from "./MathText";

export function QuestionHeader({ question, index }: { question: string; index: number }) {
  return (
    <header className="mb-5">
      <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
        Problem {String(index).padStart(2, "0")}
      </p>
      <MathText as="h2" className="text-2xl font-semibold leading-snug text-foreground">
        {question}
      </MathText>
    </header>
  );
}