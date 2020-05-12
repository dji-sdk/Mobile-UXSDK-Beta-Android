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
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import dji.ux.beta.R;
import dji.ux.beta.core.base.GlobalPreferencesManager;
import dji.ux.beta.core.util.ViewUtil;

/**
 * Displays an icon based on the given {@link CenterPointType}.
 */
public class CenterPointView extends AppCompatImageView {

    //region Constructors
    public CenterPointView(Context context) {
        super(context);
        initView();
    }

    public CenterPointView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CenterPointView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        CenterPointType type = getType();
        if (type == CenterPointType.NONE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            setImageResource(type.getDrawableId());
            ViewUtil.tintImage(this, getColor());
        }
    }
    //endregion

    //region Customization

    /**
     * Set the type of center point
     *
     * @param type The type of center point
     */
    public void setType(@NonNull final CenterPointType type) {
        if (getType() != type && GlobalPreferencesManager.getInstance() != null) {
            GlobalPreferencesManager.getInstance().setCenterPointType(type);
            initView();
        }
    }

    /**
     * Get the type of center point
     *
     * @return The type of center point
     */
    @NonNull
    public CenterPointType getType() {
        if (GlobalPreferencesManager.getInstance() != null) {
            return GlobalPreferencesManager.getInstance().getCenterPointType();
        } else {
            return CenterPointType.NONE;
        }
    }

    /**
     * Set the color of the center point
     *
     * @param color The color of the center point
     */
    public void setColor(@ColorInt int color) {
        if (getColor() != color && GlobalPreferencesManager.getInstance() != null) {
            GlobalPreferencesManager.getInstance().setCenterPointColor(color);
            ViewUtil.tintImage(this, color);
        }
    }

    /**
     * Get the color of the center point
     *
     * @return The color of the center point
     */
    @ColorInt
    public int getColor() {
        if (GlobalPreferencesManager.getInstance() != null) {
            return GlobalPreferencesManager.getInstance().getCenterPointColor();
        } else {
            return Color.WHITE;
        }
    }

    /**
     * Sets a new drawable to display when the center point is set to the given type.
     *
     * @param type       The type of center point
     * @param drawableId The drawable that will be displayed
     */
    public void setImageForType(@NonNull CenterPointType type, @DrawableRes int drawableId) {
        type.setDrawableId(drawableId);
    }
    //endregion

    /**
     * Represents the types of center points that can be set.
     */
    public enum CenterPointType {

        /**
         * The center point is hidden.
         */
        NONE(0, -1),

        /**
         * The center point is a standard shape.
         */
        STANDARD(1, R.drawable.uxsdk_ic_centerpoint_standard),

        /**
         * The center point is a cross shape.
         */
        CROSS(2, R.drawable.uxsdk_ic_centerpoint_cross),

        /**
         * The center point is a narrow cross shape.
         */
        NARROW_CROSS(3, R.drawable.uxsdk_ic_centerpoint_narrow_cross),

        /**
         * The center point is a frame shape.
         */
        FRAME(4, R.drawable.uxsdk_ic_centerpoint_frame),

        /**
         * The center point is a frame shape with a cross inside.
         */
        FRAME_AND_CROSS(5, R.drawable.uxsdk_ic_centerpoint_frame_and_cross),

        /**
         * The center point is a square shape.
         */
        SQUARE(6, R.drawable.uxsdk_ic_centerpoint_square),

        /**
         * The center point is a square shape with a cross inside.
         */
        SQUARE_AND_CROSS(7, R.drawable.uxsdk_ic_centerpoint_square_and_cross),

        /**
         * The center point is an unknown shape.
         */
        UNKNOWN(8, -1);

        private final int value;
        @DrawableRes
        private int drawableId;

        CenterPointType(int value, @DrawableRes int drawableId) {
            this.value = value;
            this.drawableId = drawableId;
        }

        public int value() {
            return value;
        }

        @DrawableRes
        public int getDrawableId() {
            return drawableId;
        }

        private void setDrawableId(@DrawableRes int drawableId) {
            this.drawableId = drawableId;
        }

        private boolean _equals(int b) {
            return value == b;
        }

        public static CenterPointType find(int value) {
            CenterPointType result = UNKNOWN;
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
