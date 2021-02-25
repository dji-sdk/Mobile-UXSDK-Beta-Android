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
 * Access Locker Change Password Widget
 * <p>
 * Widget is a part of the {@link AccessLockerControlWidget}
 * <p>
 * It provides a user interface to change password used for aircraft.
 */

public class AccessLockerChangePasswordWidget extends ConstraintLayoutWidget implements View.OnClickListener {

    //region Fields
    private static final String TAG = "ALChangePasswordWidget";
    private TextView headerTextView;
    private TextView messageTextView;
    private TextView savePasswordTextView;
    private TextView cancelTextView;
    private EditText enterCurrentPasswordEditText;
    private EditText enterNewPasswordEditText;
    private EditText confirmNewPasswordEditText;
    private AccessLockerChangePasswordWidgetModel widgetModel;
    private AccessLockerControlStateChangeListener accessLockerControlStateChangeListener;
    //endregion


    //region Lifecycle
    public AccessLockerChangePasswordWidget(Context context) {
        super(context);
    }

    public AccessLockerChangePasswordWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessLockerChangePasswordWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_dialog_access_locker_change_password_view, this);
        setBackgroundResource(R.drawable.uxsdk_background_dialog_rounded_corners);
        init();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        if (!isInEditMode()) {
            widgetModel = new AccessLockerChangePasswordWidgetModel(DJISDKModel.getInstance(),
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
    protected void reactToModelChanges() {
        //Empty
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_access_locker_change_password_ratio);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text_view_save_password) {
            changePassword();
        } else if (id == R.id.text_view_cancel_access_locker_dialog) {
            cancelDialog();
        }
    }
    //endregion

    //region private methods

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccessLockerChangePasswordWidget);

        if (typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerHeaderBackground) != null) {
            setHeaderBackground(typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerHeaderBackground));
        }
        int color = typedArray.getColor(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerHeaderTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setHeaderTextColor(color);
        }
        float textSize =
                typedArray.getDimension(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerHeaderTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setHeaderTextSize(textSize);
        }
        int textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerHeaderTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setHeaderTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerMessageBackground) != null) {
            setMessageBackground(typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerMessageBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerMessageTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setMessageTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerMessageTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setMessageTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerMessageTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setMessageTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerCancelBackground) != null) {
            setCancelButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerCancelBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerCancelTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setCancelButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerCancelTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setCancelButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerCancelTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setCancelButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerSaveBackground) != null) {
            setSavePasswordButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerSaveBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerSaveTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setSavePasswordButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerSaveTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setSavePasswordButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerSaveTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setSavePasswordButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerInputBackground) != null) {
            setInputBackground(typedArray.getDrawable(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerInputBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerInputTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setInputTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerInputTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setInputTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerChangePasswordWidget_uxsdk_accessLockerInputTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setInputTextAppearance(textAppearance);
        }
        typedArray.recycle();

    }

    private void init() {
        headerTextView = findViewById(R.id.text_view_select_action_header);
        messageTextView = findViewById(R.id.text_view_select_action_message);
        enterCurrentPasswordEditText = findViewById(R.id.edit_text_enter_current_password);
        enterNewPasswordEditText = findViewById(R.id.edit_text_enter_new_password);
        confirmNewPasswordEditText = findViewById(R.id.edit_text_confirm_new_password);
        savePasswordTextView = findViewById(R.id.text_view_save_password);
        savePasswordTextView.setOnClickListener(this);
        cancelTextView = findViewById(R.id.text_view_cancel_access_locker_dialog);
        cancelTextView.setOnClickListener(this);
    }

    private void changePassword() {
        String enteredCurrentPassword = enterCurrentPasswordEditText.getText().toString();
        String enteredNewPassword = enterNewPasswordEditText.getText().toString();
        String confirmedNewPassword = confirmNewPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(enteredCurrentPassword)) {
            enterCurrentPasswordEditText.requestFocus();
            enterCurrentPasswordEditText.setError(getResources().getString(R.string.uxsdk_empty_field_error));
            return;
        } else if (TextUtils.isEmpty(enteredNewPassword)) {
            enterNewPasswordEditText.requestFocus();
            enterNewPasswordEditText.setError(getResources().getString(R.string.uxsdk_empty_field_error));
            return;
        } else if (TextUtils.isEmpty(confirmedNewPassword)) {
            confirmNewPasswordEditText.requestFocus();
            confirmNewPasswordEditText.setError(getResources().getString(R.string.uxsdk_empty_field_error));
            return;
        }

        if (!confirmedNewPassword.equals(enteredNewPassword)) {
            confirmNewPasswordEditText.requestFocus();
            confirmNewPasswordEditText.setError(getResources().getString(R.string.uxsdk_passwords_do_not_match_error));
            return;
        }

        //If no errors detected do the following
        addDisposable(widgetModel.changePasswordOnDevice(enteredCurrentPassword, enteredNewPassword)
                .subscribe(
                        () -> {
                        },
                        error -> {
                            if (error == null) {
                                Toast.makeText(getContext(), getResources().getString(R.string.uxsdk_password_changed_successfully), Toast.LENGTH_SHORT).show();
                                cancelDialog();
                            } else if (error instanceof UXSDKError) {
                                DJILog.e(TAG, "change password " + error.getLocalizedMessage());
                                Toast.makeText(getContext(), getResources().getString(R.string.uxsdk_change_password_failed), Toast.LENGTH_SHORT).show();
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
     * Set the color of the save password button text
     *
     * @param color integer value representing  color
     */
    public void setSavePasswordButtonTextColor(@ColorInt int color) {
        savePasswordTextView.setTextColor(color);
    }

    /**
     * Get the color of the save password button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getSavePasswordButtonTextColor() {
        return savePasswordTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the save password button
     *
     * @param textAppearance resourceId for text appearance for save password button
     */
    public void setSavePasswordButtonTextAppearance(@StyleRes int textAppearance) {
        savePasswordTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the save password button text
     *
     * @param textSize float value that represents text size
     */
    public void setSavePasswordButtonTextSize(@Dimension float textSize) {
        savePasswordTextView.setTextSize(textSize);
    }

    /**
     * Get size of save password button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getSavePasswordButtonTextSize() {
        return savePasswordTextView.getTextSize();
    }

    /**
     * Set the background of the save password button
     *
     * @param resourceId to be used
     */
    public void setSavePasswordButtonBackground(@DrawableRes int resourceId) {
        savePasswordTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the save password button
     *
     * @param drawable to be used
     */
    public void setSavePasswordButtonBackground(@Nullable Drawable drawable) {
        savePasswordTextView.setBackground(drawable);
    }

    /**
     * Get the background of the save password button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getSavePasswordButtonBackground() {
        return savePasswordTextView.getBackground();
    }


    /**
     * Set text color of input fields
     *
     * @param color integer value representing color
     */
    public void setInputTextColor(@ColorInt int color) {
        enterCurrentPasswordEditText.setTextColor(color);
        enterNewPasswordEditText.setTextColor(color);
        confirmNewPasswordEditText.setTextColor(color);

    }

    /**
     * Set text size of the input fields
     *
     * @param textSize float value
     */
    public void setInputTextSize(@Dimension float textSize) {
        enterCurrentPasswordEditText.setTextSize(textSize);
        enterNewPasswordEditText.setTextSize(textSize);
        confirmNewPasswordEditText.setTextSize(textSize);

    }

    /**
     * Get current text size of input fields
     *
     * @return float value representing text size
     */
    @Dimension
    public float getInputTextSize() {
        return enterCurrentPasswordEditText.getTextSize();
    }

    /**
     * Get text color of input fields
     *
     * @return integer value representing the color of text
     */
    @ColorInt
    public int getInputTextColor() {
        return enterCurrentPasswordEditText.getCurrentTextColor();
    }

    /**
     * Set text appearance of the input fields
     *
     * @param textAppearance resourceId for text appearance of input fields
     */
    public void setInputTextAppearance(@StyleRes int textAppearance) {
        enterCurrentPasswordEditText.setTextAppearance(getContext(), textAppearance);
        enterNewPasswordEditText.setTextAppearance(getContext(), textAppearance);
        confirmNewPasswordEditText.setTextAppearance(getContext(), textAppearance);

    }

    /**
     * Set background to input fields
     *
     * @param resourceId to be used
     */
    public void setInputBackground(@DrawableRes int resourceId) {
        setInputBackground(getResources().getDrawable(resourceId));
    }


    /**
     * Set background to input fields
     *
     * @param drawable to be used
     */
    public void setInputBackground(@Nullable Drawable drawable) {
        enterCurrentPasswordEditText.setBackground(drawable);
        enterNewPasswordEditText.setBackground(drawable);
        confirmNewPasswordEditText.setBackground(drawable);
    }


    /**
     * Get background of input fields
     *
     * @return Drawable representing background of input fields
     */
    @Nullable
    public Drawable getInputBackground() {
        return enterCurrentPasswordEditText.getBackground();
    }

    //endregion
}
