package com.futuretech.poweruser.textbook

data class TextbookChapter(
    val id: String, val number: Int, val title: String, val assetPath: String, val practiceLessonId: String,
    val sourceLessonIds: List<String>, val summary: String, val keyConcepts: List<String>,
    val humanMustKnow: String, val aiCanHelp: String, val estimatedReadMinutes: Int
)

object V1TextbookCatalog {
    const val BOOK_TITLE = "V1 · 컴퓨터·인터넷·디지털 시스템"
    const val BOOK_SUBTITLE = "코딩을 처음 배우는 사람을 위한 1권 · 쉬운말부터 실전 진단까지"

    val chapters: List<TextbookChapter> = listOf(
        c(
            "V1-C01", 1, "컴퓨터와 코드의 첫 지도", "TB1-C01", listOf("V1-01", "V1-02", "V1-03"),
            "코딩을 한 번도 해보지 않은 사람을 기준으로 입력·처리·출력부터 시작해 프로그램, 코드, 값, 변수, 상태, 파일, 인터넷, 서버, API, JSON을 한 단계씩 연결한다.",
            listOf("입력·처리·출력", "프로그램과 코드", "값·변수·상태", "파일과 저장", "서버·API·JSON"),
            "영어 용어를 외우기보다 각 개념을 자기 말로 설명하고, 화면에 보이는 결과가 여러 단계를 거쳐 만들어진다는 큰 지도를 이해한다.",
            "용어 검색과 예제 변형은 AI에게 맡길 수 있지만, 모르는 단어를 건너뛰거나 AI 설명을 이해 없이 받아들이지 않는다.",
            210
        ),
        c(
            "V1-C02", 2, "파일과 폴더를 처음부터", "TB1-C02", listOf("V1-04", "V1-05", "V1-06"),
            "파일·폴더·경로를 가장 기초부터 배우고, 확장자와 실제 형식, 텍스트와 바이너리, 인코딩, 압축, 권한, SHA-256을 실제 파일 문제와 연결한다.",
            listOf("파일·폴더", "절대/상대 경로", "확장자와 실제 형식", "압축·인코딩", "SHA-256"),
            "파일 이름만 보지 않고 존재→경로→권한→형식→무결성→출처 순서로 확인한다.",
            "경로 후보나 해시 계산 명령은 AI가 도울 수 있지만 실제 대상 파일과 삭제·덮어쓰기 범위는 사람이 확인한다.",
            190
        ),
        c(
            "V1-C03", 3, "프로그램이 실행된다는 뜻", "TB1-C03", listOf("V1-07", "V1-08"),
            "설치된 프로그램과 실행 중 프로세스를 구분한 뒤 스레드, UI 스레드, 백그라운드 작업, 비동기, 동시성을 쉬운 사례로 배운다.",
            listOf("프로그램/프로세스", "스레드", "UI 스레드", "비동기", "동시성"),
            "앱이 멈췄다는 말을 더 잘게 나누고 작업 순서와 공유 상태가 결과에 어떤 영향을 주는지 설명한다.",
            "동시성 코드 초안은 AI가 도울 수 있지만 어떤 작업이 먼저 끝나야 하는지와 최신 결과가 무엇인지는 사람이 검증한다.",
            175
        ),
        c(
            "V1-C04", 4, "컴퓨터가 일하는 곳", "TB1-C04", listOf("V1-09", "V1-10", "V1-11", "V1-12"),
            "CPU, RAM, 저장장치부터 시작해 I/O, GPU, NPU, 캐시, 가상메모리, 커널을 느림과 오류의 원인을 구분하는 관점으로 배운다.",
            listOf("CPU", "RAM/저장장치", "I/O", "캐시", "병목과 측정"),
            "느림을 CPU 문제라고 바로 단정하지 않고 어느 자원이 실제 병목인지 측정한 뒤 판단한다.",
            "측정값 정리와 후보 목록은 AI에게 맡길 수 있지만 실제 baseline과 전후 성능 비교는 사람이 확인한다.",
            170
        ),
        c(
            "V1-C05", 5, "권한과 터미널 첫걸음", "TB1-C05", listOf("V1-13", "V1-14", "V1-15", "V1-16"),
            "권한의 이유부터 터미널·셸·옵션·파이프·리다이렉션·환경변수·PATH·비밀값을 안전한 명령 실행 습관으로 연결한다.",
            listOf("권한", "터미널/셸", "pipe/redirection", "환경변수", "PATH와 secret"),
            "명령 실행 전에 현재 위치, 대상, 옵션, 권한, 복구 방법을 확인하고 모르는 명령을 그대로 붙여넣지 않는다.",
            "AI가 명령을 만들어도 위험 옵션과 경로, secret 노출, 관리자 권한 필요성을 사람이 검토한다.",
            165
        ),
        c(
            "V1-C06", 6, "설치·버전·의존성 이해하기", "TB1-C06", listOf("V1-17", "V1-18", "V1-19"),
            "APK 설치부터 버전, 패키지, 라이브러리, 의존성, 빌드, 설정, lock 파일, migration, 서명, rollback까지 업데이트 문제를 이해하는 기초를 만든다.",
            listOf("설치/패키지", "버전", "의존성", "빌드/설정", "migration/rollback"),
            "최신 버전이 무조건 안전하다고 생각하지 않고 정상 버전 조합과 환경을 기록한 뒤 작은 변경으로 검증한다.",
            "release note 요약은 AI가 도울 수 있지만 실제 build·test·upgrade path 확인 없이 호환성을 승인하지 않는다.",
            175
        ),
        c(
            "V1-C07", 7, "로그를 읽는 법", "TB1-C07", listOf("V1-20"),
            "로그 한 줄을 시간·레벨·컴포넌트·메시지로 나눠 읽는 것부터 시작해 최초 실패, stack trace, correlation id, 민감정보 마스킹을 익힌다.",
            listOf("timestamp", "log level", "최초 실패", "stack trace", "correlation id"),
            "마지막 ERROR 한 줄에 집착하지 않고 재현 시각 전후의 사건을 시간순으로 연결해 최초 비정상 지점을 찾는다.",
            "로그 요약은 AI가 도울 수 있지만 비밀값 제거와 실제 원문 로그 대조는 사람이 한다.",
            160
        ),
        c(
            "V1-C08", 8, "인터넷이 연결되는 순서", "TB1-C08", listOf("V1-21", "V1-22", "V1-23"),
            "Wi-Fi와 인터넷의 차이부터 IP, 도메인, DNS, 포트, TCP/UDP, TLS, HTTP, URL, 상태코드까지 실제 연결 순서에 맞춰 배운다.",
            listOf("IP/도메인", "DNS", "포트/연결", "TLS/HTTPS", "HTTP/API"),
            "네트워크가 안 된다는 말을 DNS·연결·TLS·HTTP·응답 데이터 단계로 나누어 확인한다.",
            "진단 명령은 AI가 도울 수 있지만 실제 host, port, status code, response를 직접 확인한다.",
            210
        ),
        c(
            "V1-C09", 9, "Android 앱이 실제로 사는 곳", "TB1-C09", listOf("V1-24", "V1-25"),
            "Android sandbox와 runtime permission부터 저장공간, WorkManager, 알림 채널, push token, sync와 backup, lifecycle까지 앱이 운영체제 안에서 동작하는 흐름을 배운다.",
            listOf("sandbox", "runtime permission", "background work", "알림 경로", "sync/backup"),
            "알림·저장·동기화 문제를 앱 코드 한 곳으로 단정하지 않고 Android 권한과 OS 정책, 서버 흐름을 함께 나눈다.",
            "권한/설정 목록 정리는 AI가 도울 수 있지만 기기별 실제 상태와 복구 가능성은 사람이 시험한다.",
            200
        ),
        c(
            "V1-C10", 10, "느림·멈춤·오류를 찾는 법", "TB1-C10", listOf("V1-26", "V1-27"),
            "디버깅을 재현→관찰→가설→범위 좁히기→최소 수정→경계값 테스트→회귀시험의 순서로 익히고 PASS를 증거로 말하는 법을 배운다.",
            listOf("재현", "가설", "baseline", "경계값", "회귀시험"),
            "한 번에 하나의 가설만 시험하고 수정 전후를 같은 조건으로 비교하며 실행하지 않은 검증을 PASS라고 쓰지 않는다.",
            "가설과 테스트 케이스 초안은 AI에게 맡길 수 있지만 최종 판정은 실제 실행 증거로만 한다.",
            210
        ),
        c(
            "V1-C11", 11, "종합 시스템 장애 진단 프로젝트", "TB1-C11", listOf("V1-28"),
            "가족 알림, 앱 업데이트, 주가 데이터, 중복 자동화 같은 실제 사례를 사용해 문제 정의부터 root cause, 최소 수정, regression, handoff까지 한 번에 완성한다.",
            listOf("시스템 지도", "증거 장부", "root cause", "최소 수정", "회귀검증"),
            "완료는 ‘고친 것 같음’이 아니라 재현→수정→동일 조건 재검증→기존 기능 회귀시험→증거 보존으로 승인한다.",
            "보고서 정리와 대안 탐색은 AI가 도울 수 있지만 최종 COMPLETE는 실제 로그·테스트·설치·데이터 증거로만 판정한다.",
            240
        )
    )

    val practiceLessonIds: Set<String> = chapters.map { it.practiceLessonId }.toSet()
    val allSourceLessonIds: List<String> = chapters.flatMap { it.sourceLessonIds }
    fun chapterById(id: String): TextbookChapter? = chapters.find { it.id == id }
    fun chapterByPracticeLessonId(id: String): TextbookChapter? = chapters.find { it.practiceLessonId == id }

    private fun c(
        id: String,
        number: Int,
        title: String,
        practice: String,
        source: List<String>,
        summary: String,
        keyConcepts: List<String>,
        human: String,
        ai: String,
        minutes: Int
    ) = TextbookChapter(
        id,
        number,
        title,
        "textbook/v1/chapter_${number.toString().padStart(2, '0')}_full.md",
        practice,
        source,
        summary,
        keyConcepts,
        human,
        ai,
        minutes
    )
}
