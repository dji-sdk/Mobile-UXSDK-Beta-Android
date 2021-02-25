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

package dji.ux.beta.accessory.widget.beacon

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.ux.beta.accessory.R
import dji.thirdparty.io.reactivex.functions.Action
import dji.ux.beta.accessory.widget.beacon.BeaconWidget.*
import dji.ux.beta.accessory.widget.beacon.BeaconWidgetModel.*
import dji.ux.beta.accessory.widget.beacon.BeaconWidgetModel.BeaconState.*
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*

private const val TAG = "BeaconWidget"

/**
 * Beacon Widget
 *
 * This widget represents the state of the beacon accessory.
 * The widget is configured to be visible only when the accessory is connected
 * Tapping on the widget will toggle the beacon ON/OFF
 */
open class BeaconWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private val widgetModel: BeaconWidgetModel by lazy {
        BeaconWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance()
        )
    }

    /**
     * Beacon inactive (off) icon
     */
    var beaconInactiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_beacon_inactive)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Beacon active (on) icon
     */
    var beaconActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_beacon_active)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     *  Beacon inactive (off) icon tint color
     */
    @ColorInt
    var beaconInactiveIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     *  Beacon active (on) icon tint color
     */
    @ColorInt
    var beaconActiveIconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }
    //endregion

    //region Lifecycle
    init {
        background = background ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.beaconState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        if (isEnabled) {
            addDisposable(widgetModel.toggleBeaconState()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Action {}, logErrorConsumer(TAG, "toggleBeacon: "))
            )
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
            addDisposable(widgetModel.beaconState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) }, { logErrorConsumer(TAG, "Update UI ") }))
        }
    }
    //endregion

    //region private methods
    private fun updateUI(beaconState: BeaconState) {
        widgetStateDataProcessor.onNext(ModelState.BeaconStateUpdated(beaconState))
        when (beaconState) {
            ProductDisconnected, NotSupported -> {
                isEnabled = false
                foregroundImageView.setImageDrawable(beaconInactiveIcon)
                foregroundImageView.updateColorFilter(getDisconnectedStateIconColor())
                hide()
            }
            Inactive -> setUI(beaconInactiveIcon, beaconInactiveIconTintColor)
            Active -> setUI(beaconActiveIcon, beaconActiveIconTintColor)
        }
    }

    private fun setUI(icon: Drawable?, iconColor: Int?) {
        show()
        isEnabled = true
        foregroundImageView.setImageDrawable(icon)
        foregroundImageView.updateColorFilter(iconColor)
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.BeaconWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.BeaconWidget_uxsdk_beaconInactiveIcon) {
                beaconInactiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.BeaconWidget_uxsdk_beaconActiveIcon) {
                beaconActiveIcon = it
            }
            typedArray.getColorAndUse(R.styleable.BeaconWidget_uxsdk_beaconInactiveIconColor) {
                beaconInactiveIconTintColor = it
            }
            typedArray.getColorAndUse(R.styleable.BeaconWidget_uxsdk_beaconActiveIconColor) {
                beaconActiveIconTintColor = it
            }
        }
    }
    //endregion

    //region customization methods
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Set beacon inactive icon
     *
     * @param resourceId to be used
     */
    fun setBeaconInactiveIcon(@DrawableRes resourceId: Int) {
        beaconInactiveIcon = getDrawable(resourceId)
    }

    /**
     * Set beacon active icon
     *
     * @param resourceId to be used
     */
    fun setBeaconActiveIcon(@DrawableRes resourceId: Int) {
        beaconActiveIcon = getDrawable(resourceId)
    }

    //endregion

    //region Hooks
    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Beacon state updated
         */
        data class BeaconStateUpdated(val beaconState: BeaconState) : ModelState()
    }
    //endregion
}