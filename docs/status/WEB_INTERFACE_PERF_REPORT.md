# Performance Report вЂ” Web Interface 1.6 (F2)

**Р”Р°С‚Р°:** `2026-03-27`  
**РСЃРїРѕР»РЅРёС‚РµР»СЊ:** `AI agent`  
**РћРєСЂСѓР¶РµРЅРёРµ:** `local`  
**РЎР±РѕСЂРєР°:** `working tree`  
**Р‘СЂР°СѓР·РµСЂ:** `Headless Chrome 146 (CLI Lighthouse 13.0.3)`

---

## 1) Preconditions

- Production-like Р·Р°РїСѓСЃРє:

```powershell
cd D:\GitHub-Ai\IP-CSS\server\web
npm run build
npm run start
```

**РўРµРєСѓС‰РёР№ С„Р°РєС‚:** `build/start precondition ready` (`npm run build` passes; `npm run start` launched for profiling).

- РџСЂРѕРІРµСЂСЏРµРјС‹Рµ СЃС‚СЂР°РЅРёС†С‹:
  - `/dashboard`
  - `/cameras`
  - `/events`
  - `/recordings`

---

## 2) Lighthouse РјРµС‚СЂРёРєРё (Desktop)

| РЎС‚СЂР°РЅРёС†Р° | Performance | LCP (s) | INP (ms) / TBT (ms) | CLS | Long tasks | РС‚РѕРі |
|----------|-------------|---------|----------------------|-----|-----------|------|
| /dashboard | `0` | `NO_LCP` | `tbt=n/a` | `0` | `unknown` | `FAIL` |
| /cameras | `0` | `NO_LCP` | `tbt=n/a` | `0` | `unknown` | `FAIL` |
| /events | `0` | `NO_LCP` | `tbt=n/a` | `0` | `unknown` | `FAIL` |
| /recordings | `0` | `NO_LCP` | `tbt=n/a` | `0` | `unknown` | `FAIL` |

**РЎСЂРµРґРЅРёР№ Performance:** `0 (РЅРµРІР°Р»РёРґРЅРѕ РёР·-Р·Р° NO_LCP Рё timeout)`

РђСЂС‚РµС„Р°РєС‚С‹ raw-РѕС‚С‡С‘С‚РѕРІ:
- `docs/status/lighthouse/dashboard.json`
- `docs/status/lighthouse/cameras.json`
- `docs/status/lighthouse/events.json`
- `docs/status/lighthouse/recordings.json`
- `docs/status/lighthouse/dashboard-v2.json` (РїРѕРІС‚РѕСЂРЅС‹Р№ РїСЂРѕРіРѕРЅ СЃ `--throttling-method=provided --max-wait-for-load=120000`, С‚Р°РєР¶Рµ `NO_LCP`)
- `docs/status/lighthouse/dashboard-headed.json` (РїСЂРѕРіРѕРЅ Р±РµР· `--headless`, С‚Р°РєР¶Рµ `NO_LCP`)

---

## 3) РџРѕСЂРѕРіРѕРІС‹Рµ РєСЂРёС‚РµСЂРёРё

- **Partial:** СЃСЂРµРґРЅРёР№ score >= 70, РЅРµС‚ Р±Р»РѕРєРёСЂСѓСЋС‰РёС… UI-freeze.
- **Done:** СЃСЂРµРґРЅРёР№ score >= 80, LCP <= 3.0s РЅР° РєР»СЋС‡РµРІС‹С… СЃС‚СЂР°РЅРёС†Р°С…, РЅРµС‚ Р±Р»РѕРєРёСЂСѓСЋС‰РёС… long tasks.

**Р¤Р°РєС‚РёС‡РµСЃРєР°СЏ РѕС†РµРЅРєР°:** `Open (Lighthouse executed, but results invalid for acceptance thresholds)`

---

## 4) РЈР·РєРёРµ РјРµСЃС‚Р° Рё РїСЂРёС‡РёРЅС‹

- `РџСЂРµСѓСЃР»РѕРІРёРµ F2 Р·Р°РєСЂС‹С‚Рѕ: production build Рё start РїСЂРѕС…РѕРґСЏС‚ СЃС‚Р°Р±РёР»СЊРЅРѕ.`
- `Lighthouse РїРѕ РІСЃРµРј 4 СЃС‚СЂР°РЅРёС†Р°Рј РІС‹РїРѕР»РЅРµРЅ, РЅРѕ РєР°Р¶РґС‹Р№ РїСЂРѕРіРѕРЅ СЃРѕРґРµСЂР¶РёС‚ warning: "The page loaded too slowly to finish within the time limit".`
- `Р’Рѕ РІСЃРµС… РїСЂРѕРіРѕРЅР°С… Р°СѓРґРёС‚ LCP Р·Р°РІРµСЂС€РёР»СЃСЏ СЃ РѕС€РёР±РєРѕР№ NO_LCP, РїРѕСЌС‚РѕРјСѓ score=0 Рё РјРµС‚СЂРёРєРё INP/TBT РѕС‚СЃСѓС‚СЃС‚РІСѓСЋС‚ (РЅРµРїСЂРёРіРѕРґРЅРѕ РґР»СЏ РїСЂРёС‘РјРєРё F2).`
- `РџРѕРІС‚РѕСЂРЅС‹Р№ РїСЂРѕРіРѕРЅ СЃ Р°Р»СЊС‚РµСЂРЅР°С‚РёРІРЅС‹РјРё С„Р»Р°РіР°РјРё Lighthouse (provided throttling + СѓРІРµР»РёС‡РµРЅРЅС‹Р№ load timeout) РЅРµ РёР·РјРµРЅРёР» СЂРµР·СѓР»СЊС‚Р°С‚: NO_LCP РІРѕСЃРїСЂРѕРёР·РІРѕРґРёС‚СЃСЏ.`
- `РџРѕРІС‚РѕСЂРЅС‹Р№ РїСЂРѕРіРѕРЅ Р±РµР· headless (headed Chrome) С‚Р°РєР¶Рµ РІРѕСЃРїСЂРѕРёР·РІРѕРґРёС‚ NO_LCP; РїСЂРѕР±Р»РµРјР° РЅРµ РѕРіСЂР°РЅРёС‡РёРІР°РµС‚СЃСЏ headless-СЂРµР¶РёРјРѕРј.`
- `РўРµРєСѓС‰РёР№ build РїСЂРѕС…РѕРґРёС‚ clean Р±РµР· lint/type warning РІ output next build.`

---

## 5) РџР»Р°РЅ РѕРїС‚РёРјРёР·Р°С†РёР№ (РµСЃР»Рё СЃС‚Р°С‚СѓСЃ Partial/Open)

| РџСЂРёРѕСЂРёС‚РµС‚ | Р”РµР№СЃС‚РІРёРµ | РћР¶РёРґР°РµРјС‹Р№ СЌС„С„РµРєС‚ | Р’Р»Р°РґРµР»РµС† | ETA |
|-----------|----------|------------------|----------|-----|
| P1 | РџРѕРІС‚РѕСЂРёС‚СЊ Lighthouse РІ РёРЅС‚РµСЂР°РєС‚РёРІРЅРѕРј Chrome (РЅРµ headless) Рё/РёР»Рё СЃ СѓРІРµР»РёС‡РµРЅРЅС‹Рј budget timeout | РџРѕР»СѓС‡РёС‚СЊ РІР°Р»РёРґРЅС‹Рµ LCP/INP/TBT РІРјРµСЃС‚Рѕ `NO_LCP` | FE/QA | 2026-03-28 |
| P2 | РџСЂРѕРіРЅР°С‚СЊ СЃС‚СЂР°РЅРёС†С‹ СЃ Р°РІС‚РѕСЂРёР·РѕРІР°РЅРЅРѕР№ СЃРµСЃСЃРёРµР№ Рё СЃС‚Р°Р±РёР»РёР·РёСЂРѕРІР°РЅРЅС‹Рј state (Р±РµР· initial stalls) | Р РµРїСЂРµР·РµРЅС‚Р°С‚РёРІРЅС‹Р№ perf baseline РґР»СЏ MVP-СЃС†РµРЅР°СЂРёРµРІ | FE/QA | 2026-03-28 |
| P3 | РџРѕСЃР»Рµ РІР°Р»РёРґРЅС‹С… РјРµС‚СЂРёРє РїРµСЂРµСЃС‡РёС‚Р°С‚СЊ РёС‚РѕРі Рё РѕР±РЅРѕРІРёС‚СЊ `F2` РґРѕ `Partial/Done` | Р¤РѕСЂРјР°Р»СЊРЅРѕРµ Р·Р°РєСЂС‹С‚РёРµ С‡РµРє-Р»РёСЃС‚Р° 1.6 | FE/QA | 2026-03-29 |

---

## 6) Р РµС€РµРЅРёРµ РїРѕ F2

**Р РµС€РµРЅРёРµ:** `F2 -> Open`  
**РћР±РѕСЃРЅРѕРІР°РЅРёРµ:** `Build/runtime РіРѕС‚РѕРІС‹, РЅРѕ С‚РµРєСѓС‰РёРµ Lighthouse РїСЂРѕРіРѕРЅС‹ РЅРµРІР°Р»РёРґРЅС‹ (NO_LCP + timeout), РїРѕСЌС‚РѕРјСѓ РєСЂРёС‚РµСЂРёРё Partial/Done РЅРµ РїРѕРґС‚РІРµСЂР¶РґРµРЅС‹.`

---

## 7) Next executable step

- Р”Р»СЏ РїРѕР»СѓС‡РµРЅРёСЏ РІР°Р»РёРґРЅС‹С… РјРµС‚СЂРёРє РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ РёРЅС‚РµСЂР°РєС‚РёРІРЅС‹Р№ СЃС†РµРЅР°СЂРёР№:
  - `docs/status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md`






