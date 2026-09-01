# A-M8 Deadlock Matrix — 측정 결과

배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A)
**배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.**

고정값: 테넌트 5 · 계정 1000 · 발송요청 4000 · 외부API지연 400us · 반복 3회
MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s

## 요약

| # | 구성 | 수신자 데드락 | 회차별 |
|---|---|---|---|
| V0 | ① Tasklet (최초) | **41~48 (중앙 41)** | 41 / 48 / 41 |
| V1 | ② chunk 전환 | **38~49 (중앙 43)** | 38 / 43 / 49 |
| V2 | ③ 조회를 TX 밖으로 | **43~48 (중앙 45)** | 43 / 45 / 48 |
| V3a | ④a 벌크로 묶기 | **0~0 (중앙 0)** | 0 / 0 / 0 |
| V3c | ④c 외부 API를 TX 밖으로 | **40~52 (중앙 44)** | 44 / 52 / 40 |
| V5 | READ COMMITTED | **0~0 (중앙 0)** | 0 / 0 / 0 |
| V8 | FK 제거 | **0~0 (중앙 0)** | 0 / 0 / 0 |

## 회차 상세

| # | 회차 | 구성 | 배치 롤백 | 기록 | 수신자 데드락 | InnoDB | 소요 |
|---|---|---|---|---|---|---|---|
| V0 | 1 | <sub>TASKLET / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 41 | 41 | 12.1s |
| V0 | 2 | <sub>TASKLET / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 48 | 48 | 11.9s |
| V0 | 3 | <sub>TASKLET / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 41 | 41 | 11.8s |
| V1 | 1 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 38 | 38 | 11.6s |
| V1 | 2 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 43 | 43 | 11.7s |
| V1 | 3 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 49 | 49 | 12.0s |
| V2 | 1 | <sub>CHUNK(500) / 조회 TX밖 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 43 | 43 | 12.0s |
| V2 | 2 | <sub>CHUNK(500) / 조회 TX밖 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 45 | 45 | 11.7s |
| V2 | 3 | <sub>CHUNK(500) / 조회 TX밖 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 48 | 48 | 11.8s |
| V3a | 1 | <sub>CHUNK(500) / 조회 TX내 / 발송 벌크 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 9.9s |
| V3a | 2 | <sub>CHUNK(500) / 조회 TX내 / 발송 벌크 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 10.1s |
| V3a | 3 | <sub>CHUNK(500) / 조회 TX내 / 발송 벌크 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 10.0s |
| V3c | 1 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX밖 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 44 | 44 | 10.2s |
| V3c | 2 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX밖 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 52 | 52 | 10.1s |
| V3c | 3 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX밖 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | 40 | 40 | 9.9s |
| V5 | 1 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / READ COMMITTED</sub> | 0 | 4000 | 0 | 0 | 12.8s |
| V5 | 2 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / READ COMMITTED</sub> | 0 | 4000 | 0 | 0 | 13.2s |
| V5 | 3 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / READ COMMITTED</sub> | 0 | 4000 | 0 | 0 | 12.7s |
| V8 | 1 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK X / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 11.2s |
| V8 | 2 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK X / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 11.1s |
| V8 | 3 | <sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK X / REPEATABLE READ</sub> | 0 | 4000 | 0 | 0 | 11.2s |

> 실험 환경의 결과다. 운영 환경의 수치가 아니다.

---

## V0 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:40:37 135358810506816
*** (1) TRANSACTION:
TRANSACTION 159545, ACTIVE 1 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9042, OS thread handle 135358652040768, query id 867904 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 878

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 93 page no 12 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 159545 lock_mode X locks rec but not gap
Record lock, heap no 213 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 800000000000036e; asc        n;;
 1: len 6; hex 000000026f39; asc     o9;;
 2: len 7; hex 010000013f0bd8; asc     ?  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d333633353834393334; asc tk-363584934;;
 5: len 8; hex 99bac36a240711a3; asc    j$   ;;
 6: len 8; hex 99bac36a240711a3; asc    j$   ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 91 page no 24 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 159545 lock_mode X locks rec but not gap waiting
Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036e; asc        n;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf2468; asc      $h;;
 3: len 8; hex 800000000000036e; asc        n;;
 4: len 8; hex 800000000000036e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c089a7f; asc    j    ;;
 7: len 8; hex 99bac36a1c089dcd; asc    j    ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 159521, ACTIVE 1 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 61 lock struct(s), heap size 24696, 5034 row lock(s), undo log entries 1132
MySQL thread id 9000, OS thread handle 135358149797440, query id 869086 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (878, 3878, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 91 page no 24 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 159521 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000275; asc        u;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba21cf; asc      ! ;;
 3: len 8; hex 8000000000000275; asc        u;;
 4: len 8; hex 8000000000000275; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d5b04; asc    j  [ ;;
 7: len 8; hex 99bac36a1b0d5e2d; asc    j  ^-;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000276; asc        v;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2254; asc      "T;;
 3: len 8; hex 8000000000000276; asc        v;;
 4: len 8; hex 8000000000000276; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d624d; asc    j  bM;;
 7: len 8; hex 99bac36a1b0d6593; asc    j  e ;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000277; asc        w;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba22d9; asc      " ;;
 3: len 8; hex 8000000000000277; asc        w;;
 4: len 8; hex 8000000000000277; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d6977; asc    j  iw;;
 7: len 8; hex 99bac36a1b0d6c8a; asc    j  l ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000278; asc        x;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba235e; asc      #^;;
 3: len 8; hex 8000000000000278; asc        x;;
 4: len 8; hex 8000000000000278; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d70c2; asc    j  p ;;
 7: len 8; hex 99bac36a1b0d73ea; asc    j  s ;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000279; asc        y;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba23e3; asc      # ;;
 3: len 8; hex 8000000000000279; asc        y;;
 4: len 8; hex 8000000000000279; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d7d23; asc    j  }#;;
 7: len 8; hex 99bac36a1b0d814b; asc    j   K;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027a; asc        z;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2468; asc      $h;;
 3: len 8; hex 800000000000027a; asc        z;;
 4: len 8; hex 800000000000027a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d89ab; asc    j    ;;
 7: len 8; hex 99bac36a1b0d8d64; asc    j   d;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027b; asc        {;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba24ed; asc      $ ;;
 3: len 8; hex 800000000000027b; asc        {;;
 4: len 8; hex 800000000000027b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0d95f5; asc    j    ;;
 7: len 8; hex 99bac36a1b0d9a13; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027c; asc        |;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2572; asc      %r;;
 3: len 8; hex 800000000000027c; asc        |;;
 4: len 8; hex 800000000000027c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0da28a; asc    j    ;;
 7: len 8; hex 99bac36a1b0da6ec; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027d; asc        };;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba25f7; asc      % ;;
 3: len 8; hex 800000000000027d; asc        };;
 4: len 8; hex 800000000000027d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0daf66; asc    j   f;;
 7: len 8; hex 99bac36a1b0db43d; asc    j   =;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027e; asc        ~;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba267c; asc      &|;;
 3: len 8; hex 800000000000027e; asc        ~;;
 4: len 8; hex 800000000000027e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0dbcc0; asc    j    ;;
 7: len 8; hex 99bac36a1b0dc0cb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027f; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2701; asc      ' ;;
 3: len 8; hex 800000000000027f; asc         ;;
 4: len 8; hex 800000000000027f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0dc891; asc    j    ;;
 7: len 8; hex 99bac36a1b0dcca5; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000280; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2786; asc      ' ;;
 3: len 8; hex 8000000000000280; asc         ;;
 4: len 8; hex 8000000000000280; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0dd53c; asc    j   <;;
 7: len 8; hex 99bac36a1b0dd985; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000281; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba280b; asc      ( ;;
 3: len 8; hex 8000000000000281; asc         ;;
 4: len 8; hex 8000000000000281; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0de1e6; asc    j    ;;
 7: len 8; hex 99bac36a1b0de5ff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000282; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2890; asc      ( ;;
 3: len 8; hex 8000000000000282; asc         ;;
 4: len 8; hex 8000000000000282; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0ded5a; asc    j   Z;;
 7: len 8; hex 99bac36a1b0df24b; asc    j   K;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000283; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2915; asc      ) ;;
 3: len 8; hex 8000000000000283; asc         ;;
 4: len 8; hex 8000000000000283; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0dfa99; asc    j    ;;
 7: len 8; hex 99bac36a1b0dfe6c; asc    j   l;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000284; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba299a; asc      ) ;;
 3: len 8; hex 8000000000000284; asc         ;;
 4: len 8; hex 8000000000000284; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e079c; asc    j    ;;
 7: len 8; hex 99bac36a1b0e0bff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000285; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2a1f; asc      * ;;
 3: len 8; hex 8000000000000285; asc         ;;
 4: len 8; hex 8000000000000285; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e130a; asc    j    ;;
 7: len 8; hex 99bac36a1b0e171a; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000286; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2aa4; asc      * ;;
 3: len 8; hex 8000000000000286; asc         ;;
 4: len 8; hex 8000000000000286; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e1f81; asc    j    ;;
 7: len 8; hex 99bac36a1b0e24f5; asc    j  $ ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000287; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2b29; asc      +);;
 3: len 8; hex 8000000000000287; asc         ;;
 4: len 8; hex 8000000000000287; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e31d9; asc    j  1 ;;
 7: len 8; hex 99bac36a1b0e3618; asc    j  6 ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000288; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2bae; asc      + ;;
 3: len 8; hex 8000000000000288; asc         ;;
 4: len 8; hex 8000000000000288; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e4000; asc    j  @ ;;
 7: len 8; hex 99bac36a1b0e44a8; asc    j  D ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000289; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2c33; asc      ,3;;
 3: len 8; hex 8000000000000289; asc         ;;
 4: len 8; hex 8000000000000289; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e4d53; asc    j  MS;;
 7: len 8; hex 99bac36a1b0e51c8; asc    j  Q ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028a; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2cb8; asc      , ;;
 3: len 8; hex 800000000000028a; asc         ;;
 4: len 8; hex 800000000000028a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e5a17; asc    j  Z ;;
 7: len 8; hex 99bac36a1b0e5e63; asc    j  ^c;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028b; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2d3d; asc      -=;;
 3: len 8; hex 800000000000028b; asc         ;;
 4: len 8; hex 800000000000028b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e67e0; asc    j  g ;;
 7: len 8; hex 99bac36a1b0e6c3c; asc    j  l<;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028c; asc         ;;
 1: len 6; hex 000000026dfa; asc     m ;;
 2: len 7; hex 02000000f71e84; asc        ;;
 3: len 8; hex 800000000000028c; asc         ;;
 4: len 8; hex 800000000000028c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e7364; asc    j  sd;;
 7: len 8; hex 99bac36a1b0e77a9; asc    j  w ;;
 8: len 8; hex 99bac36a1c0e189d; asc    j    ;;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028d; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2e47; asc      .G;;
 3: len 8; hex 800000000000028d; asc         ;;
 4: len 8; hex 800000000000028d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e7ea6; asc    j  ~ ;;
 7: len 8; hex 99bac36a1b0e8236; asc    j   6;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028e; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2ecc; asc      . ;;
 3: len 8; hex 800000000000028e; asc         ;;
 4: len 8; hex 800000000000028e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e8841; asc    j   A;;
 7: len 8; hex 99bac36a1b0e8c70; asc    j   p;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028f; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2f51; asc      /Q;;
 3: len 8; hex 800000000000028f; asc         ;;
 4: len 8; hex 800000000000028f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e9327; asc    j   ';;
 7: len 8; hex 99bac36a1b0e9679; asc    j   y;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000290; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba2fd6; asc      / ;;
 3: len 8; hex 8000000000000290; asc         ;;
 4: len 8; hex 8000000000000290; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0e9e17; asc    j    ;;
 7: len 8; hex 99bac36a1b0ea166; asc    j   f;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000291; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba305b; asc      0[;;
 3: len 8; hex 8000000000000291; asc         ;;
 4: len 8; hex 8000000000000291; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0eaa14; asc    j    ;;
 7: len 8; hex 99bac36a1b0eae54; asc    j   T;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000292; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba30e0; asc      0 ;;
 3: len 8; hex 8000000000000292; asc         ;;
 4: len 8; hex 8000000000000292; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0eb6fd; asc    j    ;;
 7: len 8; hex 99bac36a1b0ebb36; asc    j   6;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000293; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3165; asc      1e;;
 3: len 8; hex 8000000000000293; asc         ;;
 4: len 8; hex 8000000000000293; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0ec2e6; asc    j    ;;
 7: len 8; hex 99bac36a1b0ec779; asc    j   y;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000294; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba31ea; asc      1 ;;
 3: len 8; hex 8000000000000294; asc         ;;
 4: len 8; hex 8000000000000294; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0ed20a; asc    j    ;;
 7: len 8; hex 99bac36a1b0ed63b; asc    j   ;;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000295; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba326f; asc      2o;;
 3: len 8; hex 8000000000000295; asc         ;;
 4: len 8; hex 8000000000000295; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0edfbd; asc    j    ;;
 7: len 8; hex 99bac36a1b0ee41d; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000296; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba32f4; asc      2 ;;
 3: len 8; hex 8000000000000296; asc         ;;
 4: len 8; hex 8000000000000296; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0eec93; asc    j    ;;
 7: len 8; hex 99bac36a1b0ef58d; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000297; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3379; asc      3y;;
 3: len 8; hex 8000000000000297; asc         ;;
 4: len 8; hex 8000000000000297; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0efdd8; asc    j    ;;
 7: len 8; hex 99bac36a1b0f02fb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000298; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba33fe; asc      3 ;;
 3: len 8; hex 8000000000000298; asc         ;;
 4: len 8; hex 8000000000000298; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0f0c66; asc    j   f;;
 7: len 8; hex 99bac36a1b0f113e; asc    j   >;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000299; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3483; asc      4 ;;
 3: len 8; hex 8000000000000299; asc         ;;
 4: len 8; hex 8000000000000299; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0f17eb; asc    j    ;;
 7: len 8; hex 99bac36a1b0f1cb2; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029a; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3508; asc      5 ;;
 3: len 8; hex 800000000000029a; asc         ;;
 4: len 8; hex 800000000000029a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0f2528; asc    j  %(;;
 7: len 8; hex 99bac36a1b0f298d; asc    j  ) ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029b; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba358d; asc      5 ;;
 3: len 8; hex 800000000000029b; asc         ;;
 4: len 8; hex 800000000000029b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0f32bc; asc    j  2 ;;
 7: len 8; hex 99bac36a1b0f3732; asc    j  72;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029c; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3612; asc      6 ;;
 3: len 8; hex 800000000000029c; asc         ;;
 4: len 8; hex 800000000000029c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1b0f3e06; asc    j  > ;;
 7: len 8; hex 99bac36a1b0f4160; asc    j  A`;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029d; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3697; asc      6 ;;
 3: len 8; hex 800000000000029d; asc         ;;
 4: len 8; hex 800000000000029d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c000674; asc    j   t;;
 7: len 8; hex 99bac36a1c000aba; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029e; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba371c; asc      7 ;;
 3: len 8; hex 800000000000029e; asc         ;;
 4: len 8; hex 800000000000029e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c001637; asc    j   7;;
 7: len 8; hex 99bac36a1c001ae4; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029f; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba37a1; asc      7 ;;
 3: len 8; hex 800000000000029f; asc         ;;
 4: len 8; hex 800000000000029f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0024dd; asc    j  $ ;;
 7: len 8; hex 99bac36a1c0028c9; asc    j  ( ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3826; asc      8&;;
 3: len 8; hex 80000000000002a0; asc         ;;
 4: len 8; hex 80000000000002a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c002f40; asc    j  /@;;
 7: len 8; hex 99bac36a1c00335e; asc    j  3^;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a1; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba38ab; asc      8 ;;
 3: len 8; hex 80000000000002a1; asc         ;;
 4: len 8; hex 80000000000002a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c003a28; asc    j  :(;;
 7: len 8; hex 99bac36a1c003eca; asc    j  > ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a2; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3930; asc      90;;
 3: len 8; hex 80000000000002a2; asc         ;;
 4: len 8; hex 80000000000002a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0046dd; asc    j  F ;;
 7: len 8; hex 99bac36a1c004a50; asc    j  JP;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba39b5; asc      9 ;;
 3: len 8; hex 80000000000002a3; asc         ;;
 4: len 8; hex 80000000000002a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c005204; asc    j  R ;;
 7: len 8; hex 99bac36a1c0055e3; asc    j  U ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3a3a; asc      ::;;
 3: len 8; hex 80000000000002a4; asc         ;;
 4: len 8; hex 80000000000002a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c005dc0; asc    j  ] ;;
 7: len 8; hex 99bac36a1c006227; asc    j  b';;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3abf; asc      : ;;
 3: len 8; hex 80000000000002a5; asc         ;;
 4: len 8; hex 80000000000002a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c006785; asc    j  g ;;
 7: len 8; hex 99bac36a1c006af7; asc    j  j ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3b44; asc      ;D;;
 3: len 8; hex 80000000000002a6; asc         ;;
 4: len 8; hex 80000000000002a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c007380; asc    j  s ;;
 7: len 8; hex 99bac36a1c007a04; asc    j  z ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3bc9; asc      ; ;;
 3: len 8; hex 80000000000002a7; asc         ;;
 4: len 8; hex 80000000000002a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c008304; asc    j    ;;
 7: len 8; hex 99bac36a1c0087b3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3c4e; asc      <N;;
 3: len 8; hex 80000000000002a8; asc         ;;
 4: len 8; hex 80000000000002a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c008f37; asc    j   7;;
 7: len 8; hex 99bac36a1c00938f; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a9; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3cd3; asc      < ;;
 3: len 8; hex 80000000000002a9; asc         ;;
 4: len 8; hex 80000000000002a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c009a52; asc    j   R;;
 7: len 8; hex 99bac36a1c009edf; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002aa; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3d58; asc      =X;;
 3: len 8; hex 80000000000002aa; asc         ;;
 4: len 8; hex 80000000000002aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00a65f; asc    j   _;;
 7: len 8; hex 99bac36a1c00a9fb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ab; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3ddd; asc      = ;;
 3: len 8; hex 80000000000002ab; asc         ;;
 4: len 8; hex 80000000000002ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00b03b; asc    j   ;;;
 7: len 8; hex 99bac36a1c00b473; asc    j   s;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ac; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3e62; asc      >b;;
 3: len 8; hex 80000000000002ac; asc         ;;
 4: len 8; hex 80000000000002ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00bb04; asc    j    ;;
 7: len 8; hex 99bac36a1c00bf4e; asc    j   N;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ad; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3ee7; asc      > ;;
 3: len 8; hex 80000000000002ad; asc         ;;
 4: len 8; hex 80000000000002ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00c7ff; asc    j    ;;
 7: len 8; hex 99bac36a1c00ccdb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ae; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001ba3f6c; asc      ?l;;
 3: len 8; hex 80000000000002ae; asc         ;;
 4: len 8; hex 80000000000002ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00d537; asc    j   7;;
 7: len 8; hex 99bac36a1c00d966; asc    j   f;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002af; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be008f; asc        ;;
 3: len 8; hex 80000000000002af; asc         ;;
 4: len 8; hex 80000000000002af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00e0db; asc    j    ;;
 7: len 8; hex 99bac36a1c00e4e0; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0114; asc        ;;
 3: len 8; hex 80000000000002b0; asc         ;;
 4: len 8; hex 80000000000002b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00ec68; asc    j   h;;
 7: len 8; hex 99bac36a1c00f0ad; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b1; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0199; asc        ;;
 3: len 8; hex 80000000000002b1; asc         ;;
 4: len 8; hex 80000000000002b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c00f82f; asc    j   /;;
 7: len 8; hex 99bac36a1c00fbae; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b2; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be021e; asc        ;;
 3: len 8; hex 80000000000002b2; asc         ;;
 4: len 8; hex 80000000000002b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0102f1; asc    j    ;;
 7: len 8; hex 99bac36a1c01071f; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be02a3; asc        ;;
 3: len 8; hex 80000000000002b3; asc         ;;
 4: len 8; hex 80000000000002b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c011201; asc    j    ;;
 7: len 8; hex 99bac36a1c011624; asc    j   $;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0328; asc       (;;
 3: len 8; hex 80000000000002b4; asc         ;;
 4: len 8; hex 80000000000002b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c011d91; asc    j    ;;
 7: len 8; hex 99bac36a1c01212f; asc    j  !/;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be03ad; asc        ;;
 3: len 8; hex 80000000000002b5; asc         ;;
 4: len 8; hex 80000000000002b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0126ae; asc    j  & ;;
 7: len 8; hex 99bac36a1c012a46; asc    j  *F;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0432; asc       2;;
 3: len 8; hex 80000000000002b6; asc         ;;
 4: len 8; hex 80000000000002b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0133d6; asc    j  3 ;;
 7: len 8; hex 99bac36a1c013c2c; asc    j  <,;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be04b7; asc        ;;
 3: len 8; hex 80000000000002b7; asc         ;;
 4: len 8; hex 80000000000002b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c014477; asc    j  Dw;;
 7: len 8; hex 99bac36a1c014818; asc    j  H ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be053c; asc       <;;
 3: len 8; hex 80000000000002b8; asc         ;;
 4: len 8; hex 80000000000002b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01513f; asc    j  Q?;;
 7: len 8; hex 99bac36a1c01564c; asc    j  VL;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b9; asc         ;;
 1: len 6; hex 000000026e5a; asc     nZ;;
 2: len 7; hex 010000011e0ec4; asc        ;;
 3: len 8; hex 80000000000002b9; asc         ;;
 4: len 8; hex 80000000000002b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c015ff8; asc    j  _ ;;
 7: len 8; hex 99bac36a1c0164b0; asc    j  d ;;
 8: len 8; hex 99bac36a1f0aed64; asc    j   d;;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ba; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0646; asc       F;;
 3: len 8; hex 80000000000002ba; asc         ;;
 4: len 8; hex 80000000000002ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c016e1a; asc    j  n ;;
 7: len 8; hex 99bac36a1c017245; asc    j  rE;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bb; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be06cb; asc        ;;
 3: len 8; hex 80000000000002bb; asc         ;;
 4: len 8; hex 80000000000002bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c017b61; asc    j  {a;;
 7: len 8; hex 99bac36a1c018067; asc    j   g;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bc; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0750; asc       P;;
 3: len 8; hex 80000000000002bc; asc         ;;
 4: len 8; hex 80000000000002bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0189b7; asc    j    ;;
 7: len 8; hex 99bac36a1c018e63; asc    j   c;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bd; asc         ;;
 1: len 6; hex 000000026df7; asc     m ;;
 2: len 7; hex 01000001b41710; asc        ;;
 3: len 8; hex 80000000000002bd; asc         ;;
 4: len 8; hex 80000000000002bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0195d4; asc    j    ;;
 7: len 8; hex 99bac36a1c0199d8; asc    j    ;;
 8: len 8; hex 99bac36a1c0e003d; asc    j   =;;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002be; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be085a; asc       Z;;
 3: len 8; hex 80000000000002be; asc         ;;
 4: len 8; hex 80000000000002be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01a0f1; asc    j    ;;
 7: len 8; hex 99bac36a1c01a58e; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bf; asc         ;;
 1: len 6; hex 000000026dfe; asc     m ;;
 2: len 7; hex 020000014b03f4; asc     K  ;;
 3: len 8; hex 80000000000002bf; asc         ;;
 4: len 8; hex 80000000000002bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01aaa2; asc    j    ;;
 7: len 8; hex 99bac36a1c01ae38; asc    j   8;;
 8: len 8; hex 99bac36a1c0e1bbe; asc    j    ;;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0964; asc       d;;
 3: len 8; hex 80000000000002c0; asc         ;;
 4: len 8; hex 80000000000002c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01ba00; asc    j    ;;
 7: len 8; hex 99bac36a1c01beb0; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c1; asc         ;;
 1: len 6; hex 000000026e11; asc     n ;;
 2: len 7; hex 010000013a0c0e; asc     :  ;;
 3: len 8; hex 80000000000002c1; asc         ;;
 4: len 8; hex 80000000000002c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01c8ca; asc    j    ;;
 7: len 8; hex 99bac36a1c01ccba; asc    j    ;;
 8: len 8; hex 99bac36a1d0eff1a; asc    j    ;;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c2; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0a6e; asc       n;;
 3: len 8; hex 80000000000002c2; asc         ;;
 4: len 8; hex 80000000000002c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01d37f; asc    j    ;;
 7: len 8; hex 99bac36a1c01d6ee; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0af3; asc        ;;
 3: len 8; hex 80000000000002c3; asc         ;;
 4: len 8; hex 80000000000002c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01dfb3; asc    j    ;;
 7: len 8; hex 99bac36a1c01e3ec; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0b78; asc       x;;
 3: len 8; hex 80000000000002c4; asc         ;;
 4: len 8; hex 80000000000002c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01e9fe; asc    j    ;;
 7: len 8; hex 99bac36a1c01edd8; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0bfd; asc        ;;
 3: len 8; hex 80000000000002c5; asc         ;;
 4: len 8; hex 80000000000002c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c01f57e; asc    j   ~;;
 7: len 8; hex 99bac36a1c01fac3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0c82; asc        ;;
 3: len 8; hex 80000000000002c6; asc         ;;
 4: len 8; hex 80000000000002c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c020276; asc    j   v;;
 7: len 8; hex 99bac36a1c0208da; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0d07; asc        ;;
 3: len 8; hex 80000000000002c7; asc         ;;
 4: len 8; hex 80000000000002c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02115c; asc    j   \;;
 7: len 8; hex 99bac36a1c021661; asc    j   a;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0d8c; asc        ;;
 3: len 8; hex 80000000000002c8; asc         ;;
 4: len 8; hex 80000000000002c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c021e31; asc    j   1;;
 7: len 8; hex 99bac36a1c0221ca; asc    j  ! ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c9; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0e11; asc        ;;
 3: len 8; hex 80000000000002c9; asc         ;;
 4: len 8; hex 80000000000002c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c022acb; asc    j  * ;;
 7: len 8; hex 99bac36a1c022f31; asc    j  /1;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ca; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0e96; asc        ;;
 3: len 8; hex 80000000000002ca; asc         ;;
 4: len 8; hex 80000000000002ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02390a; asc    j  9 ;;
 7: len 8; hex 99bac36a1c023dd0; asc    j  = ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cb; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0f1b; asc        ;;
 3: len 8; hex 80000000000002cb; asc         ;;
 4: len 8; hex 80000000000002cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0246b9; asc    j  F ;;
 7: len 8; hex 99bac36a1c024af8; asc    j  J ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cc; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be0fa0; asc        ;;
 3: len 8; hex 80000000000002cc; asc         ;;
 4: len 8; hex 80000000000002cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c025247; asc    j  RG;;
 7: len 8; hex 99bac36a1c0255ce; asc    j  U ;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cd; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1025; asc       %;;
 3: len 8; hex 80000000000002cd; asc         ;;
 4: len 8; hex 80000000000002cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c025e3c; asc    j  ^<;;
 7: len 8; hex 99bac36a1c0263f8; asc    j  c ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ce; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be10aa; asc        ;;
 3: len 8; hex 80000000000002ce; asc         ;;
 4: len 8; hex 80000000000002ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c026cf5; asc    j  l ;;
 7: len 8; hex 99bac36a1c027218; asc    j  r ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cf; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be112f; asc       /;;
 3: len 8; hex 80000000000002cf; asc         ;;
 4: len 8; hex 80000000000002cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c027ab7; asc    j  z ;;
 7: len 8; hex 99bac36a1c027f0b; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be11b4; asc        ;;
 3: len 8; hex 80000000000002d0; asc         ;;
 4: len 8; hex 80000000000002d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0287b6; asc    j    ;;
 7: len 8; hex 99bac36a1c028bf9; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d1; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1239; asc       9;;
 3: len 8; hex 80000000000002d1; asc         ;;
 4: len 8; hex 80000000000002d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c029669; asc    j   i;;
 7: len 8; hex 99bac36a1c029a90; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d2; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be12be; asc        ;;
 3: len 8; hex 80000000000002d2; asc         ;;
 4: len 8; hex 80000000000002d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c029f9f; asc    j    ;;
 7: len 8; hex 99bac36a1c02a2d2; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1343; asc       C;;
 3: len 8; hex 80000000000002d3; asc         ;;
 4: len 8; hex 80000000000002d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02a793; asc    j    ;;
 7: len 8; hex 99bac36a1c02ab69; asc    j   i;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be13c8; asc        ;;
 3: len 8; hex 80000000000002d4; asc         ;;
 4: len 8; hex 80000000000002d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02b066; asc    j   f;;
 7: len 8; hex 99bac36a1c02b375; asc    j   u;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be144d; asc       M;;
 3: len 8; hex 80000000000002d5; asc         ;;
 4: len 8; hex 80000000000002d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02b834; asc    j   4;;
 7: len 8; hex 99bac36a1c02bb50; asc    j   P;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be14d2; asc        ;;
 3: len 8; hex 80000000000002d6; asc         ;;
 4: len 8; hex 80000000000002d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02bfd7; asc    j    ;;
 7: len 8; hex 99bac36a1c02c2f0; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1557; asc       W;;
 3: len 8; hex 80000000000002d7; asc         ;;
 4: len 8; hex 80000000000002d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02c7a7; asc    j    ;;
 7: len 8; hex 99bac36a1c02cb2a; asc    j   *;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be15dc; asc        ;;
 3: len 8; hex 80000000000002d8; asc         ;;
 4: len 8; hex 80000000000002d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02cf53; asc    j   S;;
 7: len 8; hex 99bac36a1c02d265; asc    j   e;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d9; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1661; asc       a;;
 3: len 8; hex 80000000000002d9; asc         ;;
 4: len 8; hex 80000000000002d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02d731; asc    j   1;;
 7: len 8; hex 99bac36a1c02da74; asc    j   t;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002da; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be16e6; asc        ;;
 3: len 8; hex 80000000000002da; asc         ;;
 4: len 8; hex 80000000000002da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02def9; asc    j    ;;
 7: len 8; hex 99bac36a1c02e22e; asc    j   .;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002db; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be176b; asc       k;;
 3: len 8; hex 80000000000002db; asc         ;;
 4: len 8; hex 80000000000002db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02e7bb; asc    j    ;;
 7: len 8; hex 99bac36a1c02eb9c; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002dc; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be17f0; asc        ;;
 3: len 8; hex 80000000000002dc; asc         ;;
 4: len 8; hex 80000000000002dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02f0b3; asc    j    ;;
 7: len 8; hex 99bac36a1c02f3db; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002dd; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1875; asc       u;;
 3: len 8; hex 80000000000002dd; asc         ;;
 4: len 8; hex 80000000000002dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c02f8f7; asc    j    ;;
 7: len 8; hex 99bac36a1c02fc0d; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002de; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be18fa; asc        ;;
 3: len 8; hex 80000000000002de; asc         ;;
 4: len 8; hex 80000000000002de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03008e; asc    j    ;;
 7: len 8; hex 99bac36a1c030401; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002df; asc         ;;
 1: len 6; hex 000000026e81; asc     n ;;
 2: len 7; hex 01000001340d57; asc     4 W;;
 3: len 8; hex 80000000000002df; asc         ;;
 4: len 8; hex 80000000000002df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0308db; asc    j    ;;
 7: len 8; hex 99bac36a1c030bea; asc    j    ;;
 8: len 8; hex 99bac36a2001b33f; asc    j   ?;;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1a04; asc        ;;
 3: len 8; hex 80000000000002e0; asc         ;;
 4: len 8; hex 80000000000002e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c031074; asc    j   t;;
 7: len 8; hex 99bac36a1c03140f; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e1; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1a89; asc        ;;
 3: len 8; hex 80000000000002e1; asc         ;;
 4: len 8; hex 80000000000002e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03186b; asc    j   k;;
 7: len 8; hex 99bac36a1c031b80; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e2; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1b0e; asc        ;;
 3: len 8; hex 80000000000002e2; asc         ;;
 4: len 8; hex 80000000000002e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c032011; asc    j    ;;
 7: len 8; hex 99bac36a1c0323d5; asc    j  # ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1b93; asc        ;;
 3: len 8; hex 80000000000002e3; asc         ;;
 4: len 8; hex 80000000000002e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03284a; asc    j  (J;;
 7: len 8; hex 99bac36a1c034398; asc    j  C ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1c18; asc        ;;
 3: len 8; hex 80000000000002e4; asc         ;;
 4: len 8; hex 80000000000002e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0348b2; asc    j  H ;;
 7: len 8; hex 99bac36a1c034bf1; asc    j  K ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1c9d; asc        ;;
 3: len 8; hex 80000000000002e5; asc         ;;
 4: len 8; hex 80000000000002e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c035055; asc    j  PU;;
 7: len 8; hex 99bac36a1c03539c; asc    j  S ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1d22; asc       ";;
 3: len 8; hex 80000000000002e6; asc         ;;
 4: len 8; hex 80000000000002e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03581e; asc    j  X ;;
 7: len 8; hex 99bac36a1c035b5d; asc    j  [];;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1da7; asc        ;;
 3: len 8; hex 80000000000002e7; asc         ;;
 4: len 8; hex 80000000000002e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c036003; asc    j  ` ;;
 7: len 8; hex 99bac36a1c03635a; asc    j  cZ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1e2c; asc       ,;;
 3: len 8; hex 80000000000002e8; asc         ;;
 4: len 8; hex 80000000000002e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0367db; asc    j  g ;;
 7: len 8; hex 99bac36a1c036aec; asc    j  j ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e9; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1eb1; asc        ;;
 3: len 8; hex 80000000000002e9; asc         ;;
 4: len 8; hex 80000000000002e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c036f28; asc    j  o(;;
 7: len 8; hex 99bac36a1c0372a9; asc    j  r ;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ea; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1f36; asc       6;;
 3: len 8; hex 80000000000002ea; asc         ;;
 4: len 8; hex 80000000000002ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c037748; asc    j  wH;;
 7: len 8; hex 99bac36a1c037a8d; asc    j  z ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002eb; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be1fbb; asc        ;;
 3: len 8; hex 80000000000002eb; asc         ;;
 4: len 8; hex 80000000000002eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c037ecd; asc    j  ~ ;;
 7: len 8; hex 99bac36a1c038227; asc    j   ';;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ec; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2040; asc       @;;
 3: len 8; hex 80000000000002ec; asc         ;;
 4: len 8; hex 80000000000002ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0386e7; asc    j    ;;
 7: len 8; hex 99bac36a1c0389ff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ed; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be20c5; asc        ;;
 3: len 8; hex 80000000000002ed; asc         ;;
 4: len 8; hex 80000000000002ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c038e48; asc    j   H;;
 7: len 8; hex 99bac36a1c0391ca; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ee; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be214a; asc      !J;;
 3: len 8; hex 80000000000002ee; asc         ;;
 4: len 8; hex 80000000000002ee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03965b; asc    j   [;;
 7: len 8; hex 99bac36a1c0399dd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ef; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be21cf; asc      ! ;;
 3: len 8; hex 80000000000002ef; asc         ;;
 4: len 8; hex 80000000000002ef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c039ed2; asc    j    ;;
 7: len 8; hex 99bac36a1c03a257; asc    j   W;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f0; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2254; asc      "T;;
 3: len 8; hex 80000000000002f0; asc         ;;
 4: len 8; hex 80000000000002f0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03a78f; asc    j    ;;
 7: len 8; hex 99bac36a1c03aac3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f1; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be22d9; asc      " ;;
 3: len 8; hex 80000000000002f1; asc         ;;
 4: len 8; hex 80000000000002f1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03af46; asc    j   F;;
 7: len 8; hex 99bac36a1c03b26b; asc    j   k;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f3; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be23e3; asc      # ;;
 3: len 8; hex 80000000000002f3; asc         ;;
 4: len 8; hex 80000000000002f3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03bf41; asc    j   A;;
 7: len 8; hex 99bac36a1c03c292; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f4; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2468; asc      $h;;
 3: len 8; hex 80000000000002f4; asc         ;;
 4: len 8; hex 80000000000002f4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03c77e; asc    j   ~;;
 7: len 8; hex 99bac36a1c0412d1; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f5; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be24ed; asc      $ ;;
 3: len 8; hex 80000000000002f5; asc         ;;
 4: len 8; hex 80000000000002f5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0418ed; asc    j    ;;
 7: len 8; hex 99bac36a1c041c7b; asc    j   {;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f6; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2572; asc      %r;;
 3: len 8; hex 80000000000002f6; asc         ;;
 4: len 8; hex 80000000000002f6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c042171; asc    j  !q;;
 7: len 8; hex 99bac36a1c0424b3; asc    j  $ ;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f7; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be25f7; asc      % ;;
 3: len 8; hex 80000000000002f7; asc         ;;
 4: len 8; hex 80000000000002f7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0429cf; asc    j  ) ;;
 7: len 8; hex 99bac36a1c042ced; asc    j  , ;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f8; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be267c; asc      &|;;
 3: len 8; hex 80000000000002f8; asc         ;;
 4: len 8; hex 80000000000002f8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04316f; asc    j  1o;;
 7: len 8; hex 99bac36a1c0434b2; asc    j  4 ;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f9; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2701; asc      ' ;;
 3: len 8; hex 80000000000002f9; asc         ;;
 4: len 8; hex 80000000000002f9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c043930; asc    j  90;;
 7: len 8; hex 99bac36a1c043c4e; asc    j  <N;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fa; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2786; asc      ' ;;
 3: len 8; hex 80000000000002fa; asc         ;;
 4: len 8; hex 80000000000002fa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04411f; asc    j  A ;;
 7: len 8; hex 99bac36a1c044460; asc    j  D`;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fb; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be280b; asc      ( ;;
 3: len 8; hex 80000000000002fb; asc         ;;
 4: len 8; hex 80000000000002fb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c044935; asc    j  I5;;
 7: len 8; hex 99bac36a1c044d7f; asc    j  M ;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fc; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2890; asc      ( ;;
 3: len 8; hex 80000000000002fc; asc         ;;
 4: len 8; hex 80000000000002fc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04578a; asc    j  W ;;
 7: len 8; hex 99bac36a1c045c1d; asc    j  \ ;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fd; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2915; asc      ) ;;
 3: len 8; hex 80000000000002fd; asc         ;;
 4: len 8; hex 80000000000002fd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0463f2; asc    j  c ;;
 7: len 8; hex 99bac36a1c046748; asc    j  gH;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fe; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be299a; asc      ) ;;
 3: len 8; hex 80000000000002fe; asc         ;;
 4: len 8; hex 80000000000002fe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c046bcb; asc    j  k ;;
 7: len 8; hex 99bac36a1c046ef6; asc    j  n ;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ff; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2a1f; asc      * ;;
 3: len 8; hex 80000000000002ff; asc         ;;
 4: len 8; hex 80000000000002ff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0473d5; asc    j  s ;;
 7: len 8; hex 99bac36a1c047706; asc    j  w ;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000300; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2aa4; asc      * ;;
 3: len 8; hex 8000000000000300; asc         ;;
 4: len 8; hex 8000000000000300; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c047baf; asc    j  { ;;
 7: len 8; hex 99bac36a1c047ed1; asc    j  ~ ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000301; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2b29; asc      +);;
 3: len 8; hex 8000000000000301; asc         ;;
 4: len 8; hex 8000000000000301; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c048328; asc    j   (;;
 7: len 8; hex 99bac36a1c04876c; asc    j   l;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000302; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2bae; asc      + ;;
 3: len 8; hex 8000000000000302; asc         ;;
 4: len 8; hex 8000000000000302; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c048f65; asc    j   e;;
 7: len 8; hex 99bac36a1c049473; asc    j   s;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000303; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2c33; asc      ,3;;
 3: len 8; hex 8000000000000303; asc         ;;
 4: len 8; hex 8000000000000303; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0499c3; asc    j    ;;
 7: len 8; hex 99bac36a1c049d10; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000304; asc         ;;
 1: len 6; hex 000000026e03; asc     n ;;
 2: len 7; hex 02000001301524; asc     0 $;;
 3: len 8; hex 8000000000000304; asc         ;;
 4: len 8; hex 8000000000000304; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04a23c; asc    j   <;;
 7: len 8; hex 99bac36a1c04a5aa; asc    j    ;;
 8: len 8; hex 99bac36a1c0e4391; asc    j  C ;;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000305; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2d3d; asc      -=;;
 3: len 8; hex 8000000000000305; asc         ;;
 4: len 8; hex 8000000000000305; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04aa51; asc    j   Q;;
 7: len 8; hex 99bac36a1c04ae19; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000306; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2dc2; asc      - ;;
 3: len 8; hex 8000000000000306; asc         ;;
 4: len 8; hex 8000000000000306; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04b415; asc    j    ;;
 7: len 8; hex 99bac36a1c04b7f4; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000307; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2e47; asc      .G;;
 3: len 8; hex 8000000000000307; asc         ;;
 4: len 8; hex 8000000000000307; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04bd61; asc    j   a;;
 7: len 8; hex 99bac36a1c04c120; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000308; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2ecc; asc      . ;;
 3: len 8; hex 8000000000000308; asc         ;;
 4: len 8; hex 8000000000000308; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04cea9; asc    j    ;;
 7: len 8; hex 99bac36a1c04d379; asc    j   y;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000309; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2f51; asc      /Q;;
 3: len 8; hex 8000000000000309; asc         ;;
 4: len 8; hex 8000000000000309; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04dac1; asc    j    ;;
 7: len 8; hex 99bac36a1c04defa; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030a; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be2fd6; asc      / ;;
 3: len 8; hex 800000000000030a; asc         ;;
 4: len 8; hex 800000000000030a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04e472; asc    j   r;;
 7: len 8; hex 99bac36a1c04e851; asc    j   Q;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030b; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be305b; asc      0[;;
 3: len 8; hex 800000000000030b; asc         ;;
 4: len 8; hex 800000000000030b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04efce; asc    j    ;;
 7: len 8; hex 99bac36a1c04f34a; asc    j   J;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030c; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be30e0; asc      0 ;;
 3: len 8; hex 800000000000030c; asc         ;;
 4: len 8; hex 800000000000030c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c04f870; asc    j   p;;
 7: len 8; hex 99bac36a1c04fbc8; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030d; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3165; asc      1e;;
 3: len 8; hex 800000000000030d; asc         ;;
 4: len 8; hex 800000000000030d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05018d; asc    j    ;;
 7: len 8; hex 99bac36a1c0504c9; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030e; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be31ea; asc      1 ;;
 3: len 8; hex 800000000000030e; asc         ;;
 4: len 8; hex 800000000000030e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0509a6; asc    j    ;;
 7: len 8; hex 99bac36a1c050cea; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030f; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be326f; asc      2o;;
 3: len 8; hex 800000000000030f; asc         ;;
 4: len 8; hex 800000000000030f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0511ed; asc    j    ;;
 7: len 8; hex 99bac36a1c05150b; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000310; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be32f4; asc      2 ;;
 3: len 8; hex 8000000000000310; asc         ;;
 4: len 8; hex 8000000000000310; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c051a69; asc    j   i;;
 7: len 8; hex 99bac36a1c051d96; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000311; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3379; asc      3y;;
 3: len 8; hex 8000000000000311; asc         ;;
 4: len 8; hex 8000000000000311; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c052262; asc    j  "b;;
 7: len 8; hex 99bac36a1c052594; asc    j  % ;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000312; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be33fe; asc      3 ;;
 3: len 8; hex 8000000000000312; asc         ;;
 4: len 8; hex 8000000000000312; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c052b0d; asc    j  + ;;
 7: len 8; hex 99bac36a1c052e77; asc    j  .w;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000313; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3483; asc      4 ;;
 3: len 8; hex 8000000000000313; asc         ;;
 4: len 8; hex 8000000000000313; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0534a4; asc    j  4 ;;
 7: len 8; hex 99bac36a1c0538c9; asc    j  8 ;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000314; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3508; asc      5 ;;
 3: len 8; hex 8000000000000314; asc         ;;
 4: len 8; hex 8000000000000314; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0541f1; asc    j  A ;;
 7: len 8; hex 99bac36a1c0546bb; asc    j  F ;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000315; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be358d; asc      5 ;;
 3: len 8; hex 8000000000000315; asc         ;;
 4: len 8; hex 8000000000000315; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c054ec8; asc    j  N ;;
 7: len 8; hex 99bac36a1c05537a; asc    j  Sz;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000316; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3612; asc      6 ;;
 3: len 8; hex 8000000000000316; asc         ;;
 4: len 8; hex 8000000000000316; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c055ca0; asc    j  \ ;;
 7: len 8; hex 99bac36a1c0560fe; asc    j  ` ;;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000317; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3697; asc      6 ;;
 3: len 8; hex 8000000000000317; asc         ;;
 4: len 8; hex 8000000000000317; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0567ec; asc    j  g ;;
 7: len 8; hex 99bac36a1c056b32; asc    j  k2;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000318; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be371c; asc      7 ;;
 3: len 8; hex 8000000000000318; asc         ;;
 4: len 8; hex 8000000000000318; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c057014; asc    j  p ;;
 7: len 8; hex 99bac36a1c057379; asc    j  sy;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000319; asc         ;;
 1: len 6; hex 000000026eb9; asc     n ;;
 2: len 7; hex 02000001761918; asc     v  ;;
 3: len 8; hex 8000000000000319; asc         ;;
 4: len 8; hex 8000000000000319; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0578cd; asc    j  x ;;
 7: len 8; hex 99bac36a1c057bd4; asc    j  { ;;
 8: len 8; hex 99bac36a210384fd; asc    j!   ;;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031a; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3826; asc      8&;;
 3: len 8; hex 800000000000031a; asc         ;;
 4: len 8; hex 800000000000031a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c058313; asc    j    ;;
 7: len 8; hex 99bac36a1c058763; asc    j   c;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031b; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be38ab; asc      8 ;;
 3: len 8; hex 800000000000031b; asc         ;;
 4: len 8; hex 800000000000031b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c059040; asc    j   @;;
 7: len 8; hex 99bac36a1c05963b; asc    j   ;;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031c; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3930; asc      90;;
 3: len 8; hex 800000000000031c; asc         ;;
 4: len 8; hex 800000000000031c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c059add; asc    j    ;;
 7: len 8; hex 99bac36a1c059dff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031d; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be39b5; asc      9 ;;
 3: len 8; hex 800000000000031d; asc         ;;
 4: len 8; hex 800000000000031d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05a2ef; asc    j    ;;
 7: len 8; hex 99bac36a1c05a639; asc    j   9;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031e; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3a3a; asc      ::;;
 3: len 8; hex 800000000000031e; asc         ;;
 4: len 8; hex 800000000000031e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05ab0f; asc    j    ;;
 7: len 8; hex 99bac36a1c05ae1c; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031f; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3abf; asc      : ;;
 3: len 8; hex 800000000000031f; asc         ;;
 4: len 8; hex 800000000000031f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05b360; asc    j   `;;
 7: len 8; hex 99bac36a1c05b7dc; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000320; asc         ;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3b44; asc      ;D;;
 3: len 8; hex 8000000000000320; asc         ;;
 4: len 8; hex 8000000000000320; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05bfc0; asc    j    ;;
 7: len 8; hex 99bac36a1c05c3dd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000321; asc        !;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3bc9; asc      ; ;;
 3: len 8; hex 8000000000000321; asc        !;;
 4: len 8; hex 8000000000000321; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05ca1b; asc    j    ;;
 7: len 8; hex 99bac36a1c05ce73; asc    j   s;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000322; asc        ";;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3c4e; asc      <N;;
 3: len 8; hex 8000000000000322; asc        ";;
 4: len 8; hex 8000000000000322; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05d3b7; asc    j    ;;
 7: len 8; hex 99bac36a1c05d6e3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000323; asc        #;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3cd3; asc      < ;;
 3: len 8; hex 8000000000000323; asc        #;;
 4: len 8; hex 8000000000000323; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05db65; asc    j   e;;
 7: len 8; hex 99bac36a1c05de99; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000324; asc        $;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3d58; asc      =X;;
 3: len 8; hex 8000000000000324; asc        $;;
 4: len 8; hex 8000000000000324; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05e363; asc    j   c;;
 7: len 8; hex 99bac36a1c05e67d; asc    j   };;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000325; asc        %;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3ddd; asc      = ;;
 3: len 8; hex 8000000000000325; asc        %;;
 4: len 8; hex 8000000000000325; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05eb57; asc    j   W;;
 7: len 8; hex 99bac36a1c05eea9; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000326; asc        &;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3e62; asc      >b;;
 3: len 8; hex 8000000000000326; asc        &;;
 4: len 8; hex 8000000000000326; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05f34e; asc    j   N;;
 7: len 8; hex 99bac36a1c05f63d; asc    j   =;;
 8: SQL NULL;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000327; asc        ';;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3ee7; asc      > ;;
 3: len 8; hex 8000000000000327; asc        ';;
 4: len 8; hex 8000000000000327; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c05facb; asc    j    ;;
 7: len 8; hex 99bac36a1c05fddd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000328; asc        (;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001be3f6c; asc      ?l;;
 3: len 8; hex 8000000000000328; asc        (;;
 4: len 8; hex 8000000000000328; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0602de; asc    j    ;;
 7: len 8; hex 99bac36a1c060616; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000329; asc        );;
 1: len 6; hex 000000026e9b; asc     n ;;
 2: len 7; hex 02000001860c2a; asc       *;;
 3: len 8; hex 8000000000000329; asc        );;
 4: len 8; hex 8000000000000329; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c060aff; asc    j    ;;
 7: len 8; hex 99bac36a1c060e53; asc    j   S;;
 8: len 8; hex 99bac36a2102f389; asc    j!   ;;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032a; asc        *;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0114; asc        ;;
 3: len 8; hex 800000000000032a; asc        *;;
 4: len 8; hex 800000000000032a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c061310; asc    j    ;;
 7: len 8; hex 99bac36a1c061637; asc    j   7;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032b; asc        +;;
 1: len 6; hex 000000026e40; asc     n@;;
 2: len 7; hex 020000013d072c; asc     = ,;;
 3: len 8; hex 800000000000032b; asc        +;;
 4: len 8; hex 800000000000032b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c061ab8; asc    j    ;;
 7: len 8; hex 99bac36a1c061db9; asc    j    ;;
 8: len 8; hex 99bac36a1f0a70b7; asc    j  p ;;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032c; asc        ,;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf021e; asc        ;;
 3: len 8; hex 800000000000032c; asc        ,;;
 4: len 8; hex 800000000000032c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c062264; asc    j  "d;;
 7: len 8; hex 99bac36a1c062571; asc    j  %q;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032d; asc        -;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf02a3; asc        ;;
 3: len 8; hex 800000000000032d; asc        -;;
 4: len 8; hex 800000000000032d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06299d; asc    j  ) ;;
 7: len 8; hex 99bac36a1c062cb2; asc    j  , ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032e; asc        .;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0328; asc       (;;
 3: len 8; hex 800000000000032e; asc        .;;
 4: len 8; hex 800000000000032e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0631ce; asc    j  1 ;;
 7: len 8; hex 99bac36a1c06351e; asc    j  5 ;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032f; asc        /;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf03ad; asc        ;;
 3: len 8; hex 800000000000032f; asc        /;;
 4: len 8; hex 800000000000032f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c063c54; asc    j  <T;;
 7: len 8; hex 99bac36a1c064012; asc    j  @ ;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000330; asc        0;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0432; asc       2;;
 3: len 8; hex 8000000000000330; asc        0;;
 4: len 8; hex 8000000000000330; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c064690; asc    j  F ;;
 7: len 8; hex 99bac36a1c0649fa; asc    j  I ;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000331; asc        1;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf04b7; asc        ;;
 3: len 8; hex 8000000000000331; asc        1;;
 4: len 8; hex 8000000000000331; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c064ed1; asc    j  N ;;
 7: len 8; hex 99bac36a1c065252; asc    j  RR;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000332; asc        2;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf053c; asc       <;;
 3: len 8; hex 8000000000000332; asc        2;;
 4: len 8; hex 8000000000000332; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0656eb; asc    j  V ;;
 7: len 8; hex 99bac36a1c0659f6; asc    j  Y ;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000333; asc        3;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf05c1; asc        ;;
 3: len 8; hex 8000000000000333; asc        3;;
 4: len 8; hex 8000000000000333; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c065e7a; asc    j  ^z;;
 7: len 8; hex 99bac36a1c06632e; asc    j  c.;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000334; asc        4;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0646; asc       F;;
 3: len 8; hex 8000000000000334; asc        4;;
 4: len 8; hex 8000000000000334; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c066a68; asc    j  jh;;
 7: len 8; hex 99bac36a1c066dad; asc    j  m ;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000335; asc        5;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf06cb; asc        ;;
 3: len 8; hex 8000000000000335; asc        5;;
 4: len 8; hex 8000000000000335; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06750f; asc    j  u ;;
 7: len 8; hex 99bac36a1c06792c; asc    j  y,;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000336; asc        6;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0750; asc       P;;
 3: len 8; hex 8000000000000336; asc        6;;
 4: len 8; hex 8000000000000336; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c068064; asc    j   d;;
 7: len 8; hex 99bac36a1c06839a; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000337; asc        7;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf07d5; asc        ;;
 3: len 8; hex 8000000000000337; asc        7;;
 4: len 8; hex 8000000000000337; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06888c; asc    j    ;;
 7: len 8; hex 99bac36a1c068bde; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000338; asc        8;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf085a; asc       Z;;
 3: len 8; hex 8000000000000338; asc        8;;
 4: len 8; hex 8000000000000338; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c069310; asc    j    ;;
 7: len 8; hex 99bac36a1c06973d; asc    j   =;;
 8: SQL NULL;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000339; asc        9;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf08df; asc        ;;
 3: len 8; hex 8000000000000339; asc        9;;
 4: len 8; hex 8000000000000339; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c069e75; asc    j   u;;
 7: len 8; hex 99bac36a1c06a1f5; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033a; asc        :;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0964; asc       d;;
 3: len 8; hex 800000000000033a; asc        :;;
 4: len 8; hex 800000000000033a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06a7d6; asc    j    ;;
 7: len 8; hex 99bac36a1c06ab20; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033b; asc        ;;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf09e9; asc        ;;
 3: len 8; hex 800000000000033b; asc        ;;;
 4: len 8; hex 800000000000033b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06aff7; asc    j    ;;
 7: len 8; hex 99bac36a1c06b31a; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033c; asc        <;;
 1: len 6; hex 000000026f0d; asc     o ;;
 2: len 7; hex 01000001200e9f; asc        ;;
 3: len 8; hex 800000000000033c; asc        <;;
 4: len 8; hex 800000000000033c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06b962; asc    j   b;;
 7: len 8; hex 99bac36a1c06bde6; asc    j    ;;
 8: len 8; hex 99bac36a230a2578; asc    j# %x;;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033d; asc        =;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0af3; asc        ;;
 3: len 8; hex 800000000000033d; asc        =;;
 4: len 8; hex 800000000000033d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06c4c9; asc    j    ;;
 7: len 8; hex 99bac36a1c06c7dd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033e; asc        >;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0b78; asc       x;;
 3: len 8; hex 800000000000033e; asc        >;;
 4: len 8; hex 800000000000033e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06ccde; asc    j    ;;
 7: len 8; hex 99bac36a1c06d01e; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033f; asc        ?;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0bfd; asc        ;;
 3: len 8; hex 800000000000033f; asc        ?;;
 4: len 8; hex 800000000000033f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06d5dd; asc    j    ;;
 7: len 8; hex 99bac36a1c06d90a; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000340; asc        @;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0c82; asc        ;;
 3: len 8; hex 8000000000000340; asc        @;;
 4: len 8; hex 8000000000000340; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06dd9e; asc    j    ;;
 7: len 8; hex 99bac36a1c06e20a; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000341; asc        A;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0d07; asc        ;;
 3: len 8; hex 8000000000000341; asc        A;;
 4: len 8; hex 8000000000000341; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06e816; asc    j    ;;
 7: len 8; hex 99bac36a1c06eb40; asc    j   @;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000342; asc        B;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0d8c; asc        ;;
 3: len 8; hex 8000000000000342; asc        B;;
 4: len 8; hex 8000000000000342; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06f040; asc    j   @;;
 7: len 8; hex 99bac36a1c06f3b8; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000343; asc        C;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0e11; asc        ;;
 3: len 8; hex 8000000000000343; asc        C;;
 4: len 8; hex 8000000000000343; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c06f878; asc    j   x;;
 7: len 8; hex 99bac36a1c06fba3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000344; asc        D;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0e96; asc        ;;
 3: len 8; hex 8000000000000344; asc        D;;
 4: len 8; hex 8000000000000344; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07008b; asc    j    ;;
 7: len 8; hex 99bac36a1c0703d4; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000345; asc        E;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0f1b; asc        ;;
 3: len 8; hex 8000000000000345; asc        E;;
 4: len 8; hex 8000000000000345; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c070db3; asc    j    ;;
 7: len 8; hex 99bac36a1c0711dd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000346; asc        F;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf0fa0; asc        ;;
 3: len 8; hex 8000000000000346; asc        F;;
 4: len 8; hex 8000000000000346; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c071b04; asc    j    ;;
 7: len 8; hex 99bac36a1c071fc6; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000347; asc        G;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1025; asc       %;;
 3: len 8; hex 8000000000000347; asc        G;;
 4: len 8; hex 8000000000000347; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c072645; asc    j  &E;;
 7: len 8; hex 99bac36a1c0729e6; asc    j  ) ;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000348; asc        H;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf10aa; asc        ;;
 3: len 8; hex 8000000000000348; asc        H;;
 4: len 8; hex 8000000000000348; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c072eda; asc    j  . ;;
 7: len 8; hex 99bac36a1c07321c; asc    j  2 ;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000349; asc        I;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf112f; asc       /;;
 3: len 8; hex 8000000000000349; asc        I;;
 4: len 8; hex 8000000000000349; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c073711; asc    j  7 ;;
 7: len 8; hex 99bac36a1c073a7e; asc    j  :~;;
 8: SQL NULL;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034a; asc        J;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf11b4; asc        ;;
 3: len 8; hex 800000000000034a; asc        J;;
 4: len 8; hex 800000000000034a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c073f85; asc    j  ? ;;
 7: len 8; hex 99bac36a1c0742c1; asc    j  B ;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034b; asc        K;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1239; asc       9;;
 3: len 8; hex 800000000000034b; asc        K;;
 4: len 8; hex 800000000000034b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07477b; asc    j  G{;;
 7: len 8; hex 99bac36a1c074a98; asc    j  J ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034c; asc        L;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf12be; asc        ;;
 3: len 8; hex 800000000000034c; asc        L;;
 4: len 8; hex 800000000000034c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07505e; asc    j  P^;;
 7: len 8; hex 99bac36a1c07552f; asc    j  U/;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034d; asc        M;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1343; asc       C;;
 3: len 8; hex 800000000000034d; asc        M;;
 4: len 8; hex 800000000000034d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c075b3f; asc    j  [?;;
 7: len 8; hex 99bac36a1c075ed4; asc    j  ^ ;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034e; asc        N;;
 1: len 6; hex 000000026e85; asc     n ;;
 2: len 7; hex 01000001842923; asc      )#;;
 3: len 8; hex 800000000000034e; asc        N;;
 4: len 8; hex 800000000000034e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0763a0; asc    j  c ;;
 7: len 8; hex 99bac36a1c07678b; asc    j  g ;;
 8: len 8; hex 99bac36a20034871; asc    j  Hq;;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034f; asc        O;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf144d; asc       M;;
 3: len 8; hex 800000000000034f; asc        O;;
 4: len 8; hex 800000000000034f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c076db3; asc    j  m ;;
 7: len 8; hex 99bac36a1c0771ba; asc    j  q ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000350; asc        P;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf14d2; asc        ;;
 3: len 8; hex 8000000000000350; asc        P;;
 4: len 8; hex 8000000000000350; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07781d; asc    j  x ;;
 7: len 8; hex 99bac36a1c077e1a; asc    j  ~ ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000351; asc        Q;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1557; asc       W;;
 3: len 8; hex 8000000000000351; asc        Q;;
 4: len 8; hex 8000000000000351; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07865c; asc    j   \;;
 7: len 8; hex 99bac36a1c078a04; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000352; asc        R;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf15dc; asc        ;;
 3: len 8; hex 8000000000000352; asc        R;;
 4: len 8; hex 8000000000000352; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c079131; asc    j   1;;
 7: len 8; hex 99bac36a1c079564; asc    j   d;;
 8: SQL NULL;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000353; asc        S;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1661; asc       a;;
 3: len 8; hex 8000000000000353; asc        S;;
 4: len 8; hex 8000000000000353; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c079bca; asc    j    ;;
 7: len 8; hex 99bac36a1c079fc9; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000354; asc        T;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf16e6; asc        ;;
 3: len 8; hex 8000000000000354; asc        T;;
 4: len 8; hex 8000000000000354; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07a663; asc    j   c;;
 7: len 8; hex 99bac36a1c07a9e6; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000355; asc        U;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf176b; asc       k;;
 3: len 8; hex 8000000000000355; asc        U;;
 4: len 8; hex 8000000000000355; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07aff7; asc    j    ;;
 7: len 8; hex 99bac36a1c07b4ff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000356; asc        V;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf17f0; asc        ;;
 3: len 8; hex 8000000000000356; asc        V;;
 4: len 8; hex 8000000000000356; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07bb6b; asc    j   k;;
 7: len 8; hex 99bac36a1c07bf34; asc    j   4;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000357; asc        W;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1875; asc       u;;
 3: len 8; hex 8000000000000357; asc        W;;
 4: len 8; hex 8000000000000357; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07c65a; asc    j   Z;;
 7: len 8; hex 99bac36a1c07caf3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000358; asc        X;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf18fa; asc        ;;
 3: len 8; hex 8000000000000358; asc        X;;
 4: len 8; hex 8000000000000358; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07d39b; asc    j    ;;
 7: len 8; hex 99bac36a1c07d7e3; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000359; asc        Y;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf197f; asc        ;;
 3: len 8; hex 8000000000000359; asc        Y;;
 4: len 8; hex 8000000000000359; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07de98; asc    j    ;;
 7: len 8; hex 99bac36a1c07e1fd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035a; asc        Z;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1a04; asc        ;;
 3: len 8; hex 800000000000035a; asc        Z;;
 4: len 8; hex 800000000000035a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07e785; asc    j    ;;
 7: len 8; hex 99bac36a1c07eaff; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035b; asc        [;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1a89; asc        ;;
 3: len 8; hex 800000000000035b; asc        [;;
 4: len 8; hex 800000000000035b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07f05b; asc    j   [;;
 7: len 8; hex 99bac36a1c07f408; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035c; asc        \;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1b0e; asc        ;;
 3: len 8; hex 800000000000035c; asc        \;;
 4: len 8; hex 800000000000035c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c07f993; asc    j    ;;
 7: len 8; hex 99bac36a1c07fd01; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035d; asc        ];;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1b93; asc        ;;
 3: len 8; hex 800000000000035d; asc        ];;
 4: len 8; hex 800000000000035d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c080231; asc    j   1;;
 7: len 8; hex 99bac36a1c080572; asc    j   r;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035e; asc        ^;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1c18; asc        ;;
 3: len 8; hex 800000000000035e; asc        ^;;
 4: len 8; hex 800000000000035e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c080bc1; asc    j    ;;
 7: len 8; hex 99bac36a1c081048; asc    j   H;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035f; asc        _;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1c9d; asc        ;;
 3: len 8; hex 800000000000035f; asc        _;;
 4: len 8; hex 800000000000035f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0816e5; asc    j    ;;
 7: len 8; hex 99bac36a1c081acb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000360; asc        `;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1d22; asc       ";;
 3: len 8; hex 8000000000000360; asc        `;;
 4: len 8; hex 8000000000000360; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c081fd4; asc    j    ;;
 7: len 8; hex 99bac36a1c082302; asc    j  # ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000361; asc        a;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1da7; asc        ;;
 3: len 8; hex 8000000000000361; asc        a;;
 4: len 8; hex 8000000000000361; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0827e6; asc    j  ' ;;
 7: len 8; hex 99bac36a1c082b36; asc    j  +6;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000362; asc        b;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1e2c; asc       ,;;
 3: len 8; hex 8000000000000362; asc        b;;
 4: len 8; hex 8000000000000362; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c083019; asc    j  0 ;;
 7: len 8; hex 99bac36a1c08335c; asc    j  3\;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000363; asc        c;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1eb1; asc        ;;
 3: len 8; hex 8000000000000363; asc        c;;
 4: len 8; hex 8000000000000363; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c083853; asc    j  8S;;
 7: len 8; hex 99bac36a1c083ba6; asc    j  ; ;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000364; asc        d;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1f36; asc       6;;
 3: len 8; hex 8000000000000364; asc        d;;
 4: len 8; hex 8000000000000364; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c084161; asc    j  Aa;;
 7: len 8; hex 99bac36a1c0844b7; asc    j  D ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000365; asc        e;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf1fbb; asc        ;;
 3: len 8; hex 8000000000000365; asc        e;;
 4: len 8; hex 8000000000000365; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c084b07; asc    j  K ;;
 7: len 8; hex 99bac36a1c084e98; asc    j  N ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000366; asc        f;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf2040; asc       @;;
 3: len 8; hex 8000000000000366; asc        f;;
 4: len 8; hex 8000000000000366; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0854e4; asc    j  T ;;
 7: len 8; hex 99bac36a1c085942; asc    j  YB;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000367; asc        g;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf20c5; asc        ;;
 3: len 8; hex 8000000000000367; asc        g;;
 4: len 8; hex 8000000000000367; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c085ef8; asc    j  ^ ;;
 7: len 8; hex 99bac36a1c08628c; asc    j  b ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000368; asc        h;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf214a; asc      !J;;
 3: len 8; hex 8000000000000368; asc        h;;
 4: len 8; hex 8000000000000368; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c086849; asc    j  hI;;
 7: len 8; hex 99bac36a1c086bdf; asc    j  k ;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000369; asc        i;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf21cf; asc      ! ;;
 3: len 8; hex 8000000000000369; asc        i;;
 4: len 8; hex 8000000000000369; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c0870a4; asc    j  p ;;
 7: len 8; hex 99bac36a1c087446; asc    j  tF;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036a; asc        j;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf2254; asc      "T;;
 3: len 8; hex 800000000000036a; asc        j;;
 4: len 8; hex 800000000000036a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c08795d; asc    j  y];;
 7: len 8; hex 99bac36a1c087cab; asc    j  | ;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036b; asc        k;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf22d9; asc      " ;;
 3: len 8; hex 800000000000036b; asc        k;;
 4: len 8; hex 800000000000036b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c088179; asc    j   y;;
 7: len 8; hex 99bac36a1c088539; asc    j   9;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036c; asc        l;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf235e; asc      #^;;
 3: len 8; hex 800000000000036c; asc        l;;
 4: len 8; hex 800000000000036c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c088a75; asc    j   u;;
 7: len 8; hex 99bac36a1c088da4; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036d; asc        m;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf23e3; asc      # ;;
 3: len 8; hex 800000000000036d; asc        m;;
 4: len 8; hex 800000000000036d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c08927b; asc    j   {;;
 7: len 8; hex 99bac36a1c0895cb; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036e; asc        n;;
 1: len 6; hex 000000026dcd; asc     m ;;
 2: len 7; hex 01000001bf2468; asc      $h;;
 3: len 8; hex 800000000000036e; asc        n;;
 4: len 8; hex 800000000000036e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c089a7f; asc    j    ;;
 7: len 8; hex 99bac36a1c089dcd; asc    j    ;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f2; asc         ;;
 1: len 6; hex 000000026f17; asc     o ;;
 2: len 7; hex 020000012d085c; asc     - \;;
 3: len 8; hex 80000000000002f2; asc         ;;
 4: len 8; hex 80000000000002f2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36a1c03b6fe; asc    j    ;;
 7: len 8; hex 99bac36a1c03ba69; asc    j   i;;
 8: len 8; hex 99bac36a24004c60; asc    j$ L`;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 93 page no 12 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 159521 lock mode S locks rec but not gap waiting
Record lock, heap no 213 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 800000000000036e; asc        n;;
 1: len 6; hex 000000026f39; asc     o9;;
 2: len 7; hex 010000013f0bd8; asc     ?  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d333633353834393334; asc tk-363584934;;
 5: len 8; hex 99bac36a240711a3; asc    j$   ;;
 6: len 8; hex 99bac36a240711a3; asc    j$   ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V1 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:44:26 135358810506816
*** (1) TRANSACTION:
TRANSACTION 184417, ACTIVE 1 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9040, OS thread handle 135358650984000, query id 947977 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 749

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 105 page no 8 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 184417 lock_mode X locks rec but not gap
Record lock, heap no 225 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000002ed; asc         ;;
 1: len 6; hex 00000002d061; asc      a;;
 2: len 7; hex 0200000198028e; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 11; hex 746b2d3832313937333435; asc tk-82197345;;
 5: len 8; hex 99bac36b19081930; asc    k   0;;
 6: len 8; hex 99bac36b19081930; asc    k   0;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 103 page no 22 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 184417 lock_mode X locks rec but not gap waiting
Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ed; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100267c; asc      &|;;
 3: len 8; hex 80000000000002ed; asc         ;;
 4: len 8; hex 80000000000002ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105fc0a; asc    k    ;;
 7: len 8; hex 99bac36b1105fef0; asc    k    ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 184399, ACTIVE 1 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 47 lock struct(s), heap size 24696, 4516 row lock(s), undo log entries 745
MySQL thread id 9000, OS thread handle 135358149797440, query id 948746 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (749, 3749, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 103 page no 22 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 184399 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000274; asc        t;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139272f; asc     9'/;;
 3: len 8; hex 8000000000000274; asc        t;;
 4: len 8; hex 8000000000000274; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110289bf; asc    k    ;;
 7: len 8; hex 99bac36b11028cc1; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000275; asc        u;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013927b4; asc     9' ;;
 3: len 8; hex 8000000000000275; asc        u;;
 4: len 8; hex 8000000000000275; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11029080; asc    k    ;;
 7: len 8; hex 99bac36b11029378; asc    k   x;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000276; asc        v;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392839; asc     9(9;;
 3: len 8; hex 8000000000000276; asc        v;;
 4: len 8; hex 8000000000000276; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102977a; asc    k   z;;
 7: len 8; hex 99bac36b11029ae4; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000277; asc        w;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013928be; asc     9( ;;
 3: len 8; hex 8000000000000277; asc        w;;
 4: len 8; hex 8000000000000277; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11029f49; asc    k   I;;
 7: len 8; hex 99bac36b1102a271; asc    k   q;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000278; asc        x;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392943; asc     9)C;;
 3: len 8; hex 8000000000000278; asc        x;;
 4: len 8; hex 8000000000000278; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102a683; asc    k    ;;
 7: len 8; hex 99bac36b1102a976; asc    k   v;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000279; asc        y;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013929c8; asc     9) ;;
 3: len 8; hex 8000000000000279; asc        y;;
 4: len 8; hex 8000000000000279; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102ad7d; asc    k   };;
 7: len 8; hex 99bac36b1102b070; asc    k   p;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027a; asc        z;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392a4d; asc     9*M;;
 3: len 8; hex 800000000000027a; asc        z;;
 4: len 8; hex 800000000000027a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102b44c; asc    k   L;;
 7: len 8; hex 99bac36b1102b757; asc    k   W;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027b; asc        {;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392ad2; asc     9* ;;
 3: len 8; hex 800000000000027b; asc        {;;
 4: len 8; hex 800000000000027b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102bb3e; asc    k   >;;
 7: len 8; hex 99bac36b1102be31; asc    k   1;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027c; asc        |;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392b57; asc     9+W;;
 3: len 8; hex 800000000000027c; asc        |;;
 4: len 8; hex 800000000000027c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102c201; asc    k    ;;
 7: len 8; hex 99bac36b1102c50c; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027d; asc        };;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392bdc; asc     9+ ;;
 3: len 8; hex 800000000000027d; asc        };;
 4: len 8; hex 800000000000027d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102c92b; asc    k   +;;
 7: len 8; hex 99bac36b1102cc27; asc    k   ';;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027e; asc        ~;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392c61; asc     9,a;;
 3: len 8; hex 800000000000027e; asc        ~;;
 4: len 8; hex 800000000000027e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102cff7; asc    k    ;;
 7: len 8; hex 99bac36b1102d2d3; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000027f; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392ce6; asc     9, ;;
 3: len 8; hex 800000000000027f; asc         ;;
 4: len 8; hex 800000000000027f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102d6d4; asc    k    ;;
 7: len 8; hex 99bac36b1102d9ec; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000280; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392d6b; asc     9-k;;
 3: len 8; hex 8000000000000280; asc         ;;
 4: len 8; hex 8000000000000280; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102de3b; asc    k   ;;;
 7: len 8; hex 99bac36b1102e15c; asc    k   \;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000281; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392df0; asc     9- ;;
 3: len 8; hex 8000000000000281; asc         ;;
 4: len 8; hex 8000000000000281; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102e527; asc    k   ';;
 7: len 8; hex 99bac36b1102e84f; asc    k   O;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000282; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392e75; asc     9.u;;
 3: len 8; hex 8000000000000282; asc         ;;
 4: len 8; hex 8000000000000282; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102ec2f; asc    k   /;;
 7: len 8; hex 99bac36b1102ef47; asc    k   G;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000283; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392efa; asc     9. ;;
 3: len 8; hex 8000000000000283; asc         ;;
 4: len 8; hex 8000000000000283; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102f329; asc    k   );;
 7: len 8; hex 99bac36b1102f627; asc    k   ';;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000284; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001392f7f; asc     9/ ;;
 3: len 8; hex 8000000000000284; asc         ;;
 4: len 8; hex 8000000000000284; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1102fa6b; asc    k   k;;
 7: len 8; hex 99bac36b1102fd75; asc    k   u;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000285; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393004; asc     90 ;;
 3: len 8; hex 8000000000000285; asc         ;;
 4: len 8; hex 8000000000000285; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11030163; asc    k   c;;
 7: len 8; hex 99bac36b11030468; asc    k   h;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000286; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393089; asc     90 ;;
 3: len 8; hex 8000000000000286; asc         ;;
 4: len 8; hex 8000000000000286; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11030978; asc    k   x;;
 7: len 8; hex 99bac36b11030cff; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000287; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139310e; asc     91 ;;
 3: len 8; hex 8000000000000287; asc         ;;
 4: len 8; hex 8000000000000287; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11031217; asc    k    ;;
 7: len 8; hex 99bac36b11031506; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000288; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393193; asc     91 ;;
 3: len 8; hex 8000000000000288; asc         ;;
 4: len 8; hex 8000000000000288; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11031a0f; asc    k    ;;
 7: len 8; hex 99bac36b11031d22; asc    k   ";;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000289; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393218; asc     92 ;;
 3: len 8; hex 8000000000000289; asc         ;;
 4: len 8; hex 8000000000000289; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11032110; asc    k  ! ;;
 7: len 8; hex 99bac36b110323f6; asc    k  # ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028a; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139329d; asc     92 ;;
 3: len 8; hex 800000000000028a; asc         ;;
 4: len 8; hex 800000000000028a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110327fa; asc    k  ' ;;
 7: len 8; hex 99bac36b11032b12; asc    k  + ;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028b; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393322; asc     93";;
 3: len 8; hex 800000000000028b; asc         ;;
 4: len 8; hex 800000000000028b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11032f30; asc    k  /0;;
 7: len 8; hex 99bac36b1103322e; asc    k  2.;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028c; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013933a7; asc     93 ;;
 3: len 8; hex 800000000000028c; asc         ;;
 4: len 8; hex 800000000000028c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11033671; asc    k  6q;;
 7: len 8; hex 99bac36b1103398c; asc    k  9 ;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028d; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139342c; asc     94,;;
 3: len 8; hex 800000000000028d; asc         ;;
 4: len 8; hex 800000000000028d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11033d5d; asc    k  =];;
 7: len 8; hex 99bac36b11034054; asc    k  @T;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028e; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013934b1; asc     94 ;;
 3: len 8; hex 800000000000028e; asc         ;;
 4: len 8; hex 800000000000028e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103442b; asc    k  D+;;
 7: len 8; hex 99bac36b1103473b; asc    k  G;;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000028f; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393536; asc     956;;
 3: len 8; hex 800000000000028f; asc         ;;
 4: len 8; hex 800000000000028f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11034b02; asc    k  K ;;
 7: len 8; hex 99bac36b11034e07; asc    k  N ;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000290; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013935bb; asc     95 ;;
 3: len 8; hex 8000000000000290; asc         ;;
 4: len 8; hex 8000000000000290; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110351f5; asc    k  Q ;;
 7: len 8; hex 99bac36b11035574; asc    k  Ut;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000291; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393640; asc     96@;;
 3: len 8; hex 8000000000000291; asc         ;;
 4: len 8; hex 8000000000000291; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103595b; asc    k  Y[;;
 7: len 8; hex 99bac36b11035c61; asc    k  \a;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000292; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013936c5; asc     96 ;;
 3: len 8; hex 8000000000000292; asc         ;;
 4: len 8; hex 8000000000000292; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11036063; asc    k  `c;;
 7: len 8; hex 99bac36b11036358; asc    k  cX;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000293; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139374a; asc     97J;;
 3: len 8; hex 8000000000000293; asc         ;;
 4: len 8; hex 8000000000000293; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11036714; asc    k  g ;;
 7: len 8; hex 99bac36b110369d2; asc    k  i ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000294; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013937cf; asc     97 ;;
 3: len 8; hex 8000000000000294; asc         ;;
 4: len 8; hex 8000000000000294; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11036ddb; asc    k  m ;;
 7: len 8; hex 99bac36b110370c6; asc    k  p ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000295; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393854; asc     98T;;
 3: len 8; hex 8000000000000295; asc         ;;
 4: len 8; hex 8000000000000295; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110374da; asc    k  t ;;
 7: len 8; hex 99bac36b110377c2; asc    k  w ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000296; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013938d9; asc     98 ;;
 3: len 8; hex 8000000000000296; asc         ;;
 4: len 8; hex 8000000000000296; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11037b7f; asc    k  { ;;
 7: len 8; hex 99bac36b11037ea1; asc    k  ~ ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000297; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000139395e; asc     99^;;
 3: len 8; hex 8000000000000297; asc         ;;
 4: len 8; hex 8000000000000297; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110382c3; asc    k    ;;
 7: len 8; hex 99bac36b110385bf; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000298; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000013939e3; asc     99 ;;
 3: len 8; hex 8000000000000298; asc         ;;
 4: len 8; hex 8000000000000298; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11038965; asc    k   e;;
 7: len 8; hex 99bac36b11038c60; asc    k   `;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000299; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393a68; asc     9:h;;
 3: len 8; hex 8000000000000299; asc         ;;
 4: len 8; hex 8000000000000299; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110390d4; asc    k    ;;
 7: len 8; hex 99bac36b110393f2; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029a; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393aed; asc     9: ;;
 3: len 8; hex 800000000000029a; asc         ;;
 4: len 8; hex 800000000000029a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110397da; asc    k    ;;
 7: len 8; hex 99bac36b11039ace; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029b; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393b72; asc     9;r;;
 3: len 8; hex 800000000000029b; asc         ;;
 4: len 8; hex 800000000000029b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11039ecd; asc    k    ;;
 7: len 8; hex 99bac36b1103a1ce; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029c; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393bf7; asc     9; ;;
 3: len 8; hex 800000000000029c; asc         ;;
 4: len 8; hex 800000000000029c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103a5c9; asc    k    ;;
 7: len 8; hex 99bac36b1103a8ae; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029d; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393c7c; asc     9<|;;
 3: len 8; hex 800000000000029d; asc         ;;
 4: len 8; hex 800000000000029d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103ad07; asc    k    ;;
 7: len 8; hex 99bac36b1103b000; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029e; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393d01; asc     9= ;;
 3: len 8; hex 800000000000029e; asc         ;;
 4: len 8; hex 800000000000029e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103b381; asc    k    ;;
 7: len 8; hex 99bac36b1103b654; asc    k   T;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000029f; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393d86; asc     9= ;;
 3: len 8; hex 800000000000029f; asc         ;;
 4: len 8; hex 800000000000029f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103ba14; asc    k    ;;
 7: len 8; hex 99bac36b1103bcfb; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a0; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393e0b; asc     9> ;;
 3: len 8; hex 80000000000002a0; asc         ;;
 4: len 8; hex 80000000000002a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103c0a7; asc    k    ;;
 7: len 8; hex 99bac36b1103c385; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a1; asc         ;;
 1: len 6; hex 00000002cf6b; asc      k;;
 2: len 7; hex 020000017f07aa; asc        ;;
 3: len 8; hex 80000000000002a1; asc         ;;
 4: len 8; hex 80000000000002a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103c705; asc    k    ;;
 7: len 8; hex 99bac36b1103ca3d; asc    k   =;;
 8: len 8; hex 99bac36b130204f2; asc    k    ;;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a2; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393f15; asc     9? ;;
 3: len 8; hex 80000000000002a2; asc         ;;
 4: len 8; hex 80000000000002a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103cf2c; asc    k   ,;;
 7: len 8; hex 99bac36b1103d205; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a3; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001393f9a; asc     9? ;;
 3: len 8; hex 80000000000002a3; asc         ;;
 4: len 8; hex 80000000000002a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103d5c0; asc    k    ;;
 7: len 8; hex 99bac36b1103d898; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a4; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100008f; asc        ;;
 3: len 8; hex 80000000000002a4; asc         ;;
 4: len 8; hex 80000000000002a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103dcb1; asc    k    ;;
 7: len 8; hex 99bac36b1103df81; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a5; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000114; asc        ;;
 3: len 8; hex 80000000000002a5; asc         ;;
 4: len 8; hex 80000000000002a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103e361; asc    k   a;;
 7: len 8; hex 99bac36b1103e654; asc    k   T;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a6; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000199; asc        ;;
 3: len 8; hex 80000000000002a6; asc         ;;
 4: len 8; hex 80000000000002a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103eada; asc    k    ;;
 7: len 8; hex 99bac36b1103ede0; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a7; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100021e; asc        ;;
 3: len 8; hex 80000000000002a7; asc         ;;
 4: len 8; hex 80000000000002a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103f1fa; asc    k    ;;
 7: len 8; hex 99bac36b1103f4f2; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a8; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010002a3; asc        ;;
 3: len 8; hex 80000000000002a8; asc         ;;
 4: len 8; hex 80000000000002a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1103f932; asc    k   2;;
 7: len 8; hex 99bac36b1103fc4d; asc    k   M;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002a9; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000328; asc       (;;
 3: len 8; hex 80000000000002a9; asc         ;;
 4: len 8; hex 80000000000002a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11040067; asc    k   g;;
 7: len 8; hex 99bac36b11040361; asc    k   a;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002aa; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010003ad; asc        ;;
 3: len 8; hex 80000000000002aa; asc         ;;
 4: len 8; hex 80000000000002aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11040788; asc    k    ;;
 7: len 8; hex 99bac36b11040b59; asc    k   Y;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ab; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000432; asc       2;;
 3: len 8; hex 80000000000002ab; asc         ;;
 4: len 8; hex 80000000000002ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11040f9d; asc    k    ;;
 7: len 8; hex 99bac36b11041284; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ac; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010004b7; asc        ;;
 3: len 8; hex 80000000000002ac; asc         ;;
 4: len 8; hex 80000000000002ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110416eb; asc    k    ;;
 7: len 8; hex 99bac36b11041a38; asc    k   8;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ad; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100053c; asc       <;;
 3: len 8; hex 80000000000002ad; asc         ;;
 4: len 8; hex 80000000000002ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11041e0e; asc    k    ;;
 7: len 8; hex 99bac36b11042138; asc    k  !8;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ae; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010005c1; asc        ;;
 3: len 8; hex 80000000000002ae; asc         ;;
 4: len 8; hex 80000000000002ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11042507; asc    k  % ;;
 7: len 8; hex 99bac36b11042807; asc    k  ( ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002af; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000646; asc       F;;
 3: len 8; hex 80000000000002af; asc         ;;
 4: len 8; hex 80000000000002af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11042be7; asc    k  + ;;
 7: len 8; hex 99bac36b11042f24; asc    k  /$;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b0; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010006cb; asc        ;;
 3: len 8; hex 80000000000002b0; asc         ;;
 4: len 8; hex 80000000000002b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11043321; asc    k  3!;;
 7: len 8; hex 99bac36b110435ef; asc    k  5 ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b1; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000750; asc       P;;
 3: len 8; hex 80000000000002b1; asc         ;;
 4: len 8; hex 80000000000002b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11043993; asc    k  9 ;;
 7: len 8; hex 99bac36b11043c6e; asc    k  <n;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b2; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010007d5; asc        ;;
 3: len 8; hex 80000000000002b2; asc         ;;
 4: len 8; hex 80000000000002b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104405f; asc    k  @_;;
 7: len 8; hex 99bac36b1104433c; asc    k  C<;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b3; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100085a; asc       Z;;
 3: len 8; hex 80000000000002b3; asc         ;;
 4: len 8; hex 80000000000002b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11044860; asc    k  H`;;
 7: len 8; hex 99bac36b11044b8e; asc    k  K ;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b4; asc         ;;
 1: len 6; hex 00000002cf53; asc      S;;
 2: len 7; hex 020000017e2d3d; asc     ~-=;;
 3: len 8; hex 80000000000002b4; asc         ;;
 4: len 8; hex 80000000000002b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11044fe8; asc    k  O ;;
 7: len 8; hex 99bac36b1104536b; asc    k  Sk;;
 8: len 8; hex 99bac36b110eb7e3; asc    k    ;;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b5; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000964; asc       d;;
 3: len 8; hex 80000000000002b5; asc         ;;
 4: len 8; hex 80000000000002b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104579c; asc    k  W ;;
 7: len 8; hex 99bac36b11045a7a; asc    k  Zz;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b6; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010009e9; asc        ;;
 3: len 8; hex 80000000000002b6; asc         ;;
 4: len 8; hex 80000000000002b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11045e58; asc    k  ^X;;
 7: len 8; hex 99bac36b1104613d; asc    k  a=;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b7; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000a6e; asc       n;;
 3: len 8; hex 80000000000002b7; asc         ;;
 4: len 8; hex 80000000000002b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11046565; asc    k  ee;;
 7: len 8; hex 99bac36b11046851; asc    k  hQ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b8; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000af3; asc        ;;
 3: len 8; hex 80000000000002b8; asc         ;;
 4: len 8; hex 80000000000002b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11046c4b; asc    k  lK;;
 7: len 8; hex 99bac36b11046faf; asc    k  o ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002b9; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000b78; asc       x;;
 3: len 8; hex 80000000000002b9; asc         ;;
 4: len 8; hex 80000000000002b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110473ce; asc    k  s ;;
 7: len 8; hex 99bac36b110476c4; asc    k  v ;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ba; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000bfd; asc        ;;
 3: len 8; hex 80000000000002ba; asc         ;;
 4: len 8; hex 80000000000002ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11047a92; asc    k  z ;;
 7: len 8; hex 99bac36b11047d98; asc    k  } ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bc; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000d07; asc        ;;
 3: len 8; hex 80000000000002bc; asc         ;;
 4: len 8; hex 80000000000002bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110488e0; asc    k    ;;
 7: len 8; hex 99bac36b11048bd4; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bd; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000d8c; asc        ;;
 3: len 8; hex 80000000000002bd; asc         ;;
 4: len 8; hex 80000000000002bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11048fcb; asc    k    ;;
 7: len 8; hex 99bac36b110492d6; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002be; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000e11; asc        ;;
 3: len 8; hex 80000000000002be; asc         ;;
 4: len 8; hex 80000000000002be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11049690; asc    k    ;;
 7: len 8; hex 99bac36b1104998e; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bf; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000e96; asc        ;;
 3: len 8; hex 80000000000002bf; asc         ;;
 4: len 8; hex 80000000000002bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11049d5c; asc    k   \;;
 7: len 8; hex 99bac36b1104a060; asc    k   `;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c0; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000f1b; asc        ;;
 3: len 8; hex 80000000000002c0; asc         ;;
 4: len 8; hex 80000000000002c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104a41f; asc    k    ;;
 7: len 8; hex 99bac36b1104a71d; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c1; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001000fa0; asc        ;;
 3: len 8; hex 80000000000002c1; asc         ;;
 4: len 8; hex 80000000000002c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104ab0f; asc    k    ;;
 7: len 8; hex 99bac36b1104ae22; asc    k   ";;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c2; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001025; asc       %;;
 3: len 8; hex 80000000000002c2; asc         ;;
 4: len 8; hex 80000000000002c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104b21e; asc    k    ;;
 7: len 8; hex 99bac36b1104b542; asc    k   B;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c3; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010010aa; asc        ;;
 3: len 8; hex 80000000000002c3; asc         ;;
 4: len 8; hex 80000000000002c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104b99b; asc    k    ;;
 7: len 8; hex 99bac36b1104bc9a; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c5; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010011b4; asc        ;;
 3: len 8; hex 80000000000002c5; asc         ;;
 4: len 8; hex 80000000000002c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104c892; asc    k    ;;
 7: len 8; hex 99bac36b1104cba6; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c6; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001239; asc       9;;
 3: len 8; hex 80000000000002c6; asc         ;;
 4: len 8; hex 80000000000002c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104cfab; asc    k    ;;
 7: len 8; hex 99bac36b1104d29f; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c7; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010012be; asc        ;;
 3: len 8; hex 80000000000002c7; asc         ;;
 4: len 8; hex 80000000000002c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104d6be; asc    k    ;;
 7: len 8; hex 99bac36b1104d9a0; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c8; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001343; asc       C;;
 3: len 8; hex 80000000000002c8; asc         ;;
 4: len 8; hex 80000000000002c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104ddf4; asc    k    ;;
 7: len 8; hex 99bac36b1104e10f; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c9; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010013c8; asc        ;;
 3: len 8; hex 80000000000002c9; asc         ;;
 4: len 8; hex 80000000000002c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104e532; asc    k   2;;
 7: len 8; hex 99bac36b1104e822; asc    k   ";;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ca; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100144d; asc       M;;
 3: len 8; hex 80000000000002ca; asc         ;;
 4: len 8; hex 80000000000002ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104ec75; asc    k   u;;
 7: len 8; hex 99bac36b1104ef72; asc    k   r;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cb; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010014d2; asc        ;;
 3: len 8; hex 80000000000002cb; asc         ;;
 4: len 8; hex 80000000000002cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104f428; asc    k   (;;
 7: len 8; hex 99bac36b1104f728; asc    k   (;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cc; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001557; asc       W;;
 3: len 8; hex 80000000000002cc; asc         ;;
 4: len 8; hex 80000000000002cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104fbe6; asc    k    ;;
 7: len 8; hex 99bac36b1104fee2; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cd; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010015dc; asc        ;;
 3: len 8; hex 80000000000002cd; asc         ;;
 4: len 8; hex 80000000000002cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110502e6; asc    k    ;;
 7: len 8; hex 99bac36b110505f7; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ce; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001661; asc       a;;
 3: len 8; hex 80000000000002ce; asc         ;;
 4: len 8; hex 80000000000002ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110509e8; asc    k    ;;
 7: len 8; hex 99bac36b11050cc5; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002cf; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010016e6; asc        ;;
 3: len 8; hex 80000000000002cf; asc         ;;
 4: len 8; hex 80000000000002cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105109a; asc    k    ;;
 7: len 8; hex 99bac36b110513c0; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d0; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100176b; asc       k;;
 3: len 8; hex 80000000000002d0; asc         ;;
 4: len 8; hex 80000000000002d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11051805; asc    k    ;;
 7: len 8; hex 99bac36b11051b03; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d1; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010017f0; asc        ;;
 3: len 8; hex 80000000000002d1; asc         ;;
 4: len 8; hex 80000000000002d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11051f1b; asc    k    ;;
 7: len 8; hex 99bac36b1105220e; asc    k  " ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d2; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001875; asc       u;;
 3: len 8; hex 80000000000002d2; asc         ;;
 4: len 8; hex 80000000000002d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11052679; asc    k  &y;;
 7: len 8; hex 99bac36b11052985; asc    k  ) ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d3; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010018fa; asc        ;;
 3: len 8; hex 80000000000002d3; asc         ;;
 4: len 8; hex 80000000000002d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11052db5; asc    k  - ;;
 7: len 8; hex 99bac36b110530a5; asc    k  0 ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d4; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100197f; asc        ;;
 3: len 8; hex 80000000000002d4; asc         ;;
 4: len 8; hex 80000000000002d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110534d0; asc    k  4 ;;
 7: len 8; hex 99bac36b11053850; asc    k  8P;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d5; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001a04; asc        ;;
 3: len 8; hex 80000000000002d5; asc         ;;
 4: len 8; hex 80000000000002d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11053cee; asc    k  < ;;
 7: len 8; hex 99bac36b11054001; asc    k  @ ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d6; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001a89; asc        ;;
 3: len 8; hex 80000000000002d6; asc         ;;
 4: len 8; hex 80000000000002d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105440e; asc    k  D ;;
 7: len 8; hex 99bac36b1105472a; asc    k  G*;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d7; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001b0e; asc        ;;
 3: len 8; hex 80000000000002d7; asc         ;;
 4: len 8; hex 80000000000002d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11054b4c; asc    k  KL;;
 7: len 8; hex 99bac36b11054e76; asc    k  Nv;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d8; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001b93; asc        ;;
 3: len 8; hex 80000000000002d8; asc         ;;
 4: len 8; hex 80000000000002d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11055255; asc    k  RU;;
 7: len 8; hex 99bac36b1105553d; asc    k  U=;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002da; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001c9d; asc        ;;
 3: len 8; hex 80000000000002da; asc         ;;
 4: len 8; hex 80000000000002da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11056082; asc    k  ` ;;
 7: len 8; hex 99bac36b11056364; asc    k  cd;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002db; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001d22; asc       ";;
 3: len 8; hex 80000000000002db; asc         ;;
 4: len 8; hex 80000000000002db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11056784; asc    k  g ;;
 7: len 8; hex 99bac36b11056a6a; asc    k  jj;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002dc; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001da7; asc        ;;
 3: len 8; hex 80000000000002dc; asc         ;;
 4: len 8; hex 80000000000002dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11056e68; asc    k  nh;;
 7: len 8; hex 99bac36b110571c9; asc    k  q ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002dd; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001e2c; asc       ,;;
 3: len 8; hex 80000000000002dd; asc         ;;
 4: len 8; hex 80000000000002dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110576bb; asc    k  v ;;
 7: len 8; hex 99bac36b11057a2f; asc    k  z/;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002de; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001eb1; asc        ;;
 3: len 8; hex 80000000000002de; asc         ;;
 4: len 8; hex 80000000000002de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11057e78; asc    k  ~x;;
 7: len 8; hex 99bac36b11058175; asc    k   u;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002df; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001f36; asc       6;;
 3: len 8; hex 80000000000002df; asc         ;;
 4: len 8; hex 80000000000002df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110585b8; asc    k    ;;
 7: len 8; hex 99bac36b110588b7; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e0; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001001fbb; asc        ;;
 3: len 8; hex 80000000000002e0; asc         ;;
 4: len 8; hex 80000000000002e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11058cab; asc    k    ;;
 7: len 8; hex 99bac36b11058fb1; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e1; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002040; asc       @;;
 3: len 8; hex 80000000000002e1; asc         ;;
 4: len 8; hex 80000000000002e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110593c8; asc    k    ;;
 7: len 8; hex 99bac36b110596c2; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e2; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010020c5; asc        ;;
 3: len 8; hex 80000000000002e2; asc         ;;
 4: len 8; hex 80000000000002e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11059ab4; asc    k    ;;
 7: len 8; hex 99bac36b11059d81; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e3; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100214a; asc      !J;;
 3: len 8; hex 80000000000002e3; asc         ;;
 4: len 8; hex 80000000000002e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105a15c; asc    k   \;;
 7: len 8; hex 99bac36b1105b5ea; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e4; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010021cf; asc      ! ;;
 3: len 8; hex 80000000000002e4; asc         ;;
 4: len 8; hex 80000000000002e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105ba35; asc    k   5;;
 7: len 8; hex 99bac36b1105bd37; asc    k   7;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e5; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002254; asc      "T;;
 3: len 8; hex 80000000000002e5; asc         ;;
 4: len 8; hex 80000000000002e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105c19b; asc    k    ;;
 7: len 8; hex 99bac36b1105c488; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e6; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010022d9; asc      " ;;
 3: len 8; hex 80000000000002e6; asc         ;;
 4: len 8; hex 80000000000002e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105c8ca; asc    k    ;;
 7: len 8; hex 99bac36b1105cba9; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e7; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100235e; asc      #^;;
 3: len 8; hex 80000000000002e7; asc         ;;
 4: len 8; hex 80000000000002e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105d003; asc    k    ;;
 7: len 8; hex 99bac36b1105d30c; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e8; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010023e3; asc      # ;;
 3: len 8; hex 80000000000002e8; asc         ;;
 4: len 8; hex 80000000000002e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105d744; asc    k   D;;
 7: len 8; hex 99bac36b1105da5a; asc    k   Z;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002e9; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002468; asc      $h;;
 3: len 8; hex 80000000000002e9; asc         ;;
 4: len 8; hex 80000000000002e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105de8d; asc    k    ;;
 7: len 8; hex 99bac36b1105e18e; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ea; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010024ed; asc      $ ;;
 3: len 8; hex 80000000000002ea; asc         ;;
 4: len 8; hex 80000000000002ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105e5a1; asc    k    ;;
 7: len 8; hex 99bac36b1105e90f; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002eb; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002572; asc      %r;;
 3: len 8; hex 80000000000002eb; asc         ;;
 4: len 8; hex 80000000000002eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105ed62; asc    k   b;;
 7: len 8; hex 99bac36b1105f09d; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ec; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 010000010025f7; asc      % ;;
 3: len 8; hex 80000000000002ec; asc         ;;
 4: len 8; hex 80000000000002ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105f4e5; asc    k    ;;
 7: len 8; hex 99bac36b1105f7f7; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ed; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 0100000100267c; asc      &|;;
 3: len 8; hex 80000000000002ed; asc         ;;
 4: len 8; hex 80000000000002ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105fc0a; asc    k    ;;
 7: len 8; hex 99bac36b1105fef0; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ee; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002701; asc      ' ;;
 3: len 8; hex 80000000000002ee; asc         ;;
 4: len 8; hex 80000000000002ee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11060301; asc    k    ;;
 7: len 8; hex 99bac36b110605e7; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ef; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002786; asc      ' ;;
 3: len 8; hex 80000000000002ef; asc         ;;
 4: len 8; hex 80000000000002ef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110609cd; asc    k    ;;
 7: len 8; hex 99bac36b11060ce1; asc    k    ;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f0; asc         ;;
 1: len 6; hex 00000002cf65; asc      e;;
 2: len 7; hex 01000001cc0ed4; asc        ;;
 3: len 8; hex 80000000000002f0; asc         ;;
 4: len 8; hex 80000000000002f0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11061124; asc    k   $;;
 7: len 8; hex 99bac36b1106142a; asc    k   *;;
 8: len 8; hex 99bac36b120cd30c; asc    k    ;;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f1; asc         ;;
 1: len 6; hex 00000002cf1d; asc       ;;
 2: len 7; hex 01000001002890; asc      ( ;;
 3: len 8; hex 80000000000002f1; asc         ;;
 4: len 8; hex 80000000000002f1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b11061830; asc    k   0;;
 7: len 8; hex 99bac36b11061b21; asc    k   !;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002bb; asc         ;;
 1: len 6; hex 00000002cfc3; asc       ;;
 2: len 7; hex 02000001590b52; asc     Y R;;
 3: len 8; hex 80000000000002bb; asc         ;;
 4: len 8; hex 80000000000002bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b110481aa; asc    k    ;;
 7: len 8; hex 99bac36b110484e4; asc    k    ;;
 8: len 8; hex 99bac36b1502650b; asc    k  e ;;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002d9; asc         ;;
 1: len 6; hex 00000002d02c; asc      ,;;
 2: len 7; hex 01000000db0699; asc        ;;
 3: len 8; hex 80000000000002d9; asc         ;;
 4: len 8; hex 80000000000002d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1105598e; asc    k  Y ;;
 7: len 8; hex 99bac36b11055c79; asc    k  \y;;
 8: len 8; hex 99bac36b170c4948; asc    k  IH;;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002c4; asc         ;;
 1: len 6; hex 00000002d030; asc      0;;
 2: len 7; hex 01000001e8173e; asc       >;;
 3: len 8; hex 80000000000002c4; asc         ;;
 4: len 8; hex 80000000000002c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36b1104c176; asc    k   v;;
 7: len 8; hex 99bac36b1104c47e; asc    k   ~;;
 8: len 8; hex 99bac36b170c6c0b; asc    k  l ;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 105 page no 8 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 184399 lock mode S locks rec but not gap waiting
Record lock, heap no 225 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000002ed; asc         ;;
 1: len 6; hex 00000002d061; asc      a;;
 2: len 7; hex 0200000198028e; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 11; hex 746b2d3832313937333435; asc tk-82197345;;
 5: len 8; hex 99bac36b19081930; asc    k   0;;
 6: len 8; hex 99bac36b19081930; asc    k   0;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V2 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:48:14 135358810506816
*** (1) TRANSACTION:
TRANSACTION 209035, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9035, OS thread handle 135358142400064, query id 1028068 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 1935

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 117 page no 12 n bits 288 index PRIMARY of table `deadlock_lab`.`member_account` trx id 209035 lock_mode X locks rec but not gap
Record lock, heap no 218 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 00000003308b; asc     0 ;;
 2: len 7; hex 02000001502f87; asc     P/ ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343535373034353231; asc tk-455704521;;
 5: len 8; hex 99bac36c0e00403b; asc    l  @;;;
 6: len 8; hex 99bac36c0e00403b; asc    l  @;;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 115 page no 20 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 209035 lock_mode X locks rec but not gap waiting
Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93abf; asc      : ;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 800000000000078f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cc28b; asc    l    ;;
 7: len 8; hex 99bac36c070cc595; asc    l    ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 208989, ACTIVE 1 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 64 lock struct(s), heap size 24696, 5260 row lock(s), undo log entries 1303
MySQL thread id 9000, OS thread handle 135358149797440, query id 1028595 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (935, 3935, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 115 page no 20 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 208989 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006de; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a81e2c; asc       ,;;
 3: len 8; hex 80000000000002f6; asc         ;;
 4: len 8; hex 80000000000006de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704bbea; asc    l    ;;
 7: len 8; hex 99bac36c0704bee7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006df; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a81eb1; asc        ;;
 3: len 8; hex 80000000000002f7; asc         ;;
 4: len 8; hex 80000000000006df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704c44f; asc    l   O;;
 7: len 8; hex 99bac36c0704c766; asc    l   f;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e0; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a81f36; asc       6;;
 3: len 8; hex 80000000000002f8; asc         ;;
 4: len 8; hex 80000000000006e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704ccb4; asc    l    ;;
 7: len 8; hex 99bac36c0704cfdf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e1; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a81fbb; asc        ;;
 3: len 8; hex 80000000000002f9; asc         ;;
 4: len 8; hex 80000000000006e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704d52a; asc    l   *;;
 7: len 8; hex 99bac36c0704d82d; asc    l   -;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e2; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82040; asc       @;;
 3: len 8; hex 80000000000002fa; asc         ;;
 4: len 8; hex 80000000000006e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704de12; asc    l    ;;
 7: len 8; hex 99bac36c0704e15c; asc    l   \;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e3; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a820c5; asc        ;;
 3: len 8; hex 80000000000002fb; asc         ;;
 4: len 8; hex 80000000000006e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704e78c; asc    l    ;;
 7: len 8; hex 99bac36c0704eae5; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e4; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8214a; asc      !J;;
 3: len 8; hex 80000000000002fc; asc         ;;
 4: len 8; hex 80000000000006e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704f0c1; asc    l    ;;
 7: len 8; hex 99bac36c0704f3d4; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e5; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a821cf; asc      ! ;;
 3: len 8; hex 80000000000002fd; asc         ;;
 4: len 8; hex 80000000000006e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0704f972; asc    l   r;;
 7: len 8; hex 99bac36c0704fce3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e6; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82254; asc      "T;;
 3: len 8; hex 80000000000002fe; asc         ;;
 4: len 8; hex 80000000000006e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070502c9; asc    l    ;;
 7: len 8; hex 99bac36c070505d8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e7; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a822d9; asc      " ;;
 3: len 8; hex 80000000000002ff; asc         ;;
 4: len 8; hex 80000000000006e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07050b30; asc    l   0;;
 7: len 8; hex 99bac36c07050e17; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e8; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8235e; asc      #^;;
 3: len 8; hex 8000000000000300; asc         ;;
 4: len 8; hex 80000000000006e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070513a2; asc    l    ;;
 7: len 8; hex 99bac36c07051690; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006e9; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a823e3; asc      # ;;
 3: len 8; hex 8000000000000301; asc         ;;
 4: len 8; hex 80000000000006e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07051dae; asc    l    ;;
 7: len 8; hex 99bac36c070520be; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ea; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82468; asc      $h;;
 3: len 8; hex 8000000000000302; asc         ;;
 4: len 8; hex 80000000000006ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07052751; asc    l  'Q;;
 7: len 8; hex 99bac36c07052ab2; asc    l  * ;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006eb; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a824ed; asc      $ ;;
 3: len 8; hex 8000000000000303; asc         ;;
 4: len 8; hex 80000000000006eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705308d; asc    l  0 ;;
 7: len 8; hex 99bac36c070533a3; asc    l  3 ;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ec; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82572; asc      %r;;
 3: len 8; hex 8000000000000304; asc         ;;
 4: len 8; hex 80000000000006ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070538df; asc    l  8 ;;
 7: len 8; hex 99bac36c07053c04; asc    l  < ;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ed; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a825f7; asc      % ;;
 3: len 8; hex 8000000000000305; asc         ;;
 4: len 8; hex 80000000000006ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07054173; asc    l  As;;
 7: len 8; hex 99bac36c07054484; asc    l  D ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ee; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8267c; asc      &|;;
 3: len 8; hex 8000000000000306; asc         ;;
 4: len 8; hex 80000000000006ee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07054a0b; asc    l  J ;;
 7: len 8; hex 99bac36c07054cf6; asc    l  L ;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ef; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82701; asc      ' ;;
 3: len 8; hex 8000000000000307; asc         ;;
 4: len 8; hex 80000000000006ef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07055277; asc    l  Rw;;
 7: len 8; hex 99bac36c0705557e; asc    l  U~;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f0; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82786; asc      ' ;;
 3: len 8; hex 8000000000000308; asc         ;;
 4: len 8; hex 80000000000006f0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07055ce4; asc    l  \ ;;
 7: len 8; hex 99bac36c07055fea; asc    l  _ ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f1; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8280b; asc      ( ;;
 3: len 8; hex 8000000000000309; asc         ;;
 4: len 8; hex 80000000000006f1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070565fa; asc    l  e ;;
 7: len 8; hex 99bac36c07056938; asc    l  i8;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f2; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82890; asc      ( ;;
 3: len 8; hex 800000000000030a; asc         ;;
 4: len 8; hex 80000000000006f2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07056f83; asc    l  o ;;
 7: len 8; hex 99bac36c070572ab; asc    l  r ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f3; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82915; asc      ) ;;
 3: len 8; hex 800000000000030b; asc         ;;
 4: len 8; hex 80000000000006f3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07057865; asc    l  xe;;
 7: len 8; hex 99bac36c07057b68; asc    l  {h;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f4; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8299a; asc      ) ;;
 3: len 8; hex 800000000000030c; asc         ;;
 4: len 8; hex 80000000000006f4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705816f; asc    l   o;;
 7: len 8; hex 99bac36c0705844e; asc    l   N;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f5; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82a1f; asc      * ;;
 3: len 8; hex 800000000000030d; asc         ;;
 4: len 8; hex 80000000000006f5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070589b2; asc    l    ;;
 7: len 8; hex 99bac36c07058c95; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f6; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82aa4; asc      * ;;
 3: len 8; hex 800000000000030e; asc         ;;
 4: len 8; hex 80000000000006f6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07059205; asc    l    ;;
 7: len 8; hex 99bac36c07059528; asc    l   (;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f7; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82b29; asc      +);;
 3: len 8; hex 800000000000030f; asc         ;;
 4: len 8; hex 80000000000006f7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07059aef; asc    l    ;;
 7: len 8; hex 99bac36c07059dcf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f8; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82bae; asc      + ;;
 3: len 8; hex 8000000000000310; asc         ;;
 4: len 8; hex 80000000000006f8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705a332; asc    l   2;;
 7: len 8; hex 99bac36c0705a67a; asc    l   z;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006f9; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82c33; asc      ,3;;
 3: len 8; hex 8000000000000311; asc         ;;
 4: len 8; hex 80000000000006f9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705ac22; asc    l   ";;
 7: len 8; hex 99bac36c0705af07; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006fa; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82cb8; asc      , ;;
 3: len 8; hex 8000000000000312; asc         ;;
 4: len 8; hex 80000000000006fa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705b478; asc    l   x;;
 7: len 8; hex 99bac36c0705b772; asc    l   r;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006fb; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82d3d; asc      -=;;
 3: len 8; hex 8000000000000313; asc         ;;
 4: len 8; hex 80000000000006fb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705bce8; asc    l    ;;
 7: len 8; hex 99bac36c0705bfe6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006fc; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82dc2; asc      - ;;
 3: len 8; hex 8000000000000314; asc         ;;
 4: len 8; hex 80000000000006fc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705c566; asc    l   f;;
 7: len 8; hex 99bac36c0705c877; asc    l   w;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006fd; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82e47; asc      .G;;
 3: len 8; hex 8000000000000315; asc         ;;
 4: len 8; hex 80000000000006fd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705cdf7; asc    l    ;;
 7: len 8; hex 99bac36c0705d130; asc    l   0;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006fe; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82ecc; asc      . ;;
 3: len 8; hex 8000000000000316; asc         ;;
 4: len 8; hex 80000000000006fe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705d6ec; asc    l    ;;
 7: len 8; hex 99bac36c0705da04; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000006ff; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82f51; asc      /Q;;
 3: len 8; hex 8000000000000317; asc         ;;
 4: len 8; hex 80000000000006ff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705df64; asc    l   d;;
 7: len 8; hex 99bac36c0705e27d; asc    l   };;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000700; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a82fd6; asc      / ;;
 3: len 8; hex 8000000000000318; asc         ;;
 4: len 8; hex 8000000000000700; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705e8d8; asc    l    ;;
 7: len 8; hex 99bac36c0705ebe6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000701; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8305b; asc      0[;;
 3: len 8; hex 8000000000000319; asc         ;;
 4: len 8; hex 8000000000000701; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705f186; asc    l    ;;
 7: len 8; hex 99bac36c0705f474; asc    l   t;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000702; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a830e0; asc      0 ;;
 3: len 8; hex 800000000000031a; asc         ;;
 4: len 8; hex 8000000000000702; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0705f9e3; asc    l    ;;
 7: len 8; hex 99bac36c0705fcd2; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000703; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83165; asc      1e;;
 3: len 8; hex 800000000000031b; asc         ;;
 4: len 8; hex 8000000000000703; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07060298; asc    l    ;;
 7: len 8; hex 99bac36c070605af; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000704; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a831ea; asc      1 ;;
 3: len 8; hex 800000000000031c; asc         ;;
 4: len 8; hex 8000000000000704; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07060b5c; asc    l   \;;
 7: len 8; hex 99bac36c07060edd; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000705; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8326f; asc      2o;;
 3: len 8; hex 800000000000031d; asc         ;;
 4: len 8; hex 8000000000000705; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07061470; asc    l   p;;
 7: len 8; hex 99bac36c070617a3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000706; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a832f4; asc      2 ;;
 3: len 8; hex 800000000000031e; asc         ;;
 4: len 8; hex 8000000000000706; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07061dcb; asc    l    ;;
 7: len 8; hex 99bac36c07062119; asc    l  ! ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000707; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83379; asc      3y;;
 3: len 8; hex 800000000000031f; asc         ;;
 4: len 8; hex 8000000000000707; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070626c0; asc    l  & ;;
 7: len 8; hex 99bac36c070629ac; asc    l  ) ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000708; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a833fe; asc      3 ;;
 3: len 8; hex 8000000000000320; asc         ;;
 4: len 8; hex 8000000000000708; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07062f2d; asc    l  /-;;
 7: len 8; hex 99bac36c0706321e; asc    l  2 ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000709; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83483; asc      4 ;;
 3: len 8; hex 8000000000000321; asc        !;;
 4: len 8; hex 8000000000000709; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706375f; asc    l  7_;;
 7: len 8; hex 99bac36c07063a2f; asc    l  :/;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070a; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83508; asc      5 ;;
 3: len 8; hex 8000000000000322; asc        ";;
 4: len 8; hex 800000000000070a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07063f8a; asc    l  ? ;;
 7: len 8; hex 99bac36c07064287; asc    l  B ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070b; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8358d; asc      5 ;;
 3: len 8; hex 8000000000000323; asc        #;;
 4: len 8; hex 800000000000070b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07064837; asc    l  H7;;
 7: len 8; hex 99bac36c07064b85; asc    l  K ;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070c; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83612; asc      6 ;;
 3: len 8; hex 8000000000000324; asc        $;;
 4: len 8; hex 800000000000070c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706516c; asc    l  Ql;;
 7: len 8; hex 99bac36c0706548d; asc    l  T ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070d; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83697; asc      6 ;;
 3: len 8; hex 8000000000000325; asc        %;;
 4: len 8; hex 800000000000070d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07065a05; asc    l  Z ;;
 7: len 8; hex 99bac36c07065d1e; asc    l  ] ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070e; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a8371c; asc      7 ;;
 3: len 8; hex 8000000000000326; asc        &;;
 4: len 8; hex 800000000000070e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070662a2; asc    l  b ;;
 7: len 8; hex 99bac36c07066598; asc    l  e ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000070f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a837a1; asc      7 ;;
 3: len 8; hex 8000000000000327; asc        ';;
 4: len 8; hex 800000000000070f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07066bea; asc    l  k ;;
 7: len 8; hex 99bac36c07066f11; asc    l  o ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000710; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83826; asc      8&;;
 3: len 8; hex 8000000000000328; asc        (;;
 4: len 8; hex 8000000000000710; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07067543; asc    l  uC;;
 7: len 8; hex 99bac36c0706786f; asc    l  xo;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000711; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a838ab; asc      8 ;;
 3: len 8; hex 8000000000000329; asc        );;
 4: len 8; hex 8000000000000711; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07067e02; asc    l  ~ ;;
 7: len 8; hex 99bac36c0706810c; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000712; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83930; asc      90;;
 3: len 8; hex 800000000000032a; asc        *;;
 4: len 8; hex 8000000000000712; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070687e7; asc    l    ;;
 7: len 8; hex 99bac36c07068b08; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000713; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a839b5; asc      9 ;;
 3: len 8; hex 800000000000032b; asc        +;;
 4: len 8; hex 8000000000000713; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07069055; asc    l   U;;
 7: len 8; hex 99bac36c07069348; asc    l   H;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000714; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83a3a; asc      ::;;
 3: len 8; hex 800000000000032c; asc        ,;;
 4: len 8; hex 8000000000000714; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07069889; asc    l    ;;
 7: len 8; hex 99bac36c07069b93; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000715; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83abf; asc      : ;;
 3: len 8; hex 800000000000032d; asc        -;;
 4: len 8; hex 8000000000000715; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706a0d4; asc    l    ;;
 7: len 8; hex 99bac36c0706a3e7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000716; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83b44; asc      ;D;;
 3: len 8; hex 800000000000032e; asc        .;;
 4: len 8; hex 8000000000000716; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706a9e7; asc    l    ;;
 7: len 8; hex 99bac36c0706ad10; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000717; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83bc9; asc      ; ;;
 3: len 8; hex 800000000000032f; asc        /;;
 4: len 8; hex 8000000000000717; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706b34e; asc    l   N;;
 7: len 8; hex 99bac36c0706b690; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000718; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83c4e; asc      <N;;
 3: len 8; hex 8000000000000330; asc        0;;
 4: len 8; hex 8000000000000718; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706bc9c; asc    l    ;;
 7: len 8; hex 99bac36c0706c017; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000719; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83cd3; asc      < ;;
 3: len 8; hex 8000000000000331; asc        1;;
 4: len 8; hex 8000000000000719; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706c60d; asc    l    ;;
 7: len 8; hex 99bac36c0706c905; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071a; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83d58; asc      =X;;
 3: len 8; hex 8000000000000332; asc        2;;
 4: len 8; hex 800000000000071a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706ce83; asc    l    ;;
 7: len 8; hex 99bac36c0706d16d; asc    l   m;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071b; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83ddd; asc      = ;;
 3: len 8; hex 8000000000000333; asc        3;;
 4: len 8; hex 800000000000071b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706d6f9; asc    l    ;;
 7: len 8; hex 99bac36c0706da11; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071c; asc         ;;
 1: len 6; hex 000000033038; asc     08;;
 2: len 7; hex 02000001b918ea; asc        ;;
 3: len 8; hex 8000000000000334; asc        4;;
 4: len 8; hex 800000000000071c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706df61; asc    l   a;;
 7: len 8; hex 99bac36c0706e27c; asc    l   |;;
 8: len 8; hex 99bac36c0b03d94b; asc    l   K;;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071d; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83ee7; asc      > ;;
 3: len 8; hex 8000000000000335; asc        5;;
 4: len 8; hex 800000000000071d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706e81d; asc    l    ;;
 7: len 8; hex 99bac36c0706eb8d; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071e; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a83f6c; asc      ?l;;
 3: len 8; hex 8000000000000336; asc        6;;
 4: len 8; hex 800000000000071e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706f154; asc    l   T;;
 7: len 8; hex 99bac36c0706f453; asc    l   S;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000071f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9008f; asc        ;;
 3: len 8; hex 8000000000000337; asc        7;;
 4: len 8; hex 800000000000071f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0706fa70; asc    l   p;;
 7: len 8; hex 99bac36c0706fd6d; asc    l   m;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000720; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90114; asc        ;;
 3: len 8; hex 8000000000000338; asc        8;;
 4: len 8; hex 8000000000000720; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070702ed; asc    l    ;;
 7: len 8; hex 99bac36c070705cb; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000721; asc        !;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90199; asc        ;;
 3: len 8; hex 8000000000000339; asc        9;;
 4: len 8; hex 8000000000000721; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07070b8a; asc    l    ;;
 7: len 8; hex 99bac36c07070e8b; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000722; asc        ";;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9021e; asc        ;;
 3: len 8; hex 800000000000033a; asc        :;;
 4: len 8; hex 8000000000000722; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070713f4; asc    l    ;;
 7: len 8; hex 99bac36c0707519d; asc    l  Q ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000723; asc        #;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a902a3; asc        ;;
 3: len 8; hex 800000000000033b; asc        ;;;
 4: len 8; hex 8000000000000723; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07075778; asc    l  Wx;;
 7: len 8; hex 99bac36c07075a7b; asc    l  Z{;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000724; asc        $;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90328; asc       (;;
 3: len 8; hex 800000000000033c; asc        <;;
 4: len 8; hex 8000000000000724; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07075fe9; asc    l  _ ;;
 7: len 8; hex 99bac36c0707631a; asc    l  c ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000725; asc        %;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a903ad; asc        ;;
 3: len 8; hex 800000000000033d; asc        =;;
 4: len 8; hex 8000000000000725; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070768c8; asc    l  h ;;
 7: len 8; hex 99bac36c07076bb5; asc    l  k ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000726; asc        &;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90432; asc       2;;
 3: len 8; hex 800000000000033e; asc        >;;
 4: len 8; hex 8000000000000726; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070771fb; asc    l  q ;;
 7: len 8; hex 99bac36c070774f4; asc    l  t ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000727; asc        ';;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a904b7; asc        ;;
 3: len 8; hex 800000000000033f; asc        ?;;
 4: len 8; hex 8000000000000727; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07077ae8; asc    l  z ;;
 7: len 8; hex 99bac36c07077dc5; asc    l  } ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000728; asc        (;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9053c; asc       <;;
 3: len 8; hex 8000000000000340; asc        @;;
 4: len 8; hex 8000000000000728; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07078374; asc    l   t;;
 7: len 8; hex 99bac36c07078687; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000729; asc        );;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a905c1; asc        ;;
 3: len 8; hex 8000000000000341; asc        A;;
 4: len 8; hex 8000000000000729; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07078c61; asc    l   a;;
 7: len 8; hex 99bac36c07078f60; asc    l   `;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072a; asc        *;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90646; asc       F;;
 3: len 8; hex 8000000000000342; asc        B;;
 4: len 8; hex 800000000000072a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07079635; asc    l   5;;
 7: len 8; hex 99bac36c0707994e; asc    l   N;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072b; asc        +;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a906cb; asc        ;;
 3: len 8; hex 8000000000000343; asc        C;;
 4: len 8; hex 800000000000072b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07079f39; asc    l   9;;
 7: len 8; hex 99bac36c0707a22e; asc    l   .;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072c; asc        ,;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90750; asc       P;;
 3: len 8; hex 8000000000000344; asc        D;;
 4: len 8; hex 800000000000072c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707a7db; asc    l    ;;
 7: len 8; hex 99bac36c0707aae1; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072d; asc        -;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a907d5; asc        ;;
 3: len 8; hex 8000000000000345; asc        E;;
 4: len 8; hex 800000000000072d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707b084; asc    l    ;;
 7: len 8; hex 99bac36c0707b4a8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072e; asc        .;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9085a; asc       Z;;
 3: len 8; hex 8000000000000346; asc        F;;
 4: len 8; hex 800000000000072e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707ba94; asc    l    ;;
 7: len 8; hex 99bac36c0707bd95; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000072f; asc        /;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a908df; asc        ;;
 3: len 8; hex 8000000000000347; asc        G;;
 4: len 8; hex 800000000000072f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707c32d; asc    l   -;;
 7: len 8; hex 99bac36c0707c631; asc    l   1;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000730; asc        0;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90964; asc       d;;
 3: len 8; hex 8000000000000348; asc        H;;
 4: len 8; hex 8000000000000730; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707cb89; asc    l    ;;
 7: len 8; hex 99bac36c0707ce51; asc    l   Q;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000731; asc        1;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a909e9; asc        ;;
 3: len 8; hex 8000000000000349; asc        I;;
 4: len 8; hex 8000000000000731; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707d3c8; asc    l    ;;
 7: len 8; hex 99bac36c0707d67e; asc    l   ~;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000732; asc        2;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90a6e; asc       n;;
 3: len 8; hex 800000000000034a; asc        J;;
 4: len 8; hex 8000000000000732; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707dbb0; asc    l    ;;
 7: len 8; hex 99bac36c0707de8e; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000733; asc        3;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90af3; asc        ;;
 3: len 8; hex 800000000000034b; asc        K;;
 4: len 8; hex 8000000000000733; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707e3e7; asc    l    ;;
 7: len 8; hex 99bac36c0707e6bf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000734; asc        4;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90b78; asc       x;;
 3: len 8; hex 800000000000034c; asc        L;;
 4: len 8; hex 8000000000000734; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707ec3f; asc    l   ?;;
 7: len 8; hex 99bac36c0707effa; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000735; asc        5;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90bfd; asc        ;;
 3: len 8; hex 800000000000034d; asc        M;;
 4: len 8; hex 8000000000000735; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707f63f; asc    l   ?;;
 7: len 8; hex 99bac36c0707f94a; asc    l   J;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000736; asc        6;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90c82; asc        ;;
 3: len 8; hex 800000000000034e; asc        N;;
 4: len 8; hex 8000000000000736; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0707feb1; asc    l    ;;
 7: len 8; hex 99bac36c070801bf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000737; asc        7;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90d07; asc        ;;
 3: len 8; hex 800000000000034f; asc        O;;
 4: len 8; hex 8000000000000737; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07080769; asc    l   i;;
 7: len 8; hex 99bac36c07080a4e; asc    l   N;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000738; asc        8;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90d8c; asc        ;;
 3: len 8; hex 8000000000000350; asc        P;;
 4: len 8; hex 8000000000000738; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07080fc5; asc    l    ;;
 7: len 8; hex 99bac36c070812c8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000739; asc        9;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90e11; asc        ;;
 3: len 8; hex 8000000000000351; asc        Q;;
 4: len 8; hex 8000000000000739; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07081837; asc    l   7;;
 7: len 8; hex 99bac36c07081b29; asc    l   );;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073a; asc        :;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90e96; asc        ;;
 3: len 8; hex 8000000000000352; asc        R;;
 4: len 8; hex 800000000000073a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070820e1; asc    l    ;;
 7: len 8; hex 99bac36c070823c6; asc    l  # ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073b; asc        ;;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90f1b; asc        ;;
 3: len 8; hex 8000000000000353; asc        S;;
 4: len 8; hex 800000000000073b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07082925; asc    l  )%;;
 7: len 8; hex 99bac36c07082cb2; asc    l  , ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073c; asc        <;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a90fa0; asc        ;;
 3: len 8; hex 8000000000000354; asc        T;;
 4: len 8; hex 800000000000073c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070832de; asc    l  2 ;;
 7: len 8; hex 99bac36c0708361a; asc    l  6 ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073d; asc        =;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91025; asc       %;;
 3: len 8; hex 8000000000000355; asc        U;;
 4: len 8; hex 800000000000073d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07083c29; asc    l  <);;
 7: len 8; hex 99bac36c07083f73; asc    l  ?s;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073e; asc        >;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a910aa; asc        ;;
 3: len 8; hex 8000000000000356; asc        V;;
 4: len 8; hex 800000000000073e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070844ea; asc    l  D ;;
 7: len 8; hex 99bac36c070847db; asc    l  G ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000073f; asc        ?;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9112f; asc       /;;
 3: len 8; hex 8000000000000357; asc        W;;
 4: len 8; hex 800000000000073f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07084d31; asc    l  M1;;
 7: len 8; hex 99bac36c07085049; asc    l  PI;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000740; asc        @;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a911b4; asc        ;;
 3: len 8; hex 8000000000000358; asc        X;;
 4: len 8; hex 8000000000000740; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070855bd; asc    l  U ;;
 7: len 8; hex 99bac36c070858b1; asc    l  X ;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000741; asc        A;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91239; asc       9;;
 3: len 8; hex 8000000000000359; asc        Y;;
 4: len 8; hex 8000000000000741; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07085e81; asc    l  ^ ;;
 7: len 8; hex 99bac36c0708619d; asc    l  a ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000742; asc        B;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a912be; asc        ;;
 3: len 8; hex 800000000000035a; asc        Z;;
 4: len 8; hex 8000000000000742; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708690e; asc    l  i ;;
 7: len 8; hex 99bac36c07086db9; asc    l  m ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000743; asc        C;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91343; asc       C;;
 3: len 8; hex 800000000000035b; asc        [;;
 4: len 8; hex 8000000000000743; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708754a; asc    l  uJ;;
 7: len 8; hex 99bac36c07087a9d; asc    l  z ;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000744; asc        D;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a913c8; asc        ;;
 3: len 8; hex 800000000000035c; asc        \;;
 4: len 8; hex 8000000000000744; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07088171; asc    l   q;;
 7: len 8; hex 99bac36c070884aa; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000745; asc        E;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9144d; asc       M;;
 3: len 8; hex 800000000000035d; asc        ];;
 4: len 8; hex 8000000000000745; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07088a4e; asc    l   N;;
 7: len 8; hex 99bac36c07088d8f; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000746; asc        F;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a914d2; asc        ;;
 3: len 8; hex 800000000000035e; asc        ^;;
 4: len 8; hex 8000000000000746; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07089307; asc    l    ;;
 7: len 8; hex 99bac36c070895e9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000747; asc        G;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91557; asc       W;;
 3: len 8; hex 800000000000035f; asc        _;;
 4: len 8; hex 8000000000000747; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07089b23; asc    l   #;;
 7: len 8; hex 99bac36c07089df0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000748; asc        H;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a915dc; asc        ;;
 3: len 8; hex 8000000000000360; asc        `;;
 4: len 8; hex 8000000000000748; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708a400; asc    l    ;;
 7: len 8; hex 99bac36c0708a729; asc    l   );;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000749; asc        I;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91661; asc       a;;
 3: len 8; hex 8000000000000361; asc        a;;
 4: len 8; hex 8000000000000749; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708aca6; asc    l    ;;
 7: len 8; hex 99bac36c0708afae; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074a; asc        J;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a916e6; asc        ;;
 3: len 8; hex 8000000000000362; asc        b;;
 4: len 8; hex 800000000000074a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708b581; asc    l    ;;
 7: len 8; hex 99bac36c0708b891; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074b; asc        K;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9176b; asc       k;;
 3: len 8; hex 8000000000000363; asc        c;;
 4: len 8; hex 800000000000074b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708bdfd; asc    l    ;;
 7: len 8; hex 99bac36c0708c0e5; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074c; asc        L;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a917f0; asc        ;;
 3: len 8; hex 8000000000000364; asc        d;;
 4: len 8; hex 800000000000074c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708c68e; asc    l    ;;
 7: len 8; hex 99bac36c0708c991; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074d; asc        M;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91875; asc       u;;
 3: len 8; hex 8000000000000365; asc        e;;
 4: len 8; hex 800000000000074d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708cfdd; asc    l    ;;
 7: len 8; hex 99bac36c0708d2cf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074e; asc        N;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a918fa; asc        ;;
 3: len 8; hex 8000000000000366; asc        f;;
 4: len 8; hex 800000000000074e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708d8e5; asc    l    ;;
 7: len 8; hex 99bac36c0708dbe3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000074f; asc        O;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9197f; asc        ;;
 3: len 8; hex 8000000000000367; asc        g;;
 4: len 8; hex 800000000000074f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708e2b7; asc    l    ;;
 7: len 8; hex 99bac36c0708e619; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000750; asc        P;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91a04; asc        ;;
 3: len 8; hex 8000000000000368; asc        h;;
 4: len 8; hex 8000000000000750; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708ece8; asc    l    ;;
 7: len 8; hex 99bac36c0708f039; asc    l   9;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000751; asc        Q;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91a89; asc        ;;
 3: len 8; hex 8000000000000369; asc        i;;
 4: len 8; hex 8000000000000751; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0708f77a; asc    l   z;;
 7: len 8; hex 99bac36c0708fae8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000752; asc        R;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91b0e; asc        ;;
 3: len 8; hex 800000000000036a; asc        j;;
 4: len 8; hex 8000000000000752; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070900e6; asc    l    ;;
 7: len 8; hex 99bac36c07090422; asc    l   ";;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000753; asc        S;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91b93; asc        ;;
 3: len 8; hex 800000000000036b; asc        k;;
 4: len 8; hex 8000000000000753; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709099d; asc    l    ;;
 7: len 8; hex 99bac36c07090c87; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000754; asc        T;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91c18; asc        ;;
 3: len 8; hex 800000000000036c; asc        l;;
 4: len 8; hex 8000000000000754; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070911cb; asc    l    ;;
 7: len 8; hex 99bac36c0709148d; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000755; asc        U;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91c9d; asc        ;;
 3: len 8; hex 800000000000036d; asc        m;;
 4: len 8; hex 8000000000000755; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07091be5; asc    l    ;;
 7: len 8; hex 99bac36c07091f97; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000756; asc        V;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91d22; asc       ";;
 3: len 8; hex 800000000000036e; asc        n;;
 4: len 8; hex 8000000000000756; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07092599; asc    l  % ;;
 7: len 8; hex 99bac36c070928ac; asc    l  ( ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000757; asc        W;;
 1: len 6; hex 000000032fd0; asc     / ;;
 2: len 7; hex 02000001710c88; asc     q  ;;
 3: len 8; hex 800000000000036f; asc        o;;
 4: len 8; hex 8000000000000757; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07092e74; asc    l  .t;;
 7: len 8; hex 99bac36c070931aa; asc    l  1 ;;
 8: len 8; hex 99bac36c0800dd7e; asc    l   ~;;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000758; asc        X;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91e2c; asc       ,;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000758; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070937ee; asc    l  7 ;;
 7: len 8; hex 99bac36c07093b27; asc    l  ;';;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000759; asc        Y;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91eb1; asc        ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000759; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070941a0; asc    l  A ;;
 7: len 8; hex 99bac36c070944b0; asc    l  D ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075a; asc        Z;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91f36; asc       6;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 800000000000075a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07094a77; asc    l  Jw;;
 7: len 8; hex 99bac36c07094d4e; asc    l  MN;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075b; asc        [;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a91fbb; asc        ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 800000000000075b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070952ad; asc    l  R ;;
 7: len 8; hex 99bac36c070956f0; asc    l  V ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075c; asc        \;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92040; asc       @;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 800000000000075c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070961c8; asc    l  a ;;
 7: len 8; hex 99bac36c0709667f; asc    l  f ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075d; asc        ];;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a920c5; asc        ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 800000000000075d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07097149; asc    l  qI;;
 7: len 8; hex 99bac36c070975b6; asc    l  u ;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075e; asc        ^;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9214a; asc      !J;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 800000000000075e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07098082; asc    l    ;;
 7: len 8; hex 99bac36c070984a5; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075f; asc        _;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a921cf; asc      ! ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 800000000000075f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c07098e5d; asc    l   ];;
 7: len 8; hex 99bac36c07099397; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000760; asc        `;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92254; asc      "T;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000760; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709a138; asc    l   8;;
 7: len 8; hex 99bac36c0709b9da; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000761; asc        a;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a922d9; asc      " ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000761; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709c505; asc    l    ;;
 7: len 8; hex 99bac36c0709ce08; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000762; asc        b;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9235e; asc      #^;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 8000000000000762; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709dcdf; asc    l    ;;
 7: len 8; hex 99bac36c0709e02c; asc    l   ,;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000763; asc        c;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a923e3; asc      # ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 8000000000000763; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709e880; asc    l    ;;
 7: len 8; hex 99bac36c0709ed4b; asc    l   K;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000764; asc        d;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92468; asc      $h;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 8000000000000764; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0709f754; asc    l   T;;
 7: len 8; hex 99bac36c0709fb62; asc    l   b;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000765; asc        e;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a924ed; asc      $ ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 8000000000000765; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a05ec; asc    l    ;;
 7: len 8; hex 99bac36c070a0a0d; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000766; asc        f;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92572; asc      %r;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 8000000000000766; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a15b7; asc    l    ;;
 7: len 8; hex 99bac36c070a1a4c; asc    l   L;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000767; asc        g;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a925f7; asc      % ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 8000000000000767; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a22f5; asc    l  " ;;
 7: len 8; hex 99bac36c070a277e; asc    l  '~;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000768; asc        h;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9267c; asc      &|;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000768; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a3022; asc    l  0";;
 7: len 8; hex 99bac36c070a34ab; asc    l  4 ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000769; asc        i;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92701; asc      ' ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000769; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a3d03; asc    l  = ;;
 7: len 8; hex 99bac36c070a418f; asc    l  A ;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076a; asc        j;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92786; asc      ' ;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 800000000000076a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a4a8a; asc    l  J ;;
 7: len 8; hex 99bac36c070a4ed3; asc    l  N ;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076b; asc        k;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9280b; asc      ( ;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 800000000000076b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a5ed5; asc    l  ^ ;;
 7: len 8; hex 99bac36c070a6322; asc    l  c";;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076c; asc        l;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92890; asc      ( ;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 800000000000076c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a76b5; asc    l  v ;;
 7: len 8; hex 99bac36c070a7b2f; asc    l  {/;;
 8: SQL NULL;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076d; asc        m;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92915; asc      ) ;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 800000000000076d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a861f; asc    l    ;;
 7: len 8; hex 99bac36c070a8a49; asc    l   I;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076e; asc        n;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9299a; asc      ) ;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 800000000000076e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070a952a; asc    l   *;;
 7: len 8; hex 99bac36c070a9a2d; asc    l   -;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076f; asc        o;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92a1f; asc      * ;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 800000000000076f; asc        o;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070aa274; asc    l   t;;
 7: len 8; hex 99bac36c070aa6f6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000770; asc        p;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92aa4; asc      * ;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000770; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070aaf11; asc    l    ;;
 7: len 8; hex 99bac36c070ab4a9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000771; asc        q;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92b29; asc      +);;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000771; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070abe58; asc    l   X;;
 7: len 8; hex 99bac36c070ac1a2; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000772; asc        r;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92bae; asc      + ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 8000000000000772; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070acb54; asc    l   T;;
 7: len 8; hex 99bac36c070ad76a; asc    l   j;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000773; asc        s;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92c33; asc      ,3;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 8000000000000773; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ae394; asc    l    ;;
 7: len 8; hex 99bac36c070ae746; asc    l   F;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000774; asc        t;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92cb8; asc      , ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 8000000000000774; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070af20d; asc    l    ;;
 7: len 8; hex 99bac36c070af670; asc    l   p;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000775; asc        u;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92d3d; asc      -=;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 8000000000000775; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b01b6; asc    l    ;;
 7: len 8; hex 99bac36c070b0622; asc    l   ";;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000776; asc        v;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92dc2; asc      - ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 8000000000000776; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b1627; asc    l   ';;
 7: len 8; hex 99bac36c070b1aa5; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000777; asc        w;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92e47; asc      .G;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 8000000000000777; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b233c; asc    l  #<;;
 7: len 8; hex 99bac36c070b279b; asc    l  ' ;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000778; asc        x;;
 1: len 6; hex 000000032fe7; asc     / ;;
 2: len 7; hex 020000019319f7; asc        ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000778; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b318f; asc    l  1 ;;
 7: len 8; hex 99bac36c070b3641; asc    l  6A;;
 8: len 8; hex 99bac36c080a2866; asc    l  (f;;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000779; asc        y;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92f51; asc      /Q;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000779; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b3eb5; asc    l  > ;;
 7: len 8; hex 99bac36c070b4373; asc    l  Cs;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077a; asc        z;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a92fd6; asc      / ;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 800000000000077a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b4e2e; asc    l  N.;;
 7: len 8; hex 99bac36c070b531b; asc    l  S ;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077b; asc        {;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9305b; asc      0[;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 800000000000077b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b7d1e; asc    l  } ;;
 7: len 8; hex 99bac36c070b83c5; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077c; asc        |;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a930e0; asc      0 ;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 800000000000077c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b8ecb; asc    l    ;;
 7: len 8; hex 99bac36c070b9341; asc    l   A;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077d; asc        };;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93165; asc      1e;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 800000000000077d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070b9b5d; asc    l   ];;
 7: len 8; hex 99bac36c070b9f92; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077e; asc        ~;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a931ea; asc      1 ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 800000000000077e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070babba; asc    l    ;;
 7: len 8; hex 99bac36c070bb0c7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9326f; asc      2o;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 800000000000077f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070bbb5d; asc    l   ];;
 7: len 8; hex 99bac36c070bbf25; asc    l   %;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000780; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a932f4; asc      2 ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000780; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070bd3f5; asc    l    ;;
 7: len 8; hex 99bac36c070bd7c6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000781; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93379; asc      3y;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000781; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070be1e0; asc    l    ;;
 7: len 8; hex 99bac36c070be60d; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000782; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a933fe; asc      3 ;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 8000000000000782; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070bedee; asc    l    ;;
 7: len 8; hex 99bac36c070bf223; asc    l   #;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000783; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93483; asc      4 ;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 8000000000000783; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070bfe1b; asc    l    ;;
 7: len 8; hex 99bac36c070c030a; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000784; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93508; asc      5 ;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 8000000000000784; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c0bb5; asc    l    ;;
 7: len 8; hex 99bac36c070c0ff1; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000785; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9358d; asc      5 ;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 8000000000000785; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c2498; asc    l  $ ;;
 7: len 8; hex 99bac36c070c2940; asc    l  )@;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000786; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93612; asc      6 ;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 8000000000000786; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c34ea; asc    l  4 ;;
 7: len 8; hex 99bac36c070c383a; asc    l  8:;;
 8: SQL NULL;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000787; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93697; asc      6 ;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 8000000000000787; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c4110; asc    l  A ;;
 7: len 8; hex 99bac36c070c45e9; asc    l  E ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000788; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a9371c; asc      7 ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 8000000000000788; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c5062; asc    l  Pb;;
 7: len 8; hex 99bac36c070c54ec; asc    l  T ;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000789; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a937a1; asc      7 ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 8000000000000789; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c5fb0; asc    l  _ ;;
 7: len 8; hex 99bac36c070c6490; asc    l  d ;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078a; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93826; asc      8&;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 800000000000078a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c6e93; asc    l  n ;;
 7: len 8; hex 99bac36c070c72d8; asc    l  r ;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078b; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a938ab; asc      8 ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 800000000000078b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c7d30; asc    l  }0;;
 7: len 8; hex 99bac36c070c899d; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078c; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93930; asc      90;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 800000000000078c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070c9240; asc    l   @;;
 7: len 8; hex 99bac36c070c9714; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078d; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a939b5; asc      9 ;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 800000000000078d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ca116; asc    l    ;;
 7: len 8; hex 99bac36c070ca53f; asc    l   ?;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078e; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93a3a; asc      ::;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 800000000000078e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cb5e9; asc    l    ;;
 7: len 8; hex 99bac36c070cba0c; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93abf; asc      : ;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 800000000000078f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cc28b; asc    l    ;;
 7: len 8; hex 99bac36c070cc595; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000790; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93b44; asc      ;D;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 8000000000000790; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ccb98; asc    l    ;;
 7: len 8; hex 99bac36c070cce7f; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000791; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93bc9; asc      ; ;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 8000000000000791; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cd42d; asc    l   -;;
 7: len 8; hex 99bac36c070cd7bd; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000792; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93c4e; asc      <N;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 8000000000000792; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cdd83; asc    l    ;;
 7: len 8; hex 99bac36c070ce08b; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000793; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93cd3; asc      < ;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 8000000000000793; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ce626; asc    l   &;;
 7: len 8; hex 99bac36c070ce929; asc    l   );;
 8: SQL NULL;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000794; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93d58; asc      =X;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 8000000000000794; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ceed4; asc    l    ;;
 7: len 8; hex 99bac36c070cf1df; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000795; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93ddd; asc      = ;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 8000000000000795; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cf793; asc    l    ;;
 7: len 8; hex 99bac36c070cfa8c; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000796; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93e62; asc      >b;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 8000000000000796; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070cffe3; asc    l    ;;
 7: len 8; hex 99bac36c070d02ff; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000797; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93ee7; asc      > ;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 8000000000000797; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d08ac; asc    l    ;;
 7: len 8; hex 99bac36c070d0ba7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000798; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001a93f6c; asc      ?l;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 8000000000000798; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d1238; asc    l   8;;
 7: len 8; hex 99bac36c070d51da; asc    l  Q ;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000799; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa008f; asc        ;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 8000000000000799; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d5ad0; asc    l  Z ;;
 7: len 8; hex 99bac36c070d5e06; asc    l  ^ ;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079a; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0114; asc        ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 800000000000079a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d6486; asc    l  d ;;
 7: len 8; hex 99bac36c070d67b6; asc    l  g ;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079b; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0199; asc        ;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 800000000000079b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d6cf4; asc    l  l ;;
 7: len 8; hex 99bac36c070d6fb4; asc    l  o ;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079c; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa021e; asc        ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 800000000000079c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d74dc; asc    l  t ;;
 7: len 8; hex 99bac36c070d779e; asc    l  w ;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079d; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa02a3; asc        ;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 800000000000079d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d7cc3; asc    l  | ;;
 7: len 8; hex 99bac36c070d7fa6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079e; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0328; asc       (;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 800000000000079e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d8509; asc    l    ;;
 7: len 8; hex 99bac36c070d880e; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079f; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa03ad; asc        ;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 800000000000079f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d8e77; asc    l   w;;
 7: len 8; hex 99bac36c070d91ce; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a0; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0432; asc       2;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000007a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d97a0; asc    l    ;;
 7: len 8; hex 99bac36c070d9ab0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a1; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa04b7; asc        ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000007a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070d9ff3; asc    l    ;;
 7: len 8; hex 99bac36c070da2c3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a2; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa053c; asc       <;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000007a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070da835; asc    l   5;;
 7: len 8; hex 99bac36c070dab21; asc    l   !;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a3; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa05c1; asc        ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000007a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070db0ab; asc    l    ;;
 7: len 8; hex 99bac36c070db3c3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a4; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0646; asc       F;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000007a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070db8f6; asc    l    ;;
 7: len 8; hex 99bac36c070dbbb0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a5; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa06cb; asc        ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000007a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dc0fe; asc    l    ;;
 7: len 8; hex 99bac36c070dc3ae; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a6; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0750; asc       P;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000007a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dc8d6; asc    l    ;;
 7: len 8; hex 99bac36c070dcc30; asc    l   0;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a7; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa07d5; asc        ;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000007a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dd28a; asc    l    ;;
 7: len 8; hex 99bac36c070dd55f; asc    l   _;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a8; asc         ;;
 1: len 6; hex 000000032fe3; asc     / ;;
 2: len 7; hex 020000013303f4; asc     3  ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000007a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dda92; asc    l    ;;
 7: len 8; hex 99bac36c070ddd3c; asc    l   <;;
 8: len 8; hex 99bac36c08069c72; asc    l   r;;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a9; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa08df; asc        ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000007a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070de2a0; asc    l    ;;
 7: len 8; hex 99bac36c070de55f; asc    l   _;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007aa; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0964; asc       d;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000007aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dea7c; asc    l   |;;
 7: len 8; hex 99bac36c070ded2a; asc    l   *;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ab; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa09e9; asc        ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000007ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070df279; asc    l   y;;
 7: len 8; hex 99bac36c070df52c; asc    l   ,;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ac; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0a6e; asc       n;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000007ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070dfa1c; asc    l    ;;
 7: len 8; hex 99bac36c070dfcbf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ad; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0af3; asc        ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000007ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e01f0; asc    l    ;;
 7: len 8; hex 99bac36c070e04d9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ae; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0b78; asc       x;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000007ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e0a7e; asc    l   ~;;
 7: len 8; hex 99bac36c070e0d99; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007af; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0bfd; asc        ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000007af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e1350; asc    l   P;;
 7: len 8; hex 99bac36c070e164e; asc    l   N;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b0; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0c82; asc        ;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000007b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e1bc1; asc    l    ;;
 7: len 8; hex 99bac36c070e1f61; asc    l   a;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b1; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0d07; asc        ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000007b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e24a7; asc    l  $ ;;
 7: len 8; hex 99bac36c070e2763; asc    l  'c;;
 8: SQL NULL;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b2; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0d8c; asc        ;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000007b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e2c7b; asc    l  ,{;;
 7: len 8; hex 99bac36c070e2f23; asc    l  /#;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b3; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0e11; asc        ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000007b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e3408; asc    l  4 ;;
 7: len 8; hex 99bac36c070e36a7; asc    l  6 ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b4; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0e96; asc        ;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000007b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e3bfc; asc    l  ; ;;
 7: len 8; hex 99bac36c070e3ea5; asc    l  > ;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b5; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0f1b; asc        ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000007b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e446c; asc    l  Dl;;
 7: len 8; hex 99bac36c070e4761; asc    l  Ga;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b6; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa0fa0; asc        ;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000007b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e4d8d; asc    l  M ;;
 7: len 8; hex 99bac36c070e504b; asc    l  PK;;
 8: SQL NULL;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b7; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1025; asc       %;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000007b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e5547; asc    l  UG;;
 7: len 8; hex 99bac36c070e57fc; asc    l  W ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b8; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa10aa; asc        ;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000007b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e5de7; asc    l  ] ;;
 7: len 8; hex 99bac36c070e60c4; asc    l  ` ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b9; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa112f; asc       /;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000007b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e664c; asc    l  fL;;
 7: len 8; hex 99bac36c070e6970; asc    l  ip;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ba; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa11b4; asc        ;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000007ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e6ee7; asc    l  n ;;
 7: len 8; hex 99bac36c070e71a3; asc    l  q ;;
 8: SQL NULL;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bb; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1239; asc       9;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000007bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e76d8; asc    l  v ;;
 7: len 8; hex 99bac36c070e79b3; asc    l  y ;;
 8: SQL NULL;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bc; asc         ;;
 1: len 6; hex 000000032fdf; asc     / ;;
 2: len 7; hex 020000012e017b; asc     . {;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000007bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e80d4; asc    l    ;;
 7: len 8; hex 99bac36c070e84e9; asc    l    ;;
 8: len 8; hex 99bac36c0804dc92; asc    l    ;;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bd; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1343; asc       C;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000007bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e8d3d; asc    l   =;;
 7: len 8; hex 99bac36c070e914b; asc    l   K;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007be; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa13c8; asc        ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000007be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070e9bca; asc    l    ;;
 7: len 8; hex 99bac36c070e9f7b; asc    l   {;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bf; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa144d; asc       M;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000007bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ea8fa; asc    l    ;;
 7: len 8; hex 99bac36c070eabec; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c0; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa14d2; asc        ;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000007c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070eb15a; asc    l   Z;;
 7: len 8; hex 99bac36c070eb440; asc    l   @;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c1; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1557; asc       W;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000007c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070eba58; asc    l   X;;
 7: len 8; hex 99bac36c070ebe06; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c2; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa15dc; asc        ;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000007c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ec3ec; asc    l    ;;
 7: len 8; hex 99bac36c070ec6ce; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c3; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1661; asc       a;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000007c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ecc46; asc    l   F;;
 7: len 8; hex 99bac36c070ecf4c; asc    l   L;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c4; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa16e6; asc        ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000007c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ed4d8; asc    l    ;;
 7: len 8; hex 99bac36c070ed7c7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c5; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa176b; asc       k;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000007c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070edd6c; asc    l   l;;
 7: len 8; hex 99bac36c070ee053; asc    l   S;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c6; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa17f0; asc        ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000007c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ee618; asc    l    ;;
 7: len 8; hex 99bac36c070ee8f9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c7; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1875; asc       u;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000007c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070eee54; asc    l   T;;
 7: len 8; hex 99bac36c070ef12e; asc    l   .;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c8; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa18fa; asc        ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000007c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070ef89b; asc    l    ;;
 7: len 8; hex 99bac36c070efbd0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c9; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa197f; asc        ;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000007c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070f0190; asc    l    ;;
 7: len 8; hex 99bac36c070f0476; asc    l   v;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ca; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1a04; asc        ;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000007ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070f0a27; asc    l   ';;
 7: len 8; hex 99bac36c070f233f; asc    l  #?;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cb; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1a89; asc        ;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000007cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070f29a6; asc    l  ) ;;
 7: len 8; hex 99bac36c070f2cab; asc    l  , ;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cc; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1b0e; asc        ;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000007cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070f348b; asc    l  4 ;;
 7: len 8; hex 99bac36c070f377f; asc    l  7 ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cd; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1b93; asc        ;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000007cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c070f3d1c; asc    l  = ;;
 7: len 8; hex 99bac36c070f3ffc; asc    l  ? ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ce; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1c18; asc        ;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000007ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0800037a; asc    l   z;;
 7: len 8; hex 99bac36c0800065a; asc    l   Z;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cf; asc         ;;
 1: len 6; hex 000000032f83; asc     / ;;
 2: len 7; hex 02000001aa1c9d; asc        ;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000007cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c08000bee; asc    l    ;;
 7: len 8; hex 99bac36c08000ebf; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d1; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d147e; asc       ~;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000007d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c080076a2; asc    l  v ;;
 7: len 8; hex 99bac36c08007a5e; asc    l  z^;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d2; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d1501; asc        ;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000007d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0800bddf; asc    l    ;;
 7: len 8; hex 99bac36c0800c4ef; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d3; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d1584; asc        ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000007d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0800db7e; asc    l   ~;;
 7: len 8; hex 99bac36c0800df39; asc    l   9;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d4; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d1607; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000007d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0800e678; asc    l   x;;
 7: len 8; hex 99bac36c0800ea30; asc    l   0;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d5; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d168a; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 80000000000007d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c0800f122; asc    l   ";;
 7: len 8; hex 99bac36c0800f4ad; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d6; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d170d; asc        ;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 80000000000007d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c080117e6; asc    l    ;;
 7: len 8; hex 99bac36c08011e37; asc    l   7;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d7; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d1790; asc        ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 80000000000007d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c08012cc5; asc    l  , ;;
 7: len 8; hex 99bac36c0801370e; asc    l  7 ;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d8; asc         ;;
 1: len 6; hex 000000032fc1; asc     / ;;
 2: len 7; hex 010000019d1813; asc        ;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 80000000000007d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c08015afa; asc    l  Z ;;
 7: len 8; hex 99bac36c08016853; asc    l  hS;;
 8: SQL NULL;

Record lock, heap no 252 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d0; asc         ;;
 1: len 6; hex 000000033036; asc     06;;
 2: len 7; hex 0200000187268a; asc      & ;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000007d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36c08001561; asc    l   a;;
 7: len 8; hex 99bac36c08002dfe; asc    l  - ;;
 8: len 8; hex 99bac36c0b03d7fc; asc    l    ;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 117 page no 12 n bits 288 index PRIMARY of table `deadlock_lab`.`member_account` trx id 208989 lock mode S locks rec but not gap waiting
Record lock, heap no 218 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 00000003308b; asc     0 ;;
 2: len 7; hex 02000001502f87; asc     P/ ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343535373034353231; asc tk-455704521;;
 5: len 8; hex 99bac36c0e00403b; asc    l  @;;;
 6: len 8; hex 99bac36c0e00403b; asc    l  @;;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V3a — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:50:45 135358810506816
*** (1) TRANSACTION:
TRANSACTION 225399, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9041, OS thread handle 135358147683904, query id 1080709 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 1932

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 125 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 225399 lock_mode X locks rec but not gap
Record lock, heap no 113 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 000000037077; asc     pw;;
 2: len 7; hex 020000013312bf; asc     3  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d323231393030303930; asc tk-221900090;;
 5: len 8; hex 99bac36cad03544d; asc    l  TM;;
 6: len 8; hex 99bac36cad03544d; asc    l  TM;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 123 page no 27 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 225399 lock_mode X locks rec but not gap waiting
Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078c; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63bc9; asc      ; ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 800000000000078c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7051a50; asc    l   P;;
 7: len 8; hex 99bac36ca7051de0; asc    l    ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 225367, ACTIVE 1 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 65 lock struct(s), heap size 24696, 5252 row lock(s), undo log entries 1294
MySQL thread id 9000, OS thread handle 135358149797440, query id 1081271 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (932, 3932, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 123 page no 27 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 225367 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075b; asc        [;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62254; asc      "T;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 800000000000075b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7026ade; asc    l  j ;;
 7: len 8; hex 99bac36ca7026e06; asc    l  n ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075c; asc        \;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f622d9; asc      " ;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 800000000000075c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70274ae; asc    l  t ;;
 7: len 8; hex 99bac36ca70277ec; asc    l  w ;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075d; asc        ];;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6235e; asc      #^;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 800000000000075d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7027dd8; asc    l  } ;;
 7: len 8; hex 99bac36ca7028107; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075f; asc        _;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62468; asc      $h;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 800000000000075f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702935d; asc    l   ];;
 7: len 8; hex 99bac36ca7029740; asc    l   @;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000760; asc        `;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f624ed; asc      $ ;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000760; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7029e3e; asc    l   >;;
 7: len 8; hex 99bac36ca702a190; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000761; asc        a;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62572; asc      %r;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000761; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702a7cd; asc    l    ;;
 7: len 8; hex 99bac36ca702ab3e; asc    l   >;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000762; asc        b;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f625f7; asc      % ;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 8000000000000762; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702b2be; asc    l    ;;
 7: len 8; hex 99bac36ca702b680; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000763; asc        c;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6267c; asc      &|;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 8000000000000763; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702bdab; asc    l    ;;
 7: len 8; hex 99bac36ca702c121; asc    l   !;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000764; asc        d;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62701; asc      ' ;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 8000000000000764; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702c930; asc    l   0;;
 7: len 8; hex 99bac36ca702cc6d; asc    l   m;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000765; asc        e;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62786; asc      ' ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 8000000000000765; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702d484; asc    l    ;;
 7: len 8; hex 99bac36ca702d843; asc    l   C;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000766; asc        f;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6280b; asc      ( ;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 8000000000000766; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702df3f; asc    l   ?;;
 7: len 8; hex 99bac36ca702e355; asc    l   U;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000767; asc        g;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62890; asc      ( ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 8000000000000767; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702eb7e; asc    l   ~;;
 7: len 8; hex 99bac36ca702ef68; asc    l   h;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000768; asc        h;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62915; asc      ) ;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000768; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca702f844; asc    l   D;;
 7: len 8; hex 99bac36ca702fc5b; asc    l   [;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000769; asc        i;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6299a; asc      ) ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000769; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70304d2; asc    l    ;;
 7: len 8; hex 99bac36ca7030992; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076a; asc        j;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62a1f; asc      * ;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 800000000000076a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703127d; asc    l   };;
 7: len 8; hex 99bac36ca70316e6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076b; asc        k;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62aa4; asc      * ;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 800000000000076b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7031fd8; asc    l    ;;
 7: len 8; hex 99bac36ca703236d; asc    l  #m;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076c; asc        l;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62b29; asc      +);;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 800000000000076c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7032add; asc    l  * ;;
 7: len 8; hex 99bac36ca7032ec0; asc    l  . ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076d; asc        m;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62bae; asc      + ;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 800000000000076d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703357c; asc    l  5|;;
 7: len 8; hex 99bac36ca70338d1; asc    l  8 ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076e; asc        n;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62c33; asc      ,3;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 800000000000076e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7033fc4; asc    l  ? ;;
 7: len 8; hex 99bac36ca7034382; asc    l  C ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076f; asc        o;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62cb8; asc      , ;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 800000000000076f; asc        o;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7034ac9; asc    l  J ;;
 7: len 8; hex 99bac36ca7034e35; asc    l  N5;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000770; asc        p;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62d3d; asc      -=;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000770; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703559d; asc    l  U ;;
 7: len 8; hex 99bac36ca70359a7; asc    l  Y ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000771; asc        q;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62dc2; asc      - ;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000771; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703613d; asc    l  a=;;
 7: len 8; hex 99bac36ca7036525; asc    l  e%;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000772; asc        r;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62e47; asc      .G;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 8000000000000772; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7036c3e; asc    l  l>;;
 7: len 8; hex 99bac36ca7036fbb; asc    l  o ;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000773; asc        s;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62ecc; asc      . ;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 8000000000000773; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70376d4; asc    l  v ;;
 7: len 8; hex 99bac36ca7037a01; asc    l  z ;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000774; asc        t;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62f51; asc      /Q;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 8000000000000774; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70380bd; asc    l    ;;
 7: len 8; hex 99bac36ca70383d8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000775; asc        u;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f62fd6; asc      / ;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 8000000000000775; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7038a29; asc    l   );;
 7: len 8; hex 99bac36ca7038d4f; asc    l   O;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000776; asc        v;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6305b; asc      0[;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 8000000000000776; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703934d; asc    l   M;;
 7: len 8; hex 99bac36ca7039668; asc    l   h;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000777; asc        w;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f630e0; asc      0 ;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 8000000000000777; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7039c73; asc    l   s;;
 7: len 8; hex 99bac36ca7039fad; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000778; asc        x;;
 1: len 6; hex 000000036fa2; asc     o ;;
 2: len 7; hex 01000001e80d9e; asc        ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000778; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703a576; asc    l   v;;
 7: len 8; hex 99bac36ca703a897; asc    l    ;;
 8: len 8; hex 99bac36ca7090dd4; asc    l    ;;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000779; asc        y;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f631ea; asc      1 ;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000779; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703ae85; asc    l    ;;
 7: len 8; hex 99bac36ca703b19c; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077a; asc        z;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6326f; asc      2o;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 800000000000077a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703b7f9; asc    l    ;;
 7: len 8; hex 99bac36ca703bc0f; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077b; asc        {;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f632f4; asc      2 ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 800000000000077b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca703e3d0; asc    l    ;;
 7: len 8; hex 99bac36ca703e7d9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077c; asc        |;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63379; asc      3y;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 800000000000077c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7043e6d; asc    l  >m;;
 7: len 8; hex 99bac36ca704424e; asc    l  BN;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077d; asc        };;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f633fe; asc      3 ;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 800000000000077d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7046097; asc    l  ` ;;
 7: len 8; hex 99bac36ca7047213; asc    l  r ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077e; asc        ~;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63483; asc      4 ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 800000000000077e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7047989; asc    l  y ;;
 7: len 8; hex 99bac36ca7047cb8; asc    l  | ;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077f; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63508; asc      5 ;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 800000000000077f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704830c; asc    l    ;;
 7: len 8; hex 99bac36ca7048693; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000780; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6358d; asc      5 ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000780; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7048e0d; asc    l    ;;
 7: len 8; hex 99bac36ca704914a; asc    l   J;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000782; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63697; asc      6 ;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 8000000000000782; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704a367; asc    l   g;;
 7: len 8; hex 99bac36ca704a6ef; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000783; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f6371c; asc      7 ;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 8000000000000783; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704b1b3; asc    l    ;;
 7: len 8; hex 99bac36ca704b5d3; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000784; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f637a1; asc      7 ;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 8000000000000784; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704be23; asc    l   #;;
 7: len 8; hex 99bac36ca704c2a4; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000785; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63826; asc      8&;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 8000000000000785; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704ca8f; asc    l    ;;
 7: len 8; hex 99bac36ca704ce79; asc    l   y;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000786; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f638ab; asc      8 ;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 8000000000000786; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704d765; asc    l   e;;
 7: len 8; hex 99bac36ca704dac8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000787; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63930; asc      90;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 8000000000000787; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704e0e2; asc    l    ;;
 7: len 8; hex 99bac36ca704e3ff; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000788; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f639b5; asc      9 ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 8000000000000788; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704ea73; asc    l   s;;
 7: len 8; hex 99bac36ca704ee35; asc    l   5;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000789; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63a3a; asc      ::;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 8000000000000789; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca704f747; asc    l   G;;
 7: len 8; hex 99bac36ca704facd; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078a; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63abf; asc      : ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 800000000000078a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705032e; asc    l   .;;
 7: len 8; hex 99bac36ca7050737; asc    l   7;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078b; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63b44; asc      ;D;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 800000000000078b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7050f5b; asc    l   [;;
 7: len 8; hex 99bac36ca7051313; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078c; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63bc9; asc      ; ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 800000000000078c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7051a50; asc    l   P;;
 7: len 8; hex 99bac36ca7051de0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078d; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63c4e; asc      <N;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 800000000000078d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705253a; asc    l  %:;;
 7: len 8; hex 99bac36ca705289e; asc    l  ( ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078e; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63cd3; asc      < ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 800000000000078e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7053019; asc    l  0 ;;
 7: len 8; hex 99bac36ca7053399; asc    l  3 ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078f; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63d58; asc      =X;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 800000000000078f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7053ad0; asc    l  : ;;
 7: len 8; hex 99bac36ca7053e6f; asc    l  >o;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000790; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63ddd; asc      = ;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 8000000000000790; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70545f1; asc    l  E ;;
 7: len 8; hex 99bac36ca7054948; asc    l  IH;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000791; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63e62; asc      >b;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 8000000000000791; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7054fb0; asc    l  O ;;
 7: len 8; hex 99bac36ca705531c; asc    l  S ;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000792; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63ee7; asc      > ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 8000000000000792; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70559c9; asc    l  Y ;;
 7: len 8; hex 99bac36ca7055d5c; asc    l  ]\;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000793; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f63f6c; asc      ?l;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 8000000000000793; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705645a; asc    l  dZ;;
 7: len 8; hex 99bac36ca70567b0; asc    l  g ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000794; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8008f; asc        ;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 8000000000000794; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7056e7c; asc    l  n|;;
 7: len 8; hex 99bac36ca70571dc; asc    l  q ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000795; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80114; asc        ;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 8000000000000795; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70578b0; asc    l  x ;;
 7: len 8; hex 99bac36ca7057c34; asc    l  |4;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000796; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80199; asc        ;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 8000000000000796; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7058396; asc    l    ;;
 7: len 8; hex 99bac36ca70586fb; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000797; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8021e; asc        ;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 8000000000000797; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7058d7f; asc    l    ;;
 7: len 8; hex 99bac36ca705907e; asc    l   ~;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000798; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f802a3; asc        ;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 8000000000000798; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7059703; asc    l    ;;
 7: len 8; hex 99bac36ca7059a10; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000799; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80328; asc       (;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 8000000000000799; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705a09a; asc    l    ;;
 7: len 8; hex 99bac36ca705a3c2; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079a; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f803ad; asc        ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 800000000000079a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705a989; asc    l    ;;
 7: len 8; hex 99bac36ca705ac98; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079b; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80432; asc       2;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 800000000000079b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705b2b5; asc    l    ;;
 7: len 8; hex 99bac36ca705b5fe; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079c; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f804b7; asc        ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 800000000000079c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705bc4d; asc    l   M;;
 7: len 8; hex 99bac36ca705bf52; asc    l   R;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079d; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8053c; asc       <;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 800000000000079d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705c663; asc    l   c;;
 7: len 8; hex 99bac36ca705c9b8; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079e; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f805c1; asc        ;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 800000000000079e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705d131; asc    l   1;;
 7: len 8; hex 99bac36ca705d4ad; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079f; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80646; asc       F;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 800000000000079f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705dab9; asc    l    ;;
 7: len 8; hex 99bac36ca705ddf2; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a0; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f806cb; asc        ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000007a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705e416; asc    l    ;;
 7: len 8; hex 99bac36ca705e76b; asc    l   k;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a1; asc         ;;
 1: len 6; hex 000000036fbf; asc     o ;;
 2: len 7; hex 02000001950193; asc        ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000007a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705edcb; asc    l    ;;
 7: len 8; hex 99bac36ca705f0fe; asc    l    ;;
 8: len 8; hex 99bac36ca709e6ef; asc    l    ;;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a2; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f807d5; asc        ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000007a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705f6f3; asc    l    ;;
 7: len 8; hex 99bac36ca705fa18; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a3; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8085a; asc       Z;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000007a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca705fff1; asc    l    ;;
 7: len 8; hex 99bac36ca706030e; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a4; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f808df; asc        ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000007a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706095f; asc    l   _;;
 7: len 8; hex 99bac36ca7060c72; asc    l   r;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a5; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80964; asc       d;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000007a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70613cc; asc    l    ;;
 7: len 8; hex 99bac36ca7061701; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a6; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f809e9; asc        ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000007a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7061d1d; asc    l    ;;
 7: len 8; hex 99bac36ca7062037; asc    l   7;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a7; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80a6e; asc       n;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000007a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7062635; asc    l  &5;;
 7: len 8; hex 99bac36ca706296c; asc    l  )l;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a8; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80af3; asc        ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000007a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7062f69; asc    l  /i;;
 7: len 8; hex 99bac36ca706328f; asc    l  2 ;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a9; asc         ;;
 1: len 6; hex 000000036fba; asc     o ;;
 2: len 7; hex 01000001d412a2; asc        ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000007a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70638cb; asc    l  8 ;;
 7: len 8; hex 99bac36ca7063bd5; asc    l  ; ;;
 8: len 8; hex 99bac36ca70999ae; asc    l    ;;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007aa; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80bfd; asc        ;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000007aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70641b8; asc    l  A ;;
 7: len 8; hex 99bac36ca70644f0; asc    l  D ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ab; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80c82; asc        ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000007ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7064b3d; asc    l  K=;;
 7: len 8; hex 99bac36ca7064e8b; asc    l  N ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ac; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80d07; asc        ;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000007ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70655df; asc    l  U ;;
 7: len 8; hex 99bac36ca7065920; asc    l  Y ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ad; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80d8c; asc        ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000007ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7065f3b; asc    l  _;;;
 7: len 8; hex 99bac36ca706624a; asc    l  bJ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ae; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80e11; asc        ;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000007ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7066842; asc    l  hB;;
 7: len 8; hex 99bac36ca7066b6f; asc    l  ko;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007af; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80e96; asc        ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000007af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706714e; asc    l  qN;;
 7: len 8; hex 99bac36ca70674b6; asc    l  t ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b0; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80f1b; asc        ;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000007b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7067b22; asc    l  {";;
 7: len 8; hex 99bac36ca7067e4d; asc    l  ~M;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b1; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f80fa0; asc        ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000007b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7068473; asc    l   s;;
 7: len 8; hex 99bac36ca7068867; asc    l   g;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b2; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81025; asc       %;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000007b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7068eec; asc    l    ;;
 7: len 8; hex 99bac36ca70691ca; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b3; asc         ;;
 1: len 6; hex 000000036fa7; asc     o ;;
 2: len 7; hex 020000012015f7; asc        ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000007b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70697f2; asc    l    ;;
 7: len 8; hex 99bac36ca7069b0c; asc    l    ;;
 8: len 8; hex 99bac36ca70922e6; asc    l  " ;;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b4; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8112f; asc       /;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000007b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706a5bb; asc    l    ;;
 7: len 8; hex 99bac36ca706aa3d; asc    l   =;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b5; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f811b4; asc        ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000007b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706b375; asc    l   u;;
 7: len 8; hex 99bac36ca706b890; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b6; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81239; asc       9;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000007b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706c289; asc    l    ;;
 7: len 8; hex 99bac36ca706c7b4; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b7; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f812be; asc        ;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000007b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca706d306; asc    l    ;;
 7: len 8; hex 99bac36ca707199e; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b8; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81343; asc       C;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000007b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70723b5; asc    l  # ;;
 7: len 8; hex 99bac36ca70727af; asc    l  ' ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b9; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f813c8; asc        ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000007b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7072ef5; asc    l  . ;;
 7: len 8; hex 99bac36ca7073253; asc    l  2S;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ba; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8144d; asc       M;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000007ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7073ce2; asc    l  < ;;
 7: len 8; hex 99bac36ca707410a; asc    l  A ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bb; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f814d2; asc        ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000007bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7074d57; asc    l  MW;;
 7: len 8; hex 99bac36ca7075120; asc    l  Q ;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bc; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81557; asc       W;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000007bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70757a7; asc    l  W ;;
 7: len 8; hex 99bac36ca7075ad2; asc    l  Z ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bd; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f815dc; asc        ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000007bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70761bf; asc    l  a ;;
 7: len 8; hex 99bac36ca70764ff; asc    l  d ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007be; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81661; asc       a;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000007be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7076b14; asc    l  k ;;
 7: len 8; hex 99bac36ca7076e5c; asc    l  n\;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bf; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f816e6; asc        ;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000007bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707748b; asc    l  t ;;
 7: len 8; hex 99bac36ca7077784; asc    l  w ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c0; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8176b; asc       k;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000007c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7077d6f; asc    l  }o;;
 7: len 8; hex 99bac36ca70780f9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c1; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f817f0; asc        ;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000007c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707878a; asc    l    ;;
 7: len 8; hex 99bac36ca7078b19; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c2; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81875; asc       u;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000007c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70791f9; asc    l    ;;
 7: len 8; hex 99bac36ca70795d6; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c3; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f818fa; asc        ;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000007c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7079ca4; asc    l    ;;
 7: len 8; hex 99bac36ca7079fe9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c4; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f8197f; asc        ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000007c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707a630; asc    l   0;;
 7: len 8; hex 99bac36ca707a983; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c5; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81a04; asc        ;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000007c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707af77; asc    l   w;;
 7: len 8; hex 99bac36ca707b2e9; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c6; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81a89; asc        ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000007c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707ba76; asc    l   v;;
 7: len 8; hex 99bac36ca707bdf4; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c7; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81b0e; asc        ;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000007c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707c4ea; asc    l    ;;
 7: len 8; hex 99bac36ca707c84d; asc    l   M;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c8; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81b93; asc        ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000007c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707cef9; asc    l    ;;
 7: len 8; hex 99bac36ca707d24b; asc    l   K;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c9; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81c18; asc        ;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000007c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707d88c; asc    l    ;;
 7: len 8; hex 99bac36ca707dc29; asc    l   );;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ca; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81c9d; asc        ;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000007ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca707e50e; asc    l    ;;
 7: len 8; hex 99bac36ca707fd4d; asc    l   M;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cb; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81d22; asc       ";;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000007cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca708048e; asc    l    ;;
 7: len 8; hex 99bac36ca70807c0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cc; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81da7; asc        ;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000007cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7080ea1; asc    l    ;;
 7: len 8; hex 99bac36ca70811b7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cd; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81e2c; asc       ,;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000007cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7081813; asc    l    ;;
 7: len 8; hex 99bac36ca7081b2c; asc    l   ,;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ce; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81eb1; asc        ;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000007ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca708213e; asc    l  !>;;
 7: len 8; hex 99bac36ca7082439; asc    l  $9;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cf; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81f36; asc       6;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000007cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70829f5; asc    l  ) ;;
 7: len 8; hex 99bac36ca7082cd5; asc    l  , ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d0; asc         ;;
 1: len 6; hex 000000036f61; asc     oa;;
 2: len 7; hex 01000001f81fbb; asc        ;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000007d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70832ef; asc    l  2 ;;
 7: len 8; hex 99bac36ca708501f; asc    l  P ;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d1; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 02000001842eed; asc      . ;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000007d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca708950e; asc    l    ;;
 7: len 8; hex 99bac36ca7089fb7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d2; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 02000001842f70; asc      /p;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000007d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca709ecaf; asc    l    ;;
 7: len 8; hex 99bac36ca709efe7; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d3; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 02000001842ff3; asc      / ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000007d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca709f62e; asc    l   .;;
 7: len 8; hex 99bac36ca709f986; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d4; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 02000001843076; asc      0v;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000007d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca709ff97; asc    l    ;;
 7: len 8; hex 99bac36ca70a02c0; asc    l    ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d5; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 020000018430f9; asc      0 ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 80000000000007d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70a0869; asc    l   i;;
 7: len 8; hex 99bac36ca70a0b6e; asc    l   n;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d6; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 0200000184317c; asc      1|;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 80000000000007d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70a1184; asc    l    ;;
 7: len 8; hex 99bac36ca70a1538; asc    l   8;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d7; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 020000018431ff; asc      1 ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 80000000000007d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70a1c60; asc    l   `;;
 7: len 8; hex 99bac36ca70a43ce; asc    l  C ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d8; asc         ;;
 1: len 6; hex 000000036f8f; asc     o ;;
 2: len 7; hex 02000001843282; asc      2 ;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 80000000000007d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70a4b99; asc    l  K ;;
 7: len 8; hex 99bac36ca70a4fc5; asc    l  O ;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000781; asc         ;;
 1: len 6; hex 000000036fef; asc     o ;;
 2: len 7; hex 010000010c0193; asc        ;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000781; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca70497bc; asc    l    ;;
 7: len 8; hex 99bac36ca7049b63; asc    l   c;;
 8: len 8; hex 99bac36ca900be06; asc    l    ;;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075e; asc        ^;;
 1: len 6; hex 000000037037; asc     p7;;
 2: len 7; hex 020000018c2f61; asc      /a;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 800000000000075e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36ca7028726; asc    l   &;;
 7: len 8; hex 99bac36ca7028ae5; asc    l    ;;
 8: len 8; hex 99bac36caa0a70fe; asc    l  p ;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 125 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 225367 lock mode S locks rec but not gap waiting
Record lock, heap no 113 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 000000037077; asc     pw;;
 2: len 7; hex 020000013312bf; asc     3  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d323231393030303930; asc tk-221900090;;
 5: len 8; hex 99bac36cad03544d; asc    l  TM;;
 6: len 8; hex 99bac36cad03544d; asc    l  TM;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V3c — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:55:44 135358810506816
*** (1) TRANSACTION:
TRANSACTION 260592, ACTIVE 1 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9037, OS thread handle 135358137116224, query id 1193045 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 1981

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 141 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 260592 lock_mode X locks rec but not gap
Record lock, heap no 161 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 00000003f9f0; asc       ;;
 2: len 7; hex 02000001501ac0; asc     P  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d333236303033333030; asc tk-326003300;;
 5: len 8; hex 99bac36deb00e9c5; asc    m    ;;
 6: len 8; hex 99bac36deb00e9c5; asc    m    ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 139 page no 26 n bits 192 index PRIMARY of table `deadlock_lab`.`notification` trx id 260592 lock_mode X locks rec but not gap waiting
Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bd; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112280b; asc      ( ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000007bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c2e43; asc    m  .C;;
 7: len 8; hex 99bac36de60c2f9b; asc    m  / ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 260569, ACTIVE 1 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 68 lock struct(s), heap size 24696, 5448 row lock(s), undo log entries 1441
MySQL thread id 9000, OS thread handle 135358149797440, query id 1194547 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (981, 3981, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 139 page no 26 n bits 192 index PRIMARY of table `deadlock_lab`.`notification` trx id 260569 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075c; asc        \;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113508; asc      5 ;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 800000000000075c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608d2ee; asc    m    ;;
 7: len 8; hex 99bac36de608d45c; asc    m   \;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075d; asc        ];;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000111358d; asc      5 ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 800000000000075d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608d9fc; asc    m    ;;
 7: len 8; hex 99bac36de608db7c; asc    m   |;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075e; asc        ^;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113612; asc      6 ;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 800000000000075e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608e0e1; asc    m    ;;
 7: len 8; hex 99bac36de608e25f; asc    m   _;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000075f; asc        _;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113697; asc      6 ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 800000000000075f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608e8a3; asc    m    ;;
 7: len 8; hex 99bac36de608ea56; asc    m   V;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000760; asc        `;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000111371c; asc      7 ;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000760; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608efb0; asc    m    ;;
 7: len 8; hex 99bac36de608f0d8; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000761; asc        a;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011137a1; asc      7 ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000761; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608f62b; asc    m   +;;
 7: len 8; hex 99bac36de608f7a1; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000762; asc        b;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113826; asc      8&;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 8000000000000762; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de608fd33; asc    m   3;;
 7: len 8; hex 99bac36de608fe88; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000763; asc        c;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011138ab; asc      8 ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 8000000000000763; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609042c; asc    m   ,;;
 7: len 8; hex 99bac36de6090556; asc    m   V;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000764; asc        d;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113930; asc      90;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 8000000000000764; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6090a59; asc    m   Y;;
 7: len 8; hex 99bac36de6090c49; asc    m   I;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000765; asc        e;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011139b5; asc      9 ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 8000000000000765; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609126d; asc    m   m;;
 7: len 8; hex 99bac36de609142b; asc    m   +;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000766; asc        f;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113a3a; asc      ::;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 8000000000000766; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60919ba; asc    m    ;;
 7: len 8; hex 99bac36de6091b2d; asc    m   -;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000767; asc        g;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113abf; asc      : ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 8000000000000767; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60920cc; asc    m    ;;
 7: len 8; hex 99bac36de609223a; asc    m  ":;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000768; asc        h;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113b44; asc      ;D;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000768; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60927d3; asc    m  ' ;;
 7: len 8; hex 99bac36de6092942; asc    m  )B;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000769; asc        i;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113bc9; asc      ; ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000769; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6092ecb; asc    m  . ;;
 7: len 8; hex 99bac36de6093036; asc    m  06;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076a; asc        j;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113c4e; asc      <N;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 800000000000076a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609360b; asc    m  6 ;;
 7: len 8; hex 99bac36de6093776; asc    m  7v;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076b; asc        k;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113cd3; asc      < ;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 800000000000076b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6093cf4; asc    m  < ;;
 7: len 8; hex 99bac36de6093e31; asc    m  >1;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076c; asc        l;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113d58; asc      =X;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 800000000000076c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6094392; asc    m  C ;;
 7: len 8; hex 99bac36de609450a; asc    m  E ;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076d; asc        m;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113ddd; asc      = ;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 800000000000076d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6094af1; asc    m  J ;;
 7: len 8; hex 99bac36de6094cb3; asc    m  L ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076e; asc        n;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113e62; asc      >b;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 800000000000076e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609524f; asc    m  RO;;
 7: len 8; hex 99bac36de60953a0; asc    m  S ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000076f; asc        o;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113ee7; asc      > ;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 800000000000076f; asc        o;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6095943; asc    m  YC;;
 7: len 8; hex 99bac36de6095cca; asc    m  \ ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000770; asc        p;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001113f6c; asc      ?l;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000770; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60962bd; asc    m  b ;;
 7: len 8; hex 99bac36de609641e; asc    m  d ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000771; asc        q;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112008f; asc        ;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000771; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60969d2; asc    m  i ;;
 7: len 8; hex 99bac36de6096b12; asc    m  k ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000772; asc        r;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120114; asc        ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 8000000000000772; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6097096; asc    m  p ;;
 7: len 8; hex 99bac36de60971e1; asc    m  q ;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000773; asc        s;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120199; asc        ;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 8000000000000773; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609779f; asc    m  w ;;
 7: len 8; hex 99bac36de60978f0; asc    m  x ;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000774; asc        t;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112021e; asc        ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 8000000000000774; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6097e57; asc    m  ~W;;
 7: len 8; hex 99bac36de6097fb7; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000775; asc        u;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011202a3; asc        ;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 8000000000000775; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60984e8; asc    m    ;;
 7: len 8; hex 99bac36de6098670; asc    m   p;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000776; asc        v;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120328; asc       (;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 8000000000000776; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de6098c13; asc    m    ;;
 7: len 8; hex 99bac36de6098d69; asc    m   i;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000777; asc        w;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011203ad; asc        ;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 8000000000000777; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60992b9; asc    m    ;;
 7: len 8; hex 99bac36de60993e2; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000778; asc        x;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120432; asc       2;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000778; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60999b8; asc    m    ;;
 7: len 8; hex 99bac36de6099b0e; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000779; asc        y;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011204b7; asc        ;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000779; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609a052; asc    m   R;;
 7: len 8; hex 99bac36de609a1cc; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077a; asc        z;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112053c; asc       <;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 800000000000077a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609a70e; asc    m    ;;
 7: len 8; hex 99bac36de609a849; asc    m   I;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077b; asc        {;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011205c1; asc        ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 800000000000077b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609c82c; asc    m   ,;;
 7: len 8; hex 99bac36de609c981; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077c; asc        |;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120646; asc       F;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 800000000000077c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609cf1f; asc    m    ;;
 7: len 8; hex 99bac36de609d08c; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077d; asc        };;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011206cb; asc        ;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 800000000000077d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609d6ac; asc    m    ;;
 7: len 8; hex 99bac36de609d7f3; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077e; asc        ~;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120750; asc       P;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 800000000000077e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609dd27; asc    m   ';;
 7: len 8; hex 99bac36de609de98; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000077f; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011207d5; asc        ;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 800000000000077f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de609e3bd; asc    m    ;;
 7: len 8; hex 99bac36de60a153b; asc    m   ;;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000780; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112085a; asc       Z;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000780; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a1b28; asc    m   (;;
 7: len 8; hex 99bac36de60a1cda; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000781; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011208df; asc        ;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000781; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a2273; asc    m  "s;;
 7: len 8; hex 99bac36de60a23da; asc    m  # ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000782; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120964; asc       d;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 8000000000000782; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a2978; asc    m  )x;;
 7: len 8; hex 99bac36de60a2ae7; asc    m  * ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000783; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011209e9; asc        ;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 8000000000000783; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a3067; asc    m  0g;;
 7: len 8; hex 99bac36de60a31a5; asc    m  1 ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000784; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120a6e; asc       n;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 8000000000000784; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a3740; asc    m  7@;;
 7: len 8; hex 99bac36de60a388a; asc    m  8 ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000785; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120af3; asc        ;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 8000000000000785; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a3e38; asc    m  >8;;
 7: len 8; hex 99bac36de60a3f7d; asc    m  ?};;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000786; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120b78; asc       x;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 8000000000000786; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a44f5; asc    m  D ;;
 7: len 8; hex 99bac36de60a4684; asc    m  F ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000787; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120bfd; asc        ;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 8000000000000787; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a4d9d; asc    m  M ;;
 7: len 8; hex 99bac36de60a4f60; asc    m  O`;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000788; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120c82; asc        ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 8000000000000788; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a5543; asc    m  UC;;
 7: len 8; hex 99bac36de60a56e6; asc    m  V ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000789; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120d07; asc        ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 8000000000000789; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a5cbb; asc    m  \ ;;
 7: len 8; hex 99bac36de60a5e17; asc    m  ^ ;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078a; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120d8c; asc        ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 800000000000078a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a637b; asc    m  c{;;
 7: len 8; hex 99bac36de60a64c4; asc    m  d ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078b; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120e11; asc        ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 800000000000078b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a6a56; asc    m  jV;;
 7: len 8; hex 99bac36de60a6ba4; asc    m  k ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078c; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120e96; asc        ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 800000000000078c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a714f; asc    m  qO;;
 7: len 8; hex 99bac36de60a72ad; asc    m  r ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078d; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120f1b; asc        ;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 800000000000078d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a788a; asc    m  x ;;
 7: len 8; hex 99bac36de60a79da; asc    m  y ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078e; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001120fa0; asc        ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 800000000000078e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60a7f3d; asc    m   =;;
 7: len 8; hex 99bac36de60ab0da; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000078f; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121025; asc       %;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 800000000000078f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ab6a0; asc    m    ;;
 7: len 8; hex 99bac36de60ab80c; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000790; asc         ;;
 1: len 6; hex 00000003f9bb; asc       ;;
 2: len 7; hex 01000000db1ab3; asc        ;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 8000000000000790; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60abdda; asc    m    ;;
 7: len 8; hex 99bac36de60abf2f; asc    m   /;;
 8: len 8; hex 99bac36de9082244; asc    m  "D;;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000791; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112112f; asc       /;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 8000000000000791; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ac4fa; asc    m    ;;
 7: len 8; hex 99bac36de60ac644; asc    m   D;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000792; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011211b4; asc        ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 8000000000000792; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60acb95; asc    m    ;;
 7: len 8; hex 99bac36de60accd5; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000793; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121239; asc       9;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 8000000000000793; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ad323; asc    m   #;;
 7: len 8; hex 99bac36de60ad4b5; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000794; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011212be; asc        ;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 8000000000000794; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ada7c; asc    m   |;;
 7: len 8; hex 99bac36de60b0e48; asc    m   H;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000795; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121343; asc       C;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 8000000000000795; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b14cb; asc    m    ;;
 7: len 8; hex 99bac36de60b1643; asc    m   C;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000796; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011213c8; asc        ;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 8000000000000796; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b1be3; asc    m    ;;
 7: len 8; hex 99bac36de60b1d3b; asc    m   ;;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000797; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112144d; asc       M;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 8000000000000797; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b2372; asc    m  #r;;
 7: len 8; hex 99bac36de60b2512; asc    m  % ;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000798; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011214d2; asc        ;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 8000000000000798; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b2b21; asc    m  +!;;
 7: len 8; hex 99bac36de60b2c6d; asc    m  ,m;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000799; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121557; asc       W;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 8000000000000799; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b31fe; asc    m  1 ;;
 7: len 8; hex 99bac36de60b3383; asc    m  3 ;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079a; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011215dc; asc        ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 800000000000079a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b396f; asc    m  9o;;
 7: len 8; hex 99bac36de60b3b08; asc    m  ; ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079b; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121661; asc       a;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 800000000000079b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b4092; asc    m  @ ;;
 7: len 8; hex 99bac36de60b421c; asc    m  B ;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079c; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011216e6; asc        ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 800000000000079c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b47b0; asc    m  G ;;
 7: len 8; hex 99bac36de60b48f0; asc    m  H ;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079d; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112176b; asc       k;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 800000000000079d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b4e79; asc    m  Ny;;
 7: len 8; hex 99bac36de60b4fbc; asc    m  O ;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079e; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011217f0; asc        ;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 800000000000079e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b5524; asc    m  U$;;
 7: len 8; hex 99bac36de60b5670; asc    m  Vp;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000079f; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121875; asc       u;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 800000000000079f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b5ba3; asc    m  [ ;;
 7: len 8; hex 99bac36de60b5d0a; asc    m  ] ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a0; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011218fa; asc        ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000007a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b6251; asc    m  bQ;;
 7: len 8; hex 99bac36de60b63a9; asc    m  c ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a1; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112197f; asc        ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000007a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b6944; asc    m  iD;;
 7: len 8; hex 99bac36de60b6aca; asc    m  j ;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a2; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121a04; asc        ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000007a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b701f; asc    m  p ;;
 7: len 8; hex 99bac36de60b71d8; asc    m  q ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a3; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121a89; asc        ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000007a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b7866; asc    m  xf;;
 7: len 8; hex 99bac36de60b79d1; asc    m  y ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a4; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121b0e; asc        ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000007a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b7f60; asc    m   `;;
 7: len 8; hex 99bac36de60b80ed; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a5; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121b93; asc        ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000007a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b86d9; asc    m    ;;
 7: len 8; hex 99bac36de60b885f; asc    m   _;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a6; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121c18; asc        ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000007a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b8e18; asc    m    ;;
 7: len 8; hex 99bac36de60b8f67; asc    m   g;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a7; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121c9d; asc        ;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000007a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b94db; asc    m    ;;
 7: len 8; hex 99bac36de60b9633; asc    m   3;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a8; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121d22; asc       ";;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000007a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60b9bb8; asc    m    ;;
 7: len 8; hex 99bac36de60b9d13; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007a9; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121da7; asc        ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000007a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ba26a; asc    m   j;;
 7: len 8; hex 99bac36de60ba417; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007aa; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121e2c; asc       ,;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000007aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ba967; asc    m   g;;
 7: len 8; hex 99bac36de60baaa0; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ab; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121eb1; asc        ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000007ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bb014; asc    m    ;;
 7: len 8; hex 99bac36de60bb14a; asc    m   J;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ac; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121f36; asc       6;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000007ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bb68d; asc    m    ;;
 7: len 8; hex 99bac36de60bb7b7; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ad; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001121fbb; asc        ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000007ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bbcc8; asc    m    ;;
 7: len 8; hex 99bac36de60bbded; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ae; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122040; asc       @;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000007ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bc2fe; asc    m    ;;
 7: len 8; hex 99bac36de60bc44e; asc    m   N;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007af; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011220c5; asc        ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000007af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bc9db; asc    m    ;;
 7: len 8; hex 99bac36de60bcb9c; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b0; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112214a; asc      !J;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000007b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bd17a; asc    m   z;;
 7: len 8; hex 99bac36de60bd308; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b1; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011221cf; asc      ! ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000007b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bd8f6; asc    m    ;;
 7: len 8; hex 99bac36de60bdaf3; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b2; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122254; asc      "T;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000007b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60be177; asc    m   w;;
 7: len 8; hex 99bac36de60be302; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b3; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011222d9; asc      " ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000007b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60be87c; asc    m   |;;
 7: len 8; hex 99bac36de60bea32; asc    m   2;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b4; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112235e; asc      #^;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000007b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bf031; asc    m   1;;
 7: len 8; hex 99bac36de60bf16c; asc    m   l;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b5; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011223e3; asc      # ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000007b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bf704; asc    m    ;;
 7: len 8; hex 99bac36de60bf849; asc    m   I;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b6; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122468; asc      $h;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000007b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60bfdd1; asc    m    ;;
 7: len 8; hex 99bac36de60bff07; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b7; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011224ed; asc      $ ;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000007b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c0437; asc    m   7;;
 7: len 8; hex 99bac36de60c058c; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b8; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122572; asc      %r;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000007b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c0ad0; asc    m    ;;
 7: len 8; hex 99bac36de60c0c04; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007b9; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011225f7; asc      % ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000007b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c1155; asc    m   U;;
 7: len 8; hex 99bac36de60c127d; asc    m   };;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ba; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112267c; asc      &|;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000007ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c17a9; asc    m    ;;
 7: len 8; hex 99bac36de60c18cf; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bb; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122701; asc      ' ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000007bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c1dfc; asc    m    ;;
 7: len 8; hex 99bac36de60c1f1f; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bc; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122786; asc      ' ;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000007bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c24b3; asc    m  $ ;;
 7: len 8; hex 99bac36de60c289d; asc    m  ( ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bd; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112280b; asc      ( ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000007bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c2e43; asc    m  .C;;
 7: len 8; hex 99bac36de60c2f9b; asc    m  / ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007be; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122890; asc      ( ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000007be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c3535; asc    m  55;;
 7: len 8; hex 99bac36de60c36b8; asc    m  6 ;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007bf; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122915; asc      ) ;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000007bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c3bb9; asc    m  ; ;;
 7: len 8; hex 99bac36de60c3d15; asc    m  = ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c0; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112299a; asc      ) ;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000007c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c42db; asc    m  B ;;
 7: len 8; hex 99bac36de60c442f; asc    m  D/;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c1; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122a1f; asc      * ;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000007c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c49c5; asc    m  I ;;
 7: len 8; hex 99bac36de60c4b14; asc    m  K ;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c2; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122aa4; asc      * ;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000007c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c5091; asc    m  P ;;
 7: len 8; hex 99bac36de60c51d3; asc    m  Q ;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c3; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122b29; asc      +);;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000007c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c573e; asc    m  W>;;
 7: len 8; hex 99bac36de60c5874; asc    m  Xt;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c4; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122bae; asc      + ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000007c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c5de5; asc    m  ] ;;
 7: len 8; hex 99bac36de60c5f3d; asc    m  _=;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c5; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122c33; asc      ,3;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000007c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c64f7; asc    m  d ;;
 7: len 8; hex 99bac36de60c664c; asc    m  fL;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c6; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122cb8; asc      , ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000007c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c6bc4; asc    m  k ;;
 7: len 8; hex 99bac36de60c6d11; asc    m  m ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c7; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122d3d; asc      -=;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000007c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c7256; asc    m  rV;;
 7: len 8; hex 99bac36de60c7390; asc    m  s ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c8; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122dc2; asc      - ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000007c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c78d3; asc    m  x ;;
 7: len 8; hex 99bac36de60c79f6; asc    m  y ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007c9; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122e47; asc      .G;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000007c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c7f52; asc    m   R;;
 7: len 8; hex 99bac36de60c8080; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ca; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122ecc; asc      . ;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000007ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60c866d; asc    m   m;;
 7: len 8; hex 99bac36de60c9c1f; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cb; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122f51; asc      /Q;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000007cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60ca30b; asc    m    ;;
 7: len 8; hex 99bac36de60ca47f; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cc; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001122fd6; asc      / ;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000007cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60caa5c; asc    m   \;;
 7: len 8; hex 99bac36de60cabff; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cd; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 0200000112305b; asc      0[;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000007cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60cb257; asc    m   W;;
 7: len 8; hex 99bac36de60cb3f6; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007ce; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011230e0; asc      0 ;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000007ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60cb9f4; asc    m    ;;
 7: len 8; hex 99bac36de60cbb79; asc    m   y;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007cf; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 02000001123165; asc      1e;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000007cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60cc137; asc    m   7;;
 7: len 8; hex 99bac36de60cc293; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d0; asc         ;;
 1: len 6; hex 00000003f90d; asc       ;;
 2: len 7; hex 020000011231ea; asc      1 ;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000007d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60cc80c; asc    m    ;;
 7: len 8; hex 99bac36de60cdf46; asc    m   F;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d1; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a724d7; asc      $ ;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000007d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60d2bed; asc    m  + ;;
 7: len 8; hex 99bac36de60d40bc; asc    m  @ ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d2; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a7255a; asc      %Z;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000007d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60d82d7; asc    m    ;;
 7: len 8; hex 99bac36de60d91f9; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d3; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a725dd; asc      % ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000007d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60da05c; asc    m   \;;
 7: len 8; hex 99bac36de60da399; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d4; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a72660; asc      &`;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000007d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60dadac; asc    m    ;;
 7: len 8; hex 99bac36de60db044; asc    m   D;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d5; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a726e3; asc      & ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 80000000000007d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60db8be; asc    m    ;;
 7: len 8; hex 99bac36de60dba87; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d6; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a72766; asc      'f;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 80000000000007d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60dc145; asc    m   E;;
 7: len 8; hex 99bac36de60dc304; asc    m    ;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d7; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a727e9; asc      ' ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 80000000000007d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60dc8ee; asc    m    ;;
 7: len 8; hex 99bac36de60dcb66; asc    m   f;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000007d8; asc         ;;
 1: len 6; hex 00000003f941; asc      A;;
 2: len 7; hex 02000001a7286c; asc      (l;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 80000000000007d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36de60dd11a; asc    m    ;;
 7: len 8; hex 99bac36de60dd2a7; asc    m    ;;
 8: SQL NULL;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 141 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 260569 lock mode S locks rec but not gap waiting
Record lock, heap no 161 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 00000003f9f0; asc       ;;
 2: len 7; hex 02000001501ac0; asc     P  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d333236303033333030; asc tk-326003300;;
 5: len 8; hex 99bac36deb00e9c5; asc    m    ;;
 6: len 8; hex 99bac36deb00e9c5; asc    m    ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V5 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:58:11 135358810506816
*** (1) TRANSACTION:
TRANSACTION 277390, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9036, OS thread handle 135358138172992, query id 1248348 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 989

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 149 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 277390 lock_mode X locks rec but not gap
Record lock, heap no 224 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043b8e; asc     ; ;;
 2: len 7; hex 020000013212e2; asc     2  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343234363138303133; asc tk-424618013;;
 5: len 8; hex 99bac36e8b0218a5; asc    n    ;;
 6: len 8; hex 99bac36e8b0218a5; asc    n    ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 147 page no 12 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 277390 lock_mode X locks rec but not gap waiting
Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782040; asc     x @;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304bffd; asc    n    ;;
 7: len 8; hex 99bac36e8304c133; asc    n   3;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 277352, ACTIVE 2 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 64 lock struct(s), heap size 24696, 5478 row lock(s), undo log entries 1465
MySQL thread id 9000, OS thread handle 135358149797440, query id 1248542 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (989, 3989, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 147 page no 12 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 277352 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000370; asc        p;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772701; asc     w' ;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000370; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300e895; asc    n    ;;
 7: len 8; hex 99bac36e8300e9dc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000371; asc        q;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772786; asc     w' ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000371; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300edcb; asc    n    ;;
 7: len 8; hex 99bac36e8300ef25; asc    n   %;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000372; asc        r;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177280b; asc     w( ;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 8000000000000372; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300f30d; asc    n    ;;
 7: len 8; hex 99bac36e8300f449; asc    n   I;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000373; asc        s;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772890; asc     w( ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 8000000000000373; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300f854; asc    n   T;;
 7: len 8; hex 99bac36e8300f987; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000374; asc        t;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772915; asc     w) ;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 8000000000000374; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300fd56; asc    n   V;;
 7: len 8; hex 99bac36e8300fe84; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000375; asc        u;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177299a; asc     w) ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 8000000000000375; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830102ae; asc    n    ;;
 7: len 8; hex 99bac36e830106cc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000376; asc        v;;
 1: len 6; hex 000000043a8c; asc     : ;;
 2: len 7; hex 01000001991e9d; asc        ;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 8000000000000376; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83010b7a; asc    n   z;;
 7: len 8; hex 99bac36e83010cb7; asc    n    ;;
 8: len 8; hex 99bac36e8307faa5; asc    n    ;;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000377; asc        w;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772aa4; asc     w* ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 8000000000000377; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830110ad; asc    n    ;;
 7: len 8; hex 99bac36e830111e7; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000378; asc        x;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772b29; asc     w+);;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000378; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83011612; asc    n    ;;
 7: len 8; hex 99bac36e8301177a; asc    n   z;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000379; asc        y;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772bae; asc     w+ ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000379; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83011b99; asc    n    ;;
 7: len 8; hex 99bac36e83011cc9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037a; asc        z;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772c33; asc     w,3;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 800000000000037a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83012093; asc    n    ;;
 7: len 8; hex 99bac36e830121b4; asc    n  ! ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037b; asc        {;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772cb8; asc     w, ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 800000000000037b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830125be; asc    n  % ;;
 7: len 8; hex 99bac36e83012706; asc    n  ' ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037c; asc        |;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772d3d; asc     w-=;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 800000000000037c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83012b6f; asc    n  +o;;
 7: len 8; hex 99bac36e83012cd7; asc    n  , ;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037d; asc        };;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772dc2; asc     w- ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 800000000000037d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830130ad; asc    n  0 ;;
 7: len 8; hex 99bac36e830131f7; asc    n  1 ;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037e; asc        ~;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772e47; asc     w.G;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 800000000000037e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830135f9; asc    n  5 ;;
 7: len 8; hex 99bac36e83013730; asc    n  70;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772ecc; asc     w. ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 800000000000037f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83013b3d; asc    n  ;=;;
 7: len 8; hex 99bac36e83014204; asc    n  B ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000380; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772f51; asc     w/Q;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000380; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83015227; asc    n  R';;
 7: len 8; hex 99bac36e83015940; asc    n  Y@;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000381; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772fd6; asc     w/ ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000381; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830160bf; asc    n  ` ;;
 7: len 8; hex 99bac36e830165b7; asc    n  e ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000382; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177305b; asc     w0[;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 8000000000000382; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83016adf; asc    n  j ;;
 7: len 8; hex 99bac36e83016c51; asc    n  lQ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000383; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017730e0; asc     w0 ;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 8000000000000383; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830170e1; asc    n  p ;;
 7: len 8; hex 99bac36e8301722c; asc    n  r,;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000384; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773165; asc     w1e;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 8000000000000384; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301768b; asc    n  v ;;
 7: len 8; hex 99bac36e83017817; asc    n  x ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000385; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017731ea; asc     w1 ;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 8000000000000385; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83017cc6; asc    n  | ;;
 7: len 8; hex 99bac36e83017e46; asc    n  ~F;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000386; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177326f; asc     w2o;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 8000000000000386; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301824d; asc    n   M;;
 7: len 8; hex 99bac36e830183a6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000387; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017732f4; asc     w2 ;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 8000000000000387; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830187f1; asc    n    ;;
 7: len 8; hex 99bac36e83018959; asc    n   Y;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000388; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773379; asc     w3y;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000388; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301910a; asc    n    ;;
 7: len 8; hex 99bac36e83019254; asc    n   T;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000389; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017733fe; asc     w3 ;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000389; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83019692; asc    n    ;;
 7: len 8; hex 99bac36e83019958; asc    n   X;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038a; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773483; asc     w4 ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 800000000000038a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301a989; asc    n    ;;
 7: len 8; hex 99bac36e8301aaf0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038b; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773508; asc     w5 ;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 800000000000038b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301af18; asc    n    ;;
 7: len 8; hex 99bac36e8301b068; asc    n   h;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038c; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177358d; asc     w5 ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 800000000000038c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301b475; asc    n   u;;
 7: len 8; hex 99bac36e8301b5e2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038d; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773612; asc     w6 ;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 800000000000038d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301bf60; asc    n   `;;
 7: len 8; hex 99bac36e8301c0f6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038e; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773697; asc     w6 ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 800000000000038e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301c5ab; asc    n    ;;
 7: len 8; hex 99bac36e8301c6fc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177371c; asc     w7 ;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 800000000000038f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301cb4f; asc    n   O;;
 7: len 8; hex 99bac36e8301cc96; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000390; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017737a1; asc     w7 ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000390; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301d0c1; asc    n    ;;
 7: len 8; hex 99bac36e8301d201; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000391; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773826; asc     w8&;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000391; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301dcc2; asc    n    ;;
 7: len 8; hex 99bac36e8301e1be; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000392; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017738ab; asc     w8 ;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 8000000000000392; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301ebad; asc    n    ;;
 7: len 8; hex 99bac36e8301efdb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000393; asc         ;;
 1: len 6; hex 000000043a7c; asc     :|;;
 2: len 7; hex 01000001ba1295; asc        ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 8000000000000393; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301f56d; asc    n   m;;
 7: len 8; hex 99bac36e8301f77a; asc    n   z;;
 8: len 8; hex 99bac36e8305cc05; asc    n    ;;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000394; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017739b5; asc     w9 ;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 8000000000000394; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83020023; asc    n   #;;
 7: len 8; hex 99bac36e83020496; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000395; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773a3a; asc     w::;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 8000000000000395; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83021303; asc    n    ;;
 7: len 8; hex 99bac36e83021542; asc    n   B;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000396; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773abf; asc     w: ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 8000000000000396; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83021d95; asc    n    ;;
 7: len 8; hex 99bac36e830221be; asc    n  ! ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000397; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773b44; asc     w;D;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 8000000000000397; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830233c3; asc    n  3 ;;
 7: len 8; hex 99bac36e830235fb; asc    n  5 ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000398; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773bc9; asc     w; ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000398; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83023ef8; asc    n  > ;;
 7: len 8; hex 99bac36e83024324; asc    n  C$;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000399; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773c4e; asc     w<N;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000399; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83024f45; asc    n  OE;;
 7: len 8; hex 99bac36e830253ce; asc    n  S ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039a; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773cd3; asc     w< ;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 800000000000039a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83025dbc; asc    n  ] ;;
 7: len 8; hex 99bac36e8302627c; asc    n  b|;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039b; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773d58; asc     w=X;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 800000000000039b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83026b42; asc    n  kB;;
 7: len 8; hex 99bac36e830270fa; asc    n  p ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039c; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773ddd; asc     w= ;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 800000000000039c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83027997; asc    n  y ;;
 7: len 8; hex 99bac36e83028064; asc    n   d;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039d; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773e62; asc     w>b;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 800000000000039d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83028939; asc    n   9;;
 7: len 8; hex 99bac36e83028f2f; asc    n   /;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039e; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773ee7; asc     w> ;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 800000000000039e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830299d1; asc    n    ;;
 7: len 8; hex 99bac36e83029c1e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773f6c; asc     w?l;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 800000000000039f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302a319; asc    n    ;;
 7: len 8; hex 99bac36e8302a735; asc    n   5;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178008f; asc     x  ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 80000000000003a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302afd9; asc    n    ;;
 7: len 8; hex 99bac36e8302b3a5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780114; asc     x  ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 80000000000003a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302bcc3; asc    n    ;;
 7: len 8; hex 99bac36e8302c0ed; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780199; asc     x  ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 80000000000003a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302c96b; asc    n   k;;
 7: len 8; hex 99bac36e8302cd1f; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178021e; asc     x  ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 80000000000003a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302d6df; asc    n    ;;
 7: len 8; hex 99bac36e8302d8ba; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017802a3; asc     x  ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 80000000000003a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302e3d1; asc    n    ;;
 7: len 8; hex 99bac36e8302e807; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780328; asc     x (;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 80000000000003a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302f136; asc    n   6;;
 7: len 8; hex 99bac36e8302f5c3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a6; asc         ;;
 1: len 6; hex 000000043b4e; asc     ;N;;
 2: len 7; hex 01000001bb13d5; asc        ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 80000000000003a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302fe62; asc    n   b;;
 7: len 8; hex 99bac36e83030254; asc    n   T;;
 8: len 8; hex 99bac36e8803fd20; asc    n    ;;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780432; asc     x 2;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 80000000000003a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83030c0c; asc    n    ;;
 7: len 8; hex 99bac36e83030e86; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017804b7; asc     x  ;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 80000000000003a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830319cf; asc    n    ;;
 7: len 8; hex 99bac36e83031e6a; asc    n   j;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178053c; asc     x <;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 80000000000003a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830328fb; asc    n  ( ;;
 7: len 8; hex 99bac36e83032d97; asc    n  - ;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003aa; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017805c1; asc     x  ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 80000000000003aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830335dd; asc    n  5 ;;
 7: len 8; hex 99bac36e83033a1b; asc    n  : ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ab; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780646; asc     x F;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 80000000000003ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83034316; asc    n  C ;;
 7: len 8; hex 99bac36e83034646; asc    n  FF;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ac; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017806cb; asc     x  ;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 80000000000003ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83034fa9; asc    n  O ;;
 7: len 8; hex 99bac36e830352d8; asc    n  R ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ad; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780750; asc     x P;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 80000000000003ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83035cc5; asc    n  \ ;;
 7: len 8; hex 99bac36e8303614f; asc    n  aO;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ae; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017807d5; asc     x  ;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 80000000000003ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83036b3d; asc    n  k=;;
 7: len 8; hex 99bac36e83036fe9; asc    n  o ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003af; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178085a; asc     x Z;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 80000000000003af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83037707; asc    n  w ;;
 7: len 8; hex 99bac36e83037959; asc    n  yY;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017808df; asc     x  ;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 80000000000003b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83038131; asc    n   1;;
 7: len 8; hex 99bac36e830383f5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780964; asc     x d;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 80000000000003b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83038c87; asc    n    ;;
 7: len 8; hex 99bac36e83039039; asc    n   9;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017809e9; asc     x  ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 80000000000003b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303998c; asc    n    ;;
 7: len 8; hex 99bac36e83039cb3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780a6e; asc     x n;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 80000000000003b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303a24c; asc    n   L;;
 7: len 8; hex 99bac36e8303a41e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780af3; asc     x  ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 80000000000003b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303a9f9; asc    n    ;;
 7: len 8; hex 99bac36e8303abe1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780b78; asc     x x;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 80000000000003b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303b19a; asc    n    ;;
 7: len 8; hex 99bac36e8303b31e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780bfd; asc     x  ;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 80000000000003b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303b858; asc    n   X;;
 7: len 8; hex 99bac36e8303b9bf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780c82; asc     x  ;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 80000000000003b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303be70; asc    n   p;;
 7: len 8; hex 99bac36e8303bfd9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780d07; asc     x  ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000003b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303c43a; asc    n   :;;
 7: len 8; hex 99bac36e8303c68d; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780d8c; asc     x  ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000003b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303cbb9; asc    n    ;;
 7: len 8; hex 99bac36e8303cdbe; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ba; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780e11; asc     x  ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000003ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303d60e; asc    n    ;;
 7: len 8; hex 99bac36e8303d8d1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bb; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780e96; asc     x  ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000003bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303e2cf; asc    n    ;;
 7: len 8; hex 99bac36e8303e6bf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780f1b; asc     x  ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000003bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303ef61; asc    n   a;;
 7: len 8; hex 99bac36e8303f7c3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780fa0; asc     x  ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000003bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303ff49; asc    n   I;;
 7: len 8; hex 99bac36e83040286; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003be; asc         ;;
 1: len 6; hex 000000043a7f; asc     : ;;
 2: len 7; hex 02000001a11817; asc        ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000003be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83040988; asc    n    ;;
 7: len 8; hex 99bac36e83040b26; asc    n   &;;
 8: len 8; hex 99bac36e8305cd57; asc    n   W;;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bf; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017810aa; asc     x  ;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000003bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83040fa2; asc    n    ;;
 7: len 8; hex 99bac36e830410de; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c0; asc         ;;
 1: len 6; hex 000000043a7e; asc     :~;;
 2: len 7; hex 01000000df29b6; asc      ) ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000003c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830415fc; asc    n    ;;
 7: len 8; hex 99bac36e830417a6; asc    n    ;;
 8: len 8; hex 99bac36e8305ccb0; asc    n    ;;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017811b4; asc     x  ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000003c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83041c19; asc    n    ;;
 7: len 8; hex 99bac36e83041db0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781239; asc     x 9;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000003c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83042218; asc    n  " ;;
 7: len 8; hex 99bac36e83042362; asc    n  #b;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017812be; asc     x  ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000003c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830427f8; asc    n  ' ;;
 7: len 8; hex 99bac36e8304294f; asc    n  )O;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781343; asc     x C;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000003c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83042daa; asc    n  - ;;
 7: len 8; hex 99bac36e83042f1a; asc    n  / ;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017813c8; asc     x  ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000003c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043369; asc    n  3i;;
 7: len 8; hex 99bac36e830434b2; asc    n  4 ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178144d; asc     x M;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000003c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043912; asc    n  9 ;;
 7: len 8; hex 99bac36e83043a57; asc    n  :W;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017814d2; asc     x  ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000003c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043efb; asc    n  > ;;
 7: len 8; hex 99bac36e8304404f; asc    n  @O;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781557; asc     x W;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000003c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304442b; asc    n  D+;;
 7: len 8; hex 99bac36e830445b3; asc    n  E ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017815dc; asc     x  ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000003c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83044a3a; asc    n  J:;;
 7: len 8; hex 99bac36e83044b90; asc    n  K ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ca; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781661; asc     x a;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000003ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045041; asc    n  PA;;
 7: len 8; hex 99bac36e83045199; asc    n  Q ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cb; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017816e6; asc     x  ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000003cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045621; asc    n  V!;;
 7: len 8; hex 99bac36e83045795; asc    n  W ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178176b; asc     x k;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000003cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045bf8; asc    n  [ ;;
 7: len 8; hex 99bac36e83045d47; asc    n  ]G;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017817f0; asc     x  ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000003cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046162; asc    n  ab;;
 7: len 8; hex 99bac36e830462ac; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ce; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781875; asc     x u;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000003ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046701; asc    n  g ;;
 7: len 8; hex 99bac36e8304683b; asc    n  h;;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cf; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017818fa; asc     x  ;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000003cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046c32; asc    n  l2;;
 7: len 8; hex 99bac36e83046d8e; asc    n  m ;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178197f; asc     x  ;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000003d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047257; asc    n  rW;;
 7: len 8; hex 99bac36e830473b9; asc    n  s ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781a04; asc     x  ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000003d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047878; asc    n  xx;;
 7: len 8; hex 99bac36e83047a1f; asc    n  z ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781a89; asc     x  ;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000003d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047ed8; asc    n  ~ ;;
 7: len 8; hex 99bac36e8304803a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781b0e; asc     x  ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000003d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304850b; asc    n    ;;
 7: len 8; hex 99bac36e83048689; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781b93; asc     x  ;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000003d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83048aff; asc    n    ;;
 7: len 8; hex 99bac36e83048d1a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781c18; asc     x  ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000003d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830491b5; asc    n    ;;
 7: len 8; hex 99bac36e83049308; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781c9d; asc     x  ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000003d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830497e9; asc    n    ;;
 7: len 8; hex 99bac36e8304996e; asc    n   n;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781d22; asc     x ";;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000003d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83049de0; asc    n    ;;
 7: len 8; hex 99bac36e83049f2b; asc    n   +;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781da7; asc     x  ;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000003d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304a364; asc    n   d;;
 7: len 8; hex 99bac36e8304a50b; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781e2c; asc     x ,;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000003d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304a9a5; asc    n    ;;
 7: len 8; hex 99bac36e8304aaea; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003da; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781eb1; asc     x  ;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000003da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304af49; asc    n   I;;
 7: len 8; hex 99bac36e8304b0b3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003db; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781f36; asc     x 6;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000003db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304b537; asc    n   7;;
 7: len 8; hex 99bac36e8304b68a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781fbb; asc     x  ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000003dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304babf; asc    n    ;;
 7: len 8; hex 99bac36e8304bc0e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782040; asc     x @;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304bffd; asc    n    ;;
 7: len 8; hex 99bac36e8304c133; asc    n   3;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003de; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017820c5; asc     x  ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000003de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304c59d; asc    n    ;;
 7: len 8; hex 99bac36e8304e236; asc    n   6;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003df; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178214a; asc     x!J;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000003df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304e6f3; asc    n    ;;
 7: len 8; hex 99bac36e8304e87c; asc    n   |;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017821cf; asc     x! ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000003e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304ecfe; asc    n    ;;
 7: len 8; hex 99bac36e8304ee62; asc    n   b;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782254; asc     x"T;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000003e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304f307; asc    n    ;;
 7: len 8; hex 99bac36e8304f46b; asc    n   k;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017822d9; asc     x" ;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000003e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304f86f; asc    n   o;;
 7: len 8; hex 99bac36e8304fa19; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178235e; asc     x#^;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000003e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304fea6; asc    n    ;;
 7: len 8; hex 99bac36e8305000c; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017823e3; asc     x# ;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000003e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830504d8; asc    n    ;;
 7: len 8; hex 99bac36e83050674; asc    n   t;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782468; asc     x$h;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000003e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83050b64; asc    n   d;;
 7: len 8; hex 99bac36e83050ceb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017824ed; asc     x$ ;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000003e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305118d; asc    n    ;;
 7: len 8; hex 99bac36e8305132f; asc    n   /;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782572; asc     x%r;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000003e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830517d9; asc    n    ;;
 7: len 8; hex 99bac36e8305193d; asc    n   =;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017825f7; asc     x% ;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000003e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83051de3; asc    n    ;;
 7: len 8; hex 99bac36e83051f71; asc    n   q;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e9; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001060efd; asc        ;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000003e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830575e2; asc    n  u ;;
 7: len 8; hex 99bac36e830577c9; asc    n  w ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ea; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001060f80; asc        ;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000003ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305b490; asc    n    ;;
 7: len 8; hex 99bac36e8305b65e; asc    n   ^;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003eb; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061003; asc        ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000003eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305bb64; asc    n   d;;
 7: len 8; hex 99bac36e8305bd20; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ec; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061086; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000003ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305c513; asc    n    ;;
 7: len 8; hex 99bac36e8305ca83; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ed; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061109; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 80000000000003ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305cee5; asc    n    ;;
 7: len 8; hex 99bac36e8305d0cd; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ee; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106118c; asc        ;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 80000000000003ee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305d591; asc    n    ;;
 7: len 8; hex 99bac36e8305d71a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ef; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106120f; asc        ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 80000000000003ef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305db51; asc    n   Q;;
 7: len 8; hex 99bac36e8305dcdf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f0; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061292; asc        ;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 80000000000003f0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305e118; asc    n    ;;
 7: len 8; hex 99bac36e8305e275; asc    n   u;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f1; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061315; asc        ;;
 3: len 8; hex 8000000000000009; asc         ;;
 4: len 8; hex 80000000000003f1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305e746; asc    n   F;;
 7: len 8; hex 99bac36e8305e8a8; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f2; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061398; asc        ;;
 3: len 8; hex 800000000000000a; asc         ;;
 4: len 8; hex 80000000000003f2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305ed25; asc    n   %;;
 7: len 8; hex 99bac36e8305eea6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f3; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106141b; asc        ;;
 3: len 8; hex 800000000000000b; asc         ;;
 4: len 8; hex 80000000000003f3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305f2ad; asc    n    ;;
 7: len 8; hex 99bac36e8305f4a5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f4; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106149e; asc        ;;
 3: len 8; hex 800000000000000c; asc         ;;
 4: len 8; hex 80000000000003f4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305fa61; asc    n   a;;
 7: len 8; hex 99bac36e8305fc16; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f5; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061521; asc       !;;
 3: len 8; hex 800000000000000d; asc         ;;
 4: len 8; hex 80000000000003f5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830600c3; asc    n    ;;
 7: len 8; hex 99bac36e830601f5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f6; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010615a4; asc        ;;
 3: len 8; hex 800000000000000e; asc         ;;
 4: len 8; hex 80000000000003f6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83060626; asc    n   &;;
 7: len 8; hex 99bac36e83060788; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f7; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061627; asc       ';;
 3: len 8; hex 800000000000000f; asc         ;;
 4: len 8; hex 80000000000003f7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83060b55; asc    n   U;;
 7: len 8; hex 99bac36e83060c9c; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f8; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010616aa; asc        ;;
 3: len 8; hex 8000000000000010; asc         ;;
 4: len 8; hex 80000000000003f8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830610bc; asc    n    ;;
 7: len 8; hex 99bac36e830611f2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f9; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106172d; asc       -;;
 3: len 8; hex 8000000000000011; asc         ;;
 4: len 8; hex 80000000000003f9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83061633; asc    n   3;;
 7: len 8; hex 99bac36e8306176e; asc    n   n;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fa; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010617b0; asc        ;;
 3: len 8; hex 8000000000000012; asc         ;;
 4: len 8; hex 80000000000003fa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83061b9e; asc    n    ;;
 7: len 8; hex 99bac36e83061cda; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fb; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061833; asc       3;;
 3: len 8; hex 8000000000000013; asc         ;;
 4: len 8; hex 80000000000003fb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306211a; asc    n  ! ;;
 7: len 8; hex 99bac36e8306225f; asc    n  "_;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fc; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010618b6; asc        ;;
 3: len 8; hex 8000000000000014; asc         ;;
 4: len 8; hex 80000000000003fc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306266c; asc    n  &l;;
 7: len 8; hex 99bac36e830627ac; asc    n  ' ;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fd; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061939; asc       9;;
 3: len 8; hex 8000000000000015; asc         ;;
 4: len 8; hex 80000000000003fd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83062d3c; asc    n  -<;;
 7: len 8; hex 99bac36e83062e92; asc    n  . ;;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fe; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010619bc; asc        ;;
 3: len 8; hex 8000000000000016; asc         ;;
 4: len 8; hex 80000000000003fe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830632f4; asc    n  2 ;;
 7: len 8; hex 99bac36e8306346b; asc    n  4k;;
 8: SQL NULL;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ff; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061a3f; asc       ?;;
 3: len 8; hex 8000000000000017; asc         ;;
 4: len 8; hex 80000000000003ff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830638a4; asc    n  8 ;;
 7: len 8; hex 99bac36e830639dc; asc    n  9 ;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000400; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061ac2; asc        ;;
 3: len 8; hex 8000000000000018; asc         ;;
 4: len 8; hex 8000000000000400; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83063dfc; asc    n  = ;;
 7: len 8; hex 99bac36e83063f58; asc    n  ?X;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000401; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061b45; asc       E;;
 3: len 8; hex 8000000000000019; asc         ;;
 4: len 8; hex 8000000000000401; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306431c; asc    n  C ;;
 7: len 8; hex 99bac36e8306447e; asc    n  D~;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000402; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061bc8; asc        ;;
 3: len 8; hex 800000000000001a; asc         ;;
 4: len 8; hex 8000000000000402; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306484e; asc    n  HN;;
 7: len 8; hex 99bac36e83064972; asc    n  Ir;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000403; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061c4b; asc       K;;
 3: len 8; hex 800000000000001b; asc         ;;
 4: len 8; hex 8000000000000403; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83064d40; asc    n  M@;;
 7: len 8; hex 99bac36e83064e74; asc    n  Nt;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000404; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061cce; asc        ;;
 3: len 8; hex 800000000000001c; asc         ;;
 4: len 8; hex 8000000000000404; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83065211; asc    n  R ;;
 7: len 8; hex 99bac36e83065364; asc    n  Sd;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000405; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061d51; asc       Q;;
 3: len 8; hex 800000000000001d; asc         ;;
 4: len 8; hex 8000000000000405; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306577e; asc    n  W~;;
 7: len 8; hex 99bac36e830658ee; asc    n  X ;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000406; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061dd4; asc        ;;
 3: len 8; hex 800000000000001e; asc         ;;
 4: len 8; hex 8000000000000406; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83065d23; asc    n  ]#;;
 7: len 8; hex 99bac36e83065e75; asc    n  ^u;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000407; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061e57; asc       W;;
 3: len 8; hex 800000000000001f; asc         ;;
 4: len 8; hex 8000000000000407; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83066292; asc    n  b ;;
 7: len 8; hex 99bac36e830663d7; asc    n  c ;;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000408; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061eda; asc        ;;
 3: len 8; hex 8000000000000020; asc         ;;
 4: len 8; hex 8000000000000408; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830667ea; asc    n  g ;;
 7: len 8; hex 99bac36e8306693d; asc    n  i=;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000409; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061f5d; asc       ];;
 3: len 8; hex 8000000000000021; asc        !;;
 4: len 8; hex 8000000000000409; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83066dec; asc    n  m ;;
 7: len 8; hex 99bac36e83066f42; asc    n  oB;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040a; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061fe0; asc        ;;
 3: len 8; hex 8000000000000022; asc        ";;
 4: len 8; hex 800000000000040a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83067323; asc    n  s#;;
 7: len 8; hex 99bac36e83067490; asc    n  t ;;
 8: SQL NULL;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040b; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062063; asc       c;;
 3: len 8; hex 8000000000000023; asc        #;;
 4: len 8; hex 800000000000040b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830678a6; asc    n  x ;;
 7: len 8; hex 99bac36e83067a28; asc    n  z(;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040c; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010620e6; asc        ;;
 3: len 8; hex 8000000000000024; asc        $;;
 4: len 8; hex 800000000000040c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83067eb7; asc    n  ~ ;;
 7: len 8; hex 99bac36e8306800e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040d; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062169; asc      !i;;
 3: len 8; hex 8000000000000025; asc        %;;
 4: len 8; hex 800000000000040d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830685ba; asc    n    ;;
 7: len 8; hex 99bac36e8306875c; asc    n   \;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040e; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010621ec; asc      ! ;;
 3: len 8; hex 8000000000000026; asc        &;;
 4: len 8; hex 800000000000040e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83068c0a; asc    n    ;;
 7: len 8; hex 99bac36e83068d88; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000410; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010622f2; asc      " ;;
 3: len 8; hex 8000000000000028; asc        (;;
 4: len 8; hex 8000000000000410; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830697e0; asc    n    ;;
 7: len 8; hex 99bac36e8306994f; asc    n   O;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000411; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062375; asc      #u;;
 3: len 8; hex 8000000000000029; asc        );;
 4: len 8; hex 8000000000000411; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83069d9e; asc    n    ;;
 7: len 8; hex 99bac36e83069efe; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000412; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010623f8; asc      # ;;
 3: len 8; hex 800000000000002a; asc        *;;
 4: len 8; hex 8000000000000412; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306a2d3; asc    n    ;;
 7: len 8; hex 99bac36e8306a43f; asc    n   ?;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000413; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106247b; asc      ${;;
 3: len 8; hex 800000000000002b; asc        +;;
 4: len 8; hex 8000000000000413; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306a90f; asc    n    ;;
 7: len 8; hex 99bac36e8306aa6a; asc    n   j;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000414; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010624ff; asc      $ ;;
 3: len 8; hex 800000000000002c; asc        ,;;
 4: len 8; hex 8000000000000414; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306ae66; asc    n   f;;
 7: len 8; hex 99bac36e8306afbb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000415; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062584; asc      % ;;
 3: len 8; hex 800000000000002d; asc        -;;
 4: len 8; hex 8000000000000415; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306b38f; asc    n    ;;
 7: len 8; hex 99bac36e8306b4b9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000416; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062609; asc      & ;;
 3: len 8; hex 800000000000002e; asc        .;;
 4: len 8; hex 8000000000000416; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306b860; asc    n   `;;
 7: len 8; hex 99bac36e8306ee50; asc    n   P;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000417; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106268e; asc      & ;;
 3: len 8; hex 800000000000002f; asc        /;;
 4: len 8; hex 8000000000000417; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306f312; asc    n    ;;
 7: len 8; hex 99bac36e8306f466; asc    n   f;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000418; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062713; asc      ' ;;
 3: len 8; hex 8000000000000030; asc        0;;
 4: len 8; hex 8000000000000418; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306f8bc; asc    n    ;;
 7: len 8; hex 99bac36e8306fa16; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000419; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062798; asc      ' ;;
 3: len 8; hex 8000000000000031; asc        1;;
 4: len 8; hex 8000000000000419; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306fe28; asc    n   (;;
 7: len 8; hex 99bac36e8306ffa7; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041a; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106281d; asc      ( ;;
 3: len 8; hex 8000000000000032; asc        2;;
 4: len 8; hex 800000000000041a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830703e7; asc    n    ;;
 7: len 8; hex 99bac36e83070514; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041b; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010628a2; asc      ( ;;
 3: len 8; hex 8000000000000033; asc        3;;
 4: len 8; hex 800000000000041b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307094d; asc    n   M;;
 7: len 8; hex 99bac36e83074084; asc    n  @ ;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041c; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062927; asc      )';;
 3: len 8; hex 8000000000000034; asc        4;;
 4: len 8; hex 800000000000041c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830746c9; asc    n  F ;;
 7: len 8; hex 99bac36e830748dc; asc    n  H ;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041d; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010629ac; asc      ) ;;
 3: len 8; hex 8000000000000035; asc        5;;
 4: len 8; hex 800000000000041d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83074e5b; asc    n  N[;;
 7: len 8; hex 99bac36e83075019; asc    n  P ;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041e; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062a31; asc      *1;;
 3: len 8; hex 8000000000000036; asc        6;;
 4: len 8; hex 800000000000041e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830754c0; asc    n  T ;;
 7: len 8; hex 99bac36e8307565b; asc    n  V[;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041f; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062ab6; asc      * ;;
 3: len 8; hex 8000000000000037; asc        7;;
 4: len 8; hex 800000000000041f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83075c69; asc    n  \i;;
 7: len 8; hex 99bac36e83076184; asc    n  a ;;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000420; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062b3b; asc      +;;;
 3: len 8; hex 8000000000000038; asc        8;;
 4: len 8; hex 8000000000000420; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830766cd; asc    n  f ;;
 7: len 8; hex 99bac36e83076857; asc    n  hW;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000421; asc        !;;
 1: len 6; hex 000000043aba; asc     : ;;
 2: len 7; hex 02000001711eb0; asc     q  ;;
 3: len 8; hex 8000000000000039; asc        9;;
 4: len 8; hex 8000000000000421; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83076d0a; asc    n  m ;;
 7: len 8; hex 99bac36e830770d4; asc    n  p ;;
 8: len 8; hex 99bac36e840bad8a; asc    n    ;;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000422; asc        ";;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062c45; asc      ,E;;
 3: len 8; hex 800000000000003a; asc        :;;
 4: len 8; hex 8000000000000422; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830775d4; asc    n  u ;;
 7: len 8; hex 99bac36e83077742; asc    n  wB;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000423; asc        #;;
 1: len 6; hex 000000043ab2; asc     : ;;
 2: len 7; hex 02000001170da3; asc        ;;
 3: len 8; hex 800000000000003b; asc        ;;;
 4: len 8; hex 8000000000000423; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83077bae; asc    n  { ;;
 7: len 8; hex 99bac36e83077eda; asc    n  ~ ;;
 8: len 8; hex 99bac36e8407065e; asc    n   ^;;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000424; asc        $;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062d4f; asc      -O;;
 3: len 8; hex 800000000000003c; asc        <;;
 4: len 8; hex 8000000000000424; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83078374; asc    n   t;;
 7: len 8; hex 99bac36e83078513; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000425; asc        %;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062dd4; asc      - ;;
 3: len 8; hex 800000000000003d; asc        =;;
 4: len 8; hex 8000000000000425; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83078a7d; asc    n   };;
 7: len 8; hex 99bac36e83078db4; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000426; asc        &;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062e59; asc      .Y;;
 3: len 8; hex 800000000000003e; asc        >;;
 4: len 8; hex 8000000000000426; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83079258; asc    n   X;;
 7: len 8; hex 99bac36e8307cb4b; asc    n   K;;
 8: SQL NULL;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000427; asc        ';;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062ede; asc      . ;;
 3: len 8; hex 800000000000003f; asc        ?;;
 4: len 8; hex 8000000000000427; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307d0a7; asc    n    ;;
 7: len 8; hex 99bac36e8307d2f3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000428; asc        (;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062f63; asc      /c;;
 3: len 8; hex 8000000000000040; asc        @;;
 4: len 8; hex 8000000000000428; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307d7e9; asc    n    ;;
 7: len 8; hex 99bac36e8307d994; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000429; asc        );;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062fe8; asc      / ;;
 3: len 8; hex 8000000000000041; asc        A;;
 4: len 8; hex 8000000000000429; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307de3a; asc    n   :;;
 7: len 8; hex 99bac36e8307dfed; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042a; asc        *;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106306d; asc      0m;;
 3: len 8; hex 8000000000000042; asc        B;;
 4: len 8; hex 800000000000042a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307e4dd; asc    n    ;;
 7: len 8; hex 99bac36e8307e688; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042b; asc        +;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010630f2; asc      0 ;;
 3: len 8; hex 8000000000000043; asc        C;;
 4: len 8; hex 800000000000042b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307ebda; asc    n    ;;
 7: len 8; hex 99bac36e8307ed5f; asc    n   _;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042c; asc        ,;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063177; asc      1w;;
 3: len 8; hex 8000000000000044; asc        D;;
 4: len 8; hex 800000000000042c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307f220; asc    n    ;;
 7: len 8; hex 99bac36e8307f3aa; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042d; asc        -;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010631fc; asc      1 ;;
 3: len 8; hex 8000000000000045; asc        E;;
 4: len 8; hex 800000000000042d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307f872; asc    n   r;;
 7: len 8; hex 99bac36e8307fa3a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042e; asc        .;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063281; asc      2 ;;
 3: len 8; hex 8000000000000046; asc        F;;
 4: len 8; hex 800000000000042e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307ff10; asc    n    ;;
 7: len 8; hex 99bac36e830800a0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042f; asc        /;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063306; asc      3 ;;
 3: len 8; hex 8000000000000047; asc        G;;
 4: len 8; hex 800000000000042f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830805d2; asc    n    ;;
 7: len 8; hex 99bac36e83080780; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000430; asc        0;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106338b; asc      3 ;;
 3: len 8; hex 8000000000000048; asc        H;;
 4: len 8; hex 8000000000000430; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83080eac; asc    n    ;;
 7: len 8; hex 99bac36e830810bb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000431; asc        1;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063410; asc      4 ;;
 3: len 8; hex 8000000000000049; asc        I;;
 4: len 8; hex 8000000000000431; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308159f; asc    n    ;;
 7: len 8; hex 99bac36e83081708; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000432; asc        2;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063495; asc      4 ;;
 3: len 8; hex 800000000000004a; asc        J;;
 4: len 8; hex 8000000000000432; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83081bf5; asc    n    ;;
 7: len 8; hex 99bac36e83081da0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000433; asc        3;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106351a; asc      5 ;;
 3: len 8; hex 800000000000004b; asc        K;;
 4: len 8; hex 8000000000000433; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830824b9; asc    n  $ ;;
 7: len 8; hex 99bac36e830825fc; asc    n  % ;;
 8: SQL NULL;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000434; asc        4;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106359f; asc      5 ;;
 3: len 8; hex 800000000000004c; asc        L;;
 4: len 8; hex 8000000000000434; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83082b07; asc    n  + ;;
 7: len 8; hex 99bac36e83082d33; asc    n  -3;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000435; asc        5;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063624; asc      6$;;
 3: len 8; hex 800000000000004d; asc        M;;
 4: len 8; hex 8000000000000435; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83083265; asc    n  2e;;
 7: len 8; hex 99bac36e83083412; asc    n  4 ;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000436; asc        6;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010636a9; asc      6 ;;
 3: len 8; hex 800000000000004e; asc        N;;
 4: len 8; hex 8000000000000436; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830838c7; asc    n  8 ;;
 7: len 8; hex 99bac36e83083a64; asc    n  :d;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000437; asc        7;;
 1: len 6; hex 000000043ab4; asc     : ;;
 2: len 7; hex 02000001612881; asc     a( ;;
 3: len 8; hex 800000000000004f; asc        O;;
 4: len 8; hex 8000000000000437; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83083f96; asc    n  ? ;;
 7: len 8; hex 99bac36e8308414d; asc    n  AM;;
 8: len 8; hex 99bac36e840707dd; asc    n    ;;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000438; asc        8;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010637b3; asc      7 ;;
 3: len 8; hex 8000000000000050; asc        P;;
 4: len 8; hex 8000000000000438; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83084616; asc    n  F ;;
 7: len 8; hex 99bac36e830847a3; asc    n  G ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000439; asc        9;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063838; asc      88;;
 3: len 8; hex 8000000000000051; asc        Q;;
 4: len 8; hex 8000000000000439; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83084dee; asc    n  M ;;
 7: len 8; hex 99bac36e83084fa7; asc    n  O ;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043a; asc        :;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010638bd; asc      8 ;;
 3: len 8; hex 8000000000000052; asc        R;;
 4: len 8; hex 800000000000043a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83085442; asc    n  TB;;
 7: len 8; hex 99bac36e830855c1; asc    n  U ;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043b; asc        ;;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063942; asc      9B;;
 3: len 8; hex 8000000000000053; asc        S;;
 4: len 8; hex 800000000000043b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83085a92; asc    n  Z ;;
 7: len 8; hex 99bac36e83085c31; asc    n  \1;;
 8: SQL NULL;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043c; asc        <;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010639c7; asc      9 ;;
 3: len 8; hex 8000000000000054; asc        T;;
 4: len 8; hex 800000000000043c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83086129; asc    n  a);;
 7: len 8; hex 99bac36e830862db; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043d; asc        =;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063a4c; asc      :L;;
 3: len 8; hex 8000000000000055; asc        U;;
 4: len 8; hex 800000000000043d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830867bc; asc    n  g ;;
 7: len 8; hex 99bac36e8308695c; asc    n  i\;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043e; asc        >;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ad1; asc      : ;;
 3: len 8; hex 8000000000000056; asc        V;;
 4: len 8; hex 800000000000043e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83086dff; asc    n  m ;;
 7: len 8; hex 99bac36e83086f98; asc    n  o ;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043f; asc        ?;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063b56; asc      ;V;;
 3: len 8; hex 8000000000000057; asc        W;;
 4: len 8; hex 800000000000043f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83087446; asc    n  tF;;
 7: len 8; hex 99bac36e830875a4; asc    n  u ;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000440; asc        @;;
 1: len 6; hex 000000043ad7; asc     : ;;
 2: len 7; hex 0200000143040d; asc     C  ;;
 3: len 8; hex 8000000000000058; asc        X;;
 4: len 8; hex 8000000000000440; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830879ef; asc    n  y ;;
 7: len 8; hex 99bac36e83087b85; asc    n  { ;;
 8: len 8; hex 99bac36e8509012b; asc    n   +;;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000441; asc        A;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063c60; asc      <`;;
 3: len 8; hex 8000000000000059; asc        Y;;
 4: len 8; hex 8000000000000441; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83087ffe; asc    n    ;;
 7: len 8; hex 99bac36e8308813a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000442; asc        B;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ce5; asc      < ;;
 3: len 8; hex 800000000000005a; asc        Z;;
 4: len 8; hex 8000000000000442; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83088591; asc    n    ;;
 7: len 8; hex 99bac36e83088748; asc    n   H;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000443; asc        C;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063d6a; asc      =j;;
 3: len 8; hex 800000000000005b; asc        [;;
 4: len 8; hex 8000000000000443; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83088bbf; asc    n    ;;
 7: len 8; hex 99bac36e83088d1a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000444; asc        D;;
 1: len 6; hex 000000043ab5; asc     : ;;
 2: len 7; hex 01000000f52888; asc      ( ;;
 3: len 8; hex 800000000000005c; asc        \;;
 4: len 8; hex 8000000000000444; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830891a1; asc    n    ;;
 7: len 8; hex 99bac36e83089307; asc    n    ;;
 8: len 8; hex 99bac36e840708bc; asc    n    ;;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000445; asc        E;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063e74; asc      >t;;
 3: len 8; hex 800000000000005d; asc        ];;
 4: len 8; hex 8000000000000445; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308975b; asc    n   [;;
 7: len 8; hex 99bac36e830898bc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000446; asc        F;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ef9; asc      > ;;
 3: len 8; hex 800000000000005e; asc        ^;;
 4: len 8; hex 8000000000000446; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83089d69; asc    n   i;;
 7: len 8; hex 99bac36e83089edc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000447; asc        G;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063f7e; asc      ?~;;
 3: len 8; hex 800000000000005f; asc        _;;
 4: len 8; hex 8000000000000447; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308a36e; asc    n   n;;
 7: len 8; hex 99bac36e8308a4b6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000448; asc        H;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b008f; asc     {  ;;
 3: len 8; hex 8000000000000060; asc        `;;
 4: len 8; hex 8000000000000448; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308a8d5; asc    n    ;;
 7: len 8; hex 99bac36e8308aa13; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000449; asc        I;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0114; asc     {  ;;
 3: len 8; hex 8000000000000061; asc        a;;
 4: len 8; hex 8000000000000449; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308aea6; asc    n    ;;
 7: len 8; hex 99bac36e8308aff2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044a; asc        J;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0199; asc     {  ;;
 3: len 8; hex 8000000000000062; asc        b;;
 4: len 8; hex 800000000000044a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308b47a; asc    n   z;;
 7: len 8; hex 99bac36e8308b5e5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044b; asc        K;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b021e; asc     {  ;;
 3: len 8; hex 8000000000000063; asc        c;;
 4: len 8; hex 800000000000044b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308ba1a; asc    n    ;;
 7: len 8; hex 99bac36e8308bb86; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044c; asc        L;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b02a3; asc     {  ;;
 3: len 8; hex 8000000000000064; asc        d;;
 4: len 8; hex 800000000000044c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308c066; asc    n   f;;
 7: len 8; hex 99bac36e8308c234; asc    n   4;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044d; asc        M;;
 1: len 6; hex 000000043b66; asc     ;f;;
 2: len 7; hex 02000001472f50; asc     G/P;;
 3: len 8; hex 8000000000000065; asc        e;;
 4: len 8; hex 800000000000044d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308c722; asc    n   ";;
 7: len 8; hex 99bac36e8308c88f; asc    n    ;;
 8: len 8; hex 99bac36e890dba5d; asc    n   ];;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044e; asc        N;;
 1: len 6; hex 000000043b64; asc     ;d;;
 2: len 7; hex 01000001b816b2; asc        ;;
 3: len 8; hex 8000000000000066; asc        f;;
 4: len 8; hex 800000000000044e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308cda2; asc    n    ;;
 7: len 8; hex 99bac36e8308cf2b; asc    n   +;;
 8: len 8; hex 99bac36e890db857; asc    n   W;;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044f; asc        O;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0432; asc     { 2;;
 3: len 8; hex 8000000000000067; asc        g;;
 4: len 8; hex 800000000000044f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308d3e0; asc    n    ;;
 7: len 8; hex 99bac36e8308d553; asc    n   S;;
 8: SQL NULL;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000450; asc        P;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b04b7; asc     {  ;;
 3: len 8; hex 8000000000000068; asc        h;;
 4: len 8; hex 8000000000000450; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308d9c3; asc    n    ;;
 7: len 8; hex 99bac36e8308db51; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000451; asc        Q;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b053c; asc     { <;;
 3: len 8; hex 8000000000000069; asc        i;;
 4: len 8; hex 8000000000000451; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308e02a; asc    n   *;;
 7: len 8; hex 99bac36e8308e19f; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000452; asc        R;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b05c1; asc     {  ;;
 3: len 8; hex 800000000000006a; asc        j;;
 4: len 8; hex 8000000000000452; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308e61d; asc    n    ;;
 7: len 8; hex 99bac36e8308e773; asc    n   s;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000453; asc        S;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0646; asc     { F;;
 3: len 8; hex 800000000000006b; asc        k;;
 4: len 8; hex 8000000000000453; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308ebb6; asc    n    ;;
 7: len 8; hex 99bac36e8308ed2b; asc    n   +;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000454; asc        T;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b06cb; asc     {  ;;
 3: len 8; hex 800000000000006c; asc        l;;
 4: len 8; hex 8000000000000454; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308f1b6; asc    n    ;;
 7: len 8; hex 99bac36e8308f325; asc    n   %;;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000455; asc        U;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0750; asc     { P;;
 3: len 8; hex 800000000000006d; asc        m;;
 4: len 8; hex 8000000000000455; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308f77f; asc    n    ;;
 7: len 8; hex 99bac36e8308f8da; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000456; asc        V;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b07d5; asc     {  ;;
 3: len 8; hex 800000000000006e; asc        n;;
 4: len 8; hex 8000000000000456; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308fd5f; asc    n   _;;
 7: len 8; hex 99bac36e8308ff2d; asc    n   -;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000457; asc        W;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b085a; asc     { Z;;
 3: len 8; hex 800000000000006f; asc        o;;
 4: len 8; hex 8000000000000457; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83090446; asc    n   F;;
 7: len 8; hex 99bac36e83090651; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000458; asc        X;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b08df; asc     {  ;;
 3: len 8; hex 8000000000000070; asc        p;;
 4: len 8; hex 8000000000000458; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83090b27; asc    n   ';;
 7: len 8; hex 99bac36e83090c7e; asc    n   ~;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000459; asc        Y;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0964; asc     { d;;
 3: len 8; hex 8000000000000071; asc        q;;
 4: len 8; hex 8000000000000459; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830910e2; asc    n    ;;
 7: len 8; hex 99bac36e83091255; asc    n   U;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045a; asc        Z;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b09e9; asc     {  ;;
 3: len 8; hex 8000000000000072; asc        r;;
 4: len 8; hex 800000000000045a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830916a4; asc    n    ;;
 7: len 8; hex 99bac36e83091801; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045b; asc        [;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0a6e; asc     { n;;
 3: len 8; hex 8000000000000073; asc        s;;
 4: len 8; hex 800000000000045b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83091c68; asc    n   h;;
 7: len 8; hex 99bac36e83091db8; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045c; asc        \;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0af3; asc     {  ;;
 3: len 8; hex 8000000000000074; asc        t;;
 4: len 8; hex 800000000000045c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83092245; asc    n  "E;;
 7: len 8; hex 99bac36e83092440; asc    n  $@;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045d; asc        ];;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0b78; asc     { x;;
 3: len 8; hex 8000000000000075; asc        u;;
 4: len 8; hex 800000000000045d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830928e2; asc    n  ( ;;
 7: len 8; hex 99bac36e83095c49; asc    n  \I;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045e; asc        ^;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0bfd; asc     {  ;;
 3: len 8; hex 8000000000000076; asc        v;;
 4: len 8; hex 800000000000045e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309612f; asc    n  a/;;
 7: len 8; hex 99bac36e830962aa; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045f; asc        _;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0c82; asc     {  ;;
 3: len 8; hex 8000000000000077; asc        w;;
 4: len 8; hex 800000000000045f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83096752; asc    n  gR;;
 7: len 8; hex 99bac36e830968bf; asc    n  h ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000460; asc        `;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0d07; asc     {  ;;
 3: len 8; hex 8000000000000078; asc        x;;
 4: len 8; hex 8000000000000460; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83096da5; asc    n  m ;;
 7: len 8; hex 99bac36e83096efe; asc    n  n ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000461; asc        a;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0d8c; asc     {  ;;
 3: len 8; hex 8000000000000079; asc        y;;
 4: len 8; hex 8000000000000461; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83097366; asc    n  sf;;
 7: len 8; hex 99bac36e830974f4; asc    n  t ;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000462; asc        b;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0e11; asc     {  ;;
 3: len 8; hex 800000000000007a; asc        z;;
 4: len 8; hex 8000000000000462; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83097a3c; asc    n  z<;;
 7: len 8; hex 99bac36e83097bf7; asc    n  { ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000463; asc        c;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0e96; asc     {  ;;
 3: len 8; hex 800000000000007b; asc        {;;
 4: len 8; hex 8000000000000463; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830980b0; asc    n    ;;
 7: len 8; hex 99bac36e83098204; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000464; asc        d;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0f1b; asc     {  ;;
 3: len 8; hex 800000000000007c; asc        |;;
 4: len 8; hex 8000000000000464; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83098645; asc    n   E;;
 7: len 8; hex 99bac36e830987a6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000465; asc        e;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0fa0; asc     {  ;;
 3: len 8; hex 800000000000007d; asc        };;
 4: len 8; hex 8000000000000465; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83098be5; asc    n    ;;
 7: len 8; hex 99bac36e83098d51; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000466; asc        f;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b1025; asc     { %;;
 3: len 8; hex 800000000000007e; asc        ~;;
 4: len 8; hex 8000000000000466; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309918b; asc    n    ;;
 7: len 8; hex 99bac36e830992e1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000467; asc        g;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b10aa; asc     {  ;;
 3: len 8; hex 800000000000007f; asc         ;;
 4: len 8; hex 8000000000000467; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309978f; asc    n    ;;
 7: len 8; hex 99bac36e83099906; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000468; asc        h;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b112f; asc     { /;;
 3: len 8; hex 8000000000000080; asc         ;;
 4: len 8; hex 8000000000000468; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83099df1; asc    n    ;;
 7: len 8; hex 99bac36e83099f63; asc    n   c;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000469; asc        i;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b11b4; asc     {  ;;
 3: len 8; hex 8000000000000081; asc         ;;
 4: len 8; hex 8000000000000469; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309a40b; asc    n    ;;
 7: len 8; hex 99bac36e8309a5ef; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040f; asc         ;;
 1: len 6; hex 000000043b54; asc     ;T;;
 2: len 7; hex 01000001c811b3; asc        ;;
 3: len 8; hex 8000000000000027; asc        ';;
 4: len 8; hex 800000000000040f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306924c; asc    n   L;;
 7: len 8; hex 99bac36e8306939c; asc    n    ;;
 8: len 8; hex 99bac36e890dbc2b; asc    n   +;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 149 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 277352 lock mode S locks rec but not gap waiting
Record lock, heap no 224 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043b8e; asc     ; ;;
 2: len 7; hex 020000013212e2; asc     2  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343234363138303133; asc tk-424618013;;
 5: len 8; hex 99bac36e8b0218a5; asc    n    ;;
 6: len 8; hex 99bac36e8b0218a5; asc    n    ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V8 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 22:58:11 135358810506816
*** (1) TRANSACTION:
TRANSACTION 277390, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 9036, OS thread handle 135358138172992, query id 1248348 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 989

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 149 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 277390 lock_mode X locks rec but not gap
Record lock, heap no 224 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043b8e; asc     ; ;;
 2: len 7; hex 020000013212e2; asc     2  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343234363138303133; asc tk-424618013;;
 5: len 8; hex 99bac36e8b0218a5; asc    n    ;;
 6: len 8; hex 99bac36e8b0218a5; asc    n    ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 147 page no 12 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 277390 lock_mode X locks rec but not gap waiting
Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782040; asc     x @;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304bffd; asc    n    ;;
 7: len 8; hex 99bac36e8304c133; asc    n   3;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 277352, ACTIVE 2 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 64 lock struct(s), heap size 24696, 5478 row lock(s), undo log entries 1465
MySQL thread id 9000, OS thread handle 135358149797440, query id 1248542 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (989, 3989, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 147 page no 12 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 277352 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000370; asc        p;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772701; asc     w' ;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000370; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300e895; asc    n    ;;
 7: len 8; hex 99bac36e8300e9dc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000371; asc        q;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772786; asc     w' ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000371; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300edcb; asc    n    ;;
 7: len 8; hex 99bac36e8300ef25; asc    n   %;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000372; asc        r;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177280b; asc     w( ;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 8000000000000372; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300f30d; asc    n    ;;
 7: len 8; hex 99bac36e8300f449; asc    n   I;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000373; asc        s;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772890; asc     w( ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 8000000000000373; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300f854; asc    n   T;;
 7: len 8; hex 99bac36e8300f987; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000374; asc        t;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772915; asc     w) ;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 8000000000000374; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8300fd56; asc    n   V;;
 7: len 8; hex 99bac36e8300fe84; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000375; asc        u;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177299a; asc     w) ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 8000000000000375; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830102ae; asc    n    ;;
 7: len 8; hex 99bac36e830106cc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000376; asc        v;;
 1: len 6; hex 000000043a8c; asc     : ;;
 2: len 7; hex 01000001991e9d; asc        ;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 8000000000000376; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83010b7a; asc    n   z;;
 7: len 8; hex 99bac36e83010cb7; asc    n    ;;
 8: len 8; hex 99bac36e8307faa5; asc    n    ;;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000377; asc        w;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772aa4; asc     w* ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 8000000000000377; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830110ad; asc    n    ;;
 7: len 8; hex 99bac36e830111e7; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000378; asc        x;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772b29; asc     w+);;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000378; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83011612; asc    n    ;;
 7: len 8; hex 99bac36e8301177a; asc    n   z;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000379; asc        y;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772bae; asc     w+ ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000379; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83011b99; asc    n    ;;
 7: len 8; hex 99bac36e83011cc9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037a; asc        z;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772c33; asc     w,3;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 800000000000037a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83012093; asc    n    ;;
 7: len 8; hex 99bac36e830121b4; asc    n  ! ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037b; asc        {;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772cb8; asc     w, ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 800000000000037b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830125be; asc    n  % ;;
 7: len 8; hex 99bac36e83012706; asc    n  ' ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037c; asc        |;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772d3d; asc     w-=;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 800000000000037c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83012b6f; asc    n  +o;;
 7: len 8; hex 99bac36e83012cd7; asc    n  , ;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037d; asc        };;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772dc2; asc     w- ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 800000000000037d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830130ad; asc    n  0 ;;
 7: len 8; hex 99bac36e830131f7; asc    n  1 ;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037e; asc        ~;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772e47; asc     w.G;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 800000000000037e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830135f9; asc    n  5 ;;
 7: len 8; hex 99bac36e83013730; asc    n  70;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772ecc; asc     w. ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 800000000000037f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83013b3d; asc    n  ;=;;
 7: len 8; hex 99bac36e83014204; asc    n  B ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000380; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772f51; asc     w/Q;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000380; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83015227; asc    n  R';;
 7: len 8; hex 99bac36e83015940; asc    n  Y@;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000381; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001772fd6; asc     w/ ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000381; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830160bf; asc    n  ` ;;
 7: len 8; hex 99bac36e830165b7; asc    n  e ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000382; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177305b; asc     w0[;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 8000000000000382; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83016adf; asc    n  j ;;
 7: len 8; hex 99bac36e83016c51; asc    n  lQ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000383; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017730e0; asc     w0 ;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 8000000000000383; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830170e1; asc    n  p ;;
 7: len 8; hex 99bac36e8301722c; asc    n  r,;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000384; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773165; asc     w1e;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 8000000000000384; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301768b; asc    n  v ;;
 7: len 8; hex 99bac36e83017817; asc    n  x ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000385; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017731ea; asc     w1 ;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 8000000000000385; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83017cc6; asc    n  | ;;
 7: len 8; hex 99bac36e83017e46; asc    n  ~F;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000386; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177326f; asc     w2o;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 8000000000000386; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301824d; asc    n   M;;
 7: len 8; hex 99bac36e830183a6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000387; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017732f4; asc     w2 ;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 8000000000000387; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830187f1; asc    n    ;;
 7: len 8; hex 99bac36e83018959; asc    n   Y;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000388; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773379; asc     w3y;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000388; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301910a; asc    n    ;;
 7: len 8; hex 99bac36e83019254; asc    n   T;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000389; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017733fe; asc     w3 ;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000389; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83019692; asc    n    ;;
 7: len 8; hex 99bac36e83019958; asc    n   X;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038a; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773483; asc     w4 ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 800000000000038a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301a989; asc    n    ;;
 7: len 8; hex 99bac36e8301aaf0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038b; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773508; asc     w5 ;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 800000000000038b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301af18; asc    n    ;;
 7: len 8; hex 99bac36e8301b068; asc    n   h;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038c; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177358d; asc     w5 ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 800000000000038c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301b475; asc    n   u;;
 7: len 8; hex 99bac36e8301b5e2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038d; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773612; asc     w6 ;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 800000000000038d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301bf60; asc    n   `;;
 7: len 8; hex 99bac36e8301c0f6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038e; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773697; asc     w6 ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 800000000000038e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301c5ab; asc    n    ;;
 7: len 8; hex 99bac36e8301c6fc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000177371c; asc     w7 ;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 800000000000038f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301cb4f; asc    n   O;;
 7: len 8; hex 99bac36e8301cc96; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000390; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017737a1; asc     w7 ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000390; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301d0c1; asc    n    ;;
 7: len 8; hex 99bac36e8301d201; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000391; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773826; asc     w8&;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000391; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301dcc2; asc    n    ;;
 7: len 8; hex 99bac36e8301e1be; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000392; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017738ab; asc     w8 ;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 8000000000000392; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301ebad; asc    n    ;;
 7: len 8; hex 99bac36e8301efdb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000393; asc         ;;
 1: len 6; hex 000000043a7c; asc     :|;;
 2: len 7; hex 01000001ba1295; asc        ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 8000000000000393; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8301f56d; asc    n   m;;
 7: len 8; hex 99bac36e8301f77a; asc    n   z;;
 8: len 8; hex 99bac36e8305cc05; asc    n    ;;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000394; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017739b5; asc     w9 ;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 8000000000000394; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83020023; asc    n   #;;
 7: len 8; hex 99bac36e83020496; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000395; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773a3a; asc     w::;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 8000000000000395; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83021303; asc    n    ;;
 7: len 8; hex 99bac36e83021542; asc    n   B;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000396; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773abf; asc     w: ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 8000000000000396; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83021d95; asc    n    ;;
 7: len 8; hex 99bac36e830221be; asc    n  ! ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000397; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773b44; asc     w;D;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 8000000000000397; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830233c3; asc    n  3 ;;
 7: len 8; hex 99bac36e830235fb; asc    n  5 ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000398; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773bc9; asc     w; ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000398; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83023ef8; asc    n  > ;;
 7: len 8; hex 99bac36e83024324; asc    n  C$;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000399; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773c4e; asc     w<N;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000399; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83024f45; asc    n  OE;;
 7: len 8; hex 99bac36e830253ce; asc    n  S ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039a; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773cd3; asc     w< ;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 800000000000039a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83025dbc; asc    n  ] ;;
 7: len 8; hex 99bac36e8302627c; asc    n  b|;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039b; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773d58; asc     w=X;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 800000000000039b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83026b42; asc    n  kB;;
 7: len 8; hex 99bac36e830270fa; asc    n  p ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039c; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773ddd; asc     w= ;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 800000000000039c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83027997; asc    n  y ;;
 7: len 8; hex 99bac36e83028064; asc    n   d;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039d; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773e62; asc     w>b;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 800000000000039d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83028939; asc    n   9;;
 7: len 8; hex 99bac36e83028f2f; asc    n   /;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039e; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773ee7; asc     w> ;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 800000000000039e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830299d1; asc    n    ;;
 7: len 8; hex 99bac36e83029c1e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001773f6c; asc     w?l;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 800000000000039f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302a319; asc    n    ;;
 7: len 8; hex 99bac36e8302a735; asc    n   5;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178008f; asc     x  ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 80000000000003a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302afd9; asc    n    ;;
 7: len 8; hex 99bac36e8302b3a5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780114; asc     x  ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 80000000000003a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302bcc3; asc    n    ;;
 7: len 8; hex 99bac36e8302c0ed; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780199; asc     x  ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 80000000000003a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302c96b; asc    n   k;;
 7: len 8; hex 99bac36e8302cd1f; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178021e; asc     x  ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 80000000000003a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302d6df; asc    n    ;;
 7: len 8; hex 99bac36e8302d8ba; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017802a3; asc     x  ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 80000000000003a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302e3d1; asc    n    ;;
 7: len 8; hex 99bac36e8302e807; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780328; asc     x (;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 80000000000003a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302f136; asc    n   6;;
 7: len 8; hex 99bac36e8302f5c3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a6; asc         ;;
 1: len 6; hex 000000043b4e; asc     ;N;;
 2: len 7; hex 01000001bb13d5; asc        ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 80000000000003a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8302fe62; asc    n   b;;
 7: len 8; hex 99bac36e83030254; asc    n   T;;
 8: len 8; hex 99bac36e8803fd20; asc    n    ;;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780432; asc     x 2;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 80000000000003a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83030c0c; asc    n    ;;
 7: len 8; hex 99bac36e83030e86; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017804b7; asc     x  ;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 80000000000003a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830319cf; asc    n    ;;
 7: len 8; hex 99bac36e83031e6a; asc    n   j;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178053c; asc     x <;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 80000000000003a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830328fb; asc    n  ( ;;
 7: len 8; hex 99bac36e83032d97; asc    n  - ;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003aa; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017805c1; asc     x  ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 80000000000003aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830335dd; asc    n  5 ;;
 7: len 8; hex 99bac36e83033a1b; asc    n  : ;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ab; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780646; asc     x F;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 80000000000003ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83034316; asc    n  C ;;
 7: len 8; hex 99bac36e83034646; asc    n  FF;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ac; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017806cb; asc     x  ;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 80000000000003ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83034fa9; asc    n  O ;;
 7: len 8; hex 99bac36e830352d8; asc    n  R ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ad; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780750; asc     x P;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 80000000000003ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83035cc5; asc    n  \ ;;
 7: len 8; hex 99bac36e8303614f; asc    n  aO;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ae; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017807d5; asc     x  ;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 80000000000003ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83036b3d; asc    n  k=;;
 7: len 8; hex 99bac36e83036fe9; asc    n  o ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003af; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178085a; asc     x Z;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 80000000000003af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83037707; asc    n  w ;;
 7: len 8; hex 99bac36e83037959; asc    n  yY;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017808df; asc     x  ;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 80000000000003b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83038131; asc    n   1;;
 7: len 8; hex 99bac36e830383f5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780964; asc     x d;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 80000000000003b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83038c87; asc    n    ;;
 7: len 8; hex 99bac36e83039039; asc    n   9;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017809e9; asc     x  ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 80000000000003b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303998c; asc    n    ;;
 7: len 8; hex 99bac36e83039cb3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780a6e; asc     x n;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 80000000000003b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303a24c; asc    n   L;;
 7: len 8; hex 99bac36e8303a41e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780af3; asc     x  ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 80000000000003b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303a9f9; asc    n    ;;
 7: len 8; hex 99bac36e8303abe1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780b78; asc     x x;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 80000000000003b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303b19a; asc    n    ;;
 7: len 8; hex 99bac36e8303b31e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780bfd; asc     x  ;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 80000000000003b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303b858; asc    n   X;;
 7: len 8; hex 99bac36e8303b9bf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780c82; asc     x  ;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 80000000000003b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303be70; asc    n   p;;
 7: len 8; hex 99bac36e8303bfd9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780d07; asc     x  ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000003b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303c43a; asc    n   :;;
 7: len 8; hex 99bac36e8303c68d; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780d8c; asc     x  ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000003b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303cbb9; asc    n    ;;
 7: len 8; hex 99bac36e8303cdbe; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ba; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780e11; asc     x  ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000003ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303d60e; asc    n    ;;
 7: len 8; hex 99bac36e8303d8d1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bb; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780e96; asc     x  ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000003bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303e2cf; asc    n    ;;
 7: len 8; hex 99bac36e8303e6bf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780f1b; asc     x  ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000003bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303ef61; asc    n   a;;
 7: len 8; hex 99bac36e8303f7c3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001780fa0; asc     x  ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000003bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8303ff49; asc    n   I;;
 7: len 8; hex 99bac36e83040286; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003be; asc         ;;
 1: len 6; hex 000000043a7f; asc     : ;;
 2: len 7; hex 02000001a11817; asc        ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000003be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83040988; asc    n    ;;
 7: len 8; hex 99bac36e83040b26; asc    n   &;;
 8: len 8; hex 99bac36e8305cd57; asc    n   W;;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bf; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017810aa; asc     x  ;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000003bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83040fa2; asc    n    ;;
 7: len 8; hex 99bac36e830410de; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c0; asc         ;;
 1: len 6; hex 000000043a7e; asc     :~;;
 2: len 7; hex 01000000df29b6; asc      ) ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000003c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830415fc; asc    n    ;;
 7: len 8; hex 99bac36e830417a6; asc    n    ;;
 8: len 8; hex 99bac36e8305ccb0; asc    n    ;;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017811b4; asc     x  ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000003c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83041c19; asc    n    ;;
 7: len 8; hex 99bac36e83041db0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781239; asc     x 9;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000003c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83042218; asc    n  " ;;
 7: len 8; hex 99bac36e83042362; asc    n  #b;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017812be; asc     x  ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000003c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830427f8; asc    n  ' ;;
 7: len 8; hex 99bac36e8304294f; asc    n  )O;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781343; asc     x C;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000003c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83042daa; asc    n  - ;;
 7: len 8; hex 99bac36e83042f1a; asc    n  / ;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017813c8; asc     x  ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000003c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043369; asc    n  3i;;
 7: len 8; hex 99bac36e830434b2; asc    n  4 ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178144d; asc     x M;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000003c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043912; asc    n  9 ;;
 7: len 8; hex 99bac36e83043a57; asc    n  :W;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017814d2; asc     x  ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000003c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83043efb; asc    n  > ;;
 7: len 8; hex 99bac36e8304404f; asc    n  @O;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781557; asc     x W;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000003c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304442b; asc    n  D+;;
 7: len 8; hex 99bac36e830445b3; asc    n  E ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017815dc; asc     x  ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000003c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83044a3a; asc    n  J:;;
 7: len 8; hex 99bac36e83044b90; asc    n  K ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ca; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781661; asc     x a;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000003ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045041; asc    n  PA;;
 7: len 8; hex 99bac36e83045199; asc    n  Q ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cb; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017816e6; asc     x  ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000003cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045621; asc    n  V!;;
 7: len 8; hex 99bac36e83045795; asc    n  W ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178176b; asc     x k;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000003cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83045bf8; asc    n  [ ;;
 7: len 8; hex 99bac36e83045d47; asc    n  ]G;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017817f0; asc     x  ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000003cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046162; asc    n  ab;;
 7: len 8; hex 99bac36e830462ac; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ce; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781875; asc     x u;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000003ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046701; asc    n  g ;;
 7: len 8; hex 99bac36e8304683b; asc    n  h;;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cf; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017818fa; asc     x  ;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000003cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83046c32; asc    n  l2;;
 7: len 8; hex 99bac36e83046d8e; asc    n  m ;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178197f; asc     x  ;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000003d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047257; asc    n  rW;;
 7: len 8; hex 99bac36e830473b9; asc    n  s ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781a04; asc     x  ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000003d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047878; asc    n  xx;;
 7: len 8; hex 99bac36e83047a1f; asc    n  z ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781a89; asc     x  ;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000003d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83047ed8; asc    n  ~ ;;
 7: len 8; hex 99bac36e8304803a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781b0e; asc     x  ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000003d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304850b; asc    n    ;;
 7: len 8; hex 99bac36e83048689; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781b93; asc     x  ;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000003d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83048aff; asc    n    ;;
 7: len 8; hex 99bac36e83048d1a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781c18; asc     x  ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000003d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830491b5; asc    n    ;;
 7: len 8; hex 99bac36e83049308; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781c9d; asc     x  ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000003d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830497e9; asc    n    ;;
 7: len 8; hex 99bac36e8304996e; asc    n   n;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781d22; asc     x ";;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000003d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83049de0; asc    n    ;;
 7: len 8; hex 99bac36e83049f2b; asc    n   +;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781da7; asc     x  ;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000003d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304a364; asc    n   d;;
 7: len 8; hex 99bac36e8304a50b; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d9; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781e2c; asc     x ,;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000003d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304a9a5; asc    n    ;;
 7: len 8; hex 99bac36e8304aaea; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003da; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781eb1; asc     x  ;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000003da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304af49; asc    n   I;;
 7: len 8; hex 99bac36e8304b0b3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003db; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781f36; asc     x 6;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000003db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304b537; asc    n   7;;
 7: len 8; hex 99bac36e8304b68a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dc; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001781fbb; asc     x  ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000003dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304babf; asc    n    ;;
 7: len 8; hex 99bac36e8304bc0e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782040; asc     x @;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304bffd; asc    n    ;;
 7: len 8; hex 99bac36e8304c133; asc    n   3;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003de; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017820c5; asc     x  ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000003de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304c59d; asc    n    ;;
 7: len 8; hex 99bac36e8304e236; asc    n   6;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003df; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178214a; asc     x!J;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000003df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304e6f3; asc    n    ;;
 7: len 8; hex 99bac36e8304e87c; asc    n   |;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e0; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017821cf; asc     x! ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000003e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304ecfe; asc    n    ;;
 7: len 8; hex 99bac36e8304ee62; asc    n   b;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e1; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782254; asc     x"T;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000003e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304f307; asc    n    ;;
 7: len 8; hex 99bac36e8304f46b; asc    n   k;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e2; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017822d9; asc     x" ;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000003e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304f86f; asc    n   o;;
 7: len 8; hex 99bac36e8304fa19; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e3; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 0200000178235e; asc     x#^;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000003e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8304fea6; asc    n    ;;
 7: len 8; hex 99bac36e8305000c; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e4; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017823e3; asc     x# ;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000003e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830504d8; asc    n    ;;
 7: len 8; hex 99bac36e83050674; asc    n   t;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782468; asc     x$h;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000003e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83050b64; asc    n   d;;
 7: len 8; hex 99bac36e83050ceb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e6; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017824ed; asc     x$ ;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000003e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305118d; asc    n    ;;
 7: len 8; hex 99bac36e8305132f; asc    n   /;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e7; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 02000001782572; asc     x%r;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000003e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830517d9; asc    n    ;;
 7: len 8; hex 99bac36e8305193d; asc    n   =;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e8; asc         ;;
 1: len 6; hex 000000043a52; asc     :R;;
 2: len 7; hex 020000017825f7; asc     x% ;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000003e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83051de3; asc    n    ;;
 7: len 8; hex 99bac36e83051f71; asc    n   q;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e9; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001060efd; asc        ;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000003e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830575e2; asc    n  u ;;
 7: len 8; hex 99bac36e830577c9; asc    n  w ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ea; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001060f80; asc        ;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000003ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305b490; asc    n    ;;
 7: len 8; hex 99bac36e8305b65e; asc    n   ^;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003eb; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061003; asc        ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000003eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305bb64; asc    n   d;;
 7: len 8; hex 99bac36e8305bd20; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ec; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061086; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000003ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305c513; asc    n    ;;
 7: len 8; hex 99bac36e8305ca83; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ed; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061109; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 80000000000003ed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305cee5; asc    n    ;;
 7: len 8; hex 99bac36e8305d0cd; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ee; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106118c; asc        ;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 80000000000003ee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305d591; asc    n    ;;
 7: len 8; hex 99bac36e8305d71a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ef; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106120f; asc        ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 80000000000003ef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305db51; asc    n   Q;;
 7: len 8; hex 99bac36e8305dcdf; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f0; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061292; asc        ;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 80000000000003f0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305e118; asc    n    ;;
 7: len 8; hex 99bac36e8305e275; asc    n   u;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f1; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061315; asc        ;;
 3: len 8; hex 8000000000000009; asc         ;;
 4: len 8; hex 80000000000003f1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305e746; asc    n   F;;
 7: len 8; hex 99bac36e8305e8a8; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f2; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061398; asc        ;;
 3: len 8; hex 800000000000000a; asc         ;;
 4: len 8; hex 80000000000003f2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305ed25; asc    n   %;;
 7: len 8; hex 99bac36e8305eea6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f3; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106141b; asc        ;;
 3: len 8; hex 800000000000000b; asc         ;;
 4: len 8; hex 80000000000003f3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305f2ad; asc    n    ;;
 7: len 8; hex 99bac36e8305f4a5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f4; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106149e; asc        ;;
 3: len 8; hex 800000000000000c; asc         ;;
 4: len 8; hex 80000000000003f4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8305fa61; asc    n   a;;
 7: len 8; hex 99bac36e8305fc16; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f5; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061521; asc       !;;
 3: len 8; hex 800000000000000d; asc         ;;
 4: len 8; hex 80000000000003f5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830600c3; asc    n    ;;
 7: len 8; hex 99bac36e830601f5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f6; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010615a4; asc        ;;
 3: len 8; hex 800000000000000e; asc         ;;
 4: len 8; hex 80000000000003f6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83060626; asc    n   &;;
 7: len 8; hex 99bac36e83060788; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f7; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061627; asc       ';;
 3: len 8; hex 800000000000000f; asc         ;;
 4: len 8; hex 80000000000003f7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83060b55; asc    n   U;;
 7: len 8; hex 99bac36e83060c9c; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f8; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010616aa; asc        ;;
 3: len 8; hex 8000000000000010; asc         ;;
 4: len 8; hex 80000000000003f8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830610bc; asc    n    ;;
 7: len 8; hex 99bac36e830611f2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003f9; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106172d; asc       -;;
 3: len 8; hex 8000000000000011; asc         ;;
 4: len 8; hex 80000000000003f9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83061633; asc    n   3;;
 7: len 8; hex 99bac36e8306176e; asc    n   n;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fa; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010617b0; asc        ;;
 3: len 8; hex 8000000000000012; asc         ;;
 4: len 8; hex 80000000000003fa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83061b9e; asc    n    ;;
 7: len 8; hex 99bac36e83061cda; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fb; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061833; asc       3;;
 3: len 8; hex 8000000000000013; asc         ;;
 4: len 8; hex 80000000000003fb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306211a; asc    n  ! ;;
 7: len 8; hex 99bac36e8306225f; asc    n  "_;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fc; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010618b6; asc        ;;
 3: len 8; hex 8000000000000014; asc         ;;
 4: len 8; hex 80000000000003fc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306266c; asc    n  &l;;
 7: len 8; hex 99bac36e830627ac; asc    n  ' ;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fd; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061939; asc       9;;
 3: len 8; hex 8000000000000015; asc         ;;
 4: len 8; hex 80000000000003fd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83062d3c; asc    n  -<;;
 7: len 8; hex 99bac36e83062e92; asc    n  . ;;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003fe; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010619bc; asc        ;;
 3: len 8; hex 8000000000000016; asc         ;;
 4: len 8; hex 80000000000003fe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830632f4; asc    n  2 ;;
 7: len 8; hex 99bac36e8306346b; asc    n  4k;;
 8: SQL NULL;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ff; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061a3f; asc       ?;;
 3: len 8; hex 8000000000000017; asc         ;;
 4: len 8; hex 80000000000003ff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830638a4; asc    n  8 ;;
 7: len 8; hex 99bac36e830639dc; asc    n  9 ;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000400; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061ac2; asc        ;;
 3: len 8; hex 8000000000000018; asc         ;;
 4: len 8; hex 8000000000000400; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83063dfc; asc    n  = ;;
 7: len 8; hex 99bac36e83063f58; asc    n  ?X;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000401; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061b45; asc       E;;
 3: len 8; hex 8000000000000019; asc         ;;
 4: len 8; hex 8000000000000401; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306431c; asc    n  C ;;
 7: len 8; hex 99bac36e8306447e; asc    n  D~;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000402; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061bc8; asc        ;;
 3: len 8; hex 800000000000001a; asc         ;;
 4: len 8; hex 8000000000000402; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306484e; asc    n  HN;;
 7: len 8; hex 99bac36e83064972; asc    n  Ir;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000403; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061c4b; asc       K;;
 3: len 8; hex 800000000000001b; asc         ;;
 4: len 8; hex 8000000000000403; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83064d40; asc    n  M@;;
 7: len 8; hex 99bac36e83064e74; asc    n  Nt;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000404; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061cce; asc        ;;
 3: len 8; hex 800000000000001c; asc         ;;
 4: len 8; hex 8000000000000404; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83065211; asc    n  R ;;
 7: len 8; hex 99bac36e83065364; asc    n  Sd;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000405; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061d51; asc       Q;;
 3: len 8; hex 800000000000001d; asc         ;;
 4: len 8; hex 8000000000000405; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306577e; asc    n  W~;;
 7: len 8; hex 99bac36e830658ee; asc    n  X ;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000406; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061dd4; asc        ;;
 3: len 8; hex 800000000000001e; asc         ;;
 4: len 8; hex 8000000000000406; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83065d23; asc    n  ]#;;
 7: len 8; hex 99bac36e83065e75; asc    n  ^u;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000407; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061e57; asc       W;;
 3: len 8; hex 800000000000001f; asc         ;;
 4: len 8; hex 8000000000000407; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83066292; asc    n  b ;;
 7: len 8; hex 99bac36e830663d7; asc    n  c ;;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000408; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061eda; asc        ;;
 3: len 8; hex 8000000000000020; asc         ;;
 4: len 8; hex 8000000000000408; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830667ea; asc    n  g ;;
 7: len 8; hex 99bac36e8306693d; asc    n  i=;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000409; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061f5d; asc       ];;
 3: len 8; hex 8000000000000021; asc        !;;
 4: len 8; hex 8000000000000409; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83066dec; asc    n  m ;;
 7: len 8; hex 99bac36e83066f42; asc    n  oB;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040a; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001061fe0; asc        ;;
 3: len 8; hex 8000000000000022; asc        ";;
 4: len 8; hex 800000000000040a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83067323; asc    n  s#;;
 7: len 8; hex 99bac36e83067490; asc    n  t ;;
 8: SQL NULL;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040b; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062063; asc       c;;
 3: len 8; hex 8000000000000023; asc        #;;
 4: len 8; hex 800000000000040b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830678a6; asc    n  x ;;
 7: len 8; hex 99bac36e83067a28; asc    n  z(;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040c; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010620e6; asc        ;;
 3: len 8; hex 8000000000000024; asc        $;;
 4: len 8; hex 800000000000040c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83067eb7; asc    n  ~ ;;
 7: len 8; hex 99bac36e8306800e; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040d; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062169; asc      !i;;
 3: len 8; hex 8000000000000025; asc        %;;
 4: len 8; hex 800000000000040d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830685ba; asc    n    ;;
 7: len 8; hex 99bac36e8306875c; asc    n   \;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040e; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010621ec; asc      ! ;;
 3: len 8; hex 8000000000000026; asc        &;;
 4: len 8; hex 800000000000040e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83068c0a; asc    n    ;;
 7: len 8; hex 99bac36e83068d88; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000410; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010622f2; asc      " ;;
 3: len 8; hex 8000000000000028; asc        (;;
 4: len 8; hex 8000000000000410; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830697e0; asc    n    ;;
 7: len 8; hex 99bac36e8306994f; asc    n   O;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000411; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062375; asc      #u;;
 3: len 8; hex 8000000000000029; asc        );;
 4: len 8; hex 8000000000000411; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83069d9e; asc    n    ;;
 7: len 8; hex 99bac36e83069efe; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000412; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010623f8; asc      # ;;
 3: len 8; hex 800000000000002a; asc        *;;
 4: len 8; hex 8000000000000412; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306a2d3; asc    n    ;;
 7: len 8; hex 99bac36e8306a43f; asc    n   ?;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000413; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106247b; asc      ${;;
 3: len 8; hex 800000000000002b; asc        +;;
 4: len 8; hex 8000000000000413; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306a90f; asc    n    ;;
 7: len 8; hex 99bac36e8306aa6a; asc    n   j;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000414; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010624ff; asc      $ ;;
 3: len 8; hex 800000000000002c; asc        ,;;
 4: len 8; hex 8000000000000414; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306ae66; asc    n   f;;
 7: len 8; hex 99bac36e8306afbb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000415; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062584; asc      % ;;
 3: len 8; hex 800000000000002d; asc        -;;
 4: len 8; hex 8000000000000415; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306b38f; asc    n    ;;
 7: len 8; hex 99bac36e8306b4b9; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000416; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062609; asc      & ;;
 3: len 8; hex 800000000000002e; asc        .;;
 4: len 8; hex 8000000000000416; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306b860; asc    n   `;;
 7: len 8; hex 99bac36e8306ee50; asc    n   P;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000417; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106268e; asc      & ;;
 3: len 8; hex 800000000000002f; asc        /;;
 4: len 8; hex 8000000000000417; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306f312; asc    n    ;;
 7: len 8; hex 99bac36e8306f466; asc    n   f;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000418; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062713; asc      ' ;;
 3: len 8; hex 8000000000000030; asc        0;;
 4: len 8; hex 8000000000000418; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306f8bc; asc    n    ;;
 7: len 8; hex 99bac36e8306fa16; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000419; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062798; asc      ' ;;
 3: len 8; hex 8000000000000031; asc        1;;
 4: len 8; hex 8000000000000419; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306fe28; asc    n   (;;
 7: len 8; hex 99bac36e8306ffa7; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041a; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106281d; asc      ( ;;
 3: len 8; hex 8000000000000032; asc        2;;
 4: len 8; hex 800000000000041a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830703e7; asc    n    ;;
 7: len 8; hex 99bac36e83070514; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041b; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010628a2; asc      ( ;;
 3: len 8; hex 8000000000000033; asc        3;;
 4: len 8; hex 800000000000041b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307094d; asc    n   M;;
 7: len 8; hex 99bac36e83074084; asc    n  @ ;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041c; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062927; asc      )';;
 3: len 8; hex 8000000000000034; asc        4;;
 4: len 8; hex 800000000000041c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830746c9; asc    n  F ;;
 7: len 8; hex 99bac36e830748dc; asc    n  H ;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041d; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010629ac; asc      ) ;;
 3: len 8; hex 8000000000000035; asc        5;;
 4: len 8; hex 800000000000041d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83074e5b; asc    n  N[;;
 7: len 8; hex 99bac36e83075019; asc    n  P ;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041e; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062a31; asc      *1;;
 3: len 8; hex 8000000000000036; asc        6;;
 4: len 8; hex 800000000000041e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830754c0; asc    n  T ;;
 7: len 8; hex 99bac36e8307565b; asc    n  V[;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000041f; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062ab6; asc      * ;;
 3: len 8; hex 8000000000000037; asc        7;;
 4: len 8; hex 800000000000041f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83075c69; asc    n  \i;;
 7: len 8; hex 99bac36e83076184; asc    n  a ;;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000420; asc         ;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062b3b; asc      +;;;
 3: len 8; hex 8000000000000038; asc        8;;
 4: len 8; hex 8000000000000420; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830766cd; asc    n  f ;;
 7: len 8; hex 99bac36e83076857; asc    n  hW;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000421; asc        !;;
 1: len 6; hex 000000043aba; asc     : ;;
 2: len 7; hex 02000001711eb0; asc     q  ;;
 3: len 8; hex 8000000000000039; asc        9;;
 4: len 8; hex 8000000000000421; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83076d0a; asc    n  m ;;
 7: len 8; hex 99bac36e830770d4; asc    n  p ;;
 8: len 8; hex 99bac36e840bad8a; asc    n    ;;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000422; asc        ";;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062c45; asc      ,E;;
 3: len 8; hex 800000000000003a; asc        :;;
 4: len 8; hex 8000000000000422; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830775d4; asc    n  u ;;
 7: len 8; hex 99bac36e83077742; asc    n  wB;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000423; asc        #;;
 1: len 6; hex 000000043ab2; asc     : ;;
 2: len 7; hex 02000001170da3; asc        ;;
 3: len 8; hex 800000000000003b; asc        ;;;
 4: len 8; hex 8000000000000423; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83077bae; asc    n  { ;;
 7: len 8; hex 99bac36e83077eda; asc    n  ~ ;;
 8: len 8; hex 99bac36e8407065e; asc    n   ^;;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000424; asc        $;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062d4f; asc      -O;;
 3: len 8; hex 800000000000003c; asc        <;;
 4: len 8; hex 8000000000000424; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83078374; asc    n   t;;
 7: len 8; hex 99bac36e83078513; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000425; asc        %;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062dd4; asc      - ;;
 3: len 8; hex 800000000000003d; asc        =;;
 4: len 8; hex 8000000000000425; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83078a7d; asc    n   };;
 7: len 8; hex 99bac36e83078db4; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000426; asc        &;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062e59; asc      .Y;;
 3: len 8; hex 800000000000003e; asc        >;;
 4: len 8; hex 8000000000000426; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83079258; asc    n   X;;
 7: len 8; hex 99bac36e8307cb4b; asc    n   K;;
 8: SQL NULL;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000427; asc        ';;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062ede; asc      . ;;
 3: len 8; hex 800000000000003f; asc        ?;;
 4: len 8; hex 8000000000000427; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307d0a7; asc    n    ;;
 7: len 8; hex 99bac36e8307d2f3; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000428; asc        (;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062f63; asc      /c;;
 3: len 8; hex 8000000000000040; asc        @;;
 4: len 8; hex 8000000000000428; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307d7e9; asc    n    ;;
 7: len 8; hex 99bac36e8307d994; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000429; asc        );;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001062fe8; asc      / ;;
 3: len 8; hex 8000000000000041; asc        A;;
 4: len 8; hex 8000000000000429; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307de3a; asc    n   :;;
 7: len 8; hex 99bac36e8307dfed; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042a; asc        *;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106306d; asc      0m;;
 3: len 8; hex 8000000000000042; asc        B;;
 4: len 8; hex 800000000000042a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307e4dd; asc    n    ;;
 7: len 8; hex 99bac36e8307e688; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042b; asc        +;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010630f2; asc      0 ;;
 3: len 8; hex 8000000000000043; asc        C;;
 4: len 8; hex 800000000000042b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307ebda; asc    n    ;;
 7: len 8; hex 99bac36e8307ed5f; asc    n   _;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042c; asc        ,;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063177; asc      1w;;
 3: len 8; hex 8000000000000044; asc        D;;
 4: len 8; hex 800000000000042c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307f220; asc    n    ;;
 7: len 8; hex 99bac36e8307f3aa; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042d; asc        -;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010631fc; asc      1 ;;
 3: len 8; hex 8000000000000045; asc        E;;
 4: len 8; hex 800000000000042d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307f872; asc    n   r;;
 7: len 8; hex 99bac36e8307fa3a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042e; asc        .;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063281; asc      2 ;;
 3: len 8; hex 8000000000000046; asc        F;;
 4: len 8; hex 800000000000042e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8307ff10; asc    n    ;;
 7: len 8; hex 99bac36e830800a0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000042f; asc        /;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063306; asc      3 ;;
 3: len 8; hex 8000000000000047; asc        G;;
 4: len 8; hex 800000000000042f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830805d2; asc    n    ;;
 7: len 8; hex 99bac36e83080780; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000430; asc        0;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106338b; asc      3 ;;
 3: len 8; hex 8000000000000048; asc        H;;
 4: len 8; hex 8000000000000430; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83080eac; asc    n    ;;
 7: len 8; hex 99bac36e830810bb; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000431; asc        1;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063410; asc      4 ;;
 3: len 8; hex 8000000000000049; asc        I;;
 4: len 8; hex 8000000000000431; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308159f; asc    n    ;;
 7: len 8; hex 99bac36e83081708; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000432; asc        2;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063495; asc      4 ;;
 3: len 8; hex 800000000000004a; asc        J;;
 4: len 8; hex 8000000000000432; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83081bf5; asc    n    ;;
 7: len 8; hex 99bac36e83081da0; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000433; asc        3;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106351a; asc      5 ;;
 3: len 8; hex 800000000000004b; asc        K;;
 4: len 8; hex 8000000000000433; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830824b9; asc    n  $ ;;
 7: len 8; hex 99bac36e830825fc; asc    n  % ;;
 8: SQL NULL;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000434; asc        4;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 0200000106359f; asc      5 ;;
 3: len 8; hex 800000000000004c; asc        L;;
 4: len 8; hex 8000000000000434; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83082b07; asc    n  + ;;
 7: len 8; hex 99bac36e83082d33; asc    n  -3;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000435; asc        5;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063624; asc      6$;;
 3: len 8; hex 800000000000004d; asc        M;;
 4: len 8; hex 8000000000000435; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83083265; asc    n  2e;;
 7: len 8; hex 99bac36e83083412; asc    n  4 ;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000436; asc        6;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010636a9; asc      6 ;;
 3: len 8; hex 800000000000004e; asc        N;;
 4: len 8; hex 8000000000000436; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830838c7; asc    n  8 ;;
 7: len 8; hex 99bac36e83083a64; asc    n  :d;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000437; asc        7;;
 1: len 6; hex 000000043ab4; asc     : ;;
 2: len 7; hex 02000001612881; asc     a( ;;
 3: len 8; hex 800000000000004f; asc        O;;
 4: len 8; hex 8000000000000437; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83083f96; asc    n  ? ;;
 7: len 8; hex 99bac36e8308414d; asc    n  AM;;
 8: len 8; hex 99bac36e840707dd; asc    n    ;;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000438; asc        8;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010637b3; asc      7 ;;
 3: len 8; hex 8000000000000050; asc        P;;
 4: len 8; hex 8000000000000438; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83084616; asc    n  F ;;
 7: len 8; hex 99bac36e830847a3; asc    n  G ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000439; asc        9;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063838; asc      88;;
 3: len 8; hex 8000000000000051; asc        Q;;
 4: len 8; hex 8000000000000439; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83084dee; asc    n  M ;;
 7: len 8; hex 99bac36e83084fa7; asc    n  O ;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043a; asc        :;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010638bd; asc      8 ;;
 3: len 8; hex 8000000000000052; asc        R;;
 4: len 8; hex 800000000000043a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83085442; asc    n  TB;;
 7: len 8; hex 99bac36e830855c1; asc    n  U ;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043b; asc        ;;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063942; asc      9B;;
 3: len 8; hex 8000000000000053; asc        S;;
 4: len 8; hex 800000000000043b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83085a92; asc    n  Z ;;
 7: len 8; hex 99bac36e83085c31; asc    n  \1;;
 8: SQL NULL;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043c; asc        <;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000010639c7; asc      9 ;;
 3: len 8; hex 8000000000000054; asc        T;;
 4: len 8; hex 800000000000043c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83086129; asc    n  a);;
 7: len 8; hex 99bac36e830862db; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043d; asc        =;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063a4c; asc      :L;;
 3: len 8; hex 8000000000000055; asc        U;;
 4: len 8; hex 800000000000043d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830867bc; asc    n  g ;;
 7: len 8; hex 99bac36e8308695c; asc    n  i\;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043e; asc        >;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ad1; asc      : ;;
 3: len 8; hex 8000000000000056; asc        V;;
 4: len 8; hex 800000000000043e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83086dff; asc    n  m ;;
 7: len 8; hex 99bac36e83086f98; asc    n  o ;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000043f; asc        ?;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063b56; asc      ;V;;
 3: len 8; hex 8000000000000057; asc        W;;
 4: len 8; hex 800000000000043f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83087446; asc    n  tF;;
 7: len 8; hex 99bac36e830875a4; asc    n  u ;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000440; asc        @;;
 1: len 6; hex 000000043ad7; asc     : ;;
 2: len 7; hex 0200000143040d; asc     C  ;;
 3: len 8; hex 8000000000000058; asc        X;;
 4: len 8; hex 8000000000000440; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830879ef; asc    n  y ;;
 7: len 8; hex 99bac36e83087b85; asc    n  { ;;
 8: len 8; hex 99bac36e8509012b; asc    n   +;;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000441; asc        A;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063c60; asc      <`;;
 3: len 8; hex 8000000000000059; asc        Y;;
 4: len 8; hex 8000000000000441; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83087ffe; asc    n    ;;
 7: len 8; hex 99bac36e8308813a; asc    n   :;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000442; asc        B;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ce5; asc      < ;;
 3: len 8; hex 800000000000005a; asc        Z;;
 4: len 8; hex 8000000000000442; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83088591; asc    n    ;;
 7: len 8; hex 99bac36e83088748; asc    n   H;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000443; asc        C;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063d6a; asc      =j;;
 3: len 8; hex 800000000000005b; asc        [;;
 4: len 8; hex 8000000000000443; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83088bbf; asc    n    ;;
 7: len 8; hex 99bac36e83088d1a; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000444; asc        D;;
 1: len 6; hex 000000043ab5; asc     : ;;
 2: len 7; hex 01000000f52888; asc      ( ;;
 3: len 8; hex 800000000000005c; asc        \;;
 4: len 8; hex 8000000000000444; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830891a1; asc    n    ;;
 7: len 8; hex 99bac36e83089307; asc    n    ;;
 8: len 8; hex 99bac36e840708bc; asc    n    ;;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000445; asc        E;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063e74; asc      >t;;
 3: len 8; hex 800000000000005d; asc        ];;
 4: len 8; hex 8000000000000445; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308975b; asc    n   [;;
 7: len 8; hex 99bac36e830898bc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000446; asc        F;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063ef9; asc      > ;;
 3: len 8; hex 800000000000005e; asc        ^;;
 4: len 8; hex 8000000000000446; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83089d69; asc    n   i;;
 7: len 8; hex 99bac36e83089edc; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000447; asc        G;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 02000001063f7e; asc      ?~;;
 3: len 8; hex 800000000000005f; asc        _;;
 4: len 8; hex 8000000000000447; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308a36e; asc    n   n;;
 7: len 8; hex 99bac36e8308a4b6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000448; asc        H;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b008f; asc     {  ;;
 3: len 8; hex 8000000000000060; asc        `;;
 4: len 8; hex 8000000000000448; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308a8d5; asc    n    ;;
 7: len 8; hex 99bac36e8308aa13; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000449; asc        I;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0114; asc     {  ;;
 3: len 8; hex 8000000000000061; asc        a;;
 4: len 8; hex 8000000000000449; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308aea6; asc    n    ;;
 7: len 8; hex 99bac36e8308aff2; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044a; asc        J;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0199; asc     {  ;;
 3: len 8; hex 8000000000000062; asc        b;;
 4: len 8; hex 800000000000044a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308b47a; asc    n   z;;
 7: len 8; hex 99bac36e8308b5e5; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044b; asc        K;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b021e; asc     {  ;;
 3: len 8; hex 8000000000000063; asc        c;;
 4: len 8; hex 800000000000044b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308ba1a; asc    n    ;;
 7: len 8; hex 99bac36e8308bb86; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044c; asc        L;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b02a3; asc     {  ;;
 3: len 8; hex 8000000000000064; asc        d;;
 4: len 8; hex 800000000000044c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308c066; asc    n   f;;
 7: len 8; hex 99bac36e8308c234; asc    n   4;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044d; asc        M;;
 1: len 6; hex 000000043b66; asc     ;f;;
 2: len 7; hex 02000001472f50; asc     G/P;;
 3: len 8; hex 8000000000000065; asc        e;;
 4: len 8; hex 800000000000044d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308c722; asc    n   ";;
 7: len 8; hex 99bac36e8308c88f; asc    n    ;;
 8: len 8; hex 99bac36e890dba5d; asc    n   ];;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044e; asc        N;;
 1: len 6; hex 000000043b64; asc     ;d;;
 2: len 7; hex 01000001b816b2; asc        ;;
 3: len 8; hex 8000000000000066; asc        f;;
 4: len 8; hex 800000000000044e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308cda2; asc    n    ;;
 7: len 8; hex 99bac36e8308cf2b; asc    n   +;;
 8: len 8; hex 99bac36e890db857; asc    n   W;;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000044f; asc        O;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0432; asc     { 2;;
 3: len 8; hex 8000000000000067; asc        g;;
 4: len 8; hex 800000000000044f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308d3e0; asc    n    ;;
 7: len 8; hex 99bac36e8308d553; asc    n   S;;
 8: SQL NULL;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000450; asc        P;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b04b7; asc     {  ;;
 3: len 8; hex 8000000000000068; asc        h;;
 4: len 8; hex 8000000000000450; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308d9c3; asc    n    ;;
 7: len 8; hex 99bac36e8308db51; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000451; asc        Q;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b053c; asc     { <;;
 3: len 8; hex 8000000000000069; asc        i;;
 4: len 8; hex 8000000000000451; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308e02a; asc    n   *;;
 7: len 8; hex 99bac36e8308e19f; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000452; asc        R;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b05c1; asc     {  ;;
 3: len 8; hex 800000000000006a; asc        j;;
 4: len 8; hex 8000000000000452; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308e61d; asc    n    ;;
 7: len 8; hex 99bac36e8308e773; asc    n   s;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000453; asc        S;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0646; asc     { F;;
 3: len 8; hex 800000000000006b; asc        k;;
 4: len 8; hex 8000000000000453; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308ebb6; asc    n    ;;
 7: len 8; hex 99bac36e8308ed2b; asc    n   +;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000454; asc        T;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b06cb; asc     {  ;;
 3: len 8; hex 800000000000006c; asc        l;;
 4: len 8; hex 8000000000000454; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308f1b6; asc    n    ;;
 7: len 8; hex 99bac36e8308f325; asc    n   %;;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000455; asc        U;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0750; asc     { P;;
 3: len 8; hex 800000000000006d; asc        m;;
 4: len 8; hex 8000000000000455; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308f77f; asc    n    ;;
 7: len 8; hex 99bac36e8308f8da; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000456; asc        V;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b07d5; asc     {  ;;
 3: len 8; hex 800000000000006e; asc        n;;
 4: len 8; hex 8000000000000456; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8308fd5f; asc    n   _;;
 7: len 8; hex 99bac36e8308ff2d; asc    n   -;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000457; asc        W;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b085a; asc     { Z;;
 3: len 8; hex 800000000000006f; asc        o;;
 4: len 8; hex 8000000000000457; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83090446; asc    n   F;;
 7: len 8; hex 99bac36e83090651; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000458; asc        X;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b08df; asc     {  ;;
 3: len 8; hex 8000000000000070; asc        p;;
 4: len 8; hex 8000000000000458; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83090b27; asc    n   ';;
 7: len 8; hex 99bac36e83090c7e; asc    n   ~;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000459; asc        Y;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0964; asc     { d;;
 3: len 8; hex 8000000000000071; asc        q;;
 4: len 8; hex 8000000000000459; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830910e2; asc    n    ;;
 7: len 8; hex 99bac36e83091255; asc    n   U;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045a; asc        Z;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b09e9; asc     {  ;;
 3: len 8; hex 8000000000000072; asc        r;;
 4: len 8; hex 800000000000045a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830916a4; asc    n    ;;
 7: len 8; hex 99bac36e83091801; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045b; asc        [;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0a6e; asc     { n;;
 3: len 8; hex 8000000000000073; asc        s;;
 4: len 8; hex 800000000000045b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83091c68; asc    n   h;;
 7: len 8; hex 99bac36e83091db8; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045c; asc        \;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0af3; asc     {  ;;
 3: len 8; hex 8000000000000074; asc        t;;
 4: len 8; hex 800000000000045c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83092245; asc    n  "E;;
 7: len 8; hex 99bac36e83092440; asc    n  $@;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045d; asc        ];;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0b78; asc     { x;;
 3: len 8; hex 8000000000000075; asc        u;;
 4: len 8; hex 800000000000045d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830928e2; asc    n  ( ;;
 7: len 8; hex 99bac36e83095c49; asc    n  \I;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045e; asc        ^;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0bfd; asc     {  ;;
 3: len 8; hex 8000000000000076; asc        v;;
 4: len 8; hex 800000000000045e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309612f; asc    n  a/;;
 7: len 8; hex 99bac36e830962aa; asc    n  b ;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000045f; asc        _;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0c82; asc     {  ;;
 3: len 8; hex 8000000000000077; asc        w;;
 4: len 8; hex 800000000000045f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83096752; asc    n  gR;;
 7: len 8; hex 99bac36e830968bf; asc    n  h ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000460; asc        `;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0d07; asc     {  ;;
 3: len 8; hex 8000000000000078; asc        x;;
 4: len 8; hex 8000000000000460; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83096da5; asc    n  m ;;
 7: len 8; hex 99bac36e83096efe; asc    n  n ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000461; asc        a;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0d8c; asc     {  ;;
 3: len 8; hex 8000000000000079; asc        y;;
 4: len 8; hex 8000000000000461; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83097366; asc    n  sf;;
 7: len 8; hex 99bac36e830974f4; asc    n  t ;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000462; asc        b;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0e11; asc     {  ;;
 3: len 8; hex 800000000000007a; asc        z;;
 4: len 8; hex 8000000000000462; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83097a3c; asc    n  z<;;
 7: len 8; hex 99bac36e83097bf7; asc    n  { ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000463; asc        c;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0e96; asc     {  ;;
 3: len 8; hex 800000000000007b; asc        {;;
 4: len 8; hex 8000000000000463; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e830980b0; asc    n    ;;
 7: len 8; hex 99bac36e83098204; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000464; asc        d;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0f1b; asc     {  ;;
 3: len 8; hex 800000000000007c; asc        |;;
 4: len 8; hex 8000000000000464; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83098645; asc    n   E;;
 7: len 8; hex 99bac36e830987a6; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000465; asc        e;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b0fa0; asc     {  ;;
 3: len 8; hex 800000000000007d; asc        };;
 4: len 8; hex 8000000000000465; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83098be5; asc    n    ;;
 7: len 8; hex 99bac36e83098d51; asc    n   Q;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000466; asc        f;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b1025; asc     { %;;
 3: len 8; hex 800000000000007e; asc        ~;;
 4: len 8; hex 8000000000000466; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309918b; asc    n    ;;
 7: len 8; hex 99bac36e830992e1; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000467; asc        g;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b10aa; asc     {  ;;
 3: len 8; hex 800000000000007f; asc         ;;
 4: len 8; hex 8000000000000467; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309978f; asc    n    ;;
 7: len 8; hex 99bac36e83099906; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000468; asc        h;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b112f; asc     { /;;
 3: len 8; hex 8000000000000080; asc         ;;
 4: len 8; hex 8000000000000468; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e83099df1; asc    n    ;;
 7: len 8; hex 99bac36e83099f63; asc    n   c;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000469; asc        i;;
 1: len 6; hex 000000043a6e; asc     :n;;
 2: len 7; hex 020000017b11b4; asc     {  ;;
 3: len 8; hex 8000000000000081; asc         ;;
 4: len 8; hex 8000000000000469; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8309a40b; asc    n    ;;
 7: len 8; hex 99bac36e8309a5ef; asc    n    ;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000040f; asc         ;;
 1: len 6; hex 000000043b54; asc     ;T;;
 2: len 7; hex 01000001c811b3; asc        ;;
 3: len 8; hex 8000000000000027; asc        ';;
 4: len 8; hex 800000000000040f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac36e8306924c; asc    n   L;;
 7: len 8; hex 99bac36e8306939c; asc    n    ;;
 8: len 8; hex 99bac36e890dbc2b; asc    n   +;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 149 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 277352 lock mode S locks rec but not gap waiting
Record lock, heap no 224 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000043b8e; asc     ; ;;
 2: len 7; hex 020000013212e2; asc     2  ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343234363138303133; asc tk-424618013;;
 5: len 8; hex 99bac36e8b0218a5; asc    n    ;;
 6: len 8; hex 99bac36e8b0218a5; asc    n    ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```
