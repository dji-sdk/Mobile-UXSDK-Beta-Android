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

package dji.ux.beta.accessory.widget.spotlight;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.accessory.widget.spotlight.SpotlightIndicatorWidgetModel.SpotlightIndicatorState;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;


/**
 * This class tests the public methods in {@link SpotlightIndicatorWidgetModel}
 * <p>
 * 1. {@link SpotlightIndicatorWidgetModelTest#spotlightIndicatorWidgetModel_getSpotlightState_hidden()}
 * Test spotlight indicator widget state when spotlight not connected
 * <p>
 * 2. {@link SpotlightIndicatorWidgetModelTest#spotlightIndicatorWidgetModel_getSpotlightState_inactive()}
 * Test spotlight indicator widget state when spotlight connected but not active
 * <p>
 * 3. {@link SpotlightIndicatorWidgetModelTest#spotlightIndicatorWidgetModel_getSpotlightState_active()}
 * Test spotlight indicator widget state when spotlight connected and active
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SpotlightIndicatorWidgetModelTest {

    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    private SpotlightIndicatorWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new SpotlightIndicatorWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel);

    }

    @Test
    public void spotlightIndicatorWidgetModel_getSpotlightState_hidden() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);

        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                false, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<SpotlightIndicatorState> testSubscriber =
                widgetModel.getSpotlightState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(14, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN,
                SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void spotlightIndicatorWidgetModel_getSpotlightState_inactive() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                true, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED),
                false, 30, TimeUnit.SECONDS);


        widgetModel.setup();

        TestSubscriber<SpotlightIndicatorState> testSubscriber =
                widgetModel.getSpotlightState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(14, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN,
                SpotlightIndicatorState.INACTIVE, SpotlightIndicatorState.INACTIVE);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void spotlightIndicatorWidgetModel_getSpotlightState_active() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                true, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED),
                true, 30, TimeUnit.SECONDS);


        widgetModel.setup();

        TestSubscriber<SpotlightIndicatorState> testSubscriber =
                widgetModel.getSpotlightState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(14, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN);
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        testSubscriber.assertValues(SpotlightIndicatorState.HIDDEN, SpotlightIndicatorState.HIDDEN,
                SpotlightIndicatorState.INACTIVE, SpotlightIndicatorState.ACTIVE);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

}
