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

package dji.ux.beta.intelligentflight.widget.returnhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.flightcontroller.flyzone.FlyZoneReturnToHomeState
import dji.log.DJILog
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.Disposable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.schedulers.Schedulers
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.GlobalPreferencesManager
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.base.widget.IconButtonWidget.WidgetUIState.*
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.ui.SlidingDialog
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.ProductUtil
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.intelligentflight.R
import dji.ux.beta.intelligentflight.widget.returnhome.ReturnHomeWidget.ReturnHomeWidgetState
import dji.ux.beta.intelligentflight.widget.returnhome.ReturnHomeWidget.ReturnHomeWidgetState.*
import dji.ux.beta.intelligentflight.widget.returnhome.ReturnHomeWidgetModel.ReturnHomeState
import java.text.DecimalFormat

private const val TAG = "ReturnHomeWidget"
private const val RETURN_TO_HOME_DISTANCE_THRESHOLD = 20
private const val RETURN_HOME_AT_CURRENT_ALTITUDE_MINIMUM = 3

/**
 * A button that performs actions related to returning home. There are two possible states for the
 * widget: ready to return home, and returning home in progress. Clicking the button when the
 * aircraft is ready to return home will open a dialog to confirm returning to home.
 */
open class ReturnHomeWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ReturnHomeWidgetState>(context, attrs, defStyleAttr), View.OnClickListener {
    //region Fields
    private var slidingDialog: SlidingDialog? = null
    private val decimalFormat = DecimalFormat("#.#")

    private val widgetModel: ReturnHomeWidgetModel by lazy {
        ReturnHomeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                SchedulerProvider.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    private val description: String
        get() {
            val (distanceToHome, currentHeight, goToHomeHeight, unitType) = widgetModel.distanceToHome
            return if (distanceToHome < RETURN_TO_HOME_DISTANCE_THRESHOLD) {
                if (distanceToHome > RETURN_HOME_AT_CURRENT_ALTITUDE_MINIMUM && widgetModel.isRTHAtCurrentAltitudeEnabled) {
                    getString(R.string.uxsdk_return_home_at_current_altitude)
                } else {
                    getString(R.string.uxsdk_return_home_inner_desc)
                }
            } else {
                if (widgetModel.flyZoneReturnToHomeState == FlyZoneReturnToHomeState.NEAR_NO_FLY_ZONE) {
                    getString(R.string.uxsdk_return_home_near_nfz)
                } else if (ProductUtil.isProductWiFiConnected()) {
                    getString(R.string.uxsdk_return_home_wifi)
                } else {
                    context.getString(R.string.uxsdk_return_home_below_desc,
                            getHeightString(goToHomeHeight, unitType),
                            getHeightString(currentHeight, unitType))
                }
            }
        }

    /**
     * The theme of the dialog
     */
    @get:StyleRes
    @setparam:StyleRes
    var dialogTheme = R.style.UXSDKTakeOffDialogTheme
        set(value) {
            field = value
            initDialog()
        }

    /**
     * Cancel return home action icon drawable
     */
    var cancelReturnHomeActionIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_cancel_landing)
        set(drawable) {
            field = drawable
            checkAndUpdateReturnHomeState()
        }

    /**
     * Return home action icon
     */
    var returnHomeActionIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_return_home)
        set(drawable) {
            field = drawable
            checkAndUpdateReturnHomeState()
        }

    /**
     * Dialog icon drawable
     */
    var returnHomeDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_return_home_yellow)

    /**
     * The text size of the title for the dialog shown by this widget
     */
    var dialogTitleTextSize: Float
        @Dimension
        get() = slidingDialog?.dialogTitleTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(@Dimension textSize) {
            slidingDialog?.dialogTitleTextSize = textSize
        }

    /**
     * The color of the title for the dialog shown by this widget
     */
    @ColorInt
    var dialogTitleTextColor: Int = getColor(R.color.uxsdk_yellow)

    /**
     * The background of the title for the dialog shown by this widget
     */
    var dialogTitleBackground: Drawable?
        get() = slidingDialog?.dialogTitleBackground
        set(value) {
            slidingDialog?.dialogTitleBackground = value
        }

    /**
     * The text size of the message for the dialog shown by this widget
     */
    var dialogMessageTextSize: Float
        get() = slidingDialog?.dialogMessageTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(value) {
            slidingDialog?.dialogMessageTextSize = value
        }

    /**
     * The text color of the message for the dialog shown by this widget
     */
    var dialogMessageTextColor: Int
        @ColorInt
        get() = slidingDialog?.dialogMessageTextColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.dialogMessageTextColor = color
        }

    /**
     * The background of the message for the dialog shown by this widget
     */
    var dialogMessageBackground: Drawable?
        get() = slidingDialog?.dialogMessageBackground
        set(value) {
            slidingDialog?.dialogMessageBackground = value
        }

    /**
     * The text size of the cancel button for the dialog shown by this widget
     */
    var dialogCancelTextSize: Float
        @Dimension
        get() = slidingDialog?.cancelTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(@Dimension textSize) {
            slidingDialog?.cancelTextSize = textSize
        }

    /**
     * The text color of the cancel button for the dialog shown by this widget
     */
    var dialogCancelTextColor: Int
        @ColorInt
        get() = slidingDialog?.cancelTextColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.cancelTextColor = color
        }

    /**
     * The text colors of the cancel button for the dialog shown by this widget
     */
    var dialogCancelTextColors: ColorStateList?
        get() = slidingDialog?.cancelTextColors
                ?: ColorStateList.valueOf(getColor(R.color.uxsdk_white))
        set(colors) {
            slidingDialog?.cancelTextColors = colors
        }

    /**
     * The background of the cancel button for the dialog shown by this widget
     */
    var dialogCancelBackground: Drawable?
        get() = slidingDialog?.cancelBackground
        set(value) {
            slidingDialog?.cancelBackground = value
        }

    /**
     * The text size of the slider message for the dialog shown by this widget
     */
    var dialogSliderMessageTextSize: Float
        @Dimension
        get() = slidingDialog?.actionMessageTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal))
        set(@Dimension textSize) {
            slidingDialog?.actionMessageTextSize = textSize
        }

    /**
     * The text color of the slider message for the dialog shown by this widget
     */
    var dialogSliderMessageTextColor: Int
        @ColorInt
        get() = slidingDialog?.actionMessageTextColor
                ?: getColor(R.color.uxsdk_slider_text)
        set(@ColorInt color) {
            slidingDialog?.actionMessageTextColor = color
        }

    /**
     * The background of the slider message for the dialog shown by this widget
     */
    var dialogSliderMessageBackground: Drawable?
        get() = slidingDialog?.actionMessageBackground
        set(value) {
            slidingDialog?.actionMessageBackground = value
        }

    /**
     * The icon to the right of the slider message for the dialog shown by this widget
     */
    var dialogSliderIcon: Drawable?
        get() = slidingDialog?.actionIcon
        set(icon) {
            slidingDialog?.actionIcon = icon
        }

    /**
     * The color of the slider thumb for the dialog shown by this widget
     */
    var dialogSliderThumbColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderThumbColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.actionSliderThumbColor = color
        }

    /**
     * The color of the slider thumb when selected for the dialog shown by this widget
     */
    var dialogSliderThumbSelectedColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderThumbSelectedColor
                ?: getColor(R.color.uxsdk_slider_thumb_selected)
        set(@ColorInt color) {
            slidingDialog?.actionSliderThumbSelectedColor = color
        }

    /**
     * The fill color of the slider for the dialog shown by this widget
     */
    var dialogSliderFillColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderFillColor ?: getColor(R.color.uxsdk_slider_filled)
        set(@ColorInt color) {
            slidingDialog?.actionSliderFillColor = color
        }

    /**
     * The background of the dialog shown by this widget
     */
    var dialogBackground: Drawable?
        get() = slidingDialog?.background
                ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        set(background) {
            slidingDialog?.background = background
        }

    //endregion

    //region lifecycle
    init {
        setOnClickListener(this)
        initDialog()
        attrs?.let { initAttributes(context, it) }
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
        addReaction(widgetModel.returnHomeState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { returnHomeState: ReturnHomeState -> updateReturnHomeState(returnHomeState) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun getIdealDimensionRatioString(): String = getString(R.string.uxsdk_widget_default_ratio)

    override fun onClick(view: View?) {
        super.onClick(view)
        val drawable = foregroundImageView.drawable
        drawable?.let {
            when (it) {
                returnHomeActionIcon -> showConfirmDialog(description)
                else -> addDisposable(cancelReturnToHome())
            }
        }
    }
    //endregion

    //region private helpers
    private fun initDialog() {
        if (!isInEditMode) {
            slidingDialog = SlidingDialog(context, dialogTheme)
            slidingDialog?.setOnEventListener(object : SlidingDialog.OnEventListener {
                override fun onCancelClick(dialog: DialogInterface?) {
                    slidingDialog?.dismiss()
                    uiUpdateStateProcessor.onNext(DialogActionDismiss(null))
                }

                override fun onSlideChecked(dialog: DialogInterface?, checked: Boolean) {
                    if (checked) {
                        addDisposable(returnToHome())
                        slidingDialog?.dismiss()
                        uiUpdateStateProcessor.onNext(DialogActionConfirm(null))
                    }
                }

                override fun onCheckBoxChecked(dialog: DialogInterface?, checked: Boolean) {
                    // no checkbox used
                }
            })
        }
    }

    private fun showConfirmDialog(msg: String) {
        slidingDialog?.setDialogTitleRes(R.string.uxsdk_return_to_home_header)
                ?.setDialogTitleTextColor(dialogTitleTextColor)
                ?.setDialogMessage(msg)
                ?.setActionMessageRes(R.string.uxsdk_return_to_home_action)
                ?.setDialogIcon(returnHomeDialogIcon)
                ?.show()
        uiUpdateStateProcessor.onNext(DialogDisplayed(null))
    }

    private fun returnToHome(): Disposable {
        return widgetModel.performReturnHomeAction()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    widgetStateDataProcessor.onNext(ReturnHomeStartedSuccess)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        DJILog.e(TAG, error.toString())
                        widgetStateDataProcessor.onNext(ReturnHomeStartedError(error))
                    }
                }
    }

    private fun cancelReturnToHome(): Disposable {
        return widgetModel.performCancelReturnHomeAction()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    widgetStateDataProcessor.onNext(ReturnHomeCanceledSuccess)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        DJILog.e(TAG, error.toString())
                        widgetStateDataProcessor.onNext(ReturnHomeCanceledError(error))
                    }
                }
    }

    private fun getHeightString(height: Float, unitType: UnitConversionUtil.UnitType): String {
        val resourceString =
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    R.string.uxsdk_value_feet
                } else {
                    R.string.uxsdk_value_meters
                }
        return resources.getString(resourceString, decimalFormat.format(height))
    }

    private fun updateReturnHomeState(returnHomeState: ReturnHomeState) {
        widgetStateDataProcessor.onNext(ReturnHomeStateUpdated(returnHomeState))
        foregroundImageView.setImageDrawable(
                when (returnHomeState) {
                    ReturnHomeState.READY_TO_RETURN_HOME,
                    ReturnHomeState.RETURN_HOME_DISABLED -> returnHomeActionIcon
                    ReturnHomeState.RETURNING_TO_HOME,
                    ReturnHomeState.FORCED_RETURNING_TO_HOME -> cancelReturnHomeActionIcon
                    else -> null
                })
        isEnabled = !(returnHomeState == ReturnHomeState.RETURN_HOME_DISABLED ||
                returnHomeState == ReturnHomeState.FORCED_RETURNING_TO_HOME)
        visibility =
                if (returnHomeState == ReturnHomeState.AUTO_LANDING ||
                        returnHomeState == ReturnHomeState.DISCONNECTED) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        updateImageAlpha()
    }

    private fun updateImageAlpha() {
        if (foregroundImageView.imageDrawable == cancelReturnHomeActionIcon) {
            foregroundImageView.isEnabled = isEnabled
            if (isPressed) {
                foregroundImageView.alpha = DISABLE_ALPHA
            } else {
                foregroundImageView.alpha = ENABLE_ALPHA
            }
            return
        }

        if ((isPressed || isFocused) || !isEnabled) {
            foregroundImageView.alpha = DISABLE_ALPHA
        } else {
            foregroundImageView.alpha = ENABLE_ALPHA
        }
    }

    private fun checkAndUpdateReturnHomeState() {
        if (!isInEditMode) {
            addDisposable(widgetModel.returnHomeState.firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateReturnHomeState(it) },
                            logErrorConsumer(TAG, "Update Return Home State ")))
        }
    }

    override fun checkAndUpdateIconColor() {
        // do nothing
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ReturnHomeWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogTheme) {
                dialogTheme = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_returnHomeDrawable) {
                returnHomeActionIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_cancelReturnHomeDrawable) {
                cancelReturnHomeActionIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_returnHomeDialogIcon) {
                returnHomeDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogTitleTextAppearance) {
                setDialogTitleTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogTitleTextSize) {
                dialogTitleTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogTitleTextColor) {
                dialogTitleTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogTitleBackground) {
                dialogTitleBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogMessageTextAppearance) {
                setDialogMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogMessageTextSize) {
                dialogMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogMessageTextColor) {
                dialogMessageTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogMessageBackground) {
                dialogMessageBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogCancelTextAppearance) {
                setDialogCancelTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogCancelTextSize) {
                dialogCancelTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogCancelTextColor) {
                dialogCancelTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogCancelBackground) {
                dialogCancelBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderMessageTextAppearance) {
                setDialogSliderMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderMessageTextSize) {
                dialogSliderMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderMessageTextColor) {
                dialogSliderMessageTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderMessageBackground) {
                dialogSliderMessageBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderIcon) {
                dialogSliderIcon = it
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderThumbColor) {
                dialogSliderThumbColor = it
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderThumbSelectedColor) {
                dialogSliderThumbSelectedColor = it
            }
            typedArray.getColorAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogSliderFillColor) {
                dialogSliderFillColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnHomeWidget_uxsdk_dialogBackground) {
                dialogBackground = it
            }
        }
    }
    //endregion

    //region Customization
    /**
     * Set return home action drawable resource
     *
     * @param resourceId resource id of return home action icon
     */
    fun setReturnHomeActionIcon(@DrawableRes resourceId: Int) {
        returnHomeActionIcon = getDrawable(resourceId)
    }

    /**
     * Set cancel return home action icon drawable
     *
     * @param resourceId resource id of cancel return home action
     */
    fun setCancelReturnHomeActionIcon(@DrawableRes resourceId: Int) {
        cancelReturnHomeActionIcon = getDrawable(resourceId)
    }

    /**
     * Set return home dialog icon drawable
     *
     * @param resourceId resource id of return home dialog icon
     */
    fun setReturnHomeDialogIcon(@DrawableRes resourceId: Int) {
        returnHomeDialogIcon = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the title for the dialog shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogTitleTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setDialogTitleTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the message for the dialog shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogMessageTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setDialogMessageTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the cancel button for the dialog shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogCancelTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setCancelTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the slider message for the dialog shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogSliderMessageTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setActionMessageTextAppearance(textAppearance)
    }
    //endregion

    //region hooks
    /**
     * Get the [ReturnHomeWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<ReturnHomeWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ReturnHomeWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ReturnHomeWidgetState()

        /**
         * Return Home State update
         */
        data class ReturnHomeStateUpdated(val state: ReturnHomeState) : ReturnHomeWidgetState()

        /**
         * Return to Home started successfully
         */
        object ReturnHomeStartedSuccess : ReturnHomeWidgetState()

        /**
         * Return to Home not started due to error
         */
        data class ReturnHomeStartedError(val error: UXSDKError) : ReturnHomeWidgetState()

        /**
         * Return to Home canceled successfully
         */
        object ReturnHomeCanceledSuccess : ReturnHomeWidgetState()

        /**
         * Return to Home not canceled due to error
         */
        data class ReturnHomeCanceledError(val error: UXSDKError) : ReturnHomeWidgetState()
    }

    //endregion
}