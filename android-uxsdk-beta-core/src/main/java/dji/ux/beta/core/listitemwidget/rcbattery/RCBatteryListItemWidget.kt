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

package dji.ux.beta.core.listitemwidget.rcbattery

import android.content.Context
import android.util.AttributeSet
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.listitemwidget.rcbattery.RCBatteryListItemWidget.RCBatteryListItemState
import dji.ux.beta.core.listitemwidget.rcbattery.RCBatteryListItemWidget.RCBatteryListItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.rcbattery.RCBatteryListItemWidget.RCBatteryListItemState.RCBatteryStateUpdate
import dji.ux.beta.core.listitemwidget.rcbattery.RCBatteryListItemWidgetModel.RCBatteryState

/**
 * Remote controller battery list item
 */
open class RCBatteryListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<RCBatteryListItemState>(context, attrs, defStyleAttr, WidgetType.LABEL) {

    private val widgetModel by lazy {
        RCBatteryListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_rc_battery)
        listItemTitle = getString(R.string.uxsdk_list_item_rc_batery)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.rcBatteryState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { this.updateUI(it) })
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


    private fun updateUI(rcBatteryState: RCBatteryState) {
        widgetStateDataProcessor.onNext(RCBatteryStateUpdate(rcBatteryState))
        when (rcBatteryState) {
            RCBatteryState.ProductDisconnected -> {
                listItemLabelTextColor = disconnectedValueColor
                listItemLabel = getString(R.string.uxsdk_string_default_value)
            }
            is RCBatteryState.Normal -> {
                listItemLabelTextColor = normalValueColor
                listItemLabel = String.format(getString(R.string.uxsdk_rc_battery_percent),
                        rcBatteryState.remainingChargePercent)
            }
            is RCBatteryState.Low -> {
                listItemLabelTextColor = errorValueColor
                listItemLabel = String.format(getString(R.string.uxsdk_rc_battery_percent),
                        rcBatteryState.remainingChargePercent)
            }
        }
    }

    /**
     * Class defines the widget state updates
     */
    sealed class RCBatteryListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : RCBatteryListItemState()

        /**
         * RC battery State update
         */
        data class RCBatteryStateUpdate(val rcBatteryState: RCBatteryState) : RCBatteryListItemState()
    }

    override fun onButtonClick() {
        // No implementation needed
    }

}
