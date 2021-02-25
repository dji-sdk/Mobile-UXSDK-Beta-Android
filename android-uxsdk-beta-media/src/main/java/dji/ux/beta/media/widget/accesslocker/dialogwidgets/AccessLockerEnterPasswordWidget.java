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

package dji.ux.beta.media.widget.accesslocker.dialogwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.log.DJILog;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.media.R;
import dji.ux.beta.media.widget.accesslocker.AccessLockerControlStateChangeListener;
import dji.ux.beta.media.widget.accesslocker.AccessLockerControlWidget;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Access Locker Enter Password Widget
 * <p>
 * Widget is a part of the {@link AccessLockerControlWidget}
 * <p>
 * It provides a user interface to enter password to unlock aircraft.
 */
public class AccessLockerEnterPasswordWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region Fields
    private static final String TAG = "ALEnterPasswordWidget";
    private EditText enterAircraftPasswordEditText;
    private AccessLockerEnterPasswordWidgetModel widgetModel;
    private AccessLockerControlStateChangeListener accessLockerControlStateChangeListener;
    private TextView formatAircraftTextView;
    private TextView unlockAircraftTextView;
    private TextView cancelTextView;
    private TextView headerTextView;
    private TextView messageTextView;

    //endregion

    //region Lifecycle
    public AccessLockerEnterPasswordWidget(Context context) {
        super(context);
    }

    public AccessLockerEnterPasswordWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessLockerEnterPasswordWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_dialog_access_locker_enter_password_view, this);
        setBackgroundResource(R.drawable.uxsdk_background_dialog_rounded_corners);
        init();
        if (attrs != null) {
            initAttributes(context, attrs);
        }

        if (!isInEditMode()) {
            widgetModel = new AccessLockerEnterPasswordWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }
    }

    @Override
    protected void reactToModelChanges() {
        //empty method
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_access_locker_enter_password_ratio);
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccessLockerEnterPasswordWidget);

        if (typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerHeaderBackground) != null) {
            setHeaderBackground(typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerHeaderBackground));
        }
        int color = typedArray.getColor(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerHeaderTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setHeaderTextColor(color);
        }
        float textSize =
                typedArray.getDimension(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerHeaderTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setHeaderTextSize(textSize);
        }
        int textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerHeaderTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setHeaderTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerMessageBackground) != null) {
            setMessageBackground(typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerMessageBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerMessageTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setMessageTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerMessageTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setMessageTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerMessageTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setMessageTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerCancelBackground) != null) {
            setCancelButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerCancelBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerCancelTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setCancelButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerCancelTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setCancelButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerCancelTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setCancelButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerSetPasswordWidget_uxsdk_accessLockerInputBackground) != null) {
            setInputBackground(typedArray.getDrawable(R.styleable.AccessLockerSetPasswordWidget_uxsdk_accessLockerInputBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerSetPasswordWidget_uxsdk_accessLockerInputTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setInputTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerSetPasswordWidget_uxsdk_accessLockerInputTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setInputTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSetPasswordWidget_uxsdk_accessLockerInputTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setInputTextAppearance(textAppearance);
        }


        if (typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerFormatBackground) != null) {
            setEnterPasswordButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerFormatBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerFormatTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setEnterPasswordButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerFormatTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setEnterPasswordButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerEnterPasswordWidget_uxsdk_accessLockerFormatTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setEnterPasswordButtonTextAppearance(textAppearance);
        }
        typedArray.recycle();
    }

    private void init() {
        headerTextView = findViewById(R.id.unlock_aircraft_header);
        messageTextView = findViewById(R.id.unlock_aircraft_warning);
        enterAircraftPasswordEditText = findViewById(R.id.enter_aircraft_password);
        unlockAircraftTextView = findViewById(R.id.unlock_aircraft);
        unlockAircraftTextView.setOnClickListener(this);
        formatAircraftTextView = findViewById(R.id.text_view_format_aircraft);
        formatAircraftTextView.setOnClickListener(this);
        cancelTextView = findViewById(R.id.text_view_cancel_access_locker_dialog);
        cancelTextView.setOnClickListener(this);
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
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.unlock_aircraft) {
            verifyPassword();
        } else if (id == R.id.text_view_format_aircraft) {
            if (accessLockerControlStateChangeListener != null) {
                accessLockerControlStateChangeListener.onStateChange(AccessLockerControlStateChangeListener.AccessLockerControlState.FORMAT_AIRCRAFT);
            }
        } else if (id == R.id.text_view_cancel_access_locker_dialog) {
            cancelDialog();
        }
    }
    //endregion

    //region private methods
    private void verifyPassword() {
        String enteredPassword = enterAircraftPasswordEditText.getText().toString();
        if (TextUtils.isEmpty(enteredPassword)) {
            enterAircraftPasswordEditText.setError(getResources().getString(R.string.uxsdk_empty_field_error));
            return;
        }

        addDisposable(widgetModel.verifyPassword(enteredPassword)
                .subscribe(
                        () -> {
                        },
                        error -> {
                            if (error == null) {
                                Toast.makeText(getContext(), getResources().getString(R.string.uxsdk_aircraft_unlocked_successfully), Toast.LENGTH_SHORT).show();
                                cancelDialog();
                            } else if (error instanceof UXSDKError) {
                                DJILog.e(TAG, "verify password " + error.getLocalizedMessage());
                                enterAircraftPasswordEditText.setError(((UXSDKError) error).getDJIError().getDescription());

                            }
                        }
                ));
    }

    private void cancelDialog() {
        if (accessLockerControlStateChangeListener != null) {
            accessLockerControlStateChangeListener.onStateChange(AccessLockerControlStateChangeListener.AccessLockerControlState.CANCEL_DIALOG);
        }
    }
    //endregion

    //region public methods

    /**
     * Set the access locker control state change listener
     *
     * @param accessLockerControlStateChangeListener instance of listener
     */
    public void setAccessLockerControlStateChangeListener(@Nullable AccessLockerControlStateChangeListener accessLockerControlStateChangeListener) {
        this.accessLockerControlStateChangeListener = accessLockerControlStateChangeListener;
    }

    /**
     * Set the color of the header text
     *
     * @param color integer value representing text color
     */
    public void setHeaderTextColor(@ColorInt int color) {
        headerTextView.setTextColor(color);
    }

    /**
     * Get the color of the header text
     *
     * @return integer value representing text color of header
     */
    @ColorInt
    public int getHeaderTextColor() {
        return headerTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the header
     *
     * @param textAppearance resourceId for text appearance for header
     */
    public void setHeaderTextAppearance(@StyleRes int textAppearance) {
        headerTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the header text
     *
     * @param textSize float value representing text size
     */
    public void setHeaderTextSize(@Dimension float textSize) {
        headerTextView.setTextSize(textSize);
    }

    /**
     * Get size of the header text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getHeaderTextSize() {
        return headerTextView.getTextSize();
    }

    /**
     * Set the background of the header
     *
     * @param resourceId to be used
     */
    public void setHeaderBackground(@DrawableRes int resourceId) {
        headerTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the header
     *
     * @param drawable to be used
     */
    public void setHeaderBackground(@Nullable Drawable drawable) {
        headerTextView.setBackground(drawable);
    }

    /**
     * Get the background of the header
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getHeaderBackground() {
        return headerTextView.getBackground();
    }

    /**
     * Set the color of the message text
     *
     * @param color integer value representing  color
     */
    public void setMessageTextColor(@ColorInt int color) {
        messageTextView.setTextColor(color);
    }

    /**
     * Get the color of the message text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getMessageTextColor() {
        return messageTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the message
     *
     * @param textAppearance resourceId for text appearance for message
     */
    public void setMessageTextAppearance(@StyleRes int textAppearance) {
        messageTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the message text
     *
     * @param textSize float value that represents text size
     */
    public void setMessageTextSize(@Dimension float textSize) {
        messageTextView.setTextSize(textSize);
    }

    /**
     * Get size of message text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getMessageTextSize() {
        return messageTextView.getTextSize();
    }

    /**
     * Set the background of the message
     *
     * @param resourceId to be used
     */
    public void setMessageBackground(@DrawableRes int resourceId) {
        messageTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the message
     *
     * @param drawable to be used
     */
    public void setMessageBackground(@Nullable Drawable drawable) {
        messageTextView.setBackground(drawable);
    }

    /**
     * Get the background of the message
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getMessageBackground() {
        return messageTextView.getBackground();
    }

    /**
     * Set the color of the format aircraft button text
     *
     * @param color integer value representing  color
     */
    public void setEnterPasswordButtonTextColor(@ColorInt int color) {
        formatAircraftTextView.setTextColor(color);
    }

    /**
     * Get the color of the format aircraft button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getEnterPasswordButtonTextColor() {
        return formatAircraftTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the format aircraft button
     *
     * @param textAppearance resourceId for text appearance for format aircraft button
     */
    public void setEnterPasswordButtonTextAppearance(@StyleRes int textAppearance) {
        formatAircraftTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the format aircraft button text
     *
     * @param textSize float value that represents text size
     */
    public void setEnterPasswordButtonTextSize(@Dimension float textSize) {
        formatAircraftTextView.setTextSize(textSize);
    }

    /**
     * Get size of format aircraft button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getEnterPasswordButtonTextSize() {
        return formatAircraftTextView.getTextSize();
    }

    /**
     * Set the background of the format aircraft button
     *
     * @param resourceId to be used
     */
    public void setEnterPasswordButtonBackground(@DrawableRes int resourceId) {
        formatAircraftTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the format aircraft button
     *
     * @param drawable to be used
     */
    public void setEnterPasswordButtonBackground(@Nullable Drawable drawable) {
        formatAircraftTextView.setBackground(drawable);
    }

    /**
     * Get the background of the format aircraft button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getEnterPasswordButtonBackground() {
        return formatAircraftTextView.getBackground();
    }

    /**
     * Set the color of the cancel button text
     *
     * @param color integer value representing  color
     */
    public void setCancelButtonTextColor(@ColorInt int color) {
        cancelTextView.setTextColor(color);
    }

    /**
     * Get the color of the cancel button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getCancelButtonTextColor() {
        return cancelTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the cancel button
     *
     * @param textAppearance resourceId for text appearance for cancel button
     */
    public void setCancelButtonTextAppearance(@StyleRes int textAppearance) {
        cancelTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the cancel button text
     *
     * @param textSize float value that represents text size
     */
    public void setCancelButtonTextSize(@Dimension float textSize) {
        cancelTextView.setTextSize(textSize);
    }

    /**
     * Get size of cancel button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getCancelButtonTextSize() {
        return cancelTextView.getTextSize();
    }

    /**
     * Set the background of the cancel button
     *
     * @param resourceId to be used
     */
    public void setCancelButtonBackground(@DrawableRes int resourceId) {
        cancelTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the cancel button
     *
     * @param drawable to be used
     */
    public void setCancelButtonBackground(@Nullable Drawable drawable) {
        cancelTextView.setBackground(drawable);
    }

    /**
     * Get the background of the cancel button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getCancelButtonBackground() {
        return cancelTextView.getBackground();
    }


    /**
     * Set the color of the unlock aircraft button text
     *
     * @param color integer value representing  color
     */
    public void setUnlockAircraftButtonTextColor(@ColorInt int color) {
        unlockAircraftTextView.setTextColor(color);
    }

    /**
     * Get the color of the unlock aircraft button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getUnlockAircraftButtonTextColor() {
        return unlockAircraftTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the unlock aircraft button
     *
     * @param textAppearance resourceId for text appearance for unlock aircraft button
     */
    public void setUnlockAircraftButtonTextAppearance(@StyleRes int textAppearance) {
        unlockAircraftTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the unlock aircraft button text
     *
     * @param textSize float value that represents text size
     */
    public void setUnlockAircraftButtonTextSize(@Dimension float textSize) {
        unlockAircraftTextView.setTextSize(textSize);
    }

    /**
     * Get size of unlock aircraft button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getUnlockAircraftButtonTextSize() {
        return unlockAircraftTextView.getTextSize();
    }

    /**
     * Set the background of the unlock aircraft button
     *
     * @param resourceId to be used
     */
    public void setUnlockAircraftButtonBackground(@DrawableRes int resourceId) {
        unlockAircraftTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the unlock aircraft button
     *
     * @param drawable to be used
     */
    public void setUnlockAircraftButtonBackground(@Nullable Drawable drawable) {
        unlockAircraftTextView.setBackground(drawable);
    }

    /**
     * Get the background of the unlock aircraft button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getUnlockAircraftButtonBackground() {
        return unlockAircraftTextView.getBackground();
    }

    /**
     * Set text color of input field
     *
     * @param color integer value representing color
     */
    public void setInputTextColor(@ColorInt int color) {
        enterAircraftPasswordEditText.setTextColor(color);

    }

    /**
     * Set text size of the input field
     *
     * @param textSize float value
     */
    public void setInputTextSize(@Dimension float textSize) {
        enterAircraftPasswordEditText.setTextSize(textSize);

    }

    /**
     * Get current text size of input field
     *
     * @return float value representing text size
     */
    @Dimension
    public float getInputTextSize() {
        return enterAircraftPasswordEditText.getTextSize();
    }

    /**
     * Get text color of input field
     *
     * @return integer value representing the color of text
     */
    @ColorInt
    public int getInputTextColor() {
        return enterAircraftPasswordEditText.getCurrentTextColor();
    }

    /**
     * Set text appearance of the input field
     *
     * @param textAppearance resourceId for text appearance of input field
     */
    public void setInputTextAppearance(@StyleRes int textAppearance) {
        enterAircraftPasswordEditText.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background to input field
     *
     * @param resourceId to be used
     */
    public void setInputBackground(@DrawableRes int resourceId) {
        setInputBackground(getResources().getDrawable(resourceId));
    }


    /**
     * Set background to input field
     *
     * @param drawable to be used
     */
    public void setInputBackground(@Nullable Drawable drawable) {
        enterAircraftPasswordEditText.setBackground(drawable);
    }


    /**
     * Get background of input field
     *
     * @return Drawable representing background of input field
     */
    @Nullable
    public Drawable getInputBackground() {
        return enterAircraftPasswordEditText.getBackground();
    }

    //endregion
}
