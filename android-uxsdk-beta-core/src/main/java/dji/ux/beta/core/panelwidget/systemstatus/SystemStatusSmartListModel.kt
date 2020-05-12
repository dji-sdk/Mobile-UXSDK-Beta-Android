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
 */

package dji.ux.beta.core.panelwidget.systemstatus

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.panelwidget.SmartListModel
import dji.ux.beta.core.base.panelwidget.WidgetID
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget
import dji.ux.beta.core.listitemwidget.rcstickmode.RCStickModeListItemWidget
import dji.ux.beta.core.listitemwidget.sdcardstatus.SDCardStatusListItemWidget

private const val TAG = "systemstatuslist"

/**
 * [SmartListModel] to handle what items should be shown for the [SystemStatusListPanelWidget].
 */
open class SystemStatusSmartListModel @JvmOverloads constructor(
        context: Context,
        private val attrs: AttributeSet? = null,
        blacklist: Set<WidgetID>? = null
) : SmartListModel(context, attrs, blacklist) {

    //region Default Items
    /**
     * List of [WidgetID] of widgets that are allowed in this list.
     */
    override val registeredWidgetIDList: List<WidgetID> by lazy {
        WidgetOrder.values().map { it.widgetID }
    }

    /**
     * Default set of widgets that should be shown
     */
    override val defaultActiveWidgetSet: Set<WidgetID> by lazy {
        registeredWidgetIDList.toSet()
    }
    //endregion

    //region Fields
    private val widgetModel by lazy {
        SystemStatusListWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    //endregion

    //region Lifecycle
    override fun inSetUp() {
        widgetModel.setup()
    }


    override fun inCleanUp() {
        widgetModel.cleanup()
    }

    override fun createWidget(widgetID: WidgetID): View {
        return when (WidgetOrder.from(widgetID)) {
            WidgetOrder.FLIGHT_MODE -> FlightModeListItemWidget(context, attrs)
            WidgetOrder.RC_STICK_MODE -> RCStickModeListItemWidget(context, attrs)
            WidgetOrder.SD_CARD_STATUS -> SDCardStatusListItemWidget(context, attrs)
            WidgetOrder.MAX_ALTITUDE -> MaxAltitudeListItemWidget(context, attrs)
            null -> throw IllegalStateException("The WidgetID ($widgetID) is not recognized.")
        }
    }
    //endregion

    /**
     * Default Widgets for the [SystemStatusListPanelWidget]
     * @property widgetID Identifier for the item
     * @property value Int value for excluding items
     */
    enum class WidgetOrder(val widgetID: WidgetID, val value: Int) {
        /**
         * Maps to [FlightModeListItemWidget].
         */
        FLIGHT_MODE("flight_mode", 1),

        /**
         * Maps to [RCStickModeListItemWidget].
         */
        RC_STICK_MODE("rc_stick_mode", 2),

        /**
         * Maps to [SDCardStatusListItemWidget].
         */
        SD_CARD_STATUS("sd_card_status", 4),

        /**
         * Maps to [MaxAltitudeListItemWidget].
         */
        MAX_ALTITUDE("max_altitude", 8);

        /**
         * Checks if the item is excluded given the flag [excludeItems].
         */
        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }

        companion object {
            /**
             * Create a [WidgetOrder] from a [WidgetID].
             */
            @JvmStatic
            fun from(widgetID: WidgetID): WidgetOrder? =
                    values().find { it.widgetID == widgetID }

            /**
             * Create a [WidgetOrder] from an int value.
             */
            @JvmStatic
            fun from(value: Int): WidgetOrder? =
                    values().find { it.value == value }
        }
    }
}