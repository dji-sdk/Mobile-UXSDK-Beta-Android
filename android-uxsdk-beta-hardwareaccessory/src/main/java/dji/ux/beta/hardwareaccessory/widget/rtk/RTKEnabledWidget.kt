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
import android.os.Build
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.res.use
import dji.log.DJILog
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.disposables.Disposable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.core.base.ConstraintLayoutWidget
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.hardwareaccessory.R
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKEnabledWidget.RTKEnabledWidgetState
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKEnabledWidget.RTKEnabledWidgetState.ProductConnected
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKEnabledWidget.RTKEnabledWidgetState.RTKEnabledUpdate
import dji.ux.beta.hardwareaccessory.widget.rtk.RTKEnabledWidget.RTKEnabledWidgetUIState.RTKEnabledSwitchCheckChanged

private const val TAG = "RTKEnabledWidget"

/**
 * This widget displays a switch that will enable or disable RTK.
 */
open class RTKEnabledWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<RTKEnabledWidgetState>(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    //region Fields
    private val rtkTitleTextView: TextView = findViewById(R.id.textview_rtk_title)
    private val rtkEnabledSwitch: Switch = findViewById(R.id.switch_rtk_enabled)
    private val rtkEnabledDescriptionTextView: TextView = findViewById(R.id.textview_rtk_enabled_description)
    private val uiUpdateStateProcessor: PublishProcessor<RTKEnabledWidgetUIState> = PublishProcessor.create()

    private val widgetModel by lazy {
        RTKEnabledWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                SchedulerProvider.getInstance())
    }

    /**
     * Background of the title text
     */
    var titleTextBackground: Drawable?
        get() = rtkTitleTextView.background
        set(value) {
            rtkTitleTextView.background = value
        }

    /**
     * Size of the title text
     */
    var titleTextSize: Float
        @Dimension
        get() = rtkTitleTextView.textSize
        set(@Dimension textSize) {
            rtkTitleTextView.textSize = textSize
        }

    /**
     * Color of the title text
     */
    var titleTextColor: Int
        @ColorInt
        get() = rtkTitleTextView.textColor
        set(@ColorInt textColor) {
            rtkTitleTextView.textColor = textColor
        }

    /**
     * The drawable resource for the RTK enabled switch's thumb
     */
    var rtkEnabledSwitchThumbIcon: Drawable
        @JvmName("getRTKEnabledSwitchThumbIcon")
        get() = rtkEnabledSwitch.thumbDrawable
        @JvmName("setRTKEnabledSwitchThumbIcon")
        set(value) {
            rtkEnabledSwitch.thumbDrawable = value
        }

    /**
     * The drawable resource for the RTK enabled switch's track
     */
    var rtkEnabledSwitchTrackIcon: Drawable
        @JvmName("getRTKEnabledSwitchTrackIcon")
        get() = rtkEnabledSwitch.trackDrawable
        @JvmName("setRTKEnabledSwitchTrackIcon")
        set(value) {
            rtkEnabledSwitch.trackDrawable = value
        }

    /**
     * The text color state list for the RTK enabled switch's track
     */
    var rtkEnabledSwitchTrackColor: ColorStateList?
        @RequiresApi(Build.VERSION_CODES.M)
        @JvmName("getRTKEnabledSwitchTrackColor")
        get() = rtkEnabledSwitch.trackTintList
        @RequiresApi(Build.VERSION_CODES.M)
        @JvmName("setRTKEnabledSwitchTrackColor")
        set(value) {
            rtkEnabledSwitch.trackTintList = value
        }

    /**
     * Background of the description text
     */
    var descriptionTextBackground: Drawable?
        get() = rtkEnabledDescriptionTextView.background
        set(value) {
            rtkEnabledDescriptionTextView.background = value
        }

    /**
     * Size of the description text
     */
    var descriptionTextSize: Float
        @Dimension
        get() = rtkEnabledDescriptionTextView.textSize
        set(@Dimension textSize) {
            rtkEnabledDescriptionTextView.textSize = textSize
        }

    /**
     * Color of the description text
     */
    var descriptionTextColor: Int
        @ColorInt
        get() = rtkEnabledDescriptionTextView.textColor
        set(@ColorInt textColor) {
            rtkEnabledDescriptionTextView.textColor = textColor
        }
    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk_enabled, this)
    }

    init {
        rtkEnabledSwitch.setOnCheckedChangeListener(this)
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

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        addDisposable(widgetModel.canEnableRTK.firstOrError()
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe(Consumer { canEnableRTK: Boolean ->
                    if (!canEnableRTK) {
                        setRTKSwitch(!isChecked)
                        showLongToast(R.string.uxsdk_rtk_enabled_motors_running)
                    } else {
                        setRTKEnabled(isChecked)
                    }
                }, logErrorConsumer(TAG, "canEnableRTK: ")))
        uiUpdateStateProcessor.onNext(RTKEnabledSwitchCheckChanged(isChecked))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.rtkEnabled
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe { updateUIForRTKEnabled(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }
    //endregion

    //region Reaction helpers
    private fun updateUIForRTKEnabled(rtkEnabled: Boolean) {
        setRTKSwitch(rtkEnabled)
        widgetStateDataProcessor.onNext(RTKEnabledUpdate(rtkEnabled))
    }

    private fun setRTKEnabled(enabled: Boolean) {
        addDisposable(widgetModel.rtkEnabled
                .firstOrError()
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe(Consumer { rtkEnabled: Boolean ->
                    if (rtkEnabled != enabled) {
                        addDisposable(toggleRTK(enabled))
                    }
                }, logErrorConsumer(TAG, "rtkEnabled: ")))
    }

    private fun toggleRTK(enabled: Boolean): Disposable {
        return widgetModel.setRTKEnabled(enabled)
                .observeOn(SchedulerProvider.getInstance().ui())
                .subscribe({}
                ) { throwable: Throwable ->
                    setRTKSwitch(!enabled)
                    DJILog.e(TAG, "setRTKEnabled: " + throwable.localizedMessage)
                }
    }

    private fun setRTKSwitch(isChecked: Boolean) {
        rtkEnabledSwitch.setOnCheckedChangeListener(null)
        rtkEnabledSwitch.isChecked = isChecked
        rtkEnabledSwitch.setOnCheckedChangeListener(this)
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_rtk_enabled_ratio)
    }

    /**
     * Set text appearance of the title text
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setTitleTextAppearance(@StyleRes textAppearanceResId: Int) {
        rtkTitleTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the resource ID for the RTK enabled switch's thumb
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setRTKEnabledSwitchThumbIcon(@DrawableRes resourceId: Int) {
        rtkEnabledSwitchThumbIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the RTK enabled switch's track
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setRTKEnabledSwitchTrackIcon(@DrawableRes resourceId: Int) {
        rtkEnabledSwitchTrackIcon = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the description text
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setDescriptionTextAppearance(@StyleRes textAppearanceResId: Int) {
        rtkEnabledDescriptionTextView.setTextAppearance(context, textAppearanceResId)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RTKEnabledWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.RTKEnabledWidget_uxsdk_titleTextAppearance) {
                setTitleTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKEnabledWidget_uxsdk_titleTextSize) {
                titleTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKEnabledWidget_uxsdk_titleTextColor) {
                titleTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKEnabledWidget_uxsdk_titleTextBackground) {
                titleTextBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKEnabledWidget_uxsdk_rtkEnabledSwitchThumbIcon) {
                rtkEnabledSwitchThumbIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKEnabledWidget_uxsdk_rtkEnabledSwitchTrackIcon) {
                rtkEnabledSwitchTrackIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.RTKEnabledWidget_uxsdk_descriptionTextAppearance) {
                setDescriptionTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKEnabledWidget_uxsdk_descriptionTextSize) {
                descriptionTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKEnabledWidget_uxsdk_descriptionTextColor) {
                descriptionTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKEnabledWidget_uxsdk_descriptionTextBackground) {
                descriptionTextBackground = it
            }
        }
    }
    //endregion

    //region Hooks
    /**
     * Get the [RTKEnabledWidgetUIState] updates
     */
    fun getUIStateUpdates(): Flowable<RTKEnabledWidgetUIState> {
        return uiUpdateStateProcessor
    }

    /**
     * Widget UI update State
     */
    sealed class RTKEnabledWidgetUIState {
        /**
         * RTK enabled switch check changed update
         */
        data class RTKEnabledSwitchCheckChanged(val isChecked: Boolean) : RTKEnabledWidgetUIState()
    }

    /**
     * Get the [RTKEnabledWidgetState] updates
     */
    override fun getWidgetStateUpdate(): Flowable<RTKEnabledWidgetState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class RTKEnabledWidgetState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : RTKEnabledWidgetState()

        /**
         * RTK enabled update
         */
        data class RTKEnabledUpdate(val isRTKEnabled: Boolean) : RTKEnabledWidgetState()
    }
    //endregion
}