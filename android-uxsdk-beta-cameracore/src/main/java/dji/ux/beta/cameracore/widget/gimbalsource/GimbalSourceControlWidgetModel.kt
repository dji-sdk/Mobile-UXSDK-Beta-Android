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

package dji.ux.beta.cameracore.widget.gimbalsource

import dji.common.airlink.PhysicalSource
import dji.common.product.Model
import dji.keysdk.AirLinkKey
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.keysdk.ProductKey
import dji.sdk.camera.VideoFeeder
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.BiFunction
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.ProductUtil

private const val TAG = "GimbalSrcControlWidMod"

/**
 * Gimbal Source Control Widget Model
 *
 * Widget Model for [GimbalSourceControlWidget] used to define the
 * underlying logic and communication
 */
class GimbalSourceControlWidgetModel(djiSdkModel: DJISDKModel,
                                     keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val lbBandwidthKey: DJIKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT)
    private val leftCameraBandwidthKey: DJIKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LEFT_CAMERA)
    private val mainCameraBandwidthKey: DJIKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_PRIMARY_VIDEO)
    private val assignPrimarySourceKey: DJIKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.ASSIGN_SOURCE_TO_PRIMARY_CHANNEL)
    private val portCameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val starboardCameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val topCameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val portCameraDisplayNameDataProcessor: DataProcessor<String> = DataProcessor.create("")
    private val starboardCameraDisplayNameDataProcessor: DataProcessor<String> = DataProcessor.create("")
    private val topCameraDisplayNameDataProcessor: DataProcessor<String> = DataProcessor.create("")
    private val modelProcessor: DataProcessor<Model> = DataProcessor.create(Model.UNKNOWN_AIRCRAFT)
    private val gimbalSourceStateProcessor: DataProcessor<GimbalSourceState> =
            DataProcessor.create(GimbalSourceState(PhysicalSource.UNKNOWN, PhysicalSource.UNKNOWN))

    /**
     * Connection state of the port camera
     */
    val portCameraConnectionState: Flowable<CameraConnectionState>
        get() = Flowable.combineLatest(portCameraConnectionDataProcessor.toFlowable(),
                portCameraDisplayNameDataProcessor.toFlowable(),
                BiFunction { isConnected: Boolean,
                             displayName: String ->
                    CameraConnectionState(isConnected, displayName, PhysicalSource.LEFT_CAM)
                })

    /**
     * Connection state of the starboard camera
     */
    val starboardCameraConnectionState: Flowable<CameraConnectionState>
        get() = Flowable.combineLatest(starboardCameraConnectionDataProcessor.toFlowable(),
                starboardCameraDisplayNameDataProcessor.toFlowable(),
                BiFunction { isConnected: Boolean,
                             displayName: String ->
                    CameraConnectionState(isConnected, displayName, PhysicalSource.RIGHT_CAM)
                })

    /**
     * Connection state of the top camera
     */
    val topCameraConnectionState: Flowable<CameraConnectionState>
        get() = Flowable.combineLatest(topCameraConnectionDataProcessor.toFlowable(),
                topCameraDisplayNameDataProcessor.toFlowable(),
                BiFunction { isConnected: Boolean,
                             displayName: String ->
                    CameraConnectionState(isConnected, displayName, PhysicalSource.TOP_CAM)
                })

    /**
     * Value of the gimbal source state
     */
    val gimbalSourceState: Flowable<GimbalSourceState>
        get() = gimbalSourceStateProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {
        val portCameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, 0)
        bindDataProcessor(portCameraConnectionKey, portCameraConnectionDataProcessor)
        val starboardCameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, 1)
        bindDataProcessor(starboardCameraConnectionKey, starboardCameraConnectionDataProcessor)
        val topCameraConnectionKey: DJIKey = CameraKey.create(CameraKey.CONNECTION, 4)
        bindDataProcessor(topCameraConnectionKey, topCameraConnectionDataProcessor)

        val portCameraDisplayNameKey: DJIKey = CameraKey.create(CameraKey.DISPLAY_NAME, 0)
        bindDataProcessor(portCameraDisplayNameKey, portCameraDisplayNameDataProcessor)
        val starboardCameraDisplayNameKey: DJIKey = CameraKey.create(CameraKey.DISPLAY_NAME, 1)
        bindDataProcessor(starboardCameraDisplayNameKey, starboardCameraDisplayNameDataProcessor)
        val topCameraDisplayNameKey: DJIKey = CameraKey.create(CameraKey.DISPLAY_NAME, 4)
        bindDataProcessor(topCameraDisplayNameKey, topCameraDisplayNameDataProcessor)

        val videoViewChangedConsumer = Consumer { _: Any? -> updateVideoFeed() }
        addDisposable(djiSdkModel.addListener(mainCameraBandwidthKey, this)
                .subscribe(videoViewChangedConsumer, logErrorConsumer(TAG, "Error listening to main camera bandwidth key ")))
        addDisposable(djiSdkModel.addListener(lbBandwidthKey, this)
                .subscribe(videoViewChangedConsumer, logErrorConsumer(TAG, "Error listening to lb bandwidth key ")))
        addDisposable(djiSdkModel.addListener(leftCameraBandwidthKey, this)
                .subscribe(videoViewChangedConsumer, logErrorConsumer(TAG, "Error listening to left camera bandwidth key ")))

        val modelKey: DJIKey = ProductKey.create(ProductKey.MODEL_NAME)
        bindDataProcessor(modelKey, modelProcessor)

        getVideoFeeder()?.let {
            //If either video feed is already initialized, get video source
            updateVideoFeed()
            //If primary video feed is not yet initialized, wait for active status
            it.primaryVideoFeed
                    .addVideoActiveStatusListener { updateVideoFeed() }
            //If secondary video feed is not yet initialized, wait for active status
            it.secondaryVideoFeed
                    .addVideoActiveStatusListener { updateVideoFeed() }
        }
    }

    override fun inCleanup() {
        // No clean up needed
    }

    override fun updateStates() {
        // No states to update
    }
    //endregion

    //region Actions
    /**
     * Set the primary and secondary video sources to the given [PhysicalSource] values.
     * Matrice 300 only: If the list contains one value, only the primary video source will be set.
     */
    fun setGimbalSources(gimbalSources: List<PhysicalSource>): Completable {
        if (gimbalSources.isEmpty()) {
            return Completable.error(Throwable("Gimbal source list cannot be empty"))
        }

        val primarySource = gimbalSources[0]
        val secondarySource = if (gimbalSources.size > 1) gimbalSources[1] else null

        return if (ProductUtil.isM200V2OrM300(modelProcessor.value)) {
            if (secondarySource != null) {
                Completable.mergeArray(
                        djiSdkModel.performAction(assignPrimarySourceKey,
                                primarySource, secondarySource),
                        setMainCameraBandwidth(primarySource, secondarySource)
                )
            } else {
                if (modelProcessor.value == Model.MATRICE_300_RTK) {
                    djiSdkModel.performAction(assignPrimarySourceKey, primarySource)
                } else {
                    Completable.error(Throwable("Single gimbal source only supported on Matrice 300"))
                }
            }
        } else if (primarySource == PhysicalSource.LEFT_CAM) {
            if (secondarySource == PhysicalSource.FPV_CAM) {
                portAndFpv()
            } else if (secondarySource == PhysicalSource.RIGHT_CAM) {
                portAndStarboard()
            } else {
                // port only
                Completable.error(Throwable("Single gimbal source only supported on Matrice 300"))
            }
        } else if (primarySource == PhysicalSource.RIGHT_CAM) {
            if (secondarySource == PhysicalSource.FPV_CAM) {
                starboardAndFpv()
            } else {
                // starboard only
                Completable.error(Throwable("Single gimbal source only supported on Matrice 300"))
            }
        } else {
            // FPV only
            Completable.error(Throwable("Single gimbal source only supported on Matrice 300"))
        }
    }
    //endregion

    //region Helpers
    private fun updateVideoFeed() {
        getVideoFeeder()?.let {
            val primaryPhysicalSource: PhysicalSource? = it.primaryVideoFeed.videoSource
            val secondaryPhysicalSource: PhysicalSource? = it.secondaryVideoFeed.videoSource
            gimbalSourceStateProcessor.onNext(GimbalSourceState(primaryPhysicalSource, secondaryPhysicalSource))
        }
    }

    private fun portAndFpv(): Completable {
        return if (ProductUtil.isM200V2OrM300(modelProcessor.value)) {
            Completable.mergeArray(djiSdkModel.performAction(assignPrimarySourceKey,
                    PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM),
                    djiSdkModel.setValue(mainCameraBandwidthKey, 1.0f))
        } else {
            Completable.mergeArray(djiSdkModel.setValue(lbBandwidthKey, 0.8f),
                    djiSdkModel.setValue(leftCameraBandwidthKey, 1.0f))
        }
    }

    private fun starboardAndFpv(): Completable {
        return if (ProductUtil.isM200V2OrM300(modelProcessor.value)) {
            Completable.mergeArray(djiSdkModel.performAction(assignPrimarySourceKey,
                    PhysicalSource.RIGHT_CAM, PhysicalSource.FPV_CAM),
                    djiSdkModel.setValue(mainCameraBandwidthKey, 0.0f))
        } else {
            Completable.mergeArray(djiSdkModel.setValue(lbBandwidthKey, 0.8f),
                    djiSdkModel.setValue(leftCameraBandwidthKey, 0.0f))
        }
    }

    private fun portAndStarboard(): Completable {
        return if (ProductUtil.isM200V2OrM300(modelProcessor.value)) {
            Completable.mergeArray(djiSdkModel.performAction(assignPrimarySourceKey,
                    PhysicalSource.LEFT_CAM, PhysicalSource.RIGHT_CAM),
                    djiSdkModel.setValue(mainCameraBandwidthKey, 0.5f))
        } else {
            Completable.mergeArray(djiSdkModel.setValue(lbBandwidthKey, 1.0f),
                    djiSdkModel.setValue(leftCameraBandwidthKey, 0.5f))
        }
    }

    private fun setMainCameraBandwidth(primarySource: PhysicalSource, secondarySource: PhysicalSource): Completable {
        return if (primarySource == PhysicalSource.LEFT_CAM && secondarySource == PhysicalSource.FPV_CAM) {
            djiSdkModel.setValue(mainCameraBandwidthKey, 1.0f)
        } else if (primarySource == PhysicalSource.RIGHT_CAM && secondarySource == PhysicalSource.FPV_CAM) {
            djiSdkModel.setValue(mainCameraBandwidthKey, 0.0f)
        } else if (primarySource == PhysicalSource.LEFT_CAM && secondarySource == PhysicalSource.RIGHT_CAM) {
            djiSdkModel.setValue(mainCameraBandwidthKey, 0.5f)
        } else {
            Completable.complete()
        }
    }
    //endregion

    //region Helpers for unit testing
    private fun getVideoFeeder(): VideoFeeder? {
        return VideoFeeder.getInstance()
    }
    //endregion

    data class GimbalSourceState(val primarySource: PhysicalSource?,
                                 val secondarySource: PhysicalSource?)

    data class CameraConnectionState(val connected: Boolean,
                                     val displayName: String,
                                     val physicalSource: PhysicalSource)
}