# PART 93 · CSV schema boundary — dialect·newline·header·type conversion을 분리하기

CSV는 단순히 쉼표로 문자열을 나누는 형식이 아니다. Quoting, embedded newline, delimiter, header, encoding이 실제 record boundary를 결정하고 모든 field는 기본적으로 text로 들어온다. Spreadsheet로 다시 내보낼 때는 formula-like value가 실행 의미를 얻을 수도 있다. 이 PART에서는 **parse 규칙과 domain schema를 분리하고 large file을 streaming으로 검증하는 방법**을 다룬다.

---

## CHAPTER 01 · CSV record boundary는 `line.split(',')`로 보존되지 않는다

Quoted field 안에는 delimiter와 newline이 들어갈 수 있다.

```text
id,note
1,"first line
second line"
```

이 데이터를 물리적 line 단위로 나누면 하나의 logical record를 둘로 찢게 된다. 표준 csv parser가 quoting state를 관리하도록 맡겨야 한다.

Parser가 record를 만들었다고 schema까지 검증된 것은 아니다. Column 수, header 의미, field type은 다음 단계에서 확인한다.

---

## CHAPTER 02 · dialect는 delimiter 하나가 아니라 quoting과 escaping 규칙의 묶음이다

CSV producer마다 comma, tab, semicolon을 쓰거나 quote/escape 정책이 다를 수 있다. `csv` module의 dialect는 이런 parse 규칙을 함께 표현한다.

자동 sniffing은 편리하지만 작은 sample이나 비정상 data에서 잘못 추론할 수 있다. 계약된 import format이라면 dialect를 명시하는 편이 더 재현 가능하다.

사용자 업로드에서 여러 dialect를 지원한다면 감지 결과를 기록하고 ambiguity 시 사용자에게 format을 선택하게 하는 정책도 고려한다.

---

## CHAPTER 03 · file newline layer와 CSV parser의 newline 처리를 겹치지 않게 한다

CSV는 quoted field 안의 newline을 직접 해석해야 하므로 text file을 열 때 newline handling을 parser 기대와 맞춰야 한다. Platform text translation이 먼저 개입하면 embedded newline과 writer output이 달라질 수 있다.

Python csv 사용에서는 공식 API가 권장하는 file opening 방식을 따른다. “Windows에서 한 줄씩 빈 줄이 생긴다” 같은 현상은 parser가 아니라 newline layer를 중복 처리한 결과일 수 있다.

Encoding과 newline은 서로 다른 층이다. UTF-8 decode가 성공했다고 record framing이 올바른 것은 아니다.

---

## CHAPTER 04 · header는 이름 목록이 아니라 input schema의 첫 계약이다

`DictReader`가 header를 key로 사용하면 편리하지만 중복 header, 빈 이름, 예상하지 못한 column을 어떻게 할지 결정해야 한다.

```python
required = {"user_id", "amount"}
missing = required - set(reader.fieldnames or ())
if missing:
    raise ImportSchemaError(missing)
```

Unknown column을 무시할지 거부할지에 따라 forward compatibility가 달라진다. Financial import처럼 오독이 위험하면 strict schema가 적합하고, analytics ingest는 extra column을 보존할 수 있다.

Column order가 의미인지 이름이 의미인지도 format version에 명시한다.

---

## CHAPTER 05 · CSV field는 text이므로 domain type conversion과 validation을 별도 단계에서 수행한다

Parser가 `"0012"`를 읽었다고 이것이 정수 12인지 identifier `0012`인지 알 수 없다. Domain schema가 타입 의미를 결정한다.

```python
user_id = row["user_id"]          # leading zero 보존
amount = Decimal(row["amount"])  # 금액 정책에 맞는 변환
```

빈 문자열, whitespace, localized number, timezone이 포함된 날짜를 어떻게 처리할지도 명시한다. `int()`가 성공한다는 사실과 business-valid value라는 사실은 다르다.

Conversion error에는 row number와 column name을 붙여야 대용량 import를 수정하기 쉽다.

---

## CHAPTER 06 · spreadsheet export에서는 formula injection을 별도 trust boundary로 본다

CSV field가 `=`, `+`, `-`, `@` 같은 문자로 시작하면 일부 spreadsheet application이 이를 formula로 해석할 수 있다. Untrusted user text를 CSV로 export한 뒤 사람이 spreadsheet에서 열면 새로운 실행/외부 참조 surface가 생길 수 있다.

따라서 spreadsheet consumption이 예상되는 export는 위험한 leading pattern을 escape하거나 text field로 강제하는 정책을 둔다. 정확한 escaping 방식은 target application과 데이터 보존 요구를 함께 고려한다.

Import parser security와 export viewer security는 다른 boundary다.

---

## CHAPTER 07 · large CSV는 record 단위 streaming으로 처리해 memory와 error budget을 제한한다

전체 파일을 list로 만든 뒤 검증하면 file 크기만큼 memory를 사용한다. Record iterator를 순차 처리하면 bounded memory로 import할 수 있다.

하지만 중간 record에서 실패했을 때 이미 앞선 row를 DB에 commit했다면 partial import가 된다. Batch transaction, staging table, validate-then-publish 같은 전략을 선택한다.

Error를 모두 수집한다면 최대 error count를 두어 malicious file이 수백만 error object를 만들지 않게 한다. Streaming은 memory뿐 아니라 failure accumulation도 제한해야 한다.

---

## CHAPTER 08 · CSV contract는 parsing dialect와 domain schema를 독립적으로 고정한다

안정적인 CSV interface는 encoding/newline/dialect, header policy, field type, missing/extra column, row error policy, formula-safe export를 명시한다.

테스트에는 quoted comma, embedded newline, duplicate header, leading zero ID, empty field, 매우 큰 file, spreadsheet formula-like text를 포함한다. `split(',')` 기반 happy path test로는 CSV boundary를 검증할 수 없다.

이 PART의 핵심은 **CSV를 쉼표 문자열이 아니라 stateful record format으로 다루고, parser가 만든 text record 위에 별도의 domain schema와 export security 정책을 적용하는 것**이다.
