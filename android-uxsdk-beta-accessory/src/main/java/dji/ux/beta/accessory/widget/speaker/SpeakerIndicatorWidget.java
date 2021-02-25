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

package dji.ux.beta.accessory.widget.speaker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.log.DJILog;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.accessory.R;
import dji.ux.beta.accessory.widget.speaker.SpeakerIndicatorWidgetModel.SpeakerIndicatorState;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Speaker Indicator Widget will display the state of the speaker accessory
 * <p>
 * Widget is configured to show only when the accessory is connected
 * When widget is visible it displays if the speaker is currently playing audio
 * <p>
 * Tapping the widget can be used to open {@link SpeakerControlWidget}
 */
public class SpeakerIndicatorWidget extends FrameLayoutWidget implements View.OnClickListener {

    //region
    private static final String TAG = "SpeakerIndicatorWidget";
    private SpeakerIndicatorWidgetModel widgetModel;
    private Drawable speakerActiveIcon;
    private Drawable speakerInactiveIcon;
    private ImageView foregroundImageView;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    private int stateChangeResourceId;


    //endregion

    //region Lifecycle
    public SpeakerIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public SpeakerIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SpeakerIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_speaker_indicator, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        foregroundImageView = findViewById(R.id.image_view_speaker_indicator);

        if (!isInEditMode()) {
            widgetModel =
                    new SpeakerIndicatorWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        stateChangeResourceId = INVALID_RESOURCE;
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getSpeakerIndicatorState().observeOn(SchedulerProvider.ui()).subscribe(this::updateUI));
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
        initializeListener();

    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }


    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
    }

    //endregion

    //region private methods

    private void initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.getRootView() != null) {
            View widgetView = this.getRootView().findViewById(stateChangeResourceId);
            if (widgetView instanceof SpeakerControlWidget) {
                setStateChangeCallback((SpeakerControlWidget) widgetView);
            }
        }
    }

    private void initDefaults() {
        speakerActiveIcon = getResources().getDrawable(R.drawable.uxsdk_ic_speaker_active);
        speakerInactiveIcon = getResources().getDrawable(R.drawable.uxsdk_ic_speaker_inactive);
    }


    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeakerIndicatorWidget);
        stateChangeResourceId =
                typedArray.getResourceId(R.styleable.SpeakerIndicatorWidget_uxsdk_onStateChange, INVALID_RESOURCE);
        if (typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_speakerActiveDrawable) != null) {
            speakerActiveIcon = typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_speakerActiveDrawable);
        }

        if (typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_speakerInactiveDrawable) != null) {
            speakerInactiveIcon = typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_speakerInactiveDrawable);
        }
        if (typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_iconBackground) != null) {
            setIconBackground(typedArray.getDrawable(R.styleable.SpeakerIndicatorWidget_uxsdk_iconBackground));
        }

        typedArray.recycle();
    }

    private void updateUI(SpeakerIndicatorState speakerIndicatorState) {
        if (speakerIndicatorState == SpeakerIndicatorState.HIDDEN) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            if (speakerIndicatorState == SpeakerIndicatorState.ACTIVE) {
                foregroundImageView.setImageDrawable(speakerActiveIcon);
            } else if (speakerIndicatorState == SpeakerIndicatorState.INACTIVE) {
                foregroundImageView.setImageDrawable(speakerInactiveIcon);
            }
        }
    }

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getSpeakerIndicatorState().lastOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(
                            this::updateUI,
                            error -> {
                                if (error instanceof UXSDKError) {
                                    DJILog.e(TAG, error.toString());
                                }
                            }));
        }
    }
    //endregion

    //region customizations

    /**
     * Set call back for when the widget is tapped.
     * This can be used to link the widget to {@link SpeakerControlWidget}
     *
     * @param stateChangeCallback listener to handle call backs
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }

    /**
     * Set speaker active state icon resource
     *
     * @param resourceId resource id of speaker active icon
     */
    public void setSpeakerActiveIcon(@DrawableRes int resourceId) {
        setSpeakerActiveIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set speaker active state icon drawable
     *
     * @param drawable to be used as speaker active
     */
    public void setSpeakerActiveIcon(@Nullable Drawable drawable) {
        speakerActiveIcon = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get speaker active state icon drawable
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getSpeakerActiveIcon() {
        return speakerActiveIcon;
    }


    /**
     * Set speaker inactive state icon resource
     *
     * @param resourceId resource id of speaker inactive icon
     */
    public void setSpeakerInactiveIcon(@DrawableRes int resourceId) {
        setSpeakerInactiveIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set speaker inactive state icon drawable
     *
     * @param drawable to be used as speaker inactive state
     */
    public void setSpeakerInactiveIcon(@Nullable Drawable drawable) {
        speakerInactiveIcon = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get speaker inactive state icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getSpeakerInactiveIcon() {
        return speakerInactiveIcon;
    }

    /**
     * Set background to icon
     *
     * @param resourceId resource id of background
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        setIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to icon
     *
     * @param drawable to be used as background
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get current background of icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    //endregion
}
