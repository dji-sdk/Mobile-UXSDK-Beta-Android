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

package dji.ux.beta.core.panel.listitem.aircraftbatterytemperature

import android.content.Context
import android.util.AttributeSet
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.ux.beta.core.communication.GlobalPreferencesManager
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidgetModel.AircraftBatteryTemperatureItemState
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidgetModel.AircraftBatteryTemperatureItemState.AircraftBatteryState
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidgetModel.AircraftBatteryTemperatureItemState.ProductDisconnected
import dji.ux.beta.core.util.UnitConversionUtil.TemperatureUnitType.*

/**
 * Aircraft battery temperature list item
 *
 */
open class AircraftBatteryTemperatureListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<Any>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL,
        R.style.UXSDKAircraftBatteryTemperatureListItem
) {

    private val widgetModel by lazy {
        AircraftBatteryTemperatureListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.aircraftBatteryTemperatureState
                .observeOn(SchedulerProvider.ui())
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


    private fun updateUI(aircraftBatteryTemperatureItemState: AircraftBatteryTemperatureItemState) {
        when (aircraftBatteryTemperatureItemState) {
            is ProductDisconnected -> {
                isEnabled = false
                listItemLabelTextColor = disconnectedValueColor
                listItemLabel = resources.getString(R.string.uxsdk_string_default_value)
            }
            is AircraftBatteryState -> {
                listItemLabelTextColor = getColor(R.color.uxsdk_white)
                listItemLabel = when (aircraftBatteryTemperatureItemState.unitType) {
                    CELSIUS -> String.format(getString(R.string.uxsdk_celsius_unit), aircraftBatteryTemperatureItemState.temperature)
                    FAHRENHEIT -> String.format(getString(R.string.uxsdk_celsius_unit), aircraftBatteryTemperatureItemState.temperature)
                    KELVIN -> String.format(getString(R.string.uxsdk_celsius_unit), aircraftBatteryTemperatureItemState.temperature)
                }

            }
        }

    }

    override fun onButtonClick() {
        // No code needed
    }


}