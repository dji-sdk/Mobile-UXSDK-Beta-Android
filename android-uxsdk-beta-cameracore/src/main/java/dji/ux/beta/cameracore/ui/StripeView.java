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

package dji.ux.beta.cameracore.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;

import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.util.DisplayUtil;

public class StripeView extends View {

    private static final float BAR_HEIGHT = 9.0f;
    private static final float BAR_WIDTH = 4.0f;
    private static final float BAR_STROKE_HEIGHT = 6.0f;
    private static final float BAR_STROKE_WIDTH = 2.0f;
    private static final float BAR_STROKE_CYCLE_RADIUS = 1.0f;

    private static final int DEFAULT_MAX_UNIT = 3;
    private static final int DEFAULT_NUM_BARS_PER_UNIT = 3;

    private Bitmap shortLineBmp = null;
    private Bitmap shortHighlightLineBmp = null;
    private Bitmap longLineBmp = null;
    private Bitmap longHighlightLineBmp = null;

    private final Paint paint = new Paint();

    private int selectedPos;
    private int maxUnit = DEFAULT_MAX_UNIT;
    private int barsPerUnit = DEFAULT_NUM_BARS_PER_UNIT;
    private int centerPos;
    private float widthScale = 1;
    private float heightScale = 1;

    @ColorInt
    private int highlightLineColor;
    @ColorInt
    private int lineColor;

    private Rect src;
    private Rect dest;

    public StripeView(Context context) {
        super(context);
        initView();
    }

    public StripeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public StripeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        highlightLineColor = getResources().getColor(R.color.uxsdk_white);
        lineColor = getResources().getColor(R.color.uxsdk_white_50_percent);
        src = new Rect(0, 0, 0, 0);
        dest = new Rect(0, 0, 0, 0);
    }

    public int getSelectedPosition() {
        return selectedPos;
    }

    /**
     * Set the selected position. This corresponds to the index of the array values which are
     * matched with each bar of the stripe view.
     *
     * @param pos The selected position.
     */
    public void setSelectedPosition(final int pos) {
        if (selectedPos != pos) {
            selectedPos = pos;
            postInvalidate();
        }
    }

    /**
     * Set the maximum unit value. The default is 3 which represents a range of [-3, +3], but it
     * can be adjusted for cameras that have a different range, such as [-5, +5].
     *
     * @param max The maximum value that can be set.
     */
    public void setMaxUnit(int max) {
        maxUnit = max;
    }

    /**
     * Get the maximum value that can be set.
     *
     * @return The maximum value.
     */
    public int getMaxUnit() {
        return maxUnit;
    }

    /**
     * Set the number of bars per unit. For example, if the array is [0, 0.3, 0.7, 1, 1.3, 1.7, 2],
     * then the number of bars would be 3 since there are three steps to get from 0 to 1.
     *
     * @param max The number of bars per unit.
     */
    public void setNumberOfBarsPerUnit(int max) {
        barsPerUnit = max;
    }

    /**
     * Get the number of bars per unit.
     *
     * @return The number of bars per unit.
     */
    public int getNumberOfBarsPerUnit() {
        return barsPerUnit;
    }

    /**
     * Get the width based on the number of bars and the width of each bar.
     *
     * @return The width of the view.
     */
    public float getDesignedWidth() {
        int totalBarsNum = getMaxUnit() * getNumberOfBarsPerUnit() * 2 + 1;
        float designedWidth = totalBarsNum * BAR_WIDTH;
        return DisplayUtil.dipToPx(getContext(), designedWidth);
    }

    /**
     * Get the height of the bars.
     *
     * @return The height of the view.
     */
    public float getDesignedHeight() {
        return DisplayUtil.dipToPx(getContext(), BAR_HEIGHT);
    }

    /**
     * Set the position of the center bar which is matched with the index of the middle value of
     * the array. For example, if the array is [-1.7, -1.3, 0, 1.3, 1.7], the zero position will be
     * 2 which is the index of the 0 value.
     *
     * @param center the position of the center bar.
     */
    public void setZeroPosition(int center) {
        centerPos = center;
    }

    /**
     * Get the color of the lines
     *
     * @return The color of the lines
     */
    @ColorInt
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Set the color of the lines
     *
     * @param lineColor The color in which to draw the lines
     */
    public void setLineColor(@ColorInt int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * Get the color of the highlighted lines
     *
     * @return The color of the highlighted lines
     */
    @ColorInt
    public int getHighlightLineColor() {
        return highlightLineColor;
    }

    /**
     * Set the color of the highlighted lines
     *
     * @param highlightLineColor The color in which to draw the highlighted lines
     */
    public void setHighlightLineColor(@ColorInt int highlightLineColor) {
        this.highlightLineColor = highlightLineColor;
    }

    private Bitmap createLine(boolean isLongLine, boolean isHighlight) {
        Bitmap b = Bitmap.createBitmap((int) DisplayUtil.dipToPx(getContext(), BAR_WIDTH),
                (int) DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                Bitmap.Config.ARGB_8888);
        b.eraseColor(Color.TRANSPARENT);

        Canvas c = new Canvas(b);
        Paint p = new Paint();
        p.setAntiAlias(true);
        if (isHighlight) {
            p.setColor(highlightLineColor);
        } else {
            p.setColor(lineColor);
        }

        p.setStyle(Paint.Style.FILL);

        if (isLongLine) {
            c.drawCircle(DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2), // cycle center coordinate X
                    DisplayUtil.dipToPx(getContext(), BAR_STROKE_CYCLE_RADIUS), // cycle center coordinate Y
                    DisplayUtil.dipToPx(getContext(), BAR_STROKE_CYCLE_RADIUS), // radius
                    p);
            c.drawRect(DisplayUtil.dipToPx(getContext(), (BAR_WIDTH - BAR_STROKE_WIDTH) / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT - BAR_STROKE_HEIGHT),
                    DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 + BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                    p);
        } else {
            c.drawRect(DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 - BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 + BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                    p);
        }

        return b;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return;
        }

        shortLineBmp = createLine(false, false);
        shortHighlightLineBmp = createLine(false, true);
        longLineBmp = createLine(true, false);
        longHighlightLineBmp = createLine(true, true);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        setWillNotDraw(false);
    }

    private Bitmap getBitmap(final int index, final Bitmap normal, final boolean isLeft) {
        Bitmap bmp = normal;

        int convertedIndex;
        if (isLeft) {
            convertedIndex = centerPos - index;
        } else {
            convertedIndex = centerPos + index;
        }

        if (selectedPos < centerPos && isLeft) {
            if (convertedIndex >= selectedPos) {
                if (normal == shortLineBmp) {
                    bmp = shortHighlightLineBmp;
                } else if (normal == longLineBmp) {
                    bmp = longHighlightLineBmp;
                }
            }
        } else if (selectedPos > centerPos && !isLeft) {
            if (convertedIndex <= selectedPos) {
                if (normal == shortLineBmp) {
                    bmp = shortHighlightLineBmp;
                } else if (normal == longLineBmp) {
                    bmp = longHighlightLineBmp;
                }
            }
        }
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        float left = 0;
        float width = longHighlightLineBmp.getWidth();
        float height = longHighlightLineBmp.getHeight();

        int barNumPerUnit = getNumberOfBarsPerUnit();

        float newWidth = width * widthScale;
        float newHeight = height * heightScale;

        int barNumOnSide = getNumberOfBarsPerUnit() * getMaxUnit();
        src.set(0, 0, (int) width, (int) height);
        // Draw left side of the middle
        for (int i = 0; i < getMaxUnit(); i++) {
            for (int j = 0; j < barNumPerUnit; j++) {
                Bitmap barImg = getBitmap((barNumOnSide - i * barNumPerUnit - j),
                        (j == 0 ? longLineBmp : shortLineBmp),
                        true);
                dest.set((int) left, 0, (int) (left + newWidth), (int) newHeight);
                canvas.drawBitmap(barImg, src, dest, paint);
                left += newWidth;
            }
        }

        // Draw middle
        dest.set((int) left, 0, (int) (left + newWidth), (int) newHeight);
        canvas.drawBitmap(longHighlightLineBmp, src, dest, paint);
        left += width;

        // Draw right side of the middle
        for (int i = 0; i < getMaxUnit(); i++) {
            for (int j = 0; j < barNumPerUnit; j++) {
                Bitmap barImg = getBitmap((i * barNumPerUnit + j + 1),
                        (j + 1 != barNumPerUnit ? shortLineBmp : longLineBmp),
                        false);
                dest.set((int) left, 0, (int) (left + newWidth), (int) newHeight);
                canvas.drawBitmap(barImg, src, dest, paint);
                left += newWidth;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);

        widthScale = width / getDesignedWidth();

        int height = MeasureSpec.getSize(heightMeasureSpec);

        heightScale = height / getDesignedHeight();

        setMeasuredDimension(width, height);
    }
}
