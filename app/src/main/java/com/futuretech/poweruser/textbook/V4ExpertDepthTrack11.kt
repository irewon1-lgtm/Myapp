package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack11 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C11-S01",
            depthTitle("아키텍처를 “패턴 이름”에서 capacity·failure domain·decision record로 끌어올린다"),
            depthHeading("설계는 먼저 숫자를 거칠게 계산한다"),
            depthParagraph("""
DAU 100만, 사용자당 하루 20 request면 평균 RPS는 약 231이지만 peak가 평균의 10배라면 2300 RPS를 감당해야 할 수 있다. 사진 5MB를 하루 10만 장 저장하면 raw만 하루 500GB다. 정확한 예측이 아니라 order-of-magnitude 계산으로 어떤 자원이 지배적인지 찾는다. 숫자 없이 microservice나 cache부터 고르면 과설계와 병목을 동시에 만들 수 있다.
"""),
            depthHeading("1. Little's Law로 동시 요청 수를 감 잡는다"),
            depthParagraph("""
안정 상태에서 평균 동시 작업 수 L ≈ 도착률 λ × 평균 체류시간 W다. 초당 1000 request가 들어오고 평균 처리시간이 0.2초면 시스템 안에 평균 약 200 request가 존재한다. latency가 2초로 늘면 같은 traffic에서도 2000개가 쌓여 connection/thread/memory가 압박받는다. 성능 저하가 queue 증가를 통해 더 큰 저하로 이어지는 이유다.
"""),
            depthHeading("2. failure domain을 분리한다"),
            depthParagraph("""
모든 component를 같은 DB, same region, same credential에 묶으면 instance를 여러 개 두어도 공통 실패점이 남는다. availability zone, region, database cluster, queue partition, third-party provider 중 어디까지 장애를 독립적으로 견뎌야 하는지 요구사항을 정한다. 멀티리전은 비용과 data consistency 복잡성을 크게 올리므로 실제 RTO/RPO 요구가 있을 때 설계한다.
"""),
            depthHeading("3. modular monolith와 microservice를 조직 경계까지 본다"),
            depthParagraph("""
microservice는 작은 코드 파일이 아니라 독립 deploy, data ownership, network contract, 운영 책임을 만든다. 팀이 3명인데 20 service로 쪼개면 on-call, CI/CD, tracing, schema evolution 부담이 기능 개발보다 커질 수 있다. domain 경계가 선명하고 독립 scaling/deploy 필요가 생기기 전에는 modular monolith가 더 강한 선택일 수 있다.
"""),
            depthHeading("4. bounded context는 같은 단어의 의미 충돌을 분리한다"),
            depthParagraph("""
“고객”이 영업 시스템에서는 lead/contact 정보이고 결제 시스템에서는 billing identity일 수 있다. 하나의 거대한 Customer model에 모든 field를 넣으면 서로 다른 규칙이 충돌한다. context별 model과 contract를 두고 필요한 정보만 event/API로 변환한다.
"""),
            depthHeading("5. ADR은 결론과 함께 거절한 선택지를 남긴다"),
            depthParagraph("""
“Kafka 사용”만 기록하면 1년 뒤 왜 RabbitMQ/DB outbox를 쓰지 않았는지 알 수 없다. Architecture Decision Record에 context, options, decision, consequences, revisit condition을 짧게 남긴다. traffic가 10배가 되거나 provider 가격이 바뀌면 revisit한다는 조건까지 적으면 설계가 영구 진리가 아니라 당시 trade-off였음을 보존한다.
"""),
            depthCode("""
capacity sketch
peak RPS: 2,500
avg response: 40KB
outbound: 약 100MB/s
DB queries/request: 3 -> 7,500 QPS
cache hit target: 90%면 origin read 약 750 QPS

이 숫자에서 network/DB/cache 병목 후보를 먼저 찾고
그 다음 topology를 결정한다.
"""),
            depthBullets(
                "패턴 선택 전에 traffic, data volume, latency, RTO/RPO를 숫자로 적는다.",
                "service boundary는 독립 deploy/data ownership/운영 책임까지 포함한다.",
                "설계 결정에는 언제 다시 검토할지 조건을 남긴다."
            )
        ),
        expertPack(
            sectionId = "V1-C11-S02",
            depthTitle("분산 시스템의 핵심: ordering·dedupe·backpressure·consistency·lock의 한계"),
            depthParagraph("""
이 LESSON은 기존 V4의 좋은 방향을 유지하되 더 깊게 간다. 분산 환경에서는 message가 늦게 오고, 중복되고, 일부 node만 실패하고, 서로 다른 복제본이 잠시 다른 값을 볼 수 있다. “network call은 느린 함수 호출”이 아니라 실패 모델 자체가 다르다는 전제에서 설계한다.
"""),
            depthHeading("1. exactly-once는 범위를 말하지 않으면 의미가 모호하다"),
            depthParagraph("""
broker 내부에서 message offset을 exactly-once로 commit할 수 있어도 consumer가 외부 결제 API를 두 번 호출하지 않는다는 보장은 별도다. producer→broker, broker→consumer, consumer→DB, consumer→external side effect 각각의 경계에서 중복 가능성을 분석한다. end-to-end side effect를 한 번으로 만들려면 idempotency key와 transaction/ledger가 필요하다.
"""),
            depthHeading("2. ordering은 partition/key 범위에서만 보장될 수 있다"),
            depthParagraph("""
queue가 전체 message 순서를 보장하지 않고 같은 orderId key의 partition 안에서만 순서를 보장할 수 있다. 여러 worker가 서로 다른 partition을 병렬 처리하면 전체 global order는 없다. ordering이 필요한 entity key를 정하고, late event에는 version/sequence number로 오래된 update를 거부한다.
"""),
            depthHeading("3. optimistic concurrency와 compare-and-set"),
            depthParagraph("""
resource version=7을 읽은 client가 update할 때 WHERE version=7로 조건을 걸고 성공하면 version=8로 올린다. 다른 client가 먼저 version=8로 만들었다면 update count=0으로 conflict를 감지한다. distributed lock 없이도 “내가 본 상태가 아직 최신인가”를 검증할 수 있다. retry할 때는 사용자 변경을 자동 덮어쓸지 merge할지 정책이 필요하다.
"""),
            depthHeading("4. backpressure와 load shedding"),
            depthParagraph("""
consumer 처리량이 producer보다 낮으면 backlog가 무한히 늘지 않도록 입력을 늦추거나 일부 낮은 우선순위 작업을 거부해야 한다. queue 길이만 보고 worker를 무한 증설하면 DB가 새로운 병목이 될 수 있다. end-to-end bottleneck capacity를 보고 producer rate limit, batch size, worker count, downstream pool을 함께 조정한다.
"""),
            depthHeading("5. distributed lock의 lease와 fencing token"),
            depthParagraph("""
process A가 lock lease를 얻고 오래 멈춘 사이 lease가 만료돼 B가 새 lock을 얻을 수 있다. A가 다시 깨어나 old lock을 가진 줄 알고 storage를 덮으면 split-brain write가 된다. lock service가 증가하는 fencing token을 발급하고 storage가 더 작은 old token write를 거부하도록 만들면 stale owner를 차단할 수 있다.
"""),
            depthHeading("6. consistency level은 사용자 경험으로 번역한다"),
            depthParagraph("""
eventual consistency를 “언젠가 맞음”으로 끝내지 말고 프로필 수정 후 자기 화면에서는 즉시 새 이름을 보여 주는 read-your-writes가 필요한지, follower가 2초 stale해도 되는지 정의한다. strong consistency가 필요한 돈/재고와 stale 허용 가능한 추천/통계를 구분하면 비용을 줄일 수 있다.
"""),
            depthCode("""
optimistic update
row: id=O1, version=7, status=PAID

UPDATE orders
SET status='SHIPPED', version=8
WHERE id='O1' AND version=7;

updated rows = 1 -> 성공
updated rows = 0 -> 누군가 먼저 변경, 최신 row 재조회 후 conflict 처리
""", "sql"),
            depthBullets(
                "“exactly once”라는 말을 들으면 어느 경계까지 보장하는지 질문한다.",
                "distributed lock이 있어도 stale owner 문제와 downstream enforcement를 검토한다.",
                "backlog 증가는 worker 부족뿐 아니라 downstream bottleneck 신호일 수 있다."
            )
        ),
        expertPack(
            sectionId = "V1-C11-S03",
            depthTitle("확장과 안정성의 마지막 층: saturation·tail latency·circuit breaker·SLI/SLO·error budget·chaos"),
            depthHeading("scale out 전에 saturation 지표를 찾는다"),
            depthParagraph("""
CPU 40%인데 느리다고 web server를 늘리면 해결되지 않을 수 있다. DB connection pool 100%, thread pool queue 증가, disk IOPS 한계, external API quota처럼 실제로 꽉 찬 자원을 saturation이라고 본다. 가장 좁은 병목 앞에 instance를 더 붙이면 오히려 그 병목으로 traffic을 더 빨리 밀어 넣는다.
"""),
            depthHeading("1. tail latency는 fan-out에서 증폭된다"),
            depthParagraph("""
한 request가 20개 downstream을 병렬 호출하고 “모두 끝나야” 응답한다면 각 호출의 평균이 빨라도 가장 느린 하나가 전체 latency를 결정한다. 각 service p99가 500ms일 때 fan-out이 커질수록 적어도 하나가 tail에 걸릴 확률이 증가한다. timeout, hedged request(주의), partial response, cache로 tail을 줄이는 전략이 필요하다.
"""),
            depthHeading("2. circuit breaker와 bulkhead를 함께 구분한다"),
            depthParagraph("""
breaker는 실패하는 dependency 호출을 잠시 차단하고, bulkhead는 기능별 thread/connection/concurrency pool을 나눠 장애 자원 전파를 막는다. payment provider가 느릴 때 payment pool만 포화되고 search/login은 살아 있게 만드는 것이 bulkhead다. 둘 다 모든 요청을 성공시키는 기능이 아니라 실패를 제한된 범위로 가두는 장치다.
"""),
            depthHeading("3. SLI, SLO, SLA"),
            depthParagraph("""
SLI는 실제 측정값(성공률, latency), SLO는 내부 목표(30일간 99.9% 성공), SLA는 고객과의 외부 계약/보상 조건일 수 있다. metric 수백 개를 모으는 것보다 사용자 경험을 대표하는 몇 개의 SLI를 정의하고 정확한 분모/제외 조건을 정한다. maintenance를 제외할지, client cancel을 error에 넣을지까지 명시한다.
"""),
            depthHeading("4. error budget은 안정성 목표를 의사결정에 연결한다"),
            depthParagraph("""
99.9% availability SLO라면 30일 기준 약 43분의 오류 예산이 있다. 최근 배포로 budget을 빠르게 소진하면 feature release 속도를 줄이고 reliability 작업을 우선하는 정책을 둘 수 있다. error budget은 장애를 허용하자는 뜻이 아니라 “무조건 100%”라는 불가능한 목표 대신 위험을 수치로 관리한다.
"""),
            depthHeading("5. load test는 평균 traffic 재현이 아니라 한계를 찾는다"),
            depthParagraph("""
steady load뿐 아니라 spike, soak, stress test를 나눠 본다. 몇 시간 지속 후 memory leak/connection leak이 보이는지, sudden burst에서 queue가 얼마나 쌓이는지, overload에서 503으로 빠르게 실패하는지 확인한다. production data를 그대로 쓰지 않고 대표 cardinality와 hot key 분포를 재현한다.
"""),
            depthHeading("6. chaos test는 “망가뜨리기”가 아니라 recovery hypothesis 검증이다"),
            depthParagraph("""
“한 instance가 죽어도 30초 안에 traffic이 다른 instance로 이동한다” 같은 가설을 세우고 통제된 범위에서 process kill, network delay, dependency error를 주입해 검증한다. 관측성과 rollback이 없는 chaos는 실험이 아니라 위험이다. 작은 blast radius와 명확한 abort condition을 둔다.
"""),
            depthHeading("7. 운영 검증의 마지막 질문"),
            depthParagraph("""
AI가 코드를 생성하고 unit test가 통과해도 실제 운영에서 필요한 것은 capacity, permission, rollback, observability, incident response까지의 증거다. “배포가 성공했다”와 “사용자 SLO가 유지된다”를 구분하고, 변경 전후 SLI와 business metric을 비교한다. 이것이 초급·중급 과정을 마친 학습자가 AI 결과물을 감독하는 기준이 된다.
"""),
            depthCode("""
30일 99.9% availability SLO
총 시간 ≈ 43,200분
허용 오류 budget ≈ 43.2분

이번 주 incident 25분 + deploy error 10분 = 35분 사용
남은 budget 약 8분

새 고위험 release를 계속할지,
reliability 개선을 먼저 할지 팀 정책으로 연결한다.
"""),
            depthBullets(
                "scale 결정 전에 CPU뿐 아니라 connection/thread/queue/quota saturation을 본다.",
                "SLO 계산식의 분모와 error 정의를 문서화한다.",
                "chaos/load test에는 blast radius와 중단 조건을 먼저 둔다.",
                "release 완료 판단은 CI 성공이 아니라 production SLI/핵심 사용자 흐름 검증까지 포함한다."
            )
        )
    )
}
