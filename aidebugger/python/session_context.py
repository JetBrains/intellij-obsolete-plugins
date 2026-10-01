import threading
from typing import Optional
from dataclasses import dataclass

from targets import Target

__all__ = ['SessionContext', 'set_session_context', 'get_session_context']

_lock = threading.Lock()
_session_context: Optional["SessionContext"] = None

@dataclass(frozen=True)
class SessionContext:
    """Context information for the current profiling session."""
    target: Target

def set_session_context(context: SessionContext) -> None:
    """Set the global session context in a thread-safe manner."""
    global _session_context
    with _lock:
        _session_context = context

def get_session_context() -> Optional[SessionContext]:
    """Get the global session context in a thread-safe manner.

    Returns:
        The current session context, or None if not set.
    """
    with _lock:
        return _session_context