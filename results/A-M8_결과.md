# A-M8 Deadlock Matrix — 측정 결과

배치(알림 A → FK로 계정 B) ↔ 수신자(계정 B → 알림 A)
**배치는 member_account 를 갱신하지 않는다. 계정 락은 FK 로만 걸린다.**

고정값: 테넌트 5 · 계정 1000 · 발송요청 4000 · 외부API지연 400us
MySQL 8.0 (2 CPU / 2GB), innodb_lock_wait_timeout=5s

| # | 구성 | 배치 롤백 | 기록 | **수신자 데드락** | 수신자 타임아웃 | 토큰갱신 실패 | 알림확인 실패 | InnoDB | 소요 |
|---|---|---|---|---|---|---|---|---|---|
| V0 | ① Tasklet (최초)<br><sub>TASKLET / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | **45** | 0 | 0 | 45 | 45 | 12.8s |
| V1 | ② chunk 전환<br><sub>CHUNK(500) / 조회 TX내 / 발송 건별 / API TX내 / 수신자 B→A / FK O / REPEATABLE READ</sub> | 0 | 4000 | **35** | 0 | 0 | 35 | 35 | 13.0s |

> 실험 환경의 결과다. 운영 환경의 수치가 아니다.

---

## V0 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 16:13:21 135358810506816
*** (1) TRANSACTION:
TRANSACTION 48748, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 1099, OS thread handle 135358145570368, query id 430533 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 2997

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 40 page no 12 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 48748 lock_mode X locks rec but not gap
Record lock, heap no 198 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 00000000be6c; asc      l;;
 2: len 7; hex 0100000199071b; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d353733343934383635; asc tk-573494865;;
 5: len 8; hex 99bac303530ec4e9; asc     S   ;;
 6: len 8; hex 99bac303530ec4e9; asc     S   ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 38 page no 27 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 48748 lock_mode X locks rec but not gap waiting
Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013104b7; asc     1  ;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 8000000000000bb5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352015e7a; asc     R ^z;;
 7: len 8; hex 99bac303520161a0; asc     R a ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 48731, ACTIVE 2 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 70 lock struct(s), heap size 24696, 5512 row lock(s), undo log entries 1489
MySQL thread id 1368, OS thread handle 135358653097536, query id 432064 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (997, 3997, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 38 page no 27 n bits 320 index PRIMARY of table `deadlock_lab`.`notification` trx id 48731 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aca; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0964; asc     . d;;
 3: len 8; hex 80000000000002fa; asc         ;;
 4: len 8; hex 8000000000000aca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351053ddf; asc     Q = ;;
 7: len 8; hex 99bac3035105420d; asc     Q B ;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000acb; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e09e9; asc     .  ;;
 3: len 8; hex 80000000000002fb; asc         ;;
 4: len 8; hex 8000000000000acb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510549e7; asc     Q I ;;
 7: len 8; hex 99bac30351054e98; asc     Q N ;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000acc; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0a6e; asc     . n;;
 3: len 8; hex 80000000000002fc; asc         ;;
 4: len 8; hex 8000000000000acc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351055667; asc     Q Vg;;
 7: len 8; hex 99bac30351055a0f; asc     Q Z ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000acd; asc         ;;
 1: len 6; hex 00000000be32; asc      2;;
 2: len 7; hex 010000013a2906; asc     :) ;;
 3: len 8; hex 80000000000002fd; asc         ;;
 4: len 8; hex 8000000000000acd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105623b; asc     Q b;;;
 7: len 8; hex 99bac303510565b0; asc     Q e ;;
 8: len 8; hex 99bac30352025a2a; asc     R Z*;;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ace; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0b78; asc     . x;;
 3: len 8; hex 80000000000002fe; asc         ;;
 4: len 8; hex 8000000000000ace; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351056cfd; asc     Q l ;;
 7: len 8; hex 99bac30351057068; asc     Q ph;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000acf; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0bfd; asc     .  ;;
 3: len 8; hex 80000000000002ff; asc         ;;
 4: len 8; hex 8000000000000acf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351057834; asc     Q x4;;
 7: len 8; hex 99bac30351057bfd; asc     Q { ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad0; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0c82; asc     .  ;;
 3: len 8; hex 8000000000000300; asc         ;;
 4: len 8; hex 8000000000000ad0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510583a7; asc     Q   ;;
 7: len 8; hex 99bac3035105875f; asc     Q  _;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad1; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0d07; asc     .  ;;
 3: len 8; hex 8000000000000301; asc         ;;
 4: len 8; hex 8000000000000ad1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351058f95; asc     Q   ;;
 7: len 8; hex 99bac30351059303; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad2; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0d8c; asc     .  ;;
 3: len 8; hex 8000000000000302; asc         ;;
 4: len 8; hex 8000000000000ad2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351059a69; asc     Q  i;;
 7: len 8; hex 99bac30351059dc2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad3; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0e11; asc     .  ;;
 3: len 8; hex 8000000000000303; asc         ;;
 4: len 8; hex 8000000000000ad3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105a554; asc     Q  T;;
 7: len 8; hex 99bac3035105a89a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad4; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0e96; asc     .  ;;
 3: len 8; hex 8000000000000304; asc         ;;
 4: len 8; hex 8000000000000ad4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105b00c; asc     Q   ;;
 7: len 8; hex 99bac3035105b3b2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0f1b; asc     .  ;;
 3: len 8; hex 8000000000000305; asc         ;;
 4: len 8; hex 8000000000000ad5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105bb88; asc     Q   ;;
 7: len 8; hex 99bac3035105bf0c; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad6; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e0fa0; asc     .  ;;
 3: len 8; hex 8000000000000306; asc         ;;
 4: len 8; hex 8000000000000ad6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105c7a7; asc     Q   ;;
 7: len 8; hex 99bac3035105cb43; asc     Q  C;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad7; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1025; asc     . %;;
 3: len 8; hex 8000000000000307; asc         ;;
 4: len 8; hex 8000000000000ad7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105d2ee; asc     Q   ;;
 7: len 8; hex 99bac3035105d629; asc     Q  );;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad8; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e10aa; asc     .  ;;
 3: len 8; hex 8000000000000308; asc         ;;
 4: len 8; hex 8000000000000ad8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105dde4; asc     Q   ;;
 7: len 8; hex 99bac3035105e153; asc     Q  S;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ad9; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e112f; asc     . /;;
 3: len 8; hex 8000000000000309; asc         ;;
 4: len 8; hex 8000000000000ad9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105e95d; asc     Q  ];;
 7: len 8; hex 99bac3035105ecc7; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ada; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e11b4; asc     .  ;;
 3: len 8; hex 800000000000030a; asc         ;;
 4: len 8; hex 8000000000000ada; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035105f433; asc     Q  3;;
 7: len 8; hex 99bac3035105f776; asc     Q  v;;
 8: SQL NULL;

Record lock, heap no 19 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000adb; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1239; asc     . 9;;
 3: len 8; hex 800000000000030b; asc         ;;
 4: len 8; hex 8000000000000adb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351060009; asc     Q   ;;
 7: len 8; hex 99bac30351060360; asc     Q  `;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000adc; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e12be; asc     .  ;;
 3: len 8; hex 800000000000030c; asc         ;;
 4: len 8; hex 8000000000000adc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351060ab7; asc     Q   ;;
 7: len 8; hex 99bac30351060dcd; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000add; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1343; asc     . C;;
 3: len 8; hex 800000000000030d; asc         ;;
 4: len 8; hex 8000000000000add; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510614f8; asc     Q   ;;
 7: len 8; hex 99bac3035106181c; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ade; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e13c8; asc     .  ;;
 3: len 8; hex 800000000000030e; asc         ;;
 4: len 8; hex 8000000000000ade; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351061f22; asc     Q  ";;
 7: len 8; hex 99bac30351062249; asc     Q "I;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000adf; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e144d; asc     . M;;
 3: len 8; hex 800000000000030f; asc         ;;
 4: len 8; hex 8000000000000adf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510629a7; asc     Q ) ;;
 7: len 8; hex 99bac30351062cd7; asc     Q , ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae0; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e14d2; asc     .  ;;
 3: len 8; hex 8000000000000310; asc         ;;
 4: len 8; hex 8000000000000ae0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510633e3; asc     Q 3 ;;
 7: len 8; hex 99bac3035106372b; asc     Q 7+;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae1; asc         ;;
 1: len 6; hex 00000000be4f; asc      O;;
 2: len 7; hex 02000001ab1961; asc       a;;
 3: len 8; hex 8000000000000311; asc         ;;
 4: len 8; hex 8000000000000ae1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351063fac; asc     Q ? ;;
 7: len 8; hex 99bac303510642f1; asc     Q B ;;
 8: len 8; hex 99bac3035304663f; asc     S f?;;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae2; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e15dc; asc     .  ;;
 3: len 8; hex 8000000000000312; asc         ;;
 4: len 8; hex 8000000000000ae2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351064a91; asc     Q J ;;
 7: len 8; hex 99bac30351064dd6; asc     Q M ;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae3; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1661; asc     . a;;
 3: len 8; hex 8000000000000313; asc         ;;
 4: len 8; hex 8000000000000ae3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510654a6; asc     Q T ;;
 7: len 8; hex 99bac303510657b5; asc     Q W ;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae4; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e16e6; asc     .  ;;
 3: len 8; hex 8000000000000314; asc         ;;
 4: len 8; hex 8000000000000ae4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351065e6b; asc     Q ^k;;
 7: len 8; hex 99bac303510661bc; asc     Q a ;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e176b; asc     . k;;
 3: len 8; hex 8000000000000315; asc         ;;
 4: len 8; hex 8000000000000ae5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510668a1; asc     Q h ;;
 7: len 8; hex 99bac30351066bce; asc     Q k ;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae6; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e17f0; asc     .  ;;
 3: len 8; hex 8000000000000316; asc         ;;
 4: len 8; hex 8000000000000ae6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351067281; asc     Q r ;;
 7: len 8; hex 99bac303510675b0; asc     Q u ;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae7; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1875; asc     . u;;
 3: len 8; hex 8000000000000317; asc         ;;
 4: len 8; hex 8000000000000ae7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510689f7; asc     Q   ;;
 7: len 8; hex 99bac30351068fbb; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae8; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e18fa; asc     .  ;;
 3: len 8; hex 8000000000000318; asc         ;;
 4: len 8; hex 8000000000000ae8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351069b12; asc     Q   ;;
 7: len 8; hex 99bac30351069f35; asc     Q  5;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ae9; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e197f; asc     .  ;;
 3: len 8; hex 8000000000000319; asc         ;;
 4: len 8; hex 8000000000000ae9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035106b964; asc     Q  d;;
 7: len 8; hex 99bac3035106bddd; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aea; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1a04; asc     .  ;;
 3: len 8; hex 800000000000031a; asc         ;;
 4: len 8; hex 8000000000000aea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035106ce82; asc     Q   ;;
 7: len 8; hex 99bac3035106d371; asc     Q  q;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aeb; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1a89; asc     .  ;;
 3: len 8; hex 800000000000031b; asc         ;;
 4: len 8; hex 8000000000000aeb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035106dd93; asc     Q   ;;
 7: len 8; hex 99bac3035106e0a3; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aec; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1b0e; asc     .  ;;
 3: len 8; hex 800000000000031c; asc         ;;
 4: len 8; hex 8000000000000aec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035106e744; asc     Q  D;;
 7: len 8; hex 99bac3035106ea44; asc     Q  D;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aed; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1b93; asc     .  ;;
 3: len 8; hex 800000000000031d; asc         ;;
 4: len 8; hex 8000000000000aed; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035106f5af; asc     Q   ;;
 7: len 8; hex 99bac3035106fb44; asc     Q  D;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aee; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1c18; asc     .  ;;
 3: len 8; hex 800000000000031e; asc         ;;
 4: len 8; hex 8000000000000aee; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351070905; asc     Q   ;;
 7: len 8; hex 99bac30351070df2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aef; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1c9d; asc     .  ;;
 3: len 8; hex 800000000000031f; asc         ;;
 4: len 8; hex 8000000000000aef; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351071a86; asc     Q   ;;
 7: len 8; hex 99bac30351071f31; asc     Q  1;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af0; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1d22; asc     . ";;
 3: len 8; hex 8000000000000320; asc         ;;
 4: len 8; hex 8000000000000af0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351072a95; asc     Q * ;;
 7: len 8; hex 99bac30351072e47; asc     Q .G;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af1; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1da7; asc     .  ;;
 3: len 8; hex 8000000000000321; asc        !;;
 4: len 8; hex 8000000000000af1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351073604; asc     Q 6 ;;
 7: len 8; hex 99bac30351073930; asc     Q 90;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af2; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1e2c; asc     . ,;;
 3: len 8; hex 8000000000000322; asc        ";;
 4: len 8; hex 8000000000000af2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510740c3; asc     Q @ ;;
 7: len 8; hex 99bac3035107444e; asc     Q DN;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af3; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1eb1; asc     .  ;;
 3: len 8; hex 8000000000000323; asc        #;;
 4: len 8; hex 8000000000000af3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351074d69; asc     Q Mi;;
 7: len 8; hex 99bac303510751f3; asc     Q Q ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af4; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1f36; asc     . 6;;
 3: len 8; hex 8000000000000324; asc        $;;
 4: len 8; hex 8000000000000af4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351075e87; asc     Q ^ ;;
 7: len 8; hex 99bac303510761fb; asc     Q a ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e1fbb; asc     .  ;;
 3: len 8; hex 8000000000000325; asc        %;;
 4: len 8; hex 8000000000000af5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510769e7; asc     Q i ;;
 7: len 8; hex 99bac30351076d7d; asc     Q m};;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af6; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2040; asc     . @;;
 3: len 8; hex 8000000000000326; asc        &;;
 4: len 8; hex 8000000000000af6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510777b9; asc     Q w ;;
 7: len 8; hex 99bac30351077b03; asc     Q { ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af7; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e20c5; asc     .  ;;
 3: len 8; hex 8000000000000327; asc        ';;
 4: len 8; hex 8000000000000af7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107831e; asc     Q   ;;
 7: len 8; hex 99bac303510786c0; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af8; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e214a; asc     .!J;;
 3: len 8; hex 8000000000000328; asc        (;;
 4: len 8; hex 8000000000000af8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351079109; asc     Q   ;;
 7: len 8; hex 99bac30351079627; asc     Q  ';;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000af9; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e21cf; asc     .! ;;
 3: len 8; hex 8000000000000329; asc        );;
 4: len 8; hex 8000000000000af9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351079f5f; asc     Q  _;;
 7: len 8; hex 99bac3035107a4fe; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000afa; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2254; asc     ."T;;
 3: len 8; hex 800000000000032a; asc        *;;
 4: len 8; hex 8000000000000afa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107acbe; asc     Q   ;;
 7: len 8; hex 99bac3035107b086; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000afb; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e22d9; asc     ." ;;
 3: len 8; hex 800000000000032b; asc        +;;
 4: len 8; hex 8000000000000afb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107b8b2; asc     Q   ;;
 7: len 8; hex 99bac3035107bc74; asc     Q  t;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000afc; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e235e; asc     .#^;;
 3: len 8; hex 800000000000032c; asc        ,;;
 4: len 8; hex 8000000000000afc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107c5ba; asc     Q   ;;
 7: len 8; hex 99bac3035107ca36; asc     Q  6;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000afd; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e23e3; asc     .# ;;
 3: len 8; hex 800000000000032d; asc        -;;
 4: len 8; hex 8000000000000afd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107d3e8; asc     Q   ;;
 7: len 8; hex 99bac3035107d7ab; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000afe; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2468; asc     .$h;;
 3: len 8; hex 800000000000032e; asc        .;;
 4: len 8; hex 8000000000000afe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107e27a; asc     Q  z;;
 7: len 8; hex 99bac3035107e6ea; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000aff; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e24ed; asc     .$ ;;
 3: len 8; hex 800000000000032f; asc        /;;
 4: len 8; hex 8000000000000aff; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107f0e6; asc     Q   ;;
 7: len 8; hex 99bac3035107f546; asc     Q  F;;
 8: SQL NULL;

Record lock, heap no 56 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b00; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2572; asc     .%r;;
 3: len 8; hex 8000000000000330; asc        0;;
 4: len 8; hex 8000000000000b00; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035107ff93; asc     Q   ;;
 7: len 8; hex 99bac303510803cd; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b01; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e25f7; asc     .% ;;
 3: len 8; hex 8000000000000331; asc        1;;
 4: len 8; hex 8000000000000b01; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351080f0a; asc     Q   ;;
 7: len 8; hex 99bac30351081309; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b02; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e267c; asc     .&|;;
 3: len 8; hex 8000000000000332; asc        2;;
 4: len 8; hex 8000000000000b02; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351081c35; asc     Q  5;;
 7: len 8; hex 99bac30351082092; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b03; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2701; asc     .' ;;
 3: len 8; hex 8000000000000333; asc        3;;
 4: len 8; hex 8000000000000b03; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510828cd; asc     Q ( ;;
 7: len 8; hex 99bac30351082c36; asc     Q ,6;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b04; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2786; asc     .' ;;
 3: len 8; hex 8000000000000334; asc        4;;
 4: len 8; hex 8000000000000b04; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351083455; asc     Q 4U;;
 7: len 8; hex 99bac3035108382b; asc     Q 8+;;
 8: SQL NULL;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b05; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e280b; asc     .( ;;
 3: len 8; hex 8000000000000335; asc        5;;
 4: len 8; hex 8000000000000b05; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351084180; asc     Q A ;;
 7: len 8; hex 99bac3035108454d; asc     Q EM;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b06; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2890; asc     .( ;;
 3: len 8; hex 8000000000000336; asc        6;;
 4: len 8; hex 8000000000000b06; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351084d42; asc     Q MB;;
 7: len 8; hex 99bac303510850b5; asc     Q P ;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b07; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2915; asc     .) ;;
 3: len 8; hex 8000000000000337; asc        7;;
 4: len 8; hex 8000000000000b07; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108592d; asc     Q Y-;;
 7: len 8; hex 99bac30351085cee; asc     Q \ ;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b08; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e299a; asc     .) ;;
 3: len 8; hex 8000000000000338; asc        8;;
 4: len 8; hex 8000000000000b08; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108652c; asc     Q e,;;
 7: len 8; hex 99bac303510868b5; asc     Q h ;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b09; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2a1f; asc     .* ;;
 3: len 8; hex 8000000000000339; asc        9;;
 4: len 8; hex 8000000000000b09; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351087013; asc     Q p ;;
 7: len 8; hex 99bac3035108734c; asc     Q sL;;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0a; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2aa4; asc     .* ;;
 3: len 8; hex 800000000000033a; asc        :;;
 4: len 8; hex 8000000000000b0a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351087b16; asc     Q { ;;
 7: len 8; hex 99bac30351087ea0; asc     Q ~ ;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0b; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2b29; asc     .+);;
 3: len 8; hex 800000000000033b; asc        ;;;
 4: len 8; hex 8000000000000b0b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108870e; asc     Q   ;;
 7: len 8; hex 99bac30351088b06; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0c; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2bae; asc     .+ ;;
 3: len 8; hex 800000000000033c; asc        <;;
 4: len 8; hex 8000000000000b0c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108947b; asc     Q  {;;
 7: len 8; hex 99bac3035108984e; asc     Q  N;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0d; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2c33; asc     .,3;;
 3: len 8; hex 800000000000033d; asc        =;;
 4: len 8; hex 8000000000000b0d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108a07a; asc     Q  z;;
 7: len 8; hex 99bac3035108a3e4; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0e; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2cb8; asc     ., ;;
 3: len 8; hex 800000000000033e; asc        >;;
 4: len 8; hex 8000000000000b0e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108ac85; asc     Q   ;;
 7: len 8; hex 99bac3035108afeb; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b0f; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2d3d; asc     .-=;;
 3: len 8; hex 800000000000033f; asc        ?;;
 4: len 8; hex 8000000000000b0f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108b741; asc     Q  A;;
 7: len 8; hex 99bac3035108bad5; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b10; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2dc2; asc     .- ;;
 3: len 8; hex 8000000000000340; asc        @;;
 4: len 8; hex 8000000000000b10; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108c38c; asc     Q   ;;
 7: len 8; hex 99bac3035108c703; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b11; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2e47; asc     ..G;;
 3: len 8; hex 8000000000000341; asc        A;;
 4: len 8; hex 8000000000000b11; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108cff6; asc     Q   ;;
 7: len 8; hex 99bac3035108d3df; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b12; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2ecc; asc     .. ;;
 3: len 8; hex 8000000000000342; asc        B;;
 4: len 8; hex 8000000000000b12; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108dc19; asc     Q   ;;
 7: len 8; hex 99bac3035108dfa8; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b13; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2f51; asc     ./Q;;
 3: len 8; hex 8000000000000343; asc        C;;
 4: len 8; hex 8000000000000b13; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108e72d; asc     Q  -;;
 7: len 8; hex 99bac3035108ea88; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b14; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e2fd6; asc     ./ ;;
 3: len 8; hex 8000000000000344; asc        D;;
 4: len 8; hex 8000000000000b14; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108f1dc; asc     Q   ;;
 7: len 8; hex 99bac3035108f4e9; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b15; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e305b; asc     .0[;;
 3: len 8; hex 8000000000000345; asc        E;;
 4: len 8; hex 8000000000000b15; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035108fca1; asc     Q   ;;
 7: len 8; hex 99bac3035108ffba; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b16; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e30e0; asc     .0 ;;
 3: len 8; hex 8000000000000346; asc        F;;
 4: len 8; hex 8000000000000b16; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351090791; asc     Q   ;;
 7: len 8; hex 99bac30351090b53; asc     Q  S;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b17; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3165; asc     .1e;;
 3: len 8; hex 8000000000000347; asc        G;;
 4: len 8; hex 8000000000000b17; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351091417; asc     Q   ;;
 7: len 8; hex 99bac3035109178a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b18; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e31ea; asc     .1 ;;
 3: len 8; hex 8000000000000348; asc        H;;
 4: len 8; hex 8000000000000b18; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351091ee3; asc     Q   ;;
 7: len 8; hex 99bac303510921f4; asc     Q ! ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b19; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e326f; asc     .2o;;
 3: len 8; hex 8000000000000349; asc        I;;
 4: len 8; hex 8000000000000b19; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351092932; asc     Q )2;;
 7: len 8; hex 99bac30351092c54; asc     Q ,T;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1a; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e32f4; asc     .2 ;;
 3: len 8; hex 800000000000034a; asc        J;;
 4: len 8; hex 8000000000000b1a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351093382; asc     Q 3 ;;
 7: len 8; hex 99bac303510936a4; asc     Q 6 ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1b; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3379; asc     .3y;;
 3: len 8; hex 800000000000034b; asc        K;;
 4: len 8; hex 8000000000000b1b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351093d82; asc     Q = ;;
 7: len 8; hex 99bac303510940df; asc     Q @ ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1c; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e33fe; asc     .3 ;;
 3: len 8; hex 800000000000034c; asc        L;;
 4: len 8; hex 8000000000000b1c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510948ec; asc     Q H ;;
 7: len 8; hex 99bac30351094c55; asc     Q LU;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1d; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3483; asc     .4 ;;
 3: len 8; hex 800000000000034d; asc        M;;
 4: len 8; hex 8000000000000b1d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351095377; asc     Q Sw;;
 7: len 8; hex 99bac303510956c7; asc     Q V ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1e; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3508; asc     .5 ;;
 3: len 8; hex 800000000000034e; asc        N;;
 4: len 8; hex 8000000000000b1e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351095fa3; asc     Q _ ;;
 7: len 8; hex 99bac30351096356; asc     Q cV;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b1f; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e358d; asc     .5 ;;
 3: len 8; hex 800000000000034f; asc        O;;
 4: len 8; hex 8000000000000b1f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351096b37; asc     Q k7;;
 7: len 8; hex 99bac30351096e54; asc     Q nT;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b20; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3612; asc     .6 ;;
 3: len 8; hex 8000000000000350; asc        P;;
 4: len 8; hex 8000000000000b20; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510975ac; asc     Q u ;;
 7: len 8; hex 99bac303510978b4; asc     Q x ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b21; asc        !;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3697; asc     .6 ;;
 3: len 8; hex 8000000000000351; asc        Q;;
 4: len 8; hex 8000000000000b21; asc        !;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351097fb2; asc     Q   ;;
 7: len 8; hex 99bac3035109830c; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b22; asc        ";;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e371c; asc     .7 ;;
 3: len 8; hex 8000000000000352; asc        R;;
 4: len 8; hex 8000000000000b22; asc        ";;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351098aa9; asc     Q   ;;
 7: len 8; hex 99bac30351098e15; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b23; asc        #;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e37a1; asc     .7 ;;
 3: len 8; hex 8000000000000353; asc        S;;
 4: len 8; hex 8000000000000b23; asc        #;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109957c; asc     Q  |;;
 7: len 8; hex 99bac3035109986e; asc     Q  n;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b24; asc        $;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3826; asc     .8&;;
 3: len 8; hex 8000000000000354; asc        T;;
 4: len 8; hex 8000000000000b24; asc        $;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30351099eff; asc     Q   ;;
 7: len 8; hex 99bac3035109a216; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b25; asc        %;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e38ab; asc     .8 ;;
 3: len 8; hex 8000000000000355; asc        U;;
 4: len 8; hex 8000000000000b25; asc        %;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109a8ce; asc     Q   ;;
 7: len 8; hex 99bac3035109abff; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b26; asc        &;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3930; asc     .90;;
 3: len 8; hex 8000000000000356; asc        V;;
 4: len 8; hex 8000000000000b26; asc        &;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109b383; asc     Q   ;;
 7: len 8; hex 99bac3035109b694; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b27; asc        ';;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e39b5; asc     .9 ;;
 3: len 8; hex 8000000000000357; asc        W;;
 4: len 8; hex 8000000000000b27; asc        ';;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109bd72; asc     Q  r;;
 7: len 8; hex 99bac3035109c0c1; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b28; asc        (;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3a3a; asc     .::;;
 3: len 8; hex 8000000000000358; asc        X;;
 4: len 8; hex 8000000000000b28; asc        (;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109c880; asc     Q   ;;
 7: len 8; hex 99bac3035109cbf0; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b29; asc        );;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3abf; asc     .: ;;
 3: len 8; hex 8000000000000359; asc        Y;;
 4: len 8; hex 8000000000000b29; asc        );;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109d340; asc     Q  @;;
 7: len 8; hex 99bac3035109d679; asc     Q  y;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2a; asc        *;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3b44; asc     .;D;;
 3: len 8; hex 800000000000035a; asc        Z;;
 4: len 8; hex 8000000000000b2a; asc        *;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109ddbf; asc     Q   ;;
 7: len 8; hex 99bac3035109e0c8; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2b; asc        +;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3bc9; asc     .; ;;
 3: len 8; hex 800000000000035b; asc        [;;
 4: len 8; hex 8000000000000b2b; asc        +;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109e7db; asc     Q   ;;
 7: len 8; hex 99bac3035109eafb; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2c; asc        ,;;
 1: len 6; hex 00000000be59; asc      Y;;
 2: len 7; hex 01000001132f71; asc      /q;;
 3: len 8; hex 800000000000035c; asc        \;;
 4: len 8; hex 8000000000000b2c; asc        ,;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109f238; asc     Q  8;;
 7: len 8; hex 99bac3035109f5b5; asc     Q   ;;
 8: len 8; hex 99bac303530e3d13; asc     S = ;;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2d; asc        -;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3cd3; asc     .< ;;
 3: len 8; hex 800000000000035d; asc        ];;
 4: len 8; hex 8000000000000b2d; asc        -;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035109fe72; asc     Q  r;;
 7: len 8; hex 99bac303510a020f; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2e; asc        .;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3d58; asc     .=X;;
 3: len 8; hex 800000000000035e; asc        ^;;
 4: len 8; hex 8000000000000b2e; asc        .;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a0982; asc     Q   ;;
 7: len 8; hex 99bac303510a0cde; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b2f; asc        /;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3ddd; asc     .= ;;
 3: len 8; hex 800000000000035f; asc        _;;
 4: len 8; hex 8000000000000b2f; asc        /;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a1434; asc     Q  4;;
 7: len 8; hex 99bac303510a1772; asc     Q  r;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b30; asc        0;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3e62; asc     .>b;;
 3: len 8; hex 8000000000000360; asc        `;;
 4: len 8; hex 8000000000000b30; asc        0;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a228c; asc     Q " ;;
 7: len 8; hex 99bac303510a2646; asc     Q &F;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b31; asc        1;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3ee7; asc     .> ;;
 3: len 8; hex 8000000000000361; asc        a;;
 4: len 8; hex 8000000000000b31; asc        1;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a30af; asc     Q 0 ;;
 7: len 8; hex 99bac303510a3488; asc     Q 4 ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b32; asc        2;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000012e3f6c; asc     .?l;;
 3: len 8; hex 8000000000000362; asc        b;;
 4: len 8; hex 8000000000000b32; asc        2;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a3ff9; asc     Q ? ;;
 7: len 8; hex 99bac303510a4382; asc     Q C ;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b33; asc        3;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130008f; asc     0  ;;
 3: len 8; hex 8000000000000363; asc        c;;
 4: len 8; hex 8000000000000b33; asc        3;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a4d84; asc     Q M ;;
 7: len 8; hex 99bac303510a527f; asc     Q R ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b34; asc        4;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300114; asc     0  ;;
 3: len 8; hex 8000000000000364; asc        d;;
 4: len 8; hex 8000000000000b34; asc        4;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a5dd6; asc     Q ] ;;
 7: len 8; hex 99bac303510a613f; asc     Q a?;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b35; asc        5;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300199; asc     0  ;;
 3: len 8; hex 8000000000000365; asc        e;;
 4: len 8; hex 8000000000000b35; asc        5;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a6902; asc     Q i ;;
 7: len 8; hex 99bac303510a6c20; asc     Q l ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b36; asc        6;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130021e; asc     0  ;;
 3: len 8; hex 8000000000000366; asc        f;;
 4: len 8; hex 8000000000000b36; asc        6;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a7560; asc     Q u`;;
 7: len 8; hex 99bac303510a786b; asc     Q xk;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b37; asc        7;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013002a3; asc     0  ;;
 3: len 8; hex 8000000000000367; asc        g;;
 4: len 8; hex 8000000000000b37; asc        7;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a7f69; asc     Q  i;;
 7: len 8; hex 99bac303510a82a5; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b38; asc        8;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300328; asc     0 (;;
 3: len 8; hex 8000000000000368; asc        h;;
 4: len 8; hex 8000000000000b38; asc        8;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a899d; asc     Q   ;;
 7: len 8; hex 99bac303510a8d94; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b39; asc        9;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013003ad; asc     0  ;;
 3: len 8; hex 8000000000000369; asc        i;;
 4: len 8; hex 8000000000000b39; asc        9;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510a9656; asc     Q  V;;
 7: len 8; hex 99bac303510a99e2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3a; asc        :;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300432; asc     0 2;;
 3: len 8; hex 800000000000036a; asc        j;;
 4: len 8; hex 8000000000000b3a; asc        :;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510aa177; asc     Q  w;;
 7: len 8; hex 99bac303510aa4a5; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3b; asc        ;;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013004b7; asc     0  ;;
 3: len 8; hex 800000000000036b; asc        k;;
 4: len 8; hex 8000000000000b3b; asc        ;;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510aac24; asc     Q  $;;
 7: len 8; hex 99bac303510aafa3; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3c; asc        <;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130053c; asc     0 <;;
 3: len 8; hex 800000000000036c; asc        l;;
 4: len 8; hex 8000000000000b3c; asc        <;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ab749; asc     Q  I;;
 7: len 8; hex 99bac303510aba9d; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3d; asc        =;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013005c1; asc     0  ;;
 3: len 8; hex 800000000000036d; asc        m;;
 4: len 8; hex 8000000000000b3d; asc        =;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ac1e1; asc     Q   ;;
 7: len 8; hex 99bac303510ac52a; asc     Q  *;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3e; asc        >;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300646; asc     0 F;;
 3: len 8; hex 800000000000036e; asc        n;;
 4: len 8; hex 8000000000000b3e; asc        >;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510acc89; asc     Q   ;;
 7: len 8; hex 99bac303510acff4; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b3f; asc        ?;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013006cb; asc     0  ;;
 3: len 8; hex 800000000000036f; asc        o;;
 4: len 8; hex 8000000000000b3f; asc        ?;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ad7ca; asc     Q   ;;
 7: len 8; hex 99bac303510adb24; asc     Q  $;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b40; asc        @;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300750; asc     0 P;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000b40; asc        @;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ae2a4; asc     Q   ;;
 7: len 8; hex 99bac303510ae5c6; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b41; asc        A;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013007d5; asc     0  ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000b41; asc        A;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510aee40; asc     Q  @;;
 7: len 8; hex 99bac303510af176; asc     Q  v;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b42; asc        B;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130085a; asc     0 Z;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 8000000000000b42; asc        B;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510af89c; asc     Q   ;;
 7: len 8; hex 99bac303510afc1a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 123 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b43; asc        C;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013008df; asc     0  ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 8000000000000b43; asc        C;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b03e8; asc     Q   ;;
 7: len 8; hex 99bac303510b0719; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b44; asc        D;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300964; asc     0 d;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 8000000000000b44; asc        D;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b0e83; asc     Q   ;;
 7: len 8; hex 99bac303510b11cc; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b45; asc        E;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013009e9; asc     0  ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 8000000000000b45; asc        E;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b1983; asc     Q   ;;
 7: len 8; hex 99bac303510b1c9f; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 126 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b46; asc        F;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300a6e; asc     0 n;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 8000000000000b46; asc        F;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b2382; asc     Q # ;;
 7: len 8; hex 99bac303510b2676; asc     Q &v;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b47; asc        G;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300af3; asc     0  ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 8000000000000b47; asc        G;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b2ecb; asc     Q . ;;
 7: len 8; hex 99bac303510b3217; asc     Q 2 ;;
 8: SQL NULL;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b48; asc        H;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300b78; asc     0 x;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000b48; asc        H;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b38d7; asc     Q 8 ;;
 7: len 8; hex 99bac303510b3c0a; asc     Q < ;;
 8: SQL NULL;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b49; asc        I;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300bfd; asc     0  ;;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000b49; asc        I;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b4359; asc     Q CY;;
 7: len 8; hex 99bac303510b465e; asc     Q F^;;
 8: SQL NULL;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4a; asc        J;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300c82; asc     0  ;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 8000000000000b4a; asc        J;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b4d3e; asc     Q M>;;
 7: len 8; hex 99bac303510b50d5; asc     Q P ;;
 8: SQL NULL;

Record lock, heap no 131 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4b; asc        K;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300d07; asc     0  ;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 8000000000000b4b; asc        K;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b586b; asc     Q Xk;;
 7: len 8; hex 99bac303510b5ba5; asc     Q [ ;;
 8: SQL NULL;

Record lock, heap no 132 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4c; asc        L;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300d8c; asc     0  ;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 8000000000000b4c; asc        L;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b628a; asc     Q b ;;
 7: len 8; hex 99bac303510b6663; asc     Q fc;;
 8: SQL NULL;

Record lock, heap no 133 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4d; asc        M;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300e11; asc     0  ;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 8000000000000b4d; asc        M;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b7037; asc     Q p7;;
 7: len 8; hex 99bac303510b73ab; asc     Q s ;;
 8: SQL NULL;

Record lock, heap no 134 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4e; asc        N;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300e96; asc     0  ;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 8000000000000b4e; asc        N;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b7bc6; asc     Q { ;;
 7: len 8; hex 99bac303510b7f69; asc     Q  i;;
 8: SQL NULL;

Record lock, heap no 135 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b4f; asc        O;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300f1b; asc     0  ;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 8000000000000b4f; asc        O;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b86bf; asc     Q   ;;
 7: len 8; hex 99bac303510b89c8; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 136 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b50; asc        P;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001300fa0; asc     0  ;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000b50; asc        P;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b915f; asc     Q  _;;
 7: len 8; hex 99bac303510b95e0; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 137 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b51; asc        Q;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301025; asc     0 %;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000b51; asc        Q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510b9efa; asc     Q   ;;
 7: len 8; hex 99bac303510ba387; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 138 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b52; asc        R;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013010aa; asc     0  ;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 8000000000000b52; asc        R;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bab25; asc     Q  %;;
 7: len 8; hex 99bac303510bae3e; asc     Q  >;;
 8: SQL NULL;

Record lock, heap no 139 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b53; asc        S;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130112f; asc     0 /;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 8000000000000b53; asc        S;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bb504; asc     Q   ;;
 7: len 8; hex 99bac303510bb804; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 140 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b54; asc        T;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013011b4; asc     0  ;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 8000000000000b54; asc        T;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bc19c; asc     Q   ;;
 7: len 8; hex 99bac303510bc4f9; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 141 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b55; asc        U;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301239; asc     0 9;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 8000000000000b55; asc        U;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bcc26; asc     Q  &;;
 7: len 8; hex 99bac303510bcf1e; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 142 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b56; asc        V;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013012be; asc     0  ;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 8000000000000b56; asc        V;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bd6aa; asc     Q   ;;
 7: len 8; hex 99bac303510bda1a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 143 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b57; asc        W;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301343; asc     0 C;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 8000000000000b57; asc        W;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510be1b5; asc     Q   ;;
 7: len 8; hex 99bac303510be563; asc     Q  c;;
 8: SQL NULL;

Record lock, heap no 144 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b58; asc        X;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013013c8; asc     0  ;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000b58; asc        X;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bed3b; asc     Q  ;;;
 7: len 8; hex 99bac303510bf061; asc     Q  a;;
 8: SQL NULL;

Record lock, heap no 145 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b59; asc        Y;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130144d; asc     0 M;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000b59; asc        Y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510bfb8d; asc     Q   ;;
 7: len 8; hex 99bac303510bffcc; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 146 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5a; asc        Z;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013014d2; asc     0  ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 8000000000000b5a; asc        Z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c071b; asc     Q   ;;
 7: len 8; hex 99bac303510c0a89; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 147 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5b; asc        [;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301557; asc     0 W;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 8000000000000b5b; asc        [;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c1761; asc     Q  a;;
 7: len 8; hex 99bac303510c1bc0; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 148 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5c; asc        \;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013015dc; asc     0  ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 8000000000000b5c; asc        \;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c280c; asc     Q ( ;;
 7: len 8; hex 99bac303510c403e; asc     Q @>;;
 8: SQL NULL;

Record lock, heap no 149 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5d; asc        ];;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301661; asc     0 a;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 8000000000000b5d; asc        ];;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c4c22; asc     Q L";;
 7: len 8; hex 99bac303510c5106; asc     Q Q ;;
 8: SQL NULL;

Record lock, heap no 150 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5e; asc        ^;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013016e6; asc     0  ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 8000000000000b5e; asc        ^;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c5fde; asc     Q _ ;;
 7: len 8; hex 99bac303510c63e4; asc     Q c ;;
 8: SQL NULL;

Record lock, heap no 151 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b5f; asc        _;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130176b; asc     0 k;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 8000000000000b5f; asc        _;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c6f85; asc     Q o ;;
 7: len 8; hex 99bac303510c7393; asc     Q s ;;
 8: SQL NULL;

Record lock, heap no 152 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b60; asc        `;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013017f0; asc     0  ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000b60; asc        `;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c92cd; asc     Q   ;;
 7: len 8; hex 99bac303510c9665; asc     Q  e;;
 8: SQL NULL;

Record lock, heap no 153 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b61; asc        a;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301875; asc     0 u;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000b61; asc        a;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510c9e30; asc     Q  0;;
 7: len 8; hex 99bac303510ca1a1; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 154 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b62; asc        b;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013018fa; asc     0  ;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 8000000000000b62; asc        b;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ca921; asc     Q  !;;
 7: len 8; hex 99bac303510cac76; asc     Q  v;;
 8: SQL NULL;

Record lock, heap no 155 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b63; asc        c;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130197f; asc     0  ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 8000000000000b63; asc        c;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510cb3ad; asc     Q   ;;
 7: len 8; hex 99bac303510cb6a9; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 156 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b64; asc        d;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301a04; asc     0  ;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 8000000000000b64; asc        d;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510cbdab; asc     Q   ;;
 7: len 8; hex 99bac303510cc0f7; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 157 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b65; asc        e;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301a89; asc     0  ;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 8000000000000b65; asc        e;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510cc7e3; asc     Q   ;;
 7: len 8; hex 99bac303510ccd45; asc     Q  E;;
 8: SQL NULL;

Record lock, heap no 158 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b66; asc        f;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301b0e; asc     0  ;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 8000000000000b66; asc        f;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510cd826; asc     Q  &;;
 7: len 8; hex 99bac303510cdc6a; asc     Q  j;;
 8: SQL NULL;

Record lock, heap no 159 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b67; asc        g;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301b93; asc     0  ;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 8000000000000b67; asc        g;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510cec38; asc     Q  8;;
 7: len 8; hex 99bac303510cf2af; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 160 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b68; asc        h;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301c18; asc     0  ;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000b68; asc        h;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d01ec; asc     Q   ;;
 7: len 8; hex 99bac303510d0607; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 161 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b69; asc        i;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301c9d; asc     0  ;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000b69; asc        i;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d0ea7; asc     Q   ;;
 7: len 8; hex 99bac303510d12ad; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 162 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6a; asc        j;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301d22; asc     0 ";;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 8000000000000b6a; asc        j;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d1c0b; asc     Q   ;;
 7: len 8; hex 99bac303510d205d; asc     Q  ];;
 8: SQL NULL;

Record lock, heap no 163 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6b; asc        k;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301da7; asc     0  ;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 8000000000000b6b; asc        k;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d29ed; asc     Q ) ;;
 7: len 8; hex 99bac303510d2eaf; asc     Q . ;;
 8: SQL NULL;

Record lock, heap no 164 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6c; asc        l;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301e2c; asc     0 ,;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 8000000000000b6c; asc        l;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d38de; asc     Q 8 ;;
 7: len 8; hex 99bac303510d3d55; asc     Q =U;;
 8: SQL NULL;

Record lock, heap no 165 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6d; asc        m;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301eb1; asc     0  ;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 8000000000000b6d; asc        m;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d456f; asc     Q Eo;;
 7: len 8; hex 99bac303510d48ef; asc     Q H ;;
 8: SQL NULL;

Record lock, heap no 166 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6e; asc        n;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301f36; asc     0 6;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 8000000000000b6e; asc        n;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d50aa; asc     Q P ;;
 7: len 8; hex 99bac303510d53a7; asc     Q S ;;
 8: SQL NULL;

Record lock, heap no 167 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b6f; asc        o;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001301fbb; asc     0  ;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 8000000000000b6f; asc        o;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d5b25; asc     Q [%;;
 7: len 8; hex 99bac303510d5e50; asc     Q ^P;;
 8: SQL NULL;

Record lock, heap no 168 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b70; asc        p;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302040; asc     0 @;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 8000000000000b70; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d6536; asc     Q e6;;
 7: len 8; hex 99bac303510d684b; asc     Q hK;;
 8: SQL NULL;

Record lock, heap no 169 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b71; asc        q;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013020c5; asc     0  ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 8000000000000b71; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d701c; asc     Q p ;;
 7: len 8; hex 99bac303510d734e; asc     Q sN;;
 8: SQL NULL;

Record lock, heap no 170 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b72; asc        r;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130214a; asc     0!J;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 8000000000000b72; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d7a1e; asc     Q z ;;
 7: len 8; hex 99bac303510d7d2a; asc     Q }*;;
 8: SQL NULL;

Record lock, heap no 171 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b73; asc        s;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013021cf; asc     0! ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 8000000000000b73; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d8489; asc     Q   ;;
 7: len 8; hex 99bac303510d87bc; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 172 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b74; asc        t;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302254; asc     0"T;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 8000000000000b74; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d8f36; asc     Q  6;;
 7: len 8; hex 99bac303510d9263; asc     Q  c;;
 8: SQL NULL;

Record lock, heap no 173 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b75; asc        u;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013022d9; asc     0" ;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 8000000000000b75; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510d9a47; asc     Q  G;;
 7: len 8; hex 99bac303510d9dd3; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 174 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b76; asc        v;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130235e; asc     0#^;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 8000000000000b76; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510da503; asc     Q   ;;
 7: len 8; hex 99bac303510da857; asc     Q  W;;
 8: SQL NULL;

Record lock, heap no 175 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b77; asc        w;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013023e3; asc     0# ;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 8000000000000b77; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510dafb0; asc     Q   ;;
 7: len 8; hex 99bac303510db29a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 176 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b78; asc        x;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302468; asc     0$h;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 8000000000000b78; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510db9b2; asc     Q   ;;
 7: len 8; hex 99bac303510dbced; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 177 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b79; asc        y;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013024ed; asc     0$ ;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 8000000000000b79; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510dc4d9; asc     Q   ;;
 7: len 8; hex 99bac303510dc834; asc     Q  4;;
 8: SQL NULL;

Record lock, heap no 178 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7a; asc        z;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302572; asc     0%r;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 8000000000000b7a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510dcf45; asc     Q  E;;
 7: len 8; hex 99bac303510dd222; asc     Q  ";;
 8: SQL NULL;

Record lock, heap no 179 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7b; asc        {;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013025f7; asc     0% ;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 8000000000000b7b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510dd93c; asc     Q  <;;
 7: len 8; hex 99bac303510ddc52; asc     Q  R;;
 8: SQL NULL;

Record lock, heap no 180 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7c; asc        |;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130267c; asc     0&|;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 8000000000000b7c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510de4c3; asc     Q   ;;
 7: len 8; hex 99bac303510de834; asc     Q  4;;
 8: SQL NULL;

Record lock, heap no 181 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7d; asc        };;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302701; asc     0' ;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 8000000000000b7d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510df00a; asc     Q   ;;
 7: len 8; hex 99bac303510df354; asc     Q  T;;
 8: SQL NULL;

Record lock, heap no 182 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7e; asc        ~;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302786; asc     0' ;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 8000000000000b7e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510dfb3c; asc     Q  <;;
 7: len 8; hex 99bac303510dfef2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 183 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b7f; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130280b; asc     0( ;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 8000000000000b7f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e06f4; asc     Q   ;;
 7: len 8; hex 99bac303510e0ab9; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 184 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b80; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302890; asc     0( ;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 8000000000000b80; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e1211; asc     Q   ;;
 7: len 8; hex 99bac303510e158b; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 185 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b81; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302915; asc     0) ;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 8000000000000b81; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e1d31; asc     Q  1;;
 7: len 8; hex 99bac303510e20eb; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 186 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b82; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130299a; asc     0) ;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 8000000000000b82; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e29f1; asc     Q ) ;;
 7: len 8; hex 99bac303510e2d62; asc     Q -b;;
 8: SQL NULL;

Record lock, heap no 187 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b83; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302a1f; asc     0* ;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 8000000000000b83; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e34c4; asc     Q 4 ;;
 7: len 8; hex 99bac303510e3821; asc     Q 8!;;
 8: SQL NULL;

Record lock, heap no 188 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b84; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302aa4; asc     0* ;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 8000000000000b84; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e3fdc; asc     Q ? ;;
 7: len 8; hex 99bac303510e4333; asc     Q C3;;
 8: SQL NULL;

Record lock, heap no 189 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b85; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302b29; asc     0+);;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 8000000000000b85; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e4b80; asc     Q K ;;
 7: len 8; hex 99bac303510e4ebb; asc     Q N ;;
 8: SQL NULL;

Record lock, heap no 190 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b86; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302bae; asc     0+ ;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 8000000000000b86; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e572b; asc     Q W+;;
 7: len 8; hex 99bac303510e5a12; asc     Q Z ;;
 8: SQL NULL;

Record lock, heap no 191 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b87; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302c33; asc     0,3;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 8000000000000b87; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e6197; asc     Q a ;;
 7: len 8; hex 99bac303510e64af; asc     Q d ;;
 8: SQL NULL;

Record lock, heap no 192 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b88; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302cb8; asc     0, ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 8000000000000b88; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e6ecc; asc     Q n ;;
 7: len 8; hex 99bac303510e73c9; asc     Q s ;;
 8: SQL NULL;

Record lock, heap no 193 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b89; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302d3d; asc     0-=;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 8000000000000b89; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e7c53; asc     Q |S;;
 7: len 8; hex 99bac303510e8006; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 194 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8a; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302dc2; asc     0- ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 8000000000000b8a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e8844; asc     Q  D;;
 7: len 8; hex 99bac303510e8bb5; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 195 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8b; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302e47; asc     0.G;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 8000000000000b8b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510e9369; asc     Q  i;;
 7: len 8; hex 99bac303510e96b8; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 196 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8c; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302ecc; asc     0. ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 8000000000000b8c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ea0b1; asc     Q   ;;
 7: len 8; hex 99bac303510ea41a; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 197 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8d; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302f51; asc     0/Q;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 8000000000000b8d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510eaf7a; asc     Q  z;;
 7: len 8; hex 99bac303510eb376; asc     Q  v;;
 8: SQL NULL;

Record lock, heap no 198 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8e; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001302fd6; asc     0/ ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 8000000000000b8e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ebbb6; asc     Q   ;;
 7: len 8; hex 99bac303510ebeea; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 199 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b8f; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130305b; asc     00[;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 8000000000000b8f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ec7aa; asc     Q   ;;
 7: len 8; hex 99bac303510ecb64; asc     Q  d;;
 8: SQL NULL;

Record lock, heap no 200 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b90; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013030e0; asc     00 ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 8000000000000b90; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ed30f; asc     Q   ;;
 7: len 8; hex 99bac303510ed6d4; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 201 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b91; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303165; asc     01e;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 8000000000000b91; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ede5b; asc     Q  [;;
 7: len 8; hex 99bac303510ee1a2; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 202 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b93; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130326f; asc     02o;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 8000000000000b93; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ef8d8; asc     Q   ;;
 7: len 8; hex 99bac303510efda4; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 203 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b94; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013032f4; asc     02 ;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 8000000000000b94; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510f071c; asc     Q   ;;
 7: len 8; hex 99bac303510f0a93; asc     Q   ;;
 8: SQL NULL;

Record lock, heap no 204 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b95; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303379; asc     03y;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 8000000000000b95; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510f2dea; asc     Q - ;;
 7: len 8; hex 99bac303510f3217; asc     Q 2 ;;
 8: SQL NULL;

Record lock, heap no 205 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b96; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013033fe; asc     03 ;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 8000000000000b96; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510f3ab9; asc     Q : ;;
 7: len 8; hex 99bac303510f3dfd; asc     Q = ;;
 8: SQL NULL;

Record lock, heap no 206 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b97; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303483; asc     04 ;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 8000000000000b97; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520003c6; asc     R   ;;
 7: len 8; hex 99bac30352000739; asc     R  9;;
 8: SQL NULL;

Record lock, heap no 207 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b98; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303508; asc     05 ;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 8000000000000b98; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352000fc0; asc     R   ;;
 7: len 8; hex 99bac303520012f8; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 208 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b99; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130358d; asc     05 ;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 8000000000000b99; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352001a6c; asc     R  l;;
 7: len 8; hex 99bac30352001dde; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 209 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9a; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303612; asc     06 ;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 8000000000000b9a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352002771; asc     R 'q;;
 7: len 8; hex 99bac30352002b1a; asc     R + ;;
 8: SQL NULL;

Record lock, heap no 210 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9b; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303697; asc     06 ;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 8000000000000b9b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200339b; asc     R 3 ;;
 7: len 8; hex 99bac303520036d6; asc     R 6 ;;
 8: SQL NULL;

Record lock, heap no 211 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9c; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000130371c; asc     07 ;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 8000000000000b9c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352003e00; asc     R > ;;
 7: len 8; hex 99bac303520040fc; asc     R @ ;;
 8: SQL NULL;

Record lock, heap no 212 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9d; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013037a1; asc     07 ;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 8000000000000b9d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352004862; asc     R Hb;;
 7: len 8; hex 99bac30352004b9d; asc     R K ;;
 8: SQL NULL;

Record lock, heap no 213 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9e; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303826; asc     08&;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 8000000000000b9e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520052bf; asc     R R ;;
 7: len 8; hex 99bac303520055e2; asc     R U ;;
 8: SQL NULL;

Record lock, heap no 214 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b9f; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013038ab; asc     08 ;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 8000000000000b9f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352005d5d; asc     R ]];;
 7: len 8; hex 99bac30352006128; asc     R a(;;
 8: SQL NULL;

Record lock, heap no 215 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba0; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303930; asc     090;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 8000000000000ba0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520068dd; asc     R h ;;
 7: len 8; hex 99bac30352006c96; asc     R l ;;
 8: SQL NULL;

Record lock, heap no 216 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba1; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013039b5; asc     09 ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 8000000000000ba1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520074b2; asc     R t ;;
 7: len 8; hex 99bac303520077eb; asc     R w ;;
 8: SQL NULL;

Record lock, heap no 217 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba2; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303a3a; asc     0::;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 8000000000000ba2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352007f65; asc     R  e;;
 7: len 8; hex 99bac30352008279; asc     R  y;;
 8: SQL NULL;

Record lock, heap no 218 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba3; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303abf; asc     0: ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 8000000000000ba3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352008967; asc     R  g;;
 7: len 8; hex 99bac30352008c78; asc     R  x;;
 8: SQL NULL;

Record lock, heap no 219 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba4; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303b44; asc     0;D;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 8000000000000ba4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520093a5; asc     R   ;;
 7: len 8; hex 99bac303520096b8; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 220 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303bc9; asc     0; ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 8000000000000ba5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352009f5b; asc     R  [;;
 7: len 8; hex 99bac3035200a2c1; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 221 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba6; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303c4e; asc     0<N;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 8000000000000ba6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200ab61; asc     R  a;;
 7: len 8; hex 99bac3035200af81; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 222 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba7; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303cd3; asc     0< ;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 8000000000000ba7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200b7f1; asc     R   ;;
 7: len 8; hex 99bac3035200bb39; asc     R  9;;
 8: SQL NULL;

Record lock, heap no 223 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba8; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303d58; asc     0=X;;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 8000000000000ba8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200c24d; asc     R  M;;
 7: len 8; hex 99bac3035200c570; asc     R  p;;
 8: SQL NULL;

Record lock, heap no 224 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000ba9; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303ddd; asc     0= ;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 8000000000000ba9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200ccb4; asc     R   ;;
 7: len 8; hex 99bac3035200cffc; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 225 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000baa; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303e62; asc     0>b;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 8000000000000baa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200d967; asc     R  g;;
 7: len 8; hex 99bac3035200dcea; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 226 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bab; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303ee7; asc     0> ;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 8000000000000bab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200e525; asc     R  %;;
 7: len 8; hex 99bac3035200e8b2; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 227 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bac; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001303f6c; asc     0?l;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 8000000000000bac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200f0d0; asc     R   ;;
 7: len 8; hex 99bac3035200f47e; asc     R  ~;;
 8: SQL NULL;

Record lock, heap no 228 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bad; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000131008f; asc     1  ;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 8000000000000bad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035200fca7; asc     R   ;;
 7: len 8; hex 99bac3035200fff9; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 229 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bae; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001310114; asc     1  ;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 8000000000000bae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520107aa; asc     R   ;;
 7: len 8; hex 99bac30352010acb; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 230 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000baf; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001310199; asc     1  ;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 8000000000000baf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352011330; asc     R  0;;
 7: len 8; hex 99bac3035201169c; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 231 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb0; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000131021e; asc     1  ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 8000000000000bb0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352011ec3; asc     R   ;;
 7: len 8; hex 99bac3035201222a; asc     R "*;;
 8: SQL NULL;

Record lock, heap no 232 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb1; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013102a3; asc     1  ;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 8000000000000bb1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352012a07; asc     R * ;;
 7: len 8; hex 99bac30352012d80; asc     R - ;;
 8: SQL NULL;

Record lock, heap no 233 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb2; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001310328; asc     1 (;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 8000000000000bb2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352013955; asc     R 9U;;
 7: len 8; hex 99bac30352013d6c; asc     R =l;;
 8: SQL NULL;

Record lock, heap no 234 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb3; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013103ad; asc     1  ;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 8000000000000bb3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520147bd; asc     R G ;;
 7: len 8; hex 99bac30352014c1f; asc     R L ;;
 8: SQL NULL;

Record lock, heap no 235 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb4; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001310432; asc     1 2;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 8000000000000bb4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520153d0; asc     R S ;;
 7: len 8; hex 99bac303520156fb; asc     R V ;;
 8: SQL NULL;

Record lock, heap no 236 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb5; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013104b7; asc     1  ;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 8000000000000bb5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352015e7a; asc     R ^z;;
 7: len 8; hex 99bac303520161a0; asc     R a ;;
 8: SQL NULL;

Record lock, heap no 237 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb6; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 0200000131053c; asc     1 <;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 8000000000000bb6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352016ac5; asc     R j ;;
 7: len 8; hex 99bac303520189ce; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 238 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb7; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 020000013105c1; asc     1  ;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 8000000000000bb7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035201929f; asc     R   ;;
 7: len 8; hex 99bac303520195e9; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 239 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb8; asc         ;;
 1: len 6; hex 00000000bdfb; asc       ;;
 2: len 7; hex 02000001310646; asc     1 F;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 8000000000000bb8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352019dc0; asc     R   ;;
 7: len 8; hex 99bac3035201a0ea; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 240 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bb9; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 010000018709bd; asc        ;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 8000000000000bb9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035201ea39; asc     R  9;;
 7: len 8; hex 99bac3035201f3a6; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 241 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bba; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870a40; asc       @;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 8000000000000bba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303520293c3; asc     R   ;;
 7: len 8; hex 99bac303520297b5; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 242 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bbb; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870ac3; asc        ;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 8000000000000bbb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352029fa2; asc     R   ;;
 7: len 8; hex 99bac3035202a30c; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 243 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bbc; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870b46; asc       F;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 8000000000000bbc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202aa4e; asc     R  N;;
 7: len 8; hex 99bac3035202ad98; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 244 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bbd; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870bc9; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 8; hex 8000000000000bbd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202b599; asc     R   ;;
 7: len 8; hex 99bac3035202b910; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 245 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bbe; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870c4c; asc       L;;
 3: len 8; hex 8000000000000006; asc         ;;
 4: len 8; hex 8000000000000bbe; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202c0b0; asc     R   ;;
 7: len 8; hex 99bac3035202c404; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 246 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bbf; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870ccf; asc        ;;
 3: len 8; hex 8000000000000007; asc         ;;
 4: len 8; hex 8000000000000bbf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202cb35; asc     R  5;;
 7: len 8; hex 99bac3035202ce96; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 247 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bc0; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870d52; asc       R;;
 3: len 8; hex 8000000000000008; asc         ;;
 4: len 8; hex 8000000000000bc0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202d60d; asc     R   ;;
 7: len 8; hex 99bac3035202d93a; asc     R  :;;
 8: SQL NULL;

Record lock, heap no 248 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bc1; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870dd5; asc        ;;
 3: len 8; hex 8000000000000009; asc         ;;
 4: len 8; hex 8000000000000bc1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202e014; asc     R   ;;
 7: len 8; hex 99bac3035202e305; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 249 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bc2; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870e58; asc       X;;
 3: len 8; hex 800000000000000a; asc         ;;
 4: len 8; hex 8000000000000bc2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202eae8; asc     R   ;;
 7: len 8; hex 99bac3035202ef35; asc     R  5;;
 8: SQL NULL;

Record lock, heap no 250 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bc3; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870edb; asc        ;;
 3: len 8; hex 800000000000000b; asc         ;;
 4: len 8; hex 8000000000000bc3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3035202f71f; asc     R   ;;
 7: len 8; hex 99bac3035202fabb; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 251 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000bc4; asc         ;;
 1: len 6; hex 00000000be1d; asc       ;;
 2: len 7; hex 01000001870f5e; asc       ^;;
 3: len 8; hex 800000000000000c; asc         ;;
 4: len 8; hex 8000000000000bc4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac30352030236; asc     R  6;;
 7: len 8; hex 99bac303520305a3; asc     R   ;;
 8: SQL NULL;

Record lock, heap no 252 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000b92; asc         ;;
 1: len 6; hex 00000000be55; asc      U;;
 2: len 7; hex 01000001192d8f; asc      - ;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 8000000000000b92; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303510ee9af; asc     Q   ;;
 7: len 8; hex 99bac303510eeee2; asc     Q   ;;
 8: len 8; hex 99bac3035307478f; asc     S G ;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 40 page no 12 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 48731 lock mode S locks rec but not gap waiting
Record lock, heap no 198 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 00000000be6c; asc      l;;
 2: len 7; hex 0100000199071b; asc        ;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d353733343934383635; asc tk-573494865;;
 5: len 8; hex 99bac303530ec4e9; asc     S   ;;
 6: len 8; hex 99bac303530ec4e9; asc     S   ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```

---

## V1 — LATEST DETECTED DEADLOCK

```
LATEST DETECTED DEADLOCK
------------------------
2026-09-01 16:14:36 135358810506816
*** (1) TRANSACTION:
TRANSACTION 57269, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 1
MySQL thread id 1100, OS thread handle 135358650984000, query id 457856 172.20.0.3 root updating
UPDATE notification SET read_at = NOW(6) WHERE id = 927

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 44 page no 13 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 57269 lock_mode X locks rec but not gap
Record lock, heap no 233 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 00000000dfb5; asc       ;;
 2: len 7; hex 01000001d02f76; asc      /v;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343835383031303536; asc tk-485801056;;
 5: len 8; hex 99bac303a20614c7; asc         ;;
 6: len 8; hex 99bac303a20614c7; asc         ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 42 page no 15 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 57269 lock_mode X locks rec but not gap waiting
Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843ee7; asc      > ;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 800000000000039f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ecba3; asc         ;;
 7: len 8; hex 99bac303990ecfcb; asc         ;;
 8: SQL NULL;


*** (2) TRANSACTION:
TRANSACTION 57245, ACTIVE 2 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 65 lock struct(s), heap size 24696, 5230 row lock(s), undo log entries 1279
MySQL thread id 1368, OS thread handle 135358653097536, query id 459190 172.20.0.1 root update
INSERT INTO notification (account_id, request_id, status, created_at) VALUES (927, 3927, 'CREATED', NOW(6))

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 42 page no 15 n bits 200 index PRIMARY of table `deadlock_lab`.`notification` trx id 57245 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000370; asc        p;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184267c; asc      &|;;
 3: len 8; hex 8000000000000370; asc        p;;
 4: len 8; hex 8000000000000370; asc        p;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c6021; asc       `!;;
 7: len 8; hex 99bac303990c6367; asc       cg;;
 8: SQL NULL;

Record lock, heap no 3 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000371; asc        q;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842701; asc      ' ;;
 3: len 8; hex 8000000000000371; asc        q;;
 4: len 8; hex 8000000000000371; asc        q;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c6847; asc       hG;;
 7: len 8; hex 99bac303990c6b6f; asc       ko;;
 8: SQL NULL;

Record lock, heap no 4 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000372; asc        r;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842786; asc      ' ;;
 3: len 8; hex 8000000000000372; asc        r;;
 4: len 8; hex 8000000000000372; asc        r;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c6fdf; asc       o ;;
 7: len 8; hex 99bac303990c72f8; asc       r ;;
 8: SQL NULL;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000373; asc        s;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184280b; asc      ( ;;
 3: len 8; hex 8000000000000373; asc        s;;
 4: len 8; hex 8000000000000373; asc        s;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c775a; asc       wZ;;
 7: len 8; hex 99bac303990c7b4c; asc       {L;;
 8: SQL NULL;

Record lock, heap no 6 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000374; asc        t;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842890; asc      ( ;;
 3: len 8; hex 8000000000000374; asc        t;;
 4: len 8; hex 8000000000000374; asc        t;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c83fe; asc         ;;
 7: len 8; hex 99bac303990c8a0e; asc         ;;
 8: SQL NULL;

Record lock, heap no 7 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000375; asc        u;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842915; asc      ) ;;
 3: len 8; hex 8000000000000375; asc        u;;
 4: len 8; hex 8000000000000375; asc        u;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c92d1; asc         ;;
 7: len 8; hex 99bac303990c96bb; asc         ;;
 8: SQL NULL;

Record lock, heap no 8 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000376; asc        v;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184299a; asc      ) ;;
 3: len 8; hex 8000000000000376; asc        v;;
 4: len 8; hex 8000000000000376; asc        v;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990c9f5f; asc        _;;
 7: len 8; hex 99bac303990ca407; asc         ;;
 8: SQL NULL;

Record lock, heap no 9 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000377; asc        w;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842a1f; asc      * ;;
 3: len 8; hex 8000000000000377; asc        w;;
 4: len 8; hex 8000000000000377; asc        w;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990cb136; asc        6;;
 7: len 8; hex 99bac303990cb4aa; asc         ;;
 8: SQL NULL;

Record lock, heap no 10 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000378; asc        x;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842aa4; asc      * ;;
 3: len 8; hex 8000000000000378; asc        x;;
 4: len 8; hex 8000000000000378; asc        x;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990cbb2e; asc        .;;
 7: len 8; hex 99bac303990cc389; asc         ;;
 8: SQL NULL;

Record lock, heap no 11 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000379; asc        y;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842b29; asc      +);;
 3: len 8; hex 8000000000000379; asc        y;;
 4: len 8; hex 8000000000000379; asc        y;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ccd46; asc        F;;
 7: len 8; hex 99bac303990cd199; asc         ;;
 8: SQL NULL;

Record lock, heap no 12 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037a; asc        z;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842bae; asc      + ;;
 3: len 8; hex 800000000000037a; asc        z;;
 4: len 8; hex 800000000000037a; asc        z;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990cd74b; asc        K;;
 7: len 8; hex 99bac303990cdaf4; asc         ;;
 8: SQL NULL;

Record lock, heap no 13 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037b; asc        {;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842c33; asc      ,3;;
 3: len 8; hex 800000000000037b; asc        {;;
 4: len 8; hex 800000000000037b; asc        {;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990cdf92; asc         ;;
 7: len 8; hex 99bac303990ce2ea; asc         ;;
 8: SQL NULL;

Record lock, heap no 14 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037c; asc        |;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842cb8; asc      , ;;
 3: len 8; hex 800000000000037c; asc        |;;
 4: len 8; hex 800000000000037c; asc        |;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990cfaae; asc         ;;
 7: len 8; hex 99bac303990cfe2f; asc        /;;
 8: SQL NULL;

Record lock, heap no 15 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037d; asc        };;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842d3d; asc      -=;;
 3: len 8; hex 800000000000037d; asc        };;
 4: len 8; hex 800000000000037d; asc        };;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d06ff; asc         ;;
 7: len 8; hex 99bac303990d0b3c; asc        <;;
 8: SQL NULL;

Record lock, heap no 16 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037e; asc        ~;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842dc2; asc      - ;;
 3: len 8; hex 800000000000037e; asc        ~;;
 4: len 8; hex 800000000000037e; asc        ~;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d1439; asc        9;;
 7: len 8; hex 99bac303990d1872; asc        r;;
 8: SQL NULL;

Record lock, heap no 17 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000037f; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842e47; asc      .G;;
 3: len 8; hex 800000000000037f; asc         ;;
 4: len 8; hex 800000000000037f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d2294; asc       " ;;
 7: len 8; hex 99bac303990d2680; asc       & ;;
 8: SQL NULL;

Record lock, heap no 18 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000380; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842ecc; asc      . ;;
 3: len 8; hex 8000000000000380; asc         ;;
 4: len 8; hex 8000000000000380; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d2f40; asc       /@;;
 7: len 8; hex 99bac303990d32ec; asc       2 ;;
 8: SQL NULL;

Record lock, heap no 20 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000382; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001842fd6; asc      / ;;
 3: len 8; hex 8000000000000382; asc         ;;
 4: len 8; hex 8000000000000382; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d462a; asc       F*;;
 7: len 8; hex 99bac303990d4a4f; asc       JO;;
 8: SQL NULL;

Record lock, heap no 21 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000383; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184305b; asc      0[;;
 3: len 8; hex 8000000000000383; asc         ;;
 4: len 8; hex 8000000000000383; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d4efa; asc       N ;;
 7: len 8; hex 99bac303990d51f5; asc       Q ;;
 8: SQL NULL;

Record lock, heap no 22 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000384; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018430e0; asc      0 ;;
 3: len 8; hex 8000000000000384; asc         ;;
 4: len 8; hex 8000000000000384; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d5996; asc       Y ;;
 7: len 8; hex 99bac303990d5cd8; asc       \ ;;
 8: SQL NULL;

Record lock, heap no 23 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000385; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843165; asc      1e;;
 3: len 8; hex 8000000000000385; asc         ;;
 4: len 8; hex 8000000000000385; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d6215; asc       b ;;
 7: len 8; hex 99bac303990d65ea; asc       e ;;
 8: SQL NULL;

Record lock, heap no 24 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000386; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018431ea; asc      1 ;;
 3: len 8; hex 8000000000000386; asc         ;;
 4: len 8; hex 8000000000000386; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d6c0e; asc       l ;;
 7: len 8; hex 99bac303990d6f57; asc       oW;;
 8: SQL NULL;

Record lock, heap no 25 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000387; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184326f; asc      2o;;
 3: len 8; hex 8000000000000387; asc         ;;
 4: len 8; hex 8000000000000387; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ddac1; asc         ;;
 7: len 8; hex 99bac303990ddf4e; asc        N;;
 8: SQL NULL;

Record lock, heap no 26 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000388; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018432f4; asc      2 ;;
 3: len 8; hex 8000000000000388; asc         ;;
 4: len 8; hex 8000000000000388; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990de852; asc        R;;
 7: len 8; hex 99bac303990dec1e; asc         ;;
 8: SQL NULL;

Record lock, heap no 27 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000389; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843379; asc      3y;;
 3: len 8; hex 8000000000000389; asc         ;;
 4: len 8; hex 8000000000000389; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990df18f; asc         ;;
 7: len 8; hex 99bac303990df6c8; asc         ;;
 8: SQL NULL;

Record lock, heap no 28 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038a; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018433fe; asc      3 ;;
 3: len 8; hex 800000000000038a; asc         ;;
 4: len 8; hex 800000000000038a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990dfb21; asc        !;;
 7: len 8; hex 99bac303990dfe11; asc         ;;
 8: SQL NULL;

Record lock, heap no 29 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038b; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843483; asc      4 ;;
 3: len 8; hex 800000000000038b; asc         ;;
 4: len 8; hex 800000000000038b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e025e; asc        ^;;
 7: len 8; hex 99bac303990e0550; asc        P;;
 8: SQL NULL;

Record lock, heap no 30 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038c; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843508; asc      5 ;;
 3: len 8; hex 800000000000038c; asc         ;;
 4: len 8; hex 800000000000038c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e097f; asc         ;;
 7: len 8; hex 99bac303990e0d3d; asc        =;;
 8: SQL NULL;

Record lock, heap no 31 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038d; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184358d; asc      5 ;;
 3: len 8; hex 800000000000038d; asc         ;;
 4: len 8; hex 800000000000038d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e2235; asc       "5;;
 7: len 8; hex 99bac303990e267e; asc       &~;;
 8: SQL NULL;

Record lock, heap no 32 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038e; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843612; asc      6 ;;
 3: len 8; hex 800000000000038e; asc         ;;
 4: len 8; hex 800000000000038e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e2fba; asc       / ;;
 7: len 8; hex 99bac303990e3418; asc       4 ;;
 8: SQL NULL;

Record lock, heap no 33 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000038f; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843697; asc      6 ;;
 3: len 8; hex 800000000000038f; asc         ;;
 4: len 8; hex 800000000000038f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e3d1c; asc       = ;;
 7: len 8; hex 99bac303990e4147; asc       AG;;
 8: SQL NULL;

Record lock, heap no 34 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000390; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000184371c; asc      7 ;;
 3: len 8; hex 8000000000000390; asc         ;;
 4: len 8; hex 8000000000000390; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e497c; asc       I|;;
 7: len 8; hex 99bac303990e4d30; asc       M0;;
 8: SQL NULL;

Record lock, heap no 35 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000391; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018437a1; asc      7 ;;
 3: len 8; hex 8000000000000391; asc         ;;
 4: len 8; hex 8000000000000391; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e5228; asc       R(;;
 7: len 8; hex 99bac303990e5507; asc       U ;;
 8: SQL NULL;

Record lock, heap no 36 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000392; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843826; asc      8&;;
 3: len 8; hex 8000000000000392; asc         ;;
 4: len 8; hex 8000000000000392; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e5910; asc       Y ;;
 7: len 8; hex 99bac303990e5bf3; asc       [ ;;
 8: SQL NULL;

Record lock, heap no 37 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000393; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018438ab; asc      8 ;;
 3: len 8; hex 8000000000000393; asc         ;;
 4: len 8; hex 8000000000000393; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e600c; asc       ` ;;
 7: len 8; hex 99bac303990e6317; asc       c ;;
 8: SQL NULL;

Record lock, heap no 38 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000394; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843930; asc      90;;
 3: len 8; hex 8000000000000394; asc         ;;
 4: len 8; hex 8000000000000394; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e6749; asc       gI;;
 7: len 8; hex 99bac303990e6aa8; asc       j ;;
 8: SQL NULL;

Record lock, heap no 39 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000395; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018439b5; asc      9 ;;
 3: len 8; hex 8000000000000395; asc         ;;
 4: len 8; hex 8000000000000395; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e75eb; asc       u ;;
 7: len 8; hex 99bac303990e79e0; asc       y ;;
 8: SQL NULL;

Record lock, heap no 40 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000396; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843a3a; asc      ::;;
 3: len 8; hex 8000000000000396; asc         ;;
 4: len 8; hex 8000000000000396; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e81f5; asc         ;;
 7: len 8; hex 99bac303990e8508; asc         ;;
 8: SQL NULL;

Record lock, heap no 41 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000397; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843abf; asc      : ;;
 3: len 8; hex 8000000000000397; asc         ;;
 4: len 8; hex 8000000000000397; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e8977; asc        w;;
 7: len 8; hex 99bac303990e8c7b; asc        {;;
 8: SQL NULL;

Record lock, heap no 42 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000398; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843b44; asc      ;D;;
 3: len 8; hex 8000000000000398; asc         ;;
 4: len 8; hex 8000000000000398; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e90e1; asc         ;;
 7: len 8; hex 99bac303990e93ab; asc         ;;
 8: SQL NULL;

Record lock, heap no 43 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000399; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843bc9; asc      ; ;;
 3: len 8; hex 8000000000000399; asc         ;;
 4: len 8; hex 8000000000000399; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e97da; asc         ;;
 7: len 8; hex 99bac303990e9abf; asc         ;;
 8: SQL NULL;

Record lock, heap no 44 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039a; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843c4e; asc      <N;;
 3: len 8; hex 800000000000039a; asc         ;;
 4: len 8; hex 800000000000039a; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990e9eef; asc         ;;
 7: len 8; hex 99bac303990ea1d8; asc         ;;
 8: SQL NULL;

Record lock, heap no 45 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039b; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843cd3; asc      < ;;
 3: len 8; hex 800000000000039b; asc         ;;
 4: len 8; hex 800000000000039b; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ea632; asc        2;;
 7: len 8; hex 99bac303990eaa96; asc         ;;
 8: SQL NULL;

Record lock, heap no 46 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039c; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843d58; asc      =X;;
 3: len 8; hex 800000000000039c; asc         ;;
 4: len 8; hex 800000000000039c; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990eafb2; asc         ;;
 7: len 8; hex 99bac303990eb2b2; asc         ;;
 8: SQL NULL;

Record lock, heap no 47 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039d; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843ddd; asc      = ;;
 3: len 8; hex 800000000000039d; asc         ;;
 4: len 8; hex 800000000000039d; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990eb729; asc        );;
 7: len 8; hex 99bac303990eba2b; asc        +;;
 8: SQL NULL;

Record lock, heap no 48 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039e; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843e62; asc      >b;;
 3: len 8; hex 800000000000039e; asc         ;;
 4: len 8; hex 800000000000039e; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ebea1; asc         ;;
 7: len 8; hex 99bac303990ec181; asc         ;;
 8: SQL NULL;

Record lock, heap no 49 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843ee7; asc      > ;;
 3: len 8; hex 800000000000039f; asc         ;;
 4: len 8; hex 800000000000039f; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ecba3; asc         ;;
 7: len 8; hex 99bac303990ecfcb; asc         ;;
 8: SQL NULL;

Record lock, heap no 50 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a0; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001843f6c; asc      ?l;;
 3: len 8; hex 80000000000003a0; asc         ;;
 4: len 8; hex 80000000000003a0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990edce0; asc         ;;
 7: len 8; hex 99bac303990ee1b3; asc         ;;
 8: SQL NULL;

Record lock, heap no 51 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a1; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189008f; asc        ;;
 3: len 8; hex 80000000000003a1; asc         ;;
 4: len 8; hex 80000000000003a1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ee930; asc        0;;
 7: len 8; hex 99bac303990eeef2; asc         ;;
 8: SQL NULL;

Record lock, heap no 52 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a2; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890114; asc        ;;
 3: len 8; hex 80000000000003a2; asc         ;;
 4: len 8; hex 80000000000003a2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990ef739; asc        9;;
 7: len 8; hex 99bac303990efb5c; asc        \;;
 8: SQL NULL;

Record lock, heap no 53 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a3; asc         ;;
 1: len 6; hex 00000000df37; asc      7;;
 2: len 7; hex 020000016402d0; asc     d  ;;
 3: len 8; hex 80000000000003a3; asc         ;;
 4: len 8; hex 80000000000003a3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990f0406; asc         ;;
 7: len 8; hex 99bac303990f0f0e; asc         ;;
 8: len 8; hex 99bac3039d08e2ac; asc         ;;

Record lock, heap no 54 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a4; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189021e; asc        ;;
 3: len 8; hex 80000000000003a4; asc         ;;
 4: len 8; hex 80000000000003a4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990f16b9; asc         ;;
 7: len 8; hex 99bac303990f1cf9; asc         ;;
 8: SQL NULL;

Record lock, heap no 55 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a5; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018902a3; asc        ;;
 3: len 8; hex 80000000000003a5; asc         ;;
 4: len 8; hex 80000000000003a5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990f23f3; asc       # ;;
 7: len 8; hex 99bac303990f2832; asc       (2;;
 8: SQL NULL;

Record lock, heap no 57 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a7; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018903ad; asc        ;;
 3: len 8; hex 80000000000003a7; asc         ;;
 4: len 8; hex 80000000000003a7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990f3fb3; asc       ? ;;
 7: len 8; hex 99bac3039a00019b; asc         ;;
 8: SQL NULL;

Record lock, heap no 58 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a8; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890432; asc       2;;
 3: len 8; hex 80000000000003a8; asc         ;;
 4: len 8; hex 80000000000003a8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a000866; asc        f;;
 7: len 8; hex 99bac3039a000c86; asc         ;;
 8: SQL NULL;

Record lock, heap no 59 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a9; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018904b7; asc        ;;
 3: len 8; hex 80000000000003a9; asc         ;;
 4: len 8; hex 80000000000003a9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0019f0; asc         ;;
 7: len 8; hex 99bac3039a001e04; asc         ;;
 8: SQL NULL;

Record lock, heap no 60 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003aa; asc         ;;
 1: len 6; hex 00000000dee5; asc       ;;
 2: len 7; hex 010000012f07aa; asc     /  ;;
 3: len 8; hex 80000000000003aa; asc         ;;
 4: len 8; hex 80000000000003aa; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00286d; asc       (m;;
 7: len 8; hex 99bac3039a002c13; asc       , ;;
 8: len 8; hex 99bac3039b092e29; asc       .);;

Record lock, heap no 61 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ab; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018905c1; asc        ;;
 3: len 8; hex 80000000000003ab; asc         ;;
 4: len 8; hex 80000000000003ab; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0035c3; asc       5 ;;
 7: len 8; hex 99bac3039a003e98; asc       > ;;
 8: SQL NULL;

Record lock, heap no 62 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ac; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890646; asc       F;;
 3: len 8; hex 80000000000003ac; asc         ;;
 4: len 8; hex 80000000000003ac; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a004705; asc       G ;;
 7: len 8; hex 99bac3039a004a57; asc       JW;;
 8: SQL NULL;

Record lock, heap no 63 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ad; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018906cb; asc        ;;
 3: len 8; hex 80000000000003ad; asc         ;;
 4: len 8; hex 80000000000003ad; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a005126; asc       Q&;;
 7: len 8; hex 99bac3039a00556d; asc       Um;;
 8: SQL NULL;

Record lock, heap no 64 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ae; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890750; asc       P;;
 3: len 8; hex 80000000000003ae; asc         ;;
 4: len 8; hex 80000000000003ae; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a005c30; asc       \0;;
 7: len 8; hex 99bac3039a005f4e; asc       _N;;
 8: SQL NULL;

Record lock, heap no 65 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003af; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018907d5; asc        ;;
 3: len 8; hex 80000000000003af; asc         ;;
 4: len 8; hex 80000000000003af; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a006426; asc       d&;;
 7: len 8; hex 99bac3039a006729; asc       g);;
 8: SQL NULL;

Record lock, heap no 66 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b0; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189085a; asc       Z;;
 3: len 8; hex 80000000000003b0; asc         ;;
 4: len 8; hex 80000000000003b0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a006bb4; asc       k ;;
 7: len 8; hex 99bac3039a006f31; asc       o1;;
 8: SQL NULL;

Record lock, heap no 67 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b1; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018908df; asc        ;;
 3: len 8; hex 80000000000003b1; asc         ;;
 4: len 8; hex 80000000000003b1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a007719; asc       w ;;
 7: len 8; hex 99bac3039a007b5e; asc       {^;;
 8: SQL NULL;

Record lock, heap no 68 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b2; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890964; asc       d;;
 3: len 8; hex 80000000000003b2; asc         ;;
 4: len 8; hex 80000000000003b2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a008953; asc        S;;
 7: len 8; hex 99bac3039a008d34; asc        4;;
 8: SQL NULL;

Record lock, heap no 69 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b3; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018909e9; asc        ;;
 3: len 8; hex 80000000000003b3; asc         ;;
 4: len 8; hex 80000000000003b3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00952c; asc        ,;;
 7: len 8; hex 99bac3039a009a96; asc         ;;
 8: SQL NULL;

Record lock, heap no 70 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b4; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890a6e; asc       n;;
 3: len 8; hex 80000000000003b4; asc         ;;
 4: len 8; hex 80000000000003b4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00a150; asc        P;;
 7: len 8; hex 99bac3039a00a480; asc         ;;
 8: SQL NULL;

Record lock, heap no 71 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b5; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890af3; asc        ;;
 3: len 8; hex 80000000000003b5; asc         ;;
 4: len 8; hex 80000000000003b5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00a943; asc        C;;
 7: len 8; hex 99bac3039a00ac57; asc        W;;
 8: SQL NULL;

Record lock, heap no 72 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b6; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890b78; asc       x;;
 3: len 8; hex 80000000000003b6; asc         ;;
 4: len 8; hex 80000000000003b6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00b2c4; asc         ;;
 7: len 8; hex 99bac3039a00b696; asc         ;;
 8: SQL NULL;

Record lock, heap no 73 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b7; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890bfd; asc        ;;
 3: len 8; hex 80000000000003b7; asc         ;;
 4: len 8; hex 80000000000003b7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00bc1c; asc         ;;
 7: len 8; hex 99bac3039a00bf76; asc        v;;
 8: SQL NULL;

Record lock, heap no 74 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b8; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890c82; asc        ;;
 3: len 8; hex 80000000000003b8; asc         ;;
 4: len 8; hex 80000000000003b8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00c43b; asc        ;;;
 7: len 8; hex 99bac3039a00c74b; asc        K;;
 8: SQL NULL;

Record lock, heap no 75 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003b9; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890d07; asc        ;;
 3: len 8; hex 80000000000003b9; asc         ;;
 4: len 8; hex 80000000000003b9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00cca9; asc         ;;
 7: len 8; hex 99bac3039a00d065; asc        e;;
 8: SQL NULL;

Record lock, heap no 76 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ba; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890d8c; asc        ;;
 3: len 8; hex 80000000000003ba; asc         ;;
 4: len 8; hex 80000000000003ba; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00d713; asc         ;;
 7: len 8; hex 99bac3039a00da46; asc        F;;
 8: SQL NULL;

Record lock, heap no 77 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bb; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890e11; asc        ;;
 3: len 8; hex 80000000000003bb; asc         ;;
 4: len 8; hex 80000000000003bb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00dfba; asc         ;;
 7: len 8; hex 99bac3039a00e2f5; asc         ;;
 8: SQL NULL;

Record lock, heap no 78 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bc; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890e96; asc        ;;
 3: len 8; hex 80000000000003bc; asc         ;;
 4: len 8; hex 80000000000003bc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00e7cd; asc         ;;
 7: len 8; hex 99bac3039a00eb16; asc         ;;
 8: SQL NULL;

Record lock, heap no 79 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bd; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890f1b; asc        ;;
 3: len 8; hex 80000000000003bd; asc         ;;
 4: len 8; hex 80000000000003bd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00f08c; asc         ;;
 7: len 8; hex 99bac3039a00f41c; asc         ;;
 8: SQL NULL;

Record lock, heap no 80 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003be; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001890fa0; asc        ;;
 3: len 8; hex 80000000000003be; asc         ;;
 4: len 8; hex 80000000000003be; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a00f95b; asc        [;;
 7: len 8; hex 99bac3039a00fc8b; asc         ;;
 8: SQL NULL;

Record lock, heap no 81 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003bf; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891025; asc       %;;
 3: len 8; hex 80000000000003bf; asc         ;;
 4: len 8; hex 80000000000003bf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0101bc; asc         ;;
 7: len 8; hex 99bac3039a0104e9; asc         ;;
 8: SQL NULL;

Record lock, heap no 82 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c0; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018910aa; asc        ;;
 3: len 8; hex 80000000000003c0; asc         ;;
 4: len 8; hex 80000000000003c0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a010c81; asc         ;;
 7: len 8; hex 99bac3039a0110cc; asc         ;;
 8: SQL NULL;

Record lock, heap no 83 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c1; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189112f; asc       /;;
 3: len 8; hex 80000000000003c1; asc         ;;
 4: len 8; hex 80000000000003c1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a011a19; asc         ;;
 7: len 8; hex 99bac3039a011db3; asc         ;;
 8: SQL NULL;

Record lock, heap no 84 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c2; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018911b4; asc        ;;
 3: len 8; hex 80000000000003c2; asc         ;;
 4: len 8; hex 80000000000003c2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0125c7; asc       % ;;
 7: len 8; hex 99bac3039a0128d9; asc       ( ;;
 8: SQL NULL;

Record lock, heap no 85 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c3; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891239; asc       9;;
 3: len 8; hex 80000000000003c3; asc         ;;
 4: len 8; hex 80000000000003c3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01323e; asc       2>;;
 7: len 8; hex 99bac3039a013595; asc       5 ;;
 8: SQL NULL;

Record lock, heap no 86 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c4; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018912be; asc        ;;
 3: len 8; hex 80000000000003c4; asc         ;;
 4: len 8; hex 80000000000003c4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a013dcb; asc       = ;;
 7: len 8; hex 99bac3039a014206; asc       B ;;
 8: SQL NULL;

Record lock, heap no 87 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c5; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891343; asc       C;;
 3: len 8; hex 80000000000003c5; asc         ;;
 4: len 8; hex 80000000000003c5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0148eb; asc       H ;;
 7: len 8; hex 99bac3039a014c04; asc       L ;;
 8: SQL NULL;

Record lock, heap no 88 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c6; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018913c8; asc        ;;
 3: len 8; hex 80000000000003c6; asc         ;;
 4: len 8; hex 80000000000003c6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0150fb; asc       P ;;
 7: len 8; hex 99bac3039a015415; asc       T ;;
 8: SQL NULL;

Record lock, heap no 89 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c7; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189144d; asc       M;;
 3: len 8; hex 80000000000003c7; asc         ;;
 4: len 8; hex 80000000000003c7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a018580; asc         ;;
 7: len 8; hex 99bac3039a018ab8; asc         ;;
 8: SQL NULL;

Record lock, heap no 90 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c8; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018914d2; asc        ;;
 3: len 8; hex 80000000000003c8; asc         ;;
 4: len 8; hex 80000000000003c8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0196b2; asc         ;;
 7: len 8; hex 99bac3039a019b95; asc         ;;
 8: SQL NULL;

Record lock, heap no 91 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003c9; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891557; asc       W;;
 3: len 8; hex 80000000000003c9; asc         ;;
 4: len 8; hex 80000000000003c9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01a552; asc        R;;
 7: len 8; hex 99bac3039a01a9e1; asc         ;;
 8: SQL NULL;

Record lock, heap no 92 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ca; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018915dc; asc        ;;
 3: len 8; hex 80000000000003ca; asc         ;;
 4: len 8; hex 80000000000003ca; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01b3d7; asc         ;;
 7: len 8; hex 99bac3039a01b769; asc        i;;
 8: SQL NULL;

Record lock, heap no 93 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cb; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891661; asc       a;;
 3: len 8; hex 80000000000003cb; asc         ;;
 4: len 8; hex 80000000000003cb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01c160; asc        `;;
 7: len 8; hex 99bac3039a01c5ff; asc         ;;
 8: SQL NULL;

Record lock, heap no 94 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cc; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018916e6; asc        ;;
 3: len 8; hex 80000000000003cc; asc         ;;
 4: len 8; hex 80000000000003cc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01d017; asc         ;;
 7: len 8; hex 99bac3039a01d4d3; asc         ;;
 8: SQL NULL;

Record lock, heap no 95 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cd; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189176b; asc       k;;
 3: len 8; hex 80000000000003cd; asc         ;;
 4: len 8; hex 80000000000003cd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01dcd2; asc         ;;
 7: len 8; hex 99bac3039a01e0c9; asc         ;;
 8: SQL NULL;

Record lock, heap no 96 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ce; asc         ;;
 1: len 6; hex 00000000deca; asc       ;;
 2: len 7; hex 01000001303124; asc     01$;;
 3: len 8; hex 80000000000003ce; asc         ;;
 4: len 8; hex 80000000000003ce; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01e9e3; asc         ;;
 7: len 8; hex 99bac3039a01ed79; asc        y;;
 8: len 8; hex 99bac3039a03f95f; asc        _;;

Record lock, heap no 97 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003cf; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891875; asc       u;;
 3: len 8; hex 80000000000003cf; asc         ;;
 4: len 8; hex 80000000000003cf; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a01f594; asc         ;;
 7: len 8; hex 99bac3039a01fa82; asc         ;;
 8: SQL NULL;

Record lock, heap no 98 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d0; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018918fa; asc        ;;
 3: len 8; hex 80000000000003d0; asc         ;;
 4: len 8; hex 80000000000003d0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a020909; asc         ;;
 7: len 8; hex 99bac3039a020d34; asc        4;;
 8: SQL NULL;

Record lock, heap no 99 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d1; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189197f; asc        ;;
 3: len 8; hex 80000000000003d1; asc         ;;
 4: len 8; hex 80000000000003d1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02166e; asc        n;;
 7: len 8; hex 99bac3039a021b41; asc        A;;
 8: SQL NULL;

Record lock, heap no 100 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d2; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891a04; asc        ;;
 3: len 8; hex 80000000000003d2; asc         ;;
 4: len 8; hex 80000000000003d2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02250c; asc       % ;;
 7: len 8; hex 99bac3039a0229b2; asc       ) ;;
 8: SQL NULL;

Record lock, heap no 101 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d3; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891a89; asc        ;;
 3: len 8; hex 80000000000003d3; asc         ;;
 4: len 8; hex 80000000000003d3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a023212; asc       2 ;;
 7: len 8; hex 99bac3039a02371e; asc       7 ;;
 8: SQL NULL;

Record lock, heap no 102 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d4; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891b0e; asc        ;;
 3: len 8; hex 80000000000003d4; asc         ;;
 4: len 8; hex 80000000000003d4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0240f2; asc       @ ;;
 7: len 8; hex 99bac3039a0248fc; asc       H ;;
 8: SQL NULL;

Record lock, heap no 103 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d5; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891b93; asc        ;;
 3: len 8; hex 80000000000003d5; asc         ;;
 4: len 8; hex 80000000000003d5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a025043; asc       PC;;
 7: len 8; hex 99bac3039a025666; asc       Vf;;
 8: SQL NULL;

Record lock, heap no 104 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d6; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891c18; asc        ;;
 3: len 8; hex 80000000000003d6; asc         ;;
 4: len 8; hex 80000000000003d6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02603b; asc       `;;;
 7: len 8; hex 99bac3039a026465; asc       de;;
 8: SQL NULL;

Record lock, heap no 105 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d7; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891c9d; asc        ;;
 3: len 8; hex 80000000000003d7; asc         ;;
 4: len 8; hex 80000000000003d7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a026d28; asc       m(;;
 7: len 8; hex 99bac3039a02718e; asc       q ;;
 8: SQL NULL;

Record lock, heap no 106 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d8; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891d22; asc       ";;
 3: len 8; hex 80000000000003d8; asc         ;;
 4: len 8; hex 80000000000003d8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02778b; asc       w ;;
 7: len 8; hex 99bac3039a027b44; asc       {D;;
 8: SQL NULL;

Record lock, heap no 107 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003d9; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891da7; asc        ;;
 3: len 8; hex 80000000000003d9; asc         ;;
 4: len 8; hex 80000000000003d9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02838a; asc         ;;
 7: len 8; hex 99bac3039a0286cb; asc         ;;
 8: SQL NULL;

Record lock, heap no 108 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003da; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891e2c; asc       ,;;
 3: len 8; hex 80000000000003da; asc         ;;
 4: len 8; hex 80000000000003da; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a028d5f; asc        _;;
 7: len 8; hex 99bac3039a029191; asc         ;;
 8: SQL NULL;

Record lock, heap no 109 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003db; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891eb1; asc        ;;
 3: len 8; hex 80000000000003db; asc         ;;
 4: len 8; hex 80000000000003db; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a029c85; asc         ;;
 7: len 8; hex 99bac3039a02a0aa; asc         ;;
 8: SQL NULL;

Record lock, heap no 110 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dc; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891f36; asc       6;;
 3: len 8; hex 80000000000003dc; asc         ;;
 4: len 8; hex 80000000000003dc; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02a9bc; asc         ;;
 7: len 8; hex 99bac3039a02adfa; asc         ;;
 8: SQL NULL;

Record lock, heap no 111 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003dd; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001891fbb; asc        ;;
 3: len 8; hex 80000000000003dd; asc         ;;
 4: len 8; hex 80000000000003dd; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02b87e; asc        ~;;
 7: len 8; hex 99bac3039a02bd1a; asc         ;;
 8: SQL NULL;

Record lock, heap no 112 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003de; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001892040; asc       @;;
 3: len 8; hex 80000000000003de; asc         ;;
 4: len 8; hex 80000000000003de; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02c447; asc        G;;
 7: len 8; hex 99bac3039a02da41; asc        A;;
 8: SQL NULL;

Record lock, heap no 113 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003df; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018920c5; asc        ;;
 3: len 8; hex 80000000000003df; asc         ;;
 4: len 8; hex 80000000000003df; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02e30a; asc         ;;
 7: len 8; hex 99bac3039a02e702; asc         ;;
 8: SQL NULL;

Record lock, heap no 114 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e0; asc         ;;
 1: len 6; hex 00000000defc; asc       ;;
 2: len 7; hex 01000000ef040d; asc        ;;
 3: len 8; hex 80000000000003e0; asc         ;;
 4: len 8; hex 80000000000003e0; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02f21a; asc         ;;
 7: len 8; hex 99bac3039a02f60b; asc         ;;
 8: len 8; hex 99bac3039b0aaff7; asc         ;;

Record lock, heap no 115 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e1; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018921cf; asc      ! ;;
 3: len 8; hex 80000000000003e1; asc         ;;
 4: len 8; hex 80000000000003e1; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a02fecb; asc         ;;
 7: len 8; hex 99bac3039a03030c; asc         ;;
 8: SQL NULL;

Record lock, heap no 116 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e2; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001892254; asc      "T;;
 3: len 8; hex 80000000000003e2; asc         ;;
 4: len 8; hex 80000000000003e2; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a030bee; asc         ;;
 7: len 8; hex 99bac3039a030f69; asc        i;;
 8: SQL NULL;

Record lock, heap no 117 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e3; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018922d9; asc      " ;;
 3: len 8; hex 80000000000003e3; asc         ;;
 4: len 8; hex 80000000000003e3; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0317fe; asc         ;;
 7: len 8; hex 99bac3039a031ca7; asc         ;;
 8: SQL NULL;

Record lock, heap no 118 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e4; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 0200000189235e; asc      #^;;
 3: len 8; hex 80000000000003e4; asc         ;;
 4: len 8; hex 80000000000003e4; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a03270c; asc       ' ;;
 7: len 8; hex 99bac3039a032ae0; asc       * ;;
 8: SQL NULL;

Record lock, heap no 119 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e5; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018923e3; asc      # ;;
 3: len 8; hex 80000000000003e5; asc         ;;
 4: len 8; hex 80000000000003e5; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a032ff9; asc       / ;;
 7: len 8; hex 99bac3039a033309; asc       3 ;;
 8: SQL NULL;

Record lock, heap no 120 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e6; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001892468; asc      $h;;
 3: len 8; hex 80000000000003e6; asc         ;;
 4: len 8; hex 80000000000003e6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a033b91; asc       ; ;;
 7: len 8; hex 99bac3039a03409f; asc       @ ;;
 8: SQL NULL;

Record lock, heap no 121 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e7; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 020000018924ed; asc      $ ;;
 3: len 8; hex 80000000000003e7; asc         ;;
 4: len 8; hex 80000000000003e7; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a0345f6; asc       E ;;
 7: len 8; hex 99bac3039a034912; asc       I ;;
 8: SQL NULL;

Record lock, heap no 122 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e8; asc         ;;
 1: len 6; hex 00000000de9b; asc       ;;
 2: len 7; hex 02000001892572; asc      %r;;
 3: len 8; hex 80000000000003e8; asc         ;;
 4: len 8; hex 80000000000003e8; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a034de8; asc       M ;;
 7: len 8; hex 99bac3039a035228; asc       R(;;
 8: SQL NULL;

Record lock, heap no 124 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ea; asc         ;;
 1: len 6; hex 00000000deb7; asc       ;;
 2: len 7; hex 020000016302f9; asc     c  ;;
 3: len 8; hex 8000000000000002; asc         ;;
 4: len 8; hex 80000000000003ea; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a03e2fe; asc         ;;
 7: len 8; hex 99bac3039a03e679; asc        y;;
 8: SQL NULL;

Record lock, heap no 125 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003eb; asc         ;;
 1: len 6; hex 00000000deb7; asc       ;;
 2: len 7; hex 0200000163037c; asc     c |;;
 3: len 8; hex 8000000000000003; asc         ;;
 4: len 8; hex 80000000000003eb; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a03eb05; asc         ;;
 7: len 8; hex 99bac3039a03efa8; asc         ;;
 8: SQL NULL;

Record lock, heap no 127 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003ec; asc         ;;
 1: len 6; hex 00000000df2f; asc      /;;
 2: len 7; hex 02000001a03087; asc      0 ;;
 3: len 8; hex 8000000000000004; asc         ;;
 4: len 8; hex 80000000000003ec; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a03f699; asc         ;;
 7: len 8; hex 99bac3039a03f9e1; asc         ;;
 8: len 8; hex 99bac3039e0ad957; asc        W;;

Record lock, heap no 128 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003e9; asc         ;;
 1: len 6; hex 00000000df63; asc      c;;
 2: len 7; hex 0200000193305b; asc      0[;;
 3: len 8; hex 8000000000000001; asc         ;;
 4: len 8; hex 80000000000003e9; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac3039a03a074; asc        t;;
 7: len 8; hex 99bac3039a03a474; asc        t;;
 8: len 8; hex 99bac303a001a576; asc        v;;

Record lock, heap no 129 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 80000000000003a6; asc         ;;
 1: len 6; hex 00000000df8b; asc       ;;
 2: len 7; hex 02000001422f09; asc     B/ ;;
 3: len 8; hex 80000000000003a6; asc         ;;
 4: len 8; hex 80000000000003a6; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990f3238; asc       28;;
 7: len 8; hex 99bac303990f36b7; asc       6 ;;
 8: len 8; hex 99bac303a0079fed; asc         ;;

Record lock, heap no 130 PHYSICAL RECORD: n_fields 9; compact format; info bits 0
 0: len 8; hex 8000000000000381; asc         ;;
 1: len 6; hex 00000000df77; asc      w;;
 2: len 7; hex 01000000e10519; asc        ;;
 3: len 8; hex 8000000000000381; asc         ;;
 4: len 8; hex 8000000000000381; asc         ;;
 5: len 4; hex 53454e54; asc SENT;;
 6: len 8; hex 99bac303990d3b0d; asc       ; ;;
 7: len 8; hex 99bac303990d3e74; asc       >t;;
 8: len 8; hex 99bac303a0073e7c; asc       >|;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 44 page no 13 n bits 304 index PRIMARY of table `deadlock_lab`.`member_account` trx id 57245 lock mode S locks rec but not gap waiting
Record lock, heap no 233 PHYSICAL RECORD: n_fields 7; compact format; info bits 0
 0: len 8; hex 800000000000039f; asc         ;;
 1: len 6; hex 00000000dfb5; asc       ;;
 2: len 7; hex 01000001d02f76; asc      /v;;
 3: len 8; hex 8000000000000005; asc         ;;
 4: len 12; hex 746b2d343835383031303536; asc tk-485801056;;
 5: len 8; hex 99bac303a20614c7; asc         ;;
 6: len 8; hex 99bac303a20614c7; asc         ;;

*** WE ROLL BACK TRANSACTION (1)
------------

```
