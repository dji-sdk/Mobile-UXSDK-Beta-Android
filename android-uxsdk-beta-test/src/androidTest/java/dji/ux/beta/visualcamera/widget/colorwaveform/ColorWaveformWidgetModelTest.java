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

package dji.ux.beta.visualcamera.widget.colorwaveform;

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

import dji.common.camera.ColorWaveformSettings;
import dji.common.error.DJIError;
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
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKeys;

/**
 * Class will test public methods of {@link ColorWaveformWidgetModel}
 * <p>
 * 1. {@link ColorWaveformWidgetModelTest#colorWaveformWidgetModel_colorWaveformEnabled_isUpdated()}
 * Test the color waveform enabled flowable is initialized with a false value as expected and is
 * updated with the given test value as expected.
 * 2. {@link ColorWaveformWidgetModelTest#colorWaveformWidgetModel_displayState_isUpdated()}
 * Test the color waveform display state flowable is initialized with an unknown value as expected
 * and is updated with the given test value as expected.
 * 3. {@link ColorWaveformWidgetModelTest#colorWaveformWidgetModel_setDisplayState_success()}
 * Test the set display state method successfully updates the value of the ux key as expected.
 * 4. {@link ColorWaveformWidgetModelTest#colorWaveformWidgetModel_setEnabled_success()}
 * Test the set color waveform enabled method successfully updates the value of the ux key as
 * expected.
 * 5. {@link ColorWaveformWidgetModelTest#colorWaveformWidgetModel_setDisplayState_throwsError()}
 * Test the set display state method throws an error as expected.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ColorWaveformWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private GlobalPreferencesInterface preferencesManager;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private ColorWaveformWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        Mockito.when(preferencesManager.isColorWaveformEnabled()).thenReturn(false);
        Mockito.when(preferencesManager.getColorWaveformDisplayState())
                .thenReturn(ColorWaveformSettings.ColorWaveformDisplayState.UNKNOWN);
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new ColorWaveformWidgetModel(djiSdkModel, keyedStore, preferencesManager);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void colorWaveformWidgetModel_colorWaveformEnabled_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED),
                true,
                10,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE));
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the color waveform enabled flowable from the model
        TestSubscriber<Boolean> testSubscriber = widgetModel.getColorWaveformEnabled().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void colorWaveformWidgetModel_displayState_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE),
                ColorWaveformSettings.ColorWaveformDisplayState.COLOR,
                10,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED));
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the display state flowable from the model
        TestSubscriber<ColorWaveformSettings.ColorWaveformDisplayState> testSubscriber =
                widgetModel.getColorWaveformDisplayState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(ColorWaveformSettings.ColorWaveformDisplayState.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(ColorWaveformSettings.ColorWaveformDisplayState.UNKNOWN,
                ColorWaveformSettings.ColorWaveformDisplayState.COLOR);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void colorWaveformWidgetModel_setDisplayState_success() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE));
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED));
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE),
                ColorWaveformSettings.ColorWaveformDisplayState.EXPOSURE, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the set color waveform display state method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setColorWaveformDisplayState(ColorWaveformSettings.ColorWaveformDisplayState.EXPOSURE).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void colorWaveformWidgetModel_setEnabled_success() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE));
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED));
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED),
                true, null);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the set color waveform enabled method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setColorWaveformEnabled(true).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void colorWaveformWidgetModel_setDisplayState_throwsError() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE));
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_ENABLED));
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.COLOR_WAVEFORM_DISPLAY_STATE),
                ColorWaveformSettings.ColorWaveformDisplayState.EXPOSURE, uxsdkError);

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test observer that observes the result of the set color waveform display state method
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setColorWaveformDisplayState(ColorWaveformSettings.ColorWaveformDisplayState.EXPOSURE).test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
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
