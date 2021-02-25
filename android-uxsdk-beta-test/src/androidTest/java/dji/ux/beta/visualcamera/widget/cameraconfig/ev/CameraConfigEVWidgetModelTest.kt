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

package dji.ux.beta.visualcamera.widget.cameraconfig.ev

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.ExposureSettings
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
import dji.ux.beta.visualcamera.widget.cameraconfig.ev.CameraConfigEVWidgetModel.CameraConfigEVState
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
 * This class tests the public methods in the [CameraConfigEVWidgetModel]
 * 1.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_productConnected]
 * Test the exposure compensation state when product disconnected.
 * 2.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_cameraConnected]
 * Test the exposure compensation state when camera disconnected.
 * 3.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_eiMode]
 * Test the exposure compensation state when exposure sensitivity mode is EI.
 * 4.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_ISO]
 * Test the exposure compensation state when exposure sensitivity mode is ISO.
 * 5.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_NOT_MANUAL_AND_FIXED]
 * Test the exposure compensation state when exposure is not Manual and exposure compensation is not fixed.
 * 6.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_FIXED]
 * Test the exposure compensation state when exposure is Manual and exposure settings compensation is fixed.
 * 7.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_exposureCompensationUpdated]
 * Test the exposure compensation state when exposure is Manual and exposure settings compensation is updated.
 * 8.
 * [CameraConfigEVWidgetModelTest.cameraConfigEVWidgetModel_exposureCompensation_lensSupported]
 * Test the exposure compensation state when multi lens is supported.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigEVWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore

    private lateinit var widgetModel: CameraConfigEVWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigEVWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_productConnected() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_cameraConnected() {
        setEmptyValues()
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
                8,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_eiMode() {
        setEmptyValues()
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE),
                SettingsDefinitions.ExposureSensitivityMode.EI,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_ISO() {
        setEmptyValues()
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_NOT_MANUAL_AND_FIXED() {
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                SettingsDefinitions.ExposureMode.APERTURE_PRIORITY,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_COMPENSATION, 0),
                SettingsDefinitions.ExposureCompensation.N_0_3,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.N_0_3))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_FIXED() {
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                SettingsDefinitions.ExposureMode.MANUAL,
                10,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.F_13,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.FIXED)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.N_0_0))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_exposureCompensationUpdated() {
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_MODE, 0),
                SettingsDefinitions.ExposureMode.MANUAL,
                10,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.F_13,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.N_0_0)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_COMPENSATION, 0),
                SettingsDefinitions.ExposureCompensation.N_0_3,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigEVState.ProductDisconnected,
                CameraConfigEVState.CameraDisconnected,
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.UNKNOWN),
                CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.N_0_3))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigEVWidgetModel_exposureCompensation_lensSupported() {
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
                8,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_MODE, 0, 0),
                SettingsDefinitions.ExposureMode.MANUAL,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)
        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.F_13,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                0,
                SettingsDefinitions.ExposureCompensation.N_0_0)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_SETTINGS, 0, 0),
                exposureSettings,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_COMPENSATION, 0, 0),
                SettingsDefinitions.ExposureCompensation.N_0_3,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<CameraConfigEVState> = widgetModel.exposureCompensationState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigEVState.ProductDisconnected)
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(11) { it == CameraConfigEVState.CurrentExposureValue(SettingsDefinitions.ExposureCompensation.N_0_3) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SETTINGS))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_COMPENSATION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }
}