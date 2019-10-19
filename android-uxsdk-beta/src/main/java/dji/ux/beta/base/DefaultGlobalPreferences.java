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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import dji.ux.beta.util.SettingDefinitions;
import dji.ux.beta.util.UnitConversionUtil;
import dji.ux.beta.widget.fpv.CenterPointView;
import dji.ux.beta.widget.fpv.GridLineView;

/**
 * Default implementation of the GlobalPreferencesInterface using SharedPreferences.
 * These settings will persist across app restarts.
 */
public class DefaultGlobalPreferences implements GlobalPreferencesInterface {
    //region Constants
    private static final String PREF_IS_AFC_ENABLED = "afcEnabled";
    private static final String PREF_GLOBAL_UNIT_TYPE = "globalUnitType";
    private static final String PREF_GRID_LINE_TYPE = "gridLineType";
    private static final String PREF_CENTER_POINT_TYPE = "centerPointType";
    private static final String PREF_CENTER_POINT_COLOR = "centerPointColor";
    private static final String PREF_CONTROL_MODE = "controlMode";
    //endregion

    //region Fields
    private SharedPreferences sharedPreferences;
    //endregion

    public DefaultGlobalPreferences(Context context) {
        super();
        sharedPreferences = getSharedPreferences(context);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    @Override
    public void setUpListener() {
        //Do nothing
    }

    @Override
    public void cleanup() {
        //Do nothing
    }

    @NonNull
    @Override
    public UnitConversionUtil.UnitType getUnitType() {
        return UnitConversionUtil.UnitType.find(sharedPreferences.getInt(PREF_GLOBAL_UNIT_TYPE,
                UnitConversionUtil.UnitType.METRIC.value()));
    }

    @Override
    public void setUnitType(@NonNull UnitConversionUtil.UnitType unitType) {
        sharedPreferences.edit().putInt(PREF_GLOBAL_UNIT_TYPE, unitType.value()).apply();
    }

    @Override
    public boolean getAFCEnabled() {
        return sharedPreferences.getBoolean(PREF_IS_AFC_ENABLED, true);
    }

    @Override
    public void setAFCEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_IS_AFC_ENABLED, enabled).apply();
    }

    @Override
    public void setGridLineType(@NonNull GridLineView.GridLineType gridLineType) {
        sharedPreferences.edit().putInt(PREF_GRID_LINE_TYPE, gridLineType.value()).apply();
    }

    @NonNull
    @Override
    public GridLineView.GridLineType getGridLineType() {
        return GridLineView.GridLineType.find(sharedPreferences.getInt(PREF_GRID_LINE_TYPE,
                GridLineView.GridLineType.NONE.value()));
    }

    @Override
    public void setCenterPointType(@NonNull CenterPointView.CenterPointType centerPointType) {
        sharedPreferences.edit().putInt(PREF_CENTER_POINT_TYPE, centerPointType.value()).apply();
    }

    @NonNull
    @Override
    public CenterPointView.CenterPointType getCenterPointType() {
        return CenterPointView.CenterPointType.find(sharedPreferences.getInt(PREF_CENTER_POINT_TYPE,
                CenterPointView.CenterPointType.NONE.value()));
    }

    @Override
    public void setCenterPointColor(@ColorInt int centerPointColor) {
        sharedPreferences.edit().putInt(PREF_CENTER_POINT_COLOR, centerPointColor).apply();
    }

    @Override
    @ColorInt
    public int getCenterPointColor() {
        return sharedPreferences.getInt(PREF_CENTER_POINT_COLOR, Color.WHITE);
    }

    @NonNull
    @Override
    public SettingDefinitions.ControlMode getControlMode() {
        return SettingDefinitions.ControlMode.find(sharedPreferences.getInt(PREF_CONTROL_MODE,
                SettingDefinitions.ControlMode.SPOT_METER.value()));
    }

    @Override
    public void setControlMode(@NonNull SettingDefinitions.ControlMode controlMode) {
        sharedPreferences.edit().putInt(PREF_CONTROL_MODE, controlMode.value()).apply();
    }
}
