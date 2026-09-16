# NAS Field Validation Configuration
# Это файл конфигурации для полевых тестов NAS платформ

# ========================================
# НАСТРОЙКИ СЕТИ И УСТРОЙСТВ
# ========================================

# Synology NAS (реальное устройство)
$SynologyConfig = @{
    Host = "192.168.10.38"           # Synology RS2416+ в сети
    Username = "Andrey"
    Port = 22
    InstallPath = "/volume1/Git/IP-CSS-open"
    PackagePath = "build/ip-css-Alfa-0.1.1-synology-x86_64.spk"
    Architecture = "x86_64"
    DSMVersion = "7.2"
    SSHKeyPath = "credentials/ssh-keys/nas_andrey"
}

# QNAP NAS (если есть)
$QNAPConfig = @{
    Host = "192.168.1.101"
    Username = "admin"
    Port = 22
    InstallPath = "/share/External/IPCamera"
    PackagePath = "build/ip-css-Alfa-0.1.1-qnap-x86_64.qpkg"
    Architecture = "x86_64"
}

# Asustor NAS (если есть)
$AsustorConfig = @{
    Host = "192.168.1.102"
    Username = "admin"
    Port = 22
    InstallPath = "/Volume1/IPCamera"
    PackagePath = "build/ip-css-Alfa-0.1.1-asustor-x86_64.apk"
    Architecture = "x86_64"
}

# TrueNAS (если есть)
$TrueNASConfig = @{
    Host = "192.168.1.103"
    Username = "root"
    Port = 22
    InstallPath = "/mnt/pool/IPCamera"
    PackagePath = "build/truenas-Alfa-0.1.1/"
    Architecture = "x86_64"
}

# ========================================
# НАСТРОЙКИ ТЕСТОВЫХ КАМЕР
# ========================================

# Камеры для нагрузочного тестирования (реальные или эмулированные)
$TestCameras = @(
    @{
        Name = "Camera1-RTSP"
        IP = "192.168.10.50"
        Port = 554
        Protocol = "RTSP"
        Username = "admin"
        Password = "camera123"
        StreamPath = "/stream1"
        Codec = "H.264"
        Resolution = "1920x1080"
    },
    @{
        Name = "Camera2-ONVIF"
        IP = "192.168.10.51"
        Port = 80
        Protocol = "ONVIF"
        Username = "admin"
        Password = "camera123"
        StreamPath = "/media/stream1"
        Codec = "H.264"
        Resolution = "1920x1080"
    },
    @{
        Name = "Camera3-H265"
        IP = "192.168.10.52"
        Port = 554
        Protocol = "RTSP"
        Username = "admin"
        Password = "camera123"
        StreamPath = "/live/ch00"
        Codec = "H.265"
        Resolution = "3840x2160"
    },
    @{
        Name = "Camera4-Motion"
        IP = "192.168.10.53"
        Port = 554
        Protocol = "RTSP"
        Username = "admin"
        Password = "camera123"
        StreamPath = "/cam/realmonitor"
        Codec = "H.264"
        Resolution = "1280x720"
    }
)

# ========================================
# НАСТРОЙКИ МОНИТОРИНГА
# ========================================

$MonitoringConfig = @{
    # Интервал сбора метрик (секунды)
    MetricsInterval = 60
    
    # Длительность long-run теста (часы)
    LongRunDuration = 24
    
    # Пороги для предупреждений
    CpuWarningThreshold = 70        # %
    CpuCriticalThreshold = 85       # %
    MemoryWarningThreshold = 70     # %
    MemoryCriticalThreshold = 85    # %
    DiskWarningThreshold = 80       # %
    
    # Путь для логов мониторинга
    LogPath = "docs/reports/nas-field-validation/logs"
    
    # Включить расширенный мониторинг
    EnableExtendedMonitoring = $true
    
    # Собирать метрики сети
    CollectNetworkMetrics = $true
    
    # Собирать метрики записей
    CollectRecordingMetrics = $true
}

# ========================================
# НАСТРОЙКИ SSH КЛЮЧЕЙ (реальная конфигурация)
# ========================================

# Путь к SSH ключу для доступа к NAS
$SSHKeyPath = "credentials/ssh-keys/nas_andrey"

# Использовать парольную аутентификацию (если ключ не работает)
$UsePasswordAuth = $true
$SSHPassphrase = $env:NAS_SSH_PASSWORD  # пароль НЕ хранится в коде (утечка в git устранена 14.09)

# Для plink (если OpenSSH не работает)
$PlinkPath = "C:\Program Files\PuTTY\plink.exe"

# Параметры SSH подключения
$SSHParams = @{
    IdentityFile = $SSHKeyPath
    Password = $SSHPassphrase
    UsePlink = $true  # Использовать plink для совместимости
}

# ========================================
# НАСТРОЙКИ РЕЗЕРВНОГО КОПИРОВАНИЯ
# ========================================

$BackupConfig = @{
    # Создавать бэкап перед тестами
    CreateBackupBeforeTest = $true
    
    # Путь для бэкапов на локальной машине
    BackupLocalPath = "nas-backups"
    
    # Сохранять конфигурацию
    BackupConfig = $true
    
    # Сохранять данные записей (опционально, может быть большим)
    BackupRecordings = $false
    
    # Сжатие бэкапов
    CompressBackups = $true
}

# ========================================
# ЭКСПОРТ КОНФИГУРАЦИИ
# ========================================

# Экспортируем конфигурацию в переменные окружения
$env:NAS_FIELD_VALIDATION_CONFIG = "$PSScriptRoot\nas-field-validation-config.ps1"
$env:TEST_CAMERAS_JSON = ($TestCameras | ConvertTo-Json -Depth 5)
$env:MONITORING_CONFIG_JSON = ($MonitoringConfig | ConvertTo-Json -Depth 5)

Write-Host "NAS Field Validation Configuration loaded successfully!" -ForegroundColor Green
Write-Host "Synology: $($SynologyConfig.Host)" -ForegroundColor Cyan
Write-Host "Test Cameras: $($TestCameras.Count) cameras configured" -ForegroundColor Cyan
Write-Host "Monitoring: $($MonitoringConfig.LongRunDuration)h long-run test" -ForegroundColor Cyan
