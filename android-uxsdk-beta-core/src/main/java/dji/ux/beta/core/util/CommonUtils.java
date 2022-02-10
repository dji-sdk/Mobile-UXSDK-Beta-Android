package dji.ux.beta.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/23
 * <p>
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
public class CommonUtils {

    public static List<Integer> toList(int [] ints){
        List<Integer> list = new ArrayList<>();
        if (ints == null){
            return list;
        }
        for (int i : ints){
            list.add(i);
        }
        return list;
    }

    public static SettingDefinitions.GimbalIndex getGimbalIndex(SettingDefinitions.CameraSide cameraSide) {
        if (cameraSide == null){
            return SettingDefinitions.GimbalIndex.PORT;
        }
        switch (cameraSide) {
            case STARBOARD:
                return SettingDefinitions.GimbalIndex.STARBOARD;
            case TOP:
                return SettingDefinitions.GimbalIndex.TOP;
            default:
                return SettingDefinitions.GimbalIndex.PORT;
        }
    }
}
