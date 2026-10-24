$adb = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\platform-tools\adb.exe'
$backupDir = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\backup'
$tvIp = '192.168.1.10:5555'

Write-Host "Connecting to $tvIp..."
& $adb connect $tvIp

Write-Host "Backing up installed packages..."
& $adb shell pm list packages -f | Out-File -FilePath "$backupDir\packages_all.txt" -Encoding utf8

Write-Host "Backing up disabled packages..."
& $adb shell pm list packages -d | Out-File -FilePath "$backupDir\packages_disabled.txt" -Encoding utf8

Write-Host "Backing up home activities..."
& $adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Out-File -FilePath "$backupDir\home_activities.txt" -Encoding utf8

Write-Host "Backing up current home..."
& $adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Out-File -FilePath "$backupDir\current_home.txt" -Encoding utf8

Write-Host "Backing up getprop..."
& $adb shell getprop | Out-File -FilePath "$backupDir\getprop.txt" -Encoding utf8

Write-Host "Backing up system settings..."
& $adb shell settings list system | Out-File -FilePath "$backupDir\settings_system.txt" -Encoding utf8
& $adb shell settings list secure | Out-File -FilePath "$backupDir\settings_secure.txt" -Encoding utf8
& $adb shell settings list global | Out-File -FilePath "$backupDir\settings_global.txt" -Encoding utf8

Write-Host "Backup completed successfully."
