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

package dji.ux.beta.core.widget.horizontalvelocity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import java.text.DecimalFormat;

import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.R;
import dji.ux.beta.core.base.ConstraintLayoutWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.GlobalPreferencesInterface;
import dji.ux.beta.core.base.GlobalPreferencesManager;
import dji.ux.beta.core.base.uxsdkkeys.GlobalPreferenceKeys;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.UnitConversionUtil;

/**
 * Shows the horizontal velocity of the aircraft.
 * <p>
 * Uses the unit set in the UNIT_TYPE global preferences
 * {@link GlobalPreferencesInterface#getUnitType()} and the
 * {@link GlobalPreferenceKeys#UNIT_TYPE} UX Key
 * and defaults to m/s if nothing is set.
 */
public class HorizontalVelocityWidget extends ConstraintLayoutWidget {
    //region Constants
    private static final int EMS = 2;
    private static final float MINIMUM_VELOCITY = 0.0f;
    private static final float MAXIMUM_VELOCITY = 50.0f;
    //endregion

    //region Fields
    private static DecimalFormat decimalFormat = new DecimalFormat("#0.0");
    private TextView horizontalVelocityTitleTextView;
    private TextView horizontalVelocityValueTextView;
    private TextView horizontalVelocityUnitTextView;
    private UnitConversionUtil.SpeedMetricUnitType speedMetricUnitType;
    private HorizontalVelocityWidgetModel widgetModel;
    //endregion

    //region Constructor
    public HorizontalVelocityWidget(Context context) {
        super(context);
    }

    public HorizontalVelocityWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalVelocityWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_dashboard_text_only, this);
        horizontalVelocityTitleTextView = findViewById(R.id.textview_title);
        horizontalVelocityValueTextView = findViewById(R.id.textview_value);
        horizontalVelocityUnitTextView = findViewById(R.id.textview_unit);

        if (!isInEditMode()) {
            widgetModel = new HorizontalVelocityWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
            horizontalVelocityTitleTextView.setText(getResources().getString(R.string.uxsdk_horizontal_velocity_title));
            horizontalVelocityValueTextView.setMinEms(EMS);
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
        addReaction(reactToHorizontalVelocityChange());
    }
    //endregion

    //region reaction helpers
    private Disposable reactToHorizontalVelocityChange() {
        return Flowable.combineLatest(widgetModel.getHorizontalVelocity(), widgetModel.getUnitType(), Pair::new)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(values -> updateUI(values.first, values.second));
    }

    private void updateUI(@FloatRange(from = MINIMUM_VELOCITY, to = MAXIMUM_VELOCITY) float horizontalVelocity,
                          UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.METRIC
                && speedMetricUnitType == UnitConversionUtil.SpeedMetricUnitType.KM_PER_HOUR) {
            horizontalVelocityValueTextView.setText(decimalFormat.format(UnitConversionUtil.convertMetersPerSecToKmPerHr(
                    horizontalVelocity)));
        } else {
            //Metric m/s or imperial mph will come through already converted
            horizontalVelocityValueTextView.setText(decimalFormat.format(horizontalVelocity));
        }
        updateUnitText(unitType);
    }

    private void updateUnitText(UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            horizontalVelocityUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_mile_per_hr));
        } else {
            if (speedMetricUnitType == UnitConversionUtil.SpeedMetricUnitType.KM_PER_HOUR) {
                horizontalVelocityUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_km_per_hr));
            } else {
                horizontalVelocityUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_meter_per_second));
            }
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
     * Set text appearance of the horizontal velocity title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setHorizontalVelocityTitleTextAppearance(@StyleRes int textAppearance) {
        horizontalVelocityTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the horizontal velocity title text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getHorizontalVelocityTitleTextColors() {
        return horizontalVelocityTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the horizontal velocity title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getHorizontalVelocityTitleTextColor() {
        return horizontalVelocityTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the horizontal velocity title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setHorizontalVelocityTitleTextColor(@NonNull ColorStateList colorStateList) {
        horizontalVelocityTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the horizontal velocity title text view
     *
     * @param color color integer resource
     */
    public void setHorizontalVelocityTitleTextColor(@ColorInt int color) {
        horizontalVelocityTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the horizontal velocity title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getHorizontalVelocityTitleTextSize() {
        return horizontalVelocityTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the horizontal velocity title text view
     *
     * @param textSize text size float value
     */
    public void setHorizontalVelocityTitleTextSize(@Dimension float textSize) {
        horizontalVelocityTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the horizontal velocity title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getHorizontalVelocityTitleTextBackground() {
        return horizontalVelocityTitleTextView.getBackground();
    }

    /**
     * Set the background of the horizontal velocity title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setHorizontalVelocityTitleTextBackground(@Nullable Drawable drawable) {
        horizontalVelocityTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the horizontal velocity title text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setHorizontalVelocityTitleTextBackground(@DrawableRes int resourceId) {
        horizontalVelocityTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the horizontal velocity value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setHorizontalVelocityValueTextAppearance(@StyleRes int textAppearance) {
        horizontalVelocityValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the horizontal velocity value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getHorizontalVelocityValueTextColors() {
        return horizontalVelocityValueTextView.getTextColors();
    }

    /**
     * Get current text color of the horizontal velocity value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getHorizontalVelocityValueTextColor() {
        return horizontalVelocityValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the horizontal velocity value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setHorizontalVelocityValueTextColor(@NonNull ColorStateList colorStateList) {
        horizontalVelocityValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the horizontal velocity value text view
     *
     * @param color color integer resource
     */
    public void setHorizontalVelocityValueTextColor(@ColorInt int color) {
        horizontalVelocityValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the horizontal velocity value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getHorizontalVelocityValueTextSize() {
        return horizontalVelocityValueTextView.getTextSize();
    }

    /**
     * Set the text size of the horizontal velocity value text view
     *
     * @param textSize text size float value
     */
    public void setHorizontalVelocityValueTextSize(@Dimension float textSize) {
        horizontalVelocityValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the horizontal velocity value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getHorizontalVelocityValueTextBackground() {
        return horizontalVelocityValueTextView.getBackground();
    }

    /**
     * Set the background for the horizontal velocity value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setHorizontalVelocityValueTextBackground(@Nullable Drawable drawable) {
        horizontalVelocityValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the horizontal velocity value text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setHorizontalVelocityValueTextBackground(@DrawableRes int resourceId) {
        horizontalVelocityValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the horizontal velocity unit text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setHorizontalVelocityUnitTextAppearance(@StyleRes int textAppearance) {
        horizontalVelocityUnitTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the horizontal velocity unit text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getHorizontalVelocityUnitTextColors() {
        return horizontalVelocityUnitTextView.getTextColors();
    }

    /**
     * Get current text color of the horizontal velocity unit text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getHorizontalVelocityUnitTextColor() {
        return horizontalVelocityUnitTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the horizontal velocity  unit text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setHorizontalVelocityUnitTextColor(@NonNull ColorStateList colorStateList) {
        horizontalVelocityUnitTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the horizontal velocity unit text view
     *
     * @param color color integer resource
     */
    public void setHorizontalVelocityUnitTextColor(@ColorInt int color) {
        horizontalVelocityUnitTextView.setTextColor(color);
    }

    /**
     * Get current text size of the horizontal velocity unit text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getHorizontalVelocityUnitTextSize() {
        return horizontalVelocityUnitTextView.getTextSize();
    }

    /**
     * Set the text size of the horizontal velocity unit text view
     *
     * @param textSize text size float value
     */
    public void setHorizontalVelocityUnitTextSize(@Dimension float textSize) {
        horizontalVelocityUnitTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the horizontal velocity unit text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getHorizontalVelocityUnitTextBackground() {
        return horizontalVelocityUnitTextView.getBackground();
    }

    /**
     * Set the background for the horizontal velocity unit text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setHorizontalVelocityUnitTextBackground(@Nullable Drawable drawable) {
        horizontalVelocityUnitTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the horizontal velocity unit text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setHorizontalVelocityUnitTextBackground(@DrawableRes int resourceId) {
        horizontalVelocityUnitTextView.setBackgroundResource(resourceId);
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalVelocityWidget);
        speedMetricUnitType =
                UnitConversionUtil.SpeedMetricUnitType.find(typedArray.getInteger(R.styleable.HorizontalVelocityWidget_uxsdk_speedMetricUnitType,
                        UnitConversionUtil.SpeedMetricUnitType.METERS_PER_SECOND
                                .value()));

        int horizontalVelocityTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityTitleTextAppearance,
                        INVALID_RESOURCE);
        if (horizontalVelocityTitleTextAppearanceId != INVALID_RESOURCE) {
            setHorizontalVelocityTitleTextAppearance(horizontalVelocityTitleTextAppearanceId);
        }

        float horizontalVelocityTitleTextSize =
                typedArray.getDimension(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityTitleTextSize,
                        INVALID_RESOURCE);
        if (horizontalVelocityTitleTextSize != INVALID_RESOURCE) {
            setHorizontalVelocityTitleTextSize(DisplayUtil.pxToSp(context, horizontalVelocityTitleTextSize));
        }

        int horizontalVelocityTitleTextColor =
                typedArray.getColor(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityTitleTextColor,
                        INVALID_COLOR);
        if (horizontalVelocityTitleTextColor != INVALID_COLOR) {
            setHorizontalVelocityTitleTextColor(horizontalVelocityTitleTextColor);
        }

        Drawable horizontalVelocityTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityTitleBackgroundDrawable);
        if (horizontalVelocityTitleTextBackgroundDrawable != null) {
            setHorizontalVelocityTitleTextBackground(horizontalVelocityTitleTextBackgroundDrawable);
        }

        int horizontalVelocityValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityValueTextAppearance,
                        INVALID_RESOURCE);
        if (horizontalVelocityValueTextAppearanceId != INVALID_RESOURCE) {
            setHorizontalVelocityValueTextAppearance(horizontalVelocityValueTextAppearanceId);
        }

        float horizontalVelocityValueTextSize =
                typedArray.getDimension(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityValueTextSize,
                        INVALID_RESOURCE);
        if (horizontalVelocityValueTextSize != INVALID_RESOURCE) {
            setHorizontalVelocityValueTextSize(DisplayUtil.pxToSp(context, horizontalVelocityValueTextSize));
        }

        int horizontalVelocityValueTextColor =
                typedArray.getColor(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityValueTextColor,
                        INVALID_COLOR);
        if (horizontalVelocityValueTextColor != INVALID_COLOR) {
            setHorizontalVelocityValueTextColor(horizontalVelocityValueTextColor);
        }

        Drawable horizontalVelocityValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityValueBackgroundDrawable);
        if (horizontalVelocityValueTextBackgroundDrawable != null) {
            setHorizontalVelocityValueTextBackground(horizontalVelocityValueTextBackgroundDrawable);
        }

        int horizontalVelocityUnitTextAppearanceId =
                typedArray.getResourceId(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityUnitTextAppearance,
                        INVALID_RESOURCE);
        if (horizontalVelocityUnitTextAppearanceId != INVALID_RESOURCE) {
            setHorizontalVelocityUnitTextAppearance(horizontalVelocityUnitTextAppearanceId);
        }

        float horizontalVelocityUnitTextSize =
                typedArray.getDimension(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityUnitTextSize,
                        INVALID_RESOURCE);
        if (horizontalVelocityUnitTextSize != INVALID_RESOURCE) {
            setHorizontalVelocityUnitTextSize(DisplayUtil.pxToSp(context, horizontalVelocityUnitTextSize));
        }

        int horizontalVelocityUnitTextColor =
                typedArray.getColor(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityUnitTextColor,
                        INVALID_COLOR);
        if (horizontalVelocityUnitTextColor != INVALID_COLOR) {
            setHorizontalVelocityUnitTextColor(horizontalVelocityUnitTextColor);
        }

        Drawable horizontalVelocityUnitTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.HorizontalVelocityWidget_uxsdk_horizontalVelocityUnitBackgroundDrawable);
        if (horizontalVelocityUnitTextBackgroundDrawable != null) {
            setHorizontalVelocityUnitTextBackground(horizontalVelocityUnitTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
