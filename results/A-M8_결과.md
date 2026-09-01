# A-M8 Deadlock Matrix — 측정 결과

토폴로지: 배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A)
**배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.**

고정값: 테넌트 5 · 계정 1000 · 발송요청 4000 · API지연 400us
MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s
배치: Spring Batch chunk-oriented step (chunk = 트랜잭션)

| # | 구성 | 배치 롤백 | 기록 건수 | 수신자 데드락 | 수신자 타임아웃 | 토큰갱신 실패 | 알림확인 실패 | InnoDB dl | 소요 |
|---|---|---|---|---|---|---|---|---|---|
| V0 | baseline (사건 재현)<br><sub>조회 TX내 / chunk 500 / 개별 UPDATE / 수신자 B->A(반대) / FK 있음 / REPEATABLE READ</sub> | 0 | 4000 | 48 | 0 | 0 | 48 | 48 | 12.7s |

> 실험 환경의 결과다. 운영 환경의 수치가 아니다.

---

## V0 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 16:05:33 135358810506816
*** (1) TRANSACTION:
TRANSACTION 40180, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 1100, OS thread handle 135358650984000, query id 401369 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 962

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 36 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 40180 lock_mode X locks rec but not gap
Record lock, heap no 211 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000009cf4; asc       ;;
 2: len 7; hex 01000001cb21b0; asc      ! ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d373831393637383632; asc tk-781967862;;
 5: len 8; hex 99bac3015f0982dc; asc     _   ;;
 6: len 8; hex 99bac3015f0982dc; asc     _   ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 34 page no 9 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 40180 lock_mode X locks rec but not gap waiting
Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51a89; asc        ;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000003c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157081368; asc     W  h;;
 7: len 8; hex 99bac301570816aa; asc     W   ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 40166, ACTIVE 2 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 64 lock struct(s), heap size 24696, 5368 row lock(s), undo log entries 1384
MySQL thread id 1203, OS thread handle 135358652040768, query id 402821 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (962, 3962, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 34 page no 9 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 40166 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2d3d; asc      -=;;
 3: len 8; hex 80000000000002f2; asc         ;;
 4: len 8; hex 80000000000002f2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560ec8b2; asc     V   ;;
 7: len 8; hex 99bac301560ecbfe; asc     V   ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2dc2; asc      - ;;
 3: len 8; hex 80000000000002f3; asc         ;;
 4: len 8; hex 80000000000002f3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560ed2b9; asc     V   ;;
 7: len 8; hex 99bac301560ed6e7; asc     V   ;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2e47; asc      .G;;
 3: len 8; hex 80000000000002f4; asc         ;;
 4: len 8; hex 80000000000002f4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560ee321; asc     V  !;;
 7: len 8; hex 99bac301560f171c; asc     V   ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f5; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2ecc; asc      . ;;
 3: len 8; hex 80000000000002f5; asc         ;;
 4: len 8; hex 80000000000002f5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560f1f4a; asc     V  J;;
 7: len 8; hex 99bac301560f247a; asc     V $z;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2f51; asc      /Q;;
 3: len 8; hex 80000000000002f6; asc         ;;
 4: len 8; hex 80000000000002f6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560f2ae3; asc     V * ;;
 7: len 8; hex 99bac301560f2ef0; asc     V . ;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db2fd6; asc      / ;;
 3: len 8; hex 80000000000002f7; asc         ;;
 4: len 8; hex 80000000000002f7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301560f3836; asc     V 86;;
 7: len 8; hex 99bac301560f3cbb; asc     V < ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db305b; asc      0[;;
 3: len 8; hex 80000000000002f8; asc         ;;
 4: len 8; hex 80000000000002f8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157000190; asc     W   ;;
 7: len 8; hex 99bac301570005a7; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002f9; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db30e0; asc      0 ;;
 3: len 8; hex 80000000000002f9; asc         ;;
 4: len 8; hex 80000000000002f9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157000d80; asc     W   ;;
 7: len 8; hex 99bac30157001145; asc     W  E;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fa; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3165; asc      1e;;
 3: len 8; hex 80000000000002fa; asc         ;;
 4: len 8; hex 80000000000002fa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570017d5; asc     W   ;;
 7: len 8; hex 99bac30157001c6c; asc     W  l;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fb; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db31ea; asc      1 ;;
 3: len 8; hex 80000000000002fb; asc         ;;
 4: len 8; hex 80000000000002fb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157002557; asc     W %W;;
 7: len 8; hex 99bac30157002988; asc     W ) ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fc; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db326f; asc      2o;;
 3: len 8; hex 80000000000002fc; asc         ;;
 4: len 8; hex 80000000000002fc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700321b; asc     W 2 ;;
 7: len 8; hex 99bac301570036aa; asc     W 6 ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fd; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db32f4; asc      2 ;;
 3: len 8; hex 80000000000002fd; asc         ;;
 4: len 8; hex 80000000000002fd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157003fe0; asc     W ? ;;
 7: len 8; hex 99bac301570043e1; asc     W C ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002fe; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3379; asc      3y;;
 3: len 8; hex 80000000000002fe; asc         ;;
 4: len 8; hex 80000000000002fe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157004c69; asc     W Li;;
 7: len 8; hex 99bac30157005143; asc     W QC;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000002ff; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db33fe; asc      3 ;;
 3: len 8; hex 80000000000002ff; asc         ;;
 4: len 8; hex 80000000000002ff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157005966; asc     W Yf;;
 7: len 8; hex 99bac30157005d4d; asc     W ]M;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000300; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3483; asc      4 ;;
 3: len 8; hex 8000000000000300; asc         ;;
 4: len 8; hex 8000000000000300; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700664e; asc     W fN;;
 7: len 8; hex 99bac30157006a6a; asc     W jj;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000301; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3508; asc      5 ;;
 3: len 8; hex 8000000000000301; asc         ;;
 4: len 8; hex 8000000000000301; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157007176; asc     W qv;;
 7: len 8; hex 99bac30157007521; asc     W u!;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000302; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db358d; asc      5 ;;
 3: len 8; hex 8000000000000302; asc         ;;
 4: len 8; hex 8000000000000302; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157007be2; asc     W { ;;
 7: len 8; hex 99bac3015700801e; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000303; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3612; asc      6 ;;
 3: len 8; hex 8000000000000303; asc         ;;
 4: len 8; hex 8000000000000303; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570084cb; asc     W   ;;
 7: len 8; hex 99bac30157008856; asc     W  V;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000304; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3697; asc      6 ;;
 3: len 8; hex 8000000000000304; asc         ;;
 4: len 8; hex 8000000000000304; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157008d37; asc     W  7;;
 7: len 8; hex 99bac3015700908d; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000305; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db371c; asc      7 ;;
 3: len 8; hex 8000000000000305; asc         ;;
 4: len 8; hex 8000000000000305; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570095c1; asc     W   ;;
 7: len 8; hex 99bac30157009925; asc     W  %;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000306; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db37a1; asc      7 ;;
 3: len 8; hex 8000000000000306; asc         ;;
 4: len 8; hex 8000000000000306; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157009d3e; asc     W  >;;
 7: len 8; hex 99bac3015700a017; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000307; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3826; asc      8&;;
 3: len 8; hex 8000000000000307; asc         ;;
 4: len 8; hex 8000000000000307; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700a444; asc     W  D;;
 7: len 8; hex 99bac3015700a706; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000308; asc         ;;
 1: len 6; hex 000000009ca0; asc       ;;
 2: len 7; hex 020000018a0193; asc        ;;
 3: len 8; hex 8000000000000308; asc         ;;
 4: len 8; hex 8000000000000308; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700ac75; asc     W  u;;
 7: len 8; hex 99bac3015700b0e6; asc     W   ;;
 8: len 8; hex 99bac3015d0a4904; asc     ] I ;;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000309; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3930; asc      90;;
 3: len 8; hex 8000000000000309; asc         ;;
 4: len 8; hex 8000000000000309; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700b80a; asc     W   ;;
 7: len 8; hex 99bac3015700bd2b; asc     W  +;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030a; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db39b5; asc      9 ;;
 3: len 8; hex 800000000000030a; asc         ;;
 4: len 8; hex 800000000000030a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700c268; asc     W  h;;
 7: len 8; hex 99bac3015700c550; asc     W  P;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030c; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3abf; asc      : ;;
 3: len 8; hex 800000000000030c; asc         ;;
 4: len 8; hex 800000000000030c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700d365; asc     W  e;;
 7: len 8; hex 99bac3015700d79f; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030d; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3b44; asc      ;D;;
 3: len 8; hex 800000000000030d; asc         ;;
 4: len 8; hex 800000000000030d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700df79; asc     W  y;;
 7: len 8; hex 99bac3015700e347; asc     W  G;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030e; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3bc9; asc      ; ;;
 3: len 8; hex 800000000000030e; asc         ;;
 4: len 8; hex 800000000000030e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700e95a; asc     W  Z;;
 7: len 8; hex 99bac3015700ed13; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030f; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3c4e; asc      <N;;
 3: len 8; hex 800000000000030f; asc         ;;
 4: len 8; hex 800000000000030f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700f391; asc     W   ;;
 7: len 8; hex 99bac3015700f7cb; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000310; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3cd3; asc      < ;;
 3: len 8; hex 8000000000000310; asc         ;;
 4: len 8; hex 8000000000000310; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157010104; asc     W   ;;
 7: len 8; hex 99bac3015701061d; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000311; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3d58; asc      =X;;
 3: len 8; hex 8000000000000311; asc         ;;
 4: len 8; hex 8000000000000311; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157010e80; asc     W   ;;
 7: len 8; hex 99bac3015701123f; asc     W  ?;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000312; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3ddd; asc      = ;;
 3: len 8; hex 8000000000000312; asc         ;;
 4: len 8; hex 8000000000000312; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570119d6; asc     W   ;;
 7: len 8; hex 99bac30157011e10; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000313; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3e62; asc      >b;;
 3: len 8; hex 8000000000000313; asc         ;;
 4: len 8; hex 8000000000000313; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157012610; asc     W & ;;
 7: len 8; hex 99bac3015701295d; asc     W )];;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000314; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3ee7; asc      > ;;
 3: len 8; hex 8000000000000314; asc         ;;
 4: len 8; hex 8000000000000314; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157012db8; asc     W - ;;
 7: len 8; hex 99bac301570130bd; asc     W 0 ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000315; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000db3f6c; asc      ?l;;
 3: len 8; hex 8000000000000315; asc         ;;
 4: len 8; hex 8000000000000315; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570134af; asc     W 4 ;;
 7: len 8; hex 99bac30157013791; asc     W 7 ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000316; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3008f; asc        ;;
 3: len 8; hex 8000000000000316; asc         ;;
 4: len 8; hex 8000000000000316; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157013bc2; asc     W ; ;;
 7: len 8; hex 99bac30157013e8f; asc     W > ;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000317; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30114; asc        ;;
 3: len 8; hex 8000000000000317; asc         ;;
 4: len 8; hex 8000000000000317; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570149a0; asc     W I ;;
 7: len 8; hex 99bac30157014d4a; asc     W MJ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000318; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30199; asc        ;;
 3: len 8; hex 8000000000000318; asc         ;;
 4: len 8; hex 8000000000000318; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570155a8; asc     W U ;;
 7: len 8; hex 99bac30157015903; asc     W Y ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000319; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3021e; asc        ;;
 3: len 8; hex 8000000000000319; asc         ;;
 4: len 8; hex 8000000000000319; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157015d49; asc     W ]I;;
 7: len 8; hex 99bac30157016054; asc     W `T;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031a; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e302a3; asc        ;;
 3: len 8; hex 800000000000031a; asc         ;;
 4: len 8; hex 800000000000031a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157016482; asc     W d ;;
 7: len 8; hex 99bac30157016f9b; asc     W o ;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031b; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30328; asc       (;;
 3: len 8; hex 800000000000031b; asc         ;;
 4: len 8; hex 800000000000031b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701794d; asc     W yM;;
 7: len 8; hex 99bac30157017dfc; asc     W } ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031c; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e303ad; asc        ;;
 3: len 8; hex 800000000000031c; asc         ;;
 4: len 8; hex 800000000000031c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570184b9; asc     W   ;;
 7: len 8; hex 99bac3015701882e; asc     W  .;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031d; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30432; asc       2;;
 3: len 8; hex 800000000000031d; asc         ;;
 4: len 8; hex 800000000000031d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157018db1; asc     W   ;;
 7: len 8; hex 99bac30157019125; asc     W  %;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031e; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e304b7; asc        ;;
 3: len 8; hex 800000000000031e; asc         ;;
 4: len 8; hex 800000000000031e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570198bb; asc     W   ;;
 7: len 8; hex 99bac30157019d86; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000031f; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3053c; asc       <;;
 3: len 8; hex 800000000000031f; asc         ;;
 4: len 8; hex 800000000000031f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701a3f4; asc     W   ;;
 7: len 8; hex 99bac3015701a70f; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000320; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e305c1; asc        ;;
 3: len 8; hex 8000000000000320; asc         ;;
 4: len 8; hex 8000000000000320; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701ac3a; asc     W  :;;
 7: len 8; hex 99bac3015701af34; asc     W  4;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000321; asc        !;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30646; asc       F;;
 3: len 8; hex 8000000000000321; asc        !;;
 4: len 8; hex 8000000000000321; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701b831; asc     W  1;;
 7: len 8; hex 99bac3015701bd29; asc     W  );;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000322; asc        ";;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e306cb; asc        ;;
 3: len 8; hex 8000000000000322; asc        ";;
 4: len 8; hex 8000000000000322; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701c1a8; asc     W   ;;
 7: len 8; hex 99bac3015701c4a3; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000323; asc        #;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30750; asc       P;;
 3: len 8; hex 8000000000000323; asc        #;;
 4: len 8; hex 8000000000000323; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701c8d6; asc     W   ;;
 7: len 8; hex 99bac3015701cbe9; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000324; asc        $;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e307d5; asc        ;;
 3: len 8; hex 8000000000000324; asc        $;;
 4: len 8; hex 8000000000000324; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701d5e5; asc     W   ;;
 7: len 8; hex 99bac3015701da08; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000325; asc        %;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3085a; asc       Z;;
 3: len 8; hex 8000000000000325; asc        %;;
 4: len 8; hex 8000000000000325; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701de82; asc     W   ;;
 7: len 8; hex 99bac3015701e25b; asc     W  [;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000326; asc        &;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e308df; asc        ;;
 3: len 8; hex 8000000000000326; asc        &;;
 4: len 8; hex 8000000000000326; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701e93e; asc     W  >;;
 7: len 8; hex 99bac3015701ecb3; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000327; asc        ';;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30964; asc       d;;
 3: len 8; hex 8000000000000327; asc        ';;
 4: len 8; hex 8000000000000327; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701f11f; asc     W   ;;
 7: len 8; hex 99bac3015701f3ff; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000328; asc        (;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e309e9; asc        ;;
 3: len 8; hex 8000000000000328; asc        (;;
 4: len 8; hex 8000000000000328; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701f862; asc     W  b;;
 7: len 8; hex 99bac3015701fb7c; asc     W  |;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000329; asc        );;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30a6e; asc       n;;
 3: len 8; hex 8000000000000329; asc        );;
 4: len 8; hex 8000000000000329; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015701ff99; asc     W   ;;
 7: len 8; hex 99bac3015702027b; asc     W  {;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032a; asc        *;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30af3; asc        ;;
 3: len 8; hex 800000000000032a; asc        *;;
 4: len 8; hex 800000000000032a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157020798; asc     W   ;;
 7: len 8; hex 99bac30157020af9; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032b; asc        +;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30b78; asc       x;;
 3: len 8; hex 800000000000032b; asc        +;;
 4: len 8; hex 800000000000032b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702109d; asc     W   ;;
 7: len 8; hex 99bac301570213e6; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032c; asc        ,;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30bfd; asc        ;;
 3: len 8; hex 800000000000032c; asc        ,;;
 4: len 8; hex 800000000000032c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570218dd; asc     W   ;;
 7: len 8; hex 99bac30157021bfe; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032d; asc        -;;
 1: len 6; hex 000000009bfa; asc       ;;
 2: len 7; hex 010000010028a3; asc      ( ;;
 3: len 8; hex 800000000000032d; asc        -;;
 4: len 8; hex 800000000000032d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157022084; asc     W   ;;
 7: len 8; hex 99bac301570223a3; asc     W # ;;
 8: len 8; hex 99bac301570a3916; asc     W 9 ;;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032e; asc        .;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30d07; asc        ;;
 3: len 8; hex 800000000000032e; asc        .;;
 4: len 8; hex 800000000000032e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570228c1; asc     W ( ;;
 7: len 8; hex 99bac30157022bce; asc     W + ;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000032f; asc        /;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30d8c; asc        ;;
 3: len 8; hex 800000000000032f; asc        /;;
 4: len 8; hex 800000000000032f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570230a8; asc     W 0 ;;
 7: len 8; hex 99bac3015702346a; asc     W 4j;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000330; asc        0;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30e11; asc        ;;
 3: len 8; hex 8000000000000330; asc        0;;
 4: len 8; hex 8000000000000330; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157023912; asc     W 9 ;;
 7: len 8; hex 99bac30157023c38; asc     W <8;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000331; asc        1;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30e96; asc        ;;
 3: len 8; hex 8000000000000331; asc        1;;
 4: len 8; hex 8000000000000331; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570240f0; asc     W @ ;;
 7: len 8; hex 99bac301570243d5; asc     W C ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000332; asc        2;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30f1b; asc        ;;
 3: len 8; hex 8000000000000332; asc        2;;
 4: len 8; hex 8000000000000332; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570247e5; asc     W G ;;
 7: len 8; hex 99bac30157024ac5; asc     W J ;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000333; asc        3;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e30fa0; asc        ;;
 3: len 8; hex 8000000000000333; asc        3;;
 4: len 8; hex 8000000000000333; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157025077; asc     W Pw;;
 7: len 8; hex 99bac301570253e1; asc     W S ;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000334; asc        4;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31025; asc       %;;
 3: len 8; hex 8000000000000334; asc        4;;
 4: len 8; hex 8000000000000334; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570258bc; asc     W X ;;
 7: len 8; hex 99bac30157025bda; asc     W [ ;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000335; asc        5;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e310aa; asc        ;;
 3: len 8; hex 8000000000000335; asc        5;;
 4: len 8; hex 8000000000000335; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157026034; asc     W `4;;
 7: len 8; hex 99bac30157026320; asc     W c ;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000336; asc        6;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3112f; asc       /;;
 3: len 8; hex 8000000000000336; asc        6;;
 4: len 8; hex 8000000000000336; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157026795; asc     W g ;;
 7: len 8; hex 99bac30157026a8a; asc     W j ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000337; asc        7;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e311b4; asc        ;;
 3: len 8; hex 8000000000000337; asc        7;;
 4: len 8; hex 8000000000000337; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157026f10; asc     W o ;;
 7: len 8; hex 99bac3015702724f; asc     W rO;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000338; asc        8;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31239; asc       9;;
 3: len 8; hex 8000000000000338; asc        8;;
 4: len 8; hex 8000000000000338; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157027738; asc     W w8;;
 7: len 8; hex 99bac30157027a6b; asc     W zk;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000339; asc        9;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e312be; asc        ;;
 3: len 8; hex 8000000000000339; asc        9;;
 4: len 8; hex 8000000000000339; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157027ef8; asc     W ~ ;;
 7: len 8; hex 99bac301570281e1; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033a; asc        :;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31343; asc       C;;
 3: len 8; hex 800000000000033a; asc        :;;
 4: len 8; hex 800000000000033a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702861e; asc     W   ;;
 7: len 8; hex 99bac30157028919; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033b; asc        ;;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e313c8; asc        ;;
 3: len 8; hex 800000000000033b; asc        ;;;
 4: len 8; hex 800000000000033b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157028d31; asc     W  1;;
 7: len 8; hex 99bac30157029013; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033c; asc        <;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3144d; asc       M;;
 3: len 8; hex 800000000000033c; asc        <;;
 4: len 8; hex 800000000000033c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702959a; asc     W   ;;
 7: len 8; hex 99bac301570298e2; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033d; asc        =;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e314d2; asc        ;;
 3: len 8; hex 800000000000033d; asc        =;;
 4: len 8; hex 800000000000033d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157029d20; asc     W   ;;
 7: len 8; hex 99bac3015702a010; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033e; asc        >;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31557; asc       W;;
 3: len 8; hex 800000000000033e; asc        >;;
 4: len 8; hex 800000000000033e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702a47e; asc     W  ~;;
 7: len 8; hex 99bac3015702a789; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000033f; asc        ?;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e315dc; asc        ;;
 3: len 8; hex 800000000000033f; asc        ?;;
 4: len 8; hex 800000000000033f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702acc3; asc     W   ;;
 7: len 8; hex 99bac3015702b040; asc     W  @;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000340; asc        @;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31661; asc       a;;
 3: len 8; hex 8000000000000340; asc        @;;
 4: len 8; hex 8000000000000340; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702b530; asc     W  0;;
 7: len 8; hex 99bac3015702b850; asc     W  P;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000341; asc        A;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e316e6; asc        ;;
 3: len 8; hex 8000000000000341; asc        A;;
 4: len 8; hex 8000000000000341; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702bc7d; asc     W  };;
 7: len 8; hex 99bac3015702bf6c; asc     W  l;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000342; asc        B;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3176b; asc       k;;
 3: len 8; hex 8000000000000342; asc        B;;
 4: len 8; hex 8000000000000342; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702c34a; asc     W  J;;
 7: len 8; hex 99bac3015702c639; asc     W  9;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000343; asc        C;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e317f0; asc        ;;
 3: len 8; hex 8000000000000343; asc        C;;
 4: len 8; hex 8000000000000343; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702cf32; asc     W  2;;
 7: len 8; hex 99bac3015702d39c; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000344; asc        D;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31875; asc       u;;
 3: len 8; hex 8000000000000344; asc        D;;
 4: len 8; hex 8000000000000344; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702d802; asc     W   ;;
 7: len 8; hex 99bac3015702dacf; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000345; asc        E;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e318fa; asc        ;;
 3: len 8; hex 8000000000000345; asc        E;;
 4: len 8; hex 8000000000000345; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702df30; asc     W  0;;
 7: len 8; hex 99bac3015702e26d; asc     W  m;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000346; asc        F;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3197f; asc        ;;
 3: len 8; hex 8000000000000346; asc        F;;
 4: len 8; hex 8000000000000346; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702e6f1; asc     W   ;;
 7: len 8; hex 99bac3015702ea67; asc     W  g;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000347; asc        G;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31a04; asc        ;;
 3: len 8; hex 8000000000000347; asc        G;;
 4: len 8; hex 8000000000000347; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702f01e; asc     W   ;;
 7: len 8; hex 99bac3015702f37b; asc     W  {;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000348; asc        H;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31a89; asc        ;;
 3: len 8; hex 8000000000000348; asc        H;;
 4: len 8; hex 8000000000000348; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015702f882; asc     W   ;;
 7: len 8; hex 99bac3015702fba0; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000349; asc        I;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31b0e; asc        ;;
 3: len 8; hex 8000000000000349; asc        I;;
 4: len 8; hex 8000000000000349; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703024b; asc     W  K;;
 7: len 8; hex 99bac30157030533; asc     W  3;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034a; asc        J;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31b93; asc        ;;
 3: len 8; hex 800000000000034a; asc        J;;
 4: len 8; hex 800000000000034a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157030946; asc     W  F;;
 7: len 8; hex 99bac30157030c2b; asc     W  +;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034b; asc        K;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31c18; asc        ;;
 3: len 8; hex 800000000000034b; asc        K;;
 4: len 8; hex 800000000000034b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157031060; asc     W  `;;
 7: len 8; hex 99bac3015703144f; asc     W  O;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034c; asc        L;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31c9d; asc        ;;
 3: len 8; hex 800000000000034c; asc        L;;
 4: len 8; hex 800000000000034c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157031a07; asc     W   ;;
 7: len 8; hex 99bac30157031d69; asc     W  i;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034d; asc        M;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31d22; asc       ";;
 3: len 8; hex 800000000000034d; asc        M;;
 4: len 8; hex 800000000000034d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157032350; asc     W #P;;
 7: len 8; hex 99bac3015703271c; asc     W ' ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034e; asc        N;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31da7; asc        ;;
 3: len 8; hex 800000000000034e; asc        N;;
 4: len 8; hex 800000000000034e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157033212; asc     W 2 ;;
 7: len 8; hex 99bac30157033544; asc     W 5D;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000034f; asc        O;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31e2c; asc       ,;;
 3: len 8; hex 800000000000034f; asc        O;;
 4: len 8; hex 800000000000034f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157033a33; asc     W :3;;
 7: len 8; hex 99bac30157033d65; asc     W =e;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000350; asc        P;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31eb1; asc        ;;
 3: len 8; hex 8000000000000350; asc        P;;
 4: len 8; hex 8000000000000350; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703421f; asc     W B ;;
 7: len 8; hex 99bac30157034516; asc     W E ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000351; asc        Q;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31f36; asc       6;;
 3: len 8; hex 8000000000000351; asc        Q;;
 4: len 8; hex 8000000000000351; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157034925; asc     W I%;;
 7: len 8; hex 99bac30157034bf3; asc     W K ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000352; asc        R;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e31fbb; asc        ;;
 3: len 8; hex 8000000000000352; asc        R;;
 4: len 8; hex 8000000000000352; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157035010; asc     W P ;;
 7: len 8; hex 99bac3015703532b; asc     W S+;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000353; asc        S;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32040; asc       @;;
 3: len 8; hex 8000000000000353; asc        S;;
 4: len 8; hex 8000000000000353; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703583b; asc     W X;;;
 7: len 8; hex 99bac30157035b2f; asc     W [/;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000354; asc        T;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e320c5; asc        ;;
 3: len 8; hex 8000000000000354; asc        T;;
 4: len 8; hex 8000000000000354; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157035fa7; asc     W _ ;;
 7: len 8; hex 99bac30157036374; asc     W ct;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000355; asc        U;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3214a; asc      !J;;
 3: len 8; hex 8000000000000355; asc        U;;
 4: len 8; hex 8000000000000355; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157036912; asc     W i ;;
 7: len 8; hex 99bac30157036c48; asc     W lH;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000356; asc        V;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e321cf; asc      ! ;;
 3: len 8; hex 8000000000000356; asc        V;;
 4: len 8; hex 8000000000000356; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570370d4; asc     W p ;;
 7: len 8; hex 99bac301570373d2; asc     W s ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000357; asc        W;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32254; asc      "T;;
 3: len 8; hex 8000000000000357; asc        W;;
 4: len 8; hex 8000000000000357; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570377c6; asc     W w ;;
 7: len 8; hex 99bac30157037adb; asc     W z ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000358; asc        X;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e322d9; asc      " ;;
 3: len 8; hex 8000000000000358; asc        X;;
 4: len 8; hex 8000000000000358; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157037f02; asc     W   ;;
 7: len 8; hex 99bac30157038231; asc     W  1;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000359; asc        Y;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3235e; asc      #^;;
 3: len 8; hex 8000000000000359; asc        Y;;
 4: len 8; hex 8000000000000359; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157038681; asc     W   ;;
 7: len 8; hex 99bac30157038a6d; asc     W  m;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035a; asc        Z;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e323e3; asc      # ;;
 3: len 8; hex 800000000000035a; asc        Z;;
 4: len 8; hex 800000000000035a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570392c4; asc     W   ;;
 7: len 8; hex 99bac30157039602; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035b; asc        [;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32468; asc      $h;;
 3: len 8; hex 800000000000035b; asc        [;;
 4: len 8; hex 800000000000035b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157039acd; asc     W   ;;
 7: len 8; hex 99bac30157039df4; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035c; asc        \;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e324ed; asc      $ ;;
 3: len 8; hex 800000000000035c; asc        \;;
 4: len 8; hex 800000000000035c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703a290; asc     W   ;;
 7: len 8; hex 99bac3015703a586; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035d; asc        ];;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32572; asc      %r;;
 3: len 8; hex 800000000000035d; asc        ];;
 4: len 8; hex 800000000000035d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703aa30; asc     W  0;;
 7: len 8; hex 99bac3015703ad3f; asc     W  ?;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035e; asc        ^;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e325f7; asc      % ;;
 3: len 8; hex 800000000000035e; asc        ^;;
 4: len 8; hex 800000000000035e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703b1dc; asc     W   ;;
 7: len 8; hex 99bac3015703b50c; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000035f; asc        _;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3267c; asc      &|;;
 3: len 8; hex 800000000000035f; asc        _;;
 4: len 8; hex 800000000000035f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703b98d; asc     W   ;;
 7: len 8; hex 99bac3015703bca2; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000360; asc        `;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32701; asc      ' ;;
 3: len 8; hex 8000000000000360; asc        `;;
 4: len 8; hex 8000000000000360; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703c14a; asc     W  J;;
 7: len 8; hex 99bac3015703c453; asc     W  S;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000361; asc        a;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32786; asc      ' ;;
 3: len 8; hex 8000000000000361; asc        a;;
 4: len 8; hex 8000000000000361; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703c8d6; asc     W   ;;
 7: len 8; hex 99bac3015703cbd0; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000362; asc        b;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3280b; asc      ( ;;
 3: len 8; hex 8000000000000362; asc        b;;
 4: len 8; hex 8000000000000362; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703d053; asc     W  S;;
 7: len 8; hex 99bac3015703d344; asc     W  D;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000363; asc        c;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32890; asc      ( ;;
 3: len 8; hex 8000000000000363; asc        c;;
 4: len 8; hex 8000000000000363; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703dc17; asc     W   ;;
 7: len 8; hex 99bac3015703e0d5; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000364; asc        d;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32915; asc      ) ;;
 3: len 8; hex 8000000000000364; asc        d;;
 4: len 8; hex 8000000000000364; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703ec33; asc     W  3;;
 7: len 8; hex 99bac3015703f105; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000365; asc        e;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3299a; asc      ) ;;
 3: len 8; hex 8000000000000365; asc        e;;
 4: len 8; hex 8000000000000365; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015703fa0a; asc     W   ;;
 7: len 8; hex 99bac3015703fd91; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000366; asc        f;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32a1f; asc      * ;;
 3: len 8; hex 8000000000000366; asc        f;;
 4: len 8; hex 8000000000000366; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157040272; asc     W  r;;
 7: len 8; hex 99bac3015704056e; asc     W  n;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000367; asc        g;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32aa4; asc      * ;;
 3: len 8; hex 8000000000000367; asc        g;;
 4: len 8; hex 8000000000000367; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157040a32; asc     W  2;;
 7: len 8; hex 99bac30157040d5d; asc     W  ];;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000368; asc        h;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32b29; asc      +);;
 3: len 8; hex 8000000000000368; asc        h;;
 4: len 8; hex 8000000000000368; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570411d6; asc     W   ;;
 7: len 8; hex 99bac30157041542; asc     W  B;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000369; asc        i;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32bae; asc      + ;;
 3: len 8; hex 8000000000000369; asc        i;;
 4: len 8; hex 8000000000000369; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157041de8; asc     W   ;;
 7: len 8; hex 99bac3015704238c; asc     W # ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036a; asc        j;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32c33; asc      ,3;;
 3: len 8; hex 800000000000036a; asc        j;;
 4: len 8; hex 800000000000036a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570428db; asc     W ( ;;
 7: len 8; hex 99bac30157042be4; asc     W + ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036b; asc        k;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32cb8; asc      , ;;
 3: len 8; hex 800000000000036b; asc        k;;
 4: len 8; hex 800000000000036b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157043083; asc     W 0 ;;
 7: len 8; hex 99bac30157043389; asc     W 3 ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036c; asc        l;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32d3d; asc      -=;;
 3: len 8; hex 800000000000036c; asc        l;;
 4: len 8; hex 800000000000036c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570437f6; asc     W 7 ;;
 7: len 8; hex 99bac30157043ae7; asc     W : ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036d; asc        m;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32dc2; asc      - ;;
 3: len 8; hex 800000000000036d; asc        m;;
 4: len 8; hex 800000000000036d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157043f92; asc     W ? ;;
 7: len 8; hex 99bac3015704429e; asc     W B ;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036e; asc        n;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32e47; asc      .G;;
 3: len 8; hex 800000000000036e; asc        n;;
 4: len 8; hex 800000000000036e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157044729; asc     W G);;
 7: len 8; hex 99bac30157044a1e; asc     W J ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000036f; asc        o;;
 1: len 6; hex 000000009c12; asc       ;;
 2: len 7; hex 010000012c2051; asc     , Q;;
 3: len 8; hex 800000000000036f; asc        o;;
 4: len 8; hex 800000000000036f; asc        o;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157044ec4; asc     W N ;;
 7: len 8; hex 99bac30157045292; asc     W R ;;
 8: len 8; hex 99bac301580cd629; asc     X  );;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000370; asc        p;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32f51; asc      /Q;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000370; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704576b; asc     W Wk;;
 7: len 8; hex 99bac30157045cc3; asc     W \ ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000371; asc        q;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e32fd6; asc      / ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000371; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570464dc; asc     W d ;;
 7: len 8; hex 99bac30157046907; asc     W i ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000372; asc        r;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3305b; asc      0[;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 8000000000000372; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704746d; asc     W tm;;
 7: len 8; hex 99bac301570478b9; asc     W x ;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000373; asc        s;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e330e0; asc      0 ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 8000000000000373; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570482c5; asc     W   ;;
 7: len 8; hex 99bac3015704871a; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000374; asc        t;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33165; asc      1e;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 8000000000000374; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570491a5; asc     W   ;;
 7: len 8; hex 99bac30157049545; asc     W  E;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000375; asc        u;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e331ea; asc      1 ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 8000000000000375; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157049b37; asc     W  7;;
 7: len 8; hex 99bac30157049f3d; asc     W  =;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000376; asc        v;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3326f; asc      2o;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 8000000000000376; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704ab6a; asc     W  j;;
 7: len 8; hex 99bac3015704af6d; asc     W  m;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000377; asc        w;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e332f4; asc      2 ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 8000000000000377; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704b532; asc     W  2;;
 7: len 8; hex 99bac3015704b863; asc     W  c;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000378; asc        x;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33379; asc      3y;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000378; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704bca1; asc     W   ;;
 7: len 8; hex 99bac3015704bf98; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000379; asc        y;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e333fe; asc      3 ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000379; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704c4af; asc     W   ;;
 7: len 8; hex 99bac3015704c813; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037a; asc        z;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33483; asc      4 ;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 800000000000037a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704cedb; asc     W   ;;
 7: len 8; hex 99bac3015704d231; asc     W  1;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037b; asc        {;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33508; asc      5 ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 800000000000037b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704d754; asc     W  T;;
 7: len 8; hex 99bac3015704da73; asc     W  s;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037c; asc        |;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3358d; asc      5 ;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 800000000000037c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704e044; asc     W  D;;
 7: len 8; hex 99bac3015704e379; asc     W  y;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037d; asc        };;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33612; asc      6 ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 800000000000037d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704e891; asc     W   ;;
 7: len 8; hex 99bac3015704ebc8; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037e; asc        ~;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33697; asc      6 ;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 800000000000037e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704f083; asc     W   ;;
 7: len 8; hex 99bac3015704f3a8; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037f; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e3371c; asc      7 ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 800000000000037f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015704f897; asc     W   ;;
 7: len 8; hex 99bac3015704fbab; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000380; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e337a1; asc      7 ;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000380; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157050070; asc     W  p;;
 7: len 8; hex 99bac30157050381; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000381; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33826; asc      8&;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000381; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570508cb; asc     W   ;;
 7: len 8; hex 99bac30157050c25; asc     W  %;;
 8: SQL NULL;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000382; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e338ab; asc      8 ;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 8000000000000382; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157051130; asc     W  0;;
 7: len 8; hex 99bac30157051445; asc     W  E;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000383; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33930; asc      90;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 8000000000000383; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705191d; asc     W   ;;
 7: len 8; hex 99bac30157051c25; asc     W  %;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000384; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e339b5; asc      9 ;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 8000000000000384; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157052149; asc     W !I;;
 7: len 8; hex 99bac3015705249e; asc     W $ ;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000385; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33a3a; asc      ::;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 8000000000000385; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570529b0; asc     W ) ;;
 7: len 8; hex 99bac30157052dd0; asc     W - ;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000386; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33abf; asc      : ;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 8000000000000386; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157053b9a; asc     W ; ;;
 7: len 8; hex 99bac30157053fe7; asc     W ? ;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000387; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33b44; asc      ;D;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 8000000000000387; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705488c; asc     W H ;;
 7: len 8; hex 99bac30157054bec; asc     W K ;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000388; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33bc9; asc      ; ;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000388; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570554b7; asc     W T ;;
 7: len 8; hex 99bac301570558da; asc     W X ;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000389; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33c4e; asc      <N;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000389; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157056855; asc     W hU;;
 7: len 8; hex 99bac30157056d19; asc     W m ;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038a; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33cd3; asc      < ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 800000000000038a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570574cb; asc     W t ;;
 7: len 8; hex 99bac3015705784e; asc     W xN;;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038b; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33d58; asc      =X;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 800000000000038b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157057fc4; asc     W   ;;
 7: len 8; hex 99bac30157058517; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038c; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33ddd; asc      = ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 800000000000038c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157058c26; asc     W  &;;
 7: len 8; hex 99bac3015705905b; asc     W  [;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038d; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33e62; asc      >b;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 800000000000038d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705a626; asc     W  &;;
 7: len 8; hex 99bac3015705aa4f; asc     W  O;;
 8: SQL NULL;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038e; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33ee7; asc      > ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 800000000000038e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705b3b0; asc     W   ;;
 7: len 8; hex 99bac3015705b7ec; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038f; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e33f6c; asc      ?l;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 800000000000038f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705be89; asc     W   ;;
 7: len 8; hex 99bac3015705c2b9; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000390; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5008f; asc        ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000390; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705cc5a; asc     W  Z;;
 7: len 8; hex 99bac3015705cfb2; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000391; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50114; asc        ;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000391; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705d733; asc     W  3;;
 7: len 8; hex 99bac3015705dbde; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000392; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50199; asc        ;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 8000000000000392; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705e486; asc     W   ;;
 7: len 8; hex 99bac3015705e88b; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000393; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5021e; asc        ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 8000000000000393; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705f165; asc     W  e;;
 7: len 8; hex 99bac3015705f651; asc     W  Q;;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000394; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e502a3; asc        ;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 8000000000000394; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015705ff34; asc     W  4;;
 7: len 8; hex 99bac30157060340; asc     W  @;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000395; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50328; asc       (;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 8000000000000395; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157060c0e; asc     W   ;;
 7: len 8; hex 99bac30157061040; asc     W  @;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000396; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e503ad; asc        ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 8000000000000396; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570618c4; asc     W   ;;
 7: len 8; hex 99bac30157061d75; asc     W  u;;
 8: SQL NULL;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000397; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50432; asc       2;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 8000000000000397; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157062592; asc     W % ;;
 7: len 8; hex 99bac30157062995; asc     W ) ;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000398; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e504b7; asc        ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000398; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157063095; asc     W 0 ;;
 7: len 8; hex 99bac30157063507; asc     W 5 ;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000399; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5053c; asc       <;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000399; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157063978; asc     W 9x;;
 7: len 8; hex 99bac30157063cb7; asc     W < ;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039a; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e505c1; asc        ;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 800000000000039a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570642e0; asc     W B ;;
 7: len 8; hex 99bac30157064711; asc     W G ;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039b; asc         ;;
 1: len 6; hex 000000009c6c; asc      l;;
 2: len 7; hex 02000001382146; asc     8!F;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 800000000000039b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157064d81; asc     W M ;;
 7: len 8; hex 99bac301570650d9; asc     W P ;;
 8: len 8; hex 99bac3015b0a6a9a; asc     [ j ;;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039c; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e506cb; asc        ;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 800000000000039c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570657aa; asc     W W ;;
 7: len 8; hex 99bac30157065add; asc     W Z ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039d; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50750; asc       P;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 800000000000039d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570661fd; asc     W a ;;
 7: len 8; hex 99bac301570665e3; asc     W e ;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039e; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e507d5; asc        ;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 800000000000039e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157066b3c; asc     W k<;;
 7: len 8; hex 99bac30157066eaf; asc     W n ;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5085a; asc       Z;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 800000000000039f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157067491; asc     W t ;;
 7: len 8; hex 99bac30157067838; asc     W x8;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a0; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e508df; asc        ;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 80000000000003a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570681c4; asc     W   ;;
 7: len 8; hex 99bac30157068610; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a1; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50964; asc       d;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 80000000000003a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157068dd5; asc     W   ;;
 7: len 8; hex 99bac30157069152; asc     W  R;;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e509e9; asc        ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 80000000000003a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157069b86; asc     W   ;;
 7: len 8; hex 99bac30157069fb7; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50a6e; asc       n;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 80000000000003a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706ada2; asc     W   ;;
 7: len 8; hex 99bac3015706b163; asc     W  c;;
 8: SQL NULL;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50af3; asc        ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 80000000000003a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706b9e5; asc     W   ;;
 7: len 8; hex 99bac3015706c57a; asc     W  z;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a5; asc         ;;
 1: len 6; hex 000000009c5b; asc      [;;
 2: len 7; hex 010000011524c8; asc      $ ;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 80000000000003a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706cc4b; asc     W  K;;
 7: len 8; hex 99bac3015706cfc7; asc     W   ;;
 8: len 8; hex 99bac3015a0870ba; asc     Z p ;;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50bfd; asc        ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 80000000000003a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706d629; asc     W  );;
 7: len 8; hex 99bac3015706da4c; asc     W  L;;
 8: SQL NULL;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50c82; asc        ;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 80000000000003a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706e1b2; asc     W   ;;
 7: len 8; hex 99bac3015706e58d; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50d07; asc        ;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 80000000000003a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706eebb; asc     W   ;;
 7: len 8; hex 99bac3015706f29f; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a9; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50d8c; asc        ;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 80000000000003a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015706fbf1; asc     W   ;;
 7: len 8; hex 99bac3015706ff65; asc     W  e;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003aa; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50e11; asc        ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 80000000000003aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157070882; asc     W   ;;
 7: len 8; hex 99bac30157070cf4; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ab; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50e96; asc        ;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 80000000000003ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707152a; asc     W  *;;
 7: len 8; hex 99bac301570719a8; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ac; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50f1b; asc        ;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 80000000000003ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570720d6; asc     W   ;;
 7: len 8; hex 99bac301570725e3; asc     W % ;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ad; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e50fa0; asc        ;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 80000000000003ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157072e5c; asc     W .\;;
 7: len 8; hex 99bac301570731d1; asc     W 1 ;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ae; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51025; asc       %;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 80000000000003ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157073a65; asc     W :e;;
 7: len 8; hex 99bac30157073eb9; asc     W > ;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003af; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e510aa; asc        ;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 80000000000003af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157074cf4; asc     W L ;;
 7: len 8; hex 99bac30157075265; asc     W Re;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b0; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5112f; asc       /;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 80000000000003b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157075851; asc     W XQ;;
 7: len 8; hex 99bac30157075b93; asc     W [ ;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b1; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e511b4; asc        ;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 80000000000003b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570764f4; asc     W d ;;
 7: len 8; hex 99bac30157076971; asc     W iq;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51239; asc       9;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 80000000000003b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707746e; asc     W tn;;
 7: len 8; hex 99bac3015707789a; asc     W x ;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e512be; asc        ;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 80000000000003b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157077f70; asc     W  p;;
 7: len 8; hex 99bac301570782f6; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51343; asc       C;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 80000000000003b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157078d22; asc     W  ";;
 7: len 8; hex 99bac30157079151; asc     W  Q;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b5; asc         ;;
 1: len 6; hex 000000009c59; asc      Y;;
 2: len 7; hex 01000001142397; asc      # ;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 80000000000003b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707996b; asc     W  k;;
 7: len 8; hex 99bac30157079e62; asc     W  b;;
 8: len 8; hex 99bac3015a086def; asc     Z m ;;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5144d; asc       M;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 80000000000003b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707a8c1; asc     W   ;;
 7: len 8; hex 99bac3015707ad4c; asc     W  L;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e514d2; asc        ;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 80000000000003b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707b3cf; asc     W   ;;
 7: len 8; hex 99bac3015707b823; asc     W  #;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51557; asc       W;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000003b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707be01; asc     W   ;;
 7: len 8; hex 99bac3015707c1a6; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b9; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e515dc; asc        ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000003b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707c767; asc     W  g;;
 7: len 8; hex 99bac3015707ca8b; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ba; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51661; asc       a;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000003ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707cf51; asc     W  Q;;
 7: len 8; hex 99bac3015707d3b5; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bb; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e516e6; asc        ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000003bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707d863; asc     W  c;;
 7: len 8; hex 99bac3015707db74; asc     W  t;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bc; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5176b; asc       k;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000003bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707e089; asc     W   ;;
 7: len 8; hex 99bac3015707e416; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bd; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e517f0; asc        ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000003bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707ebde; asc     W   ;;
 7: len 8; hex 99bac3015707ef30; asc     W  0;;
 8: SQL NULL;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003be; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51875; asc       u;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000003be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707f431; asc     W  1;;
 7: len 8; hex 99bac3015707f73d; asc     W  =;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bf; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e518fa; asc        ;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000003bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015707fc04; asc     W   ;;
 7: len 8; hex 99bac3015707fef9; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c0; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5197f; asc        ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000003c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570803a8; asc     W   ;;
 7: len 8; hex 99bac301570806b5; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c1; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51a04; asc        ;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000003c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157080b67; asc     W  g;;
 7: len 8; hex 99bac30157080e67; asc     W  g;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51a89; asc        ;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000003c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157081368; asc     W  h;;
 7: len 8; hex 99bac301570816aa; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51b0e; asc        ;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000003c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157081b0e; asc     W   ;;
 7: len 8; hex 99bac30157081e11; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51b93; asc        ;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000003c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708226c; asc     W "l;;
 7: len 8; hex 99bac30157082560; asc     W %`;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c5; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51c18; asc        ;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000003c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157082992; asc     W ) ;;
 7: len 8; hex 99bac3015708337f; asc     W 3 ;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51c9d; asc        ;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000003c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570838b9; asc     W 8 ;;
 7: len 8; hex 99bac30157083d4f; asc     W =O;;
 8: SQL NULL;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51d22; asc       ";;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000003c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708467e; asc     W F~;;
 7: len 8; hex 99bac30157084a0f; asc     W J ;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51da7; asc        ;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000003c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708534d; asc     W SM;;
 7: len 8; hex 99bac301570857c3; asc     W W ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c9; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51e2c; asc       ,;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000003c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157086106; asc     W a ;;
 7: len 8; hex 99bac301570865a8; asc     W e ;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ca; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51eb1; asc        ;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000003ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157086cf9; asc     W l ;;
 7: len 8; hex 99bac30157087092; asc     W p ;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cb; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51f36; asc       6;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000003cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708759c; asc     W u ;;
 7: len 8; hex 99bac30157087894; asc     W x ;;
 8: SQL NULL;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cc; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e51fbb; asc        ;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000003cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157087d04; asc     W } ;;
 7: len 8; hex 99bac30157088005; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cd; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52040; asc       @;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000003cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157088493; asc     W   ;;
 7: len 8; hex 99bac3015708878a; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ce; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e520c5; asc        ;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000003ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157088c52; asc     W  R;;
 7: len 8; hex 99bac30157088f10; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cf; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5214a; asc      !J;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000003cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708938f; asc     W   ;;
 7: len 8; hex 99bac30157089669; asc     W  i;;
 8: SQL NULL;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d0; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e521cf; asc      ! ;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000003d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157089a8f; asc     W   ;;
 7: len 8; hex 99bac30157089d97; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d1; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52254; asc      "T;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000003d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708a1ce; asc     W   ;;
 7: len 8; hex 99bac3015708a4a0; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e522d9; asc      " ;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000003d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708a860; asc     W  `;;
 7: len 8; hex 99bac3015708ab1b; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5235e; asc      #^;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000003d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708b348; asc     W  H;;
 7: len 8; hex 99bac3015708b752; asc     W  R;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e523e3; asc      # ;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000003d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708bce4; asc     W   ;;
 7: len 8; hex 99bac3015708bfc9; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52468; asc      $h;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000003d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708c487; asc     W   ;;
 7: len 8; hex 99bac3015708c8ca; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e524ed; asc      $ ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000003d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708d062; asc     W  b;;
 7: len 8; hex 99bac3015708d35d; asc     W  ];;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52572; asc      %r;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000003d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708d815; asc     W   ;;
 7: len 8; hex 99bac3015708db25; asc     W  %;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e525f7; asc      % ;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000003d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708df9e; asc     W   ;;
 7: len 8; hex 99bac3015708e276; asc     W  v;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d9; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5267c; asc      &|;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000003d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708e6d0; asc     W   ;;
 7: len 8; hex 99bac3015708e9e6; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003da; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52701; asc      ' ;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000003da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015708f1a5; asc     W   ;;
 7: len 8; hex 99bac3015708f684; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003db; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52786; asc      ' ;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000003db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015709005c; asc     W  \;;
 7: len 8; hex 99bac30157090592; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dc; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5280b; asc      ( ;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000003dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157090e0d; asc     W   ;;
 7: len 8; hex 99bac30157091292; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52890; asc      ( ;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157091bbb; asc     W   ;;
 7: len 8; hex 99bac301570920ab; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003de; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52915; asc      ) ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000003de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570925c8; asc     W % ;;
 7: len 8; hex 99bac30157093f4d; asc     W ?M;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003df; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e5299a; asc      ) ;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000003df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570944d3; asc     W D ;;
 7: len 8; hex 99bac3015709481d; asc     W H ;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e0; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52a1f; asc      * ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000003e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157094d95; asc     W M ;;
 7: len 8; hex 99bac3015709594e; asc     W YN;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e1; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52aa4; asc      * ;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000003e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570962fa; asc     W b ;;
 7: len 8; hex 99bac30157096606; asc     W f ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e2; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52b29; asc      +);;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000003e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157096a57; asc     W jW;;
 7: len 8; hex 99bac30157096d81; asc     W m ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e3; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52bae; asc      + ;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000003e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015709780e; asc     W x ;;
 7: len 8; hex 99bac30157097bdb; asc     W { ;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e4; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52c33; asc      ,3;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000003e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015709825f; asc     W  _;;
 7: len 8; hex 99bac3015709869c; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52cb8; asc      , ;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000003e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157098b3e; asc     W  >;;
 7: len 8; hex 99bac30157098e6a; asc     W  j;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e6; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52d3d; asc      -=;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000003e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570992b8; asc     W   ;;
 7: len 8; hex 99bac301570995a4; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e7; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52dc2; asc      - ;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000003e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30157099a85; asc     W   ;;
 7: len 8; hex 99bac30157099dca; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e8; asc         ;;
 1: len 6; hex 000000009bce; asc       ;;
 2: len 7; hex 01000000e52e47; asc      .G;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000003e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015709a33c; asc     W  <;;
 7: len 8; hex 99bac3015709a6a2; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e9; asc         ;;
 1: len 6; hex 000000009bea; asc       ;;
 2: len 7; hex 01000001c11f25; asc       %;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000003e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015709fc9d; asc     W   ;;
 7: len 8; hex 99bac301570a01b3; asc     W   ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ea; asc         ;;
 1: len 6; hex 000000009bea; asc       ;;
 2: len 7; hex 01000001c11fa8; asc        ;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000003ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570a2579; asc     W %y;;
 7: len 8; hex 99bac301570a28e9; asc     W ( ;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003eb; asc         ;;
 1: len 6; hex 000000009bea; asc       ;;
 2: len 7; hex 01000001c1202b; asc       +;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000003eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570a2f40; asc     W /@;;
 7: len 8; hex 99bac301570a346d; asc     W 4m;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ec; asc         ;;
 1: len 6; hex 000000009bea; asc       ;;
 2: len 7; hex 01000001c120ae; asc        ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000003ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac301570a3b39; asc     W ;9;;
 7: len 8; hex 99bac301570a3e98; asc     W > ;;
 8: SQL NULL;

Record lock, heap no 252 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000030b; asc         ;;
 1: len 6; hex 000000009cba; asc       ;;
 2: len 7; hex 010000018c21ff; asc      ! ;;
 3: len 8; hex 800000000000030b; asc         ;;
 4: len 8; hex 800000000000030b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3015700c92f; asc     W  /;;
 7: len 8; hex 99bac3015700cc92; asc     W   ;;
 8: len 8; hex 99bac3015d0ad651; asc     ]  Q;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 36 page no 12 n bits 296 index PRIMARY of table `deadlock_lab`.`member_account` trx id 40166 lock mode S locks rec but not gap waiting
Record lock, heap no 211 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 000000009cf4; asc       ;;
 2: len 7; hex 01000001cb21b0; asc      ! ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d373831393637383632; asc tk-781967862;;
 5: len 8; hex 99bac3015f0982dc; asc     _   ;;
 6: len 8; hex 99bac3015f0982dc; asc     _   ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```
