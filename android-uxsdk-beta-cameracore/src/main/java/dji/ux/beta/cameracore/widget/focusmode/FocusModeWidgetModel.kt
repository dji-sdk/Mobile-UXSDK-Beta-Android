/*
 * Copyright (c) 2018-2021 DJI
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

package dji.ux.beta.cameracore.widget.focusmode

import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.LensType
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "FocusModeWidgetModel"

/**
 * Focus Mode Widget Model
 *
 *
 * Widget Model for the [FocusModeWidget] used to define the
 * underlying logic and communication
 */
class FocusModeWidgetModel(
        djiSdkModel: DJISDKModel,
        val keyedStore: ObservableInMemoryKeyedStore,
        val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isAFCSupportedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isAFCEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val focusModeDataProcessor: DataProcessor<SettingsDefinitions.FocusMode> = DataProcessor.create(SettingsDefinitions.FocusMode.UNKNOWN)
    private val focusModeStateDataProcessor: DataProcessor<FocusModeState> = DataProcessor.create(FocusModeState.ProductDisconnected)
    private val controlModeProcessor: DataProcessor<SettingDefinitions.ControlMode> = DataProcessor.create(SettingDefinitions.ControlMode.SPOT_METER)
    private lateinit var focusModeKey: DJIKey
    private lateinit var controlModeKey: UXKey
    private lateinit var afcEnabledKey: UXKey
    private val lensModule = LensModule()
    /**
     * Camera index for which the model is reacting.
     */
    var cameraIndex: CameraIndex = CameraIndex.find(CameraIndex.CAMERA_INDEX_0.index)
        set(value) {
            field = value
            lensModule.setCameraIndex(this, value)
            restart()
        }

    /**
     * The lens the widget model is reacting to
     */
    var lensType: LensType = LensType.ZOOM
        set(value) {
            field = value
            restart()
        }

    /**
     * Current [FocusModeState]
     */
    val focusModeState: Flowable<FocusModeState>
        get() = focusModeStateDataProcessor.toFlowable()

    //endregion

    //region Lifecycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        focusModeKey = lensModule.createLensKey(CameraKey.FOCUS_MODE, cameraIndex.index, lensType.value())
        bindDataProcessor(focusModeKey, focusModeDataProcessor)
        val cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        val isAFCSupportedKey: DJIKey = lensModule.createLensKey(CameraKey.IS_AFC_SUPPORTED, cameraIndex.index, lensType.value())
        bindDataProcessor(isAFCSupportedKey, isAFCSupportedProcessor)
        afcEnabledKey = UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED)
        bindDataProcessor(afcEnabledKey, isAFCEnabledProcessor)
        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE)
        bindDataProcessor(controlModeKey, controlModeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.controlMode?.let { controlModeProcessor.onNext(it) }

        addDisposable(djiSdkModel.getValue(focusModeKey).observeOn(SchedulerProvider.io())
                .subscribe({ focusMode ->
                    when (focusMode) {
                        SettingsDefinitions.FocusMode.AFC -> setAFCEnabled(true)
                        SettingsDefinitions.FocusMode.AUTO -> setAFCEnabled(false)
                        else -> {
                            preferencesManager?.afcEnabled?.let { isAFCEnabledProcessor.onNext(it) }
                        }
                    }
                }, {
                    logErrorConsumer(TAG, "Get default AFC Enabled ")
                }))

        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))

    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if(cameraConnectionDataProcessor.value) {
                if (djiSdkModel.isKeySupported(focusModeKey)
                        && focusModeDataProcessor.value != SettingsDefinitions.FocusMode.UNKNOWN) {
                    val isAFCEnabled = isAFCEnabledProcessor.value && isAFCSupportedProcessor.value
                    if (focusModeDataProcessor.value == SettingsDefinitions.FocusMode.MANUAL) {
                        focusModeStateDataProcessor.onNext(FocusModeState.ManualFocus(isAFCEnabled))
                    } else {
                        focusModeStateDataProcessor.onNext(FocusModeState.AutoFocus(isAFCEnabled))
                    }
                } else {
                    focusModeStateDataProcessor.onNext(FocusModeState.NotSupported)
                }
            } else {
                focusModeStateDataProcessor.onNext(FocusModeState.CameraDisconnected)
            }
        } else {
            focusModeStateDataProcessor.onNext(FocusModeState.ProductDisconnected)
        }
    }

    //endregion
    //region Actions
    /**
     * Switch between focus modes
     *
     * @return Completable representing the success/failure of set action
     */
    fun toggleFocusMode(): Completable {
        val currentFocusMode = focusModeDataProcessor.value
        val nextFocusMode = getNextFocusMode(currentFocusMode)
        return djiSdkModel.setValue(focusModeKey, nextFocusMode)
                .doOnComplete { onFocusModeUpdate(nextFocusMode) }
    }

    //endregion

    //region Private helpers
    private fun setAFCEnabled(isEnabled: Boolean) {
        preferencesManager?.afcEnabled = isEnabled
        addDisposable(keyedStore.setValue(afcEnabledKey, isEnabled)
                .subscribe(Action {}, logErrorConsumer(TAG, "set AFC: ")))
    }

    private fun getNextFocusMode(currentFocusMode: SettingsDefinitions.FocusMode): SettingsDefinitions.FocusMode {
        return if (currentFocusMode == SettingsDefinitions.FocusMode.MANUAL) {
            if (isAFCSupportedProcessor.value && isAFCEnabledProcessor.value) {
                SettingsDefinitions.FocusMode.AFC
            } else {
                SettingsDefinitions.FocusMode.AUTO
            }
        } else {
            SettingsDefinitions.FocusMode.MANUAL
        }
    }

    private fun onFocusModeUpdate(focusMode: SettingsDefinitions.FocusMode) {
        if (controlModeProcessor.value == SettingDefinitions.ControlMode.SPOT_METER ||
                controlModeProcessor.value == SettingDefinitions.ControlMode.CENTER_METER) {
            return
        }
        when (focusMode) {
            SettingsDefinitions.FocusMode.AUTO -> {
                preferencesManager?.controlMode = SettingDefinitions.ControlMode.AUTO_FOCUS
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS)
                        .subscribe(Action {}, logErrorConsumer(TAG, "setControlModeAutoFocus: ")))
            }
            SettingsDefinitions.FocusMode.AFC -> {
                preferencesManager?.controlMode = SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE)
                        .subscribe(Action {}, logErrorConsumer(TAG, "setControlModeAutoFocusContinuous: ")))
            }
            SettingsDefinitions.FocusMode.MANUAL -> {
                preferencesManager?.controlMode = SettingDefinitions.ControlMode.MANUAL_FOCUS
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.MANUAL_FOCUS)
                        .subscribe(Action {}, logErrorConsumer(TAG, "setControlModeManualFocus: ")))
            }
            else -> {
            }
        }
    }
    //endregion

    /**
     * Class defines states of Focus Mode Widget
     */
    sealed class FocusModeState {
        /**
         * Product is disconnected.
         */
        object ProductDisconnected : FocusModeState()

        /**
         * Camera is disconnected.
         */
        object CameraDisconnected : FocusModeState()

        /**
         * Product connected but does not support switching focus mode.
         */
        object NotSupported : FocusModeState()

        /**
         * Product is in Manual Focus Mode.
         */
        data class ManualFocus(val isAFCEnabled: Boolean) : FocusModeState()

        /**
         * Product is Auto Focus Mode.
         */
        data class AutoFocus(val isAFCEnabled: Boolean) : FocusModeState()
    }
}