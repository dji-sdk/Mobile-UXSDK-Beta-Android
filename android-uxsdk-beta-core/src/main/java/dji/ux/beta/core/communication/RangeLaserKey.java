package dji.ux.beta.core.communication;


import dji.ux.beta.core.model.RangeEnable;

public class RangeLaserKey extends UXKeys {
    @UXParamKey(type = RangeEnable.class, updateType = UpdateType.ON_CHANGE)
    public static final String RANGE_LASER_KEY = "RANGE_LASER_KEY";
}