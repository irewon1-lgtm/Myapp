# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 04 · cache·queue·background work로 느린 일을 분리하기

### LESSON 05 · HTTP cache와 ETag·conditional request

## CHAPTER 01 · HTTP cache와 ETag·conditional request — 쉬운 말에서 정확한 계약까지

**출발 개념.** HTTP 자체에도 client/proxy가 응답을 재사용하거나 변경 여부만 확인하는 규칙이 있다.

**아주 쉬운 사건.** client가 ETag를 다시 보낸다. 이 사건에서는 먼저 **304와 representation version을 판단한다**.

**왜 필요한가.** 정상 동작은 Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다. 반대로 개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다.

**암기:** `HTTP cache와 ETag·conditional request`의 역할 한 줄.

**직접 이해:** `HTTP cache와 ETag·conditional request`의 입력·상태·결과 경계.

**AI 위임 가능:** `HTTP cache와 ETag·conditional request` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `client가 ETag를 다시 보낸다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 request conditional header, cache policy, response status/age를 확인한다.

## CHAPTER 02 · HTTP cache와 ETag·conditional request — 아주 쉬운 예를 한 단계씩 해석

T07-P50: `client가 ETag를 다시 보낸다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | client가 ETag를 다시 보낸다 | T07-P50 외부 입력 | T07-P50: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | 304와 representation version을 판단한다 | T07-P50 판단 기준 | T07-P50/CH08 관측표와 대조 |
| 정상 경로 | T07-P50/CH03 M1→M5 | HTTP cache와 ETag·conditional request: 완료 시점을 단계별로 분리 | T07-P50: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P50/CH06 개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다. | T07-P50: 깨진 계약 하나를 특정 | HTTP cache와 ETag·conditional request: 증상과 원인을 분리 |
| 재검증 | T07-P50/CH10 직접 실행 | T07-P50: 예상값 T07-P050 4 기록 | HTTP cache와 ETag·conditional request: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P50/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P50/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · HTTP cache와 ETag·conditional request — 내부 메커니즘과 상태 전이

Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | client가 ETag를 다시 보낸다 | source/actor/size를 보존 |
| M2 | 경계 판단 | 304와 representation version을 판단한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | request conditional header, cache policy, response status/age를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | etag가 같을 때 body 전송이 필요한지 판정한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`HTTP cache와 ETag·conditional request` 흐름을 framework 이름 없이 설명한다.

막히면 `client가 ETag를 다시 보낸다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · HTTP cache와 ETag·conditional request — 실전 경계 A

**경계 A.** ETag는 선택된 representation의 version 식별자로 사용해 client가 If-None-Match를 보내면 변경이 없을 때 304로 body 전송을 생략할 수 있다.

T07-P50/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P50에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P50/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P50/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P50): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P50/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P50-A1-321 | T07-P50 조건 | ETag는 선택된 representation의 version 식별자로 사용해 client가 If-None-Match를 보내면 변경이 없을 때 304로 body 전송을 생략할 수 있다. |
| T07-P50-A2-322 | T07-P50 변화점 | T07-P50/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P50-A3-323 | T07-P50 반례 | T07-P50/CH06 대표 실패와 A 위반을 구별 |
| T07-P50-A4-324 | T07-P50 근거 | T07-P50/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P50-A5-325 | T07-P50 재실험 | client가 ETag를 다시 보낸다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · HTTP cache와 ETag·conditional request — 실전 경계 B

**경계 B.** Cache-Control의 public·private·no-store는 누가 저장해도 되는지와 저장 자체를 피해야 하는지를 구분하므로 사용자별 민감 응답에 shared cache 정책을 잘못 적용하지 않는다.

T07-P50/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P50에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P50/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P50/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P50): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P50/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P50-B1-352 | T07-P50 조건 | Cache-Control의 public·private·no-store는 누가 저장해도 되는지와 저장 자체를 피해야 하는지를 구분하므로 사용자별 민감 응답에 shared cache 정책을 잘못 적용하지 않는다. |
| T07-P50-B2-353 | T07-P50 독립성 | T07-P50/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P50-B3-354 | T07-P50 상태 | T07-P50/CH03 before·after 위치를 다시 지정 |
| T07-P50-B4-355 | T07-P50 반증 | T07-P50/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P50-B5-356 | T07-P50 적용 | HTTP cache와 ETag·conditional request의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · HTTP cache와 ETag·conditional request — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다.**

아래 여섯 사례는 T07-P50의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P50-F01 | T07-P50: 대표 실패 | T07-P50: T07-P50/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P50: 현상만 보고 원인을 확정 | T07-P50/CH08 evidence map에서 상태를 대조 |
| T07-P50-F02 | T07-P50: 경계 A 누락 | T07-P50: T07-P50/CH04 경계 A 위반 입력 | T07-P50: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P50/CH08 evidence map에서 상태를 대조 |
| T07-P50-F03 | T07-P50: 경계 B 누락 | T07-P50: T07-P50/CH05 경계 B 위반 입력 | T07-P50: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P50/CH08 evidence map에서 상태를 대조 |
| T07-P50-F04 | T07-P50: 복구 경계 C 누락 | T07-P50: T07-P50/CH07 경계 C 복구 조건 | T07-P50: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P50/CH08 evidence map에서 상태를 대조 |
| T07-P50-F05 | T07-P50: 운영 경계 D 누락 | T07-P50: T07-P50/CH09 경계 D 운영 조건 | T07-P50: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P50/CH08 evidence map에서 상태를 대조 |
| T07-P50-F06 | T07-P50: 증거 없는 결론 | T07-P50: T07-P50/CH02 첫 판단만 존재 | T07-P50: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P50/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P50/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · HTTP cache와 ETag·conditional request — 복구 가능한 상태와 수명

**경계 C.** Vary header는 Accept-Encoding·Origin처럼 response가 달라지는 request header를 cache key에 포함하도록 알려 representation 혼합을 막는다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P50에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P50/CH08 evidence map을 본다. 복구 후에는 T07-P50/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P50에서 이미 확정된 side effect는 T07-P50/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P50-R1-414 | T07-P50 중단 직전 | T07-P50/CH03에서 이미 확정된 상태만 표시 |
| T07-P50-R2-415 | T07-P50 재시작 직후 | Vary header는 Accept-Encoding·Origin처럼 response가 달라지는 request header를 cache key에 포함하도록 알려 representation 혼합을 막는다. |
| T07-P50-R3-416 | T07-P50 재검증 | T07-P50/CH08 근거로 중복·누락 여부 확인 |
| T07-P50-R4-417 | T07-P50 재실행 | T07-P50/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · HTTP cache와 ETag·conditional request — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | client가 ETag를 다시 보낸다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | 304와 representation version을 판단한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | request conditional header, cache policy, response status/age를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`HTTP cache와 ETag·conditional request` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · HTTP cache와 ETag·conditional request — 운영 한계와 종료 조건

**경계 D.** 304는 ‘resource가 없다’가 아니라 client가 가진 representation이 여전히 유효하다는 응답이므로 body와 status 처리 코드를 일반 200과 구분한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P50/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P50/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P50 과제: 경계 D와 T07-P50/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P50-O1-476 | synthetic-load=156 | T07-P50/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P50-O2-477 | synthetic-budget=626ms | T07-P50 timeout과 unknown outcome을 분리 |
| T07-P50-O3-478 | T07-P50 종료 | 304는 ‘resource가 없다’가 아니라 client가 가진 representation이 여전히 유효하다는 응답이므로 body와 status 처리 코드를 일반 200과 구분한다. |
| T07-P50-O4-479 | T07-P50 완화 | T07-P50/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · HTTP cache와 ETag·conditional request — 직접 실행하는 작은 모델

`HTTP cache와 ETag·conditional request` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P050 4`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P050";
const current = 3;
const incoming = [2, 3, 4];
const next = incoming.reduce((state, v) => v > state ? v : state, current);
console.log(marker, next);
```

기준 출력: `T07-P050 4`.

`HTTP cache와 ETag·conditional request`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P50-L1-508 | constmarker="T07-P050"; | T07-P50 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P50-L2-509 | constcurrent=3; | T07-P50 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P50-L3-510 | constincoming=[2,3,4]; | T07-P50 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P50-L4-511 | constnext=incoming.reduce((state,v)=>v>state?v:state,current); | T07-P50 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P50-L5-512 | console.log(marker,next); | T07-P50 출력 관측점; 예상 `T07-P050 4`와 비교 |
| T07-P50-LX-597 | T07-P50 실행 기록 | T07-P50 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · HTTP cache와 ETag·conditional request — 한 부분만 수정하고 다시 예측

수정 과제: **incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다**.

수정 전은 `T07-P050 4`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P50/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P50-D1-538 | 기준 `T07-P050 4` | T07-P50 수정 전 실행을 먼저 재현 |
| T07-P50-D2-539 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | T07-P50 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P50-D3-540 | T07-P50 새 예측 | T07-P50 실행 전에 출력·상태를 먼저 기록 |
| T07-P50-D4-541 | T07-P50 재실행 | T07-P50/CH10 실제값과 새 예측을 대조 |
| T07-P50-D5-542 | T07-P50 반례 | T07-P50/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P50-D6-543 | T07-P50 근거 | T07-P50/CH08 상태가 설명과 일치해야 완료 |
| T07-P50-D7-544 | T07-P50 이유 | T07-P50 변경 이유를 HTTP cache와 ETag·conditional request 계약과 연결해 설명 |

## CHAPTER 12 · HTTP cache와 ETag·conditional request — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: HTTP cache와 ETag·conditional request | HTTP 자체에도 client/proxy가 응답을 재사용하거나 변경 여부만 확인하는 규칙이 있다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P50/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | request conditional header, cache policy, response status/age를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P50/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P50/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P50/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P50/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `HTTP cache와 ETag·conditional request` 실행 코드 수정 | incoming 순서를 거꾸로 바꾸고 결과가 같아야 하는 이유를 적는다 | `HTTP cache와 ETag·conditional request` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P50/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P50/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · HTTP cache와 ETag·conditional request — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | HTTP 자체에도 client/proxy가 응답을 재사용하거나 변경 여부만 확인하는 규칙이 있다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P50/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P50/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P50/CH10 실행용 boilerplate·test 후보 | T07-P50: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P050 4` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P50/CH02의 판단 기준과 T07-P50/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P50-AI1-600 | T07-P50 사람 결정 | T07-P50 업무 의미·허용 위험·완료 기준 소유 |
| T07-P50-AI2-601 | T07-P50 AI 초안 | T07-P50/CH10 boilerplate·test 후보까지만 위임 |
| T07-P50-AI3-602 | T07-P50 검증 | T07-P50/CH06 반례와 T07-P50/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · HTTP cache와 ETag·conditional request — 경계 조합 실험 8개

T07-P50: T07-P50/CH04~T07-P50/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P50의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-K01 | T07-P50: 경계 A | T07-P50: 경계 B | T07-P50: DRAIN[ready=0; active=6; drain_deadline_s=6] / sample=34 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + DRAIN_STATE |
| T07-P50-K02 | T07-P50: 경계 A | T07-P50: 경계 C | T07-P50: RECOVERY_SCOPE[selected=236; expected=11; backup=1; dry_run=0] / sample=41 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + RECOVERY_AUDIT |
| T07-P50-K03 | T07-P50: 경계 A | T07-P50: 경계 D | T07-P50: REPLAY[key=cmd-050-03; attempts=2; response_seen=0] / sample=48 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + IDEMPOTENCY_RECORD |
| T07-P50-K04 | T07-P50: 경계 B | T07-P50: 경계 C | T07-P50: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=32] / sample=55 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + STATE_VERSION |
| T07-P50-K05 | T07-P50: 경계 B | T07-P50: 경계 D | T07-P50: UNKNOWN_OUTCOME[timeout_ms=138; provider_state=UNKNOWN; lookup_id=p05005] / sample=62 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + PROVIDER_RESULT |
| T07-P50-K06 | T07-P50: 경계 C | T07-P50: 경계 D | T07-P50: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] / sample=69 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + DURABLE_STATE |
| T07-P50-K07 | T07-P50: 경계 A | T07-P50: 경계 B+C | T07-P50: OVERLOAD[rps=380; p99_ms=939; queue=28] / sample=76 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + QUEUE_PRESSURE |
| T07-P50-K08 | T07-P50: 경계 B | T07-P50: 경계 C+D | T07-P50: SAMPLING[sample_rate=14%; trace_present=0; metric_present=1] / sample=83 | T07-P50: 먼저 깨지는 경계를 판정 | T07-P50: T07-P50/CH08 + TRACE_METRIC_CROSSCHECK |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P50/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · HTTP cache와 ETag·conditional request — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P50와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-B04 | T07-P50: cache stampede와 single-flight | T07-P50: 같은 hot key에 100개 miss가 몰린다 | T07-P50: single-flight가 원본 호출을 합친다 | T07-P50: T07-P50/CH08 증거와 형제 LESSON 증거를 분리 | T07-P50: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P50-B06 | T07-P50: in-process cache와 distributed cache 선택 | T07-P50: instance 두 개가 서로 다른 local cache를 가진다 | T07-P50: 일관성과 network 비용을 비교한다 | T07-P50: T07-P50/CH08 증거와 형제 LESSON 증거를 분리 | T07-P50: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P50-B03 | T07-P50: TTL과 invalidation으로 stale 범위를 정하기 | T07-P50: 1,000개 key가 같은 초에 만료된다 | T07-P50: TTL jitter와 invalidation을 구분한다 | T07-P50: T07-P50/CH08 증거와 형제 LESSON 증거를 분리 | T07-P50: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P50-B07 | T07-P50: queue는 일을 나중에 처리하도록 경계를 만든다 | T07-P50: report 생성 요청을 queue에 넣는다 | T07-P50: 접수와 완료를 분리한다 | T07-P50: T07-P50/CH08 증거와 형제 LESSON 증거를 분리 | T07-P50: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P50-B02 | T07-P50: cache key는 값의 의미를 완전히 식별해야 한다 | T07-P50: 같은 query인데 tenant가 다르다 | T07-P50: cache key에 의미를 바꾸는 차원을 넣는다 | T07-P50: T07-P50/CH08 증거와 형제 LESSON 증거를 분리 | T07-P50: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · HTTP cache와 ETag·conditional request — 선택형 실패 주입 6개

T07-P50: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P50 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-X01 | T07-P50: 재전송 | T07-P50: 응답 유실 뒤 같은 command가 다시 도착함; sample=570 | T07-P50: 중복 side effect 여부 | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P50-X02 | T07-P50: 동시 변경 | T07-P50: 두 actor가 같은 resource를 수정함; sample=587 | T07-P50: lost update 또는 conflict | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: version·affected rows·lock/wait 기록 |
| T07-P50-X03 | T07-P50: unknown outcome | T07-P50: dependency timeout 후 성공 여부 불명; sample=604 | T07-P50: 실패와 미확정 결과 | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: provider id·조회 결과·retry history |
| T07-P50-X04 | T07-P50: 재시작 | T07-P50: side effect 직후 process가 재시작됨; sample=621 | T07-P50: durable state와 memory state | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: commit·outbox·job id·restart 전후 상태 |
| T07-P50-X05 | T07-P50: 과부하 | T07-P50: traffic 세 배, p99 급증; sample=638 | T07-P50: 기능 실패와 saturation | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: queue age·pool wait·CPU/event-loop·quota |
| T07-P50-X06 | T07-P50: sampling | T07-P50: 일부 log가 sampling으로 빠짐; sample=655 | T07-P50: 기록 부재와 사건 부재 | T07-P50: T07-P50/CH02 판단과 별도 기록 | T07-P50: metric·trace·durable state 교차 근거 |

## CHAPTER 17 · HTTP cache와 ETag·conditional request — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P50에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-E01 | T07-P50: 대표 실패를 원인으로 착각 | T07-P50: T07-P50/CH06 실패 case를 다른 입력으로 재현 | T07-P50: 현상과 원인을 같은 것으로 봄 | T07-P50: T07-P50/CH06 대표 실패와 T07-P50/CH08 증거를 다시 대조 | T07-P50: T07-P50/CH08 |
| T07-P50-E02 | T07-P50: 경계 A 생략 | T07-P50: T07-P50/CH04의 조건 하나를 반대로 설정 | T07-P50: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P50: T07-P50/CH04를 새 입력에 적용 | T07-P50: T07-P50/CH08 |
| T07-P50-E03 | T07-P50: 경계 B 생략 | T07-P50: T07-P50/CH05의 조건 하나를 반대로 설정 | T07-P50: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P50: T07-P50/CH05를 새 입력에 적용 | T07-P50: T07-P50/CH08 |
| T07-P50-E04 | T07-P50: 복구 상태 혼동 | T07-P50: T07-P50/CH07에서 처리 중단을 주입 | T07-P50: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P50: T07-P50/CH07에서 수명 경계를 다시 표시 | T07-P50: T07-P50/CH08 |
| T07-P50-E05 | T07-P50: 운영 한계 누락 | T07-P50: T07-P50/CH09에서 부하 또는 drain 조건을 변경 | T07-P50: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P50: T07-P50/CH09의 종료 조건을 다시 작성 | T07-P50: T07-P50/CH08 |
| T07-P50-E06 | T07-P50: 증거 없는 성공 판정 | T07-P50: T07-P50/CH08에서 증거 하나를 숨김 | T07-P50: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P50: T07-P50/CH08에서 독립 증거 둘을 선택 | T07-P50: T07-P50/CH08 |

## CHAPTER 18 · HTTP cache와 ETag·conditional request — synthetic 관측값 판독 6개

T07-P50: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P50의 숫자 하나만으로 원인을 단정하지 않고 T07-P50/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-O01 | T07-P50/cache_hit_ratio | 71 | T07-P50: cache hit 비율 | T07-P50: 축=과부하; 원인 확정 금지 | T07-P50: QUEUE_PRESSURE + T07-P50/CH08 |
| T07-P50-O02 | T07-P50/cache_age_s | 88 | T07-P50: cache 항목 나이 | T07-P50: 축=sampling; 원인 확정 금지 | T07-P50: TRACE_METRIC_CROSSCHECK + T07-P50/CH08 |
| T07-P50-O03 | T07-P50/queue_depth | 105 | T07-P50: 대기 job 수 | T07-P50: 축=client disconnect; 원인 확정 금지 | T07-P50: COMMIT_TIMELINE + T07-P50/CH08 |
| T07-P50-O04 | T07-P50/job_attempt | 122 | T07-P50: job 실행 횟수 | T07-P50: 축=재발; 원인 확정 금지 | T07-P50: RECURRENCE_TIMELINE + T07-P50/CH08 |
| T07-P50-O05 | T07-P50/dlq_count | 139 | T07-P50: DLQ 항목 수 | T07-P50: 축=대형 입력; 원인 확정 금지 | T07-P50: SIZE_LIMIT + T07-P50/CH08 |
| T07-P50-O06 | T07-P50/worker_latency_ms | 156 | T07-P50: worker 처리 지연 | T07-P50: 축=순서 역전; 원인 확정 금지 | T07-P50: SEQUENCE_STATE + T07-P50/CH08 |

## CHAPTER 19 · HTTP cache와 ETag·conditional request — 선택형 코드 리뷰 6질문

T07-P50: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P50에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-R01 | T07-P50: 관측 | T07-P50: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P50: SAMPLING[sample_rate=87%; trace_present=0; metric_present=1] | T07-P50: T07-P50/CH04 | T07-P50: TRACE_METRIC_CROSSCHECK |
| T07-P50-R02 | T07-P50: 복구 | T07-P50: 재시작 뒤에도 필요한 상태가 남는가 | T07-P50: DISCONNECT[disconnect_ms=110; commit_state=UNKNOWN; request=050-01] | T07-P50: T07-P50/CH05 | T07-P50: COMMIT_TIMELINE |
| T07-P50-R03 | T07-P50: 중복 | T07-P50: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P50: RECURRENCE[occurrence=4; interval_s=246; mitigation_applied=1] | T07-P50: T07-P50/CH06 | T07-P50: RECURRENCE_TIMELINE |
| T07-P50-R04 | T07-P50: timeout | T07-P50: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P50: LARGE_INPUT[body_kb=640; limit_kb=256; parsed=0] | T07-P50: T07-P50/CH07 | T07-P50: SIZE_LIMIT |
| T07-P50-R05 | T07-P50: 관측 | T07-P50: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P50: REORDER[in_seq=7,5,6; applied_version=3] | T07-P50: T07-P50/CH04 | T07-P50: SEQUENCE_STATE |
| T07-P50-R06 | T07-P50: 복구 | T07-P50: 재시작 뒤에도 필요한 상태가 남는가 | T07-P50: DRAIN[ready=0; active=12; drain_deadline_s=10] | T07-P50: T07-P50/CH05 | T07-P50: DRAIN_STATE |

## CHAPTER 20 · HTTP cache와 ETag·conditional request — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P50에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-I01 | T07-P50: correlation | T07-P50: 한 request/job/resource id를 시간축에 고정 | T07-P50: DISCONNECT[disconnect_ms=97; commit_state=UNKNOWN; request=050-00] | T07-P50: T07-P50/CH08 + COMMIT_TIMELINE | T07-P50-incident-950 |
| T07-P50-I02 | T07-P50: 마지막 정상 | T07-P50: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P50: RECURRENCE[occurrence=3; interval_s=235; mitigation_applied=1] | T07-P50: T07-P50/CH08 + RECURRENCE_TIMELINE | T07-P50-incident-951 |
| T07-P50-I03 | T07-P50: 가설 검증 | T07-P50: 원인 후보 하나만 뒤집어 재현 | T07-P50: LARGE_INPUT[body_kb=2240; limit_kb=256; parsed=0] | T07-P50: T07-P50/CH08 + SIZE_LIMIT | T07-P50-incident-952 |
| T07-P50-I04 | T07-P50: 복구 확인 | T07-P50: durable state와 사용자 결과를 모두 확인 | T07-P50: REORDER[in_seq=6,4,5; applied_version=3] | T07-P50: T07-P50/CH08 + SEQUENCE_STATE | T07-P50-incident-953 |
| T07-P50-I05 | T07-P50: 재주입 | T07-P50: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P50: DRAIN[ready=0; active=16; drain_deadline_s=9] | T07-P50: T07-P50/CH08 + DRAIN_STATE | T07-P50-incident-954 |
| T07-P50-I06 | T07-P50: 회귀 고정 | T07-P50: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P50: RECOVERY_SCOPE[selected=58; expected=12; backup=1; dry_run=1] | T07-P50: T07-P50/CH08 + RECOVERY_AUDIT | T07-P50-incident-955 |

## CHAPTER 21 · HTTP cache와 ETag·conditional request — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P50/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P50/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P50/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P50/CH04~T07-P50/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P50/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P50/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P50/CH18 signal 두 개와 T07-P50/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P50/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P50/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · HTTP cache와 ETag·conditional request — 통합 casebook 16문제

T07-P50 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P50 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P50-C01 | T07-P50: 경계 A | T07-P50: RECURRENCE[occurrence=2; interval_s=224; mitigation_applied=1] | T07-P50: load=650, window=50s | T07-P50: review=자원 | T07-P50: 경계 A 위반 여부를 판정 | T07-P50: RECURRENCE_TIMELINE + T07-P50/CH08 |
| T07-P50-C02 | T07-P50: 경계 B | T07-P50: LARGE_INPUT[body_kb=2152; limit_kb=256; parsed=0] | T07-P50: load=673, window=69s | T07-P50: review=순서 | T07-P50: 경계 B 위반 여부를 판정 | T07-P50: SIZE_LIMIT + T07-P50/CH08 |
| T07-P50-C03 | T07-P50: 경계 C | T07-P50: REORDER[in_seq=5,3,4; applied_version=3] | T07-P50: load=696, window=88s | T07-P50: review=관측 | T07-P50: 경계 C 위반 여부를 판정 | T07-P50: SEQUENCE_STATE + T07-P50/CH08 |
| T07-P50-C04 | T07-P50: 경계 D | T07-P50: DRAIN[ready=0; active=3; drain_deadline_s=8] | T07-P50: load=719, window=17s | T07-P50: review=상태 변경 | T07-P50: 경계 D 위반 여부를 판정 | T07-P50: DRAIN_STATE + T07-P50/CH08 |
| T07-P50-C05 | T07-P50: 경계 A | T07-P50: RECOVERY_SCOPE[selected=47; expected=18; backup=1; dry_run=0] | T07-P50: load=742, window=36s | T07-P50: review=retry | T07-P50: 경계 A 위반 여부를 판정 | T07-P50: RECOVERY_AUDIT + T07-P50/CH08 |
| T07-P50-C06 | T07-P50: 경계 B | T07-P50: REPLAY[key=cmd-050-05; attempts=4; response_seen=0] | T07-P50: load=765, window=55s | T07-P50: review=복구 | T07-P50: 경계 B 위반 여부를 판정 | T07-P50: IDEMPOTENCY_RECORD + T07-P50/CH08 |
| T07-P50-C07 | T07-P50: 경계 C | T07-P50: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=58] | T07-P50: load=788, window=74s | T07-P50: review=동시성 | T07-P50: 경계 C 위반 여부를 판정 | T07-P50: STATE_VERSION + T07-P50/CH08 |
| T07-P50-C08 | T07-P50: 경계 D | T07-P50: UNKNOWN_OUTCOME[timeout_ms=160; provider_state=UNKNOWN; lookup_id=p05007] | T07-P50: load=811, window=93s | T07-P50: review=민감정보 | T07-P50: 경계 D 위반 여부를 판정 | T07-P50: PROVIDER_RESULT + T07-P50/CH08 |
| T07-P50-C09 | T07-P50: 경계 A | T07-P50: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P50: load=834, window=22s | T07-P50: review=중복 | T07-P50: 경계 A 위반 여부를 판정 | T07-P50: DURABLE_STATE + T07-P50/CH08 |
| T07-P50-C10 | T07-P50: 경계 B | T07-P50: OVERLOAD[rps=446; p99_ms=1173; queue=36] | T07-P50: load=857, window=41s | T07-P50: review=권한 | T07-P50: 경계 B 위반 여부를 판정 | T07-P50: QUEUE_PRESSURE + T07-P50/CH08 |
| T07-P50-C11 | T07-P50: 경계 C | T07-P50: SAMPLING[sample_rate=23%; trace_present=0; metric_present=1] | T07-P50: load=880, window=60s | T07-P50: review=입력 경계 | T07-P50: 경계 C 위반 여부를 판정 | T07-P50: TRACE_METRIC_CROSSCHECK + T07-P50/CH08 |
| T07-P50-C12 | T07-P50: 경계 D | T07-P50: DISCONNECT[disconnect_ms=46; commit_state=UNKNOWN; request=050-11] | T07-P50: load=903, window=79s | T07-P50: review=timeout | T07-P50: 경계 D 위반 여부를 판정 | T07-P50: COMMIT_TIMELINE + T07-P50/CH08 |
| T07-P50-C13 | T07-P50: 경계 A | T07-P50: RECURRENCE[occurrence=4; interval_s=145; mitigation_applied=1] | T07-P50: load=926, window=98s | T07-P50: review=자원 | T07-P50: 경계 A 위반 여부를 판정 | T07-P50: RECURRENCE_TIMELINE + T07-P50/CH08 |
| T07-P50-C14 | T07-P50: 경계 B | T07-P50: LARGE_INPUT[body_kb=1520; limit_kb=256; parsed=0] | T07-P50: load=949, window=27s | T07-P50: review=순서 | T07-P50: 경계 B 위반 여부를 판정 | T07-P50: SIZE_LIMIT + T07-P50/CH08 |
| T07-P50-C15 | T07-P50: 경계 C | T07-P50: REORDER[in_seq=17,15,16; applied_version=3] | T07-P50: load=972, window=46s | T07-P50: review=관측 | T07-P50: 경계 C 위반 여부를 판정 | T07-P50: SEQUENCE_STATE + T07-P50/CH08 |
| T07-P50-C16 | T07-P50: 경계 D | T07-P50: DRAIN[ready=0; active=11; drain_deadline_s=12] | T07-P50: load=995, window=65s | T07-P50: review=상태 변경 | T07-P50: 경계 D 위반 여부를 판정 | T07-P50: DRAIN_STATE + T07-P50/CH08 |

채점은 결론보다 근거를 본다. T07-P50/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · HTTP cache와 ETag·conditional request — evidence 판독 문제 14개

T07-P50 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P50 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P50-V01 | T07-P50: latency=270ms; queue=30; retry=1 | T07-P50: 대형 입력 | T07-P50: 축=대형 입력; 원인 확정은 보류 | T07-P50: SIZE_LIMIT + T07-P50/CH08 | T07-P50: 피할 오판=순서 가정 |
| T07-P50-V02 | T07-P50: latency=337ms; queue=41; retry=4 | T07-P50: 순서 역전 | T07-P50: 축=순서 역전; 원인 확정은 보류 | T07-P50: SEQUENCE_STATE + T07-P50/CH08 | T07-P50: 피할 오판=상태 수명 혼동 |
| T07-P50-V03 | T07-P50: latency=404ms; queue=52; retry=0 | T07-P50: drain | T07-P50: 축=drain; 원인 확정은 보류 | T07-P50: DRAIN_STATE + T07-P50/CH08 | T07-P50: 피할 오판=운영 한계 누락 |
| T07-P50-V04 | T07-P50: latency=471ms; queue=63; retry=3 | T07-P50: 복구 범위 | T07-P50: 축=복구 범위; 원인 확정은 보류 | T07-P50: RECOVERY_AUDIT + T07-P50/CH08 | T07-P50: 피할 오판=오류 합치기 |
| T07-P50-V05 | T07-P50: latency=538ms; queue=74; retry=6 | T07-P50: 재전송 | T07-P50: 축=재전송; 원인 확정은 보류 | T07-P50: IDEMPOTENCY_RECORD + T07-P50/CH08 | T07-P50: 피할 오판=복구 과잉 |
| T07-P50-V06 | T07-P50: latency=605ms; queue=5; retry=2 | T07-P50: 동시 변경 | T07-P50: 축=동시 변경; 원인 확정은 보류 | T07-P50: STATE_VERSION + T07-P50/CH08 | T07-P50: 피할 오판=잘못된 전제 |
| T07-P50-V07 | T07-P50: latency=672ms; queue=16; retry=5 | T07-P50: unknown outcome | T07-P50: 축=unknown outcome; 원인 확정은 보류 | T07-P50: PROVIDER_RESULT + T07-P50/CH08 | T07-P50: 피할 오판=경계 누락 |
| T07-P50-V08 | T07-P50: latency=739ms; queue=27; retry=1 | T07-P50: 재시작 | T07-P50: 축=재시작; 원인 확정은 보류 | T07-P50: DURABLE_STATE + T07-P50/CH08 | T07-P50: 피할 오판=증거 혼동 |
| T07-P50-V09 | T07-P50: latency=806ms; queue=38; retry=4 | T07-P50: 과부하 | T07-P50: 축=과부하; 원인 확정은 보류 | T07-P50: QUEUE_PRESSURE + T07-P50/CH08 | T07-P50: 피할 오판=재시도 오판 |
| T07-P50-V10 | T07-P50: latency=873ms; queue=49; retry=0 | T07-P50: sampling | T07-P50: 축=sampling; 원인 확정은 보류 | T07-P50: TRACE_METRIC_CROSSCHECK + T07-P50/CH08 | T07-P50: 피할 오판=동시성 무시 |
| T07-P50-V11 | T07-P50: latency=940ms; queue=60; retry=3 | T07-P50: client disconnect | T07-P50: 축=client disconnect; 원인 확정은 보류 | T07-P50: COMMIT_TIMELINE + T07-P50/CH08 | T07-P50: 피할 오판=순서 가정 |
| T07-P50-V12 | T07-P50: latency=1007ms; queue=71; retry=6 | T07-P50: 재발 | T07-P50: 축=재발; 원인 확정은 보류 | T07-P50: RECURRENCE_TIMELINE + T07-P50/CH08 | T07-P50: 피할 오판=상태 수명 혼동 |
| T07-P50-V13 | T07-P50: latency=1074ms; queue=2; retry=2 | T07-P50: 대형 입력 | T07-P50: 축=대형 입력; 원인 확정은 보류 | T07-P50: SIZE_LIMIT + T07-P50/CH08 | T07-P50: 피할 오판=운영 한계 누락 |
| T07-P50-V14 | T07-P50: latency=1141ms; queue=13; retry=5 | T07-P50: 순서 역전 | T07-P50: 축=순서 역전; 원인 확정은 보류 | T07-P50: SEQUENCE_STATE + T07-P50/CH08 | T07-P50: 피할 오판=오류 합치기 |

## CHAPTER 24 · HTTP cache와 ETag·conditional request — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P50에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P50-D01 | T07-P50: 강제 종료 | T07-P50: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P50: in-flight request와 background work의 결과를 잃는지 | T07-P50: DRAIN[ready=0; active=10; drain_deadline_s=5] | T07-P50: DRAIN_STATE + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D02 | T07-P50: AI package 추가 | T07-P50: AI가 제안한 새 dependency를 도입한다 | T07-P50: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P50: RECOVERY_SCOPE[selected=225; expected=19; backup=1; dry_run=1] | T07-P50: RECOVERY_AUDIT + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D03 | T07-P50: 비동기화 | T07-P50: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P50: durability·status API·worker retry 계약이 생기는지 | T07-P50: REPLAY[key=cmd-050-02; attempts=4; response_seen=0] | T07-P50: IDEMPOTENCY_RECORD + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D04 | T07-P50: 권한 shortcut | T07-P50: payload의 owner/tenant id를 바로 사용한다 | T07-P50: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P50: CONCURRENT_WRITE[actors=2; base_version=6; writes=2; gap_ms=19] | T07-P50: STATE_VERSION + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D05 | T07-P50: 순서 병렬화 | T07-P50: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P50: 선후관계 invariant와 race를 깨지 않는지 | T07-P50: UNKNOWN_OUTCOME[timeout_ms=127; provider_state=UNKNOWN; lookup_id=p05004] | T07-P50: PROVIDER_RESULT + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D06 | T07-P50: validation 이동 | T07-P50: validation을 business side effect 뒤로 옮긴다 | T07-P50: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P50: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P50: DURABLE_STATE + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D07 | T07-P50: batch 확대 | T07-P50: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P50: memory·deadline·부분 실패 범위가 커지는지 | T07-P50: OVERLOAD[rps=347; p99_ms=822; queue=34] | T07-P50: QUEUE_PRESSURE + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P50-D08 | T07-P50: fallback 추가 | T07-P50: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P50: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P50: SAMPLING[sample_rate=81%; trace_present=1; metric_present=1] | T07-P50: TRACE_METRIC_CROSSCHECK + T07-P50/CH08 | T07-P50: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P50: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · HTTP cache와 ETag·conditional request — 최종 contract와 evidence spine

**최종 계약:** HTTP 자체에도 client/proxy가 응답을 재사용하거나 변경 여부만 확인하는 규칙이 있다.

**정상 메커니즘:** Cache-Control로 재사용 정책을 표현하고 validator인 ETag/Last-Modified와 If-None-Match 등을 사용해 304를 만들 수 있다.

**대표 실패:** 개인화 응답을 shared cache에 공개하거나 ETag를 단순 version 번호 의미와 혼동한다.

**검증 evidence:** request conditional header, cache policy, response status/age를 확인한다.

**직접 행동:** etag가 같을 때 body 전송이 필요한지 판정한다.

**다음 연결:** `in-process cache와 distributed cache 선택`.

`HTTP cache와 ETag·conditional request`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| DDIA | DDIA | HTTP cache와 ETag·conditional request의 개념·실패·운영 판단 교차 확인 |
| RFC9111 | HTTP Caching | HTTP cache와 ETag·conditional request의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | HTTP cache와 ETag·conditional request의 개념·실패·운영 판단 교차 확인 |
| SRE-WORKBOOK | SRE-WORKBOOK | HTTP cache와 ETag·conditional request의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | HTTP cache와 ETag·conditional request의 개념·실패·운영 판단 교차 확인 |

`HTTP cache와 ETag·conditional request` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
