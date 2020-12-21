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

package dji.ux.beta.core.panel.listitem.maxflightdistance

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.error.DJIError
import dji.common.util.DJIParamMinMaxCapability
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
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.util.UnitConversionUtil.convertFeetToMeters
import dji.ux.beta.core.util.UnitConversionUtil.convertMetersToFeet
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 1. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_isProductConnected]
 * Test if product is connected
 * 2. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_noviceModeMetric]
 * Test if novice mode is enabled in metric unit type
 * 3. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_noviceModeImperial]
 * Test if novice mode is enabled in imperial unit type
 * 4. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_disabled]
 * Test if max flight distance setting is disabled
 * 5. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_enabledMetric]
 * Test latest max flight distance state in metric unit type
 * 6. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_enabledImperial]
 * Test latest max flight distance state in imperial unit type
 * 7. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceMetric_success]
 * Test setting of max flight distance in metric unit success
 * 8. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceMetric_failed]
 * Test setting of max flight distance in metric unit fails
 * 9. [MaxAltitudeListItemWidgetModelTest#maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceImperial_success]
 * Test setting of max flight distance in imperial unit success
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaxFlightDistanceListItemWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: MaxFlightDistanceListItemWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface


    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        Mockito.`when`( preferencesManager.unitType).thenReturn(UnitConversionUtil.UnitType.METRIC)
        widgetModel = MaxFlightDistanceListItemWidgetModel(djiSdkModel, keyedStore,
                 preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_isProductConnected() {
        initEmptyValues()
        val prodConnectionData = listOf(true, false)
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                prodConnectionData,
                5,
                5,
                TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == MaxFlightDistanceState.ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_noviceModeMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == MaxFlightDistanceState.NoviceMode(UnitConversionUtil.UnitType.METRIC) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_noviceModeImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                6, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 7, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == MaxFlightDistanceState.NoviceMode(UnitConversionUtil.UnitType.IMPERIAL) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_disabled() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                false, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == MaxFlightDistanceState.Disabled }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_enabledMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE),
                DJIParamMinMaxCapability(true, 20, 8000), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                100, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == MaxFlightDistanceState.MaxFlightDistanceValue(100, 20, 8000, UnitConversionUtil.UnitType.METRIC) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_maxFlightDistanceState_enabledImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE),
                DJIParamMinMaxCapability(true, 20, 8000), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                100, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxFlightDistanceState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) {
            it == MaxFlightDistanceState.MaxFlightDistanceValue(
                    convertMetersToFeet(100f).roundToInt(),
                    convertMetersToFeet(20f).roundToInt(),
                    convertMetersToFeet(8000f).roundToInt(),
                    UnitConversionUtil.UnitType.IMPERIAL)
        }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceMetric_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE),
                DJIParamMinMaxCapability(true, 20, 8000), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                100, 5, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                110,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setMaxFlightDistance(110).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }


    @Test
    fun maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceMetric_failed() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE),
                DJIParamMinMaxCapability(true, 20, 8000), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                100, 5, TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                110,
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setMaxFlightDistance(110).test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun maxFlightDistanceListItemWidgetModel_setMaxFlightDistanceImperial_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE),
                DJIParamMinMaxCapability(true, 20, 8000), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                100, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS),
                convertFeetToMeters(500f).roundToInt(),
                null)

        widgetModel.setup()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.setMaxFlightDistance(500).test()
        testScheduler.triggerActions()
        observer.assertComplete()
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
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_RANGE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_RADIUS))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
        WidgetTestUtil.setEmptyValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE))

    }
}