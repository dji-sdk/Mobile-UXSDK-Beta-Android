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

package dji.ux.beta.core.panelwidget.topbar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.ux.beta.R
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.panelwidget.BarPanelWidget
import dji.ux.beta.core.base.panelwidget.PanelItem
import dji.ux.beta.core.base.panelwidget.PanelWidgetConfiguration
import dji.ux.beta.core.extension.getDimension
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.widget.airsense.AirSenseWidget
import dji.ux.beta.core.widget.battery.BatteryWidget
import dji.ux.beta.core.widget.connection.ConnectionWidget
import dji.ux.beta.core.widget.flightmode.FlightModeWidget
import dji.ux.beta.core.widget.gpssignal.GPSSignalWidget
import dji.ux.beta.core.widget.remotecontrollersignal.RemoteControllerSignalWidget
import dji.ux.beta.core.widget.simulator.SimulatorIndicatorWidget
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget
import dji.ux.beta.core.widget.videosignal.VideoSignalWidget
import dji.ux.beta.core.widget.vision.VisionWidget
import java.util.*

/**
 * Container for the top bar widgets. This [BarPanelWidget] is divided into two parts.
 * The left list contains:
 * - [SystemStatusWidget]
 * The right list contains
 * - [FlightModeWidget]
 * - [SimulatorIndicatorWidget]
 * - [AirSenseWidget]
 * - [GPSSignalWidget]
 * - [VisionWidget]
 * - [RemoteControllerSignalWidget]
 * - [VideoSignalWidget]
 * - [BatteryWidget]
 * - [ConnectionWidget]
 *
 * * Customization:
 * Use the attribute "excludeItem" to permanently remove items from the list. This will prevent a
 * certain item from being created and shown throughout the lifecycle of the bar panel widget. Here are
 * all the flags: system_status, flight_mode, simulator_indicator, air_sense, gps_signal,
 * vision, rc_signal, video_signal, battery, connection.
 *
 * Note that multiple flags can be used simultaneously by logically OR'ing
 * them. For example, to hide flight_mode and vision, it can be done by the
 * following two steps.
 * Define custom xmlns in its layout file:
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * Then, add following attribute to the [TopBarPanelWidget]:
 * app:excludeItem="flight_mode|vision".
 *
 * This panel widget also passes attributes to each of the child widgets created. See each
 * individual's widget documentation for more customization options.
 */
open class TopBarPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        barPanelWidgetOrientation: BarPanelWidgetOrientation = BarPanelWidgetOrientation.HORIZONTAL
) : BarPanelWidget<Any>(context, attrs, defStyleAttr, barPanelWidgetOrientation) {

    //region Widgets Properties
    /**
     * Getter for [SystemStatusWidget]. Null when excluded from the bar panel.
     */
    val systemStatusWidget: SystemStatusWidget?
    /**
     * Getter for [FlightModeWidget]. Null when excluded from the bar panel.
     */
    val flightModeWidget: FlightModeWidget?
    /**
     * Getter for [SimulatorIndicatorWidget]. Null when excluded from the bar panel.
     */
    val simulatorIndicatorWidget: SimulatorIndicatorWidget?
    /**
     * Getter for [AirSenseWidget]. Null when excluded from the bar panel.
     */
    val airSenseWidget: AirSenseWidget?
    /**
     * Getter for [GPSSignalWidget]. Null when excluded from the bar panel.
     */
    @get:JvmName("getGPSSignalWidget")
    val gpsSignalWidget: GPSSignalWidget?
    /**
     * Getter for [VisionWidget]. Null when excluded from the bar panel.
     */
    val visionWidget: VisionWidget?
    /**
     * Getter for [RemoteControllerSignalWidget]. Null when excluded from the bar panel.
     */
    val remoteControllerSignalWidget: RemoteControllerSignalWidget?
    /**
     * Getter for [VideoSignalWidget]. Null when excluded from the bar panel.
     */
    val videoSignalWidget: VideoSignalWidget?
    /**
     * Getter for [BatteryWidget]. Null when excluded from the bar panel.
     */
    val batteryWidget: BatteryWidget?
    /**
     * Getter for [ConnectionWidget]. Null when excluded from the bar panel.
     */
    val connectionWidget: ConnectionWidget?
    //endregion

    //region Private properties
    private var blacklistValue = 0
    //endregion

    //region Lifecycle & Setup

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // Nothing to do
    }

    init {
        val leftPanelItems = ArrayList<PanelItem>()
        if (!WidgetValue.SYSTEM_STATUS.isItemExcluded(blacklistValue)) {
            systemStatusWidget = SystemStatusWidget(context, attrs)
            leftPanelItems.add(PanelItem(systemStatusWidget, itemMarginTop = 0, itemMarginBottom = 0))
        } else {
            systemStatusWidget = null
        }
        addLeftWidgets(leftPanelItems.toTypedArray())

        val rightPanelItems = ArrayList<PanelItem>()
        if (!WidgetValue.FLIGHT_MODE.isItemExcluded(blacklistValue)) {
            flightModeWidget = FlightModeWidget(context, attrs)
            rightPanelItems.add(PanelItem(flightModeWidget))
        } else {
            flightModeWidget = null
        }
        if (!WidgetValue.SIMULATOR_INDICATOR.isItemExcluded(blacklistValue)) {
            simulatorIndicatorWidget = SimulatorIndicatorWidget(context, attrs)
            rightPanelItems.add(PanelItem(simulatorIndicatorWidget))
        } else {
            simulatorIndicatorWidget = null
        }
        if (!WidgetValue.AIR_SENSE.isItemExcluded(blacklistValue)) {
            airSenseWidget = AirSenseWidget(context, attrs)
            rightPanelItems.add(PanelItem(airSenseWidget))
        } else {
            airSenseWidget = null
        }
        if (!WidgetValue.GPS_SIGNAL.isItemExcluded(blacklistValue)) {
            gpsSignalWidget = GPSSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(gpsSignalWidget as GPSSignalWidget))
        } else {
            gpsSignalWidget = null
        }
        if (!WidgetValue.VISION.isItemExcluded(blacklistValue)) {
            visionWidget = VisionWidget(context, attrs)
            rightPanelItems.add(PanelItem(visionWidget))
        } else {
            visionWidget = null
        }
        if (!WidgetValue.RC_SIGNAL.isItemExcluded(blacklistValue)) {
            remoteControllerSignalWidget = RemoteControllerSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(remoteControllerSignalWidget))
        } else {
            remoteControllerSignalWidget = null
        }
        if (!WidgetValue.VIDEO_SIGNAL.isItemExcluded(blacklistValue)) {
            videoSignalWidget = VideoSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(videoSignalWidget))
        } else {
            videoSignalWidget = null
        }
        if (!WidgetValue.BATTERY.isItemExcluded(blacklistValue)) {
            batteryWidget = BatteryWidget(context, attrs)
            rightPanelItems.add(PanelItem(batteryWidget))
        } else {
            batteryWidget = null
        }
        if (!WidgetValue.CONNECTION.isItemExcluded(blacklistValue)) {
            connectionWidget = ConnectionWidget(context, attrs)
            rightPanelItems.add(PanelItem(connectionWidget))
        } else {
            connectionWidget = null
        }
        addRightWidgets(rightPanelItems.toTypedArray())
    }

    @SuppressLint("Recycle")
    override fun initAttributes(attrs: AttributeSet) {
        guidelinePercent = 0.3f
        itemsMarginTop = getDimension(R.dimen.uxsdk_bar_panel_margin).toInt()
        itemsMarginBottom = getDimension(R.dimen.uxsdk_bar_panel_margin).toInt()

        context.obtainStyledAttributes(attrs, R.styleable.TopBarPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.TopBarPanelWidget_uxsdk_excludeTopBarItem) {
                blacklistValue = it
            }
        }

        super.initAttributes(attrs)
    }

    override fun reactToModelChanges() {
        // Nothing to do
    }
    //endregion

    //region Customizations
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(
                    WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.EXPAND)
    //endregion

    private enum class WidgetValue(val value: Int) {
        SYSTEM_STATUS(1),
        FLIGHT_MODE(2),
        SIMULATOR_INDICATOR(4),
        AIR_SENSE(8),
        GPS_SIGNAL(16),
        VISION(32),
        RC_SIGNAL(64),
        VIDEO_SIGNAL(128),
        BATTERY(256),
        CONNECTION(512);

        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }
    }
}