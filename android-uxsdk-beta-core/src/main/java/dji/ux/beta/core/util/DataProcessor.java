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

import androidx.annotation.NonNull;

import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.processors.BehaviorProcessor;
import dji.ux.beta.core.base.SchedulerProvider;

/**
 * Processor that emits the most recent item it has observed and all subsequent observed items
 *
 * @param <T> the type of item expected to be observed and emitted by the Processor
 */
public final class DataProcessor<T> {

    private final BehaviorProcessor<T> processor;

    /**
     * Creates a DataProcessor with the given default value
     *
     * @param defaultValue The first item that will be emitted
     * @param <T>          The type of item the processor will emit
     * @return The constructed DataProcessor
     */
    @NonNull
    public static <T> DataProcessor<T> create(@NonNull T defaultValue) {
        return new DataProcessor<>(defaultValue);
    }

    private DataProcessor(@NonNull T defaultValue) {
        processor = BehaviorProcessor.createDefault(defaultValue);
    }

    /**
     * Emit a new item
     *
     * @param data item to be emitted
     */
    public void onNext(@NonNull Object data) {
        T newData = (T) data;
        processor.onNext(newData);
    }

    /**
     * Emit completion event
     */
    public void onComplete() {
        processor.onComplete();
    }

    /**
     * Emit an error event
     *
     * @param error The error to emit
     */
    public void onError(@NonNull Throwable error) {
        processor.onError(error);
    }

    /**
     * Get the latest value of the processor
     *
     * @return The latest value of the processor
     */
    @NonNull
    public T getValue() {
        return processor.getValue();
    }

    /**
     * Get the stream of data from the processor
     *
     * @return A Flowable representing the stream of data
     */
    @NonNull
    public Flowable<T> toFlowable() {
        return processor.observeOn(SchedulerProvider.computation())
                .onBackpressureLatest();
    }
}
