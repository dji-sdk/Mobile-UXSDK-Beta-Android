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

package dji.ux.beta.cameracore.widget.focusmode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.FocusMode;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
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
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Focus Mode Widget Model
 * <p>
 * Widget Model for the {@link FocusModeWidget} used to define the
 * underlying logic and communication
 */
public class FocusModeWidgetModel extends WidgetModel {

    //region Fields
    private static final String TAG = "FocusModeWidgetModel";
    private final DataProcessor<Boolean> isFocusModeSupportedDataProcessor;
    private final DataProcessor<FocusMode> focusModeDataProcessor;
    private final DataProcessor<Boolean> isAFCSupportedProcessor;
    private final DataProcessor<Boolean> isAFCEnabledKeyProcessor;
    private final DataProcessor<Boolean> isAFCEnabledProcessor;
    private final DataProcessor<SettingDefinitions.ControlMode> controlModeProcessor;
    private final ObservableInMemoryKeyedStore keyedStore;
    private final GlobalPreferencesInterface preferencesManager;
    private DJIKey focusModeKey;
    private UXKey controlModeKey;
    private int cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    //endregion

    //region Lifecycle
    public FocusModeWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                @NonNull ObservableInMemoryKeyedStore keyedStore,
                                @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        focusModeDataProcessor = DataProcessor.create(FocusMode.UNKNOWN);
        isAFCSupportedProcessor = DataProcessor.create(false);
        isAFCEnabledKeyProcessor = DataProcessor.create(false);
        isAFCEnabledProcessor = DataProcessor.create(false);
        isFocusModeSupportedDataProcessor = DataProcessor.create(false);
        controlModeProcessor = DataProcessor.create(SettingDefinitions.ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            isAFCEnabledKeyProcessor.onNext(preferencesManager.getAFCEnabled());
            updateAFCEnabledProcessor();
            controlModeProcessor.onNext(preferencesManager.getControlMode());
        }
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }

    @Override
    protected void inSetup() {
        focusModeKey = djiSdkModel.createLensKey(CameraKey.FOCUS_MODE, cameraIndex, lensType.value());
        bindDataProcessor(focusModeKey, focusModeDataProcessor);
        DJIKey isAFCSupportedKey = djiSdkModel.createLensKey(CameraKey.IS_AFC_SUPPORTED, cameraIndex, lensType.value());
        bindDataProcessor(isAFCSupportedKey, isAFCSupportedProcessor);
        UXKey afcEnabledKey = UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED);
        bindDataProcessor(afcEnabledKey, isAFCEnabledKeyProcessor);

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
        updateAFCEnabledProcessor();
    }

    private void updateAFCEnabledProcessor() {
        isAFCEnabledProcessor.onNext(isAFCEnabledKeyProcessor.getValue() && isAFCSupportedProcessor.getValue());
    }

    @Override
    protected void onProductConnectionChanged(boolean isConnected) {
        super.onProductConnectionChanged(isConnected);
        if (isConnected) {
            isFocusModeSupportedDataProcessor.onNext(djiSdkModel.isKeySupported(focusModeKey));
        } else {
            isFocusModeSupportedDataProcessor.onNext(false);
        }
    }

    //endregion

    //region Actions

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
     * Set the camera index for which the widget model should react to.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        restart();
    }

    /**
     * Get the current type of the lens the widget model is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return lensType;
    }

    /**
     * Set the type of the lens for which the widget model should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        this.lensType = lensType;
        restart();
    }

    /**
     * Switch between focus modes
     *
     * @return Completable representing the success/failure of set action
     */
    public Completable toggleFocusMode() {
        final FocusMode currentFocusMode = focusModeDataProcessor.getValue();
        final FocusMode nextFocusMode = getNextFocusMode(currentFocusMode);

        return djiSdkModel.setValue(focusModeKey, nextFocusMode)
                .doOnComplete(() -> onFocusModeUpdate(nextFocusMode))
                .doOnError(error -> focusModeDataProcessor.onNext(currentFocusMode));
    }

    private FocusMode getNextFocusMode(FocusMode currentFocusMode) {
        FocusMode nextFocusMode;
        if (currentFocusMode == FocusMode.MANUAL) {
            if (isAFCEnabledProcessor.getValue()) {
                nextFocusMode = FocusMode.AFC;
            } else {
                nextFocusMode = FocusMode.AUTO;
            }
        } else {
            nextFocusMode = FocusMode.MANUAL;
        }
        return nextFocusMode;
    }
    //endregion

    //region Data

    /**
     * Get the current {@link FocusMode}
     *
     * @return Flowable with instance of FocusMode
     */
    public Flowable<FocusMode> getFocusMode() {
        return focusModeDataProcessor.toFlowable();
    }

    /**
     * Check if Auto Focus Continuous(AFC) is enabled
     *
     * @return Flowable with boolean true - AFC enabled false - AFC not enabled
     */
    public Flowable<Boolean> isAFCEnabled() {
        return isAFCEnabledProcessor.toFlowable();
    }

    /**
     * Check if focus mode change is supported
     *
     * @return Flowable with boolean true - supported false - not supported
     */
    public Flowable<Boolean> isFocusModeChangeSupported() {
        return isFocusModeSupportedDataProcessor.toFlowable();
    }
    //endregion

    //region Helpers
    private void onFocusModeUpdate(FocusMode focusMode) {
        if (controlModeProcessor.getValue() == SettingDefinitions.ControlMode.SPOT_METER ||
                controlModeProcessor.getValue() == SettingDefinitions.ControlMode.CENTER_METER) {
            return;
        }

        switch (focusMode) {
            case AUTO:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.AUTO_FOCUS);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS)
                        .subscribe(() -> {
                            //do nothing
                        }, logErrorConsumer(TAG, "setControlModeAutoFocus: ")));
                break;
            case AFC:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE)
                        .subscribe(() -> {
                            //do nothing
                        }, logErrorConsumer(TAG, "setControlModeAutoFocusContinuous: ")));
                break;
            case MANUAL:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.MANUAL_FOCUS);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.MANUAL_FOCUS)
                        .subscribe(() -> {
                            //do nothing
                        }, logErrorConsumer(TAG, "setControlModeManualFocus: ")));
                break;
            default:
                break;
        }
    }
    //endregion
}
