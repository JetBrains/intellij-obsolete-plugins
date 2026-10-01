import copy
import time
import traceback
from typing import Dict, Any, List, Optional

from langchain_core.tracers import BaseTracer
from langchain_core.tracers.schemas import Run

import hierarchy
from data import (
    INIT_NODE_NAME, LLM_CALL_NODE_NAME, PROMPT_TEMPLATE_NAME,
    SpanEnterTrace, SpanExitTrace, SpanEventType, PayloadKey, InstantTrace,
    Framework, LangChain, LangGraph,
)
from session_context import get_session_context
from utility import parse_stack_trace


class EventsCollectorHandler(BaseTracer):
    # surprisingly, this is a must-have for a contextvar-based call stack to work!
    # otherwise it gets launched in a new asyncio Task and the Context with ContextVar-s is not preserved
    run_inline: bool = True

    def _persist_run(self, run: Run) -> None:
        self.latest_run = copy.copy(run)

    def __init__(self, graph: Optional[str] = None, **kwargs: Any):  # pyright: ignore [reportArgumentType]
        super().__init__(**kwargs)

        context = get_session_context()

        self.started_nodes = dict()
        self.target = context.target
        self.graph = graph
        self.latest_run: Optional[Run] = None
        self.start_span_id : Optional[str] = None
        self.spans_to_ignore: set[str] = set()

    def _start_trace(self, run: Run) -> None:
        super()._start_trace(run)

        if self.start_span_id is not None or run.run_type == "chain":
            return

        span_id = hierarchy.new_id()
        self.start_span_id = span_id

        trace = SpanEnterTrace(
            id=span_id,
            parent_id=None,
            trace_id=hierarchy.get_trace_id(),
            name="LangChain",
            event_type=SpanEventType.Group,
            framework=LangChain,
            timestamp_ms=int(time.time() * 1000),
            payload={
                PayloadKey.StackTrace: parse_stack_trace(traceback.format_stack()),
            },
        )
        hierarchy.push_call(trace)
        self.target.send(trace)

    def _on_llm_start(self, run: Run):
        run_id = run.id
        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)

        if self._should_ignore(run):
            self.spans_to_ignore.add(str(run_id))
            return

        invocation_params = run.extra.get("invocation_params")

        payload: dict[PayloadKey, Any] = {
            PayloadKey.Inputs: run.inputs,
        }

        if invocation_params is None:
            pass
        else:
            model = {
                "model": invocation_params.get("model"),
                "modelName": invocation_params.get("model_name"),
                "temperature": invocation_params.get("temperature"),
                "tools": parse_tools(invocation_params.get("tools")) if "tools" in invocation_params else None,
            }
            payload[PayloadKey.Model] = model

        trace = SpanEnterTrace(
            id=str(run_id),
            parent_id=parent_run_id,
            trace_id=hierarchy.get_trace_id(),
            name=LLM_CALL_NODE_NAME,
            event_type=SpanEventType.LlmCall,
            framework=get_framework(run),
            timestamp_ms=int(time.time() * 1000),
            payload=payload,
        )
        hierarchy.push_call(trace)
        self.target.send(trace)

    def _on_chat_model_start(self, run: Run):
        run_id = run.id
        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)

        if self._should_ignore(run):
            self.spans_to_ignore.add(str(run_id))
            return

        invocation_params = run.extra.get("invocation_params")

        payload: dict[PayloadKey, Any] = {
            PayloadKey.Inputs: run.inputs,
        }

        if invocation_params is None:
            pass
        else:
            model = {
                "model": invocation_params.get("model"),
                "modelName": invocation_params.get("model_name"),
                "temperature": invocation_params.get("temperature"),
                "tools": parse_tools(invocation_params.get("tools")) if "tools" in invocation_params else None,
            }
            payload[PayloadKey.Model] = model

        # if parent_run_id not in self.known_ids:
        #     raise Exception("EventsCollectorHandler: Unknown parent run id")

        # self.known_ids.add(run_id)

        trace = SpanEnterTrace(
            id=str(run_id),
            parent_id=parent_run_id,
            trace_id=hierarchy.get_trace_id(),
            name=LLM_CALL_NODE_NAME,
            event_type=SpanEventType.LlmCall,
            framework=get_framework(run),
            timestamp_ms=int(time.time() * 1000),
            payload=payload,
        )
        hierarchy.push_call(trace)
        self.target.send(trace)

    def _on_llm_end(self, run: Run):
        run_id = run.id
        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)

        if self._should_ignore(run):
            self.spans_to_ignore.add(str(run_id))
            return

        payload: dict[PayloadKey, Any] = {
            PayloadKey.Outputs: run.outputs,
        }

        trace = SpanExitTrace(
            id=str(run_id),
            parent_id=parent_run_id ,
            trace_id=hierarchy.get_trace_id(),
            name=LLM_CALL_NODE_NAME,
            event_type=SpanEventType.LlmCall,
            framework=get_framework(run),
            timestamp_ms=int(time.time() * 1000),
            payload=payload,
        )
        hierarchy.pop_call(trace.id)
        self.target.send(trace)

    def _on_chain_start(self, run: Run):
        name = str(run.name)
        run_id = run.id

        if self._should_ignore(run):
            self.spans_to_ignore.add(str(run_id))
            return

        if self.start_span_id is None:
            is_first_chain = True
            self.start_span_id = str(run_id)
        else:
            is_first_chain = False

        # We use parent_run_id if it exists, or use a current element id from hierarchy, which becomes the parent of the new chain.
        # Sometimes with subgraphs (or subgraphs-as-tools) the parent_run_id is None, so we have to ensure it by having own hierarchy.
        # Also, if one uses the mix of 2+ frameworks (e.g. LangGraph and PydanticAI), we need to keep the consistent hierarchy of spans
        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)
        metadata = run.metadata

        payload: Dict[PayloadKey, Any] = {
            PayloadKey.Inputs: run.inputs,
        }
        event_type = SpanEventType.General

        if name == INIT_NODE_NAME:
            # __init__ node triggered this callback in older versions of LangGraph, but it does not in the newer
            # But we use it as a node to show initial inputs, so we always show it.
            # We use the data from the top-level chain ("LangGraph")
            return

        # self.known_ids.add(run_id)
        self.started_nodes[run_id] = name

        # if parent_run_id not in self.known_ids:
        #     raise Exception("EventsCollectorHandler: Unknown parent run id")

        if is_first_chain:
            # we are the top-level chain ("LangGraph")
            self.start_span_id = str(run_id)
            payload[PayloadKey.Graph] = self.graph
            payload[PayloadKey.StackTrace] = parse_stack_trace(traceback.format_stack())
            event_type = SpanEventType.Group

        trace = SpanEnterTrace(
            id = str(run_id),
            parent_id = parent_run_id,
            trace_id=hierarchy.get_trace_id(),
            name = name,
            event_type = event_type,
            framework=get_framework(run),
            timestamp_ms=int(time.time() * 1000),
            payload = payload
        )

        hierarchy.push_call(trace)
        self.target.send(trace)

        if is_first_chain:
            # TODO: process on Kotlin side
            # we are the first span, so we also need to send __init__. To simplify, we send it as INSTANT event
            # It was sent by langgraph itself in old versions, but in newer it is not, so we make sure it is sent
            payload: Dict[PayloadKey, Any] = {
                PayloadKey.Outputs: run.inputs,
            }

            trace = InstantTrace(
                id = hierarchy.new_id(),
                parent_id = str(run_id),
                trace_id=hierarchy.get_trace_id(),
                name = INIT_NODE_NAME,
                event_type = SpanEventType.General, # TODO:
                framework=get_framework(run),
                timestamp_ms=int(time.time() * 1000),
                payload = payload
            )
            self.target.send(trace)



    def _on_chain_end(self, run: Run):
        run_id = run.id

        if self._should_ignore(run):
            self.spans_to_ignore.add(str(run_id))
            return

        if self.start_span_id == str(run_id):
            is_first_chain = True
        else:
            is_first_chain = False

        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)
        name: Optional[str] = self.started_nodes.pop(run_id, None)

        if name is None:
            return

        if name == INIT_NODE_NAME:
            return

        if is_first_chain:
            event_type = SpanEventType.Group
        else:
            event_type = SpanEventType.General

        trace = SpanExitTrace(
            id = str(run_id),
            parent_id = parent_run_id,
            trace_id=hierarchy.get_trace_id(),
            name = name,
            event_type = event_type,
            framework=get_framework(run),
            timestamp_ms = int(time.time() * 1000),
            payload = {
                PayloadKey.Outputs: run.outputs,
            },
        )

        hierarchy.pop_call(trace.id)
        self.target.send(trace)

    def _should_ignore(self, run: Run) -> bool:
        run_id = run.id
        name = str(run.name)
        parent_run_id = hierarchy.get_parent_id(run_id, run.parent_run_id)

        if parent_run_id in self.spans_to_ignore:
            return True

        if name == PROMPT_TEMPLATE_NAME and parent_run_id is None:
            return True

        return False

def get_framework(run: Run) -> Framework:
    if run.metadata is None: return LangChain

    langgraph_node = run.metadata.get("langgraph_node")

    return LangGraph if langgraph_node is not None else LangChain

def parse_tools(tools: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    def not_none(value: Any) -> bool:
        return value is not None

    def parse_tool(tool: Dict[str, Any]) -> Dict[str, Any] | None:
        tool_type = tool.get("type")

        if tool_type == "function":
            function = tool.get("function")
            if function is None: return None

            return {
                "name": function.get("name"),
                "description": function.get("description"),
            }

        return None

    return list(filter(not_none, map(parse_tool, tools)))  # pyright: ignore [reportReturnType]

def find_messages(output: Dict[str, Any]) -> Optional[List[Any]]:
    for key, value in output.items():
        if key.lower() == "messages" and isinstance(value, list):
            return value

    return None


def get_last_message(data: Any) -> Any:
    if isinstance(data, dict):
        messages = find_messages(data)
        if messages:
            return messages[-1] if messages else None
    elif isinstance(data, list):
        # Try to find a message in any item in the list
        for item in data:
            result = get_last_message(item)
            if result is not None:
                return result

    return None

