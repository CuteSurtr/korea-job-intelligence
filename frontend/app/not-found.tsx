import Link from "next/link";
import { Container } from "./components/ui";

export default function NotFound() {
  return (
    <Container width="prose">
      <h1 className="text-2xl font-semibold tracking-tight">Not found</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        The API has no record with that id. It may have been merged into another row as a
        duplicate, or never existed.
      </p>
      <p className="text-muted-foreground">
        <Link href="/jobs">Browse jobs</Link> - <Link href="/companies">companies</Link> -{" "}
        <Link href="/search-runs">search runs</Link>
      </p>
    </Container>
  );
}
