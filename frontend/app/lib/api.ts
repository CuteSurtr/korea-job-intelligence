const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

/** The backend could not be reached at all: wrong URL, not running, DNS, TLS, timeout. */
export class BackendUnavailableError extends Error {
  constructor(path: string, cause: unknown) {
    super(`Backend request failed for ${path}: ${String(cause)}`);
    this.name = "BackendUnavailableError";
  }
}

/**
 * The backend answered, but not with a success. Distinct from
 * {@link BackendUnavailableError} because a 404 means the row does not exist, which is a
 * normal answer, not an outage.
 */
export class BackendRequestError extends Error {
  readonly status: number;
  readonly path: string;
  readonly body: string;

  constructor(path: string, status: number, body: string) {
    super(`Backend returned ${status} for ${path}: ${body.slice(0, 300)}`);
    this.name = "BackendRequestError";
    this.status = status;
    this.path = path;
    this.body = body;
  }
}

/** True when the backend answered that the resource does not exist. */
export function isNotFound(error: unknown): boolean {
  return error instanceof BackendRequestError && error.status === 404;
}

export async function fetchJson<T>(
  path: string,
  params?: Record<string, string | number | boolean | undefined | null>,
): Promise<T> {
  const url = new URL(path, BACKEND_URL);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  let response: Response;
  try {
    response = await fetch(url, { cache: "no-store" });
  } catch (cause) {
    throw new BackendUnavailableError(path, cause);
  }

  if (!response.ok) {
    throw new BackendRequestError(path, response.status, await response.text());
  }
  return (await response.json()) as T;
}

/**
 * Sends a body to the API and returns the parsed answer.
 *
 * Writes are never cached and never retried: a retry on a status change would record a second
 * entry in the application's history, which is the one thing that history is for.
 */
export async function sendJson<T>(
  path: string,
  method: "POST" | "PATCH",
  body: unknown,
): Promise<T> {
  const url = new URL(path, BACKEND_URL);

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      cache: "no-store",
    });
  } catch (cause) {
    throw new BackendUnavailableError(path, cause);
  }

  if (!response.ok) {
    throw new BackendRequestError(path, response.status, await response.text());
  }
  return (await response.json()) as T;
}

/**
 * The sentence the API meant for a person, when it sent one.
 *
 * The error bodies carry {error, message}; anything else falls back to the raw text so a
 * failure is never reported as an empty banner.
 */
export function apiMessage(error: unknown): string {
  if (error instanceof BackendRequestError) {
    try {
      const parsed = JSON.parse(error.body) as { message?: string; error?: string };
      if (parsed.message) {
        return parsed.message;
      }
      if (parsed.error) {
        return parsed.error;
      }
    } catch {
      // not JSON; fall through to the raw body
    }
    return error.body.slice(0, 300) || `The API answered ${error.status}.`;
  }
  if (error instanceof BackendUnavailableError) {
    return `The API at ${BACKEND_URL} could not be reached.`;
  }
  return error instanceof Error ? error.message : String(error);
}

export function backendUrl(): string {
  return BACKEND_URL;
}
