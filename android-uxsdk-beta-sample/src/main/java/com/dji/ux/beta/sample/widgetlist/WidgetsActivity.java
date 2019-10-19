/*
 * Copyright (c) 2018-2019 DJI
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
 *
 */

package com.dji.ux.beta.sample.widgetlist;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.dji.ux.beta.sample.R;

import java.util.ArrayList;

import dji.ux.beta.widget.altitude.AltitudeWidget;
import dji.ux.beta.widget.autoexposurelock.AutoExposureLockWidget;
import dji.ux.beta.widget.cameracapture.CameraCaptureWidget;
import dji.ux.beta.widget.cameracapture.recordvideo.RecordVideoWidget;
import dji.ux.beta.widget.cameracapture.shootphoto.ShootPhotoWidget;
import dji.ux.beta.widget.cameraconfig.aperture.CameraConfigApertureWidget;
import dji.ux.beta.widget.cameraconfig.ev.CameraConfigEVWidget;
import dji.ux.beta.widget.cameraconfig.iso.CameraConfigISOAndEIWidget;
import dji.ux.beta.widget.cameraconfig.shutter.CameraConfigShutterWidget;
import dji.ux.beta.widget.cameraconfig.ssd.CameraConfigSSDWidget;
import dji.ux.beta.widget.cameraconfig.storage.CameraConfigStorageWidget;
import dji.ux.beta.widget.cameraconfig.wb.CameraConfigWBWidget;
import dji.ux.beta.widget.cameracontrols.CameraControlsWidget;
import dji.ux.beta.widget.cameracontrols.camerasettingsindicator.CameraSettingsMenuIndicatorWidget;
import dji.ux.beta.widget.cameracontrols.exposuresettingsindicator.ExposureSettingsIndicatorWidget;
import dji.ux.beta.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget;
import dji.ux.beta.widget.compass.CompassWidget;
import dji.ux.beta.widget.dashboard.DashboardWidget;
import dji.ux.beta.widget.distancehome.DistanceHomeWidget;
import dji.ux.beta.widget.distancerc.DistanceRCWidget;
import dji.ux.beta.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.ux.beta.widget.focusmode.FocusModeWidget;
import dji.ux.beta.widget.fpv.FPVWidget;
import dji.ux.beta.widget.fpv.interaction.FPVInteractionWidget;
import dji.ux.beta.widget.horizontalvelocity.HorizontalVelocityWidget;
import dji.ux.beta.widget.preflightstatus.PreFlightStatusWidget;
import dji.ux.beta.widget.remotecontrolsignal.RemoteControlSignalWidget;
import dji.ux.beta.widget.simulator.SimulatorControlWidget;
import dji.ux.beta.widget.simulator.SimulatorIndicatorWidget;
import dji.ux.beta.widget.useraccount.UserAccountLoginWidget;
import dji.ux.beta.widget.verticalvelocity.VerticalVelocityWidget;
import dji.ux.beta.widget.vision.VisionWidget;
import dji.ux.beta.widget.vps.VPSWidget;

/**
 * Displays a list of widget names. Clicking on a widget name will show that widget in a separate
 * panel on large devices, or in a new activity on smaller devices.
 */
public class WidgetsActivity extends AppCompatActivity implements WidgetListFragment.OnWidgetItemSelectedListener {

    //region Fields
    protected ArrayList<WidgetListItem> widgetListItems;
    private WidgetListFragment listFragment;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populateList();
        setContentView(R.layout.activity_widgets);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            listFragment = new WidgetListFragment();
            listFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, listFragment)
                    .commit();
        } else {
            listFragment = (WidgetListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_widget_list);
        }
    }
    //endregion

    /**
     * Initializes the list of widget names.
     */
    private void populateList() {
        widgetListItems = new ArrayList<>();
        widgetListItems.add(new WidgetListItem(R.string.altitude_widget_title,
                new WidgetViewHolder(AltitudeWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.auto_exposure_lock_widget_title,
                new WidgetViewHolder(AutoExposureLockWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.camera_capture_widget_title,
                new WidgetViewHolder(CameraCaptureWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_aperture_widget_title,
                new WidgetViewHolder(CameraConfigApertureWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_ev_widget_title,
                new WidgetViewHolder(CameraConfigEVWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_iso_widget_title,
                new WidgetViewHolder(CameraConfigISOAndEIWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_shutter_widget_title,
                new WidgetViewHolder(CameraConfigShutterWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_ssd_widget_title,
                new WidgetViewHolder(CameraConfigSSDWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 28)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_storage_widget_title,
                new WidgetViewHolder(CameraConfigStorageWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 28)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_wb_widget_title,
                new WidgetViewHolder(CameraConfigWBWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_controls_widget_title,
                new WidgetViewHolder(CameraControlsWidget.class, 50, 213)));
        widgetListItems.add(new WidgetListItem(R.string.camera_settings_menu_indicator_widget_title,
                new WidgetViewHolder(CameraSettingsMenuIndicatorWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.compass_widget_title,
                new WidgetViewHolder(CompassWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 91)));
        widgetListItems.add(new WidgetListItem(R.string.dashboard_widget_title,
                new WidgetViewHolder(DashboardWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 91)));
        widgetListItems.add(new WidgetListItem(R.string.distance_home_widget_title,
                new WidgetViewHolder(DistanceHomeWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.distance_rc_widget_title,
                new WidgetViewHolder(DistanceRCWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.exposure_settings_indicator_widget_title,
                new WidgetViewHolder(ExposureSettingsIndicatorWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.focus_exposure_switch_widget_title,
                new WidgetViewHolder(FocusExposureSwitchWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.focus_mode_widget_title,
                new WidgetViewHolder(FocusModeWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.fpv_widget_title,
                new WidgetViewHolder(FPVWidget.class, 150, 100)));
        widgetListItems.add(new WidgetListItem(R.string.fpv_interaction_widget_title,
                new WidgetViewHolder(FPVInteractionWidget.class, 150, 100)));
        widgetListItems.add(new WidgetListItem(R.string.horizontal_velocity_widget_title,
                new WidgetViewHolder(HorizontalVelocityWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.photo_video_switch_widget_title,
                new WidgetViewHolder(PhotoVideoSwitchWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.pre_flight_status_widget_title,
                new WidgetViewHolder(PreFlightStatusWidget.class, 238, 33)));
        widgetListItems.add(new WidgetListItem(R.string.record_video_widget_title,
                new WidgetViewHolder(RecordVideoWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.remote_control_signal_widget_title,
                new WidgetViewHolder(RemoteControlSignalWidget.class, 38, 22)));
        widgetListItems.add(new WidgetListItem(R.string.shoot_photo_widget_title,
                new WidgetViewHolder(ShootPhotoWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.simulator_indicator_control_widgets_title,
                new WidgetViewHolder(SimulatorIndicatorWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                new WidgetViewHolder(SimulatorControlWidget.class, 300, ViewGroup.LayoutParams.MATCH_PARENT)));
        widgetListItems.add(new WidgetListItem(R.string.user_account_login_widget_title,
                new WidgetViewHolder(UserAccountLoginWidget.class,240,60)));
        widgetListItems.add(new WidgetListItem(R.string.vertical_velocity_widget_title,
                new WidgetViewHolder(VerticalVelocityWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.vision_widget_title,
                new WidgetViewHolder(VisionWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.vps_widget_title,
                new WidgetViewHolder(VPSWidget.class)));
    }

    @Override
    public void onWidgetItemSelected(int position) {
        WidgetFragment widgetFragment = (WidgetFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_widget);

        if (widgetFragment != null) {
            // Two-pane layout - update the fragment
            widgetFragment.updateWidgetView(position);
            listFragment.updateSelectedView(position);
        } else {
            // One-pane layout - swap fragments
            WidgetFragment newFragment = new WidgetFragment();
            Bundle args = new Bundle();
            args.putInt(WidgetFragment.ARG_POSITION, position);
            newFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
