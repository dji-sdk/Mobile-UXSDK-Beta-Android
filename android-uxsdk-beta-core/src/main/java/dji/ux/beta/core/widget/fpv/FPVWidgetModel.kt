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
package dji.ux.beta.core.widget.fpv

import dji.common.airlink.PhysicalSource
import dji.common.camera.ResolutionAndFrameRate
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.CameraMode
import dji.common.camera.SettingsDefinitions.PhotoAspectRatio
import dji.common.product.Model
import dji.keysdk.AirLinkKey
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.sdk.camera.VideoFeeder
import dji.sdk.camera.VideoFeeder.VideoDataListener
import dji.sdk.camera.VideoFeeder.VideoFeed
import dji.sdk.codec.DJICodecManager
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.schedulers.Schedulers
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.ProductUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.core.util.SettingDefinitions.CameraSide

private const val TAG = "FPVWidgetModel"

/**
 * Widget Model for the [FPVWidget] used to define
 * the underlying logic and communication
 */
class FPVWidgetModel(djiSdkModel: DJISDKModel,
                     keyedStore: ObservableInMemoryKeyedStore,
                     private val videoDataListener: VideoDataListener?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val modelNameDataProcessor: DataProcessor<Model> = DataProcessor.create(Model.UNKNOWN_AIRCRAFT)
    private val orientationProcessor: DataProcessor<SettingsDefinitions.Orientation> = DataProcessor.create(SettingsDefinitions.Orientation.UNKNOWN)
    private val photoAspectRatioProcessor: DataProcessor<PhotoAspectRatio> = DataProcessor.create(PhotoAspectRatio.UNKNOWN)
    private val cameraModeProcessor: DataProcessor<CameraMode> = DataProcessor.create(CameraMode.UNKNOWN)
    private val resolutionAndFrameRateProcessor: DataProcessor<ResolutionAndFrameRate> =
            DataProcessor.create(ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.UNKNOWN,
                    SettingsDefinitions.VideoFrameRate.UNKNOWN))
    private val videoViewChangedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val videoFeedSourceProcessor: DataProcessor<DJICodecManager.VideoSource> = DataProcessor.create(DJICodecManager.VideoSource.UNKNOWN)
    private val cameraNameProcessor: DataProcessor<String> = DataProcessor.create("")
    private val cameraSideProcessor: DataProcessor<CameraSide> = DataProcessor.create(CameraSide.UNKNOWN)

    private var currentVideoFeed: VideoFeed? = null
    private var currentModel: Model? = null
    //endregion

    //region Data
    /**
     * Get the current camera index. This value should only be used for video size calculation.
     * To get the camera side, use [FPVWidgetModel.cameraSide] instead.
     */
    var currentCameraIndex: CameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN
        private set

    /**
     * The video source can be one of these three options: AUTO, PRIMARY, SECONDARY. By
     * default, the video source is set to "AUTO" if user does not specify it.
     */
    var videoSource: SettingDefinitions.VideoSource? = SettingDefinitions.VideoSource.PRIMARY
        set(value) {
            field = value
            restart()
        }

    /**
     * Get the model of the product
     */
    val model: Flowable<Model>
        get() = modelNameDataProcessor.toFlowable()

    /**
     * Get the orientation of the video feed
     */
    val orientation: Flowable<SettingsDefinitions.Orientation>
        get() = orientationProcessor.toFlowable()

    /**
     * Get the video feed source
     */
    val videoFeedSource: Flowable<DJICodecManager.VideoSource>
        get() = videoFeedSourceProcessor.toFlowable()

    /**
     * Get whether the video view has changed
     */
    @get:JvmName("hasVideoViewChanged")
    val hasVideoViewChanged: Flowable<Boolean>
        get() = videoViewChangedProcessor.toFlowable()

    /**
     * Get the name of the current camera
     */
    val cameraName: Flowable<String>
        get() = cameraNameProcessor.toFlowable()

    /**
     * Get the current camera's side
     */
    val cameraSide: Flowable<CameraSide>
        get() = cameraSideProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        //TODO: Add peak threshold, overexposure and dimensions event for grid display once inter-widget communication is done
        val modelNameKey = ProductKey.create(ProductKey.MODEL_NAME)
        val orientationKey = CameraKey.create(CameraKey.ORIENTATION)
        val photoAspectRatioKey = CameraKey.create(CameraKey.PHOTO_ASPECT_RATIO)
        val cameraModeKey = CameraKey.create(CameraKey.MODE)
        val videoResolutionAndFrameRateKey = CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE)
        bindDataProcessor(modelNameKey, modelNameDataProcessor) { model: Any? ->
            currentModel = model as Model?
            updateVideoFeed()
            updateCameraDisplay()
        }
        val videoViewChangedConsumer = Consumer { _: Any? -> videoViewChangedProcessor.onNext(true) }
        bindDataProcessor(orientationKey, orientationProcessor, videoViewChangedConsumer)
        bindDataProcessor(photoAspectRatioKey, photoAspectRatioProcessor, videoViewChangedConsumer)
        bindDataProcessor(cameraModeKey, cameraModeProcessor, videoViewChangedConsumer)
        bindDataProcessor(videoResolutionAndFrameRateKey, resolutionAndFrameRateProcessor, videoViewChangedConsumer)
    }

    override fun inCleanup() {
        currentVideoFeed?.let {
            if (it.listeners.contains(videoDataListener)) {
                it.removeVideoDataListener(videoDataListener)
            }
        }
    }

    //endregion
    //region Updates
    public override fun updateStates() {
        updateCameraDisplay()
    }

    //endregion
    //region Helpers
    private fun updateVideoFeed() {
        getVideoFeeder()?.let {
            if (videoSource == SettingDefinitions.VideoSource.AUTO) {
                if (isExtPortSupportedProduct()) {
                    registerLiveVideo(it.secondaryVideoFeed, false)
                    switchToExternalCameraChannel()
                } else {
                    registerLiveVideo(it.primaryVideoFeed, true)
                }
            } else if (videoSource == SettingDefinitions.VideoSource.PRIMARY) {
                registerLiveVideo(it.primaryVideoFeed, true)
            } else if (videoSource == SettingDefinitions.VideoSource.SECONDARY) {
                registerLiveVideo(it.secondaryVideoFeed, false)
            }
        }
    }

    private fun registerLiveVideo(videoFeed: VideoFeed?, isPrimary: Boolean) {
        if (videoFeed != null && videoDataListener != null) {
            // Note: this check is necessary to avoid set the same videoDataListener to two different video feeds
            currentVideoFeed?.let {
                if (it.listeners.contains(videoDataListener)) {
                    it.removeVideoDataListener(videoDataListener)
                }
            }
            currentVideoFeed = videoFeed
            currentVideoFeed?.addVideoDataListener(videoDataListener)
        }
        if (currentVideoFeed != null && currentVideoFeed?.videoSource == null) {
            videoSource = null
        }
        videoFeedSourceProcessor.onNext(
                if (isPrimary) {
                    DJICodecManager.VideoSource.CAMERA
                } else {
                    DJICodecManager.VideoSource.FPV
                })
    }

    private fun switchToExternalCameraChannel() {
        val extVideoInputPortEnabledKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED)
        val extEnabled = djiSdkModel.getCacheValue(extVideoInputPortEnabledKey) as Boolean?
        if (extEnabled == null || !extEnabled) {
            addDisposable(djiSdkModel.setValue(extVideoInputPortEnabledKey, true)
                    .subscribeOn(Schedulers.io())
                    .subscribe(Action { setCameraChannelBandwidth() },
                            logErrorConsumer(TAG, "SetExtVideoInputPortEnabled: ")))
        } else {
            setCameraChannelBandwidth()
        }
    }

    private fun setCameraChannelBandwidth() {
        val bandwidthAllocationForLBVideoInputPort = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT)
        addDisposable(djiSdkModel.setValue(bandwidthAllocationForLBVideoInputPort, 0.0f)
                .subscribeOn(Schedulers.io())
                .subscribe(Action {}, logErrorConsumer(TAG, "SetBandwidthAllocationForLBVideoInputPort: ")))
    }

    private fun updateCameraDisplay() {
        val displayName0Key = CameraKey.create(CameraKey.DISPLAY_NAME, 0)
        val displayName1Key = CameraKey.create(CameraKey.DISPLAY_NAME, 1)
        var displayName0 = djiSdkModel.getCacheValue(displayName0Key) as String?
        var displayName1 = djiSdkModel.getCacheValue(displayName1Key) as String?
        var displayName = ""
        var displaySide = CameraSide.UNKNOWN
        currentCameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN
        if (displayName0 == null) {
            displayName0 = PhysicalSource.UNKNOWN.toString()
        }
        if (displayName1 == null) {
            displayName1 = PhysicalSource.UNKNOWN.toString()
        }
        val physicalVideoSource: PhysicalSource?
        if (currentModel != null && currentVideoFeed != null) {
            physicalVideoSource = currentVideoFeed?.videoSource
            if (physicalVideoSource == null || (physicalVideoSource == PhysicalSource.UNKNOWN)) {
                displayName = PhysicalSource.UNKNOWN.toString()
            } else {
                if (isExtPortSupportedProduct()) {
                    if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN
                    } else if (physicalVideoSource == PhysicalSource.EXT) {
                        displayName = PhysicalSource.EXT.toString()
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_2
                    } else if (physicalVideoSource == PhysicalSource.HDMI) {
                        displayName = PhysicalSource.HDMI.toString()
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_2
                    } else if (physicalVideoSource == PhysicalSource.AV) {
                        displayName = PhysicalSource.AV.toString()
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_2
                    }
                } else if (currentModel == Model.MATRICE_210_RTK
                        || currentModel == Model.MATRICE_210
                        || currentModel == Model.MATRICE_210_RTK_V2
                        || currentModel == Model.MATRICE_210_V2
                        || currentModel == Model.MATRICE_300_RTK) {
                    if (physicalVideoSource == PhysicalSource.LEFT_CAM) {
                        displayName = displayName0
                        displaySide = CameraSide.PORT
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_0
                    } else if (physicalVideoSource == PhysicalSource.RIGHT_CAM) {
                        displayName = displayName1
                        displaySide = CameraSide.STARBOARD
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_2
                    } else if (physicalVideoSource == PhysicalSource.FPV_CAM) {
                        displayName = PhysicalSource.FPV_CAM.toString()
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN //Index will be assigned as required
                    } else if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_0
                    }
                } else if (currentModel == Model.INSPIRE_2
                        || currentModel == Model.MATRICE_200_V2) {
                    if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_0
                    } else if (physicalVideoSource == PhysicalSource.FPV_CAM) {
                        displayName = PhysicalSource.FPV_CAM.toString()
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN //Index will be assigned as required
                    }
                } else {
                    displayName = displayName0
                    currentCameraIndex = CameraIndex.CAMERA_INDEX_0
                }
                displayName = displayName.replace("-Visual", "")
            }
        }
        cameraNameProcessor.onNext(displayName)
        cameraSideProcessor.onNext(displaySide)
    }
    //endregion

    //region Helpers for unit testing
    private fun getVideoFeeder(): VideoFeeder? {
        return VideoFeeder.getInstance()
    }

    private fun isExtPortSupportedProduct(): Boolean {
        return ProductUtil.isExtPortSupportedProduct()
    }
    //endregion
}