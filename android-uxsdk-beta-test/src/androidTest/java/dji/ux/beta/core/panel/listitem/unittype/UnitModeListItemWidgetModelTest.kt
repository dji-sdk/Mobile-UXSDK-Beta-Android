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

package dji.ux.beta.core.panel.listitem.unittype

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.error.DJIError
import dji.keysdk.ProductKey
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
import dji.ux.beta.core.panel.listitem.unittype.UnitModeListItemWidgetModel.UnitTypeState.CurrentUnitType
import dji.ux.beta.core.panel.listitem.unittype.UnitModeListItemWidgetModel.UnitTypeState.ProductDisconnected
import dji.ux.beta.core.util.UnitConversionUtil.UnitType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Class tests public methods in [UnitModeListItemWidgetModel]
 * 1. [UnitModeListItemWidgetModelTest.unitModeListItemWidgetModel_unitTypeState_productDisconnected]
 * Test product disconnected state
 * 2. [UnitModeListItemWidgetModelTest.unitModeListItemWidgetModel_unitTypeState_metricState]
 * Test metric unit type state
 * 3. [UnitModeListItemWidgetModelTest.unitModeListItemWidgetModel_unitTypeState_imperialState]
 * Test imperial unit type state
 * 4. [UnitModeListItemWidgetModelTest.unitModeListItemWidgetModel_setUnitType_success]
 * Test set unit type success
 * 5. [UnitModeListItemWidgetModelTest.unitModeListItemWidgetModel_setUnitType_failed]
 * Test set unit type failed
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class UnitModeListItemWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: UnitModeListItemWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        val testSchedulerProvider = TestSchedulerProvider()
        // Need to call this because the UXKey classes are not added
        // Mocking the class does not call the constructor.
        ObservableInMemoryKeyedStore.getInstance()
        Mockito.`when`( preferencesManager.unitType).thenReturn(UnitType.METRIC)
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = UnitModeListItemWidgetModel(djiSdkModel, keyedStore,
                 preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun unitModeListItemWidgetModel_unitTypeState_productDisconnected() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                false, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.METRIC,
                0, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.unitTypeState.test()

        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(0) { it == ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun unitModeListItemWidgetModel_unitTypeState_metricState() {

        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.METRIC,
                7, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.unitTypeState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(ProductDisconnected, CurrentUnitType(UnitType.METRIC), CurrentUnitType(UnitType.METRIC))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun unitModeListItemWidgetModel_unitTypeState_imperialState() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL,
                7, TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriber = widgetModel.unitTypeState.test()

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        testSubscriber.assertValues(ProductDisconnected, CurrentUnitType(UnitType.METRIC), CurrentUnitType(UnitType.IMPERIAL))
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun unitModeListItemWidgetModel_setUnitType_success() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.METRIC,
                0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL, null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setUnitType(UnitType.IMPERIAL).test()
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun unitModeListItemWidgetModel_setUnitType_failed() {
        WidgetTestUtil.setEmittedValue(keyedStore,
                GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.METRIC,
                0, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitType.IMPERIAL, uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.setUnitType(UnitType.IMPERIAL).test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }


    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }
}