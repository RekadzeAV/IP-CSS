#!/bin/bash
# Gradle задачи для сборки IP-CSS
# Соответствует реальным tasks из build.gradle.kts

get_gradle_tasks() {
    local build_mode="${1:-full}"
    
    case "$build_mode" in
        "quick")
            echo "ciUnitSmokeSuite"
            ;;
        "native")
            echo "buildNativeLibraries"
            ;;
        "kotlin")
            echo "ciBuildCoreSharedModules"
            ;;
        "full"|*)
            echo "ciBuildCoreSharedModules buildNativeLibraries :server:api:build"
            ;;
    esac
}
