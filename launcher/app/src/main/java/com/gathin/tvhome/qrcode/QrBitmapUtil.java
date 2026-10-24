package com.gathin.tvhome.qrcode;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class QrBitmapUtil {

    public static Bitmap createQrBitmap(String content, int sizePx) {
        try {
            QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
            int border = 2; // quiet zone
            int modules = qr.size + border * 2;
            int scale = Math.max(1, sizePx / modules);
            int actualSize = modules * scale;

            Bitmap bitmap = Bitmap.createBitmap(actualSize, actualSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);

            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                        int left = (x + border) * scale;
                        int top = (y + border) * scale;
                        canvas.drawRect(left, top, left + scale, top + scale, paint);
                    }
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
