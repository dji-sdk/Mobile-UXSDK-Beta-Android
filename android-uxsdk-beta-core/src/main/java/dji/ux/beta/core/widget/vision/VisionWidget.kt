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

package dji.ux.beta.core.widget.vision

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.FrameLayoutWidget
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getDrawableAndUse
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.widget.vision.VisionWidget.VisionWidgetState
import dji.ux.beta.core.widget.vision.VisionWidget.VisionWidgetState.*

/**
 * Shows the current state of the vision system. There are two different vision systems that are
 * used by different aircraft. Older aircraft have three states that indicate whether the system is
 * enabled and working correctly. Newer aircraft such as the Mavic 2 and Mavic 2 Enterprise have an
 * omnidirectional vision system, which means they use the statuses that begin with "OMNI" to
 * indicate which directions are enabled and working correctly.
 */
open class VisionWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayoutWidget<VisionWidgetState>(
        context,
        attrs,
        defStyleAttr
) {
    //region Fields
    private val schedulerProvider: SchedulerProvider = SchedulerProvider.getInstance()
    private val visionIconImageView: ImageView = findViewById(R.id.imageview_vision_icon)
    private val visionMap: MutableMap<VisionWidgetModel.VisionSystemStatus, Drawable?> =
            mutableMapOf(
                    VisionWidgetModel.VisionSystemStatus.NORMAL to getDrawable(R.drawable.uxsdk_ic_topbar_visual_normal),
                    VisionWidgetModel.VisionSystemStatus.CLOSED to getDrawable(R.drawable.uxsdk_ic_topbar_visual_closed),
                    VisionWidgetModel.VisionSystemStatus.DISABLED to getDrawable(R.drawable.uxsdk_ic_topbar_visual_error),
                    VisionWidgetModel.VisionSystemStatus.OMNI_ALL to getDrawable(R.drawable.uxsdk_ic_avoid_normal_all),
                    VisionWidgetModel.VisionSystemStatus.OMNI_FRONT_BACK to getDrawable(R.drawable.uxsdk_ic_avoid_normal_front_back),
                    VisionWidgetModel.VisionSystemStatus.OMNI_HORIZONTAL to getDrawable(R.drawable.uxsdk_ic_omni_perception_horizontal),
                    VisionWidgetModel.VisionSystemStatus.OMNI_VERTICAL to getDrawable(R.drawable.uxsdk_ic_omni_perception_vertical),
                    VisionWidgetModel.VisionSystemStatus.OMNI_DISABLED to getDrawable(R.drawable.uxsdk_ic_avoid_disable_all),
                    VisionWidgetModel.VisionSystemStatus.OMNI_CLOSED to getDrawable(R.drawable.uxsdk_ic_avoid_disable_all))
    private val widgetModel by lazy {
        VisionWidgetModel(DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                schedulerProvider)
    }

    /**
     * The color of the vision icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Background drawable resource for the vision icon
     */
    var iconBackground: Drawable?
        get() = visionIconImageView.background
        set(value) {
            visionIconImageView.background = value
        }
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_vision, this)
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
        addReaction(widgetModel.visionSystemStatus
                .observeOn(schedulerProvider.ui())
                .subscribe { updateIcon(it) })
        addReaction(widgetModel.isUserAvoidanceEnabled
                .observeOn(schedulerProvider.ui())
                .subscribe { sendWarningMessage(it) })
        addReaction(widgetModel.isVisionSupportedByProduct
                .observeOn(schedulerProvider.ui())
                .subscribe { updateVisibility(it) })
        addReaction(widgetModel.productConnection
                .observeOn(schedulerProvider.ui())
                .subscribe { updateIconColor(it) })
    }
    //endregion

    //region Reactions
    private fun updateIcon(visionSystemStatus: VisionWidgetModel.VisionSystemStatus) {
        visionIconImageView.setImageDrawable(visionMap[visionSystemStatus])
        widgetStateDataProcessor.onNext(VisionSystemStatusUpdate(visionSystemStatus))
    }

    private fun sendWarningMessage(isUserAvoidanceEnabled: Boolean) {
        addDisposable(widgetModel.sendWarningMessage(getString(R.string.uxsdk_visual_radar_avoidance_disabled_message_post),
                        isUserAvoidanceEnabled)
                .subscribe())
        widgetStateDataProcessor.onNext(UserAvoidanceEnabledUpdate(isUserAvoidanceEnabled))
    }

    private fun updateVisibility(isVisionSupported: Boolean) {
        visibility = if (isVisionSupported) View.VISIBLE else View.GONE
        widgetStateDataProcessor.onNext(VisibilityUpdate(isVisionSupported))
    }

    private fun updateIconColor(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        if (isConnected) {
            visionIconImageView.clearColorFilter()
        } else {
            visionIconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(widgetModel.visionSystemStatus.firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { this.updateIcon(it) }, logErrorConsumer(TAG, "Update Icon ")))
        }
    }

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
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Sets the icon to the given image when the [VisionWidgetModel.VisionSystemStatus] is the
     * given value.
     *
     * @param status     The status at which the icon will change to the given image.
     * @param resourceId The id of the image the icon will change to.
     */
    fun setVisionIcon(status: VisionWidgetModel.VisionSystemStatus, @DrawableRes resourceId: Int) {
        setVisionIcon(status, getDrawable(resourceId))
    }

    /**
     * Sets the icon to the given image when the [VisionWidgetModel.VisionSystemStatus] is the
     * given value.
     *
     * @param status   The status at which the icon will change to the given image.
     * @param drawable The image the icon will change to.
     */
    fun setVisionIcon(status: VisionWidgetModel.VisionSystemStatus, drawable: Drawable?) {
        visionMap[status] = drawable
        checkAndUpdateIcon()
    }

    /**
     * Gets the image that the icon will change to when the [VisionWidgetModel.VisionSystemStatus]
     * is the given value.
     *
     * @param status The status at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    fun getVisionIcon(status: VisionWidgetModel.VisionSystemStatus): Drawable? {
        return visionMap[status]
    }

    /**
     * Set the resource ID for the vision icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setIconBackground(@DrawableRes resourceId: Int) {
        iconBackground = getDrawable(resourceId)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.VisionWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_normalVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.NORMAL, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_closedVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.CLOSED, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_disabledVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.DISABLED, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniAllVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.OMNI_ALL, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniFrontBackVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.OMNI_FRONT_BACK, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniClosedVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.OMNI_CLOSED, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniDisabledVisionIcon) {
                setVisionIcon(VisionWidgetModel.VisionSystemStatus.OMNI_DISABLED, it)
            }
            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_visionIconBackground) {
                iconBackground = it
            }
            disconnectedStateIconColor = typedArray.getColor(R.styleable.VisionWidget_uxsdk_disconnectedStateIconColor,
                    disconnectedStateIconColor)
        }
    }
    //endregion

    //region Hooks

    /**
     * Get the [VisionWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<VisionWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class VisionWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : VisionWidgetState()

        /**
         * Vision system status update
         */
        data class VisionSystemStatusUpdate(val visionSystemStatus: VisionWidgetModel.VisionSystemStatus) : VisionWidgetState()

        /**
         * Is user avoidance enabled update
         */
        data class UserAvoidanceEnabledUpdate(val isUserAvoidanceEnabled: Boolean) : VisionWidgetState()

        /**
         * Is vision supported by product update
         */
        data class VisibilityUpdate(val isVisible: Boolean) : VisionWidgetState()
    }
    //endregion

    companion object {
        private const val TAG = "VisionWidget"
    }
}