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

import dji.common.error.DJIError;
import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.observers.TestObserver;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;


/**
 * Class will test public methods {@link SpotlightControlWidgetModel}
 * <p>
 * 1.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_getSpotlightState_updateOrder()}
 * Check the if the spotlight state is updated
 * 2.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_isSpotlightConnected_connected()}
 * Check if the spotlight is connected
 * 3.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_isSpotlightConnected_notConnected()}
 * Check if the spotlight is not connected
 * 4.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_setSpotlightBrightnessPercentage_success()}
 * Check if the spotlight brightness percentage is set successfully
 * 5.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_setSpotlightBrightnessPercentage_error()}
 * Check if setting the spotlight brightness percentage fails
 * 6.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_toggleSpotlight_success()}
 * Test if toggling the spotlight state is successful
 * 7.{@link SpotlightControlWidgetModelTest#spotlightControlWidgetModel_toggleSpotlight_error()}
 * Test if toggling the spotlight state fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SpotlightControlWidgetModelTest {

    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    private SpotlightControlWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new SpotlightControlWidgetModel(djiSdkModel,
                keyedStore);

        WidgetTestUtil.initialize(djiSdkModel);
    }

    @Test
    public void spotlightControlWidgetModel_isSpotlightConnected_notConnected() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                false, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isSpotlightConnected().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void spotlightControlWidgetModel_isSpotlightConnected_connected() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isSpotlightConnected().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void spotlightControlWidgetModel_getSpotlightState_updateOrder() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED),
                true, 15, TimeUnit.SECONDS);
        SpotlightState defaultSpotlightState = new SpotlightState(false, 0, 0.0f);
        SpotlightState spotlightState = new SpotlightState(true, 0, 0.0f);
        widgetModel.setup();

        TestSubscriber<SpotlightState> testSubscriber =
                widgetModel.getSpotlightState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultSpotlightState);
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultSpotlightState, defaultSpotlightState, defaultSpotlightState, spotlightState);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void spotlightControlWidgetModel_setSpotlightBrightnessPercentage_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS),
                50, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setSpotlightBrightnessPercentage(50).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void spotlightControlWidgetModel_setSpotlightBrightnessPercentage_error() {
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS),
                50, uxsdkError);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setSpotlightBrightnessPercentage(50).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }


    @Test
    public void spotlightControlWidgetModel_toggleSpotlight_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED),
                true, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleSpotlight().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void spotlightControlWidgetModel_toggleSpotlight_error() {
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_TEMPERATURE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.CONNECTION));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED));
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED),
                true, uxsdkError);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleSpotlight().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
