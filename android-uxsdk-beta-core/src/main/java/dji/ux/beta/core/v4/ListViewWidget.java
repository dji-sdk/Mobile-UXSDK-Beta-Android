package dji.ux.beta.core.v4;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dji.common.bus.UXSDKEventBus;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;


/**
 * Abstract class for any Widget that should be a View
 */

public abstract class ListViewWidget extends SimpleFrameLayoutWidget
    implements HeaderTitleClient, RecyclerAdapter.ChangeListener,RecyclerAdapter.KeyboardActionListener {

    //region  Properties
    protected RecyclerListView contentList;
    protected RecyclerAdapter adapter;
    protected String[] itemNameArray;
    protected TypedArray itemImageIdArray;
    protected int currentPosition;
    protected List<View> childViews = new ArrayList<>();
    protected TextView titleView;

    private Animation leftInAnimation;
    private Animation leftOutAnimation;
    private Animation rightInAnimation;
    private Animation rightOutAnimation;
    //endregion
    //region enum
    public enum State {
        VISIBLE,
        DISABLED,
        HIDDEN
    }
    //endregion

    //region  Life Cycle
    public ListViewWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hideSoftKeyboard();
    }
    //endregion

    //region List View methods
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.uxsdk_widget_list_view, this);
        contentList = (RecyclerListView) view.findViewById(R.id.recycle_list_view_content);
        initAnimations();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            hideSoftKeyboard();
        }
    }

    @Override
    public void updateTitle(TextView textTitle) {
        titleView = textTitle;
    }

    protected void initAdapter(int[] range) {
        if (range == null) {
            return;
        }

        if (itemNameArray == null) {
            return;
        }

        // todo Fix ArrayOutOfIndexException: the old adapter can receive onClick event.
        if (adapter != null) {
            adapter.clear();
        } else {
            adapter = new RecyclerAdapter(this);
            adapter.setKeyboardActionListener(this);
        }

        for (int aRange : range) {
            ListItem model;
            if (aRange < itemNameArray.length) {
                if (itemImageIdArray != null && itemImageIdArray.hasValue(aRange)) {
                    model = ListItem.createCameraSublistImageItem(aRange,
                                                         itemNameArray[aRange],
                                                         "",
                                                         itemImageIdArray.getResourceId(aRange, 0));
                } else {
                    model = ListItem.createCameraSublistItem(aRange, itemNameArray[aRange], "");
                }
            } else {
                model = ListItem.createCameraSublistItem(aRange, "", "");
            }
            adapter.add(model);
        }

        contentList.setAdapter(adapter);
    }

    //endregion

    protected void updateItem(ListItem item, State state) {
        item.setEnabled(state == State.VISIBLE);
        item.setVisible(state != State.HIDDEN);
        if (state == State.DISABLED) {
            item.valueImgResId = 0;
            item.setValue(getResources().getString(R.string.uxsdk_string_default_value));
        } else {
            notifyItemChanged(adapter.findIndexByItem(item));
        }
    }
    protected void updateItem(ListItem item, String value) {
        item.setValue(value);
        notifyItemChanged(adapter.findIndexByItem(item));
    }

    private void notifyItemChanged(final int index) {
        if (index >= 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyItemChanged(index);
                }
            });
        }
    }

    protected void initAnimations() {
        leftInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_in);
        leftOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_out);
        rightInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_in);
        rightOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_out);
    }

    public void pushChild(ListViewWidget childView) {
        if (childView != null) {
            childView.setKeyIndex(keyIndex);
            childView.updateTitle(titleView);

            ViewGroup.LayoutParams params = getLayoutParams();
            childView.setLayoutParams(params);

            childView.startAnimation(rightInAnimation);
            addView(childView);
            if (!childViews.isEmpty()) {
                childViews.get(childViews.size() -1 ).startAnimation(leftOutAnimation);
                childViews.get(childViews.size() -1 ).setVisibility(INVISIBLE);
            } else {
                contentList.startAnimation(leftOutAnimation);
                contentList.setVisibility(INVISIBLE);
            }
            childViews.add(childView);
            hideSoftKeyboard();
        }
    }

    public void popChild() {
        if (!childViews.isEmpty()) {
            View childView = childViews.remove(childViews.size() - 1);
            childView.startAnimation(rightOutAnimation);
            removeView(childView);
            if (!childViews.isEmpty()) {
                childViews.get(childViews.size() - 1).startAnimation(leftInAnimation);
                childViews.get(childViews.size() - 1).setVisibility(VISIBLE);
            } else {
                contentList.startAnimation(leftInAnimation);
                contentList.setVisibility(VISIBLE);
                updateTitle(titleView);
            }
        }
    }


    public boolean handleBackPressed() {
        if (!childViews.isEmpty()) {
            popChild();
            return true;
        } else {
            return false;
        }

    }

    protected void disableItemsWhenCameraBusy() {
        subscription.add(
                UXSDKEventBus.getInstance()
                        .register(Events.CameraBusyEvent.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Events.CameraBusyEvent>() {
                            @Override
                            public void call(Events.CameraBusyEvent event) {
                                setAllItemsEnabled(!event.isBusy());
                            }
                        })
        );
    }

    @Override
    public boolean handleKeyboardAction(View stateView, int position, int actionId, KeyEvent keyEvent) {
        return false;
    }

    private void hideSoftKeyboard() {
        if (getContext() instanceof Activity) {
            View view = ((Activity) getContext()).getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    protected void setAllItemsEnabled(boolean enabled) {
        adapter.setAllItemsEnabled(enabled);
    }
}
