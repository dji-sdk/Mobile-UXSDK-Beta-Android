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
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.ResolutionAndFrameRate
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.PhotoAspectRatio
import dji.common.product.Model
import dji.keysdk.*
import dji.log.DJILog
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.camera.VideoFeeder.VideoDataListener
import dji.sdk.camera.VideoFeeder.VideoFeed
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.FlatCameraModule
import dji.ux.beta.core.util.CameraUtil
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
                     private val videoDataListener: VideoDataListener?,
                     private val flatCameraModule: FlatCameraModule
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val modelNameDataProcessor: DataProcessor<Model> = DataProcessor.create(Model.UNKNOWN_AIRCRAFT)
    private val orientationProcessor: DataProcessor<SettingsDefinitions.Orientation> = DataProcessor.create(SettingsDefinitions.Orientation.UNKNOWN)
    private val photoAspectRatioProcessor: DataProcessor<PhotoAspectRatio> = DataProcessor.create(PhotoAspectRatio.UNKNOWN)
    private val resolutionAndFrameRateProcessor: DataProcessor<ResolutionAndFrameRate> =
            DataProcessor.create(ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.UNKNOWN,
                    SettingsDefinitions.VideoFrameRate.UNKNOWN))
    private val videoViewChangedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val videoFeedSourceProcessor: DataProcessor<DJICodecManager.VideoSource> = DataProcessor.create(DJICodecManager.VideoSource.UNKNOWN)
    private val cameraNameProcessor: DataProcessor<String> = DataProcessor.create("")
    private val cameraSideProcessor: DataProcessor<CameraSide> = DataProcessor.create(CameraSide.UNKNOWN)

    private var currentVideoFeed: VideoFeed? = null
    private var currentModel: Model? = null
    private var cameraVideoStreamSource: CameraVideoStreamSource = CameraVideoStreamSource.ZOOM
    //endregion

    //region Data
    /**
     * The current camera index. This value should only be used for video size calculation.
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

    //region Constructor
    init {
        addModule(flatCameraModule)
    }
    //endregion

    //region Lifecycle
    override fun inSetup() {
        //TODO: Add peak threshold, overexposure
        val modelNameKey = ProductKey.create(ProductKey.MODEL_NAME)
        val orientationKey = CameraKey.create(CameraKey.ORIENTATION)
        val photoAspectRatioKey = djiSdkModel.createLensKey(CameraKey.PHOTO_ASPECT_RATIO,
                currentCameraIndex.index,
                CameraUtil.getLensIndex(cameraVideoStreamSource, cameraNameProcessor.value))
        val videoResolutionAndFrameRateKey = djiSdkModel.createLensKey(CameraKey.RESOLUTION_FRAME_RATE,
                currentCameraIndex.index,
                CameraUtil.getLensIndex(cameraVideoStreamSource, cameraNameProcessor.value))
        bindDataProcessor(modelNameKey, modelNameDataProcessor) { model: Any? ->
            currentModel = model as Model?
            if (model == Model.MATRICE_300_RTK) {
                updateSources()
            } else {
                updateVideoFeed()
                updateCameraDisplay()
            }
        }
        val videoViewChangedConsumer = Consumer { _: Any? -> videoViewChangedProcessor.onNext(true) }
        bindDataProcessor(orientationKey, orientationProcessor, videoViewChangedConsumer)
        bindDataProcessor(photoAspectRatioKey, photoAspectRatioProcessor, videoViewChangedConsumer)
        bindDataProcessor(videoResolutionAndFrameRateKey, resolutionAndFrameRateProcessor, videoViewChangedConsumer)
        addDisposable(flatCameraModule.cameraModeDataProcessor.toFlowable()
                .doOnNext(videoViewChangedConsumer)
                .subscribe(Consumer { }, logErrorConsumer(TAG, "camera mode: ")))

        val primaryVideoFeedPhysicalSourceKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.PRIMARY_VIDEO_FEED_PHYSICAL_SOURCE)
        addDisposable(djiSdkModel.addListener(primaryVideoFeedPhysicalSourceKey, this)
                .subscribe(Consumer {
                    updateVideoFeed()
                    updateCameraDisplay()
                }, logErrorConsumer(TAG, "Error listening to primary video feed physical source key ")))
        val secondaryVideoFeedPhysicalSourceKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.SECONDARY_VIDEO_FEED_PHYSICAL_SOURCE)
        addDisposable(djiSdkModel.addListener(secondaryVideoFeedPhysicalSourceKey, this)
                .subscribe(Consumer {
                    updateVideoFeed()
                    updateCameraDisplay()
                }, logErrorConsumer(TAG, "Error listening to secondary video feed physical source key ")))
        val rcModeKey = RemoteControllerKey.create(RemoteControllerKey.MODE)
        addDisposable(djiSdkModel.addListener(rcModeKey, this)
                .subscribe(Consumer {
                    if (currentModel == Model.MATRICE_300_RTK) {
                        updateSources()
                    }
                }, logErrorConsumer(TAG, "Error listening to RC Mode key ")))
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

    //region User interaction
    /**
     * Set the [cameraVideoStreamSource] for multi-lens cameras.
     *
     * @return Completable representing the success/failure of set action
     */
    fun setCameraVideoStreamSource(cameraVideoStreamSource: CameraVideoStreamSource): Completable {
        this.cameraVideoStreamSource = cameraVideoStreamSource
        val cameraVideoStreamSourceKey: DJIKey = CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE, currentCameraIndex.index)
        return djiSdkModel.setValue(cameraVideoStreamSourceKey, cameraVideoStreamSource).also {
            restart()
        }
    }
    //endregion

    //region Helpers
    private fun updateSources() {
        var mainVideoStream = PhysicalSource.UNKNOWN
        var secondaryVideoStream = PhysicalSource.UNKNOWN
        val product: BaseProduct? = DJISDKManager.getInstance().product
        if (product is Aircraft) {
            val portCamera: Camera? = product.getCameraWithComponentIndex(0)
            val starboardCamera: Camera? = product.getCameraWithComponentIndex(1)
            val topCamera: Camera? = product.getCameraWithComponentIndex(4)
            if (portCamera != null && portCamera.isConnected) {
                mainVideoStream = PhysicalSource.LEFT_CAM
                secondaryVideoStream = if (starboardCamera != null && starboardCamera.isConnected) {
                    PhysicalSource.RIGHT_CAM
                } else if (topCamera != null && topCamera.isConnected) {
                    PhysicalSource.TOP_CAM
                } else {
                    PhysicalSource.FPV_CAM
                }
            } else if (starboardCamera != null && starboardCamera.isConnected) {
                mainVideoStream = PhysicalSource.RIGHT_CAM
                secondaryVideoStream = if (topCamera != null && topCamera.isConnected) {
                    PhysicalSource.TOP_CAM
                } else {
                    PhysicalSource.FPV_CAM
                }
            } else if (topCamera != null && topCamera.isConnected) {
                mainVideoStream = PhysicalSource.TOP_CAM
                secondaryVideoStream = PhysicalSource.FPV_CAM
            } else {
                mainVideoStream = PhysicalSource.FPV_CAM
            }
        }

        val assignSourceKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.ASSIGN_SOURCE_TO_PRIMARY_CHANNEL)
        addDisposable(djiSdkModel.performAction(assignSourceKey,
                mainVideoStream, secondaryVideoStream)
                .observeOn(SchedulerProvider.io())
                .subscribe({ }, { error ->
                    if (error is UXSDKError) {
                        DJILog.e(TAG, "assign source to primary channel failure: $error")
                    }
                }))
    }

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
                    .subscribeOn(SchedulerProvider.io())
                    .subscribe(Action { setCameraChannelBandwidth() },
                            logErrorConsumer(TAG, "SetExtVideoInputPortEnabled: ")))
        } else {
            setCameraChannelBandwidth()
        }
    }

    private fun setCameraChannelBandwidth() {
        val bandwidthAllocationForLBVideoInputPort = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT)
        addDisposable(djiSdkModel.setValue(bandwidthAllocationForLBVideoInputPort, 0.0f)
                .subscribeOn(SchedulerProvider.io())
                .subscribe(Action {}, logErrorConsumer(TAG, "SetBandwidthAllocationForLBVideoInputPort: ")))
    }

    private fun updateCameraDisplay() {
        val displayName0Key = CameraKey.create(CameraKey.DISPLAY_NAME, 0)
        val displayName1Key = CameraKey.create(CameraKey.DISPLAY_NAME, 1)
        val displayName4Key = CameraKey.create(CameraKey.DISPLAY_NAME, 4)
        var displayName0 = djiSdkModel.getCacheValue(displayName0Key) as String?
        var displayName1 = djiSdkModel.getCacheValue(displayName1Key) as String?
        var displayName4 = djiSdkModel.getCacheValue(displayName4Key) as String?
        var displayName = ""
        var displaySide = CameraSide.UNKNOWN
        currentCameraIndex = CameraIndex.CAMERA_INDEX_UNKNOWN
        if (displayName0 == null) {
            displayName0 = PhysicalSource.UNKNOWN.toString()
        }
        if (displayName1 == null) {
            displayName1 = PhysicalSource.UNKNOWN.toString()
        }
        if (displayName4 == null) {
            displayName4 = PhysicalSource.UNKNOWN.toString()
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
                    } else if (physicalVideoSource == PhysicalSource.TOP_CAM) {
                        displayName = displayName4
                        displaySide = CameraSide.TOP
                        currentCameraIndex = CameraIndex.CAMERA_INDEX_4
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