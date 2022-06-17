package dji.ux.beta.core.v4;

import android.content.Context;

import dji.ux.beta.core.v4.ListItem.ItemType;

/**
 * Factory to create ListItemView
 */

public class ListItemViewFactory {
    public static BaseFrameLayout createListItemView(Context ctx,
                                                     ItemType type) {
        if (type == ItemType.GROUP_TYPE) {
            return new ListGroupItemView(ctx);
        } else if (type == ItemType.BUTTON_TYPE) {
            return new ListButtonItemView(ctx);
        } else if (type == ItemType.SWITCH_BUTTON_TYPE) {
            return new ListSwitchItemView(ctx);
        } else if (type == ItemType.IMAGE_TITLE_TYPE) {
            return new ListImageItemView(ctx);
        } else if (type == ItemType.PRE_CHECK_TYPE) {
            return new PreFlightListItemView(ctx);
        } else if (type == ItemType.PRE_CHECK_BUTTON_TYPE) {
            PreFlightListItemView view = new PreFlightListItemView(ctx);
            view.setButtonVisible(true);
            return view;
        } else if (type == ItemType.PRE_CHECK_VALUE_TYPE) {
            PreFlightListItemView view = new PreFlightListItemView(ctx);
            view.setItemEditable(true);
            return view;
        } else if (type == ItemType.COLOR_PICKER_TYPE) {
            return new ListColorPickerItemView(ctx);
        } else if (type == ItemType.TEXT_TYPE) {
            return new ListTextItemView(ctx);
        } else if (type == ItemType.SINGLE_EDIT_TEXT_TYPE) {
            return new ListEditTextItemView(ctx);
        } else if (type == ItemType.SINGLE_EDIT_TEXT_BIG_TYPE) {
            return new ListEditTextBigItemView(ctx);
        } else if (type == ItemType.SEGMENTED_TYPE) {
            return new ListSegmentedItemView(ctx);
        } else if (type == ItemType.SEEK_BAR_TYPE) {
            return new ListSeekBarItemView(ctx);
        } else if (type == ItemType.SECTION_TYPE) {
            return new ListSectionItemView(ctx);
        } else if (type == ItemType.TIPS_TYPE) {
            return new ListTipsItemView(ctx);
        } else if (type == ItemType.PRE_CHECK_FULL_BUTTON_TYPE) {
            PreFlightListItemView view = new PreFlightListItemView(ctx);
            view.setFullButtonItem(true);
            return view;
        } else if (type == ItemType.CHECK_BOX_TYPE){
            return new ListCheckBoxItemView(ctx);
        }
        else {
            return new ListItemView(ctx);
        }
    }
}
