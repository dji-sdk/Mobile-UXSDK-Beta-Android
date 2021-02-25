/*
 * Copyright (c) 2018-2021 DJI
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

package com.dji.ux.beta.sample.development.panel

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import dji.common.product.Model
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.panel.*
import dji.ux.beta.core.base.panel.listitem.ListItemTitleWidget
import dji.ux.beta.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidget
import dji.ux.beta.core.panel.listitem.flightmode.FlightModeListItemWidget
import dji.ux.beta.core.panel.listitem.radioquality.RadioQualityListItemWidget

// TODO: Alain clean up this examples

class SingleListPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(context, PanelWidgetType.LIST)
) : ListPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        //Do nothing
    }

    init {
        smartListModel = SingleListSmartListModel(context)
    }

    override fun reactToModelChanges() {
        // TODO
    }

    override fun onSmartListModelCreated() {
        val disposable = smartListModel?.widgetCreated
                ?.observeOn(SchedulerProvider.ui())
                ?.subscribe {
                    val view = it.second
                    if (view is ListItemTitleWidget<*>) {
                        view.listItemTitleTextColor = Color.BLUE
                    }
                }
        disposable?.let { addDisposable(it) }
    }

}

// This is an example of SmartModel defined by UXSDK
open class SingleListSmartListModel @JvmOverloads constructor(
        context: Context,
        blacklist: Set<String>? = setOf(WidgetOrder.AircraftBatteryA.widgetID) // TODO remove
) : SmartListModel(context, null, blacklist) {

    protected open val mavic2EntWidgets by lazy {
        setOf(
                WidgetOrder.AircraftBatteryB.widgetID,
                WidgetOrder.FlightModeB.widgetID)
    }
    protected open val mavic2ZoomWidgets by lazy {
        setOf(
                WidgetOrder.AircraftBatteryA.widgetID,
                WidgetOrder.FlightModeA.widgetID)
    }
    protected open val p4pWidgets by lazy { mavic2EntWidgets + mavic2ZoomWidgets }

    override val registeredWidgetIDList: List<WidgetID> = WidgetOrder.values.map { it.widgetID }.toList()

    override val defaultActiveWidgetSet: Set<WidgetID> = setOf(WidgetOrder.FlightModeA.widgetID)

    override fun inSetUp() {
        //Do nothing
    }

    protected open fun getActiveWidgets(model: Model): Set<WidgetID> {
        return when (model) {
            Model.MAVIC_2, Model.MAVIC_2_ZOOM, Model.MAVIC_2_PRO -> mavic2ZoomWidgets
            Model.MAVIC_2_ENTERPRISE -> mavic2EntWidgets
            Model.PHANTOM_4_PRO -> p4pWidgets
            else -> defaultActiveWidgetSet
        }
    }

    override fun inCleanUp() {
        //Do nothing
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        //Do nothing
    }

    override fun onAircraftModelChanged(model: Model) {
        if (model != Model.DISCONNECT || model != Model.UNKNOWN_AIRCRAFT || model != Model.UNKNOWN_HANDHELD) {
            val newOrder = getActiveWidgets(model)
            buildAndInstallWidgets(newOrder)
        }
    }

    override fun createWidget(widgetID: WidgetID): View {
        return when (widgetID) {
            WidgetOrder.FlightModeA.widgetID -> FlightModeListItemWidget(context)
            WidgetOrder.FlightModeB.widgetID -> {
                val flightModeListItemWidget = FlightModeListItemWidget(context)
                flightModeListItemWidget.setBackgroundColor(Color.RED)
                flightModeListItemWidget
            }
            WidgetOrder.AircraftBatteryA.widgetID -> AircraftBatteryTemperatureListItemWidget(context)
            WidgetOrder.AircraftBatteryB.widgetID -> {
                val aircraftBatteryTemperatureListItemWidget = AircraftBatteryTemperatureListItemWidget(context)
                aircraftBatteryTemperatureListItemWidget.setBackgroundColor(Color.RED)
                aircraftBatteryTemperatureListItemWidget
            }
            else -> throw IllegalStateException("The WidgetID ($widgetID) is not recognized.")
        }
    }

    // TODO do we need explicit index? we can use ordinal
    enum class WidgetOrder(val widgetID: WidgetID) {
        FlightModeA("flightmodeA"),
        AircraftBatteryA("aircraftbatteryA"),
        FlightModeB("flightmodeB"),
        AircraftBatteryB("aircraftbatteryB");

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun from(widgetID: WidgetID): WidgetOrder? =
                    values.find { it.widgetID == widgetID }
        }
    }
}


// This is an example of how a developer can customize the original smartmodel.
// Here the developer wants to add a new item for any product connected,
// change the order when a mavic 2 Ent is connected, and change the default order of items.
class CustomSingleListSmartListModel @JvmOverloads constructor(
        context: Context,
        blacklist: Set<WidgetID>? = null
) : SingleListSmartListModel(context, blacklist) {

    val radioQualityWidgetID: WidgetID = "radioquality"

    // Remove a widget from the mavic 2 enterprise
    override val mavic2EntWidgets: Set<WidgetID> by lazy {
        super.mavic2EntWidgets.minus(WidgetOrder.AircraftBatteryA.widgetID)
    }

    // Add a new widget
    override val registeredWidgetIDList: List<WidgetID> by lazy {
        super.registeredWidgetIDList.plus(radioQualityWidgetID)
    }

    // Change the default list
    override val defaultActiveWidgetSet: Set<WidgetID> by lazy {
        super.defaultActiveWidgetSet.plus(radioQualityWidgetID)
    }

    override fun inSetUp() {
        super.inSetUp()
        // Change the original order of the items
        setIndex(0, WidgetOrder.AircraftBatteryA.widgetID) // Order can be changed externally as well
    }

    // Provide a widget that the user wants to inject
    override fun createWidget(widgetID: WidgetID): View {
        if (widgetID == radioQualityWidgetID) {
            return RadioQualityListItemWidget(context)
        }

        return super.createWidget(widgetID)
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        //Do nothing
    }

    override fun onAircraftModelChanged(model: Model) {
        //Do nothing
    }

    override fun getActiveWidgets(model: Model): Set<WidgetID> {
        // Add widget to all models
        return super.getActiveWidgets(model) + setOf(radioQualityWidgetID)
    }

}
