package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.camera.OriginalPhotoSettings;
import dji.common.camera.SettingsDefinitions.PhotoFileFormat;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_CHILD_BUTTON;
import static dji.ux.beta.core.v4.ExpandableGroupListItem.TYPE_GROUP_SWITCH;


public class CameraSaveOriginalListWidget extends ExpandableListViewWidget {
    //region Properties
    private static final String TAG = "CameraSaveOriginalListWidget";
    private static final int SAVE_ORIGINAL_PANORAMA = 0;
    private static final int SAVE_ORIGINAL_HYPERLAPSE = 1;
    private DJIKey panoOriginalPhotoSettingsKey;
    private DJIKey panoOriginalImagesFormatRangeKey;
    private DJIKey hyperlapseOriginalPhotoSettingsKey;
    private DJIKey hyperlapseOriginalImagesFormatRangeKey;
    private SparseArray<PhotoFileFormat[]> saveOriginalRangeMap;
    private OriginalPhotoSettings panoOriginalPhotoSettings;
    private OriginalPhotoSettings hyperlapseOriginalPhotoSettings;

    //endregion
    //region Default Constructors
    public CameraSaveOriginalListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraSaveOriginalListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraSaveOriginalListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_save_original_name_array);
        saveOriginalRangeMap = new SparseArray<>();
        saveOriginalRangeMap.put(SAVE_ORIGINAL_PANORAMA, null);
        saveOriginalRangeMap.put(SAVE_ORIGINAL_HYPERLAPSE, null);
        initAdapter();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_save_original);
        }
    }

    private void initAdapter() {
        groupValueId = Integer.MAX_VALUE;
        childValueId = Integer.MAX_VALUE;
        adapter.setDataList(generateData());
        if (sdGroupLy != null && sdGroupLy.isShown()) {
            sdGroupLy.setVisibility(View.GONE);
        }
    }

    protected List<ExpandableGroupListItem> generateData() {
        final ArrayList<ExpandableGroupListItem> data = new ArrayList<>();
        if (saveOriginalRangeMap != null && saveOriginalRangeMap.size() != 0) {
            for (int soRange = 0; soRange < saveOriginalRangeMap.size(); soRange++) {
                //TODO: Remove the following if statement once hyperlapse is supported by SDK
                if (soRange != SAVE_ORIGINAL_HYPERLAPSE) {
                    ExpandableGroupListItem group = new ExpandableGroupListItem();
                    group.valueId = soRange;
                    group.groupStr = itemNameArray[group.valueId];
                    group.groupType = TYPE_GROUP_SWITCH;
                    if (saveOriginalRangeMap.get(soRange) != null) {
                        for (PhotoFileFormat format : saveOriginalRangeMap.get(soRange)) {
                            group.childType = TYPE_CHILD_BUTTON;
                            final ExpandableChildListItem child = new ExpandableChildListItem();
                            child.groupValueId = group.valueId;
                            child.valueId = format.value();
                            child.childStr = format.name();
                            group.childs.add(child);
                        }
                    }
                    data.add(group);
                }
            }
        }
        return data;
    }

    @Override
    public void initKey() {
        panoOriginalPhotoSettingsKey = CameraKey.create(CameraKey.PANO_ORIGINAL_PHOTO_SETTINGS);
        panoOriginalImagesFormatRangeKey = CameraKey.create(CameraKey.PANO_ORIGINAL_IMAGES_FORMAT_RANGE);
        hyperlapseOriginalPhotoSettingsKey = CameraKey.create(CameraKey.HYPERLAPSE_ORIGINAL_PHOTO_SETTINGS);
        hyperlapseOriginalImagesFormatRangeKey = CameraKey.create(CameraKey.HYPERLAPSE_ORIGINAL_IMAGES_FORMAT_RANGE);

        addDependentKey(panoOriginalPhotoSettingsKey);
        addDependentKey(panoOriginalImagesFormatRangeKey);
        addDependentKey(hyperlapseOriginalPhotoSettingsKey);
        addDependentKey(hyperlapseOriginalImagesFormatRangeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(panoOriginalPhotoSettingsKey)) {
            panoOriginalPhotoSettings = (OriginalPhotoSettings) value;
        } else if (key.equals(panoOriginalImagesFormatRangeKey)) {
            saveOriginalRangeMap.put(SAVE_ORIGINAL_PANORAMA, (PhotoFileFormat[]) value);
        } else if (key.equals(hyperlapseOriginalPhotoSettingsKey)) {
            hyperlapseOriginalPhotoSettings = (OriginalPhotoSettings) value;
        } else if (key.equals(hyperlapseOriginalImagesFormatRangeKey)) {
            saveOriginalRangeMap.put(SAVE_ORIGINAL_HYPERLAPSE, (PhotoFileFormat[]) value);
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(panoOriginalPhotoSettingsKey)) {
            updateItem(panoOriginalPhotoSettings, SAVE_ORIGINAL_PANORAMA);
        } else if (key.equals(panoOriginalImagesFormatRangeKey)) {
            initAdapter();
        } else if (key.equals(hyperlapseOriginalPhotoSettingsKey)) {
            updateItem(hyperlapseOriginalPhotoSettings, SAVE_ORIGINAL_HYPERLAPSE);
        } else if (key.equals(hyperlapseOriginalImagesFormatRangeKey)) {
            initAdapter();
        }
    }

    private void updateItem(OriginalPhotoSettings originalPhotoSettings, int group) {
        if (originalPhotoSettings != null) {
            ExpandableGroupListItem groupItem = (ExpandableGroupListItem) adapter.getGroup(group);
            if (groupItem != null) {
                groupItem.setSwitchEnabled(originalPhotoSettings.shouldSaveOriginalPhotos());
                groupItem.childValueId = originalPhotoSettings.getFormat().value();
                childValueId = groupItem.childValueId;
                updateSelected(groupValueId, group, childValueId);
            }
        }
    }

    private void updateFormatToCamera(final int id, PhotoFileFormat format, final boolean shouldSaveOriginal) {
        if (KeyManager.getInstance() == null) return;
        final OriginalPhotoSettings originalPhotoSettings = new OriginalPhotoSettings(shouldSaveOriginal, format);
        DJIKey saveOriginalKey = null;
        if (id == SAVE_ORIGINAL_PANORAMA) {
            saveOriginalKey = panoOriginalPhotoSettingsKey;
        } else if (id == SAVE_ORIGINAL_HYPERLAPSE) {
            saveOriginalKey = hyperlapseOriginalPhotoSettingsKey;
        }

        if (saveOriginalKey != null) {
            KeyManager.getInstance().setValue(saveOriginalKey, originalPhotoSettings, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Save Original set to" + shouldSaveOriginal + " successfully");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    updateItem(originalPhotoSettings, id);
                    DJILog.d(TAG, "Failed to set save original: " + error.getDescription());
                }
            });
        }
    }

    @Override
    protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final ExpandableGroupListItem group = (ExpandableGroupListItem) adapter.getGroup(groupPosition);
        if (!group.isEnabled()) {
            return true;
        }
        int formatValue = group.childs.get(0).valueId;
        PhotoFileFormat format = PhotoFileFormat.find(group.childValueId);
        if (format != PhotoFileFormat.UNKNOWN) {
            formatValue = format.value();
        }
        if (!group.isSwitchEnabled()) {
            group.setSwitchEnabled(true);

            updateFormatToCamera(group.valueId, PhotoFileFormat.find(formatValue), true);

            updateSelected(groupValueId, group.valueId, formatValue);
            childValueId = formatValue;
        } else {
            group.setSwitchEnabled(false);
            updateFormatToCamera(group.valueId, PhotoFileFormat.find(group.childValueId), false);
        }
        return true;
    }

    @Override
    protected void onChildViewClick(final Object tag) {
        if (tag instanceof ExpandableChildListItem) {
            final ExpandableChildListItem model = (ExpandableChildListItem) tag;
            if (groupValueId == model.groupValueId && model.valueId == childValueId) {
                return;
            }

            updateFormatToCamera(model.groupValueId, PhotoFileFormat.find(model.valueId), true);
            childValueId = model.valueId;
        }
    }
}
