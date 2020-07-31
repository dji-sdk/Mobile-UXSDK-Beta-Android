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

package dji.ux.beta.core.listitemwidget.travelmode

import dji.common.flightcontroller.LandingGearMode
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProviderInterface
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [TravelModeListItemWidget] used to define
 * the underlying logic and communication
 */
class TravelModeListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val schedulerProvider: SchedulerProviderInterface
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields

    private val travelModeStateProcessor: DataProcessor<TravelModeState> = DataProcessor.create(TravelModeState.ProductDisconnected)

    private val travelModeMovableProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    private val landingGearModeProcessor: DataProcessor<LandingGearMode> = DataProcessor.create(LandingGearMode.UNKNOWN)


    //endregion

    //region Data
    /**
     * Get the travel mode state processor
     */
    val travelModeState: Flowable<TravelModeState>
        get() = travelModeStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        val landingGearMovableKey = FlightControllerKey.create(FlightControllerKey.IS_LANDING_GEAR_MOVABLE)
        bindDataProcessor(landingGearMovableKey, travelModeMovableProcessor)
        val landingGearModeKey = FlightControllerKey.create(FlightControllerKey.LANDING_GEAR_MODE)
        bindDataProcessor(landingGearModeKey, landingGearModeProcessor)
    }

    override fun inCleanup() {
        // No Clean up
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (travelModeMovableProcessor.value) {
                if (landingGearModeProcessor.value == LandingGearMode.TRANSPORT) {
                    travelModeStateProcessor.onNext(TravelModeState.Active)
                } else {
                    travelModeStateProcessor.onNext(TravelModeState.Inactive)
                }
            } else {
                travelModeStateProcessor.onNext(TravelModeState.NotSupported)
            }

        } else {
            travelModeStateProcessor.onNext(TravelModeState.ProductDisconnected)
        }
    }


    /**
     * Enter travel mode
     */
    fun enterTravelMode(): Completable {
        val enterTransportModeKey = FlightControllerKey.create(FlightControllerKey.ENTER_TRANSPORT_MODE)
        return djiSdkModel.performAction(enterTransportModeKey).subscribeOn(schedulerProvider.io())
    }

    /**
     * Exit travel mode
     */
    fun exitTravelMode(): Completable {
        val enterTransportModeKey = FlightControllerKey.create(FlightControllerKey.EXIT_TRANSPORT_MODE)
        return djiSdkModel.performAction(enterTransportModeKey).subscribeOn(schedulerProvider.io())
    }


    /**
     * Class to represent states of travel mode state
     */
    sealed class TravelModeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : TravelModeState()

        /**
         * When product is connected and travel mode is not supported
         */
        object NotSupported : TravelModeState()

        /**
         * When product is connected and landing gear is in transport mode
         */
        object Active : TravelModeState()

        /**
         * When product is connected and landing gear is not in transport mode
         */
        object Inactive : TravelModeState()
    }
}
//endregion