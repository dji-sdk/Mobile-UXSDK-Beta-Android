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

package dji.ux.beta.core.widget.altitude

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
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
import dji.ux.beta.core.widget.altitude.AltitudeWidgetModel.AltitudeState
import dji.ux.beta.core.widget.altitude.AltitudeWidgetModel.AltitudeState.CurrentAltitude
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
 * This class tests the public methods in the [AltitudeWidgetModel]
 * 1.
 * [AltitudeWidgetModelTest.altitudeWidgetModel_altitudeState_isProductConnected]
 * Test the initial value emitted by the model reflects product disconnected state
 * 2.
 * [AltitudeWidgetModelTest.altitudeWidgetModel_altitudeState_AGLMetricUpdate]
 * Test the update of above ground level(AGL) altitude in metric units
 * 3.
 * [AltitudeWidgetModelTest.altitudeWidgetModel_altitudeState_AGLImperialUpdate]
 * Test the update of above ground level(AGL) altitude in imperial units
 * 4.
 * [AltitudeWidgetModelTest.altitudeWidgetModel_altitudeState_AMSLMetricUpdate]
 * Test the update of above mean sea level(AMSL) altitude in metric units
 * 5.
 * [AltitudeWidgetModelTest.altitudeWidgetModel_altitudeState_AMSLImperialUpdate]
 * Test the update of above mean sea level(AMSL) altitude in imperial units
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AltitudeWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: AltitudeWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(TEST_INITIAL_UNIT_TYPE_METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = AltitudeWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)

    }

    @Test
    fun altitudeWidgetModel_altitudeState_isProductConnected() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.altitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(AltitudeState.ProductDisconnected,
                AltitudeState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun altitudeWidgetModel_altitudeState_AGLMetricUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE))

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.altitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == CurrentAltitude(5f.toDistance(UnitType.METRIC),
                    5f.toDistance(UnitType.METRIC), UnitType.METRIC)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun altitudeWidgetModel_altitudeState_AGLImperialUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE))
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.altitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == CurrentAltitude(5f.toDistance(UnitType.IMPERIAL),
                    5f.toDistance(UnitType.IMPERIAL), UnitType.IMPERIAL)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun altitudeWidgetModel_altitudeState_AMSLMetricUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE),
                10,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.altitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == CurrentAltitude(5f.toDistance(UnitType.METRIC),
                    15f.toDistance(UnitType.METRIC), UnitType.METRIC)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun altitudeWidgetModel_altitudeState_AMSLImperialUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKEOFF_LOCATION_ALTITUDE),
                10,
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
        val testSubscriber = widgetModel.altitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) {
            it == CurrentAltitude(5f.toDistance(UnitType.IMPERIAL),
                    15f.toDistance(UnitType.IMPERIAL), UnitType.IMPERIAL)
        }
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