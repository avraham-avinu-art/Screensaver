package com.avrahamart.nightscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** A compact analog dial used by both the main screensaver and Android Dream. */
public final class AnalogClockView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int hour;
    private int minute;
    private int accentColor = Color.WHITE;
    private int faceColor = Color.BLACK;
    private String dialStyle = "classic";

    public AnalogClockView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("שעון אנלוגי");
    }

    public void setTime(int hourOfDay, int minuteOfHour) {
        hour = Math.floorMod(hourOfDay, 12);
        minute = Math.max(0, Math.min(59, minuteOfHour));
        invalidate();
    }

    public void setColors(int textColor, int backgroundColor) {
        accentColor = textColor;
        faceColor = backgroundColor;
        invalidate();
    }

    public void setDialStyle(String value) {
        if ("minimal".equals(value) || "numerals".equals(value)) {
            dialStyle = value;
        } else {
            dialStyle = "classic";
        }
        invalidate();
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float radius = Math.max(0f, Math.min(width, height) * 0.455f);
        if (radius <= 0f) return;
        float cx = width * 0.5f;
        float cy = height * 0.5f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(26, Color.red(faceColor),
                Color.green(faceColor), Color.blue(faceColor)));
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp("minimal".equals(dialStyle) ? 1f : 1.6f));
        paint.setColor(withAlpha(accentColor, "minimal".equals(dialStyle) ? 145 : 205));
        canvas.drawCircle(cx, cy, radius, paint);

        for (int tick = 0; tick < 60; tick++) {
            boolean major = tick % 5 == 0;
            if ("minimal".equals(dialStyle) && !major) continue;
            double angle = Math.toRadians(tick * 6.0 - 90.0);
            float outer = radius * 0.91f;
            float inner = radius * (major ? 0.77f : 0.85f);
            float x1 = cx + (float) Math.cos(angle) * inner;
            float y1 = cy + (float) Math.sin(angle) * inner;
            float x2 = cx + (float) Math.cos(angle) * outer;
            float y2 = cy + (float) Math.sin(angle) * outer;
            paint.setStrokeWidth(dp(major ? 1.8f : 0.8f));
            paint.setColor(withAlpha(accentColor, major ? 230 : 105));
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        if ("numerals".equals(dialStyle)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(accentColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(radius * 0.145f);
            paint.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
            for (int number = 1; number <= 12; number++) {
                double angle = Math.toRadians(number * 30.0 - 90.0);
                float textRadius = radius * 0.63f;
                float x = cx + (float) Math.cos(angle) * textRadius;
                float y = cy + (float) Math.sin(angle) * textRadius;
                Paint.FontMetrics metrics = paint.getFontMetrics();
                float baseline = y - (metrics.ascent + metrics.descent) * 0.5f;
                canvas.drawText(String.valueOf(number), x, baseline, paint);
            }
        }

        drawHand(canvas, cx, cy,
                ((hour + minute / 60f) * 30f) - 90f,
                radius * 0.48f, dp(4.0f), withAlpha(accentColor, 245));
        drawHand(canvas, cx, cy,
                (minute * 6f) - 90f,
                radius * 0.72f, dp(2.6f), withAlpha(accentColor, 255));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accentColor);
        canvas.drawCircle(cx, cy, dp(4.2f), paint);
        paint.setColor(withAlpha(accentColor, 95));
        canvas.drawCircle(cx, cy, dp(1.6f), paint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float degrees,
                          float length, float stroke, int color) {
        double angle = Math.toRadians(degrees);
        float endX = cx + (float) Math.cos(angle) * length;
        float endY = cy + (float) Math.sin(angle) * length;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(stroke);
        paint.setColor(color);
        canvas.drawLine(cx, cy, endX, endY, paint);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
