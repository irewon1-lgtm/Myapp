# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 02 · route는 요청을 처리할 코드로 연결하는 규칙

## CHAPTER 01 · route는 요청을 처리할 코드로 연결하는 규칙 — 쉬운 말에서 정확한 계약까지

**출발 개념.** route는 HTTP method와 path를 보고 어떤 handler를 실행할지 정하는 연결표다.

**아주 쉬운 사건.** GET /users/me가 /users/:id로 잘못 들어간다. 이 사건에서는 먼저 **route table의 method와 specificity를 확인한다**.

**왜 필요한가.** 정상 동작은 router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다. 반대로 GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다.

**암기:** `route는 요청을 처리할 코드로 연결하는 규칙`의 역할 한 줄.

**직접 이해:** `route는 요청을 처리할 코드로 연결하는 규칙`의 입력·상태·결과 경계.

**AI 위임 가능:** `route는 요청을 처리할 코드로 연결하는 규칙` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `GET /users/me가 /users/:id로 잘못 들어간다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 등록된 route 표, 실제 method/path, 선택된 handler 이름과 parameter 값을 함께 본다.

## CHAPTER 02 · route는 요청을 처리할 코드로 연결하는 규칙 — 아주 쉬운 예를 한 단계씩 해석

T07-P02: `GET /users/me가 /users/:id로 잘못 들어간다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | GET /users/me가 /users/:id로 잘못 들어간다 | T07-P02 외부 입력 | T07-P02: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | route table의 method와 specificity를 확인한다 | T07-P02 판단 기준 | T07-P02/CH08 관측표와 대조 |
| 정상 경로 | T07-P02/CH03 M1→M5 | route는 요청을 처리할 코드로 연결하는 규칙: 완료 시점을 단계별로 분리 | T07-P02: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P02/CH06 GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다. | T07-P02: 깨진 계약 하나를 특정 | route는 요청을 처리할 코드로 연결하는 규칙: 증상과 원인을 분리 |
| 재검증 | T07-P02/CH10 직접 실행 | T07-P02: 예상값 T07-P002 2,2,3,1 기록 | route는 요청을 처리할 코드로 연결하는 규칙: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P02/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P02/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · route는 요청을 처리할 코드로 연결하는 규칙 — 내부 메커니즘과 상태 전이

router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | GET /users/me가 /users/:id로 잘못 들어간다 | source/actor/size를 보존 |
| M2 | 경계 판단 | route table의 method와 specificity를 확인한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 등록된 route 표, 실제 method/path, 선택된 handler 이름과 parameter 값을 함께 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 작은 route table에서 method/path가 어떤 handler에 매칭되는지 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`route는 요청을 처리할 코드로 연결하는 규칙` 흐름을 framework 이름 없이 설명한다.

막히면 `GET /users/me가 /users/:id로 잘못 들어간다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · route는 요청을 처리할 코드로 연결하는 규칙 — 실전 경계 A

**경계 A.** route 탐색에서는 path가 맞아도 method가 다르면 ‘없는 경로’와 ‘허용되지 않은 method’를 구분할 수 있어 404와 405를 같은 문제로 취급하지 않는다.

T07-P02/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P02에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P02/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P02/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P02): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P02/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P02-A1-304 | T07-P02 조건 | route 탐색에서는 path가 맞아도 method가 다르면 ‘없는 경로’와 ‘허용되지 않은 method’를 구분할 수 있어 404와 405를 같은 문제로 취급하지 않는다. |
| T07-P02-A2-305 | T07-P02 변화점 | T07-P02/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P02-A3-306 | T07-P02 반례 | T07-P02/CH06 대표 실패와 A 위반을 구별 |
| T07-P02-A4-307 | T07-P02 근거 | T07-P02/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P02-A5-308 | T07-P02 재실험 | GET /users/me가 /users/:id로 잘못 들어간다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · route는 요청을 처리할 코드로 연결하는 규칙 — 실전 경계 B

**경계 B.** /users/me 같은 고정 route와 /users/:id 같은 동적 route가 겹치면 router의 우선순위 규칙을 알아야 하고, 등록 순서에 따라 ‘me’가 id로 잘못 해석되는 framework도 있다.

T07-P02/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P02에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P02/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P02/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P02): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P02/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P02-B1-335 | T07-P02 조건 | /users/me 같은 고정 route와 /users/:id 같은 동적 route가 겹치면 router의 우선순위 규칙을 알아야 하고, 등록 순서에 따라 ‘me’가 id로 잘못 해석되는 framework도 있다. |
| T07-P02-B2-336 | T07-P02 독립성 | T07-P02/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P02-B3-337 | T07-P02 상태 | T07-P02/CH03 before·after 위치를 다시 지정 |
| T07-P02-B4-338 | T07-P02 반증 | T07-P02/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P02-B5-339 | T07-P02 적용 | route는 요청을 처리할 코드로 연결하는 규칙의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · route는 요청을 처리할 코드로 연결하는 규칙 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다.**

아래 여섯 사례는 T07-P02의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P02-F01 | T07-P02: 대표 실패 | T07-P02: T07-P02/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P02: 현상만 보고 원인을 확정 | T07-P02/CH08 evidence map에서 상태를 대조 |
| T07-P02-F02 | T07-P02: 경계 A 누락 | T07-P02: T07-P02/CH04 경계 A 위반 입력 | T07-P02: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P02/CH08 evidence map에서 상태를 대조 |
| T07-P02-F03 | T07-P02: 경계 B 누락 | T07-P02: T07-P02/CH05 경계 B 위반 입력 | T07-P02: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P02/CH08 evidence map에서 상태를 대조 |
| T07-P02-F04 | T07-P02: 복구 경계 C 누락 | T07-P02: T07-P02/CH07 경계 C 복구 조건 | T07-P02: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P02/CH08 evidence map에서 상태를 대조 |
| T07-P02-F05 | T07-P02: 운영 경계 D 누락 | T07-P02: T07-P02/CH09 경계 D 운영 조건 | T07-P02: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P02/CH08 evidence map에서 상태를 대조 |
| T07-P02-F06 | T07-P02: 증거 없는 결론 | T07-P02: T07-P02/CH02 첫 판단만 존재 | T07-P02: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P02/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P02/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · route는 요청을 처리할 코드로 연결하는 규칙 — 복구 가능한 상태와 수명

**경계 C.** URL path는 percent-decoding 과정에서 잘못된 escape나 예상 밖 문자가 생길 수 있으므로 route parameter를 이미 안전한 식별자라고 간주하지 않는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P02에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P02/CH08 evidence map을 본다. 복구 후에는 T07-P02/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P02에서 이미 확정된 side effect는 T07-P02/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P02-R1-397 | T07-P02 중단 직전 | T07-P02/CH03에서 이미 확정된 상태만 표시 |
| T07-P02-R2-398 | T07-P02 재시작 직후 | URL path는 percent-decoding 과정에서 잘못된 escape나 예상 밖 문자가 생길 수 있으므로 route parameter를 이미 안전한 식별자라고 간주하지 않는다. |
| T07-P02-R3-399 | T07-P02 재검증 | T07-P02/CH08 근거로 중복·누락 여부 확인 |
| T07-P02-R4-400 | T07-P02 재실행 | T07-P02/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · route는 요청을 처리할 코드로 연결하는 규칙 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | GET /users/me가 /users/:id로 잘못 들어간다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | route table의 method와 specificity를 확인한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 등록된 route 표, 실제 method/path, 선택된 handler 이름과 parameter 값을 함께 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`route는 요청을 처리할 코드로 연결하는 규칙` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · route는 요청을 처리할 코드로 연결하는 규칙 — 운영 한계와 종료 조건

**경계 D.** trailing slash와 대소문자를 같은 route로 볼지 다른 route로 볼지 정책을 정하지 않으면 proxy·framework·client가 서로 다른 URL을 같은 자원이라고 믿을 수 있다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P02/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P02/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P02 과제: 경계 D와 T07-P02/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P02-O1-459 | synthetic-load=139 | T07-P02/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P02-O2-460 | synthetic-budget=609ms | T07-P02 timeout과 unknown outcome을 분리 |
| T07-P02-O3-461 | T07-P02 종료 | trailing slash와 대소문자를 같은 route로 볼지 다른 route로 볼지 정책을 정하지 않으면 proxy·framework·client가 서로 다른 URL을 같은 자원이라고 믿을 수 있다. |
| T07-P02-O4-462 | T07-P02 완화 | T07-P02/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · route는 요청을 처리할 코드로 연결하는 규칙 — 직접 실행하는 작은 모델

`route는 요청을 처리할 코드로 연결하는 규칙` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P002 2,2,3,1`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P002";
const limit = 5;
const input = [2, 2, 3, 1];
const accepted = input.filter(value => value <= limit);
console.log(marker, accepted.join(","));
```

기준 출력: `T07-P002 2,2,3,1`.

`route는 요청을 처리할 코드로 연결하는 규칙`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P02-L1-491 | constmarker="T07-P002"; | T07-P02 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P02-L2-492 | constlimit=5; | T07-P02 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P02-L3-493 | constinput=[2,2,3,1]; | T07-P02 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P02-L4-494 | constaccepted=input.filter(value=>value<=limit); | T07-P02 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P02-L5-495 | console.log(marker,accepted.join(",")); | T07-P02 출력 관측점; 예상 `T07-P002 2,2,3,1`와 비교 |
| T07-P02-LX-580 | T07-P02 실행 기록 | T07-P02 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · route는 요청을 처리할 코드로 연결하는 규칙 — 한 부분만 수정하고 다시 예측

수정 과제: **limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다**.

수정 전은 `T07-P002 2,2,3,1`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P02/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P02-D1-521 | 기준 `T07-P002 2,2,3,1` | T07-P02 수정 전 실행을 먼저 재현 |
| T07-P02-D2-522 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | T07-P02 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P02-D3-523 | T07-P02 새 예측 | T07-P02 실행 전에 출력·상태를 먼저 기록 |
| T07-P02-D4-524 | T07-P02 재실행 | T07-P02/CH10 실제값과 새 예측을 대조 |
| T07-P02-D5-525 | T07-P02 반례 | T07-P02/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P02-D6-526 | T07-P02 근거 | T07-P02/CH08 상태가 설명과 일치해야 완료 |
| T07-P02-D7-527 | T07-P02 이유 | T07-P02 변경 이유를 route는 요청을 처리할 코드로 연결하는 규칙 계약과 연결해 설명 |

## CHAPTER 12 · route는 요청을 처리할 코드로 연결하는 규칙 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: route는 요청을 처리할 코드로 연결하는 규칙 | route는 HTTP method와 path를 보고 어떤 handler를 실행할지 정하는 연결표다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P02/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 등록된 route 표, 실제 method/path, 선택된 handler 이름과 parameter 값을 함께 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P02/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P02/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P02/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P02/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `route는 요청을 처리할 코드로 연결하는 규칙` 실행 코드 수정 | limit를 1 낮추고 어떤 값이 새로 거부되는지 예측한다 | `route는 요청을 처리할 코드로 연결하는 규칙` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P02/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P02/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · route는 요청을 처리할 코드로 연결하는 규칙 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | route는 HTTP method와 path를 보고 어떤 handler를 실행할지 정하는 연결표다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P02/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P02/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P02/CH10 실행용 boilerplate·test 후보 | T07-P02: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P002 2,2,3,1` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P02/CH02의 판단 기준과 T07-P02/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P02-AI1-583 | T07-P02 사람 결정 | T07-P02 업무 의미·허용 위험·완료 기준 소유 |
| T07-P02-AI2-584 | T07-P02 AI 초안 | T07-P02/CH10 boilerplate·test 후보까지만 위임 |
| T07-P02-AI3-585 | T07-P02 검증 | T07-P02/CH06 반례와 T07-P02/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · route는 요청을 처리할 코드로 연결하는 규칙 — 경계 조합 실험 8개

T07-P02: T07-P02/CH04~T07-P02/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P02의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-K01 | T07-P02: 경계 A | T07-P02: 경계 B | T07-P02: REPLAY[key=cmd-002-01; attempts=3; response_seen=0] / sample=40 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + IDEMPOTENCY_RECORD |
| T07-P02-K02 | T07-P02: 경계 A | T07-P02: 경계 C | T07-P02: OLD_SCHEMA[client=v1; server=v2; extra_field=0] / sample=47 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + CLIENT_VERSION |
| T07-P02-K03 | T07-P02: 경계 A | T07-P02: 경계 D | T07-P02: UNKNOWN_OUTCOME[timeout_ms=201; provider_state=UNKNOWN; lookup_id=p00203] / sample=54 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + PROVIDER_RESULT |
| T07-P02-K04 | T07-P02: 경계 B | T07-P02: 경계 C | T07-P02: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=61 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + DURABLE_STATE |
| T07-P02-K05 | T07-P02: 경계 B | T07-P02: 경계 D | T07-P02: OVERLOAD[rps=569; p99_ms=345; queue=20] / sample=68 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + QUEUE_PRESSURE |
| T07-P02-K06 | T07-P02: 경계 C | T07-P02: 경계 D | T07-P02: SAMPLING[sample_rate=28%; trace_present=0; metric_present=1] / sample=75 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P02-K07 | T07-P02: 경계 A | T07-P02: 경계 B+C | T07-P02: DISCONNECT[disconnect_ms=51; commit_state=UNKNOWN; request=002-07] / sample=82 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + COMMIT_TIMELINE |
| T07-P02-K08 | T07-P02: 경계 B | T07-P02: 경계 C+D | T07-P02: RECURRENCE[occurrence=5; interval_s=186; mitigation_applied=1] / sample=89 | T07-P02: 먼저 깨지는 경계를 판정 | T07-P02: T07-P02/CH08 + RECURRENCE_TIMELINE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P02/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · route는 요청을 처리할 코드로 연결하는 규칙 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P02와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-B01 | T07-P02: 서버 프로그램과 요청의 첫 진입점 | T07-P02: 요청 A가 앱까지 도착하지 않는다 | T07-P02: process/listen/network 경계를 분리해서 본다 | T07-P02: T07-P02/CH08 증거와 형제 LESSON 증거를 분리 | T07-P02: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P02-B03 | T07-P02: path parameter·query·header·body를 구분하기 | T07-P02: ?tag=a&tag=b와 body가 함께 온다 | T07-P02: 값의 출처를 path/query/header/body로 표시한다 | T07-P02: T07-P02/CH08 증거와 형제 LESSON 증거를 분리 | T07-P02: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P02-B04 | T07-P02: parsing은 문자열을 구조로 읽는 일 | T07-P02: Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | T07-P02: parsing 실패와 validation 실패를 구분한다 | T07-P02: T07-P02/CH08 증거와 형제 LESSON 증거를 분리 | T07-P02: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P02-B05 | T07-P02: middleware는 공통 단계를 순서대로 연결한다 | T07-P02: auth middleware가 parser보다 먼저/뒤에 놓인다 | T07-P02: middleware 순서를 화살표로 그린다 | T07-P02: T07-P02/CH08 증거와 형제 LESSON 증거를 분리 | T07-P02: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P02-B06 | T07-P02: validation은 외부 입력을 내부 계약으로 바꾸는 문 | T07-P02: age="20", admin=true가 입력으로 온다 | T07-P02: coercion과 허용 field를 따로 결정한다 | T07-P02: T07-P02/CH08 증거와 형제 LESSON 증거를 분리 | T07-P02: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · route는 요청을 처리할 코드로 연결하는 규칙 — 선택형 실패 주입 6개

T07-P02: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P02 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-X01 | T07-P02: unknown outcome | T07-P02: dependency timeout 후 성공 여부 불명; sample=79 | T07-P02: 실패와 미확정 결과 | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: provider id·조회 결과·retry history |
| T07-P02-X02 | T07-P02: 재시작 | T07-P02: side effect 직후 process가 재시작됨; sample=96 | T07-P02: durable state와 memory state | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: commit·outbox·job id·restart 전후 상태 |
| T07-P02-X03 | T07-P02: 과부하 | T07-P02: traffic 세 배, p99 급증; sample=113 | T07-P02: 기능 실패와 saturation | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: queue age·pool wait·CPU/event-loop·quota |
| T07-P02-X04 | T07-P02: sampling | T07-P02: 일부 log가 sampling으로 빠짐; sample=130 | T07-P02: 기록 부재와 사건 부재 | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: metric·trace·durable state 교차 근거 |
| T07-P02-X05 | T07-P02: client disconnect | T07-P02: 응답 전에 연결이 끊김; sample=147 | T07-P02: 연결 종료와 server effect | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: commit 시각·worker/outbox·request lifecycle |
| T07-P02-X06 | T07-P02: 재발 | T07-P02: 같은 오류가 잠시 뒤 다시 발생; sample=164 | T07-P02: 완화와 원인 제거 | T07-P02: T07-P02/CH02 판단과 별도 기록 | T07-P02: 재발 timeline·변경점·resource state |

## CHAPTER 17 · route는 요청을 처리할 코드로 연결하는 규칙 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P02에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-E01 | T07-P02: 대표 실패를 원인으로 착각 | T07-P02: T07-P02/CH06 실패 case를 다른 입력으로 재현 | T07-P02: 현상과 원인을 같은 것으로 봄 | T07-P02: T07-P02/CH06 대표 실패와 T07-P02/CH08 증거를 다시 대조 | T07-P02: T07-P02/CH08 |
| T07-P02-E02 | T07-P02: 경계 A 생략 | T07-P02: T07-P02/CH04의 조건 하나를 반대로 설정 | T07-P02: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P02: T07-P02/CH04를 새 입력에 적용 | T07-P02: T07-P02/CH08 |
| T07-P02-E03 | T07-P02: 경계 B 생략 | T07-P02: T07-P02/CH05의 조건 하나를 반대로 설정 | T07-P02: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P02: T07-P02/CH05를 새 입력에 적용 | T07-P02: T07-P02/CH08 |
| T07-P02-E04 | T07-P02: 복구 상태 혼동 | T07-P02: T07-P02/CH07에서 처리 중단을 주입 | T07-P02: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P02: T07-P02/CH07에서 수명 경계를 다시 표시 | T07-P02: T07-P02/CH08 |
| T07-P02-E05 | T07-P02: 운영 한계 누락 | T07-P02: T07-P02/CH09에서 부하 또는 drain 조건을 변경 | T07-P02: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P02: T07-P02/CH09의 종료 조건을 다시 작성 | T07-P02: T07-P02/CH08 |
| T07-P02-E06 | T07-P02: 증거 없는 성공 판정 | T07-P02: T07-P02/CH08에서 증거 하나를 숨김 | T07-P02: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P02: T07-P02/CH08에서 독립 증거 둘을 선택 | T07-P02: T07-P02/CH08 |

## CHAPTER 18 · route는 요청을 처리할 코드로 연결하는 규칙 — synthetic 관측값 판독 6개

T07-P02: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P02의 숫자 하나만으로 원인을 단정하지 않고 T07-P02/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-O01 | T07-P02/requests_received | 164 | T07-P02: 요청 수신 수 | T07-P02: 축=client disconnect; 원인 확정 금지 | T07-P02: COMMIT_TIMELINE + T07-P02/CH08 |
| T07-P02-O02 | T07-P02/handler_started | 181 | T07-P02: handler 진입 수 | T07-P02: 축=재발; 원인 확정 금지 | T07-P02: RECURRENCE_TIMELINE + T07-P02/CH08 |
| T07-P02-O03 | T07-P02/responses_completed | 198 | T07-P02: 응답 완료 수 | T07-P02: 축=대형 입력; 원인 확정 금지 | T07-P02: SIZE_LIMIT + T07-P02/CH08 |
| T07-P02-O04 | T07-P02/latency_ms | 215 | T07-P02: 요청 처리 지연 | T07-P02: 축=drain; 원인 확정 금지 | T07-P02: DRAIN_STATE + T07-P02/CH08 |
| T07-P02-O05 | T07-P02/body_kb | 232 | T07-P02: 입력 크기 | T07-P02: 축=재전송; 원인 확정 금지 | T07-P02: IDEMPOTENCY_RECORD + T07-P02/CH08 |
| T07-P02-O06 | T07-P02/active_connections | 249 | T07-P02: 활성 연결 수 | T07-P02: 축=구버전 client; 원인 확정 금지 | T07-P02: CLIENT_VERSION + T07-P02/CH08 |

## CHAPTER 19 · route는 요청을 처리할 코드로 연결하는 규칙 — 선택형 코드 리뷰 6질문

T07-P02: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P02에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-R01 | T07-P02: 관측 | T07-P02: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P02: RECURRENCE[occurrence=2; interval_s=98; mitigation_applied=1] | T07-P02: T07-P02/CH04 | T07-P02: RECURRENCE_TIMELINE |
| T07-P02-R02 | T07-P02: 복구 | T07-P02: 재시작 뒤에도 필요한 상태가 남는가 | T07-P02: LARGE_INPUT[body_kb=1144; limit_kb=512; parsed=0] | T07-P02: T07-P02/CH05 | T07-P02: SIZE_LIMIT |
| T07-P02-R03 | T07-P02: 중복 | T07-P02: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P02: DRAIN[ready=0; active=13; drain_deadline_s=7] | T07-P02: T07-P02/CH06 | T07-P02: DRAIN_STATE |
| T07-P02-R04 | T07-P02: timeout | T07-P02: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P02: REPLAY[key=cmd-002-03; attempts=2; response_seen=0] | T07-P02: T07-P02/CH07 | T07-P02: IDEMPOTENCY_RECORD |
| T07-P02-R05 | T07-P02: 관측 | T07-P02: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P02: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P02: T07-P02/CH04 | T07-P02: CLIENT_VERSION |
| T07-P02-R06 | T07-P02: 복구 | T07-P02: 재시작 뒤에도 필요한 상태가 남는가 | T07-P02: UNKNOWN_OUTCOME[timeout_ms=223; provider_state=UNKNOWN; lookup_id=p00205] | T07-P02: T07-P02/CH05 | T07-P02: PROVIDER_RESULT |

## CHAPTER 20 · route는 요청을 처리할 코드로 연결하는 규칙 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P02에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-I01 | T07-P02: correlation | T07-P02: 한 request/job/resource id를 시간축에 고정 | T07-P02: LARGE_INPUT[body_kb=1056; limit_kb=512; parsed=0] | T07-P02: T07-P02/CH08 + SIZE_LIMIT | T07-P02-incident-038 |
| T07-P02-I02 | T07-P02: 마지막 정상 | T07-P02: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P02: DRAIN[ready=0; active=17; drain_deadline_s=6] | T07-P02: T07-P02/CH08 + DRAIN_STATE | T07-P02-incident-039 |
| T07-P02-I03 | T07-P02: 가설 검증 | T07-P02: 원인 후보 하나만 뒤집어 재현 | T07-P02: REPLAY[key=cmd-002-02; attempts=4; response_seen=0] | T07-P02: T07-P02/CH08 + IDEMPOTENCY_RECORD | T07-P02-incident-040 |
| T07-P02-I04 | T07-P02: 복구 확인 | T07-P02: durable state와 사용자 결과를 모두 확인 | T07-P02: OLD_SCHEMA[client=v2; server=v3; extra_field=1] | T07-P02: T07-P02/CH08 + CLIENT_VERSION | T07-P02-incident-041 |
| T07-P02-I05 | T07-P02: 재주입 | T07-P02: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P02: UNKNOWN_OUTCOME[timeout_ms=212; provider_state=UNKNOWN; lookup_id=p00204] | T07-P02: T07-P02/CH08 + PROVIDER_RESULT | T07-P02-incident-042 |
| T07-P02-I06 | T07-P02: 회귀 고정 | T07-P02: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P02: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P02: T07-P02/CH08 + DURABLE_STATE | T07-P02-incident-043 |

## CHAPTER 21 · route는 요청을 처리할 코드로 연결하는 규칙 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P02/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P02/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P02/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P02/CH04~T07-P02/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P02/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P02/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P02/CH18 signal 두 개와 T07-P02/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P02/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P02/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · route는 요청을 처리할 코드로 연결하는 규칙 — 통합 casebook 16문제

T07-P02 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P02 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P02-C01 | T07-P02: 경계 A | T07-P02: DRAIN[ready=0; active=4; drain_deadline_s=5] | T07-P02: load=158, window=44s | T07-P02: review=자원 | T07-P02: 경계 A 위반 여부를 판정 | T07-P02: DRAIN_STATE + T07-P02/CH08 |
| T07-P02-C02 | T07-P02: 경계 B | T07-P02: REPLAY[key=cmd-002-01; attempts=3; response_seen=0] | T07-P02: load=181, window=63s | T07-P02: review=순서 | T07-P02: 경계 B 위반 여부를 판정 | T07-P02: IDEMPOTENCY_RECORD + T07-P02/CH08 |
| T07-P02-C03 | T07-P02: 경계 C | T07-P02: OLD_SCHEMA[client=v1; server=v2; extra_field=0] | T07-P02: load=204, window=82s | T07-P02: review=관측 | T07-P02: 경계 C 위반 여부를 판정 | T07-P02: CLIENT_VERSION + T07-P02/CH08 |
| T07-P02-C04 | T07-P02: 경계 D | T07-P02: UNKNOWN_OUTCOME[timeout_ms=201; provider_state=UNKNOWN; lookup_id=p00203] | T07-P02: load=227, window=11s | T07-P02: review=상태 변경 | T07-P02: 경계 D 위반 여부를 판정 | T07-P02: PROVIDER_RESULT + T07-P02/CH08 |
| T07-P02-C05 | T07-P02: 경계 A | T07-P02: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P02: load=250, window=30s | T07-P02: review=retry | T07-P02: 경계 A 위반 여부를 판정 | T07-P02: DURABLE_STATE + T07-P02/CH08 |
| T07-P02-C06 | T07-P02: 경계 B | T07-P02: OVERLOAD[rps=569; p99_ms=345; queue=20] | T07-P02: load=273, window=49s | T07-P02: review=복구 | T07-P02: 경계 B 위반 여부를 판정 | T07-P02: QUEUE_PRESSURE + T07-P02/CH08 |
| T07-P02-C07 | T07-P02: 경계 C | T07-P02: SAMPLING[sample_rate=28%; trace_present=0; metric_present=1] | T07-P02: load=296, window=68s | T07-P02: review=동시성 | T07-P02: 경계 C 위반 여부를 판정 | T07-P02: TRACE_METRIC_CROSSCHECK + T07-P02/CH08 |
| T07-P02-C08 | T07-P02: 경계 D | T07-P02: DISCONNECT[disconnect_ms=51; commit_state=UNKNOWN; request=002-07] | T07-P02: load=319, window=87s | T07-P02: review=민감정보 | T07-P02: 경계 D 위반 여부를 판정 | T07-P02: COMMIT_TIMELINE + T07-P02/CH08 |
| T07-P02-C09 | T07-P02: 경계 A | T07-P02: RECURRENCE[occurrence=5; interval_s=186; mitigation_applied=1] | T07-P02: load=342, window=16s | T07-P02: review=중복 | T07-P02: 경계 A 위반 여부를 판정 | T07-P02: RECURRENCE_TIMELINE + T07-P02/CH08 |
| T07-P02-C10 | T07-P02: 경계 B | T07-P02: LARGE_INPUT[body_kb=1848; limit_kb=512; parsed=0] | T07-P02: load=365, window=35s | T07-P02: review=권한 | T07-P02: 경계 B 위반 여부를 판정 | T07-P02: SIZE_LIMIT + T07-P02/CH08 |
| T07-P02-C11 | T07-P02: 경계 C | T07-P02: DRAIN[ready=0; active=3; drain_deadline_s=7] | T07-P02: load=388, window=54s | T07-P02: review=입력 경계 | T07-P02: 경계 C 위반 여부를 판정 | T07-P02: DRAIN_STATE + T07-P02/CH08 |
| T07-P02-C12 | T07-P02: 경계 D | T07-P02: REPLAY[key=cmd-002-11; attempts=4; response_seen=0] | T07-P02: load=411, window=73s | T07-P02: review=timeout | T07-P02: 경계 D 위반 여부를 판정 | T07-P02: IDEMPOTENCY_RECORD + T07-P02/CH08 |
| T07-P02-C13 | T07-P02: 경계 A | T07-P02: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P02: load=434, window=92s | T07-P02: review=자원 | T07-P02: 경계 A 위반 여부를 판정 | T07-P02: CLIENT_VERSION + T07-P02/CH08 |
| T07-P02-C14 | T07-P02: 경계 B | T07-P02: UNKNOWN_OUTCOME[timeout_ms=311; provider_state=UNKNOWN; lookup_id=p00213] | T07-P02: load=457, window=21s | T07-P02: review=순서 | T07-P02: 경계 B 위반 여부를 판정 | T07-P02: PROVIDER_RESULT + T07-P02/CH08 |
| T07-P02-C15 | T07-P02: 경계 C | T07-P02: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P02: load=480, window=40s | T07-P02: review=관측 | T07-P02: 경계 C 위반 여부를 판정 | T07-P02: DURABLE_STATE + T07-P02/CH08 |
| T07-P02-C16 | T07-P02: 경계 D | T07-P02: OVERLOAD[rps=266; p99_ms=642; queue=43] | T07-P02: load=503, window=59s | T07-P02: review=상태 변경 | T07-P02: 경계 D 위반 여부를 판정 | T07-P02: QUEUE_PRESSURE + T07-P02/CH08 |

채점은 결론보다 근거를 본다. T07-P02/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · route는 요청을 처리할 코드로 연결하는 규칙 — evidence 판독 문제 14개

T07-P02 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P02 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P02-V01 | T07-P02: latency=102ms; queue=14; retry=2 | T07-P02: 재전송 | T07-P02: 축=재전송; 원인 확정은 보류 | T07-P02: IDEMPOTENCY_RECORD + T07-P02/CH08 | T07-P02: 피할 오판=동시성 무시 |
| T07-P02-V02 | T07-P02: latency=169ms; queue=25; retry=5 | T07-P02: 구버전 client | T07-P02: 축=구버전 client; 원인 확정은 보류 | T07-P02: CLIENT_VERSION + T07-P02/CH08 | T07-P02: 피할 오판=상태 수명 혼동 |
| T07-P02-V03 | T07-P02: latency=236ms; queue=36; retry=1 | T07-P02: unknown outcome | T07-P02: 축=unknown outcome; 원인 확정은 보류 | T07-P02: PROVIDER_RESULT + T07-P02/CH08 | T07-P02: 피할 오판=운영 한계 누락 |
| T07-P02-V04 | T07-P02: latency=303ms; queue=47; retry=4 | T07-P02: 재시작 | T07-P02: 축=재시작; 원인 확정은 보류 | T07-P02: DURABLE_STATE + T07-P02/CH08 | T07-P02: 피할 오판=오류 합치기 |
| T07-P02-V05 | T07-P02: latency=370ms; queue=58; retry=0 | T07-P02: 과부하 | T07-P02: 축=과부하; 원인 확정은 보류 | T07-P02: QUEUE_PRESSURE + T07-P02/CH08 | T07-P02: 피할 오판=복구 과잉 |
| T07-P02-V06 | T07-P02: latency=437ms; queue=69; retry=3 | T07-P02: sampling | T07-P02: 축=sampling; 원인 확정은 보류 | T07-P02: TRACE_METRIC_CROSSCHECK + T07-P02/CH08 | T07-P02: 피할 오판=잘못된 전제 |
| T07-P02-V07 | T07-P02: latency=504ms; queue=0; retry=6 | T07-P02: client disconnect | T07-P02: 축=client disconnect; 원인 확정은 보류 | T07-P02: COMMIT_TIMELINE + T07-P02/CH08 | T07-P02: 피할 오판=경계 누락 |
| T07-P02-V08 | T07-P02: latency=571ms; queue=11; retry=2 | T07-P02: 재발 | T07-P02: 축=재발; 원인 확정은 보류 | T07-P02: RECURRENCE_TIMELINE + T07-P02/CH08 | T07-P02: 피할 오판=증거 혼동 |
| T07-P02-V09 | T07-P02: latency=638ms; queue=22; retry=5 | T07-P02: 대형 입력 | T07-P02: 축=대형 입력; 원인 확정은 보류 | T07-P02: SIZE_LIMIT + T07-P02/CH08 | T07-P02: 피할 오판=동시성 무시 |
| T07-P02-V10 | T07-P02: latency=705ms; queue=33; retry=1 | T07-P02: drain | T07-P02: 축=drain; 원인 확정은 보류 | T07-P02: DRAIN_STATE + T07-P02/CH08 | T07-P02: 피할 오판=상태 수명 혼동 |
| T07-P02-V11 | T07-P02: latency=772ms; queue=44; retry=4 | T07-P02: 재전송 | T07-P02: 축=재전송; 원인 확정은 보류 | T07-P02: IDEMPOTENCY_RECORD + T07-P02/CH08 | T07-P02: 피할 오판=운영 한계 누락 |
| T07-P02-V12 | T07-P02: latency=839ms; queue=55; retry=0 | T07-P02: 구버전 client | T07-P02: 축=구버전 client; 원인 확정은 보류 | T07-P02: CLIENT_VERSION + T07-P02/CH08 | T07-P02: 피할 오판=오류 합치기 |
| T07-P02-V13 | T07-P02: latency=906ms; queue=66; retry=3 | T07-P02: unknown outcome | T07-P02: 축=unknown outcome; 원인 확정은 보류 | T07-P02: PROVIDER_RESULT + T07-P02/CH08 | T07-P02: 피할 오판=복구 과잉 |
| T07-P02-V14 | T07-P02: latency=973ms; queue=77; retry=6 | T07-P02: 재시작 | T07-P02: 축=재시작; 원인 확정은 보류 | T07-P02: DURABLE_STATE + T07-P02/CH08 | T07-P02: 피할 오판=잘못된 전제 |

## CHAPTER 24 · route는 요청을 처리할 코드로 연결하는 규칙 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P02에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P02-D01 | T07-P02: batch 확대 | T07-P02: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P02: memory·deadline·부분 실패 범위가 커지는지 | T07-P02: UNKNOWN_OUTCOME[timeout_ms=168; provider_state=UNKNOWN; lookup_id=p00200] | T07-P02: PROVIDER_RESULT + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D02 | T07-P02: fallback 추가 | T07-P02: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P02: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P02: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P02: DURABLE_STATE + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D03 | T07-P02: 외부 호출 이동 | T07-P02: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P02: lock duration과 unknown outcome 경계가 달라지는지 | T07-P02: OVERLOAD[rps=470; p99_ms=867; queue=15] | T07-P02: QUEUE_PRESSURE + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D04 | T07-P02: cache 추가 | T07-P02: 현재 결과 앞에 cache layer를 추가한다 | T07-P02: stale·key·invalidation 책임이 새로 생기는지 | T07-P02: SAMPLING[sample_rate=86%; trace_present=1; metric_present=1] | T07-P02: TRACE_METRIC_CROSSCHECK + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D05 | T07-P02: pool 확대 | T07-P02: connection/worker pool 상한을 늘린다 | T07-P02: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P02: DISCONNECT[disconnect_ms=109; commit_state=UNKNOWN; request=002-04] | T07-P02: COMMIT_TIMELINE + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D06 | T07-P02: schema 변경 | T07-P02: 필드 이름·형식·required 조건을 바꾼다 | T07-P02: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P02: RECURRENCE[occurrence=2; interval_s=153; mitigation_applied=1] | T07-P02: RECURRENCE_TIMELINE + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D07 | T07-P02: 결과 합치기 | T07-P02: 여러 오류를 하나의 status/error code로 합친다 | T07-P02: client 행동과 retry 가능성을 잃지 않는지 | T07-P02: LARGE_INPUT[body_kb=1584; limit_kb=512; parsed=0] | T07-P02: SIZE_LIMIT + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P02-D08 | T07-P02: retry 추가 | T07-P02: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P02: unknown outcome과 duplicate side effect를 구분하는지 | T07-P02: DRAIN[ready=0; active=15; drain_deadline_s=12] | T07-P02: DRAIN_STATE + T07-P02/CH08 | T07-P02: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P02: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · route는 요청을 처리할 코드로 연결하는 규칙 — 최종 contract와 evidence spine

**최종 계약:** route는 HTTP method와 path를 보고 어떤 handler를 실행할지 정하는 연결표다.

**정상 메커니즘:** router가 method와 path pattern을 비교하고 path parameter를 추출한 뒤 선택된 handler에 요청을 전달한다.

**대표 실패:** GET과 POST를 같은 동작으로 보거나, 넓은 동적 route가 구체적 route를 가려 엉뚱한 handler가 선택된다.

**검증 evidence:** 등록된 route 표, 실제 method/path, 선택된 handler 이름과 parameter 값을 함께 본다.

**직접 행동:** 작은 route table에서 method/path가 어떤 handler에 매칭되는지 계산한다.

**다음 연결:** `path parameter·query·header·body를 구분하기`.

`route는 요청을 처리할 코드로 연결하는 규칙`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | route는 요청을 처리할 코드로 연결하는 규칙의 개념·실패·운영 판단 교차 확인 |

`route는 요청을 처리할 코드로 연결하는 규칙` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
