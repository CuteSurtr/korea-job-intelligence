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
