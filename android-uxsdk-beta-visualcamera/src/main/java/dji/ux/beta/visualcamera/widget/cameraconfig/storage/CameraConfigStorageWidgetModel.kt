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

package dji.ux.beta.visualcamera.widget.cameraconfig.storage

import dji.common.camera.ResolutionAndFrameRate
import dji.common.camera.SettingsDefinitions.*
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.FlatCameraModule
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions

private const val TAG = "ConfigStorageWidMod"
/**
 * Widget Model for the [CameraConfigStorageWidget] used to define
 * the underlying logic and communication
 */
class CameraConfigStorageWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val cameraConfigStorageStateProcessor: DataProcessor<CameraConfigStorageState> = DataProcessor.create(CameraConfigStorageState.ProductDisconnected)
    private val cameraConnectionProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val storageLocationProcessor: DataProcessor<StorageLocation> = DataProcessor.create(StorageLocation.UNKNOWN)
    private val sdCardOperationStateProcessor: DataProcessor<SDCardOperationState> = DataProcessor.create(SDCardOperationState.UNKNOWN)
    private val sdCardStorageCountProcessor: DataProcessor<Long> = DataProcessor.create(0)
    private val internalOperationStateProcessor: DataProcessor<SDCardOperationState> = DataProcessor.create(SDCardOperationState.UNKNOWN)
    private val internalStorageCountProcessor: DataProcessor<Long> = DataProcessor.create(0)
    private val photoFileFormatProcessor: DataProcessor<PhotoFileFormat> = DataProcessor.create(PhotoFileFormat.UNKNOWN)
    private val sdCardRecordingTimeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val internalRecordingTimeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val resolutionAndFrameRateProcessor: DataProcessor<ResolutionAndFrameRate> = DataProcessor.create(ResolutionAndFrameRate(
            VideoResolution.UNKNOWN,
            VideoFrameRate.UNKNOWN))
    private val flatCameraModule = FlatCameraModule()
    private val lensModule = LensModule()



    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: SettingDefinitions.CameraIndex = SettingDefinitions.CameraIndex.find(SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            lensModule.setCameraIndex(this, value)
            restart()
        }

    /**
     * The lens the widget model is reacting to
     */
    var lensType: LensType = LensType.ZOOM
        set(value) {
            field = value
            restart()
        }

    /**
     * The camera config storage state.
     */
    val cameraConfigStorageState: Flowable<CameraConfigStorageState>
        get() = cameraConfigStorageStateProcessor.toFlowable()

    init {
        addModule(lensModule)
        addModule(flatCameraModule)
    }

    /**
     * Setup method for initialization that must be implemented
     */
    override fun inSetup() {
        val cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionProcessor)
        val storageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex.index)
        bindDataProcessor(storageLocationKey, storageLocationProcessor)
        val sdCardStateKey = CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex.index)
        bindDataProcessor(sdCardStateKey, sdCardOperationStateProcessor)
        val sdCardPhotoCountKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex.index)
        bindDataProcessor(sdCardPhotoCountKey, sdCardStorageCountProcessor)
        val sdCardRecordingTimeKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex.index)
        bindDataProcessor(sdCardRecordingTimeKey, sdCardRecordingTimeProcessor)
        val internalStorageStateKey = CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex.index)
        bindDataProcessor(internalStorageStateKey, internalOperationStateProcessor)
        val internalPhotoCountKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex.index)
        bindDataProcessor(internalPhotoCountKey, internalStorageCountProcessor)
        val internalRecordingTimeKey = CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex.index)
        bindDataProcessor(internalRecordingTimeKey, internalRecordingTimeProcessor)
        val photoFileFormatKey: DJIKey = lensModule.createLensKey(CameraKey.PHOTO_FILE_FORMAT, cameraIndex.index, lensType.value())
        bindDataProcessor(photoFileFormatKey, photoFileFormatProcessor)
        val videoResolutionFrameRateKey = lensModule.createLensKey(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex.index, lensType.value())
        bindDataProcessor(videoResolutionFrameRateKey, resolutionAndFrameRateProcessor)
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))
    }

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    override fun inCleanup() {
        // Empty method
    }

    /**
     * Method to update states for the required processors in the child classes as required
     */
    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (cameraConnectionProcessor.value) {
                when (flatCameraModule.cameraModeDataProcessor.value) {
                    CameraMode.SHOOT_PHOTO -> {
                        cameraConfigStorageStateProcessor.onNext(preparePhotoState())
                    }
                    CameraMode.RECORD_VIDEO -> {
                        cameraConfigStorageStateProcessor.onNext(prepareVideoState())
                    }
                    else -> {
                        cameraConfigStorageStateProcessor.onNext(CameraConfigStorageState.NotSupported)
                    }
                }
            } else {
                cameraConfigStorageStateProcessor.onNext(CameraConfigStorageState.CameraDisconnected)
            }
        } else {
            cameraConfigStorageStateProcessor.onNext(CameraConfigStorageState.ProductDisconnected)
        }
    }

    private fun preparePhotoState(): CameraConfigStorageState.PhotoMode {
        val availableCaptureCount: Long = if (storageLocationProcessor.value == StorageLocation.INTERNAL_STORAGE) {
            internalStorageCountProcessor.value
        } else {
            sdCardStorageCountProcessor.value
        }
        return CameraConfigStorageState.PhotoMode(storageLocationProcessor.value,
                getOperationState(),
                availableCaptureCount,
                photoFileFormatProcessor.value)
    }

    private fun prepareVideoState(): CameraConfigStorageState.VideoMode {
        val availableRecordTime: Int = if (storageLocationProcessor.value == StorageLocation.INTERNAL_STORAGE) {
            internalRecordingTimeProcessor.value
        } else {
            sdCardRecordingTimeProcessor.value
        }
        return CameraConfigStorageState.VideoMode(storageLocationProcessor.value,
                getOperationState(),
                availableRecordTime,
                resolutionAndFrameRateProcessor.value)
    }

    private fun getOperationState(): SDCardOperationState {
        return if (storageLocationProcessor.value == StorageLocation.INTERNAL_STORAGE) {
            internalOperationStateProcessor.value
        } else {
            sdCardOperationStateProcessor.value
        }
    }

    /**
     * Class to represent states of Storage
     */
    sealed class CameraConfigStorageState {

        /**
         *  When product is disconnected
         */
        object ProductDisconnected : CameraConfigStorageState()

        /**
         *  When camera is disconnected
         */
        object CameraDisconnected : CameraConfigStorageState()

        /**
         *  When camera is connected but not in shoot photo
         *  or record video mode
         */
        object NotSupported : CameraConfigStorageState()

        /**
         * When camera is in Photo Mode
         */
        data class PhotoMode(val storageLocation: StorageLocation,
                             val storageOperationState: SDCardOperationState,
                             val availableCaptureCount: Long,
                             val photoFileFormat: PhotoFileFormat) : CameraConfigStorageState()

        /**
         * When camera is in Video Mode
         */
        data class VideoMode(val storageLocation: StorageLocation,
                             val storageOperationState: SDCardOperationState,
                             val availableRecordTime: Int,
                             val resolutionAndFrameRate: ResolutionAndFrameRate) : CameraConfigStorageState()
    }


}