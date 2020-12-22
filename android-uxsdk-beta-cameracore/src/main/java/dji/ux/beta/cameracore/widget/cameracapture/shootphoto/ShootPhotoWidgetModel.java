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

package dji.ux.beta.cameracore.widget.cameracapture.shootphoto;

import androidx.annotation.NonNull;

import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.module.FlatCameraModule;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Shoot Photo Widget Model
 * <p>
 * Widget Model for {@link ShootPhotoWidget} used to define underlying
 * logic and communication
 */
public class ShootPhotoWidgetModel extends WidgetModel {

    //region Constants
    private static final int INVALID_AVAILABLE_CAPTURE_COUNT = -1;
    private static final long INVALID_AVAILABLE_CAPTURE_COUNT_LONG = -1L;
    //endregion

    //region Public data
    private final DataProcessor<CameraPhotoState> cameraPhotoState;
    private final DataProcessor<CameraPhotoStorageState> cameraStorageState;
    private final DataProcessor<Boolean> isShootingPhoto;
    private final DataProcessor<Boolean> isStoringPhoto;
    private final DataProcessor<Boolean> canStartShootingPhoto;
    private final DataProcessor<Boolean> canStopShootingPhoto;
    private final DataProcessor<String> cameraDisplayName;
    private final DataProcessor<Boolean> isShootingInterval;
    //endregion

    //region Internal data
    private final DataProcessor<Boolean> isShootingPanorama;
    private final DataProcessor<SettingsDefinitions.PhotoAEBCount> aebCount;
    private final DataProcessor<SettingsDefinitions.PhotoBurstCount> burstCount;
    private final DataProcessor<SettingsDefinitions.PhotoBurstCount> rawBurstCount;
    private final DataProcessor<SettingsDefinitions.PhotoTimeIntervalSettings> timeIntervalSettings;
    private final DataProcessor<SettingsDefinitions.PhotoPanoramaMode> panoramaMode;
    private final DataProcessor<SettingsDefinitions.StorageLocation> storageLocation;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> sdCardState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> storageState;
    private final DataProcessor<SettingsDefinitions.SDCardOperationState> innerStorageState;
    private final DataProcessor<SSDOperationState> ssdState;
    private final DataProcessor<Long> sdAvailableCaptureCount;
    private final DataProcessor<Long> innerStorageAvailableCaptureCount;
    private final DataProcessor<Integer> ssdAvailableCaptureCount;
    private final DataProcessor<Boolean> isProductConnected;
    //endregion

    //region Other fields
    private final SettingsDefinitions.PhotoTimeIntervalSettings defaultIntervalSettings;
    private int cameraIndex;
    private DJIKey stopShootPhotoKey;
    private DJIKey startShootPhotoKey;
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Constructor
    public ShootPhotoWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                 @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        this.cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.getIndex();
        defaultIntervalSettings = new SettingsDefinitions.PhotoTimeIntervalSettings(0, 0);
        CameraPhotoState cameraPhotoState = new CameraPhotoState(SettingsDefinitions.ShootPhotoMode.UNKNOWN);

        this.cameraPhotoState = DataProcessor.create(cameraPhotoState);
        CameraSDPhotoStorageState cameraSDStorageState = new CameraSDPhotoStorageState(
                SettingsDefinitions.StorageLocation.SDCARD,
                0,
                SettingsDefinitions.SDCardOperationState.NOT_INSERTED);
        cameraStorageState = DataProcessor.create(cameraSDStorageState);
        canStartShootingPhoto = DataProcessor.create(false);
        canStopShootingPhoto = DataProcessor.create(false);
        cameraDisplayName = DataProcessor.create("");
        aebCount = DataProcessor.create(SettingsDefinitions.PhotoAEBCount.UNKNOWN);
        burstCount = DataProcessor.create(SettingsDefinitions.PhotoBurstCount.UNKNOWN);
        rawBurstCount = DataProcessor.create(SettingsDefinitions.PhotoBurstCount.UNKNOWN);
        timeIntervalSettings = DataProcessor.create(defaultIntervalSettings);
        panoramaMode = DataProcessor.create(SettingsDefinitions.PhotoPanoramaMode.UNKNOWN);
        isShootingPhoto = DataProcessor.create(false);
        isShootingInterval = DataProcessor.create(false);
        isShootingPanorama = DataProcessor.create(false);
        isStoringPhoto = DataProcessor.create(false);
        storageLocation = DataProcessor.create(SettingsDefinitions.StorageLocation.SDCARD);
        sdCardState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
        storageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.NORMAL);
        innerStorageState = DataProcessor.create(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
        ssdState = DataProcessor.create(SSDOperationState.UNKNOWN);
        sdAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT_LONG);
        innerStorageAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT_LONG);
        ssdAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        isProductConnected = DataProcessor.create(false);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }
    //endregion

    //region Data

    /**
     * Get the current shoot photo mode
     *
     * @return Flowable with {@link CameraPhotoState} instance
     */
    public Flowable<CameraPhotoState> getCameraPhotoState() {
        return cameraPhotoState.toFlowable();
    }

    /**
     * Get the current camera photo storage location
     *
     * @return Flowable with {@link CameraPhotoStorageState} instance
     */
    public Flowable<CameraPhotoStorageState> getCameraStorageState() {
        return cameraStorageState.toFlowable();
    }

    /**
     * Check if the device is currently shooting photo
     *
     * @return Flowable with boolean value
     * true - if camera is shooting photo false - camera is not shooting photo
     */
    public Flowable<Boolean> isShootingPhoto() {
        return isShootingPhoto.toFlowable();
    }

    /**
     * Check if the device is currently in the process of storing photo
     *
     * @return Flowable with boolean value
     * true - if device is storing photo false - device is not storing photo
     */
    public Flowable<Boolean> isStoringPhoto() {
        return isStoringPhoto.toFlowable();
    }

    /**
     * Check if the device is ready to shoot photo.
     *
     * @return Flowable with boolean value
     * true - device ready  false - device not ready
     */
    public Flowable<Boolean> canStartShootingPhoto() {
        return canStartShootingPhoto.toFlowable();
    }

    /**
     * Check if the device is currently shooting photo and is ready to stop
     *
     * @return Flowable with boolean value
     * true - can stop shooting false - can not stop shooting photo
     */
    public Flowable<Boolean> canStopShootingPhoto() {
        return canStopShootingPhoto.toFlowable();
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
     * Get the display name of the camera which the model is reacting to
     *
     * @return Flowable with string of camera name
     */
    public Flowable<String> getCameraDisplayName() {
        return cameraDisplayName.toFlowable();
    }
    //endregion

    //region Actions

    /**
     * Start shooting photo
     *
     * @return Completable to determine the status of the action
     */
    public Completable startShootPhoto() {
        if (!canStartShootingPhoto.getValue() || !djiSdkModel.isAvailable()) {
            return Completable.complete();
        }
        return djiSdkModel.performAction(startShootPhotoKey);
    }

    /**
     * Stop shooting photo
     *
     * @return Completable to determine the status of the action
     */
    public Completable stopShootPhoto() {
        if (!canStopShootingPhoto.getValue() || !djiSdkModel.isAvailable()) {
            return Completable.complete();
        }
        return djiSdkModel.performAction(stopShootPhotoKey);
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        // Action keys
        stopShootPhotoKey = CameraKey.create(CameraKey.STOP_SHOOT_PHOTO, cameraIndex);
        startShootPhotoKey = CameraKey.create(CameraKey.START_SHOOT_PHOTO, cameraIndex);
        // Product connection
        DJIKey cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex);
        bindDataProcessor(cameraConnectionKey, isProductConnected, newValue -> onCameraConnected((boolean) newValue));
        // Photo mode
        DJIKey photoAEBParamKey = CameraKey.create(CameraKey.PHOTO_AEB_COUNT, cameraIndex);
        DJIKey photoBurstCountKey = CameraKey.create(CameraKey.PHOTO_BURST_COUNT, cameraIndex);
        DJIKey photoIntervalParamKey = CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, cameraIndex);
        DJIKey rawBurstCountKey = CameraKey.create(CameraKey.PHOTO_RAW_BURST_COUNT, cameraIndex);
        DJIKey panoramaModeKey = CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, cameraIndex);
        bindDataProcessor(photoAEBParamKey, aebCount);
        bindDataProcessor(photoBurstCountKey, burstCount);
        bindDataProcessor(rawBurstCountKey, rawBurstCount);
        bindDataProcessor(photoIntervalParamKey, timeIntervalSettings);
        bindDataProcessor(panoramaModeKey, panoramaMode);

        // Is shooting photo state
        DJIKey isShootingPhotoKey = CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex);
        bindDataProcessor(isShootingPhotoKey, isShootingPhoto);
        // Is storing photo state
        DJIKey isStoringPhotoKey = CameraKey.create(CameraKey.IS_STORING_PHOTO, cameraIndex);
        bindDataProcessor(isStoringPhotoKey, isStoringPhoto);
        // Can start shooting photo
        // TODO can't take photo when product is not connected
        DJIKey isShootingPhotoEnabledKey = CameraKey.create(CameraKey.IS_SHOOTING_PHOTO_ENABLED, cameraIndex);
        bindDataProcessor(isShootingPhotoEnabledKey, canStartShootingPhoto);
        // Can stop shooting photo
        DJIKey isShootingIntervalPhotoKey = CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex);
        DJIKey isShootingPanoramaKey = CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex);
        bindDataProcessor(isShootingIntervalPhotoKey, isShootingInterval, newValue -> onCanStopShootingPhoto((boolean) newValue));
        bindDataProcessor(isShootingPanoramaKey, isShootingPanorama, newValue -> onCanStopShootingPhoto((boolean) newValue));
        // Display name
        DJIKey cameraDisplayNameKey = CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex);
        bindDataProcessor(cameraDisplayNameKey, cameraDisplayName);
        // Storage
        DJIKey storageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex);
        DJIKey sdCardStateKey = CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex);
        DJIKey storageStateKey = CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex);
        DJIKey innerStorageStateKey = CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex);
        DJIKey ssdOperationStateKey = CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex);
        DJIKey sdAvailableCaptureCountKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex);
        DJIKey innerStorageAvailableCaptureCountKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex);
        DJIKey ssdAvailableCaptureCountKey = CameraKey.create(CameraKey.RAW_PHOTO_BURST_COUNT, cameraIndex);
        bindDataProcessor(storageLocationKey, storageLocation);
        bindDataProcessor(sdCardStateKey, sdCardState);
        bindDataProcessor(storageStateKey, storageState);
        bindDataProcessor(innerStorageStateKey, innerStorageState);
        bindDataProcessor(ssdOperationStateKey, ssdState);
        bindDataProcessor(sdAvailableCaptureCountKey, sdAvailableCaptureCount);
        bindDataProcessor(innerStorageAvailableCaptureCountKey, innerStorageAvailableCaptureCount);
        bindDataProcessor(ssdAvailableCaptureCountKey, ssdAvailableCaptureCount);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        updateCameraPhotoState();
        updateCameraStorageState();
    }
    //endregion

    //region Helpers
    private void updateCameraPhotoState() {
        CameraPhotoState cameraPhotoState = null;
        SettingsDefinitions.ShootPhotoMode shootPhotoMode = flatCameraModule.getShootPhotoModeProcessor().getValue();
        switch (shootPhotoMode) {
            case SINGLE:
            case HDR:
            case HYPER_LIGHT:
            case SHALLOW_FOCUS:
            case EHDR:
                cameraPhotoState = new CameraPhotoState(shootPhotoMode);
                break;
            case BURST:
                if (!SettingsDefinitions.PhotoBurstCount.UNKNOWN.equals(burstCount.getValue())) {
                    cameraPhotoState = new CameraBurstPhotoState(
                            shootPhotoMode,
                            burstCount.getValue());
                }
                break;
            case RAW_BURST:
                if (!SettingsDefinitions.PhotoBurstCount.UNKNOWN.equals(rawBurstCount.getValue())) {
                    cameraPhotoState = new CameraBurstPhotoState(
                            shootPhotoMode,
                            rawBurstCount.getValue()
                    );
                }
                break;
            case AEB:
                if (!SettingsDefinitions.PhotoAEBCount.UNKNOWN.equals(aebCount.getValue())) {
                    cameraPhotoState = new CameraAEBPhotoState(
                            shootPhotoMode,
                            aebCount.getValue()
                    );
                }
                break;
            case INTERVAL:
                SettingsDefinitions.PhotoTimeIntervalSettings intervalSettings = timeIntervalSettings.getValue();
                if (!defaultIntervalSettings.equals(timeIntervalSettings.getValue())) {
                    cameraPhotoState = new CameraIntervalPhotoState(
                            shootPhotoMode,
                            intervalSettings.getCaptureCount(),
                            intervalSettings.getTimeIntervalInSeconds()
                    );
                }
                break;
            case PANORAMA:
                if (!SettingsDefinitions.PhotoPanoramaMode.UNKNOWN.equals(panoramaMode.getValue())) {
                    cameraPhotoState = new CameraPanoramaPhotoState(
                            shootPhotoMode,
                            panoramaMode.getValue()
                    );
                }
                break;
            default:
                break;
        }

        if (cameraPhotoState != null) {
            this.cameraPhotoState.onNext(cameraPhotoState);
        }
    }

    private void updateCameraStorageState() {
        SettingsDefinitions.StorageLocation currentStorageLocation = storageLocation.getValue();
        if (SettingsDefinitions.StorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        SettingsDefinitions.ShootPhotoMode currentShootPhotoMode = flatCameraModule.getShootPhotoModeProcessor().getValue();
        long availableCaptureCount = getAvailableCaptureCount(currentStorageLocation, currentShootPhotoMode);
        if (availableCaptureCount == INVALID_AVAILABLE_CAPTURE_COUNT) {
            return;
        }

        CameraPhotoStorageState newCameraPhotoStorageState = null;
        if (currentShootPhotoMode == SettingsDefinitions.ShootPhotoMode.RAW_BURST) {
            newCameraPhotoStorageState = new CameraSSDPhotoStorageState(SettingsDefinitions.StorageLocation.UNKNOWN, availableCaptureCount, ssdState.getValue());
        } else if (SettingsDefinitions.StorageLocation.SDCARD.equals(currentStorageLocation)) {
            if (!SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR.equals(sdCardState.getValue())) {
                newCameraPhotoStorageState = new CameraSDPhotoStorageState(currentStorageLocation, availableCaptureCount, sdCardState.getValue());
            } else if (!SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR.equals(storageState.getValue())) {
                newCameraPhotoStorageState = new CameraSDPhotoStorageState(currentStorageLocation, availableCaptureCount, storageState.getValue());
            }
        } else if (SettingsDefinitions.StorageLocation.INTERNAL_STORAGE.equals(currentStorageLocation)) {
            newCameraPhotoStorageState = new CameraSDPhotoStorageState(currentStorageLocation, availableCaptureCount, innerStorageState.getValue());
        }

        if (newCameraPhotoStorageState != null) {
            cameraStorageState.onNext(newCameraPhotoStorageState);
        }
    }

    private long getAvailableCaptureCount(SettingsDefinitions.StorageLocation storageLocation,
                                          SettingsDefinitions.ShootPhotoMode shootPhotoMode) {
        if (shootPhotoMode == SettingsDefinitions.ShootPhotoMode.RAW_BURST) {
            return ssdAvailableCaptureCount.getValue();
        }

        switch (storageLocation) {
            case SDCARD:
                return sdAvailableCaptureCount.getValue();
            case INTERNAL_STORAGE:
                return innerStorageAvailableCaptureCount.getValue();
            case UNKNOWN:
            default:
                return INVALID_AVAILABLE_CAPTURE_COUNT;
        }
    }

    private void onCanStopShootingPhoto(boolean canStopShootingPhoto) {
        this.canStopShootingPhoto.onNext(canStopShootingPhoto);
    }

    private void onCameraConnected(boolean isCameraConnected) {
        if (!isCameraConnected) {
            // Reset storage state
            sdCardState.onNext(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
            storageState.onNext(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
            innerStorageState.onNext(SettingsDefinitions.SDCardOperationState.UNKNOWN_ERROR);
            ssdState.onNext(SSDOperationState.UNKNOWN);
        }
    }
    //endregion
}
