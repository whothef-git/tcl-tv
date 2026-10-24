#!/bin/zsh
# cache-clean.sh - clear cached files on Android-based TVs over the network.
#
# Finds every Android TV on the local network that has network ADB enabled,
# confirms it really is a TV (never a phone or tablet), and empties its app
# caches. Cache is disposable: the TV re-downloads whatever it still needs
# during normal use.
#
# What it never touches: app data, sign-ins, accounts, settings, watch
# history, or installed apps. `pm trim-caches` can only reach cache folders.
#
# Works with: Android TV, Google TV, Fire TV, and any other Android-based TV
# with network ADB enabled. It cannot work with Samsung Tizen or LG webOS -
# those have no equivalent interface.
#
#   tools/cache-clean.sh                  clean every TV found
#   tools/cache-clean.sh --dry-run        report what it would clean
#   tools/cache-clean.sh --scan-only      just list what is out there
#   tools/cache-clean.sh --device 10.0.0.5          target one device
#   tools/cache-clean.sh --subnet 192.168.1         scan a specific /24
#
# adb is found via $ADB, then PATH, then ./platform-tools/adb.

set -u
emulate -L zsh
setopt null_glob

BASE="${0:A:h}"
LOG="${TV_CACHE_LOG:-$HOME/.cache/clean-tv/cache-clean.log}"
STATE="${TV_CACHE_STATE:-$HOME/.cache/clean-tv/known-tvs}"
PORT=5555
PARALLEL=64

DRY_RUN=0
SCAN_ONLY=0
TARGETS=()
SUBNETS=()

while (( $# )); do
  case "$1" in
    --dry-run)   DRY_RUN=1 ;;
    --scan-only) SCAN_ONLY=1 ;;
    --device)    shift; TARGETS+=("${1:?--device needs an address}") ;;
    --subnet)    shift; SUBNETS+=("${1:?--subnet needs a prefix like 192.168.1}") ;;
    -h|--help)   sed -n '2,25p' "$0"; exit 0 ;;
    *)           print -u2 "Unknown option: $1"; exit 2 ;;
  esac
  shift
done

mkdir -p "${LOG:h}"
if [[ -f "$LOG" ]] && (( $(stat -f %z "$LOG") > 1048576 )); then
  tail -n 500 "$LOG" > "$LOG.tmp" && mv "$LOG.tmp" "$LOG"
fi
log() { print -r -- "$(date '+%Y-%m-%d %H:%M:%S')  $*" | tee -a "$LOG"; }

# --- locate adb ---------------------------------------------------------
ADB="${ADB:-}"
if [[ -z "$ADB" || ! -x "$ADB" ]]; then
  if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
  elif [[ -x "$BASE/platform-tools/adb" ]]; then
    ADB="$BASE/platform-tools/adb"
  else
    log "adb not found. Install Android platform-tools, or set ADB=/path/to/adb."
    exit 1
  fi
fi

# --- work out which subnets to scan -------------------------------------
if (( ! ${#SUBNETS} )); then
  local_ips=($(ifconfig 2>/dev/null | awk '/inet /{print $2}' | grep -v '^127\.'))
  for ip in $local_ips; do
    SUBNETS+=("${ip%.*}")
  done
  SUBNETS=(${(u)SUBNETS})
fi

probe_subnet() {
  local prefix="$1"
  seq 1 254 | xargs -P "$PARALLEL" -I{} sh -c \
    "nc -z -G 1 -w 1 $prefix.{} $PORT >/dev/null 2>&1 && echo $prefix.{}" 2>/dev/null
}

# --- find candidates ----------------------------------------------------
candidates=()
if (( ${#TARGETS} )); then
  candidates=("${TARGETS[@]}")
else
  # Devices that answered last time are tried first and cost nothing to recheck.
  if [[ -f "$STATE" ]]; then
    while read -r known; do
      [[ -n "$known" ]] && nc -z -G 1 -w 1 "$known" "$PORT" >/dev/null 2>&1 && candidates+=("$known")
    done < "$STATE"
  fi
  for prefix in $SUBNETS; do
    for found in $(probe_subnet "$prefix"); do
      candidates+=("$found")
    done
  done
  candidates=(${(u)candidates})
fi

if (( ! ${#candidates} )); then
  log "No device with ADB on port $PORT found on: ${SUBNETS[*]}. Is the TV on, and is network ADB still enabled?"
  exit 1
fi

# --- act on each candidate ----------------------------------------------
confirmed_tvs=()
cleaned=0

usage_pct() {  # $1 = addr:port
  "$ADB" -s "$1" shell "df /data | tail -1" 2>/dev/null \
    | awk '{for(i=1;i<=NF;i++) if($i ~ /%$/){gsub(/%/,"",$i); print $i; exit}}'
}

for addr in $candidates; do
  [[ "$addr" == *:* ]] || addr="$addr:$PORT"

  "$ADB" connect "$addr" >/dev/null 2>&1
  sleep 1

  model="$("$ADB" -s "$addr" shell getprop ro.product.model 2>/dev/null | tr -d '\r\n')"
  if [[ -z "$model" ]]; then
    log "$addr: ADB is open but the device would not answer (it may be waiting for an on-screen 'Allow debugging' prompt). Skipped."
    "$ADB" disconnect "$addr" >/dev/null 2>&1
    continue
  fi

  # Only ever touch televisions. A phone or tablet with ADB left on is skipped.
  characteristics="$("$ADB" -s "$addr" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r\n')"
  leanback="$("$ADB" -s "$addr" shell "pm list features 2>/dev/null | grep -c leanback" 2>/dev/null | tr -d '\r\n')"
  if [[ "$characteristics" != *tv* ]] && [[ "${leanback:-0}" == 0 ]]; then
    log "$addr ($model): not a TV - skipped, nothing touched."
    "$ADB" disconnect "$addr" >/dev/null 2>&1
    continue
  fi

  confirmed_tvs+=("${addr%:*}")
  before="$(usage_pct "$addr")"

  if (( DRY_RUN || SCAN_ONLY )); then
    log "$addr ($model): TV, /data ${before:-?}% used.$( (( DRY_RUN )) && print -n ' DRY RUN - would clear cached files.')"
    "$ADB" disconnect "$addr" >/dev/null 2>&1
    continue
  fi

  "$ADB" -s "$addr" shell "pm trim-caches 9999999999" >/dev/null 2>&1
  sleep 3
  after="$(usage_pct "$addr")"
  log "$addr ($model): cleared cached files. /data ${before:-?}% -> ${after:-?}% used."
  (( cleaned++ ))

  "$ADB" disconnect "$addr" >/dev/null 2>&1
done

# Remember confirmed TVs so the next run finds them without a full scan.
if (( ${#confirmed_tvs} )); then
  print -l -- ${(u)confirmed_tvs} > "$STATE"
fi

if (( ! ${#confirmed_tvs} )); then
  log "Found ${#candidates} device(s) with ADB open, but none were TVs. Nothing touched."
  exit 1
fi

(( SCAN_ONLY || DRY_RUN )) || log "Done. Cleaned $cleaned of ${#confirmed_tvs} TV(s)."
