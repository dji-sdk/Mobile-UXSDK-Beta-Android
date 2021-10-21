package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.widget.FrameLayout
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.LensType
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureModeSettingModel @JvmOverloads constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    val exposureModeProcessor: DataProcessor<SettingsDefinitions.ExposureMode> = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN)
    val exposureModeRangeProcessor: DataProcessor<Array<SettingsDefinitions.ExposureMode>> = DataProcessor.create(arrayOf())

    private var cameraIndex = CameraIndex.CAMERA_INDEX_0.index
    private var lensType = LensType.ZOOM

    /**
     *
     */
    val currentExposureMode: Flowable<SettingsDefinitions.ExposureMode> = exposureModeProcessor.toFlowable()

    /**
     *
     */
    val currentExposureModeRange: Flowable<Array<SettingsDefinitions.ExposureMode>> = exposureModeRangeProcessor.toFlowable()

    override fun inSetup() {
        val exposureModeKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value())
        val exposureModeRangeKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_MODE_RANGE, cameraIndex, lensType.value())

        bindDataProcessor(exposureModeKey, exposureModeProcessor)
        bindDataProcessor(exposureModeRangeKey, exposureModeRangeProcessor)
    }

    override fun inCleanup() {
    }

    override fun updateStates() {
    }

    override fun getCameraIndex(): CameraIndex {
        return CameraIndex.find(cameraIndex)
    }

    override fun setCameraIndex(cameraIndex: CameraIndex) {
        this.cameraIndex = cameraIndex.index
        restart()
    }

    override fun getLensType(): LensType {
        return lensType
    }

    override fun setLensType(lensType: LensType) {
        this.lensType = lensType
        restart()
    }

    fun setExposureMode(mode: SettingsDefinitions.ExposureMode): Completable {
        if (!djiSdkModel.isAvailable) {
            return Completable.complete()
        }
        val exposureModeKey = djiSdkModel.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value())
        return djiSdkModel.setValue(exposureModeKey, mode)
    }
}