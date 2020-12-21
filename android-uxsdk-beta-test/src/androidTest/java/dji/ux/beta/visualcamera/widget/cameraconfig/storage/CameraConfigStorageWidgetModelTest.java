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

package dji.ux.beta.visualcamera.widget.cameraconfig.storage;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.ResolutionAndFrameRate;
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

import static dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidgetModel.INVALID_AVAILABLE_CAPTURE_COUNT_LONG;
import static dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidgetModel.INVALID_AVAILABLE_RECORDING_TIME;

/**
 * Test:
 * This class tests the public methods in the {@link CameraConfigStorageWidgetModel}
 * 1.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_imageFormat_cameraModeIsUpdated()}
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_imageFormat_flatCameraModeIsUpdated()}
 * Test that the image format's camera mode is updated.
 * 2.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_imageFormat_photoFileFormatIsUpdated()}
 * Test that the image format's photo file format is updated.
 * 3.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_imageFormat_resolutionAndFrameRateAreUpdated()}
 * Test that the image format's resolution and frame rate are updated.
 * 4.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_cameraModeIsUpdated()}
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_flatCameraModeIsUpdated()}
 * Test that the camera storage state's camera mode is updated.
 * 5.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_sdCardStateNormal()}
 * Test that the camera storage state's storage location and operation state are updated when the
 * sd card state is normal.
 * 6.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_storageStateNormal()}
 * Test that the camera storage state's storage location and operation state are updated when the
 * storage state is normal.
 * 7.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_innerStorageStateNormal()}
 * Test that the camera storage state's storage location and operation state are updated when the
 * inner storage state is normal.
 * 8.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_sdCardAvailableSpace()}
 * Test that the camera storage state's available space is updated when the storage location is the
 * SD card.
 * 9.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraStorageState_innerStorageAvailableSpace()}
 * Test that the camera storage state's available space is updated when the storage location is the
 * internal storage.
 * 10.
 * {@link CameraConfigStorageWidgetModelTest#cameraConfigStorageWidgetModel_cameraColor_isUpdated()}
 * Test that the camera color is updated.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraConfigStorageWidgetModelTest {
    private static CameraConfigStorageWidgetModel.CameraStorageState DEFAULT_CAMERA_STORAGE_STATE =
            new CameraConfigStorageWidgetModel.CameraStorageState(
                    SettingsDefinitions.CameraMode.UNKNOWN,
                    SettingsDefinitions.StorageLocation.UNKNOWN,
                    SettingsDefinitions.SDCardOperationState.UNKNOWN,
                    INVALID_AVAILABLE_CAPTURE_COUNT_LONG,
                    INVALID_AVAILABLE_RECORDING_TIME);
    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    private CompositeDisposable compositeDisposable;
    private CameraConfigStorageWidgetModel widgetModel;
    private TestScheduler testScheduler;
    private int cameraIndex = 0;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new CameraConfigStorageWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);

    }

    @Test
    public void cameraConfigStorageWidgetModel_imageFormat_cameraModeIsUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the image format flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.ImageFormat> testSubscriber = widgetModel.getImageFormat().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, imageFormat -> imageFormat.getCameraMode() == SettingsDefinitions.CameraMode.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, imageFormat -> imageFormat.getCameraMode() == SettingsDefinitions.CameraMode.RECORD_VIDEO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_imageFormat_flatCameraModeIsUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 8, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the image format flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.ImageFormat> testSubscriber = widgetModel.getImageFormat().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, imageFormat -> imageFormat.getCameraMode() == SettingsDefinitions.CameraMode.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, imageFormat -> imageFormat.getCameraMode() == SettingsDefinitions.CameraMode.RECORD_VIDEO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_imageFormat_photoFileFormatIsUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, cameraIndex),
                SettingsDefinitions.PhotoFileFormat.JPEG,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the image format flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.ImageFormat> testSubscriber = widgetModel.getImageFormat().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, imageFormat -> imageFormat.getPhotoFileFormat() == SettingsDefinitions.PhotoFileFormat.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, imageFormat -> imageFormat.getPhotoFileFormat() == SettingsDefinitions.PhotoFileFormat.JPEG);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_imageFormat_resolutionAndFrameRateAreUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        ResolutionAndFrameRate testResolutionAndFrameRate = new ResolutionAndFrameRate(
                SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex),
                testResolutionAndFrameRate,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the image format flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.ImageFormat> testSubscriber = widgetModel.getImageFormat().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, imageFormat -> imageFormat.getResolution() == SettingsDefinitions.VideoResolution.UNKNOWN
                && imageFormat.getFrameRate() == SettingsDefinitions.VideoFrameRate.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, imageFormat -> imageFormat.getResolution() == SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080
                && imageFormat.getFrameRate() == SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_cameraModeIsUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO,
                20,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, cameraStorageState -> cameraStorageState.getCameraMode() == SettingsDefinitions.CameraMode.RECORD_VIDEO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_flatCameraModeIsUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true,
                18,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL,
                20,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(2, cameraStorageState -> cameraStorageState.getCameraMode() == SettingsDefinitions.CameraMode.RECORD_VIDEO);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_sdCardStateNormal() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, cameraStorageState -> cameraStorageState.getStorageLocation() == SettingsDefinitions.StorageLocation.SDCARD
                && cameraStorageState.getStorageOperationState() == SettingsDefinitions.SDCardOperationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_storageStateNormal() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, cameraStorageState -> cameraStorageState.getStorageLocation() == SettingsDefinitions.StorageLocation.SDCARD
                && cameraStorageState.getStorageOperationState() == SettingsDefinitions.SDCardOperationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_innerStorageStateNormal() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, cameraStorageState -> cameraStorageState.getStorageLocation() == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE
                && cameraStorageState.getStorageOperationState() == SettingsDefinitions.SDCardOperationState.NORMAL);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_sdCardAvailableSpace() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex),
                1L,
                20,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex),
                2,
                25,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(3, cameraStorageState -> cameraStorageState.getStorageLocation() == SettingsDefinitions.StorageLocation.SDCARD
                && cameraStorageState.getStorageOperationState() == SettingsDefinitions.SDCardOperationState.NORMAL
                && cameraStorageState.getAvailableCaptureCount() == 1L
                && cameraStorageState.getAvailableRecordingTime() == 2);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraStorageState_innerStorageAvailableSpace() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                10,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                15,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex),
                1L,
                20,
                TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex),
                2,
                25,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera storage state flowable from the model
        TestSubscriber<CameraConfigStorageWidgetModel.CameraStorageState> testSubscriber = widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValue(DEFAULT_CAMERA_STORAGE_STATE);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(3, cameraStorageState -> cameraStorageState.getStorageLocation() == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE
                && cameraStorageState.getStorageOperationState() == SettingsDefinitions.SDCardOperationState.NORMAL
                && cameraStorageState.getAvailableCaptureCount() == 1L
                && cameraStorageState.getAvailableRecordingTime() == 2);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void cameraConfigStorageWidgetModel_cameraColor_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_COLOR, cameraIndex),
                SettingsDefinitions.CameraColor.ART,
                10,
                TimeUnit.SECONDS);
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup();

        // Initialize a test subscriber that subscribes to the camera color flowable from the model
        TestSubscriber<SettingsDefinitions.CameraColor> testSubscriber = widgetModel.getCameraColor().test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(0, cameraColor -> cameraColor == SettingsDefinitions.CameraColor.UNKNOWN);

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValueAt(1, cameraColor -> cameraColor == SettingsDefinitions.CameraColor.ART);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_COLOR, cameraIndex));
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, cameraIndex);
    }
}
