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

package dji.ux.beta.widget.altitude;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.ux.beta.R;
import dji.ux.beta.base.ConstraintLayoutWidget;
import dji.ux.beta.base.DJISDKModel;
import dji.ux.beta.base.GlobalPreferencesManager;
import dji.ux.beta.base.uxsdkkeys.ObservableInMemoryKeyedStore;
import dji.ux.beta.util.DisplayUtil;
import dji.ux.beta.util.UnitConversionUtil;

import java.text.DecimalFormat;

/**
 * Shows the current altitude of the aircraft.
 * Uses the unit set in the UNIT_TYPE global preferences
 * {@link dji.ux.beta.base.GlobalPreferencesInterface#getUnitType()} and the
 * {@link dji.ux.beta.base.uxsdkkeys.GlobalPreferenceKeys#UNIT_TYPE} UX Key
 * and defaults to meters.
 */
public class AltitudeWidget extends ConstraintLayoutWidget {
    //region Fields
    private static final int EMS = 3;
    private static DecimalFormat decimalFormat = new DecimalFormat("##0.0");
    private TextView altitudeTitleTextView;
    private TextView altitudeValueTextView;
    private TextView altitudeUnitTextView;
    private AltitudeWidgetModel widgetModel;
    //endregion

    //region Constructors
    public AltitudeWidget(Context context) {
        super(context);
    }

    public AltitudeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AltitudeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_dashboard_text_only, this);
        altitudeTitleTextView = findViewById(R.id.textview_title);
        altitudeValueTextView = findViewById(R.id.textview_value);
        altitudeUnitTextView = findViewById(R.id.textview_unit);

        if (!isInEditMode()) {
            widgetModel = new AltitudeWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
            altitudeTitleTextView.setText(getResources().getString(R.string.uxsdk_altitude_title));
            altitudeValueTextView.setMinEms(EMS);
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
        addReaction(widgetModel.getAltitude()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateValueText));
        addReaction(widgetModel.getUnitType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUnitText));
    }
    //endregion

    //region Reactions to model
    private void updateValueText(float altitude) {
        altitudeValueTextView.setText(decimalFormat.format(altitude));
    }

    private void updateUnitText(UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            altitudeUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_feet));
        } else {
            altitudeUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_meters));
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
     * Set text appearance of the altitude title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setAltitudeTitleTextAppearance(@StyleRes int textAppearance) {
        altitudeTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the altitude title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setAltitudeTitleTextColor(@NonNull ColorStateList colorStateList) {
        altitudeTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the altitude title text view
     *
     * @param color color integer resource
     */
    public void setAltitudeTitleTextColor(@ColorInt int color) {
        altitudeTitleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the altitude title text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getAltitudeTitleTextColors() {
        return altitudeTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the altitude title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getAltitudeTitleTextColor() {
        return altitudeTitleTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the altitude title text view
     *
     * @param textSize text size float value
     */
    public void setAltitudeTitleTextSize(@Dimension float textSize) {
        altitudeTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the altitude title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getAltitudeTitleTextSize() {
        return altitudeTitleTextView.getTextSize();
    }

    /**
     * Set the background of the altitude title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setAltitudeTitleTextBackground(@Nullable Drawable drawable) {
        altitudeTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the altitude title text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setAltitudeTitleTextBackground(@DrawableRes int resourceId) {
        altitudeTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the altitude title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getAltitudeTitleTextBackground() {
        return altitudeTitleTextView.getBackground();
    }

    /**
     * Set text appearance of the altitude value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setAltitudeValueTextAppearance(@StyleRes int textAppearance) {
        altitudeValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the altitude value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setAltitudeValueTextColor(@NonNull ColorStateList colorStateList) {
        altitudeValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the altitude value text view
     *
     * @param color color integer resource
     */
    public void setAltitudeValueTextColor(@ColorInt int color) {
        altitudeValueTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the altitude value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getAltitudeValueTextColors() {
        return altitudeValueTextView.getTextColors();
    }

    /**
     * Get current text color of the altitude value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getAltitudeValueTextColor() {
        return altitudeValueTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the altitude value text view
     *
     * @param textSize text size float value
     */
    public void setAltitudeValueTextSize(@Dimension float textSize) {
        altitudeValueTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the altitude value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getAltitudeValueTextSize() {
        return altitudeValueTextView.getTextSize();
    }

    /**
     * Set the background for the altitude value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setAltitudeValueTextBackground(@Nullable Drawable drawable) {
        altitudeValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the altitude value text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setAltitudeValueTextBackground(@DrawableRes int resourceId) {
        altitudeValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the altitude value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getAltitudeValueTextBackground() {
        return altitudeValueTextView.getBackground();
    }

    /**
     * Set text appearance of the altitude unit text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setAltitudeUnitTextAppearance(@StyleRes int textAppearance) {
        altitudeUnitTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the altitude unit text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setAltitudeUnitTextColor(@NonNull ColorStateList colorStateList) {
        altitudeUnitTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the altitude unit text view
     *
     * @param color color integer resource
     */
    public void setAltitudeUnitTextColor(@ColorInt int color) {
        altitudeUnitTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the altitude unit text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getAltitudeUnitTextColors() {
        return altitudeUnitTextView.getTextColors();
    }

    /**
     * Get current text color of the altitude unit text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getAltitudeUnitTextColor() {
        return altitudeUnitTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the altitude unit text view
     *
     * @param textSize text size float value
     */
    public void setAltitudeUnitTextSize(@Dimension float textSize) {
        altitudeUnitTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the altitude unit text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getAltitudeUnitTextSize() {
        return altitudeUnitTextView.getTextSize();
    }

    /**
     * Set the background for the altitude unit text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setAltitudeUnitTextBackground(@Nullable Drawable drawable) {
        altitudeUnitTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the altitude unit text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setAltitudeUnitTextBackground(@DrawableRes int resourceId) {
        altitudeUnitTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the altitude unit text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getAltitudeUnitTextBackground() {
        return altitudeUnitTextView.getBackground();
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AltitudeWidget);
        int altitudeTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.AltitudeWidget_uxsdk_altitudeTitleTextAppearance, INVALID_RESOURCE);
        if (altitudeTitleTextAppearanceId != INVALID_RESOURCE) {
            setAltitudeTitleTextAppearance(altitudeTitleTextAppearanceId);
        }

        float altitudeTitleTextSize =
                typedArray.getDimension(R.styleable.AltitudeWidget_uxsdk_altitudeTitleTextSize, INVALID_RESOURCE);
        if (altitudeTitleTextSize != INVALID_RESOURCE) {
            setAltitudeTitleTextSize(DisplayUtil.pxToSp(context, altitudeTitleTextSize));
        }

        int altitudeTitleTextColor =
                typedArray.getColor(R.styleable.AltitudeWidget_uxsdk_altitudeTitleTextColor, INVALID_COLOR);
        if (altitudeTitleTextColor != INVALID_COLOR) {
            setAltitudeTitleTextColor(altitudeTitleTextColor);
        }

        Drawable altitudeTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.AltitudeWidget_uxsdk_altitudeTitleBackgroundDrawable);
        if (altitudeTitleTextBackgroundDrawable != null) {
            setAltitudeTitleTextBackground(altitudeTitleTextBackgroundDrawable);
        }

        int altitudeValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.AltitudeWidget_uxsdk_altitudeValueTextAppearance, INVALID_RESOURCE);
        if (altitudeValueTextAppearanceId != INVALID_RESOURCE) {
            setAltitudeValueTextAppearance(altitudeValueTextAppearanceId);
        }

        float altitudeValueTextSize =
                typedArray.getDimension(R.styleable.AltitudeWidget_uxsdk_altitudeValueTextSize, INVALID_RESOURCE);
        if (altitudeValueTextSize != INVALID_RESOURCE) {
            setAltitudeValueTextSize(DisplayUtil.pxToSp(context, altitudeValueTextSize));
        }

        int altitudeValueTextColor =
                typedArray.getColor(R.styleable.AltitudeWidget_uxsdk_altitudeValueTextColor, INVALID_COLOR);
        if (altitudeValueTextColor != INVALID_COLOR) {
            setAltitudeValueTextColor(altitudeValueTextColor);
        }

        Drawable altitudeValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.AltitudeWidget_uxsdk_altitudeValueBackgroundDrawable);
        if (altitudeValueTextBackgroundDrawable != null) {
            setAltitudeValueTextBackground(altitudeValueTextBackgroundDrawable);
        }

        int altitudeUnitTextAppearanceId =
                typedArray.getResourceId(R.styleable.AltitudeWidget_uxsdk_altitudeUnitTextAppearance, INVALID_RESOURCE);
        if (altitudeUnitTextAppearanceId != INVALID_RESOURCE) {
            setAltitudeUnitTextAppearance(altitudeUnitTextAppearanceId);
        }

        float altitudeUnitTextSize =
                typedArray.getDimension(R.styleable.AltitudeWidget_uxsdk_altitudeUnitTextSize, INVALID_RESOURCE);
        if (altitudeUnitTextSize != INVALID_RESOURCE) {
            setAltitudeUnitTextSize(DisplayUtil.pxToSp(context, altitudeUnitTextSize));
        }

        int altitudeUnitTextColor =
                typedArray.getColor(R.styleable.AltitudeWidget_uxsdk_altitudeUnitTextColor, INVALID_COLOR);
        if (altitudeUnitTextColor != INVALID_COLOR) {
            setAltitudeUnitTextColor(altitudeUnitTextColor);
        }

        Drawable altitudeUnitTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.AltitudeWidget_uxsdk_altitudeUnitBackgroundDrawable);
        if (altitudeUnitTextBackgroundDrawable != null) {
            setAltitudeUnitTextBackground(altitudeUnitTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}