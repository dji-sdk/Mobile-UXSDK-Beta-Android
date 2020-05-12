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
 */

package dji.ux.beta.core.widget.distancerc;

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
 * and the RC (pilot).
 * Uses the unit set in the UNIT_TYPE global preferences
 * {@link GlobalPreferencesInterface#getUnitType()} and the
 * {@link GlobalPreferenceKeys#UNIT_TYPE} UX Key
 * and defaults to meters.
 */
public class DistanceRCWidget extends ConstraintLayoutWidget {
    //region Fields
    private static final int EMS = 3;
    private static DecimalFormat decimalFormat = new DecimalFormat("###0.0");
    private TextView distanceRCTitleTextView;
    private TextView distanceRCValueTextView;
    private TextView distanceRCUnitTextView;
    private DistanceRCWidgetModel widgetModel;
    //endregion

    //region Constructors
    public DistanceRCWidget(Context context) {
        super(context);
    }

    public DistanceRCWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DistanceRCWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_dashboard_text_only, this);
        distanceRCTitleTextView = findViewById(R.id.textview_title);
        distanceRCValueTextView = findViewById(R.id.textview_value);
        distanceRCUnitTextView = findViewById(R.id.textview_unit);

        if (!isInEditMode()) {
            widgetModel = new DistanceRCWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
            distanceRCTitleTextView.setText(getResources().getString(R.string.uxsdk_distance_rc_title));
            distanceRCValueTextView.setMinEms(EMS);
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
        addReaction(widgetModel.getDistanceFromRC()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateValueText));
        addReaction(widgetModel.getUnitType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUnitText));
    }
    //endregion

    //region Reactions to model
    private void updateValueText(@FloatRange(from = 0.0f) float distanceFromRC) {
        distanceRCValueTextView.setText(decimalFormat.format(distanceFromRC));
    }

    private void updateUnitText(UnitConversionUtil.UnitType unitType) {
        if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            distanceRCUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_feet));
        } else {
            distanceRCUnitTextView.setText(getResources().getString(R.string.uxsdk_unit_meters));
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
     * Set text appearance of the distance from RC title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceRCTitleTextAppearance(@StyleRes int textAppearance) {
        distanceRCTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the distance from RC title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceRCTitleTextColor(@NonNull ColorStateList colorStateList) {
        distanceRCTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from RC title text view
     *
     * @param color color integer resource
     */
    public void setDistanceRCTitleTextColor(@ColorInt int color) {
        distanceRCTitleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the distance from RC title text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceRCTitleTextColors() {
        return distanceRCTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from RC title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceRCTitleTextColor() {
        return distanceRCTitleTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the distance from RC title text view
     *
     * @param textSize text size float value
     */
    public void setDistanceRCTitleTextSize(@Dimension float textSize) {
        distanceRCTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the distance from RC title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceRCTitleTextSize() {
        return distanceRCTitleTextView.getTextSize();
    }

    /**
     * Set the background of the distance from RC title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceRCTitleTextBackground(@Nullable Drawable drawable) {
        distanceRCTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from RC title text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceRCTitleTextBackground(@DrawableRes int resourceId) {
        distanceRCTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the distance from RC title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceRCTitleTextBackground() {
        return distanceRCTitleTextView.getBackground();
    }

    /**
     * Set text appearance of the distance from RC value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceRCValueTextAppearance(@StyleRes int textAppearance) {
        distanceRCValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the distance from RC value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceRCValueTextColor(@NonNull ColorStateList colorStateList) {
        distanceRCValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from RC value text view
     *
     * @param color color integer resource
     */
    public void setDistanceRCValueTextColor(@ColorInt int color) {
        distanceRCValueTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the distance from RC value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceRCValueTextColors() {
        return distanceRCValueTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from RC value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceRCValueTextColor() {
        return distanceRCValueTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the distance from RC value text view
     *
     * @param textSize text size float value
     */
    public void setDistanceRCValueTextSize(@Dimension float textSize) {
        distanceRCValueTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the distance from RC value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceRCValueTextSize() {
        return distanceRCValueTextView.getTextSize();
    }

    /**
     * Set the background for the distance from RC value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceRCValueTextBackground(@Nullable Drawable drawable) {
        distanceRCValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from RC value text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceRCValueTextBackground(@DrawableRes int resourceId) {
        distanceRCValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the distance from RC value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceRCValueTextBackground() {
        return distanceRCValueTextView.getBackground();
    }

    /**
     * Set text appearance of the distance from RC unit text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setDistanceRCUnitTextAppearance(@StyleRes int textAppearance) {
        distanceRCUnitTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set text color state list for the distance from RC  unit text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setDistanceRCUnitTextColor(@NonNull ColorStateList colorStateList) {
        distanceRCUnitTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the distance from RC unit text view
     *
     * @param color color integer resource
     */
    public void setDistanceRCUnitTextColor(@ColorInt int color) {
        distanceRCUnitTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of the distance from RC unit text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getDistanceRCUnitTextColors() {
        return distanceRCUnitTextView.getTextColors();
    }

    /**
     * Get current text color of the distance from RC unit text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getDistanceRCUnitTextColor() {
        return distanceRCUnitTextView.getCurrentTextColor();
    }

    /**
     * Set the text size of the distance from RC unit text view
     *
     * @param textSize text size float value
     */
    public void setDistanceRCUnitTextSize(@Dimension float textSize) {
        distanceRCUnitTextView.setTextSize(textSize);
    }

    /**
     * Get current text size of the distance from RC unit text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getDistanceRCUnitTextSize() {
        return distanceRCUnitTextView.getTextSize();
    }

    /**
     * Set the background for the distance from RC unit text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setDistanceRCUnitTextBackground(@Nullable Drawable drawable) {
        distanceRCUnitTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the distance from RC unit text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    public void setDistanceRCUnitTextBackground(@DrawableRes int resourceId) {
        distanceRCUnitTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get current background of the distance from RC unit text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getDistanceRCUnitTextBackground() {
        return distanceRCUnitTextView.getBackground();
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DistanceRCWidget);
        int distanceRCTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceRCWidget_uxsdk_distanceRCTitleTextAppearance,
                        INVALID_RESOURCE);
        if (distanceRCTitleTextAppearanceId != INVALID_RESOURCE) {
            setDistanceRCTitleTextAppearance(distanceRCTitleTextAppearanceId);
        }

        float distanceRCTitleTextSize =
                typedArray.getDimension(R.styleable.DistanceRCWidget_uxsdk_distanceRCTitleTextSize, INVALID_RESOURCE);
        if (distanceRCTitleTextSize != INVALID_RESOURCE) {
            setDistanceRCTitleTextSize(DisplayUtil.pxToSp(context, distanceRCTitleTextSize));
        }

        int distanceRCTitleTextColor =
                typedArray.getColor(R.styleable.DistanceRCWidget_uxsdk_distanceRCTitleTextColor, INVALID_COLOR);
        if (distanceRCTitleTextColor != INVALID_COLOR) {
            setDistanceRCTitleTextColor(distanceRCTitleTextColor);
        }

        Drawable distanceRCTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceRCWidget_uxsdk_distanceRCTitleBackgroundDrawable);
        if (distanceRCTitleTextBackgroundDrawable != null) {
            setDistanceRCTitleTextBackground(distanceRCTitleTextBackgroundDrawable);
        }

        int distanceRCValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceRCWidget_uxsdk_distanceRCValueTextAppearance,
                        INVALID_RESOURCE);
        if (distanceRCValueTextAppearanceId != INVALID_RESOURCE) {
            setDistanceRCValueTextAppearance(distanceRCValueTextAppearanceId);
        }

        float distanceRCValueTextSize =
                typedArray.getDimension(R.styleable.DistanceRCWidget_uxsdk_distanceRCValueTextSize, INVALID_RESOURCE);
        if (distanceRCValueTextSize != INVALID_RESOURCE) {
            setDistanceRCValueTextSize(DisplayUtil.pxToSp(context, distanceRCValueTextSize));
        }

        int distanceRCValueTextColor =
                typedArray.getColor(R.styleable.DistanceRCWidget_uxsdk_distanceRCValueTextColor, INVALID_COLOR);
        if (distanceRCValueTextColor != INVALID_COLOR) {
            setDistanceRCValueTextColor(distanceRCValueTextColor);
        }

        Drawable distanceRCValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceRCWidget_uxsdk_distanceRCValueBackgroundDrawable);
        if (distanceRCValueTextBackgroundDrawable != null) {
            setDistanceRCValueTextBackground(distanceRCValueTextBackgroundDrawable);
        }

        int distanceRCUnitTextAppearanceId =
                typedArray.getResourceId(R.styleable.DistanceRCWidget_uxsdk_distanceRCUnitTextAppearance,
                        INVALID_RESOURCE);
        if (distanceRCUnitTextAppearanceId != INVALID_RESOURCE) {
            setDistanceRCUnitTextAppearance(distanceRCUnitTextAppearanceId);
        }

        float distanceRCUnitTextSize =
                typedArray.getDimension(R.styleable.DistanceRCWidget_uxsdk_distanceRCUnitTextSize, INVALID_RESOURCE);
        if (distanceRCUnitTextSize != INVALID_RESOURCE) {
            setDistanceRCUnitTextSize(DisplayUtil.pxToSp(context, distanceRCUnitTextSize));
        }

        int distanceRCUnitTextColor =
                typedArray.getColor(R.styleable.DistanceRCWidget_uxsdk_distanceRCUnitTextColor, INVALID_COLOR);
        if (distanceRCUnitTextColor != INVALID_COLOR) {
            setDistanceRCUnitTextColor(distanceRCUnitTextColor);
        }

        Drawable distanceRCUnitTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.DistanceRCWidget_uxsdk_distanceRCUnitBackgroundDrawable);
        if (distanceRCUnitTextBackgroundDrawable != null) {
            setDistanceRCUnitTextBackground(distanceRCUnitTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
