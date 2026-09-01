// 수신자 서버 워크로드.
//   1) 로그인 / 토큰 갱신  → UPDATE member_account  (자원 B에 X락)
//   2) 알림 확인          → UPDATE notification      (자원 A에 X락)
// 배치는 A → (FK로) B 순서라서, 이 B → A 가 반대 방향이 된다.
import mysql from 'mysql2/promise';

const DB = {
  host: process.env.DB_HOST ?? '127.0.0.1',
  port: Number(process.env.DB_PORT ?? 13306),
  user: 'root', password: 'labpass', database: 'deadlock_lab',
  connectionLimit: Number(process.env.POOL ?? 12),   // 배치와 분리된 전용 풀
};
const CONCURRENCY = Number(process.env.CONCURRENCY ?? 12);
const INTERVAL_MS = Number(process.env.INTERVAL_MS ?? 3);
const GAP_MICROS  = Number(process.env.GAP_MICROS ?? 120);  // 토큰 갱신 → 알림 확인 사이

const pool = mysql.createPool(DB);
const spin = (us) => { const end = process.hrtime.bigint() + BigInt(us * 1000); while (process.hrtime.bigint() < end); };

const M = () => ({ attempted:0, committed:0, deadlocks:0, lockTimeouts:0, otherErrors:0,
                   stepTokenFail:0, stepReadFail:0, lat:[] });
let metrics = M(), currentRun = null, active = false, variant = {};

async function readControl() {
  const [rows] = await pool.query('SELECT * FROM experiment_control WHERE id = 1');
  return rows[0] ?? null;
}

async function pickAccountAndNotification() {
  const [rows] = await pool.query(
    'SELECT n.id AS nid, n.account_id AS aid FROM notification n ORDER BY RAND() LIMIT 1');
  if (rows.length) return { accountId: rows[0].aid, notificationId: rows[0].nid };
  const [acc] = await pool.query('SELECT id FROM member_account ORDER BY RAND() LIMIT 1');
  return acc.length ? { accountId: acc[0].id, notificationId: null } : null;
}

const TOKEN_SQL = 'UPDATE member_account SET access_token = ?, token_refreshed_at = NOW(6), last_login_at = NOW(6) WHERE id = ?';
const READ_SQL  = 'UPDATE notification SET read_at = NOW(6) WHERE id = ?';

async function oneRequest() {
  const t = await pickAccountAndNotification();
  if (!t) return;
  const token = 'tk-' + Math.floor(Math.random() * 1e9);
  const conn = await pool.getConnection();
  metrics.attempted++;
  const t0 = process.hrtime.bigint();
  let step = 'token';
  try {
    if (variant.receiver_tx_split) {
      // V9 — 토큰 갱신과 알림 확인을 별도 트랜잭션으로 분리
      await conn.beginTransaction();
      await conn.execute(TOKEN_SQL, [token, t.accountId]);
      await conn.commit();
      step = 'read';
      if (t.notificationId) {
        await conn.beginTransaction();
        await conn.execute(READ_SQL, [t.notificationId]);
        await conn.commit();
      }
    } else {
      await conn.query(`SET TRANSACTION ISOLATION LEVEL ${variant.isolation_level ?? 'REPEATABLE READ'}`);
      await conn.beginTransaction();
      if (variant.receiver_unify) {
        // V4 — 배치와 같은 A → B 순서로 통일
        step = 'read';
        if (t.notificationId) await conn.execute(READ_SQL, [t.notificationId]);
        spin(GAP_MICROS);
        step = 'token';
        await conn.execute(TOKEN_SQL, [token, t.accountId]);
      } else {
        // V0 — 로그인/토큰 갱신이 먼저다. 자연스러운 순서이자 사건의 원인
        await conn.execute(TOKEN_SQL, [token, t.accountId]);
        spin(GAP_MICROS);
        step = 'read';
        if (t.notificationId) await conn.execute(READ_SQL, [t.notificationId]);
      }
      await conn.commit();
    }
    metrics.committed++;
    metrics.lat.push(Number(process.hrtime.bigint() - t0) / 1e6);
  } catch (err) {
    try { await conn.rollback(); } catch {}
    // Spring 처럼 예외 타입에 기대지 않고 벤더 에러코드로 구분한다
    if (err.errno === 1213) metrics.deadlocks++;
    else if (err.errno === 1205) metrics.lockTimeouts++;
    else metrics.otherErrors++;
    if (step === 'token') metrics.stepTokenFail++; else metrics.stepReadFail++;
  } finally {
    conn.release();
  }
}

function p95() {
  if (!metrics.lat.length) return 0;
  const s = [...metrics.lat].sort((a, b) => a - b);
  return Math.round(s[Math.max(0, Math.ceil(0.95 * s.length) - 1)]);
}

async function flush() {
  if (!currentRun) return;
  await pool.execute(
    `INSERT INTO experiment_metric
       (run_id, role, attempted, committed, deadlocks, lock_timeouts, other_errors, p95_millis, step_token_fail, step_read_fail)
     VALUES (?, 'receiver', ?, ?, ?, ?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE
       attempted=VALUES(attempted), committed=VALUES(committed), deadlocks=VALUES(deadlocks),
       lock_timeouts=VALUES(lock_timeouts), other_errors=VALUES(other_errors),
       p95_millis=VALUES(p95_millis), step_token_fail=VALUES(step_token_fail), step_read_fail=VALUES(step_read_fail)`,
    [currentRun, metrics.attempted, metrics.committed, metrics.deadlocks,
     metrics.lockTimeouts, metrics.otherErrors, p95(), metrics.stepTokenFail, metrics.stepReadFail]);
}

async function worker() {
  for (;;) {
    if (active) await oneRequest();
    await new Promise(r => setTimeout(r, active ? INTERVAL_MS : 200));
  }
}

async function controlLoop() {
  for (;;) {
    try {
      const c = await readControl();
      if (c) {
        if (c.run_id !== currentRun) {          // 새 variant → 지표 리셋
          if (currentRun) await flush();
          currentRun = c.run_id; metrics = M();
          console.log(`[receiver] run=${c.run_id} variant=${c.variant_id} unify=${c.receiver_unify} split=${c.receiver_tx_split}`);
        }
        variant = c;
        active = c.active === 1;
        if (active) await flush();
      }
    } catch (e) { /* 컨트롤 테이블 준비 전 */ }
    await new Promise(r => setTimeout(r, 300));
  }
}

console.log(`[receiver] 시작 — 동시 ${CONCURRENCY}, 풀 ${DB.connectionLimit}, 간격 ${INTERVAL_MS}ms`);
controlLoop();
for (let i = 0; i < CONCURRENCY; i++) worker();
