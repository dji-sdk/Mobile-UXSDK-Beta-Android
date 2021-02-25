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

package dji.ux.beta.visualcamera.widget.cameraconfig.wb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.SettingsDefinitions.WhiteBalancePreset
import dji.common.camera.WhiteBalance
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
import dji.ux.beta.visualcamera.widget.cameraconfig.wb.CameraConfigWBWidgetModel.CameraConfigWBState
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
 * This class tests the public methods in the [CameraConfigWBWidgetModel]
 * 1.
 * [CameraConfigWBWidgetModelTest.cameraConfigWBWidgetModel_whiteBalanceState_productConnected]
 * Test that the white balance state is updated when product connected.
 * 2.
 * [CameraConfigWBWidgetModelTest.cameraConfigWBWidgetModel_whiteBalanceState_cameraConnected]
 * Test that the white balance state is updated when camera connected.
 * 3.
 * [CameraConfigWBWidgetModelTest.cameraConfigWBWidgetModel_whiteBalanceState_whiteBalanceUpdated]
 * Test that the white balance state is updated when white balance is updated.
 * 4.
 * [CameraConfigWBWidgetModelTest.cameraConfigWBWidgetModel_whiteBalanceState_lensSupported]
 * Test that the white balance state is updated when multi lens is supported.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigWBWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: CameraConfigWBWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigWBWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigWBWidgetModel_whiteBalanceState_productConnected() {
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
        val testSubscriber: TestSubscriber<CameraConfigWBState> = widgetModel.whiteBalanceState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigWBState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigWBState.ProductDisconnected, CameraConfigWBState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigWBWidgetModel_whiteBalanceState_cameraConnected() {
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
                CameraKey.create(CameraKey.CONNECTION),
                true,
                20,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigWBState> = widgetModel.whiteBalanceState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigWBState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigWBState.ProductDisconnected,
                CameraConfigWBState.CameraDisconnected)
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigWBState.ProductDisconnected,
                CameraConfigWBState.CameraDisconnected,
                CameraConfigWBState.CurrentWBValue(WhiteBalance(WhiteBalancePreset.UNKNOWN)))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigWBWidgetModel_whiteBalanceState_whiteBalanceUpdated() {
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
                CameraKey.create(CameraKey.CONNECTION),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.WHITE_BALANCE, 0),
                WhiteBalance(WhiteBalancePreset.CLOUDY),
                30,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigWBState> = widgetModel.whiteBalanceState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigWBState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigWBState.ProductDisconnected,
                CameraConfigWBState.CameraDisconnected)
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigWBState.ProductDisconnected,
                CameraConfigWBState.CameraDisconnected,
                CameraConfigWBState.CurrentWBValue(WhiteBalance(WhiteBalancePreset.UNKNOWN)),
                CameraConfigWBState.CurrentWBValue(WhiteBalance(WhiteBalancePreset.CLOUDY)))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigWBWidgetModel_whiteBalanceState_lensSupported() {
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
                CameraKey.create(CameraKey.CONNECTION),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.WHITE_BALANCE, 0, 0),
                WhiteBalance(WhiteBalancePreset.CLOUDY),
                14,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the iso flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigWBState> = widgetModel.whiteBalanceState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigWBState.ProductDisconnected)
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(9)
        { it == CameraConfigWBState.CurrentWBValue(WhiteBalance(WhiteBalancePreset.CLOUDY)) }
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
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.WHITE_BALANCE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.createLensKey(CameraKey.WHITE_BALANCE, 0, 0))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }

}