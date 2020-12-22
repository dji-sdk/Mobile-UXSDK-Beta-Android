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

package dji.ux.beta.core.base

import androidx.annotation.CheckResult
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.RxUtil

/**
 * Base module class for grouping sets of data that are often used together.
 */
abstract class BaseModule {

    //region Lifecycle
    /**
     * Setup method for initialization that must be implemented
     */
    protected abstract fun setup(widgetModel: WidgetModel)

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    protected abstract fun cleanup()
    //endregion

    /**
     * Bind the given DJIKey to the given data processor and attach the given consumer to it.
     * The data processor and side effect consumer will be invoked with every update to the key.
     * The side effect consumer will be called before the data processor is updated.
     *
     * @param key                DJIKey to be bound
     * @param dataProcessor      DataProcessor to be bound
     * @param sideEffectConsumer Consumer to be called along with data processor
     */
    protected open fun bindDataProcessor(widgetModel: WidgetModel,
                                         key: DJIKey,
                                         dataProcessor: DataProcessor<*>,
                                         sideEffectConsumer: Consumer<Any> = Consumer {}) {
        widgetModel.bindDataProcessor(key, dataProcessor, sideEffectConsumer)
    }

    /**
     * Get a throwable error consumer for the given error.
     *
     * @param tag     Tag for the log
     * @param message Message to be logged
     * @return Throwable consumer
     */
    @CheckResult
    protected open fun logErrorConsumer(tag: String, message: String): Consumer<Throwable?>? {
        return RxUtil.logErrorConsumer(tag, message)
    }

}