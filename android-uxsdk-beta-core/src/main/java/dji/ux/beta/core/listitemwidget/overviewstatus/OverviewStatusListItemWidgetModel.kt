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

package dji.ux.beta.core.listitemwidget.overviewstatus

import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.keysdk.DJIKey
import dji.keysdk.DiagnosticsKey
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore
import dji.ux.beta.core.listitemwidget.overviewstatus.OverviewListItemWidgetModel.OverviewState.CurrentStatus
import dji.ux.beta.core.util.DataProcessor

/**
 * Widget Model for the [OverviewListItemWidget] used to define
 * the underlying logic and communication
 */
class OverviewListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val systemStatusProcessor: DataProcessor<WarningStatusItem> = DataProcessor.create(WarningStatusItem.getDefaultItem())
    private val overviewStateProcessor: DataProcessor<OverviewState> = DataProcessor.create(OverviewState.ProductDisconnected)

    /**
     * Get the overview status
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    val overviewStatus: Flowable<OverviewState>
        get() = overviewStateProcessor.toFlowable()

    override fun inSetup() {
        val systemStatusKey: DJIKey = DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS)
        bindDataProcessor(systemStatusKey, systemStatusProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            overviewStateProcessor.onNext(CurrentStatus(systemStatusProcessor.value))
        } else {
            overviewStateProcessor.onNext(OverviewState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        // No Code required
    }

    sealed class OverviewState {

        object ProductDisconnected : OverviewState()

        data class CurrentStatus(val warningStatusItem: WarningStatusItem) : OverviewState()
    }


}