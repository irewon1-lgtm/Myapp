# PART 05 · BLOCK 01 · LESSON 05 · body parsing과 크기 제한을 business logic보다 앞에서 다루기

JSON body가 handler에서 object로 보이면 network에서 object가 그대로 전달됐다고 착각하기 쉽다. 실제 경로에는 bytes 수신, HTTP framing, content coding, media type 선택, charset 해석, parser 실행, runtime value 생성이 있다. 각 단계는 서로 다른 실패와 자원 비용을 만든다. 이 PART는 parsing을 단순 `JSON.parse` 문법으로 보지 않고 **외부 byte를 application이 다룰 수 있는 값으로 바꾸는 경계**로 다룬다. size limit, decompression, numeric precision, upload streaming 같은 문제도 이 경계에서 함께 판단한다.

---

## CHAPTER 01 · request body는 byte sequence에서 시작하고 parser가 구조를 만든다

HTTP body는 application object가 아니라 전송된 octet sequence다. server runtime은 connection에서 body chunk를 읽고 message framing에 따라 어디까지가 현재 request인지 판단한다. 그 다음 middleware나 framework가 `Content-Type`을 기준으로 JSON, form, multipart, binary parser를 선택할 수 있다. 이 단계를 지나야 `req.body.name` 같은 field 접근이 의미를 갖는다.

이 구분은 디버깅에서 중요하다. `req.body`가 비어 있을 때 business rule을 고치기 전에 client가 body를 실제로 보냈는지, media type이 맞는지, parser가 route보다 먼저 등록됐는지, size limit에 걸리지 않았는지 확인해야 한다. parser 이전에 실패한 request는 service 함수에 도달하지 않는다.

직접 raw body byte count와 parser 이후 object를 둘 다 기록하는 개발용 route를 만든다. `{ "name":"kim" }`을 보내고 byte 길이와 field를 비교한다. 같은 의미 JSON에 공백과 줄바꿈을 늘리면 object는 같아도 전송 byte 수가 달라지는 것을 확인한다. wire representation과 semantic value를 분리해 본다.

---

## CHAPTER 02 · Content-Type은 어떤 parser를 사용할지 정하는 입력 계약이다

`Content-Type: application/json`은 body가 JSON representation이라는 뜻을 전달한다. server가 header를 무시하고 모든 body를 JSON으로 parsing하면 다른 media type을 잘못 해석하고, 반대로 client가 JSON을 보내면서 header를 빠뜨리면 framework가 parser를 실행하지 않을 수 있다. parser 선택은 body 내용의 모양을 추측하는 방식보다 media type contract에 기반하는 편이 상호운용성이 높다.

media type에는 parameter가 붙을 수 있고 vendor-specific type이나 `+json` suffix를 사용할 수도 있다. API가 무엇을 지원할지 명시하고 unsupported type은 415 계열 response로 구분할 수 있다. body 형식을 지원하지 않는 실패와 JSON syntax가 잘못된 실패는 원인이 다르다.

실습에서는 같은 JSON bytes를 `application/json`, `text/plain`, header 없음으로 보내 framework behavior를 기록한다. 그 뒤 explicit media-type guard를 두고 허용 목록을 test한다. handler가 body 존재만 보고 format을 추측하지 않도록 만든다.

---

## CHAPTER 03 · charset decoding이 실패하면 JSON parser보다 앞에서 의미가 깨질 수 있다

JSON text는 문자 encoding을 거쳐 byte로 전송된다. 일반적인 API에서는 UTF-8을 사용하지만, text media type과 library 설정에 따라 charset parameter가 개입할 수 있다. invalid byte sequence를 decoder가 거부할지 replacement character로 바꿀지도 구현에 영향을 준다. 문자열 validation은 decoding이 끝난 뒤 값에 적용되므로 decoder behavior가 서로 다르면 security rule도 달라질 수 있다.

문자 길이 제한도 byte 길이와 Unicode code point, grapheme cluster 수가 다르다. body 전체는 byte 단위로 resource limit을 두고, field `displayName`은 사용자 의미에 맞는 문자열 길이 정책을 별도로 적용할 수 있다. `100KB body limit`과 `name 100자 limit`은 다른 방어선이다.

실습에서 한글, emoji, ASCII를 동일 문자 수만큼 담은 JSON을 만들고 byte size를 비교한다. 이후 body byte limit과 field length validation이 서로 다른 위치에서 실패하도록 test한다. 한 기준으로 두 문제를 해결하려 하지 않는다.

---

## CHAPTER 04 · JSON syntax 성공과 application validation 성공은 별개다

`{"age":-900}`은 JSON 문법상 완전한 object일 수 있다. parser는 key와 number를 올바르게 읽어 runtime value를 만들지만, 사람 나이라는 domain rule은 실패한다. parsing은 **문법적으로 구조를 읽을 수 있는가**, validation은 **우리 API contract에 맞는 값인가**를 묻는다.

반대로 `{"age":20`처럼 닫는 중괄호가 없는 payload는 schema validator에 도달할 수 없다. parser error를 `age가 잘못됐다`고 표현하면 client가 무엇을 고쳐야 할지 알기 어렵다. error response와 metric도 syntax failure와 validation failure를 나누면 incident 분석이 쉬워진다.

실습에서는 malformed JSON, valid JSON/wrong type, valid type/out-of-range 세 요청을 연속으로 보낸다. 각 요청이 parser, schema, domain rule 중 어디에서 멈추는지 기록한다. handler가 실행됐는지도 counter로 검증한다.

---

## CHAPTER 05 · JSON number는 언어 runtime 숫자 모델과 정확히 같지 않을 수 있다

JSON에는 `number`라는 문법 범주가 있지만 JavaScript의 일반 number는 IEEE 754 double precision으로 표현된다. 아주 큰 integer identifier를 JSON number로 받아 JavaScript number로 변환하면 정밀도를 잃을 수 있다. 서로 다른 언어가 같은 JSON number를 int64, decimal, double로 받을 때 결과가 달라질 수 있다.

identifier는 arithmetic이 필요하지 않다면 문자열로 표현하는 선택이 더 안전할 수 있다. 금액도 integer minor unit이나 decimal type을 사용하는 등 domain precision contract가 필요하다. `JSON이라서 언어 간 타입 문제가 해결된다`고 생각하지 않는다.

실습으로 `9007199254740991`, 그보다 큰 integer를 JSON으로 parsing하고 `Number.isSafeInteger`를 확인한다. stringify 후 원래 문자열과 값이 동일하게 보존되는지 비교한다. API schema에서 큰 ID를 string으로 바꿨을 때 client type과 validation이 어떻게 단순해지는지 본다.

---

## CHAPTER 06 · duplicate object member는 parser마다 다른 결과를 만들 수 있어 입력 계약을 좁혀야 한다

`{"role":"user","role":"admin"}`처럼 같은 key가 한 JSON object에 반복되면 parser가 마지막 값을 남기거나 첫 값을 남기거나 오류로 처리할 수 있다. 여러 component가 서로 다른 parser를 사용하면 gateway에서 검증한 의미와 application이 사용하는 의미가 달라지는 parser differential 문제가 생길 수 있다.

표준 문법이 어떤 자유도를 허용하는지와 별개로 public API는 duplicate member를 허용하지 않는 명확한 정책을 둘 수 있다. 일반 `JSON.parse`만으로 duplicate key를 감지하기 어렵다면 streaming parser 또는 gateway validation 등 추가 도구가 필요할 수 있다. 위험도가 높은 signed payload에서는 canonical representation도 중요해진다.

실습에서는 사용하는 runtime이 duplicate key를 어떻게 처리하는지 확인한다. 그 결과를 문서에 기록하고, security-sensitive payload에서 duplicate member를 차단해야 한다면 어떤 parser hook이나 schema gateway가 필요한지 조사한다. 추측으로 `마지막 값이 항상 표준`이라고 결론 내리지 않는다.

---

## CHAPTER 07 · body size limit은 memory·CPU·downstream 비용을 시작 전에 제한한다

body를 전부 memory에 모아 JSON parse하는 route에서 100MB payload를 허용하면 request 하나가 큰 allocation과 parse CPU를 소비한다. 그 뒤 validation에서 거부하더라도 이미 비용을 지불했다. size limit은 business validation보다 앞에서 적용해 과도한 body를 빨리 차단하는 resource budget이다.

limit 값은 endpoint 특성에 맞춘다. profile JSON과 image upload의 정상 크기는 완전히 다르다. global limit 하나를 지나치게 크게 잡아 모든 route가 대용량 body를 받게 만들기보다 parser 또는 route별 제한을 사용할 수 있다. reverse proxy에도 별도 limit이 있을 수 있어 application과 infrastructure의 실제 최소값을 알아야 한다.

실습에서는 JSON parser limit을 작게 설정하고 경계 바로 아래·정확히 경계·경계 초과 payload를 보낸다. handler 시작 log가 찍혔는지 확인해 rejection 위치를 증명한다. error body도 client가 `payload too large`를 구분할 수 있도록 만든다.

---

## CHAPTER 08 · 압축 body는 wire 크기보다 decompressed 크기가 훨씬 커질 수 있다

`Content-Encoding: gzip` 같은 압축을 허용하면 network로 전송된 byte는 작아도 decompression 후 메모리와 CPU 사용량이 크게 늘 수 있다. wire size만 제한하면 highly-compressible payload가 작은 요청처럼 보였다가 server 안에서 거대한 data로 팽창할 수 있다. decompressed byte budget과 compression ratio, CPU 시간을 함께 고려한다.

모든 framework가 compressed request body를 자동 지원하는 것은 아니다. reverse proxy가 먼저 풀어 주는지 application이 직접 푸는지도 architecture에 따라 다르다. limit이 압축 전 또는 후 어느 단계에 적용되는지 문서와 실험으로 확인해야 한다. `100KB limit`이라는 설정명만 보고 실제 방어 범위를 추측하지 않는다.

테스트 환경에서 반복 문자를 큰 JSON으로 만들고 gzip 크기와 원본 크기를 비교한다. compressed request 지원이 있다면 limit behavior를 실제로 측정한다. 지원이 없다면 명시적으로 reject하고 문서에 허용 encoding을 고정한다.

---

## CHAPTER 09 · form-urlencoded와 multipart는 JSON과 다른 parser 및 공격면을 가진다

HTML form은 `application/x-www-form-urlencoded` 또는 multipart form을 사용할 수 있다. urlencoded parser는 nested key convention, repeated key, parameter count와 depth 같은 설정을 가질 수 있다. 무제한 nesting을 object로 만들면 CPU와 memory 비용이 커질 수 있다. multipart는 boundary를 기준으로 text field와 file part를 streaming 방식으로 다룬다.

API가 JSON만 필요하다면 불필요한 parser를 전역으로 켜 두지 않는 편이 attack surface를 줄일 수 있다. file upload route에만 multipart parser를 적용하고, field count·file count·part size를 제한한다. parser feature가 많을수록 `어떤 형태를 실제로 허용하는가`를 좁히는 일이 중요해진다.

실습에서는 urlencoded repeated key와 nested key를 보내 실제 parser output을 확인한다. 이어 multipart test에서 text field 하나와 작은 file 하나를 받고, 허용하지 않은 두 번째 file이 들어오면 거부하도록 count limit을 적용한다.

---

## CHAPTER 10 · file upload는 filename보다 content와 저장 lifecycle을 관리해야 한다

client가 보내는 filename은 metadata일 뿐 신뢰할 수 있는 storage path가 아니다. `../../something` 같은 이름이나 매우 긴 Unicode 이름을 그대로 filesystem path에 붙이면 traversal과 portability 문제가 생길 수 있다. server는 내부 object key를 별도로 생성하고 원래 filename은 표시용 metadata로 보존할 수 있다.

`Content-Type: image/png`도 client 주장일 수 있다. 필요한 경우 실제 file signature와 decoder를 사용해 content를 검증하고, executable/script content가 public origin에서 실행되지 않도록 delivery policy를 설계한다. malware scanning이나 image re-encoding은 위협 모델에 따라 추가한다.

대용량 upload는 streaming으로 object storage에 보내면서 전체 memory 사용을 제한할 수 있다. 중간 실패 시 partial object cleanup과 DB metadata consistency도 필요하다. 실습에서는 filename을 직접 path로 사용하지 않고 random internal key를 생성하며 file size와 허용 media type을 검사한다.

---

## CHAPTER 11 · streaming parser는 partial chunk와 경계 분할을 정상 상황으로 처리해야 한다

network chunk는 JSON token이나 multipart boundary와 정확히 맞춰 오지 않는다. header 문자열 한가운데서 chunk가 나뉠 수도 있고 multibyte UTF-8 sequence가 분할될 수도 있다. streaming parser가 `chunk 하나 = record 하나`라고 가정하면 network timing에 따라 정상 payload를 깨뜨린다.

parser는 `아직 데이터가 부족함`, `완전한 token/record가 준비됨`, `명백히 invalid함`을 구분하는 state를 가진다. length prefix protocol이라면 header를 읽을 최소 byte가 도착할 때까지 기다리고, length가 max를 넘으면 추가 allocation 전에 거부한다. body stream이 끝났는데 incomplete structure라면 syntax error로 끝낸다.

실습은 동일 JSON/text record를 1-byte, 7-byte, random chunk로 나누어 custom line parser에 공급한다. 모든 chunking에서 같은 record 결과가 나오는지 property-style test를 만든다. parser가 네트워크 packet 경계에 의존하지 않는지 확인한다.

---

## CHAPTER 12 · client abort와 partial upload는 임시 자원을 정리해야 한다

1GB upload 중 700MB에서 client가 연결을 끊으면 server가 만든 temporary file이나 object-storage multipart upload가 남을 수 있다. connection close를 감지해 stream을 중단하고 partial resource를 cleanup해야 한다. 그러나 cleanup 자체도 실패할 수 있어 background reaper나 lifecycle policy가 필요할 수 있다.

DB에 `upload started` row를 먼저 만들었다면 status를 FAILED/ABORTED로 남길지 삭제할지 business audit 요구를 본다. file storage와 DB는 같은 transaction에 자동으로 묶이지 않으므로 partial failure를 state machine으로 다룬다. user retry가 이전 temporary resource와 충돌하지 않도록 upload ID를 고유하게 관리한다.

실습에서 large test stream을 전송하다 client를 중단하고 temp directory에 file이 남는지 확인한다. cleanup handler를 추가한 뒤 같은 실험을 반복하고, process crash처럼 handler가 실행되지 않는 경우를 위해 오래된 temp file cleanup job 설계를 적는다.

---

## CHAPTER 13 · parser error는 client가 고칠 수 있는 실패와 server 결함을 구분한다

malformed JSON, unsupported media type, size limit 초과, invalid encoding은 대개 request가 contract를 만족하지 못한 실패다. parser library 자체 bug나 out-of-memory, disk write failure는 server/infrastructure 실패일 수 있다. 모두 `SyntaxError` 문자열 하나로 처리하면 status와 alert가 뒤섞인다.

error middleware는 parser가 제공하는 error type/code를 application error taxonomy로 번역한다. client response에는 내부 stack과 filesystem path를 숨기고, server log에는 request ID와 parser stage, safe size metadata를 남긴다. raw body를 오류 log에 그대로 남기면 password나 token이 유출될 수 있으므로 payload 자체보다 길이·media type·schema field path 같은 최소 evidence를 선호한다.

실습에서는 malformed JSON, too-large body, unsupported type을 각각 보내 error response type과 metric counter가 다르게 증가하는지 확인한다. handler 강제 throw와 섞이지 않는지 검증한다.

---

## CHAPTER 14 · parsing metrics는 payload distribution과 abuse 징후를 보여 준다

request body size histogram, parse duration, rejected-too-large count, malformed rate를 route template별로 보면 API 사용 특성을 알 수 있다. 갑자기 평균 body가 10배 커지면 client bug나 abuse가 있을 수 있고, parse CPU가 latency의 큰 부분을 차지하면 payload design을 검토할 근거가 된다. metric label에는 raw user ID나 URL query를 넣지 않는다.

모든 body를 tracing span attribute에 복사하는 방식은 privacy와 telemetry 비용을 폭발시킨다. observability는 payload 자체를 저장하는 것이 아니라 단계별 latency와 결과 class를 구조화해서 수집한다. debugging에 sample payload가 필요하면 별도 redaction과 접근 통제를 가진 안전한 capture workflow를 설계한다.

실습에서는 body byte bucket과 parse result `ok|syntax_error|too_large|unsupported_type` counter를 남긴다. 정상 100회와 오류 burst를 보낸 뒤 distribution이 어떻게 달라지는지 확인한다. 이를 application validation metric과 별도 dashboard로 구분한다.

---

## CHAPTER 15 · parsing 실습은 bytes·format·budget·cleanup 네 축을 동시에 검증한다

`POST /documents`를 만들어 JSON metadata 또는 multipart file을 받되 route별 parser를 분리한다. JSON은 64KB, file은 5MB 같은 다른 상한을 적용하고, 지원 media type을 명시한다. JSON parser가 성공한 뒤 schema validation을 실행하며, upload는 internal object key를 사용하고 client filename은 metadata로만 저장한다.

실패 케이스는 malformed JSON, wrong media type, invalid UTF-8 처리, JSON limit 초과, compressed payload 정책 위반, too many multipart fields, oversized file, client abort를 포함한다. 각 실패에서 application service가 호출되지 않았는지 확인하고 temporary resource가 남지 않았는지 검사한다. 단순 4xx 응답만 보는 것으로 끝내지 않는다.

AI는 multipart library 사용법이나 parser option 목록을 조사하는 데 유용하다. 사람은 어떤 media type을 실제 지원할지, wire/decompressed/field/file 각각의 budget을 얼마로 둘지, partial resource를 어떻게 회수할지 결정한다. parsing boundary가 견고해야 뒤 validation과 business logic이 **이미 자원 폭주가 일어난 뒤 늦게 거부하는 구조**가 되지 않는다.
