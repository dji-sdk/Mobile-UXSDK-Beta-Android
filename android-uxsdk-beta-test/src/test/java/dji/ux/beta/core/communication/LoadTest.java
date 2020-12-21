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

package dji.ux.beta.core.communication;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.FakeUXKeys;
import dji.ux.beta.categories.UnitTest;
import dji.ux.beta.core.base.SchedulerProvider;

/*********************************************************************************
 * Test:
 * Load test the UX Key system by adding several observers for each key
 * and setting several values to each key to ensure that the architecture can
 * handle this.
 **********************************************************************************/
@Category(UnitTest.class)
public class LoadTest {
    private static final int MAX_OBSERVERS_PER_KEY = 20;
    private static final int MAX_SET_VALUE = 45;
    private static ObservableInMemoryKeyedStore uxKeyManager;

    @BeforeClass
    public static void setUp() {
        uxKeyManager = ObservableInMemoryKeyedStore.getInstance();
        UXKeys.addNewKeyClass(FakeUXKeys.class);
    }

    @Before
    public void beforeTest() {
        RxJavaPlugins.reset();
    }

    @Test
    public void uxKeys_loadTest_allKeysUpdateSuccessfully() {
        // Create instances of the UX key
        ArrayList<UXKey> keyList = new ArrayList<>();
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_1));
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_2));
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_3));
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_4));
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_5));
        keyList.add(UXKeys.create(FakeUXKeys.TEST_KEY_6));

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        ArrayList<TestSubscriber<BroadcastValues>> testSubscribersList = new ArrayList<>();

        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);

        // Initialize all keys with an initial value of 0
        for (UXKey key : keyList) {
            compositeDisposable.add(uxKeyManager.setValue(key, 0).subscribe());
        }

        // Assert that the starting value for each key is 0
        for (UXKey key : keyList) {
            int testValue = (int) uxKeyManager.getValue(key);
            Assert.assertEquals(0, testValue);
        }

        // Initialize and add MAX_OBSERVERS_PER_KEY number of observers for each key in the key list
        // Add an assert in the call on cancellation of the disposables to check the final value of the observer
        // which is incremented on each set value call
        for (int i = 0; i < MAX_OBSERVERS_PER_KEY; i++) {
            for (UXKey key : keyList) {
                TestSubscriber<BroadcastValues> testSubscriber =
                    uxKeyManager.addObserver(key).observeOn(SchedulerProvider.io()).test();
                testSubscriber.assertNoValues();
                testSubscribersList.add(testSubscriber);
                compositeDisposable.add(testSubscriber);
            }
        }

        //Make sure the number of observers added are as expected
        Assert.assertEquals(MAX_OBSERVERS_PER_KEY * keyList.size(), testSubscribersList.size());

        // Set MAX_SET_VALUE values for each key with a wait period
        for (int i = 0; i < MAX_SET_VALUE; i++) {
            for (UXKey key : keyList) {
                compositeDisposable.add(uxKeyManager.setValue(key, i + 1).test().assertResult());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // Test that each subscriber has the right value count
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
        for (TestSubscriber testSubscriber : testSubscribersList) {
            testSubscriber.assertValueCount(MAX_SET_VALUE);
        }

        // Dispose the composite disposable
        compositeDisposable.dispose();
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
    }
}
