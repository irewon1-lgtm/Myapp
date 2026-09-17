# PART 92 · Safe archive extraction — entry path·symlink·압축 팽창을 trust boundary로 다루기

ZIP이나 TAR 파일을 푸는 코드는 간단해 보이지만 archive 내부 entry 이름과 metadata는 외부 입력이다. `../`, absolute path, symlink, 과도한 압축률을 그대로 신뢰하면 지정 directory 밖의 파일을 덮거나 disk를 소진할 수 있다. 안전한 extraction은 `extractall()` 한 줄보다 **entry 검증 → resource budget → staging → 최종 반영**의 pipeline으로 설계한다.

---

## CHAPTER 01 · archive entry는 파일 하나가 아니라 path와 metadata를 가진 입력 record다

Archive member에는 이름, 압축/원본 크기, directory 여부, 권한과 링크 관련 metadata가 있을 수 있다. 따라서 extraction 전에 member list를 먼저 읽고 정책을 적용한다.

```python
with zipfile.ZipFile(path) as zf:
    for info in zf.infolist():
        validate_member(info)
```

“확장자가 zip이다”는 신뢰 근거가 아니다. Parser가 archive로 인식한 뒤에도 각 entry는 독립적인 untrusted input이다.

먼저 전체 entry를 inventory하면 개수·총 예상 크기·중복 이름 같은 archive-level invariant를 확인할 수 있다.

---

## CHAPTER 02 · path traversal은 destination 문자열을 붙이는 방식만으로 막을 수 없다

Entry 이름이 `../../outside.txt`라면 단순히 destination과 join했을 때 extraction root를 벗어날 수 있다. Absolute path, drive prefix, platform separator도 함께 고려한다.

```text
root = /safe/upload/42
entry = ../../etc/example
```

안전한 정책은 entry를 relative logical path로 검증하고 최종 target이 허용 root 아래에 있는지 확인하는 것이다. 하지만 P78에서 본 것처럼 lexical path 검사만으로 symlink race까지 모두 해결되지는 않는다.

Archive library의 현재 안전 동작을 무조건 가정하지 말고 application security contract에서 명시적으로 검증한다.

---

## CHAPTER 03 · symlink entry는 extraction root 밖으로 후속 write를 유도할 수 있다

TAR 같은 형식은 symbolic link나 hard link 정보를 담을 수 있다. 먼저 `link -> /outside`를 만든 뒤 다음 entry가 `link/file`로 기록되면 경로 문자열은 root 아래처럼 보여도 실제 write target은 밖일 수 있다.

Untrusted archive에서는 symlink/hardlink를 아예 거부하는 정책이 가장 단순할 수 있다. 링크가 꼭 필요하다면 target이 허용 tree 안에 머무는지 검증하고 extraction 순서와 race를 고려해야 한다.

“최종 resolved path 한 번 확인”만으로 concurrent filesystem mutation을 완전히 제거했다고 주장하지 않는다. Security-sensitive extraction은 staging directory와 제한된 capability를 사용한다.

---

## CHAPTER 04 · 작은 compressed file이 매우 큰 output으로 팽창할 수 있다

Archive bomb은 compressed size만 보면 작지만 해제하면 매우 큰 disk/memory를 소비할 수 있다. Entry header가 알려주는 uncompressed size를 이용해 사전 budget을 걸 수 있지만 metadata 자체가 거짓일 가능성도 생각한다.

따라서 두 단계 제한이 좋다.

1. 사전에 선언된 총 uncompressed size와 개별 entry size를 검사한다.
2. 실제 extraction stream에서도 누적 write byte를 세어 hard limit을 넘으면 중단한다.

Memory에 전체 entry를 읽은 뒤 크기를 확인하는 방식은 이미 resource exhaustion을 일으킨 뒤다. Streaming budget을 적용한다.

---

## CHAPTER 05 · entry count도 별도 resource budget이다

0-byte 파일 수십만 개는 총 byte가 작아도 inode, directory traversal, antivirus scan, metadata DB를 압박할 수 있다. 따라서 총 크기뿐 아니라 entry count와 path depth에도 제한을 둔다.

극단적으로 긴 filename, 깊은 directory nesting, 같은 이름 중복도 처리 비용을 키운다. Archive processing은 CPU·disk·metadata budget을 동시에 사용한다.

Budget 초과는 일반 parse error와 분리된 policy rejection으로 다루면 운영자가 악성/비정상 입력을 식별하기 쉽다.

---

## CHAPTER 06 · permission과 timestamp metadata를 그대로 복원할지 정책을 정한다

Archive가 executable bit, mode, timestamp를 가지고 있다고 해서 서버가 그대로 적용해야 하는 것은 아니다. Upload unpacking처럼 data file만 필요한 서비스는 application-controlled permission으로 새 파일을 만들 수 있다.

Ownership, special file, device node 같은 metadata를 허용하는 것은 훨씬 강한 capability다. 일반 application archive에서는 regular file과 directory만 허용하는 allowlist가 단순하다.

Metadata preservation이 요구되는 backup tool은 threat model이 다르므로 별도 privileged path로 설계한다.

---

## CHAPTER 07 · staged extraction은 검증되지 않은 파일을 최종 경로에 바로 노출하지 않는다

안전한 흐름은 private staging directory에 extraction하고 전체 검증을 마친 뒤 최종 location으로 이동하는 것이다.

```text
archive -> isolated staging -> validate tree -> publish
```

중간에 parser error나 budget 초과가 나면 staging tree를 폐기하면 된다. 최종 서비스 directory에 절반의 파일이 보이는 상태를 줄일 수 있다.

Publish 단계에도 P91의 atomic replace 원칙을 적용할 수 있다. 다만 directory tree 교체 semantics는 platform/filesystem별로 검증한다.

---

## CHAPTER 08 · archive contract는 path와 resource limit을 extraction 전에 결정한다

Untrusted archive API는 허용 entry type, 최대 entry count, 최대 개별/총 size, 최대 path depth, symlink 정책, permission 정책, staging 위치를 명시해야 한다.

테스트 corpus에는 `../` path, absolute path, symlink chain, duplicate name, huge declared size, 실제 팽창, 매우 많은 empty file을 포함한다. 정상 archive 하나가 풀린다는 사실은 security 검증이 아니다.

이 PART의 핵심은 **archive를 파일 묶음으로만 보지 않고 filesystem mutation을 요청하는 untrusted instruction set으로 보고, entry마다 path·metadata·resource capability를 제한하는 것**이다.
