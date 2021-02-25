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

package dji.ux.beta.visualcamera.widget.manualzoom;

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
import androidx.annotation.StyleRes;

import dji.common.camera.SettingsDefinitions;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.ui.RulerView;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.visualcamera.R;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Manual Zoom Widget
 * The widget can be used to change the optical focal length.
 * In other words, it can be used to increase and decrease the optical zoom level of the camera
 * <p>
 * The widget will be visible only when a supported camera is connected.
 */
public class ManualZoomWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region Fields
    private static final String TAG = "ManualZoomWidget";
    private ManualZoomWidgetModel widgetModel;
    private RulerView rulerView;
    private ImageView plusButtonImageView;
    private ImageView minusButtonImageView;
    private TextView zoomLevelTextView;
    //endregion

    //region Lifecycle
    public ManualZoomWidget(@NonNull Context context) {
        super(context);
    }

    public ManualZoomWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ManualZoomWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_manual_zoom, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        rulerView = findViewById(R.id.ruler_view_manual_zoom);
        zoomLevelTextView = findViewById(R.id.text_view_zoom_value);
        minusButtonImageView = findViewById(R.id.image_view_zoom_out);
        minusButtonImageView.setOnClickListener(this);
        plusButtonImageView = findViewById(R.id.image_view_zoom_in);
        plusButtonImageView.setOnClickListener(this);
        rulerView.setOnScrollListener(new RulerView.OnRulerScrollListener() {
            @Override
            public void onScrollingStarted(RulerView rulerView) {
                //Do nothing
            }

            @Override
            public void onScrollingFinished(RulerView rView) {
                addDisposable(widgetModel.setZoomLevel(rulerView.getCurSize()).observeOn(SchedulerProvider.ui()).subscribe(() -> {
                }, logErrorConsumer(TAG, "Scroll ruler ")));
            }
        });
        if (!isInEditMode()) {
            widgetModel =
                    new ManualZoomWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }

    }


    @Override
    protected void reactToModelChanges() {
        addReaction(reactToScrollUpdate());
        addReaction(widgetModel.getOpticalZoomSpec().observeOn(SchedulerProvider.ui()).subscribe(this::onZoomSpecChange));
        addReaction(widgetModel.getZoomLevelText().observeOn(SchedulerProvider.ui()).subscribe(this::onZoomTextUpdated));
        addReaction(widgetModel.isSupported().observeOn(SchedulerProvider.ui()).subscribe(this::updateUI));

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
        return getResources().getString(R.string.uxsdk_widget_manual_zoom_ratio);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.image_view_zoom_out) {
            addDisposable(widgetModel.decreaseZoomLevel()
                    .subscribe(() -> {
                    }, logErrorConsumer(TAG, "decrease zoom level ")));
        } else if (id == R.id.image_view_zoom_in) {
            addDisposable(widgetModel.increaseZoomLevel()
                    .subscribe(() -> {
                    }, logErrorConsumer(TAG, "increase zoom level ")));
        }
    }
    //endregion

    //region private methods

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ManualZoomWidget);

        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.ManualZoomWidget_uxsdk_cameraIndex, 0)));
        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.ManualZoomWidget_uxsdk_lensType, 0)));

        int textAppearance = typedArray.getResourceId(R.styleable.ManualZoomWidget_uxsdk_zoomLevelTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setZoomLevelTextAppearance(textAppearance);
        }
        setZoomLevelTextColor(typedArray.getColor(R.styleable.ManualZoomWidget_uxsdk_zoomLevelTextColor, getResources().getColor(R.color.uxsdk_black)));
        if (typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomLevelTextBackground) != null) {
            setZoomLevelTextBackground(typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomLevelTextBackground));
        }
        setZoomLevelTextSize(typedArray.getDimension(R.styleable.ManualZoomWidget_uxsdk_zoomLevelTextSize, 12));
        if (typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomInIcon) != null) {
            setZoomInButtonIcon(typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomInIcon));
        }
        setZoomInButtonBackground(typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomInIconBackground));

        if (typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomOutIcon) != null) {
            setZoomOutButtonIcon(typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomOutIcon));
        }

        setZoomOutButtonBackground(typedArray.getDrawable(R.styleable.ManualZoomWidget_uxsdk_zoomOutIconBackground));
        typedArray.recycle();
    }

    private Disposable reactToScrollUpdate() {
        return Flowable.combineLatest(widgetModel.getCurrentZoomLevel(), widgetModel.isZooming(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> onZoomLevelChange(values.first, values.second),
                        logErrorConsumer(TAG, "react to Zoom level: "));
    }

    private void onZoomLevelChange(int zoomLevel, boolean isZooming) {
        if (!isZooming) {
            rulerView.setCurSizeNow(zoomLevel);
        }

    }

    private void onZoomSpecChange(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
        rulerView.setMaxSize(opticalZoomSpec.getMaxFocalLength() - opticalZoomSpec.getMinFocalLength());

    }

    private void onZoomTextUpdated(String zoomLevel) {
        zoomLevelTextView.setText(zoomLevel);
    }

    private void updateUI(boolean isSupported) {
        setVisibility(isSupported ? VISIBLE : GONE);
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
     * Get the zoom in button icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getZoomInButtonIcon() {
        return plusButtonImageView.getDrawable();
    }

    /**
     * Set the zoom in button icon
     *
     * @param resourceId to be used
     */
    public void setZoomInButtonIcon(@DrawableRes int resourceId) {
        plusButtonImageView.setImageResource(resourceId);
    }

    /**
     * Set the zoom in button icon
     *
     * @param drawable to be used
     */
    public void setZoomInButtonIcon(@Nullable Drawable drawable) {
        plusButtonImageView.setImageDrawable(drawable);
    }

    /**
     * Get the background of the zoom in button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getZoomInButtonBackground() {
        return plusButtonImageView.getBackground();
    }

    /**
     * Set the background of the zoom in button
     *
     * @param resourceId to be used
     */
    public void setZoomInButtonBackground(@DrawableRes int resourceId) {
        plusButtonImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the zoom in button
     *
     * @param drawable to be used
     */
    public void setZoomInButtonBackground(@Nullable Drawable drawable) {
        plusButtonImageView.setBackground(drawable);
    }

    /**
     * Get the zoom out button icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getZoomOutButtonIcon() {
        return minusButtonImageView.getDrawable();
    }

    /**
     * Set the zoom out button icon
     *
     * @param resourceId to be used
     */
    public void setZoomOutButtonIcon(@DrawableRes int resourceId) {
        minusButtonImageView.setImageResource(resourceId);
    }

    /**
     * Set the zoom out button icon
     *
     * @param drawable to be used
     */
    public void setZoomOutButtonIcon(@Nullable Drawable drawable) {
        minusButtonImageView.setImageDrawable(drawable);
    }

    /**
     * Get the background of the zoom out button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getZoomOutButtonBackground() {
        return minusButtonImageView.getBackground();
    }

    /**
     * Set the background of the zoom out button
     *
     * @param resourceId to be used
     */
    public void setZoomOutButtonBackground(@DrawableRes int resourceId) {
        minusButtonImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the zoom out button
     *
     * @param drawable to be be used
     */
    public void setZoomOutButtonBackground(@Nullable Drawable drawable) {
        minusButtonImageView.setBackground(drawable);
    }

    /**
     * Get ruler scale color
     *
     * @return integer color value
     */
    @ColorInt
    public int getRulerColor() {
        return rulerView.getScaleColor();
    }

    /**
     * Set the ruler scale color
     *
     * @param color integer value
     */
    public void setRulerColor(@ColorInt int color) {
        rulerView.setScaleColor(color);
    }

    /**
     * Get the background of the zoom level label
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getZoomLevelTextBackground() {
        return zoomLevelTextView.getBackground();
    }

    /**
     * Set the background of the zoom level label
     *
     * @param resourceId to be used
     */
    public void setZoomLevelTextBackground(@DrawableRes int resourceId) {
        zoomLevelTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the zoom level label
     *
     * @param drawable to be used
     */
    public void setZoomLevelTextBackground(@Nullable Drawable drawable) {
        zoomLevelTextView.setBackground(drawable);
    }

    /**
     * Get the text colors of zoom level label
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getZoomLevelTextColors() {
        return zoomLevelTextView.getTextColors();
    }

    /**
     * Set the text colors of zoom level label
     *
     * @param colorStateList to be used as color list
     */
    public void setZoomLevelTextColors(@Nullable ColorStateList colorStateList) {
        zoomLevelTextView.setTextColor(colorStateList);
    }

    /**
     * Get the text color of zoom level label
     *
     * @return integer color value
     */
    @ColorInt
    public int getZoomLevelTextColor() {
        return zoomLevelTextView.getCurrentTextColor();
    }

    /**
     * Set the text color of zoom level label
     *
     * @param color integer value representing color
     */
    public void setZoomLevelTextColor(@ColorInt int color) {
        zoomLevelTextView.setTextColor(color);
    }

    /**
     * Get the text size of zoom level label
     *
     * @return float value representing text size
     */
    @Dimension
    public float getZoomLevelTextSize() {
        return zoomLevelTextView.getTextSize();
    }

    /**
     * Set the text size of zoom level label
     *
     * @param textSize float value
     */
    public void setZoomLevelTextSize(@Dimension float textSize) {
        zoomLevelTextView.setTextSize(textSize);
    }

    /**
     * Set the text appearance of zoom level label
     *
     * @param textAppearance to be used
     */
    public void setZoomLevelTextAppearance(@StyleRes int textAppearance) {
        zoomLevelTextView.setTextAppearance(getContext(), textAppearance);
    }


    //endregion
}
