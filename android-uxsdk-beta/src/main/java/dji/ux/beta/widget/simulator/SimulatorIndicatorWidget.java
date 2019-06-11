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

package dji.ux.beta.widget.simulator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.FrameLayoutWidget;
import dji.ux.beta.base.OnStateChangeCallback;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;

/**
 * Simulator Indicator Widget will display the current state of the simulator
 * <p>
 * Simulator Indicator Widget has two states
 * Active - Green icon indicates currently simulator is running on the device
 * InActive - White icon indicates simulator is currently turned off on the device
 */
public class SimulatorIndicatorWidget extends FrameLayoutWidget implements View.OnClickListener {

    //region fields
    private ImageView foregroundImageView;
    private SimulatorIndicatorWidgetModel widgetModel;
    private Drawable simulatorActiveDrawable;
    private Drawable simulatorInactiveDrawable;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    private int stateChangeResourceId;
    //endregion

    //region lifecycle
    public SimulatorIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public SimulatorIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SimulatorIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_simulator_indicator, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        foregroundImageView = findViewById(R.id.imageview_simulator_indicator);
        setOnClickListener(this);
        if (!isInEditMode()) {
            widgetModel = new SimulatorIndicatorWidgetModel(DJISDKModel.getInstance(),
                                                            ObservableInMemoryKeyedStore.getInstance());
        }

        initDefaults();
        stateChangeResourceId = INVALID_RESOURCE;
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.isSimulatorActive()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateUI));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
        initializeListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyListener();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
    }

    /**
     * Set call back for when the widget is tapped.
     * This can be used to link the widget to {@link SimulatorControlWidget}
     *
     * @param stateChangeCallback listener to handle call backs
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }
    //endregion

    //region private Methods

    private void checkAndUpdateUI() {

        addDisposable(widgetModel.isSimulatorActive()
                                 .lastOrError()
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribe(this::updateUI));
    }

    private void updateUI(Boolean isActive) {
        if (isActive) {
            foregroundImageView.setImageDrawable(simulatorActiveDrawable);
        } else {
            foregroundImageView.setImageDrawable(simulatorInactiveDrawable);
        }
    }

    private void initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.getRootView() != null) {
            View widgetView = this.getRootView().findViewById(stateChangeResourceId);
            if (widgetView instanceof SimulatorControlWidget) {
                setStateChangeCallback((SimulatorControlWidget) widgetView);
            }
        }
    }

    private void destroyListener() {
        stateChangeCallback = null;
    }

    private void initDefaults() {
        simulatorActiveDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_simulator_active);
        simulatorInactiveDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_simulator);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimulatorIndicatorWidget);
        stateChangeResourceId =
            typedArray.getResourceId(R.styleable.SimulatorIndicatorWidget_uxsdk_onStateChange, INVALID_RESOURCE);
        if (typedArray.getDrawable(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorActiveDrawable) != null) {
            simulatorActiveDrawable =
                typedArray.getDrawable(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorActiveDrawable);
        }
        if (typedArray.getDrawable(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorInactiveDrawable) != null) {
            simulatorInactiveDrawable =
                typedArray.getDrawable(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorInactiveDrawable);
        }
        setIconBackground(typedArray.getDrawable(R.styleable.SimulatorIndicatorWidget_uxsdk_iconBackground));
        typedArray.recycle();
    }
    //endregion

    //region customizations

    /**
     * Set simulator active icon resource
     *
     * @param resourceId resource id of simulator active icon
     */
    public void setSimulatorActiveDrawable(@DrawableRes int resourceId) {
        setSimulatorActiveDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set simulator active icon drawable
     *
     * @param drawable to be used as simulator active
     */
    public void setSimulatorActiveDrawable(@Nullable Drawable drawable) {
        simulatorActiveDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get the simulator active state drawable
     *
     * @return Drawable when the simulator is running
     */
    public Drawable getSimulatorActiveDrawable() {
        return simulatorActiveDrawable;
    }

    /**
     * Set simulator inactive icon resource
     *
     * @param resourceId resource id of simulator inactive resource
     */
    public void setSimulatorInactiveDrawable(@DrawableRes int resourceId) {
        setSimulatorInactiveDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set simulator inactive icon
     *
     * @param drawable Object to be used as inactive icon
     */
    public void setSimulatorInactiveDrawable(@Nullable Drawable drawable) {
        simulatorInactiveDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get the simulator inactive state drawable
     *
     * @return Drawable when the is simulator not running
     */
    @Nullable
    public Drawable getSimulatorInactiveDrawable() {
        return simulatorInactiveDrawable;
    }

    /**
     * Set background to icon resource
     *
     * @param resourceId resource id of background
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        setIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to the icon
     *
     * @param drawable Drawable to be used as background for icon
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get the icon's background
     *
     * @return Drawable for the icon's background
     */
    @Nullable
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    //endregion
}
