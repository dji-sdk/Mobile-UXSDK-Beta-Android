package dji.ux.beta.cameracore.widget.cameracontrols.ranging

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import dji.common.camera.LaserError
import dji.common.camera.LaserMeasureInformation
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.model.RangeEnable
import dji.ux.beta.core.util.SettingDefinitions

class RangingWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayoutWidget<RangingWidget.ModelState>(context, attrs) {
    private var tvDroneLocation: TextView? = null
    private var tvDroneDistance: TextView? = null
    private var tvDroneAltitude: TextView? = null
    private var laserMeasureInformation: LaserMeasureInformation? = null
    private val widgetModel by lazy {
        RangingModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_ranging_distance, this)
        setBackgroundColor(getColor(R.color.uxsdk_black_80_percent))
        tvDroneLocation = findViewById(R.id.tvDroneLocation)
        tvDroneDistance = findViewById(R.id.tvRngDistance)
        tvDroneAltitude = findViewById(R.id.tvFlyAltitudeValue)
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


    @SuppressLint("SetTextI18n")
    override fun reactToModelChanges() {
        addReaction(widgetModel.getterMeasureInformation().subscribe {
            laserMeasureInformation = it
            updateData()
        })
        addReaction(widgetModel.getLaserEnableData().subscribe {
            updateRangingWidget()
        })
        addReaction(widgetModel.getThermalMeasurementMode().subscribe {
            updateRangingWidget()
        })

    }

    @SuppressLint("SetTextI18n")
    fun updateData() {
        laserMeasureInformation?.let { laserMeasureInformation ->
            val laserError = laserMeasureInformation.laserError
            if (laserError != LaserError.NORMAL) {
                tvDroneDistance?.setText(R.string.uxsdk_na)
                tvDroneLocation?.setText(R.string.uxsdk_na)
                tvDroneAltitude?.setText(R.string.uxsdk_na)
                return
            }
            val targetDistance = laserMeasureInformation.targetDistance
            val targetLocation = laserMeasureInformation.targetLocation
            val latitude = targetLocation.latitude
            val longitude = targetLocation.longitude
            val altitude = targetLocation.altitude
            tvDroneDistance?.text =
                "${targetDistance}m"
            tvDroneLocation?.text =
                "${latitude}, " + longitude
            tvDroneAltitude?.text =
                "${altitude.toDouble()}"
        }
    }


    private fun updateRangingWidget() {
        if (widgetModel.isThermalMeasurementMode()) {
            this.visibility = GONE
            return
        }
        this.visibility =
            if (widgetModel.laserEnableDataProcessor.value == RangeEnable.ENABLE) {
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

    sealed class ModelState
}