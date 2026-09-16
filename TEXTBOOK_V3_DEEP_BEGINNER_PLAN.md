# TEXTBOOK V3 · 완전초급 심화 교재 최종 계약

## 1. 사용자 요구를 숫자로 고정

기존 V2 `### LESSON` 총수: **590개**

최종 learner-facing V3 LESSON: **26개**

감소율: **95.6%**

사용자 최소 요구인 `1/3 이상 감소`보다 훨씬 크게 줄이되, 내용을 삭제해서 줄이는 것이 아니라 **직접 연결되는 짧은 주제를 한 개의 큰 책형 LESSON으로 합친다.**

계층/사용 경험은 유지한다.

```text
TRACK
↓
LESSON 선택/좌우 이동
↓
LESSON 안에서 관련 BLOCK을 책의 소단원처럼 연속 학습
↓
BLOCK 내부 step-by-step 소제목
↓
짧은 회상
↓
다음 LESSON
```

V2 원본은 보존하고 V3 asset을 별도로 둔다.

`main`, GitHub Actions, APK 배포, 운영환경은 사용자 검토/승인 전 실행하지 않는다.

## 2. TRACK별 최종 learner-facing LESSON 수

| TRACK | V2 authored LESSON | V3 source BLOCK | 최종 learner LESSON |
|---|---:|---:|---:|
| 01 컴퓨터와 프로그래밍의 언어 | 31 | 5 | 2 |
| 02 프로그래밍 사고와 문법 | 70 | 8 | 4 |
| 03 자료구조와 알고리즘 | 51 | 7 | 2 |
| 04 웹 화면과 브라우저 | 43 | 6 | 3 |
| 05 JavaScript와 TypeScript | 66 | 8 | 4 |
| 06 인터넷·네트워크·API | 49 | 7 | 2 |
| 07 서버와 백엔드 | 48 | 6 | 2 |
| 08 데이터베이스 | 63 | 8 | 2 |
| 09 보안과 데이터 보호 | 39 | 3 | 1 |
| 10 오류·테스트·Git·빌드·배포 | 53 | 4 | 2 |
| 11 소프트웨어 설계와 종합 프로젝트 | 77 | 5 | 2 |
| **합계** | **590** | **67** | **26** |

## 3. 최종 LESSON 묶음

### TRACK 01 · 2 LESSON
1. 컴퓨터·파일·문자는 어떻게 데이터가 되는가
2. 프로그램과 코드를 읽고 실행하는 첫걸음

### TRACK 02 · 4 LESSON
1. 문제를 나누고 값과 변수로 표현한다
2. 입력·조건·반복으로 프로그램의 흐름을 만든다
3. 여러 값과 함수를 이용해 프로그램을 구조화한다
4. 파일·모듈·오류처리까지 작은 프로그램으로 연결한다

### TRACK 03 · 2 LESSON
1. 배열·연결구조·해시·트리로 데이터를 담는 방법
2. 그래프·검색·정렬·복잡도로 문제를 푸는 방법

### TRACK 04 · 3 LESSON
1. 브라우저와 HTML로 의미 있는 화면 구조를 만든다
2. CSS와 반응형으로 화면을 배치하고 꾸민다
3. DOM·이벤트·DevTools로 화면을 움직이고 고친다

### TRACK 05 · 4 LESSON
1. JavaScript의 값·참조·함수가 움직이는 방식
2. 데이터 가공과 비동기의 출발점
3. Promise·async/await·event loop를 시간 순서로 이해한다
4. 여러 비동기 작업과 TypeScript로 실제 앱을 안전하게 만든다

### TRACK 06 · 2 LESSON
1. 내 기기에서 서버까지 IP·DNS·TCP·TLS의 길을 따라간다
2. HTTP·MIME·API·CORS까지 실제 통신 문제를 해결한다

### TRACK 07 · 2 LESSON
1. 서버가 요청을 받고 검증해 업무 로직으로 보내는 전체 흐름
2. 인증·캐시·큐·동시성·관측성으로 운영 서버를 만든다

### TRACK 08 · 2 LESSON
1. DB 구조·SQL·집계·무결성으로 데이터를 올바르게 저장한다
2. 관계·JOIN·index·transaction으로 DB를 실제 서비스에 연결한다

### TRACK 09 · 1 LESSON
1. 보안을 처음부터 끝까지: 신뢰·암호·웹 공격을 연결한다

### TRACK 10 · 2 LESSON
1. 버그 조사와 테스트로 수정의 증거를 만든다
2. Git에서 빌드·배포·rollback까지 변경을 추적한다

### TRACK 11 · 2 LESSON
1. 요구사항에서 모듈·아키텍처까지 시스템의 뼈대를 설계한다
2. 상태·확장·장애대응을 종합 프로젝트로 연결한다

## 4. LESSON 편집 계약

각 source BLOCK은 정의 카드가 아니라 최소한 다음 흐름으로 쓴다.

1. **실제 상황부터** — 왜 필요한지 먼저 보여준다.
2. **한국어로 먼저** — 영어 전문용어는 상황을 이해한 뒤 이름으로 붙인다.
3. **비유와 그림** — 데이터/실행 상태가 어떻게 움직이는지 눈으로 보여준다.
4. **한 단계씩** — 아직 배우지 않은 용어를 선행 사용하지 않는다.
5. **작은 코드** — 실행 가능한 최소 예부터 보여준다.
6. **한 줄씩 해석** — 문법을 건너뛰지 않는다.
7. **조금씩 확장** — 새 기능을 단계별로 추가한다.
8. **실수/실패 사례** — 틀린 코드와 왜 틀렸는지 함께 설명한다.
9. **실제 앱 연결** — 화면·서버·DB·네트워크에서 어디에 쓰이는지 연결한다.
10. **앞뒤 개념 연결** — 다음 전문용어가 왜 필요한지 자연스럽게 이어 준다.
11. **책을 덮고 확인** — 자기 말로 설명하는 회상 질문을 둔다.

## 5. 분량 계약

정적 분량 지표:

```text
새 TRACK bytes / 최종 visible lesson count
-------------------------------------------
기존 V2 TRACK bytes / 기존 authored lesson count
```

TRACK별 결과는 현재 **25.12x ~ 43.99x**다.

추가 JVM 품질 gate는 더 엄격하게:

```text
각 실제 V3 learner-facing LESSON weightedLength
>=
같은 TRACK V2 reader page 평균 weightedLength × 20
```

을 검사하도록 작성한다.

이 JVM gate는 전체 Gradle suite가 실제 실행되기 전까지 PASS라고 보고하지 않는다.

## 6. 회귀 방지 품질 gate

- V3 asset 11개 존재.
- source BLOCK 수 3~8 범위.
- 최종 learner-facing LESSON 수 정확히 26.
- 모든 authored BLOCK이 merged LESSON 내부에 남아 있어야 함.
- source의 작은 `LESSON 01 ·` 라벨은 reader에서 별도 page가 되지 않아야 함.
- source BLOCK은 충분한 설명량과 step subsection을 가져야 함.
- 반복 문단 padding 금지.
- TODO/TBD/준비중 금지.
- async/MIME/integrity 같은 선행 개념 순서 회귀 금지.
- synthetic/non-V3 split 동작은 기존 방식으로 보존.

## 7. 현재 승인 경계

허용된 작업:

```text
별도 GitHub branch에서 V3 source/코드/test 수정
무료·비파괴 로컬 정적 검증
```

아직 실행하지 않는 작업:

```text
main merge
GitHub Actions
APK build/release 배포
운영환경 변경
```

사용자가 V3 원고/구조를 먼저 검토한 뒤 다음 실행 승인을 받는다.
