@echo off
for /f %%i in ('git.exe rev-parse --show-toplevel') do set "toplevel=%%~fi"
call "%toplevel%\build\protobuf\getprotoc.bat"
@echo on

protoc --java_out=lite:generated-source remotemessage.proto
