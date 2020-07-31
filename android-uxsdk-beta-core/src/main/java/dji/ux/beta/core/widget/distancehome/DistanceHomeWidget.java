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

package dji.ux.beta.core.widget.distancehome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
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
import dji.ux.beta.core.base.ConstraintLayoutWidget;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.GlobalPreferencesInterface;
import dji.ux.beta.core.base.GlobalPreferencesManager;
import dji.ux.beta.core.base.uxsdkkeys.GlobalPreferenceKeys;
import dji.ux.beta.core.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.UnitConversionUtil;

/**
 * Shows the distance between the current location of the aircraft
 * and home.
 * Uses the unit set in the UNIT_TYPE global preferences
 * {@link GlobalPreferencesInterface#getUnitType()} and the
 * {@link GlobalPreferenceKeys#UNIT_TYPE} UX Key
 * and defaults to meters.
 */
public class DistanceHomeWidget extends ConstraintLayoutWidget {
    //region Fields
    private static final int EMS = 3;
    private static DecimalFormat decimalFormat = new DecimalFormat("###0.0");
    private TextView distanceHomeTitleTextView;
    private TextView distanceHomeValueTextView;
    private TextView distanceHomeUnitTextView;
    private DistanceHomeWidgetModel widgetModel;
    //endregion

    //region Constructors
    public DistanceHomeWidget(Context context) {
        super(context);
    }

    public DistanceHomeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DistanceHomeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_dashboard_text_only, this);
        distanceHomeTitleTextView = findViewById(R.id.textview_title);
        distanceHomeValueTextView = findViewById(R.id.textview_value);
        distanceHomeUnitTextView = findViewById(R.id.textview_unit);

        if (!isInEditMode()) {
            widgetModel = new DistanceHomeWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
            distanceHomeTitleTextView.setText(getResources().getString(R.string.uxsdk_distance_home_title));
            distanceHomeValueTextView.setMinEms(EMS);
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
        addReaction(widgetModel.getDistanceFromHome()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateValueText));
        addReaction(widgetModel.getUnitType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUnitText));
    }
    //endregion

    //region Reactions to model
    private void updateValueText(@FloatRange(from = 0.0f) float distanceFromHome) {
        distanceHomeValueTextView.setText(decimalFormat.format(distanceFromHome));
    }

    private void updateUnitText(UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            distanceHomeUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_feet));
        } else {
            distanceHomeUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_meters));
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
     * Set text appearance of the distance from home title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceHomeTitleTextAppearance(@StyleRes int textAppearance) {
        distanceHomeTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the distance from home title text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceHomeTitleTextColors() {
        return distanceHomeTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from home title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceHomeTitleTextColor() {
        return distanceHomeTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the distance from home title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceHomeTitleTextColor(@NonNull ColorStateList colorStateList) {
        distanceHomeTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from home title text view
     *
     * @param color color integer resource
     */
    public void setDistanceHomeTitleTextColor(@ColorInt int color) {
        distanceHomeTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the distance from home title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceHomeTitleTextSize() {
        return distanceHomeTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the distance from home title text view
     *
     * @param textSize text size float value
     */
    public void setDistanceHomeTitleTextSize(@Dimension float textSize) {
        distanceHomeTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the distance from home title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceHomeTitleTextBackground() {
        return distanceHomeTitleTextView.getBackground();
    }

    /**
     * Set the background of the distance from home title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceHomeTitleTextBackground(@Nullable Drawable drawable) {
        distanceHomeTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from home title text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceHomeTitleTextBackground(@DrawableRes int resourceId) {
        distanceHomeTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the distance from home value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceHomeValueTextAppearance(@StyleRes int textAppearance) {
        distanceHomeValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the distance from home value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceHomeValueTextColors() {
        return distanceHomeValueTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from home value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceHomeValueTextColor() {
        return distanceHomeValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the distance from home value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceHomeValueTextColor(@NonNull ColorStateList colorStateList) {
        distanceHomeValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from home value text view
     *
     * @param color color integer resource
     */
    public void setDistanceHomeValueTextColor(@ColorInt int color) {
        distanceHomeValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the distance from home value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceHomeValueTextSize() {
        return distanceHomeValueTextView.getTextSize();
    }

    /**
     * Set the text size of the distance from home value text view
     *
     * @param textSize text size float value
     */
    public void setDistanceHomeValueTextSize(@Dimension float textSize) {
        distanceHomeValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the distance from home value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceHomeValueTextBackground() {
        return distanceHomeValueTextView.getBackground();
    }

    /**
     * Set the background for the distance from home value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceHomeValueTextBackground(@Nullable Drawable drawable) {
        distanceHomeValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from home value text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceHomeValueTextBackground(@DrawableRes int resourceId) {
        distanceHomeValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the distance from home unit text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceHomeUnitTextAppearance(@StyleRes int textAppearance) {
        distanceHomeUnitTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the distance from home unit text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceHomeUnitTextColors() {
        return distanceHomeUnitTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from home unit text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceHomeUnitTextColor() {
        return distanceHomeUnitTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the distance from home unit text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceHomeUnitTextColor(@NonNull ColorStateList colorStateList) {
        distanceHomeUnitTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from home unit text view
     *
     * @param color color integer resource
     */
    public void setDistanceHomeUnitTextColor(@ColorInt int color) {
        distanceHomeUnitTextView.setTextColor(color);
    }

    /**
     * Get current text size of the distance from home unit text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceHomeUnitTextSize() {
        return distanceHomeUnitTextView.getTextSize();
    }

    /**
     * Set the text size of the distance from home unit text view
     *
     * @param textSize text size float value
     */
    public void setDistanceHomeUnitTextSize(@Dimension float textSize) {
        distanceHomeUnitTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the distance from home unit text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceHomeUnitTextBackground() {
        return distanceHomeUnitTextView.getBackground();
    }

    /**
     * Set the background for the distance from home unit text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceHomeUnitTextBackground(@Nullable Drawable drawable) {
        distanceHomeUnitTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from home unit text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceHomeUnitTextBackground(@DrawableRes int resourceId) {
        distanceHomeTitleTextView.setBackgroundResource(resourceId);
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DistanceHomeWidget);
        int distanceHomeTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeTitleTextAppearance,
                        INVALID_RESOURCE);
        if (distanceHomeTitleTextAppearanceId != INVALID_RESOURCE) {
            setDistanceHomeTitleTextAppearance(distanceHomeTitleTextAppearanceId);
        }

        float distanceHomeTitleTextSize =
                typedArray.getDimension(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeTitleTextSize, INVALID_RESOURCE);
        if (distanceHomeTitleTextSize != INVALID_RESOURCE) {
            setDistanceHomeTitleTextSize(DisplayUtil.pxToSp(context, distanceHomeTitleTextSize));
        }

        int distanceHomeTitleTextColor =
                typedArray.getColor(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeTitleTextColor, INVALID_COLOR);
        if (distanceHomeTitleTextColor != INVALID_COLOR) {
            setDistanceHomeTitleTextColor(distanceHomeTitleTextColor);
        }

        Drawable distanceHomeTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeTitleBackgroundDrawable);
        if (distanceHomeTitleTextBackgroundDrawable != null) {
            setDistanceHomeTitleTextBackground(distanceHomeTitleTextBackgroundDrawable);
        }

        int distanceHomeValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeValueTextAppearance,
                        INVALID_RESOURCE);
        if (distanceHomeValueTextAppearanceId != INVALID_RESOURCE) {
            setDistanceHomeValueTextAppearance(distanceHomeValueTextAppearanceId);
        }

        float distanceHomeValueTextSize =
                typedArray.getDimension(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeValueTextSize, INVALID_RESOURCE);
        if (distanceHomeValueTextSize != INVALID_RESOURCE) {
            setDistanceHomeValueTextSize(DisplayUtil.pxToSp(context, distanceHomeValueTextSize));
        }

        int distanceHomeValueTextColor =
                typedArray.getColor(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeValueTextColor, INVALID_COLOR);
        if (distanceHomeValueTextColor != INVALID_COLOR) {
            setDistanceHomeValueTextColor(distanceHomeValueTextColor);
        }

        Drawable distanceHomeValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeValueBackgroundDrawable);
        if (distanceHomeValueTextBackgroundDrawable != null) {
            setDistanceHomeValueTextBackground(distanceHomeValueTextBackgroundDrawable);
        }

        int distanceHomeUnitTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeUnitTextAppearance,
                        INVALID_RESOURCE);
        if (distanceHomeUnitTextAppearanceId != INVALID_RESOURCE) {
            setDistanceHomeUnitTextAppearance(distanceHomeUnitTextAppearanceId);
        }

        float distanceHomeUnitTextSize =
                typedArray.getDimension(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeUnitTextSize, INVALID_RESOURCE);
        if (distanceHomeUnitTextSize != INVALID_RESOURCE) {
            setDistanceHomeUnitTextSize(DisplayUtil.pxToSp(context, distanceHomeUnitTextSize));
        }

        int distanceHomeUnitTextColor =
                typedArray.getColor(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeUnitTextColor, INVALID_COLOR);
        if (distanceHomeUnitTextColor != INVALID_COLOR) {
            setDistanceHomeUnitTextColor(distanceHomeUnitTextColor);
        }

        Drawable distanceHomeUnitTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceHomeWidget_uxsdk_distanceHomeUnitBackgroundDrawable);
        if (distanceHomeUnitTextBackgroundDrawable != null) {
            setDistanceHomeUnitTextBackground(distanceHomeUnitTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
