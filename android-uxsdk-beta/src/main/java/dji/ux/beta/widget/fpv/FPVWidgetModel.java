/*
 * Copyright (c) 2018-2019 DJI
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

package dji.ux.beta.widget.fpv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import dji.common.airlink.PhysicalSource;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.product.Model;
import dji.keysdk.AirLinkKey;
import dji.keysdk.CameraKey;
import dji.keysdk.ProductKey;
import dji.sdk.camera.VideoFeeder;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.functions.Consumer;
import dji.thirdparty.io.reactivex.schedulers.Schedulers;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.WidgetModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DataProcessor;
import dji.ux.beta.util.ProductUtil;
import dji.ux.beta.util.SettingDefinitions;

import static dji.ux.beta.util.SettingDefinitions.CameraIndex.CAMERA_INDEX_0;
import static dji.ux.beta.util.SettingDefinitions.CameraIndex.CAMERA_INDEX_2;
import static dji.ux.beta.util.SettingDefinitions.CameraIndex.CAMERA_INDEX_UNKNOWN;

/**
 * Widget Model for the {@link FPVWidget} used to define
 * the underlying logic and communication
 */
public class FPVWidgetModel extends WidgetModel {
    //region Constants
    private final static String TAG = "FPVWidgetModel";
    //endregion

    //region Fields
    private DataProcessor<Model> modelNameDataProcessor;
    private DataProcessor<SettingsDefinitions.Orientation> orientationProcessor;
    private DataProcessor<SettingsDefinitions.PhotoAspectRatio> photoAspectRatioProcessor;
    private DataProcessor<SettingsDefinitions.CameraMode> cameraModeProcessor;
    private DataProcessor<ResolutionAndFrameRate> resolutionAndFrameRateProcessor;
    private DataProcessor<Boolean> videoViewChangedProcessor;
    private DataProcessor<Boolean> isPrimaryVideoProcessor;
    private DataProcessor<String> cameraNameProcessor;
    private DataProcessor<SettingDefinitions.CameraSide> cameraSideProcessor;

    private SettingDefinitions.CameraIndex currentCameraIndex;
    private VideoFeeder.VideoDataListener videoDataListener;
    private VideoFeeder.VideoFeed currentVideoFeed;
    private SettingDefinitions.VideoSource videoSource;
    private Model model;
    //endregion

    //region Constructor
    public FPVWidgetModel(@NonNull DJISDKModel djiSdkModel,
                          @NonNull ObservableInMemoryKeyedStore keyedStore,
                          @Nullable VideoFeeder.VideoDataListener videoDataListener) {
        super(djiSdkModel, keyedStore);
        this.videoDataListener = videoDataListener;
        this.videoSource = SettingDefinitions.VideoSource.PRIMARY;
        modelNameDataProcessor = DataProcessor.create(Model.UNKNOWN_AIRCRAFT);
        orientationProcessor = DataProcessor.create(SettingsDefinitions.Orientation.UNKNOWN);
        photoAspectRatioProcessor = DataProcessor.create(SettingsDefinitions.PhotoAspectRatio.UNKNOWN);
        cameraModeProcessor = DataProcessor.create(SettingsDefinitions.CameraMode.UNKNOWN);
        resolutionAndFrameRateProcessor =
                DataProcessor.create(new ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.UNKNOWN,
                        SettingsDefinitions.VideoFrameRate.UNKNOWN));
        videoViewChangedProcessor = DataProcessor.create(false);
        isPrimaryVideoProcessor = DataProcessor.create(true);
        //default for the following fields is an empty string
        cameraNameProcessor = DataProcessor.create("");
        cameraSideProcessor = DataProcessor.create(SettingDefinitions.CameraSide.UNKNOWN);
        currentCameraIndex = CAMERA_INDEX_UNKNOWN;
    }
    //endregion

    //region Data

    /**
     * User can set video source to these three options: AUTO, PRIMARY, SECONDARY. By
     * default, the video source is set to "AUTO" if user does not specify it.
     *
     * @param videoSource An enum value of `VideoSource`.
     */
    public void setVideoSource(@NonNull SettingDefinitions.VideoSource videoSource) {
        this.videoSource = videoSource;
        restart();
    }

    /**
     * Get the current video source.
     *
     * @return An enum value of `VideoSource`.
     */
    @Nullable
    public SettingDefinitions.VideoSource getVideoSource() {
        return videoSource;
    }

    /**
     * Get the current camera index. This value should only be used for video size calculation.
     * To get the camera side, use {@link FPVWidgetModel#getCameraSide()} instead.
     *
     * @return The camera index.
     */
    public SettingDefinitions.CameraIndex getCurrentCameraIndex() {
        return currentCameraIndex;
    }

    /**
     * Get the model of the product
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Model> getModel() {
        return modelNameDataProcessor.toFlowable();
    }

    /**
     * Get the orientation of the video feed
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingsDefinitions.Orientation> getOrientation() {
        return orientationProcessor.toFlowable();
    }

    /**
     * Get whether the video feed is the primary video feed
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> isPrimaryVideoFeed() {
        return isPrimaryVideoProcessor.toFlowable();
    }

    /**
     * Get whether the video view has changed
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> hasVideoViewChanged() {
        return videoViewChangedProcessor.toFlowable();
    }

    /**
     * Get the name of the current camera
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<String> getCameraName() {
        return cameraNameProcessor.toFlowable();
    }

    /**
     * Get the current camera's side
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SettingDefinitions.CameraSide> getCameraSide() {
        return cameraSideProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        //TODO: Add peak threshold, overexposure and dimensions event for grid display once inter-widget communication is done
        ProductKey modelNameKey = ProductKey.create(ProductKey.MODEL_NAME);
        CameraKey orientationKey = CameraKey.create(CameraKey.ORIENTATION);

        CameraKey photoAspectRatioKey = CameraKey.create(CameraKey.PHOTO_ASPECT_RATIO);
        CameraKey cameraModeKey = CameraKey.create(CameraKey.MODE);
        CameraKey videoResolutionAndFrameRateKey = CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE);

        bindDataProcessor(modelNameKey, modelNameDataProcessor, model -> {
            this.model = (Model) model;
            updateVideoFeed();
            updateCameraDisplay();
        });
        Consumer<Object> videoViewChangedConsumer = hasChanged -> videoViewChangedProcessor.onNext(true);
        bindDataProcessor(orientationKey, orientationProcessor, videoViewChangedConsumer);
        bindDataProcessor(photoAspectRatioKey, photoAspectRatioProcessor, videoViewChangedConsumer);
        bindDataProcessor(cameraModeKey, cameraModeProcessor, videoViewChangedConsumer);
        bindDataProcessor(videoResolutionAndFrameRateKey, resolutionAndFrameRateProcessor, videoViewChangedConsumer);
    }

    @Override
    protected void inCleanup() {
        if (currentVideoFeed != null && currentVideoFeed.getListeners().contains(videoDataListener)) {
            currentVideoFeed.removeVideoDataListener(videoDataListener);
        }
    }
    //endregion

    //region Updates
    @Override
    protected void updateStates() {
        updateCameraDisplay();
    }
    //endregion

    //region Helpers
    private void updateVideoFeed() {
        if (getVideoFeeder() == null) {
            return;
        }
        if (videoSource.equals(SettingDefinitions.VideoSource.AUTO)) {
            if (isExtPortSupportedProduct()) {
                registerLiveVideo(getVideoFeeder().getSecondaryVideoFeed(), false);
                switchToExternalCameraChannel();
            } else {
                registerLiveVideo(getVideoFeeder().getPrimaryVideoFeed(), true);
            }
        } else if (videoSource.equals(SettingDefinitions.VideoSource.PRIMARY)) {
            registerLiveVideo(getVideoFeeder().getPrimaryVideoFeed(), true);
        } else if (videoSource.equals(SettingDefinitions.VideoSource.SECONDARY)) {
            registerLiveVideo(getVideoFeeder().getSecondaryVideoFeed(), false);
        }
    }

    private void registerLiveVideo(VideoFeeder.VideoFeed videoFeed, boolean isPrimary) {
        if (videoFeed != null && videoDataListener != null) {
            // Note: this check is necessary to avoid set the same videoDataListener to two different video feeds
            if (currentVideoFeed != null && currentVideoFeed.getListeners().contains(videoDataListener)) {
                currentVideoFeed.removeVideoDataListener(videoDataListener);
            }
            currentVideoFeed = videoFeed;
            currentVideoFeed.addVideoDataListener(videoDataListener);
        }
        if (currentVideoFeed != null && currentVideoFeed.getVideoSource() == null) {
            videoSource = null;
        }
        isPrimaryVideoProcessor.onNext(isPrimary);
    }

    private void switchToExternalCameraChannel() {
        AirLinkKey extVideoInputPortEnabledKey =
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED);
        Boolean extEnabled = (Boolean) djiSdkModel.getCacheValue(extVideoInputPortEnabledKey);
        if (extEnabled == null || !extEnabled) {
            addDisposable(djiSdkModel.setValue(extVideoInputPortEnabledKey, true)
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::setCameraChannelBandwidth,
                            error -> logErrorConsumer(TAG, "SetExtVideoInputPortEnabled: ")));
        } else {
            setCameraChannelBandwidth();
        }
    }

    private void setCameraChannelBandwidth() {
        AirLinkKey bandwidthAllocationForLBVideoInputPort =
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT);
        addDisposable(djiSdkModel.setValue(bandwidthAllocationForLBVideoInputPort, 0.0f)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                    //Do nothing
                }, error -> logErrorConsumer(TAG, "SetBandwidthAllocationForLBVideoInputPort: ")));
    }

    private void updateCameraDisplay() {
        CameraKey displayName0Key = CameraKey.create(CameraKey.DISPLAY_NAME, 0);
        CameraKey displayName1Key = CameraKey.create(CameraKey.DISPLAY_NAME, 1);
        String displayName0 = (String) djiSdkModel.getCacheValue(displayName0Key);
        String displayName1 = (String) djiSdkModel.getCacheValue(displayName1Key);
        String displayName = "";
        SettingDefinitions.CameraSide displaySide = SettingDefinitions.CameraSide.UNKNOWN;
        currentCameraIndex = CAMERA_INDEX_UNKNOWN;
        if (displayName0 == null) {
            displayName0 = PhysicalSource.UNKNOWN.toString();
        }
        if (displayName1 == null) {
            displayName1 = PhysicalSource.UNKNOWN.toString();
        }

        PhysicalSource physicalVideoSource;
        if (model != null && currentVideoFeed != null) {
            physicalVideoSource = currentVideoFeed.getVideoSource();
            if ((physicalVideoSource == null) || (physicalVideoSource
                    == PhysicalSource.UNKNOWN)) {
                displayName = PhysicalSource.UNKNOWN.toString();
            } else {
                if (isExtPortSupportedProduct()) {
                    if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0;
                        currentCameraIndex = CAMERA_INDEX_UNKNOWN;
                    } else if (physicalVideoSource == PhysicalSource.EXT) {
                        displayName = PhysicalSource.EXT.toString();
                        currentCameraIndex = CAMERA_INDEX_2;
                    } else if (physicalVideoSource == PhysicalSource.HDMI) {
                        displayName = PhysicalSource.HDMI.toString();
                        currentCameraIndex = CAMERA_INDEX_2;
                    } else if (physicalVideoSource == PhysicalSource.AV) {
                        displayName = PhysicalSource.AV.toString();
                        currentCameraIndex = CAMERA_INDEX_2;
                    }
                } else if (model == Model.MATRICE_210_RTK || model == Model.MATRICE_210
                        || model == Model.MATRICE_210_RTK_V2 || model == Model.MATRICE_210_V2) {
                    if (physicalVideoSource == PhysicalSource.LEFT_CAM) {
                        displayName = displayName0;
                        displaySide = SettingDefinitions.CameraSide.PORT;
                        currentCameraIndex = CAMERA_INDEX_0;
                    } else if (physicalVideoSource == PhysicalSource.RIGHT_CAM) {
                        displayName = displayName1;
                        displaySide = SettingDefinitions.CameraSide.STARBOARD;
                        currentCameraIndex = CAMERA_INDEX_2;
                    } else if (physicalVideoSource == PhysicalSource.FPV_CAM) {
                        displayName = PhysicalSource.FPV_CAM.toString();
                        currentCameraIndex = CAMERA_INDEX_UNKNOWN; //Index will be assigned as required
                    } else if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0;
                        currentCameraIndex = CAMERA_INDEX_0;
                    }
                } else if (model == Model.INSPIRE_2 || model == Model.MATRICE_200_V2) {
                    if (physicalVideoSource == PhysicalSource.MAIN_CAM) {
                        displayName = displayName0;
                        currentCameraIndex = CAMERA_INDEX_0;
                    } else if (physicalVideoSource == PhysicalSource.FPV_CAM) {
                        displayName = PhysicalSource.FPV_CAM.toString();
                        currentCameraIndex = CAMERA_INDEX_UNKNOWN; //Index will be assigned as required
                    }
                } else {
                    displayName = displayName0;
                    currentCameraIndex = CAMERA_INDEX_0;
                }
            }
        }
        cameraNameProcessor.onNext(displayName);
        cameraSideProcessor.onNext(displaySide);
    }
    //endregion

    //region Helpers for unit testing

    /**
     * A wrapper for the {@link VideoFeeder#getInstance()} method so it can be mocked in unit tests.
     *
     * @return An instance of {@link VideoFeeder}.
     */
    @VisibleForTesting
    @Nullable
    protected VideoFeeder getVideoFeeder() {
        return VideoFeeder.getInstance();
    }

    /**
     * A wrapper for the {@link ProductUtil#isExtPortSupportedProduct()} method so it can be mocked
     * in unit tests.
     *
     * @return `true` if the connected product supports external video input. `false if there is
     * no product connected or if the connected product does not support external video input.
     */
    @VisibleForTesting
    protected boolean isExtPortSupportedProduct() {
        return ProductUtil.isExtPortSupportedProduct();
    }
    //endregion
}
