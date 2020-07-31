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

package dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch;

import androidx.annotation.NonNull;

import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProviderInterface;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Photo Video Switch Widget Model
 * <p>
 * Widget Model for the {@link PhotoVideoSwitchWidget} used to define the
 * underlying logic and communication
 */
public class PhotoVideoSwitchWidgetModel extends WidgetModel {

    //region fields
    private final DataProcessor<Boolean> isCameraConnectedDataProcessor;
    private final DataProcessor<Boolean> isRecordingDataProcessor;
    private final DataProcessor<Boolean> isShootingDataProcessor;
    private final DataProcessor<Boolean> isShootingIntervalDataProcessor;
    private final DataProcessor<Boolean> isShootingBurstDataProcessor;
    private final DataProcessor<Boolean> isShootingRawBurstDataProcessor;
    private final DataProcessor<Boolean> isShootingPanoramaDataProcessor;
    private final DataProcessor<CameraMode> cameraModeDataProcessor;
    private final DataProcessor<Boolean> isEnabledDataProcessor;
    private int cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
    private CameraKey cameraModeKey;
    private SchedulerProviderInterface schedulerProvider;
    //endregion

    //region lifecycle
    public PhotoVideoSwitchWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore,
                                       @NonNull SchedulerProviderInterface scheduletProviderInterface) {
        super(djiSdkModel, keyedStore);
        this.schedulerProvider = scheduletProviderInterface;
        isCameraConnectedDataProcessor = DataProcessor.create(false);
        isRecordingDataProcessor = DataProcessor.create(false);
        isShootingDataProcessor = DataProcessor.create(false);
        isShootingIntervalDataProcessor = DataProcessor.create(false);
        isShootingBurstDataProcessor = DataProcessor.create(false);
        isShootingRawBurstDataProcessor = DataProcessor.create(false);
        isShootingPanoramaDataProcessor = DataProcessor.create(false);
        cameraModeDataProcessor = DataProcessor.create(CameraMode.UNKNOWN);
        isEnabledDataProcessor = DataProcessor.create(false);
    }

    @Override
    protected void inSetup() {
        DJIKey cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex);
        bindDataProcessor(cameraConnectionKey, isCameraConnectedDataProcessor);
        CameraKey isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, cameraIndex);
        bindDataProcessor(isRecordingKey, isRecordingDataProcessor);
        CameraKey isShootingKey = CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex);
        bindDataProcessor(isShootingKey, isShootingDataProcessor);
        CameraKey isShootingIntervalKey = CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex);
        bindDataProcessor(isShootingIntervalKey, isShootingIntervalDataProcessor);
        CameraKey isShootingBurstKey = CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex);
        bindDataProcessor(isShootingBurstKey, isShootingBurstDataProcessor);
        CameraKey isShootingRawBurstKey = CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex);
        bindDataProcessor(isShootingRawBurstKey, isShootingRawBurstDataProcessor);
        CameraKey isShootingPanoramaKey = CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex);
        bindDataProcessor(isShootingPanoramaKey, isShootingPanoramaDataProcessor);
        bindDataProcessor(isShootingBurstKey, isShootingBurstDataProcessor);
        cameraModeKey = CameraKey.create(CameraKey.MODE, cameraIndex);
        bindDataProcessor(cameraModeKey, cameraModeDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // No Code
    }

    @Override
    protected void updateStates() {
        boolean isEnabled = productConnectionProcessor.getValue()
                && isCameraConnectedDataProcessor.getValue()
                && !isRecordingDataProcessor.getValue()
                && !isShootingDataProcessor.getValue()
                && !isShootingBurstDataProcessor.getValue()
                && !isShootingIntervalDataProcessor.getValue()
                && !isShootingRawBurstDataProcessor.getValue()
                && !isShootingPanoramaDataProcessor.getValue();

        isEnabledDataProcessor.onNext(isEnabled);
    }
    //endregion

    //region Data

    /**
     * Check if the widget should be enabled.
     *
     * @return Flowable with boolean value
     */
    public Flowable<Boolean> isEnabled() {
        return isEnabledDataProcessor.toFlowable();
    }

    /**
     * Get the current camera mode
     *
     * @return {@link CameraMode}
     */
    public Flowable<CameraMode> getCameraMode() {
        return cameraModeDataProcessor.toFlowable();
    }
    //endregion

    //region Actions

    /**
     * Toggle between photo mode and video mode
     *
     * @return Completable
     */
    public Completable toggleCameraMode() {
        CameraMode cameraMode = CameraMode.SHOOT_PHOTO;
        if (cameraModeDataProcessor.getValue() == CameraMode.SHOOT_PHOTO) {
            cameraMode = CameraMode.RECORD_VIDEO;
        }
        return djiSdkModel.setValue(cameraModeKey, cameraMode).subscribeOn(schedulerProvider.io());
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

    /**
     * Set camera index to which the model should react.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        restart();
    }
    //endregion
}
