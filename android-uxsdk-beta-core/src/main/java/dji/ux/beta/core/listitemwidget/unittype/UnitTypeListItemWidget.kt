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

package dji.ux.beta.core.listitemwidget.unittype

import android.content.Context
import android.util.AttributeSet
import dji.ux.beta.R
import dji.ux.beta.core.base.*
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemRadioButtonWidget
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.listitemwidget.unittype.UnitTypeListItemWidget.UnitTypeListItemState
import dji.ux.beta.core.listitemwidget.unittype.UnitTypeListItemWidget.UnitTypeListItemState.*
import dji.ux.beta.core.listitemwidget.unittype.UnitTypeListItemWidgetModel.UnitTypeState
import dji.ux.beta.core.util.UnitConversionUtil.UnitType

/**
 * Widget shows the current [UnitType] being used.
 * It also provides an option to switch between them.
 */
open class UnitTypeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemRadioButtonWidget<UnitTypeListItemState>(context, attrs, defStyleAttr) {

    private val schedulerProvider = SchedulerProvider.getInstance()


    private val widgetModel: UnitTypeListItemWidgetModel by lazy {
        UnitTypeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider,
                GlobalPreferencesManager.getInstance())
    }

    private var imperialItemIndex: Int = INVALID_OPTION_INDEX
    private var metricItemIndex: Int = INVALID_OPTION_INDEX

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_unit_type)
        listItemTitle = getString(R.string.uxsdk_list_item_unit_type)
        imperialItemIndex = addOptionToGroup(getString(R.string.uxsdk_list_item_unit_type_imperial))
        metricItemIndex = addOptionToGroup(getString(R.string.uxsdk_list_item_unit_type_metric))
    }


    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.unitTypeState
                .observeOn(schedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(UnitTypeUpdated(it))
                    updateUI(it)
                })
    }

    private fun updateUI(unitTypeState: UnitTypeState) {
        isEnabled = if (unitTypeState is UnitTypeState.CurrentUnitType) {
            if (unitTypeState.unitType == UnitType.IMPERIAL) {
                setSelected(imperialItemIndex)
            } else {
                setSelected(metricItemIndex)
            }
            true
        } else {
            false

        }
    }

    override fun onOptionTapped(optionIndex: Int, optionLabel: String) {
        val newUnitType: UnitType = if (optionIndex == imperialItemIndex) {
            UnitType.IMPERIAL
        } else {
            UnitType.METRIC
        }
        addDisposable(widgetModel.setUnitType(newUnitType)
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    widgetStateDataProcessor.onNext(SetUnitTypeSuccess)
                }, {
                    widgetStateDataProcessor.onNext(SetUnitTypeFailed(it as UXSDKError))
                    resetUI()
                }))

    }

    private fun resetUI() {
        addDisposable(widgetModel.unitTypeState
                .observeOn(schedulerProvider.ui())
                .subscribe { updateUI(it) })
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

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)


    /**
     * Class defines widget state updates
     */
    sealed class UnitTypeListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : UnitTypeListItemState()

        /**
         * Set unit type success
         */
        object SetUnitTypeSuccess : UnitTypeListItemState()

        /**
         * Set unit type failed
         */
        data class SetUnitTypeFailed(val error: UXSDKError) : UnitTypeListItemState()

        /**
         * Current unit type
         */
        data class UnitTypeUpdated(val unitTypeState: UnitTypeState) : UnitTypeListItemState()
    }

}