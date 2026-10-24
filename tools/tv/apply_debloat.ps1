param(
    [string]$TvIp = '192.168.1.10:5555'
)
$adb = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\platform-tools\adb.exe'
$logFile = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\backup\log.txt'

$packages = @(
    # Batch 1: Telemetry and Background Trackers
    'com.tcl.bi',
    'com.tcl.ttvs',
    'com.tcl.guard',
    'com.tcl.logkit',
    'com.tcl.useragreement',
    'com.tcl.usercenter',
    'com.tcl.esticker',
    'com.tcl.ocean.instructions',

    # Batch 2: Ads, Promos and Unwanted Bloat
    'com.tcl.partnercustomizer',
    'com.tcl.waterfall.overseas',
    'com.tcl.overseasappshow',
    'com.tcl.messagebox',
    'com.tcl.dashboard',
    'com.tcl.tv.tclhome_passive',
    'com.tcl.channelplus',
    'com.tcl.browser',
    'com.tcl.android.webview',
    'com.google.android.feedback',
    'com.google.android.partnersetup',
    'com.google.android.tvrecommendations',
    'com.google.android.apps.tv.dreamx'
)

Write-Host "Ensuring ADB connection to $TvIp..."
& $adb connect $TvIp | Out-Null

$timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
"[$timestamp] Starting debloat process" | Out-File -FilePath $logFile -Append -Encoding utf8

$successCount = 0
foreach ($pkg in $packages) {
    Write-Host "Disabling package: $pkg..."
    $result = (& $adb -s $TvIp shell pm disable-user --user 0 $pkg)
    $msg = "pm disable-user --user 0 $pkg -> $result"
    Write-Host "  Result: $result"
    "[$timestamp] $msg" | Out-File -FilePath $logFile -Append -Encoding utf8
    if ($result -like "*new state: disabled*") {
        $successCount++
    }
}

Write-Host "`nSuccessfully processed $successCount / $($packages.Count) packages."
