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

package dji.ux.beta.widget.focusmode;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.SettingsDefinitions.FocusMode;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.R;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.FrameLayoutWidget;
import dji.ux.beta.base.GlobalPreferencesManager;
import dji.ux.beta.base.SchedulerProvider;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.SettingDefinitions;

/**
 * Widget will display the current focus mode of aircraft camera.
 * - MF text highlighted (in green) indicates focus mode is Manual Focus.
 * - AF text highlighted (in green) indicates focus mode is Auto Focus.
 * - AFC text highlighted (in green) indicates focus mode is Auto Focus Continuous.
 * <p>
 * Interaction:
 * Tapping will toggle between AF and MF mode.
 */
public class FocusModeWidget extends FrameLayoutWidget implements OnClickListener {

    //region constants
    private static final String TAG = "FocusModeWidget";
    //endregion

    //region fields
    private FocusModeWidgetModel widgetModel;
    private TextView titleTextView;
    private int activeColor;
    private int inactiveColor;
    private SchedulerProvider schedulerProvider;
    //endregion

    //region lifecycle
    public FocusModeWidget(Context context) {
        super(context);
    }

    public FocusModeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusModeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_focus_mode_switch, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        titleTextView = findViewById(R.id.text_view_camera_control_af);
        schedulerProvider = SchedulerProvider.getInstance();
        if (!isInEditMode()) {
            widgetModel = new FocusModeWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance(),
                    schedulerProvider);
        }
        setOnClickListener(this);
        activeColor = getResources().getColor(R.color.uxsdk_green);
        inactiveColor = getResources().getColor(R.color.uxsdk_white);
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
        reactToFocusModeChange();
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        addDisposable(widgetModel.toggleFocusMode().subscribe(() -> {
            // Do nothing
        }, logErrorConsumer(TAG, "switch focus mode: ")));
    }

    //endregion

    //region private helpers

    private void checkAndUpdateUI() {
        addDisposable(reactToFocusModeChange());
    }

    private Disposable reactToFocusModeChange() {
        return Flowable.combineLatest(widgetModel.isAFCEnabled(), widgetModel.getFocusMode(), Pair::new)
                .observeOn(schedulerProvider.ui())
                .subscribe(values -> updateUI(values.first, values.second),
                        logErrorConsumer(TAG, "react to Focus Mode Change: "));
    }

    private void updateUI(boolean isAFCEnabled, FocusMode focusMode) {
        int autoFocusTextColor;
        int manualFocusTextColor;
        if (focusMode == FocusMode.MANUAL) {
            manualFocusTextColor = activeColor;
            autoFocusTextColor = inactiveColor;
        } else {
            autoFocusTextColor = activeColor;
            manualFocusTextColor = inactiveColor;
        }

        String autoFocusText;
        if (isAFCEnabled) {
            autoFocusText = getResources().getString(R.string.uxsdk_widget_focus_mode_afc);
        } else {
            autoFocusText = getResources().getString(R.string.uxsdk_widget_focus_mode_auto);

        }

        makeSpannableString(autoFocusText, autoFocusTextColor, manualFocusTextColor);
    }

    private void makeSpannableString(String autoFocusText, int autoFocusColor, int manualFocusColor) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString str1 = new SpannableString(autoFocusText);
        str1.setSpan(new ForegroundColorSpan(autoFocusColor), 0, str1.length(), 0);
        builder.append(str1);

        SpannableString str2 = new SpannableString(getResources().getString(R.string.uxsdk_widget_focus_mode_separator));
        str2.setSpan(new ForegroundColorSpan(Color.WHITE), 0, str2.length(), 0);
        builder.append(str2);

        SpannableString str3 = new SpannableString(getResources().getString(R.string.uxsdk_widget_focus_mode_manual));
        str3.setSpan(new ForegroundColorSpan(manualFocusColor), 0, str3.length(), 0);
        builder.append(str3);
        titleTextView.setText(builder, TextView.BufferType.SPANNABLE);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FocusModeWidget);
        if (!isInEditMode()) {
            setCameraIndex(SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.FocusModeWidget_uxsdk_cameraIndex, 0)));
        }
        activeColor = typedArray.getColor(R.styleable.FocusModeWidget_uxsdk_activeModeTextColor, getResources().getColor(R.color.uxsdk_green));
        inactiveColor = typedArray.getColor(R.styleable.FocusModeWidget_uxsdk_inactiveModeTextColor, getResources().getColor(R.color.uxsdk_white));
        Drawable background = typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawable);
        setTitleBackground(background);
        typedArray.recycle();
    }
    //endregion

    //region customizations

    /**
     * Set the index of camera to which the widget should react
     *
     * @param cameraIndex index of the camera.
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
     * Set active mode text color
     *
     * @param color color integer
     */
    public void setActiveModeTextColor(@ColorInt int color) {
        activeColor = color;
        checkAndUpdateUI();
    }

    /**
     * Get active mode text color
     *
     * @return color integer
     */
    @ColorInt
    public int getActiveModeTextColor() {
        return activeColor;
    }

    /**
     * Set in-active mode text color
     *
     * @param color color integer
     */
    public void setInactiveModeTextColor(@ColorInt int color) {
        inactiveColor = color;
        checkAndUpdateUI();
    }

    /**
     * Get in-active mode text color
     *
     * @return color integer
     */
    @ColorInt
    public int getInactiveModeTextColor() {
        return inactiveColor;
    }

    /**
     * Set background to title text
     *
     * @param resourceId resource id of background
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     *
     * @param drawable Drawable to be used as background
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    /**
     * Get current background of title text
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Sets the text size of the widget text
     *
     * @param textSize text size float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        titleTextView.setTextSize(textSize);
    }

    /**
     * Get current text size
     *
     * @return text size of the title
     */
    @Dimension
    public float getTitleTextSize() {
        return titleTextView.getTextSize();
    }

    //endregion
}
