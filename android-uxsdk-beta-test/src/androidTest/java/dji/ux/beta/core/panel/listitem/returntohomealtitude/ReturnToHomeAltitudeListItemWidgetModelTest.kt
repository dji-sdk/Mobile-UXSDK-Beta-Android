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

package dji.ux.beta.core.panel.listitem.returntohomealtitude

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
import dji.ux.beta.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState
import dji.ux.beta.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.ProductDisconnected
import dji.ux.beta.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.ReturnToHomeAltitudeValue
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
 * This class tests the public methods in [ReturnToHomeAltitudeListItemWidgetModel]
 * 1.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_rthAltitudeState_isProductConnected]
 * Test product connection change
 * 2.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_rthAltitudeState_noviceMode]
 * Test product is in novice mode
 * 3.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_rthAltitudeState_returnHomeAltitudeValueMetric]
 * Test return to home altitude metric
 * 4.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_rthAltitudeState_returnHomeAltitudeValueImperial]
 * Test return to home altitude imperial
 * 5.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_setRTHAltitudeMetric_success]
 * Test setting return to home altitude is successful in metric
 * 6.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_setRTHAltitudeMetric_failed]
 * Test setting return to home altitude fails
 * 7.[ReturnToHomeAltitudeListItemWidgetModelTest.rthAltitudeListItemWidgetModel_setRTHAltitudeImperial_success]
 * Test setting return to home altitude is successful in imperial
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ReturnToHomeAltitudeListItemWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: ReturnToHomeAltitudeListItemWidgetModel
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
        Mockito.`when`(preferencesManager.unitType).thenReturn(UnitType.METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = ReturnToHomeAltitudeListItemWidgetModel(djiSdkModel, keyedStore,
                 preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun rthAltitudeListItemWidgetModel_rthAltitudeState_isProductConnected() {
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

        val testSubscriber = widgetModel.returnToHomeAltitudeState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun rthAltitudeListItemWidgetModel_rthAltitudeState_noviceMode() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.returnToHomeAltitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == ReturnToHomeAltitudeState.NoviceMode(UnitType.METRIC)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun rthAltitudeListItemWidgetModel_rthAltitudeState_returnHomeAltitudeValueMetric() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                50, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 200), 5, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.returnToHomeAltitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == ReturnToHomeAltitudeValue(50,
                    20,
                    200,
                    UnitType.METRIC,
                    0)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun rthAltitudeListItemWidgetModel_rthAltitudeState_returnHomeAltitudeValueImperial() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                30, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE),
                DJIParamMinMaxCapability(true, 20, 500), 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.returnToHomeAltitudeState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) {
            it == ReturnToHomeAltitudeValue(98,
                    66,
                    1640,
                    UnitType.IMPERIAL,
                    0)
        }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun rthAltitudeListItemWidgetModel_setRTHAltitudeMetric_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                40,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setReturnToHomeAltitude(40).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }


    @Test
    fun rthAltitudeListItemWidgetModel_setRTHAltitudeMetric_failed() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)

        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                40,
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setReturnToHomeAltitude(40).test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun rthAltitudeListItemWidgetModel_setRTHAltitudeImperial_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                29,
                null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)

        val observer: TestObserver<*> = widgetModel.setReturnToHomeAltitude(98).test()
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
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT))
        WidgetTestUtil.setEmptyValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_RANGE))


    }
}