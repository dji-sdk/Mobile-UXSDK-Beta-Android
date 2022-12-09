package dji.ux.beta.cameracore.widget.resetgimbal

import android.content.Context
import dji.common.camera.SettingsDefinitions
import dji.common.gimbal.Axis
import dji.common.gimbal.ResetDirection
import dji.keysdk.GimbalKey
import dji.sdk.base.BaseProduct
import dji.sdk.gimbal.Gimbal
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.SettingDefinitions


class ResetGimbalModel constructor(
    private val context: Context,
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {
    private lateinit var resetGimbalYawKey: GimbalKey
    private lateinit var resetGimbalKey: GimbalKey
    private var cameraIndex: SettingDefinitions.CameraIndex =
        SettingDefinitions.CameraIndex.CAMERA_INDEX_0
    private var lensType: SettingsDefinitions.LensType = SettingsDefinitions.LensType.WIDE


    override fun inSetup() {

    }

    fun setGimbalCenter() {
        val gimbal = getGimbal()
        gimbal?.reset(Axis.YAW_AND_PITCH, ResetDirection.CENTER) {
        }
    }

    fun setYawGimbalCenter() {
        val gimbal = getGimbal()
        gimbal?.reset(Axis.YAW, ResetDirection.CENTER) {
        }
    }

    fun setPithGimbalCenter() {
        val gimbal = getGimbal()
        gimbal?.reset(Axis.PITCH, ResetDirection.CENTER) {
        }
    }

    fun setPithGimbalYawPitchDown() {
        val gimbal = getGimbal()
        gimbal?.reset(Axis.YAW_AND_PITCH, ResetDirection.UP_OR_DOWN) {
        }
    }

    @Synchronized
    fun getGimbal(): Gimbal? {
        val aircraftInstance = getAircraftInstance()
        return aircraftInstance?.gimbal
    }

    @Synchronized
    fun getAircraftInstance(): Aircraft? {
        return if (!isAircraftConnected()) null else getProductInstance() as Aircraft
    }

    private fun isAircraftConnected(): Boolean {
        return getProductInstance() != null && getProductInstance() is Aircraft
    }


    @Synchronized
    fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    override fun inCleanup() {

    }


    override fun getCameraIndex(): SettingDefinitions.CameraIndex {
        return cameraIndex
    }

    override fun getLensType(): SettingsDefinitions.LensType {
        return lensType
    }

    override fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType

    }
}