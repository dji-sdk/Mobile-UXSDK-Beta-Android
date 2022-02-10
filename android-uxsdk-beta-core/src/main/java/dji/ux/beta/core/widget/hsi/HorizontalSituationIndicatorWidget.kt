package dji.ux.beta.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.core.R
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class HorizontalSituationIndicatorWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<HorizontalSituationIndicatorWidget.ModelState>(context, attrs, defStyleAttr) {

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_fpv_view_horizontal_situation_indicator, this)
    }

    override fun reactToModelChanges() {
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    sealed class ModelState
}