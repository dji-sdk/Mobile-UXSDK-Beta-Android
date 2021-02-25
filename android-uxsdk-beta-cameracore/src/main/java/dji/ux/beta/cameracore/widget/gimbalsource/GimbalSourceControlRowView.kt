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

package dji.ux.beta.cameracore.widget.gimbalsource

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import dji.common.airlink.PhysicalSource
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.util.ViewUtil

class GimbalSourceControlRowView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var gimbalDotImageView: ImageView
    private var gimbalNameTextView: TextView
    private var transmissionCheckBox: CheckBox

    var isCheckBoxEnabled: Boolean
        get() = transmissionCheckBox.isEnabled
        set(value) {
            transmissionCheckBox.isEnabled = value
            transmissionCheckBox.alpha = if (value) checkBoxEnabledAlpha else checkBoxDisabledAlpha
        }

    var isChecked
        get() = transmissionCheckBox.isChecked
        set(value) {
            transmissionCheckBox.isChecked = value
        }

    /**
     * The alpha of the check box when it's disabled
     */
    var checkBoxDisabledAlpha = 0.3f

    /**
     * The alpha of the check box when it's enabled
     */
    var checkBoxEnabledAlpha = 1.0f

    init {
        inflate(context, R.layout.uxsdk_view_gimbal_source_control_row, this)
        gimbalDotImageView = findViewById(R.id.imageview_gimbal_dot)
        gimbalNameTextView = findViewById(R.id.textview_gimbal_name)
        transmissionCheckBox = findViewById(R.id.checkbox_transmission)
    }

    fun updateUI(physicalSource: PhysicalSource, cameraName: String) {
        gimbalNameTextView.text = getGimbalIndexPref(physicalSource, cameraName)
        ViewUtil.tintImage(gimbalDotImageView, getGimbalDotColor(physicalSource))
    }

    fun setOnCheckedChangeListener(onCheckedChangeListener: (GimbalSourceControlRowView, Boolean) -> Unit) {
        transmissionCheckBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            onCheckedChangeListener(this, isChecked)
        }
    }

    //region Customizations
    fun setCheckBoxColor(@ColorInt uncheckedColor: Int, @ColorInt checkedColor: Int) {
        ViewUtil.tintCheckBox(transmissionCheckBox, uncheckedColor, checkedColor)
    }
    //endregion

    //region Helpers
    private fun getGimbalIndexPref(physicalSource: PhysicalSource, cameraName: String): String {
        return when (physicalSource) {
            PhysicalSource.MAIN_CAM,
            PhysicalSource.LEFT_CAM -> resources.getString(R.string.uxsdk_gimbal_source_item_name_1, cameraName)
            PhysicalSource.RIGHT_CAM -> resources.getString(R.string.uxsdk_gimbal_source_item_name_2, cameraName)
            PhysicalSource.TOP_CAM -> resources.getString(R.string.uxsdk_gimbal_source_item_name_3, cameraName)
            PhysicalSource.FPV_CAM -> getString(R.string.uxsdk_gimbal_source_item_name_fpv)
            else -> ""
        }
    }

    @ColorInt
    private fun getGimbalDotColor(physicalSource: PhysicalSource): Int {
        return when (physicalSource) {
            PhysicalSource.MAIN_CAM,
            PhysicalSource.LEFT_CAM -> getColor(R.color.uxsdk_gimbal_source_1)
            PhysicalSource.RIGHT_CAM -> getColor(R.color.uxsdk_gimbal_source_2)
            PhysicalSource.TOP_CAM -> getColor(R.color.uxsdk_gimbal_source_3)
            else -> getColor(R.color.uxsdk_transparent)
        }
    }
    //endregion
}