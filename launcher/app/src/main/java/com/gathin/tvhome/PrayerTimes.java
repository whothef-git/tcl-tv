package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Official e-solat.gov.my Prayer Times provider for TV Home Launcher.
 * Features:
 * - Direct official JAKIM e-solat.gov.my API integration.
 * - Comprehensive zone selection covering all Malaysian states (Perak PRK06 default).
 * - Multi-month / monthly local caching in SharedPreferences.
 * - Fallback to secondary takwim API and offline astronomical calculation.
 * - Authoritative JAKIM Hijrah date synchronization.
 */
final class PrayerTimes {
    private static final String TAG = "TvHome.Prayer";
    private static final String PREFS_NAME = "prayer_times_cache";
    private static final String KEY_JSON = "takwim_solat_json";
    private static final String KEY_MONTH = "takwim_month";
    private static final String KEY_HIJRI = "takwim_hijri_today";
    private static final String KEY_MIGRATED = "migrated_to_esolat_prk06_v1";

    public static final String DEFAULT_ZONE = "PRK06";
    public static final String DEFAULT_ZONE_NAME = "Taiping, Bagan Serai, Parit Buntar, Selama";
    public static final double DEFAULT_LAT = 4.8569;
    public static final double DEFAULT_LON = 100.7417;
    private static final double TIMEZONE = 8.0;

    private static final String[] PRAYER_NAMES = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String[] PRAYER_KEYS = {"fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha"};

    private static volatile String lastKnownHijri = null;

    private PrayerTimes() {
    }

    private static void checkMigration(SharedPreferences prefs) {
        if (!prefs.getBoolean(KEY_MIGRATED, false)) {
            // Default user to Perak PRK06 (Taiping, Bagan Serai, Parit Buntar, Selama)
            prefs.edit()
                    .putString("manual_prayer_zone", DEFAULT_ZONE)
                    .putString("manual_prayer_zone_name", DEFAULT_ZONE_NAME)
                    .putString("manual_lat", String.valueOf(DEFAULT_LAT))
                    .putString("manual_lon", String.valueOf(DEFAULT_LON))
                    .remove(KEY_JSON) // Force re-fetch from e-solat.gov.my
                    .putBoolean(KEY_MIGRATED, true)
                    .apply();
        }
    }

    /** Forces an immediate background sync for the active location/zone. */
    static void sync(Context context, Runnable onUpdated) {
        syncInternal(context, onUpdated, true);
    }

    /** Refresh timetable in background if needed (once a month / day). */
    static void syncIfDue(Context context, Runnable onUpdated) {
        syncInternal(context, onUpdated, false);
    }

    private static void syncInternal(Context context, Runnable onUpdated, boolean force) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                checkMigration(prefs);

                Calendar now = Calendar.getInstance();
                int currentMonth = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1);
                String zone = getActiveZone(context);
                String cachedZone = prefs.getString("cached_zone", "");

                if (!force && zone.equals(cachedZone) && prefs.getInt(KEY_MONTH, 0) == currentMonth && prefs.contains(KEY_JSON)) {
                    return; // already have this month's takwim
                }

                String jsonResponse = null;

                // 1. Primary: Official e-solat.gov.my API
                try {
                    String esolatUrl = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=" + zone;
                    URL url = new URL(esolatUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
                    conn.setRequestProperty("Accept", "application/json");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        jsonResponse = sb.toString();
                        Log.i(TAG, "e-solat.gov.my fetch successful for zone " + zone);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "e-solat.gov.my connection failed, falling back: " + e.getMessage());
                }

                // 2. Secondary fallback: api.waktusolat.app
                if (jsonResponse == null || jsonResponse.trim().isEmpty() || !jsonResponse.contains("prayerTime")) {
                    try {
                        String fbUrl = "https://api.waktusolat.app/v2/solat/" + zone;
                        URL url = new URL(fbUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);
                        conn.setRequestMethod("GET");

                        if (conn.getResponseCode() == 200) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) sb.append(line);
                            reader.close();
                            jsonResponse = sb.toString();
                            Log.i(TAG, "api.waktusolat.app fallback fetch successful for zone " + zone);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "waktusolat fallback also failed: " + e.getMessage());
                    }
                }

                if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                    prefs.edit()
                            .putString(KEY_JSON, jsonResponse)
                            .putInt(KEY_MONTH, currentMonth)
                            .putString("cached_zone", zone)
                            .apply();

                    // Extract and cache today's Hijri date
                    extractTodayHijri(jsonResponse, prefs);

                    if (onUpdated != null) {
                        new Handler(Looper.getMainLooper()).post(onUpdated);
                    }
                    Log.i(TAG, "Synced prayer times for zone " + zone + " successfully");
                }
            } catch (Exception e) {
                Log.w(TAG, "Prayer sync offline/failed: " + e.getMessage());
            }
        }, "prayer-sync").start();
    }

    private static void extractTodayHijri(String jsonStr, SharedPreferences prefs) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            if (obj.has("prayerTime")) {
                JSONArray arr = obj.getJSONArray("prayerTime");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    String dateStr = item.optString("date", "");
                    if (dateStr.contains("-")) {
                        int dayNum = Integer.parseInt(dateStr.substring(0, dateStr.indexOf('-')).trim());
                        if (dayNum == today) {
                            String hijriRaw = item.optString("hijri", "");
                            String formatted = formatHijriString(hijriRaw);
                            if (formatted != null && !formatted.isEmpty()) {
                                lastKnownHijri = formatted;
                                prefs.edit().putString(KEY_HIJRI, formatted).apply();
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static class PrayerZone {
        public final String code;
        public final String state;
        public final String name;
        public final double lat;
        public final double lon;

        public PrayerZone(String code, String state, String name, double lat, double lon) {
            this.code = code;
            this.state = state;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        public String getDisplayName() {
            return name + " (" + state + " • " + code + ")";
        }
    }

    private static final List<PrayerZone> PRESET_ZONES = new ArrayList<>();

    static {
        // =========================================================================
        // PERAK (Preferred: PRK06 Taiping, Bagan Serai, Parit Buntar, Selama)
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("PRK06", "Perak", "Taiping, Bagan Serai, Parit Buntar, Selama", 4.8569, 100.7417));
        PRESET_ZONES.add(new PrayerZone("PRK01", "Perak", "Tapah, Slim River, Tanjung Malim", 3.6800, 101.5200));
        PRESET_ZONES.add(new PrayerZone("PRK02", "Perak", "Ipoh, Batu Gajah, Kampar, Sg. Siput, Kuala Kangsar", 4.5975, 101.0901));
        PRESET_ZONES.add(new PrayerZone("PRK03", "Perak", "Pengkalan Hulu, Grik, Lenggong", 5.4290, 101.1294));
        PRESET_ZONES.add(new PrayerZone("PRK04", "Perak", "Temengor, Belum", 5.5333, 101.3500));
        PRESET_ZONES.add(new PrayerZone("PRK05", "Perak", "Teluk Intan, Bagan Datuk, Seri Iskandar, Lumut, Sitiawan, Pangkor", 4.1953, 100.6728));
        PRESET_ZONES.add(new PrayerZone("PRK07", "Perak", "Bukit Larut", 4.8628, 100.7933));

        // =========================================================================
        // WILAYAH PERSEKUTUAN
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("WLY01", "W.P.", "Kuala Lumpur, Putrajaya", 2.9158, 101.6909));
        PRESET_ZONES.add(new PrayerZone("WLY02", "W.P.", "Labuan", 5.2831, 115.2308));

        // =========================================================================
        // SELANGOR
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("SGR01", "Selangor", "Shah Alam, Petaling, Subang, Gombak, Hulu Langat, Sepang", 3.0738, 101.5183));
        PRESET_ZONES.add(new PrayerZone("SGR02", "Selangor", "Kuala Selangor, Sabak Bernam", 3.3500, 101.2500));
        PRESET_ZONES.add(new PrayerZone("SGR03", "Selangor", "Klang, Kuala Langat, Banting", 3.0108, 101.4081));

        // =========================================================================
        // PULAU PINANG
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("PNG01", "Pulau Pinang", "Seluruh Negeri Pulau Pinang (George Town, Butterworth)", 5.4141, 100.3288));

        // =========================================================================
        // KEDAH
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("KDH01", "Kedah", "Kota Setar, Kubang Pasu, Pokok Sena (Alor Setar)", 6.1210, 100.3601));
        PRESET_ZONES.add(new PrayerZone("KDH02", "Kedah", "Kuala Muda, Yan, Pendang (Sungai Petani)", 5.6470, 100.4877));
        PRESET_ZONES.add(new PrayerZone("KDH03", "Kedah", "Padang Terap, Sik", 6.0000, 100.7500));
        PRESET_ZONES.add(new PrayerZone("KDH04", "Kedah", "Baling", 5.6764, 100.9169));
        PRESET_ZONES.add(new PrayerZone("KDH05", "Kedah", "Bandar Baharu, Kulim", 5.3647, 100.5557));
        PRESET_ZONES.add(new PrayerZone("KDH06", "Kedah", "Pulau Langkawi", 6.3500, 99.8000));
        PRESET_ZONES.add(new PrayerZone("KDH07", "Kedah", "Puncak Gunung Jerai", 5.7950, 100.4347));

        // =========================================================================
        // PERLIS
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("PLS01", "Perlis", "Seluruh Negeri Perlis (Kangar, Arau, Padang Besar)", 6.4414, 100.1986));

        // =========================================================================
        // NEGERI SEMBILAN
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("NGS01", "Negeri Sembilan", "Tampin, Jempol", 2.4700, 102.2300));
        PRESET_ZONES.add(new PrayerZone("NGS02", "Negeri Sembilan", "Seremban, Port Dickson, Nilai, Rembau, Jelebu", 2.7258, 101.9424));

        // =========================================================================
        // MELAKA
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("MLK01", "Melaka", "Bandaraya Melaka, Alor Gajah, Jasin, Masjid Tanah", 2.1896, 102.2501));

        // =========================================================================
        // JOHOR
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("JHR01", "Johor", "Pulau Aur, Pulau Pemanggil", 2.4469, 104.5161));
        PRESET_ZONES.add(new PrayerZone("JHR02", "Johor", "Johor Bahru, Kota Tinggi, Pasir Gudang, Kulai", 1.4927, 103.7414));
        PRESET_ZONES.add(new PrayerZone("JHR03", "Johor", "Kluang, Pontian", 2.0306, 103.3214));
        PRESET_ZONES.add(new PrayerZone("JHR04", "Johor", "Batu Pahat, Muar, Segamat, Tangkak", 1.8548, 102.9325));

        // =========================================================================
        // PAHANG
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("PHG01", "Pahang", "Pulau Tioman", 2.7904, 104.1706));
        PRESET_ZONES.add(new PrayerZone("PHG02", "Pahang", "Kuantan, Pekan, Rompin, Muadzam Shah", 3.8168, 103.3317));
        PRESET_ZONES.add(new PrayerZone("PHG03", "Pahang", "Jerantut, Temerloh, Maran, Bera, Jengka", 3.4478, 102.4178));
        PRESET_ZONES.add(new PrayerZone("PHG04", "Pahang", "Bentong, Lipis, Raub", 3.5200, 101.9000));
        PRESET_ZONES.add(new PrayerZone("PHG05", "Pahang", "Genting Sempah, Janda Baik, Bukit Tinggi", 3.3100, 101.8600));
        PRESET_ZONES.add(new PrayerZone("PHG06", "Pahang", "Cameron Highlands, Bukit Fraser", 4.4700, 101.3800));

        // =========================================================================
        // TERENGGANU
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("TRG01", "Terengganu", "Kuala Terengganu, Marang, Kuala Nerus", 5.3117, 103.1324));
        PRESET_ZONES.add(new PrayerZone("TRG02", "Terengganu", "Besut, Setiu", 5.7500, 102.5500));
        PRESET_ZONES.add(new PrayerZone("TRG03", "Terengganu", "Hulu Terengganu (Kuala Berang)", 5.0667, 103.0167));
        PRESET_ZONES.add(new PrayerZone("TRG04", "Terengganu", "Dungun, Kemaman", 4.7734, 103.4190));

        // =========================================================================
        // KELANTAN
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("KTN01", "Kelantan", "Kota Bharu, Bachok, Pasir Puteh, Tumpat, Pasir Mas", 6.1254, 102.2386));
        PRESET_ZONES.add(new PrayerZone("KTN02", "Kelantan", "Gua Musang, Jeli, Lojing", 4.8823, 101.9680));

        // =========================================================================
        // SABAH
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("SBH01", "Sabah", "Sandakan (Timur), Bukit Garam, Semawang, Sukau", 5.8402, 118.1179));
        PRESET_ZONES.add(new PrayerZone("SBH02", "Sabah", "Beluran, Telupid, Pinangah, Kuamut", 5.6500, 117.5500));
        PRESET_ZONES.add(new PrayerZone("SBH03", "Sabah", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna", 5.0268, 118.3270));
        PRESET_ZONES.add(new PrayerZone("SBH04", "Sabah", "Bandar Tawau, Balung, Merotai, Kalabakan", 4.2447, 117.8912));
        PRESET_ZONES.add(new PrayerZone("SBH05", "Sabah", "Kudat, Kota Marudu, Pitas, Pulau Banggi", 6.8837, 116.8483));
        PRESET_ZONES.add(new PrayerZone("SBH06", "Sabah", "Gunung Kinabalu", 6.0750, 116.5583));
        PRESET_ZONES.add(new PrayerZone("SBH07", "Sabah", "Kota Kinabalu, Penampang, Tuaran, Papar, Putatan", 5.9804, 116.0735));
        PRESET_ZONES.add(new PrayerZone("SBH08", "Sabah", "Keningau, Tambunan, Nabawan, Pensiangan", 5.3444, 116.1603));
        PRESET_ZONES.add(new PrayerZone("SBH09", "Sabah", "Beaufort, Kuala Penyu, Sipitang, Tenom", 5.3473, 115.7455));

        // =========================================================================
        // SARAWAK
        // =========================================================================
        PRESET_ZONES.add(new PrayerZone("SWK01", "Sarawak", "Limbang, Lawas, Sundar, Trusan", 4.7500, 115.0000));
        PRESET_ZONES.add(new PrayerZone("SWK02", "Sarawak", "Miri, Niah, Bekenu, Sibuti, Marudi", 4.3995, 113.9914));
        PRESET_ZONES.add(new PrayerZone("SWK03", "Sarawak", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu", 3.1667, 113.0333));
        PRESET_ZONES.add(new PrayerZone("SWK04", "Sarawak", "Sibu, Mukah, Dalat, Song, Kanowit, Kapit", 2.2878, 111.8305));
        PRESET_ZONES.add(new PrayerZone("SWK05", "Sarawak", "Sarikei, Matu, Julau, Rajang, Daro, Bintangor", 2.1167, 111.5167));
        PRESET_ZONES.add(new PrayerZone("SWK06", "Sarawak", "Sri Aman, Lubok Antu, Betong, Saratok", 1.2333, 111.4667));
        PRESET_ZONES.add(new PrayerZone("SWK07", "Sarawak", "Serian, Simunjan, Samarahan, Sebuyau", 1.1667, 110.5667));
        PRESET_ZONES.add(new PrayerZone("SWK08", "Sarawak", "Kuching, Bau, Lundu, Sematan", 1.5533, 110.3592));
        PRESET_ZONES.add(new PrayerZone("SWK09", "Sarawak", "Zon Khas (Kampung Matang)", 1.5833, 110.2500));
    }

    public static List<PrayerZone> getPresetZones() {
        return Collections.unmodifiableList(PRESET_ZONES);
    }

    public static String getActiveZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkMigration(prefs);
        if (prefs.contains("manual_prayer_zone")) {
            return prefs.getString("manual_prayer_zone", DEFAULT_ZONE);
        }
        return getJakimZone(context);
    }

    public static String getActiveZoneName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkMigration(prefs);
        if (prefs.contains("manual_prayer_zone_name")) {
            return prefs.getString("manual_prayer_zone_name", DEFAULT_ZONE_NAME);
        }
        String zone = getActiveZone(context);
        for (PrayerZone pz : PRESET_ZONES) {
            if (pz.code.equalsIgnoreCase(zone)) return pz.name;
        }
        return DEFAULT_ZONE_NAME;
    }

    public static String getShortZoneName(Context context) {
        String name = getActiveZoneName(context);
        if (name == null || name.trim().isEmpty()) return "Taiping";
        int cut = name.indexOf(',');
        if (cut > 0) name = name.substring(0, cut);
        cut = name.indexOf('&');
        if (cut > 0) name = name.substring(0, cut);
        cut = name.indexOf('(');
        if (cut > 0) name = name.substring(0, cut);
        name = name.trim();
        return name.isEmpty() ? "Taiping" : name;
    }

    public static void clearManualZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove("manual_prayer_zone")
                .remove("manual_prayer_zone_name")
                .remove("manual_lat")
                .remove("manual_lon")
                .remove(KEY_JSON)
                .apply();
    }

    public static void setPrayerZone(Context context, PrayerZone zone) {
        if (zone == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("manual_prayer_zone", zone.code)
                .putString("manual_prayer_zone_name", zone.name)
                .putString("manual_lat", String.valueOf(zone.lat))
                .putString("manual_lon", String.valueOf(zone.lon))
                .remove(KEY_JSON) // Clear cache to sync new zone immediately
                .apply();
        sync(context, null);
    }

    private static String getJakimZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains("manual_prayer_zone")) {
            return prefs.getString("manual_prayer_zone", DEFAULT_ZONE);
        }
        String loc = AppLocationManager.getLocationName(context).toLowerCase(Locale.ROOT);
        String stationId = AppLocationManager.getStationId(context).toUpperCase(Locale.ROOT);

        if (loc.contains("taiping") || loc.contains("selama") || loc.contains("kerian") || loc.contains("parit buntar") || loc.contains("bagan serai")) return "PRK06";
        if (loc.contains("ipoh") || loc.contains("batu gajah") || loc.contains("kampar")) return "PRK02";
        if (loc.contains("tapah") || loc.contains("slim river") || loc.contains("tanjung malim")) return "PRK01";
        if (loc.contains("lumut") || loc.contains("sitiawan") || loc.contains("manjung") || loc.contains("teluk intan")) return "PRK05";
        if (loc.contains("putrajaya") || stationId.equals("CA17W")) return "WLY01";
        if (loc.contains("kuala lumpur") || loc.contains("cheras") || loc.contains("batu muda")) return "WLY01";
        if (loc.contains("labuan")) return "WLY02";
        if (loc.contains("melaka") || loc.contains("alor gajah") || loc.contains("rambai")) return "MLK01";
        if (loc.contains("petaling") || loc.contains("shah alam")) return "SGR01";
        if (loc.contains("klang") || loc.contains("banting")) return "SGR03";
        if (loc.contains("seremban") || loc.contains("nilai") || loc.contains("port dickson")) return "NGS02";
        if (loc.contains("johor") || loc.contains("larkin") || loc.contains("pasir gudang")) return "JHR02";
        if (loc.contains("batu pahat")) return "JHR04";
        if (loc.contains("kluang")) return "JHR03";
        if (loc.contains("pinang") || loc.contains("seberang") || loc.contains("minden")) return "PNG01";
        if (loc.contains("alor setar")) return "KDH01";
        if (loc.contains("sungai petani")) return "KDH02";
        if (loc.contains("langkawi")) return "KDH06";
        if (loc.contains("perlis") || loc.contains("kangar")) return "PLS01";
        if (loc.contains("kuantan")) return "PHG02";
        if (loc.contains("temerloh")) return "PHG03";
        if (loc.contains("terengganu") || loc.contains("kemaman")) return "TRG01";
        if (loc.contains("kota bharu") || loc.contains("kelantan")) return "KTN01";
        if (loc.contains("kota kinabalu")) return "SBH07";
        if (loc.contains("sandakan")) return "SBH01";
        if (loc.contains("tawau")) return "SBH04";
        if (loc.contains("kuching")) return "SWK08";
        if (loc.contains("miri")) return "SWK02";
        if (loc.contains("bintulu")) return "SWK03";
        if (loc.contains("sibu")) return "SWK04";

        return DEFAULT_ZONE; // Perak PRK06
    }

    static class PrayerItem {
        final String name;
        final String timeStr;
        final boolean isNext;
        final boolean isPast;

        PrayerItem(String name, String timeStr, boolean isNext, boolean isPast) {
            this.name = name;
            this.timeStr = timeStr;
            this.isNext = isNext;
            this.isPast = isPast;
        }
    }

    /** Returns all 6 prayers for today with formatted times and status. */
    static PrayerItem[] getPrayerItems(Context context) {
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now);
        if (times == null || times.length < 6) {
            times = calculateFallback(context, now);
        }
        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue; // Skip Syuruk
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0;

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        PrayerItem[] items = new PrayerItem[6];
        for (int i = 0; i < 6; i++) {
            items[i] = new PrayerItem(
                    PRAYER_NAMES[i],
                    timeFmt.format(new Date(times[i])),
                    i == nextIdx,
                    times[i] < nowMs
            );
        }
        return items;
    }

    /** Returns formatted clean UI CharSequence with highlighted next prayer, bold and legible. */
    static CharSequence getFormatted(Context context) {
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now);

        if (times == null || times.length < 6) {
            times = calculateFallback(context, now);
        }

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;

        // Find next prayer (skip Syuruk idx 1)
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue;
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0; // After Isyak -> Subuh next morning

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        SpannableStringBuilder sb = new SpannableStringBuilder();

        // LINE 1: LOCATION  •  NEXT: ASAR 16:23
        String nextName = PRAYER_NAMES[nextIdx].toUpperCase(Locale.ROOT);
        long displayTimeMs = times[nextIdx];
        if (nextIdx == 0 && times[0] < nowMs) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            long[] tomTimes = getTimesForToday(context, tomorrow);
            if (tomTimes == null || tomTimes.length < 6) tomTimes = calculateFallback(context, tomorrow);
            if (tomTimes != null && tomTimes.length > 0) displayTimeMs = tomTimes[0];
        }
        String nextTime = timeFmt.format(new Date(displayTimeMs));

        int l1Start = sb.length();
        String zoneHeader = getShortZoneName(context).toUpperCase(Locale.ROOT);
        sb.append(zoneHeader).append("  •  NEXT: ").append(nextName).append(" ").append(nextTime);
        int l1End = sb.length();
        sb.setSpan(new ForegroundColorSpan(0xFFFFFFFF), l1Start, l1End, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), l1Start, l1End, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sb.append("\n");

        // LINE 2: SUBUH 05:54  •  SYURUK 07:01  •  ZOHOR 13:04  •  ASAR 16:23  •  MAGHRIB 19:02  •  ISYAK 20:12
        int[] displayIndices = {0, 1, 2, 3, 4, 5}; // Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak
        for (int i = 0; i < displayIndices.length; i++) {
            int idx = displayIndices[i];
            boolean isNext = (idx == nextIdx);
            int start = sb.length();

            String name = PRAYER_NAMES[idx].toUpperCase(Locale.ROOT);
            String timeStr = timeFmt.format(new Date(times[idx]));

            sb.append(name).append(" ").append(timeStr);
            if (isNext) {
                sb.setSpan(new ForegroundColorSpan(0xFF4ADE80), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.setSpan(new ForegroundColorSpan(0xE6FFFFFF), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < displayIndices.length - 1) {
                int dotStart = sb.length();
                sb.append("  •  ");
                sb.setSpan(new ForegroundColorSpan(0x80FFFFFF), dotStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return sb;
    }

    private static long[] getTimesForToday(Context context, Calendar targetCal) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_JSON, null);
            if (jsonStr == null) return null;

            JSONObject obj = new JSONObject(jsonStr);
            int targetDay = targetCal.get(Calendar.DAY_OF_MONTH);

            // 1. Official e-solat.gov.my format: "prayerTime": [ { "date": "25-Okt-2026", "fajr": "05:54:00", ... } ]
            if (obj.has("prayerTime")) {
                JSONArray array = obj.getJSONArray("prayerTime");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    String dateStr = item.optString("date", "");
                    int dayNum = -1;
                    if (dateStr.contains("-")) {
                        try {
                            dayNum = Integer.parseInt(dateStr.substring(0, dateStr.indexOf('-')).trim());
                        } catch (Exception ignored) {}
                    }
                    if (dayNum == targetDay) {
                        long[] res = new long[6];
                        for (int k = 0; k < 6; k++) {
                            String timeStr = item.optString(PRAYER_KEYS[k], ""); // "05:54:00"
                            res[k] = parseTimeStringToMillis(targetCal, timeStr);
                        }
                        return res;
                    }
                }
            }

            // 2. Fallback waktusolat.app format: "prayers": [ { "day": 25, "fajr": 1729806840, ... } ]
            if (obj.has("prayers")) {
                JSONArray prayers = obj.getJSONArray("prayers");
                for (int i = 0; i < prayers.length(); i++) {
                    JSONObject p = prayers.getJSONObject(i);
                    if (p.optInt("day") == targetDay) {
                        long[] res = new long[6];
                        for (int k = 0; k < 6; k++) {
                            res[k] = p.getLong(PRAYER_KEYS[k]) * 1000L;
                        }
                        return res;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static long parseTimeStringToMillis(Calendar baseCal, String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            int second = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;

            Calendar cal = (Calendar) baseCal.clone();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long[] getTimesForToday(Context context, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, day);
        return getTimesForToday(context, cal);
    }

    /** Astronomical calculation fallback based on user's active Malaysian location. */
    private static long[] calculateFallback(Context context, Calendar cal) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = prefs.contains("manual_lat")
                ? Double.parseDouble(prefs.getString("manual_lat", String.valueOf(DEFAULT_LAT)))
                : DEFAULT_LAT;
        double longitude = prefs.contains("manual_lon")
                ? Double.parseDouble(prefs.getString("manual_lon", String.valueOf(DEFAULT_LON)))
                : DEFAULT_LON;

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        double d = 367.0 * year - Math.floor((7.0 * (year + Math.floor((month + 9.0) / 12.0))) / 4.0)
                + Math.floor((275.0 * month) / 9.0) + day - 730531.5;

        double L = (280.461 + 0.9856474 * d) % 360.0;
        if (L < 0) L += 360.0;
        double M = (357.528 + 0.9856003 * d) % 360.0;
        if (M < 0) M += 360.0;

        double lambda = L + 1.915 * Math.sin(Math.toRadians(M)) + 0.020 * Math.sin(Math.toRadians(2 * M));
        double epsilon = 23.439 - 0.0000004 * d;
        double alpha = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda)), Math.cos(Math.toRadians(lambda)))) / 15.0;
        double delta = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda))));

        double noon = 12.0 + TIMEZONE - (longitude / 15.0) - (L / 15.0 - alpha);
        if (noon < 0) noon += 24.0;
        if (noon >= 24) noon -= 24.0;

        double fajrHour = noon - sunAngleHour(-20.0, latitude, delta);
        double sunriseHour = noon - sunAngleHour(-0.833, latitude, delta);
        double asrHour = noon + asrHour(1, latitude, delta);
        double sunsetHour = noon + sunAngleHour(-0.833, latitude, delta);
        double ishaHour = noon + sunAngleHour(-18.0, latitude, delta);

        // Malaysia standard rounding & +2 min ihtiyat
        double[] hours = {fajrHour + 0.05, sunriseHour, noon + 0.04, asrHour + 0.04, sunsetHour + 0.05, ishaHour + 0.05};
        long[] res = new long[6];
        Calendar base = (Calendar) cal.clone();
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 6; i++) {
            int h = (int) hours[i];
            int m = (int) ((hours[i] - h) * 60.0);
            base.set(Calendar.HOUR_OF_DAY, h);
            base.set(Calendar.MINUTE, m);
            res[i] = base.getTimeInMillis();
        }
        return res;
    }

    private static double sunAngleHour(double angle, double lat, double delta) {
        double cosH = (Math.sin(Math.toRadians(angle)) - Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(delta)))
                / (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(delta)));
        cosH = Math.max(-1.0, Math.min(1.0, cosH));
        return Math.toDegrees(Math.acos(cosH)) / 15.0;
    }

    private static double asrHour(int shadowFactor, double lat, double delta) {
        double phi = Math.abs(lat - delta);
        double cotA = shadowFactor + Math.tan(Math.toRadians(phi));
        double angle = Math.toDegrees(Math.atan(1.0 / cotA));
        return sunAngleHour(angle, lat, delta);
    }

    static class ScheduleItem {
        final String name;
        final String timeFormatted;
        final boolean isNext;
        final boolean isPast;

        ScheduleItem(String name, String timeFormatted, boolean isNext, boolean isPast) {
            this.name = name;
            this.timeFormatted = timeFormatted;
            this.isNext = isNext;
            this.isPast = isPast;
        }
    }

    /** Returns full day prayer list for popup display. */
    static java.util.List<ScheduleItem> getFullSchedule(Context context) {
        java.util.List<ScheduleItem> list = new java.util.ArrayList<>();
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now);
        if (times == null || times.length < 6) times = calculateFallback(context, now);

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue; // Skip Syuruk (sunrise)
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        for (int i = 0; i < times.length; i++) {
            boolean isNext = (i == nextIdx);
            boolean isPast = (times[i] < nowMs);
            list.add(new ScheduleItem(PRAYER_NAMES[i], sdf.format(new Date(times[i])), isNext, isPast));
        }
        return list;
    }

    /** Returns short summary of next prayer (e.g. for screensaver). */
    static String getNextPrayerSummary(Context context) {
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now);
        if (times == null || times.length < 6) times = calculateFallback(context, now);

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue; // Skip Syuruk (sunrise)
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }

        long nextTimeMs;
        String prayerName;
        if (nextIdx != -1) {
            prayerName = PRAYER_NAMES[nextIdx];
            nextTimeMs = times[nextIdx];
        } else {
            // All prayers today have passed (after Isyak) -> Next is tomorrow's Subuh
            prayerName = PRAYER_NAMES[0];
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            long[] tomTimes = getTimesForToday(context, tomorrow);
            if (tomTimes == null || tomTimes.length < 6) {
                tomTimes = calculateFallback(context, tomorrow);
            }
            nextTimeMs = (tomTimes != null && tomTimes.length > 0) ? tomTimes[0] : (times[0] + 86400000L);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String zone = getShortZoneName(context);
        return prayerName + " • " + sdf.format(new Date(nextTimeMs)) + " (" + zone + ")";
    }

    /** Returns official Islamic Hijrah date (e.g. "14 Jamadilawal 1448H"). */
    static String getHijriDate(Context context) {
        if (lastKnownHijri != null && !lastKnownHijri.isEmpty()) {
            return lastKnownHijri;
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String cached = prefs.getString(KEY_HIJRI, null);
            if (cached != null && !cached.isEmpty()) {
                lastKnownHijri = cached;
                return cached;
            }
            String jsonStr = prefs.getString(KEY_JSON, null);
            if (jsonStr != null) {
                extractTodayHijri(jsonStr, prefs);
                if (lastKnownHijri != null) return lastKnownHijri;
            }
        }
        return getHijriDateFallback();
    }

    static String getHijriDate() {
        return getHijriDate(null);
    }

    private static String formatHijriString(String raw) {
        if (raw == null || !raw.contains("-")) return null;
        try {
            String[] parts = raw.split("-");
            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int day = Integer.parseInt(parts[2].trim());
            String[] months = {
                "Muharram", "Safar", "Rabiulawal", "Rabiulakhir",
                "Jamadilawal", "Jamadilakhir", "Rejab", "Syaaban",
                "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah"
            };
            String monthName = (month >= 1 && month <= 12) ? months[month - 1] : "Hijrah";
            return day + " " + monthName + " " + year + "H";
        } catch (Exception e) {
            return null;
        }
    }

    private static String getHijriDateFallback() {
        try {
            java.time.chrono.HijrahDate h = java.time.chrono.HijrahDate.now();
            int day = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
            int month = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
            int year = h.get(java.time.temporal.ChronoField.YEAR);
            String[] months = {
                "Muharram", "Safar", "Rabiulawal", "Rabiulakhir",
                "Jamadilawal", "Jamadilakhir", "Rejab", "Syaaban",
                "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah"
            };
            String monthName = (month >= 1 && month <= 12) ? months[month - 1] : "Hijrah";
            return day + " " + monthName + " " + year + "H";
        } catch (Exception e) {
            return "";
        }
    }
}
