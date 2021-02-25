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

package dji.ux.beta.visualcamera.widget.manualzoom;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Class will test public methods in {@link ManualZoomWidgetModel}
 * <p>
 * 1. {@link ManualZoomWidgetModelTest#manualZoomWidgetModel_isOpticalZoomSupported_updateToSupported()}
 * Test if optical zoom supported method updates value
 * <p>
 * 2. {@link ManualZoomWidgetModelTest#manualZoomWidgetModel_getOpticalZoomSpec_updatedZoomSpec()}
 * Test to check update in zoom specifications
 * <p>
 * 3. {@link ManualZoomWidgetModelTest#manualZoomWidgetModel_getZoomLevel_updatedZoomLevel()}
 * Test to check update in zoom level
 * <p>
 * 4.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_getZoomLevelText_updatedString()}
 * Test to check update in zoom level text
 * <p>
 * 5.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_increaseZoomLevel_success()}
 * Test to check increase zoom level method sets zoom level to current zoom level + zoom level step
 * <p>
 * 6.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_increaseZoomLevel_throwsError()}
 * Test to check if increase zoom level method throws error
 * <p>
 * 7.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_decreaseZoomLevel_success()}
 * Test to check decrease zoom level method sets zoom level to current zoom level - zoom level step
 * <p>
 * 8.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_decreaseZoomLevel_throwsError()}
 * Test to check if decrease zoom level method throws error
 * <p>
 * 9.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_setZoomLevel_success()}
 * Test to check if set zoom level is successful in calculating the value based on scale
 * <p>
 * 10.{@link ManualZoomWidgetModelTest#manualZoomWidgetModel_setZoomLevel_throwsError()}
 * Test to check if set zoom level method throws error
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ManualZoomWidgetModelTest {

    private CompositeDisposable compositeDisposable;

    @Mock
    private DJISDKModel djiSdkModel;
    private ManualZoomWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new ManualZoomWidgetModel(djiSdkModel, keyedStore);

        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void manualZoomWidgetModel_isOpticalZoomSupported_updateToSupported() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isSupported().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void manualZoomWidgetModel_getOpticalZoomSpec_updatedZoomSpec() {
        SettingsDefinitions.OpticalZoomSpec defaultZoomSpec = new SettingsDefinitions.OpticalZoomSpec(0, 0, 0);
        SettingsDefinitions.OpticalZoomSpec zoomSpec = new SettingsDefinitions.OpticalZoomSpec(300, 10, 10);
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                zoomSpec, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<SettingsDefinitions.OpticalZoomSpec> testSubscriber =
                widgetModel.getOpticalZoomSpec().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultZoomSpec);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultZoomSpec, zoomSpec);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void manualZoomWidgetModel_getZoomLevel_updatedZoomLevel() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED));
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<Integer> testSubscriber =
                widgetModel.getCurrentZoomLevel().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 300, 150);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void manualZoomWidgetModel_getZoomLevelText_updatedString() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED));
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC));

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        TestSubscriber<String> testSubscriber =
                widgetModel.getZoomLevelText().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues("0", "15");

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void manualZoomWidgetModel_increaseZoomLevel_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                160, null);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.increaseZoomLevel().test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void manualZoomWidgetModel_increaseZoomLevel_throwsError() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                160, uxsdkError);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.increaseZoomLevel().test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void manualZoomWidgetModel_decreaseZoomLevel_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                140, null);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.decreaseZoomLevel().test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void manualZoomWidgetModel_decreaseZoomLevel_throwsError() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                140, uxsdkError);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.decreaseZoomLevel().test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }


    @Test
    public void manualZoomWidgetModel_setZoomLevel_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                200, null);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setZoomLevel(100).test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void manualZoomWidgetModel_setZoomLevel_throwsError() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_OPTICAL_ZOOM_SUPPORTED),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_SPEC),
                new SettingsDefinitions.OpticalZoomSpec(300, 10, 10), 5, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                150, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel, CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH),
                200, uxsdkError);
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0);
        widgetModel.setup();

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.setZoomLevel(100).test();
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS);
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
