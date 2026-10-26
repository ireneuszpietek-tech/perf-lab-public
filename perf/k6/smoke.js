import http from 'k6/http';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
   rampingTest: {
      executor: "ramping-arrival-rate",
      startRate: 50,
      timeUnit: "1s",
      preAllocatedVUs: 50,
      maxVUs: 9000,
      stages: [
        { duration: "5m", target: 9000 }  // linear ramp
      ],
    },

//    'ramping-test': {
//      executor: 'ramping-arrival-rate',
//      startRate: 50,
//      timeUnit: '1s',
//      preAllocatedVUs: 50,
//      maxVUs: 1400,
//      stages: [
//        { duration: '30s', target: 50 },   // light traffic
//        { duration: '30s', target: 100 },  // normal
//        { duration: '30s', target: 200 },  // heavier
//        { duration: '30s', target: 400 },  // meltdown 1
//        { duration: '30s', target: 600 },  // meltdown 2
//        { duration: '30s', target: 800 },  // meltdown 3
//        { duration: '30s', target: 1000 },  // meltdown 4
//        { duration: '30s', target: 1200 },  // meltdown 5
//        { duration: '30s', target: 1400 },  // meltdown 6
//      ],
//    },

//    rps_2001: {
//      executor: 'constant-arrival-rate',
//      rate: 400,          // 2001 reqs...
//      timeUnit: '1s',      // ...per second
//      duration: '1s',     // duration
//      preAllocatedVUs: 400, // k6 pre-allocates 50 VUs
//      maxVUs: 2000,        // ramps up to 2001 if needed
//    },

//    load_test_ddos: {
//      executor: 'constant-arrival-rate',
//      rate: 400,
//      timeUnit: '1s',
//      duration: '30s',
//      preAllocatedVUs: 200,
//      maxVUs: 1000,
//    }
  },
  thresholds: {
    os_errors: ['count == 0'],        // FAIL if there are OS/NET errors
    http_req_failed: ['rate == 0'],   // FAIL if there are HTTP errors (5xx, timeouts)
  },
};

// custom metric for network/OS errors
export const os_errors = new Counter('os_errors');

export default function () {
  const url = 'http://localhost:8080/api/shipping/v1/parcels';

  const payload = JSON.stringify({
    from: 'Warsaw',
    to: 'Berlin',
  });


  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization:
        'Bearer eyJraWQiOiJkZXYtaHMyNTYiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJleHAiOjE3NjQzNTc1NDAsImlhdCI6MTc2NDM0Mzk0MCwianRpIjoiMjAxYmUxNDUtNTI2YS00N2NlLWIzYWQtZDMwMTE1NzIxNWY1Iiwic2NvcGUiOiIifQ.4Igm_PhGnOnEuG2TuxNLSr3y1XLgAaKcdf04DCRPh8s',
    },
  };

  try {
    const res = http.post(url, payload, params);
    // NOTE: console.log heavily slows down load generator at high RPS
    // console.log(`Response status: ${res.status}`);
  } catch (e) {
    os_errors.add(1);
    console.error(`OS/NET error: ${e}`);
  }
}
