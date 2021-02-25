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

package dji.ux.beta.cameracore.widget.cameraevsetting;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

import dji.common.camera.SettingsDefinitions;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.ui.StripeView;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.AudioUtil;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Allows the user to set the camera's exposure compensation (EV). If the EV cannot be set in the
 * current camera state, the widget displays an EV status indicator instead.
 */
public class CameraEVSettingWidget extends ConstraintLayoutWidget {

    //region Fields
    private TextView evTitleText;
    private View cameraSettingBackground;
    private ImageView evMinusImage;
    private TextView evValueText;
    private ImageView evPlusImage;
    private StripeView evStatusView;
    private TextView evStatusValueText;
    private CameraEVSettingWidgetModel widgetModel;

    private int evCenterSound = R.raw.uxsdk_camera_ev_center;
    private int evChangeSound = R.raw.uxsdk_camera_simple_click;
    @ColorInt
    private int cameraSettingBackgroundColor;
    //endregion

    //region Lifecycle
    public CameraEVSettingWidget(@NonNull Context context) {
        super(context);
    }

    public CameraEVSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraEVSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_ev_setting, this);
        evTitleText = findViewById(R.id.textview_ev_title);
        cameraSettingBackground = findViewById(R.id.view_camera_setting_background);
        evMinusImage = findViewById(R.id.imageview_ev_setting_minus);
        evValueText = findViewById(R.id.textview_setting_ev_value);
        evPlusImage = findViewById(R.id.imageview_ev_setting_plus);
        evStatusView = findViewById(R.id.stripeview_setting_ev_status);
        evStatusValueText = findViewById(R.id.textview_setting_ev_status_value);
        cameraSettingBackgroundColor = getResources().getColor(R.color.uxsdk_black_30_percent);
        initClickListeners();

        if (!isInEditMode()) {
            widgetModel =
                    new CameraEVSettingWidgetModel(DJISDKModel.getInstance(),
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

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getEVRange()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateEVStatusViewRange));
        addReaction(reactToEVPosition());
        addReaction(widgetModel.isEditable()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateEditable));
        addReaction(widgetModel.isEIMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(isEIMode -> setVisibility(isEIMode ? GONE : VISIBLE)));
    }
    //endregion

    //region Reaction helpers
    @NonNull
    private Disposable reactToEVPosition() {
        return Flowable.combineLatest(widgetModel.getEVRange(), widgetModel.getCurrentEVPosition(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateEV(values.first, values.second));
    }

    private void updateEVStatusViewRange(@NonNull SettingsDefinitions.ExposureCompensation[] evValueArray) {
        evStatusView.setZeroPosition(evValueArray.length / 2);
    }

    public void updateEV(@NonNull SettingsDefinitions.ExposureCompensation[] evValueArray, int evPos) {
        updateEVStateView(getEVNames(evValueArray), evPos);
    }

    private void updateEVStateView(@NonNull String[] evNameArray, int curPos) {
        if (curPos >= 0 && curPos < evNameArray.length) {
            evStatusValueText.setText(evNameArray[curPos]);
            evValueText.setText(evNameArray[curPos]);
            evStatusView.setSelectedPosition(curPos);
        }
    }

    @NonNull
    private String[] getEVNames(@NonNull SettingsDefinitions.ExposureCompensation[] evValueArray) {
        String[] newEV = new String[evValueArray.length];
        for (int i = 0; i < evValueArray.length; i++) {
            String evName = CameraUtil.exposureValueDisplayName(evValueArray[i]);
            newEV[i] = evName;
        }
        return newEV;
    }

    private void updateEditable(boolean isEditable) {
        if (isEditable) {
            evPlusImage.setEnabled(true);
            evPlusImage.setVisibility(View.VISIBLE);
            evMinusImage.setEnabled(true);
            evMinusImage.setVisibility(View.VISIBLE);
            evValueText.setEnabled(true);
            evValueText.setVisibility(View.VISIBLE);

            evStatusView.setVisibility(View.INVISIBLE);
            evStatusValueText.setVisibility(View.INVISIBLE);
        } else {
            evMinusImage.setEnabled(false);
            evMinusImage.setVisibility(INVISIBLE);
            evPlusImage.setEnabled(false);
            evPlusImage.setVisibility(INVISIBLE);
            evValueText.setEnabled(false);
            evValueText.setVisibility(View.INVISIBLE);

            evStatusView.setVisibility(View.VISIBLE);
            evStatusValueText.setVisibility(View.VISIBLE);
        }
    }

    private void initClickListeners() {
        OnClickListener minusClickListener = v -> addDisposable(widgetModel.decrementEV()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        this::playEVChangeSound,
                        error -> {
                            // do nothing
                        }
                ));

        OnClickListener plusClickListener = v -> addDisposable(widgetModel.incrementEV()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        this::playEVChangeSound,
                        error -> {
                            // do nothing
                        }
                ));

        OnClickListener restoreClickListener = v -> addDisposable(widgetModel.restoreEV()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        this::playEVCenterSound,
                        error -> {
                            // do nothing
                        }
                ));

        evMinusImage.setOnClickListener(minusClickListener);
        evPlusImage.setOnClickListener(plusClickListener);
        evValueText.setOnClickListener(restoreClickListener);
    }

    private void playEVCenterSound() {
        addDisposable(AudioUtil.playSoundInBackground(getContext(), evCenterSound));
    }

    private void playEVChangeSound() {
        addDisposable(AudioUtil.playSoundInBackground(getContext(), evChangeSound));
    }
    //endregion

    //region customization helpers
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraEVSettingWidget);
        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.CameraEVSettingWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.CameraEVSettingWidget_uxsdk_lensType, 0)));

        int titleTextAppearance = typedArray.getResourceId(R.styleable.CameraEVSettingWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (titleTextAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(titleTextAppearance);
        }
        Drawable titleBackground = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_widgetTitleBackground);
        if (titleBackground != null) {
            setTitleBackground(titleBackground);
        }
        ColorStateList titleColorStateList = typedArray.getColorStateList(R.styleable.CameraEVSettingWidget_uxsdk_widgetTitleTextColor);
        if (titleColorStateList != null) {
            setTitleTextColor(titleColorStateList);
        }
        int titleColor = typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_widgetTitleTextColor, INVALID_COLOR);
        if (titleColor != INVALID_COLOR) {
            setTitleTextColor(titleColor);
        }
        float titleTextSize = typedArray.getDimension(R.styleable.CameraEVSettingWidget_uxsdk_widgetTitleTextSize, INVALID_RESOURCE);
        if (titleTextSize != INVALID_RESOURCE) {
            setTitleTextSize(DisplayUtil.pxToSp(context, titleTextSize));
        }

        int cameraSettingBackgroundColor = typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_cameraSettingBackgroundColor, INVALID_COLOR);
        if (cameraSettingBackgroundColor != INVALID_COLOR) {
            setCameraSettingBackgroundColor(cameraSettingBackgroundColor);
        }

        Drawable plusIcon = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_plusIcon);
        if (plusIcon != null) {
            setPlusIcon(plusIcon);
        }

        Drawable plusIconBackground = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_plusIconBackground);
        if (plusIconBackground != null) {
            setPlusIconBackground(plusIconBackground);
        }

        int evValueTextAppearanceId = typedArray.getResourceId(R.styleable.CameraEVSettingWidget_uxsdk_evValueTextAppearance, INVALID_RESOURCE);
        if (evValueTextAppearanceId != INVALID_RESOURCE) {
            setEVValueTextAppearance(evValueTextAppearanceId);
        }

        ColorStateList evValueColorStateList = typedArray.getColorStateList(R.styleable.CameraEVSettingWidget_uxsdk_evValueTextColor);
        if (evValueColorStateList != null) {
            setEVValueTextColor(evValueColorStateList);
        }

        @ColorInt int evValueTextColor = typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_evValueTextColor, getResources().getColor(R.color.uxsdk_blue_highlight));
        setEVValueTextColor(evValueTextColor);

        Drawable evValueTextBackground = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_evValueBackgroundDrawable);
        if (evValueTextBackground != null) {
            setEVValueTextBackground(evValueTextBackground);
        }

        float evValueTextSize = typedArray.getDimension(R.styleable.CameraEVSettingWidget_uxsdk_evValueTextSize, INVALID_RESOURCE);
        if (evValueTextSize != INVALID_RESOURCE) {
            setEVValueTextSize(DisplayUtil.pxToSp(context, evValueTextSize));
        }

        Drawable minusIcon = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_minusIcon);
        if (minusIcon != null) {
            setMinusIcon(minusIcon);
        }

        Drawable minusIconBackground = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_minusIconBackground);
        if (minusIconBackground != null) {
            setMinusIconBackground(minusIconBackground);
        }

        int evStatusTextAppearanceId = typedArray.getResourceId(R.styleable.CameraEVSettingWidget_uxsdk_evStatusTextAppearance, INVALID_RESOURCE);
        if (evStatusTextAppearanceId != INVALID_RESOURCE) {
            setEVStatusTextAppearance(evStatusTextAppearanceId);
        }

        ColorStateList evStatusColorStateList = typedArray.getColorStateList(R.styleable.CameraEVSettingWidget_uxsdk_evValueTextColor);
        if (evStatusColorStateList != null) {
            setEVStatusTextColor(evStatusColorStateList);
        }

        @ColorInt int evStatusTextColor = typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_evStatusTextColor, getResources().getColor(R.color.uxsdk_white));
        setEVStatusTextColor(evStatusTextColor);

        Drawable evStatusTextBackground = typedArray.getDrawable(R.styleable.CameraEVSettingWidget_uxsdk_evStatusBackgroundDrawable);
        if (evStatusTextBackground != null) {
            setEVStatusTextBackground(evStatusTextBackground);
        }

        float evStatusTextSize = typedArray.getDimension(R.styleable.CameraEVSettingWidget_uxsdk_evStatusTextSize, INVALID_RESOURCE);
        if (evStatusTextSize != INVALID_RESOURCE) {
            setEVStatusTextSize(DisplayUtil.pxToSp(context, evStatusTextSize));
        }

        setEVStatusLineColor(typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_evStatusLineColor, getResources().getColor(R.color.uxsdk_white_50_percent)));
        setEVStatusHighlightLineColor(typedArray.getColor(R.styleable.CameraEVSettingWidget_uxsdk_evStatusHighlightedLineColor, getResources().getColor(R.color.uxsdk_white)));

        typedArray.recycle();
    }
    //endregion

    //region customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_camera_ev_setting_ratio);
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
        return evTitleText.getBackground();
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
        evTitleText.setBackground(drawable);
    }

    /**
     * Get current text color state list of widget title
     *
     * @return ColorStateList used
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return evTitleText.getTextColors();
    }

    /**
     * Get the current color of title text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTitleTextColor() {
        return evTitleText.getCurrentTextColor();
    }

    /**
     * Set text color state list to the widget title
     *
     * @param colorStateList to be used
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        evTitleText.setTextColor(colorStateList);
    }

    /**
     * Set the color of title text
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        evTitleText.setTextColor(color);
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance to be used
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        evTitleText.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the size of title text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTitleTextSize() {
        return evTitleText.getTextSize();
    }

    /**
     * Set the size of the title text
     *
     * @param textSize float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        evTitleText.setTextSize(textSize);
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
     * Get the drawable resource for the minus icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getMinusIcon() {
        return evMinusImage.getDrawable();
    }

    /**
     * Set the resource ID for the minus icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setMinusIcon(@DrawableRes int resourceId) {
        setMinusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the minus icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setMinusIcon(@Nullable Drawable icon) {
        evMinusImage.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the minus icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getMinusIconBackground() {
        return evMinusImage.getBackground();
    }

    /**
     * Set the resource ID for the minus icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setMinusIconBackground(@DrawableRes int resourceId) {
        evMinusImage.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the minus icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setMinusIconBackground(@Nullable Drawable icon) {
        evMinusImage.setBackground(icon);
    }

    /**
     * Set text appearance of the EV value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setEVValueTextAppearance(@StyleRes int textAppearanceResId) {
        evValueText.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the EV value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getEVValueTextColor() {
        return evValueText.getCurrentTextColor();
    }

    /**
     * Set text color for the EV value text view
     *
     * @param color color integer resource
     */
    public void setEVValueTextColor(@ColorInt int color) {
        evValueText.setTextColor(color);
    }

    /**
     * Set text color state list for the EV value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setEVValueTextColor(@NonNull ColorStateList colorStateList) {
        evValueText.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the EV value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getTextColors() {
        return evValueText.getTextColors();
    }

    /**
     * Get current background of the EV value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getEVValueTextBackground() {
        return evValueText.getBackground();
    }

    /**
     * Set the resource ID for the background of the EV value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setEVValueTextBackground(@DrawableRes int resourceId) {
        evValueText.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the EV value text view
     *
     * @param background Drawable resource for the background
     */
    public void setEVValueTextBackground(@Nullable Drawable background) {
        evValueText.setBackground(background);
    }

    /**
     * Get current text size of the EV value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getEVValueTextSize() {
        return evValueText.getTextSize();
    }

    /**
     * Set the text size of the EV value text view
     *
     * @param textSize text size float value
     */
    public void setEVValueTextSize(@Dimension float textSize) {
        evValueText.setTextSize(textSize);
    }

    /**
     * Get the drawable resource for the plus icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getPlusIcon() {
        return evPlusImage.getDrawable();
    }

    /**
     * Set the resource ID for the plus icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setPlusIcon(@DrawableRes int resourceId) {
        setPlusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource plus icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setPlusIcon(@Nullable Drawable icon) {
        evPlusImage.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the plus icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getPlusIconBackground() {
        return evPlusImage.getBackground();
    }

    /**
     * Set the resource ID for the plus icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setPlusIconBackground(@DrawableRes int resourceId) {
        evPlusImage.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the plus icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setPlusIconBackground(@Nullable Drawable icon) {
        evPlusImage.setBackground(icon);
    }

    /**
     * Set text appearance of the EV status text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setEVStatusTextAppearance(@StyleRes int textAppearanceResId) {
        evStatusValueText.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the EV status text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getEVStatusTextColor() {
        return evStatusValueText.getCurrentTextColor();
    }

    /**
     * Set text color for the EV status text view
     *
     * @param color color integer resource
     */
    public void setEVStatusTextColor(@ColorInt int color) {
        evStatusValueText.setTextColor(color);
    }

    /**
     * Set text color state list for the EV status text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setEVStatusTextColor(@NonNull ColorStateList colorStateList) {
        evStatusValueText.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the EV status text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getEVStatusTextColors() {
        return evStatusValueText.getTextColors();
    }

    /**
     * Get current background of the EV status text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getEVStatusTextBackground() {
        return evStatusValueText.getBackground();
    }

    /**
     * Set the resource ID for the background of the EV status text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setEVStatusTextBackground(@DrawableRes int resourceId) {
        evStatusValueText.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the EV status text view
     *
     * @param background Drawable resource for the background
     */
    public void setEVStatusTextBackground(@Nullable Drawable background) {
        evStatusValueText.setBackground(background);
    }

    /**
     * Get current text size of the EV status text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getEVStatusTextSize() {
        return evStatusValueText.getTextSize();
    }

    /**
     * Set the text size of the EV status text view
     *
     * @param textSize text size float value
     */
    public void setEVStatusTextSize(@Dimension float textSize) {
        evStatusValueText.setTextSize(textSize);
    }

    /**
     * Get the color of the EV status lines
     *
     * @return The color of the lines
     */
    @ColorInt
    public int getEVStatusLineColor() {
        return evStatusView.getLineColor();
    }

    /**
     * Set the color of the EV status lines
     *
     * @param lineColor The color in which to draw the lines
     */
    public void setEVStatusLineColor(@ColorInt int lineColor) {
        evStatusView.setLineColor(lineColor);
    }

    /**
     * Get the color of the EV status highlighted lines
     *
     * @return The color of the highlighted lines
     */
    @ColorInt
    public int getEVStatusHighlightLineColor() {
        return evStatusView.getHighlightLineColor();
    }

    /**
     * Set the color of the EV status highlighted lines
     *
     * @param highlightLineColor The color in which to draw the highlighted lines
     */
    public void setEVStatusHighlightLineColor(@ColorInt int highlightLineColor) {
        evStatusView.setHighlightLineColor(highlightLineColor);
    }

    /**
     * Get the audio resource that plays when the EV is reset to the center point of the range.
     *
     * @return Raw resource of the sound
     */
    @RawRes
    public int getEVCenterSound() {
        return evCenterSound;
    }

    /**
     * Set the audio resource that plays when the EV is reset to the center point of the range.
     *
     * @param evCenterSound Raw resource of the sound
     */
    public void setEVCenterSound(@RawRes int evCenterSound) {
        this.evCenterSound = evCenterSound;
    }

    /**
     * Get the audio resource that plays when the EV is incremented or decremented.
     *
     * @return Raw resource of the sound
     */
    @RawRes
    public int getEVChangeSound() {
        return evChangeSound;
    }

    /**
     * Set the audio resource that plays when the EV is incremented or decremented.
     *
     * @param evChangeSound Raw resource of the sound
     */
    public void setEVChangeSound(@RawRes int evChangeSound) {
        this.evChangeSound = evChangeSound;
    }

    //endregion
}
