package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Draws the glass tray behind the first (dock) row and adds a gap below it: the blurred wallpaper
 * behind it, an even veil and a light rim, with no brighter top.
 */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trayShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GlassRim edge;
    private Glider glider;
    private final int[] at = new int[2], gliderAt = new int[2];
    private final Backdrop.Fill blur = new Backdrop.Fill();

    private final RectF rect = new RectF();
    /** The tray as last drawn, in the grid's coordinates: where the focus shape shows on it. */
    final RectF area = new RectF();
    private final RectF overShape = new RectF();
    private final Paint topRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    DockDecoration(int dockSize, float density) {
        this.dockSize = dockSize;
        this.density = density;
        // Same glass as the top-bar pills (GlassDrawable): an even veil and a shining edge.
        band.setColor(0x12FFFFFF);
        trayShadowPaint.setColor(0x38000000);
        trayShadowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(14f * density, android.graphics.BlurMaskFilter.Blur.NORMAL));
        edge = new GlassRim(1.5f * density, 0xD9FFFFFF, 0x1AFFFFFF, 0.5f);
        topRimPaint.setStyle(Paint.Style.STROKE);
        topRimPaint.setStrokeWidth(2.5f * density);
    }

    /** The focus shape: the tray's glass would hide it, so the tray paints it over the glass itself. */
    void setGlider(Glider glider) {
        this.glider = glider;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (12 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            View card = child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;
        float pad = 14 * density;
        rect.inset(-pad, -pad);
        area.set(rect);
        float radius = 26 * density;
        // Soft drop shadow under the glass dock tray
        RectF trayShadow = new RectF(rect);
        trayShadow.offset(0, 4f * density);
        c.drawRoundRect(trayShadow, radius, radius, trayShadowPaint);

        blur.draw(c, parent, rect, radius, 1f);
        c.drawRoundRect(rect, radius, radius, band);
        // The focus frame of a dock tile sits on the tray's glass and under the tiles; the rest of
        // the shape, outside the tray, is the glider itself showing behind.
        if (glider != null && glider.isShowing()) {
            parent.getLocationInWindow(at);
            glider.getLocationInWindow(gliderAt);
            c.save();
            RectF clipArea = new RectF(rect);
            float shadowBleed = 70f * density;
            clipArea.inset(-shadowBleed, -shadowBleed);
            c.clipRect(clipArea);
            glider.drawShape(c, gliderAt[0] - at[0], gliderAt[1] - at[1], parent);
            c.restore();
        }
        // Drawn just inside the tray, as the pills draw theirs: a stroke centred on the boundary
        // spreads over an extra pixel and reads thicker than the pills' edge.
        float half = edge.paint.getStrokeWidth() / 2f;
        rect.inset(half, half);
        edge.fit(rect);
        c.drawRoundRect(rect, radius - half, radius - half, edge.paint);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (glider != null && glider.isShowing() && glider.getStyle() == Glider.GLASS) {
            parent.getLocationInWindow(at);
            glider.getLocationInWindow(gliderAt);
            float dx = gliderAt[0] - at[0];
            float dy = gliderAt[1] - at[1];

            float l = glider.getShapeLeft() + dx;
            float t = glider.getShapeTop() + dy;
            float r = glider.getShapeRight() + dx;
            float b = glider.getShapeBottom() + dy;
            float radius = glider.getShapeRadius();

            overShape.set(l, t, r, b);
            topRimPaint.setColor(glider.getRimColor());
            topRimPaint.setAlpha(Math.round(230 * glider.getOpacity()));
            c.drawRoundRect(overShape, radius, radius, topRimPaint);
        }
    }
}
