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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dji.ux.beta.core.R;
import dji.ux.beta.core.util.DisplayUtil;

/**
 * A square-shaped view that shows the progress of a remote controller's stick calibration.
 */
public class CalibrationSquareView extends View {
    //region constants
    private static final int DEFAULT_SEGMENT_NUMBER = 15;
    /**
     * The maximum value of a 15-segment binary segment representation.
     */
    public static final int MAX_CALIBRATION_STATUS = 32767;
    //endregion

    //region Fields
    private float textSize; //px
    private int textWidth;
    private int rectangleGap;
    private Bitmap hintCircle;
    private Bitmap scaledHintCircleBitmap;

    private int circleRadius;
    private float progressAlpha;

    @ColorInt
    private int primaryColor;
    @ColorInt
    private int rectangleColor;
    @ColorInt
    private int textColor;
    @ColorInt
    private int movementCircleColor;

    private int[] progress = new int[4];

    private int leftPercent = 0;
    private int rightPercent = 0;
    private int topPercent = 0;
    private int bottomPercent = 0;

    private int segmentNum = DEFAULT_SEGMENT_NUMBER;

    private int viewWidth;
    private int viewHeight;
    private int movementCircleX;
    private int movementCircleY;
    private int hintCircleX;
    private int hintCircleY;

    private int cornerNumber;

    private float unitStrokeLength;
    private float topMiddleStartX;
    private float topMiddleStartY;
    private float topLeftStartX;
    private float topLeftStartY;
    private float topRightStartX;
    private float topRightStartY;
    private float bottomRightStartX;
    private float bottomRightStartY;
    private float bottomLeftStartX;
    private float bottomLeftStartY;

    private RectF outsideRectangle;
    private RectF insideRectangle;

    private Paint textPaint;
    private Paint rectanglePainter;
    private Paint hintCirclePainter;
    private Paint movementCirclePainter;
    private Paint progressStrokePainter;
    //endregion

    //region Constructors
    public CalibrationSquareView(Context context) {
        this(context, null);
    }

    public CalibrationSquareView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrationSquareView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CalibrationSquareView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(outsideRectangle, rectanglePainter);
        canvas.drawRect(insideRectangle, rectanglePainter);
        canvas.drawCircle(movementCircleX, movementCircleY, circleRadius, movementCirclePainter);
        if (getOffset(1) <= cornerNumber && getOffset(0) > getSegment(0) - cornerNumber) {
            canvas.drawLine(topMiddleStartX, topMiddleStartY, topMiddleStartX + unitStrokeLength * (getSegment(0) - cornerNumber), topMiddleStartY, progressStrokePainter);
            hintCircleX = viewWidth - textWidth - circleRadius;
            hintCircleY = textWidth + circleRadius;
        } else {
            if (getOffset(0) == 8) {
                canvas.drawLine(topMiddleStartX, topMiddleStartY, viewWidth - textWidth, topMiddleStartY, progressStrokePainter);
            } else {
                canvas.drawLine(topMiddleStartX, topMiddleStartY, topMiddleStartX + unitStrokeLength * getOffset(0), topMiddleStartY, progressStrokePainter);
            }
            int tempX1 = (int) (topMiddleStartX + unitStrokeLength * getOffset(0) + circleRadius);
            int tempX2 = viewWidth - textWidth - circleRadius;
            hintCircleX = tempX1 > tempX2 ? tempX2 : tempX1;
            hintCircleY = textWidth + circleRadius;
        }
        if (isFinishedOneEdgeCalibration(0) && getOffset(1) > cornerNumber) {
            if (getOffset(2) <= cornerNumber && getOffset(1) > getSegment(1) - cornerNumber) {
                canvas.drawLine(topRightStartX, topRightStartY + rectangleGap, topRightStartX, topRightStartY + unitStrokeLength * (getSegment(1) - cornerNumber), progressStrokePainter);
                hintCircleX = viewWidth - textWidth - circleRadius;
                hintCircleY = viewHeight - textWidth - circleRadius;
            } else {
                if (getOffset(1) == 15) {
                    canvas.drawLine(topRightStartX, topRightStartY + rectangleGap, topRightStartX, viewHeight - textWidth, progressStrokePainter);
                } else {
                    canvas.drawLine(topRightStartX, topRightStartY + rectangleGap, topRightStartX, topRightStartY + unitStrokeLength * getOffset(1), progressStrokePainter);
                }
                hintCircleX = viewWidth - textWidth - circleRadius;
                int tempY1 = viewHeight - textWidth - circleRadius;
                int tempY2 = (int) (topRightStartY + unitStrokeLength * getOffset(1) + circleRadius);
                hintCircleY = tempY1 > tempY2 ? tempY2 : tempY1;
            }
        }
        if (isFinishedOneEdgeCalibration(1) && getOffset(2) > cornerNumber) {
            if (getOffset(3) <= cornerNumber && getOffset(2) > getSegment(2) - cornerNumber) {
                canvas.drawLine(bottomRightStartX - rectangleGap, bottomRightStartY, bottomRightStartX - unitStrokeLength * (getSegment(2) - cornerNumber), bottomRightStartY, progressStrokePainter);
                hintCircleX = textWidth + circleRadius;
                hintCircleY = viewHeight - textWidth - circleRadius;
            } else {
                if (getOffset(2) == 15) {
                    canvas.drawLine(bottomRightStartX - rectangleGap, bottomRightStartY, textWidth, bottomRightStartY, progressStrokePainter);
                } else {
                    canvas.drawLine(bottomRightStartX - rectangleGap, bottomRightStartY, bottomRightStartX - unitStrokeLength * getOffset(2), bottomRightStartY, progressStrokePainter);
                }
                int tempX1 = (int) (bottomRightStartX - unitStrokeLength * getOffset(2) - circleRadius);
                int tempX2 = textWidth + circleRadius;
                hintCircleX = tempX1 > tempX2 ? tempX1 : tempX2;
                hintCircleY = viewHeight - textWidth - circleRadius;
            }
        }
        if (isFinishedOneEdgeCalibration(2) && getOffset(3) > cornerNumber) {
            if (getOffset(4) <= cornerNumber && getOffset(3) > getSegment(3) - cornerNumber) {
                canvas.drawLine(bottomLeftStartX, bottomLeftStartY - rectangleGap, bottomLeftStartX, bottomLeftStartY - unitStrokeLength * (getSegment(3) - cornerNumber), progressStrokePainter);
                hintCircleX = textWidth + circleRadius;
                hintCircleY = textWidth + circleRadius;
            } else {
                if (getOffset(3) == 15) {
                    canvas.drawLine(bottomLeftStartX, bottomLeftStartY - rectangleGap, bottomLeftStartX, textWidth, progressStrokePainter);
                } else {
                    canvas.drawLine(bottomLeftStartX, bottomLeftStartY - rectangleGap, bottomLeftStartX, bottomLeftStartY - unitStrokeLength * getOffset(3), progressStrokePainter);
                }
                hintCircleX = textWidth + circleRadius;
                int tempY1 = textWidth + circleRadius;
                int tempY2 = (int) (bottomLeftStartY - unitStrokeLength * getOffset(3) - circleRadius);
                hintCircleY = tempY1 > tempY2 ? tempY1 : tempY2;
            }
        }
        if (isFinishedOneEdgeCalibration(3) && getOffset(4) > cornerNumber) {
            canvas.drawLine(topLeftStartX + rectangleGap, topLeftStartY, topLeftStartX + unitStrokeLength * getOffset(4), topLeftStartY, progressStrokePainter);
            hintCircleX = (int) (topLeftStartX + unitStrokeLength * getOffset(4) + circleRadius);
            hintCircleY = textWidth + circleRadius;
        }
        if (getOffset(0) == 0) {
            canvas.drawBitmap(scaledHintCircleBitmap, getWidth() / 2 - circleRadius, hintCircleY - circleRadius, hintCirclePainter);
        } else {
            canvas.drawBitmap(scaledHintCircleBitmap, hintCircleX - circleRadius, hintCircleY - circleRadius, hintCirclePainter);
        }
        drawText(canvas);
    }
    //endregion

    //region public methods

    /**
     * Each parameter expects an integer where each byte represents whether the corresponding
     * segment is filled. For example, 000010000000001 means that the 1st and 11th segment are
     * filled up.
     *
     * @param left   the filled segments along the left side of the square.
     * @param top    the filled segments along the top side of the square.
     * @param right  the filled segments along the right side of the square.
     * @param bottom the filled segments along the bottom side of the square.
     */
    public void setProgress(@IntRange(from = 0, to = MAX_CALIBRATION_STATUS) int left,
                            @IntRange(from = 0, to = MAX_CALIBRATION_STATUS) int top,
                            @IntRange(from = 0, to = MAX_CALIBRATION_STATUS) int right,
                            @IntRange(from = 0, to = MAX_CALIBRATION_STATUS) int bottom) {
        progress[0] = top;
        progress[1] = right;
        progress[2] = bottom;
        progress[3] = left;
        invalidate();
    }

    /**
     * Resets the progress for all segments.
     */
    public void reset() {
        for (int i = 0; i < progress.length; i++) {
            progress[i] = 0;
        }
        invalidate();
    }

    /**
     * Sets the number of segments for each side of the square.
     *
     * @param segmentNum The number of segments.
     */
    public void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    /**
     * Sets the position of the center of the circle and changes the color based on whether it has
     * reached the edge of the square or not.
     *
     * @param left   The position of the left side.
     * @param top    The position of the top side.
     * @param right  The position of the right side.
     * @param bottom The position of the bottom side.
     */
    public void setCircleCenter(@IntRange(from = 0, to = 100) int left,
                                @IntRange(from = 0, to = 100) int top,
                                @IntRange(from = 0, to = 100) int right,
                                @IntRange(from = 0, to = 100) int bottom) {
        movementCircleX = viewWidth / 2 - left * (viewWidth - 2 * textWidth - rectangleGap) / 200 + right * (viewWidth - 2 * textWidth - rectangleGap) / 200;
        movementCircleY = viewHeight / 2 - top * (viewHeight - 2 * textWidth - rectangleGap) / 200 + bottom * (viewHeight - 2 * textWidth - rectangleGap) / 200;
        leftPercent = left;
        topPercent = top;
        rightPercent = right;
        bottomPercent = bottom;
        if (bottom == 100 || top == 100 || right == 100 || left == 100) {
            switchCirclePainter(true);
        } else {
            switchCirclePainter(false);
        }
        invalidate();
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
     * Set the text color of the progress indicator text.
     *
     * @param textSizeSp The text size.
     */
    public void setTextSize(@Dimension float textSizeSp) {
        textSize = DisplayUtil.spToPx(getContext(), textSizeSp);
        textPaint.setTextSize(textSize);
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
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, rectangleGap, rectangleGap, false);
    }

    /**
     * Get the width of the track which the circle follows.
     *
     * @return the width of the track which the circle follows.
     */
    public int getRectangleGap() {
        return rectangleGap;
    }

    /**
     * Sets the width of the track which the circle follows.
     *
     * @param rectangleGap the width of the track which the circle follows.
     */
    public void setRectangleGap(int rectangleGap) {
        this.rectangleGap = rectangleGap;
        circleRadius = rectangleGap / 2;
        progressStrokePainter.setStrokeWidth(rectangleGap);
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, rectangleGap, rectangleGap, false);
    }

    /**
     * Get the color of the hint circle, progress strokes, and movement circle highlight.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Set the color of the hint circle, progress strokes, and movement circle highlight.
     *
     * @param primaryColor color integer resource.
     */
    public void setPrimaryColor(@ColorInt int primaryColor) {
        this.primaryColor = primaryColor;
        hintCirclePainter.setColor(primaryColor);
        progressStrokePainter.setColor(primaryColor);
    }

    /**
     * Get the alpha of the progress path.
     *
     * @return a float between 0 and 1 that represents the alpha of the progress path.
     */
    @FloatRange(from = 0, to = 1)
    public float getProgressAlpha() {
        return progressAlpha;
    }

    /**
     * Set the alpha of the progress path.
     *
     * @param progressAlpha a float between 0 and 1 that represents the alpha of the progress path.
     */
    public void setProgressAlpha(@FloatRange(from = 0, to = 1) float progressAlpha) {
        this.progressAlpha = progressAlpha;
        progressStrokePainter.setAlpha((int) (255 * progressAlpha));
    }

    /**
     * Get the border paint color of the track which the circle follows.
     *
     * @return color integer resource.
     */
    @ColorInt
    public int getRectangleColor() {
        return rectangleColor;
    }

    /**
     * Set the border paint color of the track which the circle follows.
     *
     * @param rectangleColor color integer resource.
     */
    public void setRectangleColor(@ColorInt int rectangleColor) {
        this.rectangleColor = rectangleColor;
        rectanglePainter.setColor(rectangleColor);
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

    /**
     * Get the color of the center circle which shows the position of the joystick.
     *
     * @return the color of the movement circle.
     */
    @ColorInt
    public int getMovementCircleColor() {
        return movementCircleColor;
    }

    /**
     * Set the color of the center circle which shows the position of the joystick.
     *
     * @param movementCircleColor the color of the movement circle.
     */
    public void setMovementCircleColor(@ColorInt int movementCircleColor) {
        this.movementCircleColor = movementCircleColor;
        setCircleCenter(leftPercent, topPercent, rightPercent, bottomPercent);
    }
    //endregion

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        viewWidth = w;
        viewHeight = h;
        movementCircleX = viewWidth / 2;
        movementCircleY = viewHeight / 2;
        hintCircleX = viewWidth / 2;
        hintCircleY = circleRadius + textWidth;

        unitStrokeLength = (viewWidth - 2 * textWidth) / segmentNum;
        cornerNumber = (int) (rectangleGap / unitStrokeLength) - 1;

        topMiddleStartX = textWidth + unitStrokeLength * 7;
        topMiddleStartY = textWidth + rectangleGap / 2;
        topLeftStartX = textWidth;
        topLeftStartY = textWidth + rectangleGap / 2;
        topRightStartX = viewWidth - textWidth - rectangleGap / 2f;
        topRightStartY = textWidth;
        bottomRightStartX = viewWidth - textWidth;
        bottomRightStartY = viewHeight - textWidth - rectangleGap / 2f;
        bottomLeftStartX = textWidth + rectangleGap / 2;
        bottomLeftStartY = viewHeight - textWidth;

        setUpRectangle(textWidth, outsideRectangle);
        setUpRectangle(textWidth + rectangleGap, insideRectangle);
    }

    //region private methods
    private void init() {
        primaryColor = getResources().getColor(R.color.uxsdk_blue_highlight);
        rectangleColor = Color.WHITE;
        textColor = Color.WHITE;
        movementCircleColor = Color.WHITE;

        textSize = DisplayUtil.spToPx(getContext(), 10);
        textWidth = (int) DisplayUtil.dipToPx(getContext(), 35);
        rectangleGap = (int) DisplayUtil.dipToPx(getContext(), 20);
        circleRadius = rectangleGap / 2;
        progressAlpha = 0.3f;

        outsideRectangle = new RectF();
        insideRectangle = new RectF();

        rectanglePainter = new Paint();
        rectanglePainter.setAntiAlias(true);
        rectanglePainter.setDither(true);
        rectanglePainter.setColor(rectangleColor);
        rectanglePainter.setStyle(Paint.Style.STROKE);
        rectanglePainter.setStrokeWidth(2);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStrokeWidth(0);
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);

        movementCirclePainter = new Paint();
        movementCirclePainter.setAntiAlias(true);
        movementCirclePainter.setDither(true);
        movementCirclePainter.setColor(movementCircleColor);
        movementCirclePainter.setStrokeWidth(10);

        hintCirclePainter = new Paint();
        hintCirclePainter.setAntiAlias(true);
        hintCirclePainter.setDither(true);
        hintCirclePainter.setColor(primaryColor);
        hintCirclePainter.setAlpha(255);
        hintCirclePainter.setStyle(Paint.Style.STROKE);
        hintCirclePainter.setStrokeWidth(10);

        progressStrokePainter = new Paint();
        progressStrokePainter.setAntiAlias(true);
        progressStrokePainter.setDither(true);
        progressStrokePainter.setColor(primaryColor);
        progressStrokePainter.setAlpha((int) (255 * progressAlpha));
        progressStrokePainter.setStyle(Paint.Style.STROKE);
        progressStrokePainter.setStrokeWidth(rectangleGap);
        hintCircle = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_ic_rc_dial_hint_circle);
        scaledHintCircleBitmap = Bitmap.createScaledBitmap(hintCircle, rectangleGap, rectangleGap, false);
    }

    private void switchCirclePainter(boolean isOnEdge) {
        if (isOnEdge) {
            movementCirclePainter = new Paint();
            movementCirclePainter.setAntiAlias(true);
            movementCirclePainter.setDither(true);
            movementCirclePainter.setColor(primaryColor);
            movementCirclePainter.setAlpha(255);
            movementCirclePainter.setStrokeWidth(5);
        } else {
            movementCirclePainter = new Paint();
            movementCirclePainter.setAntiAlias(true);
            movementCirclePainter.setDither(true);
            movementCirclePainter.setColor(movementCircleColor);
            movementCirclePainter.setStrokeWidth(5);
        }
        invalidate();
    }

    private void setUpRectangle(int offset, RectF target) {
        target.set(offset, offset, viewWidth - offset, viewHeight - offset);
    }

    private void drawText(Canvas canvas) {
        int w = getWidth();

        drawCenterText(canvas, String.valueOf(leftPercent), new Rect(0, 0, textWidth, w));

        drawCenterText(canvas, String.valueOf(rightPercent), new Rect(w - textWidth, 0, w, w));

        drawCenterText(canvas, String.valueOf(topPercent), new Rect(0, 0, w, textWidth));

        drawCenterText(canvas, String.valueOf(bottomPercent), new Rect(0, w - textWidth, w, w));
    }

    private void drawCenterText(Canvas canvas, String text, Rect rect) {
        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
        int baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        // the following line is horizontally centered，and drawText is changed to targetRect.centerX()
        canvas.drawText(text, rect.centerX(), baseline, textPaint);
    }

    private int getOffset(int index) {
        int result = 0;
        int currentProgress;
        int blockNum = getSegment(index);
        switch (index) {
            case 0:
            case 4:
                currentProgress = progress[0];
                break;
            default:
                currentProgress = progress[index];
        }
        int factor = index == 0 ? (int) Math.pow(2, blockNum - 1) : 1;
        for (int i = 0; i < blockNum; i++) {
            if ((currentProgress & factor) == factor) {
                result++;
            } else {
                break;
            }
            factor = factor * 2;
        }
        if (index == 0 || isFinishedOneEdgeCalibration(index - 1)) {
            return result;
        } else {
            return 0;
        }
    }

    private int getSegment(int index) {
        int blockNum;
        switch (index) {
            case 0:
                if (segmentNum % 2 != 0) {
                    blockNum = segmentNum / 2 + 1;
                } else {
                    blockNum = segmentNum / 2;
                }
                break;
            case 4:
                blockNum = segmentNum / 2;
                break;
            default:
                blockNum = segmentNum;
        }
        return blockNum;
    }

    private boolean isFinishedOneEdgeCalibration(int index) {
        switch (index) {
            case 0:
                if (segmentNum % 2 != 0) {
                    return getOffset(index) == segmentNum / 2 + 1;
                } else {
                    return getOffset(index) == segmentNum / 2;
                }
            case 4:
                return getOffset(index) == segmentNum / 2;
            default:
                return getOffset(index) == segmentNum;
        }
    }
    //endregion
}