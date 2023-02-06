/**
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dji.ux.beta.core.R;

import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_GROUP;
import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_GROUP_SWITCH;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    public interface OnSbChangedListener {
        void onStopTrackingTouch(ExpandableGroupListItem group, ExpandableChildListItem child);

        void onStartTrackingTouch(ExpandableGroupListItem group, ExpandableChildListItem child);

        void onProgressChanged(ExpandableGroupListItem group, ExpandableChildListItem child);

        void onPlusClicked(ExpandableGroupListItem group, ExpandableChildListItem child);

        void onMinusClicked(ExpandableGroupListItem group, ExpandableChildListItem child);
    }

    private static final int[] RESID_CHILDS = new int[]{
        R.id.child_value1, R.id.child_value2, R.id.child_value3
    };
    private static final int[] RESID_CHILD_TAGS = new int[] {
        R.id.child_tag1, R.id.child_tag2, R.id.child_tag3
    };
    private static final int MAX_CHILDS = RESID_CHILDS.length;
    private static final int INCREMENT_DECREMENT_VALUE = 1;

    private static final int[] RESID_CHILD_IMAGES = new int[] {
        R.id.child_image1, R.id.child_image2, R.id.child_image3
    };
    private static final int[] RESID_CHILD_BUTTONS = new int[] {
        R.id.child_button1, R.id.child_button2, R.id.child_button3
    };

    private static final int MAX_CHILD_IMAGES = RESID_CHILD_IMAGES.length;
    private static final int MAX_CHILD_BUTTONS = RESID_CHILD_BUTTONS.length;

    private List<ExpandableGroupListItem> dataList = null;
    private ExpandableGroupListItem selectedItem;
    private View.OnClickListener childClickListener = null;
    private OnSbChangedListener childSbListener = null;
    private SeekBar.OnSeekBarChangeListener childSbInnerListener = null;
    private boolean isSeekBarTracking;
    //endregion

    //region Listeners
    public ExpandableListAdapter(final Context context) {
        childSbInnerListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress) {
                final Object tag = seekBar.getTag();
                if (tag instanceof SbChildHolder) {
                    final SbChildHolder holder = (SbChildHolder) tag;
                    final int groupPosition = (int) (holder.childId / 1000);
                    final int childPosition = (int) (holder.childId % 1000);

                    final Object group = getGroup(groupPosition);
                    if (group instanceof ExpandableGroupListItem) {
                        final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
                        final ExpandableChildListItem child = tGroup.childs.get(childPosition);

                        if (isSeekBarTracking) {
                            if (child.argObj instanceof String) {
                                String textValue = String.valueOf(progress + child.sbMinValue) + child.argObj;
                                holder.seekBar.setText(textValue);
                            } else {
                                holder.seekBar.setText(String.valueOf(progress + child.sbMinValue));
                            }
                        }
                        childSbListener.onProgressChanged(tGroup, child);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar, int progress) {
                final Object tag = seekBar.getTag();
                isSeekBarTracking = true;
                if (tag instanceof SbChildHolder) {
                    final SbChildHolder holder = (SbChildHolder) tag;
                    final int groupPosition = (int) (holder.childId / 1000);
                    final int childPosition = (int) (holder.childId % 1000);

                    final Object group = getGroup(groupPosition);
                    if (group instanceof ExpandableGroupListItem) {
                        final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
                        final ExpandableChildListItem child = tGroup.childs.get(childPosition);
                        childSbListener.onStartTrackingTouch(tGroup, child);
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar, int progress) {
                final Object tag = seekBar.getTag();
                isSeekBarTracking = false;
                if (tag instanceof SbChildHolder) {
                    final SbChildHolder holder = (SbChildHolder) tag;
                    final int groupPosition = (int) (holder.childId / 1000);
                    final int childPosition = (int) (holder.childId % 1000);

                    final Object group = getGroup(groupPosition);
                    if (group instanceof ExpandableGroupListItem) {
                        final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
                        final ExpandableChildListItem child = tGroup.childs.get(childPosition);

                        child.valueId = progress + child.sbMinValue;
                        if (child.argObj instanceof String) {
                            holder.seekBar.setText(String.valueOf(child.valueId) + child.argObj);
                        } else {
                            holder.seekBar.setText(String.valueOf(child.valueId));
                        }
                        childSbListener.onStopTrackingTouch(tGroup, child);
                    }
                }
            }

            @Override
            public void onPlusClicked(SeekBar seekBar) {
                final Object tag = seekBar.getTag();
                if (tag instanceof SbChildHolder) {
                    final SbChildHolder holder = (SbChildHolder) tag;
                    final int groupPosition = (int) (holder.childId / 1000);
                    final int childPosition = (int) (holder.childId % 1000);

                    final Object group = getGroup(groupPosition);
                    if (group instanceof ExpandableGroupListItem) {
                        final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
                        final ExpandableChildListItem child = tGroup.childs.get(childPosition);
                        int newValue = child.valueId + INCREMENT_DECREMENT_VALUE;
                        if (newValue < child.sbMaxValue) {
                            child.valueId = newValue;
                        } else {
                            child.valueId = child.sbMaxValue;
                        }
                        if (child.argObj instanceof String) {
                            holder.seekBar.setText(String.valueOf(child.valueId) + child.argObj);
                        } else {
                            holder.seekBar.setText(String.valueOf(child.valueId));
                        }
                        childSbListener.onPlusClicked(tGroup, child);
                    }
                }
            }

            @Override
            public void onMinusClicked(SeekBar seekBar) {
                final Object tag = seekBar.getTag();
                if (tag instanceof SbChildHolder) {
                    final SbChildHolder holder = (SbChildHolder) tag;
                    final int groupPosition = (int) (holder.childId / 1000);
                    final int childPosition = (int) (holder.childId % 1000);

                    final Object group = getGroup(groupPosition);
                    if (group instanceof ExpandableGroupListItem) {
                        final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
                        final ExpandableChildListItem child = tGroup.childs.get(childPosition);
                        int newValue = seekBar.getProgress() - INCREMENT_DECREMENT_VALUE + child.sbMinValue;
                        if (newValue > child.sbMinValue) {
                            child.valueId = newValue;
                        } else {
                            child.valueId = child.sbMinValue;
                        }
                        if (child.argObj instanceof String) {
                            holder.seekBar.setText(String.valueOf(child.valueId) + child.argObj);
                        } else {
                            holder.seekBar.setText(String.valueOf(child.valueId));
                        }
                        childSbListener.onMinusClicked(tGroup, child);
                    }
                }
            }
        };
    }

    public void setDataList(final List<ExpandableGroupListItem> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    public List<ExpandableGroupListItem> getDataList(){
        return this.dataList;
    }

    public void setChildClickListener(final View.OnClickListener listener) {
        childClickListener = listener;
    }

    public void setChildSbListener(final OnSbChangedListener listener) {
        childSbListener = listener;
    }
    //endregion

    //region Override methods
    @Override
    public int getGroupCount() {
        return (dataList != null) ? dataList.size() : 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int count = 0;
        if (dataList != null && !dataList.isEmpty()) {
            if (0 <= groupPosition && groupPosition < dataList.size()) {
                final ExpandableGroupListItem group = dataList.get(groupPosition);
                count = group.childs.size();
                if (0 != count && (group.childType == ExpandableGroupListItem.TYPE_CHILD_TEXT
                    || group.childType == ExpandableGroupListItem.TYPE_CHILD_IMAGE
                    || group.childType == ExpandableGroupListItem.TYPE_CHILD_BUTTON)) {
                    count = (count - 1) / MAX_CHILDS + 1;
                }
            }
        }
        return count;
    }

    @Override
    public Object getGroup(int groupPosition) {
        ExpandableGroupListItem group = null;
        if (dataList != null && !dataList.isEmpty()) {
            if (0 <= groupPosition && groupPosition < dataList.size()) {
                group = dataList.get(groupPosition);
            }
        }
        return group;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 1000 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final Object group = getGroup(groupPosition);
        if (group instanceof ExpandableGroupListItem) {
            GroupHolder holder;

            if (null == convertView || convertView.getTag() == null) {
                holder = new GroupHolder();
                convertView = new ListGroupItemView(parent.getContext());
                holder.groupView = convertView.findViewById(R.id.expendable_group_layout);
                holder.groupItemIcon = (ImageView) convertView.findViewById(R.id.expandable_group_icon);
                holder.groupItemName = (TextView) convertView.findViewById(R.id.expandable_group_name);
                holder.groupItemValue = (TextView) convertView.findViewById(R.id.expandable_group_value);
                holder.groupItemValueBG = convertView.findViewById(R.id.expandable_group_value_bg);
                holder.groupItemSwitch = (SwitchButton) convertView.findViewById(R.id.expandable_group_switch_button);
                holder.groupItemSwitch.setEnabled(false);
                if (((ExpandableGroupListItem) group).groupType == ExpandableGroupListItem.TYPE_GROUP_SWITCH) {
                    ((ListGroupItemView)convertView).setSwitchTypeItem(true);
                }

                convertView.setTag(holder);
            } else {
                holder = (GroupHolder) convertView.getTag();
            }

            holder.draw((ExpandableGroupListItem) group);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                             ViewGroup parent) {
        final Object group = getGroup(groupPosition);
        if (group instanceof ExpandableGroupListItem) {
            final ExpandableGroupListItem tGroup = (ExpandableGroupListItem) group;
            if (ExpandableGroupListItem.TYPE_CHILD_TEXT == tGroup.childType) {
                ChildHolder holder;
                if (null == convertView || !(convertView.getTag() instanceof ChildHolder)) {
                    holder = new ChildHolder();
                    convertView = new ListGroupChildItemView(parent.getContext());
                    holder.childLayout = convertView.findViewById(R.id.expandable_child_layout);

                    for (int i = 0; i < MAX_CHILDS; i++) {
                        holder.childrenTextViews[i] = convertView.findViewById(RESID_CHILDS[i]);
                        holder.childrenTagViews[i] = convertView.findViewById(RESID_CHILD_TAGS[i]);
                        holder.childrenTextViews[i].setOnClickListener(childClickListener);
                    }
                    convertView.setTag(holder);
                } else {
                    holder = (ChildHolder) convertView.getTag();
                }
                holder.draw(tGroup, childPosition);
            } else if (ExpandableGroupListItem.TYPE_CHILD_SEEKBAR == tGroup.childType) {
                SbChildHolder holder;
                final ExpandableChildListItem child = tGroup.childs.get(childPosition);
                if (null == convertView || !(convertView.getTag() instanceof SbChildHolder)) {
                    holder = new SbChildHolder();
                    convertView = new ListGroupChildSeekBarView(parent.getContext());
                    holder.seekBar = (SeekBar) convertView.findViewById(R.id.expandable_child_sb);
                    convertView.setTag(holder);
                    holder.seekBar.setOnSeekBarChangeListener(childSbInnerListener);
                    holder.seekBar.setMinusVisibility(true);
                    holder.seekBar.setPlusVisibility(true);
                    holder.seekBar.setMinValueVisibility(false);
                    holder.seekBar.setMaxValueVisibility(false);
                    holder.seekBar.setMax(child.sbMaxValue);
                    holder.seekBar.setBaselineVisibility(false);
                    holder.seekBar.enable(true);
                } else {
                    holder = (SbChildHolder) convertView.getTag();
                }
                holder.draw(tGroup, groupPosition, childPosition);
            } else if (ExpandableGroupListItem.TYPE_CHILD_IMAGE == tGroup.childType) {
                ImageChildHolder holder;
                if (null == convertView || !(convertView.getTag() instanceof ImageChildHolder)) {
                    holder = new ImageChildHolder();
                    convertView = new ListGroupChildImageView(parent.getContext());
                    holder.childImageLayout = convertView.findViewById(R.id.expandable_child_image_layout);
                    for (int i = 0; i < MAX_CHILD_IMAGES; i++) {
                        holder.childrenImageViews[i] = convertView.findViewById(RESID_CHILD_IMAGES[i]);
                        holder.childrenImageViews[i].setOnClickListener(childClickListener);
                    }
                    convertView.setTag(holder);
                } else {
                    holder = (ImageChildHolder) convertView.getTag();
                }
                holder.draw(tGroup, childPosition);
            } else if (ExpandableGroupListItem.TYPE_CHILD_BUTTON == tGroup.childType) {
                ButtonChildHolder holder;
                if (null == convertView || !(convertView.getTag() instanceof ButtonChildHolder) ) {
                    holder = new ButtonChildHolder();
                    convertView = new ListGroupChildButtonView(parent.getContext());

                    holder.childButtonLayout = convertView.findViewById(R.id.expandable_child_button_layout);
                    for (int i = 0; i < MAX_CHILD_BUTTONS; i++) {
                        holder.childrenButtonViews[i] = convertView.findViewById(RESID_CHILD_BUTTONS[i]);
                        holder.childrenButtonViews[i].setOnClickListener(childClickListener);
                    }
                    convertView.setTag(holder);
                } else {
                    holder = (ButtonChildHolder) convertView.getTag();
                }
                holder.draw(tGroup, childPosition);
            }
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    //endregion

    //region Public methods
    public ExpandableGroupListItem getGroupItemByValueId(int valueId) {
        ExpandableGroupListItem item = null;
        if (dataList != null) {
            for (ExpandableGroupListItem it : dataList) {
                if (it.valueId == valueId) {
                    item = it;
                    break;
                }
            }
        }

        return item;
    }

    public void setSelectedItem(ExpandableGroupListItem selItem) {
        if (selItem == null || selItem == selectedItem)
        {
            return;
        }

        if (selectedItem != null) {
            selectedItem.setSelected(false);
        }

        selItem.setSelected(true);
        selectedItem = selItem;
        notifyDataSetChanged();
    }

    public void setAllItemsEnabled(boolean enabled) {
        if (dataList != null) {
            for (ExpandableGroupListItem it : dataList) {
                it.setEnabled(enabled);
            }
        }
        notifyDataSetChanged();
    }

    //endregion

    //region Holders
    private static final class GroupHolder {
        public View groupView = null;
        public ImageView groupItemIcon = null;
        public TextView groupItemName = null;
        public TextView groupItemValue = null;
        public View groupItemValueBG = null;
        public SwitchButton groupItemSwitch = null;

        public void draw(final ExpandableGroupListItem group) {
            groupItemName.setText(group.groupStr);
            groupItemName.setEnabled(group.isEnabled());
            if (group.groupType == TYPE_GROUP) {
                groupView.setSelected(group.isSelected());
                groupItemSwitch.setVisibility(View.GONE);
                if (group.isSelected()
                    && group.childStr != null
                    && !group.childStr.isEmpty()
                    && group.childType == ExpandableGroupListItem.TYPE_CHILD_TEXT) {
                    groupItemValue.setVisibility(View.VISIBLE);
                    groupItemValueBG.setVisibility(View.VISIBLE);
                    groupItemValueBG.setEnabled(group.isEnabled());
                } else {
                    groupItemValue.setVisibility(View.INVISIBLE);
                    groupItemValueBG.setVisibility(View.GONE);
                }

                if (group.imgResId == 0) {
                    groupItemIcon.setVisibility(View.GONE);
                } else {
                    groupItemIcon.setVisibility(View.VISIBLE);
                    groupItemIcon.setImageResource(group.imgResId);
                    ViewUtils.tintImage(groupItemIcon, R.color.uxsdk_camera_settings_text_color);
                    groupItemIcon.setEnabled(group.isEnabled());
                }
                groupItemValue.setText(group.childStr);
                groupItemValue.setEnabled(group.isEnabled());
            } else if (group.groupType == TYPE_GROUP_SWITCH) {
                groupItemSwitch.setVisibility(View.VISIBLE);
                groupItemSwitch.setChecked(group.isSwitchEnabled());
                groupItemSwitch.setClickable(false);
                groupItemIcon.setVisibility(View.GONE);
                groupItemValue.setVisibility(View.GONE);
                groupItemValueBG.setVisibility(View.GONE);
            }
        }
    }

    private static final class ChildHolder {
        public View childLayout = null;
        public final TextView[] childrenTextViews = new TextView[MAX_CHILDS];
        public final TextView[] childrenTagViews = new TextView[MAX_CHILDS];

        public void draw(final ExpandableGroupListItem group, int childPosition) {
            int index = 0;
            childPosition *= MAX_CHILDS;
            final List<ExpandableChildListItem> list = group.childs;

            for (int i = 0; i < MAX_CHILDS; i++) {
                childrenTextViews[i].setVisibility(View.INVISIBLE);
                childrenTagViews[i].setVisibility(View.INVISIBLE);
            }

            while (childPosition + index < list.size() && index < MAX_CHILDS) {
                final ExpandableChildListItem model = list.get(childPosition + index);
                childrenTextViews[index].setTag(model);

                childrenTextViews[index].setVisibility(View.VISIBLE);
                childrenTextViews[index].setSelected(model.selected);
                childrenTextViews[index].setEnabled(group.isEnabled());
                childrenTextViews[index].setText(model.childStr);
                if (!TextUtils.isEmpty(model.tagStr)) {
                    childrenTagViews[index].setSelected(model.selected);
                    childrenTagViews[index].setEnabled(group.isEnabled());
                    childrenTagViews[index].setVisibility(View.VISIBLE);
                    childrenTagViews[index].setText(model.tagStr);
                }

                index++;
            }
        }
    }

    private final class SbChildHolder {
        private SeekBar seekBar = null;
        private long childId = 0;

        public void draw(final ExpandableGroupListItem group, final int groupPosition, int childPosition) {
            childId = getChildId(groupPosition, childPosition);
            final ExpandableChildListItem child = group.childs.get(childPosition);
            seekBar.setTag(this);
            seekBar.setMax(child.sbMaxValue - child.sbMinValue);
            seekBar.setProgress(child.valueId - child.sbMinValue);
            if (child.argObj instanceof String) {
                String text = String.valueOf(child.valueId) + child.argObj;
                seekBar.setText(text);
            } else {
                seekBar.setText(String.valueOf(child.valueId));
            }
            seekBar.setEnabled(group.isEnabled());
        }
    }

    private static final class ImageChildHolder {
        public View childImageLayout = null;
        public final ImageView[] childrenImageViews = new ImageView[MAX_CHILD_IMAGES];

        public void draw(final ExpandableGroupListItem group, int childPosition) {
            int index = 0;
            childPosition *= MAX_CHILD_IMAGES;
            final List<ExpandableChildListItem> list = group.childs;
            for (int i = 0; i < MAX_CHILD_IMAGES; i++) {
                childrenImageViews[i].setVisibility(View.INVISIBLE);
            }
            while (childPosition + index < list.size() && index < MAX_CHILD_IMAGES) {
                final ExpandableChildListItem model = list.get(childPosition + index);
                childrenImageViews[index].setTag(model);
                childrenImageViews[index].setVisibility(View.VISIBLE);
                childrenImageViews[index].setSelected(model.selected);
                childrenImageViews[index].setEnabled(group.isEnabled());
                childrenImageViews[index].setImageDrawable(childrenImageViews[index].getContext().getResources().getDrawable(model.childImageResource));
                ViewUtils.tintImage(childrenImageViews[index], R.color.uxsdk_camera_settings_text_color);
                index++;
            }
        }
    }

    private static final class ButtonChildHolder {
        public View childButtonLayout = null;
        public final TextView[] childrenButtonViews = new TextView[MAX_CHILD_BUTTONS];

        public void draw(final ExpandableGroupListItem group, int childPosition) {
            int index = 0;
            childPosition *= MAX_CHILD_BUTTONS;
            final List<ExpandableChildListItem> list = group.childs;
            for (int i = 0; i < MAX_CHILD_BUTTONS; i++) {
                childrenButtonViews[i].setVisibility(View.INVISIBLE);
            }
            while (childPosition + index < list.size() && index < MAX_CHILD_BUTTONS) {
                final ExpandableChildListItem model = list.get(childPosition + index);
                childrenButtonViews[index].setTag(model);
                childrenButtonViews[index].setVisibility(View.VISIBLE);
                childrenButtonViews[index].setSelected(model.selected);
                childrenButtonViews[index].setEnabled(group.isEnabled());
                childrenButtonViews[index].setText(model.childStr);
                index++;
            }
        }
    }
    //endregion


}
