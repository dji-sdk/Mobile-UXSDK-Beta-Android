/*
 * Copyright (c) 2018-2021 DJI
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

package dji.ux.beta.flight.widget.apas

import dji.common.flightcontroller.FlightMode
import dji.common.flightcontroller.RemoteControllerFlightMode
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

private const val TAG = "APASWidgetModel"

/**
 * Advanced Pilot Assistance System (APAS) Widget Model
 *
 * Widget Model for the [APASWidget] used to define the
 * underlying logic and communication
 *
 */
class APASWidgetModel(djiSdkModel: DJISDKModel,
                      uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager) {

    //region Fields
    private val apasEnabledDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val apasActiveDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val apasTempErrorDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isOnLimitAreaBoundariesDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val flightModeDataProcessor: DataProcessor<FlightMode> = DataProcessor.create(FlightMode.UNKNOWN)
    private val multiModeOpenDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rcCurrentModeDataProcessor: DataProcessor<RemoteControllerFlightMode> = DataProcessor.create(RemoteControllerFlightMode.UNKNOWN)
    private val apasStateProcessor: DataProcessor<APASState> = DataProcessor.create(APASState.ProductDisconnected)
    private lateinit var apasEnabledKey: DJIKey

    /**
     * APAS State
     */
    val apasState: Flowable<APASState>
        get() = apasStateProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {
        apasEnabledKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED)
        bindDataProcessor(apasEnabledKey, apasEnabledDataProcessor)
        val apasActiveKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_APAS_FUNCTIONING)
        bindDataProcessor(apasActiveKey, apasActiveDataProcessor)
        val apasTempErrorKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DOES_APAS_HAVE_TEMP_ERROR)
        bindDataProcessor(apasTempErrorKey, apasTempErrorDataProcessor)
        val isOnLimitAreaBoundariesKey: DJIKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IN_ON_LIMITE_AREA_BOUNDARIES)
        bindDataProcessor(isOnLimitAreaBoundariesKey, isOnLimitAreaBoundariesDataProcessor)
        val flightModeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE)
        bindDataProcessor(flightModeKey, flightModeDataProcessor)
        val multiModeOpenKey: DJIKey = FlightControllerKey.create(FlightControllerKey.MULTI_MODE_OPEN)
        bindDataProcessor(multiModeOpenKey, multiModeOpenDataProcessor)
        val rcCurrentModeKey: DJIKey = FlightControllerKey.create(FlightControllerKey.CURRENT_MODE)
        bindDataProcessor(rcCurrentModeKey, rcCurrentModeDataProcessor)
    }

    override fun inCleanup() {
        // Nothing to clean up
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (isAPASSupported()) {
                if (apasEnabledDataProcessor.value) {
                    if (apasActiveDataProcessor.value) {
                        apasStateProcessor.onNext(APASState.Active)
                    } else {
                        if (isOnLimitAreaBoundariesDataProcessor.value) {
                            apasStateProcessor.onNext(APASState.EnabledButFlightDistanceLimitReached)
                        } else {
                            apasStateProcessor.onNext(APASState.EnabledWithTemporaryError)
                        }
                    }
                } else {
                    apasStateProcessor.onNext(APASState.Disabled)
                }
            } else {
                apasStateProcessor.onNext(APASState.NotSupported)
            }
        } else {
            apasStateProcessor.onNext(APASState.ProductDisconnected)
        }
    }
    //endregion

    //region Actions
    /**
     * Toggle the APAS state between enabled and disabled
     *
     * @return Completable representing success/failure of the action
     */
    fun toggleAPAS(): Completable {
        return djiSdkModel.setValue(apasEnabledKey, !apasEnabledDataProcessor.value)
    }
    //endregion

    //region helpers
    private fun isAPASSupported(): Boolean {
        return (djiSdkModel.isKeySupported(apasEnabledKey)
                && flightModeDataProcessor.value != FlightMode.GPS_SPORT
                && flightModeDataProcessor.value != FlightMode.TRIPOD
                && !(multiModeOpenDataProcessor.value && rcCurrentModeDataProcessor.value != RemoteControllerFlightMode.P))
    }
    //endregion

    /**
     * Class defines the states of the Advanced Pilot Assistance System (APAS)
     */
    sealed class APASState {
        /**
         * Product is disconnected
         */
        object ProductDisconnected : APASState()

        /**
         * Product does not support APAS
         */
        object NotSupported : APASState()

        /**
         * Product supporting APAS is connected but APAS is disabled
         */
        object Disabled : APASState()

        /**
         * Product supporting APAS is connected and APAS is enabled but inactive
         * due to a temporary error caused by required conditions not being met
         */
        object EnabledWithTemporaryError : APASState()

        /**
         * Product supporting APAS is connected and APAS is enabled but inactive
         * due to reaching the flight distance limit
         */
        object EnabledButFlightDistanceLimitReached : APASState()

        /**
         * Product supporting APAS is connected and APAS is enabled and active
         */
        object Active : APASState()
    }

}
