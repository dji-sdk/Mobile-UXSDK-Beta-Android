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