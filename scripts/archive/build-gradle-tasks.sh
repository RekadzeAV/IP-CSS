#!/bin/bash
# Gradle tasks для сборки IP-CSS
# Соответствует реальным tasks из build.gradle.kts

get_gradle_tasks() {
    local build_mode="${1:-full}"
    
    case "$build_mode" in
        "quick"|"fast")
            # Быстрая проверка - только базовые задачи
            echo "ciUnitSmokeSuite"
            ;;
        "native"|"kotlin"|"web"|"docker")
            # Поэтапная сборка
            case "$build_mode" in
                "native")
                    echo "buildNativeLibraries"
                    ;;
                "kotlin")
                    echo "ciBuildCoreSharedModules"
                    ;;
                "web")
                    echo "web_build"  # Будет реализовано отдельно
                    ;;
                "docker")
                    echo "docker_build"  # Будет реализовано отдельно
                    ;;
            esac
            ;;
        "clean")
            echo "clean cleanBuildCache"
            ;;
        "full"|*)
            # Полная сборка - все модули
            echo "ciBuildCoreSharedModules buildNativeLibraries :server:api:build"
            ;;
    esac
}

# Пример использования:
# GRADLE_TASKS=$(get_gradle_tasks "full")
# ./gradlew $GRADLE_TASKS --no-daemon
