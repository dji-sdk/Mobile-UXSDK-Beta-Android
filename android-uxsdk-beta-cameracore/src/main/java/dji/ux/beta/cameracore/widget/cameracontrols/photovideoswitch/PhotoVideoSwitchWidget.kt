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

package dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import dji.log.DJILog
import dji.thirdparty.io.reactivex.functions.Action
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget.ModelState
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget.ModelState.PhotoVideoSwitchUpdated
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget.ModelState.ProductConnected
import dji.ux.beta.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidgetModel.PhotoVideoSwitchState
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.IconButtonWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex

private const val TAG = "PhotoVideoSwitchWidget"

/**
 * Widget can be used to switch between shoot photo mode and record video mode
 */
open class PhotoVideoSwitchWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private val widgetModel: PhotoVideoSwitchWidgetModel by lazy {
        PhotoVideoSwitchWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * Photo mode icon
     */
    var photoModeIcon: Drawable? = getDrawable(R.drawable.uxsdk_selector_camera_mode_photo)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Video mode icon
     */
    var videoModeIcon: Drawable? = getDrawable(R.drawable.uxsdk_selector_camera_mode_video)
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Photo mode icon color
     */
    var iconTintColor: Int? = INVALID_COLOR
        set(value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    init {
        foregroundImageView.setImageDrawable(photoModeIcon)
        attrs?.let { initAttributes(context, it) }
    }

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
        addReaction(widgetModel.photoVideoSwitchState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        if (isEnabled) {
            addDisposable(widgetModel.toggleCameraMode()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(
                            Action {}, logErrorConsumer(TAG, "Switch camera Mode ")
                    ))
        }
    }

    override fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.photoVideoSwitchState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) }, {
                        DJILog.e(TAG, "Update PhotoVideoSwitch UI " + it.message)
                    }))
        }
    }
    //endregion

    //region private helpers
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.PhotoVideoSwitchWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.PhotoVideoSwitchWidget_uxsdk_cameraIndex) {
                cameraIndex = CameraIndex.find(0)
            }
            typedArray.getDrawableAndUse(R.styleable.PhotoVideoSwitchWidget_uxsdk_photoModeIcon) {
                photoModeIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.PhotoVideoSwitchWidget_uxsdk_videoModeIcon) {
                videoModeIcon = it
            }
            typedArray.getColorAndUse(R.styleable.PhotoVideoSwitchWidget_uxsdk_iconTint) {
                iconTintColor = it
            }
        }

    }

    private fun updateUI(photoVideoSwitchState: PhotoVideoSwitchState) {
        widgetStateDataProcessor.onNext(PhotoVideoSwitchUpdated(photoVideoSwitchState))
        when (photoVideoSwitchState) {
            PhotoVideoSwitchState.ProductDisconnected,
            PhotoVideoSwitchState.CameraDisconnected,
            PhotoVideoSwitchState.Disabled -> updateDisabledState()
            PhotoVideoSwitchState.PhotoMode -> updateModeState(photoModeIcon)
            PhotoVideoSwitchState.VideoMode -> updateModeState(videoModeIcon)
        }
    }

    private fun updateModeState(modeDrawable: Drawable?) {
        isEnabled = true
        foregroundImageView.setImageDrawable(modeDrawable)
        foregroundImageView.updateColorFilter(iconTintColor)
    }

    private fun updateDisabledState() {
        isEnabled = false
        foregroundImageView.setColorFilter(getDisconnectedStateIconColor())
    }

    //endregion
    //region customization

    /**
     * Set photo mode drawable resource
     *
     * @param resourceId resource id of  photo mode icon
     */
    fun setPhotoModeIcon(@DrawableRes resourceId: Int) {
        photoModeIcon = getDrawable(resourceId)
    }

    /**
     * Set video mode drawable resource
     *
     * @param resourceId resource id of  video mode icon
     */
    fun setVideoModeIcon(@DrawableRes resourceId: Int) {
        videoModeIcon = getDrawable(resourceId)
    }

    //endregion


    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Photo/Video switch updated
         */
        data class PhotoVideoSwitchUpdated(val photoVideoSwitchState: PhotoVideoSwitchState) : ModelState()
    }
}