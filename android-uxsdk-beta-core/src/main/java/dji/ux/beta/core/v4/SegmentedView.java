package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Created by eleven on 2017/8/15.
 */

public class SegmentedView extends View {

    private static final String TAG = SegmentedView.class.getSimpleName();

    private Context context;
    private Drawable segmentedControlFgDrawable;
    private Drawable segmentedControlBgDrawable;
    private Drawable dividerDrawable;
    private ColorStateList textColorState;
    private CharSequence[] segmentStrings;
    private int textSize;
    private boolean enableDivider;

    private Paint textPaint;
    private int count;
    private int selectedIndex = 0;
    private int touchIndex = -1;
    private int defaultHeight;
    private int minEachWidth;
    private OnSegmentSelectedListener onSegmentSelectedListener;

    public SegmentedView(Context context) {
        this(context, null);
    }

    public SegmentedView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SegmentedView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;

        defaultHeight = dip2px(30);
        minEachWidth = dip2px(70);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedView);
        textSize = a.getDimensionPixelSize(R.styleable.SegmentedView_uxsdk_segmentedTextSize, 30);
        segmentedControlBgDrawable = a.getDrawable(R.styleable.SegmentedView_uxsdk_segmentedBackground);
        segmentedControlFgDrawable = a.getDrawable(R.styleable.SegmentedView_uxsdk_segmentedForeground);
        dividerDrawable = a.getDrawable(R.styleable.SegmentedView_uxsdk_segmentedDivider);
        textColorState = a.getColorStateList(R.styleable.SegmentedView_uxsdk_textColor);
        segmentStrings = a.getTextArray(R.styleable.SegmentedView_uxsdk_segmentedTexts);
        enableDivider = a.getBoolean(R.styleable.SegmentedView_uxsdk_segmentedEnableDivider, false);
        a.recycle();

        if (textColorState == null) {
            textColorState = getResources().getColorStateList(R.color.uxsdk_selector_default_segmented_color);
        }
        if (segmentedControlBgDrawable == null) {
            segmentedControlBgDrawable = getResources().getDrawable(R.drawable.uxsdk_default_segmented_shape_bg);
        }
        if (segmentedControlFgDrawable == null) {
            segmentedControlFgDrawable = getResources().getDrawable(R.drawable.uxsdk_default_segmented_shape_fg);
        }
        if (dividerDrawable == null) {
            dividerDrawable = getResources().getDrawable(R.drawable.uxsdk_default_segmented_divider);
        }


        selectedIndex = 0;
        if (segmentStrings == null) {
            segmentStrings = new CharSequence[]{"Segment 1", "Segment 2", "Segment 3"};
        }
        if (isInEditMode()) {
            segmentStrings = new CharSequence[]{"Segment 1", "Segment 2", "Segment 3"};
        }
        count = segmentStrings.length;

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSize);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            width = segmentedControlBgDrawable.getIntrinsicWidth();
            if (width == -1) {
                width = minEachWidth * count;
            }
        }
        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            height = segmentedControlBgDrawable.getIntrinsicHeight();
            if (height == -1) {
                height = defaultHeight;
            }
        }
        if (widthMode == MeasureSpec.EXACTLY) {
            DJILog.d(TAG,"EXACTLY");
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            DJILog.d(TAG, "height exactly");
        }
        DJILog.d(TAG, "width = " + width + " " + "height = " + height);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        segmentedControlBgDrawable.setBounds(0, 0, getWidth(), getHeight());
        segmentedControlBgDrawable.draw(canvas);

        canvas.save();
        int eachWidth = getWidth() / count;
        segmentedControlFgDrawable.setBounds(0, 0, getWidth(), getHeight());
        canvas.clipRect(selectedIndex * eachWidth, 0, selectedIndex * eachWidth + eachWidth, getHeight());
        segmentedControlFgDrawable.draw(canvas);
        canvas.restore();

        canvas.save();
        if (touchIndex != -1 && touchIndex != selectedIndex) {
            canvas.clipRect(touchIndex * eachWidth, 0, touchIndex * eachWidth + eachWidth, getHeight());
            segmentedControlFgDrawable.draw(canvas);
        }
        canvas.restore();

        // text
        if (segmentStrings != null) {
            for (int i = 0; i < count; i++) {
                if (i == touchIndex) {
                    int color = textColorState.getColorForState(View.PRESSED_ENABLED_STATE_SET, Color.BLACK);
                    textPaint.setColor(color);
                } else if (i == selectedIndex) {
                    int color = textColorState.getColorForState(View.ENABLED_SELECTED_STATE_SET, Color.WHITE);
                    textPaint.setColor(color);
                } else {
                    textPaint.setColor(textColorState.getDefaultColor());
                }
                float start = i * eachWidth + eachWidth / 2;
                Rect textBounds = new Rect();
                textPaint.getTextBounds(segmentStrings[i].toString(), 0, segmentStrings[i].length(), textBounds);
                float v = getHeight()/2 - textBounds.exactCenterY();
                canvas.save();
                canvas.clipRect(i * eachWidth, 0, i * eachWidth + eachWidth, getHeight());
                canvas.drawText(segmentStrings[i].toString(), start, v, textPaint);
                canvas.restore();
            }
        }

        // divider
        if (enableDivider) {
            for (int i = eachWidth; i < eachWidth * count; i += eachWidth) {
                int r = dividerDrawable.getIntrinsicWidth() / 2;
                dividerDrawable.setBounds(i - r, 0, i + r, getHeight());
                dividerDrawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            {
                touchIndex = detectTouchIndex(event.getX(), event.getY());
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE:
            {
                return true;
            }
            case MotionEvent.ACTION_UP:
            {
                touchIndex = detectTouchIndex(event.getX(), event.getY());
                if (touchIndex != -1 && touchIndex != selectedIndex) {
                    selectedIndex = touchIndex;
                    touchIndex = -1;
                    if (onSegmentSelectedListener != null) {
                        onSegmentSelectedListener.onSelectIndex(selectedIndex);
                    }
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                touchIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int detectTouchIndex(float x, float y) {
        if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
            return -1;
        }
        int eachWidth = getWidth() / count;
        for (int i = eachWidth * count, k = count-1; i > 0; i-=eachWidth, k--) {
            if (x < i && x > i-eachWidth) {
                return k;
            }
        }
        return -1;
    }

    public int dip2px(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public OnSegmentSelectedListener getOnSegmentSelectedListener() {
        return onSegmentSelectedListener;
    }

    public void setOnSegmentSelectedListener(OnSegmentSelectedListener onSegmentSelectedListener) {
        this.onSegmentSelectedListener = onSegmentSelectedListener;
    }

    public Drawable getSegmentedControlFgDrawable() {
        return segmentedControlFgDrawable;
    }

    public void setSegmentedControlFgDrawable(Drawable segmentedControlFgDrawable) {
        this.segmentedControlFgDrawable = segmentedControlFgDrawable;
    }

    public Drawable getSegmentedControlBgDrawable() {
        return segmentedControlBgDrawable;
    }

    public void setSegmentedControlBgDrawable(Drawable segmentedControlBgDrawable) {
        this.segmentedControlBgDrawable = segmentedControlBgDrawable;
    }

    public Drawable getDividerDrawable() {
        return dividerDrawable;
    }

    public void setDividerDrawable(Drawable dividerDrawable) {
        this.dividerDrawable = dividerDrawable;
    }

    public ColorStateList getTextColorState() {
        return textColorState;
    }

    public void setTextColorState(ColorStateList textColorState) {
        this.textColorState = textColorState;
    }

    public CharSequence[] getSegmentStrings() {
        return segmentStrings;
    }

    public void setSegmentStrings(CharSequence[] segmentStrings) {
        this.segmentStrings = segmentStrings;
        count = segmentStrings.length;
        postInvalidateOnAnimation();
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        postInvalidateOnAnimation();
    }

    public interface OnSegmentSelectedListener {
        void onSelectIndex(int index);
    }
}
