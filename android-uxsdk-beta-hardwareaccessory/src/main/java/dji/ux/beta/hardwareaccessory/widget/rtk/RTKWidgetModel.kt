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

package dji.ux.beta.hardwareaccessory.widget.rtk

import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProviderInterface
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

private const val TAG = "RTKWidgetModel"

/**
 * Widget Model for the [RTKWidget] used to define
 * the underlying logic and communication
 */
class RTKWidgetModel(djiSdkModel: DJISDKModel,
                     uxKeyManager: ObservableInMemoryKeyedStore,
                     private val schedulerProvider: SchedulerProviderInterface
) : WidgetModel(djiSdkModel, uxKeyManager) {

    //region Fields
    private val rtkEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rtkSupportedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isRTKConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    //endregion

    //region Data
    /**
     * Get whether the RTK is enabled.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = rtkEnabledProcessor.toFlowable()

    /**
     * Get whether the RTK is supported.
     */
    val rtkSupported: Flowable<Boolean>
        @JvmName("getRTKSupported")
        get() = rtkSupportedProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {
        val rtkSupportedKey = FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED)
        bindDataProcessor(rtkSupportedKey, rtkSupportedProcessor)
        val rtkEnabledKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED)
        bindDataProcessor(rtkEnabledKey, rtkEnabledProcessor)
        val isRTKConnectedKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED)
        bindDataProcessor(isRTKConnectedKey, isRTKConnectedProcessor) {
            addDisposable(djiSdkModel.getValue(rtkEnabledKey)
                    .observeOn(schedulerProvider.io())
                    .subscribe(Consumer { }, logErrorConsumer(TAG, "isRTKEnabled: ")))
        }
    }

    override fun inCleanup() {
        // Nothing to clean up
    }

    override fun updateStates() {
        // Nothing to update
    }
    //endregion
}