import argparse
import ast
from typing import Optional, Tuple, Any
import json

from session_context import set_session_context, SessionContext
from utility import is_package_installed, Script, Module, execute_target
from profiler_server import ProfilerServerImpl
from profiler_client import ProfilerClientImpl
from targets import (
    CombinedTarget,
    DisplayToStdoutTarget,
    SaveToFileTarget,
    SendToClientTarget
)
from data import RequirementsNotMetTrace

required_packages = [
    "langgraph"
]

def are_requirements_met() -> bool:
    for package in required_packages:
        if not is_package_installed(package):
            return False
        
    return True

def parse_host_and_port(host_and_port: str) -> Tuple[str, int]:
    if ":" in host_and_port:
        (host, port) = host_and_port.split(":")
    else:
        host = "127.0.0.1"
        port = host_and_port

    return host, int(port)

def safe_eval(input: str) -> Any:
    s = input.strip()

    try:
        return ast.literal_eval(s)
    except Exception:
        pass

    if (s.startswith("'") and s.endswith("'")) or (s.startswith('"') and s.endswith('"')):
        try:
            unquoted = s[1:-1]
            unquoted = unquoted.replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n")
            return json.loads(unquoted)
        except Exception:
            pass
    try:
        return json.loads(s)
    except Exception:
        pass

    return s

def parse_command_line():
    arg_parser = argparse.ArgumentParser(description="AI Profiler")

    arg_parser.add_argument(
        "--stdout",
        action="store_true",
        help="Output events to stdout",
        required=False
    )
    arg_parser.add_argument(
        "--file",
        type=str,
        metavar="FILE_PATH",
        help="Output events to file",
        required=False
    )
    arg_parser.add_argument(
        "--server",
        type=str,
        metavar="[HOST:]PORT",
        help="Start the profiler server",
        required=False
    )
    arg_parser.add_argument(
        "--client",
        type=str,
        metavar="[HOST:]PORT",
        help="Connect to the profiler server",
        required=False
    )
    arg_parser.add_argument(
        "--test-server",
        type=str,
        metavar="[HOST:]PORT",
        help="Start the profiler server",
        required=False
    )
    arg_parser.add_argument(
        "--input",
        type=str,
        help="String input to invoke an agent",
        required=False,
    )

    script_or_module_group = arg_parser.add_mutually_exclusive_group(required=True)

    script_or_module_group.add_argument(
        "--script",
        type=str,
        help="Script to run",
        required=False,
    )
    script_or_module_group.add_argument(
        "--module",
        type=str,
        help="Module to run",
        required=False,
    )

    return arg_parser.parse_known_args()

def main():
    args, original_args = parse_command_line()
    if original_args[0] == "--":
        original_args = original_args[1:]

    senders = []
    server: Optional[ProfilerServerImpl] = None
    client: Optional[ProfilerClientImpl] = None

    if args.script and args.module:
        raise ValueError("Exactly one of --script and --module should be provided, received both")
    elif not args.script and not args.module:
        raise ValueError("Exactly one of --script and --module should be provided, received neither")
    elif args.script:
        run_type = Script(args.script, original_args)
    elif args.module:
        run_type = Module(args.module, original_args)
    else:
        raise ValueError("Should not be here")

    if args.stdout:
        senders.append(DisplayToStdoutTarget())

    if args.file:
        senders.append(SaveToFileTarget(args.file))

    if args.server:
        (server_host, server_port) = parse_host_and_port(args.server)

        server = ProfilerServerImpl(int(server_port), server_host)
        server.start()
        client_from_server = server.wait_for_client()

        senders.append(SendToClientTarget(client_from_server))

    if args.client:
        (server_host, server_port) = parse_host_and_port(args.client)

        client = ProfilerClientImpl(int(server_port), server_host)
        client.start()
        client.wait_for_connection()

        senders.append(SendToClientTarget(client))

    if args.test_server:
        (server_host, server_port) = parse_host_and_port(args.test_server)

        server = ProfilerServerImpl(int(server_port), server_host)
        server.start()
        # server.join()
        return

    input = None
    if args.input:
        input = safe_eval(args.input)

    sender = CombinedTarget(senders)

    set_session_context(
        SessionContext(sender)
    )

    exception = None

    try:
        if are_requirements_met():
            from profiler_session import ProfilerSession
            session = ProfilerSession(run_type, sender, input)
            session.run()
        else:
            sender.send(RequirementsNotMetTrace())
            execute_target(run_type)
    except Exception as e:
        exception = e

    if server:
        server.finish_sending_and_stop()

    if client:
        client.finish_sending_and_stop()

    if exception is not None:
        raise exception

if __name__ == "__main__":
    main()
