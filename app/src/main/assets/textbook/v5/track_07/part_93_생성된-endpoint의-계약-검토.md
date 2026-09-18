# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 07 · AI와 함께 백엔드를 만들되 사람이 계약과 검증을 통제하기

### LESSON 03 · 생성된 endpoint의 계약 검토

## CHAPTER 01 · 생성된 endpoint의 계약 검토 — 쉬운 말에서 정확한 계약까지

**출발 개념.** endpoint가 실행된다는 것과 API 계약이 올바른 것은 다르다.

**아주 쉬운 사건.** 생성 endpoint가 200만 반환한다. 이 사건에서는 먼저 **status·error·idempotency 계약을 검토한다**.

**왜 필요한가.** 정상 동작은 method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다. 반대로 happy path curl 하나만 성공해 완료 처리한다.

**암기:** `생성된 endpoint의 계약 검토`의 역할 한 줄.

**직접 이해:** `생성된 endpoint의 계약 검토`의 입력·상태·결과 경계.

**AI 위임 가능:** `생성된 endpoint의 계약 검토` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `생성 endpoint가 200만 반환한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 유효·경계·잘못된 입력별 실제/예상 status와 body를 비교한다.

## CHAPTER 02 · 생성된 endpoint의 계약 검토 — 아주 쉬운 예를 한 단계씩 해석

T07-P93: `생성 endpoint가 200만 반환한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 생성 endpoint가 200만 반환한다 | T07-P93 외부 입력 | T07-P93: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | status·error·idempotency 계약을 검토한다 | T07-P93 판단 기준 | T07-P93/CH08 관측표와 대조 |
| 정상 경로 | T07-P93/CH03 M1→M5 | 생성된 endpoint의 계약 검토: 완료 시점을 단계별로 분리 | T07-P93: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P93/CH06 happy path curl 하나만 성공해 완료 처리한다. | T07-P93: 깨진 계약 하나를 특정 | 생성된 endpoint의 계약 검토: 증상과 원인을 분리 |
| 재검증 | T07-P93/CH10 직접 실행 | T07-P93: 예상값 T07-P093 410 기록 | 생성된 endpoint의 계약 검토: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P93/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P93/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · 생성된 endpoint의 계약 검토 — 내부 메커니즘과 상태 전이

method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 생성 endpoint가 200만 반환한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | status·error·idempotency 계약을 검토한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 유효·경계·잘못된 입력별 실제/예상 status와 body를 비교한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | test case 표에서 예상과 실제가 다른 항목을 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`생성된 endpoint의 계약 검토` 흐름을 framework 이름 없이 설명한다.

막히면 `생성 endpoint가 200만 반환한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · 생성된 endpoint의 계약 검토 — 실전 경계 A

**경계 A.** 생성된 endpoint는 method/path/status/body schema를 표로 꺼내 OpenAPI/spec와 비교하고 handler 안 코드가 많아도 계약 누락을 먼저 찾는다.

T07-P93/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P93에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P93/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P93/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P93): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P93/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P93-A1-885 | T07-P93 조건 | 생성된 endpoint는 method/path/status/body schema를 표로 꺼내 OpenAPI/spec와 비교하고 handler 안 코드가 많아도 계약 누락을 먼저 찾는다. |
| T07-P93-A2-886 | T07-P93 변화점 | T07-P93/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P93-A3-887 | T07-P93 반례 | T07-P93/CH06 대표 실패와 A 위반을 구별 |
| T07-P93-A4-888 | T07-P93 근거 | T07-P93/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P93-A5-889 | T07-P93 재실험 | 생성 endpoint가 200만 반환한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · 생성된 endpoint의 계약 검토 — 실전 경계 B

**경계 B.** unknown field·default·coercion 정책을 AI가 framework 기본값에 맡겼는지 확인해 client가 예상하지 못한 값이 domain으로 흘러가지 않게 한다.

T07-P93/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P93에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P93/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P93/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P93): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P93/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P93-B1-916 | T07-P93 조건 | unknown field·default·coercion 정책을 AI가 framework 기본값에 맡겼는지 확인해 client가 예상하지 못한 값이 domain으로 흘러가지 않게 한다. |
| T07-P93-B2-917 | T07-P93 독립성 | T07-P93/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P93-B3-918 | T07-P93 상태 | T07-P93/CH03 before·after 위치를 다시 지정 |
| T07-P93-B4-919 | T07-P93 반증 | T07-P93/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P93-B5-920 | T07-P93 적용 | 생성된 endpoint의 계약 검토의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · 생성된 endpoint의 계약 검토 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **happy path curl 하나만 성공해 완료 처리한다.**

아래 여섯 사례는 T07-P93의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P93-F01 | T07-P93: 대표 실패 | T07-P93: T07-P93/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P93: 현상만 보고 원인을 확정 | T07-P93/CH08 evidence map에서 상태를 대조 |
| T07-P93-F02 | T07-P93: 경계 A 누락 | T07-P93: T07-P93/CH04 경계 A 위반 입력 | T07-P93: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P93/CH08 evidence map에서 상태를 대조 |
| T07-P93-F03 | T07-P93: 경계 B 누락 | T07-P93: T07-P93/CH05 경계 B 위반 입력 | T07-P93: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P93/CH08 evidence map에서 상태를 대조 |
| T07-P93-F04 | T07-P93: 복구 경계 C 누락 | T07-P93: T07-P93/CH07 경계 C 복구 조건 | T07-P93: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P93/CH08 evidence map에서 상태를 대조 |
| T07-P93-F05 | T07-P93: 운영 경계 D 누락 | T07-P93: T07-P93/CH09 경계 D 운영 조건 | T07-P93: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P93/CH08 evidence map에서 상태를 대조 |
| T07-P93-F06 | T07-P93: 증거 없는 결론 | T07-P93: T07-P93/CH02 첫 판단만 존재 | T07-P93: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P93/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P93/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · 생성된 endpoint의 계약 검토 — 복구 가능한 상태와 수명

**경계 C.** POST command는 timeout 뒤 retry할 수 있는지, idempotency key가 필요한지, duplicate가 어떤 status/result를 받는지 계약에 포함한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P93에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P93/CH08 evidence map을 본다. 복구 후에는 T07-P93/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P93에서 이미 확정된 side effect는 T07-P93/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P93-R1-978 | T07-P93 중단 직전 | T07-P93/CH03에서 이미 확정된 상태만 표시 |
| T07-P93-R2-979 | T07-P93 재시작 직후 | POST command는 timeout 뒤 retry할 수 있는지, idempotency key가 필요한지, duplicate가 어떤 status/result를 받는지 계약에 포함한다. |
| T07-P93-R3-980 | T07-P93 재검증 | T07-P93/CH08 근거로 중복·누락 여부 확인 |
| T07-P93-R4-981 | T07-P93 재실행 | T07-P93/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · 생성된 endpoint의 계약 검토 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 생성 endpoint가 200만 반환한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | status·error·idempotency 계약을 검토한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | happy path curl 하나만 성공해 완료 처리한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 유효·경계·잘못된 입력별 실제/예상 status와 body를 비교한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`생성된 endpoint의 계약 검토` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · 생성된 endpoint의 계약 검토 — 운영 한계와 종료 조건

**경계 D.** 필드 추가·enum 추가·error shape 변경이 이미 배포된 client를 깨는지 contract test로 확인해 ‘server가 compile되니 호환’이라는 결론을 막는다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P93/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P93/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P93 과제: 경계 D와 T07-P93/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P93-O1-43 | synthetic-load=83 | T07-P93/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P93-O2-44 | synthetic-budget=193ms | T07-P93 timeout과 unknown outcome을 분리 |
| T07-P93-O3-45 | T07-P93 종료 | 필드 추가·enum 추가·error shape 변경이 이미 배포된 client를 깨는지 contract test로 확인해 ‘server가 compile되니 호환’이라는 결론을 막는다. |
| T07-P93-O4-46 | T07-P93 완화 | T07-P93/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · 생성된 endpoint의 계약 검토 — 직접 실행하는 작은 모델

`생성된 endpoint의 계약 검토` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P093 410`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P093";
let remaining = 800;
for (const cost of [120, 180, 90]) remaining -= cost;
console.log(marker, remaining);
```

기준 출력: `T07-P093 410`.

`생성된 endpoint의 계약 검토`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P93-L1-75 | constmarker="T07-P093"; | T07-P93 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P93-L2-76 | letremaining=800; | T07-P93 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P93-L3-77 | for(constcostof[120,180,90])remaining-=cost; | T07-P93 계산 지점; 수정 전후 결과가 갈리는 위치 |
| T07-P93-L4-78 | console.log(marker,remaining); | T07-P93 출력 관측점; 예상 `T07-P093 410`와 비교 |
| T07-P93-LX-164 | T07-P93 실행 기록 | T07-P93 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · 생성된 endpoint의 계약 검토 — 한 부분만 수정하고 다시 예측

수정 과제: **두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다**.

수정 전은 `T07-P093 410`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P93/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P93-D1-105 | 기준 `T07-P093 410` | T07-P93 수정 전 실행을 먼저 재현 |
| T07-P93-D2-106 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | T07-P93 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P93-D3-107 | T07-P93 새 예측 | T07-P93 실행 전에 출력·상태를 먼저 기록 |
| T07-P93-D4-108 | T07-P93 재실행 | T07-P93/CH10 실제값과 새 예측을 대조 |
| T07-P93-D5-109 | T07-P93 반례 | T07-P93/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P93-D6-110 | T07-P93 근거 | T07-P93/CH08 상태가 설명과 일치해야 완료 |
| T07-P93-D7-111 | T07-P93 이유 | T07-P93 변경 이유를 생성된 endpoint의 계약 검토 계약과 연결해 설명 |

## CHAPTER 12 · 생성된 endpoint의 계약 검토 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: 생성된 endpoint의 계약 검토 | endpoint가 실행된다는 것과 API 계약이 올바른 것은 다르다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P93/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | happy path curl 하나만 성공해 완료 처리한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 유효·경계·잘못된 입력별 실제/예상 status와 body를 비교한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P93/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P93/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P93/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P93/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `생성된 endpoint의 계약 검토` 실행 코드 수정 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | `생성된 endpoint의 계약 검토` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P93/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P93/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · 생성된 endpoint의 계약 검토 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | endpoint가 실행된다는 것과 API 계약이 올바른 것은 다르다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P93/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P93/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P93/CH10 실행용 boilerplate·test 후보 | T07-P93: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P093 410` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P93/CH02의 판단 기준과 T07-P93/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P93-AI1-167 | T07-P93 사람 결정 | T07-P93 업무 의미·허용 위험·완료 기준 소유 |
| T07-P93-AI2-168 | T07-P93 AI 초안 | T07-P93/CH10 boilerplate·test 후보까지만 위임 |
| T07-P93-AI3-169 | T07-P93 검증 | T07-P93/CH06 반례와 T07-P93/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · 생성된 endpoint의 계약 검토 — 경계 조합 실험 8개

T07-P93: T07-P93/CH04~T07-P93/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P93의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-K01 | T07-P93: 경계 A | T07-P93: 경계 B | T07-P93: RECURRENCE[occurrence=3; interval_s=216; mitigation_applied=1] / sample=62 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + RECURRENCE_TIMELINE |
| T07-P93-K02 | T07-P93: 경계 A | T07-P93: 경계 C | T07-P93: LARGE_INPUT[body_kb=2088; limit_kb=640; parsed=0] / sample=69 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + SIZE_LIMIT |
| T07-P93-K03 | T07-P93: 경계 A | T07-P93: 경계 D | T07-P93: OWNER_SPOOF[actor=A2; payload_owner=B3; auth_owner=A2] / sample=76 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + AUTHZ_POLICY |
| T07-P93-K04 | T07-P93: 경계 B | T07-P93: 경계 C | T07-P93: REORDER[in_seq=7,5,6; applied_version=4] / sample=83 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + SEQUENCE_STATE |
| T07-P93-K05 | T07-P93: 경계 B | T07-P93: 경계 D | T07-P93: RECOVERY_SCOPE[selected=39; expected=7; backup=1; dry_run=1] / sample=90 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + RECOVERY_AUDIT |
| T07-P93-K06 | T07-P93: 경계 C | T07-P93: 경계 D | T07-P93: REPLAY[key=cmd-093-06; attempts=2; response_seen=0] / sample=97 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + IDEMPOTENCY_RECORD |
| T07-P93-K07 | T07-P93: 경계 A | T07-P93: 경계 B+C | T07-P93: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=15 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + CLIENT_VERSION |
| T07-P93-K08 | T07-P93: 경계 B | T07-P93: 경계 C+D | T07-P93: CONCURRENT_WRITE[actors=2; base_version=4; writes=2; gap_ms=39] / sample=22 | T07-P93: 먼저 깨지는 경계를 판정 | T07-P93: T07-P93/CH08 + STATE_VERSION |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P93/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · 생성된 endpoint의 계약 검토 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P93와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-B02 | T07-P93: AI에게 구현을 맡기기 전 spec 쓰기 | T07-P93: 구현 요청에 “알아서 안전하게”만 적혀 있다 | T07-P93: spec의 빈 결정을 명시한다 | T07-P93: T07-P93/CH08 증거와 형제 LESSON 증거를 분리 | T07-P93: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P93-B04 | T07-P93: 생성된 persistence 코드와 migration 경계 검토 | T07-P93: AI migration이 column을 바로 삭제한다 | T07-P93: expand-migrate-contract로 위험을 줄인다 | T07-P93: T07-P93/CH08 증거와 형제 LESSON 증거를 분리 | T07-P93: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P93-B01 | T07-P93: AI가 만든 backend 코드를 읽는 순서 | T07-P93: AI가 새 endpoint 8개를 만들었다 | T07-P93: entry point에서 side effect까지 data flow를 읽는다 | T07-P93: T07-P93/CH08 증거와 형제 LESSON 증거를 분리 | T07-P93: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P93-B05 | T07-P93: 생성된 인증·권한 코드 검토 | T07-P93: auth middleware는 있지만 BOLA test가 없다 | T07-P93: route coverage와 object permission을 확인한다 | T07-P93: T07-P93/CH08 증거와 형제 LESSON 증거를 분리 | T07-P93: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P93-B06 | T07-P93: 생성된 retry·async 코드를 검토 | T07-P93: AI가 모든 오류에 retry를 붙였다 | T07-P93: idempotency와 deadline을 먼저 검토한다 | T07-P93: T07-P93/CH08 증거와 형제 LESSON 증거를 분리 | T07-P93: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · 생성된 endpoint의 계약 검토 — 선택형 실패 주입 6개

T07-P93: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P93 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-X01 | T07-P93: 소유권 위조 | T07-P93: tenant/owner가 payload에 포함됨; sample=906 | T07-P93: 식별 정보와 권한 근거 | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: authenticated context·resource owner·policy result |
| T07-P93-X02 | T07-P93: 순서 역전 | T07-P93: event가 원래 순서와 반대로 도착; sample=923 | T07-P93: 수신 순서와 업무 순서 | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: version/sequence·dedupe id·applied state |
| T07-P93-X03 | T07-P93: 복구 범위 | T07-P93: 복구 script 대상이 예상보다 큼; sample=940 | T07-P93: 진단과 destructive recovery | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: selected ids/count·backup·audit trail |
| T07-P93-X04 | T07-P93: 재전송 | T07-P93: 응답 유실 뒤 같은 command가 다시 도착함; sample=957 | T07-P93: 중복 side effect 여부 | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P93-X05 | T07-P93: 구버전 client | T07-P93: 한 단계 이전 schema가 요청됨; sample=974 | T07-P93: 호환 입력과 breaking change | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: schema version·실제 client 분포·contract test |
| T07-P93-X06 | T07-P93: 동시 변경 | T07-P93: 두 actor가 같은 resource를 수정함; sample=991 | T07-P93: lost update 또는 conflict | T07-P93: T07-P93/CH02 판단과 별도 기록 | T07-P93: version·affected rows·lock/wait 기록 |

## CHAPTER 17 · 생성된 endpoint의 계약 검토 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P93에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-E01 | T07-P93: 대표 실패를 원인으로 착각 | T07-P93: T07-P93/CH06 실패 case를 다른 입력으로 재현 | T07-P93: 현상과 원인을 같은 것으로 봄 | T07-P93: T07-P93/CH06 대표 실패와 T07-P93/CH08 증거를 다시 대조 | T07-P93: T07-P93/CH08 |
| T07-P93-E02 | T07-P93: 경계 A 생략 | T07-P93: T07-P93/CH04의 조건 하나를 반대로 설정 | T07-P93: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P93: T07-P93/CH04를 새 입력에 적용 | T07-P93: T07-P93/CH08 |
| T07-P93-E03 | T07-P93: 경계 B 생략 | T07-P93: T07-P93/CH05의 조건 하나를 반대로 설정 | T07-P93: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P93: T07-P93/CH05를 새 입력에 적용 | T07-P93: T07-P93/CH08 |
| T07-P93-E04 | T07-P93: 복구 상태 혼동 | T07-P93: T07-P93/CH07에서 처리 중단을 주입 | T07-P93: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P93: T07-P93/CH07에서 수명 경계를 다시 표시 | T07-P93: T07-P93/CH08 |
| T07-P93-E05 | T07-P93: 운영 한계 누락 | T07-P93: T07-P93/CH09에서 부하 또는 drain 조건을 변경 | T07-P93: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P93: T07-P93/CH09의 종료 조건을 다시 작성 | T07-P93: T07-P93/CH08 |
| T07-P93-E06 | T07-P93: 증거 없는 성공 판정 | T07-P93: T07-P93/CH08에서 증거 하나를 숨김 | T07-P93: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P93: T07-P93/CH08에서 독립 증거 둘을 선택 | T07-P93: T07-P93/CH08 |

## CHAPTER 18 · 생성된 endpoint의 계약 검토 — synthetic 관측값 판독 6개

T07-P93: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P93의 숫자 하나만으로 원인을 단정하지 않고 T07-P93/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-O01 | T07-P93/changed_files | 735 | T07-P93: 변경 파일 수 | T07-P93: 축=구버전 client; 원인 확정 금지 | T07-P93: CLIENT_VERSION + T07-P93/CH08 |
| T07-P93-O02 | T07-P93/dependency_count | 752 | T07-P93: 추가 dependency 수 | T07-P93: 축=동시 변경; 원인 확정 금지 | T07-P93: STATE_VERSION + T07-P93/CH08 |
| T07-P93-O03 | T07-P93/test_failures | 769 | T07-P93: 실패 test 수 | T07-P93: 축=unknown outcome; 원인 확정 금지 | T07-P93: PROVIDER_RESULT + T07-P93/CH08 |
| T07-P93-O04 | T07-P93/eval_passes | 786 | T07-P93: 통과 eval 수 | T07-P93: 축=과부하; 원인 확정 금지 | T07-P93: QUEUE_PRESSURE + T07-P93/CH08 |
| T07-P93-O05 | T07-P93/p99_ms | 803 | T07-P93: 변경 후 p99 | T07-P93: 축=sampling; 원인 확정 금지 | T07-P93: TRACE_METRIC_CROSSCHECK + T07-P93/CH08 |
| T07-P93-O06 | T07-P93/cost_units | 820 | T07-P93: resource/cost 단위 | T07-P93: 축=AI 복잡도; 원인 확정 금지 | T07-P93: DIFF_EVAL_ROLLBACK + T07-P93/CH08 |

## CHAPTER 19 · 생성된 endpoint의 계약 검토 — 선택형 코드 리뷰 6질문

T07-P93: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P93에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-R01 | T07-P93: 입력 경계 | T07-P93: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P93: CONCURRENT_WRITE[actors=2; base_version=4; writes=2; gap_ms=32] | T07-P93: T07-P93/CH04 | T07-P93: STATE_VERSION |
| T07-P93-R02 | T07-P93: 순서 | T07-P93: old/new event 순서가 바뀌어도 안전한가 | T07-P93: UNKNOWN_OUTCOME[timeout_ms=286; provider_state=UNKNOWN; lookup_id=p09301] | T07-P93: T07-P93/CH05 | T07-P93: PROVIDER_RESULT |
| T07-P93-R03 | T07-P93: retry | T07-P93: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P93: OVERLOAD[rps=791; p99_ms=822; queue=30] | T07-P93: T07-P93/CH06 | T07-P93: QUEUE_PRESSURE |
| T07-P93-R04 | T07-P93: 민감정보 | T07-P93: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P93: SAMPLING[sample_rate=81%; trace_present=1; metric_present=1] | T07-P93: T07-P93/CH07 | T07-P93: TRACE_METRIC_CROSSCHECK |
| T07-P93-R05 | T07-P93: 입력 경계 | T07-P93: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P93: AI_COMPLEXITY[deps_added=1; cache_layer=1; rollback_plan=0] | T07-P93: T07-P93/CH04 | T07-P93: DIFF_EVAL_ROLLBACK |
| T07-P93-R06 | T07-P93: 순서 | T07-P93: old/new event 순서가 바뀌어도 안전한가 | T07-P93: RECURRENCE[occurrence=2; interval_s=49; mitigation_applied=1] | T07-P93: T07-P93/CH05 | T07-P93: RECURRENCE_TIMELINE |

## CHAPTER 20 · 생성된 endpoint의 계약 검토 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P93에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-I01 | T07-P93: 복구 확인 | T07-P93: durable state와 사용자 결과를 모두 확인 | T07-P93: UNKNOWN_OUTCOME[timeout_ms=275; provider_state=UNKNOWN; lookup_id=p09300] | T07-P93: T07-P93/CH08 + PROVIDER_RESULT | T07-P93-incident-776 |
| T07-P93-I02 | T07-P93: 재주입 | T07-P93: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P93: OVERLOAD[rps=758; p99_ms=705; queue=16] | T07-P93: T07-P93/CH08 + QUEUE_PRESSURE | T07-P93-incident-777 |
| T07-P93-I03 | T07-P93: 회귀 고정 | T07-P93: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P93: SAMPLING[sample_rate=68%; trace_present=0; metric_present=1] | T07-P93: T07-P93/CH08 + TRACE_METRIC_CROSSCHECK | T07-P93-incident-778 |
| T07-P93-I04 | T07-P93: 영향 범위 | T07-P93: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P93: AI_COMPLEXITY[deps_added=4; cache_layer=1; rollback_plan=1] | T07-P93: T07-P93/CH08 + DIFF_EVAL_ROLLBACK | T07-P93-incident-779 |
| T07-P93-I05 | T07-P93: 변경 동결 | T07-P93: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P93: RECURRENCE[occurrence=6; interval_s=249; mitigation_applied=1] | T07-P93: T07-P93/CH08 + RECURRENCE_TIMELINE | T07-P93-incident-780 |
| T07-P93-I06 | T07-P93: correlation | T07-P93: 한 request/job/resource id를 시간축에 고정 | T07-P93: LARGE_INPUT[body_kb=664; limit_kb=640; parsed=0] | T07-P93: T07-P93/CH08 + SIZE_LIMIT | T07-P93-incident-781 |

## CHAPTER 21 · 생성된 endpoint의 계약 검토 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P93/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P93/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P93/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P93/CH04~T07-P93/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P93/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P93/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P93/CH18 signal 두 개와 T07-P93/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P93/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P93/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · 생성된 endpoint의 계약 검토 — 통합 casebook 16문제

T07-P93 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P93 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P93-C01 | T07-P93: 경계 A | T07-P93: OVERLOAD[rps=725; p99_ms=588; queue=22] | T07-P93: load=997, window=61s | T07-P93: review=민감정보 | T07-P93: 경계 A 위반 여부를 판정 | T07-P93: QUEUE_PRESSURE + T07-P93/CH08 |
| T07-P93-C02 | T07-P93: 경계 B | T07-P93: SAMPLING[sample_rate=55%; trace_present=1; metric_present=1] | T07-P93: load=120, window=80s | T07-P93: review=중복 | T07-P93: 경계 B 위반 여부를 판정 | T07-P93: TRACE_METRIC_CROSSCHECK + T07-P93/CH08 |
| T07-P93-C03 | T07-P93: 경계 C | T07-P93: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P93: load=143, window=99s | T07-P93: review=권한 | T07-P93: 경계 C 위반 여부를 판정 | T07-P93: DIFF_EVAL_ROLLBACK + T07-P93/CH08 |
| T07-P93-C04 | T07-P93: 경계 D | T07-P93: RECURRENCE[occurrence=5; interval_s=238; mitigation_applied=1] | T07-P93: load=166, window=28s | T07-P93: review=입력 경계 | T07-P93: 경계 D 위반 여부를 판정 | T07-P93: RECURRENCE_TIMELINE + T07-P93/CH08 |
| T07-P93-C05 | T07-P93: 경계 A | T07-P93: LARGE_INPUT[body_kb=2264; limit_kb=640; parsed=0] | T07-P93: load=189, window=47s | T07-P93: review=timeout | T07-P93: 경계 A 위반 여부를 판정 | T07-P93: SIZE_LIMIT + T07-P93/CH08 |
| T07-P93-C06 | T07-P93: 경계 B | T07-P93: OWNER_SPOOF[actor=A2; payload_owner=B0; auth_owner=A2] | T07-P93: load=212, window=66s | T07-P93: review=자원 | T07-P93: 경계 B 위반 여부를 판정 | T07-P93: AUTHZ_POLICY + T07-P93/CH08 |
| T07-P93-C07 | T07-P93: 경계 C | T07-P93: REORDER[in_seq=9,7,8; applied_version=4] | T07-P93: load=235, window=85s | T07-P93: review=순서 | T07-P93: 경계 C 위반 여부를 판정 | T07-P93: SEQUENCE_STATE + T07-P93/CH08 |
| T07-P93-C08 | T07-P93: 경계 D | T07-P93: RECOVERY_SCOPE[selected=61; expected=12; backup=1; dry_run=1] | T07-P93: load=258, window=14s | T07-P93: review=관측 | T07-P93: 경계 D 위반 여부를 판정 | T07-P93: RECOVERY_AUDIT + T07-P93/CH08 |
| T07-P93-C09 | T07-P93: 경계 A | T07-P93: REPLAY[key=cmd-093-08; attempts=4; response_seen=0] | T07-P93: load=281, window=33s | T07-P93: review=상태 변경 | T07-P93: 경계 A 위반 여부를 판정 | T07-P93: IDEMPOTENCY_RECORD + T07-P93/CH08 |
| T07-P93-C10 | T07-P93: 경계 B | T07-P93: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P93: load=304, window=52s | T07-P93: review=retry | T07-P93: 경계 B 위반 여부를 판정 | T07-P93: CLIENT_VERSION + T07-P93/CH08 |
| T07-P93-C11 | T07-P93: 경계 C | T07-P93: CONCURRENT_WRITE[actors=2; base_version=4; writes=2; gap_ms=65] | T07-P93: load=327, window=71s | T07-P93: review=복구 | T07-P93: 경계 C 위반 여부를 판정 | T07-P93: STATE_VERSION + T07-P93/CH08 |
| T07-P93-C12 | T07-P93: 경계 D | T07-P93: UNKNOWN_OUTCOME[timeout_ms=185; provider_state=UNKNOWN; lookup_id=p09311] | T07-P93: load=350, window=90s | T07-P93: review=동시성 | T07-P93: 경계 D 위반 여부를 판정 | T07-P93: PROVIDER_RESULT + T07-P93/CH08 |
| T07-P93-C13 | T07-P93: 경계 A | T07-P93: OVERLOAD[rps=488; p99_ms=1119; queue=33] | T07-P93: load=373, window=19s | T07-P93: review=민감정보 | T07-P93: 경계 A 위반 여부를 판정 | T07-P93: QUEUE_PRESSURE + T07-P93/CH08 |
| T07-P93-C14 | T07-P93: 경계 B | T07-P93: SAMPLING[sample_rate=17%; trace_present=1; metric_present=1] | T07-P93: load=396, window=38s | T07-P93: review=중복 | T07-P93: 경계 B 위반 여부를 판정 | T07-P93: TRACE_METRIC_CROSSCHECK + T07-P93/CH08 |
| T07-P93-C15 | T07-P93: 경계 C | T07-P93: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P93: load=419, window=57s | T07-P93: review=권한 | T07-P93: 경계 C 위반 여부를 판정 | T07-P93: DIFF_EVAL_ROLLBACK + T07-P93/CH08 |
| T07-P93-C16 | T07-P93: 경계 D | T07-P93: RECURRENCE[occurrence=2; interval_s=159; mitigation_applied=1] | T07-P93: load=442, window=76s | T07-P93: review=입력 경계 | T07-P93: 경계 D 위반 여부를 판정 | T07-P93: RECURRENCE_TIMELINE + T07-P93/CH08 |

채점은 결론보다 근거를 본다. T07-P93/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · 생성된 endpoint의 계약 검토 — evidence 판독 문제 14개

T07-P93 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P93 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P93-V01 | T07-P93: latency=233ms; queue=11; retry=2 | T07-P93: sampling | T07-P93: 축=sampling; 원인 확정은 보류 | T07-P93: TRACE_METRIC_CROSSCHECK + T07-P93/CH08 | T07-P93: 피할 오판=경계 누락 |
| T07-P93-V02 | T07-P93: latency=300ms; queue=22; retry=5 | T07-P93: AI 복잡도 | T07-P93: 축=AI 복잡도; 원인 확정은 보류 | T07-P93: DIFF_EVAL_ROLLBACK + T07-P93/CH08 | T07-P93: 피할 오판=증거 혼동 |
| T07-P93-V03 | T07-P93: latency=367ms; queue=33; retry=1 | T07-P93: 재발 | T07-P93: 축=재발; 원인 확정은 보류 | T07-P93: RECURRENCE_TIMELINE + T07-P93/CH08 | T07-P93: 피할 오판=재시도 오판 |
| T07-P93-V04 | T07-P93: latency=434ms; queue=44; retry=4 | T07-P93: 대형 입력 | T07-P93: 축=대형 입력; 원인 확정은 보류 | T07-P93: SIZE_LIMIT + T07-P93/CH08 | T07-P93: 피할 오판=소유권 혼동 |
| T07-P93-V05 | T07-P93: latency=501ms; queue=55; retry=0 | T07-P93: 소유권 위조 | T07-P93: 축=소유권 위조; 원인 확정은 보류 | T07-P93: AUTHZ_POLICY + T07-P93/CH08 | T07-P93: 피할 오판=동시성 무시 |
| T07-P93-V06 | T07-P93: latency=568ms; queue=66; retry=3 | T07-P93: 순서 역전 | T07-P93: 축=순서 역전; 원인 확정은 보류 | T07-P93: SEQUENCE_STATE + T07-P93/CH08 | T07-P93: 피할 오판=운영 한계 누락 |
| T07-P93-V07 | T07-P93: latency=635ms; queue=77; retry=6 | T07-P93: 복구 범위 | T07-P93: 축=복구 범위; 원인 확정은 보류 | T07-P93: RECOVERY_AUDIT + T07-P93/CH08 | T07-P93: 피할 오판=오류 합치기 |
| T07-P93-V08 | T07-P93: latency=702ms; queue=8; retry=2 | T07-P93: 재전송 | T07-P93: 축=재전송; 원인 확정은 보류 | T07-P93: IDEMPOTENCY_RECORD + T07-P93/CH08 | T07-P93: 피할 오판=AI 과신 |
| T07-P93-V09 | T07-P93: latency=769ms; queue=19; retry=5 | T07-P93: 구버전 client | T07-P93: 축=구버전 client; 원인 확정은 보류 | T07-P93: CLIENT_VERSION + T07-P93/CH08 | T07-P93: 피할 오판=복구 과잉 |
| T07-P93-V10 | T07-P93: latency=836ms; queue=30; retry=1 | T07-P93: 동시 변경 | T07-P93: 축=동시 변경; 원인 확정은 보류 | T07-P93: STATE_VERSION + T07-P93/CH08 | T07-P93: 피할 오판=잘못된 전제 |
| T07-P93-V11 | T07-P93: latency=903ms; queue=41; retry=4 | T07-P93: unknown outcome | T07-P93: 축=unknown outcome; 원인 확정은 보류 | T07-P93: PROVIDER_RESULT + T07-P93/CH08 | T07-P93: 피할 오판=경계 누락 |
| T07-P93-V12 | T07-P93: latency=970ms; queue=52; retry=0 | T07-P93: 과부하 | T07-P93: 축=과부하; 원인 확정은 보류 | T07-P93: QUEUE_PRESSURE + T07-P93/CH08 | T07-P93: 피할 오판=증거 혼동 |
| T07-P93-V13 | T07-P93: latency=1037ms; queue=63; retry=3 | T07-P93: sampling | T07-P93: 축=sampling; 원인 확정은 보류 | T07-P93: TRACE_METRIC_CROSSCHECK + T07-P93/CH08 | T07-P93: 피할 오판=재시도 오판 |
| T07-P93-V14 | T07-P93: latency=1104ms; queue=74; retry=6 | T07-P93: AI 복잡도 | T07-P93: 축=AI 복잡도; 원인 확정은 보류 | T07-P93: DIFF_EVAL_ROLLBACK + T07-P93/CH08 | T07-P93: 피할 오판=소유권 혼동 |

## CHAPTER 24 · 생성된 endpoint의 계약 검토 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P93에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P93-D01 | T07-P93: 로그 확대 | T07-P93: debug를 위해 payload와 context 기록을 늘린다 | T07-P93: secret·PII·cardinality 비용을 통제하는지 | T07-P93: RECURRENCE[occurrence=2; interval_s=205; mitigation_applied=1] | T07-P93: RECURRENCE_TIMELINE + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D02 | T07-P93: 강제 종료 | T07-P93: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P93: in-flight request와 background work의 결과를 잃는지 | T07-P93: LARGE_INPUT[body_kb=2000; limit_kb=640; parsed=0] | T07-P93: SIZE_LIMIT + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D03 | T07-P93: AI package 추가 | T07-P93: AI가 제안한 새 dependency를 도입한다 | T07-P93: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P93: OWNER_SPOOF[actor=A2; payload_owner=B2; auth_owner=A2] | T07-P93: AUTHZ_POLICY + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D04 | T07-P93: 비동기화 | T07-P93: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P93: durability·status API·worker retry 계약이 생기는지 | T07-P93: REORDER[in_seq=6,4,5; applied_version=4] | T07-P93: SEQUENCE_STATE + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D05 | T07-P93: 권한 shortcut | T07-P93: payload의 owner/tenant id를 바로 사용한다 | T07-P93: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P93: RECOVERY_SCOPE[selected=239; expected=13; backup=1; dry_run=0] | T07-P93: RECOVERY_AUDIT + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D06 | T07-P93: 순서 병렬화 | T07-P93: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P93: 선후관계 invariant와 race를 깨지 않는지 | T07-P93: REPLAY[key=cmd-093-05; attempts=4; response_seen=0] | T07-P93: IDEMPOTENCY_RECORD + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D07 | T07-P93: validation 이동 | T07-P93: validation을 business side effect 뒤로 옮긴다 | T07-P93: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P93: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P93: CLIENT_VERSION + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P93-D08 | T07-P93: batch 확대 | T07-P93: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P93: memory·deadline·부분 실패 범위가 커지는지 | T07-P93: CONCURRENT_WRITE[actors=2; base_version=4; writes=2; gap_ms=26] | T07-P93: STATE_VERSION + T07-P93/CH08 | T07-P93: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P93: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · 생성된 endpoint의 계약 검토 — 최종 contract와 evidence spine

**최종 계약:** endpoint가 실행된다는 것과 API 계약이 올바른 것은 다르다.

**정상 메커니즘:** method/path/input schema/status/error/idempotency/authorization/compatibility를 checklist가 아니라 실제 예제로 검증한다.

**대표 실패:** happy path curl 하나만 성공해 완료 처리한다.

**검증 evidence:** 유효·경계·잘못된 입력별 실제/예상 status와 body를 비교한다.

**직접 행동:** test case 표에서 예상과 실제가 다른 항목을 계산한다.

**다음 연결:** `생성된 persistence 코드와 migration 경계 검토`.

`생성된 endpoint의 계약 검토`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |
| OWASP-API | OWASP-API | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | 생성된 endpoint의 계약 검토의 개념·실패·운영 판단 교차 확인 |

`생성된 endpoint의 계약 검토` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
