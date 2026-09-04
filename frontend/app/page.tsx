import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { BackendError } from "./components/BackendError";
import { StatusBadge } from "./components/Badges";
import {
  Card,
  CardHeader,
  Container,
  Empty,
  PageHeader,
  Stat,
  TableWrap,
  Td,
  Th,
} from "./components/ui";
import { fetchJson } from "./lib/api";
import { formatDateTime } from "./lib/format";
import type { Dashboard } from "./lib/types";

export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  let dashboard: Dashboard;
  try {
    dashboard = await fetchJson<Dashboard>("/api/dashboard");
  } catch (error) {
    return (
      <Container>
        <PageHeader title="Dashboard" />
        <BackendError error={error} />
      </Container>
    );
  }

  const open = dashboard.totalJobs - (dashboard.jobsByLifecycleState.CLOSED ?? 0);
  const activeApplications = Object.entries(dashboard.applicationsByStatus)
    .filter(([status]) => ["INTERESTED", "READY_TO_APPLY", "APPLIED", "INTERVIEW"].includes(status))
    .reduce((total, [, count]) => total + count, 0);

  return (
    <Container>
      <PageHeader
        title="Dashboard"
        subtitle={`Profile ${dashboard.profileCode ?? "none"} · ${dashboard.sourceCount} sources registered`}
      />

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        <Stat label="Open jobs" value={open} sub={`${dashboard.totalJobs} tracked in total`} />
        <Stat
          label="Junior accessible"
          value={dashboard.juniorAccessibleOpenJobs}
          sub="seniority A or B and open"
        />
        <Stat
          label="Discovered this week"
          value={dashboard.jobsDiscoveredLastSevenDays}
          sub="first seen in the last 7 days"
        />
        <Stat
          label="Healthy sources"
          value={`${dashboard.healthySources} / ${dashboard.sourceCount}`}
          sub={
            dashboard.openCircuits > 0
              ? `${dashboard.openCircuits} circuit(s) open`
              : "no open circuits"
          }
        />
        <Stat
          label="Active applications"
          value={activeApplications}
          sub="interested through interview"
        />
        <Stat
          label="Pairs awaiting review"
          value={dashboard.pendingMergeReviews}
          sub="too similar to ignore, too weak to merge"
        />
      </div>

      <div className="mt-6 grid items-start gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Lifecycle" hint="where every tracked posting stands" />
          <TableWrap>
            <thead>
              <tr>
                <Th>State</Th>
                <Th align="right">Jobs</Th>
                <Th>Meaning</Th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {Object.entries(dashboard.jobsByLifecycleState).map(([state, count]) => (
                <tr key={state}>
                  <Td>{state.toLowerCase()}</Td>
                  <Td align="right">{count}</Td>
                  <Td className="whitespace-normal text-muted-foreground">
                    {LIFECYCLE_MEANING[state] ?? ""}
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
        </Card>

        <Card>
          <CardHeader title="Applications by status" />
          <TableWrap>
            <thead>
              <tr>
                <Th>Status</Th>
                <Th align="right">Count</Th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {Object.entries(dashboard.applicationsByStatus).map(([status, count]) => (
                <tr key={status}>
                  <Td>
                    <StatusBadge status={status} />
                  </Td>
                  <Td align="right" className={count === 0 ? "text-muted-foreground/60" : ""}>
                    {count}
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableWrap>
        </Card>
      </div>

      <div className="mt-6">
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="text-sm font-semibold tracking-tight">Recent ingestion runs</h2>
          <Link
            href="/search-runs"
            className="inline-flex items-center gap-1 text-sm text-brand hover:underline"
          >
            All search runs
            <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
          </Link>
        </div>
        <TableWrap>
          <thead>
            <tr>
              <Th>Started</Th>
              <Th>Source</Th>
              <Th>Status</Th>
              <Th align="right">Received</Th>
              <Th align="right">New</Th>
              <Th align="right">Duplicates</Th>
              <Th align="right">Closed</Th>
              <Th align="right">Failures</Th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {dashboard.recentRuns.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <Empty>No ingestion run has been recorded yet.</Empty>
                </td>
              </tr>
            ) : (
              dashboard.recentRuns.map((run) => (
                <tr key={run.runUuid}>
                  <Td className="text-muted-foreground">{formatDateTime(run.startedAt)}</Td>
                  <Td>{run.sourceCode}</Td>
                  <Td>
                    <StatusBadge status={run.status} />
                  </Td>
                  <Td align="right">{run.recordsReceived}</Td>
                  <Td align="right">{run.newJobs}</Td>
                  <Td align="right">{run.duplicates}</Td>
                  <Td align="right">{run.jobsClosed}</Td>
                  <Td align="right" className={run.failures > 0 ? "text-bad" : ""}>
                    {run.failures}
                  </Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>
      </div>
    </Container>
  );
}

const LIFECYCLE_MEANING: Record<string, string> = {
  DISCOVERED: "seen once, not yet corroborated",
  ACTIVE: "present in the latest successful run of a source",
  UNVERIFIED: "not confirmed recently, no source reported it absent",
  STALE: "past the staleness horizon without confirmation",
  CLOSED: "closed on recorded evidence",
  REOPENED: "seen again after being closed",
};
