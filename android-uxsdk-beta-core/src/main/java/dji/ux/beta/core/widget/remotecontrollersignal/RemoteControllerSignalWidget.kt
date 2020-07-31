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

package dji.ux.beta.core.widget.remotecontrollersignal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.R
import dji.ux.beta.core.base.ConstraintLayoutWidget
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.widget.remotecontrollersignal.RemoteControllerSignalWidget.RemoteControllerSignalWidgetState
import dji.ux.beta.core.widget.remotecontrollersignal.RemoteControllerSignalWidget.RemoteControllerSignalWidgetState.ProductConnected
import dji.ux.beta.core.widget.remotecontrollersignal.RemoteControllerSignalWidget.RemoteControllerSignalWidgetState.SignalQualityUpdated

/**
 * This widget shows the strength of the signal between the RC and the aircraft.
 */
open class RemoteControllerSignalWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<RemoteControllerSignalWidgetState>(context, attrs, defStyleAttr) {

    //region Fields
    private val rcIconImageView: ImageView = findViewById(R.id.imageview_rc_icon)
    private val rcSignalImageView: ImageView = findViewById(R.id.imageview_rc_signal)
    private val widgetModel by lazy {
        RemoteControllerSignalWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * The color of the RC icon when the product is connected
     */
    @get:ColorInt
    var connectedStateIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * The color of the RC icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Drawable for the RC icon
     */
    var rcIcon: Drawable?
        @JvmName("getRCIcon")
        get() = rcIconImageView.drawable
        @JvmName("setRCIcon")
        set(value) {
            rcIconImageView.imageDrawable = value
        }

    /**
     * Background drawable resource for the RC icon
     */
    var rcIconBackground: Drawable?
        @JvmName("getRCIconBackground")
        get() = rcIconImageView.background
        @JvmName("setRCIconBackground")
        set(value) {
            rcIconImageView.background = value
        }

    /**
     * Drawable resource for the RC signal icon
     */
    var rcSignalIcon: Drawable?
        @JvmName("getRCSignalIcon")
        get() = rcSignalImageView.drawable
        @JvmName("setRCSignalIcon")
        set(value) {
            rcSignalImageView.imageDrawable = value
        }

    /**
     * Drawable resource for the RC signal icon's background
     */
    var rcSignalIconBackground: Drawable?
        @JvmName("getRCSignalIconBackground")
        get() = rcSignalImageView.background
        @JvmName("setRCSignalIconBackground")
        set(value) {
            rcSignalImageView.background = value
        }
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_remote_controller_signal, this)
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
        addReaction(widgetModel.rcSignalQuality
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { this.updateRCSignal(it) })
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { this.updateIconColor(it) })
    }
    //endregion

    //region Reactions to model
    private fun updateRCSignal(@IntRange(from = 0, to = 100) rcSignalQuality: Int) {
        rcSignalImageView.setImageLevel(rcSignalQuality)
        widgetStateDataProcessor.onNext(SignalQualityUpdated(rcSignalQuality))
    }

    private fun updateIconColor(isConnected: Boolean) {
        if (isConnected) {
            rcIconImageView.setColorFilter(connectedStateIconColor, PorterDuff.Mode.SRC_IN)
        } else {
            rcIconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
        }
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
    }
    //endregion

    //region helpers
    private fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.productConnection.firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateIconColor(it) }, logErrorConsumer(TAG, "Update Icon Color ")))
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_remote_control_signal_ratio)
    }

    /**
     * Set the [resourceId] for the RC icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setRCIcon(@DrawableRes resourceId: Int) {
        rcIcon = getDrawable(resourceId)
    }

    /**
     * Set the [resourceId] for the RC icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setRCIconBackground(@DrawableRes resourceId: Int) {
        rcIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the [resourceId] for the RC signal icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setRCSignalIcon(@DrawableRes resourceId: Int) {
        rcSignalIcon = getDrawable(resourceId)
    }

    /**
     * Set the [resourceId] for the RC signal icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setRCSignalIconBackground(@DrawableRes resourceId: Int) {
        rcSignalIconBackground = getDrawable(resourceId)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RemoteControllerSignalWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.RemoteControllerSignalWidget_uxsdk_rcIcon) {
                rcIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.RemoteControllerSignalWidget_uxsdk_rcSignalIcon) {
                this.rcSignalIcon = it
            }
            connectedStateIconColor = typedArray.getColor(R.styleable.RemoteControllerSignalWidget_uxsdk_connectedStateIconColor,
                    connectedStateIconColor)
            disconnectedStateIconColor = typedArray.getColor(R.styleable.RemoteControllerSignalWidget_uxsdk_disconnectedStateIconColor,
                    disconnectedStateIconColor)
        }
    }
    //endregion

    //region hooks
    /**
     * Get the [RemoteControllerSignalWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<RemoteControllerSignalWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class RemoteControllerSignalWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : RemoteControllerSignalWidgetState()

        /**
         * Signal quality update
         */
        data class SignalQualityUpdated(val signalValue: Int) : RemoteControllerSignalWidgetState()
    }
    //endregion

    companion object {
        private const val TAG = "RCSignalWidget"
    }
}
