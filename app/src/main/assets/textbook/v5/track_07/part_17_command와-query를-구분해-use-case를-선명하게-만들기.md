# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 02 · 업무 규칙과 상태 변화가 깨지지 않게 만들기

### LESSON 02 · command와 query를 구분해 use case를 선명하게 만들기

## CHAPTER 01 · command와 query를 구분해 use case를 선명하게 만들기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** command는 상태를 바꾸려는 의도이고 query는 현재 정보를 읽으려는 요청으로 구분하면 책임이 선명해진다.

**아주 쉬운 사건.** 조회 endpoint가 lastSeen을 갱신한다. 이 사건에서는 먼저 **query와 업무 side effect를 구분한다**.

**왜 필요한가.** 정상 동작은 use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다. 반대로 읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다.

**암기:** `command와 query를 구분해 use case를 선명하게 만들기`의 역할 한 줄.

**직접 이해:** `command와 query를 구분해 use case를 선명하게 만들기`의 입력·상태·결과 경계.

**AI 위임 가능:** `command와 query를 구분해 use case를 선명하게 만들기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `조회 endpoint가 lastSeen을 갱신한다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 route별 write 여부, 호출한 의존성, 반환 shape를 추적한다.

## CHAPTER 02 · command와 query를 구분해 use case를 선명하게 만들기 — 아주 쉬운 예를 한 단계씩 해석

T07-P17: `조회 endpoint가 lastSeen을 갱신한다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 조회 endpoint가 lastSeen을 갱신한다 | T07-P17 외부 입력 | T07-P17: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | query와 업무 side effect를 구분한다 | T07-P17 판단 기준 | T07-P17/CH08 관측표와 대조 |
| 정상 경로 | T07-P17/CH03 M1→M5 | command와 query를 구분해 use case를 선명하게 만들기: 완료 시점을 단계별로 분리 | T07-P17: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P17/CH06 읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다. | T07-P17: 깨진 계약 하나를 특정 | command와 query를 구분해 use case를 선명하게 만들기: 증상과 원인을 분리 |
| 재검증 | T07-P17/CH10 직접 실행 | T07-P17: 예상값 T07-P017 1,2,4,1 기록 | command와 query를 구분해 use case를 선명하게 만들기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P17/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P17/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · command와 query를 구분해 use case를 선명하게 만들기 — 내부 메커니즘과 상태 전이

use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 조회 endpoint가 lastSeen을 갱신한다 | source/actor/size를 보존 |
| M2 | 경계 판단 | query와 업무 side effect를 구분한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | route별 write 여부, 호출한 의존성, 반환 shape를 추적한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | command/query 목록을 분류하고 허용되지 않은 side effect를 찾는 코드를 실행한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`command와 query를 구분해 use case를 선명하게 만들기` 흐름을 framework 이름 없이 설명한다.

막히면 `조회 endpoint가 lastSeen을 갱신한다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · command와 query를 구분해 use case를 선명하게 만들기 — 실전 경계 A

**경계 A.** query는 읽기라고 약속했는데 마지막 조회 시각을 갱신하거나 cache warm-up을 business side effect로 섞으면 retry·cache·권한 판단이 어려워질 수 있어 관측용 부수효과와 업무 변경을 구분한다.

T07-P17/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P17에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P17/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P17/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P17): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P17/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P17-A1-552 | T07-P17 조건 | query는 읽기라고 약속했는데 마지막 조회 시각을 갱신하거나 cache warm-up을 business side effect로 섞으면 retry·cache·권한 판단이 어려워질 수 있어 관측용 부수효과와 업무 변경을 구분한다. |
| T07-P17-A2-553 | T07-P17 변화점 | T07-P17/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P17-A3-554 | T07-P17 반례 | T07-P17/CH06 대표 실패와 A 위반을 구별 |
| T07-P17-A4-555 | T07-P17 근거 | T07-P17/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P17-A5-556 | T07-P17 재실험 | 조회 endpoint가 lastSeen을 갱신한다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · command와 query를 구분해 use case를 선명하게 만들기 — 실전 경계 B

**경계 B.** command input은 사용자가 바꾸려는 의도만 담고 DB row 전체를 받지 않게 해 server-owned field가 client payload로 덮이는 것을 막는다.

T07-P17/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P17에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P17/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P17/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P17): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P17/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P17-B1-583 | T07-P17 조건 | command input은 사용자가 바꾸려는 의도만 담고 DB row 전체를 받지 않게 해 server-owned field가 client payload로 덮이는 것을 막는다. |
| T07-P17-B2-584 | T07-P17 독립성 | T07-P17/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P17-B3-585 | T07-P17 상태 | T07-P17/CH03 before·after 위치를 다시 지정 |
| T07-P17-B4-586 | T07-P17 반증 | T07-P17/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P17-B5-587 | T07-P17 적용 | command와 query를 구분해 use case를 선명하게 만들기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · command와 query를 구분해 use case를 선명하게 만들기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다.**

아래 여섯 사례는 T07-P17의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P17-F01 | T07-P17: 대표 실패 | T07-P17: T07-P17/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P17: 현상만 보고 원인을 확정 | T07-P17/CH08 evidence map에서 상태를 대조 |
| T07-P17-F02 | T07-P17: 경계 A 누락 | T07-P17: T07-P17/CH04 경계 A 위반 입력 | T07-P17: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P17/CH08 evidence map에서 상태를 대조 |
| T07-P17-F03 | T07-P17: 경계 B 누락 | T07-P17: T07-P17/CH05 경계 B 위반 입력 | T07-P17: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P17/CH08 evidence map에서 상태를 대조 |
| T07-P17-F04 | T07-P17: 복구 경계 C 누락 | T07-P17: T07-P17/CH07 경계 C 복구 조건 | T07-P17: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P17/CH08 evidence map에서 상태를 대조 |
| T07-P17-F05 | T07-P17: 운영 경계 D 누락 | T07-P17: T07-P17/CH09 경계 D 운영 조건 | T07-P17: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P17/CH08 evidence map에서 상태를 대조 |
| T07-P17-F06 | T07-P17: 증거 없는 결론 | T07-P17: T07-P17/CH02 첫 판단만 존재 | T07-P17: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P17/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P17/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · command와 query를 구분해 use case를 선명하게 만들기 — 복구 가능한 상태와 수명

**경계 C.** 읽기 화면이 필요로 하는 projection은 write model과 다를 수 있어 query DTO를 별도로 만들 수 있으며 이것만으로 CQRS 같은 큰 architecture를 도입해야 하는 것은 아니다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P17에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P17/CH08 evidence map을 본다. 복구 후에는 T07-P17/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P17에서 이미 확정된 side effect는 T07-P17/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P17-R1-645 | T07-P17 중단 직전 | T07-P17/CH03에서 이미 확정된 상태만 표시 |
| T07-P17-R2-646 | T07-P17 재시작 직후 | 읽기 화면이 필요로 하는 projection은 write model과 다를 수 있어 query DTO를 별도로 만들 수 있으며 이것만으로 CQRS 같은 큰 architecture를 도입해야 하는 것은 아니다. |
| T07-P17-R3-647 | T07-P17 재검증 | T07-P17/CH08 근거로 중복·누락 여부 확인 |
| T07-P17-R4-648 | T07-P17 재실행 | T07-P17/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · command와 query를 구분해 use case를 선명하게 만들기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 조회 endpoint가 lastSeen을 갱신한다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | query와 업무 side effect를 구분한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | route별 write 여부, 호출한 의존성, 반환 shape를 추적한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`command와 query를 구분해 use case를 선명하게 만들기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · command와 query를 구분해 use case를 선명하게 만들기 — 운영 한계와 종료 조건

**경계 D.** command와 query를 분리해도 같은 domain vocabulary와 authorization 규칙을 공유할 수 있으므로 파일을 나누는 것보다 side effect contract가 실제 기준이다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P17/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P17/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P17 과제: 경계 D와 T07-P17/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P17-O1-707 | synthetic-load=207 | T07-P17/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P17-O2-708 | synthetic-budget=857ms | T07-P17 timeout과 unknown outcome을 분리 |
| T07-P17-O3-709 | T07-P17 종료 | command와 query를 분리해도 같은 domain vocabulary와 authorization 규칙을 공유할 수 있으므로 파일을 나누는 것보다 side effect contract가 실제 기준이다. |
| T07-P17-O4-710 | T07-P17 완화 | T07-P17/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · command와 query를 구분해 use case를 선명하게 만들기 — 직접 실행하는 작은 모델

`command와 query를 구분해 use case를 선명하게 만들기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P017 1,2,4,1`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P017";
const limit = 5;
const input = [1, 2, 4, 1];
const accepted = input.filter(value => value <= limit);
console.log(marker, accepted.join(","));
```

기준 출력: `T07-P017 1,2,4,1`.

`command와 query를 구분해 use case를 선명하게 만들기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P17-L1-739 | constmarker="T07-P017"; | T07-P17 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P17-L2-740 | constlimit=5; | T07-P17 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P17-L3-741 | constinput=[1,2,4,1]; | T07-P17 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P17-L4-742 | constaccepted=input.filter(value=>value<=limit); | T07-P17 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P17-L5-743 | console.log(marker,accepted.join(",")); | T07-P17 출력 관측점; 예상 `T07-P017 1,2,4,1`와 비교 |
| T07-P17-LX-828 | T07-P17 실행 기록 | T07-P17 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · command와 query를 구분해 use case를 선명하게 만들기 — 한 부분만 수정하고 다시 예측

수정 과제: **limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다**.

수정 전은 `T07-P017 1,2,4,1`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P17/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P17-D1-769 | 기준 `T07-P017 1,2,4,1` | T07-P17 수정 전 실행을 먼저 재현 |
| T07-P17-D2-770 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | T07-P17 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P17-D3-771 | T07-P17 새 예측 | T07-P17 실행 전에 출력·상태를 먼저 기록 |
| T07-P17-D4-772 | T07-P17 재실행 | T07-P17/CH10 실제값과 새 예측을 대조 |
| T07-P17-D5-773 | T07-P17 반례 | T07-P17/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P17-D6-774 | T07-P17 근거 | T07-P17/CH08 상태가 설명과 일치해야 완료 |
| T07-P17-D7-775 | T07-P17 이유 | T07-P17 변경 이유를 command와 query를 구분해 use case를 선명하게 만들기 계약과 연결해 설명 |

## CHAPTER 12 · command와 query를 구분해 use case를 선명하게 만들기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: command와 query를 구분해 use case를 선명하게 만들기 | command는 상태를 바꾸려는 의도이고 query는 현재 정보를 읽으려는 요청으로 구분하면 책임이 선명해진다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P17/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | route별 write 여부, 호출한 의존성, 반환 shape를 추적한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P17/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P17/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P17/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P17/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `command와 query를 구분해 use case를 선명하게 만들기` 실행 코드 수정 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | `command와 query를 구분해 use case를 선명하게 만들기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P17/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P17/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · command와 query를 구분해 use case를 선명하게 만들기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | command는 상태를 바꾸려는 의도이고 query는 현재 정보를 읽으려는 요청으로 구분하면 책임이 선명해진다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P17/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P17/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P17/CH10 실행용 boilerplate·test 후보 | T07-P17: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P017 1,2,4,1` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P17/CH02의 판단 기준과 T07-P17/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P17-AI1-831 | T07-P17 사람 결정 | T07-P17 업무 의미·허용 위험·완료 기준 소유 |
| T07-P17-AI2-832 | T07-P17 AI 초안 | T07-P17/CH10 boilerplate·test 후보까지만 위임 |
| T07-P17-AI3-833 | T07-P17 검증 | T07-P17/CH06 반례와 T07-P17/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · command와 query를 구분해 use case를 선명하게 만들기 — 경계 조합 실험 8개

T07-P17: T07-P17/CH04~T07-P17/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P17의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-K01 | T07-P17: 경계 A | T07-P17: 경계 B | T07-P17: DISCONNECT[disconnect_ms=34; commit_state=UNKNOWN; request=017-01] / sample=27 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + COMMIT_TIMELINE |
| T07-P17-K02 | T07-P17: 경계 A | T07-P17: 경계 C | T07-P17: RECURRENCE[occurrence=4; interval_s=133; mitigation_applied=1] / sample=34 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + RECURRENCE_TIMELINE |
| T07-P17-K03 | T07-P17: 경계 A | T07-P17: 경계 D | T07-P17: LARGE_INPUT[body_kb=1424; limit_kb=512; parsed=0] / sample=41 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + SIZE_LIMIT |
| T07-P17-K04 | T07-P17: 경계 B | T07-P17: 경계 C | T07-P17: REORDER[in_seq=7,5,6; applied_version=6] / sample=48 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + SEQUENCE_STATE |
| T07-P17-K05 | T07-P17: 경계 B | T07-P17: 경계 D | T07-P17: RECOVERY_SCOPE[selected=156; expected=14; backup=1; dry_run=1] / sample=55 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + RECOVERY_AUDIT |
| T07-P17-K06 | T07-P17: 경계 C | T07-P17: 경계 D | T07-P17: REPLAY[key=cmd-017-06; attempts=2; response_seen=0] / sample=62 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + IDEMPOTENCY_RECORD |
| T07-P17-K07 | T07-P17: 경계 A | T07-P17: 경계 B+C | T07-P17: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=69 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + CLIENT_VERSION |
| T07-P17-K08 | T07-P17: 경계 B | T07-P17: 경계 C+D | T07-P17: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=8] / sample=76 | T07-P17: 먼저 깨지는 경계를 판정 | T07-P17: T07-P17/CH08 + STATE_VERSION |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P17/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · command와 query를 구분해 use case를 선명하게 만들기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P17와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-B01 | T07-P17: domain model과 invariant | T07-P17: 재고가 1인데 2개를 빼려 한다 | T07-P17: invariant가 invalid state를 막아야 한다 | T07-P17: T07-P17/CH08 증거와 형제 LESSON 증거를 분리 | T07-P17: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P17-B03 | T07-P17: idempotency로 같은 요청이 다시 와도 결과를 통제하기 | T07-P17: 같은 결제 요청이 두 번 도착한다 | T07-P17: NEW·REPLAY·CONFLICT를 나눈다 | T07-P17: T07-P17/CH08 증거와 형제 LESSON 증거를 분리 | T07-P17: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P17-B04 | T07-P17: state machine으로 가능한 상태 전이를 제한하기 | T07-P17: PAID 주문에 PAY event가 다시 온다 | T07-P17: state transition table로 거부한다 | T07-P17: T07-P17/CH08 증거와 형제 LESSON 증거를 분리 | T07-P17: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P17-B05 | T07-P17: 돈·수량·비율에서 정밀도 규칙 정하기 | T07-P17: 10% 할인과 세금 반올림 순서가 다르다 | T07-P17: minor unit과 rounding policy를 고정한다 | T07-P17: T07-P17/CH08 증거와 형제 LESSON 증거를 분리 | T07-P17: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P17-B06 | T07-P17: 시간·timezone·deadline을 업무 데이터로 다루기 | T07-P17: 자정 경계와 다른 timezone이 만난다 | T07-P17: instant와 local date를 분리한다 | T07-P17: T07-P17/CH08 증거와 형제 LESSON 증거를 분리 | T07-P17: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · command와 query를 구분해 use case를 선명하게 만들기 — 선택형 실패 주입 6개

T07-P17: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P17 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-X01 | T07-P17: 대형 입력 | T07-P17: 입력 크기가 정상의 100배; sample=544 | T07-P17: 의미 검증과 resource limit | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: body/batch size·parse time·memory·reject status |
| T07-P17-X02 | T07-P17: 순서 역전 | T07-P17: event가 원래 순서와 반대로 도착; sample=561 | T07-P17: 수신 순서와 업무 순서 | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: version/sequence·dedupe id·applied state |
| T07-P17-X03 | T07-P17: 복구 범위 | T07-P17: 복구 script 대상이 예상보다 큼; sample=578 | T07-P17: 진단과 destructive recovery | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: selected ids/count·backup·audit trail |
| T07-P17-X04 | T07-P17: 재전송 | T07-P17: 응답 유실 뒤 같은 command가 다시 도착함; sample=595 | T07-P17: 중복 side effect 여부 | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P17-X05 | T07-P17: 구버전 client | T07-P17: 한 단계 이전 schema가 요청됨; sample=612 | T07-P17: 호환 입력과 breaking change | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: schema version·실제 client 분포·contract test |
| T07-P17-X06 | T07-P17: 동시 변경 | T07-P17: 두 actor가 같은 resource를 수정함; sample=629 | T07-P17: lost update 또는 conflict | T07-P17: T07-P17/CH02 판단과 별도 기록 | T07-P17: version·affected rows·lock/wait 기록 |

## CHAPTER 17 · command와 query를 구분해 use case를 선명하게 만들기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P17에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-E01 | T07-P17: 대표 실패를 원인으로 착각 | T07-P17: T07-P17/CH06 실패 case를 다른 입력으로 재현 | T07-P17: 현상과 원인을 같은 것으로 봄 | T07-P17: T07-P17/CH06 대표 실패와 T07-P17/CH08 증거를 다시 대조 | T07-P17: T07-P17/CH08 |
| T07-P17-E02 | T07-P17: 경계 A 생략 | T07-P17: T07-P17/CH04의 조건 하나를 반대로 설정 | T07-P17: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P17: T07-P17/CH04를 새 입력에 적용 | T07-P17: T07-P17/CH08 |
| T07-P17-E03 | T07-P17: 경계 B 생략 | T07-P17: T07-P17/CH05의 조건 하나를 반대로 설정 | T07-P17: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P17: T07-P17/CH05를 새 입력에 적용 | T07-P17: T07-P17/CH08 |
| T07-P17-E04 | T07-P17: 복구 상태 혼동 | T07-P17: T07-P17/CH07에서 처리 중단을 주입 | T07-P17: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P17: T07-P17/CH07에서 수명 경계를 다시 표시 | T07-P17: T07-P17/CH08 |
| T07-P17-E05 | T07-P17: 운영 한계 누락 | T07-P17: T07-P17/CH09에서 부하 또는 drain 조건을 변경 | T07-P17: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P17: T07-P17/CH09의 종료 조건을 다시 작성 | T07-P17: T07-P17/CH08 |
| T07-P17-E06 | T07-P17: 증거 없는 성공 판정 | T07-P17: T07-P17/CH08에서 증거 하나를 숨김 | T07-P17: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P17: T07-P17/CH08에서 독립 증거 둘을 선택 | T07-P17: T07-P17/CH08 |

## CHAPTER 18 · command와 query를 구분해 use case를 선명하게 만들기 — synthetic 관측값 판독 6개

T07-P17: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P17의 숫자 하나만으로 원인을 단정하지 않고 T07-P17/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-O01 | T07-P17/commands_received | 884 | T07-P17: command 수신 수 | T07-P17: 축=구버전 client; 원인 확정 금지 | T07-P17: CLIENT_VERSION + T07-P17/CH08 |
| T07-P17-O02 | T07-P17/state_version | 901 | T07-P17: 상태 버전 | T07-P17: 축=동시 변경; 원인 확정 금지 | T07-P17: STATE_VERSION + T07-P17/CH08 |
| T07-P17-O03 | T07-P17/conflicts | 918 | T07-P17: 동시 수정 충돌 수 | T07-P17: 축=unknown outcome; 원인 확정 금지 | T07-P17: PROVIDER_RESULT + T07-P17/CH08 |
| T07-P17-O04 | T07-P17/retries | 935 | T07-P17: 재처리 수 | T07-P17: 축=재시작; 원인 확정 금지 | T07-P17: DURABLE_STATE + T07-P17/CH08 |
| T07-P17-O05 | T07-P17/side_effects | 952 | T07-P17: 외부 side effect 수 | T07-P17: 축=과부하; 원인 확정 금지 | T07-P17: QUEUE_PRESSURE + T07-P17/CH08 |
| T07-P17-O06 | T07-P17/invariant_violations | 969 | T07-P17: invariant 위반 수 | T07-P17: 축=sampling; 원인 확정 금지 | T07-P17: TRACE_METRIC_CROSSCHECK + T07-P17/CH08 |

## CHAPTER 19 · command와 query를 구분해 use case를 선명하게 만들기 — 선택형 코드 리뷰 6질문

T07-P17: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P17에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-R01 | T07-P17: 관측 | T07-P17: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P17: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=98] | T07-P17: T07-P17/CH04 | T07-P17: STATE_VERSION |
| T07-P17-R02 | T07-P17: 복구 | T07-P17: 재시작 뒤에도 필요한 상태가 남는가 | T07-P17: UNKNOWN_OUTCOME[timeout_ms=192; provider_state=UNKNOWN; lookup_id=p01701] | T07-P17: T07-P17/CH05 | T07-P17: PROVIDER_RESULT |
| T07-P17-R03 | T07-P17: 중복 | T07-P17: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P17: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P17: T07-P17/CH06 | T07-P17: DURABLE_STATE |
| T07-P17-R04 | T07-P17: timeout | T07-P17: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P17: OVERLOAD[rps=542; p99_ms=660; queue=13] | T07-P17: T07-P17/CH07 | T07-P17: QUEUE_PRESSURE |
| T07-P17-R05 | T07-P17: 관측 | T07-P17: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P17: SAMPLING[sample_rate=63%; trace_present=0; metric_present=1] | T07-P17: T07-P17/CH04 | T07-P17: TRACE_METRIC_CROSSCHECK |
| T07-P17-R06 | T07-P17: 복구 | T07-P17: 재시작 뒤에도 필요한 상태가 남는가 | T07-P17: DISCONNECT[disconnect_ms=86; commit_state=UNKNOWN; request=017-05] | T07-P17: T07-P17/CH05 | T07-P17: COMMIT_TIMELINE |

## CHAPTER 20 · command와 query를 구분해 use case를 선명하게 만들기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P17에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-I01 | T07-P17: 변경 동결 | T07-P17: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P17: UNKNOWN_OUTCOME[timeout_ms=181; provider_state=UNKNOWN; lookup_id=p01700] | T07-P17: T07-P17/CH08 + PROVIDER_RESULT | T07-P17-incident-323 |
| T07-P17-I02 | T07-P17: correlation | T07-P17: 한 request/job/resource id를 시간축에 고정 | T07-P17: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P17: T07-P17/CH08 + DURABLE_STATE | T07-P17-incident-324 |
| T07-P17-I03 | T07-P17: 마지막 정상 | T07-P17: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P17: OVERLOAD[rps=509; p99_ms=543; queue=19] | T07-P17: T07-P17/CH08 + QUEUE_PRESSURE | T07-P17-incident-325 |
| T07-P17-I04 | T07-P17: 가설 검증 | T07-P17: 원인 후보 하나만 뒤집어 재현 | T07-P17: SAMPLING[sample_rate=50%; trace_present=1; metric_present=1] | T07-P17: T07-P17/CH08 + TRACE_METRIC_CROSSCHECK | T07-P17-incident-326 |
| T07-P17-I05 | T07-P17: 복구 확인 | T07-P17: durable state와 사용자 결과를 모두 확인 | T07-P17: DISCONNECT[disconnect_ms=73; commit_state=UNKNOWN; request=017-04] | T07-P17: T07-P17/CH08 + COMMIT_TIMELINE | T07-P17-incident-327 |
| T07-P17-I06 | T07-P17: 재주입 | T07-P17: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P17: RECURRENCE[occurrence=2; interval_s=166; mitigation_applied=1] | T07-P17: T07-P17/CH08 + RECURRENCE_TIMELINE | T07-P17-incident-328 |

## CHAPTER 21 · command와 query를 구분해 use case를 선명하게 만들기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P17/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P17/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P17/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P17/CH04~T07-P17/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P17/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P17/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P17/CH18 signal 두 개와 T07-P17/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P17/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P17/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · command와 query를 구분해 use case를 선명하게 만들기 — 통합 casebook 16문제

T07-P17 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P17 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P17-C01 | T07-P17: 경계 A | T07-P17: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P17: load=593, window=29s | T07-P17: review=상태 변경 | T07-P17: 경계 A 위반 여부를 판정 | T07-P17: DURABLE_STATE + T07-P17/CH08 |
| T07-P17-C02 | T07-P17: 경계 B | T07-P17: OVERLOAD[rps=476; p99_ms=426; queue=25] | T07-P17: load=616, window=48s | T07-P17: review=retry | T07-P17: 경계 B 위반 여부를 판정 | T07-P17: QUEUE_PRESSURE + T07-P17/CH08 |
| T07-P17-C03 | T07-P17: 경계 C | T07-P17: SAMPLING[sample_rate=37%; trace_present=0; metric_present=1] | T07-P17: load=639, window=67s | T07-P17: review=복구 | T07-P17: 경계 C 위반 여부를 판정 | T07-P17: TRACE_METRIC_CROSSCHECK + T07-P17/CH08 |
| T07-P17-C04 | T07-P17: 경계 D | T07-P17: DISCONNECT[disconnect_ms=60; commit_state=UNKNOWN; request=017-03] | T07-P17: load=662, window=86s | T07-P17: review=동시성 | T07-P17: 경계 D 위반 여부를 판정 | T07-P17: COMMIT_TIMELINE + T07-P17/CH08 |
| T07-P17-C05 | T07-P17: 경계 A | T07-P17: RECURRENCE[occurrence=6; interval_s=155; mitigation_applied=1] | T07-P17: load=685, window=15s | T07-P17: review=민감정보 | T07-P17: 경계 A 위반 여부를 판정 | T07-P17: RECURRENCE_TIMELINE + T07-P17/CH08 |
| T07-P17-C06 | T07-P17: 경계 B | T07-P17: LARGE_INPUT[body_kb=1600; limit_kb=512; parsed=0] | T07-P17: load=708, window=34s | T07-P17: review=중복 | T07-P17: 경계 B 위반 여부를 판정 | T07-P17: SIZE_LIMIT + T07-P17/CH08 |
| T07-P17-C07 | T07-P17: 경계 C | T07-P17: REORDER[in_seq=9,7,8; applied_version=6] | T07-P17: load=731, window=53s | T07-P17: review=권한 | T07-P17: 경계 C 위반 여부를 판정 | T07-P17: SEQUENCE_STATE + T07-P17/CH08 |
| T07-P17-C08 | T07-P17: 경계 D | T07-P17: RECOVERY_SCOPE[selected=178; expected=21; backup=1; dry_run=1] | T07-P17: load=754, window=72s | T07-P17: review=입력 경계 | T07-P17: 경계 D 위반 여부를 판정 | T07-P17: RECOVERY_AUDIT + T07-P17/CH08 |
| T07-P17-C09 | T07-P17: 경계 A | T07-P17: REPLAY[key=cmd-017-08; attempts=4; response_seen=0] | T07-P17: load=777, window=91s | T07-P17: review=timeout | T07-P17: 경계 A 위반 여부를 판정 | T07-P17: IDEMPOTENCY_RECORD + T07-P17/CH08 |
| T07-P17-C10 | T07-P17: 경계 B | T07-P17: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P17: load=800, window=20s | T07-P17: review=자원 | T07-P17: 경계 B 위반 여부를 판정 | T07-P17: CLIENT_VERSION + T07-P17/CH08 |
| T07-P17-C11 | T07-P17: 경계 C | T07-P17: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=34] | T07-P17: load=823, window=39s | T07-P17: review=순서 | T07-P17: 경계 C 위반 여부를 판정 | T07-P17: STATE_VERSION + T07-P17/CH08 |
| T07-P17-C12 | T07-P17: 경계 D | T07-P17: UNKNOWN_OUTCOME[timeout_ms=302; provider_state=UNKNOWN; lookup_id=p01711] | T07-P17: load=846, window=58s | T07-P17: review=관측 | T07-P17: 경계 D 위반 여부를 판정 | T07-P17: PROVIDER_RESULT + T07-P17/CH08 |
| T07-P17-C13 | T07-P17: 경계 A | T07-P17: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P17: load=869, window=77s | T07-P17: review=상태 변경 | T07-P17: 경계 A 위반 여부를 판정 | T07-P17: DURABLE_STATE + T07-P17/CH08 |
| T07-P17-C14 | T07-P17: 경계 B | T07-P17: OVERLOAD[rps=239; p99_ms=957; queue=36] | T07-P17: load=892, window=96s | T07-P17: review=retry | T07-P17: 경계 B 위반 여부를 판정 | T07-P17: QUEUE_PRESSURE + T07-P17/CH08 |
| T07-P17-C15 | T07-P17: 경계 C | T07-P17: SAMPLING[sample_rate=16%; trace_present=0; metric_present=1] | T07-P17: load=915, window=25s | T07-P17: review=복구 | T07-P17: 경계 C 위반 여부를 판정 | T07-P17: TRACE_METRIC_CROSSCHECK + T07-P17/CH08 |
| T07-P17-C16 | T07-P17: 경계 D | T07-P17: DISCONNECT[disconnect_ms=119; commit_state=UNKNOWN; request=017-15] | T07-P17: load=938, window=44s | T07-P17: review=동시성 | T07-P17: 경계 D 위반 여부를 판정 | T07-P17: COMMIT_TIMELINE + T07-P17/CH08 |

채점은 결론보다 근거를 본다. T07-P17/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · command와 query를 구분해 use case를 선명하게 만들기 — evidence 판독 문제 14개

T07-P17 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P17 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P17-V01 | T07-P17: latency=717ms; queue=39; retry=3 | T07-P17: 과부하 | T07-P17: 축=과부하; 원인 확정은 보류 | T07-P17: QUEUE_PRESSURE + T07-P17/CH08 | T07-P17: 피할 오판=복구 과잉 |
| T07-P17-V02 | T07-P17: latency=784ms; queue=50; retry=6 | T07-P17: sampling | T07-P17: 축=sampling; 원인 확정은 보류 | T07-P17: TRACE_METRIC_CROSSCHECK + T07-P17/CH08 | T07-P17: 피할 오판=잘못된 전제 |
| T07-P17-V03 | T07-P17: latency=851ms; queue=61; retry=2 | T07-P17: client disconnect | T07-P17: 축=client disconnect; 원인 확정은 보류 | T07-P17: COMMIT_TIMELINE + T07-P17/CH08 | T07-P17: 피할 오판=경계 누락 |
| T07-P17-V04 | T07-P17: latency=918ms; queue=72; retry=5 | T07-P17: 재발 | T07-P17: 축=재발; 원인 확정은 보류 | T07-P17: RECURRENCE_TIMELINE + T07-P17/CH08 | T07-P17: 피할 오판=증거 혼동 |
| T07-P17-V05 | T07-P17: latency=985ms; queue=3; retry=1 | T07-P17: 대형 입력 | T07-P17: 축=대형 입력; 원인 확정은 보류 | T07-P17: SIZE_LIMIT + T07-P17/CH08 | T07-P17: 피할 오판=재시도 오판 |
| T07-P17-V06 | T07-P17: latency=1052ms; queue=14; retry=4 | T07-P17: 순서 역전 | T07-P17: 축=순서 역전; 원인 확정은 보류 | T07-P17: SEQUENCE_STATE + T07-P17/CH08 | T07-P17: 피할 오판=동시성 무시 |
| T07-P17-V07 | T07-P17: latency=1119ms; queue=25; retry=0 | T07-P17: 복구 범위 | T07-P17: 축=복구 범위; 원인 확정은 보류 | T07-P17: RECOVERY_AUDIT + T07-P17/CH08 | T07-P17: 피할 오판=순서 가정 |
| T07-P17-V08 | T07-P17: latency=1186ms; queue=36; retry=3 | T07-P17: 재전송 | T07-P17: 축=재전송; 원인 확정은 보류 | T07-P17: IDEMPOTENCY_RECORD + T07-P17/CH08 | T07-P17: 피할 오판=상태 수명 혼동 |
| T07-P17-V09 | T07-P17: latency=1253ms; queue=47; retry=6 | T07-P17: 구버전 client | T07-P17: 축=구버전 client; 원인 확정은 보류 | T07-P17: CLIENT_VERSION + T07-P17/CH08 | T07-P17: 피할 오판=운영 한계 누락 |
| T07-P17-V10 | T07-P17: latency=1320ms; queue=58; retry=2 | T07-P17: 동시 변경 | T07-P17: 축=동시 변경; 원인 확정은 보류 | T07-P17: STATE_VERSION + T07-P17/CH08 | T07-P17: 피할 오판=오류 합치기 |
| T07-P17-V11 | T07-P17: latency=1387ms; queue=69; retry=5 | T07-P17: unknown outcome | T07-P17: 축=unknown outcome; 원인 확정은 보류 | T07-P17: PROVIDER_RESULT + T07-P17/CH08 | T07-P17: 피할 오판=복구 과잉 |
| T07-P17-V12 | T07-P17: latency=1454ms; queue=0; retry=1 | T07-P17: 재시작 | T07-P17: 축=재시작; 원인 확정은 보류 | T07-P17: DURABLE_STATE + T07-P17/CH08 | T07-P17: 피할 오판=잘못된 전제 |
| T07-P17-V13 | T07-P17: latency=1521ms; queue=11; retry=4 | T07-P17: 과부하 | T07-P17: 축=과부하; 원인 확정은 보류 | T07-P17: QUEUE_PRESSURE + T07-P17/CH08 | T07-P17: 피할 오판=경계 누락 |
| T07-P17-V14 | T07-P17: latency=1588ms; queue=22; retry=0 | T07-P17: sampling | T07-P17: 축=sampling; 원인 확정은 보류 | T07-P17: TRACE_METRIC_CROSSCHECK + T07-P17/CH08 | T07-P17: 피할 오판=증거 혼동 |

## CHAPTER 24 · command와 query를 구분해 use case를 선명하게 만들기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P17에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P17-D01 | T07-P17: batch 확대 | T07-P17: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P17: memory·deadline·부분 실패 범위가 커지는지 | T07-P17: DISCONNECT[disconnect_ms=118; commit_state=UNKNOWN; request=017-00] | T07-P17: COMMIT_TIMELINE + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D02 | T07-P17: fallback 추가 | T07-P17: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P17: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P17: RECURRENCE[occurrence=3; interval_s=122; mitigation_applied=1] | T07-P17: RECURRENCE_TIMELINE + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D03 | T07-P17: 외부 호출 이동 | T07-P17: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P17: lock duration과 unknown outcome 경계가 달라지는지 | T07-P17: LARGE_INPUT[body_kb=1336; limit_kb=512; parsed=0] | T07-P17: SIZE_LIMIT + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D04 | T07-P17: cache 추가 | T07-P17: 현재 결과 앞에 cache layer를 추가한다 | T07-P17: stale·key·invalidation 책임이 새로 생기는지 | T07-P17: REORDER[in_seq=6,4,5; applied_version=6] | T07-P17: SEQUENCE_STATE + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D05 | T07-P17: pool 확대 | T07-P17: connection/worker pool 상한을 늘린다 | T07-P17: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P17: RECOVERY_SCOPE[selected=145; expected=20; backup=1; dry_run=0] | T07-P17: RECOVERY_AUDIT + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D06 | T07-P17: schema 변경 | T07-P17: 필드 이름·형식·required 조건을 바꾼다 | T07-P17: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P17: REPLAY[key=cmd-017-05; attempts=4; response_seen=0] | T07-P17: IDEMPOTENCY_RECORD + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D07 | T07-P17: 결과 합치기 | T07-P17: 여러 오류를 하나의 status/error code로 합친다 | T07-P17: client 행동과 retry 가능성을 잃지 않는지 | T07-P17: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P17: CLIENT_VERSION + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P17-D08 | T07-P17: retry 추가 | T07-P17: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P17: unknown outcome과 duplicate side effect를 구분하는지 | T07-P17: CONCURRENT_WRITE[actors=2; base_version=9; writes=2; gap_ms=92] | T07-P17: STATE_VERSION + T07-P17/CH08 | T07-P17: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P17: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · command와 query를 구분해 use case를 선명하게 만들기 — 최종 contract와 evidence spine

**최종 계약:** command는 상태를 바꾸려는 의도이고 query는 현재 정보를 읽으려는 요청으로 구분하면 책임이 선명해진다.

**정상 메커니즘:** use case별 입력과 출력, side effect 여부를 계약으로 두고 HTTP route는 그 계약을 호출한다.

**대표 실패:** 읽기 API가 몰래 상태를 바꾸거나 command 결과가 DB row 전체를 그대로 노출한다.

**검증 evidence:** route별 write 여부, 호출한 의존성, 반환 shape를 추적한다.

**직접 행동:** command/query 목록을 분류하고 허용되지 않은 side effect를 찾는 코드를 실행한다.

**다음 연결:** `idempotency로 같은 요청이 다시 와도 결과를 통제하기`.

`command와 query를 구분해 use case를 선명하게 만들기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | command와 query를 구분해 use case를 선명하게 만들기의 개념·실패·운영 판단 교차 확인 |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | command와 query를 구분해 use case를 선명하게 만들기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | command와 query를 구분해 use case를 선명하게 만들기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | command와 query를 구분해 use case를 선명하게 만들기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | command와 query를 구분해 use case를 선명하게 만들기의 개념·실패·운영 판단 교차 확인 |

`command와 query를 구분해 use case를 선명하게 만들기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
