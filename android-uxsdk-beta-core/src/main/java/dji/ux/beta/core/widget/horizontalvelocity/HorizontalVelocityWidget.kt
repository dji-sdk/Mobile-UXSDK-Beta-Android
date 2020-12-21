package dji.ux.beta.core.widget.horizontalvelocity

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
import dji.ux.beta.core.extension.getString
import dji.ux.beta.core.extension.getVelocityString
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState.HorizontalVelocityStateUpdated
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState.ProductConnected
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidgetModel.HorizontalVelocityState
import java.text.DecimalFormat

/**
 * Widget displays the horizontal velocity of the aircraft.
 *
 */
open class HorizontalVelocityWidget @JvmOverloads constructor(
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
        R.style.UXSDKHorizontalVelocityWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    private val widgetModel: HorizontalVelocityWidgetModel by lazy {
        HorizontalVelocityWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }
    //endregion 

    //region Constructor
    init {
        setValueTextViewMinWidthByText("88.8")
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
        addReaction(widgetModel.horizontalVelocityState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(horizontalVelocityState: HorizontalVelocityState) {
        widgetStateDataProcessor.onNext(HorizontalVelocityStateUpdated(horizontalVelocityState))
        if (horizontalVelocityState is HorizontalVelocityState.CurrentVelocity) {
            valueString = getDecimalFormat(horizontalVelocityState.unitType)
                    .format(horizontalVelocityState.velocity).toString()
            unitString = getVelocityString(horizontalVelocityState.unitType)
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
         * Horizontal velocity state update
         */
        data class HorizontalVelocityStateUpdated(val horizontalVelocityState: HorizontalVelocityState) : ModelState()
    }

    //endregion
}