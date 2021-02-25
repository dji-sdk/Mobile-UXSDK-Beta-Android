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

package dji.ux.beta.core.widget.verticalvelocity

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.extension.toVelocity
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
import dji.ux.beta.core.widget.verticalvelocity.VerticalVelocityWidgetModel.VerticalVelocityState
import dji.ux.beta.core.widget.verticalvelocity.VerticalVelocityWidgetModel.VerticalVelocityState.DownwardVelocity
import dji.ux.beta.core.widget.verticalvelocity.VerticalVelocityWidgetModel.VerticalVelocityState.UpwardVelocity
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
 * This class tests the public methods in the [VerticalVelocityWidgetModel]
 * 1.
 * [VerticalVelocityWidgetModelTest.verticalVelocityWidgetModel_verticalVelocityState_isProductConnected]
 * Test the initial value emitted by the model reflects product disconnected state
 * 2.
 * [VerticalVelocityWidgetModelTest.verticalVelocityWidgetModel_verticalVelocityState_downwardVelocityUpdate]
 * Test if the velocity emitted by the model reflects the Z axis velocity in downward direction
 * 3.
 * [VerticalVelocityWidgetModelTest.verticalVelocityWidgetModel_verticalVelocityState_upwardVelocityUpdate]
 * Test if the velocity emitted by the model reflects the Z axis velocity in upward direction
 * 4.
 * [VerticalVelocityWidgetModelTest.verticalVelocityWidgetModel_verticalVelocityState_downwardVelocityImperialUpdate]
 * Test if the velocity emitted by the model reflects the Z axis velocity in downward direction in imperial units
 * 5.
 * [VerticalVelocityWidgetModelTest.verticalVelocityWidgetModel_verticalVelocityState_upwardVelocityImperialUpdate]
 * Test if the velocity emitted by the model reflects the Z axis velocity in upward direction in imperial units
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class VerticalVelocityWidgetModelTest {
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: VerticalVelocityWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(TEST_INITIAL_UNIT_TYPE_METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = VerticalVelocityWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, false)

    }

    @Test
    fun verticalVelocityWidgetModel_verticalVelocityState_isProductConnected() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.VELOCITY_Z))

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.verticalVelocityState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(VerticalVelocityState.ProductDisconnected,
                VerticalVelocityState.ProductDisconnected)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun verticalVelocityWidgetModel_verticalVelocityState_downwardVelocityUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.VELOCITY_Z),
                5f,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.verticalVelocityState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == DownwardVelocity(5f.toVelocity(TEST_INITIAL_UNIT_TYPE_METRIC), TEST_INITIAL_UNIT_TYPE_METRIC)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun verticalVelocityWidgetModel_verticalVelocityState_upwardVelocityUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.VELOCITY_Z),
                -5f,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.verticalVelocityState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) {
            it == UpwardVelocity(5f.toVelocity(TEST_INITIAL_UNIT_TYPE_METRIC), TEST_INITIAL_UNIT_TYPE_METRIC)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @Test
    fun verticalVelocityWidgetModel_verticalVelocityState_downwardVelocityImperialUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.VELOCITY_Z),
                5f,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.verticalVelocityState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == DownwardVelocity(5f.toVelocity(UnitType.IMPERIAL), UnitType.IMPERIAL)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun verticalVelocityWidgetModel_verticalVelocityState_upwardVelocityImperialUpdate() {
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.VELOCITY_Z),
                -5f,
                5,
                TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                5,
                TimeUnit.SECONDS)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        // Initialize a test subscriber that subscribes to the system state flowable from the model
        val testSubscriber = widgetModel.verticalVelocityState.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(3) {
            it == UpwardVelocity((5f).toVelocity(UnitType.IMPERIAL), UnitType.IMPERIAL)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @After
    fun afterTest() {
        compositeDisposable.dispose()
    }

    companion object {
        private val TEST_INITIAL_UNIT_TYPE_METRIC = UnitType.METRIC
    }
}