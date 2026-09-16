package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack04 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C04-S01",
            depthTitle("HTML 다음 단계: DOM만 보지 말고 접근성 트리·focus·form contract까지 본다"),
            depthParagraph("""
V3에서 semantic tag, form, link와 button을 이미 배웠다면 이제 “브라우저가 같은 HTML을 여러 관점의 tree로 해석한다”는 사실을 배운다. 화면에 보이는 DOM 구조만 맞아도 키보드와 screen reader에서 기능이 망가질 수 있다. 브라우저는 DOM과 style 정보를 바탕으로 accessibility tree를 만들고 보조기술은 role, name, state를 이용해 UI를 이해한다. 따라서 접근성은 CSS 장식이 끝난 뒤 추가하는 설명문이 아니라 요소 선택과 상태 모델에서 시작한다.
"""),
            depthHeading("1. accessible name은 화면 글자와 항상 같지 않다"),
            depthParagraph("""
button 안의 텍스트, label, aria-label, aria-labelledby 등 여러 경로가 accessible name을 만든다. 아이콘만 있는 버튼에 시각적 화살표는 보여도 이름이 없으면 screen reader 사용자는 기능을 알기 어렵다. 반대로 aria-label을 무분별하게 붙이면 화면 텍스트와 다른 이름이 생겨 음성 명령 사용자가 보이는 글자로 버튼을 찾지 못할 수 있다. native semantic을 우선하고 ARIA는 부족한 부분을 보완한다.
"""),
            depthHeading("2. focus order는 DOM 순서와 설계 문제다"),
            depthParagraph("""
키보드 Tab 이동은 보통 document 순서와 focus 가능 요소를 따른다. CSS로 화면 위치만 크게 바꾸면 눈으로 보는 순서와 keyboard focus 순서가 달라질 수 있다. tabindex 양수로 순서를 억지 지정하면 유지보수가 더 어려워진다. modal을 열 때 focus를 modal 안으로 이동하고 닫을 때 원래 trigger로 돌려주는 것처럼 focus도 상태 전이로 설계한다.
"""),
            depthHeading("3. form validation은 client와 server가 역할을 나눈다"),
            depthParagraph("""
브라우저의 required, type=email 같은 validation은 빠른 사용자 피드백에 좋지만 request를 직접 만드는 client나 조작된 요청은 우회할 수 있다. server는 같은 비즈니스 규칙을 독립적으로 검증해야 한다. 오류가 발생하면 단순 빨간색만 표시하지 말고 어떤 field가 왜 실패했는지 text와 programmatic relation으로 전달해야 한다.
"""),
            depthHeading("4. progressive enhancement는 “JS가 없으면 아무것도 안 됨”을 줄인다"),
            depthParagraph("""
가능한 기능은 HTML 기본 동작으로 먼저 만들고 JavaScript로 경험을 향상시키면 느린 네트워크, script 실패, 접근성 도구에서도 핵심 기능이 유지될 수 있다. 모든 앱이 server-rendered form이어야 한다는 뜻은 아니지만, navigation·submit·download처럼 브라우저가 원래 제공하는 기능을 불필요하게 다시 구현하지 않는 습관이 중요하다.
"""),
            depthHeading("5. DOM validity와 security context"),
            depthParagraph("""
브라우저가 잘못된 태그를 어느 정도 복구해 주기 때문에 화면이 보인다고 markup이 의도대로 해석됐다고 단정하면 안 된다. 특히 table, form 중첩처럼 parser 규칙이 복잡한 구조는 DOM이 source와 달라질 수 있다. DevTools에서 실제 DOM을 확인하고, 사용자 HTML을 삽입할 때는 parser 복구가 XSS 공격 payload와 상호작용할 수 있으므로 sanitizer를 사용한다.
"""),
            depthCode("""
아이콘 버튼을 검사한다

시각 화면: [🔍]
DOM: <button><svg ...></svg></button>
접근성 이름: 없음  -> 기능을 말로 알 수 없음

개선
<button aria-label="검색">...</button>

그리고 키보드 Tab으로 도달 가능한지,
Enter/Space로 실행되는지, focus 표시가 보이는지도 확인한다.
"""),
            depthBullets(
                "마우스로 클릭 가능하다는 것만으로 상호작용 요소 검증을 끝내지 않는다.",
                "접근성 이름·role·state와 keyboard focus를 실제 도구로 검사한다.",
                "native element로 해결되는 기능을 div+click+ARIA로 다시 만들지 않는다."
            )
        ),
        expertPack(
            sectionId = "V1-C04-S02",
            depthTitle("CSS를 꾸미기 문법에서 “계산 규칙·레이아웃 비용·반응형 제약”으로 올린다"),
            depthHeading("cascade를 이기기 위해 specificity를 키우기보다 구조를 관리한다"),
            depthParagraph("""
큰 코드베이스에서 #app .page .card button 같은 selector와 !important가 늘어나면 새 component가 기존 규칙을 예측하기 어려워진다. CSS layer, 낮은 specificity convention, component boundary, custom property를 이용하면 “누가 이기는가”를 설계할 수 있다. 개발자 도구의 Computed/Styles에서 취소선이 된 선언과 최종 source를 확인하는 습관이 우선이다.
"""),
            depthHeading("1. stacking context 때문에 z-index 999999가 안 먹을 수 있다"),
            depthParagraph("""
z-index는 모든 요소가 하나의 전역 숫자로 경쟁하는 것이 아니다. position+z-index, transform, opacity 등은 새 stacking context를 만들 수 있고, 자식의 큰 z-index도 부모 context 바깥 형제보다 위로 못 올라갈 수 있다. modal이 header 뒤에 숨어 “더 큰 z-index”만 계속 올리는 문제는 stacking context tree를 먼저 봐야 한다.
"""),
            depthHeading("2. layout thrashing은 읽기와 쓰기를 섞어서 생긴다"),
            depthParagraph("""
JavaScript가 style을 바꾼 직후 offsetWidth 같은 layout 값을 읽으면 브라우저가 미뤄 두었던 layout 계산을 즉시 수행해야 할 수 있다. 반복문에서 read→write→read→write를 수백 번 섞으면 forced synchronous layout이 반복된다. DOM read를 모아서 하고 write를 묶거나 requestAnimationFrame을 활용해 rendering pipeline과 맞추면 성능을 개선할 수 있다.
"""),
            depthHeading("3. responsive는 breakpoint 목록이 아니라 constraint 설계다"),
            depthParagraph("""
고정 360/768/1024만 맞추면 중간 폭, split-screen, 큰 글꼴에서 깨질 수 있다. min(), max(), clamp(), flex/grid의 intrinsic sizing, max-width를 이용해 “가능한 범위”를 정의하면 화면이 연속적으로 적응한다. component가 놓인 부모 폭에 따라 바뀌어야 하면 viewport media query보다 container query가 더 자연스러운 경우도 있다.
"""),
            depthHeading("4. line length와 font metric이 실제 페이지 수를 바꾼다"),
            depthParagraph("""
같은 16px 글자도 font의 x-height, glyph 폭, fallback font에 따라 읽는 밀도가 다르다. 웹폰트가 늦게 로드되면 fallback과 크기가 달라 layout shift가 생길 수 있다. 학습 앱에서는 한 줄 길이, line-height, paragraph spacing을 고정된 미감이 아니라 읽기 속도와 재탐색 비용 관점에서 본다.
"""),
            depthHeading("5. CSS containment와 content-visibility는 큰 화면의 범위를 줄인다"),
            depthParagraph("""
아주 긴 문서나 복잡한 dashboard에서는 변경 하나가 전체 layout에 영향을 주지 않도록 containment를 사용하거나 화면 밖 content rendering을 늦출 수 있다. 이런 최적화는 무조건 붙이는 마법이 아니라 접근성·검색·측정 API에 미치는 영향을 확인하면서 큰 병목이 실제로 있을 때 적용한다.
"""),
            depthCode("""
z-index 버그 추적

body
 ├─ header (position, z-index: 10)
 └─ app (transform: translateZ(0))  <- 새 stacking context
      └─ modal (z-index: 999999)

modal의 999999는 app context 안에서만 경쟁한다.
header와 app context의 순서를 먼저 해결해야 한다.
"""),
            depthBullets(
                "DevTools에서 computed style과 stacking context를 확인한 뒤 수정한다.",
                "반응형 검증은 특정 기기 3개가 아니라 연속 폭·글꼴 확대·가로모드까지 본다.",
                "성능 최적화는 Performance trace에서 layout/paint 비용이 실제 병목인지 확인한다."
            )
        ),
        expertPack(
            sectionId = "V1-C04-S03",
            depthTitle("DOM 조작 다음 단계: event path·rendering pipeline·observer·state 일관성"),
            depthParagraph("""
V3에서 DOM, event, bubbling, DevTools를 다뤘다면 이제 한 화면에서 상태가 어떻게 흐르는지 본다. 사용자가 클릭하면 event dispatch가 일어나고 handler가 state를 바꾸며, 브라우저는 style/layout/paint/composite 과정을 거쳐 다음 frame에 결과를 보여 준다. “DOM을 바꿨으니 즉시 화면 픽셀도 바뀌었다”는 단순 모델을 버리면 animation, measurement, 성능 버그를 이해하기 쉬워진다.
"""),
            depthHeading("1. event composed path와 shadow DOM"),
            depthParagraph("""
일반 DOM에서는 target이 부모로 bubble하지만 web component의 shadow DOM 경계를 넘을 때 event target이 retarget될 수 있다. composedPath()를 보면 실제 전달 경로를 확인할 수 있다. 외부 component가 내부 구현 element에 강하게 의존하지 않도록 event contract를 설계해야 한다.
"""),
            depthHeading("2. passive listener와 scroll 성능"),
            depthParagraph("""
touchmove/wheel handler가 preventDefault를 호출할 가능성이 있으면 browser가 handler 결과를 기다리느라 scrolling을 늦출 수 있다. passive listener는 “이 handler는 기본 scroll을 막지 않는다”는 약속을 줘 browser가 더 일찍 움직일 수 있게 한다. 무조건 passive=true가 아니라 실제로 preventDefault가 필요한 gesture인지 판단한다.
"""),
            depthHeading("3. ResizeObserver와 IntersectionObserver는 polling을 줄인다"),
            depthParagraph("""
element 크기 변화를 매 frame getBoundingClientRect로 확인하거나, scroll마다 모든 element 위치를 계산하면 불필요한 작업이 커진다. ResizeObserver는 size change, IntersectionObserver는 viewport/ancestor와의 교차 변화를 비동기로 알려 준다. lazy loading, infinite list, responsive component에 유용하지만 callback에서 다시 무거운 layout 작업을 만들지 않아야 한다.
"""),
            depthHeading("4. 상태의 single source of truth"),
            depthParagraph("""
같은 “선택된 탭”을 DOM class, 전역 변수, URL query 세 곳에서 각각 수정하면 어느 것이 진짜인지 충돌한다. authoritative state를 하나 정하고 나머지는 그 상태에서 파생하거나 명시적 동기화 규칙을 둔다. declarative UI framework가 state→UI 단방향 흐름을 선호하는 이유도 변경 경로를 줄이기 위해서다.
"""),
            depthHeading("5. optimistic UI에는 rollback 규칙이 필요하다"),
            depthParagraph("""
좋아요 버튼을 누르자마자 화면 숫자를 +1하면 빠르게 느껴지지만 server 요청이 실패할 수 있다. 낙관적 업데이트를 한다면 request id, 이전 상태, 실패 시 rollback 또는 reconcile 정책이 필요하다. 여러 요청이 겹치면 단순 “실패하면 -1”이 최신 상태를 망칠 수 있어 mutation ordering까지 고려한다.
"""),
            depthCode("""
좋아요 optimistic update

현재 count=10, version=7
click #A -> UI 11, request(version 7)
곧 click #B -> UI 12, request(version 8 예상)

#A 실패, #B 성공 같은 순서가 가능하다.
“실패했으니 count--”만 하면 server와 어긋날 수 있다.
server state/version을 다시 reconcile하는 전략이 필요하다.
"""),
            depthBullets(
                "DOM class와 application state를 서로 독립 진실로 두지 않는다.",
                "scroll/resize를 직접 polling하기 전에 observer API가 목적에 맞는지 확인한다.",
                "optimistic update는 성공 경로만 아니라 실패·중복·순서 뒤집힘까지 설계한다."
            )
        )
    )
}
