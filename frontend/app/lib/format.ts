export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return "unknown";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "unknown";
  }
  return date.toISOString().slice(0, 10);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "unknown";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "unknown";
  }
  return date.toISOString().replace("T", " ").slice(0, 16);
}

export function formatRelativeDays(value: string | null | undefined): string {
  if (!value) {
    return "unknown";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "unknown";
  }
  const days = Math.round((Date.now() - date.getTime()) / 86_400_000);
  if (days === 0) {
    return "today";
  }
  if (days === 1) {
    return "1 day ago";
  }
  if (days < 0) {
    return `in ${Math.abs(days)} days`;
  }
  return `${days} days ago`;
}

export function formatScore(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "unknown";
  }
  return value.toFixed(0);
}

export function formatExperience(min: number | null, max: number | null): string {
  if (min === null && max === null) {
    return "unknown";
  }
  if (min !== null && max !== null) {
    return `${min}-${max} yrs`;
  }
  if (min !== null) {
    return min === 0 ? "entry" : `${min}+ yrs`;
  }
  return `up to ${max} yrs`;
}

export function formatDeadline(deadlineAt: string | null, openEnded: boolean): string {
  if (openEnded) {
    return "continuous";
  }
  if (!deadlineAt) {
    return "unknown";
  }
  return formatDate(deadlineAt);
}

export function orUnknown(value: string | null | undefined): string {
  return value === null || value === undefined || value === "" ? "unknown" : value;
}
