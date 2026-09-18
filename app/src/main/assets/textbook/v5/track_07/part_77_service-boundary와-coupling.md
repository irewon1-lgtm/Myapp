# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 06 · 한 서버를 넘어 여러 구성요소가 협력하는 구조 이해하기

### LESSON 02 · service boundary와 coupling

## CHAPTER 01 · service boundary와 coupling — 쉬운 말에서 정확한 계약까지

**출발 개념.** 좋은 경계는 함께 변하는 규칙은 모으고 독립적으로 변할 수 있는 부분은 느슨하게 연결한다.

**아주 쉬운 사건.** 두 service가 같은 table을 직접 수정한다. 이 사건에서는 먼저 **data ownership coupling을 찾는다**.

**왜 필요한가.** 정상 동작은 업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다. 반대로 테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다.

**암기:** `service boundary와 coupling`의 역할 한 줄.

**직접 이해:** `service boundary와 coupling`의 입력·상태·결과 경계.

**AI 위임 가능:** `service boundary와 coupling` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `두 service가 같은 table을 직접 수정한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 cross-service change frequency, shared DB access, synchronous call chain을 측정한다.

## CHAPTER 02 · service boundary와 coupling — 아주 쉬운 예를 한 단계씩 해석

T07-P77: `두 service가 같은 table을 직접 수정한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 두 service가 같은 table을 직접 수정한다 | T07-P77 외부 입력 | T07-P77: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | data ownership coupling을 찾는다 | T07-P77 판단 기준 | T07-P77/CH08 관측표와 대조 |
| 정상 경로 | T07-P77/CH03 M1→M5 | service boundary와 coupling: 완료 시점을 단계별로 분리 | T07-P77: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P77/CH06 테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다. | T07-P77: 깨진 계약 하나를 특정 | service boundary와 coupling: 증상과 원인을 분리 |
| 재검증 | T07-P77/CH10 직접 실행 | T07-P77: 예상값 T07-P077 1,2,1,1 기록 | service boundary와 coupling: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P77/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P77/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · service boundary와 coupling — 내부 메커니즘과 상태 전이

업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 두 service가 같은 table을 직접 수정한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | data ownership coupling을 찾는다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | cross-service change frequency, shared DB access, synchronous call chain을 측정한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 변경 파일-서비스 매핑에서 함께 바뀌는 집합을 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`service boundary와 coupling` 흐름을 framework 이름 없이 설명한다.

막히면 `두 service가 같은 table을 직접 수정한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · service boundary와 coupling — 실전 경계 A

**경계 A.** 좋은 service boundary는 내부 응집도가 높고 다른 service와의 호출 이유가 제한되어야 하며 한 사용자 요청이 작은 service 수십 개를 순서대로 부르는 구조는 높은 coupling 신호다.

T07-P77/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P77에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P77/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P77/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P77): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P77/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P77-A1-547 | T07-P77 조건 | 좋은 service boundary는 내부 응집도가 높고 다른 service와의 호출 이유가 제한되어야 하며 한 사용자 요청이 작은 service 수십 개를 순서대로 부르는 구조는 높은 coupling 신호다. |
| T07-P77-A2-548 | T07-P77 변화점 | T07-P77/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P77-A3-549 | T07-P77 반례 | T07-P77/CH06 대표 실패와 A 위반을 구별 |
| T07-P77-A4-550 | T07-P77 근거 | T07-P77/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P77-A5-551 | T07-P77 재실험 | 두 service가 같은 table을 직접 수정한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · service boundary와 coupling — 실전 경계 B

**경계 B.** 각 service가 자신의 authoritative data를 소유하고 다른 service가 shared DB table을 직접 수정하지 않아야 독립 변경과 invariant 경계를 지킬 수 있다.

T07-P77/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P77에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P77/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P77/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P77): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P77/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P77-B1-578 | T07-P77 조건 | 각 service가 자신의 authoritative data를 소유하고 다른 service가 shared DB table을 직접 수정하지 않아야 독립 변경과 invariant 경계를 지킬 수 있다. |
| T07-P77-B2-579 | T07-P77 독립성 | T07-P77/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P77-B3-580 | T07-P77 상태 | T07-P77/CH03 before·after 위치를 다시 지정 |
| T07-P77-B4-581 | T07-P77 반증 | T07-P77/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P77-B5-582 | T07-P77 적용 | service boundary와 coupling의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · service boundary와 coupling — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다.**

아래 여섯 사례는 T07-P77의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P77-F01 | T07-P77: 대표 실패 | T07-P77: T07-P77/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P77: 현상만 보고 원인을 확정 | T07-P77/CH08 evidence map에서 상태를 대조 |
| T07-P77-F02 | T07-P77: 경계 A 누락 | T07-P77: T07-P77/CH04 경계 A 위반 입력 | T07-P77: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P77/CH08 evidence map에서 상태를 대조 |
| T07-P77-F03 | T07-P77: 경계 B 누락 | T07-P77: T07-P77/CH05 경계 B 위반 입력 | T07-P77: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P77/CH08 evidence map에서 상태를 대조 |
| T07-P77-F04 | T07-P77: 복구 경계 C 누락 | T07-P77: T07-P77/CH07 경계 C 복구 조건 | T07-P77: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P77/CH08 evidence map에서 상태를 대조 |
| T07-P77-F05 | T07-P77: 운영 경계 D 누락 | T07-P77: T07-P77/CH09 경계 D 운영 조건 | T07-P77: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P77/CH08 evidence map에서 상태를 대조 |
| T07-P77-F06 | T07-P77: 증거 없는 결론 | T07-P77: T07-P77/CH02 첫 판단만 존재 | T07-P77: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P77/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P77/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · service boundary와 coupling — 복구 가능한 상태와 수명

**경계 C.** chatty API를 줄이려고 모든 데이터를 한 endpoint에 몰기보다 use-case 단위 aggregate response·event replication 등 실제 latency/consistency 요구에 맞는 계약을 찾는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P77에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P77/CH08 evidence map을 본다. 복구 후에는 T07-P77/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P77에서 이미 확정된 side effect는 T07-P77/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P77-R1-640 | T07-P77 중단 직전 | T07-P77/CH03에서 이미 확정된 상태만 표시 |
| T07-P77-R2-641 | T07-P77 재시작 직후 | chatty API를 줄이려고 모든 데이터를 한 endpoint에 몰기보다 use-case 단위 aggregate response·event replication 등 실제 latency/consistency 요구에 맞는 계약을 찾는다. |
| T07-P77-R3-642 | T07-P77 재검증 | T07-P77/CH08 근거로 중복·누락 여부 확인 |
| T07-P77-R4-643 | T07-P77 재실행 | T07-P77/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · service boundary와 coupling — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 두 service가 같은 table을 직접 수정한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | data ownership coupling을 찾는다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | cross-service change frequency, shared DB access, synchronous call chain을 측정한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`service boundary와 coupling` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · service boundary와 coupling — 운영 한계와 종료 조건

**경계 D.** 서비스를 분리한 뒤도 같은 transaction·release·schema를 항상 동시에 바꿔야 한다면 논리적으로는 여전히 강하게 결합돼 있는지 재평가한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P77/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P77/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P77 과제: 경계 D와 T07-P77/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P77-O1-702 | synthetic-load=202 | T07-P77/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P77-O2-703 | synthetic-budget=852ms | T07-P77 timeout과 unknown outcome을 분리 |
| T07-P77-O3-704 | T07-P77 종료 | 서비스를 분리한 뒤도 같은 transaction·release·schema를 항상 동시에 바꿔야 한다면 논리적으로는 여전히 강하게 결합돼 있는지 재평가한다. |
| T07-P77-O4-705 | T07-P77 완화 | T07-P77/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · service boundary와 coupling — 직접 실행하는 작은 모델

`service boundary와 coupling` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P077 1,2,1,1`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P077";
const limit = 5;
const input = [1, 2, 1, 1];
const accepted = input.filter(value => value <= limit);
console.log(marker, accepted.join(","));
```

기준 출력: `T07-P077 1,2,1,1`.

`service boundary와 coupling`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P77-L1-734 | constmarker="T07-P077"; | T07-P77 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P77-L2-735 | constlimit=5; | T07-P77 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P77-L3-736 | constinput=[1,2,1,1]; | T07-P77 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P77-L4-737 | constaccepted=input.filter(value=>value<=limit); | T07-P77 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P77-L5-738 | console.log(marker,accepted.join(",")); | T07-P77 출력 관측점; 예상 `T07-P077 1,2,1,1`와 비교 |
| T07-P77-LX-823 | T07-P77 실행 기록 | T07-P77 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · service boundary와 coupling — 한 부분만 수정하고 다시 예측

수정 과제: **limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다**.

수정 전은 `T07-P077 1,2,1,1`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P77/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P77-D1-764 | 기준 `T07-P077 1,2,1,1` | T07-P77 수정 전 실행을 먼저 재현 |
| T07-P77-D2-765 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | T07-P77 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P77-D3-766 | T07-P77 새 예측 | T07-P77 실행 전에 출력·상태를 먼저 기록 |
| T07-P77-D4-767 | T07-P77 재실행 | T07-P77/CH10 실제값과 새 예측을 대조 |
| T07-P77-D5-768 | T07-P77 반례 | T07-P77/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P77-D6-769 | T07-P77 근거 | T07-P77/CH08 상태가 설명과 일치해야 완료 |
| T07-P77-D7-770 | T07-P77 이유 | T07-P77 변경 이유를 service boundary와 coupling 계약과 연결해 설명 |

## CHAPTER 12 · service boundary와 coupling — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: service boundary와 coupling | 좋은 경계는 함께 변하는 규칙은 모으고 독립적으로 변할 수 있는 부분은 느슨하게 연결한다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P77/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | cross-service change frequency, shared DB access, synchronous call chain을 측정한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P77/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P77/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P77/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P77/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `service boundary와 coupling` 실행 코드 수정 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | `service boundary와 coupling` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P77/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P77/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · service boundary와 coupling — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 좋은 경계는 함께 변하는 규칙은 모으고 독립적으로 변할 수 있는 부분은 느슨하게 연결한다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P77/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P77/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P77/CH10 실행용 boilerplate·test 후보 | T07-P77: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P077 1,2,1,1` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P77/CH02의 판단 기준과 T07-P77/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P77-AI1-826 | T07-P77 사람 결정 | T07-P77 업무 의미·허용 위험·완료 기준 소유 |
| T07-P77-AI2-827 | T07-P77 AI 초안 | T07-P77/CH10 boilerplate·test 후보까지만 위임 |
| T07-P77-AI3-828 | T07-P77 검증 | T07-P77/CH06 반례와 T07-P77/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · service boundary와 coupling — 경계 조합 실험 8개

T07-P77: T07-P77/CH04~T07-P77/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P77의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-K01 | T07-P77: 경계 A | T07-P77: 경계 B | T07-P77: OLD_SCHEMA[client=v3; server=v4; extra_field=1] / sample=64 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + CLIENT_VERSION |
| T07-P77-K02 | T07-P77: 경계 A | T07-P77: 경계 C | T07-P77: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=77] / sample=71 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + STATE_VERSION |
| T07-P77-K03 | T07-P77: 경계 A | T07-P77: 경계 D | T07-P77: UNKNOWN_OUTCOME[timeout_ms=266; provider_state=UNKNOWN; lookup_id=p07703] / sample=78 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + PROVIDER_RESULT |
| T07-P77-K04 | T07-P77: 경계 B | T07-P77: 경계 C | T07-P77: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=85 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + DURABLE_STATE |
| T07-P77-K05 | T07-P77: 경계 B | T07-P77: 경계 D | T07-P77: OVERLOAD[rps=764; p99_ms=471; queue=34] / sample=92 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + QUEUE_PRESSURE |
| T07-P77-K06 | T07-P77: 경계 C | T07-P77: 경계 D | T07-P77: SAMPLING[sample_rate=42%; trace_present=0; metric_present=1] / sample=99 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P77-K07 | T07-P77: 경계 A | T07-P77: 경계 B+C | T07-P77: DISCONNECT[disconnect_ms=65; commit_state=UNKNOWN; request=077-07] / sample=17 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + COMMIT_TIMELINE |
| T07-P77-K08 | T07-P77: 경계 B | T07-P77: 경계 C+D | T07-P77: RECURRENCE[occurrence=5; interval_s=40; mitigation_applied=1] / sample=24 | T07-P77: 먼저 깨지는 경계를 판정 | T07-P77: T07-P77/CH08 + RECURRENCE_TIMELINE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P77/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · service boundary와 coupling — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P77와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-B01 | T07-P77: monolith·modular monolith·microservice의 경계 | T07-P77: 기능 6개를 한 앱에 둘지 나눌지 고민한다 | T07-P77: deployment와 module boundary를 분리한다 | T07-P77: T07-P77/CH08 증거와 형제 LESSON 증거를 분리 | T07-P77: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P77-B03 | T07-P77: 동기 호출과 비동기 메시지 선택 | T07-P77: 사용자에게 즉시 결과가 필요한지 묻는다 | T07-P77: sync와 async를 요구로 선택한다 | T07-P77: T07-P77/CH08 증거와 형제 LESSON 증거를 분리 | T07-P77: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P77-B04 | T07-P77: reverse proxy·API gateway·BFF의 역할 | T07-P77: gateway에 업무 로직이 계속 늘어난다 | T07-P77: cross-cutting과 domain 책임을 나눈다 | T07-P77: T07-P77/CH08 증거와 형제 LESSON 증거를 분리 | T07-P77: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P77-B05 | T07-P77: load balancing과 service discovery | T07-P77: 새 instance가 추가돼도 traffic이 적다 | T07-P77: connection reuse와 discovery를 본다 | T07-P77: T07-P77/CH08 증거와 형제 LESSON 증거를 분리 | T07-P77: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P77-B06 | T07-P77: 분산 transaction과 saga·compensation | T07-P77: 결제 성공 뒤 재고 실패가 생긴다 | T07-P77: saga step과 compensation을 설계한다 | T07-P77: T07-P77/CH08 증거와 형제 LESSON 증거를 분리 | T07-P77: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · service boundary와 coupling — 선택형 실패 주입 6개

T07-P77: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P77 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-X01 | T07-P77: unknown outcome | T07-P77: dependency timeout 후 성공 여부 불명; sample=410 | T07-P77: 실패와 미확정 결과 | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: provider id·조회 결과·retry history |
| T07-P77-X02 | T07-P77: 재시작 | T07-P77: side effect 직후 process가 재시작됨; sample=427 | T07-P77: durable state와 memory state | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: commit·outbox·job id·restart 전후 상태 |
| T07-P77-X03 | T07-P77: 과부하 | T07-P77: traffic 세 배, p99 급증; sample=444 | T07-P77: 기능 실패와 saturation | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: queue age·pool wait·CPU/event-loop·quota |
| T07-P77-X04 | T07-P77: sampling | T07-P77: 일부 log가 sampling으로 빠짐; sample=461 | T07-P77: 기록 부재와 사건 부재 | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: metric·trace·durable state 교차 근거 |
| T07-P77-X05 | T07-P77: client disconnect | T07-P77: 응답 전에 연결이 끊김; sample=478 | T07-P77: 연결 종료와 server effect | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: commit 시각·worker/outbox·request lifecycle |
| T07-P77-X06 | T07-P77: 재발 | T07-P77: 같은 오류가 잠시 뒤 다시 발생; sample=495 | T07-P77: 완화와 원인 제거 | T07-P77: T07-P77/CH02 판단과 별도 기록 | T07-P77: 재발 timeline·변경점·resource state |

## CHAPTER 17 · service boundary와 coupling — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P77에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-E01 | T07-P77: 대표 실패를 원인으로 착각 | T07-P77: T07-P77/CH06 실패 case를 다른 입력으로 재현 | T07-P77: 현상과 원인을 같은 것으로 봄 | T07-P77: T07-P77/CH06 대표 실패와 T07-P77/CH08 증거를 다시 대조 | T07-P77: T07-P77/CH08 |
| T07-P77-E02 | T07-P77: 경계 A 생략 | T07-P77: T07-P77/CH04의 조건 하나를 반대로 설정 | T07-P77: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P77: T07-P77/CH04를 새 입력에 적용 | T07-P77: T07-P77/CH08 |
| T07-P77-E03 | T07-P77: 경계 B 생략 | T07-P77: T07-P77/CH05의 조건 하나를 반대로 설정 | T07-P77: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P77: T07-P77/CH05를 새 입력에 적용 | T07-P77: T07-P77/CH08 |
| T07-P77-E04 | T07-P77: 복구 상태 혼동 | T07-P77: T07-P77/CH07에서 처리 중단을 주입 | T07-P77: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P77: T07-P77/CH07에서 수명 경계를 다시 표시 | T07-P77: T07-P77/CH08 |
| T07-P77-E05 | T07-P77: 운영 한계 누락 | T07-P77: T07-P77/CH09에서 부하 또는 drain 조건을 변경 | T07-P77: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P77: T07-P77/CH09의 종료 조건을 다시 작성 | T07-P77: T07-P77/CH08 |
| T07-P77-E06 | T07-P77: 증거 없는 성공 판정 | T07-P77: T07-P77/CH08에서 증거 하나를 숨김 | T07-P77: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P77: T07-P77/CH08에서 독립 증거 둘을 선택 | T07-P77: T07-P77/CH08 |

## CHAPTER 18 · service boundary와 coupling — synthetic 관측값 판독 6개

T07-P77: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P77의 숫자 하나만으로 원인을 단정하지 않고 T07-P77/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-O01 | T07-P77/rpc_latency_ms | 964 | T07-P77: service 호출 지연 | T07-P77: 축=client disconnect; 원인 확정 금지 | T07-P77: COMMIT_TIMELINE + T07-P77/CH08 |
| T07-P77-O02 | T07-P77/message_lag | 981 | T07-P77: message lag | T07-P77: 축=재발; 원인 확정 금지 | T07-P77: RECURRENCE_TIMELINE + T07-P77/CH08 |
| T07-P77-O03 | T07-P77/replica_lag_ms | 1 | T07-P77: replica lag | T07-P77: 축=소유권 위조; 원인 확정 금지 | T07-P77: AUTHZ_POLICY + T07-P77/CH08 |
| T07-P77-O04 | T07-P77/leader_changes | 18 | T07-P77: leader 변경 수 | T07-P77: 축=순서 역전; 원인 확정 금지 | T07-P77: SEQUENCE_STATE + T07-P77/CH08 |
| T07-P77-O05 | T07-P77/conflicts | 35 | T07-P77: 분산 상태 충돌 수 | T07-P77: 축=drain; 원인 확정 금지 | T07-P77: DRAIN_STATE + T07-P77/CH08 |
| T07-P77-O06 | T07-P77/failover_ms | 52 | T07-P77: failover 시간 | T07-P77: 축=복구 범위; 원인 확정 금지 | T07-P77: RECOVERY_AUDIT + T07-P77/CH08 |

## CHAPTER 19 · service boundary와 coupling — 선택형 코드 리뷰 6질문

T07-P77: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P77에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-R01 | T07-P77: 관측 | T07-P77: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P77: RECURRENCE[occurrence=2; interval_s=163; mitigation_applied=1] | T07-P77: T07-P77/CH04 | T07-P77: RECURRENCE_TIMELINE |
| T07-P77-R02 | T07-P77: 복구 | T07-P77: 재시작 뒤에도 필요한 상태가 남는가 | T07-P77: OWNER_SPOOF[actor=A0; payload_owner=B1; auth_owner=A0] | T07-P77: T07-P77/CH05 | T07-P77: AUTHZ_POLICY |
| T07-P77-R03 | T07-P77: 중복 | T07-P77: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P77: REORDER[in_seq=5,3,4; applied_version=6] | T07-P77: T07-P77/CH06 | T07-P77: SEQUENCE_STATE |
| T07-P77-R04 | T07-P77: timeout | T07-P77: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P77: DRAIN[ready=0; active=6; drain_deadline_s=8] | T07-P77: T07-P77/CH07 | T07-P77: DRAIN_STATE |
| T07-P77-R05 | T07-P77: 관측 | T07-P77: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P77: RECOVERY_SCOPE[selected=197; expected=11; backup=1; dry_run=0] | T07-P77: T07-P77/CH04 | T07-P77: RECOVERY_AUDIT |
| T07-P77-R06 | T07-P77: 복구 | T07-P77: 재시작 뒤에도 필요한 상태가 남는가 | T07-P77: REPLAY[key=cmd-077-05; attempts=4; response_seen=0] | T07-P77: T07-P77/CH05 | T07-P77: IDEMPOTENCY_RECORD |

## CHAPTER 20 · service boundary와 coupling — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P77에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-I01 | T07-P77: 복구 확인 | T07-P77: durable state와 사용자 결과를 모두 확인 | T07-P77: OWNER_SPOOF[actor=A0; payload_owner=B0; auth_owner=A0] | T07-P77: T07-P77/CH08 + AUTHZ_POLICY | T07-P77-incident-472 |
| T07-P77-I02 | T07-P77: 재주입 | T07-P77: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P77: REORDER[in_seq=4,2,3; applied_version=6] | T07-P77: T07-P77/CH08 + SEQUENCE_STATE | T07-P77-incident-473 |
| T07-P77-I03 | T07-P77: 회귀 고정 | T07-P77: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P77: DRAIN[ready=0; active=10; drain_deadline_s=7] | T07-P77: T07-P77/CH08 + DRAIN_STATE | T07-P77-incident-474 |
| T07-P77-I04 | T07-P77: 영향 범위 | T07-P77: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P77: RECOVERY_SCOPE[selected=186; expected=19; backup=1; dry_run=1] | T07-P77: T07-P77/CH08 + RECOVERY_AUDIT | T07-P77-incident-475 |
| T07-P77-I05 | T07-P77: 변경 동결 | T07-P77: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P77: REPLAY[key=cmd-077-04; attempts=3; response_seen=0] | T07-P77: T07-P77/CH08 + IDEMPOTENCY_RECORD | T07-P77-incident-476 |
| T07-P77-I06 | T07-P77: correlation | T07-P77: 한 request/job/resource id를 시간축에 고정 | T07-P77: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P77: T07-P77/CH08 + CLIENT_VERSION | T07-P77-incident-477 |

## CHAPTER 21 · service boundary와 coupling — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P77/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P77/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P77/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P77/CH04~T07-P77/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P77/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P77/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P77/CH18 signal 두 개와 T07-P77/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P77/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P77/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · service boundary와 coupling — 통합 casebook 16문제

T07-P77 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P77 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P77-C01 | T07-P77: 경계 A | T07-P77: REORDER[in_seq=3,1,2; applied_version=6] | T07-P77: load=533, window=59s | T07-P77: review=상태 변경 | T07-P77: 경계 A 위반 여부를 판정 | T07-P77: SEQUENCE_STATE + T07-P77/CH08 |
| T07-P77-C02 | T07-P77: 경계 B | T07-P77: DRAIN[ready=0; active=14; drain_deadline_s=6] | T07-P77: load=556, window=78s | T07-P77: review=retry | T07-P77: 경계 B 위반 여부를 판정 | T07-P77: DRAIN_STATE + T07-P77/CH08 |
| T07-P77-C03 | T07-P77: 경계 C | T07-P77: RECOVERY_SCOPE[selected=175; expected=6; backup=1; dry_run=0] | T07-P77: load=579, window=97s | T07-P77: review=복구 | T07-P77: 경계 C 위반 여부를 판정 | T07-P77: RECOVERY_AUDIT + T07-P77/CH08 |
| T07-P77-C04 | T07-P77: 경계 D | T07-P77: REPLAY[key=cmd-077-03; attempts=2; response_seen=0] | T07-P77: load=602, window=26s | T07-P77: review=동시성 | T07-P77: 경계 D 위반 여부를 판정 | T07-P77: IDEMPOTENCY_RECORD + T07-P77/CH08 |
| T07-P77-C05 | T07-P77: 경계 A | T07-P77: OLD_SCHEMA[client=v2; server=v3; extra_field=0] | T07-P77: load=625, window=45s | T07-P77: review=민감정보 | T07-P77: 경계 A 위반 여부를 판정 | T07-P77: CLIENT_VERSION + T07-P77/CH08 |
| T07-P77-C06 | T07-P77: 경계 B | T07-P77: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=19] | T07-P77: load=648, window=64s | T07-P77: review=중복 | T07-P77: 경계 B 위반 여부를 판정 | T07-P77: STATE_VERSION + T07-P77/CH08 |
| T07-P77-C07 | T07-P77: 경계 C | T07-P77: UNKNOWN_OUTCOME[timeout_ms=299; provider_state=UNKNOWN; lookup_id=p07706] | T07-P77: load=671, window=83s | T07-P77: review=권한 | T07-P77: 경계 C 위반 여부를 판정 | T07-P77: PROVIDER_RESULT + T07-P77/CH08 |
| T07-P77-C08 | T07-P77: 경계 D | T07-P77: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P77: load=694, window=12s | T07-P77: review=입력 경계 | T07-P77: 경계 D 위반 여부를 판정 | T07-P77: DURABLE_STATE + T07-P77/CH08 |
| T07-P77-C09 | T07-P77: 경계 A | T07-P77: OVERLOAD[rps=230; p99_ms=822; queue=36] | T07-P77: load=717, window=31s | T07-P77: review=timeout | T07-P77: 경계 A 위반 여부를 판정 | T07-P77: QUEUE_PRESSURE + T07-P77/CH08 |
| T07-P77-C10 | T07-P77: 경계 B | T07-P77: SAMPLING[sample_rate=81%; trace_present=1; metric_present=1] | T07-P77: load=740, window=50s | T07-P77: review=자원 | T07-P77: 경계 B 위반 여부를 판정 | T07-P77: TRACE_METRIC_CROSSCHECK + T07-P77/CH08 |
| T07-P77-C11 | T07-P77: 경계 C | T07-P77: DISCONNECT[disconnect_ms=104; commit_state=UNKNOWN; request=077-10] | T07-P77: load=763, window=69s | T07-P77: review=순서 | T07-P77: 경계 C 위반 여부를 판정 | T07-P77: COMMIT_TIMELINE + T07-P77/CH08 |
| T07-P77-C12 | T07-P77: 경계 D | T07-P77: RECURRENCE[occurrence=3; interval_s=73; mitigation_applied=1] | T07-P77: load=786, window=88s | T07-P77: review=관측 | T07-P77: 경계 D 위반 여부를 판정 | T07-P77: RECURRENCE_TIMELINE + T07-P77/CH08 |
| T07-P77-C13 | T07-P77: 경계 A | T07-P77: OWNER_SPOOF[actor=A0; payload_owner=B2; auth_owner=A0] | T07-P77: load=809, window=17s | T07-P77: review=상태 변경 | T07-P77: 경계 A 위반 여부를 판정 | T07-P77: AUTHZ_POLICY + T07-P77/CH08 |
| T07-P77-C14 | T07-P77: 경계 B | T07-P77: REORDER[in_seq=16,14,15; applied_version=6] | T07-P77: load=832, window=36s | T07-P77: review=retry | T07-P77: 경계 B 위반 여부를 판정 | T07-P77: SEQUENCE_STATE + T07-P77/CH08 |
| T07-P77-C15 | T07-P77: 경계 C | T07-P77: DRAIN[ready=0; active=6; drain_deadline_s=11] | T07-P77: load=855, window=55s | T07-P77: review=복구 | T07-P77: 경계 C 위반 여부를 판정 | T07-P77: DRAIN_STATE + T07-P77/CH08 |
| T07-P77-C16 | T07-P77: 경계 D | T07-P77: RECOVERY_SCOPE[selected=107; expected=19; backup=1; dry_run=1] | T07-P77: load=878, window=74s | T07-P77: review=동시성 | T07-P77: 경계 D 위반 여부를 판정 | T07-P77: RECOVERY_AUDIT + T07-P77/CH08 |

채점은 결론보다 근거를 본다. T07-P77/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · service boundary와 coupling — evidence 판독 문제 14개

T07-P77 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P77 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P77-V01 | T07-P77: latency=1377ms; queue=59; retry=0 | T07-P77: drain | T07-P77: 축=drain; 원인 확정은 보류 | T07-P77: DRAIN_STATE + T07-P77/CH08 | T07-P77: 피할 오판=소유권 혼동 |
| T07-P77-V02 | T07-P77: latency=1444ms; queue=70; retry=3 | T07-P77: 복구 범위 | T07-P77: 축=복구 범위; 원인 확정은 보류 | T07-P77: RECOVERY_AUDIT + T07-P77/CH08 | T07-P77: 피할 오판=동시성 무시 |
| T07-P77-V03 | T07-P77: latency=1511ms; queue=1; retry=6 | T07-P77: 재전송 | T07-P77: 축=재전송; 원인 확정은 보류 | T07-P77: IDEMPOTENCY_RECORD + T07-P77/CH08 | T07-P77: 피할 오판=순서 가정 |
| T07-P77-V04 | T07-P77: latency=1578ms; queue=12; retry=2 | T07-P77: 구버전 client | T07-P77: 축=구버전 client; 원인 확정은 보류 | T07-P77: CLIENT_VERSION + T07-P77/CH08 | T07-P77: 피할 오판=상태 수명 혼동 |
| T07-P77-V05 | T07-P77: latency=1645ms; queue=23; retry=5 | T07-P77: 동시 변경 | T07-P77: 축=동시 변경; 원인 확정은 보류 | T07-P77: STATE_VERSION + T07-P77/CH08 | T07-P77: 피할 오판=운영 한계 누락 |
| T07-P77-V06 | T07-P77: latency=1712ms; queue=34; retry=1 | T07-P77: unknown outcome | T07-P77: 축=unknown outcome; 원인 확정은 보류 | T07-P77: PROVIDER_RESULT + T07-P77/CH08 | T07-P77: 피할 오판=오류 합치기 |
| T07-P77-V07 | T07-P77: latency=1779ms; queue=45; retry=4 | T07-P77: 재시작 | T07-P77: 축=재시작; 원인 확정은 보류 | T07-P77: DURABLE_STATE + T07-P77/CH08 | T07-P77: 피할 오판=복구 과잉 |
| T07-P77-V08 | T07-P77: latency=46ms; queue=56; retry=0 | T07-P77: 과부하 | T07-P77: 축=과부하; 원인 확정은 보류 | T07-P77: QUEUE_PRESSURE + T07-P77/CH08 | T07-P77: 피할 오판=잘못된 전제 |
| T07-P77-V09 | T07-P77: latency=113ms; queue=67; retry=3 | T07-P77: sampling | T07-P77: 축=sampling; 원인 확정은 보류 | T07-P77: TRACE_METRIC_CROSSCHECK + T07-P77/CH08 | T07-P77: 피할 오판=경계 누락 |
| T07-P77-V10 | T07-P77: latency=180ms; queue=78; retry=6 | T07-P77: client disconnect | T07-P77: 축=client disconnect; 원인 확정은 보류 | T07-P77: COMMIT_TIMELINE + T07-P77/CH08 | T07-P77: 피할 오판=증거 혼동 |
| T07-P77-V11 | T07-P77: latency=247ms; queue=9; retry=2 | T07-P77: 재발 | T07-P77: 축=재발; 원인 확정은 보류 | T07-P77: RECURRENCE_TIMELINE + T07-P77/CH08 | T07-P77: 피할 오판=재시도 오판 |
| T07-P77-V12 | T07-P77: latency=314ms; queue=20; retry=5 | T07-P77: 소유권 위조 | T07-P77: 축=소유권 위조; 원인 확정은 보류 | T07-P77: AUTHZ_POLICY + T07-P77/CH08 | T07-P77: 피할 오판=소유권 혼동 |
| T07-P77-V13 | T07-P77: latency=381ms; queue=31; retry=1 | T07-P77: 순서 역전 | T07-P77: 축=순서 역전; 원인 확정은 보류 | T07-P77: SEQUENCE_STATE + T07-P77/CH08 | T07-P77: 피할 오판=동시성 무시 |
| T07-P77-V14 | T07-P77: latency=448ms; queue=42; retry=4 | T07-P77: drain | T07-P77: 축=drain; 원인 확정은 보류 | T07-P77: DRAIN_STATE + T07-P77/CH08 | T07-P77: 피할 오판=순서 가정 |

## CHAPTER 24 · service boundary와 coupling — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P77에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P77-D01 | T07-P77: batch 확대 | T07-P77: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P77: memory·deadline·부분 실패 범위가 커지는지 | T07-P77: REPLAY[key=cmd-077-00; attempts=2; response_seen=0] | T07-P77: IDEMPOTENCY_RECORD + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D02 | T07-P77: fallback 추가 | T07-P77: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P77: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P77: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P77: CLIENT_VERSION + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D03 | T07-P77: 외부 호출 이동 | T07-P77: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P77: lock duration과 unknown outcome 경계가 달라지는지 | T07-P77: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=77] | T07-P77: STATE_VERSION + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D04 | T07-P77: cache 추가 | T07-P77: 현재 결과 앞에 cache layer를 추가한다 | T07-P77: stale·key·invalidation 책임이 새로 생기는지 | T07-P77: UNKNOWN_OUTCOME[timeout_ms=266; provider_state=UNKNOWN; lookup_id=p07703] | T07-P77: PROVIDER_RESULT + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D05 | T07-P77: pool 확대 | T07-P77: connection/worker pool 상한을 늘린다 | T07-P77: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P77: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P77: DURABLE_STATE + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D06 | T07-P77: schema 변경 | T07-P77: 필드 이름·형식·required 조건을 바꾼다 | T07-P77: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P77: OVERLOAD[rps=764; p99_ms=471; queue=34] | T07-P77: QUEUE_PRESSURE + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D07 | T07-P77: 결과 합치기 | T07-P77: 여러 오류를 하나의 status/error code로 합친다 | T07-P77: client 행동과 retry 가능성을 잃지 않는지 | T07-P77: SAMPLING[sample_rate=42%; trace_present=0; metric_present=1] | T07-P77: TRACE_METRIC_CROSSCHECK + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P77-D08 | T07-P77: retry 추가 | T07-P77: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P77: unknown outcome과 duplicate side effect를 구분하는지 | T07-P77: DISCONNECT[disconnect_ms=65; commit_state=UNKNOWN; request=077-07] | T07-P77: COMMIT_TIMELINE + T07-P77/CH08 | T07-P77: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P77: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · service boundary와 coupling — 최종 contract와 evidence spine

**최종 계약:** 좋은 경계는 함께 변하는 규칙은 모으고 독립적으로 변할 수 있는 부분은 느슨하게 연결한다.

**정상 메커니즘:** 업무 capability와 data ownership을 기준으로 API/event 계약을 만들고 내부 모델 공유를 줄인다.

**대표 실패:** 테이블 하나마다 service를 만들거나 공용 database로 모든 서비스가 서로 내부 schema를 수정한다.

**검증 evidence:** cross-service change frequency, shared DB access, synchronous call chain을 측정한다.

**직접 행동:** 변경 파일-서비스 매핑에서 함께 바뀌는 집합을 계산한다.

**다음 연결:** `동기 호출과 비동기 메시지 선택`.

`service boundary와 coupling`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | service boundary와 coupling의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | service boundary와 coupling의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | service boundary와 coupling의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | service boundary와 coupling의 개념·실패·운영 판단 교차 확인 |

`service boundary와 coupling` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
