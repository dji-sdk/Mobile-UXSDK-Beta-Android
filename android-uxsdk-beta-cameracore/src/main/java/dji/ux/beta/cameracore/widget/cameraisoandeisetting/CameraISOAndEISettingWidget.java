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

package dji.ux.beta.cameracore.widget.cameraisoandeisetting;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

import dji.common.camera.SettingsDefinitions;
import dji.log.DJILog;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.ui.SeekBarView;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Allows the user to set the camera's ISO and EI. When the camera is in ISO mode, this widget will
 * display a button to select AUTO and a seek bar to select an ISO value. In EI mode, the widget
 * will display a seek bar to select an EI value.
 */
public class CameraISOAndEISettingWidget extends ConstraintLayoutWidget implements SeekBarView.OnSeekBarChangeListener, View.OnClickListener {

    //region Fields
    private static final String TAG = "ISOAndEISettingWidget";

    private TextView isoAndEITitleText;
    private View cameraSettingBackground;
    private SeekBarView seekBarISO;
    private TextView autoISOButton;
    private SeekBarView seekBarEI;
    private CameraISOAndEISettingWidgetModel widgetModel;

    private boolean isSeekBarTracking;
    private int isoAndEIChangeSound = R.raw.uxsdk_camera_simple_click;
    @ColorInt
    private int cameraSettingBackgroundColor;
    //endregion

    //region Lifecycle
    public CameraISOAndEISettingWidget(@NonNull Context context) {
        super(context);
    }

    public CameraISOAndEISettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraISOAndEISettingWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_iso_and_ei_setting, this);
        isoAndEITitleText = findViewById(R.id.textview_iso_title);
        cameraSettingBackground = findViewById(R.id.view_camera_setting_background);
        seekBarISO = findViewById(R.id.seekbar_iso);
        autoISOButton = findViewById(R.id.button_iso_auto);
        seekBarEI = findViewById(R.id.seekbar_ei);
        cameraSettingBackgroundColor = getResources().getColor(R.color.uxsdk_black_30_percent);

        seekBarISO.setProgress(0);
        seekBarISO.addOnSeekBarChangeListener(this);
        autoISOButton.setOnClickListener(this);
        seekBarEI.addOnSeekBarChangeListener(this);

        if (!isInEditMode()) {
            widgetModel =
                    new CameraISOAndEISettingWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }
        if (attrs != null) {
            initAttributes(context, attrs);
        }
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
    //endregion

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getISOInformation()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateISOWidget));
        addReaction(reactToUpdateAutoISOButton());
        addReaction(widgetModel.getEIInformation()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateEIRangeUI));
        addReaction(widgetModel.isRecordVideoEIMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateWidgetUI));
    }

    //region iso
    @NonNull
    private Disposable reactToUpdateAutoISOButton() {
        return Flowable.combineLatest(widgetModel.getISOInformation(), widgetModel.isAutoISOSupported(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateAutoISOButton(values.second,
                        values.first.getIsoRange().length > 0,
                        values.first.isISOAuto()));
    }

    private void updateISORangeUI(@NonNull SettingsDefinitions.ISO[] isoValueArray) {
        // Workaround where ISO range updates to single value in AUTO mode
        if (isoValueArray.length != 0) {
            int minCameraISO = CameraUtil.convertISOToInt(isoValueArray[0]);
            seekBarISO.setMinValueText(String.valueOf(minCameraISO));
            int maxCameraISO = CameraUtil.convertISOToInt(isoValueArray[isoValueArray.length - 1]);
            seekBarISO.setMaxValueText(String.valueOf(maxCameraISO));

            seekBarISO.setMax(isoValueArray.length - 1);
        }
    }

    private void updateISOWidget(@NonNull CameraISOAndEISettingWidgetModel.ISOInformation isoInformation) {
        updateISORangeUI(isoInformation.getIsoRange());
        if (isoInformation.isISOLocked()) {
            updateISOLocked();
        } else {
            if (!isSeekBarTracking) {
                updateISOValue(isoInformation.getIsoRange(), isoInformation.getIsoValue());
            }

            seekBarISO.enable(!isoInformation.isISOAuto() && isoInformation.getIsoRange().length > 0);
        }
    }

    private void updateAutoISOButton(boolean isAutoISOSupported,
                                     boolean isISOSeekBarEnabled,
                                     boolean isAutoISOSelected) {
        if (isAutoISOSupported && isISOSeekBarEnabled) {
            autoISOButton.setVisibility(VISIBLE);
            autoISOButton.setSelected(isAutoISOSelected);
        } else {
            autoISOButton.setVisibility(GONE);
        }
    }

    private void updateISOValue(@NonNull SettingsDefinitions.ISO[] array, int value) {
        int progress = getISOIndex(array, value);
        seekBarISO.setProgress(progress);
    }

    private static int getISOIndex(@NonNull SettingsDefinitions.ISO[] array, int isoValue) {
        int index = -1;
        SettingsDefinitions.ISO iso = CameraUtil.convertIntToISO(isoValue);
        for (int i = 0; i < array.length; i++) {
            if (iso == array[i]) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void updateISOLocked() {
        autoISOButton.setVisibility(GONE);
        seekBarISO.enable(false);
        seekBarISO.setProgress(seekBarISO.getMax() / 2 - 1);
    }
    //endregion

    //region ei
    private void updateEIRangeUI(@NonNull CameraISOAndEISettingWidgetModel.EIInformation eiInformation) {
        int[] array = eiInformation.getEiRange();
        if (array.length == 0) {
            seekBarEI.enable(false);
            return;
        } else {
            seekBarEI.enable(true);
        }

        // Workaround where ISO range updates to single value in AUTO mode
        seekBarEI.setMax(array.length - 1);
        seekBarEI.setMinValueText(String.valueOf(array[0]));
        seekBarEI.setMaxValueText(String.valueOf(array[array.length - 1]));
        updateEIValue(array, eiInformation.getEiValue());
        updateEIBaseline(array, eiInformation.getEiRecommendedValue());
    }

    private void updateWidgetUI(boolean isRecordVideoEIMode) {
        if (isRecordVideoEIMode) {
            isoAndEITitleText.setText(R.string.uxsdk_ei_title);
            seekBarISO.setVisibility(GONE);
            autoISOButton.setVisibility(GONE);
            seekBarEI.setVisibility(VISIBLE);
        } else {
            isoAndEITitleText.setText(R.string.uxsdk_camera_exposure_iso_title);
            seekBarISO.setVisibility(VISIBLE);
            autoISOButton.setVisibility(VISIBLE);
            seekBarEI.setVisibility(GONE);
        }
    }

    private void updateEIValue(@NonNull int[] array, int eiValue) {
        if (!isSeekBarTracking) {
            int progress = getEIIndex(array, eiValue);
            seekBarEI.setProgress(progress);
        }
    }

    private void updateEIBaseline(@NonNull int[] array, int eiRecommendedValue) {
        int progress = getEIIndex(array, eiRecommendedValue);

        if (progress >= 0) {
            seekBarEI.setBaselineProgress(progress);
            seekBarEI.setBaselineVisibility(true);
        } else {
            seekBarEI.setBaselineVisibility(false);
        }
    }

    private int getEIIndex(@NonNull int[] array, int eiValue) {
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == eiValue) {
                index = i;
                break;
            }
        }
        return index;
    }
    //endregion

    //region OnSeekBarChangeListener
    @Override
    public void onProgressChanged(@NonNull SeekBarView object, int progress, boolean isFromUI) {
        if (object == seekBarISO) {
            seekBarISO.setText(widgetModel.getISOText(progress));
        } else {
            seekBarEI.setText(widgetModel.getEIText(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(@NonNull SeekBarView object, int progress) {
        isSeekBarTracking = true;
    }

    @Override
    public void onStopTrackingTouch(@NonNull SeekBarView object, int progress) {
        addDisposable(AudioUtil.playSoundInBackground(getContext(), isoAndEIChangeSound));
        if (object == seekBarISO) {
            addDisposable(widgetModel.setISO(autoISOButton.isSelected(), progress)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(() -> isSeekBarTracking = false, throwable -> {
                        DJILog.e(TAG, "set ISO " + throwable.getLocalizedMessage());
                        isSeekBarTracking = false;
                        seekBarISO.restorePreviousProgress();
                    }));
        } else {
            addDisposable(widgetModel.setEI(progress)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(() -> isSeekBarTracking = false, throwable -> {
                        DJILog.e(TAG, "set EI " + throwable.getLocalizedMessage());
                        isSeekBarTracking = false;
                        seekBarEI.restorePreviousProgress();
                    }));
        }
    }

    @Override
    public void onPlusClicked(@NonNull SeekBarView seekBar) {
        // Plus button is not displayed
    }

    @Override
    public void onMinusClicked(@NonNull SeekBarView seekBar) {
        // Minus button is not displayed
    }
    //endregion

    //region OnClickListener
    @Override
    public void onClick(View v) {
        if (v == autoISOButton) {
            addDisposable(widgetModel.setISO(!autoISOButton.isSelected(), seekBarISO.getProgress())
                    .subscribe(() -> DJILog.d(TAG, "set auto iso success"),
                            error -> DJILog.d(TAG, "set auto iso fail " + error.toString())));
        }
    }
    //endregion

    //region customization helpers
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraISOAndEISettingWidget);
        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.CameraISOAndEISettingWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.CameraISOAndEISettingWidget_uxsdk_lensType, 0)));

        int titleTextAppearance = typedArray.getResourceId(R.styleable.CameraISOAndEISettingWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (titleTextAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(titleTextAppearance);
        }
        Drawable titleBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_widgetTitleBackground);
        if (titleBackground != null) {
            setTitleBackground(titleBackground);
        }
        ColorStateList titleColorStateList = typedArray.getColorStateList(R.styleable.CameraISOAndEISettingWidget_uxsdk_widgetTitleTextColor);
        if (titleColorStateList != null) {
            setTitleTextColor(titleColorStateList);
        }
        int titleColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_widgetTitleTextColor, INVALID_COLOR);
        if (titleColor != INVALID_COLOR) {
            setTitleTextColor(titleColor);
        }
        float titleTextSize = typedArray.getDimension(R.styleable.CameraISOAndEISettingWidget_uxsdk_widgetTitleTextSize, INVALID_RESOURCE);
        if (titleTextSize != INVALID_RESOURCE) {
            setTitleTextSize(DisplayUtil.pxToSp(context, titleTextSize));
        }

        int cameraSettingBackgroundColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_cameraSettingBackgroundColor, INVALID_COLOR);
        if (cameraSettingBackgroundColor != INVALID_COLOR) {
            setCameraSettingBackgroundColor(cameraSettingBackgroundColor);
        }

        int autoISOButtonTextAppearanceId = typedArray.getResourceId(R.styleable.CameraISOAndEISettingWidget_uxsdk_autoISOButtonTextAppearance, INVALID_RESOURCE);
        if (autoISOButtonTextAppearanceId != INVALID_RESOURCE) {
            setAutoISOButtonTextAppearance(autoISOButtonTextAppearanceId);
        }

        ColorStateList autoISOButtonColorStateList = typedArray.getColorStateList(R.styleable.CameraISOAndEISettingWidget_uxsdk_autoISOButtonTextColor);
        if (autoISOButtonColorStateList != null) {
            setAutoISOButtonTextColor(autoISOButtonColorStateList);
        }

        @ColorInt int autoISOButtonTextColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_autoISOButtonTextColor, getResources().getColor(R.color.uxsdk_white));
        setAutoISOButtonTextColor(autoISOButtonTextColor);

        Drawable autoISOButtonTextBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_autoISOButtonBackgroundDrawable);
        if (autoISOButtonTextBackground != null) {
            setAutoISOButtonTextBackground(autoISOButtonTextBackground);
        }

        float autoISOButtonTextSize = typedArray.getDimension(R.styleable.CameraISOAndEISettingWidget_uxsdk_autoISOButtonTextSize, INVALID_RESOURCE);
        if (autoISOButtonTextSize != INVALID_RESOURCE) {
            setAutoISOButtonTextSize(DisplayUtil.pxToSp(context, autoISOButtonTextSize));
        }

        Drawable seekBarTrackIcon = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarTrackIcon);
        if (seekBarTrackIcon != null) {
            setSeekBarTrackIcon(seekBarTrackIcon);
        }

        Drawable seekBarTrackIconBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarTrackIconBackground);
        if (seekBarTrackIconBackground != null) {
            setSeekBarTrackIconBackground(seekBarTrackIconBackground);
        }

        Drawable seekBarThumbIcon = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarThumbIcon);
        if (seekBarThumbIcon != null) {
            setSeekBarThumbIcon(seekBarThumbIcon);
        }

        Drawable seekBarThumbIconBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarThumbIconBackground);
        if (seekBarThumbIconBackground != null) {
            setSeekBarThumbIconBackground(seekBarThumbIconBackground);
        }

        int seekBarValueTextAppearanceId = typedArray.getResourceId(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarValueTextAppearance, INVALID_RESOURCE);
        if (seekBarValueTextAppearanceId != INVALID_RESOURCE) {
            setSeekBarValueTextAppearance(seekBarValueTextAppearanceId);
        }

        ColorStateList seekBarValueColorStateList = typedArray.getColorStateList(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarValueTextColor);
        if (seekBarValueColorStateList != null) {
            setSeekBarValueTextColor(seekBarValueColorStateList);
        }

        @ColorInt int seekBarValueTextColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarValueTextColor, getResources().getColor(R.color.uxsdk_selector_slider_bar_text));
        setSeekBarValueTextColor(seekBarValueTextColor);

        Drawable seekBarValueTextBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarValueBackgroundDrawable);
        if (seekBarValueTextBackground != null) {
            setSeekBarValueTextBackground(seekBarValueTextBackground);
        }

        float seekBarValueTextSize = typedArray.getDimension(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarValueTextSize, INVALID_RESOURCE);
        if (seekBarValueTextSize != INVALID_RESOURCE) {
            setSeekBarValueTextSize(DisplayUtil.pxToSp(context, seekBarValueTextSize));
        }

        int seekBarMinValueTextAppearanceId = typedArray.getResourceId(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMinValueTextAppearance, INVALID_RESOURCE);
        if (seekBarMinValueTextAppearanceId != INVALID_RESOURCE) {
            setSeekBarMinValueTextAppearance(seekBarMinValueTextAppearanceId);
        }

        ColorStateList seekBarMinValueColorStateList = typedArray.getColorStateList(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMinValueTextColor);
        if (seekBarMinValueColorStateList != null) {
            setSeekBarMinValueTextColor(seekBarMinValueColorStateList);
        }

        @ColorInt int seekBarMinValueTextColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMinValueTextColor, getResources().getColor(R.color.uxsdk_white_75_percent));
        setSeekBarMinValueTextColor(seekBarMinValueTextColor);

        Drawable seekBarMinValueTextBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMinValueBackgroundDrawable);
        if (seekBarMinValueTextBackground != null) {
            setSeekBarMinValueTextBackground(seekBarMinValueTextBackground);
        }

        float seekBarMinValueTextSize = typedArray.getDimension(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMinValueTextSize, INVALID_RESOURCE);
        if (seekBarMinValueTextSize != INVALID_RESOURCE) {
            setSeekBarMinValueTextSize(DisplayUtil.pxToSp(context, seekBarMinValueTextSize));
        }

        int seekBarMaxValueTextAppearanceId = typedArray.getResourceId(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMaxValueTextAppearance, INVALID_RESOURCE);
        if (seekBarMaxValueTextAppearanceId != INVALID_RESOURCE) {
            setSeekBarMaxValueTextAppearance(seekBarMaxValueTextAppearanceId);
        }

        ColorStateList seekBarMaxValueColorStateList = typedArray.getColorStateList(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMaxValueTextColor);
        if (seekBarMaxValueColorStateList != null) {
            setSeekBarMaxValueTextColor(seekBarMaxValueColorStateList);
        }

        @ColorInt int seekBarMaxValueTextColor = typedArray.getColor(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMaxValueTextColor, getResources().getColor(R.color.uxsdk_white_75_percent));
        setSeekBarMaxValueTextColor(seekBarMaxValueTextColor);

        Drawable seekBarMaxValueTextBackground = typedArray.getDrawable(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMaxValueBackgroundDrawable);
        if (seekBarMaxValueTextBackground != null) {
            setSeekBarMaxValueTextBackground(seekBarMaxValueTextBackground);
        }

        float seekBarMaxValueTextSize = typedArray.getDimension(R.styleable.CameraISOAndEISettingWidget_uxsdk_seekBarMaxValueTextSize, INVALID_RESOURCE);
        if (seekBarMaxValueTextSize != INVALID_RESOURCE) {
            setSeekBarMaxValueTextSize(DisplayUtil.pxToSp(context, seekBarMaxValueTextSize));
        }

        typedArray.recycle();
    }
    //endregion

    //region customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_camera_iso_and_ei_setting_ratio);
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
     * Get current background of title text
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getTitleBackground() {
        return isoAndEITitleText.getBackground();
    }

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
        isoAndEITitleText.setBackground(drawable);
    }

    /**
     * Get current text color state list of widget title
     *
     * @return ColorStateList used
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return isoAndEITitleText.getTextColors();
    }

    /**
     * Get the current color of title text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTitleTextColor() {
        return isoAndEITitleText.getCurrentTextColor();
    }

    /**
     * Set text color state list to the widget title
     *
     * @param colorStateList to be used
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        isoAndEITitleText.setTextColor(colorStateList);
    }

    /**
     * Set the color of title text
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        isoAndEITitleText.setTextColor(color);
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance to be used
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        isoAndEITitleText.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the size of title text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTitleTextSize() {
        return isoAndEITitleText.getTextSize();
    }

    /**
     * Set the size of the title text
     *
     * @param textSize float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        isoAndEITitleText.setTextSize(textSize);
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
     * Set the background color that is shown behind everything except the title
     *
     * @param color color integer resource
     */
    public void setCameraSettingBackgroundColor(@ColorInt int color) {
        cameraSettingBackgroundColor = color;
        cameraSettingBackground.setBackgroundColor(color);
    }

    /**
     * Set text appearance of the auto ISO button text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setAutoISOButtonTextAppearance(@StyleRes int textAppearanceResId) {
        autoISOButton.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the auto ISO button text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getAutoISOButtonTextColor() {
        return autoISOButton.getCurrentTextColor();
    }

    /**
     * Set text color for the auto ISO button text view
     *
     * @param color color integer resource
     */
    public void setAutoISOButtonTextColor(@ColorInt int color) {
        autoISOButton.setTextColor(color);
    }

    /**
     * Set text color state list for the auto ISO button text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setAutoISOButtonTextColor(@NonNull ColorStateList colorStateList) {
        autoISOButton.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the auto ISO button text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getTextColors() {
        return autoISOButton.getTextColors();
    }

    /**
     * Get current background of the auto ISO button text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getAutoISOButtonTextBackground() {
        return autoISOButton.getBackground();
    }

    /**
     * Set the resource ID for the background of the auto ISO button text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setAutoISOButtonTextBackground(@DrawableRes int resourceId) {
        autoISOButton.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the auto ISO button text view
     *
     * @param background Drawable resource for the background
     */
    public void setAutoISOButtonTextBackground(@Nullable Drawable background) {
        autoISOButton.setBackground(background);
    }

    /**
     * Get current text size of the auto ISO button text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getAutoISOButtonTextSize() {
        return autoISOButton.getTextSize();
    }

    /**
     * Set the text size of the auto ISO button text view
     *
     * @param textSize text size float value
     */
    public void setAutoISOButtonTextSize(@Dimension float textSize) {
        autoISOButton.setTextSize(textSize);
    }

    /**
     * Get the drawable resource for the seek bar's track icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getSeekBarTrackIcon() {
        return seekBarISO.getTrackIcon();
    }

    /**
     * Set the resource ID for the seek bar's track icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setSeekBarTrackIcon(@DrawableRes int resourceId) {
        seekBarISO.setTrackIcon(resourceId);
        seekBarEI.setTrackIcon(resourceId);
    }

    /**
     * Set the drawable resource for the seek bar's track icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setSeekBarTrackIcon(@Nullable Drawable icon) {
        seekBarISO.setTrackIcon(icon);
        seekBarEI.setTrackIcon(icon);
    }

    /**
     * Get the drawable resource for the seek bar's track icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getSeekBarTrackIconBackground() {
        return seekBarISO.getTrackIconBackground();
    }

    /**
     * Set the resource ID for the seek bar's track icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setSeekBarTrackIconBackground(@DrawableRes int resourceId) {
        seekBarISO.setTrackIconBackground(resourceId);
        seekBarEI.setTrackIconBackground(resourceId);
    }

    /**
     * Set the drawable resource for the seek bar's track icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setSeekBarTrackIconBackground(@Nullable Drawable icon) {
        seekBarISO.setTrackIconBackground(icon);
        seekBarEI.setTrackIconBackground(icon);
    }

    /**
     * Get the drawable resource for the seek bar's thumb icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getSeekBarThumbIcon() {
        return seekBarISO.getThumbIcon();
    }

    /**
     * Set the resource ID for the seek bar's thumb icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setSeekBarThumbIcon(@DrawableRes int resourceId) {
        seekBarISO.setThumbIcon(resourceId);
        seekBarEI.setThumbIcon(resourceId);
    }

    /**
     * Set the drawable resource for the seek bar's thumb icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setSeekBarThumbIcon(@Nullable Drawable icon) {
        seekBarISO.setThumbIcon(icon);
        seekBarEI.setThumbIcon(icon);
    }

    /**
     * Get the drawable resource for the seek bar's thumb icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getSeekBarThumbIconBackground() {
        return seekBarISO.getThumbIconBackground();
    }

    /**
     * Set the resource ID for the seek bar's thumb icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setSeekBarThumbIconBackground(@DrawableRes int resourceId) {
        seekBarISO.setThumbIconBackground(resourceId);
        seekBarEI.setThumbIconBackground(resourceId);
    }

    /**
     * Set the drawable resource for the seek bar's thumb icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setSeekBarThumbIconBackground(@Nullable Drawable icon) {
        seekBarISO.setThumbIconBackground(icon);
        seekBarEI.setThumbIconBackground(icon);
    }

    /**
     * Set text appearance of the seek bar's value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setSeekBarValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarISO.setValueTextAppearance(textAppearanceResId);
        seekBarEI.setValueTextAppearance(textAppearanceResId);
    }

    /**
     * Get current text color of the seek bar's value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSeekBarValueTextColor() {
        return seekBarISO.getValueTextColor();
    }

    /**
     * Set text color for the seek bar's value text view
     *
     * @param color color integer resource
     */
    public void setSeekBarValueTextColor(@ColorInt int color) {
        seekBarISO.setValueTextColor(color);
        seekBarEI.setValueTextColor(color);
    }

    /**
     * Set text color state list for the seek bar's value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSeekBarValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarISO.setValueTextColor(colorStateList);
        seekBarEI.setValueTextColor(colorStateList);
    }

    /**
     * Get current color state list of the seek bar's value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getSeekBarValueTextColors() {
        return seekBarISO.getValueTextColors();
    }

    /**
     * Get current background of the seek bar's value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSeekBarValueTextBackground() {
        return seekBarISO.getValueTextBackground();
    }

    /**
     * Set the resource ID for the background of the seek bar's value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setSeekBarValueTextBackground(@DrawableRes int resourceId) {
        seekBarISO.setValueTextBackground(resourceId);
        seekBarEI.setValueTextBackground(resourceId);
    }

    /**
     * Set the background of the seek bar's value text view
     *
     * @param background Drawable resource for the background
     */
    public void setSeekBarValueTextBackground(@Nullable Drawable background) {
        seekBarISO.setValueTextBackground(background);
        seekBarEI.setValueTextBackground(background);
    }

    /**
     * Get current text size of the seek bar's value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSeekBarValueTextSize() {
        return seekBarISO.getValueTextSize();
    }

    /**
     * Set the text size of the seek bar's value text view
     *
     * @param textSize text size float value
     */
    public void setSeekBarValueTextSize(@Dimension float textSize) {
        seekBarISO.setValueTextSize(textSize);
        seekBarEI.setValueTextSize(textSize);
    }

    /**
     * Set text appearance of the seek bar's min value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setSeekBarMinValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarISO.setMinValueTextAppearance(textAppearanceResId);
        seekBarEI.setMinValueTextAppearance(textAppearanceResId);
    }

    /**
     * Get current text color of the seek bar's min value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSeekBarMinValueTextColor() {
        return seekBarISO.getMinValueTextColor();
    }

    /**
     * Set text color for the seek bar's min value text view
     *
     * @param color color integer resource
     */
    public void setSeekBarMinValueTextColor(@ColorInt int color) {
        seekBarISO.setMinValueTextColor(color);
        seekBarEI.setMinValueTextColor(color);
    }

    /**
     * Set text color state list for the seek bar's min value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSeekBarMinValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarISO.setMinValueTextColor(colorStateList);
        seekBarEI.setMinValueTextColor(colorStateList);
    }

    /**
     * Get current color state list of the seek bar's min value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getSeekBarMinValueTextColors() {
        return seekBarISO.getMinValueTextColors();
    }

    /**
     * Get current background of the seek bar's min value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSeekBarMinValueTextBackground() {
        return seekBarISO.getMinValueTextBackground();
    }

    /**
     * Set the resource ID for the background of the seek bar's min value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setSeekBarMinValueTextBackground(@DrawableRes int resourceId) {
        seekBarISO.setMinValueTextBackground(resourceId);
        seekBarEI.setMinValueTextBackground(resourceId);
    }

    /**
     * Set the background of the seek bar's min value text view
     *
     * @param background Drawable resource for the background
     */
    public void setSeekBarMinValueTextBackground(@Nullable Drawable background) {
        seekBarISO.setMinValueTextBackground(background);
        seekBarEI.setMinValueTextBackground(background);
    }

    /**
     * Get current text size of the seek bar's min value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSeekBarMinValueTextSize() {
        return seekBarISO.getMinValueTextSize();
    }

    /**
     * Set the text size of the seek bar's min value text view
     *
     * @param textSize text size float value
     */
    public void setSeekBarMinValueTextSize(@Dimension float textSize) {
        seekBarISO.setMinValueTextSize(textSize);
        seekBarEI.setMinValueTextSize(textSize);
    }

    /**
     * Set text appearance of the seek bar's max value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setSeekBarMaxValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarISO.setMaxValueTextAppearance(textAppearanceResId);
        seekBarEI.setMaxValueTextAppearance(textAppearanceResId);
    }

    /**
     * Get current text color of the seek bar's max value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSeekBarMaxValueTextColor() {
        return seekBarISO.getMaxValueTextColor();
    }

    /**
     * Set text color for the seek bar's max value text view
     *
     * @param color color integer resource
     */
    public void setSeekBarMaxValueTextColor(@ColorInt int color) {
        seekBarISO.setMaxValueTextColor(color);
        seekBarEI.setMaxValueTextColor(color);
    }

    /**
     * Set text color state list for the seek bar's max value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSeekBarMaxValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarISO.setMaxValueTextColor(colorStateList);
        seekBarEI.setMaxValueTextColor(colorStateList);
    }

    /**
     * Get current color state list of the seek bar's max value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getSeekBarMaxValueTextColors() {
        return seekBarISO.getMaxValueTextColors();
    }

    /**
     * Get current background of the seek bar's max value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSeekBarMaxValueTextBackground() {
        return seekBarISO.getMaxValueTextBackground();
    }

    /**
     * Set the resource ID for the background of the seek bar's max value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setSeekBarMaxValueTextBackground(@DrawableRes int resourceId) {
        seekBarISO.setMaxValueTextBackground(resourceId);
        seekBarEI.setMaxValueTextBackground(resourceId);
    }

    /**
     * Set the background of the seek bar's max value text view
     *
     * @param background Drawable resource for the background
     */
    public void setSeekBarMaxValueTextBackground(@Nullable Drawable background) {
        seekBarISO.setMaxValueTextBackground(background);
        seekBarEI.setMaxValueTextBackground(background);
    }

    /**
     * Get current text size of the seek bar's max value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSeekBarMaxValueTextSize() {
        return seekBarISO.getMaxValueTextSize();
    }

    /**
     * Set the text size of the seek bar's max value text view
     *
     * @param textSize text size float value
     */
    public void setSeekBarMaxValueTextSize(@Dimension float textSize) {
        seekBarISO.setMaxValueTextSize(textSize);
        seekBarEI.setMaxValueTextSize(textSize);
    }

    /**
     * Get the audio resource that plays when the ISO or EI is changed.
     *
     * @return Raw resource of the sound
     */
    @RawRes
    public int getISOAndEIChangeSound() {
        return isoAndEIChangeSound;
    }

    /**
     * Set the audio resource that plays when the ISO or EI is changed.
     *
     * @param evChangeSound Raw resource of the sound
     */
    public void setISOAndEIChangeSound(@RawRes int evChangeSound) {
        this.isoAndEIChangeSound = evChangeSound;
    }
    //endregion
}
