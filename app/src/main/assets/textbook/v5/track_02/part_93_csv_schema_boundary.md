# PART 93 · CSV schema boundary — dialect·newline·header·type conversion을 분리하기

CSV는 단순히 쉼표로 문자열을 나누는 형식이 아니다. Quoting, embedded newline, delimiter, header, encoding이 실제 record boundary를 결정하고 모든 field는 기본적으로 text로 들어온다. Spreadsheet로 다시 내보낼 때는 formula-like value가 실행 의미를 얻을 수도 있다. 이 PART에서는 **parse 규칙과 domain schema를 분리하고 large file을 streaming으로 검증하는 방법**을 다룬다.

---

## CHAPTER 01 · CSV record boundary는 `line.split(',')`로 보존되지 않는다

### 시작 전 용어집

#### 1. CSV

- **뜻:** 표준 csv parser가 quoting state를 관리하도록 맡겨야 한다.
- **왜 중요한가:** Parser가 record를 만들었다고 schema까지 검증된 것은 아니다.
- **예시:** id,note / 1,"first line

#### 2. CSV record

- **뜻:** Quoted field 안에는 delimiter와 newline이 들어갈 수 있다.
- **왜 중요한가:** 이 데이터를 물리적 line 단위로 나누면 하나의 logical record를 둘로 찢게 된다.
- **예시:** id,note / 1,"first line

#### 3. boundary

- **뜻:** Column 수, header 의미, field type은 다음 단계에서 확인한다.
- **예시:** id,note / 1,"first line

```text
id,note
1,"first line
second line"
```

 

 

---

## CHAPTER 02 · dialect는 delimiter 하나가 아니라 quoting과 escaping 규칙의 묶음이다

### 시작 전 용어집

#### 1. dialect

- **뜻:** `csv` module의 dialect는 이런 parse 규칙을 함께 표현한다.
- **왜 중요한가:** 자동 sniffing은 편리하지만 작은 sample이나 비정상 data에서 잘못 추론할 수 있다.
- **예시:** `csv` module의 dialect는 이런 parse 규칙을 함께 표현한다.

#### 2. delimiter

- **뜻:** CSV producer마다 comma, tab, semicolon을 쓰거나 quote/escape 정책이 다를 수 있다.
- **왜 중요한가:** 계약된 import format이라면 dialect를 명시하는 편이 더 재현 가능하다.
- **예시:** CSV producer마다 comma, tab, semicolon을 쓰거나 quote/escape 정책이 …

#### 3. quoting

- **뜻:** 사용자 업로드에서 여러 dialect를 지원한다면 감지 결과를 기록하고 ambiguity 시 사용자에게 format을 선택하게 하는 정책도 고려한다.
- **예시:** 사용자 업로드에서 여러 dialect를 지원한다면 감지 결과를 기록하고 …

---

## CHAPTER 03 · file newline layer와 CSV parser의 newline 처리를 겹치지 않게 한다

### 시작 전 용어집

#### 1. file newline

- **뜻:** CSV는 quoted field 안의 newline을 직접 해석해야 하므로 text file을 열 때 newline handling을 parser 기대와 맞춰야 한다.
- **왜 중요한가:** Platform text translation이 먼저 개입하면 embedded newline과 writer output이 달라질 수 있다.
- **예시:** CSV는 quoted field 안의 newline을 직접 해석해야 하므로 …

#### 2. layer

- **뜻:** “Windows에서 한 줄씩 빈 줄이 생긴다” 같은 현상은 parser가 아니라 newline layer를 중복 처리한 결과일 수 있다.
- **왜 중요한가:** Encoding과 newline은 서로 다른 층이다.
- **예시:** “Windows에서 한 줄씩 빈 줄이 생긴다” 같은 현상은 …

#### 3. CSV

- **뜻:** Python csv 사용에서는 공식 API가 권장하는 file opening 방식을 따른다.
- **왜 중요한가:** UTF-8 decode가 성공했다고 record framing이 올바른 것은 아니다.
- **예시:** Python csv 사용에서는 공식 API가 권장하는 file opening …

---

## CHAPTER 04 · header는 이름 목록이 아니라 input schema의 첫 계약이다

### 시작 전 용어집

#### 1. header

- **뜻:** `DictReader`가 header를 key로 사용하면 편리하지만 중복 header, 빈 이름, 예상하지 못한 column을 어떻게 할지 결정해야 한다.
- **왜 중요한가:** Unknown column을 무시할지 거부할지에 따라 forward compatibility가 달라진다.
- **예시:** required = {"user_id", "amount"} / missing = required …

#### 2. input schema

- **뜻:** Financial import처럼 오독이 위험하면 strict schema가 적합하고, analytics ingest는 extra column을 보존할 수 있다.
- **왜 중요한가:** Column order가 의미인지 이름이 의미인지도 format version에 명시한다.
- **예시:** required = {"user_id", "amount"} / missing = required …

```python
required = {"user_id", "amount"}
missing = required - set(reader.fieldnames or ())
if missing:
    raise ImportSchemaError(missing)
```

 


---

## CHAPTER 05 · CSV field는 text이므로 domain type conversion과 validation을 별도 단계에서 수행한다

### 시작 전 용어집

#### 1. CSV

- **뜻:** Parser가 `"0012"`를 읽었다고 이것이 정수 12인지 identifier `0012`인지 알 수 없다.
- **왜 중요한가:** 빈 문자열, whitespace, localized number, timezone이 포함된 날짜를 어떻게 처리할지도 명시한다.
- **예시:** user_id = row["user_id"] # leading zero 보존 / …

#### 2. CSV field

- **뜻:** `int()`가 성공한다는 사실과 business-valid value라는 사실은 다르다.
- **왜 중요한가:** Conversion error에는 row number와 column name을 붙여야 대용량 import를 수정하기 쉽다.
- **예시:** user_id = row["user_id"] # leading zero 보존 / …

Domain schema가 타입 의미를 결정한다.

```python
user_id = row["user_id"]          # leading zero 보존
amount = Decimal(row["amount"])  # 금액 정책에 맞는 변환
```

 


---

## CHAPTER 06 · spreadsheet export에서는 formula injection을 별도 trust boundary로 본다

### 시작 전 용어집

#### 1. spreadsheet export

- **뜻:** CSV field가 `=`, `+`, `-`, `@` 같은 문자로 시작하면 일부 spreadsheet application이 이를 formula로 해석할 수 있다.
- **왜 중요한가:** Untrusted user text를 CSV로 export한 뒤 사람이 spreadsheet에서 열면 새로운 실행/외부 참조 surface가 생길 수 있다.
- **예시:** 따라서 spreadsheet consumption이 예상되는 export는 위험한 leading pattern을 …

#### 2. formula injection

- **뜻:** 따라서 spreadsheet consumption이 예상되는 export는 위험한 leading pattern을 escape하거나 text field로 강제하는 정책을 둔다.
- **왜 중요한가:** 정확한 escaping 방식은 target application과 데이터 보존 요구를 함께 고려한다.
- **예시:** 따라서 spreadsheet consumption이 예상되는 export는 위험한 leading pattern을 …

#### 3. trust boundary

- **뜻:** Import parser security와 export viewer security는 다른 boundary다.
- **예시:** Import parser security와 export viewer security는 다른 boundary다.

---

## CHAPTER 07 · large CSV는 record 단위 streaming으로 처리해 memory와 error budget을 제한한다

### 시작 전 용어집

#### 1. large CSV

- **뜻:** 전체 파일을 list로 만든 뒤 검증하면 file 크기만큼 memory를 사용한다.
- **왜 중요한가:** Record iterator를 순차 처리하면 bounded memory로 import할 수 있다.
- **예시:** 전체 파일을 list로 만든 뒤 검증하면 file 크기만큼 …

#### 2. record

- **뜻:** 하지만 중간 record에서 실패했을 때 이미 앞선 row를 DB에 commit했다면 partial import가 된다.
- **왜 중요한가:** Batch transaction, staging table, validate-then-publish 같은 전략을 선택한다.
- **예시:** 하지만 중간 record에서 실패했을 때 이미 앞선 row를 …

#### 3. stream

- **뜻:** Streaming은 memory뿐 아니라 failure accumulation도 제한해야 한다.
- **왜 중요한가:** Error를 모두 수집한다면 최대 error count를 두어 malicious file이 수백만 error object를 만들지 않게 한다.
- **예시:** Streaming은 memory뿐 아니라 failure accumulation도 제한해야 한다.

---

## CHAPTER 08 · CSV contract는 parsing dialect와 domain schema를 독립적으로 고정한다

### 시작 전 용어집

#### 1. CSV

- **뜻:** 안정적인 CSV interface는 encoding/newline/dialect, header policy, field type, missing/extra column, row error policy, formula-safe export를 명시한다.
- **왜 중요한가:** 테스트에는 quoted comma, embedded newline, duplicate header, leading zero ID, empty field, 매우 큰 file, spreadsheet formula-like text를 포함한다.
- **예시:** 안정적인 CSV interface는 encoding/newline/dialect, header policy, field type, …

#### 2. CSV contract

- **뜻:** `split(',')` 기반 happy path test로는 CSV boundary를 검증할 수 없다.
- **왜 중요한가:** 이 PART의 핵심은 **CSV를 쉼표 문자열이 아니라 stateful record format으로 다루고, parser가 만든 text record 위에 별도의 domain schema와 export security 정책을 적용하는 것**이다.
- **예시:** `split(',')` 기반 happy path test로는 CSV boundary를 검증할 …

---

## 실전 학습 루프 · CSV schema boundary

### 1. 쉬운 예

CSV는 표처럼 보여도 field type, header 의미, quoting, newline, encoding, null 표현을 자동으로 정의하지 않는다. parser 성공 뒤에 header schema와 column validation, row-level domain 검증이 필요하다.

### 2. 한 줄 해석

CSV parsing과 schema validation은 분리한다. 행을 읽었다는 사실은 그 행이 업무 데이터로 유효하다는 뜻이 아니다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
import csv, io

text = 'id,amount\nA1,1200\n'
for row in csv.DictReader(io.StringIO(text)):
    amount = int(row['amount'])
    print(row['id'], amount)
```

### 4. 수정 실습

1. amount가 빈 문자열·음수·너무 큰 값인 row를 각각 처리한다.
2. 예상하지 못한 extra column을 허용할지 오류로 볼지 schema 정책을 정한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

`csv.DictReader`가 dict를 만들면 데이터 타입까지 검증된 것일까?

### 6. 정답과 오답 설명

**정답:** 아니다. 기본적으로 문자열 field를 구조화했을 뿐이며 타입·범위·cross-field 규칙은 별도 검증해야 한다.

**자주 나오는 오답:** parser success를 domain validity로 착각하는 것이 대표적인 오답이다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · CSV schema boundary

### 증상에서 시작한다

파일은 열리지만 특정 행에서 column이 밀리거나 숫자 변환이 깨지고, 일부 row만 조용히 잘못 들어간다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

raw row, parsed field count, header mapping, line number, encoding/newline, schema version과 validation error를 함께 남긴다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

quoted comma·embedded newline·빈 field·extra column·잘못된 숫자를 한 파일에 넣어 parser와 schema error를 분리한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

문법적으로 읽힌 row와 업무적으로 유효한 row가 분리되고, 오류 위치와 원인이 사용자에게 수정 가능한 형태로 반환돼야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.
## 판단 규칙 · CSV를 받을 때 최소 검증 순서

CSV ingest는 raw bytes → encoding decode → CSV syntax parse → header/schema match → field type conversion → row invariant → cross-row/domain rule 순서로 나누면 오류 위치가 선명해진다.

예를 들어 amount=-1은 CSV parser 오류가 아니다. parser는 문자열 -1을 정상적으로 읽었고, 금액이 음수라는 것은 domain validation 실패다. 반대로 따옴표가 닫히지 않은 행은 schema 단계에 도달하기 전에 syntax parsing에서 실패한다. 이 둘을 같은 CSV 오류로 합치지 않는다.

## 개념 연결 · CSV는 표가 아니라 직렬화된 record stream이다

스프레드시트에서 열어 보인다고 해서 CSV 의미가 명확한 것은 아니다. quoting, delimiter, newline, encoding, header 정책이 parser 계약을 만들고, 그 뒤의 타입·범위·필수값 규칙이 schema 계약을 만든다.

그래서 오류 보고도 “3번째 줄 실패”에서 끝내지 말고 **원본 line/record 번호, column 이름, raw value, 기대 타입, 실패 규칙**을 함께 남겨야 사용자가 데이터를 고칠 수 있다.
