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
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.flightcontroller.accesslocker.AccessLockerState;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;
import dji.ux.beta.media.R;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerChangePasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerEnterPasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerFormatAircraftWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerRemovePasswordWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerSelectActionWidget;
import dji.ux.beta.media.widget.accesslocker.dialogwidgets.AccessLockerSetPasswordWidget;

/**
 * Access Locker Control Widget
 * <p>
 * The widget lets you interact with the access locker feature of the drone.
 * Currently supported by Mavic 2 Enterprise series.
 * <p>
 * <p>
 * It incorporates {@link AccessLockerSetPasswordWidget}, {@link AccessLockerSelectActionWidget},
 * {@link AccessLockerChangePasswordWidget}, {@link AccessLockerEnterPasswordWidget},
 * {@link AccessLockerRemovePasswordWidget} and {@link AccessLockerFormatAircraftWidget}.
 * <p>
 * Based on the {@link AccessLockerState} the child widgets will be shown and hidden.
 */
public class AccessLockerControlWidget extends ConstraintLayoutWidget implements AccessLockerControlStateChangeListener, OnStateChangeCallback {

    //region Fields
    private static final String TAG = "AccessLockerCtlWidget";
    private AccessLockerControlWidgetModel widgetModel;
    private AccessLockerSetPasswordWidget accessLockerSetPasswordWidget;
    private AccessLockerRemovePasswordWidget accessLockerRemovePasswordWidget;
    private AccessLockerEnterPasswordWidget accessLockerEnterPasswordWidget;
    private AccessLockerSelectActionWidget accessLockerSelectActionWidget;
    private AccessLockerFormatAircraftWidget accessLockerFormatAircraftWidget;
    private AccessLockerChangePasswordWidget accessLockerChangePasswordWidget;
    //endregion


    //region Lifecycle
    public AccessLockerControlWidget(Context context) {
        super(context);
    }

    public AccessLockerControlWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessLockerControlWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_access_locker_control, this);
        accessLockerSelectActionWidget = findViewById(R.id.widget_action_select);
        accessLockerSetPasswordWidget = findViewById(R.id.widget_set_password);
        accessLockerRemovePasswordWidget = findViewById(R.id.widget_remove_password);
        accessLockerChangePasswordWidget = findViewById(R.id.widget_change_password);
        accessLockerEnterPasswordWidget = findViewById(R.id.widget_enter_password);
        accessLockerFormatAircraftWidget = findViewById(R.id.widget_format_aircraft);

        accessLockerSelectActionWidget.setAccessLockerControlStateChangeListener(this);
        accessLockerSetPasswordWidget.setAccessLockerControlStateChangeListener(this);
        accessLockerRemovePasswordWidget.setAccessLockerControlStateChangeListener(this);
        accessLockerChangePasswordWidget.setAccessLockerControlStateChangeListener(this);
        accessLockerEnterPasswordWidget.setAccessLockerControlStateChangeListener(this);
        accessLockerFormatAircraftWidget.setAccessLockerControlStateChangeListener(this);

        if (!isInEditMode()) {
            widgetModel = new AccessLockerControlWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
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
    public void onStateChange(@NonNull AccessLockerControlState controlState) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(INVISIBLE);
        }
        switch (controlState) {
            case CANCEL_DIALOG:
                setVisibility(GONE);
                break;
            case SELECT_ACTION:
                accessLockerSelectActionWidget.setVisibility(VISIBLE);
                break;
            case SET_PASSWORD:
                accessLockerSetPasswordWidget.setVisibility(VISIBLE);
                break;
            case REMOVE_PASSWORD:
                accessLockerRemovePasswordWidget.setVisibility(VISIBLE);
                break;
            case CHANGE_PASSWORD:
                accessLockerChangePasswordWidget.setVisibility(VISIBLE);
                break;
            case ENTER_PASSWORD:
                accessLockerEnterPasswordWidget.setVisibility(VISIBLE);
                break;
            case FORMAT_AIRCRAFT:
                accessLockerFormatAircraftWidget.setVisibility(VISIBLE);
                break;

        }
        invalidate();

    }

    @Override
    protected void reactToModelChanges() {
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

    private void onAccessLockerStateChange(AccessLockerState accessLockerState) {
        if (accessLockerState == AccessLockerState.NOT_INITIALIZED) {
            onStateChange(AccessLockerControlState.SET_PASSWORD);
        } else if (accessLockerState == AccessLockerState.LOCKED) {
            onStateChange(AccessLockerControlState.ENTER_PASSWORD);
        } else if (accessLockerState == AccessLockerState.UNLOCKED) {
            onStateChange(AccessLockerControlState.SELECT_ACTION);
        }
    }

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getAccessLockerState().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::onAccessLockerStateChange,
                            logErrorConsumer(TAG, "get access locker state")));
        }

    }

    @Override
    public void onStateChange(@Nullable Object state) {
        if (getVisibility() == VISIBLE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility == VISIBLE) {
            checkAndUpdateUI();
        }
    }

    //endregion

    //region customizations

    /**
     * Get access locker select action widget
     *
     * @return instance of {@link AccessLockerSelectActionWidget}
     */
    @NonNull
    public AccessLockerSelectActionWidget getAccessLockerSelectActionWidget() {
        return accessLockerSelectActionWidget;
    }

    /**
     * Get the access locker set password widget
     *
     * @return instance of {@link AccessLockerSetPasswordWidget}
     */
    @NonNull
    public AccessLockerSetPasswordWidget getAccessLockerSetPasswordWidget() {
        return accessLockerSetPasswordWidget;
    }

    /**
     * Get access locker enter password widget
     *
     * @return instance of {@link AccessLockerEnterPasswordWidget}
     */
    @NonNull
    public AccessLockerEnterPasswordWidget getAccessLockerEnterPasswordWidget() {
        return accessLockerEnterPasswordWidget;
    }

    /**
     * Get the access locker change password widget
     *
     * @return instance of {@link AccessLockerChangePasswordWidget}
     */
    @NonNull
    public AccessLockerChangePasswordWidget getAccessLockerChangePasswordWidget() {
        return accessLockerChangePasswordWidget;
    }

    /**
     * Get the access locker remove password widget
     *
     * @return instance of {@link AccessLockerRemovePasswordWidget}
     */
    @NonNull
    public AccessLockerRemovePasswordWidget getAccessLockerRemovePasswordWidget() {
        return accessLockerRemovePasswordWidget;
    }

    /**
     * Get the access locker format aircraft widget
     *
     * @return instance of {@link AccessLockerFormatAircraftWidget}
     */
    @NonNull
    public AccessLockerFormatAircraftWidget getAccessLockerFormatAircraftWidget() {
        return accessLockerFormatAircraftWidget;
    }


    //endregion
}
