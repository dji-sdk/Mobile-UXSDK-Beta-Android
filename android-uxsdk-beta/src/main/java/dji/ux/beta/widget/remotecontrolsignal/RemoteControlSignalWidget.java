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

package dji.ux.beta.widget.remotecontrolsignal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;

/**
 * This widget shows the strength of the signal between the RC and the aircraft.
 */
public class RemoteControlSignalWidget extends ConstraintLayoutWidget {

    //region Fields
    private ImageView rcIconImageView;
    private ImageView rcSignalImageView;
    @ColorInt
    private int connectionStateIconColor;
    private RemoteControlSignalWidgetModel widgetModel;
    //endregion

    //region Constructors
    public RemoteControlSignalWidget(@NonNull Context context) {
        super(context);
    }

    public RemoteControlSignalWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteControlSignalWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_remote_control_signal, this);
        rcIconImageView = findViewById(R.id.imageview_rc_icon);
        rcSignalImageView = findViewById(R.id.imageview_rc_signal);
        connectionStateIconColor = getResources().getColor(R.color.uxsdk_red_material_A700_67_percent);

        if (!isInEditMode()) {
            widgetModel = new RemoteControlSignalWidgetModel(DJISDKModel.getInstance(),
                                                             ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
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
        addReaction(widgetModel.getRCSignalQuality()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateIcon));
        addReaction(widgetModel.getProductConnection()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateIcon));
    }
    //endregion

    //region Reactions to model
    private void updateIcon(@IntRange(from = 0, to = 100) int rcSignalQuality) {
        rcSignalImageView.setImageLevel(rcSignalQuality);
    }

    private void updateIcon(boolean isConnected) {
        if (isConnected) {
            rcIconImageView.clearColorFilter();
        } else {
            rcIconImageView.setColorFilter(connectionStateIconColor);
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_remote_control_signal_ratio);
    }

    /**
     * Set the resource ID for the remote control icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setRemoteControlIcon(@DrawableRes int resourceId) {
        rcIconImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the remote control icon
     *
     * @param icon Drawable resource for the image
     */
    public void setRemoteControlIcon(@Nullable Drawable icon) {
        rcIconImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the remote control icon
     *
     * @return Drawable for the remote control icon
     */
    public Drawable getRemoteControlIcon() {
        return rcIconImageView.getDrawable();
    }

    /**
     * Set the resource ID for the remote control icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setRemoteControlIconBackground(@DrawableRes int resourceId) {
        rcIconImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the remote control icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setRemoteControlIconBackground(@Nullable Drawable background) {
        rcIconImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the remote control icon
     *
     * @return Drawable for the remote control icon's background
     */
    public Drawable getRemoteControlIconBackground() {
        return rcIconImageView.getBackground();
    }

    /**
     * Set the resource ID for the RC signal icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setRCSignalIcon(@DrawableRes int resourceId) {
        rcSignalImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the RC signal icon
     *
     * @param icon Drawable resource for the image
     */
    public void setRCSignalIcon(@Nullable Drawable icon) {
        rcSignalImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the RC signal icon
     *
     * @return Drawable for the RC signal icon
     */
    public Drawable getRCSignalIcon() {
        return rcSignalImageView.getDrawable();
    }

    /**
     * Set the resource ID for the RC signal icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setRCSignalIconBackground(@DrawableRes int resourceId) {
        rcSignalImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the RC signal icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setRCSignalIconBackground(@Nullable Drawable background) {
        rcSignalImageView.setBackground(background);
    }

    /**
     * Get the background drawable for the RC signal icon
     *
     * @return Drawable for the RC signal icon's background
     */
    public Drawable getRCSignalIconBackground() {
        return rcSignalImageView.getBackground();
    }

    /**
     * Set the color for the RC icon in the disconnected state
     *
     * @param color Color integer resource
     */
    public void setConnectionStateIconColor(@ColorInt int color) {
        connectionStateIconColor = color;
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RemoteControlSignalWidget);

        Drawable rcIcon = typedArray.getDrawable(R.styleable.RemoteControlSignalWidget_uxsdk_rcIcon);
        if (rcIcon != null) {
            setRemoteControlIcon(rcIcon);
        }

        Drawable rcSignalIcon = typedArray.getDrawable(R.styleable.RemoteControlSignalWidget_uxsdk_rcSignalIcon);
        if (rcSignalIcon != null) {
            setRCSignalIcon(rcSignalIcon);
        }

        connectionStateIconColor =
            typedArray.getColor(R.styleable.RemoteControlSignalWidget_uxsdk_connectionStateIconColor,
                                getResources().getColor(R.color.uxsdk_red_material_A700_67_percent));

        typedArray.recycle();
    }
    //endregion
}
