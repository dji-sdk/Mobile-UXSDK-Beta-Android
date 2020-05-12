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
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

import dji.ux.beta.R;
import dji.ux.beta.core.util.DisplayUtil;

/**
 * Custom view to display the aircraft gimbal's heading
 */
public class GimbalYawView extends View {

    //region Properties
    public static final int MAX_LINE_WIDTH = 4;

    private static final int DEAD_ANGLE = 30;
    private static final int BLINK_ANGLE = 270;
    private static final int SHOW_ANGLE = 190;
    private static final int HIDE_ANGLE = 90;
    private static final long DURATION_BLINK = 200;

    private final RectF rect = new RectF();
    private Paint paint = null;
    private float strokeWidth;

    private int curBlinkColor;
    private int yawColor;
    private int invalidColor;
    private int blinkColor;

    private boolean beforeShow;
    private float yaw;
    private float absYaw;
    private float yawStartAngle;
    private float yawSweepAngle;
    private float invalidStartAngle;
    private float invalidSweepAngle;
    //endregion

    //region Constructor
    public GimbalYawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GimbalYawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public GimbalYawView(Context context) {
        super(context);
        init();
    }
    //endregion

    //region UI Logic
    private void init() {
        if (isInEditMode()) {
            return;
        }
        final Context context = getContext();
        strokeWidth = DisplayUtil.dipToPx(context, getContext().getResources().getDimension(R.dimen.uxsdk_line_width));
        paint = new Paint();
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        yawColor = context.getResources().getColor(R.color.uxsdk_blue_material_A400);
        invalidColor = context.getResources().getColor(R.color.uxsdk_red);
        blinkColor = context.getResources().getColor(R.color.uxsdk_red_material_900_30_percent);
    }

    /**
     * Set the yaw for the view
     *
     * @param yaw Yaw of the gimbal
     */
    public void setYaw(final float yaw) {
        if (this.yaw != yaw) {
            this.yaw = yaw;
            absYaw = (yaw >= 0) ? yaw : (0 - yaw);
            if (absYaw >= SHOW_ANGLE) {
                beforeShow = true;
            } else if (absYaw < HIDE_ANGLE) {
                beforeShow = false;
            }

            yawStartAngle = 0.0f;
            yawSweepAngle = 0.0f;

            invalidStartAngle = 0.0f;
            invalidSweepAngle = DEAD_ANGLE;

            if (this.yaw < 0) {
                yawStartAngle = this.yaw;
                yawSweepAngle = 0 - this.yaw;
            } else {
                invalidStartAngle = 0 - DEAD_ANGLE;
                yawSweepAngle = this.yaw;
            }
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        final float width;
        if (getWidth() < getHeight()) {
            width = getWidth();
        } else {
            width = getHeight();
        }
        final float halfStroke = strokeWidth / 2.0f;
        final float radius = width / 2;

        rect.set(halfStroke, halfStroke, width - halfStroke, width - halfStroke);
        canvas.save();
        canvas.translate(radius, radius);
        canvas.rotate(-90.0f);
        canvas.translate(-radius, -radius);

        if (absYaw >= BLINK_ANGLE) {
            curBlinkColor = (curBlinkColor == invalidColor) ? blinkColor : invalidColor;
            paint.setColor(curBlinkColor);
            canvas.drawArc(rect, invalidStartAngle, invalidSweepAngle, false, paint);
            postInvalidateDelayed(DURATION_BLINK);
        } else if (beforeShow) {
            curBlinkColor = invalidColor;
            paint.setColor(invalidColor);
            canvas.drawArc(rect, invalidStartAngle, invalidSweepAngle, false, paint);
        }

        paint.setColor(yawColor);
        canvas.drawArc(rect, yawStartAngle, yawSweepAngle, false, paint);

        canvas.restore();
    }
    //endregion

    //region Customizations

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
     * Set the yaw color
     *
     * @param yawColor Color integer resource
     */
    public void setYawColor(@ColorInt final int yawColor) {
        this.yawColor = yawColor;
        postInvalidate();
    }

    /**
     * Set the invalid color
     *
     * @param invalidColor Color integer resource
     */
    public void setInvalidColor(@ColorInt final int invalidColor) {
        this.invalidColor = invalidColor;
        postInvalidate();
    }

    /**
     * Set the blink color
     *
     * @param blinkColor Color integer resource
     */
    public void setBlinkColor(@ColorInt final int blinkColor) {
        this.blinkColor = blinkColor;
        postInvalidate();
    }

    //endregion
}