package dji.ux.beta.cameracore.widget.cameracontrols.cameraswitch

import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.core.Flowable

class CameraIRSwitchModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    var lensIndex = 0
    var cameraIndex = 0
    val disPlayModelDataProcessor =
        DataProcessor.create(SettingsDefinitions.DisplayMode.OTHER)
    private val cameraTypeDataProcessor = DataProcessor.create(SettingsDefinitions.CameraType.OTHER)
    val isIRCameraVideoSourceDataProcessor = DataProcessor.create(false)
    var disPlayModelKey: DJIKey? = null

    override fun inSetup() {
        disPlayModelKey = djiSdkModel.createLensKey(CameraKey.DISPLAY_MODE, cameraIndex, lensIndex)
        bindDataProcessor(disPlayModelKey!!, disPlayModelDataProcessor)
        val cameraTypeKey = CameraKey.create(CameraKey.CAMERA_TYPE,cameraIndex)
        bindDataProcessor(cameraTypeKey, cameraTypeDataProcessor)
    }

    fun getDisPlayModel(): Flowable<SettingsDefinitions.DisplayMode> {
        return disPlayModelDataProcessor.toFlowable()
    }

    fun getCameraTypeModel(): Flowable<SettingsDefinitions.CameraType> {
        return cameraTypeDataProcessor.toFlowable()
    }

    fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        this.cameraIndex = cameraIndex.index
        this.lensIndex = lensType.value()
        val irVideoCameraSource = CameraUtil.isIRVideoCameraSource(lensType)
        isIRCameraVideoSourceDataProcessor.onNext(irVideoCameraSource)
        restart()
    }


    fun setDisPlayMode(disPlayMode: SettingsDefinitions.DisplayMode) {
        var disPlayModeComparable = djiSdkModel.setValue(disPlayModelKey!!, disPlayMode)
        if (disPlayMode == SettingsDefinitions.DisplayMode.PIP) {
            val pipPositionCameraKey = djiSdkModel.createLensKey(
                CameraKey.PIP_POSITION,
                cameraIndex,
                lensIndex
            )
            disPlayModeComparable = disPlayModeComparable.andThen(
                djiSdkModel.setValue(
                    pipPositionCameraKey,
                    SettingsDefinitions.PIPPosition.SIDE_BY_SIDE
                )
            )
        }
        addDisposable(disPlayModeComparable.subscribe {

        })
    }

    override fun inCleanup() {

    }


}