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

package dji.ux.beta.core.widget.connection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.FrameLayoutWidget
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getDrawableAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.extension.imageDrawable
import dji.ux.beta.core.widget.connection.ConnectionWidget.ConnectionWidgetState
import dji.ux.beta.core.widget.connection.ConnectionWidget.ConnectionWidgetState.ProductConnected

private const val TAG = "ConnectionWidget"

/**
 * This widget displays the connection status of the app with the product.
 */
open class ConnectionWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayoutWidget<ConnectionWidgetState>(
        context,
        attrs,
        defStyleAttr
) {

    //region Fields
    /**
     * Background drawable resource for the connectivity icon
     */
    var connectivityIconBackground: Drawable?
        get() = connectivityImageView.background
        set(value) {
            connectivityImageView.background = value
        }

    /**
     * The icon when the product is connected
     */
    var connectedIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_connected)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * The icon when the product is disconnected
     */
    var disconnectedIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_disconnected)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    private val widgetModel by lazy {
        ConnectionWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    private val connectivityImageView: ImageView = findViewById(R.id.image_view_connection_status)
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_connection, this)
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
        addReaction(widgetModel.productConnection
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUI))
    }

    //endregion
    //region private functions
    private fun updateUI(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        connectivityImageView.imageDrawable = if (isConnected) {
            connectedIcon
        } else {
            disconnectedIcon
        }
    }

    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(widgetModel.productConnection.lastOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { updateUI(it) }, logErrorConsumer(TAG, "product connection")))
        }
    }
    //endregion

    //region Customizations
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Set the resource ID for the icon when the product is disconnected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setDisconnectedIcon(@DrawableRes resourceId: Int) {
        disconnectedIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the icon when the product is connected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setConnectedIcon(@DrawableRes resourceId: Int) {
        connectedIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the connectivity icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setConnectivityIconBackground(@DrawableRes resourceId: Int) {
        connectivityImageView.setBackgroundResource(resourceId)
    }
    //endregion

    //region Customization helpers
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ConnectionWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_iconBackground) {
                connectivityIconBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_connectedIcon) {
                connectedIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_disconnectedIcon) {
                disconnectedIcon = it
            }
        }
    }
    //endregion

    //region Hooks

    /**
     * Get the [ConnectionWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<ConnectionWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ConnectionWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ConnectionWidgetState()
    }
    //endregion
}