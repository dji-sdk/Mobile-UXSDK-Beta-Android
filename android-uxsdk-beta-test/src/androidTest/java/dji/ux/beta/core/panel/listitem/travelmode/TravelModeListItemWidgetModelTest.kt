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

package dji.ux.beta.core.panel.listitem.travelmode

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.error.DJIError
import dji.common.flightcontroller.LandingGearMode
import dji.keysdk.FlightControllerKey
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
import dji.ux.beta.core.panel.listitem.travelmode.TravelModeListItemWidgetModel.TravelModeState.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * This class tests the methods in [TravelModeListItemWidgetModel]
 * 1. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_travelModeState_isProductConnected]
 * Test if product is connected
 * 2. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_travelModeState_isNotSupported]
 * Test if feature not supported by product
 * 3. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_travelModeState_inactive]
 * Test if feature is supported but inactive or not in transport mode
 * 4. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_travelModeState_active]
 * Test if feature is supported and active or in transport mode
 * 5. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_enterTravelModeState_success]
 * Test if entering transport mode is successful
 * 6. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_enterTravelModeState_fail]
 * Test if entering transport mode fails
 * 7. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_exitTravelModeState_success]
 * Test if exiting transport mode is successful
 * 8. [TravelModeListItemWidgetModelTest.travelModeWidgetModel_exitTravelModeState_fail]
 * Test if exiting transport mode fails
 * */
@RunWith(AndroidJUnit4::class)
@SmallTest
class TravelModeListItemWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: TravelModeListItemWidgetModel
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
        widgetModel = TravelModeListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)


    }

    @Test
    fun travelModeWidgetModel_travelModeState_isProductConnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false, 10, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.travelModeState.test()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == ProductDisconnected }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun travelModeWidgetModel_travelModeState_isNotSupported() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.travelModeState.test()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == NotSupported }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun travelModeWidgetModel_travelModeState_inactive() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING_GEAR_MOVABLE),
                true, 12, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.travelModeState.test()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == Inactive }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun travelModeWidgetModel_travelModeState_active() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING_GEAR_MOVABLE),
                true, 12, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.LANDING_GEAR_MODE),
                LandingGearMode.TRANSPORT, 12, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.travelModeState.test()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == Inactive }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun travelModeWidgetModel_enterTravelModeState_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ENTER_TRANSPORT_MODE),
                null)
        widgetModel.setup()
        val observer: TestObserver<*> = widgetModel.enterTravelMode().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun travelModeWidgetModel_enterTravelModeState_fail() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ENTER_TRANSPORT_MODE),
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.enterTravelMode().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun travelModeWidgetModel_exitTravelModeState_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.EXIT_TRANSPORT_MODE),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.exitTravelMode().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun travelModeWidgetModel_exitTravelModeState_fail() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.EXIT_TRANSPORT_MODE),
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.exitTravelMode().test()
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
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING_GEAR_MOVABLE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.LANDING_GEAR_MODE))
    }

}