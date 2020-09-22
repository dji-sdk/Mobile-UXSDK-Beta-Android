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

package dji.ux.beta.core.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.Switch
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import dji.thirdparty.io.reactivex.processors.PublishProcessor
import dji.ux.beta.R
import dji.ux.beta.core.extension.getDrawable
import dji.ux.beta.core.extension.getDrawableAndUse
import dji.ux.beta.core.util.ViewIDGenerator

/**
 * This is the base class to be used for
 * switch type list item.
 */
abstract class ListItemSwitchWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemTitleWidget<T>(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    //region fields
    private val listItemSwitch: Switch = Switch(context)
    protected val uiUpdateStateProcessor: PublishProcessor<WidgetUIState> = PublishProcessor.create()

    /**
     * The icon used for thumb for list item switch
     */
    var switchThumbIcon: Drawable? = listItemSwitch.thumbDrawable

    /**
     * The icon used for track for list item switch
     */
    var switchTrackIcon: Drawable? = listItemSwitch.trackDrawable

    /**
     * The background of the list item switch
     */
    var switchBackground: Drawable? = listItemSwitch.background
    //endregion

    init {
        configureSwitchWidget()
        val paddingValue = resources.getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        setContentPadding(0, paddingValue, 0, paddingValue)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ListItemSwitchWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.ListItemSwitchWidget_uxsdk_list_item_switch_background) {
                switchBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemSwitchWidget_uxsdk_list_item_switch_thumb_icon) {
                switchThumbIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemSwitchWidget_uxsdk_list_item_switch_track_icon) {
                switchTrackIcon = it
            }

        }
    }

    private fun configureSwitchWidget() {
        initSwitch()
        val layoutParams = LayoutParams(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT)
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        listItemSwitch.layoutParams = layoutParams
        addView(listItemSwitch)
    }

    private fun initSwitch() {
        listItemSwitch.id = ViewIDGenerator.generateViewId()
        listItemSwitch.setOnCheckedChangeListener(this)
        listItemSwitch.setThumbResource(R.drawable.uxsdk_selector_switch_thumb)
        listItemSwitch.setTrackResource(R.drawable.uxsdk_switch_background)
    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == listItemSwitch) {
            uiUpdateStateProcessor.onNext(WidgetUIState.SwitchToggle)
            onSwitchToggle(isChecked)
        }
    }

    override fun onListItemClick() {
        uiUpdateStateProcessor.onNext(WidgetUIState.ListItemClick)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        listItemSwitch.isEnabled = enabled
    }

    abstract fun onSwitchToggle(isChecked: Boolean)

    protected fun setChecked(isSwitchChecked: Boolean) {
        listItemSwitch.setOnCheckedChangeListener(null)
        listItemSwitch.isChecked = isSwitchChecked
        listItemSwitch.setOnCheckedChangeListener(this)
    }

    /**
     * Set the resource ID for the list item switch thumb icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSwitchThumbIcon(@DrawableRes resourceId: Int) {
        switchThumbIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the list item switch track icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSwitchTrackIcon(@DrawableRes resourceId: Int) {
        switchTrackIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the list item switch background
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSwitchBackground(@DrawableRes resourceId: Int) {
        switchBackground = getDrawable(resourceId)
    }


    /**
     * Widget UI update State
     */
    sealed class WidgetUIState {
        /**
         * List Item click update
         */
        object ListItemClick : WidgetUIState()

        /**
         * Button click update
         */
        object SwitchToggle : WidgetUIState()

        /**
         *  Dialog shown update
         */
        data class DialogDisplayed(val info: Any?) : WidgetUIState()

        /**
         *  Dialog action confirm
         */
        data class DialogActionConfirm(val info: Any?) : WidgetUIState()

        /**
         *  Dialog action dismiss
         */
        data class DialogActionDismiss(val info: Any?) : WidgetUIState()
    }

}