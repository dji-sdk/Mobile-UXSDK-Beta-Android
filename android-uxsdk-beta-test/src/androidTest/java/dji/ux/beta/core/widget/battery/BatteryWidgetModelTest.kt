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

package dji.ux.beta.core.widget.battery

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.battery.AggregationState
import dji.common.battery.BatteryOverview
import dji.common.battery.WarningRecord
import dji.common.flightcontroller.BatteryThresholdBehavior
import dji.keysdk.BatteryKey
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.milliVoltsToVolts
import dji.ux.beta.core.widget.battery.BatteryWidgetModel.BatteryState.*
import dji.ux.beta.core.widget.battery.BatteryWidgetModel.BatteryStatus.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * This class tests the methods in [BatteryWidgetModel]
 * 1. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_isProductConnected]
 * Check if default disconnected state is returned
 * 2. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_isSingleBatteryUpdated]
 * Check if single state is returned for a single battery drone
 * 3. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_isDualBatteryUpdated]
 * Check if dual states is returned for a dual battery drone
 * 4. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_isAggregateBatteryUpdated]
 * Check if aggregate state is returned for a drone with 6 batteries(M600)
 * 5. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_productDisconnected]
 * Check if the model returns disconnected state after losing connection with the device
 * 6. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryOverheated]
 * Check if the model returns overheated error
 * 7. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryStatusUnknown]
 * Check if the model returns unknown battery state
 * 8. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryHasError]
 * Check if the model returns battery error state
 * 9. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryWarningLevel1]
 * Check if the model returns warning level 1 when battery is enough to go home
 * 10. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryWarningBelowGoHomeLevel]
 * Check if the model returns warning level 1 when the battery is not enough to go home
 * 11. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_batteryWarningLevel2]
 * Check if the model returns warning level 2 when battery is enough to land immediately
 * 12. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_voltageAverage]
 * Check the average function in model for null values
 * 13. [BatteryWidgetModelTest.batteryWidgetModel_batteryState_aggregateStateError]
 * Check the aggregate state returns error in case of a single battery facing issues
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class BatteryWidgetModelTest {
    private val defaultArray = arrayOf(0)
    private val defaultWarningRecord = WarningRecord(
            false,
            false,
            false,
            false,
            false,
            -1,
            -1)

    private val testArray = arrayOf(10, 10, 10, 10)
    private val testWarningRecordOverheating = WarningRecord(
            false,
            true,
            false,
            false,
            false,
            -1,
            -1)

    private val testWarningRecordError = WarningRecord(
            false,
            false,
            false,
            true,
            false,
            -1,
            -1)

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: BatteryWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable


    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = BatteryWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun batteryWidgetModel_batteryState_isProductConnected() {
        initEmptyValues()

        widgetModel.setup()

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValue(DisconnectedState)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_isSingleBatteryUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                defaultWarningRecord, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), NORMAL)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_isDualBatteryUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE),
                AggregationState.Builder().numberOfConnectedBatteries(2).build(),
                20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordOverheating, 22, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 1),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1),
                defaultWarningRecord, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = DualBatteryState(10, 10f.milliVoltsToVolts(), OVERHEATING, 10, 10f.milliVoltsToVolts(), NORMAL)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(8) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_isAggregateBatteryUpdated() {
        initEmptyValues()
        val batteryOverviewArray = arrayOf(BatteryOverview(0, true, 30),
                BatteryOverview(1, true, 30),
                BatteryOverview(2, true, 30),
                BatteryOverview(3, true, 30),
                BatteryOverview(4, true, 30),
                BatteryOverview(5, true, 30))
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE),
                AggregationState.Builder().numberOfConnectedBatteries(6)
                        .batteryOverviews(batteryOverviewArray)
                        .voltage(10)
                        .chargeRemainingInPercent(30).build(),
                20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 2), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 3), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 4), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 5), testArray)

        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0), testWarningRecordOverheating)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 2), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 3), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 4), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 5), defaultWarningRecord)


        widgetModel.setup()

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == AggregateBatteryState(30, 10f.milliVoltsToVolts(), OVERHEATING) }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_productDisconnected() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                defaultWarningRecord, 22, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false, 23, TimeUnit.SECONDS)

        widgetModel.setup()

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == DisconnectedState }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryOverheated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordOverheating, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), OVERHEATING)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryStatusUnknown() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                -1, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordError, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(-1, 10f.milliVoltsToVolts(), UNKNOWN)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryHasError() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordError, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), ERROR)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryWarningLevel1() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_THRESHOLD_BEHAVIOR),
                BatteryThresholdBehavior.GO_HOME, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordError, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), WARNING_LEVEL_1)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryWarningBelowGoHomeLevel() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME),
                20, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordError, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), WARNING_LEVEL_1)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_batteryWarningLevel2() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                testArray, 21, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_THRESHOLD_BEHAVIOR),
                BatteryThresholdBehavior.LAND_IMMEDIATELY, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordError, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = SingleBatteryState(10, 10f.milliVoltsToVolts(), WARNING_LEVEL_2)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(4) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_voltageAverage() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE),
                AggregationState.Builder().numberOfConnectedBatteries(2).build(),
                20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0),
                defaultArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0),
                testWarningRecordOverheating, 22, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 1),
                10, 20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1),
                testArray, 21, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1),
                defaultWarningRecord, 22, TimeUnit.SECONDS)

        widgetModel.setup()
        val testResult = DualBatteryState(10, 0f.milliVoltsToVolts(), OVERHEATING, 10, 10f.milliVoltsToVolts(), NORMAL)

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)

        testSubscriber.assertValueAt(8) { it == testResult }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun batteryWidgetModel_batteryState_aggregateStateError() {
        initEmptyValues()
        val batteryOverviewArray = arrayOf(BatteryOverview(0, true, 30),
                BatteryOverview(1, true, 30),
                BatteryOverview(2, true, 30),
                BatteryOverview(3, true, 30),
                BatteryOverview(4, true, 30),
                BatteryOverview(5, true, 30))
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 20, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE),
                AggregationState.Builder().numberOfConnectedBatteries(6)
                        .batteryOverviews(batteryOverviewArray)
                        .voltage(10)
                        .cellDamaged(true)
                        .chargeRemainingInPercent(30).build(),
                20, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 2), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 3), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 4), testArray)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.CELL_VOLTAGES, 5), testArray)

        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 2), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 3), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 4), defaultWarningRecord)
        WidgetTestUtil.setEmittedGetCachedValue(djiSdkModel, BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 5), defaultWarningRecord)


        widgetModel.setup()

        val testSubscriber = widgetModel.batteryState.test()

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == AggregateBatteryState(30, 10f.milliVoltsToVolts(), ERROR) }

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
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.CHARGE_REMAINING_IN_PERCENT, 1))

        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.CELL_VOLTAGES, 1))

        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 0))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.create(BatteryKey.LATEST_WARNING_RECORD, 1))

        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                BatteryKey.createBatteryAggregationKey(BatteryKey.AGGREGATION_STATE))

        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_THRESHOLD_BEHAVIOR))

        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.BATTERY_PERCENTAGE_NEEDED_TO_GO_HOME))
    }
}
