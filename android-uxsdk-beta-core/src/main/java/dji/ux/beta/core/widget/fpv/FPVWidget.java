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
 */

package dji.ux.beta.core.widget.fpv;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.airlink.PhysicalSource;
import dji.common.camera.SettingsDefinitions;
import dji.keysdk.KeyManager;
import dji.log.DJILog;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.util.VideoSizeCalculatorUtil;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.core.base.ConstraintLayoutWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.ui.CenterPointView;
import dji.ux.beta.core.ui.GridLineView;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.interaction.FPVInteractionWidget;

/**
 * This widget shows the video feed from the camera.
 */
public class FPVWidget extends ConstraintLayoutWidget implements TextureView.SurfaceTextureListener {
    //region Constants
    private static final String TAG = "FPVWidget";
    private static final int ADJUST_ASPECT_RATIO_DELAY = 300;
    private static final float ORIGINAL_SCALE = 1;
    private static final int PORTRAIT_ROTATION_ANGLE = 270;
    private static final int LANDSCAPE_ROTATION_ANGLE = 0;
    //endregion

    //region Fields
    private FPVWidgetModel widgetModel;
    private FPVInteractionWidget fpvInteractionWidget;
    private DJICodecManager codecManager = null;
    private VideoSizeCalculatorUtil videoSizeCalculator;
    private SurfaceTexture videoSurface;
    private int videoWidth;
    private int videoHeight;
    private int viewWidth;
    private int viewHeight;
    private int rotationAngle;
    private DJICodecManager.VideoSource videoFeed = DJICodecManager.VideoSource.UNKNOWN;
    private TextureView fpvTextureView;
    private TextView cameraNameTextView;
    private TextView cameraSideTextView;
    private CodecManagerCallback codecManagerCallback;
    private GridLineView gridLineView;
    private CenterPointView centerPointView;
    private boolean isCameraNameTextVisible = true;
    private boolean isCameraSideTextVisible = true;
    private boolean isGridLinesEnabled = true;
    private boolean isCenterPointEnabled = true;
    private AtomicBoolean isInteractionEnabled;
    //endregion

    //region Constructors
    public FPVWidget(@NonNull Context context) {
        super(context);
    }

    public FPVWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FPVWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_fpv, this);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        fpvTextureView = findViewById(R.id.textureview_fpv);
        cameraNameTextView = findViewById(R.id.textview_camera_name);
        cameraSideTextView = findViewById(R.id.textview_camera_side);
        gridLineView = findViewById(R.id.view_grid_line);
        centerPointView = findViewById(R.id.view_center_point);
        if (!isInEditMode()) {
            fpvTextureView.setSurfaceTextureListener(this);
            rotationAngle = LANDSCAPE_ROTATION_ANGLE;
            VideoFeeder.VideoDataListener videoDataListener = (videoBuffer, size) -> {
                if (codecManager != null) {
                    codecManager.sendDataToDecoder(videoBuffer, size, videoFeed);
                }
            };
            videoSizeCalculator = new VideoSizeCalculatorUtil();
            videoSizeCalculator.setListener(this::changeView);
            widgetModel = new FPVWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    videoDataListener);
        }
        isInteractionEnabled = new AtomicBoolean(true);

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region LifeCycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getModel()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(model -> {
                    if (codecManager == null && videoSurface != null) {
                        //registration was incomplete before, so codecManager needs to be initialized now
                        onSurfaceTextureAvailable(videoSurface, videoWidth, videoHeight);
                    }
                }));

        addReaction(widgetModel.getVideoFeedSource()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(videoFeed -> {
                    this.videoFeed = videoFeed;
                    if (codecManager != null) {
                        codecManager.switchSource(videoFeed);
                    }
                }));

        addReaction(widgetModel.getOrientation()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateOrientation));

        addReaction(widgetModel.getCameraName()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCameraName));

        addReaction(widgetModel.getCameraSide()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCameraSide));

        addReaction(widgetModel.hasVideoViewChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasChanged -> delayCalculator()));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            if (KeyManager.getInstance() == null) {
                //save parameters so codecManager can be initialized once the KeyManager is available
                videoSurface = surface;
                videoWidth = width;
                videoHeight = height;
            } else {
                codecManager = new DJICodecManager(this.getContext(),
                        surface,
                        width,
                        height,
                        videoFeed);
                if (codecManagerCallback != null) {
                    codecManagerCallback.onCodecManagerChanged(codecManager);
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (codecManager != null) {
            codecManager.onSurfaceSizeChanged(width, height, rotationAngle);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager.destroyCodec();
            codecManager = null;
            if (codecManagerCallback != null) {
                codecManagerCallback.onCodecManagerChanged(null);
            }
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (codecManager == null) {
            return;
        }
        if (videoHeight != codecManager.getVideoHeight() || videoWidth != codecManager.getVideoWidth()) {
            videoWidth = codecManager.getVideoWidth();
            videoHeight = codecManager.getVideoHeight();
            delayCalculator();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!isInEditMode()) {
            setViewDimensions();
            delayCalculator();
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_fpv_ratio);
    }

    /**
     * Sets a callback to retrieve the {@link DJICodecManager} object.
     *
     * @param callback A callback that is invoked when the {@link DJICodecManager} changes.
     */
    public void setCodecManagerCallback(CodecManagerCallback callback) {
        codecManagerCallback = callback;
        if (codecManagerCallback != null) {
            codecManagerCallback.onCodecManagerChanged(codecManager);
        }
    }
    //endregion

    //region Helpers
    private void setViewDimensions() {
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
    }

    /**
     * This method should not to be called until the size of `TextureView` is fixed.
     */
    private void changeView(int width, int height, int relativeWidth, int relativeHeight) {
        ViewGroup.LayoutParams lp = fpvTextureView.getLayoutParams();
        lp.width = relativeWidth;
        lp.height = relativeHeight;
        fpvTextureView.setLayoutParams(lp);
        if (width > viewWidth) {
            fpvTextureView.setScaleX((float) width / viewWidth);
        } else {
            fpvTextureView.setScaleX(ORIGINAL_SCALE);
        }
        if (height > viewHeight) {
            fpvTextureView.setScaleY((float) height / viewHeight);
        } else {
            fpvTextureView.setScaleY(ORIGINAL_SCALE);
        }
        gridLineView.adjustDimensions(relativeWidth, relativeHeight);
        fpvInteractionWidget.adjustAspectRatio(relativeWidth, relativeHeight);
    }

    private void delayCalculator() {
        if (getHandler() != null) {
            getHandler().postDelayed(this::notifyCalculator, ADJUST_ASPECT_RATIO_DELAY);
        }
    }

    private void notifyCalculator() {
        try {
            if (videoWidth != 0 && videoHeight != 0) {
                videoSizeCalculator.setVideoTypeBySize(videoWidth,
                        videoHeight,
                        widgetModel.getCurrentCameraIndex().getIndex());
            }
            videoSizeCalculator.setScreenTypeBySize(viewWidth, viewHeight);
            videoSizeCalculator.calculateVideoSize();
        } catch (Exception exception) {
            DJILog.e(TAG, "FPVNotifyCalculator: " + exception.getLocalizedMessage());
        }
    }

    private void updateOrientation(SettingsDefinitions.Orientation orientation) {
        videoSizeCalculator.setVideoIsRotated(orientation == SettingsDefinitions.Orientation.PORTRAIT);
        if (orientation == SettingsDefinitions.Orientation.PORTRAIT) {
            rotationAngle = PORTRAIT_ROTATION_ANGLE;
        } else {
            rotationAngle = LANDSCAPE_ROTATION_ANGLE;
        }
        delayCalculator();
    }

    private void updateCameraName(@NonNull String cameraName) {
        cameraNameTextView.setText(cameraName);
        if (!cameraName.isEmpty() && isCameraNameTextVisible) {
            cameraNameTextView.setVisibility(View.VISIBLE);
        } else {
            cameraNameTextView.setVisibility(View.GONE);
        }

        if (cameraName.equals(PhysicalSource.FPV_CAM.toString()) || !isInteractionEnabled.get()) {
            fpvInteractionWidget.setVisibility(GONE);
        } else {
            fpvInteractionWidget.setVisibility(VISIBLE);
        }
    }

    private void updateCameraSide(@NonNull SettingDefinitions.CameraSide cameraSide) {
        if (cameraSide == SettingDefinitions.CameraSide.UNKNOWN) {
            cameraSideTextView.setText("");
            cameraSideTextView.setVisibility(View.GONE);
        } else {
            cameraSideTextView.setText(cameraSide.toString());
            if (isCameraSideTextVisible) {
                cameraSideTextView.setVisibility(View.VISIBLE);
            } else {
                cameraSideTextView.setVisibility(View.GONE);
            }

            // set both gimbal and camera index using the camera side
            // widgetModel.getCurrentCameraIndex should only be used for video size calculation.
            if (cameraSide == SettingDefinitions.CameraSide.PORT) {
                fpvInteractionWidget.setGimbalIndex(SettingDefinitions.GimbalIndex.PORT);
                fpvInteractionWidget.setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_0);
            } else {
                fpvInteractionWidget.setGimbalIndex(SettingDefinitions.GimbalIndex.STARBOARD);
                fpvInteractionWidget.setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_2);
            }
        }
    }

    private void checkAndUpdateCameraName() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getCameraName()
                    .firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateCameraName, logErrorConsumer(TAG, "updateCameraName")));
        }
    }

    private void checkAndUpdateCameraSide() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getCameraSide()
                    .firstOrError()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateCameraSide, logErrorConsumer(TAG, "updateCameraSide")));
        }
    }
    //endregion

    //region Customization helpers

    /**
     * User can set video source to these three options: AUTO, PRIMARY, SECONDARY. By
     * default, the video source is set to "AUTO" if user does not specify it.
     *
     * @param videoSource An enum value of `VideoSource`.
     */
    public void setVideoSource(@NonNull SettingDefinitions.VideoSource videoSource) {
        if (!isInEditMode()) {
            widgetModel.setVideoSource(videoSource);
        }
    }

    /**
     * Get the current video source.
     *
     * @return An enum value of `VideoSource`.
     */
    @Nullable
    public SettingDefinitions.VideoSource getVideoSource() {
        return widgetModel.getVideoSource();
    }

    /**
     * Get the name of the current camera
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    @NonNull
    public Flowable<String> getCameraName() {
        return widgetModel.getCameraName();
    }

    /**
     * Shows or hides the video feed source's camera name on the video feed.
     *
     * @param isVisible A boolean value that determines whether to show the source camera name.
     */
    public void setCameraSourceNameVisibility(boolean isVisible) {
        isCameraNameTextVisible = isVisible;
        checkAndUpdateCameraName();
    }

    /**
     * Get whether the video feed source's camera name is visible on the video feed.
     *
     * @return `true` if the source camera name is visible, `false otherwise.
     */
    public boolean isCameraSourceNameVisible() {
        return isCameraNameTextVisible;
    }

    /**
     * Shows or hides the side (starboard / port) of the camera on the video feed.
     * Only works on aircraft that support multiple gimbals.
     *
     * @param isVisible A boolean value that determines whether to show the source camera side.
     */
    public void setCameraSourceSideVisibility(boolean isVisible) {
        isCameraSideTextVisible = isVisible;
        checkAndUpdateCameraSide();
    }

    /**
     * Get whether the video feed source's camera side is visible on the video feed.
     *
     * @return `true` if the source camera side is visible, `false otherwise.
     */
    public boolean isCameraSourceSideVisible() {
        return isCameraSideTextVisible;
    }

    /**
     * Set text appearance of the camera name text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setCameraNameTextAppearance(@StyleRes int textAppearance) {
        cameraNameTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the camera name text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setCameraNameTextColor(@NonNull ColorStateList colorStateList) {
        cameraNameTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the camera name text view
     *
     * @param color color integer resource
     */
    public void setCameraNameTextColor(@ColorInt int color) {
        cameraNameTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the camera name text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getCameraNameTextColors() {
        return cameraNameTextView.getTextColors();
    }

    /**
     * Get current text color of the camera name text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getCameraNameTextColor() {
        return cameraNameTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the camera name text view
     *
     * @param textSize text size float value
     */
    public void setCameraNameTextSize(@Dimension float textSize) {
        cameraNameTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the camera name text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getCameraNameTextSize() {
        return cameraNameTextView.getTextSize();
    }

    /**
     * Set the background for the camera name text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setCameraNameTextBackground(@Nullable Drawable drawable) {
        cameraNameTextView.setBackground(drawable);
    }

    /**
     * Get current background of the camera name text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getCameraNameTextBackground() {
        return cameraNameTextView.getBackground();
    }

    /**
     * Set text appearance of the camera side text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setCameraSideTextAppearance(@StyleRes int textAppearance) {
        cameraSideTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the camera side text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setCameraSideTextColor(@NonNull ColorStateList colorStateList) {
        cameraSideTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the camera side text view
     *
     * @param color color integer resource
     */
    public void setCameraSideTextColor(@ColorInt int color) {
        cameraSideTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the camera side text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getCameraSideTextColors() {
        return cameraSideTextView.getTextColors();
    }

    /**
     * Get current text color of the camera side text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getCameraSideTextColor() {
        return cameraSideTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the camera side text view
     *
     * @param textSize text size float value
     */
    public void setCameraSideTextSize(@Dimension float textSize) {
        cameraSideTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the camera side text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getCameraSideTextSize() {
        return cameraSideTextView.getTextSize();
    }

    /**
     * Set the background for the camera side text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setCameraSideTextBackground(@Nullable Drawable drawable) {
        cameraSideTextView.setBackground(drawable);
    }

    /**
     * Get current background of the camera side text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getCameraSideTextBackground() {
        return cameraSideTextView.getBackground();
    }

    /**
     * Gets the GridLineView so it can be customized.
     *
     * @return A {@link GridLineView} object.
     */
    @NonNull
    public GridLineView getGridLineView() {
        return gridLineView;
    }

    /**
     * Gets the CenterPointView so it can be customized.
     *
     * @return A {@link CenterPointView} object.
     */
    @NonNull
    public CenterPointView getCenterPointView() {
        return centerPointView;
    }

    /**
     * Gets the FPVInteractionWidget so it can be customized.
     *
     * @return A FPVInteractionWidget object
     */
    @NonNull
    public FPVInteractionWidget getFPVInteractionWidget() {
        return fpvInteractionWidget;
    }

    /**
     * Set whether the grid lines are enabled.
     *
     * @param isGridLinesEnabled `true` if the grid lines are enabled, `false` otherwise.
     */
    public void setGridLinesEnabled(boolean isGridLinesEnabled) {
        this.isGridLinesEnabled = isGridLinesEnabled;
        gridLineView.setVisibility(isGridLinesEnabled ? VISIBLE : GONE);
    }

    /**
     * Get whether the grid lines are enabled.
     *
     * @return `true` if the grid lines are enabled, `false` otherwise.
     */
    public boolean isGridLinesEnabled() {
        return isGridLinesEnabled;
    }

    /**
     * Set whether the center point is enabled.
     *
     * @param isCenterPointEnabled `true` if the center point is enabled, `false` otherwise.
     */
    public void setCenterPointEnabled(boolean isCenterPointEnabled) {
        this.isCenterPointEnabled = isCenterPointEnabled;
        centerPointView.setVisibility(isCenterPointEnabled ? VISIBLE : GONE);
    }

    /**
     * Get whether the center point is enabled.
     *
     * @return `true` if the center point is enabled, `false` otherwise.
     */
    public boolean isCenterPointEnabled() {
        return isCenterPointEnabled;
    }

    /**
     * Set whether interaction is enabled. This shows or hides the FPVInteractionWidget.
     *
     * @param isInteractionEnabled `true` if interaction is enabled, `false` otherwise.
     */
    public void setInteractionEnabled(boolean isInteractionEnabled) {
        this.isInteractionEnabled.set(isInteractionEnabled);
        checkAndUpdateCameraName();
    }

    /**
     * Get whether interaction is enabled.
     *
     * @return `true` if interaction is enabled, `false` otherwise.
     */
    public boolean isInteractionEnabled() {
        return isInteractionEnabled.get();
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FPVWidget);

        int videoSourceValue =
                typedArray.getInteger(R.styleable.FPVWidget_uxsdk_videoSource, SettingDefinitions.VideoSource.AUTO.value());
        setVideoSource(SettingDefinitions.VideoSource.find(videoSourceValue));

        boolean isSourceNameVisible = typedArray.getBoolean(R.styleable.FPVWidget_uxsdk_sourceCameraNameVisibility, true);
        setCameraSourceNameVisibility(isSourceNameVisible);

        boolean isSourceSideVisible = typedArray.getBoolean(R.styleable.FPVWidget_uxsdk_sourceCameraSideVisibility, true);
        setCameraSourceSideVisibility(isSourceSideVisible);

        int cameraNameTextAppearanceId =
                typedArray.getResourceId(R.styleable.FPVWidget_uxsdk_cameraNameTextAppearance, INVALID_RESOURCE);
        if (cameraNameTextAppearanceId != INVALID_RESOURCE) {
            setCameraNameTextAppearance(cameraNameTextAppearanceId);
        }

        float cameraNameTextSize = typedArray.getDimension(R.styleable.FPVWidget_uxsdk_cameraNameTextSize, INVALID_RESOURCE);
        if (cameraNameTextSize != INVALID_RESOURCE) {
            setCameraNameTextSize(DisplayUtil.pxToSp(context, cameraNameTextSize));
        }

        int cameraNameTextColor = typedArray.getColor(R.styleable.FPVWidget_uxsdk_cameraNameTextColor, INVALID_COLOR);
        if (cameraNameTextColor != INVALID_COLOR) {
            setCameraNameTextColor(cameraNameTextColor);
        }

        Drawable cameraNameTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.FPVWidget_uxsdk_cameraNameBackgroundDrawable);
        if (cameraNameTextBackgroundDrawable != null) {
            setCameraNameTextBackground(cameraNameTextBackgroundDrawable);
        }

        int cameraSideTextAppearanceId =
                typedArray.getResourceId(R.styleable.FPVWidget_uxsdk_cameraSideTextAppearance, INVALID_RESOURCE);
        if (cameraSideTextAppearanceId != INVALID_RESOURCE) {
            setCameraSideTextAppearance(cameraSideTextAppearanceId);
        }

        float cameraSideTextSize = typedArray.getDimension(R.styleable.FPVWidget_uxsdk_cameraSideTextSize, INVALID_RESOURCE);
        if (cameraSideTextSize != INVALID_RESOURCE) {
            setCameraSideTextSize(DisplayUtil.pxToSp(context, cameraSideTextSize));
        }

        int cameraSideTextColor = typedArray.getColor(R.styleable.FPVWidget_uxsdk_cameraSideTextColor, INVALID_COLOR);
        if (cameraSideTextColor != INVALID_COLOR) {
            setCameraSideTextColor(cameraSideTextColor);
        }

        Drawable cameraSideTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.FPVWidget_uxsdk_cameraSideBackgroundDrawable);
        if (cameraSideTextBackgroundDrawable != null) {
            setCameraSideTextBackground(cameraSideTextBackgroundDrawable);
        }

        int gridLineType = typedArray.getInteger(R.styleable.FPVWidget_uxsdk_gridLineType, gridLineView.getType().value());
        gridLineView.setType(GridLineView.GridLineType.find(gridLineType));

        int gridLineColor = typedArray.getColor(R.styleable.FPVWidget_uxsdk_gridLineColor, gridLineView.getLineColor());
        gridLineView.setLineColor(gridLineColor);

        float gridLineWidth = typedArray.getFloat(R.styleable.FPVWidget_uxsdk_gridLineWidth, gridLineView.getLineWidth());
        gridLineView.setLineWidth(gridLineWidth);

        int numberOfGridLines = typedArray.getInteger(R.styleable.FPVWidget_uxsdk_gridLineNumber, gridLineView.getNumberOfLines());
        gridLineView.setNumberOfLines(numberOfGridLines);

        boolean gridLinesEnabled = typedArray.getBoolean(R.styleable.FPVWidget_uxsdk_gridLinesEnabled, true);
        setGridLinesEnabled(gridLinesEnabled);

        int centerPointType = typedArray.getInteger(R.styleable.FPVWidget_uxsdk_centerPointType, centerPointView.getType().value());
        centerPointView.setType(CenterPointView.CenterPointType.find(centerPointType));

        int centerPointColor = typedArray.getColor(R.styleable.FPVWidget_uxsdk_centerPointColor, centerPointView.getColor());
        centerPointView.setColor(centerPointColor);

        boolean centerPointEnabled = typedArray.getBoolean(R.styleable.FPVWidget_uxsdk_centerPointEnabled, true);
        setCenterPointEnabled(centerPointEnabled);

        if (!isInEditMode()) {
            boolean interactionEnabled = typedArray.getBoolean(R.styleable.FPVWidget_uxsdk_interactionEnabled, true);
            setInteractionEnabled(interactionEnabled);
        }

        typedArray.recycle();
    }
    //endregion

    /**
     * A callback to get the {@link DJICodecManager} object.
     */
    public interface CodecManagerCallback {

        /**
         * A callback method that is invoked when the {@link DJICodecManager} is initialized or
         * destroyed.
         *
         * @param codecManager An instance of {@link DJICodecManager}, or null if it's been
         *                     destroyed.
         */
        void onCodecManagerChanged(@Nullable DJICodecManager codecManager);
    }
}
