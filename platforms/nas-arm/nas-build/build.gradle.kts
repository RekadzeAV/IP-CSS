import org.gradle.api.tasks.Exec

group = "com.company.ipcamera.platforms.nas"
version = rootProject.version

fun registerNasBuildTask(
    taskName: String,
    packageType: String,
    arch: String,
) {
    tasks.register<Exec>(taskName) {
        group = "nas"
        description = "Build $packageType package for $arch on nas-arm"

        val version = rootProject.version.toString()
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            val script = rootProject.file("scripts/build-nas-package.ps1").absolutePath
            commandLine(
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script,
                "-PackageType",
                packageType,
                "-Arch",
                arch,
                "-Version",
                version,
            )
        } else {
            val script = rootProject.file("scripts/build-nas-package.sh").absolutePath
            commandLine("bash", script, packageType, arch, version)
        }
        workingDir = rootProject.projectDir
    }
}

registerNasBuildTask("buildSynologySpk", "synology", "arm64")
registerNasBuildTask("buildQnapQpkg", "qnap", "arm64")
registerNasBuildTask("buildQnapQpkgArmv7", "qnap", "armv7")
registerNasBuildTask("buildAsustorApk", "asustor", "arm64")
registerNasBuildTask("buildAsustorApkRtd1296", "asustor", "rtd1296")
registerNasBuildTask("buildTruenas", "truenas", "arm64")

tasks.register("buildAllNasPackages") {
    group = "nas"
    description = "Build all NAS packages for arm variants"
    dependsOn(
        "buildSynologySpk",
        "buildQnapQpkg",
        "buildQnapQpkgArmv7",
        "buildAsustorApk",
        "buildAsustorApkRtd1296",
        "buildTruenas",
    )
}
