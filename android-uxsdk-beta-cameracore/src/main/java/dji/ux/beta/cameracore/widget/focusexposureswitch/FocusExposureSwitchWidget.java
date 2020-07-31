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

package dji.ux.beta.cameracore.widget.focusexposureswitch;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.FrameLayoutWidget;
import dji.ux.beta.core.base.GlobalPreferencesManager;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;
import dji.ux.beta.core.util.SettingDefinitions.ControlMode;

/**
 * Focus Exposure Switch Widget
 * <p>
 * This widget can be used to switch the {@link ControlMode} between focus and exposure
 * When in focus mode the {@link FPVInteractionWidget} will help change the focus point
 * When in exposure mode the {@link FPVInteractionWidget} will help change exposure/metering
 */
public class FocusExposureSwitchWidget extends FrameLayoutWidget implements OnClickListener {

    //region fields
    private static final String TAG = "FocusExpoSwitchWidget";
    private ImageView focusExposureSwitchImageView;
    private FocusExposureSwitchWidgetModel widgetModel;
    private Drawable manualFocusDrawable;
    private Drawable autoFocusDrawable;
    private Drawable spotMeterDrawable;
    private SchedulerProvider schedulerProvider;
    //endregion

    //region lifecycle
    public FocusExposureSwitchWidget(@NonNull Context context) {
        super(context);
    }

    public FocusExposureSwitchWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusExposureSwitchWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_focus_exposure_switch, this);
        focusExposureSwitchImageView = findViewById(R.id.focus_exposure_switch_image_view);
        schedulerProvider = SchedulerProvider.getInstance();
        if (getBackground() == null) {
            setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        }
        if (!isInEditMode()) {
            widgetModel = new FocusExposureSwitchWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance(),
                    schedulerProvider);
        }

        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        setOnClickListener(this);
    }

    private void initDefaults() {
        manualFocusDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_focus_switch_manual);
        autoFocusDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_focus_switch_auto);
        spotMeterDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_metering_switch);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getControlMode().observeOn(schedulerProvider.ui()).subscribe(this::updateUI));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == this.getId()) {
            addDisposable(widgetModel.switchControlMode()
                    .observeOn(schedulerProvider.ui())
                    .subscribe(() -> {
                        //do nothing
                    }, logErrorConsumer(TAG, "switchControlMode: ")));
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

    //region private methods
    private void updateUI(ControlMode controlMode) {
        if (controlMode == ControlMode.SPOT_METER || controlMode == ControlMode.CENTER_METER) {
            focusExposureSwitchImageView.setImageDrawable(spotMeterDrawable);
        } else if (controlMode == ControlMode.MANUAL_FOCUS) {
            focusExposureSwitchImageView.setImageDrawable(manualFocusDrawable);
        } else {
            focusExposureSwitchImageView.setImageDrawable(autoFocusDrawable);
        }
    }

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getControlMode().firstOrError()
                    .observeOn(schedulerProvider.ui())
                    .subscribe(this::updateUI, logErrorConsumer(TAG, "Update UI ")));
        }
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FocusExposureSwitchWidget);
        setCameraIndex(CameraIndex.find(typedArray.getInt(R.styleable.FocusExposureSwitchWidget_uxsdk_cameraIndex, 0)));
        if (typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_meteringDrawable) != null) {
            spotMeterDrawable = typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_meteringDrawable);
        }
        if (typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_manualFocusDrawable) != null) {
            manualFocusDrawable = typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_manualFocusDrawable);
        }
        if (typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_autoFocusDrawable) != null) {
            autoFocusDrawable = typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_autoFocusDrawable);
        }
        setIconBackground(typedArray.getDrawable(R.styleable.FocusExposureSwitchWidget_uxsdk_iconBackground));

        typedArray.recycle();
    }
    //endregion

    //region customization methods
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    /**
     * Gets the camera index used by the widget
     *
     * @return Camera index
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the camera key index for which this model should subscribe to.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get current manual focus icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getManualFocusIcon() {
        return manualFocusDrawable;
    }

    /**
     * Set manual focus icon
     *
     * @param resourceId to be used
     */
    public void setManualFocusIcon(@DrawableRes int resourceId) {
        setManualFocusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set manual focus icon
     *
     * @param drawable to be used
     */
    public void setManualFocusIcon(@Nullable Drawable drawable) {
        manualFocusDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get current auto focus icon
     *
     * @return drawable
     */
    @Nullable
    public Drawable getAutoFocusIcon() {
        return autoFocusDrawable;
    }

    /**
     * Set auto focus icon
     *
     * @param resourceId to be used
     */
    public void setAutoFocusIcon(@DrawableRes int resourceId) {
        setAutoFocusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set auto focus icon
     *
     * @param drawable to be used
     */
    public void setAutoFocusIcon(@Nullable Drawable drawable) {
        autoFocusDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get current metering/exposure mode icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getMeteringIcon() {
        return spotMeterDrawable;
    }

    /**
     * Set metering/exposure mode icon
     *
     * @param resourceId to be used
     */
    public void setMeteringIcon(@DrawableRes int resourceId) {
        setMeteringIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set metering/exposure mode icon
     *
     * @param drawable to be used
     */
    public void setMeteringIcon(@Nullable Drawable drawable) {
        spotMeterDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get current icon background
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getIconBackground() {
        return focusExposureSwitchImageView.getBackground();
    }

    /**
     * Set icon background
     *
     * @param resourceId to be used
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        focusExposureSwitchImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set icon background
     *
     * @param drawable to be used
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        focusExposureSwitchImageView.setBackground(drawable);
    }
    //endregion
}
