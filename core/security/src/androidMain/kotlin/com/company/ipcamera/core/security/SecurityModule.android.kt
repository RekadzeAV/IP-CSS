package com.company.ipcamera.core.security

import com.company.ipcamera.core.common.security.PasswordEncryption
import com.company.ipcamera.core.common.security.LocalDataEncryption
import com.company.ipcamera.core.common.security.InputValidator
import com.company.ipcamera.core.network.security.CertificatePinner
import com.company.ipcamera.core.network.security.CertificatePinningConfig

/**
 * Android реализация SecurityModule
 */
actual fun createSecurityModule(): SecurityModule {
    return object : SecurityModule {
        override val passwordEncryption: PasswordEncryption
            get() = com.company.ipcamera.core.common.security.PasswordEncryptionFactory.create()
        override val localDataEncryption: LocalDataEncryption
            get() = com.company.ipcamera.core.common.security.LocalDataEncryptionFactory.create()
        override val certificatePinner: CertificatePinner
            get() = CertificatePinner(CertificatePinningConfig.disabled())
        override val inputValidator: InputValidator
            get() = InputValidator
    }
}