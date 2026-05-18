import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// ─── 自定义指标 ───────────────────────────────────────────────
const scooterDuration = new Trend('scooter_list_duration');
const bookingDuration = new Trend('booking_create_duration');
const errorRate = new Rate('error_rate');

// ─── 测试配置 ─────────────────────────────────────────────────
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // 30秒内从0爬升到10个用户
    { duration: '2m',  target: 50 },   // 2分钟内爬升到50个用户
    { duration: '1m',  target: 50 },   // 保持50个用户1分钟
    { duration: '30s', target: 0 },    // 30秒内降回0
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95%的请求必须在2秒内完成
    error_rate: ['rate<0.1'],           // 错误率必须低于10%
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// ─── 登录拿token（每个VU启动时执行一次）────────────────────────
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: '1123087596@qq.com', password: 'NewPassw0rd!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const token = loginRes.json('data.accessToken');
  console.log('Setup: token obtained =', token ? 'YES' : 'NO');
  return { token };
}

// ─── 主测试逻辑（每个VU反复执行）────────────────────────────────
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  // ── 场景1：查询可用滑板车（读接口，高频）──────────────────────
  const scooterRes = http.get(`${BASE_URL}/scooters?status=AVAILABLE`, { headers });

  scooterDuration.add(scooterRes.timings.duration);
  errorRate.add(scooterRes.status !== 200);

  check(scooterRes, {
    '[滑板车列表] 状态码 200': (r) => r.status === 200,
    '[滑板车列表] 响应时间 < 1s': (r) => r.timings.duration < 1000,
    '[滑板车列表] 返回数组': (r) => Array.isArray(r.json('data')),
  });

  sleep(0.5);

  // ── 场景2：获取租赁选项（读接口）──────────────────────────────
  const hireRes = http.get(`${BASE_URL}/hire-options`, { headers });

  check(hireRes, {
    '[租赁选项] 状态码 200': (r) => r.status === 200,
    '[租赁选项] 响应时间 < 1s': (r) => r.timings.duration < 1000,
  });

  sleep(0.5);

  // ── 场景3：价格报价（写接口，中频）────────────────────────────
  const quoteRes = http.post(
    `${BASE_URL}/pricing/quotes`,
    JSON.stringify({ hireOptionCode: 'H1', scooterId: 'SCO-0001' }),
    { headers }
  );

  bookingDuration.add(quoteRes.timings.duration);
  errorRate.add(quoteRes.status !== 200);

  check(quoteRes, {
    '[价格报价] 状态码 200': (r) => r.status === 200,
    '[价格报价] 响应时间 < 2s': (r) => r.timings.duration < 2000,
    '[价格报价] finalPrice 存在': (r) => r.json('data.finalPrice') !== null,
  });

  sleep(1);
}

// ─── 测试结束后输出汇总 ───────────────────────────────────────
export function teardown(data) {
  console.log('压力测试完成');
}
