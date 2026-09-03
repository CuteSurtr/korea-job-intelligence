import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { OrUnknown, RiskBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import type { Company } from "../lib/types";

export const dynamic = "force-dynamic";

export default async function CompaniesPage() {
  let companies: Company[];
  try {
    companies = await fetchJson<Company[]>("/api/companies", { size: 200 });
  } catch (error) {
    return (
      <>
        <h1>Companies</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <>
      <h1>Companies</h1>
      <p className="page-subtitle">
        {companies.length} employers resolved from the postings seen so far.
      </p>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Normalized</th>
              <th>Country</th>
              <th>Industry</th>
              <th className="numeric">Employees</th>
              <th>Risk</th>
              <th className="numeric">Open jobs</th>
            </tr>
          </thead>
          <tbody>
            {companies.length === 0 ? (
              <tr>
                <td colSpan={7} className="muted">
                  No company has been resolved yet.
                </td>
              </tr>
            ) : (
              companies.map((company) => (
                <tr key={company.id}>
                  <td>
                    <Link href={`/companies/${company.id}`}>{company.canonicalName}</Link>
                  </td>
                  <td className="muted">{company.normalizedName}</td>
                  <td className="muted">
                    <OrUnknown value={company.countryCode} />
                  </td>
                  <td className="muted">
                    <OrUnknown value={company.industry} />
                  </td>
                  <td className="numeric">
                    <OrUnknown value={company.employeeCount} />
                  </td>
                  <td>
                    <RiskBadge level={company.riskLevel} />
                  </td>
                  <td className="numeric">{company.openJobCount}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}
