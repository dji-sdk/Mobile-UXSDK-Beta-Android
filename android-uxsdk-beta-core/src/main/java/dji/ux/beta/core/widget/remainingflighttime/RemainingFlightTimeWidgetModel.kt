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

package dji.ux.beta.core.widget.remainingflighttime

import dji.keysdk.BatteryKey
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Remaining Flight Time Widget Model
 *
 *
 * Widget Model for the [RemainingFlightTimeWidget] used to define the
 * underlying logic and communication
 */
class RemainingFlightTimeWidgetModel(djiSdkModel: DJISDKModel,
                                     keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val chargeRemainingProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val batteryNeededToLandProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val batteryNeededToGoHomeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val seriousLowBatteryThresholdProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val lowBatteryThresholdProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val remainingFlightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val remainingFlightTimeDataProcessor: DataProcessor<RemainingFlightTimeData> =
            DataProcessor.create(RemainingFlightTimeData(0, 0,
                    0, 0, 0, 0))
    private val isAircraftFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    //endregion

    override fun inSetup() {
        // For total percentage and flight time
        val chargeRemainingKey: DJIKey =
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT)
        bindDataProcessor(chargeRemainingKey, chargeRemainingProcessor)
        // For red bar
        val batteryNeededToLandKey: DJIKey =
                FlightControllerKey.create(FlightControllerKey.CURRENT_LAND_IMMEDIATELY_BATTERY)
        bindDataProcessor(batteryNeededToLandKey, batteryNeededToLandProcessor)
        // For H image and yellow bar
        val batteryNeededToGoHomeKey: DJIKey =
                FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME)
        bindDataProcessor(batteryNeededToGoHomeKey, batteryNeededToGoHomeProcessor)
        // For white dot on the left
        val seriousLowBatteryThresholdKey: DJIKey =
                FlightControllerKey.create(FlightControllerKey.SERIOUS_LOW_BATTERY_WARNING_THRESHOLD)
        bindDataProcessor(seriousLowBatteryThresholdKey, seriousLowBatteryThresholdProcessor)
        // For white dot on the right
        val lowBatteryThresholdKey: DJIKey =
                FlightControllerKey.create(FlightControllerKey.LOW_BATTERY_WARNING_THRESHOLD)
        bindDataProcessor(lowBatteryThresholdKey, lowBatteryThresholdProcessor)
        // For flight time text
        val remainingFlightTimeKey: DJIKey =
                FlightControllerKey.create(FlightControllerKey.REMAINING_FLIGHT_TIME)
        bindDataProcessor(remainingFlightTimeKey, remainingFlightProcessor)
        // To check if aircraft is flying
        val isFlyingKey: DJIKey = FlightControllerKey.create(FlightControllerKey.IS_FLYING)
        bindDataProcessor(isFlyingKey, isAircraftFlyingDataProcessor)
    }

    override fun inCleanup() { // No Clean up required
    }

    override fun updateStates() {
        val remainingFlightTimeData = RemainingFlightTimeData(
                chargeRemainingProcessor.value,
                batteryNeededToLandProcessor.value,
                batteryNeededToGoHomeProcessor.value,
                seriousLowBatteryThresholdProcessor.value,
                lowBatteryThresholdProcessor.value,
                remainingFlightProcessor.value)
        remainingFlightTimeDataProcessor.onNext(remainingFlightTimeData)
    }

    //region Data
    /**
     * Get the latest data for remaining flight based on battery level
     */
    val remainingFlightTimeData: Flowable<RemainingFlightTimeData>
        get() = remainingFlightTimeDataProcessor.toFlowable()

    /**
     * Check to see if aircraft is flying
     */
    val isAircraftFlying: Flowable<Boolean>
        get() = isAircraftFlyingDataProcessor.toFlowable()

    //endregion
    /**
     * Class representing data for remaining flight time
     */
    data class RemainingFlightTimeData(
            /**
             * Remaining battery charge in percent
             */
            val remainingCharge: Int,

            /**
             * Battery charge required to land
             */
            val batteryNeededToLand: Int,

            /**
             * Battery charge needed to go home
             */
            val batteryNeededToGoHome: Int,

            /**
             * Serious low battery level threshold
             */
            val seriousLowBatteryThreshold: Int,

            /**
             * Low battery level threshold
             */
            val lowBatteryThreshold: Int,

            /**
             * Flight time in micro seconds
             */
            val flightTime: Int)

}