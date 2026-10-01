#!/bin/sh

# TODO: Run ai_toolkit/ai_profiler.py instead of ls
ls ai_toolkit/

java -jar evaluation-cli.jar "$@"