import org.gradle.api.tasks.Exec

group = "com.company.ipcamera.platforms.nas"
version = rootProject.version

fun registerNasBuildTask(
    taskName: String,
    packageType: String,
    arch: String = "x86_64",
) {
    tasks.register<Exec>(taskName) {
        group = "nas"
        description = "Build $packageType package for $arch on nas-x86_64"

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

registerNasBuildTask("buildSynologySpk", "synology", "x86_64")
registerNasBuildTask("buildQnapQpkg", "qnap", "x86_64")
registerNasBuildTask("buildAsustorApk", "asustor", "x86_64")
registerNasBuildTask("buildTruenas", "truenas", "x86_64")

tasks.register("buildAllNasPackages") {
    group = "nas"
    description = "Build all NAS packages for x86_64"
    dependsOn("buildSynologySpk", "buildQnapQpkg", "buildAsustorApk", "buildTruenas")
}
