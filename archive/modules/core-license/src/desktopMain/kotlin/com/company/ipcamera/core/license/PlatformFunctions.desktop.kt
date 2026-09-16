package com.company.ipcamera.core.license

actual fun getPlatformCrypto(): PlatformCrypto = PlatformCrypto()

actual fun createLicenseRepository(context: Any?): LicenseRepository = LicenseRepository(context)