@echo off
if not exist "%~dp0start-twstapecolor.cmd" (
  echo start-twstapecolor.cmd is missing. Keep both command scripts together.
  pause
  exit /b 1
)
rem The shared launcher performs Java checks, displays output and pauses.
call "%~dp0start-twstapecolor.cmd" stop %*
exit /b %errorlevel%
