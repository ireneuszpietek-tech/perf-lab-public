// Experiment: EXP-001
// Hypothesis: HYP-001 — Tail Latency Driven by Allocation Rate and GC Pressure
//
// Purpose:
// - Generate mixed load (80% GET / 20% POST) on a simple in-memory REST service
// - Compare runs (baseline vs variant) while recording JFR (Allocation, GC, CPU)
// - Focus on p99 tail latency stability under steady arrival rate
//
// Usage examples:
//   MODE=baseline RATE=200 WARMUP=2m MEASURE=10m k6 run exp-0001_hyp-0001_tail_latency_gc.js
//   MODE=variant  RATE=200 WARMUP=2m MEASURE=10m k6 run exp-0001_hyp-0001_tail_latency_gc.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const os_errors = new Counter('os_errors');

const RATE = Number(__ENV.RATE || 200);
const MODE = __ENV.MODE || 'baseline';
const AUTH_URL = __ENV.AUTH_URL || 'http://localhost:8080/dev/token?sub=123';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CREATE_URL = `${BASE_URL}/api/shipping/v1/parcels`;
const GET_URL = (id) => `${BASE_URL}/api/shipping/v1/parcels/${id}`;

const READ_PCT = Number(__ENV.READ_PCT || 80);        // GET %
const HOT_IDS = Number(__ENV.HOT_IDS || 2000);        // size of hot id space (stabilizes results)
const WARMUP = __ENV.WARMUP || '2m';
const MEASURE = __ENV.MEASURE || '10m';

export function setup() {
  const res = http.get(AUTH_URL, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'access_token' },
  });

  if (res.status !== 200) {
    throw new Error(`Auth failed: ${res.status}`);
  }

  return {
    token: res.json('access_token'),
  };
}
export const options = {
//  discardResponseBodies: true,
  tags: {
    experiment: 'EXP-0001',
    hypothesis: 'HYP-0001',
    mode: MODE,
  },
  scenarios: {
    warmup: {
      executor: 'constant-arrival-rate',
      rate: Math.max(1, Math.floor(RATE * 0.5)),
      timeUnit: '1s',
      duration: WARMUP,
      preAllocatedVUs: Math.min(Math.max(50, RATE), 500),
      maxVUs: Math.max(RATE * 2, 200),
      tags: { phase: 'warmup' },
    },
    measure: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: MEASURE,
      startTime: WARMUP,
      preAllocatedVUs: Math.min(Math.max(50, RATE), 500),
      maxVUs: Math.max(RATE * 2, 200),
      tags: { phase: 'measure' },
    },
  },
  thresholds: {
    // Tail-latency-focused thresholds (adjust after first baseline run)
    'http_req_duration{phase:measure,endpoint:getParcel}': ['p(99)<200'],
    'http_req_duration{phase:measure,endpoint:createParcel}': ['p(99)<400'],

    os_errors: ['count == 0'],
    'http_req_failed{phase:measure}': ['rate == 0'],
  },
};

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  try {
    const r = Math.random() * 100;

    if (r < (100 - READ_PCT)) {
      // WRITE (create)
      const payload = JSON.stringify({ from: 'Warsaw', to: 'Berlin' });

      const res = http.post(CREATE_URL, payload, {
        headers,
        tags: { endpoint: 'createParcel' },
        responseType: 'none',
        });

      check(res, { 'POST 2xx': (x) => x.status >= 200 && x.status < 300 });
    } else {
      // READ (get) — hot id space for stable tail-latency characteristics
      const id = Math.floor(Math.random() * HOT_IDS) + 1;

      const res = http.get(GET_URL(id), {
        headers,
        tags: { endpoint: 'getParcel' },
        responseType: 'none',
      });

      // 200 or 404 are both acceptable depending on your in-memory state
      check(res, { 'GET 200/404': (x) => x.status === 200 || x.status === 404 });
    }
  } catch (e) {
    os_errors.add(1);
  }

  // optional tiny sleep to avoid tight loop in some environments
  sleep(0.001);
}
