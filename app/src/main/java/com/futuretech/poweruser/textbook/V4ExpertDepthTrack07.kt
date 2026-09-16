package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack07 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C07-S01",
            depthTitle("서버 입구를 “검증 함수”가 아니라 trust boundary와 부하 제어 계층으로 설계한다"),
            depthHeading("schema validation 전에 body size와 parser limit이 먼저다"),
            depthParagraph("""
JSON schema가 아무리 정확해도 2GB body를 다 메모리에 읽은 뒤 검증하면 이미 장애가 난다. reverse proxy와 application server에서 request body, header count/size, multipart part, parsing depth 같은 상한을 둔다. 입력 검증은 형식/업무 규칙뿐 아니라 “처리할 수 있는 크기인가”까지 포함한다.
"""),
            depthHeading("1. canonicalization을 검증보다 먼저 할지 신중히 정한다"),
            depthParagraph("""
전화번호 공백 제거, Unicode normalization, 대소문자 정리 같은 canonicalization은 같은 의미의 입력을 한 형태로 만들 수 있다. 하지만 path, URL, security identifier를 잘못 정규화하면 공격자가 서로 다른 해석을 이용할 수 있다. 어떤 변환을 허용하는지 명시하고 security-sensitive 값은 원본과 canonical form을 필요에 따라 함께 기록한다.
"""),
            depthHeading("2. idempotency key는 controller와 DB를 함께 설계한다"),
            depthParagraph("""
POST /payments 요청에 idempotency key가 와도 메모리 set에만 저장하면 server restart나 여러 instance에서 중복을 못 막는다. key, request fingerprint, final response/status를 durable store에 transaction과 함께 기록해야 한다. 같은 key로 다른 payload가 오면 conflict로 거부한다.
"""),
            depthHeading("3. validation error와 conflict를 구분한다"),
            depthParagraph("""
email 형식이 틀린 것은 입력 validation, 이미 사용 중인 email은 현재 server state와의 conflict다. 둘을 같은 400 한 줄로만 보내면 client가 수정 가능한 필드 오류와 retry/refresh가 필요한 상태 충돌을 구분하기 어렵다. status code는 절대 법칙이 아니지만 API 전체에서 일관된 error taxonomy를 만든다.
"""),
            depthHeading("4. rate limit은 비싼 작업 전에 적용한다"),
            depthParagraph("""
인증 credential 검증 자체가 저렴한지, tenant별 quota가 필요한지에 따라 위치가 달라진다. 보통 expensive DB query, AI call, password hashing을 실행하기 전에 가능한 한 빨리 제한해 자원을 보호한다. 그러나 anonymous IP limit만 두면 NAT 뒤 여러 정상 사용자가 함께 막힐 수 있어 user/API key/tenant 단위를 조합한다.
"""),
            depthHeading("5. request context를 경계에서 만든다"),
            depthParagraph("""
request_id, authenticated principal, deadline, locale 같은 context를 입구에서 만들고 하위 계층에 전달하면 logging, authorization, timeout이 일관된다. global thread-local에 무조건 의존하면 async execution에서 context가 섞일 수 있으므로 사용하는 framework의 context propagation 모델을 이해한다.
"""),
            depthCode("""
POST /payments
Headers: Idempotency-Key: K123
Body: amount=10000, orderId=O9

transaction 안에서
1) K123 조회
2) 없으면 request fingerprint 저장
3) payment 생성
4) 결과 + key 기록
5) commit

같은 K123 + 같은 body -> 저장된 결과 재사용
같은 K123 + 다른 body -> conflict
"""),
            depthBullets(
                "payload validation보다 앞에 parser/resource limit이 존재해야 한다.",
                "idempotency는 단일 process memory가 아니라 배포 구조 전체에서 보장하는 범위를 명시한다.",
                "로그 context와 auth context가 async 경계에서 유실되지 않는지 테스트한다."
            )
        ),
        expertPack(
            sectionId = "V1-C07-S02",
            depthTitle("service/repository 복습을 끝내고 transaction boundary·hexagonal port·test double로 간다"),
            depthHeading("transaction boundary는 “DB 함수 하나”가 아니라 업무 단위와 맞춘다"),
            depthParagraph("""
주문 생성에서 order row 저장, 재고 예약, outbox event 기록이 함께 성공해야 한다면 repository 메서드마다 따로 commit하면 중간 상태가 남을 수 있다. application service/use case가 하나의 업무 transaction 범위를 조정하고 repository는 같은 transaction context를 공유하는 설계가 필요할 수 있다. 반대로 외부 HTTP 호출을 DB transaction 안에서 오래 기다리면 lock을 오래 잡을 수 있어 경계를 분리해야 한다.
"""),
            depthHeading("1. port와 adapter로 “내부가 외부 기술을 모르게” 한다"),
            depthParagraph("""
핵심 use case가 Stripe SDK, PostgreSQL ORM, S3 client의 구체 타입에 직접 의존하면 provider 교체와 test가 어려워진다. 내부에서 PaymentPort, OrderRepository 같은 필요한 기능 contract를 정의하고 외부 adapter가 이를 구현하게 하면 dependency 방향이 안쪽을 향한다. 모든 클래스에 interface를 만드는 것이 아니라 실제 외부 경계와 변경 가능성이 큰 곳을 선택한다.
"""),
            depthHeading("2. fake·stub·mock을 목적에 맞게 구분한다"),
            depthParagraph("""
fake repository는 메모리 map처럼 실제 동작의 간단한 구현을 제공할 수 있고, stub은 정해진 값을 반환하며, mock은 호출 여부/순서를 검증하는 데 쓸 수 있다. mock을 너무 많이 쓰면 내부 구현 순서가 바뀔 때 테스트가 깨지고 실제 SQL/serialization 버그를 못 잡는다. business rule unit test에는 fake가 읽기 쉬운 경우가 많다.
"""),
            depthHeading("3. Unit of Work는 여러 repository 변경을 묶는 방식이다"),
            depthParagraph("""
ORM이나 repository가 여러 entity 변경을 추적하다가 한 commit에서 flush하도록 unit-of-work pattern을 사용할 수 있다. 중요한 점은 “save()를 호출했으니 DB에 즉시 확정됐다”는 가정을 하지 않고, transaction commit 시점을 명시하는 것이다. domain event도 commit 전후 어느 시점에 발행되는지 규칙이 필요하다.
"""),
            depthHeading("4. domain model과 persistence model은 같을 수도 다를 수도 있다"),
            depthParagraph("""
작은 CRUD 서비스는 ORM entity를 그대로 써도 충분할 수 있다. 복잡한 규칙이 많아지면 DB column 구조와 domain object의 invariant가 달라 mapping 계층을 둘 수 있다. 무조건 분리하거나 무조건 통합하는 것이 아니라 변경 이유와 복잡도를 보고 선택한다.
"""),
            depthHeading("5. dependency cycle은 architecture test로 막을 수 있다"),
            depthParagraph("""
폴더 규칙을 문서로만 두면 시간이 지나며 domain이 web/ORM 모듈을 import하기 시작할 수 있다. module dependency를 정적 분석해 “domain은 infrastructure를 의존하지 않는다” 같은 규칙을 CI test로 만들면 architecture erosion을 조기에 발견할 수 있다. 설계 원칙을 자동 검사 가능한 계약으로 바꾸는 예다.
"""),
            depthCode("""
PlaceOrderUseCase
  begin transaction
  orderRepo.reserveInventory(...)
  orderRepo.save(order)
  outboxRepo.append(OrderPlaced(...))
  commit

외부 email API는 transaction 안에서 호출하지 않는다.
commit된 outbox를 별도 publisher가 처리한다.
"""),
            depthBullets(
                "transaction 안에 느린 외부 network call을 넣기 전에 lock duration을 계산한다.",
                "unit test의 mock 호출 횟수보다 domain outcome/invariant를 우선 검증한다.",
                "architecture boundary가 중요하면 dependency rule을 자동 테스트로 만든다."
            )
        ),
        expertPack(
            sectionId = "V1-C07-S03",
            depthTitle("비동기 운영의 핵심: at-least-once, idempotent consumer, outbox, DLQ, saga, cache stampede"),
            depthParagraph("""
이 LESSON은 기존 V4에서 방향이 좋았던 부분을 기준점으로 삼아 더 깊게 확장한다. queue는 request latency를 줄이는 도구이면서 “나중에, 중복될 수 있고, 순서가 바뀔 수 있는 실행”을 받아들이는 모델이다. producer가 message를 한 번 보냈다는 사실과 consumer side effect가 정확히 한 번 발생했다는 사실은 다르다.
"""),
            depthHeading("1. at-least-once delivery에서 idempotent consumer를 만든다"),
            depthParagraph("""
consumer가 DB update까지 성공하고 acknowledgement 전에 죽으면 broker는 같은 message를 다시 줄 수 있다. event_id를 processed_events table에 기록하고 business change와 같은 transaction으로 commit하거나, side effect 자체에 unique key를 두어 중복을 막는다. “이미 처리했는지 조회→처리”를 별도 transaction으로 하면 race가 생길 수 있어 원자성을 함께 설계한다.
"""),
            depthHeading("2. outbox는 dual-write gap을 닫는다"),
            depthParagraph("""
DB commit과 broker publish를 따로 하면 둘 중 하나만 성공하는 창이 생긴다. outbox row를 business data와 같은 DB transaction에 기록한 뒤 publisher가 미전송 row를 읽어 broker로 보내면 “DB에는 있는데 event가 없음”을 줄인다. publisher도 중복 publish할 수 있으므로 consumer idempotency는 여전히 필요하다. outbox가 exactly-once 마법은 아니다.
"""),
            depthHeading("3. DLQ는 쓰레기통이 아니라 조사 queue다"),
            depthParagraph("""
일시 timeout은 backoff 후 retry해도 되지만 schema mismatch, 삭제된 account, 반복 validation failure는 무한 retry해도 낫지 않는다. 최대 시도 후 DLQ로 보내고 original event, error class, attempts, first/last failure time을 남긴다. 운영자가 수정 후 replay할 때 side effect 중복 위험과 schema version을 검토한다.
"""),
            depthHeading("4. saga와 compensation"),
            depthParagraph("""
주문·결제·배송이 서로 다른 service DB에 있다면 하나의 ACID transaction으로 묶기 어렵다. saga는 단계별 local transaction과 event/command로 흐름을 이어 가고, 중간 실패 시 이미 완료한 작업을 보상(compensate)한다. 결제 취소는 “시간을 되돌리는 rollback”이 아니라 별도의 refund business action이므로 실패할 수도 있고 감사 기록도 남는다.
"""),
            depthHeading("5. cache stampede와 single-flight"),
            depthParagraph("""
queue가 비동기 쓰기 문제를 만든다면 cache는 동시 read miss 문제를 만든다. hot key가 만료될 때 한 process/cluster에서 원본 refresh를 하나만 진행하고 나머지는 기다리게 하거나 stale 값을 제공한다. distributed single-flight를 구현할 때 lock lease가 만료되어 refresh가 겹칠 가능성도 고려한다.
"""),
            depthHeading("6. observability: backlog age가 queue length보다 중요할 수 있다"),
            depthParagraph("""
queue에 100만 건이 있어도 초당 50만 건 처리하면 2초 안에 비워질 수 있고, 1000건뿐이어도 worker가 멈춰 가장 오래된 message가 2시간 지연될 수 있다. depth, enqueue rate, process rate, oldest-message age, retry/DLQ rate를 함께 본다. trace id/event id로 producer→consumer 경로를 연결한다.
"""),
            depthCode("""
OrderPlaced E42
DB transaction: order 저장 + outbox(E42) 저장
commit
↓
publisher가 E42 publish
↓
email consumer: processed_events에 E42 unique insert + email job 기록
commit
↓
ack 전에 crash
↓
E42 재전달
unique constraint가 중복 side effect를 막음
"""),
            depthBullets(
                "queue delivery 보장과 business side effect 보장을 구분한다.",
                "DLQ에는 replay 판단에 필요한 원본·schema version·오류 정보를 남긴다.",
                "saga compensation도 실패할 수 있으므로 별도 retry/수동 개입 상태를 설계한다."
            )
        ),
        expertPack(
            sectionId = "V1-C07-S04",
            depthTitle("동시성·관측성·회복탄력성을 한 장애 시나리오에서 연결한다"),
            depthHeading("lost update는 transaction이 있어도 isolation에 따라 생길 수 있다"),
            depthParagraph("""
두 request가 같은 row version을 읽고 각각 계산한 값을 update하면 나중 write가 앞 write를 덮을 수 있다. optimistic concurrency는 version column을 WHERE id=? AND version=? 조건에 포함해 한 요청만 성공하게 하고 실패한 요청은 최신 상태를 다시 읽는다. pessimistic lock은 먼저 row lock을 잡아 다른 transaction을 기다리게 한다. 충돌 빈도와 lock 대기 비용을 보고 선택한다.
"""),
            depthHeading("1. isolation anomaly를 이름으로 구분한다"),
            depthParagraph("""
dirty read, non-repeatable read, phantom, write skew 같은 현상은 DB isolation level과 구현에 따라 허용 여부가 다르다. “transaction을 썼으니 동시성 문제 없음”이 아니다. 재고 1개 차감, 예약 중복 방지처럼 중요한 invariant가 어떤 anomaly에서 깨질 수 있는지 실제 concurrent test로 확인한다.
"""),
            depthHeading("2. deadlock은 lock 순서를 통일해 줄일 수 있다"),
            depthParagraph("""
T1이 A lock 후 B를 기다리고 T2가 B lock 후 A를 기다리면 cycle이 생긴다. DB는 deadlock을 탐지해 한 transaction을 abort할 수 있으므로 application은 해당 오류를 제한적으로 retry할 준비가 필요하다. 여러 row를 잠글 때 항상 id 오름차순처럼 lock acquisition order를 통일하면 deadlock 가능성을 줄일 수 있다.
"""),
            depthHeading("3. rate limit algorithm의 차이"),
            depthParagraph("""
fixed window는 단순하지만 경계 직전/직후 burst가 두 배로 들어올 수 있다. sliding window는 더 정확하지만 비용이 크고, token bucket은 일정 속도로 token을 채워 짧은 burst를 허용하면서 장기 rate를 제한한다. login brute force와 public API throughput 제한은 원하는 burst 정책이 다를 수 있다.
"""),
            depthHeading("4. logs·metrics·traces를 사건 하나로 연결한다"),
            depthParagraph("""
metric에서 p99 latency 상승과 DB connection pool saturation을 발견하고, 느린 trace에서 특정 query span 3초를 찾고, 그 span의 query_id/request_id로 log를 열어 lock timeout을 확인한다. 세 도구는 중복이 아니라 “발견→위치→세부 원인”의 서로 다른 질문에 답한다.
"""),
            depthHeading("5. bulkhead는 자원 pool을 분리한다"),
            depthParagraph("""
느린 report API가 worker thread와 DB connection을 모두 소진하면 login까지 죽을 수 있다. report용 concurrency pool/connection quota를 별도로 두면 한 기능 장애가 다른 기능 자원까지 잠식하는 것을 막는다. circuit breaker가 외부 dependency 호출을 차단하는 것과 달리 bulkhead는 내부 자원 격리 경계를 만든다.
"""),
            depthHeading("6. deadline budget을 하위 호출에 나눠 준다"),
            depthParagraph("""
client 전체 deadline이 2초인데 service A가 DB timeout 3초, 그 뒤 external API timeout 5초라면 이미 사용자 deadline을 지킬 수 없다. 남은 시간 budget을 context로 전달하고 하위 호출 timeout을 더 짧게 설정한다. retry도 남은 budget 안에서만 수행해 실패한 요청이 오래 자원을 붙잡지 않게 한다.
"""),
            depthCode("""
장애 흐름
metric: DB pool 100%, p99=6s
trace: report query가 connection 5s 점유
log: lock wait timeout 증가

개선 후보
- report pool/concurrency bulkhead 분리
- query/index 개선
- transaction lock 범위 축소
- deadline 2s 전파

단순 server instance 증설은 DB 병목을 더 악화시킬 수 있다.
"""),
            depthBullets(
                "concurrency bug는 단일 요청 테스트가 아니라 동시에 실행하는 test로 검증한다.",
                "deadlock retry는 같은 transaction 전체를 재실행할 수 있는 idempotent 경계를 고려한다.",
                "평균 latency가 아니라 p95/p99와 saturation을 함께 본다."
            )
        )
    )
}
