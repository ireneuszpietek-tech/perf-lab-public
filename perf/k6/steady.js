import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 300,            // target RPS (start with 200–300)
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 100, // k6 allocates VUs to satisfy target rate
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],          // <1% errors
    http_req_duration: ['p(95)<25', 'p(99)<50'], // in ms (can be relaxed locally)
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/hello');
  check(res, { 'status is 200': r => r.status === 200 });
}
