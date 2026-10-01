import ast
from copy import deepcopy
import sys, inspect
import io
from dataclasses import dataclass
from typing import Type, List, Optional
import importlib.util
import runpy
import base64
import json
from dataclasses import asdict, is_dataclass
from datetime import datetime, date, time
from enum import Enum
from pathlib import Path
from typing import Any
from uuid import UUID


TRACE_LINE_REGEX:str = r"""\s*File "(.+?)", line (\d+), in (\S+)"""


class RunType:
    pass

@dataclass
class Script(RunType):
    script_path: str
    args: List[str]

@dataclass
class Module(RunType):
    module_name: str
    args: List[str]

def is_package_installed(package_name: str) -> bool:
    return importlib.util.find_spec(package_name) is not None


def execute_script(script_path: str, args: List[str], input_data: Any = None) -> None:
    original_argv = sys.argv.copy()
    original_stdin = sys.stdin
    try:
        # sys.argv[0] is the script name, it will be replaced by the runpy in run_path
        # but we need to put args ourselves
        sys.argv[1:] = args

        if input_data is not None:
            sys.stdin = io.StringIO(str(input_data) if not isinstance(input_data, str) else input_data)

        # Note: __package__ should be None, but the runpy sets it to '' instead. Shouldn't cause problems, though.

        runpy.run_path(script_path, run_name="__main__")
    finally:
        sys.argv = original_argv
        sys.stdin = original_stdin


def execute_module(module_name: str, args: List[str], input_data: Any = None) -> None:
    original_argv = sys.argv.copy()
    original_stdin = sys.stdin
    try:
        # sys.argv[0] is the script name, it will be replaced by the runpy due to alter_sys=True
        # but we need to put args ourselves
        sys.argv[1:] = args

        if input_data is not None:
            sys.stdin = io.StringIO(str(input_data) if not isinstance(input_data, str) else input_data)

        runpy.run_module(module_name, run_name="__main__", alter_sys=True)
    finally:
        sys.argv = original_argv
        sys.stdin = original_stdin


def execute_target(run_type: RunType, input_data: Any = None):
    if isinstance(run_type, Script):
        execute_script(run_type.script_path, run_type.args, input_data)
    elif isinstance(run_type, Module):
        execute_module(run_type.module_name, run_type.args, input_data)
    else:
        raise ValueError(f"Unknown target type: {run_type.__class__}")


class AIDebuggerJSONEncoder(json.JSONEncoder):
   def default(self, o: Any) -> Any:
        try:
            if isinstance(o, (datetime, date, time)):
                return o.isoformat()
            if isinstance(o, UUID):
                return str(o)
            if isinstance(o, (set, frozenset)):
                return list(o)
            if isinstance(o, (bytes, bytearray)):
                return "data:application/octet-stream;base64," + base64.b64encode(o).decode('utf-8')
            if isinstance(o, Path):
                return str(o)
            if isinstance(o, Enum):
                return o.name  # or o.value?
            if is_dataclass(o) and not isinstance(o, type):
                return asdict(o)
            try:
                import pydantic
                if isinstance(o, pydantic.BaseModel):
                    if hasattr(o, "model_dump"):
                        return o.model_dump()
                    elif hasattr(o, "dict"):
                       return o.dict()
            except Exception:
                pass
            if hasattr(o, "__dict__"):
                return o.__dict__
        except Exception:
            pass
        try:
            unable_to_serialize_str = f"Unable to serialize object of type {type(o)}"
        except Exception:
            unable_to_serialize_str = f"Unable to serialize object of unknown type"
        return unable_to_serialize_str

def clean_arguments(func, args, kwargs, kwargs_outer):
    sig = inspect.signature(func)
    parameters = list(sig.parameters.items())
    pos_params = [ (name, param) for name, param in parameters if param.kind in (param.POSITIONAL_ONLY, param.POSITIONAL_OR_KEYWORD) ]
    pos_names = [name for name, _ in pos_params]

    if pos_names and pos_names[0] == 'self':
        pos_names = pos_names[1:]

    cleaned_args = []
    pos_index = 0
    for arg in args:
        if pos_index < len(pos_names):
            name = pos_names[pos_index]
            if not (name == "input" and "input" in kwargs_outer):
                cleaned_args.append(arg)
            pos_index += 1
        else:
            cleaned_args.append(arg)

    cleaned_kwargs = {}
    for k, v in kwargs.items():
        if not (k == "input" and "input" in kwargs_outer):
            cleaned_kwargs[k] = v

    return tuple(cleaned_args), cleaned_kwargs

def wrap_method(original_method: callable, wrapper_method: callable, **kwargs_outer):
    def wrapper(self, *args, **kwargs):
        args, kwargs = clean_arguments(original_method, args, kwargs, kwargs_outer)
        return wrapper_method(self, original_method, *args, **{**kwargs, **kwargs_outer})

    return wrapper

def wrap_async_method(original_method: callable, wrapper_method: callable, **kwargs_outer):
    async def wrapper(self, *args, **kwargs):
        args, kwargs = clean_arguments(original_method, args, kwargs, kwargs_outer)
        return await wrapper_method(self, original_method, *args, **{**kwargs, **kwargs_outer})

    return wrapper

# import abc
import inspect

# ---------------------------------------------------------------------
# Assumes you already have:
#   def wrap_method(original_method, patch_fn, **kwargs): ...
#   def wrap_async_method(original_async_method, patch_fn, **kwargs): ...
# ---------------------------------------------------------------------

def _unwrap_descriptor(obj):
    """Return the underlying function if obj is classmethod/staticmethod."""
    if isinstance(obj, (classmethod, staticmethod)):
        return obj.__func__
    return obj
#
# def _descriptor_kind(obj):
#     if isinstance(obj, classmethod):
#         return "classmethod"
#     if isinstance(obj, staticmethod):
#         return "staticmethod"
#     return "function"
#
# def _is_abstract(obj) -> bool:
#     return bool(getattr(_unwrap_descriptor(obj), "__isabstractmethod__", False))
#
# def _owner_of_attr(cls, name):
#     """Find the class in the MRO that *defines* name in its __dict__."""
#     for c in cls.__mro__:
#         if name in c.__dict__:
#             return c
#     return None
#
# def _iter_all_subclasses(cls):
#     """Yield all (transitive) subclasses."""
#     seen = set()
#     stack = [cls]
#     while stack:
#         base = stack.pop()
#         for sub in base.__subclasses__():
#             if sub not in seen:
#                 seen.add(sub)
#                 yield sub
#                 stack.append(sub)
#
# def _update_abc(cls):
#     try:
#         abc.update_abstractmethods(cls)
#     except Exception:
#         pass
#
# def _ensure_concrete(func):
#     """Make sure the wrapper isn't marked abstract."""
#     try:
#         if getattr(func, "__isabstractmethod__", False):
#             func.__isabstractmethod__ = False
#     except Exception:
#         pass
#     return func
#
# def _reapply_descriptor(kind, func):
#     if kind == "classmethod":
#         return classmethod(func)
#     if kind == "staticmethod":
#         return staticmethod(func)
#     return func

def _patch_single_class(cls, method_name, method, **kwargs):
    """Patch method_name on target_cls only (no subclass/future logic)."""
    original_method_name = f"__patched__{method_name}"
    original_method = getattr(cls, original_method_name, None)

    if original_method:
        return
        # raise Exception(f"The method '{method_name}' of type '{cls.__name__}' has already been patched")

    original_method = getattr(cls, method_name, None)
    if not original_method:
        raise Exception(f"The method '{method_name}' of type '{cls.__name__}' does not exist")

    is_async = inspect.iscoroutinefunction(original_method)

    setattr(
        cls,
        method_name,
        wrap_async_method(original_method, method, **kwargs) if is_async else wrap_method(original_method, method, **kwargs)
    )

    setattr(cls, original_method_name, original_method)

#
# def patch_type(cls, method_name: str, patch_fn: callable, **kwargs):
#     """
#     Patch `method_name` starting from `cls`.
#
#     - If the attribute is concrete on the owner in the MRO, patch *that owner*.
#     - If it's abstract on `cls` (or its owner), patch all *existing* subclasses
#       that provide a concrete implementation. (No future subclass autopatching.)
#     """
#     original_owner = _owner_of_attr(cls, method_name)
#     if original_owner is None:
#         raise AttributeError(f"The method '{method_name}' of type '{cls.__name__}' does not exist")
#
#     original_obj = getattr(original_owner, method_name)
#     if _is_abstract(original_obj):
#         # Patch each existing subclass that concretely implements the method
#         print(f"all subclasses: {list(_iter_all_subclasses(cls))}")
#         for sub in _iter_all_subclasses(cls):
#             print("subclass", sub)
#             owner = _owner_of_attr(sub, method_name)
#             if owner is None:
#                 print("no owner :(")
#                 continue
#             print("owner", owner)
#             obj = getattr(owner, method_name, None)
#             print("obj", obj)
#             if obj is not None and not _is_abstract(obj):
#                 print("patching sub", sub, method_name, patch_fn, kwargs)  # TODO: for some reasong patching single works, but patching sub here does not, why?
#                 _patch_single_class(sub, method_name, patch_fn, **kwargs)
#                 print("patched!")
#
#         # Refresh ABC caches (harmless if nothing changed)
#         # _update_abc(cls)
#         # for sub in _iter_all_subclasses(cls):
#         #     _update_abc(sub)
#         #     print("updated ABC!")
#     else:
#         # Concrete: patch the class that actually defines it (may be a base)
#         print("patching single", original_owner, method_name, patch_fn, kwargs)
#         _patch_single_class(original_owner, method_name, patch_fn, **kwargs)
#         print("patched single!")
#
# def unpatch_type(cls, method_name: str):
#     """
#     Revert patches on `cls` and all its subclasses (if present).
#     """
#     marker = f"__patched__{method_name}"
#
#     def _revert(c):
#         if hasattr(c, marker):
#             original = getattr(c, marker)
#             setattr(c, method_name, original)
#             delattr(c, marker)
#             _update_abc(c)
#
#     _revert(cls)
#     for sub in _iter_all_subclasses(cls):
#         _revert(sub)



def parse_stack_trace(stack_trace: List[str]) -> List[dict]:
    import re

    def parse_line(line: str) -> Optional[dict]:
        match = re.search(TRACE_LINE_REGEX, line)
        if not match: return None

        file_path, line_number, function_name = match.groups()
        return {
            "filePath": file_path,
            "lineNumber": int(line_number),
            "functionName": function_name
        }

    def not_null(value: Any) -> bool:
        return value is not None

    return list(filter(not_null, map(parse_line, stack_trace)))