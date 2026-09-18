# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 03 · 인증·권한·신뢰 경계를 백엔드 흐름에 넣기

### LESSON 08 · allowlist로 입력·출력 필드를 좁히기

## CHAPTER 01 · allowlist로 입력·출력 필드를 좁히기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 객체 전체를 그대로 bind/serialize하지 않고 필요한 필드만 명시하면 예상하지 못한 권한·정보 노출을 줄일 수 있다.

**아주 쉬운 사건.** update payload에 role=admin이 섞인다. 이 사건에서는 먼저 **allowlist DTO로 server-owned field를 막는다**.

**왜 필요한가.** 정상 동작은 create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다. 반대로 isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다.

**암기:** `allowlist로 입력·출력 필드를 좁히기`의 역할 한 줄.

**직접 이해:** `allowlist로 입력·출력 필드를 좁히기`의 입력·상태·결과 경계.

**AI 위임 가능:** `allowlist로 입력·출력 필드를 좁히기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `update payload에 role=admin이 섞인다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 입력 unknown field, 출력 field set, serializer 설정과 schema diff를 확인한다.

## CHAPTER 02 · allowlist로 입력·출력 필드를 좁히기 — 아주 쉬운 예를 한 단계씩 해석

T07-P38: `update payload에 role=admin이 섞인다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | update payload에 role=admin이 섞인다 | T07-P38 외부 입력 | T07-P38: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | allowlist DTO로 server-owned field를 막는다 | T07-P38 판단 기준 | T07-P38/CH08 관측표와 대조 |
| 정상 경로 | T07-P38/CH03 M1→M5 | allowlist로 입력·출력 필드를 좁히기: 완료 시점을 단계별로 분리 | T07-P38: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P38/CH06 isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다. | T07-P38: 깨진 계약 하나를 특정 | allowlist로 입력·출력 필드를 좁히기: 증상과 원인을 분리 |
| 재검증 | T07-P38/CH10 직접 실행 | T07-P38: 예상값 T07-P038 RECEIVED->CHECKED->APPLIED 기록 | allowlist로 입력·출력 필드를 좁히기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P38/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P38/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · allowlist로 입력·출력 필드를 좁히기 — 내부 메커니즘과 상태 전이

create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | update payload에 role=admin이 섞인다 | source/actor/size를 보존 |
| M2 | 경계 판단 | allowlist DTO로 server-owned field를 막는다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 입력 unknown field, 출력 field set, serializer 설정과 schema diff를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 객체에서 공개 허용 필드만 골라 새 응답을 만든다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`allowlist로 입력·출력 필드를 좁히기` 흐름을 framework 이름 없이 설명한다.

막히면 `update payload에 role=admin이 섞인다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · allowlist로 입력·출력 필드를 좁히기 — 실전 경계 A

**경계 A.** update DTO는 client가 바꿀 수 있는 field만 allowlist로 정의해 admin·ownerId·price 같은 server-owned field가 payload에 들어와도 적용되지 않게 한다.

T07-P38/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P38에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P38/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P38/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P38): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P38/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P38-A1-343 | T07-P38 조건 | update DTO는 client가 바꿀 수 있는 field만 allowlist로 정의해 admin·ownerId·price 같은 server-owned field가 payload에 들어와도 적용되지 않게 한다. |
| T07-P38-A2-344 | T07-P38 변화점 | T07-P38/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P38-A3-345 | T07-P38 반례 | T07-P38/CH06 대표 실패와 A 위반을 구별 |
| T07-P38-A4-346 | T07-P38 근거 | T07-P38/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P38-A5-347 | T07-P38 재실험 | update payload에 role=admin이 섞인다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · allowlist로 입력·출력 필드를 좁히기 — 실전 경계 B

**경계 B.** response DTO도 allowlist가 필요하다. DB entity 전체를 serialize하면 password hash·internal note·tenant key 같은 field가 새로 추가될 때 자동으로 API에 노출될 수 있다.

T07-P38/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P38에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P38/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P38/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P38): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P38/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P38-B1-374 | T07-P38 조건 | response DTO도 allowlist가 필요하다. DB entity 전체를 serialize하면 password hash·internal note·tenant key 같은 field가 새로 추가될 때 자동으로 API에 노출될 수 있다. |
| T07-P38-B2-375 | T07-P38 독립성 | T07-P38/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P38-B3-376 | T07-P38 상태 | T07-P38/CH03 before·after 위치를 다시 지정 |
| T07-P38-B4-377 | T07-P38 반증 | T07-P38/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P38-B5-378 | T07-P38 적용 | allowlist로 입력·출력 필드를 좁히기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · allowlist로 입력·출력 필드를 좁히기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다.**

아래 여섯 사례는 T07-P38의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P38-F01 | T07-P38: 대표 실패 | T07-P38: T07-P38/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P38: 현상만 보고 원인을 확정 | T07-P38/CH08 evidence map에서 상태를 대조 |
| T07-P38-F02 | T07-P38: 경계 A 누락 | T07-P38: T07-P38/CH04 경계 A 위반 입력 | T07-P38: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P38/CH08 evidence map에서 상태를 대조 |
| T07-P38-F03 | T07-P38: 경계 B 누락 | T07-P38: T07-P38/CH05 경계 B 위반 입력 | T07-P38: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P38/CH08 evidence map에서 상태를 대조 |
| T07-P38-F04 | T07-P38: 복구 경계 C 누락 | T07-P38: T07-P38/CH07 경계 C 복구 조건 | T07-P38: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P38/CH08 evidence map에서 상태를 대조 |
| T07-P38-F05 | T07-P38: 운영 경계 D 누락 | T07-P38: T07-P38/CH09 경계 D 운영 조건 | T07-P38: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P38/CH08 evidence map에서 상태를 대조 |
| T07-P38-F06 | T07-P38: 증거 없는 결론 | T07-P38: T07-P38/CH02 첫 판단만 존재 | T07-P38: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P38/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P38/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · allowlist로 입력·출력 필드를 좁히기 — 복구 가능한 상태와 수명

**경계 C.** unknown field를 reject할지 ignore할지 정책을 명시하면 typo를 빨리 잡는 strict API와 forward compatibility를 원하는 API 중 목적에 맞게 선택할 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P38에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P38/CH08 evidence map을 본다. 복구 후에는 T07-P38/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P38에서 이미 확정된 side effect는 T07-P38/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P38-R1-436 | T07-P38 중단 직전 | T07-P38/CH03에서 이미 확정된 상태만 표시 |
| T07-P38-R2-437 | T07-P38 재시작 직후 | unknown field를 reject할지 ignore할지 정책을 명시하면 typo를 빨리 잡는 strict API와 forward compatibility를 원하는 API 중 목적에 맞게 선택할 수 있다. |
| T07-P38-R3-438 | T07-P38 재검증 | T07-P38/CH08 근거로 중복·누락 여부 확인 |
| T07-P38-R4-439 | T07-P38 재실행 | T07-P38/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · allowlist로 입력·출력 필드를 좁히기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | update payload에 role=admin이 섞인다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | allowlist DTO로 server-owned field를 막는다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 입력 unknown field, 출력 field set, serializer 설정과 schema diff를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`allowlist로 입력·출력 필드를 좁히기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · allowlist로 입력·출력 필드를 좁히기 — 운영 한계와 종료 조건

**경계 D.** 중첩 object도 top-level만 검사하지 않고 각 depth에서 허용 field와 size를 제한해 예상하지 못한 구조가 그대로 persistence layer로 흘러가지 않게 한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P38/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P38/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P38 과제: 경계 D와 T07-P38/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P38-O1-498 | synthetic-load=178 | T07-P38/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P38-O2-499 | synthetic-budget=648ms | T07-P38 timeout과 unknown outcome을 분리 |
| T07-P38-O3-500 | T07-P38 종료 | 중첩 object도 top-level만 검사하지 않고 각 depth에서 허용 field와 size를 제한해 예상하지 못한 구조가 그대로 persistence layer로 흘러가지 않게 한다. |
| T07-P38-O4-501 | T07-P38 완화 | T07-P38/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · allowlist로 입력·출력 필드를 좁히기 — 직접 실행하는 작은 모델

`allowlist로 입력·출력 필드를 좁히기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P038 RECEIVED->CHECKED->APPLIED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P038";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 3;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P038 RECEIVED->CHECKED->APPLIED`.

`allowlist로 입력·출력 필드를 좁히기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P38-L1-530 | constmarker="T07-P038"; | T07-P38 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P38-L2-531 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P38 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P38-L3-532 | conststopAt=3; | T07-P38 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P38-L4-533 | constvisited=stages.slice(0,stopAt); | T07-P38 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P38-L5-534 | console.log(marker,visited.join("->")); | T07-P38 출력 관측점; 예상 `T07-P038 RECEIVED->CHECKED->APPLIED`와 비교 |
| T07-P38-LX-619 | T07-P38 실행 기록 | T07-P38 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · allowlist로 입력·출력 필드를 좁히기 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P038 RECEIVED->CHECKED->APPLIED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P38/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P38-D1-560 | 기준 `T07-P038 RECEIVED->CHECKED->APPLIED` | T07-P38 수정 전 실행을 먼저 재현 |
| T07-P38-D2-561 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P38 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P38-D3-562 | T07-P38 새 예측 | T07-P38 실행 전에 출력·상태를 먼저 기록 |
| T07-P38-D4-563 | T07-P38 재실행 | T07-P38/CH10 실제값과 새 예측을 대조 |
| T07-P38-D5-564 | T07-P38 반례 | T07-P38/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P38-D6-565 | T07-P38 근거 | T07-P38/CH08 상태가 설명과 일치해야 완료 |
| T07-P38-D7-566 | T07-P38 이유 | T07-P38 변경 이유를 allowlist로 입력·출력 필드를 좁히기 계약과 연결해 설명 |

## CHAPTER 12 · allowlist로 입력·출력 필드를 좁히기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: allowlist로 입력·출력 필드를 좁히기 | 객체 전체를 그대로 bind/serialize하지 않고 필요한 필드만 명시하면 예상하지 못한 권한·정보 노출을 줄일 수 있다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P38/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 입력 unknown field, 출력 field set, serializer 설정과 schema diff를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P38/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P38/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P38/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P38/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `allowlist로 입력·출력 필드를 좁히기` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `allowlist로 입력·출력 필드를 좁히기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P38/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P38/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · allowlist로 입력·출력 필드를 좁히기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 객체 전체를 그대로 bind/serialize하지 않고 필요한 필드만 명시하면 예상하지 못한 권한·정보 노출을 줄일 수 있다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P38/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P38/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P38/CH10 실행용 boilerplate·test 후보 | T07-P38: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P038 RECEIVED->CHECKED->APPLIED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P38/CH02의 판단 기준과 T07-P38/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P38-AI1-622 | T07-P38 사람 결정 | T07-P38 업무 의미·허용 위험·완료 기준 소유 |
| T07-P38-AI2-623 | T07-P38 AI 초안 | T07-P38/CH10 boilerplate·test 후보까지만 위임 |
| T07-P38-AI3-624 | T07-P38 검증 | T07-P38/CH06 반례와 T07-P38/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · allowlist로 입력·출력 필드를 좁히기 — 경계 조합 실험 8개

T07-P38: T07-P38/CH04~T07-P38/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P38의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-K01 | T07-P38: 경계 A | T07-P38: 경계 B | T07-P38: OWNER_SPOOF[actor=A3; payload_owner=B1; auth_owner=A3] / sample=80 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + AUTHZ_POLICY |
| T07-P38-K02 | T07-P38: 경계 A | T07-P38: 경계 C | T07-P38: REORDER[in_seq=5,3,4; applied_version=3] / sample=87 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + SEQUENCE_STATE |
| T07-P38-K03 | T07-P38: 경계 A | T07-P38: 경계 D | T07-P38: RECOVERY_SCOPE[selected=110; expected=14; backup=1; dry_run=1] / sample=94 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + RECOVERY_AUDIT |
| T07-P38-K04 | T07-P38: 경계 B | T07-P38: 경계 C | T07-P38: REPLAY[key=cmd-038-04; attempts=3; response_seen=0] / sample=12 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + IDEMPOTENCY_RECORD |
| T07-P38-K05 | T07-P38: 경계 B | T07-P38: 경계 D | T07-P38: OLD_SCHEMA[client=v4; server=v5; extra_field=1] / sample=19 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + CLIENT_VERSION |
| T07-P38-K06 | T07-P38: 경계 C | T07-P38: 경계 D | T07-P38: UNKNOWN_OUTCOME[timeout_ms=223; provider_state=UNKNOWN; lookup_id=p03806] / sample=26 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + PROVIDER_RESULT |
| T07-P38-K07 | T07-P38: 경계 A | T07-P38: 경계 B+C | T07-P38: OVERLOAD[rps=602; p99_ms=849; queue=18] / sample=33 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + QUEUE_PRESSURE |
| T07-P38-K08 | T07-P38: 경계 B | T07-P38: 경계 C+D | T07-P38: SAMPLING[sample_rate=84%; trace_present=0; metric_present=1] / sample=40 | T07-P38: 먼저 깨지는 경계를 판정 | T07-P38: T07-P38/CH08 + TRACE_METRIC_CROSSCHECK |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P38/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · allowlist로 입력·출력 필드를 좁히기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P38와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-B07 | T07-P38: secret와 configuration을 코드에서 분리하기 | T07-P38: API key가 config 파일과 로그에 보인다 | T07-P38: secret lifecycle과 redaction을 본다 | T07-P38: T07-P38/CH08 증거와 형제 LESSON 증거를 분리 | T07-P38: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P38-B09 | T07-P38: 민감정보를 로그와 오류에서 제거하기 | T07-P38: error log에 Authorization header가 남는다 | T07-P38: 관측성과 민감정보 최소화를 함께 지킨다 | T07-P38: T07-P38/CH08 증거와 형제 LESSON 증거를 분리 | T07-P38: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P38-B06 | T07-P38: CORS와 CSRF를 백엔드 경계에서 구분하기 | T07-P38: 다른 origin의 browser가 credential 요청을 보낸다 | T07-P38: CORS와 CSRF 질문을 분리한다 | T07-P38: T07-P38/CH08 증거와 형제 LESSON 증거를 분리 | T07-P38: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P38-B10 | T07-P38: login abuse와 rate limit | T07-P38: 같은 계정으로 100개 IP가 로그인 시도한다 | T07-P38: rate signal과 lockout DoS를 함께 본다 | T07-P38: T07-P38/CH08 증거와 형제 LESSON 증거를 분리 | T07-P38: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P38-B05 | T07-P38: API key와 service identity | T07-P38: service key가 필요 이상의 scope를 가진다 | T07-P38: caller identity와 least privilege를 본다 | T07-P38: T07-P38/CH08 증거와 형제 LESSON 증거를 분리 | T07-P38: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · allowlist로 입력·출력 필드를 좁히기 — 선택형 실패 주입 6개

T07-P38: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P38 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-X01 | T07-P38: 복구 범위 | T07-P38: 복구 script 대상이 예상보다 큼; sample=198 | T07-P38: 진단과 destructive recovery | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: selected ids/count·backup·audit trail |
| T07-P38-X02 | T07-P38: 재전송 | T07-P38: 응답 유실 뒤 같은 command가 다시 도착함; sample=215 | T07-P38: 중복 side effect 여부 | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P38-X03 | T07-P38: 구버전 client | T07-P38: 한 단계 이전 schema가 요청됨; sample=232 | T07-P38: 호환 입력과 breaking change | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: schema version·실제 client 분포·contract test |
| T07-P38-X04 | T07-P38: unknown outcome | T07-P38: dependency timeout 후 성공 여부 불명; sample=249 | T07-P38: 실패와 미확정 결과 | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: provider id·조회 결과·retry history |
| T07-P38-X05 | T07-P38: 과부하 | T07-P38: traffic 세 배, p99 급증; sample=266 | T07-P38: 기능 실패와 saturation | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: queue age·pool wait·CPU/event-loop·quota |
| T07-P38-X06 | T07-P38: sampling | T07-P38: 일부 log가 sampling으로 빠짐; sample=283 | T07-P38: 기록 부재와 사건 부재 | T07-P38: T07-P38/CH02 판단과 별도 기록 | T07-P38: metric·trace·durable state 교차 근거 |

## CHAPTER 17 · allowlist로 입력·출력 필드를 좁히기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P38에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-E01 | T07-P38: 대표 실패를 원인으로 착각 | T07-P38: T07-P38/CH06 실패 case를 다른 입력으로 재현 | T07-P38: 현상과 원인을 같은 것으로 봄 | T07-P38: T07-P38/CH06 대표 실패와 T07-P38/CH08 증거를 다시 대조 | T07-P38: T07-P38/CH08 |
| T07-P38-E02 | T07-P38: 경계 A 생략 | T07-P38: T07-P38/CH04의 조건 하나를 반대로 설정 | T07-P38: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P38: T07-P38/CH04를 새 입력에 적용 | T07-P38: T07-P38/CH08 |
| T07-P38-E03 | T07-P38: 경계 B 생략 | T07-P38: T07-P38/CH05의 조건 하나를 반대로 설정 | T07-P38: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P38: T07-P38/CH05를 새 입력에 적용 | T07-P38: T07-P38/CH08 |
| T07-P38-E04 | T07-P38: 복구 상태 혼동 | T07-P38: T07-P38/CH07에서 처리 중단을 주입 | T07-P38: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P38: T07-P38/CH07에서 수명 경계를 다시 표시 | T07-P38: T07-P38/CH08 |
| T07-P38-E05 | T07-P38: 운영 한계 누락 | T07-P38: T07-P38/CH09에서 부하 또는 drain 조건을 변경 | T07-P38: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P38: T07-P38/CH09의 종료 조건을 다시 작성 | T07-P38: T07-P38/CH08 |
| T07-P38-E06 | T07-P38: 증거 없는 성공 판정 | T07-P38: T07-P38/CH08에서 증거 하나를 숨김 | T07-P38: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P38: T07-P38/CH08에서 독립 증거 둘을 선택 | T07-P38: T07-P38/CH08 |

## CHAPTER 18 · allowlist로 입력·출력 필드를 좁히기 — synthetic 관측값 판독 6개

T07-P38: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P38의 숫자 하나만으로 원인을 단정하지 않고 T07-P38/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-O01 | T07-P38/auth_failures | 492 | T07-P38: 인증 실패 수 | T07-P38: 축=과부하; 원인 확정 금지 | T07-P38: QUEUE_PRESSURE + T07-P38/CH08 |
| T07-P38-O02 | T07-P38/policy_denials | 509 | T07-P38: 권한 거부 수 | T07-P38: 축=sampling; 원인 확정 금지 | T07-P38: TRACE_METRIC_CROSSCHECK + T07-P38/CH08 |
| T07-P38-O03 | T07-P38/validation_rejects | 526 | T07-P38: 입력 거부 수 | T07-P38: 축=client disconnect; 원인 확정 금지 | T07-P38: COMMIT_TIMELINE + T07-P38/CH08 |
| T07-P38-O04 | T07-P38/rate_limited | 543 | T07-P38: rate-limit 적용 수 | T07-P38: 축=재발; 원인 확정 금지 | T07-P38: RECURRENCE_TIMELINE + T07-P38/CH08 |
| T07-P38-O05 | T07-P38/redactions | 560 | T07-P38: 민감정보 마스킹 수 | T07-P38: 축=대형 입력; 원인 확정 금지 | T07-P38: SIZE_LIMIT + T07-P38/CH08 |
| T07-P38-O06 | T07-P38/suspicious_requests | 577 | T07-P38: 의심 요청 수 | T07-P38: 축=소유권 위조; 원인 확정 금지 | T07-P38: AUTHZ_POLICY + T07-P38/CH08 |

## CHAPTER 19 · allowlist로 입력·출력 필드를 좁히기 — 선택형 코드 리뷰 6질문

T07-P38: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P38에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-R01 | T07-P38: 관측 | T07-P38: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P38: SAMPLING[sample_rate=77%; trace_present=0; metric_present=1] | T07-P38: T07-P38/CH04 | T07-P38: TRACE_METRIC_CROSSCHECK |
| T07-P38-R02 | T07-P38: 복구 | T07-P38: 재시작 뒤에도 필요한 상태가 남는가 | T07-P38: DISCONNECT[disconnect_ms=100; commit_state=UNKNOWN; request=038-01] | T07-P38: T07-P38/CH05 | T07-P38: COMMIT_TIMELINE |
| T07-P38-R03 | T07-P38: 중복 | T07-P38: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P38: RECURRENCE[occurrence=4; interval_s=109; mitigation_applied=1] | T07-P38: T07-P38/CH06 | T07-P38: RECURRENCE_TIMELINE |
| T07-P38-R04 | T07-P38: timeout | T07-P38: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P38: LARGE_INPUT[body_kb=1232; limit_kb=640; parsed=0] | T07-P38: T07-P38/CH07 | T07-P38: SIZE_LIMIT |
| T07-P38-R05 | T07-P38: 관측 | T07-P38: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P38: OWNER_SPOOF[actor=A3; payload_owner=B4; auth_owner=A3] | T07-P38: T07-P38/CH04 | T07-P38: AUTHZ_POLICY |
| T07-P38-R06 | T07-P38: 복구 | T07-P38: 재시작 뒤에도 필요한 상태가 남는가 | T07-P38: REORDER[in_seq=8,6,7; applied_version=3] | T07-P38: T07-P38/CH05 | T07-P38: SEQUENCE_STATE |

## CHAPTER 20 · allowlist로 입력·출력 필드를 좁히기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P38에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-I01 | T07-P38: 재주입 | T07-P38: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P38: DISCONNECT[disconnect_ms=87; commit_state=UNKNOWN; request=038-00] | T07-P38: T07-P38/CH08 + COMMIT_TIMELINE | T07-P38-incident-722 |
| T07-P38-I02 | T07-P38: 회귀 고정 | T07-P38: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P38: RECURRENCE[occurrence=3; interval_s=98; mitigation_applied=1] | T07-P38: T07-P38/CH08 + RECURRENCE_TIMELINE | T07-P38-incident-723 |
| T07-P38-I03 | T07-P38: 영향 범위 | T07-P38: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P38: LARGE_INPUT[body_kb=1144; limit_kb=640; parsed=0] | T07-P38: T07-P38/CH08 + SIZE_LIMIT | T07-P38-incident-724 |
| T07-P38-I04 | T07-P38: 변경 동결 | T07-P38: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P38: OWNER_SPOOF[actor=A3; payload_owner=B3; auth_owner=A3] | T07-P38: T07-P38/CH08 + AUTHZ_POLICY | T07-P38-incident-725 |
| T07-P38-I05 | T07-P38: correlation | T07-P38: 한 request/job/resource id를 시간축에 고정 | T07-P38: REORDER[in_seq=7,5,6; applied_version=3] | T07-P38: T07-P38/CH08 + SEQUENCE_STATE | T07-P38-incident-726 |
| T07-P38-I06 | T07-P38: 마지막 정상 | T07-P38: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P38: RECOVERY_SCOPE[selected=132; expected=21; backup=1; dry_run=1] | T07-P38: T07-P38/CH08 + RECOVERY_AUDIT | T07-P38-incident-727 |

## CHAPTER 21 · allowlist로 입력·출력 필드를 좁히기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P38/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P38/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P38/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P38/CH04~T07-P38/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P38/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P38/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P38/CH18 signal 두 개와 T07-P38/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P38/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P38/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · allowlist로 입력·출력 필드를 좁히기 — 통합 casebook 16문제

T07-P38 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P38 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P38-C01 | T07-P38: 경계 A | T07-P38: RECURRENCE[occurrence=2; interval_s=87; mitigation_applied=1] | T07-P38: load=302, window=26s | T07-P38: review=자원 | T07-P38: 경계 A 위반 여부를 판정 | T07-P38: RECURRENCE_TIMELINE + T07-P38/CH08 |
| T07-P38-C02 | T07-P38: 경계 B | T07-P38: LARGE_INPUT[body_kb=1056; limit_kb=640; parsed=0] | T07-P38: load=325, window=45s | T07-P38: review=순서 | T07-P38: 경계 B 위반 여부를 판정 | T07-P38: SIZE_LIMIT + T07-P38/CH08 |
| T07-P38-C03 | T07-P38: 경계 C | T07-P38: OWNER_SPOOF[actor=A3; payload_owner=B2; auth_owner=A3] | T07-P38: load=348, window=64s | T07-P38: review=관측 | T07-P38: 경계 C 위반 여부를 판정 | T07-P38: AUTHZ_POLICY + T07-P38/CH08 |
| T07-P38-C04 | T07-P38: 경계 D | T07-P38: REORDER[in_seq=6,4,5; applied_version=3] | T07-P38: load=371, window=83s | T07-P38: review=상태 변경 | T07-P38: 경계 D 위반 여부를 판정 | T07-P38: SEQUENCE_STATE + T07-P38/CH08 |
| T07-P38-C05 | T07-P38: 경계 A | T07-P38: RECOVERY_SCOPE[selected=121; expected=8; backup=1; dry_run=0] | T07-P38: load=394, window=12s | T07-P38: review=retry | T07-P38: 경계 A 위반 여부를 판정 | T07-P38: RECOVERY_AUDIT + T07-P38/CH08 |
| T07-P38-C06 | T07-P38: 경계 B | T07-P38: REPLAY[key=cmd-038-05; attempts=4; response_seen=0] | T07-P38: load=417, window=31s | T07-P38: review=복구 | T07-P38: 경계 B 위반 여부를 판정 | T07-P38: IDEMPOTENCY_RECORD + T07-P38/CH08 |
| T07-P38-C07 | T07-P38: 경계 C | T07-P38: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P38: load=440, window=50s | T07-P38: review=동시성 | T07-P38: 경계 C 위반 여부를 판정 | T07-P38: CLIENT_VERSION + T07-P38/CH08 |
| T07-P38-C08 | T07-P38: 경계 D | T07-P38: UNKNOWN_OUTCOME[timeout_ms=234; provider_state=UNKNOWN; lookup_id=p03807] | T07-P38: load=463, window=69s | T07-P38: review=민감정보 | T07-P38: 경계 D 위반 여부를 판정 | T07-P38: PROVIDER_RESULT + T07-P38/CH08 |
| T07-P38-C09 | T07-P38: 경계 A | T07-P38: OVERLOAD[rps=635; p99_ms=966; queue=32] | T07-P38: load=486, window=88s | T07-P38: review=중복 | T07-P38: 경계 A 위반 여부를 판정 | T07-P38: QUEUE_PRESSURE + T07-P38/CH08 |
| T07-P38-C10 | T07-P38: 경계 B | T07-P38: SAMPLING[sample_rate=17%; trace_present=1; metric_present=1] | T07-P38: load=509, window=17s | T07-P38: review=권한 | T07-P38: 경계 B 위반 여부를 판정 | T07-P38: TRACE_METRIC_CROSSCHECK + T07-P38/CH08 |
| T07-P38-C11 | T07-P38: 경계 C | T07-P38: DISCONNECT[disconnect_ms=23; commit_state=UNKNOWN; request=038-10] | T07-P38: load=532, window=36s | T07-P38: review=입력 경계 | T07-P38: 경계 C 위반 여부를 판정 | T07-P38: COMMIT_TIMELINE + T07-P38/CH08 |
| T07-P38-C12 | T07-P38: 경계 D | T07-P38: RECURRENCE[occurrence=3; interval_s=208; mitigation_applied=1] | T07-P38: load=555, window=55s | T07-P38: review=timeout | T07-P38: 경계 D 위반 여부를 판정 | T07-P38: RECURRENCE_TIMELINE + T07-P38/CH08 |
| T07-P38-C13 | T07-P38: 경계 A | T07-P38: LARGE_INPUT[body_kb=2024; limit_kb=640; parsed=0] | T07-P38: load=578, window=74s | T07-P38: review=자원 | T07-P38: 경계 A 위반 여부를 판정 | T07-P38: SIZE_LIMIT + T07-P38/CH08 |
| T07-P38-C14 | T07-P38: 경계 B | T07-P38: OWNER_SPOOF[actor=A3; payload_owner=B3; auth_owner=A3] | T07-P38: load=601, window=93s | T07-P38: review=순서 | T07-P38: 경계 B 위반 여부를 판정 | T07-P38: AUTHZ_POLICY + T07-P38/CH08 |
| T07-P38-C15 | T07-P38: 경계 C | T07-P38: REORDER[in_seq=17,15,16; applied_version=3] | T07-P38: load=624, window=22s | T07-P38: review=관측 | T07-P38: 경계 C 위반 여부를 판정 | T07-P38: SEQUENCE_STATE + T07-P38/CH08 |
| T07-P38-C16 | T07-P38: 경계 D | T07-P38: RECOVERY_SCOPE[selected=31; expected=16; backup=1; dry_run=1] | T07-P38: load=647, window=41s | T07-P38: review=상태 변경 | T07-P38: 경계 D 위반 여부를 판정 | T07-P38: RECOVERY_AUDIT + T07-P38/CH08 |

채점은 결론보다 근거를 본다. T07-P38/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · allowlist로 입력·출력 필드를 좁히기 — evidence 판독 문제 14개

T07-P38 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P38 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P38-V01 | T07-P38: latency=1578ms; queue=26; retry=3 | T07-P38: 대형 입력 | T07-P38: 축=대형 입력; 원인 확정은 보류 | T07-P38: SIZE_LIMIT + T07-P38/CH08 | T07-P38: 피할 오판=소유권 혼동 |
| T07-P38-V02 | T07-P38: latency=1645ms; queue=37; retry=6 | T07-P38: 소유권 위조 | T07-P38: 축=소유권 위조; 원인 확정은 보류 | T07-P38: AUTHZ_POLICY + T07-P38/CH08 | T07-P38: 피할 오판=운영 한계 누락 |
| T07-P38-V03 | T07-P38: latency=1712ms; queue=48; retry=2 | T07-P38: 순서 역전 | T07-P38: 축=순서 역전; 원인 확정은 보류 | T07-P38: SEQUENCE_STATE + T07-P38/CH08 | T07-P38: 피할 오판=오류 합치기 |
| T07-P38-V04 | T07-P38: latency=1779ms; queue=59; retry=5 | T07-P38: 복구 범위 | T07-P38: 축=복구 범위; 원인 확정은 보류 | T07-P38: RECOVERY_AUDIT + T07-P38/CH08 | T07-P38: 피할 오판=AI 과신 |
| T07-P38-V05 | T07-P38: latency=46ms; queue=70; retry=1 | T07-P38: 재전송 | T07-P38: 축=재전송; 원인 확정은 보류 | T07-P38: IDEMPOTENCY_RECORD + T07-P38/CH08 | T07-P38: 피할 오판=복구 과잉 |
| T07-P38-V06 | T07-P38: latency=113ms; queue=1; retry=4 | T07-P38: 구버전 client | T07-P38: 축=구버전 client; 원인 확정은 보류 | T07-P38: CLIENT_VERSION + T07-P38/CH08 | T07-P38: 피할 오판=잘못된 전제 |
| T07-P38-V07 | T07-P38: latency=180ms; queue=12; retry=0 | T07-P38: unknown outcome | T07-P38: 축=unknown outcome; 원인 확정은 보류 | T07-P38: PROVIDER_RESULT + T07-P38/CH08 | T07-P38: 피할 오판=경계 누락 |
| T07-P38-V08 | T07-P38: latency=247ms; queue=23; retry=3 | T07-P38: 과부하 | T07-P38: 축=과부하; 원인 확정은 보류 | T07-P38: QUEUE_PRESSURE + T07-P38/CH08 | T07-P38: 피할 오판=증거 혼동 |
| T07-P38-V09 | T07-P38: latency=314ms; queue=34; retry=6 | T07-P38: sampling | T07-P38: 축=sampling; 원인 확정은 보류 | T07-P38: TRACE_METRIC_CROSSCHECK + T07-P38/CH08 | T07-P38: 피할 오판=소유권 혼동 |
| T07-P38-V10 | T07-P38: latency=381ms; queue=45; retry=2 | T07-P38: client disconnect | T07-P38: 축=client disconnect; 원인 확정은 보류 | T07-P38: COMMIT_TIMELINE + T07-P38/CH08 | T07-P38: 피할 오판=운영 한계 누락 |
| T07-P38-V11 | T07-P38: latency=448ms; queue=56; retry=5 | T07-P38: 재발 | T07-P38: 축=재발; 원인 확정은 보류 | T07-P38: RECURRENCE_TIMELINE + T07-P38/CH08 | T07-P38: 피할 오판=오류 합치기 |
| T07-P38-V12 | T07-P38: latency=515ms; queue=67; retry=1 | T07-P38: 대형 입력 | T07-P38: 축=대형 입력; 원인 확정은 보류 | T07-P38: SIZE_LIMIT + T07-P38/CH08 | T07-P38: 피할 오판=AI 과신 |
| T07-P38-V13 | T07-P38: latency=582ms; queue=78; retry=4 | T07-P38: 소유권 위조 | T07-P38: 축=소유권 위조; 원인 확정은 보류 | T07-P38: AUTHZ_POLICY + T07-P38/CH08 | T07-P38: 피할 오판=복구 과잉 |
| T07-P38-V14 | T07-P38: latency=649ms; queue=9; retry=0 | T07-P38: 순서 역전 | T07-P38: 축=순서 역전; 원인 확정은 보류 | T07-P38: SEQUENCE_STATE + T07-P38/CH08 | T07-P38: 피할 오판=잘못된 전제 |

## CHAPTER 24 · allowlist로 입력·출력 필드를 좁히기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P38에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P38-D01 | T07-P38: cache 추가 | T07-P38: 현재 결과 앞에 cache layer를 추가한다 | T07-P38: stale·key·invalidation 책임이 새로 생기는지 | T07-P38: REORDER[in_seq=3,1,2; applied_version=3] | T07-P38: SEQUENCE_STATE + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D02 | T07-P38: pool 확대 | T07-P38: connection/worker pool 상한을 늘린다 | T07-P38: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P38: RECOVERY_SCOPE[selected=88; expected=9; backup=1; dry_run=1] | T07-P38: RECOVERY_AUDIT + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D03 | T07-P38: schema 변경 | T07-P38: 필드 이름·형식·required 조건을 바꾼다 | T07-P38: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P38: REPLAY[key=cmd-038-02; attempts=4; response_seen=0] | T07-P38: IDEMPOTENCY_RECORD + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D04 | T07-P38: 결과 합치기 | T07-P38: 여러 오류를 하나의 status/error code로 합친다 | T07-P38: client 행동과 retry 가능성을 잃지 않는지 | T07-P38: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P38: CLIENT_VERSION + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D05 | T07-P38: retry 추가 | T07-P38: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P38: unknown outcome과 duplicate side effect를 구분하는지 | T07-P38: UNKNOWN_OUTCOME[timeout_ms=201; provider_state=UNKNOWN; lookup_id=p03804] | T07-P38: PROVIDER_RESULT + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D06 | T07-P38: 로그 확대 | T07-P38: debug를 위해 payload와 context 기록을 늘린다 | T07-P38: secret·PII·cardinality 비용을 통제하는지 | T07-P38: OVERLOAD[rps=536; p99_ms=615; queue=30] | T07-P38: QUEUE_PRESSURE + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D07 | T07-P38: 강제 종료 | T07-P38: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P38: in-flight request와 background work의 결과를 잃는지 | T07-P38: SAMPLING[sample_rate=58%; trace_present=0; metric_present=1] | T07-P38: TRACE_METRIC_CROSSCHECK + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P38-D08 | T07-P38: AI package 추가 | T07-P38: AI가 제안한 새 dependency를 도입한다 | T07-P38: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P38: DISCONNECT[disconnect_ms=81; commit_state=UNKNOWN; request=038-07] | T07-P38: COMMIT_TIMELINE + T07-P38/CH08 | T07-P38: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P38: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · allowlist로 입력·출력 필드를 좁히기 — 최종 contract와 evidence spine

**최종 계약:** 객체 전체를 그대로 bind/serialize하지 않고 필요한 필드만 명시하면 예상하지 못한 권한·정보 노출을 줄일 수 있다.

**정상 메커니즘:** create/update DTO와 public response DTO를 분리해 writable/readable field 집합을 계약으로 둔다.

**대표 실패:** isAdmin 같은 내부 필드가 body에 섞여 수정되거나 passwordHash가 response에 노출된다.

**검증 evidence:** 입력 unknown field, 출력 field set, serializer 설정과 schema diff를 확인한다.

**직접 행동:** 객체에서 공개 허용 필드만 골라 새 응답을 만든다.

**다음 연결:** `민감정보를 로그와 오류에서 제거하기`.

`allowlist로 입력·출력 필드를 좁히기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| OWASP-API | OWASP-API | allowlist로 입력·출력 필드를 좁히기의 개념·실패·운영 판단 교차 확인 |
| OWASP-ASVS | OWASP-ASVS | allowlist로 입력·출력 필드를 좁히기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | allowlist로 입력·출력 필드를 좁히기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | allowlist로 입력·출력 필드를 좁히기의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | allowlist로 입력·출력 필드를 좁히기의 개념·실패·운영 판단 교차 확인 |

`allowlist로 입력·출력 필드를 좁히기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
