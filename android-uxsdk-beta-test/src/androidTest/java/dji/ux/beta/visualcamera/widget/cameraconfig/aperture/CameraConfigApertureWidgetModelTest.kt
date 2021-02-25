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

package dji.ux.beta.visualcamera.widget.cameraconfig.aperture

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
import dji.ux.beta.visualcamera.widget.cameraconfig.aperture.CameraConfigApertureWidgetModel.CameraConfigApertureState
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
 * This class tests the public methods in the [CameraConfigApertureWidgetModel]
 * 1.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_productDisconnected]
 * Test the aperture state when product connection is updated.
 * 2.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_cameraConnected]
 * Test the aperture state when camera connection is updated.
 * 3.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_apertureUnknown]
 * Test the aperture state when aperture is Unknown.
 * 4.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_apertureUpdate]
 * Test the aperture state when aperture is updated.
 * 5.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_noLensSupport]
 * Test the aperture state when no lens support.
 * 6.
 * [CameraConfigApertureWidgetModelTest.cameraConfigApertureWidgetModel_apertureState_lensSupported]
 * Test the aperture state when lens is supported.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigApertureWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore

    private lateinit var widgetModel: CameraConfigApertureWidgetModel
    private lateinit var testScheduler: TestScheduler


    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigApertureWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigApertureWidgetModel_apertureState_productDisconnected() {
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

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected,
                CameraConfigApertureState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigApertureWidgetModel_apertureState_cameraConnected() {
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

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected,
                CameraConfigApertureState.CameraDisconnected,
                CameraConfigApertureState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigApertureWidgetModel_apertureState_apertureUnknown() {
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

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected,
                CameraConfigApertureState.CameraDisconnected,
                CameraConfigApertureState.NotSupported,
                CameraConfigApertureState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigApertureWidgetModel_apertureState_apertureUpdate() {
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
        val exposureSettings = ExposureSettings(Aperture.F_13,
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

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected,
                CameraConfigApertureState.CameraDisconnected,
                CameraConfigApertureState.NotSupported,
                CameraConfigApertureState.CurrentApertureValue(Aperture.F_13))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigApertureWidgetModel_apertureState_noLensSupport() {
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
        val exposureSettings = ExposureSettings(Aperture.F_13,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
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
        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected,
                CameraConfigApertureState.CameraDisconnected,
                CameraConfigApertureState.NotSupported,
                CameraConfigApertureState.CurrentApertureValue(Aperture.F_13),
                CameraConfigApertureState.CurrentApertureValue(Aperture.F_13))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigApertureWidgetModel_apertureState_lensSupported() {
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
        val exposureSettings = ExposureSettings(Aperture.F_13,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
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
        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigApertureState> = widgetModel.apertureState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigApertureState.ProductDisconnected)
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(9) { it == CameraConfigApertureState.CurrentApertureValue(Aperture.F_13) }
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