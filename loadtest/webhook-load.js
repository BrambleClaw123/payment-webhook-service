import http from 'loadtest/RESULT.md/http';
import exec from 'loadtest/RESULT.md/execution';
import { check } from 'loadtest/RESULT.md';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 100);         // số request mỗi giây
const DURATION = Number(__ENV.DURATION || 30);  // chạy bao nhiêu giây
const DUP = __ENV.DUP === '1';                  // 1 = gửi trùng cùng một eventId
const ORDERS = DUP ? 1 : RATE * DURATION;       // số đơn cần tạo sẵn
const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
    scenarios: {
        webhooks: {
            executor: 'constant-arrival-rate',   // giữ đúng tốc độ RATE request/giây
            rate: RATE,
            timeUnit: '1s',
            duration: `${DURATION}s`,
            preAllocatedVUs: 50,
            maxVUs: 300,
        },
    },
    thresholds: {
        'http_req_failed{endpoint:webhook}': ['rate<0.01'],
        'http_req_duration{endpoint:webhook}': ['p(95)<300'],
    },
};

// Chạy một lần trước khi bắt đầu: tạo sẵn các đơn hàng
export function setup() {
    const ids = [];
    while (ids.length < ORDERS) {
        const batch = [];
        const n = Math.min(100, ORDERS - ids.length);
        for (let j = 0; j < n; j++) {
            batch.push(['POST', `${BASE}/orders`, JSON.stringify({ amount: 150000 }), { headers: HEADERS }]);
        }
        http.batch(batch).forEach((r) => ids.push(r.json('id')));
    }
    return { run: Date.now(), ids };
}

// Mỗi lần lặp gửi một webhook
export default function (data) {
    const i = exec.scenario.iterationInTest;
    const eventId = DUP ? `evt_dup_${data.run}` : `evt_${data.run}_${i}`;
    const orderId = DUP ? data.ids[0] : data.ids[i];

    const res = http.post(
        `${BASE}/webhooks/payment`,
        JSON.stringify({ eventId, type: 'payment.succeeded', orderId, amount: 150000 }),
        { headers: HEADERS, tags: { endpoint: 'webhook' } }
    );

    check(res, {
        'status là 200': (r) => r.status === 200,
        'ACCEPTED hoặc DUPLICATE': (r) => r.body.includes('ACCEPTED') || r.body.includes('DUPLICATE'),
    });
}