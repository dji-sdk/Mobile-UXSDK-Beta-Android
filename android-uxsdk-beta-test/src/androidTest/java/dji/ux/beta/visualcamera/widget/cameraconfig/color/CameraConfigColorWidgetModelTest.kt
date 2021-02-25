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

package dji.ux.beta.visualcamera.widget.cameraconfig.color

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.camera.SettingsDefinitions
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.visualcamera.widget.cameraconfig.color.CameraConfigColorWidgetModel.CameraConfigColorState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [CameraConfigColorWidgetModel]
 * 1.
 * [CameraConfigColorWidgetModelTest.cameraConfigColorWidgetModel_colorState_productConnected]
 * Test the color state when product connection is updated.
 * 2.
 * [CameraConfigColorWidgetModelTest.cameraConfigColorWidgetModel_colorState_cameraConnected]
 * Test the color state when camera connection is updated.
 * 3.
 * [CameraConfigColorWidgetModelTest.cameraConfigColorWidgetModel_colorState_notSupported]
 * Test the color state when color is Unknown or None.
 * 4.
 * [CameraConfigColorWidgetModelTest.cameraConfigColorWidgetModel_colorState_colorProfileUpdated]
 * Test the color state when color is updated.
 */

@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraConfigColorWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore

    private lateinit var widgetModel: CameraConfigColorWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = CameraConfigColorWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun cameraConfigColorWidgetModel_colorState_productConnected() {
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
        val testSubscriber: TestSubscriber<CameraConfigColorState> = widgetModel.cameraColor.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigColorState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigColorState.ProductDisconnected,
                CameraConfigColorState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigColorWidgetModel_colorState_cameraConnected() {
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
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                7,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigColorState> = widgetModel.cameraColor.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigColorState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigColorState.ProductDisconnected,
                CameraConfigColorState.CameraDisconnected,
                CameraConfigColorState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun cameraConfigColorWidgetModel_colorState_notSupported() {
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
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_COLOR, 0),
                SettingsDefinitions.CameraColor.NONE,
                9,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigColorState> = widgetModel.cameraColor.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigColorState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigColorState.ProductDisconnected,
                CameraConfigColorState.CameraDisconnected,
                CameraConfigColorState.NotSupported,
                CameraConfigColorState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun cameraConfigColorWidgetModel_colorState_colorProfileUpdated() {
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
                CameraKey.create(CameraKey.CONNECTION, 0),
                true,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_COLOR, 0),
                SettingsDefinitions.CameraColor.ART,
                9,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the aperture flowable from the model
        val testSubscriber: TestSubscriber<CameraConfigColorState> = widgetModel.cameraColor.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(CameraConfigColorState.ProductDisconnected)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(CameraConfigColorState.ProductDisconnected,
                CameraConfigColorState.CameraDisconnected,
                CameraConfigColorState.NotSupported,
                CameraConfigColorState.CameraColor(SettingsDefinitions.CameraColor.ART))
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
                CameraKey.create(CameraKey.CAMERA_COLOR, 0))
    }
}