package dji.ux.beta.core.panel.listitem.novicemode

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import dji.common.error.DJIError
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
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.novicemode.NoviceModeListItemWidgetModel.NoviceModeState.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * This class tests the public methods in [NoviceModeListItemWidgetModel]
 * 1.[NoviceModeListItemWidgetModelTest.noviceModeListItemWidgetModel_noviceModeState_isProductConnected]
 * Test product connection change
 * 2.[NoviceModeListItemWidgetModelTest.noviceModeListItemWidgetModel_noviceModeState_enabled]
 * Test novice mode enabled state
 * 3.[NoviceModeListItemWidgetModelTest.noviceModeListItemWidgetModel_setNoviceMode_success]
 * Test set novice mode success
 * 4.[NoviceModeListItemWidgetModelTest.noviceModeListItemWidgetModel_setNoviceMode_error]
 * Test set novice mode fails
 */

@RunWith(AndroidJUnit4::class)
@SmallTest
class NoviceModeListItemWidgetModelTest {

    @Mock
    private lateinit var djiSdkModel: DJISDKModel
    private lateinit var widgetModel: NoviceModeListItemWidgetModel
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
        widgetModel = NoviceModeListItemWidgetModel(djiSdkModel, keyedStore)
        WidgetTestUtil.initialize(djiSdkModel)

    }

    @Test
    fun noviceModeListItemWidgetModel_noviceModeState_isProductConnected() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
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

        val testSubscriber = widgetModel.noviceModeState.test()
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(1) { it == Disabled }
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == ProductDisconnected }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }

    @Test
    fun noviceModeListItemWidgetModel_noviceModeState_enabled() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true, 6, TimeUnit.SECONDS)
        widgetModel.setup()

        val testSubscriber = widgetModel.noviceModeState.test()
        testScheduler.advanceTimeBy(12, TimeUnit.SECONDS)
        testSubscriber.assertValueAt(2) { it == Enabled }

        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)

    }


    @Test
    fun noviceModeListItemWidgetModel_setNoviceMode_success() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true,
                null)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.toggleNoviceMode().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testScheduler.triggerActions()
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)

    }

    @Test
    fun noviceModeListItemWidgetModel_setNoviceMode_error() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                true, 5, TimeUnit.SECONDS)
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED))
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.NOVICE_MODE_ENABLED),
                true,
                uxsdkError)
        widgetModel.setup()

        val observer: TestObserver<*> = widgetModel.toggleNoviceMode().test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
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