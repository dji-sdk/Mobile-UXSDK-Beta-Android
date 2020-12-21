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

package dji.ux.beta.visualcamera.widget.cameraconfig.iso;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Test:
 * This class tests the public methods in the {@link CameraConfigISOAndEIWidgetModel}
 * 1.
 * {@link CameraConfigISOAndEIWidgetModelTest#cameraConfigISOAndEIWidgetModel_iso_isUpdated()}
 * Test that the ISO is updated.
 * 2.
 * {@link CameraConfigISOAndEIWidgetModelTest#cameraConfigISOAndEIWidgetModel_eiMode_isUpdated()}
 * Test that the EI mode is updated.
 * 3.
 * {@link CameraConfigISOAndEIWidgetModelTest#cameraConfigISOAndEIWidgetModel_isoAndEIValue_isEIValue()}
 * Test that the ISO and EI value method returns the EI value.
 * 4.
 * {@link CameraConfigISOAndEIWidgetModelTest#cameraConfigISOAndEIWidgetModel_isoAndEIValue_isLocked()}
 * Test that the ISO and EI value method returns the {@link CameraConfigISOAndEIWidgetModel#LOCKED_ISO_VALUE} value.
 * 5.
 * {@link CameraConfigISOAndEIWidgetModelTest#cameraConfigISOAndEIWidgetModel_isoAndEIValue_isISOValue()}
 * Test that the ISO and EI value method returns the ISO value.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraConfigISOAndEIWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private CameraConfigISOAndEIWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraConfigISOAndEIWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void cameraConfigISOAndEIWidgetModel_iso_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ISO, 0),
                SettingsDefinitions.ISO.ISO_800,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        TestSubscriber<SettingsDefinitions.ISO> testSubscriber = widgetModel.getISO().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingsDefinitions.ISO.UNKNOWN);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingsDefinitions.ISO.UNKNOWN, SettingsDefinitions.ISO.ISO_800);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigISOAndEIWidgetModel_eiMode_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.EI,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the EI mode flowable from the model
        TestSubscriber<Boolean> testSubscriber = widgetModel.isEIMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigISOAndEIWidgetModel_isoAndEIValue_isEIValue() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.EI,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EI_VALUE, 0),
                500,
                20,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ISO and EI flowable from the model
        TestSubscriber<String> testSubscriber = widgetModel.getISOAndEIValue().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, s -> s.equals("500"));

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigISOAndEIWidgetModel_isoAndEIValue_isLocked() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ISO, 0),
                SettingsDefinitions.ISO.FIXED,
                15,
                TimeUnit.SECONDS);

        ExposureSettings exposureSettings = new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                20,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ISO and EI flowable from the model
        TestSubscriber<String> testSubscriber = widgetModel.getISOAndEIValue().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(3, s -> s.equals(CameraConfigISOAndEIWidgetModel.LOCKED_ISO_VALUE));

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigISOAndEIWidgetModel_isoAndEIValue_isISOValue() {
// Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS);

        ExposureSettings exposureSettings = new ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                400,
                SettingsDefinitions.ExposureCompensation.UNKNOWN);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                15,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ISO and EI flowable from the model
        TestSubscriber<String> testSubscriber = widgetModel.getISOAndEIValue().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue("0");

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, s -> s.equals("400"));

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SETTINGS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.ISO));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EI_VALUE));
    }
}
