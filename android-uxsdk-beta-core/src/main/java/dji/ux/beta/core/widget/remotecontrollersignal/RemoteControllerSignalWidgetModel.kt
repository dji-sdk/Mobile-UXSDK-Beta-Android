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

package dji.ux.beta.core.widget.remotecontrollersignal

import dji.keysdk.AirLinkKey
import dji.keysdk.AirLinkKey.UPLINK_SIGNAL_QUALITY
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [RemoteControllerSignalWidget] used to define
 * the underlying logic and communication
 */
class RemoteControllerSignalWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val ocuSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val wifiSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val lightbridgeSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val rcSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private var signalValue: Int = 0
    //endregion

    //region Data
    /**
     * Get the value of the strength of the signal between the RC and the aircraft.
     */
    val rcSignalQuality: Flowable<Int>
        get() = rcSignalQualityProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {

        val ocuSignalQualityKey = AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY)
        val wifiSignalQualityKey = AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY)
        val lightbridgeSignalQualityKey = AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY)

        //Use the latest value that comes through from any of these keys
        val setCurrentRCSignal: (Any) -> Unit = { signalValue = it as Int }
        bindDataProcessor(ocuSignalQualityKey, ocuSignalQualityProcessor, setCurrentRCSignal)
        bindDataProcessor(wifiSignalQualityKey, wifiSignalQualityProcessor, setCurrentRCSignal)
        bindDataProcessor(lightbridgeSignalQualityKey, lightbridgeSignalQualityProcessor, setCurrentRCSignal)
    }

    override fun inCleanup() {
        // Nothing to clean
    }

    override fun updateStates() {
        rcSignalQualityProcessor.onNext(signalValue)
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            signalValue = 0
        }
    }
    //endregion
}
