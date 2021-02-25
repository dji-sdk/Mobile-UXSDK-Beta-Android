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

package dji.ux.beta.visualcamera.widget.cameraconfig.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.SDCardOperationState
import dji.common.camera.SettingsDefinitions.StorageLocation
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.WidgetSizeDescription
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.visualcamera.R
import dji.ux.beta.visualcamera.widget.cameraconfig.storage.CameraConfigStorageWidgetModel.CameraConfigStorageState

private const val TAG = "CamConfStoraWid"

/**
 * Shows the camera's current capacity and other information for internal and SD card storage
 * locations.
 */
open class CameraConfigStorageWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : ConstraintLayoutWidget<CameraConfigStorageWidget.ModelState>(
        context,
        attrs,
        defStyleAttr
) {

    //region Fields
    private val storageIconImageView: ImageView = findViewById(R.id.imageview_storage_icon)
    private val imageFormatTextView: TextView = findViewById(R.id.textview_image_format)
    private val statusCapacityTitleTextView: TextView = findViewById(R.id.textview_status_capacity_title)
    private val statusCapacityValueTextView: TextView = findViewById(R.id.textview_status_capacity_value)

    private val internalStorageIconMap: MutableMap<StorageOperationIconState, Drawable?> =
            mutableMapOf(
                    StorageOperationIconState.Normal to getDrawable(R.drawable.uxsdk_ic_config_internal_normal),
                    StorageOperationIconState.Warning to getDrawable(R.drawable.uxsdk_ic_config_internal_warning),
                    StorageOperationIconState.NotInserted to getDrawable(R.drawable.uxsdk_ic_config_internal_none)
            )

    private val sdCardStorageIconMap: MutableMap<StorageOperationIconState, Drawable?> =
            mutableMapOf(
                    StorageOperationIconState.Normal to getDrawable(R.drawable.uxsdk_ic_config_sd_normal),
                    StorageOperationIconState.Warning to getDrawable(R.drawable.uxsdk_ic_config_sd_warning),
                    StorageOperationIconState.NotInserted to getDrawable(R.drawable.uxsdk_ic_config_sd_none)
            )

    private val internalStorageColorMap: MutableMap<StorageOperationIconState, Int> =
            mutableMapOf(
                    StorageOperationIconState.Normal to INVALID_COLOR,
                    StorageOperationIconState.Warning to INVALID_COLOR,
                    StorageOperationIconState.NotInserted to INVALID_COLOR
            )

    private val sdCardStorageColorMap: MutableMap<StorageOperationIconState, Int> =
            mutableMapOf(
                    StorageOperationIconState.Normal to INVALID_COLOR,
                    StorageOperationIconState.Warning to INVALID_COLOR,
                    StorageOperationIconState.NotInserted to INVALID_COLOR
            )

    private val widgetModel: CameraConfigStorageWidgetModel by lazy {
        CameraConfigStorageWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * Color of label text when disconnected
     */
    @ColorInt
    var disconnectedValueColor: Int = getColor(R.color.uxsdk_white_60_percent)

    /**
     * Color of icon text when disconnected
     */
    @ColorInt
    var disconnectedIconColor: Int = getColor(R.color.uxsdk_white_60_percent)

    /**
     * Index of the camera to which the widget is reacting
     */
    var cameraIndex: SettingDefinitions.CameraIndex
        get() = widgetModel.cameraIndex
        set(cameraIndex) {
            if (!isInEditMode) {
                widgetModel.cameraIndex = cameraIndex
            }
        }

    /**
     * The type of the lens for which the widget should react
     */
    var lensType: SettingsDefinitions.LensType
        get() = widgetModel.lensType
        set(lensType) {
            if (!isInEditMode) {
                widgetModel.lensType = lensType
            }
        }

    /**
     * Background of the icon
     */
    var iconBackground: Drawable?
        get() = storageIconImageView.background
        set(value) {
            storageIconImageView.background = value
        }

    /**
     * Color of the image format text when product is connected
     */
    var imageFormatTextColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Text size of the image format text view
     */
    var imageFormatTextSize: Float
        @Dimension get() = imageFormatTextView.textSize
        set(@Dimension value) {
            imageFormatTextView.textSize = value
        }

    /**
     * Background of the image format text view
     */
    var imageFormatTextBackground: Drawable?
        get() = imageFormatTextView.background
        set(value) {
            imageFormatTextView.background = value
        }

    /**
     * Visibility of the image format
     */
    var imageFormatVisibility: Boolean
        get() = imageFormatTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                imageFormatTextView.show()
            } else {
                imageFormatTextView.hide()
            }
        }

    /**
     * Color of the status capacity title text when product is connected
     */
    var statusCapacityTitleTextColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Text size of the status capacity title text view
     */
    var statusCapacityTitleTextSize: Float
        @Dimension get() = statusCapacityTitleTextView.textSize
        set(@Dimension value) {
            statusCapacityTitleTextView.textSize = value
        }

    /**
     * Background of the status capacity title text view
     */
    var statusCapacityTitleTextBackground: Drawable?
        get() = statusCapacityTitleTextView.background
        set(value) {
            statusCapacityTitleTextView.background = value
        }

    /**
     * Visibility of the status capacity title text view
     */
    var statusCapacityTitleVisibility: Boolean
        get() = statusCapacityTitleTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                statusCapacityTitleTextView.show()
            } else {
                statusCapacityTitleTextView.hide()
            }
        }

    /**
     * Color of the status capacity value text when product is connected
     */
    var statusCapacityValueTextColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Text size of the status capacity value text view
     */
    var statusCapacityValueTextSize: Float
        @Dimension get() = statusCapacityValueTextView.textSize
        set(@Dimension value) {
            statusCapacityValueTextView.textSize = value
        }

    /**
     * Background of the status capacity value text view
     */
    var statusCapacityValueTextBackground: Drawable?
        get() = statusCapacityValueTextView.background
        set(value) {
            statusCapacityValueTextView.background = value
        }

    /**
     * Visibility of the status capacity value text view
     */
    var statusCapacityValueVisibility: Boolean
        get() = statusCapacityValueTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                statusCapacityValueTextView.show()
            } else {
                statusCapacityValueTextView.hide()
            }
        }

    //endregion

    init {
        initThemeAttributes(widgetTheme)
        attrs?.let { initAttributesByTypedArray(context.obtainStyledAttributes(attrs, R.styleable.CameraConfigStorageWidget)) }
    }

    /**
     * Invoked during the initialization of the class.
     * Inflate should be done here. For Kotlin, load attributes, findViewById should be done in
     * the init block.
     *
     * @param context      Context
     * @param attrs        Attribute set
     * @param defStyleAttr Style attribute
     */
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_camera_config_storage, this)
    }

    /**
     * Call addReaction here to bind to the model.
     */
    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.cameraConfigStorageState.observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
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

    //region Helpers
    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.cameraConfigStorageState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({ updateUI(it) },
                            { logErrorConsumer(TAG, "Update UI ") }))
        }
    }

    private fun updateUI(cameraConfigStorageState: CameraConfigStorageState) {
        widgetStateDataProcessor.onNext(ModelState.StorageStateUpdated(cameraConfigStorageState))
        when (cameraConfigStorageState) {
            CameraConfigStorageState.ProductDisconnected,
            CameraConfigStorageState.CameraDisconnected,
            CameraConfigStorageState.NotSupported -> updateDisconnectedState()
            is CameraConfigStorageState.PhotoMode -> updatePhotoMode(cameraConfigStorageState)
            is CameraConfigStorageState.VideoMode -> updateVideoMode(cameraConfigStorageState)
        }
    }

    private fun updateVideoMode(cameraConfigStorageState: CameraConfigStorageState.VideoMode) {
        storageIconImageView.imageDrawable = getStorageIcon(cameraConfigStorageState.storageLocation,
                getStorageStateIcon(cameraConfigStorageState.storageOperationState))
        val color: Int? = getStorageIconColor(cameraConfigStorageState.storageLocation,
                getStorageStateIcon(cameraConfigStorageState.storageOperationState))
        if (color == INVALID_COLOR || color == null) {
            storageIconImageView.clearColorFilter()
        } else {
            storageIconImageView.setColorFilter(color)
        }
        val status = if (cameraConfigStorageState.storageLocation == StorageLocation.INTERNAL_STORAGE) {
            getInternalStorageStatus(cameraConfigStorageState.storageOperationState)
        } else {
            getSDCardStatus(cameraConfigStorageState.storageOperationState)
        }
        if (status.isEmpty()) {
            statusCapacityTitleTextView.text = resources.getText(R.string.uxsdk_storage_title_capacity)
            statusCapacityValueTextView.text = CameraUtil.formatVideoTime(resources,
                    cameraConfigStorageState.availableRecordTime)
        } else {
            statusCapacityTitleTextView.text = resources.getText(R.string.uxsdk_storage_title_status)
            statusCapacityValueTextView.text = status
        }
        imageFormatTextView.text = resources.getString(R.string.uxsdk_video_frame_resolution,
                CameraUtil.resolutionShortDisplayName(cameraConfigStorageState.resolutionAndFrameRate.resolution),
                CameraUtil.frameRateDisplayName(cameraConfigStorageState.resolutionAndFrameRate.frameRate))
        setNormalColorToText()
    }


    private fun updatePhotoMode(cameraConfigStorageState: CameraConfigStorageState.PhotoMode) {
        storageIconImageView.imageDrawable = getStorageIcon(cameraConfigStorageState.storageLocation,
                getStorageStateIcon(cameraConfigStorageState.storageOperationState))
        val color: Int? = getStorageIconColor(cameraConfigStorageState.storageLocation,
                getStorageStateIcon(cameraConfigStorageState.storageOperationState))
        if (color == INVALID_COLOR || color == null) {
            storageIconImageView.clearColorFilter()
        } else {
            storageIconImageView.setColorFilter(color)
        }
        val status = if (cameraConfigStorageState.storageLocation == StorageLocation.INTERNAL_STORAGE) {
            getInternalStorageStatus(cameraConfigStorageState.storageOperationState)
        } else {
            getSDCardStatus(cameraConfigStorageState.storageOperationState)
        }
        if (status.isEmpty()) {
            statusCapacityTitleTextView.text = resources.getText(R.string.uxsdk_storage_title_capacity)
            statusCapacityValueTextView.text = cameraConfigStorageState.availableCaptureCount.toString()
        } else {
            statusCapacityTitleTextView.text = resources.getText(R.string.uxsdk_storage_title_status)
            statusCapacityValueTextView.text = status
        }
        imageFormatTextView.text = CameraUtil.convertPhotoFileFormatToString(resources,
                cameraConfigStorageState.photoFileFormat)
        setNormalColorToText()
    }


    private fun setNormalColorToText() {
        imageFormatTextView.textColor = imageFormatTextColor
        statusCapacityTitleTextView.textColor = statusCapacityTitleTextColor
        statusCapacityValueTextView.textColor = statusCapacityValueTextColor
    }

    private fun updateDisconnectedState() {
        storageIconImageView.imageDrawable = sdCardStorageIconMap[StorageOperationIconState.Normal]
        storageIconImageView.setColorFilter(disconnectedIconColor)
        imageFormatTextView.textColor = disconnectedValueColor
        statusCapacityTitleTextView.textColor = disconnectedValueColor
        statusCapacityValueTextView.textColor = disconnectedValueColor
        imageFormatTextView.text = getString(R.string.uxsdk_string_default_value)
        statusCapacityTitleTextView.text = getString(R.string.uxsdk_string_default_value)
        statusCapacityValueTextView.text = getString(R.string.uxsdk_string_default_value)
    }

    private fun getStorageIcon(storageLocation: StorageLocation,
                               storageIconState: StorageOperationIconState): Drawable? {
        return if (storageLocation == StorageLocation.INTERNAL_STORAGE) {
            internalStorageIconMap[storageIconState]
        } else {
            sdCardStorageIconMap[storageIconState]
        }
    }

    private fun getStorageIconColor(storageLocation: StorageLocation,
                                    storageIconState: StorageOperationIconState): Int? {
        return if (storageLocation == StorageLocation.INTERNAL_STORAGE) {
            internalStorageColorMap[storageIconState]
        } else {
            sdCardStorageColorMap[storageIconState]
        }
    }

    private fun getSDCardStatus(sdCardOperationState: SDCardOperationState): String {
        return when (sdCardOperationState) {
            SDCardOperationState.USB_CONNECTED -> getString(R.string.uxsdk_sd_card_usb_connected)
            SDCardOperationState.NOT_INSERTED -> getString(R.string.uxsdk_sd_card_missing)
            SDCardOperationState.FULL -> getString(R.string.uxsdk_sd_card_full)
            SDCardOperationState.SLOW -> getString(R.string.uxsdk_sd_card_slow)
            SDCardOperationState.INVALID -> getString(R.string.uxsdk_sd_card_invalid)
            SDCardOperationState.READ_ONLY -> getString(R.string.uxsdk_sd_card_write_protect)
            SDCardOperationState.FORMAT_NEEDED -> getString(R.string.uxsdk_sd_card_not_formatted)
            SDCardOperationState.FORMATTING -> getString(R.string.uxsdk_sd_card_formatting)
            SDCardOperationState.BUSY -> getString(R.string.uxsdk_sd_card_busy)
            SDCardOperationState.UNKNOWN_ERROR -> getString(R.string.uxsdk_sd_card_unknown_error)
            SDCardOperationState.INITIALIZING -> getString(R.string.uxsdk_sd_card_initial)
            SDCardOperationState.RECOVERING_FILES -> getString(R.string.uxsdk_sd_card_recover_file)
            SDCardOperationState.FORMAT_RECOMMENDED -> getString(R.string.uxsdk_sd_card_needs_formatting)
            SDCardOperationState.WRITING_SLOWLY -> getString(R.string.uxsdk_sd_card_write_slow)
            else -> ""
        }
    }

    private fun getInternalStorageStatus(sdCardOperationState: SDCardOperationState): String {
        return when (sdCardOperationState) {
            SDCardOperationState.NOT_INSERTED -> getString(R.string.uxsdk_internal_storage_missing)
            SDCardOperationState.FULL -> getString(R.string.uxsdk_internal_storage_full)
            SDCardOperationState.SLOW -> getString(R.string.uxsdk_internal_storage_slow)
            SDCardOperationState.INVALID -> getString(R.string.uxsdk_internal_storage_invalid)
            SDCardOperationState.READ_ONLY -> getString(R.string.uxsdk_internal_storage_write_protect)
            SDCardOperationState.FORMAT_NEEDED -> getString(R.string.uxsdk_internal_storage_not_formatted)
            SDCardOperationState.FORMATTING -> getString(R.string.uxsdk_internal_storage_formatting)
            SDCardOperationState.BUSY -> getString(R.string.uxsdk_internal_storage_busy)
            SDCardOperationState.UNKNOWN_ERROR -> getString(R.string.uxsdk_internal_storage_unknown_error)
            SDCardOperationState.INITIALIZING -> getString(R.string.uxsdk_internal_storage_initial)
            else -> ""
        }
    }

    private fun getStorageStateIcon(storageState: SDCardOperationState): StorageOperationIconState {
        return when (storageState) {
            SDCardOperationState.NORMAL -> StorageOperationIconState.Normal
            SDCardOperationState.NOT_INSERTED -> StorageOperationIconState.NotInserted
            SDCardOperationState.FULL,
            SDCardOperationState.INVALID,
            SDCardOperationState.READ_ONLY,
            SDCardOperationState.FORMAT_NEEDED,
            SDCardOperationState.FORMATTING,
            SDCardOperationState.INVALID_FILE_SYSTEM,
            SDCardOperationState.BUSY,
            SDCardOperationState.SLOW,
            SDCardOperationState.UNKNOWN_ERROR,
            SDCardOperationState.NO_REMAIN_FILE_INDICES,
            SDCardOperationState.INITIALIZING,
            SDCardOperationState.FORMAT_RECOMMENDED,
            SDCardOperationState.RECOVERING_FILES,
            SDCardOperationState.WRITING_SLOWLY,
            SDCardOperationState.USB_CONNECTED,
            SDCardOperationState.UNKNOWN -> StorageOperationIconState.Warning
        }
    }

    @SuppressLint("Recycle")
    private fun initThemeAttributes(widgetTheme: Int) {
        val baseCameraConfigAttributeArray: IntArray = R.styleable.CameraConfigStorageWidget
        context.obtainStyledAttributes(widgetTheme, baseCameraConfigAttributeArray).use {
            initAttributesByTypedArray(it)
        }
    }


    @SuppressLint("Recycle")
    private fun initAttributesByTypedArray(typedArray: TypedArray) {
        typedArray.getIntegerAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_cameraIndex) {
            cameraIndex = SettingDefinitions.CameraIndex.find(it)
        }
        typedArray.getIntegerAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_lensType) {
            lensType = SettingsDefinitions.LensType.find(it)
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_disconnectedStateTextColor) {
            disconnectedValueColor = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_disconnectedStateIconColor) {
            disconnectedIconColor = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNormalIcon) {
            sdCardStorageIconMap[StorageOperationIconState.Normal] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardWarningIcon) {
            sdCardStorageIconMap[StorageOperationIconState.Warning] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNotInsertedIcon) {
            sdCardStorageIconMap[StorageOperationIconState.NotInserted] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNormalIconColor) {
            sdCardStorageColorMap[StorageOperationIconState.Normal] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardWarningIconColor) {
            sdCardStorageColorMap[StorageOperationIconState.Warning] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNotInsertedIconColor) {
            sdCardStorageColorMap[StorageOperationIconState.NotInserted] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNormalIcon) {
            internalStorageIconMap[StorageOperationIconState.Normal] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageWarningIcon) {
            internalStorageIconMap[StorageOperationIconState.Warning] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNotInsertedIcon) {
            internalStorageIconMap[StorageOperationIconState.NotInserted] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNormalIconColor) {
            internalStorageColorMap[StorageOperationIconState.Normal] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageWarningIconColor) {
            internalStorageColorMap[StorageOperationIconState.Warning] = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNotInsertedIconColor) {
            internalStorageColorMap[StorageOperationIconState.NotInserted] = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_storageIconBackground) {
            iconBackground = it
        }
        typedArray.getResourceIdAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextAppearance) {
            setImageFormatAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextSize) {
            imageFormatTextSize = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextBackground) {
            imageFormatTextBackground = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextColor) {
            imageFormatTextColor = it
        }
        imageFormatVisibility = typedArray.getBoolean(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatVisibility, imageFormatVisibility)
        typedArray.getResourceIdAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextAppearance) {
            setStatusCapacityTitleAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextSize) {
            statusCapacityTitleTextSize = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextBackground) {
            statusCapacityTitleTextBackground = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextColor) {
            statusCapacityTitleTextColor = it
        }
        statusCapacityTitleVisibility = typedArray.getBoolean(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTitleVisibility, statusCapacityTitleVisibility)
        typedArray.getResourceIdAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextAppearance) {
            setStatusCapacityValueAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextSize) {
            statusCapacityValueTextSize = it
        }
        typedArray.getDrawableAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextBackground) {
            statusCapacityValueTextBackground = it
        }
        typedArray.getColorAndUse(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextColor) {
            statusCapacityValueTextColor = it
        }
        statusCapacityValueVisibility = typedArray.getBoolean(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueVisibility, statusCapacityValueVisibility)

    }

    //endregion

    //region Customization
    /**
     * Ideal dimension ratio in the format width:height.
     *
     * @return dimension ratio string.
     */
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state for which the icon will change to the given image.
     * @param resourceId       The id of the image the icon will change to.
     */
    fun setInternalStorageIcon(storageIconState: StorageOperationIconState, @DrawableRes resourceId: Int) {
        setInternalStorageIcon(storageIconState, getDrawable(resourceId))
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state for which the icon will change to the given image.
     * @param drawable         The image the icon will change to.
     */
    fun setInternalStorageIcon(storageIconState: StorageOperationIconState, drawable: Drawable) {
        internalStorageIconMap[storageIconState] = drawable
        checkAndUpdateUI()
    }

    /**
     * Get the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    fun getInternalStorageIcon(storageIconState: StorageOperationIconState): Drawable? {
        return internalStorageIconMap[storageIconState]
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change to the given image.
     * @param resourceId       The id of the image the icon will change to.
     */
    fun setSDCardStorageIcon(storageIconState: StorageOperationIconState, @DrawableRes resourceId: Int) {
        setSDCardStorageIcon(storageIconState, getDrawable(resourceId))
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change to the given image.
     * @param drawable         The image the icon will change to.
     */
    fun setSDCardStorageIcon(storageIconState: StorageOperationIconState, drawable: Drawable?) {
        sdCardStorageIconMap[storageIconState] = drawable
        checkAndUpdateUI()
    }

    /**
     * Get the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    fun getSDCardStorageIcon(storageIconState: StorageOperationIconState): Drawable? {
        return sdCardStorageIconMap[storageIconState]
    }

    /**
     * Set the background image format text view
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setImageFormatTextBackground(@DrawableRes resourceId: Int) {
        imageFormatTextBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the image format
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setImageFormatAppearance(@StyleRes textAppearanceResId: Int) {
        imageFormatTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background status capacity title text view
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setStatusCapacityTitleTextBackground(@DrawableRes resourceId: Int) {
        statusCapacityTitleTextBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the status capacity title
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setStatusCapacityTitleAppearance(@StyleRes textAppearanceResId: Int) {
        statusCapacityTitleTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background status capacity value text view
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setStatusCapacityValueTextBackground(@DrawableRes resourceId: Int) {
        statusCapacityValueTextBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the status capacity value
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setStatusCapacityValueAppearance(@StyleRes textAppearanceResId: Int) {
        statusCapacityValueTextView.setTextAppearance(context, textAppearanceResId)
    }

    //endregion

    /**
     * State used for identifying storage status icons
     */
    sealed class StorageOperationIconState {
        /**
         * Storage not inserted
         */
        object NotInserted : StorageOperationIconState()

        /**
         * Storage is inserted and functional
         */
        object Normal : StorageOperationIconState()

        /**
         * Storage is inserted but has some warning
         */
        object Warning : StorageOperationIconState()
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
         * Storage state updated
         */
        data class StorageStateUpdated(val storageState: CameraConfigStorageState) : ModelState()
    }

}