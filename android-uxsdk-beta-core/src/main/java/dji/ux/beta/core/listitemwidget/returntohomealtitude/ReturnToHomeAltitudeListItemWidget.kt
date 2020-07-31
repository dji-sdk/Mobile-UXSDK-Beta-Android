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

package dji.ux.beta.core.listitemwidget.returntohomealtitude

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.log.DJILog
import dji.ux.beta.R
import dji.ux.beta.core.base.*
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemEditTextButtonWidget
import dji.ux.beta.core.base.widget.ListItemEditTextButtonWidget.WidgetUIState.DialogDisplayed
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ReturnToHomeAltitudeItemState
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ReturnToHomeAltitudeItemState.CurrentReturnToHomeAltitudeState
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ReturnToHomeAltitudeItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ReturnToHomeItemDialogState.MaxAltitudeExceededDialog
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState
import dji.ux.beta.core.listitemwidget.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.ReturnToHomeAltitudeValue
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.UnitConversionUtil.UnitType

private const val TAG = "RTHAltitudeListItem"

/**
 * Widget shows the current return to home altitude.
 * The widget enables the user to modify this value. Tap on the limit and enter
 * the new value to modify the return home altitude.
 * The return to home altitude cannot exceed the maximum flight altitude.
 */
open class ReturnToHomeAltitudeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<ReturnToHomeAltitudeItemState>(context, attrs, defStyleArr, WidgetType.EDIT) {

    private val schedulerProvider = SchedulerProvider.getInstance()

    /**
     * Enable/Disable toast messages in the widget
     */
    var toastMessagesEnabled: Boolean = true

    /**
     * Theme for the dialogs shown in the widget
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * Icon for error dialog
     */
    var errorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    private val widgetModel: ReturnToHomeAltitudeListItemWidgetModel by lazy {
        ReturnToHomeAltitudeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider,
                GlobalPreferencesManager.getInstance())
    }

    init {
        listItemTitle = getString(R.string.uxsdk_list_item_max_return_to_home_altitude)
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_return_home_altitude)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ReturnToHomeAltitudeListItemWidget).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getResourceIdAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_error_dialog_icon) {
                errorDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
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

    override fun reactToModelChanges() {
        addReaction(widgetModel.returnToHomeAltitudeState
                .observeOn(schedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(CurrentReturnToHomeAltitudeState(it))
                    this.updateUI(it)
                })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    private fun updateUI(returnToHomeAltitudeState: ReturnToHomeAltitudeState) {
        when (returnToHomeAltitudeState) {
            ReturnToHomeAltitudeState.ProductDisconnected -> updateProductDisconnectedState()
            is ReturnToHomeAltitudeState.NoviceMode -> updateNoviceMode(returnToHomeAltitudeState.unitType)
            is ReturnToHomeAltitudeValue -> updateReturnToHomeValue(returnToHomeAltitudeState)
        }
    }

    private fun updateReturnToHomeValue(returnToHomeAltitudeListItemState: ReturnToHomeAltitudeValue) {
        listItemEditTextVisibility = true
        listItemHintVisibility = true
        listItemHint = if (returnToHomeAltitudeListItemState.unitType == UnitType.METRIC) {
            String.format(getString(R.string.uxsdk_altitude_range_meters),
                    returnToHomeAltitudeListItemState.minLimit,
                    returnToHomeAltitudeListItemState.maxLimit)
        } else {
            String.format(getString(R.string.uxsdk_altitude_range_feet),
                    returnToHomeAltitudeListItemState.minLimit,
                    returnToHomeAltitudeListItemState.maxLimit)

        }
        listItemEditTextValue = returnToHomeAltitudeListItemState.returnToHomeAltitude.toString()
        isEnabled = true
        listItemEditTextColor = editTextNormalColor
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun updateNoviceMode(unitType: UnitType) {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = if (unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_novice_mode_altitude_meters)
        } else {
            getString(R.string.uxsdk_novice_mode_altitude_feet)
        }
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
    }

    private fun setReturnToHomeAltitude(currentValue: Int) {
        addDisposable(widgetModel.setReturnToHomeAltitude(currentValue)
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    showAlertDialog(dialogTheme = dialogTheme,
                            icon = successDialogIcon,
                            title = getString(R.string.uxsdk_list_rth_dialog_title),
                            message = getString(R.string.uxsdk_rth_success_dialog_message))
                    uiUpdateStateProcessor.onNext(DialogDisplayed(ReturnToHomeItemDialogState.ReturnHomeAltitudeChangeDialog))
                    widgetStateDataProcessor.onNext(ReturnToHomeAltitudeItemState.SetReturnToHomeAltitudeSuccess)
                }, { error ->
                    if (error is UXSDKError) {
                        showToast(error.djiError.description)
                        widgetStateDataProcessor.onNext(ReturnToHomeAltitudeItemState.SetReturnToHomeAltitudeFailed(error))
                        DJILog.e(TAG, error.djiError.description)
                    }

                }))
    }

    private fun resetToDefaultValue() {
        addDisposable(widgetModel.returnToHomeAltitudeState.firstOrError()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    DJILog.e(TAG, it.message)
                }))
    }

    override fun onButtonClick() {
        // Do nothing
    }

    override fun onKeyboardDoneAction() {
        val currentValue = listItemEditTextValue?.toIntOrNull()
        if (currentValue != null
                && widgetModel.isInputInRange(currentValue)) {
            addDisposable(widgetModel.returnToHomeAltitudeState.firstOrError()
                    .observeOn(schedulerProvider.ui())
                    .subscribe({
                        if (it is ReturnToHomeAltitudeValue) {
                            if (it.maxFlightAltitude < currentValue) {
                                showAlertDialog(dialogTheme = dialogTheme,
                                        icon = errorDialogIcon,
                                        title = getString(R.string.uxsdk_list_rth_dialog_title),
                                        message = getString(R.string.uxsdk_rth_error_dialog_message))
                                uiUpdateStateProcessor.onNext(DialogDisplayed(MaxAltitudeExceededDialog))
                                resetToDefaultValue()
                            } else {
                                setReturnToHomeAltitude(currentValue)
                            }
                        }
                    }, {
                        resetToDefaultValue()
                        DJILog.d(TAG, it.message)
                    }))

        } else {
            resetToDefaultValue()
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
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


    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)


    /**
     * Return to home list item dialog identifiers
     */
    sealed class ReturnToHomeItemDialogState {
        /**
         * Dialog shown when return to home altitude
         * exceeds max altitude limit
         */
        object MaxAltitudeExceededDialog : ReturnToHomeItemDialogState()

        /**
         * Dialog shown to warn user when return to home altitude is
         * updated successfully
         */
        object ReturnHomeAltitudeChangeDialog : ReturnToHomeItemDialogState()

    }

    /**
     * Class defines widget state updates
     */
    sealed class ReturnToHomeAltitudeItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ReturnToHomeAltitudeItemState()

        /**
         * Return to home altitude set action successful
         */
        object SetReturnToHomeAltitudeSuccess : ReturnToHomeAltitudeItemState()

        /**
         * Return to home altitude set action failed
         */
        data class SetReturnToHomeAltitudeFailed(val error: UXSDKError) : ReturnToHomeAltitudeItemState()

        /**
         * Current return to home altitude state
         */
        data class CurrentReturnToHomeAltitudeState(val maxAltitudeState: ReturnToHomeAltitudeState) : ReturnToHomeAltitudeItemState()
    }
}