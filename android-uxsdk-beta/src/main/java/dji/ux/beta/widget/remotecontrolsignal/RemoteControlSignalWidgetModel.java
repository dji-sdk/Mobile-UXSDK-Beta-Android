/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.widget.remotecontrolsignal;

import android.support.annotation.NonNull;
import dji.keysdk.AirLinkKey;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.functions.Consumer;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DataProcessor;

import static dji.keysdk.AirLinkKey.UPLINK_SIGNAL_QUALITY;

/**
 * Widget Model for the {@link RemoteControlSignalWidget} used to define
 * the underlying logic and communication
 */
public class RemoteControlSignalWidgetModel extends WidgetModel {

    //region Fields
    private final DataProcessor<Integer> ocuSignalQualityProcessor;
    private final DataProcessor<Integer> wifiSignalQualityProcessor;
    private final DataProcessor<Integer> lightbridgeSignalQualityProcessor;
    private final DataProcessor<Integer> rcSignalQualityProcessor;
    private int signalValue;
    //endregion

    //region Constructor
    public RemoteControlSignalWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        ocuSignalQualityProcessor = DataProcessor.create(0);
        wifiSignalQualityProcessor = DataProcessor.create(0);
        lightbridgeSignalQualityProcessor = DataProcessor.create(0);
        rcSignalQualityProcessor = DataProcessor.create(0);
    }
    //endregion

    //region Data
    /**
     * Get the value of the strength of the signal between the RC and the aircraft.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Integer> getRCSignalQuality() {
        return rcSignalQualityProcessor.toFlowable();
    }

    //region Lifecycle
    @Override
    protected void inSetup() {

        AirLinkKey ocuSignalQualityKey = AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY);
        AirLinkKey wifiSignalQualityKey = AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY);
        AirLinkKey lightbridgeSignalQualityKey = AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY);

        //Use the latest value that comes through from any of these keys
        Consumer<Object> setCurrentRCSignal = currentSignalValue -> setCurrentRCSignalQuality((int) currentSignalValue);
        bindDataProcessor(ocuSignalQualityKey, ocuSignalQualityProcessor, setCurrentRCSignal);
        bindDataProcessor(wifiSignalQualityKey, wifiSignalQualityProcessor, setCurrentRCSignal);
        bindDataProcessor(lightbridgeSignalQualityKey, lightbridgeSignalQualityProcessor, setCurrentRCSignal);
    }

    @Override
    protected void inCleanup() {
        // Nothing to clean
    }

    @Override
    protected void updateStates() {
        rcSignalQualityProcessor.onNext(signalValue);
    }
    //endregion

    //region Helpers
    private void setCurrentRCSignalQuality(int currentSignalValue) {
        signalValue = currentSignalValue;
    }
    //endregion
}
