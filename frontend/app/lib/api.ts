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

export function backendUrl(): string {
  return BACKEND_URL;
}
