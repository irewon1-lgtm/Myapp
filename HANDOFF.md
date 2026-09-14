# HANDOFF Document

## 현재 상태
- **명세 판정**: `SPEC_CLEAN_PASS`
- **앱 판정**: `COMPLETE`
- **빌드 아티팩트**:
  - Release APK: `app/build/outputs/apk/release/app-release.apk`
  - SHA-256: `52456542edff4a26427f7982fdd18587c345eb9c9bd615e3d71111d492327826`
  - 소스 ZIP: `source_code.zip`
  - SHA-256: `6dcd3ec313edefa6a3dba2cf3604f464e0b72c097de6bbc8f02de5d1da287a07`

## 주요 모듈 구성
1. `com.futuretech.poweruser.sandbox.SandboxedExecutionEngine`:
   - 파이썬 마이크로 인터프리터, SQL 로컬 SQLite, TypeScript 변환 및 안전한 HTML 미리보기 제공.
2. `com.futuretech.poweruser.data`:
   - `CurriculumDataRepository.kt`: 초급 8개 + 중급 10개 레슨.
   - `AppDatabase.kt`: Room DB (진도, 복습, 오류노트).
   - `SecureKeyStorage.kt`: EncryptedSharedPreferences 키 보관.
3. `com.futuretech.poweruser.ui`:
   - `MainHomeScreen.kt`, `LessonDetailScreen.kt`, `ProjectScreens.kt`, `SecondaryScreens.kt`.

## 다음 작업자 전달 사항
- 고급 과정(RAG, MCP, 멀티에이전트 등)은 별도 앱으로 분리 설계되어 있습니다.
- 새로운 레슨을 추가할 경우 `CurriculumDataRepository.kt`에 객체를 등록하면 UI 및 복습 시스템에 자동 반영됩니다.
