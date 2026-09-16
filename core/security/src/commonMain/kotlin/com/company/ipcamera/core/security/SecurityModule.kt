package com.company.ipcamera.core.security

import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.core.common.security.LocalDataEncryption
import com.company.ipcamera.core.network.security.CertificatePinner
import com.company.ipcamera.core.common.security.InputValidator

/**
 * Основной интерфейс модуля безопасности
 * Предоставляет доступ ко всем функциям безопасности
 */
interface SecurityModule {
    val passwordEncryption: PasswordEncryption
    val localDataEncryption: LocalDataEncryption
    val certificatePinner: CertificatePinner
    val inputValidator: InputValidator
}

/**
 * Expect класс для создания экземпляра SecurityModule
 */
expect fun createSecurityModule(): SecurityModule
