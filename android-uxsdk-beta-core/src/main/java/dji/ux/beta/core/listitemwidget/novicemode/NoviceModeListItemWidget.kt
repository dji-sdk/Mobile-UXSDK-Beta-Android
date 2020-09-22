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

package dji.ux.beta.core.listitemwidget.novicemode

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.log.DJILog
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.ListItemSwitchWidget
import dji.ux.beta.core.base.widget.ListItemSwitchWidget.WidgetUIState.*
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget.NoviceModeItemDialogState.NoviceModeDisableConfirmation
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget.NoviceModeItemDialogState.NoviceModeEnableSuccess
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget.NoviceModeListItemState
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget.NoviceModeListItemState.CurrentNoviceModeListItemState
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidget.NoviceModeListItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.novicemode.NoviceModeListItemWidgetModel.NoviceModeState

private const val TAG = "NoviceModeListItemW"

/**
 * Widget shows the current status of the Novice Mode, also known
 * as Beginner Mode.
 * It also provides an option to switch between ON/OFF state
 */
open class NoviceModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemSwitchWidget<NoviceModeListItemState>(context, attrs, defStyleArr) {


    private val schedulerProvider = SchedulerProvider.getInstance()

    private val widgetModel: NoviceModeListItemWidgetModel by lazy {
        NoviceModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider)
    }

    /**
     * Icon for confirmation dialog
     */
    var confirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)


    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Theme for the dialogs shown in the widget
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    init {
        listItemTitleIcon = getDrawable(R.drawable.uxsdk_ic_system_status_list_novice_mode)
        listItemTitle = getString(R.string.uxsdk_list_item_novice_mode)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.NoviceModeListItemWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                confirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.noviceModeState
                .observeOn(schedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(CurrentNoviceModeListItemState(it))
                    updateUI(it)
                })
    }

    private fun updateUI(noviceModeState: NoviceModeState) {
        when (noviceModeState) {
            NoviceModeState.ProductDisconnected -> isEnabled = false
            NoviceModeState.Enabled -> {
                updateState(true)
            }
            NoviceModeState.Disabled -> {
                updateState(false)
            }
        }
    }

    private fun updateState(noviceModeEnabled: Boolean) {
        isEnabled = true
        setChecked(noviceModeEnabled)
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

    override fun onSwitchToggle(isChecked: Boolean) {
        if (isChecked) {
            toggleNoviceMode(isChecked)
        } else {
            showConfirmationDialog()
        }
    }

    private fun toggleNoviceMode(checked: Boolean) {
        addDisposable(widgetModel.toggleNoviceMode()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    if (checked) {
                        showEnabledDialog()
                    }
                    DJILog.d(TAG, " toggle success ")
                }, {
                    if (it is UXSDKError) {
                        DJILog.e(TAG, it.djiError.description)
                    }
                    resetSwitchState()
                }))
    }

    private fun showConfirmationDialog() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                toggleNoviceMode(false)
                uiUpdateStateProcessor.onNext(DialogActionConfirm(NoviceModeDisableConfirmation))
            } else {
                resetSwitchState()
            }
            dialogInterface.dismiss()
            uiUpdateStateProcessor.onNext(DialogActionDismiss(NoviceModeDisableConfirmation))
        }
        showConfirmationDialog(dialogTheme = dialogTheme,
                icon = confirmationDialogIcon,
                title = getString(R.string.uxsdk_list_item_novice_mode),
                message = getString(R.string.uxsdk_novice_mode_disabled_message),
                dialogClickListener = dialogListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(NoviceModeDisableConfirmation))
    }

    private fun showEnabledDialog() {
        showAlertDialog(dialogTheme = dialogTheme,
                icon = successDialogIcon,
                title = getString(R.string.uxsdk_list_item_novice_mode),
                message = getString(R.string.uxsdk_novice_mode_enabled_message))
        uiUpdateStateProcessor.onNext(DialogDisplayed(NoviceModeEnableSuccess))
    }

    private fun resetSwitchState() {
        addDisposable(widgetModel.noviceModeState.firstOrError()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    DJILog.e(TAG, it.message)
                }))
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    /**
     * Novice mode dialog identifiers
     */
    sealed class NoviceModeItemDialogState {
        /**
         * Dialog shown when novice mode is enabled successfully
         */
        object NoviceModeEnableSuccess : NoviceModeItemDialogState()

        /**
         * Dialog shown when switching from enabled to disabled
         */
        object NoviceModeDisableConfirmation : NoviceModeItemDialogState()
    }

    /**
     * Class defines widget state updates
     */
    sealed class NoviceModeListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : NoviceModeListItemState()

        /**
         * Current novice mode state
         */
        data class CurrentNoviceModeListItemState(val noviceModeState: NoviceModeState) : NoviceModeListItemState()

    }
}