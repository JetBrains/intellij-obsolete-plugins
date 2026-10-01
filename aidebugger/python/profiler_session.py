import os
import threading
from typing import Optional, Any
from collections.abc import Iterable
from contextvars import ContextVar

from langchain_core.tracers.context import register_configure_hook
from langgraph.errors import GraphBubbleUp
from langgraph.pregel import Pregel
from langgraph.utils.config import ensure_config

from langgraph_events_collector import EventsCollectorHandler
from targets import Target
from utility import execute_target, RunType, _patch_single_class

tracing_aitoolkit_langchain_callback: ContextVar[Optional[EventsCollectorHandler]] = ContextVar(
    "tracing_aitoolkit_langchain_callback", default=None
)

def extract_config(args, kwargs):
    # config is either 2nd argument in args or an argument in kwargs
    # if it is provided in both, Python will raise an error (`TypeError: got multiple values for argument`),
    # so we ignore this case, as it will raise error anyway when original method is called
    args = list(args)
    if len(args) > 1:
        # taking config from args
        config = args[1]
        if config is None:
            config = {}
        config = ensure_config(config)  # pyright: ignore [reportArgumentType]
        args[1] = config
    elif "config" in kwargs:
        # taking config from kwargs
        config = kwargs["config"]
        if config is None:
            config = {}
        config = ensure_config(config)  # pyright: ignore [reportArgumentType]
        kwargs["config"] = config
    else:
        # no config provided, adding it to kwargs
        config = ensure_config()
        kwargs["config"] = config
    args = tuple(args)
    return config, args, kwargs

def pre(args, kwargs, self):
    config, args, kwargs = extract_config(args, kwargs)

    graph = self.get_graph().to_json()

    handler = EventsCollectorHandler(graph=graph)

    existing_callbacks = config.get("callbacks", [])

    # TODO: replace callback each pre, because graph is new DONE
    # TODO: put graph and stack_trace into callback __init__, so they are sent on first chain_enter DONE
    if isinstance(existing_callbacks, Iterable):
        old_callbacks = []
        for cb in existing_callbacks:
            if not isinstance(cb, EventsCollectorHandler):
                old_callbacks.append(cb)

        config["callbacks"] = [*old_callbacks, handler]
    else:
        config["callbacks"] = [handler]

    return args, kwargs


def fin(sender):
    sender.stop()


def err(e, sender):
    # print(f"In err! {e}")
    pass
    # tb_lines = traceback.format_exception(type(e), e, e.__traceback__)
    # frame_lines = [line for line in tb_lines if line.strip().startswith('File')]
    #
    # sender.send(EventTrace(
    #     id=get_next_id(),
    #     parent_id=invoke_id,
    #     name=EXCEPTION_NAME,
    #     event_type=SpanEventType.Exception,
    #     timestamp_ms=int(time.time() * 1000),
    #     payload={
    #         PayloadKey.Exception: str(e),
    #         PayloadKey.StackTrace: parse_stack_trace(frame_lines),
    #     },
    # ))


class TraceOnce:
    def __init__(self):
        self._lock = threading.Lock()
        self._traced = False

    def should_trace(self) -> bool:
        with self._lock:
            if self._traced:
                return False
            self._traced = True
            return True

def _apply_input_override(args, kwargs, replacement_input):
    if len(args) > 0:
        new_args = list(args)
        new_args[0] = replacement_input
        return tuple(new_args), kwargs
    elif "input" in kwargs:
        kwargs["input"] = replacement_input
        return args, kwargs
    else:
        # If not present, we assume it should be the first positional arg
        return (replacement_input,) + args, kwargs


def new_invoke(self, original_invoke: callable, *args, sender: Target, trace_once: Optional[TraceOnce] = None, replacement_input: Any = None, **kwargs):
    if trace_once and replacement_input is not None and trace_once.should_trace():
        args, kwargs = _apply_input_override(args, kwargs, replacement_input)

    args, kwargs = pre(args, kwargs, self)

    try:
        result = original_invoke(self, *args, **kwargs)
    except Exception as e:
        if isinstance(e, GraphBubbleUp):
            fin(sender)
        else:
            err(e, sender)
        raise

    fin(sender)

    return result


async def new_ainvoke(self, original_ainvoke: callable, *args, sender: Target, trace_once: Optional[TraceOnce] = None, replacement_input: Any = None, **kwargs):
    if trace_once and replacement_input is not None and trace_once.should_trace():
        args, kwargs = _apply_input_override(args, kwargs, replacement_input)

    args, kwargs = pre(args, kwargs, self)

    try:
        result = await original_ainvoke(self, *args, **kwargs)
    except Exception as e:
        if isinstance(e, GraphBubbleUp):
            fin(sender)
        else:
            err(e, sender)
        raise

    fin(sender)

    return result


def send_init_message(sender: Target, invoke_id: str, inputs: any):
    pass
    # sender.send(EventTrace(
    #     id=get_next_id(),
    #     parent_id=invoke_id,
    #     name=INIT_NODE_NAME,
    #     event_type=SpanEventType.General,
    #     timestamp_ms=int(time() * 1000),
    #     payload={
    #         PayloadKey.Outputs: inputs,
    #     }
    # ))

class ProfilerSession:
    def __init__(self, run_type: RunType, sender: Target, input: Optional[Any] = None):
        self.run_type = run_type
        self.sender = sender
        self.input = input
        self.trace_once = TraceOnce()

    def run(self):
        to_patch = {
            (Pregel, "invoke"): new_invoke,
            (Pregel, "ainvoke"): new_ainvoke,
            # (Chain, "invoke"): new_invoke,
            # (Chain, "ainvoke"): new_ainvoke,
            # (RunnableSerializable, "invoke"): new_invoke,
            # (RunnableSerializable, "ainvoke"): new_ainvoke,
            # (Runnable, "invoke"): new_invoke,
            # (Runnable, "ainvoke"): new_ainvoke,
            # (RunnableSequence, "invoke"): new_invoke,
            # (RunnableSequence, "ainvoke"): new_ainvoke,
            # (RunnableParallel, "invoke"): new_invoke,
            # (RunnableParallel, "ainvoke"): new_ainvoke,
        }

        for (cls, method_name), function in to_patch.items():
            patch_kwargs = {"sender": self.sender}

            if self.input is not None:
                patch_kwargs["replacement_input"] = self.input
                patch_kwargs["trace_once"] = self.trace_once

            _patch_single_class(cls, method_name, function, **patch_kwargs)

        self.original_trace_state = os.environ.get("AI_TOOLKIT_TRACE_LANGCHAIN")
        if self.original_trace_state is None:
            os.environ["AI_TOOLKIT_TRACE_LANGCHAIN"] = "true"
        else:
            os.environ["AI_TOOLKIT_TRACE_LANGCHAIN"] = self.original_trace_state

        register_configure_hook(
            tracing_aitoolkit_langchain_callback, True, EventsCollectorHandler, "AI_TOOLKIT_TRACE_LANGCHAIN"
        )

        # noinspection PyTypeChecker
        tracing_aitoolkit_langchain_callback.set(EventsCollectorHandler(graph=None))  # pyright: ignore [reportArgumentType]

        # TODO: (@gas) drop or keep if remote run tested successfully
        self.sender.run()

        execute_target(
            self.run_type,
            self.input,
        )

        tracing_aitoolkit_langchain_callback.set(None)