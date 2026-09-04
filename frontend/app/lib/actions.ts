"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { apiMessage, sendJson } from "./api";
import { isApplicationStatus, statusLabel } from "./applications";
import type { Application } from "./types";

/** A trimmed field, or undefined when the form did not carry one at all. */
function text(form: FormData, name: string): string | undefined {
  const value = form.get(name);
  if (typeof value !== "string") {
    return undefined;
  }
  return value.trim();
}

/**
 * A date input gives `YYYY-MM-DD`, which the API cannot read as an instant.
 *
 * A blank field is left out rather than sent empty: the API treats an absent field as "leave
 * this alone", so omitting it keeps whatever is already stored instead of failing to parse.
 */
function instant(form: FormData, name: string): string | undefined {
  const value = text(form, name);
  if (!value) {
    return undefined;
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return `${value}T00:00:00Z`;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString();
}

/** Everything on the edit form other than the status and its note. */
function details(form: FormData) {
  return {
    resumeVersion: text(form, "resumeVersion"),
    coverLetterVersion: text(form, "coverLetterVersion"),
    contactName: text(form, "contactName"),
    contactEmail: text(form, "contactEmail"),
    referral: text(form, "referral"),
    interviewStage: text(form, "interviewStage"),
    interviewNotes: text(form, "interviewNotes"),
    notes: text(form, "notes"),
    followUpAt: instant(form, "followUpAt"),
    appliedAt: instant(form, "appliedAt"),
  };
}

function failure(destination: string, message: string): never {
  const url = new URL(destination, "http://local");
  url.searchParams.set("error", message);
  redirect(`${url.pathname}${url.search}`);
}

function success(destination: string, message: string): never {
  const url = new URL(destination, "http://local");
  url.searchParams.set("saved", message);
  redirect(`${url.pathname}${url.search}`);
}

/**
 * Starts tracking a job, or moves the application that already tracks it.
 *
 * The API keys an application on the job and the profile and updates in place, so submitting
 * this twice moves one application rather than creating a second.
 */
export async function trackJob(form: FormData) {
  const jobId = Number(text(form, "jobId"));
  const status = text(form, "status") ?? "";
  const back = `/jobs/${jobId}`;

  if (!Number.isInteger(jobId) || jobId <= 0) {
    failure("/jobs", "That form did not carry a job to track.");
  }
  if (!isApplicationStatus(status)) {
    failure(back, `"${status}" is not an application status.`);
  }

  let application: Application | null = null;
  let error: string | null = null;
  try {
    application = await sendJson<Application>("/api/applications", "POST", {
      jobId,
      status,
      note: text(form, "note") || undefined,
    });
  } catch (cause) {
    error = apiMessage(cause);
  }

  if (error || !application) {
    failure(back, error ?? "The API did not return the application.");
  }

  revalidatePath(back);
  revalidatePath("/applications");
  revalidatePath("/");
  success(`/applications/${application.id}`, `Tracking as ${statusLabel(status)}.`);
}

/** Saves the whole edit form: the status, the note that explains it, and every detail field. */
export async function saveApplication(form: FormData) {
  const id = Number(text(form, "id"));
  const status = text(form, "status") ?? "";
  const back = `/applications/${id}`;

  if (!Number.isInteger(id) || id <= 0) {
    failure("/applications", "That form did not carry an application to save.");
  }
  if (!isApplicationStatus(status)) {
    failure(back, `"${status}" is not an application status.`);
  }

  let error: string | null = null;
  try {
    await sendJson<Application>(`/api/applications/${id}`, "PATCH", {
      status,
      note: text(form, "note") || undefined,
      ...details(form),
    });
  } catch (cause) {
    error = apiMessage(cause);
  }

  if (error) {
    failure(back, error);
  }

  revalidatePath(back);
  revalidatePath("/applications");
  revalidatePath("/");
  success(back, "Saved.");
}

/**
 * Moves one application's status from the list, without opening it.
 *
 * The list is where triage happens, and triage is almost always a status change and nothing
 * else, so this sends the status alone and leaves every other field untouched.
 */
export async function setApplicationStatus(form: FormData) {
  const id = Number(text(form, "id"));
  const status = text(form, "status") ?? "";
  const back = text(form, "returnTo") || "/applications";

  if (!Number.isInteger(id) || id <= 0) {
    failure("/applications", "That form did not carry an application to move.");
  }
  if (!isApplicationStatus(status)) {
    failure(back, `"${status}" is not an application status.`);
  }

  let error: string | null = null;
  try {
    await sendJson<Application>(`/api/applications/${id}`, "PATCH", { status });
  } catch (cause) {
    error = apiMessage(cause);
  }

  if (error) {
    failure(back, error);
  }

  revalidatePath("/applications");
  revalidatePath(`/applications/${id}`);
  revalidatePath("/");
  success(back, `Moved to ${statusLabel(status)}.`);
}
