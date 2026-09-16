package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack10 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C10-S01",
            depthTitle("디버깅을 “재현하고 로그 보기”에서 hypothesis tree·profiling·bisect로 올린다"),
            depthHeading("증상을 원인 후보 트리로 나눈다"),
            depthParagraph("""
“앱이 느리다”를 그대로 조사하지 않고 client rendering, network, API, DB, external dependency로 나눈 뒤 각 가지를 구분할 한 개의 측정값을 고른다. Network TTFB가 50ms인데 frame rendering이 2초라면 server 가지를 우선 제거할 수 있다. hypothesis tree는 모든 가능성을 동시에 고민하지 않고 가장 정보량이 큰 관찰로 범위를 절반씩 줄이는 방법이다.
"""),
            depthHeading("1. differential debugging"),
            depthParagraph("""
정상 환경과 실패 환경의 차이를 비교한다. app version, feature flag, DB data shape, locale, timezone, device, request header 중 무엇이 다른지 표로 만든다. 동일 code인데 특정 locale에서만 깨지면 날짜/숫자 parsing을, 특정 tenant에서만 느리면 data cardinality/skew를 의심할 수 있다. “내 PC에서는 됨”을 증거로 바꾸는 방식이다.
"""),
            depthHeading("2. profiler는 어디에 시간이/메모리가 쓰였는지 sampling한다"),
            depthParagraph("""
CPU profiler는 hot function과 call tree를, memory profiler/heap snapshot은 allocation과 retained object를 찾는 데 도움을 준다. 로그를 더 넣는 것보다 overhead가 적고 전체 분포를 보는 데 유리할 수 있다. profiler 자체가 timing을 바꿀 수 있어 production에서는 sampling rate와 안전한 도구를 사용한다.
"""),
            depthHeading("3. heap dump에서 retained size와 retaining path"),
            depthParagraph("""
큰 object 하나가 문제라기보다 작은 listener가 거대한 tree를 참조해 전체가 수집되지 않을 수 있다. shallow size는 객체 자체 크기, retained size는 이 객체 때문에 살아 있는 graph까지 포함한 크기다. GC root까지 retaining path를 따라가면 누가 reference를 잡고 있는지 찾을 수 있다.
"""),
            depthHeading("4. git bisect는 “어느 commit부터 깨졌나”를 이진 탐색한다"),
            depthParagraph("""
좋았던 commit과 나쁜 commit을 알고 있고 각 commit에서 자동 test로 good/bad를 판정할 수 있으면 Git이 중간 commit을 골라 원인 범위를 O(log n) 단계로 줄일 수 있다. 테스트가 flaky하면 bisect 결과도 오염되므로 먼저 재현을 안정화한다.
"""),
            depthHeading("5. heisenbug와 관찰 효과"),
            depthParagraph("""
race condition에 console.log나 debugger를 넣었더니 timing이 바뀌어 버그가 사라질 수 있다. 이런 경우 lock-free counter, trace timestamp, ring buffer처럼 timing 영향이 작은 관찰을 사용하고 반복률을 통계로 비교한다. “로그 넣으니 안 생김”은 해결이 아니라 timing 의존성의 힌트다.
"""),
            depthHeading("6. production replay에는 privacy와 side effect 통제가 필요하다"),
            depthParagraph("""
실제 request를 복제해 staging에서 재현하면 강력하지만 개인정보와 결제 같은 side effect를 그대로 replay하면 위험하다. sensitive field를 redaction/tokenization하고 external write를 stub하거나 sandbox account로 바꾼다. production snapshot도 최소 필요한 범위만 사용한다.
"""),
            depthCode("""
느린 화면 hypothesis tree

A. client render? -> Performance trace frame 1800ms
B. network?       -> TTFB 45ms
C. API?           -> server total 30ms

A로 범위 축소
  A1. layout? 1200ms
  A2. JS CPU? 400ms
  A3. image? 100ms

다음 관찰은 layout invalidation 횟수다.
"""),
            depthBullets(
                "버그 조사 메모에는 가설과 이를 반증할 증거를 함께 적는다.",
                "profiler/heap dump를 쓰기 전에 수집 비용과 개인정보 포함 여부를 확인한다.",
                "bisect 자동 판정 test가 flaky하지 않은지 먼저 검증한다."
            )
        ),
        expertPack(
            sectionId = "V1-C10-S02",
            depthTitle("테스트의 다음 층: property-based·mutation·contract·fuzz·determinism"),
            depthParagraph("""
V3에서 unit/integration/E2E와 경계값을 이미 배웠다면 이제 “내가 생각하지 못한 입력”과 “테스트 자체가 약한 경우”를 찾는 도구로 간다. 예제 기반 test는 선택한 몇 점을 검사하지만 property-based testing은 성질을 정의하고 많은 자동 생성 입력으로 반례를 찾는다.
"""),
            depthHeading("1. property-based testing"),
            depthParagraph("""
정렬 함수라면 특정 배열 [3,1,2]의 결과만 검사하지 않고 “결과 길이는 같음”, “모든 원소 multiset이 보존됨”, “인접 원소가 비감소” 같은 property를 정의한다. framework가 random/structured input을 생성하고 실패하면 작은 counterexample로 shrink해 준다. property 자체를 잘못 쓰면 의미 없는 test가 되므로 요구사항에서 성질을 뽑는다.
"""),
            depthHeading("2. mutation testing은 production code를 일부러 틀리게 바꾼다"),
            depthParagraph("""
>를 >=로, +를 -, 조건을 true로 바꾸는 mutation을 자동 생성했는데 기존 test가 모두 통과한다면 그 차이를 감지하는 test가 없다는 뜻이다. mutation score는 coverage보다 “테스트가 behavior 변화를 죽일 수 있는가”를 보는 보조 지표다. 살아남은 mutation을 모두 100% 죽이는 것이 목적은 아니고 위험한 로직의 test 빈틈을 찾는다.
"""),
            depthHeading("3. consumer-driven contract test"),
            depthParagraph("""
service A가 B의 response field를 사용한다면 B 전체를 띄운 E2E만 의존하지 않고 A가 기대하는 request/response contract를 artifact로 만들고 B build에서 검증할 수 있다. provider가 field를 제거하면 배포 전에 깨짐을 발견한다. 그러나 latency, auth infrastructure, 실제 DB 같은 runtime integration은 별도 test가 필요하다.
"""),
            depthHeading("4. fuzzing은 parser와 security boundary에 강하다"),
            depthParagraph("""
파일 parser, protocol decoder, API input에 random/coverage-guided byte를 대량으로 넣어 crash, hang, memory corruption, invariant violation을 찾는다. “정상 형식”만 생성하는 property test와 달리 완전히 깨진 입력도 공격한다. 발견한 crash input을 regression corpus에 저장한다.
"""),
            depthHeading("5. deterministic test를 만든다"),
            depthParagraph("""
현재 시간, random, network, thread scheduling에 직접 의존하면 같은 test가 가끔 실패한다. Clock, RandomSource, ID generator를 주입하고 async test는 “sleep 1초” 대신 특정 event/condition을 기다린다. time을 fake로 진행할 수 있으면 30일 만료 로직도 milliseconds 안에 검증할 수 있다.
"""),
            depthHeading("6. snapshot/golden test는 변화 검토 도구다"),
            depthParagraph("""
큰 JSON/UI 출력 전체를 snapshot으로 저장하면 예상치 못한 변경을 잡기 쉽지만 승인 버튼만 누르면 잘못된 결과도 새 정답이 된다. 핵심 business invariant는 명시적 assertion으로 두고 snapshot은 넓은 회귀 변화 탐지에 보조적으로 사용한다.
"""),
            depthCode("""
정렬 property 예
for all input arrays xs:
  ys = sort(xs)
  len(ys) == len(xs)
  multiset(ys) == multiset(xs)
  for every i: ys[i] <= ys[i+1]

실패 input이 10,000개짜리여도 shrinker가
[1, 0] 같은 작은 반례로 줄여 줄 수 있다.
"""),
            depthBullets(
                "line coverage 90%를 test 품질과 동일시하지 않는다.",
                "flaky test를 retry로 숨기기 전에 비결정성 source를 제거한다.",
                "contract test, integration test, E2E는 서로 다른 실패 범위를 담당한다."
            )
        ),
        expertPack(
            sectionId = "V1-C10-S03",
            depthTitle("Git과 배포를 변경 그래프에서 “재현 가능한 공급망과 안전한 migration”까지 연결한다"),
            depthHeading("merge와 rebase는 history의 의미를 다르게 만든다"),
            depthParagraph("""
merge는 두 branch의 parent를 가진 merge commit으로 실제 분기/합류를 보존하고, rebase는 commit을 새 parent 위에 다시 만들어 linear history를 만든다. 이미 공유된 commit을 rebase하면 SHA가 바뀌어 다른 사람 history와 충돌할 수 있다. “깔끔해서 rebase”보다 branch 공유 여부와 review 정책을 기준으로 선택한다.
"""),
            depthHeading("1. reproducible build와 provenance"),
            depthParagraph("""
같은 source tag를 build했는데 compiler version, dependency download, timezone, generated timestamp 때문에 artifact hash가 달라질 수 있다. toolchain/dependency pinning, deterministic archive, clean environment를 사용해 재현성을 높인다. build provenance는 어떤 source commit, builder, dependency로 artifact가 만들어졌는지 추적 가능하게 한다.
"""),
            depthHeading("2. SBOM과 dependency provenance"),
            depthParagraph("""
배포 artifact 안에 어떤 library/version이 들어갔는지 Software Bill of Materials로 기록하면 새 vulnerability가 발표됐을 때 영향 artifact를 빠르게 찾을 수 있다. package lockfile와 SBOM은 목적이 다르다. lockfile은 build dependency resolution을 고정하고, SBOM은 결과물 구성 요소의 inventory 역할을 한다.
"""),
            depthHeading("3. feature flag는 deploy와 release를 분리하지만 부채가 된다"),
            depthParagraph("""
새 code를 production에 배포하되 일부 사용자에게만 flag를 켜 canary할 수 있다. kill switch로 장애 기능을 끌 수도 있다. 그러나 오래된 flag가 수십 개 쌓이면 가능한 상태 조합이 폭발하고 test가 어려워진다. flag마다 owner와 제거 날짜를 정한다.
"""),
            depthHeading("4. expand-and-contract migration"),
            depthParagraph("""
DB schema나 event schema를 바꿀 때 producer/consumer가 동시에 업데이트되지 않는다는 가정에서 먼저 새 field를 optional로 추가하고 dual-read/write 기간을 둔 뒤 old 사용자가 사라진 것을 확인하고 제거한다. schema registry나 compatibility check를 CI에 넣으면 깨지는 변경을 막을 수 있다.
"""),
            depthHeading("5. canary와 blue-green"),
            depthParagraph("""
canary는 일부 traffic부터 새 version에 보내 error/latency를 비교해 점차 확대하고, blue-green은 old/new 환경을 나란히 두고 route를 전환해 rollback을 빠르게 한다. DB migration처럼 irreversible shared state가 있으면 application traffic만 되돌려도 완전 rollback이 안 될 수 있다.
"""),
            depthHeading("6. release verification은 “배포 성공” 다음 단계다"),
            depthParagraph("""
CI가 artifact upload 성공을 보여 줘도 production에서 실제 config, secret, traffic, DB schema와 함께 정상이라는 뜻은 아니다. deploy 후 synthetic smoke, key business metric, error rate, log anomaly를 확인하고 자동 rollback 조건을 정한다. 성공 보고는 build, deploy, runtime verification을 구분해서 한다.
"""),
            depthCode("""
안전한 release gate
source commit 고정
 -> reproducible build metadata
 -> tests/security scan
 -> artifact hash 기록
 -> canary 5%
 -> error rate/p99/business metric 확인
 -> 25% -> 50% -> 100%

각 단계 실패 시 어떤 상태를 rollback할지 미리 정의
"""),
            depthBullets(
                "공유된 history를 rebase하기 전에 팀 규칙과 영향 범위를 확인한다.",
                "feature flag에는 제거 조건과 owner를 둔다.",
                "DB/event schema가 포함된 release는 application rollback만으로 복구 가능한지 따로 검증한다."
            )
        )
    )
}
