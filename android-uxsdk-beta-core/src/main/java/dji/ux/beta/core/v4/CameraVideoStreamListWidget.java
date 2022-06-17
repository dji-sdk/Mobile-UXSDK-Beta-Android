package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatCheckBox;
import dji.common.camera.CameraStreamSettings;
import dji.common.camera.CameraVideoStreamSource;
import dji.internal.logics.CommonUtil;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.ux.beta.core.R;


public class CameraVideoStreamListWidget extends ListViewWidget {

    private static final int MAX_DUAL_LIGHT_STREAMS_LENGTH = 3;
    private static final int MAX_TIRPLE_LIGHT_STREAMS_LENGTH = 4;
    private static final ListItem.ItemProperty[] ITEMS_DUAL = new ListItem.ItemProperty[]{
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_current_livevideo, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_wide_video_storage, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_zoom_video_storage, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_video_remark, ListItem.ItemType.TEXT_TYPE)
    };

    private static final ListItem.ItemProperty[] ITEMS_TRIPLE = new ListItem.ItemProperty[]{
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_current_livevideo, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_wide_video_storage, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_zoom_video_storage, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_ir_video_storage, ListItem.ItemType.CHECK_BOX_TYPE),
            new ListItem.ItemProperty(R.string.uxsdk_camera_stream_video_remark, ListItem.ItemType.TEXT_TYPE)
    };
    private ListItem.ItemProperty[] ITEMS = ITEMS_DUAL;

    private int maxStreamsLength = MAX_DUAL_LIGHT_STREAMS_LENGTH;
    private DJIKey videoStreamsKey;
    private CameraStreamSettings cameraStreamSettings;
    private List<CameraVideoStreamSource> streamSources = new ArrayList<>();

    // constructor
    public CameraVideoStreamListWidget(Context context) {
        super(context, null, 0);
    }

    public CameraVideoStreamListWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraVideoStreamListWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    // end constructor


    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
    }

    private void updateData() {
        if (CommonUtil.isGD610DualCamera(keyIndex)) {
            maxStreamsLength = MAX_DUAL_LIGHT_STREAMS_LENGTH;
            ITEMS = ITEMS_DUAL;
        } else {
            maxStreamsLength = MAX_TIRPLE_LIGHT_STREAMS_LENGTH;
            ITEMS = ITEMS_TRIPLE;
        }
    }

    private void updateText() {
        adapter.getItemByPos(ITEMS.length-1).setEnabled(false);
        adapter.notifyItemChanged(ITEMS.length);
    }

    @Override
    public void initKey() {
        videoStreamsKey = CameraKey.create(CameraKey.RECORD_CAMERA_STREAM_SETTINGS, keyIndex);

        addDependentKey(videoStreamsKey);
        updateData();
        initAdapter();
        updateText();
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(videoStreamsKey)) {
            cameraStreamSettings = (CameraStreamSettings) value;
            streamSources = cameraStreamSettings.getCameraVideoStreamSources();
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(videoStreamsKey)) {
            updateView();
        }
    }

    private void updateView() {
        if (adapter == null || cameraStreamSettings == null || streamSources == null) {
            return;
        }
        adapter.getItemByPos(0).setChecked(cameraStreamSettings.needCurrentLiveViewStream());
        for (int i = 0; i < ITEMS.length; i++) {
            for (int j = 0; j < streamSources.size(); j++) {
                if (i == streamSources.get(j).value()) {
                    adapter.getItemByPos(i).setChecked(true);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        super.updateTitle(textTitle);
        if (textTitle != null) {
            textTitle.setText(getResources().getString(R.string.uxsdk_camera_stream_video_storage));
        }
    }

    private void initAdapter() {
        if (adapter != null) {
            adapter.clear();
        } else {
            adapter = new RecyclerAdapter(this);
        }
        for (int i = 0; i < ITEMS.length; i++) {
            ListItem item = new ListItem();
            item.setTitle(getContext().getResources().getString(ITEMS[i].itemTitleResId));
            item.itemType = ITEMS[i].type;
            adapter.add(item);
        }
        contentList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    // RecyclerView events
    @Override
    public void updateSelectedItem(ListItem item, View stateView)  {

        if ((stateView instanceof ListCheckBoxItemView || stateView instanceof CheckBox || stateView instanceof AppCompatCheckBox) && cameraStreamSettings != null && streamSources != null) {
            boolean isCheck = item.isChecked();
            int currentCount = cameraStreamSettings.needCurrentLiveViewStream() ? 1 : 0;
            if (currentCount + streamSources.size() <= 1 && isCheck) {
                Toast.makeText(getContext() , R.string.uxsdk_camera_stream_video_at_least , Toast.LENGTH_SHORT).show();
                item.setChecked(true);
                adapter.notifyDataSetChanged();
                return;
            }

            item.setChecked(!isCheck);
            adapter.notifyItemChanged(adapter.findIndexByItem(item));
            if (adapter.findIndexByItem(item) == 0) {
                cameraStreamSettings = new CameraStreamSettings(!isCheck, streamSources);
            } else {
                cameraStreamSettings = new CameraStreamSettings(cameraStreamSettings.needCurrentLiveViewStream(), updateStreams(adapter.findIndexByItem(item), !isCheck));
            }

            setStreamSettingsValue(cameraStreamSettings);
        }
    }


    private List<CameraVideoStreamSource> updateStreams(int position, boolean checked) {
        updateData();
        List<CameraVideoStreamSource> sources = streamSources;
        if (sources == null || position < 0 || position > maxStreamsLength - 1) {
            return null;
        }
        for (int i = 0; i < maxStreamsLength; i++) {
            if (i == position) {
                if (checked) {
                    sources.add(CameraVideoStreamSource.find(position));
                    return sources;
                } else {
                    sources.remove(CameraVideoStreamSource.find(position));
                    return sources;
                }
            }
        }
        return sources;
    }

    private void setStreamSettingsValue(CameraStreamSettings settings) {
        if (KeyManager.getInstance() == null) {
            return;
        }

        KeyManager.getInstance().setValue(videoStreamsKey, settings, null);
    }

    @Override
    public void destroy() {
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().removeKey(videoStreamsKey);
        }
        super.destroy();
    }

}
