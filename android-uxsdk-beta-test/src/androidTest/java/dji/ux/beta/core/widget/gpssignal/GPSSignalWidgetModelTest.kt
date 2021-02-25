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

package dji.ux.beta.core.widget.gpssignal

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.flightcontroller.*
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
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
 * This class tests the public methods in the [GPSSignalWidgetModel]
 * 1. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_gpsSignalQuality_isUpdated]
 * Test that the GPS signal quality is updated.
 *
 * 2. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_satelliteNumber_isUpdated]
 * Test that the satellite number is updated when the satellite count is updated.
 *
 * 3. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_satelliteNumber_isUpdatedByRTK]
 * Test that the satellite number is updated when the RTK satellite count is updated.
 *
 * 4. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_rtkEnabled_isUpdated]
 * Test that the RTK enabled state is updated.
 *
 * 5. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_rtkEnabled_isRTKSupported]
 * Test that the RTK enabled state is updated when the RTK supported state is updated.
 *
 * 6. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_externalGPSUsed_isUpdated]
 * Test that whether the external GPS is used is updated.
 *
 * 7. [GPSSignalWidgetModelTest.gpsSignalWidgetModel_rtkAccurate_isUpdated]
 * Test that whether the RTK is accurate is updated.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class GPSSignalWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: GPSSignalWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = GPSSignalWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)

    }

    @Test
    fun gpsSignalWidgetModel_gpsSignalQuality_isUpdated() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL),
                GPS_SIGNAL_LEVEL_TEST_VALUE,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.gpsSignalQuality.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(GPSSignalLevel.NONE)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(GPSSignalLevel.NONE, GPS_SIGNAL_LEVEL_TEST_VALUE)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun gpsSignalWidgetModel_satelliteNumber_isUpdated() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT),
                SATELLITE_COUNT_TEST_VALUE,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.satelliteNumber.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(0)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(0, SATELLITE_COUNT_TEST_VALUE)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun gpsSignalWidgetModel_satelliteNumber_isUpdatedByRTK() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                25,
                TimeUnit.SECONDS)
        val receiverInfo = ReceiverInfo.createInstance(true, 3)
        val rtkState = RTKState.Builder()
                .msReceiver1GPSInfo(receiverInfo)
                .msReceiver1BeiDouInfo(receiverInfo)
                .msReceiver1GLONASSInfo(receiverInfo)
                .msReceiver1GalileoInfo(receiverInfo)
                .build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                rtkState,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.satelliteNumber.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(0)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(0, 0, SATELLITE_COUNT_TEST_VALUE)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun gpsSignalWidgetModel_rtkEnabled_isUpdated() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkEnabled.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun gpsSignalWidgetModel_rtkEnabled_isRTKSupported() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        WidgetTestUtil.setEmittedGetValue(djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        Mockito.verify(djiSdkModel, Mockito.times(1))
                .getValue(FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        widgetModel.cleanup()
    }

    @Test
    fun gpsSignalWidgetModel_externalGPSUsed_isUpdated() {
        val state = RedundancySensorUsedState.Builder()
                .setGpsIndex(2).build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE),
                state,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.isExternalGPSUsed.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun gpsSignalWidgetModel_rtkAccurate_isUpdated() {
        val rtkState = RTKState.Builder()
                .positioningSolution(PositioningSolution.FIXED_POINT)
                .build()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE),
                rtkState,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GPS_SIGNAL_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_RTK_SUPPORTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE))
        widgetModel.setup()
        val testSubscriber = widgetModel.isRTKAccurate.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    companion object {
        private val GPS_SIGNAL_LEVEL_TEST_VALUE = GPSSignalLevel.LEVEL_3
        private const val SATELLITE_COUNT_TEST_VALUE = 12
    }
}