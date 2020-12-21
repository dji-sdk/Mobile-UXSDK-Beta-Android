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

package dji.ux.beta.accessory.widget.rtk

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.error.DJIError
import dji.keysdk.FlightControllerKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
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
 * This class tests the public methods in [RTKEnabledWidgetModel]
 * 1. [RTKEnabledWidgetModelTest.rtkEnabledWidgetModel_enabled_isUpdated]
 * Test that the RTK enabled state is updated.
 * 2. [RTKEnabledWidgetModelTest.rtkEnabledWidgetModel_canEnableRTK_whenMotorsOn]
 * Test that whether the RTK can be enabled is updated when the motors are on.
 * 3. [RTKEnabledWidgetModelTest.rtkEnabledWidgetModel_canEnableRTK_whenTakeOffHeightSet]
 * Test that whether the RTK can be enabled is updated when the RTK takeoff height is set and the
 * home point data source type is RTK.
 * 4. [RTKEnabledWidgetModelTest.rtkEnabledWidgetModel_enableRTK_success]
 * Test that the RTK can be enabled successfully.
 * 5. [RTKEnabledWidgetModelTest.rtkEnabledWidgetModel_enableRTK_failed]
 * Test than an error is thrown when RTK cannot be enabled.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RTKEnabledWidgetModelTest {

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var widgetModel: RTKEnabledWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = RTKEnabledWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun rtkEnabledWidgetModel_enabled_isUpdated() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.rtkEnabled.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkEnabledWidgetModel_canEnableRTK_whenMotorsOn() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.canEnableRTK.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(true)
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkEnabledWidgetModel_canEnableRTK_whenTakeOffHeightSet() {
        initEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HAS_SET_TAKE_OFF_ALTITUDE),
                true,
                20,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HOME_LOCATION_DATA_SOURCE),
                HomePointDataSourceType.RTK.value,
                30,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.canEnableRTK.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriber.assertValue(true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false, false)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValues(true, false, false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun rtkEnabledWidgetModel_enableRTK_success() {
        initEmptyValues()
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true, null)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.setRTKEnabled(true).test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun rtkEnabledWidgetModel_enableRTK_failed() {
        initEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED),
                true, uxsdkError)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel.setRTKEnabled(true).test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    private fun initEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_ENABLED))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HOME_LOCATION_DATA_SOURCE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createRTKKey(FlightControllerKey.RTK_FUSION_HAS_SET_TAKE_OFF_ALTITUDE))
    }
}