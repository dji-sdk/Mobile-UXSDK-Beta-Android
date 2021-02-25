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

package dji.ux.beta.cameracore.widget.cameraexposuremodesetting;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.common.camera.SettingsDefinitions;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * This widget displays a range of all exposure modes supported by the current camera, selects
 * the current exposure mode, and allows the user to change the exposure mode by tapping it.
 */
public class CameraExposureModeSettingWidget extends ConstraintLayoutWidget {

    //region Fields
    private static final String TAG = "ExpoModeSettingWidget";
    private View programBackgroundView;
    private TextView programTextView;
    private ImageView programImageView;
    private View shutterBackgroundView;
    private TextView shutterTextView;
    private View apertureBackgroundView;
    private TextView apertureTextView;
    private View manualBackgroundView;
    private TextView manualTextView;
    private CameraExposureModeSettingWidgetModel widgetModel;
    //endregion

    //region Lifecycle
    public CameraExposureModeSettingWidget(@NonNull Context context) {
        super(context);
    }

    public CameraExposureModeSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraExposureModeSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_exposure_mode_setting, this);
        setBackgroundResource(R.drawable.uxsdk_camera_exposure_mode_setting_background);
        programBackgroundView = findViewById(R.id.view_camera_mode_p_background);
        programTextView = findViewById(R.id.textview_camera_mode_p);
        programImageView = findViewById(R.id.imageview_camera_mode_p);
        shutterBackgroundView = findViewById(R.id.view_camera_mode_s_background);
        shutterTextView = findViewById(R.id.textview_camera_mode_s);
        apertureBackgroundView = findViewById(R.id.view_camera_mode_a_background);
        apertureTextView = findViewById(R.id.textview_camera_mode_a);
        manualBackgroundView = findViewById(R.id.view_camera_mode_m_background);
        manualTextView = findViewById(R.id.textview_camera_mode_m);
        if (!isInEditMode()) {
            widgetModel =
                    new CameraExposureModeSettingWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }
        if (attrs != null) {
            initAttributes(context, attrs);
        }

        programBackgroundView.setOnClickListener(view -> onExposureModeClick(SettingsDefinitions.ExposureMode.PROGRAM));
        shutterTextView.setOnClickListener(view -> onExposureModeClick(SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY));
        apertureTextView.setOnClickListener(view -> onExposureModeClick(SettingsDefinitions.ExposureMode.APERTURE_PRIORITY));
        manualTextView.setOnClickListener(view -> onExposureModeClick(SettingsDefinitions.ExposureMode.MANUAL));

        programBackgroundView.setSelected(true);
        programTextView.setSelected(true);
        programImageView.setSelected(true);
    }

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
        addReaction(widgetModel.getExposureMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateExposureMode));
        addReaction(widgetModel.getExposureModeRange()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateExposureModeRange));
    }
    //endregion

    //region helpers
    private boolean rangeContains(@NonNull SettingsDefinitions.ExposureMode[] range,
                                  @NonNull SettingsDefinitions.ExposureMode value) {
        for (SettingsDefinitions.ExposureMode item : range) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void updateExposureModeRange(@NonNull SettingsDefinitions.ExposureMode[] range) {
        programBackgroundView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.PROGRAM));
        programTextView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.PROGRAM));
        programImageView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.PROGRAM));
        apertureBackgroundView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.APERTURE_PRIORITY));
        apertureTextView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.APERTURE_PRIORITY));
        shutterBackgroundView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY));
        shutterTextView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY));
        manualBackgroundView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.MANUAL));
        manualTextView.setEnabled(rangeContains(range, SettingsDefinitions.ExposureMode.MANUAL));
    }

    private void updateExposureMode(@NonNull SettingsDefinitions.ExposureMode mode) {
        programBackgroundView.setSelected(false);
        programTextView.setSelected(false);
        programImageView.setSelected(false);
        shutterBackgroundView.setSelected(false);
        shutterTextView.setSelected(false);
        apertureBackgroundView.setSelected(false);
        apertureTextView.setSelected(false);
        manualBackgroundView.setSelected(false);
        manualTextView.setSelected(false);

        if (mode == SettingsDefinitions.ExposureMode.PROGRAM) {
            if (!programTextView.isSelected()) {
                programBackgroundView.setSelected(true);
                programTextView.setSelected(true);
                programImageView.setSelected(true);
            }
        } else if (mode == SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY) {
            if (!shutterTextView.isSelected()) {
                shutterBackgroundView.setSelected(true);
                shutterTextView.setSelected(true);
            }
        } else if (mode == SettingsDefinitions.ExposureMode.APERTURE_PRIORITY) {
            if (!apertureTextView.isSelected()) {
                apertureBackgroundView.setSelected(true);
                apertureTextView.setSelected(true);
            }
        } else if (mode == SettingsDefinitions.ExposureMode.MANUAL) {
            if (!manualTextView.isSelected()) {
                manualBackgroundView.setSelected(true);
                manualTextView.setSelected(true);
            }
        }
    }

    private void onExposureModeClick(@NonNull SettingsDefinitions.ExposureMode exposureMode) {
        addDisposable(widgetModel.setExposureMode(exposureMode).subscribe(
                () -> {
                }, logErrorConsumer(TAG, "Set exposure mode")
        ));
    }
    //endregion

    //region customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_camera_exposure_mode_setting_ratio);
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
     * Set text appearance of the program text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setProgramTextAppearance(@StyleRes int textAppearanceResId) {
        programTextView.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the program text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getProgramTextColor() {
        return programTextView.getCurrentTextColor();
    }

    /**
     * Set text color for the program text view
     *
     * @param color color integer resource
     */
    public void setProgramTextColor(@ColorInt int color) {
        programTextView.setTextColor(color);
    }

    /**
     * Set text color state list for the program text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setProgramTextColor(@NonNull ColorStateList colorStateList) {
        programTextView.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the program text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getProgramTextColors() {
        return programTextView.getTextColors();
    }

    /**
     * Get current background of the program text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getProgramTextBackground() {
        return programTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the program text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setProgramTextBackground(@DrawableRes int resourceId) {
        programTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the program text view
     *
     * @param background Drawable resource for the background
     */
    public void setProgramTextBackground(@Nullable Drawable background) {
        programTextView.setBackground(background);
    }

    /**
     * Get current text size of the program text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getProgramTextSize() {
        return programTextView.getTextSize();
    }

    /**
     * Set the text size of the program text view
     *
     * @param textSize text size float value
     */
    public void setProgramTextSize(@Dimension float textSize) {
        programTextView.setTextSize(textSize);
    }

    /**
     * Get the drawable resource for the program icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getProgramIcon() {
        return programImageView.getDrawable();
    }

    /**
     * Set the resource ID for the program icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setProgramIcon(@DrawableRes int resourceId) {
        setProgramIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the program icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setProgramIcon(@Nullable Drawable icon) {
        programImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the program icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getProgramIconBackground() {
        return programImageView.getBackground();
    }

    /**
     * Set the resource ID for the program icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setProgramIconBackground(@DrawableRes int resourceId) {
        programImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the program icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setProgramIconBackground(@Nullable Drawable icon) {
        programImageView.setBackground(icon);
    }

    /**
     * Get current background of the program view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getProgramBackground() {
        return programBackgroundView.getBackground();
    }

    /**
     * Set the resource ID for the background of the program view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setProgramBackground(@DrawableRes int resourceId) {
        programBackgroundView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the program view
     *
     * @param background Drawable resource for the background
     */
    public void setProgramBackground(@Nullable Drawable background) {
        programBackgroundView.setBackground(background);
    }

    /**
     * Set text appearance of the shutter text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setShutterTextAppearance(@StyleRes int textAppearanceResId) {
        shutterTextView.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the shutter text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getShutterTextColor() {
        return shutterTextView.getCurrentTextColor();
    }

    /**
     * Set text color for the shutter text view
     *
     * @param color color integer resource
     */
    public void setShutterTextColor(@ColorInt int color) {
        shutterTextView.setTextColor(color);
    }

    /**
     * Set text color state list for the shutter text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setShutterTextColor(@NonNull ColorStateList colorStateList) {
        shutterTextView.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the shutter text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getShutterTextColors() {
        return shutterTextView.getTextColors();
    }

    /**
     * Get current text size of the shutter text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getShutterTextSize() {
        return shutterTextView.getTextSize();
    }

    /**
     * Set the text size of the shutter text view
     *
     * @param textSize text size float value
     */
    public void setShutterTextSize(@Dimension float textSize) {
        shutterTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the shutter view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getShutterBackground() {
        return shutterBackgroundView.getBackground();
    }

    /**
     * Set the resource ID for the background of the shutter view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setShutterBackground(@DrawableRes int resourceId) {
        shutterBackgroundView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the shutter view
     *
     * @param background Drawable resource for the background
     */
    public void setShutterBackground(@Nullable Drawable background) {
        shutterBackgroundView.setBackground(background);
    }

    /**
     * Set text appearance of the aperture text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setApertureTextAppearance(@StyleRes int textAppearanceResId) {
        apertureTextView.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the aperture text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getApertureTextColor() {
        return apertureTextView.getCurrentTextColor();
    }

    /**
     * Set text color for the aperture text view
     *
     * @param color color integer resource
     */
    public void setApertureTextColor(@ColorInt int color) {
        apertureTextView.setTextColor(color);
    }

    /**
     * Set text color state list for the aperture text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setApertureTextColor(@NonNull ColorStateList colorStateList) {
        apertureTextView.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the aperture text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getApertureTextColors() {
        return apertureTextView.getTextColors();
    }

    /**
     * Get current text size of the aperture text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getApertureTextSize() {
        return apertureTextView.getTextSize();
    }

    /**
     * Set the text size of the aperture text view
     *
     * @param textSize text size float value
     */
    public void setApertureTextSize(@Dimension float textSize) {
        apertureTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the aperture view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getApertureBackground() {
        return apertureBackgroundView.getBackground();
    }

    /**
     * Set the resource ID for the background of the aperture view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setApertureBackground(@DrawableRes int resourceId) {
        apertureBackgroundView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the aperture view
     *
     * @param background Drawable resource for the background
     */
    public void setApertureBackground(@Nullable Drawable background) {
        apertureBackgroundView.setBackground(background);
    }

    /**
     * Set text appearance of the manual text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setManualTextAppearance(@StyleRes int textAppearanceResId) {
        manualTextView.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the manual text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getManualTextColor() {
        return manualTextView.getCurrentTextColor();
    }

    /**
     * Set text color for the manual text view
     *
     * @param color color integer resource
     */
    public void setManualTextColor(@ColorInt int color) {
        manualTextView.setTextColor(color);
    }

    /**
     * Set text color state list for the manual text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setManualTextColor(@NonNull ColorStateList colorStateList) {
        manualTextView.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the manual text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getManualTextColors() {
        return manualTextView.getTextColors();
    }

    /**
     * Get current text size of the manual text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getManualTextSize() {
        return manualTextView.getTextSize();
    }

    /**
     * Set the text size of the manual text view
     *
     * @param textSize text size float value
     */
    public void setManualTextSize(@Dimension float textSize) {
        manualTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the manual view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getManualBackground() {
        return manualBackgroundView.getBackground();
    }

    /**
     * Set the resource ID for the background of the manual view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setManualBackground(@DrawableRes int resourceId) {
        manualBackgroundView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the manual view
     *
     * @param background Drawable resource for the background
     */
    public void setManualBackground(@Nullable Drawable background) {
        manualBackgroundView.setBackground(background);
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraExposureModeSettingWidget);
        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.CameraExposureModeSettingWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.CameraExposureModeSettingWidget_uxsdk_lensType, 0)));

        int programTextAppearanceId = typedArray.getResourceId(R.styleable.CameraExposureModeSettingWidget_uxsdk_programTextAppearance, INVALID_RESOURCE);
        if (programTextAppearanceId != INVALID_RESOURCE) {
            setProgramTextAppearance(programTextAppearanceId);
        }

        float programTextSize = typedArray.getDimension(R.styleable.CameraExposureModeSettingWidget_uxsdk_programTextSize, INVALID_RESOURCE);
        if (programTextSize != INVALID_RESOURCE) {
            setProgramTextSize(DisplayUtil.pxToSp(context, programTextSize));
        }

        @ColorInt int programTextColor = typedArray.getColor(R.styleable.CameraExposureModeSettingWidget_uxsdk_programTextColor, INVALID_COLOR);
        if (programTextColor != INVALID_COLOR) {
            setProgramTextColor(programTextColor);
        }
        ColorStateList programTextColorStateList = typedArray.getColorStateList(R.styleable.CameraExposureModeSettingWidget_uxsdk_programTextColor);
        if (programTextColorStateList != null) {
            setProgramTextColor(programTextColorStateList);
        }

        Drawable programTextBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_programTextBackground);
        if (programTextBackground != null) {
            setProgramTextBackground(programTextBackground);
        }

        Drawable programIcon = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_programIcon);
        if (programIcon != null) {
            setProgramIcon(programIcon);
        }

        Drawable programIconBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_programIconBackground);
        if (programIconBackground != null) {
            setProgramIconBackground(programIconBackground);
        }

        Drawable programBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_programBackground);
        if (programBackground != null) {
            setProgramBackground(programBackground);
        }

        int shutterTextAppearanceId = typedArray.getResourceId(R.styleable.CameraExposureModeSettingWidget_uxsdk_shutterTextAppearance, INVALID_RESOURCE);
        if (shutterTextAppearanceId != INVALID_RESOURCE) {
            setShutterTextAppearance(shutterTextAppearanceId);
        }

        float shutterTextSize = typedArray.getDimension(R.styleable.CameraExposureModeSettingWidget_uxsdk_shutterTextSize, INVALID_RESOURCE);
        if (shutterTextSize != INVALID_RESOURCE) {
            setShutterTextSize(DisplayUtil.pxToSp(context, shutterTextSize));
        }

        @ColorInt int shutterTextColor = typedArray.getColor(R.styleable.CameraExposureModeSettingWidget_uxsdk_shutterTextColor, INVALID_COLOR);
        if (shutterTextColor != INVALID_COLOR) {
            setShutterTextColor(shutterTextColor);
        }
        ColorStateList shutterTextColorStateList = typedArray.getColorStateList(R.styleable.CameraExposureModeSettingWidget_uxsdk_shutterTextColor);
        if (shutterTextColorStateList != null) {
            setShutterTextColor(shutterTextColorStateList);
        }

        Drawable shutterBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_shutterBackground);
        if (shutterBackground != null) {
            setShutterBackground(shutterBackground);
        }

        int apertureTextAppearanceId = typedArray.getResourceId(R.styleable.CameraExposureModeSettingWidget_uxsdk_apertureTextAppearance, INVALID_RESOURCE);
        if (apertureTextAppearanceId != INVALID_RESOURCE) {
            setApertureTextAppearance(apertureTextAppearanceId);
        }

        float apertureTextSize = typedArray.getDimension(R.styleable.CameraExposureModeSettingWidget_uxsdk_apertureTextSize, INVALID_RESOURCE);
        if (apertureTextSize != INVALID_RESOURCE) {
            setApertureTextSize(DisplayUtil.pxToSp(context, apertureTextSize));
        }

        @ColorInt int apertureTextColor = typedArray.getColor(R.styleable.CameraExposureModeSettingWidget_uxsdk_apertureTextColor, INVALID_COLOR);
        if (apertureTextColor != INVALID_COLOR) {
            setApertureTextColor(apertureTextColor);
        }
        ColorStateList apertureTextColorStateList = typedArray.getColorStateList(R.styleable.CameraExposureModeSettingWidget_uxsdk_apertureTextColor);
        if (apertureTextColorStateList != null) {
            setApertureTextColor(apertureTextColorStateList);
        }

        Drawable apertureBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_apertureBackground);
        if (apertureBackground != null) {
            setApertureBackground(apertureBackground);
        }

        int manualTextAppearanceId = typedArray.getResourceId(R.styleable.CameraExposureModeSettingWidget_uxsdk_manualTextAppearance, INVALID_RESOURCE);
        if (manualTextAppearanceId != INVALID_RESOURCE) {
            setManualTextAppearance(manualTextAppearanceId);
        }

        float manualTextSize = typedArray.getDimension(R.styleable.CameraExposureModeSettingWidget_uxsdk_manualTextSize, INVALID_RESOURCE);
        if (manualTextSize != INVALID_RESOURCE) {
            setManualTextSize(DisplayUtil.pxToSp(context, manualTextSize));
        }

        @ColorInt int manualTextColor = typedArray.getColor(R.styleable.CameraExposureModeSettingWidget_uxsdk_manualTextColor, INVALID_COLOR);
        if (manualTextColor != INVALID_COLOR) {
            setManualTextColor(manualTextColor);
        }
        ColorStateList manualTextColorStateList = typedArray.getColorStateList(R.styleable.CameraExposureModeSettingWidget_uxsdk_manualTextColor);
        if (manualTextColorStateList != null) {
            setManualTextColor(manualTextColorStateList);
        }

        Drawable manualBackground = typedArray.getDrawable(R.styleable.CameraExposureModeSettingWidget_uxsdk_manualBackground);
        if (manualBackground != null) {
            setManualBackground(manualBackground);
        }

        typedArray.recycle();
    }
    //endregion
}
