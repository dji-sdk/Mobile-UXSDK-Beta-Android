/*
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

public class ExpandableChildListItem {

    public String childStr = "";
    public String tagStr = "";
    public int valueId = Integer.MAX_VALUE;
    public int groupValueId = Integer.MAX_VALUE;
    public int childImageResource = 0;
    
    public boolean selected = false;
    public Object argObj = null;
    public int sbMinValue = 0;
    public int sbMaxValue = 100;

    @Override
    public boolean equals(Object o) {
        boolean ret = super.equals(o);
        if (!ret && o instanceof ExpandableChildListItem) {
            ExpandableChildListItem child = (ExpandableChildListItem) o;
            ret = (valueId == child.valueId);
        }
        return ret;
    }

    @Override
    public int hashCode() {
        return valueId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(16);
        sb.append("desc[").append(childStr).append(tagStr).append("]");
        return sb.toString();
    }

}
