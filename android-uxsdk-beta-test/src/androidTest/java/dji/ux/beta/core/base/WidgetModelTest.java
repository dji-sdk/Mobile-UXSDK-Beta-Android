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

package dji.ux.beta.core.base;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.keysdk.DJIKey;
import dji.keysdk.ProductKey;
import dji.keysdk.RemoteControllerKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.FakeUXKeys;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKey;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.util.DataProcessor;

/**
 * Test:
 * This class tests the public and protected methods in the {@link WidgetModel}
 * 1.
 * {@link WidgetModelTest#widgetModel_productConnection_isUpdated()}
 * Test the initial value emitted by the product connection flowable is false as expected and is
 * updated with the given test value as expected.
 * 2.
 * {@link WidgetModelTest#widgetModel_setUp_throwsErrorSecondTime()}
 * Test that an error is thrown when the setUp method is called after the widget is already set up.
 * 3.
 * {@link WidgetModelTest#widgetModel_bindDataProcessor_throwsErrorWhenDJIKeyBoundBeforeSetup()}
 * Test that an error is thrown when the bindDataProcessor method is called with a DJIKey before
 * setUp.
 * 4.
 * {@link WidgetModelTest#widgetModel_bindDataProcessor_throwsErrorWhenUXKeyBoundBeforeSetup()}
 * Test that an error is thrown when the bindDataProcessor method is called with a UXKey before
 * setUp.
 * 5.
 * {@link WidgetModelTest#widgetModel_bindDataProcessor_bindsDJIKey()}
 * Test that a DJIKey can be bound to a DataProcessor.
 * 6.
 * {@link WidgetModelTest#widgetModel_bindDataProcessor_bindsUXKey()}
 * Test that a UXKey can be bound to a DataProcessor.
 * 7.
 * {@link WidgetModelTest#widgetModel_pendingKeys_areAddedAfterInit()}
 * Test that keys bound before the widget is initialized are added after initialization.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private WidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        widgetModel = new MockWidgetModel(djiSdkModel, keyedStore);
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
    }

    @Test
    public void widgetModel_productConnection_isUpdated() {
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false);
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<Boolean> testSubscriber =
                widgetModel.getProductConnection().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test (expected = IllegalStateException.class)
    public void widgetModel_setUp_throwsErrorSecondTime() {
        widgetModel.setup();
        widgetModel.setup();
    }

    @Test (expected = IllegalStateException.class)
    public void widgetModel_bindDataProcessor_throwsErrorWhenDJIKeyBoundBeforeSetup() {
        DJIKey displayNameKey = RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME);
        DataProcessor<String> displayNameProcessor = DataProcessor.create("0");
        widgetModel.bindDataProcessor(displayNameKey, displayNameProcessor);
    }

    @Test (expected = IllegalStateException.class)
    public void widgetModel_bindDataProcessor_throwsErrorWhenUXKeyBoundBeforeSetup() {
        UXKey testKey = UXKeys.create(FakeUXKeys.TEST_KEY_1);
        DataProcessor<Integer> testProcessor = DataProcessor.create(0);
        widgetModel.bindDataProcessor(testKey, testProcessor);
    }

    @Test
    public void widgetModel_bindDataProcessor_bindsDJIKey() {
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

        DJIKey displayNameKey = RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME);
        DataProcessor<String> displayNameProcessor = DataProcessor.create("0");
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                displayNameKey,
                "1",
                10,
                TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();
        // Bind displayNameKey
        widgetModel.bindDataProcessor(displayNameKey, displayNameProcessor);

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<String> testSubscriber = displayNameProcessor.toFlowable().test();

        // Default value
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        // Emitted value is added
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues("0", "1");
    }

    @Test
    public void widgetModel_bindDataProcessor_bindsUXKey() {
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

        UXKey testKey = UXKeys.create(FakeUXKeys.TEST_KEY_1);
        DataProcessor<Integer> testProcessor = DataProcessor.create(0);
        WidgetTestUtil.setEmittedValue(keyedStore,
                testKey,
                1,
                10,
                TimeUnit.SECONDS);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();
        // Bind testKey
        widgetModel.bindDataProcessor(testKey, testProcessor);

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<Integer> testSubscriber = testProcessor.toFlowable().test();

        // Default value
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        // Emitted value is added
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 1);
    }

    @Test
    public void widgetModel_pendingKeys_areAddedAfterInit() {
        Mockito.when(djiSdkModel.isAvailable()).thenReturn(false);
        // Use util method to set emitted value after given delay for given key
        DJIKey displayNameKey = RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME);
        DataProcessor<String> displayNameProcessor = DataProcessor.create("0");
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                displayNameKey,
                "1",
                5,
                TimeUnit.SECONDS); // 5 seconds after WidgetTestUtil.initialize is called
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION));

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();
        // Add displayNameKey to pendingkey queue
        widgetModel.bindDataProcessor(displayNameKey, displayNameProcessor);

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        TestSubscriber<String> testSubscriber = displayNameProcessor.toFlowable().test();

        // Default value
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        // First emitted value is still pending since WidgetModel hasn't been initialized
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues("0");

        // Initialize djiSdkModel to register pending keys
        WidgetTestUtil.initialize(djiSdkModel);

        // Emitted value is added
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues("0", "1");

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private class MockWidgetModel extends WidgetModel {

        private MockWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
            super(djiSdkModel, uxKeyManager);
        }

        @Override
        protected void inSetup() {

        }

        @Override
        protected void inCleanup() {

        }

        @Override
        protected void updateStates() {

        }
    }

}
