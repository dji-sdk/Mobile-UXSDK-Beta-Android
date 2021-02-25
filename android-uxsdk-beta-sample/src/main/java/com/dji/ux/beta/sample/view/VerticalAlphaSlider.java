/*
 * Copyright (c) 2018-2021 DJI
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

package com.dji.ux.beta.sample.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;

import com.dji.ux.beta.sample.R;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.PaintBuilder;
import com.flask.colorpicker.slider.AlphaSlider;

public class VerticalAlphaSlider extends AlphaSlider {
    public int color;
    private final Paint alphaPatternPaint = PaintBuilder.newPaint().build();
    private final Paint barPaint = PaintBuilder.newPaint().build();
    private final Paint solid = PaintBuilder.newPaint().build();
    private final Paint clearingStroke = PaintBuilder.newPaint()
            .color(getContext().getResources().getColor(R.color.white))
            .xPerMode(PorterDuff.Mode.CLEAR)
            .build();

    private Paint clearStroke = PaintBuilder.newPaint().build();
    private Bitmap clearBitmap;
    private Canvas clearBitmapCanvas;

    public VerticalAlphaSlider(Context context) {
        super(context);
    }

    public VerticalAlphaSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalAlphaSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void createBitmaps() {
        super.createBitmaps();
        alphaPatternPaint.setShader(PaintBuilder.createAlphaPatternShader(barHeight * 2));
        clearBitmap = Bitmap.createBitmap(getMeasuredHeight(), getMeasuredWidth(), Bitmap.Config.ARGB_8888);
        clearBitmapCanvas = new Canvas(clearBitmap);
    }

    @Override
    protected void drawBar(Canvas barCanvas) {
        int width = barCanvas.getWidth();
        int height = barCanvas.getHeight();

        barCanvas.drawRect(0, 0, width, height, alphaPatternPaint);
        int l = Math.max(2, width / 256);
        for (int x = 0; x <= width; x += l) {
            float alpha = (float) x / (width - 1);
            barPaint.setColor(color);
            barPaint.setAlpha(Math.round(alpha * 255));
            barCanvas.drawRect(x, 0, x + l, height, barPaint);
        }
    }

    @Override
    protected void drawHandle(Canvas canvas, float x, float y) {
        solid.setColor(color);
        solid.setAlpha(Math.round(value * 255));
        if (showBorder) canvas.drawCircle(x, y, handleRadius, clearingStroke);
        if (value < 1) {
            // this fixes the same artifact issue from ColorPickerView
            // happens when alpha pattern is drawn underneath a circle with the same size
            clearBitmapCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            clearBitmapCanvas.drawCircle(x, y, handleRadius * 0.75f + 4, alphaPatternPaint);
            clearBitmapCanvas.drawCircle(x, y, handleRadius * 0.75f + 4, solid);

            clearStroke = PaintBuilder.newPaint().color(0xffffffff).style(Paint.Style.STROKE).stroke(6).xPerMode(PorterDuff.Mode.CLEAR).build();
            clearBitmapCanvas.drawCircle(x, y, handleRadius * 0.75f + (clearStroke.getStrokeWidth() / 2), clearStroke);
            canvas.drawBitmap(clearBitmap, 0, 0, null);
        } else {
            canvas.drawCircle(x, y, handleRadius * 0.75f, solid);
        }
    }

    public void setColor(int color) {
        this.color = color;
        this.value = Utils.getAlphaPercent(color);
        if (bar != null) {
            updateBar();
            invalidate();
        }
    }
}
