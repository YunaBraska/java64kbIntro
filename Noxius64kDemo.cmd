@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "SCRIPT_SH=%SCRIPT_DIR%Noxius64kDemo.sh"
set "SHELL_EXE="

for %%I in (sh.exe) do set "FOUND_IN_PATH=%%~$PATH:I"
if defined FOUND_IN_PATH set "SHELL_EXE=%FOUND_IN_PATH%"
if not defined SHELL_EXE if exist "%ProgramFiles%\Git\bin\sh.exe" set "SHELL_EXE=%ProgramFiles%\Git\bin\sh.exe"
if not defined SHELL_EXE if exist "%ProgramFiles%\Git\usr\bin\sh.exe" set "SHELL_EXE=%ProgramFiles%\Git\usr\bin\sh.exe"
if not defined SHELL_EXE if exist "%ProgramFiles(x86)%\Git\bin\sh.exe" set "SHELL_EXE=%ProgramFiles(x86)%\Git\bin\sh.exe"
if not defined SHELL_EXE if exist "%ProgramFiles(x86)%\Git\usr\bin\sh.exe" set "SHELL_EXE=%ProgramFiles(x86)%\Git\usr\bin\sh.exe"

if not defined SHELL_EXE (
  echo [fail] No POSIX shell found. Install Git Bash, MSYS2, or Cygwin.
  echo [info] Then rerun Noxius64kDemo.cmd or call Noxius64kDemo.sh from that shell.
  exit /b 1
)

"%SHELL_EXE%" "%SCRIPT_SH%" %*
exit /b %ERRORLEVEL%
