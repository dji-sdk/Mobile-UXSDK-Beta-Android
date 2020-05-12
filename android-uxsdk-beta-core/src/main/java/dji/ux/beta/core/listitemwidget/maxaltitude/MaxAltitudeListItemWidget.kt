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

package dji.ux.beta.core.listitemwidget.maxaltitude

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.log.DJILog
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.R
import dji.ux.beta.core.base.*
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemEditTextButtonWidget
import dji.ux.beta.core.base.widget.ListItemEditTextButtonWidget.WidgetUIState.*
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget.MaxAltitudeItemDialogState.*
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget.MaxAltitudeItemState
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget.MaxAltitudeItemState.CurrentMaxAltitudeState
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidget.MaxAltitudeItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.maxaltitude.MaxAltitudeListItemWidgetModel.MaxAltitudeState
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.UnitConversionUtil.*
import kotlin.math.roundToInt

private const val TAG = "MaxAltitudeListItemWidget"
private const val ALARM_LIMIT_METRIC = 120
private const val ALARM_LIMIT_IMPERIAL = 400

/**
 * Widget shows the current flight height limit.
 * Based on the product connected and the mode that it currently is in,
 * the widget will allow the user to update the flight height limit.
 * Tap on the limit and enter the new value to modify the flight height limit.
 */
open class MaxAltitudeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<MaxAltitudeItemState>(context, attrs, defStyleArr, WidgetType.EDIT) {

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

    init {
        listItemTitle = getString(R.string.uxsdk_list_item_max_flight_altitude)
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_max_flight_altitude)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.MaxAltitudeListItemWidget).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.MaxAltitudeListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getResourceIdAndUse(R.styleable.MaxAltitudeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    private val widgetModel: MaxAltitudeListItemWidgetModel by lazy {
        MaxAltitudeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider,
                GlobalPreferencesManager.getInstance())
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
        // No code required
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.maxAltitudeState
                .observeOn(schedulerProvider.ui())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    private fun updateUI(maxAltitudeState: MaxAltitudeState) {
        widgetStateDataProcessor.onNext(CurrentMaxAltitudeState(maxAltitudeState))
        when (maxAltitudeState) {
            MaxAltitudeState.ProductDisconnected -> updateProductDisconnectedState()
            is MaxAltitudeState.NoviceMode -> updateNoviceMode(maxAltitudeState.unitType)
            is MaxAltitudeState.MaxAltitudeValue -> updateMaxAltitudeValue(maxAltitudeState)
        }
    }

    private fun updateMaxAltitudeValue(maxAltitudeListItemState: MaxAltitudeState.MaxAltitudeValue) {
        listItemEditTextVisibility = true
        listItemHintVisibility = true
        listItemHintTextSize = getDimension(R.dimen.uxsdk_list_item_hint_text_size)
        listItemHint = if (maxAltitudeListItemState.unitType == UnitType.METRIC) {
            String.format(getString(R.string.uxsdk_altitude_range_meters),
                    maxAltitudeListItemState.minAltitudeLimit,
                    maxAltitudeListItemState.maxAltitudeLimit)
        } else {
            String.format(getString(R.string.uxsdk_altitude_range_feet),
                    maxAltitudeListItemState.minAltitudeLimit,
                    maxAltitudeListItemState.maxAltitudeLimit)

        }
        listItemEditTextValue = maxAltitudeListItemState.altitudeLimit.toString()
        isEnabled = true
        listItemEditTextColor = editTextNormalColor
    }


    private fun updateNoviceMode(unitType: UnitType) {
        listItemEditTextVisibility = false
        listItemHintVisibility = true
        listItemHintTextSize = DisplayUtil.pxToSp(context, listItemEditTextSize)
        listItemHint = if (unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_novice_mode_altitude_meters)
        } else {
            getString(R.string.uxsdk_novice_mode_altitude_feet)
        }
        isEnabled = false
        listItemEditTextColor = editTextNormalColor
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
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
        if (currentValue != null
                && widgetModel.isInputInRange(currentValue)) {
            addDisposable(widgetModel.maxAltitudeState.firstOrError()
                    .observeOn(schedulerProvider.ui())
                    .subscribe({
                        if (it is MaxAltitudeState.MaxAltitudeValue) {
                            when {
                                it.needFlightLimit && isOverAlarmLimit(currentValue, it.unitType) -> {
                                    showAlertDialog(dialogTheme = dialogTheme,
                                            title = getString(R.string.uxsdk_tips),
                                            icon = getDrawable(R.drawable.uxsdk_ic_alert_yellow),
                                            message = getString(R.string.uxsdk_limit_required_error))
                                    uiUpdateStateProcessor.onNext(DialogDisplayed(FlightLimitNeededErrorDialog))
                                }
                                isOverAlarmLimit(currentValue, it.unitType) -> {
                                    showOverAlarmLimitDialog(currentValue, it.returnToHomeHeight, it.unitType)
                                }
                                else -> {
                                    verifyReturnHomeAltitudeValue(currentValue, it.returnToHomeHeight, it.unitType)
                                }
                            }
                        }
                    }, {
                        DJILog.d(TAG, it.message)
                    }))

        } else {
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
        }

    }

    private fun showOverAlarmLimitDialog(currentValue: Int, currentReturnToHomeValue: Int, unitType: UnitType) {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                verifyReturnHomeAltitudeValue(currentValue, currentReturnToHomeValue, unitType)
                uiUpdateStateProcessor.onNext(DialogActionConfirm(MaxAltitudeOverAlarmConfirmationDialog))
            }
            dialogInterface.dismiss()
            uiUpdateStateProcessor.onNext(DialogActionDismiss(MaxAltitudeOverAlarmConfirmationDialog))

        }
        showConfirmationDialog(dialogTheme = dialogTheme,
                title = getString(R.string.uxsdk_tips),
                icon = getDrawable(R.drawable.uxsdk_ic_alert_yellow),
                message = getString(R.string.uxsdk_limit_high_notice),
                dialogClickListener = dialogListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(MaxAltitudeOverAlarmConfirmationDialog))

    }

    private fun isOverAlarmLimit(currentVal: Int, unitType: UnitType): Boolean {
        return if (unitType == UnitType.METRIC) {
            currentVal > ALARM_LIMIT_METRIC
        } else {
            currentVal > ALARM_LIMIT_IMPERIAL
        }
    }

    private fun verifyReturnHomeAltitudeValue(currentValue: Int, currentReturnToHomeValue: Int, unitType: UnitType) {
        if (currentValue < currentReturnToHomeValue) {
            val metricHeight: Int = if (unitType == UnitType.METRIC) {
                currentValue
            } else {
                convertFeetToMeters(currentValue.toFloat()).roundToInt()
            }
            val imperialHeight: Int = if (unitType == UnitType.METRIC) {
                convertMetersToFeet(currentValue.toFloat()).roundToInt()
            } else {
                currentValue
            }

            val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
                if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                    setMaxAltitudeValue(currentValue)
                    uiUpdateStateProcessor.onNext(DialogActionConfirm(ReturnHomeAltitudeUpdateDialog))
                }
                dialogInterface.dismiss()
                uiUpdateStateProcessor.onNext(DialogActionDismiss(ReturnHomeAltitudeUpdateDialog))

            }
            showConfirmationDialog(dialogTheme = dialogTheme,
                    title = null,
                    message = String.format(getString(R.string.uxsdk_limit_return_home_warning),
                            imperialHeight, metricHeight),
                    dialogClickListener = dialogListener)
            uiUpdateStateProcessor.onNext(DialogDisplayed(ReturnHomeAltitudeUpdateDialog))

        } else {
            setMaxAltitudeValue(currentValue)
        }

    }

    private fun setMaxAltitudeValue(currentValue: Int) {
        addDisposable(widgetModel.setFlightMaxAltitude(currentValue)
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    showToast(getString(R.string.uxsdk_success))
                    widgetStateDataProcessor.onNext(MaxAltitudeItemState.SetMaxAltitudeSuccess)
                }, { error ->
                    if (error is UXSDKError) {
                        showToast(error.djiError.description)
                        widgetStateDataProcessor.onNext(MaxAltitudeItemState.SetMaxAltitudeFailed(error))
                    }
                }))
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
     * Get the [MaxAltitudeItemState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<MaxAltitudeItemState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Get the [ListItemEditTextButtonWidget.WidgetUIState] updates
     * The info parameter is instance of [MaxAltitudeItemDialogState]
     */
    override fun getUIStateUpdates(): Flowable<WidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * Max altitude List Item Dialog Identifiers
     */
    sealed class MaxAltitudeItemDialogState {
        /**
         * Dialog shown when max altitude is over alarm
         * levels.
         */
        object MaxAltitudeOverAlarmConfirmationDialog : MaxAltitudeItemDialogState()

        /**
         * Dialog shown when flight limit is restricted and the user
         * tries to set a higher value
         */
        object FlightLimitNeededErrorDialog : MaxAltitudeItemDialogState()

        /**
         * Dialog shown to confirm that the user will have to update return home
         * altitude along with max flight limit
         */
        object ReturnHomeAltitudeUpdateDialog : MaxAltitudeItemDialogState()

    }

    /**
     * Class defines widget state updates
     */
    sealed class MaxAltitudeItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : MaxAltitudeItemState()

        /**
         * Max altitude set action successful
         */
        object SetMaxAltitudeSuccess : MaxAltitudeItemState()

        /**
         * Max altitude set action failed
         */
        data class SetMaxAltitudeFailed(val error: UXSDKError) : MaxAltitudeItemState()

        /**
         * Current max altitude state
         */
        data class CurrentMaxAltitudeState(val maxAltitudeState: MaxAltitudeState) : MaxAltitudeItemState()
    }

}