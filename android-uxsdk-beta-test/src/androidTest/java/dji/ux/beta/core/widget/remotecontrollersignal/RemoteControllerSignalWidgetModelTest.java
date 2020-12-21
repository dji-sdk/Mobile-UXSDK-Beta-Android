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

package dji.ux.beta.core.widget.remotecontrollersignal;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

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

import static dji.keysdk.AirLinkKey.UPLINK_SIGNAL_QUALITY;

/**
 * Test:
 * This class tests the public methods in the {@link RemoteControllerSignalWidgetModel}
 * <p>
 * 1.
 * {@link RemoteControllerSignalWidgetModelTest#remoteControllerSignalWidgetModel_rcSignalQuality_onlyOcuSyncLinkIsUpdated()}
 * Test the initial value emitted by the RC signal quality flowable is as expected and it is updated
 * with the test value from only the OcuSync link as expected.
 * <p>
 * 2.
 * {@link RemoteControllerSignalWidgetModelTest#remoteControllerSignalWidgetModel_rcSignalQuality_onlyWifiLinkIsUpdated()}
 * Test the initial value emitted by the RC signal quality flowable is as expected and it is updated
 * with the test value from only the WiFi link as expected.
 * <p>
 * 3.
 * {@link RemoteControllerSignalWidgetModelTest#remoteControllerSignalWidgetModel_rcSignalQuality_onlyLightbridgeLinkIsUpdated()}
 * Test the initial value emitted by the RC signal quality flowable is as expected and it is updated
 * with the test value from only the Lightbridge link as expected.
 * <p>
 * 4.
 * {@link RemoteControllerSignalWidgetModelTest#remoteControllerSignalWidgetModel_rcSignalQuality_consolidatedUpdateInOrder()}
 * Test the initial value emitted by the RC signal quality flowable is as expected and it is updated by all three links
 * as expected with the latest value being the one sent the latest by any of the links.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteControllerSignalWidgetModelTest {
    private static final int RC_SIGNAL_QUALITY_TEST_VALUE = 77;
    private CompositeDisposable compositeDisposable;

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private RemoteControllerSignalWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new RemoteControllerSignalWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void remoteControllerSignalWidgetModel_rcSignalQuality_onlyOcuSyncLinkIsUpdated() {
        // Use util method to set emitted value after given delay for the OcuSync Link key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY),
                RC_SIGNAL_QUALITY_TEST_VALUE,
                20,
                TimeUnit.SECONDS);

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY));

        // Setup the widget model after emitted and empty values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the RC signal quality flowable from the model
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getRcSignalQuality().test();

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, RC_SIGNAL_QUALITY_TEST_VALUE);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerSignalWidgetModel_rcSignalQuality_onlyWifiLinkIsUpdated() {
        // Use util method to set emitted value after given delay for the WiFi Link key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY),
                RC_SIGNAL_QUALITY_TEST_VALUE,
                20,
                TimeUnit.SECONDS);

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY));

        // Setup the widget model after emitted and empty values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the RC signal quality flowable from the model
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getRcSignalQuality().test();

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, RC_SIGNAL_QUALITY_TEST_VALUE);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerSignalWidgetModel_rcSignalQuality_onlyLightbridgeLinkIsUpdated() {
        // Use util method to set emitted value after given delay for the Lightbridge Link key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY),
                RC_SIGNAL_QUALITY_TEST_VALUE,
                20,
                TimeUnit.SECONDS);

        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY));

        // Setup the widget model after emitted and empty values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the RC signal quality flowable from the model
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getRcSignalQuality().test();

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, RC_SIGNAL_QUALITY_TEST_VALUE);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void remoteControllerSignalWidgetModel_rcSignalQuality_consolidatedUpdateInOrder() {
        final int ocuSyncTestValue = RC_SIGNAL_QUALITY_TEST_VALUE + 1;
        final int wifiTestValue = RC_SIGNAL_QUALITY_TEST_VALUE + 2;
        final int lightbridgeTestValue = RC_SIGNAL_QUALITY_TEST_VALUE + 3;

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createOcuSyncLinkKey(UPLINK_SIGNAL_QUALITY),
                ocuSyncTestValue,
                10,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createWiFiLinkKey(UPLINK_SIGNAL_QUALITY),
                wifiTestValue,
                15,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(UPLINK_SIGNAL_QUALITY),
                lightbridgeTestValue,
                20,
                TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the RC signal quality flowable from the model
        TestSubscriber<Integer> testSubscriber =
                widgetModel.getRcSignalQuality().test();

        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, ocuSyncTestValue);

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, ocuSyncTestValue, wifiTestValue);

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, ocuSyncTestValue, wifiTestValue, lightbridgeTestValue);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
