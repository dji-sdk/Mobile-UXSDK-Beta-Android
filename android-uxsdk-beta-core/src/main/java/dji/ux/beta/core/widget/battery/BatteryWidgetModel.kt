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

package dji.ux.beta.core.widget.battery

import androidx.annotation.IntRange
import dji.common.battery.AggregationState
import dji.common.battery.WarningRecord
import dji.common.flightcontroller.BatteryThresholdBehavior
import dji.keysdk.BatteryKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.milliVoltsToVolts
import dji.ux.beta.core.util.DataProcessor

private const val DUAL_BATTERY = 2
private const val M600_BATTERY = 6
private val DEFAULT_ARRAY = arrayOf(0)
private const val DEFAULT_PERCENTAGE = 0
private val DEFAULT_WARNING_RECORD = WarningRecord(
        false,
        false,
        false,
        false,
        false,
        -1,
        -1)


/**
 * Widget Model for [BatteryWidget] used to define the
 * underlying logic and communication
 */
class BatteryWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {


    private val batteryPercentageProcessor1 = DataProcessor.create(DEFAULT_PERCENTAGE)
    private val batteryPercentageProcessor2 = DataProcessor.create(DEFAULT_PERCENTAGE)

    private val batteryVoltageProcessor1 = DataProcessor.create(DEFAULT_ARRAY)
    private val batteryVoltageProcessor2 = DataProcessor.create(DEFAULT_ARRAY)

    private val batteryWarningRecordProcessor1 = DataProcessor.create(DEFAULT_WARNING_RECORD)
    private val batteryWarningRecordProcessor2 = DataProcessor.create(DEFAULT_WARNING_RECORD)

    private val batteryAggregationProcessor = DataProcessor.create(AggregationState.Builder().build())


    private val batteryStateProcessor: DataProcessor<BatteryState> = DataProcessor.create(BatteryState.DisconnectedState)
    private val batteryThresholdBehaviorProcessor = DataProcessor.create(BatteryThresholdBehavior.UNKNOWN)
    private val batteryNeededToGoHomeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val isAircraftFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    /**
     * Get the current state of the battery of the connected product
     */
    val batteryState: Flowable<BatteryState>
        get() = batteryStateProcessor.toFlowable()

    override fun inSetup() {
        val battery1PercentKey = BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0)
        val battery1voltageKey = BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0)
        val battery1warningRecordKey = BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0)
        bindDataProcessor(battery1PercentKey, batteryPercentageProcessor1)
        bindDataProcessor(battery1voltageKey, batteryVoltageProcessor1)
        bindDataProcessor(battery1warningRecordKey, batteryWarningRecordProcessor1)


        val battery2PercentKey = BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 1)
        val battery2voltageKey = BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1)
        val battery2warningRecordKey = BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1)
        bindDataProcessor(battery2PercentKey, batteryPercentageProcessor2)
        bindDataProcessor(battery2voltageKey, batteryVoltageProcessor2)
        bindDataProcessor(battery2warningRecordKey, batteryWarningRecordProcessor2)

        val batteryAggregationKey = BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE)
        bindDataProcessor(batteryAggregationKey, batteryAggregationProcessor)

        val batteryThresholdBehaviorKey = FlightControllerKey.create(FlightControllerKey.BATTERY_THRESHOLD_BEHAVIOR)
        bindDataProcessor(batteryThresholdBehaviorKey, batteryThresholdBehaviorProcessor)
        val batteryNeededToGoHomeKey = FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME)
        bindDataProcessor(batteryNeededToGoHomeKey, batteryNeededToGoHomeProcessor)
        val isFlyingKey = FlightControllerKey.create(FlightControllerKey.IS_FLYING)
        bindDataProcessor(isFlyingKey, isAircraftFlyingDataProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when (batteryAggregationProcessor.value.numberOfConnectedBatteries) {
                M600_BATTERY -> {
                    batteryStateProcessor.onNext(BatteryState.AggregateBatteryState(
                            batteryAggregationProcessor.value.chargeRemainingInPercent,
                            batteryAggregationProcessor.value.voltage.toFloat().milliVoltsToVolts(),
                            calculateAggregateBatteryStatus(batteryAggregationProcessor.value)
                    ))
                }
                DUAL_BATTERY -> {
                    val battery1Voltage = calculateAverageVoltage(batteryVoltageProcessor1.value)
                    val battery2Voltage = calculateAverageVoltage(batteryVoltageProcessor2.value)
                    batteryStateProcessor.onNext(BatteryState.DualBatteryState(
                            batteryPercentageProcessor1.value,
                            battery1Voltage,
                            calculateBatteryStatus(batteryWarningRecordProcessor1.value,
                                    batteryThresholdBehaviorProcessor.value,
                                    batteryPercentageProcessor1.value,
                                    batteryNeededToGoHomeProcessor.value,
                                    isAircraftFlyingDataProcessor.value,
                                    battery1Voltage),
                            batteryPercentageProcessor2.value,
                            battery2Voltage,
                            calculateBatteryStatus(batteryWarningRecordProcessor2.value,
                                    batteryThresholdBehaviorProcessor.value,
                                    batteryPercentageProcessor2.value,
                                    batteryNeededToGoHomeProcessor.value,
                                    isAircraftFlyingDataProcessor.value,
                                    battery2Voltage)
                    ))
                }
                else -> {
                    val voltage = calculateAverageVoltage(batteryVoltageProcessor1.value)
                    batteryStateProcessor.onNext(BatteryState.SingleBatteryState(
                            batteryPercentageProcessor1.value,
                            voltage,
                            calculateBatteryStatus(batteryWarningRecordProcessor1.value,
                                    batteryThresholdBehaviorProcessor.value,
                                    batteryPercentageProcessor1.value,
                                    batteryNeededToGoHomeProcessor.value,
                                    isAircraftFlyingDataProcessor.value,
                                    voltage)
                    ))
                }

            }
        } else {
            batteryStateProcessor.onNext(BatteryState.DisconnectedState)
            batteryAggregationProcessor.onNext(AggregationState.Builder().build())
        }
    }


    override fun inCleanup() {
        // No Code
    }

    private fun calculateAggregateBatteryStatus(aggregationState: AggregationState): BatteryStatus {
        // Default to returning a normal status.
        var priorityStatus = BatteryStatus.NORMAL

        // Check for aggregate state.
        if (aggregationState.isAnyBatteryDisconnected
                || aggregationState.isCellDamaged
                || aggregationState.isFirmwareDifferenceDetected
                || aggregationState.isVoltageDifferenceDetected
                || aggregationState.isLowCellVoltageDetected) {
            priorityStatus = BatteryStatus.ERROR
        }

        // Iterate through all active batteries to find highest priority status.
        for (i in 0 until aggregationState.numberOfConnectedBatteries) {
            val currentWarningRecord: WarningRecord = djiSdkModel.getCacheValue(
                    BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, i)) as WarningRecord
            val currentVoltage: Float = calculateAverageVoltage(djiSdkModel.getCacheValue(
                    BatteryKey.create(BatteryKey.CELL_VOLTAGES, i)) as Array<Int>)
            val currentBatteryStatus = calculateBatteryStatus(currentWarningRecord,
                    batteryThresholdBehaviorProcessor.value,
                    aggregationState.batteryOverviews[i].chargeRemainingInPercent,
                    batteryNeededToGoHomeProcessor.value,
                    isAircraftFlyingDataProcessor.value,
                    currentVoltage)

            if (currentBatteryStatus > priorityStatus) {
                priorityStatus = currentBatteryStatus
            }
        }

        return priorityStatus
    }

    private fun calculateAverageVoltage(cellVoltages: Array<Int>?): Float {
        return if (cellVoltages != null && cellVoltages.isNotEmpty()) {
            cellVoltages.average().toFloat().milliVoltsToVolts()
        } else 0f

    }

    private fun calculateBatteryStatus(warningRecord: WarningRecord,
                                       batteryThresholdBehavior: BatteryThresholdBehavior,
                                       percentage: Int,
                                       goHomeBattery: Int,
                                       isFlying: Boolean,
                                       voltage: Float): BatteryStatus {
        if (percentage < 0 || voltage < 0f) {
            return BatteryStatus.UNKNOWN
        } else if (warningRecord.isOverHeated) {
            return BatteryStatus.OVERHEATING
        } else if (warningRecord.hasError()) {
            return BatteryStatus.ERROR
        } else if (BatteryThresholdBehavior.LAND_IMMEDIATELY == batteryThresholdBehavior) {
            return BatteryStatus.WARNING_LEVEL_2
        } else if (BatteryThresholdBehavior.GO_HOME == batteryThresholdBehavior
                || (percentage <= goHomeBattery && isFlying)) {
            return BatteryStatus.WARNING_LEVEL_1
        }
        return BatteryStatus.NORMAL
    }

    /**
     * Class representing the current state of the battery
     * based on information received from the product
     */
    sealed class BatteryState {
        /**
         * Product is currently disconnected
         */
        object DisconnectedState : BatteryState()

        /**
         * Product with single battery is connected. The status includes
         *
         * @property percentageRemaining - battery remaining in percentage
         * @property voltageLevel - voltage level of the battery
         * @property batteryStatus - [BatteryStatus] instance representing the battery
         */
        data class SingleBatteryState(val percentageRemaining: Int,
                                      val voltageLevel: Float,
                                      val batteryStatus: BatteryStatus) : BatteryState()

        /**
         * Product with dual battery is connected. The status includes
         *
         * @property percentageRemaining1 - battery remaining in percentage of battery 1
         * @property voltageLevel1 - voltage level of the battery 1
         * @property batteryStatus1 - [BatteryStatus] instance representing the battery 1
         * @property percentageRemaining2 - battery remaining in percentage of battery 2
         * @property voltageLevel2 - voltage level of the battery 2
         * @property batteryStatus2 - [BatteryStatus] instance representing the battery 2
         */
        data class DualBatteryState(val percentageRemaining1: Int,
                                    val voltageLevel1: Float,
                                    val batteryStatus1: BatteryStatus,
                                    val percentageRemaining2: Int,
                                    val voltageLevel2: Float,
                                    val batteryStatus2: BatteryStatus) : BatteryState()

        /**
         * Product with more than 2 batteries is connected. The status includes
         *
         * @property aggregatePercentage - aggregate percentage remaining from all batteries
         * @property aggregateVoltage - aggregate voltage level of all batteries
         * @property aggregateBatteryStatus - [BatteryStatus] instance representing the aggregate status
         */
        data class AggregateBatteryState(val aggregatePercentage: Int,
                                         val aggregateVoltage: Float,
                                         val aggregateBatteryStatus: BatteryStatus) : BatteryState()
    }

    //endregion


    /**
     * Enum representing the state of each battery in the battery bank
     */
    enum class BatteryStatus constructor(val index: Int) {
        /**
         * Battery is operating without issue
         */
        NORMAL(0),

        /**
         * Battery charge is starting to get low, to the point that the aircraft should return home
         */
        WARNING_LEVEL_1(1),

        /**
         * Battery charge is starting to get very low, to the point that the aircraft should
         * land immediately.
         */
        WARNING_LEVEL_2(2),

        /**
         * Battery has an error that is preventing a proper reading
         */
        ERROR(3),

        /**
         * Battery temperature is too high
         */
        OVERHEATING(4),

        /**
         * The state of the battery is unknown or the system is initializing
         */
        UNKNOWN(5);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(@IntRange(from = 0, to = 5) index: Int): BatteryStatus {
                return values.find { it.index == index } ?: UNKNOWN
            }
        }
    }


}