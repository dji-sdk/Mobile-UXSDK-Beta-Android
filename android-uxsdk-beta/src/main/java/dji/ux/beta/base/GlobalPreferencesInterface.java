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
 */

package dji.ux.beta.base;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import dji.ux.beta.util.SettingDefinitions;
import dji.ux.beta.util.UnitConversionUtil;
import dji.ux.beta.widget.fpv.CenterPointView;
import dji.ux.beta.widget.fpv.GridLineView;

/**
 * Interface to be implemented for functions included under
 * global preferences. These settings will persist across app restarts.
 */
public interface GlobalPreferencesInterface {
    /**
     * Set up the listeners for the global preferences interface
     */
    void setUpListener();

    /**
     * Clean up the listeners for the global preferences interface
     */
    void cleanup();

    //region global Settings interface

    /**
     * Get the global unit type from {@link dji.ux.beta.util.UnitConversionUtil.UnitType}
     *
     * @return Current UnitType value if saved or default value to be used if null.
     */
    @NonNull
    UnitConversionUtil.UnitType getUnitType();

    /**
     * Get the global unit type from {@link dji.ux.beta.util.UnitConversionUtil.UnitType}
     *
     * @param unitType UnitType value
     */
    void setUnitType(@NonNull UnitConversionUtil.UnitType unitType);

    /**
     * Get if continuous auto-focus is enabled.
     *
     * @return Boolean value for AFC enabled if saved or default value to be used.
     */
    boolean getAFCEnabled();

    /**
     * Set the value to enable or disable continuous auto focus
     *
     * @param enabled Boolean value for AFC enabled - true to enable, false to disable
     */
    void setAFCEnabled(boolean enabled);

    /**
     * Get the GridLine type from {@link dji.ux.beta.widget.fpv.GridLineView.GridLineType}
     * for the grid line overlay.
     *
     * @return Current GridLineType value or default value if null
     */
    @NonNull
    GridLineView.GridLineType getGridLineType();

    /**
     * Set the GridLine type from {@link dji.ux.beta.widget.fpv.GridLineView.GridLineType}
     *
     * @param gridLineType GridLineType value
     */
    void setGridLineType(@NonNull GridLineView.GridLineType gridLineType);

    /**
     * Get the Center Point Type from {@link dji.ux.beta.widget.fpv.CenterPointView.CenterPointType}
     *
     * @return Current CenterPointType value or default value
     */
    @NonNull
    CenterPointView.CenterPointType getCenterPointType();

    /**
     * Set the CenterPointType from {@link dji.ux.beta.widget.fpv.CenterPointView.CenterPointType}
     *
     * @param centerPointType CenterPointType value
     */
    void setCenterPointType(@NonNull CenterPointView.CenterPointType centerPointType);

    /**
     * Get the current center point color or default value if not set
     *
     * @return Color integer resource
     */
    @ColorInt
    int getCenterPointColor();

    /**
     * Set the center point color
     *
     * @param centerPointColor Color integer resource
     */
    void setCenterPointColor(@ColorInt int centerPointColor);

    /**
     * Get the current control mode from {@link dji.ux.beta.util.SettingDefinitions.ControlMode}
     *
     * @return Current ControlMode value or default if null
     */
    SettingDefinitions.ControlMode getControlMode();

    /**
     * Set the control mode value from {@link dji.ux.beta.util.SettingDefinitions.ControlMode}
     *
     * @param controlMode ControlMode value
     */
    void setControlMode(SettingDefinitions.ControlMode controlMode);

    //endregion
}

