package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import dji.ux.beta.core.R;


/**
 * Abstract class for camera settings
 */
public abstract class CameraSettingListView extends SimpleFrameLayoutWidget implements RecyclerAdapter.ChangeListener {

    //region Properties
    protected RecyclerAdapter adapter;
    protected ParentChildrenViewAnimator.RootViewCallback rootViewCallback;
    protected RecyclerListView rootListView;
    protected TextView titleView;
    protected View childView;
    protected boolean isCameraBusy;

    private Animation leftInAnimation;
    private Animation leftOutAnimation;
    private Animation rightInAnimation;
    private Animation rightOutAnimation;
    //endregion

    //region Constructors
    public CameraSettingListView(Context context, AttributeSet attributeSet, int style) {
        super(context, attributeSet, style);
    }

    public void setTitleTextView(TextView textView) {
        titleView = textView;
    }

    public void setRootViewCallback(ParentChildrenViewAnimator.RootViewCallback rootViewCallback) {
        this.rootViewCallback = rootViewCallback;
    }
    //endregion

    //region enum
    public enum State {
        VISIBLE,
        DISABLED,
        HIDDEN
    }
    //endregion

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.uxsdk_widget_list_view, this, true);
        rootListView = (RecyclerListView) findViewById(R.id.recycle_list_view_content);
        initRootView();
        initAdapter();
        initAnimations();
    }

    private void initRootView() {
        setRootViewCallback(new ParentChildrenViewAnimator.RootViewCallback() {
            @Override
            public void onRootViewIsShown(boolean isShown) {
                if (rootViewCallback != null) {
                    rootViewCallback.onRootViewIsShown(isShown);
                }
            }
        });
    }

    public void setRootViewIsShown(boolean state) {
        if (rootViewCallback != null) {
            rootViewCallback.onRootViewIsShown(state);
        }
        if (state) {
            rootListView.setVisibility(VISIBLE);
        } else {
            rootListView.setVisibility(INVISIBLE);
        }
    }

    protected void initAdapter() {
        adapter = new RecyclerAdapter(this);

        // Prepare the data for the list view.
        onInitData();

        if(!isInEditMode()){
            onUpdateDefaultSetting();
        }

        rootListView.setAdapter(adapter);
    }

    protected ListItem addItem(ListItem.ItemProperty property) {
        ListItem model = new ListItem();
        model.setTitle(getResources().getString(property.itemTitleResId));
        model.itemType = property.type;
        adapter.add(model);
        return model;
    }

    private void initAnimations() {
        leftInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_in);
        leftOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_out);
        rightInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_in);
        rightOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_out);
    }
    // Update item specified at index

    private void notifyItemChanged(final int index) {
        if (index >= 0) {
            post(() -> adapter.notifyItemChanged(index));
        }
    }

    protected void updateItem(ListItem item, State state) {
        item.setEnabled(state == State.VISIBLE);
        item.setVisible(state != State.HIDDEN);
        notifyItemChanged(adapter.findIndexByItem(item));
    }

    protected void updateItem(ListItem item, int value, int resource) {
        if (item.valueId != value || item.valueImgResId != resource) {
            item.valueId = value;
            item.valueImgResId = resource;
            notifyItemChanged(adapter.findIndexByItem(item));
        }
    }

    protected void updateItem(ListItem item, int value, int resource, State state) {
        if (item.valueId != value || item.valueImgResId != resource || state == State.HIDDEN) {
            item.valueId = value;
            item.valueImgResId = resource;
            notifyItemChanged(adapter.findIndexByItem(item));
        }
    }

    protected void updateItem(ListItem item, int valueId, String valueStr, int resource) {
        if (item.valueId != valueId || item.valueImgResId != resource) {
            item.valueId = valueId;
            item.setValue(valueStr);
            item.valueImgResId = resource;
            notifyItemChanged(adapter.findIndexByItem(item));
        }
    }

    protected void updateItemTitle(ListItem item, String title) {
        item.setTitle(title);
        notifyItemChanged(adapter.findIndexByItem(item));
    }

    //endregion

    //region List view action
    @Override
    abstract public void updateSelectedItem(ListItem item, View view);

    protected void removeChildViewIfNeeded() {
        if (childView != null) {
            if (childView.getParent() instanceof ViewGroup) {
                ((ViewGroup) (childView.getParent())).removeView(childView);
                childView = null;
            }
        }
    }

    protected void showChildView(int keyIndex, int subKeyIndex) {
        if (childView != null) {
            if (childView instanceof SimpleFrameLayoutWidget) {
                ((SimpleFrameLayoutWidget) childView).updateKeyOnIndex(keyIndex, subKeyIndex);
            } else if (childView instanceof FrameLayoutWidget) {
                ((FrameLayoutWidget) childView).updateKeyOnIndex(keyIndex, subKeyIndex);
            }

            ViewGroup.LayoutParams params = rootListView.getLayoutParams();
            childView.setLayoutParams(params);

            childView.startAnimation(rightInAnimation);
            titleView.startAnimation(rightInAnimation);
            rootListView.startAnimation(leftOutAnimation);
            addView(childView);
            setRootViewIsShown(false);
            HeaderTitleClient header = (HeaderTitleClient) childView;
            header.updateTitle(titleView);
        }
    }

    protected void showChildView() {
        showChildView(keyIndex, subKeyIndex);
    }

    public void onBackButtonClicked() {
        if (childView != null) {
            if (childView instanceof ListViewWidget) {
                if (((ListViewWidget) childView).handleBackPressed()) {
                    return;
                }
            }
            childView.startAnimation(rightOutAnimation);
            titleView.startAnimation(rightOutAnimation);
            rootListView.startAnimation(leftInAnimation);
            setRootViewIsShown(true);
            removeView(childView);
            childView = null;
        }
    }

    protected void updateItemState(ListItem listItem, @Nullable Object[] range) {
        if (range == null) {
            updateItem(listItem, State.HIDDEN);
        } else if (range.length <= 1) {
            updateItem(listItem, range.length == 1 ? State.DISABLED : State.HIDDEN);
        } else if (isCameraBusy) {
            updateItem(listItem, State.DISABLED);
        } else {
            updateItem(listItem, State.VISIBLE);
        }
    }

    protected void updateItemState(ListItem listItem) {
        if (isCameraBusy) {
            updateItem(listItem, State.DISABLED);
        } else {
            updateItem(listItem, State.VISIBLE);
        }
    }
    //endregion

    //region Abstract Methods
    protected abstract void onInitData();

    protected abstract void onUpdateDefaultSetting();

    //endregion
}