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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import com.dji.ux.beta.sample.R
import dji.common.product.Model
import dji.ux.beta.core.base.panel.*
import dji.ux.beta.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getResourceIdAndUse

class SampleNavigationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : PanelNavigationView(context, attrs, defStyleAttr, SampleNavigationListPanelWidget(context)) {

    @StyleRes
    private var panelStyle: Int? = null

    @StyleRes
    private var sampleViewStyle: Int? = null

    @StyleRes
    private var secondSampleViewStyle: Int? = null

    override fun onViewPushed(view: View) {
        if (view is PanelWidget<*, *>) {
            panelStyle?.let { view.setStyle(it) }
        }
        if (view is SampleNavigationListPanelWidget) {
            sampleViewStyle?.let { view.setStyle(it) }
        } else if (view is SecondSampleNavigationListPanelWidget) {
            secondSampleViewStyle?.let { view.setStyle(it) }
        }
    }

    @SuppressLint("Recycle")
    override fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SampleNavigationView).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.SampleNavigationView_uxsdk_basePanelStyle) {
                panelStyle = it
            }
            typedArray.getResourceIdAndUse(R.styleable.SampleNavigationView_uxsdk_sampleNavigationListPanelWidgetStyle) {
                sampleViewStyle = it
            }
            typedArray.getResourceIdAndUse(R.styleable.SampleNavigationView_uxsdk_secondSampleNavigationListPanelWidgetStyle) {
                secondSampleViewStyle = it
            }
        }
    }
}

//region First level list panel
class SampleNavigationListPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
                context,
                PanelWidgetType.LIST,
                showTitleBar = true,
                panelTitle = "First Level Panel",
                titleBarHeightDimensionResID = R.dimen.title_bar_size,
                hasCloseButton = true)
) : ListPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    init {
        smartListModel = SampleNavigationSmartListModel(context)
    }

    override fun onSmartListModelCreated() {
        //Do nothing
    }

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        //Do nothing
    }

    override fun reactToModelChanges() {
        //Do nothing
    }
}

class SampleNavigationSmartListModel(context: Context) : SmartListModel(context, null) {


    override val registeredWidgetIDList: List<WidgetID> = (1..1).map { "Item$it" }
    override val defaultActiveWidgetSet: Set<WidgetID> = registeredWidgetIDList.toSet()

    override fun inSetUp() {
        //Do nothing
    }

    override fun inCleanUp() {
        //Do nothing
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        //Do nothing
    }

    override fun onAircraftModelChanged(model: Model) {
        //Do nothing
    }

    override fun createWidget(widgetID: WidgetID): View {
        return NavigableSampleListItemWidget(context)
    }
}
//endregion

//region second level list panel
class SecondSampleNavigationListPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
                context,
                PanelWidgetType.LIST,
                showTitleBar = true,
                panelTitle = "Second Level Panel",
                titleBarHeightDimensionResID = R.dimen.title_bar_size,
                hasCloseButton = true)
) : ListPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    init {
        smartListModel = SecondSampleSmartListModel(context)
    }

    override fun onSmartListModelCreated() {
        //Do nothing
    }

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        //Do nothing
    }

    override fun reactToModelChanges() {
        //Do nothing
    }
}

class SecondSampleSmartListModel(context: Context) : SmartListModel(context, null) {


    override val registeredWidgetIDList: List<WidgetID> = (1..10).map { "Item$it" }
    override val defaultActiveWidgetSet: Set<WidgetID> = registeredWidgetIDList.toSet()

    override fun inSetUp() {
        //Do nothing
    }

    override fun inCleanUp() {
        //Do nothing
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        //Do nothing
    }

    override fun onAircraftModelChanged(model: Model) {
        //Do nothing
    }

    override fun createWidget(widgetID: WidgetID): View {
        val testListItemWidget = LabelSampleListItemWidget(context)
        testListItemWidget.listItemTitle = widgetID
        return testListItemWidget
    }
}
//endregion

//region ListItemWidgets
class NavigableSampleListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<Any>(context, attrs, defStyleAttr, WidgetType.LABEL, 0), Navigable {

    override var panelNavigator: PanelNavigator? = null

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_aircraft_battery_temperature)
        listItemTitle = "Navigable Label Item"
        setOnClickListener {
            val panel = SecondSampleNavigationListPanelWidget(context)
            panelNavigator?.push(panel)
        }
    }

    override fun onButtonClick() {
        //Do nothing
    }

    override fun reactToModelChanges() {
        //Do nothing
    }

    override fun getIdealDimensionRatioString(): String? = null
}

class LabelSampleListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<Any>(context, attrs, defStyleAttr, WidgetType.LABEL, 0) {

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_aircraft_battery_temperature)
        listItemTitle = "Example Label Item"
    }

    override fun onButtonClick() {
        //Do nothing
    }

    override fun reactToModelChanges() {
        //Do nothing
    }

    override fun getIdealDimensionRatioString(): String? = null
}
//endregion
