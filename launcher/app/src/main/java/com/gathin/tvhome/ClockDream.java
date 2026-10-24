package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screensaver: Clean digital clock positioned in the top-right corner of the TV screen,
 * floating over rotating high-resolution wallpapers (e.g. Madinah).
 * Periodically drifts slightly around the top-right corner to prevent OLED burn-in.
 */
public class ClockDream extends DreamService {

    private static final String TAG = "TvHome.ClockDream";
    private static final long MOVE_MS = 60_000L;
    private static final long ROTATE_MS = 15_000L; // 15-second rotation
    private static final long DREAM_SLEEP_TIMEOUT_MS = 2L * 60 * 60 * 1000L; // 2 hours

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final ExecutorService wallpaperExecutor = Executors.newSingleThreadExecutor();
    private View content;
    private ImageView dreamBackdrop;
    private List<File> wallpapers = Collections.emptyList();
    private int currentWallpaperIndex = -1;
    private Bitmap currentWallpaperBmp = null;
    private long dreamStartTime = 0;

    private final Runnable wallpaperRotateTick = new Runnable() {
        @Override
        public void run() {
            showNextWallpaper();
            handler.postDelayed(this, ROTATE_MS);
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (dreamStartTime > 0 && android.os.SystemClock.elapsedRealtime() - dreamStartTime >= DREAM_SLEEP_TIMEOUT_MS) {
                Log.i(TAG, "Screensaver ran for 2 hours: turning off TV display");
                HomeKeyService.sleepScreen();
                try {
                    Runtime.getRuntime().exec("input keyevent 223");
                } catch (Exception ignored) {
                }
                finish();
                return;
            }
            moveClock();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);

        dreamBackdrop = findViewById(R.id.dreamBackdrop);
        loadWallpapers();

        content = findViewById(R.id.dreamContent);

        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) placeClock();
        });
    }

    private void loadWallpapers() {
        String activeCat = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).getString("active_category", "Madinah");
        if ("Madinah".equalsIgnoreCase(activeCat)) {
            wallpapers = MadinahWallpapers.list(this);
        }
        if (wallpapers == null || wallpapers.isEmpty()) {
            wallpapers = MadinahWallpapers.list(this);
        }
        if (wallpapers == null || wallpapers.isEmpty()) {
            wallpapers = PlaceWallpapers.list(this);
        }
    }

    private void showNextWallpaper() {
        if (wallpapers == null || wallpapers.isEmpty() || dreamBackdrop == null) return;
        currentWallpaperIndex = (currentWallpaperIndex + 1) % wallpapers.size();
        final File file = wallpapers.get(currentWallpaperIndex);
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        wallpaperExecutor.execute(() -> {
            final Bitmap bmp = MadinahWallpapers.decodeWallpaper(file, dm);
            if (bmp == null) return;
            handler.post(() -> {
                if (dreamBackdrop == null) return;
                Bitmap old = currentWallpaperBmp;
                currentWallpaperBmp = bmp;
                if (old != null) {
                    TransitionDrawable td = new TransitionDrawable(new android.graphics.drawable.Drawable[]{
                            new BitmapDrawable(getResources(), old),
                            new BitmapDrawable(getResources(), bmp)
                    });
                    td.setCrossFadeEnabled(true);
                    dreamBackdrop.setImageDrawable(td);
                    td.startTransition(800);
                    handler.postDelayed(() -> {
                        if (currentWallpaperBmp == bmp && dreamBackdrop != null) {
                            dreamBackdrop.setImageBitmap(bmp);
                        }
                    }, 900);
                } else {
                    dreamBackdrop.setImageBitmap(bmp);
                }
            });
        });
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        dreamStartTime = android.os.SystemClock.elapsedRealtime();
        loadWallpapers();
        showNextWallpaper();
        handler.removeCallbacks(wallpaperRotateTick);
        handler.postDelayed(wallpaperRotateTick, ROTATE_MS);
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1000).start();
        });
        handler.postDelayed(tick, MOVE_MS);
    }

    @Override
    public void onDreamingStopped() {
        dreamStartTime = 0;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(wallpaperRotateTick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        dreamStartTime = 0;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(wallpaperRotateTick);
        super.onDetachedFromWindow();
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(800).withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1000).start();
        }).start();
    }

    /** Positions digital clock in the top-right corner with gentle OLED anti-burn-in drift. */
    private void placeClock() {
        if (content == null) return;
        View parent = (View) content.getParent();
        if (parent == null || parent.getWidth() <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float baseMarginTop = 48f * density;
        float baseMarginEnd = 64f * density;

        // Microscopic 10-15px drift for OLED protection
        float nudgeX = 12f * density * (2f * random.nextFloat() - 1f);
        float nudgeY = 8f * density * (2f * random.nextFloat() - 1f);

        float targetX = parent.getWidth() - content.getWidth() - baseMarginEnd + nudgeX;
        float targetY = baseMarginTop + nudgeY;

        content.setX(targetX);
        content.setY(targetY);
    }
}
