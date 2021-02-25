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

package dji.ux.beta.core.widget.remainingflighttime

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.keysdk.BatteryKey
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
import dji.ux.beta.core.widget.remainingflighttime.RemainingFlightTimeWidgetModel.RemainingFlightTimeData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [RemainingFlightTimeWidgetModel]
 * 1. [RemainingFlightTimeWidgetModelTest.remainingFlightTimeWidgetModel_aircraftFlying_isUpdated]
 * Check if aircraft is in the air/ flying
 * 2. [RemainingFlightTimeWidgetModelTest.remainingFlightTimeWidgetModel_remainingFlightTimeData_isUpdated]
 * Check if Remaining Flight Time Data update based on all the keys.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemainingFlightTimeWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: RemainingFlightTimeWidgetModel
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
        widgetModel = RemainingFlightTimeWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)


    }

    @Test
    fun remainingFlightTimeWidgetModel_aircraftFlying_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING),
                true, 20, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.isAircraftFlying.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun remainingFlightTimeWidgetModel_remainingFlightTimeData_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT),
                10, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CURRENT_LAND_IMMEDIATELY_BATTERY),
                20, 30, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME),
                30, 40, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SERIOUS_LOW_BATTERY_WARNING_THRESHOLD),
                40, 50, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.LOW_BATTERY_WARNING_THRESHOLD),
                50, 60, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REMAINING_FLIGHT_TIME),
                60, 70, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.remainingFlightTimeData.test()

        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValue(RemainingFlightTimeData(0,
                0,
                0,
                0,
                0,
                0))
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == RemainingFlightTimeData(10,
                    0,
                    0,
                    0,
                    0,
                    0)
        }
        testScheduler.advanceTimeBy(35, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == RemainingFlightTimeData(10,
                    20,
                    0,
                    0,
                    0,
                    0)
        }
        testScheduler.advanceTimeBy(45, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) {
            it == RemainingFlightTimeData(10,
                    20,
                    30,
                    0,
                    0,
                    0)
        }
        testScheduler.advanceTimeBy(55, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) {
            it == RemainingFlightTimeData(10,
                    20,
                    30,
                    40,
                    0,
                    0)
        }
        testScheduler.advanceTimeBy(65, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(6) {
            it == RemainingFlightTimeData(10,
                    20,
                    30,
                    40,
                    50,
                    0)
        }

        testScheduler.advanceTimeBy(75, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(7) {
            it == RemainingFlightTimeData(10,
                    20,
                    30,
                    40,
                    50,
                    60)
        }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    private fun initEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CURRENT_LAND_IMMEDIATELY_BATTERY))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SERIOUS_LOW_BATTERY_WARNING_THRESHOLD))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.LOW_BATTERY_WARNING_THRESHOLD))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REMAINING_FLIGHT_TIME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING))
    }

}
