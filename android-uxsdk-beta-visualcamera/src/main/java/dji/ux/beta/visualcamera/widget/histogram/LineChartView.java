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

package dji.ux.beta.visualcamera.widget.histogram;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.ux.beta.visualcamera.R;

/**
 * A view that graphs a set of histogram values onto a chart.
 */
@SuppressWarnings("FieldCanBeLocal")
public class LineChartView extends View {

    //region Fields
    private Path path = new Path();
    private Paint paint = new Paint();

    private float[] data = null;
    private boolean enableCubic = true;
    private float cubicIntensity = 0.2f;
    private boolean drawGrid = true;

    @ColorInt
    private int backgroundColor = 0;
    @ColorInt
    private int fillColor = 0;
    @ColorInt
    private int lineColor = 0;
    @ColorInt
    private int gridColor = 0;
    private int gridWidth = 2;

    private List<CPoint> points = new ArrayList<>();
    private float xInterval = 0;
    private float yInterval = 0;
    private float yOffset = 0;

    /**
     * The maximum value of a single data point.
     */
    public static final float MAX_VALUE = 256;

    /**
     * The number of data points.
     */
    public static final int NUM_DATA_POINTS = 58;
    //endregion

    //region Lifecycle
    public LineChartView(Context context) {
        super(context);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        if (isInEditMode()) {
            return;
        }
        super.onFinishInflate();
        Resources res = getResources();
        backgroundColor = res.getColor(R.color.uxsdk_black_47_percent);
        fillColor = res.getColor(R.color.uxsdk_white_75_percent);
        lineColor = res.getColor(R.color.uxsdk_background);
        gridColor = res.getColor(R.color.uxsdk_white_40_percent);

        setWillNotDraw(false);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(backgroundColor);

        if (drawGrid) {
            drawGrid(canvas);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(lineColor);
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isInEditMode()) {
            return;
        }
        if (w != oldw) {
            calculateInterval(w, h);
            transformDataToPoints(w, h);
            resetPath(h);
        }
    }
    //endregion

    //region data

    /**
     * Set the histogram data points of the chart. The size of the array should be
     * {@link LineChartView#NUM_DATA_POINTS} and the maximum value of each data point should be
     * {@link LineChartView#MAX_VALUE} for best results.
     *
     * @param datas The histogram data points
     */
    public void setData(float[] datas) {
        this.data = Arrays.copyOf(datas, datas.length);
        if (xInterval != 0) {
            int w = getWidth();
            int h = getHeight();
            transformDataToPoints(w, h);
            resetPath(h);
            postInvalidate();
        }
    }
    //endregion

    //region customization

    /**
     * Set the color of the border around the filled area of the chart.
     *
     * @param lineColor Color integer resource
     */
    public void setLineColor(@ColorInt int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * Get the color of the border around the filled area of the chart.
     *
     * @return Color integer resource
     */
    @ColorInt
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Set the color of the filled area of the chart.
     *
     * @param fillColor Color integer resource
     */
    public void setFillColor(@ColorInt int fillColor) {
        this.fillColor = fillColor;
    }

    /**
     * Get the color of the filled area of the chart.
     *
     * @return Color integer resource
     */
    @ColorInt
    public int getFillColor() {
        return fillColor;
    }

    /**
     * Set the color of the grid behind the chart.
     *
     * @param gridColor Color integer resource
     */
    public void setGridColor(@ColorInt int gridColor) {
        this.gridColor = gridColor;
    }

    /**
     * Get the color of the grid behind the chart.
     *
     * @return Color integer resource
     */
    @ColorInt
    public int getGridColor() {
        return gridColor;
    }

    /**
     * Set the chart's background color.
     *
     * @param backgroundColor Color integer resource
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Get the chart's background color.
     *
     * @return Color integer resource
     */
    @ColorInt
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Set whether the grid should be drawn behind the chart.
     *
     * @param drawGrid `true` if the grid should be drawn, `false` if the grid should be hidden.
     */
    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    /**
     * Get whether the grid should be drawn behind the chart.
     *
     * @return `true` if the grid should be drawn, `false` if the grid should be hidden.
     */
    public boolean getDrawGrid() {
        return drawGrid;
    }

    /**
     * Set whether the line should be drawn in a cubic bezier curve.
     *
     * @param drawCubic `true` if the line should be drawn in a cubic bezier curve, `false` if the
     *                  line should be drawn linearly.
     */
    public void setDrawCubic(boolean drawCubic) {
        this.enableCubic = drawCubic;
    }

    /**
     * Get whether the line should be drawn in a cubic bezier curve.
     *
     * @return `true` if the line should be drawn in a cubic bezier curve, `false` if the line
     * should be drawn linearly.
     */
    public boolean getDrawCubic() {
        return enableCubic;
    }
    //endregion

    //region private methods

    /**
     * Draw the border and the vertical lines
     */
    private void drawGrid(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();

        paint.setStrokeWidth(gridWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(gridColor);

        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(gridColor);
        float horizontalOffset = (w - 6 * gridWidth) / 5;

        float x = Math.round(gridWidth * 0.5f) + horizontalOffset + gridWidth;
        for (int i = 0; i < 4; i++, x += horizontalOffset + gridWidth) {
            canvas.drawLine(x, 1, x, h, paint);
        }
    }

    private void resetPath(int h) {
        if (enableCubic) {
            resetCubicPath(h);
        } else {
            resetLinearPath(h);
        }
    }

    /**
     * Maps the values of the data to pixels on the screen.
     */
    private void calculateInterval(int w, int h) {
        yOffset = 20;
        xInterval = w / (NUM_DATA_POINTS - 1);
        yInterval = (h - yOffset) / MAX_VALUE;
    }

    private void transformDataToPoints(int w, int h) {
        points.clear();
        if (data != null && data.length > 1) {
            points.add(new CPoint(0, h - data[0] * yInterval));
            int length = data.length;
            for (int j = 1; j < length - 1; j++) {
                points.add(new CPoint(j * xInterval, h - data[j] * yInterval));
            }
            points.add(new CPoint(w, h - data[length - 1] * yInterval));
        }
    }

    private void resetLinearPath(int h) {
        path.reset();
        if (!points.isEmpty()) {
            path.moveTo(points.get(0).x, points.get(0).y);

            int size = points.size();
            for (int i = 1; i < size; i++) {
                CPoint p = points.get(i);
                path.lineTo(p.x, p.y);
            }
            path.lineTo(points.get(size - 1).x, h);
            path.lineTo(0, h);
        }

        path.close();
    }

    private void resetCubicPath(int h) {
        path.reset();
        if (!points.isEmpty()) {
            int size = points.size();
            for (int i = 0; i < size; i++) {
                CPoint point = points.get(i);

                if (i == 0) {
                    CPoint next = points.get(i + 1);
                    next.dx = ((next.x - point.x) * cubicIntensity);
                    next.dy = ((next.y - point.y) * cubicIntensity);
                } else if (i == size - 1) {
                    CPoint prev = points.get(i - 1);
                    point.dx = ((point.x - prev.x) * cubicIntensity);
                    point.dy = ((point.y - prev.y) * cubicIntensity);
                } else {
                    CPoint next = points.get(i + 1);
                    CPoint prev = points.get(i - 1);
                    point.dx = ((next.x - prev.x) * cubicIntensity);
                    point.dy = ((next.y - prev.y) * cubicIntensity);
                }

                if (i == 0) {
                    path.moveTo(point.x, point.y);
                } else {
                    CPoint prev = points.get(i - 1);
                    path.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx,
                            point.y - point.dy, point.x, point.y);
                }
            }
            path.lineTo(points.get(size - 1).x, h);
            path.lineTo(0, h);
        }
        path.close();
    }
    //endregion

    //region private classes
    private static class CPoint {
        private float x;
        private float y;
        private float dx = 0;
        private float dy = 0;

        private CPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
    //endregion
}
