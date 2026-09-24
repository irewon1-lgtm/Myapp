def validate_task(task):
    if not isinstance(task, dict):
        raise TypeError("각 작업은 딕셔너리여야 합니다")

    required = ["id", "title", "done", "days"]
    for field in required:
        if field not in task:
            raise ValueError("필수 항목 누락: " + field)

    if type(task["id"]) is not int:
        raise TypeError("작업 번호는 정수여야 합니다")
    if task["id"] <= 0:
        raise ValueError("작업 번호는 양수여야 합니다")
    if not isinstance(task["title"], str):
        raise TypeError("제목은 문자열이어야 합니다")
    if task["title"].strip() == "":
        raise ValueError("제목은 비어 있을 수 없습니다")
    if type(task["done"]) is not bool:
        raise TypeError("완료 여부는 True 또는 False여야 합니다")
    if type(task["days"]) is not int:
        raise TypeError("남은 날 수는 정수여야 합니다")

def task_order(task):
    return (task["days"], task["id"])

def upcoming_tasks(tasks):
    """입력을 검증하고, 0~7일의 미완료 작업을 새 자료로 정렬해 반환한다."""
    if not isinstance(tasks, list):
        raise TypeError("작업 전체는 리스트여야 합니다")

    seen_ids = []
    for task in tasks:
        validate_task(task)
        if task["id"] in seen_ids:
            raise ValueError("중복 작업 번호")
        seen_ids.append(task["id"])

    result = []
    for task in tasks:
        if not task["done"] and 0 <= task["days"] <= 7:
            result.append({
                "id": task["id"],
                "title": task["title"],
                "done": task["done"],
                "days": task["days"],
            })
    return sorted(result, key=task_order)

from copy import deepcopy


def ids(tasks):
    result = []
    for task in tasks:
        result.append(task["id"])
    return result


def must_raise(exception_type, value):
    raised = False
    try:
        upcoming_tasks(value)
    except exception_type:
        raised = True
    assert raised


sample = [
    {"id": 30, "title": "완료", "done": True, "days": 2},
    {"id": 20, "title": "7일", "done": False, "days": 7},
    {"id": 12, "title": "오늘B", "done": False, "days": 0},
    {"id": 10, "title": "오늘A", "done": False, "days": 0},
    {"id": 40, "title": "지난 일", "done": False, "days": -1},
    {"id": 50, "title": "8일", "done": False, "days": 8},
]
before = deepcopy(sample)
result = upcoming_tasks(sample)
assert ids(result) == [10, 12, 20]
assert sample == before
assert result is not sample
result[0]["title"] = "변경한 제목"
assert sample == before
assert upcoming_tasks([]) == []
assert upcoming_tasks([sample[0]]) == []

valid = {"id": 1, "title": "작업", "done": False, "days": 1}
for field in ["id", "title", "done", "days"]:
    broken = valid.copy()
    del broken[field]
    must_raise(ValueError, [broken])

bad_cases = [
    ("id", True, TypeError),
    ("id", 0, ValueError),
    ("title", 123, TypeError),
    ("title", "   ", ValueError),
    ("done", "False", TypeError),
    ("done", 0, TypeError),
    ("days", "2", TypeError),
    ("days", True, TypeError),
]
for field, value, error in bad_cases:
    broken = valid.copy()
    broken[field] = value
    must_raise(error, [broken])

must_raise(TypeError, {})
must_raise(TypeError, ["작업"])
must_raise(ValueError, [valid, valid.copy()])

hidden_bad = valid.copy()
hidden_bad["done"] = True
hidden_bad["days"] = "2"
must_raise(TypeError, [hidden_bad])

with_extra = valid.copy()
with_extra["private_note"] = ["내부용"]
extra_before = deepcopy(with_extra)
extra_result = upcoming_tasks([with_extra])
assert set(extra_result[0]) == {"id", "title", "done", "days"}
assert with_extra == extra_before
print("선택·정렬·원본 보존·잘못된 입력 검사 통과")
