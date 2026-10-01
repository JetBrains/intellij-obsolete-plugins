from collections import defaultdict
import contextvars
import copy
import uuid
from typing import List, Optional, Set, Dict, Union

from data import BasicTrace

# inspired by Weave LangChain Tracer

_call_stack: Dict[str, List[BasicTrace]] = defaultdict(list)
# _invoke_run_set: contextvars.ContextVar[Set[str]] = contextvars.ContextVar(
#     "aid_invoke_run_set", default=set()
# )
_trace_id: contextvars.ContextVar[Optional[str]] = contextvars.ContextVar(
    "aid_trace_id", default=None
)

def new_id() -> str:
    return str(uuid.uuid4())

def get_current_id() -> Optional[str]:
    current_member = get_current_call()
    if current_member is None:
        return None
    else:
        return current_member.id

def get_parent_id(current_id: Union[uuid.UUID, str], parent_id_candidate: Optional[Union[uuid.UUID, str]]) -> Optional[str]:
    if parent_id_candidate is not None:
        return str(parent_id_candidate)

    current_id_str = str(current_id)
    call_stack = get_call_stack()
    for call in call_stack[::-1]:
        if call.id == current_id_str:
            return call.parent_id

    return get_current_id()


def push_call(call: BasicTrace) -> None:
    _call_stack[get_trace_id()].append(call)
    # new_stack = copy.copy(_call_stack.get())
    # new_stack.append(call)
    # _call_stack.set(new_stack)

def get_trace_id() -> str:
    trace_id_ = _trace_id.get()
    if trace_id_ is None:
        trace_id_ = new_id()
        _trace_id.set(trace_id_)
    return trace_id_

def pop_call(call_id: Optional[str]) -> None:
    new_stack = _call_stack[get_trace_id()]
    # new_stack = copy.copy(_call_stack.get())
    if len(new_stack) == 0:
        # raise ValueError("Call stack is empty")
        return
    if call_id:
        # assert that the call_id is in the stack
        for i in range(len(new_stack)):
            target_index = -(i + 1)
            call = new_stack[target_index]
            if call.id == call_id:
                # Actually do the slice
                # Note (Tim): I think this logic is not quite correct. This will
                # effectively pop off all calls up to and including the target
                # call. I think this is actually an error case. Throwing an
                # error here would disallow out-of-sequence call finishing, but
                # i think that might be a good thing.
                # new_stack = new_stack[:target_index]
                del new_stack[target_index:]
                break
        else:
            # raise ValueError(f"Call with id {call_id} not found in stack")
            return
    else:
        new_stack.pop()
    if len(new_stack) == 0:
        _trace_id.set(None)
    # _call_stack.set(new_stack)


def get_current_call() -> Optional[BasicTrace]:
    return _call_stack[get_trace_id()][-1] if _call_stack[get_trace_id()] else None


def get_call_stack() -> List[BasicTrace]:
    return _call_stack[get_trace_id()]

# def record_invoke():
#     new_set = copy.copy(_invoke_run_set.get())
#     if "undefined" in new_set:
#         raise ValueError("There is already an undefined invoke in the set")
#     new_set.add("undefined")
#     _invoke_run_set.set(new_set)
#
# def is_invoke(run_id: str):
#     return run_id in _invoke_run_set.get()
#
# def remove_invoke(invoke_id: str):
#     new_set = copy.copy(_invoke_run_set.get())
#     new_set.remove(invoke_id)
#     _invoke_run_set.set(new_set)