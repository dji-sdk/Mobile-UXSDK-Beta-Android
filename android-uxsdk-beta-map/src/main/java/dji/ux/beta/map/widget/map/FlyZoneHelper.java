/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.map.widget.map;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJIBitmapDescriptor;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJICircle;
import com.dji.mapkit.core.models.annotations.DJICircleOptions;
import com.dji.mapkit.core.models.annotations.DJIMarker;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;
import com.dji.mapkit.core.models.annotations.DJIPolygon;
import com.dji.mapkit.core.models.annotations.DJIPolygonOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.flyzone.CustomUnlockZone;
import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.common.flightcontroller.flyzone.FlyZoneInformation;
import dji.common.flightcontroller.flyzone.FlyZoneType;
import dji.common.flightcontroller.flyzone.SubFlyZoneInformation;
import dji.common.flightcontroller.flyzone.SubFlyZoneShape;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.log.DJILog;
import dji.ux.beta.core.util.ViewUtil;
import dji.ux.beta.map.R;

/**
 * Class is responsible for drawing the fly zones on the map.
 * It should also provide customization APIs to change the appearance of
 * fly zones
 */
public class FlyZoneHelper {

    protected static final float DEFAULT_BORDER_WIDTH = 5;
    //region Fields
    private static final int DEFAULT_ALPHA = 26;
    private static final float DEFAULT_ANCHOR = 0.5f;
    private static final String TAG = "FlyZoneHelper";
    private Context context;
    private DJIMap map;
    private UserAccountState userAccountState;
    private Map<String, DJICircle> authorizationDJICircleMap = new ConcurrentHashMap<>();
    private Map<String, DJIPolygon> authorizationDJIPolygonMap = new ConcurrentHashMap<>();
    private Map<String, DJICircle> warningDJICircleMap = new ConcurrentHashMap<>();
    private Map<String, DJIPolygon> warningDJIPolygonMap = new ConcurrentHashMap<>();
    private Map<String, DJICircle> enhancedWarningDJICircleMap = new ConcurrentHashMap<>();
    private Map<String, DJIPolygon> enhancedWarningDJIPolygonMap = new ConcurrentHashMap<>();
    private Map<String, DJICircle> restrictedDJICircleMap = new ConcurrentHashMap<>();
    private Map<String, DJIPolygon> restrictedDJIPolygonMap = new ConcurrentHashMap<>();
    private Map<String, DJIMarker> flyZoneLockedMarkerMap = new ConcurrentHashMap<>();
    private Map<String, DJIMarker> flyZoneUnlockedMarkerMap = new ConcurrentHashMap<>();
    private Map<String, CustomUnlockZone> customUnlockZonesOnAircraft = new ConcurrentHashMap<>();
    private Map<String, FlyZoneInformation> flyZoneMarkerInformationMap = new ConcurrentHashMap<>();
    private Set<DJIMarker> customUnlockMarkersSet = new HashSet<>();
    private Map<String, DJICircle> customUnlockCircleMap = new ConcurrentHashMap<>();
    private Set<String> maximumHeightShapeFlyZoneId = new HashSet<>();
    private Set<String> selfUnlockFlyZoneId = new HashSet<>();
    private Set<String> customUnlockFlyZoneShapeId = new HashSet<>();
    private Set<String> customUnlockFlyZoneOnAircraftShapeId = new HashSet<>();
    private Set<String> customUnlockFlyZoneEnabledShapeId = new HashSet<>();
    @ColorInt
    private int customUnlockColor;
    private int customUnlockColorAlpha;
    @ColorInt
    private int customUnlockSentToAircraftColor;
    private int customUnlockSentToAircraftColorAlpha;
    @ColorInt
    private int customUnlockEnabledColor;
    private int customUnlockEnabledColorAlpha;
    private DJIBitmapDescriptor customUnlockEnabledImg;
    private float customUnlockImgEnabledXAnchor;
    private float customUnlockImgEnabledYAnchor;
    private DJIBitmapDescriptor customUnlockSentToAircraftImg;
    private float customUnlockSentToAircraftImgXAnchor;
    private float customUnlockSentToAircraftImgYAnchor;

    private Set<FlyZoneCategory> visibleFlyZoneSet = new HashSet<>();
    private Map<FlyZoneCategory, Integer> flyZoneColorMap = new ConcurrentHashMap<>();
    private Map<FlyZoneCategory, Integer> flyZoneAlphaMap = new ConcurrentHashMap<>();
    private float flyZoneBorderWidth = 5;

    @ColorInt
    private int maximumHeightColor;
    private int maximumHeightAlpha;

    @ColorInt
    private int selfUnlockColor;
    private int selfUnlockAlpha;
    private DJIBitmapDescriptor selfUnlockedImg;
    private float selfUnlockedImgXAnchor;
    private float selfUnlockedImgYAnchor;
    private boolean isFlyZoneUnlockingEnabled = true;

    private DJIBitmapDescriptor selfLockedImg;
    private float selfLockedImgXAnchor;
    private float selfLockedImgYAnchor;
    private int alertDialogTheme = R.style.UXSDKMapWidgetTheme;

    private AlertDialog alertDialog;
    private FlyZoneActionListener flyZoneActionListener;
    private boolean customUnlockZonesVisibility;

    private ImageView warningLegend;
    private ImageView enhancedWarningLegend;
    private ImageView authorizedLegend;
    private ImageView restrictedLegend;
    private ImageView selfUnlockLegend;
    private ImageView customUnlockLegend;
    private ImageView customUnlockOnAircraftLegend;
    private ImageView customUnlockEnabledLegend;

    //endregion

    public FlyZoneHelper(@NonNull Context context, @NonNull FlyZoneActionListener flyZoneActionListener, View parent) {
        this.context = context;
        this.flyZoneActionListener = flyZoneActionListener;
        initDefaults();
        initLegend(parent);
        flyZoneActionListener.requestFlyZoneList();
    }

    //region public methods

    /**
     * Set the map object on which fly zones should be drawn
     *
     * @param map instance of {@link DJIMap}
     */
    public void initializeMap(@NonNull DJIMap map) {
        this.map = map;
    }

    /**
     * Check if user is authorized to unlock fly zones
     *
     * @return boolean value true - authorized false - unauthorized
     */
    public boolean isUserAuthorized() {
        return userAccountState == UserAccountState.AUTHORIZED;
    }

    /**
     * Set user account state
     *
     * @param userAccountState instance of {@link UserAccountState}
     */
    public void setUserAccountState(@NonNull UserAccountState userAccountState) {
        this.userAccountState = userAccountState;
    }

    /**
     * Provide the list that should be drawn on the map
     *
     * @param flyZoneList list of {@link FlyZoneInformation}
     */
    public void onFlyZoneListUpdate(@NonNull List<FlyZoneInformation> flyZoneList) {
        if (map == null) return;
        removeFlyZonesOffMap();
        for (FlyZoneInformation flyZoneInformation : flyZoneList) {
            drawFlyZone(flyZoneInformation);
        }
    }

    /**
     * Check if the fly zone ID is a valid marker
     *
     * @param flyZoneId String fly zone ID
     * @return boolean value true - fly zone ID marker false - fly zone ID not a marker
     */
    public boolean isFlyZoneMarkerId(@NonNull String flyZoneId) {
        return flyZoneMarkerInformationMap.containsKey(flyZoneId) || customUnlockZonesOnAircraft.containsKey(flyZoneId);
    }

    /**
     * Method to handle fly zone marker click
     *
     * @param flyZoneId String value of fly zone ID
     */
    public void onFlyZoneMarkerClick(@NonNull String flyZoneId) {
        if (flyZoneMarkerInformationMap.get(flyZoneId) != null) {
            final FlyZoneInformation flyZoneInformation = flyZoneMarkerInformationMap.get(flyZoneId);
            if (flyZoneLockedMarkerMap.containsKey(flyZoneId)) {
                verifyUserAndUnlock(flyZoneInformation);
            } else {
                showSingleButtonDialog(flyZoneInformation.getName(),
                        context.getResources().getString(R.string.uxsdk_fly_zone_unlock_end_time,
                                flyZoneInformation.getUnlockEndTime()));
            }
        } else if (customUnlockZonesOnAircraft.get(flyZoneId) != null) {
            final CustomUnlockZone flyZoneInformation = customUnlockZonesOnAircraft.get(flyZoneId);
            if (flyZoneInformation.isEnabled()) {
                flyZoneActionListener.requestDisableFlyZone();
            } else {
                verifyCustomUnlockFlyZoneCanBeEnabled(flyZoneInformation);
            }
        }
    }

    /**
     * Provide custom unlock zones that should be drawn on the map
     *
     * @param customUnlockZoneMap  custom unlock fly zones on aircraft
     * @param customUnlockZoneList custom unlock fly zones from server
     */
    public void onCustomUnlockZoneUpdate(@NonNull Map<Integer, CustomUnlockZone> customUnlockZoneMap,
                                         @NonNull List<CustomUnlockZone> customUnlockZoneList) {
        if (map == null) return;
        removeCustomFlyZones();
        for (Map.Entry entry : customUnlockZoneMap.entrySet()) {
            customUnlockZonesOnAircraft.put(String.valueOf(entry.getKey()), (CustomUnlockZone) entry.getValue());
        }

        for (CustomUnlockZone customUnlockZone : customUnlockZoneList) {
            if (customUnlockZoneMap.containsKey(customUnlockZone.getID())) {
                drawCustomUnlockFlyZones(customUnlockZoneMap.get(customUnlockZone.getID()), true);
            } else {
                drawCustomUnlockFlyZones(customUnlockZone, false);
            }
        }
    }

    //endregion

    //region private methods

    private void verifyCustomUnlockFlyZoneCanBeEnabled(final CustomUnlockZone customUnlockEnableZone) {
        boolean isFlyZoneEnabled = false;
        String enabledZone = "";
        for (CustomUnlockZone customUnlockZone : customUnlockZonesOnAircraft.values()) {
            if (customUnlockZone.isEnabled()) {
                isFlyZoneEnabled = true;
                enabledZone = customUnlockZone.getName();
                break;
            }
        }
        if (isFlyZoneEnabled) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, alertDialogTheme);
            alertDialogBuilder.setTitle(context.getResources().getString(R.string.uxsdk_fly_zone_warning));
            alertDialogBuilder.setMessage(context.getResources()
                    .getString(R.string.uxsdk_custom_fly_zone_duplicate,
                            enabledZone, customUnlockEnableZone.getName()));
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setNegativeButton(context.getResources().getString(R.string.uxsdk_app_cancel),
                    (dialog, which) -> dialog.cancel());
            alertDialogBuilder.setPositiveButton(context.getResources().getString(R.string.uxsdk_fly_zone_unlock),
                    (dialog, which) -> flyZoneActionListener.requestEnableFlyZone(
                            customUnlockEnableZone));
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            flyZoneActionListener.requestEnableFlyZone(customUnlockEnableZone);
        }
    }

    private void drawCustomUnlockFlyZones(CustomUnlockZone customUnlockZone, boolean isZoneSentToAircraft) {
        DJILatLng zoneCoordinates =
                new DJILatLng(customUnlockZone.getCenter().getLatitude(), customUnlockZone.getCenter().getLongitude());
        String customUnlockZoneId = String.valueOf(customUnlockZone.getID());
        DJICircleOptions zoneCircle = new DJICircleOptions()
                .radius(customUnlockZone.getRadius())
                .center(zoneCoordinates)
                .strokeWidth(flyZoneBorderWidth);
        if (isZoneSentToAircraft) {
            DJIMarkerOptions markerOptions = new DJIMarkerOptions()
                    .position(new DJILatLng(customUnlockZone.getCenter().getLatitude(),
                            customUnlockZone.getCenter().getLongitude()))
                    .title(customUnlockZoneId);
            if (customUnlockZone.isEnabled()) {

                zoneCircle.strokeColor(customUnlockEnabledColor)
                        .fillColor(ColorUtils.setAlphaComponent(customUnlockEnabledColor,
                                customUnlockEnabledColorAlpha));
                markerOptions.icon(customUnlockEnabledImg)
                        .anchor(customUnlockImgEnabledXAnchor, customUnlockImgEnabledYAnchor);
                customUnlockFlyZoneEnabledShapeId.add(customUnlockZoneId);

            } else {

                zoneCircle.strokeColor(customUnlockSentToAircraftColor)
                        .fillColor(ColorUtils.setAlphaComponent(customUnlockSentToAircraftColor,
                                customUnlockSentToAircraftColorAlpha));
                markerOptions.icon(customUnlockSentToAircraftImg)
                        .anchor(customUnlockSentToAircraftImgXAnchor, customUnlockSentToAircraftImgYAnchor);
                customUnlockFlyZoneOnAircraftShapeId.add(customUnlockZoneId);
            }

            DJIMarker marker = map.addMarker(markerOptions);
            customUnlockMarkersSet.add(marker);
            marker.setVisible(customUnlockZonesVisibility);
        } else {
            zoneCircle.strokeColor(customUnlockColor)
                    .fillColor(ColorUtils.setAlphaComponent(customUnlockColor, customUnlockColorAlpha));
            customUnlockFlyZoneShapeId.add(customUnlockZoneId);

        }
        DJICircle circle = map.addSingleCircle(zoneCircle);
        if (circle != null) {
            circle.setVisible(customUnlockZonesVisibility);
            customUnlockCircleMap.put(customUnlockZoneId, circle);
        }
    }

    private void removeCustomFlyZones() {
        if (customUnlockCircleMap != null) {
            for (DJICircle djiCircle : customUnlockCircleMap.values()) {
                djiCircle.remove();
            }
            customUnlockCircleMap = new ConcurrentHashMap<>();
        }
        if (customUnlockMarkersSet != null) {
            for (DJIMarker djiMarker : customUnlockMarkersSet) {
                djiMarker.remove();
            }
            customUnlockMarkersSet = new HashSet<>();
        }
        customUnlockFlyZoneShapeId.clear();
        customUnlockFlyZoneOnAircraftShapeId.clear();
        customUnlockFlyZoneEnabledShapeId.clear();
    }

    private void removeFlyZonesOffMap() {
        removeFlyZoneCircleAndClearMap(restrictedDJICircleMap);
        removeFlyZoneCircleAndClearMap(authorizationDJICircleMap);
        removeFlyZoneCircleAndClearMap(enhancedWarningDJICircleMap);
        removeFlyZoneCircleAndClearMap(warningDJICircleMap);

        removeFlyZonePolygonAndClearMap(restrictedDJIPolygonMap);
        removeFlyZonePolygonAndClearMap(authorizationDJIPolygonMap);
        removeFlyZonePolygonAndClearMap(enhancedWarningDJIPolygonMap);
        removeFlyZonePolygonAndClearMap(warningDJIPolygonMap);

        for (DJIMarker marker : flyZoneLockedMarkerMap.values()) {
            marker.remove();
        }
        flyZoneLockedMarkerMap.clear();

        for (DJIMarker marker : flyZoneUnlockedMarkerMap.values()) {
            marker.remove();
        }
        flyZoneUnlockedMarkerMap.clear();
        selfUnlockFlyZoneId.clear();
        maximumHeightShapeFlyZoneId.clear();
    }

    private void removeFlyZoneCircleAndClearMap(Map<String, DJICircle> map) {
        for (DJICircle flyZone : map.values()) {
            flyZone.remove();
        }
        map.clear();
    }

    private void removeFlyZonePolygonAndClearMap(Map<String, DJIPolygon> map) {
        for (DJIPolygon flyZone : map.values()) {
            flyZone.remove();
        }
        map.clear();
    }

    private void hideShowFlyZoneOfMap(FlyZoneCategory flyZoneCategory, boolean isVisible) {
        switch (flyZoneCategory) {
            case RESTRICTED:
                hideShowFlyZoneCircle(restrictedDJICircleMap, isVisible);
                hideShowFlyZonePolygon(restrictedDJIPolygonMap, isVisible);
                break;
            case AUTHORIZATION:
                hideShowFlyZoneCircle(authorizationDJICircleMap, isVisible);
                hideShowFlyZonePolygon(authorizationDJIPolygonMap, isVisible);
                hideShowFlyZoneMarker(isVisible);
                break;
            case ENHANCED_WARNING:
                hideShowFlyZoneCircle(enhancedWarningDJICircleMap, isVisible);
                hideShowFlyZonePolygon(enhancedWarningDJIPolygonMap, isVisible);
                break;
            case WARNING:
                hideShowFlyZoneCircle(warningDJICircleMap, isVisible);
                hideShowFlyZonePolygon(warningDJIPolygonMap, isVisible);
                break;
            case UNKNOWN:
            default:
        }
    }

    private void hideShowFlyZoneMarker(boolean isVisible) {
        for (DJIMarker marker : flyZoneLockedMarkerMap.values()) {
            marker.setVisible(isVisible);
        }
        for (DJIMarker marker : flyZoneUnlockedMarkerMap.values()) {
            marker.setVisible(isVisible);
        }
    }

    private void hideShowFlyZoneCircle(Map<String, DJICircle> circleMap, boolean isVisible) {
        for (DJICircle flyZone : circleMap.values()) {
            flyZone.setVisible(isVisible);
        }
    }

    private void hideShowFlyZonePolygon(Map<String, DJIPolygon> polygonMap, boolean isVisible) {
        for (DJIPolygon flyZone : polygonMap.values()) {
            flyZone.setVisible(isVisible);
        }
    }

    private int getColor(int id) {
        return context.getResources().getColor(id);
    }

    private void initDefaults() {
        flyZoneColorMap = new HashMap<>();
        flyZoneColorMap.put(FlyZoneCategory.WARNING, getColor(R.color.uxsdk_zone_warning));
        flyZoneColorMap.put(FlyZoneCategory.ENHANCED_WARNING, getColor(R.color.uxsdk_zone_warning_enhanced));
        flyZoneColorMap.put(FlyZoneCategory.AUTHORIZATION, getColor(R.color.uxsdk_zone_authorization));
        flyZoneColorMap.put(FlyZoneCategory.RESTRICTED, getColor(R.color.uxsdk_zone_restricted));

        flyZoneAlphaMap = new HashMap<>();
        flyZoneAlphaMap.put(FlyZoneCategory.WARNING, DEFAULT_ALPHA);
        flyZoneAlphaMap.put(FlyZoneCategory.ENHANCED_WARNING, DEFAULT_ALPHA);
        flyZoneAlphaMap.put(FlyZoneCategory.AUTHORIZATION, DEFAULT_ALPHA);
        flyZoneAlphaMap.put(FlyZoneCategory.RESTRICTED, DEFAULT_ALPHA);

        maximumHeightColor = getColor(R.color.uxsdk_zone_altitude);
        maximumHeightAlpha = DEFAULT_ALPHA;
        selfUnlockColor = getColor(R.color.uxsdk_unlocked_border);

        selfUnlockedImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.uxsdk_ic_flyzone_unlocked)));
        selfLockedImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.uxsdk_ic_flyzone_locked)));
        selfUnlockedImgXAnchor = DEFAULT_ANCHOR;
        selfUnlockedImgYAnchor = DEFAULT_ANCHOR;
        selfLockedImgXAnchor = DEFAULT_ANCHOR;
        selfLockedImgYAnchor = DEFAULT_ANCHOR;
        selfUnlockAlpha = DEFAULT_ALPHA;
        customUnlockColor = getColor(R.color.uxsdk_custom_unlock_not_sent);
        customUnlockSentToAircraftColor = getColor(R.color.uxsdk_custom_unlock_sent);
        customUnlockEnabledColor = getColor(R.color.uxsdk_custom_unlock_enabled);

        customUnlockColorAlpha = DEFAULT_ALPHA;
        customUnlockSentToAircraftColorAlpha = DEFAULT_ALPHA;
        customUnlockEnabledColorAlpha = DEFAULT_ALPHA;
        customUnlockImgEnabledXAnchor = DEFAULT_ANCHOR;
        customUnlockImgEnabledYAnchor = DEFAULT_ANCHOR;
        customUnlockSentToAircraftImgXAnchor = DEFAULT_ANCHOR;
        customUnlockSentToAircraftImgYAnchor = DEFAULT_ANCHOR;

        customUnlockEnabledImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.uxsdk_ic_flyzone_unlocked)));
        customUnlockSentToAircraftImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(context.getResources().getDrawable(R.drawable.uxsdk_ic_flyzone_locked)));
    }

    private void drawFlyZone(FlyZoneInformation zone) {

        boolean isSelfUnlockable = zone.getCategory() == FlyZoneCategory.AUTHORIZATION;
        boolean isUnlocked = zone.getUnlockEndTime() != null;
        String zoneID = String.valueOf(zone.getFlyZoneID());
        LocationCoordinate2D zoneLocation = zone.getCoordinate();
        double zoneRadius = zone.getRadius();
        DJILatLng zoneCoordinates = new DJILatLng(zoneLocation.getLatitude(), zoneLocation.getLongitude());
        //add the fly zone to the map
        if (zone.getFlyZoneType() == FlyZoneType.CIRCLE) {
            DJICircleOptions zoneCircle = new DJICircleOptions()
                    .radius(zoneRadius)
                    .center(zoneCoordinates)
                    .strokeColor(isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()))
                    .strokeWidth(flyZoneBorderWidth)
                    .fillColor(ColorUtils.setAlphaComponent(
                            isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()),
                            isUnlocked ? selfUnlockAlpha : getFlyZoneAlpha(zone.getCategory())));
            DJICircle circle = map.addSingleCircle(zoneCircle);
            addCircleToMap(zone, circle, zoneID);
        }

        if (isUnlocked) {
            DJIMarkerOptions markerOptions = new DJIMarkerOptions()
                    .position(new DJILatLng(zone.getCoordinate().getLatitude(), zone.getCoordinate().getLongitude()))
                    .icon(selfUnlockedImg)
                    .title(zoneID)
                    .anchor(selfUnlockedImgXAnchor, selfUnlockedImgYAnchor)
                    .visible(isFlyZoneUnlockingEnabled && visibleFlyZoneSet.contains(zone.getCategory()));
            DJIMarker djiMarker = map.addMarker(markerOptions);

            if (flyZoneLockedMarkerMap.containsKey(zoneID)) {
                flyZoneLockedMarkerMap.get(zoneID).remove();
                flyZoneLockedMarkerMap.remove(zoneID);
            }
            flyZoneUnlockedMarkerMap.put(zoneID, djiMarker);
            selfUnlockFlyZoneId.add(zoneID);
        } else if (isSelfUnlockable) {
            DJIMarkerOptions markerOptions = new DJIMarkerOptions()
                    .position(new DJILatLng(zone.getCoordinate().getLatitude(), zone.getCoordinate().getLongitude()))
                    .icon(selfLockedImg)
                    .title(zoneID)
                    .anchor(selfLockedImgXAnchor, selfLockedImgYAnchor)
                    .visible(isFlyZoneUnlockingEnabled && visibleFlyZoneSet.contains(zone.getCategory()));
            DJIMarker djiMarker = map.addMarker(markerOptions);

            if (flyZoneUnlockedMarkerMap.containsKey(zoneID)) {
                flyZoneUnlockedMarkerMap.get(zoneID).remove();
                flyZoneUnlockedMarkerMap.remove(zoneID);
            }
            flyZoneLockedMarkerMap.put(zoneID, djiMarker);
        }

        drawSubFlyZones(zone, isUnlocked);
        flyZoneMarkerInformationMap.put(String.valueOf(zone.getFlyZoneID()), zone);
    }

    private void drawSubFlyZones(FlyZoneInformation zone, boolean isUnlocked) {
        if (zone.getSubFlyZones() != null) {
            for (SubFlyZoneInformation subZone : zone.getSubFlyZones()) {
                //draw sub-flyZone based on shape
                String zoneSubZoneId = zone.getFlyZoneID() + "_" + subZone.getAreaID();
                if (isUnlocked) selfUnlockFlyZoneId.add(zoneSubZoneId);
                if (subZone.getShape() == SubFlyZoneShape.CYLINDER) {
                    LocationCoordinate2D subZoneLocation = subZone.getCenter();
                    double subZoneRadius = subZone.getRadius();
                    DJILatLng subZoneCoordinates =
                            new DJILatLng(subZoneLocation.getLatitude(), subZoneLocation.getLongitude());
                    DJICircleOptions mapCircleOptions = new DJICircleOptions()
                            .radius(subZoneRadius)
                            .center(subZoneCoordinates)
                            .strokeWidth(flyZoneBorderWidth)
                            .strokeColor(isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()))
                            .fillColor(ColorUtils.setAlphaComponent(
                                    isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()),
                                    isUnlocked ? selfUnlockAlpha : getFlyZoneAlpha(zone.getCategory())));
                    if (subZone.getMaxFlightHeight() != 0) {
                        mapCircleOptions.fillColor(ColorUtils.setAlphaComponent(maximumHeightColor, maximumHeightAlpha))
                                .strokeColor(maximumHeightColor);
                        maximumHeightShapeFlyZoneId.add(zoneSubZoneId);
                    }
                    DJICircle circle = map.addSingleCircle(mapCircleOptions);
                    if (circle != null) {
                        addCircleToMap(zone, circle, zoneSubZoneId);
                    } else {
                        DJILog.e(TAG, "Invalid flyzone not added to map: " + zone.getFlyZoneID() + "_" + subZone.getAreaID());
                    }
                } else if (subZone.getShape() == SubFlyZoneShape.POLYGON) {
                    List<LocationCoordinate2D> verticesLocations = subZone.getVertices();
                    DJIPolygonOptions geoPolygonOptions = new DJIPolygonOptions();
                    for (LocationCoordinate2D vertex : verticesLocations) {
                        DJILatLng verticesCoordinate = new DJILatLng(vertex.getLatitude(), vertex.getLongitude());
                        geoPolygonOptions.add(verticesCoordinate);
                    }

                    geoPolygonOptions.strokeWidth(flyZoneBorderWidth)
                            .strokeColor(isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()))
                            .fillColor(ColorUtils.setAlphaComponent(
                                    isUnlocked ? selfUnlockColor : getFlyZoneColor(zone.getCategory()),
                                    isUnlocked ? selfUnlockAlpha : getFlyZoneAlpha(zone.getCategory())));
                    if (subZone.getMaxFlightHeight() != 0) {
                        geoPolygonOptions.fillColor(ColorUtils.setAlphaComponent(maximumHeightColor,
                                maximumHeightAlpha))
                                .strokeColor(maximumHeightColor);
                        maximumHeightShapeFlyZoneId.add(zoneSubZoneId);
                    }
                    DJIPolygon geoPolygon = map.addPolygon(geoPolygonOptions);
                    if (geoPolygon != null) {
                        addPolygonToMap(zone, geoPolygon, zoneSubZoneId);
                    } else {
                        DJILog.e(TAG, "Invalid flyzone not added to map: " + zone.getFlyZoneID() + "_" + subZone.getAreaID());
                    }
                }
            }
        }
    }

    private void showSingleButtonDialog(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, alertDialogTheme);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton(context.getResources().getString(R.string.uxsdk_app_ok),
                (dialog, which) -> dialog.cancel());
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void addCircleToMap(FlyZoneInformation zone, DJICircle circle, String zoneId) {
        switch (zone.getCategory()) {
            case RESTRICTED:
                restrictedDJICircleMap.put(zoneId, circle);
                break;
            case AUTHORIZATION:
                authorizationDJICircleMap.put(zoneId, circle);
                break;
            case ENHANCED_WARNING:
                enhancedWarningDJICircleMap.put(zoneId, circle);
                break;
            case WARNING:
                warningDJICircleMap.put(zoneId, circle);
                break;
            case UNKNOWN:
            default:
        }
        circle.setVisible(visibleFlyZoneSet.contains(zone.getCategory()));
    }

    private void addPolygonToMap(FlyZoneInformation zone, DJIPolygon polygon, String zoneId) {
        switch (zone.getCategory()) {
            case RESTRICTED:
                restrictedDJIPolygonMap.put(zoneId, polygon);
                break;
            case AUTHORIZATION:
                authorizationDJIPolygonMap.put(zoneId, polygon);
                break;
            case ENHANCED_WARNING:
                enhancedWarningDJIPolygonMap.put(zoneId, polygon);
                break;
            case WARNING:
                warningDJIPolygonMap.put(zoneId, polygon);
                break;
            case UNKNOWN:
            default:
        }
        polygon.setVisible(visibleFlyZoneSet.contains(zone.getCategory()));
    }

    private void verifyUserAndUnlock(final FlyZoneInformation flyZoneInformation) {
        if (userAccountState == UserAccountState.AUTHORIZED) {
            unlockFlyZone(flyZoneInformation);
        } else if (userAccountState == UserAccountState.NOT_LOGGED_IN || userAccountState == UserAccountState.TOKEN_OUT_OF_DATE) {
            showSingleButtonDialog(context.getResources().getString(R.string.uxsdk_error), context.getResources().getString(R.string.uxsdk_self_fly_zone_login_requirement));
        } else {
            showSingleButtonDialog(context.getResources().getString(R.string.uxsdk_error), context.getResources().getString(R.string.uxsdk_fly_zone_unlock_failed_unauthorized));
        }
    }

    private void unlockFlyZone(FlyZoneInformation flyZoneInformation) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, alertDialogTheme);
        alertDialogBuilder.setTitle(context.getResources().getString(R.string.uxsdk_fly_zone_unlock_zone, flyZoneInformation.getName()));
        alertDialogBuilder.setMessage(context.getResources().getString(R.string.uxsdk_fly_zone_unlock_confirmation));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton(context.getResources().getString(R.string.uxsdk_app_cancel),
                (dialog, which) -> dialog.cancel());
        alertDialogBuilder.setPositiveButton(context.getResources().getString(R.string.uxsdk_fly_zone_unlock),
                (dialog, which) -> {
                    if (flyZoneActionListener != null) {
                        ArrayList<Integer> flyZoneArrayList = new ArrayList<>();
                        flyZoneArrayList.add(flyZoneInformation.getFlyZoneID());
                        flyZoneActionListener.requestSelfUnlock(flyZoneArrayList);
                    }
                });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void updateFlyZoneViews(FlyZoneCategory category) {
        Map<String, DJICircle> circleMap = null;
        Map<String, DJIPolygon> polygonMap = null;
        switch (category) {
            case RESTRICTED:
                circleMap = restrictedDJICircleMap;
                polygonMap = restrictedDJIPolygonMap;
                break;
            case AUTHORIZATION:
                circleMap = authorizationDJICircleMap;
                polygonMap = authorizationDJIPolygonMap;
                break;
            case ENHANCED_WARNING:
                circleMap = enhancedWarningDJICircleMap;
                polygonMap = enhancedWarningDJIPolygonMap;
                break;
            case WARNING:
                circleMap = warningDJICircleMap;
                polygonMap = warningDJIPolygonMap;
                break;
            case UNKNOWN:
            default:
        }
        int flyZoneCategoryColor = ColorUtils.setAlphaComponent(
                flyZoneColorMap.get(category), flyZoneAlphaMap.get(category));
        int maxHeightColor = ColorUtils.setAlphaComponent(
                maximumHeightColor, maximumHeightAlpha);
        int selfUnlockZoneColor = ColorUtils.setAlphaComponent(
                selfUnlockColor, selfUnlockAlpha);
        for (Map.Entry<String, DJICircle> entry : circleMap.entrySet()) {
            if (maximumHeightShapeFlyZoneId.contains(entry.getKey())) {
                DJICircle flyZoneCircle = entry.getValue();
                flyZoneCircle.setFillColor(maxHeightColor);
                flyZoneCircle.setStrokeColor(maximumHeightColor);
            } else if (selfUnlockFlyZoneId.contains(entry.getKey())) {
                DJICircle flyZoneCircle = entry.getValue();
                flyZoneCircle.setFillColor(selfUnlockZoneColor);
                flyZoneCircle.setStrokeColor(selfUnlockColor);
            } else {
                DJICircle flyZoneCircle = entry.getValue();
                flyZoneCircle.setFillColor(flyZoneCategoryColor);
                flyZoneCircle.setStrokeColor(flyZoneColorMap.get(category));
            }
        }

        for (Map.Entry<String, DJIPolygon> entry : polygonMap.entrySet()) {
            if (maximumHeightShapeFlyZoneId.contains(entry.getKey())) {
                DJIPolygon flyZonePolygon = entry.getValue();
                flyZonePolygon.setFillColor(maxHeightColor);
                flyZonePolygon.setStrokeColor(maximumHeightColor);
            } else if (selfUnlockFlyZoneId.contains(entry.getKey())) {
                DJIPolygon flyZonePolygon = entry.getValue();
                flyZonePolygon.setFillColor(selfUnlockZoneColor);
                flyZonePolygon.setStrokeColor(selfUnlockColor);
            } else {
                DJIPolygon flyZonePolygon = entry.getValue();
                flyZonePolygon.setFillColor(flyZoneCategoryColor);
                flyZonePolygon.setStrokeColor(flyZoneColorMap.get(category));
            }
        }
    }

    private void updateFlyZoneBorders() {
        for (DJICircle djiCircle : warningDJICircleMap.values()) {
            djiCircle.setStrokeWidth(flyZoneBorderWidth);
        }
        for (DJIPolygon djiPolygon : warningDJIPolygonMap.values()) {
            djiPolygon.setStrokeWidth(flyZoneBorderWidth);
        }

        for (DJICircle djiCircle : enhancedWarningDJICircleMap.values()) {
            djiCircle.setStrokeWidth(flyZoneBorderWidth);
        }
        for (DJIPolygon djiPolygon : enhancedWarningDJIPolygonMap.values()) {
            djiPolygon.setStrokeWidth(flyZoneBorderWidth);
        }

        for (DJICircle djiCircle : authorizationDJICircleMap.values()) {
            djiCircle.setStrokeWidth(flyZoneBorderWidth);
        }
        for (DJIPolygon djiPolygon : authorizationDJIPolygonMap.values()) {
            djiPolygon.setStrokeWidth(flyZoneBorderWidth);
        }

        for (DJICircle djiCircle : restrictedDJICircleMap.values()) {
            djiCircle.setStrokeWidth(flyZoneBorderWidth);
        }
        for (DJIPolygon djiPolygon : restrictedDJIPolygonMap.values()) {
            djiPolygon.setStrokeWidth(flyZoneBorderWidth);
        }

        for (DJICircle djiCircle : customUnlockCircleMap.values()) {
            djiCircle.setStrokeWidth(flyZoneBorderWidth);
        }

    }

    private void updateCustomFlyZoneViews() {
        int strokeColor;
        int zoneColor;

        for (Map.Entry<String, DJICircle> entry : customUnlockCircleMap.entrySet()) {
            if (customUnlockFlyZoneShapeId.contains(entry.getKey())) {
                strokeColor = customUnlockColor;
                zoneColor = ColorUtils.setAlphaComponent(customUnlockColor, customUnlockColorAlpha);
            } else if (customUnlockFlyZoneOnAircraftShapeId.contains(entry.getKey())) {
                strokeColor = customUnlockSentToAircraftColor;
                zoneColor = ColorUtils.setAlphaComponent(customUnlockSentToAircraftColor, customUnlockSentToAircraftColorAlpha);
            } else {
                strokeColor = customUnlockEnabledColor;
                zoneColor = ColorUtils.setAlphaComponent(customUnlockEnabledColor, customUnlockEnabledColorAlpha);
            }
            DJICircle flyZoneCircle = entry.getValue();
            flyZoneCircle.setFillColor(zoneColor);
            flyZoneCircle.setStrokeColor(strokeColor);
        }
    }


    private void initLegend(View parent) {
        warningLegend = parent.findViewById(R.id.imageview_legend_warning_zone);
        enhancedWarningLegend = parent.findViewById(R.id.imageview_legend_enhanced_warning_zone);
        authorizedLegend = parent.findViewById(R.id.imageview_legend_authorized_zone);
        restrictedLegend = parent.findViewById(R.id.imageview_legend_restricted_zone);
        selfUnlockLegend = parent.findViewById(R.id.imageview_legend_self_unlock_zone);
        customUnlockLegend = parent.findViewById(R.id.imageview_legend_custom_unlock_zone);
        customUnlockOnAircraftLegend = parent.findViewById(R.id.imageview_legend_custom_unlock_aircraft_zone);
        customUnlockEnabledLegend = parent.findViewById(R.id.imageview_legend_custom_unlock_enabled_zone);

        updateFlyZoneLegend();
        updateLegendColor(selfUnlockLegend, selfUnlockColor);
        updateLegendColor(customUnlockLegend, customUnlockColor);
        updateLegendColor(customUnlockOnAircraftLegend, customUnlockSentToAircraftColor);
        updateLegendColor(customUnlockEnabledLegend, customUnlockEnabledColor);
    }

    private void updateFlyZoneLegend() {
        updateLegendColor(warningLegend, flyZoneColorMap.get(FlyZoneCategory.WARNING));
        updateLegendColor(enhancedWarningLegend, flyZoneColorMap.get(FlyZoneCategory.ENHANCED_WARNING));
        updateLegendColor(authorizedLegend, flyZoneColorMap.get(FlyZoneCategory.AUTHORIZATION));
        updateLegendColor(restrictedLegend, flyZoneColorMap.get(FlyZoneCategory.RESTRICTED));
    }

    private void updateLegendColor(ImageView imageView, int color) {
        Drawable background = imageView.getBackground();
        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable) background).getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(color);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable) background).setColor(color);
        }
    }

    //endregion

    //region customizations

    /**
     * Get the alpha of the self-unlock fly zones.
     *
     * @return The alpha of the self-unlock fly zones.
     */
    public int getSelfUnlockAlpha() {
        return selfUnlockAlpha;
    }

    /**
     * Set the alpha of the self-unlock fly zones.
     *
     * @param selfUnlockAlpha The new alpha.
     */
    public void setSelfUnlockAlpha(int selfUnlockAlpha) {
        this.selfUnlockAlpha = selfUnlockAlpha;
        updateFlyZoneViews(FlyZoneCategory.AUTHORIZATION);
    }

    /**
     * Get the color of the self-unlock fly zones.
     *
     * @return A color int.
     */
    @ColorInt
    public int getSelfUnlockColor() {
        return selfUnlockColor;
    }

    /**
     * Set the color of the self-unlock fly zones.
     *
     * @param selfUnlockColor The new color.
     */
    public void setSelfUnlockColor(@ColorInt int selfUnlockColor) {
        this.selfUnlockColor = selfUnlockColor;
        updateFlyZoneViews(FlyZoneCategory.AUTHORIZATION);
        updateLegendColor(selfUnlockLegend, selfUnlockColor);
    }

    /**
     * Get selected fly zone category's visibility on the map.
     *
     * @param flyZoneCategory The category of fly zone.
     * @return `true` if the fly zones of this category are visible.
     */
    public boolean isFlyZoneVisible(@NonNull FlyZoneCategory flyZoneCategory) {
        return visibleFlyZoneSet.contains(flyZoneCategory);
    }

    /**
     * Set selected fly zone category's visibility on the map.
     *
     * @param flyZoneCategory The category of fly zone to show/hide.
     * @param isVisible       `true` to show fly zones of this category.
     */
    public void setFlyZoneVisible(@NonNull FlyZoneCategory flyZoneCategory, boolean isVisible) {
        if (isVisible && flyZoneMarkerInformationMap.isEmpty()) {
            flyZoneActionListener.requestFlyZoneList();
        }

        if (isVisible) {
            visibleFlyZoneSet.add(flyZoneCategory);
        } else {
            visibleFlyZoneSet.remove(flyZoneCategory);
        }
        hideShowFlyZoneOfMap(flyZoneCategory, isVisible);
    }

    /**
     * Sets the color of the given fly zone category.
     *
     * @param category The fly zone category.
     * @param color    The new border color.
     */
    public void setFlyZoneColor(@NonNull FlyZoneCategory category, @ColorInt int color) {
        flyZoneColorMap.put(category, color);
        updateFlyZoneViews(category);
        updateFlyZoneLegend();
    }

    /**
     * Get the color of the given fly zone category.
     *
     * @param category The fly zone category.
     * @return A color int.
     */
    @ColorInt
    public int getFlyZoneColor(@NonNull FlyZoneCategory category) {
        return flyZoneColorMap.get(category);
    }

    /**
     * Set the alpha of the given fly zone category.
     *
     * @param category The fly zone category.
     * @param alpha    An alpha value from 0-255.
     */
    public void setFlyZoneAlpha(@NonNull FlyZoneCategory category, @IntRange(from = 0, to = 255) int alpha) {
        flyZoneAlphaMap.put(category, alpha);
        updateFlyZoneViews(category);
        updateFlyZoneLegend();
    }

    /**
     * Get the alpha of the given fly zone category.
     *
     * @param category The fly zone category.
     * @return An alpha value from 0-255.
     */
    @IntRange(from = 0, to = 255)
    public int getFlyZoneAlpha(@NonNull FlyZoneCategory category) {
        return flyZoneAlphaMap.get(category);
    }

    /**
     * Get the border width of all fly zones.
     *
     * @return The width in pixels of the fly zone borders.
     */
    public float getFlyZoneBorderWidth() {
        return flyZoneBorderWidth;
    }

    /**
     * Set the border width of all fly zones.
     *
     * @param width The width in pixels of the fly zone borders.
     */
    public void setFlyZoneBorderWidth(float width) {
        flyZoneBorderWidth = width;
        updateFlyZoneBorders();
    }

    /**
     * Get the color of the custom unlock zone.
     *
     * @return A color int.
     */
    @ColorInt
    public int getCustomUnlockFlyZoneColor() {
        return customUnlockColor;
    }

    /**
     * Set the color of the custom unlock fly zones.
     *
     * @param customUnlockColor The new color.
     */
    public void setCustomUnlockFlyZoneColor(@ColorInt int customUnlockColor) {
        this.customUnlockColor = customUnlockColor;
        updateLegendColor(customUnlockLegend, customUnlockColor);
        updateCustomFlyZoneViews();
    }

    /**
     * Get the color of the custom unlock zones sent to the aircraft.
     *
     * @return A color int.
     */
    @ColorInt
    public int getCustomUnlockFlyZoneSentToAircraftColor() {
        return customUnlockSentToAircraftColor;
    }

    /**
     * Set the color of the custom unlock fly zones that have been sent to the
     * aircraft.
     *
     * @param customUnlockSentToAircraftColor The new color.
     */
    public void setCustomUnlockFlyZoneSentToAircraftColor(@ColorInt int customUnlockSentToAircraftColor) {
        this.customUnlockSentToAircraftColor = customUnlockSentToAircraftColor;
        updateLegendColor(customUnlockOnAircraftLegend, customUnlockSentToAircraftColor);
        updateCustomFlyZoneViews();
    }

    /**
     * Get the color of the currently enabled custom unlock fly zone.
     *
     * @return A color int.
     */
    @ColorInt
    public int getCustomUnlockFlyZoneEnabledColor() {
        return customUnlockEnabledColor;
    }

    /**
     * Set the color of the currently enabled custom unlock fly zones.
     *
     * @param customUnlockEnabledColor The new color.
     */
    public void setCustomUnlockFlyZoneEnabledColor(@ColorInt int customUnlockEnabledColor) {
        this.customUnlockEnabledColor = customUnlockEnabledColor;
        updateLegendColor(customUnlockEnabledLegend, customUnlockEnabledColor);
        updateCustomFlyZoneViews();
    }

    /**
     * Get the alpha of the custom unlock fly zones.
     *
     * @return The alpha.
     */
    public int getCustomUnlockFlyZoneAlpha() {
        return customUnlockColorAlpha;
    }

    /**
     * Set the alpha of the custom unlock fly zones.
     *
     * @param customUnlockColorAlpha The new alpha.
     */
    public void setCustomUnlockFlyZoneAlpha(@IntRange(from = 0, to = 255) int customUnlockColorAlpha) {
        this.customUnlockColorAlpha = customUnlockColorAlpha;
        updateCustomFlyZoneViews();
    }

    /**
     * Get the alpha of the custom unlock fly zones sent to the aircraft.
     *
     * @return The alpha.
     */
    public int getCustomUnlockFlyZoneSentToAircraftAlpha() {
        return customUnlockSentToAircraftColorAlpha;
    }

    /**
     * Set the alpha of the custom unlock fly zones sent to the aircraft.
     *
     * @param customUnlockSentToAircraftColorAlpha The new alpha.
     */
    public void setCustomUnlockFlyZoneSentToAircraftAlpha(@IntRange(from = 0, to = 255) int customUnlockSentToAircraftColorAlpha) {
        this.customUnlockSentToAircraftColorAlpha = customUnlockSentToAircraftColorAlpha;
        updateCustomFlyZoneViews();
    }

    /**
     * Get the alpha of the currently enabled custom unlock fly zone.
     *
     * @return The alpha.
     */
    public int getCustomUnlockFlyZoneEnabledAlpha() {
        return customUnlockEnabledColorAlpha;
    }

    /**
     * Set the alpha of the currently enabled custom unlock fly zones.
     *
     * @param customUnlockEnabledColorAlpha The new alpha.
     */
    public void setCustomUnlockFlyZoneEnabledAlpha(@IntRange(from = 0, to = 255) int customUnlockEnabledColorAlpha) {
        this.customUnlockEnabledColorAlpha = customUnlockEnabledColorAlpha;
        updateCustomFlyZoneViews();
    }

    /**
     * Get maximum height color
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getMaximumHeightColor() {
        return maximumHeightColor;
    }

    /**
     * Set maximum height color
     *
     * @param color integer value for maximum height color
     */
    public void setMaximumHeightColor(@ColorInt int color) {
        this.maximumHeightColor = color;
        updateFlyZoneViews(FlyZoneCategory.WARNING);
        updateFlyZoneViews(FlyZoneCategory.ENHANCED_WARNING);
        updateFlyZoneViews(FlyZoneCategory.AUTHORIZATION);
        updateFlyZoneViews(FlyZoneCategory.RESTRICTED);
    }

    /**
     * Get maximum height alpha
     *
     * @return integer value representing alpha
     */
    @IntRange(from = 0, to = 255)
    public int getMaximumHeightAlpha() {
        return maximumHeightAlpha;
    }

    /**
     * Set maximum height alpha
     *
     * @param alpha integer value for alpha
     */
    public void setMaximumHeightAlpha(@IntRange(from = 0, to = 255) int alpha) {
        this.maximumHeightAlpha = alpha;
        updateFlyZoneViews(FlyZoneCategory.WARNING);
        updateFlyZoneViews(FlyZoneCategory.ENHANCED_WARNING);
        updateFlyZoneViews(FlyZoneCategory.AUTHORIZATION);
        updateFlyZoneViews(FlyZoneCategory.RESTRICTED);
    }

    /**
     * Changes the icon of the custom unlock zones which are on aircraft but not enabled.
     *
     * @param drawable The image to be set.
     */
    public void setCustomUnlockSentToAircraftMarkerIcon(@NonNull Drawable drawable) {
        customUnlockSentToAircraftImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(drawable));
        for (DJIMarker djiMarker : customUnlockMarkersSet) {
            djiMarker.setIcon(customUnlockSentToAircraftImg);
            djiMarker.setAnchor(customUnlockSentToAircraftImgXAnchor, customUnlockSentToAircraftImgYAnchor);
        }
    }

    /**
     * Changes the icon of the custom unlock zones which are on aircraft but not enabled.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setCustomUnlockSentToAircraftMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        customUnlockSentToAircraftImgXAnchor = x;
        customUnlockSentToAircraftImgYAnchor = y;
        setCustomUnlockSentToAircraftMarkerIcon(drawable);
    }

    /**
     * Changes the icon of the custom unlock zone which is enabled.
     *
     * @param drawable The image to be set.
     */
    public void setCustomUnlockEnabledMarkerIcon(@NonNull Drawable drawable) {
        customUnlockEnabledImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(drawable));
        for (DJIMarker djiMarker : customUnlockMarkersSet) {
            CustomUnlockZone customUnlockZone = customUnlockZonesOnAircraft.get(djiMarker.getTitle());
            if (customUnlockZone.isEnabled()) {
                djiMarker.setIcon(customUnlockEnabledImg);
                djiMarker.setAnchor(customUnlockImgEnabledXAnchor, customUnlockImgEnabledYAnchor);
            }
        }
    }

    /**
     * Changes the icon of the custom unlock zone which is enabled.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setCustomUnlockEnabledMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        customUnlockImgEnabledXAnchor = x;
        customUnlockImgEnabledYAnchor = y;
        setCustomUnlockEnabledMarkerIcon(drawable);
    }

    /**
     * Changes the icon of the unlocked self-unlock zones.
     *
     * @param drawable The image to be set.
     */
    public void setSelfUnlockedMarkerIcon(@NonNull Drawable drawable) {
        selfUnlockedImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(drawable));
        for (DJIMarker djiMarker : flyZoneUnlockedMarkerMap.values()) {
            djiMarker.setAnchor(selfUnlockedImgXAnchor, selfUnlockedImgYAnchor);
            djiMarker.setIcon(selfUnlockedImg);
        }
    }

    /**
     * Changes the icon of the unlocked self-unlock zones.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setSelfUnlockedMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        selfUnlockedImgXAnchor = x;
        selfUnlockedImgYAnchor = y;
        setSelfUnlockedMarkerIcon(drawable);
    }

    /**
     * Changes the icon of the locked self-unlock zones.
     *
     * @param drawable The image to be set.
     */
    public void setSelfLockedMarkerIcon(@NonNull Drawable drawable) {
        selfLockedImg = DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(drawable));
        for (DJIMarker djiMarker : flyZoneLockedMarkerMap.values()) {
            djiMarker.setAnchor(selfLockedImgXAnchor, selfLockedImgYAnchor);
            djiMarker.setIcon(selfLockedImg);
        }
    }

    /**
     * Changes the icon of the locked self-unlock zones.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setSelfLockedMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        selfLockedImgXAnchor = x;
        selfLockedImgYAnchor = y;
        setSelfLockedMarkerIcon(drawable);
    }

    /**
     * Gets whether tap to unlock is enabled.
     *
     * @return `true` if tapping to unlock select fly zones is enabled.
     */
    public boolean isTapToUnlockEnabled() {
        return isFlyZoneUnlockingEnabled;
    }

    /**
     * This will enable the unlocking of fly zones by clicking on them.
     *
     * @param isFlyZonesUnlockingEnabled A boolean value that determines whether to enable Fly Zones Unlocking.
     */
    public void setTapToUnlockEnabled(boolean isFlyZonesUnlockingEnabled) {
        isFlyZoneUnlockingEnabled = isFlyZonesUnlockingEnabled;

        for (DJIMarker flyZone : flyZoneLockedMarkerMap.values()) {
            flyZone.setVisible(isFlyZonesUnlockingEnabled);
        }

        for (DJIMarker flyZone : flyZoneUnlockedMarkerMap.values()) {
            flyZone.setVisible(isFlyZonesUnlockingEnabled);
        }
    }

    /**
     * Get the visibility of custom unlock zones
     *
     * @return true - custom unlock zones visible
     */
    public boolean isCustomUnlockZonesVisible() {
        return customUnlockZonesVisibility;
    }

    /**
     * Set custom unlock zones visible
     *
     * @param isVisible true - custom unlock zones visible false - custom unlock zones not visible
     */
    public void setCustomUnlockZonesVisible(boolean isVisible) {
        customUnlockZonesVisibility = isVisible;
        for (DJICircle circle : customUnlockCircleMap.values()) {
            circle.setVisible(isVisible);
        }

        for (DJIMarker djiMarker : customUnlockMarkersSet) {
            djiMarker.setVisible(isVisible);
        }
    }

    //endregion
}
