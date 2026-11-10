package com.avrahamart.nightscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Battery indicator shown only while external power is connected. */
public final class BatteryChargeIndicatorView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private String mode = "off";
    private int level;
    private boolean charging;
    private int color = Color.WHITE;

    public BatteryChargeIndicatorView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setContentDescription("תצוגת סוללה בטעינה");
    }

    public void setBatteryState(String displayMode, int batteryLevel,
                                boolean isCharging, int textColor) {
        mode = displayMode == null ? "off" : displayMode;
        level = Math.max(0, Math.min(100, batteryLevel));
        charging = isCharging;
        color = textColor;
        boolean visible = charging && !"off".equals(mode)
                && ("percent".equals(mode) || "icon".equals(mode)
                || "icon_percent".equals(mode));
        setVisibility(visible ? VISIBLE : GONE);
        requestLayout();
        invalidate();
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = "percent".equals(mode) ? 58f
                : "icon".equals(mode) ? 34f : 92f;
        int measuredWidth = resolveSize(Math.round(dp(width)), widthMeasureSpec);
        int measuredHeight = resolveSize(Math.round(dp(28f)), heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!charging || "off".equals(mode)) return;
        float centerY = getHeight() * 0.5f;

        if ("percent".equals(mode)) {
            drawPercent(canvas, getWidth() * 0.5f, centerY);
        } else if ("icon".equals(mode)) {
            drawBattery(canvas, (getWidth() - dp(25f)) * 0.5f,
                    centerY - dp(7f), dp(25f), dp(14f));
        } else if ("icon_percent".equals(mode)) {
            float iconWidth = dp(25f);
            float gap = dp(6f);
            float textWidth = dp(48f);
            float groupWidth = iconWidth + gap + textWidth;
            float left = (getWidth() - groupWidth) * 0.5f;
            drawBattery(canvas, left, centerY - dp(7f), iconWidth, dp(14f));
            drawPercent(canvas, left + iconWidth + gap + textWidth * 0.5f,
                    centerY);
        }
    }

    private void drawPercent(Canvas canvas, float x, float baselineCenterY) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(14f));
        paint.setTypeface(android.graphics.Typeface.create(
                "sans-serif-medium", android.graphics.Typeface.NORMAL));
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = baselineCenterY - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(level + "%", x, baseline, paint);
    }

    private void drawBattery(Canvas canvas, float x, float y,
                             float width, float height) {
        float capWidth = dp(2.5f);
        float bodyWidth = width - capWidth;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.3f));
        paint.setColor(color);
        canvas.drawRoundRect(x, y, x + bodyWidth, y + height,
                dp(2f), dp(2f), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x + bodyWidth, y + height * 0.32f,
                x + width, y + height * 0.68f,
                dp(0.8f), dp(0.8f), paint);

        float innerPadding = dp(2f);
        float innerLeft = x + innerPadding;
        float innerTop = y + innerPadding;
        float innerWidth = Math.max(0f, bodyWidth - innerPadding * 2f);
        float innerHeight = Math.max(0f, height - innerPadding * 2f);
        paint.setColor(color);
        canvas.drawRoundRect(innerLeft, innerTop,
                innerLeft + innerWidth * level / 100f,
                innerTop + innerHeight, dp(1f), dp(1f), paint);

        if (charging) {
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(dp(0.8f));
            float cx = x + bodyWidth * 0.52f;
            float cy = y + height * 0.5f;
            android.graphics.Path bolt = new android.graphics.Path();
            bolt.moveTo(cx + dp(1.4f), cy - dp(5f));
            bolt.lineTo(cx - dp(2.0f), cy + dp(0.2f));
            bolt.lineTo(cx + dp(0.4f), cy + dp(0.2f));
            bolt.lineTo(cx - dp(1.0f), cy + dp(5f));
            bolt.lineTo(cx + dp(3.0f), cy - dp(0.8f));
            bolt.lineTo(cx + dp(0.5f), cy - dp(0.8f));
            bolt.close();
            canvas.drawPath(bolt, paint);
        }
    }
}
