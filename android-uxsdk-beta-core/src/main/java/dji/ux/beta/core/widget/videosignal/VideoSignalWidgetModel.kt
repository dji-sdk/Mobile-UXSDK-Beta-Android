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

package dji.ux.beta.core.widget.videosignal

import dji.common.airlink.LightbridgeFrequencyBand
import dji.common.airlink.OcuSyncFrequencyBand
import dji.common.airlink.WiFiFrequencyBand
import dji.keysdk.AirLinkKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [VideoSignalWidget] used to define
 * the underlying logic and communication
 */
class VideoSignalWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val videoSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val ocuSyncVideoSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val wiFiVideoSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val lightbridgeVideoSignalQualityProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val ocuSyncFrequencyBandProcessor: DataProcessor<OcuSyncFrequencyBand> = DataProcessor.create(OcuSyncFrequencyBand.UNKNOWN)
    private val wifiFrequencyBandProcessor: DataProcessor<WiFiFrequencyBand> = DataProcessor.create(WiFiFrequencyBand.UNKNOWN)
    private val lightbridgeFrequencyBandProcessor: DataProcessor<LightbridgeFrequencyBand> = DataProcessor.create(LightbridgeFrequencyBand.UNKNOWN)
    private var signalValue: Int = 0
    private val ocuFrequencyPointIndexProcessor: DataProcessor<Int> = DataProcessor.create(0)
    //endregion

    //region Data
    /**
     * Get the value of the strength of the video signal as an integer.
     */
    val videoSignalQuality: Flowable<Int>
        get() = videoSignalQualityProcessor.toFlowable()

    /**
     * Get the frequency band of the video signal as a [LightbridgeFrequencyBand] value.
     */
    val lightBridgeFrequencyBand: Flowable<LightbridgeFrequencyBand>
        get() = lightbridgeFrequencyBandProcessor.toFlowable()

    val wifiFrequencyBand: Flowable<WiFiFrequencyBand>
        get() = wifiFrequencyBandProcessor.toFlowable()

    val ocuSyncFrequencyBand: Flowable<OcuSyncFrequencyBand>
        get() = ocuSyncFrequencyBandProcessor.toFlowable()

    val ocuSyncFrequencyPointIndex: Flowable<Int>
        get() = ocuFrequencyPointIndexProcessor.toFlowable()


    //region Lifecycle
    override fun inSetup() {
        val ocuVideoSignalQualityKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY)
        val wiFiVideoSignalQualityKey = AirLinkKey.createWiFiLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY)
        val lightbridgeVideoSignalQualityKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY)

        val wifiFrequencyBandKey = AirLinkKey.createWiFiLinkKey(AirLinkKey.WIFI_FREQUENCY_BAND)
        val occuFrequencyBandKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.OCUSYNC_FREQUENCY_BAND)
        val lightbridgeFrequencyBandKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.LB_FREQUENCY_BAND)
        val ocuFrequencyPointIndexKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.FREQUENCY_POINT_INDEX)


        bindDataProcessor(ocuFrequencyPointIndexKey, ocuFrequencyPointIndexProcessor)
        //Use the latest value that comes through from any of these keys
        val setCurrentVideoSignal: (Any) -> Unit = { signalValue = it as Int }
        bindDataProcessor(ocuVideoSignalQualityKey,
                ocuSyncVideoSignalQualityProcessor,
                setCurrentVideoSignal)
        bindDataProcessor(wiFiVideoSignalQualityKey,
                wiFiVideoSignalQualityProcessor,
                setCurrentVideoSignal)
        bindDataProcessor(lightbridgeVideoSignalQualityKey,
                lightbridgeVideoSignalQualityProcessor,
                setCurrentVideoSignal)

        val frequencyBand: (Any) -> (Unit) = {
            if (it is OcuSyncFrequencyBand && it != OcuSyncFrequencyBand.UNKNOWN) {
                wifiFrequencyBandProcessor.onNext(WiFiFrequencyBand.UNKNOWN)
                lightbridgeFrequencyBandProcessor.onNext(LightbridgeFrequencyBand.UNKNOWN)
            } else if (it is WiFiFrequencyBand && it != WiFiFrequencyBand.UNKNOWN) {
                ocuSyncFrequencyBandProcessor.onNext(OcuSyncFrequencyBand.UNKNOWN)
                lightbridgeFrequencyBandProcessor.onNext(LightbridgeFrequencyBand.UNKNOWN)
            } else if (it is LightbridgeFrequencyBand && it != LightbridgeFrequencyBand.UNKNOWN) {
                wifiFrequencyBandProcessor.onNext(WiFiFrequencyBand.UNKNOWN)
                ocuSyncFrequencyBandProcessor.onNext(OcuSyncFrequencyBand.UNKNOWN)
            }

        }
        bindDataProcessor(occuFrequencyBandKey, ocuSyncFrequencyBandProcessor, frequencyBand)
        bindDataProcessor(wifiFrequencyBandKey, wifiFrequencyBandProcessor, frequencyBand)
        bindDataProcessor(lightbridgeFrequencyBandKey, lightbridgeFrequencyBandProcessor, frequencyBand)


    }

    override fun inCleanup() {
        // Nothing to clean
    }

    override fun updateStates() {
        videoSignalQualityProcessor.onNext(signalValue)
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            signalValue = 0
        }
    }
}
//endregion
