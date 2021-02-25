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

package dji.ux.beta.cameracore.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;

import dji.ux.beta.cameracore.R;

/**
 * Author : Created by kyle on 15/11/9.
 * Library : https://github.com/lantouzi/WheelView-Android
 * <p>
 * <p>
 * Modified by DJI
 */
public class WheelView extends View implements GestureDetector.OnGestureListener {
    public static final float DEFAULT_INTERVAL_FACTOR = 1.2f;
    public static final float DEFAULT_MARK_RATIO = 0.7f;

    //region Fields
    private Paint markPaint;
    private TextPaint markTextPaint;
    private int centerIndex = -1;

    private int highlightColor, markTextColor;
    private int markColor, fadeMarkColor, outOfRangeMarkColor;

    private int height;
    private List<String> itemList;
    private String additionCenterMark;
    private OnWheelItemSelectedListener onWheelItemSelectedListener;
    private float intervalFactor;
    private float markRatio;

    private int markCount;
    private float additionCenterMarkWidth;
    private Path centerIndicatorPath = new Path();
    private float cursorHeight;
    private int viewScopeSize;

    // scroll control args ---- start
    private OverScroller scroller;
    private float maxOverScrollDistance;
    private RectF contentRectF;
    private boolean fling = false;
    private float centerTextSize, normalTextSize;
    private float topSpace, bottomSpace;
    private float intervalDis;
    private float centerMarkWidth, markWidth;
    private GestureDetectorCompat mGestureDetectorCompat;
    // scroll control args ---- end

    private int lastSelectedIndex = -1;
    private int minSelectableIndex = Integer.MIN_VALUE;
    private int maxSelectableIndex = Integer.MAX_VALUE;

    private WheelType wheelType;
    //endregion

    //region Constructors
    public WheelView(Context context) {
        super(context);
        init(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //endregion

    //region private methods
    protected void init(Context context, AttributeSet attrs) {
        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        initPaint();
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
        readAttributes(typedArray);
        typedArray.recycle();
    }

    private void initPaint() {
        fadeMarkColor = highlightColor & 0xAAFFFFFF;
        intervalFactor = Math.max(DEFAULT_INTERVAL_FACTOR, intervalFactor);
        markRatio = Math.min(1, markRatio);
        topSpace = cursorHeight + getResources().getDimension(R.dimen.uxsdk_wheel_view_top_padding);
        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        markTextPaint.setTextAlign(Paint.Align.CENTER);
        markTextPaint.setColor(highlightColor);
        markPaint.setColor(markColor);
        markPaint.setStrokeWidth(centerMarkWidth);
        markTextPaint.setTextSize(centerTextSize);
        calcIntervalDis();
        scroller = new OverScroller(getContext());
        contentRectF = new RectF();
        mGestureDetectorCompat = new GestureDetectorCompat(getContext(), this);
        selectIndex(0);
    }

    private void readAttributes(@NonNull TypedArray typedArray) {
        highlightColor = typedArray.getColor(R.styleable.WheelView_uxsdk_highlightColor, highlightColor);
        markTextColor = typedArray.getColor(R.styleable.WheelView_uxsdk_markTextColor, markTextColor);
        markColor = typedArray.getColor(R.styleable.WheelView_uxsdk_markColor, markColor);
        intervalFactor = typedArray.getFloat(R.styleable.WheelView_uxsdk_intervalFactor, intervalFactor);
        markRatio = typedArray.getFloat(R.styleable.WheelView_uxsdk_markRatio, markRatio);
        additionCenterMark = typedArray.getString(R.styleable.WheelView_uxsdk_additionalCenterMark);
        centerTextSize = typedArray.getDimension(R.styleable.WheelView_uxsdk_centerMarkTextSize, centerTextSize);
        normalTextSize = typedArray.getDimension(R.styleable.WheelView_uxsdk_markTextSize, normalTextSize);
        cursorHeight = typedArray.getDimension(R.styleable.WheelView_uxsdk_cursorHeight, cursorHeight);
        outOfRangeMarkColor = typedArray.getColor(R.styleable.WheelView_uxsdk_outOfRangeMarkerColor, getResources().getColor(R.color.uxsdk_gray_640));
        wheelType = WheelType.find(typedArray.getInt(R.styleable.WheelView_uxsdk_wheelType, 0));
    }

    private void initDefaults() {
        centerMarkWidth = getResources().getDimension(R.dimen.uxsdk_wheel_view_center_mark_width);
        markWidth = getResources().getDisplayMetrics().density;
        highlightColor = getResources().getColor(R.color.uxsdk_blue_highlight);
        markTextColor = getResources().getColor(R.color.uxsdk_white_85_percent);
        markColor = getResources().getColor(R.color.uxsdk_white_85_percent);
        cursorHeight = getResources().getDimension(R.dimen.uxsdk_wheel_view_cursor_size);
        centerTextSize = getResources().getDimension(R.dimen.uxsdk_wheel_view_center_text_size);
        normalTextSize = getResources().getDimension(R.dimen.uxsdk_wheel_view_normal_text_size);
        bottomSpace = getResources().getDimension(R.dimen.uxsdk_wheel_view_bottom_space);
        intervalFactor = DEFAULT_INTERVAL_FACTOR;
        markRatio = DEFAULT_MARK_RATIO;
        wheelType = WheelType.VALUES_ONLY;
    }


    /**
     * calculate interval distance between items
     */
    private void calcIntervalDis() {
        if (markTextPaint == null) {
            return;
        }
        String defaultText = "88.8";
        Rect temp = new Rect();
        int max = 0;
        if (itemList != null && itemList.size() > 0) {
            for (String i : itemList) {
                markTextPaint.getTextBounds(i, 0, i.length(), temp);
                if (temp.width() > max) {
                    max = temp.width();
                }
            }
        } else {
            markTextPaint.getTextBounds(defaultText, 0, defaultText.length(), temp);
            max = temp.width();
        }

        if (!TextUtils.isEmpty(additionCenterMark)) {
            markTextPaint.setTextSize(normalTextSize);
            markTextPaint.getTextBounds(additionCenterMark, 0, additionCenterMark.length(), temp);
            additionCenterMarkWidth = temp.width();
            max += temp.width();
        }

        intervalDis = max * intervalFactor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int widthMeasureSpec) {
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureSize = MeasureSpec.getSize(widthMeasureSpec);
        int result = getSuggestedMinimumWidth();
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = measureSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                break;
        }
        return result;
    }

    private int measureHeight(int heightMeasure) {
        int measureMode = MeasureSpec.getMode(heightMeasure);
        int measureSize = MeasureSpec.getSize(heightMeasure);
        int result = (int) (bottomSpace + topSpace * 2 + centerTextSize);
        switch (measureMode) {
            case MeasureSpec.EXACTLY:
                result = Math.max(result, measureSize);
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(result, measureSize);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                break;
        }
        return result;
    }

    private void fling(int velocityX, int velocityY) {
        scroller.fling(getScrollX(), getScrollY(),
                velocityX, velocityY,
                (int) (-maxOverScrollDistance + minSelectableIndex * intervalDis), (int) (contentRectF.width() - maxOverScrollDistance - (markCount - 1 - maxSelectableIndex) * intervalDis),
                0, 0,
                (int) maxOverScrollDistance, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            height = h;
            maxOverScrollDistance = w / 2.f;
            contentRectF.set(0, 0, (markCount - 1) * intervalDis, h);
            viewScopeSize = (int) Math.ceil(maxOverScrollDistance / intervalDis);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        centerIndicatorPath.reset();
        centerIndicatorPath.moveTo(maxOverScrollDistance - cursorHeight + getScrollX(), 0);
        centerIndicatorPath.rLineTo(cursorHeight, cursorHeight);
        centerIndicatorPath.rLineTo(cursorHeight, -cursorHeight);
        centerIndicatorPath.close();

        markPaint.setColor(highlightColor);
        canvas.drawPath(centerIndicatorPath, markPaint);

        int start = centerIndex - viewScopeSize;
        int end = centerIndex + viewScopeSize + 1;

        start = Math.max(start, -viewScopeSize * 2);
        end = Math.min(end, markCount + viewScopeSize * 2);

        // extends both ends
        if (centerIndex == maxSelectableIndex) {
            end += viewScopeSize;
        } else if (centerIndex == minSelectableIndex) {
            start -= viewScopeSize;
        }

        float x = start * intervalDis;

        float markHeight = height - bottomSpace - centerTextSize - topSpace;
        // small scale Y offset
        float smallMarkShrinkY = markHeight * (1 - markRatio) / 2f;
        smallMarkShrinkY = Math.min((markHeight - markWidth) / 2f, smallMarkShrinkY);

        for (int i = start; i < end; i++) {
            float tempDis = intervalDis / 5f;
            // offset: Small mark offset Big mark
            for (int offset = -2; offset < 3; offset++) {
                float ox = x + offset * tempDis;

                if (i >= 0 && i <= markCount && centerIndex == i) {
                    int tempOffset = Math.abs(offset);
                    if (tempOffset == 0) {
                        markPaint.setColor(highlightColor);
                    } else if (tempOffset == 1) {
                        markPaint.setColor(fadeMarkColor);
                    } else {
                        markPaint.setColor(markColor);
                    }
                } else if (i >= 0 && i < markCount) {
                    markPaint.setColor(markColor);
                } else {
                    markPaint.setColor(outOfRangeMarkColor);
                }
                if (wheelType != WheelType.VALUES_ONLY) {
                    if (offset == 0) {
                        // center mark
                        markPaint.setStrokeWidth(centerMarkWidth);
                        canvas.drawLine(ox, topSpace, ox, topSpace + markHeight, markPaint);
                    } else {
                        // other small mark
                        if (wheelType == WheelType.MARKER_MICRO_LINES_AND_VALUES) {
                            markPaint.setStrokeWidth(markWidth);
                            canvas.drawLine(ox, topSpace + smallMarkShrinkY, ox, topSpace + markHeight - smallMarkShrinkY, markPaint);
                        }
                    }
                }
            }
            float deltaY;
            if (wheelType == WheelType.VALUES_ONLY) {
                deltaY = topSpace + cursorHeight + bottomSpace;
            } else {
                deltaY = height - bottomSpace;
            }

            // mark text
            if (markCount > 0 && i >= 0 && i < markCount) {
                CharSequence temp = itemList.get(i);
                if (centerIndex == i) {
                    markTextPaint.setColor(highlightColor);

                    markTextPaint.setTextSize(centerTextSize);
                    if (!TextUtils.isEmpty(additionCenterMark)) {
                        float off = additionCenterMarkWidth / 2f;
                        float tsize = markTextPaint.measureText(temp, 0, temp.length());
                        canvas.drawText(temp, 0, temp.length(), x - off, deltaY, markTextPaint);
                        markTextPaint.setTextSize(normalTextSize);
                        canvas.drawText(additionCenterMark, x + tsize / 2f, deltaY, markTextPaint);
                    } else {
                        canvas.drawText(temp, 0, temp.length(), x, deltaY, markTextPaint);
                    }
                } else {
                    markTextPaint.setColor(markTextColor);
                    markTextPaint.setTextSize(normalTextSize);
                    canvas.drawText(temp, 0, temp.length(), x, deltaY, markTextPaint);
                }
            }

            x += intervalDis;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (itemList == null || itemList.size() == 0 || !isEnabled()) {
            return false;
        }
        boolean ret = mGestureDetectorCompat.onTouchEvent(event);
        if (!fling && MotionEvent.ACTION_UP == event.getAction()) {
            autoSettle();
            ret = true;
        }
        return ret || super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            refreshCenter();
            invalidate();
        } else {
            if (fling) {
                fling = false;
                autoSettle();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

    }

    private void autoSettle() {
        int sx = getScrollX();
        float dx = centerIndex * intervalDis - sx - maxOverScrollDistance;
        scroller.startScroll(sx, 0, (int) dx, 0);
        postInvalidate();
        if (lastSelectedIndex != centerIndex) {
            lastSelectedIndex = centerIndex;
            if (null != onWheelItemSelectedListener) {
                onWheelItemSelectedListener.onWheelItemSelected(this, centerIndex);
            }
        }
    }

    /**
     * limit center index in bounds.
     */
    private int safeCenter(int center) {
        if (center < minSelectableIndex) {
            center = minSelectableIndex;
        } else if (center > maxSelectableIndex) {
            center = maxSelectableIndex;
        }
        return center;
    }

    private void refreshCenter(int offsetX) {
        int offset = (int) (offsetX + maxOverScrollDistance);
        int tempIndex = Math.round(offset / intervalDis);
        tempIndex = safeCenter(tempIndex);
        if (centerIndex == tempIndex) {
            return;
        }
        centerIndex = tempIndex;
        if (null != onWheelItemSelectedListener) {
            onWheelItemSelectedListener.onWheelItemChanged(this, centerIndex);
        }
    }

    private void refreshCenter() {
        refreshCenter(getScrollX());
    }


    @Override
    public boolean onDown(MotionEvent e) {
        if (!scroller.isFinished()) {
            scroller.forceFinished(false);
        }
        fling = false;
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        playSoundEffect(SoundEffectConstants.CLICK);
        refreshCenter((int) (getScrollX() + e.getX() - maxOverScrollDistance));
        autoSettle();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Empty
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float dis = distanceX;
        float scrollX = getScrollX();
        if (scrollX < minSelectableIndex * intervalDis - 2 * maxOverScrollDistance) {
            dis = 0;
        } else if (scrollX < minSelectableIndex * intervalDis - maxOverScrollDistance) {
            dis = distanceX / 4.f;
        } else if (scrollX > contentRectF.width() - (markCount - maxSelectableIndex - 1) * intervalDis) {
            dis = 0;
        } else if (scrollX > contentRectF.width() - (markCount - maxSelectableIndex - 1) * intervalDis - maxOverScrollDistance) {
            dis = distanceX / 4.f;
        }
        scrollBy((int) dis, 0);
        refreshCenter();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float scrollX = getScrollX();
        if (scrollX < -maxOverScrollDistance + minSelectableIndex * intervalDis || scrollX > contentRectF.width() - maxOverScrollDistance - (markCount - 1 - maxSelectableIndex) * intervalDis) {
            return false;
        } else {
            fling = true;
            fling((int) -velocityX, 0);
            return true;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.index = getSelectedPosition();
        ss.min = minSelectableIndex;
        ss.max = maxSelectableIndex;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        minSelectableIndex = ss.min;
        maxSelectableIndex = ss.max;
        selectIndex(ss.index);
        requestLayout();
    }

    /**
     * Wheel interaction listener
     */
    public interface OnWheelItemSelectedListener {
        /**
         * Called when wheel is changed
         *
         * @param wheelView instance of Wheelview
         * @param position  current position integer value
         */
        void onWheelItemChanged(WheelView wheelView, int position);

        /**
         * Called when wheel item is selected
         *
         * @param wheelView instance of Wheelview
         * @param position  current position integer value
         */
        void onWheelItemSelected(WheelView wheelView, int position);
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int index;
        int min;
        int max;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            index = in.readInt();
            min = in.readInt();
            max = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(index);
            out.writeInt(min);
            out.writeInt(max);
        }

        @Override
        @NonNull
        public String toString() {
            return "WheelView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " index=" + index + " min=" + min + " max=" + max + "}";
        }
    }
    //endregion

    //region public methods

    /**
     * Set item selected using index in list
     *
     * @param index int value
     */
    public void selectIndex(@IntRange(from = 0) int index) {
        centerIndex = index;
        post(() -> {
            scrollTo((int) (centerIndex * intervalDis - maxOverScrollDistance), 0);
            invalidate();
            refreshCenter();
        });
    }

    /**
     * Set item selected
     *
     * @param item String value of item to be selected
     */
    public void selectItem(@NonNull String item) {
        for (String value : itemList) {
            if (item.equals(value)) {
                selectIndex(itemList.indexOf(value));
            }
        }
    }


    /**
     * Set the style of wheel view
     *
     * @param resourceId of style
     */
    public void setStyle(@StyleRes int resourceId) {
        TypedArray typedArray = getContext().obtainStyledAttributes(resourceId, R.styleable.WheelView);
        if (typedArray != null) {
            readAttributes(typedArray);
            typedArray.recycle();
        }

        initPaint();
        invalidate();
    }

    /**
     * Get the additional marker string for selected value
     *
     * @return String representing additional mark
     */
    @NonNull
    public String getAdditionCenterMark() {
        return additionCenterMark;
    }

    /**
     * Set additional marker string for selected value
     *
     * @param additionCenterMark String
     */
    public void setAdditionCenterMark(@Nullable String additionCenterMark) {
        this.additionCenterMark = additionCenterMark;
        calcIntervalDis();
        invalidate();
    }

    /**
     * Set the color of the markers
     *
     * @param color int value
     */
    public void setMarkColor(@ColorInt int color) {
        markColor = color;
        invalidate();
    }

    /**
     * Get the current color of the markers
     *
     * @return int color
     */
    @ColorInt
    public int getMarkColor() {
        return markColor;
    }

    /**
     * Set the ratio between small to large markers
     *
     * @param ratio float value
     */
    public void setMarkRatio(@Dimension float ratio) {
        markRatio = ratio;
        invalidate();
    }

    /**
     * Get the ratio between small to large markers
     *
     * @return float value
     */
    @Dimension
    public float getMarkRatio() {
        return markRatio;
    }

    /**
     * Set the height of the cursor
     *
     * @param size float value
     */
    public void setCursorHeight(@Dimension float size) {
        cursorHeight = size;
        topSpace = cursorHeight + getResources().getDimension(R.dimen.uxsdk_wheel_view_top_padding);
        invalidate();
    }

    /**
     * Set the text size of the wheel items
     *
     * @param size float value
     */
    public void setNormalTextSize(@Dimension float size) {
        normalTextSize = size;
        invalidate();
    }

    /**
     * Get the text size of the wheel items
     *
     * @return float text size
     */
    @Dimension
    public float getNormalTextSize() {
        return normalTextSize;
    }

    /**
     * Set the text size of the selected item
     *
     * @param size float value
     */
    public void setCenterTextSize(@Dimension float size) {
        centerTextSize = size;
        invalidate();
    }

    /**
     * Get the text size of the selected item
     *
     * @return float text size
     */
    @Dimension
    public float getCenterTextSize() {
        return centerTextSize;
    }

    /**
     * Set the color used for items that cannot be selected
     *
     * @param color integer value
     */
    public void setOutOfRangeMarkColor(@ColorInt int color) {
        outOfRangeMarkColor = color;
        invalidate();
    }

    /**
     * Get the color used for items that cannot be selected
     *
     * @return color integer value
     */
    @ColorInt
    public int getOutOfRangeMarkColor() {
        return outOfRangeMarkColor;
    }

    /**
     * Set the color used to highlight selected item
     *
     * @param color integer value
     */
    public void setHighlightColor(@ColorInt int color) {
        highlightColor = color;
        invalidate();
    }

    /**
     * Get the color used to highlight selected item
     *
     * @return integer color value
     */
    @ColorInt
    public int getHighlightColor() {
        return highlightColor;
    }

    /**
     * Set index of the item that the wheel should scroll to
     *
     * @param index color value
     */
    public void smoothSelectIndex(@IntRange(from = 0) int index) {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
        int deltaIndex = index - centerIndex;
        scroller.startScroll(getScrollX(), 0, (int) (deltaIndex * intervalDis), 0);
        invalidate();
    }

    /**
     * Set the type of the wheel
     *
     * @param wheelType instance of {@link WheelType}
     */
    public void setWheelType(@NonNull WheelType wheelType) {
        this.wheelType = wheelType;
        invalidate();
    }

    /**
     * Get the current type of the wheel
     *
     * @return instance of {@link WheelType}
     */
    @NonNull
    public WheelType getWheelType() {
        return wheelType;
    }

    /**
     * Get the current min selectable index
     *
     * @return integer value
     */
    public int getMinSelectableIndex() {
        return minSelectableIndex;
    }

    /**
     * Set the index of the item that can be the min possible selection
     * If param is less than min selectable value it will default to min selectable value
     * If param is greater than count of items it will default to the index of last item
     *
     * @param minSelectableIndex int representing index in the list of items
     */
    public void setMinSelectableIndex(@IntRange(from = 0) int minSelectableIndex) {
        if (minSelectableIndex > maxSelectableIndex) {
            minSelectableIndex = maxSelectableIndex;
        }
        this.minSelectableIndex = minSelectableIndex;
        int afterCenter = safeCenter(centerIndex);
        if (afterCenter != centerIndex) {
            selectIndex(afterCenter);
        }
    }

    /**
     * Get the current max selectable index
     *
     * @return integer value
     */
    public int getMaxSelectableIndex() {
        return maxSelectableIndex;
    }

    /**
     * Set the index of the item that can be the max possible selection
     * If param is less than min selectable value it will default to min selectable value
     * If param is greater than count of items it will default to the index of last item
     *
     * @param maxSelectableIndex int representing index in the list of items
     */
    public void setMaxSelectableIndex(int maxSelectableIndex) {
        if (maxSelectableIndex < minSelectableIndex) {
            maxSelectableIndex = minSelectableIndex;
        }
        this.maxSelectableIndex = maxSelectableIndex;
        int afterCenter = safeCenter(centerIndex);
        if (afterCenter != centerIndex) {
            selectIndex(afterCenter);
        }
    }

    /**
     * Get the list of items currently displayed on the wheel
     *
     * @return List of Strings
     */
    @Nullable
    public List<String> getItems() {
        return itemList;
    }

    /**
     * Set the list of items that the wheel should be displaying
     *
     * @param items List of Strings
     */
    public void setItems(@NonNull List<String> items) {
        if (itemList == null) {
            itemList = new ArrayList<>();
        } else {
            itemList.clear();
        }
        itemList.addAll(items);
        markCount = null == itemList ? 0 : itemList.size();
        if (markCount > 0) {
            minSelectableIndex = Math.max(minSelectableIndex, 0);
            maxSelectableIndex = Math.min(maxSelectableIndex, markCount - 1);
        }
        contentRectF.set(0, 0, (markCount - 1) * intervalDis, getMeasuredHeight());
        centerIndex = Math.min(centerIndex, markCount);
        calcIntervalDis();
        invalidate();
    }

    /**
     * Get index of item currently selected
     *
     * @return integer value
     */
    public int getSelectedPosition() {
        return centerIndex;
    }

    /**
     * Set the item select listener
     *
     * @param onWheelItemSelectedListener instance of {@link OnWheelItemSelectedListener}
     */
    public void setOnWheelItemSelectedListener(@NonNull OnWheelItemSelectedListener onWheelItemSelectedListener) {
        this.onWheelItemSelectedListener = onWheelItemSelectedListener;
    }

    /**
     * Wheel Type denotes the appearance of the wheel.
     */
    public enum WheelType {
        /**
         * The wheel will show only values with no marker lines
         * <p>
         * 1  2  3  5   6  7   8
         */
        VALUES_ONLY(0),
        /**
         * The wheel will show values with marker lines
         * <p>
         * |  |  |  |  |  |  |
         * 1  2  3  5  6  7  8
         */
        MARKER_LINES_AND_VALUES(1),
        /**
         * The wheel will show values, marker lines and micro lines
         * <p>
         * |..|..|..|..|..|..|
         * 1  2  3  5  6  7  8
         */
        MARKER_MICRO_LINES_AND_VALUES(2);

        private int index;

        WheelType(int index) {
            this.index = index;
        }

        private static WheelType[] values;

        public static WheelType[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        @NonNull
        public static WheelType find(@IntRange(from = 0, to = 3) int index) {
            for (WheelType wheelType : WheelType.getValues()) {
                if (wheelType.getIndex() == index) {
                    return wheelType;
                }
            }

            return VALUES_ONLY;
        }

        public int getIndex() {
            return index;
        }
    }

    //endregion
}
