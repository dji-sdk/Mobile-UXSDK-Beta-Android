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

package dji.ux.beta.visualcamera.widget.cameraconfig.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.FlatCameraModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Widget Model for the {@link CameraConfigStorageWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigStorageWidgetModel extends WidgetModel {

    //region Constants
    /**
     * The available capture count is unknown.
     */
    protected static final long INVALID_AVAILABLE_CAPTURE_COUNT_LONG = -1L;
    /**
     * The available recording time is unknown.
     */
    protected static final int INVALID_AVAILABLE_RECORDING_TIME = -1;
    //endregion

    //region Internal Data
    private final DataProcessor<SettingsDefinitions.StorageLocation> storageLocationProcessor;
    private final DataProcessor<ResolutionAndFrameRate> resolutionAndFrameRateProcessor;
    private final DataProcessor<SettingsDefinitions.PhotoFileFormat> photoFileFormatProcessor;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> sdCardState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> storageState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> innerStorageState;
    private final DataProcessor<Long> sdAvailableCaptureCount;
    private final DataProcessor<Long> innerStorageAvailableCaptureCount;
    private final DataProcessor<Integer> sdCardRecordingTime;
    private final DataProcessor<Integer> innerStorageRecordingTime;
    private final DataProcessor<SettingsDefinitions.CameraColor> cameraColorProcessor;
    //region Public Data
    private final DataProcessor<ImageFormat> imageFormatProcessor;
    //endregion
    private final DataProcessor<CameraStorageState> cameraStorageState;
    private int cameraIndex;
    private SettingsDefinitions.LensType lensType = SettingsDefinitions.LensType.ZOOM;
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Constructor
    public CameraConfigStorageWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        storageLocationProcessor = DataProcessor.create(SettingsDefinitions.StorageLocation.UNKNOWN);
        resolutionAndFrameRateProcessor = DataProcessor.create(new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.UNKNOWN,
                SettingsDefinitions.VideoFrameRate.UNKNOWN));
        photoFileFormatProcessor = DataProcessor.create(SettingsDefinitions.PhotoFileFormat.UNKNOWN);
        sdCardState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN);
        storageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN);
        innerStorageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN);
        sdAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT_LONG);
        innerStorageAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT_LONG);
        sdCardRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        innerStorageRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        cameraColorProcessor = DataProcessor.create(SettingsDefinitions.CameraColor.UNKNOWN);

        imageFormatProcessor = DataProcessor.create(new ImageFormat(
                SettingsDefinitions.CameraMode.UNKNOWN,
                SettingsDefinitions.PhotoFileFormat.UNKNOWN,
                SettingsDefinitions.VideoResolution.UNKNOWN,
                SettingsDefinitions.VideoFrameRate.UNKNOWN));
        CameraStorageState cameraSSDStorageState = new CameraStorageState(
                SettingsDefinitions.CameraMode.UNKNOWN,
                SettingsDefinitions.StorageLocation.UNKNOWN,
                SettingsDefinitions.SDCardOperationState.UNKNOWN,
                INVALID_AVAILABLE_CAPTURE_COUNT_LONG,
                INVALID_AVAILABLE_RECORDING_TIME);
        cameraStorageState = DataProcessor.create(cameraSSDStorageState);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
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
        flatCameraModule.setCameraIndex(cameraIndex);
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
     * Get the current image format.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<ImageFormat> getImageFormat() {
        return imageFormatProcessor.toFlowable();
    }

    /**
     * Get the current camera photo storage location.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraStorageState> getCameraStorageState() {
        return cameraStorageState.toFlowable();
    }

    /**
     * Get the current camera color.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.CameraColor> getCameraColor() {
        return cameraColorProcessor.toFlowable();
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        DJIKey storageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex);
        DJIKey resolutionAndFrameRateKey = djiSdkModel.createLensKey(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex, lensType.value());
        DJIKey photoFileFormatKey = djiSdkModel.createLensKey(CameraKey.PHOTO_FILE_FORMAT, cameraIndex, lensType.value());
        DJIKey sdCardStateKey = CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex);
        DJIKey storageStateKey = CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex);
        DJIKey innerStorageStateKey = CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex);
        DJIKey sdAvailableCaptureCountKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex);
        DJIKey innerStorageAvailableCaptureCountKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex);
        DJIKey sdCardRecordingTimeKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        DJIKey innerStorageRecordingTimeKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex);
        DJIKey cameraColorKey = CameraKey.create(CameraKey.CAMERA_COLOR, cameraIndex);

        bindDataProcessor(storageLocationKey, storageLocationProcessor);
        bindDataProcessor(resolutionAndFrameRateKey, resolutionAndFrameRateProcessor);
        bindDataProcessor(photoFileFormatKey, photoFileFormatProcessor);
        bindDataProcessor(sdCardStateKey, sdCardState);
        bindDataProcessor(storageStateKey, storageState);
        bindDataProcessor(innerStorageStateKey, innerStorageState);
        bindDataProcessor(sdAvailableCaptureCountKey, sdAvailableCaptureCount);
        bindDataProcessor(innerStorageAvailableCaptureCountKey, innerStorageAvailableCaptureCount);
        bindDataProcessor(sdCardRecordingTimeKey, sdCardRecordingTime);
        bindDataProcessor(innerStorageRecordingTimeKey, innerStorageRecordingTime);
        bindDataProcessor(cameraColorKey, cameraColorProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        imageFormatProcessor.onNext(new ImageFormat(flatCameraModule.getCameraModeDataProcessor().getValue(),
                photoFileFormatProcessor.getValue(),
                resolutionAndFrameRateProcessor.getValue().getResolution(),
                resolutionAndFrameRateProcessor.getValue().getFrameRate()));
        updateCameraStorageState();
    }
    //endregion

    //region Helpers
    private void updateCameraStorageState() {
        SettingsDefinitions.StorageLocation currentStorageLocation = storageLocationProcessor.getValue();
        if (SettingsDefinitions.StorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        SettingsDefinitions.SDCardOperationState sdCardOperationState = null;
        if (SettingsDefinitions.StorageLocation.SDCARD.equals(currentStorageLocation)) {
            if (!SettingsDefinitions.SDCardOperationState.UNKNOWN.equals(sdCardState.getValue())) {
                sdCardOperationState = sdCardState.getValue();
            } else if (!SettingsDefinitions.SDCardOperationState.UNKNOWN.equals(storageState.getValue())) {
                sdCardOperationState = storageState.getValue();
            }
        } else if (SettingsDefinitions.StorageLocation.INTERNAL_STORAGE.equals(currentStorageLocation)
                && !SettingsDefinitions.SDCardOperationState.UNKNOWN.equals(innerStorageState.getValue())) {
            sdCardOperationState = innerStorageState.getValue();
        }

        if (sdCardOperationState != null) {
            cameraStorageState.onNext(new CameraStorageState(flatCameraModule.getCameraModeDataProcessor().getValue(),
                    currentStorageLocation,
                    sdCardOperationState,
                    getAvailableCaptureCount(currentStorageLocation),
                    getAvailableRecordingTime(currentStorageLocation)));
        }
    }

    private long getAvailableCaptureCount(SettingsDefinitions.StorageLocation storageLocation) {
        switch (storageLocation) {
            case SDCARD:
                return sdAvailableCaptureCount.getValue();
            case INTERNAL_STORAGE:
                return innerStorageAvailableCaptureCount.getValue();
            case UNKNOWN:
            default:
                return INVALID_AVAILABLE_CAPTURE_COUNT_LONG;
        }
    }

    private int getAvailableRecordingTime(SettingsDefinitions.StorageLocation storageLocation) {
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
    //endregion

    //region States

    /**
     * The image format info
     */
    public static class ImageFormat {
        private SettingsDefinitions.CameraMode cameraMode;
        private SettingsDefinitions.PhotoFileFormat photoFileFormat;
        private SettingsDefinitions.VideoResolution resolution;
        private SettingsDefinitions.VideoFrameRate frameRate;

        protected ImageFormat(@Nullable SettingsDefinitions.CameraMode cameraMode,
                            @Nullable SettingsDefinitions.PhotoFileFormat photoFileFormat,
                            @Nullable SettingsDefinitions.VideoResolution resolution,
                            @Nullable SettingsDefinitions.VideoFrameRate frameRate) {
            this.cameraMode = cameraMode;
            this.photoFileFormat = photoFileFormat;
            this.resolution = resolution;
            this.frameRate = frameRate;
        }

        /**
         * Get the current camera mode.
         *
         * @return The current camera mode.
         */
        @Nullable
        public SettingsDefinitions.CameraMode getCameraMode() {
            return cameraMode;
        }

        /**
         * Get the current photo file format.
         *
         * @return The current photo file format.
         */
        @Nullable
        public SettingsDefinitions.PhotoFileFormat getPhotoFileFormat() {
            return photoFileFormat;
        }

        /**
         * Get the current video resolution.
         *
         * @return The current video resolution.
         */
        @Nullable
        public SettingsDefinitions.VideoResolution getResolution() {
            return resolution;
        }

        /**
         * Get the current video frame rate.
         *
         * @return The current video frame rate.
         */
        @Nullable
        public SettingsDefinitions.VideoFrameRate getFrameRate() {
            return frameRate;
        }
    }

    /**
     * The camera storage state info.
     */
    public static class CameraStorageState {
        private final SettingsDefinitions.CameraMode cameraMode;
        private final long availableCaptureCount;
        private final int availableRecordingTime;
        private SettingsDefinitions.StorageLocation storageLocation;
        private SettingsDefinitions.SDCardOperationState storageOperationState;

        @VisibleForTesting
        protected CameraStorageState(@NonNull SettingsDefinitions.CameraMode cameraMode,
                                     @NonNull SettingsDefinitions.StorageLocation storageLocation,
                                     @NonNull SettingsDefinitions.SDCardOperationState storageOperationState,
                                     long availableCaptureCount, int availableRecordingTime) {
            this.cameraMode = cameraMode;
            this.storageLocation = storageLocation;
            this.storageOperationState = storageOperationState;
            this.availableCaptureCount = availableCaptureCount;
            this.availableRecordingTime = availableRecordingTime;
        }

        /**
         * Get the current camera mode.
         *
         * @return The current camera mode.
         */
        @NonNull
        public SettingsDefinitions.CameraMode getCameraMode() {
            return cameraMode;
        }

        /**
         * Get the current storage location.
         *
         * @return The current storage location.
         */
        @NonNull
        public SettingsDefinitions.StorageLocation getStorageLocation() {
            return storageLocation;
        }

        /**
         * Get the current storage operation state.
         *
         * @return The current storage operation state.
         */
        @NonNull
        public SettingsDefinitions.SDCardOperationState getStorageOperationState() {
            return storageOperationState;
        }

        /**
         * Get the available capture count in the current storage location.
         *
         * @return The available capture count in the current storage location.
         */
        public long getAvailableCaptureCount() {
            return availableCaptureCount;
        }

        /**
         * Get the available recording time in the current storage location.
         *
         * @return The available recording time in the current storage location.
         */
        public int getAvailableRecordingTime() {
            return availableRecordingTime;
        }

        @Override
        @NonNull
        public String toString() {
            return "CameraStorageState{" +
                    "cameraMode=" + cameraMode +
                    ", storageLocation=" + storageLocation +
                    ", storageOperationState=" + storageOperationState +
                    ", availableCaptureCount=" + availableCaptureCount +
                    ", availableRecordingTime=" + availableRecordingTime +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CameraStorageState that = (CameraStorageState) o;

            if (availableCaptureCount != that.availableCaptureCount) return false;
            if (availableRecordingTime != that.availableRecordingTime) return false;
            if (cameraMode != that.cameraMode) return false;
            if (storageLocation != that.storageLocation) return false;
            return storageOperationState == that.storageOperationState;
        }

        @Override
        public int hashCode() {
            int result = cameraMode != null ? cameraMode.hashCode() : 0;
            result = 31 * result + (storageLocation != null ? storageLocation.hashCode() : 0);
            result = 31 * result + (storageOperationState != null ? storageOperationState.hashCode() : 0);
            result = 31 * result + (int) (availableCaptureCount ^ (availableCaptureCount >>> 32));
            result = 31 * result + availableRecordingTime;
            return result;
        }
    }
    //endregion
}
