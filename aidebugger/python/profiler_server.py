from typing import Optional

from protocols import ProfilerClient

import socket
import threading
import queue


class ProfilerServerImpl(ProfilerClient):
    def __init__(self, port: int, host: str = "127.0.0.1"):
        self.port: int = port
        self.host: str = host

        self.thread: Optional[threading.Thread] = None
        self.ready_event = threading.Event()

        self.sendQueue: queue.Queue = queue.Queue()
        self.clientSocket: Optional[socket.socket] = None
        self.running = False

    def start(self):
        self.thread = threading.Thread(
            target=self.routine,
            daemon=True,
            name="profiler_server"
        )
        self.thread.start()

    def stop(self):
        self.running = False
        self.thread.join()

    def finish_sending_and_stop(self):
        self.sendQueue.put(None)
        self.thread.join()

    def wait_for_client(self) -> "ProfilerServerImpl":
        self.ready_event.wait()
        return self

    def send(self, message: str):
        self.sendQueue.put(message)

    def routine(self):
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_socket.bind((self.host, self.port))
        server_socket.listen(1)

        print(f"Server listening on {self.host}:{self.port}")

        client_socket, client_address = server_socket.accept()

        print(f"Client connected from {client_address}")

        self.clientSocket = client_socket
        self.running = True

        self.ready_event.set()

        while self.running:
            try:
                message = self.sendQueue.get(timeout=0.1)

                if message is None:
                    break

                self.clientSocket.sendall(message.encode())
            except queue.Empty:
                pass

        self.clientSocket.close()

        print("Client disconnected")