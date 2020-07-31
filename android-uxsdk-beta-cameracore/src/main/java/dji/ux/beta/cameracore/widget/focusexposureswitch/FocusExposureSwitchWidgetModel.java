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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.SettingsDefinitions.FocusMode;
import dji.common.camera.SettingsDefinitions.MeteringMode;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.log.DJILog;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.GlobalPreferencesInterface;
import dji.ux.beta.core.base.SchedulerProviderInterface;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.base.uxsdkkeys.GlobalPreferenceKeys;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.base.uxsdkkeys.UXKey;
import dji.ux.beta.core.base.uxsdkkeys.UXKeys;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;
import dji.ux.beta.core.util.SettingDefinitions.ControlMode;

/**
 * Focus Exposure Switch Widget Model
 * <p>
 * Widget Model for the {@link FocusExposureSwitchWidget} used to define the
 * underlying logic and communication
 */
public class FocusExposureSwitchWidgetModel extends WidgetModel {

    //region fields
    private static final String TAG = "FocusExpoSwitchWidMod";
    private final DataProcessor<FocusMode> focusModeDataProcessor;
    private final DataProcessor<MeteringMode> meteringModeDataProcessor;
    private final DataProcessor<ControlMode> controlModeDataProcessor;
    private final ObservableInMemoryKeyedStore keyedStore;
    private final GlobalPreferencesInterface preferencesManager;
    private DJIKey meteringModeKey;
    private UXKey controlModeKey;
    private int cameraIndex;
    private SchedulerProviderInterface schedulerProvider;
    //endregion

    //region lifecycle
    public FocusExposureSwitchWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore,
                                          @Nullable GlobalPreferencesInterface preferencesManager,
                                          @Nullable SchedulerProviderInterface schedulerProvider) {
        super(djiSdkModel, keyedStore);
        DJILog.d(TAG, "In CONSTRUCTOR");
        this.schedulerProvider = schedulerProvider;
        focusModeDataProcessor = DataProcessor.create(FocusMode.UNKNOWN);
        meteringModeDataProcessor = DataProcessor.create(MeteringMode.UNKNOWN);
        controlModeDataProcessor = DataProcessor.create(ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            controlModeDataProcessor.onNext(preferencesManager.getControlMode());
        }
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }


    @Override
    protected void inSetup() {
        DJILog.d(TAG, "In inSetup");
        meteringModeKey = CameraKey.create(CameraKey.METERING_MODE, cameraIndex);
        DJIKey focusModeKey = CameraKey.create(CameraKey.FOCUS_MODE, cameraIndex);
        bindDataProcessor(focusModeKey, focusModeDataProcessor);
        bindDataProcessor(meteringModeKey, meteringModeDataProcessor);

        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeDataProcessor);

        if (preferencesManager != null) {
            preferencesManager.setUpListener();
        }
    }

    @Override
    protected void inCleanup() {
        DJILog.d(TAG, "In inCleanup");
        if (preferencesManager != null) {
            preferencesManager.cleanup();
        }
    }

    @Override
    protected void updateStates() {
        DJILog.d(TAG, "In updateStates");
        updateFocusMode();
    }
    //endregion

    //region Data

    /**
     * Get control mode
     *
     * @return Flowable with instance of {@link ControlMode}
     */
    public Flowable<ControlMode> getControlMode() {
        return controlModeDataProcessor.toFlowable();
    }


    /**
     * Get the camera index for which the model is reacting.
     *
     * @return current camera index.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return CameraIndex.find(cameraIndex);
    }

    //endregion

    //region Actions

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
     * Switch between exposure/metering mode and focus mode
     *
     * @return Completable representing the success/failure of the set action.
     */
    public Completable switchControlMode() {
        DJILog.d(TAG, "switchControlMode");
        ControlMode currentControlMode = controlModeDataProcessor.getValue();
        if (currentControlMode == ControlMode.SPOT_METER || currentControlMode == ControlMode.CENTER_METER) {
            return setFocusMode();
        } else {
            return setMeteringMode();
        }
    }
    //endregion

    // region private methods
    private void updateFocusMode() {
        DJILog.d(TAG, "updateFocusMode");
        ControlMode currentControlMode = controlModeDataProcessor.getValue();
        if (currentControlMode != ControlMode.SPOT_METER && currentControlMode != ControlMode.CENTER_METER) {
            setFocusMode();
        }
    }

    private Completable setMeteringMode() {
        return djiSdkModel.setValue(meteringModeKey, MeteringMode.SPOT)
                .subscribeOn(schedulerProvider.io())
                .doOnComplete(
                        () -> {
                            DJILog.d(TAG, "setMeteringMode success");
                            preferencesManager.setControlMode(ControlMode.SPOT_METER);
                            addDisposable(keyedStore.setValue(controlModeKey, ControlMode.SPOT_METER)
                                    .subscribe(() -> {
                                        //do nothing
                                    }, logErrorConsumer(TAG, "setMeteringMode: ")));
                            DJILog.d(TAG, "Success");
                        }).doOnError(
                        error -> {
                            DJILog.d(TAG, "setMeteringMode error");
                            setFocusMode();
                            DJILog.d(TAG, "Fail " + error.toString());
                        }
                );
    }

    private Completable setFocusMode() {
        DJILog.d(TAG, "In setFocusMode ControlModeKey is null " + (controlModeKey == null));
        if (controlModeKey != null) {
            DJILog.d(TAG, "In setFocusMode ControlModeKey Value Type " + controlModeKey.getValueType());
        }
        if (focusModeDataProcessor.getValue() == FocusMode.MANUAL) {
            preferencesManager.setControlMode(ControlMode.MANUAL_FOCUS);
            return keyedStore.setValue(controlModeKey, ControlMode.MANUAL_FOCUS).subscribeOn(schedulerProvider.io());
        } else if (focusModeDataProcessor.getValue() == FocusMode.AFC) {
            preferencesManager.setControlMode(ControlMode.AUTO_FOCUS_CONTINUE);
            return keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS_CONTINUE).subscribeOn(schedulerProvider.io());
        } else {
            preferencesManager.setControlMode(ControlMode.AUTO_FOCUS);
            return keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS).subscribeOn(schedulerProvider.io());
        }
    }
    //endregion


}
