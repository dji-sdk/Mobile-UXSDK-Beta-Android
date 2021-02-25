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

package dji.ux.beta.cameracore.widget.focusexposureswitch

import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.LensType
import dji.common.camera.SettingsDefinitions.MeteringMode
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.log.DJILog
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidgetModel.FocusExposureSwitchState.*
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.communication.*
import dji.ux.beta.core.module.LensModule
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.core.util.SettingDefinitions.ControlMode

private const val TAG = "FocusExpoSwitchWidMod"

/**
 * Focus Exposure Switch Widget Model
 *
 *
 * Widget Model for the [FocusExposureSwitchWidget] used to define the
 * underlying logic and communication
 */
class FocusExposureSwitchWidgetModel(
        djiSdkModel: DJISDKModel,
        val keyedStore: ObservableInMemoryKeyedStore,
        val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val focusModeDataProcessor: DataProcessor<SettingsDefinitions.FocusMode> = DataProcessor.create(SettingsDefinitions.FocusMode.UNKNOWN)
    private val cameraConnectionDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val meteringModeDataProcessor: DataProcessor<MeteringMode> = DataProcessor.create(MeteringMode.UNKNOWN)
    private val controlModeDataProcessor: DataProcessor<ControlMode> = DataProcessor.create(ControlMode.SPOT_METER)
    private val focusExposureSwitchStateProcessor: DataProcessor<FocusExposureSwitchState> = DataProcessor.create(ProductDisconnected)
    private lateinit var focusModeKey: DJIKey
    private lateinit var meteringModeKey: DJIKey
    private lateinit var controlModeKey: UXKey
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
     * Focus exposure switch state
     */
    val focusExposureSwitchState: Flowable<FocusExposureSwitchState>
        get() = focusExposureSwitchStateProcessor.toFlowable()
    //endregion

    //region Lifecycle
    init {
        addModule(lensModule)
    }

    override fun inSetup() {
        DJILog.d(TAG, "In inSetup")
        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE)
        val cameraConnectionKey = CameraKey.create(CameraKey.CONNECTION, cameraIndex.index)
        focusModeKey = lensModule.createLensKey(CameraKey.FOCUS_MODE, cameraIndex.index, lensType.value())
        meteringModeKey = lensModule.createLensKey(CameraKey.METERING_MODE, cameraIndex.index, lensType.value())
        bindDataProcessor(cameraConnectionKey, cameraConnectionDataProcessor)
        bindDataProcessor(focusModeKey, focusModeDataProcessor)
        bindDataProcessor(meteringModeKey, meteringModeDataProcessor)
        bindDataProcessor(controlModeKey, controlModeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.controlMode?.let { controlModeDataProcessor.onNext(it) }
        addDisposable(lensModule.isLensArrangementUpdated()
                .observeOn(SchedulerProvider.io())
                .subscribe(Consumer { value: Boolean ->
                    if (value) {
                        restart()
                    }
                }, logErrorConsumer(TAG, "on lens arrangement updated")))

    }

    override fun inCleanup() {
        DJILog.d(TAG, "In inCleanup")
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        DJILog.d(TAG, "In updateStates")
        if (productConnectionProcessor.value) {
            if (cameraConnectionDataProcessor.value) {
                if (djiSdkModel.isKeySupported(focusModeKey)) {
                    focusExposureSwitchStateProcessor.onNext(ControlModeState(controlModeDataProcessor.value))
                } else {
                    focusExposureSwitchStateProcessor.onNext(NotSupported)
                }
            } else {
                focusExposureSwitchStateProcessor.onNext(CameraDisconnected)
            }
        } else {
            focusExposureSwitchStateProcessor.onNext(ProductDisconnected)
        }
    }

    //endregion

    //region Private helpers
    private fun setMeteringMode(): Completable {
        return djiSdkModel.setValue(meteringModeKey, MeteringMode.SPOT)
                .doOnComplete {
                    DJILog.d(TAG, "setMeteringMode success")
                    preferencesManager?.controlMode = ControlMode.SPOT_METER
                    addDisposable(keyedStore.setValue(controlModeKey, ControlMode.SPOT_METER)
                            .subscribe(Action {}, logErrorConsumer(TAG, "setMeteringMode: ")))
                    DJILog.d(TAG, "Success")
                }.doOnError { error: Throwable ->
                    DJILog.d(TAG, "setMeteringMode error")
                    setFocusMode()
                    DJILog.d(TAG, "Fail $error")
                }
    }

    private fun setFocusMode(): Completable {
        return when (focusModeDataProcessor.value) {
            SettingsDefinitions.FocusMode.MANUAL -> {
                preferencesManager?.controlMode = ControlMode.MANUAL_FOCUS
                keyedStore.setValue(controlModeKey, ControlMode.MANUAL_FOCUS)
            }
            SettingsDefinitions.FocusMode.AFC -> {
                preferencesManager?.controlMode = ControlMode.AUTO_FOCUS_CONTINUE
                keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS_CONTINUE)
            }
            else -> {
                preferencesManager?.controlMode = ControlMode.AUTO_FOCUS
                keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS)
            }
        }
    }
    //endregion

    //region Action
    /**
     * Switch between exposure/metering mode and focus mode
     *
     * @return Completable representing the success/failure of the set action.
     */
    fun switchControlMode(): Completable {
        DJILog.d(TAG, "switchControlMode")
        val currentControlMode = controlModeDataProcessor.value
        return if (currentControlMode == ControlMode.SPOT_METER || currentControlMode == ControlMode.CENTER_METER) {
            setFocusMode()
        } else {
            setMeteringMode()
        }
    }

    //endregion

    /**
     * Class defines states of Focus Exposure Switch.
     */
    sealed class FocusExposureSwitchState {
        /**
         * Product is disconnected.
         */
        object ProductDisconnected : FocusExposureSwitchState()

        /**
         * Camera is disconnected.
         */
        object CameraDisconnected : FocusExposureSwitchState()

        /**
         * Product connected but does not support switch to focus mode.
         */
        object NotSupported : FocusExposureSwitchState()

        /**
         * Product is connected and control mode is updated.
         */
        data class ControlModeState(val controlMode: ControlMode) : FocusExposureSwitchState()

    }
}