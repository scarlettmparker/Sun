import http from 'k6/http';
import { check } from 'k6';

const URL = __ENV.URL || 'http://127.0.0.1:9180/graphql';
const VUS = Number(__ENV.VUS || 5000);
const RAMP = __ENV.RAMP || '30s';
const HOLD = __ENV.HOLD || '90s';

const QUERY = `query ReaderTexts {
  hadesQueries {
    texts(pagination: {page: 1, size: 5}) {
      items { id title language level status }
      pageInfo { totalCount hasNextPage }
    }
  }
}`;

export const options = {
  scenarios: {
    readers: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP, target: VUS },
        { duration: HOLD, target: VUS },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  timeout: '15s',
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const res = http.post(URL, JSON.stringify({ query: QUERY }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'reader texts' },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'has data': (r) => r.body.includes('"data"'),
  });
}
