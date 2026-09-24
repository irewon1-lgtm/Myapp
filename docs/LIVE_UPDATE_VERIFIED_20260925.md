# 챗북 LIVE 업데이트 완료 검증 — 2026-09-25 KST

## 완료 범위
기존 https://chatbook-library-20260923.netlify.app 주소를 유지했다. 교재·챕터·본문·UI JS/CSS·현재 제작규격은 GitHub의 검증된 LIVE 묶음으로 갱신한다. 일상 업데이트 workflow에는 Netlify 배포 단계가 없다. 기존 자동 production/migration workflow들은 docs/disabled-workflows 아래 실행 불가능한 텍스트로 보관했다.

YouTube URL은 ChatGPT에 보내며, ChatGPT가 실제 /engine-spec.json을 읽고 교재를 작성한다. 앱에 유료 생성 API를 추가하지 않았다. 기존 13권·제작규격·sync 함수는 보존했다. 실제 사용자 연결 코드나 동기화 기록은 검증에 사용하지 않았다.

## 실제 무배포 갱신 증거
- 일반 업데이트 실행: https://github.com/irewon1-lgtm/Myapp/actions/runs/36031004052
- 소스: bc7419849304f3f79f55505d7c687fc86bd3e6e8
- 변경 전 LIVE: 888f87933aaefd8d03b7ebbf31fa7e553ad57229
- 변경 후 LIVE: bd4f2e3181eaef3b97a39d30d672c0d8ff47e20b
- 갱신 전후 Netlify 배포 ID: 6ab5552b5e8f84e70b848788 (동일)
- 새로운 HTML 확인표시 github-only-20260925가 실제 운영 HTTPS와 Chromium에서 확인됨.
- 일반 업데이트의 Netlify production 배포: 0회.
- 실제 운영 검사는 2026-09-24T16:59:03Z경 종료. 이후 Netlify 연결 도구 get-project로 같은 배포 ID를 재확인했다.

## 실행한 검사
- Node 테스트: 191/191 통과, 실패·건너뛰기 0. 구문 검사와 빌드 통과.
- 실제 Chromium + 격리된 오류 입력: 42/42 통과. 깨진 JS, 변조 파일, 부분 다운로드, 잘못된 채널, 이전 버전, 비호환 shell, 새 책/본문/챕터/UI 갱신, 합성 메모·책갈피·개인 추가 도서 보존, 오프라인 재실행 검사 포함.
- 실제 운영 HTTPS + 새 Chromium 프로필: 22/22 통과. 새 LIVE 선택, HTML 확인표시, 고정 파일 경로, 현재 규격, 네 가지 화면 폭, 합성 메모, 주요 화면, 오프라인 준비·재실행·기록 보존 검사 포함.
- 최종 아티팩트: 10822240734, chatbook-live-verification.

## 초기 전환 비용 기록 및 한계
최초 예상 1회와 달리 실제 Netlify production 전환은 총 2회였다. 첫 설치 후 실제 운영 환경에서 고정 파일 경로 404를 발견해 수정 배포 1회를 추가했다. 원래 영수증과 수정 영수증은 .deployment/live-bootstrap-receipt.json, .deployment/live-routing-correction-receipt.json에 별도로 보존했다. 총 2회를 0회/1회라고 표현하지 않는다.

현재 Pro 결제는 변경하지 않았다. Production 재배포 없이 업데이트하더라도 호스팅·트래픽·함수 실행 사용량은 남는다. 고정 서버/로더/보안 계약 변경은 별도 검토와 승인 없이 배포하지 않는다.

이 검증은 실제 Chromium 및 운영 HTTPS 검사이지 물리적 Galaxy 검사가 아니다. 모든 미래 오류가 없다는 보장이 아니며, 실제 사용자 동기화 기록 전체를 대조한 것도 아니다. 다운로드/시작 실패 시 기존 정상본을 사용하도록 보호했다. 처음부터 인터넷 없이 쓰려면 사전 온라인 저장이 필요하다. 기존 앱/탭을 닫았다 다시 열어 새 방식을 사용한다. 업그레이드를 이유로 사이트 데이터나 메모를 삭제하지 않는다.
