# PART 41 · Import system internals — module cache·finder·loader·package 초기화를 추적하기

`import x`는 파일 내용을 현재 위치에 붙여 넣는 동작이 아니다. Python은 module 이름을 해석하고, 이미 로드된 module인지 확인하고, finder와 loader를 통해 code를 찾고 실행한 뒤 module object를 cache에 연결한다. 이 과정 때문에 import 순서, package initialization, circular dependency, test isolation이 서로 연결된다. 핵심은 **module 이름·module object·source 위치·초기화 상태를 서로 다른 것으로 보는 것**이다.

---

## CHAPTER 01 · import의 첫 입력은 파일 경로가 아니라 완전한 module 이름이다

`import package.submodule`에서 프로그램이 요청하는 것은 특정 문자열 경로가 아니라 Python module namespace 안의 이름이다. Import machinery는 이 이름을 기준으로 이미 로드된 객체를 찾고, 없으면 search path와 finder를 사용해 module spec을 구한다. Source file은 그 결과 중 하나일 뿐이다.

같은 source file을 서로 다른 module name으로 두 번 로드하면 서로 다른 module object와 global state가 생길 수 있다. Entry script를 직접 실행했을 때 `__main__`으로 존재하면서 package import로 다시 로드되는 문제도 이런 identity 차이와 연결된다.

Module identity가 singleton처럼 사용되는 registry, cache, class identity에 영향을 주면 duplicate loading은 단순 성능 문제가 아니다. `isinstance`가 기대와 다르게 실패하거나 plugin registry가 두 개 생길 수 있다.

Import 문제를 조사할 때 path만 보지 않고 `__name__`, `__package__`, module spec, 실제 source 위치를 함께 본다.

---

## CHAPTER 02 · module cache는 같은 이름의 초기화 결과를 재사용한다

한 번 성공적으로 import된 module은 보통 module cache에서 재사용된다. 따라서 import 문을 반복한다고 top-level initialization code가 매번 다시 실행되는 일반 함수 호출과 같지 않다. Module-level singleton과 registry가 import 한 번으로 만들어지는 이유다.

이 cache는 test에서 configuration 변경을 어렵게 할 수 있다. Environment variable을 바꾼 뒤 module을 다시 import해도 이미 계산된 module global이 그대로 남을 수 있다. 설정을 import-time에 계산하지 않고 explicit initialization 단계로 옮기면 test와 reload가 단순해진다.

Cache를 직접 제거하고 re-import하는 방법은 가능하지만 다른 module이 이전 object reference를 계속 보유할 수 있다. 완전한 process 초기 상태를 재현하려면 isolated subprocess가 더 정확한 경우가 있다.

Module cache는 성능 최적화인 동시에 runtime identity contract다. Application logic이 reload 가능성을 요구한다면 단순 import semantics보다 더 명시적인 lifecycle을 설계한다.

---

## CHAPTER 03 · import 중 module이 cache에 먼저 등록될 수 있어 부분 초기화 상태가 존재한다

Circular import를 처리하려면 module이 완전히 실행되기 전에 cache에 placeholder-like 상태로 등록될 수 있다. A가 B를 import하고 B가 다시 A를 import하면 B는 완성된 A가 아니라 아직 top-level code가 끝나지 않은 module object를 볼 수 있다.

그래서 `partially initialized module` 오류는 두 파일이 서로를 참조한다는 사실보다 **어느 이름이 어느 시점에 아직 정의되지 않았는가**가 핵심이다. Function definition 아래쪽에 만들어지는 이름을 상대 module이 너무 일찍 요구하면 실패한다.

Import를 함수 안으로 이동해 실행 시점을 늦추면 cycle을 피할 수 있지만 구조적 dependency가 사라진 것은 아니다. 공통 abstraction을 더 낮은 module로 이동하거나 dependency direction을 재설계하는 편이 장기적으로 낫다.

Circular import test는 clean process에서 수행한다. 기존 test process에 module cache가 남아 있으면 초기화 순서 bug가 가려질 수 있다.

---

## CHAPTER 04 · package의 `__init__`는 namespace 진입점이자 실행 code다

Package를 import하면 package initializer가 실행될 수 있다. 이 파일에서 많은 submodule을 eager import하거나 network/config 작업을 수행하면 단순 `import package`의 비용과 실패 가능성이 커진다. Package initializer는 public namespace 정리와 가벼운 metadata 중심으로 유지하는 편이 예측 가능하다.

Top-level re-export를 사용하면 사용자는 `package.PublicClass`처럼 안정된 경로를 사용할 수 있고 내부 파일 구조를 바꿔도 public API를 유지하기 쉽다. 하지만 re-export graph가 복잡하면 import cycle과 startup cost가 생길 수 있다.

Optional dependency를 package initializer에서 무조건 import하면 해당 feature를 사용하지 않는 사용자도 설치를 요구받는다. Optional feature module에서 필요한 시점에 dependency를 확인하는 구조가 더 적합할 수 있다.

Package initialization은 application startup architecture와 연결된다. Import만으로 resource를 열기보다 명시적 `create_app()`이나 composition root가 external effect를 시작하게 한다.

---

## CHAPTER 05 · absolute import와 relative import는 dependency 위치를 표현하는 방식이 다르다

Absolute import는 project namespace에서 출처를 명확히 보여 주고 package 구조 변경 시 긴 경로가 바뀔 수 있다. Relative import는 같은 package 내부 관계를 간결하게 표현하지만 현재 module의 package context가 필요하다. 파일을 직접 실행할 때 relative import가 실패하는 이유가 될 수 있다.

Library package 내부에서는 일관된 정책을 정한다. 동일 package 이름의 third-party module과 local module이 있을 때 ambiguous import를 줄인다.

`sys.path`를 runtime에 임의 수정해 import를 맞추는 패턴은 현재 working directory와 실행 방식에 민감하다. Package를 올바르게 설치하고 entry point를 사용하는 구조가 재현 가능하다.

Notebook와 IDE가 path를 자동 조정할 수 있어 production과 다른 import 결과가 나올 수 있다. 실제 executable과 module search path를 diagnostic에 남기면 환경 차이를 좁힐 수 있다.

---

## CHAPTER 06 · finder와 loader는 “어디서 찾고 어떻게 module로 만들지”를 분리한다

Python import system은 module spec을 찾는 finder와 source/bytecode/extension에서 module을 준비·실행하는 loader 개념을 가진다. 일반 application이 custom importer를 직접 작성할 일은 드물지만 zip import, plugin, generated module 같은 기능을 이해하는 기반이다.

Custom import hook는 모든 import에 개입할 수 있어 startup과 security에 큰 영향을 준다. Remote source를 자동 다운로드해 import하는 방식은 code execution trust boundary를 network까지 넓힌다.

Import hook를 사용하는 framework에서는 hook 설치 시점 이전/이후 import 결과가 달라질 수 있다. Instrumentation agent가 import를 가로채 patch하는 경우 order가 behavior다.

Debugging에서는 module spec과 loader type을 확인해 source file인지 native extension인지, generated namespace인지 구분한다. 같은 import syntax 뒤에 다른 loading mechanism이 존재할 수 있다.

---

## CHAPTER 07 · bytecode cache는 source 실행 결과가 아니라 compile 중간 결과를 재사용한다

Python implementation은 source를 compile한 bytecode cache를 저장해 다음 startup의 parsing/compile 비용을 줄일 수 있다. 이 cache는 module의 runtime global state snapshot이 아니다. Import하면 code object가 다시 실행되어 module object와 globals가 새 process에서 만들어진다.

Cache invalidation은 source timestamp/hash와 runtime version 같은 조건에 의존할 수 있다. Stale cache를 수동으로 관리하기보다 interpreter가 제공하는 mechanism을 사용한다.

배포 artifact에 bytecode를 포함할 수 있어도 source compatibility와 Python version/ABI 조건을 확인한다. Bytecode를 source-independent stable distribution format으로 간주하지 않는다.

Startup performance를 개선하려고 import 수를 줄일 때 compile cache보다 top-level side effect와 heavy dependency import가 더 큰 비용일 수 있다. Profile로 import time을 측정한다.

---

## CHAPTER 08 · dynamic import는 plugin과 lazy dependency를 가능하게 하지만 정적 추론을 어렵게 한다

Module 이름을 문자열로 받아 runtime에 import하면 plugin discovery와 optional feature loading을 구현할 수 있다. 그러나 type checker와 code search가 dependency를 쉽게 찾지 못하고 잘못된 이름 오류가 실행할 때까지 늦어진다.

Untrusted user가 arbitrary module name을 선택하게 하면 application에 설치된 code의 예상하지 못한 import-time side effect를 실행시킬 수 있다. 허용 plugin registry나 namespace prefix를 사용해 import capability를 제한한다.

Dynamic import error를 optional dependency missing과 plugin 내부 exception으로 구분한다. Module 자체를 찾지 못한 경우와 module top-level code가 다른 dependency import에서 실패한 경우를 같은 “plugin 없음”으로 처리하면 결함을 숨긴다.

Lazy import는 startup을 줄일 수 있지만 첫 요청 latency와 failure 시점을 뒤로 옮긴다. Critical path에서는 warm-up 단계에서 미리 load해 fail-fast할 수 있다.

---

## CHAPTER 09 · import time side effect는 dependency graph를 실행 순서 graph로 바꾼다

Module top level에서 registry 등록, database 연결, logging 설정을 수행하면 dependency를 import하는 순서가 system configuration을 결정할 수 있다. A가 먼저 import됐을 때와 B가 먼저 import됐을 때 handler 우선순위가 달라지는 구조는 재현하기 어렵다.

정의와 registration을 분리해 composition root가 명시적 순서로 연결하면 import graph와 application startup graph를 분리할 수 있다. Declarative plugin metadata를 먼저 읽고 load order를 계산한 뒤 import하는 방식도 가능하다.

Testing에서 module import만으로 global registry가 오염되면 test isolation이 깨진다. Registry instance를 명시적으로 만들고 module이 factory를 제공하게 한다.

Import-time code는 모든 tool에도 영향을 준다. Static documentation generator나 migration command가 module metadata를 읽으려 import했다가 production service 연결을 시도하는 문제를 피한다.

---

## CHAPTER 10 · import 문제는 name identity·cache·initialization order 세 축으로 조사한다

`ModuleNotFoundError`만 보고 package를 다시 설치하기 전에 실제 interpreter, search path, requested module name을 확인한다. Attribute가 없으면 partially initialized module인지, 잘못된 duplicate module이 로드됐는지, version 차이인지 본다.

`module.__file__`과 distribution metadata를 비교하면 예상 package와 실제 import된 code가 같은지 확인할 수 있다. Local file이 third-party package 이름을 shadowing하는 경우도 빠르게 발견한다.

Import cycle은 graph로 그리고 edge마다 “타입 힌트만 필요한가, runtime object가 필요한가, startup side effect 때문에 필요한가”를 구분하면 끊을 지점이 보인다.

Import system을 깊게 배우는 목적은 custom loader를 매일 작성하기 위해서가 아니다. **한 줄 import 뒤에서 module identity·cache·source discovery·초기화 순서가 어떻게 연결되는지 이해해 환경 의존성과 circular dependency를 구조적으로 진단하는 것**이다.