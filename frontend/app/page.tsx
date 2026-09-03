import Link from "next/link";
import { BackendError } from "./components/BackendError";
import { StatusBadge } from "./components/Badges";
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
      <>
        <h1>Dashboard</h1>
        <BackendError error={error} />
      </>
    );
  }

  const open = dashboard.totalJobs - (dashboard.jobsByLifecycleState.CLOSED ?? 0);
  const activeApplications = Object.entries(dashboard.applicationsByStatus)
    .filter(([status]) => ["INTERESTED", "READY_TO_APPLY", "APPLIED", "INTERVIEW"].includes(status))
    .reduce((total, [, count]) => total + count, 0);

  return (
    <>
      <h1>Dashboard</h1>
      <p className="page-subtitle">
        Profile {dashboard.profileCode ?? "none"} - {dashboard.sourceCount} sources registered
      </p>

      <div className="card-grid">
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
      </div>

      <h2>Lifecycle</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>State</th>
              <th className="numeric">Jobs</th>
              <th>Meaning</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(dashboard.jobsByLifecycleState).map(([state, count]) => (
              <tr key={state}>
                <td>{state.toLowerCase()}</td>
                <td className="numeric">{count}</td>
                <td className="muted">{LIFECYCLE_MEANING[state] ?? ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Applications by status</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Status</th>
              <th className="numeric">Count</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(dashboard.applicationsByStatus).map(([status, count]) => (
              <tr key={status}>
                <td>
                  <StatusBadge status={status} />
                </td>
                <td className="numeric">{count}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Recent ingestion runs</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Started</th>
              <th>Source</th>
              <th>Status</th>
              <th className="numeric">Received</th>
              <th className="numeric">New</th>
              <th className="numeric">Duplicates</th>
              <th className="numeric">Closed</th>
              <th className="numeric">Failures</th>
            </tr>
          </thead>
          <tbody>
            {dashboard.recentRuns.length === 0 ? (
              <tr>
                <td colSpan={8} className="muted">
                  No ingestion run has been recorded yet.
                </td>
              </tr>
            ) : (
              dashboard.recentRuns.map((run) => (
                <tr key={run.runUuid}>
                  <td>{formatDateTime(run.startedAt)}</td>
                  <td>{run.sourceCode}</td>
                  <td>
                    <StatusBadge status={run.status} />
                  </td>
                  <td className="numeric">{run.recordsReceived}</td>
                  <td className="numeric">{run.newJobs}</td>
                  <td className="numeric">{run.duplicates}</td>
                  <td className="numeric">{run.jobsClosed}</td>
                  <td className="numeric">{run.failures}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <p className="pagination">
        <Link href="/search-runs">All search runs</Link>
      </p>
    </>
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

function Stat({ label, value, sub }: { label: string; value: number | string; sub: string }) {
  return (
    <div className="card">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
      <div className="sub">{sub}</div>
    </div>
  );
}
