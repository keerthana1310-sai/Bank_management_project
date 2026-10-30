@echo off
setlocal

echo ===================================================
echo   Apex Trust Bank - Management System Launcher
echo   Frontend Lead: Keerthana
echo ===================================================

:: Ensure JAVA_HOME points to Java 21 if available
if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    set "PATH=C:\Program Files\Java\jdk-21\bin;%PATH%"
)

:: Locate Maven executable
set "MVN_CMD=mvn"
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if exist "C:\Users\HP\apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_CMD=C:\Users\HP\apache-maven-3.9.6\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found in PATH or at C:\Users\HP\apache-maven-3.9.6.
        echo Please ensure Maven is installed.
        pause
        exit /b 1
    )
)

echo [INFO] Using Maven: %MVN_CMD%
echo [INFO] Compiling and launching JavaFX Application...
call "%MVN_CMD%" compile javafx:run

pause
