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

package dji.ux.beta.cameracore.widget.cameraaperturesetting;

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

import static dji.common.camera.SettingsDefinitions.Aperture;
import static dji.common.camera.SettingsDefinitions.ExposureMode.MANUAL;

/**
 * Test:
 * This class tests the public methods in {@link CameraApertureSettingWidgetModel}
 * <p>
 * 1. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_getApertureRange_updateOrder()}
 * Test the update order of Aperture Range value
 * 2. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_getAperture_updateOrder()}
 * Test the update order of Aperture value based on Exposure Settings
 * 3. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_getCurrentApertureValue_updateOrder()}
 * Test the update order of Aperture value based on Aperture Value key
 * 4. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_isChangeApertureSupported_variableAperture_updateOrder()}
 * Test the update order of isChangeableSupported value based on adjustable aperture supported
 * 5. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_isChangeApertureSupported_exposureMode_updateOrder()}
 * Test the update order of isChangeableSupported value based on exposure mode
 * 6. {@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_isChangeApertureSupported_updateOrder()}
 * Test the update order of isChangeableSupported value based on exposure mode and adjustable aperture supported
 * 7.{@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_setApertureValue_success()}
 * Test if the aperture value is changed successfully
 * 8.{@link CameraApertureSettingWidgetModelTest#cameraApertureSettingWidgetModel_setApertureValue_error()}
 * Test if changing the aperture value fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraApertureSettingWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private CameraApertureSettingWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private Aperture[] apertureRange = new Aperture[]{
            Aperture.F_2_DOT_8,
            Aperture.F_3_DOT_2,
            Aperture.F_3_DOT_5,
            Aperture.F_4,
            Aperture.F_4_DOT_5,
            Aperture.F_5,
            Aperture.F_5_DOT_6};

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraApertureSettingWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }


    @Test
    public void cameraApertureSettingWidgetModel_getApertureRange_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.APERTURE_RANGE, 0),
                apertureRange, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Aperture[]> testSubscriber =
                widgetModel.getApertureRange().test();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, result -> apertureRange == result);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    @Test
    public void cameraApertureSettingWidgetModel_getAperture_updateOrder() {
        ExposureSettings updatedExposureSettings = new ExposureSettings(Aperture.F_10,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN);
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                updatedExposureSettings, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Aperture> testSubscriber =
                widgetModel.getCurrentApertureValue().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(Aperture.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(Aperture.UNKNOWN, Aperture.F_10);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }

    @Test
    public void cameraApertureSettingWidgetModel_getCurrentApertureValue_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.APERTURE, 0),
                Aperture.F_10, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Aperture> testSubscriber =
                widgetModel.getCurrentApertureValue().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(Aperture.UNKNOWN);
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(Aperture.UNKNOWN, Aperture.F_10);
        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }


    @Test
    public void cameraApertureSettingWidgetModel_isChangeApertureSupported_variableAperture_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED, 0),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isChangeApertureSupported().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    @Test
    public void cameraApertureSettingWidgetModel_isChangeApertureSupported_exposureMode_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED, 0),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isChangeApertureSupported().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    @Test
    public void cameraApertureSettingWidgetModel_isChangeApertureSupported_updateOrder() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED, 0),
                true, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                MANUAL, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isChangeApertureSupported().test();
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);

    }


    @Test
    public void cameraApertureSettingWidgetModel_setApertureValue_success() {
        initKeys();
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.APERTURE, 0),
                Aperture.F_10, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setApertureValue(Aperture.F_10).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void cameraApertureSettingWidgetModel_setApertureValue_error() {
        initKeys();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.APERTURE, 0),
                Aperture.F_10, uxsdkError);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setApertureValue(Aperture.F_10).test();
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
                CameraKey.create(CameraKey.CAMERA_TYPE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.APERTURE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_ADJUSTABLE_APERTURE_SUPPORTED, 0));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.APERTURE_RANGE, 0));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
    }
}
