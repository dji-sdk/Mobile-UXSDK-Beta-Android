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

package dji.ux.beta.core.widget.distancehome

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
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.extension.toDistance
import dji.ux.beta.core.util.LocationUtil.distanceBetween
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidgetModel.DistanceHomeState
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidgetModel.DistanceHomeState.CurrentDistanceToHome
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
 * This class tests the public methods in the [DistanceHomeWidgetModel]
 * 1.
 * [DistanceHomeWidgetModelTest.distanceHomeWidgetModel_distanceHomeState_isProductConnected]
 * Test the initial value emitted by the model reflects product disconnected state
 * 2.
 * [DistanceHomeWidgetModelTest.distanceHomeWidgetModel_distanceHomeState_aircraftLocationUnavailable]
 * Test if state reflects location of aircraft is unavailable
 * 3.
 * [DistanceHomeWidgetModelTest.distanceHomeWidgetModel_distanceHomeState_homeLocationUnavailable]
 * Test if state reflects the location of home point is unavailable
 * 4.
 * [DistanceHomeWidgetModelTest.distanceHomeWidgetModel_distanceHomeState_distanceToHomeMetricUpdate]
 * Test the distance between home point and aircraft location in metric units
 * 5.
 * [DistanceHomeWidgetModelTest.distanceHomeWidgetModel_distanceHomeState_distanceToHomeImperialUpdate]
 * Test the distance between home point and aircraft location in imperial units
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class DistanceHomeWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: DistanceHomeWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(TEST_INITIAL_UNIT_TYPE_METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = DistanceHomeWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)

    }

    @Test
    fun distanceHomeWidgetModel_distanceHomeState_isProductConnected() {
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
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.distanceHomeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(DistanceHomeState.ProductDisconnected,
                DistanceHomeState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun distanceHomeWidgetModel_distanceHomeState_homeLocationUnavailable() {
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
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.distanceHomeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == DistanceHomeState.LocationUnavailable }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun distanceHomeWidgetModel_distanceHomeState_aircraftLocationUnavailable() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                37.14,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -122.14,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE))

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.distanceHomeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == DistanceHomeState.LocationUnavailable }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun distanceHomeWidgetModel_distanceHomeState_distanceToHomeMetricUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                35.14,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -121.14,
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

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.distanceHomeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) { it == CurrentDistanceToHome(distanceBetween(37.14, -122.14, 35.14, -121.14).toDistance(UnitType.METRIC), UnitType.METRIC) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun distanceHomeWidgetModel_distanceHomeState_distanceToHomeImperialUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                35.14,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                -121.14,
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

        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.distanceHomeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(6) { it == CurrentDistanceToHome(distanceBetween(37.14, -122.14, 35.14, -121.14).toDistance(UnitType.IMPERIAL), UnitType.IMPERIAL) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitType.METRIC
    }
}