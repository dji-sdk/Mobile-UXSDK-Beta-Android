package dji.ux.beta.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.uxsdk_primary_flight_display_widget.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class PrimaryFlightDisplayWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<PrimaryFlightDisplayWidget.ModelState>(context, attrs, defStyleAttr) {

    private val widgetModel by lazy {
        PrimaryFlightDisplayModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_primary_flight_display_widget, this)
    }

    override fun reactToModelChanges() {
        addDisposable(widgetModel.velocityXProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setSpeedX(it.toFloat())
        })
        addDisposable(widgetModel.velocityYProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setSpeedY(it.toFloat())
        })
        addDisposable(widgetModel.velocityZProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setSpeedZ(it.toFloat())
        })
        addDisposable(widgetModel.attitudePitchProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setPitch(it.toFloat())
        })
        addDisposable(widgetModel.attitudeRollProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setRoll(it.toFloat())
        })
        addDisposable(widgetModel.attitudeYawProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            fpv_attitude.setYaw(it.toFloat())
        })
        setVideoViewSize(1440, 1080)
    }

    //可以通过fpvWidget获取真实的video长宽比
    fun setVideoViewSize(videoViewWidth: Int, videoViewHeight: Int) {
        fpv_attitude.setVideoViewSize(videoViewWidth, videoViewHeight)
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
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

    sealed class ModelState
}