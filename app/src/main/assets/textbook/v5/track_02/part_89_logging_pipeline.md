# PART 89 · Logging pipeline — LogRecord·hierarchy·handler·structured field를 운영 계약으로 설계하기

Logging은 `print`의 고급 버전이 아니다. Logger가 event를 만들고, hierarchy와 propagation이 route를 결정하고, handler가 destination을 선택하며, formatter가 표현을 만든다. 여기에 request ID 같은 context와 redaction 정책이 결합된다. 구조를 모르고 logger/handler를 여기저기 추가하면 같은 오류가 두세 번 찍히거나 production에서 secret이 평문으로 남을 수 있다.

---

## CHAPTER 01 · LogRecord는 한 번의 로그 event와 실행 context를 담는다

Logger method를 호출하면 level, message template, argument, logger name, source 위치, exception 정보 같은 field가 LogRecord 형태로 전달될 수 있다.

```python
logger.info("order processed", extra={"order_id": order_id})
```

Message string 하나에 모든 정보를 합치기보다 search/filter가 필요한 값을 별도 field로 유지하면 structured logging으로 발전시키기 쉽다.

하지만 `extra` key 이름이 logging system의 기존 field와 충돌하지 않는지 확인해야 한다. Application-wide field schema를 정하면 서비스 간 query가 일관된다.

---

## CHAPTER 02 · logger hierarchy는 점으로 구분된 이름을 tree처럼 연결한다

보통 module name을 logger name으로 사용하면 `app.db`, `app.http`처럼 hierarchy를 만들 수 있다. Child logger의 record가 parent로 propagate될 수 있어 중앙 handler 설정이 가능하다.

Library code가 root logger에 직접 handler를 붙이면 application의 logging policy를 침범할 수 있다. Library는 event를 발생시키고 최종 destination/format은 application이 결정하게 두는 편이 좋다.

Logger name은 단순 label이 아니라 routing namespace다. Rename이 dashboard query와 alert rule에 영향을 줄 수 있으므로 public observability field처럼 관리한다.

---

## CHAPTER 03 · handler와 formatter는 destination과 표현 책임을 분리한다

Console, rotating file, remote queue 등 destination별로 handler를 둘 수 있고 같은 LogRecord를 서로 다른 format으로 표현할 수 있다.

Development에서는 사람이 읽는 text, production에서는 JSON-like structured output을 원할 수 있다. Business code가 직접 JSON string을 만들기보다 logging pipeline에서 format을 선택하면 transport를 교체하기 쉽다.

Slow remote handler가 request thread를 block하지 않도록 queue handler나 별도 exporter 구조를 사용할 수 있다. Logging도 I/O이므로 latency budget에서 제외되지 않는다.

---

## CHAPTER 04 · filter와 context injection은 request-scoped metadata를 record에 추가할 수 있다

Request ID, tenant ID, trace ID를 매 log call 인수에 반복해서 넘기지 않고 context variable이나 filter를 통해 주입할 수 있다.

하지만 context propagation이 async task, executor thread, background worker에서 어떻게 이어지는지 검증해야 한다. ID가 사라지거나 이전 request 값이 재사용되면 로그가 오히려 잘못된 상관관계를 만든다.

Context field는 request 종료 시 명확히 해제한다. Global mutable variable 하나로 current request를 저장하지 않는다.

---

## CHAPTER 05 · propagation과 중복 handler는 같은 LogRecord를 여러 번 출력하게 만들 수 있다

Child logger에 handler를 붙이고 propagation도 켠 채 parent handler가 있으면 한 event가 두 destination에서 중복 처리될 수 있다. 개발자는 logging call이 한 번이므로 application bug로 오해하기 쉽다.

문제 해결 시 “logger가 몇 번 호출됐나”뿐 아니라 logger→handler route를 그린다. Root와 package logger의 handler 목록, propagate flag, level을 함께 확인한다.

중복 제거를 위해 무조건 `propagate=False`를 곳곳에 넣기보다 중앙 logging architecture를 먼저 정한다.

---

## CHAPTER 06 · structured field는 message parsing 대신 query 가능한 event schema를 만든다

다음 두 로그를 비교한다.

```text
"user 42 payment failed retry=3"
```

```text
message="payment failed", user_id=42, retry=3, error_code="TIMEOUT"
```

후자는 message 문구가 바뀌어도 field query를 유지할 수 있다. Metrics로 전환하거나 trace와 join하기도 쉽다.

Field type을 안정적으로 유지한다. 어떤 서비스는 `user_id`를 int, 다른 서비스는 dict로 보내면 중앙 pipeline이 복잡해진다. Event name과 core field schema를 versioned contract처럼 관리한다.

---

## CHAPTER 07 · sensitive data redaction은 formatter 마지막 단계만 믿지 않는다

Password, token, authorization header, personal data가 LogRecord에 들어간 뒤 여러 handler로 복제되면 한 formatter의 masking만으로 충분하지 않을 수 있다.

가장 좋은 정책은 애초에 secret을 log argument로 전달하지 않는 것이다. 필요한 경우 credential 객체의 `repr`도 redacted하게 만들고, logging filter/exporter에서 두 번째 방어를 둔다.

Error logging에서 request body 전체를 남기는 관행은 특히 위험하다. Allowlist field만 기록하고 payload는 length/hash/reference ID로 대체할 수 있다.

---

## CHAPTER 08 · logging contract는 event 의미·route·비용·privacy를 함께 정의한다

운영 가능한 logging은 어떤 event를 어떤 level로 기록하는지, 어떤 structured field가 필요한지, 어디로 route되는지, duplicate와 secret을 어떻게 막는지 정한다.

테스트에서는 hierarchy propagation, handler duplication, exception logging, context ID, redaction을 확인한다. Load test에서는 logging I/O가 tail latency를 얼마나 늘리는지도 본다.

이 PART의 핵심은 **logging을 문자열 출력이 아니라 event 생성→routing→format→export로 이어지는 관찰 가능성 pipeline으로 보고, field schema와 privacy를 application contract로 관리하는 것**이다.
