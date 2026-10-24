package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

/**
 * The focus: one rounded shape that glides from whatever had focus to whatever has it now,
 * changing only its size and corners on the way. Between apps it is a pane of glass; on the top
 * menu, and travelling between the dock and the top menu, it is dark. It switches look at once,
 * never blending, and the Inputs list has a dark one of its own.
 *
 * <p>It sits behind what it marks, so it never draws over an app, a pill's label or a row's text:
 * around a tile it shows as a dark frame, and under a pill or a row as its fill. Glass that covers
 * it gets out of the way (see {@link #coverage}) or paints the shape itself (see {@link #drawShape}).
 * Moving it only redraws a round rect; it never asks for a new layout.
 */
final class Glider extends View {

    /** About 63% black: bright text reads on it, and every place that uses it matches. */
    static final int FOCUS_ALPHA = 0xA0;
    /** The top menu's look: a dark fill. */
    static final int DARK = 0;
    /** Between apps: a pane of glass, the same as the pills and the tray. */
    static final int GLASS = 1;

    private static final class Edge extends FloatPropertyCompat<Glider> {
        private final int which;

        Edge(String name, int which) {
            super(name);
            this.which = which;
        }

        @Override
        public float getValue(Glider g) {
            return g.edges[which];
        }

        @Override
        public void setValue(Glider g, float value) {
            g.edges[which] = value;
            g.changed();
        }
    }

    private static final int LEFT = 0, TOP = 1, RIGHT = 2, BOTTOM = 3, RADIUS = 4;

    /** Left, top, right, bottom (top and bottom in scroll-free space) and corner radius. */
    private final float[] edges = new float[5];
    private final SpringAnimation[] springs = new SpringAnimation[5];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Backdrop.Fill glass = new Backdrop.Fill();
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GlassRim rim;
    private final android.graphics.RectF shape = new android.graphics.RectF();
    private int style = DARK;
    private boolean paneHidden;
    /**
     * Ambient backlight glow masks and paint, drawn by the glider behind the active/moving app tile.
     * Radiates an Ambilight aura in the app's vivid signature color.
     */
    private Bitmap glowOuter;
    private Bitmap glowCore;
    private int glowW, glowH;
    private final Paint glowPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint ambientRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float outerBlur, coreBlur;
    /** How far the ambient backlight is shown: it comes in as the glider turns to glass, and goes with the dark. */
    private float shadowMix;
    private ValueAnimator shadowFade;
    private int currentAmbientColor = 0xFF00E5FF;
    private int targetAmbientColor = 0xFF00E5FF;
    private ValueAnimator colorAnimator;
    private final List<View> companions = new ArrayList<>();
    /** Per companion, the part of it the shape matters to (in its own coordinates), or null for all of it. */
    private final List<RectF> companionAreas = new ArrayList<>();
    /** Where the shape was last drawn and where it is now, shadow included, in this view's coordinates. */
    private final RectF drawn = new RectF(), dirty = new RectF(), area = new RectF();
    private final int[] gliderAt = new int[2], companionAt = new int[2];
    private final float[] target = new float[5];
    private final boolean reducedMotion;
    /** How far a scrolling list has carried the shape since its target was set. */
    private float yShift;
    private float opacity;
    private boolean shown;
    private ValueAnimator fade;
    /** Run once the shape has come to rest after a glide (see {@link #onArrive}). */
    private Runnable arrival;

    Glider(Context context, boolean reducedMotion) {
        super(context);
        this.reducedMotion = reducedMotion;
        float density = context.getResources().getDisplayMetrics().density;
        veil.setColor(0x14FFFFFF);
        rim = new GlassRim(1.5f * density, 0xD9FFFFFF, 0x1AFFFFFF);
        outerBlur = 34f * density;
        coreBlur = 15f * density;
        ambientRimPaint.setStyle(Paint.Style.STROKE);
        ambientRimPaint.setStrokeWidth(3f * density);
        Backdrop.watch(this);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        String[] names = {"left", "top", "right", "bottom", "radius"};
        for (int i = 0; i < springs.length; i++) {
            springs[i] = new SpringAnimation(this, new Edge(names[i], i));
            // Tuned for a buttery, noticeable ambient backlight glide across apps
            if (reducedMotion) {
                springs[i].setSpring(new SpringForce().setStiffness(1200f).setDampingRatio(0.95f));
                springs[i].setMinimumVisibleChange(1.0f);
            } else {
                springs[i].setSpring(new SpringForce().setStiffness(650f).setDampingRatio(0.92f));
            }
            springs[i].addEndListener((anim, canceled, value, velocity) -> {
                if (!canceled) arriveIfResting();
            });
        }
    }

    /**
     * Shows the shape at once on a rectangle, at full strength and with no fade, so it can glide on
     * from there: the Inputs menu's selection handing over to the top menu as the menu closes.
     */
    void appearAt(float l, float t, float r, float b, float radius) {
        float[] to = aim(l, t, r, b, radius);
        for (int i = 0; i < springs.length; i++) {
            springs[i].cancel();
            edges[i] = to[i];
        }
        if (fade != null) fade.cancel();
        shown = true;
        opacity = 1f;
        changed();
    }

    /** Runs {@code then} once the shape comes to rest from the glide now under way. */
    void onArrive(Runnable then) {
        arrival = then;
        // Nothing may be moving at all (already in place): settle on the next frame.
        post(this::arriveIfResting);
    }

    private void arriveIfResting() {
        if (arrival == null) return;
        for (SpringAnimation sp : springs) if (sp.isRunning()) return;
        Runnable then = arrival;
        arrival = null;
        then.run();
    }

    /** Something that paints part of the shape itself, or fades for it, and so redraws as it moves. */
    void addCompanion(View v) {
        addCompanion(v, null);
    }

    /**
     * As {@link #addCompanion(View)}, for a view where only {@code area} (in its own coordinates, kept
     * up to date by its owner) shows the shape: it redraws only when the shape is, or was, over that area.
     */
    void addCompanion(View v, RectF area) {
        companions.add(v);
        companionAreas.add(area);
    }

    void removeCompanion(View v) {
        int i = companions.indexOf(v);
        if (i < 0) return;
        companions.remove(i);
        companionAreas.remove(i);
    }

    /** The target edges, in scroll-free space, in one reused array. */
    private float[] aim(float l, float t, float r, float b, float radius) {
        target[LEFT] = l;
        target[TOP] = t - yShift;
        target[RIGHT] = r;
        target[BOTTOM] = b - yShift;
        target[RADIUS] = radius;
        return target;
    }

    /**
     * Sends the shape to the rectangle (in this view's coordinates as they are now) with the given
     * corner radius: a glide when it is showing, an appearance in place when it is not.
     */
    void moveTo(float l, float t, float r, float b, float radius) {
        float[] to = aim(l, t, r, b, radius);
        if (!shown) {
            for (int i = 0; i < springs.length; i++) {
                springs[i].cancel();
                edges[i] = to[i];
            }
            shown = true;
            fadeTo(1f, reducedMotion ? 100 : 180);
            changed();
            return;
        }
        for (int i = 0; i < springs.length; i++) springs[i].animateToFinalPosition(to[i]);
    }

    /**
     * While an app is being moved it wiggles, and the glass pane behind it would show at its corners:
     * then only the app's shadow is drawn.
     */
    void setPaneHidden(boolean hidden) {
        if (paneHidden == hidden) return;
        paneHidden = hidden;
        changed();
    }

    /** Switches look at once; there is no blend between the two. The shadow eases in with the glass. */
    void setStyle(int style) {
        if (this.style == style) return;
        this.style = style;
        if (shadowFade != null) shadowFade.cancel();
        if (style == GLASS) {
            shadowFade = ValueAnimator.ofFloat(shadowMix, 1f);
            shadowFade.setDuration(reducedMotion ? 100 : 200);
            shadowFade.addUpdateListener(a -> {
                shadowMix = (float) a.getAnimatedValue();
                changed();
            });
            shadowFade.start();
        } else {
            shadowMix = 0f;
        }
        changed();
    }

    void hide() {
        arrival = null;
        if (!shown) return;
        shown = false;
        fadeTo(0f, reducedMotion ? 100 : 160);
    }

    /** Gone at once, for when the screen is rebuilt underneath it. */
    void hideNow() {
        arrival = null;
        shown = false;
        if (fade != null) fade.cancel();
        opacity = 0f;
        changed();
    }

    /** A list scrolled by {@code dy}: the shape rides along with what it marks. */
    void scrolled(float dy) {
        yShift -= dy;
        changed();
    }

    boolean isShowing() {
        return opacity > 0f && edges[RIGHT] > edges[LEFT];
    }

    /**
     * How much of a {@code w} by {@code h} area the shape covers, 0 to 1, for an area whose origin is
     * (dx, dy) from this view's own. Fading in or out counts for less.
     */
    float coverage(float dx, float dy, float w, float h) {
        if (!isShowing() || w <= 0f || h <= 0f) return 0f;
        float ox = Math.min(edges[RIGHT] + dx, w) - Math.max(edges[LEFT] + dx, 0f);
        float oy = Math.min(edges[BOTTOM] + yShift + dy, h) - Math.max(edges[TOP] + yShift + dy, 0f);
        if (ox <= 0f || oy <= 0f) return 0f;
        return Math.min(1f, (ox * oy) / (w * h)) * opacity;
    }

    /**
     * Draws the shape on {@code canvas}, whose origin sits at (dx, dy) from this view's own.
     * {@code canvasOwner} is the view the canvas belongs to, which the glass needs to line up the
     * picture behind it.
     */
    void drawShape(Canvas canvas, float dx, float dy, View canvasOwner) {
        if (!isShowing()) return;
        float t = edges[TOP] + yShift, b = edges[BOTTOM] + yShift;
        // Never rounder than the shape allows: a pill's ends stay round as it shrinks to a circle.
        float r = Math.min(edges[RADIUS], Math.min(b - t, edges[RIGHT] - edges[LEFT]) / 2f);
        shape.set(edges[LEFT] + dx, t + dy, edges[RIGHT] + dx, b + dy);
        if (style == DARK) {
            paint.setColor((int) (FOCUS_ALPHA * opacity) << 24);
            canvas.drawRoundRect(shape, r, r, paint);
            return;
        }
        // Glass: ambient backlight glow bloom, softened glass fill, veil and glowing edge
        drawAmbientBacklight(canvas, r);
        if (paneHidden) return;
        glass.draw(canvas, canvasOwner, shape, r, opacity);
        veil.setAlpha(Math.round(0x14 * opacity));
        canvas.drawRoundRect(shape, r, r, veil);
        float half = rim.paint.getStrokeWidth() / 2f;
        shape.inset(half, half);
        rim.fit(shape);
        rim.paint.setAlpha(Math.round(255 * opacity));
        canvas.drawRoundRect(shape, r - half, r - half, rim.paint);
    }

    private void drawAmbientBacklight(Canvas canvas, float radius) {
        if (shadowMix <= 0f) return;
        int w = Math.round(shape.width()), h = Math.round(shape.height());
        if (w <= 0 || h <= 0) return;

        int padO = (int) Math.ceil(outerBlur * 2.2f);
        int padC = (int) Math.ceil(coreBlur * 2.2f);

        if (glowOuter == null || w != glowW || h != glowH) {
            glowOuter = Bitmap.createBitmap(w + 2 * padO, h + 2 * padO, Bitmap.Config.ALPHA_8);
            Paint pO = new Paint(Paint.ANTI_ALIAS_FLAG);
            pO.setColor(0xFF000000);
            pO.setMaskFilter(new android.graphics.BlurMaskFilter(outerBlur, android.graphics.BlurMaskFilter.Blur.NORMAL));
            new Canvas(glowOuter).drawRoundRect(padO, padO, padO + w, padO + h, radius, radius, pO);

            glowCore = Bitmap.createBitmap(w + 2 * padC, h + 2 * padC, Bitmap.Config.ALPHA_8);
            Paint pC = new Paint(Paint.ANTI_ALIAS_FLAG);
            pC.setColor(0xFF000000);
            pC.setMaskFilter(new android.graphics.BlurMaskFilter(coreBlur, android.graphics.BlurMaskFilter.Blur.NORMAL));
            new Canvas(glowCore).drawRoundRect(padC, padC, padC + w, padC + h, radius, radius, pC);

            glowW = w;
            glowH = h;
        }

        float glowAlpha = opacity * shadowMix;
        float motionBoost = moving() ? 1.15f : 1.0f;
        int activeColor = currentAmbientColor;

        // 1. Wide radiant outer ambient backlight (Ambilight wash across wallpaper/dock)
        glowPaint.setColor(activeColor);
        glowPaint.setAlpha(Math.min(255, Math.round(0xB5 * glowAlpha * motionBoost)));
        area.set(shape.left - padO, shape.top - padO, shape.right + padO, shape.bottom + padO);
        canvas.drawBitmap(glowOuter, null, area, glowPaint);

        // 2. High-intensity inner backlight core (concentrated edge radiance)
        glowPaint.setAlpha(Math.min(255, Math.round(0xE0 * glowAlpha * motionBoost)));
        area.set(shape.left - padC, shape.top - padC, shape.right + padC, shape.bottom + padC);
        canvas.drawBitmap(glowCore, null, area, glowPaint);

        // 3. Subtle depth contact shadow
        glowPaint.setColor(0xFF000000);
        glowPaint.setAlpha(Math.round(0x35 * glowAlpha));
        canvas.drawBitmap(glowCore, shape.left - padC, shape.top - padC + 3f, glowPaint);

        // 4. Ambient specular accent rim around shape
        ambientRimPaint.setColor(getRimColor());
        ambientRimPaint.setAlpha(Math.min(255, Math.round(240 * glowAlpha)));
        canvas.drawRoundRect(shape, radius, radius, ambientRimPaint);
    }

    private void fadeTo(float to, long ms) {
        if (fade != null) fade.cancel();
        fade = ValueAnimator.ofFloat(opacity, to);
        fade.setDuration(ms);
        fade.addUpdateListener(a -> {
            opacity = (float) a.getAnimatedValue();
            changed();
        });
        fade.start();
    }

    private boolean moving() {
        for (SpringAnimation sp : springs) if (sp.isRunning()) return true;
        return false;
    }

    /**
     * Redraws the shape, and each companion it is over or has just left: a glide among the lower
     * rows of apps leaves the tray and the top-bar pills alone.
     */
    private void changed() {
        invalidate();
        if (companions.isEmpty()) return;
        dirty.set(drawn);
        if (isShowing()) {
            float reach = outerBlur * 2.5f + 12f;
            drawn.set(edges[LEFT] - reach, edges[TOP] + yShift - reach,
                    edges[RIGHT] + reach, edges[BOTTOM] + yShift + reach);
            dirty.union(drawn);
        } else {
            drawn.setEmpty();
        }
        if (dirty.isEmpty()) return;
        getLocationInWindow(gliderAt);
        for (int i = 0; i < companions.size(); i++) {
            View v = companions.get(i);
            RectF only = companionAreas.get(i);
            v.getLocationInWindow(companionAt);
            float dx = companionAt[0] - gliderAt[0], dy = companionAt[1] - gliderAt[1];
            if (only == null || only.isEmpty()) area.set(0, 0, v.getWidth(), v.getHeight());
            else area.set(only);
            area.offset(dx, dy);
            if (RectF.intersects(area, dirty)) v.invalidate();
        }
    }

    /**
     * Takes no room of its own: in a list that sizes itself to its rows (the Inputs list), filling
     * whatever height was offered made the list stretch down the screen. Given an exact size, as in
     * Home's full-screen root, it takes that.
     */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(exactOrNothing(widthSpec), exactOrNothing(heightSpec));
    }

    private static int exactOrNothing(int spec) {
        return MeasureSpec.getMode(spec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(spec) : 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawShape(canvas, 0f, 0f, this);
    }

    void setAmbientColor(int color) {
        if (color == 0 || color == 0xFF444444) color = 0xFF00E5FF;
        color = boostForBacklight(color);
        targetAmbientColor = color;
        if (colorAnimator != null) colorAnimator.cancel();
        final int start = currentAmbientColor;
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(reducedMotion ? 120 : 250);
        colorAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            currentAmbientColor = evaluateColor(fraction, start, targetAmbientColor);
            changed();
        });
        colorAnimator.start();
    }

    int getAmbientColor() {
        return currentAmbientColor;
    }

    int getRimColor() {
        return blendColors(currentAmbientColor, 0xFFFFFFFF, 0.45f);
    }

    float getShapeLeft() { return edges[LEFT]; }
    float getShapeTop() { return edges[TOP] + yShift; }
    float getShapeRight() { return edges[RIGHT]; }
    float getShapeBottom() { return edges[BOTTOM] + yShift; }
    float getShapeRadius() { return edges[RADIUS]; }
    float getOpacity() { return opacity; }
    int getStyle() { return style; }

    private static int boostForBacklight(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] > 0.15f) {
            hsv[1] = Math.max(hsv[1], 0.85f);
        }
        hsv[2] = Math.max(hsv[2], 0.95f);
        return Color.HSVToColor(hsv);
    }

    private static int blendColors(int c1, int c2, float ratio) {
        float inv = 1f - ratio;
        int a = Math.round(Color.alpha(c1) * inv + Color.alpha(c2) * ratio);
        int r = Math.round(Color.red(c1) * inv + Color.red(c2) * ratio);
        int g = Math.round(Color.green(c1) * inv + Color.green(c2) * ratio);
        int b = Math.round(Color.blue(c1) * inv + Color.blue(c2) * ratio);
        return Color.argb(a, r, g, b);
    }

    private static int evaluateColor(float fraction, int start, int end) {
        int startA = (start >> 24) & 0xff;
        int startR = (start >> 16) & 0xff;
        int startG = (start >> 8) & 0xff;
        int startB = start & 0xff;

        int endA = (end >> 24) & 0xff;
        int endR = (end >> 16) & 0xff;
        int endG = (end >> 8) & 0xff;
        int endB = end & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24)
                | ((startR + (int) (fraction * (endR - startR))) << 16)
                | ((startG + (int) (fraction * (endG - startG))) << 8)
                | (startB + (int) (fraction * (endB - startB)));
    }
}
