@echo off
echo === Native Library Test ===

REM Check if DLL exists
if exist "native\video-processing\lib\windows\x64\video_processing.dll" (
    echo [OK] DLL found at native\video-processing\lib\windows\x64\video_processing.dll
) else (
    echo [ERROR] DLL NOT found at native\video-processing\lib\windows\x64\video_processing.dll
    exit /b 1
)

REM Create Java test file
echo public class TestNativeLoad ^{
echo     public static void main(String[] args) ^{
echo         System.out.println("OS: " + System.getProperty("os.name"));
echo         System.out.println("Arch: " + System.getProperty("os.arch"));
echo         System.out.println("User dir: " + System.getProperty("user.dir"));
echo         System.out.println("Library path: " + System.getProperty("java.library.path"));
echo         try ^{
echo             System.loadLibrary("video_processing");
echo             System.out.println("SUCCESS: Native library loaded!");
echo         ^} catch (UnsatisfiedLinkError e) ^{
echo             System.err.println("FAILED: " + e.getMessage());
echo         ^}
echo     ^}
echo ^}
> TestNativeLoad.java

REM Compile and run
echo.
echo Compiling test class...
javac TestNativeLoad.java

echo.
echo Running test with library path...
java -Djava.library.path="native\video-processing\lib\windows\x64" TestNativeLoad

REM Cleanup
del TestNativeLoad.java
del TestNativeLoad.class

echo.
echo === Test Complete ===
