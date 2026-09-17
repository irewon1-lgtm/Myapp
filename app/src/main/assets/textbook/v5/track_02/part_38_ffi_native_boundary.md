# PART 38 · FFI와 native boundary — Python 밖의 ABI·pointer·memory ownership을 안전하게 다루기

Python은 높은 수준의 객체 모델과 자동 메모리 관리를 제공하지만 C library, 운영체제 API, 성능용 native extension과 연결되는 순간 다른 실행 계약을 만나게 된다. 정수 폭, struct layout, pointer lifetime, error code, thread 규칙이 Python 객체 semantics와 다르기 때문이다. Foreign Function Interface(FFI)를 안전하게 다루려면 **Python 값과 native representation 사이의 변환·소유권·실패 경계를 명시적으로 관리**해야 한다.

---

## CHAPTER 01 · FFI는 함수 호출보다 ABI adapter다

Python에서 native 함수를 호출할 때 표면에는 `lib.add(a, b)` 같은 함수 호출이 보일 수 있다. 실제 경계에서는 Python object를 native argument representation으로 변환하고, calling convention에 맞춰 register와 stack에 값을 배치하고, native code가 반환한 결과를 다시 Python object로 바꾼다. 이 과정의 규칙은 API 이름이 아니라 ABI와 FFI adapter가 결정한다.

함수 signature가 잘못 선언되면 native 함수 자체가 정상이어도 argument가 다른 폭이나 다른 위치에서 해석될 수 있다. 64-bit pointer를 32-bit integer로 선언하거나 signedness를 잘못 맞추면 값이 잘리거나 memory access가 틀어질 수 있다. Python의 동적 타입이 native ABI의 고정 layout을 자동 추론해 주는 것은 아니다.

FFI wrapper는 그래서 단순 convenience layer가 아니라 trust boundary다. 가능한 한 native symbol을 application 곳곳에서 직접 호출하지 않고 한 adapter에 signature, conversion, ownership, error translation을 모은다. 상위 code는 Python 수준의 typed contract만 보게 만들면 ABI 세부사항이 퍼지는 범위를 줄일 수 있다.

---

## CHAPTER 02 · Python integer와 fixed-width native integer는 같은 범위를 가지지 않는다

Python의 일반 `int`는 큰 값을 표현할 수 있지만 C의 `int32_t`, `uint32_t`, `size_t` 같은 타입은 폭과 signedness가 고정된다. FFI가 Python integer를 native fixed-width value로 바꿀 때 범위를 초과하면 명시적 오류가 나거나 구현에 따라 잘림·wrap 같은 예상 밖 결과가 생길 수 있다. 외부 contract가 0..65535를 요구한다면 Python 내부에서 값이 표현 가능하다는 사실과 별개로 range validation을 해야 한다.

Signed/unsigned 변환도 중요하다. `-1`을 unsigned field에 넣으면 큰 양수 bit pattern으로 해석되는 native semantics가 있을 수 있다. Sentinel로 `-1`을 쓰는 API라면 Python wrapper에서 의미를 별도 result나 exception으로 바꾸는 편이 안전하다.

`size_t`처럼 platform pointer width와 연결된 타입은 machine에 따라 폭이 달라질 수 있다. 특정 개발 PC에서 `long`과 pointer 크기가 우연히 같았다는 사실을 portable contract로 사용하지 않는다. FFI library가 제공하는 정확한 native type mapping을 사용하고 target architecture에서 검증한다.

---

## CHAPTER 03 · pointer는 주소보다 lifetime과 ownership 계약이 핵심이다

Native pointer를 Python에서 받았다고 해서 그 memory를 언제까지 읽어도 되는지 자동으로 알 수 있는 것은 아니다. Pointer가 library 내부 buffer를 빌려준 것인지, caller가 free해야 하는 새 allocation인지, 다음 함수 호출 전까지만 유효한 temporary view인지 API contract를 확인해야 한다.

Borrowed pointer를 Python object에 오래 저장하면 native owner가 먼저 해제된 뒤 dangling pointer를 읽는 use-after-free가 생길 수 있다. 반대로 caller-owned allocation을 release하지 않으면 Python GC와 별개로 native memory leak이 발생한다. `malloc/free`, library-specific release function, reference-counted handle처럼 해제 규칙을 wrapper에 묶는다.

Pointer와 length를 따로 받는 API에서는 둘의 관계가 invariant다. Length가 실제 allocation보다 크면 out-of-bounds read/write가 되고, byte count와 element count를 혼동하면 element size만큼 범위가 틀어진다. Python sequence처럼 자동 bounds check가 있다고 기대하지 않는다.

가능하면 raw pointer를 application에 노출하지 않고 context manager나 opaque handle object로 감싼다. Handle 객체가 생성·사용·close 상태를 관리하면 double-free와 use-after-close를 Python 수준에서 더 일찍 차단할 수 있다.

---

## CHAPTER 04 · native string boundary는 bytes·encoding·NUL termination을 함께 본다

Python `str`은 Unicode text지만 많은 C API는 `char*`와 byte buffer를 사용한다. 어떤 encoding으로 encode할지, NUL terminator가 필요한지, embedded NUL을 허용하는지, 반환 pointer의 ownership이 누구에게 있는지 정해야 한다. `str.encode()` 한 번으로 모든 string boundary가 해결되는 것은 아니다.

C string API는 첫 NUL byte에서 문자열이 끝난다고 보는 경우가 많다. Python string 안에 `\x00`이 포함된 입력을 단순 encode해 넘기면 native side가 앞부분만 읽어 validation과 실제 사용이 달라질 수 있다. Security-sensitive path나 command argument에서 truncation 문제가 될 수 있다.

반환된 `char*`가 UTF-8이라는 보장도 없다. OS locale encoding, ASCII, binary data일 수 있으므로 API 문서를 기준으로 decode한다. Invalid byte를 replacement character로 바꾸면 identity가 변할 수 있는 값은 strict decode가 더 안전하다.

Text API와 arbitrary byte API를 구분해 wrapper 함수도 `str`과 `bytes`를 명시적으로 나눈다. 상위 호출자가 encoding policy를 추측하지 않게 하는 것이 목적이다.

---

## CHAPTER 05 · struct layout은 field 순서 외에 alignment와 padding을 포함한다

Native struct는 source에 적힌 field를 단순히 붙인 byte sequence가 아닐 수 있다. CPU와 ABI alignment 요구 때문에 field 사이와 struct 끝에 padding이 들어갈 수 있고, compiler option이나 platform ABI에 따라 layout이 달라질 수 있다. Python 쪽 FFI struct 선언은 native header의 field type과 순서뿐 아니라 packing/alignment contract와 맞아야 한다.

Wire format이나 file format을 native in-memory struct와 동일하다고 가정하면 portability 문제가 생긴다. Endianness와 padding이 architecture에 의존하기 때문이다. 외부 format은 명시적 serializer로 정의하고 native struct는 process 내부 ABI representation으로 제한하는 편이 안전하다.

Union은 같은 memory를 여러 type으로 해석하므로 active field를 별도 tag가 결정하는 경우가 많다. Tag와 union field가 불일치하면 Python wrapper가 잘못된 type으로 bytes를 해석할 수 있다. Tagged union contract를 wrapper에서 검증한다.

Native library version이 struct에 field를 추가하면 `sizeof`와 layout이 달라질 수 있다. ABI version이나 struct size field를 확인하고 지원되지 않는 version을 명시적으로 거부한다.

---

## CHAPTER 06 · native error channel을 Python exception으로 번역할 때 원인을 보존한다

C API는 negative return code, `NULL`, errno, 별도 `last_error()`처럼 여러 방식으로 실패를 전달한다. Python wrapper는 성공 결과와 실패 signal을 정확히 구분하고 native error detail을 읽어 의미 있는 exception으로 변환해야 한다. 실패 signal을 정상값과 혼동하면 오류가 훨씬 늦게 드러난다.

Errno 기반 API는 어떤 호출 직후에 errno를 읽어야 하는지 contract가 중요하다. 중간에 다른 native 호출이 들어가면 error state가 바뀔 수 있다. FFI library가 errno preservation option을 제공하는지 확인한다.

모든 native error를 `RuntimeError("failed")` 하나로 바꾸면 상위 code가 retry 가능한 오류와 invalid argument를 구분할 수 없다. Stable error code를 domain exception으로 번역하되 raw native code와 safe message를 cause/context로 남기면 debugging과 abstraction을 동시에 유지할 수 있다.

Native library가 callback을 통해 비동기 오류를 보고한다면 호출 함수 return만 확인해서는 충분하지 않다. Callback lifetime과 error delivery thread까지 포함해 adapter contract를 설계한다.

---

## CHAPTER 07 · GIL과 native thread 규칙은 Python callback 안전성에 영향을 준다

Native extension은 긴 계산이나 blocking I/O 동안 GIL을 해제해 다른 Python thread가 실행되게 할 수 있다. 반대로 native code가 Python C API를 호출하거나 Python object를 조작하려면 적절한 interpreter/thread state 규칙을 따라야 한다. “C 코드니까 GIL과 무관하다”는 가정은 틀리다.

Native library가 자체 worker thread에서 Python callback을 호출한다면 그 thread가 Python runtime에 어떻게 attach되는지 FFI framework가 보장하는지 확인한다. Callback object가 GC되지 않도록 Python side에서 strong reference를 유지해야 하는 경우도 있다. Function pointer만 native에 넘기고 Python reference를 버리면 나중에 native가 이미 사라진 callback을 호출할 수 있다.

GIL을 해제한 native code가 Python-owned mutable buffer를 동시에 수정한다면 data race 가능성도 생긴다. Buffer ownership과 mutation period를 분리하거나 lock/protocol로 보호한다.

CPU parallelism을 위해 native extension을 사용하는 경우 실제로 어느 구간에서 GIL이 해제되는지 profiler와 documentation으로 확인한다. Wrapper만 async/threaded로 만든다고 native library가 thread-safe해지는 것은 아니다.

---

## CHAPTER 08 · native crash는 Python exception 경계를 건너뛸 수 없다

Segmentation fault, illegal instruction, stack corruption처럼 native memory safety 오류는 일반 Python exception으로 복구되지 않고 process 전체를 종료시킬 수 있다. `try/except`로 unsafe native call을 감싼다고 memory corruption을 안전하게 처리할 수 있는 것이 아니다.

신뢰도가 낮은 parser나 third-party native plugin을 다뤄야 한다면 별도 process에서 실행해 crash isolation을 얻는 방법을 고려한다. Parent process는 IPC timeout과 exit status를 관찰하고 worker를 재시작할 수 있다. In-process FFI의 낮은 호출 비용과 process isolation의 안전성 사이 trade-off다.

Native crash dump, signal, core file은 Python traceback과 다른 증거를 제공한다. Symbol과 native stack이 필요할 수 있으므로 release build의 debug symbol 관리와 crash reporting 정책을 준비한다.

Memory corruption은 crash 직전 호출이 실제 원인이 아닐 수도 있다. 이전 out-of-bounds write가 나중에 allocator에서 터질 수 있으므로 sanitizer나 native debugger가 필요하다.

---

## CHAPTER 09 · native boundary 비용은 호출 횟수·변환·복사에서 생긴다

Native code가 빠르다고 해도 Python↔native 경계를 아주 작은 작업마다 반복하면 argument boxing/unboxing, type check, buffer copy, call dispatch 비용이 누적될 수 있다. 원소 하나씩 native 함수 백만 번 호출하는 것보다 큰 batch를 contiguous buffer로 넘겨 한 번에 처리하는 방식이 빠를 수 있다.

Zero-copy view를 사용하면 복사 비용을 줄이지만 lifetime coupling이 생긴다. Native operation이 끝날 때까지 Python buffer가 살아 있어야 하고 writable/read-only contract를 지켜야 한다. 성능 최적화가 ownership 규칙을 추가하는 셈이다.

Benchmark에서는 순수 native kernel 시간만이 아니라 Python wrapper와 conversion을 포함한 end-to-end 시간을 측정한다. 작은 입력에서는 setup overhead가 커서 pure Python이 더 빠를 수도 있다.

FFI 최적화는 call granularity를 조정하는 문제다. 병목이 실제 계산인지 conversion인지 profile한 뒤 batch size와 data representation을 선택한다.

---

## CHAPTER 10 · unsafe boundary는 좁히고 Python contract로 봉인한다

Native integration을 application 전체에 흩뿌리면 pointer, error code, buffer lifetime 규칙을 모든 호출자가 알아야 한다. FFI adapter 한 층에서 raw type과 handle을 받아 validation하고, 상위에는 Pythonic value object와 exception, context manager를 제공하면 unsafe surface를 좁힐 수 있다.

Adapter test는 정상값뿐 아니라 overflow, NULL, invalid UTF-8, short buffer, double close, callback lifetime, native error code를 검증한다. Native crash 가능성이 있는 입력은 별도 process harness에서 실행해 main test runner가 함께 죽지 않게 할 수 있다.

Library version과 ABI compatibility도 startup에서 확인할 수 있다. 지원 version 범위 밖이면 우연히 동작하기를 기대하기보다 명시적으로 실패시켜 corruption 위험을 줄인다.

FFI 설계의 핵심은 native code를 호출하는 법이 아니라 **Python의 안전한 객체 모델과 native ABI 사이의 불일치를 하나의 좁고 검증 가능한 adapter에 가두는 것**이다.