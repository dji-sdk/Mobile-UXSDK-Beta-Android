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

package dji.ux.beta.core.panel.listitem.aircraftbatterytemperature

import dji.keysdk.BatteryKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidgetModel.AircraftBatteryTemperatureItemState.AircraftBatteryState
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidgetModel.AircraftBatteryTemperatureItemState.ProductDisconnected
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.util.UnitConversionUtil.TemperatureUnitType
import dji.ux.beta.core.util.UnitConversionUtil.TemperatureUnitType.*


/**
 * Widget Model for the [AircraftBatteryTemperatureListItemWidget] used to define
 * the underlying logic and communication
 */
class AircraftBatteryTemperatureListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields

    private val batteryTemperatureStateProcessor: DataProcessor<AircraftBatteryTemperatureItemState> = DataProcessor.create(ProductDisconnected)
    private val batteryTemperatureProcessor: DataProcessor<Float> = DataProcessor.create(0.0f)
    private val temperatureUnitTypeProcessor: DataProcessor<TemperatureUnitType> = DataProcessor.create(CELSIUS)

    //endregion

    //region Data
    /**
     * Get the aircraft battery temperature state
     */
    val aircraftBatteryTemperatureState: Flowable<AircraftBatteryTemperatureItemState>
        get() = batteryTemperatureStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val batteryTemperatureKey = BatteryKey.create(BatteryKey.TEMPERATURE)
        bindDataProcessor(batteryTemperatureKey, batteryTemperatureProcessor)
        val temperatureUnitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.TEMPERATURE_UNIT_TYPE)
        bindDataProcessor(temperatureUnitTypeKey, temperatureUnitTypeProcessor)
        preferencesManager?.setUpListener()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            val temperatureValue = when (temperatureUnitTypeProcessor.value) {
                CELSIUS -> batteryTemperatureProcessor.value
                FAHRENHEIT -> UnitConversionUtil.celsiusToFahrenheit(batteryTemperatureProcessor.value)
                KELVIN -> UnitConversionUtil.celsiusToKelvin(batteryTemperatureProcessor.value)
            }
            batteryTemperatureStateProcessor.onNext(AircraftBatteryState(temperatureValue, temperatureUnitTypeProcessor.value))
        } else {
            batteryTemperatureStateProcessor.onNext(ProductDisconnected)
        }

    }

    /**
     * Class to represent states of AircraftBatteryItem
     */
    sealed class AircraftBatteryTemperatureItemState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : AircraftBatteryTemperatureItemState()

        /**
         * When product is connected and battery temperature is received.
         */
        data class AircraftBatteryState(val temperature: Float, val unitType: TemperatureUnitType) : AircraftBatteryTemperatureItemState()
    }
}
//endregion