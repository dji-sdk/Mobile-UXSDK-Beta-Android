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

package dji.ux.beta.cameracore.widget.cameracapture.recordvideo;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dji.common.camera.CameraSSDVideoLicense;
import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.ProductKey;
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
 * Class will test public methods in {@link RecordVideoWidgetModel}
 * 1. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_getCameraDisplayName_updateOrder()}
 * Test camera name update order
 * 2. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_recordingState_updated()}
 * Test the update of recording state
 * 3. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_getCameraStorageState_internalStorage_updateOrder()}
 * Test the update of camera storage state for internal storage
 * 4. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_getCameraStorageState_SDCardState_updateOrder()}
 * Test the update of camera storage state for SD Card
 * 5. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_startRecord_success()}
 * Test the success for starting the video recording
 * 6. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_startRecord_error()}
 * Test the error for while starting video recording
 * 7. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_stopRecord_success()}
 * Test the success for stopping the video recording
 * 8. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_stopRecord_error()}
 * Test the error for stopping the video recording
 * 9. {@link RecordVideoWidgetModelTest#recordVideoWidgetModel_getCameraStorageState_SSD_updateOrder()}
 * Test the update of camera storage state for SSD
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecordVideoWidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;
    private CompositeDisposable compositeDisposable;
    private RecordVideoWidgetModel widgetModel;
    private TestScheduler testScheduler;
    private int cameraIndex = 0;
    private CameraSDVideoStorageState defaultStorageState =
            new CameraSDVideoStorageState(SettingsDefinitions.StorageLocation.SDCARD,
                    0, SettingsDefinitions.SDCardOperationState.NOT_INSERTED);

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new RecordVideoWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel);
    }

    @Test
    public void recordVideoWidgetModel_recordingState_updated() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        List<Boolean> emittedValues = Arrays.asList(true, false);
        WidgetTestUtil.setEmittedValues(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                emittedValues, 20, 10, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<RecordVideoWidgetModel.RecordingState> testSubscriber =
                widgetModel.getRecordingState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(RecordVideoWidgetModel.RecordingState.UNKNOWN);
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RecordVideoWidgetModel.RecordingState.UNKNOWN, RecordVideoWidgetModel.RecordingState.RECORDING_IN_PROGRESS);
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        testSubscriber.assertValues(RecordVideoWidgetModel.RecordingState.UNKNOWN,
                RecordVideoWidgetModel.RecordingState.RECORDING_IN_PROGRESS,
                RecordVideoWidgetModel.RecordingState.RECORDING_STOPPED);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void recordVideoWidgetModel_getCameraDisplayName_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex),
                "Test Camera", 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<String> testSubscriber =
                widgetModel.getCameraDisplayName().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue("");

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues("", "Test Camera");

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void recordVideoWidgetModel_getRecordingTimeInSeconds_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CURRENT_VIDEO_RECORDING_TIME_IN_SECONDS, cameraIndex),
                20, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Integer> testSubscriber =
                widgetModel.getRecordingTimeInSeconds().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(0);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 20);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    //region tests for getCameraStorageState()

    @Test
    public void recordVideoWidgetModel_getCameraStorageState_SDCardState_updateOrder() {
        setEmptyValues();

        CameraSDVideoStorageState resultStorageState =
                new CameraSDVideoStorageState(SettingsDefinitions.StorageLocation.SDCARD,
                        10000, SettingsDefinitions.SDCardOperationState.NORMAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex),
                10000, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.SDCARD, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraVideoStorageState> testSubscriber =
                widgetModel.getCameraVideoStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void recordVideoWidgetModel_getCameraStorageState_internalStorage_updateOrder() {
        setEmptyValues();

        CameraSDVideoStorageState resultStorageState =
                new CameraSDVideoStorageState(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                        10000, SettingsDefinitions.SDCardOperationState.NORMAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex),
                SettingsDefinitions.SDCardOperationState.NORMAL, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex),
                10000, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraVideoStorageState> testSubscriber =
                widgetModel.getCameraVideoStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void recordVideoWidgetModel_getCameraStorageState_SSD_updateOrder() {
        setEmptyValues();

        CameraSSDVideoStorageState resultStorageState =
                new CameraSSDVideoStorageState(SettingsDefinitions.StorageLocation.UNKNOWN,
                        10000, SSDOperationState.IDLE);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex),
                SSDOperationState.IDLE, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, cameraIndex),
                CameraSSDVideoLicense.LicenseKeyTypeProRes422HQ, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SSD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex),
                10000, 18, TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<CameraVideoStorageState> testSubscriber =
                widgetModel.getCameraVideoStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }
    //endregion

    @Test
    public void recordVideoWidgetModel_startRecord_success() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.START_RECORD_VIDEO, cameraIndex),
                null);
        widgetModel.setup();
        TestObserver observer = widgetModel.startRecordVideo().test();
        testScheduler.triggerActions();
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void recordVideoWidgetModel_startRecord_error() {
        setEmptyValues();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.START_RECORD_VIDEO, cameraIndex),
                uxsdkError);
        widgetModel.setup();
        TestObserver observer = widgetModel.startRecordVideo().test();
        testScheduler.triggerActions();
        observer.assertError(uxsdkError);

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void recordVideoWidgetModel_stopRecord_success() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                true, 13, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.STOP_RECORD_VIDEO, cameraIndex),
                null);
        widgetModel.setup();
        TestObserver observer = widgetModel.stopRecordVideo().test();
        testScheduler.triggerActions();
        observer.assertComplete();

        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void recordVideoWidgetModel_stopRecord_error() {
        setEmptyValues();
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 1, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 2, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                true, 3, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.STOP_RECORD_VIDEO, cameraIndex),
                 uxsdkError);
        widgetModel.setup();
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.stopRecordVideo().test();
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);
        testScheduler.triggerActions();
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
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_RECORDING, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CURRENT_VIDEO_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.ACTIVATE_SSD_VIDEO_LICENSE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.START_RECORD_VIDEO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STOP_RECORD_VIDEO, cameraIndex));
    }
}