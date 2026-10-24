# Panduan Pengguna Glass TV Home (Untuk Beginner) 📺✨

Selamat datang ke **Glass TV Home**! Panduan ini khas untuk pengguna baru (*beginner-friendly*) supaya anda boleh memasang, mengoptimumkan TV agar laju tanpa iklan, menggunakan remote TV, dan menikmati semua ciri menarik dengan mudah tanpa pening kepala.

---

## 🌟 1. Apa Itu Glass TV Home?

Glass TV Home ialah pengganti skrin utama (*launcher*) untuk Android TV dan Google TV (seperti Xiaomi Mi Box S, Mi TV Stick, Android TV Sharp, Sony, TCL, Philips, Mecool dsb):
* 🚀 **Tiada Iklan & Sangat Ringan**: Hanya bersaiz ~453 KB, lancar dan tidak membebankan TV (menjimatkan lebih 400 MB RAM berbanding skrin asal).
* 🪟 **Gaya Kaca Frosted (tvOS)**: Rekaan moden seperti Apple TV dengan bayang kaca terapung dan susunan aplikasi yang kemas.
* 🕌 **Waktu Solat & Ayat Harian**: Jadual 6 waktu solat Malaysia secara automatik dengan ayat al-Quran pilihan harian.
* ⛅ **Dashboard Cuaca & Jerebu (API)**: Semakan suhu semasa, ramalan 12 jam, 7 hari, serta indeks pencemaran udara rasmi (APIMS Jabatan Alam Sekitar).
* 🌙 **Screensaver Pintar & Jimat Elektrik**: Masuk screensaver selepas 10 minit rehat, dan TV padam automatik (*sleep standby*) selepas 2 jam.

---

## 📦 Muat Turun Fail APK Terus (Direct Download)

Bagi pengguna yang ingin memuat turun fail APK siap pasang secara langsung:
* 📥 **Muat Turun Fail APK Rasmi**: [**glass-tv-home.apk** (504 KB)](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk)
* 📁 **Lokasi Dalam Repositori**: [`release/glass-tv-home.apk`](release/glass-tv-home.apk)
* ⚡ **Versi**: **v2.6.0** (Terkini: 🎮 Pilihan Sound Effect Konsol, 🔄 OTA In-App Auto Update & Prompt AI Update Patch, 🇲🇾 Wallpaper 14 Negeri Malaysia, Phone QR Upload, Photo Manager)

> [!NOTE]
> ### 🇲🇾 Pengguna Unifi TV Box (TM Unifi Plus Box): Adakah Boleh Guna & Adakah Batal Waranti?
> * **Boleh guna ke? YA, 100% BOLEH!** Unifi Plus Box (model Skyworth HP40A3, ZTE B866V2K, dan kotak Android TV baharu TM) menggunakan sistem operasi Android TV yang sah (*certified Android TV*). Aplikasi rasmi **unifi TV** (siaran langsung & saluran langganan anda) tetap ada dalam senarai aplikasi dan boleh ditonton seperti biasa.
> * **Adakah membatalkan waranti (void warranty)? TIDAK SAMA SEKALI!** Glass TV Home dipasang sebagai aplikasi biasa (*user-space APK*). Ia **TIDAK me-root (no root)** kotak TV anda, **TIDAK membuka bootloader**, dan **TIDAK mengubah sistem perisian dalaman TM (firmware)**. Anda boleh *uninstall* pada bila-bila masa melalui menu Tetapan TV untuk kembali kepada paparan asal TM tanpa sebarang kesan.

---

## 🤖 2. Cara Paling Mudah: Bantuan AI (Hanya 1 Baris Prompt!)

Anda **tidak perlu tahu koding** atau menaip arahan teknikal yang panjang. Selepas menghidupkan mod pembangun di TV, anda hanya perlu tampal **satu baris prompt** ini sahaja kepada mana-mana pembantu AI (*AI Coding Assistant* seperti Google Antigravity percuma atau Claude Code):

```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```

AI akan menguruskan **SEMUANYA** secara automatik dari mula hingga selesai!

### 🔄 Bagi Pengguna Sedia Ada: Prompt Kemas Kini Patch Terkini (Update Patch)
Jika anda sudah memasang Glass TV Home dan ingin mengemas kini ke versi atau patch terkini tanpa perlu pening kepala:
```text
update patch latest glass tv dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```
*(Atau: `kemaskini glass tv dekat tv saya: https://github.com/aqmangge/glass-tv-home`)*

AI akan menyambung ke TV melalui Wi-Fi, memasang kemas kini (`adb install -r`), dan membuka semula aplikasi dalam masa 5 saat. **Semua gambar wallpaper, susunan aplikasi di dock, dan pilihan zon solat/bunyi anda akan 100% kekal!**

### 💡 Pilihan Pembantu AI:
1. **Google Antigravity (Percuma / Free)** ⭐ *Paling Disyorkan*:
   - AI agent rasmi daripada Google DeepMind dengan akses terminal shell penuh, alatan automasi, dan keupayaan menjalankan skrip secara percuma.
2. **Claude Code (Berbayar / Subscription Anthropic)**:
   - CLI coding agent berkuasa daripada Anthropic yang berjalan terus dalam terminal komputer anda.
3. **Cursor / Windsurf IDE (Freemium / Berbayar)**:
   - Editor kod pintar berasaskan AI yang mempunyai terminal shell bersepadu.

---

### 📋 Langkah 1: Hidupkan Developer Mode & Wireless Debugging di TV Anda
Sebelum AI boleh menyambung ke TV melalui Wi-Fi, lakukan 2 tetapan ringkas ini pada TV menggunakan remote:
1. Pada remote TV anda, buka **Settings (Tetapan / ikon gear ⚙️)**.
2. Pilih **Device Preferences** (atau **System** pada Google TV baru).
3. Pilih **About (Mengenai)** $\rightarrow$ Skrol ke baris bertulis **Build** (atau *Android TV OS Build*).
4. **Tekan butang OK pada remote sebanyak 7 kali berturut-turut** sehingga muncul mesej: *"You are now a developer!"*.
5. Tekan butang *Back* sekali. Masuk ke menu baharu iaitu **Developer options (Pilihan pembangun)**.
6. Cari dan **HIDUPKAN**:
   - ✅ **USB debugging** (Tukar kepada ON/Aktif).
   - ✅ **Wireless debugging / Network debugging** (Tukar kepada ON/Aktif).
7. Pastikan TV dan komputer/telefon anda bersambung pada **rangkaian Wi-Fi yang sama**.

---

### 📋 Langkah 2: Tampal Prompt 1 Baris Ini Kepada AI
Buka **Google Antigravity**, **Claude Code**, atau **Cursor** di laptop/komputer anda. Salin dan tampal satu baris ini sahaja:

```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```

> **Apa yang AI akan lakukan secara automatik selepas anda hantar prompt ini?**
> 1. AI akan bertanya alamat IP TV anda (atau mengesan IP TV di rangkaian Wi-Fi anda secara automatik).
> 2. AI akan bertanya di mana lokasi anda di Malaysia (cth: Putrajaya, KL, Cheras, Melaka, JB, Penang, Kuantan, dsb) untuk melaraskan stesen jerebu JAS APIMS dan jadual waktu solat JAKIM.
> 3. AI menyambung ke TV anda melalui Wi-Fi via ADB (`adb connect <ip>:5555`).
> 4. AI membuat salinan sandaran (*backup*) sistem asal TV.
> 5. AI mematikan aplikasi bloatware pengeluar yang melambatkan TV, mematikan iklan tajaan, dan mengaktifkan AdGuard Private DNS untuk menyekat penjejak sistem.
> 6. AI memasang fail APK Glass TV Home yang sangat ringan (~453 KB) sebagai skrin utama.
> 7. AI mengambil gambar tangkap layar (*screenshot*) TV anda yang kini laju dan bersih untuk pengesahan anda!

*(Nota: Jika anda ingin memberikan maklumat penuh terus dalam satu mesej tanpa AI bertanya semula, anda juga boleh gunakan format terperinci ini:)*
```text
install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
Model TV: [cth: Xiaomi Mi Box S]
IP TV: [cth: 192.168.68.102]
Lokasi Saya: [cth: Putrajaya / Cheras / Shah Alam / Johor Bahru / Penang]
```

> [!IMPORTANT]
> **🚨 Pesanan Khas Untuk Pembantu AI (AI Assistant Rule)**:
> Apabila pengguna memberikan prompt `install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home`, AI **WAJIB** bertanya kepada pengguna:
> 1. *"Apakah alamat IP TV anda di menu Settings -> Network & Internet (atau jenama TV anda)?"* (jika belum dapat dikesan secara automatik)
> 2. *"Di manakah lokasi anda di Malaysia (contoh: Putrajaya, Cheras, Shah Alam, Melaka, Johor Bahru, Pulau Pinang, Kuantan, Kuching, Kota Kinabalu, dll.) supaya data jerebu/IPU rasmi Jabatan Alam Sekitar (JAS) dan jadual waktu solat JAKIM dapat diselaraskan secara automatik?"*
> 
> Selepas bersambung ke TV, AI boleh pasang terus APK rasmi sedia ada dari `release/glass-tv-home.apk` atau compile APK baharu, jalankan debloat, dan tetapkan lokasi pengguna via:
> ```bash
> adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --es query "<NAMA_LOKASI>"
> ```
> *(Jika pengguna tidak menyatakan lokasi, sistem akan menggunakan lokasi lalai: **Putrajaya** / Stesen CA17W).*

---

### 📋 Langkah 3: Tekan "Allow" Pada Skrin TV
Apabila AI mula menyambung ke TV, satu tetingkap pengesahan akan muncul di skrin TV anda:
* *"Allow USB debugging from this computer?"*
* Tandakan kotak: ☑ **"Always allow from this computer"**
* Pilih dan tekan butang **OK / Allow**.

AI akan menguruskan selebihnya: membuang bloatware yang melambatkan TV, mengaktifkan penyekat iklan, memasang Glass TV Home, dan memaparkan gambar tangkap layar (*screenshot*) TV anda yang kini sangat laju dan bersih!

---

### ↩️ Cara Kembali Ke Asal (Undo / Restore)
Semua perubahan yang dibuat oleh AI adalah **100% selamat dan boleh dikembalikan**:
* AI tidak memadam fail sistem secara kekal, hanya menyahaktifkannya.
* AI secara automatik menjana skrip pemulihan (`restore.sh` / `restore.ps1`).
* Jika anda ingin kembali ke skrin asal pengeluar pada masa akan datang, anda hanya perlu beritahu AI:
  > *"Tolong restore semula TV saya kepada tetapan dan launcher asal kilang."*
  Semua aplikasi asal akan dipulihkan serta-merta seperti sedia kala.

---

## 🎮 3. Cara Mengawal Guna Remote TV (D-Pad)

Sangat mudah dan intuitif! Anda hanya perlukan butang arah pada remote TV anda:

| Butang Remote | Tindakan / Fungsi |
| :--- | :--- |
| **Tekan Dua Kali HOME (Double-Press)** | • **Di Luar**: Buka Pengurus Recent Apps & RAM Cleaner.<br>• **Di Dalam Recent Apps**: Terus jalankan **Close All** untuk tutup semua aplikasi & cuci RAM serta-merta! |
| **Butang HOME (Sekali Tekan)** | Kembali ke skrin utama dari mana-mana aplikasi. Jika di skrin utama, buka screensaver StandBy. |
| **Butang ATAS (▲)** | Naik ke atas untuk pilih **Kapsul Cuaca & Jerebu**, menu Inputs, Wi-Fi, atau **Butang Tetapan (⚙️ Settings)**. |
| **Butang TENGAH (OK)** | • Tekan pada aplikasi untuk buka aplikasi tersebut.<br>• Tekan pada kapsul cuaca untuk buka **Dashboard Cuaca & Kualiti Udara penuh**.<br>• Tekan pada butang **Settings (⚙️)** untuk buka **Menu Dropdown Kaca** (Wallpaper, Jerebu, Waktu Solat, Android Settings).<br>• **Tekan & Tahan (Long-press)** pada aplikasi untuk susun atau pindahkan ikon ke dalam/luar dock. |
| **Butang BAWAH (▼)** | Turun semula ke barisan aplikasi utama (*Dock*). |
| **Butang KIRI / KANAN (◀ / ▶)** | Bergerak antara aplikasi atau beralih antara menu atas. |
| **Butang BACK (↩)** | Keluar dari Dashboard Cuaca, Dialog Pilihan, Recent Apps, atau Screensaver dan kembali ke skrin utama. |

---

## 📱 4. Penerangan Ciri-Ciri Utama

### A. Kapsul Cuaca & Indeks Jerebu (Bawah Jam)
* Terletak betul-betul di bawah jam utama (contoh: `☁ 27° Overcast  •  API 106 Unhealthy`).
* **Tekan butang Atas (▲) lalu tekan OK** untuk membuka skrin cuaca penuh:
  1. **Suhu Semasa & Angin**: Suhu, suhu rasa (*feels like*), kelembapan, kelajuan angin, indeks UV, waktu matahari terbit & terbenam.
  2. **Kualiti Udara Rasmi (JAS APIMS / IPU)**: Menggunakan data rasmi stesen Jabatan Alam Sekitar Malaysia (MyEQMS/APIMS) dengan bar warna piawaian rasmi Malaysia (Baik: Hijau, Sederhana: Biru Langit, Tidak Sihat: Jingga, Sangat Tidak Sihat: Merah, Berbahaya: Maroon) dan nasihat kesihatan mengikut tahap IPU semasa.
  3. **Ramalan 12 Jam**: Barisan kad jam demi jam dengan suhu dan peratus kebarangkalian hujan (`↑ %`).
  4. **Ramalan 7 Hari**: Pandangan cuaca sepanjang minggu dengan suhu maksimum/minimum.

📍 **Cara Tukar Lokasi Stesen Jerebu & Waktu Solat Secara Manual Guna Remote TV (Jika Pasang APK Sendiri):**
Pengguna yang memasang fail APK secara manual atau berpindah rumah **TIDAK PERLUKAN KOMPUTER ATAU AI** untuk menukar lokasi:
1. Pada skrin utama, tekan butang **Atas (▲)** dan tekan **OK** pada kapsul cuaca untuk buka Dashboard Cuaca.
2. Tekan butang **Atas (▲)** sekali lagi ke butang **📍 Tukar Stesen / Change** (terletak bersebelahan nama bandar).
3. Tekan **OK**. Menu pilihan senarai stesen rasmi JAS Malaysia akan muncul:
   - *Putrajaya, Cheras (KL), Batu Muda (KL), Petaling Jaya, Shah Alam, Klang, Banting, Bandaraya Melaka, Alor Gajah, Bukit Rambai, Seremban, Nilai, Port Dickson, Larkin (JB), Pasir Gudang, Batu Pahat, Kluang, Tangkak, Seberang Jaya (Penang), Minden (USM), Balik Pulau, Ipoh, Taiping, Seri Manjung, Alor Setar, Sungai Petani, Langkawi, Kangar, Kuantan, Temerloh, Kuala Terengganu, Kemaman, Kota Bharu, Kota Kinabalu, Sandakan, Tawau, Kuching, Miri, Bintulu, Sibu, Labuan*.
4. Gunakan butang **Atas / Bawah** remote untuk memilih stesen anda, lalu tekan **OK**.
5. Serta-merta bacaan Indeks Pencemaran Udara (JAS IPU) dan zon waktu solat (JAKIM) akan dikemaskini mengikut bandar pilihan anda!

### B. Waktu Solat & Ayat Al-Quran Harian
* **Jadual Solat 6 Waktu**: Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak mengikut zon waktu Malaysia secara tepat.
* **Warna Hijau Zamrud**: Waktu solat yang sedang berlangsung akan automatik bertukar warna hijau terang supaya anda sentiasa peka waktu ibadah.
* **Ayat Al-Quran Pilihan**: Ayat al-Quran harian berserta teks Arab yang indah dan terjemahan maksud.

### C. Gambar Latar Belakang & Pilihan Kategori Wallpaper (4K Dynamic)
* **Koleksi Tema Pelbagai**: Kini anda boleh memilih kategori tema wallpaper kegemaran anda secara langsung dari skrin utama melalui menu Tetapan (ikon ⚙️):
  - 🌿 **Natural (Lalai / Bing Places)**: Landskap alam semula jadi bertaraf 4K dengan info nama lokasi, negara, dan penerangan ringkas setiap minggu.
  - 🇲🇾 **Malaysia (Koleksi Mengikut Setiap Negeri)**: Pilih koleksi mengikut mana-mana 14 negeri di Malaysia atau putar semua sekali gus melalui sub-dialog khas:
    - 📍 **Johor**: Pulau Rawa Mersing & Pesisir Pantai Tropika.
    - 📍 **Kedah**: Langkawi Sky Bridge Gunung Mat Cincang & Pulau Langkawi.
    - 📍 **Kelantan**: Pantai Cahaya Bulan & Deretan Rhu Pantai Senok Kota Bharu.
    - 📍 **Melaka**: Bandaraya Bersejarah UNESCO & Persiaran Sungai Melaka Malam.
    - 📍 **Negeri Sembilan**: Pantai Cahaya Negeri & Peranginan Teluk Kemang Port Dickson.
    - 📍 **Pahang**: Hamparan Ladang Teh Cameron Highlands & Lembah Teh Bharat.
    - 📍 **Perak**: Pekan Warisan Kolonial Ipoh, Istana Kellie's Castle Batu Gajah & Gua Kek Lok Tong.
    - 📍 **Perlis**: Gua Kelam Kaki Bukit & Banjaran Batu Kapur Nakawan.
    - 📍 **Pulau Pinang**: Lorong Warisan UNESCO Georgetown & Kuil Kek Lok Si Air Itam.
    - 📍 **Sabah**: Puncak Gunung Kinabalu & South Peak Kundasang Negeri Di Bawah Bayu.
    - 📍 **Sarawak**: Pinnacles Batu Kapur Mulu UNESCO, Tebingan Kuching Waterfront & Pantai Miri.
    - 📍 **Selangor**: Tangga Batu Caves Sri Subramaniar & Masjid Sultan Salahuddin Abdul Aziz Shah Alam.
    - 📍 **Terengganu**: Pulau Perhentian Syurga Marin & Masjid Kristal Muara Sungai Terengganu.
    - 📍 **Wilayah Persekutuan**: Menara Berkembar Petronas KLCC & Horizon Malam Bandaraya Kuala Lumpur.
  - 🏙️ **Skyscraper (Pencakar Langit)**: Pemandangan menara ikonik dunia (New York Manhattan, Chicago River, Dubai Marina, Hong Kong Cityscape).
  - 🌌 **Deep Space (Angkasa Lepas)**: Galaksi, nebula dan bintang angkasa yang menakjubkan.
  - 🌊 **Ocean & Coastal (Lautan)**: Keindahan pantai pasir putih, ombak dan terumbu karang.
  - 🔲 **Minimalist Dark**: Latar belakang gelap bertekstur elegan, sangat jimat tenaga dan mesra panel OLED.
* **📂 Sub-Dropdown Pemilih Negeri (UI Kemas)**:
  - Tekan **`🇲🇾 Malaysia (Pilih Negeri ▶)`** pada menu wallpaper untuk membuka senarai negeri kemas bertaraf Leanback TV.
  - Pilihan anda dipaparkan terus pada menu utama (contoh: `🇲🇾 Malaysia: Johor (Tukar Negeri ▶)`).
  - Di sudut kiri skrin, nama lokasi, negeri, dan sejarah tempat tersebut dipaparkan secara automatik!
* **📲 Muat Naik Gambar dari Telefon (Imbas Kod QR)**:
  - Pilih **`📲 Upload Wallpaper via Phone (QR Code)`** pada menu wallpaper untuk memaparkan kod QR dan pautan web tempatan.
  - Imbas dengan telefon pintar anda (pada Wi-Fi yang sama) untuk memuat naik mana-mana foto keluarga, haiwan peliharaan, atau gambar kegemaran anda terus ke TV tanpa memerlukan sebarang aplikasi pihak ketiga atau akaun awan.
* **📱 Pengurus Gambar TV & Mod 1 Gambar Kekal (Single Mode)**:
  - Selepas memuat naik gambar, anda boleh mengurus gambar sama ada melalui telefon atau terus di TV melalui menu **`🗑️ Urus Gambar Dimuat Naik (Pilih / Padam)`**.
  - **Kunci 1 Gambar Sahaja**: Pilih **`📌 Jadikan Wallpaper Tunggal`** jika anda ingin menetapkan hanya 1 gambar kekal tanpa berputar.
  - **Putar Semua Gambar**: Pilih **`🔄 Putar Semua Gambar`** untuk memutar semua foto muat naik secara automatik.
  - **Padam Foto**: Padam foto tertentu atau kosongkan semua gambar sekali gus.
* **🖼️ Paparan Bersih (Clean View)**:
  - Apabila menggunakan wallpaper sendiri (*custom*), rak teks deskripsi di sebelah kiri skrin dimatikan sepenuhnya (`View.GONE`) supaya gambar anda dipaparkan dengan jelas tanpa terlindung.
* **Tukar Gambar Serta-Merta**: Pilih **🔀 Next Wallpaper** untuk beralih ke gambar seterusnya pada bila-bila masa.

### D. Screensaver StandBy & Auto-Off 2 Jam (Jimat Elektrik)
* **10 Minit Dibiarkan Tanpa Butang**: TV akan automatik masuk mod *StandBy Screensaver* berlatar hitam pekat (selamat untuk mengelakkan *screen burn-in* pada TV OLED/LED), memaparkan jam besar 110sp, tarikh Hijriah (cth: `🌙 12 Jamadilawal 1448H`), dan solat seterusnya.
* **2 Jam Selepas Itu**: Jika TV terus tidak disentuh, sistem akan mematikan paparan TV (*sleep standby*) secara automatik.
* **Untuk Hidupkan Semula**: Tekan mana-mana butang pada remote untuk bangunkan TV.

### E. Pengurus Recent Apps & Pembersih RAM (Tekan 2 Kali Butang Home) ⚡
Ciri pintar seumpama tvOS / Android TV premium untuk melihat dan membersihkan aplikasi latar belakang:
* **Cara Buka**: Tekan butang **HOME dua kali berturut-turut** di mana-mana skrin atau aplikasi (atau butang App Switch pada remote tertentu).
* **Tekan Home 2 Kali untuk 'Close All'**: Semasa berada di dalam skrin Recent Apps, **tekan butang HOME dua kali lagi** untuk terus menutup semua aplikasi dan membebaskan RAM serta-merta tanpa perlu menekan butang lain!
* **Penamatan Sebenar (Genuine Force-Stop)**: Sistem kami dilengkapi integrasi pintar perkhidmatan Accessibility yang secara automatik menamatkan aplikasi latar mahupun aplikasi penstriman video degil (seperti Nuvio, YouTube, Stremio) hingga ke akar umbi, bukan sekadar membuang kad dari paparan.
* **Kapsul Status RAM**: Di sudut atas kanan, memaparkan bacaan RAM masa nyata (contoh: `RAM 1.8 GB used of 2.3 GB`) dengan bar kemajuan.
* **Tutup Aplikasi Individu**: Pada mana-mana kad aplikasi yang dipilih, **tekan butang ATAS (▲)** pada remote untuk menutup proses aplikasi tersebut dan membebaskan RAM serta-merta.
* **Butang 'Close All' (Ikon Tong Sampah 🗑️)**: Gerakkan fokus ke hujung kanan senarai dan tekan butang bulat **Close All** untuk menutup semua aplikasi serentak dalam 1 klik!

### F. Menu Tetapan Frosted Glass (Settings Dropdown Menu) ⚙️
Di bar atas skrin utama, tekan butang **Settings (ikon gear ⚙️)** untuk memaparkan menu terapung gaya kaca Apple tvOS:
* 🖼️ **Wallpaper**: Membuka tetingkap pilihan kategori wallpaper (*Natural, Skyscraper, Deep Space, Ocean, Minimalist Dark, Next Wallpaper*). Pilihan anda disimpan secara kekal walaupun TV dimatikan.
* 📍 **Location API Haze**: Membuka senarai 30+ stesen jerebu rasmi JAS APIMS untuk menukar stesen pemantauan udara di seluruh negeri.
* 🕌 **Location Waktu Solat**: Membuka senarai 35+ zon rasmi JAKIM untuk menyelaraskan jadual solat secara tepat mengikut daerah anda.
* 🔊 **Sound Effect**: Memaparkan tema bunyi aktif (cth: `Sound: PlayStation 5`) dan membuka dialog pilihan tema audio konsol (PS5, Nintendo Switch, Xbox Series X/S, atau Bisu/Off) dengan pratonton nada serta-merta.
* 🔄 **Check for Updates**: Menyemak patch atau versi kemas kini baharu secara terus dari GitHub tanpa memerlukan komputer.
* ⚙️ **All Settings**: Membuka menu tetapan penuh Android TV OS.
*(Pintasan: Menekan lama butang Settings pada remote juga akan membuka tetapan Android secara langsung).*

### G. Pilihan Bunyi Konsol & Suis Bisu (Sound Effects) 🎮🔊
Kini anda boleh memilih tema kesan bunyi navigasi ala konsol permainan kegemaran anda atau mematikannya sepenuhnya:
* **PlayStation 5 (Default)**: Bunyi pergerakan dan klik ikonik konsol PS5 generasi terkini yang moden, lembut, dan kemas.
* **Nintendo Switch**: Bunyi klik mekanikal tactile dan 'snap' ceria yang sangat responsif setiap kali butang ditekan.
* **Xbox Series X/S**: Bunyi 'blip' elektronik akustik lembut dengan nada dwi-harmonik moden yang tenang.
* **Muted / Off (Bisu)**: Matikan semua kesan bunyi navigasi jika anda lebih gemar suasana senyap sunyi di waktu malam.
* **Pratonton Bunyi Segera**: Setiap kali anda klik pilihan di dalam dialog, sistem akan memainkan bunyi pilihan tersebut serta-merta sebagai sampel.
* **Kekal Tersimpan**: Pilihan anda disimpan secara kekal dalam memori TV dan tidak hilang walaupun TV ditutup atau dihidupkan semula.

### H. Semakan & Auto-Update Terus Dari TV (In-App OTA Updater) 🔄🚀
Glass TV Home dilengkapi dengan enjin kemas kini pintar tanpa perlu komputer atau bantuan luaran:
* **Semakan Manual**: Tekan **Settings (⚙️) $\rightarrow$ Check for Updates**. Sistem akan terus berhubung dengan pelayan GitHub untuk memeriksa sama ada terdapat versi terkini.
* **Semakan Automatik Latar Belakang**: Setiap kali aplikasi Glass TV dibuka, ia akan memeriksa secara senyap di latar belakang. Jika versi baharu ditemui, dialog kemas kini dengan senarai pembaharuan (*changelog*) akan dipaparkan.
* **Muat Turun & Pasang 1-Klik**: Klik butang **Muat Turun & Pasang**, TV akan memuat turun fail APK terus ke peranti dan membuka pemasang rasmi Android TV untuk anda sahkan dengan remote.
* **Selamat & Kekal**: Proses kemas kini tidak memadamkan fail data, susunan aplikasi, atau tetapan peribadi anda.

---

## 📱 5. Cara Buat Guna Telefon Pintar (Tanpa Komputer / Laptop Langsung!)

**Boleh sangat!** Jika anda tiada komputer atau laptop, anda boleh gunakan telefon pintar Android anda untuk pasang, lajukan TV, dan buang iklan.

---

### Kaedah 1: Guna Aplikasi 'Bugjaeger' (Paling Berkuasa - Pasang APK + Buang Iklan dari Telefon)
Aplikasi **Bugjaeger Mobile ADB** (percuma di Google Play Store telefon) membolehkan telefon anda bertindak seperti komputer untuk mengawal dan mengoptimumkan TV.

1. **Sambung Wi-Fi**: Pastikan telefon dan TV berada di atas Wi-Fi yang sama.
2. **Pasang Bugjaeger**: Muat turun aplikasi **Bugjaeger** di telefon anda dari Play Store.
3. **Sambung ke TV**:
   - Buka Bugjaeger, tekan ikon palam (**Connect 🔌**).
   - Masukkan Alamat IP TV anda (cth: `192.168.68.102`) dan port `5555`.
   - Tekan **Connect**.
   - Pada skrin TV, tekan **Allow** apabila keluar tetingkap *"Allow USB debugging"*.
4. **Pasang Glass TV Terus dari Telefon**:
   - Masuk tab **Packages (ikon kotak 📦)** di Bugjaeger.
   - Tekan butang tambah (**+**) di atas, pilih fail `app-release.apk` yang telah dimuat turun ke telefon anda.
   - Bugjaeger akan memasang Glass TV terus ke TV dalam beberapa saat!
5. **Buang Iklan & Sekat Tracking dari Telefon**:
   - Masuk tab **Commands (ikon terminal ⚡)** di Bugjaeger.
   - Salin dan tampal perintah ini untuk sekat iklan sistem:
     ```bash
     settings put global private_dns_specifier dns.adguard-dns.com
     settings put global private_dns_mode hostname
     ```
   - Masukkan perintah untuk matikan iklan pengeluar (cth Xiaomi PatchWall):
     ```bash
     pm disable-user --user 0 com.mitv.tvhome
     pm disable-user --user 0 com.xiaomi.mitv.advertise
     ```
   - Selesai! TV anda kini bebas iklan dan sangat laju.

---

### Kaedah 2: Paling Ringkas (Hanya Pasang APK Guna 'Send Files to TV')
Jika anda hanya mahu pasang Glass TV Home tanpa buat debloat teknikal:
1. Pasang aplikasi percuma **Send Files to TV** di telefon pintar dan di Android TV anda (dari Google Play Store).
2. Muat turun `app-release.apk` ke telefon anda (dari pautan fail GitHub).
3. Buka aplikasi pada kedua-dua peranti, hantar APK dari telefon ke TV melalui Wi-Fi.
4. Di TV anda, buka aplikasi pengurus fail (seperti *File Commander* atau *FX File Explorer*), klik pada fail APK dan pilih **Install**.
5. Tekan butang **Home** pada remote TV dan pilih **Glass TV Home** sebagai *Always / Sentiasa*.

---

### 🤖 Bolehkah Tanya AI dari Telefon?
Boleh! Anda boleh buka aplikasi AI di telefon (seperti Claude, ChatGPT, atau pelayar web):
1. Beritahu AI jenama TV anda:
   > *"Saya guna telefon dan app Bugjaeger. TV saya model Xiaomi Mi Box S. Berikan saya senarai arahan debloat untuk matikan iklan dan tracking."*
2. AI akan berikan senarai perintah `pm disable-user`.
3. Anda hanya perlu salin dan tampal perintah tersebut ke dalam aplikasi **Bugjaeger** di telefon anda!

---

### 🍏 Khusus untuk Pengguna iPhone (iOS): Boleh Ke Guna iPhone?

**Jawapan jujur: Boleh untuk pasang APK & sekat iklan, tetapi ada sedikit perbezaan berbanding Android.**

Kerana polisi Apple (iOS) yang ketat, Apple tidak membenarkan aplikasi seperti *Bugjaeger / ADB* berada di App Store. Walau bagaimanapun, pengguna iPhone tetap boleh melakukannya dengan sangat mudah:

#### 1. Cara Hantar & Pasang APK dari iPhone ke TV (Guna 'LocalSend'):
* Muat turun aplikasi percuma **LocalSend** di App Store iPhone dan di Google Play Store TV anda.
* Muat turun fail `app-release.apk` di iPhone anda dari pautan GitHub ini.
* Buka LocalSend pada iPhone dan TV (pastikan kedua-duanya bersambung pada Wi-Fi yang sama).
* Hantar APK dari iPhone ke TV dengan 1 klik sahaja!
* Di TV, klik fail APK tersebut dan tekan **Install**.

#### 2. Cara Sekat Iklan & Tracking Jika Guna iPhone (Tanpa Perlu ADB):
Anda tidak perlukan aplikasi ADB pun untuk sekat iklan. Anda boleh lakukannya terus melalui remote TV:
1. Pada remote TV, pergi ke **Settings ⚙️** $\rightarrow$ **Network & Internet**.
2. Pilih **Private DNS** (tukar daripada *Off/Automatic* kepada *Private DNS provider hostname*).
3. Masukkan alamat ini menggunakan papan kekunci di TV:
   ```text
   dns.adguard-dns.com
   ```
4. Tekan **Save / Simpan**.
5. Serta-merta semua iklan sepanduk, penjejak data pengeluar, dan analitik disekat di peringkat sistem TV anda secara automatik!

#### 3. Ingin Buang Bloatware Pengeluar Secara Penuh?
Jika anda ingin membuang aplikasi bawaan pengeluar (seperti Xiaomi PatchWall) sehingga 100% bersih, anda disyorkan menggunakan **laptop/komputer** (atau meminjam telefon Android kawan sekali sahaja selama 2 minit). Selepas dipadam sekali, ia akan kekal bersih selamanya pada TV anda!

---

## 🛠️ 6. Cara Pasang Manual Guna Pendrive (Alternatif)

Jika anda lebih suka menggunakan pendrive USB:
1. Muat turun fail [**glass-tv-home.apk**](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk) (terdapat dalam folder [`release/`](release/glass-tv-home.apk)).
2. Salin fail `glass-tv-home.apk` tersebut ke dalam pendrive USB.
3. Cucuk pendrive pada Android TV atau Xiaomi Mi Box anda.
4. Buka aplikasi pengurus fail di TV (cth: *File Commander* atau *FX File Explorer* dari Google Play Store TV).
5. Klik pada `glass-tv-home.apk` dan tekan **Install**.
   *(Jika TV meminta izin "Install unknown apps", pilih **Allow/Benarkan**)*.
6. Selepas siap dipasang, tekan butang **Home** pada remote TV dan pilih **Glass TV Home** sebagai *Always / Sentiasa*.

---

## ❓ 7. Soalan Lazim (FAQ) Mengenai AI, Telefon & Remote TV

### S1: Bolehkah jalankan Claude Code di telefon (iOS / Android) dan adakah masalah iOS Sandbox menghalang capaian network?
* **Realiti Claude Code di Telefon:** Claude Code ialah perisian Terminal/CLI (`@anthropic-ai/claude-code`) yang memerlukan enjin **Node.js 18+**. Di iPhone (iOS), sistem keselamatan Sandbox Apple tidak membenarkan pelaksanaan enjin Node.js atau shell secara terus di dalam telefon.
* **Isu Capaian Rangkaian (Cloud vs Local Network):** Claude Code (atau mana-mana AI awan seperti Claude.ai / ChatGPT) berjalan di server awan. Server awan di internet **TIDAK BOLEH menembusi Wi-Fi rumah anda** untuk mencapai IP peribadi TV (`192.168.x.x`) kerana disekat oleh *firewall router* rumah anda.
* **Penyelesaian:** Arahan ADB mesti dilancarkan dari peranti yang berada di dalam rangkaian Wi-Fi rumah yang sama:
  - **Paling senang:** Guna laptop / komputer (Google Antigravity percuma atau Claude Code di terminal laptop).
  - **Guna telefon pintar:** Pengguna Android boleh guna aplikasi tempatan seperti **Bugjaeger Mobile ADB**.

---

### S2: Adakah AI yang boleh pasang dan buat semua tugasan secara terus tanpa pengguna perlu pasang APK sendiri?
* **YA, jika menggunakan Komputer / Laptop (Google Antigravity / Claude Code / Cursor):**
  AI pada komputer boleh bersambung terus ke TV melalui Wi-Fi rumah (`adb connect 192.168.x.x`). AI akan membina APK $\rightarrow$ menghantar ke TV $\rightarrow$ memasang APK $\rightarrow$ memadam iklan pengeluar $\rightarrow$ dan menguji skrin secara automatik 100%. Anda tidak perlu muat turun atau sentuh fail APK langsung!
* **Jika Hanya Menggunakan Telefon:** Anda perlu bantuan aplikasi perantara tempatan (Android guna *Bugjaeger*, iPhone hantar fail melalui *LocalSend*).

---

### S3: Jika pengguna hanya pasang fail APK, adakah boleh akses penuh menggunakan remote TV atau perlukan akses AI juga?
* **100% BOLEH GUNA REMOTE SAHAJA, TIDAK PERLUKAN AI LAGI!**
* Glass TV Home dibina sebagai aplikasi Android TV asli (*Native Android TV app*).
* Sebaik sahaja fail APK terpasang pada TV:
  - **Remote TV berfungsi sepenuhnya**: Butang D-Pad (Atas/Bawah/Kiri/Kanan/OK) untuk kawal aplikasi dan cuaca, serta butang Home/Back berfungsi seperti biasa.
  - **Semua ciri automatik berjalan sendiri**: Jam, jadual waktu solat, cuaca & kualiti udara (APIMS), mod screensaver 10 minit, dan auto-sleep 2 jam dijana sepenuhnya oleh cip pemproses TV tanpa bergantung kepada mana-mana AI atau internet luar.
  - Ahli keluarga dan pengguna biasa boleh menikmati TV dengan lancar hanya menggunakan remote TV biasa.
* **Peranan AI hanyalah seperti "jurutera"** yang membantu memasang dan mencuci bloatware pada peringkat permulaan sahaja.

---

### S4: Saya guna Unifi TV Box (TM Unifi Plus Box), bolehkah saya guna launcher ini dan adakah ia akan membatalkan waranti (void warranty)?
* **1. Bolehkah dipasang pada Unifi TV Box?**
  - **YA, BOLEH SANGAT!** Kotak TM Unifi Plus Box (sama ada model putih Skyworth HP40A3 mahupun model hitam ZTE) beroperasi di atas sistem Android TV rasmi Google.
  - Anda boleh memasang fail APK Glass TV Home sama ada melalui pemacu USB (pendrive), aplikasi *Downloader / Send Files to TV*, atau arahan Wi-Fi ADB.
  - **Aplikasi unifi TV tetap berfungsi sepenuhnya:** Anda tidak akan kehilangan siaran TV secara langsung (*Live TV*), sukan, atau rancangan kegemaran kerana aplikasi **unifi TV** akan tersenarai secara automatik di skrin utama Glass TV dan boleh diletakkan pada dok hadapan (*favorites*).
* **2. Adakah ia membatalkan waranti TM (Void Warranty)?**
  - **TIDAK, WARANTI ANDA 100% KEKAL SELAMAT.**
  - **Tanpa Root (Zero Root / No Modding):** Glass TV Home hanyalah sebuah aplikasi *launcher* biasa (sama seperti memasang Netflix, YouTube, atau Spotify dari Play Store). Ia tidak mengubah fail sistem induk (*firmware / vendor partition*).
  - **Boleh Dibuang Bila-Bila Masa (Reversible):** Jika anda perlu memulangkan kotak kepada TM atau menuntut waranti sekiranya rosak, anda hanya perlu pergi ke:
    `Settings (Tetapan) -> Apps (Aplikasi) -> Glass TV Home -> Uninstall (Nyahpasang)`
    atau lakukan *Factory Reset*. Kotak Unifi TV akan kembali 100% kepada keadaan asal kilang tanpa meninggalkan sebarang kesan pengubahsuaian.

---

## 📄 Lesen & Penghargaan
* Dibina berasaskan [CleanYourTV](https://github.com/Gathin23/cleanyourtv) © Gathin (Lesen MIT).
* Fon Outfit oleh SIL Open Font License.
* Data cuaca oleh Open-Meteo & data kualiti udara berasaskan piawaian APIMS Jabatan Alam Sekitar Malaysia.
