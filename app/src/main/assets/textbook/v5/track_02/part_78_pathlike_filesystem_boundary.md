# PART 78 · PathLike와 filesystem boundary — 문자열·경로 identity·symlink를 분리하기

파일 경로는 단순 문자열처럼 보이지만 실제로는 운영체제 namespace를 가리키는 식별자다. Python은 `str`, `bytes`, `os.PathLike`, `pathlib.Path`를 연결하는 filesystem path protocol을 제공한다. 이 경계를 정확히 이해하지 못하면 normalize한 문자열과 실제 파일 identity를 혼동하거나, symlink·상대경로·case 규칙 때문에 보안 검사가 우회될 수 있다.

---

## CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다

`PathLike` 객체는 `__fspath__`를 통해 OS API가 이해할 수 있는 path representation을 제공할 수 있다.

```python
import os

class AppPath:
    def __init__(self, value):
        self.value = value

    def __fspath__(self):
        return self.value

print(os.fspath(AppPath("data/input.txt")))
```

이 protocol 덕분에 library는 `str`만 강제하지 않고 path-like object를 받을 수 있다. 그러나 path object가 존재한다고 해서 해당 file이 실제로 존재하거나 접근 권한이 있다는 뜻은 아니다.

Path representation과 filesystem lookup 결과를 분리한다.

---

## CHAPTER 02 · `os.fspath`는 path-like input을 실제 OS path representation으로 좁히는 경계다

Public API가 `str | PathLike`를 받는다면 내부 boundary에서 `os.fspath()`로 정규화해 처리할 수 있다. 이때 반환 type은 일반적으로 `str` 또는 `bytes` 계열 path representation이다.

Library마다 path-like를 끝까지 유지하거나 초기에 string으로 바꾸는 전략이 다를 수 있다. 중요한 것은 conversion 지점을 한 곳에 두고 이후 code가 어떤 representation을 다루는지 일관되게 만드는 것이다.

사용자 object의 `__fspath__`가 arbitrary side effect를 갖지 않도록 타입 설계도 단순하게 유지한다. Path conversion은 cheap, deterministic operation이라는 기대가 강하다.

---

## CHAPTER 03 · path object와 string은 같은 내용을 담아도 다른 역할을 가진다

`"logs/app.txt"`는 문자열 operation에 참여할 수 있고 `Path("logs/app.txt")`는 path joining, parent, suffix 같은 filesystem-oriented operation을 제공한다.

```python
from pathlib import Path

base = Path("logs")
path = base / "app.txt"
```

문자열 concatenation으로 slash를 직접 붙이면 platform separator, absolute path handling, accidental double separator를 스스로 처리해야 한다. Path object는 이런 의도를 더 명시적으로 표현한다.

그렇다고 `Path`가 security-safe path를 자동 보장하지는 않는다. User input을 `/` 연산자로 붙였다고 traversal 위험이 사라지는 것은 아니다.

---

## CHAPTER 04 · bytes path는 text path와 encoding boundary가 다르다

일부 OS API는 bytes path를 지원하지만 text path와 섞으면 encoding 의미가 복잡해진다. 대부분의 application code에서는 text path를 일관되게 사용하는 편이 이해하기 쉽다.

Bytes filename이 필요한 low-level code에서는 filesystem encoding과 undecodable byte sequence 처리 방식을 알아야 한다. `repr`에서 보이는 byte escape를 실제 human-readable filename과 혼동하지 않는다.

API가 `str`과 `bytes`를 모두 받는다면 반환 path type도 input type과 일관된지 확인한다. 서로 암묵적으로 decode/encode하는 helper를 여러 층에 흩뿌리지 않는다.

---

## CHAPTER 05 · 문자열 normalization은 파일 identity normalization과 같지 않다

`a/../b`, `./b`, absolute path 변환은 문자열 형태를 바꿀 수 있지만 실제 filesystem에서 같은 object를 가리킨다는 사실은 symlink, mount, case behavior에 따라 단순하지 않다.

`normpath` 같은 lexical normalization과 실제 `resolve`/canonicalization을 구분한다. File이 아직 존재하지 않거나 symlink target이 바뀔 수 있는 경우 결과 의미도 다르다.

Cache key로 path를 사용할 때는 어떤 identity를 원하는지 정한다. 사용자가 입력한 spelling을 보존할지, absolute lexical path를 사용할지, 실제 resolved target을 key로 삼을지에 따라 behavior가 달라진다.

---

## CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다

보안 검사를 위해 “resolved path가 허용 directory 안에 있다”고 확인한 뒤 나중에 다시 file을 열면 그 사이 symlink가 바뀌거나 filesystem state가 변할 수 있다.

```text
check path -> filesystem changes -> open path
```

이 두 operation이 분리되면 TOCTOU race가 가능하다. 고신뢰 코드에서는 directory handle 기반 open, no-follow option, OS-specific primitive처럼 검사와 사용을 더 가깝게 묶는 방법을 검토한다.

Path 문자열 비교만으로 sandbox boundary를 만들지 않는다. Filesystem은 동적인 namespace다.

---

## CHAPTER 07 · boundary validation은 path syntax보다 capability를 제한하는 쪽이 강하다

Upload filename이나 archive entry를 받는 API는 `..`, absolute path, separator를 검사할 수 있다. 하지만 blacklist만으로 모든 플랫폼 corner case를 막기 어렵다.

더 강한 설계는 caller에게 arbitrary path를 받지 않고 logical file ID를 받아 server-controlled directory에 mapping하거나, 이미 열린 directory capability를 기준으로 relative access만 허용하는 것이다.

Path validation 후에는 permission, file type, symlink, race도 고려한다. “문자열이 안전해 보인다”와 “원하는 file만 접근 가능하다”는 다른 주장이다.

---

## CHAPTER 08 · path contract는 representation·identity·trust boundary를 함께 정한다

Path API를 설계할 때 입력으로 `str`/`PathLike`를 어디까지 허용할지, relative path 기준 directory가 무엇인지, symlink를 따라갈지, 반환값이 original spelling인지 canonical path인지 명확히 한다.

테스트에는 `..`, absolute path, symlink, nonexistent path, bytes/text 경계, platform separator 차이를 포함한다. Security-critical path operation은 unit test뿐 아니라 실제 filesystem fixture에서 검증한다.

이 PART의 핵심은 **path를 문자열로 축소하지 않고, OS namespace를 가리키는 representation과 실제 filesystem identity, 그리고 접근 capability를 서로 분리해 설계하는 것**이다.
