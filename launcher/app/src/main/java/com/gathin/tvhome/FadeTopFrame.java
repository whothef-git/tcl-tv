package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * The frame that clips the grid under the top bar. While rows are scrolled up to it, its top edge feathers
 * them out to transparent (the wallpaper shows through) instead of cutting them off. The offscreen layer
 * this needs is only used while the fade is on, so the resting home screen costs nothing extra.
 */
public class FadeTopFrame extends FrameLayout {

    private final Paint mask = new Paint();
    private final float length;
    private float strength;   // 0 = off, 1 = full fade

    public FadeTopFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        length = 56 * getResources().getDisplayMetrics().density;
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        setWillNotDraw(false);
    }

    /** How strongly the top edge fades, 0 to 1; animate it to fade the effect in and out. */
    public void setFadeStrength(float s) {
        if (s == strength) return;
        strength = s;
        mask.setShader(new LinearGradient(0, 0, 0, length, (int) (255 * s) << 24, 0, Shader.TileMode.CLAMP));
        invalidate();
    }

    public float getFadeStrength() {
        return strength;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (strength <= 0f) {
            super.dispatchDraw(canvas);
            return;
        }
        int save = canvas.saveLayer(0, 0, getWidth(), length, null);
        super.dispatchDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), length, mask);
        canvas.restoreToCount(save);
        // Below the fade band the children draw normally, outside the layer.
        save = canvas.save();
        canvas.clipRect(0, length, getWidth(), getHeight());
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }
}
