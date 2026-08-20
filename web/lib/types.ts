// Types mirroring the Spring Boot JSON contract exactly as agreed.
// See CLAUDE.md task brief / README.md for the source-of-truth shapes.

export interface TokenResponse {
  token: string;
  expiresAt: string; // ISO datetime
  displayName: string;
}

export interface IntentSpec {
  durationMin: number;
  dayFrom: string; // ISO date
  dayTo: string; // ISO date
  timeOfDay: string; // e.g. "EVENING"
  hardConstraints: string[];
  softConstraints: string[];
  resourceType: string;
  partySize: number;
}

export interface Term {
  key: string;
  label: string;
  delta: number;
  satisfied: boolean;
}

/**
 * Assumption (backend not live yet — confirm against the real API once it
 * ships): the brief only shows `"relaxed": []` in the example, with no
 * populated shape. RelaxationNotice must render each relaxed entry's
 * `detail`, exactly like top-level `relaxationTrail` entries do, so we type
 * a suggestion's `relaxed` array as the same shape as a relaxation trail
 * entry. If the real API instead sends bare strings here, `lib/api.ts` is
 * the only place that needs to change (see `normalizeRelaxed`).
 */
export interface RelaxationTrailEntry {
  action: string;
  detail: string;
  droppedKeys: string[];
}

export interface Suggestion {
  resourceId: number;
  resourceName: string;
  facilityName: string;
  start: string; // ISO datetime
  end: string; // ISO datetime
  score: number;
  reason: string;
  price: string;
  terms: Term[];
  relaxed: RelaxationTrailEntry[];
}

export interface IntentSuggestResponse {
  spec: IntentSpec;
  parserUsed: string;
  suggestions: Suggestion[];
  relaxationTrail: RelaxationTrailEntry[];
}

export interface IntentSuggestRequest {
  text: string;
  partySize: number;
}

export interface BookRequest {
  resourceId: number;
  start: string;
  end: string;
  partySize: number;
  paymentMethod: string;
}

export interface BookResponse {
  reservationId: number;
  status: string;
  totalAmount: string;
  message: string;
}

/** Where a piece of data on screen actually came from — always shown to the user. */
export type DataSource = "live" | "fixture" | "fixture-fallback";
