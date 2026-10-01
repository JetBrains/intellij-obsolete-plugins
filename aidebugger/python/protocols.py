from typing import runtime_checkable, Protocol


@runtime_checkable
class ProfilerClient(Protocol):
    def send(self, message: str) -> None: ...