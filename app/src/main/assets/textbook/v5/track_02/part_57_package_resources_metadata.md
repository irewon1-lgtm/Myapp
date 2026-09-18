# PART 57 · Package resource와 metadata — 설치 위치에 의존하지 않고 data file을 찾기

개발 중에는 source tree 옆의 `data/schema.json`을 상대 경로로 열어도 잘 동작할 수 있다. 하지만 wheel로 설치되거나 zip-like loader, 다른 working directory, test environment에서 실행되면 source checkout 구조와 실제 package resource 위치가 달라질 수 있다. Package와 함께 배포되는 template·schema·기본 설정을 다루려면 **filesystem path와 logical package resource를 분리하고 build artifact에 실제로 포함되었는지 검증**해야 한다.

---

## CHAPTER 01 · package resource는 현재 working directory가 아니라 설치된 package에 소속된다

### 시작 전 용어집

#### 1. package

- **뜻:** Package resource는 특정 import package에 속한 논리적 data로 보고 package-aware resource API를 통해 찾는 편이 안정적이다.
- **왜 중요한가:** IDE가 repository root를 자동 path에 넣어 주면 source checkout에서는 file이 보이지만 실제 wheel 설치 후에는 누락될 수 있다.
- **예시:** Package resource는 특정 import package에 속한 논리적 data로 …

#### 2. resource

- **뜻:** Runtime code가 source directory layout을 암묵적으로 가정하지 않게 만들고, resource identity를 package namespace와 연결한다.
- **왜 중요한가:** 사용자가 선택한 외부 input file과 application 내부 resource도 구분한다.
- **예시:** Runtime code가 source directory layout을 암묵적으로 가정하지 않게 …

#### 3. working directory

- **뜻:** txt")`는 현재 process의 working directory를 기준으로 path를 해석한다.
- **왜 중요한가:** CLI를 repository root에서 실행할 때는 성공해도 다른 directory에서 실행하면 실패할 수 있다.
- **예시:** txt")`는 현재 process의 working directory를 기준으로 path를 해석한다.

#### 4. process

- **뜻:** 전자는 실제 filesystem path와 permission이 public contract이고, 후자는 package 설치 방식이 바뀌어도 같은 logical resource로 접근되어야 한다.
- **예시:** 전자는 실제 filesystem path와 permission이 public contract이고, 후자는 …

`open("templates/default.  

이 차이는 test에서도 드러난다.  

 

---

## CHAPTER 02 · resource object를 항상 일반 `Path`로 취급할 수 있는 것은 아니다

### 시작 전 용어집

#### 1. resource

- **뜻:** Package loader에 따라 resource가 실제 디렉터리의 독립 파일이 아닐 수 있다.
- **왜 중요한가:** Zip archive 내부나 custom loader에서 제공되는 resource는 일반 OS path가 없을 수 있다.
- **예시:** Package loader에 따라 resource가 실제 디렉터리의 독립 파일이 …

#### 2. path

- **뜻:** 외부 library가 실제 filesystem path만 받는다면 resource를 temporary file로 materialize하는 context가 필요할 수 있다.
- **왜 중요한가:** 이 temporary path는 context가 끝난 뒤 사라질 수 있으므로 native library가 path를 장기간 보관하면 lifetime이 깨진다.
- **예시:** 외부 library가 실제 filesystem path만 받는다면 resource를 temporary …

#### 3. package

- **뜻:** 따라서 package resource API가 제공하는 read/open abstraction을 사용하고, 무조건 `str(resource)`를 native library에 넘길 수 있다고 가정하지 않는다.
- **왜 중요한가:** Materialization과 consumer lifetime을 같은 scope에 둔다.
- **예시:** 따라서 package resource API가 제공하는 read/open abstraction을 사용하고, …

#### 4. API

- **뜻:** Resource abstraction을 쓰는 이유는 path 조작 문법을 줄이기 위해서가 아니라 **package storage representation을 caller에서 숨기기 위해서**다.
- **왜 중요한가:** Caller가 bytes/text만 필요하면 path로 변환하지 않고 직접 읽는 편이 더 portable하다.
- **예시:** Resource abstraction을 쓰는 이유는 path 조작 문법을 줄이기 …

---

## CHAPTER 03 · source tree에 파일이 존재해도 build artifact에 포함되지 않을 수 있다

### 시작 전 용어집

#### 1. source tree

- **뜻:** Release 검증에서는 source tree가 아니라 실제 built artifact를 clean environment에 설치한 뒤 resource를 읽는다.
- **왜 중요한가:** Wheel 내용을 직접 목록화해 예상 file이 존재하는지 확인할 수도 있다.
- **예시:** Release 검증에서는 source tree가 아니라 실제 built artifact를 …

#### 2. build artifact

- **뜻:** README, JSON schema, SQL migration, template가 repository에 있다고 해서 wheel/sdist에 자동으로 들어간다고 가정하면 안 된다.
- **왜 중요한가:** Build configuration이 어떤 package data를 포함하는지 별도 규칙을 가진다.
- **예시:** README, JSON schema, SQL migration, template가 repository에 있다고 …

#### 3. configuration

- **뜻:** 개발 환경 test만 통과하고 사용자 설치에서 resource missing error가 나는 대표 원인이다.
- **왜 중요한가:** 이 검증은 unit test와 다른 packaging failure class다.
- **예시:** 개발 환경 test만 통과하고 사용자 설치에서 resource missing …

#### 4. package

- **뜻:** Test fixture, credential sample, internal report가 실수로 package에 들어가지 않도록 allowlist 관점으로 package data를 관리한다.
- **왜 중요한가:** “필요한 것이 빠지지 않음”과 “불필요한 것이 들어가지 않음”을 둘 다 검사한다.
- **예시:** Test fixture, credential sample, internal report가 실수로 package에 …

불필요한 file을 과도하게 포함하면 artifact 크기와 정보 노출이 증가할 수 있다.  

---

## CHAPTER 04 · materialized resource는 temporary file lifetime과 cleanup 계약을 가진다

### 시작 전 용어집

#### 1. materialized resource

- **뜻:** Package resource가 실제 path가 아닐 때 일부 API는 temporary directory에 file을 추출해 path를 제공할 수 있다.
- **왜 중요한가:** 이 path는 context manager scope 동안만 유효할 수 있다.
- **예시:** Package resource가 실제 path가 아닐 때 일부 API는 …

#### 2. temporary file

- **뜻:** Scope 밖으로 path 문자열만 반환하면 caller가 나중에 이미 삭제된 file을 열려고 할 수 있다.
- **왜 중요한가:** 따라서 path-consuming operation 자체를 resource context 안에서 수행하거나 caller에게 context manager를 반환한다.
- **예시:** Scope 밖으로 path 문자열만 반환하면 caller가 나중에 이미 …

#### 3. lifetime

- **뜻:** 단순 helper가 `return path`만 하면 lifetime dependency가 signature에 보이지 않는다.
- **왜 중요한가:** Resource ownership과 ExitStack 원리가 다시 등장한다.
- **예시:** 단순 helper가 `return path`만 하면 lifetime dependency가 signature에 …

#### 4. cleanup

- **뜻:** Temporary materialization에는 disk space와 permission도 필요하다.
- **왜 중요한가:** Read-only filesystem이나 제한된 container에서 실패할 수 있으므로 가능하면 bytes/file-like interface를 지원하는 library를 선택한다.
- **예시:** Temporary materialization에는 disk space와 permission도 필요하다.

Native API처럼 path가 필수인 경우에만 materialization cost를 지불한다.

---

## CHAPTER 05 · distribution metadata는 설치된 package identity와 version을 runtime에 연결한다

### 시작 전 용어집

#### 1. distribution metadata

- **뜻:** Application이 `--version`을 보여 주거나 plugin compatibility를 검사하려면 source file에 hard-coded된 version과 실제 설치 distribution metadata가 어긋나지 않게 해야 한다.
- **왜 중요한가:** Installed metadata API를 사용하면 현재 environment에 설치된 distribution version과 entry point 같은 정보를 조회할 수 있다.
- **예시:** Application이 `--version`을 보여 주거나 plugin compatibility를 검사하려면 source …

#### 2. package

- **뜻:** Runtime diagnostic에 package version을 남기면 incident의 executable state를 source commit과 연결하기 쉽다.
- **왜 중요한가:** 다만 editable install, local development, metadata missing 같은 환경도 고려한다.
- **예시:** Runtime diagnostic에 package version을 남기면 incident의 executable state를 …

#### 3. identity

- **뜻:** Build metadata에 정의된 canonical project identity를 사용한다.
- **왜 중요한가:** Version을 찾지 못했다고 core application이 반드시 실패해야 하는지는 목적에 따라 다르다.
- **예시:** Build metadata에 정의된 canonical project identity를 사용한다.

#### 4. version

- **뜻:** Distribution name과 import package name이 다를 수 있다는 점도 중요하다.
- **왜 중요한가:** `import foo`가 성공한다고 metadata distribution도 반드시 `foo`라는 이름이라고 추측하지 않는다.
- **예시:** Distribution name과 import package name이 다를 수 있다는 …

---

## CHAPTER 06 · plugin discovery metadata는 code import 이전에 extension 후보를 찾게 한다

### 시작 전 용어집

#### 1. plugin

- **뜻:** Entry point 같은 package metadata를 사용하면 core application이 모든 plugin module 이름을 hard-code하지 않고 설치된 extension 후보를 발견할 수 있다.
- **왜 중요한가:** Metadata 단계에서는 plugin name, group, object reference를 읽고 실제 필요할 때 load할 수 있다.
- **예시:** Entry point 같은 package metadata를 사용하면 core application이 …

#### 2. metadata

- **뜻:** 하지만 metadata가 존재한다고 plugin code가 안전하거나 정상이라는 뜻은 아니다.
- **왜 중요한가:** Load 시 import side effect와 third-party code execution이 발생하므로 trust policy를 별도로 둔다.
- **예시:** 하지만 metadata가 존재한다고 plugin code가 안전하거나 정상이라는 뜻은 …

#### 3. code import

- **뜻:** 이 구조는 plugin architecture의 discovery layer를 packaging system과 연결한다.
- **왜 중요한가:** Discovery와 execution을 분리하면 incompatible plugin을 load하기 전에 version/capability를 검사할 수 있다.
- **예시:** 이 구조는 plugin architecture의 discovery layer를 packaging system과 …

#### 4. extension

- **뜻:** 동일 extension name이 여러 distribution에서 제공될 때 충돌을 어떻게 처리할지 결정한다.
- **왜 중요한가:** First-found 같은 환경 의존 선택보다 duplicate를 startup error로 보고 명시적으로 resolve하는 편이 재현성이 높을 수 있다.
- **예시:** 동일 extension name이 여러 distribution에서 제공될 때 충돌을 …

---

## CHAPTER 07 · resource test는 source checkout이 아니라 installed artifact를 기준으로 한 번 더 수행한다

### 시작 전 용어집

#### 1. resource

- **뜻:** Package resource code의 unit test는 logical API가 bytes/text를 올바르게 반환하는지 빠르게 확인한다.
- **왜 중요한가:** 그러나 build inclusion 문제는 source environment test로 잡히지 않을 수 있다.
- **예시:** Package resource code의 unit test는 logical API가 bytes/text를 …

#### 2. source checkout

- **뜻:** 별도 clean-pass에서 wheel을 build하고 temporary virtual environment에 설치한 뒤 resource path와 metadata를 실제로 조회해야 한다.
- **왜 중요한가:** Case-sensitive filesystem 차이도 고려한다.
- **예시:** 별도 clean-pass에서 wheel을 build하고 temporary virtual environment에 설치한 …

#### 3. installed artifact

- **뜻:** json`이 같은 것처럼 보이더라도 Linux deployment에서 다른 이름일 수 있다.
- **왜 중요한가:** Artifact 안의 정확한 path와 code reference가 일치하는지 확인한다.
- **예시:** json`이 같은 것처럼 보이더라도 Linux deployment에서 다른 이름일 …

#### 4. package

- **뜻:** Schema file format을 바꾸면서 old application이 같은 resource name을 읽으면 의미가 깨질 수 있으므로 package version 또는 internal resource schema version을 함께 관리한다.
- **왜 중요한가:** Resource 변경이 compatibility에 영향을 주는 경우 version도 test 대상이다.
- **예시:** Schema file format을 바꾸면서 old application이 같은 resource …

개발 machine에서 `Schema.json`과 `schema. 

 

---

## CHAPTER 08 · package resource contract는 logical identity·artifact inclusion·lifetime을 고정한다

### 시작 전 용어집

#### 1. package

- **뜻:** 어느 package에 소속된 logical resource인가, build artifact에 어떤 규칙으로 포함되는가, caller가 file-like data와 실제 path 중 무엇을 필요로 하며 그 lifetime은 얼마인가.
- **왜 중요한가:** 이 세 가지가 명확하면 source checkout과 installed environment 차이를 줄일 수 있다.
- **예시:** 어느 package에 소속된 logical resource인가, build artifact에 어떤 …

#### 2. resource

- **뜻:** Package resource를 cwd-relative path로 열지 않고, 설치 metadata를 source constant와 이중 관리하지 않으며, temporary materialization path를 scope 밖으로 무심코 누출하지 않는다.
- **왜 중요한가:** Packaging과 runtime resource access를 하나의 contract로 본다.
- **예시:** Package resource를 cwd-relative path로 열지 않고, 설치 metadata를 …

#### 3. contract

- **뜻:** 내부 data file을 다룰 때 세 질문을 먼저 정한다.
- **왜 중요한가:** Package resource 설계의 핵심은 **파일을 찾는 기술이 아니라 source tree·build artifact·runtime loader가 달라져도 같은 logical data를 동일한 의미로 읽게 만드는 것**이다.
- **예시:** 내부 data file을 다룰 때 세 질문을 먼저 …
