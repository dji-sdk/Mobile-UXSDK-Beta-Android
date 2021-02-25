/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.core.panel.listitem.radioquality

import dji.common.logics.warningstatuslogic.WarningStatusItem.WarningLevel.*
import dji.common.logics.warningstatuslogic.WarningStatusLogic
import dji.common.logics.warningstatuslogic.basicsublogics.AirlinkWarningStatusLogic
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.panel.listitem.radioquality.RadioQualityListItemWidgetModel.RadioQualityState.*
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [RadioQualityListItemWidget] used to define
 * the underlying logic and communication
 */

class RadioQualityListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields

    private val radioQualityStateProcessor: DataProcessor<RadioQualityState> = DataProcessor.create(ProductDisconnected)

    private val radioLinkDiagnosticsProcessor: DataProcessor<AirlinkWarningStatusLogic.RadioLinkWarningStatusItem> =
            DataProcessor.create(AirlinkWarningStatusLogic.RadioLinkWarningStatusItem(
                    WarningStatusLogic.getInstance().warningStatusItemRCStatusNormal))

    //endregion

    //region Data
    /**
     * Get the radio quality state
     */
    val radioQualityState: Flowable<RadioQualityState>
        get() = radioQualityStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        // TODO When code is ready get diagnostics
    }

    override fun inCleanup() {
        // No Clean up
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when (radioLinkDiagnosticsProcessor.value.radioLinkWarningStatusItem.warningLevel) {
                OFFLINE -> radioQualityStateProcessor.onNext(Disabled)
                GOOD -> radioQualityStateProcessor.onNext(Normal)
                WARNING -> radioQualityStateProcessor.onNext(Warning(
                        radioLinkDiagnosticsProcessor.value.radioLinkWarningStatusItem.message
                ))
                ERROR -> radioQualityStateProcessor.onNext(Error(
                        radioLinkDiagnosticsProcessor.value.radioLinkWarningStatusItem.message))
                else -> {
                    // Do nothing
                }
            }

        } else {
            radioQualityStateProcessor.onNext(ProductDisconnected)
        }
    }

    /**
     * Class to represent states of RadioQualityListItem
     */
    sealed class RadioQualityState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : RadioQualityState()

        /**
         * When radio link is disabled
         */
        object Disabled : RadioQualityState()

        /**
         * When radio link is normal
         */
        object Normal : RadioQualityState()

        /**
         * When radio link has an active warning
         */
        data class Warning(val message: String) : RadioQualityState()

        /**
         * When radio link an active error
         */
        data class Error(val message: String) : RadioQualityState()
    }
}
//endregion