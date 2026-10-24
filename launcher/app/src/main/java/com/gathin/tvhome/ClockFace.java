package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Calendar;

/**
 * The screensaver's analog clock: a grey dial with a raised rim and a face pressed into it, light grey
 * hour and minute hands, and a yellow-orange second hand that ticks once a second with a short ease.
 *
 * <p>The dial is drawn once into a bitmap, since a blur cannot be drawn live on this TV's GPU; each
 * tick only redraws that bitmap and three lines. Sizes are in units of the 98.4-unit rim radius the
 * design was drawn at, so the clock scales to whatever size it is given.
 */
final class ClockFace extends View {

    private static final float RIM = 98.4f, FACE = 92f;
    private static final int HAND = 0xFFC1C2C4, SECOND = 0xFFDA9835, TICK = 0xFF808185;

    private final Paint hands = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Calendar now = Calendar.getInstance();
    private final ValueAnimator ease = ValueAnimator.ofFloat(0f, 1f);
    private Bitmap dial;
    /** Design units to pixels. */
    private float unit;
    /** Room around the rim for its shadow, in pixels. */
    private float pad;
    /** The second hand's angle in degrees, eased from {@link #secondFrom} to {@link #secondTo}. */
    private float secondAngle, secondFrom, secondTo;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            now.setTimeInMillis(System.currentTimeMillis());
            float target = now.get(Calendar.SECOND) * 6f;
            // Always forward: 354° to 0° is one more step, not a turn back.
            float from = secondAngle % 360f;
            if (target < from) target += 360f;
            secondFrom = from;
            secondTo = target;
            ease.start();
            postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    ClockFace(Context context) {
        super(context);
        hands.setStrokeCap(Paint.Cap.ROUND);
        hands.setStyle(Paint.Style.STROKE);
        ease.setDuration(180);
        ease.setInterpolator(new DecelerateInterpolator(2f));
        ease.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            secondAngle = secondFrom + (secondTo - secondFrom) * f;
            invalidate();
        });
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Sizes the clock so its rim is {@code rimRadius} pixels across from the centre. */
    /** The room left around the rim for its shadow, in pixels. */
    float shadowRoom() {
        return pad;
    }

    void setRimRadius(float rimRadius) {
        unit = rimRadius / RIM;
        pad = 16f * unit;
        dial = null;
        requestLayout();
    }

    void start() {
        if (running) return;
        running = true;
        now.setTimeInMillis(System.currentTimeMillis());
        secondAngle = now.get(Calendar.SECOND) * 6f;
        invalidate();
        postDelayed(tick, 1000 - System.currentTimeMillis() % 1000);
    }

    void stop() {
        running = false;
        removeCallbacks(tick);
        ease.cancel();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = Math.round(2f * (RIM * unit + pad));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (unit <= 0f) return;
        if (dial == null) dial = drawDial();
        canvas.drawBitmap(dial, 0f, 0f, bitmapPaint);

        float c = getWidth() / 2f;
        now.setTimeInMillis(System.currentTimeMillis());
        float minute = now.get(Calendar.MINUTE) + now.get(Calendar.SECOND) / 60f;
        float hour = now.get(Calendar.HOUR) + minute / 60f;
        hands.setColor(HAND);
        hand(canvas, c, hour * 30f, 50f, 0f, 5f);
        hand(canvas, c, minute * 6f, 72f, 0f, 4f);
        hands.setColor(SECOND);
        hand(canvas, c, secondAngle, 80f, 16f, 2f);
        hands.setStyle(Paint.Style.FILL);
        canvas.drawCircle(c, c, 5f * unit, hands);
        hands.setStyle(Paint.Style.STROKE);
    }

    /** A hand at {@code degrees} clockwise from twelve, {@code length} long with a {@code tail} behind. */
    private void hand(Canvas canvas, float c, float degrees, float length, float tail, float width) {
        double a = Math.toRadians(degrees);
        float sx = (float) Math.sin(a) * unit, sy = (float) -Math.cos(a) * unit;
        hands.setStrokeWidth(width * unit);
        canvas.drawLine(c - sx * tail, c - sy * tail, c + sx * length, c + sy * length, hands);
    }

    /** The rim, its shadow, the pressed-in face and the four hour marks. */
    private Bitmap drawDial() {
        int size = getWidth();
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        float c = size / 2f, rim = RIM * unit, face = FACE * unit;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // A soft shadow under the rim, a little down and to the right.
        p.setColor(0xCC141517);
        p.setMaskFilter(new BlurMaskFilter(12f * unit, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(c + 4f * unit, c + 6f * unit, rim, p);
        p.setMaskFilter(null);

        // The rim stands out: lit at the top left, shaded at the bottom right.
        p.setShader(diagonal(c, rim, 0xFF2B2C2F, 0xFF212224, 0xFF16171A));
        canvas.drawCircle(c, c, rim, p);

        // The face is pressed in: lit the other way round.
        p.setShader(diagonal(c, face, 0xFF1A1A1C, 0xFF1F2023, 0xFF252529));
        canvas.drawCircle(c, c, face, p);
        p.setShader(null);

        // The rim's edge casts a little shadow into the face from the top left.
        canvas.save();
        Path clip = new Path();
        clip.addCircle(c, c, face, Path.Direction.CW);
        canvas.clipPath(clip);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(14f * unit);
        p.setColor(0xB30E0F11);
        p.setMaskFilter(new BlurMaskFilter(10f * unit, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(c + 6f * unit, c + 6f * unit, 98f * unit, p);
        p.setMaskFilter(null);
        canvas.restore();

        // And catches the light along its bottom right edge.
        p.setStrokeWidth(1.2f * unit);
        p.setShader(diagonal(c, face, 0xFF111214, 0xFF1F1F22, 0xFF333437));
        canvas.drawCircle(c, c, face - 0.5f * unit, p);
        p.setShader(null);

        // Hour marks at twelve, three, six and nine.
        p.setColor(TICK);
        p.setStrokeWidth(2f * unit);
        p.setStrokeCap(Paint.Cap.ROUND);
        float in = 65f * unit, out = 79f * unit;
        canvas.drawLine(c, c - out, c, c - in, p);
        canvas.drawLine(c, c + in, c, c + out, p);
        canvas.drawLine(c - out, c, c - in, c, p);
        canvas.drawLine(c + in, c, c + out, c, p);
        return bmp;
    }

    /** A top-left to bottom-right gradient across a circle of radius {@code r} centred at (c, c). */
    private static Shader diagonal(float c, float r, int start, int middle, int end) {
        return new LinearGradient(c - r, c - r, c + r, c + r,
                new int[]{start, middle, end}, null, Shader.TileMode.CLAMP);
    }
}
