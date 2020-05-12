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

package dji.ux.beta.core.widget.compass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;

import dji.ux.beta.R;
import dji.ux.beta.core.util.DisplayUtil;

/**
 * Custom view to display the compass view for the aircraft
 */
public class VisualCompassView extends View {

    //region Properties
    public static final int MAX_LINE_WIDTH = 4;
    private static final int DEFAULT_INTERVAL = 100;
    private static final int DEFAULT_DISTANCE = 400;
    private static final int DEFAULT_NUMBER_OF_LINES = 4;

    private final Paint paint = new Paint();

    private float distance;
    private int interval;
    private int lines;
    private int color;
    private float strokeWidth;
    //endregion

    //region Constructor
    public VisualCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    //endregion

    //region UI Logic
    private void init() {
        if (isInEditMode()) {
            return;
        }
        interval = DEFAULT_INTERVAL;
        distance = DEFAULT_DISTANCE;
        lines = DEFAULT_NUMBER_OF_LINES;
        color = getContext().getResources().getColor(R.color.uxsdk_white_47_percent);
        paint.setAntiAlias(true);
        paint.setColor(getContext().getResources().getColor(R.color.uxsdk_white_20_percent));
        paint.setStyle(Paint.Style.STROKE);
        strokeWidth =
                DisplayUtil.dipToPx(getContext(), getContext().getResources().getDimension(R.dimen.uxsdk_line_width));
        if (strokeWidth > MAX_LINE_WIDTH) {
            strokeWidth = MAX_LINE_WIDTH;
        }
        paint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawObstacleInfo(canvas);
    }

    private float findMod(final float distance) {
        final float radarRadius = interval * lines;
        float result = 0.0f;
        if (distance > radarRadius) {
            result = (float) (Math.log(distance / (interval * lines)) / Math.log(2));
            result = result - ((int) result);
        }
        return result;
    }

    private int getVirtualColor(final float mod) {
        int color = this.color;
        int alpha = Color.alpha(color);
        alpha = (int) (alpha * (1.0f - mod));
        color = Color.argb(alpha, Color.red(this.color), Color.green(this.color), Color.blue(this.color));
        return color;
    }

    private void drawDistance(final Canvas canvas) {
        float mod = findMod(distance);

        final int width = getWidth() - 2;
        final float radius = width * 0.5f;
        final float center = radius + 1;
        float unitRadius = radius / (4.0f * (mod + 1.0f));

        final int vColor = getVirtualColor(mod);
        float dRadius;

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        canvas.drawCircle(center, center, radius, paint);

        for (int i = 1; i * unitRadius < radius; i++) {
            if ((i % 2) == 0) {
                paint.setColor(color);
            } else {
                paint.setColor(vColor);
            }
            dRadius = i * unitRadius;
            canvas.drawCircle(center, center, dRadius, paint);
        }
    }

    private void drawObstacleInfo(final Canvas canvas) {
        drawDistance(canvas);
    }

    protected void setDistance(final float distance) {
        if (this.distance != distance) {
            this.distance = distance;
            postInvalidate();
        }
    }

    /**
     * Set the stroke width for the lines
     *
     * @param strokeWidth Float value of stroke width in px
     */
    public void setStrokeWidth(@FloatRange(from = 1.0, to = MAX_LINE_WIDTH) final float strokeWidth) {
        this.strokeWidth = strokeWidth;
        postInvalidate();
    }

    /**
     * Set the color for the lines
     *
     * @param color Color integer resource
     */
    public void setLineColor(@ColorInt final int color) {
        this.color = color;
        postInvalidate();
    }

    /**
     * Set the interval between the lines
     *
     * @param interval Integer value of the interval
     */
    public void setLineInterval(@IntRange(from = 1) final int interval) {
        this.interval = interval;
        postInvalidate();
    }

    /**
     * Set the number of lines to be drawn
     *
     * @param lines Number of lines as an integer value
     */
    public void setNumberOfLines(@IntRange(from = 3) final int lines) {
        this.lines = lines;
        postInvalidate();
    }

    //endregion
}

