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

package dji.ux.beta.cameracore.widget.manualfocus;

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

import dji.common.camera.SettingsDefinitions;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.GlobalPreferencesManager;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.ui.RulerView;
import dji.ux.beta.core.util.SettingDefinitions;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;

/**
 * Manual Focus Widget
 * <p>
 * Widget allows the focus distance to be set manually by using a slider. Macro and
 * infinity buttons can also be clicked to set focus distance to those values.
 */
public class ManualFocusWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region Fields
    private ImageView macroFocusImageView;
    private ImageView infinityFocusImageView;
    private RulerView focusRulerView;
    private ManualFocusWidgetModel widgetModel;
    //endregion

    //region Lifecycle
    public ManualFocusWidget(@NonNull Context context) {
        super(context);
    }

    public ManualFocusWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ManualFocusWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_manual_focus, this);
        setBackgroundResource(R.drawable.uxsdk_background_focus_wheel_bg);
        macroFocusImageView = findViewById(R.id.image_view_macro_focus);
        infinityFocusImageView = findViewById(R.id.image_view_infinity_focus);
        focusRulerView = findViewById(R.id.ruler_view_manual_focus);
        macroFocusImageView.setOnClickListener(this);
        infinityFocusImageView.setOnClickListener(this);
        focusRulerView.setOnChangeListener((rulerView, newSize, oldSize, fromUser) -> {
            if (fromUser) {
                setFocusValue();
            }
        });

        if (!isInEditMode()) {
            widgetModel = new ManualFocusWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }

    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getFocusRingValue()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));

        addReaction(widgetModel.getFocusRingUpperBoundValue()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateFocusUpperBound));

        addReaction(widgetModel.isManualFocusMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(isManualFocusMode -> setVisibility(isManualFocusMode ? VISIBLE : GONE)));

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
        return getResources().getString(R.string.uxsdk_widget_manual_focus_ratio);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(macroFocusImageView)) {
            focusRulerView.setCurSizeNow(focusRulerView.getMaxSize());
        } else if (v.equals(infinityFocusImageView)) {
            focusRulerView.setCurSizeNow(0);
        }
        setFocusValue();
    }

    //endregion

    //region private methods

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ManualFocusWidget);

        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.ManualFocusWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.ManualFocusWidget_uxsdk_lensType, 0)));

        if (typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_infinityFocusIcon) != null) {
            setInfinityFocusButtonIcon(typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_infinityFocusIcon));
        }
        setInfinityFocusButtonBackground(typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_infinityFocusIconBackground));

        if (typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_macroFocusIcon) != null) {
            setMacroFocusButtonIcon(typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_macroFocusIcon));
        }
        setMacroFocusButtonBackground(typedArray.getDrawable(R.styleable.ManualFocusWidget_uxsdk_macroFocusIconBackground));

        int color = typedArray.getColor(R.styleable.ManualFocusWidget_uxsdk_rulerLinesColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setRulerColor(color);
        }

        typedArray.recycle();
    }


    private void setFocusValue() {
        addDisposable(widgetModel.setFocusRingValue(focusRulerView.getCurSize()).subscribe(() -> {
                    // Do nothing
                },
                error -> {
                    // Do nothing
                }));
    }

    private void updateFocusUpperBound(int focusUpperBoundValue) {
        focusRulerView.setMaxSize(focusUpperBoundValue);
    }

    private void updateUI(int focusRingValue) {
        if (focusRingValue >= 0 && focusRingValue <= focusRulerView.getMaxSize() && (focusRulerView.getMaxSize() > 0)) {
            float value = 1f * (focusRingValue) / (focusRulerView.getMaxSize());
            focusRulerView.setCurSizeNow((int) (value * focusRulerView.getMaxSize()));
        }
    }
    //endregion

    //region customizations

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
     * Get the macro focus button icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getMacroFocusButtonIcon() {
        return macroFocusImageView.getDrawable();
    }

    /**
     * Set the macro focus button icon
     *
     * @param resourceId to be used
     */
    public void setMacroFocusButtonIcon(@DrawableRes int resourceId) {
        macroFocusImageView.setImageResource(resourceId);
    }

    /**
     * Set the macro focus button icon
     *
     * @param drawable to be used
     */
    public void setMacroFocusButtonIcon(@Nullable Drawable drawable) {
        macroFocusImageView.setImageDrawable(drawable);
    }

    /**
     * Get the background of the macro focus button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getMacroFocusButtonBackground() {
        return macroFocusImageView.getBackground();
    }

    /**
     * Set the background of the macro focus button
     *
     * @param resourceId to be used
     */
    public void setMacroFocusButtonBackground(@DrawableRes int resourceId) {
        macroFocusImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the macro focus button
     *
     * @param drawable to be used
     */
    public void setMacroFocusButtonBackground(@Nullable Drawable drawable) {
        macroFocusImageView.setBackground(drawable);
    }

    /**
     * Get infinity focus button icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getInfinityFocusButtonIcon() {
        return infinityFocusImageView.getDrawable();
    }

    /**
     * Set infinity focus button icon
     *
     * @param resourceId to be used
     */
    public void setInfinityFocusButtonIcon(@DrawableRes int resourceId) {
        infinityFocusImageView.setImageResource(resourceId);
    }

    /**
     * Set infinity focus button icon
     *
     * @param drawable to be used
     */
    public void setInfinityFocusButtonIcon(@Nullable Drawable drawable) {
        infinityFocusImageView.setImageDrawable(drawable);
    }

    /**
     * Get the background of the infinity focus button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getInfinityFocusButtonBackground() {
        return infinityFocusImageView.getBackground();
    }

    /**
     * Set the background of the infinity focus button
     *
     * @param resourceId to be used
     */
    public void setInfinityFocusButtonBackground(@DrawableRes int resourceId) {
        infinityFocusImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the infinity focus button
     *
     * @param drawable to be used
     */
    public void setInfinityFocusButtonBackground(@Nullable Drawable drawable) {
        infinityFocusImageView.setBackground(drawable);
    }

    /**
     * Get ruler scale color
     *
     * @return integer color value
     */
    @ColorInt
    public int getRulerColor() {
        return focusRulerView.getScaleColor();
    }

    /**
     * Set the ruler scale color
     *
     * @param color integer value
     */
    public void setRulerColor(@ColorInt int color) {
        focusRulerView.setScaleColor(color);
    }

    //endregion
}
