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

package dji.ux.beta.core.panel.listitem.unittype

import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.panel.listitem.unittype.UnitModeListItemWidgetModel.UnitTypeState.CurrentUnitType
import dji.ux.beta.core.panel.listitem.unittype.UnitModeListItemWidgetModel.UnitTypeState.ProductDisconnected
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.UnitConversionUtil.UnitType

/**
 * Widget Model for the [UnitModeListItemWidget] used to define
 * the underlying logic and communication
 */
class UnitModeListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val unitTypeStateProcessor: DataProcessor<UnitTypeState> = DataProcessor.create(ProductDisconnected)
    private val unitTypeProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val unitTypeKey: UXKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)

    /**
     * Get the unit type state
     */
    val unitTypeState: Flowable<UnitTypeState>
        get() = unitTypeStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(unitTypeKey, unitTypeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            unitTypeStateProcessor.onNext(CurrentUnitType(unitTypeProcessor.value))
        } else {
            unitTypeStateProcessor.onNext(ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Set unit type
     */
    fun setUnitType(unitType: UnitType): Completable {
        if (unitType == unitTypeProcessor.value) return Completable.complete()
        preferencesManager?.unitType = unitType
        return uxKeyManager.setValue(unitTypeKey, unitType)
    }

    /**
     * Class represents states of Unit Type
     */
    sealed class UnitTypeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : UnitTypeState()

        /**
         * Represents current unit system used
         */
        data class CurrentUnitType(val unitType: UnitType) : UnitTypeState()
    }

}