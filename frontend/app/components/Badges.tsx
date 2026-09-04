import { Badge, Unknown } from "./ui";

/*
 * The console's vocabulary of small labels.
 *
 * Each maps a domain value to one of five tones. Tone is meaningful, not decorative: good is
 * something working, bad is something that failed or closed, warn is something that needs
 * attention, and neutral is the honest answer when a value is simply unestablished. Anything
 * that would be a fourth colour gets neutral instead — a table where every cell is a different
 * hue is a table nobody reads.
 */

export function LifecycleBadge({ state }: { state: string }) {
  const tone =
    state === "ACTIVE" || state === "REOPENED"
      ? "good"
      : state === "CLOSED"
        ? "bad"
        : state === "STALE" || state === "UNVERIFIED"
          ? "warn"
          : "neutral";
  return <Badge tone={tone}>{state.toLowerCase()}</Badge>;
}

export function SeniorityBadge({ bucket }: { bucket: string | null }) {
  if (!bucket) {
    return <Unknown />;
  }
  const tone = bucket === "A" || bucket === "B" ? "good" : bucket === "C" ? "warn" : "bad";
  return <Badge tone={tone}>{bucket}</Badge>;
}

export function RiskBadge({ level }: { level: string }) {
  // An unassessed company is the common case, and a column of identical neutral badges reads as
  // data when it is the absence of data. Say it the same quiet way every other blank is said.
  if (!level || level === "UNKNOWN") {
    return <Unknown />;
  }
  const tone = level === "LOW" ? "good" : level === "MODERATE" ? "warn" : "bad";
  return <Badge tone={tone}>{level.toLowerCase()}</Badge>;
}

export function StatusBadge({ status }: { status: string }) {
  const tone =
    status === "SUCCEEDED" || status === "OFFER" || status === "APPLIED"
      ? "good"
      : status === "PARTIAL" || status === "INTERVIEW" || status === "READY_TO_APPLY"
        ? "warn"
        : status === "FAILED" || status === "REJECTED"
          ? "bad"
          : "neutral";
  return <Badge tone={tone}>{status.toLowerCase().replace(/_/g, " ")}</Badge>;
}

export function CircuitBadge({ state }: { state: string }) {
  const tone = state === "CLOSED" ? "good" : state === "HALF_OPEN" ? "warn" : "bad";
  return <Badge tone={tone}>{state.toLowerCase().replace(/_/g, " ")}</Badge>;
}

/**
 * A score, shown as the number it is.
 *
 * Scores run 0–100 and are compared down a column, so they are set in tabular figures and
 * tinted only at the ends of the range. Tinting every score would make the column a gradient
 * and the outliers harder to find, not easier.
 */
export function ScoreCell({ value }: { value: number | null }) {
  if (value === null || value === undefined) {
    return <span className="text-muted-foreground/70">—</span>;
  }
  // Only a strong score is worth a badge. Most postings score low, and tinting those turned the
  // column into a wall of red that made the good ones harder to find, not easier — which is the
  // opposite of what a score column is for.
  if (value >= 60) {
    return <Badge tone="good">{value.toFixed(0)}</Badge>;
  }
  return (
    <span className={value >= 35 ? "text-foreground" : "text-muted-foreground"}>
      {value.toFixed(0)}
    </span>
  );
}
