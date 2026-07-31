#!/bin/sh
. "$(git rev-parse --show-toplevel)/build/protobuf/getprotoc.sh"

protoc --java_out=lite:generated-source remotemessage.proto
