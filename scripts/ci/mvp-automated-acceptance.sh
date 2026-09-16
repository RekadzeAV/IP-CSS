#!/usr/bin/env bash
# MVP Phase 1 automated acceptance — see docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md
# Exit: 0 success, non-zero on failure.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

SKIP_VIDEO_GATE=false
GENERATE_PHASE1_SUMMARY=false
PHASE1_SUMMARY_PROFILE="MvpCi"
SHOW_HELP=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      SHOW_HELP=true
      shift
      ;;
    --skip-video-gate)
      SKIP_VIDEO_GATE=true
      shift
      ;;
    --generate-phase1-summary)
      GENERATE_PHASE1_SUMMARY=true
      shift
      ;;
    --phase1-summary-profile)
      if [[ $# -lt 2 ]]; then
        echo "error: --phase1-summary-profile requires Strict or MvpCi" >&2
        exit 1
      fi
      PHASE1_SUMMARY_PROFILE="$2"
      if [[ "$PHASE1_SUMMARY_PROFILE" != "Strict" && "$PHASE1_SUMMARY_PROFILE" != "MvpCi" ]]; then
        echo "error: --phase1-summary-profile must be Strict or MvpCi" >&2
        exit 1
      fi
      shift 2
      ;;
    *)
      echo "error: unknown option: $1 (try --help)" >&2
      exit 1
      ;;
  esac
done

if [[ "$SHOW_HELP" == true ]]; then
  cat <<'EOF'
MVP Phase 1 automated acceptance (Gradle + web + video gate + optional Phase1 summary)

Usage:
  bash scripts/ci/mvp-automated-acceptance.sh
  bash scripts/ci/mvp-automated-acceptance.sh --skip-video-gate
  bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary
  bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary --phase1-summary-profile Strict

Options:
  --skip-video-gate              Skip scripts/video-e2e-go-no-go.ps1
  --generate-phase1-summary      Run scripts/generate-phase1-go-no-go-summary.ps1 after other steps
  --phase1-summary-profile       Strict|MvpCi (default MvpCi; passed to the summary script)
                                   (summary adds latest diagnostics/platform-smoke/w4-mvp-platform-gate-*.json when present — context only)

Video gate and summary require pwsh or powershell on PATH (GitHub-hosted ubuntu-latest includes
PowerShell). Use --skip-video-gate if you only want Gradle + web without pwsh.

When GITHUB_ACTIONS is set, the video gate uses committed config/video-e2e-acceptance-profile.mvp-ci.json
(no hardware diagnostics in CI). Override anytime with env MVP_VIDEO_ACCEPTANCE_PROFILE (path to a profile JSON).

Exit codes:
  0   Success
  Non-zero  First failing step; with --generate-phase1-summary: exit code 2 from summary always fails;
            exit code 3 fails this script only when --phase1-summary-profile Strict (MvpCi tolerates CONDITIONAL).
EOF
  exit 0
fi

run_pwsh_file() {
  local script_path="$1"
  shift
  local runner=""
  if command -v pwsh >/dev/null 2>&1; then
    runner="pwsh"
  elif command -v powershell >/dev/null 2>&1; then
    runner="powershell"
  else
    echo "error: pwsh or powershell not found" >&2
    return 127
  fi
  "$runner" -NoProfile -ExecutionPolicy Bypass -File "$script_path" "$@"
}

npm_ci_with_retry() {
  local web_dir="$1"
  local max_attempts=2
  local attempt=1
  while (( attempt <= max_attempts )); do
    if (( attempt > 1 )); then
      echo "npm ci retry #${attempt}: cleaning node_modules..." >&2
      rm -rf "${web_dir}/node_modules" 2>/dev/null || true
    fi
    (cd "$web_dir" && npm ci --include=dev) && return 0
    ((attempt++)) || true
  done
  return 1
}

echo "==> mvp-automated-acceptance: Gradle (shared + core:network desktopTest + server api test)"
./gradlew :shared:desktopTest :core:network:desktopTest :server:api:test --no-daemon

WEB_DIR="$ROOT/server/web"
if [[ ! -d "$WEB_DIR" ]]; then
  echo "error: server/web not found" >&2
  exit 1
fi

echo "==> mvp-automated-acceptance: server/web (build + jest)"
npm_ci_with_retry "$WEB_DIR"
(
  cd "$WEB_DIR"
  npm run build
  npm test
)

VIDEO_SCRIPT="$ROOT/scripts/video-e2e-go-no-go.ps1"
if [[ "$SKIP_VIDEO_GATE" != true ]]; then
  if [[ ! -f "$VIDEO_SCRIPT" ]]; then
    echo "error: video gate script not found: $VIDEO_SCRIPT" >&2
    exit 1
  fi
  if ! command -v pwsh >/dev/null 2>&1 && ! command -v powershell >/dev/null 2>&1; then
    echo "error: pwsh/powershell required for video gate (install PowerShell Core or use --skip-video-gate)" >&2
    exit 1
  fi
  echo "==> mvp-automated-acceptance: video e2e gate (profile-aware)"
  video_profile_arg=()
  if [[ -n "${MVP_VIDEO_ACCEPTANCE_PROFILE:-}" ]]; then
    video_profile_arg=( -AcceptanceProfilePath "$MVP_VIDEO_ACCEPTANCE_PROFILE" )
    echo "  (AcceptanceProfilePath from MVP_VIDEO_ACCEPTANCE_PROFILE)" >&2
  elif [[ -n "${GITHUB_ACTIONS:-}" ]]; then
    ci_prof="$ROOT/config/video-e2e-acceptance-profile.mvp-ci.json"
    if [[ -f "$ci_prof" ]]; then
      video_profile_arg=( -AcceptanceProfilePath "$ci_prof" )
      echo "  (AcceptanceProfilePath: mvp-ci profile for GitHub Actions)" >&2
    fi
  fi
  run_pwsh_file "$VIDEO_SCRIPT" "${video_profile_arg[@]}" || exit $?
fi

SUMMARY_SCRIPT="$ROOT/scripts/generate-phase1-go-no-go-summary.ps1"
if [[ "$GENERATE_PHASE1_SUMMARY" == true ]]; then
  if [[ ! -f "$SUMMARY_SCRIPT" ]]; then
    echo "error: summary script not found: $SUMMARY_SCRIPT" >&2
    exit 1
  fi
  if ! command -v pwsh >/dev/null 2>&1 && ! command -v powershell >/dev/null 2>&1; then
    echo "error: pwsh/powershell required for Phase1 summary" >&2
    exit 1
  fi
  echo "==> mvp-automated-acceptance: Phase1 go/no-go summary (-DecisionProfile $PHASE1_SUMMARY_PROFILE)"
  set +e
  run_pwsh_file "$SUMMARY_SCRIPT" -DecisionProfile "$PHASE1_SUMMARY_PROFILE"
  summary_exit=$?
  set -e
  if [[ $summary_exit -eq 2 ]]; then
    exit 2
  fi
  if [[ $summary_exit -eq 3 ]]; then
    if [[ "$PHASE1_SUMMARY_PROFILE" == "Strict" ]]; then
      exit 3
    fi
    echo "Phase1 summary: CONDITIONAL (exit 3); not failing acceptance (profile MvpCi)." >&2
  elif [[ $summary_exit -ne 0 ]]; then
    exit "$summary_exit"
  fi
fi

echo "==> mvp-automated-acceptance: SUCCESS"
