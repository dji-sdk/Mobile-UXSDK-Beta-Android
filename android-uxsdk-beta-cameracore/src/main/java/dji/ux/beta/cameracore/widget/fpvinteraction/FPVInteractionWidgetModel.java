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

import android.graphics.Point;
import android.graphics.PointF;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

import dji.common.camera.SettingsDefinitions.MeteringMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.DJIParamMinMaxCapability;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.GimbalKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKey;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.ProductUtil;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;
import dji.ux.beta.core.util.SettingDefinitions.ControlMode;
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex;

/**
 * Widget Model for the {@link FPVInteractionWidget} used to define
 * the underlying logic and communication
 */
public class FPVInteractionWidgetModel extends WidgetModel {

    //region Constants
    private static final int NUM_ROWS = 8;
    private static final int NUM_COLUMNS = 12;
    //endregion
    private final DataProcessor<ControlMode> controlModeProcessor;
    private final DataProcessor<MeteringMode> meteringModeProcessor;
    private final DataProcessor<Boolean> aeLockedProcessor;
    private final DataProcessor<Map> capabilitiesMapProcessor;
    private final GlobalPreferencesInterface preferencesManager;
    private final ObservableInMemoryKeyedStore keyedStore;
    //region Fields
    private int cameraIndex;
    private int gimbalIndex;
    private int lensIndex;
    private DJIKey focusTargetKey;
    private DJIKey meteringTargetKey;
    private DJIKey meteringModeKey;
    private Rotation.Builder builder;
    private UXKey controlModeKey;
    //endregion

    //region Constructor
    public FPVInteractionWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                     @NonNull ObservableInMemoryKeyedStore keyedStore,
                                     @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
        gimbalIndex = GimbalIndex.PORT.getIndex();
        meteringModeProcessor = DataProcessor.create(MeteringMode.UNKNOWN);
        controlModeProcessor = DataProcessor.create(ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            controlModeProcessor.onNext(preferencesManager.getControlMode());
        }
        aeLockedProcessor = DataProcessor.create(false);
        capabilitiesMapProcessor = DataProcessor.create(new HashMap());
        builder = new Rotation.Builder().mode(RotationMode.SPEED);
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        focusTargetKey = djiSdkModel.createLensKey(CameraKey.FOCUS_TARGET, cameraIndex, lensIndex);
        meteringTargetKey = djiSdkModel.createLensKey(CameraKey.SPOT_METERING_TARGET, cameraIndex, lensIndex);
        meteringModeKey = djiSdkModel.createLensKey(CameraKey.METERING_MODE, cameraIndex, lensIndex);
        bindDataProcessor(meteringModeKey, meteringModeProcessor, meteringMode -> setMeteringMode((MeteringMode) meteringMode));
        DJIKey aeLockedKey = djiSdkModel.createLensKey(CameraKey.AE_LOCK, cameraIndex, lensIndex);
        bindDataProcessor(aeLockedKey, aeLockedProcessor);
        DJIKey capabilitiesKey = GimbalKey.create(GimbalKey.CAPABILITIES, gimbalIndex);
        bindDataProcessor(capabilitiesKey, capabilitiesMapProcessor);
        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeProcessor);

        if (preferencesManager != null) {
            preferencesManager.setUpListener();
        }
    }

    @Override
    protected void inCleanup() {
        if (preferencesManager != null) {
            preferencesManager.cleanup();
        }
    }

    @Override
    protected void updateStates() {
        // do nothing
    }
    //endregion

    //region Helpers
    private void setMeteringMode(MeteringMode meteringMode) {
        if (meteringMode == MeteringMode.SPOT) {
            setControlMode(ControlMode.SPOT_METER);
        } else if (meteringMode == MeteringMode.CENTER) {
            setControlMode(ControlMode.CENTER_METER);
        }
    }
    //endregion

    //region Data

    /**
     * Get the camera index for which the model is reacting.
     *
     * @return current camera index.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return CameraIndex.find(cameraIndex);
    }

    /**
     * Set camera index to which the model should react.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        restart();
    }

    /**
     * Get the gimbal index for which the model is reacting.
     *
     * @return current gimbal index.
     */
    @Nullable
    public GimbalIndex getGimbalIndex() {
        return GimbalIndex.find(gimbalIndex);
    }

    /**
     * Set gimbal index to which the model should react.
     *
     * @param gimbalIndex index of the gimbal.
     */
    public void setGimbalIndex(@Nullable GimbalIndex gimbalIndex) {
        if (gimbalIndex != null) {
            this.gimbalIndex = gimbalIndex.getIndex();
        }
        restart();
    }

    /**
     * Get the current index of the lens the widget model is reacting to
     *
     * @return current lens index
     */
    public int getLensIndex() {
        return lensIndex;
    }

    /**
     * Set the index of the lens for which the widget model should react
     *
     * @param lensIndex lens index
     */
    public void setLensIndex(int lensIndex) {
        this.lensIndex = lensIndex;
        restart();
    }

    /**
     * Set the control mode.
     *
     * @param controlMode The control mode to set.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable setControlMode(@NonNull ControlMode controlMode) {
        if (preferencesManager != null) {
            preferencesManager.setControlMode(controlMode);
        }
        return keyedStore.setValue(controlModeKey, controlMode);
    }

    /**
     * Get the control mode.
     *
     * @return A Flowable that will emit the current control mode.
     */
    @NonNull
    public Flowable<ControlMode> getControlMode() {
        return controlModeProcessor.toFlowable();
    }

    /**
     * Get whether the automatic exposure is locked.
     *
     * @return A Flowable that will emit a boolean when the automatic exposure locked state changes.
     */
    @NonNull
    public Flowable<Boolean> isAeLocked() {
        return aeLockedProcessor.toFlowable();
    }
    //endregion

    //region Reactions to user input

    /**
     * Set the focus target to the location (targetX, targetY). This is a relative coordinate
     * represented by a percentage of the width and height of the widget.
     *
     * @param targetX The relative x coordinate of the focus target represented by a percentage of
     *                the width.
     * @param targetY The relative y coordinate of the focus target represented by a percentage of
     *                the height.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable updateFocusTarget(@FloatRange(from = 0, to = 1) float targetX,
                                         @FloatRange(from = 0, to = 1) float targetY) {
        return djiSdkModel.setValue(focusTargetKey, createPointF(targetX, targetY));
    }

    /**
     * Set the spot metering target to the location (targetX, targetY). This is a relative
     * coordinate represented by a percentage of the width and height of the widget.
     *
     * @param targetX The relative x coordinate of the spot metering target represented by a
     *                percentage of the width.
     * @param targetY The relative y coordinate of the spot metering target represented by a
     *                percentage of the height.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable updateMetering(@FloatRange(from = 0, to = 1) float targetX,
                                      @FloatRange(from = 0, to = 1) float targetY) {
        if (controlModeProcessor.getValue() == ControlMode.SPOT_METER) {
            //Converting target to position in grid
            int column = (int) (targetX * NUM_COLUMNS);
            int row = (int) (targetY * NUM_ROWS);
            if (column >= 0 && column < NUM_COLUMNS && row >= 0 && row < NUM_ROWS) {
                if (meteringModeProcessor.getValue() != MeteringMode.SPOT) {
                    return djiSdkModel.setValue(meteringModeKey, MeteringMode.SPOT)
                            .andThen(djiSdkModel.setValue(meteringTargetKey, createPoint(column, row)));
                } else {
                    return djiSdkModel.setValue(meteringTargetKey, createPoint(column, row));

                }
            }
        } else if (controlModeProcessor.getValue() == ControlMode.CENTER_METER) {
            return djiSdkModel.setValue(meteringModeKey, MeteringMode.CENTER);
        }
        return Completable.complete();
    }

    /**
     * Determine whether the gimbal is able to move in the yaw direction.
     *
     * @return `true` if the current product supports gimbal yaw rotation, `false` otherwise.
     */
    public boolean canRotateGimbalYaw() {
        Object capability = capabilitiesMapProcessor.getValue().get(CapabilityKey.ADJUST_YAW);
        return capability instanceof DJIParamMinMaxCapability
                && ((DJIParamMinMaxCapability) capability).isSupported()
                && !isPhantom4Series();
    }

    /**
     * Rotate the gimbal using {@link RotationMode#SPEED}.
     *
     * @param yaw   The amount to rotate the gimbal in the yaw direction.
     * @param pitch The amount to rotate the gimbal in the pitch direction.
     * @return Completable representing the success/failure of the set action.
     */
    public Completable rotateGimbalBySpeed(float yaw, float pitch) {
        if (yaw == 0) {
            yaw = Rotation.NO_ROTATION;
        }
        if (pitch == 0) {
            pitch = Rotation.NO_ROTATION;
        }
        Rotation r = builder.yaw(yaw).pitch(pitch).build();
        return djiSdkModel.performAction(GimbalKey.create(GimbalKey.ROTATE, gimbalIndex), r);
    }
    //endregion

    //region Unit test helpers

    /**
     * A wrapper for the {@link PointF} constructor so it can be mocked in unit tests.
     *
     * @return A PointF object.
     */
    @VisibleForTesting
    @NonNull
    protected PointF createPointF(float x, float y) {
        return new PointF(x, y);
    }

    /**
     * A wrapper for the {@link Point} constructor so it can be mocked in unit tests.
     *
     * @return A Point object.
     */
    @VisibleForTesting
    @NonNull
    protected Point createPoint(int x, int y) {
        return new Point(x, y);
    }

    /**
     * A wrapper for the {@link ProductUtil#isPhantom4Series()} method so it can be mocked
     * in unit tests.
     *
     * @return `true` if the connected product is in the Phantom 4 series. `false` if there is
     * no product connected or if the connected product is not in the Phantom 4 series.
     */
    @VisibleForTesting
    protected boolean isPhantom4Series() {
        return ProductUtil.isPhantom4Series();
    }
    //endregion
}
