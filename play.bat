@echo off
set MAVEN_OPTS=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
if "%~1"=="" (
    mvn -q exec:exec
) else (
    setlocal enabledelayedexpansion
    set "URL=%*"
    set "URL=!URL:"=!"
    echo !URL!| mvn -q exec:exec
)
