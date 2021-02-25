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

package dji.ux.beta.cameracore.widget.cameraaperturesetting;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.Aperture;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.base.widget.WheelWidget;
import dji.ux.beta.cameracore.ui.WheelView;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Camera Aperture Setting Widget
 * <p>
 * The widget displays the current aperture value. It also enables the user
 * to change aperture when {@link ExposureMode} is MANUAL or APERTURE_PRIORITY
 */
public class CameraApertureSettingWidget extends WheelWidget {

    //region Fields
    private static final String TAG = "ApertureSettingWidget";
    private CameraApertureSettingWidgetModel widgetModel;
    //endregion

    //region Lifecycle
    public CameraApertureSettingWidget(@NonNull Context context) {
        super(context);
    }

    public CameraApertureSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraApertureSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            widgetModel = new CameraApertureSettingWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
        setCameraIndex(CameraIndex.CAMERA_INDEX_0);
    }

    @Override
    protected void initDefaults() {
        titleTextView.setText(getResources().getString(R.string.uxsdk_aperture_widget_header));

    }

    private void setApertureToAircraft(int position) {
        addDisposable(widgetModel.getApertureRange()
                .firstOrError()
                .flatMapCompletable(apertureRange -> widgetModel.setApertureValue(apertureRange[position]))
                .onErrorResumeNext(throwable -> widgetModel.getCurrentApertureValue().firstOrError()
                        .doOnSuccess(aperture -> updateApertureValue(aperture, true)).toCompletable())
                .subscribe(() -> {
                    // Do nothing
                }, logErrorConsumer(TAG, "Set Aperture failed ")));

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
        addReaction(widgetModel.isChangeApertureSupported()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::enableWidget));
        addReaction(widgetModel.getApertureRange()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateApertureRange));
        addReaction(reactToApertureValueUpdate());

    }

    private void updateApertureValue(Aperture aperture, boolean isChangeApertureSupported) {
        if (aperture != null) {
            String value = CameraUtil.apertureDisplayName(getResources(), aperture);
            if (isChangeApertureSupported) {
                wheelView.selectItem(value);
            } else {
                valueTextView.setText(value);
            }
        }
    }

    private void updateApertureRange(Aperture[] apertures) {
        List<String> apertureList = new ArrayList<>();
        for (Aperture aperture : apertures) {
            apertureList.add(CameraUtil.apertureDisplayName(getResources(), aperture));
        }
        wheelView.setItems(apertureList);
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_aperture_settings_ratio);
    }

    private Disposable reactToApertureValueUpdate() {
        return Flowable.combineLatest(widgetModel.getCurrentApertureValue(), widgetModel.isChangeApertureSupported(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateApertureValue(values.first, values.second),
                        logErrorConsumer(TAG, "reactToApertureValueUpdate: "));
    }

    @Override
    public void onWheelItemChanged(WheelView wheelView, int position) {
        setApertureToAircraft(position);
    }

    @Override
    public void onWheelItemSelected(WheelView wheelView, int position) {
        // Empty Method
    }
    //endregion

    //region customizations

    /**
     * Gets the camera index used by the widget.
     *
     * @return instance of {@link CameraIndex}.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the camera key index for which this model should subscribe to.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the current type of the lens the widget is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public SettingsDefinitions.LensType getLensType() {
        return widgetModel.getLensType();
    }

    /**
     * Set the type of the lens for which the widget should react
     *
     * @param lensType lens type
     */
    public void setLensType(@NonNull SettingsDefinitions.LensType lensType) {
        if (!isInEditMode()) {
            widgetModel.setLensType(lensType);
        }
    }
    //endregion


}
