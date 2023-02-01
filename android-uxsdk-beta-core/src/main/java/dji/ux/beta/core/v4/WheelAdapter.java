/**
 * @filename		: WheelAdapter.java
 * @package			: dji.pilot.fpv.camera.more
 * @date			: 2015年7月14日 下午4:06:18
 * @author			: kevin.he
 * 
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import dji.ux.beta.core.R;


public class WheelAdapter<T> extends AbstractWheelTextAdapter {

    private T mItems[];

    private final int[] mInterval = new int[] {
        Integer.MIN_VALUE, Integer.MAX_VALUE
    };
    private int mOverColor = 0;
    private int mNormalColor = 0;
    private int mSelectColor = 0;
    private int mDisableColor = 0;
    private int mMaxPos = Integer.MAX_VALUE;
    private int mCurPosition = -1;
    
    private boolean mEnable = true;

    public WheelAdapter(Context context, T[] items) {
        super(context);
        mItems = items;
        mMaxPos = items.length;
        mOverColor = context.getResources().getColor(R.color.uxsdk_red);
        mNormalColor = context.getResources().getColor(R.color.uxsdk_white);
        mDisableColor = context.getResources().getColor(R.color.uxsdk_white);
        mSelectColor = context.getResources().getColor(R.color.uxsdk_blue_highlight);
    }
    
    public void setEnable(final boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
            notifyDataChangedEvent();
        }
    }
    
    public void setDatas(T[] items) {
        mItems = items;
        mMaxPos = items.length;
        notifyDataInvalidatedEvent();
    }

    /** 
     * Description  : 设置当前选中项的颜色
     * @author      : kevin.he
     * @date        : 2015年7月14日 下午4:25:03
     * @param color resource id 
     */
    public void setNormalColor(int color){
        mNormalColor = context.getResources().getColor(color);
    }
    
    public void setSelectColor() {
        mSelectColor = context.getResources().getColor(R.color.uxsdk_blue);
    }
    
    public void setSelectColor(int color) {
        mSelectColor = color;
    }

    public void setInterval(final int min, final int max) {
        if (min != mInterval[0] || max != mInterval[1]) {
            mInterval[0] = min;
            mInterval[1] = max;
            notifyDataChangedEvent();
        }
    }

    public void setCurPos(final int curPos) {
        if (mCurPosition != curPos) {
            mCurPosition = curPos;
            notifyDataChangedEvent();
        }
    }

    public void resetInterval() {
        if (mInterval[0] != Integer.MIN_VALUE || mInterval[1] != Integer.MAX_VALUE) {
            mInterval[0] = Integer.MIN_VALUE;
            mInterval[1] = Integer.MAX_VALUE;
            notifyDataChangedEvent();
        }
    }

    public void setMaxPos(final int maxPos) {
        if (mMaxPos != maxPos) {
            mMaxPos = maxPos;
            notifyDataChangedEvent();
        }
    }

    @Override
    public int getItemsCount() {
        if (mMaxPos != -1) {
            return mMaxPos;
        } else {
            return mItems.length;
        }
    }

    @Override
    protected CharSequence getItemText(int index) {
        if (index >= 0 && index < getItemsCount()) {
            T item = mItems[index];
            if (item instanceof CharSequence) {
                return (CharSequence) item;
            }
            return item.toString();
        }
        return null;
    }

    @Override
    public View getEmptyItem(View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public View getItem(int index, View convertView, ViewGroup parent) {
        if (index >= 0 && index < getItemsCount()) {
            if (convertView == null) {
                convertView = getView(itemResourceId, parent);
            }
            TextView textView = getTextView(convertView, itemTextResourceId);
            if (textView != null) {
                CharSequence text = getItemText(index);
                if (text == null) {
                    text = "";
                }
                textView.setText(text);
                configureTextView(textView, index);
            }
            return convertView;
        }
        return null;
    }

    protected void configureTextView(TextView txt, final int index) {
        if (!mEnable) {
            txt.setTextColor(mDisableColor);
        } else if (index < mInterval[0] || index > mInterval[1]) {
            txt.setTextColor(mOverColor);
        } else if (mSelectColor != 0 && index == mCurPosition) {
            txt.setTextColor(mSelectColor);
        } else {
            txt.setTextColor(mNormalColor);
        }
    }

    private TextView getTextView(View view, int textResource) {
        return (TextView) view.findViewById(textResource);
    }

    private View getView(int resource, ViewGroup parent) {
        return inflater.inflate(resource, parent, false);
    }
}
