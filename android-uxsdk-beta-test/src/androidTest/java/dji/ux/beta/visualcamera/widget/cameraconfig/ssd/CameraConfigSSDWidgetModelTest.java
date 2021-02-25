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

package dji.ux.beta.visualcamera.widget.cameraconfig.ssd;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.CameraSSDVideoLicense;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SSDOperationState;
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
 * This class tests the public methods in the {@link CameraConfigSSDWidgetModel}
 * 1.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdSupported_isUpdated()}
 * Test that the SSD supported state is updated.
 * 2.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdLicense_isUpdated()}
 * Test that the SSD licence is updated.
 * 3.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdRemainingSpace_isUpdated()}
 * Test that the SSD remaining space is updated.
 * 4.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdClipName_isUpdated()}
 * Test that the SSD clip name is updated.
 * 5.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdResolutionAndFrameRate_isUpdated()}
 * Test that the SSD resolution and frame rate is updated.
 * 6.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdOperationState_isUpdated()}
 * Test that the SSD operation state is updated.
 * 7.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_ssdColor_isUpdated()}
 * Test that the SSD color is updated.
 * 8.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_shootPhotoMode_isUpdated()}
 * Test that the shoot photo mode is updated.
 * 9.
 * {@link CameraConfigSSDWidgetModelTest#cameraConfigSSDWidgetModel_cameraMode_isUpdated()}
 * Test that the camera mode is updated.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraConfigSSDWidgetModelTest {
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private CameraConfigSSDWidgetModel widgetModel;
    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraConfigSSDWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdSupported_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_SSD_SUPPORTED),
                true,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd supported flowable from the model
        TestSubscriber<Boolean> testSubscriber = widgetModel.isSSDSupported().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdLicense_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE),
                CameraSSDVideoLicense.LicenseKeyTypeCinemaDNG,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd license flowable from the model
        TestSubscriber<CameraSSDVideoLicense> testSubscriber = widgetModel.getSSDLicense().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(CameraSSDVideoLicense.Unknown);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(CameraSSDVideoLicense.Unknown, CameraSSDVideoLicense.LicenseKeyTypeCinemaDNG);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdRemainingSpace_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SSD_REMAINING_SPACE_IN_MB),
                10L,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd remaining space flowable from the model
        TestSubscriber<Long> testSubscriber = widgetModel.getSSDRemainingSpace().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(CameraConfigSSDWidgetModel.INVALID_AVAILABLE_CAPTURE_COUNT);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(CameraConfigSSDWidgetModel.INVALID_AVAILABLE_CAPTURE_COUNT, 10L);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdClipName_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        SettingsDefinitions.SSDClipFileName defaultClipFileName = new SettingsDefinitions.SSDClipFileName("", 0, 0);
        SettingsDefinitions.SSDClipFileName testClipFileName = new SettingsDefinitions.SSDClipFileName("test", 1, 2);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SSD_CLIP_FILE_NAME),
                testClipFileName,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd clip file name flowable from the model
        TestSubscriber<SettingsDefinitions.SSDClipFileName> testSubscriber = widgetModel.getSSDClipName().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultClipFileName);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultClipFileName, testClipFileName);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdResolutionAndFrameRate_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        ResolutionAndFrameRate defaultResolutionAndFrameRate = new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.UNKNOWN,
                SettingsDefinitions.VideoFrameRate.UNKNOWN);
        ResolutionAndFrameRate testResolutionAndFrameRate = new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE),
                testResolutionAndFrameRate,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd resolution and frame rate flowable from the model
        TestSubscriber<ResolutionAndFrameRate> testSubscriber = widgetModel.getSSDResolutionAndFrameRate().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultResolutionAndFrameRate);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultResolutionAndFrameRate, testResolutionAndFrameRate);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdOperationState_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SSD_OPERATION_STATE),
                SSDOperationState.IDLE,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd operation state flowable from the model
        TestSubscriber<SSDOperationState> testSubscriber = widgetModel.getSSDOperationState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SSDOperationState.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(SSDOperationState.UNKNOWN, SSDOperationState.IDLE);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_ssdColor_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SSD_COLOR),
                SettingsDefinitions.SSDColor.DLOG,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the ssd color flowable from the model
        TestSubscriber<SettingsDefinitions.SSDColor> testSubscriber = widgetModel.getSSDColor().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingsDefinitions.SSDColor.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingsDefinitions.SSDColor.UNKNOWN, SettingsDefinitions.SSDColor.DLOG);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_shootPhotoMode_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE),
                SettingsDefinitions.ShootPhotoMode.HDR,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the shoot photo mode flowable from the model
        TestSubscriber<SettingsDefinitions.ShootPhotoMode> testSubscriber = widgetModel.getShootPhotoMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingsDefinitions.ShootPhotoMode.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingsDefinitions.ShootPhotoMode.UNKNOWN, SettingsDefinitions.ShootPhotoMode.HDR);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigSSDWidgetModel_cameraMode_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE),
                SettingsDefinitions.CameraMode.RECORD_VIDEO,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera mode flowable from the model
        TestSubscriber<SettingsDefinitions.CameraMode> testSubscriber = widgetModel.getCameraMode().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(SettingsDefinitions.CameraMode.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(SettingsDefinitions.CameraMode.UNKNOWN, SettingsDefinitions.CameraMode.RECORD_VIDEO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SSD_SUPPORTED));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_REMAINING_SPACE_IN_MB));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_CLIP_FILE_NAME));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.MODE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SHOOT_PHOTO_MODE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_OPERATION_STATE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_AVAILABLE_RECORDING_TIME_IN_SECONDS));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_COLOR));
    }
}
