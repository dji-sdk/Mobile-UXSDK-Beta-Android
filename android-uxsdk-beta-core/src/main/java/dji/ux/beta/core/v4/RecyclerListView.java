package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Recycler view with additional functionality
 */

public class RecyclerListView extends RecyclerView {
    private boolean interceptTouchEvent = true;

    public RecyclerListView(Context context) {
        this(context, null, 0);
    }

    public RecyclerListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public RecyclerListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (!interceptTouchEvent) {
            return false;
        }
        return super.onInterceptTouchEvent(e);
    }

    public boolean isInterceptTouchEvent() {
        return interceptTouchEvent;
    }

    public void setInterceptTouchEvent(boolean interceptTouchEvent) {
        this.interceptTouchEvent = interceptTouchEvent;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        // Remove flickering animation
        getItemAnimator().setChangeDuration(0);
        setLayoutManager(new LinearLayoutManager(getContext()));
        setHasFixedSize(true);
        //Fixed crash on IndexOutOfBoundsException: Inconsistency detected.
        swapAdapter(adapter, true);
    }

    
}
