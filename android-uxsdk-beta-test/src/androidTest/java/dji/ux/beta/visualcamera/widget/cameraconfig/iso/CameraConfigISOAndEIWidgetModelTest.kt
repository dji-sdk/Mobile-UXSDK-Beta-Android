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

package dji.ux.beta.visualcamera.widget.cameraconfig.iso

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
import dji.ux.beta.visualcamera.widget.cameraconfig.iso.CameraConfigISOAndEIWidgetModel.CameraConfigISOEIState
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
 * This class tests the public methods in the [CameraConfigISOAndEIWidgetModel]
 * 1.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_productConnected]
 * Test that the ISO EI state is updated on product connected.
 * 2.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_cameraConnected]
 * Test that the ISO EI state is updated on camera connected.
 * 3.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_EIMode]
 * Test that the ISO EI state is updated if exposure sensitivity mode is EI.
 * 4.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_FixedISO]
 * Test that the ISO EI state is updated if exposure sensitivity mode is ISO and ISO is FIXED.
 * 5.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_AutoISO]
 * Test that the ISO EI state is updated if exposure sensitivity mode is ISO and ISO is Auto.
 * 6.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_ISO]
 * Test that the ISO EI state is updated if exposure sensitivity mode is ISO.
 * 7.
 * [CameraConfigISOAndEIWidgetModelTest.cameraConfigISOAndEIWidgetModel_isoEIState_lensSupported]
 * Test that the ISO EI state is updated when multi lens camera is supported.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigISOAndEIWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore

    private lateinit var widgetModel: CameraConfigISOAndEIWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigISOAndEIWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_productConnected() {
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
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_cameraConnected() {
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
                7,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected,
                CameraConfigISOEIState.ISO("0"))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_EIMode() {
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
                7,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.EI,
                9,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EI_VALUE, 0),
                10,
                11,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected,
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.EI("0"),
                CameraConfigISOEIState.EI("10"))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_FixedISO() {
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
                7,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                9,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ISO, 0),
                SettingsDefinitions.ISO.FIXED,
                12,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected,
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.FIXED("500"))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_AutoISO() {
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
                7,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                9,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ISO, 0),
                SettingsDefinitions.ISO.AUTO,
                12,
                TimeUnit.SECONDS)

        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                100,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                14,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected,
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.AUTO("0"),
                CameraConfigISOEIState.AUTO("100"))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_ISO() {
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
                7,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                9,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ISO, 0),
                SettingsDefinitions.ISO.ISO_1600,
                12,
                TimeUnit.SECONDS)

        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                1600,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0),
                exposureSettings,
                14,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigISOEIState.ProductDisconnected,
                CameraConfigISOEIState.CameraDisconnected,
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.ISO("0"),
                CameraConfigISOEIState.ISO("1600"))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigISOAndEIWidgetModel_isoEIState_lensSupported() {
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
                7,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0, 0),
                SettingsDefinitions.ExposureSensitivityMode.ISO,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.ISO, 0, 0),
                SettingsDefinitions.ISO.ISO_1600,
                12,
                TimeUnit.SECONDS)

        val exposureSettings = ExposureSettings(SettingsDefinitions.Aperture.UNKNOWN,
                SettingsDefinitions.ShutterSpeed.UNKNOWN,
                1600,
                SettingsDefinitions.ExposureCompensation.UNKNOWN)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.EXPOSURE_SETTINGS, 0, 0),
                exposureSettings,
                14,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigISOEIState> = widgetModel.isoEIState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigISOEIState.ProductDisconnected)
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(10) { it == CameraConfigISOEIState.ISO("1600") }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.CONNECTION, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SETTINGS, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.ISO, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EXPOSURE_SENSITIVITY_MODE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.EI_VALUE, 0))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }
}