plugins {
    kotlin("multiplatform") version "2.0.0" apply false
    kotlin("android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.android.application") version "8.3.0" apply false
    id("com.android.library") version "8.3.0" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.1" apply false
    id("app.cash.sqldelight") version "2.0.2" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// Локальный Maven-репозиторий для офлайн-сборок
val localRepo = file("${rootProject.projectDir}/local-maven-repo")

allprojects {
    repositories {
        // Сначала локальный репозиторий (для офлайн-сборок)
        if (localRepo.exists()) {
            maven { url = uri(localRepo.toURI()) }
        }
        
        // Затем удалённые репозитории
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://jitpack.io") }
    }
    
    // Configure build output directory structure
    afterEvaluate {
        if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") || 
            plugins.hasPlugin("org.jetbrains.kotlin.android") ||
            plugins.hasPlugin("com.android.application") ||
            plugins.hasPlugin("com.android.library")) {
            tasks.withType<AbstractArchiveTask>().configureEach {
                archiveBaseName.set(project.name)
                archiveVersion.set(project.version.toString())
            }
        }
    }
}

// ====================================
// Task: downloadAllDependencies
// Скачивает все зависимости из Gradle cache в local-maven-repo для офлайн-сборок
// ====================================
tasks.register("downloadAllDependencies") {
    group = "dependency management"
    description = "Download all dependencies from Gradle cache to local-maven-repo for offline builds"

    val repoDir = localRepo
    val cacheDirPath = "${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1"

    doLast {
        println("\n==========================================")
        println("Downloading dependencies to local repository")
        println("==========================================")
        println("Local repository: ${repoDir.absolutePath}")
        println("==========================================\n")

        repoDir.mkdirs()

        val gradleCache = file(cacheDirPath)
        if (!gradleCache.exists()) {
            throw GradleException("Gradle cache not found at: ${gradleCache.absolutePath}")
        }

        var totalFiles = 0
        var totalSize = 0L

        gradleCache.listFiles()?.forEach { groupDir ->
            if (groupDir.isDirectory) {
                groupDir.listFiles()?.forEach { artifactDir ->
                    if (artifactDir.isDirectory) {
                        artifactDir.listFiles()?.forEach { versionDir ->
                            if (versionDir.isDirectory) {
                                val sourceFiles = versionDir.listFiles { _, name ->
                                    name.endsWith(".jar") || name.endsWith(".pom")
                                } ?: emptyArray()

                                sourceFiles.forEach { sourceFile ->
                                    val destDir = file("${repoDir.absolutePath}/${groupDir.name}/${artifactDir.name}/${versionDir.name}")
                                    destDir.mkdirs()

                                    val destFile = file("${destDir.absolutePath}/${sourceFile.name}")
                                    if (!destFile.exists()) {
                                        sourceFile.copyTo(destFile, overwrite = true)
                                        totalFiles++
                                        totalSize += sourceFile.length()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        println("\n==========================================")
        println("Download complete!")
        println("Total files: $totalFiles")
        println("Total size: ${totalSize / 1024 / 1024} MB")
        println("Local repository: ${repoDir.absolutePath}")
        println("==========================================\n")
    }
}

// ====================================
// Build Cache Optimizations
// ====================================

subprojects {
    // Enable reproducible builds for better cache hits
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    // Optimize copy tasks
    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
    dependsOn("cleanNativeLibraries")
}

// ====================================
// Build Cache Management Tasks
// ====================================

tasks.register("buildCacheStats") {
    group = "help"
    description = "Show build cache statistics"

    doLast {
        println("\n==========================================")
        println("Build Cache Statistics")
        println("==========================================")
        println("Local cache directory: ${gradle.gradleUserHomeDir}/caches/build-cache-1")
        val cacheDir = file("${gradle.gradleUserHomeDir}/caches/build-cache-1")
        val cacheSize = if (cacheDir.exists()) {
            cacheDir.listFiles()?.size ?: 0
        } else {
            0
        }
        println("Cache entries: $cacheSize")
        println("Cache is enabled: true (configured in gradle.properties)")
        println("==========================================\n")
    }
}

tasks.register("cleanBuildCache") {
    group = "build"
    description = "Clean Gradle build cache"

    doLast {
        val cacheDir = file("${gradle.gradleUserHomeDir}/caches/build-cache-1")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            println("✓ Build cache cleaned: ${cacheDir.absolutePath}")
        } else {
            println("✓ Build cache already empty")
        }
    }
}

tasks.register("verifyBuildCache") {
    group = "verification"
    description = "Verify build cache is working correctly"

    doLast {
        println("\n==========================================")
        println("Build Cache Verification")
        println("==========================================")
        
        val cacheDir = file("${gradle.gradleUserHomeDir}/caches/build-cache-1")
        val cacheSize = if (cacheDir.exists()) {
            cacheDir.listFiles()?.size ?: 0
        } else {
            0
        }
        
        println("Cache entries: $cacheSize")
        println("Cache directory: ${cacheDir.absolutePath}")
        
        if (cacheSize > 0) {
            println("✓ Build cache is active and storing entries")
        } else {
            println("⚠ Build cache may not be active yet")
        }
        
        println("==========================================\n")
    }
}

tasks.register("buildAll") {
    dependsOn(
        "buildNativeLibraries",
        ":core:common:build",
        ":shared:build",
        // ":core:license:build", // Отложено: лицензирование вынесено за рамки проекта
        ":core:network:build"
    )
    group = "build"
    description = "Build all available modules"
}

tasks.register("testAll") {
    dependsOn(
        ":core:common:test",
        ":shared:test",
        // ":core:license:test", // Отложено: лицензирование вынесено за рамки проекта
        ":core:network:test"
    )
    group = "verification"
    description = "Run all tests"
}

/** Фаза 1 MVP: автоматическая приёмка (JVM-интеграция shared + network desktop + тесты server API). См. docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md */
tasks.register("mvpAutomatedAcceptance") {
    group = "verification"
    description =
        "MVP Phase 1 automated acceptance: :shared:desktopTest + :core:network:desktopTest + :server:api:test"
    dependsOn(":shared:desktopTest", ":core:network:desktopTest", ":server:api:test")
}

/** CI: один граф задач вместо нескольких вызовов gradlew (Linux KMP + metadata + KGP checks). См. .github/workflows/ci.yml */
tasks.register("ciKmpLinuxSanity") {
    group = "verification"
    description =
        "CI Linux: common Android/Desktop/NativeLinux + metadata + checkKotlinGradlePluginConfigurationErrors"
    dependsOn(
        ":core:common:compileReleaseKotlinAndroid",
        ":core:common:compileKotlinDesktop",
        ":core:common:compileKotlinNativeLinux",
        ":core:common:compileKotlinMetadata",
        ":core:network:compileKotlinMetadata",
        ":shared:compileKotlinMetadata",
        ":core:common:checkKotlinGradlePluginConfigurationErrors",
        ":core:network:checkKotlinGradlePluginConfigurationErrors",
        ":shared:checkKotlinGradlePluginConfigurationErrors",
    )
}

/** CI: macOS iOS compile slice + metadata (см. .github/workflows/ci.yml) */
tasks.register("ciKmpMacosIosCompile") {
    group = "verification"
    description = "CI macOS: iOS X64 + iOS SimulatorArm64 + network/shared metadata"
    dependsOn(
        ":core:common:compileKotlinIosX64",
        ":core:common:compileKotlinIosSimulatorArm64",
        ":core:network:compileKotlinMetadata",
        ":shared:compileKotlinMetadata",
    )
}

/** CI: Windows desktop + mingw native + metadata (см. .github/workflows/ci.yml) */
tasks.register("ciKmpWindowsSanity") {
    group = "verification"
    description = "CI Windows: common Desktop/NativeWindows + network/shared metadata"
    dependsOn(
        ":core:common:compileKotlinDesktop",
        ":core:common:compileKotlinNativeWindows",
        ":core:network:compileKotlinMetadata",
        ":shared:compileKotlinMetadata",
    )
}

/** CI: полный build трёх KMP-модулей (включает check/test внутри каждого `build`). См. .github/workflows/ci.yml */
tasks.register("ciBuildCoreSharedModules") {
    group = "verification"
    description = "CI: :core:common:build + :core:network:build + :shared:build (один демон; отдельный :test не нужен)"
    dependsOn(
        ":core:common:build",
        ":core:network:build",
        ":shared:build",
    )
}

/** CI: coverage gate for critical modules */
tasks.register("ciCoverageGate") {
    group = "verification"
    description = "CI coverage gate for :core:network, :shared and :server:api"
    dependsOn(
        ":core:network:koverVerify",
        ":shared:koverVerify",
        ":server:api:koverVerify",
    )
}

/** Deterministic unit/smoke test suite for PR CI. */
tasks.register("ciUnitSmokeSuite") {
    group = "verification"
    description = "Deterministic unit/smoke suite for core/shared/server/android"
    dependsOn(
        ":core:common:test",
        ":core:network:test",
        ":shared:test",
        ":server:api:test",
        ":android:app:testDebugUnitTest",
    )
}

/** Opt-in live integration suite (nightly/manual only). */
tasks.register("ciLiveIntegrationSuite") {
    group = "verification"
    description = "Live integration suite (requires external RTSP environment)"
    dependsOn(
        ":core:network:allTests",
        ":server:api:test",
    )
}

// =============================================================================
// Сборка без Android / только JVM и Desktop (стабилизация сборки)
// =============================================================================
// Требует ipcss.buildAndroidNative=false в gradle.properties (по умолчанию).

tasks.register("buildCoreDesktop") {
    group = "build"
    description = "Build core:common and core:network for Desktop (JVM) only; no Android, no native. Use when full build fails."
    dependsOn(
        ":core:common:compileKotlinDesktop",
        ":core:network:compileKotlinDesktop"
    )
}

tasks.register("buildServerApi") {
    group = "build"
    description = "Build server API module (JVM). Depends on shared; may fail if shared has compilation errors."
    dependsOn(":server:api:build")
}

tasks.register("publishToLocalMaven") {
    dependsOn(
        ":core:common:publishToMavenLocal",
        ":core:network:publishToMavenLocal",
        // ":core:license:publishToMavenLocal", // Отложено: лицензирование вынесено за рамки проекта
        ":shared:publishToMavenLocal"
    )
    group = "publishing"
    description = "Publish all modules to local Maven repository (~/.m2/repository)"
}

// =============================================================================
// NAS Packages Build Tasks
// =============================================================================

val isWindowsHost = System.getProperty("os.name").lowercase().contains("windows")
val projectVersion = project.findProperty("version")?.toString() ?: "Alfa-0.1.1"

// NAS package targets with their metadata
data class NasTarget(
    val name: String,
    val type: String,
    val arch: String,
    val packageFormat: String,
    val description: String
)

val nasTargets = listOf(
    NasTarget("SynologyX86", "synology", "x86_64", "SPK", "Synology SPK package for x86_64"),
    NasTarget("SynologyArm", "synology", "arm64", "SPK", "Synology SPK package for ARM64"),
    NasTarget("QnapX86", "qnap", "x86_64", "QPKG", "QNAP QPKG package for x86_64"),
    NasTarget("QnapArm", "qnap", "arm64", "QPKG", "QNAP QPKG package for ARM64"),
    NasTarget("AsustorX86", "asustor", "x86_64", "APK", "Asustor APK package for x86_64"),
    NasTarget("AsustorArm", "asustor", "arm64", "APK", "Asustor APK package for ARM64"),
    NasTarget("TruenasX86", "truenas", "x86_64", "Docker", "TrueNAS package (Docker/Kubernetes)")
)

// Register parameterized NAS build tasks
nasTargets.forEach { target ->
    val taskName = "buildNasPackage${target.name}"
    tasks.register<Exec>(taskName) {
        group = "build"
        description = "Build ${target.description}"
        dependsOn(":server:api:assemble")

        if (isWindowsHost) {
            commandLine(
                "powershell", "-ExecutionPolicy", "Bypass", "-File",
                "scripts/build-nas-package.ps1",
                "-PackageType", target.type,
                "-Arch", target.arch,
                "-Version", projectVersion
            )
        } else {
            commandLine(
                "bash", "scripts/build-nas-package.sh",
                target.type, target.arch, projectVersion
            )
        }
    }
}

tasks.register("buildNasPackages") {
    group = "build"
    description = "Build all NAS packages (Synology, QNAP, Asustor, TrueNAS)"
    dependsOn(nasTargets.map { "buildNasPackage${it.name}" })
}

// =============================================================================
// Native Libraries Build Tasks
// =============================================================================

// Native library targets
data class NativeTarget(val name: String, val osFilter: String, val scriptArg: String, val description: String)

val nativeTargets = listOf(
    NativeTarget("All", "all", "all", "Build all native libraries for current platform"),
    NativeTarget("Linux", "linux", "linux", "Build native libraries for Linux x64"),
    NativeTarget("MacOS", "mac", "macos", "Build native libraries for macOS (x64 and arm64)"),
    NativeTarget("Windows", "windows", "windows", "Build native libraries for Windows x64")
)

nativeTargets.forEach { target ->
    val taskName = "buildNativeFor${target.name}"
    tasks.register<Exec>(taskName) {
        group = "build"
        description = target.description
        workingDir = file("native")

        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("windows") -> {
                commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "../scripts/build-all-native-libs.ps1")
            }
            os.contains("linux") || os.contains("mac") -> {
                commandLine("bash", "../scripts/build-all-native-libs.sh", target.scriptArg)
            }
            else -> {
                doFirst { throw GradleException("Unsupported OS: $os") }
            }
        }
    }
}

tasks.register("buildNativeLibraries") {
    group = "build"
    description = "Build all native libraries for current platform"
    dependsOn("buildNativeForAll")
}

tasks.register("cleanNativeLibraries") {
    group = "clean"
    description = "Clean native libraries build artifacts"
    doLast {
        delete(fileTree("native/build") { include("**/*") })
        delete(fileTree("native/video-processing/lib") { include("**/*") })
        delete(fileTree("native/analytics/lib") { include("**/*") })
        delete(fileTree("native/codecs/lib") { include("**/*") })
    }
}

// =============================================================================
// Version Management Tasks
// =============================================================================

tasks.register("incrementVersion") {
    group = "versioning"
    description = "Increment project version by 0.0.1 (e.g., Alfa-0.1.1 -> Alfa-0.1.2)"

    doLast {
        val gradleProps = file("gradle.properties")
        if (!gradleProps.exists()) {
            throw GradleException("Файл gradle.properties не найден")
        }

        val content = gradleProps.readText()
        val versionRegex = Regex("""version=Alfa-(\d+)\.(\d+)\.(\d+)""")
        val match = versionRegex.find(content)

        if (match != null) {
            val major = match.groupValues[1].toInt()
            val minor = match.groupValues[2].toInt()
            val patch = match.groupValues[3].toInt()

            val currentVersion = "Alfa-$major.$minor.$patch"
            val newPatch = patch + 1
            val newVersion = "Alfa-$major.$minor.$newPatch"

            val updatedContent = content.replace(
                versionRegex,
                "version=$newVersion"
            )

            gradleProps.writeText(updatedContent)

            println("\n==========================================")
            println("Версия успешно обновлена!")
            println("Текущая версия: $currentVersion → $newVersion")
            println("==========================================")
        } else {
            throw GradleException("Не удалось найти версию в формате Alfa-X.Y.Z в gradle.properties")
        }
    }
}

tasks.register("getVersion") {
    group = "versioning"
    description = "Get current project version"

    doLast {
        val gradleProps = file("gradle.properties")
        if (!gradleProps.exists()) {
            throw GradleException("Файл gradle.properties не найден")
        }

        val content = gradleProps.readText()
        val versionRegex = Regex("""version=(.+)""")
        val match = versionRegex.find(content)

        if (match != null) {
            val version = match.groupValues[1].trim()
            println("Текущая версия проекта: $version")
        } else {
            throw GradleException("Не удалось найти версию в gradle.properties")
        }
    }
}

// =============================================================================
// Documentation Management Tasks
// =============================================================================

tasks.register("updateDocumentationVersion") {
    group = "documentation"
    description = "Update documentation version by incrementing version number by 1"

    doLast {
        val docsDir = file("docs")
        val mdFiles = fileTree(docsDir) {
            include("**/*.md")
            exclude("**/archive/**")
            exclude("**/node_modules/**")
        }

        val dateFormatter = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru", "RU"))
        val currentDate = dateFormatter.format(java.util.Date())
        val dateShort = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

        var updatedCount = 0

        mdFiles.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            var updatedContent = content
            var wasUpdated = false

            // Паттерн для версии: **Версия документации:** X.Y или X.Y.Z
            val versionPattern = Regex("""\*\*Версия документации:\*\* (\d+)\.(\d+)(\.(\d+))?""")
            val versionMatch = versionPattern.find(content)

            if (versionMatch != null) {
                val major = versionMatch.groupValues[1].toInt()
                val minor = versionMatch.groupValues[2].toInt()
                val patch = versionMatch.groupValues.getOrNull(4)?.toInt() ?: 0

                // Увеличиваем версию на 1 (увеличиваем минорную версию)
                val newMinor = minor + 1
                val newVersion = if (patch > 0) {
                    "$major.$minor.${patch + 1}"
                } else {
                    "$major.$newMinor"
                }

                updatedContent = versionPattern.replace(updatedContent) {
                    "**Версия документации:** $newVersion"
                }
                wasUpdated = true

                println("✓ ${file.name}: версия обновлена на $newVersion")
            } else {
                // Если версии нет, добавляем версию 1.0
                val firstHeaderPattern = Regex("""^(# .+?)\n""")
                val firstHeaderMatch = firstHeaderPattern.find(content)
                if (firstHeaderMatch != null) {
                    updatedContent = content.replaceFirst(
                        firstHeaderPattern,
                        "${firstHeaderMatch.value}\n**Версия документации:** 1.0\n"
                    )
                    wasUpdated = true
                    println("✓ ${file.name}: добавлена версия 1.0")
                }
            }

            // Обновление даты последнего обновления
            val datePattern = Regex("""\*\*Дата последнего обновления:\*\* .+""")
            if (datePattern.containsMatchIn(updatedContent)) {
                updatedContent = datePattern.replace(updatedContent) {
                    "**Дата последнего обновления:** $currentDate"
                }
            } else {
                // Добавляем дату после версии
                val versionLinePattern = Regex("""(\*\*Версия документации:\*\* .+)\n""")
                updatedContent = versionLinePattern.replace(updatedContent) {
                    "${it.value}**Дата последнего обновления:** $currentDate\n"
                }
            }

            // Обновление даты создания (если её нет)
            val creationDatePattern = Regex("""\*\*Дата создания:\*\* .+""")
            if (!creationDatePattern.containsMatchIn(updatedContent)) {
                val versionLinePattern = Regex("""(\*\*Версия документации:\*\* .+)\n""")
                updatedContent = versionLinePattern.replace(updatedContent) {
                    "${it.value}**Дата создания:** $currentDate\n"
                }
            }

            if (wasUpdated || updatedContent != content) {
                file.writeText(updatedContent, Charsets.UTF_8)
                updatedCount++
            }
        }

        println("\n==========================================")
        println("Обновление документации завершено!")
        println("Обновлено документов: $updatedCount")
        println("Дата обновления: $currentDate")
        println("==========================================")
    }
}

tasks.register("updateDocumentationDate") {
    group = "documentation"
    description = "Update documentation dates to current system date"

    doLast {
        val docsDir = file("docs")
        val mdFiles = fileTree(docsDir) {
            include("**/*.md")
            exclude("**/archive/**")
            exclude("**/node_modules/**")
        }

        val dateFormatter = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru", "RU"))
        val currentDate = dateFormatter.format(java.util.Date())

        var updatedCount = 0

        mdFiles.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            var updatedContent = content

            // Обновление даты последнего обновления
            val datePattern = Regex("""\*\*Дата последнего обновления:\*\* .+""")
            if (datePattern.containsMatchIn(content)) {
                updatedContent = datePattern.replace(updatedContent) {
                    "**Дата последнего обновления:** $currentDate"
                }
                file.writeText(updatedContent, Charsets.UTF_8)
                updatedCount++
            }
        }

        println("\n==========================================")
        println("Обновление дат завершено!")
        println("Обновлено документов: $updatedCount")
        println("Текущая дата: $currentDate")
        println("==========================================")
    }
}

tasks.register("incrementDocumentVersion") {
    group = "documentation"
    description = "Increment version of a specific document by 1"

    doLast {
        val documentPath = project.findProperty("document") as String?
            ?: throw GradleException("Укажите путь к документу: -Pdocument=docs/FILE.md")

        val documentFile = file(documentPath)
        if (!documentFile.exists()) {
            throw GradleException("Документ не найден: $documentPath")
        }

        val dateFormatter = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru", "RU"))
        val currentDate = dateFormatter.format(java.util.Date())
        val dateShort = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

        var content = documentFile.readText(Charsets.UTF_8)

        // Поиск текущей версии
        val versionPattern = Regex("""\*\*Версия документации:\*\* (\d+)\.(\d+)(\.(\d+))?""")
        val versionMatch = versionPattern.find(content)

        if (versionMatch != null) {
            val major = versionMatch.groupValues[1].toInt()
            val minor = versionMatch.groupValues[2].toInt()
            val patch = versionMatch.groupValues.getOrNull(4)?.toInt() ?: 0
            val oldVersion = if (patch > 0) "$major.$minor.$patch" else "$major.$minor"

            // Увеличиваем версию на 1
            val newMinor = minor + 1
            val newVersion = if (patch > 0) {
                "$major.$minor.${patch + 1}"
            } else {
                "$major.$newMinor"
            }

            content = versionPattern.replace(content) {
                "**Версия документации:** $newVersion"
            }

            // Обновление даты
            val datePattern = Regex("""\*\*Дата последнего обновления:\*\* .+""")
            if (datePattern.containsMatchIn(content)) {
                content = datePattern.replace(content) {
                    "**Дата последнего обновления:** $currentDate"
                }
            } else {
                val versionLinePattern = Regex("""(\*\*Версия документации:\*\* .+)\n""")
                content = versionLinePattern.replace(content) {
                    "${it.value}**Дата последнего обновления:** $currentDate\n"
                }
            }

            // Добавление информации о предыдущей версии
            val previousVersionPattern = Regex("""\*\*Предыдущая версия:\*\* .+""")
            if (!previousVersionPattern.containsMatchIn(content)) {
                val dateLinePattern = Regex("""(\*\*Дата последнего обновления:\*\* .+)\n""")
                content = dateLinePattern.replace(content) {
                    "${it.value}**Предыдущая версия:** $oldVersion (архивирована: $currentDate)\n"
                }
            }

            documentFile.writeText(content, Charsets.UTF_8)

            println("\n==========================================")
            println("Версия документа обновлена!")
            println("Документ: ${documentFile.name}")
            println("Версия: $oldVersion → $newVersion")
            println("Дата: $currentDate")
            println("==========================================")
        } else {
            throw GradleException("В документе не найдена версия. Добавьте строку: **Версия документации:** 1.0")
        }
    }
}

// =============================================================================
// Dokka Configuration
// =============================================================================

// Применяем Dokka ко всем подпроектам с Kotlin кодом
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jetbrains.dokka")

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    // Включаем только публичные API
                    includeNonPublic.set(false)

                    // Настройки для документации
                    jdkVersion.set(11)

                    // Отчеты о недокументированных элементах
                    reportUndocumented.set(true)

                    // Пропускать пустые пакеты
                    skipEmptyPackages.set(true)

                    // Настройки для различных форматов
                    perPackageOption {
                        matchingRegex.set(".*")
                        reportUndocumented.set(true)
                    }
                }
            }

            // Директория вывода
            outputDirectory.set(file("${project.buildDir}/dokka"))
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jetbrains.dokka")

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includeNonPublic.set(false)
                    jdkVersion.set(11)
                    reportUndocumented.set(true)
                    skipEmptyPackages.set(true)
                }
            }

            outputDirectory.set(file("${project.buildDir}/dokka"))
        }
    }
}

// Задача для генерации всей документации с автоматическим обновлением версий
tasks.register("generateDocumentation") {
    group = "documentation"
    description = "Generate all documentation and update versions automatically"

    dependsOn("updateDocumentationDate")

    doLast {
        println("\n==========================================")
        println("Генерация документации")
        println("==========================================")
        println("Для генерации API документации используйте:")
        println("  ./gradlew dokkaHtml")
        println("  ./gradlew dokkaJavadoc")
        println("  ./gradlew dokkaGfm")
        println("\nДокументация будет сгенерирована в build/dokka/")
        println("==========================================")
    }
}

// Задача для обновления версий перед генерацией документации
tasks.register("prepareDocumentation") {
    group = "documentation"
    description = "Prepare documentation by updating versions and dates before generation"

    dependsOn("updateDocumentationVersion")

    doLast {
        println("\nДокументация подготовлена к генерации!")
        println("Используйте: ./gradlew generateDocumentation")
    }
}
