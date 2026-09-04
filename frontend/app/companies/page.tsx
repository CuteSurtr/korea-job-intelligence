import Link from "next/link";
import { BackendError } from "../components/BackendError";
import { RiskBadge } from "../components/Badges";
import { fetchJson } from "../lib/api";
import type { Company } from "../lib/types";
import {
  Container,
  OrUnknown,
  TableWrap,
  Td,
  Th,
} from "../components/ui";

export const dynamic = "force-dynamic";

export default async function CompaniesPage() {
  let companies: Company[];
  try {
    companies = await fetchJson<Company[]>("/api/companies", { size: 200 });
  } catch (error) {
    return (
      <>
        <h1 className="text-2xl font-semibold tracking-tight">Companies</h1>
        <BackendError error={error} />
      </>
    );
  }

  return (
    <Container width="table">
      <h1 className="text-2xl font-semibold tracking-tight">Companies</h1>
      <p className="mt-1 mb-6 text-sm text-muted-foreground">
        {companies.length} employers resolved from the postings seen so far.
      </p>

      <TableWrap>
          <thead>
            <tr>
              <Th>Name</Th>
              <Th>Normalized</Th>
              <Th>Country</Th>
              <Th>Industry</Th>
              <Th align="right">Employees</Th>
              <Th>Risk</Th>
              <Th align="right">Open jobs</Th>
            </tr>
          </thead>
          <tbody>
            {companies.length === 0 ? (
              <tr>
                <Td colSpan={7} className="text-muted-foreground">
                  No company has been resolved yet.
                </Td>
              </tr>
            ) : (
              companies.map((company) => (
                <tr key={company.id}>
                  <Td>
                    <Link href={`/companies/${company.id}`}>{company.canonicalName}</Link>
                  </Td>
                  <Td className="text-muted-foreground">{company.normalizedName}</Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={company.countryCode} />
                  </Td>
                  <Td className="text-muted-foreground">
                    <OrUnknown value={company.industry} />
                  </Td>
                  <Td align="right">
                    <OrUnknown value={company.employeeCount} />
                  </Td>
                  <Td>
                    <RiskBadge level={company.riskLevel} />
                  </Td>
                  <Td align="right">{company.openJobCount}</Td>
                </tr>
              ))
            )}
          </tbody>
        </TableWrap>
    </Container>
  );
}
