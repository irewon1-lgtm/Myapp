package com.futuretech.poweruser.textbook

internal object V4BookDepthTrack04To07 {
    val packs: List<BookDepthPack> = listOf(
        depthPack(
            sectionId = "V1-C04-S01",
            topics = listOf(
                depthTopic("HTML은 화면 모양보다 의미를 먼저 적는 언어다", "브라우저는 h1, button, nav, form 같은 요소 이름을 통해 콘텐츠 역할을 이해한다. div만으로도 비슷한 모양을 만들 수 있지만 의미가 사라지면 키보드 조작, 화면 읽기 도구, 검색엔진, 유지보수가 어려워진다. semantic HTML은 디자인을 위한 장식이 아니라 문서 구조를 설명하는 계약이다."),
                depthTopic("브라우저가 HTML 문자열을 바로 화면으로 그리는 것은 아니다", "브라우저는 HTML을 읽어 DOM tree를 만들고 CSS를 적용해 어떤 요소가 어떤 크기와 위치를 가질지 계산한 뒤 화면에 그린다. 그래서 HTML 태그 하나를 바꿔도 DOM 구조, 스타일 계산, 레이아웃, 페인트 단계에 영향을 줄 수 있다. 개발자 도구에서 Elements 패널을 보면 이 중 DOM 구조를 직접 확인할 수 있다."),
                depthTopic("form은 입력칸 묶음 이상의 의미가 있다", "회원가입처럼 여러 입력값을 서버에 제출하는 흐름에서는 label, input, button, validation이 함께 움직인다. label을 입력칸과 연결하면 화면을 보지 못하는 사용자도 어떤 값을 넣어야 하는지 알 수 있고 클릭 영역도 넓어진다. 브라우저 기본 validation은 첫 방어선이지만 서버 validation을 대신할 수는 없다."),
                depthTopic("접근성은 나중에 덧붙이는 옵션이 아니다", "키보드만 사용하는 사람, 화면 읽기 도구를 쓰는 사람, 작은 화면이나 느린 네트워크를 사용하는 사람도 같은 기능에 접근할 수 있어야 한다. 버튼처럼 행동하는 요소를 실제 button으로 만들고 이미지에 의미 있는 대체 텍스트를 제공하는 기본 습관만으로도 많은 문제를 예방할 수 있다."),
                depthTopic("link와 button을 구분해야 하는 이유", "다른 주소로 이동하는 행동은 link가 자연스럽고 현재 화면의 상태를 바꾸거나 작업을 실행하는 행동은 button이 자연스럽다. 모양은 CSS로 비슷하게 만들 수 있지만 의미와 키보드 동작은 다르다. 역할에 맞는 요소를 고르면 추가 JavaScript를 덜 쓰고도 올바른 기본 동작을 얻는다.")
            ),
            workedExample = """
                요구사항: 로그인 화면

                구조
                form
                  label "이메일"
                  input type=email
                  label "비밀번호"
                  input type=password
                  button type=submit "로그인"

                브라우저
                HTML 읽기 → DOM 생성 → CSS 적용 → 화면 표시

                사용자가 Enter 입력
                form submit 이벤트 발생
                입력값 검증 후 서버 요청
            """,
            mistakes = listOf(
                "모든 요소를 div와 span으로 만들고 클릭 이벤트만 붙인다.",
                "placeholder만 넣고 label을 생략한다.",
                "이미지마다 의미 없는 파일명을 alt에 그대로 넣는다.",
                "페이지 이동과 작업 실행을 모두 같은 요소로 처리한다.",
                "브라우저 validation이 있으니 서버 검증은 필요 없다고 생각한다."
            ),
            questions = listOf(
                "DOM tree와 HTML 원문은 언제 달라질 수 있을까?",
                "ARIA는 semantic HTML을 언제 보완해야 할까?",
                "브라우저가 잘못 닫힌 HTML 태그를 어떻게 복구할까?",
                "form submit과 JavaScript fetch는 어떤 상황에서 함께 쓰일까?"
            )
        ),
        depthPack(
            sectionId = "V1-C04-S02",
            topics = listOf(
                depthTopic("CSS cascade는 왜 이름 그대로 ‘겹쳐서 결정’되는가", "하나의 요소에 여러 CSS 규칙이 동시에 적용될 수 있다. 브라우저는 출처, 중요도, specificity, 선언 순서를 기준으로 최종 값을 고른다. 스타일이 안 먹을 때 무조건 !important를 붙이기보다 어떤 규칙이 이겼는지 개발자 도구에서 확인하는 습관이 중요하다."),
                depthTopic("box model을 모르면 크기 계산이 계속 어긋난다", "요소 크기는 content만 있는 것이 아니라 padding, border, margin과 함께 생각해야 한다. 기본 box-sizing에서는 width를 줘도 padding과 border가 바깥에 더해져 실제 폭이 커질 수 있다. 많은 프로젝트가 border-box를 기본으로 쓰는 이유는 지정한 width 안에 padding과 border를 포함시켜 계산을 단순하게 만들기 때문이다."),
                depthTopic("Flexbox와 Grid는 경쟁 기술이 아니다", "Flexbox는 한 축을 중심으로 항목을 배치하는 데 강하고 Grid는 행과 열 두 축의 전체 구조를 만들기 좋다. 카드 내부 버튼 정렬은 Flexbox, 페이지의 여러 열 구조는 Grid가 자연스러울 수 있다. 실제 화면에서는 둘을 중첩해서 사용하는 경우가 많다."),
                depthTopic("반응형 디자인은 화면 폭 숫자 외우기가 아니다", "모바일과 태블릿, 데스크톱은 단순히 폭만 다르지 않고 입력 방식, 읽는 거리, 사용 맥락도 다르다. 콘텐츠가 깨지는 지점에서 layout이 자연스럽게 바뀌도록 만들고, 고정 px 대신 유연한 크기와 max-width를 조합하면 특정 기기 목록에 덜 의존할 수 있다."),
                depthTopic("가독성은 font-size 하나로 결정되지 않는다", "본문 폭이 너무 넓으면 다음 줄을 찾기 어렵고 line-height가 너무 좁으면 줄끼리 붙어 보인다. 글자 크기, 줄 높이, 문단 간격, 대비, 한 줄 길이를 함께 조정해야 한다. 코드 학습 화면에서는 일반 본문과 code font 역할도 시각적으로 분리해야 한다.")
            ),
            workedExample = """
                카드 목록 화면

                전체 layout: CSS Grid
                grid-template-columns: repeat(auto-fit, minmax(260px, 1fr))

                카드 내부: Flexbox
                display: flex
                flex-direction: column

                버튼 영역
                margin-top: auto

                작은 화면에서는 1열
                공간이 생기면 자동으로 2열, 3열
            """,
            mistakes = listOf(
                "스타일 충돌을 이해하지 못하고 !important를 계속 추가한다.",
                "width만 보고 padding과 border를 계산에서 빼먹는다.",
                "Flexbox와 Grid 중 하나만 써야 한다고 생각한다.",
                "모든 위치를 fixed pixel과 absolute positioning으로 맞춘다.",
                "텍스트 한 줄 폭이 너무 길어져도 화면이 넓으니 괜찮다고 생각한다."
            ),
            questions = listOf(
                "specificity를 지나치게 높이면 유지보수가 왜 어려워질까?",
                "min-width와 max-width는 반응형 layout에서 어떻게 다르게 쓰일까?",
                "CSS custom property는 theme 관리에 어떻게 도움이 될까?",
                "layout shift는 사용 경험과 성능에 어떤 문제를 만들까?"
            )
        ),
        depthPack(
            sectionId = "V1-C04-S03",
            topics = listOf(
                depthTopic("DOM을 수정한다는 것은 HTML 파일을 다시 쓰는 일이 아니다", "페이지가 열린 뒤 JavaScript는 메모리에 만들어진 DOM 객체를 찾고 속성이나 자식 구조를 바꿀 수 있다. 화면이 바뀌어도 서버의 원본 HTML 파일이 자동으로 수정되는 것은 아니다. 새로고침하면 서버나 앱이 다시 만든 초기 상태로 돌아갈 수 있다."),
                depthTopic("event 객체에는 사용자의 행동 증거가 들어 있다", "클릭, 키 입력, submit 같은 event에는 어떤 요소에서 시작됐는지, 어떤 키가 눌렸는지, 기본 동작을 막았는지 같은 정보가 있다. handler가 전역 상태만 보는 대신 event의 실제 target과 현재 상태를 함께 확인하면 디버깅이 쉬워진다."),
                depthTopic("event bubbling은 왜 존재하는가", "자식 요소에서 발생한 event가 부모 방향으로 전달되면 부모 하나에 handler를 두고 여러 자식의 이벤트를 처리하는 event delegation이 가능하다. 동적으로 생기는 목록 항목마다 listener를 붙이지 않아도 돼 효율적이지만, 원치 않는 부모 동작까지 발생할 수 있어 target과 currentTarget 차이를 이해해야 한다."),
                depthTopic("DevTools는 결과를 보는 도구가 아니라 가설을 검증하는 도구다", "Elements에서 DOM과 CSS, Console에서 값과 오류, Network에서 요청·응답, Sources에서 breakpoint를 확인할 수 있다. 화면이 이상할 때 코드를 감으로 바꾸기보다 ‘DOM이 틀렸나, CSS가 덮였나, 데이터가 안 왔나’라는 가설을 패널별로 확인한다."),
                depthTopic("화면 상태와 DOM 상태를 직접 섞으면 복잡해질 수 있다", "작은 앱에서는 DOM을 직접 바꾸기 쉽지만 기능이 늘면 어떤 코드가 어떤 화면을 바꿨는지 추적하기 어려워진다. React 같은 UI 라이브러리가 state를 기준으로 화면을 다시 계산하는 방식을 쓰는 이유도 상태와 화면 변경 규칙을 더 예측 가능하게 만들기 위해서다.")
            ),
            workedExample = """
                할 일 목록
                ul#todos 에 click listener 하나 등록

                사용자가 세 번째 삭제 버튼 클릭
                event.target → 실제 삭제 버튼
                event가 부모 ul까지 bubble
                부모 handler가 target의 todo id 확인
                해당 데이터 삭제
                DOM에서 해당 row 제거

                새로 추가된 todo 버튼에도 같은 부모 handler가 동작
            """,
            mistakes = listOf(
                "DOM 변경이 서버 파일까지 영구 저장된다고 생각한다.",
                "event.target과 currentTarget을 구분하지 않는다.",
                "동적으로 생기는 요소마다 불필요하게 listener를 계속 추가한다.",
                "화면 오류가 나면 Network나 Console을 보지 않고 CSS만 수정한다.",
                "DOM과 데이터 상태를 서로 다른 곳에서 중복 관리한다."
            ),
            questions = listOf(
                "capture 단계와 bubble 단계는 어떤 순서일까?",
                "preventDefault와 stopPropagation은 목적이 어떻게 다를까?",
                "MutationObserver는 언제 필요할까?",
                "virtual DOM이나 declarative UI가 직접 DOM 조작과 무엇이 다를까?"
            )
        ),

        depthPack(
            sectionId = "V1-C05-S01",
            topics = listOf(
                depthTopic("primitive와 object를 다르게 느껴야 하는 이유", "숫자나 boolean 같은 primitive 값은 대입할 때 값 자체를 다루는 느낌이 강하지만 object는 여러 변수가 같은 객체를 가리킬 수 있다. 한쪽에서 객체 속성을 바꾸면 다른 변수로 봐도 바뀐 상태가 보일 수 있다. 예상치 못한 공유 변경을 막기 위해 복사와 불변성 개념이 중요해진다."),
                depthTopic("함수도 값이라는 말이 실제로 무엇을 가능하게 하는가", "JavaScript에서는 함수를 변수에 넣고 다른 함수의 argument로 전달하고 return할 수 있다. 그래서 버튼 클릭 뒤 실행할 함수, 배열 각 항목에 적용할 변환 함수, 비동기 작업 완료 뒤 실행할 callback을 데이터처럼 전달할 수 있다."),
                depthTopic("scope와 closure는 시간을 건너 상태를 기억하게 한다", "함수가 만들어진 위치의 바깥 변수를 나중에도 접근할 수 있는 동작이 closure다. 버튼 handler가 생성 당시의 id를 기억하거나 private 상태를 숨기는 패턴에 쓰인다. 하지만 큰 객체를 필요 이상으로 붙잡으면 메모리 해제가 늦어질 수 있다."),
                depthTopic("this는 변수 이름처럼 고정된 값이 아니다", "JavaScript의 this는 함수가 어떻게 호출됐는지에 따라 달라질 수 있다. 객체 method 호출, 일반 함수 호출, arrow function에서 규칙이 다르다. 초보 단계에서는 this를 마법처럼 외우기보다 가능하면 명시적인 parameter와 lexical 변수로 흐름을 단순하게 유지하는 편이 좋다."),
                depthTopic("prototype은 객체가 기능을 공유하는 한 방식이다", "객체에 원하는 속성이 없을 때 연결된 prototype 쪽에서 찾을 수 있다. class 문법을 써도 내부적으로 prototype 기반 동작과 연결된다. 모든 세부를 외울 필요는 없지만 객체마다 같은 method를 통째로 복제하지 않고 기능을 공유할 수 있다는 그림은 알아 두면 좋다.")
            ),
            workedExample = """
                function makeCounter() {
                  let count = 0
                  return function () {
                    count = count + 1
                    return count
                  }
                }

                counter = makeCounter()
                counter() → 1
                counter() → 2

                makeCounter 실행은 끝났지만 반환된 함수가 count를 기억함
                이것이 closure의 핵심 그림
            """,
            mistakes = listOf(
                "object를 다른 변수에 대입하면 완전히 독립 복사된다고 생각한다.",
                "callback 함수가 실제로 언제 실행되는지 확인하지 않는다.",
                "closure를 단순히 함수 안의 함수라고만 외운다.",
                "this가 항상 객체 자신을 가리킨다고 생각한다.",
                "class를 쓰면 JavaScript prototype과 완전히 무관하다고 생각한다."
            ),
            questions = listOf(
                "shallow copy와 deep copy는 언제 차이가 날까?",
                "const object의 속성은 왜 바꿀 수 있을까?",
                "garbage collector는 closure가 붙잡은 값을 언제 해제할까?",
                "prototype chain이 너무 길면 어떤 문제가 생길까?"
            )
        ),
        depthPack(
            sectionId = "V1-C05-S02",
            topics = listOf(
                depthTopic("map·filter·reduce를 문법이 아니라 데이터 흐름으로 본다", "map은 각 항목을 다른 값으로 바꾸고, filter는 조건에 맞는 항목만 남기고, reduce는 여러 값을 하나의 결과로 누적한다. 어떤 함수를 써야 할지 헷갈리면 ‘개수 유지하며 변환, 개수 줄여 선택, 하나로 접기’라는 흐름부터 본다."),
                depthTopic("동기 코드가 오래 걸리면 화면이 멈출 수 있다", "JavaScript의 한 실행 흐름을 오래 붙잡는 계산이 있으면 그동안 클릭 처리나 화면 갱신 같은 다른 작업이 기다릴 수 있다. 네트워크처럼 시간이 걸리는 일은 완료를 기다리는 동안 다른 일을 할 수 있게 비동기 API와 callback, Promise가 필요해진다."),
                depthTopic("callback이 나쁜 게 아니라 중첩과 오류 흐름이 어려운 것이다", "callback 자체는 이벤트와 비동기 처리의 기본 도구다. 문제가 되는 것은 여러 비동기 작업을 깊게 중첩하면서 성공과 실패 흐름이 흩어지는 경우다. Promise와 async/await는 이 흐름을 더 구조적으로 표현하기 위한 방법이다."),
                depthTopic("JSON 변환은 객체를 그대로 순간이동시키는 게 아니다", "JSON.stringify는 JavaScript 객체의 일부 정보를 문자열 형식으로 바꾸고 JSON.parse는 그 문자열에서 새 값을 만든다. 함수, undefined, prototype 정보 등은 그대로 보존되지 않는다. 네트워크 경계를 넘는 데이터는 명시적인 schema와 검증이 필요하다.")
            ),
            workedExample = """
                prices = [1000, 2500, 500, 4000]

                1) filter: 1000 이상만 남김
                   [1000, 2500, 4000]
                2) map: 10% 할인값으로 변환
                   [900, 2250, 3600]
                3) reduce: 합계 계산
                   6750

                각 단계의 입력과 출력 모양을 적으면 chain을 읽기 쉬움
            """,
            mistakes = listOf(
                "map을 쓰면서 원본 배열을 몰래 수정해 부작용을 만든다.",
                "filter callback에서 true/false가 아닌 의도를 불분명하게 반환한다.",
                "오래 걸리는 동기 loop가 UI thread를 막을 수 있다는 점을 무시한다.",
                "callback과 Promise를 서로 완전히 다른 비동기 세계라고 생각한다.",
                "JSON으로 변환하면 객체의 모든 정보가 그대로 보존된다고 생각한다."
            ),
            questions = listOf(
                "for loop와 map 중 어느 쪽이 더 읽기 쉬운지는 어떻게 판단할까?",
                "Web Worker는 긴 계산 문제를 어떻게 줄일까?",
                "JSON schema나 runtime validation은 왜 필요할까?",
                "callback을 Promise로 감싸는 과정은 어떻게 동작할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C05-S03",
            topics = listOf(
                depthTopic("Promise는 미래의 값 그 자체보다 상태를 다루는 약속이다", "Promise는 pending에서 시작해 fulfilled 또는 rejected로 한 번 결정된다. then은 성공 결과를 이어 받고 catch는 실패를 다룬다. 중요한 점은 Promise를 만든 순간 결과가 이미 있는 것이 아니라 완료 시점의 흐름을 연결한다는 것이다."),
                depthTopic("async/await가 비동기를 동기로 바꾸는 것은 아니다", "await는 해당 async 함수의 나머지 실행을 잠시 미루는 표현이다. JavaScript 전체가 멈추는 것이 아니어서 다른 event와 작업은 계속 처리될 수 있다. 코드가 위에서 아래로 읽히게 만들어도 네트워크 완료 순서와 경쟁 조건은 여전히 생각해야 한다."),
                depthTopic("event loop를 이해하면 실행 순서 퀴즈가 현실 버그로 보인다", "현재 call stack이 비어야 다음 task가 실행되고 Promise 후속 작업은 microtask queue에서 일반 task보다 먼저 처리되는 규칙이 있다. 이 순서를 모르면 setTimeout 0이 즉시 실행된다고 생각하거나, 상태 변경 순서를 잘못 예상할 수 있다."),
                depthTopic("비동기 오류는 호출한 줄에서 바로 터지지 않을 수 있다", "요청을 시작한 함수는 이미 끝났는데 나중에 response 처리 중 오류가 생길 수 있다. 그래서 Promise chain의 catch나 async 함수의 try/catch처럼 비동기 경로 안에서 실패를 수집해야 한다. 처리되지 않은 rejection은 사용자 화면이 멈추지 않아도 데이터 상태를 깨뜨릴 수 있다."),
                depthTopic("순차 실행과 병렬 시작을 의식적으로 선택한다", "서로 의존하지 않는 두 요청을 await A 다음 await B로 쓰면 A가 끝난 뒤 B를 시작해 시간이 합쳐질 수 있다. 둘을 먼저 시작한 뒤 Promise.all로 기다리면 동시에 진행할 수 있다. 반대로 두 번째 작업이 첫 결과에 의존하면 순차 실행이 맞다.")
            ),
            workedExample = """
                console.log("A")
                setTimeout(() => console.log("B"), 0)
                Promise.resolve().then(() => console.log("C"))
                console.log("D")

                먼저 현재 stack
                A
                D

                다음 microtask
                C

                다음 일반 task
                B

                결과: A D C B
            """,
            mistakes = listOf(
                "await를 쓰면 JavaScript 프로그램 전체가 멈춘다고 생각한다.",
                "setTimeout 0은 바로 다음 줄보다 먼저 실행된다고 생각한다.",
                "Promise rejection 처리를 빼먹고 성공 경로만 작성한다.",
                "독립 요청을 필요 없이 순차 await해 응답 시간을 늘린다.",
                "Promise.all에서 하나 실패하면 전체 결과가 어떻게 되는지 확인하지 않는다."
            ),
            questions = listOf(
                "Promise.allSettled는 Promise.all과 언제 다르게 쓰일까?",
                "microtask가 너무 많이 이어지면 일반 task가 늦어질 수 있을까?",
                "AbortController는 오래 걸리는 요청 취소에 어떻게 쓰일까?",
                "timeout은 Promise 표준 기능이 아니라면 어떻게 조합할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C05-S04",
            topics = listOf(
                depthTopic("race condition은 멀티스레드에서만 생기는 문제가 아니다", "JavaScript가 한 번에 한 JS 코드를 실행하더라도 여러 비동기 작업의 완료 순서는 달라질 수 있다. 검색어 A 요청 뒤 B 요청을 보냈는데 A가 늦게 도착하면 오래된 A 결과가 최신 B 화면을 덮을 수 있다. 요청 id 비교나 취소로 최신 결과만 반영해야 한다."),
                depthTopic("TypeScript는 runtime 검증을 대신하지 않는다", "TypeScript 타입은 개발 중 코드 관계를 검사하지만 네트워크에서 들어온 JSON이 실제로 그 타입인지 자동 보장하지 않는다. 외부 입력은 runtime schema 검증이 필요하다. 타입 선언만 믿으면 잘못된 서버 응답에서 앱이 실제 실행 중 깨질 수 있다."),
                depthTopic("unknown이 any보다 안전한 이유", "any는 타입 검사를 사실상 끄지만 unknown은 어떤 값인지 확인하기 전에는 함부로 사용할 수 없게 한다. 외부 JSON, catch error처럼 형태를 모르는 값은 unknown으로 받고 typeof나 schema 검사 뒤 구체 타입으로 좁히는 편이 안전하다."),
                depthTopic("generic은 타입을 복사하는 문법이 아니라 관계를 보존하는 도구다", "입력 타입과 출력 타입 사이 관계를 함수 하나로 표현하고 싶을 때 generic이 유용하다. 예를 들어 첫 항목을 돌려주는 함수는 string 배열이면 string, number 배열이면 number를 돌려줘야 한다. generic은 이 관계를 잃지 않게 한다."),
                depthTopic("취소와 timeout은 사용자 경험 문제이면서 상태 일관성 문제다", "사용자가 화면을 떠났는데 이전 요청이 늦게 완료돼 state를 바꾸면 보이지 않는 화면이나 새 화면을 오염시킬 수 있다. 요청 취소, component 생명주기 확인, timeout 정책을 두면 오래된 작업이 현재 상태를 덮는 문제를 줄일 수 있다.")
            ),
            workedExample = """
                검색창
                1) 사용자가 "ca" 입력 → request #1 시작
                2) 바로 "cat" 입력 → request #2 시작
                3) #2가 먼저 도착 → cat 결과 표시
                4) #1이 늦게 도착

                잘못된 코드: #1 결과로 화면 덮음
                안전한 코드: 현재 request id가 #2인지 확인하고 #1 무시 또는 취소
            """,
            mistakes = listOf(
                "비동기 완료 순서는 요청 시작 순서와 같다고 가정한다.",
                "TypeScript 타입 선언만 하면 서버 응답도 자동으로 검증된다고 생각한다.",
                "외부 데이터를 any로 받고 바로 속성에 접근한다.",
                "generic을 복잡한 문법으로만 보고 입력·출력 관계를 읽지 않는다.",
                "화면이 사라져도 진행 중 요청이 state를 바꿔도 괜찮다고 생각한다."
            ),
            questions = listOf(
                "discriminated union은 상태 모델링에 어떻게 도움이 될까?",
                "type guard가 잘못 구현되면 어떤 위험이 있을까?",
                "retry를 자동으로 할 때 idempotency가 왜 중요할까?",
                "여러 요청 중 가장 먼저 성공한 결과만 쓰려면 어떤 Promise 조합이 필요할까?"
            )
        ),

        depthPack(
            sectionId = "V1-C06-S01",
            topics = listOf(
                depthTopic("인터넷 요청은 한 번에 서버로 순간 이동하지 않는다", "기기는 먼저 네트워크에 연결되고 목적지 이름을 IP로 찾은 뒤 여러 router를 거쳐 packet을 보낸다. 각 단계에서 실패 원인이 다르다. Wi-Fi 아이콘이 떠 있어도 DNS가 실패하거나 외부 route가 끊기면 사이트에 접속하지 못할 수 있다."),
                depthTopic("IP와 port를 함께 봐야 하는 이유", "IP가 어느 기기나 네트워크 인터페이스로 갈지 알려 준다면 port는 그 기기 안의 어떤 network service와 통신할지 구분한다. 같은 서버 IP에서 443은 HTTPS, 다른 port는 별도 서비스가 들을 수 있다. firewall 규칙도 IP와 port 조합을 기준으로 제한하는 경우가 많다."),
                depthTopic("TCP가 신뢰성을 주는 대신 비용이 생기는 이유", "TCP는 순서, 재전송, 흐름 제어 같은 기능으로 누락되거나 순서가 뒤섞인 packet을 애플리케이션이 직접 처리하는 부담을 줄인다. 대신 연결 설정과 확인 과정, 손실 시 재전송 비용이 있다. UDP는 이런 보장을 줄이고 더 단순하게 datagram을 보내므로 실시간성 요구에서 선택될 수 있다."),
                depthTopic("latency와 bandwidth를 구분해야 성능 원인을 찾을 수 있다", "작은 API 한 건이 느리면 왕복 지연 latency가 문제일 수 있고 대용량 영상 전송이 느리면 bandwidth가 더 중요할 수 있다. 빠른 회선도 멀리 있는 서버와 여러 번 왕복해야 하면 반응이 느릴 수 있다. 요청 횟수를 줄이는 최적화가 효과적인 이유다."),
                depthTopic("NAT 때문에 내부 IP와 외부 IP가 다를 수 있다", "가정이나 회사 내부 기기는 private IP를 쓰고 router가 외부 통신을 public IP와 연결해 주는 경우가 많다. 외부 인터넷에서 내부 기기로 바로 들어오는 연결이 단순하지 않은 이유다. port forwarding, VPN 같은 기능은 이 경계를 다루는 방법과 관련된다.")
            ),
            workedExample = """
                https://example.com 접속
                1) DNS: example.com → 목적지 IP 확인
                2) route: packet이 여러 router를 거침
                3) TCP: server 443 port와 연결 준비
                4) TLS: 암호화 통신 준비
                5) HTTP request 전송
                6) response가 같은 연결을 통해 돌아옴

                실패 지점을 단계별로 나눠 진단
            """,
            mistakes = listOf(
                "Wi-Fi에 연결됐다는 사실을 인터넷 전체 정상과 같은 뜻으로 본다.",
                "IP와 domain을 같은 개념으로 생각한다.",
                "TCP와 HTTP를 같은 계층의 같은 종류 protocol로 생각한다.",
                "bandwidth가 높으면 모든 API latency도 즉시 낮아진다고 생각한다.",
                "private IP를 외부 인터넷에서도 그대로 접근 가능한 주소로 생각한다."
            ),
            questions = listOf(
                "IPv6에서는 NAT가 어떤 식으로 달라질까?",
                "packet loss가 TCP 응답 시간에 어떤 영향을 줄까?",
                "CDN이 사용자 가까이에 서버를 두면 어떤 latency가 줄어들까?",
                "QUIC은 TCP와 TLS의 일부 문제를 어떤 방향으로 바꾸려 할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C06-S02",
            topics = listOf(
                depthTopic("URL은 한 덩어리 문자열이 아니라 여러 역할의 조합이다", "scheme은 어떤 protocol 계열로 접근할지, host는 어느 서버인지, port는 어느 service인지, path는 어떤 자원이나 route인지, query는 추가 조건을 전달하는 데 쓰인다. URL을 부분별로 읽으면 request가 어디로 가고 무엇을 요구하는지 빠르게 파악할 수 있다."),
                depthTopic("TLS는 내용을 숨기는 것과 상대 확인을 함께 다룬다", "HTTPS 연결에서는 HTTP 내용을 보내기 전에 TLS가 암호화 channel을 준비하고 certificate를 이용해 접속한 domain과 상대 신원을 확인하는 데 도움을 준다. 암호화만 되고 상대가 누구인지 확인하지 못하면 공격자가 중간에 끼어도 알아차리기 어렵다."),
                depthTopic("HTTP status는 성공·실패 한 비트가 아니다", "2xx는 성공 계열, 3xx는 이동·cache 관련, 4xx는 client 요청 문제 계열, 5xx는 server 처리 실패 계열이라는 큰 분류가 있다. 404와 401과 500을 같은 오류로 다루면 사용자에게 잘못된 안내를 하거나 자동 retry를 위험하게 수행할 수 있다."),
                depthTopic("header와 body를 구분하면 API를 읽기 쉬워진다", "header는 content type, cache, authorization 같은 요청·응답의 메타정보를 담고 body는 실제 데이터 내용을 담는 경우가 많다. 인증 token을 body가 아니라 Authorization header에 두는 관례처럼 역할 분리가 protocol 설계를 읽는 기준이 된다."),
                depthTopic("GET과 POST만 외우면 REST를 이해한 것이 아니다", "method는 의도와 HTTP 동작 특성을 표현한다. 같은 요청을 반복해도 최종 상태가 중복되지 않는 idempotency 같은 성질은 retry 설계와 연결된다. 결제 같은 작업에서 네트워크 timeout 뒤 무작정 POST를 다시 보내면 중복 side effect가 생길 수 있다.")
            ),
            workedExample = """
                POST /orders HTTP/1.1
                Host: api.example.com
                Content-Type: application/json
                Authorization: Bearer ...

                { "productId": 10, "quantity": 2 }

                response
                HTTP/1.1 201 Created
                Content-Type: application/json

                { "orderId": 501 }

                method/path/header/body/status를 따로 읽는다
            """,
            mistakes = listOf(
                "HTTPS를 쓰면 서버의 모든 보안 문제가 자동 해결된다고 생각한다.",
                "401, 403, 404, 500을 모두 같은 실패 처리로 묶는다.",
                "header와 body 역할을 구분하지 않고 아무 정보나 넣는다.",
                "timeout 뒤 같은 생성 요청을 무조건 재전송한다.",
                "URL query에 password나 token 같은 민감정보를 습관적으로 넣는다."
            ),
            questions = listOf(
                "certificate chain은 브라우저가 무엇을 신뢰하게 할까?",
                "HTTP/2와 HTTP/3는 request 의미와 transport를 어떻게 바꿀까?",
                "redirect가 여러 번 이어지면 어떤 성능·보안 문제가 생길까?",
                "idempotency key는 결제 API에서 어떻게 쓰일까?"
            )
        ),
        depthPack(
            sectionId = "V1-C06-S03",
            topics = listOf(
                depthTopic("byte만 보고 데이터 종류를 항상 알 수 없는 이유", "같은 byte 배열도 어떤 규칙으로 해석하느냐에 따라 텍스트, 이미지, 압축 데이터 등 다른 의미를 가질 수 있다. MIME type은 sender가 이 body를 어떤 종류로 보냈는지 receiver에게 알려 주는 표준 이름이다. 확장자와 MIME이 충돌하면 보안과 처리 오류가 생길 수 있다."),
                depthTopic("Content-Type과 Accept는 방향이 다르다", "Content-Type은 현재 보내는 body가 무엇인지 말하고 Accept는 상대에게 어떤 response 형식을 받을 수 있거나 원하는지 알린다. request와 response 양쪽에 각각 Content-Type이 존재할 수 있다. 둘을 바꿔 쓰면 content negotiation을 이해하기 어렵다."),
                depthTopic("multipart는 왜 boundary가 필요한가", "한 HTTP body 안에 text field와 file처럼 여러 part를 넣으면 각 part가 어디서 시작하고 끝나는지 구분해야 한다. boundary 문자열이 그 경계를 표시한다. browser FormData를 쓸 때 Content-Type을 직접 고정하면 실제 boundary 값이 빠져 server parser가 실패할 수 있다."),
                depthTopic("파일 업로드는 이름만 검사하면 위험하다", "사용자가 photo.jpg라는 이름으로 실행 파일이나 예상하지 못한 형식을 올릴 수 있다. server는 MIME, magic byte, 크기, 저장 위치, 파일명 sanitization을 함께 고려해야 한다. 업로드 파일을 web root에 그대로 저장하면 실행이나 경로 공격 위험도 생길 수 있다.")
            ),
            workedExample = """
                multipart/form-data body 개념

                --boundary123
                Content-Disposition: form-data; name="title"

                profile
                --boundary123
                Content-Disposition: form-data; name="file"; filename="photo.jpg"
                Content-Type: image/jpeg

                ...binary bytes...
                --boundary123--

                boundary가 각 part를 나눈다
            """,
            mistakes = listOf(
                "Content-Type과 Accept를 같은 의미로 사용한다.",
                "FormData 사용 중 multipart Content-Type을 boundary 없이 직접 지정한다.",
                "파일 확장자만 보고 실제 형식과 안전성을 신뢰한다.",
                "업로드 파일 원본 이름을 그대로 서버 경로로 사용한다.",
                "파일 크기 제한 없이 메모리에 한 번에 모두 읽는다."
            ),
            questions = listOf(
                "streaming upload는 큰 파일 메모리 문제를 어떻게 줄일까?",
                "magic byte와 MIME type이 다르면 무엇을 믿어야 할까?",
                "presigned URL로 object storage에 직접 업로드하는 이유는 무엇일까?",
                "다운로드 response의 Content-Disposition은 어떤 역할을 할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C06-S04",
            topics = listOf(
                depthTopic("API는 함수 호출을 네트워크 경계까지 확장한 계약처럼 볼 수 있다", "client는 endpoint에 정해진 입력을 보내고 server는 정해진 출력과 오류를 돌려준다. 함수와 달리 network 실패, latency, version 차이, 인증이 추가되므로 계약을 더 명확히 문서화해야 한다. schema와 status code가 중요한 이유다."),
                depthTopic("authentication과 authorization을 섞으면 보안 구멍이 생긴다", "로그인해 사용자가 누구인지 확인했다고 모든 데이터에 접근할 수 있는 것은 아니다. authentication 뒤에 이 사용자가 이 주문, 파일, 관리자 기능에 접근할 권한이 있는지 authorization을 매 요청에서 확인해야 한다. URL id만 바꿔 다른 사용자 데이터를 보는 취약점이 여기서 생긴다."),
                depthTopic("cookie와 token은 저장 위치보다 전달·신뢰 모델을 본다", "cookie는 browser가 domain 규칙에 따라 자동 전송할 수 있고 HttpOnly, SameSite 같은 속성이 있다. Bearer token은 보통 Authorization header로 명시적으로 전달한다. 어떤 방식을 써도 노출된 credential은 공격자가 권한을 행사할 수 있으므로 저장과 전송 보호가 필요하다."),
                depthTopic("pagination은 UI 편의가 아니라 시스템 비용 제어다", "DB row 수천만 개를 한 번에 response로 만들면 query, memory, network, browser rendering 모두 부담이 커진다. limit/offset이나 cursor 기반 pagination으로 한 번에 처리하는 범위를 제한하면 latency와 자원 사용을 제어하기 쉽다."),
                depthTopic("API versioning은 변경 비용을 관리하는 문제다", "client가 이미 배포돼 있으면 server response field를 마음대로 없애기 어렵다. 호환 가능한 필드를 추가하거나 version을 분리하고 deprecation 기간을 두는 이유는 서로 다른 배포 시점의 client와 server가 함께 살아 있기 때문이다.")
            ),
            workedExample = """
                GET /users/42/orders/501

                1) token 확인 → 사용자가 42번인지 확인
                2) order 501 조회
                3) order.ownerId == currentUser.id 확인
                4) 권한 있으면 200 + data
                5) 권한 없으면 적절한 오류

                로그인 성공만 확인하고 3번을 빼면 다른 사용자 주문 접근 가능
            """,
            mistakes = listOf(
                "로그인된 사용자면 모든 object 접근을 허용한다.",
                "Bearer token을 URL query나 로그에 그대로 남긴다.",
                "목록 API에서 전체 데이터를 무제한 반환한다.",
                "client가 하나뿐이라고 생각하고 response 구조를 갑자기 깨뜨린다.",
                "REST를 무조건 특정 URL 모양 규칙만 지키는 것으로 이해한다."
            ),
            questions = listOf(
                "session id를 탈취당하면 어떤 보호가 필요할까?",
                "cursor pagination이 offset pagination보다 유리한 경우는 언제일까?",
                "OAuth와 OIDC는 어떤 문제를 해결하려고 만들어졌을까?",
                "API contract test는 client-server 변경을 어떻게 감지할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C06-S05",
            topics = listOf(
                depthTopic("CORS는 server 보안의 전부가 아니다", "CORS는 browser가 다른 origin의 response를 JavaScript에 노출할지 결정하는 정책이다. curl이나 다른 server는 CORS 제한을 그대로 받지 않는다. 그래서 중요한 API는 CORS 설정과 별개로 authentication, authorization, validation을 반드시 해야 한다."),
                depthTopic("cache는 빠르게 만드는 대신 오래된 값을 만들 수 있다", "browser, CDN, server, application 내부에 여러 cache가 존재할 수 있다. 같은 값을 복사해 두면 빠르지만 원본이 바뀐 뒤 언제 갱신할지 문제가 생긴다. TTL, ETag, invalidation 전략은 속도와 최신성 사이의 타협이다."),
                depthTopic("SSE와 WebSocket은 같은 실시간 통신이 아니다", "SSE는 주로 server에서 client로 event를 계속 보내는 단방향 흐름에 단순하고 HTTP 기반 재연결이 쉽다. WebSocket은 양방향 message가 자주 오가는 채팅, 협업 같은 상황에 맞을 수 있다. 실시간이라는 이유만으로 무조건 WebSocket을 선택할 필요는 없다."),
                depthTopic("Network 패널로 요청 하나를 끝까지 추적한다", "브라우저 DevTools에서 request URL, method, status, request/response headers, payload, timing을 보면 frontend와 server 사이 문제를 크게 줄일 수 있다. 화면에 데이터가 없을 때 request 자체가 안 나갔는지, 401인지, 500인지, response가 맞는데 rendering이 틀렸는지를 분리한다."),
                depthTopic("retry는 친절한 기능이지만 서버를 더 망가뜨릴 수도 있다", "server가 과부하로 timeout 중인데 모든 client가 즉시 여러 번 retry하면 traffic이 더 늘어난다. exponential backoff, jitter, 최대 횟수, retry 가능한 오류 분류가 필요하다. 쓰기 요청은 idempotency까지 함께 생각해야 한다.")
            ),
            workedExample = """
                화면에 사용자 목록이 안 보임

                Network 확인
                request 없음 → frontend event/조건 문제 가능
                401 → 인증 상태 확인
                403 → 권한 확인
                500 → server 로그 확인
                200 + 빈 배열 → query/조건 확인
                200 + 정상 data → frontend parsing/rendering 확인

                같은 증상도 단계별 원인이 다름
            """,
            mistakes = listOf(
                "CORS만 설정하면 API가 인증 없이도 안전하다고 생각한다.",
                "cache 값을 영원히 최신이라고 가정한다.",
                "실시간 기능이면 무조건 WebSocket부터 선택한다.",
                "Network 패널을 보지 않고 화면 코드만 수정한다.",
                "모든 network error를 즉시 무한 retry한다."
            ),
            questions = listOf(
                "cache stampede는 많은 key가 동시에 만료될 때 어떻게 생길까?",
                "ETag 기반 conditional request는 network 비용을 어떻게 줄일까?",
                "SSE 재연결 시 누락 event를 어떻게 보완할 수 있을까?",
                "circuit breaker는 반복 실패 호출을 어떻게 줄일까?"
            )
        ),

        depthPack(
            sectionId = "V1-C07-S01",
            topics = listOf(
                depthTopic("서버는 request가 도착했다고 바로 업무 로직을 실행하면 안 된다", "네트워크에서 온 모든 값은 신뢰 경계를 넘어온 외부 입력이다. path parameter, query, header, JSON body를 parsing한 뒤 타입, 범위, 필수값, 형식 등을 validation하고 나서 핵심 로직으로 넘겨야 한다. 잘못된 입력을 깊은 계층까지 보내면 오류 원인이 멀어지고 보안 위험도 커진다."),
                depthTopic("parsing과 validation은 서로 다른 실패다", "문자열 abc를 숫자로 읽지 못하는 것은 parsing 실패이고 나이 -10처럼 숫자로 읽을 수 있지만 업무 규칙상 허용되지 않는 것은 validation 실패다. 둘을 분리하면 사용자가 무엇을 고쳐야 하는지 더 정확하게 알려 줄 수 있다."),
                depthTopic("controller는 왜 얇게 유지하려고 하는가", "route/controller가 request 해석, DB query, 가격 계산, 이메일 발송을 모두 직접 하면 HTTP 세부와 업무 규칙이 뒤섞인다. controller는 입력을 받아 service에 넘기고 결과를 response로 바꾸는 역할에 집중하면 핵심 로직을 HTTP 없이도 테스트하기 쉽다."),
                depthTopic("에러 response도 API contract의 일부다", "성공 JSON만 문서화하고 실패 형식을 제각각 만들면 client가 모든 경우를 따로 처리해야 한다. error code, message, field errors, request id 같은 공통 구조를 정하면 사용자 안내와 server 로그 연결이 쉬워진다.")
            ),
            workedExample = """
                POST /users
                body: { "age": "abc", "email": "bad" }

                parsing
                age를 integer로 읽을 수 없음 → age format error

                다른 요청: { "age": -5, "email": "a@example.com" }
                parsing 성공
                validation
                age >= 0 규칙 실패 → business validation error

                둘을 같은 500 error로 보내면 원인을 잃음
            """,
            mistakes = listOf(
                "request body를 타입 선언만 믿고 실제 validation 없이 service에 넘긴다.",
                "잘못된 사용자 입력을 500 server error로 처리한다.",
                "controller에 DB와 업무 규칙을 모두 작성한다.",
                "오류 response 형식을 endpoint마다 제각각 만든다.",
                "로그에 password나 token까지 request body 전체를 남긴다."
            ),
            questions = listOf(
                "schema validation library는 어떤 중복을 줄여 줄까?",
                "400, 409, 422 같은 status를 어떻게 구분할까?",
                "request id를 client와 server log에 함께 남기면 무엇이 좋아질까?",
                "rate limit은 validation 전과 후 중 어디에서 하는 것이 좋을까?"
            )
        ),
        depthPack(
            sectionId = "V1-C07-S02",
            topics = listOf(
                depthTopic("service는 단순히 controller 코드를 옮겨 놓는 층이 아니다", "service에는 주문 가능 여부, 할인 규칙, 재고 차감 순서처럼 transport나 DB 구현과 독립적인 업무 규칙이 모여야 한다. HTTP가 아니라 batch job에서 같은 기능을 호출해도 규칙을 재사용할 수 있어야 책임 분리가 의미가 있다."),
                depthTopic("repository는 DB를 숨긴다는 말의 정확한 뜻", "핵심 로직이 SQL 문법이나 특정 ORM API를 직접 알지 않도록 데이터 조회·저장 인터페이스를 경계에 둔다. 그렇다고 모든 query를 똑같은 CRUD 함수로 감추라는 뜻은 아니다. 업무에 필요한 의미 있는 조회를 계약으로 표현해야 한다."),
                depthTopic("dependency injection은 테스트 가능성과 연결된다", "service 안에서 실제 DB client를 직접 new하면 테스트에서도 DB가 필요해진다. repository interface를 constructor로 전달받게 하면 production에서는 실제 구현을, test에서는 메모리 fake를 넣을 수 있다. 핵심은 framework가 아니라 의존성을 밖에서 주입하는 방향이다."),
                depthTopic("계층을 많이 만든다고 좋은 architecture가 되는 것은 아니다", "작은 앱에 controller-service-usecase-domain-repository를 무조건 다 만들면 파일 이동만 늘고 역할 차이가 모호해질 수 있다. 변경 이유와 테스트 경계를 기준으로 필요한 계층만 두고, 같은 책임이 반복되기 시작할 때 분리하는 편이 낫다.")
            ),
            workedExample = """
                OrderController
                  HTTP body 검증
                  ↓
                OrderService.placeOrder(userId, items)
                  재고 확인
                  가격 계산
                  주문 규칙 확인
                  ↓
                OrderRepository.save(order)
                  실제 DB 접근

                test에서는 FakeOrderRepository 주입
                DB 없이 주문 규칙 검증 가능
            """,
            mistakes = listOf(
                "service를 controller 코드 복사본처럼 만든다.",
                "repository interface가 DB table CRUD와 1:1이어야 한다고 생각한다.",
                "DI를 특정 framework annotation 이름으로만 이해한다.",
                "작은 기능에도 불필요한 계층을 여러 겹 만들어 이동 비용만 늘린다.",
                "핵심 업무 규칙이 DB ORM object에 직접 강하게 묶인다."
            ),
            questions = listOf(
                "use case와 service를 따로 나누는 기준은 무엇일까?",
                "fake와 mock은 테스트에서 어떤 차이가 있을까?",
                "transaction 경계는 service와 repository 중 어디에 둘까?",
                "dependency inversion은 구체 구현 의존을 어떤 방향으로 바꿀까?"
            )
        ),
        depthPack(
            sectionId = "V1-C07-S03",
            topics = listOf(
                depthTopic("권한 검사는 endpoint 입구 한 번으로 끝나지 않는다", "관리자 화면에 들어올 수 있는지 확인하는 것과 특정 object를 수정할 수 있는지는 별개의 규칙이다. service가 중요 데이터를 다룰 때도 current user와 resource owner 관계를 확인해야 우회 경로를 막을 수 있다."),
                depthTopic("cache는 DB를 없애는 기술이 아니다", "cache miss가 나면 원본 source에서 값을 가져와야 하고 원본이 바뀌면 cache 갱신 정책이 필요하다. hit ratio만 높이는 것이 목표가 아니라 틀린 오래된 값을 얼마나 허용할지와 장애 시 어떤 fallback을 할지도 함께 설계한다."),
                depthTopic("queue는 느린 일을 뒤로 미루면서 새로운 실패를 만든다", "이메일 발송, 이미지 처리처럼 request 안에서 끝낼 필요가 없는 작업을 queue로 보내면 response를 빠르게 할 수 있다. 하지만 message 중복, 처리 실패, 순서, retry, DLQ를 다뤄야 한다. consumer는 같은 message를 두 번 받아도 안전하도록 idempotent하게 만드는 경우가 많다."),
                depthTopic("DB 변경과 event 발행 사이에 틈이 생길 수 있다", "주문은 DB에 저장됐는데 queue publish 직전에 process가 죽으면 후속 작업 event가 사라질 수 있다. outbox pattern은 DB 변경과 발행할 event 기록을 같은 transaction에 남긴 뒤 별도 worker가 전송하도록 해 이 틈을 줄인다."),
                depthTopic("cache와 queue를 넣으면 관찰 지점도 늘려야 한다", "요청이 실패했을 때 DB뿐 아니라 cache hit/miss, queue backlog, consumer error를 함께 봐야 한다. 성능 기술을 하나 추가할수록 장애 경로도 늘어나므로 metric과 log 설계를 같이 해야 한다.")
            ),
            workedExample = """
                주문 생성
                1) 권한 확인
                2) DB transaction 안에서 order 저장
                3) 같은 transaction에 outbox row 저장
                4) commit
                5) background publisher가 outbox를 queue로 전송
                6) email consumer가 message 처리
                7) 실패하면 retry, 계속 실패하면 DLQ

                request와 후속 작업이 분리돼도 추적 id를 유지
            """,
            mistakes = listOf(
                "로그인 여부만 확인하고 resource별 authorization을 생략한다.",
                "cache에 값이 있으니 원본 DB가 없어도 된다고 생각한다.",
                "queue에 넣으면 작업이 반드시 한 번만 처리된다고 가정한다.",
                "retry 가능한 consumer가 side effect를 중복 발생시킨다.",
                "cache와 queue를 추가하면서 monitoring 지표는 추가하지 않는다."
            ),
            questions = listOf(
                "cache stampede를 single-flight로 어떻게 줄일까?",
                "at-least-once delivery에서 idempotency key가 왜 중요할까?",
                "DLQ message는 언제 자동 재처리하고 언제 사람이 조사해야 할까?",
                "saga와 compensation은 여러 service transaction을 어떻게 연결할까?"
            )
        ),
        depthPack(
            sectionId = "V1-C07-S04",
            topics = listOf(
                depthTopic("동시성 버그는 요청 하나씩 보면 안 보인다", "재고가 1개 남았을 때 두 요청이 동시에 재고 1을 읽고 둘 다 구매 성공으로 저장하면 재고가 음수가 될 수 있다. 각 요청 코드는 혼자 실행하면 정상이다. shared state를 읽고 쓰는 구간의 원자성과 DB lock, transaction 조건을 함께 봐야 한다."),
                depthTopic("rate limit은 단순 공격 차단 외에도 자원 보호다", "로그인 brute force뿐 아니라 비싼 report API를 한 사용자가 초당 수백 번 호출하는 상황도 service 전체를 느리게 만들 수 있다. user, IP, API key 단위와 시간 window를 정하고 초과 시 명확한 response와 retry 정보를 준다."),
                depthTopic("logs·metrics·traces는 같은 정보를 세 번 저장하는 게 아니다", "log는 구체 사건과 문맥, metric은 시간에 따른 숫자 추세, trace는 한 request가 여러 service를 거친 경로와 시간을 보여 주는 데 강하다. CPU 상승은 metric으로 발견하고 느린 request trace를 열어 어느 service가 오래 걸렸는지 찾고 해당 log로 세부 오류를 보는 식으로 연결한다."),
                depthTopic("timeout이 없으면 장애가 자원을 계속 붙잡는다", "외부 service가 응답하지 않을 때 무한히 기다리면 thread, connection, request slot이 쌓여 정상 요청까지 처리하지 못할 수 있다. timeout으로 기다림 상한을 정하고 필요하면 제한된 retry와 circuit breaker를 조합한다."),
                depthTopic("운영 문제는 평균만 보면 숨는다", "평균 latency가 100ms여도 일부 사용자는 5초를 기다릴 수 있다. p95, p99 같은 percentile과 error rate, saturation을 함께 보면 tail latency와 과부하를 더 잘 발견할 수 있다.")
            ),
            workedExample = """
                장애 조사
                metric: p99 latency가 300ms → 4s 상승
                trace: payment service 구간이 3.5s
                log: payment provider timeout 반복

                대응
                provider timeout 상한 확인
                retry 횟수 제한
                circuit breaker 상태 확인
                사용자에게 지연/실패 response 제공

                증거를 연결해 원인을 좁힘
            """,
            mistakes = listOf(
                "각 요청이 따로는 정상이라 concurrency bug가 없다고 생각한다.",
                "rate limit을 보안 기능으로만 보고 자원 보호 목적을 무시한다.",
                "모든 정보를 log에만 쌓고 metric과 trace를 쓰지 않는다.",
                "외부 호출에 timeout 없이 기본값만 믿는다.",
                "평균 latency 하나만 보고 느린 사용자 tail을 놓친다."
            ),
            questions = listOf(
                "optimistic lock과 pessimistic lock은 어떤 차이가 있을까?",
                "distributed rate limit은 여러 server에서 count를 어떻게 공유할까?",
                "trace context는 service 사이에서 어떻게 전달될까?",
                "bulkhead는 circuit breaker와 어떤 장애 전파를 다르게 막을까?"
            )
        )
    )
}
