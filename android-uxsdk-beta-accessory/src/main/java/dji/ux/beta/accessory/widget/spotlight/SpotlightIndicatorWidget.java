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

package dji.ux.beta.accessory.widget.spotlight;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.log.DJILog;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.accessory.R;
import dji.ux.beta.accessory.widget.spotlight.SpotlightIndicatorWidgetModel.SpotlightIndicatorState;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Widget represents the state of Spotlight accessory
 * Widget is configured to show only when the accessory is connected
 * Tapping on the widget can be used to open the {@link SpotlightControlWidget}
 */
public class SpotlightIndicatorWidget extends FrameLayoutWidget implements OnClickListener {

    //region Fields
    private static final String TAG = "SpotlightIndWidget";
    private SpotlightIndicatorWidgetModel widgetModel;
    private ImageView foregroundImageView;
    private Drawable spotlightEnabledIcon;
    private Drawable spotlightDisabledIcon;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    private int stateChangeResourceId;
    //endregion

    //region Lifecycle
    public SpotlightIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public SpotlightIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SpotlightIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_spotlight_indicator, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        foregroundImageView = findViewById(R.id.image_view_spotlight_indicator);
        setOnClickListener(this);

        if (!isInEditMode()) {
            widgetModel = new SpotlightIndicatorWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        stateChangeResourceId = INVALID_RESOURCE;
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
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
        destroyListener();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getSpotlightState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }
    //endregion

    //region private methods
    private void initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.getRootView() != null) {
            View widgetView = this.getRootView().findViewById(stateChangeResourceId);
            if (widgetView instanceof SpotlightControlWidget) {
                setStateChangeCallback((SpotlightControlWidget) widgetView);
            }
        }
    }

    private void destroyListener() {
        stateChangeCallback = null;
    }

    private void initDefaults() {
        spotlightEnabledIcon = getResources().getDrawable(R.drawable.uxsdk_ic_spotlight_enabled);
        spotlightDisabledIcon = getResources().getDrawable(R.drawable.uxsdk_ic_spotlight_disabled);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpotlightIndicatorWidget);
        stateChangeResourceId =
                typedArray.getResourceId(R.styleable.SpotlightIndicatorWidget_uxsdk_onStateChange, INVALID_RESOURCE);

        if (typedArray.getDrawable(R.styleable.SpotlightIndicatorWidget_uxsdk_spotlightEnabled) != null) {
            spotlightEnabledIcon = typedArray.getDrawable(R.styleable.SpotlightIndicatorWidget_uxsdk_spotlightEnabled);
        }

        if (typedArray.getDrawable(R.styleable.SpotlightIndicatorWidget_uxsdk_spotlightDisabled) != null) {
            spotlightDisabledIcon = typedArray.getDrawable(R.styleable.SpotlightIndicatorWidget_uxsdk_spotlightDisabled);
        }
        setIconBackground(typedArray.getDrawable(R.styleable.SpotlightIndicatorWidget_uxsdk_iconBackground));

        typedArray.recycle();
    }

    private void updateUI(SpotlightIndicatorState spotlightState) {
        if (spotlightState == SpotlightIndicatorState.HIDDEN) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            if (spotlightState == SpotlightIndicatorState.ACTIVE) {
                foregroundImageView.setImageDrawable(spotlightEnabledIcon);
            } else if (spotlightState == SpotlightIndicatorState.INACTIVE) {
                foregroundImageView.setImageDrawable(spotlightDisabledIcon);
            }
        }
    }

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getSpotlightState()
                    .lastOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::updateUI, error -> {
                        if (error instanceof UXSDKError) {
                            DJILog.e(TAG, error.toString());
                        }
                    }));
        }
    }

    //endregion

    /**
     * Set call back for when the widget is tapped.
     * This can be used to link the widget to {@link SpotlightControlWidget}
     *
     * @param stateChangeCallback listener to handle call backs
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }

    //region customizations

    /**
     * Set spotlight enabled resource id
     *
     * @param resourceId resource id of spotlight enabled
     */
    public void setSpotlightEnabledIcon(@DrawableRes int resourceId) {
        setSpotlightEnabledIcon(getResources().getDrawable(resourceId));
    }


    /**
     * Set spotlight enabled drawable
     *
     * @param drawable Object to be used as spotlight enabled
     */
    public void setSpotlightEnabledIcon(@Nullable Drawable drawable) {
        spotlightEnabledIcon = drawable;
        checkAndUpdateUI();
    }


    /**
     * Get spotlight enabled icon drawable
     */
    @Nullable
    public Drawable getSpotlightEnabledIcon() {
        return spotlightEnabledIcon;
    }


    /**
     * Set spotlight disabled resource
     *
     * @param resourceId resource id of spotlight disabled
     */
    public void setSpotlightDisabledIcon(@DrawableRes int resourceId) {
        setSpotlightDisabledIcon(getResources().getDrawable(resourceId));
    }


    /**
     * Set spotlight disabled drawable
     *
     * @param drawable Object to be used as spotlight disabled icon
     */
    public void setSpotlightDisabledIcon(@Nullable Drawable drawable) {
        spotlightDisabledIcon = drawable;
        checkAndUpdateUI();
    }


    /**
     * Get spotlight disabled icon
     */
    @Nullable
    public Drawable getSpotlightDisabledIcon() {
        return spotlightDisabledIcon;
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
     * @param drawable Object to be used as background
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get current background of icon
     */
    @Nullable
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    //endregion
}

