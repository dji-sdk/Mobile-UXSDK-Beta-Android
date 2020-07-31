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

package dji.ux.beta.core.listitemwidget.flightmode

import android.content.Context
import android.util.AttributeSet
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget.FlightModeListItemState
import dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget.FlightModeListItemState.FlightModeTextUpdated
import dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget.FlightModeListItemState.ProductConnected
import dji.ux.beta.core.widget.flightmode.FlightModeWidgetModel
import dji.ux.beta.core.widget.flightmode.FlightModeWidgetModel.FlightModeState.FlightModeUpdated

/**
 * Widget displays the current flight mode
 *
 */
open class FlightModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<FlightModeListItemState>(context, attrs, defStyleAttr, WidgetType.LABEL) {

    private val widgetModel by lazy {
        FlightModeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_flight_mode_list_item)
        listItemTitle = getString(R.string.uxsdk_list_item_flight_mode_title)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.flightModeState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

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

    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)


    private fun updateUI(flightModeState: FlightModeWidgetModel.FlightModeState) {
        if (flightModeState is FlightModeUpdated) {
            listItemLabel = flightModeState.flightModeString
            listItemLabelTextColor = normalValueColor
            widgetStateDataProcessor.onNext(FlightModeTextUpdated(flightModeState.flightModeString))
        } else {
            listItemLabel = getString(R.string.uxsdk_string_default_value)
            listItemLabelTextColor = disconnectedValueColor

        }
    }


    override fun onButtonClick() {
        // No code needed
    }

    /**
     * Get the [FlightModeListItemState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<FlightModeListItemState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class FlightModeListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : FlightModeListItemState()

        /**
         * Flight mode text update
         */
        data class FlightModeTextUpdated(val flightModeText: String) : FlightModeListItemState()
    }


}