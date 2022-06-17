package dji.ux.beta.core.v4;

import android.text.InputType;

import androidx.annotation.ColorInt;

/**
 * A generic item used for list view adapter
 */

public class ListItem {

    public enum ItemType {
        PARENT_TYPE(0),
        BUTTON_TYPE(1),
        SWITCH_BUTTON_TYPE(2),
        IMAGE_TITLE_TYPE(3),
        GROUP_TYPE(4),
        PRE_CHECK_TYPE(5),
        PRE_CHECK_BUTTON_TYPE(6),
        PRE_CHECK_VALUE_TYPE(7),
        COLOR_PICKER_TYPE(8),
        TEXT_TYPE(9),
        SINGLE_EDIT_TEXT_TYPE(10),
        SINGLE_EDIT_TEXT_BIG_TYPE(11),
        SEGMENTED_TYPE(12),
        SEEK_BAR_TYPE(13),
        SECTION_TYPE(14),
        TIPS_TYPE(15),
        PRE_CHECK_FULL_BUTTON_TYPE(16),
        CHECK_BOX_TYPE(17),
        AUTO_TEXT_TYPE(18);

        public final int value;

        ItemType(int value) {
            this.value = value;
        }

        private static ItemType[] values;
        public static ItemType[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static ItemType find(int x) {
            ItemType result = PARENT_TYPE;
            for (int i = 0; i < getValues().length; i++) {
                if (getValues()[i].value == x) {
                    result = getValues()[i];
                    break;
                }
            }
            return result;
        }
    }

    public int valueId = Integer.MAX_VALUE;
    public int titleImgResId = 0;
    private String titleStr = "";
    public int valueImgResId = 0;
    private String valueStr = "";
    @ColorInt
    private int valueColorID = 0;
    public ItemType itemType;
    private boolean isSelected = false;
    private String buttonTitle = null;
    private boolean isEnabled = true;
    private String rangeStr = "";
    private String rangeUnit;
    private String[] segmentedTitles;
    private int selectedIndex;
    private int inputType = (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    private boolean isVisible = true;
    private int maxProgress;
    private int progress;
    private boolean isButtonTitleVisible = true;
    private String inputString = "";
    private boolean isInputEditable;
    private boolean isInputVisible;
    private String fullButtonTitle = null;
    private boolean isFullButtonTitleVisible = true;
    private boolean isFullButtonEnabled = true;
    private boolean isChecked = false;

    @ColorInt
    private int inputColorID = 0;

    public int getInputColorID() {
        return inputColorID;
    }

    public void setInputColorID(int inputColorID) {
        this.inputColorID = inputColorID;
    }

    static public class ItemProperty {
        public int itemTitleResId;
        public ItemType type;

        public ItemProperty(int id, ItemType t) {
            itemTitleResId = id;
            type = t;
        }
    }

    public static ListItem createCameraSublistItem(int valueId, String title, String value) {
        ListItem item = new ListItem();
        item.valueId = valueId;
        item.setTitle(title);
        item.setValue(value);
        return item;
    }

    public static ListItem createCameraSublistImageItem(int valueId, String title, String value, int titleImgResId) {
        ListItem item = createCameraSublistItem(valueId, title, value);
        item.titleImgResId = titleImgResId;
        item.itemType = ItemType.IMAGE_TITLE_TYPE;
        return item;
    }

    public static ListItem createPreFlightItem(int imageRes, String title, String value, @ColorInt int valueColor) {
        ListItem item = new ListItem();
        item.itemType = ItemType.PRE_CHECK_TYPE;
        item.titleImgResId = imageRes;
        item.titleStr = title;
        item.valueStr = value;
        item.valueColorID = valueColor;
        return item;
    }

    public static ListItem createPreFlightItem(int imageRes, String title, String value, @ColorInt int valueColor, String buttonTitle) {
        ListItem item = createPreFlightItem(imageRes, title, value, valueColor);
        item.itemType = ItemType.PRE_CHECK_BUTTON_TYPE;
        item.setButtonTitle(buttonTitle);
        return item;
    }

    public static ListItem createPreFlightItem(int imageRes, String title, String value, @ColorInt int valueColor, ItemType itemType) {
        ListItem item = createPreFlightItem(imageRes, title, value, valueColor);
        item.itemType = itemType;
        return item;
    }

    public static ListItem createPreFlightItem(int imageRes, String title, String value, @ColorInt int valueColor, ItemType itemType, String buttonTitle) {
        ListItem item = createPreFlightItem(imageRes, title, value, valueColor);
        item.itemType = itemType;
        item.setButtonTitle(buttonTitle);
        return item;
    }

    public static ListItem createPreFlightItem(String fullButtonTitle) {
        ListItem item = createPreFlightItem(0, "", "", 0);
        item.itemType = ItemType.PRE_CHECK_FULL_BUTTON_TYPE;
        item.setFullButtonTitle(fullButtonTitle);
        return item;
    }

    @Override
    public boolean equals(Object o) {
        boolean ret = super.equals(o);
        if (!ret && o instanceof ListItem) {
            final ListItem tmp = (ListItem) o;
            ret = (titleStr.equals(tmp.titleStr) && titleImgResId == tmp.titleImgResId);
        }
        return ret;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(20);
        sb.append("tId").append(String.valueOf(titleStr)).append("]");
        sb.append("vId").append(String.valueOf(valueId)).append("]");
        return sb.toString();
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean getSelected() {
        return isSelected;
    }

    public void setTitle(String titleStr) {
        this.titleStr = titleStr;
    }

    public String getTitle() {
        return titleStr;
    }

    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    @ColorInt
    public int getValueColorID() {
        return valueColorID;
    }

    public void setValueColorID(@ColorInt int valueColorID) {
        this.valueColorID = valueColorID;
    }

    public String getValue() {
        return valueStr;
    }

    public void setValue(String valueStr) {
        this.valueStr = valueStr;
    }

    public String getButtonTitle() {
        return buttonTitle;
    }

    public void setButtonTitle(String buttonTitle) {
        this.buttonTitle = buttonTitle;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        setInputEditable(enabled);
        setFullButtonEnabled(enabled);
    }

    public String getRangeStr() {
        return rangeStr;
    }

    public void setRangeStr(String rangeStr) {
        this.rangeStr = rangeStr;
    }

    public String getRangeUnit() {
        return rangeUnit;
    }

    public void setRangeUnit(String rangeUnit) {
        this.rangeUnit = rangeUnit;
    }

    public String[] getSegmentedTitles() {
        return segmentedTitles;
    }

    public void setSegmentedTitles(String[] segmentedTitles) {
        this.segmentedTitles = segmentedTitles;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isButtonTitleVisible() {
        return isButtonTitleVisible;
    }

    public void setButtonTitleVisible(boolean buttonTitleVisible) {
        isButtonTitleVisible = buttonTitleVisible;
    }

    public boolean isInputVisible() {
        return this.isInputVisible;
    }

    public void setInputVisible(boolean inputVisible) {
        this.isInputVisible = inputVisible;
    }

    public boolean isInputEditable() {
        return this.isInputEditable;
    }

    public void setInputEditable(boolean inputEditable) {
        this.isInputEditable = inputEditable;
    }

    public String getFullButtonTitle() {
        return fullButtonTitle;
    }

    public void setFullButtonTitle(String fullButtonTitle) {
        this.fullButtonTitle = fullButtonTitle;
    }

    public void setFullButtonTitleVisible(boolean fullButtonTitleVisible) {
        isFullButtonTitleVisible = fullButtonTitleVisible;
    }

    public void setFullButtonEnabled(boolean isEnabled) {
        isFullButtonEnabled = isEnabled;
    }
    public boolean isFullButtonEnabled() {
        return isFullButtonEnabled;
    }

    public boolean isFullButtonTitleVisible() {
        return isFullButtonTitleVisible;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}