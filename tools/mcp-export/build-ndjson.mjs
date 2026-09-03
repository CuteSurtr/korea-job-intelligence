import { readFileSync, writeFileSync } from "node:fs";
import { argv, exit } from "node:process";

const MAPPERS = {
  pathsdog: mapPathsdog,
  jobkorea: mapKoreanBoard,
  saramin: mapKoreanBoard,
  linkedin: mapLinkedin,
  indeed: mapIndeed,
  freehire: mapFreehire,
};

function fail(message) {
  console.error(message);
  exit(1);
}

function text(value) {
  if (value === undefined || value === null) {
    return null;
  }
  const trimmed = String(value).trim();
  return trimmed.length === 0 ? null : trimmed;
}

function base(record, sourceCode, fetchedAt, collector) {
  return {
    sourceCode,
    externalId: null,
    sourceUrl: null,
    originalApplyUrl: null,
    fetchedAt,
    postedAt: null,
    rawTitle: null,
    rawCompany: null,
    rawLocation: null,
    rawDescription: null,
    rawRequirements: null,
    rawEmploymentType: null,
    rawExperience: null,
    rawEducation: null,
    rawDeadline: null,
    rawRemotePolicy: null,
    rawSkills: [],
    companyIdentifiers: {},
    collector,
    rawPayload: record,
  };
}

function mapPathsdog(record, fetchedAt, collector) {
  return {
    ...base(record, "pathsdog", fetchedAt, collector),
    externalId: text(record.id) === null ? null : `pathsdog:${record.id}`,
    sourceUrl: text(record.pathsdogUrl),
    originalApplyUrl: text(record.originalUrl),
    rawTitle: text(record.title),
    rawCompany: text(record.company),
    rawLocation: text(record.location),
    rawEmploymentType: text(record.employmentType),
    rawExperience: text(record.experience),
    rawDeadline: text(record.deadline),
    rawRemotePolicy: text(record.remotePolicy),
    rawDescription: text(record.description),
    rawSkills: Array.isArray(record.skills) ? record.skills : [],
    companyIdentifiers: text(record.company)
      ? { pathsdog_company: text(record.company) }
      : {},
  };
}

function mapKoreanBoard(record, fetchedAt, collector, sourceCode) {
  const externalId = text(record.externalId) ?? extractBoardId(record.url);
  return {
    ...base(record, sourceCode, fetchedAt, collector),
    externalId: externalId === null ? null : `${sourceCode}:${externalId}`,
    sourceUrl: text(record.url),
    originalApplyUrl: text(record.originalUrl) ?? text(record.url),
    rawTitle: text(record.title),
    rawCompany: text(record.company),
    rawLocation: text(record.location),
    rawExperience: text(record.experience),
    rawEducation: text(record.education),
    rawDeadline: text(record.deadline),
    companyIdentifiers: text(record.company)
      ? { [`${sourceCode}_company`]: text(record.company) }
      : {},
  };
}

function extractBoardId(url) {
  if (!url) {
    return null;
  }
  const jobkorea = String(url).match(/GI_Read\/(\d+)/);
  if (jobkorea) {
    return jobkorea[1];
  }
  const saramin = String(url).match(/rec_idx=(\d+)/);
  if (saramin) {
    return saramin[1];
  }
  return null;
}

function mapLinkedin(record, fetchedAt, collector) {
  return {
    ...base(record, "linkedin", fetchedAt, collector),
    externalId: text(record.id) === null ? null : `linkedin:${record.id}`,
    sourceUrl: text(record.url),
    originalApplyUrl: text(record.url),
    postedAt: record.posted_date ? `${record.posted_date}T00:00:00Z` : null,
    rawTitle: text(record.title),
    rawCompany: text(record.company),
    rawLocation: text(record.location),
    companyIdentifiers: text(record.company_url)
      ? { linkedin_company_url: text(record.company_url) }
      : {},
  };
}

function mapIndeed(record, fetchedAt, collector) {
  return {
    ...base(record, "indeed", fetchedAt, collector),
    externalId: null,
    sourceUrl: text(record.url),
    originalApplyUrl: text(record.url),
    postedAt: record.postedOn ? new Date(record.postedOn).toISOString() : null,
    rawTitle: text(record.title),
    rawCompany: text(record.company),
    rawLocation: text(record.location),
    rawEmploymentType: text(record.jobType),
  };
}

function mapFreehire(record, fetchedAt, collector) {
  const skills = Array.isArray(record.skills) ? record.skills : [];
  const enrichment = record.enrichment ?? {};
  return {
    ...base(record, "freehire", fetchedAt, collector),
    externalId: text(record.external_id) === null ? null : `freehire:${record.external_id}`,
    sourceUrl: text(record.url),
    originalApplyUrl: stripTracking(text(record.url)),
    postedAt: record.posted_at ? `${record.posted_at}T00:00:00Z` : null,
    rawTitle: text(record.title),
    rawCompany: text(record.company),
    rawLocation: text(record.location),
    rawDescription: text(record.description),
    rawEmploymentType: text(enrichment.employment_type),
    rawExperience:
      enrichment.experience_years_min === undefined || enrichment.experience_years_min === null
        ? null
        : `${enrichment.experience_years_min}+ years of experience`,
    rawRemotePolicy: text(record.work_mode),
    rawSkills: skills,
    companyIdentifiers: buildFreehireIdentifiers(record),
  };
}

function buildFreehireIdentifiers(record) {
  const identifiers = {};
  if (text(record.company_slug)) {
    identifiers.freehire_company = text(record.company_slug);
  }
  const externalId = text(record.external_id);
  const source = text(record.source);
  if (externalId && source && externalId.includes(":")) {
    const boardToken = externalId.split(":")[0];
    if (source === "greenhouse") {
      identifiers.greenhouse_board = boardToken;
    } else if (source === "ashby") {
      identifiers.ashby_board = boardToken;
    } else if (source === "lever") {
      identifiers.lever_board = boardToken;
    }
  }
  return identifiers;
}

function stripTracking(url) {
  if (!url) {
    return null;
  }
  try {
    const parsed = new URL(url);
    for (const key of [...parsed.searchParams.keys()]) {
      if (key.startsWith("utm_")) {
        parsed.searchParams.delete(key);
      }
    }
    return parsed.toString();
  } catch {
    return url;
  }
}

function main() {
  const input = argv[2];
  const output = argv[3];
  if (!input || !output) {
    fail("usage: node build-ndjson.mjs <collected.json> <out.ndjson>");
  }

  const payload = JSON.parse(readFileSync(input, "utf8"));
  const provider = payload.provider;
  const mapper = MAPPERS[provider];
  if (!mapper) {
    fail(`unknown provider "${provider}"; expected one of ${Object.keys(MAPPERS).join(", ")}`);
  }

  const fetchedAt = payload.fetchedAt ?? new Date().toISOString();
  const collector = payload.collector ?? "mcp-export";
  const records = payload.records ?? [];

  const lines = records
    .map((record) => mapper(record, fetchedAt, collector, provider))
    .filter((record) => record.rawTitle && record.rawCompany)
    .map((record) => JSON.stringify(record));

  writeFileSync(output, lines.join("\n") + "\n", "utf8");
  const skipped = records.length - lines.length;
  console.log(
    `${provider}: ${lines.length} records written to ${output}` +
      (skipped > 0 ? ` (${skipped} skipped for missing title or company)` : ""),
  );
}

main();
