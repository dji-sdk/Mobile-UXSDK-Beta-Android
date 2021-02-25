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

package dji.ux.beta.cameracore.widget.camerashuttersetting;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SettingsDefinitions.ExposureMode;
import dji.common.camera.SettingsDefinitions.ExposureState;
import dji.common.camera.SettingsDefinitions.ShutterSpeed;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.cameracore.base.widget.WheelWidget;
import dji.ux.beta.cameracore.ui.WheelView;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.CameraUtil;
import dji.ux.beta.core.util.SettingDefinitions;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;

/**
 * Camera Shutter Setting Widget
 * <p>
 * The widget displays the current shutter value. It also enables the user to
 * change shutter speed when {@link ExposureMode} is MANUAL or SHUTTER_PRIORITY
 */
public class CameraShutterSettingWidget extends WheelWidget {

    //region Fields
    private static final String TAG = "ShutterSettingWidget";
    private CameraShutterSettingWidgetModel widgetModel;
    private int exposureErrorTextColor;
    private int headerTextColor;
    private int exposureErrorHighlightColor;
    private int normalExposureHighlightColor;
    //endregion

    //region private methods
    public CameraShutterSettingWidget(Context context) {
        super(context);
    }

    public CameraShutterSettingWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraShutterSettingWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            widgetModel =
                    new CameraShutterSettingWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }
        setCameraIndex(SettingDefinitions.CameraIndex.CAMERA_INDEX_0);
        if (attrs != null) {
            initAttributes(context, attrs);
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
        addReaction(widgetModel.isChangeShutterSpeedSupported()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::enableWidget));
        addReaction(widgetModel.getShutterSpeedRange()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateShutterRange));
        addReaction(reactToShutterValueUpdate());
        addReaction(widgetModel.getExposureState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateExposureState));

    }

    private void updateExposureState(ExposureState exposureState) {
        if (exposureState == ExposureState.OVEREXPOSED) {
            makeSpannableString(getResources().getString(R.string.uxsdk_overexposed));
            wheelView.setHighlightColor(exposureErrorHighlightColor);
        } else if (exposureState == ExposureState.UNDEREXPOSED) {
            makeSpannableString(getResources().getString(R.string.uxsdk_underexposed));
            wheelView.setHighlightColor(exposureErrorHighlightColor);
        } else {
            titleTextView.setText(getResources().getString(R.string.uxsdk_shutter_widget_header));
            wheelView.setHighlightColor(normalExposureHighlightColor);
        }
    }

    private void updateShutterValue(ShutterSpeed shutterSpeed, boolean isChangeShutterSpeedSupported) {
        if (shutterSpeed != null) {
            String value = CameraUtil.shutterSpeedDisplayName(shutterSpeed);
            if (isChangeShutterSpeedSupported) {
                wheelView.selectItem(value);
            } else {
                valueTextView.setText(value);
            }
        }
    }

    private void setShutterSpeedToAircraft(int position) {
        addDisposable(widgetModel.getShutterSpeedRange()
                .firstOrError()
                .flatMapCompletable(shutterSpeedRange -> widgetModel.setShutterSpeedValue(shutterSpeedRange[position]))
                .onErrorResumeNext(throwable -> widgetModel.getCurrentShutterSpeedValue().firstOrError()
                        .doOnSuccess(shutterSpeed -> updateShutterValue(shutterSpeed, true)).toCompletable())
                .subscribe(() -> {
                    // Do nothing
                }, logErrorConsumer(TAG, "Set Shutter speed failed ")));

    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_shutter_settings_ratio);
    }


    @Override
    protected void initDefaults() {
        titleTextView.setText(getResources().getString(R.string.uxsdk_shutter_widget_header));
        exposureErrorTextColor = getResources().getColor(R.color.uxsdk_red_500);
        headerTextColor = getResources().getColor(R.color.uxsdk_white);
        exposureErrorHighlightColor = getResources().getColor(R.color.uxsdk_red_500);
        normalExposureHighlightColor = getResources().getColor(R.color.uxsdk_blue_highlight);
    }

    private void updateShutterRange(ShutterSpeed[] shutterSpeeds) {
        List<String> shutterSpeedList = new ArrayList<>();
        for (ShutterSpeed shutterSpeed : shutterSpeeds) {
            shutterSpeedList.add(CameraUtil.shutterSpeedDisplayName(shutterSpeed));
        }
        wheelView.setItems(shutterSpeedList);
    }

    private Disposable reactToShutterValueUpdate() {
        return Flowable.combineLatest(widgetModel.getCurrentShutterSpeedValue(), widgetModel.isChangeShutterSpeedSupported(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateShutterValue(values.first, values.second),
                        logErrorConsumer(TAG, "reactToShutterValueUpdate: "));
    }

    private void makeSpannableString(String exposureWarningText) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        SpannableString str1 = new SpannableString(getResources().getString(R.string.uxsdk_shutter_widget_header));
        str1.setSpan(new ForegroundColorSpan(headerTextColor), 0, str1.length(), 0);
        builder.append(str1);
        builder.append(" ");

        SpannableString str2 = new SpannableString(exposureWarningText);
        str2.setSpan(new ForegroundColorSpan(exposureErrorTextColor), 0, str2.length(), 0);
        str2.setSpan(new RelativeSizeSpan(0.75f), 0, str2.length(), 0);
        builder.append(str2);
        titleTextView.setText(builder, TextView.BufferType.SPANNABLE);
    }

    @Override
    public void onWheelItemChanged(WheelView wheelView, int position) {
        setShutterSpeedToAircraft(position);
    }

    @Override
    public void onWheelItemSelected(WheelView wheelView, int position) {
        //empty method
    }

    @Override
    protected void initAttributes(@NonNull Context context, AttributeSet attrs) {
        super.initAttributes(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraShutterSettingWidget);

        //TODO refactor to prevent crash
//        setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.CameraShutterSettingWidget_uxsdk_cameraIndex, 0)));
//        setLensType(SettingsDefinitions.LensType.find(typedArray.getInt(R.styleable.CameraShutterSettingWidget_uxsdk_lensType, 0)));

        int color = typedArray.getColor(R.styleable.WheelWidget_uxsdk_widgetTitleTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setTitleTextColor(color);
        }

        color = typedArray.getColor(R.styleable.CameraShutterSettingWidget_uxsdk_exposureErrorHighlightColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setExposureErrorHighlightColor(color);
        }

        color = typedArray.getColor(R.styleable.CameraShutterSettingWidget_uxsdk_exposureErrorTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setExposureErrorTextColor(color);
        }

        color = typedArray.getColor(R.styleable.CameraShutterSettingWidget_uxsdk_normalExposureHighlightColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setNormalExposureHighlightColor(color);
        }
        typedArray.recycle();
    }


    //endregion

    //region customizations

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex {@link SettingDefinitions.CameraIndex}
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return {@link SettingDefinitions.CameraIndex}
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
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

    /**
     * Get the exposure error text color
     *
     * @return int value representing color
     */
    @ColorInt
    public int getExposureErrorTextColor() {
        return exposureErrorTextColor;
    }

    /**
     * Set the exposure error text color
     *
     * @param textColor int value
     */
    public void setExposureErrorTextColor(@ColorInt int textColor) {
        exposureErrorTextColor = textColor;
    }


    /**
     * Set the text color of the title
     *
     * @param textColor int value
     */
    @Override
    public void setTitleTextColor(@ColorInt int textColor) {
        headerTextColor = textColor;
    }

    /**
     * Get the text color of the title
     *
     * @return int color value
     */
    @ColorInt
    @Override
    public int getTitleTextColor() {
        return headerTextColor;
    }

    /**
     * Get the highlight color for wheel for exposure error
     *
     * @return int color value
     */
    @ColorInt
    public int getExposureErrorHighlightColor() {
        return exposureErrorHighlightColor;
    }

    /**
     * Set the highlight color for wheel for exposure error
     *
     * @param highlightColor int value
     */
    public void setExposureErrorHighlightColor(@ColorInt int highlightColor) {
        exposureErrorHighlightColor = highlightColor;
    }

    /**
     * Get the highlight color for wheel when exposure is normal
     *
     * @return int color value
     */
    @ColorInt
    public int getNormalExposureHighlightColor() {
        return normalExposureHighlightColor;
    }

    /**
     * Set the highlight color for wheel when exposure is normal
     *
     * @param highlightColor int value
     */
    public void setNormalExposureHighlightColor(@ColorInt int highlightColor) {
        normalExposureHighlightColor = highlightColor;
    }

    //endregion
}
