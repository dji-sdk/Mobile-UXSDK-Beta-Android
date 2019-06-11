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

package dji.ux.beta.widget.vision;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;

import java.util.HashMap;
import java.util.Map;

import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.FrameLayoutWidget;
import dji.ux.beta.widget.vision.VisionWidgetModel.AvoidanceSensorStatus;
import dji.ux.beta.widget.vision.VisionWidgetModel.VisionSystemStatus;

/**
 * Shows the current state of the vision system. There are two different vision systems that are
 * used by different aircraft. Older aircraft use the {@link VisionSystemStatus} which has three
 * states that indicate whether the system is enabled and working correctly. Newer aircraft such
 * as the Mavic 2 and Mavic 2 Enterprise have an omnidirectional vision system, which means they
 * use the {@link AvoidanceSensorStatus} to indicate which directions are enabled and working
 * correctly.
 */
public class VisionWidget extends FrameLayoutWidget {

    //region Fields
    private ImageView visionIconImageView;
    private VisionWidgetModel widgetModel;
    private Map<VisionSystemStatus, Drawable> visionMap;
    private Map<AvoidanceSensorStatus, Drawable> avoidanceMap;
    //endregion

    //region Constructors
    public VisionWidget(@NonNull Context context) {
        super(context);
    }

    public VisionWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VisionWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_vision, this);

        visionIconImageView = findViewById(R.id.imageview_vision_icon);

        if (!isInEditMode()) {
            widgetModel = new VisionWidgetModel(DJISDKModel.getInstance(),
                                                ObservableInMemoryKeyedStore.getInstance(),
                                                DJISDKManager.getInstance().getMissionControl());
        }

        initDefaultIcons();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

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
        addReaction(widgetModel.getVisionSystemStatus()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateIcon));
        addReaction(widgetModel.getAvoidanceSensorStatus()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateIcon));
    }
    //endregion

    //region Reactions
    private void updateIcon(VisionWidgetModel.VisionSystemStatus visionSystemStatus) {
        visionIconImageView.setImageDrawable(visionMap.get(visionSystemStatus));
    }

    private void updateIcon(VisionWidgetModel.AvoidanceSensorStatus avoidanceSensorStatus) {
        visionIconImageView.setImageDrawable(avoidanceMap.get(avoidanceSensorStatus));
    }
    //endregion

    //region Helpers
    private void initDefaultIcons() {
        visionMap = new HashMap<>();
        visionMap.put(VisionSystemStatus.NORMAL,
                      ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_topbar_visual_normal));
        visionMap.put(VisionSystemStatus.CLOSED,
                      ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_topbar_visual_closed));
        visionMap.put(VisionSystemStatus.DISABLED,
                      ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_topbar_visual_error));

        avoidanceMap = new HashMap<>();
        avoidanceMap.put(AvoidanceSensorStatus.ALL,
                         ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_avoid_normal_all));
        avoidanceMap.put(AvoidanceSensorStatus.FRONT_BACK,
                         ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_avoid_normal_front_back));
        avoidanceMap.put(AvoidanceSensorStatus.DISABLED,
                         ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_avoid_disable_all));
        avoidanceMap.put(AvoidanceSensorStatus.CLOSED,
                         ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_avoid_disable_all));
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    /**
     * Sets the icon to the given image when the {@link VisionSystemStatus} is the given value.
     *
     * @param status The status at which the icon will change to the given image.
     * @param resourceId The id of the image the icon will change to.
     */
    public void setVisionIcon(@NonNull VisionSystemStatus status, @DrawableRes int resourceId) {
        setVisionIcon(status, ContextCompat.getDrawable(getContext(), resourceId));
    }

    /**
     * Sets the icon to the given image when the {@link VisionSystemStatus} is the given value.
     *
     * @param status The status at which the icon will change to the given image.
     * @param drawable The image the icon will change to.
     */
    public void setVisionIcon(@NonNull VisionSystemStatus status, @Nullable Drawable drawable) {
        visionMap.put(status, drawable);
    }

    /**
     * Gets the image that the icon will change to when the {@link VisionSystemStatus} is the
     * given value.
     *
     * @param status The status at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    @Nullable
    public Drawable getVisionIcon(@NonNull VisionSystemStatus status) {
        return visionMap.get(status);
    }

    /**
     * Sets the icon to the given image when the {@link AvoidanceSensorStatus} is the given value.
     *
     * @param status The status at which the icon will change to the given image.
     * @param resourceId The id of the image the icon will change to.
     */
    public void setAvoidanceIcon(@NonNull AvoidanceSensorStatus status, @DrawableRes int resourceId) {
        setAvoidanceIcon(status, ContextCompat.getDrawable(getContext(), resourceId));
    }

    /**
     * Sets the icon to the given image when the {@link AvoidanceSensorStatus} is the given value.
     *
     * @param status The status at which the icon will change to the given image.
     * @param drawable The image the icon will change to.
     */
    public void setAvoidanceIcon(@NonNull AvoidanceSensorStatus status, @Nullable Drawable drawable) {
        avoidanceMap.put(status, drawable);
    }

    /**
     * Gets the image that the icon will change to when the {@link AvoidanceSensorStatus} is the
     * given value.
     *
     * @param status The status at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    @Nullable
    public Drawable getAvoidanceIcon(@NonNull AvoidanceSensorStatus status) {
        return avoidanceMap.get(status);
    }

    /**
     * Set the resource ID for the vision icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        visionIconImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the vision icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setIconBackground(@Nullable Drawable background) {
        visionIconImageView.setBackground(background);
    }

    /**
     * Get the background drawable resource for the vision icon
     *
     * @return Drawable for the icon's background
     */
    @Nullable
    public Drawable getIconBackground() {
        return visionIconImageView.getBackground();
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VisionWidget);

        Drawable normalVisionIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_normalVisionIcon);
        if (normalVisionIcon != null) {
            setVisionIcon(VisionSystemStatus.NORMAL, normalVisionIcon);
        }

        Drawable closedVisionIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_closedVisionIcon);
        if (closedVisionIcon != null) {
            setVisionIcon(VisionSystemStatus.CLOSED, closedVisionIcon);
        }

        Drawable disabledVisionIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_disabledVisionIcon);
        if (disabledVisionIcon != null) {
            setVisionIcon(VisionSystemStatus.DISABLED, disabledVisionIcon);
        }

        Drawable allAvoidanceIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_allAvoidanceIcon);
        if (allAvoidanceIcon != null) {
            setAvoidanceIcon(AvoidanceSensorStatus.ALL, allAvoidanceIcon);
        }

        Drawable frontBackAvoidanceIcon =
            typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_frontBackAvoidanceIcon);
        if (frontBackAvoidanceIcon != null) {
            setAvoidanceIcon(AvoidanceSensorStatus.FRONT_BACK, frontBackAvoidanceIcon);
        }

        Drawable closedAvoidanceIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_closedAvoidanceIcon);
        if (closedAvoidanceIcon != null) {
            setAvoidanceIcon(AvoidanceSensorStatus.CLOSED, closedAvoidanceIcon);
        }

        Drawable disabledAvoidanceIcon = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_disabledAvoidanceIcon);
        if (disabledAvoidanceIcon != null) {
            setAvoidanceIcon(AvoidanceSensorStatus.DISABLED, disabledAvoidanceIcon);
        }

        Drawable visionIconBackground = typedArray.getDrawable(R.styleable.VisionWidget_uxsdk_visionIconBackground);
        if (visionIconBackground != null) {
            setIconBackground(visionIconBackground);
        }

        typedArray.recycle();
    }
    //endregion
}
