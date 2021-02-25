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

package dji.ux.beta.visualcamera.widget.histogram;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.observers.TestObserver;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.ArrayUtil;

/**
 * Class will test public methods of {@link HistogramWidgetModel}
 * <p>
 * 1. {@link HistogramWidgetModelTest#histogramWidgetModel_histogramEnabled_isUpdated()}
 * Test the histogram enabled flowable is initialized with a false value as expected and is updated
 * with the given test value as expected.
 * 2. {@link HistogramWidgetModelTest#histogramWidgetModel_lightValues_areUpdated()}
 * Test the histogram light values flowable is initialized with an empty array of the expected size
 * and is updated with a subset of the given test array as expected.
 * 3. {@link HistogramWidgetModelTest#histogramWidgetModel_setHistogramEnabled_isSuccessful()}
 * Test the set histogram enabled method successfully updates the value of the dji key as expected.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class HistogramWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private HistogramWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new HistogramWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void histogramWidgetModel_histogramEnabled_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_ENABLED),
                true,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_LIGHT_VALUES));

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<Boolean> testSubscriber = widgetModel.getHistogramEnabled().test();

        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void histogramWidgetModel_lightValues_areUpdated() {
        Float[] defaultValues = ArrayUtil.toObject(new float[LineChartView.NUM_DATA_POINTS]);
        Short[] lightValuesInput = ArrayUtil.toObject(new short[LineChartView.NUM_DATA_POINTS + 6]);
        lightValuesInput[3] = 1;
        Float[] lightValuesOutput = ArrayUtil.toObject(new float[LineChartView.NUM_DATA_POINTS]);
        lightValuesOutput[0] = 1f;

        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_LIGHT_VALUES),
                lightValuesInput,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_ENABLED));

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<Float[]> testSubscriber = widgetModel.getLightValues().test();

        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
        testSubscriber.assertValue(floats -> Arrays.equals(floats, defaultValues));

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, floats -> Arrays.equals(floats, lightValuesOutput));

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void histogramWidgetModel_setHistogramEnabled_isSuccessful() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_ENABLED),
                true,
                0,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_LIGHT_VALUES));

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.HISTOGRAM_ENABLED),
                false,
                null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setHistogramEnabled(false).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
