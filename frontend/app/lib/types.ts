export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Job {
  id: number;
  companyId: number;
  companyName: string;
  companyRiskLevel: string;
  title: string;
  roleFamily: string | null;
  sector: string | null;
  seniorityBucket: string | null;
  yearsExperienceMin: number | null;
  yearsExperienceMax: number | null;
  degreeRequired: string | null;
  employmentType: string | null;
  remotePolicy: string | null;
  lifecycleState: string;
  locationRaw: string | null;
  locationCity: string | null;
  locationRegion: string | null;
  sourceCount: number;
  careerValueScore: number | null;
  candidateFitScore: number | null;
  applicationPriorityScore: number | null;
  firstSeenAt: string;
  lastSeenAt: string;
  lastVerifiedAt: string | null;
  postedAt: string | null;
  deadlineAt: string | null;
  deadlineOpenEnded: boolean;
  closedAt: string | null;
  closedReason: string | null;
  applyUrl: string | null;
}

export interface JobSource {
  id: number;
  sourceCode: string;
  externalId: string | null;
  externalKey: string;
  sourceUrl: string | null;
  applyUrl: string | null;
  active: boolean;
  firstSeenAt: string;
  lastSeenAt: string;
  lastVerifiedAt: string | null;
  closedAt: string | null;
  matchMethod: string;
  matchConfidence: number;
  matchEvidence: string;
  manuallyCorrected: boolean;
  latestSnapshotId: number | null;
}

export interface FieldEvidence {
  fieldName: string;
  fieldValue: string | null;
  confidence: number;
  extractionMethod: string;
  evidenceText: string | null;
  evidenceSnapshotId: number | null;
  extractedAt: string;
}

export interface SkillEvidence {
  skillSlug: string;
  category: string;
  requirementLevel: string;
  confidence: number;
  evidenceText: string | null;
  evidenceSnapshotId: number | null;
}

export interface JobIntelligence {
  extractorVersion: string;
  roleFamily: string | null;
  seniorityBucket: string | null;
  seniorityLabel: string | null;
  yearsExperienceMin: number | null;
  yearsExperienceMax: number | null;
  degreeRequired: string | null;
  degreePreferred: string | null;
  employmentType: string | null;
  remotePolicy: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  salaryPeriod: string | null;
  responsibilities: string[];
  requirements: string[];
  preferredRequirements: string[];
  extractedAt: string;
  fields: FieldEvidence[];
  skills: SkillEvidence[];
}

export interface JobScore {
  scoreKind: string;
  profileCode: string | null;
  score: number;
  scoreVersion: string;
  confidence: number;
  componentScores: string;
  explanation: string;
  computedAt: string;
}

export interface Snapshot {
  id: number;
  sourceCode: string;
  externalId: string | null;
  sourceUrl: string | null;
  fetchedAt: string;
  contentHash: string;
  rawTitle: string | null;
  rawCompany: string | null;
  rawLocation: string | null;
  rawExperience: string | null;
  rawEducation: string | null;
  rawDeadline: string | null;
}

export interface Verification {
  id: number;
  verifiedAt: string;
  method: string;
  outcome: string;
  httpStatus: number | null;
  snapshotId: number | null;
  detail: string | null;
}

export interface LifecycleEvent {
  id: number;
  fromState: string | null;
  toState: string;
  reasonCode: string;
  occurredAt: string;
  verificationId: number | null;
}

export interface JobDetail {
  job: Job;
  description: string | null;
  intelligence: JobIntelligence | null;
  scores: JobScore[];
  sources: JobSource[];
  snapshots: Snapshot[];
  verifications: Verification[];
  lifecycle: LifecycleEvent[];
}

export interface Company {
  id: number;
  canonicalName: string;
  normalizedName: string;
  websiteDomain: string | null;
  countryCode: string | null;
  industry: string | null;
  companyType: string | null;
  foundedOn: string | null;
  employeeCount: number | null;
  riskLevel: string;
  riskAssessedAt: string | null;
  openJobCount: number;
  aliases: string[];
  identifiers: { type: string; value: string; observedAt: string }[];
  metrics: {
    key: string;
    numericValue: number | null;
    textValue: string | null;
    unit: string | null;
    effectiveDate: string | null;
    observedAt: string;
    evidenceUrl: string | null;
    confidence: number | null;
  }[];
  riskReasons: {
    riskLevel: string;
    reasonCode: string;
    reasonDetail: string;
    assessedAt: string;
    evidence: string;
  }[];
}

export interface SourceRegistryEntry {
  id: number;
  code: string;
  displayName: string;
  adapterKind: string;
  runtimeAvailable: boolean;
  trustTier: number;
  stableExternalId: boolean;
  providesFullDescription: boolean;
  enabled: boolean;
  baseUrl: string | null;
  adapterRegistered: boolean;
}

export interface SourceHealthEntry {
  sourceCode: string;
  lastSuccessAt: string | null;
  lastFailureAt: string | null;
  lastAttemptAt: string | null;
  lastStatus: string | null;
  lastHttpStatus: number | null;
  lastError: string | null;
  lastLatencyMs: number | null;
  rollingLatencyMs: number | null;
  recordsLastRun: number;
  consecutiveFailures: number;
  totalSuccesses: number;
  totalFailures: number;
  rateLimitEvents: number;
  rateLimitedUntil: string | null;
  circuitState: string;
  circuitOpenedAt: string | null;
  updatedAt: string;
}

export interface SearchRun {
  id: number;
  runUuid: string;
  sourceCode: string;
  triggerKind: string;
  queryText: string | null;
  status: string;
  startedAt: string;
  completedAt: string | null;
  durationMs: number | null;
  recordsReceived: number;
  recordsNormalized: number;
  newJobs: number;
  updatedJobs: number;
  duplicates: number;
  failures: number;
  rateLimitEvents: number;
  jobsClosed: number;
  errorSummary: string | null;
  collector: string | null;
  failureDetails: {
    id: number;
    stage: string;
    reasonCode: string;
    message: string;
    externalId: string | null;
    sourceUrl: string | null;
    occurredAt: string;
  }[];
}

export interface Application {
  id: number;
  jobId: number;
  jobTitle: string | null;
  companyName: string | null;
  profileId: number;
  profileCode: string | null;
  status: string;
  appliedAt: string | null;
  resumeVersion: string | null;
  coverLetterVersion: string | null;
  contactName: string | null;
  contactEmail: string | null;
  referral: string | null;
  interviewStage: string | null;
  interviewNotes: string | null;
  followUpAt: string | null;
  rejectionAt: string | null;
  offerAt: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  history: {
    fromStatus: string | null;
    toStatus: string;
    changedAt: string;
    note: string | null;
  }[];
}

export interface Dashboard {
  totalJobs: number;
  jobsByLifecycleState: Record<string, number>;
  jobsDiscoveredLastSevenDays: number;
  juniorAccessibleOpenJobs: number;
  sourceCount: number;
  healthySources: number;
  openCircuits: number;
  pendingMergeReviews: number;
  profileCode: string | null;
  applicationsByStatus: Record<string, number>;
  recentRuns: {
    runUuid: string;
    sourceCode: string;
    status: string;
    startedAt: string;
    recordsReceived: number;
    newJobs: number;
    duplicates: number;
    failures: number;
    jobsClosed: number;
  }[];
}
