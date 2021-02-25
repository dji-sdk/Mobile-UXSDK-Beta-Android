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

package dji.ux.beta.flight.widget.apas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.error.DJIError
import dji.common.flightcontroller.FlightMode
import dji.common.flightcontroller.RemoteControllerFlightMode
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.flight.widget.apas.APASWidgetModel.APASState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit


/**
 * Test:
 * This class will test the public methods in [APASWidgetModel]
 * 1. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_productDisconnected]
 * Test state when product not connected
 * 2. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_notSupported_productNotSupported]
 * Test state when product does not support APAS
 * 3. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_notSupported_flightModeNotSupported]
 * Test state when product supports APAS but is not in the supported flight mode
 * 4. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_notSupported_rcModeNotSupported]
 * Test state when product supports APAS but is in an unsupported RC Mode
 * 5. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_disabled]
 * Test state when product supports APAS but is disabled
 * 6. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_enabledWithTemporaryError]
 * Test state when product supports APAS and is enabled but inactive due to a temporary error
 * 7. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_enabledButFlightDistanceLimitReached]
 * Test state when product supports APAS and is enabled but inactive due to reaching the flight distance limit
 * 8. [APASWidgetModelTest.apasWidgetModel_apasWidgetState_active]
 * Test state when product supports APAS, is enabled and is active
 * 9. [APASWidgetModelTest.apasWidgetModel_toggleAPAS_success]
 * Test if toggling the APAS mode is successful
 * 10. [APASWidgetModelTest.apasWidgetModel_toggleAPAS_error]
 * Test if toggling the APAS mode fails
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class APASWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: APASWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = APASWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_productDisconnected() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION), false, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(11, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_notSupported_productNotSupported() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                false)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(11, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_notSupported_flightModeNotSupported() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE),
                FlightMode.GPS_SPORT, 11, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled,
                APASState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_notSupported_rcModeNotSupported() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE),
                FlightMode.GPS_ATTI, 11, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MULTI_MODE_OPEN),
                true, 12, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CURRENT_MODE),
                RemoteControllerFlightMode.A, 13, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(14, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled,
                APASState.Disabled,
                APASState.NotSupported,
                APASState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_disabled() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                false, 11, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled, APASState.Disabled)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_enabledWithTemporaryError() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true, 11, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DOES_APAS_HAVE_TEMP_ERROR),
                true, 12, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(14, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled,
                APASState.EnabledWithTemporaryError,
                APASState.EnabledWithTemporaryError)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_enabledButFlightDistanceLimitReached() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true, 11, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IN_ON_LIMITE_AREA_BOUNDARIES),
                true, 13, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled,
                APASState.EnabledWithTemporaryError,
                APASState.EnabledButFlightDistanceLimitReached)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_apasWidgetState_active() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setKeySupported(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true, 11, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_APAS_FUNCTIONING),
                true, 14, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<APASState> = widgetModel.apasState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(APASState.ProductDisconnected)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(APASState.ProductDisconnected,
                APASState.Disabled,
                APASState.EnabledWithTemporaryError,
                APASState.Active)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun apasWidgetModel_toggleAPAS_success() {
        setEmptyValues()
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleAPAS().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun apasWidgetModel_toggleAPAS_error() {
        setEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED),
                true, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleAPAS().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ADVANCED_PILOT_ASSISTANT_SYSTEM_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_APAS_FUNCTIONING))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DOES_APAS_HAVE_TEMP_ERROR))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IN_ON_LIMITE_AREA_BOUNDARIES))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MULTI_MODE_OPEN))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CURRENT_MODE))

    }
}