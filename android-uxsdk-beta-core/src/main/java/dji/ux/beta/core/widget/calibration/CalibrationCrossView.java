/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.core.widget.calibration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dji.ux.beta.core.R;
import dji.ux.beta.core.util.DisplayUtil;

/**
 * Displays a cross-shaped calibration view for a remote controller's joystick.
 */
public class CalibrationCrossView extends View {
    //region Fields
    private int smallCircleRadius;
    private int largeCircleRadius;
    private int linePadding;
    private int lineWidth;

    private float textSize; //px
    private int textWidth;
    private int progressTextPadding;

    @ColorInt
    private int backgroundColor;
    @ColorInt
    private int primaryColor;
    @ColorInt
    private int textColor;

    private Paint fillPaint;
    private Paint largePaint;
    private Paint backgroundPaint;
    private Paint textPaint;

    private int leftPercent;
    private int topPercent;
    private int rightPercent;
    private int bottomPercent;
    //endregion

    //region Constructors
    public CalibrationCrossView(Context context) {
        this(context, null);
    }

    public CalibrationCrossView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrationCrossView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CalibrationCrossView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    //endregion

    private void init() {
        backgroundColor = getResources().getColor(R.color.uxsdk_gray_calibration_background);
        primaryColor = getResources().getColor(R.color.uxsdk_blue_calibration_fill);
        textColor = Color.WHITE;

        smallCircleRadius = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_small_circle_radius);
        largeCircleRadius = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_large_circle_radius);
        linePadding = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_line_padding);
        lineWidth = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_line_width);
        textWidth = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_text_width);
        textSize = getResources().getDimension(R.dimen.uxsdk_rc_calibration_text_size);
        progressTextPadding = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_progress_text_padding);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        fillPaint.setColor(primaryColor);
        fillPaint.setStrokeWidth(0);

        largePaint = new Paint();
        largePaint.setStyle(Paint.Style.STROKE);
        largePaint.setAntiAlias(true);
        largePaint.setColor(primaryColor);
        largePaint.setStrokeWidth(1);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStrokeWidth(0);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStrokeWidth(0);
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    //region data

    /**
     * Sets the fill percentages of each side of the view.
     *
     * @param left   The fill percentage of the left side.
     * @param top    The fill percentage of the top side.
     * @param right  The fill percentage of the right side.
     * @param bottom The fill percentage of the bottom side.
     */
    public void setValue(@IntRange(from = 0, to = 100) int left,
                         @IntRange(from = 0, to = 100) int top,
                         @IntRange(from = 0, to = 100) int right,
                         @IntRange(from = 0, to = 100) int bottom) {
        leftPercent = left;
        topPercent = top;
        rightPercent = right;
        bottomPercent = bottom;
        invalidate();
    }
    //endregion

    //region Constructors

    /**
     * Get the radius of the filled center circle.
     *
     * @return The radius of the filled center circle.
     */
    public int getSmallCircleRadius() {
        return smallCircleRadius;
    }

    /**
     * Set the radius of the filled center circle.
     *
     * @param smallCircleRadius The radius of the filled center circle.
     */
    public void setSmallCircleRadius(int smallCircleRadius) {
        this.smallCircleRadius = smallCircleRadius;
    }

    /**
     * Get the radius of the center circle outline.
     *
     * @return The radius of the center circle outline.
     */
    public int getLargeCircleRadius() {
        return largeCircleRadius;
    }

    /**
     * Set the radius of the center circle outline.
     *
     * @param largeCircleRadius The radius of the center circle outline.
     */
    public void setLargeCircleRadius(int largeCircleRadius) {
        this.largeCircleRadius = largeCircleRadius;
    }

    /**
     * Get the width of the progress lines.
     *
     * @return The width of the progress lines.
     */
    public int getLineWidth() {
        return lineWidth;
    }

    /**
     * Set the width of the progress lines.
     *
     * @param lineWidth The width of the progress lines.
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Get the text size of the progress indicator text.
     *
     * @return the text size.
     */
    @Dimension
    public float getTextSize() {
        return DisplayUtil.pxToSp(getContext(), textSize);
    }

    /**
     * Set the text size of the progress indicator text.
     *
     * @param textSizeSp The text size.
     */
    public void setTextSize(@Dimension float textSizeSp) {
        textSize = DisplayUtil.spToPx(getContext(), textSizeSp);
        textPaint.setTextSize(textSize);
    }

    /**
     * Get the progress background paint color. This color is used to represent the unfilled areas
     * of the progress bar.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getProgressBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Set the progress background paint color. This color is used to represent the unfilled areas
     * of the progress bar.
     *
     * @param backgroundColor color integer resource.
     */
    public void setProgressBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        backgroundPaint.setColor(backgroundColor);
    }

    /**
     * Get the color of the circles and filled progress lines.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Set the color of the circles and filled progress lines.
     *
     * @param primaryColor color integer resource.
     */
    public void setPrimaryColor(@ColorInt int primaryColor) {
        this.primaryColor = primaryColor;
        fillPaint.setColor(primaryColor);
    }

    /**
     * Get the text color of the progress indicator text.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getTextColor() {
        return textColor;
    }

    /**
     * Set the text color of the progress indicator text.
     *
     * @param textColor color integer resource.
     */
    public void setTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
        textPaint.setColor(textColor);
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawCircle(canvas);

        canvas.save();
        canvas.translate(textWidth, textWidth);

        drawLines(canvas);

        canvas.restore();

        drawText(canvas);
    }
    //endregion

    //region private methods
    private void drawCircle(Canvas canvas) {
        int width = getWidth();

        canvas.drawCircle(width / 2, width / 2, smallCircleRadius, fillPaint);
        canvas.drawCircle(width / 2, width / 2, largeCircleRadius, largePaint);
    }

    private void drawLines(Canvas canvas) {
        int width = getWidth() - textWidth * 2;
        int lineLen = width / 2 - largeCircleRadius - linePadding * 2;

        //draw background lines
        drawLeft(canvas, width, lineLen, 100, backgroundPaint);
        drawRight(canvas, width, lineLen, 100, backgroundPaint);
        drawTop(canvas, width, lineLen, 100, backgroundPaint);
        drawBottom(canvas, width, lineLen, 100, backgroundPaint);

        //draw filled lines
        drawLeft(canvas, width, lineLen, leftPercent, fillPaint);
        drawRight(canvas, width, lineLen, rightPercent, fillPaint);
        drawTop(canvas, width, lineLen, topPercent, fillPaint);
        drawBottom(canvas, width, lineLen, bottomPercent, fillPaint);

    }

    private void drawLeft(Canvas canvas, int width, int len, int percent, Paint paint) {
        int extra = len * (100 - percent) / 100;
        canvas.drawRect(new Rect(linePadding + extra, (width - lineWidth) / 2,
                linePadding + len, (width - lineWidth) / 2 + lineWidth), paint);
    }

    private void drawRight(Canvas canvas, int width, int len, int percent, Paint paint) {
        int extra = len * (100 - percent) / 100;
        canvas.drawRect(new Rect(width - linePadding - len, (width - lineWidth) / 2,
                width - linePadding - extra, (width - lineWidth) / 2 + lineWidth), paint);
    }

    private void drawTop(Canvas canvas, int width, int len, int percent, Paint paint) {
        int extra = len * (100 - percent) / 100;
        canvas.drawRect(new Rect((width - lineWidth) / 2, linePadding + extra,
                (width - lineWidth) / 2 + lineWidth, linePadding + len), paint);
    }

    private void drawBottom(Canvas canvas, int width, int len, int percent, Paint paint) {
        int extra = len * (100 - percent) / 100;
        canvas.drawRect(new Rect((width - lineWidth) / 2, width - linePadding - len,
                (width - lineWidth) / 2 + lineWidth, width - linePadding - extra), paint);
    }

    private void drawText(Canvas canvas) {
        int w = getWidth();

        drawCenterText(canvas, getResources().getString(R.string.uxsdk_battery_percent, leftPercent),
                new Rect(progressTextPadding, 0, progressTextPadding + textWidth, w));
        drawCenterText(canvas, getResources().getString(R.string.uxsdk_battery_percent, rightPercent),
                new Rect(w - textWidth - progressTextPadding, 0, w - progressTextPadding, w));
        drawCenterText(canvas, getResources().getString(R.string.uxsdk_battery_percent, topPercent),
                new Rect(0, progressTextPadding, w, textWidth + progressTextPadding));
        drawCenterText(canvas, getResources().getString(R.string.uxsdk_battery_percent, bottomPercent),
                new Rect(0, w - textWidth - progressTextPadding, w, w - progressTextPadding));
    }

    private void drawCenterText(Canvas canvas, String text, Rect rect) {
        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
        int baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        // the following line is horizontally centered，and drawText is changed to targetRect.centerX()
        canvas.drawText(text, rect.centerX(), baseline, textPaint);
    }
    //endregion
}
