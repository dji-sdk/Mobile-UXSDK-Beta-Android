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

package dji.ux.beta.flight.widget.returnhome

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.error.DJIError
import dji.common.flightcontroller.flyzone.FlyZoneReturnToHomeState
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.keysdk.RemoteControllerKey
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
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.util.UnitConversionUtil
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [ReturnHomeWidgetModel]
 * 1. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isDisconnected]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED] when the
 * product is disconnected.
 * 2. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isReturnHomeDisabled]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.RETURN_HOME_DISABLED]
 * when the product cannot return home.
 * 3. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isReadyToReturnHome]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.READY_TO_RETURN_HOME]
 * when the product is ready to return home.
 * 4. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isAutoLanding]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.AUTO_LANDING] when the
 * product is auto landing.
 * 5. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isReturningToHome]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.RETURNING_TO_HOME] when
 * the product is returning to home.
 * 6. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_returnHomeState_isForcedReturningToHome]
 * Test that the ReturnHomeState is [ReturnHomeWidgetModel.ReturnHomeState.FORCED_RETURNING_TO_HOME]
 * when the product is forced to return to home.
 * 7. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_distanceToHome_unitTypeIsUpdated]
 * Test that the values and unit type of the distance to home are updated.
 * 8. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_isRTHAtCurrentAltitudeEnabled_isUpdated]
 * Test that whether RTH is enabled at current altitude is updated.
 * 9. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_flyZoneReturnToHomeState_isUpdated]
 * Test that the fly zone return to home state is updated.
 * 10. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_performReturnHomeAction_success]
 * Test a success state occurs when the return home action is performed.
 * 11. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_performReturnHomeAction_error]
 * Test an error state occurs when the return home action is performed.
 * 12. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_performCancelReturnHomeAction_success]
 * Test a success state occurs when the cancel return home action is performed.
 * 13. [ReturnHomeWidgetModelTest.returnHomeWidgetModel_performCancelReturnHomeAction_error]
 * Test an error state occurs when the cancel return home action is performed.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ReturnHomeWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: ReturnHomeWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(TEST_INITIAL_UNIT_TYPE_METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = spyk(ReturnHomeWidgetModel(djiSdkModel, keyedStore,  preferencesManager), recordPrivateCalls = true)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)

    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isDisconnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isReturnHomeDisabled() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == ReturnHomeWidgetModel.ReturnHomeState.RETURN_HOME_DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isReadyToReturnHome() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == ReturnHomeWidgetModel.ReturnHomeState.READY_TO_RETURN_HOME }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isAutoLanding() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == ReturnHomeWidgetModel.ReturnHomeState.AUTO_LANDING }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isReturningToHome() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == ReturnHomeWidgetModel.ReturnHomeState.RETURNING_TO_HOME }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_returnHomeState_isForcedReturningToHome() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_CANCEL_RETURN_TO_HOME_DISABLED),
                true,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.returnHomeState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(ReturnHomeWidgetModel.ReturnHomeState.DISCONNECTED)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == ReturnHomeWidgetModel.ReturnHomeState.FORCED_RETURNING_TO_HOME }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun returnHomeWidgetModel_distanceToHome_unitTypeIsUpdated() {
        val homeLatitude = 42.99
        val homeLongitude = -71.46
        val aircraftLocationLat = 42.98
        val aircraftLocationLong = -71.45
        val distance = 1871f
        every { widgetModel["distanceBetween"](homeLatitude, homeLongitude, aircraftLocationLat, aircraftLocationLong) } returns distance

        initEmptyValues()
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS),
                20)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ALTITUDE),
                30f)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE),
                homeLatitude)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE),
                homeLongitude)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE),
                aircraftLocationLat)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE),
                aircraftLocationLong)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                5,
                TimeUnit.SECONDS)

        widgetModel.setup()
        var distanceToHome = widgetModel.distanceToHome
        Assert.assertEquals(distanceToHome.goToHomeHeight, 20f)
        Assert.assertEquals(distanceToHome.currentHeight, 30f)
        Assert.assertEquals(distanceToHome.distanceToHome, distance)
        Assert.assertEquals(distanceToHome.unitType, UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        distanceToHome = widgetModel.distanceToHome
        Assert.assertEquals(distanceToHome.goToHomeHeight, UnitConversionUtil.convertMetersToFeet(20f))
        Assert.assertEquals(distanceToHome.currentHeight, UnitConversionUtil.convertMetersToFeet(30f))
        Assert.assertEquals(distanceToHome.distanceToHome, distance)
        Assert.assertEquals(distanceToHome.unitType, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
    }

    @Test
    fun returnHomeWidgetModel_isRTHAtCurrentAltitudeEnabled_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CONFIG_RTH_IN_CURRENT_ALTITUDE),
                false)

        widgetModel.setup()
        val isRTHAtCurrentAltitudeEnabled = widgetModel.isRTHAtCurrentAltitudeEnabled
        Assert.assertFalse(isRTHAtCurrentAltitudeEnabled)
        widgetModel.cleanup()
    }

    @Test
    fun returnHomeWidgetModel_flyZoneReturnToHomeState_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.RETURN_TO_HOME_STATE),
                FlyZoneReturnToHomeState.NEAR_NO_FLY_ZONE,
                5,
                TimeUnit.SECONDS)

        widgetModel.setup()
        var flyZoneReturnToHomeState = widgetModel.flyZoneReturnToHomeState
        Assert.assertEquals(flyZoneReturnToHomeState, FlyZoneReturnToHomeState.UNKNOWN)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        flyZoneReturnToHomeState = widgetModel.flyZoneReturnToHomeState
        Assert.assertEquals(flyZoneReturnToHomeState, FlyZoneReturnToHomeState.NEAR_NO_FLY_ZONE)
        widgetModel.cleanup()
    }

    @Test
    fun returnHomeWidgetModel_performReturnHomeAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_GO_HOME),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performReturnHomeAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun returnHomeWidgetModel_performReturnHomeAction_error() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_GO_HOME),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performReturnHomeAction().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun returnHomeWidgetModel_performCancelReturnHomeAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CANCEL_GO_HOME),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performCancelReturnHomeAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun returnHomeWidgetModel_performCancelReturnHomeAction_error() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CANCEL_GO_HOME),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performCancelReturnHomeAction().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    private fun initEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_FLYING))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_CANCEL_RETURN_TO_HOME_DISABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.MODE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.RETURN_TO_HOME_STATE))
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitConversionUtil.UnitType.METRIC
    }
}