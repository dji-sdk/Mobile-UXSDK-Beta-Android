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

package dji.ux.beta.flight.widget.apas

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.functions.Action
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.flight.R
import dji.ux.beta.flight.widget.apas.APASWidget.*
import dji.ux.beta.flight.widget.apas.APASWidget.DialogType.*
import dji.ux.beta.flight.widget.apas.APASWidgetModel.APASState

private const val TAG = "APASWidget"

/**
 * The APAS Widget displays the state of Advanced Pilot Assistance System.
 *
 * When APAS is enabled, the aircraft continues to respond to user commands and plans
 * its path according to both control stick inputs and the flight environment.
 * APAS makes it easier to avoid obstacles and obtain smoother footage, by planning a
 * path around obstacles if possible, when enabled.
 *
 * The widget is visible only when APAS is supported and the product is connected.
 * The widget has the following states
 *
 * Disabled - APAS has been disabled.
 * Tapping on the widget while the aircraft is on ground will enable the system but it will not be
 * active until you take off.
 * Tapping on the widget while the aircraft is flying will make the system enabled and active.
 *
 * Enabled - APAS is enabled but not active.
 * System will switch to active state when aircraft takes off.
 * System will switch to inactive state when aircraft lands.
 * Tapping on the widget in this state will disable the system.
 *
 * Active - APAS is enabled and currently active.
 * Tapping on the widget in this state will disable the system.
 */
open class APASWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private val widgetModel: APASWidgetModel by lazy {
        APASWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance()
        )
    }

    private var safetyWarningDialogShown: Boolean = false

    /**
     * APAS disabled icon
     */
    var apasDisabledIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_apas_disabled)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * APAS enabled icon
     */
    var apasEnabledIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_apas_enabled)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * APAS active icon
     */
    var apasActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_apas_active)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * APAS disabled icon tint color
     */
    @ColorInt
    var apasDisabledIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * APAS enabled icon tint color
     */
    @ColorInt
    var apasEnabledIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * APAS active icon tint color
     */
    @ColorInt
    var apasActiveIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Theme for the safety warning dialog
     */
    @StyleRes
    var safetyWarningDialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * Icon for the safety warning dialog
     */
    var safetyWarningDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    //endregion

    //region Lifecycle
    init {
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.apasState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        if (isEnabled) {
            checkAndToggleAPAS()
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

    override fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.apasState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) }, { logErrorConsumer(TAG, "Update UI ") }))
        }
    }
    //endregion

    //region private methods
    private fun updateUI(apasState: APASState) {
        widgetStateDataProcessor.onNext(ModelState.APASStateUpdated(apasState))
        //TODO: Note - the toasts below should be removed and shown as tips once the tips widget is created
        when (apasState) {
            APASState.ProductDisconnected, APASState.NotSupported -> {
                isEnabled = false
                foregroundImageView.setImageDrawable(apasDisabledIcon)
                foregroundImageView.updateColorFilter(getDisconnectedStateIconColor())
                hide()
            }
            APASState.Disabled -> setUI(apasDisabledIcon, apasDisabledIconTintColor)
            APASState.EnabledButFlightDistanceLimitReached -> {
                setUI(apasEnabledIcon, apasEnabledIconTintColor)
                showShortToast(R.string.uxsdk_apas_widget_flight_distance_limit_reached)
            }
            APASState.EnabledWithTemporaryError -> {
                setUI(apasEnabledIcon, apasEnabledIconTintColor)
                showShortToast(R.string.uxsdk_apas_widget_temp_disabled)
            }
            APASState.Active -> {
                setUI(apasActiveIcon, apasActiveIconTintColor)
                showShortToast(R.string.uxsdk_apas_widget_active)
            }
        }
    }

    private fun setUI(icon: Drawable?, iconColor: Int?) {
        show()
        isEnabled = true
        foregroundImageView.setImageDrawable(icon)
        foregroundImageView.updateColorFilter(iconColor)
    }

    private fun checkAndToggleAPAS() {
        addDisposable(widgetModel.apasState.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    when (it) {
                        APASState.Disabled -> {
                            if (!safetyWarningDialogShown) {
                                val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
                                    if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                                        uiUpdateStateProcessor.onNext(UIState.DialogActionConfirmed(SafetyWarning))
                                        toggleAPASState()
                                        safetyWarningDialogShown = true
                                    } else {
                                        uiUpdateStateProcessor.onNext(UIState.DialogActionCancelled(SafetyWarning))
                                    }
                                    dialogInterface.dismiss()
                                }
                                val dialogDismissListener = DialogInterface.OnDismissListener {
                                    uiUpdateStateProcessor.onNext(UIState.DialogDismissed(SafetyWarning))
                                }
                                showConfirmationDialog(title = getString(R.string.uxsdk_apas_safety_warning_dialog_title),
                                        icon = safetyWarningDialogIcon,
                                        dialogTheme = safetyWarningDialogTheme,
                                        message = getString(R.string.uxsdk_apas_safety_warning_dialog_message),
                                        dialogClickListener = dialogListener,
                                        dialogDismissListener = dialogDismissListener)
                                uiUpdateStateProcessor.onNext(UIState.DialogDisplayed(SafetyWarning))
                            } else {
                                toggleAPASState()
                            }
                        }
                        else -> toggleAPASState()
                    }
                }, {})
        )
    }

    private fun toggleAPASState() {
        addDisposable(widgetModel.toggleAPAS()
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action {}, logErrorConsumer(TAG, "toggleAPAS: "))
        )
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.APASWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.APASWidget_uxsdk_apasDisabledDrawable) {
                apasDisabledIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.APASWidget_uxsdk_apasEnabledDrawable) {
                apasEnabledIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.APASWidget_uxsdk_apasActiveDrawable) {
                apasActiveIcon = it
            }

            typedArray.getColorAndUse(R.styleable.APASWidget_uxsdk_apasDisabledIconColor) {
                apasDisabledIconTintColor = it
            }
            typedArray.getColorAndUse(R.styleable.APASWidget_uxsdk_apasEnabledIconColor) {
                apasEnabledIconTintColor = it
            }
            typedArray.getColorAndUse(R.styleable.APASWidget_uxsdk_apasActiveIconColor) {
                apasActiveIconTintColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.APASWidget_uxsdk_safetyWarningDialogIcon) {
                safetyWarningDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.APASWidget_uxsdk_safetyWarningDialogTheme) {
                safetyWarningDialogTheme = it
            }
        }
    }
    //endregion

    //region customization methods
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Set APAS disabled icon
     *
     * @param resourceId to be used
     */
    fun setAPASDisabledIcon(@DrawableRes resourceId: Int) {
        apasDisabledIcon = getDrawable(resourceId)
    }

    /**
     * Set APAS enabled icon
     *
     * @param resourceId to be used
     */
    fun setAPASEnabledIcon(@DrawableRes resourceId: Int) {
        apasEnabledIcon = getDrawable(resourceId)
    }

    /**
     * Set APAS active icon
     *
     * @param resourceId to be used
     */
    fun setAPASActiveIcon(@DrawableRes resourceId: Int) {
        apasActiveIcon = getDrawable(resourceId)
    }

    /**
     * Set the safety warning dialog icon
     */
    fun setSafetyWarningDialogIcon(@DrawableRes resourceId: Int) {
        safetyWarningDialogIcon = getDrawable(resourceId)
    }
    //endregion

    //region Hooks

    /**
     * The type of dialog shown
     */
    sealed class DialogType {
        /**
         * Dialog shown for APAS safety warning
         */
        object SafetyWarning : DialogType()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * APAS State updated
         */
        data class APASStateUpdated(val apasState: APASState) : ModelState()
    }
    //endregion
}