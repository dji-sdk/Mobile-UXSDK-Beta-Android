package dji.ux.beta.cameracore.widget.resetgimbal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.PopUtils

class ResetGimbalWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayoutWidget<ResetGimbalWidget.ModelState>(context, attrs) {


    private val resetGimbalModel by lazy {
        ResetGimbalModel(context,
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_reset_gimbal_widget, this)
        this.setOnClickListener {
            val ppResetGimbalView = View.inflate(context, R.layout.uxsdk_pp_reset_gimbal, null)
            PopUtils.getPp(ppResetGimbalView, this)
            ppResetGimbalView.findViewById<View>(R.id.imgGimbalResetCenter).setOnClickListener {
                resetGimbalModel.setGimbalCenter()
                PopUtils.closePP()
            }
            ppResetGimbalView.findViewById<View>(R.id.imgGimbalResetCenterYaw).setOnClickListener {
                resetGimbalModel.setYawGimbalCenter()
                PopUtils.closePP()
            }
            ppResetGimbalView.findViewById<View>(R.id.imgGimbalResetPitchDown).setOnClickListener {
                resetGimbalModel.setPithGimbalCenter()
                PopUtils.closePP()
            }
            ppResetGimbalView.findViewById<View>(R.id.imgGimbalResetYawPitchDown)
                .setOnClickListener {
                    resetGimbalModel.setPithGimbalYawPitchDown()
                    PopUtils.closePP()
                }

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            resetGimbalModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            resetGimbalModel.cleanup()
        }
    }


    override fun reactToModelChanges() {

    }

    override fun getIdealDimensionRatioString(): String {
        return "16:9"
    }

    sealed class ModelState


}