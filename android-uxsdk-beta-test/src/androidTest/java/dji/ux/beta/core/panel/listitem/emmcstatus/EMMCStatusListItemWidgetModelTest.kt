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

package dji.ux.beta.core.panel.listitem.emmcstatus

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.SDCardOperationState
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
import dji.ux.beta.core.panel.listitem.emmcstatus.EMMCStatusListItemWidgetModel.EMMCState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [EMMCStatusListItemWidgetModel]
 * 1.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_eMMCState_isProductConnected]
 * Test Product connection change
 * 2.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_eMMCState_isInternalStorageSupported]
 * Test if internal storage is supported
 * 3.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_eMMCState_remainingSpaceUpdate]
 * Test eMMC remaining space
 * 4.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_eMMCState_warningStateUpdate]
 * Test eMMC state updates
 * 5.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_format_success]
 * Test if eMMC format is successful
 * 6.[EMMCStatusListItemWidgetModelTest.eMMCStatusListItemWidgetModel_format_error]
 * Test if eMMC format fails.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class EMMCStatusListItemWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: EMMCStatusListItemWidgetModel
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
        widgetModel = EMMCStatusListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun eMMCStatusListItemWidgetModel_eMMCState_isProductConnected() {
        initEmptyValues()
        val prodConnectionData = listOf(true, false)
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                prodConnectionData,
                10,
                5,
                TimeUnit.SECONDS)


        widgetModel.setup()

        val testSubscriber = widgetModel.eMMCState.test()
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == EMMCState.NotSupported }
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == EMMCState.ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun eMMCStatusListItemWidgetModel_eMMCState_isInternalStorageSupported() {
        initEmptyValues()
        val featureSupportList = listOf(true, false)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 4, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, 0),
                featureSupportList,
                10,
                5,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber = widgetModel.eMMCState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(0) { it == EMMCState.ProductDisconnected }
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == EMMCState.NotSupported }
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == EMMCState.CurrentEMMCState(SDCardOperationState.UNKNOWN, 0) }
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == EMMCState.NotSupported }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun eMMCStatusListItemWidgetModel_eMMCState_remainingSpaceUpdate() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_REMAINING_SPACE_IN_MB, 0),
                1024,
                10,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, 0),
                SDCardOperationState.NORMAL,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.eMMCState.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == EMMCState.CurrentEMMCState(SDCardOperationState.NORMAL, 1024) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun eMMCStatusListItemWidgetModel_eMMCState_warningStateUpdate() {
        initEmptyValues()
        val stateList = listOf(SDCardOperationState.NORMAL,
                SDCardOperationState.SLOW,
                SDCardOperationState.FULL)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_REMAINING_SPACE_IN_MB, 0),
                1024,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, 0),
                stateList,
                10,
                3,
                TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.eMMCState.test()

        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == EMMCState.CurrentEMMCState(SDCardOperationState.NORMAL, 1024) }
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) { it == EMMCState.CurrentEMMCState(SDCardOperationState.SLOW, 1024) }
        testScheduler.advanceTimeBy(18, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(6) { it == EMMCState.CurrentEMMCState(SDCardOperationState.FULL, 1024) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun eMMCStatusListItemWidgetModel_format_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_INTERNAL_STORAGE, 0),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.formatEMMC().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun eMMCStatusListItemWidgetModel_format_error() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_INTERNAL_STORAGE, 0),
                SettingsDefinitions.StorageLocation.INTERNAL_STORAGE,
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.formatEMMC().test()
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
                CameraKey.create(CameraKey.INNERSTORAGE_REMAINING_SPACE_IN_MB, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.INNERSTORAGE_STATE, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FORMAT_INTERNAL_STORAGE, 0))
    }

}