package dji.ux.beta.cameracore.widget.cameracontrols.ranging

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.model.RangeEnable
import dji.ux.beta.core.util.SettingDefinitions

class RangingIndicatorWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayoutWidget<RangingWidget.ModelState>(context, attrs) {
    private lateinit var imgRangingIndicator: ImageView
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_ranging_indicator, this)
        imgRangingIndicator = findViewById(R.id.imgRangView)
    }

    val widgetModel by lazy {
        RangingIndicatorModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        this.setOnClickListener {
            var rangeEnable = widgetModel.laserEnableGoalDataProcessor.value
            rangeEnable = if (rangeEnable == RangeEnable.DISABLES) {
                RangeEnable.ENABLE
            } else {
                RangeEnable.DISABLES
            }
            widgetModel.setLaserEnable(rangeEnable)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.laserEnable().subscribe {
            imgRangingIndicator.setImageResource(if (it == RangeEnable.ENABLE) {
                R.drawable.ic_rng_select
            } else {
                R.drawable.ic_rng_normal
            }
            )

        })
        addReaction(widgetModel.isSupportLaser().subscribe {
            updateRangingIndicatorWidget()
        })
    }

    fun updateRangingIndicatorWidget() {
        this.visibility =
            if (widgetModel.isSupportLaserDataProcessor.value) {
                VISIBLE
            } else {
                GONE
            }
    }


    fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }


    override fun getIdealDimensionRatioString(): String {
        return "16:9"
    }
}