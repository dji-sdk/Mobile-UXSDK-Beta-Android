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

package dji.ux.beta.cameracore.widget.focusmode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions.LensType
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidget.ModelState
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidgetModel.FocusModeState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.FrameLayoutWidget
import dji.ux.beta.core.communication.GlobalPreferencesManager
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "FocusModeWidget"

/**
 * Widget will display the current focus mode of aircraft camera.
 * - MF text highlighted (in green) indicates focus mode is Manual Focus.
 * - AF text highlighted (in green) indicates focus mode is Auto Focus.
 * - AFC text highlighted (in green) indicates focus mode is Auto Focus Continuous.
 *
 *
 * Interaction:
 * Tapping will toggle between AF and MF mode.
 */
open class FocusModeWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayoutWidget<ModelState>(context, attrs, defStyleAttr), View.OnClickListener {

    //region Fields
    private val widgetModel: FocusModeWidgetModel by lazy {
        FocusModeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    private val titleTextView: TextView = findViewById(R.id.text_view_camera_control_af)
    private val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    /**
     * Lens type for the widget
     */
    var lensType: LensType
        get() = widgetModel.lensType
        set(lensType) {
            if (!isInEditMode) {
                widgetModel.lensType = lensType
            }
        }

    /**
     * Camera key index for which this model should subscribe to.
     */
    var cameraIndex: CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * Color for the text of active mode
     */
    @ColorInt
    var activeModeTextColor = getColor(R.color.uxsdk_green)
        set(value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Color for the text of inactive mode
     */
    @ColorInt
    var inactiveModeTextColor = getColor(R.color.uxsdk_white)
        set(value) {
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
    //endregion

    //region Lifecycle

    init {
        background = background ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        attrs?.let { initAttributes(context, it) }
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_focus_mode_switch, this)
        setOnClickListener(this)
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
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.focusModeState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun onClick(v: View) {
        if (isEnabled) {
            uiUpdateStateProcessor.onNext(UIState.WidgetClicked)
            addDisposable(widgetModel.toggleFocusMode()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Action {}, logErrorConsumer(TAG, "switch focus mode: ")))
        }
    }

    //endregion

    //region private helpers
    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.focusModeState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) },
                            { logErrorConsumer(TAG, "Update UI ") }))
        }
    }

    private fun updateUI(focusModeState: FocusModeState) {
        widgetStateDataProcessor.onNext(ModelState.FocusModeStateUpdated(focusModeState))
        when (focusModeState) {
            FocusModeState.ProductDisconnected,
            FocusModeState.CameraDisconnected,
            FocusModeState.NotSupported -> updateDisconnectedState()
            is FocusModeState.ManualFocus -> updateFocusModeString(getAutoFocusString(focusModeState.isAFCEnabled),
                    inactiveModeTextColor,
                    activeModeTextColor)
            is FocusModeState.AutoFocus -> updateFocusModeString(getAutoFocusString(focusModeState.isAFCEnabled),
                    activeModeTextColor,
                    inactiveModeTextColor)
        }
    }

    private fun updateDisconnectedState() {
        titleTextView.setTextColor(disconnectedStateTextColor)
        isEnabled = false
        hide()
    }

    private fun getAutoFocusString(isAFCEnabled: Boolean): String {
        return if (isAFCEnabled) {
            getString(R.string.uxsdk_widget_focus_mode_afc)
        } else {
            getString(R.string.uxsdk_widget_focus_mode_auto)
        }
    }

    private fun updateFocusModeString(autoFocusText: String, autoFocusColor: Int, manualFocusColor: Int) {
        val builder = SpannableStringBuilder()
        val str1 = SpannableString(autoFocusText)
        str1.setSpan(ForegroundColorSpan(autoFocusColor), 0, str1.length, 0)
        builder.append(str1)
        val str2 = SpannableString(resources.getString(R.string.uxsdk_widget_focus_mode_separator))
        str2.setSpan(ForegroundColorSpan(inactiveModeTextColor), 0, str2.length, 0)
        builder.append(str2)
        val str3 = SpannableString(resources.getString(R.string.uxsdk_widget_focus_mode_manual))
        str3.setSpan(ForegroundColorSpan(manualFocusColor), 0, str3.length, 0)
        builder.append(str3)
        titleTextView.setText(builder, TextView.BufferType.SPANNABLE)
        isEnabled = true
        show()
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.FocusModeWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.FocusModeWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(it)
            }
            typedArray.getIntegerAndUse(R.styleable.FocusModeWidget_uxsdk_lensType) {
                lensType = LensType.find(it)
            }
            typedArray.getColorAndUse(R.styleable.FocusModeWidget_uxsdk_activeModeTextColor) {
                activeModeTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.FocusModeWidget_uxsdk_inactiveModeTextColor) {
                inactiveModeTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.FocusModeWidget_uxsdk_disconnectedStateTextColor) {
                disconnectedStateTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.FocusModeWidget_uxsdk_widgetTitleTextSize) {
                titleTextSize = it
            }
            typedArray.getDrawableAndUse(R.styleable.FocusModeWidget_uxsdk_widgetTitleBackground) {
                titleBackground = it
            }
        }
    }
    //endregion

    //region customizations

    /**
     * Set background to title text
     *
     * @param resourceId resource id of background
     */
    fun setTitleBackground(@DrawableRes resourceId: Int) {
        titleBackground = getDrawable(resourceId)
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
         * Focus mode state update
         */
        data class FocusModeStateUpdated(val focusModeState: FocusModeState) : ModelState()
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