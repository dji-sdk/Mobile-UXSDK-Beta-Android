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
 */

package dji.ux.beta.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.ux.beta.R;
import dji.ux.beta.core.base.GlobalPreferencesManager;

/**
 * Displays a grid centered in the view.
 */
public class GridLineView extends View {

    //region fields
    private static final int DISABLED = 0;
    private static final int DEFAULT_NUM_LINES = 4;
    private static final int DEFAULT_LINE_WIDTH = 1;
    private Paint paint;
    private int gridWidth;
    private int gridHeight;
    @ColorInt
    private int lineColor;
    private float lineWidth;
    private int numLines;
    //endregion

    //region Constructors
    public GridLineView(@NonNull Context context) {
        super(context);
        initView();
    }

    public GridLineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public GridLineView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        gridHeight = DISABLED;
        gridWidth = DISABLED;
        lineColor = getResources().getColor(R.color.uxsdk_white_80_percent);
        lineWidth = DEFAULT_LINE_WIDTH;
        numLines = DEFAULT_NUM_LINES;

        if (isInEditMode()) {
            return;
        }

        setWillNotDraw(false);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(lineColor);
        paint.setStrokeWidth(lineWidth);
    }
    //endregion

    //region Customization

    /**
     * Set the type of grid line
     *
     * @param type The type of grid line
     */
    public void setType(@NonNull final GridLineType type) {
        if (getType() != type && GlobalPreferencesManager.getInstance() != null) {
            GlobalPreferencesManager.getInstance().setGridLineType(type);
            invalidate();
        }
    }

    /**
     * Get the type of grid line.
     *
     * @return The type of grid line
     */
    @NonNull
    public GridLineType getType() {
        if (GlobalPreferencesManager.getInstance() != null) {
            return GlobalPreferencesManager.getInstance().getGridLineType();
        } else {
            return GridLineType.NONE;
        }
    }

    /**
     * Adjust the width and height of the grid lines. The grid will be centered within the view.
     *
     * @param width  The new width of the grid lines.
     * @param height The new height of the grid lines.
     */
    public void adjustDimensions(int width, int height) {

        if (width > 0 && height > 0) {
            gridWidth = width;
            gridHeight = height;
        } else {
            gridWidth = DISABLED;
            gridHeight = DISABLED;
        }
        invalidate();
    }

    /**
     * Get the color of the grid lines
     *
     * @return The color of the grid lines
     */
    @ColorInt
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Set the color of the grid lines
     *
     * @param lineColor The color of the grid lines
     */
    public void setLineColor(@ColorInt int lineColor) {
        this.lineColor = lineColor;
        if (!isInEditMode()) {
            paint.setColor(lineColor);
        }
        invalidate();
    }

    /**
     * Get the width of the grid lines
     *
     * @return The width of the grid lines
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Set the width of the grid lines
     *
     * @param lineWidth The width of the grid lines
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
        if (!isInEditMode()) {
            paint.setStrokeWidth(lineWidth);
        }
        invalidate();
    }

    /**
     * Get the number of lines drawn both horizontally and vertically on the screen, including the
     * two border lines.
     *
     * @return The number of lines
     */
    public int getNumberOfLines() {
        return numLines;
    }

    /**
     * Set the number of lines drawn both horizontally and vertically on the screen, including the
     * two border lines.
     *
     * @param numLines The number of lines
     */
    public void setNumberOfLines(int numLines) {
        this.numLines = numLines;
        invalidate();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onDraw(Canvas canvas) {
        if (gridHeight == DISABLED || gridWidth == DISABLED) {
            return;
        }

        float measureWidth = getMeasuredWidth();
        float measureHeight = getMeasuredHeight();

        // Offset by 1 because canvas origin is at 0
        measureHeight -= 1;
        measureWidth -= 1;

        // Calculate offset for different aspect ratios
        int widthOffset = (int) ((measureWidth - gridWidth) / 2);
        if (widthOffset < 0) {
            widthOffset = 0;
        }
        int heightOffset = (int) ((measureHeight - gridHeight) / 2);
        if (heightOffset < 0) {
            heightOffset = 0;
        }

        if (getType() != GridLineType.NONE) {
            // Draw horizontal lines
            final float horizontalOffset = (measureHeight - heightOffset - heightOffset) / (numLines - 1);
            for (float y = heightOffset; y <= measureHeight - heightOffset; y += horizontalOffset) {
                canvas.drawLine(widthOffset, y, measureWidth - widthOffset, y, paint);
            }

            // Draw vertical lines
            final float verticalOffset = (measureWidth - widthOffset - widthOffset) / (numLines - 1);
            for (float x = widthOffset; x <= measureWidth - widthOffset; x += verticalOffset) {
                canvas.drawLine(x, heightOffset, x, measureHeight - heightOffset, paint);
            }

            // Draw diagonal lines
            if (getType() == GridLineType.PARALLEL_DIAGONAL) {
                canvas.drawLine(widthOffset, heightOffset, measureWidth - widthOffset, measureHeight - heightOffset, paint);
                canvas.drawLine(widthOffset, measureHeight - heightOffset, measureWidth - widthOffset, heightOffset, paint);
            }
        }
    }
    //endregion

    /**
     * Represents the types of grid lines that can be set.
     */
    public enum GridLineType {

        /**
         * No grid lines are visible.
         */
        NONE(0),

        /**
         * Horizontal and vertical grid lines are visible using a NxN grid.
         */
        PARALLEL(1),

        /**
         * Same as PARALLEL with the addition of 2 diagonal lines running through the center.
         */
        PARALLEL_DIAGONAL(2),

        /**
         * The type of grid is unknown.
         */
        UNKNOWN(3);

        private int value;

        GridLineType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        private boolean _equals(int b) {
            return value == b;
        }

        public static GridLineType find(int value) {
            GridLineType result = UNKNOWN;
            for (int i = 0; i < values().length; i++) {
                if (values()[i]._equals(value)) {
                    result = values()[i];
                    break;
                }
            }
            return result;
        }
    }
}
