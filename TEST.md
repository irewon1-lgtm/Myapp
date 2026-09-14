# Test Instructions

## 유닛 테스트 및 샌드박스 보안 검증

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
