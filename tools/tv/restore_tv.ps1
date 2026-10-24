param(
    [string]$TvIp = '192.168.1.10:5555'
)
$adb = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\platform-tools\adb.exe'

Write-Host "Connecting to $TvIp..."
& $adb connect $TvIp | Out-Null

$packagesToRestore = @(
    'com.tcl.bi',
    'com.tcl.ttvs',
    'com.tcl.guard',
    'com.tcl.logkit',
    'com.tcl.useragreement',
    'com.tcl.usercenter',
    'com.tcl.esticker',
    'com.tcl.ocean.instructions',
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
    'com.google.android.apps.tv.dreamx',
    'com.google.android.tvrecommendations',
    'com.google.android.tungsten.setupwraith',
    'com.google.android.apps.tv.launcherx'
)

Write-Host "Re-enabling all debloated packages..."
foreach ($pkg in $packagesToRestore) {
    Write-Host "Enabling $pkg..."
    & $adb -s $TvIp shell pm enable --user 0 $pkg
}

Write-Host "Resetting default Home to stock launcher..."
& $adb -s $TvIp shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
& $adb -s $TvIp shell input keyevent KEYCODE_HOME

Write-Host "Restoration complete! Stock Google TV launcher and services have been restored."
