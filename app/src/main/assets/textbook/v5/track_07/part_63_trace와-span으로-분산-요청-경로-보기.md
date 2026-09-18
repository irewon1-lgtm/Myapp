# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 05 · 관측 가능성과 장애 복구를 요청 단위로 익히기

### LESSON 03 · trace와 span으로 분산 요청 경로 보기

## CHAPTER 01 · trace와 span으로 분산 요청 경로 보기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** trace는 하나의 요청이 여러 service·DB·queue를 지날 때 구간별 시간을 연결한 기록이다.

**아주 쉬운 사건.** gateway→app→DB span이 이어진다. 이 사건에서는 먼저 **trace parent-child와 context를 확인한다**.

**왜 필요한가.** 정상 동작은 trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다. 반대로 trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다.

**암기:** `trace와 span으로 분산 요청 경로 보기`의 역할 한 줄.

**직접 이해:** `trace와 span으로 분산 요청 경로 보기`의 입력·상태·결과 경계.

**AI 위임 가능:** `trace와 span으로 분산 요청 경로 보기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `gateway→app→DB span이 이어진다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 critical path, span duration, error status, context propagation gap을 본다.

## CHAPTER 02 · trace와 span으로 분산 요청 경로 보기 — 아주 쉬운 예를 한 단계씩 해석

T07-P63: `gateway→app→DB span이 이어진다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | gateway→app→DB span이 이어진다 | T07-P63 외부 입력 | T07-P63: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | trace parent-child와 context를 확인한다 | T07-P63 판단 기준 | T07-P63/CH08 관측표와 대조 |
| 정상 경로 | T07-P63/CH03 M1→M5 | trace와 span으로 분산 요청 경로 보기: 완료 시점을 단계별로 분리 | T07-P63: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P63/CH06 trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다. | T07-P63: 깨진 계약 하나를 특정 | trace와 span으로 분산 요청 경로 보기: 증상과 원인을 분리 |
| 재검증 | T07-P63/CH10 직접 실행 | T07-P63: 예상값 T07-P063 410 기록 | trace와 span으로 분산 요청 경로 보기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P63/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P63/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · trace와 span으로 분산 요청 경로 보기 — 내부 메커니즘과 상태 전이

trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | gateway→app→DB span이 이어진다 | source/actor/size를 보존 |
| M2 | 경계 판단 | trace parent-child와 context를 확인한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | critical path, span duration, error status, context propagation gap을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | span 목록에서 parent 관계를 따라 총 경로와 가장 긴 구간을 찾는다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`trace와 span으로 분산 요청 경로 보기` 흐름을 framework 이름 없이 설명한다.

막히면 `gateway→app→DB span이 이어진다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · trace와 span으로 분산 요청 경로 보기 — 실전 경계 A

**경계 A.** trace id는 한 end-to-end 요청을, span id는 그 안의 한 작업 구간을 식별하고 parent-child relation으로 call path를 복원한다.

T07-P63/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P63에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P63/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P63/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P63): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P63/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P63-A1-389 | T07-P63 조건 | trace id는 한 end-to-end 요청을, span id는 그 안의 한 작업 구간을 식별하고 parent-child relation으로 call path를 복원한다. |
| T07-P63-A2-390 | T07-P63 변화점 | T07-P63/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P63-A3-391 | T07-P63 반례 | T07-P63/CH06 대표 실패와 A 위반을 구별 |
| T07-P63-A4-392 | T07-P63 근거 | T07-P63/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P63-A5-393 | T07-P63 재실험 | gateway→app→DB span이 이어진다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · trace와 span으로 분산 요청 경로 보기 — 실전 경계 B

**경계 B.** span attribute는 route template·dependency name처럼 집계 가능한 값 위주로 두고 user id·raw URL을 무제한 label처럼 넣어 cardinality를 폭발시키지 않는다.

T07-P63/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P63에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P63/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P63/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P63): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P63/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P63-B1-420 | T07-P63 조건 | span attribute는 route template·dependency name처럼 집계 가능한 값 위주로 두고 user id·raw URL을 무제한 label처럼 넣어 cardinality를 폭발시키지 않는다. |
| T07-P63-B2-421 | T07-P63 독립성 | T07-P63/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P63-B3-422 | T07-P63 상태 | T07-P63/CH03 before·after 위치를 다시 지정 |
| T07-P63-B4-423 | T07-P63 반증 | T07-P63/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P63-B5-424 | T07-P63 적용 | trace와 span으로 분산 요청 경로 보기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · trace와 span으로 분산 요청 경로 보기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다.**

아래 여섯 사례는 T07-P63의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P63-F01 | T07-P63: 대표 실패 | T07-P63: T07-P63/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P63: 현상만 보고 원인을 확정 | T07-P63/CH08 evidence map에서 상태를 대조 |
| T07-P63-F02 | T07-P63: 경계 A 누락 | T07-P63: T07-P63/CH04 경계 A 위반 입력 | T07-P63: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P63/CH08 evidence map에서 상태를 대조 |
| T07-P63-F03 | T07-P63: 경계 B 누락 | T07-P63: T07-P63/CH05 경계 B 위반 입력 | T07-P63: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P63/CH08 evidence map에서 상태를 대조 |
| T07-P63-F04 | T07-P63: 복구 경계 C 누락 | T07-P63: T07-P63/CH07 경계 C 복구 조건 | T07-P63: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P63/CH08 evidence map에서 상태를 대조 |
| T07-P63-F05 | T07-P63: 운영 경계 D 누락 | T07-P63: T07-P63/CH09 경계 D 운영 조건 | T07-P63: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P63/CH08 evidence map에서 상태를 대조 |
| T07-P63-F06 | T07-P63: 증거 없는 결론 | T07-P63: T07-P63/CH02 첫 판단만 존재 | T07-P63: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P63/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P63/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · trace와 span으로 분산 요청 경로 보기 — 복구 가능한 상태와 수명

**경계 C.** sampling은 비용을 줄이지만 rare failure trace가 빠질 수 있어 head/tail sampling과 error-biased 보존 같은 trade-off를 이해한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P63에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P63/CH08 evidence map을 본다. 복구 후에는 T07-P63/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P63에서 이미 확정된 side effect는 T07-P63/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P63-R1-482 | T07-P63 중단 직전 | T07-P63/CH03에서 이미 확정된 상태만 표시 |
| T07-P63-R2-483 | T07-P63 재시작 직후 | sampling은 비용을 줄이지만 rare failure trace가 빠질 수 있어 head/tail sampling과 error-biased 보존 같은 trade-off를 이해한다. |
| T07-P63-R3-484 | T07-P63 재검증 | T07-P63/CH08 근거로 중복·누락 여부 확인 |
| T07-P63-R4-485 | T07-P63 재실행 | T07-P63/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · trace와 span으로 분산 요청 경로 보기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | gateway→app→DB span이 이어진다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | trace parent-child와 context를 확인한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | critical path, span duration, error status, context propagation gap을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`trace와 span으로 분산 요청 경로 보기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · trace와 span으로 분산 요청 경로 보기 — 운영 한계와 종료 조건

**경계 D.** queue를 건너는 async workflow에서는 producer context를 message에 안전하게 전달하고 consumer가 새 span을 link/parent로 연결해야 trace가 request에서 background work까지 이어진다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P63/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P63/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P63 과제: 경계 D와 T07-P63/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P63-O1-544 | synthetic-load=44 | T07-P63/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P63-O2-545 | synthetic-budget=694ms | T07-P63 timeout과 unknown outcome을 분리 |
| T07-P63-O3-546 | T07-P63 종료 | queue를 건너는 async workflow에서는 producer context를 message에 안전하게 전달하고 consumer가 새 span을 link/parent로 연결해야 trace가 request에서 background work까지 이어진다. |
| T07-P63-O4-547 | T07-P63 완화 | T07-P63/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · trace와 span으로 분산 요청 경로 보기 — 직접 실행하는 작은 모델

`trace와 span으로 분산 요청 경로 보기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P063 410`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P063";
let remaining = 800;
for (const cost of [120, 180, 90]) remaining -= cost;
console.log(marker, remaining);
```

기준 출력: `T07-P063 410`.

`trace와 span으로 분산 요청 경로 보기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P63-L1-576 | constmarker="T07-P063"; | T07-P63 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P63-L2-577 | letremaining=800; | T07-P63 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P63-L3-578 | for(constcostof[120,180,90])remaining-=cost; | T07-P63 계산 지점; 수정 전후 결과가 갈리는 위치 |
| T07-P63-L4-579 | console.log(marker,remaining); | T07-P63 출력 관측점; 예상 `T07-P063 410`와 비교 |
| T07-P63-LX-665 | T07-P63 실행 기록 | T07-P63 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · trace와 span으로 분산 요청 경로 보기 — 한 부분만 수정하고 다시 예측

수정 과제: **두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다**.

수정 전은 `T07-P063 410`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P63/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P63-D1-606 | 기준 `T07-P063 410` | T07-P63 수정 전 실행을 먼저 재현 |
| T07-P63-D2-607 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | T07-P63 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P63-D3-608 | T07-P63 새 예측 | T07-P63 실행 전에 출력·상태를 먼저 기록 |
| T07-P63-D4-609 | T07-P63 재실행 | T07-P63/CH10 실제값과 새 예측을 대조 |
| T07-P63-D5-610 | T07-P63 반례 | T07-P63/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P63-D6-611 | T07-P63 근거 | T07-P63/CH08 상태가 설명과 일치해야 완료 |
| T07-P63-D7-612 | T07-P63 이유 | T07-P63 변경 이유를 trace와 span으로 분산 요청 경로 보기 계약과 연결해 설명 |

## CHAPTER 12 · trace와 span으로 분산 요청 경로 보기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: trace와 span으로 분산 요청 경로 보기 | trace는 하나의 요청이 여러 service·DB·queue를 지날 때 구간별 시간을 연결한 기록이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P63/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | critical path, span duration, error status, context propagation gap을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P63/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P63/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P63/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P63/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `trace와 span으로 분산 요청 경로 보기` 실행 코드 수정 | 두 번째 cost를 50 늘려 remaining이 음수가 되는지 확인한다 | `trace와 span으로 분산 요청 경로 보기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P63/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P63/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · trace와 span으로 분산 요청 경로 보기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | trace는 하나의 요청이 여러 service·DB·queue를 지날 때 구간별 시간을 연결한 기록이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P63/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P63/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P63/CH10 실행용 boilerplate·test 후보 | T07-P63: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P063 410` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P63/CH02의 판단 기준과 T07-P63/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P63-AI1-668 | T07-P63 사람 결정 | T07-P63 업무 의미·허용 위험·완료 기준 소유 |
| T07-P63-AI2-669 | T07-P63 AI 초안 | T07-P63/CH10 boilerplate·test 후보까지만 위임 |
| T07-P63-AI3-670 | T07-P63 검증 | T07-P63/CH06 반례와 T07-P63/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · trace와 span으로 분산 요청 경로 보기 — 경계 조합 실험 8개

T07-P63: T07-P63/CH04~T07-P63/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P63의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-K01 | T07-P63: 경계 A | T07-P63: 경계 B | T07-P63: DISCONNECT[disconnect_ms=40; commit_state=UNKNOWN; request=063-01] / sample=88 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + COMMIT_TIMELINE |
| T07-P63-K02 | T07-P63: 경계 A | T07-P63: 경계 C | T07-P63: RECURRENCE[occurrence=4; interval_s=201; mitigation_applied=1] / sample=95 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + RECURRENCE_TIMELINE |
| T07-P63-K03 | T07-P63: 경계 A | T07-P63: 경계 D | T07-P63: LARGE_INPUT[body_kb=1968; limit_kb=640; parsed=0] / sample=13 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + SIZE_LIMIT |
| T07-P63-K04 | T07-P63: 경계 B | T07-P63: 경계 C | T07-P63: DRAIN[ready=0; active=9; drain_deadline_s=9] / sample=20 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + DRAIN_STATE |
| T07-P63-K05 | T07-P63: 경계 B | T07-P63: 경계 D | T07-P63: RECOVERY_SCOPE[selected=224; expected=20; backup=1; dry_run=1] / sample=27 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + RECOVERY_AUDIT |
| T07-P63-K06 | T07-P63: 경계 C | T07-P63: 경계 D | T07-P63: UNKNOWN_OUTCOME[timeout_ms=315; provider_state=UNKNOWN; lookup_id=p06306] / sample=34 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + PROVIDER_RESULT |
| T07-P63-K07 | T07-P63: 경계 A | T07-P63: 경계 B+C | T07-P63: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=41 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + DURABLE_STATE |
| T07-P63-K08 | T07-P63: 경계 B | T07-P63: 경계 C+D | T07-P63: OVERLOAD[rps=278; p99_ms=426; queue=32] / sample=48 | T07-P63: 먼저 깨지는 경계를 판정 | T07-P63: T07-P63/CH08 + QUEUE_PRESSURE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P63/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · trace와 span으로 분산 요청 경로 보기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P63와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-B02 | T07-P63: server metric을 rate·error·duration으로 보기 | T07-P63: 요청 수와 오류 수가 함께 증가한다 | T07-P63: rate·error·duration을 같은 창에서 본다 | T07-P63: T07-P63/CH08 증거와 형제 LESSON 증거를 분리 | T07-P63: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P63-B04 | T07-P63: SLI·SLO를 backend 계약으로 정하기 | T07-P63: 30일 SLO가 빠르게 소진된다 | T07-P63: error budget과 burn rate를 본다 | T07-P63: T07-P63/CH08 증거와 형제 LESSON 증거를 분리 | T07-P63: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P63-B01 | T07-P63: structured log와 request correlation | T07-P63: 같은 오류가 여러 문장 형태로 로그된다 | T07-P63: event name과 field를 구조화한다 | T07-P63: T07-P63/CH08 증거와 형제 LESSON 증거를 분리 | T07-P63: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P63-B05 | T07-P63: health·readiness·liveness를 분리하기 | T07-P63: DB가 잠깐 느린데 liveness도 실패한다 | T07-P63: readiness와 restart 조건을 분리한다 | T07-P63: T07-P63/CH08 증거와 형제 LESSON 증거를 분리 | T07-P63: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P63-B06 | T07-P63: graceful shutdown과 draining | T07-P63: 배포 중 20개 요청이 처리 중이다 | T07-P63: draining deadline을 적용한다 | T07-P63: T07-P63/CH08 증거와 형제 LESSON 증거를 분리 | T07-P63: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · trace와 span으로 분산 요청 경로 보기 — 선택형 실패 주입 6개

T07-P63: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P63 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-X01 | T07-P63: 대형 입력 | T07-P63: 입력 크기가 정상의 100배; sample=973 | T07-P63: 의미 검증과 resource limit | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: body/batch size·parse time·memory·reject status |
| T07-P63-X02 | T07-P63: drain | T07-P63: 배포 중 기존 요청이 처리 중; sample=990 | T07-P63: 새 traffic 차단과 in-flight 처리 | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: readiness·active requests·deadline·final state |
| T07-P63-X03 | T07-P63: 복구 범위 | T07-P63: 복구 script 대상이 예상보다 큼; sample=10 | T07-P63: 진단과 destructive recovery | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: selected ids/count·backup·audit trail |
| T07-P63-X04 | T07-P63: unknown outcome | T07-P63: dependency timeout 후 성공 여부 불명; sample=27 | T07-P63: 실패와 미확정 결과 | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: provider id·조회 결과·retry history |
| T07-P63-X05 | T07-P63: 재시작 | T07-P63: side effect 직후 process가 재시작됨; sample=44 | T07-P63: durable state와 memory state | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: commit·outbox·job id·restart 전후 상태 |
| T07-P63-X06 | T07-P63: 과부하 | T07-P63: traffic 세 배, p99 급증; sample=61 | T07-P63: 기능 실패와 saturation | T07-P63: T07-P63/CH02 판단과 별도 기록 | T07-P63: queue age·pool wait·CPU/event-loop·quota |

## CHAPTER 17 · trace와 span으로 분산 요청 경로 보기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P63에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-E01 | T07-P63: 대표 실패를 원인으로 착각 | T07-P63: T07-P63/CH06 실패 case를 다른 입력으로 재현 | T07-P63: 현상과 원인을 같은 것으로 봄 | T07-P63: T07-P63/CH06 대표 실패와 T07-P63/CH08 증거를 다시 대조 | T07-P63: T07-P63/CH08 |
| T07-P63-E02 | T07-P63: 경계 A 생략 | T07-P63: T07-P63/CH04의 조건 하나를 반대로 설정 | T07-P63: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P63: T07-P63/CH04를 새 입력에 적용 | T07-P63: T07-P63/CH08 |
| T07-P63-E03 | T07-P63: 경계 B 생략 | T07-P63: T07-P63/CH05의 조건 하나를 반대로 설정 | T07-P63: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P63: T07-P63/CH05를 새 입력에 적용 | T07-P63: T07-P63/CH08 |
| T07-P63-E04 | T07-P63: 복구 상태 혼동 | T07-P63: T07-P63/CH07에서 처리 중단을 주입 | T07-P63: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P63: T07-P63/CH07에서 수명 경계를 다시 표시 | T07-P63: T07-P63/CH08 |
| T07-P63-E05 | T07-P63: 운영 한계 누락 | T07-P63: T07-P63/CH09에서 부하 또는 drain 조건을 변경 | T07-P63: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P63: T07-P63/CH09의 종료 조건을 다시 작성 | T07-P63: T07-P63/CH08 |
| T07-P63-E06 | T07-P63: 증거 없는 성공 판정 | T07-P63: T07-P63/CH08에서 증거 하나를 숨김 | T07-P63: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P63: T07-P63/CH08에서 독립 증거 둘을 선택 | T07-P63: T07-P63/CH08 |

## CHAPTER 18 · trace와 span으로 분산 요청 경로 보기 — synthetic 관측값 판독 6개

T07-P63: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P63의 숫자 하나만으로 원인을 단정하지 않고 T07-P63/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-O01 | T07-P63/error_rate | 992 | T07-P63: 오류율 | T07-P63: 축=재시작; 원인 확정 금지 | T07-P63: DURABLE_STATE + T07-P63/CH08 |
| T07-P63-O02 | T07-P63/p99_ms | 12 | T07-P63: p99 지연 | T07-P63: 축=과부하; 원인 확정 금지 | T07-P63: QUEUE_PRESSURE + T07-P63/CH08 |
| T07-P63-O03 | T07-P63/queue_depth | 29 | T07-P63: 대기열 깊이 | T07-P63: 축=sampling; 원인 확정 금지 | T07-P63: TRACE_METRIC_CROSSCHECK + T07-P63/CH08 |
| T07-P63-O04 | T07-P63/heap_mb | 46 | T07-P63: heap 사용량 | T07-P63: 축=client disconnect; 원인 확정 금지 | T07-P63: COMMIT_TIMELINE + T07-P63/CH08 |
| T07-P63-O05 | T07-P63/event_loop_lag_ms | 63 | T07-P63: event-loop 지연 | T07-P63: 축=재발; 원인 확정 금지 | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 |
| T07-P63-O06 | T07-P63/dependency_errors | 80 | T07-P63: dependency 오류 수 | T07-P63: 축=대형 입력; 원인 확정 금지 | T07-P63: SIZE_LIMIT + T07-P63/CH08 |

## CHAPTER 19 · trace와 span으로 분산 요청 경로 보기 — 선택형 코드 리뷰 6질문

T07-P63: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P63에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-R01 | T07-P63: 입력 경계 | T07-P63: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P63: OVERLOAD[rps=647; p99_ms=363; queue=17] | T07-P63: T07-P63/CH04 | T07-P63: QUEUE_PRESSURE |
| T07-P63-R02 | T07-P63: 순서 | T07-P63: old/new event 순서가 바뀌어도 안전한가 | T07-P63: SAMPLING[sample_rate=30%; trace_present=1; metric_present=1] | T07-P63: T07-P63/CH05 | T07-P63: TRACE_METRIC_CROSSCHECK |
| T07-P63-R03 | T07-P63: retry | T07-P63: 재시도가 전체 deadline과 idempotency를 존중하는가 | T07-P63: DISCONNECT[disconnect_ms=53; commit_state=UNKNOWN; request=063-02] | T07-P63: T07-P63/CH06 | T07-P63: COMMIT_TIMELINE |
| T07-P63-R04 | T07-P63: 민감정보 | T07-P63: 관측 데이터가 secret/PII를 과하게 남기지 않는가 | T07-P63: RECURRENCE[occurrence=5; interval_s=212; mitigation_applied=1] | T07-P63: T07-P63/CH07 | T07-P63: RECURRENCE_TIMELINE |
| T07-P63-R05 | T07-P63: 입력 경계 | T07-P63: 외부 입력이 내부 신뢰값으로 바로 섞이는가 | T07-P63: LARGE_INPUT[body_kb=2056; limit_kb=640; parsed=0] | T07-P63: T07-P63/CH04 | T07-P63: SIZE_LIMIT |
| T07-P63-R06 | T07-P63: 순서 | T07-P63: old/new event 순서가 바뀌어도 안전한가 | T07-P63: DRAIN[ready=0; active=5; drain_deadline_s=10] | T07-P63: T07-P63/CH05 | T07-P63: DRAIN_STATE |

## CHAPTER 20 · trace와 span으로 분산 요청 경로 보기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P63에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-I01 | T07-P63: 회귀 고정 | T07-P63: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P63: SAMPLING[sample_rate=17%; trace_present=0; metric_present=1] | T07-P63: T07-P63/CH08 + TRACE_METRIC_CROSSCHECK | T07-P63-incident-206 |
| T07-P63-I02 | T07-P63: 영향 범위 | T07-P63: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P63: DISCONNECT[disconnect_ms=40; commit_state=UNKNOWN; request=063-01] | T07-P63: T07-P63/CH08 + COMMIT_TIMELINE | T07-P63-incident-207 |
| T07-P63-I03 | T07-P63: 변경 동결 | T07-P63: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P63: RECURRENCE[occurrence=4; interval_s=201; mitigation_applied=1] | T07-P63: T07-P63/CH08 + RECURRENCE_TIMELINE | T07-P63-incident-208 |
| T07-P63-I04 | T07-P63: correlation | T07-P63: 한 request/job/resource id를 시간축에 고정 | T07-P63: LARGE_INPUT[body_kb=1968; limit_kb=640; parsed=0] | T07-P63: T07-P63/CH08 + SIZE_LIMIT | T07-P63-incident-209 |
| T07-P63-I05 | T07-P63: 마지막 정상 | T07-P63: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P63: DRAIN[ready=0; active=9; drain_deadline_s=9] | T07-P63: T07-P63/CH08 + DRAIN_STATE | T07-P63-incident-210 |
| T07-P63-I06 | T07-P63: 가설 검증 | T07-P63: 원인 후보 하나만 뒤집어 재현 | T07-P63: RECOVERY_SCOPE[selected=224; expected=20; backup=1; dry_run=1] | T07-P63: T07-P63/CH08 + RECOVERY_AUDIT | T07-P63-incident-211 |

## CHAPTER 21 · trace와 span으로 분산 요청 경로 보기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P63/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P63/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P63/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P63/CH04~T07-P63/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P63/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P63/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P63/CH18 signal 두 개와 T07-P63/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P63/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P63/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · trace와 span으로 분산 요청 경로 보기 — 통합 casebook 16문제

T07-P63 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P63 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P63-C01 | T07-P63: 경계 A | T07-P63: DISCONNECT[disconnect_ms=27; commit_state=UNKNOWN; request=063-00] | T07-P63: load=127, window=91s | T07-P63: review=순서 | T07-P63: 경계 A 위반 여부를 판정 | T07-P63: COMMIT_TIMELINE + T07-P63/CH08 |
| T07-P63-C02 | T07-P63: 경계 B | T07-P63: RECURRENCE[occurrence=3; interval_s=190; mitigation_applied=1] | T07-P63: load=150, window=20s | T07-P63: review=관측 | T07-P63: 경계 B 위반 여부를 판정 | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 |
| T07-P63-C03 | T07-P63: 경계 C | T07-P63: LARGE_INPUT[body_kb=1880; limit_kb=640; parsed=0] | T07-P63: load=173, window=39s | T07-P63: review=상태 변경 | T07-P63: 경계 C 위반 여부를 판정 | T07-P63: SIZE_LIMIT + T07-P63/CH08 |
| T07-P63-C04 | T07-P63: 경계 D | T07-P63: DRAIN[ready=0; active=13; drain_deadline_s=8] | T07-P63: load=196, window=58s | T07-P63: review=retry | T07-P63: 경계 D 위반 여부를 판정 | T07-P63: DRAIN_STATE + T07-P63/CH08 |
| T07-P63-C05 | T07-P63: 경계 A | T07-P63: RECOVERY_SCOPE[selected=213; expected=7; backup=1; dry_run=0] | T07-P63: load=219, window=77s | T07-P63: review=복구 | T07-P63: 경계 A 위반 여부를 판정 | T07-P63: RECOVERY_AUDIT + T07-P63/CH08 |
| T07-P63-C06 | T07-P63: 경계 B | T07-P63: UNKNOWN_OUTCOME[timeout_ms=304; provider_state=UNKNOWN; lookup_id=p06305] | T07-P63: load=242, window=96s | T07-P63: review=동시성 | T07-P63: 경계 B 위반 여부를 판정 | T07-P63: PROVIDER_RESULT + T07-P63/CH08 |
| T07-P63-C07 | T07-P63: 경계 C | T07-P63: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P63: load=265, window=25s | T07-P63: review=민감정보 | T07-P63: 경계 C 위반 여부를 판정 | T07-P63: DURABLE_STATE + T07-P63/CH08 |
| T07-P63-C08 | T07-P63: 경계 D | T07-P63: OVERLOAD[rps=245; p99_ms=1182; queue=35] | T07-P63: load=288, window=44s | T07-P63: review=중복 | T07-P63: 경계 D 위반 여부를 판정 | T07-P63: QUEUE_PRESSURE + T07-P63/CH08 |
| T07-P63-C09 | T07-P63: 경계 A | T07-P63: SAMPLING[sample_rate=24%; trace_present=0; metric_present=1] | T07-P63: load=311, window=63s | T07-P63: review=권한 | T07-P63: 경계 A 위반 여부를 판정 | T07-P63: TRACE_METRIC_CROSSCHECK + T07-P63/CH08 |
| T07-P63-C10 | T07-P63: 경계 B | T07-P63: DISCONNECT[disconnect_ms=47; commit_state=UNKNOWN; request=063-09] | T07-P63: load=334, window=82s | T07-P63: review=입력 경계 | T07-P63: 경계 B 위반 여부를 판정 | T07-P63: COMMIT_TIMELINE + T07-P63/CH08 |
| T07-P63-C11 | T07-P63: 경계 C | T07-P63: RECURRENCE[occurrence=2; interval_s=78; mitigation_applied=1] | T07-P63: load=357, window=11s | T07-P63: review=timeout | T07-P63: 경계 C 위반 여부를 판정 | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 |
| T07-P63-C12 | T07-P63: 경계 D | T07-P63: LARGE_INPUT[body_kb=984; limit_kb=640; parsed=0] | T07-P63: load=380, window=30s | T07-P63: review=자원 | T07-P63: 경계 D 위반 여부를 판정 | T07-P63: SIZE_LIMIT + T07-P63/CH08 |
| T07-P63-C13 | T07-P63: 경계 A | T07-P63: DRAIN[ready=0; active=16; drain_deadline_s=9] | T07-P63: load=403, window=49s | T07-P63: review=순서 | T07-P63: 경계 A 위반 여부를 판정 | T07-P63: DRAIN_STATE + T07-P63/CH08 |
| T07-P63-C14 | T07-P63: 경계 B | T07-P63: RECOVERY_SCOPE[selected=101; expected=8; backup=1; dry_run=1] | T07-P63: load=426, window=68s | T07-P63: review=관측 | T07-P63: 경계 B 위반 여부를 판정 | T07-P63: RECOVERY_AUDIT + T07-P63/CH08 |
| T07-P63-C15 | T07-P63: 경계 C | T07-P63: UNKNOWN_OUTCOME[timeout_ms=192; provider_state=UNKNOWN; lookup_id=p06314] | T07-P63: load=449, window=87s | T07-P63: review=상태 변경 | T07-P63: 경계 C 위반 여부를 판정 | T07-P63: PROVIDER_RESULT + T07-P63/CH08 |
| T07-P63-C16 | T07-P63: 경계 D | T07-P63: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P63: load=472, window=16s | T07-P63: review=retry | T07-P63: 경계 D 위반 여부를 판정 | T07-P63: DURABLE_STATE + T07-P63/CH08 |

채점은 결론보다 근거를 본다. T07-P63/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · trace와 span으로 분산 요청 경로 보기 — evidence 판독 문제 14개

T07-P63 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P63 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P63-V01 | T07-P63: latency=803ms; queue=41; retry=0 | T07-P63: 재발 | T07-P63: 축=재발; 원인 확정은 보류 | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 | T07-P63: 피할 오판=운영 한계 누락 |
| T07-P63-V02 | T07-P63: latency=870ms; queue=52; retry=3 | T07-P63: 대형 입력 | T07-P63: 축=대형 입력; 원인 확정은 보류 | T07-P63: SIZE_LIMIT + T07-P63/CH08 | T07-P63: 피할 오판=오류 합치기 |
| T07-P63-V03 | T07-P63: latency=937ms; queue=63; retry=6 | T07-P63: drain | T07-P63: 축=drain; 원인 확정은 보류 | T07-P63: DRAIN_STATE + T07-P63/CH08 | T07-P63: 피할 오판=복구 과잉 |
| T07-P63-V04 | T07-P63: latency=1004ms; queue=74; retry=2 | T07-P63: 복구 범위 | T07-P63: 축=복구 범위; 원인 확정은 보류 | T07-P63: RECOVERY_AUDIT + T07-P63/CH08 | T07-P63: 피할 오판=경계 누락 |
| T07-P63-V05 | T07-P63: latency=1071ms; queue=5; retry=5 | T07-P63: unknown outcome | T07-P63: 축=unknown outcome; 원인 확정은 보류 | T07-P63: PROVIDER_RESULT + T07-P63/CH08 | T07-P63: 피할 오판=증거 혼동 |
| T07-P63-V06 | T07-P63: latency=1138ms; queue=16; retry=1 | T07-P63: 재시작 | T07-P63: 축=재시작; 원인 확정은 보류 | T07-P63: DURABLE_STATE + T07-P63/CH08 | T07-P63: 피할 오판=재시도 오판 |
| T07-P63-V07 | T07-P63: latency=1205ms; queue=27; retry=4 | T07-P63: 과부하 | T07-P63: 축=과부하; 원인 확정은 보류 | T07-P63: QUEUE_PRESSURE + T07-P63/CH08 | T07-P63: 피할 오판=동시성 무시 |
| T07-P63-V08 | T07-P63: latency=1272ms; queue=38; retry=0 | T07-P63: sampling | T07-P63: 축=sampling; 원인 확정은 보류 | T07-P63: TRACE_METRIC_CROSSCHECK + T07-P63/CH08 | T07-P63: 피할 오판=순서 가정 |
| T07-P63-V09 | T07-P63: latency=1339ms; queue=49; retry=3 | T07-P63: client disconnect | T07-P63: 축=client disconnect; 원인 확정은 보류 | T07-P63: COMMIT_TIMELINE + T07-P63/CH08 | T07-P63: 피할 오판=상태 수명 혼동 |
| T07-P63-V10 | T07-P63: latency=1406ms; queue=60; retry=6 | T07-P63: 재발 | T07-P63: 축=재발; 원인 확정은 보류 | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 | T07-P63: 피할 오판=운영 한계 누락 |
| T07-P63-V11 | T07-P63: latency=1473ms; queue=71; retry=2 | T07-P63: 대형 입력 | T07-P63: 축=대형 입력; 원인 확정은 보류 | T07-P63: SIZE_LIMIT + T07-P63/CH08 | T07-P63: 피할 오판=오류 합치기 |
| T07-P63-V12 | T07-P63: latency=1540ms; queue=2; retry=5 | T07-P63: drain | T07-P63: 축=drain; 원인 확정은 보류 | T07-P63: DRAIN_STATE + T07-P63/CH08 | T07-P63: 피할 오판=복구 과잉 |
| T07-P63-V13 | T07-P63: latency=1607ms; queue=13; retry=1 | T07-P63: 복구 범위 | T07-P63: 축=복구 범위; 원인 확정은 보류 | T07-P63: RECOVERY_AUDIT + T07-P63/CH08 | T07-P63: 피할 오판=경계 누락 |
| T07-P63-V14 | T07-P63: latency=1674ms; queue=24; retry=4 | T07-P63: unknown outcome | T07-P63: 축=unknown outcome; 원인 확정은 보류 | T07-P63: PROVIDER_RESULT + T07-P63/CH08 | T07-P63: 피할 오판=증거 혼동 |

## CHAPTER 24 · trace와 span으로 분산 요청 경로 보기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P63에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P63-D01 | T07-P63: 로그 확대 | T07-P63: debug를 위해 payload와 context 기록을 늘린다 | T07-P63: secret·PII·cardinality 비용을 통제하는지 | T07-P63: DRAIN[ready=0; active=8; drain_deadline_s=5] | T07-P63: DRAIN_STATE + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D02 | T07-P63: 강제 종료 | T07-P63: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P63: in-flight request와 background work의 결과를 잃는지 | T07-P63: RECOVERY_SCOPE[selected=180; expected=6; backup=1; dry_run=1] | T07-P63: RECOVERY_AUDIT + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D03 | T07-P63: AI package 추가 | T07-P63: AI가 제안한 새 dependency를 도입한다 | T07-P63: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P63: UNKNOWN_OUTCOME[timeout_ms=271; provider_state=UNKNOWN; lookup_id=p06302] | T07-P63: PROVIDER_RESULT + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D04 | T07-P63: 비동기화 | T07-P63: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P63: durability·status API·worker retry 계약이 생기는지 | T07-P63: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P63: DURABLE_STATE + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D05 | T07-P63: 권한 shortcut | T07-P63: payload의 owner/tenant id를 바로 사용한다 | T07-P63: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P63: OVERLOAD[rps=779; p99_ms=831; queue=33] | T07-P63: QUEUE_PRESSURE + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D06 | T07-P63: 순서 병렬화 | T07-P63: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P63: 선후관계 invariant와 race를 깨지 않는지 | T07-P63: SAMPLING[sample_rate=82%; trace_present=1; metric_present=1] | T07-P63: TRACE_METRIC_CROSSCHECK + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D07 | T07-P63: validation 이동 | T07-P63: validation을 business side effect 뒤로 옮긴다 | T07-P63: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P63: DISCONNECT[disconnect_ms=105; commit_state=UNKNOWN; request=063-06] | T07-P63: COMMIT_TIMELINE + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P63-D08 | T07-P63: batch 확대 | T07-P63: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P63: memory·deadline·부분 실패 범위가 커지는지 | T07-P63: RECURRENCE[occurrence=4; interval_s=45; mitigation_applied=1] | T07-P63: RECURRENCE_TIMELINE + T07-P63/CH08 | T07-P63: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P63: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · trace와 span으로 분산 요청 경로 보기 — 최종 contract와 evidence spine

**최종 계약:** trace는 하나의 요청이 여러 service·DB·queue를 지날 때 구간별 시간을 연결한 기록이다.

**정상 메커니즘:** trace context를 하위 호출에 전달하고 각 작업을 span으로 기록해 parent-child 관계를 만든다.

**대표 실패:** trace id가 끊겨 service별 조각만 보이거나 모든 세부 값을 attribute로 넣어 비용과 개인정보가 커진다.

**검증 evidence:** critical path, span duration, error status, context propagation gap을 본다.

**직접 행동:** span 목록에서 parent 관계를 따라 총 경로와 가장 긴 구간을 찾는다.

**다음 연결:** `SLI·SLO를 backend 계약으로 정하기`.

`trace와 span으로 분산 요청 경로 보기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| SRE | SRE | trace와 span으로 분산 요청 경로 보기의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | trace와 span으로 분산 요청 경로 보기의 개념·실패·운영 판단 교차 확인 |
| OTEL-SEMCONV | OpenTelemetry Semantic Conventions | trace와 span으로 분산 요청 경로 보기의 개념·실패·운영 판단 교차 확인 |
| NODE-DOCS | Node.js Documentation | trace와 span으로 분산 요청 경로 보기의 개념·실패·운영 판단 교차 확인 |

`trace와 span으로 분산 요청 경로 보기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
