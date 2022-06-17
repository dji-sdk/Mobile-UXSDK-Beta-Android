package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import dji.ux.beta.core.R;

public class TabBarView extends FrameLayout {

    private ImageView imgTabIndicator;

    public interface OnStageChangeCallback {
        void onStageChange(final int stage);
    }

    //region Properties
    static final String TAG = "TabBarView";

    //region Default Constructors
    public TabBarView(Context context) {
        super(context);
        init(context);
    }

    public TabBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TabBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    //endregion

    protected ImageView[] imgTabs = new ImageView[0];

    protected OnStageChangeCallback stageChangedCallback = null;
    protected View.OnClickListener widgetClickListener = null;

    public int getCurrentTabIndex() {
        return currentTabIndex;
    }

    protected int currentTabIndex = -1;
    private int switchTime;
    private int timeMultiplier = 1;
    private int indicatorWidth;

    protected boolean supportAnim = true;

    private void init(Context context) {
        switchTime = context.getResources().getInteger(R.integer.uxsdk_translate_duration);
    }

    public void setStageChangedCallback(OnStageChangeCallback stageChangedCallback) {
        this.stageChangedCallback = stageChangedCallback;
    }

    public void initTabBar(final ImageView[] imageViews, final ImageView indicator, final boolean anim) {
        supportAnim = anim;
        imgTabs = imageViews;
        imgTabIndicator = indicator;

        initMembers();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (imgTabs != null) {
            indicatorWidth = this.getMeasuredWidth() / imgTabs.length;
        }
    }

    protected void switchToTabIndicator(final int tabIndex) {
        if (supportAnim) {
            imgTabIndicator.animate()
                    .translationX(tabIndex * indicatorWidth)
                    .setDuration(switchTime * timeMultiplier)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            imgTabIndicator.setTranslationX(tabIndex * indicatorWidth);
        } else {
            imgTabIndicator.setTranslationX(tabIndex * indicatorWidth);
        }
    }

    protected void switchTabSelected(final int current, final int last) {

        for (int i = 0; i < imgTabs.length; i++) {
            imgTabs[i].setSelected(current == i);
        }
    }

    public void handleTabChanged(final int tabIndex) {
        if (tabIndex == currentTabIndex) {
            return;
        }

        final int lastIndex = currentTabIndex;
        currentTabIndex = tabIndex;

        switchTabSelected(tabIndex, lastIndex);
        timeMultiplier = Math.abs(tabIndex - lastIndex);

        switchToTabIndicator(tabIndex);

        if (stageChangedCallback != null) {
            stageChangedCallback.onStageChange(tabIndex);
        }
    }

    protected void initMembers() {
        if (isInEditMode()) {
            return;
        }

        widgetClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0, length = imgTabs.length; i < length; i++) {
                    if (v == imgTabs[i]) {
                        if (i != currentTabIndex) {
                            handleTabChanged(i);
                        }
                        break;
                    }
                }
            }
        };

        for (int i = 0, length = imgTabs.length; i < length; i++) {
            imgTabs[i].setOnClickListener(widgetClickListener);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
