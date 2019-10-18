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

package dji.ux.beta.widget.verticalvelocity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import java.text.DecimalFormat;

import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.GlobalPreferencesManager;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DisplayUtil;
import dji.ux.beta.util.UnitConversionUtil;

/**
 * Shows the vertical velocity of the aircraft. The
 * arrow indicates the direction of travel of the aircraft.
 * <p>
 * Uses the unit set in the UNIT_TYPE global preferences
 * {@link dji.ux.beta.base.GlobalPreferencesInterface#getUnitType()} and the
 * {@link dji.ux.beta.base.uxsdkkeys.GlobalPreferenceKeys#UNIT_TYPE} UX Key
 * and defaults to m/s if nothing is set.
 */
public class VerticalVelocityWidget extends ConstraintLayoutWidget {
    //region Constants
    private static final int EMS = 2;
    private static final float MINIMUM_VELOCITY = -20.0f;
    private static final float MAXIMUM_VELOCITY = 20.0f;
    //endregion

    //region Fields
    private static DecimalFormat decimalFormat = new DecimalFormat("#0.0");
    private ImageView verticalVelocityImageView;
    private TextView verticalVelocityTitleTextView;
    private TextView verticalVelocityValueTextView;
    private TextView verticalVelocityUnitTextView;
    private Drawable upwardVelocityDrawable;
    private Drawable downwardVelocityDrawable;
    private VerticalVelocityWidgetModel widgetModel;
    //endregion

    public VerticalVelocityWidget(Context context) {
        super(context);
    }

    public VerticalVelocityWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalVelocityWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_dashboard_image_and_text, this);
        verticalVelocityImageView = findViewById(R.id.imageview_icon);
        verticalVelocityTitleTextView = findViewById(R.id.textview_title);
        verticalVelocityValueTextView = findViewById(R.id.textview_value);
        verticalVelocityUnitTextView = findViewById(R.id.textview_unit);

        if (!isInEditMode()) {
            widgetModel = new VerticalVelocityWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
            verticalVelocityTitleTextView.setText(getResources().getString(R.string.uxsdk_vertical_velocity_title));
            verticalVelocityValueTextView.setMinEms(EMS);
        }

        upwardVelocityDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_arrow_up);
        downwardVelocityDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_arrow_down);

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
        addReaction(widgetModel.getVerticalVelocity()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateValueText));
        addReaction(widgetModel.getUnitType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUnitText));
    }
    //endregion

    //region reaction helpers
    private void updateValueText(@FloatRange(from = MINIMUM_VELOCITY, to = MAXIMUM_VELOCITY) float verticalVelocity) {
        verticalVelocityValueTextView.setText(decimalFormat.format(Math.abs(verticalVelocity)));
        if (verticalVelocity == 0) {
            verticalVelocityImageView.setVisibility(GONE);
        } else if (verticalVelocity < 0) {
            verticalVelocityImageView.setVisibility(VISIBLE);
            verticalVelocityImageView.setImageDrawable(upwardVelocityDrawable);
        } else {
            verticalVelocityImageView.setVisibility(VISIBLE);
            verticalVelocityImageView.setImageDrawable(downwardVelocityDrawable);
        }
    }

    private void updateUnitText(UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            verticalVelocityUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_mile_per_hr));
        } else {
            verticalVelocityUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_meter_per_second));
        }
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_base_dashboard_distance_ratio);
    }
    //endregion

    //region Customization Helpers

    /**
     * Set text appearance of the vertical velocity title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setVerticalVelocityTitleTextAppearance(@StyleRes int textAppearance) {
        verticalVelocityTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the vertical velocity title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setVerticalVelocityTitleTextColor(@NonNull ColorStateList colorStateList) {
        verticalVelocityTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the vertical velocity title text view
     *
     * @param color color integer resource
     */
    public void setVerticalVelocityTitleTextColor(@ColorInt int color) {
        verticalVelocityTitleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the vertical velocity title text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getVerticalVelocityTitleTextColors() {
        return verticalVelocityTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the vertical velocity title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getVerticalVelocityTitleTextColor() {
        return verticalVelocityTitleTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the vertical velocity title text view
     *
     * @param textSize text size float value
     */
    public void setVerticalVelocityTitleTextSize(@Dimension float textSize) {
        verticalVelocityTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the vertical velocity title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getVerticalVelocityTitleTextSize() {
        return verticalVelocityTitleTextView.getTextSize();
    }

    /**
     * Set the background of the vertical velocity title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setVerticalVelocityTitleTextBackground(@Nullable Drawable drawable) {
        verticalVelocityTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the vertical velocity title text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setVerticalVelocityTitleTextBackground(@DrawableRes int resourceId) {
        verticalVelocityTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the vertical velocity title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getVerticalVelocityTitleTextBackground() {
        return verticalVelocityTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the upward vertical velocity icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setUpwardVerticalVelocityIcon(@DrawableRes int resourceId) {
        upwardVelocityDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Set the drawable resource for the upward vertical velocity icon
     *
     * @param icon Drawable resource for the image
     */
    public void setUpwardVerticalVelocityIcon(@Nullable Drawable icon) {
        upwardVelocityDrawable = icon;
    }

    /**
     * Set the resource ID for the downward vertical velocity icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setDownwardVerticalVelocityIcon(@DrawableRes int resourceId) {
        downwardVelocityDrawable = getResources().getDrawable(resourceId);
    }

    /**
     * Set the drawable resource for the downward vertical velocity icon
     *
     * @param icon Drawable resource for the image
     */
    public void setDownwardVerticalVelocityIcon(@Nullable Drawable icon) {
        downwardVelocityDrawable = icon;
    }

    /**
     * Get the drawable resource for the upward vertical velocity icon
     *
     * @return Drawable resource of the icon
     */
    @Nullable
    public Drawable getUpwardVerticalVelocityIcon() {
        return upwardVelocityDrawable;
    }

    /**
     * Get the drawable resource for the downward vertical velocity icon
     *
     * @return Drawable resource of the icon
     */
    @Nullable
    public Drawable getDownwardVerticalVelocityIcon() {
        return downwardVelocityDrawable;
    }

    /**
     * Set the resource ID for the vertical velocity icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    public void setVerticalVelocityIconBackground(@DrawableRes int resourceId) {
        verticalVelocityImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the vertical velocity icon's background
     *
     * @param background Drawable resource for the icon's background
     */
    public void setVerticalVelocityIconBackground(@Nullable Drawable background) {
        verticalVelocityImageView.setBackground(background);
    }

    /**
     * Get the drawable resource for the vertical velocity icon's background
     *
     * @return Drawable resource of the icon's background
     */
    @Nullable
    public Drawable getVerticalVelocityIconBackground() {
        return verticalVelocityImageView.getBackground();
    }

    /**
     * Set text appearance of the vertical velocity value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setVerticalVelocityValueTextAppearance(@StyleRes int textAppearance) {
        verticalVelocityValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the vertical velocity value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setVerticalVelocityValueTextColor(@NonNull ColorStateList colorStateList) {
        verticalVelocityValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the vertical velocity value text view
     *
     * @param color color integer resource
     */
    public void setVerticalVelocityValueTextColor(@ColorInt int color) {
        verticalVelocityValueTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the vertical velocity value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getVerticalVelocityValueTextColors() {
        return verticalVelocityValueTextView.getTextColors();
    }

    /**
     * Get current text color of the vertical velocity value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getVerticalVelocityValueTextColor() {
        return verticalVelocityValueTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the vertical velocity value text view
     *
     * @param textSize text size float value
     */
    public void setVerticalVelocityValueTextSize(@Dimension float textSize) {
        verticalVelocityValueTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the vertical velocity value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getVerticalVelocityValueTextSize() {
        return verticalVelocityValueTextView.getTextSize();
    }

    /**
     * Set the background for the vertical velocity value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setVerticalVelocityValueTextBackground(@Nullable Drawable drawable) {
        verticalVelocityValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the vertical velocity value text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setVerticalVelocityValueTextBackground(@DrawableRes int resourceId) {
        verticalVelocityValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the vertical velocity value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getVerticalVelocityValueTextBackground() {
        return verticalVelocityValueTextView.getBackground();
    }

    /**
     * Set text appearance of the vertical velocity unit text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setVerticalVelocityUnitTextAppearance(@StyleRes int textAppearance) {
        verticalVelocityUnitTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the vertical velocity  unit text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setVerticalVelocityUnitTextColor(@NonNull ColorStateList colorStateList) {
        verticalVelocityUnitTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the vertical velocity unit text view
     *
     * @param color color integer resource
     */
    public void setVerticalVelocityUnitTextColor(@ColorInt int color) {
        verticalVelocityUnitTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the vertical velocity unit text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getVerticalVelocityUnitTextColors() {
        return verticalVelocityUnitTextView.getTextColors();
    }

    /**
     * Get current text color of the vertical velocity unit text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getVerticalVelocityUnitTextColor() {
        return verticalVelocityUnitTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the vertical velocity unit text view
     *
     * @param textSize text size float value
     */
    public void setVerticalVelocityUnitTextSize(@Dimension float textSize) {
        verticalVelocityUnitTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the vertical velocity unit text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getVerticalVelocityUnitTextSize() {
        return verticalVelocityUnitTextView.getTextSize();
    }

    /**
     * Set the background for the vertical velocity unit text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setVerticalVelocityUnitTextBackground(@Nullable Drawable drawable) {
        verticalVelocityUnitTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the vertical velocity unit text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setVerticalVelocityUnitTextBackground(@DrawableRes int resourceId) {
        verticalVelocityUnitTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the vertical velocity unit text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getVerticalVelocityUnitTextBackground() {
        return verticalVelocityUnitTextView.getBackground();
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalVelocityWidget);
        int verticalVelocityTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityTitleTextAppearance,
                        INVALID_RESOURCE);
        if (verticalVelocityTitleTextAppearanceId != INVALID_RESOURCE) {
            setVerticalVelocityTitleTextAppearance(verticalVelocityTitleTextAppearanceId);
        }

        float verticalVelocityTitleTextSize =
                typedArray.getDimension(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityTitleTextSize, INVALID_RESOURCE);
        if (verticalVelocityTitleTextSize != INVALID_RESOURCE) {
            setVerticalVelocityTitleTextSize(DisplayUtil.pxToSp(context, verticalVelocityTitleTextSize));
        }

        int verticalVelocityTitleTextColor =
                typedArray.getColor(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityTitleTextColor, INVALID_COLOR);
        if (verticalVelocityTitleTextColor != INVALID_COLOR) {
            setVerticalVelocityTitleTextColor(verticalVelocityTitleTextColor);
        }

        Drawable verticalVelocityTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityTitleBackgroundDrawable);
        if (verticalVelocityTitleTextBackgroundDrawable != null) {
            setVerticalVelocityTitleTextBackground(verticalVelocityTitleTextBackgroundDrawable);
        }

        Drawable upwardVerticalVelocityIcon =
                typedArray.getDrawable(R.styleable.VerticalVelocityWidget_uxsdk_upwardVerticalVelocityIcon);
        if (upwardVerticalVelocityIcon != null) {
            setUpwardVerticalVelocityIcon(upwardVerticalVelocityIcon);
        }

        Drawable downwardVerticalVelocityIcon =
                typedArray.getDrawable(R.styleable.VerticalVelocityWidget_uxsdk_downwardVerticalVelocityIcon);
        if (downwardVerticalVelocityIcon != null) {
            setDownwardVerticalVelocityIcon(downwardVerticalVelocityIcon);
        }

        int verticalVelocityValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityValueTextAppearance,
                        INVALID_RESOURCE);
        if (verticalVelocityValueTextAppearanceId != INVALID_RESOURCE) {
            setVerticalVelocityValueTextAppearance(verticalVelocityValueTextAppearanceId);
        }

        float verticalVelocityValueTextSize =
                typedArray.getDimension(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityValueTextSize, INVALID_RESOURCE);
        if (verticalVelocityValueTextSize != INVALID_RESOURCE) {
            setVerticalVelocityValueTextSize(DisplayUtil.pxToSp(context, verticalVelocityValueTextSize));
        }

        int verticalVelocityValueTextColor =
                typedArray.getColor(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityValueTextColor, INVALID_COLOR);
        if (verticalVelocityValueTextColor != INVALID_COLOR) {
            setVerticalVelocityValueTextColor(verticalVelocityValueTextColor);
        }

        Drawable verticalVelocityValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityValueBackgroundDrawable);
        if (verticalVelocityValueTextBackgroundDrawable != null) {
            setVerticalVelocityValueTextBackground(verticalVelocityValueTextBackgroundDrawable);
        }

        int verticalVelocityUnitTextAppearanceId =
                typedArray.getResourceId(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityUnitTextAppearance,
                        INVALID_RESOURCE);
        if (verticalVelocityUnitTextAppearanceId != INVALID_RESOURCE) {
            setVerticalVelocityUnitTextAppearance(verticalVelocityUnitTextAppearanceId);
        }

        float verticalVelocityUnitTextSize =
                typedArray.getDimension(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityUnitTextSize, INVALID_RESOURCE);
        if (verticalVelocityUnitTextSize != INVALID_RESOURCE) {
            setVerticalVelocityUnitTextSize(DisplayUtil.pxToSp(context, verticalVelocityUnitTextSize));
        }

        int verticalVelocityUnitTextColor =
                typedArray.getColor(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityUnitTextColor, INVALID_COLOR);
        if (verticalVelocityUnitTextColor != INVALID_COLOR) {
            setVerticalVelocityUnitTextColor(verticalVelocityUnitTextColor);
        }

        Drawable verticalVelocityUnitTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.VerticalVelocityWidget_uxsdk_verticalVelocityUnitBackgroundDrawable);
        if (verticalVelocityUnitTextBackgroundDrawable != null) {
            setVerticalVelocityUnitTextBackground(verticalVelocityUnitTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
