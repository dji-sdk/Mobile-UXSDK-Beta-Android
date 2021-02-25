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

package dji.ux.beta.cameracore.widget.autoexposurelock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.error.DJIError
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.cameracore.widget.autoexposurelock.AutoExposureLockWidgetModel.AutoExposureLockState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Class will test public methods of [AutoExposureLockWidgetModel]
 *
 *
 * 1. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_autoExposureLockState_productDisconnected]
 * Test auto exposure lock state when product disconnected
 * 2. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_autoExposureLockState_cameraDisconnected]
 * Test auto exposure lock state when camera disconnected
 * 3. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_autoExposureLockState_unlocked]
 * Test auto exposure lock state is unlocked
 * 4. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_autoExposureLockState_locked]
 * Test auto exposure lock state is locked
 * 5. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_autoExposureLockState_lensSupport]
 * Test auto exposure lock state when multi lens support is available
 * 6. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_setAutoExposureLock_lockedSuccess]
 * Test set auto exposure lock completable success
 * 7. [AutoExposureLockWidgetModelTest.autoExposureLockWidgetModel_setAutoExposureLock_lockedFailure]
 * Test set auto exposure lock completable error
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AutoExposureLockWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: AutoExposureLockWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = AutoExposureLockWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun autoExposureLockWidgetModel_autoExposureLockState_productDisconnected() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<AutoExposureLockState> = widgetModel.autoExposureLockState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(AutoExposureLockState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(AutoExposureLockState.ProductDisconnected,
                AutoExposureLockState.CameraDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun autoExposureLockWidgetModel_autoExposureLockState_cameraDisconnected() {
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
        widgetModel.setup()
        val testSubscriber: TestSubscriber<AutoExposureLockState> = widgetModel.autoExposureLockState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(AutoExposureLockState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(AutoExposureLockState.ProductDisconnected,
                AutoExposureLockState.CameraDisconnected,
                AutoExposureLockState.Unlocked)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun autoExposureLockWidgetModel_autoExposureLockState_unlocked() {
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
                CameraKey.create(CameraKey.AE_LOCK, 0),
                false,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<AutoExposureLockState> = widgetModel.autoExposureLockState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(AutoExposureLockState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(AutoExposureLockState.ProductDisconnected,
                AutoExposureLockState.CameraDisconnected,
                AutoExposureLockState.Unlocked,
                AutoExposureLockState.Unlocked)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun autoExposureLockWidgetModel_autoExposureLockState_locked() {
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
                CameraKey.create(CameraKey.AE_LOCK, 0),
                true,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<AutoExposureLockState> = widgetModel.autoExposureLockState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(AutoExposureLockState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(AutoExposureLockState.ProductDisconnected,
                AutoExposureLockState.CameraDisconnected,
                AutoExposureLockState.Unlocked,
                AutoExposureLockState.Locked)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun autoExposureLockWidgetModel_autoExposureLockState_lensSupport() {
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
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, 0),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.AE_LOCK, 0, 0),
                true,
                15,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber: TestSubscriber<AutoExposureLockState> = widgetModel.autoExposureLockState.test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(9) { it == AutoExposureLockState.Locked }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun autoExposureLockWidgetModel_setAutoExposureLock_lockedSuccess() {
        setEmptyValues()
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleAutoExposureLock().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun autoExposureLockWidgetModel_setAutoExposureLock_lockedFailure() {
        setEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleAutoExposureLock().test()
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
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
                CameraKey.create(CameraKey.AE_LOCK, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                CameraKey.createLensKey(CameraKey.AE_LOCK, 0, 0))
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }

}