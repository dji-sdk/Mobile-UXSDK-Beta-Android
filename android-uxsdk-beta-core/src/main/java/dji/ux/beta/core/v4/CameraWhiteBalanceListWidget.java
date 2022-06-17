package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions.WhiteBalancePreset;
import dji.common.camera.WhiteBalance;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;
import dji.ux.beta.core.util.CameraUtil;

import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_CHILD_SEEKBAR;

/**
 * Widget that displays list of white balance presets or custom preset with specific color temperature
 */
public class CameraWhiteBalanceListWidget extends ExpandableListViewWidget {
    //region Properties
    private static final String TAG = "CameraWhiteBalanceListWidget";
    private static final String CUSTOM_POSTFIX = "00K";
    private int[] whiteBalancePresentRange;
    private DJIKey wbPresentRangeKey;
    private DJIKey customColorRangeKey;
    private DJIKey wbColorKey;
    private int[] customColorRange;
    private ExpandableGroupListItem toBeSelectedItem;
    private WhiteBalance whiteBalance, currentWhiteBalance;

    //endregion

    //region Default Constructors

    public CameraWhiteBalanceListWidget(Context context) {
        super(context);
    }

    public CameraWhiteBalanceListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraWhiteBalanceListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_white_balance_name_array);
        itemImageIdArray = getResources().obtainTypedArray(R.array.uxsdk_camera_white_balance_img_array);
        whiteBalancePresentRange = getResources().getIntArray(R.array.uxsdk_camera_white_balance_default_value_array);
        customColorRange = getResources().getIntArray(R.array.uxsdk_camera_white_balance_Interval_value_boundary);

        initAdapter(whiteBalancePresentRange, customColorRange);

        toBeSelectedItem = adapter.getGroupItemByValueId(WhiteBalancePreset.AUTO.value());
        adapter.setSelectedItem(toBeSelectedItem);
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_white_balance_name);
        }
    }

    private void initAdapter(int[] range, int[] childRange) {
        groupValueId = Integer.MAX_VALUE;
        childValueId = Integer.MAX_VALUE;
        adapter.setDataList(generateDatas(range, childRange));
        if (sdGroupLy != null && sdGroupLy.isShown()) {
            sdGroupLy.setVisibility(View.GONE);
        }
    }

    protected List<ExpandableGroupListItem> generateDatas(int[] wbRange, int[] customValueRange) {

        if (wbRange == null) {
            return null;
        }

        final ArrayList<ExpandableGroupListItem> data = new ArrayList<>();

        for (int aWbRange : wbRange) {
            if (aWbRange >= itemNameArray.length) {
                //TODO: We need to actually update WB range in 4.5. This is only to make sure crash doesn't happen
                DJILog.e(TAG, "White balance range has more value than expected");
                break;
            }
            ExpandableGroupListItem group = new ExpandableGroupListItem();

            group.valueId = aWbRange;
            group.groupStr = itemNameArray[group.valueId];
            group.imgResId = itemImageIdArray.getResourceId(group.valueId, 0);

            if (group.valueId == WhiteBalancePreset.CUSTOM.value() && customValueRange != null) {
                group.childType = TYPE_CHILD_SEEKBAR;
                final ExpandableChildListItem child = new ExpandableChildListItem();
                child.sbMinValue = customValueRange[0];
                child.sbMaxValue = customValueRange[1];

                child.groupValueId = aWbRange;
                child.valueId = (child.sbMinValue + child.sbMaxValue) / 2; // default value is [20 - 80] from SDK.
                child.argObj = CUSTOM_POSTFIX;
                group.childs.add(child);
            }
            data.add(group);
        }

        return data;
    }

    //region Key life cycle
    @Override
    public void initKey() {
        wbPresentRangeKey = CameraUtil.createCameraKeys(CameraKey.WHITE_BALANCE_PRESENT_RANGE, keyIndex, subKeyIndex);
        wbColorKey = CameraUtil.createCameraKeys(CameraKey.WHITE_BALANCE, keyIndex, subKeyIndex);
        customColorRangeKey = CameraKey.create(CameraKey.WHITE_BALANCE_CUSTOM_COLOR_TEMPERATURE_RANGE, keyIndex);
        addDependentKey(wbPresentRangeKey);
        addDependentKey(customColorRangeKey);
        addDependentKey(wbColorKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(customColorRangeKey)) {
            customColorRange = (int[]) value;
        } else if (key.equals(wbPresentRangeKey)) {
            Object[] array = (WhiteBalancePreset[]) value;
            whiteBalancePresentRange = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                whiteBalancePresentRange[i] = ((WhiteBalancePreset) array[i]).value();
            }
        } else if (key.equals(wbColorKey)) {
            whiteBalance = (WhiteBalance) value;
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(wbPresentRangeKey) || key.equals(customColorRangeKey)) {
            initAdapter(whiteBalancePresentRange, customColorRange);
            // update the selection since adapter changed.
            updateItemSelection(whiteBalance);
        } else if (key.equals(wbColorKey)) {
            updateItemSelection(whiteBalance);
        }
    }

    private void updateItemSelection(WhiteBalance whiteBalance) {
        if (whiteBalance != null && whiteBalance.getWhiteBalancePreset() != null) {
            if (currentWhiteBalance != whiteBalance) {
                currentWhiteBalance = whiteBalance;
                toBeSelectedItem = adapter.getGroupItemByValueId(whiteBalance.getWhiteBalancePreset().value());
                if (toBeSelectedItem != null) {
                    adapter.setSelectedItem(toBeSelectedItem);
                }
            }
        }
    }
    //endregion

    protected void updateUserSelectedItemToCamera(final int valueId, final int childId) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        if (whiteBalancePresentRange != null) {

            ExpandableGroupListItem selectItem = adapter.getGroupItemByValueId(valueId);
            adapter.setSelectedItem(selectItem);

            WhiteBalancePreset whiteBalancePreset = WhiteBalancePreset.find(valueId);
            final WhiteBalance newWhiteBalance;
            if (whiteBalancePreset == WhiteBalancePreset.CUSTOM) {
                newWhiteBalance = new WhiteBalance(whiteBalancePreset, childId);
            } else {
                newWhiteBalance = new WhiteBalance(whiteBalancePreset);
            }
            updateItemSelection(newWhiteBalance);
            KeyManager.getInstance().setValue(wbColorKey, newWhiteBalance, new SetCallback() {
                @Override
                public void onSuccess() {
                    updateSelected(groupValueId, valueId, childId);
                    groupValueId = valueId;
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItemSelection(whiteBalance);
                    DJILog.d(TAG, "Failed to set camera white balance");
                }
            });
        }
    }

    @Override
    protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final ExpandableGroupListItem group = (ExpandableGroupListItem) adapter.getGroup(groupPosition);

        if (group.isEnabled() && group.valueId != groupValueId) {
            if (group.valueId == WhiteBalancePreset.CUSTOM.value()) {
                if (group.childValueId < customColorRange[0] || group.childValueId > customColorRange[1]) {
                    if (whiteBalance != null && whiteBalance.getWhiteBalancePreset() == WhiteBalancePreset.CUSTOM) {
                        group.childValueId = whiteBalance.getColorTemperature();
                    } else {
                        //TODO: The key has bug, there is no way to get the current custom color temperature.
                        //Get the mid value of the range to workaround a bug for Spark which range is [28, 100], but
                        // this range is not supported in Sharelib for now, so still [20, 100].  If just use the
                        // low boundary value as default value, it will fail to set this child value.
                        group.childValueId = (customColorRange[0] + customColorRange[1]) / 2;
                    }
                }
            }

            updateUserSelectedItemToCamera(group.valueId, group.childValueId);
        }

        return true;
    }

    @Override
    protected void onChildViewInputComplete(ExpandableGroupListItem group, ExpandableChildListItem child) {
        updateUserSelectedItemToCamera(child.groupValueId, child.valueId);
        groupValueId = child.groupValueId;
        childValueId = child.valueId;
    }
}
