const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export class BackendUnavailableError extends Error {
  constructor(path: string, cause: unknown) {
    super(`Backend request failed for ${path}: ${String(cause)}`);
    this.name = "BackendUnavailableError";
  }
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
    const body = await response.text();
    throw new Error(`Backend returned ${response.status} for ${path}: ${body.slice(0, 300)}`);
  }
  return (await response.json()) as T;
}

export function backendUrl(): string {
  return BACKEND_URL;
}
