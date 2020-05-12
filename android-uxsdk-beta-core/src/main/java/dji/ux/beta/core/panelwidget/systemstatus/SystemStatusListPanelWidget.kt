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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.ux.beta.R
import dji.ux.beta.core.base.OnStateChangeCallback
import dji.ux.beta.core.base.panelwidget.ListPanelWidget
import dji.ux.beta.core.base.panelwidget.PanelWidgetConfiguration
import dji.ux.beta.core.base.panelwidget.PanelWidgetType
import dji.ux.beta.core.base.panelwidget.WidgetID
import dji.ux.beta.core.extension.getIntegerAndUse
import dji.ux.beta.core.extension.toggleVisibility
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget

/**
 * To Allow the user to toggle hide and show this panel widget, use in conjunction
 * with [SystemStatusWidget].
 *
 * This panel widget shows the system status list that includes a list of items (like IMU, GPS, etc.).
 * The current version of this panel widget is a sample and more items are to come
 * in future releases.
 *
 * Customization:
 * Use the attribute "excludeItem" to permanently remove items from the list. This will prevent a
 * certain item from being created and shown throughout the lifecycle of the panel widget. Here are
 * all the flags: flight_mode, rc_stick_mode, sd_card_status, max_altitude.
 *
 * Note that multiple flags can be used simultaneously by logically OR'ing
 * them. For example, to hide sd card status and rc stick mode, it can be done by the
 * following two steps.
 * Define custom xmlns in its layout file:
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * Then, add following attribute to the SystemStatusListPanelWidget:
 * app:excludeItem="sd_card_status|rc_stick_mode".
 *
 * This panel widget also passes attributes to each of the child widgets created. See each
 * widget for individual customizations:
 * [dji.ux.beta.core.listitemwidget.flightmode.FlightModeListItemWidget],
 * [dji.ux.beta.core.listitemwidget.rcstickmode.RCStickModeListItemWidget],
 * [dji.ux.beta.core.listitemwidget.sdcardstatus.SDCardStatusListItemWidget],
 * [dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget].
 *
 * To customize the individual widgets, pass a theme in XML:
 * <code>android:theme="@style/UXSDKSystemStatusListTheme"</code
 */
class SystemStatusListPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
                context,
                PanelWidgetType.LIST,
                showTitleBar = true,
                panelTitle = context.getString(R.string.uxsdk_system_status_list_title),
                hasCloseButton = true)
) : ListPanelWidget<Any>(context, attrs, defStyleAttr, configuration), OnStateChangeCallback<Any?> {

    //region Lifecycle
    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // Nothing to do
    }

    init {
        val blacklistValue = attrs?.let { initAttributes(context, it) }
        val blacklistSet = getBlacklistSet(blacklistValue)

        smartListModel = SystemStatusSmartListModel(context, attrs, blacklistSet)
    }

    override fun onSmartListModelCreated() {
        // Nothing to do
    }


    override fun reactToModelChanges() {
        // Nothing to do
    }

    override fun onStateChange(state: Any?) {
        toggleVisibility()
    }
    //endregion

    //region Customizations
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet): Int {
        context.obtainStyledAttributes(attrs, R.styleable.SystemStatusListPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.SystemStatusListPanelWidget_uxsdk_excludeItem) {
                return it
            }
        }

        return 0
    }
    //endregion

    //region Helpers
    private fun getBlacklistSet(blacklistValue: Int?): Set<WidgetID>? {
        return if (blacklistValue != null) {
            SystemStatusSmartListModel.WidgetOrder.values()
                    .filter { it.isItemExcluded(blacklistValue) }
                    .map { it.widgetID }
                    .toSet()
        } else {
            null
        }
    }
    //endregion

}