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

package dji.ux.beta.core.panel.listitem.returntohomealtitude

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
import dji.ux.beta.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.*
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil.*
import kotlin.math.roundToInt

private const val MIN_LIMIT = 20
private const val MAX_LIMIT = 500

/**
 * Widget Model for the [ReturnToHomeAltitudeListItemWidget] used to define
 * the underlying logic and communication
 */
class ReturnToHomeAltitudeListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val maxFlightAltitudeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val returnToHomeAltitudeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val unitTypeProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val returnToHomeAltitudeStateProcessor: DataProcessor<ReturnToHomeAltitudeState> = DataProcessor.create(ProductDisconnected)
    private val returnToHomeAltitudeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS)
    private val noviceModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxFlightHeightRangeProcessor: DataProcessor<DJIParamMinMaxCapability> = DataProcessor.create(DJIParamMinMaxCapability(false, 0, 0))

    /**
     * Get the return to home altitude state
     */
    val returnToHomeAltitudeState: Flowable<ReturnToHomeAltitudeState>
        get() = returnToHomeAltitudeStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(returnToHomeAltitudeKey, returnToHomeAltitudeProcessor)
        val maxFlightAltitudeKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT)
        bindDataProcessor(maxFlightAltitudeKey, maxFlightAltitudeProcessor)
        val maxFlightRangeKey = FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE)
        bindDataProcessor(maxFlightRangeKey, maxFlightHeightRangeProcessor)
        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeProcessor)
        val noviceModeEnabledKey = FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED)
        bindDataProcessor(noviceModeEnabledKey, noviceModeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (noviceModeProcessor.value) {
                returnToHomeAltitudeStateProcessor.onNext(NoviceMode(unitTypeProcessor.value))
            } else {
                returnToHomeAltitudeStateProcessor.onNext(
                        ReturnToHomeAltitudeValue(returnToHomeAltitude = getReturnToHomeAltitudeValue(),
                                minLimit = getMinLimit(),
                                maxLimit = getMaxLimit(),
                                unitType = unitTypeProcessor.value,
                                maxFlightAltitude = getMaxAltitudeLimitByUnit()))
            }
        } else {
            returnToHomeAltitudeStateProcessor.onNext(ProductDisconnected)
        }
    }

    private fun getMaxAltitudeLimitByUnit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightAltitudeProcessor.value.toFloat()).roundToInt()
        } else {
            maxFlightAltitudeProcessor.value
        }
    }


    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    private fun getReturnToHomeAltitudeValue(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(returnToHomeAltitudeProcessor.value.toFloat()).roundToInt()
        } else {
            returnToHomeAltitudeProcessor.value
        }
    }

    private fun getMinLimit(): Int {
        val tempMinValue: Int = if (maxFlightHeightRangeProcessor.value.isSupported) {
            maxFlightHeightRangeProcessor.value.min.toInt()
        } else {
            MIN_LIMIT
        }
        return if (unitTypeProcessor.value == UnitType.METRIC) {
            tempMinValue
        } else {
            convertMetersToFeet(tempMinValue.toFloat()).roundToInt()
        }
    }

    private fun getMaxLimit(): Int {
        val tempMaxValue: Int = if (maxFlightHeightRangeProcessor.value.isSupported) {
            maxFlightHeightRangeProcessor.value.max.toInt()
        } else {
            MAX_LIMIT
        }
        return if (unitTypeProcessor.value == UnitType.METRIC) {
            tempMaxValue
        } else {
            convertMetersToFeet(tempMaxValue.toFloat()).roundToInt()
        }
    }

    /**
     * Set return to home altitude
     *
     * @return Completable to determine status of action
     */
    fun setReturnToHomeAltitude(returnToHomeAltitude: Int): Completable {
        val tempAltitude: Int = if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertFeetToMeters(returnToHomeAltitude.toFloat()).toInt()
        } else {
            returnToHomeAltitude
        }
        return djiSdkModel.setValue(returnToHomeAltitudeKey, tempAltitude)
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
     * Class represents states of Return To Home Altitude List Item
     */
    sealed class ReturnToHomeAltitudeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : ReturnToHomeAltitudeState()

        /**
         * When product is in beginner mode
         * @property unitType - current unit system used
         */
        data class NoviceMode(val unitType: UnitType) : ReturnToHomeAltitudeState()


        /**
         * Return to home value and range
         * along with unit
         * @property returnToHomeAltitude - Return to home altitude
         * @property minLimit - Minimum limit of return to home altitude
         * @property maxLimit - Maximum limit of return to home altitude
         * @property unitType - Unit of values
         * @property maxFlightAltitude - Maximum permitted flight altitude.
         */
        data class ReturnToHomeAltitudeValue(val returnToHomeAltitude: Int,
                                             val minLimit: Int,
                                             val maxLimit: Int,
                                             val unitType: UnitType,
                                             val maxFlightAltitude: Int) : ReturnToHomeAltitudeState()
    }

}