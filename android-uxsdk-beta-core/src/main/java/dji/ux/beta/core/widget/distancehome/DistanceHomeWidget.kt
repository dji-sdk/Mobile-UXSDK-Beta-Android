package dji.ux.beta.core.widget.distancehome

import android.content.Context
import android.util.AttributeSet
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.widget.BaseTelemetryWidget
import dji.ux.beta.core.communication.GlobalPreferencesManager
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.getDistanceString
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.util.UnitConversionUtil
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidget.ModelState
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidget.ModelState.DistanceHomeStateUpdated
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidget.ModelState.ProductConnected
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidgetModel.DistanceHomeState
import java.text.DecimalFormat

/**
 * Widget displays the distance between the current location of the aircraft
 * and the recorded home point.
 */
open class DistanceHomeWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseTelemetryWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.TEXT,
        widgetTheme,
        R.style.UXSDKDistanceHomeWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0")

    private val widgetModel: DistanceHomeWidgetModel by lazy {
        DistanceHomeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }
    //endregion

    //region Lifecycle

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

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.distanceHomeState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }
    //endregion

    //region Reactions to model
    private fun updateUI(distanceHomeState: DistanceHomeState) {
        widgetStateDataProcessor.onNext(DistanceHomeStateUpdated(distanceHomeState))
        if (distanceHomeState is DistanceHomeState.CurrentDistanceToHome) {
            if(distanceHomeState.unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                setValueTextViewMinWidthByText("8888")
            } else {
                setValueTextViewMinWidthByText("888.8")
            }
            valueString = getDecimalFormat(distanceHomeState.unitType)
                    .format(distanceHomeState.distance).toString()
            unitString = getDistanceString(distanceHomeState.unitType)
        } else {
            valueString = getString(R.string.uxsdk_string_default_value)
            unitString = null
        }
    }
    //endregion

    //region customizations
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    //endregion

    //region Hooks
    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * Distance to home state update
         */
        data class DistanceHomeStateUpdated(val distanceHomeState: DistanceHomeState) : ModelState()
    }
    //endregion

}