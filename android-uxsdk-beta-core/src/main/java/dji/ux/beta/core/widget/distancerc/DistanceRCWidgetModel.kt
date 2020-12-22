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

package dji.ux.beta.core.widget.distancerc

import dji.common.remotecontroller.GPSData
import dji.keysdk.FlightControllerKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.toDistance
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.LocationUtil
import dji.ux.beta.core.util.LocationUtil.distanceBetween
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.widget.distancerc.DistanceRCWidgetModel.DistanceRCState.*

/**
 * Widget Model for the [DistanceRCWidget] used to define
 * the underlying logic and communication
 */
class DistanceRCWidgetModel(djiSdkModel: DJISDKModel,
                            keyedStore: ObservableInMemoryKeyedStore,
                            private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val rcGPSLocationProcessor: DataProcessor<GPSData> = DataProcessor.create(GPSData.Builder().build())
    private val unitTypeDataProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val aircraftLatitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val aircraftLongitudeProcessor: DataProcessor<Double> = DataProcessor.create(0.0)
    private val distanceRCStateProcessor: DataProcessor<DistanceRCState> = DataProcessor.create(ProductDisconnected)

    /**
     * Value of the distance to RC state of the aircraft
     */
    val distanceRCState: Flowable<DistanceRCState>
        get() = distanceRCStateProcessor.toFlowable()

    override fun inSetup() {
        val aircraftLatitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE)
        val aircraftLongitudeKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE)
        bindDataProcessor(aircraftLatitudeKey, aircraftLatitudeProcessor)
        bindDataProcessor(aircraftLongitudeKey, aircraftLongitudeProcessor)

        val rcGPSKey = RemoteControllerKey.create(RemoteControllerKey.GPS_DATA)
        bindDataProcessor(rcGPSKey, rcGPSLocationProcessor)

        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (LocationUtil.checkLatitude(aircraftLatitudeProcessor.value)
                    && LocationUtil.checkLongitude(aircraftLongitudeProcessor.value)
                    && rcGPSLocationProcessor.value.isValid) {
                distanceRCStateProcessor.onNext(CurrentDistanceToRC(
                        distanceBetween(aircraftLatitudeProcessor.value,
                                aircraftLongitudeProcessor.value,
                                rcGPSLocationProcessor.value.location.latitude,
                                rcGPSLocationProcessor.value.location.longitude)
                                .toDistance(unitTypeDataProcessor.value),
                        unitTypeDataProcessor.value))
            } else {
                distanceRCStateProcessor.onNext(LocationUnavailable)
            }
        } else {
            distanceRCStateProcessor.onNext(ProductDisconnected)
        }

    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states distance of aircraft from the remote controller
     */
    sealed class DistanceRCState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : DistanceRCState()

        /**
         * Product is connected but GPS location fix is unavailable
         */
        object LocationUnavailable : DistanceRCState()

        /**
         * Reflecting the distance to the remote controller
         */
        data class CurrentDistanceToRC(val distance: Float, val unitType: UnitConversionUtil.UnitType) : DistanceRCState()

    }

}