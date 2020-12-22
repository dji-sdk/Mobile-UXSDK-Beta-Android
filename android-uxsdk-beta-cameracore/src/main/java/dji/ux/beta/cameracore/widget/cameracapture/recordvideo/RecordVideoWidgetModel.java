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

package dji.ux.beta.cameracore.widget.cameracapture.recordvideo;

import androidx.annotation.NonNull;

import dji.common.camera.CameraSSDVideoLicense;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Record Video Widget Model
 * <p>
 * Widget Model for {@link RecordVideoWidget} used to define underlying
 * logic and communication
 */
public class RecordVideoWidgetModel extends WidgetModel {

    //region Constants
    private static final int INVALID_AVAILABLE_RECORDING_TIME = -1;
    private static final int MAX_VIDEO_TIME_THRESHOLD_MINUTES = 29;
    private static final int SECONDS_PER_MIN = 60;
    //endregion

    //region Public data
    private final DataProcessor<CameraVideoStorageState> cameraVideoStorageState;
    private final DataProcessor<Boolean> isRecording;
    private final DataProcessor<String> cameraDisplayName;
    private final DataProcessor<Integer> recordingTimeInSeconds;
    private final DataProcessor<ResolutionAndFrameRate> recordedVideoParameters;
    //region Internal data
    private final DataProcessor<CameraSSDVideoLicense> cameraSSDVideoLicenseDataProcessor;
    //endregion
    private final DataProcessor<ResolutionAndFrameRate> nonSSDRecordedVideoParameters;
    private final DataProcessor<ResolutionAndFrameRate> ssdRecordedVideoParameters;
    private final DataProcessor<SettingsDefinitions.StorageLocation> storageLocation;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> sdCardState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> storageState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> innerStorageState;
    private final DataProcessor<SSDOperationState> ssdState;
    private final DataProcessor<Integer> sdCardRecordingTime;
    private final DataProcessor<Integer> innerStorageRecordingTime;
    private final DataProcessor<Integer> ssdRecordingTime;
    private final DataProcessor<RecordingState> recordingStateProcessor;
    private int cameraIndex;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private DJIKey stopVideoRecordingKey;
    private DJIKey startVideoRecordingKey;
    //endregion

    //region Constructor
    public RecordVideoWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                  @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        CameraSDVideoStorageState cameraSDVideoStorageState = new CameraSDVideoStorageState(
                SettingsDefinitions.StorageLocation.SDCARD,
                0,
                SettingsDefinitions.SDCardOperationState.NOT_INSERTED);
        cameraVideoStorageState = DataProcessor.create(cameraSDVideoStorageState);
        isRecording = DataProcessor.create(false);
        cameraDisplayName = DataProcessor.create("");
        recordingTimeInSeconds = DataProcessor.create(0);
        ResolutionAndFrameRate resolutionAndFrameRate = new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.UNKNOWN,
                SettingsDefinitions.VideoFrameRate.UNKNOWN);
        recordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        nonSSDRecordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        ssdRecordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        storageLocation = DataProcessor.create(SettingsDefinitions.StorageLocation.SDCARD);
        sdCardState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
        storageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.NORMAL);
        innerStorageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
        ssdState = DataProcessor.create(SSDOperationState.UNKNOWN);
        sdCardRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        innerStorageRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        ssdRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        cameraSSDVideoLicenseDataProcessor = DataProcessor.create(CameraSSDVideoLicense.Unknown);
        recordingStateProcessor = DataProcessor.create(RecordingState.UNKNOWN);
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        DJIKey isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, cameraIndex);
        bindDataProcessor(isRecordingKey, isRecording, newValue -> {
            if ((boolean) newValue) {
                recordingStateProcessor.onNext(RecordingState.RECORDING_IN_PROGRESS);
            } else {
                recordingStateProcessor.onNext(RecordingState.RECORDING_STOPPED);
            }
        });
        DJIKey recordingTimeKey = CameraKey.create(CameraKey.CURRENT_VIDEO_RECORDING_TIME_IN_SECONDS, cameraIndex);
        bindDataProcessor(recordingTimeKey, recordingTimeInSeconds);
        // Display name
        DJIKey cameraDisplayNameKey = CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex);
        bindDataProcessor(cameraDisplayNameKey, cameraDisplayName);
        // Storage
        DJIKey storageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex);
        DJIKey sdCardStateKey = CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex);
        DJIKey storageStateKey = CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex);
        DJIKey innerStorageStateKey = CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex);
        DJIKey ssdVideoLicenseKey = CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, cameraIndex);
        DJIKey ssdOperationStateKey = CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex);
        DJIKey sdCardRecordingTimeKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        DJIKey innerStorageRecordingTimeKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        DJIKey ssdRecordingTimeKey = CameraKey.create(CameraKey.SSD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        bindDataProcessor(storageLocationKey, storageLocation);
        bindDataProcessor(sdCardStateKey, sdCardState);
        bindDataProcessor(storageStateKey, storageState);
        bindDataProcessor(innerStorageStateKey, innerStorageState);
        bindDataProcessor(ssdOperationStateKey, ssdState);
        bindDataProcessor(sdCardRecordingTimeKey, sdCardRecordingTime);
        bindDataProcessor(innerStorageRecordingTimeKey, innerStorageRecordingTime);
        bindDataProcessor(ssdRecordingTimeKey, ssdRecordingTime);
        bindDataProcessor(ssdVideoLicenseKey, cameraSSDVideoLicenseDataProcessor);
        // Resolution and Frame Rates
        DJIKey nonSSDRecordedVideoParametersKey = djiSdkModel.createLensKey(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex, lensType.value());
        DJIKey ssdRecordedVideoParametersKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE, cameraIndex);
        bindDataProcessor(nonSSDRecordedVideoParametersKey, nonSSDRecordedVideoParameters);
        bindDataProcessor(ssdRecordedVideoParametersKey, ssdRecordedVideoParameters);
        startVideoRecordingKey = CameraKey.create(CameraKey.START_RECORD_VIDEO, cameraIndex);
        stopVideoRecordingKey = CameraKey.create(CameraKey.STOP_RECORD_VIDEO, cameraIndex);
    }

    @Override
    protected void inCleanup() {
        // Do nothing
    }

    @Override
    protected void updateStates() {
        updateVideoStorageState();
        if (isRecording.getValue()) {
            checkIsOverRecordTime(recordingTimeInSeconds.getValue());
        }
    }

    @Override
    protected void onProductConnectionChanged(boolean isConnected) {
        super.onProductConnectionChanged(isConnected);
        if (!isConnected) {
            recordingStateProcessor.onNext(RecordingState.UNKNOWN);
        }
    }

    //endregion

    //region Data

    /**
     * Get the current camera video storage state
     *
     * @return Flowable with {@link CameraVideoStorageState} instance
     */
    public Flowable<CameraVideoStorageState> getCameraVideoStorageState() {
        return cameraVideoStorageState.toFlowable();
    }

    /**
     * Check the recording state of the camera
     *
     * @return Flowable with {@link RecordingState} value
     */
    public Flowable<RecordingState> getRecordingState() {
        return recordingStateProcessor.toFlowable();
    }

    /**
     * Get the display name of the camera which the model is reacting to
     *
     * @return Flowable with string of camera name
     */
    public Flowable<String> getCameraDisplayName() {
        return cameraDisplayName.toFlowable();
    }

    /**
     * Get the duration of on going video recording in seconds
     *
     * @return Flowable with integer value representing seconds
     */
    public Flowable<Integer> getRecordingTimeInSeconds() {
        return recordingTimeInSeconds.toFlowable();
    }

    /**
     * Get the current index of the camera the widget model is reacting to
     *
     * @return current camera index
     */
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return SettingDefinitions.CameraIndex.find(cameraIndex);
    }

    /**
     * Set the camera index to which the model should react
     *
     * @param cameraIndex camera index to set
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
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
    //endregion

    //region Actions

    /**
     * Start video recording
     *
     * @return Completable to determine the status of the action
     */
    public Completable startRecordVideo() {
        if (isRecording.getValue() || !djiSdkModel.isAvailable()) {
            return Completable.complete();
        }

        return djiSdkModel.performAction(startVideoRecordingKey);
    }

    /**
     * Stop video recording
     *
     * @return Completable to determine the status of the action
     */
    public Completable stopRecordVideo() {
        if (!isRecording.getValue() || !djiSdkModel.isAvailable()) {
            return Completable.complete();
        }

        return djiSdkModel.performAction(stopVideoRecordingKey);
    }
    //endregion

    //region Helpers
    private void updateVideoStorageState() {
        SettingsDefinitions.StorageLocation currentStorageLocation = storageLocation.getValue();
        if (SettingsDefinitions.StorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        CameraSSDVideoLicense currentSSDVideoLicense = cameraSSDVideoLicenseDataProcessor.getValue();
        int availableRecordingTime = getAvailableRecordingTime(currentStorageLocation, currentSSDVideoLicense);
        if (availableRecordingTime == INVALID_AVAILABLE_RECORDING_TIME) {
            return;
        }

        CameraVideoStorageState newCameraVideoStorageState = null;
        if (currentSSDVideoLicense != CameraSSDVideoLicense.Unknown) {
            newCameraVideoStorageState = new CameraSSDVideoStorageState(SettingsDefinitions.StorageLocation.UNKNOWN, availableRecordingTime, ssdState.getValue());
        } else if (SettingsDefinitions.StorageLocation.SDCARD.equals(currentStorageLocation)) {
            if (!SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR.equals(sdCardState.getValue())) {
                newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, sdCardState.getValue());
            } else if (!SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR.equals(storageState.getValue())) {
                newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, storageState.getValue());
            }
            recordedVideoParameters.onNext(nonSSDRecordedVideoParameters.getValue());
        } else if (SettingsDefinitions.StorageLocation.INTERNAL_STORAGE.equals(currentStorageLocation)) {
            newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, innerStorageState.getValue());
        }

        if (newCameraVideoStorageState != null) {
            cameraVideoStorageState.onNext(newCameraVideoStorageState);
        }
    }

    private int getAvailableRecordingTime(SettingsDefinitions.StorageLocation storageLocation,
                                          CameraSSDVideoLicense ssdVideoLicense) {
        if (ssdVideoLicense != CameraSSDVideoLicense.Unknown) {
            return ssdRecordingTime.getValue();
        }

        switch (storageLocation) {
            case SDCARD:
                return sdCardRecordingTime.getValue();
            case INTERNAL_STORAGE:
                return innerStorageRecordingTime.getValue();
            case UNKNOWN:
            default:
                return INVALID_AVAILABLE_RECORDING_TIME;
        }
    }

    /**
     * Determine if the time is exceeded, and close the video in
     * {@link RecordVideoWidgetModel#MAX_VIDEO_TIME_THRESHOLD_MINUTES} minutes.
     *
     * @param recordTime The time to check.
     */
    private void checkIsOverRecordTime(int recordTime) {
        if (recordTime > (MAX_VIDEO_TIME_THRESHOLD_MINUTES * SECONDS_PER_MIN)) {
            stopRecordVideo();
        }
    }

    //endregion

    //region Classes

    /**
     * The recording state of the camera.
     */
    public enum RecordingState {

        /**
         * No product is connected, or the recording state is unknown.
         */
        UNKNOWN,
        /**
         * The camera is recording video.
         */
        RECORDING_IN_PROGRESS,

        /**
         * The camera is not recording video.
         */
        RECORDING_STOPPED
    }
    //endregion
}
