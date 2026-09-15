import ast
import contextlib
import io
import json
import sys
import time


class SandboxSecurityError(Exception):
    pass


class SandboxTimeoutError(Exception):
    pass


MAX_CODE_CHARS = 32768
MAX_AST_NODES = 4000
MAX_LITERAL_CHARS = 10000
MAX_RANGE_ITEMS = 10000


def _safe_range(*args):
    result = range(*args)
    if len(result) > MAX_RANGE_ITEMS:
        raise SandboxSecurityError(f"range is limited to {MAX_RANGE_ITEMS} items")
    return result


SAFE_BUILTINS = {
    "print": print,
    "range": _safe_range,
    "len": len,
    "min": min,
    "max": max,
    "sum": sum,
    "abs": abs,
    "round": round,
    "sorted": sorted,
    "enumerate": enumerate,
    "zip": zip,
    "list": list,
    "dict": dict,
    "set": set,
    "tuple": tuple,
    "str": str,
    "int": int,
    "float": float,
    "bool": bool,
    "isinstance": isinstance,
    "Exception": Exception,
    "ValueError": ValueError,
    "TypeError": TypeError,
    "IndexError": IndexError,
    "KeyError": KeyError,
    "ZeroDivisionError": ZeroDivisionError,
}

BANNED_CALL_NAMES = {
    "open", "exec", "eval", "compile", "__import__", "input", "help",
    "globals", "locals", "vars", "dir", "getattr", "setattr", "delattr",
    "breakpoint", "memoryview", "classmethod", "staticmethod", "property",
}

BANNED_NODES = (
    ast.Import, ast.ImportFrom, ast.AsyncFunctionDef, ast.Await, ast.AsyncFor,
    ast.AsyncWith, ast.With, ast.Global, ast.Nonlocal,
)


class SecurityValidator(ast.NodeVisitor):
    def __init__(self):
        self.node_count = 0

    def generic_visit(self, node):
        self.node_count += 1
        if self.node_count > MAX_AST_NODES:
            raise SandboxSecurityError("code is too complex for the learning sandbox")
        if isinstance(node, BANNED_NODES):
            raise SandboxSecurityError(f"{type(node).__name__} is not allowed in the learning sandbox")
        super().generic_visit(node)

    def visit_Name(self, node):
        if node.id.startswith("__"):
            raise SandboxSecurityError("dunder names are blocked")
        self.generic_visit(node)

    def visit_Attribute(self, node):
        if node.attr.startswith("_"):
            raise SandboxSecurityError("private and dunder attributes are blocked")
        self.generic_visit(node)

    def visit_Call(self, node):
        if isinstance(node.func, ast.Name) and node.func.id in BANNED_CALL_NAMES:
            raise SandboxSecurityError(f"{node.func.id}() is blocked")
        self.generic_visit(node)

    def visit_Constant(self, node):
        if isinstance(node.value, str) and len(node.value) > MAX_LITERAL_CHARS:
            raise SandboxSecurityError("string literal is too large")
        if isinstance(node.value, int) and abs(node.value) > 10**12:
            raise SandboxSecurityError("integer literal is too large")
        self.generic_visit(node)

    def visit_BinOp(self, node):
        if isinstance(node.op, ast.Mult):
            for side in (node.left, node.right):
                if isinstance(side, ast.Constant) and isinstance(side.value, int) and abs(side.value) > 100000:
                    raise SandboxSecurityError("large sequence multiplication is blocked")
        if isinstance(node.op, ast.Pow) and isinstance(node.right, ast.Constant) and isinstance(node.right.value, int) and node.right.value > 20:
            raise SandboxSecurityError("large exponentiation is blocked")
        self.generic_visit(node)


def _truncate(text, max_output):
    if len(text) <= max_output:
        return text
    return text[:max_output] + "\n...[output truncated by sandbox]"


def run_code(code, timeout_ms=2500, max_steps=50000, max_output=65536):
    started = time.monotonic()
    if not isinstance(code, str):
        return json.dumps({"success": False, "output": "", "errorMessage": "code must be text", "isSecurityViolation": False, "isTimeout": False})
    if len(code) > MAX_CODE_CHARS:
        return json.dumps({"success": False, "output": "", "errorMessage": "code is too long", "isSecurityViolation": True, "isTimeout": False})

    try:
        tree = ast.parse(code, mode="exec")
        SecurityValidator().visit(tree)
    except SandboxSecurityError as exc:
        return json.dumps({"success": False, "output": "", "errorMessage": f"[security] {exc}", "isSecurityViolation": True, "isTimeout": False})
    except SyntaxError as exc:
        return json.dumps({"success": False, "output": "", "errorMessage": f"SyntaxError: {exc.msg} (line {exc.lineno})", "isSecurityViolation": False, "isTimeout": False})

    deadline = started + (max(100, int(timeout_ms)) / 1000.0)
    steps = 0

    def tracer(frame, event, arg):
        nonlocal steps
        if event in ("line", "call"):
            steps += 1
            if steps > max_steps or time.monotonic() > deadline:
                raise SandboxTimeoutError("execution budget exceeded")
        return tracer

    stdout = io.StringIO()
    globals_dict = {"__builtins__": SAFE_BUILTINS}
    old_trace = sys.gettrace()

    try:
        compiled = compile(tree, "<learner>", "exec")
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stdout):
            sys.settrace(tracer)
            exec(compiled, globals_dict, globals_dict)
        return json.dumps({
            "success": True,
            "output": _truncate(stdout.getvalue(), int(max_output)),
            "errorMessage": None,
            "isSecurityViolation": False,
            "isTimeout": False,
        }, ensure_ascii=False)
    except SandboxTimeoutError as exc:
        return json.dumps({
            "success": False,
            "output": _truncate(stdout.getvalue(), int(max_output)),
            "errorMessage": f"TimeoutError: {exc}",
            "isSecurityViolation": False,
            "isTimeout": True,
        }, ensure_ascii=False)
    except SandboxSecurityError as exc:
        return json.dumps({
            "success": False,
            "output": _truncate(stdout.getvalue(), int(max_output)),
            "errorMessage": f"[security] {exc}",
            "isSecurityViolation": True,
            "isTimeout": False,
        }, ensure_ascii=False)
    except Exception as exc:
        return json.dumps({
            "success": False,
            "output": _truncate(stdout.getvalue(), int(max_output)),
            "errorMessage": f"{type(exc).__name__}: {exc}",
            "isSecurityViolation": False,
            "isTimeout": False,
        }, ensure_ascii=False)
    finally:
        sys.settrace(old_trace)
