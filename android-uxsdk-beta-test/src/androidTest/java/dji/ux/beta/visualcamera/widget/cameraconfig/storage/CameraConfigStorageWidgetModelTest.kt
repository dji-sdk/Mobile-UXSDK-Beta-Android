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

package dji.ux.beta.visualcamera.widget.cameraconfig.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.ResolutionAndFrameRate
import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidgetModel.CameraConfigStorageState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [CameraConfigStorageWidgetModel]
 * 1.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_productConnected]
 * Test the camera storage state update when product is connected.
 * 2.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_cameraConnected]
 * Test the camera storage state update when camera is connected.
 * 3.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_cameraModeUpdated]
 * Test the camera storage state update when camera mode is updated.
 * 4.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_photoStateUpdated]
 * Test the camera storage photo state update for all parameters.
 * 5.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_videoStateUpdated]
 * Test the camera storage video state update for all parameters.
 * 6.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_storageLocationUpdated]
 * Test the camera storage state update when storage is internal instead of SD Card.
 * 7.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_flatCameraSupported]
 * Test the camera storage state update when aircraft supports flat camera mode.
 * 8.
 * [CameraConfigStorageWidgetModelTest.cameraConfigStorageModel_cameraStorageState_lensModeSupported]
 * Test the camera storage state update when aircraft supports multi lens camera.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigStorageWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: CameraConfigStorageWidgetModel
    private lateinit var testScheduler: TestScheduler
    private val cameraIndex = 0

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigStorageWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_productConnected() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigStorageState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigStorageState.ProductDisconnected, CameraConfigStorageState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_cameraConnected() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigStorageState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigStorageState.ProductDisconnected,
                CameraConfigStorageState.CameraDisconnected,
                CameraConfigStorageState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun cameraConfigStorageModel_cameraStorageState_cameraModeUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, 0),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                12,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigStorageState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigStorageState.ProductDisconnected,
                CameraConfigStorageState.CameraDisconnected,
                CameraConfigStorageState.NotSupported,
                CameraConfigStorageState.PhotoMode(SettingsDefinitions.StorageLocation.UNKNOWN,
                        SettingsDefinitions.SDCardOperationState.UNKNOWN,
                        0,
                        SettingsDefinitions.PhotoFileFormat.UNKNOWN))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_photoStateUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, 0),
                SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, 0),
                SettingsDefinitions.StorageLocation.SDCARD,
                14,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                16,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, 0),
                1024,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, 0),
                SettingsDefinitions.PhotoFileFormat.JPEG,
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(7) {
            it == CameraConfigStorageState.PhotoMode(SettingsDefinitions.StorageLocation.SDCARD,
                    SettingsDefinitions.SDCardOperationState.NORMAL,
                    1024,
                    SettingsDefinitions.PhotoFileFormat.JPEG)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_videoStateUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, 0),
                SettingsDefinitions.CameraMode.RECORD_VIDEO,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, 0),
                SettingsDefinitions.StorageLocation.SDCARD,
                14,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                16,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, 0),
                1024,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, 0),
                ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                        SettingsDefinitions.VideoFrameRate.FRAME_RATE_100_FPS),
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(7) {
            it == CameraConfigStorageState.VideoMode(SettingsDefinitions.StorageLocation.SDCARD,
                    SettingsDefinitions.SDCardOperationState.NORMAL,
                    1024,
                    ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                            SettingsDefinitions.VideoFrameRate.FRAME_RATE_100_FPS))
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_storageLocationUpdated() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE, 0),
                SettingsDefinitions.CameraMode.RECORD_VIDEO,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, 0),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                14,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                16,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, 0),
                1024,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, 0),
                ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                        SettingsDefinitions.VideoFrameRate.FRAME_RATE_100_FPS),
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(7) {
            it == CameraConfigStorageState.VideoMode(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                    SettingsDefinitions.SDCardOperationState.NORMAL,
                    1024,
                    ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080,
                            SettingsDefinitions.VideoFrameRate.FRAME_RATE_100_FPS))
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_flatCameraSupported() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, 0),
                true,
                11,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, 0),
                SettingsDefinitions.FlatCameraMode.PHOTO_EHDR,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, 0),
                SettingsDefinitions.StorageLocation.SDCARD,
                14,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                16,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, 0),
                1024,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, 0),
                SettingsDefinitions.PhotoFileFormat.JPEG,
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(22, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(8) {
            it == CameraConfigStorageState.PhotoMode(SettingsDefinitions.StorageLocation.SDCARD,
                    SettingsDefinitions.SDCardOperationState.NORMAL,
                    1024,
                    SettingsDefinitions.PhotoFileFormat.JPEG)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigStorageModel_cameraStorageState_lensModeSupported() {
        // Use util method to set emitted value after given delay for given key
        setEmptyValues()
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0), widgetModel)).thenReturn(Flowable.just(true))
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.DISPLAY_NAME, 0), widgetModel)).thenReturn(Flowable.just(""))
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, 0),
                true,
                11,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, 0),
                SettingsDefinitions.FlatCameraMode.PHOTO_EHDR,
                12,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                13,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, 0),
                SettingsDefinitions.StorageLocation.SDCARD,
                14,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                16,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, 0),
                1024,
                18,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.PHOTO_FILE_FORMAT, 0,0),
                SettingsDefinitions.PhotoFileFormat.JPEG,
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigStorageState> = widgetModel.cameraConfigStorageState.test()
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(16) {
            it == CameraConfigStorageState.PhotoMode(SettingsDefinitions.StorageLocation.SDCARD,
                    SettingsDefinitions.SDCardOperationState.NORMAL,
                    1024,
                    SettingsDefinitions.PhotoFileFormat.JPEG)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.createLensKey(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.createLensKey(CameraKey.PHOTO_FILE_FORMAT, cameraIndex, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_FILE_FORMAT, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_STATE, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.STORAGE_STATE, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_STATE, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, cameraIndex))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CAMERA_COLOR, cameraIndex))
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, cameraIndex)
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, cameraIndex)
    }

}