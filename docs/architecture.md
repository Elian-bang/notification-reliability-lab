# 아키텍처와 시퀀스

A-M8 Deadlock Matrix 의 구조 문서. 기획 배경은 [`adr/0001-topology-and-variables.md`](adr/0001-topology-and-variables.md).

---

## 1. 정상 흐름

```mermaid
sequenceDiagram
    autonumber
    participant RQ as 요청 서버<br/>(Next.js)
    participant BS as Batch-Server<br/>(Spring Batch)
    participant EX as 외부 채널사 API
    participant RC as 수신자 서버<br/>(Next.js)
    participant DB as MySQL

    RQ->>DB: INSERT notification_request<br/>(requested_at, status=PENDING)<br/>FK로 계정 행 S락

    Note over BS: Job 실행 — chunk = 트랜잭션
    BS->>DB: reader: PENDING 요청 조회
    BS->>EX: processor: 발송 호출
    EX-->>BS: 응답
    BS->>DB: writer: INSERT notification (created_at)<br/>FK로 계정 행 S락
    BS->>DB: writer: UPDATE notification SET sent_at
    BS->>DB: writer: UPDATE notification_request SET finished_at, status=DONE

    Note over RC: 사용자 접속
    RC->>DB: 1) 토큰 갱신 — UPDATE member_account (X락)
    RC->>DB: 2) 알림 확인 — UPDATE notification SET read_at (X락)
```

**배치는 `member_account` 와 `tenant` 을 갱신하지 않는다.**
수신자만 계정 행을 X락으로 잡는다. 배치·요청 서버는 **FK 를 통해 S락**만 건다.

- 배치: 알림(A) → FK로 계정(B) — 사실상 **A → B**
- 수신자: 계정(B) → 알림(A) — **B → A**

어느 쪽도 잘못 짜지 않았다. 발송은 알림을 만드는 게 먼저고,
수신은 **로그인해서 토큰을 갱신하는 게 먼저다.**
**토큰 갱신을 락으로 생각하는 사람은 없다.**

---

## 2. 데드락 — FK 가 유일한 연결 고리

**배치는 `member_account` 를 갱신하는 코드가 한 줄도 없다.**
그런데 `notification` INSERT 시 FK 검사가 부모 행에 S락을 건다.

```mermaid
sequenceDiagram
    autonumber
    participant BS as Batch-Server
    participant A as notification<br/>(자원 A)
    participant B as member_account<br/>(자원 B)
    participant RC as 수신자 서버

    BS->>A: UPDATE notification(42) SET sent_at — X락 획득
    RC->>B: UPDATE member_account(7) — X락 획득 (토큰 갱신)

    Note over BS: 외부 채널사 API 대기<br/>알림 락을 쥔 채 머문다 (조건 ③)

    BS->>A: INSERT notification(account_id=7)
    Note over BS,B: FK 검사 → 부모 행 7에 S락 필요
    BS->>B: S락 요청
    Note over B: 수신자가 X락 보유 → 대기

    RC->>A: UPDATE notification(42) SET read_at
    Note over A: 배치가 X락 보유 → 대기

    Note over A,B: 사이클 → InnoDB 1213
    B--xRC: victim — undo 적은 쪽<br/>수신자 트랜잭션 롤백
```

**배치는 계정을 갱신하지 않는데 계정 락 때문에 죽는다.**
코드를 아무리 읽어도 `member_account` 를 잠그는 문장이 없다. FK 가 대신 잠근다.

> *"배치에는 그 테이블을 건드리는 코드가 없었습니다. FK가 잠그고 있었습니다."*

**FK 가 없으면 배치는 계정을 아예 안 잠근다 → 사이클이 불가능하다.**
즉 **FK 는 이 데드락의 필요조건**이다. V8 이 그것을 검증한다.

---

## 3. 요청 서버의 역할 — 사이클은 아니지만 경합을 키운다

`notification_request.account_id` 에도 FK 가 있으므로 요청 INSERT 도 계정 행에 S락을 건다.

```mermaid
sequenceDiagram
    participant RQ as 요청 서버
    participant B as member_account
    participant RC as 수신자 서버

    RC->>B: UPDATE member_account(7) — X락 (토큰 갱신)
    RQ->>B: INSERT notification_request(account_id=7)<br/>→ FK S락 요청
    Note over B: X락과 충돌 → 대기
    Note over RQ,RC: 요청 서버는 단문 트랜잭션이라<br/>사이클은 만들지 않는다
```

S락끼리는 호환되지만 **수신자의 X락과는 충돌**한다.
요청 부하가 올라가면 **락 타임아웃(1205)** 이 늘어나고,
배치·수신자의 대기가 길어져 조건 ③이 간접적으로 악화된다.

---

## 4. 조치가 어느 조건을 없애나

```mermaid
flowchart TD
    C1["① 같은 (알림, 계정) 쌍을 건드림"]
    C2["② 자원 접근 순서가 반대"]
    C3["③ 배치가 락을 오래 쥠"]
    C4["④ FK로 인한 부모 행 S락"]
    DL(["데드락 발생"])

    C1 --> DL
    C2 --> DL
    C3 --> DL
    C4 --> DL

    M1["조회를 TX 밖으로 (V1)"] -.완화.-> C3
    M2["chunk 500→200 (V2)<br/><i>탐색 과정</i>"] -.효과 미확인.-> C3
    M3["BATCH UPDATE (V3)"] -.완화.-> C3
    M4["자원 순서 통일 (V4)"] ==제거==> C2
    M5["FK 제거 (V8)"] ==제거==> C4
    M6["READ COMMITTED (V5)"] -.완화.-> C1

    style M4 fill:#22543d,color:#fff
    style M5 fill:#22543d,color:#fff
    style DL fill:#742a2a,color:#fff
```

**가설: 운영에서 넣은 두 조치(V1·V3)는 둘 다 ③을 줄이는 같은 손잡이다.**
확률을 낮출 뿐 사이클 조건 자체는 남는다. 굵은 화살표(V4·V8)만 조건을 제거한다. 청크 축소(V2)는 조치가 아니라 탐색 과정이었다 — 효과 유무를 대조군으로 확인한다.

**핵심 비교는 V6(운영 조합) vs V4(순서 통일) vs V8(FK 제거)** 이다.

---

## 5. Spring Batch 구조와 V1 의 관계

```mermaid
flowchart LR
    subgraph tx["chunk = 1 트랜잭션 (기본 동작)"]
        R["reader<br/>PENDING 조회"] --> P["processor<br/>외부 API"] --> W["writer<br/>A → B 갱신"]
    end
    style tx fill:#2c5282,color:#fff
```

**조회가 트랜잭션 안에 있는 것이 Spring Batch 의 표준 동작이다.**
운영에서 겪은 원본 조건이 기본 설정 그대로 재현된다.

V1(조회를 트랜잭션 밖으로)은 이 구조를 벗어나야 만들 수 있다 —
선행 Step 에서 대상 id 를 확보하고, chunk 트랜잭션에서는 쓰기만 하게 바꾼다.

```mermaid
flowchart LR
    S1["Step 1<br/>대상 id 확보<br/>(TX 밖)"] --> S2
    subgraph S2["Step 2 — chunk = TX"]
        W2["writer only<br/>A → B 갱신"]
    end
    style S2 fill:#2c5282,color:#fff
```

---

## 6. JPA 주의 — 코드 순서 ≠ 락 획득 순서

`save()` 호출 순서와 무관하게, 실제 UPDATE 는 **flush 시점에 Hibernate 액션 큐 순서**로 나간다.
Hibernate 는 엔티티 타입별로 묶어 정렬한다.

```mermaid
flowchart TD
    code["코드: A 저장 → B 저장"] --> ctx["영속성 컨텍스트<br/>(아직 SQL 없음)"]
    ctx --> flush{"flush 시점"}
    flush --> queue["Hibernate 액션 큐<br/>타입별 정렬"]
    queue --> sql["실제 SQL: B → A 일 수 있다"]
    style sql fill:#742a2a,color:#fff
```

자원 순서가 이 실험의 핵심 변수이므로 **반드시 통제한다.**

- 각 갱신 후 **명시적 `em.flush()`** — 코드 순서를 실행 순서로 강제
- 또는 **JPQL `@Modifying` bulk update** — 즉시 실행, 영속성 컨텍스트를 거치지 않음

> 통제 대상인 동시에 **건질 발견**이다.
> *"JPA 를 쓰면 락 획득 순서가 코드에 드러나지 않는다"* — 실패 리포트에 남긴다.
