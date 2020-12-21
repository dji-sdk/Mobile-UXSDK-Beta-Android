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

package dji.ux.beta.accessory.widget.rtk

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
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
 * Test:
 * This class tests the public methods in [RTKWidgetModel]
 * 1. [RTKWidgetModelTest.rtkWidgetModel_rtkSupported_isUpdated]
 * Test that the RTK supported state is updated.
 * 2. [RTKWidgetModelTest.rtkWidgetModel_rtkEnabled_isUpdated]
 * Test that the RTK enabled state is updated.
 * 3. [RTKWidgetModelTest.rtkWidgetModel_rtkEnabled_isUpdatedByRTKConnected]
 * Test that the RTK enabled state is updated when the RTK connected state is updated.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RTKWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: RTKWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = RTKWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun rtkWidgetModel_rtkSupported_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkSupported.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkWidgetModel_rtkEnabled_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkEnabled.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkWidgetModel_rtkEnabled_isUpdatedByRTKConnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedGetValue(djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        Mockito.verify(djiSdkModel, Mockito.times(1))
                .getValue(FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        widgetModel.cleanup()
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    private fun initEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED))
    }
}