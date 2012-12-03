@if "%DEBUG%" == "" @echo off

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

:begin
@rem Determine what directory it is in.
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

if not defined STATICALIZER_HOME (
    set STATICALIZER_HOME=%DIRNAME%..
)
if not exist "%STATICALIZER_HOME%\lib\staticalizer-*.jar" (
    echo ERROR: Not found a valid STATICALIZER_HOME directory: "%STATICALIZER_HOME%" >&2
    goto end
)

set CLASSPATH=%STATICALIZER_HOME%\lib\staticalizer-@STATICALIZER_VERSION@.jar;%CLASSPATH%
groovy -e 'groovy.ui.GroovyMain2.main(args)' %*

:end

@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
%COMSPEC% /C exit /B %ERRORLEVEL%

