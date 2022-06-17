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
 * The widget to set the video resolution and frame rate.
 */
public class CameraSSDVideoSizeListWidget extends ExpandableListViewWidget {
    //region Properties

    private static final String TAG = "CameraSSDVideoSizeListWidget";
    private Map<VideoResolution, ArrayList<VideoFrameRate>> videoSizeRangeMap;
    private DJIKey ssdVideoSizeRangeKey;
    private DJIKey ssdVideoSizeKey;
    private ResolutionAndFrameRate ssdVideoResolutionAndFrameRate,currentSSDVideoResolutionAndFrameRate;
    private String[] resolutionNameArray;

    //endregion

    //region Default Constructors
    public CameraSSDVideoSizeListWidget(Context context) {
        super(context);
    }

    public CameraSSDVideoSizeListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraSSDVideoSizeListWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region List view initialization
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);

        // Init the adapter here.
        videoSizeRangeMap = new HashMap<>();
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
            textTitle.setText(R.string.uxsdk_camera_ssd_video_size);
        }
    }

    private int[] getResolutionValueIds(Map<VideoResolution, ArrayList<VideoFrameRate>> map) {
        if (map == null || map.keySet().isEmpty()) {
            return null;
        }

        Object[] resolutions = map.keySet().toArray();
        Arrays.sort(resolutions, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return getTotalPixelCount((VideoResolution)o2) - getTotalPixelCount((VideoResolution)o1);
            }
        });
        int[] valueIds = new int[resolutions.length];
        
        for (int i = 0; i < resolutions.length; i++) {
            valueIds[i] = ((VideoResolution)resolutions[i]).value();
        }
        return valueIds;
    }

    /**
     * Get the total number of pixels based on the resolution size
     */
    private int getTotalPixelCount(VideoResolution resolution) {
        String name = resolutionNameArray[resolution.value()];
        int x = name.indexOf('x');
        String a = name.substring(0, x);
        String b = name.substring(x+1);
        return Integer.parseInt(a) * Integer.parseInt(b);
    }

    private int[] getRateValueIds(Map<VideoResolution, ArrayList<VideoFrameRate>> map, int resolutionId) {
        if (map == null || map.keySet().isEmpty()) {
            return null;
        }

        int[] rateIds = null;
        Object[] resolutions = map.keySet().toArray();
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutionId == ((VideoResolution)resolutions[i]).value()) {
                ArrayList<VideoFrameRate> rates = map.get(VideoResolution.find(resolutionId));
                Object[] rateArray = rates.toArray();
                rateIds = new int[rateArray.length];
                for (int j = 0; j < rateArray.length; j++) {
                    rateIds[j] = ((VideoFrameRate)rateArray[j]).value();
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
        String[] frameRateNameArray = getResources().getStringArray(R.array.uxsdk_camera_video_frame_rate_real_value_array);

        int[] resolutionIds;
        int[] rateIds = null;
        if (videoSizeRangeMap != null && videoSizeRangeMap.size() > 0) {
            resolutionIds = getResolutionValueIds(videoSizeRangeMap);

            if (null != resolutionIds && resolutionIds.length > 0) {
                for (int i = 0, length = resolutionIds.length; i < length; i++) {
                    ExpandableGroupListItem group = new ExpandableGroupListItem();
                    group.valueId = resolutionIds[i];
                    if (group.valueId < resolutionNameArray.length) {
                        group.groupStr = resolutionNameArray[group.valueId];
                    } else {
                        DJILog.d(TAG, "Unexpected error!");
                    }

                    if (videoSizeRangeMap != null && videoSizeRangeMap.size() > 0) {
                        rateIds = getRateValueIds(videoSizeRangeMap, group.valueId);
                    }

                    if (null != rateIds
                        && group.valueId < itemImgResIds.length
                        && rateIds[0] < itemImgResIds[group.valueId].length) {
                        group.imgResId = itemImgResIds[group.valueId][rateIds[0]];
                        for (int j = 0, size = rateIds.length; j < size; j++) {
                            final ExpandableChildListItem child = new ExpandableChildListItem();
                            child.groupValueId = group.valueId;
                            child.valueId = rateIds[j];
                            if (child.valueId < frameRateNameArray.length) {
                                child.childStr = frameRateNameArray[child.valueId];
                            } else {
                                DJILog.d(TAG, "Unexpected error!");
                            }
                            group.childs.add(child);
                        }
                    }

                    datas.add(group);
                }
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
        ssdVideoSizeRangeKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_FRAME_RATE_RANGE);
        ssdVideoSizeKey = CameraKey.create(CameraKey.SSD_VIDEO_RESOLUTION_AND_FRAME_RATE);

        addDependentKey(ssdVideoSizeRangeKey);
        addDependentKey(ssdVideoSizeKey);
    }

    @Override
    public void transformValue(Object value, DJIKey key) {
        if (key.equals(ssdVideoSizeKey)) {
            ssdVideoResolutionAndFrameRate = (ResolutionAndFrameRate) value;
            DJILog.d(TAG, "SSDVideoResolutionAndFrameRate " + ssdVideoResolutionAndFrameRate);
        } else if (key.equals(ssdVideoSizeRangeKey)) {
            Object[] array = (ResolutionAndFrameRate[]) value;
            for (int i = 0; i < array.length; i++) {
                ResolutionAndFrameRate item = (ResolutionAndFrameRate) array[i];
                DJILog.d(TAG, "item " + i + " is " + item);
                if (videoSizeRangeMap.keySet().contains(item.getResolution())) {
                    videoSizeRangeMap.get(item.getResolution()).add(item.getFrameRate());
                } else {
                    ArrayList<VideoFrameRate> rateArray = new ArrayList<>();
                    rateArray.add(item.getFrameRate());
                    videoSizeRangeMap.put(item.getResolution(), rateArray);
                }
            }
        }
    }

    public void updateItemSelection(ResolutionAndFrameRate sSDVideoResolutionAndFrameRate) {

        if (sSDVideoResolutionAndFrameRate != null ) {
            if(currentSSDVideoResolutionAndFrameRate!=sSDVideoResolutionAndFrameRate) {
                currentSSDVideoResolutionAndFrameRate =sSDVideoResolutionAndFrameRate;
                int group;
                int child;

                group = sSDVideoResolutionAndFrameRate.getResolution().value();
                child = sSDVideoResolutionAndFrameRate.getFrameRate().value();

                if ((group != Integer.MAX_VALUE && child != Integer.MAX_VALUE)
                        && (group != groupValueId || child != childValueId)) {
                    updateSelected(groupValueId, group, child);
                    groupValueId = group;
                    childValueId = child;
                }
            }
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        if (key.equals(ssdVideoSizeRangeKey)) {
            initAdapter();
            updateItemSelection(ssdVideoResolutionAndFrameRate);
        } else if (key.equals(ssdVideoSizeKey)) {
            updateItemSelection(ssdVideoResolutionAndFrameRate);
        }
    }

    @Override
    public float aspectRatio() {
        return 0;
    }
    //endregion

    //region User actions
    private void updateVideoResolutionAndRateToCamera(VideoResolution resolution, VideoFrameRate rate) {
        if (KeyManager.getInstance() == null) return;

        ResolutionAndFrameRate resolutionAndFrameRate = new ResolutionAndFrameRate(resolution,rate);


        final ResolutionAndFrameRate finalValue = resolutionAndFrameRate;
        updateItemSelection(finalValue);
        DJILog.d(TAG, "Camera setting " + finalValue);
        KeyManager.getInstance().setValue(ssdVideoSizeKey, finalValue, new SetCallback() {
            @Override
            public void onSuccess() {
                DJILog.d(TAG, "Camera setting " + finalValue + " successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                updateItemSelection(ssdVideoResolutionAndFrameRate);
                DJILog.d(TAG, "Failed to set Camera resolution and frame rate: " + error.getDescription());
            }
        });
    }

    @Override
    protected boolean handleGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final ExpandableGroupListItem group = (ExpandableGroupListItem)adapter.getGroup(groupPosition);
        if (group.isEnabled() && group.valueId != groupValueId) {

            // Use the first framerate as Default value
            int fps = group.childs.get(0).valueId;
            VideoFrameRate fpsRate = VideoFrameRate.find(group.childValueId);
            if (fpsRate != VideoFrameRate.UNKNOWN) {
                fps = fpsRate.value();
            }

            updateVideoResolutionAndRateToCamera(VideoResolution.find(group.valueId), VideoFrameRate.find(fps));

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

            final int ratio = model.groupValueId;
            final int fps = model.valueId;
            updateVideoResolutionAndRateToCamera(VideoResolution.find(ratio), VideoFrameRate.find(fps));

            updateSelected(groupValueId, model.groupValueId, model.valueId);
            groupValueId = model.groupValueId;
            childValueId = model.valueId;

        }
    }
    //endregion

}
