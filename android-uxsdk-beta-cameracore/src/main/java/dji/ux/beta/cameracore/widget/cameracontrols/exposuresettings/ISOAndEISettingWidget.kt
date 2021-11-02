package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ISOAndEISettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener {

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {

    }

    override fun reactToModelChanges() {
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {
    }

    sealed class ModelState
}