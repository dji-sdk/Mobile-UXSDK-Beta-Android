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
package dji.ux.beta.core.widget.fpv

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.use
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.keysdk.KeyManager
import dji.log.DJILog
import dji.sdk.camera.VideoFeeder.VideoDataListener
import dji.sdk.codec.DJICodecManager
import dji.sdk.util.VideoSizeCalculatorUtil
import dji.thirdparty.io.reactivex.Flowable
import dji.thirdparty.io.reactivex.functions.Action
import dji.thirdparty.io.reactivex.functions.Consumer
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.extension.*
import dji.ux.beta.core.module.FlatCameraModule
import dji.ux.beta.core.ui.CenterPointView
import dji.ux.beta.core.ui.GridLineView
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraSide
import dji.ux.beta.core.widget.fpv.FPVWidget.ModelState
import dji.ux.beta.core.widget.fpv.FPVWidget.ModelState.*
import java.util.*

private const val TAG = "FPVWidget"
private const val ADJUST_ASPECT_RATIO_DELAY = 300
private const val ORIGINAL_SCALE = 1f
private const val PORTRAIT_ROTATION_ANGLE = 270
private const val LANDSCAPE_ROTATION_ANGLE = 0

/**
 * This widget shows the video feed from the camera.
 */
open class FPVWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {
    //region Fields
    private var codecManager: DJICodecManager? = null
    private val videoSizeCalculator: VideoSizeCalculatorUtil = VideoSizeCalculatorUtil()
    private var videoSurface: SurfaceTexture? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var viewWidth = 0
    private var viewHeight = 0
    private var rotationAngle = 0
    private var videoFeed = DJICodecManager.VideoSource.UNKNOWN
    private val fpvTextureView: TextureView = findViewById(R.id.textureview_fpv)
    private val cameraNameTextView: TextView = findViewById(R.id.textview_camera_name)
    private val cameraSideTextView: TextView = findViewById(R.id.textview_camera_side)
    private val verticalOffset: Guideline = findViewById(R.id.vertical_offset)
    private val horizontalOffset: Guideline = findViewById(R.id.horizontal_offset)
    private var codecManagerCallback: CodecManagerCallback? = null
    private var fpvStateChangeResourceId: Int = INVALID_RESOURCE

    private val widgetModel by lazy {
        val videoDataListener = VideoDataListener { videoBuffer: ByteArray?, size: Int ->
            widgetStateDataProcessor.onNext(VideoFeedUpdated(videoBuffer, size))
            codecManager?.sendDataToDecoder(videoBuffer, size, videoFeed)
        }

        FPVWidgetModel(DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                videoDataListener,
                FlatCameraModule())
    }

    /**
     * Whether the video feed source's camera name is visible on the video feed.
     */
    var isCameraSourceNameVisible = true
        set(value) {
            field = value
            checkAndUpdateCameraName()
        }

    /**
     * Whether the video feed source's camera side is visible on the video feed.
     * Only shown on aircraft that support multiple gimbals.
     */
    var isCameraSourceSideVisible = true
        set(value) {
            field = value
            checkAndUpdateCameraSide()
        }

    /**
     * Whether the grid lines are enabled.
     */
    var isGridLinesEnabled = true
        set(isGridLinesEnabled) {
            field = isGridLinesEnabled
            updateGridLineVisibility()
        }

    /**
     * Whether the center point is enabled.
     */
    var isCenterPointEnabled = true
        set(isCenterPointEnabled) {
            field = isCenterPointEnabled
            centerPointView.visibility = if (isCenterPointEnabled) View.VISIBLE else View.GONE
        }

    /**
     * The video source can be one of these three options: AUTO, PRIMARY, SECONDARY. By
     * default, the video source is set to "AUTO" if user does not specify it.
     */
    var videoSource: SettingDefinitions.VideoSource?
        get() = widgetModel.videoSource
        set(value) {
            widgetModel.videoSource = value
        }

    /**
     * The name of the current camera
     */
    val cameraName: Flowable<String>
        get() = widgetModel.cameraName

    /**
     * The text color state list of the camera name text view
     */
    var cameraNameTextColors: ColorStateList?
        get() = cameraNameTextView.textColors
        set(colorStateList) {
            cameraNameTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the camera name text view
     */
    @get:ColorInt
    @setparam:ColorInt
    var cameraNameTextColor: Int
        get() = cameraNameTextView.currentTextColor
        set(color) {
            cameraNameTextView.setTextColor(color)
        }

    /**
     * The text size of the camera name text view
     */
    @get:Dimension
    @setparam:Dimension
    var cameraNameTextSize: Float
        get() = cameraNameTextView.textSize
        set(textSize) {
            cameraNameTextView.textSize = textSize
        }

    /**
     * The background for the camera name text view
     */
    var cameraNameTextBackground: Drawable?
        get() = cameraNameTextView.background
        set(drawable) {
            cameraNameTextView.background = drawable
        }

    /**
     * The text color state list of the camera name text view
     */
    var cameraSideTextColors: ColorStateList?
        get() = cameraSideTextView.textColors
        set(colorStateList) {
            cameraSideTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the camera side text view
     */
    @get:ColorInt
    @setparam:ColorInt
    var cameraSideTextColor: Int
        get() = cameraSideTextView.currentTextColor
        set(color) {
            cameraSideTextView.setTextColor(color)
        }

    /**
     * The text size of the camera side text view
     */
    @get:Dimension
    @setparam:Dimension
    var cameraSideTextSize: Float
        get() = cameraSideTextView.textSize
        set(textSize) {
            cameraSideTextView.textSize = textSize
        }

    /**
     * The background for the camera side text view
     */
    var cameraSideTextBackground: Drawable?
        get() = cameraSideTextView.background
        set(drawable) {
            cameraSideTextView.background = drawable
        }

    /**
     * The vertical alignment of the camera name and side text views
     */
    var cameraDetailsVerticalAlignment: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() {
            val layoutParams: LayoutParams = verticalOffset.layoutParams as LayoutParams
            return layoutParams.guidePercent
        }
        set(@FloatRange(from = 0.0, to = 1.0) percent) {
            val layoutParams: LayoutParams = verticalOffset.layoutParams as LayoutParams
            layoutParams.guidePercent = percent
            verticalOffset.layoutParams = layoutParams
        }

    /**
     * The horizontal alignment of the camera name and side text views
     */
    var cameraDetailsHorizontalAlignment: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() {
            val layoutParams: LayoutParams = horizontalOffset.layoutParams as LayoutParams
            return layoutParams.guidePercent
        }
        set(@FloatRange(from = 0.0, to = 1.0) percent) {
            val layoutParams: LayoutParams = horizontalOffset.layoutParams as LayoutParams
            layoutParams.guidePercent = percent
            horizontalOffset.layoutParams = layoutParams
        }

    /**
     * The [GridLineView] shown in this widget
     */
    val gridLineView: GridLineView = findViewById(R.id.view_grid_line)

    /**
     * The [CenterPointView] shown in this widget
     */
    val centerPointView: CenterPointView = findViewById(R.id.view_center_point)

    /**
     * Call back for when the camera state is updated.
     * This can be used to link the widget to FPVInteractionWidget
     */
    var stateChangeCallback: FPVStateChangeCallback? = null

    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_fpv, this)
    }

    init {
        if (!isInEditMode) {
            fpvTextureView.surfaceTextureListener = this
            rotationAngle = LANDSCAPE_ROTATION_ANGLE

            videoSizeCalculator.setListener { width: Int, height: Int, relativeWidth: Int, relativeHeight: Int -> changeView(width, height, relativeWidth, relativeHeight) }
        }
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region LifeCycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListeners()
    }

    override fun onDetachedFromWindow() {
        destroyListeners()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.model
                .observeOn(SchedulerProvider.ui())
                .subscribe {
                    if (codecManager == null) {
                        videoSurface?.let {
                            //registration was incomplete before, so codecManager needs to be initialized now
                            onSurfaceTextureAvailable(it, videoWidth, videoHeight)
                        }
                    }
                })
        addReaction(widgetModel.videoFeedSource
                .observeOn(SchedulerProvider.ui())
                .subscribe { videoFeed: DJICodecManager.VideoSource ->
                    widgetStateDataProcessor.onNext(VideoFeedSourceUpdated(videoFeed))
                    this.videoFeed = videoFeed
                    updateGridLineVisibility()
                    codecManager?.switchSource(videoFeed)
                })
        addReaction(widgetModel.orientation
                .observeOn(SchedulerProvider.ui())
                .subscribe { orientation: SettingsDefinitions.Orientation -> updateOrientation(orientation) })
        addReaction(widgetModel.cameraName
                .observeOn(SchedulerProvider.ui())
                .subscribe { cameraName: String -> updateCameraName(cameraName) })
        addReaction(widgetModel.cameraSide
                .observeOn(SchedulerProvider.ui())
                .subscribe { cameraSide: CameraSide -> updateCameraSide(cameraSide) })
        addReaction(widgetModel.hasVideoViewChanged
                .observeOn(SchedulerProvider.ui())
                .subscribe { delayCalculator() })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateConnectionState(it) })
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (codecManager == null) {
            if (KeyManager.getInstance() == null) {
                //save parameters so codecManager can be initialized once the KeyManager is available
                videoSurface = surface
                videoWidth = width
                videoHeight = height
            } else {
                codecManager = DJICodecManager(this.context,
                        surface,
                        width,
                        height,
                        videoFeed)
                codecManagerCallback?.onCodecManagerChanged(codecManager)
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        codecManager?.onSurfaceSizeChanged(width, height, rotationAngle)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if (codecManager != null) {
            codecManager?.cleanSurface()
            codecManager?.destroyCodec()
            codecManager = null
            codecManagerCallback?.onCodecManagerChanged(null)
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        codecManager?.let {
            if (videoHeight != it.videoHeight || videoWidth != it.videoWidth) {
                videoWidth = it.videoWidth
                videoHeight = it.videoHeight
                delayCalculator()
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!isInEditMode) {
            setViewDimensions()
            delayCalculator()
        }
    }

    private fun initializeListeners() {
        if (fpvStateChangeResourceId != INVALID_RESOURCE && this.rootView != null) {
            val widgetView = this.rootView.findViewById<View>(fpvStateChangeResourceId)
            if (widgetView is FPVStateChangeCallback?) {
                stateChangeCallback = widgetView
            }
        }
    }

    private fun destroyListeners() {
        stateChangeCallback = null
    }

    //endregion
    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_fpv_ratio)
    }

    /**
     * Set the [cameraVideoStreamSource] for multi-lens cameras.
     *
     * @return Disposable
     */
    fun setCameraVideoStreamSource(cameraVideoStreamSource: CameraVideoStreamSource) {
        addDisposable(widgetModel.setCameraVideoStreamSource(cameraVideoStreamSource)
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action {
                    // do nothing
                }, logErrorConsumer(TAG, "set camera video stream source ")))
        stateChangeCallback?.onStreamSourceChange(cameraVideoStreamSource)
    }

    /**
     * Sets a callback to retrieve the [DJICodecManager] object.
     *
     * @param callback A callback that is invoked when the [DJICodecManager] changes.
     */
    fun setCodecManagerCallback(callback: CodecManagerCallback?) {
        codecManagerCallback = callback
        codecManagerCallback?.onCodecManagerChanged(codecManager)
    }

    //endregion
    //region Helpers
    private fun setViewDimensions() {
        viewWidth = measuredWidth
        viewHeight = measuredHeight
    }

    /**
     * This method should not to be called until the size of `TextureView` is fixed.
     */
    private fun changeView(width: Int, height: Int, relativeWidth: Int, relativeHeight: Int) {
        val lp = fpvTextureView.layoutParams
        lp.width = width
        lp.height = height
        fpvTextureView.layoutParams = lp
        if (width > viewWidth) {
            fpvTextureView.scaleX = width.toFloat() / viewWidth
        } else {
            fpvTextureView.scaleX = ORIGINAL_SCALE
        }
        if (height > viewHeight) {
            fpvTextureView.scaleY = height.toFloat() / viewHeight
        } else {
            fpvTextureView.scaleY = ORIGINAL_SCALE
        }
        gridLineView.adjustDimensions(relativeWidth, relativeHeight)
        stateChangeCallback?.onFPVSizeChange(FPVSize(relativeWidth, relativeHeight))
        widgetStateDataProcessor.onNext(FPVSizeUpdated(relativeWidth, relativeHeight))
    }

    private fun delayCalculator() {
        if (handler != null) {
            handler.postDelayed({ notifyCalculator() }, ADJUST_ASPECT_RATIO_DELAY.toLong())
        }
    }

    private fun notifyCalculator() {
        try {
            if (videoWidth != 0 && videoHeight != 0) {
                videoSizeCalculator.setVideoTypeBySize(videoWidth,
                        videoHeight,
                        widgetModel.currentCameraIndex.index)
            }
            videoSizeCalculator.setScreenTypeBySize(viewWidth, viewHeight)
            videoSizeCalculator.calculateVideoSize()
        } catch (exception: Exception) {
            DJILog.e(TAG, "FPVNotifyCalculator: " + exception.localizedMessage)
        }
    }

    private fun updateOrientation(orientation: SettingsDefinitions.Orientation) {
        widgetStateDataProcessor.onNext(OrientationUpdated(orientation))
        videoSizeCalculator.setVideoIsRotated(orientation == SettingsDefinitions.Orientation.PORTRAIT)
        rotationAngle = if (orientation == SettingsDefinitions.Orientation.PORTRAIT) {
            PORTRAIT_ROTATION_ANGLE
        } else {
            LANDSCAPE_ROTATION_ANGLE
        }
        delayCalculator()
    }

    private fun updateCameraName(cameraName: String) {
        widgetStateDataProcessor.onNext(CameraNameUpdated(cameraName))
        cameraNameTextView.text = cameraName
        if (cameraName.isNotEmpty() && isCameraSourceNameVisible) {
            cameraNameTextView.visibility = View.VISIBLE
        } else {
            cameraNameTextView.visibility = View.GONE
        }
        stateChangeCallback?.onCameraNameChange(cameraName)
    }

    private fun updateCameraSide(cameraSide: CameraSide) {
        widgetStateDataProcessor.onNext(CameraSideUpdated(cameraSide))
        if (cameraSide == CameraSide.UNKNOWN) {
            cameraSideTextView.text = ""
            cameraSideTextView.visibility = View.GONE
        } else {
            cameraSideTextView.text = cameraSide.toString()
            if (isCameraSourceSideVisible) {
                cameraSideTextView.visibility = View.VISIBLE
            } else {
                cameraSideTextView.visibility = View.GONE
            }
            stateChangeCallback?.onCameraSideChange(cameraSide)
        }
    }

    private fun updateConnectionState(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        if (!isConnected) {
            codecManager?.setSurfaceToGray()
        }
    }

    private fun checkAndUpdateCameraName() {
        if (!isInEditMode) {
            addDisposable(widgetModel.cameraName
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { cameraName: String -> updateCameraName(cameraName) },
                            logErrorConsumer(TAG, "updateCameraName")))
        }
    }

    private fun checkAndUpdateCameraSide() {
        if (!isInEditMode) {
            addDisposable(widgetModel.cameraSide
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { cameraSide: CameraSide -> updateCameraSide(cameraSide) },
                            logErrorConsumer(TAG, "updateCameraSide")))
        }
    }

    private fun updateGridLineVisibility() {
        gridLineView.visibility = if (isGridLinesEnabled
                && videoFeed != DJICodecManager.VideoSource.FPV) View.VISIBLE else View.GONE
    }
    //endregion

    //region Customization helpers
    /**
     * Set text appearance of the camera name text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setCameraNameTextAppearance(@StyleRes textAppearance: Int) {
        cameraNameTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set text appearance of the camera side text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setCameraSideTextAppearance(@StyleRes textAppearance: Int) {
        cameraSideTextView.setTextAppearance(context, textAppearance)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.FPVWidget).use { typedArray ->
            if (!isInEditMode) {
                typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_videoSource) {
                    videoSource = (SettingDefinitions.VideoSource.find(it))
                }
                typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_gridLinesEnabled, true) {
                    isGridLinesEnabled = it
                }
                typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_centerPointEnabled, true) {
                    isCenterPointEnabled = it
                }
            }
            typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_sourceCameraNameVisibility, true) {
                isCameraSourceNameVisible = it
            }
            typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_sourceCameraSideVisibility, true) {
                isCameraSourceSideVisible = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextAppearance) {
                setCameraNameTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextSize) {
                cameraNameTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextColor) {
                cameraNameTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.FPVWidget_uxsdk_cameraNameBackgroundDrawable) {
                cameraNameTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextAppearance) {
                setCameraSideTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextSize) {
                cameraSideTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextColor) {
                cameraSideTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.FPVWidget_uxsdk_cameraSideBackgroundDrawable) {
                cameraSideTextBackground = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_cameraDetailsVerticalAlignment) {
                cameraDetailsVerticalAlignment = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_cameraDetailsHorizontalAlignment) {
                cameraDetailsHorizontalAlignment = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_gridLineType) {
                gridLineView.type = GridLineView.GridLineType.find(it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_gridLineColor) {
                gridLineView.lineColor = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_gridLineWidth) {
                gridLineView.lineWidth = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_gridLineNumber) {
                gridLineView.numberOfLines = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_centerPointType) {
                centerPointView.type = CenterPointView.CenterPointType.find(it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_centerPointColor) {
                centerPointView.color = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_onStateChange) {
                fpvStateChangeResourceId = it
            }
        }
    }
    //endregion

    //region Classes
    /**
     * A callback to get the [DJICodecManager] object.
     */
    interface CodecManagerCallback {
        /**
         * A callback method that is invoked when the [DJICodecManager] is initialized or
         * destroyed.
         *
         * @param codecManager An instance of [DJICodecManager], or null if it's been
         * destroyed.
         */
        fun onCodecManagerChanged(codecManager: DJICodecManager?)
    }

    /**
     * The size of the video feed within this widget
     *
     * @property width The width of the video feed within this widget
     * @property height The height of the video feed within this widget
     */
    data class FPVSize(val width: Int, val height: Int)

    /**
     * Interface to be implemented by widgets for coupled 1:1 communication
     */
    interface FPVStateChangeCallback {

        /**
         * Called when the camera name has changed
         */
        fun onCameraNameChange(cameraName: String?)

        /**
         * Called when the camera side has changed
         */
        fun onCameraSideChange(cameraSide: CameraSide?)

        /**
         * Called when the camera stream source has changed
         */
        fun onStreamSourceChange(streamSource: CameraVideoStreamSource?)

        /**
         * Called when the size of the video feed has changed
         */
        fun onFPVSizeChange(size: FPVSize?)
    }
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
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Orientation update
         */
        data class OrientationUpdated(val orientation: SettingsDefinitions.Orientation) : ModelState()

        /**
         * Video feed update
         */
        data class VideoFeedSourceUpdated(val videoFeed: DJICodecManager.VideoSource) : ModelState()

        /**
         * Video feed size update
         */
        data class FPVSizeUpdated(val width: Int, val height: Int) : ModelState()

        /**
         * Camera name update
         */
        data class CameraNameUpdated(val cameraName: String) : ModelState()

        /**
         * Camera side update
         */
        data class CameraSideUpdated(val cameraSide: CameraSide) : ModelState()

        /**
         * Video feed update
         *
         * @property videoBuffer H.264 or H.265 raw video data. See [SettingsDefinitions.VideoFileCompressionStandard]
         * @property size The data size
         */
        data class VideoFeedUpdated(val videoBuffer: ByteArray?, val size: Int) : ModelState() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false
                other as VideoFeedUpdated
                return Arrays.equals(videoBuffer, other.videoBuffer) && size == other.size
            }

            override fun hashCode(): Int {
                var result = if (videoBuffer != null) Arrays.hashCode(videoBuffer) else 0
                result = 31 * result + size
                return result
            }
        }
    }
    //endregion
}