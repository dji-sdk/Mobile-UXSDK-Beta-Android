package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions.VideoFov;
import dji.common.camera.SettingsDefinitions.VideoFrameRate;
import dji.common.camera.SettingsDefinitions.VideoResolution;
import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.ux.beta.core.R;

/**
 * Created by Robert on 11/03/16.
 */
public class CameraVideoSizeListWidget extends ExpandableListViewWidget {
    //region Properties

    private static final String TAG = "CameraVideoSizeListWidget";
    private Map<ResolutionAndFov, ArrayList<VideoFrameRate>> videoSizeRangeMap;
    private DJIKey videoSizeRangeKey;
    private DJIKey videoSizeKey;
    private ResolutionAndFrameRate videoResolutionFrameRate, currentVideoResolutionAndFrameRate;
    private String[] resolutionNameArray;

    //endregion

    //region Default Constructors
    public CameraVideoSizeListWidget(Context context) {
        super(context);
    }

    public CameraVideoSizeListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraVideoSizeListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region List view initialization
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        videoSizeRangeMap = new HashMap<>();
        videoResolutionFrameRate =
            new ResolutionAndFrameRate(VideoResolution.RESOLUTION_4096x2160, VideoFrameRate.FRAME_RATE_30_FPS);

        initAdapter();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }
        disableItemsWhenCameraBusy();
    }

    @Override
    public void updateTitle(TextView textTitle) {
        if (textTitle != null) {
            textTitle.setText(R.string.uxsdk_camera_video_size_name);
        }
    }

    private int[] getResolutionValueIds(Map<ResolutionAndFov, ArrayList<VideoFrameRate>> map) {
        if (map == null || map.keySet().isEmpty()) {
            return null;
        }

        Object[] resolutionAndFovs = map.keySet().toArray();
        Arrays.sort(resolutionAndFovs, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int pixelCount1 = getTotalPixelCount((ResolutionAndFov)o1);
                int pixelCount2 = getTotalPixelCount((ResolutionAndFov)o2);
                if (pixelCount1 == pixelCount2) {
                    return ((ResolutionAndFov)o1).getFov().compareTo(((ResolutionAndFov)o2).getFov());
                } else {
                    return pixelCount2 - pixelCount1;
                }
            }
        });
        int[] valueIds = new int[resolutionAndFovs.length];

        for (int i = 0; i < resolutionAndFovs.length; i++) {
            ResolutionAndFov item = (ResolutionAndFov) resolutionAndFovs[i];
            valueIds[i] = item.getResolution().value()*100 + item.getFov().value();
        }
        return valueIds;
    }

    /**
     * Get the total number of pixels based on the resolution size
     */
    private int getTotalPixelCount(ResolutionAndFov resolutionAndFov) {
        String name = resolutionNameArray[resolutionAndFov.getResolution().value()];
        int x = name.indexOf('x');
        String a = name.substring(0, x);
        String b = name.substring(x+1);
        return Integer.parseInt(a) * Integer.parseInt(b);
    }

    private int[] getRateValueIds(Map<ResolutionAndFov, ArrayList<VideoFrameRate>> map, int resolutionAndFovId) {
        if (map == null || map.keySet().isEmpty()) {
            return null;
        }

        int[] rateIds = null;
        Object[] resolutionAndFovs = map.keySet().toArray();
        int resolutionId = resolutionAndFovId / 100;
        int fovId = resolutionAndFovId % 100;
        for (int i = 0; i < resolutionAndFovs.length; i++) {
            ResolutionAndFov item = (ResolutionAndFov) resolutionAndFovs[i];
            if (resolutionId == item.getResolution().value() && fovId == item.getFov().value()) {
                ArrayList<VideoFrameRate> rates = map.get(item);
                Object[] rateArray = rates.toArray();
                rateIds = new int[rateArray.length];
                for (int j = 0; j < rateArray.length; j++) {
                    rateIds[j] = ((VideoFrameRate) rateArray[j]).value();
                }
                break;
            }
        }
        Arrays.sort(rateIds);
        return rateIds;
    }

    protected List<ExpandableGroupListItem> generateDatas() {
        final ArrayList<ExpandableGroupListItem> datas = new ArrayList<ExpandableGroupListItem>();

        itemImgResIds = CameraResource.videoFpsImgResIds;
        resolutionNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_resolution_name_array);
        String[] fovNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_fov_name_array);
        String[] frameRateNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_frame_rate_real_value_array);

        int[] resolutionAndFovIds;
        int[] rateIds = null;
        if (videoSizeRangeMap == null || videoSizeRangeMap.isEmpty()) {
            resolutionAndFovIds = getResources().getIntArray(R.array.uxsdk_camera_video_resolution_default_value_array);
            rateIds = getResources().getIntArray(R.array.uxsdk_camera_video_frame_rate_value_array);
        } else {
            resolutionAndFovIds = getResolutionValueIds(videoSizeRangeMap);
        }

        if (null != resolutionAndFovIds && resolutionAndFovIds.length > 0) {
            for (int i = 0, length = resolutionAndFovIds.length; i < length; i++) {
                ExpandableGroupListItem group = new ExpandableGroupListItem();
                group.valueId = resolutionAndFovIds[i];
                if (group.valueId/100 < resolutionNameArray.length && group.valueId%100 < fovNameArray.length) {
                    group.groupStr = resolutionNameArray[group.valueId/100] + " " + fovNameArray[group.valueId%100];
                } else {
                    DJILog.d("LWF", "Unexpected error!");
                }

                if (videoSizeRangeMap != null && videoSizeRangeMap.size() > 0) {
                    rateIds = getRateValueIds(videoSizeRangeMap, group.valueId);
                }

                if (null != rateIds
                    && group.valueId/100 < itemImgResIds.length
                    && rateIds[0] < itemImgResIds[group.valueId/100].length) {
                    group.imgResId = itemImgResIds[group.valueId/100][rateIds[0]];
                    for (int j = 0, size = rateIds.length; j < size; j++) {
                        final ExpandableChildListItem child = new ExpandableChildListItem();
                        child.groupValueId = group.valueId;
                        child.valueId = rateIds[j];
                        if (child.valueId < frameRateNameArray.length) {
                            child.childStr = frameRateNameArray[child.valueId];
                            if (child.valueId == VideoFrameRate.FRAME_RATE_120_FPS.value()) {
                                child.tagStr = getResources().getString(R.string.uxsdk_slow_tag);
                            }
                        } else {
                            DJILog.d("LWF", "Unexpected error!");
                        }
                        group.childs.add(child);
                    }
                }

                datas.add(group);
            }
        }
        return datas;
    }

    private void initAdapter() {
        groupValueId = Integer.MAX_VALUE;
        childValueId = Integer.MAX_VALUE;
        adapter.setDataList(generateDatas());
        if (sdGroupLy != null && sdGroupLy.isShown()) {
            sdGroupLy.setVisibility(View.GONE);
        }
    }
    //endregion

    //region Key life cycle
    @Override
    public void initKey() {
        videoSizeRangeKey = CameraKey.create(CameraKey.VIDEO_RESOLUTION_FRAME_RATE_RANGE, keyIndex);
        videoSizeKey = CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, keyIndex);

        addDependentKey(videoSizeRangeKey);
        addDependentKey(videoSizeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(videoSizeKey)) {
            videoResolutionFrameRate = (ResolutionAndFrameRate) value;
        } else if (key.equals(videoSizeRangeKey)) {
            Object[] array = (ResolutionAndFrameRate[]) value;
            ResolutionAndFrameRate item;
            ResolutionAndFov resolutionAndFov;
            for (int i = 0; i < array.length; i++) {
                item = (ResolutionAndFrameRate) array[i];
                resolutionAndFov = new ResolutionAndFov(item.getResolution(), item.getFov());
                if (videoSizeRangeMap.keySet().contains(resolutionAndFov)) {
                    videoSizeRangeMap.get(resolutionAndFov).add(item.getFrameRate());
                } else {
                    ArrayList<VideoFrameRate> rateArray = new ArrayList<>();
                    rateArray.add(item.getFrameRate());
                    videoSizeRangeMap.put(resolutionAndFov, rateArray);
                }
            }
        }
    }

    public void updateItemSelection(ResolutionAndFrameRate resolutionAndFrameRate) {

        if (resolutionAndFrameRate != null) {
            if (currentVideoResolutionAndFrameRate != videoResolutionFrameRate) {
                currentVideoResolutionAndFrameRate = resolutionAndFrameRate;
                int group;
                int child;

                group = resolutionAndFrameRate.getResolution().value() * 100 + resolutionAndFrameRate.getFov().value();
                child = resolutionAndFrameRate.getFrameRate().value();

                if ((group != Integer.MAX_VALUE && child != Integer.MAX_VALUE) && (group != groupValueId
                        || child != childValueId)) {
                    updateSelected(groupValueId, group, child);
                    groupValueId = group;
                    childValueId = child;
                }
            }
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(videoSizeRangeKey)) {
            initAdapter();
            updateItemSelection(videoResolutionFrameRate);
        } else if (key.equals(videoSizeKey)) {
            updateItemSelection(videoResolutionFrameRate);
        }
    }

    @Override
    public float aspectRatio() {
        return 0;
    }
    //endregion

    //region User actions
    private void updateVideoResolutionAndRateToCamera(VideoResolution resolution, VideoFrameRate rate, VideoFov fov) {
        if (KeyManager.getInstance() == null) return;

        final ResolutionAndFrameRate resolutionAndFrameRate = new ResolutionAndFrameRate(resolution,rate, fov);

        final ResolutionAndFrameRate finalValue = resolutionAndFrameRate;
        updateItemSelection(finalValue);
        KeyManager.getInstance().setValue(videoSizeKey, finalValue, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera setting " + finalValue + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateItemSelection(videoResolutionFrameRate);
                    }
                });

                DJILog.d(TAG, "Failed to set Camera resolution and frame rate");
            }
        });
    }

    @Override
    protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final ExpandableGroupListItem group = (ExpandableGroupListItem) adapter.getGroup(groupPosition);
        if (group.isEnabled() && group.valueId != groupValueId) {

            // Use the first framerate as Default value
            int fps = group.childs.get(0).valueId;
            VideoFrameRate fpsRate = VideoFrameRate.find(group.childValueId);
            if (fpsRate != VideoFrameRate.UNKNOWN) {
                fps = fpsRate.value();
            }

            updateVideoResolutionAndRateToCamera(VideoResolution.find(group.valueId/100),
                                                 VideoFrameRate.find(fps),
                                                 VideoFov.find(group.valueId%100));

            updateSelected(groupValueId, group.valueId, fps);
            groupValueId = group.valueId;
            childValueId = fps;
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

            final int ratio = model.groupValueId/100;
            final int fov = model.groupValueId%100;
            final int fps = model.valueId;
            updateVideoResolutionAndRateToCamera(VideoResolution.find(ratio),
                                                 VideoFrameRate.find(fps),
                                                 VideoFov.find(fov));

            updateSelected(groupValueId, model.groupValueId, model.valueId);
            groupValueId = model.groupValueId;
            childValueId = model.valueId;
        }
    }
    //endregion

    private class ResolutionAndFov {
        private VideoResolution resolution;
        private VideoFov fov;

        ResolutionAndFov(VideoResolution resolution, VideoFov fov) {
            this.resolution = resolution;
            this.fov = fov;
        }

        @Override
        public int hashCode() {
            return (resolution.value() << 16) + fov.value();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ResolutionAndFov)) return false;
            ResolutionAndFov other = (ResolutionAndFov) obj;
            return resolution == other.resolution && fov == other.fov;
        }

        public VideoResolution getResolution() {
            return resolution;
        }

        public VideoFov getFov() {
            return fov;
        }
    }
}
