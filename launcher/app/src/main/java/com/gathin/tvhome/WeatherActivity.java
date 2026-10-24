package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class WeatherActivity extends Activity {

    private TextView weatherLocation;
    private ImageView weatherMainIcon;
    private TextView weatherMainTemp;
    private TextView weatherMainCondition;
    private TextView weatherMainSub;
    private TextView weatherDetail1;
    private TextView weatherDetail2;

    private TextView aqiValue;
    private TextView aqiStatus;
    private TextView aqiStation;
    private View aqiBar;
    private View aqiIndicator;
    private TextView aqiStationsSub;
    private TextView aqiModelSub;
    private TextView aqiAdvisory;

    private LinearLayout hourlyContainer;
    private LinearLayout dailyContainer;
    private TextView weatherFooter;
    private View btnChangeLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherLocation = findViewById(R.id.weatherLocation);
        btnChangeLocation = findViewById(R.id.btnChangeLocation);
        weatherMainIcon = findViewById(R.id.weatherMainIcon);
        weatherMainTemp = findViewById(R.id.weatherMainTemp);
        weatherMainCondition = findViewById(R.id.weatherMainCondition);
        weatherMainSub = findViewById(R.id.weatherMainSub);
        weatherDetail1 = findViewById(R.id.weatherDetail1);
        weatherDetail2 = findViewById(R.id.weatherDetail2);

        aqiValue = findViewById(R.id.aqiValue);
        aqiStatus = findViewById(R.id.aqiStatus);
        aqiStation = findViewById(R.id.aqiStation);
        aqiBar = findViewById(R.id.aqiBar);
        aqiIndicator = findViewById(R.id.aqiIndicator);
        aqiStationsSub = findViewById(R.id.aqiStationsSub);
        aqiModelSub = findViewById(R.id.aqiModelSub);
        aqiAdvisory = findViewById(R.id.aqiAdvisory);

        hourlyContainer = findViewById(R.id.hourlyContainer);
        dailyContainer = findViewById(R.id.dailyContainer);
        weatherFooter = findViewById(R.id.weatherFooter);

        if (btnChangeLocation != null) {
            btnChangeLocation.setOnClickListener(v -> showLocationPickerDialog());
            setupFocusAnimation(btnChangeLocation);
        }

        populateUi();

        // Background sync to ensure freshest data
        WeatherAirQuality.syncIfDue(this, this::populateUi);
    }

    private void showLocationPickerDialog() {
        final List<AppLocationManager.Station> stations = AppLocationManager.getPresetStations();
        String currentId = AppLocationManager.getStationId(this);
        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            AppLocationManager.Station s = stations.get(i);
            items[i] = s.getDisplayName() + "\n" + s.place;
            if (s.id.equalsIgnoreCase(currentId)) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("📍 Select Air Quality / Haze Station");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            AppLocationManager.Station chosen = stations.get(which);
            AppLocationManager.setStation(WeatherActivity.this, chosen);
            dialog.dismiss();
            Toast.makeText(WeatherActivity.this, "Station set to: " + chosen.name, Toast.LENGTH_SHORT).show();
            populateUi();
            // Sync in background to update readings immediately
            WeatherAirQuality.sync(WeatherActivity.this, () -> runOnUiThread(WeatherActivity.this::populateUi));
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void populateUi() {
        WeatherAirQuality.FullWeather w = WeatherAirQuality.getFullWeather(this);
        if (w == null) return;

        boolean isMalay = Locale.getDefault().getLanguage().startsWith("ms");

        weatherLocation.setText(w.location);
        weatherMainTemp.setText(w.temp + "°");
        weatherMainCondition.setText(w.condition);
        weatherMainSub.setText(String.format(Locale.ROOT, "%s %d°  •  H %d° L %d°",
                isMalay ? "Terasa seperti" : "Feels like",
                w.apparentTemp, w.highTemp, w.lowTemp));
        weatherMainIcon.setImageResource(getIconDrawable(w.icon));

        weatherDetail1.setText(String.format(Locale.ROOT, "%s %d%%  •  %s %d km/h  •  UV %d",
                isMalay ? "Kelembapan" : "Humidity", w.humidity,
                isMalay ? "Angin" : "Wind", w.windSpeed, w.uvIndex));
        weatherDetail2.setText(String.format(Locale.ROOT, "%s %d%%  •  %s %s  •  %s %s",
                isMalay ? "Kebarangkalian hujan" : "Rain chance today", w.rainChanceToday,
                isMalay ? "Terbit" : "Sunrise", w.sunrise,
                isMalay ? "Terbenam" : "Sunset", w.sunset));

        // Air Quality
        aqiValue.setText(String.valueOf(w.aqi));
        aqiValue.setTextColor(w.aqiColor);
        aqiStatus.setText(w.aqiStatus);
        aqiStatus.setTextColor(w.aqiColor);

        String param = (w.aqiParam != null && !w.aqiParam.isEmpty()) ? w.aqiParam : "PM2.5";
        String place = (w.stationPlace != null && !w.stationPlace.isEmpty()) ? w.stationPlace : w.location;
        aqiStation.setText("JAS IPU  •  " + place + "  •  " + param);

        // Scale bar indicator position
        aqiBar.post(() -> {
            int barWidth = aqiBar.getWidth();
            int indWidth = aqiIndicator.getWidth();
            if (barWidth > 0) {
                float fraction = Math.min(1.0f, Math.max(0.0f, (w.aqi - 10f) / 250f));
                float transX = fraction * (barWidth - indWidth);
                aqiIndicator.setTranslationX(transX);
            }
        });

        // Current station info
        aqiStationsSub.setText(String.format(Locale.ROOT, "%s: %s  •  ID: %s",
                isMalay ? "Stesen Pemantauan" : "Monitoring Station", place, w.stationId));

        if (w.isJasSource) {
            aqiModelSub.setText("Sumber Rasmi: Jabatan Alam Sekitar Malaysia (APIMS/MyEQMS)");
        } else {
            aqiModelSub.setText(String.format(Locale.ROOT, "Model Open-Meteo Air Quality  •  PM2.5 %.1f  •  PM10 %.1f µg/m³",
                    w.pm25, w.pm10));
        }

        // Health advisory based on AQI
        if (w.aqi <= 50) {
            aqiAdvisory.setText(isMalay
                    ? "Kualiti udara baik. Aktiviti riadah luar boleh dijalankan seperti biasa."
                    : "Air quality is satisfactory. Normal outdoor activities can be enjoyed.");
        } else if (w.aqi <= 100) {
            aqiAdvisory.setText(isMalay
                    ? "Kualiti udara sederhana. Tiada sekatan aktiviti bagi kebanyakan orang."
                    : "Normal activities. People with heart or lung conditions should limit long outdoor exertion.");
        } else if (w.aqi <= 200) {
            aqiAdvisory.setText(isMalay
                    ? "Kualiti udara TIDAK SIHAT. Golongan berisiko (asma, kanak-kanak, warga emas) kurangkan aktiviti luar."
                    : "Unhealthy. Sensitive groups should reduce prolonged or heavy outdoor exertion.");
        } else if (w.aqi <= 300) {
            aqiAdvisory.setText(isMalay
                    ? "Kualiti udara SANGAT TIDAK SIHAT. Elakkan sebarang aktiviti luar. Tutup tingkap."
                    : "Very Unhealthy. Everyone should avoid outdoor exertion. Keep windows closed.");
        } else {
            aqiAdvisory.setText(isMalay
                    ? "BERBAHAYA! Kekal di dalam rumah. Patuhi amaran pihak berkuasa."
                    : "Hazardous! Avoid all physical activity outdoors. Remain indoors.");
        }

        // Hourly row (Next 12 Hours)
        hourlyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < w.hourly.size(); i++) {
            WeatherAirQuality.HourlyItem item = w.hourly.get(i);
            View v = inflater.inflate(R.layout.item_weather_hourly, hourlyContainer, false);
            TextView timeView = v.findViewById(R.id.hourlyTime);
            ImageView iconView = v.findViewById(R.id.hourlyIcon);
            TextView tempView = v.findViewById(R.id.hourlyTemp);
            TextView rainView = v.findViewById(R.id.hourlyRain);

            timeView.setText(item.time);
            iconView.setImageResource(getIconDrawable(item.icon));
            tempView.setText(item.temp + "°");
            rainView.setText("↑ " + item.rainChance + "%");

            setupFocusAnimation(v);
            hourlyContainer.addView(v);

            // Set initial focus to the first item for instant D-pad navigation
            if (i == 0) {
                v.requestFocus();
            }
        }

        // 7 Days row
        dailyContainer.removeAllViews();
        for (int i = 0; i < w.daily.size(); i++) {
            WeatherAirQuality.DailyItem item = w.daily.get(i);
            View v = inflater.inflate(R.layout.item_weather_daily, dailyContainer, false);
            TextView dayView = v.findViewById(R.id.dailyDay);
            ImageView iconView = v.findViewById(R.id.dailyIcon);
            TextView tempView = v.findViewById(R.id.dailyTemp);
            TextView rainView = v.findViewById(R.id.dailyRain);

            dayView.setText(item.day);
            iconView.setImageResource(getIconDrawable(item.icon));
            tempView.setText(item.maxTemp + "° / " + item.minTemp + "°");
            rainView.setText("↑ " + item.rainChance + "%");

            setupFocusAnimation(v);
            dailyContainer.addView(v);
        }

        weatherFooter.setText(String.format(Locale.ROOT,
                "Updated %s  •  API: Jabatan Alam Sekitar (APIMS)  •  Weather & Forecast: Open-Meteo",
                w.updatedTime));
    }

    private void setupFocusAnimation(View v) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.06f : 1.0f;
            float elev = hasFocus ? 8f : 0f;
            view.animate().scaleX(scale).scaleY(scale).translationZ(elev).setDuration(150).start();
        });
    }

    private int getIconDrawable(String icon) {
        if (icon == null) return R.drawable.ic_weather_partly_cloudy;
        if (icon.contains("☀")) return R.drawable.ic_weather_sun;
        if (icon.contains("⛅")) return R.drawable.ic_weather_partly_cloudy;
        if (icon.contains("🌧")) return R.drawable.ic_weather_rain;
        if (icon.contains("⛈")) return R.drawable.ic_weather_thunder;
        if (icon.contains("☁") || icon.contains("🌫")) return R.drawable.ic_weather_cloud;
        return R.drawable.ic_weather_partly_cloudy;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
