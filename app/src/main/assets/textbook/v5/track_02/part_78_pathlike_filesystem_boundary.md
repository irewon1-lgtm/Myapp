# PART 78 · PathLike와 filesystem boundary — 문자열·경로 identity·symlink를 분리하기

파일 경로는 단순 문자열처럼 보이지만 실제로는 운영체제 namespace를 가리키는 식별자다. Python은 `str`, `bytes`, `os.PathLike`, `pathlib.Path`를 연결하는 filesystem path protocol을 제공한다. 이 경계를 정확히 이해하지 못하면 normalize한 문자열과 실제 파일 identity를 혼동하거나, symlink·상대경로·case 규칙 때문에 보안 검사가 우회될 수 있다.

---

## CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다

### 시작 전 용어집

#### 1. PathLike

- **뜻:** `PathLike` 객체는 `__fspath__`를 통해 OS API가 이해할 수 있는 path representation을 제공할 수 있다.
- **왜 중요한가:** 이 protocol 덕분에 library는 `str`만 강제하지 않고 path-like object를 받을 수 있다.
- **예시:** import os / class AppPath:

#### 2. filesystem

- **뜻:** Path representation과 filesystem lookup 결과를 분리한다.
- **왜 중요한가:** 그러나 path object가 존재한다고 해서 해당 file이 실제로 존재하거나 접근 권한이 있다는 뜻은 아니다.
- **예시:** import os / class AppPath:

```python
import os

class AppPath:
    def __init__(self, value):
        self.value = value

    def __fspath__(self):
        return self.value

print(os.fspath(AppPath("data/input.txt")))
```

 


---

**검증 시나리오 P78-C1 — CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다**
`CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다` 검증은 반례부터 하나 만든다하는 데서 시작한다. P78-C1에서는 `CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다`에 대해 반례와 정상례의 차이만 좁힌다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다`의 중간 값을 직접 출력해 추측을 줄인다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P78-C1의 마무리는 수정 후 두 사례를 같은 순서로 재검증하는 것이다. 통과 기준은 `CHAPTER 01 · `os.PathLike`는 경로를 문자열 자체가 아니라 filesystem 표현 가능 객체로 만든다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 02 · `os.fspath`는 path-like input을 실제 OS path representation으로 좁히는 경계다

### 시작 전 용어집

#### 1. path

- **뜻:** Public API가 `str | PathLike`를 받는다면 내부 boundary에서 `os.
- **왜 중요한가:** 이때 반환 type은 일반적으로 `str` 또는 `bytes` 계열 path representation이다.
- **예시:** Public API가 `str | PathLike`를 받는다면 내부 boundary에서 …

#### 2. 경계

- **뜻:** Library마다 path-like를 끝까지 유지하거나 초기에 string으로 바꾸는 전략이 다를 수 있다.
- **왜 중요한가:** 중요한 것은 conversion 지점을 한 곳에 두고 이후 code가 어떤 representation을 다루는지 일관되게 만드는 것이다.
- **예시:** Library마다 path-like를 끝까지 유지하거나 초기에 string으로 바꾸는 전략이 …

#### 3. bytes

- **뜻:** 사용자 object의 `__fspath__`가 arbitrary side effect를 갖지 않도록 타입 설계도 단순하게 유지한다.
- **왜 중요한가:** Path conversion은 cheap, deterministic operation이라는 기대가 강하다.
- **예시:** 사용자 object의 `__fspath__`가 arbitrary side effect를 갖지 않도록 …

fspath()`로 정규화해 처리할 수 있다. 

 

 

---

## CHAPTER 03 · path object와 string은 같은 내용을 담아도 다른 역할을 가진다

### 시작 전 용어집

#### 1. path

- **뜻:** txt"`는 문자열 operation에 참여할 수 있고 `Path("logs/app.
- **왜 중요한가:** txt")`는 path joining, parent, suffix 같은 filesystem-oriented operation을 제공한다.
- **예시:** from pathlib import Path / base = Path("logs")

#### 2. string

- **뜻:** 문자열 concatenation으로 slash를 직접 붙이면 platform separator, absolute path handling, accidental double separator를 스스로 처리해야 한다.
- **왜 중요한가:** Path object는 이런 의도를 더 명시적으로 표현한다.
- **예시:** from pathlib import Path / base = Path("logs")

#### 3. FFI

- **뜻:** 그렇다고 `Path`가 security-safe path를 자동 보장하지는 않는다.
- **왜 중요한가:** User input을 `/` 연산자로 붙였다고 traversal 위험이 사라지는 것은 아니다.
- **예시:** from pathlib import Path / base = Path("logs")

`"logs/app.

```python
from pathlib import Path

base = Path("logs")
path = base / "app.txt"
```

 

 

---

## CHAPTER 04 · bytes path는 text path와 encoding boundary가 다르다

### 시작 전 용어집

#### 1. bytes

- **뜻:** 일부 OS API는 bytes path를 지원하지만 text path와 섞으면 encoding 의미가 복잡해진다.
- **왜 중요한가:** 대부분의 application code에서는 text path를 일관되게 사용하는 편이 이해하기 쉽다.
- **예시:** 일부 OS API는 bytes path를 지원하지만 text path와 …

#### 2. path

- **뜻:** API가 `str`과 `bytes`를 모두 받는다면 반환 path type도 input type과 일관된지 확인한다.
- **왜 중요한가:** 서로 암묵적으로 decode/encode하는 helper를 여러 층에 흩뿌리지 않는다.
- **예시:** API가 `str`과 `bytes`를 모두 받는다면 반환 path type도 …

#### 3. encoding boundary

- **뜻:** Bytes filename이 필요한 low-level code에서는 filesystem encoding과 undecodable byte sequence 처리 방식을 알아야 한다.
- **왜 중요한가:** `repr`에서 보이는 byte escape를 실제 human-readable filename과 혼동하지 않는다.
- **예시:** Bytes filename이 필요한 low-level code에서는 filesystem encoding과 undecodable …

---

## CHAPTER 05 · 문자열 normalization은 파일 identity normalization과 같지 않다

### 시작 전 용어집

#### 1. normalization

- **뜻:** `normpath` 같은 lexical normalization과 실제 `resolve`/canonicalization을 구분한다.
- **왜 중요한가:** File이 아직 존재하지 않거나 symlink target이 바뀔 수 있는 경우 결과 의미도 다르다.
- **예시:** `normpath` 같은 lexical normalization과 실제 `resolve`/canonicalization을 구분한다.

#### 2. identity

- **뜻:** Cache key로 path를 사용할 때는 어떤 identity를 원하는지 정한다.
- **왜 중요한가:** 사용자가 입력한 spelling을 보존할지, absolute lexical path를 사용할지, 실제 resolved target을 key로 삼을지에 따라 behavior가 달라진다.
- **예시:** Cache key로 path를 사용할 때는 어떤 identity를 원하는지 …

#### 3. path

- **뜻:** /b`, absolute path 변환은 문자열 형태를 바꿀 수 있지만 실제 filesystem에서 같은 object를 가리킨다는 사실은 symlink, mount, case behavior에 따라 단순하지 않다.
- **예시:** /b`, absolute path 변환은 문자열 형태를 바꿀 수 …

`a/../b`, `.

 

 

---

## CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다

### 시작 전 용어집

#### 1. symlink resolution

- **뜻:** 보안 검사를 위해 “resolved path가 허용 directory 안에 있다”고 확인한 뒤 나중에 다시 file을 열면 그 사이 symlink가 바뀌거나 filesystem state가 변할 수 있다.
- **왜 중요한가:** 이 두 operation이 분리되면 TOCTOU race가 가능하다.
- **예시:** check path -> filesystem changes -> open path

#### 2. time-of-check

- **뜻:** 고신뢰 코드에서는 directory handle 기반 open, no-follow option, OS-specific primitive처럼 검사와 사용을 더 가깝게 묶는 방법을 검토한다.
- **왜 중요한가:** Path 문자열 비교만으로 sandbox boundary를 만들지 않는다.
- **예시:** check path -> filesystem changes -> open path

```text
check path -> filesystem changes -> open path
```

 

 Filesystem은 동적인 namespace다.

---

**검증 시나리오 P78-C6 — CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다**
`CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다` 검증은 호출 순서를 단순화하는 데서 시작한다. P78-C6에서는 `CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다`에 대해 순서 하나만 뒤집어 차이를 본다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다`의 호출 전후의 상태 전이를 번호로 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P78-C6의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 06 · symlink resolution은 time-of-check와 time-of-use 문제를 만들 수 있다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 07 · boundary validation은 path syntax보다 capability를 제한하는 쪽이 강하다

### 시작 전 용어집

#### 1. boundary validation

- **뜻:** Upload filename이나 archive entry를 받는 API는 `.
- **왜 중요한가:** `, absolute path, separator를 검사할 수 있다.
- **예시:** Upload filename이나 archive entry를 받는 API는 `.

#### 2. path

- **뜻:** 더 강한 설계는 caller에게 arbitrary path를 받지 않고 logical file ID를 받아 server-controlled directory에 mapping하거나, 이미 열린 directory capability를 기준으로 relative access만 허용하는 것이다.
- **왜 중요한가:** Path validation 후에는 permission, file type, symlink, race도 고려한다.
- **예시:** 더 강한 설계는 caller에게 arbitrary path를 받지 않고 …

#### 3. capability

- **뜻:** 하지만 blacklist만으로 모든 플랫폼 corner case를 막기 어렵다.
- **왜 중요한가:** “문자열이 안전해 보인다”와 “원하는 file만 접근 가능하다”는 다른 주장이다.
- **예시:** 하지만 blacklist만으로 모든 플랫폼 corner case를 막기 어렵다.

. 


 

---

## CHAPTER 08 · path contract는 representation·identity·trust boundary를 함께 정한다

### 시작 전 용어집

#### 1. path

- **뜻:** Path API를 설계할 때 입력으로 `str`/`PathLike`를 어디까지 허용할지, relative path 기준 directory가 무엇인지, symlink를 따라갈지, 반환값이 original spelling인지 canonical path인지 명확히 한다.
- **왜 중요한가:** `, absolute path, symlink, nonexistent path, bytes/text 경계, platform separator 차이를 포함한다.
- **예시:** Path API를 설계할 때 입력으로 `str`/`PathLike`를 어디까지 허용할지, …

#### 2. representation

- **뜻:** 이 PART의 핵심은 **path를 문자열로 축소하지 않고, OS namespace를 가리키는 representation과 실제 filesystem identity, 그리고 접근 capability를 서로 분리해 설계하는 것**이다.
- **왜 중요한가:** Security-critical path operation은 unit test뿐 아니라 실제 filesystem fixture에서 검증한다.
- **예시:** 이 PART의 핵심은 **path를 문자열로 축소하지 않고, OS …

테스트에는 `..

---

## 실전 학습 루프 · PathLike와 filesystem boundary

### 1. 쉬운 예

함수 인자가 문자열 경로만 받는다고 생각하면 `pathlib.Path` 같은 객체와의 상호운용을 놓친다. 반대로 path를 받는 순간에는 문자열 변환보다 실제 파일시스템 경계, canonicalization, race, 권한을 함께 고려해야 한다.

### 2. 한 줄 해석

PathLike는 경로 표현 protocol이고, 실제 파일 접근의 안전성은 별도의 filesystem 정책이 결정한다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
from pathlib import Path

def size_of(path):
    p = Path(path)
    return p.stat().st_size

print(size_of(Path('.')))
```

### 4. 수정 실습

1. 사용자 입력을 기준 디렉터리에 붙인 뒤 `..` 탈출 가능성을 검사한다.
2. 존재 확인 후 open 사이에 파일이 바뀌는 TOCTOU 가능성을 생각한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

`Path.resolve()` 한 번이면 모든 파일 경로 보안 문제가 끝날까?

### 6. 정답과 오답 설명

**정답:** 아니다. symlink, 권한, race, open 방식과 허용 루트 정책까지 함께 본다.

**자주 나오는 오답:** 경로 문자열 정규화와 실제 파일 객체에 대한 권한 검증을 같은 것으로 보는 것이 흔한 오답이다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · PathLike·filesystem boundary

### 증상에서 시작한다

검증 단계에서는 안전했던 경로가 실제 open 시 다른 파일을 가리키거나 허용 루트 밖으로 빠져나간다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

raw path, resolved path, symlink chain, open 대상 inode/metadata와 권한을 가능한 범위에서 연결한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

검증 직후 symlink target을 바꾸는 TOCTOU 상황을 만들고 path string 재검사만으로 막히는지 본다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

허용 디렉터리 정책과 실제 열린 파일 객체 사이의 경계가 일치하고 race를 허용하지 않는 API를 사용해야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

