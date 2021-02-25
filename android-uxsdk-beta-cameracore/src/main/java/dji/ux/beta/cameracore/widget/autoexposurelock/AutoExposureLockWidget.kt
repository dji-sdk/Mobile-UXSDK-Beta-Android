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

package dji.ux.beta.cameracore.widget.autoexposurelock

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.LensType
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.autoexposurelock.AutoExposureLockWidgetModel.AutoExposureLockState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "AutoExposureLockWidget"

/**
 * Auto Exposure Lock Widget will display the current state of exposure lock.
 *
 *
 * When locked the exposure of the camera will remain constant.
 * Changing the exposure parameters manually will release the lock.
 */
open class AutoExposureLockWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<AutoExposureLockWidget.ModelState>(context, attrs, defStyleAttr), View.OnClickListener {

    private var foregroundImageView: ImageView = findViewById(R.id.auto_exposure_lock_widget_foreground_image_view)
    private var titleTextView: TextView = findViewById(R.id.auto_exposure_lock_widget_title_text_view)
    private val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    private val widgetModel: AutoExposureLockWidgetModel by lazy {
        AutoExposureLockWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * Type of the lens for which the widget should react
     */
    var lensType: LensType
        get() = widgetModel.lensType
        set(lensType) {
            if (!isInEditMode) {
                widgetModel.lensType = lensType
            }
        }

    /**
     * The color of the icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(dji.ux.beta.core.R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    @get:ColorInt
    var autoExposureLockIconColor: Int = INVALID_COLOR
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    @get:ColorInt
    var autoExposureUnlockIconColor: Int = INVALID_COLOR
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    var autoExposureLockIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_auto_exposure_lock)
        set(value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    var autoExposureUnlockIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_auto_exposure_unlock)
        set(value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * Background of ImageView
     */
    var iconBackground: Drawable?
        get() = foregroundImageView.background
        set(value) {
            foregroundImageView.background = value
        }

    /**
     * Text size of the widget text
     *
     */
    @get:Dimension
    var titleTextSize: Float
        get() = titleTextView.textSize
        set(textSize) {
            titleTextView.textSize = textSize
        }

    /**
     * background to title text
     */
    var titleBackground: Drawable?
        get() = titleTextView.background
        set(value) {
            titleTextView.background = value
        }

    /**
     * The color of the text when the product is disconnected
     */
    @get:ColorInt
    var disconnectedTextColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the text when the product is disconnected
     */
    @get:ColorInt
    var autoExposureLockedTextColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    /**
     * The color of the text when the product is disconnected
     */
    @get:ColorInt
    var autoExposureUnlockedTextColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateAELock()
        }

    //endregion
    //region Lifecycle


    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_auto_exposure_lock, this)
        background = background ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        setOnClickListener(this)
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.autoExposureLockState.observeOn(SchedulerProvider.ui()).subscribe { updateUI(it) })
    }

    private fun updateUI(autoExposureLockState: AutoExposureLockState) {
        widgetStateDataProcessor.onNext(ModelState.AutoExposureStateUpdated(autoExposureLockState))
        when (autoExposureLockState) {
            AutoExposureLockState.ProductDisconnected,
            AutoExposureLockState.CameraDisconnected -> updateDisconnectedState()
            AutoExposureLockState.Unlocked ->
                updateConnectedState(autoExposureUnlockIcon,
                        autoExposureUnlockIconColor, autoExposureUnlockedTextColor)
            AutoExposureLockState.Locked -> updateConnectedState(autoExposureLockIcon,
                    autoExposureLockIconColor, autoExposureLockedTextColor)
        }
    }

    private fun updateConnectedState(icon: Drawable, iconColor: Int, textColor: Int) {
        show()
        isEnabled = true
        foregroundImageView.imageDrawable = icon
        if (iconColor != INVALID_COLOR) {
            foregroundImageView.setColorFilter(iconColor)
        } else {
            foregroundImageView.clearColorFilter()
        }
        titleTextView.setTextColor(textColor)
    }

    private fun updateDisconnectedState() {
        hide()
        isEnabled = false
        foregroundImageView.imageDrawable = autoExposureUnlockIcon
        if (disconnectedStateIconColor != INVALID_COLOR) {
            foregroundImageView.setColorFilter(disconnectedStateIconColor)
        } else {
            foregroundImageView.clearColorFilter()
        }
        titleTextView.setTextColor(disconnectedTextColor)
    }

    override fun onClick(v: View) {
        if (isEnabled) {
            uiUpdateStateProcessor.onNext(UIState.WidgetClicked)
            toggleExposureLock()
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

    //endregion
    //region private methods


    private fun toggleExposureLock() {
        addDisposable(widgetModel.toggleAutoExposureLock()
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action {}, logErrorConsumer(TAG, "set auto exposure lock: ")))
    }

    private fun checkAndUpdateAELock() {
        if (!isInEditMode) {
            addDisposable(widgetModel.autoExposureLockState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { updateUI(it) }, logErrorConsumer(TAG, "Update AE Lock ")))
        }
    }

    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.AutoExposureLockWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.AutoExposureLockWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.AutoExposureLockWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
            typedArray.getDimensionAndUse(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextSize) {
                titleTextSize = it
            }
            typedArray.getDrawableAndUse(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleBackground) {
                titleBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextAppearance) {
                setTitleTextAppearance(it)
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockTextColor) {
                autoExposureUnlockedTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockTextColor) {
                autoExposureLockedTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_disconnectedTextColor) {
                disconnectedTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_disconnectedIconColor) {
                disconnectedStateIconColor = it
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockIconColor) {
                autoExposureUnlockIconColor = it
            }
            typedArray.getColorAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockIconColor) {
                autoExposureLockIconColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockIcon) {
                autoExposureUnlockIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockIcon) {
                autoExposureLockIcon = it
            }
        }
    }

    //endregion
    //region customization
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_auto_exposure_lock_ratio)
    }


    /**
     * Set background to icon
     *
     * @param resourceId to be used
     */
    fun setIconBackground(@DrawableRes resourceId: Int) {
        iconBackground = getDrawable(resourceId)
    }

    /**
     * Set drawable for auto exposure lock in locked state
     *
     * @param resourceId to be used
     */
    fun setAutoExposureLockIcon(@DrawableRes resourceId: Int) {
        autoExposureLockIcon = getDrawable(resourceId)
    }

    /**
     * Set drawable for auto exposure lock in unlocked state
     *
     * @param resourceId to be used
     */
    fun setAutoExposureUnlockIcon(@DrawableRes resourceId: Int) {
        autoExposureUnlockIcon = getDrawable(resourceId)
    }


    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance to be used
     */
    fun setTitleTextAppearance(@StyleRes textAppearance: Int) {
        titleTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background to title text
     *
     * @param resourceId to be used
     */
    fun setTitleBackground(@DrawableRes resourceId: Int) {
        titleBackground = getDrawable(resourceId)
    }


    override fun setEnabled(enabled: Boolean) {
        titleTextView.isEnabled = enabled
        foregroundImageView.isEnabled = enabled
        super.setEnabled(enabled)
    }

    //endregion

    //region hooks
    /**
     * Get the [UIState] updates
     */
    fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
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
         * Auto exposure lock state update
         */
        data class AutoExposureStateUpdated(val autoExposureLockState: AutoExposureLockState) : ModelState()
    }

    /**
     * Class defines the UI state updates
     */
    sealed class UIState {
        /**
         * Widget click update
         */
        object WidgetClicked : UIState()
    }
    //endregion

}