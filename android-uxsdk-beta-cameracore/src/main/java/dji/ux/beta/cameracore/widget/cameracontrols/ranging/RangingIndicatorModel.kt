package dji.ux.beta.cameracore.widget.cameracontrols.ranging

import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.RangeLaserKey
import dji.ux.beta.core.communication.UXKey
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.model.RangeEnable
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable

class RangingIndicatorModel(djiSdkModel: DJISDKModel, uxKeyManager: ObservableInMemoryKeyedStore) :
    WidgetModel(djiSdkModel, uxKeyManager) {
    var cameraIndex = 0
    var lensIndex = 0
    val isSupportLaserDataProcessor = DataProcessor.create(false)
    private val rangeLaserKey: UXKey = UXKeys.create(RangeLaserKey.RANGE_LASER_KEY)
    val laserEnableGoalDataProcessor = DataProcessor.create(RangeEnable.DISABLES)
    private lateinit var laserEnabledKey: DJIKey
    override fun inSetup() {
        laserEnabledKey = CameraKey.create(CameraKey.LASER_ENABLED, cameraIndex)
        bindDataSupportKeyProcessor(laserEnabledKey, isSupportLaserDataProcessor)
        val value = uxKeyManager.getValue(rangeLaserKey)
        value?.let {
            laserEnableGoalDataProcessor.onNext(value as RangeEnable)
        } ?: let {
            laserEnableGoalDataProcessor.onNext(RangeEnable.DISABLES)
        }

        bindDataProcessor(rangeLaserKey, laserEnableGoalDataProcessor)
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) setLaserEnable(RangeEnable.DISABLES)

    }

    override fun inCleanup() {

    }

    fun laserEnable(): Flowable<RangeEnable> {
        return laserEnableGoalDataProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread())
    }

    fun isSupportLaser(): Flowable<Boolean> {
        return isSupportLaserDataProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread())
    }

    fun setLaserEnable(laserEnable: RangeEnable) {
        addDisposable(djiSdkModel.setValue(laserEnabledKey, laserEnable == RangeEnable.ENABLE)
            .subscribe {
                uxKeyManager.setValue(rangeLaserKey, laserEnable).subscribe {
                }
            })
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