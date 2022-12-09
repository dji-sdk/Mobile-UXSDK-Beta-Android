package dji.ux.beta.cameracore.widget.cameracontrols.ranging

import android.graphics.PointF
import dji.common.camera.LaserError
import dji.common.camera.LaserMeasureInformation
import dji.common.camera.SettingsDefinitions
import dji.common.camera.ThermalMeasurementMode
import dji.common.flightcontroller.LocationCoordinate3D
import dji.keysdk.CameraKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.model.RangeEnable
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable

class RangingModel(djiSdkModel: DJISDKModel, uxKeyManager: ObservableInMemoryKeyedStore) :
    WidgetModel(djiSdkModel, uxKeyManager) {
    var cameraIndex = 0
    var lensIndex = 0
    private val laserMeasureInformationDataProcessor =
        DataProcessor.create(LaserMeasureInformation.Builder().setTargetDistance(0f)
            .setTargetPoint(PointF(0f, 0f))
            .setLaserError(LaserError.TOO_CLOSE).setTargetLocation(
                LocationCoordinate3D(0.0, 0.0, 0f)).build())

    val laserEnableDataProcessor = DataProcessor.create(RangeEnable.DISABLES)
    private var thermalMeasurementModeDataProcessor =
        DataProcessor.create(ThermalMeasurementMode.UNKNOWN)

    override fun inSetup() {
        thermalMeasurementModeDataProcessor.onNext(ThermalMeasurementMode.UNKNOWN)
        val laserMeasureInformationKey =
            djiSdkModel.createLensKey(CameraKey.LASER_MEASURE_INFORMATION, cameraIndex, lensIndex)
        bindDataProcessor(laserMeasureInformationKey, laserMeasureInformationDataProcessor)

        val thermalMeasurementModeKey =
            djiSdkModel.createLensKey(CameraKey.THERMAL_MEASUREMENT_MODE, cameraIndex, lensIndex)
        bindDataProcessor(thermalMeasurementModeKey, thermalMeasurementModeDataProcessor)

        val laserEnabledKey = UXKeys.create(RangeLaserKey.RANGE_LASER_KEY)
        val laserEnableValue = uxKeyManager.getValue(laserEnabledKey) ?: RangeEnable.DISABLES
        laserEnableDataProcessor.onNext(laserEnableValue)
        bindDataProcessor(laserEnabledKey, laserEnableDataProcessor)

    }

    fun getThermalMeasurementMode(): Flowable<ThermalMeasurementMode> {
        return thermalMeasurementModeDataProcessor.toFlowable().observeOn(SchedulerProvider.ui())
    }

    fun isThermalMeasurementMode(): Boolean {
        return thermalMeasurementModeDataProcessor.value == ThermalMeasurementMode.AREA_METERING
    }


    fun getLaserEnableData(): Flowable<RangeEnable> {
        return laserEnableDataProcessor.toFlowable().observeOn(SchedulerProvider.ui())
    }


    override fun inCleanup() {

    }

    fun getterMeasureInformation(): Flowable<LaserMeasureInformation> {
        return laserMeasureInformationDataProcessor.toFlowable()
            .observeOn(AndroidSchedulers.mainThread())
    }


    fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        this.cameraIndex = cameraIndex.index
        this.lensIndex = lensType.value()
        restart()
    }

}