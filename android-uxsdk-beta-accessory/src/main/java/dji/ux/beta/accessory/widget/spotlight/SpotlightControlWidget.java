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

package dji.ux.beta.accessory.widget.spotlight;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;

import dji.ux.beta.accessory.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Widget can be used to control of state of Spotlight accessory
 * Spotlight can be switched on and switched off and its
 * brightness can be controlled.
 */
public class SpotlightControlWidget extends ConstraintLayoutWidget
        implements OnSeekBarChangeListener, OnCheckedChangeListener, OnStateChangeCallback {

    //region Fields
    private static final String TAG = "SpotlightControlWidget";
    private SpotlightControlWidgetModel widgetModel;
    private TextView headerTextView;
    private TextView brightnessLabelTextView;
    private TextView enabledLabelTextView;
    private TextView temperatureLabelTextView;
    private TextView temperatureValueTextView;
    private TextView warningMessageTextView;
    private SeekBar brightnessSeekbar;
    private Switch enableSwitch;
    //endregion

    //region Lifecycle
    public SpotlightControlWidget(Context context) {
        super(context);
    }

    public SpotlightControlWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpotlightControlWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_spotlight_control, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        headerTextView = findViewById(R.id.spotlight_header_text);
        enabledLabelTextView = findViewById(R.id.spotlight_enabled_label);
        brightnessLabelTextView = findViewById(R.id.spotlight_brightness_label);
        brightnessSeekbar = findViewById(R.id.spotlight_brightness_seekbar);
        temperatureLabelTextView = findViewById(R.id.spotlight_temperature_label);
        temperatureValueTextView = findViewById(R.id.spotlight_temperature_value);
        enableSwitch = findViewById(R.id.spotlight_enabled_switch);
        warningMessageTextView = findViewById(R.id.spotlight_warning);

        if (!isInEditMode()) {
            widgetModel =
                    new SpotlightControlWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }

        enableSwitch.setOnCheckedChangeListener(this);
        brightnessSeekbar.setOnSeekBarChangeListener(this);

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.isSpotlightConnected()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateEnabled));

        addReaction(widgetModel.getSpotlightState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
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

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_spotlight_control_ratio);
    }

    @Override
    public void onStateChange(@Nullable Object state) {
        toggleVisibility();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        handleCheckChanged();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) handleSeekbarChanged(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Empty function
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Empty function
    }
    //endregion

    //region private helpers

    private void toggleVisibility() {
        if (getVisibility() == VISIBLE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpotlightControlWidget);

        int textAppearance =
                typedArray.getResourceId(R.styleable.SpotlightControlWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setHeaderTextAppearance(textAppearance);
        }
        setHeaderTextColor(typedArray.getColor(R.styleable.SpotlightControlWidget_uxsdk_widgetTitleTextColor, Color.WHITE));
        setHeaderTextBackground(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_widgetTitleBackground));
        setHeaderTextSize(typedArray.getDimension(R.styleable.SpotlightControlWidget_uxsdk_widgetTitleTextSize, 14));
        textAppearance = typedArray.getResourceId(R.styleable.SpotlightControlWidget_uxsdk_labelsTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setLabelsTextAppearance(textAppearance);
        }
        setLabelsTextColor(typedArray.getColor(R.styleable.SpotlightControlWidget_uxsdk_labelsTextColor, Color.WHITE));
        setLabelsTextBackground(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_labelsBackground));
        setLabelsTextSize(typedArray.getDimension(R.styleable.SpotlightControlWidget_uxsdk_labelsTextSize, 12));
        textAppearance = typedArray.getResourceId(R.styleable.SpotlightControlWidget_uxsdk_warningTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setWarningMessageTextAppearance(textAppearance);
        }
        setWarningMessageTextColor(typedArray.getColor(R.styleable.SpotlightControlWidget_uxsdk_warningTextColor, Color.WHITE));
        setWarningMessageTextBackground(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_warningTextBackground));
        setWarningMessageTextSize(typedArray.getDimension(R.styleable.SpotlightControlWidget_uxsdk_warningTextSize, 12));
        textAppearance =
                typedArray.getResourceId(R.styleable.SpotlightControlWidget_uxsdk_temperatureValueTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTemperatureValueTextAppearance(textAppearance);
        }
        setTemperatureValueTextColor(typedArray.getColor(R.styleable.SpotlightControlWidget_uxsdk_temperatureValueTextColor,
                Color.WHITE));
        setTemperatureValueTextBackground(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_temperatureValueTextBackground));
        setTemperatureValueTextSize(typedArray.getDimension(R.styleable.SpotlightControlWidget_uxsdk_temperatureValueTextSize, 12));

        if (typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_switchThumbIcon) != null) {
            setSwitchThumb(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_switchThumbIcon));
        }

        if (typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_seekbarThumbIcon) != null) {
            setSwitchThumb(typedArray.getDrawable(R.styleable.SpotlightControlWidget_uxsdk_seekbarThumbIcon));
        }

        typedArray.recycle();
    }

    private void updateUI(SpotlightState spotlightState) {
        enableSwitch.setChecked(spotlightState.isEnabled());
        temperatureValueTextView.setText(getResources().getString(R.string.uxsdk_spotlight_temperature_value,
                spotlightState.getTemperature()));
        brightnessSeekbar.setProgress(spotlightState.getBrightnessPercentage());
    }

    private void updateEnabled(boolean isConnected) {
        brightnessSeekbar.setEnabled(isConnected);
        enableSwitch.setEnabled(isConnected);
    }

    private void handleCheckChanged() {
        addDisposable(widgetModel.toggleSpotlight().subscribe(() -> {
        }, logErrorConsumer(TAG, "Enable check ")));
    }

    private void handleSeekbarChanged(int progress) {
        addDisposable(widgetModel.setSpotlightBrightnessPercentage(progress).subscribe(() -> {
        }, logErrorConsumer(TAG, "Set Progress ")));
    }

    //endregion

    //region customizations

    /**
     * Get the background of the header
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getHeaderTextBackground() {
        return headerTextView.getBackground();
    }

    /**
     * Set the background of the header
     *
     * @param resourceId to be used
     */
    public void setHeaderTextBackground(@DrawableRes int resourceId) {
        headerTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the header
     *
     * @param drawable to be used
     */
    public void setHeaderTextBackground(@NonNull Drawable drawable) {
        headerTextView.setBackground(drawable);
    }

    /**
     * Get the color state list of header text
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getHeaderTextColors() {
        return headerTextView.getTextColors();
    }

    /**
     * Set color state list of header text
     *
     * @param colorStateList to be used
     */
    public void setHeaderTextColors(@Nullable ColorStateList colorStateList) {
        headerTextView.setTextColor(colorStateList);
    }

    /**
     * Get the color of header text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getHeaderTextColor() {
        return headerTextView.getCurrentTextColor();
    }

    /**
     * Set the color of header text
     *
     * @param color integer value
     */
    public void setHeaderTextColor(@ColorInt int color) {
        headerTextView.setTextColor(color);
    }

    /**
     * Get the current text size of header
     *
     * @return float value representing text size
     */
    @Dimension
    public float getHeaderTextSize() {
        return headerTextView.getTextSize();
    }

    /**
     * Set the text size of header
     *
     * @param textSize float value
     */
    public void setHeaderTextSize(@Dimension float textSize) {
        headerTextView.setTextSize(textSize);
    }

    /**
     * Set the text appearance of header
     *
     * @param textAppearance to be used
     */
    public void setHeaderTextAppearance(@StyleRes int textAppearance) {
        headerTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the background of labels
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getLabelsTextBackground() {
        return brightnessLabelTextView.getBackground();
    }

    /**
     * Set the background of labels
     *
     * @param resourceId to be used
     */
    public void setLabelsTextBackground(@DrawableRes int resourceId) {
        brightnessLabelTextView.setBackgroundResource(resourceId);
        temperatureLabelTextView.setBackgroundResource(resourceId);
        enabledLabelTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of labels
     *
     * @param drawable to be used
     */
    public void setLabelsTextBackground(@NonNull Drawable drawable) {
        brightnessLabelTextView.setBackground(drawable);
        temperatureLabelTextView.setBackground(drawable);
        enabledLabelTextView.setBackground(drawable);
    }

    /**
     * Get the color state list used for labels
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getLabelsTextColors() {
        return brightnessLabelTextView.getTextColors();
    }

    /**
     * Set the color state list of label text
     *
     * @param colorStateList to be used
     */
    public void setLabelsTextColors(@Nullable ColorStateList colorStateList) {
        brightnessLabelTextView.setTextColor(colorStateList);
        temperatureLabelTextView.setTextColor(colorStateList);
        enabledLabelTextView.setTextColor(colorStateList);
    }

    /**
     * Get the current color of label text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getLabelsTextColor() {
        return brightnessLabelTextView.getCurrentTextColor();
    }

    /**
     * Set the color of text in all labels
     *
     * @param color integer value
     */
    public void setLabelsTextColor(@ColorInt int color) {
        brightnessLabelTextView.setTextColor(color);
        temperatureLabelTextView.setTextColor(color);
        enabledLabelTextView.setTextColor(color);
    }

    /**
     * Get the current text size of labels
     *
     * @return float value representing text size
     */
    @Dimension
    public float getLabelsTextSize() {
        return brightnessLabelTextView.getTextSize();
    }

    /**
     * Set the text size of all the labels
     *
     * @param textSize float value
     */
    public void setLabelsTextSize(@Dimension float textSize) {
        brightnessLabelTextView.setTextSize(textSize);
        temperatureLabelTextView.setTextSize(textSize);
        enabledLabelTextView.setTextSize(textSize);
    }

    /**
     * Set the text appearance of all the labels
     *
     * @param textAppearance to be used
     */
    public void setLabelsTextAppearance(@StyleRes int textAppearance) {
        brightnessLabelTextView.setTextAppearance(getContext(), textAppearance);
        temperatureLabelTextView.setTextAppearance(getContext(), textAppearance);
        enabledLabelTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the background of the temperature value text
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getTemperatureValueTextBackground() {
        return temperatureValueTextView.getBackground();
    }

    /**
     * Set the background of the temperature value text
     *
     * @param resourceId to be used
     */
    public void setTemperatureValueTextBackground(@DrawableRes int resourceId) {
        temperatureValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the temperature value text
     *
     * @param drawable to be used
     */
    public void setTemperatureValueTextBackground(@NonNull Drawable drawable) {
        temperatureValueTextView.setBackground(drawable);
    }

    /**
     * Get the color state list used for temperature value text
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getTemperatureValueTextColors() {
        return temperatureValueTextView.getTextColors();
    }

    /**
     * Set the color state list for temperature value text
     *
     * @param colorStateList to be used
     */
    public void setTemperatureValueTextColors(@Nullable ColorStateList colorStateList) {
        temperatureValueTextView.setTextColor(colorStateList);
    }

    /**
     * Get the current color of temperature text value
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTemperatureValueTextColor() {
        return temperatureValueTextView.getCurrentTextColor();
    }

    /**
     * Set the color of temperature value text
     *
     * @param color integer value
     */
    public void setTemperatureValueTextColor(@ColorInt int color) {
        temperatureValueTextView.setTextColor(color);
    }

    /**
     * Get the temperature value text size
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTemperatureValueTextSize() {
        return temperatureValueTextView.getTextSize();
    }

    /**
     * Set the temperature value text size
     *
     * @param textSize float value
     */
    public void setTemperatureValueTextSize(@Dimension float textSize) {
        temperatureValueTextView.setTextSize(textSize);
    }

    /**
     * Set the text appearance for temperature value text
     *
     * @param textAppearance to be used
     */
    public void setTemperatureValueTextAppearance(@StyleRes int textAppearance) {
        temperatureValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the current background of warning message text
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getWarningMessageTextBackground() {
        return warningMessageTextView.getBackground();
    }

    /**
     * Set the background of warning message text
     *
     * @param resourceId to be used
     */
    public void setWarningMessageTextBackground(@DrawableRes int resourceId) {
        warningMessageTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of warning message text
     *
     * @param drawable to be used
     */
    public void setWarningMessageTextBackground(@NonNull Drawable drawable) {
        warningMessageTextView.setBackground(drawable);
    }

    /**
     * Get warning message text color state list
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getWarningMessageTextColors() {
        return warningMessageTextView.getTextColors();
    }

    /**
     * Set the text color state list for warning message text
     *
     * @param colorStateList to be used
     */
    public void setWarningMessageTextColors(@Nullable ColorStateList colorStateList) {
        warningMessageTextView.setTextColor(colorStateList);
    }

    /**
     * Get the current text color of warning message
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getWarningMessageTextColor() {
        return warningMessageTextView.getCurrentTextColor();
    }

    /**
     * Set the text color of warning text message
     *
     * @param color integer value
     */
    public void setWarningMessageTextColor(@ColorInt int color) {
        warningMessageTextView.setTextColor(color);
    }

    /**
     * Get the current text size of warning message
     *
     * @return float value representing text size
     */
    @Dimension
    public float getWarningMessageTextSize() {
        return warningMessageTextView.getTextSize();
    }

    /**
     * Set the text size of the warning message
     *
     * @param textSize float value
     */
    public void setWarningMessageTextSize(@Dimension float textSize) {
        warningMessageTextView.setTextSize(textSize);
    }

    /**
     * Set text appearance of message text
     *
     * @param textAppearance to be used
     */
    public void setWarningMessageTextAppearance(@StyleRes int textAppearance) {
        warningMessageTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the thumb icon for the enable switch
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getSwitchThumb() {
        return enableSwitch.getThumbDrawable();
    }

    /**
     * Set the thumb icon for the enable switch
     *
     * @param resourceId to be used
     */
    public void setSwitchThumb(@DrawableRes int resourceId) {
        enableSwitch.setThumbResource(resourceId);
    }

    /**
     * Set the thumb icon for the enable switch
     *
     * @param drawable to be used
     */
    public void setSwitchThumb(@Nullable Drawable drawable) {
        enableSwitch.setThumbDrawable(drawable);
    }

    /**
     * Get the thumb tint color state list for enable switch
     *
     * @return ColorStateList
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Nullable
    public ColorStateList getSwitchThumbTintList() {
        return enableSwitch.getThumbTintList();
    }

    /**
     * Set the thumb tint color state list for enable switch
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public void setSwitchThumbTintList(@Nullable ColorStateList colorStateList) {
        enableSwitch.setThumbTintList(colorStateList);
    }

    /**
     * Get the thumb icon of brightness seek bar
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getBrightnessSeekbarThumbDrawable() {
        return brightnessSeekbar.getThumb();
    }

    /**
     * Set the thumb icon for brightness seek bar
     *
     * @param resourceId to be used
     */
    public void setBrightnessSeekbarThumbDrawable(@DrawableRes int resourceId) {
        setBrightnessSeekbarThumbDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the thumb icon for brightness seek bar
     *
     * @param drawable to be used
     */
    public void setBrightnessSeekbarThumbDrawable(@NonNull Drawable drawable) {
        brightnessSeekbar.setThumb(drawable);
    }

    /**
     * Get the color state list of the background of brightness seek bar
     *
     * @return ColorStateList
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorStateList getBrightnessSeekbarBackgroundTintList() {
        return brightnessSeekbar.getBackgroundTintList();
    }

    /**
     * Set the color state list for background of brightness seek bar
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setBrightnessSeekbarBackgroundTintList(@Nullable ColorStateList colorStateList) {
        brightnessSeekbar.setBackgroundTintList(colorStateList);
    }

    /**
     * Get the color state list used for brightness seek bar progress tint
     *
     * @return ColorStateList
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorStateList getBrightnessSeekbarProgressTintList() {
        return brightnessSeekbar.getProgressTintList();
    }

    /**
     * Set the color state list for brightness seek bar progress tint
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setBrightnessSeekbarProgressTintList(@Nullable ColorStateList colorStateList) {
        brightnessSeekbar.setProgressTintList(colorStateList);
    }
    //endregion
}
