@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem Keep the versioned JAR beside this script. No administrator rights are requested.
pushd "%~dp0" >nul 2>&1
if errorlevel 1 goto folder_error
set "TAPE_JAR=%~dp0twstapecolor-v0.3.24.jar"
if not exist "%TAPE_JAR%" goto missing_jar

rem Prefer JAVA_HOME; otherwise use the first Java on PATH.
set "TAPE_JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "TAPE_JAVA=%JAVA_HOME%\bin\java.exe"
if not defined TAPE_JAVA (
  for /f "delims=" %%J in ('where.exe java.exe 2^>nul') do if not defined TAPE_JAVA set "TAPE_JAVA=%%J"
)
if not defined TAPE_JAVA goto missing_jdk

rem A plain JRE is insufficient: the attach module is required.
"%TAPE_JAVA%" --list-modules 2>nul | findstr.exe /b /c:"jdk.attach@" >nul
if errorlevel 1 goto missing_jdk
rem Also ensure this Java can load the Java 21 application classes.
"%TAPE_JAVA%" --add-modules jdk.attach -jar "%TAPE_JAR%" --version >nul 2>&1
if errorlevel 1 goto java_error

echo twstapecolor v0.3.24
if "%~1"=="" (
  "%TAPE_JAVA%" --add-modules jdk.attach -jar "%TAPE_JAR%" start
) else (
  "%TAPE_JAVA%" --add-modules jdk.attach -jar "%TAPE_JAR%" %*
)
set "TAPE_EXIT=%errorlevel%"
echo.
if not "%TAPE_EXIT%"=="0" echo The command failed. Read the message above before closing this window.
pause
popd
exit /b %TAPE_EXIT%

:missing_jdk
echo A Windows JDK 21 or newer with the jdk.attach module was not found.
echo Install a 64-bit JDK 21 or newer, then set JAVA_HOME to its installation
echo folder or add its bin folder to PATH. Reopen this launcher afterward.
echo Download: https://learn.microsoft.com/java/openjdk/download
goto failure

:java_error
echo The selected Java could not run twstapecolor. A 64-bit JDK 21 or newer is required.
echo Java selected: "%TAPE_JAVA%"
echo Check JAVA_HOME and PATH. If the JDK is current, download the JAR again.
goto failure

:missing_jar
echo twstapecolor-v0.3.24.jar is missing.
echo Keep the JAR and both Windows command scripts together in the dist folder.
goto failure

:folder_error
echo Cannot open the launcher folder. Move dist to a writable local folder.
pause
exit /b 1

:failure
echo.
pause
popd
exit /b 1
