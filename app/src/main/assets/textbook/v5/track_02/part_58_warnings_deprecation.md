# PART 58 · Warning과 deprecation — 현재 호환성을 유지하면서 미래 변경을 전달하기

모든 잘못을 즉시 exception으로 막는 것이 좋은 API evolution은 아니다. 오래 사용된 parameter 이름이나 method를 바꾸려면 기존 호출이 한동안 계속 동작하면서 사용자에게 migration 신호를 보내야 할 수 있다. Warning은 **현재 실행은 허용하지만 이 사용법이 문제가 있거나 미래에 제거될 수 있다는 별도 진단 채널**이다. 잘 설계된 deprecation은 메시지를 출력하는 데서 끝나지 않고 도입·관측·전환·제거까지 명시적인 lifecycle을 가진다.

---

## CHAPTER 01 · warning은 성공과 실패 사이의 제3상태가 아니라 진단 채널이다

함수가 정상 result를 반환하면서 warning을 발생시킬 수 있기 때문에 warning은 return value나 exception과 다른 축이다. Deprecated option을 사용해도 현재 version에서는 결과를 만들 수 있지만 caller가 아무 조치 없이 계속 의존하면 미래 version에서 깨질 수 있다. 이 정보를 stderr 문자열 출력으로 흩뜨리지 않고 warning mechanism을 사용하면 category와 filter, test가 가능해진다.

Warning을 validation failure 대신 사용하면 안 되는 경우도 많다. 금액이 음수라서 계약을 수행할 수 없다면 경고 후 계속 계산하는 것보다 exception이 적합하다. 반대로 오래된 alias가 아직 완전히 지원되고 정확한 결과를 내지만 migration이 필요하다면 warning이 자연스럽다. 핵심은 현재 contract가 성공 가능한지 여부다.

운영 application에서는 warning이 최종 사용자에게 직접 보일 필요가 없을 수도 있다. Library author와 application developer가 migration 신호를 받을 수 있도록 test/CI에서 수집하고 production에서는 logging policy에 맞게 처리할 수 있다.

---

## CHAPTER 02 · warning category는 원인과 대상 사용자에 따라 다르게 분류한다

Python warning system은 deprecation, runtime concern, resource issue처럼 여러 category를 구분할 수 있다. Category가 있으면 application이 특정 종류만 error로 승격하거나 무시하지 못하게 강제할 수 있다. 모든 진단을 `UserWarning` 하나로 내보내면 filter policy가 거칠어진다.

Library public API deprecation과 내부 개발자용 deprecation의 default visibility가 다를 수 있으므로 현재 Python warning category semantics를 확인한다. 사용자에게 실제 migration이 필요한 public API라면 일반 실행과 test에서 충분히 보이는 category를 선택해야 한다. 단순 구현 세부사항 변경을 모든 caller에게 경고하면 noise가 커진다.

Custom warning subclass를 만들어 domain별 정책을 제공할 수도 있다. 예를 들어 `LegacyConfigWarning`을 정의하면 application이 config migration warning만 CI failure로 처리할 수 있다. 그러나 category hierarchy를 지나치게 세분화하면 사용자가 모든 타입을 알아야 하므로 실제 행동 차이가 있을 때만 나눈다.

---

## CHAPTER 03 · warning filter는 반복·위치·category에 따라 진단량을 통제한다

같은 deprecated function이 loop에서 만 번 호출된다고 warning 만 개가 유용한 것은 아니다. Warning filter는 동일 위치/메시지/category의 반복을 한 번만 보여 주거나 특정 module을 무시하고, CI에서는 error로 바꾸는 정책을 적용할 수 있다. 진단 system 자체가 application output을 과도하게 오염시키지 않도록 한다.

개발 환경에서는 deprecation warning을 최대한 보이게 하고 test suite에서는 unexpected warning을 failure로 만드는 전략이 유용하다. 반대로 production에서 third-party library가 수천 warning을 내며 log를 채우면 signal-to-noise가 나빠진다. Dependency upgrade 계획과 함께 filter를 조정한다.

Warning filter를 global mutable state로 test가 바꾸면 다음 test에 영향을 줄 수 있다. Context manager나 test framework fixture를 사용해 scope가 끝난 뒤 원래 filter로 복원한다. Warning 검증도 execution context의 일부다.

---

## CHAPTER 04 · `stacklevel`은 library 내부가 아니라 caller 위치를 가리키게 한다

Deprecated helper 내부에서 warning을 발생시키면 기본 traceback 위치가 library implementation line을 가리킬 수 있다. 사용자에게 필요한 것은 “내 코드의 어느 호출을 바꿔야 하는가”이므로 적절한 stack level을 지정해 external caller 위치를 표시하는 것이 migration usability를 크게 높인다.

Wrapper가 여러 층인 경우 고정 stack level 하나가 항상 정확하지 않을 수 있다. Public boundary 수를 줄이거나 warning helper가 call stack을 조심스럽게 계산할 수 있다. 하지만 지나친 introspection보다 architecture를 단순화하는 편이 낫다.

Stack location은 단순 장식이 아니다. IDE와 test output이 수정해야 할 source line으로 바로 이동할 수 있게 하므로 대규모 codebase migration 비용을 줄인다. Warning message에는 old API와 replacement, 제거 예정 version을 함께 적으면 caller가 documentation을 다시 찾는 시간을 줄일 수 있다.

---

## CHAPTER 05 · deprecation에는 도입·공존·제거의 시간 계획이 필요하다

API를 deprecated 표시한 뒤 다음 patch version에서 즉시 삭제하면 warning을 본 사용자가 대응할 시간이 없다. Public library는 release cadence와 compatibility policy에 맞춰 최소 한두 release cycle 또는 명시된 기간 동안 old/new path를 함께 지원할 수 있다. 조직 내부 API도 consumer 수와 배포 독립성을 기준으로 window를 정한다.

Deprecation 기간에는 old path가 새 path와 같은 semantics를 제공해야 한다. Wrapper가 parameter 변환을 잘못해 결과가 미묘하게 달라지면 사용자는 migration 전에 이미 bug를 겪는다. Old API contract test를 유지하면서 implementation을 new API adapter로 위임하면 중복 logic을 줄일 수 있다.

제거 예정 version을 무기한 미루면 deprecated code가 영구 분기로 남아 test matrix와 유지보수 비용이 늘어난다. Usage metric이나 source search로 remaining caller를 확인하고 계획된 major/minor release에서 실제로 제거한다.

---

## CHAPTER 06 · compatibility adapter는 old shape를 new shape로 한 방향 변환한다

`old_connect(host, timeout_seconds)`가 새 API `connect(Endpoint(host), Timeout(...))`으로 바뀌었다면 old function에서 input을 새 domain type으로 변환하고 새 implementation 하나만 호출하게 할 수 있다. 두 API가 각자 전체 logic을 유지하면 bug fix가 한쪽에만 들어갈 위험이 있다.

Adapter는 old semantics를 정확히 재현해야 한다. Old default가 30초였는데 new default가 10초라면 argument가 생략되었을 때 explicit 30초를 전달해 compatibility를 유지해야 한다. 단순 signature forwarding으로는 behavior compatibility가 보장되지 않는다.

Old input이 new model로 완전히 표현되지 않는다면 migration이 breaking change라는 뜻이다. Warning message와 release note에서 손실되는 behavior를 명확히 설명하고 automatic adapter가 임의의 선택을 하지 않게 한다.

---

## CHAPTER 07 · deprecation test는 warning과 old behavior를 동시에 검증한다

Deprecated call을 실행했을 때 expected warning category와 message가 발생하는지 확인하고, 동시에 현재 지원 기간 동안 return value와 side effect가 기존 contract를 유지하는지 test한다. Warning만 나고 기능이 이미 깨져 있다면 deprecation window의 의미가 없다.

Replacement API를 호출한 경우에는 warning이 나오지 않아야 한다. 그렇지 않으면 사용자가 migration을 완료해도 CI가 계속 실패한다. Stack level을 test해 warning filename/line이 caller fixture를 가리키는지 검증할 수도 있다.

제거 release에서는 반대로 old symbol이 더 이상 export되지 않거나 명시적인 error를 내는 test로 바뀐다. Deprecation lifecycle에 맞춰 test도 상태 전이를 가진다. Warning test를 영구히 남겨 제거를 방해하지 않는다.

---

## CHAPTER 08 · API 제거는 code deletion보다 consumer contract 종료를 의미한다

Deprecated 함수 한 개를 지우기 전에 public import path, docs, type stub, examples, CLI option, config key, serialized schema에서 동일 contract가 노출되는 곳을 확인한다. 코드 symbol은 없어졌는데 documentation이 계속 old call을 생성하면 migration이 완료된 것이 아니다.

Version control history와 release note에 replacement와 migration rule을 남기면 오래된 deployment를 upgrade하는 사용자가 변경 이유를 찾을 수 있다. Automated codemod가 가능한 단순 rename이라면 도구를 제공할 수 있고 의미 변경이 있다면 manual review가 필요하다.

Warning/deprecation 설계의 핵심은 **과거 코드를 무기한 보존하는 것이 아니라 consumer가 변경을 발견하고 안전하게 이동할 시간을 제공한 뒤 old contract를 실제로 제거하는 관리 가능한 전환 protocol을 만드는 것**이다.