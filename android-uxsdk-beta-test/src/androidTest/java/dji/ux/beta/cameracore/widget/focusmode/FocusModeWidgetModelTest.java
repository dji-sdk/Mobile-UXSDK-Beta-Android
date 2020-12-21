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

package dji.ux.beta.cameracore.widget.focusmode;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions.FocusMode;
import dji.common.error.DJIError;
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
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.GlobalPreferencesInterface;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKeys;
import dji.ux.beta.core.util.SettingDefinitions;

/**
 * Test:
 * This class tests the public methods in {@link FocusModeWidgetModel}
 * 1. {@link FocusModeWidgetModelTest#focusModeWidgetModel_getFocusMode_updateOrder()}
 * Test the update order of focus mode
 * 2. {@link FocusModeWidgetModelTest#focusModeWidgetModel_isAFCEnabled_enabled()}
 * Test the if Auto focus Continuous mode is enabled
 * 3. {@link FocusModeWidgetModelTest#focusModeWidgetModel_isAFCEnabled_disabled()}}
 * Test the if Auto focus Continuous mode is disabled when AFC setting is disabled
 * 4. {@link FocusModeWidgetModelTest#focusModeWidgetModel_toggleFocusMode_success()}
 * Test if the focus mode is changed successfully
 * 5. {@link FocusModeWidgetModelTest#focusModeWidgetModel_toggleFocusMode_error()}}
 * Test if changing the focus mode fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FocusModeWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private FocusModeWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    @Mock
    private GlobalPreferencesInterface preferencesManager;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        Mockito.when(preferencesManager.getAFCEnabled()).thenReturn(true);
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new FocusModeWidgetModel(djiSdkModel, keyedStore, preferencesManager);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void focusModeWidgetModel_getFocusMode_updateOrder() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                true,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE),
                FocusMode.MANUAL, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<FocusMode> testSubscriber =
                widgetModel.getFocusMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(FocusMode.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(FocusMode.UNKNOWN, FocusMode.MANUAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void focusModeWidgetModel_isAFCEnabled_enabled() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                true,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isAFCEnabled().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false, false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void focusModeWidgetModel_isAFCEnabled_disabled() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                false,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isAFCEnabled().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false, false, false);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void focusModeWidgetModel_toggleFocusMode_success() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                false,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.FOCUS_MODE),
                FocusMode.MANUAL, null);
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS, null);

        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleFocusMode().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void focusModeWidgetModel_toggleFocusMode_error() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED),
                false,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.MANUAL_FOCUS,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_AFC_SUPPORTED),
                true, 20, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.FOCUS_MODE),
                FocusMode.MANUAL, uxsdkError);

        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleFocusMode().test();
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
