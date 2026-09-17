# PART 14 · File·path·stream — bytes와 text, 경로와 lifetime을 정확히 다루기

파일 입출력은 `open()`과 `read()` 몇 줄로 시작할 수 있지만 실제 프로그램에서는 encoding, buffering, partial read, path semantics, atomic replacement, permission, resource lifetime이 함께 작동한다. 파일을 단순히 “디스크에 저장된 문자열”로 생각하면 운영체제 경계에서 데이터 손상과 환경 의존 문제가 생긴다. 핵심은 **저장된 bytes와 프로그램이 해석하는 text, 그리고 파일 이름을 찾는 namespace를 분리하는 것**이다.

---

## CHAPTER 01 · file은 bytes의 sequence이고 text mode는 decoding layer를 추가한다

디스크의 일반 파일은 bytes를 저장한다. Python에서 text mode로 파일을 열면 I/O layer가 bytes를 특정 encoding으로 decode해 `str`을 제공하고, 쓰기에서는 `str`을 encode해 bytes로 변환한다. 따라서 text 파일을 읽는다는 말에는 실제로 `storage bytes → decoder → Unicode text`라는 경계가 포함된다.

Encoding을 생략하고 platform default에 의존하면 같은 파일이 machine에 따라 다르게 해석될 수 있다. 프로그램의 data format이라면 UTF-8처럼 명시적 encoding을 계약으로 정하는 편이 재현성이 높다. Legacy file을 읽는 경우에는 실제 encoding이 무엇인지 evidence를 확보하고 오류 처리 정책을 결정한다.

Decode error를 무조건 replacement character로 바꾸면 프로그램은 계속 실행되지만 원본 데이터가 손실될 수 있다. 반대로 모든 invalid byte를 거부하면 일부 로그나 외부 파일을 처리하지 못할 수 있다. `strict`, replacement, skip 같은 정책은 데이터 중요도와 복구 가능성에 따라 선택한다.

Binary format, image, compressed data, cryptographic material은 text decode를 거치지 않고 bytes로 다룬다. 확장자가 `.txt`인지보다 내용의 wire/storage format이 무엇인지가 mode 선택 기준이다.

---

## CHAPTER 02 · newline도 text encoding과 별개의 변환 규칙을 가진다

운영체제와 protocol은 줄바꿈을 서로 다른 byte sequence로 표현할 수 있다. Text mode는 환경에 따라 newline translation을 제공할 수 있으며 Python의 universal newline 처리로 여러 형태를 읽을 수도 있다. 하지만 파일을 byte-for-byte 보존하거나 protocol signature를 계산해야 한다면 이런 변환이 중요한 차이가 된다.

소스 코드나 configuration처럼 logical line이 중요할 때는 line 단위 iteration이 편리하다. 반대로 CSV처럼 quoted field 안에 newline이 들어갈 수 있는 형식은 임의로 `splitlines()`한 뒤 직접 parsing하면 format 규칙을 깨뜨릴 수 있다. 해당 format parser가 newline semantics를 소유하도록 한다.

마지막 줄이 newline으로 끝나지 않는 파일도 정상일 수 있다. `for line in f`는 그 줄을 제공할 수 있으므로 “모든 line 끝에는 반드시 `\n`이 있다”는 가정을 코드에 넣지 않는다. Trimming에서도 `.strip()`은 공백 전체를 제거하므로 의미 있는 leading/trailing space를 잃을 수 있다.

Text processing은 line ending, whitespace normalization, Unicode normalization을 각각 별개의 규칙으로 본다. 한 번의 “정리” 함수가 모두 처리하면 데이터 의미가 어느 단계에서 바뀌었는지 추적하기 어렵다.

---

## CHAPTER 03 · buffering은 호출 횟수와 실제 system I/O 횟수를 분리한다

프로그램이 `write()`를 호출했다고 해서 그 순간 bytes가 물리 저장장치에 영구 기록되었다는 뜻은 아니다. Python runtime과 운영체제는 작은 I/O를 모아 처리하기 위해 여러 단계의 buffer를 사용할 수 있다. 이 구조는 성능을 높이지만 visibility와 durability 시점을 다르게 만든다.

`flush()`는 일반적으로 사용자 공간 buffer의 데이터를 다음 계층으로 밀어내는 의미를 가지지만 전원 장애 뒤에도 반드시 남는 durable storage를 보장하는 것과는 다르다. 중요한 데이터를 영구화하려면 운영체제와 filesystem의 fsync 계열 semantics까지 고려해야 할 수 있다. 단순 script와 transaction log는 필요한 보장 수준이 다르다.

읽기에서도 buffering은 작은 `read(1)` 호출이 매번 system call을 발생시키지 않게 할 수 있다. 성능 분석에서 Python API 호출 수와 실제 disk operation을 같은 것으로 보지 않는다. 큰 파일을 처리할 때는 적절한 chunk size와 sequential access가 memory와 throughput에 영향을 준다.

Buffering policy를 수동으로 조정하기 전에 실제 bottleneck을 측정한다. 너무 작은 buffer는 syscall overhead를 키우고 너무 큰 buffer는 memory와 latency를 늘릴 수 있다. 기본값이 합리적인 경우가 많으며 명확한 workload evidence가 있을 때 변경한다.

---

## CHAPTER 04 · stream read는 요청한 길이만큼 항상 한 번에 준다고 가정하지 않는다

일반 파일에서는 원하는 byte 수를 쉽게 읽는 것처럼 보이지만 stream abstraction 전체에서는 `read(n)`이 n보다 적은 데이터를 반환할 수 있다. Pipe, socket, compressed stream처럼 생산 속도와 소비 속도가 다른 경우 partial read가 정상이다. Protocol parser는 “한 번 read하면 header 전체가 온다”는 가정을 피해야 한다.

고정 길이 N bytes가 반드시 필요하다면 반복해서 읽어 누적하고 EOF를 별도 처리한다. 반대로 streaming parser는 현재 buffer에 충분한 데이터가 없으면 `need more data` state로 남는다. Partial data와 invalid data를 구분해야 정상 network fragmentation을 오류로 오인하지 않는다.

Write도 일부 byte만 처리될 수 있는 low-level interface가 있다. High-level file object가 이를 내부에서 보완할 수 있지만 system programming이나 socket API를 다룰 때는 반환된 실제 byte count를 확인한다. “요청한 양”과 “처리된 양”을 분리하는 습관이 중요하다.

Stream은 끝을 알리는 EOF semantics도 가진다. Empty bytes가 일시적인 “아직 없음”인지 실제 EOF인지 API마다 다를 수 있으므로 문서를 확인한다. Blocking/non-blocking mode와 함께 해석해야 한다.

---

## CHAPTER 05 · path는 문자열이 아니라 filesystem namespace의 위치 표현이다

`"data/file.txt"`는 문자열처럼 보이지만 실제 의미는 current working directory, path separator, root, drive, symlink, case sensitivity 같은 filesystem 규칙에 의존한다. Path API를 사용하면 문자열 붙이기보다 운영체제별 separator와 normalization을 더 안전하게 처리할 수 있다.

Relative path는 “현재 어디에서 실행했는가”라는 숨은 입력을 가진다. CLI를 프로젝트 root에서 실행할 때는 성공하고 IDE나 service manager에서 다른 working directory로 실행하면 실패할 수 있다. 사용자 입력 파일인지 package resource인지 configuration 파일인지에 따라 기준 directory를 명시한다.

Path component에 `..`가 있다고 무조건 공격은 아니지만 untrusted input으로 base directory 밖을 접근할 수 있게 되면 path traversal 문제가 된다. 단순 문자열 prefix 검사보다 canonicalized path와 허용 root 관계를 안전하게 검사해야 한다. Symlink가 개입하면 canonicalization 시점과 실제 open 시점 사이 race도 고려할 수 있다.

파일명은 사용자에게 보이는 label과 동일하지 않을 수 있다. Unicode normalization, case folding, reserved names, 최대 길이 규칙이 filesystem마다 다르다. 외부 identifier를 그대로 파일명으로 사용할 때는 portability와 collision 정책을 정한다.

---

## CHAPTER 06 · atomic replacement는 “파일 쓰기 중 실패”의 상태를 설계한다

Configuration이나 중요 데이터를 기존 파일에 직접 덮어쓰다가 process가 중간에 죽으면 파일 일부만 기록될 수 있다. 더 안전한 패턴은 같은 filesystem 안의 임시 파일에 새 내용을 완성하고 필요한 durability 작업을 한 뒤 rename/replace operation으로 기존 이름에 교체하는 것이다. Filesystem이 제공하는 atomic rename semantics를 활용하면 독자가 old 또는 new version 중 하나를 보게 할 수 있다.

그러나 rename이 항상 모든 durability를 보장하는 것은 아니다. 전원 손실까지 견뎌야 하는 시스템에서는 file data와 directory metadata의 sync semantics를 함께 고려해야 한다. 단순 개인 설정 저장과 database storage engine은 요구 수준이 다르다.

Temporary file의 permission과 위치도 중요하다. 다른 filesystem에 만들면 rename이 atomic하지 않을 수 있고 copy+delete로 바뀔 수 있다. Predictable temporary filename을 공유 directory에 만들면 race나 security 문제가 생길 수 있어 안전한 temp API를 사용한다.

Atomicity는 한 파일 내부의 교체 문제를 해결할 뿐 여러 파일의 일관성을 자동 보장하지 않는다. 두 파일이 반드시 같은 version이어야 한다면 manifest pointer, transaction-like directory swap, database 같은 더 높은 수준의 모델이 필요하다.

---

## CHAPTER 07 · file lock과 process concurrency는 application-level 상태를 추가한다

여러 process가 같은 파일을 동시에 쓰면 각자의 buffer와 write 순서가 섞여 데이터가 손상될 수 있다. 한 process 안에서 thread lock을 걸어도 다른 process에는 영향이 없다. 공유 파일의 writer coordination이 필요하다면 OS-level lock, atomic append semantics, single-writer architecture 등 문제에 맞는 전략을 사용한다.

File locking semantics는 platform과 filesystem에 따라 advisory/mandatory behavior가 다를 수 있다. Network filesystem에서는 local disk와 다른 제약이 있을 수 있다. “lock API를 호출했다”는 사실만으로 원하는 배타성이 보장되는지 확인한다.

Lock을 획득한 상태에서 느린 작업을 하면 다른 writer가 오래 대기한다. 필요한 data를 미리 계산하고 critical section을 좁히며 timeout/cancellation 정책을 둔다. Process가 비정상 종료했을 때 lock이 어떻게 해제되는지도 API semantics를 확인한다.

더 큰 시스템에서는 파일을 공유 mutable database처럼 사용하기보다 single writer service나 실제 database를 선택하는 것이 단순할 수 있다. Coordination cost가 자료 저장 편의보다 커지는 시점을 인식한다.

---

## CHAPTER 08 · directory traversal은 tree walk이지만 cycle과 오류 정책을 가진다

Directory tree를 재귀적으로 순회할 때는 file, directory, symlink, permission denied, disappearing entry를 만날 수 있다. “폴더 안 모든 파일”이라는 요구에는 symlink를 따라갈지, hidden file을 포함할지, 다른 filesystem mount를 넘을지 같은 정책이 필요하다.

Symlink를 directory처럼 따라가면 cycle이 생겨 무한 traversal이 가능하다. 방문한 filesystem identity를 추적하거나 symlink follow 정책을 제한한다. Directory 내용은 순회 중 다른 process에 의해 바뀔 수 있으므로 처음 목록을 읽었을 때 존재한 entry가 open 시점에는 사라질 수 있다.

한 파일 permission 오류가 전체 작업을 중단해야 하는지 건너뛰고 보고해야 하는지도 domain에 따라 다르다. Backup이라면 누락을 조용히 무시하면 위험하고, 검색 indexer라면 일부 unreadable file을 기록하고 계속 진행할 수 있다.

대규모 tree는 모든 path를 list에 모으지 않고 iterator로 streaming할 수 있다. 하지만 traversal 중 directory handle과 metadata call이 많아질 수 있어 resource와 latency를 측정한다.

---

## CHAPTER 09 · file format은 extension보다 version과 schema 계약이 중요하다

`.json`, `.csv`, `.bin` 같은 확장자는 format 후보를 알려 주지만 데이터가 실제로 어떤 schema와 version을 가지는지는 별도 문제다. 프로그램이 저장한 파일을 미래 version이 읽어야 한다면 field 추가/삭제, default, migration 규칙을 설계해야 한다.

파일 앞에 magic number나 version metadata를 두면 잘못된 format을 더 빨리 거부할 수 있다. Text format에서도 top-level schema version을 명시할 수 있다. “파싱 가능”과 “현재 프로그램이 의미를 이해할 수 있음”을 구분한다.

User-editable configuration은 친숙한 text format이 장점이지만 syntax error와 partial edit를 고려해야 한다. 읽기 전에 이전 valid version을 보존하거나 validation 후에만 active config를 교체하면 잘못된 파일 하나가 전체 system을 불안정하게 만드는 것을 줄일 수 있다.

File format을 internal object dump로 생각하지 않는다. Object layout은 코드 리팩터링에 따라 바뀌지만 storage schema는 장기 호환성 요구를 가질 수 있다. 명시적인 serialization boundary가 필요하다.

---

## CHAPTER 10 · I/O boundary를 좁히면 계산 로직과 실패 모델이 단순해진다

파일을 읽는 함수 안에서 parsing, validation, 계산, network 요청, 결과 저장을 모두 수행하면 테스트와 오류 처리가 복잡해진다. `read bytes/text → parse → validate → compute → serialize → write` 단계로 경계를 나누면 각 단계가 다른 failure class를 가질 수 있다.

Core 계산 함수는 이미 검증된 data structure를 받고 file path를 모르게 만들 수 있다. 그러면 unit test는 filesystem 없이 값만 사용하고 file integration test는 encoding, path, permission, atomic replacement를 집중적으로 검증한다.

Streaming이 필요한 경우에도 parser가 file object 자체에 과도하게 결합되지 않도록 iterable bytes/lines를 받을 수 있다. File, memory buffer, network stream을 같은 parser와 연결할 수 있지만 lifetime ownership은 호출자와 함수 중 누가 갖는지 명확히 한다.

I/O 설계의 핵심은 `open-read-close` 문법이 아니다. **어떤 bytes가 어떤 의미로 변환되고, 어느 시점까지 resource가 살아 있으며, 실패하면 외부에 어떤 상태가 남는가**를 설명할 수 있는 경계를 만드는 것이다.