/**
 * @filename		: DJIUnitUtil.java
 * @package			: dji.pilot.publics.util
 * @date			: 2015-2-10 下午3:08:56
 * @author			: gashion.fang
 * 
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

public class DJIUnitUtil {

    //region Properties
    public static final float LENGTH_METRIC2IMPERIAL = 3.2808f; // meter to foot
    public static final float SPEED_METRIC2IMPERIAL = 2.2369f; // meter/s to
                                                               // mile/h
    public static final float LENGTH_METRIC2INCH = 39.4f; // meter to inch
    public static final String DjiFormat = "DjiFormat";

    public static final float TEMPERATURE_K2C = 273.15f;
    //endregion

    //region UnitType Enum
    public enum UnitType{
        METRIC("Metric", 0),
        IMPERIAL("Imperial", 1);

        private String stringValue;
        private int intValue;

        UnitType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public  String toString(){
            return stringValue;
        }
    }
    //endregion

    //region APIs
    /**
     * Description : 温度转换（开氏度 - 摄氏度）
     * 
     * @author : gashion.fang
     * @date : 2015-9-11 上午11:31:27
     * @param kelvin
     * @return
     */
    public static final float kelvinToCelsius(final float kelvin) {
        return (kelvin - TEMPERATURE_K2C);
    }

    /**
     * Description : 温度转换（摄氏度 - 开氏度）
     * 
     * @author : gashion.fang
     * @date : 2015-9-11 下午3:06:13
     * @param celsius
     * @return
     */
    public static final float celsiusToKelvin(final float celsius) {
        return (celsius + TEMPERATURE_K2C);
    }

    /**
     * Description : 温度转换（摄氏度 - 华氏度）
     * 
     * @author : gashion.fang
     * @date : 2015-9-11 上午11:33:31
     * @param celsius
     * @return
     */
    public static final float celsiusToFahrenheit(final float celsius) {
        return (celsius * 1.8f + 32);
    }

    /**
     * Description : 温度转换（华氏度 - 摄氏度）
     * 
     * @author : gashion.fang
     * @date : 2015-11-27 下午12:07:26
     * @param fahrenheit
     * @return
     */
    public static final float fahrenheitToCelsius(final float fahrenheit) {
        return (fahrenheit - 32) / 1.8f;
    }

    /**
     * Description : 公制转换成英制(长度）
     * 
     * @author : gashion.fang
     * @date : 2014-12-20 下午6:04:52
     * @param value
     * @return
     */
    public static float metricToImperialByLength(final float value) {
        return (value * LENGTH_METRIC2IMPERIAL);
    }

    /**
     * Description : 英制转换成公制（长度）
     * 
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:06:22
     * @param value
     * @return
     */
    public static float imperialToMetricByLength(final float value) {
        return (value / LENGTH_METRIC2IMPERIAL);
    }

    /**
     * Description : 公制转换成英制（速度）
     * 
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:12:35
     * @param vlaue
     * @return
     */
    public static float metricToImperialBySpeed(final float value) {
        return (value * SPEED_METRIC2IMPERIAL);
    }

    /**
     * Description : 英制转换成公制（速度）
     * 
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:14:58
     * @param value
     * @return
     */
    public static float imperialToMetricBySpeed(final float value) {
        return (value / SPEED_METRIC2IMPERIAL);
    }

    /**
     * Description : 将公制转换到当前所需的单位
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:42:23
     * @param value
     * @return
     */
    public static float getValueFromMetricByLength(float value, UnitType type) {
        float result;
        if (!isMetric(type)) {
            result = metricToImperialByLength(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将英制转换到当前所需的单位
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:42:26
     * @param value
     * @return
     */
    public static float getValueFromImperialByLength(float value, UnitType type) {
        float result;
        if (isMetric(type)) {
            result = imperialToMetricByLength(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将当前单位转换到公制
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午4:14:31
     * @param value
     * @return
     */
    public static float getValueFromMetricBySpeed(float value, UnitType type) {
        float result;
        if (!isMetric(type)) {
            result = metricToImperialBySpeed(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将当前单位转换到英制
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午4:14:35
     * @param value
     * @return
     */
    public static float getValueToImperialBySpeed(float value, UnitType type) {
        float result;
        if (isMetric(type)) {
            result = metricToImperialBySpeed(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 长度单位
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:38:07
     * @return
     */
    public static String getUintStrByLength(UnitType type) {
        return isMetric(type) ? "m" : "ft";
    }

    /**
     * Description : 速度单位
     * 
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:38:11
     * @return
     */
    public static String getUintStrBySpeed(UnitType type) {
        return isMetric(type) ? "m/s" : "mile/h";
    }

    public static boolean isMetric(UnitType type) {// 这里如果要取真实值 那么在限高那里要修改一下
        return type == UnitType.METRIC;
    }
    //endregion
}
