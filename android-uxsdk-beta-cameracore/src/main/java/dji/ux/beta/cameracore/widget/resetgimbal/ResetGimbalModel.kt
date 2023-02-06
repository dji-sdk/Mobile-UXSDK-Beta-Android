package dji.ux.beta.cameracore.widget.resetgimbal

import android.content.Context
import dji.common.gimbal.Axis
import dji.common.gimbal.ResetDirection
import dji.sdk.base.BaseProduct
import dji.sdk.gimbal.Gimbal
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex


class ResetGimbalModel constructor(
    private val context: Context,
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private var index = 0

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
        val gimbals = aircraftInstance?.gimbals
        if (gimbals != null) {
            for (gimbal in gimbals) {
                if (gimbal != null && gimbal.index == index) {
                    return gimbal
                }
            }
        }
        return null
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

    fun setGimbalIndex(gimbalIndex: GimbalIndex?) {
        if (gimbalIndex != null) {
            index = gimbalIndex.index
        }
        restart()
    }
}