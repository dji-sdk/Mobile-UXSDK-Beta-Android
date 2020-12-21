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

package dji.ux.beta.cameracore.widget.autoexposurelock;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.observers.TestObserver;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.WidgetTestUtil;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.TestSchedulerProvider;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Class will test public methods of {@link AutoExposureLockWidgetModel}
 * <p>
 * 1. {@link AutoExposureLockWidgetModelTest#autoExposureLockWidgetModel_isAutoExposureLockOn_updateWithUnlocked()}
 * Test if auto exposure lock is off
 * 2. {@link AutoExposureLockWidgetModelTest#autoExposureLockWidgetModel_isAutoExposureLockOn_updateWithLocked()}
 * Test if auto exposure lock is on
 * 3. {@link AutoExposureLockWidgetModelTest#autoExposureLockWidgetModel_setAutoExposureLock_unlockedSuccess()}
 * Test set auto exposure lock unlocked
 * 4. {@link AutoExposureLockWidgetModelTest#autoExposureLockWidgetModel_setAutoExposureLock_lockedSuccess()}
 * Test set auto exposure lock locked
 * 5. {@link AutoExposureLockWidgetModelTest#autoExposureLockWidgetModel_setAutoExposureLock_throwsError()}
 * Test set auto exposure lock completable error
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class AutoExposureLockWidgetModelTest {
    private static CompositeDisposable compositeDisposable;
    @Mock
    private DJISDKModel djiSdkModel;
    private AutoExposureLockWidgetModel widgetModel;
    private TestScheduler testScheduler;
    @Mock
    private ObservableInMemoryKeyedStore keyedStore;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        RxJavaPlugins.reset();
        compositeDisposable = new CompositeDisposable();
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        testScheduler = testSchedulerProvider.getTestScheduler();
        SchedulerProvider.setScheduler(testSchedulerProvider);
        widgetModel = new AutoExposureLockWidgetModel(djiSdkModel, keyedStore);
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true);
    }

    @Test
    public void autoExposureLockWidgetModel_isAutoExposureLockOn_updateWithUnlocked() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                false, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isAutoExposureLockOn().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, false);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void autoExposureLockWidgetModel_isAutoExposureLockOn_updateWithLocked() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true, 20, TimeUnit.SECONDS);
        widgetModel.setup();

        TestSubscriber<Boolean> testSubscriber =
                widgetModel.isAutoExposureLockOn().test();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        testSubscriber.assertValue(false);

        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        testSubscriber.assertValues(false, true);

        widgetModel.cleanup();
        compositeDisposable.add(testSubscriber);
    }

    @Test
    public void autoExposureLockWidgetModel_setAutoExposureLock_lockedSuccess() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK));

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleAutoExposureLock().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void autoExposureLockWidgetModel_setAutoExposureLock_unlockedSuccess() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true,
                10,
                TimeUnit.SECONDS);

        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                false, null);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleAutoExposureLock().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertComplete();
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @Test
    public void autoExposureLockWidgetModel_setAutoExposureLock_throwsError() {
        WidgetTestUtil.setEmittedValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                true,
                10,
                TimeUnit.SECONDS);
        UXSDKError uxsdkError = new UXSDKError(DJIError.COMMON_EXECUTION_FAILED);
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.AE_LOCK),
                false, uxsdkError);
        widgetModel.setup();

        testScheduler.advanceTimeBy(15, TimeUnit.SECONDS);
        TestObserver observer = widgetModel.toggleAutoExposureLock().test();
        testScheduler.advanceTimeBy(25, TimeUnit.SECONDS);
        observer.assertError(uxsdkError);
        widgetModel.cleanup();
        compositeDisposable.add(observer);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
