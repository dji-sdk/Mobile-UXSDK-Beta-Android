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

package dji.ux.beta.core.panel.listitem.rcbattery

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.remotecontroller.BatteryState
import dji.keysdk.ProductKey
import dji.keysdk.RemoteControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.rcbattery.RCBatteryListItemWidgetModel.RCBatteryState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [RCBatteryListItemWidgetModel]
 * 1.[RCBatteryListItemWidgetModelTest.rcBatteryListItemWidgetModel_rcBatteryState_isProductConnected]
 * Test Product connection change
 * 2.[RCBatteryListItemWidgetModelTest.rcBatteryListItemWidgetModel_rcBatteryState_batteryLevel]
 * Test battery level of RC
 * 3.[RCBatteryListItemWidgetModelTest.rcBatteryListItemWidgetModel_rcBatteryState_lowBattery]
 * Test if battery level of RC is low
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RCBatteryListItemWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: RCBatteryListItemWidgetModel
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
        widgetModel = RCBatteryListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)


    }

    @Test
    fun rcBatteryListItemWidgetModel_rcBatteryState_isProductConnected() {
        initEmptyValues()
        val prodConnectionData = listOf(true, false)
        WidgetTestUtil.setEmittedValues(
                widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                prodConnectionData,
                10,
                5,
                TimeUnit.SECONDS)


        widgetModel.setup()

        val testSubscriber = widgetModel.rcBatteryState.test()
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == RCBatteryState.Normal(0) }
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == RCBatteryState.RCDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun rcBatteryListItemWidgetModel_rcBatteryState_batteryLevel() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 4, TimeUnit.SECONDS)
        val chargeRemaining = BatteryState(10, 70)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BATTERY_STATE),
                chargeRemaining, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.rcBatteryState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(0) { it == RCBatteryState.RCDisconnected }
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == RCBatteryState.Normal(70) }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rcBatteryListItemWidgetModel_rcBatteryState_lowBattery() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 4, TimeUnit.SECONDS)
        val chargeRemaining = BatteryState(10, 20)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.BATTERY_STATE),
                chargeRemaining, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.IS_CHARGE_REMAINING_LOW),
                true, 5, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.rcBatteryState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(0) { it == RCBatteryState.RCDisconnected }
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) { it == RCBatteryState.Low(20) }
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
                RemoteControllerKey.create(RemoteControllerKey.BATTERY_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                RemoteControllerKey.create(RemoteControllerKey.IS_CHARGE_REMAINING_LOW))

    }
}