# PART 19 · Packaging·dependency·version — 코드를 재현 가능한 배포 단위로 만들기

로컬 폴더에서 실행되는 Python 코드와 다른 machine에서 설치 가능한 package 사이에는 metadata, dependency resolution, build artifact, versioning, import layout이라는 계약이 있다. Packaging을 마지막에 zip 파일을 만드는 작업으로 보면 환경 차이와 공급망 문제가 늦게 나타난다. 핵심은 **어떤 source가 어떤 artifact가 되고, 어떤 dependency 집합과 Python version에서 같은 interface를 제공하는지**를 명시하는 것이다.

---

## CHAPTER 01 · distribution package와 import package는 이름이 같을 수도 다를 수도 있다

`pip` 같은 installer가 다루는 distribution package 이름과 Python 코드에서 `import`하는 package namespace는 개념적으로 다르다. 하나의 distribution이 여러 import package를 제공할 수 있고 배포 이름과 import 이름이 다를 수도 있다. 설치 오류를 조사할 때 “pip에는 있는데 import가 안 된다”는 현상을 이 두 namespace로 분리한다.

Project metadata는 이름, version, supported Python, dependency, entry point를 installer와 tooling에 전달한다. Source directory 구조만으로 모든 정보가 결정되는 것은 아니다. Build backend와 configuration이 어떤 files를 artifact에 포함하는지도 확인해야 한다.

Local source tree가 import path에 우연히 포함되어 개발 환경에서는 동작하지만 built wheel에는 파일이 누락되는 경우가 있다. 실제 artifact를 만들어 깨끗한 environment에 설치해 import/test하는 검증이 필요한 이유다.

Namespace package와 plugin architecture처럼 여러 distribution이 하나의 logical namespace를 구성하는 고급 구조도 있다. 하지만 작은 프로젝트에서는 단순하고 명시적인 package layout이 debugging과 배포에 유리하다.

---

## CHAPTER 02 · `pyproject` 계열 metadata는 build tool과 project contract를 분리한다

현대 Python packaging에서는 project metadata와 build system configuration을 declarative file에 두어 어떤 backend가 source를 artifact로 만드는지 명시할 수 있다. 핵심은 특정 syntax를 외우는 것이 아니라 build가 developer machine의 숨은 상태에 의존하지 않게 하는 것이다.

Build dependency와 runtime dependency는 역할이 다르다. Package를 만드는 데만 필요한 tool이 최종 application 실행에도 설치될 필요는 없다. 반대로 runtime library가 build environment에 우연히 존재해서 artifact 생성은 성공했지만 metadata에 누락되면 사용자 환경에서 import error가 날 수 있다.

Optional dependency group은 개발, test, documentation, 특정 feature를 분리할 수 있다. 모든 사용자가 무거운 optional library를 설치하게 만들지 않으면서 기능별 extra를 제공할 수 있다. 하지만 group이 너무 세분화되면 조합 compatibility를 검증해야 하는 범위가 커진다.

Metadata 변경도 source code 변경과 같은 review 대상이다. Supported Python 범위와 dependency upper/lower bound가 실제 test matrix와 일치해야 한다.

---

## CHAPTER 03 · semantic versioning은 숫자 규칙보다 호환성 약속을 관리하는 방식이다

Version number는 사용자와 dependency resolver에게 어떤 변경이 들어갔는지 힌트를 준다. Semantic versioning은 public API의 incompatible change, backward-compatible feature, bug fix를 major/minor/patch로 구분하는 관례를 제공하지만 실제 호환성을 자동으로 판정해 주지는 않는다.

함수 parameter 이름 변경, exception type 변경, CLI option default 변경도 사용자가 의존하고 있다면 breaking change가 될 수 있다. Internal refactor는 behavior가 유지되면 version impact가 작을 수 있다. 무엇이 public contract인지 먼저 정의해야 version policy가 의미 있다.

Pre-release와 development version을 사용하면 안정 release 전에 새 behavior를 시험할 수 있지만 dependency resolver가 이를 어떻게 선택하는지 이해해야 한다. Version 문자열을 임의 비교하면 `1.10`과 `1.9` 순서를 잘못 처리할 수 있으므로 표준 version parser를 사용한다.

한 조직 내부 application은 엄격한 semantic version을 사용하지 않을 수도 있지만 artifact와 migration을 추적할 안정된 build identity는 필요하다. Git commit, build number, schema version을 연결하면 incident 당시 실행 중인 코드를 재현하기 쉽다.

---

## CHAPTER 04 · dependency constraint는 “최신 설치”와 “재현 가능한 설치” 사이의 정책이다

`library>=2`처럼 넓은 범위는 새 patch/minor를 자동으로 받을 수 있지만 미래 release가 behavior를 바꾸면 같은 source에서 다른 설치 결과가 생길 수 있다. Exact lock은 재현성을 높이지만 security fix와 compatibility update를 의도적으로 반영해야 한다. Library package와 deployable application도 적합한 전략이 다를 수 있다.

재사용 library가 transitive dependency까지 exact pin하면 다른 package와 resolution conflict를 만들 수 있다. Application은 실제 배포 환경을 재현하기 위해 더 강한 lock을 사용하는 편이 자연스러울 수 있다. Constraint의 주체가 누구인지 구분한다.

Dependency solver가 선택한 최종 graph를 artifact나 lock file로 기록하면 시간이 지나도 같은 환경을 만들기 쉽다. “requirements에 direct dependency 세 개만 적혀 있음”은 실제 설치된 수십 개 transitive version을 완전히 설명하지 않는다.

Upgrade는 자동으로 섞이기보다 별도 change로 review하고 tests를 수행한다. Security advisory가 있는 dependency는 우선순위를 높이되 breaking update의 migration도 함께 계획한다.

---

## CHAPTER 05 · wheel과 source distribution은 설치 시 수행되는 작업이 다르다

Source distribution은 source와 build metadata를 제공해 사용자 환경에서 build가 필요할 수 있고, wheel은 많은 경우 바로 설치 가능한 built distribution을 제공한다. Native extension이 포함되면 platform, CPU architecture, Python ABI와 호환되는 wheel tag가 중요하다.

Pure Python package는 portability가 높지만 package data file이 artifact에 포함되었는지 확인해야 한다. Template, schema, static resource를 source tree에만 두고 build configuration에서 제외하면 개발에서는 보이지만 설치 후 사라진다.

Native dependency build는 compiler와 system library가 필요할 수 있어 설치 실패 원인이 application code 밖에 있다. Supported platform에 prebuilt wheel을 제공하면 사용자 build complexity를 줄일 수 있지만 artifact supply chain과 release automation을 관리해야 한다.

Release 전에는 built artifact 내용을 검사하고 clean environment에 설치해 smoke test한다. Source checkout에서의 test와 installed artifact test는 다른 failure class를 잡는다.

---

## CHAPTER 06 · editable install은 개발 편의가 artifact 검증을 대신하지 않는다

Editable install은 source 파일 변경이 바로 import에 반영되어 개발 loop를 빠르게 한다. 하지만 실제 wheel 설치와 path/layout behavior가 다를 수 있다. 개발 환경에서 editable로만 테스트하면 build artifact 누락과 metadata 문제를 놓칠 수 있다.

IDE가 repository root를 자동으로 Python path에 넣는 경우도 비슷하다. 잘못된 relative import가 local에서는 우연히 성공하고 clean install에서는 실패할 수 있다. 테스트가 실제 package import path를 사용하도록 environment를 통제한다.

Release gate에서는 fresh virtual environment를 만들고 wheel을 설치한 뒤 public import, CLI entry point, resource loading을 검증한다. 이 단계는 unit test를 반복하는 것이 아니라 packaging boundary를 검증하는 별도의 CLEAN PASS다.

개발 편의 기능 자체를 피할 필요는 없다. 다만 편의 환경과 실제 사용자 환경의 차이를 알고 release 전에 그 차이를 제거하는 검증을 둔다.

---

## CHAPTER 07 · plugin entry point와 dynamic discovery는 설치된 code가 실행 경로에 참여하게 한다

Plugin system은 main application이 구체 module 이름을 미리 알지 못해도 설치된 distribution의 metadata를 통해 extension을 발견할 수 있게 한다. 이는 확장성을 높이지만 신뢰하지 않은 package가 runtime behavior에 참여하는 공급망 경계가 된다.

Plugin interface에는 supported API version과 capability를 명시한다. Main application이 interface를 변경할 때 old plugin을 어떻게 거부하거나 compatibility adapter를 제공할지 정책이 필요하다. 단순 import 성공만으로 semantic compatibility가 보장되지 않는다.

Discovery 시 모든 plugin을 import하면 startup latency와 side effect가 커질 수 있다. Metadata로 후보를 찾고 실제 사용 시 lazy load할 수 있지만 오류가 늦게 나타난다. Startup validation과 lazy load 사이 trade-off를 선택한다.

Plugin isolation이 필요한 보안 환경에서는 같은 process에 임의 plugin code를 import하는 방식이 적합하지 않을 수 있다. 별도 process와 IPC로 boundary를 강화하는 구조를 고려한다.

---

## CHAPTER 08 · supply-chain 검증은 package 이름과 version을 신뢰하는 것에서 시작하지 않는다

Dependency 설치는 외부 code를 application 안으로 가져오는 행위다. Typosquatting, compromised maintainer account, malicious release, dependency confusion처럼 package source 자체가 공격면이 될 수 있다. 정확한 registry와 package identity를 확인하고 organization 정책에 따라 allowlist나 internal mirror를 사용할 수 있다.

Lock file과 hash verification은 예상한 artifact가 설치되는지 통제하는 데 도움을 준다. 그러나 hash가 맞다는 사실은 artifact 내용이 안전하다는 뜻이 아니라 “검토한 바로 그 artifact”라는 identity를 보장하는 한 층이다. Vulnerability scanning과 maintainer/repository 검토가 별도로 필요할 수 있다.

Build process가 network에서 임의 script를 받아 실행하거나 version이 고정되지 않은 tool을 사용하는지 확인한다. Reproducible build를 높일수록 공급망 사건에서 어떤 입력이 artifact에 들어갔는지 추적하기 쉽다.

Dependency 수를 줄이는 것도 공격면과 update burden을 줄이는 전략이다. 하지만 보안에 민감한 암호 구현을 직접 작성하는 것처럼 검증된 library를 피하는 것이 더 위험한 경우도 있다. 기능의 전문성과 유지 비용을 함께 판단한다.

---

## CHAPTER 09 · package API는 import path까지 호환성 계약이 될 수 있다

사용자가 `from package.foo import Bar`를 코드에 작성하면 `package.foo.Bar` 경로 자체가 public contract가 될 수 있다. 내부 파일을 이동하는 refactor가 behavior를 바꾸지 않아도 import path가 깨지면 사용자에게 breaking change다. Public symbols를 안정된 top-level namespace로 re-export하는 전략을 사용할 수 있다.

`__all__`이나 documentation으로 public surface를 명확하게 하면 내부 helper에 외부 code가 우연히 의존하는 범위를 줄일 수 있다. Python은 접근을 완전히 막는 private keyword가 없더라도 naming과 documentation으로 계약 경계를 표현한다.

Deprecation은 즉시 삭제보다 migration 기간을 제공한다. Warning에 대체 API와 제거 예정 version을 포함하면 사용자가 준비할 수 있다. 너무 오래 모든 legacy path를 유지하면 내부 복잡성이 커지므로 정책에 따라 sunset한다.

API compatibility test로 대표 import path와 signature를 snapshot하거나 type checker fixture를 사용할 수 있다. 사용자가 실제로 의존하는 surface를 자동 검증하는 것이다.

---

## CHAPTER 10 · release artifact는 source commit보다 더 많은 provenance를 가져야 한다

운영 환경에서 어떤 코드가 실행 중인지 알려면 source commit뿐 아니라 build tool version, dependency lock, Python version, artifact digest, build configuration을 연결할 수 있어야 한다. 같은 commit도 다른 dependency와 compiler 환경에서 다른 artifact가 만들어질 수 있다.

Artifact에 version metadata를 포함하고 runtime diagnostic에서 안전하게 조회할 수 있게 하면 incident와 release를 연결하기 쉽다. Container나 package registry의 immutable digest를 기록하면 tag가 움직여도 실제 bytes를 식별할 수 있다.

Release pipeline에서 test result와 artifact를 같은 source revision에 묶는다. 테스트한 뒤 source를 다시 build하면서 dependency가 달라지면 “검증한 artifact”와 “배포한 artifact”가 다를 수 있다. Build once, promote same artifact 같은 원칙이 이 차이를 줄인다.

Packaging의 최종 목적은 설치 명령을 성공시키는 것이 아니다. **source와 dependency, build environment, public interface를 하나의 추적 가능한 실행 단위로 고정해 다른 환경에서도 같은 프로그램을 얻는 것**이다.