package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A blurred copy of the wallpaper as it sits on screen, so glass can show what is behind it.
 *
 * <p>Android 10 has no live blur: RenderEffect arrived in Android 12. The wallpaper only changes
 * every few minutes, though, so it is blurred once per change at half the screen's size, and each
 * glass surface draws the patch of it that lies underneath, scaled back up, with the focused app's
 * glow over it, as the glow lies over the wallpaper itself. The two are made into one picture per
 * colour ahead of time, so at rest each piece of glass is a single textured shape.
 */
final class Backdrop {

    /**
     * The blurred copy is this many times smaller than the screen in each direction. Shrinking is
     * itself a blur, so this sets a floor on the softness: an eighth was too soft to tune down.
     */
    static final int SCALE = 2;
    /** Box radius and passes at the small size: about 4px of softening on screen. */
    private static final int RADIUS = 2;
    private static final int PASSES = 2;

    private static Bitmap blurred;
    /**
     * A finished-with blurred copy kept for the next picture to be drawn into, and the blur's two
     * working arrays. On a TV that changes picture every few seconds, making these afresh each time
     * churned several megabytes a minute, which is the kind of garbage that makes a long session lag.
     */
    private static Bitmap spare;
    private static int[] pixels, scratch;
    private static final Object SPARE_LOCK = new Object();
    /** While the wallpaper fades to a new picture, the glass fades from the old blur to the new one. */
    private static Bitmap previous;
    private static float blurMix = 1f;
    private static android.animation.ValueAnimator blurFade;
    /** The glow over the wallpaper: its shape, the focused app's colour and how far it is faded in. */
    private static Bitmap glowShape;
    /** While the room blends from one app's colour to the next, both, and how far along it is. */
    private static int glowFrom, glowTo;
    private static float glowMix = 1f;
    private static float glowAlpha;
    private static final List<WeakReference<View>> watchers = new ArrayList<>();
    /**
     * The blurred copy with the glow already in it, one per colour on screen. Painting the glow over
     * the glass as a layer of its own cost a full pass over every piece of glass on every frame;
     * a copy made once per colour, off the main thread, lets the glass paint a single layer instead.
     * Until a colour's copy is ready the glass paints the glow as a layer, as before.
     * {@link #tintedGen} is the blurred copy they were made from.
     */
    private static final java.util.LinkedHashMap<Integer, Bitmap> tinted = new java.util.LinkedHashMap<>();
    private static final java.util.Set<Integer> tinting = new java.util.HashSet<>();
    private static int blurGen, tintedGen = -1;
    private static Executor tintWorker;
    private static Handler tintMain;

    private Backdrop() {
    }

    /** Where the coloured copies are made, and where they are handed back. Main thread. */
    static void setWorker(Executor worker, Handler main) {
        tintWorker = worker;
        tintMain = main;
        requestTints();
    }

    /** The coloured copy for {@code color}, or null while it is being made. Main thread. */
    private static Bitmap tintedFor(int color) {
        return tintedGen == blurGen ? tinted.get(color) : null;
    }

    /** Starts making the coloured copies the glass needs now. Main thread. */
    private static void requestTints() {
        if (tintedGen != blurGen) {
            tinted.clear();
            tintedGen = blurGen;
        }
        requestTint(glowTo);
        if (glowMix < 1f) requestTint(glowFrom);
    }

    private static void requestTint(int color) {
        final Bitmap base = blurred, shape = glowShape;
        final Executor worker = tintWorker;
        final Handler main = tintMain;
        if (base == null || shape == null || worker == null || main == null) return;
        if (tinted.containsKey(color) || !tinting.add(color)) return;
        final int gen = blurGen;
        try {
            worker.execute(() -> {
                Bitmap made = tint(base, shape, color);
                main.post(() -> {
                    tinting.remove(color);
                    // A new picture came while this one was being made: make it again for that.
                    if (gen != blurGen || made == null) {
                        requestTints();
                        return;
                    }
                    tinted.put(color, made);
                    // Keep the colours on screen and this one, and a few recent ones; let the rest go.
                    java.util.Iterator<Integer> it = tinted.keySet().iterator();
                    while (tinted.size() > 4 && it.hasNext()) {
                        int k = it.next();
                        if (k != glowFrom && k != glowTo && k != color) it.remove();
                    }
                    invalidateWatchers();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            tinting.remove(color);   // the activity that owned the worker has gone
        }
    }

    /** The blurred copy with the glow over it at full strength, as the glass would paint the two. */
    private static Bitmap tint(Bitmap base, Bitmap shape, int color) {
        try {
            int w = base.getWidth(), h = base.getHeight();
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            c.drawBitmap(base, 0f, 0f, null);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            p.setColorFilter(new PorterDuffColorFilter(0xFF000000 | color, PorterDuff.Mode.SRC_IN));
            c.drawBitmap(shape, null, new android.graphics.Rect(0, 0, w, h), p);
            return out;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * The glow MainActivity lays over the wallpaper in the focused app's colour. Glass draws it too,
     * so what shows through the glass matches what lies around it.
     */
    static void setGlow(Bitmap shape, int from, int to, float mix, float alpha) {
        if (shape == glowShape && from == glowFrom && to == glowTo && mix == glowMix && alpha == glowAlpha) return;
        glowShape = shape;
        glowFrom = from;
        glowTo = to;
        glowMix = mix;
        glowAlpha = alpha;
        requestTints();
        invalidateWatchers();
    }

    private static void invalidateWatchers() {
        for (int i = watchers.size() - 1; i >= 0; i--) {
            View v = watchers.get(i).get();
            if (v == null) watchers.remove(i);
            else v.invalidate();
        }
    }

    /** Asks {@code view} to redraw whenever a new blurred copy or glow is ready. */
    static void watch(View view) {
        // Popups come and go: drop the ones that have been collected before adding another.
        for (int i = watchers.size() - 1; i >= 0; i--) if (watchers.get(i).get() == null) watchers.remove(i);
        watchers.add(new WeakReference<>(view));
    }

    /** Memory is short: let go of what can be remade. The copy on the glass stays. */
    static void trim() {
        tinted.clear();
        synchronized (SPARE_LOCK) {
            spare = null;
            pixels = null;
            scratch = null;
        }
    }

    private static void keepSpare(Bitmap done) {
        if (done == null || done == blurred) return;
        synchronized (SPARE_LOCK) {
            spare = done;
        }
    }

    /**
     * Blurs {@code wallpaper}, laid out the way the wallpaper view shows it (centre-cropped to the
     * screen, then multiplied by {@code multiply} when that is not 0), on {@code worker}.
     */
    static void update(Bitmap wallpaper, int screenW, int screenH, int multiply, Executor worker, Handler main) {
        if (wallpaper == null || screenW <= 0 || screenH <= 0) return;
        worker.execute(() -> {
            Bitmap small = render(wallpaper, screenW, screenH, multiply);
            main.post(() -> present(small, 0));
        });
    }

    /**
     * The blurred copy of {@code wallpaper}, made on the caller's (background) thread. Making it
     * before the picture is shown is what lets the glass change at the same moment as the wallpaper,
     * instead of a few seconds behind it.
     */
    static Bitmap blur(Bitmap wallpaper, int screenW, int screenH, int multiply) {
        if (wallpaper == null || screenW <= 0 || screenH <= 0) return null;
        return render(wallpaper, screenW, screenH, multiply);
    }

    /**
     * Shows a new blurred copy on the glass. With {@code fadeMs}, the glass fades from the old copy
     * to the new one over that time, alongside the wallpaper's own fade. Main thread only.
     */
    static void present(Bitmap blur, long fadeMs) {
        if (blur == null) return;
        if (blurFade != null) blurFade.cancel();
        if (previous != null) keepSpare(previous);
        Bitmap before = blurred;
        previous = fadeMs > 0 ? before : null;
        blurred = blur;
        if (previous == null) keepSpare(before);
        blurMix = previous != null ? 0f : 1f;
        blurGen++;
        requestTints();
        if (previous != null) {
            blurFade = android.animation.ValueAnimator.ofFloat(0f, 1f);
            blurFade.setDuration(fadeMs);
            blurFade.addUpdateListener(a -> {
                blurMix = (float) a.getAnimatedValue();
                if (blurMix >= 1f && previous != null) {
                    keepSpare(previous);
                    previous = null;
                }
                invalidateWatchers();
            });
            blurFade.start();
        }
        invalidateWatchers();
    }

    private static Bitmap render(Bitmap src, int screenW, int screenH, int multiply) {
        int w = Math.max(1, screenW / SCALE);
        int h = Math.max(1, screenH / SCALE);
        Bitmap out = null;
        int[] px, tmp;
        synchronized (SPARE_LOCK) {
            if (spare != null && spare.getWidth() == w && spare.getHeight() == h && spare.isMutable()) {
                out = spare;
                spare = null;
            }
            if (pixels == null || pixels.length != w * h) {
                pixels = new int[w * h];
                scratch = new int[w * h];
            }
            px = pixels;
            tmp = scratch;
        }
        if (out == null) out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(0xFF000000);
        // The same centre crop as ImageView.ScaleType.CENTER_CROP, at the small size.
        float s = Math.max(screenW / (float) src.getWidth(), screenH / (float) src.getHeight());
        Matrix m = new Matrix();
        m.setScale(s / SCALE, s / SCALE);
        m.postTranslate((screenW - src.getWidth() * s) / 2f / SCALE, (screenH - src.getHeight() * s) / 2f / SCALE);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        // Exactly the wallpaper's own dimming: the glass softens what is behind it but must not
        // change its colour, or the glass reads as a different colour from its surroundings.
        if (multiply != 0) p.setColorFilter(new PorterDuffColorFilter(multiply, PorterDuff.Mode.MULTIPLY));
        canvas.drawBitmap(src, m, p);

        out.getPixels(px, 0, w, 0, 0, w, h);
        // Repeated box passes each way come close to a Gaussian, at a fraction of its cost.
        for (int pass = 0; pass < PASSES; pass++) {
            boxBlur(px, tmp, w, h, RADIUS, true);
            boxBlur(tmp, px, w, h, RADIUS, false);
        }
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    /** One box pass along rows ({@code horizontal}) or columns, with the edges held. */
    private static void boxBlur(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w;
        int len = horizontal ? w : h;
        int step = horizontal ? 1 : w;
        int div = r * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            int sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[base + clamp(i, len) * step];
                sr += (c >> 16) & 0xFF;
                sg += (c >> 8) & 0xFF;
                sb += c & 0xFF;
            }
            for (int i = 0; i < len; i++) {
                out[base + i * step] = 0xFF000000 | (sr / div) << 16 | (sg / div) << 8 | (sb / div);
                int add = in[base + clamp(i + r + 1, len) * step];
                int sub = in[base + clamp(i - r, len) * step];
                sr += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                sg += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                sb += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    private static int clamp(int i, int len) {
        return i < 0 ? 0 : i >= len ? len - 1 : i;
    }

    /**
     * A paint that fills with the blurred wallpaper lined up under one view, so a shape drawn with
     * it in that view's coordinates shows exactly what lies behind the shape. One per glass surface:
     * it keeps its shader, matrix and position array and allocates nothing per frame.
     */
    static final class Fill {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint prevPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint glowOld = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint glowNew = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Matrix matrix = new Matrix();
        private final Matrix glowMatrix = new Matrix();
        private final int[] at = new int[2];
        private Bitmap shaderOf, prevOf, glowOf, underOf, overOf;
        private final Paint under = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint over = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private int oldColorOf, newColorOf;

        /**
         * Draws what lies behind {@code rect} (in {@code owner}'s coordinates), softened: the
         * blurred wallpaper, then the focused app's glow over it. Nothing until the first blurred
         * copy is ready; the caller's veil still draws.
         */
        void draw(Canvas canvas, View owner, RectF rect, float radius, float alpha) {
            Bitmap b = blurred;
            if (b == null || alpha <= 0f) return;
            if (b != shaderOf) {
                paint.setShader(new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                shaderOf = b;
            }
            // Screen position, not window position: popups live in their own window, and the
            // blurred copy is laid out like the whole screen.
            owner.getLocationOnScreen(at);
            matrix.setScale(SCALE, SCALE);
            matrix.postTranslate(-at[0], -at[1]);
            if (drawTinted(canvas, rect, radius, alpha)) return;
            // Mid-change, the old picture's blur goes under the new one, which fades in on top.
            Bitmap p = previous;
            if (p != null && blurMix < 1f) {
                if (p != prevOf) {
                    prevPaint.setShader(new BitmapShader(p, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                    prevOf = p;
                }
                prevPaint.getShader().setLocalMatrix(matrix);
                prevPaint.setAlpha(Math.round(255 * alpha));
                canvas.drawRoundRect(rect, radius, radius, prevPaint);
            }
            paint.getShader().setLocalMatrix(matrix);
            paint.setAlpha(Math.round(255 * alpha * (p != null ? blurMix : 1f)));
            canvas.drawRoundRect(rect, radius, radius, paint);

            Bitmap g = glowShape;
            android.util.DisplayMetrics dm = owner.getResources().getDisplayMetrics();
            if (g == null || glowAlpha <= 0f || dm.widthPixels == 0) return;
            if (g != glowOf) {
                glowOld.setShader(new BitmapShader(g, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                glowNew.setShader(new BitmapShader(g, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                glowOf = g;
                oldColorOf = newColorOf = 0;
            }
            if (glowFrom != oldColorOf) {
                glowOld.setColorFilter(new PorterDuffColorFilter(0xFF000000 | glowFrom, PorterDuff.Mode.SRC_IN));
                oldColorOf = glowFrom;
            }
            if (glowTo != newColorOf) {
                glowNew.setColorFilter(new PorterDuffColorFilter(0xFF000000 | glowTo, PorterDuff.Mode.SRC_IN));
                newColorOf = glowTo;
            }
            // The glow is stretched over the whole screen, as the ambient view stretches it.
            glowMatrix.setScale(dm.widthPixels / (float) g.getWidth(), dm.heightPixels / (float) g.getHeight());
            glowMatrix.postTranslate(-at[0], -at[1]);
            // The same cross-fade as the room: the old colour fades out as the new one fades in.
            if (glowMix < 1f) {
                glowOld.getShader().setLocalMatrix(glowMatrix);
                glowOld.setAlpha(Math.round(255 * alpha * glowAlpha * (1f - glowMix)));
                canvas.drawRoundRect(rect, radius, radius, glowOld);
            }
            glowNew.getShader().setLocalMatrix(glowMatrix);
            glowNew.setAlpha(Math.round(255 * alpha * glowAlpha * glowMix));
            canvas.drawRoundRect(rect, radius, radius, glowNew);
        }

        /**
         * The glass from the coloured copies: one layer at rest, two while the colour blends from
         * one app to the next or the glow fades in or out. False when a copy is not ready yet, or
         * the wallpaper is changing, and the glass is painted layer by layer instead. Uses the
         * matrix {@link #draw} has just set.
         */
        private boolean drawTinted(Canvas canvas, RectF rect, float radius, float alpha) {
            if (previous != null && blurMix < 1f) return false;
            if (glowAlpha <= 0f) return false;
            Bitmap to = tintedFor(glowTo);
            if (to == null) return false;
            boolean blending = glowMix < 1f && glowFrom != glowTo;
            if (blending && glowAlpha < 1f) return false;
            Bitmap first;
            float second;
            if (blending) {
                first = tintedFor(glowFrom);
                if (first == null) return false;
                second = glowMix;
            } else if (glowAlpha < 1f) {
                first = blurred;
                second = glowAlpha;
            } else {
                first = null;
                second = 1f;
            }
            if (first != null) {
                if (first != underOf) {
                    under.setShader(new BitmapShader(first, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                    underOf = first;
                }
                under.getShader().setLocalMatrix(matrix);
                under.setAlpha(Math.round(255 * alpha));
                canvas.drawRoundRect(rect, radius, radius, under);
            }
            if (to != overOf) {
                over.setShader(new BitmapShader(to, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                overOf = to;
            }
            over.getShader().setLocalMatrix(matrix);
            over.setAlpha(Math.round(255 * alpha * second));
            canvas.drawRoundRect(rect, radius, radius, over);
            return true;
        }
    }
}
