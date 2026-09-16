# TRACK 08 · 데이터베이스

이번 TRACK은 `DB`, `PRIMARY KEY`, `FOREIGN KEY`, `JOIN`, `transaction`, `ACID`를 처음 듣는 사람을 기준으로 시작한다.

처음 질문은 이것뿐이다.

> 프로그램을 껐다 켜도 데이터가 남아 있어야 한다면 어디에 저장할까?

그리고 다음 질문으로 조금씩 확장한다.

```text
사용자가 100만 명이면?
두 사람이 동시에 수정하면?
중복되면 안 되는 값이 들어오면?
서로 관계있는 여러 데이터를 어떻게 연결하면?
검색이 너무 느리면?
중간에 실패했을 때 절반만 저장되면?
```

이 문제들을 하나씩 해결하다 보면 table, key, index, 무결성, transaction, ACID가 자연스럽게 나온다.

---

## BLOCK 01 · 왜 파일만으로는 부족하고 데이터베이스가 필요한가

### LESSON 01 · data·file·database·DBMS·relational DB·table·row·column·schema·type을 메모 앱에서 시작한다

#### 1. 프로그램이 다루는 사실과 값을 data라고 부른다

메모 앱이라면:

```text
메모 제목
메모 내용
작성 시간
완료 여부
```

쇼핑몰이라면:

```text
사용자 이름
상품 가격
주문 수량
배송 주소
결제 상태
```

이처럼 프로그램이 저장하고 처리하는 정보를 **data(데이터)**라고 부른다.

#### 2. 실행 중 변수에만 저장하면 프로그램 종료 후 사라질 수 있다

```python
name = "민수"
```

프로그램이 실행되는 동안에는 name을 사용할 수 있다.

하지만 process가 끝나면 RAM에 있던 실행 상태는 사라진다.

다음 실행에서도 값을 남기려면 영구 저장이 필요하다.

#### 3. 가장 단순한 방법은 file에 저장하는 것이다

```text
memo.txt
users.json
orders.csv
```

파일도 훌륭한 저장 방식이다.

작은 프로그램에서는 파일만으로 충분할 수 있다.

예를 들어 개인용 설정:

```json
{
  "theme": "dark",
  "fontSize": 18
}
```

정도는 JSON 파일로 저장할 수 있다.

#### 4. 데이터가 커지고 여러 사람이 동시에 사용하면 직접 해결할 일이 늘어난다

사용자 100만 명을 하나의 JSON 파일에 저장한다고 하자.

질문이 생긴다.

```text
사용자 735,421번만 빠르게 찾으려면?
두 server가 같은 파일을 동시에 수정하면?
중간에 process가 죽으면 파일 절반만 쓰이지 않을까?
email 중복을 어떻게 막지?
주문과 사용자를 어떻게 연결하지?
백업과 복구는?
```

이 문제들을 application 코드에서 모두 직접 구현하는 것은 어렵다.

#### 5. database는 데이터를 조직해서 저장·검색·수정·관리하는 시스템이다

**database(데이터베이스)**는 데이터를 일정한 구조와 규칙으로 저장하고 검색·수정·삭제할 수 있게 관리하는 시스템이다.

한 줄로:

> database = 많은 데이터를 규칙 있게 저장하고 안전하고 효율적으로 다루기 위한 시스템

#### 6. DBMS는 database를 실제로 관리하는 software다

대표:

```text
PostgreSQL
MySQL
SQLite
SQL Server
Oracle Database
```

이런 software를 **DBMS(Database Management System)**라고 부른다.

일상에서는 DB와 DBMS를 섞어 말하기도 하지만 정확히는 database의 data와 그것을 관리하는 software를 구분할 수 있다.

#### 7. SQLite와 server형 DB를 구분해 본다

SQLite는 별도 database server process 없이 application이 library를 통해 하나의 file 기반 database를 사용할 수 있다.

PostgreSQL/MySQL은 일반적으로 별도의 server process가 실행되고 client가 network/local connection으로 접속한다.

```text
SQLite
app ↔ DB file

PostgreSQL
app ↔ DB server process ↔ data files
```

어느 것이 무조건 더 좋은 것이 아니라 앱 규모와 동시성, 운영 요구에 따라 고른다.

#### 8. relational database는 table과 관계를 중심으로 데이터를 표현한다

사용자 table:

```text
id | name | age
1  | 민수 | 20
2  | 지수 | 25
```

주문 table:

```text
id  | user_id | amount
100 | 1       | 30000
101 | 2       | 15000
```

`user_id`를 통해 주문이 어떤 사용자와 관계있는지 표현한다.

이런 **관계형 데이터베이스(relational database)**를 이번 TRACK의 중심으로 배운다.

#### 9. table은 같은 종류의 record를 모은 구조다

```text
users
orders
products
```

같은 종류의 데이터를 각각 table로 나눌 수 있다.

`users`에는 사용자, `orders`에는 주문을 넣는다.

#### 10. row는 한 개의 record다

```text
1 | 민수 | 20
```

이 한 줄이 사용자 한 명의 **row(행)**다.

다른 말로 record라고 부르기도 한다.

#### 11. column은 record의 속성 종류다

```text
id
name
age
```

각 세로 항목이 **column(열)**이다.

모든 row는 같은 column 구조를 따른다.

#### 12. Excel 표와 비슷해 보여도 DB table은 더 강한 규칙을 가진다

관계형 DB에는:

```text
column type
PRIMARY KEY
UNIQUE
NOT NULL
CHECK
FOREIGN KEY
transaction
index
```

같은 규칙과 기능이 있다.

이것이 단순 spreadsheet와 다른 중요한 점이다.

#### 13. schema는 데이터 구조의 설계도다

```text
users
  id: integer
  name: text
  age: integer
```

어떤 table과 column이 있고 각 type/constraint가 무엇인지 정한 구조를 **schema**라고 부른다.

```text
schema = 데이터가 어떤 모양이어야 하는지 정한 설계
row = 그 설계에 실제로 들어간 값
```

#### 14. type은 column에 들어갈 값 종류를 제한한다

대표적인 개념:

```text
INTEGER
TEXT
BOOLEAN
DATE
TIMESTAMP
NUMERIC/DECIMAL
```

DBMS마다 정확한 type 이름과 동작은 다르다.

`age`에 아무 binary file이나 넣지 않도록 기본 종류를 정한다.

#### 15. type만으로 business rule 전체를 지킬 수는 없다

```text
age INTEGER
```

이라고 했다고:

```text
age = -999999
```

를 자동으로 막는 것은 아니다.

정수라는 **형식**은 맞지만 사람 나이라는 **의미**는 틀렸다.

이 차이가 뒤의 CHECK constraint와 데이터 무결성으로 이어진다.

#### 16. table 설계를 메모 앱에 적용한다

```text
memos
- id
- title
- body
- created_at
- completed
```

row 하나:

```text
1 | 약 사기 | 퇴근 후 약국 방문 | 2026-09-16 18:00 | false
```

이제 파일 한 덩어리보다 `어떤 data가 어떤 column에 들어가는지` 구조가 명확하다.

#### 17. 책을 덮고 확인한다

1. variable에 저장한 값과 영구 storage의 차이를 설명하라.
2. 작은 JSON file만으로 충분한 경우와 DB가 필요한 경우를 각각 말하라.
3. database와 DBMS를 구분하라.
4. table/row/column을 users 예로 설명하라.
5. schema와 실제 row data는 무엇이 다른가?
6. INTEGER type만으로 age=-100을 막을 수 없는 이유는 무엇인가?

---

## BLOCK 02 · SQL로 table을 만들고 데이터를 넣고 찾고 바꾼다

### LESSON 01 · CREATE·INSERT·SELECT·WHERE·UPDATE·DELETE·ORDER BY·LIMIT을 하나의 users table로 익힌다

#### 1. SQL은 관계형 DB에 명령을 전달하는 언어다

**SQL(Structured Query Language)**은 관계형 database에서 구조를 만들고 데이터를 조회·수정하는 데 사용하는 표준화된 언어다.

DBMS마다 세부 문법 차이가 있지만 핵심 형태는 비슷하다.

DB에 보내는 SQL 명령을 흔히 **query**라고 부른다.

#### 2. CREATE TABLE로 구조를 만든다

```sql
CREATE TABLE users (
  id INTEGER,
  name TEXT,
  age INTEGER
);
```

사람말:

```text
users라는 table을 만든다.
id는 integer
name은 text
age는 integer
```

아직 key/constraint는 뒤에서 추가한다.

#### 3. INSERT로 새 row를 넣는다

```sql
INSERT INTO users (id, name, age)
VALUES (1, '민수', 20);
```

column과 value를 대응한다.

```text
id   → 1
name → 민수
age  → 20
```

순서를 틀리면 잘못된 data가 들어가거나 type error가 날 수 있다.

#### 4. 여러 row를 넣는다

```sql
INSERT INTO users (id, name, age)
VALUES
  (2, '지수', 25),
  (3, '현우', 17),
  (4, '수진', 31);
```

현재:

```text
id | name | age
1  | 민수 | 20
2  | 지수 | 25
3  | 현우 | 17
4  | 수진 | 31
```

#### 5. SELECT로 데이터를 조회한다

```sql
SELECT id, name
FROM users;
```

결과:

```text
1 | 민수
2 | 지수
3 | 현우
4 | 수진
```

필요한 column만 선택했다.

#### 6. SELECT *는 편하지만 운영 코드에서 신중하게 사용한다

```sql
SELECT * FROM users;
```

모든 column을 가져온다.

작은 실습에서는 편하다.

하지만 실제 service에서는:

```text
필요 없는 큰 column까지 전송
민감한 column 노출 위험
schema 변경 영향
network 비용
```

이 생길 수 있다.

필요한 column을 명시하는 습관이 좋다.

#### 7. WHERE로 필요한 row만 찾는다

```sql
SELECT id, name
FROM users
WHERE age >= 20;
```

20세 이상만 조회한다.

조건을 두 개 연결한다.

```sql
WHERE age >= 20 AND name <> '수진'
```

#### 8. 문자열 값을 SQL에 직접 이어 붙이면 injection 위험이 있다

나쁜 예:

```javascript
const sql = "SELECT * FROM users WHERE name = '" + userInput + "'";
```

사용자가 SQL 문법을 포함한 값을 넣을 수 있다.

실제 application에서는 parameterized query/prepared statement를 사용한다.

예시 형태:

```sql
SELECT id, name
FROM users
WHERE name = $1;
```

값은 별도 parameter로 전달한다.

SQL injection은 보안 TRACK에서 더 깊게 다룬다.

#### 9. UPDATE로 기존 row를 수정한다

```sql
UPDATE users
SET age = 21
WHERE id = 1;
```

1번 사용자의 age만 21로 바꾼다.

#### 10. WHERE 없는 UPDATE는 모든 row를 바꿀 수 있다

```sql
UPDATE users
SET age = 21;
```

모든 사용자의 age가 21이 될 수 있다.

운영 DB 변경 전에:

```text
같은 WHERE로 SELECT 먼저 확인
transaction 고려
backup/rollback 방법 확인
```

하는 습관이 중요하다.

#### 11. DELETE로 row를 삭제한다

```sql
DELETE FROM users
WHERE id = 3;
```

3번 사용자 삭제.

하지만:

```sql
DELETE FROM users;
```

는 모든 row를 삭제할 수 있다.

변경 query는 특히 조심한다.

#### 12. ORDER BY로 정렬한다

```sql
SELECT id, name, age
FROM users
ORDER BY age DESC;
```

나이가 큰 순서.

```text
DESC = 내림차순
ASC  = 오름차순
```

#### 13. 여러 정렬 기준을 둘 수 있다

```sql
ORDER BY age DESC, id ASC;
```

나이가 같으면 id가 작은 순으로 정렬한다.

정렬 기준이 없으면 DB가 row를 항상 같은 순서로 반환한다고 가정하면 안 된다.

pagination에서는 특히 안정적인 ORDER BY가 중요하다.

#### 14. LIMIT으로 결과 수를 제한한다

```sql
SELECT id, name
FROM users
ORDER BY id
LIMIT 10;
```

최대 10개만 가져온다.

대량 데이터를 한꺼번에 application memory로 읽는 것을 줄일 수 있다.

#### 15. OFFSET pagination을 만든다

```sql
SELECT id, name
FROM users
ORDER BY id
LIMIT 50 OFFSET 100;
```

100개를 건너뛰고 다음 50개.

하지만 OFFSET이 수백만으로 커지면 앞 row를 건너뛰는 비용이 커질 수 있다.

나중에 index/cursor pagination과 연결된다.

#### 16. CRUD라는 말을 실제 SQL과 연결한다

```text
Create → INSERT
Read   → SELECT
Update → UPDATE
Delete → DELETE
```

이 네 작업을 묶어 **CRUD**라고 부른다.

모든 app이 단순 CRUD만 있는 것은 아니지만 data 기능의 기본 vocabulary다.

#### 17. 책을 덮고 확인한다

1. SQL과 query를 자기 말로 설명하라.
2. CREATE/INSERT/SELECT/UPDATE/DELETE의 역할을 각각 말하라.
3. UPDATE/DELETE에서 WHERE가 중요한 이유는 무엇인가?
4. 사용자 입력을 SQL 문자열에 직접 합치면 왜 위험한가?
5. ORDER BY 없이 결과 순서를 당연하다고 생각하면 왜 안 되는가?
6. LIMIT/OFFSET 방식이 아주 뒤 page에서 느릴 수 있는 이유는 무엇인가?

---

## BLOCK 03 · 여러 row를 묶어서 숫자를 계산한다

### LESSON 01 · aggregate function·GROUP BY·HAVING을 매출 통계로 단계별로 배운다

#### 1. 주문 table에서 "몇 건인가"를 알고 싶다

```text
orders
id | category | amount
1  | 약       | 10000
2  | 식품     | 20000
3  | 약       | 15000
4  | 식품     | 5000
```

row 전체를 application으로 가져와 직접 세지 않고 DB에서 계산할 수 있다.

#### 2. COUNT는 row 개수를 센다

```sql
SELECT COUNT(*)
FROM orders;
```

결과:

```text
4
```

#### 3. SUM은 값을 더한다

```sql
SELECT SUM(amount)
FROM orders;
```

결과:

```text
50000
```

#### 4. AVG/MIN/MAX도 자주 사용한다

```sql
SELECT
  AVG(amount),
  MIN(amount),
  MAX(amount)
FROM orders;
```

```text
AVG = 평균
MIN = 최솟값
MAX = 최댓값
```

이런 함수를 **aggregate function(집계 함수)**라고 부른다.

#### 5. category별 합계를 알고 싶다

원하는 결과:

```text
약   | 25000
식품 | 25000
```

같은 category끼리 묶고 각 그룹 안에서 SUM을 계산해야 한다.

```sql
SELECT category, SUM(amount)
FROM orders
GROUP BY category;
```

#### 6. GROUP BY를 손으로 수행해 본다

원본:

```text
약   10000
식품 20000
약   15000
식품  5000
```

그룹:

```text
약 그룹
10000, 15000
→ SUM 25000

식품 그룹
20000, 5000
→ SUM 25000
```

이 과정을 이해하면 SQL 문법이 덜 추상적이다.

#### 7. WHERE는 grouping 전에 row를 거른다

20세 이상 사용자 주문만 집계한다고 하자.

```sql
SELECT category, SUM(amount)
FROM orders
WHERE amount > 0
GROUP BY category;
```

WHERE 조건을 통과한 row들만 group된다.

#### 8. HAVING은 group 결과에 조건을 건다

총매출이 20000 이상인 category만:

```sql
SELECT category, SUM(amount) AS total_amount
FROM orders
GROUP BY category
HAVING SUM(amount) >= 20000;
```

구분:

```text
WHERE
→ 개별 row를 group 전 필터

HAVING
→ group 집계 결과를 필터
```

#### 9. alias로 계산 결과 이름을 붙인다

```sql
SELECT
  category,
  SUM(amount) AS total_amount
FROM orders
GROUP BY category;
```

`total_amount`라는 읽기 좋은 이름으로 결과 column을 표시한다.

#### 10. COUNT(column)과 COUNT(*)는 NULL에서 차이가 날 수 있다

```sql
COUNT(*)
```

은 row 수를 센다.

```sql
COUNT(nickname)
```

은 nickname이 NULL인 row를 세지 않는다.

따라서 통계 query에서 어떤 count를 원하는지 확인한다.

#### 11. 평균의 평균은 단순 평균하면 틀릴 수 있다

병원 A 환자 100명 평균 80, 병원 B 환자 10명 평균 100이라고 하자.

```text
(80 + 100) / 2 = 90
```

라고 하면 환자 수 차이를 무시한다.

원래 row 또는 count/total을 이용해 가중 평균을 계산해야 한다.

DB 집계도 숫자 뜻을 이해해야 한다.

#### 12. grouping이 많으면 index와 query plan이 중요해질 수 있다

수억 row를 grouping하면 DB가 정렬하거나 hash aggregate를 수행하고 memory/disk를 사용할 수 있다.

SQL이 짧다고 실행 비용이 작은 것은 아니다.

뒤의 EXPLAIN에서 실제 query plan을 확인한다.

#### 13. application에서 계산할지 DB에서 계산할지 생각한다

DB에서 aggregate하면 network로 불필요한 row 수백만 개를 가져오지 않아도 된다.

하지만 복잡한 business logic은 application에서 처리하는 편이 나을 수 있다.

데이터 크기, DB 부하, 재사용, 정확성 요구를 보고 결정한다.

#### 14. 책을 덮고 확인한다

1. COUNT/SUM/AVG/MIN/MAX를 각각 말하라.
2. GROUP BY가 row를 어떻게 묶는지 손으로 설명하라.
3. WHERE와 HAVING은 어느 시점의 데이터를 거르는가?
4. COUNT(*)와 COUNT(column)이 NULL에서 왜 다를 수 있는가?
5. 그룹 평균의 평균을 단순 계산하면 왜 틀릴 수 있는가?

---

## BLOCK 04 · 데이터가 말이 안 되는 상태가 되지 않도록 DB 자체에 규칙을 둔다

### LESSON 01 · ID·PRIMARY KEY·UNIQUE·NULL·NOT NULL·CHECK·무결성·FOREIGN KEY를 "깨진 주문 데이터"에서 시작한다

#### 1. 데이터 타입이 맞아도 데이터는 틀릴 수 있다

다음 user row를 보자.

```text
id = 1
name = 민수
age = -928374928
```

age는 integer다.

type 규칙은 지켰다.

하지만 사람 나이로는 말이 되지 않는다.

또:

```text
user id = 10은 존재하지 않음
order.user_id = 10
```

이라면 주문이 존재하지 않는 사용자에게 연결됐다.

이것도 각 column type은 맞을 수 있지만 data 관계가 틀렸다.

#### 2. 데이터 무결성은 정해진 규칙과 관계가 깨지지 않은 올바른 상태다

**data integrity(데이터 무결성)**은 데이터가 시스템이 정한 규칙을 지키고 서로 모순되지 않는 상태를 유지하는 성질이다.

예:

```text
사용자 id 중복 없음
필수 name은 NULL 아님
age는 0 이상
email 중복 없음
모든 order.user_id는 실제 user를 가리킴
주문 금액은 음수가 아님
```

이 규칙이 계속 참이어야 한다.

#### 3. 무결성은 "파일이 물리적으로 안 깨짐"만 뜻하지 않는다

DB 파일을 완벽히 읽을 수 있어도:

```text
잔액 = -999999999999
```

처럼 business rule상 불가능한 값이 들어 있을 수 있다.

물리 파일은 읽히지만 논리 data 상태는 잘못됐다.

무결성은 이런 **논리적 정확성**을 포함한다.

#### 4. id가 필요한 이유 — 이름은 중복된다

```text
민수, 20세
민수, 35세
```

이름만으로 어느 민수인지 정확히 구분할 수 없다.

고유 식별값을 둔다.

```text
id 101 → 민수 20세
id 102 → 민수 35세
```

#### 5. PRIMARY KEY는 row의 대표 고유 식별자다

```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT
);
```

`PRIMARY KEY`는 각 row를 고유하게 식별한다.

중복될 수 없고 NULL일 수 없다.

#### 6. 왜 primary key 중복이 위험할까

```text
id 1 → 민수
id 1 → 지수
```

라면:

```sql
UPDATE users
SET name = '현우'
WHERE id = 1;
```

이 어느 사용자를 뜻하는지 모호해진다.

row를 안정적으로 참조하기 위해 고유 key가 필요하다.

#### 7. 자연 key와 surrogate key를 구분한다

email 자체를 primary key로 사용할 수도 있다.

하지만 email은 변경될 수 있고 길며 다른 table의 foreign key에도 반복된다.

그래서 별도의 숫자/UUID 같은 **surrogate key**를 id로 두고 email에는 UNIQUE를 거는 설계를 많이 사용한다.

모든 상황에서 surrogate key가 무조건 정답인 것은 아니다. domain의 실제 고유 식별자를 key로 쓰는 **natural key**가 적합한 경우도 있다.

#### 8. UNIQUE는 primary key가 아닌 값의 중복도 막는다

```sql
email TEXT UNIQUE
```

같은 email이 두 user에게 들어오는 것을 막을 수 있다.

한 table에는 primary key가 하나지만 UNIQUE constraint는 여러 column/조합에 둘 수 있다.

#### 9. 여러 column 조합의 UNIQUE가 필요할 수 있다

같은 사용자가 같은 event에 한 번만 신청 가능:

```sql
UNIQUE (user_id, event_id)
```

user_id 자체는 여러 번 나올 수 있고 event_id도 여러 번 나올 수 있다.

하지만 두 값의 **조합**은 한 번만 허용한다.

#### 10. NULL은 "값 없음/알 수 없음"을 나타내는 특별한 상태다

```text
nickname = NULL
```

빈 문자열과 구분한다.

```text
NULL
→ 값이 없거나 알 수 없음

''
→ 길이가 0인 문자열이라는 실제 값
```

NULL 비교는 일반 값과 다르게 다뤄진다.

```sql
WHERE nickname IS NULL
```

처럼 확인한다.

```sql
nickname = NULL
```

로 비교하면 기대와 다를 수 있다.

#### 11. NOT NULL은 반드시 값이 있어야 한다는 규칙이다

```sql
name TEXT NOT NULL
```

name에 NULL을 허용하지 않는다.

frontend form에서 `required`를 넣었다고 DB 제약이 필요 없다는 뜻은 아니다.

API를 직접 호출하거나 batch/import가 DB에 쓸 수 있기 때문이다.

DB constraint는 마지막 방어선이 된다.

#### 12. CHECK는 값의 범위를 DB에서도 검사한다

```sql
age INTEGER CHECK (age >= 0 AND age <= 150)
```

```sql
amount INTEGER CHECK (amount >= 0)
```

같이 row 값이 조건을 만족해야 저장되게 할 수 있다.

#### 13. constraint를 하나의 table에 연결한다

```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  age INTEGER CHECK (age >= 0 AND age <= 150)
);
```

읽는다.

```text
id = 고유 식별
email = 반드시 있고 중복 불가
name = 반드시 있음
age = 정수이고 허용 범위
```

#### 14. 이제 orders table과 관계를 만든다

```sql
CREATE TABLE orders (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  amount INTEGER CHECK (amount >= 0),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

`user_id`가 users.id를 참조한다.

#### 15. FOREIGN KEY는 관계가 실제 row를 가리키도록 돕는다

user 999가 없는데:

```sql
INSERT INTO orders (id, user_id, amount)
VALUES (1, 999, 30000);
```

를 넣으면 FK constraint가 거부할 수 있다.

이런 관계의 올바름을 **referential integrity(참조 무결성)**라고 부른다.

#### 16. 사용자를 삭제하면 그 사용자의 주문은 어떻게 할까

FK가 있으면 delete 정책을 정해야 한다.

```text
RESTRICT/NO ACTION
→ 참조하는 order가 있으면 user 삭제 막기

CASCADE
→ user 삭제 시 관련 order도 삭제

SET NULL
→ order.user_id를 NULL로 변경 가능할 때
```

어떤 정책이 맞는지는 domain 의미에 따라 다르다.

쇼핑몰 주문 기록을 user 탈퇴와 함께 모두 물리 삭제하면 회계/법적 보존 요구와 충돌할 수 있다.

#### 17. app validation과 DB constraint는 경쟁 관계가 아니다

app:

```text
사용자에게 빠르고 친절한 오류 메시지
```

server:

```text
business rule 검증
```

DB:

```text
마지막 불변조건 방어
```

처럼 여러 층이 협력한다.

#### 18. constraint로 모든 business rule을 표현할 수는 없다

```text
VIP는 하루 1억원 결제 가능
일반 사용자는 하루 100만원
```

처럼 시간, 다른 table, 사용자 등급이 얽힌 규칙은 단순 CHECK 하나로 표현하기 어려울 수 있다.

DB constraint에 넣을 불변조건과 application business logic을 나눈다.

#### 19. 책을 덮고 확인한다

1. data integrity를 age=-999와 없는 user의 order 예로 설명하라.
2. PRIMARY KEY가 필요한 이유는 무엇인가?
3. UNIQUE와 PRIMARY KEY는 어떤 역할 차이가 있는가?
4. NULL과 빈 문자열을 구분하라.
5. CHECK constraint의 실제 예를 하나 만들어라.
6. FOREIGN KEY가 referential integrity를 어떻게 돕는가?
7. ON DELETE CASCADE를 무조건 쓰면 안 되는 이유는 무엇인가?
8. frontend validation이 있어도 DB constraint가 필요한 이유는 무엇인가?

---

## BLOCK 05 · 현실의 관계를 table로 설계한다

### LESSON 01 · 1:1·1:N·N:M·junction table·normalization을 사용자/주문/상품으로 배운다

#### 1. 현실의 객체들은 서로 관계가 있다

```text
사용자 한 명은 주문 여러 개를 할 수 있다.
주문 하나는 사용자 한 명에게 속한다.
주문 하나에는 상품 여러 개가 들어갈 수 있다.
상품 하나는 여러 주문에 들어갈 수 있다.
```

이 관계를 table 구조로 표현해야 한다.

#### 2. one-to-many: 사용자 1명 → 주문 여러 개

users:

```text
id | name
1  | 민수
```

orders:

```text
id  | user_id | amount
100 | 1       | 30000
101 | 1       | 15000
102 | 1       | 5000
```

user 1이 여러 order row에 참조된다.

이것이 **1:N(one-to-many)** 관계다.

#### 3. one-to-one은 한 row가 상대 table의 최대 한 row와 연결된다

사용자와 1개의 추가 profile row:

```text
users
id | email
1  | a@example.com

profiles
user_id | bio
1       | 안녕하세요
```

profiles.user_id에 UNIQUE/FK를 두어 user당 profile 하나만 허용할 수 있다.

하지만 `무조건 1:1이면 table을 나눈다`는 뜻은 아니다. 보안/옵션/크기/수명주기 등 분리 이유가 있어야 한다.

#### 4. many-to-many: 주문과 상품

주문 100에는 상품 A,B가 있다.

주문 101에도 상품 A가 있다.

```text
order 100 → product A, B
order 101 → product A
```

한 order에 여러 product, 한 product가 여러 order에 등장한다.

이것이 **N:M(many-to-many)** 관계다.

#### 5. 관계형 DB에서는 junction table로 N:M을 푼다

orders:

```text
id
100
101
```

products:

```text
id | name
10 | 약
20 | 물
```

order_items:

```text
order_id | product_id | quantity | unit_price
100      | 10         | 2        | 5000
100      | 20         | 1        | 1000
101      | 10         | 1        | 5000
```

`order_items`가 두 table 사이 관계를 나타내는 **junction/association table** 역할을 한다.

#### 6. 관계 row 자체에도 data가 있을 수 있다

order와 product의 관계에는:

```text
quantity
unit_price_at_purchase
discount
```

같은 data가 있다.

따라서 junction table은 단순히 두 id만 연결하는 것보다 실제 domain entity에 가까워질 수 있다.

#### 7. 주문 가격에는 현재 product.price만 참조하면 안 될 수 있다

상품 현재 가격이:

```text
10,000 → 12,000 변경
```

됐다고 과거 주문 금액까지 12,000으로 바뀌면 안 된다.

그래서 주문 시점의 unit_price를 order_items에 snapshot으로 저장할 수 있다.

`중복을 무조건 없애야 한다`는 단순 규칙보다 domain history가 중요하다.

#### 8. normalization은 data 중복과 갱신 이상을 줄이기 위한 설계 원칙이다

나쁜 table:

```text
order_id | user_name | user_email | product1 | product2 | product3
```

사용자 email이 바뀌면 과거 여러 order row를 모두 수정해야 할 수 있다.

상품이 4개면 product4 column을 새로 만들어야 한다.

구조가 확장에 약하다.

#### 9. 1NF를 아주 쉽게 이해한다

한 cell에 반복 list를 넣지 않고 원자적인 값 구조를 만들고 반복 group을 row로 나누는 방향이다.

나쁜 예:

```text
order 100 | products = "10,20,30"
```

문자열을 매번 split해야 하고 FK/index를 적용하기 어렵다.

order_items row로 분리한다.

#### 10. 2NF/3NF를 공식보다 문제로 이해한다

예를 들어 order_items에:

```text
order_id
product_id
product_name
product_category_name
```

를 매번 복사한다고 하자.

상품 이름이 바뀔 때 수천 order item을 수정해야 한다.

상품 자체 속성은 products table에 두고 relation은 product_id로 참조하는 방식이 중복과 update anomaly를 줄인다.

#### 11. normalization을 끝까지 하면 언제나 최고인가

조회마다 JOIN 20개가 필요한 지나치게 분리된 구조는 읽기와 성능이 어려워질 수 있다.

분석 시스템에서는 일부 **denormalization**으로 중복을 허용해 읽기 성능을 높이는 경우도 있다.

중요한 것은:

```text
왜 중복을 허용하는가?
어떤 data가 source of truth인가?
어떻게 동기화할 것인가?
```

를 명확히 하는 것이다.

#### 12. 관계를 먼저 문장으로 적고 schema로 옮긴다

```text
User 1명은 Order 여러 개를 가진다.
Order 하나는 User 한 명에 속한다.
Order는 Product 여러 개를 가진다.
Product는 여러 Order에 들어갈 수 있다.
```

이 문장을:

```text
users
orders(user_id FK)
products
order_items(order_id FK, product_id FK)
```

로 변환한다.

#### 13. ER diagram으로 관계를 시각화할 수 있다

```text
users 1 ─── N orders
orders 1 ─── N order_items N ─── 1 products
```

table이 많아지면 그림으로 관계를 보는 것이 이해에 도움이 된다.

#### 14. 책을 덮고 확인한다

1. 1:N 관계를 user/order로 설명하라.
2. N:M 관계는 왜 junction table이 필요한가?
3. order_items에 quantity가 있는 이유는 무엇인가?
4. 과거 주문에 현재 product.price를 그대로 쓰면 어떤 문제가 생길 수 있는가?
5. normalization이 줄이려는 문제는 무엇인가?
6. denormalization을 일부 사용하는 이유는 무엇인가?

---

## BLOCK 06 · 여러 table의 관계를 실제 query 결과로 합친다

### LESSON 01 · JOIN·INNER JOIN·LEFT JOIN·subquery를 손으로 결과표를 그리며 배운다

#### 1. users와 orders를 따로 저장했는데 화면에는 함께 보여 줘야 한다

users:

```text
id | name
1  | 민수
2  | 지수
3  | 현우
```

orders:

```text
id  | user_id | amount
100 | 1       | 30000
101 | 1       | 10000
102 | 2       | 5000
```

원하는 결과:

```text
order_id | user_name | amount
100      | 민수      | 30000
101      | 민수      | 10000
102      | 지수      | 5000
```

두 table을 관계 key로 연결해 읽어야 한다.

이때 **JOIN**을 사용한다.

#### 2. INNER JOIN은 양쪽에 매칭되는 row를 합친다

```sql
SELECT
  orders.id AS order_id,
  users.name AS user_name,
  orders.amount
FROM orders
INNER JOIN users
  ON orders.user_id = users.id;
```

`ON` 조건:

```text
orders.user_id와 users.id가 같은 row를 연결
```

#### 3. 손으로 join한다

order 100:

```text
user_id=1
→ users id=1 찾음
→ 민수
```

order 101:

```text
user_id=1
→ 민수
```

order 102:

```text
user_id=2
→ 지수
```

결과 row를 하나씩 만들면 JOIN이 덜 어렵다.

#### 4. users에서 주문이 없는 현우는 INNER JOIN 결과에 나오지 않는다

현우 id=3을 참조하는 order가 없다.

`orders INNER JOIN users`에서 matching order가 없으므로 결과가 없다.

그런데 요구사항이:

> 주문이 없는 사용자도 모두 보여 줘.

라면 다른 JOIN이 필요하다.

#### 5. LEFT JOIN은 왼쪽 table row를 모두 유지한다

```sql
SELECT
  users.name,
  orders.id,
  orders.amount
FROM users
LEFT JOIN orders
  ON orders.user_id = users.id;
```

결과:

```text
민수 | 100 | 30000
민수 | 101 | 10000
지수 | 102 | 5000
현우 | NULL| NULL
```

현우도 왼쪽 users row이므로 남고, matching order가 없어 오른쪽 column이 NULL이 된다.

#### 6. INNER와 LEFT를 질문으로 구분한다

```text
양쪽에 실제 관계가 있는 것만 보고 싶다
→ INNER JOIN

왼쪽 대상은 모두 보고, 관계가 없으면 NULL로라도 남기고 싶다
→ LEFT JOIN
```

#### 7. JOIN 조건을 빼먹으면 row 수가 폭증할 수 있다

users 1,000개 × orders 100,000개를 모든 조합으로 만들면 1억 row 후보가 된다.

잘못된 join/cartesian product는 성능과 결과를 망가뜨린다.

반드시:

```text
어떤 key와 어떤 key를 연결하는가?
```

를 확인한다.

#### 8. 같은 이름 column은 table 이름/alias로 구분한다

users에도 id, orders에도 id가 있다.

```sql
SELECT u.id, o.id
FROM users AS u
JOIN orders AS o
  ON o.user_id = u.id;
```

alias `u`, `o`로 간결하게 구분한다.

#### 9. 여러 table을 계속 join할 수 있다

주문 상품까지:

```sql
SELECT
  u.name,
  o.id AS order_id,
  p.name AS product_name,
  oi.quantity
FROM orders o
JOIN users u ON u.id = o.user_id
JOIN order_items oi ON oi.order_id = o.id
JOIN products p ON p.id = oi.product_id;
```

한 번에 외우지 않는다.

```text
orders → user 연결
orders → order_items 연결
order_items → product 연결
```

한 관계씩 추가한다.

#### 10. subquery는 query 안에 다른 query를 사용한다

평균 주문 금액보다 큰 주문:

```sql
SELECT id, amount
FROM orders
WHERE amount > (
  SELECT AVG(amount)
  FROM orders
);
```

안쪽 query가 평균을 계산하고, 바깥 query가 그 값을 조건에 사용한다.

#### 11. subquery와 JOIN 중 하나가 무조건 더 빠른 것은 아니다

현대 DB optimizer는 query를 여러 방식으로 바꿔 실행할 수 있다.

읽기 쉬운 SQL과 실제 query plan을 함께 본다.

`subquery는 항상 느리다`, `JOIN은 항상 빠르다` 같은 단순 규칙은 피한다.

#### 12. N+1 query 문제를 맛본다

application:

```text
사용자 100명 조회 → query 1번
각 사용자마다 order 조회 → query 100번
총 101번
```

이것이 **N+1 query problem**의 전형적인 모양이다.

JOIN, eager loading, batch query 등으로 줄일 수 있다.

#### 13. ORM을 사용해도 실제 SQL을 이해해야 한다

ORM은 object와 table을 편하게 연결해 주지만 잘못 사용하면 N+1이나 비효율적인 query를 만들 수 있다.

`코드가 짧다 = DB 작업도 적다`가 아니다.

개발 도구에서 실제 SQL과 실행 횟수를 확인한다.

#### 14. 책을 덮고 확인한다

1. JOIN이 필요한 이유를 users/orders 예로 설명하라.
2. INNER JOIN과 LEFT JOIN 결과를 손으로 그려라.
3. LEFT JOIN에서 관계없는 오른쪽 column은 어떤 값이 되는가?
4. JOIN 조건을 잘못 쓰면 왜 row가 폭증할 수 있는가?
5. subquery를 평균 주문 예로 설명하라.
6. N+1 query problem은 무엇인가?

---

## BLOCK 07 · 원하는 row를 빨리 찾도록 index와 query plan을 이해한다

### LESSON 01 · index·B-tree 감각·복합 index·EXPLAIN·N+1·read/write trade-off를 실제 느린 query로 배운다

#### 1. 사용자 10명에서는 모든 row를 읽어도 빠르다

```sql
SELECT id, name
FROM users
WHERE email = 'a@example.com';
```

users가 10 row면 하나씩 다 봐도 거의 즉시 끝난다.

1억 row면 이야기가 달라진다.

DB가 매번 처음부터 끝까지 전부 읽는 **full table scan**을 하면 비용이 크다.

#### 2. 책의 색인처럼 index를 생각한다

1000쪽 책에서 `transaction`이라는 단어를 찾는다.

매 페이지를 처음부터 읽기보다 뒤의 index에서:

```text
transaction → 820쪽
```

을 찾아 바로 이동한다.

DB의 **index**도 column 값을 찾기 쉬운 별도 자료구조로 유지해서 검색을 빠르게 할 수 있다.

#### 3. email index를 만든다

```sql
CREATE INDEX idx_users_email
ON users(email);
```

이제 DB optimizer가 email 검색에서 index를 사용할 수 있다.

`index를 만들었다 = 모든 query가 무조건 사용`은 아니다. optimizer가 비용을 계산한다.

#### 4. B-tree를 정확한 구현보다 정렬된 tree 감각으로 본다

많은 관계형 DB의 일반 index는 B-tree 계열을 사용한다.

값이 정렬된 tree 구조에 가까워:

```text
정확한 값 찾기
범위 찾기
정렬 지원
```

에 유리하다.

TRACK 03의 tree가 실제 DB 내부에 연결된다.

#### 5. index는 공짜가 아니다

users row를 INSERT하면 table에 data만 쓰는 것이 아니다.

해당 row의 index entry도 갱신해야 한다.

index가 20개라면 write마다 여러 index를 수정한다.

```text
장점
→ read query 빠르게

비용
→ storage 증가
→ INSERT/UPDATE/DELETE 비용 증가
→ maintenance 필요
```

#### 6. 모든 column에 index를 만들면 좋은가

아니다.

자주 검색하지 않는 column의 index는 write 비용만 늘릴 수 있다.

값 종류가 거의 없는 boolean column:

```text
active = true/false
```

만 단독 index했을 때도 selectivity가 낮아 optimizer가 사용하지 않을 수 있다.

실제 query pattern을 보고 만든다.

#### 7. 복합 index는 column 순서가 중요하다

```sql
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at);
```

이 index는:

```text
특정 user의 order를 created_at 범위로 찾기
```

에 유용할 수 있다.

하지만 `(user_id, created_at)`과 `(created_at, user_id)`는 같은 index가 아니다.

어떤 조건과 정렬을 주로 사용하는지 보고 순서를 정한다.

#### 8. leftmost prefix 감각을 배운다

B-tree 복합 index `(user_id, created_at)`에서는 앞쪽 `user_id`를 기준으로 정렬된 그룹 안에서 created_at이 정렬되는 형태로 생각할 수 있다.

따라서 created_at만 조건으로 검색할 때 이 index가 기대만큼 유용하지 않을 수 있다.

정확한 사용 여부는 DB optimizer와 EXPLAIN으로 확인한다.

#### 9. UNIQUE constraint도 내부적으로 index와 연결되는 경우가 많다

```sql
email TEXT UNIQUE
```

중복 여부를 빠르게 확인하기 위해 DB가 unique index를 만들 수 있다.

하지만 DBMS 구현 세부는 확인해야 한다.

#### 10. EXPLAIN은 DB가 query를 어떻게 실행하려는지 보여 준다

PostgreSQL 예:

```sql
EXPLAIN
SELECT id, name
FROM users
WHERE email = 'a@example.com';
```

결과에:

```text
Seq Scan
Index Scan
cost
rows
```

같은 정보가 보일 수 있다.

#### 11. EXPLAIN ANALYZE는 실제 실행까지 할 수 있어 주의한다

```sql
EXPLAIN ANALYZE ...
```

는 실제 query를 실행해 측정하는 기능이다.

SELECT에서는 유용하지만 UPDATE/DELETE와 함께 쓰면 실제 data 변경이 일어날 수 있다.

운영에서 무심코 실행하지 않는다.

#### 12. estimated rows가 틀리면 나쁜 plan을 선택할 수 있다

optimizer는 table 통계를 이용해 row 수와 비용을 추정한다.

통계가 오래되거나 data 분포가 매우 치우치면 예상과 실제가 크게 다를 수 있다.

```text
estimated rows = 10
actual rows = 1,000,000
```

이면 선택한 join/index plan이 좋지 않을 수 있다.

#### 13. 느린 query를 index 하나로 무조건 해결하지 않는다

원인 후보:

```text
필터 조건에 index 없음
잘못된 JOIN
N+1
필요 없는 SELECT *
너무 많은 row 반환
정렬/집계 비용
lock 대기
DB connection 대기
network
```

먼저 실제 plan과 timing을 본다.

#### 14. pagination에서도 index가 중요하다

```sql
SELECT id, created_at
FROM orders
WHERE user_id = 10
ORDER BY created_at DESC
LIMIT 50;
```

`(user_id, created_at DESC)`에 적합한 index가 있으면 필요한 최신 row를 빠르게 찾을 수 있다.

반대로 OFFSET 1,000,000을 쓰면 index가 있어도 많은 entry를 건너뛰어야 할 수 있다.

cursor/keyset pagination이 유리한 경우가 있다.

#### 15. N+1은 index보다 query 횟수 자체가 문제일 수 있다

사용자 1000명을 불러오고 각자 order query를 한 번씩 한다.

각 query가 1ms라도 network round trip과 parsing이 1000번 반복된다.

JOIN/batch query로 1~몇 번에 가져오는 것이 훨씬 빠를 수 있다.

#### 16. index 추가 전후를 측정한다

```text
1. 문제 query와 parameter 저장
2. EXPLAIN/ANALYZE로 기존 plan 기록
3. index 추가
4. 같은 조건으로 다시 측정
5. read 개선 확인
6. write/size 영향 확인
```

`index 만들었으니 빨라졌을 것`이라고 추측하지 않는다.

#### 17. 책을 덮고 확인한다

1. index를 책 뒤 색인에 비유해 설명하라.
2. index가 read를 빠르게 해도 write 비용을 늘리는 이유는 무엇인가?
3. 복합 index에서 column 순서가 중요한 이유는 무엇인가?
4. EXPLAIN은 무엇을 보여 주는가?
5. EXPLAIN ANALYZE를 변경 query에서 조심해야 하는 이유는 무엇인가?
6. N+1이 index만으로 해결되지 않을 수 있는 이유는 무엇인가?

---

## BLOCK 08 · 여러 변경을 한 작업처럼 안전하게 처리한다

### LESSON 01 · transaction·ACID·lock·deadlock·isolation·rollback을 계좌 이체로 단계별로 배운다

#### 1. 계좌 이체를 두 UPDATE로만 하면 어떤 일이 생길까

A 계좌 100만원.

B 계좌 50만원.

A에서 B로 10만원 보낸다.

해야 할 일:

```text
A: -100,000
B: +100,000
```

첫 UPDATE 성공:

```text
A = 900,000
```

그 다음 process가 죽었다.

B UPDATE는 실행되지 않았다.

결과:

```text
A에서는 돈이 빠짐
B에는 안 들어감
```

데이터 무결성이 깨졌다.

#### 2. 두 변경을 하나의 작업 단위로 묶고 싶다

```text
둘 다 성공
또는
둘 다 취소
```

이런 작업 단위를 **transaction(트랜잭션)**이라고 부른다.

SQL 큰 흐름:

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 100000
WHERE id = 1;

UPDATE accounts
SET balance = balance + 100000
WHERE id = 2;

COMMIT;
```

문제가 생기면:

```sql
ROLLBACK;
```

으로 transaction 변경을 취소할 수 있다.

#### 3. commit은 transaction 변경을 확정한다

```text
BEGIN
↓
여러 SQL
↓
검사
↓
COMMIT
```

commit 이후에는 다른 transaction에서도 확정된 data를 볼 수 있게 된다.

DBMS와 isolation에 따라 보이는 시점 세부는 다르다.

#### 4. rollback은 현재 transaction의 변경을 되돌린다

```text
A 차감 성공
B 증가 실패
↓
ROLLBACK
↓
A 차감도 취소
```

그래서 application code에서도 오류가 나면 transaction을 rollback하는 처리가 필요하다.

많은 framework/ORM은 transaction helper를 제공한다.

#### 5. transaction 안에서 외부 email/결제까지 rollback되지는 않는다

DB transaction 중:

```text
DB update
↓
외부 SMS 전송 성공
↓
DB rollback
```

DB는 되돌아갔지만 SMS는 이미 전송됐다.

DB transaction이 인터넷의 모든 side effect를 되돌리는 마법은 아니다.

TRACK 07의 outbox/idempotency가 필요한 이유다.

#### 6. ACID는 transaction이 지키려는 핵심 성질을 묶은 말이다

```text
A = Atomicity
C = Consistency
I = Isolation
D = Durability
```

단어부터 외우지 않고 계좌 이체에 붙인다.

#### 7. Atomicity — 전부 또는 전혀

A 차감과 B 증가를 하나의 단위로 본다.

```text
둘 다 commit
또는
둘 다 rollback
```

중간 절반 상태를 남기지 않는다.

이것이 **Atomicity(원자성)**다.

#### 8. Consistency — transaction 전후에도 DB 규칙을 지킨다

예:

```text
balance가 허용 규칙을 지킨다.
FOREIGN KEY가 깨지지 않는다.
총액 불변조건이 유지된다.
```

transaction이 올바른 상태에서 시작해 application/DB 규칙을 지키는 또 다른 올바른 상태로 끝나야 한다.

DB가 business logic을 자동으로 모두 보장한다는 뜻은 아니다. 잘못된 application transaction도 일관되게 잘못된 data를 commit할 수 있다.

#### 9. Isolation — 동시에 실행돼도 서로 함부로 중간 상태를 보지 않게 한다

두 사람이 같은 좌석 1개를 동시에 예약한다.

```text
A: 빈 좌석 확인
B: 빈 좌석 확인
A: 예약
B: 예약
```

둘 다 성공하면 중복 예약이다.

transaction isolation/lock/unique constraint 등을 이용해 concurrent transaction 충돌을 제어해야 한다.

#### 10. Durability — commit된 data는 장애 후에도 보존돼야 한다

DB가 `COMMIT 성공`을 client에게 알려 줬다면 갑자기 process가 재시작돼도 확정된 변경이 사라지지 않도록 WAL/log와 storage를 이용해 복구한다.

이것이 **Durability(지속성)**와 연결된다.

#### 11. lock은 동시에 같은 data를 위험하게 바꾸지 않도록 제어한다

A transaction이 row를 수정하는 동안 DB가 row에 lock을 걸 수 있다.

다른 transaction은 기다리거나 충돌 오류를 받을 수 있다.

```text
T1: account 1 lock
↓ update

T2: account 1 update 시도
→ T1 종료까지 대기
```

#### 12. lock 범위가 크고 오래 유지되면 성능이 떨어진다

transaction 안에서:

```text
row update
↓
10초짜리 외부 API 기다림
↓
commit
```

하면 lock을 10초 이상 유지할 수 있다.

다른 request가 줄줄이 기다린다.

transaction은 필요한 범위로 짧게 유지하는 것이 중요하다.

#### 13. deadlock은 서로 상대 lock을 기다리는 상태다

Transaction A:

```text
row 1 lock
→ row 2 lock 기다림
```

Transaction B:

```text
row 2 lock
→ row 1 lock 기다림
```

둘 다 상대가 놓기를 기다린다.

```text
A holds 1, waits 2
B holds 2, waits 1
```

이것이 **deadlock**이다.

DB는 deadlock을 감지해 한 transaction을 강제로 실패시킬 수 있다.

application은 이 오류를 retry할 수 있도록 설계할 수 있다.

#### 14. lock 순서를 일관되게 하면 deadlock 위험을 줄일 수 있다

모든 code가 account id가 작은 것부터 lock한다.

```text
항상 1 → 2 순서
```

로 접근하면 서로 반대 순서로 lock하는 상황을 줄일 수 있다.

#### 15. isolation level은 동시 transaction이 서로 무엇을 볼 수 있는지 정한다

대표 개념:

```text
READ COMMITTED
REPEATABLE READ
SERIALIZABLE
```

DBMS마다 구현과 기본값이 다르다.

높은 isolation은 anomaly를 더 막을 수 있지만 lock/retry/성능 비용이 커질 수 있다.

무조건 가장 높은 level이 정답은 아니다.

#### 16. dirty read는 commit되지 않은 값을 읽는 문제다

T1:

```text
balance 100 → 0 변경
아직 commit 안 함
```

T2가 0을 읽었다.

그런데 T1이 rollback했다.

T2는 실제로 확정되지 않은 값을 본 것이다.

이런 것을 **dirty read**라고 부른다.

많은 일반 DB 설정은 이를 막는다.

#### 17. non-repeatable read는 같은 row를 두 번 읽었는데 값이 바뀌는 현상이다

T1:

```text
user age 읽음 = 20
```

그 사이 T2가 age=21 commit.

T1이 다시 읽으면 21.

같은 transaction 안에서 두 조회 결과가 달라질 수 있다.

isolation level에 따라 허용 여부가 달라진다.

#### 18. phantom read는 조건에 맞는 row 집합 자체가 달라지는 현상이다

T1:

```sql
SELECT * FROM orders WHERE amount >= 10000;
```

5 row.

T2가 새 order를 insert하고 commit.

T1이 같은 조건을 다시 조회하면 6 row가 될 수 있다.

새로운 `phantom` row가 나타난다.

#### 19. lost update를 application에서도 조심한다

A와 B가 같은 profile을 읽는다.

```text
둘 다 version 1 읽음
A 이름 수정 → 저장
B 주소 수정 → 옛 version 전체를 저장
A 이름 변경이 사라짐
```

optimistic locking/version column으로 충돌을 감지할 수 있다.

```text
UPDATE ... WHERE id=1 AND version=1
```

영향 row가 0이면 누군가 먼저 변경했다는 뜻이다.

#### 20. optimistic vs pessimistic locking을 큰 생각으로 본다

**pessimistic lock**:

```text
충돌할 것이라고 보고 미리 lock을 잡음
```

**optimistic lock**:

```text
대부분 충돌하지 않을 것이라고 보고 진행
마지막에 version 등으로 충돌 검출
```

업무 특성과 충돌 빈도에 따라 선택한다.

#### 21. 계좌 이체 transaction을 안전하게 생각한다

```text
1. A/B account row를 일관된 순서로 lock
2. A balance 충분한지 확인
3. A 차감
4. B 증가
5. transfer log 기록
6. constraint 확인
7. commit
```

외부 알림은 commit 뒤 outbox/event로 처리할 수 있다.

#### 22. TRACK 08 완료 기준

책을 보지 않고 다음을 직접 설명할 수 있어야 한다.

- file과 database가 각각 적합한 상황
- DBMS/table/row/column/schema/type
- CREATE/INSERT/SELECT/UPDATE/DELETE
- WHERE/ORDER BY/LIMIT과 parameterized query
- aggregate/GROUP BY/HAVING
- PRIMARY KEY/UNIQUE/NULL/NOT NULL/CHECK
- 데이터 무결성과 참조 무결성
- FOREIGN KEY와 delete policy
- 1:1, 1:N, N:M 관계와 junction table
- normalization과 denormalization의 trade-off
- INNER JOIN/LEFT JOIN/subquery/N+1
- index의 read/write trade-off와 복합 index
- EXPLAIN과 실제 query plan 확인
- transaction의 commit/rollback
- ACID 네 성질
- lock/deadlock/isolation anomaly
- optimistic/pessimistic locking

### TRACK 프로젝트 · 주문 database 설계와 고장 실험

다음 table을 직접 설계한다.

```text
users
products
orders
order_items
payments
```

반드시:

```text
PRIMARY KEY
UNIQUE email
NOT NULL
CHECK quantity > 0
FOREIGN KEY
N:M order_items
purchase unit_price snapshot
index
transaction
```

을 포함한다.

실험:

```text
1. 중복 email INSERT → 실패 확인
2. 음수 quantity → 실패 확인
3. 없는 user_id order → FK 실패 확인
4. LEFT JOIN으로 주문 없는 user 표시
5. index 전/후 EXPLAIN 비교
6. transaction 중간에 강제로 오류 → rollback 확인
7. 두 connection에서 같은 row를 동시에 수정해 lock/isolation 관찰
8. 반대 순서로 row lock해 deadlock 가능성 실험
```

DB 공부의 목표는 `PRIMARY KEY = 기본키`를 외우는 것이 아니다.

**어떤 잘못된 데이터 상태를 막아야 하고, 여러 row와 여러 사용자가 동시에 움직일 때도 그 규칙을 어떻게 유지할지 설계할 수 있어야 한다.**
