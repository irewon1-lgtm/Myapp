# Test Instructions

## 자동 검증 명령

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

## 현재 자동 테스트 범위

1. `SandboxedExecutionEngineTest`
   - Python 학습용 실행기 성공/오류
   - 무한루프/시간 제한
   - SQL 연습 DB
   - 앱 DB/KeyStore/민감 경로 접근 차단
   - 임의 외부 네트워크 호출 차단

2. `CurriculumDataRepositoryTest`
   - 초급 **40개** 마이크로 레슨
   - 중급 **50개** 마이크로 레슨
   - 초급 8모듈 / 중급 10모듈
   - 각 모듈 Level 1→5 정확히 존재
   - 레슨 ID/순서 중복 없음
   - 모든 레슨에 설명, 목표, 코드, 실습, 빈칸, 디버깅, AI 판별 문제, 설명 키워드 포함
   - 최소 예상 학습시간 값 검증

3. `QuizEngineTest`
   - 빈칸 정답이 문제 화면에서 숨겨지는지
   - 복수 허용 정답 채점
   - 코드 채점 시 공백/주석 정규화
   - 자기 설명이 단순 글자 수가 아니라 핵심개념 포함률을 만족해야 통과하는지
   - 짧은 키워드 나열과 길기만 한 무관 문장은 FAIL 처리

## 최근 실제 CI 증거

최신 `fix/problem-engine-v2` head `3bb133dc4d59b6763074dc96126b574ea29a9282`에 대해 GitHub Actions run **34909167948**에서:

- `testDebugUnitTest` — **PASS**
- `assembleDebug` — **PASS**

이 결과는 컴파일/Unit Test/Debug APK 빌드 검증이며, 실기 설치·실행·Release APK·극한60/CLEAN25 전체 완료를 의미하지 않습니다.
