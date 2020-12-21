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

package dji.ux.beta.core.widget.airsense

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.flightcontroller.adsb.AirSenseAirplaneState
import dji.common.flightcontroller.adsb.AirSenseWarningLevel
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
import dji.ux.beta.core.communication.MessagingKeys
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.model.WarningMessage
import dji.ux.beta.core.model.WarningMessageError
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [AirSenseWidgetModel]
 * 1.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_warningLevel_isUpdated]
 * Test the initial value emitted by the AirSense warning level flowable is initialized with the
 * default value and it is updated with the given test value as expected.
 * 2.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_airSenseStatus_productConnectionUpdate]
 * Test the initial value emitted by the AirSense state flowable is initialized with the
 * default value and it is updated when the product is disconnected.
 * 3.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_airSenseStatus_airSenseNotConnected]
 * Test the initial value emitted by the AirSense state flowable is initialized with the
 * default value and it is updated with a product without AirSense is connected.
 * 4.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_airSenseStatus_noAirplanesNearby]
 * Test the initial value emitted by the AirSense state flowable is initialized with the
 * default value and it is updated when no airplanes are nearby.
 * 5.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_airSenseStatus_warningLevelUpdated]
 * Test the initial value emitted by the AirSense state flowable is initialized with the
 * default value and it is updated when the warning level is updated.
 * 6.
 * [AirSenseWidgetModelTest.airSenseWidgetModel_warningMessageSent]
 * Test that the warning messages are sent.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AirSenseWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var widgetModel: AirSenseWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = AirSenseWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun airSenseWidgetModel_warningLevel_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED))
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL),
                AirSenseWarningLevel.LEVEL_1,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the AirSense warning level flowable from the model
        val testSubscriber = widgetModel.airSenseWarningLevel.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(AirSenseWarningLevel.UNKNOWN)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValues(AirSenseWarningLevel.UNKNOWN, AirSenseWarningLevel.LEVEL_1)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun airSenseWidgetModel_airSenseStatus_productConnectionUpdate() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValues(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                mutableListOf(true, false),
                10,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the AirSense state flowable from the model
        val testSubscriber = widgetModel.airSenseState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(AirSenseWidgetModel.AirSenseState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == AirSenseWidgetModel.AirSenseState.DISCONNECTED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun airSenseWidgetModel_airSenseStatus_airSenseNotConnected() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the AirSense state flowable from the model
        val testSubscriber = widgetModel.airSenseState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(AirSenseWidgetModel.AirSenseState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == AirSenseWidgetModel.AirSenseState.NO_AIR_SENSE_CONNECTED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun airSenseWidgetModel_airSenseStatus_noAirplanesNearby() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED),
                true,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the AirSense state flowable from the model
        val testSubscriber = widgetModel.airSenseState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(AirSenseWidgetModel.AirSenseState.DISCONNECTED)
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == AirSenseWidgetModel.AirSenseState.NO_AIRPLANES_NEARBY }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun airSenseWidgetModel_airSenseStatus_warningLevelUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED),
                true,
                15,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL),
                AirSenseWarningLevel.LEVEL_1,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES),
                arrayOf(AirSenseAirplaneState.Builder().build()),
                25,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the AirSense state flowable from the model
        val testSubscriber = widgetModel.airSenseState.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(AirSenseWidgetModel.AirSenseState.DISCONNECTED)
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == AirSenseWidgetModel.AirSenseState.WARNING_LEVEL_1 }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun airSenseWidgetModel_warningMessageSent() {
        var builder = WarningMessage.Builder(WarningMessage.WarningType.FLY_SAFE)
                .code(-1)
                .subCode(WarningMessageError.OTHER_AIRCRAFT_NEARBY.value())
                .reason("reason")
                .solution("warningSolution")
                .level(WarningMessage.Level.WARNING)
                .type(WarningMessage.Type.PINNED)
                .action(WarningMessage.Action.INSERT)
        val warningMessage = builder.build()
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE), warningMessage, null)
        builder = WarningMessage.Builder(WarningMessage.WarningType.FLY_SAFE)
                .code(-1)
                .subCode(WarningMessageError.OTHER_AIRCRAFT_NEARBY.value())
                .reason("reason")
                .solution("dangerousSolution")
                .level(WarningMessage.Level.DANGEROUS)
                .type(WarningMessage.Type.PINNED)
                .action(WarningMessage.Action.REMOVE)
        val dangerousMessage = builder.build()
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE), dangerousMessage, null)
        // Use util method to set empty values to other keys
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_CONNECTED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_SYSTEM_WARNING_LEVEL))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.AIR_SENSE_AIRPLANE_STATES))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.sendWarningMessages("reason", "warningSolution", "dangerousSolution", AirSenseWarningLevel.LEVEL_2).test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }
}