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

package dji.ux.beta.hardwareaccessory.widget.rtk

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.core.base.ConstraintLayoutWidget
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.OnStateChangeCallback
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.hardwareaccessory.R
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKWidget.RTKWidgetState
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKWidget.RTKWidgetState.*
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKWidget.RTKWidgetUIState.DialogOKTap

private const val TAG = "RTKWidget"

/**
 * This widget contains multiple widgets to control and get information related to RTK.
 */
open class RTKWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<RTKWidgetState>(context, attrs, defStyleAttr), View.OnClickListener,
        OnStateChangeCallback<Any?> {
    //region Fields
    private val dialogOkTextView: TextView = findViewById(R.id.textview_ok)
    private val rtkDescriptionTextView: TextView = findViewById(R.id.textview_rtk_description)
    private val rtkDialogSeparator: View = findViewById(R.id.rtk_dialog_separator)
    private val uiUpdateStateProcessor: PublishProcessor<RTKWidgetUIState> = PublishProcessor.create()

    private val widgetModel by lazy {
        RTKWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                SchedulerProvider.getInstance())
    }

    /**
     * Get the RTK Enabled Widget so it can be customized.
     */
    @get:JvmName("getRTKEnabledWidget")
    val rtkEnabledWidget: RTKEnabledWidget = findViewById(R.id.widget_rtk_enabled)

    /**
     * Get the RTK Satellite Status Widget so it can be customized.
     */
    @get:JvmName("getRTKSatelliteStatusWidget")
    val rtkSatelliteStatusWidget: RTKSatelliteStatusWidget = findViewById(R.id.widget_rtk_satellite_status)

    /**
     * Text color state list for the RTK description text view
     */
    var rtkDescriptionTextColors: ColorStateList
        @JvmName("getRTKDescriptionTextColors")
        get() = rtkDescriptionTextView.textColors
        @JvmName("setRTKDescriptionTextColors")
        set(colorStateList) {
            rtkDescriptionTextView.textColorStateList = colorStateList
        }

    /**
     * The text color for the RTK description text view
     */
    var rtkDescriptionTextColor: Int
        @JvmName("getRTKDescriptionTextColor")
        @ColorInt
        get() = rtkDescriptionTextView.textColor
        @JvmName("setRTKDescriptionTextColor")
        set(@ColorInt color) {
            rtkDescriptionTextView.textColor = color
        }

    /**
     * The text size of the RTK description text view
     */
    var rtkDescriptionTextSize: Float
        @JvmName("getRTKDescriptionTextSize")
        get() = rtkDescriptionTextView.textSize
        @JvmName("setRTKDescriptionTextSize")
        set(textSize) {
            rtkDescriptionTextView.textSize = textSize
        }

    /**
     * The background for the RTK description text view
     */
    var rtkDescriptionTextBackground: Drawable?
        @JvmName("getRTKDescriptionTextBackground")
        get() = rtkDescriptionTextView.background
        @JvmName("setRTKDescriptionTextBackground")
        set(drawable) {
            rtkDescriptionTextView.background = drawable
        }

    /**
     * The text color state list of the dialog OK text view
     */
    var dialogOkTextColors: ColorStateList?
        get() = dialogOkTextView.textColors
        set(colorStateList) {
            dialogOkTextView.textColorStateList = colorStateList
        }

    /**
     * The text color of the dialog OK text view
     */
    var dialogOkTextColor: Int
        @ColorInt
        get() = dialogOkTextView.textColor
        set(@ColorInt color) {
            dialogOkTextView.textColor = color
        }

    /**
     * The text size of the dialog OK text view
     */
    var dialogOkTextSize: Float
        get() = dialogOkTextView.textSize
        set(textSize) {
            dialogOkTextView.textSize = textSize
        }

    /**
     * The background for the dialog OK text view
     */
    var dialogOkTextBackground: Drawable?
        get() = dialogOkTextView.background
        set(drawable) {
            dialogOkTextView.background = drawable
        }

    /**
     * The color for the separator line views
     */
    var rtkSeparatorsColor: Int
        @JvmName("getRTKSeparatorsColor")
        @ColorInt
        get() = rtkSatelliteStatusWidget.rtkSeparatorsColor
        @JvmName("setRTKSeparatorsColor")
        set(@ColorInt color) {
            rtkSatelliteStatusWidget.rtkSeparatorsColor = color
            rtkDialogSeparator.setBackgroundColor(color)
        }
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk, this)
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle)
    }

    init {
        dialogOkTextView.setOnClickListener(this)
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

    override fun onClick(v: View) {
        if (v.id == R.id.textview_ok) {
            visibility = View.GONE
            uiUpdateStateProcessor.onNext(DialogOKTap)
        }
    }

    override fun onStateChange(state: Any?) {
        toggleVisibility()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.rtkEnabled
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe { rtkEnabled: Boolean -> updateUIForRTKEnabled(rtkEnabled) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }
    //endregion

    //region Reactions to model
    private fun updateUIForRTKEnabled(rtkEnabled: Boolean) {
        if (rtkEnabled) {
            rtkSatelliteStatusWidget.visibility = View.VISIBLE
            rtkDescriptionTextView.visibility = View.GONE
        } else {
            rtkSatelliteStatusWidget.visibility = View.GONE
            rtkDescriptionTextView.visibility = View.VISIBLE
        }
        widgetStateDataProcessor.onNext(RTKEnabledUpdate(rtkEnabled))
    }
    //endregion

    //region Helper Methods
    private fun toggleVisibility() {
        addDisposable(widgetModel.rtkSupported
                .firstOrError()
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe(Consumer { rtkSupported: Boolean ->
                    if (rtkSupported) {
                        visibility = if (visibility == View.VISIBLE) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                        widgetStateDataProcessor.onNext(VisibilityUpdate(visibility == View.VISIBLE))
                    }
                }, logErrorConsumer(TAG, "getRTKSupported: ")))
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_rtk_ratio)
    }

    /**
     * Set text appearance of the RTK description text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKDescriptionTextAppearance(@StyleRes textAppearance: Int) {
        rtkDescriptionTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the background for the RTK description text view
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setRTKDescriptionTextBackground(@DrawableRes resourceId: Int) {
        rtkDescriptionTextBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the dialog OK text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogOkTextAppearance(@StyleRes textAppearance: Int) {
        dialogOkTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the background for the dialog OK text view
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setDialogOkTextBackground(@DrawableRes resourceId: Int) {
        dialogOkTextBackground = getDrawable(resourceId)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RTKWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.RTKWidget_uxsdk_rtkDescriptionTextAppearance) {
                setRTKDescriptionTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKWidget_uxsdk_rtkDescriptionTextSize) {
                rtkDescriptionTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKWidget_uxsdk_rtkDescriptionTextColor) {
                rtkDescriptionTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKWidget_uxsdk_rtkDescriptionBackgroundDrawable) {
                rtkDescriptionTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.RTKWidget_uxsdk_dialogOkTextAppearance) {
                setDialogOkTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKWidget_uxsdk_dialogOkTextSize) {
                dialogOkTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKWidget_uxsdk_dialogOkTextColor) {
                dialogOkTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKWidget_uxsdk_dialogOkBackgroundDrawable) {
                dialogOkTextBackground = it
            }
            typedArray.getColorAndUse(R.styleable.RTKWidget_uxsdk_rtkSeparatorsColor) {
                rtkSeparatorsColor = it
            }
        }
    }
    //endregion

    //region Hooks
    /**
     * Get the [RTKWidgetUIState] updates
     */
    fun getUIStateUpdates(): Flowable<RTKWidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * Widget UI update State
     */
    sealed class RTKWidgetUIState {
        /**
         * OK button tapped
         */
        object DialogOKTap : RTKWidgetUIState()
    }

    /**
     * Get the [RTKWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<RTKWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class RTKWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : RTKWidgetState()

        /**
         * RTK enabled update
         */
        data class RTKEnabledUpdate(val isRTKEnabled: Boolean) : RTKWidgetState()

        /**
         * Widget visibility update
         */
        data class VisibilityUpdate(val isVisible: Boolean) : RTKWidgetState()
    }
    //endregion
}