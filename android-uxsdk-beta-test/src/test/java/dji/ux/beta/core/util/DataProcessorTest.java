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

package dji.ux.beta.core.util;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;

import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins;
import dji.thirdparty.io.reactivex.schedulers.TestScheduler;
import dji.thirdparty.io.reactivex.subscribers.TestSubscriber;
import dji.ux.beta.categories.UnitTest;

/**
 * Test:
 * This class tests the public methods in the {@link DataProcessor}
 * 1.
 * {@link DataProcessorTest#dataProcessor_emitsResults()}
 * Test the initial value set by the onCreate() method appears in the results and the values
 * added by the onNext() method appear in the results when the subscriber is subscribed at the
 * time the values are added.
 * 2.
 * {@link DataProcessorTest#dataProcessor_onError()}
 * Test an error added by the onError method appears in the results.
 * 3.
 * {@link DataProcessorTest#dataProcessor_getValue()}
 * Test the getValue() method returns the latest value in the data processor and updates when a
 * new value is added to the data processor.
 */
@Category(UnitTest.class)
public class DataProcessorTest {

    private TestScheduler testScheduler;

    @Before
    public void beforeTest() {
        RxJavaPlugins.reset();
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
    }

    @Test
    public void dataProcessor_emitsResults() {
        DataProcessor<Integer> processor = DataProcessor.create(0);

        Flowable<Integer> flowable = processor.toFlowable();

        TestSubscriber<Integer> testSubscriber = flowable.test();

        processor.onNext(1);
        processor.onNext(2);

        TestSubscriber<Integer> testSubscriber2 = flowable.test();

        processor.onNext(3);
        processor.onComplete();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        /// testSubscriber received all items
        testSubscriber.assertResult(0, 1, 2, 3);

        // current item was 2 when testSubscriber2 got subscribed
        testSubscriber2.assertResult(2, 3);

        testSubscriber.dispose();
        testSubscriber2.dispose();
    }

    @Test
    public void dataProcessor_onError() {
        DataProcessor<Integer> processor = DataProcessor.create(0);

        Flowable<Integer> flowable = processor.toFlowable();

        TestSubscriber<Integer> testSubscriber = flowable.test();

        RuntimeException runtimeException = new RuntimeException("error");
        processor.onError(runtimeException);

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        // testSubscriber received the error
        testSubscriber.assertError(runtimeException);

        testSubscriber.dispose();
    }

    @Test
    public void dataProcessor_getValue() {
        DataProcessor<Integer> processor = DataProcessor.create(0);

        processor.onNext(1);
        Assert.assertEquals(processor.getValue().intValue(), 1);

        processor.onNext(2);
        Assert.assertEquals(processor.getValue().intValue(), 2);
    }

    @After
    public void afterTest() {
        RxJavaPlugins.reset();
    }
}
