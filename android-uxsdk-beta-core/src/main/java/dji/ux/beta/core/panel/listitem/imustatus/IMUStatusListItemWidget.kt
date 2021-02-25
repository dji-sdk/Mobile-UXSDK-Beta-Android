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

package dji.ux.beta.core.panel.listitem.imustatus

import android.content.Context
import android.util.AttributeSet
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.panel.listitem.imustatus.IMUStatusListItemWidgetModel.IMUState
import dji.ux.beta.core.panel.listitem.imustatus.IMUStatusListItemWidgetModel.IMUState.*

/**
 * List item shows the status of the IMU
 * Provides an option to start IMU calibration
 */
class IMUStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<Any>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL,
        R.style.UXSDKIMUListItem
) {

    //region Fields
    private val widgetModel by lazy {
        IMUStatusListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.imuItemState
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


    override fun onButtonClick() {
        // TODO not yet limited
    }
    //endregion

    //region Reactions to Model
    private fun updateUI(imuState: IMUState) {
        when (imuState) {
            ProductDisconnected -> updateDisconnectedState()
            Disabled -> updateDisabledState()
            Normal -> updateNormalState()
            is Warning -> updateWarningState(imuState)
            is Error -> updateErrorState(imuState)
        }
    }
    //endregion

    //region Helpers
    private fun updateErrorState(imuErrorState: Error) {
        isEnabled = true
        listItemLabel = imuErrorState.message
        listItemLabelTextColor = errorValueColor
    }

    private fun updateWarningState(imuWarningState: Warning) {
        isEnabled = true
        listItemLabel = imuWarningState.message
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
    //endregion

    //region Customizations
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)
    //endregion

}