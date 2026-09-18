# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 06 · 한 서버를 넘어 여러 구성요소가 협력하는 구조 이해하기

### LESSON 01 · monolith·modular monolith·microservice의 경계

## CHAPTER 01 · monolith·modular monolith·microservice의 경계 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 배포 단위 수보다 중요한 것은 책임과 데이터 소유 경계, 변경 결합도다.

**아주 쉬운 사건.** 기능 6개를 한 앱에 둘지 나눌지 고민한다. 이 사건에서는 먼저 **deployment와 module boundary를 분리한다**.

**왜 필요한가.** 정상 동작은 한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다. 반대로 작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다.

**암기:** `monolith·modular monolith·microservice의 경계`의 역할 한 줄.

**직접 이해:** `monolith·modular monolith·microservice의 경계`의 입력·상태·결과 경계.

**AI 위임 가능:** `monolith·modular monolith·microservice의 경계` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `기능 6개를 한 앱에 둘지 나눌지 고민한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 변경 동시성, dependency graph, deploy unit, data ownership, cross-boundary call을 본다.

## CHAPTER 02 · monolith·modular monolith·microservice의 경계 — 아주 쉬운 예를 한 단계씩 해석

T07-P76: `기능 6개를 한 앱에 둘지 나눌지 고민한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 기능 6개를 한 앱에 둘지 나눌지 고민한다 | T07-P76 외부 입력 | T07-P76: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | deployment와 module boundary를 분리한다 | T07-P76 판단 기준 | T07-P76/CH08 관측표와 대조 |
| 정상 경로 | T07-P76/CH03 M1→M5 | monolith·modular monolith·microservice의 경계: 완료 시점을 단계별로 분리 | T07-P76: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P76/CH06 작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다. | T07-P76: 깨진 계약 하나를 특정 | monolith·modular monolith·microservice의 경계: 증상과 원인을 분리 |
| 재검증 | T07-P76/CH10 직접 실행 | T07-P76: 예상값 T07-P076 RECEIVED->CHECKED 기록 | monolith·modular monolith·microservice의 경계: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P76/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P76/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · monolith·modular monolith·microservice의 경계 — 내부 메커니즘과 상태 전이

한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 기능 6개를 한 앱에 둘지 나눌지 고민한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | deployment와 module boundary를 분리한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 변경 동시성, dependency graph, deploy unit, data ownership, cross-boundary call을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | module dependency 목록에서 cycle과 과도한 coupling을 찾는다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`monolith·modular monolith·microservice의 경계` 흐름을 framework 이름 없이 설명한다.

막히면 `기능 6개를 한 앱에 둘지 나눌지 고민한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · monolith·modular monolith·microservice의 경계 — 실전 경계 A

**경계 A.** monolith는 하나의 deployment unit 안에 여러 기능이 있고 modular monolith는 그 안에서도 module dependency·data ownership을 명확히 제한하는 구조라 ‘한 process=무질서’는 아니다.

T07-P76/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P76에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P76/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P76/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P76): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P76/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P76-A1-457 | T07-P76 조건 | monolith는 하나의 deployment unit 안에 여러 기능이 있고 modular monolith는 그 안에서도 module dependency·data ownership을 명확히 제한하는 구조라 ‘한 process=무질서’는 아니다. |
| T07-P76-A2-458 | T07-P76 변화점 | T07-P76/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P76-A3-459 | T07-P76 반례 | T07-P76/CH06 대표 실패와 A 위반을 구별 |
| T07-P76-A4-460 | T07-P76 근거 | T07-P76/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P76-A5-461 | T07-P76 재실험 | 기능 6개를 한 앱에 둘지 나눌지 고민한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · monolith·modular monolith·microservice의 경계 — 실전 경계 B

**경계 B.** microservice는 독립 배포·scale 장점을 줄 수 있지만 network failure·distributed tracing·data consistency·deployment 운영 비용을 새로 만든다.

T07-P76/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P76에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P76/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P76/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P76): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P76/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P76-B1-488 | T07-P76 조건 | microservice는 독립 배포·scale 장점을 줄 수 있지만 network failure·distributed tracing·data consistency·deployment 운영 비용을 새로 만든다. |
| T07-P76-B2-489 | T07-P76 독립성 | T07-P76/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P76-B3-490 | T07-P76 상태 | T07-P76/CH03 before·after 위치를 다시 지정 |
| T07-P76-B4-491 | T07-P76 반증 | T07-P76/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P76-B5-492 | T07-P76 적용 | monolith·modular monolith·microservice의 경계의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · monolith·modular monolith·microservice의 경계 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다.**

아래 여섯 사례는 T07-P76의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P76-F01 | T07-P76: 대표 실패 | T07-P76: T07-P76/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P76: 현상만 보고 원인을 확정 | T07-P76/CH08 evidence map에서 상태를 대조 |
| T07-P76-F02 | T07-P76: 경계 A 누락 | T07-P76: T07-P76/CH04 경계 A 위반 입력 | T07-P76: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P76/CH08 evidence map에서 상태를 대조 |
| T07-P76-F03 | T07-P76: 경계 B 누락 | T07-P76: T07-P76/CH05 경계 B 위반 입력 | T07-P76: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P76/CH08 evidence map에서 상태를 대조 |
| T07-P76-F04 | T07-P76: 복구 경계 C 누락 | T07-P76: T07-P76/CH07 경계 C 복구 조건 | T07-P76: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P76/CH08 evidence map에서 상태를 대조 |
| T07-P76-F05 | T07-P76: 운영 경계 D 누락 | T07-P76: T07-P76/CH09 경계 D 운영 조건 | T07-P76: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P76/CH08 evidence map에서 상태를 대조 |
| T07-P76-F06 | T07-P76: 증거 없는 결론 | T07-P76: T07-P76/CH02 첫 판단만 존재 | T07-P76: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P76/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P76/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · monolith·modular monolith·microservice의 경계 — 복구 가능한 상태와 수명

**경계 C.** 서비스를 나눌 기준은 table 개수보다 business ownership·변경 이유·scale 특성·팀 책임이며 서로 항상 함께 바뀌는 기능을 억지로 network로 분리하지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P76에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P76/CH08 evidence map을 본다. 복구 후에는 T07-P76/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P76에서 이미 확정된 side effect는 T07-P76/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P76-R1-550 | T07-P76 중단 직전 | T07-P76/CH03에서 이미 확정된 상태만 표시 |
| T07-P76-R2-551 | T07-P76 재시작 직후 | 서비스를 나눌 기준은 table 개수보다 business ownership·변경 이유·scale 특성·팀 책임이며 서로 항상 함께 바뀌는 기능을 억지로 network로 분리하지 않는다. |
| T07-P76-R3-552 | T07-P76 재검증 | T07-P76/CH08 근거로 중복·누락 여부 확인 |
| T07-P76-R4-553 | T07-P76 재실행 | T07-P76/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · monolith·modular monolith·microservice의 경계 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 기능 6개를 한 앱에 둘지 나눌지 고민한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | deployment와 module boundary를 분리한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 변경 동시성, dependency graph, deploy unit, data ownership, cross-boundary call을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`monolith·modular monolith·microservice의 경계` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · monolith·modular monolith·microservice의 경계 — 운영 한계와 종료 조건

**경계 D.** 초기/중간 규모 제품에서는 modular monolith로 경계를 먼저 검증하고 실제 독립 배포 필요가 증명될 때 일부 module을 분리하는 전략도 충분히 실용적이다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P76/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P76/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P76 과제: 경계 D와 T07-P76/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P76-O1-612 | synthetic-load=112 | T07-P76/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P76-O2-613 | synthetic-budget=762ms | T07-P76 timeout과 unknown outcome을 분리 |
| T07-P76-O3-614 | T07-P76 종료 | 초기/중간 규모 제품에서는 modular monolith로 경계를 먼저 검증하고 실제 독립 배포 필요가 증명될 때 일부 module을 분리하는 전략도 충분히 실용적이다. |
| T07-P76-O4-615 | T07-P76 완화 | T07-P76/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · monolith·modular monolith·microservice의 경계 — 직접 실행하는 작은 모델

`monolith·modular monolith·microservice의 경계` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P076 RECEIVED->CHECKED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P076";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 2;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P076 RECEIVED->CHECKED`.

`monolith·modular monolith·microservice의 경계`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P76-L1-644 | constmarker="T07-P076"; | T07-P76 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P76-L2-645 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P76 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P76-L3-646 | conststopAt=2; | T07-P76 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P76-L4-647 | constvisited=stages.slice(0,stopAt); | T07-P76 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P76-L5-648 | console.log(marker,visited.join("->")); | T07-P76 출력 관측점; 예상 `T07-P076 RECEIVED->CHECKED`와 비교 |
| T07-P76-LX-733 | T07-P76 실행 기록 | T07-P76 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · monolith·modular monolith·microservice의 경계 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P076 RECEIVED->CHECKED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P76/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P76-D1-674 | 기준 `T07-P076 RECEIVED->CHECKED` | T07-P76 수정 전 실행을 먼저 재현 |
| T07-P76-D2-675 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P76 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P76-D3-676 | T07-P76 새 예측 | T07-P76 실행 전에 출력·상태를 먼저 기록 |
| T07-P76-D4-677 | T07-P76 재실행 | T07-P76/CH10 실제값과 새 예측을 대조 |
| T07-P76-D5-678 | T07-P76 반례 | T07-P76/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P76-D6-679 | T07-P76 근거 | T07-P76/CH08 상태가 설명과 일치해야 완료 |
| T07-P76-D7-680 | T07-P76 이유 | T07-P76 변경 이유를 monolith·modular monolith·microservice의 경계 계약과 연결해 설명 |

## CHAPTER 12 · monolith·modular monolith·microservice의 경계 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: monolith·modular monolith·microservice의 경계 | 배포 단위 수보다 중요한 것은 책임과 데이터 소유 경계, 변경 결합도다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P76/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 변경 동시성, dependency graph, deploy unit, data ownership, cross-boundary call을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P76/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P76/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P76/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P76/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `monolith·modular monolith·microservice의 경계` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `monolith·modular monolith·microservice의 경계` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P76/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P76/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · monolith·modular monolith·microservice의 경계 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 배포 단위 수보다 중요한 것은 책임과 데이터 소유 경계, 변경 결합도다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P76/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P76/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P76/CH10 실행용 boilerplate·test 후보 | T07-P76: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P076 RECEIVED->CHECKED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P76/CH02의 판단 기준과 T07-P76/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P76-AI1-736 | T07-P76 사람 결정 | T07-P76 업무 의미·허용 위험·완료 기준 소유 |
| T07-P76-AI2-737 | T07-P76 AI 초안 | T07-P76/CH10 boilerplate·test 후보까지만 위임 |
| T07-P76-AI3-738 | T07-P76 검증 | T07-P76/CH06 반례와 T07-P76/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · monolith·modular monolith·microservice의 경계 — 경계 조합 실험 8개

T07-P76: T07-P76/CH04~T07-P76/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P76의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-K01 | T07-P76: 경계 A | T07-P76: 경계 B | T07-P76: DRAIN[ready=0; active=14; drain_deadline_s=6] / sample=53 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + DRAIN_STATE |
| T07-P76-K02 | T07-P76: 경계 A | T07-P76: 경계 C | T07-P76: RECOVERY_SCOPE[selected=146; expected=8; backup=1; dry_run=0] / sample=60 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + RECOVERY_AUDIT |
| T07-P76-K03 | T07-P76: 경계 A | T07-P76: 경계 D | T07-P76: REPLAY[key=cmd-076-03; attempts=2; response_seen=0] / sample=67 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + IDEMPOTENCY_RECORD |
| T07-P76-K04 | T07-P76: 경계 B | T07-P76: 경계 C | T07-P76: OLD_SCHEMA[client=v1; server=v2; extra_field=0] / sample=74 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + CLIENT_VERSION |
| T07-P76-K05 | T07-P76: 경계 B | T07-P76: 경계 D | T07-P76: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=99] / sample=81 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + STATE_VERSION |
| T07-P76-K06 | T07-P76: 경계 C | T07-P76: 경계 D | T07-P76: UNKNOWN_OUTCOME[timeout_ms=270; provider_state=UNKNOWN; lookup_id=p07606] / sample=88 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + PROVIDER_RESULT |
| T07-P76-K07 | T07-P76: 경계 A | T07-P76: 경계 B+C | T07-P76: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=95 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + DURABLE_STATE |
| T07-P76-K08 | T07-P76: 경계 B | T07-P76: 경계 C+D | T07-P76: OVERLOAD[rps=776; p99_ms=669; queue=19] / sample=13 | T07-P76: 먼저 깨지는 경계를 판정 | T07-P76: T07-P76/CH08 + QUEUE_PRESSURE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P76/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · monolith·modular monolith·microservice의 경계 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P76와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-B02 | T07-P76: service boundary와 coupling | T07-P76: 두 service가 같은 table을 직접 수정한다 | T07-P76: data ownership coupling을 찾는다 | T07-P76: T07-P76/CH08 증거와 형제 LESSON 증거를 분리 | T07-P76: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P76-B03 | T07-P76: 동기 호출과 비동기 메시지 선택 | T07-P76: 사용자에게 즉시 결과가 필요한지 묻는다 | T07-P76: sync와 async를 요구로 선택한다 | T07-P76: T07-P76/CH08 증거와 형제 LESSON 증거를 분리 | T07-P76: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P76-B04 | T07-P76: reverse proxy·API gateway·BFF의 역할 | T07-P76: gateway에 업무 로직이 계속 늘어난다 | T07-P76: cross-cutting과 domain 책임을 나눈다 | T07-P76: T07-P76/CH08 증거와 형제 LESSON 증거를 분리 | T07-P76: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P76-B05 | T07-P76: load balancing과 service discovery | T07-P76: 새 instance가 추가돼도 traffic이 적다 | T07-P76: connection reuse와 discovery를 본다 | T07-P76: T07-P76/CH08 증거와 형제 LESSON 증거를 분리 | T07-P76: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P76-B06 | T07-P76: 분산 transaction과 saga·compensation | T07-P76: 결제 성공 뒤 재고 실패가 생긴다 | T07-P76: saga step과 compensation을 설계한다 | T07-P76: T07-P76/CH08 증거와 형제 LESSON 증거를 분리 | T07-P76: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · monolith·modular monolith·microservice의 경계 — 선택형 실패 주입 6개

T07-P76: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P76 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-X01 | T07-P76: 재전송 | T07-P76: 응답 유실 뒤 같은 command가 다시 도착함; sample=379 | T07-P76: 중복 side effect 여부 | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P76-X02 | T07-P76: 구버전 client | T07-P76: 한 단계 이전 schema가 요청됨; sample=396 | T07-P76: 호환 입력과 breaking change | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: schema version·실제 client 분포·contract test |
| T07-P76-X03 | T07-P76: 동시 변경 | T07-P76: 두 actor가 같은 resource를 수정함; sample=413 | T07-P76: lost update 또는 conflict | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: version·affected rows·lock/wait 기록 |
| T07-P76-X04 | T07-P76: unknown outcome | T07-P76: dependency timeout 후 성공 여부 불명; sample=430 | T07-P76: 실패와 미확정 결과 | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: provider id·조회 결과·retry history |
| T07-P76-X05 | T07-P76: 재시작 | T07-P76: side effect 직후 process가 재시작됨; sample=447 | T07-P76: durable state와 memory state | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: commit·outbox·job id·restart 전후 상태 |
| T07-P76-X06 | T07-P76: 과부하 | T07-P76: traffic 세 배, p99 급증; sample=464 | T07-P76: 기능 실패와 saturation | T07-P76: T07-P76/CH02 판단과 별도 기록 | T07-P76: queue age·pool wait·CPU/event-loop·quota |

## CHAPTER 17 · monolith·modular monolith·microservice의 경계 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P76에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-E01 | T07-P76: 대표 실패를 원인으로 착각 | T07-P76: T07-P76/CH06 실패 case를 다른 입력으로 재현 | T07-P76: 현상과 원인을 같은 것으로 봄 | T07-P76: T07-P76/CH06 대표 실패와 T07-P76/CH08 증거를 다시 대조 | T07-P76: T07-P76/CH08 |
| T07-P76-E02 | T07-P76: 경계 A 생략 | T07-P76: T07-P76/CH04의 조건 하나를 반대로 설정 | T07-P76: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P76: T07-P76/CH04를 새 입력에 적용 | T07-P76: T07-P76/CH08 |
| T07-P76-E03 | T07-P76: 경계 B 생략 | T07-P76: T07-P76/CH05의 조건 하나를 반대로 설정 | T07-P76: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P76: T07-P76/CH05를 새 입력에 적용 | T07-P76: T07-P76/CH08 |
| T07-P76-E04 | T07-P76: 복구 상태 혼동 | T07-P76: T07-P76/CH07에서 처리 중단을 주입 | T07-P76: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P76: T07-P76/CH07에서 수명 경계를 다시 표시 | T07-P76: T07-P76/CH08 |
| T07-P76-E05 | T07-P76: 운영 한계 누락 | T07-P76: T07-P76/CH09에서 부하 또는 drain 조건을 변경 | T07-P76: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P76: T07-P76/CH09의 종료 조건을 다시 작성 | T07-P76: T07-P76/CH08 |
| T07-P76-E06 | T07-P76: 증거 없는 성공 판정 | T07-P76: T07-P76/CH08에서 증거 하나를 숨김 | T07-P76: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P76: T07-P76/CH08에서 독립 증거 둘을 선택 | T07-P76: T07-P76/CH08 |

## CHAPTER 18 · monolith·modular monolith·microservice의 경계 — synthetic 관측값 판독 6개

T07-P76: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P76의 숫자 하나만으로 원인을 단정하지 않고 T07-P76/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-O01 | T07-P76/rpc_latency_ms | 916 | T07-P76: service 호출 지연 | T07-P76: 축=재시작; 원인 확정 금지 | T07-P76: DURABLE_STATE + T07-P76/CH08 |
| T07-P76-O02 | T07-P76/message_lag | 933 | T07-P76: message lag | T07-P76: 축=과부하; 원인 확정 금지 | T07-P76: QUEUE_PRESSURE + T07-P76/CH08 |
| T07-P76-O03 | T07-P76/replica_lag_ms | 950 | T07-P76: replica lag | T07-P76: 축=sampling; 원인 확정 금지 | T07-P76: TRACE_METRIC_CROSSCHECK + T07-P76/CH08 |
| T07-P76-O04 | T07-P76/leader_changes | 967 | T07-P76: leader 변경 수 | T07-P76: 축=client disconnect; 원인 확정 금지 | T07-P76: COMMIT_TIMELINE + T07-P76/CH08 |
| T07-P76-O05 | T07-P76/conflicts | 984 | T07-P76: 분산 상태 충돌 수 | T07-P76: 축=재발; 원인 확정 금지 | T07-P76: RECURRENCE_TIMELINE + T07-P76/CH08 |
| T07-P76-O06 | T07-P76/failover_ms | 4 | T07-P76: failover 시간 | T07-P76: 축=소유권 위조; 원인 확정 금지 | T07-P76: AUTHZ_POLICY + T07-P76/CH08 |

## CHAPTER 19 · monolith·modular monolith·microservice의 경계 — 선택형 코드 리뷰 6질문

T07-P76: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P76에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-R01 | T07-P76: 동시성 | T07-P76: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P76: OVERLOAD[rps=512; p99_ms=606; queue=24] | T07-P76: T07-P76/CH04 | T07-P76: QUEUE_PRESSURE |
| T07-P76-R02 | T07-P76: 권한 | T07-P76: actor·action·resource가 같은 판단 안에 있는가 | T07-P76: SAMPLING[sample_rate=57%; trace_present=1; metric_present=1] | T07-P76: T07-P76/CH05 | T07-P76: TRACE_METRIC_CROSSCHECK |
| T07-P76-R03 | T07-P76: 자원 | T07-P76: pool·queue·memory·connection 상한이 있는가 | T07-P76: DISCONNECT[disconnect_ms=80; commit_state=UNKNOWN; request=076-02] | T07-P76: T07-P76/CH06 | T07-P76: COMMIT_TIMELINE |
| T07-P76-R04 | T07-P76: 상태 변경 | T07-P76: side effect가 어느 줄에서 확정되는가 | T07-P76: RECURRENCE[occurrence=5; interval_s=167; mitigation_applied=1] | T07-P76: T07-P76/CH07 | T07-P76: RECURRENCE_TIMELINE |
| T07-P76-R05 | T07-P76: 동시성 | T07-P76: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P76: OWNER_SPOOF[actor=A6; payload_owner=B4; auth_owner=A6] | T07-P76: T07-P76/CH04 | T07-P76: AUTHZ_POLICY |
| T07-P76-R06 | T07-P76: 권한 | T07-P76: actor·action·resource가 같은 판단 안에 있는가 | T07-P76: REORDER[in_seq=8,6,7; applied_version=5] | T07-P76: T07-P76/CH05 | T07-P76: SEQUENCE_STATE |

## CHAPTER 20 · monolith·modular monolith·microservice의 경계 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P76에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-I01 | T07-P76: 가설 검증 | T07-P76: 원인 후보 하나만 뒤집어 재현 | T07-P76: SAMPLING[sample_rate=44%; trace_present=0; metric_present=1] | T07-P76: T07-P76/CH08 + TRACE_METRIC_CROSSCHECK | T07-P76-incident-453 |
| T07-P76-I02 | T07-P76: 복구 확인 | T07-P76: durable state와 사용자 결과를 모두 확인 | T07-P76: DISCONNECT[disconnect_ms=67; commit_state=UNKNOWN; request=076-01] | T07-P76: T07-P76/CH08 + COMMIT_TIMELINE | T07-P76-incident-454 |
| T07-P76-I03 | T07-P76: 재주입 | T07-P76: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P76: RECURRENCE[occurrence=4; interval_s=156; mitigation_applied=1] | T07-P76: T07-P76/CH08 + RECURRENCE_TIMELINE | T07-P76-incident-455 |
| T07-P76-I04 | T07-P76: 회귀 고정 | T07-P76: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P76: OWNER_SPOOF[actor=A6; payload_owner=B3; auth_owner=A6] | T07-P76: T07-P76/CH08 + AUTHZ_POLICY | T07-P76-incident-456 |
| T07-P76-I05 | T07-P76: 영향 범위 | T07-P76: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P76: REORDER[in_seq=7,5,6; applied_version=5] | T07-P76: T07-P76/CH08 + SEQUENCE_STATE | T07-P76-incident-457 |
| T07-P76-I06 | T07-P76: 변경 동결 | T07-P76: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P76: DRAIN[ready=0; active=15; drain_deadline_s=10] | T07-P76: T07-P76/CH08 + DRAIN_STATE | T07-P76-incident-458 |

## CHAPTER 21 · monolith·modular monolith·microservice의 경계 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P76/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P76/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P76/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P76/CH04~T07-P76/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P76/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P76/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P76/CH18 signal 두 개와 T07-P76/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P76/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P76/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · monolith·modular monolith·microservice의 경계 — 통합 casebook 16문제

T07-P76 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P76 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P76-C01 | T07-P76: 경계 A | T07-P76: DISCONNECT[disconnect_ms=54; commit_state=UNKNOWN; request=076-00] | T07-P76: load=504, window=42s | T07-P76: review=관측 | T07-P76: 경계 A 위반 여부를 판정 | T07-P76: COMMIT_TIMELINE + T07-P76/CH08 |
| T07-P76-C02 | T07-P76: 경계 B | T07-P76: RECURRENCE[occurrence=3; interval_s=145; mitigation_applied=1] | T07-P76: load=527, window=61s | T07-P76: review=상태 변경 | T07-P76: 경계 B 위반 여부를 판정 | T07-P76: RECURRENCE_TIMELINE + T07-P76/CH08 |
| T07-P76-C03 | T07-P76: 경계 C | T07-P76: OWNER_SPOOF[actor=A6; payload_owner=B2; auth_owner=A6] | T07-P76: load=550, window=80s | T07-P76: review=retry | T07-P76: 경계 C 위반 여부를 판정 | T07-P76: AUTHZ_POLICY + T07-P76/CH08 |
| T07-P76-C04 | T07-P76: 경계 D | T07-P76: REORDER[in_seq=6,4,5; applied_version=5] | T07-P76: load=573, window=99s | T07-P76: review=복구 | T07-P76: 경계 D 위반 여부를 판정 | T07-P76: SEQUENCE_STATE + T07-P76/CH08 |
| T07-P76-C05 | T07-P76: 경계 A | T07-P76: DRAIN[ready=0; active=2; drain_deadline_s=9] | T07-P76: load=596, window=28s | T07-P76: review=동시성 | T07-P76: 경계 A 위반 여부를 판정 | T07-P76: DRAIN_STATE + T07-P76/CH08 |
| T07-P76-C06 | T07-P76: 경계 B | T07-P76: RECOVERY_SCOPE[selected=179; expected=9; backup=1; dry_run=1] | T07-P76: load=619, window=47s | T07-P76: review=민감정보 | T07-P76: 경계 B 위반 여부를 판정 | T07-P76: RECOVERY_AUDIT + T07-P76/CH08 |
| T07-P76-C07 | T07-P76: 경계 C | T07-P76: REPLAY[key=cmd-076-06; attempts=2; response_seen=0] | T07-P76: load=642, window=66s | T07-P76: review=중복 | T07-P76: 경계 C 위반 여부를 판정 | T07-P76: IDEMPOTENCY_RECORD + T07-P76/CH08 |
| T07-P76-C08 | T07-P76: 경계 D | T07-P76: OLD_SCHEMA[client=v4; server=v5; extra_field=1] | T07-P76: load=665, window=85s | T07-P76: review=권한 | T07-P76: 경계 D 위반 여부를 판정 | T07-P76: CLIENT_VERSION + T07-P76/CH08 |
| T07-P76-C09 | T07-P76: 경계 A | T07-P76: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=41] | T07-P76: load=688, window=14s | T07-P76: review=입력 경계 | T07-P76: 경계 A 위반 여부를 판정 | T07-P76: STATE_VERSION + T07-P76/CH08 |
| T07-P76-C10 | T07-P76: 경계 B | T07-P76: UNKNOWN_OUTCOME[timeout_ms=303; provider_state=UNKNOWN; lookup_id=p07609] | T07-P76: load=711, window=33s | T07-P76: review=timeout | T07-P76: 경계 B 위반 여부를 판정 | T07-P76: PROVIDER_RESULT + T07-P76/CH08 |
| T07-P76-C11 | T07-P76: 경계 C | T07-P76: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P76: load=734, window=52s | T07-P76: review=자원 | T07-P76: 경계 C 위반 여부를 판정 | T07-P76: DURABLE_STATE + T07-P76/CH08 |
| T07-P76-C12 | T07-P76: 경계 D | T07-P76: OVERLOAD[rps=242; p99_ms=1020; queue=21] | T07-P76: load=757, window=71s | T07-P76: review=순서 | T07-P76: 경계 D 위반 여부를 판정 | T07-P76: QUEUE_PRESSURE + T07-P76/CH08 |
| T07-P76-C13 | T07-P76: 경계 A | T07-P76: SAMPLING[sample_rate=23%; trace_present=0; metric_present=1] | T07-P76: load=780, window=90s | T07-P76: review=관측 | T07-P76: 경계 A 위반 여부를 판정 | T07-P76: TRACE_METRIC_CROSSCHECK + T07-P76/CH08 |
| T07-P76-C14 | T07-P76: 경계 B | T07-P76: DISCONNECT[disconnect_ms=29; commit_state=UNKNOWN; request=076-13] | T07-P76: load=803, window=19s | T07-P76: review=상태 변경 | T07-P76: 경계 B 위반 여부를 판정 | T07-P76: COMMIT_TIMELINE + T07-P76/CH08 |
| T07-P76-C15 | T07-P76: 경계 C | T07-P76: RECURRENCE[occurrence=6; interval_s=77; mitigation_applied=1] | T07-P76: load=826, window=38s | T07-P76: review=retry | T07-P76: 경계 C 위반 여부를 판정 | T07-P76: RECURRENCE_TIMELINE + T07-P76/CH08 |
| T07-P76-C16 | T07-P76: 경계 D | T07-P76: OWNER_SPOOF[actor=A6; payload_owner=B0; auth_owner=A6] | T07-P76: load=849, window=57s | T07-P76: review=복구 | T07-P76: 경계 D 위반 여부를 판정 | T07-P76: AUTHZ_POLICY + T07-P76/CH08 |

채점은 결론보다 근거를 본다. T07-P76/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · monolith·modular monolith·microservice의 경계 — evidence 판독 문제 14개

T07-P76 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P76 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P76-V01 | T07-P76: latency=1336ms; queue=52; retry=6 | T07-P76: 재발 | T07-P76: 축=재발; 원인 확정은 보류 | T07-P76: RECURRENCE_TIMELINE + T07-P76/CH08 | T07-P76: 피할 오판=증거 혼동 |
| T07-P76-V02 | T07-P76: latency=1403ms; queue=63; retry=2 | T07-P76: 소유권 위조 | T07-P76: 축=소유권 위조; 원인 확정은 보류 | T07-P76: AUTHZ_POLICY + T07-P76/CH08 | T07-P76: 피할 오판=재시도 오판 |
| T07-P76-V03 | T07-P76: latency=1470ms; queue=74; retry=5 | T07-P76: 순서 역전 | T07-P76: 축=순서 역전; 원인 확정은 보류 | T07-P76: SEQUENCE_STATE + T07-P76/CH08 | T07-P76: 피할 오판=소유권 혼동 |
| T07-P76-V04 | T07-P76: latency=1537ms; queue=5; retry=1 | T07-P76: drain | T07-P76: 축=drain; 원인 확정은 보류 | T07-P76: DRAIN_STATE + T07-P76/CH08 | T07-P76: 피할 오판=동시성 무시 |
| T07-P76-V05 | T07-P76: latency=1604ms; queue=16; retry=4 | T07-P76: 복구 범위 | T07-P76: 축=복구 범위; 원인 확정은 보류 | T07-P76: RECOVERY_AUDIT + T07-P76/CH08 | T07-P76: 피할 오판=순서 가정 |
| T07-P76-V06 | T07-P76: latency=1671ms; queue=27; retry=0 | T07-P76: 재전송 | T07-P76: 축=재전송; 원인 확정은 보류 | T07-P76: IDEMPOTENCY_RECORD + T07-P76/CH08 | T07-P76: 피할 오판=상태 수명 혼동 |
| T07-P76-V07 | T07-P76: latency=1738ms; queue=38; retry=3 | T07-P76: 구버전 client | T07-P76: 축=구버전 client; 원인 확정은 보류 | T07-P76: CLIENT_VERSION + T07-P76/CH08 | T07-P76: 피할 오판=운영 한계 누락 |
| T07-P76-V08 | T07-P76: latency=1805ms; queue=49; retry=6 | T07-P76: 동시 변경 | T07-P76: 축=동시 변경; 원인 확정은 보류 | T07-P76: STATE_VERSION + T07-P76/CH08 | T07-P76: 피할 오판=오류 합치기 |
| T07-P76-V09 | T07-P76: latency=72ms; queue=60; retry=2 | T07-P76: unknown outcome | T07-P76: 축=unknown outcome; 원인 확정은 보류 | T07-P76: PROVIDER_RESULT + T07-P76/CH08 | T07-P76: 피할 오판=복구 과잉 |
| T07-P76-V10 | T07-P76: latency=139ms; queue=71; retry=5 | T07-P76: 재시작 | T07-P76: 축=재시작; 원인 확정은 보류 | T07-P76: DURABLE_STATE + T07-P76/CH08 | T07-P76: 피할 오판=잘못된 전제 |
| T07-P76-V11 | T07-P76: latency=206ms; queue=2; retry=1 | T07-P76: 과부하 | T07-P76: 축=과부하; 원인 확정은 보류 | T07-P76: QUEUE_PRESSURE + T07-P76/CH08 | T07-P76: 피할 오판=경계 누락 |
| T07-P76-V12 | T07-P76: latency=273ms; queue=13; retry=4 | T07-P76: sampling | T07-P76: 축=sampling; 원인 확정은 보류 | T07-P76: TRACE_METRIC_CROSSCHECK + T07-P76/CH08 | T07-P76: 피할 오판=증거 혼동 |
| T07-P76-V13 | T07-P76: latency=340ms; queue=24; retry=0 | T07-P76: client disconnect | T07-P76: 축=client disconnect; 원인 확정은 보류 | T07-P76: COMMIT_TIMELINE + T07-P76/CH08 | T07-P76: 피할 오판=재시도 오판 |
| T07-P76-V14 | T07-P76: latency=407ms; queue=35; retry=3 | T07-P76: 재발 | T07-P76: 축=재발; 원인 확정은 보류 | T07-P76: RECURRENCE_TIMELINE + T07-P76/CH08 | T07-P76: 피할 오판=소유권 혼동 |

## CHAPTER 24 · monolith·modular monolith·microservice의 경계 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P76에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P76-D01 | T07-P76: retry 추가 | T07-P76: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P76: unknown outcome과 duplicate side effect를 구분하는지 | T07-P76: REORDER[in_seq=3,1,2; applied_version=5] | T07-P76: SEQUENCE_STATE + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D02 | T07-P76: 로그 확대 | T07-P76: debug를 위해 payload와 context 기록을 늘린다 | T07-P76: secret·PII·cardinality 비용을 통제하는지 | T07-P76: DRAIN[ready=0; active=14; drain_deadline_s=6] | T07-P76: DRAIN_STATE + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D03 | T07-P76: 강제 종료 | T07-P76: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P76: in-flight request와 background work의 결과를 잃는지 | T07-P76: RECOVERY_SCOPE[selected=146; expected=8; backup=1; dry_run=0] | T07-P76: RECOVERY_AUDIT + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D04 | T07-P76: AI package 추가 | T07-P76: AI가 제안한 새 dependency를 도입한다 | T07-P76: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P76: REPLAY[key=cmd-076-03; attempts=2; response_seen=0] | T07-P76: IDEMPOTENCY_RECORD + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D05 | T07-P76: 비동기화 | T07-P76: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P76: durability·status API·worker retry 계약이 생기는지 | T07-P76: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P76: CLIENT_VERSION + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D06 | T07-P76: 권한 shortcut | T07-P76: payload의 owner/tenant id를 바로 사용한다 | T07-P76: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P76: CONCURRENT_WRITE[actors=2; base_version=5; writes=2; gap_ms=99] | T07-P76: STATE_VERSION + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D07 | T07-P76: 순서 병렬화 | T07-P76: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P76: 선후관계 invariant와 race를 깨지 않는지 | T07-P76: UNKNOWN_OUTCOME[timeout_ms=270; provider_state=UNKNOWN; lookup_id=p07606] | T07-P76: PROVIDER_RESULT + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P76-D08 | T07-P76: validation 이동 | T07-P76: validation을 business side effect 뒤로 옮긴다 | T07-P76: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P76: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P76: DURABLE_STATE + T07-P76/CH08 | T07-P76: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P76: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · monolith·modular monolith·microservice의 경계 — 최종 contract와 evidence spine

**최종 계약:** 배포 단위 수보다 중요한 것은 책임과 데이터 소유 경계, 변경 결합도다.

**정상 메커니즘:** 한 process 안에서도 module boundary를 지킬 수 있고 서비스 분리는 network·운영 복잡성을 추가한다.

**대표 실패:** 작은 팀이 멋있어 보인다는 이유로 모든 기능을 microservice로 쪼개 distributed failure만 늘린다.

**검증 evidence:** 변경 동시성, dependency graph, deploy unit, data ownership, cross-boundary call을 본다.

**직접 행동:** module dependency 목록에서 cycle과 과도한 coupling을 찾는다.

**다음 연결:** `service boundary와 coupling`.

`monolith·modular monolith·microservice의 경계`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | monolith·modular monolith·microservice의 경계의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | monolith·modular monolith·microservice의 경계의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | monolith·modular monolith·microservice의 경계의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | monolith·modular monolith·microservice의 경계의 개념·실패·운영 판단 교차 확인 |

`monolith·modular monolith·microservice의 경계` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
