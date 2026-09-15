package com.futuretech.poweruser.textbook

data class TextbookChapter(
    val id: String, val number: Int, val title: String, val assetPath: String, val practiceLessonId: String,
    val sourceLessonIds: List<String>, val summary: String, val keyConcepts: List<String>,
    val humanMustKnow: String, val aiCanHelp: String, val estimatedReadMinutes: Int
)

object V1TextbookCatalog {
    const val BOOK_TITLE = "V1 · 컴퓨터·인터넷·디지털 시스템"
    const val BOOK_SUBTITLE = "AI 시대 Power User 실전 교재"

    val chapters: List<TextbookChapter> = listOf(
        c("V1-C01",1,"시스템 전체 그림과 디지털 표현","TB1-C01",listOf("V1-01","V1-02","V1-03"),"화면에서 보이는 결과를 앱·OS·저장장치·네트워크·서버·바이트 표현의 흐름으로 분해한다.",listOf("시스템 경계","프로그램/프로세스","비트/바이트","Unicode/UTF-8","클라우드 데이터 흐름"),"증상을 한 덩어리로 부르지 말고 어느 계층까지 정상인지 증거로 좁힌다.","단위 변환·로그 정리·흐름도 초안은 맡겨도 되지만 원인 계층 판정은 직접 검증한다.",40),
        c("V1-C02",2,"파일·경로·형식·무결성","TB1-C02",listOf("V1-04","V1-05","V1-06"),"파일의 위치·형식·확장자·MIME·압축·해시를 구분하고 다운로드/배포 파일을 안전하게 검증한다.",listOf("절대/상대 경로","확장자와 실제 형식","MIME","압축","SHA-256 무결성"),"이름이 아니라 실제 경로와 형식, 해시 증거로 파일을 식별한다.","명령 초안과 해시 계산은 AI가 도울 수 있지만 대상 파일을 정확히 지정하고 결과를 비교하는 일은 사람이 한다.",40),
        c("V1-C03",3,"프로세스와 동시성","TB1-C03",listOf("V1-07","V1-08"),"설치된 프로그램과 실행 중 프로세스, 메인 스레드와 백그라운드 작업의 차이를 실제 앱 멈춤 현상에 연결한다.",listOf("프로그램/프로세스","스레드","메인/UI 스레드","동시성","race 감각"),"앱이 설치돼 있다는 사실과 지금 정상 실행 중이라는 사실은 다르며, UI를 막는 작업을 구분해야 한다.","병렬화 코드 초안은 맡길 수 있지만 공유 상태와 완료 순서, UI thread 영향은 직접 확인한다.",45),
        c("V1-C04",4,"메모리·연산장치·커널 경계","TB1-C04",listOf("V1-09","V1-10","V1-11","V1-12"),"RAM·저장장치·cache·가상메모리와 CPU/GPU/NPU, 사용자 공간/커널 공간을 성능과 오류 진단에 연결한다.",listOf("RAM/저장장치","cache","가상메모리","CPU/GPU/NPU","kernel/user space"),"느림의 원인을 ‘성능’ 한 단어로 부르지 말고 어느 자원이 병목인지 측정한다.","측정값 요약은 맡길 수 있지만 병목 결론은 CPU·memory·I/O 지표와 재현 조건을 함께 본다.",55),
        c("V1-C05",5,"권한·터미널·환경변수","TB1-C05",listOf("V1-13","V1-14","V1-15","V1-16"),"권한과 사용자 경계, shell 명령 구조, pipe/redirection, PATH와 환경변수를 안전한 작업의 기본기로 묶는다.",listOf("사용자/권한","shell","pipe/redirection","환경변수","PATH"),"명령 실행 전 현재 위치·대상·권한·환경을 확인하고 비밀값을 코드에 박아 넣지 않는다.","긴 명령은 AI가 만들 수 있지만 실행 전에 경로, 삭제 범위, secret 노출, 권한 상승을 사람이 감사한다.",55),
        c("V1-C06",6,"설치·버전·설정과 의존성","TB1-C06",listOf("V1-17","V1-18","V1-19"),"앱/패키지 설치와 버전 호환성, 의존성, 설정파일과 환경 분리를 재현 가능한 배포 관점에서 이해한다.",listOf("패키지/의존성","버전 호환","lock/재현성","설정 분리","secret"),"‘최신이면 됨’이 아니라 함께 동작하도록 검증된 버전 조합과 환경 차이를 기록한다.","업데이트 후보 조사와 changelog 요약은 AI에게 맡겨도 되지만 실제 빌드/테스트 없이 호환이라고 판정하지 않는다.",50),
        c("V1-C07",7,"로그를 읽고 시간축으로 사건 재구성하기","TB1-C07",listOf("V1-20"),"로그의 수준·시간·component·correlation을 이용해 실패 전후 사건을 재구성하고 근거 없는 추측을 줄인다.",listOf("timestamp","log level","component","correlation","재현 시각"),"오류 문장 하나보다 실패 직전부터 직후까지의 시간축과 최초 원인을 찾는다.","로그 분류와 요약은 AI가 잘하지만 비밀값 마스킹과 실제 첫 실패 지점 확인은 사람이 한다.",40),
        c("V1-C08",8,"포트·DNS·앱/웹/서버 경계","TB1-C08",listOf("V1-21","V1-22","V1-23"),"주소·포트·socket·DNS와 앱/브라우저/서버/cloud의 실행 경계를 이용해 통신 실패를 단계적으로 진단한다.",listOf("IP/host","port/socket","DNS","client/server","native/web/WebView"),"네트워크가 된다는 말도 DNS·연결·TLS·HTTP 중 어디까지 성공했는지 나눠 말한다.","진단 명령 추천은 맡길 수 있지만 실제 target host와 port, DNS 결과를 직접 확인한다.",50),
        c("V1-C09",9,"Android 권한·동기화·백업","TB1-C09",listOf("V1-24","V1-25"),"Android sandbox와 runtime permission, sync와 backup의 차이를 데이터 안전성과 최소 권한 원칙으로 묶는다.",listOf("Android sandbox","runtime permission","최소 권한","sync vs backup","복구 검증"),"동기화는 백업이 아니며, 권한은 기능에 필요한 최소 범위만 주고 복구 가능성은 실제로 시험한다.","권한 목록 정리는 맡길 수 있지만 필요한 이유와 데이터 노출 범위, 복구 시험은 직접 확인한다.",45),
        c("V1-C10",10,"성능 병목과 장애를 계층별로 진단하기","TB1-C10",listOf("V1-26","V1-27"),"느림·멈춤·crash·network failure를 재현하고 CPU/memory/I/O/network/UI 계층별 증거로 최소 원인을 찾는다.",listOf("재현","baseline","CPU/memory/I/O","ANR/crash","network failure"),"한 번에 하나의 가설만 시험하고 변경 전후 같은 조건의 증거를 남긴다.","가설 목록과 테스트 케이스 초안은 맡길 수 있지만 실행되지 않은 테스트를 PASS라고 쓰지 않는다.",60),
        c("V1-C11",11,"종합 시스템 장애 진단 프로젝트","TB1-C11",listOf("V1-28"),"설치부터 화면 표시와 복구까지 전체 경로를 한 장의 시스템 지도로 연결하고 실제 장애 보고서를 완성한다.",listOf("end-to-end map","증거 장부","가설/실험","최소 수정","회귀검증"),"완료의 기준은 ‘고친 것 같음’이 아니라 재현→수정→동일 조건 재검증→증거 보존이다.","보고서 정리와 대안 탐색은 맡길 수 있지만 최종 PASS는 실제 실행 로그와 설치/복구 증거로만 승인한다.",75)
    )

    val practiceLessonIds: Set<String> = chapters.map { it.practiceLessonId }.toSet()
    val allSourceLessonIds: List<String> = chapters.flatMap { it.sourceLessonIds }
    fun chapterById(id: String): TextbookChapter? = chapters.find { it.id == id }
    fun chapterByPracticeLessonId(id: String): TextbookChapter? = chapters.find { it.practiceLessonId == id }

    private fun c(id:String,number:Int,title:String,practice:String,source:List<String>,summary:String,keyConcepts:List<String>,human:String,ai:String,minutes:Int) = TextbookChapter(id,number,title,"textbook/v1/chapter_${number.toString().padStart(2,'0')}.md",practice,source,summary,keyConcepts,human,ai,minutes)
}
