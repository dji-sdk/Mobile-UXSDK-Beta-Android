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
import android.util.AttributeSet
import dji.ux.beta.core.R
import dji.ux.beta.core.base.panel.PanelWidgetConfiguration
import dji.ux.beta.core.base.panel.PanelWidgetType
import dji.ux.beta.core.base.panel.ToolbarPanelTemplate
import dji.ux.beta.core.base.panel.ToolbarPanelWidget
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget

class TestToolbarPanel @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
                context = context,
                panelWidgetType = PanelWidgetType.TOOLBAR_TOP),
        private var widgetTheme: Int = 0
) : ToolbarPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    init {
        val wid = SystemStatusListPanelWidget(context)
        wid.setBackgroundColor(getColor(R.color.uxsdk_green))
        insert(0, ToolbarPanelTemplate(
                icon = getDrawable(R.drawable.uxsdk_ic_overview_status),
                title = "System",
                widgetView = wid))

        insert(1, ToolbarPanelTemplate(
                icon = getDrawable(R.drawable.uxsdk_ic_compass_home),
                title = "Tele",
                className = "dji.ux.beta.core.widget.compass.CompassWidget",
                keepAlive = true))
    }

    override fun updateUI() {
        //Do nothing
    }

    override fun reactToModelChanges() {
        //Do nothing
    }
}