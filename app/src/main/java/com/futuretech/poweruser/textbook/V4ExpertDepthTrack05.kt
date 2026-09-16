package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack05 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C05-S01",
            depthTitle("JavaScript 객체와 함수의 깊은 층: identity·prototype·closure lifetime·this binding"),
            depthHeading("값이 같은 것과 같은 객체인 것은 다르다"),
            depthParagraph("""
{x:1} 두 개는 내용이 같아 보여도 서로 다른 identity를 가진 객체다. Map/Set key, memoization, UI diff에서 identity가 중요할 수 있다. 반대로 같은 객체 reference를 공유하면 한 곳의 mutation이 다른 곳에서도 보인다. 구조적 동등성과 reference 동등성을 구분하지 않으면 cache miss, state update 누락 같은 버그가 생긴다.
"""),
            depthHeading("1. property lookup은 prototype chain을 따라갈 수 있다"),
            depthParagraph("""
obj.method가 own property에 없으면 prototype 쪽을 탐색할 수 있다. class syntax도 이 모델 위에 만들어진다. instance마다 method를 복제하지 않고 prototype에서 공유할 수 있지만, prototype을 runtime에 무심코 수정하면 모든 instance에 영향이 갈 수 있다. hasOwn과 in의 차이를 이해해야 외부 객체를 안전하게 순회할 수 있다.
"""),
            depthHeading("2. closure가 오래 사는 만큼 잡고 있는 데이터도 오래 살 수 있다"),
            depthParagraph("""
event handler가 큰 object를 closure로 참조하고 listener가 제거되지 않으면 화면에서 element가 사라져도 참조 경로가 남아 garbage collection이 늦어질 수 있다. closure는 유용한 상태 캡슐화 도구지만 “함수 실행이 끝났으니 지역 변수는 무조건 사라진다”는 가정은 틀리다. memory leak 조사에서 heap snapshot의 retaining path를 보는 이유다.
"""),
            depthHeading("3. this는 함수가 정의된 위치만으로 결정되지 않는다"),
            depthParagraph("""
일반 함수의 this는 호출 형태(call-site)에 따라 달라질 수 있고, arrow function은 자신만의 this를 만들지 않고 lexical this를 캡처한다. method를 변수에 떼어 전달하면 receiver 정보가 사라질 수 있다. bind/call/apply와 arrow function을 쓰기 전에 “누가 이 함수를 어떤 형태로 호출하는가”를 확인한다.
"""),
            depthHeading("4. property descriptor와 immutability"),
            depthParagraph("""
const는 변수 binding 재대입을 막지만 object 내부 mutation까지 막지 않는다. Object.freeze는 얕은 수준의 property 변경을 막지만 중첩 객체까지 자동 deep freeze하는 것은 아니다. descriptor의 writable/configurable/enumerable 같은 속성은 library API가 객체 표면을 어떻게 통제하는지 이해하는 데 도움이 된다.
"""),
            depthHeading("5. garbage collection은 “안 쓰는 것처럼 보임”이 아니라 도달 가능성을 본다"),
            depthParagraph("""
현대 JS engine은 root에서 reference graph를 따라 reachable object를 판단한다. cycle이 있다는 이유만으로 leak이 되는 것은 아니며, root에서 더 이상 도달할 수 없으면 서로 cycle을 이루어도 수집 가능하다. 반대로 global cache나 listener가 reference를 붙잡고 있으면 화면에서 안 보이는 object도 살아 있을 수 있다.
"""),
            depthCode("""
class Timer {
  constructor() { this.seconds = 0 }
  tick() { this.seconds++ }
}

const t = new Timer()
const f = t.tick

// f() 호출에서 this가 t라는 보장은 없다.
// const f = t.tick.bind(t) 처럼 호출 계약을 고정할 수 있다.
// 또는 API가 receiver를 잃지 않는 구조인지 설계한다.
""", "javascript"),
            depthBullets(
                "객체 버그에서는 value, identity, reference path를 따로 본다.",
                "memory leak은 heap 크기보다 “누가 이 객체를 계속 붙잡는가”를 추적한다.",
                "this 문제는 함수 본문보다 call-site를 먼저 확인한다."
            )
        ),
        expertPack(
            sectionId = "V1-C05-S02",
            depthTitle("배열 처리 다음 단계: pure transformation·iterator·generator·backpressure의 입구"),
            depthParagraph("""
map/filter/reduce 문법을 이미 안다면 중요한 것은 chain을 짧게 만드는 기술이 아니라 데이터가 언제 만들어지고 얼마나 메모리에 머무는지다. 백만 건을 map→filter→map으로 처리하면 구현에 따라 중간 배열을 여러 개 만들 수 있다. 작은 데이터에서는 가독성이 우선이지만 streaming pipeline에서는 iterator/generator로 한 항목씩 처리하는 설계가 필요하다.
"""),
            depthHeading("1. pure transformation은 재실행 가능한 계산을 만든다"),
            depthParagraph("""
map callback이 외부 배열을 push하거나 전역 counter를 바꾸면 결과가 iteration 순서와 실행 횟수에 의존한다. 가능한 변환 단계는 입력→출력만 만들고 side effect를 마지막 경계로 몰아두면 병렬화, retry, test가 쉬워진다. “함수형이 멋져서”가 아니라 재실행 안전성과 reasoning 비용을 줄이기 위한 선택이다.
"""),
            depthHeading("2. iterator는 pull model이다"),
            depthParagraph("""
소비자가 next를 요청할 때 생산자가 값을 하나 주는 구조는 소비 속도에 맞춰 진행할 수 있다. 파일 stream이나 generator로 큰 데이터를 처리할 때 전체를 미리 메모리에 만들지 않아도 된다. 하지만 외부 source가 push 방식으로 더 빠르게 데이터를 보내는 경우에는 buffer와 backpressure가 필요하다.
"""),
            depthHeading("3. generator는 control flow를 중단했다가 이어 간다"),
            depthParagraph("""
yield는 함수가 현재 실행 상태를 보존한 채 값을 하나 내보내고, 다음 요청에서 그 지점 이후를 이어서 실행하게 한다. 일반 함수처럼 return으로 완전히 끝나는 것과 다르다. 무한 sequence도 필요할 때만 값을 생성할 수 있지만, 소비자가 끝없이 요청하면 끝나지 않으므로 limit과 cancellation을 둬야 한다.
"""),
            depthHeading("4. JSON serialization은 경계에서 정보가 사라진다"),
            depthParagraph("""
Date, Map, Set, BigInt, class instance는 JSON으로 단순 stringify할 때 의미가 사라지거나 오류가 날 수 있다. undefined property가 빠지거나 number 정밀도가 다른 언어와 충돌할 수도 있다. API contract에서는 “JS object를 보낸다”가 아니라 wire format의 field type, optional/null, date format, integer range를 문서화한다.
"""),
            depthHeading("5. pipeline을 측정하는 기준"),
            depthParagraph("""
처리량(초당 몇 건), latency(한 항목이 끝날 때까지), memory high-water mark를 함께 본다. batch를 크게 하면 throughput은 좋아질 수 있지만 한 항목 latency와 메모리가 늘고, 너무 작으면 per-call overhead가 커진다. 실제 workload로 batch size를 결정한다.
"""),
            depthCode("""
function* lines(file) {
  // 개념 예시: 파일 전체를 배열로 만들지 않고
  // 다음 줄이 필요할 때 하나씩 yield
}

for (const line of lines(bigFile)) {
  if (isValid(line)) process(line)
}

핵심은 “문법이 짧다”가 아니라
메모리에 전체 데이터를 동시에 두지 않는다는 점이다.
""", "javascript"),
            depthBullets(
                "map/filter chain의 가독성뿐 아니라 중간 allocation을 실제 profiler로 확인한다.",
                "wire format에서 Date/BigInt/optional/null 의미를 명시한다.",
                "producer가 consumer보다 빠를 수 있는 경계에서는 buffer 상한과 backpressure를 설계한다."
            )
        ),
        expertPack(
            sectionId = "V1-C05-S03",
            depthTitle("Promise/event loop 복습을 끝내고 동시성 제한·취소·async iterator로 간다"),
            depthParagraph("""
V3에 Promise, async/await, call stack, task, microtask, Promise.all, timeout, cancellation까지 이미 있다. 따라서 A-D-C-B 실행 순서를 다시 맞히는 데 시간을 쓰지 않는다. 실무에서 더 중요한 문제는 “동시에 몇 개를 시작할 것인가”, “부분 실패를 어떻게 모을 것인가”, “더 이상 필요 없는 작업을 어떻게 취소할 것인가”다.
"""),
            depthHeading("1. Promise.all은 무제한 동시성 제어기가 아니다"),
            depthParagraph("""
URL 10,000개를 map(fetch)한 뒤 Promise.all에 넣으면 10,000개 요청을 거의 한꺼번에 시작할 수 있다. 브라우저/OS connection limit이 있어도 queue와 memory가 커지고 server rate limit을 때릴 수 있다. worker pool 또는 semaphore로 “동시에 최대 8개”처럼 concurrency limit을 둔다. throughput은 높이되 상대 시스템과 자신의 자원을 보호하는 설계다.
"""),
            depthHeading("2. all, allSettled, race, any는 실패 의미가 다르다"),
            depthParagraph("""
모두 성공해야 의미 있는 작업은 Promise.all이 자연스럽고, 각 결과를 독립 보고해야 하면 allSettled가 유용하다. race는 첫 settled 결과(성공/실패)를 받고, any는 첫 성공을 찾으며 모두 실패하면 aggregate failure가 난다. 이름을 외우기보다 “부분 실패를 허용하는가, 첫 성공인가, 첫 완료인가”로 선택한다.
"""),
            depthHeading("3. cancellation은 signal을 전파해야 실제로 멈춘다"),
            depthParagraph("""
Promise 자체를 무시한다고 underlying network나 stream 작업이 자동 중단되는 것은 아니다. AbortSignal 같은 취소 신호를 API 계층을 따라 전달하고, 각 단계가 취소를 관찰해 자원을 정리해야 한다. 화면 이동 시 fetch만 취소하고 그 뒤 시작한 image decode나 worker가 계속 돌면 전체 작업은 멈춘 것이 아니다.
"""),
            depthHeading("4. async iterator는 비동기 streaming을 순차 문법으로 읽게 한다"),
            depthParagraph("""
서버에서 chunk가 도착할 때마다 다음 값을 비동기로 얻는 stream은 for await...of로 소비할 수 있다. 전체 response가 끝날 때까지 기다리지 않고 일부 결과부터 처리할 수 있어 AI streaming response, log tail, 대용량 download에 유용하다. 다만 consumer가 느릴 때 buffer가 어떻게 제한되는지 API의 backpressure semantics를 확인한다.
"""),
            depthHeading("5. unhandled rejection은 운영 신호다"),
            depthParagraph("""
fire-and-forget으로 Promise를 만들고 await/return/catch하지 않으면 실패가 호출자 계약 밖으로 새어 나간다. runtime은 unhandled rejection 경고나 global handler를 제공하지만 이를 정상 오류 처리로 사용하면 안 된다. 작업 소유자가 누구인지 정하고, 기다릴 필요가 없는 background task라도 실패를 수집해 log/metric으로 남긴다.
"""),
            depthCode("""
10,000개 요청을 8개씩 처리하는 개념

queue = URLs
workers = 8
각 worker:
  queue에서 하나 가져옴
  await fetch(url)
  결과 기록
  다음 항목

한 요청이 끝나야 그 worker가 다음 것을 시작한다.
동시 요청 수의 상한이 8로 유지된다.
"""),
            depthBullets(
                "비동기 API를 설계할 때 성공값뿐 아니라 cancellation ownership과 partial failure를 정의한다.",
                "동시성 수는 “클수록 빠름”이 아니라 connection·memory·rate limit으로 측정한다.",
                "background Promise는 실패가 관측되는 경로가 반드시 있어야 한다."
            )
        ),
        expertPack(
            sectionId = "V1-C05-S04",
            depthTitle("TypeScript를 타입 이름 붙이기에서 “상태 불가능성 제거와 runtime 경계 검증”으로 올린다"),
            depthHeading("discriminated union으로 불가능한 상태를 타입에서 없앤다"),
            depthParagraph("""
loading:boolean, error?:string, data?:User를 따로 두면 loading=true인데 data도 있고 error도 있는 모순 상태가 가능하다. 대신 {status:\"loading\"} | {status:\"error\", error:...} | {status:\"success\", data:...}처럼 상태별 필드를 묶으면 허용 조합이 타입에 드러난다. switch에서 never를 이용한 exhaustive check를 두면 새 상태 추가 시 처리 누락을 compiler가 알려 줄 수 있다.
"""),
            depthHeading("1. type guard는 “내가 맞다고 주장”하는 함수이므로 검증 품질이 중요하다"),
            depthParagraph("""
function isUser(x): x is User 같은 signature는 compiler에게 강한 정보를 준다. 하지만 내부 검사가 name 하나만 보고 id/email을 확인하지 않으면 타입 시스템이 잘못된 믿음을 갖게 된다. 외부 JSON에는 ad-hoc guard보다 schema validator를 사용하고, validator에서 추론한 타입을 재사용하면 runtime contract와 static type의 중복을 줄일 수 있다.
"""),
            depthHeading("2. branded type으로 같은 primitive의 의미를 분리한다"),
            depthParagraph("""
UserId와 OrderId가 둘 다 string이면 실수로 서로 바꿔 전달해도 compiler가 모를 수 있다. brand/tag를 붙인 타입으로 의미를 구분하면 parse/validation을 통과한 값만 특정 함수에 들어가게 만들 수 있다. runtime에는 같은 string일 수 있지만 compile-time에서 도메인 오류를 줄이는 방법이다.
"""),
            depthHeading("3. generic constraint는 관계를 제한한다"),
            depthParagraph("""
generic T를 아무 값이나 받게 두는 것과 T extends {id:string}처럼 필요한 능력을 요구하는 것은 다르다. generic은 “아무 타입이나”가 아니라 입력과 출력 사이 관계를 유지하면서 필요한 제약을 표현하는 도구다. 과도한 generic은 읽기 어렵게 만들므로 실제로 여러 타입에서 같은 관계를 재사용할 때 쓴다.
"""),
            depthHeading("4. Result 타입은 예상 가능한 실패를 값으로 표현할 수 있다"),
            depthParagraph("""
예외가 아니라 {ok:true,value:T}|{ok:false,error:E}처럼 성공/실패를 return하면 caller가 실패 처리를 빼먹기 어렵게 만들 수 있다. 모든 오류를 Result로 바꿀 필요는 없지만 validation, parse처럼 자주 예상되는 실패는 control flow에 명시적으로 나타낼 수 있다.
"""),
            depthHeading("5. race condition과 타입 안전성은 다른 축이다"),
            depthParagraph("""
완벽하게 typed된 두 request도 완료 순서가 뒤집히면 오래된 응답이 최신 state를 덮을 수 있다. request version, AbortController, state machine으로 “이 응답이 아직 유효한가”를 runtime에서 확인해야 한다. static type은 시간 순서와 외부 시스템의 진실성을 보장하지 않는다.
"""),
            depthCode("""
type RemoteState<T> =
  | { status: \"idle\" }
  | { status: \"loading\"; requestId: string }
  | { status: \"success\"; data: T }
  | { status: \"error\"; message: string }

// loading + data + error 같은 모순 조합을
// 아예 표현하기 어렵게 만든다.
""", "typescript"),
            depthBullets(
                "외부 JSON은 TypeScript interface 선언만으로 신뢰하지 않는다.",
                "상태 모델은 boolean 여러 개보다 허용 가능한 조합 자체를 type으로 표현한다.",
                "type guard와 schema validator가 실제 필드를 충분히 검사하는지 실패 케이스로 테스트한다."
            )
        )
    )
}
