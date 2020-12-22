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

package dji.ux.beta.core.widget.gpssignal

import dji.common.flightcontroller.GPSSignalLevel
import dji.common.flightcontroller.PositioningSolution
import dji.common.flightcontroller.RTKState
import dji.common.flightcontroller.RedundancySensorUsedState
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [GPSSignalWidget] used to define
 * the underlying logic and communication
 */
class GPSSignalWidgetModel(djiSdkModel: DJISDKModel,
                           keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val gpsSignalQualityProcessor: DataProcessor<GPSSignalLevel> = DataProcessor.create(GPSSignalLevel.NONE)
    private val satelliteCountProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val rtkEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rtkSupportedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rtkStateProcessor: DataProcessor<RTKState> = DataProcessor.create(RTKState.Builder().build())
    private val redundancySensorUsedStateProcessor: DataProcessor<RedundancySensorUsedState> = DataProcessor.create(RedundancySensorUsedState.Builder().build())
    private val satelliteNumberProcessor: DataProcessor<Int> = DataProcessor.create(0)
    //endregion

    //region Data
    /**
     * Get the value of the strength of the GPS signal as a [GPSSignalLevel].
     */
    val gpsSignalQuality: Flowable<GPSSignalLevel>
        @JvmName("getGPSSignalQuality")
        get() = gpsSignalQualityProcessor.toFlowable()

    /**
     * Get the number of satellites as an integer value.
     */
    val satelliteNumber: Flowable<Int>
        get() = satelliteNumberProcessor.toFlowable()

    /**
     * Get if RTK is enabled on supported aircraft as a boolean value.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = rtkEnabledProcessor.toFlowable()

    /**
     * Get whether an external GPS device is in use.
     */
    val isExternalGPSUsed: Flowable<Boolean>
        get() = redundancySensorUsedStateProcessor.toFlowable()
                .concatMap { state: RedundancySensorUsedState -> Flowable.just(state.gpsIndex == 2) }

    /**
     * Get whether RTK is using the most accurate positioning solution.
     */
    val isRTKAccurate: Flowable<Boolean>
        get() = rtkStateProcessor.toFlowable()
                .concatMap { state: RTKState -> Flowable.just(state.positioningSolution == PositioningSolution.FIXED_POINT) }
    //endregion

    //region Lifecycle
    override fun inSetup() {
        val gpsSignalQualityKey = FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL)
        val satelliteCountKey = FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT)
        val rtkEnabledKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED)
        val rtkSupportedKey = FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED)
        val rtkStateKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE)
        val redundancySensorUsedState = FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE)
        bindDataProcessor(gpsSignalQualityKey, gpsSignalQualityProcessor)
        bindDataProcessor(satelliteCountKey, satelliteCountProcessor)
        bindDataProcessor(rtkEnabledKey, rtkEnabledProcessor)
        //Use the supported key to begin getting the RTK Enabled values
        bindDataProcessor(rtkSupportedKey, rtkSupportedProcessor) {
            addDisposable(djiSdkModel.getValue(rtkEnabledKey)
                    .observeOn(SchedulerProvider.io())
                    .subscribe(Consumer { }, logErrorConsumer("GPSSignalWidget", "isRTKSupported: ")))
        }
        bindDataProcessor(rtkStateKey, rtkStateProcessor)
        bindDataProcessor(redundancySensorUsedState, redundancySensorUsedStateProcessor)
    }

    override fun inCleanup() {
        // Nothing to clean
    }

    override fun updateStates() {
        satelliteNumberProcessor.onNext(
                if (rtkEnabledProcessor.value) rtkStateProcessor.value.mobileStation1SatelliteCount
                else satelliteCountProcessor.value)
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            rtkEnabledProcessor.onNext(false)
            gpsSignalQualityProcessor.onNext(GPSSignalLevel.NONE)
        }
    }
    //endregion
}