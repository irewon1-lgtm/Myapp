# PART 60 · Reusable library capstone — public API·domain type·adapter·compatibility를 한 package로 연결하기

재사용 가능한 library는 application 내부 helper 모음과 다르다. 호출자가 어떤 환경에서 어떤 방식으로 import할지 통제할 수 없고, 한번 공개한 함수 이름·parameter·exception·serialization 의미가 다른 codebase의 dependency가 된다. 따라서 내부 구현보다 **public surface를 작게 고정하고 외부 입력·side effect·version change를 경계에서 통제하는 것**이 중요하다. 여기서는 “규칙 기반 text transformer” library를 예로 들어 Track 02의 주요 개념을 하나의 package 구조로 결합한다.

---

## CHAPTER 01 · library contract는 기능 목록보다 입력·출력·실패·side effect를 먼저 정의한다

예제 library가 text와 transformation rule을 받아 결과를 만든다고 하자. 가장 먼저 `transform(text, rules) -> TransformResult`라는 public contract를 고정한다. Text encoding은 이미 Python `str`로 decode된 상태를 요구하고, rule은 검증된 value object를 받으며, filesystem과 network에는 접근하지 않는 pure core로 정의할 수 있다.

실패도 분류한다. 잘못된 rule은 `InvalidRule`, 지원하지 않는 strategy는 `UnsupportedStrategy`, 내부 invariant 위반은 public domain failure와 다른 unexpected exception으로 본다. Caller가 입력을 고쳐야 하는지 library bug를 report해야 하는지 구분할 수 있어야 한다.

Performance boundary도 일부 contract가 될 수 있다. Input 최대 길이를 문서화하거나 streaming API를 별도로 제공해 거대한 text를 일반 function에 넣지 않게 한다. “문자열을 변환한다” 한 문장보다 허용 상태와 resource expectation을 구체적으로 고정한다.

---

## CHAPTER 02 · public import surface는 내부 module layout과 분리한다

Package 내부를 `core.py`, `models.py`, `errors.py`, `plugins.py`로 나누더라도 caller가 모든 내부 path를 직접 import하게 둘 필요는 없다. Stable top-level namespace에서 `transform`, `Rule`, `TransformResult`, public exceptions만 export하면 내부 파일을 리팩터링해도 caller import를 유지하기 쉽다.

`from package.internal.engine import ...` 같은 path가 documentation과 example에 퍼지면 internal module이 사실상 public API가 된다. Private naming과 `__all__`, documentation을 이용해 지원 surface를 명확히 한다. 완벽히 접근을 막는 것이 목적이 아니라 compatibility를 약속하는 범위를 줄이는 것이다.

Public object를 re-export할 때 circular import와 initialization side effect도 조심한다. Top-level import는 가볍고 deterministic하게 유지하고 network 연결이나 plugin scan 같은 무거운 작업을 import 시점에 실행하지 않는다.

---

## CHAPTER 03 · domain type은 raw dict보다 잘못된 rule 상태를 줄인다

Caller가 `{"mode":"regex","pattern":...}` 같은 dict를 넘기게 하면 key 오타와 missing field를 runtime 깊숙이 처리해야 한다. `Rule` value object와 strategy enum을 사용해 생성 단계에서 허용 field와 invariant를 검증하면 core transformation은 이미 유효한 rule만 다룰 수 있다.

Regex rule과 literal rule이 서로 다른 data를 요구한다면 하나의 object에 optional field를 많이 두기보다 variant class로 나눌 수 있다. `LiteralRule(old,new)`과 `RegexRule(pattern,replacement,flags)`처럼 각 상태가 필요한 값만 가지게 한다. Type union과 pattern matching 또는 dispatch가 자연스럽게 연결된다.

Rule object를 immutable하게 만들면 caller와 library가 동시에 같은 object를 공유해도 실행 도중 설정이 바뀌지 않는다. Cached compiled pattern을 넣고 싶다면 logical value와 transient cache를 분리해 equality와 serialization 의미를 보호한다.

---

## CHAPTER 04 · filesystem·plugin 같은 effect는 optional adapter로 바깥에 둔다

Core library가 file path를 직접 열게 만들면 encoding, permission, path security가 transform logic과 결합된다. `transform_text()`는 str만 받고, convenience adapter `transform_file(input_path, output_path, encoding=...)`가 file lifetime과 atomic write를 책임지게 한다. Caller는 필요에 따라 pure core만 사용할 수 있다.

Plugin strategy도 core registry interface 뒤에 둔다. Built-in strategy는 정적으로 등록하고 third-party plugin discovery는 optional module에서 수행한다. Library import만으로 설치된 모든 third-party code를 실행하지 않게 하고 caller가 명시적으로 plugin loading을 선택하도록 할 수 있다.

이 구조에서는 side effect layer가 실패해도 pure transformation test와 분리할 수 있다. File error는 adapter exception으로, rule error는 domain exception으로 남아 failure vocabulary도 명확하다.

---

## CHAPTER 05 · configuration과 error message는 caller 환경을 침범하지 않게 설계한다

Library가 module import 시 environment variable을 읽어 global timeout이나 mode를 결정하면 동일 process의 여러 caller가 서로 다른 설정을 사용할 수 없다. 필요한 option은 function parameter나 immutable configuration object로 받는다. Application이 environment/file을 읽고 library config로 변환하는 것이 더 재사용 가능하다.

Error object에는 machine-readable code와 safe context를 포함할 수 있지만 logger configuration을 library가 강제로 바꾸지 않는다. Library는 warning/exception을 발생시키고 최종 logging format과 destination은 application이 결정하게 한다. Root logger에 handler를 추가하거나 log level을 global로 바꾸는 것은 host application의 정책을 침범할 수 있다.

Secret이나 원문 전체를 exception message에 넣지 않는다. Pattern 자체가 민감할 수 있다면 message에는 rule ID와 위치만 포함하고 debug context는 caller가 opt-in하도록 설계한다.

---

## CHAPTER 06 · package resource는 built-in rule/schema를 source tree 경로 없이 제공한다

Library가 기본 Unicode mapping이나 JSON schema file을 포함한다면 `../data/file` 같은 source-relative path로 읽지 않는다. Package resource API를 사용해 installed wheel에서도 같은 logical data를 찾게 하고 release test에서 artifact에 실제 file이 포함됐는지 검증한다.

Resource format에도 version을 둔다. Library 2.0이 schema meaning을 바꾸면서 같은 filename을 사용하면 old cache와 혼동될 수 있다. Package version과 resource schema version의 관계를 명시한다.

Resource를 native library에 path로 전달해야 해 temporary materialization을 사용한다면 context lifetime 안에서 native operation을 완료한다. Temporary path를 public result로 반환해 caller가 scope 밖에서 사용하게 만들지 않는다.

---

## CHAPTER 07 · compatibility gate는 old caller와 installed artifact를 함께 검증한다

Library test는 current API unit test만으로 끝나지 않는다. 이전 minor release의 대표 call signature를 fixture로 보관해 여전히 binding되는지, deprecated call은 warning을 내면서 같은 결과를 주는지 확인한다. Public exception code와 enum wire value도 snapshot contract가 될 수 있다.

Packaging clean-pass에서는 wheel을 build해 빈 virtual environment에 설치하고 public import와 package resource를 실제로 사용한다. Source checkout에서만 보이는 file과 editable-install path가 release artifact에 없는 문제를 잡는다.

Type-checking fixture도 compatibility layer가 된다. Caller sample code를 static checker에 통과시키면 annotation 변경 때문에 source는 실행되지만 typed consumer가 깨지는 문제를 발견할 수 있다. Runtime, typing, packaging을 서로 다른 gate로 본다.

---

## CHAPTER 08 · reusable library architecture는 `작은 core + 명시적 boundary + 좁은 public surface`로 정리된다

최종 구조에서 core는 immutable domain type과 pure transformation을 소유하고, adapter는 file/resource/plugin 같은 effect를 처리하며, top-level package는 안정된 public API만 export한다. Configuration은 caller가 명시적으로 전달하고 failure는 domain error와 infrastructure error로 구분된다. Packaging metadata와 resource는 release artifact test에서 검증한다.

이 구조의 목적은 파일을 여러 개 만드는 것이 아니다. 내부 algorithm을 바꾸거나 plugin mechanism을 교체해도 caller가 의존하는 contract가 유지되고, breaking change가 필요하면 warning/deprecation window를 통해 계획적으로 이동할 수 있게 하는 것이다.

Reusable library를 만드는 핵심은 **다른 사람이 내 내부 구현을 모르고도 안전하게 사용할 수 있도록 호출 가능한 상태 공간을 좁히고, side effect와 environment dependency를 가장자리로 밀어내며, 공개한 계약을 version과 test로 유지하는 것**이다.