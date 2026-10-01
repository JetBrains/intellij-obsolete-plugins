__all__ = [
    "AsyncTarget", "TraceType", "Trace", "RequirementsNotMetTrace",
    "InstantTrace", "SpanEnterTrace", "SpanExitTrace",
    "SpanEventType", "PayloadKey",
    "INIT_NODE_NAME", "LLM_CALL_NODE_NAME", "PROMPT_TEMPLATE_NAME",
    "LangChain", "LangGraph", "LangchainExpressionLangauge", "Framework"
]

import asyncio
from abc import ABC, abstractmethod
from enum import Enum
from typing import Dict, Any, Optional

INIT_NODE_NAME = "__init__"
LLM_CALL_NODE_NAME = "llm_call"
PROMPT_TEMPLATE_NAME = "PromptTemplate"


# noinspection SpellCheckingInspection
class TraceType(str, Enum):
    REQUIREMENTS_NOT_MET = "REQUIREMENTS_NOT_MET"
    SPAN_ENTER = "SPAN_ENTER"
    SPAN_EXIT = "SPAN_EXIT"
    INSTANT = "INSTANT"  # Starts and ends right after, so it's "instant"


class SpanEventType(str, Enum):
    General = "General"
    Group = "Group"
    Init = "Init"
    LlmCall = "LlmCall"
    ToolCall = "ToolCall"
    Exception = "Exception"


class PayloadKey(str, Enum):
    Inputs = "inputs"
    Outputs = "outputs"
    # Messages = "messages"
    Model = "model"
    StackTrace = "stack_trace"
    Graph = "graph"
    Exception = "exception"
    # Hidden = "hidden"


class Trace(ABC):
    @abstractmethod
    def to_dict(self) -> dict[str, Any]:
        pass


class RequirementsNotMetTrace(Trace):
    def to_dict(self):
        return {
            "type": TraceType.REQUIREMENTS_NOT_MET.value
        }


class Framework:
    value = "framework"

class Python(Framework):
    value = "python"

class LangChain(Python):
    value = "langchain"

class LangchainExpressionLangauge(LangChain):
    value = "langchain_expression_language"

class LangGraph(LangChain):
    value = "langgraph"


class BasicTrace(Trace):
    def __init__(
            self,
            id: str,
            parent_id: Optional[str],
            trace_id: str,
            name: str,
            event_type: SpanEventType,
            framework: Framework,
            timestamp_ms: int,
            payload: Dict[PayloadKey, Any]):
        self.id: str = id
        self.parent_id: Optional[str] = parent_id
        self.trace_id: str = trace_id
        self.name: str = name
        self.event_type: SpanEventType = event_type
        self.framework: Framework = framework
        self.timestamp_ms: int = timestamp_ms
        self.payload: Dict[PayloadKey, Any] = payload

    def to_dict(self):
        return {
            "id": self.id,
            "parent_id": self.parent_id,
            "trace_id": self.trace_id,
            "name": self.name,
            "event_type": self.event_type.value,
            "framework": self.framework.value,
            "timestamp_ms": self.timestamp_ms,
            "payload": self.payload,
        }


class SpanEnterTrace(BasicTrace):
    def __init__(
            self,
            id: str,
            parent_id: Optional[str],
            trace_id: str,
            name: str,
            event_type: SpanEventType,
            framework: Framework,
            timestamp_ms: int,
            payload: Dict[PayloadKey, Any]):

        super().__init__(
            id=id,
            parent_id=parent_id,
            trace_id=trace_id,
            name=name,
            event_type=event_type,
            framework=framework,
            timestamp_ms=timestamp_ms,
            payload=payload
        )


    def to_dict(self):
        return {
            "type": TraceType.SPAN_ENTER.value,
            **super().to_dict(),
        }


class SpanExitTrace(BasicTrace):
    def __init__(
            self,
            id: str,
            parent_id: Optional[str],
            trace_id: str,
            name: str,
            event_type: SpanEventType,
            framework: Framework,
            timestamp_ms: int,
            payload: Dict[PayloadKey, Any]):

        super().__init__(
            id=id,
            parent_id=parent_id,
            trace_id=trace_id,
            name=name,
            event_type=event_type,
            framework=framework,
            timestamp_ms=timestamp_ms,
            payload=payload
        )

    def to_dict(self):
        return {
            "type": TraceType.SPAN_EXIT.value,
            **super().to_dict(),
        }


class InstantTrace(BasicTrace):
    def __init__(
            self,
            id: str,
            parent_id: Optional[str],
            trace_id: str,
            name: str,
            event_type: SpanEventType,
            framework: Framework,
            timestamp_ms: int,
            payload: Dict[PayloadKey, Any]):

        super().__init__(
            id=id,
            parent_id=parent_id,
            trace_id=trace_id,
            name=name,
            event_type=event_type,
            framework=framework,
            timestamp_ms=timestamp_ms,
            payload=payload
        )


    def to_dict(self):
        return {
            "type": TraceType.INSTANT.value,
            **super().to_dict(),
        }


class AsyncTarget(ABC):
    @abstractmethod
    async def send(self, trace: Trace) -> None:
        pass

    @abstractmethod
    def run(self) -> asyncio.Task:
        pass

    @abstractmethod
    async def stop(self) -> None:
        pass

