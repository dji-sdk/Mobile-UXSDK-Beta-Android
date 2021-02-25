/*
 * Copyright (c) 2018-2021 DJI
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

package dji.ux.beta.accessory.widget.beacon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.error.DJIError
import dji.keysdk.AccessoryAggregationKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.accessory.widget.beacon.BeaconWidgetModel.BeaconState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [BeaconWidgetModel]
 * 1. [BeaconWidgetModelTest.beaconWidgetModel_beaconWidgetState_productDisconnected]
 * Test beacon widget state when product is disconnected
 * 2. [BeaconWidgetModelTest.beaconWidgetModel_beaconWidgetState_notSupported]
 * Test beacon widget state when product is connected but the beacon accessory is not supported
 * 3. [BeaconWidgetModelTest.beaconWidgetModel_beaconWidgetState_inactive]
 * Test beacon widget state when beacon is connected but inactive
 * 4. [BeaconWidgetModelTest.beaconWidgetModel_beaconWidgetState_active]
 * Test beacon widget state when beacon is connected and active
 * 5. [BeaconWidgetModelTest.beaconWidgetModel_toggleBeaconState_success]
 * Test if toggling the beacon state is successful
 * 6. [BeaconWidgetModelTest.beaconWidgetModel_toggleBeaconState_error]
 * Test if toggling the beacon state fails
 */

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeaconWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: BeaconWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = BeaconWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)
    }

    @Test
    fun beaconWidgetModel_beaconWidgetState_productDisconnected() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION), false, 10, TimeUnit.SECONDS)

        widgetModel.setup()
        val testSubscriber: TestSubscriber<BeaconState> = widgetModel.beaconState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(BeaconState.ProductDisconnected)
        testScheduler.advanceTimeBy(11, TimeUnit.SECONDS)
        testSubscriber.assertValues(BeaconState.ProductDisconnected, BeaconState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun beaconWidgetModel_beaconWidgetState_notSupported() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION), true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.CONNECTION),
                false, 11, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<BeaconState> = widgetModel.beaconState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(BeaconState.ProductDisconnected)
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValues(BeaconState.ProductDisconnected,
                BeaconState.NotSupported, BeaconState.NotSupported)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun beaconWidgetModel_beaconWidgetState_inactive() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION), true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED),
                false, 18, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<BeaconState> = widgetModel.beaconState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(BeaconState.ProductDisconnected)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(BeaconState.ProductDisconnected,
                BeaconState.NotSupported, BeaconState.Inactive, BeaconState.Inactive)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun beaconWidgetModel_beaconWidgetState_active() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION), true, 10, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.CONNECTION),
                true, 15, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED),
                true, 17, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber: TestSubscriber<BeaconState> = widgetModel.beaconState.test()
        testScheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        testSubscriber.assertValue(BeaconState.ProductDisconnected)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(BeaconState.ProductDisconnected,
                BeaconState.NotSupported, BeaconState.Inactive, BeaconState.Active)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun beaconWidgetModel_toggleBeaconState_success() {
        setEmptyValues()
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED),
                true, null)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleBeaconState().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun beaconWidgetModel_toggleBeaconState_error() {
        setEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED),
                true, uxsdkError)
        widgetModel.setup()
        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.toggleBeaconState().test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.CONNECTION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED))
    }

}