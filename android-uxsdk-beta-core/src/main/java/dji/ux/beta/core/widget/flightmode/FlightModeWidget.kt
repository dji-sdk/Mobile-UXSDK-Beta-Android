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

package dji.ux.beta.core.widget.flightmode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.R
import dji.ux.beta.core.base.ConstraintLayoutWidget
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.widget.flightmode.FlightModeWidget.FlightModeWidgetState
import dji.ux.beta.core.widget.flightmode.FlightModeWidget.FlightModeWidgetState.FlightModeTextUpdated
import dji.ux.beta.core.widget.flightmode.FlightModeWidget.FlightModeWidgetState.ProductConnected
import dji.ux.beta.core.widget.flightmode.FlightModeWidgetModel.FlightModeState

private const val TAG = "FlightModeWidget"

/**
 * Shows the current flight mode next to a flight mode icon.
 */
open class FlightModeWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<FlightModeWidgetState>(context, attrs, defStyleAttr) {

    //region Fields
    private val iconImageView: ImageView = findViewById(R.id.imageview_flight_mode_icon)
    private val flightModeTextView: TextView = findViewById(R.id.textview_flight_mode_string)
    private val widgetModel by lazy {
        FlightModeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * The color of the icon when the product is connected
     */
    @get:ColorInt
    var connectedStateIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * The color of the text when the product is connected
     */
    @get:ColorInt
    var connectedStateTextColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * The color of the text when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateTextColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Background of the flight mode text
     */
    var textBackground: Drawable?
        get() = flightModeTextView.background
        set(value) {
            flightModeTextView.background = value
        }

    /**
     * Text size for the flight mode
     */
    var textSize: Float
        @Dimension
        get() = flightModeTextView.textSize
        set(@Dimension textSize) {
            flightModeTextView.textSize = textSize
        }

    /**
     * Icon for flight mode. When customizing the flight mode icon with an image that is not the
     * default 1:1, use the [setIcon] method that takes a String ratio as well.
     */
    var icon: Drawable?
        get() = iconImageView.imageDrawable
        set(value) {
            iconImageView.imageDrawable = value
        }

    /**
     * Background of flight mode icon
     */
    var iconBackground: Drawable?
        get() = iconImageView.background
        set(value) {
            iconImageView.background = value
        }
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_flight_mode, this)
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
        addReaction(widgetModel.flightModeState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })

    }
    //endregion

    //region Reactions to model
    private fun updateUI(flightModeState: FlightModeState) {
        if (flightModeState is FlightModeState.FlightModeUpdated) {
            flightModeTextView.text = flightModeState.flightModeString
            iconImageView.setColorFilter(connectedStateIconColor, PorterDuff.Mode.SRC_IN)
            flightModeTextView.setTextColor(connectedStateTextColor)
            widgetStateDataProcessor.onNext(FlightModeTextUpdated(flightModeState.flightModeString))
        } else {
            flightModeTextView.text = getString(R.string.uxsdk_string_default_value)
            iconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
            flightModeTextView.setTextColor(disconnectedStateTextColor)
        }
    }

    //endregion

    //region helpers

    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.flightModeState
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateUI(it) }, logErrorConsumer(TAG, "Update UI ")))
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.WRAP,
                    heightDimension = WidgetSizeDescription.Dimension.EXPAND)

    /**
     * Set text appearance of the flight mode text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setTextAppearance(@StyleRes textAppearanceResId: Int) {
        flightModeTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the resource ID for the background of the flight mode text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    fun setTextBackground(@DrawableRes resourceId: Int) {
        textBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the flight mode icon. If the ratio of the image is not the default
     * 1:1, use the [setIcon] method that takes a String ratio as well.
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setIcon(@DrawableRes resourceId: Int) {
        icon = getDrawable(resourceId)
    }

    /**
     * Set the flight mode icon with a custom ratio.
     *
     * @param drawable The drawable resource
     * @param dimensionRatio String indicating the ratio of the custom icon
     */
    fun setIcon(drawable: Drawable?, dimensionRatio: String) {
        icon = drawable

        val set = ConstraintSet()
        set.clone(this)
        set.setDimensionRatio(iconImageView.id, dimensionRatio)
        set.applyTo(this)
    }

    /**
     * Set the resource ID for the flight mode icon with a custom ratio.
     *
     * @param resourceId Integer ID of the drawable resource
     * @param dimensionRatio String indicating the ratio of the custom icon
     */
    fun setIcon(@DrawableRes resourceId: Int, dimensionRatio: String) {
        setIcon(getDrawable(resourceId), dimensionRatio)
    }

    /**
     * Set the resource ID for the flight mode icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setIconBackground(@DrawableRes resourceId: Int) {
        iconBackground = getDrawable(resourceId)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.FlightModeWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.FlightModeWidget_uxsdk_textAppearance) {
                setTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.FlightModeWidget_uxsdk_icon) {
                val flightModeIconDimensionRatio = typedArray.getString(
                        R.styleable.FlightModeWidget_uxsdk_iconDimensionRatio,
                        getString(R.string.uxsdk_icon_flight_mode_ratio))
                setIcon(it, flightModeIconDimensionRatio)
            }
            typedArray.getDrawableAndUse(R.styleable.FlightModeWidget_uxsdk_iconBackground) {
                iconBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.FlightModeWidget_uxsdk_textBackground) {
                textBackground = it
            }
            typedArray.getDimensionAndUse(R.styleable.FlightModeWidget_uxsdk_textSize) {
                textSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.FlightModeWidget_uxsdk_connectedStateIconColor) {
                connectedStateIconColor = it
            }
            typedArray.getColorAndUse(R.styleable.FlightModeWidget_uxsdk_disconnectedStateIconColor) {
                disconnectedStateIconColor = it
            }
            typedArray.getColorAndUse(R.styleable.FlightModeWidget_uxsdk_connectedStateTextColor) {
                connectedStateTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.FlightModeWidget_uxsdk_disconnectedStateTextColor) {
                disconnectedStateTextColor = it
            }
        }
    }
    //endregion

    //region hooks
    /**
     * Get the [FlightModeWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<FlightModeWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class FlightModeWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : FlightModeWidgetState()

        /**
         * Flight mode text update
         */
        data class FlightModeTextUpdated(val flightModeText: String) : FlightModeWidgetState()
    }
    //endregion

}
