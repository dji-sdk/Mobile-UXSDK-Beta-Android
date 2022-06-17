package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import dji.thirdparty.rx.subscriptions.CompositeSubscription;

/**
 * Base class for non-widget View that needs to use Appearance
 */
public abstract class BaseFrameLayout extends FrameLayout implements TrackableWidget {

    //region Properties
    protected static final String TAG = "BaseFrameLayout";
    protected Context context;
    protected CompositeSubscription subscription = new CompositeSubscription();
    protected ArrayList<View> viewList;
    //endregion

    //region To be implemented
    protected abstract BaseWidgetAppearances getWidgetAppearances();
    //endregion

    //region View Life-cycle
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        BaseWidgetAppearances widgetAppearances = getWidgetAppearances();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if ((width == 0 && height == 0) || widgetAppearances == null||viewList ==null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            if (width == 0) {
                width = widgetAppearances.getWidthFromHeight(height);
            }
            if (height == 0) {
                height = widgetAppearances.getHeightFromWidth(width);
            }
            int widthConstraints = getPaddingLeft() + getPaddingRight();
            int heightConstraints = getPaddingTop() + getPaddingBottom();
            widgetAppearances.calculateAppearance(width, height);
            Appearance[] elementAppearances = widgetAppearances.getElementAppearances();

            for (int i = 0; i < viewList.size(); i++) {
                final Appearance childAppearance = elementAppearances[i];
                final View childView = viewList.get(i);
                // Let child do measure
                measureChild(widgetAppearances,
                             childView,
                             childAppearance,
                             widthMeasureSpec,
                             widthConstraints,
                             heightMeasureSpec,
                             heightConstraints);
                // Adjust TextView Size
                if (childView instanceof TextView && childAppearance instanceof TextAppearance) {
                    ((TextAppearance) childAppearance).adjustTextViewSize((TextView) childView);
                }
            }
            setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        BaseWidgetAppearances widgetAppearances = getWidgetAppearances();
        if (widgetAppearances == null||viewList==null) {
            super.onLayout(changed, l, t, r, b);
        } else {
            Appearance[] elementAppearances = widgetAppearances.getElementAppearances();
            for (int i = 0; i < viewList.size(); i++) {
                final Appearance childAppearance = elementAppearances[i];
                final View childView = viewList.get(i);
                widgetAppearances.adjustPosition(childView, childAppearance);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        super.onDetachedFromWindow();
    }

    private void measureChild(BaseWidgetAppearances widgetAppearances,
                              View child,
                              Appearance childAppearance,
                              int parentWidthMeasureSpec,
                              int widthUsed,
                              int parentHeightMeasureSpec,
                              int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int width = widgetAppearances.getAbsoluteWidth(childAppearance);
        final int height = widgetAppearances.getAbsoluteHeight(childAppearance);

        int childWidthMeasureSpec =
            getChildMeasureSpec(parentWidthMeasureSpec, widthUsed + lp.leftMargin + lp.rightMargin, width);

        int childHeightMeasureSpec =
            getChildMeasureSpec(parentHeightMeasureSpec, heightUsed + lp.topMargin + lp.bottomMargin, height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        BaseWidgetAppearances widgetAppearances = getWidgetAppearances();
        if (widgetAppearances != null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = widgetAppearances.inflate(inflater, this);
            if (view != null) {
                viewList = new ArrayList<>();
                Appearance[] elementAppearances = widgetAppearances.getElementAppearances();
                for (Appearance viewAppearance : elementAppearances) {
                    final View eachChild = view.findViewById(viewAppearance.getViewID());
                    viewList.add(eachChild);
                }
            }
        }
    }

    @Override
    public boolean shouldTrack() {
        return true;
    }
    //endregion

    //region Default Constructor
    public BaseFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initView(context, attrs, defStyle);
    }
    //endregion

    //region Logic UI thread update.

    public float aspectRatio() {
        BaseWidgetAppearances widgetAppearances = getWidgetAppearances();
        if (widgetAppearances != null) {
            return widgetAppearances.aspectRatio();
        } else {
            return 1;
        }
    }

    //endregion
}
