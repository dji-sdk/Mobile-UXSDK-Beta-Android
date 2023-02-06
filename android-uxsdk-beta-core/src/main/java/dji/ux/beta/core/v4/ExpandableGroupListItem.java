/*
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

import java.util.ArrayList;

public class ExpandableGroupListItem {

    //region Properties
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_GROUP_SWITCH = 2;

    public static final int TYPE_CHILD_TEXT = 1;
    public static final int TYPE_CHILD_SEEKBAR = 2;
    public static final int TYPE_CHILD_IMAGE = 3;
    public static final int TYPE_CHILD_BUTTON = 4;
    public static final int INVALID_VALUE = Integer.MAX_VALUE;

    public int imgResId = 0;
    public String groupStr = "";
    public String childStr = "";
    public int valueId = INVALID_VALUE;
    public int childValueId = INVALID_VALUE;
    public int childType = TYPE_CHILD_TEXT;
    public int groupType = TYPE_GROUP;
    public final ArrayList<ExpandableChildListItem> childs = new ArrayList<ExpandableChildListItem>();

    private boolean selected = false;
    private boolean enabled = true;
    private boolean switchEnabled;
    //endregion

    //region Override methods
    @Override
    public boolean equals(Object o) {
        boolean ret = super.equals(o);
        if (!ret && o instanceof ExpandableGroupListItem) {
            ExpandableGroupListItem group = (ExpandableGroupListItem) o;
            ret = (imgResId == group.imgResId);
        }
        return ret;
    }

    @Override
    public int hashCode() {
        return imgResId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(24);
        sb.append("group[").append(groupStr).append("]");
        sb.append("child[").append(String.valueOf(childs.size())).append("]");
        return sb.toString();
    }
    //endregion

    //region Public methods
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSwitchEnabled(boolean enabled) {
        this.switchEnabled = enabled;
    }

    public boolean isSwitchEnabled() {
        return switchEnabled;
    }
    //endregion
}
