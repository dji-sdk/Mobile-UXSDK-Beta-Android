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

package dji.ux.beta.cameracore.base.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.ui.WheelView;
import dji.ux.beta.cameracore.ui.WheelView.OnWheelItemSelectedListener;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.util.DisplayUtil;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * This is a base class for widget with {@link WheelView}
 * The widget provides a title and a wheel for variable values.
 *
 */
public abstract class WheelWidget extends ConstraintLayoutWidget implements OnWheelItemSelectedListener {

    //region Fields
    protected TextView titleTextView;
    private View cameraSettingBackground;
    protected TextView valueTextView;
    protected WheelView wheelView;

    @ColorInt
    private int cameraSettingBackgroundColor;
    //endregion

    public WheelWidget(@NonNull Context context) {
        super(context);
    }

    public WheelWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WheelWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_wheel_setting, this);
        valueTextView = findViewById(R.id.widget_value);
        titleTextView = findViewById(R.id.widget_header);
        cameraSettingBackground = findViewById(R.id.view_camera_setting_background);
        wheelView = findViewById(R.id.widget_wheel);
        wheelView.setCenterTextSize(wheelView.getNormalTextSize());
        wheelView.setOnWheelItemSelectedListener(this);
        cameraSettingBackgroundColor = getResources().getColor(R.color.uxsdk_black_30_percent);
        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }


    protected void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WheelWidget);
        int textAppearance = typedArray.getResourceId(R.styleable.WheelWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(textAppearance);
        }
        Drawable background = typedArray.getDrawable(R.styleable.WheelWidget_uxsdk_widgetTitleBackground);
        if (background != null) {
            setTitleBackground(background);
        }
        ColorStateList colorStateList = typedArray.getColorStateList(R.styleable.WheelWidget_uxsdk_widgetTitleTextColor);
        if (colorStateList != null) {
            setTitleTextColor(colorStateList);
        }
        int color = typedArray.getColor(R.styleable.WheelWidget_uxsdk_widgetTitleTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setTitleTextColor(color);
        }
        float textSize = typedArray.getDimension(R.styleable.WheelWidget_uxsdk_widgetTitleTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setTitleTextSize(DisplayUtil.pxToSp(context, textSize));
        }
        int cameraSettingBackgroundColor = typedArray.getColor(R.styleable.WheelWidget_uxsdk_cameraSettingBackgroundColor, INVALID_COLOR);
        if (cameraSettingBackgroundColor != INVALID_COLOR) {
            setCameraSettingBackgroundColor(cameraSettingBackgroundColor);
        }
        int resourceId = typedArray.getResourceId(R.styleable.WheelWidget_uxsdk_wheelStyle, INVALID_RESOURCE);
        if (resourceId != INVALID_RESOURCE) {
            wheelView.setStyle(resourceId);
        }
        typedArray.recycle();
    }

    public void enableWidget(boolean enabled) {
        wheelView.setVisibility(enabled ? VISIBLE : GONE);
        valueTextView.setVisibility(enabled ? GONE : VISIBLE);
        wheelView.setEnabled(enabled);
    }

    protected abstract void initDefaults();

    /**
     * Set background to title text
     *
     * @param resourceId to be used
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     *
     * @param drawable to be used
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    /**
     * Get current background of title text
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Set text color state list to the widget title
     *
     * @param colorStateList to be used
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        titleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the color of title text
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        titleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of widget title
     *
     * @return ColorStateList used
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return titleTextView.getTextColors();
    }

    /**
     * Get the current color of title text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTitleTextColor() {
        return titleTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance to be used
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        titleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the size of the title text
     *
     * @param textSize float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        titleTextView.setTextSize(textSize);
    }

    /**
     * Get the size of title text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTitleTextSize() {
        return titleTextView.getTextSize();
    }

    /**
     * Set the background color that is shown behind everything except the title
     *
     * @param color color integer resource
     */
    public void setCameraSettingBackgroundColor(@ColorInt int color) {
        cameraSettingBackgroundColor = color;
        cameraSettingBackground.setBackgroundColor(color);
    }

    /**
     * Get the background color that is shown behind everything except the title
     *
     * @return color integer resource
     */
    @ColorInt
    public int getCameraSettingBackgroundColor() {
        return cameraSettingBackgroundColor;
    }

    /**
     * Get the instance of the WheelView
     *
     * @return {@link WheelView}
     */
    @NonNull
    public WheelView getWheelView() {
        return wheelView;
    }

}

