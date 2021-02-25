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

package dji.ux.beta.core.widget.simulator

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in [SimulatorIndicatorWidgetModel]
 * 1. [SimulatorIndicatorWidgetModelTest.simulatorIndicatorWidgetModel_simulatorState_isActive]
 * Check if simulator active state update is successful
 * 2. [SimulatorIndicatorWidgetModelTest.simulatorIndicatorWidgetModel_simulatorState_isInactive]
 * Check if simulator inactive state update is successful
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SimulatorIndicatorWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: SimulatorIndicatorWidgetModel
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
        widgetModel = SimulatorIndicatorWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun simulatorIndicatorWidgetModel_simulatorState_isActive() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                true, 20, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.isSimulatorActive.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, true)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun simulatorIndicatorWidgetModel_simulatorState_isInactive() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 10, TimeUnit.SECONDS)

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE),
                false, 20, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.isSimulatorActive.test()

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS)
        testSubscriber.assertValue(false)

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValues(false, false)

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }


    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }


}
