package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AppLocationManager {
    private static final String TAG = "TvHome.Location";
    public static final String PREFS_NAME = "app_location_prefs";

    public static final String KEY_STATION_ID = "station_id";
    public static final String KEY_LOCATION_NAME = "location_name";
    public static final String KEY_STATION_PLACE = "station_place";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";

    // Defaults to Putrajaya (DOE CA17W)
    public static final String DEFAULT_STATION_ID = "CA17W";
    public static final String DEFAULT_LOCATION_NAME = "Putrajaya";
    public static final String DEFAULT_STATION_PLACE = "SK Putrajaya Presint 18 (2)";
    public static final double DEFAULT_LATITUDE = 2.9158;
    public static final double DEFAULT_LONGITUDE = 101.6909;

    public static class Station {
        public final String id;
        public final String name;
        public final String state;
        public final String place;
        public final double lat;
        public final double lon;

        public Station(String id, String name, String state, String place, double lat, double lon) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.place = place;
            this.lat = lat;
            this.lon = lon;
        }

        public String getDisplayName() {
            return name + " (" + state + ")";
        }
    }

    private static final List<Station> PRESET_STATIONS = new ArrayList<>();

    static {
        // W.P. Putrajaya
        PRESET_STATIONS.add(new Station("CA17W", "Putrajaya", "W.P. Putrajaya", "SK Putrajaya Presint 18 (2)", 2.9158, 101.6909));

        // W.P. Kuala Lumpur
        PRESET_STATIONS.add(new Station("CA16W", "Cheras", "W.P. Kuala Lumpur", "SMK Seri Permaisuri", 3.1063, 101.7179));
        PRESET_STATIONS.add(new Station("CA15W", "Batu Muda", "W.P. Kuala Lumpur", "SK Batu Muda", 3.2124, 101.6822));

        // Selangor
        PRESET_STATIONS.add(new Station("CA19B", "Petaling Jaya", "Selangor", "Taman Universiti", 3.1090, 101.6548));
        PRESET_STATIONS.add(new Station("CA20B", "Shah Alam", "Selangor", "Seksyen 4, Shah Alam", 3.0683, 101.5058));
        PRESET_STATIONS.add(new Station("CA21B", "Klang", "Selangor", "SMK Raja Lumu", 3.0108, 101.4081));
        PRESET_STATIONS.add(new Station("CA22B", "Banting", "Selangor", "Kolej MARA Banting", 2.8167, 101.6230));

        // Melaka
        PRESET_STATIONS.add(new Station("CA28M", "Bandaraya Melaka", "Melaka", "SMK Tinggi Melaka", 2.1909, 102.2571));
        PRESET_STATIONS.add(new Station("CA27M", "Bukit Rambai", "Melaka", "SK Tanjung Minyak 2", 2.2685, 102.1936));
        PRESET_STATIONS.add(new Station("CA26M", "Alor Gajah", "Melaka", "SMK Seri Pengkalan", 2.3709, 102.2232));

        // Negeri Sembilan
        PRESET_STATIONS.add(new Station("CA23N", "Seremban", "Negeri Sembilan", "SK Seremban 2B", 2.6953, 101.9056));
        PRESET_STATIONS.add(new Station("CA24N", "Nilai", "Negeri Sembilan", "SMK Desa Cempaka", 2.8128, 101.7989));
        PRESET_STATIONS.add(new Station("CA25N", "Port Dickson", "Negeri Sembilan", "SK Port Dickson", 2.5283, 101.7967));

        // Johor
        PRESET_STATIONS.add(new Station("CA30J", "Larkin (Johor Bahru)", "Johor", "Pusat Kraftangan Larkin", 1.4947, 103.7419));
        PRESET_STATIONS.add(new Station("CA32J", "Pasir Gudang", "Johor", "SK Pasir Gudang 4", 1.4703, 103.8961));
        PRESET_STATIONS.add(new Station("CA31J", "Batu Pahat", "Johor", "MRSM Batu Pahat", 1.9191, 102.8658));
        PRESET_STATIONS.add(new Station("CA33J", "Kluang", "Johor", "Sekolah Menengah Agama Kluang", 2.0306, 103.3214));
        PRESET_STATIONS.add(new Station("CA66J", "Tangkak", "Johor", "Tapak Semaian Majlis Daerah", 2.2957, 102.5713));

        // Pulau Pinang
        PRESET_STATIONS.add(new Station("CA08P", "Seberang Jaya", "Pulau Pinang", "SK Seberang Jaya II", 5.3942, 100.3983));
        PRESET_STATIONS.add(new Station("CA07P", "Minden", "Pulau Pinang", "Universiti Sains Malaysia", 5.3581, 100.2975));
        PRESET_STATIONS.add(new Station("CA09P", "Balik Pulau", "Pulau Pinang", "Kolej Vokasional Balik Pulau", 5.3371, 100.2166));

        // Perak
        PRESET_STATIONS.add(new Station("CA12A", "Ipoh (Tasek)", "Perak", "SK Tasek Damai", 4.6397, 101.1083));
        PRESET_STATIONS.add(new Station("CA11A", "Taiping", "Perak", "SK Seri Aman", 4.8569, 100.7417));
        PRESET_STATIONS.add(new Station("CA13A", "Seri Manjung", "Perak", "SK Seri Manjung", 4.1953, 100.6728));

        // Kedah & Perlis
        PRESET_STATIONS.add(new Station("CA03K", "Alor Setar", "Kedah", "SMK Agama Kedah", 6.1371, 100.3469));
        PRESET_STATIONS.add(new Station("CA04K", "Sungai Petani", "Kedah", "SMK Tunku Ismail", 5.6297, 100.4677));
        PRESET_STATIONS.add(new Station("CA02K", "Langkawi", "Kedah", "Kompleks Sukan Langkawi", 6.3316, 99.8585));
        PRESET_STATIONS.add(new Station("CA01R", "Kangar", "Perlis", "Institut Latihan Perindustrian", 6.4299, 100.2109));

        // Pahang, Terengganu & Kelantan
        PRESET_STATIONS.add(new Station("CA40C", "Kuantan (Indera Mahkota)", "Pahang", "SK Indera Mahkota", 3.8192, 103.2966));
        PRESET_STATIONS.add(new Station("CA38C", "Temerloh", "Pahang", "SK Temerloh", 3.4478, 102.4178));
        PRESET_STATIONS.add(new Station("CA44T", "Kuala Terengganu", "Terengganu", "SK Seri Budiman", 5.3117, 103.1325));
        PRESET_STATIONS.add(new Station("CA42T", "Kemaman", "Terengganu", "SMK Bukit Kuang", 4.2622, 103.4258));
        PRESET_STATIONS.add(new Station("CA46D", "Kota Bharu", "Kelantan", "SMK Tanjung Mas", 6.1256, 102.2386));

        // Sabah & Sarawak
        PRESET_STATIONS.add(new Station("CA48S", "Kota Kinabalu", "Sabah", "Politeknik Kota Kinabalu", 6.0971, 116.1665));
        PRESET_STATIONS.add(new Station("CA49S", "Sandakan", "Sabah", "SMK Perempuan Sandakan", 5.8456, 118.0678));
        PRESET_STATIONS.add(new Station("CA50S", "Tawau", "Sabah", "SMK Tawau", 4.2564, 117.8931));
        PRESET_STATIONS.add(new Station("CA54Q", "Kuching", "Sarawak", "Cawangan Farmasi Logistik", 1.5622, 110.3889));
        PRESET_STATIONS.add(new Station("CA55Q", "Miri", "Sarawak", "ILP Miri", 4.4947, 114.0434));
        PRESET_STATIONS.add(new Station("CA58Q", "Bintulu", "Sarawak", "Balai Polis Bintulu", 3.1772, 113.0412));
        PRESET_STATIONS.add(new Station("CA59Q", "Sibu", "Sarawak", "SK Bandaran Sibu No. 4", 2.2981, 111.8344));
        PRESET_STATIONS.add(new Station("CA53L", "Labuan", "W.P. Labuan", "Kolej Matrikulasi Labuan", 5.3347, 115.2289));
    }

    public static List<Station> getPresetStations() {
        return Collections.unmodifiableList(PRESET_STATIONS);
    }

    public static String getStationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_STATION_ID, DEFAULT_STATION_ID);
    }

    public static String getLocationName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOCATION_NAME, DEFAULT_LOCATION_NAME);
    }

    public static String getStationPlace(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_STATION_PLACE, DEFAULT_STATION_PLACE);
    }

    public static double getLatitude(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            return Double.parseDouble(prefs.getString(KEY_LATITUDE, String.valueOf(DEFAULT_LATITUDE)));
        } catch (Exception ignored) {
            return DEFAULT_LATITUDE;
        }
    }

    public static double getLongitude(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            return Double.parseDouble(prefs.getString(KEY_LONGITUDE, String.valueOf(DEFAULT_LONGITUDE)));
        } catch (Exception ignored) {
            return DEFAULT_LONGITUDE;
        }
    }

    public static void setStation(Context context, Station s) {
        if (s == null) return;
        setCustomLocation(context, s.id, s.name, s.place, s.lat, s.lon);
    }

    public static void setCustomLocation(Context context, String stationId, String locationName, String place, double lat, double lon) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_STATION_ID, stationId)
                .putString(KEY_LOCATION_NAME, locationName)
                .putString(KEY_STATION_PLACE, place)
                .putString(KEY_LATITUDE, String.valueOf(lat))
                .putString(KEY_LONGITUDE, String.valueOf(lon))
                .apply();

        Log.i(TAG, "Location set to: " + locationName + " (" + stationId + ") [" + lat + ", " + lon + "]");

        // Trigger immediate sync for Weather & Prayer Times
        WeatherAirQuality.sync(context, null);
        PrayerTimes.sync(context, null);
    }

    public static Station findStationByQuery(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Station s : PRESET_STATIONS) {
            if (s.id.equalsIgnoreCase(q) || s.name.toLowerCase(Locale.ROOT).contains(q) || s.state.toLowerCase(Locale.ROOT).contains(q)) {
                return s;
            }
        }
        return null;
    }

    public static Station findNearestStation(double lat, double lon) {
        Station best = null;
        double minDistance = Double.MAX_VALUE;
        for (Station s : PRESET_STATIONS) {
            double dLat = s.lat - lat;
            double dLon = s.lon - lon;
            double dist = dLat * dLat + dLon * dLon;
            if (dist < minDistance) {
                minDistance = dist;
                best = s;
            }
        }
        return best != null ? best : PRESET_STATIONS.get(0);
    }
}
