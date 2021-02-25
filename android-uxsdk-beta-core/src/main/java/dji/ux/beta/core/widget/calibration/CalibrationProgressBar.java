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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dji.ux.beta.core.R;

/**
 * A two-direction progress bar with the default position in the middle that shows the progress of
 * a remote controller's dial or lever calibration.
 */
public class CalibrationProgressBar extends View {

    //region constants

    /**
     * Horizontal orientation with the progress extending to the left and right.
     */
    public static final int HORIZONTAL = 0;
    /**
     * Vertical orientation with the progress extending to the top and bottom.
     */
    public static final int VERTICAL = 1;
    //endregion

    //region Fields
    private int lineWidth;
    private int dividerWidth;

    private int orientation;

    @ColorInt
    private int backgroundColor;
    @ColorInt
    private int primaryColor;
    @ColorInt
    private int dividerColor;

    private Paint paint;

    private int leftPercent;
    private int rightPercent;
    //endregion

    //region Constructors
    public CalibrationProgressBar(Context context) {
        this(context, null);
    }

    public CalibrationProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrationProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributeSet(context, attrs);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CalibrationProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttributeSet(context, attrs);
        init();
    }
    //endregion

    private void initAttributeSet(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            orientation = HORIZONTAL;
            return;
        }
        TypedArray ar = context.obtainStyledAttributes(attrs, R.styleable.CalibrationProgressBar);
        orientation = ar.getInt(R.styleable.CalibrationProgressBar_uxsdk_orientation, HORIZONTAL);
        ar.recycle();
    }

    private void init() {
        backgroundColor = getResources().getColor(R.color.uxsdk_gray_calibration_background);
        primaryColor = getResources().getColor(R.color.uxsdk_blue_calibration_fill);
        dividerColor = Color.WHITE;

        lineWidth = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_progress_line_width);
        dividerWidth = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_progress_divider_width);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(primaryColor);
        paint.setStrokeWidth(0);
    }

    //region data

    /**
     * Sets the fill value of the progress bars.
     *
     * @param left  The percentage filled in the left (if horizontal) or top (if vertical) progress
     *              bar
     * @param right The percentage filled in the right (if horizontal) or bottom (if vertical)
     *              progress bar
     */
    public void setValue(@IntRange(from = 0, to = 100) int left,
                         @IntRange(from = 0, to = 100) int right) {
        leftPercent = left;
        rightPercent = right;
        invalidate();
    }
    //endregion

    //region customization

    /**
     * Get the orientation of the progress bar.
     *
     * @return {@link CalibrationProgressBar#HORIZONTAL} or {@link CalibrationProgressBar#VERTICAL}.
     */
    @IntRange(from = 0, to = 1)
    public int getOrientation() {
        return orientation;
    }

    /**
     * Set the orientation of the progress bar.
     *
     * @param orientation {@link CalibrationProgressBar#HORIZONTAL} or
     *                    {@link CalibrationProgressBar#VERTICAL}.
     */
    public void setOrientation(@IntRange(from = 0, to = 1) int orientation) {
        this.orientation = orientation;
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
    }

    /**
     * Get the primary paint color. This color is used to represent the filled areas of the
     * progress bar.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Set the progress paint color. This color is used to represent the filled areas of the
     * progress bar.
     *
     * @param primaryColor color integer resource.
     */
    public void setPrimaryColor(@ColorInt int primaryColor) {
        this.primaryColor = primaryColor;
    }

    /**
     * Get the paint color of the divider in the center of the progress bar.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getDividerColor() {
        return dividerColor;
    }

    /**
     * Set the paint color of the divider in the center of the progress bar.
     *
     * @param dividerColor color integer resource.
     */
    public void setDividerColor(@ColorInt int dividerColor) {
        this.dividerColor = dividerColor;
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        drawBackground(canvas, w, h);
        drawProgress(canvas, w, h);
        drawDivider(canvas, w, h);
    }
    //endregion

    //region helpers
    private void drawBackground(Canvas canvas, int w, int h) {
        paint.setColor(backgroundColor);
        if (orientation == VERTICAL) {
            canvas.drawRect(new Rect((w - lineWidth) / 2, 0, (w - lineWidth) / 2 + lineWidth, h), paint);
        } else {
            canvas.drawRect(new Rect(0, (h - lineWidth) / 2, w, (h - lineWidth) / 2 + lineWidth), paint);
        }
    }

    private void drawProgress(Canvas canvas, int w, int h) {
        paint.setColor(primaryColor);
        if (orientation == VERTICAL) {
            canvas.drawRect(new Rect((w - lineWidth) / 2, h / 2, (w - lineWidth) / 2 + lineWidth, h / 2 + (h / 2) * leftPercent / 100), paint);
            canvas.drawRect(new Rect((w - lineWidth) / 2, (h / 2) * (100 - rightPercent) / 100, (w - lineWidth) / 2 + lineWidth, h / 2), paint);
        } else {
            canvas.drawRect(new Rect((w / 2) * (100 - leftPercent) / 100, (h - lineWidth) / 2, w / 2, (h - lineWidth) / 2 + lineWidth), paint);
            canvas.drawRect(new Rect(w / 2, (h - lineWidth) / 2, w / 2 + (w / 2) * rightPercent / 100, (h - lineWidth) / 2 + lineWidth), paint);
        }
    }

    private void drawDivider(Canvas canvas, int w, int h) {
        paint.setColor(dividerColor);
        if (orientation == VERTICAL) {
            canvas.drawRect(new Rect((w - lineWidth) / 2, (h - dividerWidth) / 2, (w - lineWidth) / 2 + lineWidth, (h - dividerWidth) / 2 + dividerWidth), paint);
        } else {
            canvas.drawRect(new Rect((w - dividerWidth) / 2, (h - lineWidth) / 2, (w - dividerWidth) / 2 + dividerWidth, (h - lineWidth) / 2 + lineWidth), paint);
        }
    }
    //endregion
}
