# PART 92 · Safe archive extraction — entry path·symlink·압축 팽창을 trust boundary로 다루기

ZIP이나 TAR 파일을 푸는 코드는 간단해 보이지만 archive 내부 entry 이름과 metadata는 외부 입력이다. `../`, absolute path, symlink, 과도한 압축률을 그대로 신뢰하면 지정 directory 밖의 파일을 덮거나 disk를 소진할 수 있다. 안전한 extraction은 `extractall()` 한 줄보다 **entry 검증 → resource budget → staging → 최종 반영**의 pipeline으로 설계한다.

---

## CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다

### 시작 전 용어집

#### 1. archive

- **뜻:** Archive member에는 이름, 압축/원본 크기, directory 여부, 권한과 링크 관련 metadata가 있을 수 있다.
- **왜 중요한가:** 따라서 extraction 전에 member list를 먼저 읽고 정책을 적용한다.
- **예시:** with zipfile.ZipFile(path) as zf: / for info in …

#### 2. path

- **뜻:** Parser가 archive로 인식한 뒤에도 각 entry는 독립적인 untrusted input이다.
- **왜 중요한가:** 먼저 전체 entry를 inventory하면 개수·총 예상 크기·중복 이름 같은 archive-level invariant를 확인할 수 있다.
- **예시:** with zipfile.ZipFile(path) as zf: / for info in …

```python
with zipfile.ZipFile(path) as zf:
    for info in zf.infolist():
        validate_member(info)
```

“확장자가 zip이다”는 신뢰 근거가 아니다. 


---

**검증 시나리오 P92-C1 — CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다**
`CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다` 검증은 입력 크기를 고정하는 데서 시작한다. P92-C1에서는 `CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다`에 대해 변형은 한 요소만 허용고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다`의 측정값을 여러 번 모아 분포를 비교하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P92-C1의 마무리는 워밍업과 측정 자체의 비용을 분리하는 것이다. 통과 기준은 `CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 02 · path traversal은 destination 문자열을 붙이는 방식만으로 막을 수 없다

### 시작 전 용어집

#### 1. path

- **뜻:** Absolute path, drive prefix, platform separator도 함께 고려한다.
- **왜 중요한가:** 안전한 정책은 entry를 relative logical path로 검증하고 최종 target이 허용 root 아래에 있는지 확인하는 것이다.
- **예시:** root = /safe/upload/42 / entry = ../../etc/example

#### 2. destination

- **뜻:** txt`라면 단순히 destination과 join했을 때 extraction root를 벗어날 수 있다.
- **왜 중요한가:** 하지만 P78에서 본 것처럼 lexical path 검사만으로 symlink race까지 모두 해결되지는 않는다.
- **예시:** root = /safe/upload/42 / entry = ../../etc/example

#### 3. 검증

- **뜻:** Archive library의 현재 안전 동작을 무조건 가정하지 말고 application security contract에서 명시적으로 검증한다.
- **예시:** root = /safe/upload/42 / entry = ../../etc/example

Entry 이름이 `../../outside. 

```text
root = /safe/upload/42
entry = ../../etc/example
```

 


---

## CHAPTER 03 · symlink entry는 extraction root 밖으로 후속 write를 유도할 수 있다

### 시작 전 용어집

#### 1. symlink entry

- **뜻:** TAR 같은 형식은 symbolic link나 hard link 정보를 담을 수 있다.
- **왜 중요한가:** 먼저 `link -> /outside`를 만든 뒤 다음 entry가 `link/file`로 기록되면 경로 문자열은 root 아래처럼 보여도 실제 write target은 밖일 수 있다.
- **예시:** Untrusted archive에서는 symlink/hardlink를 아예 거부하는 정책이 가장 단순할 …

#### 2. extraction root

- **뜻:** Untrusted archive에서는 symlink/hardlink를 아예 거부하는 정책이 가장 단순할 수 있다.
- **왜 중요한가:** 링크가 꼭 필요하다면 target이 허용 tree 안에 머무는지 검증하고 extraction 순서와 race를 고려해야 한다.
- **예시:** Untrusted archive에서는 symlink/hardlink를 아예 거부하는 정책이 가장 단순할 …

#### 3. write

- **뜻:** “최종 resolved path 한 번 확인”만으로 concurrent filesystem mutation을 완전히 제거했다고 주장하지 않는다.
- **왜 중요한가:** Security-sensitive extraction은 staging directory와 제한된 capability를 사용한다.
- **예시:** “최종 resolved path 한 번 확인”만으로 concurrent filesystem …

---

## CHAPTER 04 · 작은 compressed file이 매우 큰 output으로 팽창할 수 있다

### 시작 전 용어집

#### 1. compressed file

- **뜻:** Archive bomb은 compressed size만 보면 작지만 해제하면 매우 큰 disk/memory를 소비할 수 있다.
- **왜 중요한가:** Entry header가 알려주는 uncompressed size를 이용해 사전 budget을 걸 수 있지만 metadata 자체가 거짓일 가능성도 생각한다.
- **예시:** Archive bomb은 compressed size만 보면 작지만 해제하면 매우 …

#### 2. output

- **뜻:** 사전에 선언된 총 uncompressed size와 개별 entry size를 검사한다.
- **왜 중요한가:** 실제 extraction stream에서도 누적 write byte를 세어 hard limit을 넘으면 중단한다.
- **예시:** 사전에 선언된 총 uncompressed size와 개별 entry size를 …

#### 3. archive

- **뜻:** Memory에 전체 entry를 읽은 뒤 크기를 확인하는 방식은 이미 resource exhaustion을 일으킨 뒤다.
- **예시:** Memory에 전체 entry를 읽은 뒤 크기를 확인하는 방식은 …

따라서 두 단계 제한이 좋다.

1. 
2. 

 Streaming budget을 적용한다.

---

## CHAPTER 05 · entry count도 별도 resource budget이다

### 시작 전 용어집

#### 1. entry count

- **뜻:** 따라서 총 크기뿐 아니라 entry count와 path depth에도 제한을 둔다.
- **왜 중요한가:** 극단적으로 긴 filename, 깊은 directory nesting, 같은 이름 중복도 처리 비용을 키운다.
- **예시:** 따라서 총 크기뿐 아니라 entry count와 path depth에도 …

#### 2. resource budget

- **뜻:** 0-byte 파일 수십만 개는 총 byte가 작아도 inode, directory traversal, antivirus scan, metadata DB를 압박할 수 있다.
- **왜 중요한가:** Archive processing은 CPU·disk·metadata budget을 동시에 사용한다.
- **예시:** 0-byte 파일 수십만 개는 총 byte가 작아도 inode, …

#### 3. path

- **뜻:** Budget 초과는 일반 parse error와 분리된 policy rejection으로 다루면 운영자가 악성/비정상 입력을 식별하기 쉽다.
- **예시:** Budget 초과는 일반 parse error와 분리된 policy rejection으로 …

---

**검증 시나리오 P92-C5 — CHAPTER 05 · entry count도 별도 resource budget이다**
`CHAPTER 05 · entry count도 별도 resource budget이다` 검증은 가장 작은 객체 상태로 시작하는 데서 시작한다. P92-C5에서는 `CHAPTER 05 · entry count도 별도 resource budget이다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 05 · entry count도 별도 resource budget이다`에 대해 속성 하나만 바꿔 재실행고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 05 · entry count도 별도 resource budget이다`의 반환값과 부수효과를 따로 기록하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 05 · entry count도 별도 resource budget이다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P92-C5의 마무리는 수정 뒤 원래 조건을 다시 회귀 확인하는 것이다. 통과 기준은 `CHAPTER 05 · entry count도 별도 resource budget이다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 06 · permission과 timestamp metadata를 그대로 복원할지 정책을 정한다

### 시작 전 용어집

#### 1. permission

- **뜻:** Upload unpacking처럼 data file만 필요한 서비스는 application-controlled permission으로 새 파일을 만들 수 있다.
- **왜 중요한가:** Ownership, special file, device node 같은 metadata를 허용하는 것은 훨씬 강한 capability다.
- **예시:** Upload unpacking처럼 data file만 필요한 서비스는 application-controlled permission으로 …

#### 2. timestamp metadata

- **뜻:** Archive가 executable bit, mode, timestamp를 가지고 있다고 해서 서버가 그대로 적용해야 하는 것은 아니다.
- **왜 중요한가:** 일반 application archive에서는 regular file과 directory만 허용하는 allowlist가 단순하다.
- **예시:** Archive가 executable bit, mode, timestamp를 가지고 있다고 해서 …

#### 3. archive

- **뜻:** Metadata preservation이 요구되는 backup tool은 threat model이 다르므로 별도 privileged path로 설계한다.
- **예시:** Metadata preservation이 요구되는 backup tool은 threat model이 다르므로 …

---

## CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다

### 시작 전 용어집

#### 1. staged extraction

- **뜻:** 안전한 흐름은 private staging directory에 extraction하고 전체 검증을 마친 뒤 최종 location으로 이동하는 것이다.
- **왜 중요한가:** 중간에 parser error나 budget 초과가 나면 staging tree를 폐기하면 된다.
- **예시:** archive -> isolated staging -> validate tree -> …

#### 2. 검증

- **뜻:** 다만 directory tree 교체 semantics는 platform/filesystem별로 검증한다.
- **왜 중요한가:** 최종 서비스 directory에 절반의 파일이 보이는 상태를 줄일 수 있다.
- **예시:** archive -> isolated staging -> validate tree -> …

#### 3. archive

- **뜻:** Publish 단계에도 P91의 atomic replace 원칙을 적용할 수 있다.
- **예시:** archive -> isolated staging -> validate tree -> …

```text
archive -> isolated staging -> validate tree -> publish
```

 

 

---

**검증 시나리오 P92-C7 — CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다**
`CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다` 검증은 정상 경로를 먼저 재현하는 데서 시작한다. P92-C7에서는 `CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다`에 대해 오류 경로 하나를 의도적으로 만든다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다`의 예외 종류와 직전 상태를 함께 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P92-C7의 마무리는 복구 후 같은 오류가 다시 재현되지 않는지 검사하는 것이다. 통과 기준은 `CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 08 · archive contract는 path와 resource limit을 extraction 전에 결정한다

### 시작 전 용어집

#### 1. archive

- **뜻:** Untrusted archive API는 허용 entry type, 최대 entry count, 최대 개별/총 size, 최대 path depth, symlink 정책, permission 정책, staging 위치를 명시해야 한다.
- **왜 중요한가:** /` path, absolute path, symlink chain, duplicate name, huge declared size, 실제 팽창, 매우 많은 empty file을 포함한다.
- **예시:** Untrusted archive API는 허용 entry type, 최대 entry …

#### 2. path

- **뜻:** 이 PART의 핵심은 **archive를 파일 묶음으로만 보지 않고 filesystem mutation을 요청하는 untrusted instruction set으로 보고, entry마다 path·metadata·resource capability를 제한하는 것**이다.
- **왜 중요한가:** 정상 archive 하나가 풀린다는 사실은 security 검증이 아니다.
- **예시:** 이 PART의 핵심은 **archive를 파일 묶음으로만 보지 않고 …

테스트 corpus에는 `..

---

## 실전 학습 루프 · safe archive extraction

### 1. 쉬운 예

ZIP/TAR entry 이름을 그대로 destination에 붙이면 `../` 경로 탈출, absolute path, symlink를 통한 외부 쓰기가 가능할 수 있다. 압축 해제 전 inventory와 총 확장 크기, entry 수, 허용 경로를 검증해야 한다.

### 2. 한 줄 해석

archive extraction은 파일 복사가 아니라 공격자가 제어할 수 있는 경로·용량·링크를 해석하는 보안 경계다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
from pathlib import Path

root = Path('/safe/root').resolve()
name = '../outside.txt'
candidate = (root / name).resolve()
print(candidate.is_relative_to(root))
```

### 4. 수정 실습

1. entry 수와 압축 해제 총 byte budget을 추가한다.
2. symlink entry를 허용할지 거부할지 정책을 명시하고 우회 사례를 생각한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

파일명에서 문자열 `..`만 제거하면 안전한 archive extraction이 될까?

### 6. 정답과 오답 설명

**정답:** 아니다. absolute path, separator 변형, symlink, canonicalization, resource exhaustion까지 함께 다뤄야 한다.

**자주 나오는 오답:** 문자열 필터 하나로 실제 filesystem 경계를 대신하려는 접근이 위험하다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · safe archive extraction

### 증상에서 시작한다

테스트 ZIP은 잘 풀리지만 악성 entry에서 허용 루트 밖 파일이 생기거나 disk가 예상보다 크게 소모된다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

entry raw name, normalized/resolved path, type, compressed/uncompressed size, 누적 budget과 symlink 정보를 본다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

`../`, absolute path, nested symlink, 많은 작은 파일, 높은 compression ratio archive를 각각 주입한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

모든 entry가 허용 루트와 resource budget 안에 있고 실제 write 전에 위험 entry가 거부돼야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

