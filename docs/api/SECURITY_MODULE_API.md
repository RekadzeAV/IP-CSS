# Security Module API

## Overview

The Security Module (`core:security`) provides cross-platform security utilities for password hashing, data encryption, token protection, and brute force protection.

**Module Path:** `core:security`  
**Platforms:** Desktop (JVM), Android, iOS, macOS, Linux, Windows  
**Kotlin Multiplatform:** ✅ Yes

---

## Table of Contents

1. [Password Hashing](#password-hashing)
2. [Security Configuration](#security-configuration)
3. [Token Encryption](#token-encryption)
4. [Brute Force Protection](#brute-force-protection)
5. [Password Encryption](#password-encryption)
6. [Examples](#examples)

---

## Password Hashing

### Interface: `PasswordHasher`

Hashes and verifies passwords using secure algorithms.

```kotlin
interface PasswordHasher {
    fun hash(password: CharArray): String
    fun verify(password: CharArray, hashedPassword: String): Boolean
    fun isValidHash(hashedPassword: String): Boolean
    fun getAlgorithm(): String
}
```

### Factory: `PasswordHasherFactory`

```kotlin
object PasswordHasherFactory {
    fun create(): PasswordHasher
    fun create(algorithm: PasswordHashAlgorithm): PasswordHasher
}
```

### Algorithms

```kotlin
enum class PasswordHashAlgorithm {
    ARGON2ID,  // Recommended (winner of Password Hashing Competition)
    BCRYPT,    // Classic secure algorithm (JVM/Desktop)
    SCRYPT     // GPU/ASIC resistant (Android/iOS)
}
```

### Usage

```kotlin
// Create hasher
val hasher = PasswordHasherFactory.create()

// Hash password
val password = "MySecurePassword123!".toCharArray()
val hashedPassword = hasher.hash(password)
// Format: $pbkdf2-sha256$iterations$salt$hash

// Verify password
val isValid = hasher.verify(password, hashedPassword)

// Check algorithm
val algorithm = hasher.getAlgorithm() // "PBKDF2WithHmacSHA256" or "bcrypt"
```

### Platform-Specific Implementations

| Platform | Algorithm | Notes |
|----------|-----------|-------|
| JVM/Desktop | bcrypt (work factor 12) | Uses `org.mindrot:jbcrypt` |
| Android | PBKDF2-SHA256 | Uses Android Keystore |
| iOS | PBKDF2-SHA256 | Uses CommonCrypto |

---

## Security Configuration

### Data Class: `SecurityConfig`

```kotlin
data class SecurityConfig(
    val enablePasswordEncryption: Boolean = true,
    val enableLocalDataEncryption: Boolean = true,
    val enableCertificatePinning: Boolean = true,
    val certificatePinningConfigPath: String = "security/certificate-pinning.json",
    val enableInputValidation: Boolean = true,
    val strictMode: Boolean = false,
    val logAttackAttempts: Boolean = true,
    val keyStorePath: String? = null
)
```

### Factory: `SecurityConfigFactory`

```kotlin
object SecurityConfigFactory {
    fun create(block: SecurityConfigBuilder.() -> Unit = {}): SecurityConfig
    fun createProduction(): SecurityConfig
    fun createDevelopment(): SecurityConfig
}
```

### Builder: `SecurityConfigBuilder`

```kotlin
class SecurityConfigBuilder {
    fun enablePasswordEncryption(enabled: Boolean): SecurityConfigBuilder
    fun enableLocalDataEncryption(enabled: Boolean): SecurityConfigBuilder
    fun enableCertificatePinning(enabled: Boolean): SecurityConfigBuilder
    fun certificatePinningConfigPath(path: String): SecurityConfigBuilder
    fun enableInputValidation(enabled: Boolean): SecurityConfigBuilder
    fun strictMode(enabled: Boolean): SecurityConfigBuilder
    fun logAttackAttempts(enabled: Boolean): SecurityConfigBuilder
    fun keyStorePath(path: String?): SecurityConfigBuilder
    fun build(): SecurityConfig
}
```

### Usage

```kotlin
// Production config
val prodConfig = SecurityConfigFactory.createProduction()

// Development config
val devConfig = SecurityConfigFactory.createDevelopment()

// Custom config
val customConfig = SecurityConfigFactory.create {
    enablePasswordEncryption(true)
    enableLocalDataEncryption(true)
    enableCertificatePinning(true)
    strictMode(true)
    logAttackAttempts(true)
}
```

---

## Token Encryption

### Interface: `TokenEncryption`

```kotlin
interface TokenEncryption {
    fun encrypt(token: String): String
    fun decrypt(encryptedToken: String): String
    fun isValid(encryptedToken: String): Boolean
}
```

### Factory: `TokenEncryptionFactory`

```kotlin
object TokenEncryptionFactory {
    fun create(): TokenEncryption
}
```

### Usage

```kotlin
val tokenEncryption = TokenEncryptionFactory.create()

// Encrypt JWT token
val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
val encrypted = tokenEncryption.encrypt(jwtToken)

// Decrypt token
val decrypted = tokenEncryption.decrypt(encrypted)

// Validate token
val isValid = tokenEncryption.isValid(encrypted)
```

---

## Brute Force Protection

### Interface: `BruteForceProtection`

```kotlin
interface BruteForceProtection {
    fun recordLoginAttempt(username: String?, ipAddress: String?, success: Boolean)
    fun isUserBlocked(username: String): Boolean
    fun isIpBlocked(ipAddress: String): Boolean
    fun getUserUnlockTime(username: String): Long?
    fun getIpUnlockTime(ipAddress: String): Long?
    fun resetFailedAttempts(username: String)
    fun resetIpFailedAttempts(ipAddress: String)
    fun getFailedAttempts(username: String): Int
}
```

### Factory: `BruteForceProtectionFactory`

```kotlin
object BruteForceProtectionFactory {
    fun create(): BruteForceProtection
    fun create(config: BruteForceConfig): BruteForceProtection
}
```

### Configuration: `BruteForceConfig`

```kotlin
data class BruteForceConfig(
    val maxFailedAttempts: Int = 5,
    val lockoutDurationMinutes: Long = 30,
    val timeWindowMinutes: Long = 15,
    val enableIpBlocking: Boolean = true,
    val enableUsernameBlocking: Boolean = true
)
```

### Usage

```kotlin
val protection = BruteForceProtectionFactory.create()

// Record login attempt
protection.recordLoginAttempt("admin", "192.168.1.100", success = false)

// Check if blocked
if (protection.isUserBlocked("admin")) {
    val unlockTime = protection.getUserUnlockTime("admin")
    println("Account locked until: $unlockTime")
}

// Get failed attempts
val attempts = protection.getFailedAttempts("admin")

// Unlock account
protection.resetFailedAttempts("admin")
```

---

## Password Encryption

### Interface: `PasswordEncryption`

```kotlin
interface PasswordEncryption {
    fun encrypt(password: String): String
    fun decrypt(encryptedPassword: String): String
    fun isEncrypted(value: String): Boolean
}
```

### Factory: `PasswordEncryptionFactory`

```kotlin
object PasswordEncryptionFactory {
    fun create(): PasswordEncryption
}
```

### Usage

```kotlin
val encryption = PasswordEncryptionFactory.create()

// Encrypt password for database storage
val password = "MyPassword123"
val encrypted = encryption.encrypt(password)

// Decrypt password from database
val decrypted = encryption.decrypt(encrypted)

// Check if already encrypted
val isEncrypted = encryption.isEncrypted(password)
```

---

## Examples

### Complete Authentication Flow

```kotlin
class AuthenticationService {
    private val hasher = PasswordHasherFactory.create()
    private val encryption = PasswordEncryptionFactory.create()
    private val bruteForce = BruteForceProtectionFactory.create()

    suspend fun login(username: String, password: String): LoginResult {
        // Check brute force
        if (bruteForce.isUserBlocked(username)) {
            return LoginResult.Failure("Account locked")
        }

        // Get user from database
        val user = getUserByUsername(username) ?: return LoginResult.Failure("Invalid credentials")

        // Verify password
        val hashedPassword = encryption.decrypt(user.encryptedPassword)
        if (!hasher.verify(password.toCharArray(), hashedPassword)) {
            bruteForce.recordLoginAttempt(username, null, success = false)
            return LoginResult.Failure("Invalid credentials")
        }

        // Success
        bruteForce.resetFailedAttempts(username)
        return LoginResult.Success(user)
    }

    suspend fun register(username: String, password: String, email: String): RegistrationResult {
        // Hash password
        val hashedPassword = hasher.hash(password.toCharArray())
        val encryptedPassword = encryption.encrypt(hashedPassword)

        // Create user
        val user = User(
            id = generateId(),
            username = username,
            email = email,
            encryptedPassword = encryptedPassword
        )

        saveUser(user)
        return RegistrationResult.Success(user)
    }
}
```

### Security Module Integration

```kotlin
class AppSecurityModule {
    private val config = SecurityConfigFactory.createProduction()
    private val hasher = PasswordHasherFactory.create()
    private val tokenEncryption = TokenEncryptionFactory.create()
    private val bruteForce = BruteForceProtectionFactory.create()

    fun createAuthenticationService(): AuthenticationService {
        return AuthenticationService(
            hasher = hasher,
            tokenEncryption = tokenEncryption,
            bruteForce = bruteForce
        )
    }
}
```

---

## Testing

### Unit Tests

```kotlin
class PasswordHasherTest {
    private val hasher = PasswordHasherFactory.create()

    @Test
    fun testHashAndVerify() {
        val password = "TestPassword123".toCharArray()
        val hashed = hasher.hash(password)
        
        assertTrue(hasher.verify(password, hashed))
        assertFalse(hasher.verify("WrongPassword".toCharArray(), hashed))
    }

    @Test
    fun testDifferentHashes() {
        val password = "SamePassword".toCharArray()
        val hash1 = hasher.hash(password)
        val hash2 = hasher.hash(password)
        
        assertNotEquals(hash1, hash2) // Different salts
    }
}
```

---

## API Reference

### Packages

- `com.company.ipcamera.core.security` — Main security interfaces and factories
- `com.company.ipcamera.core.security.algorithm` — Algorithm-specific implementations
- `com.company.ipcamera.core.security.config` — Configuration classes

### Dependencies

#### Required
- `kotlinx-serialization-json` — Configuration serialization
- `ktor-client-core` — HTTP client security

#### Platform-Specific
- JVM: `org.mindrot:jbcrypt:4.0.0`
- Android: `androidx.security:security-crypto`
- iOS: CommonCrypto (built-in)

---

## Security Best Practices

1. **Always use strong passwords** — Minimum 12 characters, mix of character types
2. **Use production configuration** — Enable all security features in production
3. **Enable certificate pinning** — Protects against MITM attacks
4. **Monitor brute force attempts** — Log and alert on suspicious activity
5. **Regular key rotation** — Rotate encryption keys periodically
6. **Secure key storage** — Use platform keystore/keychain
7. **Never log passwords** — Always use CharArray for passwords

---

**API Version:** 1.0  
**Last Updated:** 2026-05-17  
**Maintainer:** NLP-Core-Team
