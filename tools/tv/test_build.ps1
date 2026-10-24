$sdkDir = 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\android-sdk'
if (-not (Test-Path $sdkDir)) { New-Item -ItemType Directory -Path $sdkDir -Force | Out-Null }

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:ANDROID_HOME = $sdkDir
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location 'C:\Users\AMIRAH YASIRA\.gemini\antigravity\scratch\glass-tv\glass-tv-home-main\launcher'
cmd.exe /c gradlew.bat assembleRelease
