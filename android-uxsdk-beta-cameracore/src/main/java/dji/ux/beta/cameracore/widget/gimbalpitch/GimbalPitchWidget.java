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

package dji.ux.beta.cameracore.widget.gimbalpitch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.FrameLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_COLOR;
import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

public class GimbalPitchWidget extends FrameLayoutWidget {

    //region Constants
    private static final float INDICATOR_SIZE_MULTIPLIER = 2.3f;
    private static final int DEFAULT_LEVELS_COUNT = 12;
    private static final int DEFAULT_HORIZONTAL_LEVEL = 3;
    private static final int INDICATOR_END_PITCH_RANGE = 5;
    private static final int INDICATOR_HORIZONTAL_PITCH_RANGE = 5;
    private static final float LABEL_PADDING_PERCENTAGE = 0.15f;
    private static final float ALPHA_ENABLED = 1f;
    private static final float ALPHA_DISABLED = 0f;
    private static final int LEVEL_STEP_SIZE = 10;
    private static final int ANIMATION_DURATION = 300;
    private static final float ALPHA_FADE = 0.3f;
    private static final int CIRCLE_PADDING = 2;
    //endregion
    //region Fields
    private ImageView indicatorImageView;
    private TextView labelTextView;
    private GimbalPitchWidgetModel widgetModel;
    private Paint levelsPaint;
    private Paint positivePitchBackgroundColor;
    @ColorInt
    private int linesColor;
    @ColorInt
    private int horizontalPitchLineColor;
    private int absoluteRange;
    private int levelsCount;
    private int horizontalLevel;
    private boolean isLabelEnabled;
    private boolean isAnimating;
    @ColorInt
    private int indicatorEndColor;
    @ColorInt
    private int indicatorNormalColor;
    @ColorInt
    private int indicatorHorizontalColor;
    //endregion

    //region Constructor
    public GimbalPitchWidget(@NonNull Context context) {
        super(context);
    }

    public GimbalPitchWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GimbalPitchWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_gimbal_pitch, this);
        setWillNotDraw(false);
        levelsCount = DEFAULT_LEVELS_COUNT;
        absoluteRange = DEFAULT_LEVELS_COUNT * 10;
        horizontalLevel = DEFAULT_HORIZONTAL_LEVEL;
        labelTextView = findViewById(R.id.text_view_gimbal_pitch_label);
        indicatorImageView = findViewById(R.id.image_view_gimbal_pitch_indicator);

        if (!isInEditMode()) {
            widgetModel =
                new GimbalPitchWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }

        initBackground();
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(
                widgetModel.getPitchState()
                        .doOnNext(this::onStateChanged)
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateUI));
        addReaction(
                widgetModel.shouldFade()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateAlpha)
        );
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_gimbal_pitch_ratio);
    }

    /**
     * Enable/disable the pitch label.
     *
     * @param isEnabled true - label is visible, false - label is hidden.
     */
    public void setGimbalPitchLabelEnabled(boolean isEnabled) {
        this.isLabelEnabled = isEnabled;
        if (isEnabled) {
            labelTextView.setVisibility(View.VISIBLE);
        } else {
            labelTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Set the gimbal key index for which this model should subscribe to.
     *
     * @param gimbalIndex index of the gimbal.
     */
    public void setGimbalIndex(@NonNull GimbalIndex gimbalIndex) {
        if (!isInEditMode()) {
            widgetModel.setGimbalIndex(gimbalIndex.getIndex());
        }
    }


    public void setLabelTextAppearance(@StyleRes int textAppearanceResId) {
        labelTextView.setTextAppearance(getContext(), textAppearanceResId);
    }

    public void setLabelTextSize(float textSize) {
        labelTextView.setTextSize(textSize);
    }

    public float getLabelTextSize() {
        return labelTextView.getTextSize();
    }

    public void setLabelTextColor(@ColorInt int textColor) {
        labelTextView.setTextColor(textColor);
    }

    public int getLabelTextColor() {
        return labelTextView.getCurrentTextColor();
    }

    public void setLabelTextColor(@NonNull ColorStateList colorStateList) {
        labelTextView.setTextColor(colorStateList);
    }

    public ColorStateList getLabelTextColors() {
        return labelTextView.getTextColors();
    }

    public void setLabelTextBackground(@DrawableRes int resourceId) {
        labelTextView.setBackgroundResource(resourceId);
    }

    public void setLabelTextBackground(@NonNull Drawable drawable) {
        labelTextView.setBackground(drawable);
    }

    public Drawable getLabelTextBackground() {
        return labelTextView.getBackground();
    }

    public void setPositivePitchBackgroundColor(@ColorInt int color) {
        positivePitchBackgroundColor.setColor(color);
        invalidate();
    }

    public int getPositivePitchBackgroundColor() {
        return positivePitchBackgroundColor.getColor();
    }

    public void setLinesColor(@ColorInt int color) {
        linesColor = color;
        invalidate();
    }

    public int getLinesColor() {
        return linesColor;
    }

    public void setHorizontalPitchLineColor(@ColorInt int color) {
        horizontalPitchLineColor = color;
        invalidate();
    }

    public int getHorizontalPitchLineColor() {
        return horizontalPitchLineColor;
    }

    public void setIndicatorEndColor(@ColorInt int indicatorEndColor) {
        this.indicatorEndColor = indicatorEndColor;
    }

    public int getIndicatorEndColor() {
        return indicatorEndColor;
    }

    public void setIndicatorNormalColor(@ColorInt int indicatorNormalColor) {
        this.indicatorNormalColor = indicatorNormalColor;
    }

    public int getIndicatorNormalColor() {
        return indicatorNormalColor;
    }

    public void setIndicatorHorizontalColor(@ColorInt int indicatorHorizontalColor) {
        this.indicatorHorizontalColor = indicatorHorizontalColor;
    }

    public int getIndicatorHorizontalColor() {
        return indicatorHorizontalColor;
    }

    //endregion

    //region Reactions to model
    private void onStateChanged(GimbalPitchWidgetModel.PitchState pitchState) {
        absoluteRange = Math.abs(pitchState.getMaxRange()) + Math.abs(pitchState.getMinRange());
        levelsCount = absoluteRange / LEVEL_STEP_SIZE;
        horizontalLevel = pitchState.getMaxRange() / LEVEL_STEP_SIZE;
        if (horizontalLevel < 0) {
            horizontalLevel = 0;
        }
    }

    private void updateUI(GimbalPitchWidgetModel.PitchState pitchState) {
        IndicatorState indicatorState = generateIndicatorState(pitchState);
        String pitchLabelText = generateLabelText(pitchState);
        float pitchLabelY = calculateLabelY(pitchState);
        float indicatorY = calculateIndicatorY(pitchState);
        int indicatorColor = getIndicatorColor(indicatorState);
        float thumbAlpha = calculateIndicatorAlpha(indicatorState);

        indicatorImageView.setColorFilter(indicatorColor);
        indicatorImageView.setAlpha(thumbAlpha);
        indicatorImageView.setTranslationY(indicatorY);
        labelTextView.setText(pitchLabelText);
        labelTextView.setTranslationY(pitchLabelY);
    }

    private void updateAlpha(boolean shouldFade) {
        if (shouldFade) {
            animateAlphaFade(ALPHA_ENABLED, ALPHA_FADE);
        } else {
            animateAlphaFade(ALPHA_FADE, ALPHA_ENABLED);
        }
    }
    //endregion

    //region Helpers
    private String generateLabelText(GimbalPitchWidgetModel.PitchState pitchState) {
        return String.valueOf(Math.round(pitchState.getPitch()));
    }


    private int normalizeGimbalPitch(GimbalPitchWidgetModel.PitchState pitchState) {
        int progress = pitchState.getMaxRange() - Math.round(pitchState.getPitch());

        if (progress < 0) {
            progress = 0;
        } else if (progress > absoluteRange) {
            progress = absoluteRange;
        }

        return progress;
    }

    private IndicatorState generateIndicatorState(GimbalPitchWidgetModel.PitchState pitchState) {
        int gimbalPitch = pitchState.getPitch();
        if (gimbalPitch + INDICATOR_END_PITCH_RANGE >= pitchState.getMaxRange()
                || gimbalPitch - INDICATOR_END_PITCH_RANGE <= pitchState.getMinRange()) {
            return IndicatorState.END;
        } else if (gimbalPitch <= INDICATOR_HORIZONTAL_PITCH_RANGE && gimbalPitch >= INDICATOR_HORIZONTAL_PITCH_RANGE * -1) {
            return IndicatorState.HORIZONTAL;
        } else {
            return IndicatorState.NORMAL;
        }
    }

    private float calculateLabelY(GimbalPitchWidgetModel.PitchState pitchState) {
        int normalizedGimbalPitch = normalizeGimbalPitch(pitchState);
        int labelLevelPadding = (int) (LABEL_PADDING_PERCENTAGE * absoluteRange);
        if (normalizedGimbalPitch < labelLevelPadding) {
            normalizedGimbalPitch = labelLevelPadding;
        } else if (normalizedGimbalPitch > absoluteRange - labelLevelPadding) {
            normalizedGimbalPitch = absoluteRange - labelLevelPadding;
        }

        return (calculateY(normalizedGimbalPitch) - getLabelHeight() / 2);
    }

    private float calculateIndicatorY(GimbalPitchWidgetModel.PitchState pitchState) {
        int widgetHeight = getAdjustedHeight();
        if (widgetHeight == 0) {
            return 0;
        }

        int normalizedGimbalPitch = normalizeGimbalPitch(pitchState);
        return calculateY(
                normalizedGimbalPitch) * ((widgetHeight - getIndicatorHeight()) / (float) widgetHeight);
    }

    private float calculateY(int normalizedGimbalPitch) {
        if (absoluteRange == 0 || getAdjustedHeight() == 0) {
            return 0;
        }
        return (normalizedGimbalPitch * 1f / absoluteRange * getAdjustedHeight());
    }

    private float getLabelHeight() {
        return labelTextView.getHeight();
    }

    private float getIndicatorHeight() {
        return indicatorImageView.getHeight();
    }

    private int getIndicatorColor(IndicatorState indicatorState) {
        switch (indicatorState) {
            case END:
                return indicatorEndColor;
            case HORIZONTAL:
                return indicatorHorizontalColor;
            case NORMAL:
            default:
                return indicatorNormalColor;
        }
    }

    private float calculateIndicatorAlpha(IndicatorState indicatorState) {
        switch (indicatorState) {
            case END:
                return ALPHA_ENABLED;
            case HORIZONTAL:
            case NORMAL:
            default:
                if (isLabelEnabled) {
                    return ALPHA_DISABLED;
                } else {
                    return ALPHA_ENABLED;
                }
        }
    }

    private void animateAlphaFade(float fromAlpha, float toAlpha) {
        if (!isAnimating && getAlpha() != toAlpha) {
            isAnimating = true;
            setAlpha(fromAlpha);
            animate().alpha(toAlpha)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isAnimating = false;
                        }
                    });
        }
    }
    //endregion

    //region Background drawing
    private int getAdjustedHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private void initBackground() {
        levelsPaint = new Paint();
        levelsPaint.setStrokeWidth(4);
        levelsPaint.setStyle(Paint.Style.STROKE);
        levelsPaint.setAntiAlias(true);

        positivePitchBackgroundColor = new Paint();
        positivePitchBackgroundColor.setStrokeWidth(4);
        positivePitchBackgroundColor.setStyle(Paint.Style.FILL);
        positivePitchBackgroundColor.setAntiAlias(true);
        positivePitchBackgroundColor.setAlpha(100);
    }


    private void drawBackground(Canvas canvas) {
        final int height = getAdjustedHeight();
        final int width = getWidth();
        if (height == 0 || width == 0 || levelsCount == 0) {
            return;
        }

        final float radius = (height / levelsCount) / 4;
        final float topCircleCenterY = radius + CIRCLE_PADDING + getPaddingTop();
        final float bottomCircleCenterY = radius + CIRCLE_PADDING + getPaddingBottom();
        final float levelHeight = (getHeight() - topCircleCenterY - bottomCircleCenterY) / levelsCount;
        final float linePadding = (width - (radius * 2)) / 2;
        // Draw background
        if (horizontalLevel > 0) {
            canvas.drawRect(new RectF(
                            linePadding,
                            radius + getPaddingTop(),
                            width - linePadding,
                            levelHeight * horizontalLevel + radius + getPaddingTop()),
                    positivePitchBackgroundColor);
            canvas.drawCircle(width / 2, topCircleCenterY, radius, positivePitchBackgroundColor);
        }
        // Draw circles
        Paint circlePaint = getLinesPaint();
        canvas.drawCircle(width / 2, topCircleCenterY, radius, circlePaint);
        canvas.drawCircle(width / 2, getHeight() - bottomCircleCenterY, radius, circlePaint);
        // Draw lines
        float currentLevel = levelHeight + topCircleCenterY;
        for (int i = 0; currentLevel <= topCircleCenterY + height - levelHeight; currentLevel += levelHeight, i++) {
            Paint paint = getLinesPaint();
            if (i + 1 == horizontalLevel) {
                paint = getHorizontalPitchLinePaint();
            }
            canvas.drawLine(
                    0 + linePadding,
                    currentLevel,
                    width - linePadding,
                    currentLevel,
                    paint
            );
        }

        updateIndicatorSize((int) (radius * INDICATOR_SIZE_MULTIPLIER));
    }

    private Paint getLinesPaint() {
        levelsPaint.setColor(linesColor);
        return levelsPaint;
    }

    private Paint getHorizontalPitchLinePaint() {
        levelsPaint.setColor(horizontalPitchLineColor);
        return levelsPaint;
    }

    private void updateIndicatorSize(int indicatorSize) {
        if(indicatorImageView.getWidth() != indicatorSize) {
            // width has changed
            indicatorImageView.getLayoutParams().height = indicatorSize;
            indicatorImageView.getLayoutParams().width = indicatorSize;
            indicatorImageView.requestLayout();
        }
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        // R.style.GimbalPitchWidgetDefault defines all the default attributes for this widget
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GimbalPitchWidget, 0, R.style.UXSDKGimbalPitchWidgetDefault);

        int index = typedArray.getInt(R.styleable.GimbalPitchWidget_uxsdk_gimbalIndex, INVALID_RESOURCE);
        GimbalIndex gimbalIndex = GimbalIndex.find(index);
        if (index != INVALID_RESOURCE && gimbalIndex != null) {
            setGimbalIndex(gimbalIndex);
        }

        int textAppearanceId = typedArray.getResourceId(R.styleable.GimbalPitchWidget_uxsdk_labelsTextAppearance, INVALID_RESOURCE);
        if (textAppearanceId != INVALID_RESOURCE) {
            setLabelTextAppearance(textAppearanceId);
        }

        float textSize = typedArray.getDimension(R.styleable.GimbalPitchWidget_uxsdk_labelsTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setLabelTextSize(DisplayUtil.pxToSp(context, textSize));
        }

        int textColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_labelsTextColor, INVALID_COLOR);
        if (textColor != INVALID_COLOR) {
            setLabelTextColor(textColor);
        }

        Drawable textBackgroundDrawable = typedArray.getDrawable(R.styleable.GimbalPitchWidget_uxsdk_labelsBackground);
        if (textBackgroundDrawable != null) {
            setLabelTextBackground(textBackgroundDrawable);
        }

        int positivePitchBackgroundColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_positivePitchBackgroundColor, INVALID_COLOR);
        if (positivePitchBackgroundColor != INVALID_COLOR) {
            setPositivePitchBackgroundColor(positivePitchBackgroundColor);
        }

        int linesColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_linesColor, INVALID_COLOR);
        if (linesColor != INVALID_COLOR) {
            setLinesColor(linesColor);
        }

        int horizontalPitchLineColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_horizontalPitchLineColor, INVALID_COLOR);
        if (horizontalPitchLineColor != INVALID_COLOR) {
            setHorizontalPitchLineColor(horizontalPitchLineColor);
        }

        int indicatorEndColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_indicatorEndColor, INVALID_COLOR);
        if (indicatorEndColor != INVALID_COLOR) {
            setIndicatorEndColor(indicatorEndColor);
            indicatorImageView.setColorFilter(indicatorEndColor);
        }

        int indicatorNormalColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_indicatorNormalColor, INVALID_COLOR);
        if (indicatorNormalColor != INVALID_COLOR) {
            setIndicatorNormalColor(indicatorNormalColor);
        }

        int indicatorHorizontalColor = typedArray.getColor(R.styleable.GimbalPitchWidget_uxsdk_indicatorHorizontalColor, INVALID_COLOR);
        if (indicatorHorizontalColor != INVALID_COLOR) {
            setIndicatorHorizontalColor(indicatorHorizontalColor);
        }

        boolean labelEnabled = typedArray.getBoolean(R.styleable.GimbalPitchWidget_uxsdk_labelEnabled, true);
        setGimbalPitchLabelEnabled(labelEnabled);

        typedArray.recycle();
    }
    //endregion

    private enum IndicatorState {
        END,
        HORIZONTAL,
        NORMAL
    }


}
