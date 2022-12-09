package dji.ux.beta.cameracore.widget.cameracontrols.cameraswitch

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.PopUtils
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.BiFunction

class CameraIRSwitchWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<CameraIRSwitchWidget.ModelState>(context, attrs, defStyleAttr) {
    private var tvDisPlayModel: TextView? = null
    private val widgetModel by lazy {
        CameraIRSwitchModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_ir_switch, this)
        tvDisPlayModel = findViewById(R.id.tvDisPlayMode)
        tvDisPlayModel?.setOnClickListener {
            val ppDisPlayModelSelect = inflate(context, R.layout.uxsdk_pp_pip_ir, null)
            val tvDisPlayMode = ppDisPlayModelSelect.findViewById<TextView>(R.id.tvNextDisPlayMode)
            PopUtils.getPp(ppDisPlayModelSelect, it)
            val value = widgetModel.disPlayModelDataProcessor.value
            if (value == SettingsDefinitions.DisplayMode.PIP) {
                tvDisPlayMode.setText(R.string.uxsdk_ir)
            } else {
                tvDisPlayMode.setText(R.string.uxsdk_split)
            }
            tvDisPlayMode.setOnClickListener {
                if (value == SettingsDefinitions.DisplayMode.PIP) {
                    widgetModel.setDisPlayMode(SettingsDefinitions.DisplayMode.THERMAL_ONLY)
                } else {
                    widgetModel.setDisPlayMode(SettingsDefinitions.DisplayMode.PIP)
                }
                PopUtils.closePP()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    override fun reactToModelChanges() {
        addReaction(Flowable.combineLatest(widgetModel.getCameraTypeModel(),
            widgetModel.getDisPlayModel(),
            BiFunction<SettingsDefinitions.CameraType, SettingsDefinitions.DisplayMode, Pair<SettingsDefinitions.CameraType, SettingsDefinitions.DisplayMode>> { cameraType, displayMode ->
                Pair<SettingsDefinitions.CameraType, SettingsDefinitions.DisplayMode>(
                    cameraType,
                    displayMode
                )
            }).observeOn(SchedulerProvider.ui()).subscribe {
            val cameraType = it.first
            if (cameraType != SettingsDefinitions.CameraType.DJICameraTypeGD610TripleLight) {
                visibility = GONE
                return@subscribe
            }

            val irCameraVideoSourceDataProcessor = widgetModel.isIRCameraVideoSourceDataProcessor
            visibility = if (irCameraVideoSourceDataProcessor.value) {
                val second = it.second
                val text = if (second == SettingsDefinitions.DisplayMode.PIP) {
                    R.string.uxsdk_split
                } else {
                    R.string.uxsdk_ir
                }
                tvDisPlayModel?.setText(text)
                VISIBLE
            } else {
                GONE
            }
        })

    }


    override fun getIdealDimensionRatioString(): String {
        return "16:9"
    }

    sealed class ModelState
}