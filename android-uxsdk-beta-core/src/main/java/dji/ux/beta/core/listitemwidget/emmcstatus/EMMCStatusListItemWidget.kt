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

package dji.ux.beta.core.listitemwidget.emmcstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.SDCardOperationState
import dji.common.camera.SettingsDefinitions.SDCardOperationState.*
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget
import dji.ux.beta.core.base.widget.ListItemLabelButtonWidget.WidgetUIState.*
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.emmcstatus.EMMCStatusListItemWidget.EMMCListItemDialogState.*
import dji.ux.beta.core.listitemwidget.emmcstatus.EMMCStatusListItemWidget.EMMCListItemState
import dji.ux.beta.core.listitemwidget.emmcstatus.EMMCStatusListItemWidget.EMMCListItemState.ProductConnected
import dji.ux.beta.core.util.UnitConversionUtil.getSpaceWithUnit


/**
 *  eMMC status list item
 *
 *  It displays the remaining capacity of the eMMC (internal storage) along with
 *  any warnings / errors related to the eMMC.
 *  It provides a button to format eMMC.
 */
open class EMMCStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<EMMCListItemState>(context, attrs, defStyleAttr, WidgetType.LABEL_BUTTON) {

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

    private val widgetModel: EMMCStatusListItemWidgetModel by lazy {
        EMMCStatusListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider)
    }

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_emmc)
        listItemTitle = getString(R.string.uxsdk_list_item_emmc)
        listItemButtonText = getString(R.string.uxsdk_list_item_format_button)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.EMMCStatusListItemWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                formatConfirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_success_dialog_icon) {
                formatSuccessDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_error_dialog_icon) {
                formatErrorDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    override fun onButtonClick() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                uiUpdateStateProcessor.onNext(DialogActionConfirm(FormatConfirmationDialog))
                formatEMMC()
            }
            dialogInterface.dismiss()
            uiUpdateStateProcessor.onNext(DialogActionDismiss(FormatConfirmationDialog))
        }
        showConfirmationDialog(title = getString(R.string.uxsdk_emmc_dialog_title),
                icon = formatConfirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_emmc_format_confirmation),
                dialogClickListener = dialogListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(FormatConfirmationDialog))
    }

    private fun formatEMMC() {
        addDisposable(widgetModel.formatEMMC()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    showAlertDialog(title = getString(R.string.uxsdk_emmc_dialog_title),
                            icon = formatSuccessDialogIcon,
                            dialogTheme = dialogTheme,
                            message = getString(R.string.uxsdk_emmc_format_complete))
                    uiUpdateStateProcessor.onNext(DialogDisplayed(FormatSuccessDialog))
                }, { error ->
                    if (error is UXSDKError) {
                        showAlertDialog(title = getString(R.string.uxsdk_emmc_dialog_title),
                                icon = formatErrorDialogIcon,
                                dialogTheme = dialogTheme,
                                message = String.format(getString(R.string.uxsdk_emmc_format_error),
                                        error.djiError.description))
                        uiUpdateStateProcessor.onNext(DialogDisplayed(FormatErrorDialog))
                    }
                }))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.eMMCState
                .observeOn(schedulerProvider.ui())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }


    private fun updateUI(eMMCState: EMMCStatusListItemWidgetModel.EMMCState) {
        widgetStateDataProcessor.onNext(EMMCListItemState.CurrentEMMCListItemState(eMMCState))
        when (eMMCState) {
            EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected,
            EMMCStatusListItemWidgetModel.EMMCState.NotSupported -> updateDisabledState(eMMCState)
            is EMMCStatusListItemWidgetModel.EMMCState.CurrentEMMCState -> {
                isEnabled = true
                listItemLabel = getEMMCMessage(eMMCState.eMMCOperationState,
                        eMMCState.remainingSpace)
                listItemLabelTextColor = getEMMCMessageColor(eMMCState.eMMCOperationState)
                listItemButtonEnabled = getFormatButtonVisibility(eMMCState.eMMCOperationState)
            }
        }
    }

    private fun updateDisabledState(eMMCState: EMMCStatusListItemWidgetModel.EMMCState) {
        listItemLabel = if (eMMCState is EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected) {
            getString(R.string.uxsdk_string_default_value)
        } else {
            getString(R.string.uxsdk_storage_status_not_supported)
        }
        listItemLabelTextColor = if (eMMCState is EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected) {
            disconnectedValueColor
        } else {
            errorValueColor
        }
        isEnabled = false
    }


    private fun getFormatButtonVisibility(eMMCOperationState: SDCardOperationState): Boolean {
        return when (eMMCOperationState) {
            NORMAL,
            FORMAT_RECOMMENDED,
            FULL,
            SLOW,
            WRITING_SLOWLY,
            INVALID_FILE_SYSTEM,
            FORMAT_NEEDED -> true
            else -> false
        }
    }

    private fun getEMMCMessageColor(eMMCOperationState: SDCardOperationState): Int {
        return when (eMMCOperationState) {
            NORMAL -> normalValueColor
            else -> warningValueColor
        }
    }

    private fun getEMMCMessage(eMMCOperationState: SDCardOperationState,
                               space: Int): String? {
        return when (eMMCOperationState) {
            NORMAL -> getSpaceWithUnit(context, space)
            NOT_INSERTED -> getString(R.string.uxsdk_storage_status_missing)
            INVALID -> getString(R.string.uxsdk_storage_status_invalid)
            READ_ONLY -> getString(R.string.uxsdk_storage_status_write_protect)
            INVALID_FILE_SYSTEM,
            FORMAT_NEEDED -> getString(R.string.uxsdk_storage_status_needs_formatting)
            FORMATTING -> getString(R.string.uxsdk_storage_status_formatting)
            BUSY -> getString(R.string.uxsdk_storage_status_busy)
            FULL -> getString(R.string.uxsdk_storage_status_full)
            SLOW -> String.format(getString(R.string.uxsdk_storage_status_slow), getSpaceWithUnit(context, space))
            NO_REMAIN_FILE_INDICES -> getString(R.string.uxsdk_storage_status_file_indices)
            INITIALIZING -> getString(R.string.uxsdk_storage_status_initial)
            FORMAT_RECOMMENDED -> getString(R.string.uxsdk_storage_status_formatting_recommended)
            RECOVERING_FILES -> getString(R.string.uxsdk_storage_status_recover_file)
            WRITING_SLOWLY -> getString(R.string.uxsdk_storage_status_write_slow)
            USB_CONNECTED -> getString(R.string.uxsdk_storage_status_usb_connected)
            UNKNOWN_ERROR -> getString(R.string.uxsdk_storage_status_unknown_error)
            UNKNOWN -> getString(R.string.uxsdk_string_default_value)
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

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    /**
     * Get the [EMMCListItemState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<EMMCListItemState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Get the [ListItemLabelButtonWidget.WidgetUIState] updates
     * The info parameter is instance of [EMMCListItemDialogState]
     */
    override fun getUIStateUpdates(): Flowable<WidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * eMMC List Item Dialog Identifiers
     */
    sealed class EMMCListItemDialogState {
        /**
         * Dialog shown for format confirmation
         */
        object FormatConfirmationDialog : EMMCListItemDialogState()

        /**
         * Dialog shown for format success
         */
        object FormatSuccessDialog : EMMCListItemDialogState()

        /**
         * Dialog shown for format fail
         */
        object FormatErrorDialog : EMMCListItemDialogState()
    }

    /**
     * Class defines widget state updates
     */
    sealed class EMMCListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : EMMCListItemState()

        /**
         * Current eMMC List Item State
         */
        data class CurrentEMMCListItemState(val eMMCState: EMMCStatusListItemWidgetModel.EMMCState) : EMMCListItemState()
    }

}