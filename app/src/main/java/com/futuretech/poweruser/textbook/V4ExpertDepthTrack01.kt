package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack01 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C01-S01",
            depthTitle("컴퓨터를 부품 목록이 아니라 “지연과 병목이 있는 시스템”으로 본다"),
            depthParagraph("""
V3에서 입력→처리→출력과 CPU·RAM·저장장치의 역할을 이미 배웠다면, 다음 단계는 “어느 부품이 무엇을 한다”가 아니라 “한 동작이 왜 늦어지고 어디서 막히는가”를 보는 것이다. 프로그램의 체감 속도는 계산량 하나로 결정되지 않는다. CPU가 계산하는 시간, RAM에서 데이터를 찾는 시간, SSD에서 읽는 시간, 네트워크 왕복을 기다리는 시간은 규모가 완전히 다르다. 그래서 성능 문제를 볼 때는 빠른 부품 이름을 외우는 대신 작업을 여러 구간으로 쪼개고 각 구간의 대기 시간을 측정한다.
"""),
            depthHeading("1. CPU가 놀고 있는데도 앱이 느릴 수 있다"),
            depthParagraph("""
웹 화면이 4초 뒤에 뜬다고 해서 CPU가 4초 동안 계산했다는 뜻은 아니다. CPU 사용률이 10%인데도 느리다면 네트워크 응답, DB lock, 디스크 I/O, 다른 프로세스와의 자원 경쟁을 기다리고 있을 수 있다. 이를 I/O bound라고 부르는 경우가 많다. 반대로 영상 인코딩처럼 CPU 코어를 계속 사용하며 계산이 병목이면 CPU bound에 가깝다. 같은 “느림”이라도 병목 종류가 다르면 해결 방법도 완전히 달라진다.
"""),
            depthHeading("2. 캐시 계층과 “가까운 데이터가 빠르다”는 원리"),
            depthParagraph("""
CPU는 RAM보다 훨씬 빠르게 명령을 처리할 수 있기 때문에 CPU 내부에도 작은 cache가 여러 층 존재한다. 자주 쓰는 데이터가 가까운 cache에 있으면 빠르지만, 멀리 있는 RAM이나 저장장치까지 가야 하면 기다림이 커진다. 이 때문에 같은 O(n) 반복문이라도 연속된 배열을 순서대로 읽는 코드가 여기저기 흩어진 객체를 따라가는 코드보다 실제 하드웨어에서 훨씬 빠를 수 있다. 뒤에서 자료구조를 배울 때 “메모리 배치”가 성능에 영향을 주는 이유다.
"""),
            depthHeading("3. 운영체제는 CPU 시간을 잘게 나눠 준다"),
            depthParagraph("""
프로그램이 여러 개 동시에 실행되는 것처럼 보이지만 한 CPU 코어는 특정 순간에는 한 실행 흐름을 처리한다. 운영체제 스케줄러가 매우 짧은 시간 단위로 실행할 작업을 바꾸고 여러 코어가 있으면 일부 작업을 진짜 병렬로 진행한다. 따라서 “프로그램 A가 실행 중”이라는 사실만으로 CPU를 독점한다고 생각하면 안 된다. context switch가 너무 많아지면 전환 비용도 생기므로 thread를 무작정 많이 만드는 것이 항상 빠르지 않다.
"""),
            depthHeading("4. 저장 성공과 “전원이 꺼져도 남음”은 같은 말이 아니다"),
            depthParagraph("""
애플리케이션이 파일 쓰기 함수를 호출해 성공을 받았더라도 데이터가 즉시 영구 저장장치에 물리적으로 기록됐다고 단정할 수는 없다. 운영체제와 저장장치는 성능을 위해 write buffer와 cache를 사용한다. 중요한 데이터베이스가 WAL이나 fsync 같은 개념을 다루는 이유는 “사용자에게 성공했다고 말한 데이터가 장애 뒤에도 남아야 한다”는 durability 요구 때문이다. 초보 단계에서는 저장=영구라는 단순 모델을 쓰지만 운영 단계에서는 성공의 의미를 더 엄격히 정의해야 한다.
"""),
            depthCode("""
사용자가 “저장”을 눌렀다
↓
UI event 처리 3ms
↓
JSON 변환 2ms
↓
API 왕복 180ms
↓
DB lock 대기 900ms
↓
DB write 12ms
↓
응답 렌더링 8ms

총 1.1초가 넘지만 CPU 계산은 몇 ms뿐이다.
가장 먼저 고칠 곳은 CPU가 아니라 DB lock 대기다.
"""),
            depthBullets(
                "성능 문제는 “느리다”가 아니라 단계별 latency와 자원 사용률로 표현한다.",
                "CPU·RAM·SSD 용량 숫자만으로 실제 앱 속도를 예측하지 않는다.",
                "성공 응답의 의미가 “메모리에 기록됨”인지 “영구 저장됨”인지 시스템마다 다를 수 있다.",
                "측정 없이 하드웨어 업그레이드부터 하는 것은 병목이 다른 곳이면 거의 효과가 없다."
            )
        ),
        expertPack(
            sectionId = "V1-C01-S02",
            depthTitle("파일을 “이름 붙은 상자”가 아니라 운영체제가 관리하는 영속 데이터 구조로 본다"),
            depthHeading("파일 이름과 실제 파일 객체 사이에는 메타데이터가 있다"),
            depthParagraph("""
V3에서 경로·확장자·byte를 배웠다면 이제 파일 시스템이 파일을 어떻게 관리하는지 본다. 많은 파일 시스템에서 사람이 보는 이름은 디렉터리 항목이고, 실제 데이터 블록·소유자·권한·수정 시각 같은 메타데이터는 별도 구조에 기록된다. Unix 계열의 inode가 대표적이다. 그래서 이름을 바꾸는 작업이 파일 전체 내용을 다시 쓰는 것보다 훨씬 빠를 수 있고, 하나의 실제 파일을 여러 이름이 가리키는 hard link 같은 기능도 가능하다.
"""),
            depthHeading("1. 확장자보다 강한 단서: magic number와 구조 검증"),
            depthParagraph("""
photo.jpg라는 이름은 사람이 붙인 문자열일 뿐이다. 일부 형식은 파일 시작 부분에 고유한 byte signature를 두고, parser는 헤더와 내부 구조를 읽어 실제 형식을 판단한다. 업로드 보안에서 확장자만 검사하면 공격자가 이름을 바꿔 우회할 수 있다. 그렇다고 magic byte 하나만 맞으면 안전한 것도 아니다. 이후 구조가 유효한지, 압축 폭탄처럼 비정상적인 크기 확장이 없는지까지 검증해야 한다.
"""),
            depthHeading("2. 안전한 저장은 임시 파일 + 원자적 교체를 자주 쓴다"),
            depthParagraph("""
설정 파일을 직접 덮어쓰는 도중 앱이 죽으면 파일 절반만 새 내용이고 절반은 사라진 상태가 될 수 있다. 그래서 새 내용을 임시 파일에 완전히 쓴 뒤 필요하면 flush/fsync하고, 마지막에 rename으로 교체하는 패턴을 사용한다. 같은 파일 시스템 안에서 rename이 원자적으로 보장되는 환경이라면 독자가 “이전 버전” 또는 “새 버전” 중 하나만 보게 만들기 쉽다. 이것이 단순히 write 한 번 호출하는 것보다 복구 가능한 설계다.
"""),
            depthHeading("3. 파일 잠금과 동시 쓰기"),
            depthParagraph("""
두 프로세스가 같은 파일의 같은 위치를 동시에 수정하면 각자 혼자 실행할 때는 정상이어도 결과가 뒤섞일 수 있다. 파일 lock을 쓰거나 한 process만 writer가 되게 만들거나, 아예 transaction을 제공하는 DB에 상태를 맡기는 이유다. 특히 JSON 파일 하나를 여러 요청이 read→modify→write하는 구조는 마지막 writer가 앞의 변경을 덮어쓰는 lost update를 만들기 쉽다.
"""),
            depthHeading("4. 경로는 보안 경계이기도 하다"),
            depthParagraph("""
사용자가 준 파일명을 서버 경로에 그대로 붙이면 ../../secret 같은 path traversal이 생길 수 있다. 안전한 시스템은 저장 루트를 고정하고 입력을 canonicalize한 뒤 최종 경로가 허용된 루트 안인지 검사하거나, 사용자가 준 이름과 실제 저장 이름을 분리한다. “파일을 찾는 문자열”이 곧 접근 권한이 되어서는 안 된다.
"""),
            depthCode("""
안전한 설정 저장 예

1) settings.tmp 에 새 JSON 전체 작성
2) 파일 형식 검증
3) 필요한 환경이면 flush/fsync
4) settings.tmp → settings.json 원자적 rename
5) 이전 백업은 정책에 따라 보관

중간에 죽어도 “반쯤 쓴 settings.json”을 만들 가능성을 줄인다.
"""),
            depthBullets(
                "경로·권한·파일 형식 검증은 서로 다른 문제다.",
                "읽기 성공은 내용이 신뢰할 수 있다는 뜻이 아니다. parser 검증이 따로 필요하다.",
                "여러 writer가 있는 파일 기반 상태는 concurrency 정책이 없으면 쉽게 깨진다."
            )
        ),
        expertPack(
            sectionId = "V1-C01-S03",
            depthTitle("문자열을 “글자 배열”이라고만 보면 깨지는 지점: Unicode의 실제 경계"),
            depthParagraph("""
V3에서 Unicode와 UTF-8의 기본 관계를 이미 이해했다면, 이제 실제 버그를 만드는 세 단위를 분리해야 한다. code point는 Unicode가 부여한 번호이고, code unit은 UTF-16 같은 표현에서 언어 런타임이 다루는 단위이며, grapheme cluster는 사용자가 화면에서 “한 글자”라고 느끼는 단위다. 이 셋이 같을 때도 많지만 항상 같지는 않는다. 이 차이를 놓치면 길이 제한, 커서 이동, 문자열 자르기, 검색에서 예상치 못한 오류가 난다.
"""),
            depthHeading("1. 같은 글자가 byte 수준에서 다를 수 있다: NFC와 NFD"),
            depthParagraph("""
é를 하나의 code point U+00E9로 저장할 수도 있고, e(U+0065)와 결합 악센트(U+0301) 두 code point로 표현할 수도 있다. 화면에는 거의 똑같이 보이지만 byte sequence는 다르다. macOS 파일명, 외부 데이터, 복사·붙여넣기에서 이런 조합 차이가 들어오면 단순 문자열 == 비교가 실패할 수 있다. Unicode normalization은 서로 동등한 표현을 NFC나 NFD 같은 정규형으로 맞추는 과정이다. 한국어도 완성형 음절과 자모 조합 표현 때문에 같은 문제가 생길 수 있다.
"""),
            depthHeading("2. String.length가 사용자 글자 수가 아닐 수 있다"),
            depthParagraph("""
JavaScript의 length는 UTF-16 code unit 수를 기준으로 하므로 일부 이모지는 2 이상이 되고, 가족 이모지처럼 여러 code point를 ZWJ로 연결한 것은 화면에 하나처럼 보여도 훨씬 긴 길이를 가질 수 있다. “닉네임 최대 10글자” 요구를 단순 length <= 10으로 구현하면 사용자가 보는 글자 수와 정책이 달라질 수 있다. UI 정책이 grapheme 단위인지, 저장 정책이 byte 단위인지 요구사항부터 나눠야 한다.
"""),
            depthHeading("3. DB 길이 제한과 byte 제한은 다르다"),
            depthParagraph("""
DB의 VARCHAR(100)이 정확히 100 byte를 뜻하는지 100문자를 뜻하는지는 DB와 설정에 따라 확인해야 한다. 반대로 네트워크 프로토콜이나 외부 API가 “UTF-8 255 byte 이하”라고 명시하면 한글·이모지는 영문보다 더 빨리 한도에 도달할 수 있다. 따라서 문자 수 제한과 전송 byte 제한을 한 변수로 처리하지 않는다.
"""),
            depthHeading("4. charset이 틀렸는지 확인하는 실전 순서"),
            depthParagraph("""
깨진 문자열을 눈으로 추측하지 말고 원래 byte를 hex로 확인한다. HTTP라면 Content-Type의 charset, 파일이라면 실제 byte와 BOM, DB라면 connection/client encoding을 확인한다. 같은 byte를 잘못 decoding한 것인지, 처음 encoding할 때 이미 잘못된 byte가 만들어진 것인지 경계를 찾는 것이 핵심이다. UTF-8의 BOM EF BB BF는 필수가 아니며 일부 parser에서는 예상치 못한 첫 문자처럼 취급될 수도 있다.
"""),
            depthCode("""
문자 é 의 두 표현

NFC:  U+00E9
UTF-8: C3 A9

NFD:  U+0065 U+0301
UTF-8: 65 CC 81

화면은 같아 보여도 byte가 다르다.
검색 key를 만들기 전 normalization 정책을 맞추지 않으면 exact match가 실패할 수 있다.
"""),
            depthHeading("5. 정규식과 문자열 자르기도 Unicode 정책이 필요하다"),
            depthParagraph("""
정규식의 . 한 글자, 대소문자 변환, 문자 클래스가 어떤 Unicode 규칙을 따르는지 언어별로 다르다. 문자열을 임의 index에서 잘라 UTF-16 surrogate pair나 결합 문자 중간을 끊으면 깨진 표시가 생길 수 있다. 국제화가 필요한 코드는 “문자열은 단순한 char 배열”이라는 가정을 버리고 라이브러리의 Unicode-aware API를 확인한다.
"""),
            depthBullets(
                "정책을 먼저 말로 쓴다: 사용자에게 보이는 글자 수 제한인지, code point 수인지, UTF-8 byte 제한인지.",
                "검색·중복 검사는 normalization과 대소문자/locale 정책까지 정의한다.",
                "문자 깨짐 조사에서는 원본 byte와 decoding 규칙을 따로 수집한다."
            )
        ),
        expertPack(
            sectionId = "V1-C01-S04",
            depthTitle("소스 코드 한 줄이 CPU까지 내려가는 길: process·thread·runtime·system call"),
            depthHeading("프로세스는 “실행 중인 앱”보다 조금 더 구체적인 격리 단위다"),
            depthParagraph("""
운영체제는 각 process에게 자기만의 가상 주소 공간을 제공한다. 프로그램은 마치 자신이 넓은 메모리를 독점하는 것처럼 주소를 사용하지만 실제 물리 메모리는 운영체제가 page 단위로 매핑한다. 이 덕분에 한 process의 잘못된 pointer가 다른 process 메모리를 마음대로 읽지 못하도록 격리하기 쉽다. virtual memory는 RAM을 늘려 주는 마술이 아니라 주소 공간·보호·매핑을 제공하는 핵심 추상화다.
"""),
            depthHeading("1. thread는 같은 process 안에서 자원을 공유한다"),
            depthParagraph("""
같은 process의 여러 thread는 코드와 heap 같은 많은 자원을 공유하지만 각자 call stack과 실행 위치를 갖는다. 공유 상태를 동시에 수정할 수 있기 때문에 race condition, lock, atomic operation 같은 문제가 생긴다. “thread를 늘리면 빨라진다”가 아니라 작업이 병렬화 가능한지, 공유 자원 경쟁이 얼마나 있는지부터 판단해야 한다.
"""),
            depthHeading("2. compiler·bytecode·JIT는 한 줄로 나뉘지 않는다"),
            depthParagraph("""
언어 구현은 compile 방식과 interpret 방식을 섞을 수 있다. Java/Kotlin은 JVM bytecode를 만들고 런타임에서 JIT 최적화를 할 수 있으며, JavaScript 엔진도 소스를 parse한 뒤 bytecode·JIT를 사용한다. 중요한 것은 “컴파일 언어/인터프리터 언어”라는 이분법보다 어느 단계에서 어떤 중간 표현을 만들고 최적화하며 실행하는지 보는 것이다.
"""),
            depthHeading("3. 파일 읽기와 네트워크는 CPU 명령만으로 끝나지 않는다"),
            depthParagraph("""
프로그램이 파일 open/read나 socket send를 호출하면 일반적으로 runtime/library를 거쳐 kernel에 system call을 요청한다. kernel은 권한과 자원 상태를 확인하고 device driver나 network stack과 연결한다. 그래서 application stack trace만 봐서는 실제 I/O 대기 원인이 안 보일 수 있고, 운영체제 수준의 profiler나 trace가 필요한 경우도 있다.
"""),
            depthHeading("4. ABI와 환경 차이가 “내 컴퓨터에서는 되는데”를 만든다"),
            depthParagraph("""
같은 source라도 CPU architecture, operating system, runtime version, native library ABI가 다르면 동작이 달라질 수 있다. x86_64용 native binary를 ARM 기기에서 그대로 실행할 수 없는 이유가 대표적이다. container와 build pipeline은 이런 환경 차이를 줄이려고 dependency와 실행 조건을 고정하지만 kernel·hardware까지 완전히 같게 만드는 것은 아니다.
"""),
            depthHeading("5. 부동소수점과 정수 overflow는 실행 환경의 숫자 모델 문제다"),
            depthParagraph("""
float는 근삿값이라 0.1+0.2가 정확한 0.3이 아닐 수 있고, 고정 폭 정수는 범위를 넘으면 overflow가 발생한다. 돈은 최소 화폐 단위 정수나 decimal을 쓰고, 외부에서 큰 숫자를 받을 때는 언어의 integer 범위를 확인한다. “수학적으로 맞는 식”이 “컴퓨터 표현에서도 안전한 식”인지 별도로 검증해야 한다.
"""),
            depthCode("""
버튼 클릭
→ 앱 event handler
→ runtime/library 함수
→ 파일 저장 요청
→ system call
→ kernel 권한·파일시스템 처리
→ storage driver
→ 장치

문제가 생기면 어느 경계에서 실패했는지 추적한다.
permission denied는 계산식 문제가 아니고, disk full은 UI 문제가 아니다.
"""),
            depthBullets(
                "process 격리, thread 공유, virtual memory 매핑을 한 그림에 섞지 않는다.",
                "언어 이름만 보고 실행 모델을 단정하지 않고 해당 runtime의 실제 파이프라인을 확인한다.",
                "native dependency가 있으면 CPU architecture와 ABI가 배포 조건이 된다."
            )
        ),
        expertPack(
            sectionId = "V1-C01-S05",
            depthTitle("코드를 읽는 능력을 “값 추적”에서 “불변식과 호출 구조 추적”으로 올린다"),
            depthParagraph("""
V3에서 한 줄씩 실행하고 오류를 일부러 만들어 봤다면 이제 디버깅의 단위를 넓힌다. 큰 프로그램에서는 모든 줄을 처음부터 끝까지 따라갈 수 없다. 대신 함수가 지켜야 할 조건, 데이터가 변하는 지점, 호출 관계, 예외가 전파되는 경계를 잡아 원인 후보를 줄인다. 이것이 “감으로 고치기”와 체계적 디버깅의 차이다.
"""),
            depthHeading("1. invariant는 중간 상태가 반드시 지켜야 할 규칙이다"),
            depthParagraph("""
은행 계좌라면 “잔액은 허용된 범위를 벗어나지 않는다”, 장바구니라면 “총액은 항목 금액 합과 같다” 같은 규칙을 invariant로 둘 수 있다. 버그가 최종 화면에서 발견돼도 invariant가 처음 깨지는 지점을 찾으면 원인에 더 가깝다. assert나 validation을 경계에 두는 이유는 잘못된 상태가 멀리 퍼지기 전에 멈추기 위해서다.
"""),
            depthHeading("2. call stack을 읽으면 “에러 난 줄”의 앞 이야기가 보인다"),
            depthParagraph("""
함수 A가 B를 부르고 B가 C를 부른 뒤 C에서 예외가 났다면 C의 줄만 보는 것으로 부족할 수 있다. C에 잘못된 값을 넘긴 B, B를 잘못 호출한 A가 실제 원인일 수 있다. stack trace는 호출 경로를 역으로 보여 주므로 맨 위 한 줄만 읽지 말고 application code가 처음 등장하는 frame과 입력이 어디서 만들어졌는지 확인한다.
"""),
            depthHeading("3. mutation point를 줄이면 상태 버그를 찾기 쉬워진다"),
            depthParagraph("""
같은 객체를 열 군데에서 수정하면 값이 언제 바뀌었는지 찾기 어렵다. 가능한 곳에서는 immutable value를 사용하고, 수정이 필요한 상태는 update 함수를 한곳으로 모으면 관찰 지점이 줄어든다. 디버거 watchpoint나 로그를 “읽는 곳”보다 “쓰는 곳”에 배치하는 이유도 같다.
"""),
            depthHeading("4. 예외는 발생 위치와 처리 위치가 다를 수 있다"),
            depthParagraph("""
낮은 계층에서 파일 읽기 오류가 발생해도 사용자가 볼 메시지는 상위 계층에서 “설정 파일을 읽을 수 없습니다”로 바꿀 수 있다. 그러나 원래 exception의 원인과 stack을 잃으면 진단이 어려워진다. 예외를 의미 있는 도메인 오류로 감싸더라도 cause를 보존하고, 무조건 catch 후 무시하는 코드를 피한다.
"""),
            depthHeading("5. 최소 재현과 결정성"),
            depthParagraph("""
버그가 외부 API, 현재 시간, random 값, 공유 DB 상태에 의존하면 같은 조건을 만들기 어렵다. 테스트에서 clock과 random source를 주입하고 외부 dependency를 통제하면 재현성을 높일 수 있다. 재현 가능한 버그는 수정 검증도 가능하지만 “가끔 사라지는 버그”는 고친 것처럼 착각하기 쉽다.
"""),
            depthCode("""
증상: 합계가 가끔 음수

좋지 않은 접근
- 계산 함수를 여러 줄 수정
- 음수면 0으로 강제 보정

추적 접근
1) invariant: quantity >= 0, price >= 0 선언
2) 상태가 바뀌는 모든 mutation point 찾기
3) quantity가 처음 -1이 되는 호출 stack 기록
4) 동일 입력으로 재현
5) 원인 한 곳만 수정
6) regression test 추가
"""),
            depthBullets(
                "오류 메시지를 지우거나 증상만 clamp해서 PASS로 만들지 않는다.",
                "stack trace의 발생 지점과 잘못된 값의 생성 지점을 구분한다.",
                "수정 후에는 같은 재현 절차와 반례로 다시 실패하지 않는지 확인한다."
            )
        )
    )
}
