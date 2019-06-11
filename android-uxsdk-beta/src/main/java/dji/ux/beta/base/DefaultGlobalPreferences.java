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
import android.support.annotation.NonNull;
import dji.ux.beta.util.UnitConversionUtil;

/**
 * Default implementation of the GlobalPreferencesInterface using SharedPreferences.
 * These settings will persist across app restarts.
 */
public class DefaultGlobalPreferences implements GlobalPreferencesInterface {
    //region Constants
    private static final String PREF_GLOBAL_UNIT_TYPE = "globalUnitType";
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
}
