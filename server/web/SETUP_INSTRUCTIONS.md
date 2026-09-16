# Инструкция по установке зависимостей для Web UI

## Проблема

В файле `VideoPlayer.tsx` есть ошибки TypeScript, связанные с отсутствием модулей:
- `react`
- `@mui/material`
- `@mui/icons-material`
- `hls.js`

Эти ошибки возникают, потому что зависимости не установлены (отсутствует директория `node_modules`).

## Решение

### 1. Установка Node.js (если не установлен)

**Для macOS:**

Используя Homebrew:
```bash
# Установка Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка Node.js
brew install node
```

Или скачайте установщик с официального сайта:
- https://nodejs.org/
- Рекомендуется версия LTS (Long Term Support)

**Проверка установки:**
```bash
node --version   # Должно показать версию (например, v20.x.x)
npm --version    # Должно показать версию npm (например, 10.x.x)
```

### 2. Установка зависимостей проекта

Перейдите в директорию веб-интерфейса и установите зависимости:

```bash
cd server/web
npm install
```

Это команда:
- Прочитает файл `package.json`
- Установит все зависимости, указанные в `dependencies` и `devDependencies`
- Создаст директорию `node_modules` со всеми необходимыми модулями
- Создаст файл `package-lock.json` для фиксации версий

### 3. Проверка установки

После установки проверьте, что все зависимости установлены:

```bash
# Проверка наличия node_modules
ls -la node_modules | head -10

# Проверка конкретных модулей
ls node_modules/react
ls node_modules/@mui/material
ls node_modules/hls.js
```

### 4. Перезагрузка IDE

После установки зависимостей:
1. Перезапустите IDE (Cursor/VSCode)
2. Или перезагрузите окно TypeScript сервера в IDE

Ошибки TypeScript должны исчезнуть после того, как TypeScript найдет установленные модули.

## Альтернативные менеджеры пакетов

Вместо `npm` можно использовать:

**Yarn:**
```bash
yarn install
```

**pnpm:**
```bash
pnpm install
```

## Требования

Согласно `package.json`, требуется:
- Node.js >= 20.0.0
- npm >= 10.0.0

Проверьте версии:
```bash
node --version
npm --version
```

## Дополнительная информация

- Все зависимости определены в файле `server/web/package.json`
- После установки зависимостей можно запустить проект: `npm run dev`
- См. `server/web/README.md` для подробной информации о проекте

