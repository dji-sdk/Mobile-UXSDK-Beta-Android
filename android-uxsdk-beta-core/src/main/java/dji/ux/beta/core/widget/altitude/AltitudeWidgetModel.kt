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

package dji.ux.beta.core.widget.altitude

import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.toDistance
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil.UnitType

/**
 * Widget Model for the [AMSLAltitudeWidget] and [AGLAltitudeWidget] used to define
 * the underlying logic and communication.
 */
class AltitudeWidgetModel @JvmOverloads constructor(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val altitudeProcessor: DataProcessor<Float> = DataProcessor.create(0.0f)
    private val takeOffLocationAltitudeProcessor: DataProcessor<Float> = DataProcessor.create(0.0f)
    private val unitTypeDataProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val altitudeStateProcessor: DataProcessor<AltitudeState> = DataProcessor.create(AltitudeState.ProductDisconnected)

    /**
     * Value of the altitude state of the aircraft
     */
    val altitudeState: Flowable<AltitudeState>
        get() = altitudeStateProcessor.toFlowable()

    override fun inSetup() {
        val altitudeKey = FlightControllerKey.create(FlightControllerKey.ALTITUDE)
        bindDataProcessor(altitudeKey, altitudeProcessor)

        val takeoffLocationAltitudeKey = FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE)
        bindDataProcessor(takeoffLocationAltitudeKey, takeOffLocationAltitudeProcessor)

        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            altitudeStateProcessor.onNext(
                    AltitudeState.CurrentAltitude(
                            altitudeAGL = altitudeProcessor.value.toDistance(unitTypeDataProcessor.value),
                            altitudeAMSL = (altitudeProcessor.value + takeOffLocationAltitudeProcessor.value)
                                    .toDistance(unitTypeDataProcessor.value),
                            unitType = unitTypeDataProcessor.value))
        } else {
            altitudeStateProcessor.onNext(AltitudeState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states of Altitude
     */
    sealed class AltitudeState {

        /**
         *  When product is disconnected
         */
        object ProductDisconnected : AltitudeState()

        /**
         *  When product is connected and altitude level is available
         *
         *  @property altitudeAGL - Above Ground Level Altitude
         *  @property altitudeAMSL - Above Mean Sea Level Altitude
         *  @property unitType - Unit of altitude
         */
        data class CurrentAltitude(val altitudeAGL: Float,
                                   val altitudeAMSL: Float,
                                   val unitType: UnitType) : AltitudeState()
    }

}