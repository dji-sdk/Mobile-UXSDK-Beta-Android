package dji.ux.beta.core.module

import dji.common.camera.SettingsDefinitions.*
import dji.keysdk.CameraKey
import dji.thirdparty.io.reactivex.Completable
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.base.BaseModule
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.WidgetModel
import dji.ux.beta.core.extension.isPictureMode
import dji.ux.beta.core.extension.toFlatCameraMode
import dji.ux.beta.core.extension.toShootPhotoMode
import dji.ux.beta.core.util.DataProcessor
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

/**
 * Abstraction for getting and setting camera mode and photo mode.
 */
class FlatCameraModule : BaseModule() {

    //region Fields
    private val isFlatCameraModeSupportedDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val flatCameraModeDataProcessor: DataProcessor<FlatCameraMode> = DataProcessor.create(FlatCameraMode.UNKNOWN)
    private var cameraIndex = CameraIndex.CAMERA_INDEX_0.index
    private var cameraModeKey: CameraKey = CameraKey.create(CameraKey.MODE, cameraIndex)
    private var shootPhotoModeKey: CameraKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex)
    private var flatCameraModeKey: CameraKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex)

    /**
     * The camera mode.
     */
    val cameraModeDataProcessor: DataProcessor<CameraMode> = DataProcessor.create(CameraMode.UNKNOWN)

    /**
     *  The shoot photo mode.
     */
    val shootPhotoModeProcessor: DataProcessor<ShootPhotoMode> = DataProcessor.create(ShootPhotoMode.UNKNOWN)
    //endregion

    //region Lifecycle
    override fun setup(widgetModel: WidgetModel) {
        cameraModeKey = CameraKey.create(CameraKey.MODE, cameraIndex)
        bindDataProcessor(widgetModel, cameraModeKey, cameraModeDataProcessor)
        shootPhotoModeKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex)
        bindDataProcessor(widgetModel, shootPhotoModeKey, shootPhotoModeProcessor)

        val isFlatCameraModeSupportedKey = CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex)
        bindDataProcessor(widgetModel, isFlatCameraModeSupportedKey, isFlatCameraModeSupportedDataProcessor, Consumer { isFlatCameraModeSupported ->
            if (isFlatCameraModeSupported as Boolean) {
                updateModes(flatCameraModeDataProcessor.value)
            }
        })
        flatCameraModeKey = CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex)
        bindDataProcessor(widgetModel, flatCameraModeKey, flatCameraModeDataProcessor, Consumer { flatCameraMode: Any ->
            if (isFlatCameraModeSupportedDataProcessor.value) {
                updateModes(flatCameraMode as FlatCameraMode)
            }
        })
    }

    override fun cleanup() {
        // no code
    }
    //endregion

    //region Actions
    /**
     * Set camera mode
     *
     * @return Completable
     */
    fun setCameraMode(djiSdkModel: DJISDKModel, cameraMode: CameraMode): Completable {
        return if (isFlatCameraModeSupportedDataProcessor.value) {
            if (cameraMode == CameraMode.SHOOT_PHOTO) {
                djiSdkModel.setValue(flatCameraModeKey, FlatCameraMode.PHOTO_SINGLE)
            } else {
                djiSdkModel.setValue(flatCameraModeKey, FlatCameraMode.VIDEO_NORMAL)
            }
        } else {
            djiSdkModel.setValue(cameraModeKey, cameraMode)
        }
    }

    /**
     * Set photo mode
     *
     * @return Completable
     */
    fun setPhotoMode(djiSdkModel: DJISDKModel, photoMode: ShootPhotoMode): Completable {
        return if (isFlatCameraModeSupportedDataProcessor.value) {
            djiSdkModel.setValue(flatCameraModeKey, photoMode.toFlatCameraMode())
        } else {
            djiSdkModel.setValue(shootPhotoModeKey, photoMode)
        }
    }
    //endregion

    //region Customizations
    /**
     * Get the camera index for which the module is reacting.
     *
     * @return current camera index.
     */
    fun getCameraIndex(): CameraIndex {
        return CameraIndex.find(cameraIndex)
    }

    /**
     * Set camera index to which the module should react.
     *
     * @param cameraIndex index of the camera.
     */
    fun setCameraIndex(cameraIndex: CameraIndex) {
        this.cameraIndex = cameraIndex.index
    }
    //endregion

    //region Helpers
    private fun updateModes(flatCameraMode: FlatCameraMode) {
        cameraModeDataProcessor.onNext(if (flatCameraMode.isPictureMode()) {
            CameraMode.SHOOT_PHOTO
        } else {
            CameraMode.RECORD_VIDEO
        })
        shootPhotoModeProcessor.onNext(flatCameraMode.toShootPhotoMode())
    }
    //endregion
}