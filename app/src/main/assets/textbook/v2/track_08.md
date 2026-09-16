# TRACK 08 · 데이터베이스

이번 TRACK은 `DB`, `PRIMARY KEY`, `FOREIGN KEY`, `ACID` 같은 말을 처음 듣는 사람을 기준으로 시작한다.

먼저 아주 단순한 질문부터 간다.

> 프로그램을 껐다 켜도 데이터가 남아 있어야 한다면 어디에 저장할까?

그 답을 따라가다 보면 table, key, 관계, SQL, index, 무결성, transaction, ACID가 자연스럽게 나온다.

용어를 한꺼번에 던지지 않는다.

---

## BLOCK 01 · 데이터란 무엇인가

### LESSON 01 · 프로그램이 다루는 사실과 값

사용자 이름, 주문 금액, 일정 날짜처럼 프로그램이 저장하고 처리하는 정보를 **데이터(data)**라고 부른다.

예:

```text
사용자 이름 = 민수
나이 = 20
주문 금액 = 35000
```

### LESSON 02 · 데이터는 메모리에만 둘 수 있다

```python
name = "민수"
```

라고 변수에 저장하면 프로그램이 실행되는 동안에는 사용할 수 있다.

하지만 프로그램이 종료되면 메모리의 값은 사라질 수 있다.

오래 남길 저장 방식이 필요하다.

---

## BLOCK 02 · 파일에 저장하면 안 되나

### LESSON 01 · 작은 프로그램은 파일로도 가능하다

```text
users.json
orders.csv
memo.txt
```

처럼 파일에 데이터를 저장할 수 있다.

### LESSON 02 · 데이터가 커지면 문제가 생긴다

사용자가 100만 명이고 여러 서버가 동시에 수정한다고 하자.

```text
원하는 사용자 빠르게 찾기
두 사람이 동시에 수정
중복 막기
관계 연결
일부 실패 시 되돌리기
```

를 직접 구현해야 한다.

이런 문제를 체계적으로 해결하기 위해 데이터베이스를 사용한다.

---

## BLOCK 03 · 데이터베이스

### LESSON 01 · 데이터를 조직해서 저장하고 관리하는 시스템

**데이터베이스(database)**는 데이터를 일정한 구조로 저장하고 검색·수정·삭제할 수 있게 관리하는 시스템이다.

한 줄 뜻:

> 데이터베이스 = 많은 데이터를 안전하고 효율적으로 저장하고 찾기 위한 시스템

### LESSON 02 · DBMS

데이터베이스를 실제로 관리하는 소프트웨어를 **DBMS(Database Management System)**라고 부른다.

예:

```text
PostgreSQL
MySQL
SQLite
SQL Server
```

---

## BLOCK 04 · 관계형 데이터베이스

### LESSON 01 · 표 형태와 관계

**관계형 데이터베이스(relational database)**는 데이터를 table 형태로 저장하고 table 사이 관계를 표현하는 데이터베이스 종류다.

PostgreSQL, MySQL, SQLite 등이 대표적이다.

### LESSON 02 · 표가 Excel과 완전히 같지는 않다

보기에는 표지만 데이터 타입, key, constraint, transaction 같은 강한 규칙이 있다.

그 규칙이 데이터 정확성을 지키는 데 중요하다.

---

## BLOCK 05 · table

### LESSON 01 · 같은 종류의 데이터를 모은 표

사용자를 저장하는 table:

```text
id | name | age
1  | 민수 | 20
2  | 지수 | 25
```

**table**은 같은 종류의 데이터를 행과 열 구조로 모은 것이다.

### LESSON 02 · table 이름

```text
users
orders
products
```

처럼 무엇을 저장하는지 드러나는 이름을 사용한다.

---

## BLOCK 06 · row와 column

### LESSON 01 · row

table에서 하나의 데이터 기록을 **행(row)**이라고 부른다.

```text
1 | 민수 | 20
```

이 한 줄이 사용자 한 명의 row다.

### LESSON 02 · column

데이터의 특정 속성을 나타내는 세로 항목을 **열(column)**이라고 부른다.

```text
id
name
age
```

각 column은 어떤 종류의 값을 저장할지 정의한다.

---

## BLOCK 07 · schema

### LESSON 01 · 데이터 구조의 설계도

어떤 table이 있고 각 column의 이름·타입·규칙이 무엇인지 정의한 구조를 **스키마(schema)**라고 부른다.

예:

```text
users
  id: integer
  name: text
  age: integer
```

### LESSON 02 · 데이터와 schema

```text
schema = 데이터가 어떤 모양이어야 하는지 정한 구조
row data = 그 구조에 실제로 들어간 값
```

둘을 구분한다.

---

## BLOCK 08 · 데이터 타입

### LESSON 01 · column마다 값 종류를 정한다

```text
INTEGER = 정수
TEXT = 문자열
BOOLEAN = 참/거짓
DATE = 날짜
TIMESTAMP = 날짜+시간
```

DBMS마다 정확한 타입 이름과 동작은 다르다.

### LESSON 02 · 왜 타입이 필요한가

`age` column에 아무 문자열이나 들어가면 계산과 검증이 어려워진다.

데이터베이스 타입은 잘못된 종류의 값을 일부 막아 준다.

---

## BLOCK 09 · SQL

### LESSON 01 · 관계형 DB에 명령하는 언어

**SQL(Structured Query Language)**은 관계형 데이터베이스에서 데이터를 정의하고 조회·수정하는 데 사용하는 표준화된 언어다.

### LESSON 02 · query

DB에 보내는 SQL 명령을 흔히 **쿼리(query)**라고 부른다.

```sql
SELECT name FROM users;
```

사람말:

```text
users table에서 name 값을 조회한다.
```

---

## BLOCK 10 · CREATE TABLE

### LESSON 01 · table 만들기

```sql
CREATE TABLE users (
  id INTEGER,
  name TEXT,
  age INTEGER
);
```

### LESSON 02 · 문법보다 구조 읽기

```text
users라는 table을 만든다.
id는 integer
name은 text
age는 integer
```

이라고 읽는다.

---

## BLOCK 11 · INSERT

### LESSON 01 · 새 row 추가

```sql
INSERT INTO users (id, name, age)
VALUES (1, '민수', 20);
```

`users` table에 새 사용자 row를 추가한다.

### LESSON 02 · column 순서 확인

column 목록과 VALUES의 값 순서가 맞아야 한다.

```text
id → 1
name → 민수
age → 20
```

---

## BLOCK 12 · SELECT

### LESSON 01 · 데이터 조회

```sql
SELECT id, name
FROM users;
```

users의 id와 name을 조회한다.

### LESSON 02 · SELECT *

```sql
SELECT * FROM users;
```

모든 column을 가져온다.

작은 실습에는 편하지만 운영 코드에서 필요 없는 column까지 읽으면 비용과 데이터 노출이 늘 수 있다.

필요한 column을 명시하는 습관이 좋다.

---

## BLOCK 13 · WHERE

### LESSON 01 · 조건에 맞는 row만

```sql
SELECT id, name
FROM users
WHERE age >= 20;
```

20살 이상 사용자만 찾는다.

### LESSON 02 · 조건 여러 개

```sql
WHERE age >= 20 AND active = TRUE
```

처럼 여러 조건을 조합할 수 있다.

---

## BLOCK 14 · UPDATE

### LESSON 01 · 기존 데이터 수정

```sql
UPDATE users
SET age = 21
WHERE id = 1;
```

id 1 사용자의 age를 21로 바꾼다.

### LESSON 02 · WHERE를 빼면

```sql
UPDATE users SET age = 21;
```

모든 row의 age가 바뀔 수 있다.

변경 query는 대상 범위를 먼저 SELECT로 확인하는 습관이 중요하다.

---

## BLOCK 15 · DELETE

### LESSON 01 · row 삭제

```sql
DELETE FROM users
WHERE id = 1;
```

### LESSON 02 · 전체 삭제 위험

```sql
DELETE FROM users;
```

은 모든 row를 삭제할 수 있다.

운영 DB에서는 backup, transaction, 권한, 확인 절차 없이 실행하지 않는다.

---

## BLOCK 16 · ORDER BY와 LIMIT

### LESSON 01 · 정렬

```sql
SELECT * FROM users
ORDER BY age DESC;
```

age가 큰 순서로 정렬한다.

### LESSON 02 · 개수 제한

```sql
SELECT * FROM users
LIMIT 10;
```

최대 10개 row를 가져온다.

대량 데이터를 한꺼번에 읽는 것을 줄일 수 있다.

---

## BLOCK 17 · aggregate function

### LESSON 01 · 여러 row를 하나의 값으로 계산

```sql
SELECT COUNT(*) FROM users;
```

row 개수를 센다.

### LESSON 02 · 대표 함수

```text
COUNT = 개수
SUM = 합계
AVG = 평균
MIN = 최솟값
MAX = 최댓값
```

이런 함수를 **집계 함수(aggregate function)**라고 부른다.

---

## BLOCK 18 · GROUP BY

### LESSON 01 · 그룹별 집계

카테고리별 주문 금액 합계를 구한다고 하자.

```sql
SELECT category, SUM(amount)
FROM orders
GROUP BY category;
```

같은 category row를 묶어서 합계를 계산한다.

### LESSON 02 · HAVING

그룹 계산 결과에 조건을 걸 때 `HAVING`을 사용할 수 있다.

```sql
HAVING SUM(amount) >= 100000
```

---

## BLOCK 19 · ID가 왜 필요한가

### LESSON 01 · 이름은 중복될 수 있다

사용자 이름이 `민수`인 사람이 두 명일 수 있다.

```text
민수, 20세
민수, 35세
```

이름만으로 정확한 사용자를 구분하기 어렵다.

### LESSON 02 · 고유 식별값

각 row를 고유하게 구분하는 ID를 둔다.

```text
id=101 → 민수
id=102 → 민수
```

이제 어떤 민수인지 정확히 알 수 있다.

---

## BLOCK 20 · PRIMARY KEY

### LESSON 01 · table의 row를 고유하게 식별

table에서 각 row를 유일하게 구분하기 위한 핵심 key를 **PRIMARY KEY(기본키)**라고 부른다.

```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT
);
```

### LESSON 02 · 왜 중복되면 안 되나

id 1이 두 명이면:

```text
UPDATE users WHERE id = 1
```

이 어느 한 명을 뜻하는지 모호해진다.

PRIMARY KEY는 중복될 수 없고 NULL일 수 없다.

---

## BLOCK 21 · UNIQUE

### LESSON 01 · 이 값도 중복되면 안 된다

PRIMARY KEY가 아닌 column도 중복을 금지할 수 있다.

```sql
email TEXT UNIQUE
```

**UNIQUE constraint**는 해당 값의 중복을 막는다.

### LESSON 02 · PRIMARY KEY와 UNIQUE

한 table의 대표 식별자는 PRIMARY KEY다.

그 외 `이 값도 중복되면 안 됨` 규칙에 UNIQUE를 사용할 수 있다.

DBMS마다 NULL과 UNIQUE 처리 세부는 다를 수 있다.

---

## BLOCK 22 · NULL

### LESSON 01 · 값이 없음

DB에서 `NULL`은 값이 없거나 알 수 없음을 나타내는 특별한 상태다.

```text
nickname = NULL
```

### LESSON 02 · 빈 문자열과 다르다

```text
NULL = 값 자체가 없음/모름
''   = 길이가 0인 문자열 값
```

서로 다른 상태다.

---

## BLOCK 23 · NOT NULL

### LESSON 01 · 반드시 값이 있어야 한다

```sql
name TEXT NOT NULL
```

**NOT NULL constraint**는 해당 column에 NULL을 허용하지 않는다.

### LESSON 02 · 앱 코드만 믿지 않는다

프론트엔드에서 이름 필수 입력을 만들었어도 다른 경로로 DB에 잘못된 데이터가 들어올 수 있다.

DB constraint를 두면 마지막 방어선이 된다.

---

## BLOCK 24 · CHECK

### LESSON 01 · 값 범위를 DB에서도 검사

```sql
age INTEGER CHECK (age >= 0)
```

**CHECK constraint**는 row의 값이 정해진 조건을 만족하는지 검사한다.

### LESSON 02 · 무결성 규칙을 데이터 가까이에 둔다

나이는 음수가 될 수 없다는 불변 규칙을 여러 앱 코드에 흩어 놓는 대신 DB에도 선언하면 잘못된 데이터를 막는 데 도움이 된다.

---

## BLOCK 25 · 관계가 필요한 이유

### LESSON 01 · 주문에는 사용자가 있다

주문 table:

```text
order_id | user_id | amount
1        | 10      | 30000
```

`user_id=10`은 users table의 사용자 10을 가리킨다.

### LESSON 02 · 같은 사용자 정보를 주문마다 복사하면

주문 row마다 사용자 이름·주소를 반복 저장하면 사용자 이름을 바꿀 때 여러 row를 모두 수정해야 한다.

관계를 이용하면 중복을 줄일 수 있다.

---

## BLOCK 26 · FOREIGN KEY

### LESSON 01 · 다른 table의 row를 가리키는 key

한 table의 column이 다른 table의 PRIMARY KEY를 참조하도록 만든 것을 **FOREIGN KEY(외래키)**라고 부른다.

```sql
CREATE TABLE orders (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### LESSON 02 · 존재하지 않는 사용자 주문 막기

users에 id 999가 없는데 order의 user_id를 999로 넣으려 하면 FOREIGN KEY constraint가 막을 수 있다.

이렇게 table 사이 참조가 깨지지 않게 한다.

---

## BLOCK 27 · referential integrity

### LESSON 01 · 참조가 실제 row를 가리키게 유지

orders.user_id가 users.id를 참조한다면 항상 존재하는 사용자와 연결되어야 한다.

이런 참조 관계의 올바름을 **참조 무결성(referential integrity)**이라고 부른다.

### LESSON 02 · 사용자를 삭제하면 주문은?

사용자 row를 삭제하면 그 사용자를 참조하던 주문이 남을 수 있다.

정책을 정해야 한다.

```text
삭제 금지
주문도 함께 삭제(CASCADE)
user_id를 NULL로 변경
```

데이터 의미에 맞게 선택한다.

---

## BLOCK 28 · 데이터 무결성

### LESSON 01 · 무결성은 어려운 말이 아니다

**데이터 무결성(data integrity)**은 데이터가 정해진 규칙을 지키고 서로 모순되지 않은 올바른 상태를 유지하는 성질이다.

예:

```text
사용자 id 중복 없음
나이 음수 없음
없는 사용자에게 주문 연결 없음
필수 이름 NULL 없음
```

### LESSON 02 · constraint가 무결성을 지킨다

```text
PRIMARY KEY → row 고유성
UNIQUE → 특정 값 중복 금지
NOT NULL → 필수 값 존재
CHECK → 값 조건
FOREIGN KEY → 참조 관계
```

이제 이름을 각각 배웠기 때문에 목록이 의미를 가진다.

---

## BLOCK 29 · 관계 종류

### LESSON 01 · one-to-one

한 사용자에게 프로필 하나처럼 1:1 관계를 만들 수 있다.

### LESSON 02 · one-to-many

한 사용자에게 주문 여러 개:

```text
User 1 → Order A
       → Order B
       → Order C
```

이를 1:N 관계라고 부른다.

### LESSON 03 · many-to-many

학생 여러 명이 과목 여러 개를 듣는다.

학생과 과목 사이를 직접 연결하기보다 `enrollments` 같은 중간 table을 만든다.

---

## BLOCK 30 · JOIN

### LESSON 01 · 관계를 따라 table을 합쳐 조회

orders에는 user_id만 있다.

사용자 이름까지 보고 싶다면 users와 연결해야 한다.

```sql
SELECT orders.id, users.name, orders.amount
FROM orders
JOIN users ON users.id = orders.user_id;
```

### LESSON 02 · ON

`ON users.id = orders.user_id`는 두 table의 어떤 row를 서로 연결할지 정하는 조건이다.

잘못된 JOIN 조건은 row가 폭증하거나 엉뚱한 결과를 만들 수 있다.

---

## BLOCK 31 · INNER JOIN과 LEFT JOIN

### LESSON 01 · INNER JOIN

양쪽 table 모두에 연결되는 row만 결과에 포함한다.

### LESSON 02 · LEFT JOIN

왼쪽 table row는 모두 유지하고 오른쪽에서 연결되는 값이 없으면 NULL로 채운다.

```text
users 전체를 보고 싶고 주문이 없어도 사용자는 보여야 함
→ LEFT JOIN 후보
```

---

## BLOCK 32 · self join과 여러 JOIN

### LESSON 01 · 같은 table끼리 연결

직원 table에 manager_id가 같은 employees.id를 가리킬 수 있다.

이때 같은 table을 서로 다른 별칭으로 JOIN한다.

### LESSON 02 · JOIN이 많으면 복잡도가 늘어난다

다섯 table을 한꺼번에 JOIN하면 데이터 중복과 성능을 잘 확인해야 한다.

먼저 관계 구조를 그림으로 그린다.

---

## BLOCK 33 · index를 배우기 전에 검색 비용

### LESSON 01 · 1억 row에서 한 사용자를 찾는다면

index가 없으면 DB가 많은 row를 하나씩 확인해야 할 수 있다.

```text
1
2
3
...
100,000,000
```

이를 **full table scan**이라고 부른다.

### LESSON 02 · 책의 색인

책 뒤의 색인을 보면 원하는 단어가 어느 페이지에 있는지 바로 찾을 수 있다.

DB index도 비슷하게 검색할 값을 별도 구조에 정리한다.

---

## BLOCK 34 · INDEX

### LESSON 01 · 빠른 검색을 위한 별도 자료구조

**index**는 특정 column 값을 빠르게 찾기 위해 DB가 별도로 관리하는 자료구조다.

```sql
CREATE INDEX idx_users_email
ON users(email);
```

### LESSON 02 · 공짜가 아니다

index는:

```text
추가 저장공간 사용
INSERT/UPDATE/DELETE 때 index도 갱신
```

비용이 있다.

모든 column에 index를 만들면 오히려 쓰기 성능과 공간이 나빠질 수 있다.

---

## BLOCK 35 · B-tree index

### LESSON 01 · 균형 트리 구조

많은 관계형 DB는 B-tree 계열 index를 기본적으로 사용한다.

B-tree는 많은 key를 정렬된 상태로 관리하며 트리 높이를 낮게 유지하도록 설계된다.

### LESSON 02 · 어떤 query에 유리한가

```text
email = ?
age > 30
created_at BETWEEN ...
ORDER BY created_at
```

같은 equality/range 검색과 정렬에 유용할 수 있다.

실제 사용 여부는 query plan으로 확인한다.

---

## BLOCK 36 · composite index

### LESSON 01 · 여러 column을 함께 index

```sql
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at);
```

처럼 여러 column을 묶을 수 있다.

### LESSON 02 · column 순서가 중요하다

`(user_id, created_at)` index와 `(created_at, user_id)` index는 같은 것이 아니다.

실제 WHERE, ORDER BY 패턴에 맞춰 설계한다.

---

## BLOCK 37 · query plan

### LESSON 01 · DB가 query를 어떻게 실행할지 본다

SQL 한 문장을 DB가 실제로 어떤 순서로 처리할지 만든 실행 계획을 **query plan**이라고 부른다.

PostgreSQL에서는:

```sql
EXPLAIN SELECT ...;
```

으로 계획을 볼 수 있다.

### LESSON 02 · EXPLAIN ANALYZE

실제 query를 실행하며 시간과 row 수를 측정하는 기능도 있다.

운영 데이터에서 변경 query에 함부로 사용하지 않고 안전한 환경에서 실행한다.

---

## BLOCK 38 · N+1 query

### LESSON 01 · 목록 하나 때문에 query 101번

주문 100개를 먼저 조회한다.

그 다음 주문마다 사용자 query를 한 번씩 실행한다.

```text
1 + 100 = 101 query
```

이를 **N+1 query problem**이라고 부른다.

### LESSON 02 · 해결 후보

```text
JOIN
IN query로 batch 조회
ORM eager loading
```

등으로 query 수를 줄일 수 있다.

---

## BLOCK 39 · ORM

### LESSON 01 · 객체 코드로 DB를 다루는 도구

**ORM(Object-Relational Mapping)**은 프로그램의 객체와 관계형 DB table 사이를 연결해 SQL 작성을 일부 자동화하는 도구다.

예:

```text
Hibernate
Prisma
SQLAlchemy
Django ORM
```

### LESSON 02 · ORM이 SQL을 없애지는 않는다

ORM이 잘못된 query를 생성할 수 있고 N+1이 생길 수 있다.

중요한 성능 문제에서는 실제 생성 SQL과 query plan을 확인한다.

---

## BLOCK 40 · 정규화

### LESSON 01 · 중복과 수정 이상 줄이기

같은 정보를 여러 곳에 반복 저장하면 한 곳만 수정되어 모순이 생길 수 있다.

데이터를 적절한 table로 나누어 중복과 이상을 줄이는 설계를 **정규화(normalization)**라고 부른다.

### LESSON 02 · update anomaly

```text
orders row 100개에 고객 주소 복사
```

고객이 이사하면 100개를 모두 바꿔야 한다.

하나를 빼먹으면 주소가 서로 달라진다.

이런 문제를 **갱신 이상(update anomaly)**이라고 부른다.

---

## BLOCK 41 · 1NF

### LESSON 01 · 한 칸에는 하나의 원자적 값

**제1정규형(1NF)**은 column 하나에 반복 그룹을 넣지 않고 하나의 값으로 다루는 기본 원칙이다.

나쁜 예:

```text
phone = "010..., 011..., 02..."
```

전화번호가 여러 개라면 별도 row/table로 표현할 수 있다.

---

## BLOCK 42 · 2NF와 3NF

### LESSON 01 · 2NF

복합 PRIMARY KEY를 사용하는 table에서 key 일부에만 의존하는 column을 분리하는 원칙이 **2NF**다.

### LESSON 02 · 3NF

key가 아닌 column이 다른 key가 아닌 column에 의존하는 구조를 줄이는 원칙이 **3NF**다.

### LESSON 03 · 이름 암기보다 의존성 질문

```text
이 값은 무엇에 의해 결정되는가?
같은 사실을 여러 곳에 저장하고 있는가?
한 값을 바꾸려면 여러 row를 수정해야 하는가?
```

를 묻는 것이 더 중요하다.

---

## BLOCK 43 · denormalization

### LESSON 01 · 일부 중복을 의도적으로 허용

읽기 성능이나 단순화를 위해 일부 데이터를 중복 저장하는 것을 **비정규화(denormalization)**라고 부른다.

### LESSON 02 · 공짜 최적화가 아니다

중복 값을 서로 맞추는 책임이 생긴다.

측정과 명확한 이유 없이 미리 비정규화하지 않는다.

---

## BLOCK 44 · transaction을 배우기 전에

### LESSON 01 · 은행 이체

A 계좌에서 10만원 빼고 B 계좌에 10만원 넣는다.

```text
A -100000 성공
↓
서버 장애
↓
B +100000 실패
```

중간 상태로 남으면 돈이 사라진다.

두 변경을 하나의 작업처럼 묶어야 한다.

---

## BLOCK 45 · transaction

### LESSON 01 · 여러 DB 작업을 하나의 작업 단위로

**transaction**은 여러 데이터베이스 작업을 하나의 논리적 작업 단위로 묶는 기능이다.

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100000 WHERE id = 1;
UPDATE accounts SET balance = balance + 100000 WHERE id = 2;
COMMIT;
```

### LESSON 02 · COMMIT

transaction 안의 변경을 최종 확정하는 것을 **COMMIT**이라고 부른다.

### LESSON 03 · ROLLBACK

문제가 생겼을 때 transaction 안의 변경을 취소하는 것을 **ROLLBACK**이라고 부른다.

---

## BLOCK 46 · ACID를 배우기 전에

### LESSON 01 · 네 글자를 한꺼번에 외우지 않는다

transaction이 신뢰할 수 있으려면 어떤 성질이 필요한지 하나씩 본다.

```text
중간까지만 반영되면 안 됨
규칙이 깨지면 안 됨
동시에 실행되어도 이상하면 안 됨
완료한 데이터가 사라지면 안 됨
```

이 네 생각에 이름이 붙은 것이 ACID다.

---

## BLOCK 47 · Atomicity

### LESSON 01 · 전부 성공하거나 전부 취소

**Atomicity(원자성)**는 transaction의 작업이 전부 성공하거나 전부 실패한 것처럼 처리되어야 한다는 성질이다.

은행 이체에서 A 차감만 남는 상황을 막는다.

```text
A 차감 + B 증가 = 둘 다 반영
또는
둘 다 취소
```

---

## BLOCK 48 · Consistency

### LESSON 01 · 규칙을 지키는 상태에서 다른 올바른 상태로

**Consistency(일관성)**는 transaction이 DB의 constraint와 불변 규칙을 깨지 않고 올바른 상태에서 다른 올바른 상태로 이동해야 한다는 성질이다.

예:

```text
PRIMARY KEY 중복 금지
FOREIGN KEY 참조 유지
balance가 허용 범위 유지
```

### LESSON 02 · 앱의 모든 의미를 DB가 자동으로 보장하는 것은 아니다

DB constraint로 표현하지 않은 비즈니스 규칙은 application이 지켜야 할 수도 있다.

ACID의 consistency가 `모든 복제본이 즉시 같은 값`이라는 분산시스템 consistency와 같은 뜻은 아니다.

---

## BLOCK 49 · Isolation

### LESSON 01 · 동시에 transaction이 실행될 때

사용자 A와 B가 같은 재고 1개를 동시에 주문한다고 하자.

둘 다 `재고=1`을 읽고 각각 주문하면 재고가 -1이 될 수 있다.

**Isolation(격리성)**은 동시 transaction들이 서로의 중간 작업 때문에 비정상 결과를 만들지 않도록 격리하는 성질이다.

### LESSON 02 · 완벽한 직렬 실행과 성능

모든 transaction을 완전히 한 줄로 세우면 안전할 수 있지만 성능이 떨어질 수 있다.

DB는 여러 **isolation level**을 제공해 성능과 이상 현상 방지 사이를 조절한다.

---

## BLOCK 50 · Durability

### LESSON 01 · COMMIT된 데이터는 남아야 한다

**Durability(지속성)**는 transaction이 성공적으로 COMMIT된 뒤 장애가 나도 그 결과가 보존되어야 한다는 성질이다.

DBMS는 로그, 디스크 flush, 복제 같은 기술을 이용해 이를 지원한다.

---

## BLOCK 51 · ACID 한 번에 연결하기

### LESSON 01 · 네 글자의 사람말

```text
A Atomicity   = 전부 아니면 전무
C Consistency = 규칙을 지키는 상태 유지
I Isolation   = 동시에 실행되어도 서로 중간 상태 때문에 망가지지 않게
D Durability  = 완료한 결과가 장애 후에도 남게
```

이제 이름을 처음부터 외우는 것이 아니라 이미 이해한 네 성질에 영어 이름을 붙인다.

---

## BLOCK 52 · dirty read

### LESSON 01 · 아직 COMMIT되지 않은 값을 읽는다

transaction A가 값을 바꿨지만 아직 COMMIT하지 않았다.

transaction B가 그 값을 읽었다.

A가 ROLLBACK하면 B는 실제로 확정되지 않은 값을 읽은 것이다.

이 현상을 **dirty read**라고 부른다.

---

## BLOCK 53 · non-repeatable read

### LESSON 01 · 같은 row를 두 번 읽었는데 값이 달라짐

transaction A가 user 1을 읽었다.

그 사이 transaction B가 user 1을 수정하고 COMMIT했다.

A가 다시 읽으니 값이 달라졌다.

이를 **non-repeatable read**라고 부른다.

---

## BLOCK 54 · phantom read

### LESSON 01 · 같은 조건 조회에 새로운 row가 나타남

transaction A가:

```sql
SELECT * FROM orders WHERE amount > 10000;
```

를 실행했다.

B가 조건에 맞는 새 order를 INSERT하고 COMMIT했다.

A가 같은 query를 다시 실행하니 row가 하나 늘었다.

이런 현상을 **phantom read**라고 부른다.

---

## BLOCK 55 · isolation level

### LESSON 01 · 격리 강도를 선택

SQL DB는 대표적으로:

```text
READ UNCOMMITTED
READ COMMITTED
REPEATABLE READ
SERIALIZABLE
```

같은 isolation level을 제공할 수 있다.

### LESSON 02 · 높은 격리 = 무조건 최고가 아니다

강한 격리는 동시성을 줄이거나 충돌·재시도를 늘릴 수 있다.

업무 정확성과 성능 요구에 맞춰 선택한다.

DBMS마다 실제 구현과 보장에 차이가 있다.

---

## BLOCK 56 · lock

### LESSON 01 · 동시에 같은 데이터를 바꾸지 못하게 잠근다

DB가 row나 table 일부를 다른 transaction이 동시에 변경하지 못하게 막는 것을 **lock(잠금)**이라고 부른다.

### LESSON 02 · row lock

특정 row만 잠그면 다른 row는 동시에 작업할 수 있다.

잠금 범위가 작으면 동시성이 좋아질 수 있다.

---

## BLOCK 57 · deadlock

### LESSON 01 · 서로가 가진 lock을 기다림

```text
Transaction A: row 1 lock 보유 → row 2 기다림
Transaction B: row 2 lock 보유 → row 1 기다림
```

둘이 서로 기다려 진행하지 못한다.

이를 **deadlock**이라고 부른다.

### LESSON 02 · DB가 하나를 중단시킬 수 있다

DBMS는 deadlock을 감지해 transaction 하나를 rollback시켜 순환 대기를 풀 수 있다.

application은 해당 오류를 적절히 retry할 준비가 필요할 수 있다.

---

## BLOCK 58 · optimistic locking

### LESSON 01 · 일단 읽고 마지막에 충돌 확인

충돌이 드물다고 가정하고 row의 version 값을 이용해 업데이트 시점에 다른 사람이 수정했는지 확인하는 방식을 **optimistic locking**이라고 부른다.

```text
읽을 때 version=5
업데이트: WHERE id=1 AND version=5
```

이미 version=6이면 업데이트가 0건이라 충돌을 알 수 있다.

---

## BLOCK 59 · pessimistic locking

### LESSON 01 · 먼저 lock을 잡고 작업

충돌 가능성이 높거나 반드시 순서를 보장해야 할 때 데이터를 읽으면서 lock을 잡는 방식을 **pessimistic locking**이라고 부른다.

예:

```sql
SELECT ... FOR UPDATE;
```

다른 transaction은 lock이 풀릴 때까지 기다릴 수 있다.

---

## BLOCK 60 · MVCC

### LESSON 01 · row의 여러 version을 이용해 읽기와 쓰기 충돌 줄이기

PostgreSQL 같은 DB는 **MVCC(Multi-Version Concurrency Control)**라는 방식을 사용한다.

데이터의 여러 version을 관리해 읽기 transaction이 쓰기 transaction과 덜 막히게 할 수 있다.

### LESSON 02 · 오래된 version은 정리해야 한다

여러 version이 영원히 쌓일 수는 없다.

DBMS는 vacuum 같은 정리 작업을 사용한다.

세부 구현은 DBMS마다 다르다.

---

## BLOCK 61 · migration

### LESSON 01 · schema를 안전하게 바꾸기

앱 업데이트로 column을 추가하거나 table을 나눌 수 있다.

기존 schema를 새 schema로 바꾸는 작업을 **migration**이라고 부른다.

### LESSON 02 · 앱 코드와 DB 변경 순서

서버 코드가 새 column을 먼저 기대하는데 DB migration이 아직 안 됐으면 오류가 난다.

배포 순서를 설계해야 한다.

---

## BLOCK 62 · backup과 restore

### LESSON 01 · backup

장애에 대비해 데이터의 별도 복사본을 만드는 것을 **backup**이라고 부른다.

### LESSON 02 · restore

backup에서 데이터를 실제로 되살리는 과정을 **restore**라고 부른다.

backup 파일이 존재하는 것과 복원이 실제로 가능한 것은 다르다.

정기적으로 restore test를 해야 한다.

---

## BLOCK 63 · replication

### LESSON 01 · 여러 DB 서버에 데이터 복사

같은 데이터를 여러 DB 서버에 복제하는 것을 **replication**이라고 부른다.

읽기 부하 분산과 장애 대응에 사용할 수 있다.

### LESSON 02 · replica lag

주 DB에 쓰기가 완료된 뒤 replica에 반영되기까지 시간이 걸릴 수 있다.

이를 **replica lag**이라고 부른다.

쓰기 직후 replica에서 읽으면 예전 데이터가 보일 수 있다.

---

## BLOCK 64 · partitioning과 sharding 맛보기

### LESSON 01 · partition

큰 table을 날짜나 key 범위에 따라 여러 조각으로 나누는 것을 **partitioning**이라고 부른다.

### LESSON 02 · sharding

데이터를 여러 독립 DB 서버에 분산하는 것을 **sharding**이라고 부른다.

확장에는 도움이 되지만 JOIN, transaction, 운영 복잡도가 크게 늘어난다.

처음부터 필요하지 않으면 단일 DB를 잘 설계하는 것이 더 단순하다.

---

## BLOCK 65 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| database | 많은 데이터를 구조적으로 저장·검색·수정하는 시스템 |
| DBMS | database를 실제로 관리하는 소프트웨어 |
| relational database | table과 관계를 중심으로 데이터를 관리하는 DB |
| table | 같은 종류의 데이터를 행과 열로 모은 구조 |
| row | table의 데이터 기록 한 개 |
| column | row의 특정 속성 항목 |
| schema | table·column·타입·규칙을 정의한 구조 |
| SQL | 관계형 DB에 조회·변경 명령을 보내는 언어 |
| query | DB에 보내는 명령 |
| PRIMARY KEY | table의 각 row를 고유하게 식별하는 key |
| UNIQUE | 특정 column의 중복을 막는 규칙 |
| NULL | 값이 없거나 알 수 없음을 나타내는 상태 |
| NOT NULL | NULL을 허용하지 않는 규칙 |
| CHECK | 값이 조건을 만족하는지 DB가 검사하는 규칙 |
| FOREIGN KEY | 다른 table의 key를 참조하는 column 규칙 |
| referential integrity | FOREIGN KEY 참조가 올바른 row를 가리키는 상태 |
| data integrity | 데이터가 정해진 규칙을 지키며 올바른 상태를 유지하는 성질 |
| JOIN | 관계를 따라 여러 table의 row를 합쳐 조회하는 기능 |
| index | 빠른 검색을 위해 DB가 따로 관리하는 자료구조 |
| query plan | DB가 SQL을 실행할 실제 방법 계획 |
| N+1 | 목록 N개 때문에 추가 query를 N번 실행하는 문제 |
| normalization | 데이터 중복과 수정 이상을 줄이기 위해 구조를 나누는 설계 |
| transaction | 여러 DB 작업을 하나의 논리적 작업 단위로 묶는 기능 |
| COMMIT | transaction 변경을 최종 확정하는 것 |
| ROLLBACK | transaction 변경을 취소하는 것 |
| Atomicity | transaction이 전부 성공하거나 전부 취소되는 성질 |
| Consistency | constraint와 불변 규칙을 지키는 상태를 유지하는 성질 |
| Isolation | 동시 transaction이 서로의 중간 상태 때문에 망가지지 않게 하는 성질 |
| Durability | COMMIT 결과가 장애 뒤에도 보존되는 성질 |
| lock | 동시 접근을 제어하기 위해 데이터에 거는 잠금 |
| deadlock | transaction들이 서로 lock을 기다리며 진행하지 못하는 상태 |
| MVCC | 여러 row version을 이용해 동시성을 높이는 DB 방식 |
| migration | 기존 schema와 데이터를 새 구조로 바꾸는 작업 |
| replication | 데이터를 여러 DB 서버에 복제하는 것 |
| sharding | 데이터를 여러 독립 DB 서버로 나누는 확장 방식 |

---

## BLOCK 66 · TRACK 08 완료 기준

다음을 직접 할 수 있어야 한다.

- table/row/column/schema를 구분한다.
- CREATE/INSERT/SELECT/UPDATE/DELETE를 직접 작성한다.
- WHERE, ORDER BY, GROUP BY와 집계 함수를 사용한다.
- PRIMARY KEY가 왜 필요한지 설명한다.
- UNIQUE, NOT NULL, CHECK를 각각 언제 쓰는지 설명한다.
- FOREIGN KEY와 referential integrity를 설명한다.
- 데이터 무결성을 constraint와 연결해 설명한다.
- 1:1, 1:N, N:M 관계를 table로 설계한다.
- INNER JOIN과 LEFT JOIN 결과 차이를 예측한다.
- index가 검색을 빠르게 하지만 쓰기 비용을 늘릴 수 있음을 설명한다.
- EXPLAIN으로 query plan을 확인한다.
- N+1 문제를 발견한다.
- 정규화가 중복과 update anomaly를 줄이는 이유를 설명한다.
- transaction, COMMIT, ROLLBACK을 직접 사용한다.
- ACID 네 글자를 각각 실제 문제와 연결해 설명한다.
- dirty/non-repeatable/phantom read의 차이를 설명한다.
- lock과 deadlock을 재현하고 해결 원리를 설명한다.
- migration과 backup/restore를 실제로 시험한다.

### TRACK 프로젝트 · 쇼핑몰 데이터베이스

다음 table을 설계한다.

```text
users
products
orders
order_items
payments
```

필수 조건:

1. 모든 table에 적절한 PRIMARY KEY를 둔다.
2. email에는 UNIQUE를 둔다.
3. 필수 값에는 NOT NULL을 둔다.
4. price와 quantity가 잘못된 범위를 갖지 않도록 CHECK를 검토한다.
5. FOREIGN KEY로 주문-사용자, 주문항목-상품 관계를 연결한다.
6. 주문 상세를 JOIN 한 번으로 조회한다.
7. 사용자별 총 구매금액을 GROUP BY로 계산한다.
8. 실제 query plan을 보고 필요한 index를 추가한다.
9. index 전후 실행 계획과 시간을 비교한다.
10. 재고 감소 + 주문 생성 + payment 기록을 하나의 transaction으로 묶는다.
11. 중간에 일부러 오류를 내고 ROLLBACK되는지 확인한다.
12. 두 transaction이 같은 재고를 동시에 주문하는 상황을 재현한다.
13. isolation 또는 locking 전략으로 overselling을 막는다.
14. schema migration을 하나 작성한다.
15. backup을 만든 뒤 별도 DB에 restore해 row 수와 핵심 합계를 검증한다.

`ACID`를 외웠다고 끝나는 것이 아니다. **실제로 중간 실패와 동시 실행을 만들어 데이터가 어떻게 망가질 수 있는지 본 뒤 막을 수 있어야** 통과다.
