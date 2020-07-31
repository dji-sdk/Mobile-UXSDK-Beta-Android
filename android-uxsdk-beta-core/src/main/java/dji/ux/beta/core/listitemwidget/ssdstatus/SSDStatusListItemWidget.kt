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

package dji.ux.beta.core.listitemwidget.ssdstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.camera.SSDOperationState
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget.WidgetUIState.DialogActionConfirm
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget.WidgetUIState.DialogActionDismiss
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidget.SSDListItemDialogState.*
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidget.SSDListItemState
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidget.SSDListItemState.CurrentSSDListItemState
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidget.SSDListItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.ssdstatus.SSDStatusListItemWidgetModel.SSDState
import dji.ux.beta.core.util.UnitConversionUtil

/**
 *  SSD status list item
 *
 *  It displays the remaining capacity of the SSD along with
 *  any warnings / errors related to the SSD.
 *  It provides a button to format SSD.
 */
open class SSDStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<SSDListItemState>(context, attrs, defStyleAttr, WidgetType.LABEL_BUTTON) {

    private val schedulerProvider = SchedulerProvider.getInstance()

    /**
     * Theme for the dialogs shown for format
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * Icon for the dialog which shows format confirmation message
     */
    var formatConfirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    /**
     * Icon for the dialog which shows format success message
     */
    var formatSuccessDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Icon for the dialog which shows format error message
     */
    var formatErrorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    private val widgetModel: SSDStatusListItemWidgetModel by lazy {
        SSDStatusListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider)
    }

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_ssd)
        listItemTitle = getString(R.string.uxsdk_list_item_ssd)
        listItemButtonText = getString(R.string.uxsdk_list_item_format_button)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SSDStatusListItemWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                formatConfirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_success_dialog_icon) {
                formatSuccessDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_error_dialog_icon) {
                formatErrorDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.ssdState
                .observeOn(schedulerProvider.ui())
                .subscribe { this.updateUI(it) })

    }

    private fun updateUI(ssdState: SSDState) {
        widgetStateDataProcessor.onNext(CurrentSSDListItemState(ssdState))
        when (ssdState) {
            SSDState.ProductDisconnected,
            SSDState.NotSupported -> updateDisabledState(ssdState)

            is SSDState.CurrentSSDState -> {
                isEnabled = true
                listItemLabel = getSSDMessage(ssdState.ssdOperationState,
                        ssdState.remainingSpace)
                listItemLabelTextColor = getSSDMessageColor(ssdState.ssdOperationState)
                listItemButtonEnabled = getFormatButtonVisibility(ssdState.ssdOperationState)
            }
        }
    }

    private fun getSSDMessage(ssdOperationState: SSDOperationState, remainingSpace: Int): String {
        return when (ssdOperationState) {
            SSDOperationState.NOT_FOUND -> getString(R.string.uxsdk_ssd_not_found)
            SSDOperationState.IDLE -> UnitConversionUtil.getSpaceWithUnit(context, remainingSpace)
            SSDOperationState.SAVING -> getString(R.string.uxsdk_ssd_saving)
            SSDOperationState.FORMATTING -> getString(R.string.uxsdk_ssd_formatting)
            SSDOperationState.INITIALIZING -> getString(R.string.uxsdk_ssd_initializing)
            SSDOperationState.ERROR -> getString(R.string.uxsdk_ssd_error)
            SSDOperationState.FULL -> getString(R.string.uxsdk_ssd_full)
            SSDOperationState.POOR_CONNECTION -> getString(R.string.uxsdk_ssd_poor_connection)
            SSDOperationState.SWITCHING_LICENSE -> getString(R.string.uxsdk_ssd_switching_license)
            SSDOperationState.FORMATTING_REQUIRED -> getString(R.string.uxsdk_ssd_formatting_required)
            SSDOperationState.NOT_INITIALIZED -> getString(R.string.uxsdk_ssd_not_initialized)
            SSDOperationState.INVALID_FILE_SYSTEM -> getString(R.string.uxsdk_ssd_formatting_required)
            SSDOperationState.UNKNOWN -> getString(R.string.uxsdk_string_default_value)
        }
    }

    private fun getSSDMessageColor(ssdOperationState: SSDOperationState): Int {
        return when (ssdOperationState) {
            SSDOperationState.IDLE -> normalValueColor
            SSDOperationState.ERROR -> errorValueColor
            else -> warningValueColor
        }
    }

    private fun getFormatButtonVisibility(ssdOperationState: SSDOperationState): Boolean {
        return when (ssdOperationState) {
            SSDOperationState.FORMATTING_REQUIRED,
            SSDOperationState.INVALID_FILE_SYSTEM,
            SSDOperationState.FULL,
            SSDOperationState.IDLE -> true
            else -> false
        }
    }

    private fun updateDisabledState(ssdState: SSDState) {
        listItemLabel = if (ssdState is SSDState.ProductDisconnected) {
            getString(R.string.uxsdk_string_default_value)
        } else {
            getString(R.string.uxsdk_storage_status_not_supported)
        }
        listItemLabelTextColor = if (ssdState is SSDState.ProductDisconnected) {
            disconnectedValueColor
        } else {
            errorValueColor
        }
        isEnabled = false
    }

    private fun formatSSD() {
        addDisposable(widgetModel.formatSSD()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    showAlertDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                            icon = formatSuccessDialogIcon,
                            dialogTheme = dialogTheme,
                            message = getString(R.string.uxsdk_ssd_format_complete))
                    uiUpdateStateProcessor.onNext(WidgetUIState.DialogDisplayed(FormatSuccessDialog))
                }, { error ->
                    if (error is UXSDKError) {
                        showAlertDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                                icon = formatErrorDialogIcon,
                                dialogTheme = dialogTheme,
                                message = String.format(getString(R.string.uxsdk_ssd_format_error),
                                        error.djiError.description))
                        uiUpdateStateProcessor.onNext(WidgetUIState.DialogDisplayed(FormatErrorDialog))
                    }
                }))
    }

    override fun onButtonClick() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                uiUpdateStateProcessor.onNext(DialogActionConfirm(FormatConfirmationDialog))
                formatSSD()
            }
            dialogInterface.dismiss()
            uiUpdateStateProcessor.onNext(DialogActionDismiss(FormatConfirmationDialog))
        }
        showConfirmationDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                icon = formatConfirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_ssd_format_confirmation),
                dialogClickListener = dialogListener)
        uiUpdateStateProcessor.onNext(WidgetUIState.DialogDisplayed(FormatConfirmationDialog))
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

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    /**
     * SSD List Item Dialog Identifiers
     */
    sealed class SSDListItemDialogState {
        /**
         * Dialog shown for format confirmation
         */
        object FormatConfirmationDialog : SSDListItemDialogState()

        /**
         * Dialog shown for format success
         */
        object FormatSuccessDialog : SSDListItemDialogState()

        /**
         * Dialog shown for format fail
         */
        object FormatErrorDialog : SSDListItemDialogState()
    }


    /**
     * Class defines widget state updates
     */
    sealed class SSDListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : SSDListItemState()

        /**
         * Current SSD List Item State
         */
        data class CurrentSSDListItemState(val ssdState: SSDState) : SSDListItemState()
    }


}