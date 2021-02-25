/*
 * Copyright (c) 2018-2021 DJI
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

package dji.ux.beta.cameracore.widget.manualfocus;

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

import static dji.ux.beta.core.util.SettingDefinitions.ControlMode.MANUAL_FOCUS;
import static dji.ux.beta.core.util.SettingDefinitions.ControlMode.SPOT_METER;

/**
 * Class will test public methods of {@link ManualFocusWidgetModel}
 * <p>
 * 1.{@link ManualFocusWidgetModelTest#manualFocusWidgetModel_getFocusRingUpperBoundValue_valueUpdate()}
 * Test the focus ring upper bound value update
 * 2.{@link ManualFocusWidgetModelTest#manualFocusWidgetModel_getFocusRingValue_valueUpdate()}
 * Test the focus ring value update
 * 3.{@link ManualFocusWidgetModelTest#manualFocusWidgetModel_isManualFocusMode_valueUpdate()}}
 * Test if the focus mode is manual focus
 * 4.{@link ManualFocusWidgetModelTest#manualFocusWidgetModel_setFocusRingValue_success()}
 * Test set focus ring value is successful
 * 5.{@link ManualFocusWidgetModelTest#manualFocusWidgetModel_setFocusRingValue_throwsError()}
 * Test set focus ring value fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ManualFocusWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private ManualFocusWidgetModel widgetModel;
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
        Mockito.when(preferencesManager.getControlMode()).thenReturn(SPOT_METER);
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new ManualFocusWidgetModel(djiSdkModel, keyedStore, preferencesManager);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void manualFocusWidgetModel_getFocusRingValue_valueUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE, 0),
                10, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<Integer> testSubscriber =
                widgetModel.getFocusRingValue().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 10);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void manualFocusWidgetModel_getFocusRingUpperBoundValue_valueUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, 0),
                10, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<Integer> testSubscriber =
                widgetModel.getFocusRingUpperBoundValue().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 10);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void manualFocusWidgetModel_isManualFocusMode_valueUpdate() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE), MANUAL_FOCUS, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, 0));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isManualFocusMode().test();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void manualFocusWidgetModel_setFocusRingValue_success() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE),
                10, null);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setFocusRingValue(10).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void manualFocusWidgetModel_setFocusRingValue_throwsError() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE_UPPER_BOUND, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_RING_VALUE),
                10, uxsdkError);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setFocusRingValue(10).test();
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
