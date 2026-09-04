import { canWrite } from "../lib/api";

/**
 * Says the console cannot write, before someone fills in a form that would only fail.
 *
 * The API requires the shared token on every write. A console started without it can still
 * show everything; it just cannot change anything, and saying so up front beats a rejection
 * after the typing is done.
 */
export function WriteDisabled() {
  if (canWrite()) {
    return null;
  }
  return (
    <div className="flash flash-warn" role="note">
      <strong>This console cannot write.</strong> The API requires an internal token on every
      change, and this console was started without one. Set <code>INTERNAL_API_TOKEN</code> to
      the same value the API uses and restart it.
    </div>
  );
}

/**
 * The outcome of the last write, carried back on the URL.
 *
 * Server actions redirect after they finish, so the result has to survive a navigation. A
 * query parameter does that without any client-side state, and it keeps the forms working
 * with no JavaScript at all.
 */
export function Flash({ saved, error }: { saved?: string; error?: string }) {
  if (error) {
    return (
      <div className="flash flash-error" role="alert">
        <strong>That did not save.</strong> {error}
      </div>
    );
  }
  if (saved) {
    return (
      <div className="flash flash-ok" role="status">
        {saved}
      </div>
    );
  }
  return null;
}

/** Reads one string out of the searchParams shape App Router provides. */
export function param(
  params: Record<string, string | string[] | undefined>,
  key: string,
): string | undefined {
  const value = params[key];
  const single = Array.isArray(value) ? value[0] : value;
  return single === "" ? undefined : single;
}
