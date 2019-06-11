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

/**
 * Utility class for unit conversions
 */
public final class UnitConversionUtil {
    //region Constants
    private static final float METER_TO_FOOT = 3.2808f;
    private static final float METER_PER_SEC_TO_MILE_PER_HR = 2.2369f;
    private static final float METER_PER_SEC_TO_KM_PER_HR = 3.6f;
    private static final int SEC_TO_MIN = 60;
    private static final int MIN_TO_HOUR = 60;
    private static final int SEC_TO_HOUR = 3600;
    //endregion

    //region Constructor
    private UnitConversionUtil() {
        //Empty
    }
    //endregion

    //region Length or Distance Conversion Functions
    /**
     * Utility function to convert meters to feet
     *
     * @param value in meters
     * @return float value of the input meter value converted to feet
     */
    public static float convertMetersToFeet(float value) {
        return (value * METER_TO_FOOT);
    }

    /**
     * Utility function to convert feet to meters
     *
     * @param value in feet
     * @return float value of the input feet value converted to meters
     */
    public static float convertFeetToMeters(float value) {
        return (value / METER_TO_FOOT);
    }
    //endregion

    //region Speed Conversion Functions
    /**
     * Utility function to convert meters per second to miles per hour
     *
     * @param value in meter per second
     * @return float value of the input m/s converted to mph
     */
    public static float convertMetersPerSecToMilesPerHr(float value) {
        return (value * METER_PER_SEC_TO_MILE_PER_HR);
    }

    /**
     * Utility function to convert miles per hour to meters per second
     *
     * @param value in miles per hour
     * @return float value of the input mph converted to m/s
     */
    public static float convertMilesPerHrToMetersPerSec(float value) {
        return (value / METER_PER_SEC_TO_MILE_PER_HR);
    }

    /**
     * Utility function to convert meters per second to kilometers per hour
     *
     * @param value in meters per second
     * @return float value of the input m/s converted to km/hr
     */
    public static float convertMetersPerSecToKmPerHr(float value) {
        return (value * METER_PER_SEC_TO_KM_PER_HR);
    }

    /**
     * Utility function to convert kilometers per hour to meters per second
     *
     * @param value in kilometers per hour
     * @return float value of the input km/hr converted to m/s
     */
    public static float convertKmPerHrToMetersPerSec(float value) {
        return (value / METER_PER_SEC_TO_KM_PER_HR);
    }
    //endregion

    //region Time Conversion Functions
    /**
     * Convert the total value of seconds into an array with seconds, minutes, hours.
     *
     * @param second value in seconds
     * @return Array with equivalent value formatted as seconds, minutes, hours
     */
    public static int[] formatSecondToHour(int second) {
        final int[] time = new int[3];
        time[0] = second % SEC_TO_MIN;
        time[1] = (second / SEC_TO_MIN) % MIN_TO_HOUR;
        time[2] = second / (SEC_TO_HOUR);
        return time;
    }
    //endregion

    //region Enums

    /**
     * Specify the unit type of a given value
     */
    public enum UnitType {
        /**
         * The unit type is metric
         */
        METRIC("Metric", 0),

        /**
         * The unit type is imperial
         */
        IMPERIAL("Imperial", 1);

        private String stringValue;
        private int intValue;

        UnitType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int value() {
            return this.intValue;
        }
        public static UnitType find(int value) {
            UnitType result = METRIC;
            for (int i = 0; i < values().length; i++) {
                if (values()[i].intValue == value) {
                    result = values()[i];
                    break;
                }
            }
            return result;
        }
    }

    /**
     * Specify the metric unit type of a given speed value
     */
    public enum SpeedMetricUnitType {
        /**
         * The unit type is meters per second
         */
        METERS_PER_SECOND("MeterPerSec", 0),

        /**
         * The unit type is kilometers per hour
         */
        KM_PER_HOUR("KmPerHour", 1);
        private String stringValue;
        private int intValue;

        SpeedMetricUnitType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int value() {
            return this.intValue;
        }
        public static SpeedMetricUnitType find(int value) {
            SpeedMetricUnitType result = METERS_PER_SECOND;
            for (int i = 0; i < values().length; i++) {
                if (values()[i].intValue == value) {
                    result = values()[i];
                    break;
                }
            }
            return result;
        }
    }
    //endregion
}
