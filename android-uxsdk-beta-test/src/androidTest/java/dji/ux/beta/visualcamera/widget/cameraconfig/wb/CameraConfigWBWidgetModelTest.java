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

package dji.ux.beta.visualcamera.widget.cameraconfig.wb;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.WhiteBalance;
import dji.keysdk.CameraKey;
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
 * Test:
 * This class tests the public methods in the {@link CameraConfigWBWidgetModel}
 * 1.
 * {@link CameraConfigWBWidgetModelTest#cameraConfigWBWidgetModel_whiteBalance_isUpdated()}
 * Test that the white balance is updated.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraConfigWBWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private CameraConfigWBWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        widgetModel = new CameraConfigWBWidgetModel(djiSdkModel, keyedStore);
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void cameraConfigWBWidgetModel_whiteBalance_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WhiteBalance testWhiteBalance = new WhiteBalance(SettingsDefinitions.WhiteBalancePreset.AUTO);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.WHITE_BALANCE, 0),
                testWhiteBalance,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the white balance flowable from the model
        TestSubscriber<WhiteBalance> testSubscriber = widgetModel.getWhiteBalance().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, whiteBalance -> whiteBalance.getWhiteBalancePreset() == SettingsDefinitions.WhiteBalancePreset.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, whiteBalance -> whiteBalance.getWhiteBalancePreset() == SettingsDefinitions.WhiteBalancePreset.AUTO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
