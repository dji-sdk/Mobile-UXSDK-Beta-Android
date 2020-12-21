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

package dji.ux.beta.visualcamera.widget.cameraconfig.ev;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.common.camera.SettingsDefinitions;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.visualcamera.R;

/**
 * Shows the camera's current exposure compensation.
 */
public class CameraConfigEVWidget extends ConstraintLayoutWidget {

    //region Fields
    private CameraConfigEVWidgetModel widgetModel;
    private TextView evTitleTextView;
    private TextView evValueTextView;
    //endregion

    //region Constructor
    public CameraConfigEVWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigEVWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigEVWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_camera_info, this);
        evTitleTextView = findViewById(R.id.textview_title);
        evValueTextView = findViewById(R.id.textview_value);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigEVWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            evTitleTextView.setText(getResources().getString(R.string.uxsdk_ev_title));
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region LifeCycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getExposureCompensation()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
        addReaction(widgetModel.getExposureSensitivityMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateVisibility));
    }
    //endregion

    //region Reactions to model
    private void updateUI(@NonNull SettingsDefinitions.ExposureCompensation exposureCompensation) {
        evValueTextView.setText(CameraUtil.exposureValueDisplayName(exposureCompensation));
    }

    private void updateVisibility(@NonNull SettingsDefinitions.ExposureSensitivityMode exposureSensitivityMode) {
        if (exposureSensitivityMode == SettingsDefinitions.ExposureSensitivityMode.EI) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    //endregion

    //region Customizations
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_base_camera_info_ratio);
    }

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return {@link SettingDefinitions.CameraIndex}
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex {@link SettingDefinitions.CameraIndex}
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the current type of the lens the widget is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return widgetModel.getLensType();
    }

    /**
     * Set the type of the lens for which the widget should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        if (!isInEditMode()) {
            widgetModel.setLensType(lensType);
        }
    }

    /**
     * Set text appearance of the exposure compensation value title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setEVTitleTextAppearance(@StyleRes int textAppearance) {
        evTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the exposure compensation value title text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getEVTitleTextColors() {
        return evTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the exposure compensation value title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getEVTitleTextColor() {
        return evTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the exposure compensation value title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setEVTitleTextColor(@NonNull ColorStateList colorStateList) {
        evTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the exposure compensation value title text view
     *
     * @param color color integer resource
     */
    public void setEVTitleTextColor(@ColorInt int color) {
        evTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the exposure compensation value title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getEVTitleTextSize() {
        return evTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the exposure compensation value title text view
     *
     * @param textSize text size float value
     */
    public void setEVTitleTextSize(@Dimension float textSize) {
        evTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the exposure compensation value title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getEVTitleTextBackground() {
        return evTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the exposure compensation title text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setEVTitleTextBackground(@DrawableRes int resourceId) {
        evTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the exposure compensation value title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setEVTitleTextBackground(@Nullable Drawable drawable) {
        evTitleTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the exposure compensation value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setEVValueTextAppearance(@StyleRes int textAppearance) {
        evValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the exposure compensation value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getEVValueTextColors() {
        return evValueTextView.getTextColors();
    }

    /**
     * Get current text color of the exposure compensation value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getEVValueTextColor() {
        return evValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the exposure compensation value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setEVValueTextColor(@NonNull ColorStateList colorStateList) {
        evValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the exposure compensation value text view
     *
     * @param color color integer resource
     */
    public void setEVValueTextColor(@ColorInt int color) {
        evValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the exposure compensation value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getEVValueTextSize() {
        return evValueTextView.getTextSize();
    }

    /**
     * Set the text size of the exposure compensation value text view
     *
     * @param textSize text size float value
     */
    public void setEVValueTextSize(@Dimension float textSize) {
        evValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the exposure compensation value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getEVValueTextBackground() {
        return evValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the exposure compensation value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setEVValueTextBackground(@DrawableRes int resourceId) {
        evValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the exposure compensation value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setEVValueTextBackground(@Nullable Drawable drawable) {
        evValueTextView.setBackground(drawable);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigEVWidget);

        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.CameraConfigEVWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.CameraConfigEVWidget_uxsdk_lensType, 0)));

        int evTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigEVWidget_uxsdk_evTitleTextAppearance, INVALID_RESOURCE);
        if (evTitleTextAppearanceId != INVALID_RESOURCE) {
            setEVTitleTextAppearance(evTitleTextAppearanceId);
        }

        float evTitleTextSize =
                typedArray.getDimension(R.styleable.CameraConfigEVWidget_uxsdk_evTitleTextSize, INVALID_RESOURCE);
        if (evTitleTextSize != INVALID_RESOURCE) {
            setEVTitleTextSize(DisplayUtil.pxToSp(context, evTitleTextSize));
        }

        int evTitleTextColor = typedArray.getColor(R.styleable.CameraConfigEVWidget_uxsdk_evTitleTextColor, INVALID_COLOR);
        if (evTitleTextColor != INVALID_COLOR) {
            setEVTitleTextColor(evTitleTextColor);
        }

        Drawable evTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigEVWidget_uxsdk_evTitleBackgroundDrawable);
        if (evTitleTextBackgroundDrawable != null) {
            setEVTitleTextBackground(evTitleTextBackgroundDrawable);
        }

        int evValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigEVWidget_uxsdk_evValueTextAppearance, INVALID_RESOURCE);
        if (evValueTextAppearanceId != INVALID_RESOURCE) {
            setEVValueTextAppearance(evValueTextAppearanceId);
        }

        float evValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigEVWidget_uxsdk_evValueTextSize, INVALID_RESOURCE);
        if (evValueTextSize != INVALID_RESOURCE) {
            setEVValueTextSize(DisplayUtil.pxToSp(context, evValueTextSize));
        }

        int evValueTextColor = typedArray.getColor(R.styleable.CameraConfigEVWidget_uxsdk_evValueTextColor, INVALID_COLOR);
        if (evValueTextColor != INVALID_COLOR) {
            setEVValueTextColor(evValueTextColor);
        }

        Drawable evValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigEVWidget_uxsdk_evValueBackgroundDrawable);
        if (evValueTextBackgroundDrawable != null) {
            setEVValueTextBackground(evValueTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
