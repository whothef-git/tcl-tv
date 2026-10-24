package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gathin.tvhome.qrcode.QrBitmapUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tvOS style home: black, a pinned dock row, an app grid, and one glass highlight that springs between tiles.
 */
public class MainActivity extends Activity implements AppAdapter.TileListener {

    private static java.lang.ref.WeakReference<MainActivity> sInstance;

    public static MainActivity getInstance() {
        return sInstance != null ? sInstance.get() : null;
    }

    /** Tiles per row and dock slots (res/values/config.xml, columns). Set in onCreate. */
    static int COLUMNS = 4;
    private static final long MINUTE_MS = 60_000L;
    private String liveTvPkg;
    private String liveTvAction;
    private String inputAction;
    private String sourcePanelService;
    private String sourcePanelAction;

    private AppAdapter adapter;
    private RecyclerView grid;
    private FrameLayout root;
    private GlassView glass;
    private View ambient;
    /** The focus: one dark shape gliding between tiles and pills (see Glider). */
    private Glider glider;
    /** Whether the glider is on the top menu, so a move down travels dark. */
    private boolean gliderOnPill;
    private final int[] pillAt = new int[2], rootAt = new int[2];
    /** The top-right buttons and the two rectangles used to line them up with the dock. */
    private View topRow, topSettings;
    private final Rect alignCard = new Rect(), alignButton = new Rect();
    /** How much of the space between the side paddings the top shelf may use. */
    private static final float HERO_WIDTH_SHARE = 0.40f;
    /** The name is one line, and may run wider than the description, up to this share. */
    private static final float HERO_NAME_WIDTH_SHARE = 0.60f;
    private static final float HERO_TITLE_MAX_SP = 40f;
    /** The breathing room between the shelf's last line and the top of the app container. */
    private static final float HERO_GAP_DP = 36f;
    private static final float HERO_TITLE_MIN_SP = 26f;
    /** The shelf's width in pixels, worked out once from the screen. */
    private int heroMaxWidth, heroNameMaxWidth;
    private View heroShelf;
    private TextView heroName;
    private TextView heroDesc;
    private TextView heroWhere;
    /** The place on screen and a few lines about it; null for the user's own wallpapers. */
    private String placeName;
    private String placeText = "", placeCountry = "";
    private int ambientColor = -1;
    private boolean heroShown;
    /** The glow behind the tiles. It follows every app, while the shelf only names dock apps. */
    private boolean ambientShown;

    /** The colour the glass is blending from, how far along it is, and the blend itself. */
    private int glowFrom;
    private float glowMix = 1f;
    private android.animation.ValueAnimator glowBlend;
    private Bitmap ambientShape;
    /** Where the shelf's bottom edge belongs; its top follows once the lines have been measured. */
    private float heroBottom;
    private View btnInputs;
    private TextView btnWifi;
    private TextView ampmView, dateView;
    private TextView shamsiDate;
    private TextView prayerTimesView;
    private TextView weatherAirQualityView;
    private View islamicShelf;
    private TextView ayatRef, ayatArabic, ayatTranslation, prayerHeader;
    private TextView lblSubuh, lblSyuruk, lblZohor, lblAsar, lblMaghrib, lblIsyak;
    private TextView timeSubuh, timeSyuruk, timeZohor, timeAsar, timeMaghrib, timeIsyak;
    private View arsenalMatchCard;
    private TextView arsenalCardHeader, arsenalMatchOpponent, arsenalLiveScore, arsenalMatchCompetition, arsenalMatchTime, arsenalMatchCountdown;
    private boolean shamsi;
    private String vpnApp;
    private SpringAnimation springX, springY;
    private float glassTargetX, glassTargetY;
    // How far the grid has scrolled the glass. springY works without it, in grid content space, so a
    // scroll moves the glass in lockstep with its tile instead of becoming a moving spring target.
    private float glassScrollShift;
    private boolean glassShown;
    // Static so they survive the activity being destroyed while another app is open (low-memory TVs,
    // or Developer options → "Don't keep activities"); they only reset when the process dies.
    private static int lastFocused = 0;
    /** App of the last focused tile, so focus follows it when the app list changes. */
    private static String lastFocusedKey;
    private float density;
    /** Module cards (res/values/config.xml, module_cards): discovered on resume, shown only after the owner's Show. */
    private boolean moduleCards;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): stiff spring, short fades, no replay. */
    private boolean reducedMotion;
    /** hidden_apps as the adapter sees them (config.xml or a theme's config.json): a hidden app gets no card either. */
    private Set<String> hiddenApps = Collections.emptySet();
    private List<Cards.Card> cards = Collections.emptyList();
    /** The grid has been laid out and focused since the last resume; the card prompt waits for this. */
    private boolean tilesReady;
    private boolean tilesAnimatedOnce;
    /** Grid rows left showing under the dock (config rows_below_dock). */
    private int rowsBelowDockSetting;
    /** The longest Home holds its first frame while it puts itself together (see holdFirstFrame). */
    private static final long FIRST_FRAME_WAIT_MS = 1200;
    private static final long RETURN_SETTLE_MS = 6000;
    /** True while Home rebuilds on return; focus passes through the pills then and must not light them. */
    private boolean resuming;
    /** True while Home's first frame is held back (see holdFirstFrame). */
    private boolean firstFrameHeld;
    private Dialog cardPrompt;
    /** Set when a decision could not be saved: no more prompts until the next resume, instead of asking in a loop. */
    private boolean promptsPaused;
    /** The Inputs or Module cards dropdown while one is open, so onPause can close it. */
    private PopupWindow openPopup;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Rotating wallpaper (Config.wallpapers), the view that shows it and the bitmap on screen. */
    private List<File> wallpapers = Collections.emptyList();
    private long wallpaperIntervalMs;
    /** Decodes the next picture off the main thread, so a switch never stutters the D-pad animations. */
    private final java.util.concurrent.ExecutorService wallDecoder = java.util.concurrent.Executors.newSingleThreadExecutor();
    private long wallShownAt;
    private android.widget.ImageView wallView;
    private android.graphics.Bitmap wallBitmap;
    /** Settings kept for the places refresh, which runs after onCreate. */
    private Config config;
    /** Pictures of places are on (see PlaceWallpapers); they are dimmed behind the tiles. */
    private boolean placesMode;
    private volatile boolean placesRefreshRunning;
    private static final long SCREENSAVER_TIMEOUT_MS = 10L * 60 * 1000L; // 10 minutes idle -> screensaver
    private static final long TV_SLEEP_TIMEOUT_MS = SCREENSAVER_TIMEOUT_MS + (2L * 60 * 60 * 1000L); // 2 hours later -> TV off
    private long lastUserInteraction = android.os.SystemClock.elapsedRealtime();
    private boolean screensaverActive = false;
    private View screensaverView;
    private boolean wallpaperFullscreen = false;
    private View mainContent;
    private static final long DOUBLE_BACK_DELAY_MS = 350L;
    private Runnable singleBackRunnable = null;
    private boolean doubleBackPending = false;
    private static final long DOUBLE_HOME_INTERVAL_MS = 400L;
    private long lastHomePressTime = 0;
    private Runnable singleHomeRunnable = null;
    /** Refreshes the network pill when the connection changes: at boot Wi-Fi often connects after Home is shown. */
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    /** Shows the Inputs pill once the TV registers an input: at boot HDMI inputs can appear after Home is shown. */
    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputAdded(String inputId) {
            updateInputsButton();
        }

        @Override
        public void onInputRemoved(String inputId) {
            updateInputsButton();
        }
    };
    /** Listens for any VPN coming up or going down, even one this app's traffic is excluded from. */
    private final ConnectivityManager.NetworkCallback vpnCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    private final IntentFilter packageFilter = new IntentFilter();
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Updates send REMOVED followed by ADDED; keep the tile until the new APK is ready.
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            reloadApps(false);
        }
    };
    private final Runnable minuteTick = new Runnable() {
        @Override
        public void run() {
            updateShamsiDate();
            updatePrayerTimes();
            updateDateAndPeriod();
            updateWifiLabel();
            updateWeatherAqi();
            WeatherAirQuality.syncIfDue(MainActivity.this, MainActivity.this::updateWeatherAqi);
            updateIslamicShelf();
            updateArsenalCard();
            ArsenalMatch.syncIfDue(MainActivity.this, MainActivity.this::updateArsenalCard);
            refreshPlacesIfDue();
            refreshOnlineWallpapersIfDue();
            checkInactivity();
            handler.postDelayed(this, MINUTE_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = new java.lang.ref.WeakReference<>(this);
        SoundEffects.init(this);
        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;
        // config.xml, overlaid by config.json from a pushed theme (see Config and tools/theme.sh).
        Config cfg = Config.load(this);
        COLUMNS = cfg.columns;
        reducedMotion = cfg.reducedMotion;
        liveTvPkg = cfg.liveTvPackage;
        liveTvAction = cfg.liveTvAction;
        inputAction = cfg.inputAction;
        sourcePanelService = cfg.sourcePanelService;
        sourcePanelAction = cfg.sourcePanelAction;

        root = findViewById(R.id.root);
        // The tiles' entrance plays once per TV boot. Android reclaims the launcher during long stays
        // in big apps; starting again then is a return to Home, not a first launch.
        long bootAt = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        android.content.SharedPreferences launch = getSharedPreferences("launch", MODE_PRIVATE);
        if (Math.abs(launch.getLong("boot_at", 0) - bootAt) < 60_000) tilesAnimatedOnce = true;
        else launch.edit().putLong("boot_at", bootAt).apply();
        ensureSystemScreensaverAndSleepSettings();
        root.setBackgroundColor(cfg.backgroundColor);
        config = cfg;
        wallpapers = cfg.wallpapers;
        wallpaperIntervalMs = cfg.wallpaperIntervalMs;
        placesMode = cfg.places;
        mainContent = findViewById(R.id.mainContent);
        glass = findViewById(R.id.glass);
        ambient = findViewById(R.id.ambient);
        Backdrop.setWorker(wallDecoder, handler);
        heroShelf = findViewById(R.id.heroShelf);
        heroName = findViewById(R.id.heroName);
        heroDesc = findViewById(R.id.heroDesc);
        heroWhere = findViewById(R.id.heroWhere);
        int usable = getResources().getDisplayMetrics().widthPixels - (int) ((55 + 56) * density);
        heroMaxWidth = Math.round(usable * HERO_WIDTH_SHARE);
        heroNameMaxWidth = Math.round(usable * HERO_NAME_WIDTH_SHARE);
        if (heroName != null) heroName.setMaxWidth(heroNameMaxWidth);
        if (heroDesc != null) heroDesc.setMaxWidth(heroMaxWidth);

        String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");
        if ("Madinah".equalsIgnoreCase(activeCat)) {
            placesMode = false;
            wallpaperIntervalMs = 15_000L;
            List<File> madinahFiles = MadinahWallpapers.list(this);
            if (!madinahFiles.isEmpty()) {
                wallpapers = madinahFiles;
            }
        } else if ("Custom".equalsIgnoreCase(activeCat)) {
            placesMode = false;
            File customDir = new File(getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            boolean singleMode = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getBoolean("custom_single_mode", false);
            String singleFile = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("custom_single_file", null);
            if (singleMode && singleFile != null) {
                File sf = new File(customDir, singleFile);
                if (sf.exists() && sf.length() > 0) {
                    wallpapers = Collections.singletonList(sf);
                } else if (files != null && files.length > 0) {
                    Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    wallpapers = new ArrayList<>(Arrays.asList(files));
                }
            } else if (files != null && files.length > 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                wallpapers = new ArrayList<>(Arrays.asList(files));
            } else if (cfg.wallpaperOnline && wallpapers.isEmpty()) {
                wallpapers = OnlineWallpapers.list(this);
            }
        } else if (!"Natural".equalsIgnoreCase(activeCat)) {
            placesMode = false;
            List<File> catFiles = OnlineWallpapers.listByCategory(this, activeCat);
            if (!catFiles.isEmpty()) wallpapers = catFiles;
            else if (cfg.wallpaperOnline && wallpapers.isEmpty()) wallpapers = OnlineWallpapers.list(this);
        } else if (placesMode) {
            wallpapers = PlaceWallpapers.list(this);
        } else if (cfg.wallpaperOnline && wallpapers.isEmpty()) {
            wallpapers = OnlineWallpapers.list(this);
        }
        if (!wallpapers.isEmpty()) showNextWallpaper(false);
        else if (cfg.wallpaper != null && !placesMode) setWallpaper(decodeWallpaper(cfg.wallpaper), false);
        // The shelf grows and shrinks with the name and the description, so it hangs from its
        // bottom edge: every time it is measured, its top is worked out again. Positioning it by
        // the height it happened to have beforehand is what let a tall block run into the dock.
        heroShelf.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            if (heroBottom > 0) v.setY(heroBottom - v.getHeight());
        });
        ampmView = findViewById(R.id.ampm);
        dateView = findViewById(R.id.date);
        updateDateAndPeriod();
        weatherAirQualityView = findViewById(R.id.weatherAirQuality);
        if (weatherAirQualityView != null) {
            weatherAirQualityView.setOnClickListener(v -> {
                startActivity(new Intent(this, WeatherActivity.class));
            });
            weatherAirQualityView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusTile(0);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    View target = (btnInputs != null && btnInputs.getVisibility() == View.VISIBLE) ? btnInputs
                            : (btnWifi != null && btnWifi.getVisibility() == View.VISIBLE) ? btnWifi
                            : findViewById(R.id.btnSettings);
                    if (target != null) {
                        target.requestFocus();
                        return true;
                    }
                }
                return false;
            });
            weatherAirQualityView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.05f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
            });
        }
        updateWeatherAqi();
        WeatherAirQuality.syncIfDue(this, this::updateWeatherAqi);

        islamicShelf = findViewById(R.id.islamicShelf);
        if (islamicShelf != null) {
            ayatRef = findViewById(R.id.ayatRef);
            ayatArabic = findViewById(R.id.ayatArabic);
            ayatTranslation = findViewById(R.id.ayatTranslation);
            prayerHeader = findViewById(R.id.prayerHeader);

            lblSubuh = findViewById(R.id.lblSubuh);
            lblSyuruk = findViewById(R.id.lblSyuruk);
            lblZohor = findViewById(R.id.lblZohor);
            lblAsar = findViewById(R.id.lblAsar);
            lblMaghrib = findViewById(R.id.lblMaghrib);
            lblIsyak = findViewById(R.id.lblIsyak);

            timeSubuh = findViewById(R.id.timeSubuh);
            timeSyuruk = findViewById(R.id.timeSyuruk);
            timeZohor = findViewById(R.id.timeZohor);
            timeAsar = findViewById(R.id.timeAsar);
            timeMaghrib = findViewById(R.id.timeMaghrib);
            timeIsyak = findViewById(R.id.timeIsyak);

            islamicShelf.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
                if (heroBottom > 0) v.setY(heroBottom - v.getHeight());
            });
            updateIslamicShelf();
            updateArsenalCard();
            ArsenalMatch.syncIfDue(MainActivity.this, MainActivity.this::updateArsenalCard);
        }

        btnWifi = findViewById(R.id.btnWifi);
        if (!cfg.showNetworkPill) btnWifi.setVisibility(View.GONE);
        shamsiDate = findViewById(R.id.shamsiDate);
        shamsi = cfg.shamsiDate;
        if (shamsi) {
            shamsiDate.setVisibility(View.VISIBLE);
            TextView date = findViewById(R.id.date);
            date.setTextSize(11f);
            date.setPadding(0, (int) (4 * density), 0, 0);
        }
        prayerTimesView = findViewById(R.id.prayerTimes);
        if (prayerTimesView != null) {
            prayerTimesView.setOnClickListener(v -> showPrayerPopup());
            prayerTimesView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    View inputs = findViewById(R.id.btnInputs);
                    View wifi = findViewById(R.id.btnWifi);
                    View settings = findViewById(R.id.btnSettings);
                    if (inputs != null && inputs.getVisibility() == View.VISIBLE) inputs.requestFocus();
                    else if (wifi != null && wifi.getVisibility() == View.VISIBLE) wifi.requestFocus();
                    else if (settings != null) settings.requestFocus();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusTile(lastFocused % COLUMNS);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    showPrayerPopup();
                    return true;
                }
                return false;
            });
        }
        updatePrayerTimes();
        PrayerTimes.syncIfDue(this, this::updatePrayerTimes);

        arsenalMatchCard = findViewById(R.id.arsenalMatchCard);
        arsenalCardHeader = findViewById(R.id.arsenalCardHeader);
        arsenalMatchOpponent = findViewById(R.id.arsenalMatchOpponent);
        arsenalLiveScore = findViewById(R.id.arsenalLiveScore);
        arsenalMatchCompetition = findViewById(R.id.arsenalMatchCompetition);
        arsenalMatchTime = findViewById(R.id.arsenalMatchTime);
        arsenalMatchCountdown = findViewById(R.id.arsenalMatchCountdown);
        if (arsenalMatchCard != null) {
            arsenalMatchCard.setFocusable(false);
            arsenalMatchCard.setClickable(false);
        }
        updateArsenalCard();
        ArsenalMatch.syncIfDue(this, this::updateArsenalCard);
        vpnApp = cfg.vpnApp;
        moduleCards = cfg.moduleCards;
        hiddenApps = new HashSet<>(cfg.hiddenApps);

        grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, COLUMNS) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                // The dock's line is set before any row is placed. Set afterwards, the grid kept its
                // rows where the first layout had put them, and Home came back with the dock and its
                // tray hundreds of pixels up the screen until focus slid them down.
                parkDock();
                super.onLayoutChildren(recycler, state);
            }
        });
        grid.setHasFixedSize(true);
        grid.setItemAnimator(null);
        DockDecoration tray = new DockDecoration(COLUMNS, density);
        grid.addItemDecoration(tray);
        // The top-right buttons line up with the dock, which is only known once the tiles are laid out.
        grid.getViewTreeObserver().addOnGlobalLayoutListener(this::alignTopButtons);
        // Glass shows the blurred wallpaper behind it: the dock tray (drawn by the grid) and the
        // top-bar pills redraw when a new blurred copy is ready.
        Backdrop.watch(grid);
        setUpGlider();
        holdFirstFrame();
        for (int i = 0; i < grid.getItemDecorationCount(); i++) {
            if (grid.getItemDecorationAt(i) instanceof DockDecoration) {
                ((DockDecoration) grid.getItemDecorationAt(i)).setGlider(glider);
            }
        }
        // The grid shows the ambient backlight and top cursor highlight across all items.
        glider.addCompanion(grid, null);
        for (int id : new int[]{R.id.btnInputs, R.id.btnWifi, R.id.btnSettings}) {
            View pill = findViewById(id);
            if (pill == null) continue;
            float radius = 21f * density;
            pill.setBackground(new GlassDrawable(pill, radius, density, glider));
            glider.addCompanion(pill);
            Backdrop.watch(pill);
            pill.setOnFocusChangeListener((v, has) -> {
                if (has) moveTopHighlight(v);
            });
        }
        if (prayerTimesView != null) {
            prayerTimesView.setOnFocusChangeListener((v, has) -> {
                if (has) {
                    SoundEffects.playNav();
                    glider.hide();
                }
            });
        }
        // The apps rise into place on the first launch after the TV starts, and only then. Setting
        // the animation arms it for the grid's first draw, so on any later fresh start (Android
        // closing Home while an app was open) it is not set at all: armed, it drew the dock's tray
        // empty while the apps faded in.
        if (!tilesAnimatedOnce) {
            int enterAnim = reducedMotion ? R.anim.tile_enter_reduced : R.anim.tile_enter;
            float enterDelay = reducedMotion ? 0f : 0.06f;
            grid.setLayoutAnimation(new LayoutAnimationController(
                    AnimationUtils.loadAnimation(this, enterAnim), enterDelay));
        }
        adapter = new AppAdapter(this, this, COLUMNS, cfg);
        grid.setAdapter(adapter);

        // tvOS layout: push the dock row towards the bottom of the screen, leaving room for
        // rows_below_dock grid rows under it; the rest of the grid stays below until you press Down.
        rowsBelowDockSetting = cfg.rowsBelowDock;
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            parkDock();
            updateTopFade();
        });

        // Rows feather out under the top bar instead of being cut off. The fade needs an offscreen
        // layer and draws the grid twice, on every frame of every animation, so it is on only while a
        // row is actually in the band: it switches on in the same frame a row scrolls into it.
        gridWrap = findViewById(R.id.gridWrap);

        // Keep the glass glued to its tile while the grid scrolls.
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                updateTopFade();
                if (!glassShown) return;
                glassTargetY -= dy;
                glassScrollShift -= dy;
                glider.scrolled(dy);
            }
        });

        // Liquid feel: soft springs on the highlight's position.
        springX = new SpringAnimation(glass, DynamicAnimation.TRANSLATION_X);
        springY = new SpringAnimation(glass, new FloatPropertyCompat<View>("glassContentY") {
            @Override
            public float getValue(View v) {
                return v.getTranslationY() - glassScrollShift;
            }

            @Override
            public void setValue(View v, float value) {
                v.setTranslationY(value + glassScrollShift);
            }
        });
        for (SpringAnimation s : new SpringAnimation[]{springX, springY}) {
            if (reducedMotion) {
                // Stiff and almost critically damped: settles in a few frames instead of oscillating,
                // and stops once the highlight is within 1.5 px of its tile.
                s.setSpring(new SpringForce().setStiffness(1400f).setDampingRatio(0.95f));
                s.setMinimumVisibleChange(1.5f);
            } else {
                // Critically damped: glides onto the tile and stops there, with no bounce back.
                s.setSpring(new SpringForce().setStiffness(900f).setDampingRatio(1.0f));
            }
        }

        btnInputs = findViewById(R.id.btnInputs);
        View btnSettings = findViewById(R.id.btnSettings);
        btnInputs.setOnClickListener(v -> showInputs());
        btnWifi.setOnClickListener(v -> showWifiPopup());
        btnWifi.setOnLongClickListener(v -> {
            open(systemIntent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        });
        btnSettings.setOnClickListener(v -> showSettingsPopup());
        btnSettings.setOnLongClickListener(v -> {
            if (moduleCards) {
                showModuleCards();
                return true;
            }
            open(systemIntent(Settings.ACTION_SETTINGS));
            return true;
        });

        // Keep focus on the top bar at its ends and at the top: Android's focus finder would otherwise
        // jump to a tile far below when there is nothing in that direction.
        View.OnKeyListener pillKeys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // Back to the dock tile Up came from, not wherever the focus finder lands.
                focusTile(lastFocused % COLUMNS);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (v.getId() == R.id.btnInputs || (v.getId() == R.id.btnWifi && (btnInputs == null || btnInputs.getVisibility() != View.VISIBLE))) {
                    if (weatherAirQualityView != null && weatherAirQualityView.getVisibility() == View.VISIBLE) {
                        weatherAirQualityView.requestFocus();
                        return true;
                    }
                }
                if (v.getId() == R.id.btnInputs && prayerTimesView != null && prayerTimesView.getVisibility() == View.VISIBLE) {
                    prayerTimesView.requestFocus();
                    return true;
                }
                // The left end is Inputs, or Wi-Fi on a box without inputs, or Settings when both are gone.
                return v.getId() == R.id.btnInputs
                        || (v.getId() == R.id.btnWifi && btnInputs.getVisibility() != View.VISIBLE)
                        || (v.getId() == R.id.btnSettings && btnInputs.getVisibility() != View.VISIBLE
                            && btnWifi.getVisibility() != View.VISIBLE);
            }
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v.getId() == R.id.btnSettings;
        };
        btnInputs.setOnKeyListener(pillKeys);
        btnWifi.setOnKeyListener(pillKeys);
        btnSettings.setOnKeyListener(pillKeys);

        // Hide the glass when focus leaves the tiles.
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((oldF, newF) -> {
            boolean onTile = newF != null && newF.getId() == R.id.card;
            if (!onTile) hideGlass();
            if (!onTile && (newF == null || !(isTopPill(newF) || inMenu(newF)))) glider.hide();
        });

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addDataScheme("package");

        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.registerCallback(inputCallback, handler);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(networkCallback);
            cm.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(), vpnCallback);
        }

        // Silent background update check after 5 seconds on launch
        handler.postDelayed(() -> AppUpdateManager.checkForUpdates(this, false), 5000);
    }

    /**
     * A theme's full-screen picture behind everything. Decoded once, downsampled to the screen so a 4K
     * JPEG costs a 1080p bitmap at most (about 8 MB), which is the price of a wallpaper on a 1 GB TV.
     */
    private android.graphics.Bitmap decodeWallpaper(File file) {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
            int sample = 1;
            while (opts.outWidth / (sample * 2) >= dm.widthPixels && opts.outHeight / (sample * 2) >= dm.heightPixels) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            return android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
        } catch (Exception | OutOfMemoryError e) {
            // A bad or huge picture must never take the home screen down; the plain colour stays.
            return null;
        }
    }

    /** Puts a picture behind everything, cross-fading from the previous one when asked. */
    private void setWallpaper(android.graphics.Bitmap bmp, boolean fade) {
        setWallpaper(bmp, fade, null);
    }

    /** With {@code blur} made in advance, the glass changes in step with the picture. */
    private void setWallpaper(android.graphics.Bitmap bmp, boolean fade, android.graphics.Bitmap blur) {
        if (bmp == null) return;
        if (wallView == null) {
            wallView = new android.widget.ImageView(this);
            wallView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            // Photos are bright: dim them so the clock, names and tiles stay readable.
            if (placesMode) wallView.setColorFilter(0xFF7A7A7A, android.graphics.PorterDuff.Mode.MULTIPLY);
            root.addView(wallView, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        android.graphics.Bitmap old = wallBitmap;
        wallBitmap = bmp;
        if (fade && old != null && !reducedMotion) {
            android.graphics.drawable.TransitionDrawable t = new android.graphics.drawable.TransitionDrawable(new android.graphics.drawable.Drawable[]{
                    new android.graphics.drawable.BitmapDrawable(getResources(), old),
                    new android.graphics.drawable.BitmapDrawable(getResources(), bmp)});
            t.setCrossFadeEnabled(true);
            wallView.setImageDrawable(t);
            t.startTransition(800);
            // The glass fades to the new picture over the same 800 ms as the wallpaper itself.
            if (blur != null) Backdrop.present(blur, 800);
            else blurBackdrop(bmp);
            // Drop the old picture once the fade is done, so only one 1080p bitmap stays in memory.
            handler.postDelayed(() -> {
                if (wallBitmap == bmp) wallView.setImageBitmap(bmp);
            }, 900);
        } else {
            wallView.setImageBitmap(bmp);
            if (blur != null) Backdrop.present(blur, 0);
            else blurBackdrop(bmp);
        }
        // An opaque picture covers the whole screen, so the black window and the plain background
        // under it are never seen. Painting them anyway cost two full-screen passes on every frame,
        // and this TV's graphics chip, not the app, is what holds each frame back.
        if (!bmp.hasAlpha()) {
            root.setBackground(null);
            getWindow().setBackgroundDrawable(null);
        }

        // Keep screensaver backdrop in sync with rotating wallpaper
        if (screensaverActive && screensaverView != null) {
            ImageView nightstandBackdrop = screensaverView.findViewById(R.id.nightstandBackdrop);
            if (nightstandBackdrop != null) {
                if (fade && old != null && !reducedMotion) {
                    android.graphics.drawable.TransitionDrawable td = new android.graphics.drawable.TransitionDrawable(new android.graphics.drawable.Drawable[]{
                            new android.graphics.drawable.BitmapDrawable(getResources(), old),
                            new android.graphics.drawable.BitmapDrawable(getResources(), bmp)});
                    td.setCrossFadeEnabled(true);
                    nightstandBackdrop.setImageDrawable(td);
                    td.startTransition(800);
                    handler.postDelayed(() -> {
                        if (wallBitmap == bmp && nightstandBackdrop != null) nightstandBackdrop.setImageBitmap(bmp);
                    }, 900);
                } else {
                    nightstandBackdrop.setImageBitmap(bmp);
                }
            }
        }
    }

    /** Hands the picture now on screen to the glass, which blurs it once, off the main thread. */
    private void blurBackdrop(android.graphics.Bitmap bmp) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        Backdrop.update(bmp, dm.widthPixels, dm.heightPixels, placesMode ? 0xFF7A7A7A : 0, wallDecoder, handler);
    }

    /** Shows the next picture from the wallpapers folder; the position survives restarts. */
    private void showNextWallpaper(boolean fade) {
        android.content.SharedPreferences prefs = getSharedPreferences("wallpaper", MODE_PRIVATE);
        int next = (prefs.getInt("index", -1) + 1) % wallpapers.size();
        prefs.edit().putInt("index", next).apply();
        wallShownAt = android.os.SystemClock.elapsedRealtime();
        final File file = wallpapers.get(next);
        // On a cold start, name the place straight away: waiting for the decode showed the app's name
        // at full size and shrank it a moment later. After that the name changes with the picture,
        // or it would name the next place over the last one's picture while that one decodes.
        if (placeName == null) setPlace(file);
        final boolean firstPicture = wallBitmap == null;
        wallDecoder.execute(() -> {
            android.graphics.Bitmap bmp = decodeWallpaper(file);
            // After the first picture, the blur is made here, before the picture is shown, so the glass
            // changes with the wallpaper rather than after it. The first picture of a fresh start does
            // not wait for its blur: Home is held for it, and the glass catches up a moment later.
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            android.graphics.Bitmap blur = firstPicture ? null
                    : Backdrop.blur(bmp, dm.widthPixels, dm.heightPixels, placesMode ? 0xFF7A7A7A : 0);
            handler.post(() -> {
                if (isDestroyed()) return;
                setWallpaper(bmp, fade, blur);
                setPlace(file);
            });
        });
    }

    /** Switches to the next picture every wallpaper interval while Home is in front. */
    private final Runnable wallTick = new Runnable() {
        @Override
        public void run() {
            if (wallpapers.size() >= 2) showNextWallpaper(true);
            handler.postDelayed(this, wallpaperIntervalMs);
        }
    };

    /** Fetches the week's places in the background, then switches the rotation to them. */
    private void refreshPlacesIfDue() {
        if (!placesMode || placesRefreshRunning || !PlaceWallpapers.due(this)) return;
        placesRefreshRunning = true;
        new Thread(() -> {
            boolean changed = PlaceWallpapers.refresh(getApplicationContext());
            handler.post(() -> {
                placesRefreshRunning = false;
                if (!changed || isDestroyed()) return;
                List<File> places = PlaceWallpapers.list(this);
                if (places.isEmpty()) return;
                // Last week's pictures are gone: show one of this week's straight away, so the
                // name on the shelf and the picture behind it always match.
                wallpapers = places;
                showNextWallpaper(wallBitmap != null);
                if (!isFinishing() && hasWindowFocus()) startWallpaperRotation();
            });
        }, "place-wallpapers").start();
    }

    private boolean onlineRefreshRunning;

    /** Fetches fresh 4K online wallpapers in the background when due. */
    private void refreshOnlineWallpapersIfDue() {
        if (config == null || !config.wallpaperOnline || onlineRefreshRunning || !OnlineWallpapers.due(this)) return;
        onlineRefreshRunning = true;
        new Thread(() -> {
            boolean changed = OnlineWallpapers.sync(getApplicationContext(), config.wallpaperFeed);
            handler.post(() -> {
                onlineRefreshRunning = false;
                if (!changed || isDestroyed()) return;
                List<File> fresh = OnlineWallpapers.list(this);
                if (fresh.isEmpty()) return;
                List<File> combined = new ArrayList<>(config.wallpapers);
                for (File f : fresh) {
                    if (!combined.contains(f)) combined.add(f);
                }
                wallpapers = combined;
                if (wallBitmap == null) showNextWallpaper(false);
                if (!isFinishing() && hasWindowFocus()) startWallpaperRotation();
            });
        }, "online-wallpapers").start();
    }

    /** Clears the TV's app caches when due, once Home has sat idle on screen for a while. */
    private final Runnable cacheClean = () -> {
        if (hasWindowFocus() && config != null) CacheCleaner.maybeRun(this, config.cacheCleanDays);
    };

    /** Whenever Home comes back: picks the rotation up where the interval left it. */
    private void startWallpaperRotation() {
        refreshPlacesIfDue();
        refreshOnlineWallpapersIfDue();
        handler.removeCallbacks(wallTick);
        String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");
        if ("Madinah".equalsIgnoreCase(activeCat)) {
            wallpaperIntervalMs = 15_000L;
        }
        if (wallpapers.size() < 2 || wallpaperIntervalMs <= 0) return;
        long waited = android.os.SystemClock.elapsedRealtime() - wallShownAt;
        // A picture that fell due while you were in an app waits a few seconds: decoding, blurring
        // and fading it the moment you come back competed with Home for the TV's slow CPU.
        handler.postDelayed(wallTick, Math.max(RETURN_SETTLE_MS, Math.min(wallpaperIntervalMs, wallpaperIntervalMs - waited)));
    }

    /**
     * Home pressed while Home is already in front: singleTask delivers it here, then onResume runs.
     * Back to the top with the first dock tile focused, like the stock launcher. Back from an app
     * never comes this way, so returning to the app you left from is unchanged (#118).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("screensaver", false)) {
            showNightstandScreensaver();
            return;
        }
        if (intent == null || !intent.hasCategory(Intent.CATEGORY_HOME)) return;
        // Home pressed on Home goes to screensaver sleep mode. Home pressed in another app comes back to
        // the launcher without launching screensaver.
        if (hiddenSinceResume) return;
        if (screensaverActive) {
            dismissScreensaver();
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastHomePressTime < DOUBLE_HOME_INTERVAL_MS) {
            lastHomePressTime = 0;
            if (singleHomeRunnable != null) {
                handler.removeCallbacks(singleHomeRunnable);
                singleHomeRunnable = null;
            }
            startActivity(new Intent(this, RecentAppsActivity.class));
            return;
        } else {
            lastHomePressTime = now;
            singleHomeRunnable = () -> {
                singleHomeRunnable = null;
                if (screensaverActive) {
                    dismissScreensaver();
                } else {
                    if (cardPrompt != null) cardPrompt.dismiss();
                    if (openPopup != null) openPopup.dismiss();
                    closeMenu(false);
                    if (movingPos >= 0) finishMove();
                    showNightstandScreensaver();
                }
            };
            handler.postDelayed(singleHomeRunnable, DOUBLE_HOME_INTERVAL_MS);
        }
    }

    /**
     * Home went behind another app since it was last resumed. A Home press that brings it back
     * arrives before onResume, so this tells it apart from a Home press on Home itself.
     */
    private boolean hiddenSinceResume;

    @Override
    protected void onStop() {
        hiddenSinceResume = true;
        handler.removeCallbacks(cacheClean);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, packageFilter);
        }
        tilesReady = false;
        promptsPaused = false;
        resuming = true;
        reloadApps(true);
        updateShamsiDate();
        updatePrayerTimes();
        updateDateAndPeriod();
        updateWifiLabel();
        updateWeatherAqi();
        WeatherAirQuality.syncIfDue(this, this::updateWeatherAqi);
        updateIslamicShelf();
        updateInputsButton();
        startWallpaperRotation();
        handler.postDelayed(minuteTick, MINUTE_MS);
        handler.removeCallbacks(cacheClean);
        handler.postDelayed(cacheClean, CacheCleaner.IDLE_MS);
        hiddenSinceResume = false;
    }

    /** Rebuilds the tiles (and the module cards), then puts focus back where it was. */
    private void reloadApps(boolean animate) {
        // Capture the app before rebinding can move focus and overwrite its key.
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        final boolean restoreFocus = animate || grid.hasFocus();
        long started = android.os.SystemClock.uptimeMillis();
        boolean rebuilt = reloadTiles();
        // Nothing installed or removed: Home comes back exactly as it was left, with no rebuild and
        // no shelf, glow or selection fading back in.
        if (restoreFocus && rebuilt) {
            glassShown = false;
            glider.hideNow();
            heroShown = false;
            heroShelf.setAlpha(0f);
            ambientShown = false;
            ambient.setAlpha(0f);
            syncGlassGlow();
        }
        android.util.Log.i("TvHome.Perf", "tiles " + (rebuilt ? "rebuilt" : "unchanged") + " in "
                + (android.os.SystemClock.uptimeMillis() - started) + " ms");
        // The tiles rise into place on the first launch only. Replayed on every return from an app,
        // it showed the glass tray empty for a moment and held Home back while the tiles arrived.
        if (animate && !reducedMotion && !tilesAnimatedOnce) grid.scheduleLayoutAnimation();
        if (animate) tilesAnimatedOnce = true;

        if (restoreFocus) {
            // Follow the same app, or the nearest remaining slot when it was removed.
            restoreFocus();
        } else {
            // A package change must not steal focus from a top button or the Inputs popup.
            int pos = adapter.positionOf(returnKey);
            lastFocused = Math.max(0, Math.min(pos >= 0 ? pos : returnSlot, adapter.getItemCount() - 1));
            lastFocusedKey = adapter.keyAt(lastFocused);
            if (pos < 0) hideHero();
            // A newly installed app may have brought a card with it.
            tilesReady = true;
            showCardPrompt();
        }
    }

    /**
     * Rebuilds the tiles; the module cards the owner chose to show stand in for their app's tile.
     * Every step into the card code runs under CardFeature: if it fails, the tiles are the ordinary
     * ones and cards stay off for this process.
     */
    private boolean reloadTiles() {
        // Read these before reload(): rebinding can move focus to the first tile and overwrite them.
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        final List<Cards.Card> none = Collections.emptyList();
        cards = moduleCards ? CardFeature.run(() -> Cards.discover(this, hiddenApps, Cards.load(Cards.storeFile(this))), none) : none;
        boolean rebuilt = adapter.reload(cards.isEmpty() ? none : CardFeature.run(() -> Cards.shown(cards), none));
        lastFocusedKey = returnKey;
        lastFocused = returnSlot;
        return rebuilt;
    }

    /**
     * Return to the app we left from, or the same slot if that app is gone. Always refocus: after
     * reload() the view that holds focus may no longer show the app it had.
     */
    private void restoreFocus() {
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        grid.post(() -> {
            int pos = adapter.positionOf(returnKey);
            focusTile(pos >= 0 ? pos : returnSlot);
            tilesReady = true;
            resuming = false;
            View now = getCurrentFocus();
            if (now != null && isTopPill(now)) moveTopHighlight(now);
            showCardPrompt();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(packageReceiver);
        handler.removeCallbacks(minuteTick);
        handler.removeCallbacks(wallTick);
        // Nothing of ours stays on screen over another app; an undecided prompt simply returns next time.
        if (cardPrompt != null) cardPrompt.dismiss();
        if (openPopup != null) openPopup.dismiss();
        closeMenu(false);
        finishMove();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) showCardPrompt();
    }

    /**
     * Android is short of memory while Home sits behind another app: give back what can be remade,
     * so the launcher is less likely to be reclaimed and Home comes back without starting over.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) Backdrop.trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sInstance != null && sInstance.get() == this) {
            sInstance = null;
        }
        if (uploadServer != null) {
            uploadServer.stop();
            uploadServer = null;
        }
        SoundEffects.release();
        wallDecoder.shutdownNow();
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.unregisterCallback(inputCallback);
        if (cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
            cm.unregisterNetworkCallback(vpnCallback);
        }
    }

    public void refreshLocationDependentData() {
        updateWeatherAqi();
        updatePrayerTimes();
        WeatherAirQuality.sync(this, this::updateWeatherAqi);
        PrayerTimes.sync(this, this::updatePrayerTimes);
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress() {
        if (openMenu != null) {
            closeMenu(true);
            return;
        }
        if (screensaverActive) {
            dismissScreensaver();
            return;
        }
        if (wallpaperFullscreen) {
            setWallpaperFullscreen(false);
            return;
        }

        if (doubleBackPending) {
            if (singleBackRunnable != null) {
                handler.removeCallbacks(singleBackRunnable);
                singleBackRunnable = null;
            }
            doubleBackPending = false;
            showNightstandScreensaver();
        } else {
            doubleBackPending = true;
            singleBackRunnable = () -> {
                doubleBackPending = false;
                singleBackRunnable = null;
                setWallpaperFullscreen(true);
            };
            handler.postDelayed(singleBackRunnable, DOUBLE_BACK_DELAY_MS);
        }
    }

    private void setWallpaperFullscreen(boolean fullscreen) {
        if (wallpaperFullscreen == fullscreen) return;
        wallpaperFullscreen = fullscreen;
        float alpha = fullscreen ? 0f : 1f;
        int duration = reducedMotion ? 120 : 300;

        if (mainContent != null) {
            mainContent.animate().alpha(alpha).setDuration(duration).start();
        }
        if (heroShelf != null) {
            heroShelf.animate().alpha(alpha).setDuration(duration).start();
        }
        if (islamicShelf != null) {
            islamicShelf.animate().alpha(alpha).setDuration(duration).start();
        }
        if (glass != null) {
            glass.animate().alpha(alpha).setDuration(duration).start();
        }
        if (glider != null) {
            glider.animate().alpha(alpha).setDuration(duration).start();
        }
        if (ambient != null && fullscreen) {
            ambient.animate().alpha(0f).setDuration(duration).start();
        }

        if (wallView != null && placesMode) {
            if (fullscreen) {
                wallView.clearColorFilter();
            } else {
                wallView.setColorFilter(0xFF7A7A7A, android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    // ---- glass highlight -------------------------------------------------------------------------

    /** Reused by every focus move. */
    private final Rect tileRect = new Rect(), showRect = new Rect();

    @Override
    public void onTileFocused(int position, View card) {
        if (!resuming && hasWindowFocus()) SoundEffects.playNav();
        lastFocused = position;
        lastFocusedKey = adapter.keyAt(position);
        // Tile rect in root coordinates, using layout positions (ignores the scale animation).
        Rect r = tileRect;
        r.set(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, r);
        float pad = 6f * density;        // matches GlassView's glow inset
        float w = card.getWidth() * AppAdapter.FOCUS_SCALE + 2 * pad;
        float h = card.getHeight() * AppAdapter.FOCUS_SCALE + 2 * pad;
        float x = r.exactCenterX() - w / 2f;
        float y = r.exactCenterY() - h / 2f;

        ViewGroup.LayoutParams lp = glass.getLayoutParams();
        if (lp.width != (int) w || lp.height != (int) h) {
            lp.width = (int) w;
            lp.height = (int) h;
            glass.setLayoutParams(lp);
            // The rim is drawn inset by pad, right on the zoomed tile, so its corners use the tile's radius as is.
            glass.setRadius(AppAdapter.cornerRadius(card.getWidth(), density) * AppAdapter.FOCUS_SCALE);
        }
        glassTargetX = x;
        glassTargetY = y;
        // Exactly the zoomed card, behind it: at rest the app hides it completely, so it never lies
        // over an app and only shows while it travels between places.
        float cardRadius = AppAdapter.cornerRadius(card.getWidth(), density) * AppAdapter.FOCUS_SCALE;
        int appAmbient = adapter.ambientColorAt(position);
        if (glider != null) {
            glider.setAmbientColor(appAmbient);
            glider.setStyle(Glider.GLASS);
        }
        glider.moveTo(x + pad, y + pad, x + w - pad, y + h - pad, cardRadius);
        gliderOnPill = false;
        glassShown = true;
        View item = (View) card.getParent();
        if (position < COLUMNS) {
            // Dock: park it at the bottom of the screen and show the top shelf for this app.
            int decoratedTop = item.getTop() - (int) (6 * density);
            int dy = decoratedTop - grid.getPaddingTop();
            // A 1px mismatch used to start a scroll on every dock move. Only scroll when the row is
            // actually off; reduced motion does it in one jump so it does not fight the highlight.
            boolean scroll = Math.abs(dy) > (int) (8 * density);
            if (scroll) {
                if (reducedMotion) grid.scrollBy(0, dy);
                else grid.smoothScrollBy(0, dy);
            }
            showHero(position, scroll ? r.top - dy : r.top);
        } else {
            // Below the dock there is no shelf, but the glow still takes this app's colour, so every
            // tile lights the room the same way the dock does.
            hideShelf();
            showAmbient(adapter.ambientColorAt(position));
            // Scroll just enough so the whole glass (not only the tile) is visible.
            showRect.set(0, -(int) pad * 2, item.getWidth(), item.getHeight() + (int) pad);
            grid.requestChildRectangleOnScreen(item, showRect, reducedMotion);
        }
    }

    // ---- top shelf -------------------------------------------------------------------------------

    private boolean isCustomWallpaper() {
        String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Natural");
        if ("Custom".equalsIgnoreCase(activeCat)) return true;
        if (wallpapers != null && !wallpapers.isEmpty()) {
            int idx = getSharedPreferences("wallpaper", MODE_PRIVATE).getInt("index", 0);
            if (idx >= 0 && idx < wallpapers.size()) {
                File f = wallpapers.get(idx);
                if (f != null && f.getAbsolutePath().contains("custom_wallpapers")) return true;
            }
        }
        return false;
    }

    private void showHero(int position, float dockTopInRoot) {
        int color = adapter.ambientColorAt(position);
        heroBottom = dockTopInRoot - HERO_GAP_DP * density;

        if (islamicShelf != null) {
            float iy = heroBottom - islamicShelf.getHeight();
            islamicShelf.setY(iy);
            if (!heroShown) {
                islamicShelf.animate().cancel();
                if (firstFrameHeld || resuming) islamicShelf.setAlpha(1f);
                else islamicShelf.animate().alpha(1f).setDuration(260).start();
            } else {
                islamicShelf.setAlpha(1f);
            }
        }

        // When using custom wallpaper, disable the description part completely
        if (isCustomWallpaper()) {
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.setAlpha(0f);
                heroShelf.setVisibility(View.GONE);
            }
            showAmbient(color);
            heroShown = true;
            return;
        }

        if (heroShelf != null) {
            heroShelf.setVisibility(View.VISIBLE);
        }

        // The place the picture is of, when there is one; the app's own name is the fallback for
        // the user's own wallpapers, which carry no title.
        final String label = placeName != null ? placeName : adapter.labelAt(position);
        final String desc = placeName != null ? placeText : "";
        final String where = placeName != null ? placeCountry : "";
        float y = heroBottom - heroShelf.getHeight();
        boolean changed = !label.contentEquals(heroName.getText());

        if (reducedMotion) {
            // Snap the shelf instead of sliding and fading it: each of those frames repaints the
            // full-screen panel, and this switch exists for the GPUs that cannot afford it.
            heroShelf.animate().cancel();
            setHeroText(label, where, desc);
            heroShelf.setY(y);
            heroShelf.setTranslationX(0f);
            heroShelf.setAlpha(1f);
            showAmbient(color);
            heroShown = true;
            return;
        }

        if (!heroShown) {
            setHeroText(label, where, desc);
            heroShelf.setY(y);
            heroShelf.setTranslationX(0f);
            // Coming back to Home, the words are simply there with the picture: faded in after the
            // first frame, they arrived a beat behind everything else.
            heroShelf.animate().cancel();
            if (firstFrameHeld || resuming) heroShelf.setAlpha(1f);
            else heroShelf.animate().alpha(1f).setDuration(260).start();
            showAmbient(color);
            heroShown = true;
        } else if (changed) {
            heroShelf.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction(() -> {
                setHeroText(label, where, desc);
                heroShelf.setY(y);
                heroShelf.setTranslationX(12 * density);
                heroShelf.animate().alpha(1f).translationX(0f).setDuration(200).start();
            }).start();
            showAmbient(color);
        } else {
            // Same words, different app: every dock app shares the place's name, so the colour has
            // to follow focus on its own rather than ride along with a change of text.
            heroShelf.setY(y);
            showAmbient(color);
        }
    }

    /**
     * Lines the top-right buttons up with the dock: the Settings circle's right edge sits on the
     * right edge of the last dock card, and Inputs follows on its left. Card width follows the
     * column count and the screen, so the edge is measured rather than written down.
     */
    private void alignTopButtons() {
        // This runs on every layout pass, which during a grid scroll is every frame, so it keeps
        // its views and rectangles rather than looking them up or allocating each time.
        if (topRow == null) {
            topRow = findViewById(R.id.topButtons);
            topSettings = findViewById(R.id.btnSettings);
        }
        if (topRow == null || topSettings.getWidth() == 0) return;
        RecyclerView.ViewHolder last = grid.findViewHolderForAdapterPosition(COLUMNS - 1);
        if (last == null) return;
        View card = last.itemView.findViewById(R.id.card);
        if (card == null || card.getWidth() == 0) return;
        // Layout positions, not drawn ones: a focused card is scaled up, and the row's own shift
        // must not be counted twice.
        alignCard.set(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, alignCard);
        alignButton.set(0, 0, topSettings.getWidth(), topSettings.getHeight());
        root.offsetDescendantRectToMyCoords(topSettings, alignButton);
        float shift = alignCard.right - alignButton.right;
        if (Math.abs(topRow.getTranslationX() - shift) >= 1f) topRow.setTranslationX(shift);
    }

    /**
     * The one focus shape (Glider) goes behind everything it marks; the pills' glass makes way for it
     * as it arrives, and the dock tray paints it over its own glass.
     */
    private void setUpGlider() {
        glider = new Glider(this, reducedMotion);
        // Behind the shelf's text, the pills and the tiles, so it never draws over any of them.
        root.addView(glider, root.indexOfChild(heroShelf), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // The glider replaces the glass selection outright.
        glass.setVisibility(View.GONE);
    }

    private boolean isTopPill(View v) {
        int id = v.getId();
        return id == R.id.btnInputs || id == R.id.btnWifi || id == R.id.btnSettings;
    }

    /** Sends the focus shape onto a pill: from a tile it glides up and narrows into the pill. */
    private void moveTopHighlight(View pill) {
        if (resuming) return;
        SoundEffects.playNav();
        pill.getLocationInWindow(pillAt);
        glider.getLocationInWindow(rootAt);
        float l = pillAt[0] - rootAt[0], t = pillAt[1] - rootAt[1];
        // The pills do not scroll with the grid, so the shape stops following the grid's scroll.
        glassShown = false;
        // Up to the top menu it is dark at once, and travels that way.
        gliderOnPill = true;
        glider.setStyle(Glider.DARK);
        glider.moveTo(l, t, l + pill.getWidth(), t + pill.getHeight(), pill.getHeight() / 2f);
    }

    /** The shelf's two lines. An empty description takes no room, so the title keeps its place. */
    private void setHeroText(String title, String where, String desc) {
        if (heroName == null || heroWhere == null || heroDesc == null) return;
        heroName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleSizeSp(title));
        heroName.setText(title);
        heroWhere.setText(where);
        heroWhere.setVisibility(where == null || where.isEmpty() ? View.GONE : View.VISIBLE);
        // The description sits further down under a country line; with none, close under the name.
        ViewGroup.MarginLayoutParams descLp = (ViewGroup.MarginLayoutParams) heroDesc.getLayoutParams();
        if (descLp != null) {
            int top = Math.round(((where == null || where.isEmpty()) ? 6f : 9.5f) * density);
            if (descLp.topMargin != top) {
                descLp.topMargin = top;
                heroDesc.setLayoutParams(descLp);
            }
        }
        heroDesc.setText(desc);
        heroDesc.setVisibility(desc == null || desc.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Shouting titles are calmed down: "THE RIBBON HERO" becomes "The Ribbon Hero". A title that
     * already has lower case is left exactly as its studio wrote it, so "LEGO Star Wars" and
     * "WALL·E" keep their capitals instead of being flattened to "Lego" and "Wall·e".
     */
    private static String titleCase(String title) {
        if (title == null || title.isEmpty()) return title;
        for (int i = 0; i < title.length(); i++) {
            if (Character.isLowerCase(title.charAt(i))) return title;
        }
        StringBuilder out = new StringBuilder(title.length());
        boolean start = true;
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            out.append(start ? Character.toUpperCase(c) : Character.toLowerCase(c));
            // A new word begins after a space or any of the marks a title tends to use.
            start = c == ' ' || c == '-' || c == ':' || c == '.' || c == '\'' || c == '(' || c == '/';
        }
        return out.toString();
    }

    /**
     * The largest size at which the name still fits on one line, up to its share of the width.
     * Names run longer than app names ("Wroclaw Market Square"), and a name cut short tells the
     * viewer nothing, so a long one is set smaller rather than clipped.
     */
    private float titleSizeSp(String title) {
        if (title == null || title.isEmpty() || heroNameMaxWidth <= 0) return HERO_TITLE_MAX_SP;
        android.text.TextPaint paint = new android.text.TextPaint(heroName.getPaint());
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        for (float sp = HERO_TITLE_MAX_SP; sp > HERO_TITLE_MIN_SP; sp -= 2f) {
            paint.setTextSize(sp * scaled);
            if (paint.measureText(title) <= heroNameMaxWidth) return sp;
        }
        return HERO_TITLE_MIN_SP;
    }

    /**
     * Follows the rotation: the shelf names whichever place is on screen. Called after the picture
     * changes, so the words and the picture never disagree.
     */
    private void setPlace(File picture) {
        if (picture != null && picture.getAbsolutePath().contains("custom_wallpapers")) {
            placeName = null;
            placeText = "";
            placeCountry = "";
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.animate().alpha(0f).setDuration(reducedMotion ? 100 : 250).withEndAction(() -> {
                    if (heroShelf != null) heroShelf.setVisibility(View.GONE);
                }).start();
            }
            return;
        }
        if (heroShelf != null) {
            heroShelf.setVisibility(View.VISIBLE);
        }
        String activeCategory = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");
        String[] about;
        if ("Madinah".equalsIgnoreCase(activeCategory)) {
            about = MadinahWallpapers.describe(picture);
        } else if (placesMode) {
            about = PlaceWallpapers.describe(picture);
        } else {
            about = OnlineWallpapers.describe(picture);
        }
        String title = about != null ? titleCase(about[0]) : null;
        String overview = about != null ? about[1] : "";
        String country = about != null && about.length > 2 ? about[2] : "";
        if (country != null) {
            country = country.replaceAll("(?i)\\bunsplash\\s*•?\\s*", "").trim();
            if (country.startsWith("Malaysia_")) {
                String st = country.substring("Malaysia_".length());
                if ("WP".equalsIgnoreCase(st)) st = "Wilayah Persekutuan";
                else if ("NegeriSembilan".equalsIgnoreCase(st)) st = "Negeri Sembilan";
                else if ("PulauPinang".equalsIgnoreCase(st)) st = "Pulau Pinang";
                country = st + ", Malaysia";
            }
        }
        if (title != null) {
            title = title.replaceAll("(?i)\\bunsplash\\s*•?\\s*", "").trim();
        }
        if (title == null ? placeName == null : title.equals(placeName)) return;
        placeName = title;
        placeText = overview;
        placeCountry = country;
        // When the shelf is up, the words fade out with the old picture and back in with the new
        // one, inside the picture's own cross-fade: swapped at once, the next place's name sat over
        // the last one's picture while it faded. The layout listener puts the shelf back where it
        // belongs once the new lines have been measured.
        if (heroShown && title != null) {
            final String t = title;
            final String c = country;
            final String o = overview;
            heroShelf.animate().cancel();
            heroShelf.animate().alpha(0f).setDuration(reducedMotion ? 100 : 250).withEndAction(() -> {
                setHeroText(t, c, o);
                if (heroShown) heroShelf.animate().alpha(1f).setDuration(reducedMotion ? 120 : 400).start();
            }).start();
        }
    }

    /**
     * The glow used to be a full-screen radial GradientDrawable. On Android 10 that drawable is
     * drawn on the CPU, so fading it for half a second redrew 1920×1080 every frame and the
     * highlight looked like it was running at a few frames per second. A small bitmap stretched
     * over the screen is one texture; changing apps only uploads it once.
     */
    private void setAmbient(int color) {
        setAmbient(color, false);
    }

    private FadeTopFrame gridWrap;

    /** Turns the top fade on while any row reaches into its band, with room for a focused tile's zoom. */
    private void updateTopFade() {
        if (gridWrap == null) return;
        float band = 56 * density + 24 * density;
        boolean inBand = false;
        for (int i = 0; i < grid.getChildCount() && !inBand; i++) {
            View child = grid.getChildAt(i);
            inBand = grid.getTop() + child.getTop() + child.getTranslationY() < band;
        }
        gridWrap.setFadeStrength(inBand ? 1f : 0f);
    }

    /**
     * The dock row sits towards the bottom of the screen, leaving room for rows_below_dock grid rows
     * under it; the rest of the grid stays below until you press Down. Sets the grid's top padding
     * for that, and says whether it was already right.
     */
    private boolean parkDock() {
        int h = grid.getHeight();
        if (h <= 0) return false;
        // Item height follows the tile size, which follows the column count (AppAdapter.tileWidthPx).
        int inner = grid.getWidth() - grid.getPaddingLeft() - grid.getPaddingRight();
        int itemH = AppAdapter.itemHeightPx(AppAdapter.tileHeightPx(AppAdapter.tileWidthPx(inner, COLUMNS, density)), density);
        int dockRow = itemH + (int) ((6 + 12) * density);   // item height plus DockDecoration offsets
        int want = Math.max(0, h - dockRow - itemH * rowsBelowDockSetting - (int) (30 * density));
        if (grid.getPaddingTop() == want) return true;
        grid.setPadding(grid.getPaddingLeft(), want, grid.getPaddingRight(), grid.getPaddingBottom());
        return false;
    }

    /**
     * Home's first frame waits until Home is whole: tiles bound and laid out, the dock parked at the
     * bottom and the wallpaper in. Drawn as soon as the window was ready, it showed the glass tray
     * floating empty near the top, then the tiles, then the dock dropping into place. This matters
     * most after a long stay in another app, when Android has reclaimed the launcher and Home starts
     * from nothing. The wait is capped, so Home can never hang on a slow picture.
     */
    private void holdFirstFrame() {
        final long start = android.os.SystemClock.uptimeMillis();
        firstFrameHeld = true;
        final boolean expectsWallpaper = !wallpapers.isEmpty() || (config.wallpaper != null && !placesMode) || config.wallpaperOnline;
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // Parked here rather than by the layout listener alone: while frames are held, the
                // second layout pass that listener relies on does not always come.
                boolean parked = parkDock();
                // The place's name and lines come in the same frame as the picture they describe,
                // whenever focus lands in the dock, where the shelf shows.
                boolean whole = adapter.getItemCount() > 0 && grid.getChildCount() > 0 && parked
                        && (wallBitmap != null || !expectsWallpaper) && (heroShown || lastFocused >= COLUMNS);
                if (!whole && android.os.SystemClock.uptimeMillis() - start < FIRST_FRAME_WAIT_MS) {
                    return false;   // skip this frame; the next traversal asks again
                }
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                firstFrameHeld = false;
                android.util.Log.i("TvHome.Perf", "first frame after " + (android.os.SystemClock.uptimeMillis() - start)
                        + " ms" + (whole ? "" : " (gave up waiting: tiles=" + adapter.getItemCount()
                        + " laidOut=" + grid.getChildCount() + " dockParked=" + parked
                        + " wallpaper=" + (wallBitmap != null) + ")"));
                return true;
            }
        });
    }

    /** Tells the glass the glow's colour and strength, so what shows through matches around it. */
    private void syncGlassGlow() {
        Backdrop.setGlow(ambientShape(), glowFrom, ambientColor, glowMix, ambient.getAlpha());
    }

    /** Runs the glass through the same colour blend as the room's TransitionDrawable. */
    private void blendGlassGlow(int from, long ms) {
        if (glowBlend != null) glowBlend.cancel();
        glowFrom = from;
        glowMix = 0f;
        glowBlend = android.animation.ValueAnimator.ofFloat(0f, 1f);
        glowBlend.setDuration(ms);
        glowBlend.addUpdateListener(a -> {
            glowMix = (float) a.getAnimatedValue();
            syncGlassGlow();
        });
        glowBlend.start();
    }

    /** The glow's shape in white, drawn on first use and kept: a tint gives it each app's colour. */
    private Bitmap ambientShape() {
        if (ambientShape != null) return ambientShape;
        int w = 480;
        int h = 270;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(
                w * 0.32f, h * 0.42f, w * 0.62f,
                new int[]{0x5CFFFFFF, 0x26FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        new Canvas(bmp).drawRect(0, 0, w, h, paint);
        ambientShape = bmp;
        return bmp;
    }

    /** With crossFade, the old glow fades into the new one (two small textures, one blend). */
    private void setAmbient(int color, boolean crossFade) {
        if (color == ambientColor && ambient.getBackground() != null) return;
        android.graphics.drawable.Drawable old = ambient.getBackground();
        if (old instanceof android.graphics.drawable.TransitionDrawable) {
            android.graphics.drawable.TransitionDrawable t = (android.graphics.drawable.TransitionDrawable) old;
            old = t.getDrawable(t.getNumberOfLayers() - 1);   // a blend still running: start from where it was heading
        }
        int previous = ambientColor;
        ambientColor = color;
        // Every app's glow is the same shape in a different colour, so the shape is drawn once and
        // each app only tints it. Drawing a fresh bitmap per move cost a CPU fill, half a megabyte
        // and a texture upload every time focus moved, which is what made moving around feel heavy
        // once every tile, not only the dock, tinted the room.
        BitmapDrawable drawable = new BitmapDrawable(getResources(), ambientShape());
        drawable.setGravity(Gravity.FILL);
        drawable.setColorFilter(new android.graphics.PorterDuffColorFilter(
                0xFF000000 | color, android.graphics.PorterDuff.Mode.SRC_IN));
        if (crossFade && old != null) {
            android.graphics.drawable.TransitionDrawable t = new android.graphics.drawable.TransitionDrawable(
                    new android.graphics.drawable.Drawable[]{old, drawable});
            t.setCrossFadeEnabled(true);
            ambient.setBackground(t);
            t.startTransition(450);
            // The glass blends with the room, over the same 450 ms, instead of jumping ahead of it.
            blendGlassGlow(previous, 450);
        } else {
            ambient.setBackground(drawable);
            if (glowBlend != null) glowBlend.cancel();
            glowFrom = color;
            glowMix = 1f;
            syncGlassGlow();
        }
    }

    /** Focus has left the apps altogether: both the shelf and the glow go. */
    private void hideHero() {
        hideShelf();
        if (!ambientShown) return;
        ambientShown = false;
        ambient.animate().alpha(0f).setDuration(reducedMotion ? 140 : 300)
                .setUpdateListener(a -> syncGlassGlow()).start();
    }

    private void hideShelf() {
        if (!heroShown) return;
        heroShown = false;
        heroShelf.animate().alpha(0f).setDuration(reducedMotion ? 100 : 160).start();
        if (islamicShelf != null) islamicShelf.animate().alpha(0f).setDuration(reducedMotion ? 100 : 160).start();
    }

    /**
     * Lights the room in this app's colour. From one app to the next it blends straight across,
     * with no dip to plain in between, whether the move is along the dock, down into the grid or
     * back up again.
     */
    private void showAmbient(int color) {
        if (!ambientShown) {
            setAmbient(color);
            ambient.animate().alpha(1f).setDuration(reducedMotion ? 160 : 600)
                    .setUpdateListener(a -> syncGlassGlow()).start();
            ambientShown = true;
        } else if (color != ambientColor) {
            setAmbient(color, !reducedMotion);
        }
    }

    private void hideGlass() {
        glassShown = false;
    }

    // ---- navigation ------------------------------------------------------------------------------

    // ---- arranging the tiles ---------------------------------------------------------------------

    /** The tile being moved (adapter position), or -1; its card wiggles while it is. */
    private int movingPos = -1;
    private View movingCard;
    private android.animation.ObjectAnimator wiggle;
    /** A key whose release is swallowed: the OK or Back that put a moved tile down must not also open it. */
    private int swallowRelease = -1;
    /**
     * Slides the tiles into their new places while an app is moved. The grid has no item animations
     * otherwise: they would replay on every rebuild and cost frames while scrolling.
     */
    private androidx.recyclerview.widget.DefaultItemAnimator moveSlide;

    /** Holding OK on an app: a glass menu to move it, or to open Android's page for it. */
    @Override
    public boolean onTileHeld(int position, View card) {
        if (openMenu != null || movingPos >= 0) return true;
        FrameLayout panel = (FrameLayout) getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        final String key = adapter.keyAt(position);
        if (adapter.canMove(position)) {
            addInputRow(rows, () -> {
                // No selection gliding back from the menu: it is simply gone, and the app wiggles.
                glider.hideNow();
                gliderOnPill = false;
                closeMenu(true);
            }, R.drawable.ic_move, getString(R.string.tile_move), -1, () -> startMove(key));
        }
        addInputRow(rows, () -> closeMenu(false), R.drawable.ic_info, getString(R.string.tile_app_info), -1,
                () -> adapter.openAppInfo(adapter.positionOf(key)));
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = Math.max((int) (190 * density), panel.getMeasuredWidth());
        // Under the app when there is room; over it in the dock, which sits near the bottom.
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
        card.getLocationInWindow(menuAt);
        root.getLocationInWindow(menuGliderAt);
        int below = menuAt[1] - menuGliderAt[1] + card.getHeight() + (int) (18 * density) + panel.getMeasuredHeight();
        showMenu(panel, rows, card, width, below > root.getHeight() - (int) (24 * density));
        return true;
    }

    /** Picks the app up: it wiggles, the arrows carry it, and OK or Back puts it down. */
    private void startMove(String key) {
        int pos = adapter.positionOf(key);
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh == null || !adapter.canMove(pos)) return;
        movingPos = pos;
        movingCard = vh.itemView.findViewById(R.id.card);
        movingCard.requestFocus();
        glider.setPaneHidden(true);
        if (moveSlide == null) {
            moveSlide = new androidx.recyclerview.widget.DefaultItemAnimator();
            moveSlide.setSupportsChangeAnimations(false);
            moveSlide.setMoveDuration(reducedMotion ? 140 : 220);
        }
        grid.setItemAnimator(moveSlide);
        float tilt = reducedMotion ? 0.8f : 1.4f;
        wiggle = android.animation.ObjectAnimator.ofFloat(movingCard, View.ROTATION, -tilt, tilt);
        wiggle.setDuration(reducedMotion ? 260 : 140);
        wiggle.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        wiggle.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        wiggle.start();
    }

    /** Puts the moving app down where it is; the order was kept as it moved. */
    private void finishMove() {
        if (movingPos < 0) return;
        if (wiggle != null) wiggle.cancel();
        wiggle = null;
        if (movingCard != null) movingCard.animate().rotation(0f).setDuration(120).start();
        movingCard = null;
        movingPos = -1;
        glider.setPaneHidden(false);
        // Back to no item animations once the last slide has finished.
        RecyclerView.ItemAnimator slide = grid.getItemAnimator();
        if (slide != null) slide.isRunning(() -> {
            if (movingPos < 0 && grid.getItemAnimator() == slide) grid.setItemAnimator(null);
        });
    }

    /** Carries the moving app one step, the other tiles making room, and the selection follows it. */
    private void stepMove(int keyCode) {
        int last = adapter.lastMovable();
        int to = movingPos;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (movingPos % COLUMNS > 0) to = movingPos - 1;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (movingPos % COLUMNS < COLUMNS - 1) to = Math.min(movingPos + 1, last);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (movingPos >= COLUMNS) to = movingPos - COLUMNS;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (movingPos / COLUMNS < last / COLUMNS) to = Math.min(movingPos + COLUMNS, last);
                break;
            default:
                return;
        }
        if (!adapter.move(movingPos, to)) return;
        movingPos = to;
        final View card = movingCard;
        final int pos = to;
        // Once the tiles have shifted, glide the selection (and the shelf, and the scroll) to it.
        grid.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                grid.removeOnLayoutChangeListener(this);
                grid.post(() -> {
                    if (movingPos == pos && card != null && card.isAttachedToWindow()) onTileFocused(pos, card);
                });
            }
        });
        grid.requestLayout();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        lastUserInteraction = android.os.SystemClock.elapsedRealtime();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        lastUserInteraction = android.os.SystemClock.elapsedRealtime();
        if (screensaverActive) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                dismissScreensaver();
            }
            return true;
        }
        int code = event.getKeyCode();
        if (wallpaperFullscreen) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                setWallpaperFullscreen(false);
                if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
                    doubleBackPending = true;
                    singleBackRunnable = () -> {
                        doubleBackPending = false;
                        singleBackRunnable = null;
                    };
                    handler.postDelayed(singleBackRunnable, DOUBLE_BACK_DELAY_MS);
                }
            }
            return true;
        }
        if (doubleBackPending && code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_ESCAPE) {
            if (singleBackRunnable != null) {
                handler.removeCallbacks(singleBackRunnable);
                singleBackRunnable = null;
            }
            doubleBackPending = false;
        }
        if (code == swallowRelease && event.getAction() == KeyEvent.ACTION_UP) {
            swallowRelease = -1;
            return true;
        }
        if (code == KeyEvent.KEYCODE_APP_SWITCH) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                startActivity(new Intent(this, RecentAppsActivity.class));
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_HOME || code == KeyEvent.KEYCODE_ALL_APPS) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (screensaverActive) {
                    dismissScreensaver();
                    return true;
                }
                long now = android.os.SystemClock.uptimeMillis();
                if (now - lastHomePressTime < DOUBLE_HOME_INTERVAL_MS) {
                    lastHomePressTime = 0;
                    if (singleHomeRunnable != null) {
                        handler.removeCallbacks(singleHomeRunnable);
                        singleHomeRunnable = null;
                    }
                    startActivity(new Intent(this, RecentAppsActivity.class));
                    return true;
                } else {
                    lastHomePressTime = now;
                    singleHomeRunnable = () -> {
                        singleHomeRunnable = null;
                        if (screensaverActive) {
                            dismissScreensaver();
                        } else {
                            if (cardPrompt != null) cardPrompt.dismiss();
                            if (openPopup != null) openPopup.dismiss();
                            closeMenu(false);
                            if (movingPos >= 0) finishMove();
                            showNightstandScreensaver();
                        }
                    };
                    handler.postDelayed(singleHomeRunnable, DOUBLE_HOME_INTERVAL_MS);
                    return true;
                }
            }
            return true;
        }
        if (movingPos < 0) {
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    handleBackPress();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) stepMove(code);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (down) {
                    finishMove();
                    swallowRelease = code;
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public boolean onTileVerticalKey(int position, int keyCode) {
        int count = adapter.getItemCount();
        int target;
        // Horizontal: swallow the key at a row's ends so focus never wraps to the next row or
        // escapes up into the top bar. Mid-row moves fall through to the default focus search.
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return position % COLUMNS == 0;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return position % COLUMNS == COLUMNS - 1 || position == count - 1;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (position < COLUMNS) {
                if (position <= 1 && weatherAirQualityView != null && weatherAirQualityView.getVisibility() == View.VISIBLE) {
                    weatherAirQualityView.requestFocus();
                    return true;
                }
                return false;          // dock row: let focus move to the top bar
            }
            target = position - COLUMNS;
        } else {
            int lastRowStart = ((count - 1) / COLUMNS) * COLUMNS;
            if (position >= lastRowStart) return true;     // already on the last row: stay put
            target = Math.min(position + COLUMNS, count - 1);
        }
        focusTile(target);
        return true;
    }

    /** The tile {@link #focusWhenSettled} focuses once the grid stops scrolling to it. */
    private int pendingFocus;
    private final RecyclerView.OnScrollListener focusWhenSettled = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
            rv.removeOnScrollListener(this);
            RecyclerView.ViewHolder h = rv.findViewHolderForAdapterPosition(pendingFocus);
            View target = h != null ? h.itemView.findViewById(R.id.card) : rv.getChildAt(0);
            if (target != null) target.requestFocus();
        }
    };

    private void focusTile(int position) {
        if (adapter.getItemCount() == 0) {
            (btnWifi.getVisibility() == View.VISIBLE ? btnWifi : findViewById(R.id.btnSettings)).requestFocus();
            return;
        }
        final int pos = Math.max(0, Math.min(position, adapter.getItemCount() - 1));
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            View card = vh.itemView.findViewById(R.id.card);
            // Already focused (e.g. back from an app): no focus change fires, so restore the glass by hand.
            if (card.isFocused()) onTileFocused(pos, card);
            else card.requestFocus();
            return;
        }
        // Not laid out yet: scroll there, then focus once the scroll settles. One listener at most,
        // aimed at the latest target, so asking again mid-scroll cannot pull focus back to an older one.
        pendingFocus = pos;
        grid.removeOnScrollListener(focusWhenSettled);
        grid.addOnScrollListener(focusWhenSettled);
        grid.smoothScrollToPosition(pos);
    }

    // ---- top bar ---------------------------------------------------------------------------------

    private void updateShamsiDate() {
        if (shamsi) shamsiDate.setText(Shamsi.today());
    }

    private void updatePrayerTimes() {
        if (prayerTimesView != null) {
            prayerTimesView.setText(PrayerTimes.getFormatted(this));
        }
        updateIslamicShelf();
        if (screensaverActive) {
            updateNightstandScreensaver();
        }
    }

    private void updateWeatherAqi() {
        if (weatherAirQualityView != null) {
            weatherAirQualityView.setText(WeatherAirQuality.getFormatted(this));
        }
    }

    private void updateIslamicShelf() {
        if (islamicShelf == null) return;
        DailyAyat.Ayat ayat = DailyAyat.getToday();
        if (ayatRef != null) ayatRef.setText(ayat.reference);
        if (ayatArabic != null) ayatArabic.setText(ayat.arabic);
        if (ayatTranslation != null) ayatTranslation.setText(ayat.translation);

        if (prayerHeader != null) {
            prayerHeader.setText("PRAYER TIMES  •  " + PrayerTimes.getHijriDate(this).toUpperCase(Locale.ROOT));
        }

        PrayerTimes.PrayerItem[] items = PrayerTimes.getPrayerItems(this);
        if (items != null && items.length >= 6) {
            TextView[] labels = {lblSubuh, lblSyuruk, lblZohor, lblAsar, lblMaghrib, lblIsyak};
            TextView[] times = {timeSubuh, timeSyuruk, timeZohor, timeAsar, timeMaghrib, timeIsyak};

            for (int i = 0; i < 6; i++) {
                if (times[i] != null) times[i].setText(items[i].timeStr);
                if (labels[i] != null) {
                    labels[i].setText(items[i].name);
                    if (items[i].isNext) {
                        labels[i].setTextColor(0xFF4ADE80);
                        if (times[i] != null) times[i].setTextColor(0xFF4ADE80);
                    } else {
                        labels[i].setTextColor(0xB3FFFFFF);
                        if (times[i] != null) times[i].setTextColor(0xFFFFFFFF);
                    }
                }
            }
        }
    }

    private void updateArsenalCard() {
        if (arsenalMatchCard == null) return;
        ArsenalMatch.MatchCardData data = ArsenalMatch.getNextMatch();
        if (arsenalCardHeader != null) {
            arsenalCardHeader.setText(data.header);
            if (data.isLive) {
                arsenalCardHeader.setTextColor(0xFFFF4D4D);
            } else {
                arsenalCardHeader.setTextColor(0xCCFFFFFF);
            }
        }
        if (arsenalMatchOpponent != null) arsenalMatchOpponent.setText(data.opponent);
        if (arsenalLiveScore != null) {
            if (data.hasScore) {
                arsenalLiveScore.setText(data.score);
                arsenalLiveScore.setVisibility(View.VISIBLE);
            } else {
                arsenalLiveScore.setVisibility(View.GONE);
            }
        }
        if (arsenalMatchCompetition != null) arsenalMatchCompetition.setText(data.details);
        if (arsenalMatchTime != null) arsenalMatchTime.setText(data.timeStr);
        if (arsenalMatchCountdown != null) {
            arsenalMatchCountdown.setText(data.countdownStr);
            if (data.isLive) {
                arsenalMatchCountdown.setTextColor(0xFF4ADE80);
            } else {
                arsenalMatchCountdown.setTextColor(0x80FFFFFF);
            }
        }
    }

    private void updateDateAndPeriod() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String period = (hour >= 0 && hour < 12) ? "AM" : "PM";

        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        String[] englishMonths = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        String mStr = (month >= 0 && month < 12) ? englishMonths[month] : "";
        String dateStr = day + " " + mStr + " " + year;

        if (ampmView != null) ampmView.setText(period);
        if (dateView != null) dateView.setText(dateStr);
    }

    /** Shows the Wi-Fi network name on the button when we can read it, otherwise the connection type. */
    private void updateWifiLabel() {
        if (btnWifi.getVisibility() == View.GONE) return;   // show_network_pill off
        String text = getString(R.string.wifi);
        boolean vpn = false, online = false;
        int icon = R.drawable.ic_wifi;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean wifi = false, ethernet = false;
            // All networks, not just the active one: with a VPN up the active network is the VPN itself.
            for (Network net : cm != null ? cm.getAllNetworks() : new Network[0]) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                if (caps == null) continue;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpn = true;
                    continue;
                }
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue;
                online = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) ethernet = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true;
            }
            if (!online) {
                text = getString(R.string.offline);
                vpn = false;
            } else if (ethernet) {
                text = getString(R.string.ethernet);
                icon = R.drawable.ic_ethernet;
            } else if (wifi) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String ssid = info != null ? info.getSSID() : null;
                if (ssid != null) {
                    ssid = ssid.replace("\"", "").trim();
                    if (!ssid.isEmpty() && !ssid.contains("unknown") && !ssid.equals("0x")) text = ssid;
                }
            }
        } catch (Exception ignored) {
        }
        btnWifi.setText(text);
        btnWifi.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
    }

    /** Shows an Apple TV-style frosted glass Wi-Fi control center popup. */
    private void showWifiPopup() {
        if (openMenu != null) return;
        SoundEffects.playSelect();
        View anchor = btnWifi;
        FrameLayout panel = (FrameLayout) getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        Runnable close = () -> closeMenu(false);

        String ssid = getString(R.string.wifi);
        String ip = "";
        boolean isConnected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network activeNet = cm != null ? cm.getActiveNetwork() : null;
            if (activeNet != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
                if (caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                    isConnected = true;
                }
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                WifiInfo info = wm.getConnectionInfo();
                if (info != null && info.getNetworkId() != -1) {
                    String s = info.getSSID();
                    if (s != null && !s.equals("<unknown ssid>") && !s.isEmpty()) {
                        ssid = s.replaceAll("^\"|\"$", "");
                    }
                    int ipVal = info.getIpAddress();
                    if (ipVal != 0) {
                        ip = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                (ipVal & 0xff), (ipVal >> 8 & 0xff), (ipVal >> 16 & 0xff), (ipVal >> 24 & 0xff));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        int width = (int) (240 * density);

        // Row 1: Current Wi-Fi status & SSID
        addInputRow(rows, close, R.drawable.ic_wifi, ssid, isConnected ? TvInputManager.INPUT_STATE_CONNECTED : TvInputManager.INPUT_STATE_DISCONNECTED, () -> {
            SoundEffects.playSelect();
            open(systemIntent(Settings.ACTION_WIFI_SETTINGS));
        });

        addSeparator(rows);

        // Row 2: Tetapan Wi-Fi
        addInputRow(rows, close, R.drawable.ic_settings, "Wi-Fi Settings", -1, () -> {
            SoundEffects.playSelect();
            open(systemIntent(Settings.ACTION_WIFI_SETTINGS));
        });

        // Row 3: Tutup
        addInputRow(rows, close, R.drawable.ic_move, "Close", -1, () -> {
            SoundEffects.playSelect();
            closeMenu(true);
        });

        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        width = Math.max(width, panel.getMeasuredWidth());

        showMenu(panel, rows, anchor, width, false);
    }

    /** Shows an Apple TV-style frosted glass Settings dropdown menu. */
    private void showSettingsPopup() {
        if (openMenu != null) return;
        SoundEffects.playSelect();
        View anchor = findViewById(R.id.btnSettings);
        if (anchor == null) return;
        FrameLayout panel = (FrameLayout) getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        Runnable close = () -> closeMenu(false);

        // Row 1: Wallpaper
        addInputRow(rows, close, R.drawable.ic_wallpaper, "Wallpaper", -1, () -> {
            SoundEffects.playSelect();
            showWallpaperDialog();
        });

        // Row 2: Location API Haze
        addInputRow(rows, close, R.drawable.ic_location, "Air Quality / Haze Location", -1, () -> {
            SoundEffects.playSelect();
            showHazeLocationDialog();
        });

        // Row 3: Location Waktu Solat
        addInputRow(rows, close, R.drawable.ic_mosque, "Prayer Times Location", -1, () -> {
            SoundEffects.playSelect();
            showPrayerZoneDialog();
        });

        // Row 4: Sound Effects
        String currentSoundTheme = SoundEffects.getActiveTheme(this);
        String soundLabel = "Sound: " + SoundEffects.getThemeDisplayName(currentSoundTheme);
        addInputRow(rows, close, R.drawable.ic_sound, soundLabel, -1, () -> {
            SoundEffects.playSelect();
            showSoundEffectsDialog();
        });

        // Row 5: Check for Updates (OTA)
        addInputRow(rows, close, R.drawable.ic_update, "Check for Updates", -1, () -> {
            SoundEffects.playSelect();
            AppUpdateManager.checkForUpdates(MainActivity.this, true);
        });

        addSeparator(rows);

        // Row 6: All Settings (Android Settings)
        addInputRow(rows, close, R.drawable.ic_settings, "All Settings", -1, () -> {
            SoundEffects.playSelect();
            open(systemIntent(Settings.ACTION_SETTINGS));
        });

        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = Math.max((int) (220 * density), panel.getMeasuredWidth());

        showMenu(panel, rows, anchor, width, false);
    }

    private void showSoundEffectsDialog() {
        final String[] soundOptions = new String[]{
                "🎮 PlayStation 5 (Default Chime)",
                "🕹️ Nintendo Switch (Click & Snap Pop)",
                "🟩 Xbox Series X/S (Warm Harmonic Chime)",
                "🔇 Muted (Sound Effects Off)"
        };

        final String[] soundKeys = new String[]{
                SoundEffects.THEME_PS5,
                SoundEffects.THEME_SWITCH,
                SoundEffects.THEME_XBOX,
                SoundEffects.THEME_OFF
        };

        String currentTheme = SoundEffects.getActiveTheme(this);
        int selectedIndex = 0;
        for (int i = 0; i < soundKeys.length; i++) {
            if (soundKeys[i].equalsIgnoreCase(currentTheme)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🔊 Select Sound Effects");
        builder.setSingleChoiceItems(soundOptions, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            String chosenKey = soundKeys[which];
            SoundEffects.setTheme(MainActivity.this, chosenKey);
            String displayName = SoundEffects.getThemeDisplayName(chosenKey);

            if (SoundEffects.THEME_OFF.equalsIgnoreCase(chosenKey)) {
                Toast.makeText(MainActivity.this, "🔇 Sound Effects Muted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "🔊 Sound Effects: " + displayName, Toast.LENGTH_SHORT).show();
                // Play sample of the selected theme
                handler.postDelayed(() -> SoundEffects.playSelect(), 120);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Dialog qrUploadDialog;
    private WallpaperUploadServer uploadServer;
    private ImageView qrImageView;
    private TextView qrStatusText;
    private TextView qrUrlText;

    private void showWallpaperDialog() {
        File customDir = new File(getFilesDir(), "custom_wallpapers");
        File[] customFiles = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
        boolean hasCustom = customFiles != null && customFiles.length > 0;

        String currentKey = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");

        List<String> catList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();

        catList.add("🕌 Madinah / Al-Madinah Al-Munawwarah");
        keyList.add("Madinah");

        catList.add("🌿 Natural Landscapes");
        keyList.add("Natural");

        String malaysiaLabel;
        if (currentKey.startsWith("Malaysia_")) {
            malaysiaLabel = "🇲🇾 Malaysia: " + getStateDisplayName(currentKey) + " (Change State ▶)";
        } else if ("Malaysia".equalsIgnoreCase(currentKey)) {
            malaysiaLabel = "🇲🇾 Malaysia: All States (Change State ▶)";
        } else {
            malaysiaLabel = "🇲🇾 Malaysia (By State ▶)";
        }
        catList.add(malaysiaLabel);
        keyList.add("MALAYSIA_PICKER");

        catList.add("🏙️ Skyscrapers & Architecture");
        keyList.add("Skyscraper");

        catList.add("🌌 Deep Space & Astronomy");
        keyList.add("Space");

        catList.add("🌊 Ocean & Coastal");
        keyList.add("Ocean");

        catList.add("🔲 Minimalist Dark");
        keyList.add("Minimalist");

        if (hasCustom) {
            boolean isSingle = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getBoolean("custom_single_mode", false);
            String customLabel = isSingle
                    ? "📱 Custom / Single Locked Image (" + customFiles.length + " photos)"
                    : "📱 Custom / Rotating Slideshow (" + customFiles.length + " photos)";
            catList.add(customLabel);
            keyList.add("Custom");

            catList.add("🗑️ Manage Uploaded Wallpapers");
            keyList.add("MANAGE_CUSTOM");
        }

        catList.add("📲 Upload Wallpaper via Phone (QR Code)");
        keyList.add("UPLOAD_QR");

        catList.add("🔀 Next Wallpaper");
        keyList.add("NEXT");

        final String[] categories = catList.toArray(new String[0]);
        final String[] keys = keyList.toArray(new String[0]);

        int selectedIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equalsIgnoreCase(currentKey) || ("MALAYSIA_PICKER".equals(keys[i]) && currentKey.startsWith("Malaysia"))) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🖼️ Select Wallpaper Collection");
        builder.setSingleChoiceItems(categories, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            String chosenKey = keys[which];
            if ("NEXT".equals(chosenKey)) {
                if (wallpapers != null && !wallpapers.isEmpty()) {
                    showNextWallpaper(true);
                    Toast.makeText(MainActivity.this, "Switching to next wallpaper...", Toast.LENGTH_SHORT).show();
                }
            } else if ("MALAYSIA_PICKER".equals(chosenKey)) {
                showMalaysiaStateSelectorDialog();
            } else if ("UPLOAD_QR".equals(chosenKey)) {
                showWallpaperUploadQrDialog();
            } else if ("MANAGE_CUSTOM".equals(chosenKey)) {
                showCustomWallpaperManagerDialog();
            } else {
                switchWallpaperCategory(chosenKey);
            }
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getStateDisplayName(String key) {
        if (key == null) return "Semua Negeri";
        if (key.equalsIgnoreCase("Malaysia_Johor")) return "Johor";
        if (key.equalsIgnoreCase("Malaysia_Kedah")) return "Kedah";
        if (key.equalsIgnoreCase("Malaysia_Kelantan")) return "Kelantan";
        if (key.equalsIgnoreCase("Malaysia_Melaka")) return "Melaka";
        if (key.equalsIgnoreCase("Malaysia_NegeriSembilan")) return "Negeri Sembilan";
        if (key.equalsIgnoreCase("Malaysia_Pahang")) return "Pahang";
        if (key.equalsIgnoreCase("Malaysia_Perak")) return "Perak";
        if (key.equalsIgnoreCase("Malaysia_Perlis")) return "Perlis";
        if (key.equalsIgnoreCase("Malaysia_PulauPinang")) return "Pulau Pinang";
        if (key.equalsIgnoreCase("Malaysia_Sabah")) return "Sabah";
        if (key.equalsIgnoreCase("Malaysia_Sarawak")) return "Sarawak";
        if (key.equalsIgnoreCase("Malaysia_Selangor")) return "Selangor";
        if (key.equalsIgnoreCase("Malaysia_Terengganu")) return "Terengganu";
        if (key.equalsIgnoreCase("Malaysia_WP")) return "Wilayah Persekutuan";
        return "Semua Negeri";
    }

    private void showMalaysiaStateSelectorDialog() {
        final String[] stateLabels = new String[]{
                "🇲🇾 Semua Negeri di Malaysia (Putar Keseluruhan)",
                "📍 Johor — Pulau Rawa & Pesisir Pantai",
                "📍 Kedah — Langkawi Sky Bridge & Sawah Padi",
                "📍 Kelantan — Pantai Cahaya Bulan & Senok",
                "📍 Melaka — Bandaraya Bersejarah & Sungai Melaka",
                "📍 Negeri Sembilan — Pantai Port Dickson & Teluk Kemang",
                "📍 Pahang — Ladang Teh Cameron Highlands",
                "📍 Perak — Pekan Warisan Ipoh & Kellie's Castle",
                "📍 Perlis — Gua Kelam & Banjaran Nakawan",
                "📍 Pulau Pinang — Georgetown UNESCO & Kek Lok Si",
                "📍 Sabah — Puncak Gunung Kinabalu",
                "📍 Sarawak — Pinnacles Mulu & Kuching Waterfront",
                "📍 Selangor — Batu Caves & Masjid Negeri Shah Alam",
                "📍 Terengganu — Pulau Perhentian & Masjid Kristal",
                "📍 Wilayah Persekutuan — Menara Berkembar KLCC"
        };

        final String[] stateKeys = new String[]{
                "Malaysia",
                "Malaysia_Johor",
                "Malaysia_Kedah",
                "Malaysia_Kelantan",
                "Malaysia_Melaka",
                "Malaysia_NegeriSembilan",
                "Malaysia_Pahang",
                "Malaysia_Perak",
                "Malaysia_Perlis",
                "Malaysia_PulauPinang",
                "Malaysia_Sabah",
                "Malaysia_Sarawak",
                "Malaysia_Selangor",
                "Malaysia_Terengganu",
                "Malaysia_WP"
        };

        String currentCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Natural");
        int selectedIndex = 0;
        for (int i = 0; i < stateKeys.length; i++) {
            if (stateKeys[i].equalsIgnoreCase(currentCat)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🇲🇾 Koleksi Malaysia — Pilih Negeri");
        builder.setSingleChoiceItems(stateLabels, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            String chosenKey = stateKeys[which];
            switchWallpaperCategory(chosenKey);
        });
        builder.setNegativeButton("Kembali / Back", (dialog, which) -> {
            dialog.dismiss();
            showWallpaperDialog();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void switchWallpaperCategory(String category) {
        getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit().putString("active_category", category).apply();
        String displayLabel = category.startsWith("Malaysia_") ? ("Malaysia (" + getStateDisplayName(category) + ")") : category;

        if ("Madinah".equalsIgnoreCase(category)) {
            placesMode = false;
            wallpaperIntervalMs = 15_000L;
            if (heroShelf != null) {
                heroShelf.setVisibility(View.VISIBLE);
            }
            List<File> madinahFiles = MadinahWallpapers.list(this);
            if (!madinahFiles.isEmpty()) {
                wallpapers = madinahFiles;
                getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
                showNextWallpaper(true);
                startWallpaperRotation();
                Toast.makeText(this, "Wallpaper: 🕌 Madinah (15s rotation)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, "Category: " + displayLabel, Toast.LENGTH_SHORT).show();

        if ("Natural".equalsIgnoreCase(category)) {
            placesMode = true;
            List<File> places = PlaceWallpapers.list(this);
            if (!places.isEmpty()) {
                wallpapers = places;
                showNextWallpaper(true);
                return;
            }
        } else if ("Custom".equalsIgnoreCase(category)) {
            placesMode = false;
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.setAlpha(0f);
                heroShelf.setVisibility(View.GONE);
            }
            File customDir = new File(getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            boolean singleMode = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getBoolean("custom_single_mode", false);
            String singleFile = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("custom_single_file", null);
            if (singleMode && singleFile != null) {
                File sf = new File(customDir, singleFile);
                if (sf.exists() && sf.length() > 0) {
                    wallpapers = Collections.singletonList(sf);
                    getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
                    showNextWallpaper(true);
                    return;
                }
            }
            if (files != null && files.length > 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                wallpapers = new ArrayList<>(Arrays.asList(files));
                getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
                showNextWallpaper(true);
                return;
            }
        } else {
            placesMode = false;
            if (heroShelf != null) {
                heroShelf.setVisibility(View.VISIBLE);
            }
        }

        List<File> matching = OnlineWallpapers.listByCategory(this, category);
        if (!matching.isEmpty()) {
            wallpapers = matching;
            showNextWallpaper(true);
        } else {
            Toast.makeText(this, "Memuat turun 4K Wallpaper " + displayLabel + "...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean downloaded = OnlineWallpapers.syncCategory(getApplicationContext(), category);
                handler.post(() -> {
                    if (isDestroyed()) return;
                    List<File> fresh = OnlineWallpapers.listByCategory(MainActivity.this, category);
                    if (!fresh.isEmpty()) {
                        wallpapers = fresh;
                        showNextWallpaper(true);
                    }
                });
            }, "wall-cat-sync").start();
        }
    }

    private void showWallpaperUploadQrDialog() {
        if (uploadServer != null) {
            uploadServer.stop();
            uploadServer = null;
        }

        String localIp = WallpaperUploadServer.getLocalIpAddress();
        int port = 8080;
        WallpaperUploadServer.UploadListener listener = createUploadListener();
        uploadServer = new WallpaperUploadServer(this, port, listener);
        boolean started = uploadServer.start();
        if (!started) {
            port = 8888;
            uploadServer = new WallpaperUploadServer(this, port, listener);
            started = uploadServer.start();
        }

        if (!started) {
            Toast.makeText(this, "Ralat: Tidak dapat memulakan pelayan muat naik tempatan.", Toast.LENGTH_LONG).show();
            return;
        }

        String uploadUrl = "http://" + localIp + ":" + port;
        Bitmap qrBmp = QrBitmapUtil.createQrBitmap(uploadUrl, Math.round(260 * density));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        int padH = Math.round(36 * density);
        int padV = Math.round(28 * density);
        rootLayout.setPadding(padH, padV, padH, padV);
        rootLayout.setBackgroundColor(0xF0131722); // Elegant dark slate

        // Title
        TextView title = new TextView(this);
        title.setText("📲 Muat Naik Wallpaper Dari Telefon");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        rootLayout.addView(title);

        // Subtitle instructions
        TextView sub = new TextView(this);
        sub.setText("Sambungkan telefon pada Wi-Fi yang sama, kemudian imbas kod QR di bawah:");
        sub.setTextColor(0xFF94A3B8);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Math.round(6 * density), 0, Math.round(18 * density));
        rootLayout.addView(sub);

        // QR Code Container (Crisp White Card with subtle margin)
        FrameLayout qrContainer = new FrameLayout(this);
        qrContainer.setBackgroundColor(0xFFFFFFFF);
        int qrPad = Math.round(12 * density);
        qrContainer.setPadding(qrPad, qrPad, qrPad, qrPad);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qrLp.gravity = Gravity.CENTER_HORIZONTAL;
        qrContainer.setLayoutParams(qrLp);

        qrImageView = new ImageView(this);
        if (qrBmp != null) {
            qrImageView.setImageBitmap(qrBmp);
        }
        int qrDim = Math.round(220 * density);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(qrDim, qrDim);
        imgLp.gravity = Gravity.CENTER;
        qrImageView.setLayoutParams(imgLp);
        qrContainer.addView(qrImageView);
        rootLayout.addView(qrContainer);

        // Web URL pill
        qrUrlText = new TextView(this);
        qrUrlText.setText("🌐 " + uploadUrl);
        qrUrlText.setTextColor(0xFF38BDF8);
        qrUrlText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        qrUrlText.setTypeface(Typeface.DEFAULT_BOLD);
        qrUrlText.setGravity(Gravity.CENTER);
        qrUrlText.setPadding(0, Math.round(14 * density), 0, Math.round(4 * density));
        rootLayout.addView(qrUrlText);

        // Status text
        qrStatusText = new TextView(this);
        qrStatusText.setText("📡 Menunggu imbasan kamera atau fail dari telefon...");
        qrStatusText.setTextColor(0xFFCBD5E1);
        qrStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        qrStatusText.setGravity(Gravity.CENTER);
        qrStatusText.setPadding(0, 0, 0, Math.round(18 * density));
        rootLayout.addView(qrStatusText);

        // Close button
        Button btnClose = new Button(this);
        btnClose.setText("Tutup / Cancel");
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setBackgroundResource(R.drawable.pill_bg);
        btnClose.setFocusable(true);
        btnClose.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                Math.round(180 * density), Math.round(42 * density));
        btnLp.gravity = Gravity.CENTER_HORIZONTAL;
        btnClose.setLayoutParams(btnLp);
        rootLayout.addView(btnClose);

        // Watermark credit
        TextView credit = new TextView(this);
        credit.setText("✨ Powered by @imsomangge");
        credit.setTextColor(0xFF94A3B8);
        credit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        credit.setTypeface(Typeface.DEFAULT_BOLD);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, Math.round(14 * density), 0, 0);
        rootLayout.addView(credit);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setView(rootLayout);
        qrUploadDialog = builder.create();

        btnClose.setOnClickListener(v -> qrUploadDialog.dismiss());

        qrUploadDialog.setOnDismissListener(dialog -> {
            if (uploadServer != null) {
                uploadServer.stop();
                uploadServer = null;
            }
            qrUploadDialog = null;
        });

        qrUploadDialog.show();
        btnClose.requestFocus();
    }

    private WallpaperUploadServer.UploadListener createUploadListener() {
        return new WallpaperUploadServer.UploadListener() {
            @Override
            public void onUploaded(File imageFile) {
                onCustomWallpaperUploaded(imageFile);
            }

            @Override
            public void onDeleted(File file) {
                onCustomWallpaperDeleted(file);
            }

            @Override
            public void onSelected(File file) {
                onCustomWallpaperSelected(file);
            }

            @Override
            public void onSlideshowMode() {
                onCustomWallpaperSlideshow();
            }

            @Override
            public void onCleared() {
                onCustomWallpaperCleared();
            }
        };
    }

    private void onCustomWallpaperUploaded(File file) {
        runOnUiThread(() -> {
            SoundEffects.playSelect();
            Toast.makeText(this, "✅ Wallpaper berjaya dimuat naik dari telefon!", Toast.LENGTH_LONG).show();

            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit()
                    .putString("active_category", "Custom")
                    .putBoolean("custom_single_mode", false)
                    .remove("custom_single_file")
                    .apply();

            placesMode = false;
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.setAlpha(0f);
                heroShelf.setVisibility(View.GONE);
            }
            File customDir = new File(getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            if (files != null && files.length > 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                wallpapers = new ArrayList<>(Arrays.asList(files));
            } else {
                wallpapers = Collections.singletonList(file);
            }

            getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
            showNextWallpaper(true);

            if (qrStatusText != null) {
                qrStatusText.setText("🎉 Berjaya Diterima & Dipaparkan di TV!");
                qrStatusText.setTextColor(0xFF4ADE80);
            }
            if (qrUrlText != null) {
                qrUrlText.setText("✅ Wallpaper Aktif: " + file.getName());
                qrUrlText.setTextColor(0xFF4ADE80);
            }
            if (qrImageView != null) {
                try {
                    Bitmap thumb = decodeWallpaper(file);
                    if (thumb != null) {
                        qrImageView.setImageBitmap(thumb);
                        qrImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                } catch (Exception ignored) {}
            }

            handler.postDelayed(() -> {
                if (qrUploadDialog != null && qrUploadDialog.isShowing()) {
                    qrUploadDialog.dismiss();
                }
            }, 3000);
        });
    }

    private void onCustomWallpaperSelected(File file) {
        runOnUiThread(() -> {
            SoundEffects.playSelect();
            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit()
                    .putString("active_category", "Custom")
                    .putBoolean("custom_single_mode", true)
                    .putString("custom_single_file", file.getName())
                    .apply();

            placesMode = false;
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.setAlpha(0f);
                heroShelf.setVisibility(View.GONE);
            }
            wallpapers = Collections.singletonList(file);
            getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
            showNextWallpaper(true);
            Toast.makeText(this, "📌 Kunci 1 Wallpaper: " + file.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void onCustomWallpaperSlideshow() {
        runOnUiThread(() -> {
            SoundEffects.playSelect();
            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit()
                    .putString("active_category", "Custom")
                    .putBoolean("custom_single_mode", false)
                    .remove("custom_single_file")
                    .apply();

            placesMode = false;
            if (heroShelf != null) {
                heroShelf.animate().cancel();
                heroShelf.setAlpha(0f);
                heroShelf.setVisibility(View.GONE);
            }

            File customDir = new File(getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            if (files != null && files.length > 0) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                wallpapers = new ArrayList<>(Arrays.asList(files));
                getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
                showNextWallpaper(true);
                Toast.makeText(this, "🔄 Mod Putar Semua Wallpaper Aktif (" + files.length + " foto)", Toast.LENGTH_SHORT).show();
            } else {
                switchWallpaperCategory("Natural");
            }
        });
    }

    private void onCustomWallpaperDeleted(File file) {
        runOnUiThread(() -> {
            String singleFile = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("custom_single_file", null);
            if (singleFile != null && singleFile.equals(file.getName())) {
                getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit()
                        .putBoolean("custom_single_mode", false)
                        .remove("custom_single_file")
                        .apply();
            }

            File customDir = new File(getFilesDir(), "custom_wallpapers");
            File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
            String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Natural");

            if ("Custom".equalsIgnoreCase(activeCat)) {
                if (files != null && files.length > 0) {
                    Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    wallpapers = new ArrayList<>(Arrays.asList(files));
                    getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("index", -1).apply();
                    showNextWallpaper(true);
                    Toast.makeText(this, "🗑️ Photo deleted. Remaining: " + files.length + " photos.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "All custom photos deleted. Switching to Madinah.", Toast.LENGTH_SHORT).show();
                    switchWallpaperCategory("Madinah");
                }
            } else {
                Toast.makeText(this, "🗑️ Photo deleted.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCustomWallpaperCleared() {
        runOnUiThread(() -> {
            getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit()
                    .putBoolean("custom_single_mode", false)
                    .remove("custom_single_file")
                    .apply();

            String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");
            if ("Custom".equalsIgnoreCase(activeCat)) {
                switchWallpaperCategory("Madinah");
            }
            Toast.makeText(this, "🧹 All uploaded photos have been cleared.", Toast.LENGTH_SHORT).show();
        });
    }

    private void showCustomWallpaperManagerDialog() {
        File customDir = new File(getFilesDir(), "custom_wallpapers");
        File[] files = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
        if (files == null || files.length == 0) {
            Toast.makeText(this, "No uploaded wallpapers found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        boolean isSingleMode = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getBoolean("custom_single_mode", false);
        String singleFile = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("custom_single_file", "");

        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        // Quick Mode Actions at top
        items.add("🔄 Rotate All Photos (Slideshow Mode)");
        actions.add(this::onCustomWallpaperSlideshow);

        items.add("🗑️ Delete All Photos (Clear All)");
        actions.add(() -> {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("⚠️ Delete All Photos?")
                    .setMessage("Are you sure you want to delete all " + files.length + " uploaded wallpapers?")
                    .setPositiveButton("Yes, Delete All", (d, w) -> {
                        for (File f : files) f.delete();
                        onCustomWallpaperCleared();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
        for (File f : files) {
            double kb = f.length() / 1024.0;
            String sizeStr = kb > 1024 ? String.format(Locale.ROOT, "%.1f MB", kb / 1024.0) : String.format(Locale.ROOT, "%.0f KB", kb);
            boolean isCurrent = isSingleMode && f.getName().equals(singleFile);
            String prefix = isCurrent ? "📌 [Locked] " : "🖼️ ";
            items.add(prefix + f.getName() + " (" + sizeStr + " • " + sdf.format(new Date(f.lastModified())) + ")");
            actions.add(() -> showSingleCustomActionDialog(f));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("📱 TV Wallpaper Manager (" + files.length + " Photos)");
        builder.setItems(items.toArray(new String[0]), (dialog, which) -> {
            if (which >= 0 && which < actions.size()) {
                actions.get(which).run();
            }
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSingleCustomActionDialog(File file) {
        String[] actions = {
                "📌 Set as Single Wallpaper (Lock This Photo)",
                "🗑️ Delete This Photo",
                "🧹 Keep This Photo Only & Delete Others",
                "⬅️ Back"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🖼️ " + file.getName());
        builder.setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0: // Lock single
                    onCustomWallpaperSelected(file);
                    break;
                case 1: // Delete this
                    file.delete();
                    onCustomWallpaperDeleted(file);
                    break;
                case 2: // Keep this, delete others
                    File customDir = new File(getFilesDir(), "custom_wallpapers");
                    File[] allFiles = customDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg"));
                    if (allFiles != null) {
                        for (File f : allFiles) {
                            if (!f.getName().equals(file.getName())) {
                                f.delete();
                            }
                        }
                    }
                    onCustomWallpaperSelected(file);
                    Toast.makeText(this, "🧹 Other photos deleted. This photo is preserved.", Toast.LENGTH_SHORT).show();
                    break;
                case 3: // Back
                    showCustomWallpaperManagerDialog();
                    break;
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showHazeLocationDialog() {
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
        builder.setTitle("📍 Select APIMS / JAS Air Quality Station");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            AppLocationManager.Station chosen = stations.get(which);
            AppLocationManager.setStation(MainActivity.this, chosen);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "AQI Station: " + chosen.name, Toast.LENGTH_SHORT).show();
            refreshLocationDependentData();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPrayerZoneDialog() {
        final List<PrayerTimes.PrayerZone> zones = PrayerTimes.getPresetZones();
        String activeZone = PrayerTimes.getActiveZone(this);
        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[zones.size()];
        for (int i = 0; i < zones.size(); i++) {
            PrayerTimes.PrayerZone z = zones.get(i);
            items[i] = z.getDisplayName();
            if (z.code.equalsIgnoreCase(activeZone)) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🕌 Select Prayer Times Zone");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            PrayerTimes.PrayerZone chosen = zones.get(which);
            PrayerTimes.setPrayerZone(MainActivity.this, chosen);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Prayer Zone: " + chosen.name, Toast.LENGTH_SHORT).show();
            PrayerTimes.sync(MainActivity.this, MainActivity.this::updatePrayerTimes);
            refreshLocationDependentData();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /** Shows an Apple TV-style frosted glass Putrajaya prayer timetable popup. */
    private void showPrayerPopup() {
        if (openMenu != null) return;
        SoundEffects.playSelect();
        View anchor = prayerTimesView != null ? prayerTimesView : findViewById(R.id.leftBlock);
        FrameLayout panel = (FrameLayout) getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        Runnable close = () -> closeMenu(false);

        // Header: Active Prayer Zone
        addInputRow(rows, close, R.drawable.ic_settings, "Prayer Times: " + PrayerTimes.getShortZoneName(this), TvInputManager.INPUT_STATE_CONNECTED, () -> {});

        // Date row: Islamic Hijri
        addInputRow(rows, close, R.drawable.ic_input, PrayerTimes.getHijriDate(this), -1, () -> {});

        addSeparator(rows);

        // Prayer times list
        java.util.List<PrayerTimes.ScheduleItem> schedule = PrayerTimes.getFullSchedule(this);
        for (PrayerTimes.ScheduleItem item : schedule) {
            addPrayerRow(rows, close, item.name, item.timeFormatted, item.isNext);
        }

        addSeparator(rows);

        // Close
        addInputRow(rows, close, R.drawable.ic_move, "Close", -1, () -> {
            SoundEffects.playSelect();
            closeMenu(true);
        });

        int width = (int) (270 * density);
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        width = Math.max(width, panel.getMeasuredWidth());

        showMenu(panel, rows, anchor, width, false);
    }

    private void addPrayerRow(LinearLayout rows, Runnable close, String name, String time, boolean isNext) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(R.drawable.ic_input);
        TextView title = row.findViewById(R.id.title);
        title.setText(name + (isNext ? " (Next)" : ""));
        TextView status = row.findViewById(R.id.status);
        View dot = row.findViewById(R.id.dot);
        status.setText(time);
        status.setVisibility(View.VISIBLE);
        if (isNext) {
            dot.setVisibility(View.VISIBLE);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            status.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            dot.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> {
            SoundEffects.playSelect();
            close.run();
        });
        rows.addView(row);
    }

    private void checkInactivity() {
        if (!hasWindowFocus() || isFinishing() || isDestroyed()) return;
        long idleMs = android.os.SystemClock.elapsedRealtime() - lastUserInteraction;
        if (idleMs >= TV_SLEEP_TIMEOUT_MS) {
            turnOffTv();
        } else if (idleMs >= SCREENSAVER_TIMEOUT_MS && !screensaverActive) {
            showNightstandScreensaver();
        } else if (screensaverActive && screensaverView != null) {
            updateNightstandScreensaver();
        }
    }

    private void showNightstandScreensaver() {
        if (screensaverActive || isFinishing() || isDestroyed()) return;
        if (wallpaperFullscreen) {
            setWallpaperFullscreen(false);
        }
        screensaverActive = true;
        if (screensaverView == null) {
            screensaverView = getLayoutInflater().inflate(R.layout.view_nightstand, root, false);
            screensaverView.setOnClickListener(v -> dismissScreensaver());
            root.addView(screensaverView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        updateNightstandScreensaver();

        screensaverView.setVisibility(View.VISIBLE);
        screensaverView.setAlpha(0f);
        screensaverView.animate().alpha(1f).setDuration(500).start();
        startWallpaperRotation();
        android.util.Log.i("TvHome", "Nightstand screensaver activated");
    }

    private void updateNightstandScreensaver() {
        if (screensaverView == null) return;
        ImageView backdrop = screensaverView.findViewById(R.id.nightstandBackdrop);
        if (backdrop != null && wallBitmap != null) {
            backdrop.setImageBitmap(wallBitmap);
        }

        TextView hijri = screensaverView.findViewById(R.id.nightstandHijri);
        if (hijri != null) hijri.setText("🌙 " + PrayerTimes.getHijriDate(this));

        TextView prayer = screensaverView.findViewById(R.id.nightstandPrayer);
        if (prayer != null) prayer.setText("🕌 " + PrayerTimes.getNextPrayerSummary(this));

        // Center locked strictly with no movement
        View content = screensaverView.findViewById(R.id.nightstandContent);
        if (content != null) {
            content.animate().cancel();
            content.setTranslationX(0f);
            content.setTranslationY(0f);
        }

        // iPhone StandBy Wind Down Mode: 10:00 PM (22:00) to 6:00 AM (06:00) -> Red Monochrome
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        boolean isNightMode = (hour >= 22 || hour < 6);

        TextView clock = screensaverView.findViewById(R.id.nightstandClock);
        TextView ampm = screensaverView.findViewById(R.id.nightstandAmPm);
        TextView date = screensaverView.findViewById(R.id.nightstandDate);
        TextView hint = screensaverView.findViewById(R.id.nightstandHint);

        if (isNightMode) {
            int red = 0xFFFF453A;
            if (clock != null) clock.setTextColor(red);
            if (ampm != null) ampm.setTextColor(0xCCFF453A);
            if (date != null) date.setTextColor(0xE6FF453A);
            if (hijri != null) hijri.setTextColor(0xE6FF453A);
            if (prayer != null) {
                prayer.setTextColor(red);
                android.graphics.drawable.GradientDrawable redPill = new android.graphics.drawable.GradientDrawable();
                redPill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                redPill.setColor(0x26FF453A);
                redPill.setStroke((int) (1 * density), 0x59FF453A);
                redPill.setCornerRadius(21 * density);
                prayer.setBackground(redPill);
            }
            if (hint != null) hint.setTextColor(0x55FF453A);
        } else {
            if (clock != null) clock.setTextColor(0xFFFFFFFF);
            if (ampm != null) ampm.setTextColor(0xB3FFFFFF);
            if (date != null) date.setTextColor(0xE6FFFFFF);
            if (hijri != null) hijri.setTextColor(0xD9E6E0D2);
            if (prayer != null) {
                prayer.setTextColor(0xFFFFFFFF);
                prayer.setBackgroundResource(R.drawable.pill_bg);
            }
            if (hint != null) hint.setTextColor(0x4DFFFFFF);
        }
    }

    private void dismissScreensaver() {
        if (!screensaverActive || screensaverView == null) return;
        screensaverActive = false;
        lastUserInteraction = android.os.SystemClock.elapsedRealtime();
        screensaverView.animate().alpha(0f).setDuration(350).withEndAction(() -> {
            if (!screensaverActive && screensaverView != null) {
                screensaverView.setVisibility(View.GONE);
            }
        }).start();
        startWallpaperRotation();
        android.util.Log.i("TvHome", "Nightstand screensaver dismissed");
    }

    private void turnOffTv() {
        android.util.Log.i("TvHome", "TV idle: 10m screensaver + 2h timeout reached, putting TV to sleep");
        boolean ok = HomeKeyService.sleepScreen();
        if (!ok) {
            try {
                Runtime.getRuntime().exec("input keyevent 223");
            } catch (Exception e) {
                android.util.Log.w("TvHome", "Failed to send sleep keyevent: " + e);
            }
        }
    }

    private void ensureSystemScreensaverAndSleepSettings() {
        new Thread(() -> {
            try {
                android.content.ContentResolver cr = getContentResolver();
                // 10 minutes idle -> screensaver (600,000 ms)
                android.provider.Settings.System.putInt(cr, "screen_off_timeout", 600000);
                // 2 hours after screensaver -> TV off (10m + 2h = 7,800,000 ms)
                android.provider.Settings.Secure.putInt(cr, "sleep_timeout", 7800000);
                android.provider.Settings.Secure.putInt(cr, "screensaver_enabled", 1);
                android.provider.Settings.Secure.putString(cr, "screensaver_components", "com.gathin.tvhome/.ClockDream");
            } catch (Exception ignored) {
            }
        }, "system-timeout-sync").start();
    }


    /**
     * China-market MIUI TV firmware answers no standard Settings intents at all; open Xiaomi's own
     * settings app (com.xiaomi.mitv.settings) there, so the Settings and Wi-Fi pills keep working.
     * On firmware with a standard settings handler the behaviour is unchanged.
     */
    private Intent systemIntent(String standardAction) {
        Intent i = new Intent(standardAction);
        if (i.resolveActivity(getPackageManager()) != null) return i;
        i = new Intent().setComponent(new ComponentName(
                "com.xiaomi.mitv.settings", "com.xiaomi.mitv.settings.entry.MainActivity"));
        if (i.resolveActivity(getPackageManager()) != null) return i;
        return new Intent(standardAction);   // nothing found: let open() show the can't-open toast
    }

    /** Hides the Inputs pill when it would have nothing to list: streaming boxes have no HDMI-in, AV or tuner. */
    private void updateInputsButton() {
        boolean any = liveTvIntent() != null || !sourcePanelService.isEmpty();
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (!any && tim != null) {
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.isPassthroughInput()) {
                    any = true;
                    break;
                }
            }
        }
        btnInputs.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    /** The Live TV app's launch intent: its leanback entry point, or the configured action if it has none. */
    private Intent liveTvIntent() {
        if (liveTvPkg.isEmpty()) return null;
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(liveTvPkg);
        if (i != null || liveTvAction.isEmpty()) return i;
        Intent byAction = new Intent(liveTvAction).setPackage(liveTvPkg);
        return getPackageManager().resolveActivity(byAction, 0) != null ? byAction : null;
    }

    /** The TV's own source-selection panel, for TVs that have no TvInputManager (TCL Android TV 9). */
    private void openSourcePanel() {
        Intent i = new Intent(sourcePanelAction);
        ComponentName c = ComponentName.unflattenFromString(sourcePanelService);
        if (c != null) i.setComponent(c);
        try {
            startService(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void open(Intent i) {
        SoundEffects.playSelect();
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    /** Switches to a passthrough input (HDMI, AV). The standard way is ACTION_VIEW on its channel URI,
     *  which the Live TV app handles; where nothing takes that, the configured `input_action` is sent
     *  with the same URI instead (XGIMI's HDMI player only answers com.xgimi.action.hdmiPlayer). */
    private void openInput(String inputId) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
        Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
            return;
        } catch (ActivityNotFoundException e) {
            if (inputAction.isEmpty()) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        open(new Intent(inputAction).setDataAndType(uri, "vnd.android.cursor.item/channel"));
    }

    /** The glass menu while one is open (Inputs, or an app's), and what it opened from. */
    private View openMenu;
    private View menuAnchor;
    private final int[] menuAt = new int[2], menuGliderAt = new int[2];

    /**
     * Menu under the Inputs pill: HDMI ports (with connection state), AV and Live TV. It is drawn in
     * Home's own window rather than a popup window of its own: a new window takes about a quarter of
     * a second to reach the screen on this TV, so the selection could not glide out of the pill into
     * it. Here Home's own focus shape glides from the pill to the first row, the way it glides into
     * the dock's tray, and the menu's glass paints it over itself.
     */
    private void showInputs() {
        if (openMenu != null) return;
        SoundEffects.playSelect();
        View anchor = btnInputs;
        FrameLayout panel = (FrameLayout) getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        Runnable close = () -> closeMenu(false);

        // Compact: as wide as the Inputs pill (a touch wider so "HDMI 1" fits), hanging right below it.
        int width = Math.max(anchor.getWidth(), (int) (150 * density));
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        boolean sawNonHdmi = false;
        String tunerId = null;
        boolean tunerIsHw = false;
        if (tim != null) {
            List<TvInputInfo> inputs = new ArrayList<>(tim.getTvInputList());
            // HDMI ports first in port order, then AV and the rest.
            Collections.sort(inputs, (a, b) -> {
                boolean ha = a.getType() == TvInputInfo.TYPE_HDMI, hb = b.getType() == TvInputInfo.TYPE_HDMI;
                if (ha != hb) return ha ? -1 : 1;
                return String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this)));
            });
            // Some TVs (Sony) report every HDMI port as connected whether or not anything is plugged in,
            // so drop the HDMI status then. A TV with every port really in use loses it too.
            int hdmiPorts = 0, hdmiConnected = 0;
            for (TvInputInfo info : inputs) {
                if (!info.isPassthroughInput() || info.getType() != TvInputInfo.TYPE_HDMI) continue;
                hdmiPorts++;
                if (tim.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED) hdmiConnected++;
            }
            boolean hdmiStateBogus = hdmiPorts > 1 && hdmiConnected == hdmiPorts;
            for (TvInputInfo info : inputs) {
                if (info.getType() == TvInputInfo.TYPE_TUNER
                        && (info.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // Prefer a hardware tuner: the framework names those ".../HW<n>". System apps such as
                    // AirPlay also register software inputs of type TUNER and may sort first (seen on TCL G05).
                    boolean hw = info.getId().matches(".*/HW\\d+$");
                    if (tunerId == null || (hw && !tunerIsHw) || (hw == tunerIsHw && info.getId().compareTo(tunerId) < 0)) {
                        tunerId = info.getId();
                        tunerIsHw = hw;
                    }
                }
                if (!info.isPassthroughInput()) continue;
                CharSequence l = info.loadLabel(this);
                String label = l == null ? info.getId() : l.toString();
                boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
                if (hdmi && !label.toUpperCase(Locale.ROOT).contains("HDMI")) label = "HDMI " + label;
                final String id = info.getId();
                int state = hdmi && hdmiStateBogus ? -1 : tim.getInputState(id);
                if (!hdmi && rows.getChildCount() > 0 && !sawNonHdmi) addSeparator(rows);
                if (!hdmi) sawNonHdmi = true;
                addInputRow(rows, close, hdmi ? R.drawable.ic_hdmi : R.drawable.ic_av, label, state,
                        () -> openInput(id));
            }
        }
        // Nothing to list where there is no TvInputManager: the TV's own source panel switches inputs.
        if (rows.getChildCount() == 0 && !sourcePanelService.isEmpty()) {
            addInputRow(rows, close, R.drawable.ic_input, getString(R.string.inputs), -1, this::openSourcePanel);
        }
        Intent liveTv = liveTvIntent();
        if (liveTv != null && tunerId != null) {
            liveTv = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(TvContract.buildChannelUriForPassthroughInput(tunerId), "vnd.android.cursor.item/channel")
                    .setPackage(liveTvPkg);
        }
        if (liveTv != null && config.showLiveTv) {
            if (rows.getChildCount() > 0) addSeparator(rows);
            final Intent liveTvIntent = liveTv;
            addInputRow(rows, close, R.drawable.ic_livetv, getString(R.string.live_tv), -1, () -> open(liveTvIntent));
        }
        if (rows.getChildCount() == 0) {
            Toast.makeText(this, R.string.no_inputs, Toast.LENGTH_SHORT).show();
            return;
        }

        // Fit the widest row: TVs that report a status on every port (Sony) would otherwise squeeze the names out.
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        width = Math.max(width, panel.getMeasuredWidth());

        showMenu(panel, rows, anchor, width, false);
    }

    /**
     * Shows a glass menu in Home's own window, under {@code anchor} or, with {@code above}, over it:
     * Home's focus shape glides from the anchor to the first row, the menu's glass paints it over
     * itself, and the glass and rows fade in around it. Up from the first row (Down from the last when
     * the menu is above), or Back, closes it and the shape glides back to the anchor.
     */
    private void showMenu(FrameLayout panel, LinearLayout rows, View anchor, int width, boolean above) {
        GlassDrawable glass = new GlassDrawable(panel, 20 * density, density, glider, 0.5f, true);
        panel.setBackground(glass);
        Backdrop.watch(panel);
        glider.addCompanion(panel);
        anchor.getLocationInWindow(menuAt);
        root.getLocationInWindow(menuGliderAt);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Math.max(0, Math.min(menuAt[0] - menuGliderAt[0], root.getWidth() - width));
        int gap = (int) (10 * density);
        if (above) {
            panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
            lp.topMargin = Math.max(0, menuAt[1] - menuGliderAt[1] - gap - panel.getMeasuredHeight());
        } else {
            lp.topMargin = menuAt[1] - menuGliderAt[1] + anchor.getHeight() + gap;
        }
        root.addView(panel, lp);
        openMenu = panel;
        menuAnchor = anchor;

        // Focus stays in the menu until it closes. Leaving it towards the anchor closes it, and the
        // selection glides back into what it opened from.
        View first = null, last = null;
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            if (!row.isFocusable()) continue;
            if (first == null) first = row;
            last = row;
            row.setOnFocusChangeListener((v, has) -> {
                if (has) glideToMenuRow(panel, rows, v);
            });
        }
        final View top = first, bottom = last;
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            if (!row.isFocusable()) continue;
            row.setOnKeyListener((v, keyCode, event) -> {
                boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        if (v != top) return false;
                        if (down && !above) closeMenu(true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        if (v != bottom) return false;
                        if (down && above) closeMenu(true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        if (down) closeMenu(true);
                        return true;
                    default:
                        return false;
                }
            });
        }

        // The glass and the rows fade in around the selection as it arrives.
        glass.setAlpha(0);
        rows.setAlpha(0f);
        final float slide = (above ? 8 : -8) * density;
        rows.setTranslationY(slide);
        android.animation.ValueAnimator in = android.animation.ValueAnimator.ofFloat(0f, 1f);
        in.setDuration(220);
        in.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));
        in.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            glass.setAlpha(Math.round(255 * f));
            rows.setAlpha(f);
            rows.setTranslationY(slide * (1f - f));
        });
        in.start();
        if (top != null) top.requestFocus();
    }

    /**
     * Glides Home's focus shape onto a row of the Inputs menu. From layout positions, not on-screen
     * ones: the rows slide into place as the menu opens, and the shape must land where the row ends
     * up, not where it was when focus arrived. The first row takes focus before the menu has been
     * laid out, so then it waits for the layout.
     */
    private void glideToMenuRow(View panel, View rows, View row) {
        SoundEffects.playNav();
        if (!panel.isLaidOut() || row.getHeight() == 0) {
            panel.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    panel.removeOnLayoutChangeListener(this);
                    // Still inside the layout pass here, where the panel does not count as laid
                    // out yet: glide once the pass is over.
                    panel.post(() -> {
                        if (row.hasFocus()) glideToMenuRow(panel, rows, row);
                    });
                }
            });
            return;
        }
        float l = panel.getLeft() + rows.getLeft() + row.getLeft() - glider.getLeft();
        float t = panel.getTop() + rows.getTop() + row.getTop() - glider.getTop();
        gliderOnPill = true;
        glider.setStyle(Glider.DARK);
        glider.moveTo(l, t, l + row.getWidth(), t + row.getHeight(), 13 * density);
    }

    /** Closes the open menu; with {@code toAnchor}, focus and the selection go back to what it opened from. */
    private void closeMenu(boolean toAnchor) {
        View panel = openMenu;
        if (panel == null) return;
        SoundEffects.playNav();
        openMenu = null;
        if (toAnchor && menuAnchor != null) menuAnchor.requestFocus();
        menuAnchor = null;
        panel.animate().alpha(0f).setDuration(reducedMotion ? 80 : 150).withEndAction(() -> {
            glider.removeCompanion(panel);
            root.removeView(panel);
        }).start();
    }

    private boolean inMenu(View v) {
        for (View p = v; p != null && openMenu != null; p = p.getParent() instanceof View ? (View) p.getParent() : null) {
            if (p == openMenu) return true;
        }
        return false;
    }

    private PopupWindow glassPopup(View panel, int width) {
        // The same glass as the pills and the tray: what is behind it, softened, with a shining edge.
        panel.setBackground(new GlassDrawable(panel, 20 * density, density, null, 0.5f));
        Backdrop.watch(panel);
        final PopupWindow popup = new PopupWindow(panel, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // needed for Back to dismiss
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        popup.setOnDismissListener(() -> {
            if (openPopup == popup) openPopup = null;
        });
        openPopup = popup;
        return popup;
    }

    /** Drop in: fade and unfold from the top edge, hanging under the anchor pill. */
    private void dropIn(PopupWindow popup, View panel, View anchor, int gravity) {
        panel.setAlpha(0f);
        panel.setPivotY(0f);
        panel.setScaleY(0.92f);
        panel.setTranslationY(-8 * density);
        popup.showAsDropDown(anchor, 0, (int) (10 * density), gravity);
        panel.animate().alpha(1f).scaleY(1f).translationY(0f).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
    }

    private void addSeparator(LinearLayout rows) {
        View line = new View(this);
        line.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) density);
        int m = (int) (6 * density), h = (int) (12 * density);
        lp.setMargins(h, m, h, m);
        rows.addView(line, lp);
    }

    private void addInputRow(LinearLayout rows, Runnable close, int iconRes, String title, int state, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        TextView status = row.findViewById(R.id.status);
        View dot = row.findViewById(R.id.dot);
        // This TV reports "standby" for every port whether or not something is plugged in, so only
        // definite states are shown.
        if (state == TvInputManager.INPUT_STATE_CONNECTED) {
            status.setText(R.string.connected);
            dot.setVisibility(View.VISIBLE);
        } else if (state == TvInputManager.INPUT_STATE_DISCONNECTED) {
            status.setText(R.string.not_connected);
            dot.setVisibility(View.GONE);
        } else {
            status.setVisibility(View.GONE);
            dot.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> {
            close.run();
            action.run();
        });
        rows.addView(row);
    }

    // ---- module cards ----------------------------------------------------------------------------

    /**
     * Asks about the next card that has no decision for its current signing key. One prompt at a
     * time, only over a laid out grid with window focus (not under a popup or another app), and
     * the next one follows as soon as this one is answered. Nothing is shown until the owner says Show.
     */
    private void showCardPrompt() {
        if (!moduleCards || !tilesReady || promptsPaused || cardPrompt != null || !hasWindowFocus() || isFinishing()) return;
        if (cards.isEmpty() || !CardFeature.working()) return;
        CardFeature.run(() -> {
            showCardPromptNow();
            return null;
        }, null);
    }

    private void showCardPromptNow() {
        final Cards.Card card = Cards.firstPending(cards);
        if (card == null) return;

        View panel = getLayoutInflater().inflate(R.layout.dialog_card_prompt, root, false);
        ((TextView) panel.findViewById(R.id.title)).setText(getString(R.string.card_prompt_title, card.appLabel));
        ((TextView) panel.findViewById(R.id.body)).setText(card.state == Cards.State.RESIGNED
                ? getString(R.string.card_prompt_resigned, card.appLabel)
                : card.state == Cards.State.RELABELLED
                ? getString(R.string.card_prompt_relabelled, card.appLabel)
                : getString(R.string.card_prompt_body, card.appLabel));
        TextView names = panel.findViewById(R.id.names);
        if (Cards.fold(card.label).equals(Cards.fold(card.appLabel))) names.setVisibility(View.GONE);
        else names.setText(getString(R.string.card_prompt_names, card.label, card.appLabel));
        ((TextView) panel.findViewById(R.id.identity)).setText(
                getString(R.string.card_prompt_identity, card.pkg, Cards.shortDigest(card.digest)));
        TextView keep = panel.findViewById(R.id.btnKeep);
        TextView show = panel.findViewById(R.id.btnShow);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(false);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (440 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.6f);
        }
        // One decision per prompt: the buttons and Back (cancel) all end here.
        final boolean[] decided = {false};
        View.OnClickListener choose = v -> {
            if (decided[0]) return;
            decided[0] = true;
            cardPrompt = null;
            dialog.dismiss();
            decideCard(card, v.getId() == R.id.btnShow);   // guards its own card calls; the tile reload stays outside
            restoreFocus();   // the tiles changed under the prompt; this also chains the next prompt
        };
        keep.setOnClickListener(choose);
        show.setOnClickListener(choose);
        dialog.setOnCancelListener(d -> choose.onClick(keep));
        dialog.setOnDismissListener(d -> {
            if (cardPrompt == dialog) cardPrompt = null;
        });
        // Left and Right move between the two buttons; Up and Down stay put; the ends don't wrap.
        View.OnKeyListener keys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return true;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return v.getId() == R.id.btnKeep;
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v.getId() == R.id.btnShow;
        };
        keep.setOnKeyListener(keys);
        show.setOnKeyListener(keys);
        cardPrompt = dialog;
        dialog.show();
        keep.requestFocus();
    }

    /** Writes the owner's decision and rebuilds the tiles; focus is left where it is. */
    private void decideCard(Cards.Card card, boolean show) {
        boolean saved = CardFeature.run(() -> Cards.decide(Cards.storeFile(this), card, show), false);
        if (!saved) {
            Toast.makeText(this, R.string.card_not_saved, Toast.LENGTH_SHORT).show();
            promptsPaused = true;
        }
        reloadTiles();
    }

    /** Dropdown under the Settings pill (hold OK): every discovered card, its package and key; OK toggles Shown and Hidden. */
    private void showModuleCards() {
        if (cards.isEmpty() || !CardFeature.working()) {
            Toast.makeText(this, R.string.no_module_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        CardFeature.run(() -> {
            showModuleCardsNow();
            return null;
        }, null);
    }

    private void showModuleCardsNow() {
        View anchor = findViewById(R.id.btnSettings);
        View panel = getLayoutInflater().inflate(R.layout.popup_cards, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        final PopupWindow popup = glassPopup(panel, (int) (400 * density));
        List<Cards.Card> listed = new ArrayList<>(cards);
        Collections.sort(listed, Cards::byLabel);
        for (Cards.Card card : listed) {
            View row = getLayoutInflater().inflate(R.layout.item_card_row, rows, false);
            row.setTag(card.pkg);
            ((TextView) row.findViewById(R.id.title)).setText(card.label);
            TextView app = row.findViewById(R.id.app);
            if (Cards.fold(card.label).equals(Cards.fold(card.appLabel))) app.setVisibility(View.GONE);
            else app.setText(getString(R.string.card_app_name, card.appLabel));
            ((TextView) row.findViewById(R.id.pkg)).setText(card.pkg);
            ((TextView) row.findViewById(R.id.digest)).setText(Cards.longDigest(card.digest));
            final String pkg = card.pkg;
            row.setOnClickListener(v -> CardFeature.run(() -> {
                // The list stays open; the decision is for the card as it is now, not as it was listed.
                Cards.Card now = cardFor(pkg);
                if (now == null) return null;
                decideCard(now, now.state != Cards.State.SHOWN);
                // Every row, not only this one: a toggle can move another card across the limit of 8.
                updateCardStates(rows);
                return null;
            }, null));
            rows.addView(row);
        }
        updateCardStates(rows);
        // Scroll inside the panel instead of running off the bottom of a 720p screen.
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] at = new int[2];
        anchor.getLocationInWindow(at);
        int room = root.getHeight() - (at[1] + anchor.getHeight()) - (int) (34 * density);
        popup.setHeight(Math.min(panel.getMeasuredHeight(), Math.max(room, (int) (120 * density))));
        dropIn(popup, panel, anchor, Gravity.END);
        rows.getChildAt(0).requestFocus();
    }

    private Cards.Card cardFor(String pkg) {
        for (Cards.Card c : cards) if (c.pkg.equals(pkg)) return c;
        return null;
    }

    /** Writes the state of every row in the Module cards list from the current card list. */
    private void updateCardStates(LinearLayout rows) {
        List<Cards.Card> drawn = Cards.shown(cards);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            Cards.Card card = row.getTag() instanceof String ? cardFor((String) row.getTag()) : null;
            ((TextView) row.findViewById(R.id.state)).setText(card != null ? stateText(card, drawn.contains(card)) : "");
        }
    }

    /** The row's state: a card the owner set to Shown but which is beyond the eight drawn says so. */
    private String stateText(Cards.Card card, boolean drawn) {
        switch (card.state) {
            case SHOWN:
                return getString(drawn ? R.string.card_state_shown : R.string.card_state_over_limit);
            case HIDDEN:
                return getString(R.string.card_state_hidden);
            case RESIGNED:
                return getString(R.string.card_state_new_key);
            case RELABELLED:
                return getString(R.string.card_state_new_label);
            default:
                return getString(R.string.card_state_new);
        }
    }
}
