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

package dji.ux.beta.core.panelwidget.systemstatus

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.common.product.Model
import dji.common.product.Model.*
import dji.ux.beta.core.base.panelwidget.SmartListModel
import dji.ux.beta.core.base.panelwidget.WidgetID
import dji.ux.beta.core.listitemwidget.emmcstatus.EMMCStatusListItemWidget
import dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidget
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget
import dji.ux.beta.core.listitemwidget.overviewstatus.OverviewListItemWidget
import dji.ux.beta.core.listitemwidget.rcbattery.RCBatteryListItemWidget
import dji.ux.beta.core.listitemwidget.rcstickmode.RCStickModeListItemWidget
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidget
import dji.ux.beta.core.listitemwidget.sdcardstatus.SDCardStatusListItemWidget
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidget
import dji.ux.beta.core.listitemwidget.travelmode.TravelModeListItemWidget
import dji.ux.beta.core.listitemwidget.unittype.UnitTypeListItemWidget
import dji.ux.beta.core.panelwidget.systemstatus.SystemStatusSmartListModel.SystemStatusListItem.*

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
        listOf(OVERVIEW_STATUS.widgetID,
                RTH_ALTITUDE.widgetID,
                MAX_ALTITUDE.widgetID,
                MAX_FLIGHT_DISTANCE.widgetID,
                FLIGHT_MODE.widgetID,
                RC_STICK_MODE.widgetID,
                RC_BATTERY.widgetID,
                SD_CARD_STATUS.widgetID,
                EMMC_STATUS.widgetID,
                SSD_STATUS.widgetID,
                TRAVEL_MODE.widgetID,
                NOVICE_MODE.widgetID,
                UNIT_TYPE.widgetID)
    }

    /**
     * Default set of widgets that should be shown
     */
    override val defaultActiveWidgetSet: Set<WidgetID> by lazy {
        registeredWidgetIDList.toSet()
                .minus(EMMC_STATUS.widgetID)
                .minus(TRAVEL_MODE.widgetID)
                .minus(SSD_STATUS.widgetID)
    }
    //endregion

    //region Lifecycle
    override fun inSetUp() {
    }


    override fun inCleanUp() {
    }

    override fun onAircraftModelChanged(model: Model) {
        resetSystemStatusListToDefault()
        when (model) {
            INSPIRE_1,
            INSPIRE_1_PRO,
            INSPIRE_1_RAW -> onLandingGearUpdate(true)
            INSPIRE_2 -> {
                onLandingGearUpdate(true)
                onSSDSupported(true)
            }
            MAVIC_AIR,
            MAVIC_2_ZOOM,
            MAVIC_2_PRO,
            MAVIC_2,
            MAVIC_2_ENTERPRISE,
            MAVIC_2_ENTERPRISE_DUAL -> onInternalStorageSupported(true)
            else -> {
                // Do Nothing 
            }
        }
    }


    override fun createWidget(widgetID: WidgetID): View {
        return when (SystemStatusListItem.from(widgetID)) {
            OVERVIEW_STATUS -> OverviewListItemWidget(context, attrs)
            RTH_ALTITUDE -> ReturnToHomeAltitudeListItemWidget(context, attrs)
            FLIGHT_MODE -> FlightModeListItemWidget(context, attrs)
            RC_STICK_MODE -> RCStickModeListItemWidget(context, attrs)
            RC_BATTERY -> RCBatteryListItemWidget(context, attrs)
            SD_CARD_STATUS -> SDCardStatusListItemWidget(context, attrs)
            EMMC_STATUS -> EMMCStatusListItemWidget(context, attrs)
            MAX_ALTITUDE -> MaxAltitudeListItemWidget(context, attrs)
            MAX_FLIGHT_DISTANCE -> MaxFlightDistanceListItemWidget(context, attrs)
            TRAVEL_MODE -> TravelModeListItemWidget(context, attrs)
            UNIT_TYPE -> UnitTypeListItemWidget(context, attrs)
            SSD_STATUS -> SSDStatusListItemWidget(context, attrs)
            NOVICE_MODE -> NoviceModeListItemWidget(context, attrs)
            null -> throw IllegalStateException("The WidgetID ($widgetID) is not recognized.")
        }
    }
    //endregion

    //region Helpers
    private fun resetSystemStatusListToDefault() {
        onLandingGearUpdate(false)
        onInternalStorageSupported(false)
        onSSDSupported(false)
    }

    private fun onLandingGearUpdate(movable: Boolean) {
        if (!movable) {
            updateListMinus(TRAVEL_MODE.widgetID)
        } else {
            updateListPlus(TRAVEL_MODE.widgetID)
        }
    }

    private fun onInternalStorageSupported(supported: Boolean) {
        if (!supported) {
            updateListMinus(EMMC_STATUS.widgetID)
        } else {
            updateListPlus(EMMC_STATUS.widgetID)
        }
    }

    private fun onSSDSupported(supported: Boolean) {
        if (!supported) {
            updateListMinus(SSD_STATUS.widgetID)
        } else {
            updateListPlus(SSD_STATUS.widgetID)
        }
    }
    //endregion

    /**
     * Default Widgets for the [SystemStatusListPanelWidget]
     * @property widgetID Identifier for the item
     * @property value Int value for excluding items
     */
    enum class SystemStatusListItem(val widgetID: WidgetID, val value: Int) {

        /**
         * Maps to [FlightModeListItemWidget].
         */
        FLIGHT_MODE("flight_mode", 1),

        /**
         * Maps to [RCStickModeListItemWidget].
         */
        RC_STICK_MODE("rc_stick_mode", 16),

        /**
         * Maps to [RCBatteryListItemWidget].
         */
        RC_BATTERY("rc_battery", 32),

        /**
         * Maps to [SDCardStatusListItemWidget].
         */
        SD_CARD_STATUS("sd_card_status", 128),

        /**
         * Maps to [EMMCStatusListItemWidget].
         */
        EMMC_STATUS("emmc_status", 256),

        /**
         * Maps to [MaxAltitudeListItemWidget].
         */
        MAX_ALTITUDE("max_altitude", 512),

        /**
         * Maps to [MaxFlightDistanceListItemWidget].
         */
        MAX_FLIGHT_DISTANCE("max_flight_distance", 1024),

        /**
         * Maps to [TravelModeListItemWidget].
         */
        TRAVEL_MODE("travel_mode", 2048),

        /**
         * Maps to [UnitTypeListItemWidget].
         */
        UNIT_TYPE("unit_type", 4096),

        /**
         * Maps to [SSDStatusListItemWidget].
         */
        SSD_STATUS("ssd_status", 8192),

        /**
         * Maps to [NoviceModeListItemWidget].
         */
        NOVICE_MODE("novice_mode", 16384),

        /**
         * Maps to [OverviewListItemWidget].
         */
        OVERVIEW_STATUS("overview_status", 32768),

        /**
         * Maps to [ReturnToHomeAltitudeListItemWidget].
         */
        RTH_ALTITUDE("rth_altitude", 65536);


        /**
         * Checks if the item is excluded given the flag [excludeItems].
         */
        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }

        companion object {
            /**
             * Create a [SystemStatusListItem] from a [WidgetID].
             */
            @JvmStatic
            fun from(widgetID: WidgetID): SystemStatusListItem? =
                    values().find { it.widgetID == widgetID }

            /**
             * Create a [SystemStatusListItem] from an int value.
             */
            @JvmStatic
            fun from(value: Int): SystemStatusListItem? =
                    values().find { it.value == value }
        }
    }
}