# Руководство по очистке старых веток

## Анализ веток

### Новые ветки (созданы):
- `develop/platform-sbc-arm`
- `develop/platform-server-x86_64`
- `develop/platform-nas-arm`
- `develop/platform-nas-x86_64`
- `develop/platform-client-desktop-x86_64`
- `develop/platform-client-desktop-arm`
- `develop/platform-client-android`
- `develop/platform-client-ios`
- `test/platform-sbc-arm`
- `test/platform-server-x86_64`
- `test/platform-nas-arm`
- `test/platform-nas-x86_64`
- `test/platform-client-desktop-x86_64`
- `test/platform-client-desktop-arm`
- `test/platform-client-android`
- `test/platform-client-ios`

### Старые ветки (можно удалить):

#### Локальные ветки:
- `dev/android` - заменена на `develop/platform-client-android`
- `dev/desktop` - заменена на `develop/platform-client-desktop-x86_64` и `develop/platform-client-desktop-arm`
- `dev/ios` - заменена на `develop/platform-client-ios`
- `develop` - удалена (блокировала создание новых веток)
- `test/android` - заменена на `test/platform-client-android`
- `test/desktop` - заменена на `test/platform-client-desktop-x86_64` и `test/platform-client-desktop-arm`
- `test/ios` - заменена на `test/platform-client-ios`
- `test/integration` - можно оставить для общих интеграционных тестов
- `feature/native-libraries` - можно удалить после мерджа изменений
- `feature/network-layer` - можно удалить после мерджа изменений
- `feature/server-part` - можно удалить после мерджа изменений
- `feature/ui-components` - можно удалить после мерджа изменений
- `commit` - можно удалить (похоже на временную ветку)

## Команды для удаления

### Локальные ветки:

```bash
# Удаление старых веток разработки
git branch -D dev/android
git branch -D dev/desktop
git branch -D dev/ios

# Удаление старых веток тестирования
git branch -D test/android
git branch -D test/desktop
git branch -D test/ios

# Удаление feature веток (после проверки что изменения мерджены)
git branch -D feature/native-libraries
git branch -D feature/network-layer
git branch -D feature/server-part
git branch -D feature/ui-components

# Удаление временных веток
git branch -D commit
```

### Удаленные ветки (GitHub):

```bash
# ВНИМАНИЕ: Удаление веток на GitHub требует прав доступа
# Выполняйте только после проверки что изменения не нужны

git push origin --delete dev/android
git push origin --delete dev/desktop
git push origin --delete dev/ios
git push origin --delete test/android
git push origin --delete test/desktop
git push origin --delete test/ios
git push origin --delete commit
```

## Рекомендации

1. **Перед удалением:** Убедитесь, что все важные изменения из старых веток мерджены в main или новые ветки платформ.

2. **Feature ветки:** Проверьте, что изменения из feature веток не потеряются. При необходимости мерджьте их в соответствующие платформенные ветки.

3. **test/integration:** Эту ветку можно оставить для общих интеграционных тестов, которые не привязаны к конкретной платформе.

4. **Резервное копирование:** Перед массовым удалением веток рекомендуется создать резервную копию репозитория.

## Скрипт автоматического удаления

Для Windows PowerShell (после проверки):

```powershell
$branchesToDelete = @(
    "dev/android",
    "dev/desktop",
    "dev/ios",
    "test/android",
    "test/desktop",
    "test/ios",
    "commit"
)

foreach ($branch in $branchesToDelete) {
    if (git show-ref --verify --quiet refs/heads/$branch) {
        Write-Host "Deleting local branch: $branch"
        git branch -D $branch
    }
}
```

Для Linux/Mac:

```bash
branches_to_delete=(
    "dev/android"
    "dev/desktop"
    "dev/ios"
    "test/android"
    "test/desktop"
    "test/ios"
    "commit"
)

for branch in "${branches_to_delete[@]}"; do
    if git show-ref --verify --quiet refs/heads/$branch; then
        echo "Deleting local branch: $branch"
        git branch -D $branch
    fi
done
```

## После удаления

После удаления старых веток новая структура будет выглядеть так:

```
main
├── develop/platform-sbc-arm
│   └── test/platform-sbc-arm
├── develop/platform-server-x86_64
│   └── test/platform-server-x86_64
├── develop/platform-nas-arm
│   └── test/platform-nas-arm
├── develop/platform-nas-x86_64
│   └── test/platform-nas-x86_64
├── develop/platform-client-desktop-x86_64
│   └── test/platform-client-desktop-x86_64
├── develop/platform-client-desktop-arm
│   └── test/platform-client-desktop-arm
├── develop/platform-client-android
│   └── test/platform-client-android
└── develop/platform-client-ios
    └── test/platform-client-ios
```


