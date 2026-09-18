# PART 91 · Atomic file update — 임시 파일·flush·rename·crash recovery를 한 흐름으로 설계하기

설정 파일이나 상태 스냅샷을 갱신할 때 기존 파일을 열어 바로 덮어쓰면 write 중 process가 죽거나 disk error가 나면서 이전 정상본까지 잃을 수 있다. 안전한 파일 갱신은 단순 `write()` 호출이 아니라 **새 내용 작성 → 사용자 공간 buffer flush → 필요한 durability 보장 → 원본과 교체 → directory metadata 지속성 확인**의 흐름으로 본다. Atomicity와 durability는 같은 개념이 아니므로 분리해서 설계해야 한다.

---

## CHAPTER 01 · write-replace model은 기존 정상본을 마지막 순간까지 보존한다

### 시작 전 용어집

#### 1. write-replace model

- **뜻:** 안전한 갱신의 기본 패턴은 원본 파일을 직접 truncate하지 않고 새 임시 파일에 완전한 내용을 먼저 쓰는 것이다.
- **왜 중요한가:** 이 방식의 장점은 새 파일 작성 중 실패해도 기존 `config.
- **예시:** from pathlib import Path / path = Path("config.json")

#### 2. path

- **뜻:** 단, 임시 파일 이름 충돌과 권한·소유권·mode 보존 문제를 별도로 다뤄야 한다.
- **왜 중요한가:** 핵심은 “새 내용이 완전히 준비되기 전에는 기존 정상본의 이름을 건드리지 않는다”는 invariant다.
- **예시:** from pathlib import Path / path = Path("config.json")

```python
from pathlib import Path

path = Path("config.json")
tmp = path.with_suffix(".json.tmp")

tmp.write_text(new_text, encoding="utf-8")
# 검증 후 tmp를 path로 교체한다.
```

json`이 그대로 남는다는 점이다. 


---

**현장 점검 91-1 — CHAPTER 01 · write-replace model은 기존 정상본을 마지막 순간까지 보존한다**
CHAPTER 01 · write-replace model은 기존 정상본을 마지막 순간까지 보존한다을 점검할 때는 복구 전후를 비교할 지표를 정하고 지연을 한 지점에 넣는다. PART 91 CHAPTER 1에서는 처리된 항목과 남은 항목을 구분한다. 한 번의 성공만 확인하지 말고 같은 조건을 다시 실행해 재현성과 복구 가능성을 확인한다. 수정 뒤에는 정상 입력, 경계 입력, 의도적으로 실패시키는 입력을 순서대로 실행하고 결과를 비교한다. 통과 기준은 정상 경로가 기대 결과로 끝나고 실패 경로도 데이터 손실·중복·무한 대기 없이 정해진 오류 또는 복구 상태로 종료되며, 로그만으로 원인과 처리 결과를 추적할 수 있는 것이다.
## CHAPTER 02 · 임시 파일은 같은 filesystem에 두어 rename의 의미를 보존한다

### 시작 전 용어집

#### 1. filesystem

- **뜻:** 원자적 rename은 일반적으로 같은 filesystem 안에서 가장 강한 의미를 가진다.
- **왜 중요한가:** 임시 파일을 `/tmp`처럼 다른 mount에 만든 뒤 target directory로 옮기면 cross-filesystem move가 copy+delete로 바뀌거나 실패할 수 있다.
- **예시:** `tempfile`을 사용할 때도 `dir=target.

#### 2. rename

- **뜻:** 따라서 target 파일과 같은 directory 또는 같은 filesystem에 임시 파일을 만든다.
- **왜 중요한가:** `tempfile`을 사용할 때도 `dir=target.
- **예시:** 따라서 target 파일과 같은 directory 또는 같은 filesystem에 …

#### 3. tar

- **뜻:** 임시 파일 이름에는 충돌 방지와 stale temp cleanup 전략이 필요하다.
- **왜 중요한가:** Process ID만 사용하면 재사용 문제나 동시 writer 충돌이 있을 수 있으므로 안전한 random temporary file API를 활용한다.
- **예시:** 임시 파일 이름에는 충돌 방지와 stale temp cleanup …

parent` 같은 정책을 고려한다.

 

---

## CHAPTER 03 · `flush()`와 `fsync()`는 서로 다른 buffer 층을 다룬다

### 시작 전 용어집

#### 1. flush

- **뜻:** Python file object의 `flush()`는 사용자 공간 buffer의 내용을 OS 쪽으로 밀어내지만, storage device에 영구적으로 기록됐다는 보장은 별개다.
- **왜 중요한가:** Crash durability가 중요한 데이터는 file descriptor에 대한 sync operation까지 고려한다.
- **예시:** with open(tmp, "w", encoding="utf-8") as f: / f.write(new_text)

#### 2. fsync

- **뜻:** 모든 파일에 무조건 `fsync`를 호출하면 latency가 크게 증가할 수 있다.
- **왜 중요한가:** 설정 cache처럼 재생성 가능한 파일과 결제 ledger처럼 손실이 허용되지 않는 파일은 durability 요구가 다르다.
- **예시:** with open(tmp, "w", encoding="utf-8") as f: / f.write(new_text)

#### 3. buffer

- **뜻:** 성능과 durability는 명시적 정책으로 선택한다.
- **예시:** with open(tmp, "w", encoding="utf-8") as f: / f.write(new_text)

```python
with open(tmp, "w", encoding="utf-8") as f:
    f.write(new_text)
    f.flush()
    os.fsync(f.fileno())
```

 


---

## CHAPTER 04 · rename atomicity는 이름 전환의 중간 상태를 줄이지만 모든 실패를 해결하지 않는다

### 시작 전 용어집

#### 1. rename

- **뜻:** 같은 filesystem에서 적절한 replace/rename을 사용하면 reader가 “절반 쓴 target 파일”을 보는 위험을 크게 줄일 수 있다.
- **왜 중요한가:** Reader는 이전 버전 또는 새 버전 중 하나를 보게 만드는 것이 목표다.
- **예시:** 같은 filesystem에서 적절한 replace/rename을 사용하면 reader가 “절반 쓴 …

#### 2. 상태

- **뜻:** 하지만 rename 성공이 storage power-loss 후에도 반드시 남는다는 뜻은 아니다.
- **왜 중요한가:** Filesystem과 mount semantics에 따라 metadata durability가 별도 문제다.
- **예시:** 하지만 rename 성공이 storage power-loss 후에도 반드시 남는다는 …

#### 3. 실패

- **뜻:** 또 Windows와 POSIX의 open-file replacement semantics가 다를 수 있으므로 portable library는 지원 플랫폼에서 실제 behavior를 검증한다.
- **왜 중요한가:** Atomicity라는 단어를 platform-independent 절대 보장처럼 사용하지 않는다.
- **예시:** 또 Windows와 POSIX의 open-file replacement semantics가 다를 수 …

---

## CHAPTER 05 · directory durability는 새 이름 자체가 crash 후 살아남는지와 관련된다

### 시작 전 용어집

#### 1. directory durability

- **뜻:** File content를 sync했더라도 directory entry 변경이 storage에 안정적으로 반영되지 않았다면 sudden power loss 뒤 rename 결과가 사라질 수 있다.
- **왜 중요한가:** 고신뢰 POSIX 계열 code에서는 file sync 뒤 parent directory sync까지 고려할 수 있다.
- **예시:** File content를 sync했더라도 directory entry 변경이 storage에 안정적으로 …

#### 2. crash

- **뜻:** Durability 문서를 쓸 때 “write 성공”, “rename 성공”, “process crash 후 유지”, “power loss 후 유지”를 별도 수준으로 구분한다.
- **왜 중요한가:** 이 수준의 보장은 application 요구와 filesystem에 따라 달라진다.
- **예시:** Durability 문서를 쓸 때 “write 성공”, “rename 성공”, …

#### 3. rename

- **뜻:** 일반 desktop 설정 파일과 database-like metadata file은 필요한 강도가 다르다.
- **예시:** 일반 desktop 설정 파일과 database-like metadata file은 필요한 …

---

## CHAPTER 06 · concurrent writer가 있으면 atomic rename만으로 lost update를 막을 수 없다

### 시작 전 용어집

#### 1. concurrent writer

- **뜻:** 두 process가 같은 old version을 읽고 각각 새 파일을 만든 뒤 순서대로 replace하면 마지막 writer가 앞선 변경을 덮어쓸 수 있다.
- **왜 중요한가:** 각 replace는 atomic해도 논리적 lost update는 발생한다.
- **예시:** 예를 들어 읽을 때 generation을 기록하고 replace 직전 …

#### 2. atomic rename

- **뜻:** 해결책에는 file lock, version number, compare-and-swap 스타일 검증, single-writer architecture가 있다.
- **왜 중요한가:** 예를 들어 읽을 때 generation을 기록하고 replace 직전 target generation이 그대로인지 확인할 수 있다.
- **예시:** 해결책에는 file lock, version number, compare-and-swap 스타일 검증, …

#### 3. lost update

- **뜻:** Atomicity는 “중간 파일이 보이지 않는다”를 해결하고 concurrency consistency는 별도 문제라는 점을 분리한다.
- **예시:** Atomicity는 “중간 파일이 보이지 않는다”를 해결하고 concurrency consistency는 …

---

## CHAPTER 07 · crash recovery는 stale temp와 마지막 성공본을 판별할 규칙이 필요하다

### 시작 전 용어집

#### 1. crash recovery

- **뜻:** Process가 temp write 후 rename 전에 죽으면 임시 파일이 남을 수 있다.
- **왜 중요한가:** Startup에서 무조건 temp를 target으로 승격하면 incomplete write를 정상본으로 오인할 수 있다.
- **예시:** Process가 temp write 후 rename 전에 죽으면 임시 …

#### 2. stale temp

- **뜻:** Recovery policy는 temp가 완전한지 검증할 checksum/version/footer를 둘 수 있고, target이 정상이라면 stale temp를 삭제하는 식으로 정한다.
- **왜 중요한가:** 여러 generation을 보관해 마지막 두 정상본 중 하나를 선택하는 방식도 있다.
- **예시:** Recovery policy는 temp가 완전한지 검증할 checksum/version/footer를 둘 수 …

#### 3. rename

- **뜻:** Recovery code도 destructive하므로 먼저 검증하고 나중에 삭제한다.
- **왜 중요한가:** “temp가 있으면 지운다” 같은 단순 규칙은 evidence가 부족하다.
- **예시:** Recovery code도 destructive하므로 먼저 검증하고 나중에 삭제한다.

---

## CHAPTER 08 · atomic file contract는 atomicity·durability·concurrency를 따로 보장한다

### 시작 전 용어집

#### 1. atomic file

- **뜻:** 파일 갱신 API의 계약은 최소 세 축을 명시해야 한다.
- **왜 중요한가:** Reader가 partial content를 보지 않는 atomicity, crash/power-loss 후 유지 수준인 durability, 여러 writer가 있을 때 update 손실을 막는 concurrency policy다.
- **예시:** 테스트에서는 write 중 예외, fsync 실패, rename 실패, …

#### 2. contract

- **뜻:** 테스트에서는 write 중 예외, fsync 실패, rename 실패, stale temp, 두 writer 경쟁을 각각 주입한다.
- **왜 중요한가:** 정상 저장 한 번 성공만으로 안전한 파일 갱신을 입증할 수 없다.
- **예시:** 테스트에서는 write 중 예외, fsync 실패, rename 실패, …

#### 3. atomicity

- **뜻:** 이 PART의 핵심은 **파일 저장을 `open-write-close`로 축소하지 않고, 이전 정상본을 보존하며 새 generation을 검증·지속·교체하는 crash-aware commit protocol로 이해하는 것**이다.
- **예시:** 이 PART의 핵심은 **파일 저장을 `open-write-close`로 축소하지 않고, …

---

## 실전 학습 루프 · atomic file update

### 1. 쉬운 예

설정 파일을 직접 덮어쓰다가 process가 죽으면 반만 쓴 파일이 남을 수 있다. 새 내용을 같은 filesystem의 임시 파일에 완전히 쓰고 필요한 durability 조치를 한 뒤 rename으로 교체하면 독자에게 이전 또는 새 버전 중 하나를 보여 주기 쉽다.

### 2. 한 줄 해석

atomic update는 write 한 번이 아니라 임시 작성·검증·동기화·원자적 교체의 순서를 설계하는 문제다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
from pathlib import Path

target = Path('settings.txt')
tmp = target.with_suffix('.tmp')
tmp.write_text('version=2', encoding='utf-8')
tmp.replace(target)
```

### 4. 수정 실습

1. rename 전에 validation 실패를 만들고 기존 target이 보존되는지 확인한다.
2. 중요 데이터라면 file fsync와 directory fsync가 각각 왜 필요한지 조사한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

임시 파일을 만들었다는 사실만으로 crash-safe 저장이 완성될까?

### 6. 정답과 오답 설명

**정답:** 아니다. 같은 filesystem 여부, flush/fsync, rename semantics, directory metadata durability까지 요구 수준에 따라 확인한다.

**자주 나오는 오답:** atomic visibility와 power-loss durability를 같은 개념으로 보는 것이 오답이다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · atomic file update

### 증상에서 시작한다

정상 실행에서는 파일이 맞지만 전원 종료나 crash 뒤 빈 파일·이전/새 내용 혼합·파일 소실이 간헐적으로 생긴다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

tmp 파일 존재, write/flush/fsync/rename 순서, parent directory metadata, filesystem 경계를 기록한다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

각 단계 직후 process kill을 가정한 crash matrix를 만들고 재시작 뒤 어떤 버전이 보이는지 확인한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

정책이 요구하는 crash point마다 독자가 이전 완성본 또는 새 완성본 중 허용된 상태만 보게 해야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

