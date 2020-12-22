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

package dji.ux.beta.core.widget.airsense

import dji.common.flightcontroller.adsb.AirSenseAirplaneState
import dji.common.flightcontroller.adsb.AirSenseWarningLevel
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.MessagingKeys
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKey
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.model.WarningMessage
import dji.ux.beta.core.model.WarningMessageError
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [AirSenseWidget] used to define
 * the underlying logic and communication
 */
class AirSenseWidgetModel @JvmOverloads constructor(
        djiSdkModel: DJISDKModel,
        private val keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val airSenseConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val airSenseWarningLevelProcessor: DataProcessor<AirSenseWarningLevel> = DataProcessor.create(AirSenseWarningLevel.UNKNOWN)
    private val airSenseAirplaneStatesProcessor: DataProcessor<Array<AirSenseAirplaneState>> = DataProcessor.create(emptyArray())
    private val sendWarningMessageKey: UXKey = UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE)
    private val airSenseStateProcessor: DataProcessor<AirSenseState> = DataProcessor.create(AirSenseState.DISCONNECTED)
    //endregion

    //region Data
    /**
     * Get the AirSense warning level.
     */
    val airSenseWarningLevel: Flowable<AirSenseWarningLevel>
        get() = airSenseWarningLevelProcessor.toFlowable().distinctUntilChanged()

    /**
     * Get the number of airplanes detected by AirSense
     */
    val airSenseState: Flowable<AirSenseState>
        get() = airSenseStateProcessor.toFlowable()
    //endregion

    //region Actions
    /**
     * Send two warning messages with the given solutions for warning and dangerous levels. Based
     * on the warning level, only one message at a time will be displayed, and the other will be
     * removed.
     *
     * @param reason The reason to display on the warning message.
     * @param warningSolution The solution to display if the level is [WarningMessage.Level.WARNING]
     * @param dangerousSolution The solution to display if the level is [WarningMessage.Level.DANGEROUS]
     * @param warningLevel The current AirSense warning level.
     * @return Completable representing the success/failure of the set action.
     */
    fun sendWarningMessages(reason: String?, warningSolution: String?, dangerousSolution: String?, warningLevel: AirSenseWarningLevel): Completable {
        return when (warningLevel) {
            AirSenseWarningLevel.LEVEL_2 -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.INSERT)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.REMOVE))
            AirSenseWarningLevel.LEVEL_3, AirSenseWarningLevel.LEVEL_4 -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.REMOVE)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.INSERT))
            else -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.REMOVE)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.REMOVE))
        }
    }

    private fun sendWarningMessage(reason: String?, solution: String?, level: WarningMessage.Level?, action: WarningMessage.Action?): Completable {
        val builder = WarningMessage.Builder(WarningMessage.WarningType.FLY_SAFE)
                .code(-1)
                .subCode(WarningMessageError.OTHER_AIRCRAFT_NEARBY.value())
                .reason(reason)
                .solution(solution)
                .level(level)
                .type(WarningMessage.Type.PINNED)
                .action(action)
        val warningMessage = builder.build()
        return keyedStore.setValue(sendWarningMessageKey, warningMessage)
    }

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val airSenseConnectedKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED)
        bindDataProcessor(airSenseConnectedKey, airSenseConnectedProcessor)
        val airSenseWarningLevelKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL)
        bindDataProcessor(airSenseWarningLevelKey, airSenseWarningLevelProcessor)
        val airSenseAirplaneStatesKey: DJIKey = FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES)
        bindDataProcessor(airSenseAirplaneStatesKey, airSenseAirplaneStatesProcessor)
    }

    override fun inCleanup() {
        // do nothing
    }

    override fun updateStates() {
        airSenseStateProcessor.onNext(
                if (!productConnectionProcessor.value) {
                    AirSenseState.DISCONNECTED
                } else if (!airSenseConnectedProcessor.value) {
                    AirSenseState.NO_AIR_SENSE_CONNECTED
                } else if (airSenseAirplaneStatesProcessor.value.isEmpty()) {
                    AirSenseState.NO_AIRPLANES_NEARBY
                } else {
                    when (airSenseWarningLevelProcessor.value) {
                        AirSenseWarningLevel.LEVEL_0 -> AirSenseState.WARNING_LEVEL_0
                        AirSenseWarningLevel.LEVEL_1 -> AirSenseState.WARNING_LEVEL_1
                        AirSenseWarningLevel.LEVEL_2 -> AirSenseState.WARNING_LEVEL_2
                        AirSenseWarningLevel.LEVEL_3 -> AirSenseState.WARNING_LEVEL_3
                        AirSenseWarningLevel.LEVEL_4 -> AirSenseState.WARNING_LEVEL_4
                        else -> AirSenseState.UNKNOWN
                    }
                }
        )
    }
    //endregion

    //region States
    /**
     * The status of the AirSense system.
     */
    enum class AirSenseState {
        /**
         * There is no product connected.
         */
        DISCONNECTED,

        /**
         * The connected product does not have DJI AirSense.
         */
        NO_AIR_SENSE_CONNECTED,

        /**
         * A product that has DJI AirSense is connected and no airplanes are nearby.
         */
        NO_AIRPLANES_NEARBY,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane but the
         * DJI aircraft is either far away from the airplane or is in the opposite direction of the
         * airplane's heading.
         */
        WARNING_LEVEL_0,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * low.
         */
        WARNING_LEVEL_1,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * medium.
         */
        WARNING_LEVEL_2,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * high.
         */
        WARNING_LEVEL_3,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is very high.
         */
        WARNING_LEVEL_4,

        /**
         * Unknown.
         */
        UNKNOWN
    }
    //endregion
}