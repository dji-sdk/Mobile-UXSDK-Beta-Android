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

package dji.ux.beta.visualcamera.widget.colorwaveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.ColorWaveformSettings.ColorWaveformDisplayState;
import dji.sdk.codec.DJICodecManager;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.GlobalPreferencesManager;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.visualcamera.R;

/**
 * The ColorWaveformWidget uses data from the video stream to display a waveform. Additionally, the
 * user can switch between Color and Exposure modes as well as close the ColorWaveformWidget
 * using the supplied buttons.
 * <p>
 * In order to retrieve the data, this widget needs to be supplied with a DJICodecManager using
 * {@link ColorWaveformWidget#setCodecManager(DJICodecManager)}. The easiest way to get a
 * DJICodecManager is to wait for FPVWidget to initialize its DJICodecManager using
 * {@link FPVWidget#setCodecManagerCallback}.
 * <p>
 * To set different images for the selected and deselected states of the color mode and exposure
 * mode buttons, create a StateListDrawable and use `android:state_selected="true"` for the
 * selected state.
 */
public class ColorWaveformWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region Constants
    private static final String TAG = "ColorWaveformWidget";
    //endregion

    //region Fields
    private ColorWaveformView waveformView;
    private ImageView closeImageView;
    private ImageView colorModeImageView;
    private ImageView exposureModeImageView;
    private ColorWaveformWidgetModel widgetModel;
    //endregion

    //region Constructor
    public ColorWaveformWidget(@NonNull Context context) {
        super(context);
    }

    public ColorWaveformWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorWaveformWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_color_waveform, this);
        setBackgroundColor(getResources().getColor(R.color.uxsdk_black));

        waveformView = findViewById(R.id.view_waveform);
        closeImageView = findViewById(R.id.imageview_close_button);
        colorModeImageView = findViewById(R.id.imageview_color_switch);
        exposureModeImageView = findViewById(R.id.imageview_exposure_switch);
        closeImageView.setOnClickListener(this);
        colorModeImageView.setOnClickListener(this);
        exposureModeImageView.setOnClickListener(this);

        if (!isInEditMode()) {
            widgetModel = new ColorWaveformWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
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
        addReaction(widgetModel.getColorWaveformEnabled()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateColorWaveformWidgetUI));

        addReaction(widgetModel.getColorWaveformDisplayState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onColorWaveformDisplayChange));
    }
    //endregion

    //region Reaction helpers
    private void updateColorWaveformWidgetUI(boolean isColorWaveformEnabled) {
        if (isColorWaveformEnabled) {
            openWaveform();
        } else {
            closeWaveform();
        }
    }

    private void onColorWaveformDisplayChange(ColorWaveformDisplayState state) {
        boolean isExp = ColorWaveformDisplayState.EXPOSURE == state;
        colorModeImageView.setSelected(!isExp);
        exposureModeImageView.setSelected(isExp);
        waveformView.setDisplayState(state);
    }

    private void openWaveform() {
        setVisibility(VISIBLE);
        waveformView.setVisibility(VISIBLE);
    }

    private void closeWaveform() {
        setVisibility(GONE);
        waveformView.setVisibility(GONE);
    }

    /**
     * Sets the display state of the color waveform.
     *
     * @param displayState The display state of the color waveform.
     */
    public void setColorWaveformDisplayState(@NonNull ColorWaveformDisplayState displayState) {
        addDisposable(widgetModel.setColorWaveformDisplayState(displayState).subscribe());
    }

    /**
     * Get whether the color waveform is enabled.
     *
     * @return A Flowable that will emit a boolean when the enabled state of the color waveform
     * changes.
     */
    @NonNull
    public Flowable<Boolean> getColorWaveformEnabled() {
        return widgetModel.getColorWaveformEnabled();
    }

    /**
     * Enables or disables the color waveform feature and then sets the visibility of the widget
     * when successful.
     *
     * @param enabled True to enable the color waveform, false to disable.
     */
    public void setColorWaveformEnabled(boolean enabled) {
        addDisposable(widgetModel.setColorWaveformEnabled(enabled).subscribe());
    }
    //endregion

    //region Reactions to user input
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.imageview_color_switch) {
            setColorWaveformDisplayState(ColorWaveformDisplayState.COLOR);
        } else if (i == R.id.imageview_exposure_switch) {
            setColorWaveformDisplayState(ColorWaveformDisplayState.EXPOSURE);
        } else if (i == R.id.imageview_close_button) {
            setColorWaveformEnabled(false);
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_color_waveform_ratio);
    }

    /**
     * Set the codec manager for this color waveform.
     *
     * @param codecManager An instance of {@link DJICodecManager}
     */
    public void setCodecManager(@Nullable DJICodecManager codecManager) {
        waveformView.setCodecManager(codecManager);
    }

    /**
     * Get the drawable resource for the close icon
     *
     * @return Drawable resource of the icon
     */
    @Nullable
    public Drawable getCloseIcon() {
        return closeImageView.getDrawable();
    }

    /**
     * Set the resource ID for the close icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setCloseIcon(@DrawableRes int resourceId) {
        setCloseIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the close icon
     *
     * @param icon Drawable resource for the image
     */
    public void setCloseIcon(@Nullable Drawable icon) {
        closeImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the close icon's background
     *
     * @return Drawable resource of the icon's background
     */
    @Nullable
    public Drawable getCloseIconBackground() {
        return closeImageView.getBackground();
    }

    /**
     * Set the resource ID for the close icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    public void setCloseIconBackground(@DrawableRes int resourceId) {
        closeImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the close icon's background
     *
     * @param background Drawable resource for the icon's background
     */
    public void setCloseIconBackground(@Nullable Drawable background) {
        closeImageView.setBackground(background);
    }

    /**
     * Get the drawable resource for the color mode icon
     *
     * @return Drawable resource of the icon
     */
    @Nullable
    public Drawable getColorModeIcon() {
        return colorModeImageView.getDrawable();
    }

    /**
     * Set the resource ID for the color mode icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setColorModeIcon(@DrawableRes int resourceId) {
        setColorModeIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the color mode icon
     *
     * @param icon Drawable resource for the image
     */
    public void setColorModeIcon(@Nullable Drawable icon) {
        colorModeImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the color mode icon's background
     *
     * @return Drawable resource of the icon's background
     */
    @Nullable
    public Drawable getColorModeIconBackground() {
        return colorModeImageView.getBackground();
    }

    /**
     * Set the resource ID for the color mode icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    public void setColorModeIconBackground(@DrawableRes int resourceId) {
        colorModeImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the color mode icon's background
     *
     * @param background Drawable resource for the icon's background
     */
    public void setColorModeIconBackground(@Nullable Drawable background) {
        colorModeImageView.setBackground(background);
    }

    /**
     * Get the drawable resource for the exposure mode icon
     *
     * @return Drawable resource of the icon
     */
    @Nullable
    public Drawable getExposureModeIcon() {
        return exposureModeImageView.getDrawable();
    }

    /**
     * Set the resource ID for the exposure mode icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setExposureModeIcon(@DrawableRes int resourceId) {
        setExposureModeIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the exposure mode icon
     *
     * @param icon Drawable resource for the image
     */
    public void setExposureModeIcon(@Nullable Drawable icon) {
        exposureModeImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the exposure mode icon's background
     *
     * @return Drawable resource of the icon's background
     */
    @Nullable
    public Drawable getExposureModeIconBackground() {
        return exposureModeImageView.getBackground();
    }

    /**
     * Set the resource ID for the exposure mode icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    public void setExposureModeIconBackground(@DrawableRes int resourceId) {
        exposureModeImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the exposure mode icon's background
     *
     * @param background Drawable resource for the icon's background
     */
    public void setExposureModeIconBackground(@Nullable Drawable background) {
        exposureModeImageView.setBackground(background);
    }

    /**
     * Gets the number of horizontal lines displayed on the waveform view.
     *
     * @return The number of horizontal lines.
     */
    public int getHorizontalLineNum() {
        return waveformView.getHorizontalLineNum();
    }

    /**
     * Sets the number of horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineNum The number of horizontal lines.
     */
    public void setHorizontalLineNum(int horizontalLineNum) {
        waveformView.setHorizontalLineNum(horizontalLineNum);
    }

    /**
     * Gets the color of the horizontal lines displayed on the waveform view.
     *
     * @return The color of the horizontal lines.
     */
    @ColorInt
    public int getHorizontalLineColor() {
        return waveformView.getHorizontalLineColor();
    }

    /**
     * Sets the color of the horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineColor The color of the horizontal lines.
     */
    public void setHorizontalLineColor(@ColorInt int horizontalLineColor) {
        waveformView.setHorizontalLineColor(horizontalLineColor);
    }

    /**
     * Gets the width of the horizontal lines displayed on the waveform view.
     *
     * @return The width of the horizontal lines.
     */
    public float getHorizontalLineWidth() {
        return waveformView.getHorizontalLineWidth();
    }

    /**
     * Sets the width of the horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineWidth The width of the horizontal lines.
     */
    public void setHorizontalLineWidth(float horizontalLineWidth) {
        waveformView.setHorizontalLineWidth(horizontalLineWidth);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorWaveformWidget);

        Drawable closeIcon = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_closeIcon);
        if (closeIcon != null) {
            setCloseIcon(closeIcon);
        }

        Drawable closeIconBackground = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_closeIconBackground);
        if (closeIconBackground != null) {
            setCloseIconBackground(closeIconBackground);
        }

        Drawable colorModeIcon = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_colorModeIcon);
        if (colorModeIcon != null) {
            setColorModeIcon(colorModeIcon);
        }

        Drawable colorModeIconBackground = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_colorModeIconBackground);
        if (colorModeIconBackground != null) {
            setColorModeIconBackground(colorModeIconBackground);
        }

        Drawable exposureIcon = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_exposureModeIcon);
        if (exposureIcon != null) {
            setExposureModeIcon(exposureIcon);
        }

        Drawable exposureIconBackground = typedArray.getDrawable(R.styleable.ColorWaveformWidget_uxsdk_exposureModeIconBackground);
        if (exposureIconBackground != null) {
            setExposureModeIconBackground(exposureIconBackground);
        }

        int horizontalLineNum = typedArray.getInt(R.styleable.ColorWaveformWidget_uxsdk_horizontalLineNum, getHorizontalLineNum());
        setHorizontalLineNum(horizontalLineNum);

        int horizontalLineColor = typedArray.getColor(R.styleable.ColorWaveformWidget_uxsdk_horizontalLineColor, getHorizontalLineColor());
        setHorizontalLineColor(horizontalLineColor);

        float horizontalLineWidth = typedArray.getFloat(R.styleable.ColorWaveformWidget_uxsdk_horizontalLineWidth, getHorizontalLineWidth());
        setHorizontalLineWidth(horizontalLineWidth);

        typedArray.recycle();
    }
    //endregion
}
