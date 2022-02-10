package dji.ux.beta.cameracore.widget.cameracontrols.lenscontrol

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.SchedulerProvider.ui
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.LogUtil
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.android.synthetic.main.uxsdk_camera_lens_control_widget.view.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/13
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class LensControlWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<LensControlWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, ICameraIndex {

    private var firstBtnSource = CameraVideoStreamSource.ZOOM
    private var secondBtnSource = CameraVideoStreamSource.WIDE
    private val currentLensArrayIndex = AtomicInteger(-1)

    private val widgetModel by lazy {
        LensControlModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_camera_lens_control_widget, this)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.cameraTypeProcessor.toFlowable().observeOn(ui()).subscribe {
            showView(it)
        })
        addReaction(widgetModel.cameraVideoStreamSourceRangeProcessor.toFlowable().observeOn(ui()).subscribe {
            updateBtnView()
        })
        first_len_btn.setOnClickListener(this)
        second_len_btn.setOnClickListener(this)
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

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {
        if (v == first_len_btn) {
            dealLensBtnClicked(firstBtnSource)
        } else if (v == second_len_btn) {
            dealLensBtnClicked(secondBtnSource)
        }
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) {
        if (widgetModel.getCameraIndex() == cameraIndex){
            return
        }
        currentLensArrayIndex.set(-1)
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    private fun showView(type: SettingsDefinitions.CameraType) {
        if (type == SettingsDefinitions.CameraType.DJICameraTypeGD610DualLight || type == SettingsDefinitions.CameraType.DJICameraTypeGD610TripleLight) {
            this.visibility = VISIBLE
        } else {
            this.visibility = GONE
        }
    }

    private fun dealLensBtnClicked(source: CameraVideoStreamSource) {
        if (source == widgetModel.cameraVideoStreamSourceProcessor.value) {
            return
        }
        addDisposable(widgetModel.setCameraVideoStreamSource(source).observeOn(ui()).subscribe {
            updateBtnView()
        })
    }

    private fun updateBtnView() {
        val cameraVideoStreamSourceRange = widgetModel.cameraVideoStreamSourceRangeProcessor.value
        if (cameraVideoStreamSourceRange.isEmpty()) {
            this.visibility = GONE
            return
        }
        this.visibility = VISIBLE
        if (cameraVideoStreamSourceRange.size <= 1) {
            updateBtnText(first_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
                firstBtnSource = it
            })
            second_len_btn.visibility = GONE
            return
        }
        second_len_btn.visibility = VISIBLE
        updateBtnText(first_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
            firstBtnSource = it
        })
        updateBtnText(second_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
            secondBtnSource = it
        })
    }

    private fun updateBtnText(button: Button, source: CameraVideoStreamSource) {
        button.text = when (source) {
            CameraVideoStreamSource.WIDE -> "WIDE"
            CameraVideoStreamSource.ZOOM -> "ZOOM"
            CameraVideoStreamSource.INFRARED_THERMAL -> "IR"
            else -> "UNKNOWN"
        }
    }

    private fun getCurrentLensArrayIndexAndIncrease(range: Int): Int {
        currentLensArrayIndex.set(currentLensArrayIndex.incrementAndGet() % range)
        return currentLensArrayIndex.get()
    }

    sealed class ModelState
}