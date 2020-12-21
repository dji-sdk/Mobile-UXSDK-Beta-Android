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

package dji.ux.beta.core.widget.flightmode

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.widget.flightmode.FlightModeWidgetModel.FlightModeState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [FlightModeWidgetModel]
 * 1.[FlightModeWidgetModelTest.flightModeWidgetModel_productConnectionState_isUpdated]
 * Test if product is connected
 * 2.[FlightModeWidgetModelTest.flightModeWidgetModel_flightModeString_update]
 * Test the initial value emitted by the flight mode string flowable is initialized with the
 * default value and it is updated with the given test value as expected.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FlightModeWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: FlightModeWidgetModel
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
        widgetModel = FlightModeWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)


    }

    @Test
    fun flightModeWidgetModel_productConnectionState_isUpdated() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE_STRING))
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

        val testSubscriber = widgetModel.flightModeState.test()
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == FlightModeState.FlightModeUpdated("") }
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == FlightModeState.ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun flightModeWidgetModel_flightModeString_update() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 4, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE_STRING),
                "GPS", 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.flightModeState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(0) { it == FlightModeState.ProductDisconnected }
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == FlightModeState.FlightModeUpdated("GPS") }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

}