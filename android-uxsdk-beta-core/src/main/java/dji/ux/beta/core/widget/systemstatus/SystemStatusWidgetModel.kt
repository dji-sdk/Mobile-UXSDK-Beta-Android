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

package dji.ux.beta.core.widget.systemstatus

import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.keysdk.DJIKey
import dji.keysdk.DiagnosticsKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.model.VoiceNotificationType
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil

/**
 * Widget Model for the [SystemStatusWidget] used to define
 * the underlying logic and communication
 */
class SystemStatusWidgetModel(djiSdkModel: DJISDKModel,
                              private val keyedStore: ObservableInMemoryKeyedStore,
                              private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val systemStatusProcessor: DataProcessor<WarningStatusItem> = DataProcessor.create(WarningStatusItem.getDefaultItem())
    private val areMotorsOnDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val warningStatusMessageProcessor: DataProcessor<WarningStatusMessageData> = DataProcessor.create(WarningStatusMessageData("", 0f, UnitConversionUtil.UnitType.METRIC))
    private val sendVoiceNotificationKey: UXKey = UXKeys.create(MessagingKeys.SEND_VOICE_NOTIFICATION)
    //endregion

    //region Data
    /**
     * Get the system status of the aircraft as a WarningStatusItem.
     */
    val systemStatus: Flowable<WarningStatusItem>
        get() = systemStatusProcessor.toFlowable()

    /**
     * Get whether the motors are on.
     */
    val isMotorOn: Flowable<Boolean>
        get() = areMotorsOnDataProcessor.toFlowable()

    /**
     * Get the data required for displaying the warning status message.
     */
    val warningStatusMessageData: Flowable<WarningStatusMessageData>
        get() = warningStatusMessageProcessor.toFlowable()

    //endregion

    //region Actions
    /**
     * Send a voice notification.
     *
     * @return Completable representing the success/failure of the set action.
     */
    fun sendVoiceNotification(): Completable {
        val notificationType = VoiceNotificationType.ATTI
        return keyedStore.setValue(sendVoiceNotificationKey, notificationType)
    }
    //endregion

    //region LifeCycle
    override fun inSetup() {
        val systemStatusKey: DJIKey = DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS)
        bindDataProcessor(systemStatusKey, systemStatusProcessor)
        val areMotorsOnKey: DJIKey = FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON)
        bindDataProcessor(areMotorsOnKey, areMotorsOnDataProcessor)
        val maxHeightKey: DJIKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_IN_NFZ)
        bindDataProcessor(maxHeightKey, maxHeightProcessor)
        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitKey, unitTypeProcessor)

        preferencesManager?.setUpListener()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        warningStatusMessageProcessor.onNext(WarningStatusMessageData(
                systemStatusProcessor.value.message,
                getMaxHeight(maxHeightProcessor.value.toFloat(), unitTypeProcessor.value),
                unitTypeProcessor.value))
    }

    init {
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.unitType)
        }
    }
    //endregion

    //region Helpers
    private fun getMaxHeight(maxHeight: Float, unitType: UnitConversionUtil.UnitType): Float {
        return if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            UnitConversionUtil.convertMetersToFeet(maxHeight)
        } else {
            maxHeight
        }
    }
    //endregion

    //region Classes
    /**
     * Class representing data for displaying a warning status message
     */
    data class WarningStatusMessageData(
            /**
             * Warning Status Message
             */
            val message: String,

            /**
             * Max height of a height-limited no-fly zone
             */
            val maxHeight: Float,

            /**
             * Unit type for the height
             */
            val unitType: UnitConversionUtil.UnitType
    )
    //endregion
}