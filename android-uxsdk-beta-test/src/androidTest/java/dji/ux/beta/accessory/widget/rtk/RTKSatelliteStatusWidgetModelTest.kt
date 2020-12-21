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

package dji.ux.beta.accessory.widget.rtk

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.flightcontroller.RTKState
import dji.common.flightcontroller.rtk.LocationStandardDeviation
import dji.common.flightcontroller.rtk.NetworkServiceChannelState
import dji.common.flightcontroller.rtk.NetworkServiceState
import dji.common.flightcontroller.rtk.ReferenceStationSource
import dji.common.product.Model
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.accessory.widget.rtk.RTKSatelliteStatusWidgetModel.RTKSignal
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.util.UnitConversionUtil
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
 * This class tests the public methods in [RTKSatelliteStatusWidgetModel]
 * 1. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_rtkConnection_isUpdated]
 * Test that the RTK connection state is updated.
 * 2. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_rtkState_isUpdated]
 * Test that the RTK state is updated.
 * 3. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_model_isUpdated]
 * Test that the model is updated.
 * 4. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_rtkSignal_isUpdated]
 * Test that the RTK signal is updated.
 * 5. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_standardDeviation_isUpdatedWithMetric]
 * Test that the standard deviation is updated when the unit type is metric.
 * 6. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_standardDeviation_isUpdatedWithImperial]
 * Test that the standard deviation is updated when the unit type is imperial.
 * 7. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_rtkBaseStationStatus_isUpdated]
 * Test that the RTK base station state is updated.
 * 8. [RTKSatelliteStatusWidgetModelTest.rtkSatelliteStatusWidgetModel_rtkNetworkServiceStatus_isUpdated]
 * Test that the RTK network service state is updated.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RTKSatelliteStatusWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: RTKSatelliteStatusWidgetModel
    private lateinit var testScheduler: TestScheduler

    private val defaultRTKState: RTKState = RTKState.Builder().build()

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(TEST_INITIAL_UNIT_TYPE_METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = RTKSatelliteStatusWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_rtkConnection_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.isRTKConnected.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_rtkState_isUpdated() {
        initEmptyValues()
        val testRTKState: RTKState = RTKState.Builder().isRTKBeingUsed(true).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                testRTKState,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(defaultRTKState)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(defaultRTKState, testRTKState)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_model_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.PHANTOM_4_RTK,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.model.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(Model.UNKNOWN_AIRCRAFT)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(Model.UNKNOWN_AIRCRAFT, Model.PHANTOM_4_RTK)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_rtkSignal_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_REFERENCE_STATION_SOURCE),
                ReferenceStationSource.NETWORK_RTK,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkSignal.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(RTKSignal.BASE_STATION)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(RTKSignal.BASE_STATION, RTKSignal.NETWORK_RTK)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_standardDeviation_isUpdatedWithMetric() {
        initEmptyValues()
        val standardDeviation = LocationStandardDeviation(1f, 2f, 3f)
        val testRTKState = RTKState.Builder().mobileStationStandardDeviation(standardDeviation).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                testRTKState,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.standardDeviation.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(RTKSatelliteStatusWidgetModel.StandardDeviation(0f, 0f, 0f, UnitConversionUtil.UnitType.METRIC))
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(RTKSatelliteStatusWidgetModel.StandardDeviation(0f, 0f, 0f, UnitConversionUtil.UnitType.METRIC),
                RTKSatelliteStatusWidgetModel.StandardDeviation(1f, 2f, 3f, UnitConversionUtil.UnitType.METRIC))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_standardDeviation_isUpdatedWithImperial() {
        initEmptyValues()
        val standardDeviation = LocationStandardDeviation(1f, 2f, 3f)
        val testRTKState = RTKState.Builder().mobileStationStandardDeviation(standardDeviation).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                testRTKState,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.standardDeviation.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(RTKSatelliteStatusWidgetModel.StandardDeviation(0f, 0f, 0f, UnitConversionUtil.UnitType.METRIC))
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == RTKSatelliteStatusWidgetModel.StandardDeviation(
                    UnitConversionUtil.convertMetersToFeet(1f),
                    UnitConversionUtil.convertMetersToFeet(2f),
                    UnitConversionUtil.convertMetersToFeet(3f),
                    UnitConversionUtil.UnitType.IMPERIAL)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_rtkBaseStationStatus_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED),
                true,
                20,
                TimeUnit.SECONDS)
        val testRTKState: RTKState = RTKState.Builder().isRTKBeingUsed(true).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                testRTKState,
                30,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkBaseStationState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED,
                RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED,
                RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE,
                RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkSatelliteStatusWidgetModel_rtkNetworkServiceStatus_isUpdated() {
        initEmptyValues()
        val testRTKState: RTKState = RTKState.Builder().isRTKBeingUsed(true).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                testRTKState,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_REFERENCE_STATION_SOURCE),
                ReferenceStationSource.NETWORK_RTK,
                20,
                TimeUnit.SECONDS)

        val defaultRTKNetworkServiceStatus = RTKSatelliteStatusWidgetModel.RTKNetworkServiceState(NetworkServiceChannelState.UNKNOWN,
                isRTKBeingUsed = false,
                isNetworkServiceOpen = false,
                rtkSignal = RTKSignal.BASE_STATION)
        val testRTKNetworkServiceStatus = RTKSatelliteStatusWidgetModel.RTKNetworkServiceState(NetworkServiceChannelState.RTCM_CONNECTED,
                isRTKBeingUsed = true,
                isNetworkServiceOpen = true,
                rtkSignal = RTKSignal.NETWORK_RTK)

        widgetModel.setup()
        val testSubscriber = widgetModel.rtkNetworkServiceState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(defaultRTKNetworkServiceStatus)

        val networkServiceState = NetworkServiceState(NetworkServiceChannelState.RTCM_CONNECTED, null)
        widgetModel.onNetworkServiceStateUpdate(networkServiceState)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == testRTKNetworkServiceStatus }
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
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_REFERENCE_STATION_SOURCE))
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitConversionUtil.UnitType.METRIC
    }
}