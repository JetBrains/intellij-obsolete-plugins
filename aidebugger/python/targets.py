__all__ = ["Target", "CombinedTarget", "SaveToFileTarget", "DisplayToStdoutTarget", "SendToClientTarget"]

import asyncio
import json
from abc import abstractmethod, ABC
from typing import Optional, TextIO, List

from data import Trace
from protocols import ProfilerClient
from utility import AIDebuggerJSONEncoder


class Target(ABC):
    @abstractmethod
    def send(self, trace: Trace) -> None:
        pass

    @abstractmethod
    def run(self) -> None:
        pass

    @abstractmethod
    def stop(self) -> None:
        pass


class CombinedTarget(Target):
    def __init__(self, targets: List[Target]):
        self.targets: List[Target] = targets

    def stop(self) -> None:
        for target in self.targets:
            target.stop()

    def send(self, trace: Trace) -> None:
        for target in self.targets:
            target.send(trace)

    def run(self) -> None:
        for target in self.targets:
            target.run()


class SaveToFileTarget(Target):
    def __init__(self, file_path: str):
        self.file_path: str = file_path
        self.file: Optional[TextIO] = None

    def stop(self) -> None:
        self.file.close()

    def send(self, trace: Trace) -> None:
        if trace is None:
            return

        self.file.write(
            json.dumps(trace.to_dict(), cls=AIDebuggerJSONEncoder) + "\n"
        )

    def run(self) -> None:
        self.file = open(self.file_path, mode="w")


class DisplayToStdoutTarget(Target):
    def stop(self) -> None:
        pass

    def send(self, trace: Trace) -> None:
        print(f"{json.dumps(trace.to_dict(), cls=AIDebuggerJSONEncoder)}\n")

    def run(self) -> asyncio.Task:
        pass


class SendToClientTarget(Target):
    def __init__(self, client: ProfilerClient):
        self.client = client

    def stop(self) -> None:
        pass

    def send(self, trace: Trace) -> None:
        if trace is None:
            return

        self.client.send(f"{json.dumps(trace.to_dict(), cls=AIDebuggerJSONEncoder)}\n")

    def run(self) -> asyncio.Task:
        pass