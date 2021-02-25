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

package dji.ux.beta.media.widget.accesslocker;

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

import dji.common.flightcontroller.accesslocker.AccessLockerState;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;
import dji.ux.beta.media.R;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Access Locker Indicator Widget
 * <p>
 * Access Locker is a feature provided with the Mavic 2 Enterprise series which
 * enables the user to secure the usage of the aircraft. More details of the feature
 * can be found on the @see <a href="https://www.dji.com/mavic-2-enterprise">website</a>.
 * <p>
 * The widget will display the state of the access locker feature.
 * Tapping the widget can be used to open {@link AccessLockerControlWidget}
 */
public class AccessLockerIndicatorWidget extends FrameLayoutWidget implements OnClickListener {
    //region private fields
    private static final String TAG = "AccessLockerIndWidget";
    private ImageView foregroundImageView;
    private AccessLockerIndicatorWidgetModel widgetModel;
    private Drawable notInitializedDrawable;
    private Drawable unlockedDrawable;
    private Drawable lockedDrawable;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    private int stateChangeResourceId;
    //endregion

    //region Lifecycle
    public AccessLockerIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public AccessLockerIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessLockerIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_access_locker_indicator, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        foregroundImageView = findViewById(R.id.image_view_foreground);
        setOnClickListener(this);

        if (!isInEditMode()) {
            widgetModel = new AccessLockerIndicatorWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        stateChangeResourceId = INVALID_RESOURCE;
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
        initializeListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        destroyListener();
        super.onDetachedFromWindow();
    }

    private void initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.getRootView() != null) {
            View widgetView = this.getRootView().findViewById(stateChangeResourceId);
            if (widgetView instanceof AccessLockerControlWidget) {
                setStateChangeCallback((AccessLockerControlWidget) widgetView);
            }
        }
    }

    private void destroyListener() {
        stateChangeCallback = null;
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(
                widgetModel.isAccessLockerSupported()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(
                                this::updateVisibility,
                                logErrorConsumer(TAG, "Access Locker Supported: ")));
        addReaction(
                widgetModel.getAccessLockerState()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(
                                this::onAccessLockerStateChange,
                                logErrorConsumer(TAG, "Access Locker State Change: ")));
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
    }
    //endregion

    //region private fields
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccessLockerIndicatorWidget);
        stateChangeResourceId =
                typedArray.getResourceId(R.styleable.AccessLockerIndicatorWidget_uxsdk_onStateChange, INVALID_RESOURCE);
        if (typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerNotInitialized) != null) {
            notInitializedDrawable = typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerNotInitialized);
        }
        if (typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerUnlocked) != null) {
            unlockedDrawable =
                    typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerUnlocked);
        }
        if (typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerLocked) != null) {
            lockedDrawable =
                    typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_accessLockerLocked);
        }
        setAccessLockerIconBackground(typedArray.getDrawable(R.styleable.AccessLockerIndicatorWidget_uxsdk_iconBackground));
        typedArray.recycle();
    }

    private void initDefaults() {
        notInitializedDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_access_locker_not_initialized);
        unlockedDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_access_locker_unlocked);
        lockedDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_access_locker_locked);
    }

    private void updateVisibility(Boolean isSupported) {
        setVisibility(isSupported ? VISIBLE : GONE);
    }

    private void onAccessLockerStateChange(AccessLockerState accessLockerState) {
        if (accessLockerState == AccessLockerState.NOT_INITIALIZED) {
            foregroundImageView.setImageDrawable(notInitializedDrawable);
        } else if (accessLockerState == AccessLockerState.LOCKED) {
            foregroundImageView.setImageDrawable(lockedDrawable);
        } else if (accessLockerState == AccessLockerState.UNLOCKED) {
            foregroundImageView.setImageDrawable(unlockedDrawable);
        }
    }

    //endregion

    //region customizations

    /**
     * Set call back for when the widget is tapped.
     * This can be used to link the widget to {@link AccessLockerControlWidget}
     *
     * @param stateChangeCallback listener to handle call backs
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }

    /**
     * Set the access locker not initialized icon
     *
     * @param resourceId to be used
     */
    public void setAccessLockerNotInitializedIcon(@DrawableRes int resourceId) {
        setAccessLockerNotInitializedIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the access locker not initialized icon
     *
     * @param drawable to be used
     */
    public void setAccessLockerNotInitializedIcon(@Nullable Drawable drawable) {
        notInitializedDrawable = drawable;
    }

    /**
     * Get the access locker not initialized icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAccessLockerNotInitializedIcon() {
        return notInitializedDrawable;
    }

    /**
     * Set the access locker unlocked state icon
     *
     * @param resourceId to be used
     */
    public void setAccessLockerUnlockedIcon(@DrawableRes int resourceId) {
        setAccessLockerUnlockedIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the access locker unlocked state icon
     *
     * @param drawable to be used
     */
    public void setAccessLockerUnlockedIcon(@Nullable Drawable drawable) {
        unlockedDrawable = drawable;
    }

    /**
     * Get the access locker unlocked state icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAccessLockerUnlockedIcon() {
        return unlockedDrawable;
    }

    /**
     * Set the access locker locked state icon
     *
     * @param resourceId to be used
     */
    public void setAccessLockerLockedIcon(@DrawableRes int resourceId) {
        setAccessLockerLockedIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the access locker locked state icon
     *
     * @param drawable to be used
     */
    public void setAccessLockerLockedIcon(@Nullable Drawable drawable) {
        lockedDrawable = drawable;
    }

    /**
     * Get the access locker locked state icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAccessLockerLockedIcon() {
        return lockedDrawable;
    }


    /**
     * Set the access locker icon background
     *
     * @param resourceId to be used
     */
    public void setAccessLockerIconBackground(@DrawableRes int resourceId) {
        foregroundImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the access locker icon background
     *
     * @param drawable to be used
     */
    public void setAccessLockerIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get the access locker icon background
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAccessLockerIconBackground() {
        return foregroundImageView.getBackground();
    }


    //endregion

}
