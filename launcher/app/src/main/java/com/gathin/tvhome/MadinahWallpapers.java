package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Manages user's Madinah wallpapers stored locally on the TV.
 * Supports 15-second rotation and high-resolution decoding tailored for TV display.
 */
final class MadinahWallpapers {
    private static final String TAG = "TvHome.Madinah";
    static final String DIR = "madinah-wallpapers";

    private MadinahWallpapers() {
    }

    static File dir(Context ctx) {
        File base = Config.dir(ctx);
        if (base != null) {
            File d = new File(base, DIR);
            if (d.exists() && d.isDirectory()) return d;
        }
        File internal = new File(ctx.getFilesDir(), DIR);
        if (internal.exists() && internal.isDirectory()) return internal;
        return base != null ? new File(base, DIR) : internal;
    }

    /** Returns all valid Madinah wallpapers in sorted order. */
    static List<File> list(Context ctx) {
        List<File> out = new ArrayList<>();
        File d = dir(ctx);
        File[] files = d != null ? d.listFiles() : null;
        if (files == null) return out;
        for (File f : files) {
            if (f.isFile() && f.length() > 0) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) {
                    out.add(f);
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    /** Safely decodes wallpaper bitmap downsampled for TV memory efficiency. */
    static Bitmap decodeWallpaper(File file, DisplayMetrics dm) {
        if (file == null || !file.exists()) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), opts);
            int sample = 1;
            int targetW = (dm != null && dm.widthPixels > 0) ? dm.widthPixels : 1920;
            int targetH = (dm != null && dm.heightPixels > 0) ? dm.heightPixels : 1080;
            while (opts.outWidth / (sample * 2) >= targetW && opts.outHeight / (sample * 2) >= targetH) {
                sample *= 2;
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getPath(), opts);
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "Failed decoding wallpaper " + file.getName() + ": " + e);
            return null;
        }
    }

    /** Returns [Title, Overview/Description, Location/Country] for hero shelf display. */
    static String[] describe(File picture) {
        if (picture == null) return null;
        String name = picture.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            File text = new File(picture.getParentFile(), name.substring(0, dot) + ".txt");
            if (text.isFile() && text.length() > 0 && text.length() <= 8192) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(new FileInputStream(text), StandardCharsets.UTF_8))) {
                    String title = r.readLine();
                    if (title != null && !title.trim().isEmpty()) {
                        String desc = r.readLine();
                        String country = r.readLine();
                        return new String[]{
                                title.trim(),
                                desc == null ? "" : desc.trim(),
                                country == null ? "Saudi Arabia" : country.trim()
                        };
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return new String[]{
                "Al-Madinah Al-Munawwarah",
                "Al-Masjid an-Nabawi (The Prophet's Mosque), Madinah",
                "Madinah, Saudi Arabia"
        };
    }
}
