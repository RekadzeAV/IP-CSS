# Test script to verify native library loading
Write-Host "=== Native Library Test ===" -ForegroundColor Cyan

# Check if DLL exists
$dllPath = "native/video-processing/lib/windows/x64/video_processing.dll"
Write-Host "Checking DLL at: $dllPath" -ForegroundColor Yellow
if (Test-Path $dllPath) {
    Write-Host "✓ DLL found at $dllPath" -ForegroundColor Green
} else {
    Write-Host "✗ DLL NOT found at $dllPath" -ForegroundColor Red
}

# Run Java with library path
Write-Host "`n=== Running Java Test ===" -ForegroundColor Cyan

$javaCode = @'
public class TestNativeLoad {
    public static void main(String[] args) {
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Arch: " + System.getProperty("os.arch"));
        System.out.println("User dir: " + System.getProperty("user.dir"));
        System.out.println("Library path: " + System.getProperty("java.library.path"));
        
        try {
            System.loadLibrary("video_processing");
            System.out.println("SUCCESS: Native library loaded!");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
'@

# Compile and run
Write-Host "Compiling test class..." -ForegroundColor Yellow
Set-Content -Path "TestNativeLoad.java" -Value $javaCode

javac TestNativeLoad.java 2>&1 | Out-Null

Write-Host "Running test..." -ForegroundColor Yellow
java -Djava.library.path="native/video-processing/lib/windows/x64" TestNativeLoad 2>&1 | Select-String -Pattern "SUCCESS|FAILED|OS:|Arch:"

# Cleanup
Remove-Item -Path "TestNativeLoad.java" -ErrorAction SilentlyContinue
Remove-Item -Path "TestNativeLoad.class" -ErrorAction SilentlyContinue

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
