package com.company.ipcamera.shared.platform

import java.io.File

/**
 * JVM реализация NasPlatformDetector
 * Определяет платформу через системные файлы и переменные окружения
 */
actual class NasPlatformDetectorImpl actual constructor() : NasPlatformDetector {
    actual override fun detectPlatform(): NasPlatform? {
        // Проверка Synology DSM
        if (isSynology()) {
            return NasPlatform.SYNOLOGY_DSM
        }

        // Проверка QNAP QTS
        if (isQnap()) {
            return NasPlatform.QNAP_QTS
        }

        // Проверка Asustor ADM
        if (isAsustor()) {
            return NasPlatform.ASUSTOR_ADM
        }

        // Проверка TrueNAS CORE (FreeBSD)
        if (isTrueNasCore()) {
            return NasPlatform.TRUENAS_CORE
        }

        // Проверка TrueNAS SCALE (Linux + Kubernetes)
        if (isTrueNasScale()) {
            return NasPlatform.TRUENAS_SCALE
        }

        return null
    }

    actual override fun isRunningOnNas(): Boolean = detectPlatform() != null

    actual override suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType {
        return com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationDetector.detectAvailableAcceleration()
    }

    actual override fun getSystemPaths(): SystemPaths {
        val platform = detectPlatform() ?: return SystemPaths.default()

        return when (platform) {
            NasPlatform.SYNOLOGY_DSM -> {
                val volumePath = getSynologyVolumePath()
                SystemPaths(
                    dataPath = "$volumePath/ip-css/data",
                    recordingsPath = "$volumePath/ip-css/recordings",
                    logsPath = "/var/log/ip-css",
                    configPath = "$volumePath/ip-css/config",
                )
            }

            NasPlatform.QNAP_QTS -> {
                val sharePath = getQnapSharePath()
                SystemPaths(
                    dataPath = "$sharePath/ip-css/data",
                    recordingsPath = "$sharePath/ip-css/recordings",
                    logsPath = "/var/log/ip-css",
                    configPath = "$sharePath/ip-css/config",
                )
            }

            NasPlatform.ASUSTOR_ADM -> {
                val volumePath = getAsustorVolumePath()
                SystemPaths(
                    dataPath = "$volumePath/ip-css/data",
                    recordingsPath = "$volumePath/ip-css/recordings",
                    logsPath = "/var/log/ip-css",
                    configPath = "$volumePath/ip-css/config",
                )
            }

            NasPlatform.TRUENAS_CORE -> {
                val tankPath = getTrueNasTankPath()
                SystemPaths(
                    dataPath = "$tankPath/ip-css/data",
                    recordingsPath = "$tankPath/ip-css/recordings",
                    logsPath = "/var/log/ip-css",
                    configPath = "$tankPath/ip-css/config",
                )
            }

            NasPlatform.TRUENAS_SCALE -> {
                val tankPath = getTrueNasTankPath()
                SystemPaths(
                    dataPath = "$tankPath/ip-css/data",
                    recordingsPath = "$tankPath/ip-css/recordings",
                    logsPath = "/var/log/ip-css",
                    configPath = "$tankPath/ip-css/config",
                )
            }

            NasPlatform.UNKNOWN -> SystemPaths.default()
        }
    }

    /**
     * Проверка Synology DSM
     */
    private fun isSynology(): Boolean {
        // Synology имеет файл /etc/synoinfo.conf
        if (File("/etc/synoinfo.conf").exists()) {
            return true
        }

        // Проверка переменной окружения
        if (System.getenv("SYNOLOGY") != null) {
            return true
        }

        // Проверка наличия synology в hostname
        val hostname = System.getenv("HOSTNAME") ?: ""
        if (hostname.contains("synology", ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Проверка QNAP QTS
     */
    private fun isQnap(): Boolean {
        // QNAP имеет файл /etc/config/qpkg.conf
        if (File("/etc/config/qpkg.conf").exists()) {
            return true
        }

        // Проверка переменной окружения
        if (System.getenv("QNAP") != null) {
            return true
        }

        // Проверка наличия qnap в hostname
        val hostname = System.getenv("HOSTNAME") ?: ""
        if (hostname.contains("qnap", ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Проверка Asustor ADM
     */
    private fun isAsustor(): Boolean {
        // Asustor имеет файл /usr/local/AppCentral/AppCentral.conf
        if (File("/usr/local/AppCentral/AppCentral.conf").exists()) {
            return true
        }

        // Проверка переменной окружения
        if (System.getenv("ASUSTOR") != null) {
            return true
        }

        // Проверка наличия asustor в hostname
        val hostname = System.getenv("HOSTNAME") ?: ""
        if (hostname.contains("asustor", ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Проверка TrueNAS CORE (FreeBSD)
     */
    private fun isTrueNasCore(): Boolean {
        // TrueNAS CORE работает на FreeBSD
        val osName = System.getProperty("os.name", "").lowercase()
        if (!osName.contains("freebsd")) {
            return false
        }

        // Проверка наличия TrueNAS специфичных файлов
        if (File("/etc/rc.conf.d/truenas").exists()) {
            return true
        }

        // Проверка переменной окружения
        if (System.getenv("TRUENAS_CORE") != null) {
            return true
        }

        return false
    }

    /**
     * Проверка TrueNAS SCALE (Linux + Kubernetes)
     */
    private fun isTrueNasScale(): Boolean {
        // TrueNAS SCALE работает на Linux
        val osName = System.getProperty("os.name", "").lowercase()
        if (!osName.contains("linux")) {
            return false
        }

        // Проверка наличия TrueNAS SCALE специфичных файлов
        if (File("/etc/systemd/system/truenas.service").exists()) {
            return true
        }

        // Проверка переменной окружения
        if (System.getenv("TRUENAS_SCALE") != null) {
            return true
        }

        // Проверка наличия Kubernetes (TrueNAS SCALE использует Kubernetes)
        if (File("/usr/local/bin/k3s").exists() || File("/usr/bin/kubectl").exists()) {
            // Дополнительная проверка - может быть обычный Kubernetes
            // Проверяем наличие TrueNAS специфичных файлов
            if (File("/etc/systemd/system/truenas.service").exists()) {
                return true
            }
        }

        return false
    }

    /**
     * Получает путь к первому тому Synology
     */
    private fun getSynologyVolumePath(): String {
        // Проверяем наличие /volume1
        if (File("/volume1").exists() && File("/volume1").isDirectory) {
            return "/volume1"
        }

        // Ищем первый доступный том
        for (i in 1..10) {
            val volumePath = "/volume$i"
            if (File(volumePath).exists() && File(volumePath).isDirectory) {
                return volumePath
            }
        }

        // Fallback на /var/packages (для пакетов)
        return "/var/packages/ip-css"
    }

    /**
     * Получает путь к share QNAP
     */
    private fun getQnapSharePath(): String {
        // Проверяем наличие /share/CACHEDEV1_DATA
        if (File("/share/CACHEDEV1_DATA").exists() && File("/share/CACHEDEV1_DATA").isDirectory) {
            return "/share/CACHEDEV1_DATA"
        }

        // Ищем первый доступный share
        val shareDir = File("/share")
        if (shareDir.exists() && shareDir.isDirectory) {
            shareDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("CACHEDEV")) {
                    return file.absolutePath
                }
            }
        }

        // Fallback на /share/Public
        if (File("/share/Public").exists()) {
            return "/share/Public"
        }

        // Fallback на /share
        return "/share"
    }

    /**
     * Получает путь к первому тому Asustor
     */
    private fun getAsustorVolumePath(): String {
        // Проверяем наличие /volume1
        if (File("/volume1").exists() && File("/volume1").isDirectory) {
            return "/volume1"
        }

        // Ищем первый доступный том
        for (i in 1..10) {
            val volumePath = "/volume$i"
            if (File(volumePath).exists() && File(volumePath).isDirectory) {
                return volumePath
            }
        }

        // Fallback на /home
        return "/home"
    }

    /**
     * Получает путь к tank TrueNAS
     */
    private fun getTrueNasTankPath(): String {
        // Проверяем наличие /mnt/tank
        if (File("/mnt/tank").exists() && File("/mnt/tank").isDirectory) {
            return "/mnt/tank"
        }

        // Ищем первый доступный pool в /mnt
        val mntDir = File("/mnt")
        if (mntDir.exists() && mntDir.isDirectory) {
            mntDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.canRead()) {
                    return file.absolutePath
                }
            }
        }

        // Fallback на /mnt
        return "/mnt"
    }
}
