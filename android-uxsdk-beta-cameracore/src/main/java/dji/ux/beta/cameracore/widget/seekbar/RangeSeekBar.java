package dji.ux.beta.cameracore.widget.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import dji.ux.beta.cameracore.R;

import static dji.ux.beta.cameracore.widget.seekbar.SeekBar.INDICATOR_ALWAYS_HIDE;
import static dji.ux.beta.cameracore.widget.seekbar.SeekBar.INDICATOR_ALWAYS_SHOW;

public class RangeSeekBar extends View {

    //normal seekBar mode
    public final static int SEEKBAR_MODE_SINGLE = 1;
    //RangeSeekBar
    public final static int SEEKBAR_MODE_RANGE = 2;


    //number according to the actual proportion of the number of arranged;
    public final static int TRICK_MARK_MODE_NUMBER = 0;
    //other equally arranged
    public final static int TRICK_MARK_MODE_OTHER = 1;


    public static class Gravity {
        public final static int TOP = 0;
        public final static int BOTTOM = 1;
        public final static int CENTER = 2;
    }

    private int progressTop, progressBottom, progressLeft, progressRight;
    private int seekBarMode;

    //The texts displayed on the scale
    //进度条圆角
    //radius of progress bar
    private float progressRadius;
    //进度中进度条的颜色
    //the color of seekBar in progress
    private int progressColor;
    //默认进度条颜色
    //the default color of the progress bar
    private int progressDefaultColor;

    //the drawable of seekBar in progress
    private int progressDrawableId;
    //the default Drawable of the progress bar
    private int progressDefaultDrawableId;

    //the progress height
    private int progressHeight;
    // the progress width
    private int progressWidth;
    //the range interval of RangeSeekBar
    private float minInterval;

    private int gravity;
    //enable RangeSeekBar two thumb Overlap
    private boolean enableThumbOverlap;

    //the color of step divs
    private int stepsColor;
    //the width of each step
    private float stepsWidth;
    //the height of each step
    private float stepsHeight;
    //the radius of step divs
    private float stepsRadius;
    //steps is 0 will disable StepSeekBar
    private int steps;
    //the thumb will automatic bonding close to its value
    private boolean stepsAutoBonding;
    private int stepsDrawableId;
    //True values set by the user
    private float minProgress, maxProgress;
    //****************** the above is attr value  ******************//

    private boolean isEnable = true;
    float touchDownX, touchDownY;
    //剩余最小间隔的进度
    float reservePercent;
    boolean isScaleThumb = false;
    Paint paint = new Paint();
    RectF progressDefaultDstRect = new RectF();
    RectF progressDstRect = new RectF();
    Rect progressSrcRect = new Rect();
    RectF stepDivRect = new RectF();
    SeekBar leftSB;
    SeekBar rightSB;
    SeekBar currTouchSB;
    Bitmap progressBitmap;
    Bitmap progressDefaultBitmap;
    List<Bitmap> stepsBitmaps = new ArrayList<>();
    private int progressPaddingRight;
    private OnRangeChangedListener callback;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initPaint();
        initSeekBar(attrs);
        initStepsBitmap();
    }

    private void initProgressBitmap() {
        if (progressBitmap == null) {
            progressBitmap = SeekBarUtils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDrawableId);
        }
        if (progressDefaultBitmap == null) {
            progressDefaultBitmap = SeekBarUtils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDefaultDrawableId);
        }
    }

    private boolean verifyStepsMode() {
        return steps >= 1 && !(stepsHeight <= 0) && !(stepsWidth <= 0);
    }

    private void initStepsBitmap() {
        if (!verifyStepsMode() || stepsDrawableId == 0) return;
        if (stepsBitmaps.isEmpty()) {
            Bitmap bitmap = SeekBarUtils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableId);
            for (int i = 0; i <= steps; i++) {
                stepsBitmaps.add(bitmap);
            }
        }
    }

    private void initSeekBar(AttributeSet attrs) {
        leftSB = new SeekBar(this, attrs, true);
        rightSB = new SeekBar(this, attrs, false);
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }


    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
            seekBarMode = t.getInt(R.styleable.RangeSeekBar_rsb_mode, SEEKBAR_MODE_RANGE);
            minProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_min, 0);
            maxProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_max, 100);
            minInterval = t.getFloat(R.styleable.RangeSeekBar_rsb_min_interval, 0);
            gravity = t.getInt(R.styleable.RangeSeekBar_rsb_gravity, Gravity.CENTER);
            progressColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_color, 0xFF4BD962);
            progressRadius = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_radius, -1);
            progressDefaultColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_default_color, 0xFFD7D7D7);
            progressDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable, 0);
            progressDefaultDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable_default, 0);
            progressHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_height, SeekBarUtils.dp2px(getContext(), 6f));
            steps = t.getInt(R.styleable.RangeSeekBar_rsb_steps, 0);
            stepsColor = t.getColor(R.styleable.RangeSeekBar_rsb_step_color, 0xFF9d9d9d);
            stepsRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_step_radius, 0);
            stepsWidth = t.getDimension(R.styleable.RangeSeekBar_rsb_step_width, 0);
            stepsHeight = t.getDimension(R.styleable.RangeSeekBar_rsb_step_height, 0);
            stepsDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_step_drawable, 0);
            stepsAutoBonding = t.getBoolean(R.styleable.RangeSeekBar_rsb_step_auto_bonding, true);
            t.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * measure progress bar position
     */
    protected void onMeasureProgress(int w, int h) {
        int viewHeight = h - getPaddingBottom() - getPaddingTop();
        if (h <= 0) return;

        if (gravity == Gravity.TOP) {
            //calculate the height of indicator and thumb exceeds the part of the progress
            float maxIndicatorHeight = 0;
            if (leftSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE
                    || rightSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE) {
                maxIndicatorHeight = Math.max(leftSB.getIndicatorRawHeight(), rightSB.getIndicatorRawHeight());
            }
            float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
            thumbHeight -= progressHeight / 2f;

            //default height is indicator + thumb exceeds the part of the progress bar
            //if tickMark height is greater than (indicator + thumb exceeds the part of the progress)
            progressTop = (int) (maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            progressBottom = progressTop + progressHeight;
        } else if (gravity == Gravity.BOTTOM) {
            progressBottom = (int) (viewHeight - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2f
                    + progressHeight / 2f);
            progressTop = progressBottom - progressHeight;
        } else {
            progressTop = (viewHeight - progressHeight) / 2;
            progressBottom = progressTop + progressHeight;
        }

        int maxThumbWidth = (int) Math.max(leftSB.getThumbScaleWidth(), rightSB.getThumbScaleWidth());
        progressLeft = maxThumbWidth / 2 + getPaddingLeft();
        progressRight = w - maxThumbWidth / 2 - getPaddingRight();
        progressWidth = progressRight - progressLeft;
        progressDefaultDstRect.set(getProgressLeft(), getProgressTop(), getProgressRight(), getProgressBottom());
        progressPaddingRight = w - progressRight;
        //default value
        if (progressRadius <= 0) {
            progressRadius = (int) ((getProgressBottom() - getProgressTop()) * 0.45f);
        }
        initProgressBitmap();
    }

    //Android 7.0以后，优化了View的绘制，onMeasure和onSizeChanged调用顺序有所变化
    //Android7.0以下：onMeasure--->onSizeChanged--->onMeasure
    //Android7.0以上：onMeasure--->onSizeChanged
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST && getParent() instanceof ViewGroup
                && heightSize == ViewGroup.LayoutParams.MATCH_PARENT) {
            heightSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (gravity == Gravity.CENTER) {
                heightNeeded = (int) (2 * (getRawHeight() - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2));
            } else {
                heightNeeded = (int) getRawHeight();
            }
            heightSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSize);
    }

    protected float getRawHeight() {
        float rawHeight;
        if (seekBarMode == SEEKBAR_MODE_SINGLE) {
            rawHeight = leftSB.getRawHeight();
        } else {
            rawHeight = Math.max(leftSB.getRawHeight(), rightSB.getRawHeight());
        }
        return rawHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onMeasureProgress(w, h);
        //set default value
        setRange(minProgress, maxProgress, minInterval);
        int lineCenterY = (getProgressBottom() + getProgressTop()) / 2;
        leftSB.onSizeChanged(getProgressLeft(), lineCenterY);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.onSizeChanged(getProgressLeft(), lineCenterY);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onDrawProgressBar(canvas, paint);
        onDrawSteps(canvas, paint);
        onDrawSeekBar(canvas);
    }


    //绘制进度条
    // draw the progress bar
    protected void onDrawProgressBar(Canvas canvas, Paint paint) {

        //draw default progress
        if (SeekBarUtils.verifyBitmap(progressDefaultBitmap)) {
            canvas.drawBitmap(progressDefaultBitmap, null, progressDefaultDstRect, paint);
        } else {
            paint.setColor(progressDefaultColor);
            canvas.drawRoundRect(progressDefaultDstRect, progressRadius, progressRadius, paint);
        }

        //draw progress
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.right = rightSB.left + rightSB.getThumbScaleWidth() / 2f + progressWidth * rightSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        } else {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f;
            progressDstRect.right = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        }

        if (SeekBarUtils.verifyBitmap(progressBitmap)) {
            progressSrcRect.top = 0;
            progressSrcRect.bottom = progressBitmap.getHeight();
            int bitmapWidth = progressBitmap.getWidth();
            if (seekBarMode == SEEKBAR_MODE_RANGE) {
                progressSrcRect.left = (int) (bitmapWidth * leftSB.currPercent);
                progressSrcRect.right = (int) (bitmapWidth * rightSB.currPercent);
            } else {
                progressSrcRect.left = 0;
                progressSrcRect.right = (int) (bitmapWidth * leftSB.currPercent);
            }
            canvas.drawBitmap(progressBitmap, progressSrcRect, progressDstRect, null);
        } else {
            paint.setColor(progressColor);
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        }

    }

    //draw steps
    protected void onDrawSteps(Canvas canvas, Paint paint) {
        if (!verifyStepsMode()) return;
        int stepMarks = getProgressWidth() / (steps);
        float extHeight = (stepsHeight - getProgressHeight()) / 2f;
        for (int k = 0; k <= steps; k++) {
            float x = getProgressLeft() + k * stepMarks - stepsWidth / 2f;
            stepDivRect.set(x, getProgressTop() - extHeight, x + stepsWidth, getProgressBottom() + extHeight);
            if (stepsBitmaps.isEmpty() || stepsBitmaps.size() <= k) {
                paint.setColor(stepsColor);
                canvas.drawRoundRect(stepDivRect, stepsRadius, stepsRadius, paint);
            } else {
                canvas.drawBitmap(stepsBitmaps.get(k), null, stepDivRect, paint);
            }
        }
    }

    //绘制SeekBar相关
    protected void onDrawSeekBar(Canvas canvas) {
        //draw left SeekBar
        if (leftSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
            leftSB.setShowIndicatorEnable(true);
        }
        leftSB.draw(canvas, 0);
        //draw right SeekBar
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (rightSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
                rightSB.setShowIndicatorEnable(true);
            }
            rightSB.draw(canvas, 1);
        }
    }

    //初始化画笔
    private void initPaint() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(progressDefaultColor);
    }


    private void changeThumbActivateState(boolean hasActivate) {
        if (hasActivate && currTouchSB != null) {
            boolean state = currTouchSB == leftSB;
            leftSB.setActivate(state);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(!state);
        } else {
            leftSB.setActivate(false);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(false);
        }
    }

    protected float getEventX(MotionEvent event) {
        return event.getX();
    }

    protected float getEventY(MotionEvent event) {
        return event.getY();
    }

    /**
     * scale the touch seekBar thumb
     */
    private void scaleCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && !isScaleThumb) {
            isScaleThumb = true;
            currTouchSB.scaleThumb();
        }
    }

    /**
     * reset the touch seekBar thumb
     */
    private void resetCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && isScaleThumb) {
            isScaleThumb = false;
            currTouchSB.resetThumb();
        }
    }

    //calculate currTouchSB percent by MotionEvent
    protected float calculateCurrentSeekBarPercent(float touchDownX) {
        if (currTouchSB == null) return 0;
        float percent = (touchDownX - getProgressLeft()) / (progressWidth);
        if (touchDownX < getProgressLeft()) {
            percent = 0;
        } else if (touchDownX > getProgressRight()) {
            percent = 1;
        }
        //RangeMode minimum interval
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (currTouchSB == leftSB) {
                if (percent > rightSB.currPercent - reservePercent) {
                    percent = rightSB.currPercent - reservePercent;
                }
            } else if (currTouchSB == rightSB) {
                if (percent < leftSB.currPercent + reservePercent) {
                    percent = leftSB.currPercent + reservePercent;
                }
            }
        }
        return percent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = getEventX(event);
                touchDownY = getEventY(event);
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    if (rightSB.currPercent >= 1 && leftSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = leftSB;
                        scaleCurrentSeekBarThumb();
                    } else if (rightSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = rightSB;
                        scaleCurrentSeekBarThumb();
                    } else {
                        float performClick = (touchDownX - getProgressLeft()) / (progressWidth);
                        float distanceLeft = Math.abs(leftSB.currPercent - performClick);
                        float distanceRight = Math.abs(rightSB.currPercent - performClick);
                        if (distanceLeft < distanceRight) {
                            currTouchSB = leftSB;
                        } else {
                            currTouchSB = rightSB;
                        }
                        performClick = calculateCurrentSeekBarPercent(touchDownX);
                        currTouchSB.slide(performClick);
                    }
                } else {
                    currTouchSB = leftSB;
                    scaleCurrentSeekBarThumb();
                }

                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float x = getEventX(event);
                if ((seekBarMode == SEEKBAR_MODE_RANGE) && leftSB.currPercent == rightSB.currPercent) {
                    currTouchSB.materialRestore();
                    if (callback != null) {
                        callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                    }
                    if (x - touchDownX > 0) {
                        //method to move right
                        if (currTouchSB != rightSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = rightSB;
                        }
                    } else {
                        //method to move left
                        if (currTouchSB != leftSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = leftSB;
                        }
                    }
                    if (callback != null) {
                        callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                    }
                }
                scaleCurrentSeekBarThumb();
                currTouchSB.material = currTouchSB.material >= 1 ? 1 : currTouchSB.material + 0.1f;
                touchDownX = x;
                currTouchSB.slide(calculateCurrentSeekBarPercent(touchDownX));
                currTouchSB.setShowIndicatorEnable(true);

                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, true);
                }
                invalidate();
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                if (currTouchSB == leftSB) {
                    resetCurrentSeekBarThumb();
                } else if (currTouchSB == rightSB) {
                    resetCurrentSeekBarThumb();
                }
                leftSB.setShowIndicatorEnable(false);
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false);
                }
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(false);
                break;
            case MotionEvent.ACTION_UP:
                if (verifyStepsMode() && stepsAutoBonding) {
                    float percent = calculateCurrentSeekBarPercent(getEventX(event));
                    float stepPercent = 1.0f / steps;
                    int stepSelected = new BigDecimal(percent / stepPercent).setScale(0, RoundingMode.HALF_UP).intValue();
                    currTouchSB.slide(stepSelected * stepPercent);
                }

                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                leftSB.setShowIndicatorEnable(false);
                currTouchSB.materialRestore();
                resetCurrentSeekBarThumb();
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false);
                }
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = minProgress;
        ss.maxValue = maxProgress;
        ss.rangeInterval = minInterval;
        SeekBarState[] results = getRangeSeekBarState();
        ss.currSelectedMin = results[0].value;
        ss.currSelectedMax = results[1].value;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            float min = ss.minValue;
            float max = ss.maxValue;
            float rangeInterval = ss.rangeInterval;
            setRange(min, max, rangeInterval);
            float currSelectedMin = ss.currSelectedMin;
            float currSelectedMax = ss.currSelectedMax;
            setProgress(currSelectedMin, currSelectedMax);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //******************* Attributes getter and setter *******************//

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        callback = listener;
    }

    public void setProgress(float value) {
        setProgress(value, maxProgress);
    }

    public void setProgress(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < minInterval) {
            if (leftValue - minProgress > maxProgress - rightValue) {
                leftValue = rightValue - minInterval;
            } else {
                rightValue = leftValue + minInterval;
            }
        }

        if (leftValue < minProgress) {
            throw new IllegalArgumentException("setProgress() min < (preset min - offsetValue) . #min:" + leftValue + " #preset min:" + rightValue);
        }
        if (rightValue > maxProgress) {
            throw new IllegalArgumentException("setProgress() max > (preset max - offsetValue) . #max:" + rightValue + " #preset max:" + rightValue);
        }

        float range = maxProgress - minProgress;
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }

        if (callback != null) {
            callback.onRangeChanged(this, leftValue, rightValue, false);
        }
        invalidate();
    }


    /**
     * 设置范围
     *
     * @param min 最小值
     * @param max 最大值
     */
    public void setRange(float min, float max) {
        setRange(min, max, minInterval);
    }

    /**
     * 设置范围
     *
     * @param min         最小值
     * @param max         最大值
     * @param minInterval 最小间隔
     */
    public void setRange(float min, float max, float minInterval) {
        if (max <= min) {
            throw new IllegalArgumentException("setRange() max must be greater than min ! #max:" + max + " #min:" + min);
        }
        if (minInterval < 0) {
            throw new IllegalArgumentException("setRange() interval must be greater than zero ! #minInterval:" + minInterval);
        }
        if (minInterval >= max - min) {
            throw new IllegalArgumentException("setRange() interval must be less than (max - min) ! #minInterval:" + minInterval + " #max - min:" + (max - min));
        }

        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);

        //set default value
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
        invalidate();
    }

    /**
     * @return the two seekBar state , see {@link }
     */
    public SeekBarState[] getRangeSeekBarState() {
        SeekBarState leftSeekBarState = new SeekBarState();
        leftSeekBarState.value = leftSB.getProgress();

        leftSeekBarState.indicatorText = String.valueOf(leftSeekBarState.value);
        if (SeekBarUtils.compareFloat(leftSeekBarState.value, minProgress) == 0) {
            leftSeekBarState.isMin = true;
        } else if (SeekBarUtils.compareFloat(leftSeekBarState.value, maxProgress) == 0) {
            leftSeekBarState.isMax = true;
        }

        SeekBarState rightSeekBarState = new SeekBarState();
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSeekBarState.value = rightSB.getProgress();
            rightSeekBarState.indicatorText = String.valueOf(rightSeekBarState.value);
            if (SeekBarUtils.compareFloat(rightSB.currPercent, minProgress) == 0) {
                rightSeekBarState.isMin = true;
            } else if (SeekBarUtils.compareFloat(rightSB.currPercent, maxProgress) == 0) {
                rightSeekBarState.isMax = true;
            }
        }

        return new SeekBarState[]{leftSeekBarState, rightSeekBarState};
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;
    }

    /**
     * format number indicator text
     *
     * @param formatPattern format rules
     */
    public void setIndicatorTextDecimalFormat(String formatPattern) {
        leftSB.setIndicatorTextDecimalFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextDecimalFormat(formatPattern);
        }
    }

    /**
     * if is single mode, please use it to get the SeekBar
     *
     * @return left seek bar
     */
    public SeekBar getLeftSeekBar() {
        return leftSB;
    }

    public SeekBar getRightSeekBar() {
        return rightSB;
    }


    public int getProgressTop() {
        return progressTop;
    }

    public int getProgressBottom() {
        return progressBottom;
    }

    public int getProgressLeft() {
        return progressLeft;
    }

    public int getProgressRight() {
        return progressRight;
    }

    public int getProgressPaddingRight() {
        return progressPaddingRight;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    public float getMinProgress() {
        return minProgress;
    }

    public float getMaxProgress() {
        return maxProgress;
    }


    public int getProgressWidth() {
        return progressWidth;
    }


    public int getGravity() {
        return gravity;
    }

    /**
     * the RangeSeekBar gravity
     * Gravity.TOP and Gravity.BOTTOM
     *
     * @param gravity
     */
    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

}
