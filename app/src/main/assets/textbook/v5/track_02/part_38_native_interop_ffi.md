# PART 38 · Native interoperability와 FFI — Python 객체 경계를 C ABI·pointer·lifetime으로 넘기기

Python만으로 충분한 프로그램도 많지만 성능 library, operating-system API, 기존 C/C++ code와 연결하면 native boundary를 만나게 된다. 이 경계에서는 garbage-collected object model 밖에 raw pointer, fixed-width integer, ABI, manual resource lifetime이 등장한다. 잘못된 타입 하나가 Python exception이 아니라 process crash나 memory corruption으로 이어질 수 있다. 핵심은 **언어 경계를 넘는 모든 값의 layout·ownership·error semantics를 명시하는 것**이다.

---

## CHAPTER 01 · FFI는 Python call을 native ABI call로 변환하는 adapter다

### 시작 전 용어집

#### 1. FFI

- **뜻:** FFI wrapper는 application 전체에 raw native call을 노출하기보다 typed Python interface로 감싸고 range validation과 error translation을 한 곳에서 수행한다.
- **왜 중요한가:** Foreign Function Interface는 Python value를 native function이 기대하는 argument representation으로 바꾸고 return 값을 다시 Python object로 변환한다.
- **예시:** FFI wrapper는 application 전체에 raw native call을 노출하기보다 …

#### 2. Python call

- **뜻:** C function이 `uint32_t`, pointer, null-terminated string을 기대하면 Python의 arbitrary precision int와 Unicode str을 그대로 넘길 수 없다.
- **왜 중요한가:** Boundary declaration에서 argument type과 return type을 정확히 지정하지 않으면 pointer width나 signedness가 잘못 해석될 수 있다.
- **예시:** C function이 `uint32_t`, pointer, null-terminated string을 기대하면 Python의 …

#### 3. native ABI

- **뜻:** 작은 test 값에서는 우연히 맞다가 64-bit address와 큰 integer에서 깨질 수 있다.
- **왜 중요한가:** Native library function 이름과 symbol resolution도 deployment contract다.
- **예시:** 작은 test 값에서는 우연히 맞다가 64-bit address와 큰 …

#### 4. adapter

- **뜻:** Dynamic library version이 바뀌며 symbol이 없어지거나 signature가 바뀌면 import/load 시점 또는 call 시점에 실패할 수 있다.
- **예시:** Dynamic library version이 바뀌며 symbol이 없어지거나 signature가 바뀌면 …

---

## CHAPTER 02 · fixed-width integer는 Python int와 범위 계약이 다르다

### 시작 전 용어집

#### 1. fixed-width integer

- **뜻:** Python int는 일반적으로 큰 값을 표현할 수 있지만 C의 `int32_t`, `uint64_t`는 고정 폭 범위를 가진다.
- **왜 중요한가:** Python 값이 범위를 넘으면 wrapper가 exception을 발생시키거나 잘못된 cast가 truncation/wrap을 만들 수 있다.
- **예시:** Python int는 일반적으로 큰 값을 표현할 수 있지만 …

#### 2. Python int

- **뜻:** 함수 호출 전에 explicit range를 검증한다.
- **왜 중요한가:** `-1`을 unsigned field에 전달했을 때 native layer가 최대값으로 reinterpret하면 sentinel 의미일 수도 있고 bug일 수도 있다.
- **예시:** 함수 호출 전에 explicit range를 검증한다.

#### 3. 계약

- **뜻:** Protocol/API specification을 따른다.
- **왜 중요한가:** `size_t`처럼 platform width에 따라 달라질 수 있는 type을 임의의 32-bit int로 선언하지 않는다.
- **예시:** Protocol/API specification을 따른다.

#### 4. exception

- **뜻:** Return code가 음수 error와 양수 count를 겸하는 C API에서는 Python wrapper가 domain result와 exception으로 의미를 분리할 수 있다.
- **왜 중요한가:** Raw integer code가 application 안쪽으로 퍼지지 않게 한다.
- **예시:** Return code가 음수 error와 양수 count를 겸하는 C …

Signedness도 중요하다.  

 Target ABI에 맞는 FFI type을 사용한다.

 

---

## CHAPTER 03 · pointer는 주소일 뿐 object lifetime을 자동으로 소유하지 않는다

### 시작 전 용어집

#### 1. pointer

- **뜻:** Native function이 Python-owned buffer의 pointer를 보관하면 함수 호출이 끝난 뒤 Python object가 수집되어 pointer가 dangling될 수 있다.
- **왜 중요한가:** 반대로 native library가 allocate한 memory를 Python이 release 책임을 가져야 할 수도 있다.
- **예시:** Native function이 Python-owned buffer의 pointer를 보관하면 함수 호출이 …

#### 2. object lifetime

- **뜻:** Pointer마다 owner와 valid lifetime을 정해야 한다.
- **왜 중요한가:** “Pointer를 받았으니 나중에 free”라는 규칙도 allocator가 맞아야 한다.
- **예시:** Pointer마다 owner와 valid lifetime을 정해야 한다.

#### 3. buffer

- **뜻:** Library A의 allocator로 만든 memory를 다른 allocator/free function에 넘기면 undefined behavior가 생길 수 있다.
- **왜 중요한가:** 생성한 library가 제공하는 release API를 사용한다.
- **예시:** Library A의 allocator로 만든 memory를 다른 allocator/free function에 …

#### 4. 함수

- **뜻:** Borrowed pointer와 owned pointer를 wrapper type 이름이나 documentation에서 구분한다.
- **왜 중요한가:** Borrowed view를 장기 저장하지 않고 필요한 경우 copy한다.
- **예시:** Borrowed pointer와 owned pointer를 wrapper type 이름이나 documentation에서 …

Callback이 Python object를 native code에 등록하면 native registry가 callback을 사용하는 동안 Python reference를 유지해야 한다. 그렇지 않으면 callback function object가 수집된 뒤 native code가 invalid address를 호출할 수 있다.

---

## CHAPTER 04 · string boundary는 Unicode와 byte encoding을 다시 분리한다

### 시작 전 용어집

#### 1. string boundary

- **뜻:** C API가 `char*`를 받는다고 그것이 UTF-8 text라는 뜻은 아니다.
- **왜 중요한가:** File path, locale-dependent text, arbitrary binary buffer일 수 있다.
- **예시:** C API가 `char*`를 받는다고 그것이 UTF-8 text라는 뜻은 …

#### 2. Unicode

- **뜻:** Protocol이 요구하는 encoding과 null termination을 확인한다.
- **왜 중요한가:** Python str을 UTF-8 bytes로 encode한 temporary object의 pointer를 native code가 함수 이후에도 보관하면 lifetime 문제가 생긴다.
- **예시:** Protocol이 요구하는 encoding과 null termination을 확인한다.

#### 3. byte encoding

- **뜻:** Library가 input을 copy하는지 reference를 유지하는지 contract가 필요하다.
- **왜 중요한가:** Native output `char*`에도 length가 별도로 있는지 null terminator를 찾는지 확인한다.
- **예시:** Library가 input을 copy하는지 reference를 유지하는지 contract가 필요하다.

#### 4. API

- **뜻:** Arbitrary binary data에 null-terminated string API를 사용하면 중간 zero byte에서 data가 잘릴 수 있다.
- **왜 중요한가:** Invalid native bytes를 Python str로 decode할 때 error policy를 정한다.
- **예시:** Arbitrary binary data에 null-terminated string API를 사용하면 중간 …

OS/API identifier가 arbitrary bytes를 허용하는데 replacement decode를 사용하면 두 다른 raw name이 같은 text처럼 보일 수 있다.

---

## CHAPTER 05 · struct layout은 field 순서 외에 padding과 alignment를 포함한다

### 시작 전 용어집

#### 1. struct layout

- **뜻:** C struct는 field 사이에 alignment padding이 들어갈 수 있고 ABI에 따라 전체 size와 offset이 달라진다.
- **왜 중요한가:** Python FFI struct declaration이 field 이름만 같다고 layout이 자동으로 동일한 것은 아니다.
- **예시:** C struct는 field 사이에 alignment padding이 들어갈 수 …

#### 2. field

- **뜻:** Version이 다른 native library가 struct 끝에 field를 추가하는 경우 size/version field로 compatibility를 관리할 수 있다.
- **왜 중요한가:** Wrapper가 오래된 size로 new library를 호출해도 안전한지 공식 API contract를 따른다.
- **예시:** Version이 다른 native library가 struct 끝에 field를 추가하는 …

#### 3. padding

- **뜻:** Packing rule과 platform ABI를 확인한다.
- **왜 중요한가:** Network/file packed struct와 in-memory native struct도 분리한다.
- **예시:** Packing rule과 platform ABI를 확인한다.

#### 4. alignment

- **뜻:** Compiler-specific packing directive를 wire format으로 노출하면 interoperability가 깨지기 쉽다.
- **왜 중요한가:** External schema는 fixed offset/endian을 명시하고 native object와 변환한다.
- **예시:** Compiler-specific packing directive를 wire format으로 노출하면 interoperability가 깨지기 …

Offset과 size는 test에서 native `sizeof` reference와 비교할 수 있다. 여러 target architecture가 지원 대상이라면 32/64-bit CI matrix로 검증한다.

---

## CHAPTER 06 · native error는 return code·errno·exception·callback 등 여러 channel을 가진다

### 시작 전 용어집

#### 1. native error

- **뜻:** C API는 `-1`을 반환하고 `errno`를 설정하거나 null pointer로 실패를 알릴 수 있다.
- **왜 중요한가:** Windows API처럼 별도 thread-local error code를 읽어야 할 수도 있다.
- **예시:** C API는 `-1`을 반환하고 `errno`를 설정하거나 null pointer로 …

#### 2. return code

- **뜻:** Native library 자체 error object를 제공하는 경우도 있다.
- **왜 중요한가:** Wrapper는 정확한 시점에 error channel을 capture해야 한다.
- **예시:** Native library 자체 error object를 제공하는 경우도 있다.

#### 3. errno

- **뜻:** “나중에 errno를 읽으면 되겠지”라고 미루지 않는다.
- **왜 중요한가:** Python wrapper는 low-level code를 domain exception으로 번역하면서 original numeric code와 safe context를 보존한다.
- **예시:** “나중에 errno를 읽으면 되겠지”라고 미루지 않는다.

#### 4. exception

- **뜻:** Callback 안에서 Python exception이 발생했을 때 native caller로 어떻게 전달되는지도 명확해야 한다.
- **왜 중요한가:** FFI library가 exception을 log만 하고 무시할 수 있으므로 callback result/error protocol을 설계한다.
- **예시:** Callback 안에서 Python exception이 발생했을 때 native caller로 …

성공 함수 호출 사이에 다른 native call이 끼면 thread-local error value가 덮어써질 수 있다. 

 Caller가 raw errno table을 알아야 하는 구조를 피한다.

 

---

## CHAPTER 07 · GIL과 native call은 thread parallelism behavior에 영향을 준다

### 시작 전 용어집

#### 1. GIL

- **뜻:** Native extension이 긴 계산 동안 GIL을 release하면 다른 Python thread가 실행될 수 있고, release하지 않으면 event loop/thread가 오래 막힐 수 있다.
- **왜 중요한가:** Library documentation과 profiler로 실제 behavior를 확인한다.
- **예시:** Native extension이 긴 계산 동안 GIL을 release하면 다른 …

#### 2. native call

- **뜻:** Python callback을 native worker thread에서 호출하면 interpreter state와 GIL을 올바르게 획득해야 한다.
- **왜 중요한가:** 고수준 FFI library가 이를 처리하는지 확인하고 native thread가 Python runtime 종료 후 callback하지 않게 lifecycle을 관리한다.
- **예시:** Python callback을 native worker thread에서 호출하면 interpreter state와 …

#### 3. thread

- **뜻:** Native library가 내부 thread pool을 사용하는데 application도 thread/process pool을 늘리면 oversubscription이 발생할 수 있다.
- **왜 중요한가:** BLAS와 numerical library가 대표적이다.
- **예시:** Native library가 내부 thread pool을 사용하는데 application도 thread/process …

#### 4. behavior

- **뜻:** Worker 수를 environment/config로 조정하고 CPU utilization을 측정한다.
- **왜 중요한가:** Thread-safe native API인지도 별도 contract다.
- **예시:** Worker 수를 environment/config로 조정하고 CPU utilization을 측정한다.

Python lock이 없어도 library 내부가 safe할 수 있고, 반대로 Python에서 동시에 호출하면 안 되는 handle이 있을 수 있다.

---

## CHAPTER 08 · crash와 memory corruption은 Python exception boundary 밖에서 발생할 수 있다

### 시작 전 용어집

#### 1. crash

- **뜻:** Fuzzing으로 malformed input을 native boundary에 넣어 crash를 탐색할 수 있다.
- **왜 중요한가:** Crash dump와 core file이 필요한 환경에서는 symbol/debug info와 build ID를 artifact에 연결한다.
- **예시:** Fuzzing으로 malformed input을 native boundary에 넣어 crash를 탐색할 …

#### 2. memory corruption

- **뜻:** Native code가 invalid pointer를 dereference하거나 buffer bounds를 넘으면 interpreter process 전체가 segmentation fault로 종료될 수 있다.
- **왜 중요한가:** `try/except`로 복구할 수 없는 failure class다.
- **예시:** Native code가 invalid pointer를 dereference하거나 buffer bounds를 넘으면 …

#### 3. Python exception

- **뜻:** 따라서 untrusted input을 native parser에 넘길 때 library hardening과 process isolation이 중요하다.
- **왜 중요한가:** Memory sanitizer, address sanitizer 같은 native tooling은 extension 개발에서 Python unit test와 다른 종류의 bug를 찾는다.
- **예시:** 따라서 untrusted input을 native parser에 넘길 때 library …

#### 4. boundary

- **뜻:** Python traceback만으로 native stack corruption을 분석할 수 없을 수 있다.
- **왜 중요한가:** Risk가 큰 third-party native plugin을 main process와 같은 address space에 로드하지 않고 separate worker process로 격리하면 crash blast radius를 줄일 수 있다.
- **예시:** Python traceback만으로 native stack corruption을 분석할 수 없을 …

---

## CHAPTER 09 · native performance는 call overhead와 data copy를 함께 측정한다

### 시작 전 용어집

#### 1. native performance

- **뜻:** C 함수 자체가 매우 빨라도 Python↔native boundary를 수백만 번 넘으면 argument conversion과 call overhead가 dominant할 수 있다.
- **왜 중요한가:** Element 하나씩 native function을 호출하기보다 array/batch 단위로 넘기는 API가 더 효율적일 수 있다.
- **예시:** C 함수 자체가 매우 빨라도 Python↔native boundary를 수백만 …

#### 2. call overhead

- **뜻:** Large array를 native library에 전달할 때 contiguous memory와 dtype이 맞으면 zero-copy가 가능할 수 있고 그렇지 않으면 hidden conversion/copy가 발생한다.
- **왜 중요한가:** Profiler와 memory measurement로 실제 copy를 확인한다.
- **예시:** Large array를 native library에 전달할 때 contiguous memory와 …

#### 3. data copy

- **뜻:** Native code가 빠르다는 이유로 algorithmic complexity가 사라지는 것은 아니다.
- **왜 중요한가:** O(n²) C loop는 n이 충분히 커지면 O(n log n) Python/native combination보다 느릴 수 있다.
- **예시:** Native code가 빠르다는 이유로 algorithmic complexity가 사라지는 것은 …

#### 4. 함수

- **뜻:** Benchmark는 Python wrapper 포함 end-to-end로 수행한다.
- **왜 중요한가:** Native kernel time만 측정하면 real application의 conversion cost를 놓친다.
- **예시:** Benchmark는 Python wrapper 포함 end-to-end로 수행한다.

---

## CHAPTER 10 · FFI boundary를 좁히면 unsafe 영역과 Python 안전 영역을 분리할 수 있다

### 시작 전 용어집

#### 1. FFI

- **뜻:** Mock만으로 FFI layout과 ABI correctness를 검증할 수 없다.
- **왜 중요한가:** Native interoperability의 핵심은 **성능을 위해 C를 호출하는 기술보다 서로 다른 memory·type·error model이 만나는 경계에서 layout·ownership·lifetime을 명시하고 unsafe 범위를 최소화하는 것**이다.
- **예시:** Mock만으로 FFI layout과 ABI correctness를 검증할 수 없다.

#### 2. FFI boundary

- **뜻:** Application core가 pointer와 errno, struct packing을 직접 다루지 않게 작은 native adapter module에 모은다.
- **왜 중요한가:** Adapter는 range와 buffer size를 검증하고 ownership을 명시하며 native error를 Python exception/domain result로 변환한다.
- **예시:** Application core가 pointer와 errno, struct packing을 직접 다루지 …

#### 3. unsafe

- **뜻:** Adapter 바깥에서는 ordinary Python type과 immutable value를 사용하면 unsafe assumptions가 퍼지지 않는다.
- **왜 중요한가:** Native library version과 ABI check도 startup boundary에 둔다.
- **예시:** Adapter 바깥에서는 ordinary Python type과 immutable value를 사용하면 …

#### 4. Python

- **뜻:** Test는 Python contract test, real native integration test, malformed input/fuzz, process crash isolation처럼 failure class를 분리한다.
- **예시:** Test는 Python contract test, real native integration test, …
