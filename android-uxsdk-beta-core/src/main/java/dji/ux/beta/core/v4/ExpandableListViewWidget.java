package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import java.util.List;

import dji.common.bus.UXSDKEventBus;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.ux.beta.core.R;


/**
 * A list widget that contains expandable items
 */
public abstract class ExpandableListViewWidget extends SimpleFrameLayoutWidget implements HeaderTitleClient {
    //region  Properties
    protected ExpandableListAdapter adapter;
    protected ExpandableListView.OnGroupClickListener onGroupClickListener;
    protected OnClickListener itemClickListener;
    protected ExpandableListAdapter.OnSbChangedListener onSbChangedListener;

    protected int groupValueId = Integer.MAX_VALUE;
    protected int childValueId = Integer.MAX_VALUE;
    protected String[] itemNameArray;
    protected TypedArray itemImageIdArray;
    protected LinearLayout sdGroupLy;
    protected int[][] itemImgResIds;
    protected ExpandableListView contentList; // TODO: Shadows base class object
    //endregion

    //region  Default constructors
    public ExpandableListViewWidget(Context context) {
        super(context, null, 0);
    }

    public ExpandableListViewWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ExpandableListViewWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    //endregion

    //region View Life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.uxsdk_widget_expandable_list_view, this);
        initMembers();

    }

    private void initMembers() {
        onGroupClickListener = new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (!handleGroupClick(parent, v, groupPosition, id)) {
                }
                return true;
            }
        };

        itemClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                onChildViewClick(v.getTag());
            }
        };

        onSbChangedListener = new ExpandableListAdapter.OnSbChangedListener() {

            @Override
            public void onStopTrackingTouch(ExpandableGroupListItem group, ExpandableChildListItem child) {
                onChildViewInputComplete(group, child);
            }
            @Override
            public void onStartTrackingTouch(ExpandableGroupListItem group, ExpandableChildListItem child) {
                //Does not need to be implemented here
            }
            @Override
            public void onProgressChanged(ExpandableGroupListItem group, ExpandableChildListItem child) {
                //Does not need to be implemented here
            }
            @Override
            public void onPlusClicked(ExpandableGroupListItem group, ExpandableChildListItem child) {
                onChildViewInputComplete(group, child);
            }
            @Override
            public void onMinusClicked(ExpandableGroupListItem group, ExpandableChildListItem child) {
                onChildViewInputComplete(group, child);
            }
        };

        adapter = new ExpandableListAdapter(context);
        adapter.setChildClickListener(itemClickListener);
        adapter.setChildSbListener(onSbChangedListener);

        contentList = (ExpandableListView) findViewById(R.id.expandable_list_view_content);
        contentList.setGroupIndicator(null);
        contentList.setOnGroupClickListener(onGroupClickListener);
        contentList.setAdapter(adapter);
    }

    protected void updateSelected(final int beforeGroup, final int group, final int child) {
        if (null != adapter && !adapter.getDataList().isEmpty()) {
            List<ExpandableGroupListItem> expandableAdapterDatas = adapter.getDataList();
            for (int i = 0, size = expandableAdapterDatas.size(); i < size; i++) {
                final ExpandableGroupListItem gModel = expandableAdapterDatas.get(i);
                if (gModel.valueId == group) {
                    if (gModel.groupType == ExpandableGroupListItem.TYPE_GROUP_SWITCH) {
                        if (!gModel.isSwitchEnabled()) {
                            //gModel.setSwitchEnabled(false);
                            contentList.collapseGroup(i);
                        } else {
                            if (!contentList.isGroupExpanded(i)) {
                                contentList.expandGroup(i, true);
                                contentList.setSelectedGroup(i);
                            }
                            //gModel.setSwitchEnabled(true);
                        }
                    } else {
                        if (!contentList.isGroupExpanded(i)) {
                            contentList.expandGroup(i, true);
                            contentList.setSelectedGroup(i);
                        }
                        gModel.setSelected(true);
                    }

                    if (gModel.childType == ExpandableGroupListItem.TYPE_CHILD_SEEKBAR) {
                        gModel.childValueId = child;
                        gModel.childs.get(0).valueId = child;
                    } else {
                        for (int j = 0, length = gModel.childs.size(); j < length; j++) {
                            final ExpandableChildListItem cModel = gModel.childs.get(j);
                            if (cModel.valueId == child) {
                                cModel.selected = true;
                                gModel.childStr = cModel.childStr;
                                gModel.childValueId = child;
                                if (itemImgResIds != null && group < itemImgResIds.length && child < itemImgResIds[group].length) {
                                    gModel.imgResId = itemImgResIds[group][child];
                                }
                            } else {
                                cModel.selected = false;

                            }
                        }
                    }
                } else if (gModel.valueId == beforeGroup) {
                    if (gModel.groupType == ExpandableGroupListItem.TYPE_GROUP_SWITCH) {
                        if (!gModel.isSwitchEnabled()) {
                            contentList.collapseGroup(i);
                        }
                        gModel.setSelected(gModel.isSwitchEnabled());
                    } else {
                        contentList.collapseGroup(i);
                        gModel.setSelected(false);
                        for (int j = 0, length = gModel.childs.size(); j < length; j++) {
                            final ExpandableChildListItem cModel = gModel.childs.get(j);
                            cModel.selected = false;
                        }
                    }
                } else {
                    if (gModel.groupType == ExpandableGroupListItem.TYPE_GROUP_SWITCH) {
                        if (!gModel.isSwitchEnabled()) {
                            contentList.collapseGroup(i);
                        }
                        gModel.setSelected(gModel.isSwitchEnabled());
                    } else {
                        contentList.collapseGroup(i);
                        gModel.setSelected(false);
                    }
                }
            }
            adapter.notifyDataSetChanged();
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

    protected void setAllItemsEnabled(boolean enabled) {
        adapter.setAllItemsEnabled(enabled);
    }
    //endregion

    //region Overridable methods for child class
    abstract protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id);

    // Two kinds of actions for the child view.
    protected void onChildViewClick(final Object tag){

    }
    protected void onChildViewInputComplete(ExpandableGroupListItem group, ExpandableChildListItem child){
        //Should be implemented in any file that uses this ExpandableListViewWidget
    }
    //endregion
}
