# TRACK 07 · 서버와 백엔드 — 요청에서 운영까지

## BLOCK 01 · 요청이 서버에 도착해 응답이 되기까지

### LESSON 01 · 서버 프로그램과 요청의 첫 진입점

## CHAPTER 01 · 서버 프로그램과 요청의 첫 진입점 — 쉬운 말에서 정확한 계약까지

**출발 개념.** 서버는 저장된 파일이 아니라 실행 중인 process가 요청을 받아 처리하는 프로그램이다.

**아주 쉬운 사건.** 요청 A가 앱까지 도착하지 않는다. 이 사건에서는 먼저 **process/listen/network 경계를 분리해서 본다**.

**왜 필요한가.** 정상 동작은 listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다. 반대로 파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다.

**암기:** `서버 프로그램과 요청의 첫 진입점`의 역할 한 줄.

**직접 이해:** `서버 프로그램과 요청의 첫 진입점`의 입력·상태·결과 경계.

**AI 위임 가능:** `서버 프로그램과 요청의 첫 진입점` 문법과 boilerplate 초안. 최종 의미는 사람이 결정한다.

첫 확인 문제: `요청 A가 앱까지 도착하지 않는다`에서 서버가 확정한 사실과 아직 확정하지 못한 사실을 각각 적는다. 근거는 process 생존 여부, listen 주소와 port, 요청 도착 로그, 응답 완료 시점을 서로 다른 증거로 확인한다.

## CHAPTER 02 · 서버 프로그램과 요청의 첫 진입점 — 아주 쉬운 예를 한 단계씩 해석

T07-P01: `요청 A가 앱까지 도착하지 않는다`를 입력·판단·상태로 나눠 읽는다.

| 단계 | 현재 내용 | 확정 정도 | 다음 확인 |
| --- | --- | --- | --- |
| 입력 | 요청 A가 앱까지 도착하지 않는다 | T07-P01 외부 입력 | T07-P01: 외부에서 관찰된 값; 아직 내부 상태로 확정하지 않음 |
| 첫 판단 | process/listen/network 경계를 분리해서 본다 | T07-P01 판단 기준 | T07-P01/CH08 관측표와 대조 |
| 정상 경로 | T07-P01/CH03 M1→M5 | 서버 프로그램과 요청의 첫 진입점: 완료 시점을 단계별로 분리 | T07-P01: 중간 성공과 최종 성공을 구분 |
| 반례 | T07-P01/CH06 파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다. | T07-P01: 깨진 계약 하나를 특정 | 서버 프로그램과 요청의 첫 진입점: 증상과 원인을 분리 |
| 재검증 | T07-P01/CH10 직접 실행 | T07-P01: 예상값 T07-P001 RECEIVED->CHECKED 기록 | 서버 프로그램과 요청의 첫 진입점: 실행 결과와 이유를 함께 확인 |

오답 예는 T07-P01/CH06의 대표 실패를 재현한다. 왜 틀렸는지는 T07-P01/CH08의 실제 관측과 충돌하는지 확인한다.

## CHAPTER 03 · 서버 프로그램과 요청의 첫 진입점 — 내부 메커니즘과 상태 전이

listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다.

| 표시 | 전이 | 이 LESSON의 값 | 확인할 경계 |
| --- | --- | --- | --- |
| M1 | 입력 도착 | 요청 A가 앱까지 도착하지 않는다 | source/actor/size를 보존 |
| M2 | 경계 판단 | process/listen/network 경계를 분리해서 본다 | 확정·미확정 또는 허용·거부를 구분 |
| M3 | 메커니즘 진행 | listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다. | 상태가 바뀌는 지점을 분리 |
| M4 | 증거 남김 | process 생존 여부, listen 주소와 port, 요청 도착 로그, 응답 완료 시점을 서로 다른 증거로 확인한다. | 결과 문자열보다 상태를 우선 |
| M5 | 응답/후속 | 요청 객체를 입력으로 받아 응답 객체를 만드는 작은 순수 함수를 직접 실행한다. | 동기 완료와 후속 완료를 혼동하지 않음 |

`서버 프로그램과 요청의 첫 진입점` 흐름을 framework 이름 없이 설명한다.

막히면 `요청 A가 앱까지 도착하지 않는다`의 M1→M5를 다시 추적한다.

### 전이 체크포인트
| case | 위치 | 확인할 차이 |
| --- | --- | --- |
| T07-P01-M1-183 | T07-P01/CH02 입력 | T07-P01 읽기 전용 단계 |
| T07-P01-M2-184 | T07-P01/CH03 판단 | T07-P01 상태 변경 직전 경계 |
| T07-P01-M3-185 | T07-P01/CH08 검증 | T07-P01 변경 뒤 실제 상태 대조 |
| T07-P01-M4-186 | T07-P01/CH06 반례 | T07-P01 성공 문자열과 상태 성공을 분리 |

## CHAPTER 04 · 서버 프로그램과 요청의 첫 진입점 — 실전 경계 A

**경계 A.** 서버 process가 살아 있어도 127.0.0.1에만 bind되어 있으면 같은 기기 밖에서는 접근할 수 없고, 0.0.0.0 bind는 모든 interface에서 받겠다는 뜻이지 방화벽까지 자동으로 여는 뜻은 아니다.

T07-P01/CH02 사건에 경계 A를 추가해 다시 판정한다. T07-P01에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P01/CH06과 연결한다. 경계 A 하나만 뒤집어 재현하고 T07-P01/CH08에서 맞는 관측 항목을 고른다.

문제 A (T07-P01): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P01/CH08의 상태 근거를 적는다.

### 경계 A 판정표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P01-A1-214 | T07-P01 조건 | 서버 process가 살아 있어도 127.0.0.1에만 bind되어 있으면 같은 기기 밖에서는 접근할 수 없고, 0.0.0.0 bind는 모든 interface에서 받겠다는 뜻이지 방화벽까지 자동으로 여는 뜻은 아니다. |
| T07-P01-A2-215 | T07-P01 변화점 | T07-P01/CH03 중 경계 A가 바꾸는 단계 지정 |
| T07-P01-A3-216 | T07-P01 반례 | T07-P01/CH06 대표 실패와 A 위반을 구별 |
| T07-P01-A4-217 | T07-P01 근거 | T07-P01/CH08에서 A 판정을 뒤집을 증거 선택 |
| T07-P01-A5-218 | T07-P01 재실험 | 요청 A가 앱까지 도착하지 않는다에서 actor·순서·크기 중 하나만 변경 |

## CHAPTER 05 · 서버 프로그램과 요청의 첫 진입점 — 실전 경계 B

**경계 B.** 같은 port를 이미 다른 process가 점유하면 새 server는 listen 단계에서 실패해야 하며, 시작 로그보다 실제 socket bind 성공과 PID·port 소유 관계를 확인해야 한다.

T07-P01/CH02 사건에 경계 B를 추가해 다시 판정한다. T07-P01에서는 기존 결론을 그대로 재사용하지 않는다.

실패 비교는 T07-P01/CH06과 연결한다. 경계 B 하나만 뒤집어 재현하고 T07-P01/CH08에서 맞는 관측 항목을 고른다.

문제 B (T07-P01): 정상/거부/재시도/보류 중 이 개념에 맞는 결론을 고르고 T07-P01/CH08의 상태 근거를 적는다.

### 경계 B 분리표
| case | 축 | 이 LESSON의 작업 |
| --- | --- | --- |
| T07-P01-B1-245 | T07-P01 조건 | 같은 port를 이미 다른 process가 점유하면 새 server는 listen 단계에서 실패해야 하며, 시작 로그보다 실제 socket bind 성공과 PID·port 소유 관계를 확인해야 한다. |
| T07-P01-B2-246 | T07-P01 독립성 | T07-P01/CH04 A 통과를 B 통과로 간주하지 않음 |
| T07-P01-B3-247 | T07-P01 상태 | T07-P01/CH03 before·after 위치를 다시 지정 |
| T07-P01-B4-248 | T07-P01 반증 | T07-P01/CH08에서 기존 결론과 충돌하는 관측 선택 |
| T07-P01-B5-249 | T07-P01 적용 | 서버 프로그램과 요청의 첫 진입점의 호환·경쟁·수명 축 중 해당 축 표시 |

## CHAPTER 06 · 서버 프로그램과 요청의 첫 진입점 — 실패를 증상·원인·복구로 나누기

대표 실패 내용: **파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다.**

아래 여섯 사례는 T07-P01의 고유 경계를 하나씩 깨뜨린다. 같은 증상을 같은 원인으로 단정하지 않는다.

| 실패 | 오류 유형 | 주입 조건 | 잘못된 추론 | 확인·복구 |
| --- | --- | --- | --- | --- |
| T07-P01-F01 | T07-P01: 대표 실패 | T07-P01: T07-P01/CH02 기준 사건에 대표 실패 조건을 주입 | T07-P01: 현상만 보고 원인을 확정 | T07-P01/CH08 evidence map에서 상태를 대조 |
| T07-P01-F02 | T07-P01: 경계 A 누락 | T07-P01: T07-P01/CH04 경계 A 위반 입력 | T07-P01: 경계 A를 확인하지 않고 정상이라고 단정 | T07-P01/CH08 evidence map에서 상태를 대조 |
| T07-P01-F03 | T07-P01: 경계 B 누락 | T07-P01: T07-P01/CH05 경계 B 위반 입력 | T07-P01: 경계 B가 바뀌었는데 기존 판단을 그대로 재사용 | T07-P01/CH08 evidence map에서 상태를 대조 |
| T07-P01-F04 | T07-P01: 복구 경계 C 누락 | T07-P01: T07-P01/CH07 경계 C 복구 조건 | T07-P01: 재시작·복구 뒤 상태 수명을 확인하지 않고 완료로 판정 | T07-P01/CH08 evidence map에서 상태를 대조 |
| T07-P01-F05 | T07-P01: 운영 경계 D 누락 | T07-P01: T07-P01/CH09 경계 D 운영 조건 | T07-P01: 작은 입력의 성공을 운영 규모에도 그대로 확대 | T07-P01/CH08 evidence map에서 상태를 대조 |
| T07-P01-F06 | T07-P01: 증거 없는 결론 | T07-P01: T07-P01/CH02 첫 판단만 존재 | T07-P01: 첫 판단만으로 성공을 선언하고 관측 근거를 생략 | T07-P01/CH08 evidence map에서 상태를 대조 |

복구 뒤에는 T07-P01/CH08에서 원인 제거 근거를 확인하고, 같은 입력을 그대로 외워 재도전하지 않는다.

## CHAPTER 07 · 서버 프로그램과 요청의 첫 진입점 — 복구 가능한 상태와 수명

**경계 C.** process 생존 여부와 readiness는 다르다. process가 떠 있어도 필수 설정 로드나 dependency 준비가 끝나지 않았다면 새 traffic을 받을 준비가 안 된 상태일 수 있다.

복구 질문: 처리 중간에 process가 멈췄다고 가정한다.

T07-P01에서는 memory-only 상태와 restart 뒤에도 남아야 할 durable 상태를 나눈다.

복구 전에는 T07-P01/CH08 evidence map을 본다. 복구 후에는 T07-P01/CH10 모델을 새 입력으로 실행해 중복·누락을 확인한다.

재시작을 rollback과 같다고 쓰지 않는다. T07-P01에서 이미 확정된 side effect는 T07-P01/CH08의 상태 증거로 확인한다.

### 복구 상태 ledger
| case | 시점 | 확인할 사실 |
| --- | --- | --- |
| T07-P01-R1-307 | T07-P01 중단 직전 | T07-P01/CH03에서 이미 확정된 상태만 표시 |
| T07-P01-R2-308 | T07-P01 재시작 직후 | process 생존 여부와 readiness는 다르다. process가 떠 있어도 필수 설정 로드나 dependency 준비가 끝나지 않았다면 새 traffic을 받을 준비가 안 된 상태일 수 있다. |
| T07-P01-R3-309 | T07-P01 재검증 | T07-P01/CH08 근거로 중복·누락 여부 확인 |
| T07-P01-R4-310 | T07-P01 재실행 | T07-P01/CH10 모델 중 다시 실행 가능한 단계만 선택 |

## CHAPTER 08 · 서버 프로그램과 요청의 첫 진입점 — evidence로 추측을 줄이는 법

이 LESSON을 디버깅할 때는 관측 가능한 증거에서 시작한다.

| 증거층 | 이 LESSON의 관찰값 | 기록 원칙 | 답할 질문 |
| --- | --- | --- | --- |
| 입력 증거 | 요청 A가 앱까지 도착하지 않는다 | 원본/정규화 전 값을 구분 | 무엇이 실제로 들어왔는지 |
| 결정 증거 | process/listen/network 경계를 분리해서 본다 | 허용·거부 이유 | 어떤 규칙이 작동했는지 |
| 상태 증거 | listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다. | before/after 또는 version | 어디까지 확정됐는지 |
| 실패 증거 | 파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다. | error class와 first failure | 증상보다 앞선 원인 후보 |
| 운영 증거 | process 생존 여부, listen 주소와 port, 요청 도착 로그, 응답 완료 시점을 서로 다른 증거로 확인한다. | 시간축·correlation 유지 | 다른 요청과 섞이지 않는지 |

`서버 프로그램과 요청의 첫 진입점` 로그에 secret·token은 남기지 않는다.

필요한 식별자와 상태만 보존한다.

## CHAPTER 09 · 서버 프로그램과 요청의 첫 진입점 — 운영 한계와 종료 조건

**경계 D.** 종료할 때 listener부터 닫아 새 요청을 막고 이미 처리 중인 요청에는 제한된 drain 시간을 주는 절차를 두면 배포 중 요청을 중간에서 끊는 비율을 줄일 수 있다.

작은 개발 환경에서 맞았던 규칙을 운영 크기로 확장한다. T07-P01/CH02의 기준 사건에 traffic 증가·긴 수명·종료 중 요청 중 하나를 추가한다.

초기 판단만으로 운영 결론을 내리지 않는다. 시간축은 T07-P01/CH08 관측표로 복원한다. 이 개념에 맞는 limit·deadline·drain·retention 조건이 있다면 명시하고, 없으면 해당 없음으로 표시한다.

T07-P01 과제: 경계 D와 T07-P01/CH06 대표 실패가 겹칠 때 증폭되는 장애를 한 문장으로 설명한다.

### 운영 경계 실험표
| case | synthetic 입력 | 판정할 항목 |
| --- | --- | --- |
| T07-P01-O1-369 | synthetic-load=49 | T07-P01/CH08에서 queue·wait·미완료 중 해당 신호 선택 |
| T07-P01-O2-370 | synthetic-budget=519ms | T07-P01 timeout과 unknown outcome을 분리 |
| T07-P01-O3-371 | T07-P01 종료 | 종료할 때 listener부터 닫아 새 요청을 막고 이미 처리 중인 요청에는 제한된 drain 시간을 주는 절차를 두면 배포 중 요청을 중간에서 끊는 비율을 줄일 수 있다. |
| T07-P01-O4-372 | T07-P01 완화 | T07-P01/CH06 원인 제거와 단순 증상 감소를 구분 |

## CHAPTER 10 · 서버 프로그램과 요청의 첫 진입점 — 직접 실행하는 작은 모델

`서버 프로그램과 요청의 첫 진입점` 실습은 fake server를 만들지 않는다.

sandbox에서는 네트워크 없는 순수 계산만 실행한다.

예상 출력: `T07-P001 RECEIVED->CHECKED`. 예상 뒤에 실제 실행한다.

```typescript
const marker = "T07-P001";
const stages = ["RECEIVED", "CHECKED", "APPLIED", "DONE"];
const stopAt = 2;
const visited = stages.slice(0, stopAt);
console.log(marker, visited.join("->"));
```

기준 출력: `T07-P001 RECEIVED->CHECKED`.

`서버 프로그램과 요청의 첫 진입점`의 실제 HTTP·DB·queue 통합은 별도 환경에서 검증한다.

### 코드 한 줄 해석표
| case | 실제 코드 조각 | 이 줄에서 확인할 것 |
| --- | --- | --- |
| T07-P01-L1-401 | constmarker="T07-P001"; | T07-P01 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P01-L2-402 | conststages=["RECEIVED","CHECKED","APPLIED","DONE"]; | T07-P01 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P01-L3-403 | conststopAt=2; | T07-P01 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P01-L4-404 | constvisited=stages.slice(0,stopAt); | T07-P01 입력·상태 이름; 값 출처를 눈에 보이게 고정 |
| T07-P01-L5-405 | console.log(marker,visited.join("->")); | T07-P01 출력 관측점; 예상 `T07-P001 RECEIVED->CHECKED`와 비교 |
| T07-P01-LX-490 | T07-P01 실행 기록 | T07-P01 예상→실제→일치→이유 네 칸 저장 |

## CHAPTER 11 · 서버 프로그램과 요청의 첫 진입점 — 한 부분만 수정하고 다시 예측

수정 과제: **stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다**.

수정 전은 `T07-P001 RECEIVED->CHECKED`이다.

새 예상값을 적고 한 곳만 바꾼다.

실행 뒤 차이를 설명한다.

수정 뒤에는 T07-P01/CH02에서 세운 판단 기준이 여전히 유효한지 확인한다. 결과만 맞고 이유가 틀리면 재도전한다.

### 수정·재예측 change table
| case | 변경 단계 | 통과 조건 |
| --- | --- | --- |
| T07-P01-D1-431 | 기준 `T07-P001 RECEIVED->CHECKED` | T07-P01 수정 전 실행을 먼저 재현 |
| T07-P01-D2-432 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | T07-P01 한 위치만 바꾸고 다른 입력은 고정 |
| T07-P01-D3-433 | T07-P01 새 예측 | T07-P01 실행 전에 출력·상태를 먼저 기록 |
| T07-P01-D4-434 | T07-P01 재실행 | T07-P01/CH10 실제값과 새 예측을 대조 |
| T07-P01-D5-435 | T07-P01 반례 | T07-P01/CH06 조건에서도 수정이 안전한지 확인 |
| T07-P01-D6-436 | T07-P01 근거 | T07-P01/CH08 상태가 설명과 일치해야 완료 |
| T07-P01-D7-437 | T07-P01 이유 | T07-P01 변경 이유를 서버 프로그램과 요청의 첫 진입점 계약과 연결해 설명 |

## CHAPTER 12 · 서버 프로그램과 요청의 첫 진입점 — 작은 문제와 오답 이유

| 문제 | 직접 풀 내용 | 정답 핵심 | 왜 틀리는지/채점 |
| --- | --- | --- | --- |
| P1 | 개념 설명: 서버 프로그램과 요청의 첫 진입점 | 서버는 저장된 파일이 아니라 실행 중인 process가 요청을 받아 처리하는 프로그램이다. | 입력·결정·결과가 빠지면 부분점수 |
| P2 | T07-P01/CH02의 첫 판단을 다시 쓰기 | 입력과 확정 사실을 분리 | 현상만 다시 쓰면 오답 |
| P3 | 정상 내부 동작 | listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다. | 단계 순서와 side effect 경계를 설명 |
| P4 | 대표 실패의 원인 | 파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다. | 오류 문구 대신 깨진 계약을 지목 |
| P5 | 확인할 실제 근거 | process 생존 여부, listen 주소와 port, 요청 도착 로그, 응답 완료 시점을 서로 다른 증거로 확인한다. | 두 독립 증거를 연결하면 만점 |
| P6 | 경계 A가 바뀐 경우 | T07-P01/CH04 경계 A | 결과와 판단 근거를 함께 작성 |
| P7 | 경계 B가 바뀐 경우 | T07-P01/CH05 경계 B | 호환·경쟁·수명 중 맞는 축 표시 |
| P8 | 실패 뒤 복구 상태 | T07-P01/CH07 경계 C | durable/memory 또는 완료/미완료 분리 |
| P9 | 운영 한계 | T07-P01/CH09 경계 D | limit·deadline·drain 등 종료 조건 포함 |
| P10 | `서버 프로그램과 요청의 첫 진입점` 실행 코드 수정 | stopAt 값을 1 늘리되 배열 길이를 넘지 않게 바꾼다 | `서버 프로그램과 요청의 첫 진입점` 예상→수정→재실행 기록이 있어야 통과 |

재도전에서는 T07-P01/CH02의 actor·순서·크기 중 하나를 바꾼다. T07-P01/CH08 관측표와 설명이 일치해야 한다.

## CHAPTER 13 · 서버 프로그램과 요청의 첫 진입점 — 사람과 AI의 역할 경계

| 구분 | 이 LESSON의 대상 | 책임 경계 |
| --- | --- | --- |
| 사람이 반드시 이해 | 서버는 저장된 파일이 아니라 실행 중인 process가 요청을 받아 처리하는 프로그램이다. | 업무 의미와 허용 위험 결정 |
| 사람이 직접 판정 | T07-P01/CH06 대표 실패 | 허용 가능한 실패인지 판단 |
| 사람이 증거 확인 | T07-P01/CH08 evidence map | 실제 상태와 설명 대조 |
| AI에 맡길 수 있음 | T07-P01/CH10 실행용 boilerplate·test 후보 | T07-P01: 초안 생성 뒤 실제 실행 필수 |
| AI에 맡기면 안 됨 | 이 LESSON의 완료 기준을 임의 결정 | 요구·법/정책·risk owner가 결정 |
| 자동화가 맡을 수 있음 | `T07-P001 RECEIVED->CHECKED` 같은 deterministic 결과 비교 | 실행하지 않은 값을 PASS로 표시 금지 |

AI 변경은 T07-P01/CH02의 판단 기준과 T07-P01/CH08 evidence map으로 검토한다. 설명만으로 PASS하지 않는다.

### AI 변경 acceptance table
| case | 책임 | 검증 경계 |
| --- | --- | --- |
| T07-P01-AI1-493 | T07-P01 사람 결정 | T07-P01 업무 의미·허용 위험·완료 기준 소유 |
| T07-P01-AI2-494 | T07-P01 AI 초안 | T07-P01/CH10 boilerplate·test 후보까지만 위임 |
| T07-P01-AI3-495 | T07-P01 검증 | T07-P01/CH06 반례와 T07-P01/CH08 근거로 실제 상태 확인 |

## CHAPTER 14 · 서버 프로그램과 요청의 첫 진입점 — 경계 조합 실험 8개

T07-P01: T07-P01/CH04~T07-P01/CH09 중 두 경계를 겹쳐 판정한다.

설명 복사 대신 T07-P01의 충돌 우선순위를 적는다.

| case | 조건 1 | 조건 2 | 새 입력 변화 | 판단할 위험 | 필요 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-K01 | T07-P01: 경계 A | T07-P01: 경계 B | T07-P01: RECURRENCE[occurrence=3; interval_s=80; mitigation_applied=1] / sample=29 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + RECURRENCE_TIMELINE |
| T07-P01-K02 | T07-P01: 경계 A | T07-P01: 경계 C | T07-P01: LARGE_INPUT[body_kb=1000; limit_kb=384; parsed=0] / sample=36 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + SIZE_LIMIT |
| T07-P01-K03 | T07-P01: 경계 A | T07-P01: 경계 D | T07-P01: DRAIN[ready=0; active=9; drain_deadline_s=8] / sample=43 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + DRAIN_STATE |
| T07-P01-K04 | T07-P01: 경계 B | T07-P01: 경계 C | T07-P01: REPLAY[key=cmd-001-04; attempts=3; response_seen=0] / sample=50 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + IDEMPOTENCY_RECORD |
| T07-P01-K05 | T07-P01: 경계 B | T07-P01: 경계 D | T07-P01: OLD_SCHEMA[client=v3; server=v4; extra_field=1] / sample=57 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + CLIENT_VERSION |
| T07-P01-K06 | T07-P01: 경계 C | T07-P01: 경계 D | T07-P01: UNKNOWN_OUTCOME[timeout_ms=205; provider_state=UNKNOWN; lookup_id=p00106] / sample=64 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + PROVIDER_RESULT |
| T07-P01-K07 | T07-P01: 경계 A | T07-P01: 경계 B+C | T07-P01: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] / sample=71 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + DURABLE_STATE |
| T07-P01-K08 | T07-P01: 경계 B | T07-P01: 경계 C+D | T07-P01: OVERLOAD[rps=581; p99_ms=543; queue=25] / sample=78 | T07-P01: 먼저 깨지는 경계를 판정 | T07-P01: T07-P01/CH08 + QUEUE_PRESSURE |

각 행에서 ‘둘 다 정상’이라고 답했다면 T07-P01/CH06 실패 사례와 모순되지 않는지 확인한다.

## CHAPTER 15 · 서버 프로그램과 요청의 첫 진입점 — 인접 개념 5개와 통합

현재 LESSON 전체를 같은 BLOCK의 모든 개념과 반복 비교하지 않는다. T07-P01와 직접 맞닿는 다섯 개 개념만 골라 경계를 확인한다.

| case | 연결 개념 | 새 사건 | 형제 개념의 첫 판단 | 증거 분리 | 충돌 시 기준 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-B02 | T07-P01: route는 요청을 처리할 코드로 연결하는 규칙 | T07-P01: GET /users/me가 /users/:id로 잘못 들어간다 | T07-P01: route table의 method와 specificity를 확인한다 | T07-P01: T07-P01/CH08 증거와 형제 LESSON 증거를 분리 | T07-P01: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P01-B03 | T07-P01: path parameter·query·header·body를 구분하기 | T07-P01: ?tag=a&tag=b와 body가 함께 온다 | T07-P01: 값의 출처를 path/query/header/body로 표시한다 | T07-P01: T07-P01/CH08 증거와 형제 LESSON 증거를 분리 | T07-P01: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P01-B04 | T07-P01: parsing은 문자열을 구조로 읽는 일 | T07-P01: Content-Type은 JSON인데 body 마지막 괄호가 빠졌다 | T07-P01: parsing 실패와 validation 실패를 구분한다 | T07-P01: T07-P01/CH08 증거와 형제 LESSON 증거를 분리 | T07-P01: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P01-B05 | T07-P01: middleware는 공통 단계를 순서대로 연결한다 | T07-P01: auth middleware가 parser보다 먼저/뒤에 놓인다 | T07-P01: middleware 순서를 화살표로 그린다 | T07-P01: T07-P01/CH08 증거와 형제 LESSON 증거를 분리 | T07-P01: 충돌 시 상태 소유자를 먼저 확인 |
| T07-P01-B06 | T07-P01: validation은 외부 입력을 내부 계약으로 바꾸는 문 | T07-P01: age="20", admin=true가 입력으로 온다 | T07-P01: coercion과 허용 field를 따로 결정한다 | T07-P01: T07-P01/CH08 증거와 형제 LESSON 증거를 분리 | T07-P01: 충돌 시 상태 소유자를 먼저 확인 |

## CHAPTER 16 · 서버 프로그램과 요청의 첫 진입점 — 선택형 실패 주입 6개

T07-P01: 실패 축 여섯 개를 선택한다.

한 사례에서는 T07-P01 입력 조건 하나만 바꾼다. 원인을 동시에 섞지 않는다.

| case | 실패 축 | 바꾼 조건 | 구분할 위험 | 기준 판단 | 관측 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-X01 | T07-P01: drain | T07-P01: 배포 중 기존 요청이 처리 중; sample=48 | T07-P01: 새 traffic 차단과 in-flight 처리 | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: readiness·active requests·deadline·final state |
| T07-P01-X02 | T07-P01: 재전송 | T07-P01: 응답 유실 뒤 같은 command가 다시 도착함; sample=65 | T07-P01: 중복 side effect 여부 | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: 처리 식별자·이전 결과·idempotency 기록 |
| T07-P01-X03 | T07-P01: 구버전 client | T07-P01: 한 단계 이전 schema가 요청됨; sample=82 | T07-P01: 호환 입력과 breaking change | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: schema version·실제 client 분포·contract test |
| T07-P01-X04 | T07-P01: unknown outcome | T07-P01: dependency timeout 후 성공 여부 불명; sample=99 | T07-P01: 실패와 미확정 결과 | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: provider id·조회 결과·retry history |
| T07-P01-X05 | T07-P01: 재시작 | T07-P01: side effect 직후 process가 재시작됨; sample=116 | T07-P01: durable state와 memory state | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: commit·outbox·job id·restart 전후 상태 |
| T07-P01-X06 | T07-P01: 과부하 | T07-P01: traffic 세 배, p99 급증; sample=133 | T07-P01: 기능 실패와 saturation | T07-P01: T07-P01/CH02 판단과 별도 기록 | T07-P01: queue age·pool wait·CPU/event-loop·quota |

## CHAPTER 17 · 서버 프로그램과 요청의 첫 진입점 — 오류노트와 재학습 6유형

오류노트에는 정답 문장을 저장하지 않는다. T07-P01에서 실제로 헷갈린 추론 여섯 개를 경계별로 다시 푼다.

| 오답 | 유형 | 재도전 조건 | 잘못된 추론 | 재학습 행동 | 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-E01 | T07-P01: 대표 실패를 원인으로 착각 | T07-P01: T07-P01/CH06 실패 case를 다른 입력으로 재현 | T07-P01: 현상과 원인을 같은 것으로 봄 | T07-P01: T07-P01/CH06 대표 실패와 T07-P01/CH08 증거를 다시 대조 | T07-P01: T07-P01/CH08 |
| T07-P01-E02 | T07-P01: 경계 A 생략 | T07-P01: T07-P01/CH04의 조건 하나를 반대로 설정 | T07-P01: 경계 A 조건을 빼고 기존 결론을 유지함 | T07-P01: T07-P01/CH04를 새 입력에 적용 | T07-P01: T07-P01/CH08 |
| T07-P01-E03 | T07-P01: 경계 B 생략 | T07-P01: T07-P01/CH05의 조건 하나를 반대로 설정 | T07-P01: 경계 B 변화가 계약에 미치는 영향을 무시함 | T07-P01: T07-P01/CH05를 새 입력에 적용 | T07-P01: T07-P01/CH08 |
| T07-P01-E04 | T07-P01: 복구 상태 혼동 | T07-P01: T07-P01/CH07에서 처리 중단을 주입 | T07-P01: 중단 전 memory 상태와 복구 뒤 durable 상태를 같은 것으로 봄 | T07-P01: T07-P01/CH07에서 수명 경계를 다시 표시 | T07-P01: T07-P01/CH08 |
| T07-P01-E05 | T07-P01: 운영 한계 누락 | T07-P01: T07-P01/CH09에서 부하 또는 drain 조건을 변경 | T07-P01: 개발 환경의 성공을 부하·drain 조건까지 일반화함 | T07-P01: T07-P01/CH09의 종료 조건을 다시 작성 | T07-P01: T07-P01/CH08 |
| T07-P01-E06 | T07-P01: 증거 없는 성공 판정 | T07-P01: T07-P01/CH08에서 증거 하나를 숨김 | T07-P01: 관측값을 확인하지 않고 설명만으로 완료라고 판단함 | T07-P01: T07-P01/CH08에서 독립 증거 둘을 선택 | T07-P01: T07-P01/CH08 |

## CHAPTER 18 · 서버 프로그램과 요청의 첫 진입점 — synthetic 관측값 판독 6개

T07-P01: 이 표는 학습용 synthetic 관측값만 담는다.

실제 production 측정으로 표시하지 않는다. T07-P01의 숫자 하나만으로 원인을 단정하지 않고 T07-P01/CH08과 교차한다.

| 관측 | signal | 값 | 뜻 | 함께 볼 조건 | 추가 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-O01 | T07-P01/requests_received | 116 | T07-P01: 요청 수신 수 | T07-P01: 축=재시작; 원인 확정 금지 | T07-P01: DURABLE_STATE + T07-P01/CH08 |
| T07-P01-O02 | T07-P01/handler_started | 133 | T07-P01: handler 진입 수 | T07-P01: 축=과부하; 원인 확정 금지 | T07-P01: QUEUE_PRESSURE + T07-P01/CH08 |
| T07-P01-O03 | T07-P01/responses_completed | 150 | T07-P01: 응답 완료 수 | T07-P01: 축=sampling; 원인 확정 금지 | T07-P01: TRACE_METRIC_CROSSCHECK + T07-P01/CH08 |
| T07-P01-O04 | T07-P01/latency_ms | 167 | T07-P01: 요청 처리 지연 | T07-P01: 축=client disconnect; 원인 확정 금지 | T07-P01: COMMIT_TIMELINE + T07-P01/CH08 |
| T07-P01-O05 | T07-P01/body_kb | 184 | T07-P01: 입력 크기 | T07-P01: 축=재발; 원인 확정 금지 | T07-P01: RECURRENCE_TIMELINE + T07-P01/CH08 |
| T07-P01-O06 | T07-P01/active_connections | 201 | T07-P01: 활성 연결 수 | T07-P01: 축=대형 입력; 원인 확정 금지 | T07-P01: SIZE_LIMIT + T07-P01/CH08 |

## CHAPTER 19 · 서버 프로그램과 요청의 첫 진입점 — 선택형 코드 리뷰 6질문

T07-P01: 코드 줄 수 대신 계약을 리뷰한다.

열두 축 전체를 복제하지 않고 T07-P01에 필요한 여섯 질문만 고른다.

| 리뷰 | 축 | 질문 | 변경된 조건 | 연결 경계 | 채택 전 증거 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-R01 | T07-P01: 동시성 | T07-P01: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P01: OVERLOAD[rps=317; p99_ms=480; queue=10] | T07-P01: T07-P01/CH04 | T07-P01: QUEUE_PRESSURE |
| T07-P01-R02 | T07-P01: 권한 | T07-P01: actor·action·resource가 같은 판단 안에 있는가 | T07-P01: SAMPLING[sample_rate=43%; trace_present=1; metric_present=1] | T07-P01: T07-P01/CH05 | T07-P01: TRACE_METRIC_CROSSCHECK |
| T07-P01-R03 | T07-P01: 자원 | T07-P01: pool·queue·memory·connection 상한이 있는가 | T07-P01: DISCONNECT[disconnect_ms=66; commit_state=UNKNOWN; request=001-02] | T07-P01: T07-P01/CH06 | T07-P01: COMMIT_TIMELINE |
| T07-P01-R04 | T07-P01: 상태 변경 | T07-P01: side effect가 어느 줄에서 확정되는가 | T07-P01: RECURRENCE[occurrence=5; interval_s=102; mitigation_applied=1] | T07-P01: T07-P01/CH07 | T07-P01: RECURRENCE_TIMELINE |
| T07-P01-R05 | T07-P01: 동시성 | T07-P01: 읽기와 쓰기 사이에 다른 actor가 끼어드는가 | T07-P01: LARGE_INPUT[body_kb=1176; limit_kb=384; parsed=0] | T07-P01: T07-P01/CH04 | T07-P01: SIZE_LIMIT |
| T07-P01-R06 | T07-P01: 권한 | T07-P01: actor·action·resource가 같은 판단 안에 있는가 | T07-P01: DRAIN[ready=0; active=1; drain_deadline_s=10] | T07-P01: T07-P01/CH05 | T07-P01: DRAIN_STATE |

## CHAPTER 20 · 서버 프로그램과 요청의 첫 진입점 — 장애 대응 drill 6단계

장애 대응은 restart 버튼부터 누르는 절차가 아니다. T07-P01에서는 증거를 잃지 않는 여섯 단계를 선택해 실행 순서를 적는다.

| 순서 | 단계 | 목적 | 현재 조건 | 확인 증거 | 연습 사건 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-I01 | T07-P01: 변경 동결 | T07-P01: 추가 상태 변화를 만드는 조작을 잠시 멈춤 | T07-P01: SAMPLING[sample_rate=30%; trace_present=0; metric_present=1] | T07-P01: T07-P01/CH08 + TRACE_METRIC_CROSSCHECK | T07-P01-incident-019 |
| T07-P01-I02 | T07-P01: correlation | T07-P01: 한 request/job/resource id를 시간축에 고정 | T07-P01: DISCONNECT[disconnect_ms=53; commit_state=UNKNOWN; request=001-01] | T07-P01: T07-P01/CH08 + COMMIT_TIMELINE | T07-P01-incident-020 |
| T07-P01-I03 | T07-P01: 마지막 정상 | T07-P01: 첫 실패 바로 앞에서 확정된 상태를 기록 | T07-P01: RECURRENCE[occurrence=4; interval_s=91; mitigation_applied=1] | T07-P01: T07-P01/CH08 + RECURRENCE_TIMELINE | T07-P01-incident-021 |
| T07-P01-I04 | T07-P01: 가설 검증 | T07-P01: 원인 후보 하나만 뒤집어 재현 | T07-P01: LARGE_INPUT[body_kb=1088; limit_kb=384; parsed=0] | T07-P01: T07-P01/CH08 + SIZE_LIMIT | T07-P01-incident-022 |
| T07-P01-I05 | T07-P01: 복구 확인 | T07-P01: durable state와 사용자 결과를 모두 확인 | T07-P01: DRAIN[ready=0; active=5; drain_deadline_s=9] | T07-P01: T07-P01/CH08 + DRAIN_STATE | T07-P01-incident-023 |
| T07-P01-I06 | T07-P01: 재주입 | T07-P01: 같은 실패 유형을 다른 입력으로 다시 시험 | T07-P01: REPLAY[key=cmd-001-05; attempts=4; response_seen=0] | T07-P01: T07-P01/CH08 + IDEMPOTENCY_RECORD | T07-P01-incident-024 |

## CHAPTER 21 · 서버 프로그램과 요청의 첫 진입점 — 미니 프로젝트와 채점표

미니 프로젝트 기준 사건은 T07-P01/CH02에 한 번만 정의했다. 여기서는 사건을 다시 복사하지 않고 결과물과 검증 기준을 만든다.

| 평가 | 점수 | 해야 할 일 | 통과 조건 |
| --- | --- | --- | --- |
| 계약 | 15 | T07-P01/CH01에서 입력·성공·실패 경계를 한 문장씩 분리 | 세 문장이 서로 같은 뜻이면 감점 |
| 상태 전이 | 15 | T07-P01/CH03의 M1→M5를 그림으로 다시 구성 | side effect 확정 지점을 표시 |
| 심화 경계 | 15 | T07-P01/CH04~T07-P01/CH09 중 서로 다른 세 경계를 선택 | 세 경계가 같은 실패를 말하면 재작성 |
| 직접 실행 | 15 | T07-P01/CH10 모델을 수정하고 새 예상값을 먼저 작성 | 실행 출력과 예상값을 대조 |
| 실패 주입 | 10 | T07-P01/CH16에서 두 축을 골라 각각 따로 재현 | 원인을 동시에 두 개 바꾸지 않음 |
| 관측 | 10 | T07-P01/CH18 signal 두 개와 T07-P01/CH08 실제 증거 종류를 연결 | synthetic 값과 실제 측정을 혼동하지 않음 |
| 오류노트 | 10 | T07-P01/CH17에서 틀린 추론 하나를 새 조건으로 재도전 | 정답 문장 복사만 하면 통과하지 않음 |
| AI 검토 | 10 | T07-P01/CH19 질문 두 개로 AI 제안 diff를 검토 | 설명 대신 diff·test·evidence를 요구 |

총점 100점보다 중요한 별도 게이트는 직접 실행·실패 주입·증거 연결 세 항목을 실제로 수행했는가이다.

## CHAPTER 22 · 서버 프로그램과 요청의 첫 진입점 — 통합 casebook 16문제

T07-P01 casebook은 경계 A~D를 새 상황에 적용한다.

각 T07-P01 행은 failure axis와 수치 조건을 다르게 준다. 같은 답 암기로 통과하지 못한다.

| case | 기준 경계 | 추가 사건 | 수치 조건 | 검토 질문 | 구분할 결과 | 필요 증거 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P01-C01 | T07-P01: 경계 A | T07-P01: DISCONNECT[disconnect_ms=40; commit_state=UNKNOWN; request=001-00] | T07-P01: load=129, window=27s | T07-P01: review=timeout | T07-P01: 경계 A 위반 여부를 판정 | T07-P01: COMMIT_TIMELINE + T07-P01/CH08 |
| T07-P01-C02 | T07-P01: 경계 B | T07-P01: RECURRENCE[occurrence=3; interval_s=80; mitigation_applied=1] | T07-P01: load=152, window=46s | T07-P01: review=자원 | T07-P01: 경계 B 위반 여부를 판정 | T07-P01: RECURRENCE_TIMELINE + T07-P01/CH08 |
| T07-P01-C03 | T07-P01: 경계 C | T07-P01: LARGE_INPUT[body_kb=1000; limit_kb=384; parsed=0] | T07-P01: load=175, window=65s | T07-P01: review=순서 | T07-P01: 경계 C 위반 여부를 판정 | T07-P01: SIZE_LIMIT + T07-P01/CH08 |
| T07-P01-C04 | T07-P01: 경계 D | T07-P01: DRAIN[ready=0; active=9; drain_deadline_s=8] | T07-P01: load=198, window=84s | T07-P01: review=관측 | T07-P01: 경계 D 위반 여부를 판정 | T07-P01: DRAIN_STATE + T07-P01/CH08 |
| T07-P01-C05 | T07-P01: 경계 A | T07-P01: REPLAY[key=cmd-001-04; attempts=3; response_seen=0] | T07-P01: load=221, window=13s | T07-P01: review=상태 변경 | T07-P01: 경계 A 위반 여부를 판정 | T07-P01: IDEMPOTENCY_RECORD + T07-P01/CH08 |
| T07-P01-C06 | T07-P01: 경계 B | T07-P01: OLD_SCHEMA[client=v3; server=v4; extra_field=1] | T07-P01: load=244, window=32s | T07-P01: review=retry | T07-P01: 경계 B 위반 여부를 판정 | T07-P01: CLIENT_VERSION + T07-P01/CH08 |
| T07-P01-C07 | T07-P01: 경계 C | T07-P01: UNKNOWN_OUTCOME[timeout_ms=205; provider_state=UNKNOWN; lookup_id=p00106] | T07-P01: load=267, window=51s | T07-P01: review=복구 | T07-P01: 경계 C 위반 여부를 판정 | T07-P01: PROVIDER_RESULT + T07-P01/CH08 |
| T07-P01-C08 | T07-P01: 경계 D | T07-P01: RESTART[crash_after_step=4; memory_lost=1; durable_check=pending] | T07-P01: load=290, window=70s | T07-P01: review=동시성 | T07-P01: 경계 D 위반 여부를 판정 | T07-P01: DURABLE_STATE + T07-P01/CH08 |
| T07-P01-C09 | T07-P01: 경계 A | T07-P01: OVERLOAD[rps=581; p99_ms=543; queue=25] | T07-P01: load=313, window=89s | T07-P01: review=민감정보 | T07-P01: 경계 A 위반 여부를 판정 | T07-P01: QUEUE_PRESSURE + T07-P01/CH08 |
| T07-P01-C10 | T07-P01: 경계 B | T07-P01: SAMPLING[sample_rate=50%; trace_present=1; metric_present=1] | T07-P01: load=336, window=18s | T07-P01: review=중복 | T07-P01: 경계 B 위반 여부를 판정 | T07-P01: TRACE_METRIC_CROSSCHECK + T07-P01/CH08 |
| T07-P01-C11 | T07-P01: 경계 C | T07-P01: DISCONNECT[disconnect_ms=73; commit_state=UNKNOWN; request=001-10] | T07-P01: load=359, window=37s | T07-P01: review=권한 | T07-P01: 경계 C 위반 여부를 판정 | T07-P01: COMMIT_TIMELINE + T07-P01/CH08 |
| T07-P01-C12 | T07-P01: 경계 D | T07-P01: RECURRENCE[occurrence=3; interval_s=190; mitigation_applied=1] | T07-P01: load=382, window=56s | T07-P01: review=입력 경계 | T07-P01: 경계 D 위반 여부를 판정 | T07-P01: RECURRENCE_TIMELINE + T07-P01/CH08 |
| T07-P01-C13 | T07-P01: 경계 A | T07-P01: LARGE_INPUT[body_kb=1880; limit_kb=384; parsed=0] | T07-P01: load=405, window=75s | T07-P01: review=timeout | T07-P01: 경계 A 위반 여부를 판정 | T07-P01: SIZE_LIMIT + T07-P01/CH08 |
| T07-P01-C14 | T07-P01: 경계 B | T07-P01: DRAIN[ready=0; active=8; drain_deadline_s=10] | T07-P01: load=428, window=94s | T07-P01: review=자원 | T07-P01: 경계 B 위반 여부를 판정 | T07-P01: DRAIN_STATE + T07-P01/CH08 |
| T07-P01-C15 | T07-P01: 경계 C | T07-P01: REPLAY[key=cmd-001-14; attempts=4; response_seen=0] | T07-P01: load=451, window=23s | T07-P01: review=순서 | T07-P01: 경계 C 위반 여부를 판정 | T07-P01: IDEMPOTENCY_RECORD + T07-P01/CH08 |
| T07-P01-C16 | T07-P01: 경계 D | T07-P01: OLD_SCHEMA[client=v1; server=v2; extra_field=1] | T07-P01: load=474, window=42s | T07-P01: review=관측 | T07-P01: 경계 D 위반 여부를 판정 | T07-P01: CLIENT_VERSION + T07-P01/CH08 |

채점은 결론보다 근거를 본다. T07-P01/CH08와 연결되지 않은 답은 맞는 단어를 써도 미완료다.

## CHAPTER 23 · 서버 프로그램과 요청의 첫 진입점 — evidence 판독 문제 14개

T07-P01 표의 값은 모두 synthetic 연습 데이터다.

앱·운영 서버의 실제 측정값이 아니다. T07-P01 관측만으로 원인을 확정하지 말고 다음 증거를 고른다.

| 문제 | 관측값 | 의심 축 | 아직 말할 수 없는 것 | 다음 증거 | 오답 경고 |
| --- | --- | --- | --- | --- | --- |
| T07-P01-V01 | T07-P01: latency=61ms; queue=7; retry=1 | T07-P01: 재발 | T07-P01: 축=재발; 원인 확정은 보류 | T07-P01: RECURRENCE_TIMELINE + T07-P01/CH08 | T07-P01: 피할 오판=경계 누락 |
| T07-P01-V02 | T07-P01: latency=128ms; queue=18; retry=4 | T07-P01: 대형 입력 | T07-P01: 축=대형 입력; 원인 확정은 보류 | T07-P01: SIZE_LIMIT + T07-P01/CH08 | T07-P01: 피할 오판=증거 혼동 |
| T07-P01-V03 | T07-P01: latency=195ms; queue=29; retry=0 | T07-P01: drain | T07-P01: 축=drain; 원인 확정은 보류 | T07-P01: DRAIN_STATE + T07-P01/CH08 | T07-P01: 피할 오판=동시성 무시 |
| T07-P01-V04 | T07-P01: latency=262ms; queue=40; retry=3 | T07-P01: 재전송 | T07-P01: 축=재전송; 원인 확정은 보류 | T07-P01: IDEMPOTENCY_RECORD + T07-P01/CH08 | T07-P01: 피할 오판=상태 수명 혼동 |
| T07-P01-V05 | T07-P01: latency=329ms; queue=51; retry=6 | T07-P01: 구버전 client | T07-P01: 축=구버전 client; 원인 확정은 보류 | T07-P01: CLIENT_VERSION + T07-P01/CH08 | T07-P01: 피할 오판=운영 한계 누락 |
| T07-P01-V06 | T07-P01: latency=396ms; queue=62; retry=2 | T07-P01: unknown outcome | T07-P01: 축=unknown outcome; 원인 확정은 보류 | T07-P01: PROVIDER_RESULT + T07-P01/CH08 | T07-P01: 피할 오판=오류 합치기 |
| T07-P01-V07 | T07-P01: latency=463ms; queue=73; retry=5 | T07-P01: 재시작 | T07-P01: 축=재시작; 원인 확정은 보류 | T07-P01: DURABLE_STATE + T07-P01/CH08 | T07-P01: 피할 오판=복구 과잉 |
| T07-P01-V08 | T07-P01: latency=530ms; queue=4; retry=1 | T07-P01: 과부하 | T07-P01: 축=과부하; 원인 확정은 보류 | T07-P01: QUEUE_PRESSURE + T07-P01/CH08 | T07-P01: 피할 오판=잘못된 전제 |
| T07-P01-V09 | T07-P01: latency=597ms; queue=15; retry=4 | T07-P01: sampling | T07-P01: 축=sampling; 원인 확정은 보류 | T07-P01: TRACE_METRIC_CROSSCHECK + T07-P01/CH08 | T07-P01: 피할 오판=경계 누락 |
| T07-P01-V10 | T07-P01: latency=664ms; queue=26; retry=0 | T07-P01: client disconnect | T07-P01: 축=client disconnect; 원인 확정은 보류 | T07-P01: COMMIT_TIMELINE + T07-P01/CH08 | T07-P01: 피할 오판=증거 혼동 |
| T07-P01-V11 | T07-P01: latency=731ms; queue=37; retry=3 | T07-P01: 재발 | T07-P01: 축=재발; 원인 확정은 보류 | T07-P01: RECURRENCE_TIMELINE + T07-P01/CH08 | T07-P01: 피할 오판=동시성 무시 |
| T07-P01-V12 | T07-P01: latency=798ms; queue=48; retry=6 | T07-P01: 대형 입력 | T07-P01: 축=대형 입력; 원인 확정은 보류 | T07-P01: SIZE_LIMIT + T07-P01/CH08 | T07-P01: 피할 오판=상태 수명 혼동 |
| T07-P01-V13 | T07-P01: latency=865ms; queue=59; retry=2 | T07-P01: drain | T07-P01: 축=drain; 원인 확정은 보류 | T07-P01: DRAIN_STATE + T07-P01/CH08 | T07-P01: 피할 오판=운영 한계 누락 |
| T07-P01-V14 | T07-P01: latency=932ms; queue=70; retry=5 | T07-P01: 재전송 | T07-P01: 축=재전송; 원인 확정은 보류 | T07-P01: IDEMPOTENCY_RECORD + T07-P01/CH08 | T07-P01: 피할 오판=오류 합치기 |

## CHAPTER 24 · 서버 프로그램과 요청의 첫 진입점 — 설계 변경 검토 8건

설계 변경은 ‘좋아 보인다’로 채택하지 않는다. T07-P01에서는 서로 다른 여덟 개 변경안을 검토하고, 실험이 필요한 변경은 바로 채택/거부로 몰지 않는다.

| 검토 | 변경 종류 | 제안 | 핵심 질문 | 같이 주입할 실패 | 검증 증거 | 결정 |
| --- | --- | --- | --- | --- | --- | --- |
| T07-P01-D01 | T07-P01: retry 추가 | T07-P01: timeout이면 자동 retry를 한 번 더 수행한다 | T07-P01: unknown outcome과 duplicate side effect를 구분하는지 | T07-P01: DRAIN[ready=0; active=4; drain_deadline_s=5] | T07-P01: DRAIN_STATE + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D02 | T07-P01: 로그 확대 | T07-P01: debug를 위해 payload와 context 기록을 늘린다 | T07-P01: secret·PII·cardinality 비용을 통제하는지 | T07-P01: REPLAY[key=cmd-001-01; attempts=3; response_seen=0] | T07-P01: IDEMPOTENCY_RECORD + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D03 | T07-P01: 강제 종료 | T07-P01: 배포 속도를 위해 drain 없이 process를 종료한다 | T07-P01: in-flight request와 background work의 결과를 잃는지 | T07-P01: OLD_SCHEMA[client=v4; server=v5; extra_field=0] | T07-P01: CLIENT_VERSION + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D04 | T07-P01: AI package 추가 | T07-P01: AI가 제안한 새 dependency를 도입한다 | T07-P01: supply-chain·license·runtime·rollback 근거가 있는지 | T07-P01: UNKNOWN_OUTCOME[timeout_ms=172; provider_state=UNKNOWN; lookup_id=p00103] | T07-P01: PROVIDER_RESULT + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D05 | T07-P01: 비동기화 | T07-P01: 응답 전에 하던 일을 background job으로 넘긴다 | T07-P01: durability·status API·worker retry 계약이 생기는지 | T07-P01: RESTART[crash_after_step=1; memory_lost=1; durable_check=pending] | T07-P01: DURABLE_STATE + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D06 | T07-P01: 권한 shortcut | T07-P01: payload의 owner/tenant id를 바로 사용한다 | T07-P01: authenticated identity와 resource ownership을 다시 확인하는지 | T07-P01: OVERLOAD[rps=482; p99_ms=1065; queue=20] | T07-P01: QUEUE_PRESSURE + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D07 | T07-P01: 순서 병렬화 | T07-P01: 순차 단계를 병렬 실행해 latency를 줄인다 | T07-P01: 선후관계 invariant와 race를 깨지 않는지 | T07-P01: SAMPLING[sample_rate=28%; trace_present=0; metric_present=1] | T07-P01: TRACE_METRIC_CROSSCHECK + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |
| T07-P01-D08 | T07-P01: validation 이동 | T07-P01: validation을 business side effect 뒤로 옮긴다 | T07-P01: 잘못된 입력이 상태를 바꾸기 전에 거부되는지 | T07-P01: DISCONNECT[disconnect_ms=34; commit_state=UNKNOWN; request=001-07] | T07-P01: COMMIT_TIMELINE + T07-P01/CH08 | T07-P01: accept/reject/experiment 중 하나를 근거와 함께 선택 |

T07-P01: AI 제안도 같은 검토를 거친다. 실행하지 않은 benchmark·test 결과를 증거로 쓰지 않는다.

## CHAPTER 25 · 서버 프로그램과 요청의 첫 진입점 — 최종 contract와 evidence spine

**최종 계약:** 서버는 저장된 파일이 아니라 실행 중인 process가 요청을 받아 처리하는 프로그램이다.

**정상 메커니즘:** listen 중인 실행 단위가 연결을 받고 요청 객체와 응답 객체를 만든 뒤 handler로 제어를 넘긴다.

**대표 실패:** 파일이 존재한다는 사실을 서버가 살아 있다는 뜻으로 착각하거나, port 연결 실패와 애플리케이션 오류를 섞어 판단한다.

**검증 evidence:** process 생존 여부, listen 주소와 port, 요청 도착 로그, 응답 완료 시점을 서로 다른 증거로 확인한다.

**직접 행동:** 요청 객체를 입력으로 받아 응답 객체를 만드는 작은 순수 함수를 직접 실행한다.

**다음 연결:** `route는 요청을 처리할 코드로 연결하는 규칙`.

`서버 프로그램과 요청의 첫 진입점`에서 넘기는 값은 검증된 상태·실패 의미·evidence다.

| source id | 근거 축 | 이 LESSON에서 쓰는 목적 |
| --- | --- | --- |
| NODE-DOCS | Node.js Documentation | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |
| EXPRESS5 | Express 5 Documentation | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |
| RFC9110 | RFC9110 | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |
| OPENAPI31 | OPENAPI31 | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |
| WEBAPI2 | The Design of Web APIs, Second Edition | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |
| RFC9457 | Problem Details for HTTP APIs | 서버 프로그램과 요청의 첫 진입점의 개념·실패·운영 판단 교차 확인 |

`서버 프로그램과 요청의 첫 진입점` 설명은 출처 문장을 복사하지 않고 계약과 경계를 재구성한다.
