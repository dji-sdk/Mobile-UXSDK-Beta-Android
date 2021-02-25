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

package dji.ux.beta.cameracore.widget.cameracapture.shootphoto;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.camera.SSDOperationState;
import dji.common.camera.SettingsDefinitions.FlatCameraMode;
import dji.common.camera.SettingsDefinitions.PhotoAEBCount;
import dji.common.camera.SettingsDefinitions.PhotoBurstCount;
import dji.common.camera.SettingsDefinitions.PhotoTimeIntervalSettings;
import dji.common.camera.SettingsDefinitions.SDCardOperationState;
import dji.common.camera.SettingsDefinitions.ShootPhotoMode;
import dji.common.camera.SettingsDefinitions.StorageLocation;
import dji.keysdk.CameraKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

import static dji.common.camera.SettingsDefinitions.PhotoPanoramaMode.PANORAMA_MODE_3X3;

/**
 * Class tests the public methods of {@link ShootPhotoWidgetModel}
 * 1. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_canStartShootingPhoto_updateOrder()}
 * Test if the device can start shooting photo
 * 2. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_canStopShootingPhoto_interval_updateOrder()}
 * Test if the device can stop shooting photo if currently shooting interval mode photo
 * 3. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_canStopShootingPhoto_panorama_updateOrder()}
 * Test if the device can stop shooting photo if currently shooting panorama mode photo
 * 4. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_isShootingPhoto_updateOrder()}
 * Test if the the device is currently shooting photo
 * 5. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_isStoringPhoto_updateOrder()}
 * Test if the the device is currently storing photo
 * 6. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraDisplayName_updateOrder()}
 * Test the camera display name
 * 7. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_single_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_single_updateOrder()}
 * Test Camera Photo State when camera photo mode is single
 * 8. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_hdr_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_hdr_updateOrder()}
 * Test Camera Photo State when camera photo mode is hdr
 * 9. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_hyperLight_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_hyperLight_updateOrder()}
 * Test Camera Photo State when camera photo mode is hyperlight
 * 10. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_shallowFocus_updateOrder()}
 * Test Camera Photo State when camera photo mode is shallow focus
 * 11. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_eHDR_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_eHDR_updateOrder()}
 * Test Camera Photo State when camera photo mode is eHDR
 * 12. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_burstMode_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_burstMode_updateOrder()}
 * Test Camera Photo State when camera photo mode is burst mode with count
 * 13. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_rawBurstMode_updateOrder()}
 * Test Camera Photo State when camera photo mode is raw burst mode with count
 * 14. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_aeb_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_aeb_updateOrder()}
 * Test Camera Photo State when camera photo mode is aeb with count
 * 15. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_interval_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_interval_updateOrder()}
 * Test Camera Photo State when camera photo mode is interval with count and interval duration
 * 16. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraPhotoState_panorama_updateOrder()}
 * {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getFlatCameraPhotoState_panorama_updateOrder()}
 * Test Camera Photo State when camera photo mode is panorama
 * 17. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraStorageState_SDCardState_updateOrder()}
 * Test Camera Storage State when storage location is SD Card
 * 18. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraStorageState_internalStorage_updateOrder()}
 * Test Camera Storage State when storage location is internal storage
 * 19. {@link ShootPhotoWidgetModelTest#shootPhotoWidgetModel_getCameraStorageState_SSDStorage_updateOrder()}
 * Test Camera Storage State when storage location is SSD
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ShootPhotoWidgetModelTest {

    @Mock
    private DJISDKModel djiSdkModel;

    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    private CompositeDisposable compositeDisposable;
    private ShootPhotoWidgetModel widgetModel;
    private TestScheduler testScheduler;
    private int cameraIndex = 0;
    private CameraPhotoState defaultCameraPhotoState = new CameraPhotoState(ShootPhotoMode.UNKNOWN);
    private CameraSDPhotoStorageState defaultStorageState =
            new CameraSDPhotoStorageState(StorageLocation.SDCARD,
                    0, SDCardOperationState.NOT_INSERTED);

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new ShootPhotoWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel);
    }

    @Test
    public void shootPhotoWidgetModel_canStartShootingPhoto_updateOrder() {
        setEmptyValues();
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO_ENABLED, cameraIndex),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.canStartShootingPhoto().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_canStopShootingPhoto_interval_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.canStopShootingPhoto().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_canStopShootingPhoto_panorama_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.canStopShootingPhoto().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_isShootingPhoto_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isShootingPhoto().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_isStoringPhoto_updateOrder() {
        setEmptyValues();

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_STORING_PHOTO, cameraIndex),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isStoringPhoto().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraDisplayName_updateOrder() {
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

    //region tests for getCameraPhotoState()
    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_single_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.SINGLE);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.SINGLE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_single_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.SINGLE);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_SINGLE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_hdr_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.HDR);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.HDR, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_hdr_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.HDR);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_HDR, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_hyperLight_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.HYPER_LIGHT);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.HYPER_LIGHT, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }



    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_hyperLight_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.HYPER_LIGHT);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_HYPER_LIGHT, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_shallowFocus_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.SHALLOW_FOCUS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.SHALLOW_FOCUS, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_eHDR_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.EHDR);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.EHDR, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_eHDR_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPhotoState(ShootPhotoMode.EHDR);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_EHDR, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_burstMode_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraBurstPhotoState(ShootPhotoMode.BURST,
                PhotoBurstCount.BURST_COUNT_2);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.BURST, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_BURST_COUNT, cameraIndex),
                PhotoBurstCount.BURST_COUNT_2, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_burstMode_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraBurstPhotoState(ShootPhotoMode.BURST,
                PhotoBurstCount.BURST_COUNT_2);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_BURST, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_BURST_COUNT, cameraIndex),
                PhotoBurstCount.BURST_COUNT_2, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_rawBurstMode_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraBurstPhotoState(ShootPhotoMode.RAW_BURST,
                PhotoBurstCount.BURST_COUNT_14);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.RAW_BURST, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_RAW_BURST_COUNT, cameraIndex),
                PhotoBurstCount.BURST_COUNT_14, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_aeb_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraAEBPhotoState(ShootPhotoMode.AEB, PhotoAEBCount.AEB_COUNT_7);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.AEB, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_AEB_COUNT, cameraIndex),
                PhotoAEBCount.AEB_COUNT_7, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_aeb_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraAEBPhotoState(ShootPhotoMode.AEB, PhotoAEBCount.AEB_COUNT_7);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_AEB, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_AEB_COUNT, cameraIndex),
                PhotoAEBCount.AEB_COUNT_7, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_interval_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraIntervalPhotoState(ShootPhotoMode.INTERVAL, 20, 10);
        PhotoTimeIntervalSettings intervalSettings = new PhotoTimeIntervalSettings(20, 10);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.INTERVAL, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, cameraIndex),
                intervalSettings, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_interval_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraIntervalPhotoState(ShootPhotoMode.INTERVAL, 20, 10);
        PhotoTimeIntervalSettings intervalSettings = new PhotoTimeIntervalSettings(20, 10);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_INTERVAL, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, cameraIndex),
                intervalSettings, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraPhotoState_panorama_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPanoramaPhotoState(ShootPhotoMode.PANORAMA, PANORAMA_MODE_3X3);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.PANORAMA, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, cameraIndex),
                PANORAMA_MODE_3X3, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getFlatCameraPhotoState_panorama_updateOrder() {
        setEmptyValues();

        CameraPhotoState cameraPhotoState = new CameraPanoramaPhotoState(ShootPhotoMode.PANORAMA, PANORAMA_MODE_3X3);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                FlatCameraMode.PHOTO_PANORAMA, 20, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, cameraIndex),
                PANORAMA_MODE_3X3, 21, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoState> testSubscriber =
                widgetModel.getCameraPhotoState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultCameraPhotoState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultCameraPhotoState, cameraPhotoState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }
    //endregion

    //region tests for getCameraStorageState()
    @Test
    public void shootPhotoWidgetModel_getCameraStorageState_SDCardState_updateOrder() {
        setEmptyValues();
        CameraSDPhotoStorageState resultStorageState =
                new CameraSDPhotoStorageState(StorageLocation.SDCARD,
                        10000L, SDCardOperationState.NORMAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex),
                SDCardOperationState.NORMAL, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex),
                10000L, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                StorageLocation.SDCARD, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoStorageState> testSubscriber =
                widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraStorageState_internalStorage_updateOrder() {
        setEmptyValues();
        CameraSDPhotoStorageState resultStorageState =
                new CameraSDPhotoStorageState(StorageLocation.INTERNAL_STORAGE,
                        10000L, SDCardOperationState.NORMAL);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex),
                SDCardOperationState.NORMAL, 16, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex),
                10000L, 18, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex),
                StorageLocation.INTERNAL_STORAGE, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<CameraPhotoStorageState> testSubscriber =
                widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void shootPhotoWidgetModel_getCameraStorageState_SSDStorage_updateOrder() {
        setEmptyValues();
        CameraSSDPhotoStorageState resultStorageState =
                new CameraSSDPhotoStorageState(StorageLocation.UNKNOWN,
                        1000, SSDOperationState.FORMATTING);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 12, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex),
                ShootPhotoMode.RAW_BURST, 19, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex),
                SSDOperationState.FORMATTING, 19, TimeUnit.SECONDS);
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.RAW_PHOTO_BURST_COUNT, cameraIndex),
                1000, 20, TimeUnit.SECONDS);

        widgetModel.setup();

        TestSubscriber<CameraPhotoStorageState> testSubscriber =
                widgetModel.getCameraStorageState().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(defaultStorageState);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(defaultStorageState, resultStorageState);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }
    //endregion

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }

    private void setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.START_SHOOT_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STOP_SHOOT_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_AEB_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_BURST_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_TIME_INTERVAL_SETTINGS, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_RAW_BURST_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_PANORAMA_MODE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_STORING_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SSD_OPERATION_STATE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.RAW_PHOTO_BURST_COUNT, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.IS_SHOOTING_PHOTO_ENABLED, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.CONNECTION));
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, cameraIndex);
    }
}
