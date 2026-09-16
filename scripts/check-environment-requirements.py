#!/usr/bin/env python3
"""
Check Environment Requirements

Проверяет наличие всех необходимых компонентов, библиотек и инструментов
для сборки и тестирования проекта IP-CSS.

Использование:
    python scripts/check-environment-requirements.py [--verbose] [--quick] [--report <path>]
    python scripts/check-environment-requirements.py --help
"""

import subprocess
import sys
import json
import os
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, asdict
from datetime import datetime

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
class RequirementCheck:
    name: str
    required: bool
    installed: bool
    version: Optional[str]
    path: Optional[str]
    message: str

@dataclass
class EnvironmentReport:
    timestamp: str
    platform: str
    checks: List[Dict]
    summary: Dict
    errors: List[str]
    warnings: List[str]

class EnvironmentChecker:
    def __init__(self, verbose: bool = False, quick: bool = False):
        self.verbose = verbose
        self.quick = quick
        self.checks: List[RequirementCheck] = []
        self.errors: List[str] = []
        self.warnings: List[str] = []
    
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
    
    def check_tool(self, name: str, check_cmd: List[str], version_cmd: List[str], required: bool) -> RequirementCheck:
        """Проверяет наличие инструмента"""
        # Проверка наличия
        success, output = self.run_command(check_cmd)
        
        if not success:
            check = RequirementCheck(
                name=name,
                required=required,
                installed=False,
                version=None,
                path=None,
                message=f"❌ {name} не найден"
            )
            if required:
                self.errors.append(f"{name} требуется для сборки проекта")
            else:
                self.warnings.append(f"{name} не найден (опционально)")
            return check
        
        # Получение версии
        version_success, version_output = self.run_command(version_cmd)
        version = version_output if version_success else "unknown"
        
        # Получение пути
        path_success, path_output = self.run_command(["which", name] if os.name != 'nt' else ["where", name])
        path = path_output if path_success else None
        
        check = RequirementCheck(
            name=name,
            required=required,
            installed=True,
            version=version.split('\n')[0][:100],
            path=path,
            message=f"✅ {name} установлен: {version.split(chr(10))[0]}"
        )
        
        if self.verbose:
            if path:
                check.message += f" (путь: {path})"
        
        return check
    
    def check_python_packages(self) -> List[RequirementCheck]:
        """Проверяет Python пакеты"""
        packages = [
            ("python", ["python", "--version"], ["python", "--version"], True),
            ("pip", ["pip", "--version"], ["pip", "--version"], True),
        ]
        
        if not self.quick:
            packages.extend([
                ("pytest", ["python", "-m", "pytest", "--version"], ["python", "-m", "pytest", "--version"], False),
            ])
        
        checks = []
        for name, check_cmd, version_cmd, required in packages:
            checks.append(self.check_tool(name, check_cmd, version_cmd, required))
        
        return checks
    
    def check_java(self) -> List[RequirementCheck]:
        """Проверяет Java/JDK"""
        checks = []
        
        # Java
        success, output = self.run_command(["java", "-version"])
        # java выводит версию в stderr
        if not success:
            success, output = self.run_command(["java", "-version"])
        
        if success:
            # Парсим версию
            version_line = output.split('\n')[0] if output else "unknown"
            checks.append(RequirementCheck(
                name="Java",
                required=True,
                installed=True,
                version=version_line,
                path=None,
                message=f"✅ Java установлен: {version_line}"
            ))
            
            # Проверка версии (требуется JDK 17+)
            try:
                version_num = int(version_line.split(' ')[1].split('.')[0].replace('"', ''))
                if version_num < 17:
                    checks[-1].message = f"⚠️ Java версия {version_num} слишком старая (требуется 17+)"
                    self.warnings.append(f"Java версия {version_num} ниже требуемой 17+")
            except (ValueError, IndexError):
                pass
        else:
            checks.append(RequirementCheck(
                name="Java",
                required=True,
                installed=False,
                version=None,
                path=None,
                message="❌ Java не найден"
            ))
            self.errors.append("Java (JDK 17+) требуется для сборки проекта")
        
        return checks
    
    def check_gradle(self) -> List[RequirementCheck]:
        """Проверяет Gradle"""
        checks = []
        
        # Проверка Gradle wrapper
        wrapper_exists = os.path.exists("gradlew") or os.path.exists("gradlew.bat")
        
        if wrapper_exists:
            success, output = self.run_command(["./gradlew" if os.name != 'nt' else "gradlew.bat", "--version"])
            if success:
                version_line = output.split('\n')[0] if output else "unknown"
                checks.append(RequirementCheck(
                    name="Gradle Wrapper",
                    required=True,
                    installed=True,
                    version=version_line,
                    path="./gradlew",
                    message=f"✅ Gradle Wrapper найден: {version_line}"
                ))
            else:
                checks.append(RequirementCheck(
                    name="Gradle Wrapper",
                    required=True,
                    installed=False,
                    version=None,
                    path="./gradlew",
                    message="❌ Gradle Wrapper не работает"
                ))
                self.errors.append("Gradle Wrapper не работает")
        else:
            checks.append(RequirementCheck(
                name="Gradle Wrapper",
                required=True,
                installed=False,
                version=None,
                path="./gradlew",
                message="❌ Gradle Wrapper не найден (файл gradlew отсутствует)"
            ))
            self.errors.append("Gradle Wrapper не найден")
        
        return checks
    
    def check_nodejs(self) -> List[RequirementCheck]:
        """Проверяет Node.js"""
        checks = []
        
        # Node.js
        checks.append(self.check_tool("node", ["node", "--version"], ["node", "--version"], True))
        
        # npm
        checks.append(self.check_tool("npm", ["npm", "--version"], ["npm", "--version"], True))
        
        return checks
    
    def check_docker(self) -> List[RequirementCheck]:
        """Проверяет Docker"""
        checks = []
        
        # Docker
        checks.append(self.check_tool("docker", ["docker", "--version"], ["docker", "--version"], False))
        
        # Docker Compose
        checks.append(self.check_tool("docker-compose", ["docker-compose", "--version"], ["docker-compose", "--version"], False))
        
        return checks
    
    def check_system_resources(self) -> List[RequirementCheck]:
        """Проверяет системные ресурсы"""
        checks = []
        
        # RAM
        try:
            if os.name != 'nt':
                result = subprocess.run(["free", "-h"], capture_output=True, text=True)
                lines = result.stdout.split('\n')
                if len(lines) > 1:
                    parts = lines[1].split()
                    if len(parts) > 1:
                        ram_str = parts[1]
                        ram_gb = float(ram_str.replace('G', '').replace('M', '000'))
                        if 'M' in ram_str:
                            ram_gb /= 1000
                        
                        if ram_gb >= 8:
                            checks.append(RequirementCheck(
                                name="RAM",
                                required=True,
                                installed=True,
                                version=f"{ram_gb:.1f}GB",
                                path=None,
                                message=f"✅ RAM: {ram_gb:.1f}GB (достаточно)"
                            ))
                        else:
                            checks.append(RequirementCheck(
                                name="RAM",
                                required=True,
                                installed=True,
                                version=f"{ram_gb:.1f}GB",
                                path=None,
                                message=f"⚠️ RAM: {ram_gb:.1f}GB (рекомендуется 8GB+)"
                            ))
                            self.warnings.append(f"Мало RAM: {ram_gb:.1f}GB (рекомендуется 8GB+)")
            else:
                # Windows
                checks.append(RequirementCheck(
                    name="RAM",
                    required=True,
                    installed=True,
                    version="N/A",
                    path=None,
                    message="ℹ️ RAM: проверка недоступна на Windows"
                ))
        except Exception:
            checks.append(RequirementCheck(
                name="RAM",
                required=True,
                installed=False,
                version=None,
                path=None,
                message="⚠️ Не удалось проверить RAM"
            ))
        
        # Disk Space
        try:
            if os.name != 'nt':
                result = subprocess.run(["df", "-h", "."], capture_output=True, text=True)
                lines = result.stdout.split('\n')
                if len(lines) > 1:
                    parts = lines[1].split()
                    if len(parts) > 3:
                        free_str = parts[3]
                        free_gb = float(free_str.replace('G', '').replace('M', '000'))
                        if 'M' in free_str:
                            free_gb /= 1000
                        
                        if free_gb >= 50:
                            checks.append(RequirementCheck(
                                name="Disk Space",
                                required=True,
                                installed=True,
                                version=f"{free_gb:.1f}GB",
                                path=None,
                                message=f"✅ Disk Space: {free_gb:.1f}GB свободно (достаточно)"
                            ))
                        else:
                            checks.append(RequirementCheck(
                                name="Disk Space",
                                required=True,
                                installed=True,
                                version=f"{free_gb:.1f}GB",
                                path=None,
                                message=f"⚠️ Disk Space: {free_gb:.1f}GB свободно (рекомендуется 50GB+)"
                            ))
                            self.warnings.append(f"Мало свободного места: {free_gb:.1f}GB (рекомендуется 50GB+)")
            else:
                # Windows
                checks.append(RequirementCheck(
                    name="Disk Space",
                    required=True,
                    installed=True,
                    version="N/A",
                    path=None,
                    message="ℹ️ Disk Space: проверка недоступна на Windows"
                ))
        except Exception:
            checks.append(RequirementCheck(
                name="Disk Space",
                required=True,
                installed=False,
                version=None,
                path=None,
                message="⚠️ Не удалось проверить Disk Space"
            ))
        
        return checks
    
    def run_all_checks(self) -> EnvironmentReport:
        """Запускает все проверки"""
        all_checks = []
        
        print(f"{Colors.BOLD}Проверка окружения проекта IP-CSS{Colors.END}")
        print("=" * 60)
        print()
        
        # Python
        print(f"{Colors.BOLD}Python и скрипты{Colors.END}")
        all_checks.extend(self.check_python_packages())
        
        # Java
        print(f"{Colors.BOLD}Java и Gradle{Colors.END}")
        all_checks.extend(self.check_java())
        all_checks.extend(self.check_gradle())
        
        # Node.js (опционально)
        if not self.quick:
            print(f"{Colors.BOLD}Node.js и npm{Colors.END}")
            all_checks.extend(self.check_nodejs())
        
        # Docker (опционально)
        if not self.quick:
            print(f"{Colors.BOLD}Docker{Colors.END}")
            all_checks.extend(self.check_docker())
        
        # Системные ресурсы
        print(f"{Colors.BOLD}Системные ресурсы{Colors.END}")
        all_checks.extend(self.check_system_resources())
        
        self.checks = all_checks
        
        # Подсчёт результатов
        total = len(all_checks)
        passed = sum(1 for c in all_checks if c.installed)
        required_total = sum(1 for c in all_checks if c.required)
        required_passed = sum(1 for c in all_checks if c.required and c.installed)
        
        summary = {
            "total_checks": total,
            "passed": passed,
            "failed": total - passed,
            "required_passed": required_passed,
            "required_total": required_total,
            "all_required_passed": required_passed == required_total
        }
        
        # Вывод результатов
        print()
        print(f"{Colors.BOLD}Результаты:{Colors.END}")
        print("-" * 60)
        
        for check in all_checks:
            print(check.message)
            if self.verbose and check.path:
                print(f"  Путь: {check.path}")
        
        print()
        print("=" * 60)
        print(f"Всего проверок: {total}")
        print(f"Пройдено: {passed}")
        print(f"Не пройдено: {total - passed}")
        print(f"Обязательные: {required_passed}/{required_total}")
        
        if self.errors:
            print()
            print(f"{Colors.FAIL}{Colors.BOLD}Ошибки:{Colors.END}")
            for error in self.errors:
                print(f"  {error}")
        
        if self.warnings:
            print()
            print(f"{Colors.WARN}{Colors.BOLD}Предупреждения:{Colors.END}")
            for warning in self.warnings:
                print(f"  {warning}")
        
        print()
        
        if summary["all_required_passed"]:
            print(f"{Colors.OK}{Colors.BOLD}✅ Все обязательные проверки пройдены!{Colors.END}")
        else:
            print(f"{Colors.FAIL}{Colors.BOLD}❌ Некоторые обязательные проверки не пройдены!{Colors.END}")
        
        # Генерация отчёта
        report = EnvironmentReport(
            timestamp=datetime.now().isoformat(),
            platform=sys.platform,
            checks=[asdict(c) for c in all_checks],
            summary=summary,
            errors=self.errors,
            warnings=self.warnings
        )
        
        return report

def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Проверка окружения для сборки и тестирования проекта IP-CSS"
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Подробный вывод"
    )
    parser.add_argument(
        "--quick", "-q",
        action="store_true",
        help="Быстрая проверка (только критические компоненты)"
    )
    parser.add_argument(
        "--report", "-r",
        type=str,
        help="Путь для сохранения JSON отчёта"
    )
    
    args = parser.parse_args()
    
    checker = EnvironmentChecker(verbose=args.verbose, quick=args.quick)
    report = checker.run_all_checks()
    
    # Сохранение отчёта
    if args.report:
        os.makedirs(os.path.dirname(args.report), exist_ok=True)
        with open(args.report, 'w', encoding='utf-8') as f:
            json.dump(asdict(report), f, indent=2, ensure_ascii=False)
        print(f"\nОтчёт сохранён: {args.report}")
    
    # Возврат кода
    sys.exit(0 if report.summary["all_required_passed"] else 1)

if __name__ == "__main__":
    main()
