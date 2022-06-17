package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

public class CameraStorageListWidget extends ListViewWidget {
    //region Properties
    private static final String TAG = "CameraStorageListWidget";
    private int[] storageLocationRange;
    private DJIKey cameraStorageLocationKey;
    private DJIKey internalStorageInsertedKey;
    private DJIKey internalStorageFullKey;
    private DJIKey internalStorageHasErrorKey;
    private DJIKey internalStorageRemainingSpaceKey;
    private DJIKey sdCardInsertedKey;
    private DJIKey sdCardFullKey;
    private DJIKey sdCardHasErrorKey;
    private DJIKey sdCardRemainingSpaceKey;

    private boolean internalStorageInserted;
    private boolean internalStorageFull;
    private boolean internalStorageHasError;
    private boolean sdCardInserted;
    private boolean sdCardFull;
    private boolean sdCardHasError;

    private int internalStorageRemainingSpace = 0;
    private int sdCardRemainingSpace = 0;

    private int currentPosition;
    private ListItem internalStorage;
    private ListItem sdCardStorage;
    //endregion

    //region Default Constructors
    public CameraStorageListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraStorageListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraStorageListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region View life cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        itemNameArray = getResources().getStringArray(R.array.uxsdk_camera_storage_location_array);
        itemImageIdArray = null;
        storageLocationRange = getResources().getIntArray(R.array.uxsdk_camera_storage_location_default_value_array);
        initAdapter(storageLocationRange);
        internalStorage = adapter.getItemByPos(adapter.findIndexByValueID(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE.value()));
        sdCardStorage = adapter.getItemByPos(adapter.findIndexByValueID(SettingsDefinitions.StorageLocation.SDCARD.value()));
    }
    //endregion

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_storage_location);
        }
    }

    //region Key life cycle
    @Override
    public void initKey() {
        cameraStorageLocationKey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION);
        internalStorageInsertedKey = CameraKey.create(CameraKey.INNERSTORAGE_IS_INSERTED);
        internalStorageFullKey = CameraKey.create(CameraKey.INNERSTORAGE_IS_FULL);
        internalStorageHasErrorKey = CameraKey.create(CameraKey.INNERSTORAGE_HAS_ERROR);
        internalStorageRemainingSpaceKey = CameraKey.create(CameraKey.INNERSTORAGE_REMAINING_SPACE_IN_MB);
        sdCardInsertedKey = CameraKey.create(CameraKey.SDCARD_IS_INSERTED);
        sdCardFullKey = CameraKey.create(CameraKey.SDCARD_IS_FULL);
        sdCardHasErrorKey = CameraKey.create(CameraKey.SDCARD_HAS_ERROR);
        sdCardRemainingSpaceKey = CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB);

        addDependentKey(cameraStorageLocationKey);
        addDependentKey(internalStorageInsertedKey);
        addDependentKey(internalStorageFullKey);
        addDependentKey(internalStorageHasErrorKey);
        addDependentKey(internalStorageRemainingSpaceKey);
        addDependentKey(sdCardInsertedKey);
        addDependentKey(sdCardFullKey);
        addDependentKey(sdCardHasErrorKey);
        addDependentKey(sdCardRemainingSpaceKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(cameraStorageLocationKey)) {
            SettingsDefinitions.StorageLocation storageLocation = (SettingsDefinitions.StorageLocation) value;
            currentPosition = adapter.findIndexByValueID(storageLocation.value());
        } else if (key.equals(internalStorageInsertedKey)) {
            internalStorageInserted = (boolean) value;
            updateInternalStorageItem();
        } else if (key.equals(internalStorageFullKey)) {
            internalStorageFull = (boolean) value;
            updateInternalStorageItem();
        } else if (key.equals(internalStorageHasErrorKey)) {
            internalStorageHasError = (boolean) value;
            updateInternalStorageItem();
        } else if (key.equals(internalStorageRemainingSpaceKey)) {
            internalStorageRemainingSpace = (int) value;
            updateInternalStorageItem();
        } else if (key.equals(sdCardInsertedKey)) {
            sdCardInserted = (boolean) value;
            updateSDCardItem();
        } else if (key.equals(sdCardFullKey)) {
            sdCardFull = (boolean) value;
            updateSDCardItem();
        } else if (key.equals(sdCardHasErrorKey)) {
            sdCardHasError = (boolean) value;
            updateSDCardItem();
        } else if (key.equals(sdCardRemainingSpaceKey)) {
            sdCardRemainingSpace = (int) value;
            updateSDCardItem();
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(cameraStorageLocationKey)) {
            adapter.onItemClick(currentPosition);
        }
    }
    //endregion

    //region User action
    @Override
    public void updateSelectedItem(ListItem item, View stateView) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        adapter.onItemClick(adapter.findIndexByItem(item));
        if (storageLocationRange != null) {
            final SettingsDefinitions.StorageLocation newStorageLocation = SettingsDefinitions.StorageLocation.find(item.valueId);
            KeyManager.getInstance().setValue(cameraStorageLocationKey, newStorageLocation, new SetCallback() {
                @Override
                public void onSuccess() {
                    DJISDKModelV4.getInstance().getValueOfKey(cameraStorageLocationKey, CameraStorageListWidget.this);
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    adapter.onItemClick(currentPosition);
                    DJILog.d(TAG, "Failed to set camera storage location");
                    DJISDKModelV4.getInstance().getValueOfKey(cameraStorageLocationKey, CameraStorageListWidget.this);
                }
            });
        }
    }
    //endregion

    private void updateInternalStorageItem() {
        if (internalStorageInserted && !internalStorageFull && !internalStorageHasError) {
            updateItem(internalStorage, State.VISIBLE);
            String valueStr =
                getResources().getString(R.string.uxsdk_internal_storage_remaining_space, internalStorageRemainingSpace);
            updateItem(internalStorage, valueStr);
        } else {
            updateItem(internalStorage, State.DISABLED);
        }

    }

    private void updateSDCardItem() {
        if (sdCardInserted && !sdCardFull && !sdCardHasError) {
            updateItem(sdCardStorage, State.VISIBLE);
            String valueStr = getResources().getString(R.string.uxsdk_sd_card_remaining_space, sdCardRemainingSpace);
            updateItem(sdCardStorage, valueStr);
        } else {
            updateItem(sdCardStorage, State.DISABLED);
        }
    }
}
