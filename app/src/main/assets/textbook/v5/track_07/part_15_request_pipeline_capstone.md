# PART 15 · BLOCK 01 · LESSON 15 · 요청 한 건을 입구부터 종료까지 직접 구현하는 첫 종합 실습

앞의 PART들은 server process, request/response, route, 입력 채널, parsing, middleware, validation, normalization, error contract, response mapping, async flow, request context, config, shutdown을 각각 분리해서 다뤘다. 이제 이 조각을 하나의 작은 API에 연결한다. 목표는 많은 framework 기능을 사용하는 것이 아니라 **요청이 어느 단계에서 어떤 상태로 변하고 실패했을 때 어디서 멈추는지 직접 추적할 수 있는 구조**를 만드는 것이다. 저장소는 memory fake를 사용해 database 자체의 복잡도는 뒤 BLOCK로 미룬다.

---

## CHAPTER 01 · 기능 요구를 route보다 먼저 input·output·invariant로 적는다

만들 기능은 메모 생성과 조회다.

```text
POST /notes
GET  /notes/:id
```

생성 input은 title 1~100자, content 0~5000자다. server는 내부 id와 createdAt을 만든다. response는 id, title, content, createdAt만 공개하고 internal ownerKey나 debug field는 숨긴다. 동일 process 안에서 memory repository를 사용하므로 재시작 시 데이터가 사라진다는 limitation을 명시한다.

요구를 code 전에 표로 작성한다. 각 field의 source, raw type, validation, normalization, response representation을 적는다. title은 trim 후 비어 있지 않아야 하고 content는 원문 line break를 보존한다고 정한다. 이 표가 implementation과 test의 기준이 된다.

---

## CHAPTER 02 · startup은 typed config가 성공한 뒤에만 port를 연다

PORT와 HOST를 raw environment에서 읽어 검증하고, invalid하면 listen 전에 process를 종료한다. `AppConfig`를 생성한 뒤 server bootstrap에 전달한다. runtime code에서 `process.env`를 반복해서 읽지 않는다.

startup log에는 application version, host, port를 남기되 secret이 없도록 한다. 이 작은 API에는 외부 secret이 없지만 이후 DB credential을 추가할 때 같은 contract를 유지한다. readiness는 route/middleware setup과 repository 생성이 끝난 뒤 true가 된다.

테스트에서는 PORT=`abc`, 70000, missing/default 세 case를 실행해 startup result를 확인한다. invalid config에서 listening socket이 생기지 않는지 검증한다.

---

## CHAPTER 03 · request pipeline의 순서를 그림으로 고정한다

```text
request 도착
→ request ID/context
→ coarse body-size guard
→ JSON parser
→ route match
→ route schema validation
→ normalization
→ handler
→ application function
→ memory repository
→ response mapper
→ serializer
→ finish telemetry
```

error가 발생하면 중앙 error translator가 known/unknown failure를 response contract로 바꾼다. 모든 request가 동일 단계를 지나는 것은 아니다. GET에는 JSON body parser가 필요 없을 수 있고 POST에만 body schema가 적용된다. global과 route-specific middleware를 구분한다.

코드를 작성한 뒤 실제 log에 stage name을 남겨 정상 POST가 예상 순서로 실행되는지 한 번 확인하고 production에서는 verbose stage log를 제거한다.

---

## CHAPTER 04 · POST body는 parser와 schema를 통과해야 application에 도달한다

`Content-Type: application/json`만 허용하고 test limit을 8KB로 둔다. malformed JSON은 parser error, valid JSON이지만 title number인 경우 schema error, title이 101자인 경우 length error다. 세 failure는 service를 호출하지 않는다.

unknown field는 strict reject로 정한다. `{title,content,isAdmin:true}`처럼 허용하지 않은 field가 오면 400 contract error다. 이 결정은 client typo와 mass-assignment를 빨리 발견하게 한다.

spy로 `createNote` 호출 횟수를 확인해 invalid request에서 0임을 검증한다. status만 보는 것보다 boundary가 실제로 side effect를 차단했는지 확인한다.

---

## CHAPTER 05 · normalization은 title과 content에 서로 다른 정책을 적용한다

title은 앞뒤 공백을 제거하고 그 결과가 empty인지 검사한다. content는 사용자가 작성한 leading whitespace가 의미 있을 수 있으므로 전체 trim하지 않는다. line ending을 통일할지 제품 요구를 정하고 무심코 변환하지 않는다.

normalization function은 pure하게 만들고 raw input→normalized command test를 둔다. 같은 mapper가 password나 code field에 재사용되지 않도록 field-specific function 이름을 사용한다.

`"   hello   "` title은 `"hello"`가 되고 content `"  code\n"`는 원문 정책에 따라 보존되는지 fixture로 확인한다.

---

## CHAPTER 06 · handler는 HTTP object와 application function 사이의 adapter로 제한한다

handler는 validated input을 `createNote.execute(ctx,input)`에 전달하고 결과를 response mapper로 보낸다. repository 배열 조작과 ID 생성 규칙을 handler에 직접 넣지 않는다. 이렇게 하면 HTTP 없이 application function을 test할 수 있다.

GET handler는 path ID를 parse하고 application query를 호출한다. invalid ID는 repository 전에 거부한다. missing ID는 NotFound application result로 번역한다.

handler line count가 짧다는 것보다 HTTP concern과 application concern이 섞이지 않는지를 code review한다.

---

## CHAPTER 07 · memory repository도 port contract를 갖게 해 다음 저장소로 교체할 수 있게 한다

```ts
interface NoteRepository {
  save(note: Note): Promise<void>;
  findById(id: NoteId): Promise<Note | null>;
}
```

in-memory Map implementation을 사용하지만 service는 Map API를 모른다. 나중에 DB repository로 바꿀 때 use case signature를 유지할 수 있다. memory repository는 process restart durability가 없으므로 production persistence로 오해하지 않는다.

fake가 real DB의 unique/concurrency behavior를 완전히 재현하지 못한다는 한계도 기록한다. 지금은 request pipeline 실습이 목적이다.

동시에 두 create를 호출해 ID generator가 collision 없이 작동하는지 test하고, global counter를 사용할 경우 multi-instance 한계를 설명한다.

---

## CHAPTER 08 · response mapper는 내부 field 추가가 public JSON을 바꾸지 않게 한다

Note domain object에 `internalOwnerKey`, `normalizedTitle`, `debugRevision`을 넣어도 API response에는 지정된 네 field만 나간다. createdAt은 UTC ISO string으로 바꾼다. response schema를 OpenAPI 또는 runtime validator로 검사한다.

목록 기능을 아직 만들지 않으므로 pagination을 억지로 추가하지 않는다. feature 요구가 생길 때 contract를 확장한다. `나중에 필요할 것 같음` 때문에 내부 field를 미리 노출하지 않는다.

snapshot test에서 forbidden field 목록도 assertion한다.

---

## CHAPTER 09 · error translator는 syntax·validation·not-found·unexpected를 구분한다

malformed JSON은 `INVALID_JSON`, schema failure는 `INVALID_INPUT`, missing note는 `NOTE_NOT_FOUND`, unexpected Error는 `INTERNAL_ERROR`로 외부 code를 정한다. response envelope에는 requestId를 포함하되 stack은 포함하지 않는다.

server log는 unexpected error stack과 cause를 requestId로 기록한다. validation error는 낮은 severity metric으로 분리한다. client가 error message 한국어를 parsing하지 않고 code/status로 행동하도록 한다.

각 error fixture가 documented schema와 맞는지 integration test한다.

---

## CHAPTER 10 · async delay와 rejection을 넣어 handler error propagation을 실제 확인한다

memory repository `save`에 optional delay/reject mode를 추가해 async behavior를 재현한다. 정상 delay 동안 다른 GET `/health`가 응답하는지 확인하고, reject가 중앙 error handler로 들어오는지 test한다. await를 의도적으로 빼면 response가 repository completion 전에 나가는 bug를 관찰할 수 있다.

fire-and-forget save는 note가 실제 저장되기 전에 201을 반환할 수 있어 contract 위반이다. create success의 기준이 durable save completion이라면 await해야 한다.

이 실험으로 async syntax와 business completion semantics를 연결한다.

---

## CHAPTER 11 · request context는 concurrent log와 deadline을 분리한다

각 request에 random requestId와 2초 deadline을 넣고 AsyncLocalStorage 또는 explicit ctx로 service까지 전달한다. concurrent create 100개를 실행해 log ID가 섞이지 않는지 확인한다. context에는 body 전체를 넣지 않는다.

repository delay가 deadline을 넘으면 read/write operation을 취소할 수 있는지 fake signal로 확인한다. write commit 뒤 timeout이라면 outcome unknown 문제를 설명하고 단순 cancellation과 구분한다.

response header에도 requestId를 반환해 client failure report와 server log를 연결한다.

---

## CHAPTER 12 · body size와 response size를 측정해 resource budget을 눈으로 확인한다

8KB body limit을 경계값으로 test하고 8KB 초과 request는 parser/service 전에 거부한다. content max 5000자가 UTF-8 byte로는 더 클 수 있음을 확인한다. response byte 수와 serialization duration도 finish telemetry에 기록한다.

이 측정으로 field char limit과 transport byte limit이 다른 방어선임을 다시 확인한다. 모두 `크기 제한`이라고 부르지만 공격/운영 목적이 다르다.

boundary 바로 아래·위 payload를 자동 생성해 regression test한다.

---

## CHAPTER 13 · shutdown 중 slow request가 어떻게 처리되는지 종합한다

3초 slow save test route를 만들고 shutdown grace를 5초로 둔다. request 진행 중 SIGTERM을 보내 readiness가 false가 되고 새 request는 받지 않으며 기존 create는 완료되는지 확인한다. save 완료 후 response와 telemetry flush를 확인한다.

never-ending repository mode에서는 5초 deadline 뒤 forced path가 실행되는지 본다. graceful success와 forced shutdown을 다른 metric으로 기록한다. process cleanup이 database durability를 대체하지 않는다는 점을 기록한다.

---

## CHAPTER 14 · 사람이 직접 판단할 항목과 AI에 맡길 구현을 분리한다

AI에 맡기기 좋은 작업은 Express/Fastify route boilerplate, schema library syntax, DTO type 선언, curl fixture, table-driven test skeleton이다. 사람이 직접 확정해야 하는 것은 title/content 의미, body/field limit, unknown field policy, response public field, error code, timeout의 business 의미, shutdown completion 기준이다.

AI가 `모든 string trim`, `모든 error 500`, `save를 background로 보내 201을 빨리 반환` 같은 code를 제안하면 contract와 비교해 거부해야 한다. 생성 code를 평가할 기준이 먼저 있어야 한다.

실습 마지막에는 자신이 AI에 줄 prompt에 invariant와 forbidden behavior를 명시하고, 생성된 diff를 contract 표와 대조한다.

---

## CHAPTER 15 · 첫 BLOCK의 CLEAN 점검은 서로 다른 실패 범주를 실제로 재현한다

이번 통합 지점에서 동일 정상 request를 반복하는 대신 failure class를 나눈다. A는 연결/route: wrong port, unknown path, unsupported method. B는 parsing/resource: wrong media type, malformed JSON, oversized body. C는 validation/normalization: wrong type, unknown field, whitespace-only title. D는 async/error: repository reject, deadline, unexpected throw. E는 lifecycle: concurrent request context, SIGTERM drain, forced shutdown이다.

각 test는 `어느 단계에서 멈춰야 하는가`, `service가 호출돼야 하는가`, `external status/code`, `log/metric evidence`를 사전에 적고 실제 결과와 비교한다. 실행환경에서 수행하지 못한 검증은 PASS로 표시하지 않는다. static source review와 runtime test 결과도 별도로 기록한다.

BLOCK 01을 끝내면 request가 process에 도착한 순간부터 response 완료와 process 종료까지 경로를 설명할 수 있어야 한다. 다음 BLOCK의 관심사는 이 pipeline 안쪽에서 **business rule과 storage/external dependency 책임을 어떻게 분리할 것인가**로 이동한다.
