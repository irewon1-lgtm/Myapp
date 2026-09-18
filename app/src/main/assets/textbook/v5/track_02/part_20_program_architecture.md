# PART 20 · 작은 프로그램의 architecture — 요구 계약에서 실행·검증·배포 경계까지 연결하기

Python 문법을 각각 이해해도 실제 기능을 만들 때는 입력, domain rule, I/O, 오류, 동시성, test, packaging이 한 프로그램 안에서 만난다. 구조를 잡는 목적은 파일을 많이 나누는 것이 아니라 변화 이유와 실패 경계를 분리해 한 부분의 수정이 다른 부분을 불필요하게 흔들지 않게 하는 것이다. 여기서는 하나의 “작업 목록 처리기”를 예로 들어 앞에서 만든 실행 모델을 연결한다.

---

## CHAPTER 01 · 기능 요구를 use case와 invariant로 먼저 고정한다

### 시작 전 용어집

#### 1. use case

- **뜻:** Use case는 `add task`, `complete task`, `list tasks`처럼 사용자 의도를 기준으로 나눈다.
- **왜 중요한가:** 각 use case에 입력, 성공 결과, 예상 실패를 정의하면 함수와 module 경계를 정할 근거가 생긴다.
- **예시:** 예제 프로그램은 작업을 등록하고 완료 상태를 바꾸며 JSON …

#### 2. invariant

- **뜻:** 구현을 시작하기 전에 작업은 고유 ID를 가지고 제목은 비어 있을 수 없으며, 완료 시각은 완료 상태에서만 존재한다는 invariant를 정한다.
- **왜 중요한가:** 파일이 깨졌을 때 조용히 빈 목록으로 초기화하지 않고 명시적으로 실패한다는 정책도 포함한다.
- **예시:** 예제 프로그램은 작업을 등록하고 완료 상태를 바꾸며 JSON …

#### 3. 상태

- **뜻:** 예제 프로그램은 작업을 등록하고 완료 상태를 바꾸며 JSON 파일에 저장하고 CLI에서 조회한다고 하자.
- **왜 중요한가:** `complete`는 존재하지 않는 ID에서 `TaskNotFound`를 반환하고 이미 완료된 task에 대해 idempotent하게 유지할지 오류로 볼지 결정한다.
- **예시:** 예제 프로그램은 작업을 등록하고 완료 상태를 바꾸며 JSON …

#### 4. 실패

- **뜻:** 이 단계에서 JSON field 이름이나 argparse option을 먼저 정하지 않는다.
- **왜 중요한가:** 외부 표현은 adapter에서 바뀔 수 있지만 domain invariant는 더 오래 유지될 가능성이 높다.
- **예시:** 이 단계에서 JSON field 이름이나 argparse option을 먼저 …

안정적인 규칙을 중심에 두고 변화 가능성이 큰 I/O를 바깥에 둔다.

테스트도 이 계약에서 바로 나온다. 빈 제목, 중복 ID, 완료 상태와 timestamp 모순, 존재하지 않는 task 등 반례를 먼저 만들면 구현이 지켜야 할 상태 공간이 선명해진다.

---

## CHAPTER 02 · domain model은 I/O 형식을 모르게 만든다

### 시작 전 용어집

#### 1. domain model

- **뜻:** `Task` 객체는 ID, title, completed state, completed_at을 가지고 자신의 invariant를 보호한다.
- **왜 중요한가:** JSON dict를 직접 저장하거나 CLI 문자열을 안다면 persistence와 interface가 domain 안으로 침투한다.
- **예시:** 두 field를 외부에서 따로 설정하게 하면 `completed=True`인데 시간이 …

#### 2. 객체

- **뜻:** Domain constructor는 이미 parse된 값과 domain type만 받도록 한다.
- **왜 중요한가:** `complete(now)` method는 현재 상태를 검사하고 완료 상태와 timestamp를 함께 갱신한다.
- **예시:** 두 field를 외부에서 따로 설정하게 하면 `completed=True`인데 시간이 …

#### 3. dict

- **뜻:** 두 field를 외부에서 따로 설정하게 하면 `completed=True`인데 시간이 없는 중간 상태가 만들어질 수 있다.
- **왜 중요한가:** State transition을 한 operation에 묶어 invalid state 생성 경로를 줄인다.
- **예시:** 두 field를 외부에서 따로 설정하게 하면 `completed=True`인데 시간이 …

#### 4. interface

- **뜻:** Entity equality는 ID를 기준으로 할지 모든 field를 기준으로 할지 정한다.
- **왜 중요한가:** Snapshot 비교가 필요한 test에서는 value representation을 별도로 만들 수 있다.
- **예시:** Entity equality는 ID를 기준으로 할지 모든 field를 기준으로 …

Domain object의 identity와 serialization representation을 같은 개념으로 묶지 않는다.

Domain layer는 filesystem exception이나 argparse object를 모른다. 이 경계를 지키면 같은 규칙을 나중에 web UI나 database storage와 연결해도 재사용할 수 있다.

---

## CHAPTER 03 · repository interface는 storage 요구를 최소 capability로 표현한다

### 시작 전 용어집

#### 1. repository interface

- **뜻:** Use case가 필요한 것은 task를 조회하고 저장하는 기능이지 JSON 파일의 세부 동작이 아니다.
- **왜 중요한가:** `TaskRepository` protocol에 `get`, `add`, `save`, `list`처럼 필요한 capability만 정의하면 service는 concrete storage를 몰라도 된다.
- **예시:** Use case가 필요한 것은 task를 조회하고 저장하는 기능이지 …

#### 2. storage

- **뜻:** Interface를 너무 일반적인 CRUD로 만들지 않는다.
- **왜 중요한가:** Domain operation에 맞는 method 이름과 failure semantics를 정한다.
- **예시:** Interface를 너무 일반적인 CRUD로 만들지 않는다.

#### 3. capability

- **뜻:** `get(id)`가 없으면 None인지 exception인지, `add`가 duplicate ID를 어떻게 처리하는지 명확히 한다.
- **왜 중요한가:** Concrete file repository와 in-memory fake가 같은 contract test를 통과하도록 만들 수 있다.
- **예시:** `get(id)`가 없으면 None인지 exception인지, `add`가 duplicate ID를 어떻게 …

#### 4. protocol

- **뜻:** 나중에 database implementation을 추가한다고 file implementation이 제공하지 않는 의미를 protocol에 무조건 넣으면 구현 사이의 대체 가능성이 깨진다.
- **왜 중요한가:** 작은 프로그램에서도 interface를 실제 변화 축에만 둔다.
- **예시:** 나중에 database implementation을 추가한다고 file implementation이 제공하지 않는 …

Repository가 transaction을 지원하지 않는다면 service가 여러 write를 atomic하게 묶을 수 없다는 제한도 contract다. 

 구현 하나뿐이고 test에서도 대체할 필요가 없는 pure helper까지 protocol로 감싸면 구조가 오히려 복잡해진다.

---

## CHAPTER 04 · file adapter는 parse·schema·atomic write 책임을 소유한다

### 시작 전 용어집

#### 1. file adapter

- **뜻:** JSON repository는 path를 받고 file bytes/text를 읽어 parser와 schema validation을 수행한 뒤 domain object를 복원한다.
- **왜 중요한가:** Domain error와 file corruption을 구분해 호출자에게 전달한다.
- **예시:** File이 아예 없는 초기 상태와 존재하지만 malformed인 상태를 …

#### 2. parse

- **뜻:** File이 아예 없는 초기 상태와 존재하지만 malformed인 상태를 같은 빈 목록으로 취급하지 않는다.
- **왜 중요한가:** 저장에서는 domain object에서 public storage DTO를 만들고 JSON encode한 뒤 temporary file에 기록해 replace한다.
- **예시:** File이 아예 없는 초기 상태와 존재하지만 malformed인 상태를 …

#### 3. schema

- **뜻:** 새 데이터가 완성되기 전에 기존 파일을 직접 truncate하지 않으면 crash 중 partial file 가능성을 줄일 수 있다.
- **왜 중요한가:** 필요한 durability 수준에 따라 flush/sync 정책을 정한다.
- **예시:** 새 데이터가 완성되기 전에 기존 파일을 직접 truncate하지 …

#### 4. atomic write

- **뜻:** Repository가 list 전체를 매번 읽고 쓰는 설계는 데이터가 작을 때는 충분하다.
- **왜 중요한가:** 작업 수가 수십만으로 커지면 performance requirement가 달라져 database나 append log가 필요할 수 있다.
- **예시:** Repository가 list 전체를 매번 읽고 쓰는 설계는 데이터가 …

현재 scale contract를 문서화하면 premature optimization과 뒤늦은 장애를 모두 줄인다.

Adapter test에서는 실제 temporary directory를 사용해 Unicode title, missing file, malformed JSON, atomic replacement 실패 경로를 확인한다. Domain unit test와 다른 failure class다.

---

## CHAPTER 05 · service는 use case orchestration과 dependency 순서를 담당한다

### 시작 전 용어집

#### 1. service

- **뜻:** Service는 parser의 문자열 오류나 filesystem path를 알 필요가 없다.
- **왜 중요한가:** 이미 typed ID를 받고 repository contract와 domain exception을 조합한다.
- **예시:** Service는 parser의 문자열 오류나 filesystem path를 알 필요가 …

#### 2. use case

- **뜻:** Notification 같은 side effect를 추가한다면 저장 성공 후 수행할지, 알림 실패가 use case 전체 실패인지 결정한다.
- **왜 중요한가:** 되돌릴 수 없는 effect는 durable state와 순서를 신중히 배치한다.
- **예시:** Notification 같은 side effect를 추가한다면 저장 성공 후 …

#### 3. orchestration

- **뜻:** complete(id)`는 repository에서 task를 찾고 clock에서 현재 시각을 받아 domain transition을 호출한 뒤 저장한다.
- **왜 중요한가:** now()`와 global path를 사용하지 않으면 fixed clock과 fake repository로 결정적 test를 만들 수 있다.
- **예시:** complete(id)`는 repository에서 task를 찾고 clock에서 현재 시각을 받아 …

#### 4. dependency

- **뜻:** 여러 dependency 사이에서 어떤 operation을 먼저 수행할지와 실패 후 어떤 상태가 남는지를 책임진다.
- **왜 중요한가:** 작은 기능에서도 이 사고를 익히면 결제·메시지·DB를 다루는 큰 시스템으로 확장할 수 있다.
- **예시:** 여러 dependency 사이에서 어떤 operation을 먼저 수행할지와 실패 …

`TaskService. 이 함수가 직접 `datetime.

  

  

Service method가 수십 개 dependency와 flag를 받기 시작하면 use case가 여러 책임을 가진 신호일 수 있다. 변화 이유와 transaction boundary를 기준으로 더 작은 service로 나눈다.

---

## CHAPTER 06 · CLI adapter는 문자열 세계와 typed use case 사이를 번역한다

### 시작 전 용어집

#### 1. CLI adapter

- **뜻:** Argument parser는 `task add "제목"`, `task complete 123`, `task list` 같은 command를 구조화한다.
- **왜 중요한가:** 입력 문자열을 TaskId와 title value로 변환하고 local validation을 수행한 뒤 service를 호출한다.
- **예시:** Argument parser는 `task add "제목"`, `task complete 123`, …

#### 2. typed use

- **뜻:** Service result와 exception은 human message, machine output, exit code로 변환한다.
- **왜 중요한가:** CLI에서 domain object의 `repr`을 그대로 출력하지 않는다.
- **예시:** Service result와 exception은 human message, machine output, exit …

#### 3. case

- **뜻:** Output formatter가 공개할 field와 순서를 결정한다.
- **왜 중요한가:** JSON output mode를 제공한다면 human color/progress와 분리해 automation이 안정적으로 parse할 수 있게 한다.
- **예시:** Output formatter가 공개할 field와 순서를 결정한다.

#### 4. list

- **뜻:** Expected error는 짧고 행동 가능한 stderr message와 stable exit code로 보여 주고 unexpected exception은 diagnostic logging과 별도 code로 처리한다.
- **왜 중요한가:** 사용자가 잘못된 ID를 입력한 것과 저장 파일 corruption을 같은 “실패” 문구로 합치지 않는다.
- **예시:** Expected error는 짧고 행동 가능한 stderr message와 stable …

CLI test는 실제 entry point를 subprocess로 실행해 stdout/stderr/exit code를 확인할 수 있다. 내부 service unit test와 겹치지 않고 process boundary contract를 검증한다.

---

## CHAPTER 07 · dependency composition은 entry point 한 곳에서 수행한다

### 시작 전 용어집

#### 1. dependency

- **뜻:** Production과 test가 다른 service 코드를 사용하는 것이 아니라 dependency만 바뀐다.
- **왜 중요한가:** 이 구조는 dependency injection framework가 없어도 plain Python constructor로 충분히 구현할 수 있다.
- **예시:** Production과 test가 다른 service 코드를 사용하는 것이 아니라 …

#### 2. entry point

- **뜻:** 프로그램 startup은 configuration을 읽고 path를 정하고 FileTaskRepository와 SystemClock, TaskService를 생성한 뒤 CLI handler에 연결한다.
- **왜 중요한가:** 이 조립을 composition root 한 곳에 두면 core module이 concrete implementation을 직접 import하는 범위를 줄일 수 있다.
- **예시:** 프로그램 startup은 configuration을 읽고 path를 정하고 FileTaskRepository와 SystemClock, …

#### 3. configuration

- **뜻:** Configuration validation도 composition root에서 수행한다.
- **왜 중요한가:** Storage path가 잘못됐거나 필요한 environment가 없으면 command 실행 중간이 아니라 startup에 실패한다.
- **예시:** Configuration validation도 composition root에서 수행한다.

#### 4. path

- **뜻:** Test에서는 같은 service에 InMemoryRepository와 FixedClock을 연결한다.
- **왜 중요한가:** 단, `--help`처럼 configuration이 없어도 보여 줘야 하는 command가 있다면 parsing 순서와 lazy construction을 조정한다.
- **예시:** Test에서는 같은 service에 InMemoryRepository와 FixedClock을 연결한다.

Object graph가 커지면 factory function을 나눌 수 있지만 실제 dependency relationship이 보이지 않을 정도로 container magic에 숨기지 않는다. 조립 코드는 다소 명시적이어도 읽을 가치가 있다.

---

## CHAPTER 08 · test pyramid보다 failure-class matrix로 검증층을 배치한다

### 시작 전 용어집

#### 1. test pyramid

- **뜻:** Domain test는 invariant와 transition을 값만으로 빠르게 검증한다.
- **왜 중요한가:** Service test는 dependency interaction과 use case failure를 fake로 검증한다.
- **예시:** Domain test는 invariant와 transition을 값만으로 빠르게 검증한다.

#### 2. failure-class matrix

- **뜻:** Repository integration은 실제 file parsing과 atomic write를 확인하고 CLI end-to-end는 process contract를 확인한다.
- **왜 중요한가:** 같은 `add task` 성공 사례를 모든 층에서 수십 번 반복할 필요는 없다.
- **예시:** Repository integration은 실제 file parsing과 atomic write를 확인하고 …

#### 3. 검증

- **뜻:** Fault injection으로 write 중 실패해도 기존 파일이 유지되는지 검증할 수 있다.
- **왜 중요한가:** Static type checker는 repository protocol과 return type 불일치를 실행 전에 잡는다.
- **예시:** Fault injection으로 write 중 실패해도 기존 파일이 유지되는지 …

#### 4. dependency

- **뜻:** Unit에서는 경계값을 깊게, integration에서는 format과 resource failure를, CLI에서는 representative command와 exit code를 선택한다.
- **왜 중요한가:** Property test로 serialization round trip과 ID uniqueness invariant를 넓은 입력에서 확인할 수 있다.
- **예시:** Unit에서는 경계값을 깊게, integration에서는 format과 resource failure를, CLI에서는 …

각각 다른 종류의 오류를 잡는다.

  중복보다 failure coverage를 높인다.

  

검증 결과를 “전체 PASS” 한 단어로 뭉치지 않고 어떤 failure class를 실제 실행해 확인했는지 기록한다. 검증하지 않은 Android runtime이나 운영 filesystem까지 성공했다고 확대 해석하지 않는다.

---

## CHAPTER 09 · 성능 요구가 바뀌면 architecture의 병목 경계만 교체한다

### 시작 전 용어집

#### 1. architecture

- **뜻:** Architecture는 미래의 모든 변화에 대비하는 추상화가 아니라 현재 확인된 변화 축을 격리해 **필요한 부분만 교체 가능하게 만드는 구조**다.
- **왜 중요한가:** 작업 100개에서 전체 JSON rewrite는 충분히 빠를 수 있지만 100만 개에서 매 command마다 전체 parse/write는 부적합하다.
- **예시:** Architecture는 미래의 모든 변화에 대비하는 추상화가 아니라 현재 …

#### 2. 경계

- **뜻:** 측정 결과 storage adapter가 bottleneck이면 domain과 CLI를 유지하면서 SQLite repository로 바꿀 수 있다.
- **왜 중요한가:** Database로 바꾸면 새로운 transaction, schema migration, concurrent writer 문제가 생긴다.
- **예시:** 측정 결과 storage adapter가 bottleneck이면 domain과 CLI를 유지하면서 …

#### 3. 함수

- **뜻:** 처음부터 모든 함수를 async로 만들 이유는 없다.
- **왜 중요한가:** 기존 file repository보다 “더 고급”이라서 좋은 것이 아니라 새로운 scale contract에 맞기 때문에 선택한다.
- **예시:** 처음부터 모든 함수를 async로 만들 이유는 없다.

이것이 경계 분리의 실용적 가치다.

  데이터가 작은데 DB operation complexity만 늘어나면 오히려 비용이다.

Async가 필요해지는 경우도 마찬가지다. Local file CLI는 sync model이 단순하지만 remote service를 동시에 조회해야 하는 기능이 추가되면 adapter와 service 일부를 async boundary로 재설계할 수 있다. 


---

## CHAPTER 10 · 완성된 프로그램은 코드보다 계약 그래프로 설명할 수 있어야 한다

### 시작 전 용어집

#### 1. 계약

- **뜻:** 프로그래밍 사고의 핵심은 문법을 많이 아는 것이 아니라 **값·상태·의존성·실패·resource의 경계를 명확히 정하고 그 계약을 코드와 테스트로 유지하는 것**이다.
- **왜 중요한가:** 이후 더 큰 웹·DB·분산 시스템을 배울 때도 이 모델이 그대로 기반이 된다.
- **예시:** 프로그래밍 사고의 핵심은 문법을 많이 아는 것이 아니라 …

#### 2. resource

- **뜻:** CLI가 raw string을 parse해 typed input을 만들고, service가 use case를 orchestration하며, Task가 invariant를 보호하고, repository가 storage format과 resource lifetime을 책임지고, entry point가 dependency를 조립한다.
- **왜 중요한가:** CLI parse error, domain invalid transition, task not found, corrupted storage, unexpected system error가 서로 다른 층에서 발생하고 상위로 번역된다.
- **예시:** CLI가 raw string을 parse해 typed input을 만들고, service가 …

#### 3. dependency

- **뜻:** 작업 목록 프로그램을 설명할 때 파일 이름 목록보다 흐름을 말할 수 있어야 한다.
- **왜 중요한가:** 이 구조가 작은 프로그램에서 과한지 여부는 파일 수가 아니라 변화 비용으로 판단한다.
- **예시:** 작업 목록 프로그램을 설명할 때 파일 이름 목록보다 …

#### 4. 경계

- **뜻:** 기능 세 개뿐인데 interface 열 개와 factory 스무 개가 필요하면 과도하다.
- **왜 중요한가:** 반대로 돈·권한·영구 데이터가 한 함수에 뒤섞여 있다면 줄 수가 적어도 위험하다.
- **예시:** 기능 세 개뿐인데 interface 열 개와 factory 스무 …

각 경계에는 실패 vocabulary가 있다.  Test도 이 경계에 맞춰 배치된다.
