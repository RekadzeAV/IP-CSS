@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
:: Build All Variants with Detailed Logging
:: ============================================

set "LOG_DIR=build-logs"
set "TIMESTAMP=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "TIMESTAMP=%TIMESTAMP: =0%"
set "LOG_FILE=%LOG_DIR%\build-all_%TIMESTAMP%.log"
set "SUMMARY_FILE=%LOG_DIR%\build-summary_%TIMESTAMP%.txt"

:: Create log directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo =========================================== > "%LOG_FILE%"
echo IP-CSS Build All Variants >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"
echo Host: %COMPUTERNAME% >> "%LOG_FILE%"
echo User: %USERNAME% >> "%LOG_FILE%"
echo =========================================== >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

:: Initialize counters
set "TOTAL_BUILDS=0"
set "PASSED_BUILDS=0"
set "FAILED_BUILDS=0"
set "SKIPPED_BUILDS=0"

:: Function to log build result
:LOG_RESULT
set "BUILD_NAME=%~1"
set "BUILD_STATUS=%~2"
set "BUILD_TIME=%~3"

echo [%BUILD_STATUS%] %BUILD_NAME% - %BUILD_TIME% >> "%SUMMARY_FILE%"
echo %BUILD_STATUS% : %BUILD_NAME% 

if "%BUILD_STATUS%"=="PASS" (
    set /a PASSED_BUILDS+=1
) else if "%BUILD_STATUS%"=="FAIL" (
    set /a FAILED_BUILDS+=1
) else if "%BUILD_STATUS%"=="SKIP" (
    set /a SKIPPED_BUILDS+=1
)
goto :EOF

:: ============================================
:: 1. Clean Build
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [1/12] CLEAN BUILD >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"
set "START_TIME=%time%"

call :LOG_RESULT "Clean Build" "RUNNING" "0s"

call .\gradlew.bat clean --no-daemon --console=plain >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Clean Build" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Clean Build" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 2. Core Common - Desktop (JVM)
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [2/12] CORE COMMON - DESKTOP (JVM) >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"
set "START_TIME=%time%"

call :LOG_RESULT "Core Common Desktop" "RUNNING" "0s"

call .\gradlew.bat :core:common:compileKotlinDesktop --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Common Desktop" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Common Desktop" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 3. Core Network - Desktop (JVM)
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [3/12] CORE NETWORK - DESKTOP (JVM) >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Core Network Desktop" "RUNNING" "0s"

call .\gradlew.bat :core:network:compileKotlinDesktop --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Network Desktop" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Network Desktop" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 4. Shared - Desktop (JVM)
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [4/12] SHARED - DESKTOP (JVM) >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Shared Desktop" "RUNNING" "0s"

call .\gradlew.bat :shared:compileKotlinDesktop --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Shared Desktop" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Shared Desktop" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 5. Server API - JVM
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [5/12] SERVER API - JVM >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Server API" "RUNNING" "0s"

call .\gradlew.bat :server:api:compileKotlin --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Server API" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Server API" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 6. Core Common - Metadata
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [6/12] CORE COMMON - METADATA >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Core Common Metadata" "RUNNING" "0s"

call .\gradlew.bat :core:common:compileKotlinMetadata --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Common Metadata" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Common Metadata" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 7. Core Network - Metadata
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [7/12] CORE NETWORK - METADATA >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Core Network Metadata" "RUNNING" "0s"

call .\gradlew.bat :core:network:compileKotlinMetadata --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Network Metadata" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Network Metadata" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 8. Shared - Metadata
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [8/12] SHARED - METADATA >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Shared Metadata" "RUNNING" "0s"

call .\gradlew.bat :shared:compileKotlinMetadata --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Shared Metadata" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Shared Metadata" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 9. Core Common - Tests
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [9/12] CORE COMMON - TESTS >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Core Common Tests" "RUNNING" "0s"

call .\gradlew.bat :core:common:test --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Common Tests" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Common Tests" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 10. Core Network - Tests
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [10/12] CORE NETWORK - TESTS >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Core Network Tests" "RUNNING" "0s"

call .\gradlew.bat :core:network:test --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Core Network Tests" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Core Network Tests" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 11. Shared - Tests
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [11/12] SHARED - TESTS >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Shared Tests" "RUNNING" "0s"

call .\gradlew.bat :shared:test --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Shared Tests" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Shared Tests" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: 12. Server API - Tests
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo [12/12] SERVER API - TESTS >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Started: %date% %time% >> "%LOG_FILE%"

call :LOG_RESULT "Server API Tests" "RUNNING" "0s"

call .\gradlew.bat :server:api:test --no-daemon --console=plain --info >> "%LOG_FILE%" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

set "END_TIME=%time%"
if %GRADLE_EXIT% EQU 0 (
    call :LOG_RESULT "Server API Tests" "PASS" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
) else (
    call :LOG_RESULT "Server API Tests" "FAIL" "%END_TIME%"
    set /a TOTAL_BUILDS+=1
)

:: ============================================
:: Generate Summary
:: ============================================
echo. >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo BUILD SUMMARY >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"
echo Completed: %date% %time% >> "%LOG_FILE%"
echo Total Builds: %TOTAL_BUILDS% >> "%LOG_FILE%"
echo Passed: %PASSED_BUILDS% >> "%LOG_FILE%"
echo Failed: %FAILED_BUILDS% >> "%LOG_FILE%"
echo Skipped: %SKIPPED_BUILDS% >> "%LOG_FILE%"
echo Success Rate: >> "%LOG_FILE%"

set /a "SUCCESS_RATE=!PASSED_BUILDS! * 100 / !TOTAL_BUILDS!"
echo !SUCCESS_RATE!%% >> "%LOG_FILE%"
echo ========================================== >> "%LOG_FILE%"

echo. > "%SUMMARY_FILE%"
echo ========================================== >> "%SUMMARY_FILE%"
echo IP-CSS Build Summary >> "%SUMMARY_FILE%"
echo Date: %date% %time% >> "%SUMMARY_FILE%"
echo ========================================== >> "%SUMMARY_FILE%"
echo. >> "%SUMMARY_FILE%"
echo Total Builds: %TOTAL_BUILDS% >> "%SUMMARY_FILE%"
echo Passed: %PASSED_BUILDS% >> "%SUMMARY_FILE%"
echo Failed: %FAILED_BUILDS% >> "%SUMMARY_FILE%"
echo Skipped: %SKIPPED_BUILDS% >> "%SUMMARY_FILE%"
echo Success Rate: !SUCCESS_RATE!%% >> "%SUMMARY_FILE%"
echo. >> "%SUMMARY_FILE%"
echo Detailed Log: %LOG_FILE% >> "%SUMMARY_FILE%"
echo ========================================== >> "%SUMMARY_FILE%"

echo. >> "%LOG_FILE%"
echo Build process completed! >> "%LOG_FILE%"
echo Log file: %LOG_FILE% >> "%LOG_FILE%"
echo Summary: %SUMMARY_FILE% >> "%LOG_FILE%"

:: Display summary
echo.
echo ==========================================
echo BUILD COMPLETED
echo ==========================================
echo Total: %TOTAL_BUILDS%
echo Passed: %PASSED_BUILDS%
echo Failed: %FAILED_BUILDS%
echo Skipped: %SKIPPED_BUILDS%
echo Success Rate: !SUCCESS_RATE!%%
echo ==========================================
echo.
echo Log file: %LOG_FILE%
echo Summary: %SUMMARY_FILE%
echo.

endlocal
pause