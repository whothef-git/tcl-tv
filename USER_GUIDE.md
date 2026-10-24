# Glass TV Home User Guide (Beginner-Friendly) 📺✨

Welcome to **Glass TV Home**! This guide is crafted specifically for everyday users and beginners so you can easily install, debloat your TV for blazing-fast performance without ads, navigate seamlessly with your TV remote, and enjoy all smart ambient features effortlessly.

---

## 🌟 1. What is Glass TV Home?

Glass TV Home is a modern, lightweight home launcher replacement for Android TV and Google TV (compatible with Xiaomi Mi Box S, Mi TV Stick, Sony Bravia, TCL, Sharp, Philips, Mecool, Chromecast with Google TV, and more):

* 🚀 **Zero Ads & Ultralight**: Only ~453 KB in size. Saves over 400 MB of RAM compared to sluggish stock manufacturer launchers.
* 🪟 **Frosted Glass Aesthetic (tvOS Style)**: Modern Apple TV-inspired interface with fluid focus animations and a floating glass dock.
* 🕌 **Islamic Prayer Timetable & Daily Ayat**: Accurate 6-phase prayer timetable (Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak) with active prayer emerald highlighting and daily Quranic verses.
* ⛅ **Weather & Air Quality (API / AQI) Dashboard**: Live ambient temperature, "feels like", humidity, UV index, wind speed, sunrise/sunset, 12-hour forecast, 7-day outlook, and official Air Quality Index readings (APIMS).
* 🌙 **StandBy Screensaver & 2-Hour Auto Power-Off**: Enters ambient black nightstand screensaver after 10 minutes of inactivity (burn-in safe for OLED/LED), and automatically turns off the TV display after 2 hours of sleep standby to save electricity.

---

## 📦 Direct APK Download

For users who want to download and install the pre-compiled APK directly:
* 📥 **Official Release APK**: [**glass-tv-home.apk** (504 KB)](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk)
* 📁 **Repository Location**: [`release/glass-tv-home.apk`](release/glass-tv-home.apk)
* ⚡ **Version**: **v2.6.0** (Latest with 🎮 Console Sound Profiles: PS5, Nintendo Switch, Xbox & Mute Mode, 🔄 OTA In-App Auto Updates & 1-Line AI Patching, 🇲🇾 14 Malaysian States Wallpapers, Phone QR Upload & Unifi Box Verified).

> [!NOTE]
> ### 🇲🇾 Unifi TV Box (TM Unifi Plus Box) Users: Compatibility & Warranty Safety
> * **Can I use Glass TV Home on my Unifi TV Box? YES, 100%!** Unifi Plus Box (Skyworth HP40A3, ZTE B866V2K, and other TM Android TV boxes) runs certified Android TV. The official **unifi TV** app (for your live channels and TV subscription) remains fully functional and appears right in your app dock.
> * **Will it void my TM warranty? NO, ABSOLUTELY NOT!** Glass TV Home is installed as a standard user-space APK. It **does NOT require root**, **does NOT unlock the bootloader**, and **does NOT modify system firmware**. It can be uninstalled at any time through standard TV Settings or wiped with a simple Factory Reset, returning the box to 100% stock TM condition.

---

## 🤖 2. The Easiest Way: Let AI Do Everything Automatically! (1-Line Prompt)

You **do not need any coding skills** or technical terminal knowledge. After enabling developer mode on your TV, simply paste this **single prompt** into any AI coding assistant (such as Google Antigravity [Free] or Claude Code):

```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```

*(English alias: `install this on my home tv: https://github.com/aqmangge/glass-tv-home`)*

The AI will handle **EVERYTHING** automatically from start to finish!

### 🔄 Updating Existing Installations (1-Line AI Patching)
Already running Glass TV Home and want to update to the latest patch or version? Simply send this single line to your AI assistant:
```text
update patch latest glass tv dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```
*(English: `update glass tv to the latest patch on my tv: https://github.com/aqmangge/glass-tv-home`)*

The AI connects via Wi-Fi ADB, updates the APK in 5 seconds (`adb install -r`), and relaunches the home screen. **Your customized dock apps, wallpapers, and prayer/sound settings are 100% retained!**

### 💡 Supported AI Assistants:
1. **Google Antigravity (Free / Zero Setup)** ⭐ *Highly Recommended*:
   - Official AI coding agent from Google DeepMind with full terminal shell capabilities, automation tools, and script execution for free.
2. **Claude Code (Anthropic Paid Subscription)**:
   - Powerful CLI coding agent by Anthropic that runs inside your terminal.
3. **Cursor / Windsurf IDE (Freemium / Paid)**:
   - AI code editor with an integrated terminal shell.

---

### 📋 Step 1: Turn On Developer Mode & Debugging on Your TV
Before AI can reach your TV over Wi-Fi, perform these two simple remote actions:
1. On your TV remote, open **Settings ⚙️**.
2. Select **Device Preferences** (or **System** on newer Google TVs).
3. Select **About** $\rightarrow$ Scroll down to **Build** (or *Android TV OS Build*).
4. **Press the OK button on your remote 7 times rapidly** until a toast notification appears: *"You are now a developer!"*.
5. Press the *Back* button once. Open the new menu: **Developer options**.
6. Locate and **ENABLE**:
   - ✅ **USB debugging** (Turn ON).
   - ✅ **Wireless debugging / Network debugging** (Turn ON).
7. Ensure both your computer/phone and TV are connected to the **same Wi-Fi network**.

---

### 📋 Step 2: Paste This 1-Line Prompt to Your AI Assistant
Open **Google Antigravity**, **Claude Code**, or **Cursor** on your laptop/computer. Copy and paste this single prompt:

```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```

> **What the AI will do automatically after you send this prompt:**
> 1. The AI will ask for your TV IP address (or probe your home Wi-Fi subnet to find the TV automatically).
> 2. The AI will ask for your location in Malaysia (e.g. Putrajaya, Cheras, Shah Alam, Melaka, JB, Penang, Kuantan, etc.) to calibrate APIMS haze and JAKIM prayer zones.
> 3. Connects to your TV over Wi-Fi via ADB (`adb connect <ip>:5555`).
> 4. Creates a safety backup of your TV's factory package configuration.
> 5. Strips away sluggish manufacturer bloatware, suppresses promotional ads, and activates AdGuard Private DNS to block background analytics.
> 6. Installs the ultralight (~453 KB) Glass TV Home APK as your primary home launcher.
> 7. Pulls a live screenshot from your TV to confirm everything is running smoothly!

*(Optional: If you want to supply all info at once in a single message without back-and-forth:)*
```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
TV Model: [e.g. Xiaomi Mi Box S]
TV IP: [e.g. 192.168.68.102]
My Location: [e.g. Putrajaya / Cheras / Shah Alam / Johor Bahru / Penang]
```

> [!IMPORTANT]
> **🚨 Mandatory Instruction For AI Assistants (AI Assistant Rule)**:
> When a user gives you the prompt `install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home`, the AI **MUST** ask the user:
> 1. *"What is your TV's IP address under Settings -> Network & Internet (or your TV brand)?"* (if not auto-discovered)
> 2. *"Where are you located in Malaysia (e.g. Putrajaya, Cheras, Shah Alam, Melaka, Johor Bahru, Penang, Kuantan, Kuching, Kota Kinabalu, etc.) so that official Jabatan Alam Sekitar (DOE / APIMS) haze / IPU readings and JAKIM prayer times can be configured automatically?"*
> 
> The AI can immediately install the prebuilt APK from `release/glass-tv-home.apk` or build it, run debloat, and set the user location via:
> ```bash
> adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --es query "<USER_LOCATION>"
> ```
> *(If the user does not specify a location, the default preset is **Putrajaya** / Station CA17W).*

---

### 📋 Step 3: Click "Allow" on Your TV Screen
When the AI initiates the connection, a authorization prompt will pop up on your TV screen:
* *"Allow USB debugging from this computer?"*
* Check the box: ☑ **"Always allow from this computer"**
* Select **OK / Allow**.

The AI will handle everything else: clearing sluggish bloatware, enabling the ad blocker, deploying Glass TV Home, and showing you a screenshot of your clean, responsive TV!

---

### ↩️ How to Undo / Restore Original Launcher
All modifications made by the AI are **100% safe and reversible**:
* The AI disables apps rather than permanently deleting system packages.
* An automatic restore script (`restore.sh` / `restore.ps1`) is saved.
* If you ever wish to return to the manufacturer's original launcher, just prompt the AI:
  > *"Please restore my TV back to the original factory launcher and settings."*

---

## 🎮 3. Remote Control Navigation (D-Pad)

Glass TV Home is designed around standard TV remotes:

| Remote Button | Action / Function |
| :--- | :--- |
| **DOUBLE-PRESS HOME** | • **Outside**: Opens Recent Apps & RAM Manager.<br>• **Inside Recent Apps**: Triggers **Close All** immediately to terminate all background apps & clear RAM! |
| **HOME Button (Single Press)** | Return to home dock from any third-party app. If on home, engages ambient screensaver. |
| **UP Button (▲)** | Navigates to the top shelf to highlight the **Weather & AQI Pill**, Inputs, Wi-Fi, or **Settings Button (⚙️)**. |
| **OK / Center Button** | • Press on an app to launch it.<br>• Press on the weather pill to open the **Full-Screen Weather & Air Quality Dashboard**.<br>• Press on **Settings (⚙️)** to open the **Frosted Glass Dropdown Menu** (Wallpaper, Haze Location, Prayer Zone, Android Settings).<br>• **Long-Press** on an app to pick it up and rearrange/pin it to the dock. |
| **DOWN Button (▼)** | Returns focus down to the main application dock. |
| **LEFT / RIGHT (◀ / ▶)** | Move smoothly between dock apps or top pills with fluid glass glow transitions. |
| **BACK Button (↩)** | Closes the Weather Dashboard, Recent Apps, Dialogs, or screensaver and returns to the home dock. |

---

## 📱 4. Key Feature Walkthrough

### A. Weather & Air Quality (AQI) Hub
* Located under the digital clock (e.g., `☁ 27° Overcast  •  API 106 Unhealthy`).
* **Press UP (▲) then press OK** to launch the full Leanback weather interface:
  1. **Current Conditions**: Temperature, "Feels Like", humidity, wind velocity, UV index, sunrise and sunset times.
  2. **Official Air Quality Index (JAS Malaysia APIMS / IPU)**: Real-time ground data directly from the Department of Environment (JAS) APIMS portal with official Malaysian scale colors (Good: Green, Moderate: Sky Blue, Unhealthy: Orange, Very Unhealthy: Red, Hazardous: Maroon) and official health guidance.
  3. **12-Hour Hourly Forecast**: Hourly forecast cards with precipitation chance percentages.
  4. **7-Day Outlook**: Daily weather forecast across the upcoming week.

📍 **Manual Location Setup Directly on TV Remote (For Standalone APK Installs):**
Users who install the APK manually or relocate **DO NOT NEED A COMPUTER OR AI** to configure their station:
1. On the home screen, press **UP (▲)** and press **OK** on the weather pill to open the Weather Dashboard.
2. Press **UP (▲)** again to highlight the **📍 Tukar Stesen / Change** button next to the location name.
3. Press **OK**. A dialog modal will appear with 30+ official Malaysian DOE stations across all states:
   - *Putrajaya, Cheras (KL), Batu Muda (KL), Petaling Jaya, Shah Alam, Klang, Banting, Bandaraya Melaka, Alor Gajah, Bukit Rambai, Seremban, Nilai, Port Dickson, Larkin (JB), Pasir Gudang, Batu Pahat, Kluang, Tangkak, Seberang Jaya (Penang), Minden (USM), Balik Pulau, Ipoh, Taiping, Seri Manjung, Alor Setar, Sungai Petani, Langkawi, Kangar, Kuantan, Temerloh, Kuala Terengganu, Kemaman, Kota Bharu, Kota Kinabalu, Sandakan, Tawau, Kuching, Miri, Bintulu, Sibu, Labuan*.
4. Scroll with the remote **UP / DOWN** arrows to select your station and press **OK**.
5. Both the official JAS IPU readings and the JAKIM prayer timetable immediately calibrate to your chosen city!

### B. Islamic Prayer Timetable & Daily Ayat
* **6-Phase Prayer Timetable**: Subuh, Syuruk, Zohor, Asar, Maghrib, and Isyak synchronized with your local time zone.
* **Active Window Glow**: The current active prayer window illuminates in emerald green.
* **Daily Quranic Verse**: Rotating daily ayat with Arabic calligraphy and translations.

### C. Dynamic 4K Wallpapers & Category Switcher
* **Curated Themes**: Switch your home background between curated 4K categories directly from the remote:
  - 🌿 **Natural (Default / Bing Places)**: World landmarks, mountains, lakes, and nature landscapes with informative location stories updated weekly.
  - 🇲🇾 **Malaysia (Curated 14 States & Federal Territories)**: Pick wallpapers specifically for any of Malaysia's 14 states or rotate all states together via a dedicated sub-dialog:
    - 📍 **Johor**: Pulau Rawa Mersing & Tropical Coastline.
    - 📍 **Kedah**: Langkawi Sky Bridge Mount Mat Cincang & Emerald Seas.
    - 📍 **Kelantan**: Pantai Cahaya Bulan & Pine Groves of Pantai Senok Kota Bharu.
    - 📍 **Melaka**: UNESCO Historic City & Melaka River Night Promenade.
    - 📍 **Negeri Sembilan**: Pantai Cahaya Negeri & Teluk Kemang Port Dickson.
    - 📍 **Pahang**: Cameron Highlands Rolling Tea Plantations & Bharat Tea Valley.
    - 📍 **Perak**: Ipoh Colonial Heritage Town, Kellie's Castle & Kek Lok Tong Limestone Cave.
    - 📍 **Perlis**: Gua Kelam Limestone Cavern & Nakawan Range Foothills.
    - 📍 **Pulau Pinang**: Georgetown UNESCO Heritage Streets & Kek Lok Si Temple.
    - 📍 **Sabah**: Mount Kinabalu Peak & South Peak Kundasang.
    - 📍 **Sarawak**: UNESCO Mulu Pinnacles, Kuching Waterfront & Miri Coastline.
    - 📍 **Selangor**: Batu Caves Sri Subramaniar & Shah Alam Blue Mosque.
    - 📍 **Terengganu**: Pulau Perhentian Marine Haven & Kuala Terengganu Crystal Mosque.
    - 📍 **Wilayah Persekutuan**: Petronas Twin Towers KLCC & Kuala Lumpur City Nightscape.
  - 🏙️ **Skyscraper**: Famous modern architecture and skylines (Manhattan NYC, Chicago River, Dubai Marina, Hong Kong Cityscape).
  - 🌌 **Deep Space**: Vibrant nebulae, starfields, and cosmic vistas.
  - 🌊 **Ocean & Coastal**: Pristine coastal horizons, waves, and azure seas.
  - 🔲 **Minimalist Dark**: Refined deep textured aesthetic, power-efficient and OLED-safe.
* **📂 State Picker Sub-Dropdown (Clean TV UI)**:
  - Click **`🇲🇾 Malaysia (Pilih Negeri ▶)`** on the wallpaper menu to open a leanback dialog listing all 14 Malaysian states.
  - Your active selection is displayed right on the main menu (e.g. `🇲🇾 Malaysia: Johor (Tukar Negeri ▶)`).
  - The TV shelf automatically highlights the location, state name, and historical story of each Malaysian landmark!
* **📲 Phone QR Code Wallpaper Upload**:
  - Select **`📲 Upload Wallpaper via Phone (QR Code)`** from the wallpaper options to display a clean QR code and local web link.
  - Scan with any smartphone on the same Wi-Fi network to upload custom family photos, vacation snapshots, or artwork directly to your TV without cloud accounts or third-party apps.
* **📱 On-TV Photo Manager & Single Wallpaper Lock**:
  - After uploading photos, easily manage them right from your phone browser or through the TV D-pad menu: **`🗑️ Urus Gambar Dimuat Naik (Pilih / Padam)`**.
  - **Lock Single Wallpaper**: Choose **`📌 Set as Single Wallpaper`** to lock a single photo permanently as your background without rotating.
  - **Rotate All Photos**: Choose **`🔄 Rotate All Uploaded Photos`** to cycle through all custom photos in slideshow mode.
  - **Delete Specific / Clear All**: Delete individual uploaded photos or wipe all custom images with one click.
* **🖼️ Clean View for Custom Wallpapers**:
  - When personal custom wallpapers are active, the left-hand text shelf / description is completely hidden (`View.GONE`) so your personal photos are displayed edge-to-edge without being obscured by location captions.
* **Instant Shift**: Select **🔀 Next Wallpaper** anytime to immediately cycle to the next photo in your active category.

### D. StandBy Ambient Mode & Smart Power Saving
* **10 Minutes Idle**: Enters an ambient OLED-safe black StandBy screensaver displaying large 110sp numerals, Islamic Hijri date, and the upcoming prayer window.
* **2 Hours Later**: If left untouched, the TV display enters sleep standby (`GLOBAL_ACTION_LOCK_SCREEN`) to save power.
* **Wake Up**: Press any button on the TV remote to wake the TV immediately.

### E. Recent Apps & RAM Cleaner (Double-Press Home) ⚡
A native tvOS-style app switcher and memory flusher:
* **How to Open**: Press the remote **HOME button twice rapidly** from any app or home screen (or press the App Switch button on supported remotes).
* **Double-Press Home to Close All**: While viewing the Recent Apps screen, press the **HOME button twice rapidly** again to immediately execute **Close All** and clean RAM without touching any other key!
* **Genuine Force-Stop Automation**: Integrates background accessibility automation to cleanly terminate stubborn media players, video streams, and background tasks (such as Nuvio, YouTube, Stremio) from system memory, rather than merely dismissing their card visually.
* **Live RAM Gauge Pill**: In the top right corner, view real-time memory stats (e.g., `RAM 1.8 GB used of 2.3 GB`) with a responsive recessed progress bar.
* **Dismiss Individual Apps**: On any focused app card, press **UP (▲)** on your remote to terminate its process, animate the card away, and instantly free up RAM.
* **'Close All' Button (Trash Icon 🗑️)**: Navigate to the far right of the carousel and click the circular **Close All** button to terminate all running background apps and flush RAM in 1 tap!

### F. Frosted Glass Settings Dropdown Menu ⚙️
Clicking the **Settings (gear icon ⚙️)** on the top bar reveals an Apple tvOS-style floating glass menu:
* 🖼️ **Wallpaper**: Launches the category selector dialog (*Natural, Skyscraper, Deep Space, Ocean, Minimalist Dark, Next Wallpaper*). Preferences persist across reboots.
* 📍 **Location API Haze**: Opens a selector for 30+ official Malaysian DOE (JAS APIMS) air quality monitoring stations.
* 🕌 **Location Waktu Solat**: Opens a selector for 35+ official JAKIM prayer zones across all Malaysian states.
* 🔊 **Sound Effect**: Displays current audio theme (e.g. `Sound: PlayStation 5`) and launches the console sound profile selector with live previews.
* 🔄 **Check for Updates**: Checks GitHub directly for newer releases or patches without requiring a PC.
* ⚙️ **All Settings**: Opens standard Android TV system preferences.
*(Shortcut: Long-pressing the Settings button on your remote directly launches standard Android settings).*

### G. Console Sound Effects & Mute Mode (PS5, Switch, Xbox) 🎮🔊
Customize your TV navigation audio with iconic gaming console profiles or mute all sounds entirely:
* **PlayStation 5 (Default)**: Sleek, high-definition acoustic whooshes and tactile clicks.
* **Nintendo Switch**: Sharp mechanical tactile clicks and cheerful confirmation snaps.
* **Xbox Series X/S**: Resonant warm mid-bass navigation pulses and two-tone rising chime.
* **Muted / Off**: Complete silence for whisper-quiet late night movie sessions.
* **Instant Audio Audition**: Tapping any theme inside the dialog immediately plays its signature chime.
* **Persistent Preferences**: Settings remain saved across TV restarts via SharedPreferences.

### H. On-TV OTA Updates & Auto-Patch Checker 🔄🚀
Glass TV Home features a built-in OTA update system so you can update directly on your TV without a computer:
* **Manual Check**: Navigate to **Settings (⚙️) $\rightarrow$ Check for Updates**. The app contacts the GitHub release manifest and notifies you if a new version is available.
* **Silent Startup Check**: The launcher quietly checks for new versions in the background when booted. If a new patch is found, a changelog dialog is presented.
* **1-Click Download & Install**: Clicking **Muat Turun & Pasang** downloads the update file locally and launches Android TV's native package installer.
* **Data Preserved**: Updating preserves all apps, dock layout, and custom configurations.

---

## 📲 5. Mobile Phone Setup (Without Any PC / Laptop)

You do **not** need a computer to install Glass TV Home.

### Method 1: Android Phone via 'Bugjaeger' (Install APK + Debloat Directly)
The **Bugjaeger Mobile ADB** app (free on Google Play Store) allows your phone to act as an ADB controller over Wi-Fi:

1. Connect both phone and TV to the **same Wi-Fi**.
2. Open **Bugjaeger**, tap the **Connect 🔌** icon, enter your TV IP (e.g. `192.168.68.102:5555`), and tap **Connect**.
3. Accept the *"Allow USB debugging"* prompt on your TV screen.
4. Go to the **Packages 📦** tab in Bugjaeger, tap **+**, and select the downloaded `app-release.apk` to install it directly.
5. In the **Commands ⚡** tab, run these commands to block ads and debloat:
   ```bash
   settings put global private_dns_specifier dns.adguard-dns.com
   settings put global private_dns_mode hostname
   pm disable-user --user 0 com.mitv.tvhome
   pm disable-user --user 0 com.xiaomi.mitv.advertise
   ```

---

### Method 2: iPhone (iOS) Users: Can You Use an iPhone?
**Yes! You can easily install the APK and block ads on your TV using an iPhone.**

Due to Apple App Store sandbox security policies, Apple does not allow direct low-level ADB tools like Bugjaeger on iOS. However, iPhone users can achieve a clean setup quickly:

#### 1. Transfer & Install the APK using 'LocalSend':
* Download the free **LocalSend** app on your iPhone (App Store) and on your Android TV (Play Store).
* Download `app-release.apk` on your iPhone Safari from this GitHub repository.
* Open LocalSend on both devices (on the same Wi-Fi) and send the APK to your TV in 1 tap.
* On your TV, click the received APK file and select **Install**.

#### 2. Block Ads & Telemetry Directly from Your TV Remote (No ADB Needed):
1. On your TV, navigate to **Settings ⚙️** $\rightarrow$ **Network & Internet**.
2. Select **Private DNS** (switch from *Automatic* to *Private DNS provider hostname*).
3. Type:
   ```text
   dns.adguard-dns.com
   ```
4. Click **Save**. All system-wide banner ads, manufacturer analytics, and tracking domains will be blocked immediately!

#### 3. Deep Debloat Note for iPhone Users:
To disable deep manufacturer system packages (e.g., Xiaomi PatchWall), it is recommended to run the ADB command once from a computer, or borrow an Android phone for 2 minutes. Once disabled, the bloatware remains gone permanently.

---

## 🛠️ 6. Alternative: Manual Installation via USB Flash Drive

If you prefer using a physical USB drive:
1. Download [**glass-tv-home.apk**](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk) (also in [`release/glass-tv-home.apk`](release/glass-tv-home.apk)) onto your USB drive.
2. Plug the USB flash drive into your Android TV or Xiaomi Mi Box.
3. Open any file explorer app on the TV (e.g., *File Commander* or *FX File Explorer* from Google Play Store).
4. Click `app-release.apk` and select **Install** (allow *"Install unknown apps"* if prompted).
5. Press the **Home** button on your remote and choose **Glass TV Home** as *Always*.

---

## ❓ 7. Frequently Asked Questions (FAQ)

### Q1: Can I run Claude Code on a mobile phone (iOS / Android), and does the iOS sandbox block local network access?
* **CLI / Node.js Requirement:** Claude Code (`@anthropic-ai/claude-code`) is an interactive terminal tool that requires a full **Node.js 18+** shell environment. iOS sandbox policies strictly forbid running Node.js engines and local arbitrary shell executables directly on an iPhone.
* **Cloud vs Local Network Barrier:** Claude Code or any cloud-hosted AI (e.g., web Claude or web ChatGPT) executes on remote cloud servers. Cloud servers **CANNOT enter your private home Wi-Fi network** to access your TV's private IP (`192.168.x.x`) due to your home router's NAT firewall.
* **Solution:** ADB commands must be initiated by a local device on your home Wi-Fi (such as a laptop/PC running Google Antigravity or Claude Code, or an Android phone running Bugjaeger).

---

### Q2: Is there an AI that can install and do all tasks directly without the user installing the APK manually?
* **YES, when run on a computer/laptop on the same Wi-Fi (Google Antigravity / Claude Code / Cursor):**
  The AI connects to the TV over wireless ADB (`adb connect 192.168.x.x`), builds the APK, installs it, removes bloatware, and verifies everything with automated screenshots. The user does not need to touch or open any APK file manually!
* **On mobile phones without a PC:** You simply use a helper app (Bugjaeger on Android, or LocalSend on iPhone) to transfer and install the APK.

---

### Q3: If a user only installs the APK file, do they have full access with the TV remote, or do they still need AI access?
* **100% STANDALONE! YOU ONLY NEED YOUR TV REMOTE, ZERO AI REQUIRED!**
* Glass TV Home is a **native Android TV application** (Native Kotlin/AndroidX).
* Once the APK is installed on the TV:
  - **Full Remote Control**: The D-Pad (Up/Down/Left/Right/OK), Back, and Home buttons navigate everything natively.
  - **All smart features run locally**: Clock, Hijri dates, prayer timetable, weather & AQI (APIMS), 10-minute screensaver, and 2-hour auto sleep operate entirely inside the TV without requiring an AI agent, computer, or active subscription.
  - Family members and non-technical users can use the TV just like a standard commercial smart TV.
* **The AI's role is purely as a "mechanic/technician"** during the initial setup to automate installation and debloating. Once installed, Glass TV Home is 100% independent.

---

### Q4: Can I use this on a Unifi TV Box (TM Unifi Plus Box), and will it void my warranty?
* **1. Can it be installed on a Unifi TV Box?**
  - **YES, ABSOLUTELY!** TM Unifi Plus Boxes (both the white Skyworth HP40A3 and black ZTE models) run official Android TV.
  - You can install the Glass TV Home APK via USB flash drive, Downloader app, Send Files to TV, or wireless ADB.
  - **The official unifi TV app works normally:** You won't lose access to TM's live channels, sports, or on-demand shows because the **unifi TV** app will automatically show up in your apps list and can be pinned to your favorite dock row.
* **2. Will it void TM's warranty?**
  - **NO, YOUR WARRANTY REMAINS 100% SAFE AND INTACT.**
  - **No Rooting / Zero Firmware Modification:** Glass TV Home operates as a standard Android user-space application (just like installing Netflix or YouTube). It does not alter system firmware partitions or tamper with hardware.
  - **100% Reversible:** If you ever need technical support from TM or want to return the box, simply go to:
    `Settings -> Apps -> Glass TV Home -> Uninstall`
    or perform a standard Factory Reset. The box instantly reverts to the original TM factory launcher with zero traces left behind.

---

## 📄 License & Credits
* Built upon [CleanYourTV](https://github.com/Gathin23/cleanyourtv) © Gathin (MIT License).
* Outfit font by SIL Open Font License.
* Weather and AQI data powered by Open-Meteo & Malaysia Department of Environment (APIMS) standards.
