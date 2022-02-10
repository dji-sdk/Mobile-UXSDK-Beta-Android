package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index
    private var lensType = SettingsDefinitions.LensType.ZOOM

    val exposureSettingsProcessor: DataProcessor<ExposureSettings> = DataProcessor.create(ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN, SettingsDefinitions.ShutterSpeed.UNKNOWN, 0, SettingsDefinitions.ExposureCompensation.UNKNOWN))
    val ISOProcessor: DataProcessor<SettingsDefinitions.ISO> = DataProcessor.create(SettingsDefinitions.ISO.UNKNOWN)
    val ISORangeProcessor: DataProcessor<Array<SettingsDefinitions.ISO?>> = DataProcessor.create(arrayOf())
    val exposureSensitivityModeProcessor: DataProcessor<SettingsDefinitions.ExposureSensitivityMode> = DataProcessor.create(SettingsDefinitions.ExposureSensitivityMode.UNKNOWN)
    val eiValueProcessor: DataProcessor<Int> = DataProcessor.create(0)
    val eiRecommendedValueProcessor: DataProcessor<Int> = DataProcessor.create(0)
    val eiValueRangeProcessor: DataProcessor<Array<Int>> = DataProcessor.create(arrayOf())
    val exposureModeProcessor: DataProcessor<SettingsDefinitions.ExposureMode> = DataProcessor.create(SettingsDefinitions.ExposureMode.UNKNOWN)
    val cameraModeProcessor: DataProcessor<SettingsDefinitions.CameraMode> = DataProcessor.create(SettingsDefinitions.CameraMode.UNKNOWN)
    val flatCameraModeProcessor: DataProcessor<SettingsDefinitions.FlatCameraMode> = DataProcessor.create(SettingsDefinitions.FlatCameraMode.UNKNOWN)

    override fun inSetup() {
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.ISO, cameraIndex, lensType.value()), ISOProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.EXPOSURE_SETTINGS, cameraIndex, lensType.value()), exposureSettingsProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.ISO_RANGE, cameraIndex, lensType.value()), ISORangeProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.EXPOSURE_SENSITIVITY_MODE, cameraIndex, lensType.value()), exposureSensitivityModeProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.EI_VALUE, cameraIndex, lensType.value()), eiValueProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.RECOMMENDED_EI_VALUE, cameraIndex, lensType.value()), eiRecommendedValueProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.EI_VALUE_RANGE, cameraIndex, lensType.value()), eiValueRangeProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.MODE, cameraIndex, lensType.value()), cameraModeProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.FLAT_CAMERA_MODE, cameraIndex, lensType.value()), flatCameraModeProcessor)
        bindDataProcessor(djiSdkModel.createLensKey(CameraKey.EXPOSURE_MODE, cameraIndex, lensType.value()), exposureModeProcessor)
    }

    override fun inCleanup() {

    }

    override fun getCameraIndex() = SettingDefinitions.CameraIndex.find(cameraIndex)

    override fun getLensType() = lensType

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) {
        this.cameraIndex = cameraIndex.index
        this.lensType = lensType
        restart()
    }

    fun setISO(iso: SettingsDefinitions.ISO): Completable {
        if (!djiSdkModel.isAvailable) {
            return Completable.complete()
        }
        val exposureModeKey = djiSdkModel.createLensKey(CameraKey.ISO, cameraIndex, lensType.value())
        return djiSdkModel.setValue(exposureModeKey, iso)
    }

    fun setEI(ei: Int): Completable {
        if (!djiSdkModel.isAvailable) {
            return Completable.complete()
        }
        val exposureModeKey = djiSdkModel.createLensKey(CameraKey.EI_VALUE, cameraIndex, lensType.value())
        return djiSdkModel.setValue(exposureModeKey, ei)
    }

    fun isEIEnable(): Boolean {
        return exposureSensitivityModeProcessor.value == SettingsDefinitions.ExposureSensitivityMode.EI
    }

    fun isRecordVideoEIMode(): Boolean {
        return ((!CameraUtil.isPictureMode(flatCameraModeProcessor.value) || cameraModeProcessor.value == SettingsDefinitions.CameraMode.RECORD_VIDEO)
                && isEIEnable())
    }
}