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

package dji.ux.beta.core.widget.systemstatus

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Pair
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.common.logics.warningstatuslogic.WarningStatusItem.WarningLevel
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.Disposable
import dji.thirdparty.io.reactivex.functions.BiFunction
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.R
import dji.ux.beta.core.base.*
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget.SystemStatusWidgetState
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget.SystemStatusWidgetState.ProductConnected
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget.SystemStatusWidgetState.SystemStatusUpdated
import java.util.*

private const val TAG = "SystemStatusWidget"

/**
 * This widget shows the system status of the aircraft.
 *
 * The WarningStatusItem received by this widget contains the message to be
 * displayed, the warning level and the urgency of the message.
 *
 * The color of the background changes depending on the severity of the
 * status as determined by the WarningLevel. The UI also reacts
 * to the urgency of the message by causing the background to blink.
 */
open class SystemStatusWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<SystemStatusWidgetState>(context, attrs, defStyleAttr), View.OnClickListener {
    //region Fields
    private val systemStatusTextView: TextView = findViewById(R.id.textview_system_status)
    private val systemStatusBackgroundImageView: ImageView = findViewById(R.id.imageview_system_status_background)
    private val blinkAnimation: Animation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink)

    private val widgetModel by lazy {
        SystemStatusWidgetModel(DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                SchedulerProvider.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    private val textColorMap: MutableMap<WarningLevel, Int> =
            mutableMapOf(
                    WarningLevel.ERROR to getColor(R.color.uxsdk_status_error),
                    WarningLevel.WARNING to getColor(R.color.uxsdk_status_warning),
                    WarningLevel.GOOD to getColor(R.color.uxsdk_status_good),
                    WarningLevel.OFFLINE to getColor(R.color.uxsdk_status_offline))
    private val backgroundDrawableMap: MutableMap<WarningLevel, Drawable?> = mutableMapOf()
    private val compassErrorString: String = getString(R.string.fpv_tip_compass_error)

    /**
     * The text size of the system status message text view
     */
    var systemStatusMessageTextSize: Float
        @Dimension
        get() = systemStatusTextView.textSize
        set(@Dimension textSize) {
            systemStatusTextView.textSize = textSize
        }

    /**
     * Call back for when the widget is tapped.
     * This can be used to link the widget to [dji.ux.beta.core.panelwidget.systemstatus.SystemStatusListPanelWidget]
     */
    var stateChangeCallback: OnStateChangeCallback<Any>? = null

    private var stateChangeResourceId: Int = 0

    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_system_status, this)
    }

    init {
        setOnClickListener(this)
        systemStatusTextView.isSelected = true //Required for horizontal scrolling in textView
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListener()
    }

    override fun onDetachedFromWindow() {
        destroyListener()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.systemStatus
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateUI(it) })
        addReaction(reactToCompassError())
        addReaction(widgetModel.warningStatusMessageData
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMessage(it) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onClick(v: View?) {
        stateChangeCallback?.onStateChange(null)
    }
    //endregion

    //region Reactions to model
    private fun updateUI(status: WarningStatusItem) {
        systemStatusTextView.textColor = getSystemStatusMessageTextColor(status.warningLevel)
        systemStatusBackgroundImageView.imageDrawable =
                getSystemStatusBackgroundDrawable(status.warningLevel)
        blinkBackground(status.isUrgentMessage)
        widgetStateDataProcessor.onNext(SystemStatusUpdated(status))
    }

    private fun updateMessage(messageData: SystemStatusWidgetModel.WarningStatusMessageData) {
        systemStatusTextView.text =
                if (isMaxHeightMessage(messageData.message)) {
                    messageData.message + " - " + formatMaxHeight(messageData.maxHeight, messageData.unitType)
                } else {
                    messageData.message
                }
    }

    private fun isMaxHeightMessage(text: String?): Boolean {
        return text == getString(R.string.fpv_tip_in_limit_space) ||
                text == getString(R.string.fpv_tip_in_nfz_max_height_special_unlock)
    }

    private fun formatMaxHeight(maxHeight: Float, unitType: UnitConversionUtil.UnitType): String? {
        val maxHeightStr: String =
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    resources.getString(R.string.uxsdk_value_feet, String.format(Locale.US, "%.0f", maxHeight))
                } else {
                    resources.getString(R.string.uxsdk_value_meters, String.format(Locale.US, "%.0f", maxHeight))
                }
        return resources.getString(R.string.uxsdk_max_flight_height_limit, maxHeightStr)
    }

    private fun blinkBackground(isUrgentMessage: Boolean) {
        if (isUrgentMessage) {
            systemStatusBackgroundImageView.startAnimation(blinkAnimation)
        } else {
            systemStatusBackgroundImageView.clearAnimation()
        }
    }

    private fun reactToCompassError(): Disposable {
        return Flowable.combineLatest(widgetModel.systemStatus, widgetModel.isMotorOn,
                        BiFunction<WarningStatusItem, Boolean, Pair<WarningStatusItem, Boolean>> { first: WarningStatusItem, second: Boolean -> Pair(first, second) })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { values: Pair<WarningStatusItem, Boolean> -> updateVoiceNotification(values.first, values.second) },
                        logErrorConsumer(TAG, "react to Compass Error: "))
    }

    private fun updateVoiceNotification(statusItem: WarningStatusItem, isMotorOn: Boolean) {
        if (isMotorOn && statusItem.message == compassErrorString) {
            addDisposable(widgetModel.sendVoiceNotification().subscribe())
        }
    }

    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.systemStatus.firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateUI(it) }, logErrorConsumer(TAG, "Update UI ")))
        }
    }

    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription = WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.EXPAND
    )
    //endregion

    //region Customization Helpers
    /**
     * Set text appearance of the system status message text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setSystemStatusMessageTextAppearance(@StyleRes textAppearance: Int) {
        systemStatusTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the text color of the system status message for the given warning level.
     *
     * @param level The level for which to set the system status message text color.
     * @param color The color of the system status message text.
     */
    fun setSystemStatusMessageTextColor(level: WarningLevel, @ColorInt color: Int) {
        textColorMap[level] = color
        checkAndUpdateUI()
    }

    /**
     * Get the text color of the system status message for the given warning level.
     *
     * @param level The level for which to get the system status message text color.
     * @return The color of the system status message text.
     */
    @ColorInt
    fun getSystemStatusMessageTextColor(level: WarningLevel): Int {
        return (textColorMap[level]?.let { it } ?: getColor(R.color.uxsdk_status_offline))
    }

    /**
     * Set the background drawable of the system status message for the given warning level.
     *
     * @param level The level for which to set the system status message background drawable.
     * @param background The background of the system status message.
     */
    fun setSystemStatusBackgroundDrawable(level: WarningLevel, background: Drawable?) {
        backgroundDrawableMap[level] = background
        checkAndUpdateUI()
    }

    /**
     * Get the background drawable of the system status message for the given warning level.
     *
     * @param level The level for which to get the system status message background drawable.
     * @return The background drawable of the system status message.
     */
    fun getSystemStatusBackgroundDrawable(level: WarningLevel): Drawable? {
        return backgroundDrawableMap[level]
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SystemStatusWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageTextAppearance) {
                setSystemStatusMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageTextSize) {
                systemStatusMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageErrorTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.ERROR, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageWarningTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.WARNING, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageGoodTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.GOOD, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageOfflineTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.OFFLINE, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusErrorBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.ERROR, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusWarningBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.WARNING, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusGoodBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.GOOD, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusOfflineBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.OFFLINE, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SystemStatusWidget_uxsdk_onStateChange) {
                stateChangeResourceId = it
            }
        }
    }

    private fun initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.rootView != null) {
            val widgetView = this.rootView.findViewById<View>(stateChangeResourceId)
            if (widgetView is OnStateChangeCallback<*>) {
                stateChangeCallback = widgetView as OnStateChangeCallback<Any>
            }
        }
    }

    private fun destroyListener() {
        stateChangeCallback = null
    }

    //endregion

    //region hooks
    /**
     * Get the [SystemStatusWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<SystemStatusWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class SystemStatusWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : SystemStatusWidgetState()

        /**
         * System status update
         */
        data class SystemStatusUpdated(val status: WarningStatusItem) : SystemStatusWidgetState()
    }
    //endregion
}