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

package dji.ux.beta.accessory.widget.spotlight;

/**
 * Class represents the state of the spotlight accessory
 */
public class SpotlightState {

    private boolean isEnabled;
    private float temperature;
    private int brightnessPercentage;

    public SpotlightState(boolean isEnabled, int brightnessPercentage, float temperature) {
        this.isEnabled = isEnabled;
        this.temperature = temperature;
        this.brightnessPercentage = brightnessPercentage;
    }

    /**
     * Check if the spotlight currently switched on
     *
     * @return boolean value true - spotlight on  false - spotlight off
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Get the temperature of the spotlight
     *
     * @return float value representing temperature
     */
    public float getTemperature() {
        return temperature;
    }

    /**
     * Get the brightness percentage
     *
     * @return integer value representing percentage
     */
    public int getBrightnessPercentage() {
        return brightnessPercentage;
    }

    @Override
    public int hashCode() {
        int result = 31 * Float.floatToIntBits(temperature);
        result = result + 31 * Float.floatToIntBits(brightnessPercentage);
        result = result + 31 * (isEnabled ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpotlightState)) return false;
        SpotlightState that = (SpotlightState) o;
        return isEnabled == that.isEnabled &&
                Float.compare(that.temperature, temperature) == 0 &&
                brightnessPercentage == that.brightnessPercentage;
    }
}
