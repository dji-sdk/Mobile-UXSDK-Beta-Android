package dji.ux.beta.cameracore.widget.cameracontrols.lenscontrol

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.SchedulerProvider.ui
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.SettingDefinitions
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

    private var firstBtnDisplayMode = SettingsDefinitions.DisplayMode.VISUAL_ONLY
    private var secondBtnDisPlayMode = SettingsDefinitions.DisplayMode.THERMAL_ONLY
    private var associateView: View? = null
    private var resourceId = 0;
    private val widgetModel by lazy {
        LensControlModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_camera_lens_control_widget, this)
        this.visibility = GONE
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.cameraTypeProcessor.toFlowable().observeOn(ui()).subscribe {
            showView(it)
        })
        addReaction(widgetModel.cameraVideoStreamSourceRangeProcessor.toFlowable().observeOn(ui()).subscribe {
            updateBtnView()
        })
        addDisposable(widgetModel.cameraVideoStreamSourceProcessor.toFlowable().observeOn(ui()).subscribe {
            updateBtnView()
        })
        addDisposable(widgetModel.getDisPlayMode().observeOn(ui()).subscribe {
            updateBtnViewDisPlayMode()
        })
        addDisposable(widgetModel.getDisPlayModeRange().observeOn(ui()).subscribe {
            updateBtnViewDisPlayMode()
        })
        first_len_btn.setOnClickListener(this)
        second_len_btn.setOnClickListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        if (resourceId != 0) {
            associateView = rootView.findViewById(resourceId)
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
            if (widgetModel.cameraDisPlayModeRange.value.isEmpty()) {
                dealLensBtnClicked(firstBtnSource)
                return
            }
            dealLensDisPlayModeBtnClicked(firstBtnDisplayMode)
        } else if (v == second_len_btn) {
            if (widgetModel.cameraDisPlayModeRange.value.isEmpty()) {
                dealLensBtnClicked(secondBtnSource)
                return
            }
            dealLensDisPlayModeBtnClicked(secondBtnDisPlayMode)

        }
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) {
        if (widgetModel.getCameraIndex() == cameraIndex){
            return
        }
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    private fun showView(type: SettingsDefinitions.CameraType) {
        if (type == SettingsDefinitions.CameraType.DJICameraTypeWM247 || type == SettingsDefinitions.CameraType.DJICameraTypeGD610DualLight || type == SettingsDefinitions.CameraType.DJICameraTypeGD610TripleLight) {
            this.visibility = VISIBLE
        } else {
            this.visibility = GONE
        }
        setVisibleLensControl()

    }

    private fun setVisibleLensControl() {
        associateView?.let {
            val visibility = it.visibility
            if (visibility != VISIBLE) {
                this.visibility = GONE
            }
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

    private fun dealLensDisPlayModeBtnClicked(disPlayMode: SettingsDefinitions.DisplayMode) {
        if (disPlayMode == widgetModel.cameraDisPlayMode.value) {
            return
        }
        addDisposable(widgetModel.setCameraDisPlayMode(disPlayMode).observeOn(ui()).subscribe {
            updateBtnViewDisPlayMode()
        })
    }


    private fun updateBtnView() {

        try {
            val cameraType = widgetModel.cameraTypeProcessor.value
            if (cameraType == SettingsDefinitions.CameraType.OTHER) {
                return
            }
            val cameraVideoStreamSourceRange = widgetModel.cameraVideoStreamSourceRangeProcessor.value
            if (cameraVideoStreamSourceRange.isEmpty() || cameraVideoStreamSourceRange.size <= 1) {
                this.visibility = GONE
                first_len_btn.visibility = INVISIBLE
                second_len_btn.visibility = INVISIBLE
                return
            }
            val cameraVideoStreamSourceValue = widgetModel.cameraVideoStreamSourceProcessor.value
            if (cameraVideoStreamSourceValue == CameraVideoStreamSource.UNKNOWN) return
            this.visibility = VISIBLE
            setVisibleLensControl()
            first_len_btn.visibility = VISIBLE
            second_len_btn.visibility = INVISIBLE
            if (cameraVideoStreamSourceRange.size == 2) {
                val currentLensArray = getCurrentLensArray(
                    widgetModel.cameraVideoStreamSourceProcessor.value.value(),
                    1
                )
                val cameraVideoStreamSourceIndex = currentLensArray[0]
                updateBtnText(first_len_btn, cameraVideoStreamSourceRange[cameraVideoStreamSourceIndex!!].also {
                        firstBtnSource = it
                    })
                second_len_btn.visibility = INVISIBLE
                return
            }
            second_len_btn.visibility = VISIBLE
            val currentLensArray =
                getCurrentLensArray(widgetModel.cameraVideoStreamSourceProcessor.value.value(), 2)
            val cameraVideoStreamSourceIndex = currentLensArray[0]
            val cameraVideoStreamSourceIndex2 = currentLensArray[1]
            updateBtnText(
                first_len_btn,
                cameraVideoStreamSourceRange[cameraVideoStreamSourceIndex!!].also {
                    firstBtnSource = it
                })
            updateBtnText(
                second_len_btn,
                cameraVideoStreamSourceRange[cameraVideoStreamSourceIndex2!!].also {
                    secondBtnSource = it
                })

        } catch (e: Exception) {

        }
    }

    private fun updateBtnViewDisPlayMode() {
        try {
            val cameraType = widgetModel.cameraTypeProcessor.value
            if (cameraType != SettingsDefinitions.CameraType.DJICameraTypeWM247) {
                return
            }
            val cameraDisPlayModeRange = widgetModel.cameraDisPlayModeRange.value
            if (cameraDisPlayModeRange.isEmpty() || cameraDisPlayModeRange.size <= 1) {
                this.visibility = GONE
                first_len_btn.visibility = INVISIBLE
                second_len_btn.visibility = INVISIBLE
                return
            }
            val cameraDisplayMode = widgetModel.cameraDisPlayMode.value
            if (cameraDisplayMode == SettingsDefinitions.DisplayMode.OTHER) return
            this.visibility = VISIBLE
            setVisibleLensControl()
            first_len_btn.visibility = VISIBLE
            second_len_btn.visibility = INVISIBLE
            if (cameraDisPlayModeRange.size == 2) {
                val currentLensArray =
                    getCurrentDisPlayLensArray(widgetModel.cameraDisPlayMode.value.value(), 1)
                val cameraDisplayModeIndex = currentLensArray[0]
                updateDisplayModeBtnText(
                    first_len_btn,
                    cameraDisPlayModeRange[cameraDisplayModeIndex!!].also {
                        firstBtnDisplayMode = it
                    })
                second_len_btn.visibility = INVISIBLE
                return
            }
            second_len_btn.visibility = VISIBLE
            val currentLensArray =
                getCurrentDisPlayLensArray(widgetModel.cameraDisPlayMode.value.value(), 2)
            val cameraDisplayModeIndex = currentLensArray[0]
            val cameraDisplayModeIndex2 = currentLensArray[1]
            updateDisplayModeBtnText(
                first_len_btn,
                cameraDisPlayModeRange[cameraDisplayModeIndex!!].also {
                    firstBtnDisplayMode = it
                })
            updateDisplayModeBtnText(
                second_len_btn,
                cameraDisPlayModeRange[cameraDisplayModeIndex2!!].also {
                    secondBtnDisPlayMode = it
                })
        } catch (e: Exception) {

        }

    }

    private fun updateBtnText(button: TextView, source: CameraVideoStreamSource) {
        button.text = when (source) {
            CameraVideoStreamSource.WIDE -> "WIDE"
            CameraVideoStreamSource.ZOOM -> "ZOOM"
            CameraVideoStreamSource.INFRARED_THERMAL -> "IR"
            else -> "UNKNOWN"
        }
    }

    private fun updateDisplayModeBtnText(
        button: TextView,
        source: SettingsDefinitions.DisplayMode
    ) {
        button.text = when (source) {
            SettingsDefinitions.DisplayMode.PIP -> "Split"
            SettingsDefinitions.DisplayMode.THERMAL_ONLY -> "IR"
            SettingsDefinitions.DisplayMode.VISUAL_ONLY -> "WIDE"
            else -> "UNKNOWN"
        }
    }

    private fun getCurrentLensArray(cameraVideoSource: Int, size: Int): Array<Int?> {
        val index =
            widgetModel.cameraVideoStreamSourceRangeProcessor.value.indexOfFirst { it.value() == cameraVideoSource }
        val lens = arrayOfNulls<Int>(size)
        when (size) {
            1 -> {
                if (index == 1) {
                    lens[0] = 0
                } else {
                    lens[0] = 1
                }
            }
            2 -> {
                when (index) {
                    0 -> {
                        lens[0] = 1
                        lens[1] = 2
                    }

                    1 -> {
                        lens[0] = 0
                        lens[1] = 2
                    }
                    2 -> {
                        lens[0] = 0
                        lens[1] = 1
                    }
                }
            }
        }
        return lens
    }

    private fun getCurrentDisPlayLensArray(cameraVideoSource: Int, size: Int): Array<Int?> {
        val index =
            widgetModel.cameraDisPlayModeRange.value.indexOfFirst { it.value() == cameraVideoSource }
        val lens = arrayOfNulls<Int>(size)
        when (size) {
            1 -> {
                if (index == 1) {
                    lens[0] = 0
                } else {
                    lens[0] = 1
                }
            }
            2 -> {
                when (index) {
                    0 -> {
                        lens[0] = 1
                        lens[1] = 2
                    }

                    1 -> {
                        lens[0] = 0
                        lens[1] = 2
                    }
                    2 -> {
                        lens[0] = 0
                        lens[1] = 1
                    }
                }
            }
        }
        return lens
    }


    sealed class ModelState
}