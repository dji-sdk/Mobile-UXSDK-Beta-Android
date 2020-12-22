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

package dji.ux.beta.cameracore.widget.cameracapture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;

import java.util.HashMap;
import java.util.Map;

import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.widget.cameracapture.recordvideo.RecordVideoWidget;
import dji.ux.beta.cameracore.widget.cameracapture.shootphoto.ShootPhotoWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;

/**
 * Camera Capture Widget
 * <p>
 * Widget can be used to shoot photo and record video. It reacts to change in {@link CameraMode}
 * It encloses {@link ShootPhotoWidget} and {@link RecordVideoWidget} for respective modes
 */
public class CameraCaptureWidget extends ConstraintLayoutWidget {

    //region Fields
    private static final String TAG = "CameraCaptureWidget";
    private CameraCaptureWidgetModel widgetModel;
    private Map<CameraMode, View> widgetMap;
    //endregion

    //region Lifecycle
    public CameraCaptureWidget(Context context) {
        super(context);
    }

    public CameraCaptureWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraCaptureWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        widgetMap = new HashMap<>();
        addViewByMode(CameraMode.SHOOT_PHOTO, new ShootPhotoWidget(context));
        addViewByMode(CameraMode.RECORD_VIDEO, new RecordVideoWidget(context));

        if (!isInEditMode()) {
            widgetModel =
                    new CameraCaptureWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
    }

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
        addReaction(
                widgetModel.getCameraMode()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(
                                this::onCameraModeChange,
                                logErrorConsumer(TAG, "Camera Mode Change: ")));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }
    //endregion

    //region private helpers
    private void onCameraModeChange(CameraMode cameraMode) {
        for (View view : widgetMap.values()) {
            if (view != null) view.setVisibility(GONE);
        }
        View currentView = widgetMap.get(cameraMode);
        if (currentView != null) {
            widgetMap.get(cameraMode).setVisibility(VISIBLE);
        }
    }
    //endregion

    //region customizations

    /**
     * Add view to be shown based on camera mode
     *
     * @param cameraMode instance of camera mode
     * @param view       the view to be shown for camera mode
     */
    public void addViewByMode(@NonNull CameraMode cameraMode, @NonNull View view) {
        if (widgetMap.get(cameraMode) != null) {
            removeView(widgetMap.get(cameraMode));
        }
        widgetMap.put(cameraMode, view);
        view.setVisibility(GONE);
        addView(view);
        ConstraintLayout.LayoutParams lp = new Constraints.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(lp);
    }

    /**
     * Remove the view based on camera mode
     *
     * @param cameraMode for which the view should be removed
     */
    public void removeViewByMode(@NonNull CameraMode cameraMode) {
        if (widgetMap.get(cameraMode) == null) return;
        removeView(widgetMap.get(cameraMode));
        widgetMap.remove(cameraMode);
    }

    /**
     * Get the view that will be shown based on camera mode
     *
     * @param cameraMode for which the view is shown
     * @return View for the mode
     */
    @Nullable
    public View getViewByMode(@NonNull CameraMode cameraMode) {
        return widgetMap.get(cameraMode);
    }

    /**
     * Get shoot photo widget
     *
     * @return instance of {@link ShootPhotoWidget}
     */
    @Nullable
    public ShootPhotoWidget getShootPhotoWidget() {
        if (widgetMap.get(CameraMode.SHOOT_PHOTO) == null || !(widgetMap.get(CameraMode.SHOOT_PHOTO) instanceof ShootPhotoWidget)) {
            return null;
        }
        return (ShootPhotoWidget) widgetMap.get(CameraMode.SHOOT_PHOTO);
    }

    /**
     * Get record video widget
     *
     * @return instance of {@link RecordVideoWidget}
     */
    @Nullable
    public RecordVideoWidget getRecordVideoWidget() {
        if (widgetMap.get(CameraMode.RECORD_VIDEO) == null || !(widgetMap.get(CameraMode.RECORD_VIDEO) instanceof RecordVideoWidget)) {
            return null;
        }
        return (RecordVideoWidget) widgetMap.get(CameraMode.RECORD_VIDEO);
    }
    //endregion
}
