# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 07 · controller·service·repository로 책임을 나누는 이유

## CHAPTER 01 · controller·service·repository로 책임을 나누는 이유 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 한 handler에 모든 코드를 넣지 않고 입력 변환, 업무 규칙, 저장 접근을 역할별로 나누면 변경 이유가 분리된다.

**아주 쉬운 사건.** handler가 DB와 mailer까지 직접 부른다. 이 사건에서는 먼저 **transport와 use-case 책임을 나눈다**.

**왜 필요한가.** 정상 동작은 controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다. 반대로 계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다.

**암기:** `controller·service·repository로 책임을 나누는 이유`의 역할 한 줄.

**직접 이해:** `controller·service·repository로 책임을 나누는 이유`의 입력·상태·결과 경계.

**AI 위임 가능:** `controller·service·repository로 책임을 나누는 이유` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `handler가 DB와 mailer까지 직접 부른다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 함수 입력·출력 타입, 외부 의존 호출 위치, 변경 시 같이 수정되는 파일을 본다.

## CHAPTER 02 · controller·service·repository로 책임을 나누는 이유 — 아주 쉬운 예를 한 단계씩 해석

T07-P07: `handler가 DB와 mailer까지 직접 부른다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | handler가 DB와 mailer까지 직접 부른다 | T07-P07 외부 입력 | T07-P07: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | transport와 use-case 책임을 나눈다 | T07-P07 판단 기준 | T07-P07/CH08 관측표와 대조 |
| 정상 경로 | T07-P07/CH03 M1→M5 | controller·service·repository로 책임을 나누는 이유: 완료 시점을 단계별로 분리 | T07-P07: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P07/CH06 계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다. | T07-P07: 깨진 계약 하나를 특정 | controller·service·repository로 책임을 나누는 이유: 증상과 원인을 분리 |
| 재검증 | T07-P07/CH10 직접 실행 | T07-P07: 예상값 T07-P007 3/4 기록 | controller·service·repository로 책임을 나누는 이유: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P07/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P07/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · controller·service·repository로 책임을 나누는 이유 — 내부 메커니즘과 상태 전이

controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | handler가 DB와 mailer까지 직접 부른다 | source/actor/size를 보존 |
| M2 | 경계 판단 | transport와 use-case 책임을 나눈다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | 함수 입력·출력 타입, 외부 의존 호출 위치, 변경 시 같이 수정되는 파일을 본다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | HTTP 정보가 없는 순수 service 함수를 만들고 controller가 변환만 하도록 분리한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`controller·service·repository로 책임을 나누는 이유` 흐름을 framework 이름 없이 설명한다.

막히면 `handler가 DB와 mailer까지 직접 부른다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · controller·service·repository로 책임을 나누는 이유 — 실전 경계 A

**경계 A.** controller는 HTTP method·status·header·body와 domain input 사이 변환을 맡고, 업무 규칙은 service/use-case에 남겨 HTTP 밖에서도 같은 결정을 실행할 수 있게 한다.

T07-P07/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P07에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P07/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P07/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P07): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P07/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P07-A1-754 | T07-P07 조건 | controller는 HTTP method·status·header·body와 domain input 사이 변환을 맡고, 업무 규칙은 service/use-case에 남겨 HTTP 밖에서도 같은 결정을 실행할 수 있게 한다. |
| T07-P07-A2-755 | T07-P07 변화점 | T07-P07/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P07-A3-756 | T07-P07 반례 | T07-P07/CH06 대표 실패와 A 위반을 구별 |
| T07-P07-A4-757 | T07-P07 근거 | T07-P07/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P07-A5-758 | T07-P07 재실험 | handler가 DB와 mailer까지 직접 부른다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · controller·service·repository로 책임을 나누는 이유 — 실전 경계 B

**경계 B.** service가 transaction을 시작하고 끝낼 책임이 있다면 repository가 임의로 commit하지 않게 경계를 고정해야 여러 repository 변경을 하나의 업무 단위로 묶을 수 있다.

T07-P07/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P07에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P07/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P07/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P07): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P07/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P07-B1-785 | T07-P07 조건 | service가 transaction을 시작하고 끝낼 책임이 있다면 repository가 임의로 commit하지 않게 경계를 고정해야 여러 repository 변경을 하나의 업무 단위로 묶을 수 있다. |
| T07-P07-B2-786 | T07-P07 독립성 | T07-P07/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P07-B3-787 | T07-P07 상태 | T07-P07/CH03 before·after 위치를 다시 지정 |
| T07-P07-B4-788 | T07-P07 반증 | T07-P07/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P07-B5-789 | T07-P07 적용 | controller·service·repository로 책임을 나누는 이유의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · controller·service·repository로 책임을 나누는 이유 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다.**

아래 여섯 사례는 T07-P07의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P07-F01 | T07-P07: 대표 실패 | T07-P07: T07-P07/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P07: 현상만 보고 원인을 확정 | T07-P07/CH08 evidence map에서 상태를 대조 |
| T07-P07-F02 | T07-P07: 경계 A 누락 | T07-P07: T07-P07/CH04 경계 A 위반 입력 | T07-P07: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P07/CH08 evidence map에서 상태를 대조 |
| T07-P07-F03 | T07-P07: 경계 B 누락 | T07-P07: T07-P07/CH05 경계 B 위반 입력 | T07-P07: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P07/CH08 evidence map에서 상태를 대조 |
| T07-P07-F04 | T07-P07: 복구 경계 C 누락 | T07-P07: T07-P07/CH07 경계 C 복구 조건 | T07-P07: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P07/CH08 evidence map에서 상태를 대조 |
| T07-P07-F05 | T07-P07: 운영 경계 D 누락 | T07-P07: T07-P07/CH09 경계 D 운영 조건 | T07-P07: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P07/CH08 evidence map에서 상태를 대조 |
| T07-P07-F06 | T07-P07: 증거 없는 결론 | T07-P07: T07-P07/CH02 첫 판단만 존재 | T07-P07: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P07/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P07/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · controller·service·repository로 책임을 나누는 이유 — 복구 가능한 상태와 수명

**경계 C.** repository는 저장 기술을 숨기는 경계이지 모든 business rule을 밀어 넣는 장소가 아니다. ‘재고가 0 아래로 갈 수 없다’ 같은 규칙은 storage 종류가 바뀌어도 남아야 한다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P07에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P07/CH08 evidence map을 본다. 복구 후에는 T07-P07/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P07에서 이미 확정된 side effect는 T07-P07/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P07-R1-847 | T07-P07 중단 직전 | T07-P07/CH03에서 이미 확정된 상태만 표시 |
| T07-P07-R2-848 | T07-P07 재시작 직후 | repository는 저장 기술을 숨기는 경계이지 모든 business rule을 밀어 넣는 장소가 아니다. ‘재고가 0 아래로 갈 수 없다’ 같은 규칙은 storage 종류가 바뀌어도 남아야 한다. |
| T07-P07-R3-849 | T07-P07 재검증 | T07-P07/CH08 근거로 중복·누락 여부 확인 |
| T07-P07-R4-850 | T07-P07 재실행 | T07-P07/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · controller·service·repository로 책임을 나누는 이유 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | handler가 DB와 mailer까지 직접 부른다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | transport와 use-case 책임을 나눈다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | 함수 입력·출력 타입, 외부 의존 호출 위치, 변경 시 같이 수정되는 파일을 본다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`controller·service·repository로 책임을 나누는 이유` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · controller·service·repository로 책임을 나누는 이유 — 운영 한계와 종료 조건

**경계 D.** req·res 객체가 repository까지 내려가거나 DB row가 그대로 response까지 올라오면 계층 이름만 나뉜 것이므로 각 경계에서 최소 DTO/domain type로 변환한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P07/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P07/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P07 과제: 경계 D와 T07-P07/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P07-O1-909 | synthetic-load=49 | T07-P07/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P07-O2-910 | synthetic-budget=209ms | T07-P07 timeout과 unknown outcome을 분리 |
| T07-P07-O3-911 | T07-P07 종료 | req·res 객체가 repository까지 내려가거나 DB row가 그대로 response까지 올라오면 계층 이름만 나뉜 것이므로 각 경계에서 최소 DTO/domain type로 변환한다. |
| T07-P07-O4-912 | T07-P07 완화 | T07-P07/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · controller·service·repository로 책임을 나누는 이유 — 직접 실행하는 작은 모델

`controller·service·repository로 책임을 나누는 이유` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P007 3/4`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P007";
const checks = [true,false,true,true];
const passed = checks.filter(Boolean).length;
console.log(marker, `${passed}/${checks.length}`);
```

기준 출력: `T07-P007 3/4`.

`controller·service·repository로 책임을 나누는 이유`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P07-L1-941 | constmarker="T07-P007"; | T07-P07 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P07-L2-942 | constchecks=[true,false,true,true]; | T07-P07 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P07-L3-943 | constpassed=checks.filter(Boolean).length; | T07-P07 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P07-L4-944 | console.log(marker,`${passed}/${checks.length}`); | T07-P07 출력 관측점; 예상 `T07-P007 3/4`와 비교 |
| T07-P07-LX-1030 | T07-P07 실행 기록 | T07-P07 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · controller·service·repository로 책임을 나누는 이유 — 한 부분만 수정하고 다시 예측

수정 과제: **false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다**.

수정 전은 `T07-P007 3/4`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P07/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P07-D1-971 | 기준 `T07-P007 3/4` | T07-P07 수정 전 실행을 먼저 재현 |
| T07-P07-D2-972 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | T07-P07 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P07-D3-973 | T07-P07 새 예측 | T07-P07 실행 전에 출력·상태를 먼저 기록 |
| T07-P07-D4-974 | T07-P07 재실행 | T07-P07/CH10 실제값과 새 예측을 대조 |
| T07-P07-D5-975 | T07-P07 반례 | T07-P07/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P07-D6-976 | T07-P07 근거 | T07-P07/CH08 상태가 설명과 일치해야 완료 |
| T07-P07-D7-977 | T07-P07 이유 | T07-P07 변경 이유를 controller·service·repository로 책임을 나누는 이유 계약과 연결해 설명 |

## CHAPTER 12 · controller·service·repository로 책임을 나누는 이유 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: controller·service·repository로 책임을 나누는 이유 | 한 handler에 모든 코드를 넣지 않고 입력 변환, 업무 규칙, 저장 접근을 역할별로 나누면 변경 이유가 분리된다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P07/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | 함수 입력·출력 타입, 외부 의존 호출 위치, 변경 시 같이 수정되는 파일을 본다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P07/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P07/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P07/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P07/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `controller·service·repository로 책임을 나누는 이유` 실행 코드 수정 | false인 check 하나를 고치고 passed가 1 늘어나는지 확인한다 | `controller·service·repository로 책임을 나누는 이유` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P07/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P07/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · controller·service·repository로 책임을 나누는 이유 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 한 handler에 모든 코드를 넣지 않고 입력 변환, 업무 규칙, 저장 접근을 역할별로 나누면 변경 이유가 분리된다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P07/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P07/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P07/CH10 실행용 boilerplate·test 후보 | T07-P07: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P007 3/4` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P07/CH02의 판단 기준과 T07-P07/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P07-AI1-36 | T07-P07 사람 결정 | T07-P07 업무 의미·허용 위험·완료 기준 소유 |
| T07-P07-AI2-37 | T07-P07 AI 초안 | T07-P07/CH10 boilerplate·test 후보까지만 위임 |
| T07-P07-AI3-38 | T07-P07 검증 | T07-P07/CH06 반례와 T07-P07/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · controller·service·repository로 책임을 나누는 이유 — 경계 조합 실험 8개

T07-P07: T07-P07/CH04~T07-P07/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P07의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-K01 | T07-P07: 경계 A | T07-P07: 경계 B | T07-P07: SAMPLING[sample_rate=48%; trace_present=1; metric_present=1] / sample=95 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + TRACE_METRIC_CROSSCHECK |
| T07-P07-K02 | T07-P07: 경계 A | T07-P07: 경계 C | T07-P07: DISCONNECT[disconnect_ms=71; commit_state=UNKNOWN; request=007-02] / sample=13 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + COMMIT_TIMELINE |
| T07-P07-K03 | T07-P07: 경계 A | T07-P07: 경계 D | T07-P07: RECURRENCE[occurrence=5; interval_s=65; mitigation_applied=1] / sample=20 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + RECURRENCE_TIMELINE |
| T07-P07-K04 | T07-P07: 경계 B | T07-P07: 경계 C | T07-P07: LARGE_INPUT[body_kb=880; limit_kb=512; parsed=0] / sample=27 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + SIZE_LIMIT |
| T07-P07-K05 | T07-P07: 경계 B | T07-P07: 경계 D | T07-P07: DRAIN[ready=0; active=6; drain_deadline_s=10] / sample=34 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + DRAIN_STATE |
| T07-P07-K06 | T07-P07: 경계 C | T07-P07: 경계 D | T07-P07: REPLAY[key=cmd-007-06; attempts=2; response_seen=0] / sample=41 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + IDEMPOTENCY_RECORD |
| T07-P07-K07 | T07-P07: 경계 A | T07-P07: 경계 B+C | T07-P07: OLD_SCHEMA[client=v3; server=v4; extra_field=1] / sample=48 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + CLIENT_VERSION |
| T07-P07-K08 | T07-P07: 경계 B | T07-P07: 경계 C+D | T07-P07: UNKNOWN_OUTCOME[timeout_ms=190; provider_state=UNKNOWN; lookup_id=p00708] / sample=55 | T07-P07: 먼저 깨지는 경계를 판정 | T07-P07: T07-P07/CH08 + PROVIDER_RESULT |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P07/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · controller·service·repository로 책임을 나누는 이유 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P07와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-B06 | T07-P07: validation은 외부 입력을 내부 계약으로 바꾸는 문 | T07-P07: age="20", admin=true가 입력으로 온다 | T07-P07: coercion과 허용 field를 따로 결정한다 | T07-P07: T07-P07/CH08 증거와 형제 LESSON 증거를 분리 | T07-P07: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P07-B08 | T07-P07: dependency injection은 외부 의존성을 밖에서 넣는다 | T07-P07: 현재 시각을 함수 안에서 직접 읽는다 | T07-P07: clock dependency를 외부에서 넣는다 | T07-P07: T07-P07/CH08 증거와 형제 LESSON 증거를 분리 | T07-P07: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P07-B05 | T07-P07: middleware는 공통 단계를 순서대로 연결한다 | T07-P07: auth middleware가 parser보다 먼저/뒤에 놓인다 | T07-P07: middleware 순서를 화살표로 그린다 | T07-P07: T07-P07/CH08 증거와 형제 LESSON 증거를 분리 | T07-P07: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P07-B09 | T07-P07: 오류를 종류별로 나누고 응답 계약으로 바꾸기 | T07-P07: 없는 resource와 권한 없는 resource가 섞인다 | T07-P07: public error와 internal cause를 분리한다 | T07-P07: T07-P07/CH08 증거와 형제 LESSON 증거를 분리 | T07-P07: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P07-B04 | T07-P07: parsing은 문자열을 구조로 읽는 일 | T07-P07: Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | T07-P07: parsing 실패와 validation 실패를 구분한다 | T07-P07: T07-P07/CH08 증거와 형제 LESSON 증거를 분리 | T07-P07: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · controller·service·repository로 책임을 나누는 이유 — 선택형 실패 주입 6개

T07-P07: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P07 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-X01 | T07-P07: 재발 | T07-P07: 같은 오류가 잠시 뒤 다시 발생; sample=234 | T07-P07: 완화와 원인 제거 | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: 재발 timeline·변경점·resource state |
| T07-P07-X02 | T07-P07: 대형 입력 | T07-P07: 입력 크기가 정상의 100배; sample=251 | T07-P07: 의미 검증과 resource limit | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: body/batch size·parse time·memory·reject status |
| T07-P07-X03 | T07-P07: drain | T07-P07: 배포 중 기존 요청이 처리 중; sample=268 | T07-P07: 새 traffic 차단과 in-flight 처리 | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: readiness·active requests·deadline·final state |
| T07-P07-X04 | T07-P07: 재전송 | T07-P07: 응답 유실 뒤 같은 command가 다시 도착함; sample=285 | T07-P07: 중복 side effect 여부 | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P07-X05 | T07-P07: 구버전 client | T07-P07: 한 단계 이전 schema가 요청됨; sample=302 | T07-P07: 호환 입력과 breaking change | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: schema version·실제 client 분포·contract test |
| T07-P07-X06 | T07-P07: unknown outcome | T07-P07: dependency timeout 후 성공 여부 불명; sample=319 | T07-P07: 실패와 미확정 결과 | T07-P07: T07-P07/CH02 판단과 별도 기록 | T07-P07: provider id·조회 결과·retry history |

## CHAPTER 17 · controller·service·repository로 책임을 나누는 이유 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P07에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-E01 | T07-P07: 대표 실패를 원인으로 착각 | T07-P07: T07-P07/CH06 실패 case를 다른 입력으로 재현 | T07-P07: 현상과 원인을 같은 것으로 봄 | T07-P07: T07-P07/CH06 대표 실패와 T07-P07/CH08 증거를 다시 대조 | T07-P07: T07-P07/CH08 |
| T07-P07-E02 | T07-P07: 경계 A 생략 | T07-P07: T07-P07/CH04의 조건 하나를 반대로 설정 | T07-P07: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P07: T07-P07/CH04를 새 입력에 적용 | T07-P07: T07-P07/CH08 |
| T07-P07-E03 | T07-P07: 경계 B 생략 | T07-P07: T07-P07/CH05의 조건 하나를 반대로 설정 | T07-P07: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P07: T07-P07/CH05를 새 입력에 적용 | T07-P07: T07-P07/CH08 |
| T07-P07-E04 | T07-P07: 복구 상태 혼동 | T07-P07: T07-P07/CH07에서 처리 중단을 주입 | T07-P07: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P07: T07-P07/CH07에서 수명 경계를 다시 표시 | T07-P07: T07-P07/CH08 |
| T07-P07-E05 | T07-P07: 운영 한계 누락 | T07-P07: T07-P07/CH09에서 부하 또는 drain 조건을 변경 | T07-P07: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P07: T07-P07/CH09의 종료 조건을 다시 작성 | T07-P07: T07-P07/CH08 |
| T07-P07-E06 | T07-P07: 증거 없는 성공 판정 | T07-P07: T07-P07/CH08에서 증거 하나를 숨김 | T07-P07: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P07: T07-P07/CH08에서 독립 증거 둘을 선택 | T07-P07: T07-P07/CH08 |

## CHAPTER 18 · controller·service·repository로 책임을 나누는 이유 — synthetic 관측값 판독 6개

T07-P07: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P07의 숫자 하나만으로 원인을 단정하지 않고 T07-P07/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-O01 | T07-P07/requests_received | 404 | T07-P07: 요청 수신 수 | T07-P07: 축=구버전 client; 원인 확정 금지 | T07-P07: CLIENT_VERSION + T07-P07/CH08 |
| T07-P07-O02 | T07-P07/handler_started | 421 | T07-P07: handler 진입 수 | T07-P07: 축=unknown outcome; 원인 확정 금지 | T07-P07: PROVIDER_RESULT + T07-P07/CH08 |
| T07-P07-O03 | T07-P07/responses_completed | 438 | T07-P07: 응답 완료 수 | T07-P07: 축=재시작; 원인 확정 금지 | T07-P07: DURABLE_STATE + T07-P07/CH08 |
| T07-P07-O04 | T07-P07/latency_ms | 455 | T07-P07: 요청 처리 지연 | T07-P07: 축=과부하; 원인 확정 금지 | T07-P07: QUEUE_PRESSURE + T07-P07/CH08 |
| T07-P07-O05 | T07-P07/body_kb | 472 | T07-P07: 입력 크기 | T07-P07: 축=sampling; 원인 확정 금지 | T07-P07: TRACE_METRIC_CROSSCHECK + T07-P07/CH08 |
| T07-P07-O06 | T07-P07/active_connections | 489 | T07-P07: 활성 연결 수 | T07-P07: 축=client disconnect; 원인 확정 금지 | T07-P07: COMMIT_TIMELINE + T07-P07/CH08 |

## CHAPTER 19 · controller·service·repository로 책임을 나누는 이유 — 선택형 코드 리뷰 6질문

T07-P07: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P07에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-R01 | T07-P07: 동시성 | T07-P07: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P07: UNKNOWN_OUTCOME[timeout_ms=313; provider_state=UNKNOWN; lookup_id=p00700] | T07-P07: T07-P07/CH04 | T07-P07: PROVIDER_RESULT |
| T07-P07-R02 | T07-P07: 권한 | T07-P07: actor·action·resource가 같은 판단 안에 있는가 | T07-P07: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P07: T07-P07/CH05 | T07-P07: DURABLE_STATE |
| T07-P07-R03 | T07-P07: 자원 | T07-P07: pool·queue·memory·connection 상한이 있는가 | T07-P07: OVERLOAD[rps=272; p99_ms=759; queue=23] | T07-P07: T07-P07/CH06 | T07-P07: QUEUE_PRESSURE |
| T07-P07-R04 | T07-P07: 상태 변경 | T07-P07: side effect가 어느 줄에서 확정되는가 | T07-P07: SAMPLING[sample_rate=74%; trace_present=1; metric_present=1] | T07-P07: T07-P07/CH07 | T07-P07: TRACE_METRIC_CROSSCHECK |
| T07-P07-R05 | T07-P07: 동시성 | T07-P07: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P07: DISCONNECT[disconnect_ms=97; commit_state=UNKNOWN; request=007-04] | T07-P07: T07-P07/CH04 | T07-P07: COMMIT_TIMELINE |
| T07-P07-R06 | T07-P07: 권한 | T07-P07: actor·action·resource가 같은 판단 안에 있는가 | T07-P07: RECURRENCE[occurrence=2; interval_s=87; mitigation_applied=1] | T07-P07: T07-P07/CH05 | T07-P07: RECURRENCE_TIMELINE |

## CHAPTER 20 · controller·service·repository로 책임을 나누는 이유 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P07에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-I01 | T07-P07: 회귀 고정 | T07-P07: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P07: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P07: T07-P07/CH08 + DURABLE_STATE | T07-P07-incident-133 |
| T07-P07-I02 | T07-P07: 영향 범위 | T07-P07: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P07: OVERLOAD[rps=239; p99_ms=642; queue=29] | T07-P07: T07-P07/CH08 + QUEUE_PRESSURE | T07-P07-incident-134 |
| T07-P07-I03 | T07-P07: 변경 동결 | T07-P07: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P07: SAMPLING[sample_rate=61%; trace_present=0; metric_present=1] | T07-P07: T07-P07/CH08 + TRACE_METRIC_CROSSCHECK | T07-P07-incident-135 |
| T07-P07-I04 | T07-P07: correlation | T07-P07: 한 request/job/resource id를 시간축에 고정 | T07-P07: DISCONNECT[disconnect_ms=84; commit_state=UNKNOWN; request=007-03] | T07-P07: T07-P07/CH08 + COMMIT_TIMELINE | T07-P07-incident-136 |
| T07-P07-I05 | T07-P07: 마지막 정상 | T07-P07: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P07: RECURRENCE[occurrence=6; interval_s=76; mitigation_applied=1] | T07-P07: T07-P07/CH08 + RECURRENCE_TIMELINE | T07-P07-incident-137 |
| T07-P07-I06 | T07-P07: 가설 검증 | T07-P07: 원인 후보 하나만 뒤집어 재현 | T07-P07: LARGE_INPUT[body_kb=968; limit_kb=512; parsed=0] | T07-P07: T07-P07/CH08 + SIZE_LIMIT | T07-P07-incident-138 |

## CHAPTER 21 · controller·service·repository로 책임을 나누는 이유 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P07/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P07/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P07/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P07/CH04~T07-P07/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P07/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P07/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P07/CH18 signal 두 개와 T07-P07/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P07/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P07/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · controller·service·repository로 책임을 나누는 이유 — 통합 casebook 16문제

T07-P07 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P07 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P07-C01 | T07-P07: 경계 A | T07-P07: OVERLOAD[rps=839; p99_ms=525; queue=15] | T07-P07: load=303, window=39s | T07-P07: review=복구 | T07-P07: 경계 A 위반 여부를 판정 | T07-P07: QUEUE_PRESSURE + T07-P07/CH08 |
| T07-P07-C02 | T07-P07: 경계 B | T07-P07: SAMPLING[sample_rate=48%; trace_present=1; metric_present=1] | T07-P07: load=326, window=58s | T07-P07: review=동시성 | T07-P07: 경계 B 위반 여부를 판정 | T07-P07: TRACE_METRIC_CROSSCHECK + T07-P07/CH08 |
| T07-P07-C03 | T07-P07: 경계 C | T07-P07: DISCONNECT[disconnect_ms=71; commit_state=UNKNOWN; request=007-02] | T07-P07: load=349, window=77s | T07-P07: review=민감정보 | T07-P07: 경계 C 위반 여부를 판정 | T07-P07: COMMIT_TIMELINE + T07-P07/CH08 |
| T07-P07-C04 | T07-P07: 경계 D | T07-P07: RECURRENCE[occurrence=5; interval_s=65; mitigation_applied=1] | T07-P07: load=372, window=96s | T07-P07: review=중복 | T07-P07: 경계 D 위반 여부를 판정 | T07-P07: RECURRENCE_TIMELINE + T07-P07/CH08 |
| T07-P07-C05 | T07-P07: 경계 A | T07-P07: LARGE_INPUT[body_kb=880; limit_kb=512; parsed=0] | T07-P07: load=395, window=25s | T07-P07: review=권한 | T07-P07: 경계 A 위반 여부를 판정 | T07-P07: SIZE_LIMIT + T07-P07/CH08 |
| T07-P07-C06 | T07-P07: 경계 B | T07-P07: DRAIN[ready=0; active=6; drain_deadline_s=10] | T07-P07: load=418, window=44s | T07-P07: review=입력 경계 | T07-P07: 경계 B 위반 여부를 판정 | T07-P07: DRAIN_STATE + T07-P07/CH08 |
| T07-P07-C07 | T07-P07: 경계 C | T07-P07: REPLAY[key=cmd-007-06; attempts=2; response_seen=0] | T07-P07: load=441, window=63s | T07-P07: review=timeout | T07-P07: 경계 C 위반 여부를 판정 | T07-P07: IDEMPOTENCY_RECORD + T07-P07/CH08 |
| T07-P07-C08 | T07-P07: 경계 D | T07-P07: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P07: load=464, window=82s | T07-P07: review=자원 | T07-P07: 경계 D 위반 여부를 판정 | T07-P07: CLIENT_VERSION + T07-P07/CH08 |
| T07-P07-C09 | T07-P07: 경계 A | T07-P07: UNKNOWN_OUTCOME[timeout_ms=190; provider_state=UNKNOWN; lookup_id=p00708] | T07-P07: load=487, window=11s | T07-P07: review=순서 | T07-P07: 경계 A 위반 여부를 판정 | T07-P07: PROVIDER_RESULT + T07-P07/CH08 |
| T07-P07-C10 | T07-P07: 경계 B | T07-P07: RESTART[crash_after_step=2; memory_lost=1; durable_check=pending] | T07-P07: load=510, window=30s | T07-P07: review=관측 | T07-P07: 경계 B 위반 여부를 판정 | T07-P07: DURABLE_STATE + T07-P07/CH08 |
| T07-P07-C11 | T07-P07: 경계 C | T07-P07: OVERLOAD[rps=536; p99_ms=822; queue=38] | T07-P07: load=533, window=49s | T07-P07: review=상태 변경 | T07-P07: 경계 C 위반 여부를 판정 | T07-P07: QUEUE_PRESSURE + T07-P07/CH08 |
| T07-P07-C12 | T07-P07: 경계 D | T07-P07: SAMPLING[sample_rate=81%; trace_present=1; metric_present=1] | T07-P07: load=556, window=68s | T07-P07: review=retry | T07-P07: 경계 D 위반 여부를 판정 | T07-P07: TRACE_METRIC_CROSSCHECK + T07-P07/CH08 |
| T07-P07-C13 | T07-P07: 경계 A | T07-P07: DISCONNECT[disconnect_ms=104; commit_state=UNKNOWN; request=007-12] | T07-P07: load=579, window=87s | T07-P07: review=복구 | T07-P07: 경계 A 위반 여부를 판정 | T07-P07: COMMIT_TIMELINE + T07-P07/CH08 |
| T07-P07-C14 | T07-P07: 경계 B | T07-P07: RECURRENCE[occurrence=5; interval_s=175; mitigation_applied=1] | T07-P07: load=602, window=16s | T07-P07: review=동시성 | T07-P07: 경계 B 위반 여부를 판정 | T07-P07: RECURRENCE_TIMELINE + T07-P07/CH08 |
| T07-P07-C15 | T07-P07: 경계 C | T07-P07: LARGE_INPUT[body_kb=1760; limit_kb=512; parsed=0] | T07-P07: load=625, window=35s | T07-P07: review=민감정보 | T07-P07: 경계 C 위반 여부를 판정 | T07-P07: SIZE_LIMIT + T07-P07/CH08 |
| T07-P07-C16 | T07-P07: 경계 D | T07-P07: DRAIN[ready=0; active=10; drain_deadline_s=12] | T07-P07: load=648, window=54s | T07-P07: review=중복 | T07-P07: 경계 D 위반 여부를 판정 | T07-P07: DRAIN_STATE + T07-P07/CH08 |

채점은 결론보다 근거를 본다. T07-P07/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · controller·service·repository로 책임을 나누는 이유 — evidence 판독 문제 14개

T07-P07 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P07 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P07-V01 | T07-P07: latency=307ms; queue=49; retry=0 | T07-P07: sampling | T07-P07: 축=sampling; 원인 확정은 보류 | T07-P07: TRACE_METRIC_CROSSCHECK + T07-P07/CH08 | T07-P07: 피할 오판=운영 한계 누락 |
| T07-P07-V02 | T07-P07: latency=374ms; queue=60; retry=3 | T07-P07: client disconnect | T07-P07: 축=client disconnect; 원인 확정은 보류 | T07-P07: COMMIT_TIMELINE + T07-P07/CH08 | T07-P07: 피할 오판=오류 합치기 |
| T07-P07-V03 | T07-P07: latency=441ms; queue=71; retry=6 | T07-P07: 재발 | T07-P07: 축=재발; 원인 확정은 보류 | T07-P07: RECURRENCE_TIMELINE + T07-P07/CH08 | T07-P07: 피할 오판=복구 과잉 |
| T07-P07-V04 | T07-P07: latency=508ms; queue=2; retry=2 | T07-P07: 대형 입력 | T07-P07: 축=대형 입력; 원인 확정은 보류 | T07-P07: SIZE_LIMIT + T07-P07/CH08 | T07-P07: 피할 오판=잘못된 전제 |
| T07-P07-V05 | T07-P07: latency=575ms; queue=13; retry=5 | T07-P07: drain | T07-P07: 축=drain; 원인 확정은 보류 | T07-P07: DRAIN_STATE + T07-P07/CH08 | T07-P07: 피할 오판=경계 누락 |
| T07-P07-V06 | T07-P07: latency=642ms; queue=24; retry=1 | T07-P07: 재전송 | T07-P07: 축=재전송; 원인 확정은 보류 | T07-P07: IDEMPOTENCY_RECORD + T07-P07/CH08 | T07-P07: 피할 오판=증거 혼동 |
| T07-P07-V07 | T07-P07: latency=709ms; queue=35; retry=4 | T07-P07: 구버전 client | T07-P07: 축=구버전 client; 원인 확정은 보류 | T07-P07: CLIENT_VERSION + T07-P07/CH08 | T07-P07: 피할 오판=동시성 무시 |
| T07-P07-V08 | T07-P07: latency=776ms; queue=46; retry=0 | T07-P07: unknown outcome | T07-P07: 축=unknown outcome; 원인 확정은 보류 | T07-P07: PROVIDER_RESULT + T07-P07/CH08 | T07-P07: 피할 오판=상태 수명 혼동 |
| T07-P07-V09 | T07-P07: latency=843ms; queue=57; retry=3 | T07-P07: 재시작 | T07-P07: 축=재시작; 원인 확정은 보류 | T07-P07: DURABLE_STATE + T07-P07/CH08 | T07-P07: 피할 오판=운영 한계 누락 |
| T07-P07-V10 | T07-P07: latency=910ms; queue=68; retry=6 | T07-P07: 과부하 | T07-P07: 축=과부하; 원인 확정은 보류 | T07-P07: QUEUE_PRESSURE + T07-P07/CH08 | T07-P07: 피할 오판=오류 합치기 |
| T07-P07-V11 | T07-P07: latency=977ms; queue=79; retry=2 | T07-P07: sampling | T07-P07: 축=sampling; 원인 확정은 보류 | T07-P07: TRACE_METRIC_CROSSCHECK + T07-P07/CH08 | T07-P07: 피할 오판=복구 과잉 |
| T07-P07-V12 | T07-P07: latency=1044ms; queue=10; retry=5 | T07-P07: client disconnect | T07-P07: 축=client disconnect; 원인 확정은 보류 | T07-P07: COMMIT_TIMELINE + T07-P07/CH08 | T07-P07: 피할 오판=잘못된 전제 |
| T07-P07-V13 | T07-P07: latency=1111ms; queue=21; retry=1 | T07-P07: 재발 | T07-P07: 축=재발; 원인 확정은 보류 | T07-P07: RECURRENCE_TIMELINE + T07-P07/CH08 | T07-P07: 피할 오판=경계 누락 |
| T07-P07-V14 | T07-P07: latency=1178ms; queue=32; retry=4 | T07-P07: 대형 입력 | T07-P07: 축=대형 입력; 원인 확정은 보류 | T07-P07: SIZE_LIMIT + T07-P07/CH08 | T07-P07: 피할 오판=증거 혼동 |

## CHAPTER 24 · controller·service·repository로 책임을 나누는 이유 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P07에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P07-D01 | T07-P07: AI package 추가 | T07-P07: AI가 제안한 새 dependency를 도입한다 | T07-P07: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P07: RECURRENCE[occurrence=2; interval_s=243; mitigation_applied=1] | T07-P07: RECURRENCE_TIMELINE + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D02 | T07-P07: 비동기화 | T07-P07: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P07: durability·status API·worker retry 계약이 생기는지 | T07-P07: LARGE_INPUT[body_kb=616; limit_kb=512; parsed=0] | T07-P07: SIZE_LIMIT + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D03 | T07-P07: 권한 shortcut | T07-P07: payload의 owner/tenant id를 바로 사용한다 | T07-P07: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P07: DRAIN[ready=0; active=1; drain_deadline_s=7] | T07-P07: DRAIN_STATE + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D04 | T07-P07: 순서 병렬화 | T07-P07: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P07: 선후관계 invariant와 race를 깨지 않는지 | T07-P07: REPLAY[key=cmd-007-03; attempts=2; response_seen=0] | T07-P07: IDEMPOTENCY_RECORD + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D05 | T07-P07: validation 이동 | T07-P07: validation을 business side effect 뒤로 옮긴다 | T07-P07: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P07: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P07: CLIENT_VERSION + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D06 | T07-P07: batch 확대 | T07-P07: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P07: memory·deadline·부분 실패 범위가 커지는지 | T07-P07: UNKNOWN_OUTCOME[timeout_ms=157; provider_state=UNKNOWN; lookup_id=p00705] | T07-P07: PROVIDER_RESULT + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D07 | T07-P07: fallback 추가 | T07-P07: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P07: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P07: RESTART[crash_after_step=3; memory_lost=1; durable_check=pending] | T07-P07: DURABLE_STATE + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P07-D08 | T07-P07: 외부 호출 이동 | T07-P07: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P07: lock duration과 unknown outcome 경계가 달라지는지 | T07-P07: OVERLOAD[rps=437; p99_ms=471; queue=36] | T07-P07: QUEUE_PRESSURE + T07-P07/CH08 | T07-P07: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P07: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · controller·service·repository로 책임을 나누는 이유 — 최종 contract와 evidence spine

**최종 계약:** 한 handler에 모든 코드를 넣지 않고 입력 변환, 업무 규칙, 저장 접근을 역할별로 나누면 변경 이유가 분리된다.

**정상 메커니즘:** controller는 HTTP 계약, service/use-case는 업무 흐름, repository는 저장소 접근 경계를 맡도록 의존 방향을 제한한다.

**대표 실패:** 계층 이름만 많아지고 실제 책임은 섞이거나 service가 req/res에 의존해 HTTP 밖에서 재사용할 수 없게 된다.

**검증 evidence:** 함수 입력·출력 타입, 외부 의존 호출 위치, 변경 시 같이 수정되는 파일을 본다.

**직접 행동:** HTTP 정보가 없는 순수 service 함수를 만들고 controller가 변환만 하도록 분리한다.

**다음 연결:** `dependency injection은 외부 의존성을 밖에서 넣는다`.

`controller·service·repository로 책임을 나누는 이유`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | controller·service·repository로 책임을 나누는 이유의 개념·실패·운영 판단 교차 확인 |

`controller·service·repository로 책임을 나누는 이유` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
