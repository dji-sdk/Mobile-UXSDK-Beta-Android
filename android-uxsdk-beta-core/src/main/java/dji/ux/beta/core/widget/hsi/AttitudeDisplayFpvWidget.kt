package dji.ux.beta.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.core.R

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class AttitudeDisplayFpvWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AttitudeDisplayWidget(context, attrs, defStyleAttr) {

    override fun loadLayout(){
        View.inflate(context, R.layout.uxsdk_fpv_pfd_attitude_display_widget, this)
    }
}