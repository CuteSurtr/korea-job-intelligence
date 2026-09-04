/**
 * Shared application vocabulary.
 *
 * This is a plain module rather than part of `actions.ts` because a `"use server"` file may
 * only export async functions, and the forms need these values to render their selects.
 */

/** The statuses the API accepts, in the order an application usually moves through them. */
export const APPLICATION_STATUSES = [
  "NOT_REVIEWED",
  "INTERESTED",
  "READY_TO_APPLY",
  "APPLIED",
  "INTERVIEW",
  "OFFER",
  "REJECTED",
  "WITHDRAWN",
  "IGNORED",
] as const;

export type ApplicationStatus = (typeof APPLICATION_STATUSES)[number];

export function isApplicationStatus(value: string): value is ApplicationStatus {
  return (APPLICATION_STATUSES as readonly string[]).includes(value);
}

/** The status as a person reads it: "ready to apply" rather than "READY_TO_APPLY". */
export function statusLabel(status: string): string {
  return status.toLowerCase().replace(/_/g, " ");
}
