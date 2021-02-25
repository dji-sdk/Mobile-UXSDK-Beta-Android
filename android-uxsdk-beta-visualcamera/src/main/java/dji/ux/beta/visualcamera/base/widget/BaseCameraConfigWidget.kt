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

package dji.ux.beta.visualcamera.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.res.use
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.extension.*
import dji.ux.beta.visualcamera.R

abstract class BaseCameraConfigWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        protected val widgetTheme: Int = 0,
        @StyleRes protected val defaultStyle: Int
) : ConstraintLayoutWidget<T>(context, attrs, defStyleAttr) {

    private val labelTextView: TextView = findViewById(R.id.text_view_camera_config_label)
    private val valueTextView: TextView = findViewById(R.id.text_view_camera_config_value)

    /**
     * Color of the value is in normal state
     */
    @ColorInt
    var normalValueColor: Int = getColor(dji.ux.beta.core.R.color.uxsdk_white)

    /**
     * Color of label text when disconnected
     */
    @ColorInt
    var disconnectedValueColor: Int = getColor(dji.ux.beta.core.R.color.uxsdk_white_60_percent)

    //region label customizations
    /**
     * String value of label
     */
    var labelString: String?
        @Nullable get() = labelTextView.text.toString()
        set(value) {
            labelTextView.text = value
        }

    /**
     * Float text size of the label
     */
    var labelTextSize: Float
        @Dimension get() = labelTextView.textSize
        set(@Dimension value) {
            labelTextView.textSize = value
        }

    /**
     * Integer color for label
     */
    var labelTextColor: Int
        @ColorInt get() = labelTextView.textColor
        set(@ColorInt value) {
            labelTextView.textColor = value
        }

    /**
     * Color state list of the label
     */
    var labelTextColors: ColorStateList?
        get() = labelTextView.textColorStateList
        set(value) {
            labelTextView.textColorStateList = value
        }

    /**
     * Background of the label
     */
    var labelBackground: Drawable?
        get() = labelTextView.background
        set(value) {
            labelTextView.background = value
        }

    /**
     * Visibility of the label
     */
    var labelVisibility: Boolean
        get() = labelTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                labelTextView.show()
            } else {
                labelTextView.hide()
            }
        }

    //endregion

    //region value customizations
    /**
     * String for value text view
     */
    var valueString: String?
        @Nullable get() = valueTextView.text.toString()
        set(value) {
            valueTextView.text = value
        }

    /**
     * Float text size of the value
     */
    var valueTextSize: Float
        @Dimension get() = valueTextView.textSize
        set(@Dimension value) {
            valueTextView.textSize = value
        }

    /**
     * Integer color for value
     */
    var valueTextColor: Int
        @ColorInt get() = valueTextView.textColor
        set(@ColorInt value) {
            valueTextView.textColor = value
        }

    /**
     * Color state list of the value
     */
    var valueTextColors: ColorStateList?
        get() = valueTextView.textColorStateList
        set(value) {
            valueTextView.textColorStateList = value
        }

    /**
     * Background of the value
     */
    var valueBackground: Drawable?
        get() = valueTextView.background
        set(value) {
            valueTextView.background = value
        }

    /**
     * Visibility of the value
     */
    var valueVisibility: Boolean
        get() = valueTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                valueTextView.show()
            } else {
                valueTextView.hide()
            }
        }

    /**
     * Text position of the value
     */
    var valueTextGravity: Int
        get() = valueTextView.gravity
        set(value) {
            valueTextView.gravity = value
        }

    //endregion

    init {
        initBaseCameraConfigAttributes(context)
        initAttributes(context, attrs)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_base_camera_info, this)
    }

    @SuppressLint("Recycle")
    private fun initBaseCameraConfigAttributes(context: Context) {
        val baseCameraConfigAttributeArray: IntArray = R.styleable.BaseCameraConfigWidget
        context.obtainStyledAttributes(widgetTheme, baseCameraConfigAttributeArray).use {
            initAttributesByTypedArray(it)
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.BaseCameraConfigWidget, 0, defaultStyle).use { typedArray ->
            initAttributesByTypedArray(typedArray)
        }
    }

    private fun initAttributesByTypedArray(typedArray: TypedArray) {
        typedArray.getResourceIdAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_label_text_appearance) {
            setLabelTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_label_text_size) {
            labelTextSize = it
        }
        typedArray.getColorAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_label_text_color) {
            labelTextColor = it
        }
        typedArray.getColorStateListAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_label_text_color) {
            labelTextColors = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_label_background) {
            labelBackground = it
        }
        labelVisibility = typedArray.getBoolean(R.styleable.BaseCameraConfigWidget_uxsdk_label_visibility,
                labelVisibility)
        labelString =
                typedArray.getString(R.styleable.BaseCameraConfigWidget_uxsdk_label_string,
                        getString(R.string.uxsdk_string_default_value))

        typedArray.getResourceIdAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_text_appearance) {
            setValueTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_text_size) {
            valueTextSize = it
        }
        typedArray.getColorAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_text_color) {
            valueTextColor = it
        }
        typedArray.getColorStateListAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_text_color) {
            valueTextColors = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_background) {
            valueBackground = it
        }
        valueVisibility = typedArray.getBoolean(R.styleable.BaseCameraConfigWidget_uxsdk_value_visibility,
                valueVisibility)
        typedArray.getIntegerAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_gravity) {
            valueTextGravity = it
        }
        valueString =
                typedArray.getString(R.styleable.BaseCameraConfigWidget_uxsdk_value_string,
                        getString(R.string.uxsdk_string_default_value))
        typedArray.getColorAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_normal_color) {
            normalValueColor = it
        }
        typedArray.getColorAndUse(R.styleable.BaseCameraConfigWidget_uxsdk_value_disconnected_color) {
            disconnectedValueColor = it
        }
    }

    /**
     * Set the background of the label
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setLabelBackground(@DrawableRes resourceId: Int) {
        labelBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the label
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setLabelTextAppearance(@StyleRes textAppearanceResId: Int) {
        labelTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background of the value
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setValueBackground(@DrawableRes resourceId: Int) {
        valueBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the value
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setValueTextAppearance(@StyleRes textAppearanceResId: Int) {
        valueTextView.setTextAppearance(context, textAppearanceResId)
    }

    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_base_camera_info_ratio)
    }
}