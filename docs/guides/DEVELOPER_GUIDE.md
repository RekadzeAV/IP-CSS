 # Developer Guide

## Overview

This guide provides comprehensive documentation for developers working on the IP-CSS camera management system.

**Target Audience:** Software developers, system architects  
**Version:** 1.0  
**Last Updated:** 2026-05-17

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Development Environment Setup](#development-environment-setup)
3. [Building the Project](#building-the-project)
4. [Testing](#testing)
5. [Architecture](#architecture)
6. [Contributing](#contributing)

---

## Project Structure

```
ip-camera/
├── core/
│   ├── common/           # Common utilities and shared code
│   ├── network/          # Network layer, RTSP client, security
│   ├── security/         # Security module (password hashing, encryption)
│   └── ui-bridge/        # UI abstraction layer
├── shared/               # Shared business logic (use cases, repositories)
├── android/              # Android application
│   ├── app/
│   └── ...
├── desktop/              # Desktop application (Compose Multiplatform)
│   └── ...
├── scripts/              # Build and CI scripts
├── docs/                 # Documentation
│   ├── api/              # API documentation
│   ├── blocks/           # Block implementation docs
│   └── guides/           # User and developer guides
├── .github/              # GitHub Actions workflows
└── build.gradle.kts      # Root build configuration
```

---

## Development Environment Setup

### Prerequisites

#### 1. JDK 17+

```bash
# Verify installation
java --version
```

#### 2. Android Studio

- Download from: https://developer.android.com/studio
- Install with:
  - Android SDK
  - Android NDK (version 30.0.14904198)
  - CMake (3.22.1+)

#### 3. Xcode (macOS only)

- Install from Mac App Store
- Command Line Tools:
  ```bash
  xcode-select --install
  ```

#### 4. FFmpeg (Optional for video processing)

```bash
# macOS (Homebrew)
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg

# Windows (Chocolatey)
choco install ffmpeg
```

#### 5. Git

```bash
git --version
```

### Clone Repository

```bash
git clone https://github.com/your-org/ip-camera.git
cd ip-camera
```

### Gradle Setup

```bash
# Gradle wrapper setup
./gradlew wrapper

# Verify Gradle
./gradlew --version
```

### IDE Setup

#### IntelliJ IDEA / Android Studio

1. **Open project:**
   - File → Open → Select project root
   - Select "Open as Project"

2. **Sync Gradle:**
   - Gradle will sync automatically
   - Wait for dependencies to download

3. **Configure SDKs:**
   - File → Project Structure → SDKs
   - Add Android SDK if needed
   - Set JDK location

4. **Run Configurations:**
   - Run → Edit Configurations
   - Create new for Desktop/Android

#### VS Code (Lightweight editing)

1. Install extensions:
   - Kotlin Language
   - Gradle for Java
   - YAML

2. Open folder in VS Code

---

## Building the Project

### Common Build Commands

```bash
# Clean build
./gradlew clean

# Build all platforms
./gradlew build

# Build Desktop
./gradlew :desktop:assemble

# Build Android
./gradlew :android:app:assembleDebug

# Build iOS (macOS only)
./gradlew :iosApp:assemble

# Build native libraries
./gradlew :native:build
```

### Platform-Specific Builds

#### Windows

```powershell
.\gradlew.bat clean build --no-daemon
```

#### Linux/macOS

```bash
./gradlew clean build --no-daemon
```

### Build Profiles

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# With tests
./gradlew build --tests "*"

# Without tests (faster)
./gradlew build -x test
```

### Build Optimization

```bash
# Enable parallel builds
./gradlew build --parallel

# Enable build cache
./gradlew build --build-cache

# Limit workers
./gradlew build --max-workers=4
```

---

## Testing

### Running Tests

```bash
# All desktop tests
./gradlew desktopTest

# Security module tests
./gradlew :core:security:desktopTest

# UI Bridge tests
./gradlew :core:ui-bridge:desktopTest

# Network tests
./gradlew :core:network:desktopTest

# Single test class
./gradlew :core:security:desktopTest --tests "PasswordHasherTest"

# Single test method
./gradlew :core:security:desktopTest --tests "PasswordHasherTest.testHashPassword"
```

### Test Reports

```bash
# Generate reports
./gradlew test

# View HTML report (after running)
open core/security/build/reports/tests/desktopTest/index.html
```

### Test Coverage

```bash
# Run with coverage
./gradlew jvmTest --coverage

# Coverage report
./gradlew koverHtmlReport
```

### Writing Tests

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyTest {
    @Test
    fun testExample() {
        val result = someFunction()
        assertEquals("expected", result)
        assertTrue(result.isNotEmpty())
    }
}
```

### Test Best Practices

1. **Test naming:**
   - Use descriptive names: `testLoginSuccess`, `testPasswordHashing`
   - Follow AAA pattern: Arrange, Act, Assert

2. **Test isolation:**
   - Each test should be independent
   - No shared state between tests
   - Use setup/teardown for cleanup

3. **Mock dependencies:**
   - Use mock objects for external dependencies
   - Test only your code's behavior

4. **Coverage:**
   - Aim for 80%+ code coverage
   - Focus on critical paths

---

## Architecture

### Multiplatform Structure

```
commonMain/     # Shared code (platform-independent)
commonTest/     # Shared tests

desktopMain/    # Desktop-specific code (JVM)
desktopTest/    # Desktop-specific tests

androidMain/    # Android-specific code
androidTest/    # Android-specific tests

iosMain/        # iOS-specific code
iosTest/        # iOS-specific tests
```

### Module Dependencies

```
core:common      → No dependencies
core:security    → core:common
core:network     → core:common, core:security
core:ui-bridge   → core:common, core:network, core:security, shared
shared           → core:common, core:network, core:security
```

### Key Patterns

#### 1. Use Case Pattern

```kotlin
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        // Business logic here
        return userRepository.login(username, password)
    }
}
```

#### 2. Repository Pattern

```kotlin
interface UserRepository {
    suspend fun login(username: String, password: String): LoginResult
    suspend fun getUserById(id: String): User?
}

class UserRepositoryImpl(
    private val api: UserApi,
    private val localDataSource: UserLocalDataSource
) : UserRepository {
    // Implementation
}
```

#### 3. UI Bridge Pattern

```kotlin
interface AuthenticationBridge {
    suspend fun login(username: String, password: String): LoginResult
}

class DesktopAuthenticationBridge(
    private val loginUseCase: LoginUseCase
) : AuthenticationBridge {
    override suspend fun login(username: String, password: String): LoginResult {
        return loginUseCase(username, password)
    }
}
```

#### 4. Result Pattern

```kotlin
// Success case
val result = someOperation()
if (result.isSuccess) {
    val data = result.getOrNull()
    // Use data
}

// Or use when expression
when (result) {
    is Result.Success -> { /* handle success */ }
    is Result.Failure -> { /* handle error */ }
}
```

### Coroutine Usage

```kotlin
// Launch coroutine
lifecycleScope.launch {
    val result = suspendFunction()
    // Update UI
}

// Async
val result = withContext(Dispatchers.IO) {
    suspendFunction()
}

// Flow
val flow = flow {
    emit(data)
}.flowOn(Dispatchers.IO)
```

---

## Contributing

### Workflow

1. **Fork repository**
2. **Create branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make changes**
4. **Run tests:**
   ```bash
   ./gradlew test
   ```

5. **Commit changes:**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

6. **Push branch:**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Create Pull Request**

### Commit Message Format

```
type(scope): description

[optional body]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Build/config changes

**Examples:**
```
feat(auth): add password reset functionality
fix(camera): resolve RTSP connection timeout
docs(api): update security module API docs
```

### Code Style

#### Kotlin Style Guide

```kotlin
// Naming conventions
class ClassName {                    // PascalCase
    fun functionName() {             // camelCase
        val variableName = "value"   // camelCase
        const val CONSTANT_NAME = 42 // SCREAMING_SNAKE_CASE
    }
}

// File structure
// 1. Imports
// 2. Top-level properties
// 3. Class/function definitions

// Braces
fun function() {                     // Opening brace on same line
    if (condition) {
        // Action
    }
}

// Line length: Max 100 characters
```

### Pull Request Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Tests added for new features
- [ ] Documentation updated
- [ ] No compiler warnings
- [ ] All tests passing
- [ ] PR description is clear

### Code Review Process

1. **Submit PR**
2. **Automated checks:**
   - CI/CD pipeline runs
   - Tests execute
   - Code style checks
3. **Team review:**
   - At least 2 reviewers required
   - Comments addressed
4. **Merge:**
   - Squash and merge preferred
   - Keep commit history clean

---

## Debugging

### Desktop Debugging

1. **Run in debug mode:**
   ```bash
   ./gradlew :desktop:run --debug-jvm
   ```

2. **Attach debugger:**
   - IntelliJ: Run → Attach to Process
   - Port: 5005

### Android Debugging

1. **Enable debug logging:**
   ```kotlin
   BuildConfig.DEBUG // true in debug builds
   ```

2. **Logcat:**
   ```bash
   adb logcat
   ```

3. **Android Studio Debugger:**
   - Run → Debug
   - Breakpoints, step through code

### Common Debugging Techniques

```kotlin
// Logging
kotlin.io.println("Debug: $variable")
Log.d("TAG", "Message")

// Assertions
check(condition) { "Error message" }
require(parameter > 0) { "Parameter must be positive" }

// Try-catch
try {
    riskyOperation()
} catch (e: Exception) {
    e.printStackTrace()
}
```

---

## Performance Optimization

### Profiling

```bash
# JVM profiler
./gradlew :desktop:run --profile

# Android Profiler
# Android Studio → Profiler tab
```

### Optimization Tips

1. **Avoid unnecessary allocations**
2. **Use lazy initialization**
3. **Optimize database queries**
4. **Cache frequently used data**
5. **Use coroutines efficiently**
6. **Minimize network calls**

---

## Security Best Practices

1. **Never commit secrets/tokens**
   - Use environment variables
   - Use `.gitignore` for sensitive files

2. **Secure password storage**
   - Use `PasswordHasher` (bcrypt/PBKDF2)
   - Never store plain passwords

3. **Certificate pinning**
   - Always enable in production
   - Update pins before expiration

4. **Input validation**
   - Validate all user inputs
   - Sanitize before processing

5. **Error handling**
   - Don't leak sensitive info in errors
   - Log errors securely

---

## Release Process

### Version Bumping

```bash
# Update version in build.gradle.kts
version = "1.0.0"

# Commit
git commit -m "chore(release): bump version to 1.0.0"
```

### Build Release

```bash
# Desktop release
./gradlew :desktop:assembleRelease

# Android release
./gradlew :android:app:assembleRelease
```

### Create Tag

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

---

## Appendix

### Useful Commands

```bash
# Clean everything
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Update Gradle
./gradlew wrapper --gradle-version=8.0

# Generate API docs
./gradlew dokkaHtml

# Format code
./gradlew ktlintFormat
```

### External Resources

- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html)
- [Ktor Client](https://ktor.io/docs/getting-started-ktor-client.html)

### Contact

- **Team:** NLP-Core-Team
- **Email:** dev@ipcamera.com
- **Slack:** #ip-camera-dev

---

**End of Developer Guide**
