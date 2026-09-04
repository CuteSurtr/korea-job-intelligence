import Link from "next/link";

export default function NotFound() {
  return (
    <>
      <h1>Not found</h1>
      <p className="page-subtitle">
        The API has no record with that id. It may have been merged into another row as a
        duplicate, or never existed.
      </p>
      <p className="muted">
        <Link href="/jobs">Browse jobs</Link> - <Link href="/companies">companies</Link> -{" "}
        <Link href="/search-runs">search runs</Link>
      </p>
    </>
  );
}
