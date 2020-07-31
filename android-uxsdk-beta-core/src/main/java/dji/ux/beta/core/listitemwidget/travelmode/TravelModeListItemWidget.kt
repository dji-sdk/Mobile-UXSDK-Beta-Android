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

package dji.ux.beta.core.listitemwidget.travelmode

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
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
import dji.ux.beta.core.listitemwidget.travelmode.TravelModeListItemWidget.TravelModeItemDialogState.*
import dji.ux.beta.core.listitemwidget.travelmode.TravelModeListItemWidget.TravelModeListItemState.CurrentTravelModeListItemState
import dji.ux.beta.core.listitemwidget.travelmode.TravelModeListItemWidget.TravelModeListItemState.ProductConnected
import dji.ux.beta.core.listitemwidget.travelmode.TravelModeListItemWidgetModel.TravelModeState

/**
 * Travel Mode List Item
 *
 */
open class TravelModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<TravelModeListItemWidget.TravelModeListItemState>(context, attrs, defStyleAttr, WidgetType.BUTTON) {

    private val schedulerProvider = SchedulerProvider.getInstance()

    /**
     * Icon when landing gear is not in travel mode
     */
    var travelModeActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_travel_mode_active)

    /**
     * Icon when landing gear is not in travel mode
     */
    var travelModeInactiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_travel_mode_inactive)

    /**
     * Label for button when landing gear is in travel mode
     */
    var exitTravelModeButtonString: String = getString(R.string.uxsdk_travel_mode_exit)

    /**
     * Label for button when landing gear is not in travel mode
     */
    var enterTravelModeButtonString: String = getString(R.string.uxsdk_travel_mode_enter)

    /**
     * Icon for confirmation dialog
     */
    var confirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    /**
     * Icon for error dialog
     */
    var errorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Theme for the dialogs shown
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    private val widgetModel: TravelModeListItemWidgetModel by lazy {
        TravelModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider)
    }

    init {
        listItemTitleIcon = travelModeInactiveIcon
        listItemTitle = getString(R.string.uxsdk_list_item_travel_mode)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.TravelModeListItemWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_travel_mode_active_icon) {
                travelModeActiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_travel_mode_inactive_icon) {
                travelModeInactiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                confirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_error_dialog_icon) {
                errorDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
            }

            enterTravelModeButtonString = typedArray.getString(R.styleable.TravelModeListItemWidget_uxsdk_enter_travel_mode_button_string, enterTravelModeButtonString)
            exitTravelModeButtonString = typedArray.getString(R.styleable.TravelModeListItemWidget_uxsdk_exit_travel_mode_button_string, exitTravelModeButtonString)
            typedArray.getResourceIdAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.travelModeState
                .observeOn(schedulerProvider.ui())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })

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
        checkAndToggleTravelMode()
    }

    private fun checkAndToggleTravelMode() {
        addDisposable(widgetModel.travelModeState.firstOrError()
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    when (it) {
                        TravelModeState.Inactive -> {
                            showEnterTravelModeConfirmationDialog()
                        }
                        TravelModeState.Active -> {
                            exitTravelMode()
                        }
                    }
                }, { }))


    }

    private fun showEnterTravelModeConfirmationDialog() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                enterTravelMode()
                uiUpdateStateProcessor.onNext(DialogActionConfirm(EnterTravelModeConfirmation))
            }
            dialogInterface.dismiss()
            uiUpdateStateProcessor.onNext(DialogActionDismiss(EnterTravelModeConfirmation))
        }
        showConfirmationDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                icon = confirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_travel_mode_enter_confirmation),
                dialogClickListener = dialogListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(EnterTravelModeConfirmation))
    }

    private fun exitTravelMode() {
        addDisposable(widgetModel.exitTravelMode()
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        { },
                        { error ->
                            if (error is UXSDKError) {
                                showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                        icon = errorDialogIcon,
                                        dialogTheme = dialogTheme,
                                        message = String.format(getString(R.string.uxsdk_exit_travel_mode_failed),
                                                error.djiError.description))
                                uiUpdateStateProcessor.onNext(DialogDisplayed(ExitTravelModeError))
                            }
                        }
                ))

    }

    private fun enterTravelMode() {
        addDisposable(widgetModel.enterTravelMode()
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                    icon = successDialogIcon,
                                    dialogTheme = dialogTheme,
                                    message = getString(R.string.uxsdk_enter_travel_mode_success))
                            uiUpdateStateProcessor.onNext(DialogDisplayed(EnterTravelModeSuccess))
                        },
                        { error ->
                            if (error is UXSDKError) {
                                showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                        icon = errorDialogIcon,
                                        dialogTheme = dialogTheme,
                                        message = String.format(getString(R.string.uxsdk_enter_travel_mode_failed),
                                                error.djiError.description))
                                uiUpdateStateProcessor.onNext(DialogDisplayed(EnterTravelModeError))
                            }
                        }
                ))

    }


    private fun updateUI(travelModeState: TravelModeState) {
        widgetStateDataProcessor.onNext(CurrentTravelModeListItemState(travelModeState))
        when (travelModeState) {
            TravelModeState.ProductDisconnected,
            TravelModeState.NotSupported -> {
                isEnabled = false
                listItemTitleIcon = travelModeInactiveIcon
                listItemButtonText = getString(R.string.uxsdk_string_default_value)
            }
            TravelModeState.Active -> {
                isEnabled = true
                listItemTitleIcon = travelModeActiveIcon
                listItemButtonText = exitTravelModeButtonString
            }
            TravelModeState.Inactive -> {
                isEnabled = true
                listItemTitleIcon = travelModeInactiveIcon
                listItemButtonText = enterTravelModeButtonString
            }
        }

    }

    /**
     * Get the [TravelModeListItemState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<TravelModeListItemState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Get the [ListItemLabelButtonWidget.WidgetUIState] updates
     * The info parameter is instance of [TravelModeItemDialogState]
     */
    override fun getUIStateUpdates(): Flowable<WidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * Travel mode List Item Dialog Identifiers
     */
    sealed class TravelModeItemDialogState {
        /**
         * Dialog shown for confirmation to enter travel mode
         */
        object EnterTravelModeConfirmation : TravelModeItemDialogState()

        /**
         * Dialog shown when entering travel mode success
         */
        object EnterTravelModeSuccess : TravelModeItemDialogState()

        /**
         * Dialog shown when entering travel mode fails
         */
        object EnterTravelModeError : TravelModeItemDialogState()

        /**
         * Dialog shown when exiting travel mode fails
         */
        object ExitTravelModeError : TravelModeItemDialogState()

    }

    /**
     * Class defines widget state updates
     */
    sealed class TravelModeListItemState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : TravelModeListItemState()

        /**
         * Current travel mode state
         */
        data class CurrentTravelModeListItemState(val travelModeState: TravelModeState) : TravelModeListItemState()

    }

}
