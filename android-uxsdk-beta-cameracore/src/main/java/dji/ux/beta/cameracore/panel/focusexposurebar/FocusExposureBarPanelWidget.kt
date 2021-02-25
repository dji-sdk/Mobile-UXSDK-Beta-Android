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

package dji.ux.beta.cameracore.panel.focusexposurebar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.autoexposurelock.AutoExposureLockWidget
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget
import dji.ux.beta.cameracore.widget.focusmode.FocusModeWidget

import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.panel.BarPanelWidget
import dji.ux.beta.core.base.panel.PanelItem
import dji.ux.beta.core.base.panel.PanelWidgetConfiguration
import dji.ux.beta.core.extension.getDimension
import dji.ux.beta.core.extension.getIntegerAndUse
import java.util.*

/**
 * Container for the Focus and Exposure Control widgets.
 * The widget is built with only right side of bar panel.
 *
 * * Customization:
 * Use the attribute "excludeItem" to prevent a certain item from being created and shown
 * throughout the lifecycle of the bar panel. Here are all the flags:
 * focus_exposure_switch, auto_exposure_lock, focus_mode.
 *
 * Note that multiple flags can be used simultaneously by logically OR'ing them.
 *
 * This panel widget also passes attributes to each of the child widgets created. See each
 * individual's widget documentation for more customization options.
 */
open class FocusExposureBarPanelWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        barPanelWidgetOrientation: BarPanelWidgetOrientation = BarPanelWidgetOrientation.HORIZONTAL
) : BarPanelWidget<Any>(context, attrs, defStyleAttr, barPanelWidgetOrientation) {

    //region Widgets Properties
    /**
     * Getter for the [FocusExposureSwitchWidget]. Null when excluded from the bar panel.
     */
    val focusExposureSwitchWidget: FocusExposureSwitchWidget?

    /**
     * Getter for the [FocusModeWidget]. Null when excluded from the bar panel.
     */
    val focusModeWidget: FocusModeWidget?

    /**
     * Getter for the [AutoExposureLockWidget]. Null when excluded from the bar panel.
     */
    val autoExposureLockWidget: AutoExposureLockWidget?

    //endregion

    //region Private properties
    private var excludedItemsValue = 0
    //endregion

    //region Lifecycle & Setup
    /**
     * Invoked during the initialization of the class.
     * Inflate should be done here. For Kotlin, load attributes, findViewById should be done in
     * the init block.
     *
     * @param context      Context
     * @param attrs        Attribute set
     * @param defStyleAttr Style attribute
     */
    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // Nothing to do
    }

    init {
        val rightPanelItems = ArrayList<PanelItem>()
        if (!WidgetValue.FOCUS_MODE.isItemExcluded(excludedItemsValue)) {
            focusModeWidget = FocusModeWidget(context, attrs)
            rightPanelItems.add(PanelItem(focusModeWidget))
        } else {
            focusModeWidget = null
        }
        if (!WidgetValue.FOCUS_EXPOSURE_SWITCH.isItemExcluded(excludedItemsValue)) {
            focusExposureSwitchWidget = FocusExposureSwitchWidget(context, attrs)
            rightPanelItems.add(PanelItem(focusExposureSwitchWidget))
        } else {
            focusExposureSwitchWidget = null
        }

        if (!WidgetValue.AUTO_EXPOSURE_LOCK.isItemExcluded(excludedItemsValue)) {
            autoExposureLockWidget = AutoExposureLockWidget(context, attrs)
            rightPanelItems.add(PanelItem(autoExposureLockWidget))
        } else {
            autoExposureLockWidget = null
        }


        addRightWidgets(rightPanelItems.toTypedArray())
    }

    @SuppressLint("Recycle")
    override fun initAttributes(attrs: AttributeSet) {
        setGuidelinePercent(0.0f, 0.0f)
        itemSpacing = getDimension(R.dimen.uxsdk_focus_exposure_bar_spacing).toInt()

        context.obtainStyledAttributes(attrs, R.styleable.FocusExposureBarPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.FocusExposureBarPanelWidget_uxsdk_excludeFocusExposureBarItem) {
                excludedItemsValue = it
            }
        }

        super.initAttributes(attrs)
    }

    /**
     * Call addReaction here to bind to the model.
     */
    override fun reactToModelChanges() {
        // Nothing to do
    }

    //endregion

    //region Customization
    /**
     * Ideal dimension ratio in the format width:height.
     *
     * @return dimension ratio string.
     */
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(
                    WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.EXPAND)

    //endregion

    private enum class WidgetValue(val value: Int) {
        AUTO_EXPOSURE_LOCK(1),
        FOCUS_MODE(2),
        FOCUS_EXPOSURE_SWITCH(4);

        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }
    }
}