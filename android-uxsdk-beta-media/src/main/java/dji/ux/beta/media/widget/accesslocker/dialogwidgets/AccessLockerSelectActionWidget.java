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
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.media.R;
import dji.ux.beta.media.widget.accesslocker.AccessLockerControlStateChangeListener;
import dji.ux.beta.media.widget.accesslocker.AccessLockerControlWidget;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Access Locker Select Action Widget
 * <p>
 * Widget is a part of the {@link AccessLockerControlWidget}
 * <p>
 * It provides a user interface to select from following actions
 * 1. Change Password
 * 2. Remove Password
 * 3. Close widget
 */
public class AccessLockerSelectActionWidget extends ConstraintLayoutWidget implements View.OnClickListener {


    //region Fields
    private TextView changePasswordTextView;
    private TextView removePasswordTextView;
    private TextView cancelTextView;
    private TextView headerTextView;
    private TextView messageTextView;
    private AccessLockerControlStateChangeListener accessLockerControlStateChangeListener;
    //endregion

    public AccessLockerSelectActionWidget(Context context) {
        super(context);
    }

    public AccessLockerSelectActionWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessLockerSelectActionWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_dialog_access_locker_select_action_view, this);
        setBackgroundResource(R.drawable.uxsdk_background_dialog_rounded_corners);
        init();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        // Empty
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_access_locker_select_action_ratio);
    }


    private void init() {
        headerTextView = findViewById(R.id.text_view_select_action_header);
        messageTextView = findViewById(R.id.text_view_select_action_message);
        changePasswordTextView = findViewById(R.id.text_view_change_password_item);
        changePasswordTextView.setOnClickListener(this);
        removePasswordTextView = findViewById(R.id.text_view_remove_password_item);
        removePasswordTextView.setOnClickListener(this);
        cancelTextView = findViewById(R.id.text_view_cancel_item);
        cancelTextView.setOnClickListener(this);
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccessLockerSelectActionWidget);

        if (typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerHeaderBackground) != null) {
            setHeaderBackground(typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerHeaderBackground));
        }
        int color = typedArray.getColor(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerHeaderTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setHeaderTextColor(color);
        }
        float textSize =
                typedArray.getDimension(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerHeaderTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setHeaderTextSize(textSize);
        }
        int textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerHeaderTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setHeaderTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerMessageBackground) != null) {
            setMessageBackground(typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerMessageBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerMessageTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setMessageTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerMessageTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setMessageTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerMessageTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setMessageTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerCancelBackground) != null) {
            setCancelButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerCancelBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerCancelTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setCancelButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerCancelTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setCancelButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerCancelTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setCancelButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerRemoveBackground) != null) {
            setRemovePasswordButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerRemoveBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerRemoveTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setRemovePasswordButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerRemoveTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setRemovePasswordButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerRemoveTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setRemovePasswordButtonTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerChangeBackground) != null) {
            setChangePasswordButtonBackground(typedArray.getDrawable(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerChangeBackground));
        }
        color = typedArray.getColor(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerChangeTextColor, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setChangePasswordButtonTextColor(color);
        }
        textSize =
                typedArray.getDimension(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerChangeTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setChangePasswordButtonTextSize(textSize);
        }
        textAppearance =
                typedArray.getResourceId(R.styleable.AccessLockerSelectActionWidget_uxsdk_accessLockerChangeTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setChangePasswordButtonTextAppearance(textAppearance);
        }
        typedArray.recycle();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text_view_change_password_item) {
            if (accessLockerControlStateChangeListener != null) {
                accessLockerControlStateChangeListener.onStateChange(AccessLockerControlStateChangeListener.AccessLockerControlState.CHANGE_PASSWORD);
            }
        } else if (id == R.id.text_view_remove_password_item) {
            if (accessLockerControlStateChangeListener != null) {
                accessLockerControlStateChangeListener.onStateChange(AccessLockerControlStateChangeListener.AccessLockerControlState.REMOVE_PASSWORD);
            }
        } else if (id == R.id.text_view_cancel_item) {
            if (accessLockerControlStateChangeListener != null) {
                accessLockerControlStateChangeListener.onStateChange(AccessLockerControlStateChangeListener.AccessLockerControlState.CANCEL_DIALOG);
            }
        }
    }

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
     * Set the color of the remove password button text
     *
     * @param color integer value representing  color
     */
    public void setRemovePasswordButtonTextColor(@ColorInt int color) {
        removePasswordTextView.setTextColor(color);
    }

    /**
     * Get the color of the remove password button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getRemovePasswordButtonTextColor() {
        return removePasswordTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the remove password button
     *
     * @param textAppearance resourceId for text appearance for remove password button
     */
    public void setRemovePasswordButtonTextAppearance(@StyleRes int textAppearance) {
        removePasswordTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the remove password button text
     *
     * @param textSize float value that represents text size
     */
    public void setRemovePasswordButtonTextSize(@Dimension float textSize) {
        removePasswordTextView.setTextSize(textSize);
    }

    /**
     * Get size of remove password button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getRemovePasswordButtonTextSize() {
        return removePasswordTextView.getTextSize();
    }

    /**
     * Set the background of the remove password button
     *
     * @param resourceId to be used
     */
    public void setRemovePasswordButtonBackground(@DrawableRes int resourceId) {
        removePasswordTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the remove password button
     *
     * @param drawable to be used
     */
    public void setRemovePasswordButtonBackground(@Nullable Drawable drawable) {
        removePasswordTextView.setBackground(drawable);
    }

    /**
     * Get the background of the remove password button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getRemovePasswordButtonBackground() {
        return removePasswordTextView.getBackground();
    }

    /**
     * Set the color of the change password button text
     *
     * @param color integer value representing  color
     */
    public void setChangePasswordButtonTextColor(@ColorInt int color) {
        changePasswordTextView.setTextColor(color);
    }

    /**
     * Get the color of the change password button text
     *
     * @return integer value representing  color
     */
    @ColorInt
    public int getChangePasswordButtonTextColor() {
        return changePasswordTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the change password button
     *
     * @param textAppearance resourceId for text appearance for change password button
     */
    public void setChangePasswordButtonTextAppearance(@StyleRes int textAppearance) {
        changePasswordTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set size of the change password button text
     *
     * @param textSize float value that represents text size
     */
    public void setChangePasswordButtonTextSize(@Dimension float textSize) {
        changePasswordTextView.setTextSize(textSize);
    }

    /**
     * Get size of change password button text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getChangePasswordButtonTextSize() {
        return changePasswordTextView.getTextSize();
    }

    /**
     * Set the background of the change password button
     *
     * @param resourceId to be used
     */
    public void setChangePasswordButtonBackground(@DrawableRes int resourceId) {
        changePasswordTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the change password button
     *
     * @param drawable to be used
     */
    public void setChangePasswordButtonBackground(@Nullable Drawable drawable) {
        changePasswordTextView.setBackground(drawable);
    }

    /**
     * Get the background of the change password button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getChangePasswordButtonBackground() {
        return changePasswordTextView.getBackground();
    }


    //endregion

}