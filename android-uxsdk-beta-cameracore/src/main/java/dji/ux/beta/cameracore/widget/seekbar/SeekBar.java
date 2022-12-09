package dji.ux.beta.cameracore.widget.seekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.text.DecimalFormat;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.core.content.ContextCompat;
import dji.ux.beta.cameracore.R;
import dji.ux.beta.core.util.UnitUtils;

public class SeekBar {
    //the indicator show mode
    public static final int INDICATOR_SHOW_WHEN_TOUCH = 0;
    public static final int INDICATOR_ALWAYS_HIDE = 1;
    public static final int INDICATOR_ALWAYS_SHOW_AFTER_TOUCH = 2;
    public static final int INDICATOR_ALWAYS_SHOW = 3;

    @IntDef({INDICATOR_SHOW_WHEN_TOUCH, INDICATOR_ALWAYS_HIDE, INDICATOR_ALWAYS_SHOW_AFTER_TOUCH, INDICATOR_ALWAYS_SHOW})
    public @interface IndicatorModeDef {
    }

    public static final int WRAP_CONTENT = -1;
    public static final int MATCH_PARENT = -2;

    private int indicatorShowMode;

    //进度提示背景的高度，宽度如果是0的话会自适应调整
    //Progress prompted the background height, width,
    private int indicatorHeight;
    private int indicatorWidth;
    //进度提示背景与按钮之间的距离
    //The progress indicates the distance between the background and the button
    private int indicatorMargin;
    private int indicatorDrawableId;
    private int indicatorArrowSize;
    private int indicatorTextSize;
    private int indicatorTextColor;
    private float indicatorRadius;
    private int indicatorBackgroundColor;
    private int indicatorPaddingLeft, indicatorPaddingRight, indicatorPaddingTop, indicatorPaddingBottom;
    private int thumbDrawableId;
    private int thumbLeftDrawableId;
    private int thumbRightDrawableId;

    private int thumbInactivatedDrawableId;
    private int thumbWidth;
    private int thumbHeight;

    //when you touch or move, the thumb will scale, default not scale
    float thumbScaleRatio;

    //****************** the above is attr value  ******************//

    int left, right, top, bottom;
    float currPercent;
    float material = 0;
    private boolean isShowIndicator;
    boolean isLeft;
    Bitmap thumbBitmap;
    Bitmap thumbLeftBitmap;
    Bitmap thumbRightBitmap;
    Bitmap thumbInactivatedBitmap;
    Bitmap indicatorBitmap;
    ValueAnimator anim;
    String userText2Draw;
    boolean isActivate = false;
    boolean isVisible = true;
    RangeSeekBar rangeSeekBar;
    String indicatorTextStringFormat;
    Path indicatorArrowPath = new Path();
    Rect indicatorTextRect = new Rect();
    Rect indicatorRect = new Rect();
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    DecimalFormat indicatorTextDecimalFormat;
    int scaleThumbWidth;
    int scaleThumbHeight;

    public SeekBar(RangeSeekBar rangeSeekBar, AttributeSet attrs, boolean isLeft) {
        this.rangeSeekBar = rangeSeekBar;
        this.isLeft = isLeft;
        initAttrs(attrs);
        initBitmap();
        initVariables();
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        if (t == null) return;
        indicatorMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_margin, 0);
        indicatorDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_indicator_drawable, 0);
        indicatorShowMode = t.getInt(R.styleable.RangeSeekBar_rsb_indicator_show_mode, INDICATOR_ALWAYS_HIDE);
        indicatorHeight = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_height, WRAP_CONTENT);
        indicatorWidth = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_width, WRAP_CONTENT);
        indicatorTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_text_size, SeekBarUtils.dp2px(getContext(), 14));
        indicatorTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_text_color, Color.WHITE);
        indicatorBackgroundColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_background_color, ContextCompat.getColor(getContext(), R.color.colorAccent));
        indicatorPaddingLeft = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_left, 0);
        indicatorPaddingRight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_right, 0);
        indicatorPaddingTop = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_top, 0);
        indicatorPaddingBottom = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_bottom, 0);
        indicatorArrowSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_arrow_size, 0);
        thumbDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_drawable, R.drawable.uxsdk_rsb_default_thumb);
        thumbLeftDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_drawable, R.drawable.uxsdk_rsb_default_thumb);
//        thumbLeftDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_left_drawable, R.drawable.rsb_default_thumb);
        thumbRightDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_right_drawable, R.drawable.uxsdk_rsb_default_thumb);
        thumbInactivatedDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_inactivated_drawable, 0);
        thumbWidth = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_width, SeekBarUtils.dp2px(getContext(), 20));
        thumbHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_height, SeekBarUtils.dp2px(getContext(), 28.5f));
        thumbScaleRatio = t.getFloat(R.styleable.RangeSeekBar_rsb_thumb_scale_ratio, 1f);
        indicatorRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_radius, 0f);
        t.recycle();
    }

    protected void initVariables() {
        scaleThumbWidth = thumbWidth;
        scaleThumbHeight = thumbHeight;
        if (indicatorHeight == WRAP_CONTENT) {
            indicatorHeight = SeekBarUtils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom;
        }
        if (indicatorArrowSize <= 0) {
            indicatorArrowSize = thumbWidth / 4;
        }
    }

    public Context getContext() {
        return rangeSeekBar.getContext();
    }

    public Resources getResources() {
        if (getContext() != null) return getContext().getResources();
        return null;
    }

    /**
     * 初始化进度提示的背景
     */
    private void initBitmap() {
        setIndicatorDrawableId(indicatorDrawableId);
//        setThumbDrawableId(thumbDrawableId, thumbWidth, thumbHeight);
        setThumbDrawableId(thumbLeftDrawableId, thumbRightDrawableId, thumbWidth, thumbHeight);
        setThumbInactivatedDrawableId(thumbInactivatedDrawableId, thumbWidth, thumbHeight);
    }

    /**
     * 计算每个按钮的位置和尺寸
     * Calculates the position and size of each button
     *
     * @param x position x
     * @param y position y
     */
    protected void onSizeChanged(int x, int y) {
        initVariables();
        initBitmap();
        left = (int) (x - getThumbScaleWidth() / 2);
        right = (int) (x + getThumbScaleWidth() / 2);
        top = y - getThumbHeight() / 2;
        bottom = y + getThumbHeight() / 2;
    }


    public void scaleThumb() {
        scaleThumbWidth = (int) getThumbScaleWidth();
        scaleThumbHeight = (int) getThumbScaleHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbLeftDrawableId, thumbRightDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public void resetThumb() {
        scaleThumbWidth = getThumbWidth();
        scaleThumbHeight = getThumbHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbLeftDrawableId, thumbRightDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public float getRawHeight() {
        return getIndicatorHeight() + getIndicatorArrowSize() + getIndicatorMargin() + getThumbScaleHeight();
    }

    /**
     * 绘制按钮和提示背景和文字
     * Draw buttons and tips for background and text
     * 0 left  1 right
     *
     * @param canvas Canvas
     */
    protected void draw(Canvas canvas, int direction) {
        if (!isVisible) {
            return;
        }
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        canvas.save();
        if (direction == 0) {
            canvas.translate(offset, 34);
        } else {
            canvas.translate(offset, -34);
        }
        // translate canvas, then don't care left
        canvas.translate(left, 0);
        if (isShowIndicator) {
            onDrawIndicator(canvas, paint, formatCurrentIndicatorText(userText2Draw), direction);
        }
        onDrawThumb(canvas, direction);
        canvas.restore();
    }


    /**
     * 绘制按钮
     * 如果没有图片资源，则绘制默认按钮
     * <p>
     * draw the thumb button
     * If there is no image resource, draw the default button
     *
     * @param canvas canvas
     */
    protected void onDrawThumb(Canvas canvas, int direction) {
        if (thumbInactivatedBitmap != null && !isActivate) {
            canvas.drawBitmap(thumbInactivatedBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
        } else if (thumbLeftBitmap != null && thumbRightBitmap != null) {
            if (direction == 0) {
                canvas.drawBitmap(thumbLeftBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
            } else {
                canvas.drawBitmap(thumbRightBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
            }
        }
    }

    /**
     * 格式化提示文字
     * format the indicator text
     *
     * @param text2Draw
     * @return
     */
    protected String formatCurrentIndicatorText(String text2Draw) {
        SeekBarState[] states = rangeSeekBar.getRangeSeekBarState();
        if (TextUtils.isEmpty(text2Draw)) {
            if (isLeft) {
                if (indicatorTextDecimalFormat != null) {
                    int value = (int) states[0].value;
                    text2Draw = indicatorTextDecimalFormat.format(value);
                } else {
                    text2Draw = states[0].indicatorText;
                }
            } else {
                if (indicatorTextDecimalFormat != null) {
                    int value = (int) states[1].value;
                    text2Draw = indicatorTextDecimalFormat.format(value);
                } else {
                    text2Draw = states[1].indicatorText;
                }
            }
        }
        if (indicatorTextStringFormat != null) {
            text2Draw = String.format(indicatorTextStringFormat, text2Draw);
        }
        return text2Draw;
    }

    /**
     * This method will draw the indicator background dynamically according to the text.
     * you can use to set padding
     *
     * @param canvas    Canvas
     * @param text2Draw Indicator text
     */
    protected void onDrawIndicator(Canvas canvas, Paint paint, String text2Draw, int direction) {
        if (text2Draw == null) return;
        paint.setTextSize(indicatorTextSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(indicatorBackgroundColor);
        paint.getTextBounds(text2Draw, 0, text2Draw.length(), indicatorTextRect);
        int realIndicatorWidth = indicatorTextRect.width() + indicatorPaddingLeft + indicatorPaddingRight;
        if (indicatorWidth > realIndicatorWidth) {
            realIndicatorWidth = indicatorWidth;
        }

        int realIndicatorHeight = indicatorTextRect.height() + indicatorPaddingTop + indicatorPaddingBottom;
        if (indicatorHeight > realIndicatorHeight) {
            realIndicatorHeight = indicatorHeight;
        }
        if (direction == 0) {
            indicatorRect.left = (int) (scaleThumbWidth / 2f - realIndicatorWidth / 2f) + 70;
            indicatorRect.top = bottom - realIndicatorHeight - indicatorMargin + 12;
        } else {
            indicatorRect.left = (int) (scaleThumbWidth / 2f - realIndicatorWidth / 2f) - scaleThumbWidth - 70;
            indicatorRect.top = bottom - realIndicatorHeight - indicatorMargin - 10;
        }
        indicatorRect.right = indicatorRect.left + realIndicatorWidth;
        indicatorRect.bottom = indicatorRect.top + realIndicatorHeight;
        //draw default indicator arrow
        if (indicatorBitmap == null) {
            //arrow three point
            //  b   c
            //    a
            int ax = scaleThumbWidth / 2;
            int ay = indicatorRect.bottom;
            int bx = ax - indicatorArrowSize;
            int by = ay - indicatorArrowSize;
            int cx = ax + indicatorArrowSize;
            indicatorArrowPath.reset();
            indicatorArrowPath.moveTo(ax, ay);
            indicatorArrowPath.lineTo(bx, by);
            indicatorArrowPath.lineTo(cx, by);
            indicatorArrowPath.close();
            canvas.drawPath(indicatorArrowPath, paint);
            indicatorRect.bottom -= indicatorArrowSize;
            indicatorRect.top -= indicatorArrowSize;
        }

        //indicator background edge processing
        int defaultPaddingOffset = SeekBarUtils.dp2px(getContext(), 1);
        int leftOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * currPercent) - rangeSeekBar.getProgressLeft() + defaultPaddingOffset;
        int rightOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * (1 - currPercent)) - rangeSeekBar.getProgressPaddingRight() + defaultPaddingOffset;

        if (leftOffset > 0) {
            indicatorRect.left += leftOffset;
            indicatorRect.right += leftOffset;
        } else if (rightOffset > 0) {
            indicatorRect.left -= rightOffset;
            indicatorRect.right -= rightOffset;
        }

        //draw indicator background
        if (indicatorBitmap != null) {
            SeekBarUtils.drawBitmap(canvas, paint, indicatorBitmap, indicatorRect);
        } else if (indicatorRadius > 0f) {
            canvas.drawRoundRect(new RectF(indicatorRect), indicatorRadius, indicatorRadius, paint);
        } else {
            canvas.drawRect(indicatorRect, paint);
        }

        //draw indicator content text
        int tx, ty;
        if (indicatorPaddingLeft > 0) {
            tx = indicatorRect.left + indicatorPaddingLeft;

        } else if (indicatorPaddingRight > 0) {
            tx = indicatorRect.right - indicatorPaddingRight - indicatorTextRect.width();
        } else {
            tx = indicatorRect.left + (realIndicatorWidth - indicatorTextRect.width()) / 2;
        }

        if (indicatorPaddingTop > 0) {
            ty = indicatorRect.top + indicatorTextRect.height() + indicatorPaddingTop;
        } else if (indicatorPaddingBottom > 0) {
            ty = indicatorRect.bottom - indicatorTextRect.height() - indicatorPaddingBottom;
        } else {
            ty = indicatorRect.bottom - (realIndicatorHeight - indicatorTextRect.height()) / 2 + 1;
        }

        //draw indicator text
        paint.setColor(indicatorTextColor);
        canvas.drawText(text2Draw, tx, ty, paint);
    }

    /**
     * 拖动检测
     *
     * @return is collide
     */
    protected boolean collide(float x, float y) {
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        return x > left + offset && x < right + offset && y > top && y < bottom;
    }

    protected void slide(float percent) {
        if (percent < 0) percent = 0;
        else if (percent > 1) percent = 1;
        currPercent = percent;
    }

    protected void setShowIndicatorEnable(boolean isEnable) {
        switch (indicatorShowMode) {
            case INDICATOR_SHOW_WHEN_TOUCH:
                isShowIndicator = isEnable;
                break;
            case INDICATOR_ALWAYS_SHOW:
            case INDICATOR_ALWAYS_SHOW_AFTER_TOUCH:
                isShowIndicator = true;
                break;
            case INDICATOR_ALWAYS_HIDE:
                isShowIndicator = false;
                break;
        }
    }

    public void materialRestore() {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(material, 0);
        anim.addUpdateListener(animation -> {
            material = (float) animation.getAnimatedValue();
            if (rangeSeekBar != null) rangeSeekBar.invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                material = 0;
                if (rangeSeekBar != null) rangeSeekBar.invalidate();
            }
        });
        anim.start();
    }


    public void setIndicatorTextDecimalFormat(String formatPattern) {
        indicatorTextDecimalFormat = new DecimalFormat(formatPattern);
    }


    public void setIndicatorDrawableId(@DrawableRes int indicatorDrawableId) {
        if (indicatorDrawableId != 0) {
            this.indicatorDrawableId = indicatorDrawableId;
            indicatorBitmap = BitmapFactory.decodeResource(getResources(), indicatorDrawableId);
        }
    }

    public int getIndicatorArrowSize() {
        return indicatorArrowSize;
    }

    public int getIndicatorMargin() {
        return indicatorMargin;
    }


    public int getIndicatorShowMode() {
        return indicatorShowMode;
    }

    /**
     * include indicator text Height、padding、margin
     *
     * @return The actual occupation height of indicator
     */
    public int getIndicatorRawHeight() {
        if (indicatorHeight > 0) {
            if (indicatorBitmap != null) {
                return indicatorHeight + indicatorMargin;
            } else {
                return indicatorHeight + indicatorArrowSize + indicatorMargin;
            }
        } else {
            if (indicatorBitmap != null) {
                return SeekBarUtils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin;
            } else {
                return SeekBarUtils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin + indicatorArrowSize;
            }
        }
    }

    public int getIndicatorHeight() {
        return indicatorHeight;
    }

    public void setThumbInactivatedDrawableId(@DrawableRes int thumbInactivatedDrawableId, int width, int height) {
        if (thumbInactivatedDrawableId != 0 && getResources() != null) {
            this.thumbInactivatedDrawableId = thumbInactivatedDrawableId;
            thumbInactivatedBitmap = SeekBarUtils.drawableToBitmap(width, height, getResources().getDrawable(thumbInactivatedDrawableId, null));
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void setThumbDrawableId(@DrawableRes int leftThumbDrawableId, int rightThumbDrawableId, int width, int height) {
        if (leftThumbDrawableId != 0 && getResources() != null && width > 0 && height > 0) {
            this.thumbLeftDrawableId = leftThumbDrawableId;
            thumbLeftBitmap = SeekBarUtils.drawableToBitmap(width, height, getResources().getDrawable(leftThumbDrawableId, null));
        }
        if (rightThumbDrawableId != 0 && getResources() != null && width > 0 && height > 0) {
            this.thumbRightDrawableId = rightThumbDrawableId;
            thumbBitmap = SeekBarUtils.drawableToBitmap(width, height, getResources().getDrawable(thumbDrawableId, null));
            thumbRightBitmap = SeekBarUtils.drawableToBitmap(width, height, getResources().getDrawable(rightThumbDrawableId, null));
        }

    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public float getThumbScaleHeight() {
        return thumbHeight * thumbScaleRatio;
    }

    public float getThumbScaleWidth() {
        return thumbWidth * thumbScaleRatio;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }


    protected boolean getActivate() {
        return isActivate;
    }

    protected void setActivate(boolean activate) {
        isActivate = activate;
    }

    /**
     * when you touch or move, the thumb will scale, default not scale
     *
     * @return default 1.0f
     */
    public float getThumbScaleRatio() {
        return thumbScaleRatio;
    }

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * if visble is false, will clear the Canvas
     *
     * @param visible
     */
    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public float getProgress() {
        float range = rangeSeekBar.getMaxProgress() - rangeSeekBar.getMinProgress();
        return rangeSeekBar.getMinProgress() + range * currPercent;
    }
}
