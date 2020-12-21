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

package dji.ux.beta.cameracore.widget.cameracontrols;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

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
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.observers.TestObserver;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidgetModel;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Class will test the public methods in {@link PhotoVideoSwitchWidgetModel}
 * <p>
 * 1. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_isEnabled_updateOrder()}
 * Test the update order for isEnabled
 * 2. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_isPictureMode_isUpdated()}
 * Test whether the current mode is picture mode is updated
 * 3. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_isPictureMode_isUpdatedForFlatCamera()}
 * Test whether the current mode is picture mode is updated when flat camera mode is supported
 * 4. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_toggleCameraMode_success()}
 * Test change of camera mode is success
 * 5. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_toggleCameraMode_error()}
 * Test change of camera mode fails
 * 6. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_toggleFlatCameraMode_success()}
 * Test change of camera mode is success
 * 7. {@link PhotoVideoSwitchWidgetModelTest#photoVideoSwitchModel_toggleFlatCameraMode_error()}
 * Test change of camera mode fails
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PhotoVideoSwitchWidgetModelTest {
    private CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private PhotoVideoSwitchWidgetModel widgetModel;
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
        widgetModel = new PhotoVideoSwitchWidgetModel(djiSdkModel, keyedStore);

        WidgetTestUtil.initialize(djiSdkModel);
    }

    @Test
    public void photoVideoSwitchModel_isEnabled_updateOrder() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isEnabled().test();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false, true);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void photoVideoSwitchModel_isPictureMode_isUpdated() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 10, TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isPictureMode().test();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void photoVideoSwitchModel_isPictureMode_isUpdatedForFlatCamera() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 8, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, 10, TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isPictureMode().test();

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false, true);


        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void photoVideoSwitchModel_toggleCameraMode_success() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO, null);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleCameraMode().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void photoVideoSwitchModel_toggleCameraMode_error() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 5, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO, uxsdkError);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleCameraMode().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void photoVideoSwitchModel_toggleFlatCameraMode_success() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL, null);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleCameraMode().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void photoVideoSwitchModel_toggleFlatCameraMode_error() {
        int cameraIndex = 0;
        setEmptyValues(cameraIndex);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 10, TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL, uxsdkError);
        widgetModel.setup();
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleCameraMode().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    private void setEmptyValues(int cameraIndex) {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS);
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, cameraIndex);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
