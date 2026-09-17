# PART 07 · Module·import·실행 환경 — 코드 경계와 의존성을 재현 가능하게 만들기

파일 하나가 커지면 코드를 여러 module과 package로 나누게 된다. 이때 단순히 `.py` 파일을 여러 개 만드는 것보다 중요한 것은 이름 공간, import 시 실행되는 코드, 의존성 방향, 실행 진입점, 환경 차이를 관리하는 것이다. 같은 소스가 다른 환경에서 다르게 동작한다면 문제는 문법보다 **어떤 코드와 설정이 실제로 선택되었는지**에 있을 가능성이 높다.

---

## CHAPTER 01 · module은 파일 분할보다 namespace와 초기화 경계다

Python module은 관련된 이름을 하나의 namespace에 묶고 import를 통해 재사용할 수 있게 한다. 함수, 클래스, 상수, 다른 module reference가 module namespace에 존재한다. 따라서 module을 나누는 목적은 줄 수를 줄이는 것보다 **같이 변경되는 개념과 공개 인터페이스를 묶는 것**에 가깝다.

`pricing.py`에 가격 정책과 금액 계산이 있고 `storage.py`에 데이터 저장 책임이 있다면 변경 이유가 분리된다. 반대로 `utils.py` 하나에 문자열, 파일, 날짜, DB, 네트워크 helper가 계속 추가되면 namespace는 있지만 개념 경계는 없다. module 이름이 어떤 책임을 포함하고 포함하지 않는지 설명할 수 있어야 한다.

Module 수준의 이름은 import한 다른 코드에서 접근할 수 있으므로 사실상 public API 후보가 된다. 내부 helper와 외부에서 사용해도 되는 기능을 구분하고, 호출자가 module 내부 자료구조에 직접 의존하지 않게 하면 나중에 구현을 바꾸기 쉬워진다. 공개 API의 안정성은 함수 이름뿐 아니라 반환 타입과 exception contract까지 포함한다.

Module은 초기화 경계이기도 하다. import 시 top-level statement가 실행되기 때문에 module 안에서 네트워크 연결, 파일 쓰기, 긴 계산 같은 부작용을 즉시 수행하면 단순 import가 예상하지 못한 행동을 일으킬 수 있다. 정의와 설정을 중심으로 두고 실제 효과는 명시적 함수 호출이나 진입점에서 수행하는 편이 제어하기 쉽다.

---

## CHAPTER 02 · import는 소스 텍스트를 복사하는 것이 아니라 module object를 찾고 초기화한다

`import package.module`을 실행하면 Python의 import system이 module을 찾고 필요한 경우 코드를 로드·실행해 module object를 준비한 뒤 현재 namespace에 이름을 바인딩한다. 같은 module을 반복 import할 때 매번 처음부터 실행되는 단순 include 방식으로 이해하면 singleton-like module state와 cache 동작을 설명하기 어렵다.

Import 시점에 top-level code가 실행된다는 사실은 전역 객체 생성과 설정 읽기에 직접 영향을 준다. 테스트에서 environment variable을 바꿨는데 이미 import된 module의 설정이 바뀌지 않는 이유가 될 수 있다. Import side effect가 많으면 실행 순서와 테스트 순서에 코드가 민감해진다.

`from module import name`과 `import module`은 현재 namespace에 어떤 이름이 바인딩되는지 다르다. 후자는 `module.name`처럼 출처가 드러나고, 전자는 짧지만 동일 이름 충돌 가능성이 커진다. Wildcard import는 어떤 이름이 들어왔는지 추적하기 어렵게 만들 수 있어 공개 API가 명확한 경우가 아니면 피하는 편이 좋다.

Import 문제를 디버깅할 때는 “파일이 존재한다”만 확인하지 않는다. 실제로 어떤 module object가 로드됐는지, `__file__`이 어디를 가리키는지, import search path와 실행 환경이 무엇인지 본다. 동일한 이름의 package가 여러 환경에 설치되어 있으면 예상과 다른 코드가 선택될 수 있다.

---

## CHAPTER 03 · `__name__ == "__main__"`은 정의와 프로그램 시작을 분리한다

Python 파일은 직접 실행될 수도 있고 다른 module에서 import될 수도 있다. 직접 실행된 module과 import된 module은 `__name__` 값이 달라질 수 있으며, 이를 이용해 함수·클래스 정의와 실행 진입 동작을 분리한다.

```python
def main():
    run_application()

if __name__ == "__main__":
    main()
```

이 구조에서는 다른 test나 module이 함수를 import해도 애플리케이션이 즉시 실행되지 않는다. Import가 정의를 제공하고 명시적 entry point가 effectful startup을 담당한다. 작은 script에서도 이 분리는 재사용성과 테스트 가능성을 높인다.

`main()`이 모든 로직을 직접 수행할 필요는 없다. Command-line argument parsing, configuration loading, dependency construction 같은 startup responsibility를 처리한 뒤 실제 업무 함수에 값과 객체를 전달하는 composition root 역할을 할 수 있다. 그러면 core logic은 process 환경에 덜 의존한다.

Entry point를 여러 개 제공하는 package라면 각 진입점이 같은 domain layer를 재사용할 수 있다. CLI, web server, batch job이 각각 process interface는 달라도 계산과 validation 규칙을 공유하는 식이다. 실행 방식과 핵심 로직을 분리하면 인터페이스가 늘어도 규칙 복제가 줄어든다.

---

## CHAPTER 04 · package 구조는 의존성 방향을 표현해야 한다

Package는 module을 계층적으로 조직하지만 directory 깊이가 곧 좋은 architecture를 의미하지는 않는다. 핵심은 어떤 layer가 어떤 layer를 import할 수 있는지 방향을 정하는 것이다. UI가 domain logic을 호출하고 domain이 storage implementation을 직접 import하는 식으로 의존성이 뒤섞이면 변경이 연쇄적으로 퍼질 수 있다.

의존성 방향을 안정적인 정책 쪽으로 향하게 하면 외부 기술을 바꾸기 쉬워진다. 예를 들어 domain service가 구체적 SQLite class를 import하기보다 필요한 repository protocol을 받고 startup code가 실제 구현을 주입하도록 할 수 있다. 작은 프로젝트에서는 과한 계층이 부담이지만, 외부 I/O와 핵심 계산을 구분하는 정도만으로도 효과가 크다.

Package name은 기술명보다 책임을 설명하는 편이 유지보수에 유리한 경우가 많다. `controllers`, `services`, `helpers`만으로는 실제 업무 개념을 찾기 어려울 수 있다. 주문, 사용자, 가격 같은 domain boundary와 infrastructure boundary를 적절히 결합하면 파일 위치가 의미를 갖는다.

Import graph는 architecture의 실제 증거다. 문서에는 layer가 나뉘어 있어도 아래 계층이 위 계층을 import한다면 코드 의존성은 반대다. 순환 import가 자주 나타나는 구조는 두 module의 책임이 잘못 나뉘었거나 공통 abstraction의 위치가 부적절하다는 신호일 수 있다.

---

## CHAPTER 05 · 순환 import는 단순 문법 오류보다 초기화 순서 문제를 드러낸다

Module A가 B를 import하고 B가 다시 A를 import하면 두 module이 아직 초기화 중인 상태에서 서로의 이름을 요구할 수 있다. 어떤 이름은 이미 정의되었고 어떤 이름은 아직 실행되지 않았기 때문에 “부분 초기화된 module” 문제가 나타날 수 있다. 파일이 모두 존재해도 import 순서에 따라 attribute를 찾지 못하는 이유다.

순환 import를 `import` 문 위치를 함수 안으로 옮겨 회피할 수 있는 상황도 있지만 구조적 원인을 먼저 본다. A와 B가 서로를 알아야 한다면 실제로 하나의 책임인지, 공통 타입을 더 낮은 module로 이동해야 하는지, callback이나 protocol을 통해 방향을 끊을 수 있는지 검토한다.

Type annotation 때문에 runtime import cycle이 생기는 경우도 있다. Annotation 평가 방식과 typing 전용 import를 활용해 runtime dependency와 type-checking dependency를 구분할 수 있다. 하지만 이런 기법 역시 architecture를 감추는 용도로 남용하지 않는다.

좋은 dependency graph는 가능한 한 방향성이 선명하다. 낮은 수준 utility가 높은 수준 feature를 import하지 않고, domain rule이 UI에 의존하지 않으며, composition root가 여러 구현을 한 곳에서 연결한다. 순환 import는 이 방향성이 깨졌다는 실행 시점 신호가 될 수 있다.

---

## CHAPTER 06 · 실행 환경은 코드 밖의 숨은 입력을 제공한다

같은 Python 소스가 개발자 PC에서는 동작하고 다른 machine에서는 실패하는 이유 중 상당수는 환경 차이다. Python version, 설치된 dependency version, environment variable, current working directory, locale, timezone, filesystem permission이 프로그램 behavior에 영향을 준다. 이런 요소는 코드에 보이지 않지만 실제 입력이다.

환경 의존성을 줄이려면 필요한 설정을 startup에서 읽고 검증한 뒤 명시적 configuration object로 전달한다. 함수 깊숙한 곳에서 매번 environment variable을 직접 읽으면 어떤 값이 필요한지 찾기 어렵고 테스트마다 process environment를 바꿔야 한다. 입력을 한 경계에서 정규화하면 핵심 로직은 환경과 분리된다.

Current working directory에 상대 경로를 묵시적으로 의존하는 코드도 흔한 문제다. 실행 위치가 바뀌면 파일을 찾지 못할 수 있다. Resource path가 package와 상대적인지, 사용자 작업 directory와 상대적인지 계약을 명확히 하고 적절한 path API를 사용한다.

Timezone과 locale은 특히 조용한 데이터 오류를 만든다. 날짜 parsing, 문자열 정렬, 숫자 표시가 machine 설정에 따라 달라질 수 있다. 저장과 계산 기준을 명시적으로 정하고 표시 단계에서 사용자 locale로 변환하는 구조가 재현성을 높인다.

---

## CHAPTER 07 · virtual environment는 dependency 집합을 프로젝트 단위로 격리한다

한 machine에 여러 프로젝트가 있고 서로 다른 library version을 요구하면 전역 Python 환경 하나로 관리하기 어렵다. Virtual environment는 interpreter와 설치 package를 프로젝트 context에 맞게 분리해 dependency 충돌을 줄인다. 핵심은 폴더 하나가 생긴다는 사실보다 **어떤 실행이 어떤 package 집합을 보는지**를 통제하는 것이다.

Environment를 활성화했다고 해서 재현 가능성이 자동 보장되지는 않는다. 정확히 어떤 dependency version을 설치할지 기록하지 않으면 시간이 지나 새 version이 선택되어 behavior가 달라질 수 있다. Direct dependency와 transitive dependency의 관계를 이해하고 프로젝트의 packaging 도구가 제공하는 lock 또는 constraint 전략을 사용한다.

Dependency를 무조건 최신으로 고정하는 것과 영원히 낡은 version에 묶는 것 모두 문제가 있다. Upgrade를 의도적인 변경으로 만들고 test를 통과시킨 뒤 반영하는 흐름이 중요하다. Security fix와 compatibility change를 추적할 수 있어야 한다.

Runtime environment를 확인할 때 shell에서 보이는 `python`, IDE가 사용하는 interpreter, test runner가 사용하는 interpreter가 서로 다를 수 있다. 실제 process의 executable path와 설치 package source를 확인하면 “설치했는데 import가 안 된다” 같은 문제를 빠르게 좁힐 수 있다.

---

## CHAPTER 08 · dependency는 편의 기능이 아니라 공급망과 호환성 계약이다

외부 library를 추가하면 직접 작성할 코드가 줄어들지만 새로운 의존성의 API, versioning, security, license, transitive dependency를 받아들이게 된다. 두 줄짜리 helper를 위해 거대한 package를 추가하는 결정과 표준화된 복잡한 기능을 검증된 library에 맡기는 결정은 비용 구조가 다르다.

Dependency를 평가할 때는 필요한 기능, 유지보수 상태, release policy, 지원 Python version, security update 경로, package source를 본다. 이름이 비슷한 악성 package나 탈취된 계정을 통한 공급망 공격 가능성도 있으므로 출처 확인이 중요하다. Lockfile이나 hash verification 같은 메커니즘은 설치 결과를 더 통제 가능하게 만든다.

Library API에 프로그램 전체가 직접 의존하면 교체 비용이 커질 수 있다. 외부 service client나 serialization library처럼 변경 가능성이 있는 기술은 좁은 adapter 경계에 두면 core logic이 vendor API와 직접 결합되는 범위를 줄일 수 있다. 반대로 Python 표준 타입처럼 안정적이고 광범위한 abstraction을 억지로 감싸는 것은 불필요한 계층을 만든다.

Dependency update는 기능 추가와 별개의 change로 다루는 편이 문제 원인을 찾기 쉽다. Version bump와 대규모 refactor를 한 commit에 섞으면 regression이 어느 변화에서 왔는지 좁히기 어렵다. 재현 가능한 환경은 단순 설치 편의가 아니라 변경을 검증 가능한 단위로 만드는 기반이다.

---

## CHAPTER 09 · configuration과 secret은 source code와 다른 lifetime을 가진다

API endpoint, timeout, feature flag 같은 configuration은 환경에 따라 달라질 수 있지만 프로그램 logic과 구분해야 한다. Password와 token 같은 secret은 더욱 강한 보호가 필요하다. 소스 코드나 Git history에 secret을 넣으면 파일을 지워도 과거 commit에 남을 수 있다.

Configuration은 startup 시점에 schema와 범위를 검증한다. 필수 값이 없거나 timeout이 음수라면 요청 처리 중간까지 진행한 뒤 실패하기보다 process 시작 단계에서 명확하게 거부하는 편이 운영하기 쉽다. 문자열 environment variable을 domain type으로 변환하는 것도 이 경계에서 수행한다.

Secret은 로그와 exception에도 노출되지 않게 한다. 객체의 `repr`에 token이 포함되어 debug log에 찍히거나 HTTP header 전체를 기록하면서 credential이 저장되는 경우가 있다. Secret 값을 일반 configuration과 같은 방식으로 아무 곳에서나 출력 가능한 문자열로 다루면 위험하다.

Configuration change가 runtime에 즉시 반영되어야 하는지 restart가 필요한지도 계약이다. 동적 reload를 지원하면 consistency와 동시성 문제가 추가된다. 작은 프로그램에서는 immutable startup configuration이 더 단순하고 안전할 수 있다.

---

## CHAPTER 10 · 재현 가능한 실행은 source·dependency·configuration·entry point를 함께 고정한다

“내 코드에서는 된다”를 벗어나려면 소스 commit만으로는 부족할 수 있다. 어떤 Python version과 dependency set을 사용했고, 어떤 configuration schema를 적용했으며, 어떤 command/entry point로 실행했는지 알아야 한다. 외부 데이터까지 결과에 영향을 준다면 그 버전이나 snapshot도 필요할 수 있다.

Bug report에는 최소 재현 입력과 함께 실행 환경을 기록한다. Python version, package version, OS, 관련 configuration, stack trace가 있으면 같은 상태를 만들 가능성이 높아진다. 단순 screenshot보다 machine-readable version 정보와 exact command가 강한 증거가 된다.

자동화된 test와 CI는 이 환경을 반복적으로 만드는 도구지만 도구 자체가 재현성을 보장하지 않는다. Dependency가 매번 floating latest를 설치하거나 외부 service의 현재 상태에 의존하면 같은 commit에서도 결과가 달라질 수 있다. 결정적이어야 하는 입력과 변동 가능 입력을 구분한다.

Module과 environment 설계의 최종 목적은 폴더를 깔끔하게 만드는 것이 아니다. 코드가 **어디서 정의되고, 어떤 dependency에 연결되며, 어떤 환경 입력을 받아, 어느 진입점에서 실행되는지** 추적 가능하게 만드는 것이다. 이 구조가 잡히면 테스트·배포·디버깅 단계에서 원인 범위를 크게 줄일 수 있다.