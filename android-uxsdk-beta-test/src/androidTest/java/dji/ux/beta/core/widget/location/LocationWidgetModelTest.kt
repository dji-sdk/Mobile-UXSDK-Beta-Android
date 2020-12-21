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

package dji.ux.beta.core.widget.location

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.widget.location.LocationWidgetModel.LocationState
import dji.ux.beta.core.widget.location.LocationWidgetModel.LocationState.CurrentLocation
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [LocationWidgetModelTest]
 * 1.
 * [LocationWidgetModelTest.locationWidgetModel_locationState_isProductConnected]
 * Test the initial value emitted by the model reflects product disconnected state
 * 2.
 * [LocationWidgetModelTest.locationWidgetModel_locationState_locationUnavailable]
 * Test if the state reflects location of aircraft is unavailable
 * 3.
 * [LocationWidgetModelTest.locationWidgetModel_locationState_locationUpdate]
 * Test if the state reflects the current location of the aircraft
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: LocationWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()

        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = LocationWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)

    }

    @Test
    fun locationWidgetModel_locationState_isProductConnected() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.locationState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(LocationState.ProductDisconnected,
                LocationState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun locationWidgetModel_locationState_locationUnavailable() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.locationState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == LocationState.LocationUnavailable }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun locationWidgetModel_locationState_locationUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE),
                37.14,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE),
                -122.14,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.locationState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == CurrentLocation(37.14, -122.14) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

}