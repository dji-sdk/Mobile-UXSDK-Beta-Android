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

package dji.ux.beta.core.panel.listitem.sdcardstatus

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.sdcardstatus.SDCardStatusListItemWidgetModel.SDCardState.CurrentSDCardState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [SDCardStatusListItemWidgetModel]
 * 1.[SDCardStatusListItemWidgetModelTest.sdCardStatusWidgetModel_sdCardState_isProductConnected]
 * Test Product connection change
 * 2.[SDCardStatusListItemWidgetModelTest.sdCardStatusWidgetModel_sdCardState_remainingSpace]
 * Test remaining space state
 * 3.[SDCardStatusListItemWidgetModelTest.sdCardStatusWidgetModel_sdCardState_slow]
 * Test error state of sd card
 * 4.[SDCardStatusListItemWidgetModelTest.sdCardStatusWidgetModel_formatSDCardState_success]
 * Test if sd card format is successful
 * 5.[SDCardStatusListItemWidgetModelTest.sdCardStatusWidgetModel_formatSDCardState_fail]
 * Test if sd card format fails.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SDCardStatusListItemWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: SDCardStatusListItemWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = SDCardStatusListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun sdCardStatusWidgetModel_sdCardState_isProductConnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.sdCardState.test()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == CurrentSDCardState(SettingsDefinitions.SDCardOperationState.UNKNOWN, 0) }


        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun sdCardStatusWidgetModel_sdCardState_remainingSpace() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB, 0),
                1024,
                10,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.NORMAL,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.sdCardState.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == CurrentSDCardState(SettingsDefinitions.SDCardOperationState.NORMAL, 1024) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun sdCardStatusWidgetModel_sdCardState_slow() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB, 0),
                1024,
                11,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0),
                SettingsDefinitions.SDCardOperationState.SLOW,
                12,
                TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.sdCardState.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == CurrentSDCardState(SettingsDefinitions.SDCardOperationState.SLOW, 1024) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun sdCardStatusWidgetModel_formatSDCardState_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_SD_CARD, 0),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.formatSDCard().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun sdCardStatusWidgetModel_formatSDCardState_fail() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_SD_CARD, 0),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.formatSDCard().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    private fun initEmptyValues() {
        widgetModel.cameraIndex = 0
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.SDCARD_STATE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_SD_CARD, 0))
    }

}
