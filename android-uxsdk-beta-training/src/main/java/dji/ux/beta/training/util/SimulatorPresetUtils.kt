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

package dji.ux.beta.training.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import dji.sdk.sdkmanager.DJISDKManager

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
object SimulatorPresetUtils {
    private const val SIMULATOR_SHARED_PREFERENCES = "simulatorsharedpreferences"
    private const val SIMULATOR_FREQUENCY = "simulatorfrequency"
    private val sharedPreferences: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        sharedPreferences = DJISDKManager.getInstance().context.getSharedPreferences(SIMULATOR_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        @SuppressLint("CommitPrefErrors")
        editor = sharedPreferences.edit()
    }


    fun savePreset(key: String, lat: Double, lng: Double, satelliteCount: Int, frequency: Int) {
        editor.putString(key, "$lat $lng $satelliteCount $frequency").commit()
    }

    fun savePreset(key: String, simulatorPresetData: dji.ux.beta.training.widget.simulatorcontrol.preset.SimulatorPresetData) {
        editor.putString(key, simulatorPresetData.latitude
                .toString() + " " + simulatorPresetData.longitude
                + " " + simulatorPresetData.satelliteCount
                + " " + simulatorPresetData.updateFrequency).commit()
    }

    val presetList: Map<String, *>
        get() {
            val resultList = sharedPreferences.all
            resultList.remove(SIMULATOR_FREQUENCY)
            return resultList
        }

    fun deletePreset(key: String?) {
        editor.remove(key).commit()
    }

    fun saveCurrentSimulationFrequency(frequency: Int) {
        editor.putInt(SIMULATOR_FREQUENCY, frequency).commit()
    }

    val currentSimulatorFrequency: Int
        get() = sharedPreferences.getInt(SIMULATOR_FREQUENCY, -1)

    fun clearSimulatorFrequency() {
        editor.remove(SIMULATOR_FREQUENCY).commit()
    }

}