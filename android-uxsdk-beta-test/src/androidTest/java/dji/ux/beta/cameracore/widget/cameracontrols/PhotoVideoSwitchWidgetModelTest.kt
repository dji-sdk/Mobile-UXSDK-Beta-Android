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
package dji.ux.beta.cameracore.widget.cameracontrols

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidgetModel
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidgetModel.PhotoVideoSwitchState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider.scheduler
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Class will test the public methods in [PhotoVideoSwitchWidgetModel]
 *
 *
 * 1. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_productDisconnected]
 * Test the product disconnected state
 * 2. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_enabled]
 * Test when photo switch widget can be enabled
 * 3. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isRecording_disabled]
 * Test PhotoVideoSwitchState when recording video
 * 4. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isShootingPhoto_disabled]
 * Test PhotoVideoSwitchState when shooting photo
 * 5. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isShootingBurstPhoto_disabled]
 * Test PhotoVideoSwitchState when shooting burst photo
 * 6. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isShootingIntervalPhoto_disabled]
 * Test PhotoVideoSwitchState when shooting interval photo
 * 7. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isShootingRawBurstPhoto_disabled]
 * Test PhotoVideoSwitchState when shooting raw burst photo
 * 8. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_photoVideoSwitchState_isShootingPanoramaPhoto_disabled]
 * Test PhotoVideoSwitchState when shooting panorama photo
 * 9. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_toggleCameraMode_success]
 * Test toggle camera mode success
 * 10. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_toggleCameraMode_error]
 * Test toggle camera mode failure
 * 11. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_toggleFlatCameraMode_success]
 * Test toggle camera mode success in flat camera mode
 * 12. [PhotoVideoSwitchWidgetModelTest.photoVideoSwitchModel_toggleFlatCameraMode_error]
 * Test toggle camera mode failure in flat camera mode
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class PhotoVideoSwitchWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: PhotoVideoSwitchWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        scheduler = testSchedulerProvider
        widgetModel = PhotoVideoSwitchWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_productDisconnected() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected,
                PhotoVideoSwitchState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_enabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.VideoMode }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_photoMode() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(9) { it == PhotoVideoSwitchState.PhotoMode }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isRecording_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isShootingPhoto_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isShootingBurstPhoto_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isShootingIntervalPhoto_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isShootingRawBurstPhoto_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_photoVideoSwitchState_isShootingPanoramaPhoto_disabled() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex),
                false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<PhotoVideoSwitchState> = widgetModel.photoVideoSwitchState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(PhotoVideoSwitchState.ProductDisconnected)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriber.assertValues(PhotoVideoSwitchState.ProductDisconnected, PhotoVideoSwitchState.CameraDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) { it == PhotoVideoSwitchState.Disabled }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun photoVideoSwitchModel_toggleCameraMode_success() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleCameraMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun photoVideoSwitchModel_toggleCameraMode_error() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO, 5, TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex),
                SettingsDefinitions.CameraMode.RECORD_VIDEO, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleCameraMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun photoVideoSwitchModel_toggleFlatCameraMode_success() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleCameraMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun photoVideoSwitchModel_toggleFlatCameraMode_error() {
        val cameraIndex = 0
        setEmptyValues(cameraIndex)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex),
                true, 10, TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex),
                SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleCameraMode().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    private fun setEmptyValues(cameraIndex: Int) {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_RECORDING, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PHOTO, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_INTERVAL_PHOTO, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_BURST_PHOTO, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_RAW_BURST_PHOTO, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_SHOOTING_PANORAMA_PHOTO, cameraIndex))
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, cameraIndex)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }
}