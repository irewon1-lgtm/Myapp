# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 04 · parsing은 문자열을 구조로 읽는 일

## CHAPTER 01 · parsing은 문자열을 구조로 읽는 일 — 쉬운 말에서 정확한 계약까지

**출발 개념.** parsing은 네트워크에서 온 bytes나 text를 프로그램이 다룰 구조로 바꾸는 단계다.

**아주 쉬운 사건.** Content-Type은 JSON인데 body 마지막 괄호가 빠졌다. 이 사건에서는 먼저 **parsing 실패와 validation 실패를 구분한다**.

**왜 필요한가.** 정상 동작은 Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다. 반대로 JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다.

**암기:** `parsing은 문자열을 구조로 읽는 일`의 역할 한 줄.

**직접 이해:** `parsing은 문자열을 구조로 읽는 일`의 입력·상태·결과 경계.

**AI 위임 가능:** `parsing은 문자열을 구조로 읽는 일` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `Content-Type은 JSON인데 body 마지막 괄호가 빠졌다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 Content-Type, body byte 수, parse 성공/실패 위치, parser 오류 종류를 확인한다.

## CHAPTER 02 · parsing은 문자열을 구조로 읽는 일 — 아주 쉬운 예를 한 단계씩 해석

T07-P04: `Content-Type은 JSON인데 body 마지막 괄호가 빠졌다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | T07-P04 외부 입력 | T07-P04: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | parsing 실패와 validation 실패를 구분한다 | T07-P04 판단 기준 | T07-P04/CH08 관측표와 대조 |
| 정상 경로 | T07-P04/CH03 M1→M5 | parsing은 문자열을 구조로 읽는 일: 완료 시점을 단계별로 분리 | T07-P04: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P04/CH06 JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다. | T07-P04: 깨진 계약 하나를 특정 | parsing은 문자열을 구조로 읽는 일: 증상과 원인을 분리 |
| 재검증 | T07-P04/CH10 직접 실행 | T07-P04: 예상값 T07-P004 a → p4 → b 기록 | parsing은 문자열을 구조로 읽는 일: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P04/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P04/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · parsing은 문자열을 구조로 읽는 일 — 내부 메커니즘과 상태 전이

Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | source/actor/size를 보존 |
| M2 | 경계 판단 | parsing 실패와 validation 실패를 구분한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | Content-Type, body byte 수, parse 성공/실패 위치, parser 오류 종류를 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | JSON처럼 생긴 입력을 단순 객체로 가정하지 않고 구조 검사 전후를 비교한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`parsing은 문자열을 구조로 읽는 일` 흐름을 framework 이름 없이 설명한다.

막히면 `Content-Type은 JSON인데 body 마지막 괄호가 빠졌다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · parsing은 문자열을 구조로 읽는 일 — 실전 경계 A

**경계 A.** JSON 문법이 깨진 400과 서버가 지원하지 않는 media type의 415는 실패 원인이 다르다. parser가 읽지 못한 것과 아예 다른 형식을 보낸 것을 같은 오류로 합치지 않는다.

T07-P04/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P04에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P04/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P04/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P04): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P04/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P04-A1-484 | T07-P04 조건 | JSON 문법이 깨진 400과 서버가 지원하지 않는 media type의 415는 실패 원인이 다르다. parser가 읽지 못한 것과 아예 다른 형식을 보낸 것을 같은 오류로 합치지 않는다. |
| T07-P04-A2-485 | T07-P04 변화점 | T07-P04/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P04-A3-486 | T07-P04 반례 | T07-P04/CH06 대표 실패와 A 위반을 구별 |
| T07-P04-A4-487 | T07-P04 근거 | T07-P04/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P04-A5-488 | T07-P04 재실험 | Content-Type은 JSON인데 body 마지막 괄호가 빠졌다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · parsing은 문자열을 구조로 읽는 일 — 실전 경계 B

**경계 B.** body를 stream으로 받을 때는 중간 연결 종료나 선언된 길이와 실제 수신량 불일치가 있을 수 있으므로 ‘parse 함수가 호출됐다’는 사실만으로 완전한 payload 수신을 가정하지 않는다.

T07-P04/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P04에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P04/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P04/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P04): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P04/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P04-B1-515 | T07-P04 조건 | body를 stream으로 받을 때는 중간 연결 종료나 선언된 길이와 실제 수신량 불일치가 있을 수 있으므로 ‘parse 함수가 호출됐다’는 사실만으로 완전한 payload 수신을 가정하지 않는다. |
| T07-P04-B2-516 | T07-P04 독립성 | T07-P04/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P04-B3-517 | T07-P04 상태 | T07-P04/CH03 before·after 위치를 다시 지정 |
| T07-P04-B4-518 | T07-P04 반증 | T07-P04/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P04-B5-519 | T07-P04 적용 | parsing은 문자열을 구조로 읽는 일의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · parsing은 문자열을 구조로 읽는 일 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다.**

아래 여섯 사례는 T07-P04의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P04-F01 | T07-P04: 대표 실패 | T07-P04: T07-P04/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P04: 현상만 보고 원인을 확정 | T07-P04/CH08 evidence map에서 상태를 대조 |
| T07-P04-F02 | T07-P04: 경계 A 누락 | T07-P04: T07-P04/CH04 경계 A 위반 입력 | T07-P04: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P04/CH08 evidence map에서 상태를 대조 |
| T07-P04-F03 | T07-P04: 경계 B 누락 | T07-P04: T07-P04/CH05 경계 B 위반 입력 | T07-P04: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P04/CH08 evidence map에서 상태를 대조 |
| T07-P04-F04 | T07-P04: 복구 경계 C 누락 | T07-P04: T07-P04/CH07 경계 C 복구 조건 | T07-P04: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P04/CH08 evidence map에서 상태를 대조 |
| T07-P04-F05 | T07-P04: 운영 경계 D 누락 | T07-P04: T07-P04/CH09 경계 D 운영 조건 | T07-P04: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P04/CH08 evidence map에서 상태를 대조 |
| T07-P04-F06 | T07-P04: 증거 없는 결론 | T07-P04: T07-P04/CH02 첫 판단만 존재 | T07-P04: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P04/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P04/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · parsing은 문자열을 구조로 읽는 일 — 복구 가능한 상태와 수명

**경계 C.** body size 제한은 전체 payload를 메모리에 올린 뒤 검사하면 늦다. 가능한 한 읽는 단계에서 upper bound를 적용해야 큰 입력 하나가 process memory를 압박하는 것을 막을 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P04에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P04/CH08 evidence map을 본다. 복구 후에는 T07-P04/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P04에서 이미 확정된 side effect는 T07-P04/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P04-R1-577 | T07-P04 중단 직전 | T07-P04/CH03에서 이미 확정된 상태만 표시 |
| T07-P04-R2-578 | T07-P04 재시작 직후 | body size 제한은 전체 payload를 메모리에 올린 뒤 검사하면 늦다. 가능한 한 읽는 단계에서 upper bound를 적용해야 큰 입력 하나가 process memory를 압박하는 것을 막을 수 있다. |
| T07-P04-R3-579 | T07-P04 재검증 | T07-P04/CH08 근거로 중복·누락 여부 확인 |
| T07-P04-R4-580 | T07-P04 재실행 | T07-P04/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · parsing은 문자열을 구조로 읽는 일 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | parsing 실패와 validation 실패를 구분한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | Content-Type, body byte 수, parse 성공/실패 위치, parser 오류 종류를 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`parsing은 문자열을 구조로 읽는 일` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · parsing은 문자열을 구조로 읽는 일 — 운영 한계와 종료 조건

**경계 D.** parser 성공은 구조를 읽었다는 뜻일 뿐 schema validation 성공이 아니다. {"age":-900}은 JSON으로는 정상이어도 업무 입력으로는 거부되어야 한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P04/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P04/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P04 과제: 경계 D와 T07-P04/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P04-O1-639 | synthetic-load=139 | T07-P04/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P04-O2-640 | synthetic-budget=789ms | T07-P04 timeout과 unknown outcome을 분리 |
| T07-P04-O3-641 | T07-P04 종료 | parser 성공은 구조를 읽었다는 뜻일 뿐 schema validation 성공이 아니다. {"age":-900}은 JSON으로는 정상이어도 업무 입력으로는 거부되어야 한다. |
| T07-P04-O4-642 | T07-P04 완화 | T07-P04/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · parsing은 문자열을 구조로 읽는 일 — 직접 실행하는 작은 모델

`parsing은 문자열을 구조로 읽는 일` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P004 a|p4|b`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P004";
const ids = ["a", "p4", "a", "p4", "b"];
const unique = [...new Set(ids)];
console.log(marker, unique.join("|"));
```

기준 출력: `T07-P004 a|p4|b`.

`parsing은 문자열을 구조로 읽는 일`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P04-L1-671 | constmarker="T07-P004"; | T07-P04 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P04-L2-672 | constids=["a","p4","a","p4","b"]; | T07-P04 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P04-L3-673 | constunique=[...newSet(ids)]; | T07-P04 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P04-L4-674 | console.log(marker,unique.join("→")); | T07-P04 출력 관측점; 예상 `T07-P004 a→p4→b`와 비교 |
| T07-P04-LX-760 | T07-P04 실행 기록 | T07-P04 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · parsing은 문자열을 구조로 읽는 일 — 한 부분만 수정하고 다시 예측

수정 과제: **마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다**.

수정 전은 `T07-P004 a|p4|b`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P04/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P04-D1-701 | 기준 `T07-P004 a→p4→b` | T07-P04 수정 전 실행을 먼저 재현 |
| T07-P04-D2-702 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | T07-P04 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P04-D3-703 | T07-P04 새 예측 | T07-P04 실행 전에 출력·상태를 먼저 기록 |
| T07-P04-D4-704 | T07-P04 재실행 | T07-P04/CH10 실제값과 새 예측을 대조 |
| T07-P04-D5-705 | T07-P04 반례 | T07-P04/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P04-D6-706 | T07-P04 근거 | T07-P04/CH08 상태가 설명과 일치해야 완료 |
| T07-P04-D7-707 | T07-P04 이유 | T07-P04 변경 이유를 parsing은 문자열을 구조로 읽는 일 계약과 연결해 설명 |

## CHAPTER 12 · parsing은 문자열을 구조로 읽는 일 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: parsing은 문자열을 구조로 읽는 일 | parsing은 네트워크에서 온 bytes나 text를 프로그램이 다룰 구조로 바꾸는 단계다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P04/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | Content-Type, body byte 수, parse 성공/실패 위치, parser 오류 종류를 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P04/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P04/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P04/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P04/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `parsing은 문자열을 구조로 읽는 일` 실행 코드 수정 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | `parsing은 문자열을 구조로 읽는 일` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P04/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P04/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · parsing은 문자열을 구조로 읽는 일 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | parsing은 네트워크에서 온 bytes나 text를 프로그램이 다룰 구조로 바꾸는 단계다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P04/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P04/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P04/CH10 실행용 boilerplate·test 후보 | T07-P04: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P004 a → p4 → b` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P04/CH02의 판단 기준과 T07-P04/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P04-AI1-763 | T07-P04 사람 결정 | T07-P04 업무 의미·허용 위험·완료 기준 소유 |
| T07-P04-AI2-764 | T07-P04 AI 초안 | T07-P04/CH10 boilerplate·test 후보까지만 위임 |
| T07-P04-AI3-765 | T07-P04 검증 | T07-P04/CH06 반례와 T07-P04/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · parsing은 문자열을 구조로 읽는 일 — 경계 조합 실험 8개

T07-P04: T07-P04/CH04~T07-P04/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P04의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-K01 | T07-P04: 경계 A | T07-P04: 경계 B | T07-P04: DISCONNECT[disconnect_ms=104; commit_state=UNKNOWN; request=004-01] / sample=62 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + COMMIT_TIMELINE |
| T07-P04-K02 | T07-P04: 경계 A | T07-P04: 경계 C | T07-P04: RECURRENCE[occurrence=4; interval_s=178; mitigation_applied=1] / sample=69 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + RECURRENCE_TIMELINE |
| T07-P04-K03 | T07-P04: 경계 A | T07-P04: 경계 D | T07-P04: LARGE_INPUT[body_kb=1784; limit_kb=768; parsed=0] / sample=76 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + SIZE_LIMIT |
| T07-P04-K04 | T07-P04: 경계 B | T07-P04: 경계 C | T07-P04: DRAIN[ready=0; active=10; drain_deadline_s=9] / sample=83 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + DRAIN_STATE |
| T07-P04-K05 | T07-P04: 경계 B | T07-P04: 경계 D | T07-P04: REPLAY[key=cmd-004-05; attempts=4; response_seen=0] / sample=90 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + IDEMPOTENCY_RECORD |
| T07-P04-K06 | T07-P04: 경계 C | T07-P04: 경계 D | T07-P04: OLD_SCHEMA[client=v3; server=v4; extra_field=0] / sample=97 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + CLIENT_VERSION |
| T07-P04-K07 | T07-P04: 경계 A | T07-P04: 경계 B+C | T07-P04: UNKNOWN_OUTCOME[timeout_ms=303; provider_state=UNKNOWN; lookup_id=p00407] / sample=15 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + PROVIDER_RESULT |
| T07-P04-K08 | T07-P04: 경계 B | T07-P04: 경계 C+D | T07-P04: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] / sample=22 | T07-P04: 먼저 깨지는 경계를 판정 | T07-P04: T07-P04/CH08 + DURABLE_STATE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P04/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · parsing은 문자열을 구조로 읽는 일 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P04와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-B03 | T07-P04: path parameter·query·header·body를 구분하기 | T07-P04: ?tag=a&tag=b와 body가 함께 온다 | T07-P04: 값의 출처를 path/query/header/body로 표시한다 | T07-P04: T07-P04/CH08 증거와 형제 LESSON 증거를 분리 | T07-P04: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P04-B05 | T07-P04: middleware는 공통 단계를 순서대로 연결한다 | T07-P04: auth middleware가 parser보다 먼저/뒤에 놓인다 | T07-P04: middleware 순서를 화살표로 그린다 | T07-P04: T07-P04/CH08 증거와 형제 LESSON 증거를 분리 | T07-P04: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P04-B02 | T07-P04: route는 요청을 처리할 코드로 연결하는 규칙 | T07-P04: GET /users/me가 /users/:id로 잘못 들어간다 | T07-P04: route table의 method와 specificity를 확인한다 | T07-P04: T07-P04/CH08 증거와 형제 LESSON 증거를 분리 | T07-P04: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P04-B06 | T07-P04: validation은 외부 입력을 내부 계약으로 바꾸는 문 | T07-P04: age="20", admin=true가 입력으로 온다 | T07-P04: coercion과 허용 field를 따로 결정한다 | T07-P04: T07-P04/CH08 증거와 형제 LESSON 증거를 분리 | T07-P04: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P04-B01 | T07-P04: 서버 프로그램과 요청의 첫 진입점 | T07-P04: 요청 A가 앱까지 도착하지 않는다 | T07-P04: process/listen/network 경계를 분리해서 본다 | T07-P04: T07-P04/CH08 증거와 형제 LESSON 증거를 분리 | T07-P04: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · parsing은 문자열을 구조로 읽는 일 — 선택형 실패 주입 6개

T07-P04: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P04 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-X01 | T07-P04: 대형 입력 | T07-P04: 입력 크기가 정상의 100배; sample=141 | T07-P04: 의미 검증과 resource limit | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: body/batch size·parse time·memory·reject status |
| T07-P04-X02 | T07-P04: drain | T07-P04: 배포 중 기존 요청이 처리 중; sample=158 | T07-P04: 새 traffic 차단과 in-flight 처리 | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: readiness·active requests·deadline·final state |
| T07-P04-X03 | T07-P04: 재전송 | T07-P04: 응답 유실 뒤 같은 command가 다시 도착함; sample=175 | T07-P04: 중복 side effect 여부 | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P04-X04 | T07-P04: 구버전 client | T07-P04: 한 단계 이전 schema가 요청됨; sample=192 | T07-P04: 호환 입력과 breaking change | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: schema version·실제 client 분포·contract test |
| T07-P04-X05 | T07-P04: unknown outcome | T07-P04: dependency timeout 후 성공 여부 불명; sample=209 | T07-P04: 실패와 미확정 결과 | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: provider id·조회 결과·retry history |
| T07-P04-X06 | T07-P04: 재시작 | T07-P04: side effect 직후 process가 재시작됨; sample=226 | T07-P04: durable state와 memory state | T07-P04: T07-P04/CH02 판단과 별도 기록 | T07-P04: commit·outbox·job id·restart 전후 상태 |

## CHAPTER 17 · parsing은 문자열을 구조로 읽는 일 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P04에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-E01 | T07-P04: 대표 실패를 원인으로 착각 | T07-P04: T07-P04/CH06 실패 case를 다른 입력으로 재현 | T07-P04: 현상과 원인을 같은 것으로 봄 | T07-P04: T07-P04/CH06 대표 실패와 T07-P04/CH08 증거를 다시 대조 | T07-P04: T07-P04/CH08 |
| T07-P04-E02 | T07-P04: 경계 A 생략 | T07-P04: T07-P04/CH04의 조건 하나를 반대로 설정 | T07-P04: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P04: T07-P04/CH04를 새 입력에 적용 | T07-P04: T07-P04/CH08 |
| T07-P04-E03 | T07-P04: 경계 B 생략 | T07-P04: T07-P04/CH05의 조건 하나를 반대로 설정 | T07-P04: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P04: T07-P04/CH05를 새 입력에 적용 | T07-P04: T07-P04/CH08 |
| T07-P04-E04 | T07-P04: 복구 상태 혼동 | T07-P04: T07-P04/CH07에서 처리 중단을 주입 | T07-P04: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P04: T07-P04/CH07에서 수명 경계를 다시 표시 | T07-P04: T07-P04/CH08 |
| T07-P04-E05 | T07-P04: 운영 한계 누락 | T07-P04: T07-P04/CH09에서 부하 또는 drain 조건을 변경 | T07-P04: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P04: T07-P04/CH09의 종료 조건을 다시 작성 | T07-P04: T07-P04/CH08 |
| T07-P04-E06 | T07-P04: 증거 없는 성공 판정 | T07-P04: T07-P04/CH08에서 증거 하나를 숨김 | T07-P04: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P04: T07-P04/CH08에서 독립 증거 둘을 선택 | T07-P04: T07-P04/CH08 |

## CHAPTER 18 · parsing은 문자열을 구조로 읽는 일 — synthetic 관측값 판독 6개

T07-P04: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P04의 숫자 하나만으로 원인을 단정하지 않고 T07-P04/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-O01 | T07-P04/requests_received | 260 | T07-P04: 요청 수신 수 | T07-P04: 축=unknown outcome; 원인 확정 금지 | T07-P04: PROVIDER_RESULT + T07-P04/CH08 |
| T07-P04-O02 | T07-P04/handler_started | 277 | T07-P04: handler 진입 수 | T07-P04: 축=재시작; 원인 확정 금지 | T07-P04: DURABLE_STATE + T07-P04/CH08 |
| T07-P04-O03 | T07-P04/responses_completed | 294 | T07-P04: 응답 완료 수 | T07-P04: 축=과부하; 원인 확정 금지 | T07-P04: QUEUE_PRESSURE + T07-P04/CH08 |
| T07-P04-O04 | T07-P04/latency_ms | 311 | T07-P04: 요청 처리 지연 | T07-P04: 축=sampling; 원인 확정 금지 | T07-P04: TRACE_METRIC_CROSSCHECK + T07-P04/CH08 |
| T07-P04-O05 | T07-P04/body_kb | 328 | T07-P04: 입력 크기 | T07-P04: 축=client disconnect; 원인 확정 금지 | T07-P04: COMMIT_TIMELINE + T07-P04/CH08 |
| T07-P04-O06 | T07-P04/active_connections | 345 | T07-P04: 활성 연결 수 | T07-P04: 축=재발; 원인 확정 금지 | T07-P04: RECURRENCE_TIMELINE + T07-P04/CH08 |

## CHAPTER 19 · parsing은 문자열을 구조로 읽는 일 — 선택형 코드 리뷰 6질문

T07-P04: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P04에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-R01 | T07-P04: 동시성 | T07-P04: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P04: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P04: T07-P04/CH04 | T07-P04: DURABLE_STATE |
| T07-P04-R02 | T07-P04: 권한 | T07-P04: actor·action·resource가 같은 판단 안에 있는가 | T07-P04: OVERLOAD[rps=611; p99_ms=1056; queue=15] | T07-P04: T07-P04/CH05 | T07-P04: QUEUE_PRESSURE |
| T07-P04-R03 | T07-P04: 자원 | T07-P04: pool·queue·memory·connection 상한이 있는가 | T07-P04: SAMPLING[sample_rate=27%; trace_present=0; metric_present=1] | T07-P04: T07-P04/CH06 | T07-P04: TRACE_METRIC_CROSSCHECK |
| T07-P04-R04 | T07-P04: 상태 변경 | T07-P04: side effect가 어느 줄에서 확정되는가 | T07-P04: DISCONNECT[disconnect_ms=33; commit_state=UNKNOWN; request=004-03] | T07-P04: T07-P04/CH07 | T07-P04: COMMIT_TIMELINE |
| T07-P04-R05 | T07-P04: 동시성 | T07-P04: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P04: RECURRENCE[occurrence=6; interval_s=200; mitigation_applied=1] | T07-P04: T07-P04/CH04 | T07-P04: RECURRENCE_TIMELINE |
| T07-P04-R06 | T07-P04: 권한 | T07-P04: actor·action·resource가 같은 판단 안에 있는가 | T07-P04: LARGE_INPUT[body_kb=1960; limit_kb=768; parsed=0] | T07-P04: T07-P04/CH05 | T07-P04: SIZE_LIMIT |

## CHAPTER 20 · parsing은 문자열을 구조로 읽는 일 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P04에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-I01 | T07-P04: 가설 검증 | T07-P04: 원인 후보 하나만 뒤집어 재현 | T07-P04: OVERLOAD[rps=578; p99_ms=939; queue=21] | T07-P04: T07-P04/CH08 + QUEUE_PRESSURE | T07-P04-incident-076 |
| T07-P04-I02 | T07-P04: 복구 확인 | T07-P04: durable state와 사용자 결과를 모두 확인 | T07-P04: SAMPLING[sample_rate=14%; trace_present=1; metric_present=1] | T07-P04: T07-P04/CH08 + TRACE_METRIC_CROSSCHECK | T07-P04-incident-077 |
| T07-P04-I03 | T07-P04: 재주입 | T07-P04: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P04: DISCONNECT[disconnect_ms=117; commit_state=UNKNOWN; request=004-02] | T07-P04: T07-P04/CH08 + COMMIT_TIMELINE | T07-P04-incident-078 |
| T07-P04-I04 | T07-P04: 회귀 고정 | T07-P04: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P04: RECURRENCE[occurrence=5; interval_s=189; mitigation_applied=1] | T07-P04: T07-P04/CH08 + RECURRENCE_TIMELINE | T07-P04-incident-079 |
| T07-P04-I05 | T07-P04: 영향 범위 | T07-P04: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P04: LARGE_INPUT[body_kb=1872; limit_kb=768; parsed=0] | T07-P04: T07-P04/CH08 + SIZE_LIMIT | T07-P04-incident-080 |
| T07-P04-I06 | T07-P04: 변경 동결 | T07-P04: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P04: DRAIN[ready=0; active=6; drain_deadline_s=10] | T07-P04: T07-P04/CH08 + DRAIN_STATE | T07-P04-incident-081 |

## CHAPTER 21 · parsing은 문자열을 구조로 읽는 일 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P04/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P04/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P04/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P04/CH04~T07-P04/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P04/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P04/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P04/CH18 signal 두 개와 T07-P04/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P04/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P04/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · parsing은 문자열을 구조로 읽는 일 — 통합 casebook 16문제

T07-P04 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P04 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P04-C01 | T07-P04: 경계 A | T07-P04: SAMPLING[sample_rate=81%; trace_present=0; metric_present=1] | T07-P04: load=216, window=78s | T07-P04: review=관측 | T07-P04: 경계 A 위반 여부를 판정 | T07-P04: TRACE_METRIC_CROSSCHECK + T07-P04/CH08 |
| T07-P04-C02 | T07-P04: 경계 B | T07-P04: DISCONNECT[disconnect_ms=104; commit_state=UNKNOWN; request=004-01] | T07-P04: load=239, window=97s | T07-P04: review=상태 변경 | T07-P04: 경계 B 위반 여부를 판정 | T07-P04: COMMIT_TIMELINE + T07-P04/CH08 |
| T07-P04-C03 | T07-P04: 경계 C | T07-P04: RECURRENCE[occurrence=4; interval_s=178; mitigation_applied=1] | T07-P04: load=262, window=26s | T07-P04: review=retry | T07-P04: 경계 C 위반 여부를 판정 | T07-P04: RECURRENCE_TIMELINE + T07-P04/CH08 |
| T07-P04-C04 | T07-P04: 경계 D | T07-P04: LARGE_INPUT[body_kb=1784; limit_kb=768; parsed=0] | T07-P04: load=285, window=45s | T07-P04: review=복구 | T07-P04: 경계 D 위반 여부를 판정 | T07-P04: SIZE_LIMIT + T07-P04/CH08 |
| T07-P04-C05 | T07-P04: 경계 A | T07-P04: DRAIN[ready=0; active=10; drain_deadline_s=9] | T07-P04: load=308, window=64s | T07-P04: review=동시성 | T07-P04: 경계 A 위반 여부를 판정 | T07-P04: DRAIN_STATE + T07-P04/CH08 |
| T07-P04-C06 | T07-P04: 경계 B | T07-P04: REPLAY[key=cmd-004-05; attempts=4; response_seen=0] | T07-P04: load=331, window=83s | T07-P04: review=민감정보 | T07-P04: 경계 B 위반 여부를 판정 | T07-P04: IDEMPOTENCY_RECORD + T07-P04/CH08 |
| T07-P04-C07 | T07-P04: 경계 C | T07-P04: OLD_SCHEMA[client=v3; server=v4; extra_field=0] | T07-P04: load=354, window=12s | T07-P04: review=중복 | T07-P04: 경계 C 위반 여부를 판정 | T07-P04: CLIENT_VERSION + T07-P04/CH08 |
| T07-P04-C08 | T07-P04: 경계 D | T07-P04: UNKNOWN_OUTCOME[timeout_ms=303; provider_state=UNKNOWN; lookup_id=p00407] | T07-P04: load=377, window=31s | T07-P04: review=권한 | T07-P04: 경계 D 위반 여부를 판정 | T07-P04: PROVIDER_RESULT + T07-P04/CH08 |
| T07-P04-C09 | T07-P04: 경계 A | T07-P04: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P04: load=400, window=50s | T07-P04: review=입력 경계 | T07-P04: 경계 A 위반 여부를 판정 | T07-P04: DURABLE_STATE + T07-P04/CH08 |
| T07-P04-C10 | T07-P04: 경계 B | T07-P04: OVERLOAD[rps=242; p99_ms=1119; queue=30] | T07-P04: load=423, window=69s | T07-P04: review=timeout | T07-P04: 경계 B 위반 여부를 판정 | T07-P04: QUEUE_PRESSURE + T07-P04/CH08 |
| T07-P04-C11 | T07-P04: 경계 C | T07-P04: SAMPLING[sample_rate=17%; trace_present=0; metric_present=1] | T07-P04: load=446, window=88s | T07-P04: review=자원 | T07-P04: 경계 C 위반 여부를 판정 | T07-P04: TRACE_METRIC_CROSSCHECK + T07-P04/CH08 |
| T07-P04-C12 | T07-P04: 경계 D | T07-P04: DISCONNECT[disconnect_ms=40; commit_state=UNKNOWN; request=004-11] | T07-P04: load=469, window=17s | T07-P04: review=순서 | T07-P04: 경계 D 위반 여부를 판정 | T07-P04: COMMIT_TIMELINE + T07-P04/CH08 |
| T07-P04-C13 | T07-P04: 경계 A | T07-P04: RECURRENCE[occurrence=4; interval_s=77; mitigation_applied=1] | T07-P04: load=492, window=36s | T07-P04: review=관측 | T07-P04: 경계 A 위반 여부를 판정 | T07-P04: RECURRENCE_TIMELINE + T07-P04/CH08 |
| T07-P04-C14 | T07-P04: 경계 B | T07-P04: LARGE_INPUT[body_kb=976; limit_kb=768; parsed=0] | T07-P04: load=515, window=55s | T07-P04: review=상태 변경 | T07-P04: 경계 B 위반 여부를 판정 | T07-P04: SIZE_LIMIT + T07-P04/CH08 |
| T07-P04-C15 | T07-P04: 경계 C | T07-P04: DRAIN[ready=0; active=9; drain_deadline_s=11] | T07-P04: load=538, window=74s | T07-P04: review=retry | T07-P04: 경계 C 위반 여부를 판정 | T07-P04: DRAIN_STATE + T07-P04/CH08 |
| T07-P04-C16 | T07-P04: 경계 D | T07-P04: REPLAY[key=cmd-004-15; attempts=2; response_seen=0] | T07-P04: load=561, window=93s | T07-P04: review=복구 | T07-P04: 경계 D 위반 여부를 판정 | T07-P04: IDEMPOTENCY_RECORD + T07-P04/CH08 |

채점은 결론보다 근거를 본다. T07-P04/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · parsing은 문자열을 구조로 읽는 일 — evidence 판독 문제 14개

T07-P04 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P04 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P04-V01 | T07-P04: latency=184ms; queue=28; retry=4 | T07-P04: client disconnect | T07-P04: 축=client disconnect; 원인 확정은 보류 | T07-P04: COMMIT_TIMELINE + T07-P04/CH08 | T07-P04: 피할 오판=복구 과잉 |
| T07-P04-V02 | T07-P04: latency=251ms; queue=39; retry=0 | T07-P04: 재발 | T07-P04: 축=재발; 원인 확정은 보류 | T07-P04: RECURRENCE_TIMELINE + T07-P04/CH08 | T07-P04: 피할 오판=잘못된 전제 |
| T07-P04-V03 | T07-P04: latency=318ms; queue=50; retry=3 | T07-P04: 대형 입력 | T07-P04: 축=대형 입력; 원인 확정은 보류 | T07-P04: SIZE_LIMIT + T07-P04/CH08 | T07-P04: 피할 오판=경계 누락 |
| T07-P04-V04 | T07-P04: latency=385ms; queue=61; retry=6 | T07-P04: drain | T07-P04: 축=drain; 원인 확정은 보류 | T07-P04: DRAIN_STATE + T07-P04/CH08 | T07-P04: 피할 오판=증거 혼동 |
| T07-P04-V05 | T07-P04: latency=452ms; queue=72; retry=2 | T07-P04: 재전송 | T07-P04: 축=재전송; 원인 확정은 보류 | T07-P04: IDEMPOTENCY_RECORD + T07-P04/CH08 | T07-P04: 피할 오판=동시성 무시 |
| T07-P04-V06 | T07-P04: latency=519ms; queue=3; retry=5 | T07-P04: 구버전 client | T07-P04: 축=구버전 client; 원인 확정은 보류 | T07-P04: CLIENT_VERSION + T07-P04/CH08 | T07-P04: 피할 오판=상태 수명 혼동 |
| T07-P04-V07 | T07-P04: latency=586ms; queue=14; retry=1 | T07-P04: unknown outcome | T07-P04: 축=unknown outcome; 원인 확정은 보류 | T07-P04: PROVIDER_RESULT + T07-P04/CH08 | T07-P04: 피할 오판=운영 한계 누락 |
| T07-P04-V08 | T07-P04: latency=653ms; queue=25; retry=4 | T07-P04: 재시작 | T07-P04: 축=재시작; 원인 확정은 보류 | T07-P04: DURABLE_STATE + T07-P04/CH08 | T07-P04: 피할 오판=오류 합치기 |
| T07-P04-V09 | T07-P04: latency=720ms; queue=36; retry=0 | T07-P04: 과부하 | T07-P04: 축=과부하; 원인 확정은 보류 | T07-P04: QUEUE_PRESSURE + T07-P04/CH08 | T07-P04: 피할 오판=복구 과잉 |
| T07-P04-V10 | T07-P04: latency=787ms; queue=47; retry=3 | T07-P04: sampling | T07-P04: 축=sampling; 원인 확정은 보류 | T07-P04: TRACE_METRIC_CROSSCHECK + T07-P04/CH08 | T07-P04: 피할 오판=잘못된 전제 |
| T07-P04-V11 | T07-P04: latency=854ms; queue=58; retry=6 | T07-P04: client disconnect | T07-P04: 축=client disconnect; 원인 확정은 보류 | T07-P04: COMMIT_TIMELINE + T07-P04/CH08 | T07-P04: 피할 오판=경계 누락 |
| T07-P04-V12 | T07-P04: latency=921ms; queue=69; retry=2 | T07-P04: 재발 | T07-P04: 축=재발; 원인 확정은 보류 | T07-P04: RECURRENCE_TIMELINE + T07-P04/CH08 | T07-P04: 피할 오판=증거 혼동 |
| T07-P04-V13 | T07-P04: latency=988ms; queue=0; retry=5 | T07-P04: 대형 입력 | T07-P04: 축=대형 입력; 원인 확정은 보류 | T07-P04: SIZE_LIMIT + T07-P04/CH08 | T07-P04: 피할 오판=동시성 무시 |
| T07-P04-V14 | T07-P04: latency=1055ms; queue=11; retry=1 | T07-P04: drain | T07-P04: 축=drain; 원인 확정은 보류 | T07-P04: DRAIN_STATE + T07-P04/CH08 | T07-P04: 피할 오판=상태 수명 혼동 |

## CHAPTER 24 · parsing은 문자열을 구조로 읽는 일 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P04에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P04-D01 | T07-P04: fallback 추가 | T07-P04: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P04: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P04: LARGE_INPUT[body_kb=1520; limit_kb=768; parsed=0] | T07-P04: SIZE_LIMIT + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D02 | T07-P04: 외부 호출 이동 | T07-P04: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P04: lock duration과 unknown outcome 경계가 달라지는지 | T07-P04: DRAIN[ready=0; active=17; drain_deadline_s=6] | T07-P04: DRAIN_STATE + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D03 | T07-P04: cache 추가 | T07-P04: 현재 결과 앞에 cache layer를 추가한다 | T07-P04: stale·key·invalidation 책임이 새로 생기는지 | T07-P04: REPLAY[key=cmd-004-02; attempts=4; response_seen=0] | T07-P04: IDEMPOTENCY_RECORD + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D04 | T07-P04: pool 확대 | T07-P04: connection/worker pool 상한을 늘린다 | T07-P04: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P04: OLD_SCHEMA[client=v4; server=v5; extra_field=1] | T07-P04: CLIENT_VERSION + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D05 | T07-P04: schema 변경 | T07-P04: 필드 이름·형식·required 조건을 바꾼다 | T07-P04: 구버전 client와 저장 데이터 migration을 함께 보는지 | T07-P04: UNKNOWN_OUTCOME[timeout_ms=270; provider_state=UNKNOWN; lookup_id=p00404] | T07-P04: PROVIDER_RESULT + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D06 | T07-P04: 결과 합치기 | T07-P04: 여러 오류를 하나의 status/error code로 합친다 | T07-P04: client 행동과 retry 가능성을 잃지 않는지 | T07-P04: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P04: DURABLE_STATE + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D07 | T07-P04: retry 추가 | T07-P04: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P04: unknown outcome과 duplicate side effect를 구분하는지 | T07-P04: OVERLOAD[rps=776; p99_ms=768; queue=28] | T07-P04: QUEUE_PRESSURE + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P04-D08 | T07-P04: 로그 확대 | T07-P04: debug를 위해 payload와 context 기록을 늘린다 | T07-P04: secret·PII·cardinality 비용을 통제하는지 | T07-P04: SAMPLING[sample_rate=75%; trace_present=1; metric_present=1] | T07-P04: TRACE_METRIC_CROSSCHECK + T07-P04/CH08 | T07-P04: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P04: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · parsing은 문자열을 구조로 읽는 일 — 최종 contract와 evidence spine

**최종 계약:** parsing은 네트워크에서 온 bytes나 text를 프로그램이 다룰 구조로 바꾸는 단계다.

**정상 메커니즘:** Content-Type과 문자 인코딩을 기준으로 body를 해석하고 JSON이라면 문법을 검사해 값 구조를 만든다.

**대표 실패:** JSON 문법이 맞다는 이유만으로 업무적으로 올바른 값이라고 오해하거나 body 크기 제한 없이 전부 읽는다.

**검증 evidence:** Content-Type, body byte 수, parse 성공/실패 위치, parser 오류 종류를 확인한다.

**직접 행동:** JSON처럼 생긴 입력을 단순 객체로 가정하지 않고 구조 검사 전후를 비교한다.

**다음 연결:** `middleware는 공통 단계를 순서대로 연결한다`.

`parsing은 문자열을 구조로 읽는 일`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | parsing은 문자열을 구조로 읽는 일의 개념·실패·운영 판단 교차 확인 |

`parsing은 문자열을 구조로 읽는 일` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
