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

package dji.ux.beta.core.widget.airsense

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.use
import dji.common.flightcontroller.adsb.AirSenseWarningLevel
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.R
import dji.ux.beta.core.base.ConstraintLayoutWidget
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.GlobalPreferencesManager
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.ViewUtil
import dji.ux.beta.core.widget.airsense.AirSenseWidget.AirSenseWidgetState
import dji.ux.beta.core.widget.airsense.AirSenseWidget.AirSenseWidgetState.*
import dji.ux.beta.core.widget.airsense.AirSenseWidget.AirSenseWidgetUIState.*
import dji.ux.beta.core.widget.airsense.AirSenseWidgetModel.AirSenseStatus

/**
 * Widget that displays an icon representing whether there are any aircraft nearby and how likely
 * a collision is. The icon is shown in different colors representing the current
 * [AirSenseWarningLevel]. The widget will be hidden on devices that do not have DJI AirSense
 * installed.
 *
 * When the warning level is at [AirSenseWarningLevel.LEVEL_0] or above, a warning dialog will
 * appear. This warning dialog contains a warning message, an option to never show the dialog
 * again, and a link to an additional dialog with AirSense Terms and Conditions for the user to
 * agree to.
 *
 * When the warning level is at [AirSenseWarningLevel.LEVEL_2] or above, a WarningMessage is sent
 * to suggest that the user should descend immediately. To react to all WarningMessages sent by all
 * widgets including the AirSenseWidget, listen to MessagingKeys.SEND_WARNING_MESSAGE.
 *
 * The icon is gray when no airplanes are nearby, and adds the text "N/A" when no product is
 * connected.
 */
open class AirSenseWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<AirSenseWidgetState>(context, attrs, defStyleAttr) {

    //region Fields
    private val airSenseImageView: ImageView = findViewById(R.id.imageview_air_sense)
    private val colorMap: MutableMap<AirSenseStatus, Int> =
            mutableMapOf(
                    AirSenseStatus.DISCONNECTED to getColor(R.color.uxsdk_gray_58),
                    AirSenseStatus.NO_AIR_SENSE_CONNECTED to getColor(R.color.uxsdk_gray_58),
                    AirSenseStatus.NO_AIRPLANES_NEARBY to getColor(R.color.uxsdk_gray_58),
                    AirSenseStatus.WARNING_LEVEL_0 to getColor(R.color.uxsdk_white),
                    AirSenseStatus.WARNING_LEVEL_1 to getColor(R.color.uxsdk_blue_highlight),
                    AirSenseStatus.WARNING_LEVEL_2 to getColor(R.color.uxsdk_yellow_500),
                    AirSenseStatus.WARNING_LEVEL_3 to getColor(R.color.uxsdk_red_500),
                    AirSenseStatus.WARNING_LEVEL_4 to getColor(R.color.uxsdk_red_500))
    private val blinkAnimation: Animation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink)
    private var warningDialogDisplayed: Boolean = false
    private val uiUpdateStateProcessor: PublishProcessor<AirSenseWidgetUIState> = PublishProcessor.create()
    private val widgetModel by lazy {
        AirSenseWidgetModel(DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                SchedulerProvider.getInstance())
    }

    var airSenseDisconnectedStateIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_topbar_adsb_disconnected)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }
    var airSenseConnectedStateIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_topbar_adsb_normal)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * The drawable resource for the AirSense icon's background
     */
    var airSenseIconBackground: Drawable?
        get() = airSenseImageView.background
        set(value) {
            airSenseImageView.background = value
        }

    /**
     * The theme of the warning dialog
     */
    @get:StyleRes
    @setparam:StyleRes
    var warningDialogTheme = R.style.UXSDKAirSenseWarningDialogTheme

    /**
     * The theme of the terms dialog
     */
    @get:StyleRes
    @setparam:StyleRes
    var termsDialogTheme = 0

    /**
     * The text appearance of the terms link text view
     */
    @get:StyleRes
    @setparam:StyleRes
    var termsLinkTextAppearance = 0

    /**
     * The text color for the terms link text view
     */
    @get:ColorInt
    @setparam:ColorInt
    var termsLinkTextColor = getColor(R.color.uxsdk_blue)

    /**
     * The background of the terms link text view
     */
    var termsLinkTextBackground: Drawable? = null

    /**
     * The text size of the terms link text view
     */
    @get:Dimension
    @setparam:Dimension
    var termsLinkTextSize: Float = 0f

    /**
     * The text appearance of the check box label
     */
    @get:StyleRes
    @setparam:StyleRes
    var checkBoxTextAppearance = 0

    /**
     * The text color state list of the check box label
     */
    var checkBoxTextColor: ColorStateList? = null

    /**
     * The background of the check box
     */
    var checkBoxTextBackground: Drawable? = null

    /**
     * The text size of the check box label
     */
    @get:Dimension
    @setparam:Dimension
    var checkBoxTextSize: Float = 0f
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_air_sense, this)
    }

    init {
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
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
        addReaction(widgetModel.airSenseWarningLevel
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateAirSenseWarningLevel(it) })
        addReaction(widgetModel.airSenseStatus
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateIcon(it) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    //endregion

    //region Reactions

    private fun updateAirSenseWarningLevel(warningLevel: AirSenseWarningLevel) {
        updateWarningMessages(warningLevel)
        if (warningLevel != AirSenseWarningLevel.LEVEL_0
                && warningLevel != AirSenseWarningLevel.UNKNOWN) {
            showWarningDialog()
        }
        widgetStateDataProcessor.onNext(AirSenseWarningLevelUpdate(warningLevel))
    }

    private fun updateIcon(status: AirSenseStatus) {
        visibility = if (status == AirSenseStatus.NO_AIR_SENSE_CONNECTED) GONE else VISIBLE
        airSenseImageView.imageDrawable =
                if (status == AirSenseStatus.DISCONNECTED) {
                    airSenseDisconnectedStateIcon
                } else {
                    airSenseConnectedStateIcon
                }

        if (colorMap.containsKey(status)) {
            ViewUtil.tintImage(airSenseImageView, getAirSenseIconTintColor(status))
        }
        if (status == AirSenseStatus.WARNING_LEVEL_4) {
            airSenseImageView.startAnimation(blinkAnimation)
        } else {
            airSenseImageView.clearAnimation()
        }
        widgetStateDataProcessor.onNext(AirSenseStatusUpdate(status))
    }

    private fun updateWarningMessages(warningLevel: AirSenseWarningLevel) {
        addDisposable(widgetModel.sendWarningMessages(getString(R.string.uxsdk_message_air_sense_warning_title),
                getString(R.string.uxsdk_message_air_sense_dangerous_content),
                getString(R.string.uxsdk_message_air_sense_warning_content),
                warningLevel)
                .subscribe())
    }

    private fun showWarningDialog() {
        val checked = GlobalPreferencesManager.getInstance().isAirSenseTermsNeverShown
        if (!checked && !warningDialogDisplayed) {
            val builder: AlertDialog.Builder = if (warningDialogTheme != 0) {
                AlertDialog.Builder(context, warningDialogTheme)
            } else {
                AlertDialog.Builder(context)
            }
            builder.setCancelable(true)
            builder.setOnCancelListener { onWarningDialogClosed() }
            builder.setTitle(getString(R.string.uxsdk_message_air_sense_warning_title))
            builder.setPositiveButton(R.string.uxsdk_app_ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                onWarningDialogClosed()
            }
            builder.setView(createTermsView())
            builder.create().show()
            warningDialogDisplayed = true
        }
    }

    private fun onWarningDialogClosed() {
        warningDialogDisplayed = false
        uiUpdateStateProcessor.onNext(WarningDialogDismiss)
    }

    private fun createTermsView(): View {
        val termsView = if (warningDialogTheme != 0) {
            val ctw = ContextThemeWrapper(context, warningDialogTheme)
            val inflater: LayoutInflater = ctw.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(R.layout.uxsdk_layout_terms_view, null)
        } else {
            View.inflate(context, R.layout.uxsdk_layout_terms_view, null)
        }
        val dontShowAgainCheckBox = termsView.findViewById<CheckBox>(R.id.checkbox_dont_show_again)
        dontShowAgainCheckBox.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            GlobalPreferencesManager.getInstance().isAirSenseTermsNeverShown = checked
            uiUpdateStateProcessor.onNext(DontShowAgainCheckBoxTap(checked))
        }
        if (checkBoxTextAppearance != INVALID_RESOURCE) {
            dontShowAgainCheckBox.setTextAppearance(context, checkBoxTextAppearance)
        }
        if (checkBoxTextColor != null) {
            dontShowAgainCheckBox.setTextColor(checkBoxTextColor)
        }
        if (checkBoxTextBackground != null) {
            dontShowAgainCheckBox.background = checkBoxTextBackground
        }
        if (checkBoxTextSize != INVALID_DIMENSION) {
            dontShowAgainCheckBox.textSize = checkBoxTextSize
        }
        val termsLinkTextView = termsView.findViewById<TextView>(R.id.textview_terms_link)
        val termsLink = SpannableString(getString(R.string.uxsdk_air_sense_terms_content))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                showTermsDialog()
                uiUpdateStateProcessor.onNext(TermsLinkTap)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                if (termsLinkTextColor != 0) {
                    ds.color = termsLinkTextColor
                }
            }
        }
        termsLink.setSpan(clickableSpan, 0, termsLink.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        termsLinkTextView.linksClickable = true
        termsLinkTextView.movementMethod = LinkMovementMethod.getInstance()
        termsLinkTextView.text = termsLink
        if (termsLinkTextAppearance != INVALID_RESOURCE) {
            termsLinkTextView.setTextAppearance(context, termsLinkTextAppearance)
        }
        if (termsLinkTextBackground != null) {
            termsLinkTextView.background = termsLinkTextBackground
        }
        if (termsLinkTextSize != INVALID_DIMENSION) {
            termsLinkTextView.textSize = termsLinkTextSize
        }
        return termsView
    }

    private fun showTermsDialog() {
        val builder: AlertDialog.Builder = if (termsDialogTheme != 0) {
            AlertDialog.Builder(context, termsDialogTheme)
        } else {
            AlertDialog.Builder(context)
        }
        builder.setCancelable(true)
        builder.setOnCancelListener { onTermsDialogClosed() }
        builder.setPositiveButton(R.string.uxsdk_app_ok) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            onTermsDialogClosed()
        }
        val myWebView = WebView(context)
        myWebView.loadUrl(AIR_SENSE_TERMS_URL)
        builder.setView(myWebView)
        builder.create().show()
    }

    private fun onTermsDialogClosed() {
        uiUpdateStateProcessor.onNext(TermsDialogDismiss)
    }

    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(widgetModel.airSenseStatus.firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateIcon(it) }, logErrorConsumer(TAG, "Update Icon ")))
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_air_sense_ratio)
    }

    /**
     * Tints the AirSense icon to the given color when the AirSense status is the given value.
     *
     * @param status The status for which to tint the AirSense icon.
     * @param color The color to tint the AirSense icon.
     */
    fun setAirSenseIconTintColor(status: AirSenseStatus, @ColorInt color: Int) {
        colorMap[status] = color
        checkAndUpdateIcon()
    }

    /**
     * Returns the color that the AirSense icon will be tinted when the AirSense status is
     * the given value.
     *
     * @param status The status for which the AirSense icon will be tinted the returned
     * color.
     * @return The color the AirSense icon will be tinted.
     */
    @ColorInt
    fun getAirSenseIconTintColor(status: AirSenseStatus): Int {
        return (colorMap[status]?.let { it } ?: getColor(R.color.uxsdk_white))
    }

    /**
     * Set the resource ID for the AirSense icon when there is no product connected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setAirSenseDisconnectedStateIcon(@DrawableRes resourceId: Int) {
        airSenseDisconnectedStateIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the AirSense icon when a product is connected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setAirSenseConnectedStateIcon(@DrawableRes resourceId: Int) {
        airSenseConnectedStateIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the AirSense icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setAirSenseIconBackground(@DrawableRes resourceId: Int) {
        airSenseIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the background of the terms link text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    fun setTermsLinkTextBackground(@DrawableRes resourceId: Int) {
        termsLinkTextBackground = getDrawable(resourceId)
    }

    /**
     * Set the text color for the check box label
     *
     * @param color color integer resource
     */
    fun setCheckBoxTextColor(@ColorInt color: Int) {
        if (color != INVALID_COLOR) {
            checkBoxTextColor = ColorStateList.valueOf(color)
        }
    }

    /**
     * Set the resource ID for the background of the check box
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    fun setCheckBoxBackground(@DrawableRes resourceId: Int) {
        checkBoxTextBackground = getDrawable(resourceId)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.AirSenseWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseConnectedStateIcon) {
                this.airSenseConnectedStateIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseDisconnectedStateIcon) {
                this.airSenseDisconnectedStateIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconBackground) {
                airSenseIconBackground = it
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconDisconnectedTint) {
                setAirSenseIconTintColor(AirSenseStatus.DISCONNECTED, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconNoAirplanesNearbyTint) {
                setAirSenseIconTintColor(AirSenseStatus.NO_AIRPLANES_NEARBY, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconWarningLevel0Tint) {
                setAirSenseIconTintColor(AirSenseStatus.WARNING_LEVEL_0, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconWarningLevel1Tint) {
                setAirSenseIconTintColor(AirSenseStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconWarningLevel2Tint) {
                setAirSenseIconTintColor(AirSenseStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconWarningLevel3Tint) {
                setAirSenseIconTintColor(AirSenseStatus.WARNING_LEVEL_3, it)
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_airSenseIconWarningLevel4Tint) {
                setAirSenseIconTintColor(AirSenseStatus.WARNING_LEVEL_4, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.AirSenseWidget_uxsdk_linkTextAppearance) {
                termsLinkTextAppearance = it
            }
            typedArray.getColorAndUse(R.styleable.AirSenseWidget_uxsdk_linkTextColor) {
                termsLinkTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.AirSenseWidget_uxsdk_linkTextBackground) {
                termsLinkTextBackground = it
            }
            typedArray.getDimensionAndUse(R.styleable.AirSenseWidget_uxsdk_linkTextSize) {
                termsLinkTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.AirSenseWidget_uxsdk_checkBoxTextAppearance) {
                checkBoxTextAppearance = it
            }
            typedArray.getColorStateListAndUse(R.styleable.AirSenseWidget_uxsdk_checkBoxTextColor) {
                checkBoxTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.AirSenseWidget_uxsdk_checkBoxTextBackground) {
                checkBoxTextBackground = it
            }
            typedArray.getDimensionAndUse(R.styleable.AirSenseWidget_uxsdk_checkBoxTextSize) {
                checkBoxTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.AirSenseWidget_uxsdk_warningDialogTheme) {
                warningDialogTheme = it
            }
            typedArray.getResourceIdAndUse(R.styleable.AirSenseWidget_uxsdk_termsDialogTheme) {
                termsDialogTheme = it
            }
        }
    }
    //endregion

    //region Hooks
    /**
     * Get the [AirSenseWidgetUIState] updates
     */
    fun getUIStateUpdates(): Flowable<AirSenseWidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * Get the [AirSenseWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<AirSenseWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Widget UI update State
     */
    sealed class AirSenseWidgetUIState {

        /**
         * Update when warning dialog is dismissed
         */
        object WarningDialogDismiss : AirSenseWidgetUIState()

        /**
         * Update when terms link is tapped
         */
        object TermsLinkTap : AirSenseWidgetUIState()

        /**
         * Update when terms dialog is dismissed
         */
        object TermsDialogDismiss : AirSenseWidgetUIState()

        /**
         * Update when "Don't show again" checkbox is tapped
         */
        data class DontShowAgainCheckBoxTap(val isChecked: Boolean) : AirSenseWidgetUIState()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class AirSenseWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : AirSenseWidgetState()

        /**
         * AirSense warning level update
         */
        data class AirSenseWarningLevelUpdate(val airSenseWarningLevel: AirSenseWarningLevel) : AirSenseWidgetState()

        /**
         * AirSense status update
         */
        data class AirSenseStatusUpdate(val airSenseStatus: AirSenseStatus) : AirSenseWidgetState()

    }
    //endregion

    companion object {
        private const val AIR_SENSE_TERMS_URL = "file:///android_asset/htmls/air_sense_terms_of_use.html"
        private const val TAG = "AirSenseWidget"
    }
}