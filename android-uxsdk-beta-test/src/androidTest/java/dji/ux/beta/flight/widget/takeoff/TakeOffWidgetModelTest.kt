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

package dji.ux.beta.flight.widget.takeoff

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.error.DJIError
import dji.common.flightcontroller.VisionLandingProtectionState
import dji.common.product.Model
import dji.common.remotecontroller.RCMode
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

private const val TAKEOFF_HEIGHT: Float = 1.2f
private const val PRECISION_TAKEOFF_HEIGHT: Float = 6f
private const val LAND_HEIGHT: Float = 0.3f

/**
 * Test:
 * This class tests the public methods in [TakeOffWidgetModel]
 * 1. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isDisconnected]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED] when
 * the product is disconnected.
 * 2. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isReadyToTakeOff]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.READY_TO_TAKE_OFF]
 * when the product is ready to take off.
 * 3. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isAutoLanding]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.AUTO_LANDING]
 * when the product is auto landing.
 * 4. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isForcedAutoLanding]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.FORCED_AUTO_LANDING]
 * when the product is forced to auto land.
 * 5. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isUnsafeToLand]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.UNSAFE_TO_LAND]
 * when the product has determined it is unsafe to land.
 * 6. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isWaitingForLandingConfirmation]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION]
 * when the product is waiting for landing confirmation.
 * 7. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isReturningToHome]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.RETURNING_TO_HOME]
 * when the product is returning home.
 * 8. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isTakeOffDisabled]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.TAKE_OFF_DISABLED]
 * when the product cannot take off.
 * 9. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isLandDisabled]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.LAND_DISABLED]
 * when the product cannot land.
 * 10. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffLandingState_isReadyToLand]
 * Test that the TakeOffLandingState is [TakeOffWidgetModel.TakeOffLandingState.READY_TO_LAND]
 * when the product is ready to land.
 * 11. [TakeOffWidgetModelTest.takeOffWidgetModel_isPrecisionTakeoffSupported_isUpdated]
 * Test that whether precision take off is supported is updated.
 * 12. [TakeOffWidgetModelTest.takeOffWidgetModel_isInAttiMode_isUpdated]
 * Test that whether the product is in atti mode is updated.
 * 13. [TakeOffWidgetModelTest.takeOffWidgetModel_isInspire2OrMatrice200Series_isUpdated]
 * Test that whether the product is Inspire 2 or Matrice 200 Series is updated.
 * 14. [TakeOffWidgetModelTest.takeOffWidgetModel_takeOffHeight_unitTypeIsUpdated]
 * Test that the unit type of the take off height is updated.
 * 15. [TakeOffWidgetModelTest.takeOffWidgetModel_precisionTakeOffHeight_unitTypeIsUpdated]
 * Test that the unit type of the precision take off height is updated.
 * 16. [TakeOffWidgetModelTest.takeOffWidgetModel_landingHeight_unitTypeIsUpdated]
 * Test that the unit type of the landing height is updated.
 * 17. [TakeOffWidgetModelTest.takeOffWidgetModel_forceLandingHeight_unitTypeIsUpdated]
 * Test that the unit type of the force landing height is updated.
 * 18. [TakeOffWidgetModelTest.takeOffWidgetModel_performTakeOffAction_success]
 * Test a success state occurs when the take off action is performed.
 * 19. [TakeOffWidgetModelTest.takeOffWidgetModel_performTakeOffAction_error]
 * Test an error state occurs when the take off action is performed.
 * 20. [TakeOffWidgetModelTest.takeOffWidgetModel_performTakeOffAction_completeWhenMotorsAreOn]
 * Test that nothing happens when the take off action is performed when the motors are on.
 * 21. [TakeOffWidgetModelTest.takeOffWidgetModel_performPrecisionTakeOffAction_success]
 * Test a success state occurs when the precision take off action is performed.
 * 22. [TakeOffWidgetModelTest.takeOffWidgetModel_performPrecisionTakeOffAction_error]
 * Test an error state occurs when the precision take off action is performed.
 * 23. [TakeOffWidgetModelTest.takeOffWidgetModel_performPrecisionTakeOffAction_completeWhenMotorsAreOn]
 * Test that nothing happens when the precision take off action is performed when the motors are on.
 * 24. [TakeOffWidgetModelTest.takeOffWidgetModel_performLandingAction_success]
 * Test a success state occurs when the landing action is performed.
 * 25. [TakeOffWidgetModelTest.takeOffWidgetModel_performLandingAction_error]
 * Test an error state occurs when the landing action is performed.
 * 26. [TakeOffWidgetModelTest.takeOffWidgetModel_performCancelLandingAction_success]
 * Test a success state occurs when the cancel landing action is performed.
 * 27. [TakeOffWidgetModelTest.takeOffWidgetModel_performCancelLandingAction_error]
 * Test an error state occurs when the cancel landing action is performed.
 * 28. [TakeOffWidgetModelTest.takeOffWidgetModel_performLandingConfirmationAction_success]
 * Test a success state occurs when the landing confirmation action is performed.
 * 29. [TakeOffWidgetModelTest.takeOffWidgetModel_performLandingConfirmationAction_error]
 * Test an error state occurs when the landing confirmation action is performed.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class TakeOffWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: TakeOffWidgetModel
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
        widgetModel = TakeOffWidgetModel(djiSdkModel, keyedStore,  preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)

    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isDisconnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isReadyToTakeOff() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.READY_TO_TAKE_OFF }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isAutoLanding() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING),
                true,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.AUTO_LANDING }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isForcedAutoLanding() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_CANCEL_AUTO_LANDING_DISABLED),
                true,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.FORCED_AUTO_LANDING }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isUnsafeToLand() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.LANDING_PROTECTION_STATE),
                VisionLandingProtectionState.NOT_SAFE_TO_LAND,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.UNSAFE_TO_LAND }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isWaitingForLandingConfirmation() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_LANDING_CONFIRMATION_NEEDED),
                true,
                15,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isReturningToHome() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME),
                true,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.RETURNING_TO_HOME }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isTakeOffDisabled() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.MODE),
                RCMode.SLAVE,
                10,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.TAKE_OFF_DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isLandDisabled() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.MODE),
                RCMode.SLAVE,
                15,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.LAND_DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffLandingState_isReadyToLand() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                9,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                10,
                TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber = widgetModel.takeOffLandingState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(TakeOffWidgetModel.TakeOffLandingState.DISCONNECTED)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == TakeOffWidgetModel.TakeOffLandingState.READY_TO_LAND }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_isPrecisionTakeoffSupported_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedGetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_PRECISION_TAKE_OFF_SUPPORTED),
                true,
                null)
        widgetModel.setup()
        val testSubscriber = widgetModel.isPrecisionTakeoffSupported.test()
        testSubscriber.assertValue(true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_isInAttiMode_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE_STRING),
                "P-ATTI",
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.isInAttiMode.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_isInspire2OrMatrice200Series_isUpdated() {
        initEmptyValues()

        WidgetTestUtil.setEmittedValues(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                mutableListOf(Model.INSPIRE_2, Model.MATRICE_210_RTK),
                10,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.isInspire2OrMatrice200Series.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun takeOffWidgetModel_takeOffHeight_unitTypeIsUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()

        var takeOffHeight = widgetModel.takeOffHeight
        Assert.assertEquals(takeOffHeight.height, TAKEOFF_HEIGHT)
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        takeOffHeight = widgetModel.takeOffHeight
        Assert.assertEquals(takeOffHeight.height, UnitConversionUtil.convertMetersToFeet(TAKEOFF_HEIGHT))
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
    }

    @Test
    fun takeOffWidgetModel_precisionTakeOffHeight_unitTypeIsUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()

        var takeOffHeight = widgetModel.precisionTakeOffHeight
        Assert.assertEquals(takeOffHeight.height, PRECISION_TAKEOFF_HEIGHT)
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        takeOffHeight = widgetModel.precisionTakeOffHeight
        Assert.assertEquals(takeOffHeight.height, UnitConversionUtil.convertMetersToFeet(PRECISION_TAKEOFF_HEIGHT))
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
    }

    @Test
    fun takeOffWidgetModel_landingHeight_unitTypeIsUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FORCE_LANDING_HEIGHT),
                Int.MIN_VALUE,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        var takeOffHeight = widgetModel.landHeight
        Assert.assertEquals(takeOffHeight.height, LAND_HEIGHT)
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        takeOffHeight = widgetModel.landHeight
        Assert.assertEquals(takeOffHeight.height, UnitConversionUtil.convertMetersToFeet(LAND_HEIGHT))
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
    }

    @Test
    fun takeOffWidgetModel_forceLandingHeight_unitTypeIsUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FORCE_LANDING_HEIGHT),
                10,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                25,
                TimeUnit.SECONDS)
        widgetModel.setup()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        var takeOffHeight = widgetModel.landHeight
        Assert.assertEquals(takeOffHeight.height, 1f)
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        takeOffHeight = widgetModel.landHeight
        Assert.assertEquals(takeOffHeight.height, UnitConversionUtil.convertMetersToFeet(1f))
        Assert.assertEquals(takeOffHeight.unitType, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
    }

    @Test
    fun takeOffWidgetModel_performTakeOffAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKE_OFF),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performTakeOffAction().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performTakeOffAction_error() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKE_OFF),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performTakeOffAction().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performTakeOffAction_completeWhenMotorsAreOn() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.TAKE_OFF),
                uxsdkError)

        widgetModel.setup()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.performTakeOffAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performPrecisionTakeOffAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.PRECISION_TAKE_OFF),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performPrecisionTakeOffAction().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performPrecisionTakeOffAction_error() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.PRECISION_TAKE_OFF),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performPrecisionTakeOffAction().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performPrecisionTakeOffAction_completeWhenMotorsAreOn() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.PRECISION_TAKE_OFF),
                uxsdkError)

        widgetModel.setup()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.performPrecisionTakeOffAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performLandingAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_LANDING),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performLandingAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performLandingAction_error() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.START_LANDING),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performLandingAction().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performCancelLandingAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CANCEL_LANDING),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performCancelLandingAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performCancelLandingAction_error() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CANCEL_LANDING),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performCancelLandingAction().test()
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performLandingConfirmationAction_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CONFIRM_LANDING),
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performLandingConfirmationAction().test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun takeOffWidgetModel_performLandingConfirmationAction_error() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)

        WidgetTestUtil.setEmittedAction(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.CONFIRM_LANDING),
                uxsdkError)

        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.performLandingConfirmationAction().test()
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
                FlightControllerKey.create(FlightControllerKey.IS_LANDING_CONFIRMATION_NEEDED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FORCE_LANDING_HEIGHT))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE_STRING))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_GOING_HOME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_CANCEL_AUTO_LANDING_DISABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.MODE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.LANDING_PROTECTION_STATE))
        WidgetTestUtil.setEmptyValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitConversionUtil.UnitType.METRIC
    }
}