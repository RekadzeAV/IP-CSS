# SHA-256 certificate pin (sha256/base64) for a live TLS host (certificate pinning config).
# Usage: .\scripts\get-certificate-fingerprint.ps1 -Hostname api.example.com [-Port 443]

param(
    [string]$Hostname = "",
    [int]$Port = 443,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Connect with TLS, print sha256/<base64> pin and sample JSON snippet for certificate pins."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\get-certificate-fingerprint.ps1 -ShowHelp"
    Write-Host "  .\scripts\get-certificate-fingerprint.ps1 -Hostname api.example.com [-Port 443]"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Pin printed"
    Write-Host "  1  Missing hostname, no certificate, or connection error"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Hostname)) {
    Write-Host "Hostname is required. Use: .\scripts\get-certificate-fingerprint.ps1 -ShowHelp" -ForegroundColor Red
    exit 1
}

Write-Host "SHA-256 fingerprint for ${Hostname}:${Port}..." -ForegroundColor Cyan
Write-Host ""

try {
    # Создаем TCP соединение
    $tcpClient = New-Object System.Net.Sockets.TcpClient($Hostname, $Port)
    $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, {$true})

    try {
        $sslStream.AuthenticateAsClient($Hostname)
        $certificate = $sslStream.RemoteCertificate

        if ($certificate -is [System.Security.Cryptography.X509Certificates.X509Certificate2]) {
            $cert = [System.Security.Cryptography.X509Certificates.X509Certificate2]$certificate

            # Вычисляем SHA-256 fingerprint
            $hash = $cert.GetCertHashString("SHA256")
            $bytes = [System.Convert]::FromHexString($hash)
            $base64 = [System.Convert]::ToBase64String($bytes)
            $pin = "sha256/$base64"

            Write-Host "[OK] SHA-256 Fingerprint:" -ForegroundColor Green
            Write-Host $pin -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Добавьте в конфигурацию certificate pinning:" -ForegroundColor Cyan
            Write-Host "  `"$Hostname`": [" -ForegroundColor Gray
            Write-Host "    `"$pin`"" -ForegroundColor Gray
            Write-Host "  ]" -ForegroundColor Gray
            Write-Host ""

            Write-Host "Информация о сертификате:" -ForegroundColor Cyan
            Write-Host "  Subject: $($cert.Subject)" -ForegroundColor Gray
            Write-Host "  Issuer: $($cert.Issuer)" -ForegroundColor Gray
            Write-Host "  Valid From: $($cert.NotBefore)" -ForegroundColor Gray
            Write-Host "  Valid To: $($cert.NotAfter)" -ForegroundColor Gray
        } else {
            Write-Host "Ошибка: Не удалось получить сертификат" -ForegroundColor Red
            exit 1
        }
    } finally {
        $sslStream.Close()
        $tcpClient.Close()
    }
} catch {
    Write-Host "Ошибка: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Убедитесь, что:" -ForegroundColor Yellow
    Write-Host "  1. Хост доступен" -ForegroundColor Gray
    Write-Host "  2. Порт открыт" -ForegroundColor Gray
    Write-Host "  3. SSL/TLS сертификат валиден" -ForegroundColor Gray
    exit 1
}
