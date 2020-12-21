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

package dji.ux.beta.core.widget.vision

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.flightcontroller.FlightMode
import dji.common.flightcontroller.VisionDetectionState
import dji.common.flightcontroller.VisionSensorPosition
import dji.common.flightcontroller.VisionSystemWarning
import dji.common.flightcontroller.flightassistant.ObstacleAvoidanceSensorState
import dji.common.mission.tapfly.TapFlyMode
import dji.common.product.Model
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
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [VisionWidgetModel]
 * 1. [visionWidgetModel_visionSystemState_isUpdated]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.NORMAL].
 * 2. [visionWidgetModel_visionSystemState_isClosed]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.CLOSED].
 * 3. [visionWidgetModel_visionSystemState_isDisabled]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.DISABLED].
 * 4. [visionWidgetModel_visionSystemState_isDisabledForFreeTapFlyMode]
 * Test that when the current [TapFlyMode] is [TapFlyMode.FREE], the latest result of
 * the flowable is [VisionWidgetModel.VisionSystemState.DISABLED].
 * 5. [visionWidgetModel_visionSystemState_isDisabledForAttiMode]
 * Test that when the current [FlightMode] is [FlightMode.ATTI], the latest result of
 * the flowable is [VisionWidgetModel.VisionSystemState.DISABLED].
 * 6. [visionWidgetModel_visionSystemState_isOMNIALLForMavicEnterprise]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_ALL]
 * when the current aircraft is a Mavic 2 Enterprise.
 * 7. [visionWidgetModel_visionSystemState_isOMNIALLForMavic2]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_ALL]
 * when the current aircraft is a Mavic 2.
 * 8. [visionWidgetModel_visionSystemState_isOMNIFrontBackForMavicEnterprise]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK]
 * when the current aircraft is a Mavic 2 Enterprise.
 * 9. [visionWidgetModel_visionSystemState_isOMNIFrontBackForMavic2]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK]
 * when the current aircraft is a Mavic 2.
 * 10. [visionWidgetModel_visionSystemState_isOMNIClosed]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_CLOSED].
 * 11. [visionWidgetModel_visionSystemState_isOMNIDisabled]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_DISABLED].
 * 12. [visionWidgetModel_visionSystemState_isOmniHorizontal]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_HORIZONTAL].
 * 13. [visionWidgetModel_visionSystemState_isOmniVertical]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_VERTICAL].
 * 14. [visionWidgetModel_visionSystemState_isOmniAllForM300]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_ALL]
 * when the current aircraft is a Matrice 300.
 * 15. [visionWidgetModel_visionSystemState_isOmniClosedForM300]
 * Test a scenario which results in the latest value of the flowable being
 * [VisionWidgetModel.VisionSystemState.OMNI_CLOSED]
 * when the current aircraft is a Matrice 300.
 * 16. [visionWidgetModel_userAvoidanceEnabled_isUpdated]
 * Test that the user avoidance enabled value is updated.
 * 17. [visionWidgetModel_visionSupported_isUpdated]
 * Test that the vision supported value is updated.
 * 18. [visionWidgetModel_sendWarningMessage_success]
 * Test that the warning message is sent.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class VisionWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: VisionWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = Mockito.spy(VisionWidgetModel(djiSdkModel, keyedStore))
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isUpdated() {
        setEmptyValues()
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, false)
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(VisionWidgetModel.VisionSystemState.NORMAL)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.NORMAL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isClosed() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE),
                false, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(VisionWidgetModel.VisionSystemState.NORMAL)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.CLOSED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isDisabled() {
        setEmptyValues()
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, true)
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(VisionWidgetModel.VisionSystemState.NORMAL)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isDisabledForFreeTapFlyMode() {
        setEmptyValues()
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, false)
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.TAP_FLY_MODE),
                TapFlyMode.FREE, 25, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(VisionWidgetModel.VisionSystemState.NORMAL)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isDisabledForAttiMode() {
        setEmptyValues()
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, false)
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE),
                FlightMode.ATTI, 25, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(VisionWidgetModel.VisionSystemState.NORMAL)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIALLForMavicEnterprise() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ENTERPRISE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_LEFT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_RIGHT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_ALL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIALLForMavic2() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ZOOM, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_LEFT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_RIGHT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_ALL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIFrontBackForMavicEnterprise() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ENTERPRISE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN),
                true, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIFrontBackForMavic2() {
        setEmptyValues()
        val normalNoseTailDetectionStates: MutableList<VisionDetectionState?> = mutableListOf(
                getDetectionState(VisionSensorPosition.NOSE, false),
                getDetectionState(VisionSensorPosition.TAIL, false))
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ZOOM, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValues(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalNoseTailDetectionStates, 15, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN),
                true, 25, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN),
                true, 25, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIClosed() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ENTERPRISE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE),
                false, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOMNIDisabled() {
        setEmptyValues()
        val disabledDetectionState = getDetectionState(VisionSensorPosition.NOSE, true)
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MAVIC_2_ENTERPRISE, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                disabledDetectionState, 20, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_DISABLED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOmniHorizontal() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_ENABLED),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE),
                getOmniAvoidanceState(), 16, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_HORIZONTAL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOmniVertical() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_VERTICAL_AVOIDANCE_ENABLED),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE),
                getOmniAvoidanceState(), 16, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_VERTICAL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOmniAllForM300() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_VERTICAL_AVOIDANCE_ENABLED),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_ENABLED),
                true, 16, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE),
                getOmniAvoidanceState(), 17, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_ALL }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSystemState_isOmniClosedForM300() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE),
                getOmniAvoidanceState(), 15, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.visionSystemState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(testSubscriber.valueCount() - 1) { it == VisionWidgetModel.VisionSystemState.OMNI_CLOSED }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_userAvoidanceEnabled_isUpdated() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE),
                false, 15, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.isUserAvoidanceEnabled.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(true)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_visionSupported_isUpdated() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_100, 15, TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the vision system state flowable from the model
        val testSubscriber = widgetModel.isVisionSupportedByProduct.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(true)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun visionWidgetModel_sendWarningMessage_success() {
        setEmptyValues()
        val subCode = WarningMessageError.VISION_AVOID.value()
        val action = WarningMessage.Action.INSERT
        val builder = WarningMessage.Builder(WarningMessage.WarningType.VISION)
                .code(-1)
                .subCode(subCode)
                .reason("Obstacle Avoidance Disabled")
                .type(WarningMessage.Type.AUTO_DISAPPEAR).action(action)
        val warningMessage = builder.build()
        WidgetTestUtil.setEmittedSetValue(keyedStore, UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE),
                warningMessage, null)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.sendWarningMessage("Obstacle Avoidance Disabled", false).test()
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

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.INTELLIGENT_FLIGHT_ASSISTANT_IS_USERAVOID_ENABLE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DRAW_STATUS))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.DRAW_HEADING_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.TAP_FLY_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_FRONT_RADAR_OPEN))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_BACK_RADAR_OPEN))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_LEFT_RADAR_OPEN))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_RIGHT_RADAR_OPEN))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.MODEL_NAME))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_VERTICAL_AVOIDANCE_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_AVOIDANCE_STATE))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE))
    }

    private fun getDetectionState(sensorPosition: VisionSensorPosition, isDisabled: Boolean): VisionDetectionState {
        return VisionDetectionState.createInstance(false, 0.0, VisionSystemWarning.UNKNOWN, null,
                sensorPosition, isDisabled, VisionSystemWarning.UNKNOWN.value())
    }

    private fun getOmniAvoidanceState(): ObstacleAvoidanceSensorState {
        return ObstacleAvoidanceSensorState.Builder()
                .horizontalObstacleAvoidanceEnable(true)
                .horizontalObstacleAvoidanceWorking(true)
                .verticalObstacleAvoidanceEnable(true)
                .verticalObstacleAvoidanceWorking(true)
                .build()
    }
}