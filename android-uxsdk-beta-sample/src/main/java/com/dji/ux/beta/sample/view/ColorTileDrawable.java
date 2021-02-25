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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;

import com.dji.ux.beta.sample.R;
import com.flask.colorpicker.builder.PaintBuilder;

import dji.ux.beta.core.util.DisplayUtil;

public class ColorTileDrawable extends ColorDrawable {

    private float strokeWidth;
    private final Paint strokePaint;
    private final Paint fillPaint = PaintBuilder.newPaint()
            .style(Paint.Style.FILL)
            .color(0)
            .build();
    private final Paint fillBackPaint = PaintBuilder.newPaint()
            .shader(PaintBuilder.createAlphaPatternShader(26))
            .build();

    public ColorTileDrawable(Context context, int color) {
        super(color);
        strokePaint = PaintBuilder.newPaint()
                .style(Paint.Style.STROKE)
                .stroke(strokeWidth)
                .color(context.getResources().getColor(R.color.colorAccent))
                .build();
        strokeWidth = DisplayUtil.dipToPx(context, 2);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(0);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        strokePaint.setStrokeWidth(strokeWidth);
        fillPaint.setColor(getColor());
        canvas.drawRect(0, 0, width, height, fillBackPaint);
        canvas.drawRect(0, 0, width, height, fillPaint);
        canvas.drawRect(0, 0, width, height, strokePaint);
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        invalidateSelf();
    }
}
