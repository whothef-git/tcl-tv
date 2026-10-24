package com.gathin.tvhome;

import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * The edge of a glass shape in Apple's Liquid Glass style: light catches the top-left and the
 * bottom-right edges, and fades towards the other two corners.
 *
 * <p>The shine runs along the shape's diagonal, so for a wide pill, a circle or the dock tray alike
 * the faint point lands on the top-right and bottom-left corners. The gradient is built once in a
 * unit square and stretched onto each shape, so drawing allocates nothing.
 */
final class GlassRim {

    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();

    GlassRim(float strokePx, int shine, int faint) {
        this(strokePx, shine, faint, 1f);
    }

    /**
     * @param reach how far each shine runs towards the faint corners: 1 fades it out right at
     *              them, 0.5 only halfway. Long shapes such as the dock tray use the shorter reach,
     *              or the shine runs most of the length of their long edges.
     */
    GlassRim(float strokePx, int shine, int faint, float reach) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokePx);
        float end = 0.5f * reach;
        paint.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{shine, faint, faint, shine}, new float[]{0f, end, 1f - end, 1f},
                Shader.TileMode.CLAMP));
    }

    /** Lines the shine up with {@code rect}: brightest at its top-left and bottom-right. */
    void fit(RectF rect) {
        matrix.setScale(rect.width(), rect.height());
        matrix.postTranslate(rect.left, rect.top);
        paint.getShader().setLocalMatrix(matrix);
    }
}
