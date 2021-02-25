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

package dji.ux.beta.cameracore.widget.camerashuttersetting;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.ExposureState;
import dji.common.camera.SettingsDefinitions.ShutterSpeed;
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
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

import static dji.common.camera.SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY;

/**
 * Test:
 * This class tests the public methods in {@link CameraShutterSettingWidgetModel}
 * <p>
 * 1. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_getShutterSpeedRange_updateOrder()}
 * Test the update order of Shutter Speed Range value
 * 2. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_getShutterSpeed_updateOrder()}
 * Test the update order of Shutter Speed value based on Exposure Settings
 * 3. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_getCurrentShutterSpeedValue_updateOrder()}
 * Test the update order of Shutter Speed value based on Shutter Value Key
 * 4. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_getExposureState_updateOrder()}
 * Test the update order of  Exposure State
 * 5. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_isChangeShutterSpeedSupported_updateOrder()}
 * Test the update order of is change shutter speed supported based on camera exposure mode
 * 6. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_setShutterSpeed_success()}
 * Test if the shutter speed value is changed successfully
 * 7. {@link CameraShutterSettingWidgetModelTest#cameraShutterSettingWidgetModel_setShutterSpeed_error()}
 * Test if set shutter speed value fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraShutterSettingWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private CameraShutterSettingWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private ShutterSpeed[] shutterRange = new ShutterSpeed[]{
            ShutterSpeed.SHUTTER_SPEED_1_640,
            ShutterSpeed.SHUTTER_SPEED_1_500,
            ShutterSpeed.SHUTTER_SPEED_1_400,
            ShutterSpeed.SHUTTER_SPEED_1_320,
            ShutterSpeed.SHUTTER_SPEED_1_240,
            ShutterSpeed.SHUTTER_SPEED_1_200,
            ShutterSpeed.SHUTTER_SPEED_1_160,
            ShutterSpeed.SHUTTER_SPEED_1_120,
            ShutterSpeed.SHUTTER_SPEED_1_100};

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraShutterSettingWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);


    }

    @Test
    public void cameraShutterSettingWidgetModel_getShutterSpeedRange_updateOrder() {
        initKeys();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHUTTER_SPEED_RANGE, 0),
                shutterRange, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ShutterSpeed[]> testSubscriber =
                widgetModel.getShutterSpeedRange().test();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, result -> shutterRange == result);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    @Test
    public void cameraShutterSettingWidgetModel_getShutterSpeed_updateOrder() {
        ExposureSettings updatedExposureSettings = new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                ShutterSpeed.SHUTTER_SPEED_1_30,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN);
        initKeys();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                updatedExposureSettings, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ShutterSpeed> testSubscriber =
                widgetModel.getCurrentShutterSpeedValue().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(ShutterSpeed.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(ShutterSpeed.UNKNOWN, ShutterSpeed.SHUTTER_SPEED_1_30);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    @Test
    public void cameraShutterSettingWidgetModel_getCurrentShutterSpeedValue_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHUTTER_SPEED, 0),
                ShutterSpeed.SHUTTER_SPEED_1_30, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ShutterSpeed> testSubscriber =
                widgetModel.getCurrentShutterSpeedValue().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(ShutterSpeed.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(ShutterSpeed.UNKNOWN, ShutterSpeed.SHUTTER_SPEED_1_30);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    @Test
    public void cameraShutterSettingWidgetModel_getExposureState_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_STATE, 0),
                ExposureState.OVEREXPOSED, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ExposureState> testSubscriber =
                widgetModel.getExposureState().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(ExposureState.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(ExposureState.UNKNOWN, ExposureState.OVEREXPOSED);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    @Test
    public void cameraShutterSettingWidgetModel_isChangeShutterSpeedSupported_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                SHUTTER_PRIORITY, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isChangeShutterSpeedSupported().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    @Test
    public void cameraShutterSettingWidgetModel_setShutterSpeed_success() {
        initKeys();
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.SHUTTER_SPEED, 0),
                ShutterSpeed.SHUTTER_SPEED_1_30, null);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setShutterSpeedValue(ShutterSpeed.SHUTTER_SPEED_1_30).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);

    }


    @Test
    public void cameraShutterSettingWidgetModel_setShutterSpeed_error() {
        initKeys();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.SHUTTER_SPEED, 0),
                ShutterSpeed.SHUTTER_SPEED_1_30, uxsdkError);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setShutterSpeedValue(ShutterSpeed.SHUTTER_SPEED_1_30).test();
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

    private void initKeys() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SHUTTER_SPEED, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_STATE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SHUTTER_SPEED_RANGE, 0));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
    }

}
