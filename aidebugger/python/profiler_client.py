from typing import Optional

from protocols import ProfilerClient

import socket
import sys
import threading
import queue
import time


class ProfilerClientImpl(ProfilerClient):
    def __init__(self, port: int, host: str = "127.0.0.1"):
        self.port: int = port
        self.host: str = host

        self.thread: Optional[threading.Thread] = None
        self.ready_event = threading.Event()

        self.sendQueue: queue.Queue = queue.Queue()
        self.serverSocket: Optional[socket.socket] = None
        self.connected = False

    def start(self):
        self.thread = threading.Thread(
            target=self.routine,
            daemon=True,
            name="profiler_client"
        )
        self.thread.start()

    def stop(self):
        self.connected = False
        if self.thread is not None:
            self.thread.join()

    def finish_sending_and_stop(self):
        self.sendQueue.put(None)
        if self.thread is not None:
            self.thread.join()

    def wait_for_connection(self) -> "ProfilerClient":
        self.ready_event.wait()
        return self

    def send(self, message: str):
        if not self.connected:
            return
        self.sendQueue.put(message)

    def routine(self):
        client_socket = None
        timeout = 10.0  # seconds
        delay = 0.05  # start with 50ms
        max_delay = 1.0
        elapsed = 0.0

        while elapsed < timeout:
            try:
                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect((self.host, self.port))
                break
            except Exception as e:
                try:
                    client_socket.close()
                except Exception:
                    pass
                client_socket = None
                if elapsed + delay >= timeout:
                    print(f"ProfilerClient: failed to connect to {self.host}:{self.port} after {elapsed:.1f}s: {e}",
                          file=sys.stderr)
                    self.ready_event.set()
                    return
                time.sleep(delay)
                elapsed += delay
                delay = min(delay * 2, max_delay)

        print(f"Connected to server {self.host}:{self.port}")

        self.serverSocket = client_socket
        self.connected = True

        self.ready_event.set()

        while self.connected:
            try:
                message = self.sendQueue.get(timeout=0.1)

                if message is None:
                    break

                # ensure newline-terminated to be consistent with async server sample
                if not message.endswith("\n"):
                    message += "\n"

                self.serverSocket.sendall(message.encode())
            except queue.Empty:
                pass
            except OSError:
                # socket might be closed by server
                break

        try:
            self.serverSocket.shutdown(socket.SHUT_RDWR)
        except Exception:
            pass
        self.serverSocket.close()

        print("Disconnected from server")
