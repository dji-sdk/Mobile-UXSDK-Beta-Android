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

package dji.ux.beta.core.panel.listitem.visionsensorsstatus

import android.content.Context
import android.util.AttributeSet
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.panel.listitem.visionsensorsstatus.VisionSensorsStatusListItemWidgetModel.VisionSensorListItemState

/**
 * List item shows the status of the vision sensors
 */
open class VisionSensorsStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<Any>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL,
        R.style.UXSDKVisionSensorsListItem
) {

    private val widgetModel by lazy {
        VisionSensorsStatusListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.visionSensorItemState
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateUI(it) })
    }

    private fun updateUI(visionSensorListItemState: VisionSensorListItemState) {
        when (visionSensorListItemState) {
            VisionSensorListItemState.ProductDisconnected -> updateDisconnectedState()
            VisionSensorListItemState.Disabled -> updateDisabledState()
            VisionSensorListItemState.Normal -> updateNormalState()
            is VisionSensorListItemState.Warning -> updateWarningState(visionSensorListItemState)
            is VisionSensorListItemState.Error -> updateErrorState(visionSensorListItemState)
        }
    }

    private fun updateErrorState(visionSensorListItemState: VisionSensorListItemState.Error) {
        isEnabled = true
        listItemLabel = visionSensorListItemState.message
        listItemLabelTextColor = errorValueColor
    }

    private fun updateWarningState(visionSensorListItemState: VisionSensorListItemState.Warning) {
        isEnabled = true
        listItemLabel = visionSensorListItemState.message
        listItemLabelTextColor = warningValueColor
    }

    private fun updateNormalState() {
        isEnabled = true
        listItemLabel = getString(R.string.uxsdk_system_status_normal)
        listItemLabelTextColor = normalValueColor
    }

    private fun updateDisabledState() {
        isEnabled = true
        listItemLabel = getString(R.string.uxsdk_system_status_disabled)
        listItemLabelTextColor = errorValueColor
    }

    private fun updateDisconnectedState() {
        listItemLabel = getString(R.string.uxsdk_string_default_value)
        isEnabled = false
        listItemLabelTextColor = disconnectedValueColor
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

    override fun onButtonClick() {
        //Do nothing
    }
}