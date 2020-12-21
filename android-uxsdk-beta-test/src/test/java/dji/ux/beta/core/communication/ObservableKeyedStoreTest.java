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

import java.util.concurrent.TimeUnit;

import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.FakeUXKeys;
import dji.ux.beta.categories.UnitTest;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.base.UXSDKErrorDescription;

/**********************************************************************************************
 * Tests:
 * These tests are for the functionality of all the methods in the
 * ObservableKeyedStore interface's ObservableInMemoryKeyedStore implementation
 * 1) Testing set value for a UX key executes successfully and works as expected
 * 2) Testing failure to set value for a UX key because of value mismatch
 * 3) Testing get value for a UX key executes successfully and works as expected
 * 4) Testing addition of observers for a UX key with a subscription works as expected.
 * 5) Testing removal of all observers for a given UX key works as expected.
 * 6) Testing removal of all observers for all UX keys works as expected.
 * 7) Testing addition of observers for a UX Key with ON_EVENT update type works as expected.
 **********************************************************************************************/
@Category(UnitTest.class)
public class ObservableKeyedStoreTest {
    private static FlatStore store;
    private static ObservableInMemoryKeyedStore uxKeyManager;
    private static CompositeDisposable compositeDisposable;

    @BeforeClass
    public static void setUp() {
        store = FlatStore.getInstance();
        uxKeyManager = ObservableInMemoryKeyedStore.getInstance();
        UXKeys.addNewKeyClass(FakeUXKeys.class);
    }

    @Before
    public void beforeTest() {
        compositeDisposable = new CompositeDisposable();
        RxJavaPlugins.reset();
    }

    @Test
    public void uxKeys_setValue_updatesSuccessfully() {
        UXKey testKey7 = UXKeys.create(FakeUXKeys.TEST_KEY_7);
        Character[] testValueCharArray = new Character[] { 'a', 'b', 'c' };
        compositeDisposable.add(uxKeyManager.setValue(testKey7, testValueCharArray)
                                            .doOnComplete(() -> Assert.assertEquals(store.getModelValue(testKey7.getKeyPath())
                                                                                         .getData(),
                                                                                    testValueCharArray))
                                            .test()
                                            .assertResult());
    }

    @Test
    public void uxKeys_setValue_MismatchFailure() {
        UXKey testKey7 = UXKeys.create(FakeUXKeys.TEST_KEY_7);
        char[] wrongTestValue = new char[] { 'a', 'b', 'c' };
        Assert.assertNotEquals(testKey7.getValueType(), wrongTestValue.getClass());
        compositeDisposable.add(uxKeyManager.setValue(testKey7, wrongTestValue)
                                            .doOnError(throwable -> Assert.assertEquals(throwable.getMessage(),
                                                                                        new UXSDKError(UXSDKErrorDescription.VALUE_TYPE_MISMATCH)
                                                                                            .getMessage()))
                                            .test()
                                            .assertErrorMessage(new UXSDKError(UXSDKErrorDescription.VALUE_TYPE_MISMATCH).getMessage()));
    }

    @Test
    public void uxKeys_getValue_returnsSuccessfully() {
        UXKey testKey2 = UXKeys.create(FakeUXKeys.TEST_KEY_2);
        ModelValue testValueKey2 = new ModelValue(22);
        store.setModelValue(testValueKey2, testKey2.getKeyPath());
        Assert.assertEquals(uxKeyManager.getValue(testKey2), testValueKey2.getData());
    }

    @Test
    public void uxKeys_addObserverForOnChangeUpdateType_updatesSuccessfully() {
        UXKey testKey8 = UXKeys.create(FakeUXKeys.TEST_KEY_8);

        ModelValue previousTestValueKey1 = new ModelValue(false);
        ModelValue currentTestValueKey1 = new ModelValue(true);

        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);

        //Set the required values to the store for initialization
        store.setModelValue(previousTestValueKey1, testKey8.getKeyPath());
        Assert.assertEquals(uxKeyManager.getValue(testKey8), previousTestValueKey1.getData());

        //Add a test subscriber to check received values and asserts in onNext to check value data
        TestSubscriber<BroadcastValues> testSubscriber =
            uxKeyManager.addObserver(testKey8).doOnNext(broadcastValues -> {
                Assert.assertEquals(previousTestValueKey1.getData(), broadcastValues.getPreviousValue().getData());
                Assert.assertEquals(currentTestValueKey1.getData(), broadcastValues.getCurrentValue().getData());
            }).test();
        compositeDisposable.add(testSubscriber);
        compositeDisposable.add(uxKeyManager.setValue(testKey8, currentTestValueKey1.getData()).test().assertResult());

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
    }

    @Test
    public void uxKeys_addObserverForOnEventUpdateType_updatesSuccessfully() {
        UXKey testKey8 = UXKeys.create(FakeUXKeys.TEST_KEY_9);

        ModelValue previousTestValueKey1 = new ModelValue(272);
        ModelValue currentTestValueKey1 = new ModelValue(272);

        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);

        //Set the required values to the store for initialization
        store.setModelValue(previousTestValueKey1, testKey8.getKeyPath());
        Assert.assertEquals(uxKeyManager.getValue(testKey8), previousTestValueKey1.getData());

        //Add a test subscriber to check received values and asserts in onNext to check value data
        TestSubscriber<BroadcastValues> testSubscriber =
            uxKeyManager.addObserver(testKey8).doOnNext(broadcastValues -> {
                Assert.assertEquals(previousTestValueKey1.getData(), broadcastValues.getPreviousValue().getData());
                Assert.assertEquals(currentTestValueKey1.getData(), broadcastValues.getCurrentValue().getData());
            }).test();
        compositeDisposable.add(testSubscriber);
        compositeDisposable.add(uxKeyManager.setValue(testKey8, currentTestValueKey1.getData()).test().assertResult());

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
    }

    @Test
    public void uxKeys_removeObserverForKey_removesSuccessfully() {
        UXKey testKey3 = UXKeys.create(FakeUXKeys.TEST_KEY_3);

        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);

        Integer value = (Integer) uxKeyManager.getValue(testKey3);
        if (value == null) {
            value = 0;
        }

        // Initialize test subscribers using observers added to the keys and assert no initial values
        TestSubscriber<BroadcastValues> testSubscriber1 = uxKeyManager.addObserver(testKey3).test();
        testSubscriber1.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber2 = uxKeyManager.addObserver(testKey3).test();
        testSubscriber2.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber3 = uxKeyManager.addObserver(testKey3).test();
        testSubscriber3.assertNoValues();

        // Set 5 values to each key
        for (int i = 0; i < 5; i++) {
            compositeDisposable.add(uxKeyManager.setValue(testKey3, ++value).test().assertResult());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        // Assert that each observer received 5 values
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber1.assertValueCount(5);
        testSubscriber2.assertValueCount(5);
        testSubscriber3.assertValueCount(5);

        // remove all observers for given key
        uxKeyManager.removeAllObserversForKey(testKey3);

        // Set 5 more values to each key
        for (int i = 0; i < 5; i++) {
            compositeDisposable.add(uxKeyManager.setValue(testKey3, ++value).test().assertResult());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        // Assert that each observer received only the initial 5 values and no additional values
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber1.assertValueCount(5);
        testSubscriber2.assertValueCount(5);
        testSubscriber3.assertValueCount(5);

        // Assert all subscribers have completed
        testSubscriber1.assertComplete();
        testSubscriber2.assertComplete();
        testSubscriber3.assertComplete();

        compositeDisposable.add(testSubscriber1);
        compositeDisposable.add(testSubscriber2);
        compositeDisposable.add(testSubscriber3);
    }

    @Test
    public void uxKeys_removeAllObservers_removedSuccessfully() {
        UXKey testKey4 = UXKeys.create(FakeUXKeys.TEST_KEY_4);
        UXKey testKey5 = UXKeys.create(FakeUXKeys.TEST_KEY_5);

        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);

        Integer value1 = (Integer) uxKeyManager.getValue(testKey4);
        Integer value2 = (Integer) uxKeyManager.getValue(testKey5);
        if (value1 == null) {
            value1 = 0;
        }
        if (value2 == null) {
            value2 = 0;
        }

        // Initialize test subscribers using observers added to the keys and assert no initial values
        TestSubscriber<BroadcastValues> testSubscriber1 = uxKeyManager.addObserver(testKey4).test();
        testSubscriber1.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber2 = uxKeyManager.addObserver(testKey4).test();
        testSubscriber2.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber3 = uxKeyManager.addObserver(testKey4).test();
        testSubscriber3.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber4 = uxKeyManager.addObserver(testKey5).test();
        testSubscriber4.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber5 = uxKeyManager.addObserver(testKey5).test();
        testSubscriber5.assertNoValues();

        TestSubscriber<BroadcastValues> testSubscriber6 = uxKeyManager.addObserver(testKey5).test();
        testSubscriber6.assertNoValues();

        // Set 5 values to each key
        for (int i = 0; i < 5; i++) {
            compositeDisposable.add(uxKeyManager.setValue(testKey4, ++value1).test().assertResult());
            compositeDisposable.add(uxKeyManager.setValue(testKey5, ++value2).test().assertResult());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        // Assert that each observer received 5 values
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber1.assertValueCount(5);
        testSubscriber2.assertValueCount(5);
        testSubscriber3.assertValueCount(5);
        testSubscriber4.assertValueCount(5);
        testSubscriber5.assertValueCount(5);
        testSubscriber6.assertValueCount(5);

        // Remove all observers for all keys
        uxKeyManager.removeAllObservers();

        // Set 5 more values to each key
        for (int i = 0; i < 5; i++) {
            compositeDisposable.add(uxKeyManager.setValue(testKey4, ++value1).test().assertResult());
            compositeDisposable.add(uxKeyManager.setValue(testKey5, ++value2).test().assertResult());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        // Assert that each observer received only the initial 5 values and no additional values
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        testSubscriber1.assertValueCount(5);
        testSubscriber2.assertValueCount(5);
        testSubscriber3.assertValueCount(5);
        testSubscriber4.assertValueCount(5);
        testSubscriber5.assertValueCount(5);
        testSubscriber6.assertValueCount(5);

        // Assert all subscribers have completed
        testSubscriber1.assertComplete();
        testSubscriber2.assertComplete();
        testSubscriber3.assertComplete();
        testSubscriber4.assertComplete();
        testSubscriber5.assertComplete();
        testSubscriber6.assertComplete();

        compositeDisposable.add(testSubscriber1);
        compositeDisposable.add(testSubscriber2);
        compositeDisposable.add(testSubscriber3);
        compositeDisposable.add(testSubscriber4);
        compositeDisposable.add(testSubscriber5);
        compositeDisposable.add(testSubscriber6);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
        compositeDisposable.dispose();
    }
}
