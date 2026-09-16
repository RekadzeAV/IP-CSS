#!/usr/bin/env python3
"""
Check Dependency Conflicts

Проверяет отсутствие конфликтов зависимостей перед установкой новых модулей/библиотек.
Также проверяет наличие уже установленных версий в локальной среде.

Использование:
    python scripts/check-dependency-conflicts.py [--package <name>] [--list]
    python scripts/check-dependency-conflicts.py --help
"""

import subprocess
import sys
import json
import os
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
from datetime import datetime
import re

# Устанавливаем UTF-8 кодировку для Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# Цвета для вывода
class Colors:
    OK = '\033[92m'
    WARN = '\033[93m'
    FAIL = '\033[91m'
    END = '\033[0m'
    BOLD = '\033[1m'

@dataclass
class DependencyInfo:
    name: str
    version: str
    location: str
    is_global: bool

@dataclass
class ConflictCheck:
    package: str
    installed: bool
    versions: List[str]
    locations: List[str]
    has_conflict: bool
    message: str

class DependencyChecker:
    def __init__(self):
        self.python_packages: Dict[str, DependencyInfo] = {}
        self.npm_packages: Dict[str, DependencyInfo] = {}
        self.gradle_deps: Dict[str, Dict] = {}
    
    def run_command(self, cmd: List[str], timeout: int = 30) -> Tuple[bool, str]:
        """Выполняет команду и возвращает (успех, вывод)"""
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                shell=os.name == 'nt'
            )
            output = result.stdout.strip() or result.stderr.strip()
            return result.returncode == 0, output
        except subprocess.TimeoutExpired:
            return False, "Timeout expired"
        except FileNotFoundError:
            return False, "Command not found"
        except Exception as e:
            return False, str(e)
    
    def check_python_packages(self, package_name: Optional[str] = None) -> List[ConflictCheck]:
        """Проверяет Python пакеты"""
        checks = []
        
        # Получаем список установленных пакетов
        success, output = self.run_command(["pip", "list", "--format=json"])
        if not success:
            return checks
        
        try:
            packages = json.loads(output)
            for pkg in packages:
                name = pkg['name'].lower()
                version = pkg['version']
                
                if package_name and package_name.lower() != name:
                    continue
                
                # Проверяем локальные виртуальные окружения
                venv_path = os.path.join(os.getcwd(), "venv", "Lib", "site-packages") if os.name == 'nt' else os.path.join(os.getcwd(), "venv", "lib", "python*")
                is_global = not os.path.exists(venv_path)
                
                dep_info = DependencyInfo(
                    name=name,
                    version=version,
                    location="global" if is_global else "venv",
                    is_global=is_global
                )
                self.python_packages[name] = dep_info
                
                checks.append(ConflictCheck(
                    package=name,
                    installed=True,
                    versions=[version],
                    locations=[dep_info.location],
                    has_conflict=False,
                    message=f"✅ {name} v{version} (global)" if is_global else f"ℹ️ {name} v{version} (venv)"
                ))
        except json.JSONDecodeError:
            pass
        
        return checks
    
    def check_npm_packages(self, package_name: Optional[str] = None) -> List[ConflictCheck]:
        """Проверяет Node.js пакеты"""
        checks = []
        
        # Глобальные пакеты
        success, output = self.run_command(["npm", "list", "-g", "--json"])
        if success:
            try:
                data = json.loads(output)
                if isinstance(data, dict) and 'dependencies' in data:
                    for name, info in data['dependencies'].items():
                        if package_name and package_name.lower() != name.lower():
                            continue
                        
                        version = info.get('version', 'unknown')
                        self.npm_packages[name.lower()] = DependencyInfo(
                            name=name,
                            version=version,
                            location="global",
                            is_global=True
                        )
                        
                        checks.append(ConflictCheck(
                            package=name,
                            installed=True,
                            versions=[version],
                            locations=["global"],
                            has_conflict=False,
                            message=f"✅ {name} v{version} (global)"
                        ))
            except json.JSONDecodeError:
                pass
        
        # Локальные пакеты
        success, output = self.run_command(["npm", "list", "--json"])
        if success:
            try:
                data = json.loads(output)
                if isinstance(data, dict) and 'dependencies' in data:
                    for name, info in data['dependencies'].items():
                        if package_name and package_name.lower() != name.lower():
                            continue
                        
                        version = info.get('version', 'unknown')
                        self.npm_packages[name.lower()] = DependencyInfo(
                            name=name,
                            version=version,
                            location="local",
                            is_global=False
                        )
                        
                        # Проверяем, есть ли уже глобальная версия
                        global_version = None
                        if name.lower() in self.npm_packages:
                            global_version = self.npm_packages[name.lower()].version
                        
                        if global_version:
                            checks.append(ConflictCheck(
                                package=name,
                                installed=True,
                                versions=[version, global_version],
                                locations=["local", "global"],
                                has_conflict=True,
                                message=f"⚠️ {name} v{version} (local) и v{global_version} (global) - РАЗНЫЕ ВЕРСИИ"
                            ))
                        else:
                            checks.append(ConflictCheck(
                                package=name,
                                installed=True,
                                versions=[version],
                                locations=["local"],
                                has_conflict=False,
                                message=f"ℹ️ {name} v{version} (local)"
                            ))
            except json.JSONDecodeError:
                pass
        
        return checks
    
    def check_gradle_dependencies(self) -> Dict[str, Dict]:
        """Проверяет Gradle зависимости"""
        success, output = self.run_command(["./gradlew" if os.name != 'nt' else "gradlew.bat", "dependencies", "--configuration", "implementation"])
        if success:
            # Парсим вывод Gradle (упрощённо)
            lines = output.split('\n')
            for line in lines:
                if '+' in line or '\\' in line:
                    # Извлекаем имя и версию зависимости
                    match = re.search(r'([a-zA-Z0-9._-]+)\s+\+\s*$', line)
                    if match:
                        dep_name = match.group(1)
                        self.gradle_deps[dep_name] = {"version": "latest", "line": line}
        
        return self.gradle_deps
    
    def check_package(self, package_name: str) -> ConflictCheck:
        """Проверяет конкретный пакет на наличие конфликтов"""
        all_versions = []
        all_locations = []
        
        # Проверка Python
        success, output = self.run_command(["pip", "show", package_name])
        if success:
            version = None
            location = None
            for line in output.split('\n'):
                if line.startswith('Version:'):
                    version = line.split(':')[1].strip()
                if line.startswith('Location:'):
                    location = line.split(':')[1].strip()
            
            if version:
                all_versions.append(version)
                all_locations.append(location or "unknown")
        
        # Проверка npm
        success, output = self.run_command(["npm", "list", "-g", package_name])
        if success and package_name in output:
            version_match = re.search(r'(\d+\.\d+\.\d+)', output)
            if version_match:
                all_versions.append(version_match.group(1))
                all_locations.append("npm global")
        
        # Проверка локального npm
        success, output = self.run_command(["npm", "list", package_name])
        if success and package_name in output:
            version_match = re.search(r'(\d+\.\d+\.\d+)', output)
            if version_match:
                if version_match.group(1) not in all_versions:
                    all_versions.append(version_match.group(1))
                if "npm local" not in all_locations:
                    all_locations.append("npm local")
        
        has_conflict = len(all_versions) > 1
        installed = len(all_versions) > 0
        
        if not installed:
            message = f"✅ {package_name} не установлен (можно устанавливать)"
        elif has_conflict:
            message = f"⚠️ {package_name} имеет несколько версий: {', '.join(all_versions)}"
        else:
            message = f"ℹ️ {package_name} v{all_versions[0]} установлен в {all_locations[0]}"
        
        return ConflictCheck(
            package=package_name,
            installed=installed,
            versions=all_versions,
            locations=all_locations,
            has_conflict=has_conflict,
            message=message
        )
    
    def run_all_checks(self, package_name: Optional[str] = None) -> Tuple[List[ConflictCheck], List[str], List[str]]:
        """Запускает все проверки"""
        all_checks = []
        errors = []
        warnings = []
        
        print(f"{Colors.BOLD}Проверка зависимостей проекта IP-CSS{Colors.END}")
        print("=" * 60)
        print()
        
        if package_name:
            # Проверка конкретного пакета
            print(f"{Colors.BOLD}Проверка пакета: {package_name}{Colors.END}")
            print("-" * 60)
            check = self.check_package(package_name)
            all_checks.append(check)
            print(check.message)
            
            if check.has_conflict:
                warnings.append(f"Конфликт версий для {package_name}: {', '.join(check.versions)}")
        else:
            # Проверка всех пакетов
            print(f"{Colors.BOLD}Python пакеты{Colors.END}")
            print("-" * 60)
            python_checks = self.check_python_packages()
            all_checks.extend(python_checks)
            for check in python_checks[:10]:  # Показать первые 10
                print(check.message)
            if len(python_checks) > 10:
                print(f"  ... и ещё {len(python_checks) - 10} пакетов")
            
            print()
            print(f"{Colors.BOLD}NPM пакеты{Colors.END}")
            print("-" * 60)
            npm_checks = self.check_npm_packages()
            all_checks.extend(npm_checks)
            for check in npm_checks[:10]:
                print(check.message)
            if len(npm_checks) > 10:
                print(f"  ... и ещё {len(npm_checks) - 10} пакетов")
            
            print()
            print(f"{Colors.BOLD}Gradle зависимости{Colors.END}")
            print("-" * 60)
            self.check_gradle_dependencies()
            print(f"Найдено {len(self.gradle_deps)} зависимостей")
        
        # Поиск конфликтов
        conflicts = [c for c in all_checks if c.has_conflict]
        if conflicts:
            print()
            print(f"{Colors.FAIL}{Colors.BOLD}Обнаружены конфликты:{Colors.END}")
            for conflict in conflicts:
                print(f"  {conflict.message}")
                warnings.append(f"Конфликт: {conflict.package}")
        
        print()
        print("=" * 60)
        print(f"Всего проверено пакетов: {len(all_checks)}")
        print(f"Конфликтов: {len(conflicts)}")
        
        if conflicts:
            print(f"{Colors.FAIL}⚠️ Найдены конфликты зависимостей!{Colors.END}")
        else:
            print(f"{Colors.OK}✅ Конфликтов не найдено{Colors.END}")
        
        return all_checks, errors, warnings

def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Проверка конфликтов зависимостей для проекта IP-CSS"
    )
    parser.add_argument(
        "--package", "-p",
        type=str,
        help="Проверить конкретный пакет"
    )
    parser.add_argument(
        "--list", "-l",
        action="store_true",
        help="Показать все установленные пакеты"
    )
    parser.add_argument(
        "--report", "-r",
        type=str,
        help="Путь для сохранения JSON отчёта"
    )
    
    args = parser.parse_args()
    
    checker = DependencyChecker()
    checks, errors, warnings = checker.run_all_checks(package_name=args.package)
    
    # Сохранение отчёта
    if args.report:
        os.makedirs(os.path.dirname(args.report), exist_ok=True)
        report = {
            "timestamp": datetime.now().isoformat(),
            "package_checked": args.package,
            "checks": [asdict(c) for c in checks],
            "errors": errors,
            "warnings": warnings
        }
        with open(args.report, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        print(f"\nОтчёт сохранён: {args.report}")
    
    # Возврат кода
    has_conflicts = any(c.has_conflict for c in checks)
    sys.exit(1 if has_conflicts else 0)

if __name__ == "__main__":
    main()
