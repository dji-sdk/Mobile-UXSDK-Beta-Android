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

package dji.ux.beta.core.widget.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.ux.beta.R;
import dji.ux.beta.core.base.ConstraintLayoutWidget;
import dji.ux.beta.core.widget.altitude.AltitudeWidget;
import dji.ux.beta.core.widget.compass.CompassWidget;
import dji.ux.beta.core.widget.distancehome.DistanceHomeWidget;
import dji.ux.beta.core.widget.distancerc.DistanceRCWidget;
import dji.ux.beta.core.widget.horizontalvelocity.HorizontalVelocityWidget;
import dji.ux.beta.core.widget.verticalvelocity.VerticalVelocityWidget;
import dji.ux.beta.core.widget.vps.VPSWidget;

/**
 * Compound widget that aggregates important physical state information
 * about the aircraft into a dashboard.
 * <p>
 * It includes the {@link CompassWidget}, {@link AltitudeWidget},
 * {@link DistanceHomeWidget}, {@link DistanceRCWidget},
 * {@link HorizontalVelocityWidget}, {@link VerticalVelocityWidget}
 * and the {@link VPSWidget}.
 * <p>
 * This widget can be customized to include or exclude any of these widgets
 * as required.
 */
public class DashboardWidget extends ConstraintLayoutWidget {
    //region Fields
    private CompassWidget compassWidget;
    private AltitudeWidget altitudeWidget;
    private DistanceHomeWidget distanceHomeWidget;
    private DistanceRCWidget distanceRCWidget;
    private HorizontalVelocityWidget horizontalVelocityWidget;
    private VerticalVelocityWidget verticalVelocityWidget;
    private VPSWidget vpsWidget;
    //endregion

    //region Constructors
    public DashboardWidget(Context context) {
        super(context);
    }

    public DashboardWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DashboardWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_dashboard, this);

        compassWidget = findViewById(R.id.widget_compass);
        altitudeWidget = findViewById(R.id.widget_altitude);
        distanceHomeWidget = findViewById(R.id.widget_distance_home);
        distanceRCWidget = findViewById(R.id.widget_distance_rc);
        horizontalVelocityWidget = findViewById(R.id.widget_horizontal_velocity);
        verticalVelocityWidget = findViewById(R.id.widget_vertical_velocity);
        vpsWidget = findViewById(R.id.widget_vps);

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
    @Override
    protected void reactToModelChanges() {
        //No reactions
    }
    //endregion

    //region Customizations
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_dashboard_ratio);
    }

    /**
     * Get the compass widget object
     *
     * @return CompassWidget object
     */
    public CompassWidget getCompassWidget() {
        return compassWidget;
    }

    /**
     * Get the altitude widget object
     *
     * @return AltitudeWidget object
     */
    public AltitudeWidget getAltitudeWidget() {
        return altitudeWidget;
    }

    /**
     * Get the distance from home widget object
     *
     * @return DistanceHomeWidget object
     */
    public DistanceHomeWidget getDistanceHomeWidget() {
        return distanceHomeWidget;
    }

    /**
     * Get the distance from RC widget object
     *
     * @return DistanceRCWidget object
     */
    public DistanceRCWidget getDistanceRCWidget() {
        return distanceRCWidget;
    }

    /**
     * Get the horizontal velocity widget object
     *
     * @return HorizontalVelocityWidget object
     */
    public HorizontalVelocityWidget getHorizontalVelocityWidget() {
        return horizontalVelocityWidget;
    }

    /**
     * Get the vertical velocity widget object
     *
     * @return VerticalVelocityWidget object
     */
    public VerticalVelocityWidget getVerticalVelocityWidget() {
        return verticalVelocityWidget;
    }

    /**
     * Get the VPS widget object
     *
     * @return VPSWidget object
     */
    public VPSWidget getVPSWidget() {
        return vpsWidget;
    }

    /**
     * Set the visibility of the compass widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setCompassWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            compassWidget.setVisibility(View.VISIBLE);
        } else {
            compassWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the altitude widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setAltitudeWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            altitudeWidget.setVisibility(View.VISIBLE);
        } else {
            altitudeWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the distance from home widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setDistanceHomeWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            distanceHomeWidget.setVisibility(View.VISIBLE);
        } else {
            distanceHomeWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the distance from RC widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setDistanceRCWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            distanceRCWidget.setVisibility(View.VISIBLE);
        } else {
            distanceRCWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the horizontal velocity widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setHorizontalVelocityWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            horizontalVelocityWidget.setVisibility(View.VISIBLE);
        } else {
            horizontalVelocityWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the vertical velocity widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setVerticalVelocityWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            verticalVelocityWidget.setVisibility(View.VISIBLE);
        } else {
            verticalVelocityWidget.setVisibility(View.GONE);
        }
    }

    /**
     * Set the visibility of the VPS widget object
     *
     * @param isVisible Boolean value for visibility
     */
    public void setVPSWidgetVisibility(boolean isVisible) {
        if (isVisible) {
            vpsWidget.setVisibility(View.VISIBLE);
        } else {
            vpsWidget.setVisibility(View.GONE);
        }
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DashboardWidget);

        int hiddenWidgets = typedArray.getInteger(R.styleable.DashboardWidget_uxsdk_hideWidgets, HideWidget.NONE.value());
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.COMPASS)) {
            setCompassWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.ALTITUDE)) {
            setAltitudeWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.DISTANCE_HOME)) {
            setDistanceHomeWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.DISTANCE_RC)) {
            setDistanceRCWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.HORIZONTAL_VELOCITY)) {
            setHorizontalVelocityWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.VERTICAL_VELOCITY)) {
            setVerticalVelocityWidgetVisibility(false);
        }
        if (HideWidget.isWidgetHidden(hiddenWidgets, HideWidget.VPS)) {
            setVPSWidgetVisibility(false);
        }

        typedArray.recycle();
    }

    /**
     * Enum of all the widgets in the dashboard to set their visibility
     */
    private enum HideWidget {
        NONE(0),
        COMPASS(1),
        ALTITUDE(2),
        DISTANCE_HOME(4),
        DISTANCE_RC(8),
        HORIZONTAL_VELOCITY(16),
        VERTICAL_VELOCITY(32),
        VPS(64);

        private final int value;

        HideWidget(int value) {
            this.value = value;
        }

        protected static boolean isWidgetHidden(int hiddenWidgets, HideWidget widget) {
            return (hiddenWidgets & widget.value()) == widget.value;
        }

        public int value() {
            return this.value;
        }
    }
    //endregion
}
