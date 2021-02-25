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

package dji.ux.beta.core.module

import androidx.annotation.IntRange
import dji.keysdk.CameraKey
import dji.sdk.camera.Camera
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.BaseModule
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions

private const val MAX_COMPONENT_INDEX = 10
private const val TAG = "LensModule"

/**
 * Abstraction for re-initializing lens keys when lens type changes.
 */
class LensModule : BaseModule() {

    //region Fields
    private val isMultiLensCameraSupportedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val displayNameProcessor: DataProcessor<String> = DataProcessor.create("")
    private var cameraIndex = SettingDefinitions.CameraIndex.CAMERA_INDEX_0.index

    private val isLensArrangementUpdatedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    //endregion

    //region Lifecycle
    override fun setup(widgetModel: WidgetModel) {
        // do nothing
        val isMultiLensCameraSupported = CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, cameraIndex)
        bindDataProcessor(widgetModel, isMultiLensCameraSupported, isMultiLensCameraSupportedProcessor, Consumer { value ->
            isLensArrangementUpdatedProcessor.onNext(value != isMultiLensCameraSupportedProcessor.value)
        })
        val displayNameKey = CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex)
        bindDataProcessor(widgetModel, displayNameKey, displayNameProcessor, Consumer { value ->
            isLensArrangementUpdatedProcessor.onNext(value != displayNameProcessor.value)
        })
    }

    override fun cleanup() {
        // do nothing
    }
    //endregion

    /**
     * True if the lens arrangement has changed, false if it has stayed the same.
     */
    fun isLensArrangementUpdated(): Flowable<Boolean> = isLensArrangementUpdatedProcessor.toFlowable()

    @SuppressWarnings("Range")
    fun createLensKey(keyName: String,
                      @IntRange(from = 0, to = MAX_COMPONENT_INDEX.toLong()) componentIndex: Int,
                      @IntRange(from = 0, to = MAX_COMPONENT_INDEX.toLong()) subComponentIndex: Int): CameraKey {
        return if (!isMultiLensCameraSupportedProcessor.value) {
            if (Camera.DisplayNameXT2_VL == displayNameProcessor.value ||
                    Camera.DisplayNameMavic2EnterpriseDual_VL == displayNameProcessor.value) {
                if (subComponentIndex == Camera.XT2_IR_CAMERA_INDEX) {
                    CameraKey.create(keyName, subComponentIndex)
                } else {
                    CameraKey.create(keyName, componentIndex)
                }
            } else {
                CameraKey.create(keyName, componentIndex)
            }
        } else {
            CameraKey.createLensKey(keyName, componentIndex, subComponentIndex)
        }
    }

    //region Customizations
    /**
     * Get the camera index for which the module is reacting.
     *
     * @return current camera index.
     */
    fun getCameraIndex(): SettingDefinitions.CameraIndex {
        return SettingDefinitions.CameraIndex.find(cameraIndex)
    }

    /**
     * Set camera index to which the module should react.
     *
     * @param cameraIndex index of the camera.
     */
    fun setCameraIndex(widgetModel: WidgetModel,
                       cameraIndex: SettingDefinitions.CameraIndex) {
        this.cameraIndex = cameraIndex.index
        restart(widgetModel)
    }

    //endregion
}