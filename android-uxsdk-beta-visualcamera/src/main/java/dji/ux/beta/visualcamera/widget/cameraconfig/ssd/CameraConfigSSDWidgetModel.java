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

package dji.ux.beta.visualcamera.widget.cameraconfig.ssd;

import androidx.annotation.NonNull;

import dji.common.camera.CameraSSDVideoLicense;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraConfigSSDWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigSSDWidgetModel extends WidgetModel {

    //region Constants
    /**
     * The available capture count is unknown.
     */
    protected static final long INVALID_AVAILABLE_CAPTURE_COUNT = -1L;
    /**
     * The available recording time is unknown.
     */
    protected static final int INVALID_AVAILABLE_RECORDING_TIME = -1;
    //endregion

    //region Fields
    private final DataProcessor<Boolean> isSSDSupportedProcessor;
    private final DataProcessor<ResolutionAndFrameRate> ssdVideoResolutionAndFrameRateProcessor;
    private final DataProcessor<Long> ssdRemainingSpaceInMBProcessor;
    private final DataProcessor<SettingsDefinitions.SSDClipFileName> ssdClipFileNameProcessor;
    private final DataProcessor<SettingsDefinitions.CameraMode> cameraModeProcessor;
    private final DataProcessor<SettingsDefinitions.ShootPhotoMode> shootPhotoModeProcessor;
    private final DataProcessor<SSDOperationState> ssdOperationStateProcessor;
    private final DataProcessor<Integer> ssdAvailableRecordingTimeInSecProcessor;
    private final DataProcessor<CameraSSDVideoLicense> activateSSDVideoLicenseProcessor;
    private final DataProcessor<SettingsDefinitions.SSDColor> ssdColorProcessor;
    private int cameraIndex;
    //endregion

    //region Constructor
    public CameraConfigSSDWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                      @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        isSSDSupportedProcessor = DataProcessor.create(false);
        ssdVideoResolutionAndFrameRateProcessor = DataProcessor.create(new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.UNKNOWN,
                SettingsDefinitions.VideoFrameRate.UNKNOWN));
        ssdRemainingSpaceInMBProcessor = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        ssdClipFileNameProcessor = DataProcessor.create(new SettingsDefinitions.SSDClipFileName("", 0, 0));
        cameraModeProcessor = DataProcessor.create(SettingsDefinitions.CameraMode.UNKNOWN);
        shootPhotoModeProcessor = DataProcessor.create(SettingsDefinitions.ShootPhotoMode.UNKNOWN);
        ssdOperationStateProcessor = DataProcessor.create(SSDOperationState.UNKNOWN);
        ssdAvailableRecordingTimeInSecProcessor = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        activateSSDVideoLicenseProcessor = DataProcessor.create(CameraSSDVideoLicense.Unknown);
        ssdColorProcessor = DataProcessor.create(SettingsDefinitions.SSDColor.UNKNOWN);
    }
    //endregion

    //region Data

    /**
     * Get the current index of the camera the widget model is reacting to
     *
     * @return current camera index
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    /**
     * Set the index of the camera for which the widget model should react
     *
     * @param cameraIndex camera index
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        restart();
    }

    /**
     * Get whether the products supports SSD storage.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> isSSDSupported() {
        return isSSDSupportedProcessor.toFlowable();
    }

    /**
     * Get the SSD license.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraSSDVideoLicense> getSSDLicense() {
        return activateSSDVideoLicenseProcessor.toFlowable();
    }

    /**
     * Get the remaining space in MB on the SSD.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Long> getSSDRemainingSpace() {
        return ssdRemainingSpaceInMBProcessor.toFlowable();
    }

    /**
     * Get the SSD clip file name.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.SSDClipFileName> getSSDClipName() {
        return ssdClipFileNameProcessor.toFlowable();
    }

    /**
     * Get the SSD resolution and frame rate.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<ResolutionAndFrameRate> getSSDResolutionAndFrameRate() {
        return ssdVideoResolutionAndFrameRateProcessor.toFlowable();
    }

    /**
     * Get the SSD operation state.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SSDOperationState> getSSDOperationState() {
        return ssdOperationStateProcessor.toFlowable();
    }

    /**
     * Get the SSD color.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.SSDColor> getSSDColor() {
        return ssdColorProcessor.toFlowable();
    }

    /**
     * Get the shoot photo mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.ShootPhotoMode> getShootPhotoMode() {
        return shootPhotoModeProcessor.toFlowable();
    }

    /**
     * Get the camera mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.CameraMode> getCameraMode() {
        return cameraModeProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey isSSDSupportedKey = CameraKey.create(CameraKey.IS_SSD_SUPPORTED, cameraIndex);
        DJIKey ssdResolutionAndFrameRateKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE, cameraIndex);
        DJIKey ssdRemainingSpaceKey = CameraKey.create(CameraKey.SSD_REMAINING_SPACE_IN_MB, cameraIndex);
        DJIKey ssdClipNameKey = CameraKey.create(CameraKey.SSD_CLIP_FILE_NAME, cameraIndex);
        DJIKey cameraModeKey = CameraKey.create(CameraKey.MODE, cameraIndex);
        DJIKey cameraShootPhotoModeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex);
        DJIKey ssdOperationStateKey = CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex);
        DJIKey ssdAvailableRecordingTimeKey = CameraKey.create(CameraKey.SSD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        DJIKey ssdLicenseKey = CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, cameraIndex);
        DJIKey ssdColorKey = CameraKey.create(CameraKey.SSD_COLOR, cameraIndex);

        bindDataProcessor(isSSDSupportedKey, isSSDSupportedProcessor);
        bindDataProcessor(ssdResolutionAndFrameRateKey, ssdVideoResolutionAndFrameRateProcessor);
        bindDataProcessor(ssdRemainingSpaceKey, ssdRemainingSpaceInMBProcessor);
        bindDataProcessor(ssdClipNameKey, ssdClipFileNameProcessor);
        bindDataProcessor(cameraModeKey, cameraModeProcessor);
        bindDataProcessor(cameraShootPhotoModeKey, shootPhotoModeProcessor);
        bindDataProcessor(ssdOperationStateKey, ssdOperationStateProcessor);
        bindDataProcessor(ssdAvailableRecordingTimeKey, ssdAvailableRecordingTimeInSecProcessor);
        bindDataProcessor(ssdLicenseKey, activateSSDVideoLicenseProcessor);
        bindDataProcessor(ssdColorKey, ssdColorProcessor);
    }

    @Override
    protected void inCleanup() {
        // Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        // Nothing to update
    }
    //endregion
}
