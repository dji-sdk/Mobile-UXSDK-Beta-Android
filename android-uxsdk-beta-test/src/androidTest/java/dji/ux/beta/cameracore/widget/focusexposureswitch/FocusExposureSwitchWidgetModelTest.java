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

package dji.ux.beta.cameracore.widget.focusexposureswitch;

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

import dji.common.camera.SettingsDefinitions;
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
 * This class tests the public methods in {@link FocusExposureSwitchWidgetModel}
 * 1. {@link FocusExposureSwitchWidgetModelTest#focusExposureSwitchWidgetModel_getControlMode_updateOrder()}
 * Test the update order of control mode
 * 2. {@link FocusExposureSwitchWidgetModelTest#focusExposureSwitchWidgetModel_switchControlMode_success()}
 * Test if the switch of the control mode is successful
 * 3. {@link FocusExposureSwitchWidgetModelTest#focusExposureSwitchWidgetModel_switchControlMode_error()}
 * Test if the switch of the control mode fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FocusExposureSwitchWidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private GlobalPreferencesInterface preferencesManager;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private FocusExposureSwitchWidgetModel widgetModel;
    private TestScheduler testScheduler;


    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        Mockito.when(preferencesManager.getControlMode()).thenReturn(SettingDefinitions.ControlMode.SPOT_METER);
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new FocusExposureSwitchWidgetModel(djiSdkModel, keyedStore, preferencesManager);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void focusExposureSwitchWidgetModel_getControlMode_updateOrder() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.CENTER_METER,
                15,
                TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<SettingDefinitions.ControlMode> testSubscriber =
                widgetModel.getControlMode().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingDefinitions.ControlMode.SPOT_METER);
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingDefinitions.ControlMode.SPOT_METER, SettingDefinitions.ControlMode.CENTER_METER);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void focusExposureSwitchWidgetModel_switchControlMode_success() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE, 0),
                SettingsDefinitions.FocusMode.AFC,
                5,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE, null);

        widgetModel.setup();


        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.switchControlMode().test();
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void focusExposureSwitchWidgetModel_switchControlMode_error() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FOCUS_MODE, 0),
                SettingsDefinitions.FocusMode.AFC,
                5,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE),
                SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE, uxsdkError);

        widgetModel.setup();


        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.switchControlMode().test();
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }


    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.FOCUS_MODE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.METERING_MODE, 0));
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE));
    }
}
