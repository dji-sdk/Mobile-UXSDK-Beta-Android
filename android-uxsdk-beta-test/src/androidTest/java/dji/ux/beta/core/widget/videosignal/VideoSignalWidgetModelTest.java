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

package dji.ux.beta.core.widget.videosignal;


import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.airlink.LightbridgeFrequencyBand;
import dji.common.airlink.OcuSyncFrequencyBand;
import dji.common.airlink.WiFiFrequencyBand;
import dji.keysdk.AirLinkKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Class will test public methods in {@link VideoSignalWidgetModel}
 * 1.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getWifiVideoSignalQuality_isUpdated()}
 * Test to check if video signal quality is updated on Wi-Fi
 * <p>
 * 2.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getLightBridgeVideoSignalQuality_isUpdated()}
 * Test to check if video signal quality is updated on LightBridge
 * <p>
 * 3.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getOcuSyncVideoSignalQuality_isUpdated()}
 * Test to check if video signal quality is updated on OcuSync
 * <p>
 * 4.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getWifiFrequencyBand_isUpdated()}
 * Test to check if Wi-Fi frequency band is updated
 * <p>
 * 5.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getLightBridgeFrequencyBand_isUpdated()}
 * Test to check if LightBridge frequency band is updated
 * <p>
 * 6.{@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getOcuSyncFrequencyBand_isUpdated()}
 * Test to check if OcuSync frequency band is updated
 * <p>
 * 7. {@link VideoSignalWidgetModelTest#videoSignalWidgetModel_getOcuSyncFrequencyPointIndex_isUpdated()}
 * Test to check if OcuSync frequency point index is updated
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class VideoSignalWidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private VideoSignalWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new VideoSignalWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void videoSignalWidgetModel_getWifiVideoSignalQuality_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY),
                25,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getVideoSignalQuality().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 25);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void videoSignalWidgetModel_getOcuSyncVideoSignalQuality_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY),
                25,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getVideoSignalQuality().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 25);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void videoSignalWidgetModel_getLightBridgeVideoSignalQuality_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY),
                25,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getVideoSignalQuality().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 25);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void videoSignalWidgetModel_getWifiFrequencyBand_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(AirLinkKey.WIFI_FREQUENCY_BAND),
                WiFiFrequencyBand.FREQUENCY_BAND_2_DOT_4_GHZ,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<WiFiFrequencyBand> testSubscriber =
                widgetModel.getWifiFrequencyBand().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(WiFiFrequencyBand.UNKNOWN);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(WiFiFrequencyBand.UNKNOWN, WiFiFrequencyBand.FREQUENCY_BAND_2_DOT_4_GHZ);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void videoSignalWidgetModel_getLightBridgeFrequencyBand_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.LB_FREQUENCY_BAND),
                LightbridgeFrequencyBand.FREQUENCY_BAND_5_DOT_8_GHZ,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<LightbridgeFrequencyBand> testSubscriber =
                widgetModel.getLightBridgeFrequencyBand().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(LightbridgeFrequencyBand.UNKNOWN);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(LightbridgeFrequencyBand.UNKNOWN,
                LightbridgeFrequencyBand.FREQUENCY_BAND_5_DOT_8_GHZ);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void videoSignalWidgetModel_getOcuSyncFrequencyBand_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.OCUSYNC_FREQUENCY_BAND),
                OcuSyncFrequencyBand.FREQUENCY_BAND_5_DOT_7_GHZ,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<OcuSyncFrequencyBand> testSubscriber =
                widgetModel.getOcuSyncFrequencyBand().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(OcuSyncFrequencyBand.UNKNOWN);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(OcuSyncFrequencyBand.UNKNOWN, OcuSyncFrequencyBand.FREQUENCY_BAND_5_DOT_7_GHZ);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void videoSignalWidgetModel_getOcuSyncFrequencyPointIndex_isUpdated() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.FREQUENCY_POINT_INDEX),
                25,
                10,
                TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getOcuSyncFrequencyPointIndex().test();


        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 25);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }


    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.DOWNLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(AirLinkKey.WIFI_FREQUENCY_BAND));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.OCUSYNC_FREQUENCY_BAND));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.LB_FREQUENCY_BAND));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(AirLinkKey.FREQUENCY_POINT_INDEX));

    }
}
