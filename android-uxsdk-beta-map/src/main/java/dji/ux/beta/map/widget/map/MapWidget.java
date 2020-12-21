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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import com.dji.mapkit.amap.provider.AMapProvider;
import com.dji.mapkit.core.Mapkit;
import com.dji.mapkit.core.camera.DJICameraUpdate;
import com.dji.mapkit.core.camera.DJICameraUpdateFactory;
import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.maps.DJIMapViewInternal;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJICameraPosition;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.DJILatLngBounds;
import com.dji.mapkit.core.models.annotations.DJIMarker;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;
import com.dji.mapkit.core.models.annotations.DJIPolyline;
import com.dji.mapkit.core.models.annotations.DJIPolylineOptions;
import com.dji.mapkit.google.provider.GoogleProvider;
import com.dji.mapkit.here.provider.HereProvider;
import com.dji.mapkit.mapbox.provider.MapboxProvider;

import java.util.ArrayList;
import java.util.List;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.flyzone.CustomUnlockZone;
import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.common.flightcontroller.flyzone.FlyZoneInformation;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.log.DJILog;
import dji.sdk.util.LocationUtil;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.Single;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;
import dji.ux.beta.core.util.MathUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.util.ViewUtil;
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget;
import dji.ux.beta.map.R;

/**
 * MapWidget displays the aircraft's state and information on the map. This
 * includes aircraft location, home location, aircraft trail path, aircraft
 * heading, and No Fly Zones. It also provides the user with options to unlock some Fly Zones.
 */
public class MapWidget extends ConstraintLayoutWidget implements View.OnTouchListener, OnStateChangeCallback, FlyZoneActionListener {

    //region  Constants
    private static final int INVALID_ALPHA = -1;
    private static final int MIN_ALPHA = 0;
    private static final int MAX_ALPHA = 255;
    private static final int COUNTER_REFRESH_THRESHOLD = 200;
    private static final int FLIGHT_ANIM_DURATION = 130;
    private static final int ROTATION_ANIM_DURATION = 100;
    private static final int AIRCRAFT_MARKER_ELEVATION = 7;
    private static final int GIMBAL_MARKER_ELEVATION = 6;
    private static final int HOME_MARKER_ELEVATION = 5;
    private static final int DEFAULT_ZOOM = 16;
    private static final int DO_NOT_UPDATE_ZOOM = -1;
    private static final String TAG = "MapWidget";
    private static final String HOME_MARKER = "homemarker";
    private static final String AIRCRAFT_MARKER = "aircraftmarker";
    private static final String GIMBAL_YAW_MARKER = "gimbalyawmarker";

    //endregion

    //region map  fields
    private Group legendGroup;
    private boolean isTouching = false;
    private int centerRefreshCounter = 201;
    private MapWidgetModel widgetModel;
    private DJIMap map;
    private DJIMapViewInternal mapView;
    private MapCenterLock mapCenterLockMode = MapCenterLock.AIRCRAFT;
    private boolean isAutoFrameMapBounds = false;
    private DJIMap.MapType mapType;
    private UserAccountLoginWidget userAccountLoginWidget;
    private DJIMap.OnMarkerClickListener onMarkerClickListener;

    //endregion

    //region home marker fields
    private DJIMarker homeMarker;
    private Drawable homeIcon;
    private boolean homeMarkerEnabled;
    private float homeIconAnchorX = 0.5f;
    private float homeIconAnchorY = 0.5f;
    //endregion

    //region gimbal yaw marker fields
    private DJIMarker gimbalYawMarker;
    private Drawable gimbalYawIcon;
    private boolean gimbalYawMarkerEnabled;
    private float gimbalYawAnchorX = 0.5f;
    private float gimbalYawAnchorY = 0.5f;
    //endregion

    //region Aircraft Marker Fields
    private float aircraftMarkerHeading;
    private DJIMarker aircraftMarker;
    private Drawable aircraftIcon;
    private boolean aircraftMarkerEnabled;
    private float aircraftIconAnchorX = 0.5f;
    private float aircraftIconAnchorY = 0.5f;
    //endregion

    //region direction to home fields
    private DJIPolyline homeLine;
    private boolean homeDirectionEnabled = true;
    @ColorInt
    private int homeDirectionColor = Color.GREEN;
    private float homeDirectionWidth = 5;
    //endregion

    //region flight path fields
    private DJIPolyline flightPathLine;
    private List<DJILatLng> flightPathPoints = new ArrayList<>();
    @ColorInt
    private int flightPathColor = Color.WHITE;
    private float flightPathWidth = 5;
    private boolean flightPathEnabled = true;
    //endregion

    //region flyZone
    private FlyZoneHelper flyZoneHelper;

    //endregion

    //region Lifecycle
    public MapWidget(@NonNull Context context) {
        super(context);
    }

    public MapWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MapWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_map, this);
        legendGroup = findViewById(R.id.constraint_group_legend);
        userAccountLoginWidget = findViewById(R.id.widget_login);
        userAccountLoginWidget.setOnStateChangeCallback(this);

        if (!isInEditMode()) {
            widgetModel = new MapWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            flyZoneHelper = new FlyZoneHelper(context, this, this);
        }

        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getProductConnection().observeOn(SchedulerProvider.ui()).subscribe(connected -> {
            if (connected) {
                addReaction(reactToHeadingChanges());
                addReaction(widgetModel.getHomeLocation()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(MapWidget.this::updateHomeLocation));
                addReaction(widgetModel.getAircraftLocation()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(MapWidget.this::updateAircraftLocation));
            }
        }));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_map_ratio);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                break;

            case MotionEvent.ACTION_UP:
                isTouching = false;
                break;

            case MotionEvent.ACTION_MOVE:
            default:
                break;
        }
        return true;
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onCreate(@Nullable Bundle saveInstanceState) {
        if (mapView != null) {
            mapView.onCreate(saveInstanceState);
        }
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onDestroy() {
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                DJILog.e(TAG, "Error while attempting MapView.onDestroy(), ignoring exception" + e);
            }
        }
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onSaveInstanceState(@Nullable Bundle bundle) {
        if (mapView != null) {
            mapView.onSaveInstanceState(bundle);
        }
    }

    /**
     * Calling this method from the corresponding method in your activity is required for Google Maps.
     */
    public void onLowMemory() {
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onStateChange(@Nullable Object state) {
        if (state instanceof UserAccountState) {
            flyZoneHelper.setUserAccountState((UserAccountState) state);
        }
    }

    @Override
    public void requestSelfUnlock(@NonNull ArrayList<Integer> arrayList) {
        addDisposable(widgetModel.unlockFlyZone(arrayList)
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::getFlyZoneList, error -> {
                    Toast.makeText(getContext(),
                            getResources().getString(R.string.uxsdk_fly_zone_unlock_failed, error.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    DJILog.e(TAG, "request self unlock " + error.getLocalizedMessage());
                }));
    }

    @Override
    public void requestFlyZoneList() {
        getFlyZoneList();
    }

    @Override
    public void requestEnableFlyZone(@NonNull CustomUnlockZone customUnlockZone) {
        addDisposable(widgetModel.enableCustomUnlockZoneOnAircraft(customUnlockZone)
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::requestCustomUnlockZonesFromServer,
                        logErrorConsumer(TAG, "request enable fly zone ")));
    }

    @Override
    public void requestDisableFlyZone() {
        addDisposable(widgetModel.disableCustomUnlockZoneOnAircraft()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::requestCustomUnlockZonesFromServer,
                        logErrorConsumer(TAG, "request disable fly zone ")));
    }
    //endregion

    //region private methods

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MapWidget);
        String mapBoxAccessToken = typedArray.getString(R.styleable.MapWidget_uxsdk_mapBoxToken);
        if (!isInEditMode()) {
            int mapProviderInt = typedArray.getInt(R.styleable.MapWidget_uxsdk_mapProvider, -1);
            if (mapProviderInt >= 0 && (mapProviderInt != 3 || mapBoxAccessToken != null)) {
                setMapProvider(SettingDefinitions.MapProvider.find(mapProviderInt), mapBoxAccessToken);
            }
            setMapType(DJIMap.MapType.find(typedArray.getInt(R.styleable.MapWidget_uxsdk_djiMap_mapType,
                    DJIMap.MapType.NORMAL.getValue())));
            int color = typedArray.getColor(R.styleable.MapWidget_uxsdk_warningFlyZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setFlyZoneColor(FlyZoneCategory.WARNING, color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_enhancedWarningZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setFlyZoneColor(FlyZoneCategory.ENHANCED_WARNING, color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_authorizedFlyZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setFlyZoneColor(FlyZoneCategory.AUTHORIZATION, color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_restrictedFlyZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setFlyZoneColor(FlyZoneCategory.RESTRICTED, color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_selfUnlockedFlyZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setSelfUnlockColor(color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setCustomUnlockFlyZoneColor(color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneOnAircraftColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setCustomUnlockFlyZoneSentToAircraftColor(color);
            }
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneEnabledColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setCustomUnlockFlyZoneEnabledColor(color);
            }
            int alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_warningFlyZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setFlyZoneAlpha(FlyZoneCategory.WARNING, alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_enhancedWarningZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setFlyZoneAlpha(FlyZoneCategory.ENHANCED_WARNING, alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_authorizedFlyZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setFlyZoneAlpha(FlyZoneCategory.AUTHORIZATION, alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_restrictedFlyZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setFlyZoneAlpha(FlyZoneCategory.RESTRICTED, alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setCustomUnlockFlyZoneAlpha(alphaValue);
            }
            alphaValue =
                    typedArray.getInt(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneOnAircraftColor, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setCustomUnlockFlyZoneSentToAircraftAlpha(alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_customUnlockedFlyZoneEnabledAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setCustomUnlockFlyZoneEnabledAlpha(alphaValue);
            }
            alphaValue = typedArray.getInt(R.styleable.MapWidget_uxsdk_selfUnlockedFlyZoneAlpha, INVALID_ALPHA);
            if (isValidAlpha(alphaValue)) {
                flyZoneHelper.setSelfUnlockAlpha(alphaValue);
            }
            flyZoneHelper.setFlyZoneBorderWidth(typedArray.getFloat(R.styleable.MapWidget_uxsdk_flyZoneBorderWidth,
                    FlyZoneHelper.DEFAULT_BORDER_WIDTH));
            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_maximumHeightColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setMaximumHeightColor(color);
            }
            alphaValue = typedArray.getColor(R.styleable.MapWidget_uxsdk_maximumHeightAlpha, INVALID_ALPHA);
            if (color != INVALID_COLOR) {
                flyZoneHelper.setMaximumHeightAlpha(alphaValue);
            }

            Drawable drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_flyZoneUnLockedIcon);
            if (drawable != null) {
                flyZoneHelper.setSelfUnlockedMarkerIcon(drawable);
            }

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_flyZoneLockedIcon);
            if (drawable != null) {
                flyZoneHelper.setSelfLockedMarkerIcon(drawable);
            }

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_customFlyZoneOnAircraftIcon);
            if (drawable != null) {
                flyZoneHelper.setCustomUnlockSentToAircraftMarkerIcon(drawable);
            }

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_customFlyZoneEnabledIcon);
            if (drawable != null) {
                flyZoneHelper.setCustomUnlockEnabledMarkerIcon(drawable);
            }

            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_homeDirectionColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                setDirectionToHomeColor(color);
            }
            float dimension = typedArray.getDimension(R.styleable.MapWidget_uxsdk_homeDirectionWidth, INVALID_DIMENSION);
            if (dimension != INVALID_DIMENSION) {
                setDirectionToHomeWidth(dimension);
            }
            setDirectionToHomeEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_homeDirectionEnabled, true));

            color = typedArray.getColor(R.styleable.MapWidget_uxsdk_flightPathColor, INVALID_COLOR);
            if (color != INVALID_COLOR) {
                setFlightPathColor(color);
            }
            dimension = typedArray.getDimension(R.styleable.MapWidget_uxsdk_flightPathWidth, INVALID_DIMENSION);
            if (dimension != INVALID_DIMENSION) {
                setFlightPathWidth(dimension);
            }
            setFlightPathEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_flightPathEnabled, true));

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_aircraftMarkerIcon);
            if (drawable != null) {
                setAircraftMarkerIcon(drawable);
            }
            setAircraftMarkerEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_aircraftMarkerEnabled, true));

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_homeMarkerIcon);
            if (drawable != null) {
                setHomeMarkerIcon(drawable);
            }
            setHomeMarkerEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_homeMarkerEnabled, true));

            drawable = typedArray.getDrawable(R.styleable.MapWidget_uxsdk_gimbalMarkerIcon);
            if (drawable != null) {
                setGimbalMarkerIcon(drawable);
            }
            setGimbalAttitudeEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_gimbalAttitudeEnabled, true));
        }

        mapCenterLockMode = MapCenterLock.find(typedArray.getInt(R.styleable.MapWidget_uxsdk_mapCenterLock,
                MapCenterLock.AIRCRAFT.getIndex()));
        isAutoFrameMapBounds = typedArray.getBoolean(R.styleable.MapWidget_uxsdk_autoFrameMap, false);
        setFlyZoneLegendEnabled(typedArray.getBoolean(R.styleable.MapWidget_uxsdk_flyZoneLegendEnabled, false));

        typedArray.recycle();
    }

    private void setMapProvider(SettingDefinitions.MapProvider provider, String accessToken) {
        switch (provider) {
            case HERE:
                initHereMap(null);
                break;
            case AMAP:
                initAMap(null);
                break;
            case MAPBOX:
                initMapboxMap(accessToken, null);
                break;
            case GOOGLE:
                initGoogleMap(null);
            default:
                // do nothing
        }
    }

    private void setMapType(DJIMap.MapType mapType) {
        this.mapType = mapType;
        if (map != null) {
            map.setMapType(mapType);
        }
    }

    private Disposable reactToHeadingChanges() {
        return Flowable.combineLatest(widgetModel.getAircraftHeading(),
                widgetModel.getGimbalHeading(), Pair::create)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> {
                    updateAircraftHeading(values.first);
                    setGimbalHeading(values.first, values.second);
                }, logErrorConsumer(TAG, "react to Heading Update "));
    }

    /**
     * Updates location in Mapkit so adjustments can be made for certain countries.
     */
    private void updateHomeCountry() {
        Mapkit.inMacau(LocationUtil.isInMacau());
        Mapkit.inHongKong(LocationUtil.isInHongKong());
        Mapkit.inMainlandChina(LocationUtil.isInChina());
    }

    private void initDefaults() {
        aircraftIcon = getResources().getDrawable(R.drawable.uxsdk_ic_compass_aircraft);
        homeIcon = getResources().getDrawable(R.drawable.uxsdk_ic_home);
        gimbalYawIcon = getResources().getDrawable(R.drawable.uxsdk_ic_map_gimbal_yaw);
    }

    /**
     * Initializes the marker for home location
     *
     * @param homePosition Position of the home location
     */
    private void initHomeOnMap(DJILatLng homePosition) {
        if (map == null || !homePosition.isAvailable()) return;
        //Draw home marker
        DJIMarkerOptions homeMarkerOptions = new DJIMarkerOptions()
                .position(homePosition)
                .icon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(homeIcon)))
                .title(HOME_MARKER)
                .anchor(homeIconAnchorX, homeIconAnchorY)
                .zIndex(HOME_MARKER_ELEVATION)
                .visible(homeMarkerEnabled);
        homeMarker = map.addMarker(homeMarkerOptions);
        DJILog.d(TAG,
                "added home marker to map at ("
                        + homePosition.getLatitude()
                        + ","
                        + homePosition.getLongitude()
                        + ")");
        setMapCenter(mapCenterLockMode, DEFAULT_ZOOM, false);
    }

    /**
     * Updates the aircraft's home location on the map
     */
    private void updateHomeLocation(LocationCoordinate2D homeLocation) {
        if (homeLocation.getLatitude() == MapWidgetModel.INVALID_COORDINATE
                || homeLocation.getLongitude() == MapWidgetModel.INVALID_COORDINATE) return;
        DJILatLng homePosition = new DJILatLng(homeLocation.getLatitude(), homeLocation.getLongitude());
        if (map == null || !homePosition.isAvailable()) return;
        if (homeMarker != null) {
            homeMarker.setPosition(homePosition);
            updateCameraPosition();
        } else {
            initHomeOnMap(homePosition);
        }
    }

    /**
     * Initializes Marker for Aircraft
     *
     * @param aircraftPosition Position of the aircraft
     */
    private void initAircraftOnMap(DJILatLng aircraftPosition) {
        if (map == null || !aircraftPosition.isAvailable()) return;
        //Draw aircraft marker
        DJIMarkerOptions aircraftMarkerOptions = new DJIMarkerOptions()
                .position(aircraftPosition)
                .icon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(aircraftIcon)))
                .title(AIRCRAFT_MARKER)
                .anchor(aircraftIconAnchorX, aircraftIconAnchorY)
                .zIndex(AIRCRAFT_MARKER_ELEVATION)
                .visible(aircraftMarkerEnabled);
        aircraftMarker = map.addMarker(aircraftMarkerOptions);

        DJIMarkerOptions gimbalMarkerOptions = new DJIMarkerOptions()
                .position(aircraftPosition)
                .icon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(gimbalYawIcon)))
                .anchor(gimbalYawAnchorX, gimbalYawAnchorY)
                .zIndex(GIMBAL_MARKER_ELEVATION)
                .title(GIMBAL_YAW_MARKER)
                .visible(gimbalYawMarkerEnabled);
        gimbalYawMarker = map.addMarker(gimbalMarkerOptions);
        DJILog.d(TAG,
                "added aircraft marker to map at ("
                        + aircraftPosition.getLatitude()
                        + ","
                        + aircraftPosition.getLongitude()
                        + ")");

        setMapCenter(mapCenterLockMode, DEFAULT_ZOOM, false);
        getFlyZoneList();
    }

    private void getFlyZoneList() {
        addDisposable(widgetModel.getFlyZoneList()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onFlyZoneListUpdate,
                        logErrorConsumer(TAG, "get fly zone list  ")));
    }

    private void onFlyZoneListUpdate(List<FlyZoneInformation> flyZoneInformationList) {
        flyZoneHelper.onFlyZoneListUpdate(flyZoneInformationList);
    }

    private void updateAircraftHeading(float aircraftHeading) {
        if (((aircraftHeading >= 0 && aircraftMarkerHeading >= 0) ||
                (aircraftHeading <= 0 && aircraftMarkerHeading <= 0)) && map != null) {
            animateAircraftHeading(aircraftMarkerHeading,
                    aircraftHeading - map.getCameraPosition().bearing,
                    aircraftHeading);
        } else {
            setAircraftHeading(aircraftHeading);
        }
    }

    /**
     * Sets the aircraft heading on the map
     */
    private void setAircraftHeading(float aircraftHeading) {
        if (map == null) return;
        if (aircraftMarker != null) {
            rotateAircraftMarker(aircraftHeading - map.getCameraPosition().bearing);
        }
        aircraftMarkerHeading = aircraftHeading - map.getCameraPosition().bearing;
    }

    /**
     * Sets the gimbal heading on the map
     */
    private void setGimbalHeading(float aircraftHeading, float gimbalHeading) {
        if (map == null) return;
        if (gimbalYawMarker != null) {
            rotateGimbalMarker(gimbalHeading + aircraftHeading - map.getCameraPosition().bearing);
        }
    }

    /**
     * Animates the rotation of the aircraft
     */
    private void animateAircraftHeading(final float fromPosition, final float toPosition, float aircraftHeading) {
        if (map == null || aircraftMarker == null) return;

        //rotation animation
        ValueAnimator rotateAnimation =
                ValueAnimator.ofFloat(aircraftMarkerHeading, aircraftHeading - map.getCameraPosition().bearing);
        rotateAnimation.setDuration(ROTATION_ANIM_DURATION);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.addUpdateListener(valueAnimator -> {
            float progress = valueAnimator.getAnimatedFraction();
            float rotation = (toPosition - fromPosition) * progress + fromPosition;
            rotateAircraftMarker(rotation);
        });
        rotateAnimation.start();
        aircraftMarkerHeading = aircraftHeading - map.getCameraPosition().bearing;
    }

    /**
     * Sets the aircraft to the given rotation
     *
     * @param rotation the rotation to be set to
     */
    private void rotateAircraftMarker(float rotation) {
        if (aircraftMarker != null) {
            aircraftMarker.setRotation(rotation);
        }
    }

    /**
     * Sets the gimbal heading to the given rotation
     *
     * @param rotation the rotation to be set to
     */
    private void rotateGimbalMarker(float rotation) {
        if (gimbalYawMarker != null) {
            gimbalYawMarker.setRotation(rotation);
        }
    }

    /**
     * Animates the change in position of the aircraft
     *
     * @param toPosition   ending position
     * @param fromPosition starting position
     */
    private void animateAircraftMarker(final DJILatLng toPosition, final DJILatLng fromPosition) {
        ValueAnimator flightAnimation = ValueAnimator.ofFloat(0, 1);
        flightAnimation.setDuration(FLIGHT_ANIM_DURATION);
        flightAnimation.setInterpolator(new LinearInterpolator());
        flightAnimation.addUpdateListener(valueAnimator -> {
            float progress = valueAnimator.getAnimatedFraction();
            double latitude =
                    (toPosition.getLatitude() - fromPosition.getLatitude()) * progress + fromPosition.getLatitude();
            double longitude =
                    (toPosition.getLongitude() - fromPosition.getLongitude()) * progress + fromPosition.getLongitude();

            DJILatLng aircraftLatLng = new DJILatLng(latitude, longitude);
            if (aircraftLatLng.isAvailable()) {
                if (aircraftMarker != null) {
                    aircraftMarker.setPosition(aircraftLatLng);
                }
                if (gimbalYawMarker != null) {
                    gimbalYawMarker.setPosition(aircraftLatLng);
                }
            }
            updateCameraPosition();
        });
        flightAnimation.start();
    }

    /**
     * Changes position of camera to follow aircraft if camera is locked
     */
    private void updateCameraPosition() {
        centerRefreshCounter++;
        if (centerRefreshCounter > COUNTER_REFRESH_THRESHOLD) {
            centerRefreshCounter = 0;
            setMapCenter(mapCenterLockMode, DO_NOT_UPDATE_ZOOM, true);
        }
    }

    /**
     * Updates the aircraft location on the map
     */
    private void updateAircraftLocation(LocationCoordinate3D locationCoordinate3D) {
        if (map == null) return;
        if (locationCoordinate3D.getLatitude() == MapWidgetModel.INVALID_COORDINATE
                || locationCoordinate3D.getLongitude() == MapWidgetModel.INVALID_COORDINATE) return;

        final DJILatLng aircraftPosition = new DJILatLng(locationCoordinate3D.getLatitude(), locationCoordinate3D.getLongitude());
        if (aircraftMarker != null) {
            final DJILatLng markerPosition = aircraftMarker.getPosition();
            //Update marker
            animateAircraftMarker(aircraftPosition, markerPosition);
        } else if (aircraftPosition.isAvailable()) {
            //Create new marker
            initAircraftOnMap(aircraftPosition);
        }
        updateFlightPath();
        updateHomeDirection();
    }

    /**
     * Updates the line showing direction from aircraft to home location
     */
    private void updateHomeDirection() {
        //Update the aircraft to home path
        if (homeMarker == null || aircraftMarker == null || map == null) return;
        DJILatLng homeCoordinate = homeMarker.getPosition();
        if (homeDirectionEnabled) {
            if (homeLine != null) {
                List<DJILatLng> points = new ArrayList<>();
                points.add(aircraftMarker.getPosition());
                points.add(homeCoordinate);
                homeLine.setPoints(points);
            } else {
                //create new line
                DJIPolylineOptions homeLineOptions = new DJIPolylineOptions().add(aircraftMarker.getPosition())
                        .add(homeCoordinate)
                        .color(homeDirectionColor)
                        .width(homeDirectionWidth);

                //draw new line
                homeLine = map.addPolyline(homeLineOptions);
            }
        } else {
            if (homeLine != null) {
                homeLine.remove();
                homeLine = null;
            }
        }
    }

    /**
     * Sets the lock on the aircraft or the home location to be in center
     *
     * @param mapCenterLock the mode of centering
     * @param zoomLevel     the zoom level to set, or -1 to keep the current zoom level
     * @param animate       true if the camera should animate towards the point, false if it should go directly there
     */
    private void setMapCenter(MapCenterLock mapCenterLock, float zoomLevel, boolean animate) {
        if (map == null) return;
        if (!isTouching) {
            if (zoomLevel == DO_NOT_UPDATE_ZOOM) {
                zoomLevel = map.getCameraPosition().zoom;
            }
            DJICameraUpdate cameraUpdate = null;
            float rotation = map.getCameraPosition().getBearing();
            DJICameraPosition cameraPosition = null;
            switch (mapCenterLock) {
                case AIRCRAFT:
                    if (aircraftMarker != null) {
                        cameraPosition = new DJICameraPosition.Builder()
                                .bearing(rotation)
                                .target(aircraftMarker.getPosition())
                                .zoom(zoomLevel)
                                .build();
                    }
                    break;
                case HOME:
                    if (homeMarker != null) {
                        cameraPosition = new DJICameraPosition.Builder()
                                .bearing(rotation)
                                .target(homeMarker.getPosition())
                                .zoom(zoomLevel)
                                .build();
                    }
                    break;
                case NONE:
                default:
                    break;
            }
            if (cameraPosition != null) {
                cameraUpdate = DJICameraUpdateFactory.newCameraPosition(cameraPosition);
            }
            if (cameraUpdate != null) {
                if (animate) {
                    map.animateCamera(cameraUpdate);
                } else {
                    map.moveCamera(cameraUpdate);
                }
            }
            autoFrameMapBounds();
        }
    }

    /**
     * Keeps the home location and the aircraft location visible and adjust the map bounds when set true.
     */
    private void autoFrameMapBounds() {
        if (!isAutoFrameMapBounds) return;
        List<DJILatLng> latLngList = new ArrayList<>();
        if (homeMarker == null || aircraftMarker == null) {
            if (homeMarker != null) {
                latLngList.add(new DJILatLng(homeMarker.getPosition().getLatitude(),
                        homeMarker.getPosition().getLongitude()));
            }
            if (aircraftMarker != null) {
                latLngList.add(new DJILatLng(aircraftMarker.getPosition().getLatitude(),
                        aircraftMarker.getPosition().getLongitude()));
            }
        } else {
            double aircraftLat = aircraftMarker.getPosition().getLatitude();
            double aircraftLng = aircraftMarker.getPosition().getLongitude();
            double homeLat = homeMarker.getPosition().getLatitude();
            double homeLng = homeMarker.getPosition().getLongitude();
            double delta = 0.0002;
            if (mapCenterLockMode == MapCenterLock.AIRCRAFT) {
                final DJILatLng aircraftPosition = new DJILatLng(aircraftLat, aircraftLng);
                latLngList.add(aircraftPosition);
                double adjustedLat = homeLat > aircraftLat ? homeLat + delta : homeLat - delta;
                double adjustedLng = homeLng > aircraftLng ? homeLng + delta : homeLng - delta;
                final DJILatLng homePosition = new DJILatLng(adjustedLat, adjustedLng);
                latLngList.add(homePosition);
                final DJILatLng dummyLocation =
                        new DJILatLng(aircraftLat - (adjustedLat - aircraftLat), aircraftLng - (adjustedLng - aircraftLng));
                latLngList.add(dummyLocation);
            } else if (mapCenterLockMode == MapCenterLock.HOME) {
                final DJILatLng homePosition = new DJILatLng(homeLat, homeLng);
                latLngList.add(homePosition);
                double adjustedLat = aircraftLat > homeLat ? aircraftLat + delta : aircraftLat - delta;
                double adjustedLng = aircraftLng > homeLng ? aircraftLng + delta : aircraftLng - delta;
                final DJILatLng aircraftPosition = new DJILatLng(adjustedLat, adjustedLng);
                latLngList.add(aircraftPosition);
                final DJILatLng dummyLocation =
                        new DJILatLng(homeLat - (adjustedLat - homeLat), homeLng - (adjustedLng - homeLng));
                latLngList.add(dummyLocation);
            } else if (mapCenterLockMode == MapCenterLock.NONE) {
                double adjustedAircraftLat;
                double adjustedAircraftLng;
                double adjustedHomeLat;
                double adjustedHomeLng;
                if (aircraftLat > homeLat) {
                    adjustedAircraftLat = aircraftLat + delta;
                    adjustedHomeLat = homeLat - delta;
                } else {
                    adjustedAircraftLat = aircraftLat - delta;
                    adjustedHomeLat = homeLat + delta;
                }
                if (aircraftLng > homeLng) {
                    adjustedAircraftLng = aircraftLng + delta;
                    adjustedHomeLng = homeLng - delta;
                } else {
                    adjustedAircraftLng = aircraftLng - delta;
                    adjustedHomeLng = homeLng + delta;
                }
                latLngList.add(new DJILatLng(adjustedAircraftLat, adjustedAircraftLng));
                latLngList.add(new DJILatLng(adjustedHomeLat, adjustedHomeLng));
            }
        }
        if (!latLngList.isEmpty()) {
            DJILatLngBounds latLngBounds = DJILatLngBounds.fromLatLngs(latLngList);
            map.animateCamera(DJICameraUpdateFactory.newLatLngBounds(latLngBounds, 0, 100));
        }
    }
    //endregion

    //region map initializations

    /**
     * Initializes the MapWidget with Here Maps.
     * Note: Here Maps currently only works on arm v7 devices,  and you must sign up
     * for their premium package.
     *
     * @param listener The OnMapReadyListener which will invoke the onMapReady method when the map has finished
     *                 initializing.
     */
    public void initHereMap(@Nullable final OnMapReadyListener listener) {
        mapView = new HereProvider().dispatchMapViewRequest(getContext(), null);
        addView((ViewGroup) mapView, 0);
        mapView.getDJIMapAsync(map -> {
            MapWidget.this.map = map;
            postInit(listener);
            flyZoneHelper.initializeMap(map);
        });
    }

    /**
     * Initializes the MapWidget with Google Maps.
     * Note: Google Maps only works on devices with Google  Play Services (not Crystal
     * Sky).
     * Important: The following lifecycle methods in your activity must call  the
     * corresponding methods in MapWidget in order for the map to render correctly:
     * {@link #onCreate(Bundle)},  {@link #onResume()}, {@link #onPause()}, {@link
     * #onDestroy()}, {@link #onSaveInstanceState(Bundle)}, and {@link #onLowMemory()}.
     *
     * @param listener The OnMapReadyListener which will invoke the onMapReady method when the map has finished
     *                 initializing.
     */
    public void initGoogleMap(@Nullable final OnMapReadyListener listener) {
        mapView = new GoogleProvider().dispatchMapViewRequest(getContext(), null);
        addView((ViewGroup) mapView, 0);
        mapView.getDJIMapAsync(map -> {
            MapWidget.this.map = map;
            postInit(listener);
            flyZoneHelper.initializeMap(map);
        });
    }

    /**
     * Initializes the MapWidget with AMaps.
     *
     * @param listener The OnMapReadyListener which will invoke the onMapReady method when the map has finished
     *                 initializing.
     */
    public void initAMap(@Nullable final OnMapReadyListener listener) {
        mapView = new AMapProvider().dispatchMapViewRequest(getContext(), null);
        addView((ViewGroup) mapView, 0);
        mapView.getDJIMapAsync(map -> {
            MapWidget.this.map = map;
            postInit(listener);
            flyZoneHelper.initializeMap(map);
        });
    }

    /**
     * Initializes the MapWidget with Mapbox.
     *
     * @param listener          The OnMapReadyListener which will invoke the onMapReady method when the map has finished
     *                          initializing.
     * @param mapboxAccessToken The API access token from Mapbox.
     */
    public void initMapboxMap(@NonNull String mapboxAccessToken, @Nullable final OnMapReadyListener listener) {
        Mapkit.mapboxAccessToken(mapboxAccessToken);
        mapView = new MapboxProvider().dispatchMapViewRequest(getContext(), null);
        addView((ViewGroup) mapView, 0);
        mapView.getDJIMapAsync(map -> {
            MapWidget.this.map = map;
            postInit(listener);
            flyZoneHelper.initializeMap(map);
        });
    }

    /**
     * Perform common initialization steps after the specific provider's map has finished initializing
     */
    @SuppressWarnings("SameReturnValue")
    private void postInit(OnMapReadyListener listener) {
        updateHomeCountry();
        map.setMapType(mapType, () -> {
            Single<LocationCoordinate3D> aircraftLocation = widgetModel.getAircraftLocation().firstOrError();
            Single<LocationCoordinate2D> homeLocation = widgetModel.getHomeLocation().firstOrError();
            addDisposable(Single.zip(aircraftLocation, homeLocation, Pair::new)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(pair -> {
                        updateAircraftLocation(pair.first);
                        updateHomeLocation(pair.second);
                        if (listener != null) {
                            listener.onMapReady(map);
                        }
                    }, logErrorConsumer(TAG, "updateAircraftAndHomeLocation")));
        });
        map.setOnMarkerClickListener(marker -> {
            String title = marker.getTitle();
            if (title != null && title.length() > 0
                    && !GIMBAL_YAW_MARKER.equals(title)
                    && !AIRCRAFT_MARKER.equals(title)
                    && !HOME_MARKER.equals(title)
                    && MathUtil.isInteger(title)
                    && flyZoneHelper.isFlyZoneMarkerId(title)) {
                flyZoneHelper.onFlyZoneMarkerClick(title);
            } else {
                emitMarkerClickEvent(marker);
            }
            return true;
        });
        addDisposable(widgetModel.getAircraftLocation()
                .firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateAircraftLocation, logErrorConsumer(TAG, "updateAircraftLocation")));
        addDisposable(widgetModel.getHomeLocation()
                .firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateHomeLocation, logErrorConsumer(TAG, "updateHomeLocation")));
    }

    private void emitMarkerClickEvent(DJIMarker marker) {
        if (onMarkerClickListener != null) {
            onMarkerClickListener.onMarkerClick(marker);
        }
    }

    /**
     * Updates the aircraft's flight path
     */
    private void updateFlightPath() {
        if (aircraftMarker == null) return;
        DJILatLng aircraftPosition = aircraftMarker.getPosition();

        if (flightPathPoints.size() >= 2) {
            DJILatLng lastPosition = flightPathPoints.get(flightPathPoints.size() - 1);
            if (Math.abs(lastPosition.getLatitude() - aircraftPosition.getLatitude()) > 0.000005
                    || Math.abs(lastPosition.getLongitude() - aircraftPosition.getLongitude()) > 0.000005) {
                flightPathPoints.add(aircraftPosition);
            }
        } else {
            //new polylines require 2+ points
            flightPathPoints.add(aircraftPosition);
            flightPathPoints.add(aircraftPosition);
        }

        refreshFlightPath();
    }

    /**
     * Removes previous flight path and replaces it with the new one
     */
    private void refreshFlightPath() {
        if (aircraftMarker == null || map == null) return;
        //must create new line or else flightPathLine does not update otherwise
        if (flightPathEnabled) {
            if (flightPathLine == null) {
                DJIPolylineOptions polylineOptions = new DJIPolylineOptions()
                        .addAll(flightPathPoints)
                        .color(flightPathColor)
                        .width(flightPathWidth);
                flightPathLine = map.addPolyline(polylineOptions);
            } else {
                flightPathLine.setPoints(flightPathPoints);
            }
        } else {
            if (flightPathLine != null) {
                flightPathLine.remove();
                flightPathLine = null;
            }
        }
    }

    private boolean isValidAlpha(int alphaValue) {
        return (alphaValue >= MIN_ALPHA && alphaValue <= MAX_ALPHA);
    }

    //endregion

    //region customizations flight path and home direction

    /**
     * Clears the flight path up to the current location. The flight path is removed
     * even if it is hidden.
     */
    public void clearFlightPath() {
        if (flightPathLine == null) return;
        flightPathPoints.clear();
        flightPathLine.remove();
        flightPathLine = null;
        updateFlightPath();
    }

    /**
     * `true` if the flight path is visible. The default value is `false`
     *
     * @return A boolean value indicating if the flight path is visible.
     */
    public boolean isFlightPathEnabled() {
        return flightPathEnabled;
    }

    /**
     * Sets the flight path visibility.
     *
     * @param isEnabled A boolean value that determines whether to show the flight path.
     */
    public void setFlightPathEnabled(boolean isEnabled) {
        flightPathEnabled = isEnabled;
        refreshFlightPath();
    }

    /**
     * Gets the color of the flight path.
     *
     * @return The color of the flight path.
     */
    @ColorInt
    public int getFlightPathColor() {
        return flightPathColor;
    }

    /**
     * Sets the color of the flight path.
     *
     * @param color The color of the flight path.
     */
    public void setFlightPathColor(@ColorInt int color) {
        flightPathColor = color;
        if (flightPathEnabled && flightPathLine != null) {
            flightPathLine.setColor(flightPathColor);
        }
    }

    /**
     * Gets the line width, in pixels, of the flight path. Valid range is 0-100.
     *
     * @return The width in pixels of the flight path.
     */
    public float getFlightPathWidth() {
        return flightPathWidth;
    }

    /**
     * Sets a line width, in pixels, for the flight path. Valid range is 0-100.
     *
     * @param width The width in pixels of the flight path.
     */
    public void setFlightPathWidth(float width) {
        flightPathWidth = width;
        if (flightPathEnabled && flightPathLine != null) {
            flightPathLine.setWidth(flightPathWidth);
        }
    }

    /**
     * Defaults to `false`. A Boolean value indicating whether the map displays a line
     * showing
     * the direction to home.
     *
     * @return `true` if direction to home is visible.
     */
    public boolean isDirectionToHomeEnabled() {
        return homeDirectionEnabled;
    }

    /**
     * Sets the visibility of the path from aircraft to home point.
     *
     * @param isEnabled A boolean value that determines whether to show the path from aircraft to home point.
     */
    public void setDirectionToHomeEnabled(boolean isEnabled) {
        homeDirectionEnabled = isEnabled;
        updateHomeDirection();
    }

    /**
     * Gets the color of the path from aircraft to home point.
     *
     * @return The color of the path.
     */
    @ColorInt
    public int getDirectionToHomeColor() {
        return homeDirectionColor;
    }

    /**
     * Sets the color of the path from aircraft to home point.
     *
     * @param color The new color of the path.
     */
    public void setDirectionToHomeColor(@ColorInt int color) {
        homeDirectionColor = color;
        if (homeDirectionEnabled && homeLine != null) {
            homeLine.setColor(color);
        }
    }

    /**
     * Gets the width of the path from aircraft to home point.
     *
     * @return The width of the path.
     */
    public float getDirectionToHomeWidth() {
        return homeDirectionWidth;
    }

    /**
     * Sets the width of the path from aircraft to home point.
     *
     * @param width The width of the path.
     */
    public void setDirectionToHomeWidth(float width) {
        homeDirectionWidth = width;
        if (homeDirectionEnabled && homeLine != null) {
            homeLine.setWidth(width);
        }
    }

    /**
     * Get the DJIMap object.
     *
     * @return A DJIMap object.
     */
    @Nullable
    public DJIMap getMap() {
        return map;
    }
    //endregion

    //region marker customizations

    /**
     * Changes the icon of the aircraft marker.
     * Note: When using HERE Maps, the anchor point does not rotate with the marker.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setAircraftMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        aircraftIconAnchorX = x;
        aircraftIconAnchorY = y;
        setAircraftMarkerIcon(drawable);
        if (aircraftMarker != null) {
            aircraftMarker.setAnchor(x, y);
        }
    }

    /**
     * Get the aircraft marker icon
     *
     * @return Drawable used as aircraft icon
     */
    @NonNull
    public Drawable getAircraftMarkerIcon() {
        return aircraftIcon;
    }

    /**
     * Changes the icon of the aircraft marker
     *
     * @param drawable The image to be set.
     */
    public void setAircraftMarkerIcon(@NonNull Drawable drawable) {
        aircraftIcon = drawable;
        if (aircraftMarker != null) {
            aircraftMarker.setIcon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(
                    aircraftIcon)));
        }
    }

    /**
     * Changes the icon of the home marker.
     * Note: When using HERE Maps, the anchor point does not rotate with the marker.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setHomeMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        homeIconAnchorX = x;
        homeIconAnchorY = y;
        setHomeMarkerIcon(drawable);
        if (homeMarker != null) {
            homeMarker.setAnchor(x, y);
        }
    }

    /**
     * Get the home marker icon
     *
     * @return Drawable used as home icon
     */
    @NonNull
    public Drawable getHomeMarkerIcon() {
        return homeIcon;
    }

    /**
     * Changes the icon of the home marker
     *
     * @param drawable The image to be set.
     */
    public void setHomeMarkerIcon(@NonNull Drawable drawable) {
        homeIcon = drawable;
        if (homeMarker != null) {
            homeMarker.setIcon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(homeIcon)));
        }
    }

    /**
     * Changes the icon of the gimbal marker.
     * Note: When using HERE Maps, the anchor point does not rotate with the marker.
     *
     * @param drawable The image to be set.
     * @param x        Specifies the x axis value of anchor to be at a particular point in the marker image.
     * @param y        Specifies the y axis value of anchor to be at a particular point in the marker image.
     */
    public void setGimbalMarkerIcon(@NonNull Drawable drawable, float x, float y) {
        gimbalYawAnchorX = x;
        gimbalYawAnchorY = y;
        setGimbalMarkerIcon(drawable);
        if (gimbalYawMarker != null) {
            gimbalYawMarker.setAnchor(x, y);
        }
    }

    /**
     * Get the gimbal icon
     *
     * @return Drawable used as gimbal icon
     */
    @NonNull
    public Drawable getGimbalMarkerIcon() {
        return gimbalYawIcon;
    }

    /**
     * Changes the icon of the gimbal icon
     *
     * @param drawable The image to be set.
     */
    public void setGimbalMarkerIcon(@NonNull Drawable drawable) {
        gimbalYawIcon = drawable;
        if (gimbalYawMarker != null) {
            gimbalYawMarker.setIcon(DJIBitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(
                    gimbalYawIcon)));
        }
    }

    /**
     * Gets the visibility of the gimbal attitude marker.
     *
     * @return `true` if the gimbal attitude marker is visible.
     */
    public boolean isGimbalAttitudeEnabled() {
        return gimbalYawMarkerEnabled;
    }

    /**
     * Sets the visibility of the gimbal attitude marker.
     *
     * @param isVisible A boolean value that determines whether to show the gimbal attitude marker.
     */
    public void setGimbalAttitudeEnabled(boolean isVisible) {
        gimbalYawMarkerEnabled = isVisible;
        if (gimbalYawMarker != null) {
            gimbalYawMarker.setVisible(isVisible);
        }
    }

    /**
     * Gets the visibility of the aircraft marker.
     *
     * @return `true` if the aircraft marker is visible.
     */
    public boolean isAircraftMarkerEnabled() {
        return aircraftMarkerEnabled;
    }

    /**
     * Sets the visibility of the aircraft marker.
     *
     * @param isVisible A boolean value that determines whether to show the aircraft marker.
     */
    public void setAircraftMarkerEnabled(boolean isVisible) {
        aircraftMarkerEnabled = isVisible;
        if (aircraftMarker != null) {
            aircraftMarker.setVisible(isVisible);
        }
    }

    /**
     * `true` if the map displays the home point of the aircraft. The default value of
     * this property is `true`.
     *
     * @return The icon of the home point marker.
     */
    public boolean isHomeMarkerEnabled() {
        return homeMarkerEnabled;
    }

    /**
     * Sets the visibility of the home marker if present on the map.
     *
     * @param isEnabled A boolean value to determine if the home marker is visible.
     */
    public void setHomeMarkerEnabled(boolean isEnabled) {
        homeMarkerEnabled = isEnabled;
        if (homeMarker != null) {
            homeMarker.setVisible(isEnabled);
        }
    }
    //endregion

    //region map customizations

    /**
     * Sets the OnMarkerClickListener for this widget.
     *
     * @param onMarkerClickListener The listener that is added to this widget.
     */
    public void setOnMarkerClickListener(DJIMap.OnMarkerClickListener onMarkerClickListener) {
        this.onMarkerClickListener = onMarkerClickListener;
    }

    /**
     * Sets the lock on the aircraft or the home location to be in center.
     *
     * @param mapCenterLock Parameter to select the mode of centering.
     */
    public void setMapCenterLock(@NonNull MapCenterLock mapCenterLock) {
        mapCenterLockMode = mapCenterLock;
        setMapCenter(mapCenterLock, DO_NOT_UPDATE_ZOOM, true);
    }

    /**
     * Check if auto frame map is enabled
     *
     * @return boolean val
     */
    public boolean isAutoFrameMapEnabled() {
        return isAutoFrameMapBounds;
    }

    /**
     * Keeps the home location and the aircraft location visible and adjust the map
     * bounds when set `true`.
     *
     * @param isEnabled Parameter to enable or disable the map bounds lock.
     */
    public void setAutoFrameMapEnabled(boolean isEnabled) {
        isAutoFrameMapBounds = isEnabled;
        autoFrameMapBounds();
    }

    /**
     * Get custom unlock zones from server
     */
    public void requestCustomUnlockZonesFromServer() {
        if (flyZoneHelper.isUserAuthorized()) {
            addDisposable(Single.zip(widgetModel.getCustomUnlockZonesFromServer(),
                    widgetModel.getCustomUnlockZonesFromAircraft(), Pair::new)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(result -> flyZoneHelper.onCustomUnlockZoneUpdate(result.second, result.first),
                            logErrorConsumer(TAG, "get custom unlock zones ")));
        }
    }

    /**
     * Upload custom unlock zones to aircraft
     */
    public void syncCustomUnlockZonesToAircraft() {
        if (flyZoneHelper.isUserAuthorized()) {
            addDisposable(widgetModel.syncZonesToAircraft()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::requestCustomUnlockZonesFromServer,
                            logErrorConsumer(TAG, "sync custom unlock zones ")));
        }
    }

    /**
     * Get user account indicator to customize
     *
     * @return instance of {@link UserAccountLoginWidget}
     */
    @NonNull
    public UserAccountLoginWidget getUserAccountLoginWidget() {
        return userAccountLoginWidget;
    }

    /**
     * Get helper class to customize fly zones
     *
     * @return instance of {@link FlyZoneHelper}
     */
    @NonNull
    public FlyZoneHelper getFlyZoneHelper() {
        return flyZoneHelper;
    }

    /**
     * Check if FlyZone legend is enabled
     *
     * @return true - legend is visible false - legend is hidden
     */
    public boolean isFlyZoneLegendEnabled() {
        return legendGroup.getVisibility() == VISIBLE;
    }

    /**
     * Show/Hide FlyZone legend
     *
     * @param isEnabled true - flyZone legend visible false - flyZone legend not visible
     */
    public void setFlyZoneLegendEnabled(boolean isEnabled) {
        legendGroup.setVisibility(isEnabled ? VISIBLE : GONE);
    }

    //endregion

    /**
     * Map Centering Options.
     */
    public enum MapCenterLock {

        /**
         * This will disable the centering of map on any markers.
         */
        NONE(0),
        /**
         * This will keep the aircraft icon always in center of the screen and keep
         * following it during flight.
         */
        AIRCRAFT(1),
        /**
         * This will keep the home icon always in center of the screen. The aircraft icon
         * will disappear from view bounds if the aircraft travels that distance
         */
        HOME(2);

        private int index;

        MapCenterLock(int index) {
            this.index = index;
        }

        private static MapCenterLock[] values;

        public static MapCenterLock[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        @NonNull
        public static MapCenterLock find(@IntRange(from = -1, to = 2) int index) {
            for (MapCenterLock mapCenterLock : MapCenterLock.getValues()) {
                if (mapCenterLock.getIndex() == index) {
                    return mapCenterLock;
                }
            }
            return NONE;
        }

        public int getIndex() {
            return index;
        }

    }

    /**
     * When added to the MapWidget, the OnMapReadyListener can be used to  determine
     * when the map is ready to modify. No modifications should  be done to the
     * MapWidget before the map is initialized using one  of the initialization
     * methods.
     */
    public interface OnMapReadyListener {

        /**
         * A callback indicating that the map is finished initializing.
         *
         * @param map The object of <code>DJIMap</code>.
         */
        void onMapReady(@NonNull DJIMap map);
    }
}
