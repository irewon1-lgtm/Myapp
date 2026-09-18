# PART 55 · Memory-mapped I/O — 대용량 파일을 주소 공간과 random access 관점으로 다루기

파일을 처리하는 가장 익숙한 방법은 `read()`로 bytes를 가져오는 것이다. 그러나 수 GB 파일에서 일부 offset만 반복 조회하거나 운영체제의 page cache와 virtual memory를 활용하고 싶다면 memory mapping이 다른 실행 모델을 제공한다. `mmap`은 파일 내용을 전부 Python bytes로 복사해 두는 대신 파일 구간을 process의 virtual address space에 연결한다. 핵심은 **파일 I/O가 사라지는 것이 아니라 언제 page가 memory에 들어오고, mapping과 파일 크기·lifetime이 어떻게 결합되는지**를 이해하는 것이다.

---

## CHAPTER 01 · random access workload는 sequential `read`와 다른 비용 구조를 가진다

### 시작 전 용어집

#### 1. random access

- **뜻:** 일반 file object도 `seek()`와 `read()`로 random access가 가능하므로 mmap이 기능적으로 필수인 것은 아니다.
- **왜 중요한가:** Mmap의 장점은 byte range를 memory-like object로 다루고 operating system page cache와 address translation을 이용해 repeated access를 단순화할 수 있다는 점이다.
- **예시:** 일반 file object도 `seek()`와 `read()`로 random access가 가능하므로 …

#### 2. workload

- **뜻:** Production workload가 warm cache인지 cold scan인지에 맞는 조건에서 측정한다.
- **왜 중요한가:** 로그 파일 전체를 처음부터 끝까지 한 번 읽는다면 buffered sequential I/O가 단순하고 효율적일 수 있다.
- **예시:** Production workload가 warm cache인지 cold scan인지에 맞는 조건에서 …

#### 3. sequential

- **뜻:** 반대로 index 파일에서 offset 1,000, 5,000, 9,000 위치를 자주 조회하거나 binary table의 특정 record만 읽는다면 매번 seek/read를 호출하는 구조와 memory mapping을 비교할 수 있다.
- **왜 중요한가:** 어떤 방식이 좋은지는 파일 크기보다 access pattern이 결정한다.
- **예시:** 반대로 index 파일에서 offset 1,000, 5,000, 9,000 위치를 …

#### 4. read

- **뜻:** 한 번 읽어 OS cache에 올라온 파일을 반복 측정하면 storage I/O가 거의 없어 mmap이든 read든 실제 disk cost를 보지 못할 수 있다.
- **왜 중요한가:** 반면 mapping setup, page fault, address space 사용, file lifetime이 새로운 비용이다.
- **예시:** 한 번 읽어 OS cache에 올라온 파일을 반복 …

Benchmark에서는 hot cache와 cold cache를 구분한다.  

---

## CHAPTER 02 · mmap은 파일 byte range를 virtual address space에 연결한다

### 시작 전 용어집

#### 1. mmap

- **뜻:** Mmap은 I/O를 제거하는 기술이 아니라 **I/O 요청 시점을 memory access와 결합하는 방식**이다.
- **왜 중요한가:** Process가 같은 file을 여러 번 mapping하거나 여러 process가 같은 file-backed page를 읽으면 OS가 physical page를 공유할 수 있다.
- **예시:** Mmap은 I/O를 제거하는 기술이 아니라 **I/O 요청 시점을 …

#### 2. byte range

- **뜻:** Memory mapping을 만들면 process는 특정 virtual address range를 통해 file-backed page에 접근할 수 있다.
- **왜 중요한가:** 처음부터 모든 file byte가 physical memory에 복사되어 있는 것은 아니며 실제 접근에 따라 필요한 page가 fault를 통해 memory에 들어올 수 있다.
- **예시:** Memory mapping을 만들면 process는 특정 virtual address range를 …

#### 3. virtual address

- **뜻:** 다만 virtual address space와 kernel metadata는 사용되고, 접근한 working set이 커지면 page cache와 memory pressure가 증가한다.
- **왜 중요한가:** Random access가 파일 전체에 넓게 퍼져 있으면 결국 많은 page를 읽게 될 수 있다.
- **예시:** 다만 virtual address space와 kernel metadata는 사용되고, 접근한 …

#### 4. space

- **뜻:** 그래서 10GB 파일을 mapping했다고 즉시 10GB resident memory가 필요하다고 단정할 수 없다.
- **왜 중요한가:** 그러나 실제 sharing과 eviction policy는 운영체제 관리 영역이므로 application이 모든 page가 항상 resident하다고 기대하지 않는다.
- **예시:** 그래서 10GB 파일을 mapping했다고 즉시 10GB resident memory가 …

---

## CHAPTER 03 · page fault는 memory access처럼 보이는 코드 뒤에서 storage I/O를 일으킬 수 있다

### 시작 전 용어집

#### 1. page fault

- **뜻:** Mapped region에서 단순 byte indexing을 수행했는데 해당 page가 resident하지 않으면 page fault가 발생하고 operating system이 file data를 읽을 수 있다.
- **왜 중요한가:** Source code에는 explicit `read()`가 없지만 latency spike가 생길 수 있는 이유다.
- **예시:** Mapped region에서 단순 byte indexing을 수행했는데 해당 page가 …

#### 2. memory access

- **뜻:** Profiling에서 CPU instruction만 보지 않고 major/minor fault와 I/O wait를 함께 볼 필요가 있다.
- **왜 중요한가:** Sequential access에서는 OS가 read-ahead를 적용해 다음 page를 미리 가져올 수 있고 random access에서는 이 최적화가 덜 효과적일 수 있다.
- **예시:** Profiling에서 CPU instruction만 보지 않고 major/minor fault와 I/O …

#### 3. storage

- **뜻:** Access pattern에 따라 mmap 성능이 달라진다.
- **왜 중요한가:** 작은 random read가 매우 많을 때 system call 감소 이점이 있을 수 있지만 page fault 자체가 사라지는 것은 아니다.
- **예시:** Access pattern에 따라 mmap 성능이 달라진다.

#### 4. mmap

- **뜻:** Memory pressure가 높으면 이전에 읽은 mapped page가 eviction되어 다음 접근에서 다시 fault가 날 수 있다.
- **왜 중요한가:** “처음 한 번 느리고 그다음은 무조건 빠름”이라는 가정은 working set이 physical memory보다 작을 때만 성립할 가능성이 높다.
- **예시:** Memory pressure가 높으면 이전에 읽은 mapped page가 eviction되어 …

---

## CHAPTER 04 · slice가 view인지 copy인지에 따라 zero-copy 의미가 달라진다

### 시작 전 용어집

#### 1. slice

- **뜻:** Mapped object를 slice했을 때 API가 새로운 bytes copy를 반환하는지 memory view를 제공하는지 확인해야 한다.
- **왜 중요한가:** 큰 100MB 구간을 slice해 bytes로 만들면 mapping을 사용했더라도 Python heap에 100MB copy가 생길 수 있다.
- **예시:** Mapped object를 slice했을 때 API가 새로운 bytes copy를 …

#### 2. view

- **뜻:** Zero-copy 처리가 목적이라면 buffer protocol과 `memoryview` 같은 view abstraction을 조합할 수 있다.
- **왜 중요한가:** View는 복사를 줄이는 대신 원본 mapping lifetime에 의존한다.
- **예시:** Zero-copy 처리가 목적이라면 buffer protocol과 `memoryview` 같은 view …

#### 3. copy

- **뜻:** Mapping을 close한 뒤 view를 사용하면 오류가 발생하거나 사용할 수 없게 된다.
- **왜 중요한가:** Consumer가 view를 오래 보관하면 mapping과 file resource도 그만큼 살아 있어야 한다.
- **예시:** Mapping을 close한 뒤 view를 사용하면 오류가 발생하거나 사용할 …

#### 4. API

- **뜻:** 성능 최적화가 lifetime coupling을 추가한다.
- **왜 중요한가:** Mutable mapping에 writable view를 제공하면 여러 code path가 동일 file-backed memory를 수정할 수 있다.
- **예시:** 성능 최적화가 lifetime coupling을 추가한다.

Read-only parsing이라면 mapping mode와 view를 read-only로 제한해 잘못된 mutation 경로를 줄인다.

---

## CHAPTER 05 · mapping 중 file truncate·growth가 일어나면 size contract가 깨질 수 있다

### 시작 전 용어집

#### 1. mapping

- **뜻:** Mapping은 생성 시점의 file size와 offset range를 기준으로 만들어질 수 있다.
- **왜 중요한가:** 다른 process가 file을 더 작게 truncate하면 기존 mapping의 일부 address가 더 이상 유효한 file backing을 가지지 못해 심각한 오류가 발생할 수 있다.
- **예시:** Mapping은 생성 시점의 file size와 offset range를 기준으로 …

#### 2. file truncate

- **뜻:** 플랫폼에 따라 signal이나 exception, undefined-looking failure로 나타날 수 있다.
- **왜 중요한가:** 동시에 append되는 log file을 mapping해 따라가려면 file growth를 자동으로 mapping이 모두 포함하는지 확인해야 한다.
- **예시:** 플랫폼에 따라 signal이나 exception, undefined-looking failure로 나타날 수 …

#### 3. growth

- **뜻:** 보통 mapping length와 file size 변화에 맞춰 remap이 필요할 수 있다.
- **왜 중요한가:** “파일이 커졌으니 기존 mapping에서도 새 영역이 보인다”는 가정을 하지 않는다.
- **예시:** 보통 mapping length와 file size 변화에 맞춰 remap이 …

#### 4. size contract

- **뜻:** Immutable snapshot file이나 writer가 없는 read-only data는 mmap과 잘 맞는다.
- **왜 중요한가:** Concurrent writer가 size와 content를 자주 변경하는 file은 locking/versioning 또는 다른 access strategy가 필요하다.
- **예시:** Immutable snapshot file이나 writer가 없는 read-only data는 mmap과 …

Mapping contract에는 file mutation policy가 포함된다.

---

## CHAPTER 06 · shared와 copy-on-write mapping은 write visibility가 다르다

### 시작 전 용어집

#### 1. shared

- **뜻:** Writable mapping이 실제 file에 변경을 반영하는 shared mode와 process-private copy-on-write semantics를 가지는 mode가 있을 수 있다.
- **왜 중요한가:** Copy-on-write에서는 처음에는 file page를 공유하다 write 시 private copy가 생겨 다른 process나 underlying file에 같은 방식으로 보이지 않을 수 있다.
- **예시:** Writable mapping이 실제 file에 변경을 반영하는 shared mode와 …

#### 2. copy-on-write mapping

- **뜻:** 이 차이는 단순 performance option이 아니라 state semantics다.
- **왜 중요한가:** File editor처럼 변경을 저장해야 하는 code가 private mapping을 사용하면 수정이 durable file에 반영되지 않을 수 있고, 분석 code가 shared writable mapping을 실수로 사용하면 원본 데이터를 오염시킬 수 있다.
- **예시:** 이 차이는 단순 performance option이 아니라 state semantics다.

#### 3. write visibility

- **뜻:** Flush가 호출되었다고 storage durability가 완전히 보장되는지 역시 별도 문제다.
- **왜 중요한가:** Mapping dirty page와 filesystem flush, fsync 계열 guarantee를 구분한다.
- **예시:** Flush가 호출되었다고 storage durability가 완전히 보장되는지 역시 별도 …

#### 4. process

- **뜻:** 중요 데이터 저장은 storage engine 수준의 atomic/durable protocol을 사용한다.
- **예시:** 중요 데이터 저장은 storage engine 수준의 atomic/durable protocol을 …

---

## CHAPTER 07 · mapping lifetime은 file descriptor와 view object의 lifetime과 함께 관리한다

### 시작 전 용어집

#### 1. mapping lifetime

- **뜻:** 일부 플랫폼에서는 mapping을 만든 뒤 원래 file descriptor를 닫아도 mapping이 유지될 수 있지만 portability와 library semantics를 확인해야 한다.
- **왜 중요한가:** Application에서는 file, mapping, view의 소유권을 한 context manager scope에 묶어 cleanup 순서를 분명히 하는 편이 안전하다.
- **예시:** 일부 플랫폼에서는 mapping을 만든 뒤 원래 file descriptor를 …

#### 2. file descriptor

- **뜻:** View가 mapping을 참조하고 있는데 mapping을 먼저 close하면 cleanup이 실패할 수 있다.
- **왜 중요한가:** LIFO resource management를 사용해 마지막에 만든 view를 먼저 해제하고 mapping, file 순으로 닫을 수 있다.
- **예시:** View가 mapping을 참조하고 있는데 mapping을 먼저 close하면 cleanup이 …

#### 3. view object

- **뜻:** Dynamic view 집합이라면 ExitStack 같은 도구가 도움이 된다.
- **왜 중요한가:** Long-lived global mapping은 빠른 lookup에 유용할 수 있지만 file replacement와 deploy/migration 시점에 old mapping이 계속 old inode/content를 보고 있을 수 있다.
- **예시:** Dynamic view 집합이라면 ExitStack 같은 도구가 도움이 된다.

#### 4. context manager

- **뜻:** Versioned data file을 새로 열고 reader pointer를 교체하는 방식으로 hot reload semantics를 설계할 수 있다.
- **예시:** Versioned data file을 새로 열고 reader pointer를 교체하는 …

---

## CHAPTER 08 · mmap 선택은 `큰 파일`이 아니라 access pattern·copy budget·lifetime으로 결정한다

### 시작 전 용어집

#### 1. mmap

- **뜻:** 파일이 크다는 이유만으로 mmap이 정답은 아니다.
- **왜 중요한가:** Sequential stream 처리라면 buffered iterator가 더 단순하고 memory footprint도 예측하기 쉽다.
- **예시:** 파일이 크다는 이유만으로 mmap이 정답은 아니다.

#### 2. 큰 파일

- **뜻:** Random access가 많고 같은 page를 반복 조회하며 byte range를 view로 처리할 수 있다면 mmap의 장점이 커질 수 있다.
- **왜 중요한가:** 측정할 때 system call 수, page fault, resident memory, end-to-end latency를 본다.
- **예시:** Random access가 많고 같은 page를 반복 조회하며 byte …

#### 3. access pattern

- **뜻:** Mmap version이 Python CPU time은 낮지만 page fault 때문에 tail latency가 커질 수 있고, read version이 copy를 더 하지만 access가 단순해 안정적일 수 있다.
- **왜 중요한가:** Memory-mapped I/O의 핵심은 **파일을 memory처럼 보이게 만드는 문법이 아니라 storage byte와 virtual-memory page의 lifetime을 연결해 random access와 copy 비용을 조절하는 실행 모델을 선택하는 것**이다.
- **예시:** Mmap version이 Python CPU time은 낮지만 page fault …
