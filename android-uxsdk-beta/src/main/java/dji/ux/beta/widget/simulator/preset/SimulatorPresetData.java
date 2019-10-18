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

package dji.ux.beta.widget.simulator.preset;

/**
 * Simulator Preset Data
 *
 * The class represents the instance of values which
 * can be used to run the simulator
 *
 */
public class SimulatorPresetData {

    private double latitude;
    private double longitude;
    private int satelliteCount;
    private int updateFrequency;

    public SimulatorPresetData(double lat, double lng, int satelliteCount, int frequency) {
        this.latitude = lat;
        this.longitude = lng;
        this.satelliteCount = satelliteCount;
        this.updateFrequency = frequency;
    }

    /**
     * Get latitude value of simulated location
     *
     * @return Double value
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Get longitude value of simulated location
     *
     * @return Double value
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Get the satellite count to be simulated
     *
     * @return integer value representing satellites
     */
    public int getSatelliteCount() {
        return satelliteCount;
    }

    /**
     * Get the frequency at which the aircraft should send data
     *
     * @return integer value
     */
    public int getUpdateFrequency() {
        return updateFrequency;
    }
}
