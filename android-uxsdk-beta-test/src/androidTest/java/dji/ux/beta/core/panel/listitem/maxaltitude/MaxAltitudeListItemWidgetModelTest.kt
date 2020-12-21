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

package dji.ux.beta.core.panel.listitem.maxaltitude

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
import dji.ux.beta.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidgetModel.MaxAltitudeState.*
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
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
 * This class tests the public methods in [MaxAltitudeListItemWidgetModel]
 * 1.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_isProductConnected]
 * Test product connection change
 * 2.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_noviceModeMetric]
 * Test novice mode when unit setting is metric
 * 3.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_noviceModeImperial]
 * Test novice mode when unit setting is imperial
 * 4.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_notSupportedMetric]
 * Test max flight altitude not supported in metric
 * 5.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_notSupportedImperial]
 * Test max flight altitude not supported in imperial
 * 6.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_supportedMetric]
 * Test max flight altitude  supported in metric
 * 7.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_supportedImperial]
 * Test max flight altitude  supported in imperial
 * 8.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_flightRestriction]
 * Test max flight altitude flight restriction needed
 * 9.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_maxAltitudeState_updateReturnHome]
 * Test return home altitude value update
 * 10.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_setMaxAltitudeMetric_success]
 * Test setting max flight altitude is successful
 * 11.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_setMaxAltitudeMetric_failed]
 * Test setting max flight altitude fails
 * 12.[MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_setMaxAltitudeImperial_success]
 * Test setting max flight altitude is successful in imperial
 * 13. [MaxAltitudeListItemWidgetModelTest.maxAltitudeListItemWidgetModel_setReturnHomeAltitude_success]
 * Test updating return to home altitude when it is less than max flight altitude
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaxAltitudeListItemWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: MaxAltitudeListItemWidgetModel
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
        Mockito.`when`( preferencesManager.unitType).thenReturn(UnitType.METRIC)
        widgetModel = MaxAltitudeListItemWidgetModel(djiSdkModel, keyedStore,
                 preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_isProductConnected() {
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

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_noviceModeMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == NoviceMode(UnitType.METRIC) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_noviceModeImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == NoviceMode(UnitType.IMPERIAL) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_notSupportedMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(false, 0, 0), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == MaxAltitudeValue(30, 20, 120, UnitType.METRIC, false, 0) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_notSupportedImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 400), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == MaxAltitudeValue(30, 20, 400, UnitType.METRIC, false, 0) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_supportedMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(false, 0, 0), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) { it == MaxAltitudeValue(98, 66, 394, UnitType.IMPERIAL, false, 0) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_supportedImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 200), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) { it == MaxAltitudeValue(98, 66, 656, UnitType.IMPERIAL, false, 0) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_flightRestriction() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NEED_LIMIT_FLIGHT_HEIGHT),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 400), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == MaxAltitudeValue(30, 20, 120, UnitType.METRIC, true, 0) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_maxAltitudeState_updateReturnHome() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NEED_LIMIT_FLIGHT_HEIGHT),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 400), 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                20, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.maxAltitudeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(5) { it == MaxAltitudeValue(30, 20, 120, UnitType.METRIC, true, 20) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun maxAltitudeListItemWidgetModel_setMaxAltitudeMetric_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 120), 5, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                40,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setFlightMaxAltitude(40).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }


    @Test
    fun maxAltitudeListItemWidgetModel_setMaxAltitudeMetric_failed() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 120), 5, TimeUnit.SECONDS)

        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                40,
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setFlightMaxAltitude(40).test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun maxAltitudeListItemWidgetModel_setMaxAltitudeImperial_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 120), 5, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                30,
                null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.setFlightMaxAltitude(98).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }


    @Test
    fun maxAltitudeListItemWidgetModel_setReturnHomeAltitude_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                false, 0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 120), 0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                45, 0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                40,
                null)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT),
                40,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setFlightMaxAltitude(40).test()
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
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NEED_LIMIT_FLIGHT_HEIGHT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
        WidgetTestUtil.setEmptyValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE))

    }
}