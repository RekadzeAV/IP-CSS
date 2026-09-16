Write-Host "=== Native Library Test ===" -ForegroundColor Cyan

$dllPath = "native/video-processing/lib/windows/x64/video_processing.dll"

if (Test-Path $dllPath) {
    Write-Host "[OK] DLL found" -ForegroundColor Green
} else {
    Write-Host "[ERROR] DLL NOT found" -ForegroundColor Red
    exit 1
}

$javaCode = @"
public class TestNativeLoad {
    public static void main(String[] args) {
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Library path: " + System.getProperty("java.library.path"));
        try {
            System.loadLibrary("video_processing");
            System.out.println("SUCCESS: Native library loaded!");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("FAILED: " + e.getMessage());
        }
    }
}
"@

Set-Content -Path "TestNativeLoad.java" -Value $javaCode -Encoding ASCII

Write-Host "Compiling..." -ForegroundColor Yellow
javac TestNativeLoad.java 2>&1 | Out-Null

Write-Host "Running..." -ForegroundColor Yellow
java -Djava.library.path="native/video-processing/lib/windows/x64" TestNativeLoad 2>&1

Remove-Item "TestNativeLoad.java" -ErrorAction SilentlyContinue
Remove-Item "TestNativeLoad.class" -ErrorAction SilentlyContinue
