# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 07 · AI와 함께 백엔드를 만들되 사람이 계약과 검증을 통제하기

### LESSON 11 · cost와 resource budget을 backend 설계에 포함하기

## CHAPTER 01 · cost와 resource budget을 backend 설계에 포함하기 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 성능 개선이 항상 더 큰 서버를 쓰는 문제는 아니며 호출·저장·cache·egress·AI token 같은 자원도 비용이 된다.

**아주 쉬운 사건.** 외부 API 호출이 요청당 8회다. 이 사건에서는 먼저 **resource unit과 cost budget을 계산한다**.

**왜 필요한가.** 정상 동작은 요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다. 반대로 무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다.

**암기:** `cost와 resource budget을 backend 설계에 포함하기`의 역할 한 줄.

**직접 이해:** `cost와 resource budget을 backend 설계에 포함하기`의 입력·상태·결과 경계.

**AI 위임 가능:** `cost와 resource budget을 backend 설계에 포함하기` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `외부 API 호출이 요청당 8회다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 request당 CPU/time/external call/bytes와 월 traffic을 측정한다.

## CHAPTER 02 · cost와 resource budget을 backend 설계에 포함하기 — 아주 쉬운 예를 한 단계씩 해석

T07-P101: `외부 API 호출이 요청당 8회다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 외부 API 호출이 요청당 8회다 | T07-P101 외부 입력 | T07-P101: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | resource unit과 cost budget을 계산한다 | T07-P101 판단 기준 | T07-P101/CH08 관측표와 대조 |
| 정상 경로 | T07-P101/CH03 M1→M5 | cost와 resource budget을 backend 설계에 포함하기: 완료 시점을 단계별로 분리 | T07-P101: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P101/CH06 무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다. | T07-P101: 깨진 계약 하나를 특정 | cost와 resource budget을 backend 설계에 포함하기: 증상과 원인을 분리 |
| 재검증 | T07-P101/CH10 직접 실행 | T07-P101: 예상값 T07-P101 a → p101 → b 기록 | cost와 resource budget을 backend 설계에 포함하기: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P101/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P101/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · cost와 resource budget을 backend 설계에 포함하기 — 내부 메커니즘과 상태 전이

요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 외부 API 호출이 요청당 8회다 | source/actor/size를 보존 |
| M2 | 경계 판단 | resource unit과 cost budget을 계산한다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | 요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | request당 CPU/time/external call/bytes와 월 traffic을 측정한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 단가와 사용량 배열로 기능별 추정 비용을 계산한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`cost와 resource budget을 backend 설계에 포함하기` 흐름을 framework 이름 없이 설명한다.

막히면 `외부 API 호출이 요청당 8회다`의 M1→M5를 다시 추적한다.

## CHAPTER 04 · cost와 resource budget을 backend 설계에 포함하기 — 실전 경계 A

**경계 A.** request당 DB query·cache operation·external API call·egress byte 같은 resource unit을 세면 traffic 증가가 비용으로 어떻게 확대되는지 모델링할 수 있다.

T07-P101/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P101에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P101/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P101/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P101): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P101/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P101-A1-608 | T07-P101 조건 | request당 DB query·cache operation·external API call·egress byte 같은 resource unit을 세면 traffic 증가가 비용으로 어떻게 확대되는지 모델링할 수 있다. |
| T07-P101-A2-609 | T07-P101 변화점 | T07-P101/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P101-A3-610 | T07-P101 반례 | T07-P101/CH06 대표 실패와 A 위반을 구별 |
| T07-P101-A4-611 | T07-P101 근거 | T07-P101/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P101-A5-612 | T07-P101 재실험 | 외부 API 호출이 요청당 8회다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · cost와 resource budget을 backend 설계에 포함하기 — 실전 경계 B

**경계 B.** 평균 비용뿐 아니라 peak concurrency와 capacity headroom을 함께 봐 급격한 traffic에서 autoscale이 비용과 downstream quota를 동시에 넘지 않게 한다.

T07-P101/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P101에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P101/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P101/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P101): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P101/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P101-B1-639 | T07-P101 조건 | 평균 비용뿐 아니라 peak concurrency와 capacity headroom을 함께 봐 급격한 traffic에서 autoscale이 비용과 downstream quota를 동시에 넘지 않게 한다. |
| T07-P101-B2-640 | T07-P101 독립성 | T07-P101/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P101-B3-641 | T07-P101 상태 | T07-P101/CH03 before·after 위치를 다시 지정 |
| T07-P101-B4-642 | T07-P101 반증 | T07-P101/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P101-B5-643 | T07-P101 적용 | cost와 resource budget을 backend 설계에 포함하기의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · cost와 resource budget을 backend 설계에 포함하기 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다.**

아래 여섯 사례는 T07-P101의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P101-F01 | T07-P101: 대표 실패 | T07-P101: T07-P101/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P101: 현상만 보고 원인을 확정 | T07-P101/CH08 evidence map에서 상태를 대조 |
| T07-P101-F02 | T07-P101: 경계 A 누락 | T07-P101: T07-P101/CH04 경계 A 위반 입력 | T07-P101: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P101/CH08 evidence map에서 상태를 대조 |
| T07-P101-F03 | T07-P101: 경계 B 누락 | T07-P101: T07-P101/CH05 경계 B 위반 입력 | T07-P101: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P101/CH08 evidence map에서 상태를 대조 |
| T07-P101-F04 | T07-P101: 복구 경계 C 누락 | T07-P101: T07-P101/CH07 경계 C 복구 조건 | T07-P101: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P101/CH08 evidence map에서 상태를 대조 |
| T07-P101-F05 | T07-P101: 운영 경계 D 누락 | T07-P101: T07-P101/CH09 경계 D 운영 조건 | T07-P101: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P101/CH08 evidence map에서 상태를 대조 |
| T07-P101-F06 | T07-P101: 증거 없는 결론 | T07-P101: T07-P101/CH02 첫 판단만 존재 | T07-P101: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P101/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P101/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · cost와 resource budget을 backend 설계에 포함하기 — 복구 가능한 상태와 수명

**경계 C.** cache를 추가해 DB cost를 줄이면서 cache cluster와 stale complexity 비용이 생기는 것처럼 최적화는 다른 자원으로 비용을 옮길 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P101에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P101/CH08 evidence map을 본다. 복구 후에는 T07-P101/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P101에서 이미 확정된 side effect는 T07-P101/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P101-R1-701 | T07-P101 중단 직전 | T07-P101/CH03에서 이미 확정된 상태만 표시 |
| T07-P101-R2-702 | T07-P101 재시작 직후 | cache를 추가해 DB cost를 줄이면서 cache cluster와 stale complexity 비용이 생기는 것처럼 최적화는 다른 자원으로 비용을 옮길 수 있다. |
| T07-P101-R3-703 | T07-P101 재검증 | T07-P101/CH08 근거로 중복·누락 여부 확인 |
| T07-P101-R4-704 | T07-P101 재실행 | T07-P101/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · cost와 resource budget을 backend 설계에 포함하기 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 외부 API 호출이 요청당 8회다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | resource unit과 cost budget을 계산한다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | 요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | request당 CPU/time/external call/bytes와 월 traffic을 측정한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`cost와 resource budget을 backend 설계에 포함하기` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · cost와 resource budget을 backend 설계에 포함하기 — 운영 한계와 종료 조건

**경계 D.** 비용 절감을 위해 timeout·replica·backup·observability를 줄여 SLO와 복구 능력을 깨뜨리면 성공한 최적화가 아니므로 reliability budget과 함께 평가한다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P101/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P101/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P101 과제: 경계 D와 T07-P101/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P101-O1-763 | synthetic-load=83 | T07-P101/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P101-O2-764 | synthetic-budget=913ms | T07-P101 timeout과 unknown outcome을 분리 |
| T07-P101-O3-765 | T07-P101 종료 | 비용 절감을 위해 timeout·replica·backup·observability를 줄여 SLO와 복구 능력을 깨뜨리면 성공한 최적화가 아니므로 reliability budget과 함께 평가한다. |
| T07-P101-O4-766 | T07-P101 완화 | T07-P101/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · cost와 resource budget을 backend 설계에 포함하기 — 직접 실행하는 작은 모델

`cost와 resource budget을 backend 설계에 포함하기` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P101 a|p101|b`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P101";
const ids = ["a", "p101", "a", "p101", "b"];
const unique = [...new Set(ids)];
console.log(marker, unique.join("|"));
```

기준 출력: `T07-P101 a|p101|b`.

`cost와 resource budget을 backend 설계에 포함하기`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P101-L1-795 | constmarker="T07-P101"; | T07-P101 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P101-L2-796 | constids=["a","p101","a","p101","b"]; | T07-P101 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P101-L3-797 | constunique=[...newSet(ids)]; | T07-P101 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P101-L4-798 | console.log(marker,unique.join("→")); | T07-P101 출력 관측점; 예상 `T07-P101 a→p101→b`와 비교 |
| T07-P101-LX-884 | T07-P101 실행 기록 | T07-P101 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · cost와 resource budget을 backend 설계에 포함하기 — 한 부분만 수정하고 다시 예측

수정 과제: **마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다**.

수정 전은 `T07-P101 a|p101|b`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P101/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P101-D1-825 | 기준 `T07-P101 a→p101→b` | T07-P101 수정 전 실행을 먼저 재현 |
| T07-P101-D2-826 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | T07-P101 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P101-D3-827 | T07-P101 새 예측 | T07-P101 실행 전에 출력·상태를 먼저 기록 |
| T07-P101-D4-828 | T07-P101 재실행 | T07-P101/CH10 실제값과 새 예측을 대조 |
| T07-P101-D5-829 | T07-P101 반례 | T07-P101/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P101-D6-830 | T07-P101 근거 | T07-P101/CH08 상태가 설명과 일치해야 완료 |
| T07-P101-D7-831 | T07-P101 이유 | T07-P101 변경 이유를 cost와 resource budget을 backend 설계에 포함하기 계약과 연결해 설명 |

## CHAPTER 12 · cost와 resource budget을 backend 설계에 포함하기 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: cost와 resource budget을 backend 설계에 포함하기 | 성능 개선이 항상 더 큰 서버를 쓰는 문제는 아니며 호출·저장·cache·egress·AI token 같은 자원도 비용이 된다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P101/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | 요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | request당 CPU/time/external call/bytes와 월 traffic을 측정한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P101/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P101/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P101/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P101/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `cost와 resource budget을 backend 설계에 포함하기` 실행 코드 수정 | 마지막에 첫 id를 한 번 더 추가하고 결과가 변하는지 설명한다 | `cost와 resource budget을 backend 설계에 포함하기` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P101/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P101/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · cost와 resource budget을 backend 설계에 포함하기 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 성능 개선이 항상 더 큰 서버를 쓰는 문제는 아니며 호출·저장·cache·egress·AI token 같은 자원도 비용이 된다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P101/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P101/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P101/CH10 실행용 boilerplate·test 후보 | T07-P101: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P101 a → p101 → b` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P101/CH02의 판단 기준과 T07-P101/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P101-AI1-887 | T07-P101 사람 결정 | T07-P101 업무 의미·허용 위험·완료 기준 소유 |
| T07-P101-AI2-888 | T07-P101 AI 초안 | T07-P101/CH10 boilerplate·test 후보까지만 위임 |
| T07-P101-AI3-889 | T07-P101 검증 | T07-P101/CH06 반례와 T07-P101/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · cost와 resource budget을 backend 설계에 포함하기 — 경계 조합 실험 8개

T07-P101: T07-P101/CH04~T07-P101/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P101의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-K01 | T07-P101: 경계 A | T07-P101: 경계 B | T07-P101: RECURRENCE[occurrence=3; interval_s=237; mitigation_applied=1] / sample=61 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + RECURRENCE_TIMELINE |
| T07-P101-K02 | T07-P101: 경계 A | T07-P101: 경계 C | T07-P101: LARGE_INPUT[body_kb=2256; limit_kb=384; parsed=0] / sample=68 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + SIZE_LIMIT |
| T07-P101-K03 | T07-P101: 경계 A | T07-P101: 경계 D | T07-P101: OWNER_SPOOF[actor=A3; payload_owner=B3; auth_owner=A3] / sample=75 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + AUTHZ_POLICY |
| T07-P101-K04 | T07-P101: 경계 B | T07-P101: 경계 C | T07-P101: REORDER[in_seq=7,5,6; applied_version=6] / sample=82 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + SEQUENCE_STATE |
| T07-P101-K05 | T07-P101: 경계 B | T07-P101: 경계 D | T07-P101: RECOVERY_SCOPE[selected=60; expected=6; backup=1; dry_run=1] / sample=89 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + RECOVERY_AUDIT |
| T07-P101-K06 | T07-P101: 경계 C | T07-P101: 경계 D | T07-P101: REPLAY[key=cmd-101-06; attempts=2; response_seen=0] / sample=96 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + IDEMPOTENCY_RECORD |
| T07-P101-K07 | T07-P101: 경계 A | T07-P101: 경계 B+C | T07-P101: OLD_SCHEMA[client=v1; server=v2; extra_field=1] / sample=14 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + CLIENT_VERSION |
| T07-P101-K08 | T07-P101: 경계 B | T07-P101: 경계 C+D | T07-P101: CONCURRENT_WRITE[actors=2; base_version=3; writes=2; gap_ms=78] / sample=21 | T07-P101: 먼저 깨지는 경계를 판정 | T07-P101: T07-P101/CH08 + STATE_VERSION |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P101/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · cost와 resource budget을 backend 설계에 포함하기 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P101와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-B10 | T07-P101: observability readiness를 코드 리뷰에 포함하기 | T07-P101: dashboard는 있지만 request id가 없다 | T07-P101: instrumentation이 실제 연결되는지 확인한다 | T07-P101: T07-P101/CH08 증거와 형제 LESSON 증거를 분리 | T07-P101: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P101-B12 | T07-P101: dependency와 supply-chain 경계 검토 | T07-P101: AI가 새 package 6개를 설치했다 | T07-P101: lockfile·transitive·provenance를 검토한다 | T07-P101: T07-P101/CH08 증거와 형제 LESSON 증거를 분리 | T07-P101: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P101-B09 | T07-P101: backend load test를 숫자 하나가 아닌 곡선으로 보기 | T07-P101: 100 RPS에서만 load test했다 | T07-P101: concurrency sweep과 saturation knee를 찾는다 | T07-P101: T07-P101/CH08 증거와 형제 LESSON 증거를 분리 | T07-P101: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P101-B13 | T07-P101: legacy backend를 안전하게 refactor하기 | T07-P101: legacy handler를 전부 다시 쓰려 한다 | T07-P101: characterization test와 작은 seam을 만든다 | T07-P101: T07-P101/CH08 증거와 형제 LESSON 증거를 분리 | T07-P101: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P101-B08 | T07-P101: OpenAPI를 실행 가능한 API 계약으로 쓰기 | T07-P101: OpenAPI와 실제 response가 다르다 | T07-P101: contract test로 drift를 찾는다 | T07-P101: T07-P101/CH08 증거와 형제 LESSON 증거를 분리 | T07-P101: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · cost와 resource budget을 backend 설계에 포함하기 — 선택형 실패 주입 6개

T07-P101: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P101 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-X01 | T07-P101: 소유권 위조 | T07-P101: tenant/owner가 payload에 포함됨; sample=157 | T07-P101: 식별 정보와 권한 근거 | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: authenticated context·resource owner·policy result |
| T07-P101-X02 | T07-P101: 순서 역전 | T07-P101: event가 원래 순서와 반대로 도착; sample=174 | T07-P101: 수신 순서와 업무 순서 | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: version/sequence·dedupe id·applied state |
| T07-P101-X03 | T07-P101: 복구 범위 | T07-P101: 복구 script 대상이 예상보다 큼; sample=191 | T07-P101: 진단과 destructive recovery | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: selected ids/count·backup·audit trail |
| T07-P101-X04 | T07-P101: 재전송 | T07-P101: 응답 유실 뒤 같은 command가 다시 도착함; sample=208 | T07-P101: 중복 side effect 여부 | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P101-X05 | T07-P101: 구버전 client | T07-P101: 한 단계 이전 schema가 요청됨; sample=225 | T07-P101: 호환 입력과 breaking change | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: schema version·실제 client 분포·contract test |
| T07-P101-X06 | T07-P101: 동시 변경 | T07-P101: 두 actor가 같은 resource를 수정함; sample=242 | T07-P101: lost update 또는 conflict | T07-P101: T07-P101/CH02 판단과 별도 기록 | T07-P101: version·affected rows·lock/wait 기록 |

## CHAPTER 17 · cost와 resource budget을 backend 설계에 포함하기 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P101에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-E01 | T07-P101: 대표 실패를 원인으로 착각 | T07-P101: T07-P101/CH06 실패 case를 다른 입력으로 재현 | T07-P101: 현상과 원인을 같은 것으로 봄 | T07-P101: T07-P101/CH06 대표 실패와 T07-P101/CH08 증거를 다시 대조 | T07-P101: T07-P101/CH08 |
| T07-P101-E02 | T07-P101: 경계 A 생략 | T07-P101: T07-P101/CH04의 조건 하나를 반대로 설정 | T07-P101: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P101: T07-P101/CH04를 새 입력에 적용 | T07-P101: T07-P101/CH08 |
| T07-P101-E03 | T07-P101: 경계 B 생략 | T07-P101: T07-P101/CH05의 조건 하나를 반대로 설정 | T07-P101: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P101: T07-P101/CH05를 새 입력에 적용 | T07-P101: T07-P101/CH08 |
| T07-P101-E04 | T07-P101: 복구 상태 혼동 | T07-P101: T07-P101/CH07에서 처리 중단을 주입 | T07-P101: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P101: T07-P101/CH07에서 수명 경계를 다시 표시 | T07-P101: T07-P101/CH08 |
| T07-P101-E05 | T07-P101: 운영 한계 누락 | T07-P101: T07-P101/CH09에서 부하 또는 drain 조건을 변경 | T07-P101: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P101: T07-P101/CH09의 종료 조건을 다시 작성 | T07-P101: T07-P101/CH08 |
| T07-P101-E06 | T07-P101: 증거 없는 성공 판정 | T07-P101: T07-P101/CH08에서 증거 하나를 숨김 | T07-P101: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P101: T07-P101/CH08에서 독립 증거 둘을 선택 | T07-P101: T07-P101/CH08 |

## CHAPTER 18 · cost와 resource budget을 backend 설계에 포함하기 — synthetic 관측값 판독 6개

T07-P101: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P101의 숫자 하나만으로 원인을 단정하지 않고 T07-P101/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-O01 | T07-P101/changed_files | 419 | T07-P101: 변경 파일 수 | T07-P101: 축=구버전 client; 원인 확정 금지 | T07-P101: CLIENT_VERSION + T07-P101/CH08 |
| T07-P101-O02 | T07-P101/dependency_count | 436 | T07-P101: 추가 dependency 수 | T07-P101: 축=동시 변경; 원인 확정 금지 | T07-P101: STATE_VERSION + T07-P101/CH08 |
| T07-P101-O03 | T07-P101/test_failures | 453 | T07-P101: 실패 test 수 | T07-P101: 축=unknown outcome; 원인 확정 금지 | T07-P101: PROVIDER_RESULT + T07-P101/CH08 |
| T07-P101-O04 | T07-P101/eval_passes | 470 | T07-P101: 통과 eval 수 | T07-P101: 축=과부하; 원인 확정 금지 | T07-P101: QUEUE_PRESSURE + T07-P101/CH08 |
| T07-P101-O05 | T07-P101/p99_ms | 487 | T07-P101: 변경 후 p99 | T07-P101: 축=sampling; 원인 확정 금지 | T07-P101: TRACE_METRIC_CROSSCHECK + T07-P101/CH08 |
| T07-P101-O06 | T07-P101/cost_units | 504 | T07-P101: resource/cost 단위 | T07-P101: 축=AI 복잡도; 원인 확정 금지 | T07-P101: DIFF_EVAL_ROLLBACK + T07-P101/CH08 |

## CHAPTER 19 · cost와 resource budget을 backend 설계에 포함하기 — 선택형 코드 리뷰 6질문

T07-P101: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P101에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-R01 | T07-P101: 관측 | T07-P101: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P101: CONCURRENT_WRITE[actors=2; base_version=3; writes=2; gap_ms=71] | T07-P101: T07-P101/CH04 | T07-P101: STATE_VERSION |
| T07-P101-R02 | T07-P101: 복구 | T07-P101: 재시작 뒤에도 필요한 상태가 남는가 | T07-P101: UNKNOWN_OUTCOME[timeout_ms=307; provider_state=UNKNOWN; lookup_id=p10101] | T07-P101: T07-P101/CH05 | T07-P101: PROVIDER_RESULT |
| T07-P101-R03 | T07-P101: 중복 | T07-P101: 같은 의도가 두 번 오면 무엇이 반복되는가 | T07-P101: OVERLOAD[rps=854; p99_ms=1173; queue=29] | T07-P101: T07-P101/CH06 | T07-P101: QUEUE_PRESSURE |
| T07-P101-R04 | T07-P101: timeout | T07-P101: 시간 초과가 실패인지 unknown인지 구분하는가 | T07-P101: SAMPLING[sample_rate=23%; trace_present=1; metric_present=1] | T07-P101: T07-P101/CH07 | T07-P101: TRACE_METRIC_CROSSCHECK |
| T07-P101-R05 | T07-P101: 관측 | T07-P101: 주장을 실제 log/metric/trace/state와 연결할 수 있는가 | T07-P101: AI_COMPLEXITY[deps_added=1; cache_layer=1; rollback_plan=0] | T07-P101: T07-P101/CH04 | T07-P101: DIFF_EVAL_ROLLBACK |
| T07-P101-R06 | T07-P101: 복구 | T07-P101: 재시작 뒤에도 필요한 상태가 남는가 | T07-P101: RECURRENCE[occurrence=2; interval_s=70; mitigation_applied=1] | T07-P101: T07-P101/CH05 | T07-P101: RECURRENCE_TIMELINE |

## CHAPTER 20 · cost와 resource budget을 backend 설계에 포함하기 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P101에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-I01 | T07-P101: 복구 확인 | T07-P101: durable state와 사용자 결과를 모두 확인 | T07-P101: UNKNOWN_OUTCOME[timeout_ms=296; provider_state=UNKNOWN; lookup_id=p10100] | T07-P101: T07-P101/CH08 + PROVIDER_RESULT | T07-P101-incident-928 |
| T07-P101-I02 | T07-P101: 재주입 | T07-P101: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P101: OVERLOAD[rps=821; p99_ms=1056; queue=15] | T07-P101: T07-P101/CH08 + QUEUE_PRESSURE | T07-P101-incident-929 |
| T07-P101-I03 | T07-P101: 회귀 고정 | T07-P101: test·metric·alert·runbook 중 재발 방지 장치에 반영 | T07-P101: SAMPLING[sample_rate=27%; trace_present=0; metric_present=1] | T07-P101: T07-P101/CH08 + TRACE_METRIC_CROSSCHECK | T07-P101-incident-930 |
| T07-P101-I04 | T07-P101: 영향 범위 | T07-P101: 영향 받은 요청·job·resource 범위를 수치로 고정 | T07-P101: AI_COMPLEXITY[deps_added=4; cache_layer=1; rollback_plan=1] | T07-P101: T07-P101/CH08 + DIFF_EVAL_ROLLBACK | T07-P101-incident-931 |
| T07-P101-I05 | T07-P101: 변경 동결 | T07-P101: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P101: RECURRENCE[occurrence=6; interval_s=59; mitigation_applied=1] | T07-P101: T07-P101/CH08 + RECURRENCE_TIMELINE | T07-P101-incident-932 |
| T07-P101-I06 | T07-P101: correlation | T07-P101: 한 request/job/resource id를 시간축에 고정 | T07-P101: LARGE_INPUT[body_kb=832; limit_kb=384; parsed=0] | T07-P101: T07-P101/CH08 + SIZE_LIMIT | T07-P101-incident-933 |

## CHAPTER 21 · cost와 resource budget을 backend 설계에 포함하기 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P101/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P101/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P101/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P101/CH04~T07-P101/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P101/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P101/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P101/CH18 signal 두 개와 T07-P101/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P101/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P101/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · cost와 resource budget을 backend 설계에 포함하기 — 통합 casebook 16문제

T07-P101 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P101 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P101-C01 | T07-P101: 경계 A | T07-P101: OVERLOAD[rps=788; p99_ms=939; queue=21] | T07-P101: load=329, window=17s | T07-P101: review=상태 변경 | T07-P101: 경계 A 위반 여부를 판정 | T07-P101: QUEUE_PRESSURE + T07-P101/CH08 |
| T07-P101-C02 | T07-P101: 경계 B | T07-P101: SAMPLING[sample_rate=14%; trace_present=1; metric_present=1] | T07-P101: load=352, window=36s | T07-P101: review=retry | T07-P101: 경계 B 위반 여부를 판정 | T07-P101: TRACE_METRIC_CROSSCHECK + T07-P101/CH08 |
| T07-P101-C03 | T07-P101: 경계 C | T07-P101: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P101: load=375, window=55s | T07-P101: review=복구 | T07-P101: 경계 C 위반 여부를 판정 | T07-P101: DIFF_EVAL_ROLLBACK + T07-P101/CH08 |
| T07-P101-C04 | T07-P101: 경계 D | T07-P101: RECURRENCE[occurrence=5; interval_s=48; mitigation_applied=1] | T07-P101: load=398, window=74s | T07-P101: review=동시성 | T07-P101: 경계 D 위반 여부를 판정 | T07-P101: RECURRENCE_TIMELINE + T07-P101/CH08 |
| T07-P101-C05 | T07-P101: 경계 A | T07-P101: LARGE_INPUT[body_kb=744; limit_kb=384; parsed=0] | T07-P101: load=421, window=93s | T07-P101: review=민감정보 | T07-P101: 경계 A 위반 여부를 판정 | T07-P101: SIZE_LIMIT + T07-P101/CH08 |
| T07-P101-C06 | T07-P101: 경계 B | T07-P101: OWNER_SPOOF[actor=A3; payload_owner=B0; auth_owner=A3] | T07-P101: load=444, window=22s | T07-P101: review=중복 | T07-P101: 경계 B 위반 여부를 판정 | T07-P101: AUTHZ_POLICY + T07-P101/CH08 |
| T07-P101-C07 | T07-P101: 경계 C | T07-P101: REORDER[in_seq=9,7,8; applied_version=6] | T07-P101: load=467, window=41s | T07-P101: review=권한 | T07-P101: 경계 C 위반 여부를 판정 | T07-P101: SEQUENCE_STATE + T07-P101/CH08 |
| T07-P101-C08 | T07-P101: 경계 D | T07-P101: RECOVERY_SCOPE[selected=82; expected=13; backup=1; dry_run=1] | T07-P101: load=490, window=60s | T07-P101: review=입력 경계 | T07-P101: 경계 D 위반 여부를 판정 | T07-P101: RECOVERY_AUDIT + T07-P101/CH08 |
| T07-P101-C09 | T07-P101: 경계 A | T07-P101: REPLAY[key=cmd-101-08; attempts=4; response_seen=0] | T07-P101: load=513, window=79s | T07-P101: review=timeout | T07-P101: 경계 A 위반 여부를 판정 | T07-P101: IDEMPOTENCY_RECORD + T07-P101/CH08 |
| T07-P101-C10 | T07-P101: 경계 B | T07-P101: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P101: load=536, window=98s | T07-P101: review=자원 | T07-P101: 경계 B 위반 여부를 판정 | T07-P101: CLIENT_VERSION + T07-P101/CH08 |
| T07-P101-C11 | T07-P101: 경계 C | T07-P101: CONCURRENT_WRITE[actors=2; base_version=3; writes=2; gap_ms=7] | T07-P101: load=559, window=27s | T07-P101: review=순서 | T07-P101: 경계 C 위반 여부를 판정 | T07-P101: STATE_VERSION + T07-P101/CH08 |
| T07-P101-C12 | T07-P101: 경계 D | T07-P101: UNKNOWN_OUTCOME[timeout_ms=206; provider_state=UNKNOWN; lookup_id=p10111] | T07-P101: load=582, window=46s | T07-P101: review=관측 | T07-P101: 경계 D 위반 여부를 판정 | T07-P101: PROVIDER_RESULT + T07-P101/CH08 |
| T07-P101-C13 | T07-P101: 경계 A | T07-P101: OVERLOAD[rps=551; p99_ms=597; queue=35] | T07-P101: load=605, window=65s | T07-P101: review=상태 변경 | T07-P101: 경계 A 위반 여부를 판정 | T07-P101: QUEUE_PRESSURE + T07-P101/CH08 |
| T07-P101-C14 | T07-P101: 경계 B | T07-P101: SAMPLING[sample_rate=56%; trace_present=1; metric_present=1] | T07-P101: load=628, window=84s | T07-P101: review=retry | T07-P101: 경계 B 위반 여부를 판정 | T07-P101: TRACE_METRIC_CROSSCHECK + T07-P101/CH08 |
| T07-P101-C15 | T07-P101: 경계 C | T07-P101: AI_COMPLEXITY[deps_added=3; cache_layer=1; rollback_plan=0] | T07-P101: load=651, window=13s | T07-P101: review=복구 | T07-P101: 경계 C 위반 여부를 판정 | T07-P101: DIFF_EVAL_ROLLBACK + T07-P101/CH08 |
| T07-P101-C16 | T07-P101: 경계 D | T07-P101: RECURRENCE[occurrence=2; interval_s=180; mitigation_applied=1] | T07-P101: load=674, window=32s | T07-P101: review=동시성 | T07-P101: 경계 D 위반 여부를 판정 | T07-P101: RECURRENCE_TIMELINE + T07-P101/CH08 |

채점은 결론보다 근거를 본다. T07-P101/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · cost와 resource budget을 backend 설계에 포함하기 — evidence 판독 문제 14개

T07-P101 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P101 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P101-V01 | T07-P101: latency=561ms; queue=67; retry=3 | T07-P101: sampling | T07-P101: 축=sampling; 원인 확정은 보류 | T07-P101: TRACE_METRIC_CROSSCHECK + T07-P101/CH08 | T07-P101: 피할 오판=오류 합치기 |
| T07-P101-V02 | T07-P101: latency=628ms; queue=78; retry=6 | T07-P101: AI 복잡도 | T07-P101: 축=AI 복잡도; 원인 확정은 보류 | T07-P101: DIFF_EVAL_ROLLBACK + T07-P101/CH08 | T07-P101: 피할 오판=AI 과신 |
| T07-P101-V03 | T07-P101: latency=695ms; queue=9; retry=2 | T07-P101: 재발 | T07-P101: 축=재발; 원인 확정은 보류 | T07-P101: RECURRENCE_TIMELINE + T07-P101/CH08 | T07-P101: 피할 오판=복구 과잉 |
| T07-P101-V04 | T07-P101: latency=762ms; queue=20; retry=5 | T07-P101: 대형 입력 | T07-P101: 축=대형 입력; 원인 확정은 보류 | T07-P101: SIZE_LIMIT + T07-P101/CH08 | T07-P101: 피할 오판=잘못된 전제 |
| T07-P101-V05 | T07-P101: latency=829ms; queue=31; retry=1 | T07-P101: 소유권 위조 | T07-P101: 축=소유권 위조; 원인 확정은 보류 | T07-P101: AUTHZ_POLICY + T07-P101/CH08 | T07-P101: 피할 오판=경계 누락 |
| T07-P101-V06 | T07-P101: latency=896ms; queue=42; retry=4 | T07-P101: 순서 역전 | T07-P101: 축=순서 역전; 원인 확정은 보류 | T07-P101: SEQUENCE_STATE + T07-P101/CH08 | T07-P101: 피할 오판=증거 혼동 |
| T07-P101-V07 | T07-P101: latency=963ms; queue=53; retry=0 | T07-P101: 복구 범위 | T07-P101: 축=복구 범위; 원인 확정은 보류 | T07-P101: RECOVERY_AUDIT + T07-P101/CH08 | T07-P101: 피할 오판=재시도 오판 |
| T07-P101-V08 | T07-P101: latency=1030ms; queue=64; retry=3 | T07-P101: 재전송 | T07-P101: 축=재전송; 원인 확정은 보류 | T07-P101: IDEMPOTENCY_RECORD + T07-P101/CH08 | T07-P101: 피할 오판=소유권 혼동 |
| T07-P101-V09 | T07-P101: latency=1097ms; queue=75; retry=6 | T07-P101: 구버전 client | T07-P101: 축=구버전 client; 원인 확정은 보류 | T07-P101: CLIENT_VERSION + T07-P101/CH08 | T07-P101: 피할 오판=동시성 무시 |
| T07-P101-V10 | T07-P101: latency=1164ms; queue=6; retry=2 | T07-P101: 동시 변경 | T07-P101: 축=동시 변경; 원인 확정은 보류 | T07-P101: STATE_VERSION + T07-P101/CH08 | T07-P101: 피할 오판=운영 한계 누락 |
| T07-P101-V11 | T07-P101: latency=1231ms; queue=17; retry=5 | T07-P101: unknown outcome | T07-P101: 축=unknown outcome; 원인 확정은 보류 | T07-P101: PROVIDER_RESULT + T07-P101/CH08 | T07-P101: 피할 오판=오류 합치기 |
| T07-P101-V12 | T07-P101: latency=1298ms; queue=28; retry=1 | T07-P101: 과부하 | T07-P101: 축=과부하; 원인 확정은 보류 | T07-P101: QUEUE_PRESSURE + T07-P101/CH08 | T07-P101: 피할 오판=AI 과신 |
| T07-P101-V13 | T07-P101: latency=1365ms; queue=39; retry=4 | T07-P101: sampling | T07-P101: 축=sampling; 원인 확정은 보류 | T07-P101: TRACE_METRIC_CROSSCHECK + T07-P101/CH08 | T07-P101: 피할 오판=복구 과잉 |
| T07-P101-V14 | T07-P101: latency=1432ms; queue=50; retry=0 | T07-P101: AI 복잡도 | T07-P101: 축=AI 복잡도; 원인 확정은 보류 | T07-P101: DIFF_EVAL_ROLLBACK + T07-P101/CH08 | T07-P101: 피할 오판=잘못된 전제 |

## CHAPTER 24 · cost와 resource budget을 backend 설계에 포함하기 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P101에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P101-D01 | T07-P101: 권한 shortcut | T07-P101: payload의 owner/tenant id를 바로 사용한다 | T07-P101: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P101: RECURRENCE[occurrence=2; interval_s=226; mitigation_applied=1] | T07-P101: RECURRENCE_TIMELINE + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D02 | T07-P101: 순서 병렬화 | T07-P101: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P101: 선후관계 invariant와 race를 깨지 않는지 | T07-P101: LARGE_INPUT[body_kb=2168; limit_kb=384; parsed=0] | T07-P101: SIZE_LIMIT + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D03 | T07-P101: validation 이동 | T07-P101: validation을 business side effect 뒤로 옮긴다 | T07-P101: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P101: OWNER_SPOOF[actor=A3; payload_owner=B2; auth_owner=A3] | T07-P101: AUTHZ_POLICY + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D04 | T07-P101: batch 확대 | T07-P101: 한 번에 처리하는 item 수를 크게 늘린다 | T07-P101: memory·deadline·부분 실패 범위가 커지는지 | T07-P101: REORDER[in_seq=6,4,5; applied_version=6] | T07-P101: SEQUENCE_STATE + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D05 | T07-P101: fallback 추가 | T07-P101: dependency 실패 때 이전 값이나 기본값을 돌려준다 | T07-P101: stale 허용 범위와 사용자 오인을 정의했는지 | T07-P101: RECOVERY_SCOPE[selected=49; expected=12; backup=1; dry_run=0] | T07-P101: RECOVERY_AUDIT + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D06 | T07-P101: 외부 호출 이동 | T07-P101: 외부 API 호출 위치를 transaction 안팎으로 옮긴다 | T07-P101: lock duration과 unknown outcome 경계가 달라지는지 | T07-P101: REPLAY[key=cmd-101-05; attempts=4; response_seen=0] | T07-P101: IDEMPOTENCY_RECORD + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D07 | T07-P101: cache 추가 | T07-P101: 현재 결과 앞에 cache layer를 추가한다 | T07-P101: stale·key·invalidation 책임이 새로 생기는지 | T07-P101: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P101: CLIENT_VERSION + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P101-D08 | T07-P101: pool 확대 | T07-P101: connection/worker pool 상한을 늘린다 | T07-P101: downstream saturation을 숨기거나 증폭하지 않는지 | T07-P101: CONCURRENT_WRITE[actors=2; base_version=3; writes=2; gap_ms=65] | T07-P101: STATE_VERSION + T07-P101/CH08 | T07-P101: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P101: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · cost와 resource budget을 backend 설계에 포함하기 — 최종 contract와 evidence spine

**최종 계약:** 성능 개선이 항상 더 큰 서버를 쓰는 문제는 아니며 호출·저장·cache·egress·AI token 같은 자원도 비용이 된다.

**정상 메커니즘:** 요청당 resource unit과 예상 traffic을 곱해 budget을 만들고 비용과 latency/reliability trade-off를 함께 본다.

**대표 실패:** 무료 tier나 현재 traffic만 기준으로 설계해 scale 시 갑작스러운 비용이 생긴다.

**검증 evidence:** request당 CPU/time/external call/bytes와 월 traffic을 측정한다.

**직접 행동:** 단가와 사용량 배열로 기능별 추정 비용을 계산한다.

**다음 연결:** `dependency와 supply-chain 경계 검토`.

`cost와 resource budget을 backend 설계에 포함하기`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| FOSA2 | Fundamentals of Software Architecture, 2nd Edition | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |
| SRE | SRE | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |
| OWASP-API | OWASP-API | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |
| NIST-SSDF | NIST-SSDF | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | cost와 resource budget을 backend 설계에 포함하기의 개념·실패·운영 판단 교차 확인 |

`cost와 resource budget을 backend 설계에 포함하기` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
