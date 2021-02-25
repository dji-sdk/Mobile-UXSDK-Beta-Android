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

package dji.ux.beta.core.widget.systemstatus

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.keysdk.DiagnosticsKey
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.model.VoiceNotificationType
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
 * This class tests the public methods in the [SystemStatusWidgetModel]
 * 1.
 * [SystemStatusListWidgetModelTest.systemStatusListWidgetModel_systemStatus_isUpdated]
 * Test the initial value emitted by the system state flowable is as expected and it is updated
 * with the test value as expected.
 * 2.
 * [SystemStatusListWidgetModelTest.systemStatusListWidgetModel_motorsOn_isUpdated]
 * Test the initial value emitted by the is motor on flowable is as expected and it is updated
 * with the test value as expected.
 * 3.
 * [SystemStatusListWidgetModelTest.systemStatusListWidgetModel_sendVoiceNotification_success]
 * Test that the voice notification is sent.
 * 4.
 * [SystemStatusListWidgetModelTest.systemStatusListWidgetModel_warningStatusMessage_isUpdated]
 * Test the initial value emitted by the warning state message data flowable is as expected and
 * it is updated with the test value as expected.
 *
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SystemStatusListWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: SystemStatusWidgetModel
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
        widgetModel = SystemStatusWidgetModel(djiSdkModel, keyedStore,  preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)

    }

    @Test
    fun systemStatusListWidgetModel_systemStatus_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS),
                WarningStatusItem.getTestItem(),
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_IN_NFZ))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.systemStatus.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(WarningStatusItem.getDefaultItem())
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(WarningStatusItem.getDefaultItem(), WarningStatusItem.getTestItem())
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun systemStatusListWidgetModel_motorsOn_isUpdated() {
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_IN_NFZ))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the is motor on flowable from the model
        val testSubscriber = widgetModel.isMotorOn.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun systemStatusListWidgetModel_sendVoiceNotification_success() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_IN_NFZ))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        val notificationType = VoiceNotificationType.ATTI
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(MessagingKeys.SEND_VOICE_NOTIFICATION),
                notificationType, null)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.sendVoiceNotification().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun systemStatusListWidgetModel_warningStatusMessage_isUpdated() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS),
                WarningStatusItem.getTestItem(),
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.MAX_FLIGHT_HEIGHT_IN_NFZ),
                30,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                30,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the is motor on flowable from the model
        val testSubscriber = widgetModel.warningStatusMessageData.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) {
            it == SystemStatusWidgetModel.WarningStatusMessageData(
                    "",
                    0f,
                    TEST_INITIAL_UNIT_TYPE_METRIC)
        }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        val index = testSubscriber.valueCount() - 1
        testSubscriber.assertValueAt(index) {
            it.message == "Test Warning Status Logic Item"
        }
        testSubscriber.assertValueAt(index) {
            it.maxHeight == UnitConversionUtil.convertMetersToFeet(30f)
        }
        testSubscriber.assertValueAt(index) {
            it.unitType == UnitConversionUtil.UnitType.IMPERIAL
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitConversionUtil.UnitType.METRIC
    }
}