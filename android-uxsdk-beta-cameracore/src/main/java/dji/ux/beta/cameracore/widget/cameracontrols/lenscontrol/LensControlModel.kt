package dji.ux.beta.cameracore.widget.cameracontrols.lenscontrol

import com.dji.frame.util.V_JsonUtil
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.LogUtil
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/13
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class LensControlModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index

    val cameraTypeProcessor: DataProcessor<SettingsDefinitions.CameraType> = DataProcessor.create(SettingsDefinitions.CameraType.OTHER)
    val cameraVideoStreamSourceProcessor: DataProcessor<CameraVideoStreamSource> = DataProcessor.create(CameraVideoStreamSource.UNKNOWN)
    val cameraVideoStreamSourceRangeProcessor: DataProcessor<Array<CameraVideoStreamSource>> = DataProcessor.create(arrayOf())

    override fun getCameraIndex() = SettingDefinitions.CameraIndex.find(cameraIndex)

    override fun getLensType() = SettingsDefinitions.LensType.UNKNOWN

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) {
        if (this.cameraIndex == cameraIndex.index){
            return
        }
        this.cameraIndex = cameraIndex.index
        restart()
    }

    override fun inSetup() {
        bindDataProcessor(CameraKey.create(CameraKey.CAMERA_TYPE, cameraIndex),cameraTypeProcessor)
        bindDataProcessor(CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE, cameraIndex),cameraVideoStreamSourceProcessor)
        bindDataProcessor(CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE_RANGE, cameraIndex),cameraVideoStreamSourceRangeProcessor)
    }

    override fun inCleanup() {
    }

    fun setCameraVideoStreamSource(source: CameraVideoStreamSource): Completable {
        if (!djiSdkModel.isAvailable) {
            return Completable.complete()
        }
        return djiSdkModel.setValue(CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE, cameraIndex), source)
    }
}