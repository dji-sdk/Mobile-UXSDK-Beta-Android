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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dji.ux.beta.core.R;

/**
 * A view that shows the progress of a remote controller's dial calibration. This view contains a
 * large image of a dial with a circle that rotates along the image to indicate the dial position.
 */
public class CalibrationDialView extends View {
    //region Fields
    private int circleRadius;
    private int baseX;
    private int baseY;
    private double alpha;
    private double beta;
    private double angle;

    @ColorInt
    private int primaryColor;
    private Bitmap hintCircle;
    private Bitmap scaledHintCircleBitmap;
    private Bitmap dialBitmap;
    private Bitmap scaledDialBitmap;

    private Paint bitmapPaint;
    private Paint movementCirclePainter;

    private int viewWidth;
    private int viewHeight;

    private float negativeFlag;
    private float positiveFlag;

    private float circleLocationX;
    private float circleLocationY;

    private boolean hasReachedLeftEnd = false;

    private double square5 = Math.sqrt(5);
    //endregion

    //region Constructors
    public CalibrationDialView(Context context) {
        this(context, null);
    }

    public CalibrationDialView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrationDialView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CalibrationDialView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    //endregion

    private void init() {
        circleRadius = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_dial_circle_radius);
        baseX = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_dial_baseX);
        baseY = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_dial_baseY);
        alpha = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_dial_alpha);
        beta = (int) getResources().getDimension(R.dimen.uxsdk_rc_calibration_dial_beta);
        angle = Math.PI / 3;
        primaryColor = getResources().getColor(R.color.uxsdk_blue_highlight);

        bitmapPaint = new Paint(Paint.DITHER_FLAG);
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        movementCirclePainter = new Paint();
        movementCirclePainter.setAntiAlias(true);
        movementCirclePainter.setDither(true);
        movementCirclePainter.setColor(primaryColor);
        movementCirclePainter.setAlpha(255);
        movementCirclePainter.setStrokeWidth(5);

        negativeFlag = 0;
        positiveFlag = 0;
        dialBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_ic_rc_dial_calibration);
        hintCircle = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_ic_rc_dial_hint_circle);
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, circleRadius * 2, circleRadius * 2, false);
        setProgressCircle(0);
    }

    //region data
    /**
     * Sets the position of the progress circle. -100 means the circle is on the far left, and 100
     * means the circle is on the far right. Once the progress circle has reached -100, the hint
     * circle will move to the right side.
     *
     * @param progress the position of the progress circle
     */
    public void setProgress(@IntRange(from = -100, to = 100) int progress) {
        if (progress < negativeFlag) {
            negativeFlag = progress;
        }
        if (progress > positiveFlag) {
            positiveFlag = progress;
        }
        setProgressCircle(progress * Math.PI / 3.0 / 100);
        invalidate();
    }
    //endregion

    //region customization

    /**
     * Get the radius of the moving indicator circle.
     *
     * @return The radius of the moving circle.
     */
    public int getCircleRadius() {
        return circleRadius;
    }

    /**
     * Set the radius of the moving indicator circle.
     *
     * @param circleRadius The radius of the moving circle.
     */
    public void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, circleRadius * 2, circleRadius * 2, false);
    }

    /**
     * Get the color of the moving circle.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Set the color of the moving circle.
     *
     * @param primaryColor color integer resource.
     */
    public void setPrimaryColor(@ColorInt int primaryColor) {
        this.primaryColor = primaryColor;
        movementCirclePainter.setColor(primaryColor);
    }

    /**
     * Get the bitmap resource for the hint circle image.
     *
     * @return Bitmap resource of the hint circle image.
     */
    @NonNull
    public Bitmap getHintCircle() {
        return hintCircle;
    }

    /**
     * Set the bitmap resource for the hint circle image.
     *
     * @param hintCircle Bitmap resource for the hint circle image.
     */
    public void setHintCircle(@NonNull Bitmap hintCircle) {
        this.hintCircle = hintCircle;
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, circleRadius * 2, circleRadius * 2, false);
    }

    /**
     * Get the bitmap resource for the dial image.
     *
     * @return Bitmap resource of the dial image.
     */
    @NonNull
    public Bitmap getDialBitmap() {
        return dialBitmap;
    }

    /**
     * Set the bitmap resource for the dial image.
     *
     * @param dialBitmap Bitmap resource for the dial image.
     */
    public void setDialBitmap(@NonNull Bitmap dialBitmap) {
        this.dialBitmap = dialBitmap;
        if (viewHeight > 0 && viewWidth > 0) {
            scaledDialBitmap = Bitmap.createScaledBitmap(dialBitmap, viewWidth, viewHeight, false);
        }
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(scaledDialBitmap, 0, 0, bitmapPaint);
        float hintCircleX;
        float hintCircleY;
        if (negativeFlag != -100 && !hasReachedLeftEnd) {
            hintCircleX = (float) (baseX + alpha * square5 * Math.sin(-angle) + beta * Math.sin(-angle));
            hintCircleY = (float) (baseY + alpha * square5 * (1 - Math.cos(-angle)));
        } else {
            hasReachedLeftEnd = true;
            hintCircleX = (float) (baseX + alpha * square5 * Math.sin(angle) + beta * Math.sin(angle));
            hintCircleY = (float) (baseY + alpha * square5 * (1 - Math.cos(angle)));
        }
        canvas.drawBitmap(scaledHintCircleBitmap, hintCircleX, hintCircleY, bitmapPaint);

        canvas.drawCircle(circleLocationX + circleRadius, circleLocationY + circleRadius, circleRadius / 2, movementCirclePainter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (viewHeight != h || viewWidth != w) {
            viewWidth = w;
            viewHeight = h;
            if (viewHeight > 0 && viewWidth > 0) {
                scaledDialBitmap = Bitmap.createScaledBitmap(dialBitmap, viewWidth, viewHeight, false);
            }
        }
    }
    //endregion

    //region private methods
    private void setProgressCircle(double progress) {
        float x = (float) (baseX + alpha * square5 * Math.sin(progress) + beta * Math.sin(progress));
        float y = (float) (baseY + alpha * square5 * (1 - Math.cos(progress)));
        circleLocationX = x;
        circleLocationY = y;
    }
    //endregion
}
