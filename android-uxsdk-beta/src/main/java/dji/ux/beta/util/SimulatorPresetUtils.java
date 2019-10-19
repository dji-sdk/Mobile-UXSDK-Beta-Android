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

package dji.ux.beta.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Map;

import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.beta.widget.simulator.preset.SimulatorPresetData;

/**
 * Simulator Widget Preferences
 * This shared preference file is dedicated for storing Simulator Presets.
 * It is essential to keep this segregation for getting all Preset entries even when
 * the keys dynamically change
 * Method to
 * 1. save a preset for simulator
 * 2. load the preset list
 * 3. delete a preset from the list
 */
public final class SimulatorPresetUtils {

    private static final String SIMULATOR_SHARED_PREFERENCES = "simulatorsharedpreferences";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static SimulatorPresetUtils instance = null;


    private SimulatorPresetUtils(Context context) {
        sharedPreferences = context.getSharedPreferences(SIMULATOR_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static SimulatorPresetUtils getInstance() {
        if (instance == null) {
            instance = new SimulatorPresetUtils(DJISDKManager.getInstance().getContext());
        }
        return instance;
    }


    public void savePreset(@NonNull String key, double lat, double lng, int satelliteCount, int frequency) {
        editor.putString(key, lat + " " + lng + " " + satelliteCount + " " + frequency).commit();

    }

    public void savePreset(@NonNull String key, @NonNull SimulatorPresetData simulatorPresetData) {
        editor.putString(key, simulatorPresetData.getLatitude()
                + " " + simulatorPresetData.getLongitude()
                + " " + simulatorPresetData.getSatelliteCount()
                + " " + simulatorPresetData.getUpdateFrequency()).commit();
    }

    public Map<String, ?> getPresetList() {
        return sharedPreferences.getAll();
    }

    public void deletePreset(String key) {
        editor.remove(key).commit();
    }

}

