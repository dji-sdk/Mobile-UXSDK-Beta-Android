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

package dji.ux.beta.core.widget.location

import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.LocationUtil
import dji.ux.beta.core.widget.location.LocationWidgetModel.LocationState.ProductDisconnected

/**
 * Widget Model for the [LocationWidget] used to define
 * the underlying logic and communication
 */
class LocationWidgetModel(djiSdkModel: DJISDKModel,
                          keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val aircraftLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val aircraftLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val locationStateProcessor: DataProcessor<LocationState> = DataProcessor.create(ProductDisconnected)

    /**
     * Value of the location state of aircraft
     */
    val locationState: Flowable<LocationState>
        get() = locationStateProcessor.toFlowable()

    override fun inSetup() {
        val aircraftLatitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE)
        val aircraftLongitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE)
        bindDataProcessor(aircraftLatitudeKey, aircraftLatitudeProcessor)
        bindDataProcessor(aircraftLongitudeKey, aircraftLongitudeProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (LocationUtil.checkLatitude(aircraftLatitudeProcessor.value)
                    && LocationUtil.checkLongitude(aircraftLongitudeProcessor.value)) {
                locationStateProcessor.onNext(LocationState.CurrentLocation(
                        aircraftLatitudeProcessor.value,
                        aircraftLongitudeProcessor.value))
            } else {
                locationStateProcessor.onNext(LocationState.LocationUnavailable)
            }
        } else {
            locationStateProcessor.onNext(ProductDisconnected)
        }
    }


    override fun inCleanup() {
        // No code required
    }

    /**
     * Class to represent states of location widget
     */
    sealed class LocationState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : LocationState()

        /**
         * Product is connected but GPS location fix is unavailable
         */
        object LocationUnavailable : LocationState()

        /**
         * Reflecting the current location
         */
        data class CurrentLocation(val latitude: Double, val longitude: Double) : LocationState()

    }
}