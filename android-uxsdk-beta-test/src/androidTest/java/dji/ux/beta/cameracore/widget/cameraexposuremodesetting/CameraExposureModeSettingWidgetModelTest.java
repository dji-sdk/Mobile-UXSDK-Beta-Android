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

package dji.ux.beta.cameracore.widget.cameraexposuremodesetting;

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

import static dji.common.camera.SettingsDefinitions.ExposureMode;

/**
 * Test:
 * This class tests the public methods in {@link CameraExposureModeSettingWidgetModel}
 * <p>
 * 1. {@link CameraExposureModeSettingWidgetModelTest#cameraExposureModeSettingWidgetModel_getExposureModeRange_isUpdated()}
 * Test if the exposure mode range value is updated
 * 2. {@link CameraExposureModeSettingWidgetModelTest#cameraExposureModeSettingWidgetModel_getExposureMode_isUpdated()}
 * Test if the exposure mode value is updated
 * 3.{@link CameraExposureModeSettingWidgetModelTest#cameraExposureModeSettingWidgetModel_setExposureModeValue_success()}
 * Test if the exposure mode value is changed successfully
 * 4.{@link CameraExposureModeSettingWidgetModelTest#cameraExposureModeSettingWidgetModel_setExposureModeValue_error()}
 * Test if changing the exposure mode value fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraExposureModeSettingWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private CameraExposureModeSettingWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private ExposureMode[] exposureModeRange = new ExposureMode[]{
            ExposureMode.PROGRAM,
            ExposureMode.SHUTTER_PRIORITY,
            ExposureMode.APERTURE_PRIORITY,
            ExposureMode.MANUAL};

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraExposureModeSettingWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void cameraExposureModeSettingWidgetModel_getExposureModeRange_isUpdated() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE_RANGE, 0),
                exposureModeRange, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ExposureMode[]> testSubscriber = widgetModel.getExposureModeRange().test();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, result -> exposureModeRange == result);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraExposureModeSettingWidgetModel_getExposureMode_isUpdated() {
        initKeys();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                ExposureMode.MANUAL, 20, TimeUnit.SECONDS);
        widgetModel.setup();
        TestSubscriber<ExposureMode> testSubscriber = widgetModel.getExposureMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(ExposureMode.UNKNOWN);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(ExposureMode.UNKNOWN, ExposureMode.MANUAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraExposureModeSettingWidgetModel_setExposureModeValue_success() {
        initKeys();
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                ExposureMode.MANUAL, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setExposureMode(ExposureMode.MANUAL).test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void cameraExposureModeSettingWidgetModel_setExposureModeValue_error() {
        initKeys();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                ExposureMode.MANUAL, uxsdkError);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setExposureMode(ExposureMode.MANUAL).test();
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
                CameraKey.create(CameraKey.EXPOSURE_MODE_RANGE, 0));
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
    }
}
