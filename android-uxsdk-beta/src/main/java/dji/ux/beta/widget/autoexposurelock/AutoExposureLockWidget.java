/*
 * Copyright (c) 2018-2019 DJI
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

package dji.ux.beta.widget.autoexposurelock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.SchedulerProvider;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.SettingDefinitions.CameraIndex;

/**
 * Auto Exposure Lock Widget will display the current state of exposure lock.
 * <p>
 * When locked the exposure of the camera will remain constant.
 * Changing the exposure parameters manually will release the lock.
 */
public class AutoExposureLockWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region
    private ImageView foregroundImageView;
    private TextView titleTextView;
    private AutoExposureLockWidgetModel widgetModel;
    private boolean isLocked;
    private Drawable autoExposureLockDrawable;
    private Drawable autoExposureUnlockDrawable;
    private SchedulerProvider schedulerProvider;
    //endregion

    //region lifecycle
    public AutoExposureLockWidget(@NonNull Context context) {
        super(context);
    }

    public AutoExposureLockWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoExposureLockWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_auto_exposure_lock, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        schedulerProvider = SchedulerProvider.getInstance();
        foregroundImageView = findViewById(R.id.auto_exposure_lock_widget_foreground_image_view);
        titleTextView = findViewById(R.id.auto_exposure_lock_widget_title_text_view);
        if (!isInEditMode()) {
            widgetModel =
                    new AutoExposureLockWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance(),
                            schedulerProvider);
        }
        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        setOnClickListener(this);
    }


    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.isAutoExposureLockOn().observeOn(AndroidSchedulers.mainThread()).subscribe(this::updateUI));
    }

    @Override
    public void onClick(View v) {
        if (v == this) {
            setAutoExposureLock();
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
    //region Reactions to model
    private void updateUI(boolean isAutoExposureLockOn) {
        if (isAutoExposureLockOn != isLocked) {
            isLocked = isAutoExposureLockOn;
            onAELockChange(isLocked);
        }
    }
    //endregion

    //region private methods
    @MainThread
    private void onAELockChange(boolean isLocked) {
        if (isLocked) {
            foregroundImageView.setImageDrawable(autoExposureLockDrawable);
        } else {
            foregroundImageView.setImageDrawable(autoExposureUnlockDrawable);
        }
    }

    private void setAutoExposureLock() {
        isLocked = !isLocked;
        onAELockChange(isLocked);
        addDisposable(widgetModel.setAutoExposureLock(isLocked)
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        () -> {
                            // Do nothing
                        },
                        error -> {
                            isLocked = !isLocked;
                            onAELockChange(isLocked);
                        }
                ));
    }

    private void initDefaults() {
        autoExposureLockDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_auto_exposure_lock);
        autoExposureUnlockDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_auto_exposure_unlock);
        setCameraIndex(CameraIndex.CAMERA_INDEX_0);
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoExposureLockWidget);
        setCameraIndex(CameraIndex.find(typedArray.getInt(R.styleable.AutoExposureLockWidget_uxsdk_cameraIndex, 0)));
        ColorStateList colorStateList = getResources().getColorStateList(R.color.uxsdk_color_selector_auto_exposure_lock);
        if (typedArray.getColorStateList(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextColor) != null) {
            colorStateList = typedArray.getColorStateList(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextColor);
        }
        setTitleTextColor(colorStateList);
        int textAppearance = typedArray.getResourceId(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(textAppearance);
        }
        if (typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawable) != null) {
            autoExposureLockDrawable = typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawable);
        }

        if (typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawable) != null) {
            autoExposureUnlockDrawable = typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawable);
        }
        onAELockChange(isLocked);
        typedArray.recycle();
    }
    //endregion

    //region customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_auto_exposure_lock_ratio);
    }

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return instance of {@link CameraIndex}.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set drawable for auto exposure lock in locked state
     *
     * @param resourceId to be used
     */
    public void setAutoExposureLockIcon(@DrawableRes int resourceId) {
        setAutoExposureLockIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set drawable for auto exposure lock in locked state
     *
     * @param drawable to be used
     */
    public void setAutoExposureLockIcon(@Nullable Drawable drawable) {
        this.autoExposureLockDrawable = drawable;
        onAELockChange(isLocked);
    }

    /**
     * Get current drawable resource for auto exposure lock in locked state
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAutoExposureLockDrawable() {
        return autoExposureLockDrawable;
    }

    /**
     * Set resource for auto exposure lock in unlocked state
     *
     * @param resourceId to be used
     */
    public void setAutoExposureUnlockIcon(@DrawableRes int resourceId) {
        setAutoExposureUnlockIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set drawable for auto exposure lock in unlocked state
     *
     * @param drawable to be used
     */
    public void setAutoExposureUnlockIcon(@Nullable Drawable drawable) {
        this.autoExposureUnlockDrawable = drawable;
        onAELockChange(isLocked);
    }

    /**
     * Get current drawable resource for auto exposure lock in unlocked state
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAutoExposureUnlockDrawable() {
        return autoExposureUnlockDrawable;
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
     * Set background to icon
     *
     * @param resourceId to be used
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        setIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to icon
     *
     * @param drawable to be used
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get current background of icon
     *
     * @return Drawable
     */
    @NonNull
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
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

    @Override
    public void setEnabled(boolean enabled) {
        titleTextView.setEnabled(enabled);
        foregroundImageView.setEnabled(enabled);
        super.setEnabled(enabled);
    }
    //endregion
}
