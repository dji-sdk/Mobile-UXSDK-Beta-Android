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

package dji.ux.beta.cameracore.widget.fpvinteraction;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.AnimatorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.airlink.PhysicalSource;
import dji.common.camera.CameraVideoStreamSource;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.GlobalPreferencesManager;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;
import dji.ux.beta.core.util.SettingDefinitions.ControlMode;
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex;
import dji.ux.beta.core.widget.fpv.FPVWidget;

/**
 * This widget allows the user to interact with the FPVWidget.
 * <p>
 * When this widget is tapped, an icon will appear and the camera will either focus or perform spot
 * metering at the tapped area, depending on the current {@link ControlMode}.
 * <p>
 * When the widget is long pressed then dragged, the gimbal controls will appear and the aircraft's
 * gimbal will move. The speed at which the gimbal moves is based on the drag distance.
 */
public class FPVInteractionWidget extends ConstraintLayoutWidget implements View.OnTouchListener,
        FPVWidget.FPVStateChangeCallback {

    private static final String TAG = "FPVInteractionWidget";
    private static final int LONG_PRESS_TIME = 500; // Time in milliseconds
    private static final float DEFAULT_VELOCITY_FACTOR = 16.0f;
    private final Handler handler = new Handler();
    //region Fields
    private FocusTargetView focusTargetView;
    private ExposureMeterView exposureMeterView;
    private GimbalControlView gimbalControlView;
    private FPVInteractionWidgetModel widgetModel;
    private int relativeViewHeight, relativeViewWidth;
    private float oldAbsTargetX, oldAbsTargetY;
    private float absTargetX, absTargetY;
    private int viewHeight, viewWidth;
    private int widthOffset, heightOffset;
    private boolean touchFocusEnabled = true;
    private boolean spotMeteringEnabled = true;
    private boolean gimbalControlEnabled = true;
    private float firstX;
    private float firstY;
    private float moveDeltaX;
    private float moveDeltaY;
    private float velocityFactor;
    private Disposable gimbalMoveDisposable;
    private AtomicBoolean isInteractionEnabledAtomic;
    private String cameraName;

    private Runnable longPressed = new Runnable() {
        public void run() {
            gimbalControlView.show(absTargetX, absTargetY);
            firstX = absTargetX;
            firstY = absTargetY;
        }
    };
    //endregion

    //region Constructor
    public FPVInteractionWidget(@NonNull Context context) {
        super(context);
    }

    public FPVInteractionWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FPVInteractionWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_fpv_interaction, this);
        focusTargetView = findViewById(R.id.view_focus_target);
        exposureMeterView = findViewById(R.id.view_exposure_meter);
        gimbalControlView = findViewById(R.id.view_gimbal_control);
        setOnTouchListener(this);
        velocityFactor = DEFAULT_VELOCITY_FACTOR;
        isInteractionEnabledAtomic = new AtomicBoolean(true);
        cameraName = "";

        if (!isInEditMode()) {
            widgetModel = new FPVInteractionWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
        }

        if (attrs != null) {
            initAttributes(getContext(), attrs);
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
        addReaction(reactToUpdateVisibility());
    }

    @Override
    public void onCameraNameChange(@Nullable String cameraName) {
        this.cameraName = cameraName;
        updateVisibility();
    }

    @Override
    public void onCameraSideChange(@Nullable SettingDefinitions.CameraSide cameraSide) {
        if (cameraSide != null) {
            // set both gimbal and camera index using the camera side
            if (cameraSide == SettingDefinitions.CameraSide.PORT) {
                setGimbalIndex(SettingDefinitions.GimbalIndex.PORT);
                setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_0);
            } else if (cameraSide == SettingDefinitions.CameraSide.STARBOARD) {
                setGimbalIndex(SettingDefinitions.GimbalIndex.STARBOARD);
                setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_2);
            } else {
                setGimbalIndex(SettingDefinitions.GimbalIndex.TOP);
                setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_4);
            }
        }
    }

    @Override
    public void onStreamSourceChange(@Nullable CameraVideoStreamSource streamSource) {
        if (streamSource != null) {
            setLensIndex(CameraUtil.getLensIndex(streamSource, cameraName));
        }
    }

    @Override
    public void onFPVSizeChange(@Nullable FPVWidget.FPVSize fpvSize) {
        if (fpvSize != null) {
            adjustAspectRatio(fpvSize.getWidth(), fpvSize.getHeight());
        }
    }

    //endregion

    //region Reaction helpers
    private Disposable reactToUpdateVisibility() {
        return Flowable.combineLatest(widgetModel.getControlMode(), widgetModel.isAeLocked(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateViewVisibility(values.first, values.second),
                        logErrorConsumer(TAG, "reactToUpdateVisibility: "));
    }

    private void updateViewVisibility(ControlMode controlMode, boolean isAeLocked) {
        if (controlMode == ControlMode.SPOT_METER || controlMode == ControlMode.CENTER_METER) {
            if (isAeLocked) {
                exposureMeterView.setVisibility(View.GONE);
            } else if (controlMode == ControlMode.SPOT_METER) {
                exposureMeterView.setVisibility(View.VISIBLE);
            }
            focusTargetView.setVisibility(GONE);
        } else {
            exposureMeterView.setVisibility(GONE);
            focusTargetView.setVisibility(VISIBLE);
            focusTargetView.setControlMode(controlMode);
        }
    }
    //endregion

    //region User interaction
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        oldAbsTargetX = absTargetX;
        oldAbsTargetY = absTargetY;
        absTargetX = event.getX();
        absTargetY = event.getY();
        viewHeight = v.getHeight();
        viewWidth = v.getWidth();

        if (relativeViewWidth == 0 && relativeViewHeight == 0) {
            relativeViewWidth = viewWidth;
            relativeViewHeight = viewHeight;
        }

        // Calculate offset for different aspect ratios
        widthOffset = (viewWidth - relativeViewWidth) / 2;
        if (widthOffset < 0) {
            widthOffset = 0;
        }
        heightOffset = (viewHeight - relativeViewHeight) / 2;
        if (heightOffset < 0) {
            heightOffset = 0;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (absTargetX > widthOffset && viewWidth - absTargetX > widthOffset && gimbalControlEnabled) {
                    handler.postDelayed(longPressed, LONG_PRESS_TIME);
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPressed);
                if (gimbalControlView.isVisible()) {
                    if (absTargetX < widthOffset) {
                        absTargetX = widthOffset + 1;
                    } else if (viewWidth - absTargetX < widthOffset) {
                        absTargetX = viewWidth - widthOffset - 1;
                    }
                    gimbalControlView.hide();
                    stopGimbalRotation();
                } else {
                    float targetX = absTargetX / (float) viewWidth;
                    float targetY = absTargetY / (float) viewHeight;
                    addDisposable(Flowable.combineLatest(widgetModel.getControlMode(), widgetModel.isAeLocked(), Pair::new)
                            .firstOrError()
                            .observeOn(SchedulerProvider.ui())
                            .subscribe((Pair values) -> updateTarget((ControlMode) (values.first), (Boolean) (values.second), targetX, targetY),
                                    logErrorConsumer(TAG, "Update Target: ")));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (absTargetX < widthOffset) {
                    absTargetX = widthOffset + 1;
                } else if (viewWidth - absTargetX < widthOffset) {
                    absTargetX = viewWidth - widthOffset - 1;
                }

                if (absTargetY < heightOffset) {
                    absTargetY = heightOffset + 1;
                } else if (viewHeight - absTargetY < heightOffset) {
                    absTargetY = viewHeight - heightOffset - 1;
                }

                if (gimbalControlView.isVisible()) {
                    gimbalControlView.onMove(firstX, firstY, absTargetX, absTargetY, widgetModel.canRotateGimbalYaw());
                    rotateGimbal(firstX, firstY, absTargetX, absTargetY);
                }
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return {@link CameraIndex}
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
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
     * Get the index of the gimbal to which the widget is reacting
     *
     * @return {@link GimbalIndex}
     */
    @Nullable
    public GimbalIndex getGimbalIndex() {
        return widgetModel.getGimbalIndex();
    }

    /**
     * Set the index of gimbal to which the widget should react
     *
     * @param gimbalIndex index of the gimbal.
     */
    public void setGimbalIndex(@Nullable GimbalIndex gimbalIndex) {
        if (!isInEditMode()) {
            widgetModel.setGimbalIndex(gimbalIndex);
        }
    }

    /**
     * Get the index of the lens to which the widget is reacting
     *
     * @return current lens index
     */
    public int getLensIndex() {
        return widgetModel.getLensIndex();
    }

    /**
     * Set the index of lens to which the widget should react
     *
     * @param lensIndex index of the lens.
     */
    public void setLensIndex(int lensIndex) {
        if (!isInEditMode()) {
            widgetModel.setLensIndex(lensIndex);
        }
    }

    /**
     * Adjust the width and height of the interaction area. This should be called whenever the
     * size of the video feed changes. The interaction area will be centered within the view.
     *
     * @param width  The new width of the interaction area.
     * @param height The new height of the interaction area.
     */
    public void adjustAspectRatio(@IntRange(from = 1) int width, @IntRange(from = 1) int height) {
        if (width > 0 && height > 0) {
            relativeViewWidth = width;
            relativeViewHeight = height;
            redraw();
        }
    }
    //endregion

    //region Helpers
    private void updateTarget(ControlMode controlMode, boolean isAeLocked, float targetX, float targetY) {
        if (controlMode == ControlMode.SPOT_METER || controlMode == ControlMode.CENTER_METER) {
            if (spotMeteringEnabled && isInBounds() && !isAeLocked) {
                final ControlMode newControlMode = exposureMeterView.clickEvent(controlMode, absTargetX, absTargetY, viewWidth, viewHeight);
                addDisposable(widgetModel.setControlMode(newControlMode)
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(() -> {
                            //do nothing
                        }, logErrorConsumer(TAG, "updateTarget: ")));
                addDisposable(widgetModel.updateMetering(targetX, targetY)
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(() -> {
                            // do nothing
                        }, throwable -> onExposureMeterSetFail(newControlMode)));
            }
        } else if (touchFocusEnabled && isInBounds()) {
            focusTargetView.clickEvent(absTargetX, absTargetY);
            addDisposable(widgetModel.updateFocusTarget(targetX, targetY)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(() -> {
                        //do nothing
                    }, throwable -> onFocusTargetSetFail()));
        }
    }

    private boolean isInBounds() {
        return absTargetX > widthOffset
                && viewWidth - absTargetX > widthOffset
                && absTargetY > heightOffset
                && viewHeight - absTargetY > heightOffset;
    }

    private void onExposureMeterSetFail(ControlMode controlMode) {
        if (oldAbsTargetX > 0 && oldAbsTargetY > 0) {
            addDisposable(widgetModel
                    .setControlMode(exposureMeterView.clickEvent(controlMode,
                            oldAbsTargetX,
                            oldAbsTargetY,
                            viewWidth,
                            viewHeight))
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(() -> {
                        //do nothing
                    }, logErrorConsumer(TAG, "onExposureMeterSetFail: ")));
        }
    }

    private void onFocusTargetSetFail() {
        if (oldAbsTargetX > 0 && oldAbsTargetY > 0) {
            focusTargetView.clickEvent(oldAbsTargetX, oldAbsTargetY);
        }
    }

    private void redraw() {
        focusTargetView.removeImageBackground();
        exposureMeterView.removeImageBackground();
    }

    /**
     * Rotate the gimbal. The speed is determined by the distance from (firstX, firstY) to (x,y)
     * divided by the velocity factor. The gimbal will continue rotating until
     * {@link FPVInteractionWidget#stopGimbalRotation()} is called.
     * <p>
     * When this method is called after rotation has started, the new coordinates are used to
     * update the speed at which the gimbal rotates.
     *
     * @param firstX The x coordinate of the original point long pressed by the user.
     * @param firstY The y coordinate of the original point long pressed by the user.
     * @param x      The x coordinate the of the point the user dragged the gimbal controls to.
     * @param y      The y coordinate the of the point the user dragged the gimbal controls to.
     */
    private void rotateGimbal(float firstX, float firstY, float x, float y) {
        if (gimbalMoveDisposable == null) {
            toggleGimbalRotateBySpeed();
        }
        if (widgetModel.canRotateGimbalYaw()) {
            moveDeltaX = x - firstX;
        } else {
            moveDeltaX = 0;
        }
        moveDeltaY = y - firstY;
    }

    /**
     * Stop rotating the gimbal.
     */
    private void stopGimbalRotation() {
        if (gimbalMoveDisposable != null && !gimbalMoveDisposable.isDisposed()) {
            gimbalMoveDisposable.dispose();
            gimbalMoveDisposable = null;
        }
        this.moveDeltaX = 0;
        this.moveDeltaY = 0;
    }

    private void toggleGimbalRotateBySpeed() {
        gimbalMoveDisposable = Flowable.interval(50, TimeUnit.MILLISECONDS)
                .subscribeOn(SchedulerProvider.io())
                .subscribe(aLong -> {
                    float yawVelocity = moveDeltaX / velocityFactor;
                    float pitchVelocity = moveDeltaY / velocityFactor;

                    if (Math.abs(yawVelocity) >= 1 || Math.abs(pitchVelocity) >= 1) {
                        addDisposable(widgetModel.rotateGimbalBySpeed(yawVelocity, -pitchVelocity)
                                .observeOn(SchedulerProvider.ui())
                                .subscribe(() -> {
                                    //do nothing
                                }, logErrorConsumer(TAG, "rotate gimbal: ")));
                    }
                });
    }

    private void updateVisibility() {
        if (PhysicalSource.FPV_CAM.toString().equals(cameraName) || !isInteractionEnabledAtomic.get()) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }
    //endregion

    //region Customization
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FPVInteractionWidget);

        setCameraIndex(CameraIndex.find(typedArray.getInt(R.styleable.FPVInteractionWidget_uxsdk_cameraIndex, 0)));
        setGimbalIndex(GimbalIndex.find(typedArray.getInt(R.styleable.FPVInteractionWidget_uxsdk_gimbalIndex, 0)));
        setLensIndex(typedArray.getInt(R.styleable.FPVInteractionWidget_uxsdk_lensType, 0));

        Drawable manualFocusIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_manualFocusIcon);
        if (manualFocusIcon != null) {
            setManualFocusIcon(manualFocusIcon);
        }
        Drawable autoFocusIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_autoFocusIcon);
        if (autoFocusIcon != null) {
            setAutoFocusIcon(autoFocusIcon);
        }
        Drawable autoFocusContinuousIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_autoFocusContinuousIcon);
        if (autoFocusContinuousIcon != null) {
            setAutoFocusContinuousIcon(autoFocusContinuousIcon);
        }
        int focusTargetDuration = typedArray.getInt(R.styleable.FPVInteractionWidget_uxsdk_focusTargetDuration, FocusTargetView.DEFAULT_FOCUS_TARGET_DURATION);
        setFocusTargetDuration(focusTargetDuration);

        Drawable centerMeterIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_centerMeterIcon);
        if (centerMeterIcon != null) {
            setCenterMeterIcon(centerMeterIcon);
        }
        Drawable spotMeterIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_spotMeterIcon);
        if (spotMeterIcon != null) {
            setSpotMeterIcon(spotMeterIcon);
        }
        float centerMeterScaleX = typedArray.getFloat(R.styleable.FPVInteractionWidget_uxsdk_centerMeterScaleX, ExposureMeterView.DEFAULT_CENTER_METER_SCALE_X);
        setCenterMeterScaleX(centerMeterScaleX);
        float centerMeterScaleY = typedArray.getFloat(R.styleable.FPVInteractionWidget_uxsdk_centerMeterScaleY, ExposureMeterView.DEFAULT_CENTER_METER_SCALE_Y);
        setCenterMeterScaleY(centerMeterScaleY);

        Drawable gimbalPointIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_gimbalPointIcon);
        if (gimbalPointIcon != null) {
            setGimbalPointIcon(gimbalPointIcon);
        }
        Drawable gimbalMoveIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_gimbalMoveIcon);
        if (gimbalMoveIcon != null) {
            setGimbalMoveIcon(gimbalMoveIcon);
        }
        Drawable gimbalArrowIcon = typedArray.getDrawable(R.styleable.FPVInteractionWidget_uxsdk_gimbalArrowIcon);
        if (gimbalArrowIcon != null) {
            setGimbalArrowIcon(gimbalArrowIcon);
        }
        float gimbalVelocityFactor = typedArray.getFloat(R.styleable.FPVInteractionWidget_uxsdk_gimbalVelocityFactor, DEFAULT_VELOCITY_FACTOR);
        setGimbalVelocityFactor(gimbalVelocityFactor);
        boolean isVibrationEnabled = typedArray.getBoolean(R.styleable.FPVInteractionWidget_uxsdk_vibrationEnabled, true);
        setVibrationEnabled(isVibrationEnabled);
        int vibrationDuration = typedArray.getInt(R.styleable.FPVInteractionWidget_uxsdk_vibrationDuration, GimbalControlView.DEFAULT_VIBRATION_DURATION);
        setVibrationDuration(vibrationDuration);

        setInteractionEnabled(typedArray.getBoolean(R.styleable.FPVInteractionWidget_uxsdk_interactionEnabled, true));
        touchFocusEnabled = typedArray.getBoolean(R.styleable.FPVInteractionWidget_uxsdk_touchFocusEnabled, true);
        spotMeteringEnabled = typedArray.getBoolean(R.styleable.FPVInteractionWidget_uxsdk_spotMeteringEnabled, true);
        gimbalControlEnabled = typedArray.getBoolean(R.styleable.FPVInteractionWidget_uxsdk_gimbalControlEnabled, true);

        typedArray.recycle();
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_fpv_ratio);
    }

    /**
     * Get the drawable resource for the manual focus icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getManualFocusIcon() {
        return focusTargetView.getFocusTargetIcon(ControlMode.MANUAL_FOCUS);
    }

    /**
     * Set the resource ID for the manual focus icon.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setManualFocusIcon(@DrawableRes int resourceId) {
        setManualFocusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the manual focus icon.
     *
     * @param icon The drawable resource for the icon.
     */
    public void setManualFocusIcon(@Nullable Drawable icon) {
        focusTargetView.setFocusTargetIcon(ControlMode.MANUAL_FOCUS, icon);
    }

    /**
     * Get the drawable resource for the auto focus icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getAutoFocusIcon() {
        return focusTargetView.getFocusTargetIcon(ControlMode.AUTO_FOCUS);
    }

    /**
     * Set the resource ID for the auto focus icon.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setAutoFocusIcon(@DrawableRes int resourceId) {
        setAutoFocusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the auto focus icon.
     *
     * @param icon The drawable resource for the icon.
     */
    public void setAutoFocusIcon(@Nullable Drawable icon) {
        focusTargetView.setFocusTargetIcon(ControlMode.AUTO_FOCUS, icon);
    }

    /**
     * Get the drawable resource for the auto focus continuous icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getAutoFocusContinuousIcon() {
        return focusTargetView.getFocusTargetIcon(ControlMode.AUTO_FOCUS_CONTINUE);
    }

    /**
     * Set the resource ID for the auto focus continuous icon.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setAutoFocusContinuousIcon(@DrawableRes int resourceId) {
        setAutoFocusContinuousIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the auto focus continuous icon.
     *
     * @param icon The drawable resource for the icon.
     */
    public void setAutoFocusContinuousIcon(@Nullable Drawable icon) {
        focusTargetView.setFocusTargetIcon(ControlMode.AUTO_FOCUS_CONTINUE, icon);
    }

    /**
     * Sets the animator for the auto focus and auto focus continuous icons.
     *
     * @param animatorId The id of the animator, or 0 to remove the animation.
     */
    public void setAutoFocusAnimator(@AnimatorRes int animatorId) {
        focusTargetView.setAutoFocusAnimator(animatorId);
    }

    /**
     * Gets the duration in milliseconds that the focus target will stay on the screen before
     * disappearing. This is the amount of time after the animation completes, if any.
     *
     * @return The number of milliseconds the focus target will stay on the screen.
     */
    public long getFocusTargetDuration() {
        return focusTargetView.getFocusTargetDuration();
    }

    /**
     * Sets the duration in milliseconds that the focus target will stay on the screen before
     * disappearing. This is the amount of time after the animation completes, if any.
     *
     * @param duration The number of milliseconds the focus target will stay on the screen.
     */
    public void setFocusTargetDuration(long duration) {
        focusTargetView.setFocusTargetDuration(duration);
    }

    /**
     * Get the drawable resource for the center meter icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getCenterMeterIcon() {
        return exposureMeterView.getCenterMeterIcon();
    }

    /**
     * Set the resource ID for the center meter icon.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setCenterMeterIcon(@DrawableRes int resourceId) {
        setCenterMeterIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the center meter icon.
     *
     * @param centerMeterIcon The drawable resource for the icon.
     */
    public void setCenterMeterIcon(@Nullable Drawable centerMeterIcon) {
        exposureMeterView.setCenterMeterIcon(centerMeterIcon);
    }

    /**
     * Get the drawable resource for the spot meter icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getSpotMeterIcon() {
        return exposureMeterView.getSpotMeterIcon();
    }

    /**
     * Set the resource ID for the spot meter icon.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setSpotMeterIcon(@DrawableRes int resourceId) {
        setSpotMeterIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the spot meter icon.
     *
     * @param spotMeterIcon The drawable resource for the icon.
     */
    public void setSpotMeterIcon(@Nullable Drawable spotMeterIcon) {
        exposureMeterView.setSpotMeterIcon(spotMeterIcon);
    }

    /**
     * Gets the scaleX of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as wide as the spot meter icon, the scaleX would be 2.
     *
     * @return The scaleX of the center meter icon
     */
    public float getCenterMeterScaleX() {
        return exposureMeterView.getCenterMeterScaleX();
    }

    /**
     * Sets the scaleX of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as wide as the spot meter icon, the scaleX would be 2.
     *
     * @param centerMeterScaleX The scaleX of the center meter icon
     */
    public void setCenterMeterScaleX(float centerMeterScaleX) {
        exposureMeterView.setCenterMeterScaleX(centerMeterScaleX);
    }

    /**
     * Gets the scaleY of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as tall as the spot meter icon, the scaleY would be 2.
     *
     * @return The scaleY of the center meter icon
     */
    public float getCenterMeterScaleY() {
        return exposureMeterView.getCenterMeterScaleY();
    }

    /**
     * Sets the scaleY of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as tall as the spot meter icon, the scaleY would be 2.
     *
     * @param centerMeterScaleY The scaleY of the center meter icon
     */
    public void setCenterMeterScaleY(float centerMeterScaleY) {
        exposureMeterView.setCenterMeterScaleY(centerMeterScaleY);
    }

    /**
     * Get the drawable resource for the icon that represents the point at which the gimbal
     * started.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalPointIcon() {
        return gimbalControlView.getGimbalPointIcon();
    }

    /**
     * Set the resource ID for the icon that represents the point at which the gimbal started.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setGimbalPointIcon(@DrawableRes int resourceId) {
        setGimbalPointIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the icon that represents the point at which the gimbal
     * started.
     *
     * @param gimbalPointIcon The drawable resource for the icon.
     */
    public void setGimbalPointIcon(@Nullable Drawable gimbalPointIcon) {
        gimbalControlView.setGimbalPointIcon(gimbalPointIcon);
    }

    /**
     * Get the drawable resource for the icon that represents the point towards which the gimbal
     * is moving.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalMoveIcon() {
        return gimbalControlView.getGimbalMoveIcon();
    }

    /**
     * Set the resource ID for the icon that represents the point towards which the gimbal
     * is moving.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setGimbalMoveIcon(@DrawableRes int resourceId) {
        setGimbalMoveIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the icon that represents the point towards which the gimbal
     * is moving.
     *
     * @param gimbalMoveIcon The drawable resource for the icon.
     */
    public void setGimbalMoveIcon(@Nullable Drawable gimbalMoveIcon) {
        gimbalControlView.setGimbalMoveIcon(gimbalMoveIcon);
    }

    /**
     * Get the drawable resource for the icon that represents the direction the gimbal is moving.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalArrowIcon() {
        return gimbalControlView.getGimbalArrowIcon();
    }

    /**
     * Set the resource ID for the icon that represents the direction the gimbal is moving.
     *
     * @param resourceId The resource ID of the icon.
     */
    public void setGimbalArrowIcon(@DrawableRes int resourceId) {
        setGimbalArrowIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the icon that represents the direction the gimbal is moving.
     *
     * @param gimbalArrowIcon The drawable resource for the icon.
     */
    public void setGimbalArrowIcon(@Nullable Drawable gimbalArrowIcon) {
        gimbalControlView.setGimbalArrowIcon(gimbalArrowIcon);
    }

    /**
     * Gets the velocity factor of the gimbal control view. The distance between the original point
     * and the current touch location is divided by this number to calculate the speed of the
     * gimbal. Therefore a higher velocity factor will result in a lower gimbal rotation speed.
     * The default is 16.
     *
     * @return The velocity factor of the gimbal control view
     */
    public float getGimbalVelocityFactor() {
        return velocityFactor;
    }

    /**
     * Sets the velocity factor of the gimbal control view. The distance between the original point
     * and the current touch location is divided by this number to calculate the speed of the
     * gimbal. Therefore a higher velocity factor will result in a lower gimbal rotation speed.
     * The default is 16.
     *
     * @param velocityFactor The velocity factor of the gimbal control view
     */
    public void setGimbalVelocityFactor(@FloatRange(from = 1) float velocityFactor) {
        this.velocityFactor = velocityFactor;
    }

    /**
     * Get whether the device will vibrate when the gimbal control view appears.
     *
     * @return `true` if vibration is enabled, `false` otherwise.
     */
    public boolean isVibrationEnabled() {
        return gimbalControlView.isVibrationEnabled();
    }

    /**
     * Set whether the device will vibrate when the gimbal control view appears.
     *
     * @param vibrationEnabled `true` if vibration is enabled, `false` otherwise.
     */
    public void setVibrationEnabled(boolean vibrationEnabled) {
        gimbalControlView.setVibrationEnabled(vibrationEnabled);
    }

    /**
     * Get the duration of the vibration in milliseconds when the gimbal control view appears.
     *
     * @return The duration of the vibration in milliseconds.
     */
    public int getVibrationDuration() {
        return gimbalControlView.getVibrationDuration();
    }

    /**
     * Set the duration of the vibration in milliseconds when the gimbal control view appears.
     *
     * @param vibrationDuration The duration of the vibration in milliseconds.
     */
    public void setVibrationDuration(@IntRange(from = 0) int vibrationDuration) {
        gimbalControlView.setVibrationDuration(vibrationDuration);
    }

    /**
     * Get whether interaction is enabled.
     *
     * @return `true` if enabled, `false` if disabled.
     */
    public boolean isInteractionEnabled() {
        return isInteractionEnabledAtomic.get();
    }

    /**
     * Set whether interaction is enabled.
     *
     * @param isInteractionEnabled `true` to enable, `false` to disable.
     */
    public void setInteractionEnabled(boolean isInteractionEnabled) {
        isInteractionEnabledAtomic.set(isInteractionEnabled);
        updateVisibility();
    }

    /**
     * Method to check if Touch to Focus is enabled.
     *
     * @return `true` if enabled, `false` if disabled.
     */
    public boolean isTouchFocusEnabled() {
        return touchFocusEnabled;
    }

    /**
     * Enable or disable Touch to Focus by this method. Enabled by default.
     *
     * @param isTouchFocusEnabled `true` to enable, `false` to disable.
     */
    public void setTouchFocusEnabled(boolean isTouchFocusEnabled) {
        this.touchFocusEnabled = isTouchFocusEnabled;
    }

    /**
     * Method to check if spot metering using touch is enabled.
     *
     * @return `true` if enabled, `false` if disabled.
     */
    public boolean isSpotMeteringEnabled() {
        return spotMeteringEnabled;
    }

    /**
     * Enable or disable spot metering by this method. Enabled by default.
     *
     * @param isSpotMeteringEnabled `true` to enable, `false` to disable.
     */
    public void setSpotMeteringEnabled(boolean isSpotMeteringEnabled) {
        this.spotMeteringEnabled = isSpotMeteringEnabled;
    }

    /**
     * Method to check if gimbal control using touch is enabled.
     *
     * @return `true` if enabled, `false` if disabled.
     */
    public boolean isGimbalControlEnabled() {
        return gimbalControlEnabled;
    }

    /**
     * Enable or disable gimbal control by this method. Enabled by default.
     *
     * @param isGimbalControlEnabled `true` to enable, `false` to disable.
     */
    public void setGimbalControlEnabled(boolean isGimbalControlEnabled) {
        this.gimbalControlEnabled = isGimbalControlEnabled;
    }
    //endregion
}
