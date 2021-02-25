/*
 * Copyright (c) 2018-2021 DJI
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

package dji.ux.beta.cameracore.widget.camerashuttersetting;

import androidx.annotation.NonNull;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.ExposureState;
import dji.common.camera.SettingsDefinitions.ShutterSpeed;
import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.LensModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Camera Shutter Setting Widget Model
 * <p>
 * Widget Model for the {@link CameraShutterSettingWidget} used to define the
 * underlying logic and communication
 */
public class CameraShutterSettingWidgetModel extends WidgetModel {

    //region Constants
    private static final String TAG = "ShutterSettingWidMod";
    //endregion

    private final DataProcessor<SettingsDefinitions.ExposureMode> exposureModeDataProcessor;
    private final DataProcessor<ShutterSpeed[]> shutterRangeArrayDataProcessor;
    private final DataProcessor<ShutterSpeed> shutterValueDataProcessor;
    private final DataProcessor<ExposureState> exposureStateDataProcessor;
    private final DataProcessor<ExposureSettings> exposureSettingsDataProcessor;
    private final DataProcessor<Boolean> changeShutterSpeedSupportedDataProcessor;
    private int cameraIndex = 0;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private CameraKey currentShutterKey;
    private LensModule lensModule;

    public CameraShutterSettingWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                           @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        exposureModeDataProcessor = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN);
        shutterRangeArrayDataProcessor = DataProcessor.create(new ShutterSpeed[0]);
        shutterValueDataProcessor = DataProcessor.create(ShutterSpeed.UNKNOWN);
        exposureStateDataProcessor = DataProcessor.create(ExposureState.UNKNOWN);
        exposureSettingsDataProcessor = DataProcessor.create(new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN));
        changeShutterSpeedSupportedDataProcessor = DataProcessor.create(false);
        lensModule = new LensModule();
        addModule(lensModule);
    }

    @Override
    protected void inSetup() {
        CameraKey exposureModeKey = lensModule.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value());
        bindDataProcessor(exposureModeKey, exposureModeDataProcessor);
        CameraKey shutterRangeKey = lensModule.createLensKey(CameraKey.SHUTTER_SPEED_RANGE, cameraIndex, lensType.value());
        bindDataProcessor(shutterRangeKey, shutterRangeArrayDataProcessor);
        CameraKey exposureSettingsKey = lensModule.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value());
        bindDataProcessor(exposureSettingsKey, exposureSettingsDataProcessor, newValue -> {
            ExposureSettings exposureParameters = ((ExposureSettings) newValue);
            shutterValueDataProcessor.onNext(exposureParameters.getShutterSpeed());
        });
        currentShutterKey = lensModule.createLensKey(CameraKey.SHUTTER_SPEED, cameraIndex, lensType.value());
        bindDataProcessor(currentShutterKey, shutterValueDataProcessor);
        CameraKey exposureStateKey = lensModule.createLensKey(CameraKey.EXPOSURE_STATE, cameraIndex, lensType.value());
        bindDataProcessor(exposureStateKey, exposureStateDataProcessor);
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(value -> {
                    if (value) {
                        restart();
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")));
    }

    @Override
    protected void inCleanup() {
        // Empty block
    }

    @Override
    protected void updateStates() {
        changeShutterSpeedSupportedDataProcessor.onNext((exposureModeDataProcessor.getValue() == SettingsDefinitions.ExposureMode.MANUAL
                || exposureModeDataProcessor.getValue() == SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY));
    }


    //region Actions

    /**
     * Set the aperture value to camera
     *
     * @param shutterSpeed instance of {@link ShutterSpeed}
     * @return Completable representing success and failure of action
     */
    public Completable setShutterSpeedValue(@NonNull ShutterSpeed shutterSpeed) {
        return djiSdkModel.setValue(currentShutterKey, shutterSpeed);
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
        lensModule.setCameraIndex(this, cameraIndex);
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
     * Is shutter speed change supported by the current camera and
     * camera mode combination
     *
     * @return Flowable with boolean value
     */
    public Flowable<Boolean> isChangeShutterSpeedSupported() {
        return changeShutterSpeedSupportedDataProcessor.toFlowable();
    }

    /**
     * Get the current shutter speed value
     *
     * @return Flowable with instance of {@link ShutterSpeed} representing current value
     */
    public Flowable<ShutterSpeed> getCurrentShutterSpeedValue() {
        return shutterValueDataProcessor.toFlowable();
    }

    /**
     * Get the current exposure state
     *
     * @return Flowable with the instance of {@link ExposureState}
     */
    public Flowable<ExposureState> getExposureState() {
        return exposureStateDataProcessor.toFlowable();
    }

    /**
     * Get the range of shutter speed values supported by the camera
     *
     * @return Flowable with Array of {@link ShutterSpeed}
     */
    public Flowable<ShutterSpeed[]> getShutterSpeedRange() {
        return shutterRangeArrayDataProcessor.toFlowable();
    }

    //endregion
}
