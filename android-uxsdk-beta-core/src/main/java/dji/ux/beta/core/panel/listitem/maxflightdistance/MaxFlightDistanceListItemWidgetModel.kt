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

package dji.ux.beta.core.panel.listitem.maxflightdistance

import dji.common.util.DJIParamMinMaxCapability
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState.*
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil.*
import kotlin.math.roundToInt

/**
 * Widget Model for the [MaxFlightDistanceListItemWidget] used to define
 * the underlying logic and communication
 */
class MaxFlightDistanceListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val maxFlightDistanceKey: DJIKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS)
    private val maxFlightDistanceEnabledKey: DJIKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED)
    private val maxFlightDistanceEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxFlightDistanceProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val maxFlightDistanceRangeProcessor: DataProcessor<DJIParamMinMaxCapability> = DataProcessor.create(DJIParamMinMaxCapability(false, 0, 0))
    private val unitTypeProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val maxFlightDistanceStateProcessor: DataProcessor<MaxFlightDistanceState> = DataProcessor.create(ProductDisconnected)
    private val noviceModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    /**
     * Get the max flight distance state
     */
    val maxFlightDistanceState: Flowable<MaxFlightDistanceState>
        get() = maxFlightDistanceStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(maxFlightDistanceEnabledKey, maxFlightDistanceEnabledProcessor)
        bindDataProcessor(maxFlightDistanceKey, maxFlightDistanceProcessor)
        val maxFlightDistanceRangeKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE)
        bindDataProcessor(maxFlightDistanceRangeKey, maxFlightDistanceRangeProcessor)
        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeProcessor)
        val noviceModeEnabledKey = FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED)
        bindDataProcessor(noviceModeEnabledKey, noviceModeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when {
                noviceModeProcessor.value -> {
                    maxFlightDistanceStateProcessor.onNext(NoviceMode(unitTypeProcessor.value))
                }
                maxFlightDistanceEnabledProcessor.value -> {
                    maxFlightDistanceStateProcessor.onNext(MaxFlightDistanceValue(
                            flightDistanceLimit = getMaxFlightDistanceValue(),
                            minDistanceLimit = getMinLimit(),
                            maxDistanceLimit = getMaxLimit(),
                            unitType = unitTypeProcessor.value))

                }
                else -> {
                    maxFlightDistanceStateProcessor.onNext(Disabled)
                }
            }
        } else {
            maxFlightDistanceStateProcessor.onNext(ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    private fun getMaxFlightDistanceValue(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceProcessor.value.toFloat()).roundToInt()
        } else {
            maxFlightDistanceProcessor.value
        }
    }

    private fun getMinLimit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceRangeProcessor.value.min.toFloat()).roundToInt()
        } else {
            maxFlightDistanceRangeProcessor.value.min.toInt()
        }
    }

    private fun getMaxLimit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceRangeProcessor.value.max.toFloat()).roundToInt()
        } else {
            maxFlightDistanceRangeProcessor.value.max.toInt()
        }
    }

    /**
     * Enable or disable max flight distance
     *
     * @return Completable to determine status of action
     */
    fun toggleFlightDistanceAvailability(): Completable {
        return djiSdkModel.setValue(maxFlightDistanceEnabledKey, !maxFlightDistanceEnabledProcessor.value)
    }

    /**
     * Set max flight distance
     *
     * @return Completable to determine status of action
     */
    fun setMaxFlightDistance(flightDistance: Int): Completable {
        val tempFlightDistance: Int = if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertFeetToMeters(flightDistance.toFloat()).toInt()
        } else {
            flightDistance
        }
        return djiSdkModel.setValue(maxFlightDistanceKey, tempFlightDistance)

    }

    /**
     * Check if input is in range
     *
     * @return Boolean
     * true - if the input is in range
     * false - if the input is out of range
     */
    fun isInputInRange(input: Int): Boolean = input >= getMinLimit() && input <= getMaxLimit()

    /**
     * Class represents states of Max Flight Distance State
     */
    sealed class MaxFlightDistanceState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : MaxFlightDistanceState()

        /**
         * When max flight distance limit is disabled
         */
        object Disabled : MaxFlightDistanceState()

        /**
         * When product is in beginner mode
         * @property unitType - current unit system used
         */
        data class NoviceMode(val unitType: UnitType) : MaxFlightDistanceState()

        /**
         * Flight distance value with unit
         *
         * @property flightDistanceLimit - current flight distance limit
         * @property minDistanceLimit - flight distance limit range minimum
         * @property maxDistanceLimit - flight distance limit range maximum
         * @property unitType - current unit system used
         */
        data class MaxFlightDistanceValue(val flightDistanceLimit: Int,
                                          val minDistanceLimit: Int,
                                          val maxDistanceLimit: Int,
                                          val unitType: UnitType) : MaxFlightDistanceState()

    }

}