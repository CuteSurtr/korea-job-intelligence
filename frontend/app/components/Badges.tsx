export function Unknown() {
  return <span className="unknown">unknown</span>;
}

export function OrUnknown({ value }: { value: string | number | null | undefined }) {
  if (value === null || value === undefined || value === "") {
    return <Unknown />;
  }
  return <>{value}</>;
}

export function LifecycleBadge({ state }: { state: string }) {
  const tone =
    state === "ACTIVE" || state === "REOPENED"
      ? "badge-good"
      : state === "CLOSED"
        ? "badge-bad"
        : state === "STALE" || state === "UNVERIFIED"
          ? "badge-warn"
          : "badge-unknown";
  return <span className={`badge ${tone}`}>{state.toLowerCase()}</span>;
}

export function SeniorityBadge({ bucket }: { bucket: string | null }) {
  if (!bucket) {
    return <Unknown />;
  }
  const tone =
    bucket === "A" || bucket === "B"
      ? "badge-good"
      : bucket === "C"
        ? "badge-warn"
        : "badge-bad";
  return <span className={`badge ${tone}`}>{bucket}</span>;
}

export function RiskBadge({ level }: { level: string }) {
  const tone =
    level === "LOW"
      ? "badge-good"
      : level === "MODERATE"
        ? "badge-warn"
        : level === "HIGH"
          ? "badge-bad"
          : "badge-unknown";
  return <span className={`badge ${tone}`}>{level.toLowerCase()}</span>;
}

export function StatusBadge({ status }: { status: string }) {
  const tone =
    status === "SUCCEEDED" || status === "OFFER" || status === "APPLIED"
      ? "badge-good"
      : status === "PARTIAL" || status === "INTERVIEW" || status === "READY_TO_APPLY"
        ? "badge-warn"
        : status === "FAILED" || status === "REJECTED"
          ? "badge-bad"
          : "badge-unknown";
  return <span className={`badge ${tone}`}>{status.toLowerCase().replace(/_/g, " ")}</span>;
}

export function CircuitBadge({ state }: { state: string }) {
  const tone =
    state === "CLOSED" ? "badge-good" : state === "HALF_OPEN" ? "badge-warn" : "badge-bad";
  return <span className={`badge ${tone}`}>{state.toLowerCase().replace(/_/g, " ")}</span>;
}

export function ScoreCell({ value }: { value: number | null }) {
  if (value === null || value === undefined) {
    return <Unknown />;
  }
  const tone = value >= 60 ? "badge-good" : value >= 35 ? "badge-warn" : "badge-bad";
  return <span className={`badge ${tone}`}>{value.toFixed(0)}</span>;
}
