package com.futuretech.poweruser.textbook

data class TextbookChapter(
    val id: String, val number: Int, val title: String, val assetPath: String, val practiceLessonId: String,
    val sourceLessonIds: List<String>, val summary: String, val keyConcepts: List<String>,
    val humanMustKnow: String, val aiCanHelp: String, val estimatedReadMinutes: Int
)

object V1TextbookCatalog {
    const val BOOK_TITLE = "코딩 완전과정 · 기초 시스템 트랙"
    const val BOOK_SUBTITLE = "TRACK 11개 · 각 TRACK은 독립된 책 한 권급으로 확장 · BLOCK → LESSON → 실습"
    const val TRACK_COUNT = 11
    const val TRACK_LABEL = "TRACK"
    const val BLOCK_LABEL = "BLOCK"
    const val LESSON_LABEL = "LESSON"

    val chapters: List<TextbookChapter> = listOf(
        c(
            "V1-C01", 1, "컴퓨터와 코드의 첫 지도", "TB1-C01", listOf("V1-01", "V1-02", "V1-03"),
            "컴퓨터를 전혀 모르는 사람을 기준으로 시작한다. 새 용어를 먼저 쉬운 말로 정의하고, 입력·처리·출력, 프로그램, 코드, 값, 변수, 상태, 파일과 실행의 관계를 생활 예와 직접 실행으로 익힌다.",
            listOf("컴퓨터", "입력·처리·출력", "프로그램과 코드", "값·변수·상태", "실행과 오류"),
            "처음 보는 전문용어를 건너뛰지 않고 자기 말로 설명할 수 있어야 한다. 코드 결과를 외우는 대신 왜 그런 결과가 나오는지 순서대로 설명한다.",
            "AI는 예제 변형과 추가 설명을 도울 수 있지만, 이해하지 못한 용어를 그대로 받아들이거나 실행 결과를 확인하지 않고 맞다고 판단하지 않는다.",
            720
        ),
        c(
            "V1-C02", 2, "파일과 폴더를 처음부터", "TB1-C02", listOf("V1-04", "V1-05", "V1-06"),
            "파일·폴더·경로를 완전 기초부터 시작해 확장자와 실제 형식, 텍스트와 바이너리, 문자 저장, 인코딩, 압축, 권한, 해시와 무결성 확인까지 단계적으로 배운다.",
            listOf("파일·폴더", "경로", "확장자와 실제 형식", "인코딩", "해시와 무결성"),
            "파일 문제를 볼 때 파일 이름만 보지 않고 존재 여부, 위치, 형식, 읽기 가능 여부, 내용 보존 여부를 순서대로 확인한다.",
            "AI가 명령이나 후보 원인을 제시할 수 있지만 실제 파일 경로와 덮어쓰기·삭제 범위는 사람이 확인한다.",
            660
        ),
        c(
            "V1-C03", 3, "프로그램이 실행된다는 뜻", "TB1-C03", listOf("V1-07", "V1-08"),
            "설치된 프로그램과 지금 실행 중인 일을 구분하는 데서 시작해 프로세스, 작업 순서, 기다림, 동시에 진행되는 것처럼 보이는 일, 비동기와 동시성을 쉬운 상황부터 쌓아간다.",
            listOf("프로그램과 실행", "프로세스", "작업 순서", "기다림", "비동기와 동시성"),
            "비동기 같은 단어를 외우지 않고 어떤 일이 먼저 시작되고 언제 끝나며 결과가 어디에 반영되는지 시간 순서로 설명한다.",
            "AI가 비동기 코드 초안을 만들 수 있지만 작업 순서와 공유 상태, 최신 결과가 무엇인지는 사람이 검증한다.",
            720
        ),
        c(
            "V1-C04", 4, "컴퓨터가 실제로 일하는 곳", "TB1-C04", listOf("V1-09", "V1-10", "V1-11", "V1-12"),
            "CPU, 메모리, 저장장치가 각각 무엇인지부터 시작해 입출력, 캐시, 그래픽 처리, 가상메모리, 운영체제 핵심 역할과 성능 병목까지 연결한다.",
            listOf("CPU", "메모리", "저장장치", "입출력", "성능 병목"),
            "앱이 느리다고 무조건 CPU 문제라고 단정하지 않고 어떤 자원이 실제로 기다리고 있는지 측정해서 판단한다.",
            "AI가 측정값을 정리할 수 있지만 기준값과 변경 전후 성능 비교는 사람이 실제 수치로 확인한다.",
            720
        ),
        c(
            "V1-C05", 5, "권한과 터미널 첫걸음", "TB1-C05", listOf("V1-13", "V1-14", "V1-15", "V1-16"),
            "왜 권한이 필요한지부터 시작해 터미널, 명령, 옵션, 입력과 출력 연결, 환경변수, PATH와 비밀값을 안전한 실행 습관으로 연결한다.",
            listOf("권한", "터미널과 셸", "명령과 옵션", "환경변수", "PATH와 비밀값"),
            "명령을 실행하기 전에 현재 위치, 대상, 바뀌는 것, 되돌리는 방법을 확인하고 이해하지 못한 명령을 그대로 붙여넣지 않는다.",
            "AI가 명령을 만들어도 위험 옵션, 경로, 관리자 권한, 비밀값 노출 여부를 사람이 검토한다.",
            660
        ),
        c(
            "V1-C06", 6, "설치·버전·의존성 이해하기", "TB1-C06", listOf("V1-17", "V1-18", "V1-19"),
            "앱 설치가 무엇인지부터 시작해 버전, 패키지, 라이브러리, 의존성, 빌드, 설정, 잠금 파일, 데이터 변경, 서명과 되돌리기를 실제 업데이트 문제와 연결한다.",
            listOf("설치와 패키지", "버전", "의존성", "빌드와 설정", "업데이트와 되돌리기"),
            "최신 버전이라는 이유만으로 안전하다고 생각하지 않고 정상 조합을 기록하고 작은 변경 단위로 검증한다.",
            "AI가 변경 내용을 요약할 수 있지만 실제 빌드·테스트와 업그레이드 경로를 확인하지 않고 호환성을 승인하지 않는다.",
            690
        ),
        c(
            "V1-C07", 7, "로그를 읽고 원인을 찾는 법", "TB1-C07", listOf("V1-20"),
            "로그가 왜 필요한지부터 시작해 시간, 심각도, 어느 부분에서 나온 기록인지, 처음 실패한 지점, 오류가 이어진 경로와 여러 요청을 구분하는 방법을 배운다.",
            listOf("로그", "시간", "오류 수준", "최초 실패", "오류 경로"),
            "마지막 오류 한 줄만 보는 대신 문제가 시작되기 전후 사건을 시간 순서로 연결해 최초 비정상 지점을 찾는다.",
            "AI가 로그를 요약할 수 있지만 민감정보 제거와 실제 원문 대조는 사람이 한다.",
            630
        ),
        c(
            "V1-C08", 8, "인터넷이 연결되는 순서", "TB1-C08", listOf("V1-21", "V1-22", "V1-23"),
            "인터넷이 무엇인지부터 시작해 주소, IP, 도메인, DNS, 포트, 연결, 암호화된 통신, HTTP, 요청·응답, 데이터 종류와 API까지 실제 통신 순서대로 배운다.",
            listOf("인터넷과 주소", "DNS", "연결과 포트", "HTTPS", "HTTP와 API"),
            "네트워크 문제를 한 덩어리로 보지 않고 주소 찾기, 연결, 안전한 통신, 요청, 응답 데이터 단계로 나눠 확인한다.",
            "AI가 진단 절차를 제시할 수 있지만 실제 주소, 포트, 응답 코드와 받은 데이터는 직접 확인한다.",
            840
        ),
        c(
            "V1-C09", 9, "Android 앱이 실제로 사는 곳", "TB1-C09", listOf("V1-24", "V1-25"),
            "Android가 앱을 어떻게 분리하고 보호하는지부터 시작해 권한, 저장공간, 백그라운드 작업, 알림, 동기화, 백업과 앱 생명주기를 단계적으로 배운다.",
            listOf("앱 격리", "권한", "백그라운드 작업", "알림", "동기화와 생명주기"),
            "알림·저장·동기화 문제를 앱 코드 하나로 단정하지 않고 운영체제 권한과 정책, 서버 흐름을 함께 나눠 본다.",
            "AI가 확인 항목을 정리할 수 있지만 기기에서 실제 권한과 상태를 직접 시험한다.",
            750
        ),
        c(
            "V1-C10", 10, "느림·멈춤·오류를 찾는 법", "TB1-C10", listOf("V1-26", "V1-27"),
            "오류를 재현하는 법부터 관찰, 가설, 범위 좁히기, 최소 수정, 경계값 확인, 회귀시험까지 실제 디버깅 절차를 반복 훈련한다.",
            listOf("재현", "관찰과 가설", "범위 좁히기", "경계값", "회귀시험"),
            "한 번에 하나의 가설만 시험하고 수정 전후를 같은 조건으로 비교하며 실행하지 않은 검증을 PASS라고 쓰지 않는다.",
            "AI가 가설과 테스트 후보를 만들 수 있지만 최종 판정은 실제 실행 증거로만 한다.",
            780
        ),
        c(
            "V1-C11", 11, "종합 시스템 장애 진단 프로젝트", "TB1-C11", listOf("V1-28"),
            "앞의 열 개 TRACK에서 배운 개념을 실제 앱 문제에 적용한다. 문제 정의, 시스템 지도, 증거 수집, 원인 확인, 최소 수정, 회귀검증과 인계까지 처음부터 끝까지 수행한다.",
            listOf("시스템 지도", "증거 장부", "근본 원인", "최소 수정", "회귀검증"),
            "완료는 고친 것 같다는 느낌이 아니라 재현, 수정, 동일 조건 재검증, 기존 기능 회귀시험, 증거 보존까지 끝났을 때만 선언한다.",
            "AI가 조사와 문서 정리를 도울 수 있지만 최종 완료 판정은 실제 로그·테스트·설치·데이터 증거로만 한다.",
            960
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
        "textbook/v1/chapter_${number.toString().padStart(2, '0')}.md",
        practice,
        source,
        summary,
        keyConcepts,
        human,
        ai,
        minutes
    )
}
