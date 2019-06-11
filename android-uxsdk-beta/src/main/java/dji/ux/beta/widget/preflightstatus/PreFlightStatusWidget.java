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

package dji.ux.beta.widget.preflightstatus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import dji.common.logics.warningstatuslogic.WarningStatusItem;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DisplayUtil;

/**
 * This widget shows the pre-flight status of the aircraft.
 *
 * The WarningStatusItem received by this widget contains the message to be
 * displayed, the warning level and the urgency of the message.
 *
 * The color of the background changes depending on the severity of the
 * status as determined by the WarningLevel. The UI also reacts
 * to the urgency of the message by causing the background to blink.
 */
public class PreFlightStatusWidget extends ConstraintLayoutWidget {
    //region Fields
    private TextView preFlightStatusTextView;
    private ImageView preFlightStatusBackgroundImageView;
    private Animation blinkAnimation;
    private PreFlightStatusWidgetModel widgetModel;
    private GradientDrawable backgroundError;
    private GradientDrawable backgroundWarning;
    private GradientDrawable backgroundGood;
    private GradientDrawable backgroundOffline;
    //endregion

    //region Constructors
    public PreFlightStatusWidget(Context context) {
        super(context);
    }

    public PreFlightStatusWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreFlightStatusWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_preflight_status, this);
        preFlightStatusTextView = findViewById(R.id.textview_preflight_status);
        preFlightStatusTextView.setSelected(true); //Required for horizontal scrolling in textView

        preFlightStatusBackgroundImageView = findViewById(R.id.imageview_preflight_status_background);
        blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink);

        backgroundError = new GradientDrawable();
        initGradientDrawable(backgroundError, new int[] {
            getResources().getColor(R.color.uxsdk_red_material_700),
            getResources().getColor(R.color.uxsdk_red_material_700_transparent)
        });

        backgroundWarning = new GradientDrawable();
        initGradientDrawable(backgroundWarning, new int[] {
            getResources().getColor(R.color.uxsdk_yellow), getResources().getColor(R.color.uxsdk_yellow_transparent)
        });

        backgroundGood = new GradientDrawable();
        initGradientDrawable(backgroundGood, new int[] {
            getResources().getColor(R.color.uxsdk_green_material_400),
            getResources().getColor(R.color.uxsdk_green_material_400_transparent)
        });

        backgroundOffline = new GradientDrawable();
        initGradientDrawable(backgroundOffline, new int[] {
            getResources().getColor(R.color.uxsdk_light_gray_800),
            getResources().getColor(R.color.uxsdk_light_gray_800_transparent)
        });

        if (!isInEditMode()) {
            widgetModel =
                new PreFlightStatusWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    private void initGradientDrawable(GradientDrawable gradientDrawable, int[] colors) {
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradientDrawable.setColors(colors);
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
        addReaction(widgetModel.getPreFlightStatus()
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(this::updateUI));
    }
    //endregion

    //region Reactions to model
    private void updateUI(WarningStatusItem status) {
        preFlightStatusTextView.setText(status.getMessage());
        switch (status.getWarningLevel()) {
            case GOOD:
                preFlightStatusBackgroundImageView.setImageDrawable(backgroundGood);
                break;
            case WARNING:
                preFlightStatusBackgroundImageView.setImageDrawable(backgroundWarning);
                break;
            case ERROR:
                preFlightStatusBackgroundImageView.setImageDrawable(backgroundError);
                break;
            case OFFLINE:
            default:
                preFlightStatusBackgroundImageView.setImageDrawable(backgroundOffline);
                break;
        }
        blinkBackground(status.isUrgentMessage());
    }

    private void blinkBackground(boolean isUrgentMessage) {
        if (isUrgentMessage) {
            preFlightStatusBackgroundImageView.startAnimation(blinkAnimation);
        } else {
            preFlightStatusBackgroundImageView.clearAnimation();
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_preflight_status_ratio);
    }
    //endregion

    //region Customization Helpers

    /**
     * Set text appearance of the pre-flight status message text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setPreFlightStatusMessageTextAppearance(@StyleRes int textAppearance) {
        preFlightStatusTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the pre-flight status message text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setPreFlightStatusMessageTextColor(@NonNull ColorStateList colorStateList) {
        preFlightStatusTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the pre-flight status message text view
     *
     * @param color color integer resource
     */
    public void setPreFlightStatusMessageTextColor(@ColorInt int color) {
        preFlightStatusTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the pre-flight status message text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getPreFlightStatusMessageTextColors() {
        return preFlightStatusTextView.getTextColors();
    }

    /**
     * Get current text color of the pre-flight status message text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getPreFlightStatusMessageTextColor() {
        return preFlightStatusTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the pre-flight status message text view
     *
     * @param textSize text size float value
     */
    public void setPreFlightStatusMessageTextSize(@Dimension float textSize) {
        preFlightStatusTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the pre-flight status message text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getPreFlightStatusMessageTextSize() {
        return preFlightStatusTextView.getTextSize();
    }

    /**
     * Set the background color of the pre-flight status error background
     * The background will be a gradient from the input color param to an alpha of zero
     *
     * @param startColor color integer resource for starting color of gradient
     */
    public void setPreFlightStatusBackgroundErrorColor(@ColorInt int startColor) {
        backgroundError.setColors(new int[] { startColor, ColorUtils.setAlphaComponent(startColor, 0) });
    }

    /**
     * Set the background colors of the pre-flight status error background
     * The background will be a gradient from the start color to the end color
     *
     * @param startColor color integer resource for starting color of gradient
     * @param endColor color integer resource for ending color of gradient
     */
    public void setPreFlightStatusBackgroundErrorColors(@ColorInt int startColor, @ColorInt int endColor) {
        backgroundError.setColors(new int[] { startColor, endColor });
    }

    /**
     * Get the background colors of the pre-flight status error background
     * Returns the colors used to draw the gradient, or null if the gradient is drawn
     * using a single color or no colors.
     *
     * @return colors colors used to draw the gradient as an int array.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public int[] getPreFlightStatusBackgroundErrorColors() {
        return backgroundError.getColors();
    }

    /**
     * Set the background color of the pre-flight status warning background
     * The background will be a gradient from the input color param to an alpha of zero
     *
     * @param startColor color integer resource for starting color of gradient
     */
    public void setPreFlightStatusBackgroundWarningColor(@ColorInt int startColor) {
        backgroundWarning.setColors(new int[] { startColor, ColorUtils.setAlphaComponent(startColor, 0) });
    }

    /**
     * Set the background colors of the pre-flight status warning background
     * The background will be a gradient from the start color to the end color
     *
     * @param startColor color integer resource for starting color of gradient
     * @param endColor color integer resource for ending color of gradient
     */
    public void setPreFlightStatusBackgroundWarningColors(@ColorInt int startColor, @ColorInt int endColor) {
        backgroundWarning.setColors(new int[] { startColor, endColor });
    }

    /**
     * Get the background colors of the pre-flight status warning background
     * Returns the colors used to draw the gradient, or null if the gradient is drawn
     * using a single color or no colors.
     *
     * @return colors colors used to draw the gradient as an int array.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public int[] getPreFlightStatusBackgroundWarningColors() {
        return backgroundWarning.getColors();
    }

    /**
     * Set the background color of the pre-flight status good background
     * The background will be a gradient from the input color param to an alpha of zero
     *
     * @param startColor color integer resource for starting color of gradient
     */
    public void setPreFlightStatusBackgroundGoodColor(@ColorInt int startColor) {
        backgroundGood.setColors(new int[] { startColor, ColorUtils.setAlphaComponent(startColor, 0) });
    }

    /**
     * Set the background colors of the pre-flight status good background
     * The background will be a gradient from the start color to the end color
     *
     * @param startColor color integer resource for starting color of gradient
     * @param endColor color integer resource for ending color of gradient
     */
    public void setPreFlightStatusBackgroundGoodColors(@ColorInt int startColor, @ColorInt int endColor) {
        backgroundGood.setColors(new int[] { startColor, endColor });
    }

    /**
     * Get the background colors of the pre-flight status good background
     * Returns the colors used to draw the gradient, or null if the gradient is drawn
     * using a single color or no colors.
     *
     * @return colors colors used to draw the gradient as an int array.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public int[] getPreFlightStatusBackgroundGoodColors() {
        return backgroundGood.getColors();
    }

    /**
     * Set the background color of the pre-flight status offline background
     * The background will be a gradient from the input color param to an alpha of zero
     *
     * @param startColor color integer resource for starting color of gradient
     */
    public void setPreFlightStatusBackgroundOfflineColor(@ColorInt int startColor) {
        backgroundOffline.setColors(new int[] { startColor, ColorUtils.setAlphaComponent(startColor, 0) });
    }

    /**
     * Set the background colors of the pre-flight status offline background
     * The background will be a gradient from the start color to the end color
     *
     * @param startColor color integer resource for starting color of gradient
     * @param endColor color integer resource for ending color of gradient
     */
    public void setPreFlightStatusBackgroundOfflineColors(@ColorInt int startColor, @ColorInt int endColor) {
        backgroundOffline.setColors(new int[] { startColor, endColor });
    }

    /**
     * Get the background colors of the pre-flight status offline background
     * Returns the colors used to draw the gradient, or null if the gradient is drawn
     * using a single color or no colors.
     *
     * @return colors colors used to draw the gradient as an int array.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public int[] getPreFlightStatusBackgroundOfflineColors() {
        return backgroundOffline.getColors();
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PreFlightStatusWidget);
        int preFlightStatusMessageTextAppearanceId =
            typedArray.getResourceId(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusMessageTextAppearance,
                                     INVALID_RESOURCE);
        if (preFlightStatusMessageTextAppearanceId != INVALID_RESOURCE) {
            setPreFlightStatusMessageTextAppearance(preFlightStatusMessageTextAppearanceId);
        }
        float preFlightStatusMessageTextSize =
            typedArray.getDimension(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusMessageTextSize,
                                    INVALID_RESOURCE);
        if (preFlightStatusMessageTextSize != INVALID_RESOURCE) {
            setPreFlightStatusMessageTextSize(DisplayUtil.pxToSp(context, preFlightStatusMessageTextSize));
        }
        int preFlightStatusMessageTextColor =
            typedArray.getColor(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusMessageTextColor,
                                INVALID_COLOR);
        if (preFlightStatusMessageTextColor != INVALID_COLOR) {
            setPreFlightStatusMessageTextColor(preFlightStatusMessageTextColor);
        }
        int preFlightStatusBackgroundErrorColor =
            typedArray.getColor(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusBackgroundErrorColor,
                                INVALID_COLOR);
        if (preFlightStatusBackgroundErrorColor != INVALID_COLOR) {
            setPreFlightStatusBackgroundErrorColor(preFlightStatusBackgroundErrorColor);
        }
        int preFlightStatusBackgroundWarningColor =
            typedArray.getColor(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusBackgroundWarningColor,
                                INVALID_COLOR);
        if (preFlightStatusBackgroundWarningColor != INVALID_COLOR) {
            setPreFlightStatusBackgroundWarningColor(preFlightStatusBackgroundWarningColor);
        }
        int preFlightStatusBackgroundGoodColor =
            typedArray.getColor(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusBackgroundGoodColor,
                                INVALID_COLOR);
        if (preFlightStatusBackgroundGoodColor != INVALID_COLOR) {
            setPreFlightStatusBackgroundGoodColor(preFlightStatusBackgroundGoodColor);
        }
        int preFlightStatusBackgroundOfflineColor =
            typedArray.getColor(R.styleable.PreFlightStatusWidget_uxsdk_preFlightStatusBackgroundOfflineColor,
                                INVALID_COLOR);
        if (preFlightStatusBackgroundOfflineColor != INVALID_COLOR) {
            setPreFlightStatusBackgroundOfflineColor(preFlightStatusBackgroundOfflineColor);
        }
        typedArray.recycle();
    }
    //endregion
}
