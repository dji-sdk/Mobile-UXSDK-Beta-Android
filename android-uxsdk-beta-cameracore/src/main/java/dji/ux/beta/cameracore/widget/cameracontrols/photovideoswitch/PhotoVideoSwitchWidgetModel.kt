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

package dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch

import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidgetModel.PhotoVideoSwitchState.ProductDisconnected
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.FlatCameraModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

/**
 * Photo Video Switch Widget Model
 *
 *
 * Widget Model for the [PhotoVideoSwitchWidget] used to define the
 * underlying logic and communication
 */
class PhotoVideoSwitchWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val isCameraConnectedDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isRecordingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isShootingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isShootingIntervalDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isShootingBurstDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isShootingRawBurstDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isShootingPanoramaDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val photoVideoSwitchStateProcessor: DataProcessor<PhotoVideoSwitchState> = DataProcessor.create(ProductDisconnected)
    private val flatCameraModule: FlatCameraModule = FlatCameraModule()
    private val enabled: Boolean
        get() {
            return (!isRecordingDataProcessor.value
                    && !isShootingDataProcessor.value
                    && !isShootingBurstDataProcessor.value
                    && !isShootingIntervalDataProcessor.value
                    && !isShootingRawBurstDataProcessor.value
                    && !isShootingPanoramaDataProcessor.value)

        }

    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: CameraIndex = CameraIndex.find(CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            flatCameraModule.setCameraIndex(value)
            restart()
        }

    /**
     * Current state for the photo video switch.
     */
    val photoVideoSwitchState: Flowable<PhotoVideoSwitchState>
        get() = photoVideoSwitchStateProcessor.toFlowable()

    //endregion

    //region lifecycle
    init {
        addModule(flatCameraModule)
    }

    override fun inSetup() {
        val cameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, isCameraConnectedDataProcessor)
        val isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, cameraIndex.index)
        bindDataProcessor(isRecordingKey, isRecordingDataProcessor)
        val isShootingKey = CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex.index)
        bindDataProcessor(isShootingKey, isShootingDataProcessor)
        val isShootingIntervalKey = CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex.index)
        bindDataProcessor(isShootingIntervalKey, isShootingIntervalDataProcessor)
        val isShootingBurstKey = CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex.index)
        bindDataProcessor(isShootingBurstKey, isShootingBurstDataProcessor)
        val isShootingRawBurstKey = CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex.index)
        bindDataProcessor(isShootingRawBurstKey, isShootingRawBurstDataProcessor)
        val isShootingPanoramaKey = CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex.index)
        bindDataProcessor(isShootingPanoramaKey, isShootingPanoramaDataProcessor)
    }

    override fun inCleanup() {
        // do nothing
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (isCameraConnectedDataProcessor.value) {
                if (enabled) {
                    if (flatCameraModule.cameraModeDataProcessor.value == SettingsDefinitions.CameraMode.SHOOT_PHOTO) {
                        photoVideoSwitchStateProcessor.onNext(PhotoVideoSwitchState.PhotoMode)
                    } else {
                        photoVideoSwitchStateProcessor.onNext(PhotoVideoSwitchState.VideoMode)
                    }
                } else {
                    photoVideoSwitchStateProcessor.onNext(PhotoVideoSwitchState.Disabled)
                }
            } else {
                photoVideoSwitchStateProcessor.onNext(PhotoVideoSwitchState.CameraDisconnected)
            }
        } else {
            photoVideoSwitchStateProcessor.onNext(ProductDisconnected)
        }
    }
    //endregion

    //region public methods
    /**
     * Toggle between photo mode and video mode
     *
     * @return Completable
     */
    fun toggleCameraMode(): Completable {
        return if (flatCameraModule.cameraModeDataProcessor.value == SettingsDefinitions.CameraMode.SHOOT_PHOTO) {
            flatCameraModule.setCameraMode(djiSdkModel, SettingsDefinitions.CameraMode.RECORD_VIDEO)
        } else {
            flatCameraModule.setCameraMode(djiSdkModel, SettingsDefinitions.CameraMode.SHOOT_PHOTO)
        }
    }
    //endregion


    /**
     * The state representing photo/video state.
     */
    sealed class PhotoVideoSwitchState {
        /**
         * Product is currently disconnected
         */
        object ProductDisconnected : PhotoVideoSwitchState()

        /**
         * Camera is currently disconnected
         */
        object CameraDisconnected : PhotoVideoSwitchState()

        /**
         * Camera is not connected or in use.
         * Cannot toggle between photo/video mode
         */
        object Disabled : PhotoVideoSwitchState()

        /**
         * Camera is currently in Shoot Photo mode
         */
        object PhotoMode : PhotoVideoSwitchState()

        /**
         * Camera is currently in Record Video mode
         */
        object VideoMode : PhotoVideoSwitchState()

    }


}