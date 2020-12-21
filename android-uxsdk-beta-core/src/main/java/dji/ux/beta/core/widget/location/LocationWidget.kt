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

package dji.ux.beta.core.widget.location

import android.content.Context
import android.util.AttributeSet
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.widget.BaseTelemetryWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.widget.location.LocationWidget.ModelState
import dji.ux.beta.core.widget.location.LocationWidget.ModelState.LocationStateUpdated
import dji.ux.beta.core.widget.location.LocationWidgetModel.LocationState
import java.text.DecimalFormat

/**
 * Widget shows location coordinates of the aircraft
 */
open class LocationWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseTelemetryWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.TEXT_IMAGE_LEFT,
        widgetTheme,
        R.style.UXSDKLocationWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("+#00.000000;-#00.000000")

    override val imperialDecimalFormat: DecimalFormat = metricDecimalFormat

    private val widgetModel: LocationWidgetModel by lazy {
        LocationWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.locationState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(locationState: LocationState) {
        widgetStateDataProcessor.onNext(LocationStateUpdated(locationState))
        valueString = if (locationState is LocationState.CurrentLocation) {
            String.format(getString(R.string.uxsdk_location_coordinates),
                    metricDecimalFormat.format(locationState.latitude).toString(),
                    metricDecimalFormat.format(locationState.longitude).toString())
        } else {
            getString(R.string.uxsdk_location_default)
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)
    //endregion

    //region Hooks
    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines widget state updates
     */

    sealed class ModelState {

        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * Location model state
         */
        data class LocationStateUpdated(val locationState: LocationState) : ModelState()
    }
    //endregion
}