param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AdbArgs
)
$adb = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\platform-tools\adb.exe'
$tvIp = '192.168.1.10:5555'

$devices = & $adb devices
if ($devices -notmatch '192\.168\.1\.10:5555\s+device') {
    & $adb connect $tvIp | Out-Null
}
& $adb -s $tvIp @AdbArgs
