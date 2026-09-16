package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack06 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C06-S01",
            depthTitle("네트워크 요청 한 번을 DNS→connection→TLS→transport→packet loss까지 추적한다"),
            depthParagraph("""
V3에서 IP, port, TCP/UDP 같은 기본을 배웠다면 이제 브라우저 주소 하나가 실제 연결로 바뀌는 순서를 본다. 네트워크 장애를 “인터넷이 안 된다”로 묶지 않고 name resolution, route, transport handshake, TLS, application response로 분리하면 어디에서 실패했는지 증거를 모을 수 있다.
"""),
            depthHeading("1. DNS는 이름을 IP로 바꾸는 한 번의 전화번호부 조회가 아니다"),
            depthParagraph("""
OS와 browser cache, local resolver, recursive resolver가 관여할 수 있고 TTL 동안 결과가 cache된다. 한 domain이 여러 IP를 반환하거나 지역/상태에 따라 다른 결과를 줄 수도 있다. DNS 변경 직후 일부 사용자만 예전 서버로 가는 이유가 cache TTL일 수 있다. 장애 조사에서 “domain은 맞다”가 아니라 실제 resolver가 어떤 주소를 반환했는지 확인한다.
"""),
            depthHeading("2. TCP는 흐름제어와 혼잡제어를 구분한다"),
            depthParagraph("""
flow control은 receiver가 처리 가능한 속도보다 sender가 너무 많이 보내지 않게 하고, congestion control은 network 자체가 감당할 수 있는 전송량을 추정한다. packet loss나 RTT 증가가 있으면 전송 속도가 내려갈 수 있다. bandwidth가 큰 회선이어도 RTT가 길고 loss가 있으면 단일 connection 성능이 기대보다 낮을 수 있다.
"""),
            depthHeading("3. TLS handshake는 암호화 전에 상대와 key를 합의한다"),
            depthParagraph("""
client와 server는 지원 protocol/cipher 정보와 certificate chain을 교환하고, domain과 신뢰 체인을 검증한 뒤 session key를 만든다. certificate 날짜 만료, hostname 불일치, 중간 CA chain 누락은 HTTP application 코드에 도달하기 전에 실패할 수 있다. “server가 500을 냈다”와 TLS handshake 실패는 완전히 다른 층이다.
"""),
            depthHeading("4. NAT 때문에 private IP와 public endpoint가 다르다"),
            depthParagraph("""
가정/회사 network의 여러 기기는 private address를 사용하고 router가 외부 연결을 public address/port와 매핑할 수 있다. 외부에서 내부 service로 직접 들어오는 것은 별도 port forwarding/firewall 규칙이 없으면 불가능할 수 있다. container, VM, mobile tethering을 쓰면 NAT 층이 여러 번 생겨 “localhost가 누구인가”도 실행 위치에 따라 달라진다.
"""),
            depthHeading("5. MTU와 fragmentation은 큰 packet에서 숨어 있는 문제를 만든다"),
            depthParagraph("""
path가 허용하는 packet 크기보다 큰 data는 분할되거나 transport가 더 작은 segment로 보내야 한다. 특정 VPN/터널 환경에서 큰 request만 멈추는 경우 path MTU 문제를 의심할 수 있다. application 개발자가 매번 MTU를 조절하지는 않지만 “작은 ping은 되는데 큰 TLS/data가 실패” 같은 증상을 층별로 해석하는 데 도움이 된다.
"""),
            depthCode("""
https://api.example.com 요청
1) DNS: api.example.com -> 203.0.113.10
2) route/NAT/firewall 통과
3) TCP 또는 QUIC connection
4) TLS handshake + certificate 검증
5) HTTP request 전송
6) server 처리
7) response packet 수신

각 단계가 서로 다른 실패 코드와 측정값을 가진다.
"""),
            depthBullets(
                "DNS 결과, connect time, TLS time, TTFB를 분리해 측정한다.",
                "Wi-Fi 연결 아이콘만으로 외부 route와 DNS 정상 여부를 판단하지 않는다.",
                "application retry 전에 transport failure의 성격과 idempotency를 확인한다."
            )
        ),
        expertPack(
            sectionId = "V1-C06-S02",
            depthTitle("HTTP의 다음 층: cache validator·conditional request·retry semantics·protocol multiplexing"),
            depthHeading("HTTP cache는 “저장해 뒀다가 쓰기”보다 freshness와 validation 규칙이다"),
            depthParagraph("""
Cache-Control의 max-age 동안 fresh하면 origin에 묻지 않고 사용할 수 있고, stale해지면 ETag/If-None-Match나 Last-Modified/If-Modified-Since로 “내용이 바뀌었는지”만 확인해 304를 받을 수 있다. private/public, no-store, must-revalidate 같은 directive는 민감 데이터와 CDN cache 동작을 바꾼다. cache bug는 오래된 값 문제와 개인정보 노출 문제 둘 다 만들 수 있다.
"""),
            depthHeading("1. idempotency와 safe method는 같은 속성이 아니다"),
            depthParagraph("""
GET은 보통 safe하고 idempotent하게 설계되지만 PUT은 state를 바꾸면서도 같은 representation으로 여러 번 보내면 최종 상태가 같아 idempotent할 수 있다. POST는 일반적으로 그렇지 않다. network timeout 뒤 client가 response를 못 받았다고 server 처리도 실패한 것은 아니므로 결제/주문 생성에는 idempotency key 같은 중복 방지 계약이 필요하다.
"""),
            depthHeading("2. Range request와 resume"),
            depthParagraph("""
큰 파일 download가 중간에 끊겼을 때 처음부터 다시 받지 않고 Range header로 특정 byte 구간을 요청할 수 있다. server는 Accept-Ranges와 206 Partial Content로 응답할 수 있다. resumable download/upload는 단순 retry보다 이미 성공한 구간을 식별하고 integrity를 검증하는 protocol이 필요하다.
"""),
            depthHeading("3. HTTP/2 multiplexing과 head-of-line 문제"),
            depthParagraph("""
HTTP/1.1에서는 여러 요청을 병렬 처리하려고 connection을 여러 개 쓰는 전략이 흔했다. HTTP/2는 하나의 TCP connection 안에서 여러 stream을 multiplex하지만 TCP packet loss는 connection 전체 stream 진행에 영향을 줄 수 있다. HTTP/3는 QUIC 위의 독립 stream으로 transport-level head-of-line blocking을 줄이려 한다. application method 의미는 유지돼도 transport 특성이 달라진다.
"""),
            depthHeading("4. proxy와 CDN을 거치면 “server 하나”가 아니다"),
            depthParagraph("""
client 앞에는 corporate proxy가, server 앞에는 CDN/load balancer/reverse proxy가 있을 수 있다. X-Forwarded-For, Host, TLS termination, cache header가 중간 계층에서 바뀔 수 있다. 실제 client IP나 scheme을 application이 신뢰할 때는 어떤 proxy만 trusted인지 명확히 설정해야 spoofing을 막을 수 있다.
"""),
            depthHeading("5. status code는 retry policy와 연결한다"),
            depthParagraph("""
429는 rate limit, 503은 일시 unavailable일 수 있지만 모든 5xx를 자동 retry하면 더 큰 장애를 만들 수 있다. Retry-After, request method/idempotency, 최대 횟수, exponential backoff+jitter를 함께 사용한다. 400 validation error처럼 같은 요청을 그대로 다시 보내도 성공하지 않을 오류는 retry하지 않는다.
"""),
            depthCode("""
조건부 cache 요청

첫 응답:
ETag: \"abc123\"
Cache-Control: max-age=60

60초 뒤:
If-None-Match: \"abc123\"

변경 없음 -> 304 Not Modified, body 없음
변경 있음 -> 200 + 새 body + 새 ETag

네트워크 byte를 줄이면서 최신성도 확인한다.
"""),
            depthBullets(
                "timeout은 “server가 처리 안 함”이 아니라 “client가 결과를 모름” 상태일 수 있다.",
                "retry 여부는 method 이름만 아니라 실제 side effect와 idempotency contract로 정한다.",
                "proxy/CDN이 있는 운영에서는 request가 거친 hop과 header 변형을 추적한다."
            )
        ),
        expertPack(
            sectionId = "V1-C06-S03",
            depthTitle("MIME 복습을 끝내고 안전한 대용량 업로드 pipeline을 설계한다"),
            depthParagraph("""
V3가 이미 MIME, Content-Type/Accept, multipart boundary, FormData의 boundary 문제까지 설명했으므로 그 내용을 반복하지 않는다. 실제 서비스에서 파일 업로드를 만들 때는 “어디로 업로드하는가, 전체를 메모리에 올리는가, 실제 형식을 어떻게 검증하는가, 중간 실패를 어떻게 복구하는가”가 다음 단계다.
"""),
            depthHeading("1. application server를 거치지 않는 presigned upload"),
            depthParagraph("""
대용량 영상을 client→API server→object storage로 두 번 전송하면 server bandwidth와 memory가 낭비된다. API가 짧은 수명의 presigned URL과 허용 key/size 조건을 발급하고 client가 object storage로 직접 upload하게 할 수 있다. 업로드 완료 후 server가 callback/event 또는 client confirm을 통해 metadata를 검증하고 DB 상태를 COMPLETED로 바꾼다. URL 권한과 만료를 좁게 잡아야 한다.
"""),
            depthHeading("2. streaming은 파일 전체를 RAM에 올리지 않는다"),
            depthParagraph("""
multipart parser가 전체 body를 byte[]로 만든 뒤 저장하면 동시 대용량 upload 몇 건만으로 memory가 고갈될 수 있다. stream을 chunk 단위로 읽어 임시 storage에 쓰고, size limit을 읽는 도중 강제한다. content-length가 없거나 거짓일 수도 있으므로 실제 수신 byte를 세며 상한을 넘으면 즉시 중단한다.
"""),
            depthHeading("3. magic byte와 parser 검증은 단계가 다르다"),
            depthParagraph("""
JPEG signature가 맞다고 정상 이미지라는 보장은 없다. 헤더만 위조하거나 parser 취약점을 노린 crafted file일 수 있다. allow-list format, 실제 decoder/parser, dimension/pixel limit, 압축 해제 크기 제한을 사용한다. 이미지 처리 worker를 낮은 권한과 격리된 환경에서 실행하면 parser 취약점 피해 범위를 줄일 수 있다.
"""),
            depthHeading("4. checksum으로 전송 무결성을 확인한다"),
            depthParagraph("""
client가 SHA-256 같은 checksum을 계산해 metadata로 보내고 storage/server가 실제 upload 결과와 비교하면 전송·조립 오류를 탐지할 수 있다. checksum은 악성 파일 여부를 판정하는 보안 스캐너가 아니라 “받은 byte가 기대한 byte와 같은가”를 확인하는 무결성 도구다.
"""),
            depthHeading("5. resumable upload에는 part 상태가 필요하다"),
            depthParagraph("""
10GB 파일을 9GB까지 올리고 network가 끊겼다면 part 단위 upload id와 완료된 range를 기록해 나머지만 보낼 수 있다. 마지막 complete 단계에서 모든 part의 순서와 checksum을 검증하고 하나의 object로 확정한다. 오래된 미완료 part를 정리하는 lifecycle 정책도 필요하다.
"""),
            depthHeading("6. 업로드 후 quarantine"),
            depthParagraph("""
외부 파일을 바로 public URL로 제공하지 않고 QUARANTINED 상태에 두고 virus/malware scan, format normalize, metadata 제거를 거친 뒤 READY로 승격할 수 있다. 처리 실패 파일은 사용자에게 명확한 상태를 보여 주고 저장소에서 자동 정리한다. 업로드 성공 HTTP 200과 “서비스에서 안전하게 사용 가능”은 다른 상태다.
"""),
            depthCode("""
대용량 이미지 업로드 상태 머신

CREATED
 -> UPLOADING (presigned URL)
 -> UPLOADED (checksum 확인)
 -> QUARANTINED (scan/decoder)
 -> PROCESSING (thumbnail/metadata strip)
 -> READY

실패 시 FAILED + 원인
만료된 CREATED/UPLOADING은 cleanup job으로 제거
"""),
            depthBullets(
                "파일 크기 제한은 header 값만 믿지 않고 실제 stream byte로 강제한다.",
                "업로드 성공과 검사/처리 완료를 같은 상태로 표현하지 않는다.",
                "presigned URL은 object key, method, content 조건, 만료를 최소 권한으로 제한한다."
            )
        ),
        expertPack(
            sectionId = "V1-C06-S04",
            depthTitle("API를 endpoint 목록이 아니라 “장기 호환 contract”로 설계한다"),
            depthHeading("schema evolution은 client가 동시에 업데이트되지 않는다는 현실에서 시작한다"),
            depthParagraph("""
mobile app, 외부 partner, 오래 켜진 browser client는 server와 다른 시점의 버전을 사용할 수 있다. response field를 갑자기 삭제하거나 enum 값을 추가해도 old client가 깨질 수 있다. additive change, tolerant reader, deprecation 기간, version negotiation을 사용하고 contract test로 호환성 위반을 잡는다.
"""),
            depthHeading("1. cursor pagination은 데이터가 움직이는 목록에 강하다"),
            depthParagraph("""
OFFSET 10000 LIMIT 20은 앞의 row를 많이 건너뛰어야 할 수 있고, 페이지 사이에 새 row가 들어오면 중복/누락이 생길 수 있다. created_at,id 같은 stable sort key를 cursor에 담아 “이 항목 이후”를 요청하면 변화하는 feed에서 더 안정적이다. cursor는 client가 내부 DB offset을 조작하지 못하도록 opaque하게 만드는 편이 좋다.
"""),
            depthHeading("2. webhook은 server가 client로 보내는 API다"),
            depthParagraph("""
결제 완료처럼 비동기 event를 partner에게 알릴 때 webhook을 사용한다. network 실패 때문에 같은 event를 여러 번 보낼 수 있으므로 event id와 retry 정책을 제공하고 receiver는 idempotent하게 처리한다. attacker가 가짜 webhook을 보내지 못하도록 shared secret HMAC signature와 timestamp를 검증하고 replay window를 둔다.
"""),
            depthHeading("3. OpenAPI/schema는 문서이면서 자동 검사 입력이 될 수 있다"),
            depthParagraph("""
request/response type, required field, enum, error schema를 기계가 읽을 수 있게 정의하면 client SDK, mock server, validation, contract test에 재사용할 수 있다. 그러나 schema에 표현되지 않은 비즈니스 규칙까지 자동 보장되는 것은 아니므로 예: “startAt < endAt” 같은 관계 규칙은 별도 validation과 예제로 남긴다.
"""),
            depthHeading("4. rate limit contract를 응답에 드러낸다"),
            depthParagraph("""
사용자가 제한을 넘었을 때 그냥 500을 주면 client가 retry 폭풍을 만들 수 있다. 429와 Retry-After 또는 provider의 limit header를 사용해 언제 다시 시도할지 알려 준다. 사용자별, API key별, endpoint 비용별로 bucket을 나눌 수 있고 비싼 report endpoint는 읽기 API와 다른 quota가 필요할 수 있다.
"""),
            depthHeading("5. 오류 code는 인간 문장과 분리한다"),
            depthParagraph("""
\"이미 존재합니다\" 같은 message는 번역과 문구 변경에 취약하다. machine-readable error code(DUPLICATE_EMAIL), field path, request id를 제공하고 message는 사용자 표시용으로 둔다. client가 message 문자열을 비교해 로직을 분기하지 않게 한다.
"""),
            depthCode("""
cursor response 예
{
  \"items\": [...],
  \"nextCursor\": \"opaque-token\"
}

다음 요청
GET /orders?after=opaque-token&limit=20

server는 cursor 안의 sort key를 검증하고
항상 같은 정렬 기준(created_at, id)으로 이어서 조회한다.
""", "json"),
            depthBullets(
                "API 변경은 server deploy 하나로 끝나지 않고 구버전 client 생존 기간을 포함한다.",
                "webhook은 중복 delivery와 signature 검증을 기본 전제로 둔다.",
                "error message 문자열을 client의 machine contract로 사용하지 않는다."
            )
        ),
        expertPack(
            sectionId = "V1-C06-S05",
            depthTitle("브라우저 네트워크 기능을 운영 안정성까지 연결한다: cache stampede·SSE replay·circuit breaker"),
            depthHeading("cache stampede는 인기 key 하나가 원본을 쓰러뜨릴 수 있다"),
            depthParagraph("""
인기 상품 cache TTL이 같은 순간 만료되면 수천 request가 동시에 DB로 miss를 일으킬 수 있다. single-flight/request coalescing으로 한 request만 원본을 갱신하고 나머지는 그 결과를 기다리게 하거나, TTL에 jitter를 넣어 만료 시점을 분산하고 stale-while-revalidate로 오래된 값을 잠깐 제공할 수 있다. cache hit ratio만 높이는 것보다 miss가 몰릴 때의 동작이 중요하다.
"""),
            depthHeading("1. negative caching"),
            depthParagraph("""
존재하지 않는 user id를 공격자가 계속 요청하면 “없음” 결과도 매번 DB를 칠 수 있다. 짧은 TTL로 not-found를 cache하는 negative caching이 도움이 될 수 있다. 하지만 데이터가 곧 생성될 수 있는 시스템에서는 너무 긴 negative TTL이 새 데이터를 못 보게 하므로 의미에 맞게 짧게 설정한다.
"""),
            depthHeading("2. SSE는 재연결 시 마지막 event 위치를 복구할 수 있다"),
            depthParagraph("""
EventSource/SSE는 연결이 끊기면 자동 재연결할 수 있고 event id/Last-Event-ID를 사용해 server가 이후 event부터 다시 보내게 설계할 수 있다. 단순 “실시간 연결”보다 event log 보관 기간과 replay semantics가 중요하다. 보관 기간보다 오래 끊겼다면 snapshot을 다시 받고 stream을 이어야 할 수 있다.
"""),
            depthHeading("3. WebSocket에도 backpressure가 있다"),
            depthParagraph("""
client가 초당 10개만 처리하는데 server가 10,000개 message를 보내면 socket buffer와 application queue가 계속 커질 수 있다. queue 상한, sampling/coalescing, slow consumer disconnect, subscription 범위 제한을 둬야 한다. connection이 열려 있다는 것과 소비자가 건강하다는 것은 다르다.
"""),
            depthHeading("4. circuit breaker 상태"),
            depthParagraph("""
외부 결제 API가 계속 timeout이면 모든 request가 timeout까지 기다리며 thread/connection을 소모한다. circuit breaker는 일정 실패 조건에서 OPEN으로 전환해 호출을 빠르게 실패시키고, 잠시 뒤 HALF_OPEN에서 소수 probe로 회복 여부를 확인한다. timeout과 retry를 먼저 두고 breaker는 반복 실패가 시스템 전체를 잠그는 것을 막는 추가 보호다.
"""),
            depthHeading("5. correlation id와 distributed trace"),
            depthParagraph("""
브라우저 request id를 gateway→service→DB/외부 API까지 전달하면 Network 패널에서 본 실패와 server trace/log를 연결할 수 있다. 서로 다른 시스템의 clock이 완전히 맞지 않아도 trace span의 parent-child 관계와 duration으로 병목을 좁힐 수 있다. 민감정보를 id에 넣지 않고 무작위 식별자를 사용한다.
"""),
            depthCode("""
circuit breaker 상태

CLOSED: 정상 호출, 실패율 측정
 -> 실패 임계 초과
OPEN: 외부 호출하지 않고 빠르게 실패/fallback
 -> cool-down 후
HALF_OPEN: 제한된 probe만 허용
 -> 성공하면 CLOSED
 -> 실패하면 다시 OPEN
"""),
            depthBullets(
                "cache TTL이 같은 인기 key에는 만료 동시성 전략을 둔다.",
                "실시간 연결은 reconnect/replay/slow consumer 정책까지 포함한다.",
                "retry와 circuit breaker를 둘 다 쓸 때 총 대기시간과 호출 증폭을 계산한다."
            )
        )
    )
}
