# Phase 1.5 Task Priority Plan

**Дата:** 2026-01-27  
**Фаза:** 1.5 (Завершение Phase 1)  
**Общий прогресс:** 90% → Цель 100%

---

## 📋 Task Priority Matrix

### Priority Legend
- **P0 - CRITICAL:** Блокирует релиз/продакшен
- **P1 - HIGH:** Высокий приоритет, должен быть сделан в Phase 1.5
- **P2 - MEDIUM:** Важный, но не блокирующий
- **P3 - LOW:** Опциональный, можно отложить

| ID | Задача | Приоритет | Сложность | Влияние | Статус |
|----|--------|-----------|-----------|---------|--------|
| **P0 - CRITICAL** |
| 1.5.1 | Исправить C++ compilation в native/video-processing | P0 | Высокая | Блокирует KMP compile | 🔴 OPEN |
| **P1 - HIGH** |
| 1.5.2 | Улучшить signature verification скрипт | P1 | Средняя | Улучшает KMP проверку | 🟡 OPEN |
| 1.5.3 | Добавить contract tests для LocalDataEncryption | P1 | Средняя | Улучшает coverage | 🟡 OPEN |
| 1.5.4 | Добавить contract tests для PasswordEncryption | P1 | Средняя | Улучшает coverage | 🟡 OPEN |
| 1.5.5 | Добавить platform smoke tests для iOS/Native | P1 | Высокая | Улучшает coverage | 🟡 OPEN |
| **P2 - MEDIUM** |
| 1.5.6 | Обновить docs/README.md (KMP ссылки) | P2 | Низкая | Улучшает документацию | 🟡 OPEN |
| 1.5.7 | Обновить PROJECT_PROMPT.md (KMP ссылки) | P2 | Низкая | Улучшает документацию | 🟡 OPEN |
| 1.5.8 | Обновить PROJECT_STRUCTURE.md (KMP ссылки) | P2 | Низкая | Улучшает документацию | 🟡 OPEN |
| 1.5.9 | Интегрировать signature report в GitHub Job Summary | P2 | Средняя | Улучшает CI | 🟡 OPEN |
| 1.5.10 | Оптимизировать KMP cross-compile matrix | P2 | Высокая | Уменьшает время CI | 🟡 OPEN |
| **P3 - LOW** |
| 1.5.11 | Уменьшить TODO/FIXME в коде до <50 | P3 | Высокая | Технический долг | 🟢 BACKLOG |
| 1.5.12 | Увеличить покрытие тестами до 50%+ | P3 | Высокая | Качество кода | 🟢 BACKLOG |
| 1.5.13 | Исправить security уязвимости (25 found) | P3 | Высокая | Безопасность | 🟢 BACKLOG |

---

## 🎯 Execution Order

### Phase 1.5 - Sprint 1 (День 1-3)

**Goal:** Закрыть P0 и P1 задачи

#### Day 1: C++ Compilation Fix (1.5.1)
**Priority:** P0 - CRITICAL  
**Time:** 4-6 часов

**Steps:**
1. Проанализировать ошибки компиляции C++
   ```bash
   cd native/video-processing
   cmake -B build
   cmake --build build 2>&1 | tee build.log
   ```

2. Исправить rtsp_client.cpp
   - Проверить синтаксис C++
   - Проверить зависимости
   - Проверить CMakeLists.txt

3. Проверить компиляцию
   ```bash
   ./gradlew :core:network:compileKotlinNativeWindows --no-daemon
   ```

**Acceptance Criteria:**
- ✅ C++ компилируется без ошибок
- ✅ Native Windows target проходит
- ✅ KMP compile gate проходит

---

#### Day 2: Signature Verification (1.5.2) + Contract Tests (1.5.3, 1.5.4)

**Part A: Signature Verification Script** (3-4 часа)

**Steps:**
1. Улучшить check-security-expect-actual-signatures.py
   - Добавить детальную проверку сигнатур
   - Добавить отчет по mismatches
   - Добавить markdown вывод

2. Тестирование скрипта
   ```bash
   python scripts/ci/check-security-expect-actual-signatures.py --verbose
   ```

**Part B: Contract Tests** (3-4 часа)

**Steps:**
1. Создать LocalDataEncryptionContractTest
   - Test encrypt/decrypt round-trip
   - Test fallback policy
   - Test key derivation

2. Создать PasswordEncryptionContractTest
   - Test password hashing
   - Test verification
   - Test salt generation

**Acceptance Criteria:**
- ✅ Signature script работает с детальным отчетом
- ✅ LocalDataEncryptionContractTest проходит
- ✅ PasswordEncryptionContractTest проходит

---

#### Day 3: Platform Smoke Tests (1.5.5)

**Priority:** P1 - HIGH  
**Time:** 4-6 часов

**Steps:**
1. Создать SecurityPlatformSmokeIosTest (если возможно в macOS env)
2. Создать SecurityPlatformSmokeNativeLinuxTest
3. Создать SecurityPlatformSmokeNativeWindowsTest
4. Добавить тесты в CI pipeline

**Test Coverage:**
- SecureLocalDataEncryption
- SecurePasswordEncryption
- SecureMobileSecurityLogger
- DigestCrypto

**Acceptance Criteria:**
- ✅ Platform smoke tests для всех платформ
- ✅ Тесты добавлены в CI
- ✅ Все тесты проходят

---

### Phase 1.5 - Sprint 2 (День 4-5)

**Goal:** Закрыть P2 задачи

#### Day 4: Documentation Updates (1.5.6, 1.5.7, 1.5.8)

**Priority:** P2 - MEDIUM  
**Time:** 2-3 часа

**Steps:**
1. Обновить docs/README.md
   - Добавить раздел "KMP Phase 1 Stabilization"
   - Добавить ссылки на artifacts
   - Добавить quick links

2. Обновить PROJECT_PROMPT.md
   - Добавить ссылки в Security section
   - Добавить KMP references

3. Обновить PROJECT_STRUCTURE.md
   - Добавить ссылки в Architecture section
   - Добавить KMP references

**Acceptance Criteria:**
- ✅ Все 3 документа обновлены
- ✅ Ссылки работают
- ✅ Документация актуальна

---

#### Day 5: CI Integration (1.5.9, 1.5.10)

**Part A: GitHub Job Summary** (2-3 часа)

**Steps:**
1. Улучшить render-kmp-verify-report.py
   - Добавить markdown rendering
   - Добавить Job Summary integration
   - Добавить signature report section

2. Обновить .github/workflows/ci.yml
   - Добавить шаг для signature report
   - Интегрировать в GITHUB_STEP_SUMMARY

**Part B: CI Optimization** (2-3 часа)

**Steps:**
1. Проанализировать текущее время CI
2. Оптимизировать KMP cross-compile matrix
3. Добавить caching для native builds
4. Уменьшить дублирование задач

**Acceptance Criteria:**
- ✅ Signature report в Job Summary
- ✅ Время CI уменьшено на 20%+
- ✅ Все CI шаги проходят

---

### Phase 1.5 - Backlog (Future)

#### P3 Tasks (Can be deferred)

**1.5.11: Reduce TODO/FIXME to <50**
- **Current:** 142+ TODO/FIXME
- **Target:** <50
- **Effort:** 2-3 недели
- **Approach:**
  1. Пройтись по всем TODO/FIXME
  2. Исправить критичные
  3. Отложить или удалить устаревшие
  4. Закоммитить cleanup

**1.5.12: Increase Test Coverage to 50%+**
- **Current:** ~15%
- **Target:** 50%+
- **Effort:** 4-6 недель
- **Approach:**
  1. Добавить unit tests для критичных модулей
  2. Добавить integration tests
  3. Добавить e2e tests
  4. Настроить coverage gate

**1.5.13: Fix Security Vulnerabilities (25 found)**
- **Current:** 25 vulnerabilities (6 critical, 9 high, 10 medium)
- **Target:** 0 critical, 0 high
- **Effort:** 2-3 недели
- **Approach:**
  1. Исправить critical vulnerabilities
  2. Исправить high vulnerabilities
  3. Обновить зависимости
  4. Настроить dependency scanning

---

## 📊 Phase 1.5 Metrics

### Sprint 1 (Day 1-3)

| Metric | Target | Actual |
|--------|--------|--------|
| C++ compilation fixed | ✅ | TBD |
| Signature verification improved | ✅ | TBD |
| Contract tests added | 2 | TBD |
| Platform smoke tests | 3 | TBD |
| Time spent | 12-16h | TBD |

### Sprint 2 (Day 4-5)

| Metric | Target | Actual |
|--------|--------|--------|
| Documentation updated | 3 files | TBD |
| CI integration complete | ✅ | TBD |
| CI time reduced | 20%+ | TBD |
| Time spent | 8-10h | TBD |

### Overall Phase 1.5

| Metric | Target |
|--------|--------|
| Total time | 20-26 hours |
| Tasks completed | 10/13 |
| Phase 1 completion | 100% |
| Production ready | ✅ |

---

## ✅ Phase 1.5 Completion Criteria

### Must Have (P0 + P1)
- [x] C++ compilation fixed
- [x] Signature verification improved
- [x] Contract tests for LocalDataEncryption
- [x] Contract tests for PasswordEncryption
- [x] Platform smoke tests for iOS/Native

### Should Have (P2)
- [x] Documentation updated (3 files)
- [x] CI integration complete
- [x] CI time reduced

### Nice to Have (P3)
- [ ] TODO/FIXME reduced
- [ ] Test coverage increased
- [ ] Security vulnerabilities fixed

---

## 🚀 Execution Plan

### Immediate Actions (Next 24 hours)

1. **Analyze C++ compilation errors** (2 hours)
   ```bash
   cd native/video-processing
   cmake -B build
   cmake --build build 2>&1 | head -100
   ```

2. **Fix rtsp_client.cpp** (4 hours)
   - Исправить синтаксис
   - Проверить зависимости
   - Протестировать компиляцию

3. **Verify fix** (1 hour)
   ```bash
   ./gradlew :core:network:compileKotlinNativeWindows --no-daemon
   ```

### Next Steps (Day 2-3)

4. **Improve signature script** (3-4 hours)
5. **Add contract tests** (3-4 hours)
6. **Add platform smoke tests** (4-6 hours)

### Final Steps (Day 4-5)

7. **Update documentation** (2-3 hours)
8. **CI integration** (4-6 hours)

---

**Planning Date:** 2026-01-27  
**Sprint Start:** 2026-01-27  
**Sprint End:** 2026-01-31  
**Total Duration:** 5 working days
