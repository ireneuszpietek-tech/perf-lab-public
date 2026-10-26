// Experiment: EXP-0001
// Hypothesis: HYP-0001 — Silent Exception Swallowing and JVM-Level Degradation (Metaspace Growth)
//
// Purpose:
// - Generate a steady write load on /api/shipping/v1/parcels
// - Run once in "baseline" mode (with swallowed NoSuchMethodError)
// - Run once in "fixed" mode (after dependency / classpath fix)
// - For both runs, record JFR (Errors, Classloading, Metaspace, LambdaForms)
//
// Usage examples:
//   MODE=baseline  RATE=200 DURATION=15m k6 run h7_exp00xx_silent_exception_swallowing.js
//   MODE=fixed     RATE=200 DURATION=15m k6 run h7_exp00xx_silent_exception_swallowing.js

import http from 'k6/http';
import { Counter } from 'k6/metrics';

const os_errors = new Counter('os_errors');

const RATE = __ENV.RATE || 200;          // req / sec (default 200 RPS)
const DURATION = __ENV.DURATION || '5m'; // duration
const MODE = __ENV.MODE || 'baseline';   // 'baseline' | 'fixed' (to tag runs)
const AUTH_URL = __ENV.AUTH_URL || 'http://localhost:8080/dev/token?sub=123';

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
  tags: {
    experiment: 'EXP-001',
    hypothesis: 'HYP-001',
    mode: MODE, // baseline vs fixed
  },
  scenarios: {
    h001_constant_rps: {
      executor: 'constant-arrival-rate',
      rate: Number(RATE),
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(Number(RATE), 500),
      maxVUs: Math.max(Number(RATE) * 2, 200),
    },
  },
  thresholds: {
    'http_req_duration{endpoint:createParcel}': ['p(95)<300'],
    'http_req_duration{endpoint:access_token}': ['p(95)<1000'],
    os_errors: ['count == 0'],          // FAIL if there are OS/NET errors
    http_req_failed: ['rate == 0'],     // FAIL if there are HTTP errors (5xx, timeouts)
  },
};

export default function (data) {
//  console.info(`Bearer ${data.token}`);
  const url = 'http://localhost:8080/api/shipping/v1/parcels';

  const payload = JSON.stringify({
    from: 'Warsaw',
    to: 'Berlin',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,
    },
    tags: {
      endpoint: 'createParcel',
    },
  };

  try {
    http.post(url, payload, params);
  } catch (e) {
    os_errors.add(1);
    // logs are heavy !
    console.error(`OS/NET error: ${e}`);
  }
}
