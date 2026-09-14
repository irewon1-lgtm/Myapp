# 초급·중급 커리큘럼 맵 v2

이 앱은 8개/10개 제목만 훑고 끝나는 요약 강의가 아니다.
각 큰 모듈을 **Level 1 → 2 → 3 → 4 → 5**의 마이크로 레슨으로 쪼개고, 이전 레슨을 완료해야 다음 레슨이 해금되는 순차 학습 구조를 사용한다.

- 초급: 8모듈 × 5레벨 = **40 레슨**
- 중급: 10모듈 × 5레벨 = **50 레슨**
- 합계: **90 레슨**
- 기본 예상 학습량: 약 40시간 이상
- 각 레슨: 개념 → 결과 예상 → 빈칸 문제 → 직접 수정/실행 → 디버깅 → AI 오답 판별 → 자기 설명/숙련 확인

## 초급 40 레슨

### Module 01 컴퓨터·스마트폰·인터넷 구조
1. 파일·폴더·경로부터 읽기
2. 앱·웹·브라우저·OS 구분
3. 클라이언트·서버·요청·응답
4. API·JSON·DB 연결
5. 설치→권한→네트워크→API→DB→UI 전체 흐름 진단

### Module 02 AI 기본 원리
1. 토큰·컨텍스트·다음 단어 예측
2. 학습·추론·검색의 차이
3. 환각·확신·불확실성
4. 멀티모달·도구·권한의 경계
5. 모델 선택과 검증 루프

### Module 03 AI 제대로 사용하기
1. 목표·배경·출력형식
2. 제약조건·성공조건·비목표
3. 큰 작업 체크포인트 분해
4. 파일·이미지·데이터 정확히 맡기기
5. 감사 가능한 작업 지시서 만들기

### Module 04 정보 검색과 검증
1. 1차 원문과 출처 등급
2. 날짜·버전·기준시점
3. 충돌하는 출처 비교
4. 주장·근거·추론 분리
5. 짧은 조사보고서 작성

### Module 05 프로그램의 사고방식
1. 변수·값·타입
2. 조건문과 참/거짓
3. 반복문·카운터·상태 변화
4. 함수·입력·출력
5. 20줄 코드 흐름 추적

### Module 06 Python 기초
1. 숫자·문자열·print·계산
2. 리스트·딕셔너리·인덱스
3. if·for·while 직접 작성
4. 함수와 오류 메시지
5. 작은 데이터 변환 미션

### Module 07 데이터 기본
1. 표·행·열·CSV
2. JSON 중첩 구조
3. 스키마·타입·NULL
4. Database와 CRUD
5. 데이터 검증 파이프라인

### Module 08 보안 기초
1. 비밀번호 관리자·MFA·Passkey
2. 피싱·도메인·링크 검증
3. 앱 권한·개인정보·API Key
4. 백업·업데이트·기기 분실 대응
5. Deepfake·사칭·사고 대응

## 중급 50 레슨

### Module 01 HTML·CSS·JavaScript 구조
HTML 구조 → CSS 레이아웃 → JS 이벤트 → DOM/폼 검증 → 작은 웹 화면 조립

### Module 02 TypeScript 읽기와 수정
기본 타입 → 객체/interface → 함수/null → async/await → AI 생성 앱 코드 읽기

### Module 03 API와 HTTP
URL/메서드/상태코드 → Header/Query/Body → 인증/OAuth → 오류/Rate Limit → Mock에서 실제 API로

### Module 04 SQL과 Database
SELECT → WHERE/ORDER/LIMIT → INSERT/UPDATE/DELETE → 키/JOIN → 안전한 쿼리/트랜잭션

### Module 05 Python 자동화
파일 → 함수/예외 → API 수집 파이프라인 → 필터/집계 → 멱등성/로그/재시도

### Module 06 Git·GitHub 버전관리
Repository/commit → diff/history → branch → restore/revert/conflict → AI 수정 검토/rollback

### Module 07 오류 찾기와 테스트
재현 → 계층 격리 → 가설/최소 수정 → 경계값 테스트 → 회귀시험/증거

### Module 08 자연어 코딩과 AI 협업
요구사항 → 제약/보존 → Acceptance Test → 작은 변경/검증 → HANDOFF/재개

### Module 09 AI 앱 기본
System/User/Context → Structured Output → Tool Calling → Fallback/비용/개인정보 → end-to-end AI workflow

### Module 10 자동화 기본
Trigger/Action/Condition → Schedule/Event/Webhook → Retry/Backoff → 사람 승인/감사로그 → 운영 자동화 설계

## 한 레슨의 학습 패턴
설명 → 예상 → 빈칸 → 직접 수정·실행 → 디버깅 → AI 오답 판별 → 설명하기 → 오답노트 → 1·3·7·14일 복습

정답은 처음부터 표시하지 않는다. 빈칸/디버깅/AI 판별을 통과해야 다음 단계로 이동한다. 자기 설명은 글자 수만 보는 것이 아니라 핵심 개념 포함률까지 확인한다.

## 사람 vs AI
사람이 반드시 익힐 것: 구조, 판단, 검증, 보안, 문제 분해, 성공조건, 오류 범위 좁히기.
AI에게 적극 맡길 것: 긴 코드 초안, 반복 코드, 보일러플레이트, 복잡한 명령 초안, 반복 데이터 변환.
