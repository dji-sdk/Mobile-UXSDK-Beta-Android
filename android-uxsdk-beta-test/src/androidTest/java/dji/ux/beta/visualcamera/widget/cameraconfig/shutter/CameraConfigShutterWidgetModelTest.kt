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

package dji.ux.beta.visualcamera.widget.cameraconfig.shutter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.Aperture
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
import dji.ux.beta.visualcamera.widget.cameraconfig.shutter.CameraConfigShutterWidgetModel.CameraConfigShutterState
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
 * This class tests the public methods in the [CameraConfigShutterWidgetModel]
 * 1.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_productDisconnected]
 * Test the shutter state when product connection is updated.
 * 2.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_cameraConnected]
 * Test the shutter state when camera connection is updated.
 * 3.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_shutterUnknown]
 * Test the shutter state when shutter is Unknown.
 * 4.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_shutterUpdate]
 * Test the shutter state when shutter is updated.
 * 5.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_noLensSupport]
 * Test the shutter state when no lens support.
 * 6.
 * [CameraConfigShutterWidgetModelTest.cameraConfigShutterWidgetModel_shutterSpeedState_lensSupported]
 * Test the shutter state when lens is supported.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigShutterWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore

    private lateinit var widgetModel: CameraConfigShutterWidgetModel
    private lateinit var testScheduler: TestScheduler


    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigShutterWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_productDisconnected() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected,
                CameraConfigShutterState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_cameraConnected() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected,
                CameraConfigShutterState.CameraDisconnected,
                CameraConfigShutterState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_shutterUnknown() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                10,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected,
                CameraConfigShutterState.CameraDisconnected,
                CameraConfigShutterState.NotSupported,
                CameraConfigShutterState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_shutterUpdate() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                10,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected,
                CameraConfigShutterState.CameraDisconnected,
                CameraConfigShutterState.NotSupported,
                CameraConfigShutterState.CurrentShutterValue(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_noLensSupport() {
        setEmptyValues()
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0), widgetModel)).thenReturn(Flowable.just(true))
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.DISPLAY_NAME, 0), widgetModel)).thenReturn(Flowable.just(""))
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                false,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                10,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected,
                CameraConfigShutterState.CameraDisconnected,
                CameraConfigShutterState.NotSupported,
                CameraConfigShutterState.CurrentShutterValue(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000),
                CameraConfigShutterState.CurrentShutterValue(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigShutterWidgetModel_shutterSpeedState_lensSupported() {
        setEmptyValues()
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0), widgetModel)).thenReturn(Flowable.just(true))
        Mockito.`when`(djiSdkModel.addListener(CameraKey.create(CameraKey.DISPLAY_NAME, 0), widgetModel)).thenReturn(Flowable.just(""))
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000,
                0,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_SETTINGS, 0, 0),
                exposureSettings,
                15,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the shutter flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigShutterState> = widgetModel.shutterSpeedState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigShutterState.ProductDisconnected)
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(9) { it == CameraConfigShutterState.CurrentShutterValue(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CONNECTION, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }
}