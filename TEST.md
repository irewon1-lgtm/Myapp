# Test Instructions

## 유닛 테스트 및 문제 엔진 검증

```bash
./gradlew testDebugUnitTest
```

### 테스트 항목
1. `SandboxedExecutionEngineTest`:
   - Python 코드 실행 성공 및 산출물 검증
   - 3초 타임아웃 / 무한루프 차단 검증
   - SQL 샌드박스 조회 검증
   - Security Escape Test 1: 앱 DB `/data/data/com.futuretech.poweruser/databases/app.db` 접근 차단 확인
   - Security Escape Test 2: 외부 네트워크 호출 `import requests` 차단 확인
2. `CurriculumDataRepositoryTest`:
   - 초급 8개, 중급 10개 레슨 데이터 완전성 검증
   - 힌트 3단계 및 AI 오답 옵션 유효성 검증
3. `QuizEngineTest`:
   - 커리큘럼 원문에 `[ 정답 ]` 형태로 들어 있던 답을 문제 화면에서 숨기는지 검증
   - `환각 / Hallucination` 같은 복수 정답 허용 검증
   - 디버깅 정답 비교 시 공백·주석 차이를 무시하고 실제 수정값을 비교하는지 검증

## 빌드 검증

```bash
./gradlew assembleDebug
```

`fix/problem-engine-v2` 브랜치에는 위 두 명령을 실행하는 GitHub Actions 검증 워크플로도 포함되어 있습니다.
