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

package dji.ux.beta.core.listitemwidget.maxflightdistance

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.log.DJILog
import dji.ux.beta.R
import dji.ux.beta.core.base.*
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemEditTextButtonWidget
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidget.MaxFlightDistanceItemState
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidget.MaxFlightDistanceItemState.CurrentMaxFlightDistanceState
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidget.MaxFlightDistanceItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState
import dji.ux.beta.core.listitemwidget.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState.MaxFlightDistanceValue
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.UnitConversionUtil.UnitType

private const val TAG = "MaxFlightDistanceItem"

/**
 * Widget shows the current flight distance limit.
 * Based on the product connected and the mode that it currently is in,
 * the widget will allow the user to update the flight distance limit.
 * Tap on the limit and enter the new value to modify the flight distance  limit.
 */
open class MaxFlightDistanceListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<MaxFlightDistanceItemState>(context, attrs, defStyleArr, WidgetType.EDIT_BUTTON) {

    private val schedulerProvider = SchedulerProvider.getInstance()

    private val widgetModel: MaxFlightDistanceListItemWidgetModel by lazy {
        MaxFlightDistanceListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider,
                GlobalPreferencesManager.getInstance())
    }

    /**
     * String for enable action button
     */
    var enableActionButtonString: String = getString(R.string.uxsdk_enable)

    /**
     * String for disable action button
     */
    var disableActionButtonString: String = getString(R.string.uxsdk_disable)

    /**
     * Enable/Disable toast messages in the widget
     */
    var toastMessagesEnabled: Boolean = true

    init {
        listItemTitle = getString(R.string.uxsdk_list_item_max_flight_distance)
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_max_flight_distance)
        attrs?.let { initAttributes(context, it) }

    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.MaxFlightDistanceListItemWidget).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getStringAndUse(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_enable_action_button_string) {
                enableActionButtonString = it
            }
            typedArray.getStringAndUse(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_disable_action_button_string) {
                disableActionButtonString = it
            }
        }
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
        addDisposable(widgetModel.toggleFlightDistanceAvailability()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                }, { error ->
                    if (error is UXSDKError) {
                        showToast(error.djiError.description)
                        DJILog.e(TAG, error.djiError.description)
                    }
                }))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.maxFlightDistanceState
                .observeOn(schedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(CurrentMaxFlightDistanceState(it))
                    updateUI(it)
                })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    private fun updateUI(maxFlightDistanceState: MaxFlightDistanceState) {
        when (maxFlightDistanceState) {
            MaxFlightDistanceState.ProductDisconnected -> updateProductDisconnectedState()
            MaxFlightDistanceState.Disabled -> updateDisabledState()
            is MaxFlightDistanceState.NoviceMode -> updateNoviceMode(maxFlightDistanceState.unitType)
            is MaxFlightDistanceValue -> updateMaxFlightDistance(maxFlightDistanceState)
        }
    }

    private fun updateMaxFlightDistance(maxFlightDistanceState: MaxFlightDistanceValue) {
        isEnabled = true
        listItemHintVisibility = true
        listItemEditTextVisibility = true
        listItemButtonVisibility = true
        listItemHint = if (maxFlightDistanceState.unitType == UnitType.METRIC) {
            String.format(getString(R.string.uxsdk_altitude_range_meters),
                    maxFlightDistanceState.minDistanceLimit,
                    maxFlightDistanceState.maxDistanceLimit)
        } else {
            String.format(getString(R.string.uxsdk_altitude_range_feet),
                    maxFlightDistanceState.minDistanceLimit,
                    maxFlightDistanceState.maxDistanceLimit)
        }
        listItemEditTextValue = maxFlightDistanceState.flightDistanceLimit.toString()
        listItemEditTextColor = editTextNormalColor
        listItemButtonText = disableActionButtonString
    }

    private fun updateNoviceMode(unitType: UnitType) {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemButtonVisibility = false
        listItemEditTextValue = if (unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_novice_mode_distance_meters)
        } else {
            getString(R.string.uxsdk_novice_mode_distance_feet)
        }
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun updateDisabledState() {
        isEnabled = true
        listItemHintVisibility = false
        listItemEditTextVisibility = false
        listItemButtonVisibility = true
        listItemButtonText = enableActionButtonString
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemButtonVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        listItemEditTextColor = disconnectedValueColor
        listItemButtonText = getString(R.string.uxsdk_string_default_value)
        isEnabled = false

    }

    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
    }

    private fun resetToDefaultValue() {
        addDisposable(widgetModel.maxFlightDistanceState.firstOrError()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    DJILog.e(TAG, it.message)
                }))
    }

    private fun setMaxFlightDistance(maxFlightDistance: Int) {
        addDisposable(widgetModel.setMaxFlightDistance(maxFlightDistance)
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    showToast(getString(R.string.uxsdk_success))
                    widgetStateDataProcessor.onNext(MaxFlightDistanceItemState.SetMaxFlightDistanceSuccess)
                }, {
                    if (it is UXSDKError) {
                        showToast(it.djiError.description)
                        DJILog.e(TAG, it.djiError.description)
                        widgetStateDataProcessor.onNext(MaxFlightDistanceItemState.SetMaxFlightDistanceFailed(it))
                    }
                    resetToDefaultValue()
                }))
    }


    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    override fun onKeyboardDoneAction() {
        val currentValue = listItemEditTextValue?.toIntOrNull()
        if (currentValue == null || !widgetModel.isInputInRange(currentValue)) {
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
            resetToDefaultValue()
        } else {
            setMaxFlightDistance(currentValue)
        }
    }

    override fun onEditorTextChanged(currentText: String?) {
        listItemEditTextColor = if (!currentText.isNullOrBlank()
                && currentText.toIntOrNull() != null
                && widgetModel.isInputInRange(currentText.toInt())) {
            editTextNormalColor
        } else {
            errorValueColor
        }
    }

    /**
     * Class defines widget state updates
     */
    sealed class MaxFlightDistanceItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : MaxFlightDistanceItemState()

        /**
         * Max flight distance set action successful
         */
        object SetMaxFlightDistanceSuccess : MaxFlightDistanceItemState()

        /**
         * Max flight distance set action failed
         */
        data class SetMaxFlightDistanceFailed(val error: UXSDKError) : MaxFlightDistanceItemState()

        /**
         * Current max flight distance state
         */
        data class CurrentMaxFlightDistanceState(val maxFlightDistanceState: MaxFlightDistanceState) : MaxFlightDistanceItemState()
    }

}