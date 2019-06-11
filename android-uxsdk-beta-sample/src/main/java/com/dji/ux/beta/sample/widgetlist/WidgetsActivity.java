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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.dji.ux.beta.sample.R;

import java.util.ArrayList;

import dji.ux.beta.widget.altitude.AltitudeWidget;
import dji.ux.beta.widget.compass.CompassWidget;
import dji.ux.beta.widget.dashboard.DashboardWidget;
import dji.ux.beta.widget.distancehome.DistanceHomeWidget;
import dji.ux.beta.widget.distancerc.DistanceRCWidget;
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
        widgetListItems.add(new WidgetListItem(R.string.compass_widget_title,
                new WidgetViewHolder(CompassWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 91)));
        widgetListItems.add(new WidgetListItem(R.string.dashboard_widget_title,
                new WidgetViewHolder(DashboardWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 91)));
        widgetListItems.add(new WidgetListItem(R.string.distance_home_widget_title,
                new WidgetViewHolder(DistanceHomeWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.distance_rc_widget_title,
                new WidgetViewHolder(DistanceRCWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.horizontal_velocity_widget_title,
                new WidgetViewHolder(HorizontalVelocityWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.pre_flight_status_widget_title,
                new WidgetViewHolder(PreFlightStatusWidget.class, 238, 33)));
        widgetListItems.add(new WidgetListItem(R.string.remote_control_signal_widget_title,
                new WidgetViewHolder(RemoteControlSignalWidget.class, 38, 22)));
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
