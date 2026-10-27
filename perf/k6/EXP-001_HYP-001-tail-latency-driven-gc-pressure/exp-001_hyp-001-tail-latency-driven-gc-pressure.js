// HYP-001 / EXP-001: repeatable GET baseline for JVM allocation vs tail latency.
// Run after starting shipment-service with the "dev" Spring profile.
// Example:
//   RATE=200 SEED_COUNT=1000 WARMUP=2m MEASURE=10m k6 run \
//     perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp-001_hyp-001-tail-latency-driven-gc-pressure.js
//
// Setup POSTs are excluded from the measured GET load.
// For comparable A/B runs, keep RATE, SEED_COUNT, environment, and JVM settings identical.

import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';

const requestErrors = new Counter('experiment_request_errors');
const RATE = Number(__ENV.RATE || 200);
const SEED_COUNT = Number(__ENV.SEED_COUNT || 1000);
const WARMUP = __ENV.WARMUP || '2m';
const MEASURE = __ENV.MEASURE || '10m';
const MODE = __ENV.MODE || 'baseline';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_URL = __ENV.AUTH_URL || `${BASE_URL}/dev/token?sub=123`;
const PARCEL_URL = `${BASE_URL}/api/shipping/v1/parcels`;

if (!Number.isInteger(SEED_COUNT) || SEED_COUNT <= 0) {
  throw new Error('SEED_COUNT must be a positive integer');
}
if (!Number.isFinite(RATE) || RATE <= 0) {
  throw new Error('RATE must be positive');
}

export const options = {
  tags: { hypothesis: 'HYP-001', experiment: 'EXP-001', mode: MODE },
  scenarios: {
    warmup: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: WARMUP,
      preAllocatedVUs: Math.max(10, Math.ceil(RATE)),
      maxVUs: Math.max(20, Math.ceil(RATE * 2)),
      gracefulStop: '0s',
      tags: { phase: 'warmup' },
    },
    measure: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: MEASURE,
      startTime: WARMUP,
      preAllocatedVUs: Math.max(10, Math.ceil(RATE)),
      maxVUs: Math.max(20, Math.ceil(RATE * 2)),
      tags: { phase: 'measure' },
    },
  },
  thresholds: {
    'http_req_failed{phase:measure}': ['rate==0'],
    'experiment_request_errors{phase:measure}': ['count==0'],
  },
};

export function setup() {
  const tokenResponse = http.get(AUTH_URL);
  if (tokenResponse.status !== 200) {
    fail(`Cannot obtain dev token: HTTP ${tokenResponse.status}`);
  }
  const token = tokenResponse.json('access_token');
  if (!token) fail('Dev token response has no access_token');

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
  const trackingCodes = [];

  for (let i = 0; i < SEED_COUNT; i++) {
    const payload = JSON.stringify({
      fromLocker: 'WAW-01',
      toLocker: 'BER-01',
      weightGrams: 1000,
      size: 'M',
      clientOperationId: null,
    });
    const response = http.post(PARCEL_URL, payload, { headers, tags: { phase: 'setup', endpoint: 'createParcel' } });
    if (response.status !== 201) {
      fail(`Seeding parcel ${i + 1}/${SEED_COUNT} failed: HTTP ${response.status} ${response.body}`);
    }
    const code = response.json('trackingCode');
    if (!code) fail(`Seeding parcel ${i + 1} returned no trackingCode`);
    trackingCodes.push(code);
  }

  // Probe actual read path before the timed load (a 404 is NOT a successful GET).
  const probe = http.get(`${PARCEL_URL}/${encodeURIComponent(trackingCodes[0])}`, {
    headers, tags: { phase: 'setup', endpoint: 'getParcel' },
  });
  if (probe.status !== 200) fail(`Seeded parcel cannot be read: HTTP ${probe.status}`);

  return { token, trackingCodes };
}

export default function ({ token, trackingCodes }) {
  const code = trackingCodes[Math.floor(Math.random() * trackingCodes.length)];
  const response = http.get(`${PARCEL_URL}/${encodeURIComponent(code)}`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { endpoint: 'getParcel' },
    responseType: 'none',
  });

  const success = check(response, { 'GET existing parcel returns 200': (r) => r.status === 200 });
  if (!success) requestErrors.add(1);
}
